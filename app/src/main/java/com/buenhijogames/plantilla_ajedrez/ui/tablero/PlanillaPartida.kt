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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Planilla de la partida con figurín y estructura completa del movetext.
 *
 * Renderiza la lista de [ElementoMovetext] producida por [parsearMovetext]:
 * jugadas de la línea principal con el dibujo de la pieza en lugar de la
 * letra, variantes indentadas, comentarios en cursiva, NAGs como símbolos y
 * el resultado final.
 *
 * Las jugadas de la línea principal se pueden pulsar para navegar: al tocar
 * una jugada, [onJugadaPulsada] recibe el número de plies que hay que mostrar
 * en el tablero. La jugada actualmente visible (posicionVisible) se resalta.
 *
 * @param movetext         Movetext PGN completo a mostrar.
 * @param posicionVisible  Número de plies visible (null = posición final).
 * @param onJugadaPulsada  Callback con los plies a mostrar al pulsar una jugada.
 * @param modifier         Modificador del contenedor de la planilla.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanillaPartida(
    movetext: String,
    posicionVisible: Int?,
    onJugadaPulsada: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val elementos = remember(movetext) { parsearMovetext(movetext) }
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 140.dp)
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        var ply = 0
        for (elemento in elementos) {
            when (elemento) {
                is ElementoMovetext.Jugada -> {
                    ply++
                    // Capturamos el valor del ply de ESTA jugada: la lambda
                    // onClick clausura una variable local inmutable, no el var
                    // del bucle (que al pulsar ya valdría el total de jugadas).
                    val plyJugada = ply
                    if (plyJugada % 2 == 1) {
                        Text(
                            text = "${(plyJugada + 1) / 2}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    JugadaConIcono(
                        san = elemento.san,
                        resaltada = posicionVisible == plyJugada,
                        onClick = { onJugadaPulsada(plyJugada) },
                    )
                }

                is ElementoMovetext.Variante -> VarianteVisual(
                    elementos = elemento.elementos,
                    onClick = onJugadaPulsada,
                )

                is ElementoMovetext.Comentario -> Text(
                    text = "{${elemento.texto}}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is ElementoMovetext.Nag -> Text(
                    text = simboloNag(elemento.codigo),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.tertiary,
                )

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
 * Representa una variante como bloque indentado con fondo suave.
 *
 * Las jugadas de la variante se muestran en su propio flujo, con figurín y
 * con un ligero padding e indentación para diferenciarla de la línea
 * principal. Los comentarios y NAGs intercalados se conservan.
 *
 * @param elementos Elementos internos de la variante.
 * @param onClick   Callback de navegación (heredado de la línea principal).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VarianteVisual(
    elementos: List<ElementoMovetext>,
    onClick: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (elemento in elementos) {
            when (elemento) {
                is ElementoMovetext.Jugada -> JugadaConIcono(
                    san = elemento.san,
                    resaltada = false,
                    onClick = {},
                )

                is ElementoMovetext.Comentario -> Text(
                    text = "{${elemento.texto}}",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is ElementoMovetext.Nag -> Text(
                    text = simboloNag(elemento.codigo),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.tertiary,
                )

                else -> Unit
            }
        }
    }
}

/**
 * Representa una jugada SAN como icono de pieza más texto, con resaltado.
 *
 * Siguiendo el estándar de las planillas, todas las piezas se dibujan con la
 * misma silueta blanca con contorno, sin distinguir el bando que mueve.
 *
 * @param san       Jugada SAN ("Nxd4", "e4", "O-O"...).
 * @param resaltada true si es la jugada visible en el tablero.
 * @param onClick   Acción al pulsar la jugada (navegar a su posición).
 */
@Composable
private fun JugadaConIcono(
    san: String,
    resaltada: Boolean,
    onClick: () -> Unit,
) {
    // esBlanca = true para que segmentosDeSan emita siempre pieza mayúscula
    // (icono de pieza blanca), como es estándar en las planillas.
    val segmentos = remember(san) { segmentosDeSan(san, esBlanca = true) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (resaltada) MaterialTheme.colorScheme.tertiaryContainer
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        for (segmento in segmentos) {
            when (segmento) {
                is SegmentoSan.Texto -> Text(
                    text = segmento.texto,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = if (resaltada) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurface,
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
