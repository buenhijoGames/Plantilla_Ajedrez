package com.buenhijogames.plantilla_ajedrez.data.bd

import androidx.room.Database
import androidx.room.RoomDatabase
import com.buenhijogames.plantilla_ajedrez.data.bd.dao.PartidaDao
import com.buenhijogames.plantilla_ajedrez.data.bd.dao.TorneoDao
import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.PartidaEntity
import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.TorneoEntity

/**
 * Base de datos principal de la app.
 *
 * VERSIONADO Y ESTABILIDAD
 * ------------------------
 * - [version] se incrementa en cada cambio de esquema.
 * - Las migraciones explícitas se definen en
 *   [com.buenhijogames.plantilla_ajedrez.data.bd.migraciones.MigracionesPlantilla]
 *   y se registran al construir la instancia (en el módulo Hilt
 *   [com.buenhijogames.plantilla_ajedrez.data.di.ModuloBaseDatos]).
 * - `fallbackToDestructiveMigration = false`: si una migración falta, la
 *   app debe fallar en lugar de borrar los datos del usuario. Esto protege
 *   accidentalmente contra pérdidas en producción.
 * - `fallbackToDestructiveMigrationOnDowngrade = false`: lo mismo para
 *   downgrades (volver a una versión anterior sin migración).
 *
 * @property torneoDao  Acceso a la tabla torneos.
 * @property partidaDao Acceso a la tabla partidas.
 */
@Database(
    entities = [TorneoEntity::class, PartidaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class BaseDeDatosPlantilla : RoomDatabase() {
    abstract fun torneoDao(): TorneoDao
    abstract fun partidaDao(): PartidaDao

    companion object {
        /** Nombre del fichero de la base de datos en el almacenamiento privado. */
        const val NOMBRE_BD = "plantilla_ajedrez.db"
    }
}