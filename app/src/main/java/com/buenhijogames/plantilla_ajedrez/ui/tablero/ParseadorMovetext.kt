package com.buenhijogames.plantilla_ajedrez.ui.tablero

/**
 * Parser del movetext PGN para la planilla.
 *
 * Convierte el movetext de una partida (con números de jugada, variantes,
 * comentarios, NAGs y resultado) en una estructura navegable de
 * [ElementoMovetext]. Es una utilidad pura, sin dependencias de Android ni
 * de chesslib: solo texto PGN por entrada y estructura de datos por salida.
 *
 * Ejemplo de entrada:
 *   "1. e4 e5 2. Nf3 {buen desarrollo} (2. Bc4) Nc6 1-0"
 *
 * Produce una lista raíz con [ElementoMovetext.Jugada] de la línea principal,
 * [ElementoMovetext.Comentario] para "{...}" y ";", [ElementoMovetext.Variante]
 * para cada paréntesis (anidados incluidos), [ElementoMovetext.Nag] para
 * símbolos "$n" y [ElementoMovetext.Resultado] para el marcador final.
 * Los números de jugada y las líneas de tag "[...]" se descartan.
 */

/**
 * Elemento de movetext PGN para su representación en la planilla.
 */
sealed interface ElementoMovetext {

    /**
     * Jugada de la línea principal en notación SAN.
     *
     * @property san Notación SAN de la jugada ("e4", "Nf3", "Qxd4"...).
     */
    data class Jugada(val san: String) : ElementoMovetext

    /**
     * Variante: línea alternativa de análisis entre paréntesis.
     *
     * @property elementos Jugadas (y sub-elementos) de la variante.
     */
    data class Variante(val elementos: List<ElementoMovetext>) : ElementoMovetext

    /**
     * Comentario de la partida ("{...}" o ";...").
     *
     * @property texto Contenido del comentario sin las llaves ni el ';'.
     */
    data class Comentario(val texto: String) : ElementoMovetext

    /**
     * Símbolo NAG de evaluación ("$1" = "!", "$3" = "!!", etc.).
     *
     * @property codigo Código numérico del NAG.
     */
    data class Nag(val codigo: Int) : ElementoMovetext

    /**
     * Resultado final de la partida ("1-0", "0-1", "1/2-1/2", "*").
     *
     * @property texto Marcador en notación PGN.
     */
    data class Resultado(val texto: String) : ElementoMovetext
}

/**
 * Patrón de un número de jugada PGN: "1.", "12...", etc.
 */
private val PATRON_NUMERO_JUGADA = Regex("^\\d+\\.+$")

/**
 * Patrón de número de jugada pegado a su jugada ("1.e4", "12...Nf3").
 */
private val PATRON_NUMERO_PEGADO = Regex("^(\\d+)(\\.{1,3})(\\S+)$")

/**
 * Resultados PGN estándar.
 */
private val RESULTADOS_PGN = setOf("*", "1-0", "0-1", "1/2-1/2")

/**
 * Parsea el movetext PGN y lo convierte en una lista de [ElementoMovetext].
 *
 * @param movetext Texto con el movetext (con o sin cabecera de tags).
 * @return Lista raíz de elementos en orden de aparición.
 */
fun parsearMovetext(movetext: String): List<ElementoMovetext> {
    val raiz = mutableListOf<ElementoMovetext>()
    // Pila de listas: la última es la variante/raíz actual.
    val pila = mutableListOf(raiz)
    for (token in tokenizarMovetext(movetext)) {
        val destino = pila.last()
        when {
            // Apertura de variante: nueva lista en la pila.
            token == "(" -> pila += mutableListOf<ElementoMovetext>()

            // Cierre de variante: sacar y añadir como Variante al padre.
            token == ")" -> {
                if (pila.size > 1) {
                    val variante = pila.removeAt(pila.size - 1)
                    pila.last() += ElementoMovetext.Variante(variante)
                }
            }

            // Comentarios "{...}" y ";resto de línea".
            token.startsWith("{") || token.startsWith(";") -> destino +=
                ElementoMovetext.Comentario(limpiarComentario(token))

            // NAG "$n".
            token.startsWith("$") -> token.drop(1).toIntOrNull()?.let { destino += ElementoMovetext.Nag(it) }

            // Número de jugada suelto: se descarta (la planilla lo regenera).
            PATRON_NUMERO_JUGADA.matches(token) -> Unit

            // Resultado final.
            token in RESULTADOS_PGN -> destino += ElementoMovetext.Resultado(token)

            // Jugada normal o número de jugada pegado ("1.e4").
            else -> {
                val movimiento = despegarNumero(token) ?: token
                if (movimiento.isNotBlank()) destino += ElementoMovetext.Jugada(movimiento)
            }
        }
    }
    return raiz
}

/**
 * Extrae las jugadas SAN de la LÍNEA PRINCIPAL del movetext.
 *
 * Ignora variantes, comentarios, NAGs, números de jugada y resultado. Sirve
 * para rejugar la posición real de la partida con el motor de ajedrez.
 *
 * @param movetext Texto con el movetext PGN.
 * @return Lista de SANs de la línea principal en orden de juego.
 */
fun sansLineaPrincipal(movetext: String): List<String> =
    parsearMovetext(movetext)
        .filterIsInstance<ElementoMovetext.Jugada>()
        .map { it.san }

/**
 * Devuelve el movetext puro de un PGN descartando la cabecera de tags.
 *
 * @param pgn PGN completo (con o sin cabecera "[Event ...]").
 * @return El movetext (jugadas) sin las líneas de tag. Si no hay cabecera,
 *         devuelve el texto tal cual.
 */
fun movetextSinCabecera(pgn: String): String {
    if (pgn.isBlank()) return ""
    val lineas = pgn.lines()
    val inicioMovetext = lineas.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith("[") }
    return if (inicioMovetext < 0) {
        ""
    } else {
        lineas.drop(inicioMovetext).joinToString("\n").trim()
    }
}

/**
 * Tokeniza el movetext PGN respetando los bloques especiales.
 *
 * Devuelve una lista plana de tokens: palabras sueltas (jugadas, números),
 * "(...)" para paréntesis de variante, el contenido completo de los
 * comentarios "{...}" y ";...", los NAGs "$n" y el resultado. Las líneas de
 * tag "[...]" se descartan por completo (no generan token).
 *
 * @param texto Movetext PGN.
 * @return Lista de tokens en orden de aparición.
 */
private fun tokenizarMovetext(texto: String): List<String> {
    val tokens = mutableListOf<String>()
    val palabra = StringBuilder()
    var i = 0
    val n = texto.length

    fun cerrarPalabra() {
        if (palabra.isNotEmpty()) {
            tokens += palabra.toString()
            palabra.clear()
        }
    }

    while (i < n) {
        val c = texto[i]
        when {
            c.isWhitespace() -> {
                cerrarPalabra()
                i++
            }

            // Comentario con llaves: todo hasta '}' (puede contener espacios).
            c == '{' -> {
                cerrarPalabra()
                val fin = texto.indexOf('}', i)
                val finReal = if (fin == -1) n else fin + 1
                tokens += texto.substring(i, finReal)
                i = finReal
            }

            // Comentario de resto de línea: todo hasta el salto de línea.
            c == ';' -> {
                cerrarPalabra()
                var fin = i
                while (fin < n && texto[fin] != '\n') fin++
                tokens += texto.substring(i, fin)
                i = fin
            }

            c == '(' || c == ')' -> {
                cerrarPalabra()
                tokens += c.toString()
                i++
            }

            // NAG "$n": el '$' seguido de dígitos es un token.
            c == '$' -> {
                cerrarPalabra()
                var fin = i + 1
                while (fin < n && texto[fin].isDigit()) fin++
                tokens += texto.substring(i, fin)
                i = fin
            }

            // Tag "[...]": se descarta completo (cabecera de la partida).
            c == '[' -> {
                cerrarPalabra()
                val fin = texto.indexOf(']', i)
                i = if (fin == -1) n else fin + 1
            }

            else -> {
                palabra.append(c)
                i++
            }
        }
    }
    cerrarPalabra()
    return tokens
}

/**
 * Devuelve el texto de un token de comentario sin su delimitador.
 *
 * @param token Token de comentario ("{texto}" o ";texto").
 * @return Contenido limpio del comentario.
 */
private fun limpiarComentario(token: String): String = when {
    token.startsWith("{") -> token.removePrefix("{").removeSuffix("}").trim()
    else -> token.removePrefix(";").trim()
}

/**
 * Separa el número de jugada de una jugada pegada ("1.e4" -> "e4").
 *
 * @param token Token que puede ser número de jugada + jugada pegadas.
 * @return La jugada sin el número, o null si no había número pegado.
 */
private fun despegarNumero(token: String): String? {
    val match = PATRON_NUMERO_PEGADO.matchEntire(token) ?: return null
    return match.groupValues[3].takeIf { it.isNotEmpty() }
}

/**
 * Devuelve el símbolo textual de un código NAG estándar.
 *
 * Mapea los códigos de evaluación más usados (FIDE) a su símbolo ("!" , "!!",
 * "?!"...). Si el código no está en el catálogo, devuelve su forma "$n".
 *
 * @param codigo Código numérico del NAG.
 * @return Símbolo legible del NAG.
 */
fun simboloNag(codigo: Int): String = when (codigo) {
    1 -> "!"
    2 -> "?"
    3 -> "!!"
    4 -> "??"
    5 -> "!?"
    6 -> "?!"
    10 -> "="
    13 -> "∞"
    14 -> "=∞"
    15 -> "∞="
    16 -> "±"
    17 -> "∓"
    18 -> "+−"
    19 -> "−+"
    20 -> "±"
    21 -> "∓"
    22 -> "+−"
    23 -> "−+"
    24 -> "+−"
    25 -> "−+"
    else -> "\$$codigo"
}
