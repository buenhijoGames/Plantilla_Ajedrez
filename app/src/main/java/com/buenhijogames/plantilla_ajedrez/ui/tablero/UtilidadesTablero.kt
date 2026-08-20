package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.annotation.DrawableRes
import com.buenhijogames.plantilla_ajedrez.R

/**
 * Utilidades puras del tablero de ajedrez.
 *
 * Agrupa la conversión FEN <-> mapa de piezas, las coordenadas de casilla
 * (algebraica <-> fila/columna de Compose) y la serialización del movetext
 * PGN (lista de SANs <-> texto con numeración). Son funciones sin estado y
 * sin dependencias de Android (excepto los ids de recurso de las piezas),
 * por lo que se pueden testear con tests unitarios de JVM.
 */

/**
 * Recursos drawable de cada pieza.
 *
 * @param pieza Carácter FEN: mayúscula = blanca, minúscula = negra.
 *              Solo se esperan los 12 caracteres de pieza ('P','N','B','R','Q','K'
 *              y sus minúsculas); cualquier otro devuelve el peón blanco como
 *              recurso de seguridad (no debería ocurrir con un FEN válido).
 * @return id de recurso [DrawableRes] del VectorDrawable de la pieza.
 */
@DrawableRes
fun recursoPieza(pieza: Char): Int = when (pieza) {
    'K' -> R.drawable.pieza_blanca_rey
    'Q' -> R.drawable.pieza_blanca_dama
    'R' -> R.drawable.pieza_blanca_torre
    'B' -> R.drawable.pieza_blanca_alfil
    'N' -> R.drawable.pieza_blanca_caballo
    'P' -> R.drawable.pieza_blanca_peon
    'k' -> R.drawable.pieza_negra_rey
    'q' -> R.drawable.pieza_negra_dama
    'r' -> R.drawable.pieza_negra_torre
    'b' -> R.drawable.pieza_negra_alfil
    'n' -> R.drawable.pieza_negra_caballo
    'p' -> R.drawable.pieza_negra_peon
    else -> R.drawable.pieza_blanca_peon
}

/**
 * Extrae el mapa `casilla -> pieza` de la sección de posición de un FEN.
 *
 * @param fen FEN completo de la posición (la primera sección es la colocación
 *            de piezas en notación de campos).
 * @return Mapa con casillas en notación algebraica minúscula ("e2") como clave
 *         y el carácter FEN de la pieza como valor. Vacío si el FEN no tiene
 *         las 8 filas esperadas.
 */
fun piezasDesdeFen(fen: String): Map<String, Char> {
    val resultado = mutableMapOf<String, Char>()
    val filas = fen.trim().split(' ').getOrElse(0) { "" }.split('/')
    if (filas.size != 8) return emptyMap()
    for (indiceFila in filas.indices) {
        var columna = 0
        for (car in filas[indiceFila]) {
            when {
                car.isDigit() -> columna += car.digitToInt()
                columna <= 7 -> {
                    val rank = 8 - indiceFila
                    val file = ('a' + columna)
                    resultado["$file$rank"] = car
                    columna++
                }
                else -> columna++
            }
        }
    }
    return resultado
}

/**
 * Devuelve la fila y columna visuales de una casilla.
 *
 * Con [girado] = false (blancas abajo): la fila 0 es la superior (rank 8) y columna 0 la izquierda (file a).
 * Con [girado] = true (negras abajo): la fila 0 es la superior (rank 1) y columna 0 la izquierda (file h).
 *
 * @param casilla Casilla en notación algebraica ("e2").
 * @param girado  true si el tablero está visto desde la perspectiva de negras.
 * @return Par (fila, columna) con valores en el rango 0..7. Si la casilla no
 *         es válida devuelve (0, 0).
 */
fun filaYColumnaDeCasilla(casilla: String, girado: Boolean = false): Pair<Int, Int> {
    val file = casilla.getOrNull(0)?.lowercaseChar() ?: return 0 to 0
    val rank = casilla.getOrNull(1)?.digitToIntOrNull() ?: return 0 to 0
    val columnaNormal = (file - 'a').coerceIn(0, 7)
    val filaNormal = (8 - rank).coerceIn(0, 7)
    return if (girado) {
        (7 - filaNormal) to (7 - columnaNormal)
    } else {
        filaNormal to columnaNormal
    }
}

/**
 * Devuelve la casilla algebraica correspondiente a una fila/columna visuales.
 *
 * @param fila    Fila visual 0..7 (0 = arriba).
 * @param columna Columna visual 0..7 (0 = izquierda).
 * @param girado  true si el tablero está visto desde la perspectiva de negras.
 * @return Casilla en notación algebraica ("e2").
 */
fun casillaDeFilaColumna(fila: Int, columna: Int, girado: Boolean = false): String {
    val f = if (girado) 7 - fila.coerceIn(0, 7) else fila.coerceIn(0, 7)
    val c = if (girado) 7 - columna.coerceIn(0, 7) else columna.coerceIn(0, 7)
    val file = ('a' + c)
    val rank = 8 - f
    return "$file$rank"
}

/**
 * Serializa una lista de movimientos SAN a movetext PGN con numeración.
 *
 * @param sans Lista de jugadas en notación SAN ("e4", "Nf3", ...).
 * @return Movetext tipo "1. e4 e5 2. Nf3 Nc6" (sin resultado al final).
 */
fun movetextDesdeSans(sans: List<String>): String =
    sans.mapIndexed { indice, san ->
        if (indice % 2 == 0) "${(indice / 2) + 1}. $san" else san
    }.joinToString(" ")

/**
 * Patrón de un número de jugada PGN: "1.", "12...", etc.
 */
private val PATRON_NUMERO_JUGADA = Regex("^\\d+\\.+$")

/**
 * Extrae la lista de SANs de un movetext PGN (con o sin numeración).
 *
 * Descarta primero la cabecera de tags ("[Event ...]") si el texto es un PGN
 * completo y después elimina los números de jugada ("1.", "12...") y los
 * resultados ("1-0", "0-1", "1/2-1/2", "*") para quedarse solo con los SANs.
 *
 * Nota: pensada para movetext generado por la propia app (SANs planos, sin
 * comentarios, NAGs ni variantes). El importado/exportado completo con
 * variantes y comentarios lo gestiona el AdaptadorPgn de la capa `:data`.
 *
 * @param movetext Texto con el movetext PGN.
 * @return Lista de SANs en orden de juego.
 */
fun sansDesdeMovetext(movetext: String): List<String> {
    // Saltamos líneas de tag ("[...]") y líneas en blanco de la cabecera.
    val movetextLimpio = movetext.lines()
        .dropWhile { it.trim().startsWith("[") || it.isBlank() }
        .joinToString(" ")
    return movetextLimpio.trim().split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filterNot { PATRON_NUMERO_JUGADA.matches(it) }
        .filterNot { it in RESULTADOS_PGN }
}

/**
 * Resultados PGN estándar para ignorarlos al extraer SANs.
 */
private val RESULTADOS_PGN = setOf("*", "1-0", "0-1", "1/2-1/2")

/**
 * Devuelve el bando al que le toca mover según el FEN.
 *
 * @param fen FEN completo de la posición.
 * @return 'w' (blancas) o 'b' (negras). Si el FEN no tiene sección de turno
 *         devuelve 'w' por defecto.
 */
fun ladoEnTurno(fen: String): Char =
    fen.trim().split(' ').getOrElse(1) { "w" }.getOrElse(0) { 'w' }.let { if (it == 'b') 'b' else 'w' }

/**
 * Segmento de una jugada SAN para su representación gráfica en la planilla.
 *
 * Cada jugada se descompone en una secuencia de segmentos que alternan texto
 * puro ("xd4", "e8=", "+") y piezas dibujables, de modo que en la planilla se
 * muestre el dibujo de la pieza en lugar de su letra inicial.
 */
sealed interface SegmentoSan {

    /**
     * Texto puro de la notación (destino, captura, jaque, enroque...).
     *
     * @property texto Texto a mostrar tal cual.
     */
    data class Texto(val texto: String) : SegmentoSan

    /**
     * Pieza a dibujar como icono.
     *
     * @property simboloFen Carácter FEN de la pieza ('N' blanca, 'n' negra...).
     */
    data class Pieza(val simboloFen: Char) : SegmentoSan
}

/**
 * Letras de pieza que abren una jugada SAN (N, B, R, Q, K; nunca peón).
 */
private const val LETRAS_PIEZA_SAN = "NBRQK"

/**
 * Descompone una jugada SAN en segmentos de texto y piezas dibujables.
 *
 * Reglas aplicadas:
 *   - Si la jugada empieza por N/B/R/Q/K, esa letra se convierte en una pieza
 *     del color indicado y el resto queda como texto ("Nxd4" -> [Pieza('N'),
 *     Texto("xd4")]).
 *   - Si hay promoción ("e8=Q"), la pieza tras '=' se dibuja y el resto
 *     (p. ej. "+") sigue como texto.
 *   - En cualquier otro caso (peón, enroque) toda la jugada es texto.
 *
 * @param san      Jugada en notación SAN.
 * @param esBlanca true si la jugada la ejecutan las blancas.
 * @return Lista ordenada de segmentos que forman la jugada.
 */
fun segmentosDeSan(san: String, esBlanca: Boolean): List<SegmentoSan> {
    val piezaInicial = san.firstOrNull()?.takeIf { it in LETRAS_PIEZA_SAN }
    if (piezaInicial != null) {
        val simbolo = if (esBlanca) piezaInicial else piezaInicial.lowercaseChar()
        return listOf(
            SegmentoSan.Pieza(simbolo),
            SegmentoSan.Texto(san.substring(1)),
        )
    }
    val indiceIgual = san.indexOf('=')
    if (indiceIgual >= 0) {
        val piezaPromocion = san.getOrNull(indiceIgual + 1)?.takeIf { it in "NBRQ" }
        if (piezaPromocion != null) {
            val simbolo = if (esBlanca) piezaPromocion else piezaPromocion.lowercaseChar()
            return listOf(
                SegmentoSan.Texto(san.substring(0, indiceIgual + 1)),
                SegmentoSan.Pieza(simbolo),
                SegmentoSan.Texto(san.substring(indiceIgual + 2)),
            )
        }
    }
    return listOf(SegmentoSan.Texto(san))
}
