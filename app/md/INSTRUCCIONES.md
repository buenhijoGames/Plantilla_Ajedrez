# 📋 INSTRUCCIONES.md — Plantilla_ajedrez

> Archivo de seguimiento obligatorio (Regla de Oro 0 de AGENTS.md).
> Objetivo: poder retomar el proyecto en cualquier sesión sabiendo exactamente
> qué se ha hecho, qué falta, cómo está configurado y qué reglas se respetan.

---

## 🎯 Objetivo de la app

Plantilla de ajedrez electrónica para que Manolo y otros usuarios anoten sus
partidas mientras las juegan, las guarden en formato PGN interoperable, las
compartan (PGN + plantilla PDF estilo FIDE con figurín) y las analicen con
Stockfish **sólo tras haber finalizado** (anti-fraude).

App **gratis, sin publicidad**, sin ánimo de lucro, open-source **GPL-3.0**.
Repo público: https://github.com/Salmeron52/plantillas_ajedrez

---

## 📜 Directrices obligatorias (resumen)

- **Código 100% Español Técnico** (clases, funciones, variables, paquetes).
- **KDoc** en cada miembro, detalla propósito, @param, @return.
- **Stack**: Kotlin + KSP (no Kapt) + Hilt + Jetpack Compose + Material 3 +
  Canvas + Room (migraciones manuales, **prohibido borrado destructivo**) +
  Version Catalogs + Clean Architecture modularizada + SOLID.
- **R8/ProGuard** con `@Keep` en data classes serializables.
- **`.gitignore`** estricto (keystores, `local.properties`, `google-services.json`).
- **Git**: commits extensos en español tras cada aprobación de Manolo; nueva
  rama para funcionalidades complejas; **prohibido tocar main/master**;
  **prohibido borrar ramas**.
- **Texto**: nada hardcodeado; todo vía `strings.xml`.
- **Punto de control**: tras cada unidad lógica pedir a Manolo
  *"MANOLO, POR FAVOR COMPILE DESDE ANDROID STUDIO Y VERIFIQUE ESTABILIDAD"*.
- **Documentos `.md`** propios del proyecto se guardan en `app\md`.
- **Diseño responsivo** (orientación y pantallas) y **varios temas seleccionables**.
- **Belleza extraordinaria**, fluida y estable.
- **Optimización APK**: recursos pre-build en raíz; Stockfish se descarga
  on-demand a `app/src/main/jniLibs` via tarea Gradle `descargarStockfish`.
- **UI minimalista**: mejor icono de 3 puntos (overflow) que botones sueltos.
- **Autosave**: torneos/partidas/jugadas se guardan automáticamente.
- **Anti-fraude**: el botón Motor sólo se habilita si la partida está finalizada.

---

## 🏗️ Arquitectura

Multimódulo Gradle (Clean Architecture + DIP):

```
:app    → Presentación (Compose + ViewModels + Hilt + Nav + jniLibs/Stockfish)
:domain → Entidades, puertos (interfaces), casos de uso (Kotlin puro, sin Android)
:data   → Room, ChesslibAdapter, StockfishAdapter (UCI), PgnAdapter,
          PdfAdapter (plantilla FIDE), LicensesProvider, Módulos Hilt
```

- `:domain` NO depende de Android ni Hilt ni Room.
- `:data` implementa los puertos de `:domain`.
- `:app` depende de `:domain` y `:data`.

### Stack objetivo (versiones en `gradle/libs.versions.toml`)

- AGP 9.2.1, Kotlin 2.2.10, Gradle 9.4.1, compileSdk/targetSdk 37, minSdk 27
- Hilt 2.60.1 (compatible AGP 9), KSP 2.2.10-2.0.2
- Room 2.8.4, Navigation Compose 2.9.8
- chesslib 1.3.7 (JitPack, Apache 2.0) para validación/SAN/FEN/PGN
- Hilt Navigation Compose 1.4.0
- Coroutines Test 1.11.0, Turbine 1.2.1, MockK 1.14.11

### Licencias

- App: **GPL-3.0** (ver `LICENSE` en raíz del repo).
- Stockfish: GPLv3. Piezas cburnett (Lichess): GPLv2+.
- chesslib, AndroidX, Hilt, Room: Apache 2.0.
- Atribuciones en `NOTICE` (raíz). Visibles en app en Settings → Licencias.

---

## 📦 Estado del proyecto (sesión a sesión)

### ✅ Fase 0 — HECHA (rama `fase-0-estructura-inicial`, 2 commits)

- `e483fd1`: estructura multimódulo + Hilt/Room/Navigation base.
  - Proguard-rules.pro con keeps Hilt/Room/chesslib/JNI.
  - splits.abi (arm64-v8a + armeabi-v7a) para Play Console.
  - compileSdk/targetSdk 37, minSdk 27.
  - `PlantillaApplication @HiltAndroidApp`, `MainActivity @AndroidEntryPoint`.
  - `LICENSE` (GPL-3.0), `NOTICE` con atribuciones.
  - Dominio: `Torneo`, `Partida`, `ResultadoPartida`; puertos
    `RepositorioTorneos`, `RepositorioPartidas`, `PuertoMotorAjedrez`,
    `PuertoEvaluacionMotor`, `PuertoPgn`, `PuertoPdf`, `PuertoLicencias`.
  - `.gitignore` endurecido (keystores, secrets, jniLibs grandes).

- `2c4f752`: Stockfish + Room con migraciones estables.
  - Tarea Gradle `descargarStockfish` (config-cache compatible) que descarga
    Stockfish 18 de releases oficiales y los coloca como `libstockfish.so`
    en `app/src/main/jniLibs/<abi>/`.
  - Room: entidades `TorneoEntity`, `PartidaEntity` (FK CASCADE, índices),
    DAOs, `BaseDeDatosPlantilla` v1 con `exportSchema=true`.
  - `MigracionesPlantilla`: catálogo centralizado de migraciones explícitas.
  - `ModuloBaseDatos` (Hilt) **sin** `fallbackToDestructiveMigration` → si
    falta una migración, Room lanza excepción en vez de borrar datos.
  - Esquema `data/schemas/.../1.json` exportado para tests de migración.
  - APKs generadas: `app-arm64-v8a-debug.apk` y `app-armeabi-v7a-debug.apk`
    (~120 MB cada una, incluye Stockfish).

### 🚧 Fase 1 — EN CURSO (rama `fase-1-repositorios-casos-uso`)

Sin commit todavía. Cambios ya escritos en disco:

- **Mappers** en `data/bd/mapeadores/Mapeadores.kt`:
  `TorneoEntity.aDominio()`, `Torneo.aEntity(creadoEn)`,
  `PartidaEntity.aDominio()`, `Partida.aEntity(actualizadoEn)`.
- **Puertos dominio** nuevos: `GeneradorIds` (UUID), `Reloj` (epoch millis).
- **Repositorios impl** en `data/repositorio/`:
  `RepositorioTorneosImpl`, `RepositorioPartidasImpl`,
  `GeneradorIdsUuid`, `RelojSistema`.
- **Módulo Hilt** `data/di/ModuloRepositorios.kt` con `@Binds` para los 4 bindings.

**Pendiente**:
- Tests de mappers y repositorios (Regla 4.3 Testing).
- Commit detallado en español.
- Checkpoint Manolo.

### ⏳ Fases pendientes

| Fase | Descripción |
|---|---|
| 2 | ChesslibAdapter (motor/SAN/FEN/PGN con chesslib 1.3.7) |
| 3 | Tema M3 (varios temas seleccionables) + Nav + StartupDialog + Torneos |
| 3b | Settings (tema/piezas/licencias) + LicensesScreen |
| 4 | BoardComposable Canvas + piezas cburnett + entrada táctil |
| 5 | ScoresheetPanel (variantes/comentarios/NAGs/figurín) + autosave |
| 6 | StockfishAdapter UCI + AnalysisSheet (**sólo post-partida**, anti-fraude) |
| 7 | Import/Export PGN + PDF plantilla FIDE + overflow menu (3 puntos) |
| 8 | Tests + lint + typecheck |
| 9 | Pulido estético (animaciones, microinteracciones) |
| 10 | Preparación Play Console (splits, sign, versioning) |

---

## ⚙️ Comandos útiles

```powers
# Compilar APK debug
.\gradlew.bat :app:assembleDebug

# Compilar release (con R8/ProGuard) — requiere signing config
.\gradlew.bat :app:assembleRelease

# Descargar/verificar binarios Stockfish en jniLibs
.\gradlew.bat descargarStockfish

# Limpiar
.\gradlew.bat clean

# Listar tareas del grupo "ajedrez"
.\gradlew.bat :app:tasks --group ajedrez

# Compilar sólo :data (KSP Room)
.\gradlew.bat :data:kspDebugKotlin
```

**Esquema Room**: `data/schemas/com.buenhijogames.plantilla_ajedrez.data.bd.BaseDeDatosPlantilla/1.json`

---

## 🔀 Política de ramas

- **Main/master**: intocable (no escribir, no borrar).
- Cada fase → su propia rama `fase-N-...`.
- Commits extensos en español tras cada aprobación de Manolo.
- Nunca borrar ramas.

### Ramas existentes
- `fase-0-estructura-inicial` (Fase 0 completa)
- `fase-1-repositorios-casos-uso` (Fase 1 en curso, HEAD actual)

---

## ➡️ Continuación exacta al retomar

1. Estar en `fase-1-repositorios-casos-uso`: `git checkout fase-1-repositorios-casos-uso`.
2. Build debe pasar: `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
3. Faltan: tests de `Mapeadores.kt` y de `RepositorioTorneosImpl`/`RepositorioPartidasImpl`
   - Usar MockK para el DAO, Turbine para los Flow.
   - Carpeta: `data/src/test/kotlin/...`.
4. Tras tests OK → commit detallado en español con todo lo hecho.
5. **Checkpoint**: pedir a Manolo compilar desde Android Studio y verificar
   estabilidad. No avanzar a Fase 2 sin su OK.

---

## 📝 Notas de la última sesión

- Se leyeron `AGENTS.md` y `Esta_App.md` por completo; se reconciliaron
  conflictos:
  - Stockfish anti-fraude: sólo post-partida.
  - Stockfish versión 18 (no 8).
  - Varios temas seleccionables (no sólo dynamic).
  - Overflow menu en lugar de botones sueltos.
  - Autosave tras cada jugada.
  - `INSTRUCCIONES.md` creado en `app\md` (este archivo).
  - Checkpoint Manolo después de cada fase.
- Se eliminó un fichero espurio `nul` (nombre reservado Windows) del working tree.
- Build limpio verificado verde.