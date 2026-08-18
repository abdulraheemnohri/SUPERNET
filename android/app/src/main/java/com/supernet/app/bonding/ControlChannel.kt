package com.supernet.app.bonding

import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong

/** V1 control channel uses the same 40-byte bonding envelope as data traffic. */
class ControlChannel(
    private val network: Network,
    private val gateway: InetSocketAddress,
    private val sessionId: Long,
    private val pathId: Int
) {
    private var socket: java.net.DatagramSocket? = null
    private val sequence = AtomicLong(0)

    suspend fun open() = withContext(Dispatchers.IO) {
        if (socket != null) return@withContext
        socket = network.socketFactory.createDatagramSocket().apply { connect(gateway) }
    }

    suspend fun send(message: ControlMessage) = withContext(Dispatchers.IO) {
        val current = socket ?: error("Control channel is not open")
        require(message.sessionId == sessionId) { "Control message belongs to another session" }
        val envelope = BondingPacket(
            sessionId = sessionId,
            pathId = pathId,
            sequence = sequence.getAndIncrement(),
            timestampMillis = System.currentTimeMillis(),
            flags = 0x01,
            payload = ControlMessage.encode(message)
        )
        val bytes = BondingPacket.encode(envelope)
        current.send(DatagramPacket(bytes, bytes.size, gateway))
    }

    suspend fun receive(): ControlMessage? = withContext(Dispatchers.IO) {
        val current = socket ?: error("Control channel is not open")
        val buffer = ByteArray(65535)
        val packet = DatagramPacket(buffer, buffer.size)
        current.receive(packet)
        val envelope = BondingPacket.decode(packet.data.copyOf(packet.length)) ?: return@withContext null
        if (envelope.sessionId != sessionId || envelope.flags and 0x01 == 0) return@withContext null
        ControlMessage.decode(envelope.payload)
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        socket?.close()
        socket = null
    }
}
