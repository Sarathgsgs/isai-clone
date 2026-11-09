package com.isai.player.playback.car

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ClientTrustLevel {
    SELF,
    TRUSTED_AUTOMOTIVE,
    TRUSTED_ASSISTANT,
    TRUSTED_SYSTEM,
    UNTRUSTED_READ_ONLY
}

@Singleton
class PackageValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val PACKAGE_ANDROID_AUTO = "com.google.android.projection.gearhead"
        const val PACKAGE_ASSISTANT = "com.google.android.googlequicksearchbox"
        const val PACKAGE_SYSTEM_UI = "com.android.systemui"
        const val PACKAGE_BLUETOOTH = "com.android.bluetooth"
        const val PACKAGE_TELECOM = "com.android.server.telecom"
    }

    fun getTrustLevel(packageName: String): ClientTrustLevel {
        if (packageName == context.packageName || packageName.startsWith("com.isai.player")) {
            return ClientTrustLevel.SELF
        }

        return when (packageName) {
            PACKAGE_ANDROID_AUTO -> ClientTrustLevel.TRUSTED_AUTOMOTIVE
            PACKAGE_ASSISTANT -> ClientTrustLevel.TRUSTED_ASSISTANT
            PACKAGE_SYSTEM_UI,
            PACKAGE_BLUETOOTH,
            PACKAGE_TELECOM -> ClientTrustLevel.TRUSTED_SYSTEM
            else -> ClientTrustLevel.UNTRUSTED_READ_ONLY
        }
    }

    fun isTrusted(packageName: String): Boolean {
        return getTrustLevel(packageName) != ClientTrustLevel.UNTRUSTED_READ_ONLY
    }

    fun canModifyQueue(packageName: String): Boolean {
        val level = getTrustLevel(packageName)
        return level == ClientTrustLevel.SELF ||
            level == ClientTrustLevel.TRUSTED_AUTOMOTIVE ||
            level == ClientTrustLevel.TRUSTED_ASSISTANT
    }
}

