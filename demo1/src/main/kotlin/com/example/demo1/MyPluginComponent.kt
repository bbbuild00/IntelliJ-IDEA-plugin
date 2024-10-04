package com.example.demo1

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

/**
 * MyPluginComponent 是插件的主组件类，用于管理插件的生命周期。
 * 它负责初始化 CodeChangeListener，并在项目打开时开始监听代码变化。
 */
class MyPluginComponent(private val project: Project) : ProjectComponent {

    // 日志记录器，用于记录调试信息
    private val logger = Logger.getInstance(MyPluginComponent::class.java)

    // 版本控制对象，用于保存代码快照
    private val versionControl = VersionControlImpl(project)

    /**
     * 当项目打开时调用此方法，初始化 CodeChangeListener 并开始监听代码变化。
     */
    override fun projectOpened() {
        try {
            // 输出日志，确认项目已打开并开始监听
            logger.info("Project opened: ${project.name}. Initializing CodeChangeListener...")

            // 获取 PsiManager 并注册 CodeChangeListener 来监听 PSI 变化
            val psiManager = PsiManager.getInstance(project)
            psiManager.addPsiTreeChangeListener(CodeChangeListener(versionControl))

            // 输出日志，确认监听器已成功启动
            logger.info("CodeChangeListener started successfully for project: ${project.name}")

        } catch (e: Exception) {
            // 捕获初始化过程中的异常并记录错误日志
            logger.error("Exception caught during projectOpened for project: ${project.name}", e)
        }
    }

    /**
     * 当项目关闭时调用此方法，用于清理资源。
     */
    override fun projectClosed() {
        // 输出日志，确认项目已关闭
        logger.info("Project closed: ${project.name}. Removing listeners and cleaning up.")
    }
}
