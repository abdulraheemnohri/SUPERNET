package com.supernet.app.bonding

import java.util.LinkedHashMap

class RetransmissionQueue(
    private val maxPackets: Int = 512,
    private val maxBytes: Long = 8L * 1024 * 1024
) {
    data class Entry(val sequence: Long, val frame: ByteArray, var attempts: Int = 0)

    private val entries = LinkedHashMap<Long, Entry>()
    private var bytes = 0L

    @Synchronized
    fun retain(sequence: Long, frame: ByteArray) {
        if (entries.containsKey(sequence)) return
        while (entries.size >= maxPackets || bytes + frame.size > maxBytes) {
            val first = entries.entries.firstOrNull() ?: break
            bytes -= first.value.frame.size
            entries.remove(first.key)
        }
        entries[sequence] = Entry(sequence, frame.copyOf())
        bytes += frame.size
    }

    @Synchronized
    fun acknowledge(upToSequence: Long) {
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key <= upToSequence) {
                bytes -= entry.value.frame.size
                iterator.remove()
            } else break
        }
    }

    @Synchronized
    fun due(maxAttempts: Int = 5): List<Entry> = entries.values.filter { it.attempts < maxAttempts }.map {
        it.attempts++
        it.copy(frame = it.frame.copyOf())
    }

    @Synchronized
    fun size(): Int = entries.size
}
