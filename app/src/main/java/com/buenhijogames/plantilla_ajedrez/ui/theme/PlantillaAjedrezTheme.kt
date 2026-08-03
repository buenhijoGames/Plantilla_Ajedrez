package com.buenhijogames.plantilla_ajedrez.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.buenhijogames.plantilla_ajedrez.preferencias.PreferenciasUsuario

// Paleta por defecto heredada de la fase 0 (M3 estándar púrpura/rosa).
// Se mantiene como fallback M3 para [TemaAplicacion.CLARO] / [OSCURO].
private val EsquemaClaroPorDefecto = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1E192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

private val EsquemaOscuroPorDefecto = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

/**
 * Tema raíz de la app.
 *
 * Reemplaza al `Plantilla_ajedrezTheme` de la Fase 0. Lee el
 * [TemaAplicacion] persistido en DataStore (a través de
 * [PreferenciasUsuario]) y resuelve el [ColorScheme] concreto a aplicar:
 *
 *   - [TemaAplicacion.DINAMICO] intenta Material You si la API lo permite
 *     (Android 12+). En versiones previas degrada a la paleta por defecto
 *     en su variante clara u oscura según el modo del sistema.
 *   - [TemaAplicacion.MADERA] y [TemaAplicacion.MARMOL] usan las paletas
 *     de marca (ver [esquemasDeMarca]) en su variante Clara u Oscura según
 *     [isSystemInDarkTheme]. En el futuro se podrá añadir un toggle
 *     explícito oscuro/claro por tema.
 *   - [TemaAplicacion.CLARO] y [TemaAplicacion.OSCURO] fuerzan la paleta
 *     M3 estándar (esquema claro para CLARO, esquema oscuro para OSCURO).
 *
 * Mientras DataStore emite el primer valor, se aplica `initial = TemaAplicacion.CLARO`
 * para que la UI arranque inmediatamente sin parpadeo de tema.
 *
 * @param contenido Contenido Compose al que se aplica el tema.
 * @param forzarTema Permite al caller (típicamente el `@Preview`) forzar un
 *                   tema concreto sin leer DataStore. Útil para preview
 *                   los cinco temas en paralelo.
 * @param preferencias Si se pasa, el tema se lee reactivamente de DataStore.
 *                     Si es null (caso `@Preview`), se aplica [TemaAplicacion.CLARO]
 *                     salvo que [forzarTema] indique lo contrario.
 */
@Composable
fun PlantillaAjedrezTheme(
    forzarTema: TemaAplicacion? = null,
    preferencias: PreferenciasUsuario? = null,
    contenido: @Composable () -> Unit,
) {
    val contexto = LocalContext.current
    val temaResuelto = when {
        forzarTema != null -> forzarTema
        preferencias != null -> {
            // collectAsState con initial; si el Flow aún no ha emitido,
            // usamos CLARO para evitar parpadeo.
            preferencias.tema.collectAsState(initial = TemaAplicacion.CLARO).value
        }
        else -> TemaAplicacion.CLARO
    }
    val enModoOscuro = isSystemInDarkTheme()

    val esquema = resolverEsquema(temaResuelto, enModoOscuro, contexto)

    MaterialTheme(
        colorScheme = esquema,
        typography = Typography,
        content = contenido,
    )
}

/**
 * Resuelve el [ColorScheme] concreto a partir del [tema] y del modo
 * oscuro del sistema (cuando procede).
 */
private fun resolverEsquema(
    tema: TemaAplicacion,
    enModoOscuro: Boolean,
    contexto: android.content.Context,
): ColorScheme {
    // Primero: temas con esquemas propios de marca.
    esquemasDeMarca(tema)?.let { (claro, oscuro) ->
        return if (enModoOscuro) oscuro else claro
    }
    // DINAMICO: sólo disponible en Android 12+.
    if (tema == TemaAplicacion.DINAMICO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (enModoOscuro) dynamicDarkColorScheme(contexto)
        else dynamicLightColorScheme(contexto)
    }
    // CLARO / OSCURO / DINAMICO en API<12: esquemas M3 por defecto. Ojo:
    // DINAMICO en API<12 fuerza CLARO/OSCURO según el sistema, porque
    // no tiene un esquema dinámico real.
    return when (tema) {
        TemaAplicacion.OSCURO -> EsquemaOscuroPorDefecto
        else -> if (enModoOscuro) EsquemaOscuroPorDefecto else EsquemaClaroPorDefecto
    }
}