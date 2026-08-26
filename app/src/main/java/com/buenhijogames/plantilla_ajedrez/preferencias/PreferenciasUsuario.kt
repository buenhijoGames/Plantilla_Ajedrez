package com.buenhijogames.plantilla_ajedrez.preferencias

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.buenhijogames.plantilla_ajedrez.ui.theme.TemaAplicacion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Instancia singleton de [DataStore] de preferencias de la app.
 *
 * Se declara como extensión de [Context] para que [ModuloPreferencias]
 * la provea por Hilt sin instanciar manualmente la factoría.
 */
private val Context.dataStorePreferencias: DataStore<Preferences> by preferencesDataStore(
    name = "plantilla_ajedrez_prefs",
)

/**
 * Repositorio de preferencias del usuario respaldado por DataStore.
 *
 * Cada clave se define como [stringPreferencesKey] o [booleanPreferencesKey] resistente a renombres
 * (el nombre de la clave es el contrato con el fichero de disco, no se
 * debe tocar sin añadir migración explícita de preferencias). El value
 * null/missing se traduce a valor por defecto siempre: la lectura nunca
 * debe hacer caer la app.
 */
@Singleton
class PreferenciasUsuario @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * Emite el [TemaAplicacion]. Nunca null: si no hay valor guardado o
     * el guardado no corresponde a ningún tema conocido, se emite [TemaAplicacion.CLARO].
     */
    val tema: Flow<TemaAplicacion> = dataStore.data.map { prefs ->
        TemaAplicacion.desdeNombre(prefs[CLAVE_TEMA])
    }

    /**
     * Emite si los efectos de sonido de jugadas están habilitados (por defecto true).
     */
    val sonidoHabilitado: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CLAVE_SONIDO_HABILITADO] ?: true
    }

    /**
     * Emite los segundos de pausa para la reproducción automática (por defecto 3 segundos, rango 1..60).
     */
    val segundosAuto: Flow<Int> = dataStore.data.map { prefs ->
        prefs[CLAVE_SEGUNDOS_AUTO] ?: 3
    }

    /**
     * Persiste el [tema] elegido por el usuario.
     *
     * Suspended porque DataStore escribe en disco fuera del hilo de UI.
     */
    suspend fun guardarTema(tema: TemaAplicacion) {
        dataStore.edit { it[CLAVE_TEMA] = tema.name }
    }

    /**
     * Persiste la preferencia de efectos de sonido.
     */
    suspend fun guardarSonidoHabilitado(habilitado: Boolean) {
        dataStore.edit { it[CLAVE_SONIDO_HABILITADO] = habilitado }
    }

    /**
     * Persiste la preferencia de segundos de reproducción automática.
     */
    suspend fun guardarSegundosAuto(segundos: Int) {
        dataStore.edit { it[CLAVE_SEGUNDOS_AUTO] = segundos.coerceIn(1, 60) }
    }

    companion object {
        /** Clave estable en disco para el tema. NO renombrar sin migrar. */
        private val CLAVE_TEMA = stringPreferencesKey("tema")

        /** Clave estable en disco para el sonido. NO renombrar sin migrar. */
        private val CLAVE_SONIDO_HABILITADO = booleanPreferencesKey("sonido_habilitado")

        /** Clave estable en disco para los segundos de reproducción automática. NO renombrar sin migrar. */
        private val CLAVE_SEGUNDOS_AUTO = intPreferencesKey("segundos_auto")
    }
}

/**
 * Módulo Hilt que provee la instancia singleton de [DataStore] de
 * preferencias, construida vía la extensión [Context.dataStorePreferencias]
 * a partir del [ApplicationContext].
 */
@Module
@InstallIn(SingletonComponent::class)
object ModuloPreferencias {

    @Provides
    @Singleton
    fun proporcionarDataStore(
        @ApplicationContext contexto: Context,
    ): DataStore<Preferences> = contexto.dataStorePreferencias
}