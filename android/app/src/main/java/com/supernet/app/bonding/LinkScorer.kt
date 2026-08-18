package com.supernet.app.bonding

import com.supernet.app.model.NetworkLink

public data class LinkScoreWeights(
    val bandwidth: Double = 0.35,
    val latency: Double = 0.20,
    val stability: Double = 0.25,
    val loss: Double = 0.15,
    val batteryCost: Double = 0.05
)

public class LinkScorer(private val weights: LinkScoreWeights = LinkScoreWeights()) {
    public fun score(link: NetworkLink): Double {
        val bandwidth = (link.downMbps / 100.0).coerceIn(0.0, 1.0)
        val latency = if (link.latencyMs == Long.MAX_VALUE) 0.0 else (1.0 - link.latencyMs / 300.0).coerceIn(0.0, 1.0)
        val stability = (1.0 - link.packetLossPercent / 10.0).coerceIn(0.0, 1.0)
        val loss = (1.0 - link.packetLossPercent / 5.0).coerceIn(0.0, 1.0)
        val batteryCost = if (link.metered) 0.5 else 0.0
        return (bandwidth * weights.bandwidth + latency * weights.latency + stability * weights.stability + loss * weights.loss - batteryCost * weights.batteryCost).coerceIn(0.0, 1.0) * 100.0
    }
}
