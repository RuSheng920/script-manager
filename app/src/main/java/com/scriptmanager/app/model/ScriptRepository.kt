package com.scriptmanager.app.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 脚本仓库 - 管理脚本的增删改查
 */
class ScriptRepository(private val context: Context) {

    private val scriptsDir: File
        get() = File(context.filesDir, "scripts").also { it.mkdirs() }

    private val indexPath: File
        get() = File(context.filesDir, "scripts_index.json")

    private val _scripts = MutableStateFlow<List<Script>>(emptyList())
    val scripts: Flow<List<Script>> = _scripts.asStateFlow()

    /**
     * 从存储加载所有脚本
     */
    suspend fun loadScripts() = withContext(Dispatchers.IO) {
        try {
            val indexFile = indexPath
            if (!indexFile.exists()) {
                _scripts.value = emptyList()
                return@withContext
            }

            val json = indexFile.readText()
            val array = JSONArray(json)
            val list = mutableListOf<Script>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val script = jsonToScript(obj)
                // 检查关联的内容文件是否存在
                val contentFile = getContentFile(script.id)
                val currentContent = if (contentFile.exists()) contentFile.readText() else script.content
                list.add(script.copy(content = currentContent))
            }

            _scripts.value = list
        } catch (e: Exception) {
            _scripts.value = emptyList()
        }
    }

    /**
     * 保存/更新脚本
     */
    suspend fun saveScript(script: Script) = withContext(Dispatchers.IO) {
        val currentList = _scripts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == script.id }

        // 保存脚本内容到独立文件
        val contentFile = getContentFile(script.id)
        contentFile.writeText(script.content)

        val updatedScript = script.copy(modifiedAt = System.currentTimeMillis())

        if (index >= 0) {
            currentList[index] = updatedScript
        } else {
            currentList.add(updatedScript)
        }

        _scripts.value = currentList
        persistIndex(currentList)
    }

    /**
     * 删除脚本
     */
    suspend fun deleteScript(scriptId: String) = withContext(Dispatchers.IO) {
        // 删除内容文件
        getContentFile(scriptId).delete()
        // 从列表移除
        val currentList = _scripts.value.toMutableList()
        currentList.removeAll { it.id == scriptId }
        _scripts.value = currentList
        persistIndex(currentList)
    }

    /**
     * 从外部文件导入脚本
     */
    suspend fun importFromFile(file: File): Script = withContext(Dispatchers.IO) {
        val content = file.readText()
        val script = Script(
            name = file.name,
            content = content,
            filePath = file.absolutePath,
            description = "从 ${file.absolutePath} 导入"
        )
        saveScript(script)
        script
    }

    /**
     * 扫描外部目录导入脚本
     */
    suspend fun scanDirectory(directory: File): List<Script> = withContext(Dispatchers.IO) {
        val shFiles = directory.listFiles { file ->
            file.isFile && (file.name.endsWith(".sh") || file.name.endsWith(".bash"))
        } ?: emptyArray()

        shFiles.map { file ->
            try {
                val content = file.readText()
                val script = Script(
                    name = file.name,
                    content = content,
                    filePath = file.absolutePath
                )
                saveScript(script)
                script
            } catch (e: Exception) {
                null
            }
        }.filterNotNull()
    }

    /**
     * 记录脚本运行
     */
    suspend fun recordRun(scriptId: String) = withContext(Dispatchers.IO) {
        val currentList = _scripts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == scriptId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(
                lastRunAt = System.currentTimeMillis(),
                runCount = currentList[index].runCount + 1
            )
            _scripts.value = currentList
            persistIndex(currentList)
        }
    }

    /**
     * 切换收藏
     */
    suspend fun toggleFavorite(scriptId: String) = withContext(Dispatchers.IO) {
        val currentList = _scripts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == scriptId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(
                isFavorite = !currentList[index].isFavorite
            )
            _scripts.value = currentList
            persistIndex(currentList)
        }
    }

    // ===== 私有方法 =====

    private fun getContentFile(scriptId: String): File {
        return File(scriptsDir, "${scriptId}.sh")
    }

    private fun scriptToJson(script: Script): JSONObject {
        return JSONObject().apply {
            put("id", script.id)
            put("name", script.name)
            put("description", script.description)
            put("filePath", script.filePath ?: "")
            put("createdAt", script.createdAt)
            put("modifiedAt", script.modifiedAt)
            put("lastRunAt", script.lastRunAt ?: 0)
            put("runCount", script.runCount)
            put("isFavorite", script.isFavorite)
            put("tags", JSONArray(script.tags))
        }
    }

    private fun jsonToScript(obj: JSONObject): Script {
        val tags = mutableListOf<String>()
        val tagsArray = obj.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }

        return Script(
            id = obj.getString("id"),
            name = obj.getString("name"),
            description = obj.optString("description", ""),
            filePath = obj.optString("filePath", "").ifEmpty { null },
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            modifiedAt = obj.optLong("modifiedAt", System.currentTimeMillis()),
            lastRunAt = obj.optLong("lastRunAt", 0).takeIf { it > 0 },
            runCount = obj.optInt("runCount", 0),
            isFavorite = obj.optBoolean("isFavorite", false),
            tags = tags
        )
    }

    private fun persistIndex(scripts: List<Script>) {
        try {
            val array = JSONArray()
            scripts.forEach { array.put(scriptToJson(it)) }
            indexPath.writeText(array.toString(2))
        } catch (e: Exception) {
            // 静默失败，下次加载时恢复
        }
    }
}
