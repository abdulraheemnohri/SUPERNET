package com.supernet.app.bonding

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

/** Moves raw IP packets between Android's VpnService TUN and the bonding core. */
class TunPacketPump(
    private val scope: CoroutineScope,
    private val vpnInterface: ParcelFileDescriptor,
    private val onPacketFromTun: suspend (ByteArray) -> Unit,
    private val onPacketToTun: suspend (ByteArray) -> Unit
) {
    private var readJob: Job? = null
    private val writeLock = Any()

    fun start() {
        if (readJob?.isActive == true) return
        readJob = scope.launch(Dispatchers.IO) {
            FileInputStream(vpnInterface.fileDescriptor).use { input ->
                val buffer = ByteArray(65535)
                while (isActive) {
                    val count = input.read(buffer)
                    if (count <= 0) continue
                    onPacketFromTun(buffer.copyOf(count))
                }
            }
        }
    }

    suspend fun writeToTun(packet: ByteArray) {
        require(packet.isNotEmpty()) { "Cannot write an empty packet" }
        synchronized(writeLock) {
            FileOutputStream(vpnInterface.fileDescriptor).use { output ->
                output.write(packet)
                output.flush()
            }
        }
    }

    fun stop() {
        readJob?.cancel()
        readJob = null
    }
}
