package com.supernet.app.bonding

import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetSocketAddress

/** Control channel deliberately uses the same network-bound UDP primitive as V1 data transport. */
class ControlChannel(
    private val network: Network,
    private val gateway: InetSocketAddress
) {
    private var socket: java.net.DatagramSocket? = null

    suspend fun open() = withContext(Dispatchers.IO) {
        if (socket != null) return@withContext
        socket = network.socketFactory.createDatagramSocket().apply { connect(gateway) }
    }

    suspend fun send(message: ControlMessage) = withContext(Dispatchers.IO) {
        val current = socket ?: error("Control channel is not open")
        val bytes = ControlMessage.encode(message)
        current.send(DatagramPacket(bytes, bytes.size, gateway))
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        socket?.close()
        socket = null
    }
}
