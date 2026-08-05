package com.buenhijogames.plantilla_ajedrez.ui.tablero

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
 * Muestra el [TableroAjedrez] a tamaño completo con el FEN del
 * [PartidaViewModel], un indicador de turno (o del resultado si la partida
 * finalizó) y el movetext jugado hasta el momento. Si la partida requiere
 * promoción de peón se abre [DialogoPromocion].
 *
 * El botón "Editar" de la barra superior activa el modo edición, en el que se
 * muestra [PanelEdicion] para editar el comentario/NAG de una jugada y añadir
 * variantes jugando en el tablero. El tablero mantiene siempre su tamaño
 * completo; el contenido es scrollable para que el teclado no oculte los
 * controles.
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
            blancasAlMover = estado.ladoEnTurnoVisible == 'w',
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
                        onClick = {
                            if (estado.modoEdicion) viewModel.salirModoEdicion()
                            else viewModel.entrarModoEdicion()
                        },
                        enabled = !estado.cargando && !estado.hayError,
                    ) {
                        Icon(
                            imageVector = if (estado.modoEdicion) Icons.Filled.Close else Icons.Filled.Edit,
                            contentDescription = stringResource(
                                if (estado.modoEdicion) R.string.partida_salir_edicion
                                else R.string.partida_editar
                            ),
                        )
                    }
                    IconButton(
                        onClick = viewModel::deshacerJugada,
                        enabled = !estado.cargando && estado.jugadasSan.isNotEmpty() &&
                            estado.promocionPendiente == null && !estado.modoEdicion,
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Sección superior (estado, tablero, panel de edición): ocupa su
                // altura natural y solo scrollea si no cabe (p. ej. horizontal o
                // con el teclado abierto).
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = textoEstado(estado),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TableroAjedrez(
                        fen = estado.fenVisible,
                        casillaSeleccionada = estado.casillaSeleccionada,
                        destinosLegales = estado.destinosLegales,
                        onCasillaPulsada = viewModel::onCasillaPulsada,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (estado.modoEdicion) {
                        PanelEdicion(
                            caminoSeleccion = estado.caminoSeleccion,
                            comentario = estado.comentarioEdicion,
                            nag = estado.nagEdicion,
                            varianteEnConstruccion = estado.varianteEnConstruccion,
                            onComentarioCambiado = viewModel::actualizarComentarioEdicion,
                            onNagCambiado = viewModel::actualizarNagEdicion,
                            onGuardar = viewModel::guardarEdicion,
                            onSalir = viewModel::salirModoEdicion,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                // Zona de planilla: ocupa el resto del espacio libre y scrollea
                // internamente.
                if (estado.movetext.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlanillaPartida(
                            movetext = estado.movetext,
                            caminoVisible = estado.caminoVisible,
                            caminoSeleccion = estado.caminoSeleccion,
                            onJugadaPulsada = viewModel::alPulsarJugada,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                        if (estado.caminoVisible != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = viewModel::volverAlFinal) {
                                Text(stringResource(R.string.partida_volver_final))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Texto de estado de la partida: turno actual o resultado final.
 *
 * @param estado Estado de la partida.
 * @return Cadena localizada del estado.
 */
@Composable
private fun textoEstado(estado: EstadoPartida): String {
    val nombreBlancas = estado.blancas.ifBlank { stringResource(R.string.partida_jugador_blanco) }
    val nombreNegras = estado.negras.ifBlank { stringResource(R.string.partida_jugador_negro) }
    val resultado = estado.resultadoVisible
    return if (resultado == ResultadoPartida.EN_CURSO) {
        val quienToca = if (estado.ladoEnTurnoVisible == 'w') nombreBlancas else nombreNegras
        stringResource(R.string.partida_turno, quienToca)
    } else {
        when (resultado) {
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

/**
 * Códigos NAG ofrecidos en el panel de edición de una jugada.
 */
private val NAGS_DISPONIBLES: List<Int?> = listOf(null, 1, 2, 3, 4, 5, 6, 10, 13, 16, 18, 19)

/**
 * Panel compacto de edición inline de una jugada en modo edición.
 *
 * Muestra un único Card con una fila de comentario+guardar y debajo los chips
 * de NAG. Si no hay jugada seleccionada muestra una instrucción; si hay
 * variante en construcción muestra un indicador. Todo en alto mínimo para no
 * invadir el espacio del tablero.
 *
 * @param caminoSeleccion       Camino de la jugada seleccionada (null si no hay).
 * @param comentario            Texto del comentario en edición.
 * @param nag                   Código NAG seleccionado (null = sin símbolo).
 * @param varianteEnConstruccion Camino de la variante que se está creando.
 * @param onComentarioCambiado  Callback al cambiar el texto del comentario.
 * @param onNagCambiado         Callback al cambiar el NAG (null = sin símbolo).
 * @param onGuardar             Callback al pulsar "Guardar".
 * @param onSalir               Callback al pulsar "Salir edición".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelEdicion(
    caminoSeleccion: CaminoPlanilla?,
    comentario: String,
    nag: Int?,
    varianteEnConstruccion: CaminoPlanilla?,
    onComentarioCambiado: (String) -> Unit,
    onNagCambiado: (Int?) -> Unit,
    onGuardar: () -> Unit,
    onSalir: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Cabecera: título + salir.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.edicion_titulo),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onSalir) {
                    Text(stringResource(R.string.partida_salir_edicion))
                }
            }
            if (caminoSeleccion == null) {
                // Sin selección: instrucción breve.
                Text(
                    text = stringResource(R.string.edicion_instrucciones),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // Comentario + botón Guardar en una sola fila.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = comentario,
                        onValueChange = onComentarioCambiado,
                        label = { Text(stringResource(R.string.edicion_comentario)) },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 2,
                    )
                    TextButton(onClick = onGuardar) {
                        Text(stringResource(R.string.edicion_guardar))
                    }
                }
                // Chips de NAG en una fila compacta.
                Text(
                    text = stringResource(R.string.edicion_simbolo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.heightIn(max = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (codigo in NAGS_DISPONIBLES) {
                        FilterChip(
                            selected = nag == codigo,
                            onClick = { onNagCambiado(if (nag == codigo) null else codigo) },
                            label = {
                                Text(
                                    text = codigo?.let { simboloNag(it) }
                                        ?: stringResource(R.string.edicion_sin_simbolo),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
                // Indicador de variante en construcción.
                if (varianteEnConstruccion != null) {
                    Text(
                        text = stringResource(R.string.edicion_variante_en_curso),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.edicion_variante_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
