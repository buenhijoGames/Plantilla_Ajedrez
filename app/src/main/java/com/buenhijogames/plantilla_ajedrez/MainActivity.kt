package com.buenhijogames.plantilla_ajedrez

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.buenhijogames.plantilla_ajedrez.navegacion.NavegacionPlantilla
import com.buenhijogames.plantilla_ajedrez.preferencias.PreferenciasUsuario
import com.buenhijogames.plantilla_ajedrez.ui.theme.PlantillaAjedrezTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Actividad única de la app.
 *
 * Anotada con [AndroidEntryPoint] para recibir dependencias Hilt. Hospeda
 * la navegación Compose ([NavegacionPlantilla]) envuelta en el tema raíz
 * ([PlantillaAjedrezTheme]), que lee el tema persistido en DataStore.
 *
 * En el futuro, aquí se podrá inicializar el adaptador de Stockfish con
 * un WorkManager/Initializer, pero la lógica de arranque real se va
 * desacoplando hacia ViewModels y el grafo Hilt.
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
                val controlador = rememberNavController()
                NavegacionPlantilla(controlador = controlador)
            }
        }
    }
}