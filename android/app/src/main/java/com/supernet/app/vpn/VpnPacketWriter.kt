package com.supernet.app.vpn

import android.os.ParcelFileDescriptor
import java.io.FileOutputStream

/** Writes reconstructed raw IP packets back into Android's TUN interface. */
class VpnPacketWriter(private val descriptor: ParcelFileDescriptor) {
    private val output = FileOutputStream(descriptor.fileDescriptor)

    @Synchronized
    fun write(packet: ByteArray) {
        require(packet.isNotEmpty()) { "Packet must not be empty" }
        output.write(packet)
        output.flush()
    }

    fun close() {
        output.close()
    }
}
