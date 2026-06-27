package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * Encodes and decodes the TeleCon line protocol.
 *
 * Wire format: `APP:TYPE,key1,value1,key2,value2,...`
 * Lines are terminated with `\n` on the wire (handled by the transport layer).
 */
object LineProtocolCodec {

    fun encode(app: String, type: String, pairs: Map<String, Any>): String {
        if (pairs.isEmpty()) return "$app:$type"
        val body = pairs.entries.joinToString(",") { "${it.key},${it.value}" }
        return "$app:$type,$body"
    }

    fun decode(line: String): EspMessage? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val colonIndex = trimmed.indexOf(':')
        if (colonIndex <= 0) return null

        val app = trimmed.substring(0, colonIndex)
        val remainder = trimmed.substring(colonIndex + 1)
        if (remainder.isEmpty()) return null

        val typeEnd = remainder.indexOf(',')
        val type = if (typeEnd < 0) remainder else remainder.substring(0, typeEnd)
        val pairsPart = if (typeEnd < 0) "" else remainder.substring(typeEnd + 1)

        return EspMessage(
            app = app,
            type = type,
            values = parseKeyValuePairs(pairsPart),
        )
    }

    private fun parseKeyValuePairs(pairsPart: String): Map<String, String> {
        if (pairsPart.isEmpty()) return emptyMap()
        val tokens = pairsPart.split(',')
        if (tokens.size % 2 != 0) return emptyMap()

        return buildMap {
            var index = 0
            while (index < tokens.size - 1) {
                put(tokens[index], tokens[index + 1])
                index += 2
            }
        }
    }
}
