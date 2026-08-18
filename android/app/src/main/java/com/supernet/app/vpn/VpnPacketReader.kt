package com.supernet.app.vpn

import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

/** Reads raw IP packets from Android's TUN interface. */
class VpnPacketReader(
    private val descriptor: ParcelFileDescriptor,
    private val onPacket: (ByteArray) -> Unit
) {
    private val running = AtomicBoolean(false)

    fun start(): Thread {
        check(running.compareAndSet(false, true)) { "Reader already started" }
        return Thread {
            FileInputStream(descriptor.fileDescriptor).use { input ->
                val buffer = ByteArray(32767)
                while (running.get()) {
                    val length = input.read(buffer)
                    if (length > 0) {
                        onPacket(buffer.copyOf(length))
                    }
                }
            }
        }.apply {
            name = "supernet-tun-reader"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
    }
}
