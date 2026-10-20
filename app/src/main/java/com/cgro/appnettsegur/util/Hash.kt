package com.cgro.appnettsegur.util

import java.io.File
import java.security.MessageDigest

object Hash {
    /** SHA-256 del archivo en hexadecimal, leyendo por bloques para no cargarlo entero en memoria. */
    fun sha256(archivo: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        archivo.inputStream().use { entrada ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val leidos = entrada.read(buffer)
                if (leidos == -1) break
                digest.update(buffer, 0, leidos)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
