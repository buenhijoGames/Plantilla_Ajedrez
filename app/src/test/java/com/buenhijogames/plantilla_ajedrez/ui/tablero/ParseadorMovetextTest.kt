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

    @Test
    fun `serializarMovetext regenera numeros de la linea principal`() {
        val movetext = serializarMovetext(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Jugada("e5"),
                ElementoMovetext.Jugada("Nf3"),
                ElementoMovetext.Jugada("Nc6"),
            )
        )
        assertEquals("1. e4 e5 2. Nf3 Nc6", movetext)
    }

    @Test
    fun `serializarMovetext conserva variantes comentarios nag y resultado`() {
        val movetext = serializarMovetext(
            listOf(
                ElementoMovetext.Jugada("e4"),
                ElementoMovetext.Comentario("buen desarrollo"),
                ElementoMovetext.Nag(1),
                ElementoMovetext.Jugada("e5"),
                ElementoMovetext.Variante(listOf(ElementoMovetext.Jugada("Bc4"))),
                ElementoMovetext.Jugada("Nc6"),
                ElementoMovetext.Resultado("1-0"),
            )
        )
        assertEquals("1. e4 {buen desarrollo} \$1 e5 ( Bc4 ) 2. Nc6 1-0", movetext)
    }

    @Test
    fun `serializarMovetext devuelve vacio para lista vacia`() {
        assertEquals("", serializarMovetext(emptyList()))
    }

    @Test
    fun `agregarJugadaAlMovetext anade conservando anotaciones`() {
        val resultado = agregarJugadaAlMovetext("1. e4 {buen avance} \$1 e5", "Nf3")
        assertEquals("1. e4 {buen avance} \$1 e5 2. Nf3", resultado)
    }

    @Test
    fun `agregarJugadaAlMovetext genera numeracion desde movetext vacio`() {
        assertEquals("1. e4", agregarJugadaAlMovetext("", "e4"))
    }

    @Test
    fun `agregarJugadaAlMovetext conserva un resultado final existente`() {
        assertEquals(
            "1. e4 e5 2. Nf3 1-0",
            agregarJugadaAlMovetext("1. e4 e5 1-0", "Nf3"),
        )
    }

    @Test
    fun `eliminarUltimaJugadaDelMovetext quita la ultima jugada con sus anotaciones`() {
        assertEquals(
            "1. e4 {comentario}",
            eliminarUltimaJugadaDelMovetext("1. e4 {comentario} e5 \$3"),
        )
    }

    @Test
    fun `eliminarUltimaJugadaDelMovetext devuelve vacio si solo queda una jugada`() {
        assertEquals("", eliminarUltimaJugadaDelMovetext("1. e4"))
    }

    @Test
    fun `eliminarUltimaJugadaDelMovetext conserva variantes anteriores`() {
        assertEquals(
            "1. e4 ( d4 )",
            eliminarUltimaJugadaDelMovetext("1. e4 (1. d4) e5"),
        )
    }

    @Test
    fun `eliminarUltimaJugadaDelMovetext conserva el resultado final`() {
        assertEquals(
            "1. e4 1-0",
            eliminarUltimaJugadaDelMovetext("1. e4 e5 1-0"),
        )
    }

    @Test
    fun `anotacionDeJugada devuelve comentario y nag de la jugada`() {
        assertEquals(
            AnotacionJugada(comentario = "desarrollo", nag = 1),
            anotacionDeJugada("1. e4 {desarrollo} \$1 e5", 1),
        )
    }

    @Test
    fun `anotacionDeJugada devuelve nulos si la jugada no tiene anotaciones`() {
        assertEquals(
            AnotacionJugada(comentario = null, nag = null),
            anotacionDeJugada("1. e4 e5", 2),
        )
    }

    @Test
    fun `actualizarAnotacionDeJugada reemplaza las anotaciones existentes`() {
        assertEquals(
            "1. e4 {nuevo} \$1 e5",
            actualizarAnotacionDeJugada("1. e4 {viejo} \$2 e5", 1, "nuevo", 1),
        )
    }

    @Test
    fun `actualizarAnotacionDeJugada borra anotaciones si se pasan nulos`() {
        assertEquals(
            "1. e4 e5",
            actualizarAnotacionDeJugada("1. e4 {viejo} \$2 e5", 1, null, null),
        )
    }

    @Test
    fun `actualizarAnotacionDeJugada no modifica si el ply no existe`() {
        assertEquals(
            "1. e4 e5",
            actualizarAnotacionDeJugada("1. e4 e5", 5, "nuevo", null),
        )
    }

    // --- Camino por el árbol (variantes, subvariantes y edición) ---

    private val caminoE4 = CaminoPlanilla.INICIO + PasoCamino.Lineal(1)
    private val caminoE5 = CaminoPlanilla.INICIO + PasoCamino.Lineal(2)
    private val caminoD4 = caminoE4 + PasoCamino.EntrarVariante(0) + PasoCamino.Lineal(1)

    @Test
    fun `sansDeCamino recorre la linea principal`() {
        assertEquals(listOf("e4"), sansDeCamino("1. e4 e5", caminoE4))
        assertEquals(listOf("e4", "e5"), sansDeCamino("1. e4 e5", caminoE5))
    }

    @Test
    fun `sansDeCamino conserva la jugada padre al entrar en una variante`() {
        // La aplicación genera las variantes jugando sobre la posición que
        // deja la jugada seleccionada: la primera jugada de la variante es la
        // respuesta del rival, por lo que el SAN de la jugada "padre" se
        // conserva y la variante se reproduce después de ella.
        val caminoD5 = caminoE4 + PasoCamino.EntrarVariante(0) + PasoCamino.Lineal(1)
        assertEquals(listOf("e4", "d5"), sansDeCamino("1. e4 ( d5 ) e5", caminoD5))
        assertEquals(
            listOf("e4", "d5", "Nf3"),
            sansDeCamino("1. e4 ( d5 Nf3 ) e5", caminoD5 + PasoCamino.Lineal(1)),
        )
    }

    @Test
    fun `sansDeCamino atraviesa subvariantes anidadas`() {
        // Las subvariantes también se reproducen tras la jugada a la que se
        // pegan (el camino completo), no desde la posición inicial.
        val caminoNf3 = caminoE4 + PasoCamino.EntrarVariante(0) + PasoCamino.Lineal(2)
        val caminoBg7 = caminoNf3 + PasoCamino.EntrarVariante(0) + PasoCamino.Lineal(1)
        assertEquals(
            listOf("e4", "d5", "Nf3", "Bg7"),
            sansDeCamino("1. e4 ( d5 Nf3 ( Bg7 ) ) e5", caminoBg7),
        )
    }

    @Test
    fun `anotacionEnCamino lee anotaciones de la linea principal y de variantes`() {
        assertEquals(
            AnotacionJugada("desarrollo", 1),
            anotacionEnCamino("1. e4 {desarrollo} \$1 e5", caminoE4),
        )
        assertEquals(
            AnotacionJugada("alternativa", 2),
            anotacionEnCamino("1. e4 (1. d4 {alternativa} \$2) e5", caminoD4),
        )
    }

    @Test
    fun `actualizarAnotacionEnCamino edita jugadas de linea principal`() {
        assertEquals(
            "1. e4 {nuevo} \$1 e5",
            actualizarAnotacionEnCamino("1. e4 {viejo} \$2 e5", caminoE4, "nuevo", 1),
        )
    }

    @Test
    fun `actualizarAnotacionEnCamino edita jugadas dentro de una variante`() {
        assertEquals(
            "1. e4 ( d4 ) e5",
            actualizarAnotacionEnCamino("1. e4 (1. d4 {x} \$2) e5", caminoD4, null, null),
        )
    }

    @Test
    fun `insertarVarianteEnCamino inserta tras la jugada de la linea principal`() {
        assertEquals(
            "1. e4 ( d4 ) e5",
            insertarVarianteEnCamino("1. e4 e5", caminoE4, listOf("d4")),
        )
    }

    @Test
    fun `insertarVarianteEnCamino acumula varias variantes`() {
        val conPrimera = insertarVarianteEnCamino("1. e4 e5", caminoE4, listOf("d4"))
        assertEquals(
            "1. e4 ( d4 ) ( c4 ) e5",
            insertarVarianteEnCamino(conPrimera, caminoE4, listOf("c4")),
        )
    }

    @Test
    fun `insertarVarianteEnCamino inserta subvariantes dentro de una variante`() {
        val caminoD4 = caminoE4 + PasoCamino.EntrarVariante(0) + PasoCamino.Lineal(1)
        assertEquals(
            "1. e4 ( d4 ( c4 ) e5 ) Nf3",
            insertarVarianteEnCamino("1. e4 (1. d4 e5) Nf3", caminoD4, listOf("c4")),
        )
    }

    @Test
    fun `agregarJugadaAVarianteEnCamino extiende la variante en construccion`() {
        val caminoVariante = caminoE4 + PasoCamino.EntrarVariante(0)
        assertEquals(
            "1. e4 ( d4 Nf6 ) e5",
            agregarJugadaAVarianteEnCamino("1. e4 ( d4 ) e5", caminoVariante, "Nf6"),
        )
    }

    @Test
    fun `numeroDeVariantesPegadas cuenta variantes acumuladas de una jugada`() {
        assertEquals(2, numeroDeVariantesPegadas("1. e4 ( d4 ) ( c4 ) e5", caminoE4))
        assertEquals(0, numeroDeVariantesPegadas("1. e4 e5", caminoE4))
    }

    // ── Tests de round-trip NAG: guardar → serializar → parsear → verificar ──

    @Test
    fun `round trip NAG en linea principal`() {
        // 1. Guardar NAG en e4
        val conNag = actualizarAnotacionEnCamino("1. e4 e5", caminoE4, null, 1)
        assertEquals("1. e4 $1 e5", conNag)
        // 2. Re-parsear y verificar posición del NAG
        val elementos = parsearMovetext(conNag)
        val nagIndex = elementos.indexOfFirst { it is ElementoMovetext.Nag }
        val jugadaIndex = elementos.indexOfFirst { it is ElementoMovetext.Jugada }
        assertEquals("NAG debe estar justo después de la jugada", jugadaIndex + 1, nagIndex)
    }

    @Test
    fun `round trip NAG con comentario existente`() {
        // Guardar NAG cuando ya hay comentario
        val resultado = actualizarAnotacionEnCamino(
            "1. e4 {buen avance} e5", caminoE4, "nuevo", 1
        )
        assertEquals("1. e4 {nuevo} $1 e5", resultado)
        // Verificar que el NAG está después del comentario y antes de la siguiente jugada
        val elementos = parsearMovetext(resultado)
        val nagIndex = elementos.indexOfFirst { it is ElementoMovetext.Nag }
        val comentarioIndex = elementos.indexOfFirst { it is ElementoMovetext.Comentario }
        val jugadasCount = elementos.count { it is ElementoMovetext.Jugada }
        assertEquals("Comentario antes del NAG", true, comentarioIndex < nagIndex)
        assertEquals("NAG antes de la siguiente jugada", true, nagIndex < elementos.size - 1)
        assertEquals("Solo 2 jugadas en línea principal", 2, jugadasCount)
    }

    @Test
    fun `round trip NAG dentro de variante`() {
        val caminoD4 = caminoE4 + PasoCamino.EntrarVariante(0) + PasoCamino.Lineal(1)
        val resultado = actualizarAnotacionEnCamino(
            "1. e4 (1. d4 e5) Nf3", caminoD4, null, 1
        )
        // El NAG debe estar dentro de la variante, después de d4
        val elementos = parsearMovetext(resultado)
        val variante = elementos.filterIsInstance<ElementoMovetext.Variante>().first()
        val nagEnVariante = variante.elementos.indexOfFirst { it is ElementoMovetext.Nag }
        val jugadaEnVariante = variante.elementos.indexOfFirst { it is ElementoMovetext.Jugada }
        assertEquals("NAG justo después de d4 dentro de la variante", jugadaEnVariante + 1, nagEnVariante)
    }

    @Test
    fun `round trip guardar y re-leer NAG`() {
        // Guardar NAG
        val conNag = actualizarAnotacionEnCamino("1. e4 e5", caminoE4, null, 1)
        // Re-leer la anotación de e4
        val anotacion = anotacionEnCamino(conNag, caminoE4)
        assertEquals("NAG re-leído correctamente", 1, anotacion.nag)
        assertEquals("Sin comentario", null, anotacion.comentario)
    }

    @Test
    fun `round trip guardar comentario y NAG y re-leer`() {
        val resultado = actualizarAnotacionEnCamino(
            "1. e4 e5", caminoE4, "excelente", 3
        )
        val anotacion = anotacionEnCamino(resultado, caminoE4)
        assertEquals("Comentario re-leído", "excelente", anotacion.comentario)
        assertEquals("NAG re-leído", 3, anotacion.nag)
    }

    // ── Test combinado: flujo del ViewModel al crear una variante ──

    @Test
    fun `flujo completo crea variante y conserva comentario al guardar`() {
        // 1. Movetext inicial
        var movetext = "1. e4 e5"

        // 2. Seleccionar e4 (camino Lineal(1)) y jugar 1a jugada de variante (d5)
        val caminoE4 = CaminoPlanilla.INICIO + PasoCamino.Lineal(1)
        val indiceNueva = numeroDeVariantesPegadas(movetext, caminoE4)
        movetext = insertarVarianteEnCamino(movetext, caminoE4, listOf("d5"))
        val caminoVariante = caminoE4 + PasoCamino.EntrarVariante(indiceNueva)
        assertEquals("Variante creada con la 1a jugada", "1. e4 ( d5 ) e5", movetext)

        // 3. Segunda jugada de la variante: se extiende la misma variante
        movetext = agregarJugadaAVarianteEnCamino(movetext, caminoVariante, "Nf3")
        assertEquals("Variante extendida con la 2a jugada", "1. e4 ( d5 Nf3 ) e5", movetext)

        // 4. Guardar comentario sobre e4: la variante se conserva
        movetext = actualizarAnotacionEnCamino(movetext, caminoE4, "excelente", null)
        assertEquals("Comentario + variante coexistente", "1. e4 {excelente} ( d5 Nf3 ) e5", movetext)

        // 5. Verificar que el árbol parseado contiene la variante con sus 2 jugadas
        val elementos = parsearMovetext(movetext)
        val variantes = elementos.filterIsInstance<ElementoMovetext.Variante>()
        assertEquals("Hay una variante en el árbol", 1, variantes.size)
        assertEquals(
            "La variante conserva sus 2 jugadas",
            listOf("d5", "Nf3"),
            variantes.first().elementos.mapNotNull { (it as? ElementoMovetext.Jugada)?.san },
        )
    }

    @Test
    fun `flujo completo acumula varias variantes en la misma jugada`() {
        // Dos variantes pegadas a 1.e4: se acumulan
        val caminoE4 = CaminoPlanilla.INICIO + PasoCamino.Lineal(1)
        var movetext = "1. e4 e5"
        val indice1 = numeroDeVariantesPegadas(movetext, caminoE4)
        movetext = insertarVarianteEnCamino(movetext, caminoE4, listOf("d5"))
        val indice2 = numeroDeVariantesPegadas(movetext, caminoE4)
        assertEquals("Índice de la 2a variante", 1, indice2)
        assertEquals("Después de la 2a variante", indice1 + 1, indice2)
        movetext = insertarVarianteEnCamino(movetext, caminoE4, listOf("c5"))
        assertEquals("Dos variantes acumuladas", "1. e4 ( d5 ) ( c5 ) e5", movetext)
    }
}
