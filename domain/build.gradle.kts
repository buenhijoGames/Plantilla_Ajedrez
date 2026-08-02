import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ============================================================================
// build.gradle.kts del módulo :domain
// ----------------------------------------------------------------------------
// Capa de dominio pura: Núcleo de Clean Architecture.
//
// No depende de Android, ni de Hilt, ni de Room. Contiene entidades, puertos
// (interfaces de repositorios y servicios) y casos de uso. Esta dependencia
// cero con frameworks cumple el Principio de Inversión de Dependencias (DIP):
// presentation y data dependen de este módulo, no al revés.
// ============================================================================

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        // Alineamos el destino del bytecode Kotlin con el de la tarea
        // compileJava (Java 11). Esto evita "Inconsistent JVM-target".
        jvmTarget.set(JvmTarget.JVM_11)
        // Mantiene los nombres reales de parámetros en bytecode, útil para
        // Hilt/R8 que consumen APIs de dominio en :data y :app.
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    // Sin dependencias de framework. Sólo corutinas para los casos de uso
    // que retornan Flow (suspend fun). Mantenemos esto en la capa de dominio
    // porque la concurrencia es parte del contrato, no de la infraestructura.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}