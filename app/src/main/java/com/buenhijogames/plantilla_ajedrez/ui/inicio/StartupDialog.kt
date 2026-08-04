package com.buenhijogames.plantilla_ajedrez.ui.inicio

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.buenhijogames.plantilla_ajedrez.R

/**
 * Diálogo modal de arranque.
 *
 * Se muestra la primera vez que el usuario abre la app (o cuando vuelve
 * al inicio sin haber elegido) para preguntar si quiere:
 *   - Crear un nuevo torneo / match / partida aislada.
 *   - Abrir uno de los elementos ya guardados.
 *
 * Tres callbacks para mantener la pantalla desacoplada de la navegación
 * (mejor testeable: no conoce el NavController).
 *
 * @param onNuevo    Llamado al elegir "Nuevo".
 * @param onAbrir    Llamado al elegir "Abrir guardado".
 * @param onCancelar Llamado al cancelar (cierra el diálogo sin navegar).
 * @param onConfirm  No usado; el AlertDialog de M3 requiere el contrato
 *     completo. Aquí lo dejamos sin efecto (no hay acción afirmativa
 *     genérica: cada botón tiene su callback propio).
 */
@Composable
fun StartupDialog(
    onNuevo: () -> Unit,
    onAbrir: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.inicio_titulo)) },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.TextButton(onClick = onAbrir) {
                    Text(stringResource(R.string.inicio_abrir))
                }
                androidx.compose.material3.TextButton(onClick = onNuevo) {
                    Text(stringResource(R.string.inicio_nuevo))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.inicio_cancelar))
            }
        },
    )
}