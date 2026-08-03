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

### ✅ Fase 1 — HECHA (rama `fase-1-repositorios-casos-uso`, commit `c6ce478`)

`c6ce478` "Fase 1: repositorios, mappers, infraestructura y tests unitarios":

- **Mappers** en `data/bd/mapeadores/Mapeadores.kt`:
  `TorneoEntity.aDominio()`, `Torneo.aEntity(creadoEn)`,
  `PartidaEntity.aDominio()`, `Partida.aEntity(actualizadoEn)`.
- **Puertos dominio** nuevos: `GeneradorIds` (UUID), `Reloj` (epoch millis).
- **Repositorios impl** en `data/repositorio/`:
  `RepositorioTorneosImpl`, `RepositorioPartidasImpl`,
  `GeneradorIdsUuid`, `RelojSistema`.
- **Módulo Hilt** `data/di/ModuloRepositorios.kt` con `@Binds` para los 4 bindings.
- **Tests**: `MapeadoresTest`, `RepositorioTorneosImplTest`,
  `RepositorioPartidasImplTest`, `InfraestructuraRepositoriosTest` en
  `data/src/test/kotlin/`.

### ✅ Fase 2 — HECHA (rama `fase-2-chesslib-adapter`, commit `572ea87`)

`572ea87` "Fase 2: AdaptadorChesslib y AdaptadorPgn con chesslib + tests unitarios". Detalle en el mensaje del commit.

- **`data/ajedrez/AdaptadorChesslib.kt`** → implementa `PuertoMotorAjedrez`
  con chesslib. Sin estado (Board efímero por operación). `fenInicial`,
  `aplicarJugada(fen, san)`, `jugadaASan(fen, desde, hasta, promocion)`,
  `jugadasLegalesDesde`, `esFinal` (mate/ahogado), `esTablas` (50 jugadas,
  repetición, material insuficiente). `JugadaIlegalException` propia para
  validación de usuario; FEN inválido → `IllegalStateException` con contexto.
- **`data/pgn/AdaptadorPgn.kt`** → implementa `PuertoPgn` con chesslib.
  Exportación: Seven Tag Roster + WhiteElo/BlackElo/Time/Mode + SetUp/FEN,
  movetext conservado tal cual (variantes/comentarios/NAGs), resultado
  añadido al final si falta. Importación: `PgnIterator` + `LargeFile` sobre
  `ByteArrayInputStream`, acepta PGN multi-partida.
- **`data/di/ModuloServicios.kt`** → `@Binds` de `PuertoMotorAjedrez` y
  `PuertoPgn` (separado de `ModuloRepositorios` por responsabilidad única).
- **Fix doc** en `domain/pgn/PuertoPgn.kt` (KDoc corregido, sin cambio de API).
- **Tests**: `AdaptadorChesslibTest` (13 tests: FEN inicial, e4, ilegal,
  SAN e4/Nf3, promoción, legales desde casilla, mate del loco, material
  insuficiente...) y `AdaptadorPgnTest` (7 tests: Tag Roster, SetUp/FEN,
  interrogantes, vacío, Fischer-Spassky, round-trip, variantes/comentarios).

**Pendiente**:
- Compilar desde Android Studio y ejecutar tests de `:data` (checkpoint Manolo).
- Commit detallado en español (tras OK de Manolo).

### ⏳ Fases pendientes

| Fase | Descripción |
|---|---|
| 3 | Tema M3 (varios temas seleccionables) + Nav + StartupDialog + Torneos |
| 3b | Settings (tema/piezas/licencias) + LicensesScreen |
| 4 | BoardComposable Canvas + piezas cburnett + entrada táctil |
| 5 | ScoresheetPanel (variantes/comentarios/NAGs/figurín) + autosave |
| 6 | StockfishAdapter UCI → `PuertoEvaluacionMotor` + AnalysisSheet (**sólo post-partida**, anti-fraude). Ojo: los `.so` ya están en `app/src/main/jniLibs/` pero NO hay adaptador UCI/JNI escrito. |
| 7 | Import/Export PGN + PDF plantilla FIDE + overflow menu (3 puntos) |
| 8 | Tests + lint + typecheck |
| 9 | Pulido estético (animaciones, microinteracciones) |
| 10 | Preparación Play Console (splits, sign, versioning) |

**Nota sobre casos de uso**: `:domain` es pura (sin javax.inject). Cuando se
implementen, serán clases Kotlin sin anotaciones DI, provistas con `@Provides`
desde un módulo Hilt en `:data` (o `:app`).

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
- `fase-1-repositorios-casos-uso` (Fase 1 completa, commit `c6ce478`)
- `fase-2-chesslib-adapter` (Fase 2 en curso, HEAD actual)

### ⚠️ Remoto
- El remoto `origin` (`Salmeron52/plantillas_ajedrez`) está **vacío**: nunca
  se ha hecho push. Todo el trabajo existe solo en local. Pendiente
  `git push -u origin` de las tres ramas cuando Manolo lo autorice.

---

## ➡️ Continuación exacta al retomar

1. Estar en `fase-2-chesslib-adapter`: `git checkout fase-2-chesslib-adapter`.
2. Manolo compila desde Android Studio y ejecuta los tests de `:data`
   (`AdaptadorChesslibTest`, `AdaptadorPgnTest`, más los de Fase 1).
3. Si todo va verde → commit detallado en español de Fase 2
   (adaptadores chesslib + ModuloServicios + tests + fix doc PuertoPgn).
4. Tras el OK de Manolo → Fase 3 (Tema M3 + Nav + StartupDialog + Torneos).
   NOTA: los casos de uso de `:domain` siguen sin implementarse; decidir con
   Manolo si se crean junto a los ViewModels de Fase 3 o en fase aparte.

---

## 📝 Notas de la última sesión

- Sesión anterior: Fase 1 commiteada (`c6ce478`) con tests incluidos.
- Se creó la rama `fase-2-chesslib-adapter` y se escribió Fase 2 completa
  (sin commit): `AdaptadorChesslib`, `AdaptadorPgn`, `ModuloServicios`,
  tests de ambos adaptadores y fix de KDoc en `PuertoPgn`.
- Confusión inicial en esta sesión: el asistente no localizó los `.md` de
  planificación y creyó que el plan se había perdido. **No se perdió**:
  está en `app/md/INSTRUCCIONES.md` (este archivo) + `AGENTS.md` +
  `Esta_App.md` en raíz. Se eliminó un `docs/ROADMAP.md` que el asistente
  creó por error en ubicación incorrecta (regla 5: los `.md` van en `app\md`).
- Detectado: remoto GitHub vacío, nunca se ha hecho push (ver sección Remoto).
- Build limpio verificado verde (Fase 0). Fase 2 pendiente de compilar.