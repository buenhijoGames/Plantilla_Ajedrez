package com.buenhijogames.plantilla_ajedrez.domain.repositorio

/**
 * Genera identificadores únicos estables para entidades de dominio.
 *
 * Se abstrae como puerto para poder reemplazar la implementación en tests
 * (por ejemplo, para forzar ids predecibles) sin tocar los repositorios.
 */
interface GeneradorIds {
    /** Devuelve un nuevo identificador único en formato String. */
    fun nuevoId(): String
}