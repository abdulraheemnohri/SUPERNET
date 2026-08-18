package com.supernet.app.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LinkManager {
    private val _links = MutableStateFlow<List<NetworkLink>>(emptyList())
    val links: StateFlow<List<NetworkLink>> = _links.asStateFlow()

    fun update(discovered: List<NetworkLink>) {
        _links.value = discovered.sortedByDescending { LinkScorer.score(it) }
    }

    fun active(): List<NetworkLink> = _links.value.filter {
        it.state == LinkState.ACTIVE || it.state == LinkState.DEGRADED
    }
}
