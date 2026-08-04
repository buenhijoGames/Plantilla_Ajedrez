package com.buenhijogames.plantilla_ajedrez.ui.inicio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.buenhijogames.plantilla_ajedrez.R

/**
 * Pantalla de inicio.
 *
 * Muestra el [StartupDialog] justo al entrar para preguntar al usuario si
 * quiere crear un nuevo torneo/match/partida aislada o abrir uno guardado
 * (requisito de `Esta_App.md`: "Al abrir la app se le preguntará si es un
 * nuevo torneo (o match, o partida aislada) o si es uno ya guardado").
 *
 * La TopAppBar sigue el principio minimalista de Manolo: solo un icono
 * de tres puntos (overflow) que despliega "Ajustes". Sin botones sueltos
 * a la vista (regla de Esta_App.md).
 *
 * @param onNuevo    Navega al flujo de nuevo torneo/partida.
 * @param onAbrirGuardado Navega a la lista de torneos guardados.
 * @param onAjustes  Navega a Ajustes. Por defecto lo recibe del caller
 *    para mantener la pantalla testeable; aquí se invoca desde el menú
 *    overflow, así que el NavHost de la Fase 3b ya no lo pasa: la
 *    navegación a Ajustes se hace desde la TopAppBar con el NavController
 *    que esta pantalla sí conoce via callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(
    onNuevo: () -> Unit,
    onAbrirGuardado: () -> Unit,
    onAjustes: () -> Unit = {},
) {
    var mostrarDialogo by remember { mutableStateOf(true) }
    var menuOverflow by remember { mutableStateOf(false) }

    if (mostrarDialogo) {
        StartupDialog(
            onNuevo = {
                mostrarDialogo = false
                onNuevo()
            },
            onAbrir = {
                mostrarDialogo = false
                onAbrirGuardado()
            },
            onCancelar = { mostrarDialogo = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuOverflow = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.accion_mas),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOverflow,
                        onDismissRequest = { menuOverflow = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.accion_ajustes)) },
                            onClick = {
                                menuOverflow = false
                                onAjustes()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                textAlign = TextAlign.Center,
            )
        }
    }
}