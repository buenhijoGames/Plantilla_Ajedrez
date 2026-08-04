package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Estado de resaltado de una jugada en la planilla.
 *
 * Distingue la jugada visible en el tablero (revisión) de la jugada
 * seleccionada para edición, para que ambas tengan colores distintos.
 */
private enum class EstadoResaltadoJugada {
    /** La jugada está seleccionada en modo edición (se edita su anotación). */
    SELECCIONADA,

    /** La jugada es la visible en el tablero (revisión de la partida). */
    VISIBLE,

    /** Sin resaltado. */
    NINGUNA,
}

/**
 * Planilla de la partida con figurín y estructura completa del movetext.
 *
 * Renderiza la lista de [ElementoMovetext] producida por [parsearMovetext]:
 * jugadas de la línea principal con el dibujo de la pieza en lugar de la
 * letra, variantes indentadas (y subvariantes anidadas), comentarios en
 * cursiva, NAGs como símbolos y el resultado final.
 *
 * Cada jugada se puede pulsar: [onJugadaPulsada] recibe el [CaminoPlanilla]
 * que identifica de forma inequívoca esa jugada (línea principal, variante o
 * subvariante). La jugada visible ([caminoVisible]) se resalta en un color y
 * la seleccionada en modo edición ([caminoSeleccion]) en otro.
 *
 * @param movetext          Movetext PGN completo a mostrar.
 * @param caminoVisible     Camino de la jugada visible en el tablero (null =
 *                          posición final, sin resaltado de revisión).
 * @param onJugadaPulsada   Callback con el camino de la jugada al pulsarla.
 * @param modifier          Modificador del contenedor de la planilla.
 * @param caminoSeleccion   Camino de la jugada seleccionada en modo edición
 *                          (null si no hay selección).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanillaPartida(
    movetext: String,
    caminoVisible: CaminoPlanilla?,
    onJugadaPulsada: (CaminoPlanilla) -> Unit,
    modifier: Modifier = Modifier,
    caminoSeleccion: CaminoPlanilla? = null,
) {
    val elementos = remember(movetext) { parsearMovetext(movetext) }
    ContenidoLista(
        elementos = elementos,
        baseCamino = CaminoPlanilla.INICIO,
        esLineaPrincipal = true,
        caminoVisible = caminoVisible,
        caminoSeleccion = caminoSeleccion,
        onJugadaPulsada = onJugadaPulsada,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 140.dp)
            .verticalScroll(rememberScrollState()),
    )
}

/**
 * Renderiza el contenido de una lista de elementos del movetext.
 *
 * Recorre [elementos] construyendo el [CaminoPlanilla] de cada jugada a partir
 * de [baseCamino] (el camino del punto donde empieza la lista). Las variantes
 * pegadas a una jugada reciben el camino de esa jugada más
 * [PasoCamino.EntrarVariante], y se renderizan recursivamente con
 * [VarianteVisual], lo que permite anidar subvariantes sin límite de
 * profundidad.
 *
 * En la línea principal ([esLineaPrincipal]) se muestran los números de jugada
 * (n. antes de cada jugada impar); dentro de las variantes no se muestran.
 *
 * @param elementos         Elementos de esta lista.
 * @param baseCamino        Camino hasta el inicio de esta lista.
 * @param esLineaPrincipal  true si es la línea principal (muestra numeración).
 * @param caminoVisible     Camino de la jugada visible (resaltado de revisión).
 * @param caminoSeleccion   Camino de la jugada seleccionada (resaltado edición).
 * @param onJugadaPulsada   Callback con el camino de la jugada al pulsarla.
 * @param cursiva           true si los textos (jugadas y NAGs) se muestran en
 *                          cursiva (contenido de análisis dentro de variantes).
 * @param modifier          Modificador del contenedor de esta lista.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContenidoLista(
    elementos: List<ElementoMovetext>,
    baseCamino: CaminoPlanilla,
    esLineaPrincipal: Boolean,
    caminoVisible: CaminoPlanilla?,
    caminoSeleccion: CaminoPlanilla?,
    onJugadaPulsada: (CaminoPlanilla) -> Unit,
    cursiva: Boolean = false,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Jugadas vistas en esta lista y camino de la última jugada, para
        // construir los caminos de esta lista y de las variantes pegadas.
        var jugadasVistas = 0
        var caminoUltimaJugada: CaminoPlanilla? = null
        // Camino de la jugada a la que pertenecen los comentarios/NAGs que
        // aparecen inmediatamente después. Se usa para resaltar los NAGs de
        // la jugada seleccionada en modo edición.
        var caminoJugadaActual: CaminoPlanilla? = null
        var indiceVariante = 0
        for (elemento in elementos) {
            when (elemento) {
                is ElementoMovetext.Jugada -> {
                    jugadasVistas++
                    // Camino de ESTA jugada: se captura en un val inmutable para
                    // que la lambda onClick no cierre sobre la variable mutable.
                    val caminoJugada = baseCamino + PasoCamino.Lineal(jugadasVistas)
                    caminoUltimaJugada = caminoJugada
                    caminoJugadaActual = caminoJugada
                    indiceVariante = 0
                    if (esLineaPrincipal && jugadasVistas % 2 == 1) {
                        Text(
                            text = "${(jugadasVistas + 1) / 2}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    JugadaConIcono(
                        san = elemento.san,
                        resaltado = when {
                            caminoSeleccion == caminoJugada -> EstadoResaltadoJugada.SELECCIONADA
                            caminoVisible == caminoJugada -> EstadoResaltadoJugada.VISIBLE
                            else -> EstadoResaltadoJugada.NINGUNA
                        },
                        cursiva = cursiva,
                        onClick = { onJugadaPulsada(caminoJugada) },
                    )
                }

                is ElementoMovetext.Variante -> {
                    // La variante se pega a la jugada anterior: su camino es el
                    // de esa jugada + el índice entre las variantes pegadas.
                    val caminoVariante = caminoUltimaJugada?.plus(
                        PasoCamino.EntrarVariante(indiceVariante)
                    )
                    if (caminoVariante != null) indiceVariante++
                    VarianteVisual(
                        elementos = elemento.elementos,
                        baseCamino = caminoVariante ?: baseCamino,
                        caminoVisible = caminoVisible,
                        caminoSeleccion = caminoSeleccion,
                        onJugadaPulsada = onJugadaPulsada,
                    )
                }

                is ElementoMovetext.Comentario -> Text(
                    text = "{${elemento.texto}}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is ElementoMovetext.Nag -> {
                    // Se resalta el NAG de la jugada seleccionada en modo edición.
                    val esSeleccionada = caminoSeleccion == caminoJugadaActual
                    Text(
                        text = simboloNag(elemento.codigo),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = if (cursiva) FontStyle.Italic else FontStyle.Normal,
                        ),
                        color = if (esSeleccionada) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                }

                is ElementoMovetext.Resultado -> Text(
                    text = elemento.texto,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Representa una variante como bloque indentado con fondo suave, entre llaves
 * y con las jugadas en cursiva.
 *
 * Las jugadas de la variante (análisis) se muestran en su propio flujo, con
 * figurín, en cursiva y delimitadas por llaves "{ ... }" para distinguirlas
 * claramente de la partida real. Las subvariantes anidadas se renderizan
 * recursivamente con el mismo estilo.
 *
 * @param elementos         Elementos internos de la variante.
 * @param baseCamino        Camino hasta la propia variante (para construir los
 *                          caminos de sus jugadas).
 * @param caminoVisible     Camino de la jugada visible (resaltado de revisión).
 * @param caminoSeleccion   Camino de la jugada seleccionada (resaltado edición).
 * @param onJugadaPulsada   Callback con el camino de la jugada al pulsarla.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VarianteVisual(
    elementos: List<ElementoMovetext>,
    baseCamino: CaminoPlanilla,
    caminoVisible: CaminoPlanilla?,
    caminoSeleccion: CaminoPlanilla?,
    onJugadaPulsada: (CaminoPlanilla) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LlaveCursiva(texto = "{")
        ContenidoLista(
            elementos = elementos,
            baseCamino = baseCamino,
            esLineaPrincipal = false,
            caminoVisible = caminoVisible,
            caminoSeleccion = caminoSeleccion,
            onJugadaPulsada = onJugadaPulsada,
            cursiva = true,
        )
        LlaveCursiva(texto = "}")
    }
}

/**
 * Llave (o delimitador) del bloque de análisis, en cursiva y color atenuado.
 *
 * @param texto Texto de la llave ("{" o "}").
 */
@Composable
private fun LlaveCursiva(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Representa una jugada SAN como icono de pieza más texto, con resaltado.
 *
 * Siguiendo el estándar de las planillas, todas las piezas se dibujan con la
 * misma silueta blanca con contorno, sin distinguir el bando que mueve.
 *
 * @param san       Jugada SAN ("Nxd4", "e4", "O-O"...).
 * @param resaltado Estado de resaltado de la jugada (edición o revisión).
 * @param cursiva   true si el texto de la jugada se muestra en cursiva
 *                  (jugadas de análisis dentro de variantes).
 * @param onClick   Acción al pulsar la jugada (navegar o seleccionar).
 */
@Composable
private fun JugadaConIcono(
    san: String,
    resaltado: EstadoResaltadoJugada,
    cursiva: Boolean = false,
    onClick: () -> Unit,
) {
    // esBlanca = true para que segmentosDeSan emita siempre pieza mayúscula
    // (icono de pieza blanca), como es estándar en las planillas.
    val segmentos = remember(san) { segmentosDeSan(san, esBlanca = true) }
    val colorFondo = when (resaltado) {
        EstadoResaltadoJugada.SELECCIONADA -> MaterialTheme.colorScheme.primaryContainer
        EstadoResaltadoJugada.VISIBLE -> MaterialTheme.colorScheme.tertiaryContainer
        EstadoResaltadoJugada.NINGUNA -> Color.Transparent
    }
    val colorTexto = when (resaltado) {
        EstadoResaltadoJugada.SELECCIONADA -> MaterialTheme.colorScheme.onPrimaryContainer
        EstadoResaltadoJugada.VISIBLE -> MaterialTheme.colorScheme.onTertiaryContainer
        EstadoResaltadoJugada.NINGUNA -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colorFondo)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        for (segmento in segmentos) {
            when (segmento) {
                is SegmentoSan.Texto -> Text(
                    text = segmento.texto,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontStyle = if (cursiva) FontStyle.Italic else FontStyle.Normal,
                    ),
                    color = colorTexto,
                )

                is SegmentoSan.Pieza -> Image(
                    painter = painterResource(recursoPieza(segmento.simboloFen)),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
