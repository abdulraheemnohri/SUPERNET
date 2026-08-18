package com.supernet.app.bonding

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import java.net.DatagramSocket

class ReceiveDataPlane(
    private val scope: CoroutineScope,
    private val vpnInterface: ParcelFileDescriptor,
    private val socket: DatagramSocket,
    private val sessionId: Long
) {
    private val reassembly = PacketReassemblyBuffer()
    private val pump = TunPacketPump(scope, vpnInterface, {}, { packet ->
        // This direction is intentionally unused here; receive path writes directly to TUN.
    })

    private val receiver = UdpReceiveLoop(scope, socket, sessionId) { sequence, payload ->
        reassembly.offer(sequence, payload).forEach { packet ->
            pump.writeToTun(packet)
        }
    }

    fun start() = receiver.start()

    fun stop() {
        receiver.stop()
        pump.stop()
    }
}
