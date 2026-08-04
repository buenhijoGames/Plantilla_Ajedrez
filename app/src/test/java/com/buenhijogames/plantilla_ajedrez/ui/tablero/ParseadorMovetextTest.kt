package com.buenhijogames.plantilla_ajedrez.ui.tablero

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios del parser de movetext PGN para la planilla.
 *
 * Cubren la descomposición en jugadas, variantes (anidadas), comentarios,
 * NAGs y resultado, así como la extracción de la línea principal.
 */
class ParseadorMovetextTest {

    @Test
    fun `parsea la linea principal simple con numeracion`() {
        val elementos = parsearMovetext("1. e4 e5 2. Nf3 Nc6")
        assertEquals(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Jugada("e5"),
                ElementoMovetext.Jugada("Nf3"),
                ElementoMovetext.Jugada("Nc6"),
            ),
            elementos,
        )
    }

    @Test
    fun `soporta numeracion larga con puntos suspensivos y numero pegado`() {
        val elementos = parsearMovetext("1... e5 12.Nf3")
        assertEquals(
            listOf(
                ElementoMovetext.Jugada("e5"),
                ElementoMovetext.Jugada("Nf3"),
            ),
            elementos,
        )
    }

    @Test
    fun `extrae una variante simple`() {
        val elementos = parsearMovetext("1. e4 e5 2. Nf3 (2. Bc4) Nc6")
        assertEquals(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Jugada("e5"),
                ElementoMovetext.Jugada("Nf3"),
                ElementoMovetext.Variante(
                    listOf(
                        ElementoMovetext.Jugada("Bc4"),
                    )
                ),
                ElementoMovetext.Jugada("Nc6"),
            ),
            elementos,
        )
    }

    @Test
    fun `soporta variantes anidadas`() {
        val elementos = parsearMovetext("1. e4 (1. d4 (1. c4)) 1... e5")
        assertEquals(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Variante(
                    listOf(
                        ElementoMovetext.Jugada("d4"),
                        ElementoMovetext.Variante(
                            listOf(ElementoMovetext.Jugada("c4"))
                        ),
                    )
                ),
                ElementoMovetext.Jugada("e5"),
            ),
            elementos,
        )
    }

    @Test
    fun `extrae comentarios con llaves y de resto de linea`() {
        val elementos = parsearMovetext("1. e4 {buen avance} e5 ;comentario de linea")
        assertEquals(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Comentario("buen avance"),
                ElementoMovetext.Jugada("e5"),
                ElementoMovetext.Comentario("comentario de linea"),
            ),
            elementos,
        )
    }

    @Test
    fun `extrae NAGs`() {
        val elementos = parsearMovetext("1. e4 $1 e5 $2")
        assertEquals(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Nag(1),
                ElementoMovetext.Jugada("e5"),
                ElementoMovetext.Nag(2),
            ),
            elementos,
        )
    }

    @Test
    fun `extrae el resultado final`() {
        val elementos = parsearMovetext("1. e4 e5 1-0")
        assertEquals(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Jugada("e5"),
                ElementoMovetext.Resultado("1-0"),
            ),
            elementos,
        )
    }

    @Test
    fun `descarta la cabecera de tags del pgn`() {
        val pgn = "[Event \"Torneo\"]\n[Site \"?\"]\n\n1. e4 e5"
        val elementos = parsearMovetext(pgn)
        assertEquals(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Jugada("e5"),
            ),
            elementos,
        )
    }

    @Test
    fun `devuelve lista vacia para movetext vacio`() {
        assertEquals(emptyList<ElementoMovetext>(), parsearMovetext(""))
        assertEquals(emptyList<ElementoMovetext>(), parsearMovetext("   "))
    }

    @Test
    fun `linea principal ignora variantes comentarios y nag`() {
        val sans = sansLineaPrincipal("1. e4 (1. d4) e5 {comentario} 2. Nf3 $1 Nc6 1-0")
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), sans)
    }

    @Test
    fun `simboloNag mapea los codigos mas comunes`() {
        assertEquals("!", simboloNag(1))
        assertEquals("?", simboloNag(2))
        assertEquals("!!", simboloNag(3))
        assertEquals("??", simboloNag(4))
        assertEquals("!?", simboloNag(5))
        assertEquals("?!", simboloNag(6))
        assertEquals("\$99", simboloNag(99))
    }
}
