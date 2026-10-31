"""
1.8.1: the maagwand. Java: feature/maagwand. The client's Guhtong next to the inventory draws these two sheets:

  textures/gui/maagwand/tong.png    one 24x74 column per layer, three rows: the tongue, its plasma (full; cut off in Java), its rim
  textures/gui/maagwand/cellen.png  the guhlichaampjes, 7x7: one per layer, then the golden one

  build(h)    the textures, the quest advancements, the texts (Dutch in nl_nl, English in tools/lang/en/c300_maagwand.json)
  ftb(fq)     the quests per layer, in the chapter of the stomach

Standalone (from the project root): PYTHONPATH=tools python tools/features/maagwand.py
"""
import json
import math
import os
import sys

from PIL import Image

W, H, LAGEN = 24, 74, 5
CEL = 7
T = "gui.guhs.maagwand."

# (wand, rand, plooi, plasma, cel, celkern)
KLEUREN = [
    ((244, 176, 182), (186, 112, 128), (232, 150, 164), (252, 226, 220), (242, 150, 162), (255, 214, 216)),
    ((240, 150, 166), (170, 88, 112), (222, 120, 146), (246, 196, 196), (238, 118, 138), (255, 200, 204)),
    ((232, 124, 146), (156, 70, 98), (208, 96, 128), (216, 140, 150), (236, 96, 120), (255, 186, 192)),
    ((224, 96, 126), (138, 46, 78), (194, 66, 106), (178, 80, 104), (236, 70, 102), (255, 168, 176)),
    ((214, 64, 106), (118, 22, 58), (176, 36, 84), (142, 22, 62), (236, 44, 84), (255, 150, 160)),
]
GOUD = ((246, 196, 70), (255, 240, 170))

# key -> (Dutch, English)
TEKSTEN = {
    T + "boerp": ("BOERP! Je maag is nog niet resistent genoeg voor deze erg vadsige vads.",
                  "BURP! Your stomach isn't resistant enough yet for this very vadsy vads."),
    T + "eerst": ("Eet eerst nog %s keer %s om resistenter te worden. Deze vads vraagt om: %s.",
                  "First eat %s more of %s to grow more resistant. This vads needs: %s."),
    T + "eerst_leeg": ("Eet eerst wat meer vads van je eigen maag om resistenter te worden.",
                       "First eat more vads your stomach can handle to grow more resistant."),
    T + "of": (" of ", " or "),
    T + "gestegen": ("VAHOEG! Je maagwand is nu: %s! Bonus: %s.", "VAHOEG! Your stomach wall is now: %s! Bonus: %s."),
    T + "laag.1": ("Melkmaagje", "Milk Tummy"),
    T + "laag.2": ("Knabbelmaag", "Nibble Stomach"),
    T + "laag.3": ("Frituurmaag", "Fryer Stomach"),
    T + "laag.4": ("Saustaaie Maag", "Sauce-Tough Stomach"),
    T + "laag.5": ("Onverwoestbare Vadsmaag", "Indestructible Vads Stomach"),
    T + "bonus.2": ("je eet sneller", "you eat faster"),
    T + "bonus.3": ("vads vult beter", "vads fills you up better"),
    T + "bonus.4": ("geen hongerkramp meer van rotte vads", "no more hunger cramps from rotten vads"),
    T + "bonus.5": ("je boer duwt mobs weg, en de Vadsaura", "your burp pushes mobs away, plus the Vadsaura"),
    T + "tip.titel": ("%s (laag %s/%s)", "%s (layer %s/%s)"),
    T + "tip.teller": ("Guhlichaampjes: %s/%s", "Guh corpuscles: %s/%s"),
    T + "tip.eet": ("Eet: %s", "Eat: %s"),
    T + "tip.bonus": ("Bonus: %s", "Bonus: %s"),
    T + "tip.op_slot": ("Nog op slot:", "Still locked:"),
    T + "tip.alles": ("Alles mag! Geen vads te vadsig. VAHOEG!", "Everything goes! No vads too vadsy. VAHOEG!"),
}


def masker():
    m = [[False] * W for _ in range(H)]
    for y in range(H):
        for x in range(W):
            ry = max(0, y - (H - W * .55))
            top = max(0, 4 - y)
            m[y][x] = ((x - (W - 1) / 2) ** 2 + (ry * (W / 2) / (W * .55)) ** 2 <= (W / 2) ** 2) and x >= top * .7
    return m


def rand_van(m):
    r = [[False] * W for _ in range(H)]
    for y in range(H):
        for x in range(W):
            if m[y][x] and not all(0 <= x + a < W and 0 <= y + b < H and m[y + b][x + a] for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                r[y][x] = True
    return r


def tong_vel():
    import random
    vel = Image.new("RGBA", (W * LAGEN, H * 3), (0, 0, 0, 0))
    m, r = masker(), None
    r = rand_van(m)
    for l, (wand, rand, plooi, plasma, _, _) in enumerate(KLEUREN):
        rng = random.Random(5)
        ox = l * W
        papil = {(rng.randrange(1, W - 1), rng.randrange(1, H - 1)) for _ in range(22 + (l + 1) * 8)}
        for y in range(H):
            for x in range(W):
                if not m[y][x]:
                    continue
                vel.putpixel((ox + x, y), wand + (255,) if (x, y) not in papil else plooi + (255,))
                binnen = 2 <= x < W - 2 and y >= 6 and not r[y][x]
                if binnen:
                    vel.putpixel((ox + x, H + y), plasma + (255,))
                if r[y][x]:
                    vel.putpixel((ox + x, 2 * H + y), rand + (255,))
        groef = tuple(int(c * .85) for c in plooi) + (255,)
        for y in range(3, H - 7):
            x = W // 2 + int(math.sin(y * .3))
            if m[y][x]:
                vel.putpixel((ox + x, 2 * H + y), groef)
    return vel


def cel_vel():
    vel = Image.new("RGBA", (CEL * (LAGEN + 1), CEL), (0, 0, 0, 0))
    soorten = [(k[4], k[5]) for k in KLEUREN] + [GOUD]
    for i, (c, ck) in enumerate(soorten):
        donker = tuple(int(v * .68) for v in c) + (255,)
        c4, ck4 = c + (255,), ck + (255,)
        rij = [".#.#...", ".###...", "#ooo#..", "#oko#..", "#ooo#..", ".###...", "......."]
        for y, tekst in enumerate(rij):
            for x, ch in enumerate(tekst):
                kleur = {"#": donker, "o": c4, "k": ck4}.get(ch)
                if kleur:
                    vel.putpixel((i * CEL + x + 1, y), kleur)
        ogen = (40, 20, 30, 255)
        vel.putpixel((i * CEL + 2, 3), ogen)      # oogjes
        vel.putpixel((i * CEL + 4, 3), ogen)
    return vel


def bewaar(pad):
    os.makedirs(pad, exist_ok=True)
    tong_vel().save(os.path.join(pad, "tong.png"))
    cel_vel().save(os.path.join(pad, "cellen.png"))


def build(h):
    bewaar(os.path.join(h.A, "textures", "gui", "maagwand"))
    for laag in range(2, LAGEN + 1):
        h.w(f"{h.D}/advancement/quest/maagwand_laag{laag}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


# =====================================================================================================================
# FTB: in the chapter of your stomach, in their own section
# =====================================================================================================================
FTB_CHAPTER = "guhs_maag"
FTB_SECTION = "De maagwand"


def ftb(fq):
    q, adv = fq.q, fq.adv
    q("maagwand_laag2", "Knabbelmaag", "Open je inventaris: links hangt de &dGuhtong&r met je guhlichaampjes. Hover erover en je ziet wat je "
      "mag en wat nog op slot zit. Is vads te vadsig, dan volgt een BOERP. Niks kwijt! Eet 100 kaasknabbels, sate of kaasbrok "
      "en je maag wordt resistenter.", "guhs:kaas_knabbels", [adv("maagwand_laag2")], xp=100)
    q("maagwand_laag3", "Frituurmaag", "Gefrituurde kaasknabbels, gebakken guh-vis en kaasbolletjes: eet er 64 en je vads vult voortaan beter.",
      "guhs:gefrituurde_kaasknabbels", [adv("maagwand_laag3")], xp=150)
    q("maagwand_laag4", "Saustaaie Maag", "Nog vadsiger: 48 keer dubbel gefrituurd, gegrilde sate of een patatje. Daarna geen "
      "hongerkramp meer van rotte vads.", "guhs:gegrilde_kaasknabbelsate", [adv("maagwand_laag4")], xp=200)
    q("maagwand_laag5", "Onverwoestbare Vadsmaag", "Het allerzwaarste: 32 keer driedubbel gefrituurd, kaaskroket of Frikandel Guhspeciaal. "
      "Dan mag alles, je boer duwt mobs weg en je draagt de &6Vadsaura&r. VAHOEG!", "guhs:perfecte_knabbel", [adv("maagwand_laag5")], xp=300)


def _direct():
    a = os.path.join("src", "main", "resources")
    bewaar(os.path.join(a, "assets", "guhs", "textures", "gui", "maagwand"))
    adv = os.path.join(a, "data", "guhs", "advancement", "quest")
    for laag in range(2, LAGEN + 1):
        with open(os.path.join(adv, f"maagwand_laag{laag}.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"criteria": {"done": {"trigger": "minecraft:impossible"}}}, f, indent=2)
            f.write("\n")
    with open(os.path.join("tools", "lang", "en", "c300_maagwand.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"maagwand: two sheets, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
