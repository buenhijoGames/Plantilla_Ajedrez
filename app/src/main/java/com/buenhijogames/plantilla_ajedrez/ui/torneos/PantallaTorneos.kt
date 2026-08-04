package com.buenhijogames.plantilla_ajedrez.ui.torneos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.buenhijogames.plantilla_ajedrez.R

/**
 * Pantalla de Torneos.
 *
 * **Placeholder de Fase 3b** — la lista real con ViewModel, observación
 * reactiva del repositorio y creación/edición de torneos se implementa
 * en la Fase 3c. Hoy sólo muestra el título y el mensaje de vacío para
 * que el NavHost tenga un destino al que navegar desde el StartupDialog
 * y se pueda verificar flujo y tema visual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTorneos() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.torneos_titulo)) })
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.torneos_vacio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}