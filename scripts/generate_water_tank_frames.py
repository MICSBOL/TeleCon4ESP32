#!/usr/bin/env python3
"""Generate water-tank frames using level 075 as the golden master."""

from __future__ import annotations

import shutil
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
EMPTY_PATH = ROOT / "app/src/main/res/drawable/water_tank_empty.png"
FULL_PATH = ROOT / "app/src/main/res/drawable/water_tank_full.png"
MASTER_PATH = ROOT / "app/src/main/res/drawable/water_tank_level_075.png"
OUTPUT_DIR = ROOT / "app/src/main/res/drawable"

TARGET_HEIGHT = 480
MASTER_LEVEL = 75
SURFACE_BAND = 20


def remove_checkerboard(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            max_c = max(r, g, b)
            min_c = min(r, g, b)
            if (
                max_c - min_c < 18
                and 170 <= max_c <= 255
                and abs(r - g) < 12
                and abs(g - b) < 12
            ):
                pixels[x, y] = (r, g, b, 0)
    return rgba


def remove_background(image: Image.Image) -> Image.Image:
    rgba = remove_checkerboard(image)
    pixels = rgba.load()
    width, height = rgba.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if max(r, g, b) < 42:
                pixels[x, y] = (0, 0, 0, 0)
    return rgba


def crop_to_content(image: Image.Image, padding: int = 8) -> Image.Image:
    bbox = image.split()[3].getbbox()
    if bbox is None:
        return image
    return image.crop((
        max(0, bbox[0] - padding),
        max(0, bbox[1] - padding),
        min(image.width, bbox[2] + padding),
        min(image.height, bbox[3] + padding),
    ))


def resize_to_height(image: Image.Image, target_height: int) -> Image.Image:
    if image.height == target_height:
        return image
    ratio = target_height / image.height
    return image.resize(
        (max(1, round(image.width * ratio)), target_height),
        Image.Resampling.LANCZOS,
    )


def align_to_master(master: Image.Image, source: Image.Image) -> Image.Image:
    master_bbox = master.split()[3].getbbox()
    src_bbox = source.split()[3].getbbox()
    if master_bbox is None or src_bbox is None:
        return source.resize(master.size, Image.Resampling.LANCZOS)

    scale = (master_bbox[3] - master_bbox[1]) / max(1, src_bbox[3] - src_bbox[1])
    scaled = source.resize(
        (max(1, round(source.width * scale)), max(1, round(source.height * scale))),
        Image.Resampling.LANCZOS,
    )
    scaled_bbox = scaled.split()[3].getbbox()
    if scaled_bbox is None:
        return source.resize(master.size, Image.Resampling.LANCZOS)

    canvas = Image.new("RGBA", master.size, (0, 0, 0, 0))
    offset_x = (master.width - (scaled_bbox[2] - scaled_bbox[0])) // 2 - scaled_bbox[0]
    offset_y = master_bbox[3] - scaled_bbox[3]
    canvas.paste(scaled, (offset_x, offset_y), scaled)
    return canvas


def detect_glass_region(master: Image.Image, full: Image.Image) -> tuple[int, int, int, int]:
    width, height = master.size
    master_px = master.load()
    full_px = full.load()

    opaque_cols = [
        x for x in range(width)
        if sum(1 for y in range(height) if master_px[x, y][3] > 40) > height * 0.35
    ]
    if opaque_cols:
        x_left = opaque_cols[0] + max(3, int(width * 0.015))
        x_right = opaque_cols[-1] - max(3, int(width * 0.015))
    else:
        x_left, x_right = int(width * 0.18), int(width * 0.82)

    diff_rows: list[int] = []
    for y in range(height):
        hits = sum(
            1 for x in range(x_left, x_right)
            if master_px[x, y][3] >= 20 and full_px[x, y][3] >= 20
            and abs(full_px[x, y][0] - master_px[x, y][0])
            + abs(full_px[x, y][1] - master_px[x, y][1])
            + abs(full_px[x, y][2] - master_px[x, y][2]) > 30
        )
        if hits > (x_right - x_left) * 0.08:
            diff_rows.append(y)

    if diff_rows:
        y_top, y_bottom = diff_rows[0], diff_rows[-1]
    else:
        y_top, y_bottom = int(height * 0.14), int(height * 0.90)

    return x_left, x_right, max(y_top, int(height * 0.14)), min(y_bottom, int(height * 0.90))


def is_water_pixel(r: int, g: int, b: int) -> bool:
    return b > 90 and b > r + 15 and b > g + 5


def detect_surface_y(
    source: Image.Image,
    x_left: int,
    x_right: int,
    y_top: int,
    y_bottom: int,
) -> int:
    src_px = source.load()
    col_width = max(1, x_right - x_left)
    row_hits: dict[int, int] = {}

    for y in range(y_top, y_bottom + 1):
        hits = sum(
            1 for x in range(x_left, x_right)
            if src_px[x, y][3] >= 20 and is_water_pixel(*src_px[x, y][:3])
        )
        row_hits[y] = hits

    surface_y = y_top
    in_water = False
    for y in range(y_bottom, y_top - 1, -1):
        ratio = row_hits.get(y, 0) / col_width
        if ratio >= 0.40:
            in_water = True
            surface_y = y
        elif in_water and ratio < 0.22:
            surface_y = y + 1
            break

    band = 0
    while (
        surface_y > y_top
        and band < 16
        and row_hits.get(surface_y - 1, 0) / col_width >= 0.10
    ):
        surface_y -= 1
        band += 1

    return surface_y


def fill_y_for_level(y_top: int, y_bottom: int, level_percent: int) -> int:
    fillable = y_bottom - y_top
    return int(y_bottom - fillable * (level_percent / 100.0))


def extract_water_layer(
    source: Image.Image,
    empty: Image.Image,
    x_left: int,
    x_right: int,
    y_top: int,
    y_bottom: int,
) -> Image.Image:
    layer = Image.new("RGBA", source.size, (0, 0, 0, 0))
    src_px = source.load()
    empty_px = empty.load()
    out_px = layer.load()

    for y in range(y_top, y_bottom + 1):
        for x in range(x_left, x_right):
            sr, sg, sb, sa = src_px[x, y]
            if sa < 20:
                continue
            er, eg, eb, ea = empty_px[x, y]
            diff = abs(sr - er) + abs(sg - eg) + abs(sb - eb)
            if is_water_pixel(sr, sg, sb) or (diff > 28 and sb >= eb):
                out_px[x, y] = (sr, sg, sb, sa)

    return layer


def extract_glass_overlay(
    source: Image.Image,
    empty: Image.Image,
    water: Image.Image,
    x_left: int,
    x_right: int,
    y_top: int,
    y_bottom: int,
) -> Image.Image:
    layer = Image.new("RGBA", source.size, (0, 0, 0, 0))
    src_px = source.load()
    empty_px = empty.load()
    water_px = water.load()
    out_px = layer.load()

    for y in range(y_top, y_bottom + 1):
        for x in range(x_left, x_right):
            if water_px[x, y][3] >= 20:
                continue
            sr, sg, sb, sa = src_px[x, y]
            if sa < 20:
                continue
            er, eg, eb, ea = empty_px[x, y]
            diff = abs(sr - er) + abs(sg - eg) + abs(sb - eb)
            if diff > 18:
                out_px[x, y] = (sr, sg, sb, sa)

    return layer


def scale_water_strip(
    water: Image.Image,
    surface_y: int,
    y_bottom: int,
    target_surface_y: int,
) -> Image.Image:
    width, height = water.size
    strip_top = max(0, surface_y - SURFACE_BAND)
    strip = water.crop((0, strip_top, width, y_bottom))
    src_h = y_bottom - strip_top
    target_h = y_bottom - target_surface_y + SURFACE_BAND
    if target_h <= 0 or src_h <= 0:
        return Image.new("RGBA", water.size, (0, 0, 0, 0))

    scaled = strip.resize((width, target_h), Image.Resampling.LANCZOS)
    placed = Image.new("RGBA", water.size, (0, 0, 0, 0))
    placed.paste(scaled, (0, y_bottom - target_h), scaled)
    return placed


def composite_level(
    empty: Image.Image,
    master: Image.Image,
    full: Image.Image,
    master_water: Image.Image,
    full_water: Image.Image,
    master_glass: Image.Image,
    full_glass: Image.Image,
    master_surface_y: int,
    full_surface_y: int,
    x_left: int,
    x_right: int,
    y_top: int,
    y_bottom: int,
    level_percent: int,
) -> Image.Image:
    if level_percent <= 0:
        return empty.copy()
    if level_percent >= 100:
        return full.copy()
    if level_percent == MASTER_LEVEL:
        return master.copy()

    fill_y = fill_y_for_level(y_top, y_bottom, level_percent)
    if level_percent < MASTER_LEVEL:
        water = scale_water_strip(master_water, master_surface_y, y_bottom, fill_y)
        glass = scale_water_strip(master_glass, master_surface_y, y_bottom, fill_y)
    else:
        water = scale_water_strip(full_water, full_surface_y, y_bottom, fill_y)
        glass = scale_water_strip(full_glass, full_surface_y, y_bottom, fill_y)

    frame = empty.copy()
    frame.alpha_composite(water)
    frame.alpha_composite(glass)
    return frame


def save_png_optimized(image: Image.Image, path: Path) -> None:
    image.save(path, format="PNG", optimize=True, compress_level=9)


def main() -> None:
    if not all(p.exists() for p in (EMPTY_PATH, FULL_PATH, MASTER_PATH)):
        raise SystemExit("Missing source assets (empty, full, or master 075).")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    master = resize_to_height(
        crop_to_content(remove_background(Image.open(MASTER_PATH))),
        TARGET_HEIGHT,
    )
    empty = align_to_master(
        master,
        resize_to_height(
            crop_to_content(remove_background(Image.open(EMPTY_PATH))),
            TARGET_HEIGHT,
        ),
    )
    full = align_to_master(
        master,
        resize_to_height(
            crop_to_content(remove_background(Image.open(FULL_PATH))),
            TARGET_HEIGHT,
        ),
    )

    x_left, x_right, y_top, y_bottom = detect_glass_region(master, full)
    master_surface_y = detect_surface_y(master, x_left, x_right, y_top, y_bottom)
    full_surface_y = detect_surface_y(full, x_left, x_right, y_top, y_bottom)

    master_water = extract_water_layer(master, empty, x_left, x_right, y_top, y_bottom)
    full_water = extract_water_layer(full, empty, x_left, x_right, y_top, y_bottom)
    master_glass = extract_glass_overlay(
        master, empty, master_water, x_left, x_right, y_top, y_bottom,
    )
    full_glass = extract_glass_overlay(
        full, empty, full_water, x_left, x_right, y_top, y_bottom,
    )

    total_bytes = 0
    for level in range(0, 101):
        out_path = OUTPUT_DIR / f"water_tank_level_{level:03d}.png"
        if level == MASTER_LEVEL and MASTER_PATH.resolve() != out_path.resolve():
            shutil.copy2(MASTER_PATH, out_path)
        elif level == MASTER_LEVEL:
            pass  # already the golden master on disk
        else:
            frame = composite_level(
                empty,
                master,
                full,
                master_water,
                full_water,
                master_glass,
                full_glass,
                master_surface_y,
                full_surface_y,
                x_left,
                x_right,
                y_top,
                y_bottom,
                level,
            )
            save_png_optimized(frame, out_path)
        total_bytes += out_path.stat().st_size

    print(f"Generated 101 frames in {OUTPUT_DIR}")
    print(f"Frame size: {master.width}x{master.height}px")
    print(f"Golden master: level {MASTER_LEVEL} ({MASTER_PATH.name}) — copied verbatim")
    print(f"Glass: x={x_left}..{x_right}, y={y_top}..{y_bottom}")
    print(f"Master surface y={master_surface_y}, full surface y={full_surface_y}")
    print(f"Total size: {total_bytes / 1024 / 1024:.2f} MB")


if __name__ == "__main__":
    main()
