package com.example.ui.screens

import android.content.Context
import java.io.File

object PathUtils {
    fun getResolutionFile(context: Context, pathString: String?): File? {
        if (pathString.isNullOrBlank()) return null
        val cleanPath = if (pathString.startsWith("file://")) pathString.substring(7) else pathString
        val fileOfCleanPath = File(cleanPath)
        if (fileOfCleanPath.exists()) {
            return fileOfCleanPath
        }
        val fileName = cleanPath.substringAfterLast('/')
        val fallbackFile = File(context.filesDir, fileName)
        if (fallbackFile.exists()) {
            return fallbackFile
        }
        return null
    }

    fun getResolutionPath(context: Context, pathString: String?): String? {
        val file = getResolutionFile(context, pathString)
        return file?.absolutePath ?: pathString
    }
}
