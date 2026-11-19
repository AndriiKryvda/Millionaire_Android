#!/usr/bin/env python3
"""Generates the launcher icon of "Хто хоче стати тисячником?".

The geometry below is the single source of truth. It is emitted twice:
  * as Android adaptive-icon vectors -> app/src/main/res/drawable/ic_launcher_*.xml
  * as PNG bitmaps -> tools/icon/*.png (Play Store 512 px graphic + size previews)

Run:  python tools/build_icon.py
Needs: Pillow only (no network, no external assets - everything is drawn here).
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES_DIR = ROOT / "app" / "src" / "main" / "res"
PNG_DIR = ROOT / "tools" / "icon"

VP = 108.0  # adaptive icon viewport in dp; the round/square mask keeps the central 66 dp

# --- palette (mirrors ui/theme/Color.kt) -------------------------------------
BG_CORE = "#3A2E8F"      # stage spotlight in the middle of the background
BG_MID = "#171256"
BG_EDGE = "#06062B"      # corners / edges
RING = "#7B6BF0"         # faint studio light rings
GLOW = "#F5C542"         # warm glow behind the plate
RIM_TOP, RIM_MID, RIM_BOTTOM = "#FFE7A0", "#F5C542", "#C98F16"
PLATE_TOP, PLATE_MID, PLATE_BOTTOM = "#3A52B8", "#24368F", "#16246A"
PLATE_EDGE = "#6B84E0"   # inner glass edge of the plate
GOLD = "#FFE08A"         # hryvnia sign
GOLD_SHADOW = "#C98F16"  # engraved edge of the hryvnia sign

CX, CY = 54.0, 54.0
HEX_R_OUT, HEX_R_IN = 34.0, 29.0
HEX_ROUND_OUT, HEX_ROUND_IN = 7.6, 6.4

# The hryvnia sign (U+04B4) drawn from scratch: an "S"-like curve (the cursive "гр"
# ligature the real sign comes from) crossed by two horizontal bars. Each stroke is a
# centre line; the final shapes are the thick round-capped outlines around them.
GLYPH_CURVE_W, GLYPH_BAR_W = 6.0, 4.2
GLYPH_CENTRES = [
    ([("M", 42.0, 46.0),
      ("C", 42.0, 35.0, 66.0, 35.0, 66.0, 46.0),
      ("C", 66.0, 54.5, 42.0, 53.5, 42.0, 62.0),
      ("C", 42.0, 73.0, 66.0, 73.0, 66.0, 62.0)], GLYPH_CURVE_W),
    ([("M", 38.0, 49.0), ("L", 70.0, 49.0)], GLYPH_BAR_W),
    ([("M", 38.0, 59.0), ("L", 70.0, 59.0)], GLYPH_BAR_W),
]
# Light on top, deeper gold underneath (plain #RRGGBB: the raster side reads these
# as RGB, Android accepts #RRGGBB for gradient items too).
GLYPH_STOPS = [(0, "#FFDC5E"), (0.5, "#FFD400"), (1, "#C88A00")]
# The sign is sized for the plate, then nudged up a touch so it still reads at 48 px.
GLYPH_SCALE = 1.08


# --- small vector helpers ----------------------------------------------------
def fmt(value: float) -> str:
    text = f"{value:.2f}".rstrip("0").rstrip(".")
    return "0" if text in ("", "-0") else text


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1])


def norm(v):
    length = math.hypot(v[0], v[1]) or 1.0
    return (v[0] / length, v[1] / length)


def hexagon_points(cx: float, cy: float, r: float, rotation: float = 90.0):
    """Regular hexagon with a vertex pointing up (same silhouette as the answer plates)."""
    return [
        (cx + r * math.cos(math.radians(rotation + 60 * k)),
         cy - r * math.sin(math.radians(rotation + 60 * k)))
        for k in range(6)
    ]


def quad_point(p0, c, p1, t):
    mt = 1.0 - t
    return (mt * mt * p0[0] + 2 * mt * t * c[0] + t * t * p1[0],
            mt * mt * p0[1] + 2 * mt * t * c[1] + t * t * p1[1])


def cubic_point(p0, c0, c1, p1, t):
    mt = 1.0 - t
    return (mt ** 3 * p0[0] + 3 * mt * mt * t * c0[0] + 3 * mt * t * t * c1[0] + t ** 3 * p1[0],
            mt ** 3 * p0[1] + 3 * mt * mt * t * c0[1] + 3 * mt * t * t * c1[1] + t ** 3 * p1[1])


def rounded_polygon(points, radius, samples=10):
    """Polygon with rounded corners, as (path data list, densely sampled outline)."""
    path: list[tuple] = []
    outline: list[tuple] = []
    n = len(points)
    for i in range(n):
        prev_, corner, next_ = points[(i - 1) % n], points[i], points[(i + 1) % n]
        edge = min(radius, math.dist(corner, prev_) / 2, math.dist(corner, next_) / 2)
        start = (corner[0] + norm(sub(prev_, corner))[0] * edge,
                 corner[1] + norm(sub(prev_, corner))[1] * edge)
        end = (corner[0] + norm(sub(next_, corner))[0] * edge,
               corner[1] + norm(sub(next_, corner))[1] * edge)
        path.append(("M" if not path else "L", *start))
        path.append(("Q", *corner, *end))
        outline.append(start)
        outline.extend(quad_point(start, corner, end, s / samples) for s in range(1, samples + 1))
    path.append(("Z",))
    return path, outline


def sample_path(path, steps: int = 0):
    """Split a path list into dense polylines (one per subpath) for rasterising.

    `steps` (when given) fixes the sampling of every curve, which is what the
    outline builder wants: dense enough to look smooth, sparse enough to keep
    the generated pathData short.
    """
    lines: list[list[tuple]] = []
    current: list[tuple] = []
    start = (0.0, 0.0)
    for seg in path:
        kind = seg[0]
        if kind == "M":
            if len(current) > 1:
                lines.append(current)
            current = [(seg[1], seg[2])]
            start = current[0]
        elif kind == "L":
            current.append((seg[1], seg[2]))
        elif kind == "Q":
            p0 = current[-1]
            n = steps or 24
            current.extend(quad_point(p0, (seg[1], seg[2]), (seg[3], seg[4]), s / n)
                           for s in range(1, n + 1))
        elif kind == "C":
            p0 = current[-1]
            n = steps or 48
            current.extend(cubic_point(p0, (seg[1], seg[2]), (seg[3], seg[4]), (seg[5], seg[6]),
                                       s / n) for s in range(1, n + 1))
        elif kind == "Z":
            current.append(start)
    if len(current) > 1:
        lines.append(current)
    return lines


def path_data(path) -> str:
    parts = []
    for seg in path:
        args = " ".join(fmt(v) for v in seg[1:])
        parts.append(seg[0] + (f" {args}" if args else ""))
    return " ".join(parts)


def thick_stroke(line, width: float):
    """Closed round-capped outline of a thick stroke running through `line`.

    Filled instead of stroked: Pillow leaves jagged joints when stroking at this
    scale, and a plain filled path is the safest thing to hand to VectorDrawable.
    """
    half = width / 2.0
    n = len(line)
    left: list[tuple[float, float]] = []
    right: list[tuple[float, float]] = []
    dirs: list[tuple[float, float]] = []
    for i, (x, y) in enumerate(line):
        px, py = line[max(i - 1, 0)]
        qx, qy = line[min(i + 1, n - 1)]
        dx, dy = qx - px, qy - py
        length = math.hypot(dx, dy) or 1.0
        ux, uy = dx / length, dy / length
        dirs.append((ux, uy))
        left.append((x - uy * half, y + ux * half))
        right.append((x + uy * half, y - ux * half))

    def cap(center, from_, to, into):
        """Half circle around `center` from `from_` to `to`, bulging along `into`."""
        cx, cy = center
        a0 = math.atan2(from_[1] - cy, from_[0] - cx)
        want = math.atan2(into[1], into[0])
        sign = min((abs((a0 + s * math.pi / 2 - want + math.pi) % (2 * math.pi) - math.pi), s)
                   for s in (1.0, -1.0))[1]
        return [(cx + math.cos(a0 + sign * math.pi * k / 8) * half,
                 cy + math.sin(a0 + sign * math.pi * k / 8) * half) for k in range(1, 8)]

    # At the far end the cap bulges with the travel direction, at the near end against it.
    return (left + cap(line[-1], left[-1], right[-1], dirs[-1])
            + right[::-1] + cap(line[0], right[0], left[0], (-dirs[0][0], -dirs[0][1])))


def points_to_path(points):
    return ([("M", points[0][0], points[0][1])]
            + [("L", x, y) for x, y in points[1:]]
            + [("Z",)])


def glyph_shapes():
    """The hryvnia sign as filled outlines, ready for the canvas or the XML."""
    shapes = []
    for centre, width in GLYPH_CENTRES:
        straight = {seg[0] for seg in centre} == {"M", "L"}
        for line in sample_path(centre, steps=2 if straight else 36):
            grown = [(CX + (x - CX) * GLYPH_SCALE, CY + (y - CY) * GLYPH_SCALE) for x, y in line]
            shapes.append(thick_stroke(grown, width * GLYPH_SCALE))
    return shapes


GLYPH_SHAPES = glyph_shapes()
GLYPH_PATHS_XML = [points_to_path(shape) for shape in GLYPH_SHAPES]


def circle_path(cx: float, cy: float, r: float) -> str:
    return (f"M{fmt(cx - r)},{fmt(cy)} a{fmt(r)},{fmt(r)} 0 1 0 {fmt(2 * r)},0 "
            f"a{fmt(r)},{fmt(r)} 0 1 0 {fmt(-2 * r)},0")


# --- the icon geometry -------------------------------------------------------
HEX_OUT_PATH, HEX_OUT_OUTLINE = rounded_polygon(hexagon_points(CX, CY, HEX_R_OUT), HEX_ROUND_OUT)
HEX_IN_PATH, HEX_IN_OUTLINE = rounded_polygon(hexagon_points(CX, CY, HEX_R_IN), HEX_ROUND_IN)
HEX_EDGE_PATH, HEX_EDGE_OUTLINE = rounded_polygon(hexagon_points(CX, CY, HEX_R_IN - 1.1), 2.4)

RINGS = [(36.0, 0x30, 1.1), (46.0, 0x24, 1.0), (56.0, 0x18, 0.9)]


# --- Android adaptive icon (vector XML) --------------------------------------
VECTOR_HEAD = (
    '<?xml version="1.0" encoding="utf-8"?>\n'
    "<!-- Generated by tools/build_icon.py - do not edit by hand. -->\n"
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    xmlns:aapt="http://schemas.android.com/aapt"\n'
    '    android:width="108dp"\n'
    '    android:height="108dp"\n'
    '    android:viewportWidth="108"\n'
    '    android:viewportHeight="108">\n'
)


def with_alpha(color: str, alpha: int) -> str:
    """'#RRGGBB' + 0..255 -> '#AARRGGBB' (Android wants the alpha byte inside the hex)."""
    return f"#{alpha:02X}{color.lstrip('#')}"


def grad(kind: str, stops, indent: str = " " * 12, **coords) -> str:
    """An <aapt:attr> gradient block used as android:fillColor."""
    if kind == "radial":
        attrs = (f'android:type="radial" android:centerX="{fmt(coords["cx"])}" '
                 f'android:centerY="{fmt(coords["cy"])}" android:gradientRadius="{fmt(coords["r"])}"')
    else:
        attrs = (f'android:type="linear" android:startX="{fmt(coords["x0"])}" '
                 f'android:startY="{fmt(coords["y0"])}" android:endX="{fmt(coords["x1"])}" '
                 f'android:endY="{fmt(coords["y1"])}"')
    items = "".join(f'\n{indent}  <item android:offset="{o}" android:color="{c}"/>'
                    for o, c in stops)
    return (f'{indent[:-4]}<aapt:attr name="android:fillColor">\n{indent}<gradient {attrs}>{items}'
            f'\n{indent}</gradient>\n{indent[:-4]}</aapt:attr>\n')


def solid_path(data: str, color: str, stroke: float | None = None) -> str:
    if stroke is None:
        return f'    <path android:fillColor="{color}" android:pathData="{data}" />\n'
    return (f'    <path android:pathData="{data}" android:strokeColor="{color}"\n'
            f'        android:strokeWidth="{fmt(stroke)}" android:strokeLineCap="round"\n'
            f'        android:strokeLineJoin="round" />\n')


def gradient_path(data: str, kind: str, stops, stroke: float | None = None, **coords) -> str:
    attr = "android:strokeColor" if stroke is not None else "android:fillColor"
    extra = (f' android:strokeWidth="{fmt(stroke)}" android:strokeLineCap="round"'
             f' android:strokeLineJoin="round"' if stroke is not None else "")
    body = grad(kind, stops, **coords).replace('name="android:fillColor"', f'name="{attr}"')
    return f'    <path android:pathData="{data}">\n{body}    </path>\n'


def background_xml() -> str:
    out = [VECTOR_HEAD, "    <!-- stage: a spotlight over the dark studio -->\n",
           '    <path android:pathData="M0,0h108v108h-108z">\n',
           grad("radial", [(0, BG_CORE), (0.55, BG_MID), (1, BG_EDGE)],
                indent=" " * 16, cx=CX, cy=44.0, r=64.0),
           "    </path>\n",
           "    <!-- faint light rings of the studio set -->\n"]
    for radius, alpha, width in RINGS:
        out.append(solid_path(circle_path(CX, 44.0, radius), with_alpha(RING, alpha), width))
    out += ["    <!-- warm glow behind the plate -->\n",
            f'    <path android:pathData="{circle_path(CX, 52.0, 44.0)}">\n',
            grad("radial", [(0, with_alpha(GLOW, 0x40)), (0.6, with_alpha(GLOW, 0x1A)),
                            (1, with_alpha(GLOW, 0x00))],
                 indent=" " * 16, cx=CX, cy=52.0, r=44.0),
            "    </path>\n</vector>\n"]
    return "".join(out)


def foreground_xml(monochrome: bool = False) -> str:
    out = [VECTOR_HEAD]
    if monochrome:
        out.append("    <!-- themed icon: silhouette only, the launcher tints it -->\n")
        out.append(solid_path(path_data(HEX_OUT_PATH), "#FFFFFFFF", 3.0))
        out += [solid_path(path_data(path), "#FFFFFFFF") for path in GLYPH_PATHS_XML]
        out.append("</vector>\n")
        return "".join(out)

    out.append("    <!-- gold rim of the answer plate -->\n")
    out.append(gradient_path(path_data(HEX_OUT_PATH), "linear",
                             [(0, RIM_TOP), (0.55, RIM_MID), (1, RIM_BOTTOM)],
                             x0=CX, y0=CY - HEX_R_OUT, x1=CX, y1=CY + HEX_R_OUT))
    out.append("    <!-- the plate itself -->\n")
    out.append(gradient_path(path_data(HEX_IN_PATH), "linear",
                             [(0, PLATE_TOP), (0.5, PLATE_MID), (1, PLATE_BOTTOM)],
                             x0=CX, y0=CY - HEX_R_IN, x1=CX, y1=CY + HEX_R_IN))
    out.append("    <!-- glass edge -->\n")
    out.append(solid_path(path_data(HEX_EDGE_PATH), with_alpha(PLATE_EDGE, 0x66), 0.9))
    out.append("    <!-- hryvnia sign -->\n")
    for path in GLYPH_PATHS_XML:
        out.append(gradient_path(path_data(path), "linear", GLYPH_STOPS,
                                 x0=CX, y0=32.0, x1=CX, y1=76.0))
    out.append("</vector>\n")
    return "".join(out)



def mipmap_xml() -> str:
    """The <adaptive-icon> that ties the three layers together."""
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated by tools/build_icon.py - do not edit by hand. -->\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/ic_launcher_background" />\n'
            '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
            '</adaptive-icon>\n')


# --- rasterising the very same geometry (Pillow) -----------------------------
def rgb(color: str):
    """#rgb / #rrggbb / #rrggbbaa -> (r, g, b). Any alpha byte is ignored."""
    color = color.lstrip("#")
    if len(color) == 8:
        color = color[2:]
    if len(color) == 3:
        color = "".join(ch * 2 for ch in color)
    return tuple(int(color[i:i + 2], 16) for i in (0, 2, 4))


def _mix(stops, t: float):
    """Linear lookup in a list of (position, "#rrggbb") stops."""
    t = min(max(t, 0.0), 1.0)
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        if t <= p1:
            f = 0.0 if p1 == p0 else (t - p0) / (p1 - p0)
            a, b = rgb(c0), rgb(c1)
            return tuple(round(a[i] + (b[i] - a[i]) * f) for i in range(3))
    return rgb(stops[-1][1])


_GRADIENTS: dict = {}
RES = 192  # gradients are built once in viewport space, then scaled to the canvas


def _gradient_l(kind: str, coords):
    img = Image.new("L", (RES, RES))
    px = img.load()
    if kind == "radial":
        (cx, cy), radius = coords
        for y in range(RES):
            vy = (y + 0.5) * VP / RES
            for x in range(RES):
                d = math.hypot((x + 0.5) * VP / RES - cx, vy - cy) / radius
                px[x, y] = 0 if d <= 0 else (255 if d >= 1 else int(d * 255))
    else:
        start, end = coords
        dx, dy = end[0] - start[0], end[1] - start[1]
        length2 = dx * dx + dy * dy
        for y in range(RES):
            vy = (y + 0.5) * VP / RES
            for x in range(RES):
                t = (((x + 0.5) * VP / RES - start[0]) * dx + (vy - start[1]) * dy) / length2
                px[x, y] = 0 if t <= 0 else (255 if t >= 1 else int(t * 255))
    return img


def gradient_image(size: int, kind: str, coords, stops) -> Image.Image:
    key = (kind, coords, tuple(stops))
    if key not in _GRADIENTS:
        lut = {ch: [_mix(stops, i / 255)[ch] for i in range(256)] for ch in range(3)}
        g = _gradient_l(kind, coords)
        _GRADIENTS[key] = Image.merge("RGB", (g.point(lut[0]), g.point(lut[1]), g.point(lut[2])))
    return _GRADIENTS[key].resize((size, size), Image.BILINEAR)


def polygon_mask(size: int, outline, k: float) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).polygon([(x * k, y * k) for x, y in outline], fill=255)
    return mask


def stroke_mask(size: int, polylines, width: float, k: float, dy: float = 0.0) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    w = max(1, round(width * k))
    for line in polylines:
        pts = [(x * k, (y + dy) * k) for x, y in line]
        draw.line(pts, fill=255, width=w, joint="curve")
        r = w / 2 - 0.5
        for p in (pts[0], pts[-1]):  # round caps
            draw.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=255)
    return mask


def put(canvas: Image.Image, mask: Image.Image, fill) -> None:
    """Paint `fill` (an RGB(A) tuple or an RGB gradient image) through `mask` onto the canvas."""
    layer = (Image.new("RGBA", canvas.size, fill) if isinstance(fill, tuple)
             else fill.convert("RGBA"))
    layer.putalpha(mask)
    canvas.alpha_composite(layer)


def render_master(px: int, ss: int = 3, layer: str = "full") -> Image.Image:
    """layer: "full" (stage + plate), "background", "foreground", "monochrome"."""
    n = px * ss
    k = n / VP
    canvas = Image.new("RGBA", (n, n), (0, 0, 0, 0))

    if layer in ("full", "background"):
        put(canvas, Image.new("L", (n, n), 255),
            gradient_image(n, "radial", ((CX, 44.0), 64.0),
                           [(0, BG_CORE), (0.55, BG_MID), (1, BG_EDGE)]))
        rings = Image.new("RGBA", (n, n), (0, 0, 0, 0))
        draw = ImageDraw.Draw(rings)
        for radius, alpha, width in RINGS:
            draw.ellipse([(CX - radius) * k, (44.0 - radius) * k,
                          (CX + radius) * k, (44.0 + radius) * k],
                         outline=rgb(RING) + (alpha,), width=max(1, round(width * k)))
        canvas.alpha_composite(rings)
        glow_rgb = gradient_image(n, "radial", ((CX, 52.0), 44.0), [(0, GLOW), (1, GLOW)])
        put(canvas, glow_rgb.convert("L").point(lambda v: 0x40 - int(v * 0x40 / 255)), glow_rgb)

    if layer == "monochrome":
        mono = stroke_mask(n, [list(HEX_OUT_OUTLINE) + [HEX_OUT_OUTLINE[0]]], 3.0, k)
        draw = ImageDraw.Draw(mono)
        for shape in GLYPH_SHAPES:
            draw.polygon([(x * k, y * k) for x, y in shape], fill=255)
        put(canvas, mono, (255, 255, 255))
        return canvas.resize((px, px), Image.LANCZOS)

    if layer in ("full", "foreground"):
        put(canvas, polygon_mask(n, HEX_OUT_OUTLINE, k),
            gradient_image(n, "linear", ((CX, CY - HEX_R_OUT), (CX, CY + HEX_R_OUT)),
                           [(0, RIM_TOP), (0.55, RIM_MID), (1, RIM_BOTTOM)]))
        put(canvas, polygon_mask(n, HEX_IN_OUTLINE, k),
            gradient_image(n, "linear", ((CX, CY - HEX_R_IN), (CX, CY + HEX_R_IN)),
                           [(0, PLATE_TOP), (0.5, PLATE_MID), (1, PLATE_BOTTOM)]))
        edge = Image.new("RGBA", (n, n), (0, 0, 0, 0))
        pts = [(x * k, y * k) for x, y in HEX_EDGE_OUTLINE]
        ImageDraw.Draw(edge).line(pts + [pts[0]], fill=rgb(PLATE_EDGE) + (0x66,),
                                  width=max(1, round(0.9 * k)), joint="curve")
        canvas.alpha_composite(edge)
        glyph = Image.new("L", (n, n), 0)
        draw = ImageDraw.Draw(glyph)
        for shape in GLYPH_SHAPES:
            draw.polygon([(x * k, y * k) for x, y in shape], fill=255)
        put(canvas, glyph, gradient_image(n, "linear", ((CX, 32.0), (CX, 76.0)), GLYPH_STOPS))

    return canvas.resize((px, px), Image.LANCZOS)


def shape_mask(size: int, shape: str) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    if shape == "circle":
        draw.ellipse([0, 0, size - 1, size - 1], fill=255)
    elif shape == "squircle":
        draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=round(size * 0.28), fill=255)
    else:
        draw.rectangle([0, 0, size - 1, size - 1], fill=255)
    return mask


def masked(icon: Image.Image, shape: str) -> Image.Image:
    out = icon.copy()
    out.putalpha(Image.composite(icon.split()[3], Image.new("L", icon.size, 0),
                                 shape_mask(icon.size[0], shape)))
    return out


# --- previews ----------------------------------------------------------------
def wallpaper(size: int, kind: str) -> Image.Image:
    stops = ([(0, "#2E3A6E"), (1, "#0A0E22")] if kind == "dark"
             else [(0, "#F4F6FC"), (1, "#C7D1E8")])
    return gradient_image(size, "radial", ((54.0, 26.0), 82.0), stops).convert("RGBA")


def label():
    try:
        from PIL import ImageFont
        return ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf", 18)
    except Exception:  # pragma: no cover - fallback
        from PIL import ImageFont
        return ImageFont.load_default()


def build_previews() -> list[Path]:
    PNG_DIR.mkdir(parents=True, exist_ok=True)
    written: list[Path] = []

    def save(image: Image.Image, name: str):
        path = PNG_DIR / name
        image.save(path)
        written.append(path)

    master = render_master(512, ss=2)
    save(master, "ic_launcher_512.png")
    save(masked(master, "circle"), "ic_launcher_circle_512.png")
    save(masked(master, "squircle"), "ic_launcher_squircle_512.png")

    # Safe-zone proof: outer 108 dp, the 72 dp that always stays visible and the
    # 66 dp "keep everything important inside" circle.
    proof = master.copy()
    draw = ImageDraw.Draw(proof)
    for dp, colour in ((108, (255, 255, 255, 90)), (72, (0, 230, 180, 170)), (66, (255, 90, 90, 220))):
        r = dp / 108 * 256
        draw.ellipse([256 - r, 256 - r, 256 + r, 256 + r], outline=colour, width=2)
    save(proof, "safe_zone.png")

    # The three adaptive layers side by side.
    cell = 256
    strip = Image.new("RGBA", (cell * 3 + 48, cell + 44), (38, 38, 46, 255))
    for i, (layer, name) in enumerate((("background", "background"), ("foreground", "foreground"),
                                       ("monochrome", "monochrome"))):
        art = render_master(cell, layer=layer)
        if layer != "background":
            plate = Image.new("RGBA", (cell, cell), (128, 128, 138, 255))
            plate.alpha_composite(art)
            art = plate
        strip.alpha_composite(art, (12 + i * (cell + 12), 12))
        ImageDraw.Draw(strip).text((16 + i * (cell + 12), cell + 16), name, fill=(230, 230, 240, 255),
                                   font=label())
    save(strip, "layers.png")

    # How it reads at real launcher sizes on a dark and a light wallpaper.
    sizes = [48, 72, 96, 144, 192]
    pad, top = 26, 30
    width = sum(sizes) + pad * (len(sizes) + 1)
    for kind, shape in (("dark", "circle"), ("light", "squircle")):
        back = wallpaper(width, kind)
        draw = ImageDraw.Draw(back)
        x = pad
        for size in sizes:
            back.alpha_composite(masked(master.resize((size, size), Image.LANCZOS), shape),
                                 (x, top))
            draw.text((x, top + size + 8), f"{size} px", fill=(150, 162, 196, 255), font=label())
            x += size + pad
        save(back, f"preview_{kind}_{shape}.png")
    return written


def main() -> None:
    drawable = RES_DIR / "drawable"
    mipmap = RES_DIR / "mipmap-anydpi-v26"
    drawable.mkdir(parents=True, exist_ok=True)
    mipmap.mkdir(parents=True, exist_ok=True)
    adaptive = mipmap_xml()
    xml_files = {
        drawable / "ic_launcher_background.xml": background_xml(),
        drawable / "ic_launcher_foreground.xml": foreground_xml(),
        drawable / "ic_launcher_monochrome.xml": foreground_xml(monochrome=True),
        mipmap / "ic_launcher.xml": adaptive,
        mipmap / "ic_launcher_round.xml": adaptive,
    }
    for path, text in xml_files.items():
        path.write_text(text, encoding="utf-8", newline="\n")
        print(f"wrote {path.relative_to(ROOT)} ({len(text)} chars)")
    for path in build_previews():
        print(f"wrote {path.relative_to(ROOT)}")


if __name__ == "__main__":
    main()


