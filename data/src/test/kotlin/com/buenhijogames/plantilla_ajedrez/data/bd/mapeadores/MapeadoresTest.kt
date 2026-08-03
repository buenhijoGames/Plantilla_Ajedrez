package com.buenhijogames.plantilla_ajedrez.data.bd.mapeadores

import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.PartidaEntity
import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.TorneoEntity
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios de los mappers entity <-> dominio.
 *
 * Verifican idempotencia (dominio -> entity -> dominio == original) y que
 * todos los campos del Seven Tag Roster se preserven, incluidos los casos
 * límite (partida suelta sin torneoId, FEN de setup, resultado en curso).
 */
class MapeadoresTest {

    private val marcaTiempo = 1_700_000_000_000L

    @Test
    fun `Torneo redondo preserva todos los campos`() {
        val original = Torneo(
            id = "torneo-1",
            nombre = "Memorial Manolo",
            sitio = "Madrid, ESP",
            fechaInicio = "2026.08.03",
            fechaFin = "2026.08.10",
            arbitro = "IA Lopez",
            notas = "Ritmo 15+10",
        )

        val entity = original.aEntity(creadoEn = marcaTiempo)
        val reconstruido = entity.aDominio()

        assertEquals(original, reconstruido)
        assertEquals(marcaTiempo, entity.creadoEn)
    }

    @Test
    fun `Torneo con valores por defecto preserva id y nombre`() {
        val original = Torneo(id = "t2", nombre = "Match amistoso")
        val reconstruido = original.aEntity(marcaTiempo).aDominio()
        assertEquals(original, reconstruido)
    }

    @Test
    fun `Partida redonda preserva Seven Tag Roster y campos opcionales`() {
        val original = Partida(
            id = "p-1",
            torneoId = "torneo-1",
            evento = "Memorial Manolo",
            sitio = "Madrid, ESP",
            fecha = "2026.08.03",
            ronda = "3",
            blancas = "Carlsen, Magnus",
            negras = "Caruana, Fabiano",
            eloBlancas = 2839,
            eloNegras = 2820,
            resultado = ResultadoPartida.GANA_BLANCAS,
            fechaHora = "15:30:00",
            modo = "OTB",
            fen = null,
            posicionSetup = false,
            pgn = "1. e4 e5 2. Nf3 Nc6 3. Bb5 1-0",
        )

        val entity = original.aEntity(actualizadoEn = marcaTiempo)
        val reconstruido = entity.aDominio()

        assertEquals(original, reconstruido)
        assertEquals(marcaTiempo, entity.actualizadoEn)
        assertEquals("1-0", entity.resultado)
    }

    @Test
    fun `Partida suelta tiene torneoId null en entity`() {
        val original = Partida(
            id = "suelta-1",
            torneoId = null,
            evento = "Partida amistosa",
            sitio = "Casa",
            fecha = "2026.08.03",
            blancas = "Yo",
            negras = "Tu",
        )
        val entity = original.aEntity(marcaTiempo)
        assertNull(entity.torneoId)
        assertEquals(original, entity.aDominio())
    }

    @Test
    fun `Partida con FEN de setup preserva posicionSetup true`() {
        val original = Partida(
            id = "study-1",
            evento = "Estudio",
            sitio = "?",
            fecha = "2026.08.03",
            blancas = "?",
            negras = "?",
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            posicionSetup = true,
        )
        val entity = original.aEntity(marcaTiempo)
        assertTrue(entity.posicionSetup)
        assertEquals(original.fen, entity.fen)
        assertEquals(original, entity.aDominio())
    }

    @Test
    fun `Partida en curso mapea resultado a asterisco en entity`() {
        val original = Partida(
            id = "live-1",
            evento = "?",
            sitio = "?",
            fecha = "2026.??.??",
            blancas = "?",
            negras = "?",
            resultado = ResultadoPartida.EN_CURSO,
        )
        val entity = original.aEntity(marcaTiempo)
        assertEquals("*", entity.resultado)
    }

    @Test
    fun `Partida con tablas mapea a mitad-medio-mitad en entity`() {
        val original = Partida(
            id = "draw-1",
            evento = "?",
            sitio = "?",
            fecha = "2026.??.??",
            blancas = "?",
            negras = "?",
            resultado = ResultadoPartida.TABLAS,
        )
        val entity = original.aEntity(marcaTiempo)
        assertEquals("1/2-1/2", entity.resultado)
    }

    @Test
    fun `ResultadoPartida desdePgn reconoce los cuatro casos estandar`() {
        assertEquals(ResultadoPartida.GANA_BLANCAS, ResultadoPartida.desdePgn("1-0"))
        assertEquals(ResultadoPartida.GANA_NEGRAS, ResultadoPartida.desdePgn("0-1"))
        assertEquals(ResultadoPartida.TABLAS, ResultadoPartida.desdePgn("1/2-1/2"))
        assertEquals(ResultadoPartida.TABLAS, ResultadoPartida.desdePgn("½-½"))
        assertEquals(ResultadoPartida.EN_CURSO, ResultadoPartida.desdePgn("*"))
        assertEquals(ResultadoPartida.EN_CURSO, ResultadoPartida.desdePgn(null))
        assertEquals(ResultadoPartida.EN_CURSO, ResultadoPartida.desdePgn(""))
        assertEquals(ResultadoPartida.EN_CURSO, ResultadoPartida.desdePgn("garbage"))
    }
}