package com.manavahana.ui

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

object PlayStoreVersionFetcher {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    /**
     * Fetches the latest published version name from the Play Store webpage.
     * Uses a highly reliable schema regex parser to extract the "softwareVersion" metadata field.
     */
    suspend fun fetchVersion(packageName: String): String? = withContext(Dispatchers.IO) {
        val url = "https://play.google.com/store/apps/details?id=$packageName&hl=en&gl=US"
        try {
            val request = Request.Builder()
                .url(url)
                // Set a modern browser User-Agent to avoid getting bot-prevented pages or older structures
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("PlayStoreVersionFetcher", "Failed to load Play Store page: code ${response.code}")
                    return@withContext null
                }
                val html = response.body?.string() ?: return@withContext null
                
                // Primary Parser: HTML Schema structured JSON-LD (Search engines rely heavily on this)
                // Looks for: "softwareVersion":"X.Y.Z"
                val schemaPattern = Pattern.compile("\"softwareVersion\"\\s*:\\s*\"([^\"]+)\"")
                val schemaMatcher = schemaPattern.matcher(html)
                if (schemaMatcher.find()) {
                    val version = schemaMatcher.group(1)?.trim()
                    if (!version.isNullOrEmpty()) {
                        Log.d("PlayStoreVersionFetcher", "Extracted version via JSON-LD softwareVersion: $version")
                        return@withContext version
                    }
                }

                // Secondary Parser: search inside nested JSON string structures looking for potential version names
                val inlineJsonPattern = Pattern.compile("itemprop=\"softwareVersion\"[^>]*>\\s*([^<]+)\\s*<")
                val inlineMatcher = inlineJsonPattern.matcher(html)
                if (inlineMatcher.find()) {
                    val version = inlineMatcher.group(1)?.trim()
                    if (!version.isNullOrEmpty()) {
                        Log.d("PlayStoreVersionFetcher", "Extracted version via itemprop tag: $version")
                        return@withContext version
                    }
                }

                // Tertiary Parser: Check Play Store's client JS states if those structures are altered
                val initDataPattern = Pattern.compile("\\[\\[\\[\"([\\d]+\\.[\\d]+(?:\\.[\\d]+)?(?:-[a-zA-Z0-9.]+)?)\"\\]\\]\\]")
                val initDataMatcher = initDataPattern.matcher(html)
                if (initDataMatcher.find()) {
                    val version = initDataMatcher.group(1)?.trim()
                    if (!version.isNullOrEmpty()) {
                        Log.d("PlayStoreVersionFetcher", "Extracted version via nested data array: $version")
                        return@withContext version
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayStoreVersionFetcher", "Error scraping Play Store version", e)
        }
        null
    }
}
