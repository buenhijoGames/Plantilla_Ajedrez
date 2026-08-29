package com.buenhijogames.plantilla_ajedrez.ui.info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buenhijogames.plantilla_ajedrez.BuildConfig
import com.buenhijogames.plantilla_ajedrez.R

/**
 * Pantalla de información completa de la app.
 *
 * Muestra detalles sobre la aplicación, cómo usarla, características,
 * autor, licencia, componentes de terceros, código fuente y contacto.
 * Se accede desde el menú overflow de la pantalla de inicio.
 *
 * @param onVolver Callback para navegar hacia atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInfo(
    onVolver: () -> Unit,
) {
    val contexto = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.info_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { CabeceraApp() }
            item { SeccionAcercaDe() }
            item { SeccionComoFunciona() }
            item { SeccionCaracteristicas() }
            item { SeccionAutor() }
            item { SeccionLicencia() }
            item {
                SeccionCodigoFuente(
                    onAbrirUrl = { url ->
                        try {
                            contexto.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                        } catch (_: Exception) {
                            // Sin navegador disponible, no hacer nada.
                        }
                    },
                )
            }
            item { SeccionTerceros() }
            item { SeccionContacto() }
            item { SeccionAgradecimientos() }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Cabecera
// ---------------------------------------------------------------------------

/**
 * Cabecera de la pantalla: icono, nombre de la app y versión.
 *
 * Muesta un icono centrado, el nombre de la aplicación, una frase corta
 * descriptiva y la versión actual (nombre + código interno).
 */
@Composable
private fun CabeceraApp() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.info_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.info_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.info_version_code, BuildConfig.VERSION_CODE),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Secciones
// ---------------------------------------------------------------------------

/**
 * Sección "Acerca de la app": descripción y objetivo.
 */
@Composable
private fun SeccionAcercaDe() {
    SeccionTitulo(texto = stringResource(R.string.info_acerca_titulo))
    TextoParrafo(texto = stringResource(R.string.info_acerca_descripcion))
    Spacer(modifier = Modifier.height(4.dp))
    TextoParrafo(texto = stringResource(R.string.info_acerca_objetivo))
}

/**
 * Sección "Cómo funciona": lista de pasos numerados para usar la app.
 */
@Composable
private fun SeccionComoFunciona() {
    SeccionTitulo(texto = stringResource(R.string.info_como_titulo))
    val pasos = listOf(
        R.string.info_paso1, R.string.info_paso2, R.string.info_paso3,
        R.string.info_paso4, R.string.info_paso5, R.string.info_paso6,
    )
    for (paso in pasos) {
        TextoParrafo(texto = stringResource(paso))
    }
}

/**
 * Sección "Características": lista de funciones de la app con viñetas.
 */
@Composable
private fun SeccionCaracteristicas() {
    SeccionTitulo(texto = stringResource(R.string.info_features_titulo))
    val features = listOf(
        R.string.info_feature_tablero, R.string.info_feature_planilla,
        R.string.info_feature_variantes, R.string.info_feature_pdf,
        R.string.info_feature_pgn, R.string.info_feature_torneos,
        R.string.info_feature_temas, R.string.info_feature_autosave,
        R.string.info_feature_edicion, R.string.info_feature_responsive,
    )
    for (feature in features) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "\u2022",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(16.dp),
            )
            Text(
                text = stringResource(feature),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Sección "Autor": nombre, alias y correo dentro de una tarjeta.
 */
@Composable
private fun SeccionAutor() {
    SeccionTitulo(texto = stringResource(R.string.info_autor_titulo))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.info_autor_nombre),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.info_autor_alias),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.info_autor_correo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Sección "Licencia de la app": tipo, descripción y nota de código abierto.
 */
@Composable
private fun SeccionLicencia() {
    SeccionTitulo(texto = stringResource(R.string.info_licencia_titulo))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.info_licencia_tipo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.info_licencia_descripcion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.info_licencia_abierta),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Sección "Código fuente": tarjeta cliclable que abre el repositorio GitHub oficial.
 *
 * @param onAbrirUrl Callback que recibe la URL y la abre en el navegador.
 */
@Composable
private fun SeccionCodigoFuente(
    onAbrirUrl: (String) -> Unit,
) {
    val url = stringResource(R.string.info_codigo_url)
    SeccionTitulo(texto = stringResource(R.string.info_codigo_titulo))
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onAbrirUrl(url) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.info_codigo_descripcion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}



/**
 * Datos de un componente de terceros (referencias a recursos de strings).
 *
 * @property nombre      Id del recurso del nombre del componente.
 * @property licencia    Id del recurso del nombre corto de la licencia.
 * @property descripcion Id del recurso de la descripción del uso.
 */
private data class TerceroInfo(
    val nombre: Int,
    val licencia: Int,
    val descripcion: Int,
)

/**
 * Sección "Componentes de terceros": lista de tarjetas con nombre,
 * licencia y descripción de cada biblioteca usada.
 */
@Composable
private fun SeccionTerceros() {
    SeccionTitulo(texto = stringResource(R.string.info_terceros_titulo))

    val componentes = listOf(
        TerceroInfo(R.string.info_terceros_piezas, R.string.info_terceros_piezas_licencia, R.string.info_terceros_piezas_desc),
        TerceroInfo(R.string.info_terceros_chesslib, R.string.info_terceros_chesslib_licencia, R.string.info_terceros_chesslib_desc),
        TerceroInfo(R.string.info_terceros_androidx, R.string.info_terceros_androidx_licencia, R.string.info_terceros_androidx_desc),
        TerceroInfo(R.string.info_terceros_kotlin, R.string.info_terceros_kotlin_licencia, R.string.info_terceros_kotlin_desc),
    )

    for ((indice, componente) in componentes.withIndex()) {
        TarjetaTercero(componente = componente)
        if (indice < componentes.lastIndex) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Tarjeta de un componente de terceros: nombre, licencia y descripción.
 *
 * @param componente Datos del componente a mostrar.
 */
@Composable
private fun TarjetaTercero(componente: TerceroInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(componente.nombre),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(componente.licencia),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = stringResource(componente.descripcion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Sección "Contacto": correo electrónico y enlace a GitHub.
 */
@Composable
private fun SeccionContacto() {
    SeccionTitulo(texto = stringResource(R.string.info_contacto_titulo))
    TextoParrafo(texto = stringResource(R.string.info_contacto_correo))
    TextoParrafo(texto = stringResource(R.string.info_contacto_github))
}

/**
 * Sección "Agradecimientos": créditos a Lichess, chesslib y la comunidad.
 */
@Composable
private fun SeccionAgradecimientos() {
    SeccionTitulo(texto = stringResource(R.string.info_agradecimientos_titulo))
    TextoParrafo(texto = stringResource(R.string.info_agradecimientos_lichess))
    TextoParrafo(texto = stringResource(R.string.info_agradecimientos_chesslib))
    TextoParrafo(texto = stringResource(R.string.info_agradecimientos_community))
}

// ---------------------------------------------------------------------------
// Componentes reutilizables
// ---------------------------------------------------------------------------

/**
 * Título de sección con separador horizontal y estilo consistente.
 *
 * @param texto Texto del título de la sección.
 */
@Composable
private fun SeccionTitulo(texto: String) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Párrafo de texto con estilo bodyMedium y color onSurface.
 *
 * @param texto Contenido del párrafo.
 */
@Composable
private fun TextoParrafo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
    )
}