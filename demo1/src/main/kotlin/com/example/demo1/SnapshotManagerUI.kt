package com.example.demo1

import javax.swing.*
import java.awt.BorderLayout
import com.intellij.openapi.vfs.VirtualFile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SnapshotManagerUI(private val versionControl: VersionControl, private val file: VirtualFile) {
    private val frame = JFrame("Snapshot Manager")
    private val snapshotList = JList<String>()
    private val restoreButton = JButton("Restore Snapshot")
    private val snapshotContentArea = JTextArea() // 用于显示快照内容的文本区域

    // 定义一个日期时间格式化器，将时间戳转换为年-月-日 时:分:秒格式
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    init {
        frame.setSize(800, 400)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.layout = BorderLayout()

        // 获取历史快照并显示在列表中
        val snapshots = versionControl.getSnapshotsForFile(file)
        val snapshotStrings = snapshots.map {
            val formattedTimestamp = formatTimestamp(it.timestamp)
            "Snapshot from $formattedTimestamp"
        }.toTypedArray()
        snapshotList.setListData(snapshotStrings)

        // 快照列表放在左侧，内容显示在右侧
        frame.add(JScrollPane(snapshotList), BorderLayout.WEST)
        snapshotContentArea.isEditable = false
        frame.add(JScrollPane(snapshotContentArea), BorderLayout.CENTER)

        // 监听快照列表的点击事件，显示快照内容
        snapshotList.addListSelectionListener {
            val selectedIndex = snapshotList.selectedIndex
            if (selectedIndex != -1) {
                // 显示选中的快照内容
                val selectedSnapshot = snapshots[selectedIndex]
                val snapshotContent = String(selectedSnapshot.content, Charsets.UTF_8)
                snapshotContentArea.text = snapshotContent // 将快照内容显示在右侧文本框中
            }
        }

        // 恢复按钮放在底部
        frame.add(restoreButton, BorderLayout.SOUTH)

        // 监听恢复按钮点击事件，恢复到选中的快照
        restoreButton.addActionListener {
            val selectedIndex = snapshotList.selectedIndex
            if (selectedIndex != -1) {
                // 恢复选中的快照
                val selectedSnapshot = snapshots[selectedIndex]
                restoreSnapshot(file, selectedSnapshot)
                JOptionPane.showMessageDialog(frame, "Snapshot restored successfully!")
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a snapshot to restore.")
            }
        }

        frame.isVisible = true
    }

    // 将 timestamp 格式化为 "yyyy-MM-dd HH:mm:ss"
    private fun formatTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp) // 将时间戳转为 Instant
        return formatter.format(instant) // 使用 formatter 格式化时间戳
    }

    // 恢复快照的功能
    private fun restoreSnapshot(file: VirtualFile, snapshot: Snapshot) {
        try {
            file.setBinaryContent(snapshot.content)  // 将快照内容写回文件，恢复到该快照的状态
            println("Restored snapshot for file: ${file.name} to timestamp: ${snapshot.timestamp}")
        } catch (e: Exception) {
            println("Error restoring snapshot: ${e.message}")
        }
    }
}
