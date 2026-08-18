package com.supernet.app.bonding

import android.net.Network
import com.supernet.app.network.NetworkLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * V1 UDP transport bound to a specific Android Network.
 * QUIC can replace this transport without changing the bonding core.
 */
class NetworkPathTransport(
    override val link: NetworkLink,
    private val network: Network,
    private val gateway: InetSocketAddress
) : PathTransport {
    private var socket: DatagramSocket? = null
    private val isConnected = AtomicBoolean(false)

    override val connected: Boolean
        get() = isConnected.get()

    override suspend fun connect() = withContext(Dispatchers.IO) {
        if (connected) return@withContext
        val created = network.socketFactory.createDatagramSocket()
        created.connect(gateway)
        socket = created
        isConnected.set(true)
    }

    override suspend fun send(packet: BondingPacket) = withContext(Dispatchers.IO) {
        val current = socket ?: error("Path ${link.id} is not connected")
        val bytes = BondingPacket.encode(packet)
        current.send(DatagramPacket(bytes, bytes.size, gateway))
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        isConnected.set(false)
        socket?.close()
        socket = null
    }
}
