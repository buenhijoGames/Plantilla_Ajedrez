package com.buenhijogames.plantilla_ajedrez

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * [Application] de la app.
 *
 * Se anota con [HiltAndroidApp] para que Hilt genere el grafo de dependencias
 * raíz. Es el punto donde se inicializa la inyección de dependencias de toda
 * la app (presentación, data e infraestructura).
 *
 * Esta clase se mantiene lo más ligera posible: la lógica de arranque real
 * (aplicar migraciones Room críticas, inicializadores de fases, etc.) se
 * inyecta vía un [androidx.startup.Initializer] o WorkManager cuando se
 * incorporen esas fases. Aquí sólo arrancamos el contenedor Hilt.
 */
@HiltAndroidApp
class PlantillaApplication : Application()