package com.supernet.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.supernet.app.bonding.AndroidBondingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SuperNetVpnService : VpnService() {
    companion object {
        const val EXTRA_GATEWAY_HOST = "gateway_host"
        const val EXTRA_GATEWAY_PORT = "gateway_port"
        private const val CHANNEL_ID = "supernet_vpn"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var bondingSession: AndroidBondingSession? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()

        if (vpnInterface == null) {
            vpnInterface = Builder()
                .setSession("SUPERNet")
                .setMtu(1400)
                .addAddress("10.77.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .establish()
        }

        if (bondingSession == null) {
            val host = intent?.getStringExtra(EXTRA_GATEWAY_HOST)?.takeIf { it.isNotBlank() } ?: "127.0.0.1"
            val port = intent?.getIntExtra(EXTRA_GATEWAY_PORT, 48000) ?: 48000
            vpnInterface?.let { descriptor ->
                bondingSession = AndroidBondingSession(
                    context = this,
                    scope = serviceScope,
                    vpnDescriptor = descriptor,
                    gatewayHost = host,
                    gatewayPort = port
                ).also { it.start() }
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "SUPERNet VPN", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("SUPERNet")
                .setContentText("Bonding network connections")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("SUPERNet")
                .setContentText("Bonding network connections")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setOngoing(true)
                .build()
        }
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onRevoke() {
        stopSession()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopSession()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopSession() {
        bondingSession?.stop()
        bondingSession = null
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
