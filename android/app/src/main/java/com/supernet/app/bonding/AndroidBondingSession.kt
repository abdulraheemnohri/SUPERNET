package com.supernet.app.bonding

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.ParcelFileDescriptor
import com.supernet.app.network.LinkManager
import com.supernet.app.network.LinkState
import com.supernet.app.network.NetworkLink
import com.supernet.app.network.TransportType
import com.supernet.app.vpn.VpnPacketReader
import com.supernet.app.vpn.VpnPacketWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/** V1 Android orchestration: discover paths, establish the session, move TUN packets and recover return traffic. */
class AndroidBondingSession(
    context: Context,
    private val scope: CoroutineScope,
    private val vpnDescriptor: ParcelFileDescriptor,
    gatewayHost: String,
    gatewayPort: Int
) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val gateway = InetSocketAddress(gatewayHost, gatewayPort)
    private val linkManager = LinkManager()
    private val scheduler = PacketScheduler(linkManager)
    private val transports = PathTransportRegistry()
    private val reassembly = PacketReassemblyBuffer()
    private val sessionId = SecureRandom().nextLong().let { if (it == 0L) 1L else it }
    private val channels = ConcurrentHashMap<String, ControlChannel>()
    private var tunReader: VpnPacketReader? = null
    private var receiveJobs = mutableListOf<Job>()
    private val writer = VpnPacketWriter(vpnDescriptor)

    fun start() {
        val discovered = connectivity.allNetworks.mapNotNull { network ->
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return@mapNotNull null
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@mapNotNull null
            val transport = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TransportType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TransportType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TransportType.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> TransportType.BLUETOOTH
                else -> return@mapNotNull null
            }
            NetworkLink(
                id = "${transport.name}-${network.networkHandle}",
                transport = transport,
                state = LinkState.ACTIVE,
                bandwidthMbps = capabilities.linkDownstreamBandwidthKbps / 1000.0,
                priority = when (transport) {
                    TransportType.ETHERNET -> 100
                    TransportType.WIFI -> 80
                    TransportType.CELLULAR -> 60
                    TransportType.BLUETOOTH -> 20
                }
            ) to network
        }

        linkManager.update(discovered.map { it.first })
        discovered.forEach { (link, network) ->
            scope.launch(Dispatchers.IO) { attachPath(link, network) }
        }

        tunReader = VpnPacketReader(vpnDescriptor) { payload ->
            scope.launch(Dispatchers.IO) {
                val packet = scheduler.next(payload, sessionId) ?: return@launch
                try { transports.send(packet) } catch (_: Exception) { }
            }
        }
        tunReader?.start()
    }

    private suspend fun attachPath(link: NetworkLink, network: Network) {
        val pathId = link.id.hashCode()
        val transport = NetworkPathTransport(link, network, gateway)
        try {
            transport.connect()
            transports.attach(transport)
            val control = ControlChannel(network, gateway, sessionId, pathId)
            channels[link.id] = control
            control.open()
            control.send(ControlMessage.ClientHello(sessionId))
            val accepted = control.receive()
            if (accepted !is ControlMessage.SessionAccept || accepted.sessionId != sessionId) {
                throw IllegalStateException("SUPERNet gateway did not accept session")
            }
            control.send(ControlMessage.PathRegister(sessionId, pathId))

            receiveJobs += scope.launch(Dispatchers.IO) {
                while (isActive && transport.connected) {
                    val packet = transport.receive() ?: continue
                    if (packet.sessionId != sessionId || (packet.flags and 0x01) != 0) continue
                    val payload = ReceivedPacketValidator.extractPayload(BondingPacket.encode(packet), sessionId) ?: continue
                    reassembly.offer(packet.sequence, payload).forEach { writer.write(it) }
                }
            }
        } catch (_: Exception) {
            channels.remove(link.id)
            transport.close()
        }
    }

    fun stop() {
        tunReader?.stop()
        tunReader = null
        receiveJobs.forEach { it.cancel() }
        receiveJobs.clear()
        channels.values.forEach { channel -> scope.launch(Dispatchers.IO) { channel.close() } }
        channels.clear()
        transports.all().forEach { transport -> scope.launch(Dispatchers.IO) { transport.close() } }
    }
}
