package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.lifecycle.SavedStateHandle
import com.buenhijogames.plantilla_ajedrez.data.ajedrez.AdaptadorChesslib
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.pdf.PuertoPdf
import com.buenhijogames.plantilla_ajedrez.domain.pgn.PuertoPgn
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioPartidas
import com.buenhijogames.plantilla_ajedrez.navegacion.Destinos
import com.buenhijogames.plantilla_ajedrez.preferencias.PreferenciasUsuario
import com.buenhijogames.plantilla_ajedrez.ui.audio.ReproductorSonidos
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de [PartidaViewModel] focalizadas en la interacción de
 * promoción de peón y elección de pieza por parte del usuario.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PromocionPeonViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val motor = AdaptadorChesslib()
    private val repositorioPartidas: RepositorioPartidas = mockk(relaxed = true)
    private val generadorPdf: PuertoPdf = mockk(relaxed = true)
    private val generadorPgn: PuertoPgn = mockk(relaxed = true)
    private val preferencias: PreferenciasUsuario = mockk(relaxed = true)
    private val reproductorSonidos: ReproductorSonidos = mockk(relaxed = true)

    private val partidaIdPrueba = "partida-test-promocion"
    private val fenPromocion = "8/4P3/8/8/8/8/8/4k2K w - - 0 1"
    private val partidaPrueba = Partida(
        id = partidaIdPrueba,
        evento = "Torneo de Promoción",
        sitio = "Madrid",
        fecha = "2026.08.21",
        ronda = "1",
        blancas = "Jugador 1",
        negras = "Jugador 2",
        resultado = ResultadoPartida.EN_CURSO,
        pgn = "",
        posicionSetup = true,
        fen = fenPromocion,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { preferencias.sonidoHabilitado } returns flowOf(false)
        every { preferencias.segundosAuto } returns flowOf(3)
        coEvery { repositorioPartidas.obtenerPartida(partidaIdPrueba) } returns partidaPrueba
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): PartidaViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(Destinos.ARG_PARTIDA_ID to partidaIdPrueba))
        return PartidaViewModel(
            savedStateHandle = savedStateHandle,
            motor = motor,
            repositorioPartidas = repositorioPartidas,
            generadorPdf = generadorPdf,
            generadorPgn = generadorPgn,
            preferencias = preferencias,
            reproductorSonidos = reproductorSonidos,
        )
    }

    @Test
    fun `al tocar peon en septima fila y su casilla de coronacion se activa promocionPendiente`() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        // En posición de coronación con peón blanco en e7: tocamos e7
        viewModel.onCasillaPulsada("e7")
        assertEquals("e7", viewModel.estado.value.casillaSeleccionada)
        assertTrue(viewModel.estado.value.destinosLegales.contains("e8"))

        // Luego tocamos e8 (destino de coronación)
        viewModel.onCasillaPulsada("e8")

        // No debe ejecutar la jugada directamente, sino mostrar el diálogo (promocionPendiente)
        val pendiente = viewModel.estado.value.promocionPendiente
        assertNotNull("Debe haber una promoción pendiente de confirmación", pendiente)
        assertEquals("e7", pendiente?.desde)
        assertEquals("e8", pendiente?.hasta)
    }

    @Test
    fun `al confirmar promocion a Dama se aplica e8=Q y se guarda la partida`() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Tocamos origen e7 y destino e8
        viewModel.onCasillaPulsada("e7")
        viewModel.onCasillaPulsada("e8")

        // El usuario elige la Dama ('Q')
        viewModel.confirmarPromocion('Q')
        advanceUntilIdle()

        // Se cierra el diálogo de promoción
        assertNull(viewModel.estado.value.promocionPendiente)
        assertNull(viewModel.estado.value.casillaSeleccionada)

        // La jugada se registró como e8=Q (o con jaque si aplica)
        val ultimaJugada = viewModel.estado.value.jugadasSan.lastOrNull()
        assertNotNull(ultimaJugada)
        assertTrue("La jugada debe ser coronación a Dama", ultimaJugada!!.startsWith("e8=Q"))

        // Se persistió en el repositorio
        coVerify { repositorioPartidas.guardarPartida(any()) }
    }

    @Test
    fun `al confirmar promocion a Caballo se aplica e8=N`() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.onCasillaPulsada("e7")
        viewModel.onCasillaPulsada("e8")

        // El usuario elige Caballo ('N')
        viewModel.confirmarPromocion('N')
        advanceUntilIdle()

        assertNull(viewModel.estado.value.promocionPendiente)
        val ultimaJugada = viewModel.estado.value.jugadasSan.lastOrNull()
        assertNotNull(ultimaJugada)
        assertTrue("La jugada debe ser coronación a Caballo", ultimaJugada!!.startsWith("e8=N"))
    }

    @Test
    fun `al cancelar promocion se limpian la seleccion y el dialogo sin aplicar jugada`() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.onCasillaPulsada("e7")
        viewModel.onCasillaPulsada("e8")
        assertNotNull(viewModel.estado.value.promocionPendiente)

        // El usuario cancela el diálogo
        viewModel.cancelarPromocion()

        // El diálogo y la selección quedan limpios y no se añade jugada
        assertNull(viewModel.estado.value.promocionPendiente)
        assertNull(viewModel.estado.value.casillaSeleccionada)
        assertTrue(viewModel.estado.value.destinosLegales.isEmpty())
        assertTrue(viewModel.estado.value.jugadasSan.isEmpty())
    }

    @Test
    fun `establecerSegundosAuto persiste el valor configurado en DataStore`() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.establecerSegundosAuto(5)
        advanceUntilIdle()

        assertEquals(5, viewModel.estado.value.segundosAuto)
        coVerify { preferencias.guardarSegundosAuto(5) }
    }
}
