package com.supernet.app.model

import android.net.Network

public enum class LinkType { WIFI, CELLULAR, ETHERNET, BLUETOOTH, OTHER }
public enum class LinkState { DISCOVERING, CONNECTING, ACTIVE, DEGRADED, UNSTABLE, FAILED, RECOVERING }

public data class NetworkLink(
    val id: String,
    val network: Network,
    val type: LinkType,
    val state: LinkState = LinkState.DISCOVERING,
    val latencyMs: Long = Long.MAX_VALUE,
    val jitterMs: Long = Long.MAX_VALUE,
    val packetLossPercent: Double = 100.0,
    val downMbps: Double = 0.0,
    val upMbps: Double = 0.0,
    val score: Double = 0.0,
    val metered: Boolean = false,
    val pathId: Int = 0
)
