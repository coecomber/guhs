"""
1.10.9 Guhwinter, slice hoentjes: het waterhoentje en het alpine hoentje (feature/guhwinter/hoentjes). They stay after the
event as rare, shy wild birds of the Guhmensie (water edges; high, cold or snowy ground), with a Guhdex bonus page each,
a NOWAY chat line when you see one, and a scene API for the kerstwandeling.

build(h) writes the models/animations/textures (guhwinter_hoentjes_modellen), the spawn egg icons + item models, sounds.json
(the OGGs are made once by guhwinter_hoentjes_geluid.py and committed), the loot tables (a feather, sometimes), the
seen_<page> advancements and all texts (Dutch in nl_nl; the English is in tools/lang/en/c480_guhwinter_hoentjes.json, which
_direct() writes, so a full make_resources run keeps it).

Standalone (no full regeneration; from the project root):  PYTHONPATH=tools python tools/features/guhwinter_hoentjes.py
"""
import json
import math
import os
import sys

from PIL import Image

from features import guhwinter_hoentjes_modellen as modellen

PAGINAS = ["guhwinter_waterhoen", "guhwinter_alpinehoen"]
SOUNDS = {
    "guhwinter_hoentjes.waterhoen_roep": ["waterhoen_roep1", "waterhoen_roep2", "waterhoen_roep3"],
    "guhwinter_hoentjes.waterhoen_piep": ["waterhoen_piep1", "waterhoen_piep2"],
    "guhwinter_hoentjes.alpinehoen_roep": ["alpinehoen_roep1", "alpinehoen_roep2", "alpinehoen_roep3"],
    "guhwinter_hoentjes.alpinehoen_piep": ["alpinehoen_piep1", "alpinehoen_piep2"],
    "guhwinter_hoentjes.noway": ["noway"],
    "guhwinter_hoentjes.noway_extreem": ["noway_extreem"],
}

# (Dutch, English)
TEKSTEN = {
    "entity.guhs.guhwinter_waterhoen": ("Waterhoentje", "Moorhen"),
    "entity.guhs.guhwinter_alpinehoen": ("Alpine hoentje", "Alpine Ptarmigan"),
    "item.guhs.guhwinter_waterhoen_spawn_egg": ("Waterhoentje-spawnei", "Moorhen Spawn Egg"),
    "item.guhs.guhwinter_alpinehoen_spawn_egg": ("Alpine-hoentje-spawnei", "Alpine Ptarmigan Spawn Egg"),
    # the NOWAY chat line (only you see it)
    "gui.guhs.guhwinter_hoentjes.noway.waterhoen": ("NOWAY EEN WATERHOEN!????", "NOWAY A MOORHEN!????"),
    "gui.guhs.guhwinter_hoentjes.noway.waterhoen.sub": ("(daar, bij het water! Kijk dan, njeg!)", "(there, by the water! Look, njeg!)"),
    "gui.guhs.guhwinter_hoentjes.noway.alpinehoen": ("NOWAYYY EEN ALPINE HOENTJE?!?!??!?!", "NOWAYYY AN ALPINE PTARMIGAN?!?!??!?!"),
    "gui.guhs.guhwinter_hoentjes.noway.alpinehoen.sub": ("(IN DE SNEEUW!! Niemand gaat dit geloven. VAHOEG!!)",
                                                         "(IN THE SNOW!! Nobody is going to believe this. VAHOEG!!)"),
    # sounds
    "subtitles.guhs.guhwinter_hoentjes.waterhoen_roep": ("Waterhoentje roept krr-uk", "Moorhen calls krr-uk"),
    "subtitles.guhs.guhwinter_hoentjes.waterhoen_piep": ("Waterhoentje piept", "Moorhen squeaks"),
    "subtitles.guhs.guhwinter_hoentjes.alpinehoen_roep": ("Alpine hoentje kakelt", "Ptarmigan croaks"),
    "subtitles.guhs.guhwinter_hoentjes.alpinehoen_piep": ("Alpine hoentje piept", "Ptarmigan squeaks"),
    "subtitles.guhs.guhwinter_hoentjes.noway": ("NOWAY!", "NOWAY!"),
    "subtitles.guhs.guhwinter_hoentjes.noway_extreem": ("NOWAYYY!!", "NOWAYYY!!"),
    # the Guhdex pages (bonus pages)
    "gui.guhs.guhdex.rarity.guhwinter_waterhoen": (
        "Zeldzaamheid: Zeldzaam (aan de waterkant in de Guhmensie: vijvers, meertjes en beekjes)",
        "Rarity: Rare (at the water's edge in the Guhmension: ponds, lakes and streams)"),
    "gui.guhs.guhdex.info.guhwinter_waterhoen": (
        "Een rond donker vogeltje met een rood schildje op zijn voorhoofd, een rood snaveltje met een geel puntje en hele lange "
        "groene tenen (met een rood kousenbandje!). Het knikt met zijn kopje bij elke stap, zwemt net zo graag als het loopt en "
        "wipt steeds met zijn witte staartje. Heel verlegen: sluip dichterbij. Krr-uk, njeg!",
        "A round dark little bird with a red shield on its forehead, a red bill with a yellow tip and very long green toes "
        "(with a red garter!). It nods its head with every step, likes swimming just as much as walking and keeps flicking "
        "its white tail. Very shy: sneak closer. Krr-uk, njeg!"),
    "gui.guhs.guhdex.rarity.guhwinter_alpinehoen": (
        "Zeldzaamheid: Zeldzaam (hoog in de bergen en in de sneeuw van de Guhmensie)",
        "Rarity: Rare (high up in the mountains and in the snow of the Guhmension)"),
    "gui.guhs.guhdex.info.guhwinter_alpinehoen": (
        "Een bolletje sneeuw met guhoortjes: in de winter helemaal wit, met een zwart lijntje bij zijn oogjes en een klein rood "
        "kammetje erboven. Zijn voetjes zijn dik bevederd, net sneeuwschoentjes. Het loopt niet, het huppelt! In de sneeuw zie "
        "je het bijna niet. Nog verlegener dan het waterhoentje.",
        "A little ball of snow with guh ears: all white in winter, with a black line by its eyes and a small red comb above "
        "them. Its feet are thickly feathered, just like snowshoes. It doesn't walk, it hops! In the snow you can hardly see "
        "it. Even shyer than the moorhen."),
}


# =====================================================================================================================
# the spawn eggs (painted here: the 1.21.1 tinted template is gone in 26.1)
# =====================================================================================================================
def ei(basis, vlekken, glans=(255, 255, 255)):
    """A 16x16 spawn egg: an egg shape (narrower on top) in `basis` with a dark rim, a shine top-left and `vlekken`
    [(colour, [(x, y), ...])] painted on."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rand = tuple(int(c * 0.55) for c in basis) + (255,)
    licht = tuple(min(255, int(c * 1.12) + 12) for c in basis) + (255,)
    donker = tuple(int(c * 0.82) for c in basis) + (255,)
    binnen = set()
    for y in range(16):
        for x in range(16):
            t = (y + 0.5 - 1) / 14                          # 0 top .. 1 bottom
            rx = 5.4 * (0.72 + 0.28 * math.sin(min(1.0, t * 1.15) * math.pi * 0.62 + 0.25))
            if ((x + 0.5 - 8) / rx) ** 2 + ((y + 0.5 - 8.6) / 6.9) ** 2 <= 1:
                binnen.add((x, y))
    for (x, y) in binnen:
        rim = any((x + dx, y + dy) not in binnen for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if rim:
            px[x, y] = rand
        elif x + y > 18 or (x >= 10 and y >= 9):
            px[x, y] = donker
        else:
            px[x, y] = tuple(basis) + (255,)
    for (x, y) in ((5, 4), (5, 5), (6, 3)):
        if (x, y) in binnen:
            px[x, y] = licht
    if (5, 4) in binnen:
        px[5, 4] = tuple(glans) + (255,)
    for kleur, punten in vlekken:
        for (x, y) in punten:
            if (x, y) in binnen:
                px[x, y] = tuple(kleur) + (255,)
    return img


def eieren(h):
    w = ei(modellen.SLATE, [(modellen.ROOD, [(7, 6), (8, 6), (7, 7), (9, 10), (10, 10), (5, 11)]), (modellen.GEEL, [(8, 7), (11, 7)]),
                            (modellen.WIT, [(4, 9), (12, 12)])])
    a = ei(modellen.SNEEUW, [(modellen.KAM, [(6, 6), (7, 6), (10, 9), (11, 9)]), (modellen.ZWART, [(8, 11), (9, 11), (5, 10), (7, 13)])],
           glans=(255, 255, 255))
    for name, img in (("guhwinter_waterhoen_spawn_egg", w), ("guhwinter_alpinehoen_spawn_egg", a)):
        h.save(img, "item", f"{name}.png")
        h.w(f"{h.A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{name}"}})
        h.w(f"{h.A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})


def sounds(h):
    def patch(d):
        for event, files in SOUNDS.items():
            d[event] = {"sounds": [f"guhs:guhwinter_hoentjes/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def loot(h):
    for p in PAGINAS:
        h.w(f"{h.D}/loot_table/entities/{p}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": "minecraft:feather", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}]})


def advancements(h):
    for p in PAGINAS:
        h.w(f"{h.D}/advancement/quest/seen_{p}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def texts(h):
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def selfcheck(h):
    problems = []
    for p in PAGINAS:
        for k in (f"entity.guhs.{p}", f"gui.guhs.guhdex.rarity.{p}", f"gui.guhs.guhdex.info.{p}", f"item.guhs.{p}_spawn_egg"):
            if k not in TEKSTEN:
                problems.append(f"missing text {k}")
    for event in SOUNDS:
        if f"subtitles.guhs.{event}" not in TEKSTEN:
            problems.append(f"missing subtitle of {event}")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
        if "mika" in nl.lower():
            problems.append(f"no Mika's in Guhwinter: {k}")
    for files in SOUNDS.values():
        for f in files:
            if not os.path.exists(os.path.join(h.A, "sounds", "guhwinter_hoentjes", f + ".ogg")):
                problems.append(f"missing sound guhwinter_hoentjes/{f}.ogg (run tools/features/guhwinter_hoentjes_geluid.py)")
    if problems:
        raise SystemExit("guhwinter_hoentjes self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    eieren(h)
    sounds(h)
    loot(h)
    advancements(h)
    texts(h)
    selfcheck(h)


def _direct():
    """Only this slice's outputs (no full make_resources run): its files, its lang keys in nl_nl/en_us, its English overlay."""
    sys.path.insert(0, "tools")
    import make_v2 as h   # (its helpers: w, save, lang, patch_json; importing it runs nothing)
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c480_guhwinter_hoentjes.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"guhwinter_hoentjes: {len(PAGINAS)} hoentjes, {len(SOUNDS)} sound events, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
