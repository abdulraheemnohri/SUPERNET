package com.supernet.app.bonding

import com.supernet.app.network.LinkState
import com.supernet.app.network.NetworkLink

class FailoverController(
    private val failureThreshold: Int = 3,
    private val recoveryThreshold: Int = 3
) {
    private val failures = mutableMapOf<String, Int>()
    private val recoveries = mutableMapOf<String, Int>()

    fun observe(link: NetworkLink): LinkState {
        return when (link.state) {
            LinkState.ACTIVE, LinkState.DEGRADED -> {
                failures[link.id] = 0
                recoveries[link.id] = (recoveries[link.id] ?: 0) + 1
                if ((recoveries[link.id] ?: 0) >= recoveryThreshold) LinkState.ACTIVE else LinkState.RECOVERING
            }
            LinkState.UNSTABLE, LinkState.FAILED -> {
                recoveries[link.id] = 0
                failures[link.id] = (failures[link.id] ?: 0) + 1
                if ((failures[link.id] ?: 0) >= failureThreshold) LinkState.FAILED else LinkState.UNSTABLE
            }
            else -> link.state
        }
    }
}
