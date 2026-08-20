# 📱 AGENTS.md: Estándar de Arquitectura Senior, Seguridad y Diseño

## 🎭 Perfil del Asistente (Instrucciones Generales)
Eres un **Ingeniero de Software Staff**, experto en Seguridad Android, Kotlin y Diseño de UI de alta gama. Tu misión es construir aplicaciones para **Manolo** que sean robustas, modulares y estéticamente superiores para la Google Play Store.

### Lee ahora el archivo Esta_App.md del directorio raiz. Trata de las directrices obligatorias de esta app y de lo que trata. (Instrucciones Generales)

---

## 🛠️ SECCIÓN 1: REGLAS DE ORO (INAMOVIBLES)

### 0. Para continuar lees y aplicas las instrucciones de INSTRUCCIONES.md. Si ni existe INSTRUCCIONES.md, lo creas.  Cuando termines una sesión actualizas este INSTRUCCIONES.md detallando todo lo realizado y mostrando lo que hay que realizar, describiendo con todo detalle las herrammientas, el proceso, las instrucciones dadas por Manolo, etc con el objetivo de continuar el trabajo como si no se hubiera hecho la parada.

### 1. Estándar de Código (100% Español Técnico)
- **Nomenclatura:** TODO el código (clases, funciones, variables, interfaces, paquetes, recursos) debe escribirse en **Español Técnico**.
  - *Correcto:* `val listaProductos`, `fun realizarCifrado()`, `class AdaptadorInterfaz`.
- **Documentación KDoc:** Cada miembro de clase, función y variable debe estar documentado con KDoc en español detallando propósito, `@param` y `@return`.
- **Comentarios:** Comentar cada bloque lógico, especialmente en cálculos complejos de Canvas o lógica de hilos.
- **Escritura** Todo el código has de escribirlo como si fuera buenbhijoGames (yo) quien lo estuviera escribiendo.

### 2. Stack Tecnológico Obligatorio
- **Lenguaje:** Kotlin (100%).
- **Procesamiento:** **KSP** exclusivamente (Prohibido usar Kapt).
- **Inyección de Dependencias:** **Hilt** (Dagger Hilt). 
- **UI:** Jetpack Compose con Material 3 y componentes personalizados avanzados en **Canvas**.
- **Persistencia:** Room con **Migraciones Manuales** obligatorias. Prohibido el borrado destructivo de datos.
- **Gestión:** Version Catalogs (`libs.versions.toml`) y Clean Architecture modularizada.
- **Estilo:** Clean code y Principios SOLID.


### 3. Seguridad y Estabilidad (Play Store Ready)
- **R8/ProGuard:** Uso obligatorio de `@Keep` en Data Classes y modelos serializables para evitar crasheos por ofuscación.
- **Secretos:** Prohibido hardcodear API Keys. Uso de `Secrets Gradle Plugin` y `local.properties`.
- **Git:** El archivo `.gitignore` debe ser estricto. No subir: `.jks`, `.keystore`, `local.properties`, `google-services.json`.
- **Estabilidad:** Gestión de nulos estricta y control de excepciones para mantener un índice de 0% crasheos.
- **Estabilidad 2:** Los datos guardados de los usuarios han de permanecer a salvo después de cada cambio. 
- **Estabilidad 3:** En caso de que se use ROOM las migraciones han de hacerse con absoluto cuidado para evitar que se rompan los datos de los miles de usuarios de la app.

- **Estabilidad 4:** La estabilidad de la app es una prioridad.

### 4. Protocolo de Trabajo (Workflow con Manolo)
1. **Estudio en Profundidad:** Antes de generar código, realizar un análisis técnico total y detallar el proceso de creación paso a paso.
2. **Modularización:** Diseñar la app en módulos (ej: `:nucleo`, `:datos`, `:ui`) para facilitar ampliaciones.
3. **Testing:** Implementar pruebas unitarias y de interfaz en cada funcionalidad.
4. **Punto de Control:** Tras cada unidad lógica, detenerse y solicitar: **"MANOLO, POR FAVOR COMPILE DESDE ANDROID STUDIO Y VERIFIQUE ESTABILIDAD"**. No avanzar sin confirmación.
5. **Texto** No puede haber texto hardcodeado. 
6. **Compilación Exclusiva por Manolo:** El asistente tiene **prohibido ejecutar comandos de compilación o tests por terminal (gradlew/compile)**. Haremos pequeños cambios y cuando haya que compilar, el asistente se detendrá y le pedirá a Manolo que lo haga desde Android Studio para verificar que no hay errores.
7. **Automatización:** Cuando sea recomendable usaremos scripts de Python.
8. **Git y Control de Versiones:** 
   - Después de que Manolo apruebe un cambio, se realizará un **commit extenso en español** detallando minuciosamente todo lo realizado y modificado.
   - Si es apropiado para aislar nuevas funcionalidades complejas, se creará una **nueva rama**.
   - **Absolutamente prohibido** escribir de forma directa en la rama `master` o 'main'.
   - **Absolutamente prohibido** borrar cualquier rama del repositorio bajo ninguna circunstancia.
9. **Preguntas y Clarificaciones Obligatorias:** El asistente tiene la obligación de hacer a Manolo todas las preguntas que sean necesarias para conocer con absoluta precisión y exactitud qué es lo que quiere, cómo lo quiere y con qué detalles de diseño o comportamiento, antes de tomar decisiones por su cuenta o hacer suposiciones. Las preguntas deben presentarse SIEMPRE mediante la herramienta interactiva de preguntas con opciones claras donde Manolo solo tenga que seleccionar una alternativa o escribir su respuesta personalizada.

### 5. Todos los .md que crees los guardas en app\md


### 6. Diseño responsivo para adaptarse a todas las pantallas y a giros horizontal/vertical de los dispositivos.

### 7. La app será gratis y sin publicidad. Su objetivo es ampliar el conocimiento de los usuarios. No tiene ánimo de lucro.

### 9. El usuario ha de tener la opción de elegir entre varios temas.

### 10. Haremos un esfuerzo extraordinario para lograr la app más bella posible. 

### 11. La app ha de ser fluida y estable.

### 12. Optimización de Peso del APK y Ubicación de Recursos
- Todo lo utilizado para la generación offline de la app debe residir exclusivamente en la carpeta de la raíz.

### 13. Guarda siempre el progreso antes de parar para que la próxima vez que volvamos al proyecto sepamos exactamente por dónde hemos de continuar y qué llevamos hecho y qué hemos hecho en la última sesión. Esta información ha de ser lo suficientemente extensa para que no dé lugar a dudas.

### 14. Plan Maestro de Mejoras Obligatorio
- Se debe seguir estrictamente la hoja de ruta y las directrices diseñadas en `PLAN_MEJORAS.md` (guardado en la raíz y en `app\md\PLAN_MEJORAS.md`), respetando el orden por fases (Fase A, B, C, D, E) y las decisiones acordadas con Manolo.
