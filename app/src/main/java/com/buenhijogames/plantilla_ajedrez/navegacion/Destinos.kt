package com.buenhijogames.plantilla_ajedrez.navegacion

/**
 * Rutas tipadas de la navegación Compose de la app.
 *
 * Cada constante es una cadena estable (no se renombra sin migración de
 * `arguments`): el `NavBackStackEntry` las persiste en el savedState y
 * un renombre silencioso rompería el estado tras una rotación o
 * muerte de proceso. Por eso todas son `const val` y sin parámetro
 * embebido: los argumentos complejos viajan por NavType en el futuro.
 */
object Destinos {
    /** Pantalla de inicio con [com.buenhijogames.plantilla_ajedrez.ui.inicio.StartupDialog]. */
    const val INICIO = "inicio"

    /** Lista de torneos guardados. */
    const val TORNEOS = "torneos"

    /** Ajustes de la app (selección de tema, etc.). */
    const val AJUSTES = "ajustes"

    /** Detalle de un torneo con sus partidas. Argumento: [ARG_TORNEO_ID]. */
    const val DETALLE_TORNEO = "detalleTorneo/{torneoId}"

    /** Pantalla de partida (tablero). Argumento: [ARG_PARTIDA_ID]. */
    const val PARTIDA = "partida/{partidaId}"

    /** Nombre del argumento de id de torneo (clave del SavedStateHandle). */
    const val ARG_TORNEO_ID = "torneoId"

    /** Nombre del argumento de id de partida (clave del SavedStateHandle). */
    const val ARG_PARTIDA_ID = "partidaId"

    /**
     * Ruta concreta de [DETALLE_TORNEO] para un torneo dado.
     *
     * @param torneoId Id del torneo.
     * @return Ruta de navegación ("detalleTorneo/<id>").
     */
    fun rutaDetalleTorneo(torneoId: String): String = "detalleTorneo/$torneoId"

    /**
     * Ruta concreta de [PARTIDA] para una partida dada.
     *
     * @param partidaId Id de la partida.
     * @return Ruta de navegación ("partida/<id>").
     */
    fun rutaPartida(partidaId: String): String = "partida/$partidaId"
}
