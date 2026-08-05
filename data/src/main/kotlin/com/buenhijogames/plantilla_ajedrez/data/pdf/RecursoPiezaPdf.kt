package com.buenhijogames.plantilla_ajedrez.data.pdf

import androidx.annotation.DrawableRes
import com.buenhijogames.plantilla_ajedrez.data.R

/**
 * Recursos drawable de las piezas de ajedrez (cburnett, siluetas blancas y
 * negras) usados por el [AdaptadorPdf] para dibujar el figurin.
 *
 * Las 12 siluetas se copiaron a `:data/res/drawable` (mismos XML que la
 * planilla en pantalla de `:app`) para que la generacion del PDF sea 100%
 * offline y el figurin sea identico al de la app.
 *
 * @param pieza Caracter FEN: mayuscula = blanca, minuscula = negra.
 *              Solo se esperan los 12 caracteres de pieza ('P','N','B','R','Q','K'
 *              y sus minusculas); cualquier otro devuelve el peon blanco como
 *              recurso de seguridad.
 * @return id de recurso [DrawableRes] del VectorDrawable de la pieza.
 */
@DrawableRes
fun recursoPiezaPdf(pieza: Char): Int = when (pieza) {
    'K' -> R.drawable.pieza_blanca_rey
    'Q' -> R.drawable.pieza_blanca_dama
    'R' -> R.drawable.pieza_blanca_torre
    'B' -> R.drawable.pieza_blanca_alfil
    'N' -> R.drawable.pieza_blanca_caballo
    'P' -> R.drawable.pieza_blanca_peon
    'k' -> R.drawable.pieza_negra_rey
    'q' -> R.drawable.pieza_negra_dama
    'r' -> R.drawable.pieza_negra_torre
    'b' -> R.drawable.pieza_negra_alfil
    'n' -> R.drawable.pieza_negra_caballo
    'p' -> R.drawable.pieza_negra_peon
    else -> R.drawable.pieza_blanca_peon
}