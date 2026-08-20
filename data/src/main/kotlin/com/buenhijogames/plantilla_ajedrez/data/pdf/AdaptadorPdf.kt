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
 * Dibuja la plantilla FIDE oficial en formato A4 vertical con una cabecera
 * compacta enmarcada (Evento, Ronda, Sitio, Fecha, Blancas, Negras, Resultado)
 * y una estructura de 4 columnas (2 bloques de 30 jugadas = 60 jugadas por cara),
 * incluyendo figurines tipográficos y zona de firmas al pie.
 *
 * Las jugadas se dibujan con **figurin** (silueta cburnett): cada SAN se
 * descompone con [PlanillaFide.segmentar] y los segmentos de pieza se
 * rasterizan desde los VectorDrawables copiados a `:data/res/drawable`.
 * Asi el PDF es 100% offline y el figurin coincide con el de la app.
 *
 * Para varias partidas ([generarPlantillas]) se crea una pagina por partida.
 *
 * @param contexto Contexto de la aplicacion (para inflar los drawables y strings).
 */
@Singleton
class AdaptadorPdf @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
) : PuertoPdf {

    // --- Constantes de la hoja A4 (en puntos; 1 punto = 1/72 pulgada). ---
    private val ANCHO_HOJA = 595f
    private val ALTO_HOJA = 842f
    private val MARGEN_LATERAL = 28f
    private val MARGEN_SUPERIOR = 28f
    private val MARGEN_INFERIOR = 28f

    // --- Pinturas reutilizadas (se configuran una vez). ---
    private val pinturaTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val pinturaTextoNegrita = Paint(pinturaTexto).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pinturaCabeceraTabla = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pinturaNumeroJugada = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 8.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pinturaResultado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pinturaFondoCabeceraTabla = Paint().apply {
        color = Color.rgb(238, 238, 238)
        style = Paint.Style.FILL
    }
    private val pinturaLinea = Paint().apply {
        color = Color.rgb(180, 180, 180)
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    private val pinturaMarco = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1.0f
        style = Paint.Style.STROKE
    }

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
        val altoCabecera = 82f
        val altoPie = 32f
        val yCabeceraTop = MARGEN_SUPERIOR
        val yCabeceraBottom = yCabeceraTop + altoCabecera

        val yPieBottom = ALTO_HOJA - MARGEN_INFERIOR
        val yPieTop = yPieBottom - altoPie

        val yTablaTop = yCabeceraBottom + 10f
        val yTablaBottom = yPieTop - 10f

        dibujarCabecera(canvas, yCabeceraTop, yCabeceraBottom, plantilla.cabecera)
        dibujarTabla4Columnas(canvas, yTablaTop, yTablaBottom, plantilla.filas)
        dibujarPieFirmas(canvas, yPieTop, yPieBottom)
    }

    /**
     * Dibuja la cabecera enmarcada con los datos del torneo, jugadores y resultado.
     *
     * @param canvas Canvas de la pagina.
     * @param top Posicion Y superior del marco.
     * @param bottom Posicion Y inferior del marco.
     * @param cabecera Datos de la cabecera.
     */
    private fun dibujarCabecera(canvas: Canvas, top: Float, bottom: Float, cabecera: PlanillaFide.Cabecera) {
        val izquierda = MARGEN_LATERAL
        val derecha = ANCHO_HOJA - MARGEN_LATERAL
        val anchoTotal = derecha - izquierda

        // Marco exterior
        canvas.drawRect(izquierda, top, derecha, bottom, pinturaMarco)

        val altoFila = (bottom - top) / 4f
        val divisionX1 = izquierda + anchoTotal * 0.70f // Para Evento/Ronda y Sitio/Fecha
        val divisionX2 = izquierda + anchoTotal * 0.68f // Para Blancas/Firma y Negras/Resultado

        // Lineas horizontales divisorias
        for (i in 1..3) {
            val y = top + i * altoFila
            canvas.drawLine(izquierda, y, derecha, y, pinturaLinea)
        }

        // Lineas verticales divisorias
        canvas.drawLine(divisionX1, top, divisionX1, top + 2 * altoFila, pinturaLinea)
        canvas.drawLine(divisionX2, top + 2 * altoFila, divisionX2, bottom, pinturaLinea)

        // Fila 1: Evento | Ronda
        val yFila1 = top + altoFila * 0.70f
        dibujarCampoCabecera(canvas, izquierda + 6f, yFila1, contexto.getString(R.string.pdf_evento), cabecera.evento, divisionX1 - izquierda - 12f)
        dibujarCampoCabecera(canvas, divisionX1 + 6f, yFila1, contexto.getString(R.string.pdf_ronda), cabecera.ronda, derecha - divisionX1 - 12f)

        // Fila 2: Sitio | Fecha
        val yFila2 = top + altoFila * 1.70f
        dibujarCampoCabecera(canvas, izquierda + 6f, yFila2, contexto.getString(R.string.pdf_sitio), cabecera.sitio, divisionX1 - izquierda - 12f)
        dibujarCampoCabecera(canvas, divisionX1 + 6f, yFila2, contexto.getString(R.string.pdf_fecha), cabecera.fecha, derecha - divisionX1 - 12f)

        // Fila 3: Blancas
        val yFila3 = top + altoFila * 2.70f
        val nombreBlancas = nombreConElo(cabecera.blancas, cabecera.eloBlancas)
        dibujarCampoCabecera(canvas, izquierda + 6f, yFila3, contexto.getString(R.string.pdf_blancas), nombreBlancas, divisionX2 - izquierda - 12f)

        // Fila 4: Negras | Resultado
        val yFila4 = top + altoFila * 3.70f
        val nombreNegras = nombreConElo(cabecera.negras, cabecera.eloNegras)
        dibujarCampoCabecera(canvas, izquierda + 6f, yFila4, contexto.getString(R.string.pdf_negras), nombreNegras, divisionX2 - izquierda - 12f)

        // Resultado destacado a la derecha en la fila 4
        val etiquetaRes = contexto.getString(R.string.pdf_resultado) + ":"
        canvas.drawText(etiquetaRes, divisionX2 + 6f, yFila4, pinturaTextoNegrita)
        val anchoEtiquetaRes = pinturaTextoNegrita.measureText(etiquetaRes)
        val textoResultado = cabecera.resultado.pgn
        canvas.drawText(textoResultado, divisionX2 + 6f + anchoEtiquetaRes + 8f, yFila4, pinturaResultado)
    }

    /**
     * Dibuja un campo de la cabecera ("Etiqueta: Valor") asegurando que no sobrepase el ancho.
     */
    private fun dibujarCampoCabecera(
        canvas: Canvas,
        x: Float,
        y: Float,
        etiqueta: String,
        valor: String,
        anchoDisponible: Float,
    ) {
        val textoEtiqueta = "$etiqueta:"
        val anchoEtiqueta = pinturaTextoNegrita.measureText(textoEtiqueta)
        canvas.drawText(textoEtiqueta, x, y, pinturaTextoNegrita)

        val disponibleValor = anchoDisponible - anchoEtiqueta - 4f
        if (disponibleValor > 0 && valor.isNotBlank()) {
            val valorRecortado = recortarTexto(valor, disponibleValor, pinturaTexto)
            canvas.drawText(valorRecortado, x + anchoEtiqueta + 4f, y, pinturaTexto)
        }
    }

    /**
     * Dibuja la tabla de 4 columnas (2 bloques de 30 jugadas cada uno).
     *
     * @param canvas Canvas de la pagina.
     * @param top Posicion Y superior de la tabla.
     * @param bottom Posicion Y inferior de la tabla.
     * @param filas Lista de jugadas disponibles.
     */
    private fun dibujarTabla4Columnas(
        canvas: Canvas,
        top: Float,
        bottom: Float,
        filas: List<PlanillaFide.Fila>,
    ) {
        val izquierda = MARGEN_LATERAL
        val derecha = ANCHO_HOJA - MARGEN_LATERAL
        val anchoTotal = derecha - izquierda
        val separacionBloques = 12f
        val anchoBloque = (anchoTotal - separacionBloques) / 2f

        val xBloque1 = izquierda
        val xBloque2 = izquierda + anchoBloque + separacionBloques

        // Dibujar Bloque 1 (Jugadas 1 a 30)
        dibujarBloqueJugadas(canvas, xBloque1, top, bottom, anchoBloque, 1, 30, filas)

        // Dibujar Bloque 2 (Jugadas 31 a 60)
        dibujarBloqueJugadas(canvas, xBloque2, top, bottom, anchoBloque, 31, 60, filas)
    }

    /**
     * Dibuja un bloque de anotacion de 30 jugadas (Nº, Blancas, Negras).
     */
    private fun dibujarBloqueJugadas(
        canvas: Canvas,
        x: Float,
        top: Float,
        bottom: Float,
        ancho: Float,
        jugadaInicio: Int,
        jugadaFin: Int,
        filas: List<PlanillaFide.Fila>,
    ) {
        val altoCabecera = 16f
        val numFilas = (jugadaFin - jugadaInicio + 1)
        val altoFila = (bottom - top - altoCabecera) / numFilas

        val anchoNumero = 20f
        val anchoColumna = (ancho - anchoNumero) / 2f

        // Fondo y texto cabecera de la tabla
        canvas.drawRect(x, top, x + ancho, top + altoCabecera, pinturaFondoCabeceraTabla)
        canvas.drawRect(x, top, x + ancho, bottom, pinturaMarco)

        // Linea divisoria cabecera
        canvas.drawLine(x, top + altoCabecera, x + ancho, top + altoCabecera, pinturaMarco)

        // Divisores verticales de cabecera
        canvas.drawLine(x + anchoNumero, top, x + anchoNumero, bottom, pinturaLinea)
        canvas.drawLine(x + anchoNumero + anchoColumna, top, x + anchoNumero + anchoColumna, bottom, pinturaLinea)

        // Textos cabecera
        val yTextoCabecera = top + 11.5f
        val textoNumero = contexto.getString(R.string.pdf_numero)
        val anchoTxtNum = pinturaCabeceraTabla.measureText(textoNumero)
        canvas.drawText(textoNumero, x + (anchoNumero - anchoTxtNum) / 2f, yTextoCabecera, pinturaCabeceraTabla)

        val textoBlancas = contexto.getString(R.string.pdf_blancas).uppercase()
        val anchoTxtBla = pinturaCabeceraTabla.measureText(textoBlancas)
        canvas.drawText(textoBlancas, x + anchoNumero + (anchoColumna - anchoTxtBla) / 2f, yTextoCabecera, pinturaCabeceraTabla)

        val textoNegras = contexto.getString(R.string.pdf_negras).uppercase()
        val anchoTxtNeg = pinturaCabeceraTabla.measureText(textoNegras)
        canvas.drawText(textoNegras, x + anchoNumero + anchoColumna + (anchoColumna - anchoTxtNeg) / 2f, yTextoCabecera, pinturaCabeceraTabla)

        // Dibujar filas de jugadas
        for (i in 0 until numFilas) {
            val numJugada = jugadaInicio + i
            val yFilaTop = top + altoCabecera + i * altoFila
            val yFilaBottom = yFilaTop + altoFila
            val yCentro = yFilaTop + altoFila / 2f

            // Linea horizontal de cada fila
            if (i < numFilas - 1) {
                canvas.drawLine(x, yFilaBottom, x + ancho, yFilaBottom, pinturaLinea)
            }

            // Numero de jugada centrado
            val strNum = numJugada.toString()
            val anchoN = pinturaNumeroJugada.measureText(strNum)
            canvas.drawText(strNum, x + (anchoNumero - anchoN) / 2f, yCentro + 3f, pinturaNumeroJugada)

            // Jugadas de la fila (si existen)
            val fila = filas.firstOrNull { it.numero == numJugada }
            if (fila != null) {
                dibujarJugadaEnCelda(canvas, x + anchoNumero, yCentro, anchoColumna, fila.blancas)
                dibujarJugadaEnCelda(canvas, x + anchoNumero + anchoColumna, yCentro, anchoColumna, fila.negras)
            }
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
        var x = xInicio + 5f
        val altoIcono = 11f
        for (segmento in segmentos) {
            when (segmento) {
                is SegmentoFigurin.Texto -> {
                    val texto = segmento.texto
                    val ancho = pinturaTexto.measureText(texto)
                    if (x + ancho > xInicio + anchoCelda - 2f) break
                    canvas.drawText(texto, x, yCentro + 3.2f, pinturaTexto)
                    x += ancho
                }

                is SegmentoFigurin.Pieza -> {
                    val icono = obtenerFigurin(segmento.simboloFen) ?: continue
                    val izquierda = x
                    val derecha = izquierda + altoIcono
                    if (derecha > xInicio + anchoCelda - 2f) break
                    val rect = RectF(izquierda, yCentro - altoIcono / 2f, derecha, yCentro + altoIcono / 2f)
                    canvas.drawBitmap(icono, null, rect, null)
                    x = derecha + 1f
                }
            }
        }
    }

    /**
     * Dibuja el pie de pagina con lineas para las firmas oficiales.
     */
    private fun dibujarPieFirmas(canvas: Canvas, top: Float, bottom: Float) {
        val izquierda = MARGEN_LATERAL
        val derecha = ANCHO_HOJA - MARGEN_LATERAL
        val anchoTotal = derecha - izquierda
        val anchoSeccion = anchoTotal / 3f

        val yLinea = top + 14f
        val yTexto = yLinea + 11f

        // Firma Blancas
        canvas.drawLine(izquierda + 5f, yLinea, izquierda + anchoSeccion - 15f, yLinea, pinturaLinea)
        canvas.drawText(contexto.getString(R.string.pdf_firma_blancas), izquierda + 5f, yTexto, pinturaTexto)

        // Firma Negras
        val xNegras = izquierda + anchoSeccion
        canvas.drawLine(xNegras + 5f, yLinea, xNegras + anchoSeccion - 15f, yLinea, pinturaLinea)
        canvas.drawText(contexto.getString(R.string.pdf_firma_negras), xNegras + 5f, yTexto, pinturaTexto)

        // Firma Arbitro
        val xArbitro = izquierda + 2 * anchoSeccion
        canvas.drawLine(xArbitro + 5f, yLinea, xArbitro + anchoSeccion - 5f, yLinea, pinturaLinea)
        canvas.drawText(contexto.getString(R.string.pdf_firma_arbitro), xArbitro + 5f, yTexto, pinturaTexto)
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

    companion object {
        /** Cache de figurines rasterizados (por simbolo FEN). */
        private val cacheFigurines = mutableMapOf<Char, Bitmap>()
    }
}