package com.buenhijogames.plantilla_ajedrez.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paletas de color de los temas de marca "Madera" y "Mármol".
 *
 * No exponemos texto: sólo colores. La UI los consume vía
 * [PlantillaAjedrezTheme] al construir el `MaterialTheme` activo.
 *
 * Paleta Madera: tonos cálidos de madera noble con acento dorado, evoca
 * clubs de ajedrez tradicionales y tableros artesanos.
 *
 * Paleta Mármol: tonos fríos en gris/azul con acento turquesa, evoca
 * torneos oficiales en salones modernos.
 */

// --------------------------------------------------------------------------
// MADERA
// --------------------------------------------------------------------------

private val MaderaClaro = lightColorScheme(
    primary = Color(0xFF7A4D14),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF2C1700),
    secondary = Color(0xFF6F5B49),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBDDC8),
    onSecondaryContainer = Color(0xFF271913),
    tertiary = Color(0xFF4F6756),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2EED5),
    onTertiaryContainer = Color(0xFF0C2118),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF221A14),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF221A14),
    surfaceVariant = Color(0xFFF2DFD0),
    onSurfaceVariant = Color(0xFF51453A),
    outline = Color(0xFF837468),
    error = Color(0xFF904A48),
    onError = Color(0xFFFFFFFF),
)

private val MaderaOscuro = darkColorScheme(
    primary = Color(0xFFF6B867),
    onPrimary = Color(0xFF482900),
    primaryContainer = Color(0xFF663B00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFDDC0AC),
    onSecondary = Color(0xFF3D2E22),
    secondaryContainer = Color(0xFF554437),
    onSecondaryContainer = Color(0xFFFBDDC8),
    tertiary = Color(0xFFB6D1B9),
    onTertiary = Color(0xFF22372C),
    tertiaryContainer = Color(0xFF384E42),
    onTertiaryContainer = Color(0xFFD2EED5),
    background = Color(0xFF1A120C),
    onBackground = Color(0xFFEDE0D3),
    surface = Color(0xFF1A120C),
    onSurface = Color(0xFFEDE0D3),
    surfaceVariant = Color(0xFF51453A),
    onSurfaceVariant = Color(0xFFD6C3B5),
    outline = Color(0xFF9E8D7F),
    error = Color(0xFFFFB4AC),
    onError = Color(0xFF561E1E),
)

// --------------------------------------------------------------------------
// MÁRMOL
// --------------------------------------------------------------------------

private val MarmolClaro = lightColorScheme(
    primary = Color(0xFF3F5C75),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC3DEFD),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF555F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E3F6),
    onSecondaryContainer = Color(0xFF121C2B),
    tertiary = Color(0xFF2E6B69),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB6F2EE),
    onTertiaryContainer = Color(0xFF00201F),
    background = Color(0xFFF8FAFD),
    onBackground = Color(0xFF191C1F),
    surface = Color(0xFFF8FAFD),
    onSurface = Color(0xFF191C1F),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474F),
    outline = Color(0xFF74777F),
    error = Color(0xFF904A48),
    onError = Color(0xFFFFFFFF),
)

private val MarmolOscuro = darkColorScheme(
    primary = Color(0xFFA3C8FF),
    onPrimary = Color(0xFF003355),
    primaryContainer = Color(0xFF24446C),
    onPrimaryContainer = Color(0xFFC3DEFD),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273041),
    secondaryContainer = Color(0xFF3D4759),
    onSecondaryContainer = Color(0xFFD9E3F6),
    tertiary = Color(0xFF9AD6D2),
    onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF0E4E4D),
    onTertiaryContainer = Color(0xFFB6F2EE),
    background = Color(0xFF111417),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111417),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF43474F),
    onSurfaceVariant = Color(0xFFC4C6CF),
    outline = Color(0xFF8E9199),
    error = Color(0xFFFFB4AC),
    onError = Color(0xFF561E1E),
)

/**
 * Devuelve el par (claro, oscuro) de `ColorScheme` para un [TemaAplicacion].
 *
 * Para [TemaAplicacion.CLARO] y [TemaAplicacion.OSCURO] se usa la paleta
 * por defecto de Material 3 (resuelta más arriba en `PlantillaAjedrezTheme`
 * porque incluye tipografía scheme por defecto de M3).
 *
 * Para [TemaAplicacion.DINAMICO] se devuelve `null` para indicar al caller
 * que debe usar `dynamicLightColorScheme` / `dynamicDarkColorScheme` (o
 * degradar a un esquema estático si no está disponible en la API).
 */
fun esquemasDeMarca(tema: TemaAplicacion): Pair<androidx.compose.material3.ColorScheme, androidx.compose.material3.ColorScheme>? =
    when (tema) {
        TemaAplicacion.MADERA -> MaderaClaro to MaderaOscuro
        TemaAplicacion.MARMOL -> MarmolClaro to MarmolOscuro
        TemaAplicacion.CLARO, TemaAplicacion.OSCURO, TemaAplicacion.DINAMICO -> null
    }