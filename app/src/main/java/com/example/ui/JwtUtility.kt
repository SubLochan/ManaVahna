package com.example.ui

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object JwtUtility {
    private const val SECRET = "ManaVahanaSuperSecretJWTKey2026!#"

    // Base64url encoding (no padding, compact)
    fun base64UrlEncode(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING).trim()
    }

    fun base64UrlDecode(input: String): ByteArray {
        return Base64.decode(input, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    fun generateToken(email: String, name: String, userId: Int): String {
        val header = """{"alg":"HS256","typ":"JWT"}"""
        // expiry: 7 days from now (in seconds)
        val exp = (System.currentTimeMillis() / 1000) + (7 * 24 * 60 * 60)
        val payload = """{"sub":"$userId","email":"$email","name":"$name","exp":$exp}"""

        val base64Header = base64UrlEncode(header.toByteArray(Charsets.UTF_8))
        val base64Payload = base64UrlEncode(payload.toByteArray(Charsets.UTF_8))

        val dataToSign = "$base64Header.$base64Payload"
        val signatureBytes = hmacSha256(dataToSign, SECRET)
        val base64Signature = base64UrlEncode(signatureBytes)

        return "$dataToSign.$base64Signature"
    }

    fun decodeAndVerify(token: String): DecodedToken? {
        val parts = token.split(".")
        if (parts.size != 3) return null

        val base64Header = parts[0]
        val base64Payload = parts[1]
        val base64Signature = parts[2]

        // verify signature
        val dataToSign = "$base64Header.$base64Payload"
        val expectedSigBytes = hmacSha256(dataToSign, SECRET)
        val expectedSig = base64UrlEncode(expectedSigBytes)

        if (expectedSig != base64Signature) {
            return null // Invalid signature
        }

        return try {
            // decode payload
            val payloadJson = String(base64UrlDecode(base64Payload), Charsets.UTF_8)
            
            // Extract fields from simple manual JSON parsing
            val sub = extractField(payloadJson, "sub") ?: ""
            val email = extractField(payloadJson, "email") ?: ""
            val name = extractField(payloadJson, "name") ?: ""
            val expStr = extractField(payloadJson, "exp") ?: "0"
            val exp = expStr.toLongOrNull() ?: 0L

            // Verify expiration
            val currentTimeInSecs = System.currentTimeMillis() / 1000
            if (currentTimeInSecs > exp) {
                return null // Token expired
            }

            DecodedToken(userId = sub.toIntOrNull() ?: 0, email = email, name = name, exp = exp, rawJson = payloadJson)
        } catch (e: Exception) {
            null
        }
    }

    private fun hmacSha256(data: String, secret: String): ByteArray {
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun extractField(json: String, field: String): String? {
        val pattern = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        val match = pattern.find(json)
        if (match != null) {
            return match.groupValues[1]
        }
        val numPattern = """"$field"\s*:\s*([0-9]+)""".toRegex()
        val numMatch = numPattern.find(json)
        return numMatch?.groupValues[1]
    }
}

data class DecodedToken(
    val userId: Int,
    val email: String,
    val name: String,
    val exp: Long,
    val rawJson: String
)
