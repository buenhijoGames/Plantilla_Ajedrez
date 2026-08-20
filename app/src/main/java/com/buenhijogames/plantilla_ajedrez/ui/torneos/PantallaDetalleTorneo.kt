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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Torneo
import com.buenhijogames.plantilla_ajedrez.ui.compartir.CompartirArchivo

/**
 * Pantalla de detalle de un torneo.
 *
 * Muestra la información del torneo y la lista de sus partidas (reactiva).
 * Un [FloatingActionButton] crea una partida nueva y navega a ella. Pulsar
 * una partida existente también navega a su pantalla.
 *
 * @param onVolver     Navega hacia atrás (TopAppBar).
 * @param onAbrirPartida Navega a la pantalla de partida con su id.
 * @param viewModel    Inyectado por Hilt; parámetro para tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleTorneo(
    onVolver: () -> Unit,
    onAbrirPartida: (String) -> Unit,
    viewModel: DetalleTorneoViewModel = hiltViewModel(),
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

    // Feedback de importación via Snackbar.
    LaunchedEffect(estado.resultadoImportacion) {
        val resultado = estado.resultadoImportacion ?: return@LaunchedEffect
        val mensaje = if (resultado > 0) {
            contexto.getString(R.string.snackbar_pgn_importado_conteo, resultado)
        } else {
            contexto.getString(R.string.snackbar_pgn_error_importar)
        }
        snackbarHostState.showSnackbar(mensaje)
        viewModel.limpiarResultadoImportacion()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.torneo?.nombre ?: stringResource(R.string.torneo_detalle_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
                actions = {
                    OverflowMenuTorneo(
                        torneoNombre = estado.torneo?.nombre ?: "",
                        onExportarPdf = {
                            val bytes = viewModel.generarPdfTorneo()
                            if (bytes != null) {
                                val nombre = contexto.getString(
                                    R.string.pdf_torneo_nombre,
                                    estado.torneo?.nombre?.ifBlank { "torneo" } ?: "torneo",
                                )
                                CompartirArchivo.compartir(
                                    contexto = contexto,
                                    bytes = bytes,
                                    nombre = nombre,
                                    tipoMime = "application/pdf",
                                    asunto = contexto.getString(R.string.compartir_asunto),
                                )
                            }
                        },
                        onExportarPgn = {
                            val pgn = viewModel.exportarPgnTorneo()
                            if (pgn != null) {
                                val nombre = contexto.getString(
                                    R.string.pgn_torneo_nombre,
                                    estado.torneo?.nombre?.ifBlank { "torneo" } ?: "torneo",
                                )
                                CompartirArchivo.compartir(
                                    contexto = contexto,
                                    bytes = pgn.toByteArray(Charsets.UTF_8),
                                    nombre = nombre,
                                    tipoMime = "application/x-chess-pgn",
                                    asunto = contexto.getString(R.string.compartir_asunto),
                                )
                            }
                        },
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
            FloatingActionButton(
                onClick = { viewModel.crearPartida(onCreada = onAbrirPartida) },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.partida_nueva_titulo),
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
                    text = stringResource(R.string.torneo_detalle_error),
                    color = MaterialTheme.colorScheme.error,
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
                // Cabecera con los datos del torneo.
                estado.torneo?.let { torneo ->
                    item(key = "cabecera") {
                        TarjetaDatosTorneo(torneo = torneo)
                    }
                }
                if (estado.partidas.isEmpty()) {
                    item(key = "vacio") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.torneo_detalle_vacio),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    items(estado.partidas, key = { it.id }) { partida ->
                        FilaPartida(
                            partida = partida,
                            onClick = { onAbrirPartida(partida.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta con la información del torneo (sitio, fechas, árbitro).
 *
 * @param torneo Torneo a mostrar.
 */
@Composable
private fun TarjetaDatosTorneo(torneo: Torneo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val detalle = listOf(
                torneo.sitio,
                torneo.fechaInicio,
                torneo.arbitro,
            ).filter { it.isNotBlank() }
            if (detalle.isNotEmpty()) {
                Text(
                    text = detalle.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (torneo.notas.isNotBlank()) {
                Text(
                    text = torneo.notas,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Fila de una partida del torneo: ronda, enfrentamiento y resultado.
 *
 * @param partida Partida a mostrar.
 * @param onClick Navega a la pantalla de la partida.
 */
@Composable
private fun FilaPartida(
    partida: Partida,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val blancas = partida.blancas.ifBlank { stringResource(R.string.partida_jugador_blanco) }
                val negras = partida.negras.ifBlank { stringResource(R.string.partida_jugador_negro) }
                Text(
                    text = stringResource(R.string.partida_enfrentamiento, blancas, negras),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.partida_ronda, partida.ronda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = partida.resultado.pgn,
                style = MaterialTheme.typography.titleMedium,
                color = if (partida.resultado == ResultadoPartida.EN_CURSO) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

/**
 * Menu overflow (3 puntos) de la pantalla de detalle de un torneo.
 *
 * Ofrece "Exportar PDF", "Exportar PGN" e "Importar PGN". El PDF genera un
 * documento multipágina (una hoja FIDE por cada partida del torneo), el PGN
 * exporta todas las partidas en un solo archivo, e Importar permite añadir
 * partidas desde un archivo PGN externo al torneo actual.
 *
 * @param torneoNombre Nombre del torneo (para el nombre del fichero).
 * @param onExportarPdf Accion al pulsar "Exportar PDF".
 * @param onExportarPgn Accion al pulsar "Exportar PGN".
 * @param onImportarPgn Accion al pulsar "Importar PGN".
 */
@Composable
private fun OverflowMenuTorneo(
    torneoNombre: String,
    onExportarPdf: () -> Unit,
    onExportarPgn: () -> Unit,
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
            text = { Text(stringResource(R.string.accion_exportar_pdf)) },
            onClick = {
                expandido = false
                onExportarPdf()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_exportar_pgn)) },
            onClick = {
                expandido = false
                onExportarPgn()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_importar_pgn)) },
            onClick = {
                expandido = false
                onImportarPgn()
            },
        )
    }
}
