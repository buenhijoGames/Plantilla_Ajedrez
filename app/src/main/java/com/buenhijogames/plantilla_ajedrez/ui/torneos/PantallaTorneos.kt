package com.buenhijogames.plantilla_ajedrez.ui.torneos

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTorneos(
    viewModel: TorneosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    if (estado.dialogoNuevo) {
        DialogoNuevoTorneo(
            onConfirmar = viewModel::crearTorneo,
            onCancelar = viewModel::cerrarDialogoNuevo,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.torneos_titulo)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::abrirDialogoNuevo) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.torneo_nuevo_titulo),
                )
            }
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
                        onEliminar = { viewModel.eliminarTorneo(torneo.id) },
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de un torneo: nombre (cabecera), sitio y fecha (detalle) y un
 * botón de eliminar. La navegación al detalle del torneo se añadirá en
 * una fase posterior (detalle -> partidas).
 */
@Composable
private fun FilaTorneo(
    torneo: Torneo,
    onEliminar: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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