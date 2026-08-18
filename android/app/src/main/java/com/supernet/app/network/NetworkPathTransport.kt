package com.supernet.app.network

import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

public class NetworkPathTransport(
    private val network: Network,
    private val gateway: InetSocketAddress
) : AutoCloseable {
    private var socket: DatagramSocket? = null

    public suspend fun open() = withContext(Dispatchers.IO) {
        if (socket != null) return@withContext
        socket = network.socketFactory.createDatagramSocket().apply { connect(gateway) }
    }

    public suspend fun send(payload: ByteArray) = withContext(Dispatchers.IO) {
        val s = socket ?: error("Transport is not open")
        s.send(DatagramPacket(payload, payload.size))
    }

    public suspend fun receive(maxSize: Int = 65535): ByteArray = withContext(Dispatchers.IO) {
        val s = socket ?: error("Transport is not open")
        val buffer = ByteArray(maxSize)
        val packet = DatagramPacket(buffer, buffer.size)
        s.receive(packet)
        packet.data.copyOf(packet.length)
    }

    override fun close() {
        socket?.close()
        socket = null
    }
}
