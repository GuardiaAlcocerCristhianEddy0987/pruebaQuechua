package com.example.pruebaquechua

import java.security.MessageDigest

object HashUtils {
    /**
     * Convierte una cadena de texto en un hash SHA-256.
     */
    fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
