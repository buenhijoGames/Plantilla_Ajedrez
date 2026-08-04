package com.buenhijogames.plantilla_ajedrez.ui.torneos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioPartidas
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioTorneos
import com.buenhijogames.plantilla_ajedrez.navegacion.Destinos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la pantalla de detalle de un torneo.
 *
 * @property cargando      true mientras se cargan el torneo y sus partidas.
 * @property torneo        Torneo en detalle (null si no existe).
 * @property partidas      Partidas del torneo observadas desde Room.
 * @property hayErrorCarga true si el torneo no se encontró o el Flow falló.
 * @property creandoPartida true mientras se está creando una partida nueva.
 */
data class EstadoDetalleTorneo(
    val cargando: Boolean = true,
    val torneo: Torneo? = null,
    val partidas: List<Partida> = emptyList(),
    val hayErrorCarga: Boolean = false,
    val creandoPartida: Boolean = false,
)

/**
 * [ViewModel] del detalle de un torneo.
 *
 * Carga el [Torneo] por su id (argumento de navegación) y observa sus
 * partidas vía [RepositorioPartidas.observarPartidasDelTorneo]. Expone
 * [crearPartida] para crear una partida nueva en el torneo y navegar a la
 * pantalla de la partida (el id se devuelve por callback).
 *
 * Los errores de lectura se capturan sin tumbar la app (0% crasheos).
 *
 * @param savedStateHandle    Aporta el id de torneo del argumento de navegación.
 * @param repositorioTorneos  Repositorio de torneos.
 * @param repositorioPartidas Repositorio de partidas.
 */
@HiltViewModel
class DetalleTorneoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repositorioTorneos: RepositorioTorneos,
    private val repositorioPartidas: RepositorioPartidas,
) : ViewModel() {

    private val torneoId: String = checkNotNull(savedStateHandle[Destinos.ARG_TORNEO_ID])

    private val _estado = MutableStateFlow(EstadoDetalleTorneo(cargando = true))

    /** Estado reactivo expuesto a la UI. */
    val estado: StateFlow<EstadoDetalleTorneo> = _estado.asStateFlow()

    init {
        // Torneo en sí (lectura única; si no existe, error sin crash).
        viewModelScope.launch {
            val torneo = repositorioTorneos.obtenerTorneo(torneoId)
            if (torneo == null) {
                _estado.update { it.copy(cargando = false, hayErrorCarga = true) }
                return@launch
            }
            _estado.update { it.copy(torneo = torneo, cargando = false) }
        }
        // Partidas del torneo (reactivo: la lista se actualiza sola).
        viewModelScope.launch {
            repositorioPartidas.observarPartidasDelTorneo(torneoId)
                .catch {
                    // No tumbar la app: marcar error y continuar.
                    _estado.update { it.copy(hayErrorCarga = true) }
                }
                .collect { lista ->
                    _estado.update { it.copy(partidas = lista, cargando = false) }
                }
        }
    }

    /**
     * Crea una partida nueva dentro del torneo y notifica su id por [onCreada].
     *
     * La partida nace con los tags heredados del torneo (evento, sitio,
     * fecha), una ronda correlativa y sin jugadores (se rellenarán en la
     * planilla de la Fase 5). El id lo asigna el repositorio al guardar.
     *
     * @param onCreada Callback con el id de la partida recién creada.
     */
    fun crearPartida(onCreada: (String) -> Unit) {
        val torneo = _estado.value.torneo ?: return
        if (_estado.value.creandoPartida) return
        viewModelScope.launch {
            _estado.update { it.copy(creandoPartida = true) }
            val ronda = (_estado.value.partidas.size + 1).toString()
            val id = repositorioPartidas.guardarPartida(
                Partida(
                    torneoId = torneo.id,
                    evento = torneo.nombre,
                    sitio = torneo.sitio,
                    fecha = torneo.fechaInicio.replace('-', '.'),
                    ronda = ronda,
                    blancas = "",
                    negras = "",
                )
            )
            _estado.update { it.copy(creandoPartida = false) }
            onCreada(id)
        }
    }
}
