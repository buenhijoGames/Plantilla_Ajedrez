package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buenhijogames.plantilla_ajedrez.R
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida

/**
 * Pantalla de partida: tablero interactivo + cabecera de estado.
 *
 * Muestra el [TableroAjedrez] centrado con el FEN del [PartidaViewModel],
 * un indicador de turno (o del resultado si la partida finalizó) y el
 * movetext jugado hasta el momento. Si la partida requiere promoción de
 * peón se abre [DialogoPromocion].
 *
 * @param onVolver Navega hacia atrás (TopAppBar).
 * @param viewModel Inyectado por Hilt; parámetro para tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPartida(
    onVolver: () -> Unit,
    viewModel: PartidaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Diálogo de promoción pendiente (fuera del Scaffold para no perder foco).
    val promocion = estado.promocionPendiente
    if (promocion != null) {
        DialogoPromocion(
            blancasAlMover = estado.ladoEnTurno == 'w',
            onElegir = viewModel::confirmarPromocion,
            onCancelar = viewModel::cancelarPromocion,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.evento.ifBlank { stringResource(R.string.partida_titulo) }) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::deshacerJugada,
                        enabled = !estado.cargando && estado.jugadasSan.isNotEmpty() &&
                            estado.promocionPendiente == null,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Undo,
                            contentDescription = stringResource(R.string.accion_deshacer),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            estado.cargando -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            estado.hayError -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.partida_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = textoEstado(estado),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TableroAjedrez(
                    fen = estado.fen,
                    casillaSeleccionada = estado.casillaSeleccionada,
                    destinosLegales = estado.destinosLegales,
                    onCasillaPulsada = viewModel::onCasillaPulsada,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (estado.jugadasSan.isNotEmpty()) {
                    PlanillaPartida(
                        jugadas = estado.jugadasSan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * Texto de estado de la partida: turno actual o resultado final.
 *
 * Si la partida sigue en curso muestra "Turno de <nombre>" usando el nombre
 * del jugador o el genérico ("Blancas"/"Negras"). Si finalizó muestra el
 * resultado textual.
 *
 * @param estado Estado de la partida.
 * @return Cadena localizada del estado.
 */
@Composable
private fun textoEstado(estado: EstadoPartida): String {
    val nombreBlancas = estado.blancas.ifBlank { stringResource(R.string.partida_jugador_blanco) }
    val nombreNegras = estado.negras.ifBlank { stringResource(R.string.partida_jugador_negro) }
    return if (estado.resultado == ResultadoPartida.EN_CURSO) {
        val quienToca = if (estado.ladoEnTurno == 'w') nombreBlancas else nombreNegras
        stringResource(R.string.partida_turno, quienToca)
    } else {
        when (estado.resultado) {
            ResultadoPartida.GANA_BLANCAS -> stringResource(R.string.partida_ganan_blancas)
            ResultadoPartida.GANA_NEGRAS -> stringResource(R.string.partida_ganan_negras)
            ResultadoPartida.TABLAS -> stringResource(R.string.partida_tablas)
            ResultadoPartida.EN_CURSO -> stringResource(R.string.partida_en_curso)
        }
    }
}

/**
 * Diálogo de promoción de peón.
 *
 * Ofrece las cuatro piezas (dama, torre, alfil, caballo) en el color del
 * bando que mueve. Cada botón notifica la pieza elegida con el símbolo FEN
 * mayúsculo correspondiente.
 *
 * @param blancasAlMover true si promueve el peón blanco.
 * @param onElegir       Callback con el símbolo de pieza ('Q', 'R', 'B' o 'N').
 * @param onCancelar     Callback al cancelar el diálogo.
 */
@Composable
private fun DialogoPromocion(
    blancasAlMover: Boolean,
    onElegir: (Char) -> Unit,
    onCancelar: () -> Unit,
) {
    val piezas = if (blancasAlMover) {
        listOf('Q', 'R', 'B', 'N')
    } else {
        listOf('q', 'r', 'b', 'n')
    }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.promocion_titulo)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (pieza in piezas) {
                    IconButton(onClick = { onElegir(pieza.uppercaseChar()) }) {
                        Image(
                            painter = painterResource(recursoPieza(pieza)),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}
