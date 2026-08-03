package com.buenhijogames.plantilla_ajedrez.data.repositorio

import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.GeneradorIds
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.Reloj
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios de [GeneradorIdsUuid] y [RelojSistema].
 *
 * Aseguran que el generador produce ids únicos en formato UUID y que el reloj
 * avanza (no es determinista, pero debe ser creciente), garantizando que los
 * repositorios tienen dependencias de producción operativas.
 */
class InfraestructuraRepositoriosTest {

    @Test
    fun `GeneradorIdsUuid genera ids distintos y con formato UUID`() {
        val generador: GeneradorIds = GeneradorIdsUuid()
        val a = generador.nuevoId()
        val b = generador.nuevoId()
        assertNotEquals("IDs consecutivos deben ser distintos", a, b)
        assertTrue("ID debe tener formato UUID", a.matches(Regex("^[0-9a-f-]{36}$")))
    }

    @Test
    fun `RelojSistema devuelve marca temporal positiva y creciente`() {
        val reloj: Reloj = RelojSistema()
        val t1 = reloj.ahora()
        val t2 = reloj.ahora()
        assertTrue(t1 > 0)
        assertTrue(t2 >= t1)
    }

    @Test
    fun `ResultadoPartida enum cubre 4 estados principales`() {
        val estados = ResultadoPartida.values().map { it.pgn }
        assertTrue("1-0" in estados)
        assertTrue("0-1" in estados)
        assertTrue("1/2-1/2" in estados)
        assertTrue("*" in estados)
    }

    @Test
    fun `Torneo con id por defecto vacio es valido para alta`() {
        val nuevo = Torneo(id = "", nombre = "Sin id")
        assertEquals("", nuevo.id)
    }
}