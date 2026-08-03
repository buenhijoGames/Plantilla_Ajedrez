package com.buenhijogames.plantilla_ajedrez

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.buenhijogames.plantilla_ajedrez.preferencias.PreferenciasUsuario
import com.buenhijogames.plantilla_ajedrez.ui.theme.PlantillaAjedrezTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Actividad única de la app.
 *
 * Anotada con [AndroidEntryPoint] para recibir dependencias Hilt. Aquí se
 * hospeda la navegación Compose, pero por ahora mostramos un placeholder
 * centrado hasta incorporar las pantallas en la Fase 3b/3c.
 *
 * Hoy ya inyecta [PreferenciasUsuario] para que el tema raíz
 * [PlantillaAjedrezTheme] pueda leer el tema persistido en DataStore.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencias: PreferenciasUsuario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantillaAjedrezTheme(preferencias = preferencias) {
                ContenidoInicial()
            }
        }
    }
}

/**
 * Contenido Compose de marcador de posición mientras se desarrollan las
 * pantallas definitivas. Se centra en pantalla y respeta los insets del
 * Scaffold.
 */
@Composable
private fun ContenidoInicial() {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.fase_actual, "3a"),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}