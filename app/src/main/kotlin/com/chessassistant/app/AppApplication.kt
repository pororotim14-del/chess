package com.chessassistant.app

import android.app.Application
import com.chessassistant.security.SecurityManager
import com.chessassistant.security.engine.EngineSecurityManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppApplication : Application() {

    @Inject
    lateinit var securityManager: SecurityManager

    private lateinit var engineSecurityManager: EngineSecurityManager

    override fun onCreate() {
        super.onCreate()
        engineSecurityManager = EngineSecurityManager.getInstance(applicationContext, securityManager)
    }

    fun getEngineSecurityManager(): EngineSecurityManager = engineSecurityManager
}