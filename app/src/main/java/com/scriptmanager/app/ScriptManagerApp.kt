package com.scriptmanager.app

import android.app.Application
import com.scriptmanager.app.model.ScriptRepository
import com.scriptmanager.app.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScriptManagerApp : Application() {

    // App-level singletons
    val scriptRepository by lazy { ScriptRepository(this) }
    val preferencesManager by lazy { PreferencesManager(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Load scripts on startup
        applicationScope.launch {
            scriptRepository.loadScripts()
        }
    }

    companion object {
        lateinit var instance: ScriptManagerApp
            private set
    }
}
