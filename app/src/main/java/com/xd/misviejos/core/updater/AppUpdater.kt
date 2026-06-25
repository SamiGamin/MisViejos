package com.xd.misviejos.core.updater

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Modelo de datos de una actualización disponible
data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

class AppUpdater(private val context: Context) {

    companion object {
        private const val GITHUB_API_URL =
            "https://api.github.com/repos/SamiGamin/MisViejos/releases/latest"
    }

    /**
     * Consulta la API de GitHub y retorna UpdateInfo si hay una nueva versión,
     * o null si la app está al día.
     */
    suspend fun verificarActualizacion(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            if (connection.responseCode != 200) return@withContext null

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json = JSONObject(body)
            val tagName = json.getString("tag_name").removePrefix("v") // "v1.0.1" → "1.0.1"
            val releaseNotes = json.optString("body", "Nueva versión disponible.")

            // Buscar el asset APK
            val assets = json.getJSONArray("assets")
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (downloadUrl == null) return@withContext null

            // Comparar versiones
            val versionActual = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "0.0.0"

            if (esVersionMayorQue(tagName, versionActual)) {
                UpdateInfo(
                    versionName = tagName,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes
                )
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * Descarga el APK y lo instala usando el FileProvider.
     * Llama a [onProgress] con valores de 0 a 100.
     */
    suspend fun descargarEInstalar(
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val apkFile = File(context.externalCacheDir, "MisViejos-${updateInfo.versionName}.apk")

            val connection = URL(updateInfo.downloadUrl).openConnection() as HttpURLConnection
            connection.connect()

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloadedBytes += bytes
                        if (totalBytes > 0) {
                            onProgress(((downloadedBytes * 100) / totalBytes).toInt())
                        }
                    }
                }
            }
            connection.disconnect()
            onProgress(100)

            // Lanzar el instalador del sistema
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        }
    }

    /**
     * Compara dos versiones semánticas (MAJOR.MINOR.PATCH).
     * Retorna true si [nueva] es mayor que [actual].
     */
    private fun esVersionMayorQue(nueva: String, actual: String): Boolean {
        val parteNueva = nueva.split(".").map { it.toIntOrNull() ?: 0 }
        val parteActual = actual.split(".").map { it.toIntOrNull() ?: 0 }
        val tamano = maxOf(parteNueva.size, parteActual.size)
        for (i in 0 until tamano) {
            val n = parteNueva.getOrElse(i) { 0 }
            val a = parteActual.getOrElse(i) { 0 }
            if (n > a) return true
            if (n < a) return false
        }
        return false
    }
}
