"""
1.10.0 "Het Vadsrijk", N-EXTRA: assets for the critters around the cheered-up (pink) Vahoegnir (Java: feature/vadsrijk/vriendjes/
DraakVriendjes). All critters reuse existing Guhs models/animations (pluisvinkje, kaasmeesje, pluiseekhoorntje, guh_konijntje,
pieppiepmuisje, knabbelvlindertje); only a pink pieppiepmuisje texture is generated here (the grey fur tinted pink, the rest kept).
Standalone (from the project root): python tools/features/vadsrijk_vriendjes.py
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "guhs", "textures")


def roze_muisje():
    im = Image.open(os.path.join(TEX, "entity", "pieppiepmuisje.png")).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if 0.09 < l < 0.5 and s < 0.45:   # dark plum fur + grey paws -> soft pink (eyes, belly, nose stay)
                l2 = min(0.86, 0.66 + (l - 0.16) * 0.9)
                nr, ng, nb = colorsys.hls_to_rgb(0.94, l2, 0.6)
                px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    os.makedirs(os.path.join(TEX, "vadsrijk", "vriendjes"), exist_ok=True)
    im.save(os.path.join(TEX, "vadsrijk", "vriendjes", "muisje_roze.png"))


def main():
    roze_muisje()
    print("vadsrijk_vriendjes: pink pieppiepmuisje texture")


if __name__ == "__main__":
    main()
