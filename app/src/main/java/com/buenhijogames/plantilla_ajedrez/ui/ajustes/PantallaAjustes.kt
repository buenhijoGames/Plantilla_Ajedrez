package com.buenhijogames.plantilla_ajedrez.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.buenhijogames.plantilla_ajedrez.ui.theme.TemaAplicacion

/**
 * Pantalla de Ajustes con selector de temas y navegación de retorno.
 *
 * @param onVolver  Callback para retroceder en la pila de navegación.
 * @param viewModel Inyectado por Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(
    onVolver: () -> Unit = {},
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ajustes_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.tema_titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.tema_subtitulo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TemaAplicacion.entries.forEach { tema ->
                FilaTema(
                    tema = tema,
                    seleccionado = estado.temaSeleccionado == tema,
                    alSeleccionar = { viewModel.seleccionarTema(tema) },
                )
            }
        }
    }
}

/**
 * Fila individual de selección de tema.
 *
 * Mantiene alineación vertical centrada y etiquetas en `strings.xml`.
 */
@Composable
private fun FilaTema(
    tema: TemaAplicacion,
    seleccionado: Boolean,
    alSeleccionar: () -> Unit,
) {
    val etiquetaRes = when (tema) {
        TemaAplicacion.CLARO -> R.string.tema_claro
        TemaAplicacion.OSCURO -> R.string.tema_oscuro
        TemaAplicacion.DINAMICO -> R.string.tema_dinamico
        TemaAplicacion.MADERA -> R.string.tema_madera
        TemaAplicacion.MARMOL -> R.string.tema_marmol
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = seleccionado,
            onClick = alSeleccionar,
        )
        Text(
            text = stringResource(etiquetaRes),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}