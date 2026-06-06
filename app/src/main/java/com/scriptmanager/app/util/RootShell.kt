package com.scriptmanager.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Root Shell 执行器
 * 通过 su 二进制在 root 权限下执行命令
 */
object RootShell {

    /**
     * 检查设备是否已 root
     */
    fun isDeviceRooted(): Boolean {
        return try {
            // 方式1: 检查 su 二进制是否存在
            val suPaths = arrayOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/system/su",
                "/su/bin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/vendor/bin/su"
            )
            suPaths.any { java.io.File(it).exists() } || tryExec("su --version")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 以 root 权限执行命令，返回逐行输出的 Flow
     * @param command 要执行的 shell 命令
     * @param timeoutMs 超时时间（毫秒），0 表示不超时
     */
    fun execute(command: String, timeoutMs: Long = 0): Flow<ShellLine> = flow {
        var process: Process? = null
        try {
            // 使用 su -c 执行命令
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            var hasOutput = false

            // 读取 stdout
            while (stdoutReader.readLine().also { line = it } != null) {
                hasOutput = true
                emit(ShellLine.Stdout(line!!))
            }

            // 读取 stderr
            while (stderrReader.readLine().also { line = it } != null) {
                hasOutput = true
                emit(ShellLine.Stderr(line!!))
            }

            // 等待进程结束
            process.waitFor()
            emit(ShellLine.Exit(process.exitValue()))

        } catch (e: Exception) {
            emit(ShellLine.Stderr("执行错误: ${e.message}"))
            emit(ShellLine.Exit(-1))
        } finally {
            process?.destroy()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 以 root 权限执行命令，直接返回完整结果
     */
    fun executeBlocking(command: String): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            ShellResult(-1, "", e.message ?: "Unknown error")
        }
    }

    /**
     * 尝试执行命令（不要求 root）
     */
    private fun tryExec(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Shell 输出行
 */
sealed class ShellLine {
    data class Stdout(val text: String) : ShellLine()
    data class Stderr(val text: String) : ShellLine()
    data class Exit(val code: Int) : ShellLine()
}

/**
 * Shell 执行结果
 */
data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
    val output: String get() = stdout.ifBlank { stderr }
}
