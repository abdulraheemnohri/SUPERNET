package com.supernet.app.bonding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Coordinates packets leaving the TUN with the existing deterministic scheduler. */
class BondingDataPlane(
    private val scope: CoroutineScope,
    private val scheduler: PacketScheduler,
    private val send: suspend (BondingPacket) -> Unit
) {
    private var job: Job? = null

    fun start(source: suspend (suspend (ByteArray) -> Unit) -> Unit, sessionId: Long) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            source { packet ->
                val bonded = scheduler.next(packet, sessionId) ?: return@source
                send(bonded)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
