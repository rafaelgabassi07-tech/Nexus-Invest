package com.example.utils

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream

object DataImportExportUtils {

    fun parseXlsx(inputStream: InputStream): String {
        try {
            val zip = ZipInputStream(inputStream)
            var sharedStrings = listOf<String>()
            var sheetXml = ""
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    val content = zip.reader().readText()
                    // More robust shared strings parsing
                    val siRegex = "<si>(.*?)</si>".toRegex(RegexOption.DOT_MATCHES_ALL)
                    val tRegex = "<t[^>]*>(.*?)</t>".toRegex(RegexOption.DOT_MATCHES_ALL)
                    sharedStrings = siRegex.findAll(content).map { siMatch ->
                        val tMatches = tRegex.findAll(siMatch.groupValues[1])
                        tMatches.joinToString("") { it.groupValues[1] }.unescapeXml()
                    }.toList()
                } else if (entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml")) {
                    sheetXml = zip.reader().readText()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            zip.close()

            if (sheetXml.isEmpty()) return ""

            val rows = mutableListOf<List<String>>()
            val rowRegex = "<row[^>]*>(.*?)</row>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val cellRegex = "<c[^>]*>(.*?)</c>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val vRegex = "<v>(.*?)</v>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val rAttrRegex = "r=\"([A-Z]+)[0-9]+\"".toRegex()

            for (rowMatch in rowRegex.findAll(sheetXml)) {
                val rowContent = rowMatch.groupValues[1]
                val cellMap = mutableMapOf<Int, String>()
                var maxCol = 0
                
                for (cellMatch in cellRegex.findAll(rowContent)) {
                    val cellFull = cellMatch.groupValues[0]
                    val cellInner = cellMatch.groupValues[1]
                    
                    // Identify column index from 'r' attribute
                    val rMatch = rAttrRegex.find(cellFull)
                    val colIndex = if (rMatch != null) {
                        excelColToIndex(rMatch.groupValues[1])
                    } else {
                        -1
                    }

                    val cellRawValMatch = vRegex.find(cellFull)
                    var cellValue = ""
                    if (cellRawValMatch != null) {
                        val rawVal = cellRawValMatch.groupValues[1]
                        if (cellFull.contains("t=\"s\"") || cellFull.contains("t='s'")) {
                            val idx = rawVal.toIntOrNull()
                            if (idx != null && idx in sharedStrings.indices) {
                                cellValue = sharedStrings[idx]
                            }
                        } else {
                            cellValue = rawVal
                        }
                    } else {
                        val inlineRegex = "<t[^>]*>(.*?)</t>".toRegex()
                        val inlineMatch = inlineRegex.find(cellFull)
                        if (inlineMatch != null) {
                            cellValue = inlineMatch.groupValues[1].unescapeXml()
                        }
                    }
                    
                    if (colIndex != -1) {
                        cellMap[colIndex] = cellValue
                        if (colIndex > maxCol) maxCol = colIndex
                    }
                }
                
                if (cellMap.isNotEmpty()) {
                    val finalRow = MutableList(maxCol + 1) { "" }
                    cellMap.forEach { (idx, value) -> finalRow[idx] = value }
                    rows.add(finalRow)
                }
            }

            return rows.joinToString("\n") { r -> r.joinToString(";") }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun excelColToIndex(col: String): Int {
        var result = 0
        for (c in col) {
            result = result * 26 + (c - 'A' + 1)
        }
        return result - 1
    }

    private fun String.unescapeXml(): String {
        return this.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "arquivo"
    }
}
