package com.manavahana.ui

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object PlayStoreVersionFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetches the latest published version name directly from Google Play Store.
     * Uses multiple resilient parsers to extract the version metadata.
     */
    suspend fun fetchVersion(packageName: String): String? = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://play.google.com/store/apps/details?id=$packageName&hl=en&gl=US",
            "https://play.google.com/store/apps/details?id=$packageName&hl=en",
            "https://play.google.com/store/apps/details?id=$packageName"
        )

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code == 404) {
                            Log.d("PlayStoreVersionFetcher", "Play Store public listing not found (404) for $packageName. App may be unreleased or in closed testing.")
                        } else {
                            Log.w("PlayStoreVersionFetcher", "Failed to load Play Store page: code ${response.code}")
                        }
                        return@use
                    }
                    val html = response.body?.string() ?: return@use
                    
                    val extracted = parseVersionFromHtml(html)
                    if (!extracted.isNullOrBlank()) {
                        Log.d("PlayStoreVersionFetcher", "Successfully fetched Play Store version: $extracted from $url")
                        return@withContext extracted
                    }
                }
            } catch (e: Exception) {
                Log.w("PlayStoreVersionFetcher", "Error fetching Play Store version from $url: ${e.message}")
            }
        }
        null
    }

    private fun parseVersionFromHtml(html: String): String? {
        // Parser 1: Schema JSON-LD structured data ("softwareVersion":"X.Y.Z")
        val schemaPattern = Pattern.compile("\"softwareVersion\"\\s*:\\s*\"([^\"]+)\"")
        val schemaMatcher = schemaPattern.matcher(html)
        if (schemaMatcher.find()) {
            val version = cleanVersionString(schemaMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 2: HTML itemprop tag
        val inlineJsonPattern = Pattern.compile("itemprop=\"softwareVersion\"[^>]*>\\s*([^<]+)\\s*<")
        val inlineMatcher = inlineJsonPattern.matcher(html)
        if (inlineMatcher.find()) {
            val version = cleanVersionString(inlineMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 3: "About this app" section with "Version" title
        val aboutVersionPattern = Pattern.compile("(?i)Version</div>\\s*<div[^>]*>\\s*([0-9]+(?:\\.[0-9]+)+(?:-[a-zA-Z0-9.]+)?)\\s*</div>")
        val aboutVersionMatcher = aboutVersionPattern.matcher(html)
        if (aboutVersionMatcher.find()) {
            val version = cleanVersionString(aboutVersionMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 4: Play Store metadata class e.g. class="reAt0">1.0.2</div>
        val reAt0Pattern = Pattern.compile("class=\"reAt0\">\\s*([^<]+)\\s*<")
        val reAt0Matcher = reAt0Pattern.matcher(html)
        if (reAt0Matcher.find()) {
            val version = cleanVersionString(reAt0Matcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 5: Play Store metadata class e.g. class="wVqvd">1.0.2</div>
        val wVqvdPattern = Pattern.compile("class=\"wVqvd\">\\s*([^<]+)\\s*<")
        val wVqvdMatcher = wVqvdPattern.matcher(html)
        if (wVqvdMatcher.find()) {
            val version = cleanVersionString(wVqvdMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 6: Nested data arrays in Play Store JS chunks e.g. [[["1.0.2"]]]
        val initDataPattern = Pattern.compile("\\[\\[\\[\"([\\d]+\\.[\\d]+(?:\\.[\\d]+)?(?:-[a-zA-Z0-9.]+)?)\"\\]\\]\\]")
        val initDataMatcher = initDataPattern.matcher(html)
        if (initDataMatcher.find()) {
            val version = cleanVersionString(initDataMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 7: 2-bracket JS data chunk e.g. [["1.0.2"]]
        val twoBracketPattern = Pattern.compile("\\[\\[\"([\\d]+\\.[\\d]+(?:\\.[\\d]+)?(?:-[a-zA-Z0-9.]+)?)\"\\]")
        val twoBracketMatcher = twoBracketPattern.matcher(html)
        if (twoBracketMatcher.find()) {
            val version = cleanVersionString(twoBracketMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 8: AF_initDataCallback key-value pairs e.g. "softwareVersion","1.0.2"
        val altSchemaPattern = Pattern.compile("\"softwareVersion\",\\s*\"([^\"]+)\"")
        val altSchemaMatcher = altSchemaPattern.matcher(html)
        if (altSchemaMatcher.find()) {
            val version = cleanVersionString(altSchemaMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 9: "versionName":"1.0.2"
        val versionNamePattern = Pattern.compile("\"versionName\"\\s*:\\s*\"([^\"]+)\"")
        val versionNameMatcher = versionNamePattern.matcher(html)
        if (versionNameMatcher.find()) {
            val version = cleanVersionString(versionNameMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        // Parser 10: Legacy Play Store detail table format
        val legacyPattern = Pattern.compile("(?:Current Version|Version)[\\s\\S]{0,100}?class=\"htlgb\">([^<]+)<")
        val legacyMatcher = legacyPattern.matcher(html)
        if (legacyMatcher.find()) {
            val version = cleanVersionString(legacyMatcher.group(1))
            if (!version.isNullOrEmpty()) return version
        }

        return null
    }

    private fun cleanVersionString(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().removePrefix("v").removePrefix("V").trim()
        // Ensure it contains at least one digit and period or numeric structure
        if (cleaned.any { it.isDigit() } && (cleaned.contains(".") || cleaned.all { it.isDigit() })) {
            return cleaned
        }
        return null
    }

    /**
     * Determines whether [latest] is strictly newer than [current].
     * Handles semantic versions (e.g. 1.0.2 vs 1.0.1), different segment lengths (1.1 vs 1.0.9),
     * and non-numeric suffixes or build strings.
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        if (latest.isBlank() || latest == "Retrieving..." || latest == "Not checked yet" || latest == "Not published yet") return false
        if (current.isBlank()) return true
        if (current.trim() == latest.trim()) return false

        try {
            val currClean = current.trim().removePrefix("v").removePrefix("V")
            val lateClean = latest.trim().removePrefix("v").removePrefix("V")

            val currParts = currClean.split(".").map { part ->
                val digits = part.takeWhile { it.isDigit() }
                digits.toIntOrNull() ?: 0
            }
            val lateParts = lateClean.split(".").map { part ->
                val digits = part.takeWhile { it.isDigit() }
                digits.toIntOrNull() ?: 0
            }

            val maxLen = maxOf(currParts.size, lateParts.size)
            for (i in 0 until maxLen) {
                val currVal = currParts.getOrNull(i) ?: 0
                val lateVal = lateParts.getOrNull(i) ?: 0
                if (lateVal > currVal) return true
                if (currVal > lateVal) return false
            }
            return false
        } catch (e: Exception) {
            return latest.trim() != current.trim()
        }
    }
}

