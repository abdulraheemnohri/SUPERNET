package com.supernet.app.bonding

import kotlinx.coroutines.CoroutineScope

/** Connects bounded retry state with normal multipath scheduling. */
class RetransmissionSchedulerBridge(
    scope: CoroutineScope,
    queue: RetransmissionQueue,
    scheduler: PacketScheduler,
    sessionId: Long,
    send: suspend (BondingPacket) -> Unit
) {
    private val pathAware = PathAwareRetransmission(scheduler, sessionId, send)
    private val controller = RetransmissionController(
        scope = scope,
        queue = queue,
        onRetransmit = { frame -> pathAware.retransmit(frame) }
    )

    fun start() = controller.start()
    fun acknowledge(sequence: Long) = controller.acknowledge(sequence)
    fun stop() = controller.stop()
}
