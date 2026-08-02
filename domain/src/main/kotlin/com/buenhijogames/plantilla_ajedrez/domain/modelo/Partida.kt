package com.buenhijogames.plantilla_ajedrez.domain.modelo

/**
 * Partida individual registrada bajo un [Torneo] (o como partida suelta).
 *
 * Mapea directamente los siete Tag Roster del estándar PGN (Event, Site, Date,
 * Round, White, Black, Result) y añade FEN inicial opcional para partir de
 * una posición distinta a la inicial (por ejemplo, studies o problemas).
 *
 * El contenido completo de la partida (jugadas + variantes + comentarios +
 * NAGs) se conserva en [pgn] como PGN parsable por cualquier aplicación de
 * ajedrez (interoperabilidad bidireccional).
 *
 * @property torneoId      Id del [Torneo] al que pertenece. Puede ser nulo si
 *                         es una partida suelta (no associate a un torneo).
 * @property evento        Tag Event de PGN (nombre del evento).
 * @property sitio         Tag Site de PGN.
 * @property fecha         Tag Date de PGN en formato `YYYY.MM.DD` (`??` wildcards permitidos).
 * @property ronda         Tag Round de PGN (puede ser "1", "2.3", etc.).
 * @property blancas       Jugador con piezas blancas (`Apellidos, Nombre`).
 * @property negras        Jugador con piezas negras (`Apellidos, Nombre`).
 * @property eloBlancas    Elo Chess FIDE (opcional).
 * @property eloNegras     Elo Chess FIDE (opcional).
 * @property resultado     Resultado final (asociado a Tag Result de PGN).
 * @property fechaHora     Hora de comienzo local (Tag Time, `HH:MM:SS`) opcional.
 * @property modo          Modo de juego (Tag Mode: OTB/ICS/online/...). Opcional.
 * @property fen           FEN inicial si la partida NO comienza en la posición estándar
 *                         (Tag FEN). Si [fen] es null, se usa la posición estándar.
 * @property posicionSetup true si [fen] está configurado (equivalente a Tag SetUp = 1).
 * @property pgn           PGN completo (incluyendo variaciones y comentarios). Siempre
 *                         redundante respecto a la lista de Tag Roster; lo suficiente
 *                         para exportar/reimportar sin perder datos.
 */
data class Partida(
    val id: String = "",
    val torneoId: String? = null,
    val evento: String,
    val sitio: String,
    val fecha: String,
    val ronda: String = "?",
    val blancas: String,
    val negras: String,
    val eloBlancas: Int? = null,
    val eloNegras: Int? = null,
    val resultado: ResultadoPartida = ResultadoPartida.EN_CURSO,
    val fechaHora: String? = null,
    val modo: String = "",
    val fen: String? = null,
    val posicionSetup: Boolean = false,
    val pgn: String = "",
)