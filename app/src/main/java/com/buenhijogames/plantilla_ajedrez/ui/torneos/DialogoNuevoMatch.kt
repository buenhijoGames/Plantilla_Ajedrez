package com.buenhijogames.plantilla_ajedrez.ui.torneos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.buenhijogames.plantilla_ajedrez.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diálogo para la creación de un nuevo Match entre 2 jugadores.
 *
 * Captura:
 * - Nombre del match / evento (obligatorio).
 * - Sitio / localidad (opcional).
 * - Fecha del match (por defecto fecha actual, editable).
 * - Jugador 1 (Blancas de la 1ª partida) y su Elo opcional.
 * - Jugador 2 (Negras de la 1ª partida) y su Elo opcional.
 *
 * Al confirmar, se crea el torneo del match y su primera partida con los
 * colores y datos introducidos. Las partidas posteriores en este match
 * alternarán automáticamente los colores de ambos jugadores.
 *
 * @param onConfirmar Callback al pulsar "Crear match".
 * @param onCancelar  Cierra el diálogo sin guardar.
 */
@Composable
fun DialogoNuevoMatch(
    onConfirmar: (
        nombre: String,
        sitio: String,
        fecha: String,
        jugador1: String,
        jugador2: String,
        elo1: Int?,
        elo2: Int?,
    ) -> Unit,
    onCancelar: () -> Unit,
) {
    val fechaHoy = remember {
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
    }

    var nombre by remember { mutableStateOf("") }
    var sitio by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(fechaHoy) }
    var jugador1 by remember { mutableStateOf("") }
    var jugador2 by remember { mutableStateOf("") }
    var elo1Texto by remember { mutableStateOf("") }
    var elo2Texto by remember { mutableStateOf("") }

    val puedeCrear = nombre.isNotBlank() && jugador1.isNotBlank() && jugador2.isNotBlank()

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.match_nuevo_titulo)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.match_nombre)) },
                    placeholder = { Text("Ej: Match Amistoso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sitio,
                    onValueChange = { sitio = it },
                    label = { Text(stringResource(R.string.partida_sitio)) },
                    placeholder = { Text("Ej: Madrid") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text(stringResource(R.string.partida_fecha)) },
                    placeholder = { Text("YYYY.MM.DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = jugador1,
                    onValueChange = { jugador1 = it },
                    label = { Text(stringResource(R.string.match_jugador_1)) },
                    placeholder = { Text("Apellidos, Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = elo1Texto,
                    onValueChange = { elo1Texto = it.filter { char -> char.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.match_elo_1)) },
                    placeholder = { Text("2100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = jugador2,
                    onValueChange = { jugador2 = it },
                    label = { Text(stringResource(R.string.match_jugador_2)) },
                    placeholder = { Text("Apellidos, Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = elo2Texto,
                    onValueChange = { elo2Texto = it.filter { char -> char.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.match_elo_2)) },
                    placeholder = { Text("2150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmar(
                        nombre.trim(),
                        sitio.trim(),
                        fecha.trim(),
                        jugador1.trim(),
                        jugador2.trim(),
                        elo1Texto.toIntOrNull(),
                        elo2Texto.toIntOrNull(),
                    )
                },
                enabled = puedeCrear,
            ) {
                Text(stringResource(R.string.match_crear))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}
