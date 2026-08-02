package com.buenhijogames.plantilla_ajedrez.domain.modelo

/**
 * Resultado estándar de una partida de ajedrez, según el Seven Tag Roster de PGN.
 *
 * Se usa el formato textual de PGN: `1-0` gana Blancas, `0-1` gana Negras,
 * `1/2-1/2` tablas y `*` para PARTIDAS EN CURSO o no finalizadas.
 */
enum class ResultadoPartida(val pgn: String) {
    GANA_BLANCAS(pgn = "1-0"),
    GANA_NEGRAS(pgn = "0-1"),
    TABLAS(pgn = "1/2-1/2"),
    EN_CURSO(pgn = "*");

    companion object {
        /**
         * Parses a PGN-style result string into a [ResultadoPartida].
         * Acepta `*`, `1-0`, `0-1`, `1/2-1/2` (y la alternativamente común `½-½`).
         *
         * @return el valor correspondiente, o [EN_CURSO] si no se reconoce.
         */
        fun desdePgn(valor: String?): ResultadoPartida = when (valor?.trim()) {
            "1-0" -> GANA_BLANCAS
            "0-1" -> GANA_NEGRAS
            "1/2-1/2", "½-½" -> TABLAS
            "*", null, "" -> EN_CURSO
            else -> EN_CURSO
        }
    }
}