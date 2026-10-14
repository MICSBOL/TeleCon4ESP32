package com.micsbol.telecon4esp32.domain.bluetooth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LinkTrafficDirection {
    SENT,
    RECEIVED,
}

data class LinkTrafficEntry(
    val id: Long,
    val direction: LinkTrafficDirection,
    val text: String,
    val repeatCount: Int = 0,
    val repeatEpoch: Long = 0L,
)

/**
 * Rolling Bluetooth transcript, oldest at the head and newest at the tail.
 * A line is printed only when the payload differs from the last line in that
 * direction. An identical send or receive moves that row to the tail and bumps
 * [LinkTrafficEntry.repeatCount] so the live edge can blink it.
 */
class LinkTrafficLog(
    private val capacity: Int = 120,
) {
    private val buffer = ArrayDeque<LinkTrafficEntry>()
    private var nextId = 1L
    private val _entries = MutableStateFlow<List<LinkTrafficEntry>>(emptyList())
    val entries: StateFlow<List<LinkTrafficEntry>> = _entries.asStateFlow()

    fun record(direction: LinkTrafficDirection, text: String) {
        val line = text.trim()
        if (line.isEmpty()) return
        synchronized(buffer) {
            val previousIndex = buffer.indexOfLast { it.direction == direction }
            if (previousIndex >= 0 && buffer[previousIndex].text == line) {
                val previous = buffer.removeAt(previousIndex)
                buffer.addLast(
                    previous.copy(
                        repeatCount = previous.repeatCount + 1,
                        repeatEpoch = previous.repeatEpoch + 1,
                    ),
                )
            } else {
                buffer.addLast(LinkTrafficEntry(id = nextId++, direction = direction, text = line))
            }
            while (buffer.size > capacity) buffer.removeFirst()
            _entries.value = buffer.toList()
        }
    }

    fun clear() {
        synchronized(buffer) {
            if (buffer.isEmpty()) return
            buffer.clear()
            _entries.value = emptyList()
        }
    }
}

fun formatLinkBinary(bytes: ByteArray): String {
    val header = if (bytes.size >= 2) {
        "%02X%02X".format(bytes[0].toInt() and 0xFF, bytes[1].toInt() and 0xFF)
    } else {
        "??"
    }
    val shown = bytes.take(16).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
    val tail = if (bytes.size > 16) " …" else ""
    return "BIN $header ${bytes.size}B $shown$tail"
}
