package com.buenhijogames.plantilla_ajedrez.data.pgn

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.pgn.PuertoPgn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios de [AdaptadorPgn].
 *
 * Cubre:
 *   - Exportacion de una partida nueva (sin movimientos, con tags basicos).
 *   - Exportacion con FEN inicial (SetUp + FEN).
 *   - Round-trip exportar -> importar -> reexportar conserva Seven Tag Roster.
 *   - Importacion de un PGN clasico (Fischer-Spassky 1992) preserva jugadores,
 *     movimientos y resultado.
 *   - Casos limites: PGN vacio devuelve lista vacia.
 *
 * La prueba de round-trip es importante porque valida interoperabilidad con
 * otras apps de ajedrez (objetivo primordial del puerto PGN).
 */
class AdaptadorPgnTest {

    private lateinit var pgn: PuertoPgn

    @Before
    fun setUp() {
        pgn = AdaptadorPgn()
    }

    @Test
    fun `exportar partida nueva genera los siete Tag Roster`() {
        val partida = Partida(
            evento = "Torneo Test",
            sitio = "Madrid, ESP",
            fecha = "2026.08.03",
            ronda = "1",
            blancas = "Carlsen, Magnus",
            negras = "Caruana, Fabiano",
            resultado = ResultadoPartida.EN_CURSO,
            pgn = "",
        )

        val texto = pgn.exportar(partida)

        assertTrue("[Event] presente", texto.contains("[Event \"Torneo Test\"]"))
        assertTrue("[Site] presente", texto.contains("[Site \"Madrid, ESP\"]"))
        assertTrue("[Date] presente", texto.contains("[Date \"2026.08.03\"]"))
        assertTrue("[Round] presente", texto.contains("[Round \"1\"]"))
        assertTrue("[White] presente", texto.contains("[White \"Carlsen, Magnus\"]"))
        assertTrue("[Black] presente", texto.contains("[Black \"Caruana, Fabiano\"]"))
        assertTrue("[Result] presente", texto.contains("[Result \"*\"]"))
        assertTrue("Termina con resultado", texto.trim().endsWith("*"))
    }

    @Test
    fun `exportar partida con FEN inicial incluye SetUp y FEN`() {
        val partida = Partida(
            evento = "Estudio",
            sitio = "?",
            fecha = "2026.??.??",
            ronda = "?",
            blancas = "?",
            negras = "?",
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            posicionSetup = true,
            resultado = ResultadoPartida.EN_CURSO,
            pgn = "",
        )

        val texto = pgn.exportar(partida)

        assertTrue("[SetUp] presente", texto.contains("[SetUp \"1\"]"))
        assertTrue("[FEN] presente", texto.contains("[FEN \"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\"]"))
    }

    @Test
    fun `exportar rellena interrogantes para campos vacios`() {
        val partida = Partida(
            evento = "",
            sitio = "",
            fecha = "",
            ronda = "",
            blancas = "",
            negras = "",
            resultado = ResultadoPartida.EN_CURSO,
        )
        val texto = pgn.exportar(partida)
        assertTrue("[Event \"?\"]", texto.contains("[Event \"?\"]"))
        assertTrue("[Site \"?\"]", texto.contains("[Site \"?\"]"))
        assertTrue("[Date \"????.??.??" + "\"]", texto.contains("[Date \"????.??.??\"]"))
    }

    @Test
    fun `importar PGN vacio devuelve lista vacia`() = runTest {
        val resultado = pgn.importar("")
        assertTrue("Lista vacia para PGN vacio", resultado.isEmpty())
    }

    @Test
    fun `importar partida clasica preserva cabecera y resultado`() = runTest {
        // Partida Fischer-Spassky 1992 (mock simplificado) - los 7 tags roster
        val pgnTexto = """
            [Event "F/S Return Match"]
            [Site "Belgrade, Serbia JUG"]
            [Date "1992.11.04"]
            [Round "29"]
            [White "Fischer, Robert J."]
            [Black "Spassky, Boris V."]
            [Result "1/2-1/2"]

            1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O Be7 6.Re1 b5 7.Bb3 d6 *
        """.trimIndent()

        val partidas = pgn.importar(pgnTexto)

        assertEquals("Debe parsear 1 partida", 1, partidas.size)
        val p = partidas[0]
        assertEquals("F/S Return Match", p.evento)
        assertEquals("Belgrade, Serbia JUG", p.sitio)
        assertEquals("1992.11.04", p.fecha)
        assertEquals("29", p.ronda)
        assertEquals("Fischer, Robert J.", p.blancas)
        assertEquals("Spassky, Boris V.", p.negras)
        assertNotNull("PGN interno no vacio", p.pgn)
        assertTrue("PGN conserva e4 e5", p.pgn.contains("e4"))
        assertTrue("PGN conserva Nf3", p.pgn.contains("Nf3"))
    }

    @Test
    fun `round-trip exportar-reimportar conserva los 7 Tag Roster`() = runTest {
        val original = Partida(
            evento = "Memorial Manolo",
            sitio = "Madrid, ESP",
            fecha = "2026.08.03",
            ronda = "3",
            blancas = "Carlsen, Magnus",
            negras = "Caruana, Fabiano",
            resultado = ResultadoPartida.GANA_BLANCAS,
            pgn = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 1-0",
        )

        val exportado = pgn.exportar(original)
        val reimportado = pgn.importar(exportado)

        assertEquals(1, reimportado.size)
        val partida = reimportado[0]
        assertEquals(original.evento, partida.evento)
        assertEquals(original.sitio, partida.sitio)
        assertEquals(original.fecha, partida.fecha)
        assertEquals(original.blancas, partida.blancas)
        assertEquals(original.negras, partida.negras)
        assertEquals(original.resultado, partida.resultado)
    }

    @Test
    fun `exportar preserva movetext con variantes y comentarios`() {
        val partida = Partida(
            evento = "?",
            sitio = "?",
            fecha = "2026.??.??",
            ronda = "?",
            blancas = "?",
            negras = "?",
            resultado = ResultadoPartida.EN_CURSO,
            pgn = "1. e4 {Buen apertura} e5 (1...c5 {Siciliana}) 2. Nf3 *",
        )

        val texto = pgn.exportar(partida)

        assertTrue("Comentario preservado", texto.contains("Buen apertura"))
        assertTrue("Variante preservada", texto.contains("Siciliana"))
        assertTrue("Movetext termina con resultado", texto.trim().endsWith("*"))
    }

    @Test
    fun `exportar con resultado 1-0 añade el resultado al final si faltaba`() {
        val partida = Partida(
            evento = "?",
            sitio = "?",
            fecha = "2026",
            blancas = "A",
            negras = "B",
            resultado = ResultadoPartida.GANA_BLANCAS,
            pgn = "1. e4 e5 2. Nf3", // sin resultado al final
        )
        val texto = pgn.exportar(partida)
        assertTrue("Añade 1-0 al final", texto.trim().endsWith("1-0"))
    }
}