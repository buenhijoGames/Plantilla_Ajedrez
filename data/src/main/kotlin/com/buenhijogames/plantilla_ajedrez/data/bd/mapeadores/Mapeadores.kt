package com.buenhijogames.plantilla_ajedrez.data.bd.mapeadores

import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.PartidaEntity
import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.TorneoEntity
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo

/**
 * Conversión entre las entidades de Room (capa `:data`) y los modelos de
 * dominio (capa `:domain`).
 *
 * Mantener esta lógica aislada en un mapper dedicado:
 *   - Evita que los repositorios pipelines largos de conversión mezclados
 *     con lógica de persistencia (responsabilidad única).
 *   - Centraliza el punto exacto donde un cambio de esquema impacta al
 *     dominio, lo que facilita auditar migraciones.
 *
 * Las marcas temporales (`creadoEn`/`actualizadoEn`) son metadatos de la
 * capa de datos: no se exponen al dominio.
 */

/**
 * Convierte una [TorneoEntity] en su [Torneo] de dominio.
 * No se propaga `creadoEn` (metadato interno de ordenación).
 */
fun TorneoEntity.aDominio(): Torneo = Torneo(
    id = id,
    nombre = nombre,
    sitio = sitio,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    arbitro = arbitro,
    notas = notas,
)

/**
 * Convierte un [Torneo] de dominio en [TorneoEntity] para persistir.
 *
 * @param creadoEn Marca temporal de creación (la asigna el repositorio para
 *                 respetar el generador de reloj inyectable en tests).
 */
fun Torneo.aEntity(creadoEn: Long): TorneoEntity = TorneoEntity(
    id = id,
    nombre = nombre,
    sitio = sitio,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    arbitro = arbitro,
    notas = notas,
    creadoEn = creadoEn,
)

/**
 * Convierte una [PartidaEntity] en su [Partida] de dominio.
 * El [ResultadoPartida] se parsea desde su representación PGN.
 */
fun PartidaEntity.aDominio(): Partida = Partida(
    id = id,
    torneoId = torneoId,
    evento = evento,
    sitio = sitio,
    fecha = fecha,
    ronda = ronda,
    blancas = blancas,
    negras = negras,
    eloBlancas = eloBlancas,
    eloNegras = eloNegras,
    resultado = ResultadoPartida.desdePgn(resultado),
    fechaHora = fechaHora,
    modo = modo,
    fen = fen,
    posicionSetup = posicionSetup,
    pgn = pgn,
)

/**
 * Convierte una [Partida] de dominio en [PartidaEntity] para persistir.
 *
 * @param actualizadoEn Marca temporal de actualización (la asigna el
 *                      repositorio para respetar el reloj inyectable).
 */
fun Partida.aEntity(actualizadoEn: Long): PartidaEntity = PartidaEntity(
    id = id,
    torneoId = torneoId,
    evento = evento,
    sitio = sitio,
    fecha = fecha,
    ronda = ronda,
    blancas = blancas,
    negras = negras,
    eloBlancas = eloBlancas,
    eloNegras = eloNegras,
    resultado = resultado.pgn,
    fechaHora = fechaHora,
    modo = modo,
    fen = fen,
    posicionSetup = posicionSetup,
    pgn = pgn,
    actualizadoEn = actualizadoEn,
)