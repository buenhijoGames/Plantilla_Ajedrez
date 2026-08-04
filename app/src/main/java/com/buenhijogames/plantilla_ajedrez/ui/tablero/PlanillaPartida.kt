package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Planilla de la partida con iconos de pieza.
 *
 * Muestra el movetext jugado con las jugadas de pieza representadas por el
 * dibujo de la pieza en lugar de su letra ("Nxd4" se muestra como el icono del
 * caballo + "xd4"). Los números de jugada preceden a cada jugada blanca.
 *
 * La planilla se muestra en un flujo que salta de línea cuando no cabe y se
 * hace desplazable en vertical si hay demasiadas jugadas, para que el tablero
 * conserve el espacio protagonista.
 *
 * @param jugadas Lista de jugadas SAN jugadas hasta el momento.
 * @param modifier Modificador del contenedor de la planilla.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanillaPartida(
    jugadas: List<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 140.dp)
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        jugadas.forEachIndexed { indice, san ->
            // Las blancas mueven en índices pares; cada pareja es una jugada.
            if (indice % 2 == 0) {
                Text(
                    text = "${(indice / 2) + 1}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            JugadaConIcono(san = san)
        }
    }
}

/**
 * Representa una jugada SAN como icono de pieza más texto.
 *
 * Siguiendo el estándar de las planillas, todas las piezas se dibujan con la
 * misma silueta blanca con contorno, sin distinguir el bando que mueve.
 *
 * @param san Jugada SAN ("Nxd4", "e4", "O-O"...).
 */
@Composable
private fun JugadaConIcono(san: String) {
    // esBlanca = true para que segmentosDeSan emita siempre pieza mayúscula
    // (icono de pieza blanca), como es estándar en las planillas.
    val segmentos = remember(san) { segmentosDeSan(san, esBlanca = true) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (segmento in segmentos) {
            when (segmento) {
                is SegmentoSan.Texto -> Text(
                    text = segmento.texto,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface,
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
