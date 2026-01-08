package com.owlitech.owli.assist.util

fun String?.truncateForLog(maxChars: Int = 500): String {
    val value = this ?: return "(null)"
    if (value.length <= maxChars) return value
    return value.take(maxChars) + "... (truncated)"
}

fun logLong(tag: String, prefix: String, text: String?, chunkSize: Int = 3500) {
    val value = text ?: "(null)"
    if (value.length <= chunkSize) {
        AppLogger.d(tag, "$prefix$value")
        return
    }
    val total = (value.length + chunkSize - 1) / chunkSize
    var index = 0
    var chunk = 1
    while (index < value.length) {
        val end = (index + chunkSize).coerceAtMost(value.length)
        val part = value.substring(index, end)
        AppLogger.d(tag, "$prefix[$chunk/$total] $part")
        index = end
        chunk += 1
    }
}
