#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sintetizador acústico de efectos de sonido de ajedrez para buenhijoGames.

Genera archivos de audio WAV/PCM nativos de alta fidelidad:
  1. sonido_movimiento: Impacto de madera cálido y seco (pieza colocada sobre el tablero).
  2. sonido_captura: Doble golpe rápido y contundente de madera (retirada + colocación firme).
  3. sonido_jaque: Impacto percusivo con resonancia armónica de madera maciza.
  4. sonido_especial: Doble toque sincronizado con leve arrastre de madera para enroque y coronación.

Propiedad 100% original de buenhijoGames (sin dependencias externas ni licencias de terceros).
"""

from __future__ import annotations

import math
import os
import struct
import wave

SAMPLE_RATE = 44100
DESTINO = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "res", "raw"
)

def guardar_wav(ruta: str, samples: list[float]) -> None:
    """Escribe una lista de muestras normalizadas [-1.0, 1.0] a un archivo WAV de 16-bit PCM."""
    max_val = max(abs(s) for s in samples) if samples else 1.0
    if max_val > 0:
        samples = [s / max_val * 0.95 for s in samples]
    
    with wave.open(ruta, "w") as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(SAMPLE_RATE)
        
        frames = bytearray()
        for s in samples:
            val_int = int(s * 32767.0)
            val_int = max(-32768, min(32767, val_int))
            frames.extend(struct.pack("<h", val_int))
        wav_file.writeframes(frames)

def generar_ruido_rosa(n: int) -> list[float]:
    """Genera ruido filtrado para el cuerpo del impacto."""
    import random
    b0 = b1 = b2 = b3 = b4 = b5 = b6 = 0.0
    out = []
    for _ in range(n):
        white = random.uniform(-1.0, 1.0)
        b0 = 0.99886 * b0 + white * 0.0555179
        b1 = 0.99332 * b1 + white * 0.0750759
        b2 = 0.96900 * b2 + white * 0.1538520
        b3 = 0.86650 * b3 + white * 0.3104856
        b4 = 0.55000 * b4 + white * 0.5329522
        b5 = -0.7616 * b5 - white * 0.0168980
        out.append((b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11)
        b6 = white * 0.115926
    return out

def sintetizar_golpe_madera(duracion_ms: float, freq_base: float, decaimiento: float, brillo: float = 1.0) -> list[float]:
    """Sintetiza un golpe percusivo de pieza de madera con resonancia de cuerpo."""
    n_samples = int(SAMPLE_RATE * (duracion_ms / 1000.0))
    ruido = generar_ruido_rosa(n_samples)
    samples = []
    
    for i in range(n_samples):
        t = i / SAMPLE_RATE
        # Envolvente exponencial de decaimiento muy rápido para impacto de madera
        env = math.exp(-t * decaimiento)
        
        # Resonancias del cuerpo de madera (frecuencia fundamental + 2º y 3er armónico no lineal)
        onda1 = math.sin(2.0 * math.pi * freq_base * t)
        onda2 = 0.5 * math.sin(2.0 * math.pi * (freq_base * 1.85) * t)
        onda3 = 0.25 * math.sin(2.0 * math.pi * (freq_base * 2.7) * t)
        
        # Ruido de impacto inicial (primeros 5-10 ms)
        click = ruido[i] * math.exp(-t * 220.0) * brillo
        
        sample = (onda1 + onda2 + onda3) * env + click
        samples.append(sample)
    
    return samples

def generar_sonido_movimiento() -> list[float]:
    """Sonido de pieza colocada con suavidad en casilla de madera (70 ms, 320 Hz)."""
    return sintetizar_golpe_madera(duracion_ms=75.0, freq_base=320.0, decaimiento=48.0, brillo=1.2)

def generar_sonido_captura() -> list[float]:
    """Doble golpe de captura: retirada de pieza + colocación firme de la nueva (120 ms)."""
    golpe1 = sintetizar_golpe_madera(duracion_ms=50.0, freq_base=380.0, decaimiento=70.0, brillo=1.5)
    pausa = [0.0] * int(SAMPLE_RATE * 0.028) # 28 ms entre golpes
    golpe2 = sintetizar_golpe_madera(duracion_ms=80.0, freq_base=290.0, decaimiento=42.0, brillo=1.3)
    return golpe1 + pausa + golpe2

def generar_sonido_jaque() -> list[float]:
    """Golpe contundente con resonancia profunda y cuerpo de tablero hueco (140 ms, 230 Hz)."""
    n_samples = int(SAMPLE_RATE * 0.14)
    ruido = generar_ruido_rosa(n_samples)
    samples = []
    
    for i in range(n_samples):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 32.0)
        
        # Resonancia de advertencia de jaque (más rica y grave)
        onda1 = math.sin(2.0 * math.pi * 230.0 * t)
        onda2 = 0.6 * math.sin(2.0 * math.pi * 345.0 * t)
        onda3 = 0.35 * math.sin(2.0 * math.pi * 560.0 * t)
        onda4 = 0.2 * math.sin(2.0 * math.pi * 820.0 * t)
        
        click = ruido[i] * math.exp(-t * 180.0) * 1.4
        samples.append((onda1 + onda2 + onda3 + onda4) * env + click)
        
    return samples

def generar_sonido_especial() -> list[float]:
    """Enroque / Coronación: doble toque armónico coordinado y suave (130 ms)."""
    toque1 = sintetizar_golpe_madera(duracion_ms=55.0, freq_base=360.0, decaimiento=60.0, brillo=1.0)
    pausa = [0.0] * int(SAMPLE_RATE * 0.032) # 32 ms
    toque2 = sintetizar_golpe_madera(duracion_ms=75.0, freq_base=440.0, decaimiento=50.0, brillo=1.1)
    return toque1 + pausa + toque2

def main() -> int:
    os.makedirs(DESTINO, exist_ok=True)
    
    sonidos = [
        ("sonido_movimiento.mp3", generar_sonido_movimiento()),
        ("sonido_captura.mp3", generar_sonido_captura()),
        ("sonido_jaque.mp3", generar_sonido_jaque()),
        ("sonido_especial.mp3", generar_sonido_especial()),
    ]
    
    for nombre, samples in sonidos:
        ruta = os.path.join(DESTINO, nombre)
        guardar_wav(ruta, samples)
        print(f"[OK] Generado e instalado: {nombre} ({len(samples)} muestras)")
    
    print("\n4/4 efectos de sonido sintetizados y colocados con exito en app/src/main/res/raw/")
    return 0

if __name__ == "__main__":
    main()
