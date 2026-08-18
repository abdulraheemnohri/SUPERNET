package com.supernet.app.bonding

/**
 * Turns a timed-out frame back into scheduler input so a failed physical path
 * does not pin retransmission to the original route.
 */
class PathAwareRetransmission(
    private val scheduler: PacketScheduler,
    private val sessionId: Long,
    private val send: suspend (BondingPacket) -> Unit
) {
    suspend fun retransmit(frame: ByteArray) {
        val packet = scheduler.next(frame, sessionId) ?: return
        send(packet)
    }
}
