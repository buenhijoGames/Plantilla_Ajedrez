package com.buenhijogames.plantilla_ajedrez.data.pdf

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del modelo puro [PlanillaFide] (segmentacion SAN -> figurin y
 * construccion de la plantilla). No requiere Android.
 */
class PlanillaFideTest {

    @Test
    fun `segmentar pieza de caballo al inicio de jugada`() {
        val segmentos = PlanillaFide.segmentar("Nf3", esBlanca = true)
        assertEquals(
            listOf(SegmentoFigurin.Pieza('N'), SegmentoFigurin.Texto("f3")),
            segmentos,
        )
    }

    @Test
    fun `segmentar jugada de peon como texto puro`() {
        val segmentos = PlanillaFide.segmentar("e4", esBlanca = true)
        assertEquals(listOf(SegmentoFigurin.Texto("e4")), segmentos)
    }

    @Test
    fun `segmentar promocion separa la pieza tras el igual`() {
        val segmentos = PlanillaFide.segmentar("e8=Q", esBlanca = true)
        assertEquals(
            listOf(
                SegmentoFigurin.Texto("e8="),
                SegmentoFigurin.Pieza('Q'),
                SegmentoFigurin.Texto(""),
            ),
            segmentos,
        )
    }

    @Test
    fun `segmentar pieza negra usa minuscula`() {
        val segmentos = PlanillaFide.segmentar("Nxd4", esBlanca = false)
        assertEquals(
            listOf(SegmentoFigurin.Pieza('n'), SegmentoFigurin.Texto("xd4")),
            segmentos,
        )
    }

    @Test
    fun `construir genera cabecera y filas con jugadas de linea principal`() {
        val partida = Partida(
            evento = "Torneo de Prueba",
            sitio = "Madrid",
            fecha = "2026.08.05",
            ronda = "1",
            blancas = "García, Ana",
            negras = "López, Luis",
            eloBlancas = 2500,
            eloNegras = null,
            resultado = ResultadoPartida.GANA_BLANCAS,
            pgn = "1. e4 e5 2. Nf3 (2. Bc4) Nc6 1-0",
        )

        val plantilla = PlanillaFide.construir(partida)

        assertEquals("Torneo de Prueba", plantilla.cabecera.evento)
        assertEquals("Madrid", plantilla.cabecera.sitio)
        assertEquals("2026.08.05", plantilla.cabecera.fecha)
        assertEquals("1", plantilla.cabecera.ronda)
        assertEquals("García, Ana", plantilla.cabecera.blancas)
        assertEquals("López, Luis", plantilla.cabecera.negras)
        assertEquals(2500, plantilla.cabecera.eloBlancas)
        assertEquals(ResultadoPartida.GANA_BLANCAS, plantilla.cabecera.resultado)

        // La variante (2. Bc4) se ignora: hay 3 jugadas en la linea principal.
        assertEquals(2, plantilla.filas.size)
        assertEquals(1, plantilla.filas[0].numero)
        assertEquals(2, plantilla.filas[1].numero)
        // Fila 1: e4 (blancas) y e5 (negras).
        assertEquals(listOf(SegmentoFigurin.Texto("e4")), plantilla.filas[0].blancas)
        assertEquals(listOf(SegmentoFigurin.Texto("e5")), plantilla.filas[0].negras)
        // Fila 2: Nf3 (blancas) y Nc6 (negras); la negra usa minuscula.
        assertEquals(
            listOf(SegmentoFigurin.Pieza('N'), SegmentoFigurin.Texto("f3")),
            plantilla.filas[1].blancas,
        )
        assertEquals(
            listOf(SegmentoFigurin.Pieza('n'), SegmentoFigurin.Texto("c6")),
            plantilla.filas[1].negras,
        )
    }

    @Test
    fun `construir con jugadas pares rellena ambas columnas`() {
        // 5 jugadas: Nf3 d5 c4 e6 d4 -> 3 filas (la ultima negra vacia).
        val partida = Partida(
            evento = "", sitio = "", fecha = "", ronda = "",
            blancas = "", negras = "",
            pgn = "1. Nf3 d5 2. c4 e6 3. d4",
        )
        val plantilla = PlanillaFide.construir(partida)
        assertEquals(3, plantilla.filas.size)
        // Fila 1: Nf3 (blancas) y d5 (negras).
        assertEquals(1, plantilla.filas[0].numero)
        assertEquals(
            listOf(SegmentoFigurin.Pieza('N'), SegmentoFigurin.Texto("f3")),
            plantilla.filas[0].blancas,
        )
        assertEquals(listOf(SegmentoFigurin.Texto("d5")), plantilla.filas[0].negras)
        // Fila 2: c4 (blancas) y e6 (negras).
        assertEquals(2, plantilla.filas[1].numero)
        assertEquals(listOf(SegmentoFigurin.Texto("c4")), plantilla.filas[1].blancas)
        assertEquals(listOf(SegmentoFigurin.Texto("e6")), plantilla.filas[1].negras)
        // Fila 3: d4 (blancas), sin jugada negra.
        assertEquals(3, plantilla.filas[2].numero)
        assertEquals(listOf(SegmentoFigurin.Texto("d4")), plantilla.filas[2].blancas)
        assertNull(plantilla.filas[2].negras)
    }

    @Test
    fun `construir con movetext vacio produce cabecera sin filas`() {
        val partida = Partida(
            evento = "E", sitio = "S", fecha = "2026.01.01", ronda = "1",
            blancas = "A", negras = "B",
            pgn = "",
        )
        val plantilla = PlanillaFide.construir(partida)
        assertTrue(plantilla.filas.isEmpty())
        assertEquals("E", plantilla.cabecera.evento)
    }

    @Test
    fun `construir limita a 60 jugadas por hoja`() {
        val sans = (1..70).joinToString(" ") { if (it % 2 == 1) "${(it + 1) / 2}. e4" else "e5" }
        val partida = Partida(
            evento = "", sitio = "", fecha = "", ronda = "",
            blancas = "", negras = "",
            pgn = sans,
        )
        val plantilla = PlanillaFide.construir(partida)
        // 60 jugadas = 30 filas.
        assertEquals(30, plantilla.filas.size)
        assertEquals(30, plantilla.filas.last().numero)
    }
}