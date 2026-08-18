package com.supernet.app.bonding

import java.security.SecureRandom

object SessionId {
    private val random = SecureRandom()

    fun generate(): Long {
        var value = random.nextLong()
        if (value == 0L) value = 1L
        return value
    }
}
