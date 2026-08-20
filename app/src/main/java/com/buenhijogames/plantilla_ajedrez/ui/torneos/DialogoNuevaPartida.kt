package com.buenhijogames.plantilla_ajedrez.ui.torneos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.buenhijogames.plantilla_ajedrez.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Datos recogidos para crear una partida suelta o en torneo.
 *
 * @property blancas    Nombre del jugador de blancas.
 * @property negras     Nombre del jugador de negras.
 * @property evento     Nombre del evento / torneo (opcional).
 * @property sitio      Lugar de juego (opcional).
 * @property fecha      Fecha en formato YYYY.MM.DD.
 * @property ronda      Ronda de la partida (opcional).
 * @property eloBlancas Elo de blancas (opcional).
 * @property eloNegras  Elo de negras (opcional).
 */
data class DatosNuevaPartida(
    val blancas: String,
    val negras: String,
    val evento: String,
    val sitio: String,
    val fecha: String,
    val ronda: String,
    val eloBlancas: Int?,
    val eloNegras: Int?,
)

/**
 * Diálogo para crear una nueva partida (suelta o de torneo).
 *
 * @param eventoPorDefecto Nombre del evento precargado (si procede).
 * @param sitioPorDefecto  Sitio precargado (si procede).
 * @param onConfirmar      Callback con los [DatosNuevaPartida] completados.
 * @param onCancelar       Callback al cerrar el diálogo.
 */
@Composable
fun DialogoNuevaPartida(
    eventoPorDefecto: String = "",
    sitioPorDefecto: String = "",
    onConfirmar: (DatosNuevaPartida) -> Unit,
    onCancelar: () -> Unit,
) {
    val fechaHoy = remember {
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
    }

    var blancas by remember { mutableStateOf("") }
    var negras by remember { mutableStateOf("") }
    var evento by remember { mutableStateOf(eventoPorDefecto) }
    var sitio by remember { mutableStateOf(sitioPorDefecto) }
    var fecha by remember { mutableStateOf(fechaHoy) }
    var ronda by remember { mutableStateOf("1") }
    var eloBlancasTexto by remember { mutableStateOf("") }
    var eloNegrasTexto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.partida_nueva_titulo)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = blancas,
                    onValueChange = { blancas = it },
                    label = { Text(stringResource(R.string.partida_jugador_blanco)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = eloBlancasTexto,
                    onValueChange = { eloBlancasTexto = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.partida_elo_blancas)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = negras,
                    onValueChange = { negras = it },
                    label = { Text(stringResource(R.string.partida_jugador_negro)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = eloNegrasTexto,
                    onValueChange = { eloNegrasTexto = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.partida_elo_negras)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = evento,
                    onValueChange = { evento = it },
                    label = { Text(stringResource(R.string.partida_evento)) },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sitio,
                        onValueChange = { sitio = it },
                        label = { Text(stringResource(R.string.partida_sitio)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = ronda,
                        onValueChange = { ronda = it },
                        label = { Text(stringResource(R.string.partida_ronda_label)) },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text(stringResource(R.string.partida_fecha)) },
                    placeholder = { Text("YYYY.MM.DD") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmar(
                        DatosNuevaPartida(
                            blancas = blancas.trim().ifBlank { "Blancas" },
                            negras = negras.trim().ifBlank { "Negras" },
                            evento = evento.trim(),
                            sitio = sitio.trim(),
                            fecha = fecha.trim().ifBlank { fechaHoy },
                            ronda = ronda.trim().ifBlank { "1" },
                            eloBlancas = eloBlancasTexto.toIntOrNull(),
                            eloNegras = eloNegrasTexto.toIntOrNull(),
                        )
                    )
                },
            ) {
                Text(stringResource(R.string.torneo_crear))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}
