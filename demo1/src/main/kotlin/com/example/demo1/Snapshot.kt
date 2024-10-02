package com.example.demo1

/**
 * Snapshot 类用于保存代码文件的快照信息，包括文件路径、内容和时间戳。
 */
data class Snapshot(
        val filePath: String,    // 文件路径
        val content: ByteArray,  // 文件内容
        val timestamp: Long      // 快照创建时间戳
)
