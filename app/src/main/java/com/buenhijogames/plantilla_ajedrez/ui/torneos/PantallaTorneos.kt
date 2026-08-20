package com.buenhijogames.plantilla_ajedrez.ui.torneos

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MoreVert
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
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo

/**
 * Pantalla de Torneos.
 *
 * Lista reactiva de torneos guardados observada desde
 * [RepositorioTorneos.observarTorneos]. Un [FloatingActionButton] permite
 * crear un torneo nuevo; el formulario abre [DialogoNuevoTorneo].
 *
 * Estados:
 *   - Cargando: [CircularProgressIndicator] (evita mostrar 'vacío' antes
 *     de que Room emita la lista real).
 *   - Error de lectura: mensaje en [EstadoTorneos.errorCarga] sin tumbar
 *     la app (estabilidad 0% crasheos).
 *   - Vacío: texto de estado vacío.
 *   - Con datos: [LazyColumn] de tarjetas con nombre/sitio/fecha y botón
 *     de eliminar.
 *
 * @param viewModel Inyectado por Hilt por defecto; parámetro para tests.
 * @param onAbrirTorneo Navega al detalle de un torneo con su id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTorneos(
    onAbrirTorneo: (String) -> Unit,
    viewModel: TorneosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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

    if (estado.dialogoNuevo) {
        DialogoNuevoTorneo(
            onConfirmar = viewModel::crearTorneo,
            onCancelar = viewModel::cerrarDialogoNuevo,
        )
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.torneos_titulo)) },
                actions = {
                    OverflowMenuTorneos(
                        onImportarPgn = {
                            launcherImportarPgn.launch(
                                arrayOf("application/x-chess-pgn", "text/plain")
                            )
                        },
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::abrirDialogoNuevo) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.torneo_nuevo_titulo),
                )
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

            estado.torneos.isEmpty() -> Box(
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
                items(estado.torneos, key = { it.id }) { torneo ->
                    FilaTorneo(
                        torneo = torneo,
                        onAbrir = { onAbrirTorneo(torneo.id) },
                        onEliminar = { torneoAEliminar = torneo },
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de un torneo: nombre (cabecera), sitio y fecha (detalle) y un
 * botón de eliminar. Pulsar la tarjeta navega al detalle del torneo
 * (lista de partidas).
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
 * Menu overflow (3 puntos) de la pantalla de torneos.
 *
 * Ofrece "Importar PGN" para importar partidas desde un archivo PGN externo
 * como partidas sueltas (sin torneo asociado).
 *
 * @param onImportarPgn Accion al pulsar "Importar PGN".
 */
@Composable
private fun OverflowMenuTorneos(
    onImportarPgn: () -> Unit,
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
    }
}