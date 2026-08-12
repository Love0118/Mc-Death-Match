#!/usr/bin/env python3
"""Rebuild semantic AK-47 reference and render sheets for review.

The generated reference objects cross the original equal-width panel dividers,
so fixed thirds cut the side rifle at the gas block. This script identifies the
three connected subjects before placing them on equal semantic canvases.
"""

from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parent
PANEL_SIZE = (840, 780)
KEY_COLOR = (39, 216, 34, 255)


def foreground_components(image: Image.Image) -> list[tuple[int, tuple[int, int, int, int]]]:
    alpha = image.getchannel("A")
    pixels = alpha.load()
    width, height = image.size
    seen: set[tuple[int, int]] = set()
    components: list[tuple[int, tuple[int, int, int, int]]] = []

    for y in range(height):
        for x in range(width):
            if pixels[x, y] < 16 or (x, y) in seen:
                continue
            queue = deque([(x, y)])
            seen.add((x, y))
            points: list[tuple[int, int]] = []
            while queue:
                current_x, current_y = queue.popleft()
                points.append((current_x, current_y))
                for neighbor in (
                    (current_x - 1, current_y),
                    (current_x + 1, current_y),
                    (current_x, current_y - 1),
                    (current_x, current_y + 1),
                ):
                    neighbor_x, neighbor_y = neighbor
                    if (
                        0 <= neighbor_x < width
                        and 0 <= neighbor_y < height
                        and pixels[neighbor_x, neighbor_y] >= 16
                        and neighbor not in seen
                    ):
                        seen.add(neighbor)
                        queue.append(neighbor)
            if len(points) < 1000:
                continue
            xs = [point[0] for point in points]
            ys = [point[1] for point in points]
            components.append(
                (len(points), (min(xs), min(ys), max(xs) + 1, max(ys) + 1))
            )
    return sorted(components, reverse=True)


def centered_panel(subject: Image.Image) -> Image.Image:
    panel = Image.new("RGBA", PANEL_SIZE, (0, 0, 0, 0))
    panel.alpha_composite(
        subject,
        ((PANEL_SIZE[0] - subject.width) // 2, (PANEL_SIZE[1] - subject.height) // 2),
    )
    return panel


def write_reference_sheet() -> None:
    source = Image.open(ROOT / "reference_multiview_alpha.png").convert("RGBA")
    components = foreground_components(source)
    if len(components) != 3:
        raise ValueError(f"expected three reference subjects, found {len(components)}")

    # Pixel area orders the generated objects as side, top, muzzle.
    semantic = {
        name: centered_panel(source.crop(box))
        for name, (_, box) in zip(("side", "top", "muzzle"), components)
    }
    sheet = Image.new("RGBA", (PANEL_SIZE[0] * 3, PANEL_SIZE[1]), (0, 0, 0, 0))
    for index, name in enumerate(("muzzle", "side", "top")):
        sheet.alpha_composite(semantic[name], (index * PANEL_SIZE[0], 0))
    sheet.save(ROOT / "reference_multiview_axis_order.png")

    chroma = Image.new("RGBA", sheet.size, KEY_COLOR)
    chroma.alpha_composite(sheet)
    chroma.convert("RGB").save(ROOT / "reference_multiview_axis_order_chroma.png")


def write_render_sheet() -> None:
    source = Image.open(ROOT / "preview" / "ak47_comparison_turntable.png").convert("RGBA")
    if source.size != (2160, 470):
        raise ValueError(f"unexpected compiler comparison sheet size: {source.size}")
    panels = [source.crop((index * 720, 0, (index + 1) * 720, 470)) for index in range(3)]

    top_box = panels[2].getchannel("A").getbbox()
    if top_box is None:
        raise ValueError("top render has no foreground")
    top_subject = panels[2].crop(top_box).transpose(Image.Transpose.ROTATE_270)
    semantic_top = Image.new("RGBA", panels[2].size, (0, 0, 0, 0))
    semantic_top.alpha_composite(
        top_subject,
        ((semantic_top.width - top_subject.width) // 2, (semantic_top.height - top_subject.height) // 2),
    )
    panels[2] = semantic_top

    sheet = Image.new("RGBA", source.size, (0, 0, 0, 0))
    for index, panel in enumerate(panels):
        sheet.alpha_composite(panel, (index * 720, 0))
    sheet.save(ROOT / "preview" / "ak47_comparison_turntable_semantic.png")


if __name__ == "__main__":
    write_reference_sheet()
    write_render_sheet()
    print("rebuilt semantic reference and render sheets")
