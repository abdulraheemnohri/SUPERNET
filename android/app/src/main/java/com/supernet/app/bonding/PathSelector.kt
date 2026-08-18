package com.supernet.app.bonding

import com.supernet.app.network.LinkState
import com.supernet.app.network.NetworkLink

class PathSelector {
    fun select(links: List<NetworkLink>): List<NetworkLink> = links
        .asSequence()
        .filter { it.state == LinkState.ACTIVE || it.state == LinkState.DEGRADED }
        .sortedByDescending { it.priority * 1000.0 + score(it) }
        .toList()

    fun weighted(links: List<NetworkLink>): List<Pair<NetworkLink, Double>> {
        val active = select(links)
        val total = active.sumOf { score(it).coerceAtLeast(0.01) }
        return active.map { it to score(it).coerceAtLeast(0.01) / total }
    }

    private fun score(link: NetworkLink): Double =
        (link.bandwidthMbps / 100.0).coerceIn(0.0, 1.0) * 0.35 +
            (1.0 - link.latencyMs / 300.0).coerceIn(0.0, 1.0) * 0.20 +
            (1.0 - link.packetLossPercent / 10.0).coerceIn(0.0, 1.0) * 0.25 +
            link.stability.coerceIn(0.0, 1.0) * 0.20
}
