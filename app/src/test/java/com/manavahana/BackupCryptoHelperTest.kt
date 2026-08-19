package com.manavahana

import com.manavahana.util.BackupCryptoHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupCryptoHelperTest {

    @Test
    fun `random password generation generates unique secure passwords`() {
        val pwd1 = BackupCryptoHelper.generateRandomPassword()
        val pwd2 = BackupCryptoHelper.generateRandomPassword()

        assertEquals(10, pwd1.length)
        assertEquals(10, pwd2.length)
        assertNotEquals(pwd1, pwd2)
    }

    @Test
    fun `backup encryption and decryption with correct password succeeds`() {
        val samplePayload = """{"appName":"ManaVahana","vehicles":[{"id":1,"vehicleName":"Honda City"}]}"""
        val password = BackupCryptoHelper.generateRandomPassword()

        val encrypted = BackupCryptoHelper.encryptBackup(samplePayload, password)
        assertTrue(BackupCryptoHelper.isPasswordEncryptedBackup(encrypted))

        val decryptResult = BackupCryptoHelper.decryptBackup(encrypted, password)
        assertTrue(decryptResult is BackupCryptoHelper.DecryptResult.Success)
        val successResult = decryptResult as BackupCryptoHelper.DecryptResult.Success
        assertEquals(samplePayload, successResult.plainJson)
    }

    @Test
    fun `backup decryption with incorrect password fails securely`() {
        val samplePayload = """{"appName":"ManaVahana","vehicles":[{"id":1,"vehicleName":"Honda City"}]}"""
        val correctPassword = BackupCryptoHelper.generateRandomPassword()
        val wrongPassword = "WrongPassword123!"

        val encrypted = BackupCryptoHelper.encryptBackup(samplePayload, correctPassword)
        val decryptResult = BackupCryptoHelper.decryptBackup(encrypted, wrongPassword)

        assertTrue(decryptResult is BackupCryptoHelper.DecryptResult.InvalidPassword)
    }
}
