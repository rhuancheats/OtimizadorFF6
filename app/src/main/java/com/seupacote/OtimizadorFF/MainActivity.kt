package com.seupacote.OtimizadorFF

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private var userService: IUserService? = null

    private val servicoConectado = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            userService = IUserService.Stub.asInterface(binder)
            atualizarStatus("Shizuku conectado e pronto ✅")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            userService = null
            atualizarStatus("Serviço desconectado")
        }
    }

    private val binderRecebido = Shizuku.OnBinderReceivedListener {
        pedirPermissaoOuConectar()
    }

    private val binderMorreu = Shizuku.OnBinderDeadListener {
        userService = null
        atualizarStatus("Shizuku desconectado. Abra o app Shizuku novamente.")
    }

    private val resultadoPermissao = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == COD_PERMISSAO_SHIZUKU) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                conectarUserService()
            } else {
                atualizarStatus("Permissão do Shizuku negada")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        val btnFF = findViewById<Button>(R.id.btnFF)
        val btnFFMax = findViewById<Button>(R.id.btnFFMax)
        val btnShizuku = findViewById<Button>(R.id.btnShizuku)

        pedirPermissaoNotificacao()

        Shizuku.addBinderReceivedListenerSticky(binderRecebido)
        Shizuku.addBinderDeadListener(binderMorreu)
        Shizuku.addRequestPermissionResultListener(resultadoPermissao)

        btnShizuku.setOnClickListener { abrirOuInstalarShizuku() }
        btnFF.setOnClickListener { otimizar("Free Fire") }
        btnFFMax.setOnClickListener { otimizar("Free Fire Max") }

        atualizarStatusInicial()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderRecebido)
        Shizuku.removeBinderDeadListener(binderMorreu)
        Shizuku.removeRequestPermissionResultListener(resultadoPermissao)
        userService?.let {
            try { unbindService(servicoConectado) } catch (_: Exception) {}
        }
    }

    private fun atualizarStatusInicial() {
        if (!shizukuInstalado()) {
            atualizarStatus("Shizuku não instalado. Toque em 'Ativar Shizuku'.")
        } else if (!Shizuku.pingBinder()) {
            atualizarStatus("Shizuku instalado, mas não está rodando. Abra o app Shizuku e pareie pela depuração wi-fi.")
        } else {
            pedirPermissaoOuConectar()
        }
    }

    private fun pedirPermissaoOuConectar() {
        if (Shizuku.isPreV11()) {
            atualizarStatus("Versão do Shizuku desatualizada")
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            conectarUserService()
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            atualizarStatus("Permissão negada anteriormente. Ative manualmente no app Shizuku.")
        } else {
            Shizuku.requestPermission(COD_PERMISSAO_SHIZUKU)
        }
    }

    private fun conectarUserService() {
        val args = Shizuku.UserServiceArgs(ComponentName(packageName, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("otimizador")
            .debuggable(false)
            .version(1)
        Shizuku.bindUserService(args, servicoConectado)
    }

    private fun shizukuInstalado(): Boolean {
        return try {
            packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun abrirOuInstalarShizuku() {
        if (shizukuInstalado()) {
            val launchIntent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (launchIntent != null) {
                startActivity(launchIntent)
                Toast.makeText(this, "No Shizuku, use 'Parear via wi-fi' com o código da Depuração wi-fi", Toast.LENGTH_LONG).show()
            }
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            } catch (_: Exception) {}
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
            startActivity(intent)
            Toast.makeText(this, "Instale o app Shizuku e volte aqui", Toast.LENGTH_LONG).show()
        }
    }

    private fun otimizar(jogo: String) {
        val servico = userService
        if (servico == null) {
            Toast.makeText(this, "Shizuku não está pronto ainda", Toast.LENGTH_SHORT).show()
            return
        }

        val comandos = listOf(
            "settings put global animator_duration_scale 0",
            "settings put global transition_animation_scale 0",
            "settings put global window_animation_scale 0",
            "am kill-all",
            "settings put global background_process_limit 2"
        )

        comandos.forEach { cmd -> servico.execCommand(cmd) }

        Toast.makeText(this, "$jogo otimizado!", Toast.LENGTH_LONG).show()
        notificar("Otimização concluída", "$jogo foi otimizado com sucesso.")
    }

    private fun atualizarStatus(texto: String) {
        runOnUiThread { status.text = texto }
    }

    private fun pedirPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    COD_PERMISSAO_NOTIFICACAO
                )
            }
        }
    }

    private fun notificar(titulo: String, texto: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val notificacao = NotificationCompat.Builder(this, App.CANAL_STATUS)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notificacao)
    }

    companion object {
        private const val COD_PERMISSAO_SHIZUKU = 1001
        private const val COD_PERMISSAO_NOTIFICACAO = 2001
    }
}
