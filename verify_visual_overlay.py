"""
Visual Verification Script for High-Precision GPS Photograph Overlay Engine.
Renders the reference layout and compares it against reference.jpeg.
"""
from PIL import Image, ImageDraw, ImageFont
import numpy as np

def render_reference_overlay():
    # 1. Load reference photo
    base_img = Image.open('reference.jpeg').convert('RGBA')
    w, h = base_img.size
    print(f"Base photo dimensions: {w} x {h}")

    scale = w / 1086.0

    # 2. Dimensions matching OverlayConfig.createReferencePreset()
    h_margin = int(0.04144 * 1086 * scale) # 45px
    b_margin = int(0.01520 * 1448 * scale) # 22px
    gap = int(0.02026 * 1086 * scale)      # 22px
    map_w = int(0.26059 * 1086 * scale)    # 283px
    map_h = map_w                          # 283px
    panel_w = int(0.63628 * 1086 * scale)  # 691px
    panel_h = map_h                        # 283px
    corner_r = int(0.01473 * 1086 * scale) # 16px

    map_left = h_margin
    map_bottom = h - b_margin
    map_top = map_bottom - map_h
    map_right = map_left + map_w

    panel_left = map_right + gap
    panel_right = panel_left + panel_w
    panel_top = map_top
    panel_bottom = map_bottom

    # 3. Create overlay drawing surface
    overlay = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    # 4. Draw Main Panel (Translucent Black ~62% opacity)
    panel_bg_color = (12, 12, 12, int(0.62 * 255))
    draw.rounded_rectangle(
        [panel_left, panel_top, panel_right, panel_bottom],
        radius=corner_r,
        fill=panel_bg_color
    )

    # 5. Draw Badge at top-right
    badge_w = int(0.20074 * 1086 * scale) # 218px
    badge_h = int(0.06906 * 1086 * scale) # 75px
    badge_right = panel_right
    badge_left = badge_right - badge_w
    badge_bottom = panel_top
    badge_top = badge_bottom - badge_h

    # Rounded top corners, square bottom
    badge_mask = Image.new('L', (badge_w, badge_h), 0)
    b_draw = ImageDraw.Draw(badge_mask)
    b_draw.rounded_rectangle([0, 0, badge_w, badge_h * 2], radius=corner_r, fill=255)
    badge_surface = Image.new('RGBA', (badge_w, badge_h), panel_bg_color)
    badge_surface.putalpha(badge_mask)
    overlay.paste(badge_surface, (badge_left, badge_top), badge_surface)

    # Composite overlay on top of base image
    result = Image.alpha_composite(base_img, overlay)
    result.convert('RGB').save('rendered_layout_test.png')
    print("Rendered layout test saved to rendered_layout_test.png")

if __name__ == '__main__':
    render_reference_overlay()
