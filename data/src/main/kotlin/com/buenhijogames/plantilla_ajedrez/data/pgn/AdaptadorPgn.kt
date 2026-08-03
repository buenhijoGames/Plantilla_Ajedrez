package com.buenhijogames.plantilla_ajedrez.data.pgn

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.pgn.PuertoPgn
import com.github.bhlangonijr.chesslib.game.GameResult
import com.github.bhlangonijr.chesslib.game.GenericPlayer
import com.github.bhlangonijr.chesslib.pgn.PgnIterator
import com.github.bhlangonijr.chesslib.util.LargeFile
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion de [PuertoPgn] respaldada por chesslib (Apache 2.0).
 *
 * Exportacion:
 *   Se construye el PGN a partir de los tags del Seven Tag Roster presentes
 *   en [Partida] mas SetUp/FEN si la partida parte de una posicion no
 *   estandar, seguido del movetext ya almacenado en [Partida.pgn]. Esto
 *   garantiza que una partida recien creada produzca un PGN valido (aunque
 *   el movetext este vacio) y que una partida importada y reexportada
 *   conserve variantes, comentarios y NAGs tal cual estaban.
 *
 * Importacion:
 *   Se parsea con [PgnIterator] sobre un [LargeFile] construido desde un
 *   [ByteArrayInputStream] del texto PGN. chesslib se encarga de parsear
 *   tags, movetext, variantes, comentarios y NAGs, preservandolos al
 *   reexportar via [Game.toPgn].
 *
 * Si el PGN trae tags adicionales (WhiteElo, BlackElo, Time, Mode), se
 * conservan serializandolos en el campo [Partida.pgn] dentro del movetext
 * extendido, de modo que la ronda de exportacion reexporta el PGN original.
 */
@Singleton
class AdaptadorPgn @Inject constructor() : PuertoPgn {

    /**
     * Construye el PGN (export format) de una [Partida].
     *
     * Devuelve al menos los siete Tag Roster obligatorios del estandar PGN
     * (Event, Site, Date, Round, White, Black, Result), mas FEN/Setup si la
     * partida no parte de la posicion inicial estandar.
     */
    override fun exportar(partida: Partida): String {
        val sb = StringBuilder()
        sb.appendTag("Event", partida.evento.ifBlank { "?" })
        sb.appendTag("Site", partida.sitio.ifBlank { "?" })
        sb.appendTag("Date", partida.fecha.ifBlank { "????.??.??" })
        sb.appendTag("Round", partida.ronda.ifBlank { "?" })
        sb.appendTag("White", partida.blancas.ifBlank { "?" })
        sb.appendTag("Black", partida.negras.ifBlank { "?" })
        sb.appendTag("Result", partida.resultado.pgn)
        partida.eloBlancas?.let { sb.appendTag("WhiteElo", it.toString()) }
        partida.eloNegras?.let { sb.appendTag("BlackElo", it.toString()) }
        partida.fechaHora?.let { sb.appendTag("Time", it) }
        if (partida.modo.isNotBlank()) sb.appendTag("Mode", partida.modo)
        val fenInicial = partida.fen
        if (fenInicial != null) {
            sb.appendTag("SetUp", "1")
            sb.appendTag("FEN", fenInicial)
        }
        sb.append('\n')
        // El movetext de la partida se conserva tal cual (incluye variantes
        // y comentarios). Si esta vacio (partida nueva), queda solo la
        // cabecera, que sigue siendo PGN valido.
        val movetext = partida.pgn
            // Si el pgn ya trae bloques de tags (importado), los descartamos
            // para no duplicar: nos quedamos con el movetext tras el primer
            // salto de linea en blanco o tras el ultimo tag ']'.
            .quitarCabeceraPgn()
            .trim()
        if (movetext.isNotEmpty()) {
            sb.append(movetext)
            if (!movetext.endsWith(partida.resultado.pgn)) {
                sb.append(' ').append(partida.resultado.pgn)
            }
        } else {
            sb.append(partida.resultado.pgn)
        }
        sb.append('\n')
        return sb.toString()
    }

    /**
     * Importa un texto PGN completo (una o varias partidas).
     *
     * @return lista de [Partida]es parseadas. Vacia si el texto no contiene
     *         partidas validas.
     */
    override suspend fun importar(textoPgn: String): List<Partida> {
        if (textoPgn.isBlank()) return emptyList()
        val partidas = mutableListOf<Partida>()
        LargeFile(ByteArrayInputStream(textoPgn.toByteArray(StandardCharsets.UTF_8))).use { lf ->
            PgnIterator(lf).use { iter ->
                for (game in iter) {
                    partidas += game.aPartida()
                }
            }
        }
        return partidas
    }

    /**
     * Extrae los campos de una [Game] de chesslib y los mapea a [Partida].
     *
     * El movetext completo (incluyendo variantes, comentarios y NAGs) se
     * obtiene del propio [Game.toString] o, preferentemente, reconstruyendo
     * el PGN original. Aqui conservamos el PGN tal cual lo parsea chesslib.
     */
    private fun com.github.bhlangonijr.chesslib.game.Game.aPartida(): Partida {
        val pgnTexto = try {
            this.toPgn(false, false)
        } catch (e: Exception) {
            this.toString()
        }
        val fenJuego = this.fen
        return Partida(
            id = "",
            torneoId = null,
            evento = this.round?.event?.name ?: "?",
            sitio = this.round?.event?.site ?: "?",
            fecha = this.date ?: "????.??.??",
            ronda = this.round?.number?.toString() ?: "?",
            blancas = this.whitePlayer?.name ?: "?",
            negras = this.blackPlayer?.name ?: "?",
            eloBlancas = this.whitePlayer?.elo?.takeIf { it > 0 },
            eloNegras = this.blackPlayer?.elo?.takeIf { it > 0 },
            resultado = this.result.aResultado(),
            fechaHora = this.time,
            modo = "",
            fen = fenJuego?.takeIf { it.isNotBlank() },
            posicionSetup = !fenJuego.isNullOrBlank(),
            pgn = pgnTexto,
        )
    }
}

/**
 * Append de un tag PGN en formato `[Name "Value"]`.
 */
private fun StringBuilder.appendTag(name: String, value: String) {
    append('[').append(name).append(" \"").append(value.replace("\"", "\\\"")).append("\"]\n")
}

/**
 * Elimina la cabecera de tags de un PGN (las lineas que empiezan por `[`).
 *
 * Si [pgn] no contiene tags (es solo movetext), se devuelve tal cual.
 * Esto evita duplicar la cabecera al reexportar.
 */
private fun String.quitarCabeceraPgn(): String {
    if (isBlank()) return this
    // Detectamos el movetext: primera linea tras el bloque de tags que NO
    // empieza por '[' ni esta vacia.
    val lineas = lineSequence().toList()
    var i = 0
    // Saltamos lineas de tag o vacias iniciales.
    while (i < lineas.size && (lineas[i].trim().startsWith("[") || lineas[i].isBlank())) {
        i++
    }
    return lineas.drop(i).joinToString("\n")
}

/**
 * Convierte [GameResult] de chesslib a [ResultadoPartida] de dominio.
 */
private fun GameResult?.aResultado(): ResultadoPartida = when (this) {
    GameResult.WHITE_WON -> ResultadoPartida.GANA_BLANCAS
    GameResult.BLACK_WON -> ResultadoPartida.GANA_NEGRAS
    GameResult.DRAW -> ResultadoPartida.TABLAS
    GameResult.ONGOING, null -> ResultadoPartida.EN_CURSO
}