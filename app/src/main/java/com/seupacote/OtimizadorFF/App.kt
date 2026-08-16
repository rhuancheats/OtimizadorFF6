package com.seupacote.OtimizadorFF

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        criarCanalDeNotificacao()
    }

    private fun criarCanalDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_STATUS,
                "Status do Otimizador",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisa quando o Shizuku conecta e quando a otimização termina"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    companion object {
        const val CANAL_STATUS = "canal_status_otimizador"
    }
}
