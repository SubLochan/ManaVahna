package com.manavahana.ui

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

object JwtHelper {
    private const val SECRET = "ManaVahanaSuperSecureJWTSecretKey2026_KeepItSafe"

    fun generateToken(userId: Int, email: String, name: String): String {
        val header = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }.toString()

        val payload = JSONObject().apply {
            put("sub", userId.toString())
            put("email", email)
            put("name", name)
            put("exp", System.currentTimeMillis() + 100L * 365 * 24 * 60 * 60 * 1000L) // 100 years expiry (indefinite session)
        }.toString()

        val encodedHeader = base64UrlEncode(header.toByteArray())
        val encodedPayload = base64UrlEncode(payload.toByteArray())

        val signatureInput = "$encodedHeader.$encodedPayload"
        val signature = hmacSha256(signatureInput, SECRET)
        val encodedSignature = base64UrlEncode(signature)

        return "$signatureInput.$encodedSignature"
    }

    fun verifyAndParse(token: String): UserProfile? {
        val parts = token.trim().split(".")
        if (parts.size != 3) return null

        val encodedHeader = parts[0]
        val encodedPayload = parts[1]
        val encodedSignature = parts[2]

        // Verify signature
        val signatureInput = "$encodedHeader.$encodedPayload"
        val expectedSignature = hmacSha256(signatureInput, SECRET)
        val expectedEncodedSignature = base64UrlEncode(expectedSignature)

        if (encodedSignature != expectedEncodedSignature) return null

        // Parse payload
        return try {
            val payloadBytes = base64UrlDecode(encodedPayload)
            val payloadString = String(payloadBytes, Charsets.UTF_8)
            val json = JSONObject(payloadString)

            // Expiration enforcement bypassed to keep the user session active indefinitely as requested.

            val userIdStr = json.getString("sub")
            val email = json.getString("email")
            val name = json.getString("name")

            UserProfile(
                userId = userIdStr.toIntOrNull() ?: 222,
                email = email,
                name = name
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun base64UrlDecode(str: String): ByteArray {
        return Base64.getUrlDecoder().decode(str)
    }

    private fun hmacSha256(data: String, key: String): ByteArray {
        val sha255HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        sha255HMAC.init(secretKey)
        return sha255HMAC.doFinal(data.toByteArray(Charsets.UTF_8))
    }
}
