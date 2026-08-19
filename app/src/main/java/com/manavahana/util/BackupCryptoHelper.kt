package com.manavahana.util

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoHelper {

    private const val MAGIC_HEADER = "MANAVAHANA_ENCRYPTED_BACKUP"
    private const val BACKUP_VERSION = 2
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 16

    private val secureRandom = SecureRandom()
    private const val PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789#@!$"

    /**
     * Generates a strong, random password for backup encryption.
     */
    fun generateRandomPassword(length: Int = 10): String {
        val sb = StringBuilder(length)
        // Ensure at least one uppercase, lowercase, digit, and special char
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val special = "#@!$"

        sb.append(upper[secureRandom.nextInt(upper.length)])
        sb.append(lower[secureRandom.nextInt(lower.length)])
        sb.append(digits[secureRandom.nextInt(digits.length)])
        sb.append(special[secureRandom.nextInt(special.length)])

        for (i in 4 until length) {
            sb.append(PASSWORD_CHARS[secureRandom.nextInt(PASSWORD_CHARS.length)])
        }

        // Shuffle the characters
        val charList = sb.toString().toList().shuffled(secureRandom)
        return charList.joinToString("")
    }

    /**
     * Encrypts the backup JSON payload with a user-specified or randomly generated password.
     */
    fun encryptBackup(plainJson: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)

        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val cipherBytes = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        val root = JSONObject()
        root.put("magic", MAGIC_HEADER)
        root.put("version", BACKUP_VERSION)
        root.put("iterations", ITERATION_COUNT)
        root.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
        root.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
        root.put("cipherText", Base64.encodeToString(cipherBytes, Base64.NO_WRAP))

        return root.toString(2)
    }

    sealed interface DecryptResult {
        data class Success(val plainJson: String) : DecryptResult
        object InvalidPassword : DecryptResult
        data class Error(val message: String) : DecryptResult
    }

    /**
     * Inspects if the content is an encrypted backup requiring a password.
     */
    fun isPasswordEncryptedBackup(fileContent: String): Boolean {
        val trimmed = fileContent.trim()
        if (trimmed.startsWith("{") && trimmed.contains(MAGIC_HEADER)) {
            return try {
                val json = JSONObject(trimmed)
                json.optString("magic") == MAGIC_HEADER
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    /**
     * Decrypts the backup payload using the entered password.
     */
    fun decryptBackup(encryptedContent: String, password: String): DecryptResult {
        val trimmed = encryptedContent.trim()
        if (!isPasswordEncryptedBackup(trimmed)) {
            // Check if it's plain JSON
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                return try {
                    val root = JSONObject(trimmed)
                    if (root.optString("appName") == "ManaVahana") {
                        DecryptResult.Success(trimmed)
                    } else {
                        DecryptResult.Error("Not a valid ManaVahana backup file.")
                    }
                } catch (e: Exception) {
                    DecryptResult.Error("Corrupted backup file format.")
                }
            }
            // Check if legacy fixed-key encrypted
            return try {
                val keyBytes = "M4n4Vah4naS3cur3".toByteArray(Charsets.UTF_8)
                val ivBytes = "M4n4Vah4naIv2026".toByteArray(Charsets.UTF_8)
                val secretKey = SecretKeySpec(keyBytes, "AES")
                val ivSpec = IvParameterSpec(ivBytes)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                val decoded = Base64.decode(trimmed, Base64.DEFAULT)
                val plain = String(cipher.doFinal(decoded), Charsets.UTF_8)
                DecryptResult.Success(plain)
            } catch (e: Exception) {
                DecryptResult.InvalidPassword
            }
        }

        return try {
            val root = JSONObject(trimmed)
            val salt = Base64.decode(root.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(root.getString("iv"), Base64.NO_WRAP)
            val iterations = root.optInt("iterations", ITERATION_COUNT)
            val cipherBytes = Base64.decode(root.getString("cipherText"), Base64.NO_WRAP)

            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(spec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val plainBytes = cipher.doFinal(cipherBytes)
            val plainJson = String(plainBytes, Charsets.UTF_8)

            // Validate decrypted JSON structure
            val decryptedRoot = JSONObject(plainJson)
            if (decryptedRoot.optString("appName") == "ManaVahana") {
                DecryptResult.Success(plainJson)
            } else {
                DecryptResult.InvalidPassword
            }
        } catch (e: javax.crypto.BadPaddingException) {
            DecryptResult.InvalidPassword
        } catch (e: javax.crypto.IllegalBlockSizeException) {
            DecryptResult.InvalidPassword
        } catch (e: Exception) {
            DecryptResult.InvalidPassword
        }
    }
}
