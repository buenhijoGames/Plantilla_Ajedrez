package com.buenhijogames.plantilla_ajedrez.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buenhijogames.plantilla_ajedrez.preferencias.PreferenciasUsuario
import com.buenhijogames.plantilla_ajedrez.ui.theme.TemaAplicacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de UI de la pantalla de ajustes.
 *
 * Transporta el [temaSeleccionado] y si los efectos de sonido están habilitados.
 */
data class EstadoAjustes(
    val temaSeleccionado: TemaAplicacion = TemaAplicacion.CLARO,
    val sonidoHabilitado: Boolean = true,
)

/**
 * [ViewModel] de la pantalla de Ajustes.
 *
 * Lee el tema y el sonido desde [PreferenciasUsuario] reactivamente y los expone como
 * [StateFlow] para que la UI recomponga al cambiar.
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
     * defecto (tema CLARO y sonido activo) y se actualiza en cuanto DataStore emite.
     */
    val estado: StateFlow<EstadoAjustes> = combine(
        preferencias.tema,
        preferencias.sonidoHabilitado,
    ) { tema, sonido ->
        EstadoAjustes(
            temaSeleccionado = tema,
            sonidoHabilitado = sonido,
        )
    }.stateIn(
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

    /**
     * Persiste si los efectos de sonido están habilitados.
     */
    fun alternarSonido(habilitado: Boolean) {
        viewModelScope.launch {
            preferencias.guardarSonidoHabilitado(habilitado)
        }
    }
}