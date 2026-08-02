package com.buenhijogames.plantilla_ajedrez.domain.repositorio

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
import kotlinx.coroutines.flow.Flow

/**
 * Puerto de repositorio de torneos.
 *
 * Define el contrato que la capa `:data` debe implementar para persistir
 * torneos. Aquí no aparece ningún tipo de Android ni de Room: cumplimos DIP
 * (Dependencia invertida): la presentación depende de esta interfaz y `:data`
 * aporta la implementación concreta con Room.
 */
interface RepositorioTorneos {

    /** Emite la lista de torneos persistida,Actualizada conforme cambia. */
    fun observarTorneos(): Flow<List<Torneo>>

    /** Devuelve un único torneo por id, o null si no existe. */
    suspend fun obtenerTorneo(id: String): Torneo?

    /** Crea o reemplaza un torneo. Return el id asignado. */
    suspend fun guardarTorneo(torneo: Torneo): String

    /** Elimina un torneo por id. */
    suspend fun eliminarTorneo(id: String)
}