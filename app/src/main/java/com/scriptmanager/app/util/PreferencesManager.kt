package com.scriptmanager.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * App 偏好设置管理器
 */
class PreferencesManager(private val context: Context) {

    companion object {
        private val DEFAULT_SCRIPT_DIR = stringPreferencesKey("default_script_dir")
        private val CONFIRM_BEFORE_RUN = booleanPreferencesKey("confirm_before_run")
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val AUTO_SAVE_ON_RUN = booleanPreferencesKey("auto_save_on_run")
        private val MAX_OUTPUT_LINES = intPreferencesKey("max_output_lines")
    }

    /**
     * 默认脚本目录
     */
    val defaultScriptDir: Flow<String> = context.preferencesDataStore.data.map { prefs ->
        prefs[DEFAULT_SCRIPT_DIR] ?: "/sdcard/scripts"
    }

    suspend fun setDefaultScriptDir(dir: String) {
        context.preferencesDataStore.edit { it[DEFAULT_SCRIPT_DIR] = dir }
    }

    /**
     * 执行前确认
     */
    val confirmBeforeRun: Flow<Boolean> = context.preferencesDataStore.data.map { prefs ->
        prefs[CONFIRM_BEFORE_RUN] ?: true
    }

    suspend fun setConfirmBeforeRun(confirm: Boolean) {
        context.preferencesDataStore.edit { it[CONFIRM_BEFORE_RUN] = confirm }
    }

    /**
     * 执行时保持屏幕常亮
     */
    val keepScreenOn: Flow<Boolean> = context.preferencesDataStore.data.map { prefs ->
        prefs[KEEP_SCREEN_ON] ?: true
    }

    suspend fun setKeepScreenOn(keep: Boolean) {
        context.preferencesDataStore.edit { it[KEEP_SCREEN_ON] = keep }
    }

    /**
     * 运行时自动保存
     */
    val autoSaveOnRun: Flow<Boolean> = context.preferencesDataStore.data.map { prefs ->
        prefs[AUTO_SAVE_ON_RUN] ?: true
    }

    suspend fun setAutoSaveOnRun(autoSave: Boolean) {
        context.preferencesDataStore.edit { it[AUTO_SAVE_ON_RUN] = autoSave }
    }

    /**
     * 最大输出行数
     */
    val maxOutputLines: Flow<Int> = context.preferencesDataStore.data.map { prefs ->
        prefs[MAX_OUTPUT_LINES] ?: 5000
    }

    suspend fun setMaxOutputLines(lines: Int) {
        context.preferencesDataStore.edit { it[MAX_OUTPUT_LINES] = lines }
    }
}
