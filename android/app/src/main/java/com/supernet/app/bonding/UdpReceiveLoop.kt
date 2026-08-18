package com.supernet.app.bonding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket

class UdpReceiveLoop(
    private val scope: CoroutineScope,
    private val socket: DatagramSocket,
    private val expectedSessionId: Long,
    private val onPayload: suspend (sequence: Long, payload: ByteArray) -> Unit
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(64 * 1024)
            while (isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                runCatching { socket.receive(packet) }.getOrElse { break }
                val frame = packet.data.copyOf(packet.length)
                val payload = ReceivedPacketValidator.extractPayload(frame, expectedSessionId) ?: continue
                if (frame.size >= 36) {
                    val sequence = java.nio.ByteBuffer.wrap(frame, 20, 8).long
                    onPayload(sequence, payload)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
