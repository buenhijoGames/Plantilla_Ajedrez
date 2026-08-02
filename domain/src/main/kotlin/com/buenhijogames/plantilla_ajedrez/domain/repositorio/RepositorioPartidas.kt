package com.buenhijogames.plantilla_ajedrez.domain.repositorio

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import kotlinx.coroutines.flow.Flow

/**
 * Puerto de repositorio de partidas.
 *
 * Cada partida queda registrada independientemente, asociada opcionalmente a
 * un torneo ([Partida.torneoId]) para soportar tanto partidas sueltas como
 * partidas de un torneo/match.
 */
interface RepositorioPartidas {

    /** Emite las partidas de un torneo, ordenadas por ronda. */
    fun observarPartidasDelTorneo(torneoId: String): Flow<List<Partida>>

    /** Devuelve todas las partidas sueltas (torneoId == null). */
    fun observarPartidasSueltas(): Flow<List<Partida>>

    /** Devuelve una partida por id, o null si no existe. */
    suspend fun obtenerPartida(id: String): Partida?

    /** Crea o reemplaza una partida. Retorna el id asignado. */
    suspend fun guardarPartida(partida: Partida): String

    /** Elimina una partida por id. */
    suspend fun eliminarPartida(id: String)
}