package com.supernet.app.bonding

import android.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong

class SuperNetSessionController(
    private val scope: CoroutineScope,
    private val gateway: InetSocketAddress
) {
    private val nextSession = AtomicLong(System.currentTimeMillis())
    private var sessionId: Long = 0
    private val channels = mutableMapOf<Int, ControlChannel>()
    private val heartbeatJobs = mutableMapOf<Int, Job>()

    fun start(networks: Map<Int, Network>) {
        if (sessionId != 0L) return
        sessionId = nextSession.incrementAndGet()
        scope.launch(Dispatchers.IO) {
            for ((pathId, network) in networks) registerPath(pathId, network)
        }
    }

    private suspend fun registerPath(pathId: Int, network: Network) {
        val channel = ControlChannel(network, gateway)
        channel.open()
        channel.send(ControlMessage.ClientHello(sessionId))
        channel.send(ControlMessage.PathRegister(sessionId, pathId))
        channels[pathId] = channel
        heartbeatJobs[pathId]?.cancel()
        heartbeatJobs[pathId] = scope.launch(Dispatchers.IO) {
            var nonce = 0L
            while (isActive) {
                delay(5_000)
                channel.send(ControlMessage.Heartbeat(sessionId, pathId, ++nonce))
            }
        }
    }

    fun stop() {
        heartbeatJobs.values.forEach(Job::cancel)
        heartbeatJobs.clear()
        scope.launch(Dispatchers.IO) {
            channels.values.forEach { channel ->
                runCatching { channel.send(ControlMessage.SessionClose(sessionId)) }
                runCatching { channel.close() }
            }
            channels.clear()
        }
        sessionId = 0
    }
}
