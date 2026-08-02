package com.buenhijogames.plantilla_ajedrez.data.bd.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.TorneoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de la tabla `torneos`.
 *
 * Cada operación es `suspend` o emite [Flow] para integrarse con corutinas
 * y permitir que la UI observe cambios reactivamente.
 */
@Dao
interface TorneoDao {

    /** Emite todos los torneos ordenados por creación (más recientes primero). */
    @Query(
        """
        SELECT * FROM torneos
        ORDER BY creado_en DESC
        """
    )
    fun observarTodos(): Flow<List<TorneoEntity>>

    /** Devuelve un torneo por id, null si no existe. */
    @Query("SELECT * FROM torneos WHERE id = :id")
    suspend fun obtenerPorId(id: String): TorneoEntity?

    /** Inserta o reemplazo un torneo. El id lo asigna el dominio antes de insertar. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(torneo: TorneoEntity)

    /** Actualiza un torneo existente. */
    @Update
    suspend fun actualizar(torneo: TorneoEntity)

    /** Elimina un torneo por id. Las partidas asociadas se borran en cascada. */
    @Query("DELETE FROM torneos WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    /** Cuenta torneos. Útil para tests y diagnósticos. */
    @Query("SELECT COUNT(*) FROM torneos")
    suspend fun contar(): Int
}