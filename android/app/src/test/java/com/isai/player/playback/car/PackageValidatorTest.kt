package com.isai.player.playback.car

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PackageValidatorTest {

    private val context = mockk<Context>()
    private lateinit var validator: PackageValidator

    @Before
    fun setUp() {
        every { context.packageName } returns "com.isai.player"
        validator = PackageValidator(context)
    }

    @Test
    fun selfPackage_returnsSelfTrustLevel() {
        assertEquals(ClientTrustLevel.SELF, validator.getTrustLevel("com.isai.player"))
        assertEquals(ClientTrustLevel.SELF, validator.getTrustLevel("com.isai.player.debug"))
        assertTrue(validator.isTrusted("com.isai.player"))
        assertTrue(validator.canModifyQueue("com.isai.player"))
    }

    @Test
    fun androidAuto_returnsTrustedAutomotive() {
        assertEquals(ClientTrustLevel.TRUSTED_AUTOMOTIVE, validator.getTrustLevel("com.google.android.projection.gearhead"))
        assertTrue(validator.isTrusted("com.google.android.projection.gearhead"))
        assertTrue(validator.canModifyQueue("com.google.android.projection.gearhead"))
    }

    @Test
    fun googleAssistant_returnsTrustedAssistant() {
        assertEquals(ClientTrustLevel.TRUSTED_ASSISTANT, validator.getTrustLevel("com.google.android.googlequicksearchbox"))
        assertTrue(validator.isTrusted("com.google.android.googlequicksearchbox"))
        assertTrue(validator.canModifyQueue("com.google.android.googlequicksearchbox"))
    }

    @Test
    fun systemAndBluetooth_returnsTrustedSystem() {
        assertEquals(ClientTrustLevel.TRUSTED_SYSTEM, validator.getTrustLevel("com.android.systemui"))
        assertEquals(ClientTrustLevel.TRUSTED_SYSTEM, validator.getTrustLevel("com.android.bluetooth"))
        assertTrue(validator.isTrusted("com.android.systemui"))
        assertFalse(validator.canModifyQueue("com.android.systemui"))
    }

    @Test
    fun unknownThirdParty_returnsUntrustedReadOnly() {
        val unknown = "com.random.mediabrowser"
        assertEquals(ClientTrustLevel.UNTRUSTED_READ_ONLY, validator.getTrustLevel(unknown))
        assertFalse(validator.isTrusted(unknown))
        assertFalse(validator.canModifyQueue(unknown))
    }
}

