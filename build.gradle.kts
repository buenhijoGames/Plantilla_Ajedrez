// ============================================================================
// build.gradle.kts raíz
// ----------------------------------------------------------------------------
// Declaración de plugins comunes para los módulos del proyecto. Aquí se
// declaran sin aplicar (apply = false); cada módulo los aplica según sus
// necesidades. Esto mantiene una única fuente de verdad para las versiones.
// ============================================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}