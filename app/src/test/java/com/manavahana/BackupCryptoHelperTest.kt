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
    fun `generate sample encrypted mock backup`() {
        val plainMockJson = """
{
  "appName": "ManaVahana",
  "version": 1,
  "timestamp": 1724068800000,
  "vehicles": [
    {
      "id": 1,
      "vehicleName": "My Royal Enfield Classic",
      "vehicleNumber": "AP 09 CD 4589",
      "brand": "Royal Enfield",
      "model": "Classic 350",
      "vehicleType": "Bike",
      "fuelType": "Petrol",
      "purchaseDate": 1672531199000,
      "insuranceExpiry": 1767139200000,
      "pollutionExpiry": 1751328000000,
      "vehicleImage": ""
    }
  ],
  "serviceLogs": [],
  "fuelLogs": [],
  "expenses": [],
  "documents": [],
  "reminders": []
}
        """.trimIndent()

        val password = "ManaVahana#2026"
        val encrypted = BackupCryptoHelper.encryptBackup(plainMockJson, password)
        assertTrue(BackupCryptoHelper.isPasswordEncryptedBackup(encrypted))

        val verified = BackupCryptoHelper.decryptBackup(encrypted, password)
        assertTrue(verified is BackupCryptoHelper.DecryptResult.Success)
    }
}
