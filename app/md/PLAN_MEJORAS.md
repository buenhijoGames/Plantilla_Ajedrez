# 🛠️ PLAN MAESTRO DE MEJORAS — Plantilla_ajedrez

> Documento generado el **20-ago-2026** tras revisión exhaustiva del código y
> entrevista con Manolo sobre requisitos y prioridades.
>
> **Rama activa:** `fase-7-pgn-pdf`  
> **Archivos sin commitear:** 15 (incluye el rediseño PDF a 4 columnas de hoy).

---

## 📋 ÍNDICE

1. [Estado actual del código](#1-estado-actual-del-código)
2. [Bugs y problemas conocidos](#2-bugs-y-problemas-conocidos)
3. [Mejoras de UX aprobadas por Manolo](#3-mejoras-de-ux-aprobadas-por-manolo)
4. [Mejoras técnicas y de calidad](#4-mejoras-técnicas-y-de-calidad)
5. [Funcionalidades descartadas](#5-funcionalidades-descartadas)
6. [Plan de ejecución por fases](#6-plan-de-ejecución-por-fases)

---

## 1. Estado actual del código

### Arquitectura

```
:app    → Presentación (Compose + ViewModels + Hilt + Navigation)
:domain → Entidades, puertos/interfaces (Kotlin puro, sin Android)
:data   → Room, AdaptadorChesslib, AdaptadorPgn, AdaptadorPdf, Módulos Hilt
```

### Pantallas implementadas

| Pantalla | Archivo(s) | Estado |
|---|---|---|
| Inicio (con diálogo) | `PantallaInicio.kt`, `StartupDialog.kt` | ⚠️ Rediseñar (quitar diálogo) |
| Lista de torneos | `PantallaTorneos.kt`, `TorneosViewModel.kt` | ✅ Funcional |
| Detalle de torneo | `PantallaDetalleTorneo.kt`, `DetalleTorneoViewModel.kt` | ✅ Funcional |
| Partida (tablero + planilla) | `PantallaPartida.kt`, `PartidaViewModel.kt` | ✅ Funcional |
| Ajustes (temas) | `PantallaAjustes.kt`, `AjustesViewModel.kt` | ✅ Funcional |
| Información | `PantallaInfo.kt` | ✅ Funcional |

### Funcionalidades existentes

- ✅ Tablero interactivo con Canvas + piezas cburnett (Lichess)
- ✅ Validación de jugadas legales vía chesslib
- ✅ Planilla electrónica con figurín, variantes, comentarios y NAGs
- ✅ Navegación por toque en la planilla (revisión de posiciones)
- ✅ Modo edición: comentarios, NAGs y variantes/subvariantes
- ✅ Promoción de peón con diálogo de selección
- ✅ Exportación PDF (plantilla FIDE 4 columnas con figurín) ← **RECIÉN REDISEÑADA**
- ✅ Exportación/Importación PGN
- ✅ Compartir PDF/PGN vía Intent del sistema
- ✅ 5 temas visuales (Claro, Oscuro, Dinámico, Madera, Mármol)
- ✅ Persistencia Room con migraciones manuales
- ✅ Autoguardado tras cada jugada
- ✅ Licencias, información y atribuciones

### Tests existentes

| Módulo | Tests | Fallos |
|---|---|---|
| `:data` | 56 | 1 preexistente (`resultadoActual`) |
| `:app` | 65 | 1 preexistente (`UtilidadesTableroTest`) |

---

## 2. Bugs y problemas conocidos

### 🔴 Críticos

| # | Descripción | Archivos afectados |
|---|---|---|
| B1 | **Test `resultadoActual` falla** — `AdaptadorChesslibTest.kt:135`. El test espera `GANA_BLANCAS` con rey negro en mate pero `resultadoActual` no lo detecta correctamente. | `data/.../ajedrez/AdaptadorChesslib.kt` |
| B2 | **Test `UtilidadesTableroTest` falla** — Fallo preexistente sin corregir. | `app/.../tablero/UtilidadesTableroTest.kt` |

### 🟡 Moderados

| # | Descripción | Archivos afectados |
|---|---|---|
| B3 | **`app_name` es `Plantilla_ajedrez`** con guion bajo, poco profesional para la Play Store. Debería ser **"Plantilla de Ajedrez"** o nombre definitivo. | `app/res/values/strings.xml` (línea 2) |
| B4 | **PantallaInicio vacía** — Tras cerrar el diálogo, solo muestra el texto `"Plantilla_ajedrez"` centrado en la pantalla. Experiencia pobre. | `PantallaInicio.kt` |
| B5 | **Sin diálogo de confirmación al eliminar torneo** — El borrado es directo con CASCADE (borra todas las partidas). Peligroso. | `PantallaTorneos.kt`, `TorneosViewModel.kt` |
| B6 | **`@Keep` ausente en data classes de dominio** (`Partida`, `Torneo`). AGENTS.md exige `@Keep` en data classes serializables. Pendiente de decisión. | `domain/.../modelo/Partida.kt`, `Torneo.kt` |
| B7 | **Carpeta `casos_uso` vacía** — El paquete de dominio `casos_uso` existe pero está vacío. Limpiarlo o implementar casos de uso reales. | `domain/.../casos_uso/` |

### 🟢 Menores

| # | Descripción |
|---|---|
| B8 | Warning Kotlin en `@ApplicationContext` → ya corregido a `@param:ApplicationContext` en `AdaptadorPdf.kt`, pero revisar si afecta a otros ficheros. |
| B9 | `proguard-rules.pro` puede necesitar actualización tras los cambios de la sesión de hoy. |

---

## 3. Mejoras de UX aprobadas por Manolo

### M1 — 🏠 Quitar el diálogo de inicio y abrir directamente en Torneos

**Estado actual:** Al abrir la app se muestra `PantallaInicio` con `StartupDialog` que pregunta "¿Nuevo o guardado?". Ambas opciones navegan a la misma pantalla (`TORNEOS`).

**Acción:**
- Cambiar `startDestination` de `INICIO` a `TORNEOS` en `NavegacionPlantilla.kt`.
- Eliminar o simplificar `PantallaInicio.kt` y `StartupDialog.kt`.
- Mover las opciones del menú overflow (Ajustes, Info) a `PantallaTorneos.kt`.
- Añadir un FAB `+` visible en `PantallaTorneos` para crear torneo **o partida suelta**.

**Archivos:** `NavegacionPlantilla.kt`, `Destinos.kt`, `PantallaInicio.kt`, `StartupDialog.kt`, `PantallaTorneos.kt`.

---

### M2 — 📝 Crear partidas sueltas (sin torneo)

**Estado actual:** Toda partida requiere un `torneoId`. El campo es nullable en la entidad Room (`PartidaEntity.torneoId`), pero no hay flujo de UI para crear una partida sin torneo.

**Acción:**
- Añadir opción en `PantallaTorneos` (o en la nueva pantalla principal) para "Nueva partida suelta".
- Crear un `DialogoNuevaPartida.kt` que pida: Blancas, Negras, Evento (opcional), Sitio (opcional), Fecha, Ronda (opcional), Elo Blancas, Elo Negras.
- La partida se persiste con `torneoId = null`.
- Mostrar partidas sueltas en una sección separada de la lista de torneos, o en una pestaña/tab "Partidas sueltas".

**Archivos:** `PantallaTorneos.kt`, `TorneosViewModel.kt` (o nuevo VM), nuevo `DialogoNuevaPartida.kt`, `RepositorioPartidas` / DAO.

---

### M3 — 🔄 Botón para girar el tablero (perspectiva de negras)

**Estado actual:** El tablero siempre muestra las blancas abajo. No hay opción de girarlo.

**Acción:**
- Añadir un `IconButton` (🔄) en la `TopAppBar` o junto al tablero.
- Crear un estado `tableroGirado: Boolean` en `EstadoPartida` / `PartidaViewModel`.
- Modificar `TableroAjedrez.kt` para que invierta filas y columnas cuando `tableroGirado = true`:
  - La fila 0 pasa a ser rank 1 (en vez de 8).
  - La columna 0 pasa a ser file h (en vez de a).
  - Las coordenadas del borde (a-h, 1-8) se invierten.

**Archivos:** `TableroAjedrez.kt`, `PartidaViewModel.kt`, `PantallaPartida.kt`, `strings.xml`.

---

### M4 — ✏️ Edición de cabecera de la partida (nombre, elo, evento, fecha, etc.)

**Estado actual:** Los datos de cabecera se fijan al crear la partida y no se pueden modificar después.

**Acción:**
- Crear un `DialogoEditarCabecera.kt` accesible desde el overflow de `PantallaPartida`.
- Campos editables: Evento, Sitio, Fecha, Ronda, Blancas, Negras, Elo Blancas, Elo Negras.
- Al guardar, actualizar la `Partida` en Room y refrescar el `EstadoPartida`.
- Añadir también un botón/opción para **establecer el resultado** manualmente (1-0, 0-1, ½-½, en curso). Actualmente el resultado se detecta por mate, pero no hay opción de abandonar/acordar tablas formalmente.

**Archivos:** nuevo `DialogoEditarCabecera.kt`, `PartidaViewModel.kt`, `PantallaPartida.kt`, `RepositorioPartidas`, `strings.xml`.

---

### M5 — 🔍 Búsqueda de partidas por jugador, evento, fecha

**Estado actual:** No hay ningún tipo de búsqueda. Solo una lista plana de torneos.

**Acción:**
- Añadir un `SearchBar` o `TextField` de búsqueda en la pantalla de torneos (o una pantalla dedicada).
- Implementar consultas en `PartidaDao` con `LIKE %query%` por campos: `blancas`, `negras`, `evento`, `sitio`, `fecha`.
- Mostrar los resultados como lista de partidas con resaltado del campo coincidente.

**Archivos:** `PartidaDao.kt`, `RepositorioPartidas.kt`, `PantallaTorneos.kt` (o nueva pantalla `PantallaBusqueda.kt`).

---

### M6 — 💾 Guardar PDF/PGN directamente en carpeta del teléfono

**Estado actual:** Solo se puede compartir vía Intent (WhatsApp, email, etc.).

**Acción:**
- Usar `ActivityResultContracts.CreateDocument` para abrir el selector de destino SAF (Storage Access Framework).
- Añadir opción "Guardar en dispositivo" junto a "Compartir" en el menú overflow de la pantalla de partida y de detalle de torneo.
- Escribir los bytes del PDF/PGN al `OutputStream` del URI elegido por el usuario.

**Archivos:** `PantallaPartida.kt`, `PantallaDetalleTorneo.kt`, `CompartirArchivo.kt` (ampliar), `strings.xml`.

---

### M7 — 📱 Layout horizontal: tablero + planilla lado a lado

**Estado actual:** La pantalla de partida es siempre vertical (tablero arriba, planilla debajo con scroll).

**Acción:**
- Detectar la orientación con `LocalConfiguration.current.orientation`.
- En horizontal: `Row` con tablero grande a la izquierda (~55% ancho) y planilla scrollable a la derecha (~45%).
- En vertical: mantener el layout actual (tablero + planilla en `Column` con scroll).
- Ajustar el modo edición para que funcione también en horizontal.

**Archivos:** `PantallaPartida.kt` (refactorizar layout), posiblemente extraer `ContenidoPartidaVertical` y `ContenidoPartidaHorizontal`.

---

### M8 — ⚠️ Diálogo de confirmación al eliminar torneo

**Estado actual:** Al eliminar un torneo, se borra directamente con CASCADE (todas sus partidas).

**Acción:**
- Antes de eliminar, mostrar un `AlertDialog` indicando cuántas partidas se perderán.
- Texto: "¿Eliminar el torneo '{nombre}'? Se eliminarán X partida(s) de forma permanente."
- Botones: "Cancelar" y "Eliminar".

**Archivos:** `PantallaTorneos.kt`, `TorneosViewModel.kt`, `strings.xml`.

---

## 4. Mejoras técnicas y de calidad

### T1 — Corregir los 2 tests que fallan

- **`AdaptadorChesslibTest.resultadoActual`**: Investigar por qué no detecta el mate del rey negro. Posible problema con el FEN de prueba o con la lógica de `resultadoActual()` en `AdaptadorChesslib.kt`.
- **`UtilidadesTableroTest`**: Identificar y corregir el fallo preexistente.

### T2 — Renombrar `app_name` a un nombre profesional

- Cambiar `"Plantilla_ajedrez"` por **"Plantilla de Ajedrez"** (o el nombre definitivo para Play Store).
- Actualizar también el `android:label` en `AndroidManifest.xml`.

### T3 — Limpiar paquete `casos_uso` vacío

- Eliminarlo si no se van a usar casos de uso explícitos (la lógica vive en los ViewModels).
- O implementar casos de uso reales si se prefiere seguir estrictamente Clean Architecture (`ObtenerPartidasPorTorneoUseCase`, etc.).

### T4 — Añadir `@Keep` a data classes de dominio

- Según AGENTS.md (Regla 3), las data classes serializables necesitan `@Keep`.
- `Partida` y `Torneo` viajan como datos entre capas. Valorar si R8 puede ofuscar sus campos.

### T5 — Unificar el import innecesario en `PantallaAjustes.kt`

- Línea 92 usa `androidx.compose.foundation.layout.Row` con ruta completa en vez del import que ya existe.

### T6 — Commit pendiente

- Los 15 archivos modificados (incluido el rediseño PDF de hoy) están **sin commitear**.
- Tras la aprobación de Manolo, hacer commit extenso en español en la rama `fase-7-pgn-pdf`.

### T7 — Revisar la estrategia de `firstOrNull` en `dibujarBloqueJugadas`

- En el nuevo `AdaptadorPdf.kt`, la búsqueda de filas usa `filas.firstOrNull { it.numero == numJugada }`. Esto es O(n) por fila, resultando O(n²) en total. Para 30 filas es despreciable, pero podría optimizarse con un mapa `Map<Int, Fila>` si las partidas crecen.

---

## 5. Funcionalidades descartadas

Manolo ha indicado que **NO** quiere las siguientes funcionalidades:

| Funcionalidad | Motivo |
|---|---|
| Reloj de ajedrez | La app es solo para anotar, no para jugar con reloj |
| Análisis con Stockfish | No lo quiere; la app es solo para anotar |
| Estadísticas del jugador | No es prioritario |
| Base de datos de aperturas | No necesario |
| Colores de tablero por tema | Prefiere el clásico marrón/beige siempre |
| Soporte multiidioma | Solo español por ahora |
| Ofrecer tablas formalmente | No necesario |
| Importar posición FEN | No necesario |

---

## 6. Plan de ejecución por fases

> Cada fase es una unidad lógica: tras completarla se pide a Manolo que compile
> y verifique estabilidad, y se hace commit.

### FASE A — Correcciones y limpieza 🧹
1. **T1** Corregir los 2 tests fallidos.
2. **T2** Renombrar `app_name`.
3. **T3** Limpiar paquete `casos_uso` vacío.
4. **T4** Decidir sobre `@Keep` en data classes.
5. **T5** Corregir import en `PantallaAjustes.kt`.
6. **T6** Commit de todo lo pendiente.
7. **B5/M8** Diálogo de confirmación al eliminar torneo.

### FASE B — Flujo de inicio y partida suelta 🏠
1. **M1** Quitar `StartupDialog` y arrancar directo en Torneos.
2. **M2** Crear partidas sueltas sin torneo.
3. Mover menú overflow (Ajustes, Info) a la pantalla de Torneos.

### FASE C — Mejoras del tablero y partida ♟️
1. **M3** Botón para girar el tablero.
2. **M4** Diálogo para editar cabecera + selección manual de resultado.
3. **M7** Layout horizontal (tablero + planilla lado a lado).

### FASE D — Búsqueda y exportación 🔍
1. **M5** Búsqueda de partidas por jugador, evento, fecha.
2. **M6** Guardar PDF/PGN directamente en carpeta del dispositivo.

### FASE E — Pulido visual y Play Store 🎨
1. Diseño profesional de la pantalla de Torneos (tarjetas con información visual).
2. Icono de la app definitivo.
3. Screenshots para Play Store.
4. Revisión final de UI responsiva en tablets.

---

> **Nota:** Este documento se actualizará tras cada fase completada.
> Los cambios exactos de cada fase se detallarán en `INSTRUCCIONES.md` según
> el protocolo de AGENTS.md (Regla 0 y 13).
