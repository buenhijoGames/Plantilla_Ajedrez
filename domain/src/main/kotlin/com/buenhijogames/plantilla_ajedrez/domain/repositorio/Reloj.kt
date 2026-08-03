package com.buenhijogames.plantilla_ajedrez.domain.repositorio

/**
 * Abstrae la obtención de la marca temporal actual.
 *
 * En producción devuelve `System.currentTimeMillis()`. En tests se inyecta
 * una implementación fija, lo que permite afirmar qué marca temporal se
 * asigna a una entidad sin depender del reloj real.
 */
interface Reloj {
    /** Marca temporal actual en milisegundos desde epoch. */
    fun ahora(): Long
}