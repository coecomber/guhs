"""
1.10 "Het Vadsrijk" zone 3 "De Schemerwortel", the z3-wereld slice (Z3_BRIEF.md). Java: feature/vadsrijk/zone3/wereld.

  - the zone-3 sky textures (Hoogvads, the floating city of the resistance, by day and its pink window lights at night; the thin
    grey streak of the Grauwe Einder);
  - every z3-wereld text (Dutch; en_us gets the same Dutch): area names (kern.Gebieden keys, also for the layered Gebied3 ids),
    the airship line to Honingtak and its opening, the lifts, the travel-map / captain lines for "schemerwortel", viewpoints.

The map itself: tools/vadsrijk/maak_kaart.py --zone 3 (tools/vadsrijk/zone3: tiles, Z3WereldPlekken.java, POI_Z3.md).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z3_wereld.py
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

A = os.path.join("src", "main", "resources")
AS = os.path.join(A, "assets", "guhs")
W = "vadsrijk.wereld."
Z = W + "z3."

TEKSTEN = {
    # areas (vadsrijk.kern.gebied.<id>; the layered Z3WereldPlekken.GEBIEDEN + the flat z3_ring)
    "vadsrijk.kern.gebied.z3_honingtak": "Honingtak",
    "vadsrijk.kern.gebied.z3_kruinzee": "De Kruinzee",
    "vadsrijk.kern.gebied.z3_takkenbanen": "De Takkenbanen",
    "vadsrijk.kern.gebied.z3_schorsstad": "De Schorsstad",
    "vadsrijk.kern.gebied.z3_stamspiraal": "Het Rijpad",
    "vadsrijk.kern.gebied.z3_wortelvelden": "De Wortelvelden",
    "vadsrijk.kern.gebied.z3_wortelduister": "Het Wortelduister",
    "vadsrijk.kern.gebied.z3_zeppelinwerf": "De Kaaszeppelinwerf",
    "vadsrijk.kern.gebied.z3_hart": "Het Hart van de Boom",
    "vadsrijk.kern.gebied.z3_ring": "De Schemerwortel",
    # airship line
    Z + "luchtschip.titel": "Honingtak",
    Z + "luchtschip.onder": "De Schemerwortel - lv 30-50",
    Z + "luchtschip.nog_niet": "De kapitein tuurt naar het oosten: zo ver vlieg ik pas als de Splinterpieken weer veilig zijn.",
    Z + "luchtschip.te_laag": "De kapitein fronst: in die boom wonen echte monsters. Kom terug op level %s.",
    Z + "opening.titel": "De Schemerwortel",
    Z + "opening.onder": "Een boom zo groot als een berg",
    Z + "opening.stem": "Hij leeft nog... maar niet lang meer.",
    Z + "opening.welkom": "Welkom op Honingtak! Mmm, het ruikt hier naar honing en bloesem. Imkermoeder Raatje wacht op het Bloesemplein.",
    # lifts
    Z + "lift.titel.honing": "Honinglift",
    Z + "lift.titel.honinglift": "De grote Honinglift",
    Z + "lift.titel.schorslift": "Boomvolklift",
    Z + "lift.omhoog": "Omhoog!",
    Z + "lift.omlaag": "Omlaag...",
    Z + "honinglift.dicht": "De grote Honinglift staat nog stil. Zodra je de weg door de stam kent, gaat hij open.",
    # viewpoints
    W + "uitkijk.kroon": "De Kroon",
    W + "uitkijk.takpunt": "Takpunt",
    W + "uitkijk.wortelgewelf": "Het Wortelgewelf",
    # travel map / captains (npc Reiskaart + QuestMotor)
    "vadsrijk.npc.optie.vlieg.schemerwortel": "Naar Honingtak (De Schemerwortel)",
    "vadsrijk.npc.optie.vlieg.schemerwortel.slot": "Naar Honingtak (nog op slot)",
    "vadsrijk.npc.reis.slot.schemerwortel_verhaal": "Die reuzenboom in het oosten? Daar vlieg ik pas heen als de Splinterpieken veilig zijn. Help eerst Kandijkom.",
    "vadsrijk.npc.reis.slot.schemerwortel_level": "In die boom wonen echte monsters, rekruut. Kom terug als je level 30 bent (nu level %s).",
    "vadsrijk.npc.reis.slot.schemerwortel_verhaal.kort": "Nog niet ontdekt. Maak eerst het verhaal van de Splinterpieken af.",
    "vadsrijk.npc.reis.slot.schemerwortel_level.kort": "Nog niet ontdekt. Vanaf level 30 (jij: level %s).",
    "vadsrijk.npc.reis.kies.z3_kapitein": "Terug naar beneden? Kies hieronder waarheen of bekijk de reiskaart. (De scheepsbel bij de mast opent de reiskaart ook.)",
}


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(img, naam):
    pad = os.path.join(AS, "textures", "vadsrijk", "wereld", naam)
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    img.save(pad)


def _hoogvads(nacht):
    """Hoogvads: a floating city on three cloud islands, towers with guh-ear roofs, bridges; at night only the window lights."""
    W_, H = 256, 160
    im = Image.new("RGBA", (W_, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rng = random.Random(311)
    eilanden = [(128, 104, 62), (58, 116, 34), (202, 112, 38)]
    ramen = []
    for (cx, cy, r) in eilanden:
        if not nacht:
            # the rock under the island (a hanging cone) and the cloud rim
            d.polygon([(cx - r, cy), (cx, cy + r * 0.9), (cx + r, cy)], fill=(176, 160, 196, 230))
            for k in range(7):
                ex = cx - r + k * r / 3.2
                d.ellipse((ex - 12, cy - 8, ex + 12, cy + 8), fill=(255, 238, 246, 235))
        # buildings
        n = max(3, r // 9)
        for k in range(n):
            bx = cx - r * 0.7 + k * (1.4 * r / max(1, n - 1))
            h = rng.randint(18, 44) + (26 if abs(bx - cx) < 10 and r > 50 else 0)
            w = rng.randint(6, 10)
            if not nacht:
                d.rectangle((bx - w, cy - h, bx + w, cy), fill=(236, 220, 236, 245))
                d.polygon([(bx - w - 2, cy - h), (bx, cy - h - 14), (bx + w + 2, cy - h)], fill=(240, 150, 190, 250))
                d.ellipse((bx - w - 3, cy - h - 18, bx - w + 3, cy - h - 10), fill=(240, 150, 190, 250))
                d.ellipse((bx + w - 3, cy - h - 18, bx + w + 3, cy - h - 10), fill=(240, 150, 190, 250))
            for j in range(2, h - 4, 7):
                ramen.append((bx + rng.choice((-3, 3)), cy - h + j))
    if not nacht:
        d.line([(92, 96), (66, 106)], fill=(200, 170, 150, 230), width=2)
        d.line([(164, 96), (190, 104)], fill=(200, 170, 150, 230), width=2)
        # the pink beacon on the highest tower
        d.ellipse((122, 18, 134, 30), fill=(255, 190, 220, 200))
    for (x, y) in ramen:
        kl = (255, 200, 120, 255) if nacht else (250, 210, 150, 220)
        d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=kl)
    if nacht:
        glow = im.filter(ImageFilter.GaussianBlur(3))
        im = Image.alpha_composite(glow, im)
        d2 = ImageDraw.Draw(im)
        d2.ellipse((116, 12, 140, 36), fill=(255, 150, 210, 160))
        return im.filter(ImageFilter.GaussianBlur(0.6))
    return im.filter(ImageFilter.GaussianBlur(0.8))


def _einder():
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for y in range(32):
        v = y / 31
        a = int(220 * math.exp(-((v - 0.7) / 0.18) ** 2))
        for x in range(64):
            im.putpixel((x, y), (255, 255, 255, a))
    return im


def textures():
    _png(_hoogvads(False), "z3_hoogvads.png")
    _png(_hoogvads(True), "z3_hoogvads_nacht.png")
    _png(_einder(), "z3_einder.png")


def build(h):
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)
    textures()


def _direct():
    textures()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(AS, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(TEKSTEN)
        _schrijf(p, d)
    print(f"vadsrijk_z3_wereld: {len(TEKSTEN)} texts, sky textures written")


if __name__ == "__main__":
    _direct()
