#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Descarga y conversión de piezas de ajedrez Meridian (CC0 / Dominio Público) a VectorDrawable.

Autor del set original: Martin Sedlak (kmar/chess_svg_piece_sets).
Licencia: CC0 1.0 Universal (Public Domain).
Permite uso sin restricciones en aplicaciones comerciales y de código cerrado.
"""

from __future__ import annotations

import os
import sys
import urllib.request
import xml.etree.ElementTree as ET

BASE_URL = "https://raw.githubusercontent.com/kmar/chess_svg_piece_sets/main/meridian"

PIEZAS = [
    ("wk", "blanca", "rey"),
    ("wq", "blanca", "dama"),
    ("wr", "blanca", "torre"),
    ("wb", "blanca", "alfil"),
    ("wn", "blanca", "caballo"),
    ("wp", "blanca", "peon"),
    ("bk", "negra", "rey"),
    ("bq", "negra", "dama"),
    ("br", "negra", "torre"),
    ("bb", "negra", "alfil"),
    ("bn", "negra", "caballo"),
    ("bp", "negra", "peon"),
]

DESTINOS = [
    os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "src", "main", "res", "drawable"),
    os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data", "src", "main", "res", "drawable"),
]

def parse_style(style_str: str) -> dict[str, str]:
    res: dict[str, str] = {}
    if not style_str:
        return res
    for item in style_str.split(";"):
        if ":" in item:
            k, v = item.split(":", 1)
            res[k.strip()] = v.strip()
    return res

def normalizar_color(color: str) -> str:
    c = color.strip().lstrip("#")
    if len(c) == 3:
        c = "".join(ch * 2 for ch in c)
    if len(c) == 6:
        c = "FF" + c
    return "#" + c.upper()

def convertir_svg_a_vector(svg_texto: str) -> str:
    raiz = ET.fromstring(svg_texto)
    viewbox = raiz.attrib.get("viewBox", "0 0 64 64")
    partes = viewbox.split()
    if len(partes) == 4:
        ancho, alto = partes[2], partes[3]
    else:
        ancho = raiz.attrib.get("width", "64").replace("px", "")
        alto = raiz.attrib.get("height", "64").replace("px", "")

    paths = []
    
    for elem in raiz.iter():
        tag = elem.tag
        if tag.endswith("path"):
            d = elem.attrib.get("d")
            if not d:
                continue
            st = parse_style(elem.attrib.get("style", ""))
            fill = st.get("fill", elem.attrib.get("fill", "#000"))
            stroke = st.get("stroke", elem.attrib.get("stroke"))
            stroke_w = st.get("stroke-width", elem.attrib.get("stroke-width"))
            cap = st.get("stroke-linecap", elem.attrib.get("stroke-linecap"))
            join = st.get("stroke-linejoin", elem.attrib.get("stroke-linejoin"))
            evenodd = (st.get("fill-rule") == "evenodd") or (elem.attrib.get("fill-rule") == "evenodd")
            
            fill_c = normalizar_color(fill) if fill and fill.lower() != "none" else None
            stroke_c = normalizar_color(stroke) if stroke and stroke.lower() != "none" else None
            paths.append((d, fill_c, stroke_c, stroke_w, evenodd, cap, join))

    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{ancho}dp"',
        f'    android:height="{alto}dp"',
        f'    android:viewportWidth="{ancho}"',
        f'    android:viewportHeight="{alto}">'
    ]
    for d, fill, stroke, sw, eo, cap, join in paths:
        lines.append("  <path")
        lines.append(f'    android:pathData="{d}"')
        if fill:
            lines.append(f'    android:fillColor="{fill}"')
        if stroke:
            lines.append(f'    android:strokeColor="{stroke}"')
            lines.append(f'    android:strokeWidth="{sw or "1"}"')
        if cap in ("round", "square"):
            lines.append(f'    android:strokeLineCap="{cap}"')
        if join in ("round", "bevel"):
            lines.append(f'    android:strokeLineJoin="{join}"')
        if eo and fill:
            lines.append('    android:fillType="evenOdd"')
        lines.append("  />")
    lines.append("</vector>")
    return "\n".join(lines)

def main() -> int:
    for d in DESTINOS:
        os.makedirs(d, exist_ok=True)
    
    exito = 0
    for clave, color, tipo in PIEZAS:
        url = f"{BASE_URL}/{clave}.svg"
        try:
            with urllib.request.urlopen(url, timeout=30) as resp:
                svg_text = resp.read().decode("utf-8")
        except Exception as e:
            print(f"[ERROR] No se pudo descargar {url}: {e}")
            continue

        try:
            xml_content = convertir_svg_a_vector(svg_text)
        except Exception as e:
            print(f"[ERROR] Fallo al convertir {clave}: {e}")
            continue

        nombre_archivo = f"pieza_{color}_{tipo}.xml"
        for destino in DESTINOS:
            ruta = os.path.join(destino, nombre_archivo)
            with open(ruta, "w", encoding="utf-8", newline="\n") as f:
                f.write(xml_content)
        print(f"[OK] {nombre_archivo}")
        exito += 1

    print(f"\n{exito}/{len(PIEZAS)} piezas CC0 instaladas con exito en :app y :data")
    return 0 if exito == len(PIEZAS) else 1

if __name__ == "__main__":
    sys.exit(main())
