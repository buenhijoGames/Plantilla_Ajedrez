#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Descarga y conversión precisa de piezas de ajedrez Meridian (CC0 / Dominio Público) a VectorDrawable.

Corrige transforms globales (p.ej. translate(-306,-264) en las damas) y procesa
tanto <path> como <ellipse> / <circle> para generar XMLs VectorDrawable válidos en Android.
"""

from __future__ import annotations

import os
import re
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

def ellipse_to_path(cx: float, cy: float, rx: float, ry: float) -> str:
    # 2 semi-elipses conectadas
    return (
        f"M {cx - rx:.4f},{cy:.4f} "
        f"A {rx:.4f},{ry:.4f} 0 1 0 {cx + rx:.4f},{cy:.4f} "
        f"A {rx:.4f},{ry:.4f} 0 1 0 {cx - rx:.4f},{cy:.4f} Z"
    )

def parse_translate(transform_str: str) -> tuple[float, float]:
    if not transform_str:
        return (0.0, 0.0)
    m = re.search(r"translate\(\s*([-\d.]+)\s*,\s*([-\d.]+)\s*\)", transform_str)
    if m:
        return (float(m.group(1)), float(m.group(2)))
    return (0.0, 0.0)

def convertir_svg_a_vector(svg_texto: str) -> str:
    raiz = ET.fromstring(svg_texto)
    viewbox = raiz.attrib.get("viewBox", "0 0 64 64")
    partes = viewbox.split()
    if len(partes) == 4:
        ancho, alto = partes[2], partes[3]
    else:
        ancho = raiz.attrib.get("width", "64").replace("px", "")
        alto = raiz.attrib.get("height", "64").replace("px", "")

    # Lista de elementos: (tipo, atributos, pathData o params_elipse, transform)
    items = []

    for elem in raiz.iter():
        tag = elem.tag.split("}")[-1]
        if tag not in ("path", "ellipse", "circle"):
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
        trans = elem.attrib.get("transform", "")
        tx, ty = parse_translate(trans)

        if tag == "path":
            d = elem.attrib.get("d", "")
            if not d:
                continue
            items.append(("path", fill_c, stroke_c, stroke_w, evenodd, cap, join, d, tx, ty))
        elif tag in ("ellipse", "circle"):
            cx = float(elem.attrib.get("cx", "0")) + tx
            cy = float(elem.attrib.get("cy", "0")) + ty
            rx = float(elem.attrib.get("rx", elem.attrib.get("r", "0")))
            ry = float(elem.attrib.get("ry", elem.attrib.get("r", "0")))
            d = ellipse_to_path(cx, cy, rx, ry)
            # Como ya sumamos tx y ty a cx y cy, el translate para este pathData es 0
            items.append(("path", fill_c, stroke_c, stroke_w, evenodd, cap, join, d, 0.0, 0.0))

    # Construir el XML agrupando por transform si es necesario
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{ancho}dp"',
        f'    android:height="{alto}dp"',
        f'    android:viewportWidth="{ancho}"',
        f'    android:viewportHeight="{alto}">'
    ]

    for _, fill, stroke, sw, eo, cap, join, d, tx, ty in items:
        need_group = (tx != 0.0 or ty != 0.0)
        if need_group:
            lines.append(f'  <group android:translateX="{tx}" android:translateY="{ty}">')
            indent = "    "
        else:
            indent = "  "

        lines.append(f"{indent}<path")
        lines.append(f'{indent}  android:pathData="{d}"')
        if fill:
            lines.append(f'{indent}  android:fillColor="{fill}"')
        if stroke:
            lines.append(f'{indent}  android:strokeColor="{stroke}"')
            lines.append(f'{indent}  android:strokeWidth="{sw or "1"}"')
        if cap in ("round", "square"):
            lines.append(f'{indent}  android:strokeLineCap="{cap}"')
        if join in ("round", "bevel"):
            lines.append(f'{indent}  android:strokeLineJoin="{join}"')
        if eo and fill:
            lines.append(f'{indent}  android:fillType="evenOdd"')
        lines.append(f"{indent}/>")

        if need_group:
            lines.append("  </group>")

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

    print(f"\n{exito}/{len(PIEZAS)} piezas CC0 instaladas con exito (incluye damas completas con coronas y traslaciones).")
    return 0 if exito == len(PIEZAS) else 1

if __name__ == "__main__":
    sys.exit(main())
