package com.buenhijogames.plantilla_ajedrez.ui.compartir

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helper para compartir ficheros generados por la app (PDF/PGN) con otras apps.
 *
 * Escribe los [bytes] en la subcarpeta `compartir/` de la cache interna de la
 * app y lanza un [Intent.ACTION_SEND] con la URI proporcionada por [FileProvider],
 * de forma que solo la app receptora pueda leer el fichero (sin permisos de
 * almacenamiento externo).
 *
 * El uso es "fire and forget": no notifica resultado (la UI puede mostrar un
 * Snackbar antes de llamar a [compartir] si quiere feedback).
 */
object CompartirArchivo {

    /** Subcarpeta de la cache donde se depositan los ficheros a compartir. */
    private const val CARPETA_COMPARTIR = "compartir"

    /**
     * Comparte un fichero con el sistema (abre el selector de apps).
     *
     * @param contexto  Contexto de la aplicación.
     * @param bytes     Contenido del fichero.
     * @param nombre    Nombre de fichero (con extensión, p. ej. "partida.pdf").
     * @param tipoMime  Tipo MIME del fichero ("application/pdf", "application/x-chess-pgn"...).
     * @param asunto    Asunto del mensaje de compartir (título del Intent).
     */
    fun compartir(
        contexto: Context,
        bytes: ByteArray,
        nombre: String,
        tipoMime: String,
        asunto: String,
    ) {
        val carpeta = File(contexto.cacheDir, CARPETA_COMPARTIR).apply { mkdirs() }
        val fichero = File(carpeta, nombre)
        fichero.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            fichero,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = tipoMime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(Intent.createChooser(intent, asunto))
    }
}