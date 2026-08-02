// ============================================================================
// build.gradle.kts del módulo :app
// ----------------------------------------------------------------------------
// Punto de entrada a Android. Capa de presentación (Compose + ViewModels +
// Hilt), navegación y empaquetado final. Aquí se centraliza la minificación
// R8/ProGuard y se firman los ABIs target.
// ============================================================================

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.Serializable
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.buenhijogames.plantilla_ajedrez"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.buenhijogames.plantilla_ajedrez"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Minificación y ofuscación R8 activada para release.
            // Las reglas keep están en proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // En debug dejamos r8 apagado para compilar rápido.
            isMinifyEnabled = false
        }
    }

    // APKs por ABI para Play Console (más pequeñas por entrega).
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Clean Architecture: :app depende de :domain y :data. ---
    implementation(project(":domain"))
    implementation(project(":data"))

    // --- AndroidX core y lifecycle. ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- Compose (BOM gestiona versiones). ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- Navegación Compose. ---
    implementation(libs.androidx.navigation.compose)

    // --- Hilt. ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // --- Tests ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ============================================================================
// Tarea: descargar y empaquetar binarios de Stockfish en jniLibs.
// ----------------------------------------------------------------------------
// Los binarios de Stockfish pesan ~109 MB cada uno y GitHub no admite ficheros
// de más de 100 MB en el repo (sin Git LFS), por eso están en .gitignore.
// Esta tarea los descarga bajo demanda desde los releases oficiales de
// Stockfish (GPLv3) y los coloca como libstockfish.so en jniLibs/<abi>/, de
// modo que Android los empaqueta y extrae en el dispositivo en tiempo de
// instalación. En runtime se ejecutan como subproceso UCI.
//
// Uso:   ./gradlew descargarStockfish      (manual, una vez por clon)
//        ./gradlew :app:preBuild           (lo lanza automáticamente si falta)
//
// Se implementa como una tarea propia (DescargarStockfishTask) con servicios
// inyectados para ser compatible con la configuration cache de Gradle: los
// doLast con lambdas que capturan Project no lo son.
// ============================================================================

/**
 * Descriptor de un binario de Stockfish por ABI.
 * Implementa [Serializable] para poder serializarse en la configuration cache.
 */
data class BinarioStockfish(
    val abi: String,
    val nombreBinario: String,
    val url: String,
) : Serializable

/**
 * Tarea que descarga los binarios de Stockfish desde sus releases oficiales
 * y los coloca como `libstockfish.so` en el directorio jniLibs del módulo :app.
 *
 * Usa servicios inyectados ([ArchiveOperations], [AntBuilderFactory],
 * [FileSystemOperations]) en lugar de capturar el Project, lo que la hace
 * compatible con la configuration cache.
 */
abstract class DescargarStockfishTask : DefaultTask() {

    /** Catálogo de binarios a descargar. */
    @get:Input
    abstract val binarios: ListProperty<BinarioStockfish>

    /** Directorio jniLibs destino. Define el output para up-to-date checks. */
    @get:OutputDirectory
    abstract val jniLibs: Property<FileSystemLocation>

    @get:Inject
    abstract val archiveOps: ArchiveOperations

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun descargar() {
        val base = jniLibs.get().asFile
        binarios.get().forEach { bin ->
            val destino = File(base, "${bin.abi}/libstockfish.so")
            if (destino.exists() && destino.length() > 0) {
                logger.lifecycle("[Stockfish] ${bin.abi} ya presente, se omite la descarga.")
                return@forEach
            }
            destino.parentFile.mkdirs()
            logger.lifecycle("[Stockfish] Descargando ${bin.abi} desde ${bin.url} ...")
            val tarCache = File(temporaryDir, "stockfish-${bin.abi}.tar")
            tarCache.parentFile.mkdirs()
            // Descarga con redirección HTTP(s) seguida automáticamente.
            URI(bin.url).toURL().openStream().use { entrada ->
                tarCache.outputStream().use { salida -> entrada.copyTo(salida) }
            }
            logger.lifecycle("[Stockfish] Extrayendo ${bin.nombreBinario} de ${tarCache.name}...")
            // Extraemos el binario concreto del tar y lo copiamos a jniLibs
            // con el nombre libstockfish.so (convención lib*.so de Android).
            val extractDir = File(temporaryDir, "extract-${bin.abi}").apply { mkdirs() }
            fs.copy {
                from(archiveOps.tarTree(tarCache))
                include("**/${bin.nombreBinario}")
                into(extractDir)
            }
            val extraido = extractDir.walkTopDown().firstOrNull { it.name == bin.nombreBinario }
                ?: error("No se encontró ${bin.nombreBinario} dentro del tar descargado.")
            extraido.copyTo(destino, overwrite = true)
            tarCache.delete()
            logger.lifecycle("[Stockfish] ${bin.abi} listo en ${destino.relativeTo(base.parentFile.parentFile)} (${destino.length() / 1024 / 1024} MB).")
        }
    }
}

val binariosStockfish = listOf(
    BinarioStockfish(
        abi = "arm64-v8a",
        nombreBinario = "stockfish-android-armv8",
        url = "https://github.com/official-stockfish/Stockfish/releases/latest/download/stockfish-android-armv8.tar",
    ),
    BinarioStockfish(
        abi = "armeabi-v7a",
        nombreBinario = "stockfish-android-armv7",
        url = "https://github.com/official-stockfish/Stockfish/releases/latest/download/stockfish-android-armv7.tar",
    ),
)

val jniLibsDir = layout.projectDirectory.dir("src/main/jniLibs")

tasks.register<DescargarStockfishTask>("descargarStockfish") {
    group = "ajedrez"
    description = "Descarga los binarios de Stockfish (GPLv3) y los coloca en jniLibs como libstockfish.so."
    binarios.set(binariosStockfish)
    jniLibs.set(jniLibsDir)
    // La tarea hace su propia comprobación idempotente (omite lo que ya exista).
    // Desactivamos el up-to-date de Gradle para que siempre verifique ambos ABIs,
    // incluso si uno solo falta; es barato porque sólo descarga lo que falta.
    outputs.upToDateWhen { false }
}

// Lanza la descarga automáticamente antes de construir si falta algún binario.
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("descargarStockfish")
}