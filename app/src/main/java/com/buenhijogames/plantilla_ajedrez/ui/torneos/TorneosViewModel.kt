package com.buenhijogames.plantilla_ajedrez.ui.torneos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
import com.buenhijogames.plantilla_ajedrez.domain.pgn.PuertoPgn
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioPartidas
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioTorneos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la pantalla de torneos y partidas sueltas.
 *
 * @property torneos              Lista actual de torneos emitida por el repo.
 * @property partidasSueltas      Lista actual de partidas sueltas (sin torneo).
 * @property cargando             true mientras la lista inicial no ha emitido.
 * @property hayErrorCarga        true si el Flow de lectura lanzó.
 * @property dialogoNuevoTorneo   true si el formulario de nuevo torneo está abierto.
 * @property dialogoNuevaPartida  true si el formulario de nueva partida suelta está abierto.
 * @property menuCrearAbierto     true si el selector de creación (torneo / partida suelta) está visible.
 * @property partidaCreadaId      Id de la partida recién creada para navegar (null tras navegar).
 * @property importandoPgn        true mientras se está importando un PGN.
 * @property resultadoImportacion Número de partidas importadas (null si no hay importación reciente).
 */
data class EstadoTorneos(
    val torneos: List<Torneo> = emptyList(),
    val partidasSueltas: List<Partida> = emptyList(),
    val cargando: Boolean = true,
    val hayErrorCarga: Boolean = false,
    val dialogoNuevoTorneo: Boolean = false,
    val dialogoNuevaPartida: Boolean = false,
    val menuCrearAbierto: Boolean = false,
    val partidaCreadaId: String? = null,
    val importandoPgn: Boolean = false,
    val resultadoImportacion: Int? = null,
)

/**
 * [ViewModel] de la pantalla principal (torneos y partidas sueltas).
 */
@HiltViewModel
class TorneosViewModel @Inject constructor(
    private val repositorio: RepositorioTorneos,
    private val repositorioPartidas: RepositorioPartidas,
    private val generadorPgn: PuertoPgn,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoTorneos(cargando = true))

    /** Estado reactivo expuesto a la UI. */
    val estado: StateFlow<EstadoTorneos> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repositorio.observarTorneos(),
                repositorioPartidas.observarPartidasSueltas(),
            ) { torneos, partidas ->
                torneos to partidas
            }
                .catch {
                    _estado.update { actual ->
                        actual.copy(
                            cargando = false,
                            hayErrorCarga = true,
                        )
                    }
                }
                .collect { (torneos, partidas) ->
                    _estado.update { actual ->
                        actual.copy(
                            torneos = torneos,
                            partidasSueltas = partidas,
                            cargando = false,
                            hayErrorCarga = false,
                        )
                    }
                }
        }
    }

    /** Abre el selector de tipo de elemento a crear (+). */
    fun abrirMenuCrear() {
        _estado.update { it.copy(menuCrearAbierto = true) }
    }

    /** Cierra el selector de tipo de elemento a crear. */
    fun cerrarMenuCrear() {
        _estado.update { it.copy(menuCrearAbierto = false) }
    }

    /** Abre el formulario de nuevo torneo. */
    fun abrirDialogoNuevoTorneo() {
        _estado.update { it.copy(dialogoNuevoTorneo = true, menuCrearAbierto = false) }
    }

    /** Cierra el formulario de nuevo torneo sin guardar. */
    fun cerrarDialogoNuevoTorneo() {
        _estado.update { it.copy(dialogoNuevoTorneo = false) }
    }

    /** Abre el formulario de nueva partida suelta. */
    fun abrirDialogoNuevaPartida() {
        _estado.update { it.copy(dialogoNuevaPartida = true, menuCrearAbierto = false) }
    }

    /** Cierra el formulario de nueva partida suelta sin guardar. */
    fun cerrarDialogoNuevaPartida() {
        _estado.update { it.copy(dialogoNuevaPartida = false) }
    }

    /**
     * Crea un torneo nuevo y lo persiste.
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
            _estado.update { it.copy(dialogoNuevoTorneo = false) }
        }
    }

    /**
     * Crea una partida suelta (sin torneo) y la persiste.
     */
    fun crearPartidaSuelta(datos: DatosNuevaPartida) {
        viewModelScope.launch {
            val id = repositorioPartidas.guardarPartida(
                Partida(
                    torneoId = null,
                    evento = datos.evento.ifBlank { "Partida suelta" },
                    sitio = datos.sitio,
                    fecha = datos.fecha,
                    ronda = datos.ronda,
                    blancas = datos.blancas,
                    negras = datos.negras,
                    eloBlancas = datos.eloBlancas,
                    eloNegras = datos.eloNegras,
                )
            )
            _estado.update { it.copy(dialogoNuevaPartida = false, partidaCreadaId = id) }
        }
    }

    /** Limpia el id de partida creada tras navegar. */
    fun limpiarPartidaCreadaId() {
        _estado.update { it.copy(partidaCreadaId = null) }
    }

    /** Elimina un torneo por id. */
    fun eliminarTorneo(id: String) {
        viewModelScope.launch {
            repositorio.eliminarTorneo(id)
        }
    }

    /** Elimina una partida suelta por id. */
    fun eliminarPartidaSuelta(id: String) {
        viewModelScope.launch {
            repositorioPartidas.eliminarPartida(id)
        }
    }

    /**
     * Importa un texto PGN y guarda las partidas resultantes como partidas
     * sueltas (sin torneo asociado).
     *
     * Cada partida importada se guarda con `torneoId = null`. El estado se
     * actualiza con el número de partidas importadas para que la UI pueda
     * mostrar feedback.
     *
     * @param textoPgn Contenido del archivo PGN a importar.
     */
    fun importarPgn(textoPgn: String) {
        if (_estado.value.importandoPgn) return
        viewModelScope.launch {
            _estado.update { it.copy(importandoPgn = true, resultadoImportacion = null) }
            try {
                val partidasImportadas = generadorPgn.importar(textoPgn)
                if (partidasImportadas.isEmpty()) {
                    _estado.update { it.copy(importandoPgn = false, resultadoImportacion = 0) }
                    return@launch
                }
                for (partida in partidasImportadas) {
                    repositorioPartidas.guardarPartida(
                        partida.copy(torneoId = null)
                    )
                }
                _estado.update {
                    it.copy(
                        importandoPgn = false,
                        resultadoImportacion = partidasImportadas.size,
                    )
                }
            } catch (e: Exception) {
                _estado.update { it.copy(importandoPgn = false, resultadoImportacion = 0) }
            }
        }
    }

    /**
     * Limpia el resultado de importación después de mostrar el feedback.
     */
    fun limpiarResultadoImportacion() {
        _estado.update { it.copy(resultadoImportacion = null) }
    }
}