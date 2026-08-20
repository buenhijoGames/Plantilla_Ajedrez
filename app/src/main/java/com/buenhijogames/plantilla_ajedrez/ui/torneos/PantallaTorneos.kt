package com.buenhijogames.plantilla_ajedrez.ui.torneos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buenhijogames.plantilla_ajedrez.R
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo

/**
 * Pantalla principal de Torneos y Partidas sueltas.
 *
 * Ofrece la lista unificada de torneos y partidas sueltas guardadas, el
 * FAB '+' para crear torneo o partida suelta, el menú de ajustes/información/importar
 * PGN, y confirmación previa de borrado.
 *
 * @param onAbrirTorneo  Navega al detalle de un torneo con su id.
 * @param onAbrirPartida Navega a una partida concreta (suelta o de torneo).
 * @param onAjustes      Navega a la pantalla de Ajustes.
 * @param onInfo         Navega a la pantalla de Información.
 * @param viewModel      Inyectado por Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PantallaTorneos(
    onAbrirTorneo: (String) -> Unit,
    onAbrirPartida: (String) -> Unit,
    onAjustes: () -> Unit = {},
    onInfo: () -> Unit = {},
    viewModel: TorneosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Si se creó una partida suelta, navegamos inmediatamente a ella.
    LaunchedEffect(estado.partidaCreadaId) {
        val id = estado.partidaCreadaId ?: return@LaunchedEffect
        viewModel.limpiarPartidaCreadaId()
        onAbrirPartida(id)
    }

    // Lanzador de SAF para seleccionar archivo PGN.
    val launcherImportarPgn = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val contenido = contexto.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            if (contenido != null) {
                viewModel.importarPgn(contenido)
            }
        }
    }

    val textoImportadoExito = stringResource(R.string.snackbar_pgn_importado_conteo)
    val textoImportadoError = stringResource(R.string.snackbar_pgn_error_importar)

    // Feedback de importación via Snackbar.
    LaunchedEffect(estado.resultadoImportacion) {
        val resultado = estado.resultadoImportacion ?: return@LaunchedEffect
        val mensaje = if (resultado > 0) {
            String.format(textoImportadoExito, resultado)
        } else {
            textoImportadoError
        }
        snackbarHostState.showSnackbar(mensaje)
        viewModel.limpiarResultadoImportacion()
    }

    var torneoAEliminar by remember { mutableStateOf<Torneo?>(null) }
    var partidaAEliminar by remember { mutableStateOf<Partida?>(null) }

    // Diálogos de creación
    if (estado.dialogoNuevoTorneo) {
        DialogoNuevoTorneo(
            onConfirmar = viewModel::crearTorneo,
            onCancelar = viewModel::cerrarDialogoNuevoTorneo,
        )
    }

    if (estado.dialogoNuevaPartida) {
        DialogoNuevaPartida(
            onConfirmar = viewModel::crearPartidaSuelta,
            onCancelar = viewModel::cerrarDialogoNuevaPartida,
        )
    }

    // Diálogo confirmación eliminación de torneo
    torneoAEliminar?.let { torneo ->
        AlertDialog(
            onDismissRequest = { torneoAEliminar = null },
            title = { Text(stringResource(R.string.torneo_eliminar_confirmacion_titulo)) },
            text = { Text(stringResource(R.string.torneo_eliminar_confirmacion_mensaje, torneo.nombre)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarTorneo(torneo.id)
                        torneoAEliminar = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.accion_eliminar),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { torneoAEliminar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }

    // Diálogo confirmación eliminación de partida suelta
    partidaAEliminar?.let { partida ->
        AlertDialog(
            onDismissRequest = { partidaAEliminar = null },
            title = { Text(stringResource(R.string.partida_suelta_eliminar_confirmacion_titulo)) },
            text = {
                Text(
                    stringResource(
                        R.string.partida_suelta_eliminar_confirmacion_mensaje,
                        partida.blancas,
                        partida.negras,
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarPartidaSuelta(partida.id)
                        partidaAEliminar = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.accion_eliminar),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { partidaAEliminar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    OverflowMenuTorneos(
                        onImportarPgn = {
                            launcherImportarPgn.launch(
                                arrayOf("application/x-chess-pgn", "text/plain")
                            )
                        },
                        onAjustes = onAjustes,
                        onInfo = onInfo,
                    )
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = viewModel::abrirMenuCrear) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.torneo_nuevo_titulo),
                    )
                }
                DropdownMenu(
                    expanded = estado.menuCrearAbierto,
                    onDismissRequest = viewModel::cerrarMenuCrear,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.opcion_nuevo_torneo)) },
                        leadingIcon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
                        onClick = viewModel::abrirDialogoNuevoTorneo,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.opcion_nueva_partida)) },
                        leadingIcon = { Icon(Icons.Filled.SportsEsports, contentDescription = null) },
                        onClick = viewModel::abrirDialogoNuevaPartida,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            estado.hayErrorCarga -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.torneos_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            estado.torneos.isEmpty() && estado.partidasSueltas.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.torneos_vacio),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (estado.torneos.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.seccion_torneos),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                        )
                    }
                    items(estado.torneos, key = { "torneo_${it.id}" }) { torneo ->
                        FilaTorneo(
                            torneo = torneo,
                            onAbrir = { onAbrirTorneo(torneo.id) },
                            onEliminar = { torneoAEliminar = torneo },
                        )
                    }
                }

                if (estado.partidasSueltas.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.seccion_partidas_sueltas),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(estado.partidasSueltas, key = { "partida_${it.id}" }) { partida ->
                        FilaPartidaSuelta(
                            partida = partida,
                            onAbrir = { onAbrirPartida(partida.id) },
                            onEliminar = { partidaAEliminar = partida },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de un torneo guardado.
 */
@Composable
private fun FilaTorneo(
    torneo: Torneo,
    onAbrir: () -> Unit,
    onEliminar: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAbrir,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = torneo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = torneo.sitio.ifBlank { torneo.fechaInicio.ifBlank { stringResource(R.string.torneo_sin_datos) } },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.torneo_eliminar),
                )
            }
        }
    }
}

/**
 * Tarjeta de una partida suelta (sin torneo).
 */
@Composable
private fun FilaPartidaSuelta(
    partida: Partida,
    onAbrir: () -> Unit,
    onEliminar: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAbrir,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.partida_enfrentamiento,
                        partida.blancas.ifBlank { stringResource(R.string.partida_jugador_blanco) },
                        partida.negras.ifBlank { stringResource(R.string.partida_jugador_negro) },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${partida.evento.ifBlank { "—" }} • ${partida.fecha}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.torneo_eliminar),
                )
            }
        }
    }
}

/**
 * Menú overflow de la pantalla de torneos.
 */
@Composable
private fun OverflowMenuTorneos(
    onImportarPgn: () -> Unit,
    onAjustes: () -> Unit,
    onInfo: () -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }
    IconButton(onClick = { expandido = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.accion_mas),
        )
    }
    DropdownMenu(
        expanded = expandido,
        onDismissRequest = { expandido = false },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_importar_pgn)) },
            onClick = {
                expandido = false
                onImportarPgn()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_ajustes)) },
            onClick = {
                expandido = false
                onAjustes()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_info)) },
            onClick = {
                expandido = false
                onInfo()
            },
        )
    }
}