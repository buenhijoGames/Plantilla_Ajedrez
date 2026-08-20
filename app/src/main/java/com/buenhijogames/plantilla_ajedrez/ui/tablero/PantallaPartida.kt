package com.buenhijogames.plantilla_ajedrez.ui.tablero

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buenhijogames.plantilla_ajedrez.R
import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.ui.compartir.CompartirArchivo
import kotlinx.coroutines.launch

/**
 * Pantalla de partida: tablero interactivo + planilla electrónica.
 *
 * Muestra el [TableroAjedrez], indicador de turno y estado de la partida,
 * navegación por jugadas y modo de edición de variantes/anotaciones.
 *
 * Incluye soporte responsivo para orientación vertical y horizontal, botón
 * para girar el tablero, edición de datos de cabecera y cambio manual de resultado.
 *
 * @param onVolver  Navega hacia atrás.
 * @param viewModel Inyectado por Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPartida(
    onVolver: () -> Unit,
    viewModel: PartidaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val configuracion = LocalConfiguration.current
    val esHorizontal = configuracion.orientation == Configuration.ORIENTATION_LANDSCAPE

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val textoArchivoGuardado = stringResource(R.string.snackbar_archivo_guardar_error)
    val textoArchivoGuardadoExito = stringResource(R.string.snackbar_archivo_guardado)

    val formatoPdfPartidaNombre = stringResource(R.string.pdf_partida_nombre)
    val formatoPgnPartidaNombre = stringResource(R.string.pgn_partida_nombre)
    val asuntoCompartir = stringResource(R.string.compartir_asunto)

    // Datos temporales para escribir en SAF CreateDocument
    var bytesParaGuardar by remember { mutableStateOf<ByteArray?>(null) }

    val launcherGuardarArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        val bytes = bytesParaGuardar
        if (uri != null && bytes != null) {
            try {
                contexto.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(bytes)
                }
                scope.launch {
                    snackbarHostState.showSnackbar(textoArchivoGuardadoExito)
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(textoArchivoGuardado)
                }
            }
        }
        bytesParaGuardar = null
    }

    // Diálogo de promoción pendiente
    val promocion = estado.promocionPendiente
    if (promocion != null) {
        DialogoPromocion(
            blancasAlMover = estado.ladoEnTurnoVisible == 'w',
            onElegir = viewModel::confirmarPromocion,
            onCancelar = viewModel::cancelarPromocion,
        )
    }

    // Diálogo para editar datos de cabecera
    if (estado.dialogoEditarCabecera) {
        DialogoEditarCabecera(
            blancasInicial = estado.blancas,
            negrasInicial = estado.negras,
            eventoInicial = estado.evento,
            sitioInicial = estado.sitio,
            fechaInicial = estado.fecha,
            rondaInicial = estado.ronda,
            eloBlancasInicial = estado.eloBlancas,
            eloNegrasInicial = estado.eloNegras,
            onGuardar = viewModel::guardarCabecera,
            onCancelar = viewModel::cerrarDialogoEditarCabecera,
        )
    }

    // Diálogo para cambiar resultado manual
    if (estado.dialogoCambiarResultado) {
        DialogoCambiarResultado(
            resultadoActual = estado.resultadoVisible,
            onSeleccionarResultado = viewModel::establecerResultado,
            onCancelar = viewModel::cerrarDialogoCambiarResultado,
        )
    }

    // Diálogo para configurar el tiempo de reproducción automática
    if (estado.dialogoConfigurarSegundos) {
        DialogoConfigurarSegundos(
            segundosActuales = estado.segundosAuto,
            onConfirmar = viewModel::establecerSegundosAuto,
            onCancelar = viewModel::cerrarDialogoConfigurarSegundos,
        )
    }

    val tituloBarra = when {
        estado.blancas.isNotBlank() && estado.negras.isNotBlank() -> stringResource(
            R.string.partida_enfrentamiento,
            estado.blancas.trim(),
            estado.negras.trim()
        )
        estado.evento.isNotBlank() -> estado.evento.trim()
        else -> stringResource(R.string.partida_titulo)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tituloBarra,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
                actions = {
                    // Botón para girar el tablero (perspectiva de negras)
                    IconButton(
                        onClick = viewModel::alternarGiroTablero,
                        enabled = !estado.cargando && !estado.hayError,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ScreenRotation,
                            contentDescription = stringResource(R.string.partida_girar_tablero),
                        )
                    }

                    // Botón para entrar / salir del modo edición de jugadas
                    IconButton(
                        onClick = {
                            if (estado.modoEdicion) viewModel.salirModoEdicion()
                            else viewModel.entrarModoEdicion()
                        },
                        enabled = !estado.cargando && !estado.hayError,
                    ) {
                        Icon(
                            imageVector = if (estado.modoEdicion) Icons.Filled.Close else Icons.Filled.Edit,
                            contentDescription = stringResource(
                                if (estado.modoEdicion) R.string.partida_salir_edicion
                                else R.string.partida_editar
                            ),
                        )
                    }

                    // Botón para deshacer jugada
                    IconButton(
                        onClick = viewModel::deshacerJugada,
                        enabled = !estado.cargando && estado.jugadasSan.isNotEmpty() &&
                            estado.promocionPendiente == null && !estado.modoEdicion,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Undo,
                            contentDescription = stringResource(R.string.accion_deshacer),
                        )
                    }

                    // Menú overflow (compartir, guardar en disco, editar datos, establecer resultado)
                    OverflowMenuPartida(
                        onEditarCabecera = viewModel::abrirDialogoEditarCabecera,
                        onCambiarResultado = viewModel::abrirDialogoCambiarResultado,
                        onCompartirPdf = {
                            val bytes = viewModel.generarPdfPartida()
                            if (bytes != null) {
                                val nombre = String.format(
                                    formatoPdfPartidaNombre,
                                    estado.blancas.ifBlank { "blancas" },
                                    estado.negras.ifBlank { "negras" },
                                )
                                CompartirArchivo.compartir(
                                    contexto = contexto,
                                    bytes = bytes,
                                    nombre = nombre,
                                    tipoMime = "application/pdf",
                                    asunto = asuntoCompartir,
                                )
                            }
                        },
                        onGuardarPdf = {
                            val bytes = viewModel.generarPdfPartida()
                            if (bytes != null) {
                                bytesParaGuardar = bytes
                                val nombre = String.format(
                                    formatoPdfPartidaNombre,
                                    estado.blancas.ifBlank { "blancas" },
                                    estado.negras.ifBlank { "negras" },
                                )
                                launcherGuardarArchivo.launch(nombre)
                            }
                        },
                        onCompartirPgn = {
                            val pgn = viewModel.exportarPgnPartida()
                            if (pgn != null) {
                                val nombre = String.format(
                                    formatoPgnPartidaNombre,
                                    estado.blancas.ifBlank { "blancas" },
                                    estado.negras.ifBlank { "negras" },
                                )
                                CompartirArchivo.compartir(
                                    contexto = contexto,
                                    bytes = pgn.toByteArray(Charsets.UTF_8),
                                    nombre = nombre,
                                    tipoMime = "application/x-chess-pgn",
                                    asunto = asuntoCompartir,
                                )
                            }
                        },
                        onGuardarPgn = {
                            val pgn = viewModel.exportarPgnPartida()
                            if (pgn != null) {
                                bytesParaGuardar = pgn.toByteArray(Charsets.UTF_8)
                                val nombre = String.format(
                                    formatoPgnPartidaNombre,
                                    estado.blancas.ifBlank { "blancas" },
                                    estado.negras.ifBlank { "negras" },
                                )
                                launcherGuardarArchivo.launch(nombre)
                            }
                        },
                    )
                },
            )
        },
    ) { padding ->
        when {
            estado.cargando -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            estado.hayError -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.partida_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            esHorizontal -> {
                // Layout apaisado / horizontal: Tablero a la izquierda maximizado, Planilla y Controles a la derecha
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Columna izquierda: Tablero maximizado a toda la altura disponible
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        TableroAjedrez(
                            fen = estado.fenVisible,
                            casillaSeleccionada = estado.casillaSeleccionada,
                            destinosLegales = estado.destinosLegales,
                            onCasillaPulsada = viewModel::onCasillaPulsada,
                            girado = estado.tableroGirado,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Columna derecha: Planilla y Controles
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (estado.modoEdicion) {
                            PanelEdicion(
                                caminoSeleccion = estado.caminoSeleccion,
                                comentario = estado.comentarioEdicion,
                                nag = estado.nagEdicion,
                                varianteEnConstruccion = estado.varianteEnConstruccion,
                                onComentarioCambiado = viewModel::actualizarComentarioEdicion,
                                onNagCambiado = viewModel::actualizarNagEdicion,
                                onGuardar = viewModel::guardarEdicion,
                                onSalir = viewModel::salirModoEdicion,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (estado.movetext.isNotBlank()) {
                            PlanillaPartida(
                                movetext = estado.movetext,
                                caminoVisible = estado.caminoVisible,
                                caminoSeleccion = estado.caminoSeleccion,
                                onJugadaPulsada = viewModel::alPulsarJugada,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BarraControlesReproduccion(
                                reproduciendoAuto = estado.reproduciendoAuto,
                                segundosAuto = estado.segundosAuto,
                                onIrAlInicio = viewModel::irAlInicio,
                                onRetroceder = viewModel::retrocederJugada,
                                onAlternarAuto = viewModel::alternarReproduccionAuto,
                                onAvanzar = viewModel::avanzarJugada,
                                onIrAlFinal = viewModel::volverAlFinal,
                                onConfigurarSegundos = viewModel::abrirDialogoConfigurarSegundos,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            else -> {
                // Layout vertical por defecto: Tablero arriba, Planilla abajo
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = textoEstado(estado),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TableroAjedrez(
                            fen = estado.fenVisible,
                            casillaSeleccionada = estado.casillaSeleccionada,
                            destinosLegales = estado.destinosLegales,
                            onCasillaPulsada = viewModel::onCasillaPulsada,
                            girado = estado.tableroGirado,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (estado.modoEdicion) {
                            PanelEdicion(
                                caminoSeleccion = estado.caminoSeleccion,
                                comentario = estado.comentarioEdicion,
                                nag = estado.nagEdicion,
                                varianteEnConstruccion = estado.varianteEnConstruccion,
                                onComentarioCambiado = viewModel::actualizarComentarioEdicion,
                                onNagCambiado = viewModel::actualizarNagEdicion,
                                onGuardar = viewModel::guardarEdicion,
                                onSalir = viewModel::salirModoEdicion,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (estado.movetext.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PlanillaPartida(
                                movetext = estado.movetext,
                                caminoVisible = estado.caminoVisible,
                                caminoSeleccion = estado.caminoSeleccion,
                                onJugadaPulsada = viewModel::alPulsarJugada,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BarraControlesReproduccion(
                                reproduciendoAuto = estado.reproduciendoAuto,
                                segundosAuto = estado.segundosAuto,
                                onIrAlInicio = viewModel::irAlInicio,
                                onRetroceder = viewModel::retrocederJugada,
                                onAlternarAuto = viewModel::alternarReproduccionAuto,
                                onAvanzar = viewModel::avanzarJugada,
                                onIrAlFinal = viewModel::volverAlFinal,
                                onConfigurarSegundos = viewModel::abrirDialogoConfigurarSegundos,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diálogo para editar datos de cabecera de la partida existente.
 */
@Composable
private fun DialogoEditarCabecera(
    blancasInicial: String,
    negrasInicial: String,
    eventoInicial: String,
    sitioInicial: String,
    fechaInicial: String,
    rondaInicial: String,
    eloBlancasInicial: Int?,
    eloNegrasInicial: Int?,
    onGuardar: (blancas: String, negras: String, evento: String, sitio: String, fecha: String, ronda: String, eloBlancas: Int?, eloNegras: Int?) -> Unit,
    onCancelar: () -> Unit,
) {
    var blancas by remember { mutableStateOf(blancasInicial) }
    var negras by remember { mutableStateOf(negrasInicial) }
    var evento by remember { mutableStateOf(eventoInicial) }
    var sitio by remember { mutableStateOf(sitioInicial) }
    var fecha by remember { mutableStateOf(fechaInicial) }
    var ronda by remember { mutableStateOf(rondaInicial) }
    var eloBlancasTexto by remember { mutableStateOf(eloBlancasInicial?.toString() ?: "") }
    var eloNegrasTexto by remember { mutableStateOf(eloNegrasInicial?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.partida_editar_datos)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = blancas,
                    onValueChange = { blancas = it },
                    label = { Text(stringResource(R.string.partida_jugador_blanco)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = eloBlancasTexto,
                    onValueChange = { eloBlancasTexto = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.partida_elo_blancas)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = negras,
                    onValueChange = { negras = it },
                    label = { Text(stringResource(R.string.partida_jugador_negro)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = eloNegrasTexto,
                    onValueChange = { eloNegrasTexto = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.partida_elo_negras)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = evento,
                    onValueChange = { evento = it },
                    label = { Text(stringResource(R.string.partida_evento)) },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sitio,
                        onValueChange = { sitio = it },
                        label = { Text(stringResource(R.string.partida_sitio)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = ronda,
                        onValueChange = { ronda = it },
                        label = { Text(stringResource(R.string.partida_ronda_label)) },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text(stringResource(R.string.partida_fecha)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGuardar(
                        blancas.trim().ifBlank { "Blancas" },
                        negras.trim().ifBlank { "Negras" },
                        evento.trim(),
                        sitio.trim(),
                        fecha.trim(),
                        ronda.trim().ifBlank { "1" },
                        eloBlancasTexto.toIntOrNull(),
                        eloNegrasTexto.toIntOrNull(),
                    )
                },
            ) {
                Text(stringResource(R.string.edicion_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}

/**
 * Diálogo para establecer el resultado manual de la partida.
 */
@Composable
private fun DialogoCambiarResultado(
    resultadoActual: ResultadoPartida,
    onSeleccionarResultado: (ResultadoPartida) -> Unit,
    onCancelar: () -> Unit,
) {
    var resultadoSeleccionado by remember { mutableStateOf(resultadoActual) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.partida_establecer_resultado)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OpcionResultado(
                    texto = stringResource(R.string.resultado_ganan_blancas),
                    seleccionado = resultadoSeleccionado == ResultadoPartida.GANA_BLANCAS,
                    alSeleccionar = { resultadoSeleccionado = ResultadoPartida.GANA_BLANCAS },
                )
                OpcionResultado(
                    texto = stringResource(R.string.resultado_ganan_negras),
                    seleccionado = resultadoSeleccionado == ResultadoPartida.GANA_NEGRAS,
                    alSeleccionar = { resultadoSeleccionado = ResultadoPartida.GANA_NEGRAS },
                )
                OpcionResultado(
                    texto = stringResource(R.string.resultado_tablas),
                    seleccionado = resultadoSeleccionado == ResultadoPartida.TABLAS,
                    alSeleccionar = { resultadoSeleccionado = ResultadoPartida.TABLAS },
                )
                OpcionResultado(
                    texto = stringResource(R.string.resultado_en_curso),
                    seleccionado = resultadoSeleccionado == ResultadoPartida.EN_CURSO,
                    alSeleccionar = { resultadoSeleccionado = ResultadoPartida.EN_CURSO },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSeleccionarResultado(resultadoSeleccionado) }) {
                Text(stringResource(R.string.edicion_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}

@Composable
private fun OpcionResultado(
    texto: String,
    seleccionado: Boolean,
    alSeleccionar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = seleccionado, onClick = alSeleccionar)
        Text(
            text = texto,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Menú overflow de la pantalla de partida.
 */
@Composable
private fun OverflowMenuPartida(
    onEditarCabecera: () -> Unit,
    onCambiarResultado: () -> Unit,
    onCompartirPdf: () -> Unit,
    onGuardarPdf: () -> Unit,
    onCompartirPgn: () -> Unit,
    onGuardarPgn: () -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }
    IconButton(onClick = { expandido = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.accion_mas),
        )
    }
    DropdownMenu(
        expanded = expandido,
        onDismissRequest = { expandido = false },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.partida_editar_datos)) },
            onClick = {
                expandido = false
                onEditarCabecera()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.partida_establecer_resultado)) },
            onClick = {
                expandido = false
                onCambiarResultado()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_compartir_pdf)) },
            onClick = {
                expandido = false
                onCompartirPdf()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_guardar_pdf)) },
            onClick = {
                expandido = false
                onGuardarPdf()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_compartir_pgn)) },
            onClick = {
                expandido = false
                onCompartirPgn()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.accion_guardar_pgn)) },
            onClick = {
                expandido = false
                onGuardarPgn()
            },
        )
    }
}

/**
 * Texto de estado de la partida: turno actual o resultado final.
 */
@Composable
private fun textoEstado(estado: EstadoPartida): String {
    val nombreBlancas = estado.blancas.trim().ifBlank { stringResource(R.string.partida_jugador_blanco) }
    val nombreNegras = estado.negras.trim().ifBlank { stringResource(R.string.partida_jugador_negro) }
    return stringResource(R.string.partida_enfrentamiento, nombreBlancas, nombreNegras)
}

/**
 * Diálogo de promoción de peón.
 */
@Composable
private fun DialogoPromocion(
    blancasAlMover: Boolean,
    onElegir: (Char) -> Unit,
    onCancelar: () -> Unit,
) {
    val piezas = if (blancasAlMover) {
        listOf('Q', 'R', 'B', 'N')
    } else {
        listOf('q', 'r', 'b', 'n')
    }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.promocion_titulo)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (pieza in piezas) {
                    IconButton(onClick = { onElegir(pieza.uppercaseChar()) }) {
                        Image(
                            painter = painterResource(recursoPieza(pieza)),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}

/**
 * Códigos NAG ofrecidos en el panel de edición de una jugada.
 */
private val NAGS_DISPONIBLES: List<Int?> = listOf(null, 1, 2, 3, 4, 5, 6, 10, 13, 16, 18, 19)

/**
 * Panel compacto de edición inline de una jugada en modo edición.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelEdicion(
    caminoSeleccion: CaminoPlanilla?,
    comentario: String,
    nag: Int?,
    varianteEnConstruccion: CaminoPlanilla?,
    onComentarioCambiado: (String) -> Unit,
    onNagCambiado: (Int?) -> Unit,
    onGuardar: () -> Unit,
    onSalir: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.edicion_titulo),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onSalir) {
                    Text(stringResource(R.string.partida_salir_edicion))
                }
            }
            if (caminoSeleccion == null) {
                Text(
                    text = stringResource(R.string.edicion_instrucciones),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = comentario,
                        onValueChange = onComentarioCambiado,
                        label = { Text(stringResource(R.string.edicion_comentario)) },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 2,
                    )
                    TextButton(onClick = onGuardar) {
                        Text(stringResource(R.string.edicion_guardar))
                    }
                }
                Text(
                    text = stringResource(R.string.edicion_simbolo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.heightIn(max = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (codigo in NAGS_DISPONIBLES) {
                        FilterChip(
                            selected = nag == codigo,
                            onClick = { onNagCambiado(if (nag == codigo) null else codigo) },
                            label = {
                                Text(
                                    text = codigo?.let { simboloNag(it) }
                                        ?: stringResource(R.string.edicion_sin_simbolo),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
                if (varianteEnConstruccion != null) {
                    Text(
                        text = stringResource(R.string.edicion_variante_en_curso),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.edicion_variante_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Barra inferior con tarjeta y botones de navegación de la partida:
 * Inicio, Atrás, Reproducción automática (Play/Pause), Adelante, Final y Configurar pausa.
 */
@Composable
private fun BarraControlesReproduccion(
    reproduciendoAuto: Boolean,
    segundosAuto: Int,
    onIrAlInicio: () -> Unit,
    onRetroceder: () -> Unit,
    onAlternarAuto: () -> Unit,
    onAvanzar: () -> Unit,
    onIrAlFinal: () -> Unit,
    onConfigurarSegundos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ir al inicio
            IconButton(onClick = onIrAlInicio) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.control_ir_inicio),
                )
            }

            // Jugada anterior
            IconButton(onClick = onRetroceder) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.control_anterior),
                )
            }

            // Play / Pause automático
            IconButton(onClick = onAlternarAuto) {
                Icon(
                    imageVector = if (reproduciendoAuto) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (reproduciendoAuto) R.string.control_pausar
                        else R.string.control_reproduccion_auto
                    ),
                    tint = if (reproduciendoAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }

            // Jugada siguiente
            IconButton(onClick = onAvanzar) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.control_siguiente),
                )
            }

            // Ir al final
            IconButton(onClick = onIrAlFinal) {
                Icon(
                    imageVector = Icons.Filled.Undo,
                    contentDescription = stringResource(R.string.control_ir_final),
                )
            }

            // Configurar pausa en segundos
            TextButton(
                onClick = onConfigurarSegundos,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
            ) {
                Text(
                    text = "⏱ ${stringResource(R.string.control_velocidad, segundosAuto)}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * Diálogo para configurar el tiempo de pausa (segundos) de la reproducción automática.
 */
@Composable
private fun DialogoConfigurarSegundos(
    segundosActuales: Int,
    onConfirmar: (Int) -> Unit,
    onCancelar: () -> Unit,
) {
    var textoSegundos by remember { mutableStateOf(segundosActuales.toString()) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.control_segundos_dialogo_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.control_segundos_dialogo_mensaje),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = textoSegundos,
                    onValueChange = { textoSegundos = it.filter { c -> c.isDigit() } },
                    label = { Text("Segundos") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val seg = textoSegundos.toIntOrNull() ?: 3
                    onConfirmar(seg)
                },
            ) {
                Text(stringResource(R.string.edicion_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}
