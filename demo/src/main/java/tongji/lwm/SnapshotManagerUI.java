package tongji.lwm;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import com.intellij.openapi.vfs.VirtualFile;

public class SnapshotManagerUI {
    //创建一个窗口（JFrame
    private final JFrame frame = new JFrame("Snapshot Manager");
    //创建一个 JList 组件，用于显示快照列表。
    private final JList<String> snapshotList = new JList<>();
    private final JTextArea snapshotContentArea = new JTextArea(); // 用于显示快照内容的文本区域
    public SnapshotManagerUI(VersionControl versionControl) {
        frame.setSize(800, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);

        // 获取历史快照并显示在列表中
        Snapshot[] snapshots = versionControl.getSnapshotsForFile("E:/plugin-develop/test01/People/Student.java").toArray(new Snapshot[0]);
        String[] snapshotStrings = new String[snapshots.length];
        for (int i = 0; i < snapshots.length; i++) {
            String formattedTimestamp = snapshots[i].getTimestamp();
            snapshotStrings[i] = "Snapshot from " + formattedTimestamp;
        }
        snapshotList.setListData(snapshotStrings);

        // 快照列表放在左侧，内容显示在右侧
        frame.add(new JScrollPane(snapshotList), BorderLayout.WEST);
        snapshotContentArea.setEditable(false);
        frame.add(new JScrollPane(snapshotContentArea), BorderLayout.CENTER);

        // 监听快照列表的点击事件，显示快照内容
        snapshotList.addListSelectionListener(e -> {
            int selectedIndex = snapshotList.getSelectedIndex();
            if (selectedIndex != -1) {
                // 显示选中的快照内容
                Snapshot selectedSnapshot = snapshots[selectedIndex];
                String snapshotContent = new String(selectedSnapshot.getContent());
                snapshotContentArea.setText(snapshotContent); // 将快照内容显示在右侧文本框中
            }
        });

    }

}
