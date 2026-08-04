package com.buenhijogames.plantilla_ajedrez.ui.torneos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioTorneos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la pantalla de torneos.
 *
 * @property torneos           Lista actual de torneos emitida por el repo.
 * @property cargando          true mientras la lista inicial no ha emitido.
 * @property hayErrorCarga     true si el Flow de lectura lanzó (ver
 *                             [TorneosViewModel]). La UI resuelve el texto
 *                             desde `strings.xml` para no hardcodear.
 * @property dialogoNuevo      true si el formulario de nuevo torneo está abierto.
 */
data class EstadoTorneos(
    val torneos: List<Torneo> = emptyList(),
    val cargando: Boolean = true,
    val hayErrorCarga: Boolean = false,
    val dialogoNuevo: Boolean = false,
)

/**
 * [ViewModel] de la lista de torneos.
 *
 * Un único [MutableStateFlow] actúa como fuente de verdad del estado: el
 * `Flow` de Room lo actualiza en [init] y las acciones de UI ([abrirDialogoNuevo],
 * [crearTorneo], ...) también. Así nunca hay dos estados que diverjan.
 *
 * Errores de lectura se capturan en el [catch] para NO tumbar la app
 * (regla de estabilidad 0% crasheos): el mensaje se expone en
 * [EstadoTorneos.errorCarga] y la lista queda vacía. La escritura
 * ([crearTorneo], [eliminarTorneo]) se lanza en [viewModelScope].
 *
 * Anotado con [HiltViewModel] para recibir [RepositorioTorneos] (binding
 * `@Binds` del ModuloRepositorios de la Fase 1).
 */
@HiltViewModel
class TorneosViewModel @Inject constructor(
    private val repositorio: RepositorioTorneos,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoTorneos(cargando = true))

    /** Estado reactivo expuesto a la UI. */
    val estado: StateFlow<EstadoTorneos> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repositorio.observarTorneos()
                .catch {
                    // No derrumbar la app: marcar el error y continuar.
                    // No exponemos el mensaje técnico al usuario (texto
                    // hardcodeado prohibido); la UI muestra un string genérico.
                    _estado.update { actual ->
                        actual.copy(
                            cargando = false,
                            hayErrorCarga = true,
                        )
                    }
                }
                .collect { lista ->
                    _estado.update { actual ->
                        actual.copy(
                            torneos = lista,
                            cargando = false,
                            hayErrorCarga = false,
                        )
                    }
                }
        }
    }

    /** Abre el formulario de nuevo torneo. */
    fun abrirDialogoNuevo() {
        _estado.update { it.copy(dialogoNuevo = true) }
    }

    /** Cierra el formulario de nuevo torneo sin guardar. */
    fun cerrarDialogoNuevo() {
        _estado.update { it.copy(dialogoNuevo = false) }
    }

    /**
     * Crea un torneo nuevo y lo persiste.
     *
     * El id lo asigna el repositorio (via [com.buenhijogames.plantilla_ajedrez.domain.repositorio.GeneradorIds]
     * si viene vacío). Tras guardar, la lista se actualiza sola vía el
     * Flow observado en [init]. No crea un torneo si [nombre] queda vacío.
     */
    fun crearTorneo(nombre: String, sitio: String, fechaInicio: String) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) return
        viewModelScope.launch {
            repositorio.guardarTorneo(
                Torneo(
                    nombre = nombreLimpio,
                    sitio = sitio.trim(),
                    fechaInicio = fechaInicio.trim(),
                )
            )
            _estado.update { it.copy(dialogoNuevo = false) }
        }
    }

    /** Elimina un torneo por id. */
    fun eliminarTorneo(id: String) {
        viewModelScope.launch {
            repositorio.eliminarTorneo(id)
        }
    }
}