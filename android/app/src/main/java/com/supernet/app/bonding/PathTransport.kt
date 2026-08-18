package com.supernet.app.bonding

import com.supernet.app.network.NetworkLink

interface PathTransport {
    val link: NetworkLink
    val connected: Boolean

    suspend fun connect()
    suspend fun send(packet: BondingPacket)
    suspend fun receive(): BondingPacket? = null
    suspend fun close()
}

class PathTransportRegistry {
    private val transports = LinkedHashMap<String, PathTransport>()

    @Synchronized
    fun attach(transport: PathTransport) {
        transports[transport.link.id] = transport
    }

    @Synchronized
    fun detach(linkId: String): PathTransport? = transports.remove(linkId)

    @Synchronized
    fun connected(): List<PathTransport> = transports.values.filter { it.connected }

    @Synchronized
    fun all(): List<PathTransport> = transports.values.toList()

    suspend fun send(packet: BondingPacket) {
        val transport = synchronized(this) {
            transports.values.firstOrNull { it.link.id.hashCode() == packet.pathId && it.connected }
        } ?: return
        transport.send(packet)
    }
}
