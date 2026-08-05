package com.buenhijogames.plantilla_ajedrez.domain.licencias

/**
 * Información resumida de una licencia o atribución de componente de terceros.
 *
 * El [PuertoLicencias] expone esta lista a la UI, que la mostrará en la
 * pantalla Ajustes → Licencias (visibilidad obligatoria para cumplir GPL
 * y Apache).
 *
 * @property nombre        Nombre del componente (p.ej. `chesslib`).
 * @property licencia      Nombre corto de la licencia (p.ej. `Apache 2.0`).
 * @property autor         Autor / maintainer.
 * @property url           Enlace al upstream del componente.
 * @property resumen       Frase corta de descripción de qué se usa y porqué.
 * @property textoLicencia Texto completo de la licencia (mostrable al pulsar).
 */
data class InfoLicencia(
    val nombre: String,
    val licencia: String,
    val autor: String,
    val url: String,
    val resumen: String,
    val textoLicencia: String,
)

/**
 * Puerto que abstrae el catálogo de atribuciones de terceros.
 *
 * La implementación vive en `:data` (no pertenece a la lógica de ajedrez
 * propiamente dicha, pero es un dato de infraestructura de cumplimiento
 * legal).
 */
interface PuertoLicencias {
    /** Lista completa de componentes de terceros usados en la app. */
    fun listar(): List<InfoLicencia>

    /** Oferta pública de código fuente (GPLv3 sección 6). */
    fun urlCodigoFuenteApp(): String
}