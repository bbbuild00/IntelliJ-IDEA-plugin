package com.example.demo1

import com.intellij.openapi.vfs.VirtualFile

/**
 * VersionControl 是版本控制的接口，定义了保存代码快照的方法。
 */
interface VersionControl {

    /**
     * 保存文件的版本快照。
     *
     * @param file VirtualFile 对象，表示发生变化的文件
     * @param snapshot Snapshot 对象，包含文件的快照信息
     */
    fun saveVersionSnapshot(file: VirtualFile, snapshot: Snapshot)
}
