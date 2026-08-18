package com.supernet.app.bonding

import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed interface ControlMessage {
    val sessionId: Long
    data class ClientHello(override val sessionId: Long) : ControlMessage
    data class SessionAccept(override val sessionId: Long) : ControlMessage
    data class PathRegister(override val sessionId: Long, val pathId: Int) : ControlMessage
    data class PathAck(override val sessionId: Long, val pathId: Int) : ControlMessage
    data class Heartbeat(override val sessionId: Long, val pathId: Int, val nonce: Long) : ControlMessage
    data class HeartbeatAck(override val sessionId: Long, val pathId: Int, val nonce: Long) : ControlMessage
    data class Ack(override val sessionId: Long, val pathId: Int, val sequence: Long) : ControlMessage
    data class PathClose(override val sessionId: Long, val pathId: Int) : ControlMessage
    data class SessionClose(override val sessionId: Long) : ControlMessage

    companion object {
        private const val CLIENT_HELLO = 1
        private const val SESSION_ACCEPT = 2
        private const val PATH_REGISTER = 3
        private const val PATH_ACK = 4
        private const val HEARTBEAT = 5
        private const val HEARTBEAT_ACK = 6
        private const val ACK = 9
        private const val PATH_CLOSE = 7
        private const val SESSION_CLOSE = 8

        fun encode(message: ControlMessage): ByteArray {
            val size = when (message) {
                is ClientHello, is SessionAccept, is SessionClose -> 9
                is PathRegister, is PathAck, is PathClose -> 13
                is Heartbeat, is HeartbeatAck -> 21
                is Ack -> 21
            }
            val out = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
            when (message) {
                is ClientHello -> out.put(CLIENT_HELLO.toByte()).putLong(message.sessionId)
                is SessionAccept -> out.put(SESSION_ACCEPT.toByte()).putLong(message.sessionId)
                is PathRegister -> out.put(PATH_REGISTER.toByte()).putLong(message.sessionId).putInt(message.pathId)
                is PathAck -> out.put(PATH_ACK.toByte()).putLong(message.sessionId).putInt(message.pathId)
                is Heartbeat -> out.put(HEARTBEAT.toByte()).putLong(message.sessionId).putInt(message.pathId).putLong(message.nonce)
                is HeartbeatAck -> out.put(HEARTBEAT_ACK.toByte()).putLong(message.sessionId).putInt(message.pathId).putLong(message.nonce)
                is Ack -> out.put(ACK.toByte()).putLong(message.sessionId).putInt(message.pathId).putLong(message.sequence)
                is PathClose -> out.put(PATH_CLOSE.toByte()).putLong(message.sessionId).putInt(message.pathId)
                is SessionClose -> out.put(SESSION_CLOSE.toByte()).putLong(message.sessionId)
            }
            return out.array()
        }

        fun decode(bytes: ByteArray): ControlMessage? {
            if (bytes.isEmpty()) return null
            val b = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            fun session(): Long? = if (b.remaining() >= 8) b.long else null
            fun path(): Int? = if (b.remaining() >= 4) b.int else null
            fun nonce(): Long? = if (b.remaining() >= 8) b.long else null
            return when (b.get().toInt() and 0xff) {
                CLIENT_HELLO -> if (bytes.size == 9) session()?.let(::ClientHello) else null
                SESSION_ACCEPT -> if (bytes.size == 9) session()?.let(::SessionAccept) else null
                PATH_REGISTER -> if (bytes.size == 13) { val s = session(); val p = path(); if (s != null && p != null) PathRegister(s, p) else null } else null
                PATH_ACK -> if (bytes.size == 13) { val s = session(); val p = path(); if (s != null && p != null) PathAck(s, p) else null } else null
                HEARTBEAT -> if (bytes.size == 21) { val s = session(); val p = path(); val n = nonce(); if (s != null && p != null && n != null) Heartbeat(s, p, n) else null } else null
                HEARTBEAT_ACK -> if (bytes.size == 21) { val s = session(); val p = path(); val n = nonce(); if (s != null && p != null && n != null) HeartbeatAck(s, p, n) else null } else null
                ACK -> if (bytes.size == 21) { val s = session(); val p = path(); val seq = nonce(); if (s != null && p != null && seq != null) Ack(s, p, seq) else null } else null
                PATH_CLOSE -> if (bytes.size == 13) { val s = session(); val p = path(); if (s != null && p != null) PathClose(s, p) else null } else null
                SESSION_CLOSE -> if (bytes.size == 9) session()?.let(::SessionClose) else null
                else -> null
            }
        }
    }
}
