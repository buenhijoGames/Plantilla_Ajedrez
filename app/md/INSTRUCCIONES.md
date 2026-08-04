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

### ✅ Fase 3 — HECHA (temas + navegación + startup + torneos)
**3a — `9d924c9` "Fase 3a: sistema de temas persistente con DataStore"**
(rama `fase-3a-temas-datastore`):
- Dep: `androidx.datastore:datastore-preferences:1.1.7`.
- `preferencias/PreferenciasUsuario.kt`: `@Singleton` con
  `Flow<TemaAplicacion>` y `guardarTema`. Clave disco `tema` (no renombrar).
  `ModuloPreferencias` provee el `DataStore<Preferences>`.
- `ui/theme/TemaAplicacion.kt`: enum CLARO/OSCURO/DINAMICO/MADERA/MARMOL,
  `desdeNombre` resiliente (nunca null).
- `ui/theme/EsquemasMarca.kt`: paletas Madera (cálida) y Mármol (fría),
  variantes claro/oscuro.
- `ui/theme/PlantillaAjedrezTheme.kt`: tema raíz que lee DataStore,
  `forzarTema` para previews. Reemplaza al `Theme.kt` (borrado).
- `MainActivity`: `@Inject preferencias` → tema reactivo.

**3b — `062075b` "Fase 3b: navegacion Compose, StartupDialog y pantalla de Ajustes"**
(rama `fase-3b-navegacion-startup-tema`):
- Deps: `lifecycle-runtime-compose`, `material-icons-extended`.
- `navegacion/Destinos.kt` (INICIO/TORNEOS/AJUSTES) +
  `navegacion/NavegacionPlantilla.kt` (NavHost).
- `ui/inicio/StartupDialog.kt` + `PantallaInicio.kt`: diálogo "¿Nuevo o
  abrir guardado?" + TopAppBar con overflow (3 puntos → Ajustes).
- `ui/ajustes/AjustesViewModel.kt` (`@HiltViewModel`, StateFlow de tema) +
  `PantallaAjustes.kt` (RadioButton por tema, persistencia automática).

**3c — pendiente commit (rama `fase-3c-torneos`)**:
- `ui/torneos/TorneosViewModel.kt`: `@HiltViewModel`, observa
  `RepositorioTorneos.observarTorneos()`, un único `MutableStateFlow`
  como fuente de verdad (lista+cargando+error+diálogo). `catch` sin
  tumbar la app. `crearTorneo`/`eliminarTorneo` en `viewModelScope`.
- `ui/torneos/DialogoNuevoTorneo.kt`: formulario (nombre obligatorio).
- `ui/torneos/PantallaTorneos.kt`: reemplaza el placeholder (4 estados:
  cargando/error/vacío/lista), LazyColumn con tarjetas + FAB(+).

**Pendiente Fase 3c**: commit detallado en español (tras checkpoint Manolo).

### ✅ Fase 4 — EN CURSO (rama `fase-4-tablero`, pendiente de commit)

Tablero de ajedrez interactivo con Canvas + piezas cburnett de Lichess +
entrada táctil + navegación Torneo→Partida. Verificada por Manolo (compila
y funciona). Pendiente de commit extenso en español.

- **12 VectorDrawables** de piezas cburnett en `app/src/main/res/drawable/
  pieza_{blanca|negra}_{rey|dama|torre|alfil|caballo|peon}.xml` (GPLv2+).
  Regenerados con `herramientas/descargar_piezas.py` (script que normaliza
  `<path>`, `fill` negro por defecto, `<circle>`→pathData y stroke caps;
  los XML son idénticos 1:1 a los SVGs de lichess).
- **`domain/motor/PuertoMotorAjedrez.kt`** + **`AdaptadorChesslib.kt`**:
  nuevo método `resultadoActual(fen)` → `ResultadoPartida`
  (mate/ahogado/tablas/en curso) + 4 tests en `AdaptadorChesslibTest`.
- **`ui/tablero/UtilidadesTablero.kt`**: `piezasDesdeFen`, `filaYColumnaDeCasilla`/
  `casillaDeFilaColumna`, `recursoPieza`, `movetextDesdeSans`/`sansDesdeMovetext`,
  `ladoEnTurno`, y **`segmentosDeSan`** (descompone un SAN en segmentos texto/
  pieza para la planilla). Tests en `UtilidadesTableroTest` (11 + 7 = 18).
- **`ui/tablero/TableroAjedrez.kt`**: Canvas 8×8 (colores `#F0D9B5`/`#B58863`),
  selección ámbar, destinos (círculo/ anillo), piezas con `painterResource`,
  toque→casilla vía `detectTapGestures` (clave `fen, tamanoCasilla`),
  coordenadas del borde a 11sp fijo (letras a-h abajo, números 1-8 izquierda).
- **`ui/tablero/PartidaViewModel.kt`**: carga Partida de Room (SavedStateHandle),
  rejuega movetext, selección origen→destino, promoción con diálogo, fin de
  partida bloquea tablero, **autosave** del movetext tras cada jugada.
  **Movimiento directo** bidireccional: si una pieza solo tiene un destino
  legal se mueve sola al tocarla, y si una casilla solo es alcanzable por una
  pieza propia, esa pieza se mueve directa al tocar el destino.
  **Deshacer jugadas**: `deshacerJugada()` rejuega el movetext desde `fenInicio`
  sin la última jugada y persiste.
- **`ui/tablero/PantallaPartida.kt`**: tablero + TopAppBar con botón deshacer
  (↩) + `DialogoPromocion`.
- **`ui/tablero/PlanillaPartida.kt`**: planilla con las jugadas a 18sp y con
  **dibujo de la pieza en lugar de la letra** (estándar: todas las piezas en
  silueta blanca con contorno, sin distinguir bando). Flujo con salto de línea
  y scroll vertical (máx. 140dp).
- **`ui/torneos/DetalleTorneoViewModel.kt`** + **`PantallaDetalleTorneo.kt`**:
  carga torneo por id, lista reactiva de partidas, FAB crea partida nueva
  (ronda correlativa, hereda tags) y navega; pulsar partida navega.
- **`navegacion/Destinos.kt`** + **`NavegacionPlantilla.kt`**: rutas
  `DETALLE_TORNEO`/`PARTIDA` con `{torneoId}`/`{partidaId}` y ARGs.
- **`ui/torneos/PantallaTorneos.kt`**: `FilaTorneo` como `Card(onClick)`.
- **`strings.xml`**: cadenas de detalle de torneo, partida, promoción,
  acciones comunes y deshacer.

| Fase | Descripción |
|---|---|
| 3b (resto) | Settings: piezas/licencias + LicensesScreen |
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
- `fase-2-chesslib-adapter` (Fase 2 completa, commit `572ea87`)
- `fase-3a-temas-datastore` (Fase 3a completa, commit `9d924c9`)
- `fase-3b-navegacion-startup-tema` (Fase 3b completa, commit `062075b`)
- `fase-3c-torneos` (Fase 3c completa, commit `9c3a0cb`)
- `fase-4-tablero` (Fase 4 en curso, HEAD actual, pendiente commit)

### ⚠️ Remoto
- El remoto `origin` (`Salmeron52/plantillas_ajedrez`) está **vacío**: nunca
  se ha hecho push. Todo el trabajo existe solo en local. Pendiente
  `git push -u origin` de las ramas cuando Manolo lo autorice.

---

## ➡️ Continuación exacta al retomar

1. Estar en `fase-4-tablero`: `git checkout fase-4-tablero`.
2. **Commit extenso en español de Fase 4** (tablero + piezas + planilla +
   deshacer + detalle de torneo). Confirmar con Manolo antes de commitear.
3. Tras el OK → **Fase 5** (ScoresheetPanel completo: variantes/comentarios/
   NAGs/figurín + autosave completo). De momento no se han creado casos de
   uso de `:domain`: los ViewModels usan los puertos directamente (patrón
   establecido y testable); decidir con Manolo si crearlos en el futuro.

---

## 📝 Notas de la última sesión

- Fase 4 (tablero) casi completa en rama `fase-4-tablero`. Manolo verificó
  los checkpoints (piezas en posición, coordenadas, movimiento perfecto).
- Mejoras de esta sesión: coordenadas del borde fijas a 11sp; planilla con
  jugadas grandes (18sp) y **dibujo de pieza en silueta blanca estándar** en
  lugar de la letra (nuevo `segmentosDeSan` + `PlanillaPartida`); **movimiento
  directo** bidireccional (pieza con un solo destino → se mueve; casilla solo
  alcanzable por una pieza → se mueve); **deshacer jugadas** con botón ↩.
- Pendiente: commit extenso de Fase 4, luego Fase 5 (ScoresheetPanel).
- Fase 3c commiteada (`9c3a0cb`). Remoto GitHub sigue vacío (sin push nunca).