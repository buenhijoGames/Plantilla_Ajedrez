package com.buenhijogames.plantilla_ajedrez.domain.modelo

/**
 * Torneo o match: conjunto de partidas registradas como un evento independiente.
 *
 * Cada torneo se registra por separado (no comparte estado con otros). Es
 * una entidad de dominio pura (no conoce Room, ni `:data`, ni Android).
 *
 * @property id            Identificador interno estable; vacío si la entidad
 *                         aún no se ha persistido.
 * @property nombre        Nombre del evento (Tag Event de PGN).
 * @property sitio         Localidad del evento (Tag Site de PGN).
 * @property fechaInicio   Fecha de inicio (ISO-8601 `YYYY-MM-DD` o incompleta).
 * @property fechaFin      Fecha de fin (opcional, para torneos multi-día).
 * @property arbitro       Árbitro si procede (visible en la plantilla PDF).
 * @property notas         Notas internas del usuario, no forman parte de PGN.
 */
data class Torneo(
    val id: String = "",
    val nombre: String,
    val sitio: String = "",
    val fechaInicio: String = "",
    val fechaFin: String? = null,
    val arbitro: String = "",
    val notas: String = "",
)