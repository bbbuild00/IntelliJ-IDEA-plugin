package com.example.demo1

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile

class ShowSnapshotManagerAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        // 获取项目和虚拟文件
        val project = event.project ?: return
        val file = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return

        // 实例化版本控制对象
        val versionControl = VersionControlImpl(project)

        // 调用 SnapshotManagerUI 来显示快照管理界面
        SnapshotManagerUI(versionControl, file)
    }
}
