package com.minijarvis

import com.minijarvis.security.ConfirmationManager
import com.minijarvis.security.SecurityLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfirmationManagerTest {

    private lateinit var confirmationManager: ConfirmationManager

    @Before
    fun setup() {
        confirmationManager = ConfirmationManager()
    }

    @Test
    fun requiresConfirmation_level1_returnsFalse() {
        assertFalse(confirmationManager.requiresConfirmation(SecurityLevel.LEVEL_1_SAFE))
    }

    @Test
    fun requiresConfirmation_level2_defaultReturnsTrue() {
        assertTrue(confirmationManager.requiresConfirmation(SecurityLevel.LEVEL_2_PERSONAL))
    }

    @Test
    fun requiresConfirmation_level2_bypassReturnsFalse() {
        assertFalse(confirmationManager.requiresConfirmation(SecurityLevel.LEVEL_2_PERSONAL, userBypassPersonal = true))
    }

    @Test
    fun requiresConfirmation_level3_alwaysReturnsTrue() {
        assertTrue(confirmationManager.requiresConfirmation(SecurityLevel.LEVEL_3_SENSITIVE, userBypassPersonal = true))
    }
}
