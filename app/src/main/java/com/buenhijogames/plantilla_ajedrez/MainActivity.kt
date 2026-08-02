package com.buenhijogames.plantilla_ajedrez

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.buenhijogames.plantilla_ajedrez.ui.theme.Plantilla_ajedrezTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad única de la app.
 *
 * Marcella única con [AndroidEntryPoint] para recibir dependencias Hilt. Aquí
 * se hospeda la navegación Compose, pero por ahora mostramos un placeholder
 * centrado hasta que se incorporen las pantallas en la Fase 3.
 *
 * Próximamente aquí se inicializará el NavHost con las rutas
 * (StartupDialog -> Torneos -> Detalle -> Partida -> Ajustes).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Plantilla_ajedrezTheme {
                ContenidoInicial()
            }
        }
    }
}

/**
 * Contenido Compose de marcador de posición mientras se desarrollan las
 * pantallas definitivas. Se centra en pantalla y respetalines de Scaffold.
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
            Text(text = "Plantilla_ajedrez - Fase 0")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoInicialPreview() {
    Plantilla_ajedrezTheme {
        ContenidoInicial()
    }
}