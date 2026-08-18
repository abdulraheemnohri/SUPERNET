package com.supernet.app.bonding

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Strict validation boundary before a received frame can reach the VPN TUN. */
object ReceivedPacketValidator {
    private const val HEADER_SIZE = 40
    private const val VERSION = 1
    private const val MAX_FRAME = 64 * 1024

    fun extractPayload(frame: ByteArray, expectedSessionId: Long): ByteArray? {
        if (frame.size < HEADER_SIZE || frame.size > MAX_FRAME) return null
        if (frame[0] != 'S'.code.toByte() || frame[1] != 'U'.code.toByte() || frame[2] != 'P'.code.toByte() || frame[3] != 'N'.code.toByte()) return null
        if ((frame[4].toInt() and 0xff) != VERSION) return null
        val b = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN)
        b.position(8)
        if (b.long != expectedSessionId) return null
        b.position(36)
        val length = b.int
        if (length < 0 || HEADER_SIZE + length != frame.size) return null
        return frame.copyOfRange(HEADER_SIZE, frame.size)
    }
}
