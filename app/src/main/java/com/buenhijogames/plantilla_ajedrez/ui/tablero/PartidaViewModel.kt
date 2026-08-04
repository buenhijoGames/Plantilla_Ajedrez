package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buenhijogames.plantilla_ajedrez.data.ajedrez.JugadaIlegalException
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.motor.PuertoMotorAjedrez
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioPartidas
import com.buenhijogames.plantilla_ajedrez.navegacion.Destinos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Jugada de promoción pendiente de elegir pieza por el usuario.
 *
 * @property desde Casilla origen de la jugada.
 * @property hasta Casilla destino (última fila).
 */
data class JugadaPromocion(
    val desde: String,
    val hasta: String,
)

/**
 * Estado de la pantalla de partida.
 *
 * @property cargando          true mientras se carga la partida de Room.
 * @property fen               FEN actual de la posición sobre el tablero.
 * @property jugadasSan        Lista de jugadas SAN jugadas hasta ahora.
 * @property resultado         Resultado actual de la partida ([ResultadoPartida.EN_CURSO]
 *                             si sigue en juego).
 * @property ladoEnTurno       'w' (blancas) o 'b' (negras): quién debe mover.
 * @property blancas           Nombre del jugador de blancas (puede estar vacío).
 * @property negras            Nombre del jugador de negras (puede estar vacío).
 * @property evento            Nombre del evento (torneo) para la cabecera.
 * @property casillaSeleccionada Casilla seleccionada por el usuario (origen).
 * @property destinosLegales   Casillas destino legales desde [casillaSeleccionada].
 * @property promocionPendiente Jugada esperando a que el usuario elija pieza.
 * @property hayError          true si la partida no se pudo cargar o hubo un
 *                             movimiento ilegal (la UI resuelve el texto).
 */
data class EstadoPartida(
    val cargando: Boolean = true,
    val fen: String = "",
    val jugadasSan: List<String> = emptyList(),
    val resultado: ResultadoPartida = ResultadoPartida.EN_CURSO,
    val ladoEnTurno: Char = 'w',
    val blancas: String = "",
    val negras: String = "",
    val evento: String = "",
    val casillaSeleccionada: String? = null,
    val destinosLegales: List<String> = emptyList(),
    val promocionPendiente: JugadaPromocion? = null,
    val hayError: Boolean = false,
)

/**
 * [ViewModel] de la pantalla de partida.
 *
 * Responsabilidades:
 *   - Cargar la [Partida] (por su id del argumento de navegación) desde
 *     [RepositorioPartidas] y reconstruir la posición rejugando el movetext
 *     almacenado.
 *   - Aplicar jugadas con el [PuertoMotorAjedrez] (validación legal vía
 *     [PuertoMotorAjedrez.jugadasLegalesDesde] y conversión a SAN vía
 *     [PuertoMotorAjedrez.jugadaASan]).
 *   - Gestionar la selección origen -> destino por toques, incluyendo el
 *     diálogo de promoción de peón.
 *   - Persistir automáticamente el movetext y el resultado tras cada jugada
 *     (autosave ligero; el panel de planilla completo llega en la Fase 5).
 *
 * La fuente de verdad es un único [MutableStateFlow]. Las jugadas ilegales
 * ([JugadaIlegalException]) no tumbar la app: se marcan como error de
 * validación en el estado.
 *
 * @param savedStateHandle   Aporta el id de partida del argumento de navegación.
 * @param motor              Puerto del motor de ajedrez (chesslib).
 * @param repositorioPartidas Repositorio de persistencia de partidas.
 */
@HiltViewModel
class PartidaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val motor: PuertoMotorAjedrez,
    private val repositorioPartidas: RepositorioPartidas,
) : ViewModel() {

    private val partidaId: String = checkNotNull(savedStateHandle[Destinos.ARG_PARTIDA_ID])

    /** Datos base de la partida (tags PGN) para persistir sin perderlos. */
    private var partidaBase: Partida? = null

    /** FEN de la posición inicial de la partida (para rejugar/deshacer). */
    private var fenInicio: String = motor.fenInicial()

    private val _estado = MutableStateFlow(EstadoPartida(cargando = true))

    /** Estado reactivo expuesto a la UI. */
    val estado: StateFlow<EstadoPartida> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val partida = repositorioPartidas.obtenerPartida(partidaId)
            if (partida == null) {
                // No existe la partida (borrada o id inválido): error sin crash.
                _estado.update { it.copy(cargando = false, hayError = true) }
                return@launch
            }
            partidaBase = partida
            val sans = sansDesdeMovetext(partida.pgn)
            fenInicio = if (partida.posicionSetup) {
                partida.fen?.takeIf { it.isNotBlank() } ?: motor.fenInicial()
            } else {
                motor.fenInicial()
            }
            val fen = rejugarMovetext(fenInicio, sans)
            _estado.update {
                it.copy(
                    cargando = false,
                    fen = fen,
                    jugadasSan = sans,
                    resultado = motor.resultadoActual(fen),
                    ladoEnTurno = ladoEnTurno(fen),
                    blancas = partida.blancas,
                    negras = partida.negras,
                    evento = partida.evento,
                )
            }
        }
    }

    /**
     * Gestiona una pulsación sobre una casilla del tablero.
     *
     * Reglas:
     *   - Si la partida está cargando o finalizada, se ignora.
     *   - Pulsar la casilla ya seleccionada la deselecciona.
     *   - Con una casilla seleccionada y un destino legal: se ejecuta la
     *     jugada (o se abre la promoción si procede).
     *   - Al tocar una pieza propia con UN solo destino legal, la jugada se
     *     ejecuta directamente (movimiento directo, sin confirmación).
     *   - Al tocar una casilla (vacía o con pieza enemiga) a la que solo puede
     *     llegar UNA pieza propia, esa pieza se mueve directamente.
     *   - En cualquier otro caso, si la casilla tiene pieza propia con varios
     *     destinos legales, se selecciona y se muestran sus destinos.
     *
     * @param casilla Casilla pulsada en notación algebraica ("e2").
     */
    fun onCasillaPulsada(casilla: String) {
        val actual = _estado.value
        if (actual.cargando || actual.resultado != ResultadoPartida.EN_CURSO) return
        if (actual.promocionPendiente != null) return

        val seleccionada = actual.casillaSeleccionada
        when {
            // Pulsar la misma casilla -> deseleccionar.
            seleccionada == casilla -> _estado.update {
                it.copy(casillaSeleccionada = null, destinosLegales = emptyList())
            }

            // Hay origen seleccionado y el destino pulsado es legal -> jugada.
            seleccionada != null && casilla in actual.destinosLegales -> {
                if (esPromocion(actual.fen, seleccionada, casilla)) {
                    _estado.update {
                        it.copy(promocionPendiente = JugadaPromocion(desde = seleccionada, hasta = casilla))
                    }
                } else {
                    realizarJugada(seleccionada, casilla, promocion = null)
                }
            }

            // Casilla con pieza propia movible -> seleccionar y mostrar destinos.
            // Si solo hay UN destino legal, la jugada se ejecuta directamente
            // (movimiento directo): se ahorra el toque de confirmación.
            else -> {
                // Movimiento directo inverso: si pulsamos una casilla a la que
                // solo puede llegar UNA pieza propia, esa pieza se mueve sola.
                val origenUnico = origenUnicoParaDestino(actual.fen, actual.ladoEnTurno, casilla)
                if (origenUnico != null) {
                    if (esPromocion(actual.fen, origenUnico, casilla)) {
                        _estado.update {
                            it.copy(promocionPendiente = JugadaPromocion(desde = origenUnico, hasta = casilla))
                        }
                    } else {
                        realizarJugada(origenUnico, casilla, promocion = null)
                    }
                    return
                }

                val destinos = try {
                    motor.jugadasLegalesDesde(actual.fen, casilla)
                } catch (e: Exception) {
                    emptyList()
                }
                when {
                    destinos.size == 1 -> {
                        val destino = destinos.first()
                        if (esPromocion(actual.fen, casilla, destino)) {
                            _estado.update {
                                it.copy(promocionPendiente = JugadaPromocion(desde = casilla, hasta = destino))
                            }
                        } else {
                            realizarJugada(casilla, destino, promocion = null)
                        }
                    }

                    destinos.isNotEmpty() -> _estado.update {
                        it.copy(casillaSeleccionada = casilla, destinosLegales = destinos)
                    }

                    else -> _estado.update {
                        it.copy(casillaSeleccionada = null, destinosLegales = emptyList())
                    }
                }
            }
        }
    }

    /**
     * Busca la pieza propia que puede mover a una casilla destino dada.
     *
     * Si exactamente UNA pieza del bando en turno tiene [destino] entre sus
     * jugadas legales, se devuelve su casilla de origen; si son varias o
     * ninguna, devuelve null. Permite el movimiento directo "por destino":
     * pulsar una casilla a la que solo puede ir una pieza la mueve sola.
     *
     * @param fen       FEN actual de la posición.
     * @param ladoEnTurno 'w' (blancas) o 'b' (negras): bando que mueve.
     * @param destino   Casilla destino pulsada.
     * @return Origen de la única pieza que puede llegar a [destino], o null.
     */
    private fun origenUnicoParaDestino(fen: String, ladoEnTurno: Char, destino: String): String? {
        val piezasPropias = piezasDesdeFen(fen).filterValues { pieza ->
            if (ladoEnTurno == 'w') pieza.isUpperCase() else pieza.isLowerCase()
        }
        val origenes = piezasPropias.keys.filter { origen ->
            try {
                destino in motor.jugadasLegalesDesde(fen, origen)
            } catch (e: Exception) {
                false
            }
        }
        return origenes.singleOrNull()
    }

    /**
     * Confirma la pieza elegida en el diálogo de promoción y ejecuta la jugada.
     *
     * @param pieza Símbolo de pieza mayúscula ('Q', 'R', 'B' o 'N').
     */
    fun confirmarPromocion(pieza: Char) {
        val pendiente = _estado.value.promocionPendiente ?: return
        realizarJugada(pendiente.desde, pendiente.hasta, promocion = pieza)
    }

    /** Cancela el diálogo de promoción y deselecciona el origen. */
    fun cancelarPromocion() {
        _estado.update {
            it.copy(
                promocionPendiente = null,
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
            )
        }
    }

    /**
     * Ejecuta la jugada origen -> destino con el [PuertoMotorAjedrez].
     *
     * Tras aplicarla actualiza el estado (FEN, lista de SANs, resultado y
     * turno) y persiste el movetext y el resultado en Room. Si la jugada es
     * ilegal (o el FEN deja de ser consistente), se marca [EstadoPartida.hayError]
     * sin tumbar la app.
     *
     * @param desde     Casilla origen.
     * @param hasta     Casilla destino.
     * @param promocion Símbolo de pieza promocionada o null si no aplica.
     */
    private fun realizarJugada(desde: String, hasta: String, promocion: Char?) {
        val actual = _estado.value
        if (actual.cargando) return
        val san = try {
            motor.jugadaASan(actual.fen, desde, hasta, promocion)
        } catch (e: JugadaIlegalException) {
            marcarJugadaIlegal()
            return
        }
        val nuevoFen = try {
            motor.aplicarJugada(actual.fen, san)
        } catch (e: JugadaIlegalException) {
            marcarJugadaIlegal()
            return
        }
        val nuevasJugadas = actual.jugadasSan + san
        val resultado = motor.resultadoActual(nuevoFen)
        _estado.update {
            it.copy(
                fen = nuevoFen,
                jugadasSan = nuevasJugadas,
                resultado = resultado,
                ladoEnTurno = ladoEnTurno(nuevoFen),
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
                hayError = false,
            )
        }
        guardarPartida(nuevasJugadas, resultado)
    }

    /**
     * Deshace la última jugada y vuelve a la posición anterior.
     *
     * Rejuega el movetext desde [fenInicio] sin la última jugada, actualiza
     * el resultado y el turno, y persiste el estado en Room. No tiene efecto
     * si la partida está cargando, no hay jugadas o hay una promoción
     * pendiente de elegir.
     */
    fun deshacerJugada() {
        val actual = _estado.value
        if (actual.cargando || actual.promocionPendiente != null) return
        val jugadasRestantes = actual.jugadasSan.dropLast(1)
        if (jugadasRestantes.size == actual.jugadasSan.size) return
        val fen = rejugarMovetext(fenInicio, jugadasRestantes)
        val resultado = motor.resultadoActual(fen)
        _estado.update {
            it.copy(
                fen = fen,
                jugadasSan = jugadasRestantes,
                resultado = resultado,
                ladoEnTurno = ladoEnTurno(fen),
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
                hayError = false,
            )
        }
        guardarPartida(jugadasRestantes, resultado)
    }

    /**
     * Marca el estado de jugada ilegal y limpia la selección.
     */
    private fun marcarJugadaIlegal() {
        _estado.update {
            it.copy(
                hayError = true,
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
            )
        }
    }

    /**
     * Persiste el movetext y el resultado de la partida en Room.
     *
     * Mantiene los tags base de la partida (evento, sitio, fecha, jugadores).
     * La escritura se lanza en [viewModelScope] y no bloquea la UI.
     *
     * @param jugadas   Lista de SANs a serializar como movetext.
     * @param resultado Resultado actual a guardar.
     */
    private fun guardarPartida(jugadas: List<String>, resultado: ResultadoPartida) {
        val base = partidaBase ?: return
        viewModelScope.launch {
            repositorioPartidas.guardarPartida(
                base.copy(
                    pgn = movetextDesdeSans(jugadas),
                    resultado = resultado,
                )
            )
        }
    }

    /**
     * Comprueba si la jugada desde -> hasta es una promoción de peón.
     *
     * @param fen   FEN actual (para conocer la pieza en [desde]).
     * @param desde Casilla origen.
     * @param hasta Casilla destino.
     * @return true si la pieza es un peón que alcanza la última fila.
     */
    private fun esPromocion(fen: String, desde: String, hasta: String): Boolean {
        val pieza = piezasDesdeFen(fen)[desde] ?: return false
        val ultimaFila = if (pieza == 'P') '8' else if (pieza == 'p') '1' else return false
        return hasta.last() == ultimaFila
    }

    /**
     * Reconstruye la posición aplicando la lista de SANs desde [fenInicio].
     *
     * Si una jugada no se puede aplicar (movetext corrupto), se detiene el
     * rejuego y se devuelve la posición alcanzada hasta entonces.
     *
     * @param fenInicio FEN de partida.
     * @param sans      Lista de jugadas SAN.
     * @return FEN tras rejugar todas las jugadas posibles.
     */
    private fun rejugarMovetext(fenInicio: String, sans: List<String>): String {
        var fen = fenInicio
        for (san in sans) {
            fen = try {
                motor.aplicarJugada(fen, san)
            } catch (e: Exception) {
                break
            }
        }
        return fen
    }
}
