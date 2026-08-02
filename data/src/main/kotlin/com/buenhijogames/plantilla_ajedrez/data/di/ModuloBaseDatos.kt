package com.buenhijogames.plantilla_ajedrez.data.di

import android.content.Context
import androidx.room.Room
import com.buenhijogames.plantilla_ajedrez.data.bd.BaseDeDatosPlantilla
import com.buenhijogames.plantilla_ajedrez.data.bd.dao.PartidaDao
import com.buenhijogames.plantilla_ajedrez.data.bd.dao.TorneoDao
import com.buenhijogames.plantilla_ajedrez.data.bd.migraciones.MigracionesPlantilla
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt de la capa de datos.
 *
 * Aquí se construye la [BaseDeDatosPlantilla] como singleton y se exponen los
 * DAOs para inyectarlos en los repositorios.
 *
 * ESTABILIDAD DEL USUARIO
 * -----------------------
 * - Se registran todas las migraciones explícitas con `addMigrations`.
 * - NO se invoca `fallbackToDestructiveMigration`: si falta una migración,
 *   Room lanza `IllegalStateException` en lugar de borrar los datos del
 *   usuario. Es la postura más segura para una app que va a Play Console:
 *   un fallo controlable y detectable en QA antes que una pérdida silenciosa
 *   de partidas en producción.
 */
@Module
@InstallIn(SingletonComponent::class)
object ModuloBaseDatos {

    @Provides
    @Singleton
    fun proporcionarBaseDatos(
        @ApplicationContext contexto: Context,
    ): BaseDeDatosPlantilla =
        Room.databaseBuilder(
            contexto,
            BaseDeDatosPlantilla::class.java,
            BaseDeDatosPlantilla.NOMBRE_BD,
        )
            // Migraciones explícitas: cada nueva versión del esquema debe tener
            // su migración en MigracionesPlantilla.TODAS.
            .addMigrations(*MigracionesPlantilla.TODAS)
            // Sin fallback destructivo: ver comentario de la clase.
            .build()

    @Provides
    @Singleton
    fun proporcionarTorneoDao(bd: BaseDeDatosPlantilla): TorneoDao = bd.torneoDao()

    @Provides
    @Singleton
    fun proporcionarPartidaDao(bd: BaseDeDatosPlantilla): PartidaDao = bd.partidaDao()
}