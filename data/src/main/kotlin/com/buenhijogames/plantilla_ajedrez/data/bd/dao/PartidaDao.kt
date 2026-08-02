package com.buenhijogames.plantilla_ajedrez.data.bd.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.PartidaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de la tabla `partidas`.
 */
@Dao
interface PartidaDao {

    /** Emite las partidas de un torneo ordenadas por ronda ascendente. */
    @Query(
        """
        SELECT * FROM partidas
        WHERE torneo_id = :torneoId
        ORDER BY ronda ASC, actualizado_en DESC
        """
    )
    fun observarPartidasDelTorneo(torneoId: String): Flow<List<PartidaEntity>>

    /** Emite las partidas sueltas (no asociadas a torneo),más recientes primero. */
    @Query(
        """
        SELECT * FROM partidas
        WHERE torneo_id IS NULL
        ORDER BY actualizado_en DESC
        """
    )
    fun observarPartidasSueltas(): Flow<List<PartidaEntity>>

    /** Devuelve una partida por id, null si no existe. */
    @Query("SELECT * FROM partidas WHERE id = :id")
    suspend fun obtenerPorId(id: String): PartidaEntity?

    /** Inserta una partida (REPLACE para upsert por id). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(partida: PartidaEntity)

    /** Actualiza una partida existente. */
    @Update
    suspend fun actualizar(partida: PartidaEntity)

    /** Elimina una partida por id. */
    @Query("DELETE FROM partidas WHERE id = :id")
    suspend fun eliminarPorId(id: String)
}