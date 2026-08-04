package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colores del tablero clásico (estilo torneo, agradables en claro y oscuro).
private val ColorCasillaClara = Color(0xFFF0D9B5)
private val ColorCasillaOscura = Color(0xFFB58863)
// Resaltados: selección (ámbar translúcido) y destinos legales (verde).
private val ColorSeleccion = Color(0x99FFC107)
private val ColorDestinoVacio = Color(0x6634A853)
private val ColorDestinoCaptura = Color(0xAA34A853)

/** Número de casillas por lado del tablero. */
private const val CASILLAS_POR_LADO = 8

/**
 * Tablero de ajedrez interactivo dibujado con Canvas.
 *
 * Pinta las 64 casillas alternando colores clásicos, resalta la casilla
 * seleccionada y los destinos legales (círculo para casilla vacía, anillo
 * para captura) y dibuja las piezas a partir del [fen] usando los
 * VectorDrawables de piezas cburnett de Lichess (GPLv2+).
 *
 * El tablero siempre se orienta con las blancas abajo: la fila 0 (arriba)
 * corresponde al rank 8 y la columna 0 (izquierda) al file a.
 *
 * El toque se resuelve convirtiendo el offset al que se pulsó en una casilla
 * algebraica y notificándolo a [onCasillaPulsada] (la lógica de selección y
 * legalidad vive en [PartidaViewModel]).
 *
 * @param fen                FEN actual de la posición.
 * @param casillaSeleccionada Casilla origen seleccionada (o null).
 * @param destinosLegales    Casillas destino legales para resaltar.
 * @param onCasillaPulsada   Callback al pulsar una casilla (algebraica).
 * @param modifier           Modificador (el tablero fuerza aspecto cuadrado).
 */
@Composable
fun TableroAjedrez(
    fen: String,
    casillaSeleccionada: String?,
    destinosLegales: List<String>,
    onCasillaPulsada: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val piezas = remember(fen) { piezasDesdeFen(fen) }
    // Caché ampliada: el tablero mide 16 etiquetas de coordenadas por frame.
    val medidorTexto = rememberTextMeasurer(cacheSize = 32)

    // Sin contentAlignment explícito (TopStart por defecto): las piezas y sus
    // offsets parten de la esquina superior izquierda del tablero.
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
    ) {
        // Tamaño de casilla en Dp (el tablero es cuadrado: alto == ancho).
        val tamanoCasilla = maxWidth / CASILLAS_POR_LADO

        // Coordenadas del borde en tamaño fijo legible (11sp): discretas y
        // claras sobre el color de cada casilla.
        val estiloCoordenadaClara = TextStyle(color = Color(0xCC6B4A2A), fontSize = 11.sp)
        val estiloCoordenadaOscura = TextStyle(color = Color(0xCCF0D9B5), fontSize = 11.sp)

        // Fondo: las 64 casillas y los resaltados de selección/destinos.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tamano = size.width / CASILLAS_POR_LADO
            for (fila in 0 until CASILLAS_POR_LADO) {
                for (columna in 0 until CASILLAS_POR_LADO) {
                    val esClara = (fila + columna) % 2 == 0
                    drawRect(
                        color = if (esClara) ColorCasillaClara else ColorCasillaOscura,
                        topLeft = Offset(columna * tamano, fila * tamano),
                        size = Size(tamano, tamano),
                    )
                }
            }

            // Casilla seleccionada: relleno ámbar translúcido.
            casillaSeleccionada?.let { casilla ->
                val (fila, columna) = filaYColumnaDeCasilla(casilla)
                drawRect(
                    color = ColorSeleccion,
                    topLeft = Offset(columna * tamano, fila * tamano),
                    size = Size(tamano, tamano),
                )
            }

            // Destinos legales: círculo (vacío) o anillo (captura).
            for (destino in destinosLegales) {
                val (fila, columna) = filaYColumnaDeCasilla(destino)
                val centro = Offset((columna + 0.5f) * tamano, (fila + 0.5f) * tamano)
                val hayPieza = piezas.containsKey(destino)
                if (hayPieza) {
                    drawCircle(
                        color = ColorDestinoCaptura,
                        radius = tamano * 0.42f,
                        center = centro,
                        style = Stroke(width = tamano * 0.07f),
                    )
                } else {
                    drawCircle(
                        color = ColorDestinoVacio,
                        radius = tamano * 0.15f,
                        center = centro,
                    )
                }
            }

            // Coordenadas del borde: letras a-h abajo (rank 1) y números 1-8
            // a la izquierda (file a), para orientar al jugador. El color se
            // elige para contrastar con el color de la casilla de fondo.
            val padding = 1.dp.toPx()
            for (columna in 0 until CASILLAS_POR_LADO) {
                val casillaClara = (CASILLAS_POR_LADO - 1 + columna) % 2 == 0
                val estilo = if (casillaClara) estiloCoordenadaClara else estiloCoordenadaOscura
                val etiqueta = medidorTexto.measure(('a' + columna).toString(), estilo)
                drawText(
                    textLayoutResult = etiqueta,
                    topLeft = Offset(
                        x = columna * tamano + tamano - etiqueta.size.width - padding,
                        y = (CASILLAS_POR_LADO - 1) * tamano + tamano - etiqueta.size.height - padding,
                    ),
                )
            }
            for (fila in 0 until CASILLAS_POR_LADO) {
                val casillaClara = fila % 2 == 0
                val estilo = if (casillaClara) estiloCoordenadaClara else estiloCoordenadaOscura
                val etiqueta = medidorTexto.measure((CASILLAS_POR_LADO - fila).toString(), estilo)
                drawText(
                    textLayoutResult = etiqueta,
                    topLeft = Offset(x = padding, y = fila * tamano + padding),
                )
            }
        }

        // Piezas: cada VectorDrawable se pinta sobre su casilla. align(TopStart)
        // es imprescindible: sin él el offset partiría del centro del tablero.
        for ((casilla, pieza) in piezas) {
            val (fila, columna) = filaYColumnaDeCasilla(casilla)
            Image(
                painter = painterResource(recursoPieza(pieza)),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(tamanoCasilla)
                    .offset(x = tamanoCasilla * columna, y = tamanoCasilla * fila),
            )
        }

        // Captura de toques: convierte el offset en casilla y la notifica.
        // La clave de pointerInput incluye tamanoCasilla para que el mapeo
        // toque->casilla se recalcule si el tamaño cambia (rotación, etc.).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(fen, tamanoCasilla) {
                    detectTapGestures { offset ->
                        // PointerInputScope implementa Density: toPx() es seguro.
                        val tamanoCasillaPx = tamanoCasilla.toPx()
                        val columna = (offset.x / tamanoCasillaPx).toInt().coerceIn(0, CASILLAS_POR_LADO - 1)
                        val fila = (offset.y / tamanoCasillaPx).toInt().coerceIn(0, CASILLAS_POR_LADO - 1)
                        onCasillaPulsada(casillaDeFilaColumna(fila, columna))
                    }
                },
        )
    }
}
