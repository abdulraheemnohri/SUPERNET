package com.supernet.app.bonding

import com.supernet.app.network.LinkManager
import com.supernet.app.network.NetworkLink
import java.util.concurrent.atomic.AtomicLong

/** Deterministic weighted scheduler. It never invents capacity and only selects healthy paths. */
class PacketScheduler(private val linkManager: LinkManager) {
    private val sequence = AtomicLong(0)
    private var cursor = 0

    fun next(payload: ByteArray, sessionId: Long): BondingPacket? {
        val links = linkManager.active()
        if (links.isEmpty()) return null

        val selected = selectWeighted(links) ?: return null
        return BondingPacket(
            sessionId = sessionId,
            pathId = selected.pathId(),
            sequence = sequence.getAndIncrement(),
            timestampMillis = System.currentTimeMillis(),
            flags = 0,
            payload = payload
        )
    }

    private fun selectWeighted(links: List<NetworkLink>): NetworkLink? {
        val scored = links.map { it to LinkWeight.score(it) }
        val total = scored.sumOf { it.second }
        if (total <= 0.0) return links[cursor++ % links.size]

        val point = (cursor++ % 10000) / 10000.0 * total
        var cumulative = 0.0
        for ((link, weight) in scored) {
            cumulative += weight
            if (point < cumulative) return link
        }
        return scored.last().first
    }

    private fun NetworkLink.pathId(): Int = id.hashCode()

    private object LinkWeight {
        fun score(link: NetworkLink): Double =
            LinkScoreAdapter.score(link).coerceAtLeast(0.01)
    }

    private object LinkScoreAdapter {
        fun score(link: NetworkLink): Double =
            com.supernet.app.network.LinkScorer.score(link)
    }
}
