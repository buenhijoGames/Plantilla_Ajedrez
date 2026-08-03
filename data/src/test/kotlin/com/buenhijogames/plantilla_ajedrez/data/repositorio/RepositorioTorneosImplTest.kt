package com.buenhijogames.plantilla_ajedrez.data.repositorio

import app.cash.turbine.test
import com.buenhijogames.plantilla_ajedrez.data.bd.dao.TorneoDao
import com.buenhijogames.plantilla_ajedrez.data.bd.entidades.TorneoEntity
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
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
 * Tests unitarios de [RepositorioTorneosImpl].
 *
 * Se mockea el [TorneoDao] con MockK y se simula el reloj y el generador de
 * ids para que las pruebas sean deterministas. Turbine verifica el flujo
 * reactivo de [observarTorneos].
 */
class RepositorioTorneosImplTest {

    private val dao = mockk<TorneoDao>(relaxed = true)
    private val generadorIds = object : GeneradorIds {
        var contador = 0
        override fun nuevoId(): String = "id-${contador++}"
    }
    private val reloj = object : Reloj {
        var fija = 1_700_000_000_000L
        override fun ahora(): Long = fija
    }

    private val repositorio = RepositorioTorneosImpl(dao, generadorIds, reloj)

    @Test
    fun `observarTorneos emite la lista mapeada al dominio`() = runTest {
        val entidades = listOf(
            TorneoEntity(id = "t1", nombre = "Torneo A", sitio = "Madrid", fechaInicio = "2026.08.01"),
            TorneoEntity(id = "t2", nombre = "Torneo B", sitio = "Sevilla", fechaInicio = "2026.08.02"),
        )
        every { dao.observarTodos() } returns flowOf(entidades)

        repositorio.observarTorneos().test {
            val lista = awaitItem()
            assertEquals(2, lista.size)
            assertEquals(Torneo(id = "t1", nombre = "Torneo A", sitio = "Madrid", fechaInicio = "2026.08.01"), lista[0])
            assertEquals(Torneo(id = "t2", nombre = "Torneo B", sitio = "Sevilla", fechaInicio = "2026.08.02"), lista[1])
            awaitComplete()
        }
    }

    @Test
    fun `obtenerTorneo devuelve dominio si existe`() = runTest {
        val entidad = TorneoEntity(id = "t1", nombre = "X", sitio = "S", fechaInicio = "2026")
        coEvery { dao.obtenerPorId("t1") } returns entidad

        val resultado = repositorio.obtenerTorneo("t1")

        assertNotNull(resultado)
        assertEquals("X", resultado!!.nombre)
    }

    @Test
    fun `obtenerTorneo devuelve null si no existe`() = runTest {
        coEvery { dao.obtenerPorId("inexistente") } returns null

        val resultado = repositorio.obtenerTorneo("inexistente")

        assertNull(resultado)
    }

    @Test
    fun `guardarTorneo nuevo genera id y persiste entity con marca de reloj`() = runTest {
        val torneoNuevo = Torneo(id = "", nombre = "Nuevo", sitio = "S", fechaInicio = "2026")
        val capturado = slot<TorneoEntity>()
        coEvery { dao.obtenerPorId(any()) } returns null
        coEvery { dao.insertar(capture(capturado)) } returns Unit

        val idAsignado = repositorio.guardarTorneo(torneoNuevo)

        assertEquals("id-0", idAsignado)
        assertEquals("id-0", capturado.captured.id)
        assertEquals("Nuevo", capturado.captured.nombre)
        assertEquals(1_700_000_000_000L, capturado.captured.creadoEn)
    }

    @Test
    fun `guardarTorneo existente preserva creadoEn original`() = runTest {
        val existente = TorneoEntity(
            id = "t9", nombre = "Viejo", sitio = "S", fechaInicio = "2020",
            creadoEn = 1_000L,
        )
        coEvery { dao.obtenerPorId("t9") } returns existente
        val capturado = slot<TorneoEntity>()
        coEvery { dao.insertar(capture(capturado)) } returns Unit

        val torneoEditado = Torneo(id = "t9", nombre = "Viejo renombrado", sitio = "S", fechaInicio = "2020")
        val idAsignado = repositorio.guardarTorneo(torneoEditado)

        assertEquals("t9", idAsignado)
        assertEquals("Se preserva la marca original", 1_000L, capturado.captured.creadoEn)
        assertEquals("Viejo renombrado", capturado.captured.nombre)
    }

    @Test
    fun `eliminarTorneo delega al dao`() = runTest {
        coEvery { dao.eliminarPorId("t3") } returns Unit

        repositorio.eliminarTorneo("t3")

        coVerify { dao.eliminarPorId("t3") }
    }
}