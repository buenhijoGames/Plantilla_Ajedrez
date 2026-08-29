# ♟️ Plantilla de Ajedrez

> **Plantilla de ajedrez electrónica para Android desarrollada por buenhijoGames.**  
> Diseñada para que los ajedrecistas anoten sus partidas en torneos, matches o partidas individuales mientras juegan, con validación de reglas FIDE, generación de planillas oficiales en PDF, formato PGN estándar interoperable y herramientas de estudio y reproducción.

[![Licencia: GPL v3](https://img.shields.io/badge/Licencia-GPLv3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2027+-brightgreen.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)

---

## 🌟 Características Principales

- 📱 **Tablero Interactivo Canvas:**
  - Renderizado nítido con piezas clásicas de alta calidad (*cburnett*, autoría de Colin M. L. Burnett / Lichess).
  - Coordenadas algebraicas oficiales y resaltado visual de casillas de origen, destino y jaques.
  - Botón para **girar el tablero** (perspectiva de piezas negras o blancas).
  - **Diseño responsivo** optimizado para orientación vertical y apaisada (horizontal) maximizando el tamaño del tablero en teléfonos y tablets.

- 📝 **Planilla Electrónica Inteligente:**
  - Registro automático e instantáneo de jugadas con notación FIDE y figurín gráfico de la pieza.
  - Navegación táctil por la planilla para revisar posiciones anteriores sin perder la posición de juego.
  - **Árbol completo de análisis:** soporte de comentarios (`{...}`), símbolos de evaluación NAG (`!`, `?`, `?!`, `±`, etc.) pegados a la jugada y **variantes y subvariantes anidadas sin límite de profundidad** introducidas directamente jugando en el tablero.
  - **Rectificación y eliminación de jugadas:** permite descartar jugadas erróneas en cadena con confirmación modal y botón **Rehacer** reactivo para restaurar de inmediato si se borró por error.

- 📄 **Exportación e Interoperabilidad FIDE:**
  - **Planilla oficial FIDE en PDF:** Generación en alta resolución (hoja A4 vertical, 4 columnas con figurín) lista para imprimir y firmar.
  - **Exportación / Importación PGN:** Compatibilidad total con visores y motores universales (Chess.com, Lichess, ChessBase) usando *Storage Access Framework* (SAF) y compartición mediante el sistema nativo de Android.
  - Opción de exportar o guardar el torneo/match completo en un único documento multipágina PDF o archivo multi-partida PGN.

- ⚔️ **Gestión de Torneos, Matches y Partidas Sueltas:**
  - Creación de torneos con rondas correlativas y datos de cabecera (Evento, Sitio, Fecha, Elos).
  - **Modo Match (2 jugadores):** alternancia automática de color de piezas en cada partida consecutiva manteniendo los Elos de los rivales.
  - Creación de partidas individuales sueltas sin necesidad de torneo.
  - Búsqueda y filtrado instantáneo por nombre de jugador, torneo o fecha.

- ▶️ **Reproducción Automática (Auto-Play):**
  - Controles de navegación: *Inicio*, *Atrás*, *Auto (Play/Pausa)*, *Adelante* y *Final*.
  - Temporizador de pausa configurable por el usuario (1 a 60 segundos) persistente en DataStore.

- 🔊 **Efectos de Audio de Baja Latencia:**
  - Sonidos de ajedrez (movimiento, captura, jaque y enroque/promoción) gestionados con `SoundPool`.
  - Interruptor en Ajustes para activar o desactivar el audio.

- 🎨 **Temas Visuales y Autoguardado:**
  - 5 temas seleccionables: *Claro*, *Oscuro*, *Dinámico (Material You)*, *Madera* y *Mármol*.
  - **Autoguardado continuo:** cada movimiento y comentario se persiste inmediatamente en base de datos local SQLite/Room (0% pérdida de partidas).

---

## 🏛️ Arquitectura y Tecnologías

El proyecto sigue una arquitectura **Clean Architecture Modularizada** basada en principios SOLID:

```text
├── :app      → Capa de Presentación (Jetpack Compose, Material 3, ViewModels, Hilt, Navigation)
├── :domain   → Lógica de Negocio y Modelos Puros (Kotlin puro, sin dependencias de framework)
└── :data     → Infraestructura y Persistencia (Room DB, Mappers, AdaptadorChesslib, AdaptadorPdf)
```

- **Lenguaje:** Kotlin 100% con Español Técnico en código y documentación KDoc.
- **Inyección de Dependencias:** Dagger Hilt.
- **Procesamiento de Anotaciones:** KSP (Kotlin Symbol Processing).
- **Persistencia:** Room Database con migraciones manuales seguras y DataStore Preferences.
- **Validación de Ajedrez:** `chesslib` (Apache 2.0).
- **Generación PDF:** `PdfDocument` nativo con renderizado de vectores en Canvas.

---

## 🚀 Compilación e Instalación

### Requisitos
- Android Studio Ladybug | 2024.2+ o superior.
- JDK 17 o 21.
- Android SDK (minSdk: 27, targetSdk / compileSdk: 37).

### Pasos
1. Clona este repositorio:
   ```bash
   git clone https://github.com/buenhijoGames/Plantilla_Ajedrez.git
   ```
2. Abre el proyecto en Android Studio.
3. Sincroniza Gradle con los archivos del proyecto (*Sync Project with Gradle Files*).
4. Ejecuta en tu dispositivo o emulador:
   ```bash
   ./gradlew :app:assembleDebug
   ```

---

## 📜 Licencia y Atribuciones

Este proyecto es software libre y de código abierto, distribuido bajo los términos de la **[Licencia Pública General GNU v3.0 (GPLv3)](LICENSE)**.

### Componentes de terceros y créditos:
- **Piezas cburnett y efectos de sonido:** Colin M. L. Burnett / [Lichess.org](https://lichess.org) (GPLv2+ / CC BY-SA 3.0 / CC0).
- **Lógica y notación de ajedrez:** `chesslib` por bhlangonijr (Apache License 2.0).
- **Framework de desarrollo:** AndroidX, Jetpack Compose, Material 3, Hilt y Room por The Android Open Source Project (Apache License 2.0).
- **Lenguaje y corrutinas:** Kotlin por JetBrains s.r.o. (Apache License 2.0).

Consulta el archivo [NOTICE](NOTICE) para ver los textos completos de copyright y atribución.

---

## ✉️ Contacto

- **Desarrollador:** buenhijoGames
- **Correo electrónico:** buenhijogames@gmail.com
- **Repositorio:** [https://github.com/buenhijoGames/Plantilla_Ajedrez](https://github.com/buenhijoGames/Plantilla_Ajedrez)
