package com.buenhijogames.plantilla_ajedrez.ui.torneos

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buenhijogames.plantilla_ajedrez.R

/**
 * Formulario de creación de un torneo nuevo.
 *
 * Campos mínimos del torneo: nombre (obligatorio), sitio y fecha de
 * inicio (opcionales). El resto de campos (fecha fin, árbitro, notas) se
 * completan en la edición del detalle (Fase posterior).
 *
 * @param onConfirmar Recibe (nombre, sitio, fechaInicio) cuando el usuario
 *                    pulsa "Crear". El ViewModel valida el nombre no vacío.
 * @param onCancelar  Cierra el diálogo sin guardar.
 */
@Composable
fun DialogoNuevoTorneo(
    onConfirmar: (nombre: String, sitio: String, fechaInicio: String) -> Unit,
    onCancelar: () -> Unit,
) {
    var nombre by remember { mutableStateOf("") }
    var sitio by remember { mutableStateOf("") }
    var fechaInicio by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.torneo_nuevo_titulo)) },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.torneo_nombre)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = sitio,
                    onValueChange = { sitio = it },
                    label = { Text(stringResource(R.string.torneo_sitio)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = fechaInicio,
                    onValueChange = { fechaInicio = it },
                    label = { Text(stringResource(R.string.torneo_fecha_inicio)) },
                    placeholder = { Text(stringResource(R.string.torneo_fecha_formato)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(nombre, sitio, fechaInicio) },
                enabled = nombre.isNotBlank(),
            ) {
                Text(stringResource(R.string.torneo_crear))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.inicio_cancelar))
            }
        },
    )
}