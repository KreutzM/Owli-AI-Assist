package com.owlitech.owli.assist.settings

import java.util.Base64

data class EncryptedOpenRouterUserKeyBlob(
    val version: Int,
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    fun encode(): String {
        return listOf(
            version.toString(),
            iv.encodeBase64(),
            ciphertext.encodeBase64()
        ).joinToString(SEPARATOR)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedOpenRouterUserKeyBlob) return false
        return version == other.version &&
            iv.contentEquals(other.iv) &&
            ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }

    companion object {
        private const val SEPARATOR = ":"

        fun decodeOrNull(encoded: String): EncryptedOpenRouterUserKeyBlob? {
            val parts = encoded.split(SEPARATOR)
            if (parts.size != 3) return null
            val version = parts[0].toIntOrNull() ?: return null
            val iv = parts[1].decodeBase64OrNull() ?: return null
            val ciphertext = parts[2].decodeBase64OrNull() ?: return null
            if (iv.isEmpty() || ciphertext.isEmpty()) return null
            return EncryptedOpenRouterUserKeyBlob(
                version = version,
                iv = iv,
                ciphertext = ciphertext
            )
        }
    }
}

private fun ByteArray.encodeBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}

private fun String.decodeBase64OrNull(): ByteArray? {
    return runCatching { Base64.getDecoder().decode(this) }.getOrNull()
}
