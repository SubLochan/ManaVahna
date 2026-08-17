package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.manavahana.R
import com.manavahana.util.BackupSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ManaVahana", appName)
  }

  @Test
  fun `test aes256 backup encryption and decryption`() {
    val rawJson = """{"vehicles":[{"name":"Honda City","regNumber":"TS09AB1234"}]}"""
    val password = "MySecurePassword123!"

    val encryptedPayload = BackupSecurity.encrypt(rawJson, password)
    assertTrue(encryptedPayload.contains("AES-256-GCM"))
    assertTrue(encryptedPayload.contains("PBKDF2WithHmacSHA256"))

    val decryptedJson = BackupSecurity.decrypt(encryptedPayload, password)
    assertEquals(rawJson, decryptedJson)
  }

  @Test
  fun `test random password generation and encryption`() {
    val randomPwd1 = BackupSecurity.generateRandomPassword(8)
    val randomPwd2 = BackupSecurity.generateRandomPassword(8)
    assertEquals(8, randomPwd1.length)
    assertEquals(8, randomPwd2.length)
    assertTrue(randomPwd1.isNotBlank())

    val rawJson = """{"vehicles":[{"name":"Royal Enfield Hunter 350"}]}"""
    val encrypted = BackupSecurity.encrypt(rawJson, randomPwd1)
    val decrypted = BackupSecurity.decrypt(encrypted, randomPwd1)
    assertEquals(rawJson, decrypted)
  }

  @Test
  fun `generate mock encrypted backup json`() {
    val fullJson = """{"vehicles":[{"name":"Honda City ZX","regNumber":"TS09FA1234"}]}"""
    val encrypted = BackupSecurity.encrypt(fullJson, "ManaVahana2026!")
    val decrypted = BackupSecurity.decrypt(encrypted, "ManaVahana2026!")
    assertEquals(fullJson, decrypted)
  }

  @Test(expected = Exception::class)
  fun `test aes256 decryption fails with wrong password`() {
    val rawJson = """{"vehicles":[{"name":"Honda City"}]}"""
    val encryptedPayload = BackupSecurity.encrypt(rawJson, "CorrectPassword")
    BackupSecurity.decrypt(encryptedPayload, "WrongPassword")
  }
}
