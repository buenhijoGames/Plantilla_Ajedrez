package com.buenhijogames.plantilla_ajedrez.navegacion

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.buenhijogames.plantilla_ajedrez.ui.ajustes.PantallaAjustes
import com.buenhijogames.plantilla_ajedrez.ui.inicio.PantallaInicio
import com.buenhijogames.plantilla_ajedrez.ui.info.PantallaInfo
import com.buenhijogames.plantilla_ajedrez.ui.tablero.PantallaPartida
import com.buenhijogames.plantilla_ajedrez.ui.torneos.PantallaDetalleTorneo
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
 * El grafo completo de Fase 4 es:
 *   INICIO -> TORNEOS -> DETALLE_TORNEO -> PARTIDA
 * Los argumentos de id viajan como StringType en la ruta; los ViewModels
 * los leen desde el [androidx.lifecycle.SavedStateHandle] con las claves
 * [Destinos.ARG_TORNEO_ID] y [Destinos.ARG_PARTIDA_ID].
 *
 * @param controlador Controller de navegación.
 */
@Composable
fun NavegacionPlantilla(
    controlador: NavHostController,
) {
    NavHost(
        navController = controlador,
        startDestination = Destinos.TORNEOS,
    ) {
        composable(Destinos.INICIO) {
            PantallaInicio(
                onNuevo = { controlador.navigate(Destinos.TORNEOS) },
                onAbrirGuardado = { controlador.navigate(Destinos.TORNEOS) },
                onAjustes = { controlador.navigate(Destinos.AJUSTES) },
                onInfo = { controlador.navigate(Destinos.INFO) },
            )
        }
        composable(Destinos.TORNEOS) {
            PantallaTorneos(
                onAbrirTorneo = { torneoId ->
                    controlador.navigate(Destinos.rutaDetalleTorneo(torneoId))
                },
                onAbrirPartida = { partidaId ->
                    controlador.navigate(Destinos.rutaPartida(partidaId))
                },
                onAjustes = { controlador.navigate(Destinos.AJUSTES) },
                onInfo = { controlador.navigate(Destinos.INFO) },
            )
        }
        composable(
            route = Destinos.DETALLE_TORNEO,
            arguments = listOf(
                navArgument(Destinos.ARG_TORNEO_ID) { type = NavType.StringType },
            ),
        ) {
            PantallaDetalleTorneo(
                onVolver = { controlador.popBackStack() },
                onAbrirPartida = { partidaId ->
                    controlador.navigate(Destinos.rutaPartida(partidaId))
                },
            )
        }
        composable(
            route = Destinos.PARTIDA,
            arguments = listOf(
                navArgument(Destinos.ARG_PARTIDA_ID) { type = NavType.StringType },
            ),
        ) {
            PantallaPartida(
                onVolver = { controlador.popBackStack() },
            )
        }
        composable(Destinos.AJUSTES) {
            PantallaAjustes()
        }
        composable(Destinos.INFO) {
            PantallaInfo(
                onVolver = { controlador.popBackStack() },
            )
        }
    }
}
