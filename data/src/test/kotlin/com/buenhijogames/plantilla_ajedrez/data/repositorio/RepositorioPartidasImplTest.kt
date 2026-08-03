package com.buenhijogames.plantilla_ajedrez.data.repositorio

import app.cash.turbine.test
import com.buenhijogames.plantilla_ajedrez.data.bd.dao.PartidaDao
import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.PartidaEntity
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.GeneradorIds
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.Reloj
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests unitarios de [RepositorioPartidasImpl].
 *
 * Cubre partidas asociadas a torneo y partidas sueltas (torneoId = null).
 */
class RepositorioPartidasImplTest {

    private val dao = mockk<PartidaDao>(relaxed = true)
    private val generadorIds = object : GeneradorIds {
        var contador = 0
        override fun nuevoId(): String = "p-${contador++}"
    }
    private val reloj = object : Reloj {
        var fija = 1_700_000_000_000L
        override fun ahora(): Long = fija
    }

    private val repositorio = RepositorioPartidasImpl(dao, generadorIds, reloj)

    @Test
    fun `observarPartidasDelTorneo emite partidas mapeadas`() = runTest {
        val entidades = listOf(
            PartidaEntity(
                id = "p1", torneoId = "t1", evento = "E", sitio = "S", fecha = "2026", ronda = "1",
                blancas = "A", negras = "B", resultado = "1-0",
            ),
        )
        every { dao.observarPartidasDelTorneo("t1") } returns flowOf(entidades)

        repositorio.observarPartidasDelTorneo("t1").test {
            val lista = awaitItem()
            assertEquals(1, lista.size)
            assertEquals("p1", lista[0].id)
            assertEquals(ResultadoPartida.GANA_BLANCAS, lista[0].resultado)
            awaitComplete()
        }
    }

    @Test
    fun `observarPartidasSueltas emite partidas con torneoId null`() = runTest {
        val entidad = PartidaEntity(
            id = "p2", torneoId = null, evento = "?", sitio = "?", fecha = "2026", ronda = "?",
            blancas = "A", negras = "B",
        )
        every { dao.observarPartidasSueltas() } returns flowOf(listOf(entidad))

        repositorio.observarPartidasSueltas().test {
            val lista = awaitItem()
            assertEquals(1, lista.size)
            assertNull(lista[0].torneoId)
            awaitComplete()
        }
    }

    @Test
    fun `obtenerPartida devuelve dominio si existe`() = runTest {
        val entidad = PartidaEntity(
            id = "p3", torneoId = null, evento = "E", sitio = "S", fecha = "2026", ronda = "?",
            blancas = "A", negras = "B",
        )
        coEvery { dao.obtenerPorId("p3") } returns entidad

        val resultado = repositorio.obtenerPartida("p3")

        assertNotNull(resultado)
        assertEquals("E", resultado!!.evento)
    }

    @Test
    fun `guardarPartida nueva asigna id y marca de reloj`() = runTest {
        val nueva = Partida(
            id = "",
            torneoId = "t1",
            evento = "E",
            sitio = "S",
            fecha = "2026",
            blancas = "A",
            negras = "B",
        )
        val capturado = slot<PartidaEntity>()
        coEvery { dao.insertar(capture(capturado)) } returns Unit

        val idAsignado = repositorio.guardarPartida(nueva)

        assertEquals("p-0", idAsignado)
        assertEquals("p-0", capturado.captured.id)
        assertEquals(1_700_000_000_000L, capturado.captured.actualizadoEn)
    }

    @Test
    fun `eliminarPartida delega al dao`() = runTest {
        coEvery { dao.eliminarPorId("p9") } returns Unit

        repositorio.eliminarPartida("p9")

        coVerify { dao.eliminarPorId("p9") }
    }
}