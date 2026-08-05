# 📋 INSTRUCCIONES.md — Plantilla_ajedrez

> Archivo de seguimiento obligatorio (Regla de Oro 0 de AGENTS.md).
> Objetivo: poder retomar el proyecto en cualquier sesión sabiendo exactamente
> qué se ha hecho, qué falta, cómo está configurado y qué reglas se respetan.

---

## 🎯 Objetivo de la app

Plantilla de ajedrez electrónica para que Manolo y otros usuarios anoten sus
partidas mientras las juegan, las guarden en formato PGN interoperable y las
compartan (PGN + plantilla PDF estilo FIDE con figurín). El análisis con
Stockfish está **retirado temporalmente** (sesión 05-ago-2026); si se retoma,
iría **sólo post-partida** (anti-fraude) y con binario alineado a 16 KB.

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
- **Optimización APK**: recursos pre-build en raíz. La tarea Gradle
  `descargarStockfish` y la carpeta `app/src/main/jniLibs` fueron **retiradas
  temporalmente** junto con Stockfish (05-ago-2026).
- **UI minimalista**: mejor icono de 3 puntos (overflow) que botones sueltos.
- **Autosave**: torneos/partidas/jugadas se guardan automáticamente.
- **Anti-fraude**: el botón Motor sólo se habilita si la partida está finalizada.

---

## 🏗️ Arquitectura

Multimódulo Gradle (Clean Architecture + DIP):

```
:app    → Presentación (Compose + ViewModels + Hilt + Nav)
:domain → Entidades, puertos (interfaces), casos de uso (Kotlin puro, sin Android)
:data   → Room, ChesslibAdapter (PuertoMotorAjedrez), PgnAdapter,
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
- Stockfish (retirado temporalmente; GPLv3 si se reincorpora).
  Piezas cburnett (Lichess): GPLv2+.
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

### ✅ Fase 4 — HECHA (rama `fase-4-tablero`, commit `5eb8193`)

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

### ✅ Fase 5a — VISUALIZACIÓN PLANILLA (rama `fase-4-tablero`, commit `b1190d4`)

Alcance decidido por Manolo: **visualizar** variantes, comentarios y NAGs ya
guardados (sin edición) + navegación por toque entre jugadas. Verificada por
Manolo (compila y funciona). **Commiteada como `b1190d4`**.

- **`ui/tablero/ParseadorMovetext.kt`** (nuevo, puro y testable):
  - `ElementoMovetext` (Jugada/Variante/Comentario/Nag/Resultado).
  - `parsearMovetext`: tokenizador robusto que respeta `(...)` (variantes
    anidadas), `{...}` y `;...` (comentarios con espacios), `$n` (NAGs),
    números de jugada pegados ("12.Nf3") y descarta tags `[...]`.
  - `sansLineaPrincipal`: SANs solo de la línea principal (ignora variantes,
    comentarios, NAGs y resultado) → para rejugar el FEN real.
  - `movetextSinCabecera`: extrae el movetext puro de un PGN con tags.
  - `simboloNag`: mapa NAG→símbolo (!, !!, ?!, ±, ∞...).
- **`ui/tablero/PlanillaPartida.kt`** (reescrita): recibe `movetext` +
  `posicionVisible` + `onJugadaPulsada`. Renderiza estructura completa con
  figurín (silueta blanca estándar), variantes en bloque indentado con fondo
  suave, comentarios en cursiva, NAGs en color terciario y resultado en
  negrita. Las jugadas pulsables navegan; la jugada visible se resalta con
  `tertiaryContainer`.
- **`PartidaViewModel`**: nuevo estado `movetext`, `fenVisible`,
  `resultadoVisible`, `ladoEnTurnoVisible` y `posicionVisible: Int?`
  (null = final). `mostrarPosicion(plies)` rejuega hasta ese ply y muestra la
  posición; `volverAlFinal()` desbloquea. El tablero **se bloquea** mientras
  se revisa (`onCasillaPulsada` con `posicionVisible != null` → return). Al
  jugar o deshacer se vuelve al final. La carga usa `sansLineaPrincipal`
  (robusto ante variantes).
- **`PantallaPartida`**: tablero usa `fenVisible`; botón "Volver al final"
  visible solo en modo revisión; `textoEstado` refleja la posición visible.
- **`strings.xml`**: `partida_volver_final`.
- **Tests**: `ParseadorMovetextTest` (11 tests: línea principal, numeración
  larga/pegada, variantes anidadas, comentarios, NAGs, resultado, tags,
  línea principal ignorando variantes, símbolos NAG).

Pendiente: checkpoint Manolo + commit extenso.

### ✅ Fase 5b — EDICIÓN COMENTARIOS/NAGS + VARIANTES (rama `fase-4-tablero`, HECHA y VERIFICADA por Manolo, commiteada esta sesión)

Alcance **rediseñado por Manolo** tras feedback de la 1ª implementación
("no se abre el diálogo" con pulsación larga):
1. **Siempre ha de haber un botón para editar y añadir comentarios** (modo
   edición activable por botón de la barra superior, nunca pulsación larga).
2. En modo edición las **variantes se introducen jugando en el tablero**.
3. **Al pulsar una jugada en modo edición** se selecciona y el tablero va a su
   posición previa para poder jugar la variante.
4. **Se acumulan variantes** y dentro de cada una se pueden añadir
   **subvariantes** pulsando la jugada (sin límite de profundidad); el tablero
   muestra la posición de la variante/subvariante pulsada.

Implementado, compilado y **verificado por Manolo** ("Perfecto todo").
Detalle completo de implementación:

- **`ui/tablero/ParseadorMovetext.kt`** — **modelo de "camino"** para navegar
  y operar el árbol de la planilla de forma inequívoca:
  - `PasoCamino` (sealed): `Lineal(cantidad)` (avanza N jugadas en la lista
    actual) y `EntrarVariante(indice)` (entra en la variante nº X pegada a la
    jugada). `CaminoPlanilla(pasos)` con `operator plus` y `INICIO`.
  - Travesores privados: `localizarEnArbol` (lista+índice de la jugada),
    `localizarListaFinal`, `localizarYOperar`, `localizarYOperarLista`
    (parsean, localizan, operan y re-serializan con `serializarMovetext`).
  - Públicos:
    - `sansDeCamino(movetext, camino)`: SANs que hay que rejugar para llegar a
      la posición del camino. Al entrar en una variante se CONSERVA el SAN de la
      jugada "padre": la variante se reproduce DESPUÉS de esa jugada, porque la
      app genera las variantes jugando sobre la posición que deja la jugada
      seleccionada (la primera jugada de la variante es la respuesta del rival)
      → rejugar devuelve la posición exacta incluso en variantes/subvariantes
      anidadas.
    - `anotacionEnCamino` / `actualizarAnotacionEnCamino`: leer/reemplazar
      comentario y NAG de una jugada **cualquiera** (principal o variante).
    - `insertarVarianteEnCamino(movetext, camino, sans)`: inserta una variante
      **después** de las anotaciones y variantes ya pegadas a la jugada → su
      índice coincide con `numeroDeVariantesPegadas` previo (así se acumulan).
    - `agregarJugadaAVarianteEnCamino(movetext, caminoVariante, san)`: extiende
      la línea de una variante (para construirla jugada a jugada).
    - `numeroDeVariantesPegadas(movetext, camino)`: cuenta variantes de la
      jugada → índice de la siguiente.
  - Se mantienen las funciones por-ply (`anotacionDeJugada`,
    `actualizarAnotacionDeJugada`) de la 1ª iteración: ya NO se usan en
    producción (el ViewModel usa caminos) pero siguen con sus tests.
- **`ui/tablero/PartidaViewModel.kt`** — **modo edición** basado en caminos:
  - Estado nuevo: `caminoVisible: CaminoPlanilla?` (sustituye a
    `posicionVisible: Int?`), `modoEdicion: Boolean`, `caminoSeleccion`,
    `comentarioEdicion`, `nagEdicion`, `varianteEnConstruccion`.
  - `entrarModoEdicion()` / `salirModoEdicion()` (botón TopAppBar).
  - `seleccionarJugada(camino)` (al pulsar jugada en modo edición): carga el
    comentario/NAG actuales y **muestra la posición que deja esa jugada**
    (rejuega `sansDeCamino` completo) para poder jugar la variante desde ahí.
  - `realizarJugadaEdicion(desde,hasta,promocion)`: 1ª jugada crea la variante
    (`insertarVarianteEnCamino`, índice = `numeroDeVariantesPegadas`) y fija
    `varianteEnConstruccion`; las siguientes la extienden
    (`agregarJugadaAVarianteEnCamino`). Persiste tras cada jugada (autosave).
  - `alPulsarJugada(camino)` (en modo edición → `seleccionarJugada`; en juego
    normal → `mostrarCamino`). `mostrarCamino` sustituye a `mostrarPosicion`.
  - `actualizarComentarioEdicion` / `actualizarNagEdicion` / `guardarEdicion`
    (escribe `actualizarAnotacionEnCamino` y persiste).
  - `onCasillaPulsada` y `confirmarPromocion` ramifican modo edición (operan
    sobre `fenVisible`/`ladoEnTurnoVisible`) vs juego normal. `deshacerJugada`
    se bloquea en modo edición. `realizarJugada`/`deshacerJugada` siguen
    usando `agregarJugadaAlMovetext`/`eliminarUltimaJugadaDelMovetext` (las
    anotaciones se conservan al jugar/deshacer).
- **`ui/tablero/PlanillaPartida.kt`** (reescrita): renderizado **recursivo**
  del árbol. Cada jugada lleva su `CaminoPlanilla` (`baseCamino` +
  `Lineal(n)`); las variantes usan `baseCamino` + `EntrarVariante(indice)` y
  se renderizan con `VarianteVisual` → **subvariantes anidadas sin límite**.
  Parámetros: `caminoVisible`, `caminoSeleccion`, `onJugadaPulsada
  (CaminoPlanilla)`. Se elimina `combinedClickable`/pulsación larga. Resaltado
  doble: `tertiaryContainer` = jugada visible, `primaryContainer` = jugada
  seleccionada en edición.
- **`ui/tablero/PantallaPartida.kt`** (reescrita): botón **siempre visible**
  en TopAppBar para entrar/salir de edición (icono Editar ↔ Cerrar; deshacer
  desactivado en edición). `PanelEdicion` inline (sustituye al antiguo
  `DialogoEditarJugada`): instrucción si no hay jugada seleccionada; con
  selección → fila comentario+Guardar + chips NAG compactos + aviso de variante.
  El **tablero siempre a tamaño completo** (`fillMaxWidth`, sin `weight`); el
  contenido es `verticalScroll` para que el teclado no oculte los controles.
- **`strings.xml`**: `partida_editar`, `partida_salir_edicion`,
  `edicion_instrucciones`, `edicion_variante_hint`,
  `edicion_variante_en_curso` (más las de edición ya existentes).
- **Tests** `ParseadorMovetextTest` (+11 de caminos): `sansDeCamino` (principal,
  variantes anidadas), `anotacionEnCamino` (principal y variante),
  `actualizarAnotacionEnCamino`, `insertarVarianteEnCamino` (tras jugada,
  acumula varias, subvariante dentro de variante), `agregarJugadaAVarianteEnCamino`,
  `numeroDeVariantesPegadas`.

Pendiente Fase 5b: NINGUNO. Manolo compiló y verificó el modo edición completo,
la distinción visual de las variantes (cursiva + llaves) y la navegación a
jugadas de análisis. TODO COMMITEADO esta sesión. Siguiente trabajo: ver
"Continuación exacta al retomar".

**Bug NAG corregido (sesión actual)**: el NAG no tenía marca visual asociada
a la jugada seleccionada. Se añadió `caminoJugadaActual` en `ContenidoLista`
para rastrear a qué jugada pertenecen los comentarios/NAGs. Ahora el NAG de
la jugada seleccionada se resalta en `primary` en lugar de `tertiary`. Se
añadieron 5 tests de round-trip NAG (verificar posición tras
guardar→serializar→re-parsear).

**Variantes: distinción visual + navegación (sesión actual)**:
- Manolo confirmó que **sí se guardan** las variantes (aparecen en la
  planilla) y que los comentarios también; pero pidió que el análisis se
  distinga claramente de la partida real.
- **Distinción visual** en `PlanillaPartida.kt`: `VarianteVisual` ahora
  delimita el bloque con llaves "{ ... }" (`LlaveCursiva`) y muestra las
  jugadas del análisis **en cursiva**. Se añadió el parámetro `cursiva` a
  `ContenidoLista` (texto de jugadas y NAGs) y a `JugadaConIcono` (solo el
  texto; el figurín es imagen y no se afecta). Las subvariantes se renderizan
  recursivamente con el mismo estilo.
- **Bug de navegación corregido** en `sansDeCamino` (ParseadorMovetext.kt):
  antes, al entrar en una variante se descartaba el SAN de la jugada "padre",
  por lo que al pulsar una jugada de análisis el rejuego fallaba (p.ej. `d5`
  como jugada negra se intentaba aplicar desde la posición inicial con blancas
  al turno) y el tablero se quedaba en la posición inicial/prevía de la partida
  real. Ahora se conserva el SAN de la jugada padre y las jugadas de la
  variante se reproducen después de ella → la posición mostrada es la de la
  jugada de análisis pulsada (también en subvariantes).
- **Tests**: se sustituyeron los 2 tests de `sansDeCamino` que documentaban el
  comportamiento anterior (descarte del padre) por 2 nuevos que reflejan el
  comportamiento corregido con notación del estilo de la app:
  `sansDeCamino conserva la jugada padre al entrar en una variante`
  (`1. e4 ( d5 ) e5` → `[e4, d5]`) y `sansDeCamino atraviesa subvariantes
  anidadas` (`1. e4 ( d5 Nf3 ( Bg7 ) ) e5` → `[e4, d5, Nf3, Bg7]`).

### Fases siguientes (pendientes)

| Fase | Descripción |
|---|---|
| 3b (resto) | Settings: piezas/licencias + LicensesScreen |
| 4 | BoardComposable Canvas + piezas cburnett + entrada táctil |
| 5 | ScoresheetPanel (variantes/comentarios/NAGs/figurín) + autosave — **5a visualización ✅ (`b1190d4`), 5b edición ✅ (commit de esta sesión): modo edición con comentarios/NAGs + variantes/subvariantes desde el tablero, análisis en cursiva con llaves, navegación por toque** |
| 6 | ~~StockfishAdapter UCI~~ **RETIRADA temporalmente** (05-ago-2026): se eliminaron los `.so` de `jniLibs`, la tarea `descargarStockfish`, los adaptadores UCI (`ParseadorUci`, `ConvertidorUciSan`, `ProcesoStockfish`, `AdaptadorStockfish`), el puerto `PuertoEvaluacionMotor` y sus tests. Si se retoma hay que partir de cero con binario **alineado a 16 KB** (los builds oficiales de Stockfish vienen a 4 KB y Google Play los rechaza desde nov-2025 para targetSdk 35+; recompilar con NDK r28+ o con `-Wl,-z,max-page-size=16384`) |
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
- `fase-4-tablero` (Fase 4 `5eb8193` + Fase 5a `b1190d4` + **Fase 5b commiteada esta sesión**; HEAD actual)

### ⚠️ Remoto
- El remoto `origin` (`Salmeron52/plantillas_ajedrez`) está **vacío**: nunca
  se ha hecho push. Todo el trabajo existe solo en local. Pendiente
  `git push -u origin` de las ramas cuando Manolo lo autorice.

---

## ➡️ Continuación exacta al retomar

1. Estar en `fase-4-tablero`: `git checkout fase-4-tablero`. La Fase 5b está
   **commiteada y verificada** por Manolo (modo edición + variantes en cursiva
   con llaves + navegación correcta a jugadas de análisis).
2. **Stockfish RETIRADO temporalmente** (05-ago-2026): la app es actualmente
   **solo planilla** (anotar, guardar PGN, compartir). Siguiente trabajo
   (decidir con Manolo): borrado de jugadas intermedias, deshacer variante en
   construcción, o **Fase 7** (Import/Export PGN + PDF plantilla FIDE).
3. Nota arquitectura: aún no se han creado casos de uso de `:domain`; los
   ViewModels usan los puertos directamente (patrón establecido y testable).
   Decidir con Manolo si crearlos en el futuro.

---

## 📝 Notas de la última sesión

- **Fase 5b COMPLETA y COMMITEADA** tras OK de Manolo ("Perfecto todo").
  Manolo verifica: modo edición, variantes en cursiva con llaves "{ ... }" y
  navegación correcta al pulsar jugadas de análisis.
- **Fase 5b rediseñada e implementada**: la 1ª iteración
  (edición por pulsación larga + diálogo modal) **no funcionó** (el diálogo no
  se abría) y Manolo pidió variantes jugables en el tablero → se sustituyó por
  un **modo edición** activable con botón siempre visible. Se añadió el modelo
  de **`CaminoPlanilla`/`PasoCamino`** al parser (navegación inequívoca por el
  árbol de la planilla: principal, variantes y subvariantes sin límite) y el
  ViewModel/planilla/pantalla se reescribieron en consecuencia. El tablero de
  edición siempre a tamaño completo (sin `weight`); contenido scrollable para
  que el teclado no oculte el botón Guardar ni los controles.
- **Bug NAG corregido**: el NAG no se resaltaba visualmente como asociado a la
  jugada seleccionada. Se añadió tracking de `caminoJugadaActual` en
  `ContenidoLista` y resaltado en `primary` para NAGs de la jugada
  seleccionada. 5 tests de round-trip NAG añadidos.
- **Variantes: distinción visual + navegación (última sesión)**:
  - Manolo confirmó que **sí se guardan** las variantes (aparecen en la
    planilla) y los comentarios también; pidió que el análisis se distinga de
    la partida real.
  - **Distinción visual** en `PlanillaPartida.kt`: `VarianteVisual` delimita el
    bloque con llaves "{ ... }" (`LlaveCursiva`) y muestra las jugadas del
    análisis **en cursiva** (parámetro `cursiva` en `ContenidoLista` y
    `JugadaConIcono`; el figurín no se afecta por ser imagen). Las subvariantes
    se renderizan recursivamente con el mismo estilo.
  - **Bug de navegación corregido** en `sansDeCamino` (ParseadorMovetext.kt):
    antes se descartaba el SAN de la jugada "padre" al entrar en una variante,
    por lo que al pulsar una jugada de análisis el rejuego fallaba (p.ej. `d5`
    como jugada negra se intentaba aplicar desde la posición inicial con
    blancas al turno) y el tablero se quedaba en la posición inicial/prevía de
    la partida real. Ahora se conserva el SAN de la jugada padre y las jugadas
    de la variante se reproducen después de ella → la posición mostrada es la
    de la jugada de análisis pulsada (también en subvariantes).
  - **Tests**: 2 tests de `sansDeCamino` sustituidos por 2 nuevos que reflejan
    el comportamiento corregido con notación del estilo de la app
    (`sansDeCamino conserva la jugada padre al entrar en una variante` y
    `sansDeCamino atraviesa subvariantes anidadas`). Se suman a los tests de
    round-trip NAG, flujo completo de variantes, `serializarMovetext`,
    `agregarJugadaAlMovetext`, `eliminarUltimaJugadaDelMovetext`, caminos, etc.
- **Pendiente para la próxima sesión** (decidir con Manolo): borrado de jugadas
  intermedias, deshacer de variante en construcción, o **Fase 7 (Import/Export
  PGN + PDF)**. (Fase 6 Stockfish retirada — ver bloque de la sesión 05-ago-2026.)
- **IMPORTANTE para retomar**: la app compila y la Fase 5b está commiteada.
  No hay trabajo a medias sin commitear. Remoto GitHub sigue vacío (sin push
  nunca); autorizar push solo si Manolo lo pide.

---

## 🗒️ Sesión 05-ago-2026 — Fase 6 (Stockfish) CREADA y luego RETIRADA

**Contexto**: se arrancó la **Fase 6 (Stockfish)** elegida por Manolo. Se
implementó toda la Unidad A (capa de datos UCI) y sus tests, pero Manolo
decidió **eliminar temporalmente el análisis de partidas** para dejar la app
como **solo planilla**. Todo el trabajo de Stockfish se eliminó **antes de
commitear** (nunca llegó a git).

### Lo que se creó y luego se eliminó (para no rehacerlo a ciegas)
- **Domain**: `PuertoEvaluacionMotor.kt` (`Evaluacion(profundidad, scoreCentipepeños, mejorJugada, lineaPrincipal)` + interfaz `arrancar/parar/analizar:Flow<Evaluacion>/mejorJugada`).
- **Data (paquete `data/.../stockfish/`)**: `ParseadorUci` (regex `info depth… score cp|mate… pv…` + `bestmove`, `PUNTUACION_MATE=100_000`), `ConvertidorUciSan` (UCI→SAN vía chesslib `MoveList(fen)` + `Move` con **argumentos posicionales**, nunca con nombre: es un constructor Java), `InterfazProcesoMotor` + `ProcesoStockfish` (ProcessBuilder, UTF-8), `AdaptadorStockfish` (Mutex + Dispatchers.IO + handshake `uci`/`uciok` e `isready`/`readyok`, factory inyectable para tests).
- **Hilt**: `data/di/ModuloMotor.kt` (ruta `File(context.applicationInfo.nativeLibraryDir, "libstockfish.so")`, `@RutaBinarioStockfish` qualifier) y binding de `PuertoEvaluacionMotor`.
- **Tests** (los 3): `ParseadorUciTest`, `ConvertidorUciSanTest`, `AdaptadorStockfishTest` (con `ProcesoMotorFalso`/`FabricaProcesoFalsa`).

### ⚠️ Lección clave: alineación 16 KB (bloqueo Google Play)
- Los **builds oficiales de Stockfish para Android** (releases GitHub, ej.
  `stockfish-android-armv8.tar`) vienen con LOAD segments alineados a **4 KB**,
  incluso Stockfish 18 (`sf_18`, compilado con NDK r27c sin flags 16 KB).
- Desde el **1-nov-2025**, Google Play exige apps con targetSdk 35+ (nosotros 37)
  **compatibles con páginas de 16 KB** → un `.so` a 4 KB es **rechazado**.
- Verificado con Python leyendo `p_align` de `PT_LOAD` (3 segmentos a `0x1000`).
- **Soluciones si se retoma**: (a) recompilar desde fuente con **NDK r28+**
  (alinea por defecto) o con `-Wl,-z,max-page-size=16384
  -Wl,-z,common-page-size=16384`; (b) usar un prebuild 16 KB-aligned de un
  proyecto de confianza (ej. `ber4444/compose-multiplatform-chess` lo recompiló
  así). El `armeabi-v7a` (32 bits) NO lo exige.

### Cambios persistentes en el repo (PENDIENTES de commitear tras la sesión)
- `app/build.gradle.kts`: eliminada la tarea `descargarStockfish`, `binariosStockfish`,
  el `splits.abi` y sus imports → ahora **un único APK** sin binarios nativos.
- `app/proguard-rules.pro`: eliminada la sección JNI/Stockfish y su mención en cabecera.
- `.gitignore`: eliminadas las reglas de `jniLibs/libstockfish.so`.
- `NOTICE`: eliminada la atribución de Stockfish (nota de retirada temporal).
- Comentarios KDoc sin Stockfish en `MainActivity.kt`, `PlantillaApplication.kt`,
  `PuertoLicencias.kt` y cabecera de `data/build.gradle.kts`.

### 🎨 Ajustes de UI de la pantalla de partida (misma sesión, MANOLO: "Perfecto todo")
Manolo indicó que la **zona de planilla ha de ocupar el resto del espacio libre**
bajo el tablero, con el texto **un poco más pequeño** y **perfectamente alineado**
(las jugadas se veían ligeramente inclinadas/desalineadas).

- **`ui/tablero/PlanillaPartida.kt`**:
  - Eliminado `heightIn(max = 140.dp)` → la planilla ya no tiene tope de altura.
  - Tipografía uniformada con **`lineHeight = 20.sp`** en todos los elementos
    (números de jugada 15sp, comentarios 13sp, NAGs 15sp, resultado 15sp, texto
    de jugada 16sp, llaves 13sp). Eliminada la `copy(...)` con tamaños distintos.
  - Icono de pieza en el figurín: `Modifier.size(20.dp)` (antes 22dp).
  - Quitado el `padding(vertical = 1.dp)` del chip de jugada (queda solo el
    horizontal de 4dp) → las jugadas quedan centradas y alineadas con el texto.
  - Eliminado el import `fillMaxWidth` (ya no se usa en el archivo).
- **`ui/tablero/PantallaPartida.kt`** (reestructuración del layout):
  - Antes: todo dentro de un único `Column(verticalScroll)` → la planilla con
    `heightIn(140dp)` flotaba al final.
  - Ahora: la **sección superior** (texto de estado, `TableroAjedrez` y
    `PanelEdicion`) va en una `Column` interna con `verticalScroll` (ocupa su
    altura natural); la **zona de planilla** va en una `Column` con
    `weight(1f)` + `fillMaxWidth()` que ocupa el **resto del espacio libre**
    (solo si `movetext.isNotBlank()`), con `PlanillaPartida` a `weight(1f)`
    (scroll interno) y el botón "Volver al final" debajo.
  - Añadido `imePadding()` a la `Column` raíz (import nuevo) para que el teclado
    no oculte los controles del panel de edición.
- Verificado por Manolo en Android Studio: "Perfecto todo. Continuamos".

**Estado al cierre de la sesión**: estos cambios están en el working tree de
`fase-4-tablero`, **sin commitear** (junto con el borrado de Stockfish). El
commit extenso en español se hará tras el OK de Manolo. **Push NO** (remoto
vacío, autorización obligatoria).