package com.buenhijogames.plantilla_ajedrez.ui.tablero

/**
 * Parser del movetext PGN para la planilla.
 *
 * Convierte el movetext de una partida (con números de jugada, variantes,
 * comentarios, NAGs y resultado) en una estructura navegable de
 * [ElementoMovetext]. Es una utilidad pura, sin dependencias de Android ni
 * de chesslib: solo texto PGN por entrada y estructura de datos por salida.
 *
 * Ejemplo de entrada:
 *   "1. e4 e5 2. Nf3 {buen desarrollo} (2. Bc4) Nc6 1-0"
 *
 * Produce una lista raíz con [ElementoMovetext.Jugada] de la línea principal,
 * [ElementoMovetext.Comentario] para "{...}" y ";", [ElementoMovetext.Variante]
 * para cada paréntesis (anidados incluidos), [ElementoMovetext.Nag] para
 * símbolos "$n" y [ElementoMovetext.Resultado] para el marcador final.
 * Los números de jugada y las líneas de tag "[...]" se descartan.
 */

/**
 * Elemento de movetext PGN para su representación en la planilla.
 */
sealed interface ElementoMovetext {

    /**
     * Jugada de la línea principal en notación SAN.
     *
     * @property san Notación SAN de la jugada ("e4", "Nf3", "Qxd4"...).
     */
    data class Jugada(val san: String) : ElementoMovetext

    /**
     * Variante: línea alternativa de análisis entre paréntesis.
     *
     * @property elementos Jugadas (y sub-elementos) de la variante.
     */
    data class Variante(val elementos: List<ElementoMovetext>) : ElementoMovetext

    /**
     * Comentario de la partida ("{...}" o ";...").
     *
     * @property texto Contenido del comentario sin las llaves ni el ';'.
     */
    data class Comentario(val texto: String) : ElementoMovetext

    /**
     * Símbolo NAG de evaluación ("$1" = "!", "$3" = "!!", etc.).
     *
     * @property codigo Código numérico del NAG.
     */
    data class Nag(val codigo: Int) : ElementoMovetext

    /**
     * Resultado final de la partida ("1-0", "0-1", "1/2-1/2", "*").
     *
     * @property texto Marcador en notación PGN.
     */
    data class Resultado(val texto: String) : ElementoMovetext
}

/**
 * Patrón de un número de jugada PGN: "1.", "12...", etc.
 */
private val PATRON_NUMERO_JUGADA = Regex("^\\d+\\.+$")

/**
 * Patrón de número de jugada pegado a su jugada ("1.e4", "12...Nf3").
 */
private val PATRON_NUMERO_PEGADO = Regex("^(\\d+)(\\.{1,3})(\\S+)$")

/**
 * Resultados PGN estándar.
 */
private val RESULTADOS_PGN = setOf("*", "1-0", "0-1", "1/2-1/2")

/**
 * Parsea el movetext PGN y lo convierte en una lista de [ElementoMovetext].
 *
 * @param movetext Texto con el movetext (con o sin cabecera de tags).
 * @return Lista raíz de elementos en orden de aparición.
 */
fun parsearMovetext(movetext: String): List<ElementoMovetext> {
    val raiz = mutableListOf<ElementoMovetext>()
    // Pila de listas: la última es la variante/raíz actual.
    val pila = mutableListOf(raiz)
    for (token in tokenizarMovetext(movetext)) {
        val destino = pila.last()
        when {
            // Apertura de variante: nueva lista en la pila.
            token == "(" -> pila += mutableListOf<ElementoMovetext>()

            // Cierre de variante: sacar y añadir como Variante al padre.
            token == ")" -> {
                if (pila.size > 1) {
                    val variante = pila.removeAt(pila.size - 1)
                    pila.last() += ElementoMovetext.Variante(variante)
                }
            }

            // Comentarios "{...}" y ";resto de línea".
            token.startsWith("{") || token.startsWith(";") -> destino +=
                ElementoMovetext.Comentario(limpiarComentario(token))

            // NAG "$n".
            token.startsWith("$") -> token.drop(1).toIntOrNull()?.let { destino += ElementoMovetext.Nag(it) }

            // Número de jugada suelto: se descarta (la planilla lo regenera).
            PATRON_NUMERO_JUGADA.matches(token) -> Unit

            // Resultado final.
            token in RESULTADOS_PGN -> destino += ElementoMovetext.Resultado(token)

            // Jugada normal o número de jugada pegado ("1.e4").
            else -> {
                val movimiento = despegarNumero(token) ?: token
                if (movimiento.isNotBlank()) destino += ElementoMovetext.Jugada(movimiento)
            }
        }
    }
    return raiz
}

/**
 * Extrae las jugadas SAN de la LÍNEA PRINCIPAL del movetext.
 *
 * Ignora variantes, comentarios, NAGs, números de jugada y resultado. Sirve
 * para rejugar la posición real de la partida con el motor de ajedrez.
 *
 * @param movetext Texto con el movetext PGN.
 * @return Lista de SANs de la línea principal en orden de juego.
 */
fun sansLineaPrincipal(movetext: String): List<String> =
    parsearMovetext(movetext)
        .filterIsInstance<ElementoMovetext.Jugada>()
        .map { it.san }

/**
 * Devuelve el movetext puro de un PGN descartando la cabecera de tags.
 *
 * @param pgn PGN completo (con o sin cabecera "[Event ...]").
 * @return El movetext (jugadas) sin las líneas de tag. Si no hay cabecera,
 *         devuelve el texto tal cual.
 */
fun movetextSinCabecera(pgn: String): String {
    if (pgn.isBlank()) return ""
    val lineas = pgn.lines()
    val inicioMovetext = lineas.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith("[") }
    return if (inicioMovetext < 0) {
        ""
    } else {
        lineas.drop(inicioMovetext).joinToString("\n").trim()
    }
}

/**
 * Tokeniza el movetext PGN respetando los bloques especiales.
 *
 * Devuelve una lista plana de tokens: palabras sueltas (jugadas, números),
 * "(...)" para paréntesis de variante, el contenido completo de los
 * comentarios "{...}" y ";...", los NAGs "$n" y el resultado. Las líneas de
 * tag "[...]" se descartan por completo (no generan token).
 *
 * @param texto Movetext PGN.
 * @return Lista de tokens en orden de aparición.
 */
private fun tokenizarMovetext(texto: String): List<String> {
    val tokens = mutableListOf<String>()
    val palabra = StringBuilder()
    var i = 0
    val n = texto.length

    fun cerrarPalabra() {
        if (palabra.isNotEmpty()) {
            tokens += palabra.toString()
            palabra.clear()
        }
    }

    while (i < n) {
        val c = texto[i]
        when {
            c.isWhitespace() -> {
                cerrarPalabra()
                i++
            }

            // Comentario con llaves: todo hasta '}' (puede contener espacios).
            c == '{' -> {
                cerrarPalabra()
                val fin = texto.indexOf('}', i)
                val finReal = if (fin == -1) n else fin + 1
                tokens += texto.substring(i, finReal)
                i = finReal
            }

            // Comentario de resto de línea: todo hasta el salto de línea.
            c == ';' -> {
                cerrarPalabra()
                var fin = i
                while (fin < n && texto[fin] != '\n') fin++
                tokens += texto.substring(i, fin)
                i = fin
            }

            c == '(' || c == ')' -> {
                cerrarPalabra()
                tokens += c.toString()
                i++
            }

            // NAG "$n": el '$' seguido de dígitos es un token.
            c == '$' -> {
                cerrarPalabra()
                var fin = i + 1
                while (fin < n && texto[fin].isDigit()) fin++
                tokens += texto.substring(i, fin)
                i = fin
            }

            // Tag "[...]": se descarta completo (cabecera de la partida).
            c == '[' -> {
                cerrarPalabra()
                val fin = texto.indexOf(']', i)
                i = if (fin == -1) n else fin + 1
            }

            else -> {
                palabra.append(c)
                i++
            }
        }
    }
    cerrarPalabra()
    return tokens
}

/**
 * Devuelve el texto de un token de comentario sin su delimitador.
 *
 * @param token Token de comentario ("{texto}" o ";texto").
 * @return Contenido limpio del comentario.
 */
private fun limpiarComentario(token: String): String = when {
    token.startsWith("{") -> token.removePrefix("{").removeSuffix("}").trim()
    else -> token.removePrefix(";").trim()
}

/**
 * Separa el número de jugada de una jugada pegada ("1.e4" -> "e4").
 *
 * @param token Token que puede ser número de jugada + jugada pegadas.
 * @return La jugada sin el número, o null si no había número pegado.
 */
private fun despegarNumero(token: String): String? {
    val match = PATRON_NUMERO_PEGADO.matchEntire(token) ?: return null
    return match.groupValues[3].takeIf { it.isNotEmpty() }
}

/**
 * Devuelve el símbolo textual de un código NAG estándar.
 *
 * Mapea los códigos de evaluación más usados (FIDE) a su símbolo ("!" , "!!",
 * "?!"...). Si el código no está en el catálogo, devuelve su forma "$n".
 *
 * @param codigo Código numérico del NAG.
 * @return Símbolo legible del NAG.
 */
fun simboloNag(codigo: Int): String = when (codigo) {
    1 -> "!"
    2 -> "?"
    3 -> "!!"
    4 -> "??"
    5 -> "!?"
    6 -> "?!"
    10 -> "="
    13 -> "∞"
    14 -> "=∞"
    15 -> "∞="
    16 -> "±"
    17 -> "∓"
    18 -> "+−"
    19 -> "−+"
    20 -> "±"
    21 -> "∓"
    22 -> "+−"
    23 -> "−+"
    24 -> "+−"
    25 -> "−+"
    else -> "\$$codigo"
}

/**
 * Anotación (comentario y NAG) asociada a una jugada de la línea principal.
 *
 * @property comentario Texto del comentario "{...}", o null si la jugada no
 *                      tiene comentario.
 * @property nag        Código del NAG ("$n"), o null si la jugada no tiene NAG.
 */
data class AnotacionJugada(
    val comentario: String?,
    val nag: Int?,
)

/**
 * Serializa una lista de [ElementoMovetext] de vuelta a movetext PGN.
 *
 * Regenera la numeración de jugadas de la línea principal ("1. e4 e5 2. Nf3"),
 * conserva variantes entre paréntesis (sin numeración interna), comentarios
 * "{...}", NAGs "$n" y el resultado final. Es la operación inversa de
 * [parsearMovetext] y permite persistir las anotaciones editadas sin perderlas.
 *
 * @param elementos Lista raíz de elementos a serializar.
 * @return Movetext PGN en una sola línea.
 */
fun serializarMovetext(elementos: List<ElementoMovetext>): String {
    val piezas = mutableListOf<String>()
    var ply = 0
    for (elemento in elementos) {
        when (elemento) {
            is ElementoMovetext.Jugada -> {
                ply++
                // Solo las jugadas blancas (ply impar) llevan número delante.
                if (ply % 2 == 1) piezas += "${(ply + 1) / 2}."
                piezas += elemento.san
            }

            is ElementoMovetext.Variante ->
                piezas += "( ${serializarVariante(elemento.elementos)} )"

            is ElementoMovetext.Comentario -> piezas += "{${elemento.texto}}"

            is ElementoMovetext.Nag -> piezas += "\$${elemento.codigo}"

            is ElementoMovetext.Resultado -> piezas += elemento.texto
        }
    }
    return piezas.joinToString(" ")
}

/**
 * Serializa los elementos de una variante sin numeración de jugadas.
 *
 * Dentro de los paréntesis se mantienen los SANs, comentarios y NAGs en orden;
 * los números de jugada se omiten (la planilla los regenera solo en la línea
 * principal).
 *
 * @param elementos Elementos internos de la variante.
 * @return Contenido de la variante en una sola línea.
 */
private fun serializarVariante(elementos: List<ElementoMovetext>): String {
    val piezas = mutableListOf<String>()
    for (elemento in elementos) {
        when (elemento) {
            is ElementoMovetext.Jugada -> piezas += elemento.san

            is ElementoMovetext.Variante ->
                piezas += "( ${serializarVariante(elemento.elementos)} )"

            is ElementoMovetext.Comentario -> piezas += "{${elemento.texto}}"

            is ElementoMovetext.Nag -> piezas += "\$${elemento.codigo}"

            is ElementoMovetext.Resultado -> piezas += elemento.texto
        }
    }
    return piezas.joinToString(" ")
}

/**
 * Devuelve la anotación (comentario y NAG) pegada a una jugada de la línea
 * principal.
 *
 * Recorre la lista raíz contando solo las jugadas de la línea principal (las
 * variantes no se cuentan) y recoge el primer comentario y el primer NAG que
 * aparecen inmediatamente después de la jugada solicitada.
 *
 * @param movetext Movetext PGN.
 * @param ply      Número de jugada (1 = primera jugada de la línea principal).
 * @return [AnotacionJugada] con el comentario y el NAG encontrados, o null en
 *         cada campo si la jugada no tiene esa anotación.
 */
fun anotacionDeJugada(movetext: String, ply: Int): AnotacionJugada {
    val elementos = parsearMovetext(movetext)
    var plyActual = 0
    for ((indice, elemento) in elementos.withIndex()) {
        if (elemento !is ElementoMovetext.Jugada) continue
        plyActual++
        if (plyActual != ply) continue
        var comentario: String? = null
        var nag: Int? = null
        // Recogemos las anotaciones pegadas hasta la siguiente jugada/variante.
        for (siguiente in elementos.drop(indice + 1)) {
            when (siguiente) {
                is ElementoMovetext.Comentario -> if (comentario == null) comentario = siguiente.texto
                is ElementoMovetext.Nag -> if (nag == null) nag = siguiente.codigo
                else -> return AnotacionJugada(comentario, nag)
            }
        }
        return AnotacionJugada(comentario, nag)
    }
    return AnotacionJugada(null, null)
}

/**
 * Reemplaza el comentario y/o el NAG de una jugada de la línea principal.
 *
 * Primero elimina las anotaciones actuales pegadas a esa jugada (comentarios y
 * NAGs) y después inserta las nuevas en orden: primero el comentario "{...}" y
 * luego el NAG "$n". El resultado se devuelve serializado con [serializarMovetext].
 *
 * @param movetext  Movetext PGN a modificar.
 * @param ply       Número de jugada (1 = primera jugada de la línea principal).
 * @param comentario Nuevo texto del comentario, o null para dejarlo sin comentario.
 * @param nag       Nuevo código NAG, o null para dejarlo sin NAG.
 * @return El movetext actualizado.
 */
fun actualizarAnotacionDeJugada(movetext: String, ply: Int, comentario: String?, nag: Int?): String {
    val elementos = parsearMovetext(movetext).toMutableList()
    var plyActual = 0
    var indiceJugada = -1
    for ((indice, elemento) in elementos.withIndex()) {
        if (elemento !is ElementoMovetext.Jugada) continue
        plyActual++
        if (plyActual == ply) {
            indiceJugada = indice
            break
        }
    }
    // Si el ply no existe (partida corta), devolvemos el texto sin cambios.
    if (indiceJugada < 0) return movetext

    // Eliminamos las anotaciones actuales pegadas a la jugada.
    while (indiceJugada + 1 < elementos.size &&
        (elementos[indiceJugada + 1] is ElementoMovetext.Comentario ||
            elementos[indiceJugada + 1] is ElementoMovetext.Nag)
    ) {
        elementos.removeAt(indiceJugada + 1)
    }

    // Insertamos las anotaciones nuevas en orden: comentario y NAG.
    val nuevas = mutableListOf<ElementoMovetext>()
    if (comentario != null) nuevas += ElementoMovetext.Comentario(comentario)
    if (nag != null) nuevas += ElementoMovetext.Nag(nag)
    elementos.addAll(indiceJugada + 1, nuevas)
    return serializarMovetext(elementos)
}

/**
 * Añade una nueva jugada SAN al final de un movetext conservando anotaciones.
 *
 * Mantiene variantes, comentarios, NAGs y resultado ya existentes: la jugada
 * nueva se inserta al final (antes del resultado si lo hubiera) y el movetext
 * se re-serializa. Si el movetext está vacío, genera la numeración desde cero.
 *
 * @param movetext Movetext PGN actual.
 * @param nuevaJugada Jugada SAN a añadir ("Nf3", "O-O", ...).
 * @return El movetext con la jugada añadida.
 */
fun agregarJugadaAlMovetext(movetext: String, nuevaJugada: String): String {
    if (movetext.isBlank()) return movetextDesdeSans(listOf(nuevaJugada))
    val elementos = parsearMovetext(movetext).toMutableList()

    // Extraemos un posible resultado final para recolocarlo tras la jugada.
    val resultado = elementos.lastOrNull() as? ElementoMovetext.Resultado
    if (resultado != null) elementos.removeAt(elementos.lastIndex)

    elementos += ElementoMovetext.Jugada(nuevaJugada)
    val serializado = serializarMovetext(elementos)
    return if (resultado != null) "$serializado ${resultado.texto}" else serializado
}

/**
 * Elimina la última jugada de la línea principal con sus anotaciones pegadas.
 *
 * Retira la última jugada raíz y todo lo que la acompaña (comentarios, NAGs y
 * variantes posteriores). Un resultado final se conserva y se recoloca al
 * final. Si el movetext se queda sin jugadas devuelve cadena vacía.
 *
 * @param movetext Movetext PGN a recortar.
 * @return El movetext sin la última jugada.
 */
fun eliminarUltimaJugadaDelMovetext(movetext: String): String {
    if (movetext.isBlank()) return ""
    val elementos = parsearMovetext(movetext).toMutableList()
    val indiceUltimaJugada = elementos.indexOfLast { it is ElementoMovetext.Jugada }
    // Sin jugadas en la línea principal: no hay nada que eliminar.
    if (indiceUltimaJugada < 0) return movetext

    val resultado = elementos.lastOrNull() as? ElementoMovetext.Resultado
    while (elementos.size > indiceUltimaJugada) elementos.removeAt(indiceUltimaJugada)

    val serializado = serializarMovetext(elementos)
    return if (resultado != null) "$serializado ${resultado.texto}" else serializado
}

/**
 * Paso de un camino dentro del árbol de la planilla.
 */
sealed interface PasoCamino {

    /**
     * Avanza `cantidad` jugadas (1-based) en la lista actual.
     *
     * @property cantidad Número de jugadas que se atraviesan en la línea actual.
     */
    data class Lineal(val cantidad: Int) : PasoCamino

    /**
     * Entra en la variante nº `indice` (0-based) pegada a la posición actual.
     *
     * Las variantes pegadas a una jugada son las que aparecen justo después de
     * ella en el movetext, antes de la siguiente jugada de la línea.
     *
     * @property indice Índice de la variante entre las pegadas a esa jugada.
     */
    data class EntrarVariante(val indice: Int) : PasoCamino
}

/**
 * Camino desde el inicio de la partida hasta un nodo (jugada o variante).
 *
 * Describe de forma inequívoca qué rama del árbol hay que seguir: el primer
 * paso recorre la línea principal y los siguientes entran en variantes
 * anidadas. Un camino que termina en [PasoCamino.Lineal] apunta a esa jugada;
 * uno que termina en [PasoCamino.EntrarVariante] apunta a la propia variante.
 *
 * @property pasos Secuencia de pasos que forma el camino.
 */
data class CaminoPlanilla(val pasos: List<PasoCamino> = emptyList()) {

    /** Añade un paso al final del camino. */
    operator fun plus(paso: PasoCamino): CaminoPlanilla = CaminoPlanilla(pasos + paso)

    companion object {
        /** Camino raíz: inicio de la partida (línea principal). */
        val INICIO = CaminoPlanilla(emptyList())
    }
}

/**
 * Localiza la lista y el índice del nodo objetivo de un camino.
 *
 * [localizarListaFinal] y este método comparten la lógica de recorrido. Este
 * método exige que el último paso sea [PasoCamino.Lineal] y devuelve la lista
 * que contiene esa jugada junto con su índice dentro de ella.
 *
 * @param raiz  Lista raíz ya parseada del movetext.
 * @param camino Camino hasta la jugada objetivo.
 * @return Par (lista, índice) del nodo, o null si el camino no es válido.
 */
private fun localizarEnArbol(
    raiz: MutableList<ElementoMovetext>,
    camino: CaminoPlanilla,
): Pair<MutableList<ElementoMovetext>, Int>? {
    val pasos = camino.pasos
    if (pasos.isEmpty()) return null
    var lista = raiz
    var cursor = 0
    for ((i, paso) in pasos.withIndex()) {
        when (paso) {
            is PasoCamino.Lineal -> {
                var vistos = 0
                var indice = -1
                for (j in cursor until lista.size) {
                    if (lista[j] is ElementoMovetext.Jugada) {
                        vistos++
                        if (vistos == paso.cantidad) {
                            indice = j
                            break
                        }
                    }
                }
                if (indice < 0) return null
                if (i == pasos.lastIndex) return lista to indice
                cursor = indice + 1
            }

            is PasoCamino.EntrarVariante -> {
                var vistos = -1
                var indice = -1
                for (j in cursor until lista.size) {
                    when (lista[j]) {
                        is ElementoMovetext.Jugada -> break
                        is ElementoMovetext.Variante -> {
                            vistos++
                            if (vistos == paso.indice) {
                                indice = j
                                break
                            }
                        }
                        else -> Unit
                    }
                }
                if (indice < 0) return null
                // La variante se crea con una lista mutable en parsearMovetext,
                // por lo que el cast a MutableList es seguro sobre un parse nuevo.
                @Suppress("UNCHECKED_CAST")
                lista = (lista[indice] as ElementoMovetext.Variante).elementos as MutableList<ElementoMovetext>
                cursor = 0
            }
        }
    }
    return null
}

/**
 * Devuelve la lista donde termina un camino (para operar dentro de una variante).
 *
 * Un camino que termina en [PasoCamino.EntrarVariante] deja la posición justo
 * al inicio de la lista de esa variante; uno que termina en
 * [PasoCamino.Lineal] deja la lista de esa jugada.
 *
 * @param raiz  Lista raíz ya parseada del movetext.
 * @param camino Camino hasta el nodo.
 * @return La lista final del recorrido, o null si el camino no es válido.
 */
private fun localizarListaFinal(
    raiz: MutableList<ElementoMovetext>,
    camino: CaminoPlanilla,
): MutableList<ElementoMovetext>? {
    var lista = raiz
    var cursor = 0
    for (paso in camino.pasos) {
        when (paso) {
            is PasoCamino.Lineal -> {
                var vistos = 0
                var indice = -1
                for (j in cursor until lista.size) {
                    if (lista[j] is ElementoMovetext.Jugada) {
                        vistos++
                        if (vistos == paso.cantidad) {
                            indice = j
                            break
                        }
                    }
                }
                if (indice < 0) return null
                cursor = indice + 1
            }

            is PasoCamino.EntrarVariante -> {
                var vistos = -1
                var indice = -1
                for (j in cursor until lista.size) {
                    when (lista[j]) {
                        is ElementoMovetext.Jugada -> break
                        is ElementoMovetext.Variante -> {
                            vistos++
                            if (vistos == paso.indice) {
                                indice = j
                                break
                            }
                        }
                        else -> Unit
                    }
                }
                if (indice < 0) return null
                @Suppress("UNCHECKED_CAST")
                lista = (lista[indice] as ElementoMovetext.Variante).elementos as MutableList<ElementoMovetext>
                cursor = 0
            }
        }
    }
    return lista
}

/**
 * Parsea, localiza el nodo de un camino y le aplica una operación.
 *
 * La operación recibe la lista y el índice del nodo (jugada) objetivo. El
 * resultado se re-serializa desde la raíz, por lo que los cambios en listas
 * anidadas (variantes) se conservan.
 *
 * @param movetext Movetext PGN.
 * @param camino   Camino hasta la jugada objetivo.
 * @param operacion Operación a aplicar sobre (lista, índice).
 * @return El movetext serializado tras la operación (o el original si el
 *         camino no es válido).
 */
private fun localizarYOperar(
    movetext: String,
    camino: CaminoPlanilla,
    operacion: (MutableList<ElementoMovetext>, Int) -> Unit,
): String {
    val raiz = parsearMovetext(movetext).toMutableList()
    val localizado = localizarEnArbol(raiz, camino) ?: return movetext
    operacion(localizado.first, localizado.second)
    return serializarMovetext(raiz)
}

/**
 * Parsea, localiza la lista final de un camino y le aplica una operación.
 *
 * @param movetext Movetext PGN.
 * @param camino   Camino hasta la lista objetivo (normalmente termina en
 *                 [PasoCamino.EntrarVariante]).
 * @param operacion Operación a aplicar sobre la lista final.
 * @return El movetext serializado tras la operación (o el original si el
 *         camino no es válido).
 */
private fun localizarYOperarLista(
    movetext: String,
    camino: CaminoPlanilla,
    operacion: (MutableList<ElementoMovetext>) -> Unit,
): String {
    val raiz = parsearMovetext(movetext).toMutableList()
    val lista = localizarListaFinal(raiz, camino) ?: return movetext
    operacion(lista)
    return serializarMovetext(raiz)
}

/**
 * Devuelve los SANs necesarios para rejugar hasta la posición de un camino.
 *
 * Recorre el camino acumulando los SANs en orden de juego. Al entrar en una
 * variante ([PasoCamino.EntrarVariante]) se CONSERVA la jugada "padre": las
 * jugadas de la variante se reproducen después de esa jugada, porque esta
 * aplicación genera las variantes jugando sobre la posición que deja la
 * jugada seleccionada (la primera jugada de la variante es la respuesta del
 * rival). Rejugando los SANs devueltos desde la posición inicial se obtiene
 * la posición exacta del nodo, tanto en la línea principal como dentro de
 * variantes y subvariantes anidadas.
 *
 * @param movetext Movetext PGN.
 * @param camino   Camino por el árbol de la planilla.
 * @return Lista de SANs en orden de juego a lo largo del camino.
 */
fun sansDeCamino(movetext: String, camino: CaminoPlanilla): List<String> {
    val raiz = parsearMovetext(movetext).toMutableList()
    var lista: List<ElementoMovetext> = raiz
    val sans = mutableListOf<String>()
    var cursor = 0
    for (paso in camino.pasos) {
        when (paso) {
            is PasoCamino.Lineal -> {
                var vistos = 0
                for (j in cursor until lista.size) {
                    val elemento = lista[j]
                    if (elemento is ElementoMovetext.Jugada) {
                        sans += elemento.san
                        vistos++
                        if (vistos == paso.cantidad) {
                            cursor = j + 1
                            break
                        }
                    }
                }
            }

            is PasoCamino.EntrarVariante -> {
                var vistos = -1
                for (j in cursor until lista.size) {
                    when (lista[j]) {
                        is ElementoMovetext.Jugada -> break
                        is ElementoMovetext.Variante -> {
                            vistos++
                            if (vistos == paso.indice) {
                                lista = (lista[j] as ElementoMovetext.Variante).elementos
                                cursor = 0
                                break
                            }
                        }
                        else -> Unit
                    }
                }
                // Se mantiene el SAN de la jugada "padre": las jugadas de la
                // variante se reproducen tras ella (la variante se construye
                // jugando desde la posición que deja esa jugada).
            }
        }
    }
    return sans
}

/**
 * Devuelve la anotación (comentario y NAG) pegada a una jugada del árbol.
 *
 * @param movetext Movetext PGN.
 * @param camino   Camino hasta la jugada (termina en [PasoCamino.Lineal]).
 * @return [AnotacionJugada] con el primer comentario y NAG pegados a la jugada.
 */
fun anotacionEnCamino(movetext: String, camino: CaminoPlanilla): AnotacionJugada {
    val raiz = parsearMovetext(movetext).toMutableList()
    val localizado = localizarEnArbol(raiz, camino) ?: return AnotacionJugada(null, null)
    val (lista, indice) = localizado
    var comentario: String? = null
    var nag: Int? = null
    for (siguiente in lista.drop(indice + 1)) {
        when (siguiente) {
            is ElementoMovetext.Comentario -> if (comentario == null) comentario = siguiente.texto
            is ElementoMovetext.Nag -> if (nag == null) nag = siguiente.codigo
            else -> break
        }
    }
    return AnotacionJugada(comentario, nag)
}

/**
 * Reemplaza el comentario y/o el NAG de una jugada del árbol.
 *
 * @param movetext Movetext PGN a modificar.
 * @param camino   Camino hasta la jugada objetivo.
 * @param comentario Nuevo texto del comentario, o null para dejarlo sin él.
 * @param nag       Nuevo código NAG, o null para dejarlo sin él.
 * @return El movetext actualizado (o el original si el camino no es válido).
 */
fun actualizarAnotacionEnCamino(
    movetext: String,
    camino: CaminoPlanilla,
    comentario: String?,
    nag: Int?,
): String = localizarYOperar(movetext, camino) { lista, indice ->
    // Eliminamos las anotaciones actuales pegadas a la jugada.
    while (indice + 1 < lista.size &&
        (lista[indice + 1] is ElementoMovetext.Comentario ||
            lista[indice + 1] is ElementoMovetext.Nag)
    ) {
        lista.removeAt(indice + 1)
    }
    // Insertamos las anotaciones nuevas en orden: comentario y NAG.
    val nuevas = mutableListOf<ElementoMovetext>()
    if (comentario != null) nuevas += ElementoMovetext.Comentario(comentario)
    if (nag != null) nuevas += ElementoMovetext.Nag(nag)
    lista.addAll(indice + 1, nuevas)
}

/**
 * Inserta una nueva variante tras las ya pegadas a una jugada (se acumulan).
 *
 * La variante se añade después de las anotaciones y variantes existentes de esa
 * jugada (antes de la siguiente jugada), de modo que su índice entre las
 * variantes pegadas coincide con [numeroDeVariantesPegadas] antes de insertar.
 *
 * @param movetext Movetext PGN a modificar.
 * @param camino   Camino hasta la jugada a la que se pega la variante.
 * @param sans     Jugadas SAN de la nueva variante.
 * @return El movetext con la variante insertada (o el original si no es válido).
 */
fun insertarVarianteEnCamino(movetext: String, camino: CaminoPlanilla, sans: List<String>): String =
    localizarYOperar(movetext, camino) { lista, indice ->
        // Avanzamos hasta el final de las anotaciones/variantes pegadas.
        var insercion = indice + 1
        while (insercion < lista.size) {
            when (lista[insercion]) {
                is ElementoMovetext.Jugada, is ElementoMovetext.Resultado -> break
                else -> insercion++
            }
        }
        val variante = ElementoMovetext.Variante(
            sans.map { ElementoMovetext.Jugada(it) }.toMutableList()
        )
        lista.add(insercion, variante)
    }

/**
 * Añade una jugada al final de la línea de una variante.
 *
 * @param movetext Movetext PGN a modificar.
 * @param caminoVariante Camino que termina en [PasoCamino.EntrarVariante]
 *                       (la propia variante que se está construyendo).
 * @param san      Jugada SAN a añadir a esa variante.
 * @return El movetext con la jugada añadida (o el original si no es válido).
 */
fun agregarJugadaAVarianteEnCamino(movetext: String, caminoVariante: CaminoPlanilla, san: String): String =
    localizarYOperarLista(movetext, caminoVariante) { lista ->
        lista += ElementoMovetext.Jugada(san)
    }

/**
 * Cuenta las variantes pegadas a una jugada del árbol.
 *
 * Se usa para calcular el índice de la próxima variante que se añada a esa
 * jugada (ver [PasoCamino.EntrarVariante]).
 *
 * @param movetext Movetext PGN.
 * @param camino   Camino hasta la jugada.
 * @return Número de variantes directamente pegadas a la jugada, o 0 si el
 *         camino no es válido.
 */
fun numeroDeVariantesPegadas(movetext: String, camino: CaminoPlanilla): Int {
    val raiz = parsearMovetext(movetext).toMutableList()
    val localizado = localizarEnArbol(raiz, camino) ?: return 0
    val (lista, indice) = localizado
    var contador = 0
    for (j in indice + 1 until lista.size) {
        when (lista[j]) {
            is ElementoMovetext.Variante -> contador++
            is ElementoMovetext.Jugada, is ElementoMovetext.Resultado -> break
            else -> Unit
        }
    }
    return contador
}
