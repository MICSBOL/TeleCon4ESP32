#!/usr/bin/env python3
"""Crop smart-light ON thumbnails from the devices mockup and derive OFF variants."""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageEnhance, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
MOCKUP_PATH = (
    ROOT / "design/smart-lighting-controller/smart_lighting_controller_07_devices_settings.png"
)
OUTPUT_DIR = ROOT / "app/src/main/res/drawable"
TARGET_SIZE = 168

# Thumbnail boxes on the 1536x1024 devices mockup (left image inside each connected card).
CROPS: dict[str, tuple[int, int, int, int]] = {
    "smart_light_living_room": (550, 370, 670, 490),
    "smart_light_kitchen_strip": (550, 450, 670, 570),
    "smart_light_bedroom_lamp": (550, 630, 670, 750),
}


def crop_thumbnail(source: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    cropped = source.crop(box).convert("RGB")
    if cropped.size != (TARGET_SIZE, TARGET_SIZE):
        cropped = cropped.resize((TARGET_SIZE, TARGET_SIZE), Image.Resampling.LANCZOS)
    return cropped


def glow_mask(rgb: np.ndarray) -> np.ndarray:
    r = rgb[..., 0].astype(np.float32)
    g = rgb[..., 1].astype(np.float32)
    b = rgb[..., 2].astype(np.float32)
    max_c = np.maximum(np.maximum(r, g), b)
    min_c = np.minimum(np.minimum(r, g), b)
    saturation = (max_c - min_c) / (max_c + 1.0)

    warm_glow = (max_c > 95.0) & (r >= g) & (r >= b) & (saturation > 0.12)
    cool_glow = (max_c > 90.0) & (b > r) & (b > g) & (saturation > 0.10)
    highlight = max_c > 175.0
    return warm_glow | cool_glow | highlight


def create_off_image(on_image: Image.Image) -> Image.Image:
    rgb = np.array(on_image.convert("RGB"), dtype=np.float32)
    mask = glow_mask(rgb)
    result = rgb.copy()

    result *= 0.58
    for channel in range(3):
        channel_data = result[..., channel]
        channel_data[mask] = channel_data[mask] * 0.28 + 18.0
        result[..., channel] = channel_data

    gray = result.mean(axis=2, keepdims=True)
    result = result * 0.72 + gray * 0.28

    off = Image.fromarray(np.clip(result, 0, 255).astype(np.uint8))
    off = ImageEnhance.Brightness(off).enhance(0.92)
    off = ImageEnhance.Color(off).enhance(0.55)
    off = off.filter(ImageFilter.GaussianBlur(radius=0.4))
    return off


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True, compress_level=9)


def main() -> None:
    if not MOCKUP_PATH.exists():
        raise SystemExit(f"Missing mockup: {MOCKUP_PATH}")

    source = Image.open(MOCKUP_PATH)
    total_bytes = 0

    for base_name, box in CROPS.items():
        on_image = crop_thumbnail(source, box)
        off_image = create_off_image(on_image)

        on_path = OUTPUT_DIR / f"{base_name}_on.png"
        off_path = OUTPUT_DIR / f"{base_name}_off.png"
        save_png(on_image, on_path)
        save_png(off_image, off_path)
        total_bytes += on_path.stat().st_size + off_path.stat().st_size
        print(f"Wrote {on_path.name} and {off_path.name} from box {box}")

    print(f"Output: {OUTPUT_DIR}")
    print(f"Thumbnail size: {TARGET_SIZE}x{TARGET_SIZE}px")
    print(f"Total size: {total_bytes / 1024:.1f} KB")


if __name__ == "__main__":
    main()
