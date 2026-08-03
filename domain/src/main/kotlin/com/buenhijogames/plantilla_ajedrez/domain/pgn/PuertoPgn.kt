package com.buenhijogames.plantilla_ajedrez.domain.pgn

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida

/**
 * Puerto de importación / exportación PGN.
 *
 * El PGN completo incluye variantes, comentarios y NAGs (estándar definido
 * por Steven J. Edwards, 1994). Este puerto garantiza que las partidas
 * guardadas sean interoperables con cualquier otra app de ajedrez.
 */
interface PuertoPgn {

    /** Construye la representación PGN (export format) de una [partida]. */
    fun exportar(partida: Partida): String

    /**
     * Importa un PGN completo y devuelve una o varias [Partida]es parseadas.
     *
     * Acepta PGN con múltiples partidas: un único .pgn puede contener muchas
     * partidas separadas en bloques de tags + movetext.
     */
    suspend fun importar(textoPgn: String): List<Partida>
}