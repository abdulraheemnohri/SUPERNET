package com.supernet.app.bonding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RetransmissionController(
    private val scope: CoroutineScope,
    private val queue: RetransmissionQueue,
    private val rtoMs: Long = 500L,
    private val onRetransmit: suspend (ByteArray) -> Unit
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(rtoMs)
                queue.due().forEach { entry -> onRetransmit(entry.frame) }
            }
        }
    }

    fun acknowledge(sequence: Long) {
        queue.acknowledge(sequence)
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
