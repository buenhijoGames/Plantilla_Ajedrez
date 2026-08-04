#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Descarga y conversión de piezas de ajedrez cburnett (Lichess) a VectorDrawable.

Motivo:
  - La app de Manolo usa las piezas de lichess.org (cburnett, GPLv2+), tal y
    como indica Esta_App.md ("Los tableros y las piezas los cogeremos de
    lichess.org siempre que sea legal").
  - Los SVGs se descargan en su versión ORIGINAL del repositorio de Lichess
    (lichess-org/lila, carpeta public/piece/cburnett): no se copian ni se
    redibujan, se usan tal cual.
  - Android NO renderiza SVG nativo; para usarlas en Compose/Canvas las
    convertimos a VectorDrawable XML (android.graphics.drawable.VectorDrawable)
    preservando trazo, relleno, regla de relleno y terminaciones.
  - La conversión la hace este script una sola vez (offline). Los resultados
    se commitean en `app/src/main/res/drawable/`, así que en tiempo de build
    no hay dependencia de red (regla 12 de AGENTS.md: recursos offline en raíz).

Uso:
  python herramientas/descargar_piezas.py

Salida:
  app/src/main/res/drawable/pieza_{blanca|negra}_{rey|dama|torre|alfil|caballo|peon}.xml
"""

from __future__ import annotations

import os
import sys
import urllib.request
import xml.etree.ElementTree as ET

# ---------------------------------------------------------------------------
# Configuración
# ---------------------------------------------------------------------------

# Fuente oficial: SVGs de piezas de Lichess (cburnett, GPLv2+).
BASE_URL = "https://raw.githubusercontent.com/lichess-org/lila/master/public/piece/cburnett"

# (clave lichess, color_es, tipo_es)
PIEZAS = [
    ("wK", "blanca", "rey"),
    ("wQ", "blanca", "dama"),
    ("wR", "blanca", "torre"),
    ("wB", "blanca", "alfil"),
    ("wN", "blanca", "caballo"),
    ("wP", "blanca", "peon"),
    ("bK", "negra", "rey"),
    ("bQ", "negra", "dama"),
    ("bR", "negra", "torre"),
    ("bB", "negra", "alfil"),
    ("bN", "negra", "caballo"),
    ("bP", "negra", "peon"),
]

DESTINO = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "res", "drawable",
)

NS = {"svg": "http://www.w3.org/2000/svg"}

# ---------------------------------------------------------------------------
# Conversión SVG -> VectorDrawable
# ---------------------------------------------------------------------------


def convertir_svg_a_vector(svg_texto: str, nombre: str) -> str:
    """Convierte el contenido de un SVG de pieza a un XML VectorDrawable.

    Los SVG de cburnett (Lichess) tienen estructuras variadas que este
    conversor normaliza para que el resultado se vea EXACTAMENTE igual:

      - ``<path>`` directo bajo ``<svg>`` (peones) o bajo ``<g>`` (resto):
        se emite el propio elemento, no solo sus hijos.
      - ``<circle>`` (corona de la dama): VectorDrawable no lo soporta, se
        convierte a ``pathData`` con dos arcos.
      - Sin atributo ``fill``: en SVG el relleno por defecto es negro; en
        VectorDrawable no hay relleno por defecto. Si ninguna cadena de
        ``<g>``/``<path>`` define ``fill``, se usa negro (#000).
      - ``fill="none"`` -> sin fillColor (solo trazo).
      - ``fill-rule="evenodd"`` -> android:fillType="evenOdd" (API 24+).
      - ``stroke-linecap``/``stroke-linejoin`` -> android:strokeLineCap/Join.
    """
    raiz = ET.fromstring(svg_texto)

    viewbox = raiz.attrib.get("viewBox", "0 0 45 45")
    partes_vb = viewbox.split()
    ancho, alto = partes_vb[2], partes_vb[3]

    # Colección de paths finales:
    # (pathData, fill, stroke, strokeWidth, evenOdd, lineCap, lineJoin)
    paths: list[tuple[str, str | None, str | None, str | None, bool, str | None, str | None]] = []

    def normalizar_color(color: str) -> str:
        """Convierte '#rgb' / '#rrggbb' a '#AARRGGBB' (opaco)."""
        c = color.strip().lstrip("#")
        if len(c) == 3:
            c = "".join(ch * 2 for ch in c)
        if len(c) == 6:
            c = "FF" + c
        return "#" + c.upper()

    def resolver_fill(atributos: dict) -> str:
        """Relleno efectivo de un elemento.

        Si ninguna cadena (<g>/<path>) define `fill`, el estándar SVG usa
        negro; VectorDrawable no pinta nada sin fillColor, así que lo
        forzamos para no perder el cuerpo de las piezas negras.
        """
        return atributos.get("fill", "#000")

    def path_de_circulo(cx: str, cy: str, r: str) -> str:
        """Convierte un ``<circle>`` a pathData (dos arcos semicirculares)."""
        radio = float(r)
        x, y = float(cx), float(cy)
        return (
            f"M {x - radio} {y} a {radio} {radio} 0 1 0 {2 * radio} 0 "
            f"a {radio} {radio} 0 1 0 {-2 * radio} 0 z"
        )

    def emitir_path(d: str, atributos: dict) -> None:
        """Añade un path normalizado a la colección final."""
        fill = resolver_fill(atributos)
        stroke = atributos.get("stroke")
        ancho_trazo = atributos.get("stroke-width")
        evenodd = atributos.get("fill-rule") == "evenodd"
        fill_color = None
        if fill.strip().lower() != "none":
            fill_color = normalizar_color(fill)
        stroke_color = None
        if stroke and stroke.strip().lower() != "none":
            stroke_color = normalizar_color(stroke)
        cap = atributos.get("stroke-linecap")
        if cap not in ("round", "square"):
            cap = None
        join = atributos.get("stroke-linejoin")
        if join not in ("round", "bevel"):
            join = None
        paths.append((d, fill_color, stroke_color, ancho_trazo, evenodd, cap, join))

    def procesar_elemento(elem: ET.Element, atributos_heredados: dict) -> None:
        """Recorre el árbol propagando atributos y emitiendo shapes.

        Los atributos del elemento propio tienen prioridad sobre los
        heredados (p.ej. un ``<path>`` puede anular el ``fill`` del ``<g>``).
        Si el propio elemento es un ``<path>`` o ``<circle>`` se emite; en
        caso contrario se procesan sus hijos con los atributos acumulados.
        """
        actuales = dict(atributos_heredados)
        for k, v in elem.attrib.items():
            actuales[k] = v

        tag = elem.tag
        if tag == f"{{{NS['svg']}}}path":
            d = elem.attrib.get("d")
            if d:
                emitir_path(d, actuales)
            return
        if tag == f"{{{NS['svg']}}}circle":
            emitir_path(
                path_de_circulo(
                    elem.attrib.get("cx", "0"),
                    elem.attrib.get("cy", "0"),
                    elem.attrib.get("r", "0"),
                ),
                actuales,
            )
            return

        for hijo in elem:
            procesar_elemento(hijo, actuales)

    procesar_elemento(raiz, {})

    # Construcción del XML VectorDrawable.
    lineas = []
    lineas.append('<?xml version="1.0" encoding="utf-8"?>')
    lineas.append(
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"'
    )
    lineas.append(f'    android:width="{ancho}dp"')
    lineas.append(f'    android:height="{alto}dp"')
    lineas.append(f'    android:viewportWidth="{ancho}"')
    lineas.append(f'    android:viewportHeight="{alto}">')

    for d, fill, stroke, stroke_w, evenodd, cap, join in paths:
        lineas.append("  <path")
        lineas.append(f'    android:pathData="{d}"')
        if fill:
            lineas.append(f'    android:fillColor="{fill}"')
        if stroke:
            lineas.append(f'    android:strokeColor="{stroke}"')
            ancho_sw = stroke_w or "0"
            lineas.append(f'    android:strokeWidth="{ancho_sw}"')
        if cap:
            lineas.append(f'    android:strokeLineCap="{cap}"')
        if join:
            lineas.append(f'    android:strokeLineJoin="{join}"')
        if evenodd and fill:
            lineas.append('    android:fillType="evenOdd"')
        lineas.append("  />")

    lineas.append("</vector>")
    return "\n".join(lineas)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main() -> int:
    os.makedirs(DESTINO, exist_ok=True)
    exito = 0
    for clave, color, tipo in PIEZAS:
        url = f"{BASE_URL}/{clave}.svg"
        nombre_xml = f"pieza_{color}_{tipo}"
        ruta_xml = os.path.join(DESTINO, f"{nombre_xml}.xml")

        try:
            with urllib.request.urlopen(url, timeout=30) as resp:
                svg = resp.read().decode("utf-8")
        except Exception as err:
            print(f"[ERROR] No se pudo descargar {url}: {err}")
            continue

        try:
            vector = convertir_svg_a_vector(svg, nombre_xml)
        except Exception as err:
            print(f"[ERROR] Conversión de {clave} falló: {err}")
            continue

        with open(ruta_xml, "w", encoding="utf-8", newline="\n") as f:
            f.write(vector)
        print(f"[OK] {clave} -> {os.path.relpath(ruta_xml)}")
        exito += 1

    print(f"\n{exito}/{len(PIEZAS)} piezas convertidas en {DESTINO}")
    return 0 if exito == len(PIEZAS) else 1


if __name__ == "__main__":
    sys.exit(main())
