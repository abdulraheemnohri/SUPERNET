package com.supernet.app.bonding

import java.util.TreeMap

/** Reorders packets arriving over independent paths and exposes contiguous payloads. */
class PacketReorderBuffer(private var nextSequence: Long = 0) {
    private val pending = TreeMap<Long, BondingPacket>()

    @Synchronized
    fun offer(packet: BondingPacket): List<BondingPacket> {
        if (packet.sequence < nextSequence) return emptyList()
        pending.putIfAbsent(packet.sequence, packet)

        val ready = ArrayList<BondingPacket>()
        while (true) {
            val packetAtHead = pending.remove(nextSequence) ?: break
            ready += packetAtHead
            nextSequence++
        }
        return ready
    }

    @Synchronized
    fun bufferedPackets(): Int = pending.size

    @Synchronized
    fun expectedSequence(): Long = nextSequence
}
