package com.buenhijogames.plantilla_ajedrez.domain.pdf

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida

/**
 * Puerto de generación de plantilla PDF estilo FIDE.
 *
 * Implementado en `:data` con `android.graphics.pdf.PdfDocument`. La plantilla
 * contiene el Seven Tag Roster en la cabecera y una tabla de dos columnas
 * (Blancas / Negras) con casillas para ~60 jugadas, igual que las plantillas
 * físicas oficiales FIDE.
 */
interface PuertoPdf {

    /** Genera el PDF de la partida en bytes. */
    fun generarPlantilla(partida: Partida): ByteArray
}