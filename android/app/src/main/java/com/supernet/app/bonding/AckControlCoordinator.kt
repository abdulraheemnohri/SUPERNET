package com.supernet.app.bonding

/** Builds cumulative ACK control messages from packets delivered by reassembly. */
class AckControlCoordinator(
    private val sessionId: Long,
    private val pathId: Int
) {
    private val tracker = AckTracker()
    private var lastSent = -1L

    @Synchronized
    fun observeDelivered(sequence: Long): ControlMessage.Ack? {
        val contiguous = tracker.observe(sequence)
        if (contiguous <= lastSent) return null
        lastSent = contiguous
        return ControlMessage.Ack(sessionId, pathId, contiguous)
    }

    fun reset() {
        synchronized(this) {
            tracker.reset()
            lastSent = -1L
        }
    }
}
