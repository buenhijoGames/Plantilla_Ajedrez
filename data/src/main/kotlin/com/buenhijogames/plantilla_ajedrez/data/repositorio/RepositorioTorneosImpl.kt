package com.buenhijogames.plantilla_ajedrez.data.repositorio

import com.buenhijogames.plantilla_ajedrez.data.bd.dao.TorneoDao
import com.buenhijogames.plantilla_ajedrez.data.bd.mapeadores.aDominio
import com.buenhijogames.plantilla_ajedrez.data.bd.mapeadores.aEntity
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.GeneradorIds
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.Reloj
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioTorneos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [RepositorioTorneos] respaldada por Room.
 *
 * Responsabilidades:
 *   - Generar un id estable si el [Torneo] llega sin id (nuevo).
 *   - Asignar la marca temporal `creadoEn` mediante el [Reloj] inyectado
 *     (para mantener tests reproducibles).
 *   - Conversiones entity <-> dominio delegadas en [mapeadores].
 *
 * Se anota con [Singleton] porque el DAO subyacente también es singleton y
 * no hay estado propio que justifique instancias múltiples.
 */
@Singleton
class RepositorioTorneosImpl @Inject constructor(
    private val dao: TorneoDao,
    private val generadorIds: GeneradorIds,
    private val reloj: Reloj,
) : RepositorioTorneos {

    /** Emite los torneos ya mapeados a dominio. */
    override fun observarTorneos(): Flow<List<Torneo>> =
        dao.observarTodos().map { lista -> lista.map { it.aDominio() } }

    override suspend fun obtenerTorneo(id: String): Torneo? =
        dao.obtenerPorId(id)?.aDominio()

    override suspend fun guardarTorneo(torneo: Torneo): String {
        val idFinal = torneo.id.ifEmpty { generadorIds.nuevoId() }
        // Mantener `creadoEn` si ya existía (edición) o usar el reloj si es nuevo.
        val existente = dao.obtenerPorId(idFinal)
        val creadoEn = existente?.let { /* preservar el original */ it.creadoEn }
            ?: reloj.ahora()
        val aPersistir = torneo.copy(id = idFinal).aEntity(creadoEn)
        dao.insertar(aPersistir)
        return idFinal
    }

    override suspend fun eliminarTorneo(id: String) {
        dao.eliminarPorId(id)
    }
}