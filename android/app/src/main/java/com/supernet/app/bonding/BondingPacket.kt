package com.supernet.app.bonding

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Small deterministic envelope used between the packet core and path transports. */
data class BondingPacket(
    val sessionId: Long,
    val pathId: Int,
    val sequence: Long,
    val timestampMillis: Long,
    val flags: Int,
    val payload: ByteArray
) {
    companion object {
        private const val MAGIC = 0x53504E31 // SPN1
        private const val VERSION = 1
        private const val HEADER_SIZE = 40

        fun encode(packet: BondingPacket): ByteArray {
            require(packet.payload.size <= 16 * 1024) { "Payload too large" }
            val buffer = ByteBuffer.allocate(HEADER_SIZE + packet.payload.size)
                .order(ByteOrder.BIG_ENDIAN)
            buffer.putInt(MAGIC)
            buffer.put(VERSION.toByte())
            buffer.put(packet.flags.toByte())
            buffer.putShort(0)
            buffer.putLong(packet.sessionId)
            buffer.putInt(packet.pathId)
            buffer.putLong(packet.sequence)
            buffer.putLong(packet.timestampMillis)
            buffer.putInt(packet.payload.size)
            buffer.put(packet.payload)
            return buffer.array()
        }

        fun decode(bytes: ByteArray): BondingPacket? {
            if (bytes.size < HEADER_SIZE) return null
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            if (buffer.int != MAGIC || buffer.get().toInt() != VERSION) return null
            val flags = buffer.get().toInt() and 0xff
            buffer.short
            val sessionId = buffer.long
            val pathId = buffer.int
            val sequence = buffer.long
            val timestamp = buffer.long
            val length = buffer.int
            if (length < 0 || length != bytes.size - HEADER_SIZE) return null
            val payload = ByteArray(length)
            buffer.get(payload)
            return BondingPacket(sessionId, pathId, sequence, timestamp, flags, payload)
        }
    }
}
