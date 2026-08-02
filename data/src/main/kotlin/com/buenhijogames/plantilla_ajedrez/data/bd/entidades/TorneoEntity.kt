package com.buenhijogames.plantilla_ajedrez.data.bd.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla `torneos`.
 *
 * Mapea el [com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo] de
 * dominio. Cada columna se documenta para mantener estables las migraciones:
 * añadirlas o renombrarlas exige una migración explícita (ver
 * [com.buenhijogames.plantilla_ajedrez.data.bd.migraciones]).
 *
 * IMPORTANTE para estabilidad del usuario: nunca renombrar ni eliminar
 * columnas en una migración sin aportar una path que preserve los datos.
 *
 * @property id            Clave primaria. Texto (UUID) generado en dominio.
 * @property nombre        Nombre del evento (Tag Event de PGN).
 * @property sitio         Localidad del evento (Tag Site de PGN).
 * @property fechaInicio   Fecha inicio en formato `YYYY-MM-DD` (admite `??`).
 * @property fechaFin      Fecha fin (nullable para torneos de un solo día).
 * @property arbitro       Árbitro del torneo (visible en plantilla).
 * @property notas         Notas internas del usuario (no forman parte de PGN).
 * @property creadoEn      Marca temporal epoch millis para ordenación estable.
 */
@Entity(tableName = "torneos")
data class TorneoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "sitio")
    val sitio: String,

    @ColumnInfo(name = "fecha_inicio")
    val fechaInicio: String,

    @ColumnInfo(name = "fecha_fin")
    val fechaFin: String? = null,

    @ColumnInfo(name = "arbitro")
    val arbitro: String = "",

    @ColumnInfo(name = "notas")
    val notas: String = "",

    @ColumnInfo(name = "creado_en")
    val creadoEn: Long = System.currentTimeMillis(),
)