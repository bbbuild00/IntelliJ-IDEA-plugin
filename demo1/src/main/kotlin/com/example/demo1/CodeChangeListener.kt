package com.example.demo1

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import java.io.IOException

/**
 * CodeChangeListener 是一个监听器，用于监听项目中的 PSI 变化。
 * 当监听到代码文件发生变化时，会生成该文件的快照并保存。
 */
class CodeChangeListener(private val versionControl: VersionControl) : PsiTreeChangeAdapter() {

    // 日志记录器，用于记录调试信息，帮助跟踪插件的运行状态
    private val logger = Logger.getInstance(CodeChangeListener::class.java)

    /**
     * 当 Psi 树的子节点发生变化时触发此方法。
     * 这个方法监听到代码文件的修改，并进行快照生成与保存。
     */
    override fun childrenChanged(event: PsiTreeChangeEvent) {
        logger.info("childrenChanged event triggered")  // 记录事件触发日志

        // 获取发生变化的 PsiFile（代码文件）
        val psiFile = event.file
        if (psiFile != null) {
            logger.info("PsiFile found: ${psiFile.name}")  // 记录 PsiFile 名称

            // 获取虚拟文件对象 VirtualFile
            val virtualFile = psiFile.virtualFile
            if (virtualFile != null) {
                logger.info("VirtualFile found: ${virtualFile.name}")  // 记录 VirtualFile 名称

                try {
                    // 获取文件内容并转为字节数组
                    val content = virtualFile.contentsToByteArray()
                    logger.info("File content size: ${content.size} bytes")  // 记录文件内容大小

                    // 创建文件的快照对象
                    val snapshot = Snapshot(virtualFile.path, content, System.currentTimeMillis())
                    logger.info("Snapshot created for file: ${virtualFile.path}")  // 记录快照创建信息

                    // 保存文件快照
                    //调用B模块方法
                    versionControl.saveVersionSnapshot(virtualFile, snapshot)
                    logger.info("Snapshot saved for file: ${virtualFile.path}")  // 记录快照保存信息

                    // 控制台输出确认快照保存
                    println("Snapshot created and saved for file: ${virtualFile.name}")

                    // 发送通知，提示用户快照已经成功创建
                    val notification = Notification(
                            "CodeChangeListener",
                            "Code Change Detected",
                            "Snapshot created for file: ${virtualFile.name}",
                            NotificationType.INFORMATION
                    )
                    Notifications.Bus.notify(notification)
                } catch (e: IOException) {
                    // 捕获文件操作的异常并记录错误日志
                    logger.error("Failed to create snapshot for file: ${virtualFile.name}", e)

                    // 输出控制台错误信息
                    println("Error: Failed to create snapshot for file: ${virtualFile.name}")

                    // 发送错误通知
                    val notification = Notification(
                            "CodeChangeListener",
                            "Error",
                            "Failed to create snapshot for file: ${virtualFile.name}",
                            NotificationType.ERROR
                    )
                    Notifications.Bus.notify(notification)
                }
            } else {
                // 当 VirtualFile 为 null 时，记录警告日志并输出控制台信息
                logger.warn("VirtualFile is null for PsiFile: ${psiFile.name}")
                println("Warning: VirtualFile is null for PsiFile: ${psiFile.name}")
            }
        } else {
            // 当 PsiFile 为 null 时，记录警告日志并输出控制台信息
            logger.warn("PsiFile is null in childrenChanged event")
            println("Warning: PsiFile is null in childrenChanged event")
        }
    }
}
