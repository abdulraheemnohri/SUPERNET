package com.supernet.app.network

enum class LinkState {
    DISCOVERING,
    CONNECTING,
    ACTIVE,
    DEGRADED,
    UNSTABLE,
    FAILED,
    RECOVERING
}

data class NetworkLink(
    val id: String,
    val transport: TransportType,
    val state: LinkState = LinkState.DISCOVERING,
    val bandwidthMbps: Double = 0.0,
    val latencyMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val packetLossPercent: Double = 0.0,
    val stability: Double = 0.0,
    val batteryCost: Double = 0.0,
    val priority: Int = 0
)

enum class TransportType {
    WIFI,
    CELLULAR,
    ETHERNET,
    BLUETOOTH
}

object LinkScorer {
    fun score(link: NetworkLink): Double {
        val bandwidth = (link.bandwidthMbps / 100.0).coerceIn(0.0, 1.0)
        val latency = (1.0 - link.latencyMs / 200.0).coerceIn(0.0, 1.0)
        val jitter = (1.0 - link.jitterMs / 100.0).coerceIn(0.0, 1.0)
        val loss = (1.0 - link.packetLossPercent / 10.0).coerceIn(0.0, 1.0)
        val stability = link.stability.coerceIn(0.0, 1.0)
        val battery = link.batteryCost.coerceIn(0.0, 1.0)

        return 35.0 * bandwidth +
            20.0 * latency +
            10.0 * jitter +
            20.0 * loss +
            20.0 * stability -
            5.0 * battery
    }
}
