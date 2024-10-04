package com.example.demo1

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import java.io.File
import java.io.IOException
import java.util.UUID
import com.google.gson.Gson
import java.util.concurrent.Executors


class VersionControlImpl(private val project: Project) : VersionControl {

    private val logger = Logger.getInstance(VersionControlImpl::class.java)

    // 创建 Gson 实例
    private val gson = Gson()

    // 存储文件的 UUID 映射
    private val uuidMapping = mutableMapOf<String, String>()

    // 初始化或加载 UUID 映射
    init {
        loadUUIDMapping()
    }

    // 创建线程池，用于异步保存快照
    private val executorService = Executors.newSingleThreadExecutor()

    override fun saveVersionSnapshot(file: VirtualFile, snapshot: Snapshot) {
        executorService.submit {
            val projectBasePath = project.basePath ?: "default/path"
            val snapshotsDirectory = File("$projectBasePath/snapshots")
            if (!snapshotsDirectory.exists()) {
                snapshotsDirectory.mkdirs()
            }

            // 获取文件的 UUID
            val fileUUID = uuidMapping.getOrPut(file.path) { UUID.randomUUID().toString() }
            saveUUIDMapping()

            // 保存快照文件，使用 UUID 作为文件名的一部分
            val snapshotFile = File(snapshotsDirectory, "${fileUUID}_${snapshot.timestamp}.snapshot")
            try {
                snapshotFile.writeBytes(snapshot.content)
                logger.info("Snapshot saved for file: ${file.name} at ${snapshot.timestamp}")
            } catch (e: IOException) {
                logger.error("Failed to save snapshot for file: ${file.name}", e)
            }
        }
    }

    override fun getSnapshotsForFile(file: VirtualFile): List<Snapshot> {
        val projectBasePath = project.basePath ?: "default/path"
        val snapshotsDirectory = File("$projectBasePath/snapshots")

        // 查找文件的 UUID
        val fileUUID = uuidMapping[file.path] ?: return emptyList()

        val snapshots = mutableListOf<Snapshot>()
        if (snapshotsDirectory.exists()) {
            val snapshotFiles = snapshotsDirectory.listFiles { _, name ->
                name.startsWith(fileUUID) && name.endsWith(".snapshot")
            }

            snapshotFiles?.forEach { snapshotFile ->
                val content = snapshotFile.readBytes()
                val timestamp = extractTimestampFromFilename(snapshotFile.name)
                snapshots.add(Snapshot(file.path, content, timestamp))
            }
        }
        return snapshots.sortedByDescending { it.timestamp }
    }

    // 从文件名中提取时间戳
    private fun extractTimestampFromFilename(fileName: String): Long {
        val parts = fileName.split("_")
        return parts.last().removeSuffix(".snapshot").toLong()
    }

    // 加载 UUID 映射
    private fun loadUUIDMapping() {
        val uuidMappingFile = File("${project.basePath}/uuid_mapping.json")
        if (uuidMappingFile.exists()) {
            val json = uuidMappingFile.readText()
            uuidMapping.putAll(gson.fromJson(json, Map::class.java) as Map<String, String>)
        }
    }

    // 保存 UUID 映射
    private fun saveUUIDMapping() {
        val uuidMappingFile = File("${project.basePath}/uuid_mapping.json")
        val json = gson.toJson(uuidMapping)
        uuidMappingFile.writeText(json)
    }

    fun fileRenamed(event: VirtualFileEvent) {
        val oldPath = event.file.path
        val newPath = event.file.path
        val fileUUID = uuidMapping.remove(oldPath)
        if (fileUUID != null) {
            uuidMapping[newPath] = fileUUID
            saveUUIDMapping()
            logger.info("File renamed from $oldPath to $newPath, UUID mapping updated.")
        }
    }
}
