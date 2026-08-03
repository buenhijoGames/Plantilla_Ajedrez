package com.buenhijogames.plantilla_ajedrez.ui.theme

/**
 * Catálogo de temas visuales seleccionables por el usuario.
 *
 * Cada constante se persiste por nombre en DataStore (ver
 * `app/preferencias/PreferenciasUsuario.kt`) y se resuelve contra el enum
 * con [desdeNombre]. El nombre estable (`name`) es el que viaja a disco: si
 * en el futuro se renombra un tema, hay que añadir un alias en
 * [desdeNombre] para no romper la preferencia de usuarios ya existentes
 * (regla de Estabilidad 3 de AGENTS.md aplicada a preferencias).
 *
 * @property nombreMostrado   Etiqueta legible para la UI (resuelta vía
 *                            `strings.xml` por cada opción). Aquí no se
 *                            hardcodea texto: el id del string es
 *                            `R.string.tema_<clave>`.
 * @property esquemaOscuroPorDefecto Indica si, en ausencia de indicación
 *                            del sistema, este tema debe usar su variante
 *                            oscura (Madera y Mármol se consideran temas
 *                            "de marca" y por defecto se muestran en su
 *                            variante clara).
 */
enum class TemaAplicacion(
    val esquemaOscuroPorDefecto: Boolean = false,
) {
    /** Tema claro estándar de Material 3. */
    CLARO,

    /** Tema oscuro estándar de Material 3. */
    OSCURO(esquemaOscuroPorDefecto = true),

    /**
     * Tema dinámico (Material You): los colores los toma del fondo de
     * pantalla del usuario en Android 12+. En versiones previas se
     * degrada a [CLARO] o [OSCURO] según el modo del sistema.
     */
    DINAMICO,

    /** Tema de marca "Madera": tablero cálido, marrones y dorados. */
    MADERA,

    /** Tema de marca "Mármol": paleta fría tipo torneo FIDE. */
    MARMOL,
    ;

    companion object {
        /**
         * Resuelve un nombre persistido a [TemaAplicacion].
         *
         * Si el nombre no corresponde a ningún tema (puede ocurrir tras
         * una actualización que renombre temas, o por corrupción de la
         * preferencia), se devuelve [CLARO] como valor seguro por
         * defecto. Nunca lanza: la UI debe poder arrancar siempre.
         *
         * @param nombre Nombre persistido (valor de [name]). Acepta null.
         * @return Tema resuelto. Nunca null.
         */
        fun desdeNombre(nombre: String?): TemaAplicacion =
            nombre?.let { runCatching { valueOf(it.uppercase()) }.getOrNull() }
                ?: CLARO
    }
}