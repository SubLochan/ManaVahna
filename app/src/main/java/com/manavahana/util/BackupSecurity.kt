package com.manavahana.util

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility for AES-256-GCM Password-Protected Backup Encryption & Decryption.
 *
 * Uses:
 * - PBKDF2WithHmacSHA256 with 65,536 iterations for key derivation
 * - 16-byte cryptographically secure random salt
 * - 12-byte random IV
 * - AES-256 in GCM mode with 128-bit authentication tag (ensuring confidentiality & tamper-proofing)
 */
object BackupSecurity {

    const val FORMAT_HEADER_V2 = "MANAVAHANA_ENCRYPTED_V2"
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16
    private const val GCM_IV_LENGTH_BYTES = 12

    /**
     * Generates a secure, readable random password for each export.
     * Excludes easily confused characters (like 0/O, 1/I/l).
     */
    fun generateRandomPassword(length: Int = 8): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz"
        val random = SecureRandom()
        val sb = java.lang.StringBuilder(length)
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    sealed class BackupType {
        data class EncryptedV2(val timestamp: Long) : BackupType()
        object EncryptedLegacy : BackupType()
        object PlainJson : BackupType()
        object Invalid : BackupType()
    }

    /**
     * Inspects the backup payload to determine its encryption format.
     */
    fun inspect(content: String): BackupType {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return BackupType.Invalid

        // Check for V2 JSON envelope
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = JSONObject(trimmed)
                if (json.optString("format") == FORMAT_HEADER_V2) {
                    return BackupType.EncryptedV2(json.optLong("timestamp", System.currentTimeMillis()))
                }
                if (json.optString("appName") == "ManaVahana" || json.has("vehicles")) {
                    return BackupType.PlainJson
                }
            } catch (e: Exception) {
                // Not a valid JSON object
            }
        }

        // Check if it looks like legacy Base64 AES-CBC
        if (!trimmed.contains(" ") && trimmed.length > 30) {
            try {
                val decoded = Base64.decode(trimmed, Base64.DEFAULT)
                if (decoded.isNotEmpty()) {
                    return BackupType.EncryptedLegacy
                }
            } catch (e: Exception) {
                // Not valid base64
            }
        }

        return BackupType.Invalid
    }

    /**
     * Encrypts the plain backup JSON with AES-256-GCM using the provided password.
     */
    fun encrypt(plainText: String, password: String): String {
        require(password.isNotEmpty()) { "Password cannot be empty for encryption" }

        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }

        // Derive 256-bit AES key from password + salt using PBKDF2
        val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // Encrypt with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Build container JSON
        val envelope = JSONObject().apply {
            put("format", FORMAT_HEADER_V2)
            put("algorithm", "AES-256-GCM")
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iterations", ITERATIONS)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            put("timestamp", System.currentTimeMillis())
            put("version", 2)
        }

        return envelope.toString(2)
    }

    /**
     * Decrypts the backup payload using the provided password.
     * Throws an exception if the password is wrong or ciphertext is corrupted.
     */
    fun decrypt(encryptedContent: String, password: String): String {
        val trimmed = encryptedContent.trim()
        val type = inspect(trimmed)

        when (type) {
            is BackupType.EncryptedV2 -> {
                val json = JSONObject(trimmed)
                val salt = Base64.decode(json.getString("salt"), Base64.DEFAULT)
                val iv = Base64.decode(json.getString("iv"), Base64.DEFAULT)
                val ciphertext = Base64.decode(json.getString("ciphertext"), Base64.DEFAULT)
                val iterations = json.optInt("iterations", ITERATIONS)

                // Derive key
                val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val keyBytes = factory.generateSecret(keySpec).encoded
                val secretKey = SecretKeySpec(keyBytes, "AES")

                // Decrypt with AES-GCM
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
                val plainBytes = cipher.doFinal(ciphertext)
                return String(plainBytes, Charsets.UTF_8)
            }
            is BackupType.EncryptedLegacy -> {
                // Decrypt with legacy hardcoded key fallback
                val keyBytes = "M4n4Vah4naS3cur3".toByteArray(Charsets.UTF_8)
                val ivBytes = "M4n4Vah4naIv2026".toByteArray(Charsets.UTF_8)
                val secretKey = SecretKeySpec(keyBytes, "AES")
                val ivSpec = IvParameterSpec(ivBytes)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                val decoded = Base64.decode(trimmed, Base64.DEFAULT)
                return String(cipher.doFinal(decoded), Charsets.UTF_8)
            }
            is BackupType.PlainJson -> {
                return trimmed
            }
            is BackupType.Invalid -> {
                throw IllegalArgumentException("Unrecognized or corrupted backup format")
            }
        }
    }
}
