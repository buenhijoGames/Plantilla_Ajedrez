package com.buenhijogames.plantilla_ajedrez.domain.pdf

import com.buenhijogames.plantilla_ajedrez.domain.modelo.Partida

/**
 * Puerto de generación de plantilla PDF estilo FIDE.
 *
 * Implementado en `:data` con `android.graphics.pdf.PdfDocument`. La plantilla
 * contiene el Seven Tag Roster en la cabecera y una tabla de dos columnas
 * (Blancas / Negras) con casillas para ~60 jugadas, igual que las plantillas
 * físicas oficiales FIDE. Las jugadas se dibujan con figurín (silueta cburnett)
 * reutilizando la misma segmentación SAN que la planilla en pantalla.
 *
 * Se puede generar el PDF de una única partida o de varias (una página por
 * partida), por ejemplo para exportar un torneo completo de una sola vez.
 */
interface PuertoPdf {

    /** Genera el PDF de una sola [partida] (una página). */
    fun generarPlantilla(partida: Partida): ByteArray

    /**
     * Genera un PDF multipágina, una página por cada [partidas].
     *
     * Se usa para exportar un torneo (o una selección de partidas) de una sola
     * vez: cada partida ocupa su propia hoja FIDE.
     */
    fun generarPlantillas(partidas: List<Partida>): ByteArray
}