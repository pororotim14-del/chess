package com.chessassistant.app.di

import android.os.Build
import com.chessassistant.security.AndroidKeyStoreSecretStorage
import com.chessassistant.security.DeviceFingerprint
import com.chessassistant.security.SecretStorage
import com.chessassistant.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecretStorage(): SecretStorage = AndroidKeyStoreSecretStorage()

    @Provides
    @Singleton
    fun provideDeviceFingerprint(): DeviceFingerprint =
        DeviceFingerprint(
            model = Build.MODEL,
            board = Build.BOARD,
            manufacturer = Build.MANUFACTURER,
            bootId = readBootId(),
        )

    @Provides
    @Singleton
    fun provideSecurityManager(
        fingerprint: DeviceFingerprint,
        storage: SecretStorage,
    ): SecurityManager = SecurityManager(fingerprint, storage)

    private fun readBootId(): String? = try {
        java.io.File("/proc/sys/kernel/random/boot_id")
            .takeIf { it.exists() }
            ?.readText()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}