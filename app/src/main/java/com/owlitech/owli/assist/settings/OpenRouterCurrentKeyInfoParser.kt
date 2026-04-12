package com.owlitech.owli.assist.settings

import org.json.JSONObject

object OpenRouterCurrentKeyInfoParser {
    fun parse(body: String): OpenRouterCurrentKeyInfo? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val data = root.optJSONObject("data") ?: return null
        return OpenRouterCurrentKeyInfo(
            label = data.optNullableString("label"),
            limit = data.optNullableDouble("limit"),
            limitRemaining = data.optNullableDouble("limit_remaining"),
            limitReset = data.optNullableString("limit_reset"),
            usage = data.optNullableDouble("usage"),
            usageDaily = data.optNullableDouble("usage_daily"),
            usageWeekly = data.optNullableDouble("usage_weekly"),
            usageMonthly = data.optNullableDouble("usage_monthly"),
            isFreeTier = data.optNullableBoolean("is_free_tier"),
            expiresAt = data.optNullableString("expires_at")
        )
    }

    private fun JSONObject.optNullableString(name: String): String? {
        return optString(name, "").trim().ifEmpty { null }
    }

    private fun JSONObject.optNullableDouble(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return optDouble(name).takeUnless { it.isNaN() }
    }

    private fun JSONObject.optNullableBoolean(name: String): Boolean? {
        if (!has(name) || isNull(name)) return null
        return optBoolean(name)
    }
}
