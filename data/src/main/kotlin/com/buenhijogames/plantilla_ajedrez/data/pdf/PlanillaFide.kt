package com.buenhijogames.plantilla_ajedrez.data.pdf

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida

/**
 * Modelo y logica de presentacion de la plantilla FIDE (pura y testeable).
 *
 * Convierte una [Partida] en una estructura lista para dibujar en el PDF:
 * una [PlantillaFide] con cabecera (Seven Tag Roster + Elos) y una lista de
 * [FilaPlantilla] (numero de jugada + columnas Blancas/Negras) en la que cada
 * jugada SAN se descompone en [SegmentoFigurin] para mostrar el figurin
 * (silueta cburnett) en lugar de la letra de la pieza.
 *
 * Toda la logica de este archivo es puro Kotlin (sin Android) para poderla
 * testear con tests unitarios de JVM; el dibujo real lo hace [AdaptadorPdf].
 */
object PlanillaFide {

    /** Numero maximo de jugadas por hoja (60 jugadas = 30 filas). */
    const val MÁXIMO_JUGADAS_POR_HOJA = 60

    /**
     * Cabecera de la plantilla con los datos de la partida.
     *
     * @property evento Etiqueta `Event` del PGN (nombre del torneo o match).
     * @property sitio Etiqueta `Site` del PGN.
     * @property fecha Etiqueta `Date` del PGN en formato `YYYY.MM.DD`.
     * @property ronda Etiqueta `Round` del PGN.
     * @property blancas Nombre del jugador de blancas.
     * @property negras Nombre del jugador de negras.
     * @property eloBlancas Elo del jugador de blancas (si existe).
     * @property eloNegras Elo del jugador de negras (si existe).
     * @property resultado Resultado final de la partida.
     */
    data class Cabecera(
        val evento: String,
        val sitio: String,
        val fecha: String,
        val ronda: String,
        val blancas: String,
        val negras: String,
        val eloBlancas: Int?,
        val eloNegras: Int?,
        val resultado: ResultadoPartida,
    )

    /**
     * Fila de la tabla de jugadas.
     *
     * @property numero Numero de jugada (1-based).
     * @property blancas Segmentos del figurin de la jugada de blancas (null si no hay).
     * @property negras Segmentos del figurin de la jugada de negras (null si no hay).
     */
    data class Fila(
        val numero: Int,
        val blancas: List<SegmentoFigurin>?,
        val negras: List<SegmentoFigurin>?,
    )

    /** Plantilla FIDE completa lista para dibujar. */
    data class Plantilla(
        val cabecera: Cabecera,
        val filas: List<Fila>,
    )

    /**
     * Construye la [Plantilla] a partir de una [Partida].
     *
     * La lista de jugadas se obtiene de la linea principal del movetext
     * (se ignoran variantes, comentarios, NAGs y el resultado). Solo se
     * consideran las primeras [MÁXIMO_JUGADAS_POR_HOJA] jugadas; si hubiera
     * mas, el resto no se muestra (las plantillas fisicas FIDE suelen tener
     * 60 casillas).
     *
     * @param partida Partida a representar.
     * @return [Plantilla] con cabecera y filas ya segmentadas con figurin.
     */
    fun construir(partida: Partida): Plantilla {
        val sans = sansLineaPrincipal(partida.pgn).take(MÁXIMO_JUGADAS_POR_HOJA)
        val filas = construirFilas(sans)
        return Plantilla(
            cabecera = Cabecera(
                evento = partida.evento,
                sitio = partida.sitio,
                fecha = partida.fecha,
                ronda = partida.ronda,
                blancas = partida.blancas,
                negras = partida.negras,
                eloBlancas = partida.eloBlancas,
                eloNegras = partida.eloNegras,
                resultado = partida.resultado,
            ),
            filas = filas,
        )
    }

    /**
     * Agrupa la lista de SANs en filas Blancas/Negras.
     *
     * Cada dos jugadas se forma una [Fila]: la primera es el movimiento de
     * blancas y la segunda el de negras. Si el numero de jugadas es impar,
     * la ultima fila solo tendra blancas.
     *
     * @param sans Lista de SANs de la linea principal.
     * @return Lista de filas (cada una con su numero de jugada).
     */
    private fun construirFilas(sans: List<String>): List<Fila> {
        val filas = mutableListOf<Fila>()
        for (indice in sans.indices step 2) {
            val numero = (indice / 2) + 1
            val blancas = sans.getOrNull(indice)?.let { segmentar(it, esBlanca = true) }
            val negras = sans.getOrNull(indice + 1)?.let { segmentar(it, esBlanca = false) }
            filas += Fila(numero = numero, blancas = blancas, negras = negras)
        }
        return filas
    }

    /**
     * Extrae las jugadas SAN de la LINEA PRINCIPAL del movetext PGN.
     *
     * Ignora variantes (parentesis), comentarios (llaves y punto y coma),
     * NAGs (`$n`), numeros de jugada, lineas de tag (`[...]`) y el resultado.
     * Es una version minimalista y autonoma de `sansLineaPrincipal` (que vive
     * en `:app`) para que `:data` no dependa de la capa de presentacion.
     *
     * @param movetext Movetext/PGN de la partida.
     * @return Lista de SANs de la linea principal en orden de juego.
     */
    private fun sansLineaPrincipal(movetext: String): List<String> {
        val resultado = mutableListOf<String>()
        val palabra = StringBuilder()
        var profundidadVariante = 0

        fun cerrarPalabra() {
            if (palabra.isEmpty()) return
            val token = palabra.toString()
            palabra.clear()
            if (profundidadVariante > 0) return
            if (esJugadaSan(token)) resultado += token
        }

        var i = 0
        val n = movetext.length
        while (i < n) {
            val c = movetext[i]
            when {
                c.isWhitespace() -> {
                    cerrarPalabra()
                    i++
                }

                c == '(' -> {
                    cerrarPalabra()
                    profundidadVariante++
                    i++
                }

                c == ')' -> {
                    cerrarPalabra()
                    if (profundidadVariante > 0) profundidadVariante--
                    i++
                }

                // Comentario { ... }: se salta hasta la llave de cierre.
                c == '{' -> {
                    cerrarPalabra()
                    val fin = movetext.indexOf('}', i)
                    i = if (fin == -1) n else fin + 1
                }

                // Comentario ;resto de linea.
                c == ';' -> {
                    cerrarPalabra()
                    i = n
                }

                // NAG $n: se ignora.
                c == '$' -> {
                    cerrarPalabra()
                    i++
                }

                // Tag [ ... ]: se ignora completo.
                c == '[' -> {
                    cerrarPalabra()
                    val fin = movetext.indexOf(']', i)
                    i = if (fin == -1) n else fin + 1
                }

                else -> {
                    palabra.append(c)
                    i++
                }
            }
        }
        cerrarPalabra()
        return resultado
    }

    /**
     * Decide si un token es una jugada SAN (no numero, no resultado).
     *
     * @param token Token ya separado del movetext.
     * @return true si parece una jugada SAN.
     */
    private fun esJugadaSan(token: String): Boolean {
        if (token in RESULTADOS_PGN) return false
        // "1.", "12..." -> numero de jugada.
        if (token.matches(PATRON_NUMERO_JUGADA)) return false
        // "1.e4", "12...Nf3" -> despegar el numero.
        val match = PATRON_NUMERO_PEGADO.matchEntire(token)
        return match != null || token.all { it.isLetterOrDigit() || it in "=-+#xOo" }
    }

    /**
     * Descompone una jugada SAN en segmentos [SegmentoFigurin.Texto] y
     * [SegmentoFigurin.Pieza], replicando la segmentacion de la planilla.
     *
     * - "Nxd4" -> [Pieza('N'), Texto("xd4")].
     * - "e4"   -> [Texto("e4")].
     * - "e8=Q" -> [Texto("e8="), Pieza('Q')].
     *
     * @param san      Jugada en notacion SAN.
     * @param esBlanca true si la ejecuta el bando blanco (figurin blanco).
     * @return Lista ordenada de segmentos.
     */
    fun segmentar(san: String, esBlanca: Boolean): List<SegmentoFigurin> {
        val piezaInicial = san.firstOrNull()?.takeIf { it in LETRAS_PIEZA_SAN }
        if (piezaInicial != null) {
            val simbolo = if (esBlanca) piezaInicial else piezaInicial.lowercaseChar()
            return listOf(
                SegmentoFigurin.Pieza(simbolo),
                SegmentoFigurin.Texto(san.substring(1)),
            )
        }
        val indiceIgual = san.indexOf('=')
        if (indiceIgual >= 0) {
            val piezaPromocion = san.getOrNull(indiceIgual + 1)?.takeIf { it in "NBRQ" }
            if (piezaPromocion != null) {
                val simbolo = if (esBlanca) piezaPromocion else piezaPromocion.lowercaseChar()
                return listOf(
                    SegmentoFigurin.Texto(san.substring(0, indiceIgual + 1)),
                    SegmentoFigurin.Pieza(simbolo),
                    SegmentoFigurin.Texto(san.substring(indiceIgual + 2)),
                )
            }
        }
        return listOf(SegmentoFigurin.Texto(san))
    }

    /** Resultados PGN estandar (se ignoran al extraer jugadas). */
    private val RESULTADOS_PGN = setOf("*", "1-0", "0-1", "1/2-1/2")

    /** Patron de un numero de jugada PGN ("1.", "12..."). */
    private val PATRON_NUMERO_JUGADA = Regex("^\\d+\\.+$")

    /** Patron de numero de jugada pegado a su jugada ("1.e4"). */
    private val PATRON_NUMERO_PEGADO = Regex("^(\\d+)(\\.{1,3})(\\S+)$")

    /** Letras de pieza que abren una jugada SAN (N, B, R, Q, K; nunca peon). */
    private const val LETRAS_PIEZA_SAN = "NBRQK"
}

/**
 * Segmento de una jugada SAN para establecer el figurin en el PDF.
 */
sealed interface SegmentoFigurin {

    /**
     * Texto puro de la notacion (destino, captura, jaque, enroque...).
     *
     * @property texto Texto a mostrar tal cual.
     */
    data class Texto(val texto: String) : SegmentoFigurin

    /**
     * Pieza a dibujar como figurin (silueta cburnett).
     *
     * @property simboloFen Caracter FEN de la pieza ('N' blanca, 'n' negra...).
     */
    data class Pieza(val simboloFen: Char) : SegmentoFigurin
}