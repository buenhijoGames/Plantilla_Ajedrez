package com.buenhijogames.plantilla_ajedrez.ui.tablero

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios de las utilidades puras del tablero.
 *
 * Cubren la conversión FEN <-> mapa de piezas, las coordenadas de casilla,
 * la serialización del movetext PGN y la detección del bando en turno.
 */
class UtilidadesTableroTest {

    @Test
    fun `piezasDesdeFen extrae la posicion inicial completa`() {
        val piezas = piezasDesdeFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(32, piezas.size)
        assertEquals('r', piezas["a8"])
        assertEquals('R', piezas["a1"])
        assertEquals('p', piezas["e7"])
        assertEquals('P', piezas["e2"])
        assertEquals('K', piezas["e1"])
    }

    @Test
    fun `piezasDesdeFen con filas intermedias vacias respeta el salto`() {
        // Posicion tras 1.e4: peon blanco en e4, el resto de la fila 4 vacia.
        val piezas = piezasDesdeFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        assertEquals('P', piezas["e4"])
        assertFalse(piezas.containsKey("d4"))
        assertEquals('p', piezas["e5"])
    }

    @Test
    fun `piezasDesdeFen devuelve vacio si el FEN no tiene 8 filas`() {
        assertTrue(piezasDesdeFen("8/8/8/8").isEmpty())
    }

    @Test
    fun `filaYColumna mapea la casilla e2 a fila 6 columna 4`() {
        val (fila, columna) = filaYColumnaDeCasilla("e2")
        assertEquals(6, fila)
        assertEquals(4, columna)
    }

    @Test
    fun `filaYColumna mapea la casilla a8 a fila 0 columna 0`() {
        val (fila, columna) = filaYColumnaDeCasilla("a8")
        assertEquals(0, fila)
        assertEquals(0, columna)
    }

    @Test
    fun `casillaDeFilaColumna es inversa de filaYColumnaDeCasilla`() {
        for (fila in 0..7) {
            for (columna in 0..7) {
                val casilla = casillaDeFilaColumna(fila, columna)
                val (fila2, columna2) = filaYColumnaDeCasilla(casilla)
                assertEquals(fila to columna, fila2 to columna2)
            }
        }
    }

    @Test
    fun `movetextDesdeSans genera numeracion de jugadas correcta`() {
        val movetext = movetextDesdeSans(listOf("e4", "e5", "Nf3", "Nc6"))
        assertEquals("1. e4 e5 2. Nf3 Nc6", movetext)
    }

    @Test
    fun `sansDesdeMovetext extrae los SAN ignorando numeros y resultado`() {
        val sans = sansDesdeMovetext("1. e4 e5 2. Nf3 Nc6 1-0")
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), sans)
    }

    @Test
    fun `sansDesdeMovetext soporta numeracion larga con puntos suspensivos`() {
        val sans = sansDesdeMovetext("1... e5 12. Nf3")
        assertEquals(listOf("e5", "Nf3"), sans)
    }

    @Test
    fun `sansDesdeMovetext devuelve lista vacia para movetext vacio`() {
        assertTrue(sansDesdeMovetext("").isEmpty())
        assertTrue(sansDesdeMovetext("   ").isEmpty())
    }

    @Test
    fun `movetext redondo mantiene los SAN`() {
        val originales = listOf("e4", "c5", "Nf3", "Nc6")
        val extraidos = sansDesdeMovetext(movetextDesdeSans(originales))
        assertEquals(originales, extraidos)
    }

    @Test
    fun `ladoEnTurno detecta blancas y negras`() {
        assertEquals('w', ladoEnTurno("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"))
        assertEquals('b', ladoEnTurno("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"))
        assertEquals('w', ladoEnTurno("sin-fen-valido"))
    }

    @Test
    fun `segmentosDeSan convierte la letra de pieza en pieza blanca`() {
        assertEquals(
            listOf(SegmentoSan.Pieza('N'), SegmentoSan.Texto("xd4")),
            segmentosDeSan("Nxd4", esBlanca = true),
        )
    }

    @Test
    fun `segmentosDeSan usa pieza negra cuando mueven las negras`() {
        assertEquals(
            listOf(SegmentoSan.Pieza('q'), SegmentoSan.Texto("h4")),
            segmentosDeSan("Qh4", esBlanca = false),
        )
    }

    @Test
    fun `segmentosDeSan mantiene la desambiguacion y el jaque como texto`() {
        assertEquals(
            listOf(SegmentoSan.Pieza('R'), SegmentoSan.Texto("ae1+")),
            segmentosDeSan("Rae1+", esBlanca = true),
        )
    }

    @Test
    fun `segmentosDeSan convierte la promocion en pieza`() {
        assertEquals(
            listOf(SegmentoSan.Texto("e8="), SegmentoSan.Pieza('Q'), SegmentoSan.Texto("")),
            segmentosDeSan("e8=Q", esBlanca = true),
        )
        // El SAN usa mayúscula; el color (negras) se aplica vía esBlanca.
        assertEquals(
            listOf(SegmentoSan.Texto("e1="), SegmentoSan.Pieza('q'), SegmentoSan.Texto("")),
            segmentosDeSan("e1=Q", esBlanca = false),
        )
    }

    @Test
    fun `segmentosDeSan deja el peon como texto`() {
        assertEquals(listOf(SegmentoSan.Texto("e4")), segmentosDeSan("e4", esBlanca = true))
        assertEquals(listOf(SegmentoSan.Texto("exd5")), segmentosDeSan("exd5", esBlanca = false))
    }

    @Test
    fun `segmentosDeSan deja el enroque como texto`() {
        assertEquals(listOf(SegmentoSan.Texto("O-O")), segmentosDeSan("O-O", esBlanca = true))
        assertEquals(listOf(SegmentoSan.Texto("O-O-O")), segmentosDeSan("O-O-O", esBlanca = false))
    }
}
