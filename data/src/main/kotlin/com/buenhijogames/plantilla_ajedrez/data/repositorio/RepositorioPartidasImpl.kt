package com.buenhijogames.plantilla_ajedrez.data.repositorio

import com.buenhijogames.plantilla_ajedrez.data.bd.dao.PartidaDao
import com.buenhijogames.plantilla_ajedrez.data.bd.mapeadores.aDominio
import com.buenhijogames.plantilla_ajedrez.data.bd.mapeadores.aEntity
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.GeneradorIds
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.Reloj
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioPartidas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [RepositorioPartidas] respaldada por Room.
 *
 * Cada partida se registra independientemente. La FK `torneo_id` permite
 * agruparlas por torneo; si es `null` se considera partida suelta.
 */
@Singleton
class RepositorioPartidasImpl @Inject constructor(
    private val dao: PartidaDao,
    private val generadorIds: GeneradorIds,
    private val reloj: Reloj,
) : RepositorioPartidas {

    override fun observarPartidasDelTorneo(torneoId: String): Flow<List<Partida>> =
        dao.observarPartidasDelTorneo(torneoId).map { lista -> lista.map { it.aDominio() } }

    override fun observarPartidasSueltas(): Flow<List<Partida>> =
        dao.observarPartidasSueltas().map { lista -> lista.map { it.aDominio() } }

    override suspend fun obtenerPartida(id: String): Partida? =
        dao.obtenerPorId(id)?.aDominio()

    override suspend fun guardarPartida(partida: Partida): String {
        val idFinal = partida.id.ifEmpty { generadorIds.nuevoId() }
        val aPersistir = partida.copy(id = idFinal).aEntity(reloj.ahora())
        dao.insertar(aPersistir)
        return idFinal
    }

    override suspend fun eliminarPartida(id: String) {
        dao.eliminarPorId(id)
    }
}