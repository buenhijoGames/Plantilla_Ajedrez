package com.buenhijogames.plantilla_ajedrez.navegacion

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.buenhijogames.plantilla_ajedrez.ui.ajustes.PantallaAjustes
import com.buenhijogames.plantilla_ajedrez.ui.inicio.PantallaInicio
import com.buenhijogames.plantilla_ajedrez.ui.torneos.PantallaTorneos

/**
 * Grafo de navegación raíz de la app.
 *
 * El [NavHostController] se inyecta desde [com.buenhijogames.plantilla_ajedrez.MainActivity]
 * para poder observar el back stack fuera del composable (por ejemplo, para
 * los Tests de UI). El startDestination es [Destinos.INICIO] porque el flujo
 * inicial de la app de Manolo arranca preguntando al usuario si quiere
 * crear nuevo o abrir uno guardado (ver [PantallaInicio]).
 *
 * @param controlador Controller de navegación.
 * @param onRequestTorneos Callback opcional para navegar a la lista de
 *     torneos cuando el usuario elige "Abrir uno guardado" en el
 *     StartupDialog. Se pasa aquí para mantener la pantalla de inicio
 *     desacoplada del NavController (más testeable).
 */
@Composable
fun NavegacionPlantilla(
    controlador: NavHostController,
) {
    NavHost(
        navController = controlador,
        startDestination = Destinos.INICIO,
    ) {
        composable(Destinos.INICIO) {
            PantallaInicio(
                onNuevo = { controlador.navigate(Destinos.TORNEOS) },
                onAbrirGuardado = { controlador.navigate(Destinos.TORNEOS) },
                onAjustes = { controlador.navigate(Destinos.AJUSTES) },
            )
        }
        composable(Destinos.TORNEOS) {
            PantallaTorneos()
        }
        composable(Destinos.AJUSTES) {
            PantallaAjustes()
        }
    }
}