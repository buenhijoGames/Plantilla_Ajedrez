# ============================================================================
# consumer-rules.pro - módulo :data
# ----------------------------------------------------------------------------
# Reglas que :app consumirá automáticamente al depender de :data.
# Garantizan que las clases de :data involucradas con reflexión (entidades
# Room, DAOs, bindings Hilt, adaptadores chesslib) sobrevivan a R8.
# ============================================================================

# Mantener todas las entidades y DAOs de Room definidos en este módulo.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Mantener clases inyectadas por Hilt y bindings generados por el compilador.
-keep,allowobfuscation @dagger.Module class *
-keep,allowobfuscation @dagger.Provides class *
-keep,allowobfuscation @javax.inject.Inject class *
-keep,allowobfuscation @dagger.hilt.android.qualifiers.* class *

# chesslib: enums y PgnHolder con reflexión.
-keep class com.github.bhlangonijr.chesslib.** { *; }
-keep enum com.github.bhlangonijr.chesslib.** { *; }
-dontwarn com.github.bhlangonijr.chesslib.**