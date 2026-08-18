package com.supernet.app.bonding

import com.supernet.app.network.NetworkLink

interface PathTransport {
    val link: NetworkLink
    val connected: Boolean

    suspend fun connect()
    suspend fun send(packet: BondingPacket)
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
}
