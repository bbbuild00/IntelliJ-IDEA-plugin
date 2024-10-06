package tongji.lwm;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.Chunk;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.List;

public class SnapshotManagerUI {
    private final JFrame frame = new JFrame("Snapshot Manager");
    private final JList<String> snapshotList = new JList<>();
    private final JTextPane leftPane = new JTextPane(); // 左侧用于显示对比文件
    private final JTextPane rightPane = new JTextPane(); // 右侧用于显示选择的快照文件
    private final JLabel leftTitleLabel = new JLabel("上一快照"); // 左侧标题
    private final JLabel rightTitleLabel = new JLabel("您选择的快照"); // 右侧标题
    private final Project project;

    public SnapshotManagerUI(VersionControl versionControl, Project project) {
        this.project = project;  // 将项目对象传入
        frame.setSize(1000, 600); // 调整窗口大小
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);

        Snapshot[] snapshots = versionControl.getSnapshotsForFile("E:/plugin-develop/test01/main.java").toArray(new Snapshot[0]);
        String[] snapshotStrings = new String[snapshots.length];
        for (int i = 0; i < snapshots.length; i++) {
            String formattedTimestamp = snapshots[i].getTimestamp();
            snapshotStrings[i] = "Snapshot from " + formattedTimestamp;
        }
        snapshotList.setListData(snapshotStrings);

        // 快照列表放在左侧，内容显示在右侧的双栏
        frame.add(new JScrollPane(snapshotList), BorderLayout.WEST);

        // 创建双栏布局的 JPanel，包含标题和代码显示区
        JPanel contentPanel = new JPanel(new GridLayout(1, 2)); // 一行两列
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new BorderLayout());

        leftPane.setEditable(false);
        rightPane.setEditable(false);
        leftPanel.add(leftTitleLabel, BorderLayout.NORTH); // 左侧标题
        leftPanel.add(new JScrollPane(leftPane), BorderLayout.CENTER); // 左侧显示对比文件
        rightPanel.add(rightTitleLabel, BorderLayout.NORTH); // 右侧标题
        rightPanel.add(new JScrollPane(rightPane), BorderLayout.CENTER); // 右侧显示选择的快照文件

        contentPanel.add(leftPanel); // 左侧面板
        contentPanel.add(rightPanel); // 右侧面板
        frame.add(contentPanel, BorderLayout.CENTER); // 把双栏布局添加到主界面

        snapshotList.addListSelectionListener(e -> {
            int selectedIndex = snapshotList.getSelectedIndex();
            if (selectedIndex != -1) {
                Snapshot selectedSnapshot = snapshots[selectedIndex];
                refreshComparison(selectedSnapshot, selectedIndex, snapshots);
            }
        });

        // 添加右键菜单
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem compareWithPreviousMenuItem = new JMenuItem("与上一快照比较");
        popupMenu.add(compareWithPreviousMenuItem);

        snapshotList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = snapshotList.locationToIndex(e.getPoint());
                    snapshotList.setSelectedIndex(index); // 选中当前点击的项目
                    popupMenu.show(snapshotList, e.getX(), e.getY()); // 显示右键菜单
                }
            }
        });

        // 与上一快照比较
        compareWithPreviousMenuItem.addActionListener(e -> {
            int selectedIndex = snapshotList.getSelectedIndex();
            if (selectedIndex > 0) {
                Snapshot previousSnapshot = snapshots[selectedIndex - 1];
                Snapshot selectedSnapshot = snapshots[selectedIndex];
                displayDiff(previousSnapshot, selectedSnapshot); // 显示差异
            } else {
                JOptionPane.showMessageDialog(frame, "没有可比较的上一 snapshot。");
            }
        });
    }

    // 刷新比较显示内容
    private void refreshComparison(Snapshot selectedSnapshot, int selectedIndex, Snapshot[] snapshots) {
        if (selectedIndex > 0) {
            // 与上一快照比较
            Snapshot previousSnapshot = snapshots[selectedIndex - 1];
            leftTitleLabel.setText("上一快照");
            rightTitleLabel.setText("您选择的快照");
            displayDiff(previousSnapshot, selectedSnapshot);
        }
    }

    // 显示差异内容：左侧显示对比文件，右侧显示快照文件
    private void displayDiff(Snapshot oldSnapshot, Snapshot newSnapshot) {
        List<String> oldContent = List.of(new String(oldSnapshot.getContent()).split("\n"));
        List<String> newContent = List.of(new String(newSnapshot.getContent()).split("\n"));

        Patch<String> diff = DiffUtils.diff(oldContent, newContent);

        // 清空左右两侧内容
        StyledDocument leftDoc = leftPane.getStyledDocument();
        StyledDocument rightDoc = rightPane.getStyledDocument();
        leftPane.setText("");
        rightPane.setText("");

        // 遍历所有行，默认两边都是白色
        int maxLines = Math.max(oldContent.size(), newContent.size());
        for (int i = 0; i < maxLines; i++) {
            String leftLine = i < oldContent.size() ? oldContent.get(i) : "";
            String rightLine = i < newContent.size() ? newContent.get(i) : "";

            // 默认将未变动部分显示为白色
            appendTextWithStyle(leftLine, Color.WHITE, leftDoc);
            appendTextWithStyle(rightLine, Color.WHITE, rightDoc);
        }

        // 遍历差异的 delta 来标记变化的部分
        for (AbstractDelta<String> delta : diff.getDeltas()) {
            Chunk<String> originalChunk = delta.getSource();
            Chunk<String> revisedChunk = delta.getTarget();

            // 统一使用绿色标记变化的部分
            highlightLine(leftDoc, originalChunk.getPosition(), originalChunk.getLines(), Color.GREEN);
            highlightLine(rightDoc, revisedChunk.getPosition(), revisedChunk.getLines(), Color.GREEN);
        }
    }

    // 高亮显示某行的颜色（在原始位置上改变颜色）
    private void highlightLine(StyledDocument doc, int startPos, List<String> lines, Color color) {
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attributeSet, color);

        for (int i = 0; i < lines.size(); i++) {
            // 找到每一行的起始位置
            int lineStart = getLineStartOffset(doc, startPos + i);
            if (lineStart != -1) {
                doc.setCharacterAttributes(lineStart, lines.get(i).length(), attributeSet, true);
            }
        }
    }

    // 获取某一行的起始偏移位置
    private int getLineStartOffset(StyledDocument doc, int lineNumber) {
        Element root = doc.getDefaultRootElement();
        if (lineNumber >= 0 && lineNumber < root.getElementCount()) {
            return root.getElement(lineNumber).getStartOffset();
        } else {
            return -1; // 行号超出范围
        }
    }

    // 将带有样式的文本插入到 JTextPane 中
    private void appendTextWithStyle(String line, Color color, StyledDocument doc) {
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attributeSet, color);

        try {
            // 插入带有样式的行文本
            doc.insertString(doc.getLength(), line + "\n", attributeSet);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
