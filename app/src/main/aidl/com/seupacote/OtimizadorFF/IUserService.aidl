package com.seupacote.OtimizadorFF

import android.os.Process

class UserService : IUserService.Stub() {

    override fun execCommand(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            process.inputStream.bufferedReader().use { it.readText() }.trim()
        } catch (e: Exception) {
            "Erro ao executar: ${e.message}"
        }
    }

    override fun destroy() {
        Process.killProcess(Process.myPid())
    }
}
