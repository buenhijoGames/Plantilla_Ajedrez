package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buenhijogames.plantilla_ajedrez.data.ajedrez.JugadaIlegalException
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.motor.PuertoMotorAjedrez
import com.buenhijogames.plantilla_ajedrez.domain.pdf.PuertoPdf
import com.buenhijogames.plantilla_ajedrez.domain.pgn.PuertoPgn
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
 * @property fen               FEN de la posición final (última jugada aplicada).
 * @property fenVisible        FEN que se muestra en el tablero: coincide con [fen]
 *                             salvo cuando se está revisando o editando.
 * @property movetext          Movetext PGN completo a mostrar en la planilla.
 * @property jugadasSan        Lista de jugadas SAN (línea principal) jugadas.
 * @property resultado         Resultado real de la partida.
 * @property resultadoVisible  Resultado de la posición visible (para revisión).
 * @property ladoEnTurno       'w' (blancas) o 'b' (negras): quién debe mover.
 * @property ladoEnTurnoVisible Bando al que le toca en la posición visible.
 * @property blancas           Nombre del jugador de blancas (puede estar vacío).
 * @property negras            Nombre del jugador de negras (puede estar vacío).
  * @property evento            Nombre del evento (torneo) para la cabecera.
  * @property sitio             Sitio del evento (Tag Site del PGN).
  * @property fecha             Fecha del evento (Tag Date del PGN).
  * @property ronda             Ronda de la partida (Tag Round del PGN).
  * @property eloBlancas        Elo del jugador de blancas (si existe).
  * @property eloNegras         Elo del jugador de negras (si existe).
  * @property casillaSeleccionada Casilla seleccionada por el usuario (origen).
 * @property destinosLegales   Casillas destino legales desde [casillaSeleccionada].
 * @property promocionPendiente Jugada esperando a que el usuario elija pieza.
 * @property caminoVisible     Camino hasta la jugada visible al revisar la
 *                             partida, o null si se muestra la posición final.
 * @property modoEdicion       true si el tablero está en modo edición de
 *                             anotaciones y variantes.
 * @property caminoSeleccion   Camino de la jugada seleccionada en modo edición
 *                             (a la que se le añaden comentario, NAG y variantes).
 * @property comentarioEdicion Comentario en edición de la jugada seleccionada.
 * @property nagEdicion        NAG en edición de la jugada seleccionada.
 * @property varianteEnConstruccion Camino de la variante que se está creando en
 *                                  modo edición (null si aún no se ha jugado).
 * @property hayError          true si la partida no se pudo cargar o hubo un
 *                             movimiento ilegal (la UI resuelve el texto).
 */
data class EstadoPartida(
    val cargando: Boolean = true,
    val fen: String = "",
    val fenVisible: String = "",
    val movetext: String = "",
    val jugadasSan: List<String> = emptyList(),
    val resultado: ResultadoPartida = ResultadoPartida.EN_CURSO,
    val resultadoVisible: ResultadoPartida = ResultadoPartida.EN_CURSO,
    val ladoEnTurno: Char = 'w',
    val ladoEnTurnoVisible: Char = 'w',
    val blancas: String = "",
    val negras: String = "",
    val evento: String = "",
    val sitio: String = "",
    val fecha: String = "",
    val ronda: String = "",
    val eloBlancas: Int? = null,
    val eloNegras: Int? = null,
    val casillaSeleccionada: String? = null,
    val destinosLegales: List<String> = emptyList(),
    val promocionPendiente: JugadaPromocion? = null,
    val caminoVisible: CaminoPlanilla? = null,
    val modoEdicion: Boolean = false,
    val caminoSeleccion: CaminoPlanilla? = null,
    val comentarioEdicion: String = "",
    val nagEdicion: Int? = null,
    val varianteEnConstruccion: CaminoPlanilla? = null,
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
 *  - Gestionar la selección origen -> destino por toques, incluyendo el
 *     diálogo de promoción de peón.
 *  - Navegar por la partida: tocar una jugada de la planilla (línea principal
 *     o variante) muestra esa posición en el tablero; volver al final
 *     desbloquea el juego.
 *  - Modo edición (botón "Editar"): seleccionar una jugada y editar su
 *     comentario y NAG, y añadir variantes/subvariantes jugando en el tablero
 *     (se acumulan sin límite de profundidad).
 *  - Persistir automáticamente el movetext y el resultado tras cada cambio
 *     (jugadas nuevas, deshacer, anotaciones o variantes).
 *
 * La fuente de verdad es un único [MutableStateFlow]. Las jugadas ilegales
 * ([JugadaIlegalException]) no tumbar la app: se marcan como error de
 * validación en el estado.
 *
 * @param savedStateHandle   Aporta el id de partida del argumento de navegación.
 * @param motor              Puerto del motor de ajedrez (chesslib).
 * @param repositorioPartidas Repositorio de persistencia de partidas.
 * @param generadorPdf       Puerto de generación de plantillas PDF (FIDE).
 * @param generadorPgn       Puerto de importación/exportación PGN.
 */
@HiltViewModel
class PartidaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val motor: PuertoMotorAjedrez,
    private val repositorioPartidas: RepositorioPartidas,
    private val generadorPdf: PuertoPdf,
    private val generadorPgn: PuertoPgn,
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
            val movetext = movetextSinCabecera(partida.pgn)
            val sans = sansLineaPrincipal(movetext)
            fenInicio = if (partida.posicionSetup) {
                partida.fen?.takeIf { it.isNotBlank() } ?: motor.fenInicial()
            } else {
                motor.fenInicial()
            }
            val fen = rejugarMovetext(fenInicio, sans)
            val resultado = motor.resultadoActual(fen)
            _estado.update {
                it.copy(
                    cargando = false,
                    fen = fen,
                    fenVisible = fen,
                    movetext = movetext,
                    jugadasSan = sans,
                    resultado = resultado,
                    resultadoVisible = resultado,
                    ladoEnTurno = ladoEnTurno(fen),
                    ladoEnTurnoVisible = ladoEnTurno(fen),
                    blancas = partida.blancas,
                    negras = partida.negras,
                    evento = partida.evento,
                    sitio = partida.sitio,
                    fecha = partida.fecha,
                    ronda = partida.ronda,
                    eloBlancas = partida.eloBlancas,
                    eloNegras = partida.eloNegras,
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
     * En modo edición el tablero sirve para crear variantes sobre la jugada
     * seleccionada; el resto del flujo (selección, destino, promoción) es
     * idéntico pero opera sobre la posición visible.
     *
     * @param casilla Casilla pulsada en notación algebraica ("e2").
     */
    fun onCasillaPulsada(casilla: String) {
        val actual = _estado.value
        if (actual.cargando) return
        if (actual.promocionPendiente != null) return

        // En modo edición las jugadas del tablero construyen variantes.
        if (actual.modoEdicion) {
            gestionarSeleccion(
                casilla = casilla,
                fenContexto = actual.fenVisible,
                ladoContexto = actual.ladoEnTurnoVisible,
                onJugar = ::realizarJugadaEdicion,
            )
            return
        }

        if (actual.resultado != ResultadoPartida.EN_CURSO) return
        // Mientras se revisa una posición pasada, el tablero solo muestra:
        // no se permiten jugadas nuevas.
        if (actual.caminoVisible != null) return

        gestionarSeleccion(
            casilla = casilla,
            fenContexto = actual.fen,
            ladoContexto = actual.ladoEnTurno,
            onJugar = ::realizarJugada,
        )
    }

    /**
     * Gestiona la selección origen -> destino por toques en un contexto dado.
     *
     * Comparte toda la lógica táctil (deseleccionar, jugar a un destino legal,
     * movimiento directo por origen y por destino, promoción) entre el juego
     * normal de la línea principal y el modo edición de variantes. Al final
     * delega la ejecución de la jugada en [onJugar].
     *
     * @param casilla      Casilla pulsada.
     * @param fenContexto  FEN de la posición sobre la que se juega.
     * @param ladoContexto Bando al que le toca en esa posición.
     * @param onJugar      Callback (desde, hasta, promocion) que ejecuta la jugada.
     */
    private fun gestionarSeleccion(
        casilla: String,
        fenContexto: String,
        ladoContexto: Char,
        onJugar: (String, String, Char?) -> Unit,
    ) {
        val actual = _estado.value
        val seleccionada = actual.casillaSeleccionada
        when {
            // Pulsar la misma casilla -> deseleccionar.
            seleccionada == casilla -> _estado.update {
                it.copy(casillaSeleccionada = null, destinosLegales = emptyList())
            }

            // Hay origen seleccionado y el destino pulsado es legal -> jugada.
            seleccionada != null && casilla in actual.destinosLegales -> {
                if (esPromocion(fenContexto, seleccionada, casilla)) {
                    _estado.update {
                        it.copy(promocionPendiente = JugadaPromocion(desde = seleccionada, hasta = casilla))
                    }
                } else {
                    onJugar(seleccionada, casilla, null)
                }
            }

            // Casilla con pieza propia movible -> seleccionar y mostrar destinos.
            // Si solo hay UN destino legal, la jugada se ejecuta directamente
            // (movimiento directo): se ahorra el toque de confirmación.
            else -> {
                // Movimiento directo inverso: si pulsamos una casilla a la que
                // solo puede llegar UNA pieza propia, esa pieza se mueve sola.
                val origenUnico = origenUnicoParaDestino(fenContexto, ladoContexto, casilla)
                if (origenUnico != null) {
                    if (esPromocion(fenContexto, origenUnico, casilla)) {
                        _estado.update {
                            it.copy(promocionPendiente = JugadaPromocion(desde = origenUnico, hasta = casilla))
                        }
                    } else {
                        onJugar(origenUnico, casilla, null)
                    }
                    return
                }

                val destinos = try {
                    motor.jugadasLegalesDesde(fenContexto, casilla)
                } catch (e: Exception) {
                    emptyList()
                }
                when {
                    destinos.size == 1 -> {
                        val destino = destinos.first()
                        if (esPromocion(fenContexto, casilla, destino)) {
                            _estado.update {
                                it.copy(promocionPendiente = JugadaPromocion(desde = casilla, hasta = destino))
                            }
                        } else {
                            onJugar(casilla, destino, null)
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
     * En modo edición la promoción se aplica a la variante en construcción;
     * en juego normal, a la línea principal.
     *
     * @param pieza Símbolo de pieza mayúscula ('Q', 'R', 'B' o 'N').
     */
    fun confirmarPromocion(pieza: Char) {
        val pendiente = _estado.value.promocionPendiente ?: return
        if (_estado.value.modoEdicion) {
            realizarJugadaEdicion(pendiente.desde, pendiente.hasta, promocion = pieza)
        } else {
            realizarJugada(pendiente.desde, pendiente.hasta, promocion = pieza)
        }
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
        // Se conserva el movetext anotado (comentarios, NAGs y variantes)
        // añadiendo la jugada nueva al final en lugar de regenerarlo desde cero.
        val nuevoMovetext = agregarJugadaAlMovetext(actual.movetext, san)
        _estado.update {
            it.copy(
                fen = nuevoFen,
                fenVisible = nuevoFen,
                movetext = nuevoMovetext,
                jugadasSan = nuevasJugadas,
                resultado = resultado,
                resultadoVisible = resultado,
                ladoEnTurno = ladoEnTurno(nuevoFen),
                ladoEnTurnoVisible = ladoEnTurno(nuevoFen),
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
                caminoVisible = null,
                caminoSeleccion = null,
                varianteEnConstruccion = null,
                modoEdicion = false,
                hayError = false,
            )
        }
        guardarPartida(nuevoMovetext, resultado)
    }

    /**
     * Ejecuta una jugada de variante en modo edición.
     *
     * La jugada se calcula sobre la posición visible (fenVisible), que en modo
     * edición es la posición previa a la jugada seleccionada más las jugadas de
     * variante ya jugadas. La primera jugada crea una variante pegada a la
     * jugada seleccionada; las siguientes extienden esa variante. Se persiste
     * el movetext actualizado en cada jugada (autosave).
     *
     * @param desde     Casilla origen.
     * @param hasta     Casilla destino.
     * @param promocion Símbolo de pieza promocionada o null si no aplica.
     */
    private fun realizarJugadaEdicion(desde: String, hasta: String, promocion: Char?) {
        val actual = _estado.value
        val caminoSeleccion = actual.caminoSeleccion ?: return
        val san = try {
            motor.jugadaASan(actual.fenVisible, desde, hasta, promocion)
        } catch (e: JugadaIlegalException) {
            marcarJugadaIlegal()
            return
        }
        val nuevoFen = try {
            motor.aplicarJugada(actual.fenVisible, san)
        } catch (e: JugadaIlegalException) {
            marcarJugadaIlegal()
            return
        }
        // Primera jugada: crea la variante. Siguientes: la extienden.
        val (nuevoMovetext, nuevaVariante) = if (actual.varianteEnConstruccion == null) {
            val indiceNueva = numeroDeVariantesPegadas(actual.movetext, caminoSeleccion)
            val insertado = insertarVarianteEnCamino(actual.movetext, caminoSeleccion, listOf(san))
            insertado to (caminoSeleccion + PasoCamino.EntrarVariante(indiceNueva))
        } else {
            val extendido = agregarJugadaAVarianteEnCamino(
                actual.movetext,
                actual.varianteEnConstruccion,
                san,
            )
            extendido to actual.varianteEnConstruccion
        }
        _estado.update {
            it.copy(
                fenVisible = nuevoFen,
                movetext = nuevoMovetext,
                jugadasSan = sansLineaPrincipal(nuevoMovetext),
                ladoEnTurnoVisible = ladoEnTurno(nuevoFen),
                resultadoVisible = motor.resultadoActual(nuevoFen),
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
                varianteEnConstruccion = nuevaVariante,
                hayError = false,
            )
        }
        guardarPartida(nuevoMovetext, actual.resultado)
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
        // En modo edición el deshacer del TopAppBar no aplica a la línea principal.
        if (actual.cargando || actual.promocionPendiente != null || actual.modoEdicion) return
        val jugadasRestantes = actual.jugadasSan.dropLast(1)
        if (jugadasRestantes.size == actual.jugadasSan.size) return
        val fen = rejugarMovetext(fenInicio, jugadasRestantes)
        val resultado = motor.resultadoActual(fen)
        // Se elimina la última jugada conservando el resto del movetext anotado
        // (variantes, comentarios y NAGs de las jugadas anteriores).
        val nuevoMovetext = eliminarUltimaJugadaDelMovetext(actual.movetext)
        _estado.update {
            it.copy(
                fen = fen,
                fenVisible = fen,
                movetext = nuevoMovetext,
                jugadasSan = jugadasRestantes,
                resultado = resultado,
                resultadoVisible = resultado,
                ladoEnTurno = ladoEnTurno(fen),
                ladoEnTurnoVisible = ladoEnTurno(fen),
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
                caminoVisible = null,
                caminoSeleccion = null,
                varianteEnConstruccion = null,
                modoEdicion = false,
                hayError = false,
            )
        }
        guardarPartida(nuevoMovetext, resultado)
    }

    /**
     * Muestra en el tablero la posición de una jugada de la planilla.
     *
     * Rejuega desde [fenInicio] las jugadas que atraviesa [camino], lo que
     * permite revisar cualquier jugada de la línea principal, de una variante o
     * de una subvariante. Mientras se revisa, el tablero bloquea nuevas jugadas.
     *
     * @param camino Camino hasta la jugada cuya posición se quiere mostrar.
     */
    fun mostrarCamino(camino: CaminoPlanilla) {
        val actual = _estado.value
        if (actual.cargando) return
        val sans = sansDeCamino(actual.movetext, camino)
        val fenVisible = rejugarMovetext(fenInicio, sans)
        val resultadoVisible = motor.resultadoActual(fenVisible)
        _estado.update {
            it.copy(
                caminoVisible = camino,
                fenVisible = fenVisible,
                resultadoVisible = resultadoVisible,
                ladoEnTurnoVisible = ladoEnTurno(fenVisible),
            )
        }
    }

    /**
     * Gestiona la pulsación de una jugada de la planilla.
     *
     * En modo edición la jugada se selecciona para editarla (el tablero va a su
     * posición previa para poder añadir variantes); en juego normal navega a
     * esa posición.
     *
     * @param camino Camino de la jugada pulsada.
     */
    fun alPulsarJugada(camino: CaminoPlanilla) {
        if (_estado.value.modoEdicion) {
            seleccionarJugada(camino)
        } else {
            mostrarCamino(camino)
        }
    }

    /** Vuelve a la posición final (última jugada) y desbloquea el tablero. */
    fun volverAlFinal() {
        val actual = _estado.value
        if (actual.cargando) return
        _estado.update {
            it.copy(
                caminoVisible = null,
                fenVisible = actual.fen,
                resultadoVisible = actual.resultado,
                ladoEnTurnoVisible = actual.ladoEnTurno,
            )
        }
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
     * @param movetext Movetext PGN completo a guardar (con anotaciones).
     * @param resultado Resultado actual a guardar.
     */
    private fun guardarPartida(movetext: String, resultado: ResultadoPartida) {
        val base = partidaBase ?: return
        viewModelScope.launch {
            repositorioPartidas.guardarPartida(
                base.copy(
                    pgn = movetext,
                    resultado = resultado,
                )
            )
        }
    }

    /**
     * Entra en el modo edición de anotaciones y variantes.
     *
     * Activa [EstadoPartida.modoEdicion], muestra la posición final en el
     * tablero y limpia cualquier selección o revisión anterior. Desde este modo
     * el tablero sirve para crear variantes sobre la jugada que se seleccione.
     * No tiene efecto si la partida está cargando o falló al abrir.
     */
    fun entrarModoEdicion() {
        val actual = _estado.value
        if (actual.cargando || actual.hayError) return
        _estado.update {
            it.copy(
                modoEdicion = true,
                caminoVisible = null,
                caminoSeleccion = null,
                varianteEnConstruccion = null,
                comentarioEdicion = "",
                nagEdicion = null,
                fenVisible = it.fen,
                resultadoVisible = it.resultado,
                ladoEnTurnoVisible = it.ladoEnTurno,
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
                hayError = false,
            )
        }
    }

    /**
     * Sale del modo edición y vuelve a la posición final de la partida.
     *
     * Descarta la selección actual y cualquier variante en construcción (las
     * jugadas de variante ya jugadas permanecen guardadas en el movetext).
     */
    fun salirModoEdicion() {
        _estado.update {
            it.copy(
                modoEdicion = false,
                caminoSeleccion = null,
                varianteEnConstruccion = null,
                comentarioEdicion = "",
                nagEdicion = null,
                fenVisible = it.fen,
                resultadoVisible = it.resultado,
                ladoEnTurnoVisible = it.ladoEnTurno,
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
            )
        }
    }

    /**
     * Selecciona una jugada de la planilla para editarla en modo edición.
     *
     * Muestra en el tablero la posición que deja esa jugada (tras rejugarse el
     * camino completo): el usuario ve la jugada seleccionada en el tablero.
     * Desde esa posición puede jugar una variante que se guardará como variante
     * pegada a la jugada anterior en la línea (se inserta justo antes de la
     * jugada seleccionada). Carga también el comentario y el NAG actuales de la
     * jugada para editarlos, y reinicia la variante en construcción.
     *
     * @param camino Camino hasta la jugada seleccionada.
     */
    fun seleccionarJugada(camino: CaminoPlanilla) {
        val actual = _estado.value
        if (!actual.modoEdicion || actual.cargando) return
        if (camino.pasos.isEmpty()) return
        val sans = sansDeCamino(actual.movetext, camino)
        val anotacion = anotacionEnCamino(actual.movetext, camino)
        val fenVisible = rejugarMovetext(fenInicio, sans)
        _estado.update {
            it.copy(
                caminoSeleccion = camino,
                varianteEnConstruccion = null,
                comentarioEdicion = anotacion.comentario.orEmpty(),
                nagEdicion = anotacion.nag,
                fenVisible = fenVisible,
                resultadoVisible = motor.resultadoActual(fenVisible),
                ladoEnTurnoVisible = ladoEnTurno(fenVisible),
                casillaSeleccionada = null,
                destinosLegales = emptyList(),
                promocionPendiente = null,
                hayError = false,
            )
        }
    }

    /**
     * Actualiza el texto del comentario en edición de la jugada seleccionada.
     *
     * @param comentario Nuevo texto del comentario (sin guardar todavía).
     */
    fun actualizarComentarioEdicion(comentario: String) {
        _estado.update { it.copy(comentarioEdicion = comentario) }
    }

    /**
     * Actualiza el NAG en edición de la jugada seleccionada.
     *
     * @param nag Código NAG elegido, o null para "sin símbolo".
     */
    fun actualizarNagEdicion(nag: Int?) {
        _estado.update { it.copy(nagEdicion = nag) }
    }

    /**
     * Guarda el comentario y NAG en edición sobre la jugada seleccionada.
     *
     * Reescribe las anotaciones de esa jugada en el movetext (un comentario o
     * NAG vacíos se interpretan como "sin anotación") y persiste el resultado.
     * No tiene efecto si no hay jugada seleccionada.
     */
    fun guardarEdicion() {
        val actual = _estado.value
        val camino = actual.caminoSeleccion ?: return
        val textoComentario = actual.comentarioEdicion.trim()
        val nuevoMovetext = actualizarAnotacionEnCamino(
            movetext = actual.movetext,
            camino = camino,
            comentario = textoComentario.takeIf { it.isNotEmpty() },
            nag = actual.nagEdicion,
        )
        _estado.update { it.copy(movetext = nuevoMovetext) }
        guardarPartida(nuevoMovetext, actual.resultado)
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

    /**
     * Genera el PDF de la plantilla FIDE de la partida actual.
     *
     * Construye una [Partida] con los tags de [partidaBase] (evento, sitio,
     * fecha, ronda, jugadores y Elos) y el movetext y resultado actuales, y la
     * pasa al [PuertoPdf]. Se usa para compartir/exportar la planilla. El
     * resultado se toma del estado visible (para que el PDF refleje la partida
     * aunque se esté revisando una posición pasada).
     *
     * @return Bytes del PDF de una página, o null si la partida no se ha cargado
     *         todavía o el PDF no se pudo generar.
     */
    fun generarPdfPartida(): ByteArray? {
        val base = partidaBase ?: return null
        val actual = _estado.value
        return try {
            generadorPdf.generarPlantilla(
                base.copy(
                    pgn = actual.movetext,
                    resultado = actual.resultadoVisible,
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Genera el PGN de la partida actual para compartir/exportar.
     *
     * Construye una [Partida] con los tags de [partidaBase] y el movetext y
     * resultado actuales, y la exporta a formato PGN via [PuertoPgn].
     *
     * @return Texto PGN de la partida, o null si la partida no se ha cargado
     *         todavía o la exportación falló.
     */
    fun exportarPgnPartida(): String? {
        val base = partidaBase ?: return null
        val actual = _estado.value
        return try {
            generadorPgn.exportar(
                base.copy(
                    pgn = actual.movetext,
                    resultado = actual.resultadoVisible,
                )
            )
        } catch (e: Exception) {
            null
        }
    }
}
