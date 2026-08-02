package com.buenhijogames.plantilla_ajedrez.data.bd.migraciones

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Catálogo centralizado de migraciones de la base de datos.
 *
 * MANIFIESTO DE ESTABILIDAD PARA EL USUARIO
 * -----------------------------------------
 * Nunca se permite que Room destruya datos del usuario ante un cambio de
 * esquema. Por eso la base de datos se configura con
 * `fallbackToDestructiveMigration = false` (ver [BaseDeDatosPlantilla]). Ante
 * un incremento de versión del esquema:
 *   1. Se crea aquí una nueva [Migration] con `from -> to`.
 *   2. Se añade al array [TODAS].
 *   3. Se prueba con una migración desde un snapshot de la versión anterior
 *      (Room Testing `MigrationTestHelper`) para asegurar que los datos
 *      existentes se preservan.
 *
 * Cualquier migración que altere columnas debe usar ALTER TABLE preservando
 * datos (o copying + drop) en lugar de destruir la tabla.
 */
object MigracionesPlantilla {

    /**
     * Todas las migraciones disponibles, ordenadas por versión de origen.
     * Se pasan a Room via `addMigrations(*MigracionesPlantilla.TODAS)`.
     */
    val TODAS: Array<Migration> = arrayOf(
        // Ejemplo de futura migración (no existe en v1):
        // MIGRACION_1_2
    )

    /**
     * Ejemplo de migración de la versión 1 a 2.
     * Se deja comentada como plantilla para futuras extensiones. Si se
     * descomenta, debe añadirse también a [TODAS] y probarse con
     * MigrationTestHelper.
     */
    /*
    val MIGRACION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Ejemplo: añadir columna "lugar" a la tabla torneos preservando datos.
            // db.execSQL("ALTER TABLE torneos ADD COLUMN lugar TEXT NOT NULL DEFAULT ''")
        }
    }
    */
}