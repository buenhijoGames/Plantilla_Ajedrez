package com.buenhijogames.plantilla_ajedrez.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.VectorDrawable
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import com.buenhijogames.plantilla_ajedrez.data.R
import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida
import com.buenhijogames.plantilla_ajedrez.domain.pdf.PuertoPdf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion de [PuertoPdf] con `android.graphics.pdf.PdfDocument`.
 *
 * Dibuja la plantilla FIDE (hoja A4 vertical) con el Seven Tag Roster en la
 * cabecera (Evento, Sitio, Fecha, Ronda, Blancas, Negras, Resultado + Elos)
 * y una tabla de dos columnas (Blancas / Negras) con casillas numeradas,
 * igual que las plantillas fisicas oficiales FIDE.
 *
 * Las jugadas se dibujan con **figurin** (silueta cburnett): cada SAN se
 * descompone con [PlanillaFide.segmentar] y los segmentos de pieza se
 * rasterizan desde los VectorDrawables copiados a `:data/res/drawable`.
 * Asi el PDF es 100% offline y el figurin coincide con el de la app.
 *
 * Para varias partidas ([generarPlantillas]) se crea una pagina por partida.
 *
 * @param contexto Contexto de la aplicacion (para inflar los drawables).
 */
@Singleton
class AdaptadorPdf @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : PuertoPdf {

    // --- Constantes de la hoja A4 (en puntos; 1 punto = 1/72 pulgada). ---
    private val ANCHO_HOJA = 595f
    private val ALTO_HOJA = 842f
    private val MARGEN = 40f

    // --- Pinturas reutilizadas (se configuran una vez). ---
    private val pinturaTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val pinturaTextoNegrita = Paint(pinturaTexto).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pinturaTitulo = Paint(pinturaTextoNegrita).apply { textSize = 16f }
    private val pinturaSubtitulo = Paint(pinturaTexto).apply {
        textSize = 9f
        color = Color.DKGRAY
    }
    private val pinturaEtiqueta = Paint(pinturaTextoNegrita).apply { textSize = 9f }
    private val pinturaLinea = Paint().apply {
        color = Color.BLACK
        strokeWidth = 0.8f
        style = Paint.Style.STROKE
    }
    private val pinturaLineaGruesa = Paint(pinturaLinea).apply { strokeWidth = 1.6f }

    override fun generarPlantilla(partida: Partida): ByteArray =
        generarPlantillas(listOf(partida))

    override fun generarPlantillas(partidas: List<Partida>): ByteArray {
        val documento = PdfDocument()
        try {
            for (partida in partidas) {
                val info = PdfDocument.PageInfo.Builder(
                    ANCHO_HOJA.toInt(),
                    ALTO_HOJA.toInt(),
                    1,
                ).create()
                val pagina = documento.startPage(info)
                dibujarHoja(pagina.canvas, PlanillaFide.construir(partida))
                documento.finishPage(pagina)
            }
            // `toByteArray()` solo existe en API 33+; para minSdk 27 usamos
            // `writeTo(OutputStream)` y volcamos el resultado a un ByteArray.
            val flujo = java.io.ByteArrayOutputStream()
            documento.writeTo(flujo)
            return flujo.toByteArray()
        } finally {
            documento.close()
        }
    }

    /**
     * Dibuja la plantilla FIDE completa en el [canvas] de una pagina.
     *
     * @param canvas Canvas de la pagina PDF.
     * @param plantilla Datos de la plantilla (cabecera + filas con figurin).
     */
    private fun dibujarHoja(canvas: Canvas, plantilla: PlanillaFide.Plantilla) {
        dibujarTitulo(canvas)
        dibujarCabecera(canvas, plantilla.cabecera)
        dibujarTabla(canvas, plantilla.filas)
    }

    /** Dibuja el titulo y subtitulo centrados en la parte superior. */
    private fun dibujarTitulo(canvas: Canvas) {
        val titulo = contexto.getString(R.string.pdf_titulo)
        val subtitulo = contexto.getString(R.string.pdf_subtitulo)
        val anchoTitulo = pinturaTitulo.measureText(titulo)
        canvas.drawText(titulo, (ANCHO_HOJA - anchoTitulo) / 2f, MARGEN + 16f, pinturaTitulo)
        val anchoSub = pinturaSubtitulo.measureText(subtitulo)
        canvas.drawText(subtitulo, (ANCHO_HOJA - anchoSub) / 2f, MARGEN + 30f, pinturaSubtitulo)
    }

    /**
     * Dibuja la cabecera con los tags de la partida en dos columnas.
     *
     * Filas: (Evento | Sitio), (Fecha | Ronda), (Blancas | Negras) y una fila
     * de resultado centrada. Los nombres de jugador incluyen su Elo si existe.
     *
     * @param canvas Canvas de la pagina.
     * @param cabecera Datos de cabecera de la partida.
     */
    private fun dibujarCabecera(canvas: Canvas, cabecera: PlanillaFide.Cabecera) {
        val mitadX = (ANCHO_HOJA - 2 * MARGEN) / 2f
        var y = MARGEN + 48f
        val altoFila = 16f
        val espacioX = 8f

        fun dibujarPar(etiqueta1: String, valor1: String, etiqueta2: String, valor2: String) {
            dibujarEtiquetaValor(canvas, MARGEN, y, etiqueta1, valor1, mitadX - espacioX)
            dibujarEtiquetaValor(canvas, MARGEN + mitadX + espacioX, y, etiqueta2, valor2, mitadX - espacioX)
            y += altoFila
        }

        dibujarPar(
            contexto.getString(R.string.pdf_evento), cabecera.evento,
            contexto.getString(R.string.pdf_sitio), cabecera.sitio,
        )
        dibujarPar(
            contexto.getString(R.string.pdf_fecha), cabecera.fecha,
            contexto.getString(R.string.pdf_ronda), cabecera.ronda,
        )

        val blanco = nombreConElo(cabecera.blancas, cabecera.eloBlancas)
        val negro = nombreConElo(cabecera.negras, cabecera.eloNegras)
        dibujarPar(
            contexto.getString(R.string.pdf_blancas), blanco,
            contexto.getString(R.string.pdf_negras), negro,
        )

        val resultado = cabecera.resultado.pgn
        val etiquetaResultado = contexto.getString(R.string.pdf_resultado)
        val textoResultado = "$etiquetaResultado: $resultado"
        val anchoResultado = pinturaTextoNegrita.measureText(textoResultado)
        val yResultado = y + 18f
        canvas.drawText(
            textoResultado,
            (ANCHO_HOJA - anchoResultado) / 2f,
            yResultado,
            pinturaTextoNegrita,
        )

        // Linea divisoria bajo la cabecera (separada del resultado).
        canvas.drawLine(MARGEN, yResultado + 10f, ANCHO_HOJA - MARGEN, yResultado + 10f, pinturaLineaGruesa)
    }

    /**
     * Dibuja la tabla de jugadas Blancas/Negras con casillas numeradas.
     *
     * Cada fila de la tabla es una jugada completa: numero de jugada a la
     * izquierda y las jugadas de blancas/negras con figurin. Las filas se
     * rellenan hasta [MAXIMO_FILAS]; si faltan jugadas, quedan casillas en
     * blanco (para anotar a mano), igual que una plantilla fisica.
     *
     * @param canvas Canvas de la pagina.
     * @param filas Filas con las jugadas (puede ser vacia).
     */
    private fun dibujarTabla(canvas: Canvas, filas: List<PlanillaFide.Fila>) {
        val anchoUtil = ANCHO_HOJA - 2 * MARGEN
        val anchoNumero = 32f
        val anchoColumna = (anchoUtil - anchoNumero) / 2f
        val inicioX = MARGEN
        val top = MARGEN + 96f
        val maximoFilas = 30

        // Cabecera de la tabla: solo el numero de jugada (las columnas
        // Blancas/Negras ya aparecen en la cabecera de arriba, no se repiten).
        dibujarTextoEn(canvas, inicioX + 4f, top + 10f, contexto.getString(R.string.pdf_numero), pinturaEtiqueta)
        // Linea de cabecera de la tabla (separada de la primera jugada).
        val inicioFilasY = top + 20f
        canvas.drawLine(inicioX, inicioFilasY - 2f, ANCHO_HOJA - MARGEN, inicioFilasY - 2f, pinturaLinea)

        val altoFila = (ALTO_HOJA - inicioFilasY - MARGEN) / maximoFilas

        for (indiceFila in 0 until maximoFilas) {
            val yTop = inicioFilasY + indiceFila * altoFila
            val yCentro = yTop + altoFila / 2f
            val fila = filas.getOrNull(indiceFila)

            // Numero de jugada.
            if (fila != null) {
                val numero = fila.numero.toString()
                val anchoNum = pinturaTexto.measureText(numero)
                canvas.drawText(numero, inicioX + anchoNumero - anchoNum - 4f, yCentro + 4f, pinturaTexto)
            }

            // Columnas Blancas / Negras con figurin.
            dibujarJugadaEnCelda(canvas, inicioX + anchoNumero, yCentro, anchoColumna, fila?.blancas)
            dibujarJugadaEnCelda(canvas, inicioX + anchoNumero + anchoColumna, yCentro, anchoColumna, fila?.negras)

            // Linea separadora de la fila.
            canvas.drawLine(inicioX, yTop + altoFila, ANCHO_HOJA - MARGEN, yTop + altoFila, pinturaLinea)
        }
    }

    /**
     * Dibuja la jugada (figurin + texto) dentro de la celda, centrada verticalmente.
     *
     * @param canvas Canvas de la pagina.
     * @param xInicio Coordenada X izquierda de la celda.
     * @param yCentro Coordenada Y del centro de la celda.
     * @param anchoCelda Ancho disponible de la celda.
     * @param segmentos Segmentos del figurin (null si la casilla esta vacia).
     */
    private fun dibujarJugadaEnCelda(
        canvas: Canvas,
        xInicio: Float,
        yCentro: Float,
        anchoCelda: Float,
        segmentos: List<SegmentoFigurin>?,
    ) {
        if (segmentos.isNullOrEmpty()) return
        var x = xInicio + 6f
        val altoIcono = 14f
        for (segmento in segmentos) {
            when (segmento) {
                is SegmentoFigurin.Texto -> {
                    val texto = segmento.texto
                    val ancho = pinturaTexto.measureText(texto)
                    if (x + ancho > xInicio + anchoCelda - 4f) break
                    canvas.drawText(texto, x, yCentro + 4f, pinturaTexto)
                    x += ancho
                }

                is SegmentoFigurin.Pieza -> {
                    val icono = obtenerFigurin(segmento.simboloFen) ?: continue
                    val izquierda = x
                    val derecha = izquierda + altoIcono
                    if (derecha > xInicio + anchoCelda - 4f) break
                    val rect = RectF(izquierda, yCentro - altoIcono / 2f, derecha, yCentro + altoIcono / 2f)
                    canvas.drawBitmap(icono, null, rect, null)
                    x = derecha
                }
            }
        }
    }

    /** Dibuja una etiqueta + valor con "Etiqueta: valor" truncado al ancho. */
    private fun dibujarEtiquetaValor(
        canvas: Canvas,
        x: Float,
        y: Float,
        etiqueta: String,
        valor: String,
        ancho: Float,
    ) {
        val textoEtiqueta = "$etiqueta:"
        val anchoEtiqueta = pinturaEtiqueta.measureText(textoEtiqueta)
        canvas.drawText(textoEtiqueta, x, y + 10f, pinturaEtiqueta)
        val disponible = ancho - anchoEtiqueta - 8f
        val valorRecortado = recortarTexto(valor, disponible, pinturaTexto)
        canvas.drawText(valorRecortado, x + anchoEtiqueta + 6f, y + 10f, pinturaTexto)
    }

    /**
     * Combina el nombre de un jugador con su Elo, si existe.
     *
     * @param nombre Nombre del jugador.
     * @param elo Elo del jugador o null.
     * @return "Nombre" o "Nombre (Elo 2500)".
     */
    private fun nombreConElo(nombre: String, elo: Int?): String =
        if (elo != null) "$nombre ($elo)" else nombre

    /**
     * Recorta un texto con elipsis para que quepa en [ancho] pixels.
     *
     * @param texto Texto original.
     * @param ancho Ancho maximo disponible.
     * @param pintura Pintura con la que se dibujara.
     * @return Texto recortado con "..." si no cabe completo.
     */
    private fun recortarTexto(texto: String, ancho: Float, pintura: Paint): String {
        if (pintura.measureText(texto) <= ancho) return texto
        var fin = texto.length
        while (fin > 0 && pintura.measureText(texto.substring(0, fin) + "…") > ancho) {
            fin--
        }
        return texto.substring(0, fin) + "…"
    }

    /**
     * Rasteriza el figurin de una pieza a [Bitmap] (con cache).
     *
     * @param simboloFen Caracter FEN de la pieza ('N', 'p', ...).
     * @return Bitmap del figurin, o null si no se pudo cargar.
     */
    private fun obtenerFigurin(simboloFen: Char): Bitmap? {
        cacheFigurines[simboloFen]?.let { return it }
        val drawable = ContextCompat.getDrawable(contexto, recursoPiezaPdf(simboloFen))
        if (drawable !is VectorDrawable) return null
        val ancho = drawable.intrinsicWidth.coerceAtLeast(1)
        val alto = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        drawable.setBounds(0, 0, ancho, alto)
        drawable.draw(lienzo)
        cacheFigurines[simboloFen] = bitmap
        return bitmap
    }

    /** Dibuja texto en [x],[y] con la pintura dada (helper para una linea). */
    private fun dibujarTextoEn(canvas: Canvas, x: Float, y: Float, texto: String, pintura: Paint) {
        canvas.drawText(texto, x, y, pintura)
    }

    companion object {
        /** Cache de figurines rasterizados (por simbolo FEN). */
        private val cacheFigurines = mutableMapOf<Char, Bitmap>()
    }
}