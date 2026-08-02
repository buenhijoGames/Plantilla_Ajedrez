package com.buenhijogames.plantilla_ajedrez.data.bd.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla `partidas`.
 *
 * Mapea [com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida]. Cada
 * partida queda registrada independientemente y se asocia opcionalmente a un
 * torneo mediante `torneoId` (foreign key). Las partidas sueltas tienen
 * `torneoId = null`.
 *
 * El contenido completo de la partida (jugadas + variantes + comentarios +
 * NAGs) se conserva en `pgn` como PGN interoperable. Las columnas indexadas
 * (`blancas`, `negras`, `fecha`) aceleran las consultas habituales del listado.
 *
 * Cumple el Seven Tag Roster estándar (Event, Site, Date, Round, White, Black,
 * Result) más tags opcionales (WhiteElo, BlackElo, Time, Mode, FEN, SetUp).
 *
 * @property torneoId      FK a [TorneoEntity.id]. Nullable para partidas sueltas.
 * @property evento       Tag Event.
 * @property sitio         Tag Site.
 * @property fecha         Tag Date (`YYYY.MM.DD`, permite `??`).
 * @property ronda         Tag Round.
 * @property blancas       Tag White (`Apellidos, Nombre`).
 * @property negras        Tag Black (`Apellidos, Nombre`).
 * @property eloBlancas    Tag WhiteElo (nullable).
 * @property eloNegras     Tag BlackElo (nullable).
 * @property resultado     Tag Result (`1-0 | 0-1 | 1/2-1/2 | *`).
 * @property fechaHora     Tag Time (`HH:MM:SS`) nullable.
 * @property modo          Tag Mode (`OTB | ICS | online | ...`).
 * @property fen           Tag FEN (posición inicial distinta). Nullable.
 * @property posicionSetup  Tag SetUp (`1` si [fen] != null).
 * @property pgn           PGN completo con variantes y comentarios.
 * @property actualizadoEn Marca temporal epoch millis.
 */
@Entity(
    tableName = "partidas",
    foreignKeys = [
        ForeignKey(
            entity = TorneoEntity::class,
            parentColumns = ["id"],
            childColumns = ["torneo_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["torneo_id"]),
        Index(value = ["blancas"]),
        Index(value = ["negras"]),
        Index(value = ["fecha"]),
    ]
)
data class PartidaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "torneo_id")
    val torneoId: String? = null,

    @ColumnInfo(name = "evento")
    val evento: String,

    @ColumnInfo(name = "sitio")
    val sitio: String,

    @ColumnInfo(name = "fecha")
    val fecha: String,

    @ColumnInfo(name = "ronda")
    val ronda: String = "?",

    @ColumnInfo(name = "blancas")
    val blancas: String,

    @ColumnInfo(name = "negras")
    val negras: String,

    @ColumnInfo(name = "elo_blancas")
    val eloBlancas: Int? = null,

    @ColumnInfo(name = "elo_negras")
    val eloNegras: Int? = null,

    @ColumnInfo(name = "resultado")
    val resultado: String = "*",

    @ColumnInfo(name = "fecha_hora")
    val fechaHora: String? = null,

    @ColumnInfo(name = "modo")
    val modo: String = "",

    @ColumnInfo(name = "fen")
    val fen: String? = null,

    @ColumnInfo(name = "posicion_setup")
    val posicionSetup: Boolean = false,

    @ColumnInfo(name = "pgn")
    val pgn: String = "",

    @ColumnInfo(name = "actualizado_en")
    val actualizadoEn: Long = System.currentTimeMillis(),
)