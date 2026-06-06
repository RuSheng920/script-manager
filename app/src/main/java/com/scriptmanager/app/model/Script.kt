package com.scriptmanager.app.model

import java.util.UUID

/**
 * 脚本实体
 */
data class Script(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val content: String = "",
    val filePath: String? = null,      // 关联的外部文件路径
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val runCount: Int = 0,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
) {
    /**
     * 显示用名称（去掉 .sh 后缀）
     */
    val displayName: String
        get() = name.removeSuffix(".sh")

    /**
     * 是否是内置脚本（存在 app 内部存储）
     */
    val isBuiltIn: Boolean
        get() = filePath == null
}
