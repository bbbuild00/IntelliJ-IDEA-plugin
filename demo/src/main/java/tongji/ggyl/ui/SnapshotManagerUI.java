package tongji.ggyl.ui;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.Chunk;
import com.intellij.ui.components.JBList;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import com.github.difflib.patch.DeltaType;
import tongji.ggyl.versioncontrol.VersionControl;
import tongji.ggyl.eventlistening.Snapshot;

public class SnapshotManagerUI {
    private final JFrame frame = new JFrame("Version Control Manager");
    private JList<String> snapshotList; // 显示快照信息的列表控件
    private Snapshot[] snapshots; // 存放快照信息
    private final JTextPane leftPane = new JTextPane(); // 左侧用于显示对比文件
    private final JTextPane rightPane = new JTextPane(); // 右侧用于显示选择的快照文件

    public SnapshotManagerUI(VersionControl versionControl) {
        frame.setSize(1000, 600); // 调整窗口大小
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);

        initFileListView(versionControl);

        // 创建双栏布局的 JPanel，包含标题和代码显示区
        JPanel contentPanel = new JPanel(new GridLayout(1, 2)); // 一行两列
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new BorderLayout());

        leftPane.setEditable(false);
        rightPane.setEditable(false);
        // 左侧标题
        JLabel leftTitleLabel = new JLabel("Last Version");
        leftTitleLabel.setHorizontalAlignment(SwingConstants.CENTER); // 居中对齐
        leftTitleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // 设置字体和大小
        leftTitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // 设置边距，上下各10个像素
        leftPanel.add(leftTitleLabel, BorderLayout.NORTH); // 左侧标题
        leftPanel.add(new JScrollPane(leftPane), BorderLayout.CENTER); // 左侧显示对比文件
        // 右侧标题
        JLabel rightTitleLabel = new JLabel("Current Version");
        rightTitleLabel.setHorizontalAlignment(SwingConstants.CENTER); // 居中对齐
        rightTitleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // 设置字体和大小
        rightTitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // 设置边距，上下各10个像素
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
    // 初始化快照列表
    private void initFileListView(VersionControl versionControl){
        JPanel listPanel = new JPanel(new BorderLayout());
        snapshots = versionControl.getAllSnapshots().toArray(new Snapshot[0]);
        // 按照时间戳进行排序
        Arrays.sort(snapshots, (a, b) -> {
            String timestampA = a.getTimestamp();
            String timestampB = b.getTimestamp();

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date dateA = sdf.parse(timestampA);
                Date dateB = sdf.parse(timestampB);
                return dateB.compareTo(dateA); // 从早到晚排序
            } catch (ParseException e) {
                e.printStackTrace();
                return 0; // 出现异常时不改变顺序
            }
        });
        snapshotList = new JBList<>(getSnapshotString(snapshots));
        // 设置自定义渲染器
        snapshotList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // 使用默认的渲染组件
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // 解析数据并设置两行文字
                String[] parts = value.toString().split(", ");
                String timestamp = parts[0];
                String fileName = parts[1];

                label.setText("<html><span style='font-size: 11px; color: #888888;'>" + timestamp + "</span><br><span style='font-size: 11px;'>" + fileName + "</span></html>");

                return label;
            }
        });
        initCombo(listPanel);
        // 快照列表放在左侧，内容显示在右侧的双栏
        listPanel.add(new JScrollPane(snapshotList), BorderLayout.CENTER);
        frame.add(listPanel, BorderLayout.WEST);

    }
    // 初始化文件下拉选择框
    private void initCombo(JPanel listPanel){
        // 添加下拉框显示文件名
        JComboBox<String> fileDropdown = new JComboBox<>();
        fileDropdown.addItem("All");
        fileDropdown.setPreferredSize(new Dimension(fileDropdown.getPreferredSize().width, 40));
        Set<String> uniqueFileNames = new HashSet<>();
        for (Snapshot snapshot : snapshots) {
            // 从snapshot文件名中去掉时间戳部分
            String fileName = snapshot.getFileNameWithoutTimestamp();
            uniqueFileNames.add(fileName);
        }
        // 添加所有不重复的文件名到下拉框
        for (String fileName : uniqueFileNames) {
            fileDropdown.addItem(fileName);
        }

        // 添加ActionListener响应选择的文件名
        fileDropdown.addActionListener(e -> {
            String selectedFileName = (String) fileDropdown.getSelectedItem();
            if(Objects.equals(selectedFileName, "All")){
                snapshotList.setListData(getSnapshotString(snapshots));
            }else{
                List<Snapshot> filteredSnapshots = new ArrayList<>();

                // 过滤出所有与选中文件名匹配的快照
                for (Snapshot snapshot : snapshots) {
                    if (snapshot.getFileNameWithoutTimestamp().equals(selectedFileName)) {
                        filteredSnapshots.add(snapshot);
                    }
                }
                // 更新JList中的数据
                snapshotList.setListData(getSnapshotString(filteredSnapshots.toArray(new Snapshot[0])));
            }
        });

        // 默认显示所有文件名的快照
        snapshotList.setListData(getSnapshotString(snapshots));

        // 设置默认选项为 "All"
        fileDropdown.setSelectedItem("All");
        listPanel.add(fileDropdown, BorderLayout.NORTH);
    }
    // 刷新比较显示内容
    private void refreshComparison(Snapshot selectedSnapshot, int selectedIndex, Snapshot[] snapshots) {
        if (selectedIndex > 0) {
            // 与上一快照比较
            Snapshot previousSnapshot = snapshots[selectedIndex - 1];
            displayDiff(previousSnapshot, selectedSnapshot);
        } else {
            // 当点击最早的一版快照时，左侧文本框显示“无上一版快照”，右侧显示选择的快照内容
            leftPane.setText("This is already the earliest version. HAHA!");
            displaySnapshotContent(selectedSnapshot);
        }
    }

    // 显示选择的快照内容
    private void displaySnapshotContent(Snapshot snapshot) {
        StyledDocument rightDoc = rightPane.getStyledDocument();
        rightPane.setText(""); // 清空右侧文本框

        String[] lines = new String(snapshot.getContent()).split("\n");
        for (String line : lines) {
            appendTextWithStyle(line, Color.WHITE, rightDoc);
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
            if(delta.getType() == DeltaType.INSERT){
                // 使用绿色标记新文件增加的部分
                highlightLine(rightDoc, revisedChunk.getPosition(), revisedChunk.getLines(), Color.GREEN);
            } else if (delta.getType() == DeltaType.CHANGE) {
                // 使用黄色标记改动的部分
                highlightLine(leftDoc, originalChunk.getPosition(), originalChunk.getLines(), Color.YELLOW);
                highlightLine(rightDoc, revisedChunk.getPosition(), revisedChunk.getLines(), Color.YELLOW);
            } else if (delta.getType() == DeltaType.DELETE) {
                // 使用红色标记旧文件删除的部分
                highlightLine(leftDoc, originalChunk.getPosition(), originalChunk.getLines(), Color.RED);
            }
        }
    }

    // 高亮显示某行的颜色（在原始位置上改变颜色）
    private void highlightLine(StyledDocument doc, int startPos, List<String> lines, Color color) {
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attributeSet, Color.BLACK);
        StyleConstants.setBackground(attributeSet, color); // 设置背景颜色

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
    private String[] getSnapshotString(Snapshot[] s){
        String[] snapshotStrings = new String[s.length];
        for (int i = 0; i < s.length; i++) {
            String formattedTimestamp = s[i].getTimestamp();
            String fileName = s[i].getFileNameWithoutTimestamp();
            snapshotStrings[i] = formattedTimestamp + ", " + fileName;
        }
        return snapshotStrings;
    }
}
