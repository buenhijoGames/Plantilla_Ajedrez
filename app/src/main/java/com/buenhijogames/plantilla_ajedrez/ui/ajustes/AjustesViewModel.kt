package com.buenhijogames.plantilla_ajedrez.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buenhijogames.plantilla_ajedrez.preferencias.PreferenciasUsuario
import com.buenhijogames.plantilla_ajedrez.ui.theme.TemaAplicacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de UI de la pantalla de ajustes.
 *
 * Hoy sólo transporta el [temaSeleccionado]. Se modela como data class
 * para poder añadir más campos (tipo de pieza, idioma, etc.) sin cambiar
 * la firma del `StateFlow` ni los puntos de recolección en la UI.
 */
data class EstadoAjustes(
    val temaSeleccionado: TemaAplicacion = TemaAplicacion.CLARO,
)

/**
 * [ViewModel] de la pantalla de Ajustes.
 *
 * Lee el tema desde [PreferenciasUsuario] reactivamente y lo expone como
 * [StateFlow] para que la UI recomponga al cambiar. Expone [seleccionarTema]
 * para persistir la nueva elección: la escritura se lanza en
 * [viewModelScope] y no bloquea la UI.
 *
 * Anotado con [HiltViewModel] para recibir [PreferenciasUsuario] por
 * constructor injection. La grapa con la Activity la hace
 * `androidx.hilt:hilt-navigation-compose` (`hiltViewModel()`).
 */
@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val preferencias: PreferenciasUsuario,
) : ViewModel() {

    /**
     * Estado reactivo de la pantalla. Se arranca con [EstadoAjustes] por
     * defecto (tema CLARO) y se actualiza en cuanto DataStore emite el
     * primer valor (casi siempre inmediato).
     */
    val estado: StateFlow<EstadoAjustes> = preferencias.tema
        .map { EstadoAjustes(temaSeleccionado = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = EstadoAjustes(),
        )

    /**
     * Persiste el tema elegido por el usuario.
     */
    fun seleccionarTema(tema: TemaAplicacion) {
        viewModelScope.launch {
            preferencias.guardarTema(tema)
        }
    }
}