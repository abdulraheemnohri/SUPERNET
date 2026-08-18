package com.supernet.app.bonding

import java.util.TreeMap

class PacketReassemblyBuffer(private val maxPending: Int = 256) {
    private var nextSequence = 0L
    private val pending = TreeMap<Long, ByteArray>()

    fun offer(sequence: Long, payload: ByteArray): List<ByteArray> {
        if (sequence < nextSequence || pending.containsKey(sequence)) return emptyList()
        pending[sequence] = payload
        while (pending.size > maxPending) pending.pollFirstEntry()
        val ready = ArrayList<ByteArray>()
        while (true) {
            val packet = pending.remove(nextSequence) ?: break
            ready += packet
            nextSequence++
        }
        return ready
    }

    fun reset(firstSequence: Long = 0L) {
        nextSequence = firstSequence
        pending.clear()
    }
}
