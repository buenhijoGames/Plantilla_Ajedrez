package com.buenhijogames.plantilla_ajedrez.data.ajedrez

import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.motor.PuertoMotorAjedrez
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios de [AdaptadorChesslib].
 *
 * Verifican el contrato de [PuertoMotorAjedrez] usando posiciones conocidas:
 * posicion inicial, promocion de peon, enroque, detectar jaque mate y
 * tablas, y jugadas ilegales (movimiento de caballo en linea recta, etc.).
 *
 * El adaptador es sin estado, por lo que el orden de los tests no afecta.
 */
class AdaptadorChesslibTest {

    private lateinit var motor: PuertoMotorAjedrez

    @Before
    fun setUp() {
        motor = AdaptadorChesslib()
    }

    @Test
    fun `fenInicial devuelve la posicion estandar`() {
        val fen = motor.fenInicial()
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fen)
    }

    @Test
    fun `aplicarJugada valida e4 devuelve nuevo FEN con peon en e4`() {
        val nuevo = motor.aplicarJugada(motor.fenInicial(), "e4")
        // Tras 1.e4 el FEN debe tener el peon blanco en e4 y las negras al movimiento.
        assertTrue("El nuevo FEN debe tener negras al movimiento", nuevo.contains(" b "))
        assertTrue("El peon debe estar en e4", nuevo.startsWith("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR"))
    }

    @Test
    fun `aplicarJugada rechaza jugada ilegal con JugadaIlegalException`() {
        // "Nh5" no es legal como primer movimiento de caballo desde g1: el
        // caballo en g1 puede ir a f3 o h3 (y e2). h5 esta fuera de alcance.
        assertThrows(JugadaIlegalException::class.java) {
            motor.aplicarJugada(motor.fenInicial(), "Nh5")
        }
    }

    @Test
    fun `jugadaASan convierte el movimiento e2-e4 a SAN e4`() {
        val san = motor.jugadaASan(motor.fenInicial(), "e2", "e4")
        assertEquals("e4", san)
    }

    @Test
    fun `jugadaASan convierte el movimiento de caballo g1-f3 a SAN Nf3`() {
        val san = motor.jugadaASan(motor.fenInicial(), "g1", "f3")
        assertEquals("Nf3", san)
    }

    @Test
    fun `jugadaASan rechaza movimiento ilegal`() {
        assertThrows(JugadaIlegalException::class.java) {
            motor.jugadaASan(motor.fenInicial(), "e2", "e5") // 3 casillas adelante: ilegal
        }
    }

    @Test
    fun `jugadasLegalesDesde e2 en posicion inicial devuelve e3 y e4`() {
        val legales = motor.jugadasLegalesDesde(motor.fenInicial(), "e2")
        assertEquals(2, legales.size)
        assertTrue("e3" in legales)
        assertTrue("e4" in legales)
    }

    @Test
    fun `jugadasLegalesDesde casilla vacia devuelve lista vacia`() {
        val legales = motor.jugadasLegalesDesde(motor.fenInicial(), "e3")
        assertTrue(legales.isEmpty())
    }

    @Test
    fun `esFinal false en posicion inicial`() {
        assertFalse(motor.esFinal(motor.fenInicial()))
    }

    @Test
    fun `esTablas false en posicion inicial`() {
        assertFalse(motor.esTablas(motor.fenInicial()))
    }

    @Test
    fun `esFinal true en posicion de jaque mate del loco`() {
        // FEN del mate del loco: 1.f3 e5 2.g4?? Qh4# (negras ganan)
        val fenMate = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
        assertTrue(motor.esFinal(fenMate))
    }

    @Test
    fun `esTablas true en posicion de material insuficiente rey contra rey`() {
        // Rey blanco vs rey negro
        val fenInsuficiente = "8/8/8/4k3/8/4K3/8/8 w - - 0 1"
        assertTrue(motor.esTablas(fenInsuficiente))
    }

    @Test
    fun `jugadaASan maneja promocion de peon blanco a las cuatro piezas posibles`() {
        // Posición con peón blanco en e7 a punto de coronar en e8 (sin jaque directo)
        val fenPromocion = "8/4P3/8/8/8/8/8/4k2K w - - 0 1"
        
        val sanDama = motor.jugadaASan(fenPromocion, "e7", "e8", 'Q')
        val sanTorre = motor.jugadaASan(fenPromocion, "e7", "e8", 'R')
        val sanAlfil = motor.jugadaASan(fenPromocion, "e7", "e8", 'B')
        val sanCaballo = motor.jugadaASan(fenPromocion, "e7", "e8", 'N')

        assertTrue("Promoción a Dama empieza por e8=Q", sanDama.startsWith("e8=Q"))
        assertTrue("Promoción a Torre empieza por e8=R", sanTorre.startsWith("e8=R"))
        assertTrue("Promoción a Alfil empieza por e8=B", sanAlfil.startsWith("e8=B"))
        assertTrue("Promoción a Caballo empieza por e8=N", sanCaballo.startsWith("e8=N"))
    }

    @Test
    fun `jugadaASan maneja promocion con captura y jaque`() {
        // Peón blanco en d7 captura torre negra en e8 y da jaque al rey en f8
        val fenCapturaPromocion = "4rk2/3P4/8/8/8/8/8/7K w - - 0 1"
        val san = motor.jugadaASan(fenCapturaPromocion, "d7", "e8", 'Q')
        assertEquals("dxe8=Q+", san)
    }

    @Test
    fun `jugadaASan maneja promocion de peon negro a dama y caballo`() {
        // Peón negro en e2 a punto de coronar en e1
        val fenPromocionNegras = "7k/8/8/8/8/8/4p3/K7 b - - 0 1"
        val sanDama = motor.jugadaASan(fenPromocionNegras, "e2", "e1", 'Q')
        val sanCaballo = motor.jugadaASan(fenPromocionNegras, "e2", "e1", 'N')

        assertTrue("Promoción de negras a Dama empieza por e1=Q", sanDama.startsWith("e1=Q"))
        assertTrue("Promoción de negras a Caballo empieza por e1=N", sanCaballo.startsWith("e1=N"))
    }

    @Test
    fun `aplicarJugada ejecuta correctamente la promocion en el tablero FEN`() {
        val fenPromocion = "8/4P3/8/8/8/8/8/4k2K w - - 0 1"
        val nuevoFen = motor.aplicarJugada(fenPromocion, "e8=Q")
        
        assertTrue("El nuevo FEN debe contener la Dama blanca 'Q' en la fila 8", nuevoFen.startsWith("4Q3/8/8/8/8/8/8/4k2K"))
        assertTrue("Turno de las negras tras coronar", nuevoFen.contains(" b "))
    }

    @Test
    fun `resultadoActual devuelve EN_CURSO en posicion inicial`() {
        assertEquals(ResultadoPartida.EN_CURSO, motor.resultadoActual(motor.fenInicial()))
    }

    @Test
    fun `resultadoActual devuelve GANA_NEGRAS en el mate del loco`() {
        // Mate del loco: 1.f3 e5 2.g4?? Qh4#. Le toca a blancas y estan en mate.
        val fenMate = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
        assertEquals(ResultadoPartida.GANA_NEGRAS, motor.resultadoActual(fenMate))
    }

    @Test
    fun `resultadoActual devuelve GANA_BLANCAS con rey negro en mate`() {
        // Mate del pastor: 1.e4 e5 2.Bc4 Nc6 3.Qh5 Nf6 4.Qxf7#. Le toca a negras y estan en mate.
        val fenMate = "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4"
        assertEquals(ResultadoPartida.GANA_BLANCAS, motor.resultadoActual(fenMate))
    }

    @Test
    fun `resultadoActual devuelve TABLAS con material insuficiente`() {
        val fenInsuficiente = "8/8/8/4k3/8/4K3/8/8 w - - 0 1"
        assertEquals(ResultadoPartida.TABLAS, motor.resultadoActual(fenInsuficiente))
    }
}