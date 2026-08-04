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
}