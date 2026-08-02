// ============================================================================
// build.gradle.kts del módulo :data
// ----------------------------------------------------------------------------
// Capa de datos e infraestructura. Implementa los puertos definidos en
// :domain con tecnología concreta: Room (persistencia), chesslib (lógica de
// ajedrez), Stockfish (motor UCI) y PdfDocument (plantilla FIDE).
//
// Aplica Hilt para inyectar bindings (interfaces -> implementaciones) y
// KSP para generar código de Hilt y Room.
// ============================================================================

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.buenhijogames.plantilla_ajedrez.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            // En :data también minificamos con R8 cuando el proyecto lo activa
            // desde :app:consumerProguardFiles transmite estas reglas.
            isMinifyEnabled = false  // La minificación se centraliza en :app.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // --- Clean Architecture: :data depende de :domain (no al revés). ----
    implementation(project(":domain"))

    // --- AndroidX ---
    implementation(libs.androidx.core.ktx)

    // --- Hilt ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // --- Room ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // --- chesslib: validación de jugadas, SAN, FEN, PGN (Apache 2.0). ---
    implementation(libs.chesslib)

    // --- Tests ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.room.testing)
}