"""
1.8.0 (vadskracht theme, ontwerp-guhs180-vadskracht.md V3/V4, dromen-ontwerp/BOUWPLAN_VADS.md): De Mega-knuffelcentrale
and the export trades of the guh villagers. Java: feature/centrale (CentraleFeature, MegaKnuffelcentraleBlock(Entity),
CentraleGetallen, Export, CentraleGameTests).

  build(h)    textures (a quilted pink cushion, the kern with an embroidered guh face per snoet), models, blockstates,
              loot table, recipe, the hidden + visible advancements, texts (Dutch in nl_nl, English in
              tools/lang/en/c217_centrale.json), plus a self-check
  ftb(fq)     the FTB section "De Mega-knuffelcentrale" in Guh-technologie (make_ftbquests.py MODULES["centrale"])

Standalone (from the project root): PYTHONPATH=tools python tools/features/centrale.py
"""
import json
import os
import re
import sys

import numpy as np
from PIL import Image

JAVA_GETALLEN = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "centrale", "CentraleGetallen.java")


def getal(naam):
    """A number of CentraleGetallen (the texts never drift from the code)."""
    with open(JAVA_GETALLEN, encoding="utf-8") as f:
        m = re.search(rf"\b{naam}\s*=\s*([\d_]+)", f.read())
    return int(m.group(1).replace("_", ""))


MAX, PER, HONGER, DEKENS = getal("MAX_GUHS"), getal("PER_GUH"), getal("HONGER_PROCENT"), getal("GEEN_DEKENS_PROCENT")
VOER_S = getal("VOER_SECONDEN")

# key -> (Dutch, English)
TEKSTEN = {
    "block.guhs.mega_knuffelcentrale": ("Mega-knuffelcentrale", "Mega Cuddle Plant"),
    "block.guhs.mega_knuffelcentrale.lore": (
        f"Een reuzenkussen van 5 bij 3 waar {MAX} tamme guhs samen knuffelen: {PER} vadskracht per guh, tot {MAX * PER} in totaal. "
        f"Wel goed verzorgen: knabbels, roze broodjes of kaasknabbelblokken, en elke dag één wol voor verse dekentjes. Eén per net.",
        f"A giant 5 by 3 cushion where {MAX} tamed guhs cuddle together: {PER} vadskracht per guh, up to {MAX * PER} in all. "
        f"Do look after them: knabbels, pink buns or blocks of kaasknabbels, and one wool a day for fresh blankets. One per net."),
    "block.guhs.mega_knuffelcentrale_deel": ("Mega-knuffelcentrale", "Mega Cuddle Plant"),
    "gui.guhs.vadskracht.soort.mega_knuffelcentrale": ("Mega-knuffelcentrales", "Mega Cuddle Plants"),
    "gui.guhs.vadskracht.soort.mega_knuffelcentrale.enkel": ("Mega-knuffelcentrale", "Mega Cuddle Plant"),
    "gui.guhs.centrale.leeg": ("Nog geen guh op het reuzenkussen: tamme guhs binnen %s blokken komen vanzelf",
                               "No guh on the giant cushion yet: tamed guhs within %s blocks come by themselves"),
    "gui.guhs.centrale.guhs": ("%s/%s guhs liggen te knuffelen, njeg!", "%s/%s guhs are cuddling, njeg!"),
    "gui.guhs.centrale.honger": ("Honger! Zonder voer knuffelen ze maar voor %s procent", "Hungry! Without food they only cuddle at %s percent"),
    "gui.guhs.centrale.dekens_oud": ("De dekentjes zijn niet meer vers: %s procent. Eén wol per dag, vads!",
                                     "The blankets are not fresh any more: %s percent. One wool a day, vads!"),
    "gui.guhs.centrale.tevreden": ("Tevreden guhs! Voer: %s, wol: %s", "Happy guhs! Food: %s, wool: %s"),
    "gui.guhs.centrale.status": ("%s/%s guhs, verzorging %s procent, voer %s, wol %s",
                                 "%s/%s guhs, care %s percent, food %s, wool %s"),
    "gui.guhs.centrale.vol": ("Het bakje zit vol, njeg", "The compartment is full, njeg"),
    "advancements.guhs.techniek.tech_bronnen_mega_knuffelcentrale.title": ("Zestien knuffels", "Sixteen cuddles"),
    "advancements.guhs.techniek.tech_bronnen_mega_knuffelcentrale.description": (
        "Laat guhs knuffelen op een Mega-knuffelcentrale", "Let guhs cuddle on a Mega Cuddle Plant"),
    "advancements.guhs.techniek.centrale_export.title": ("Export-vads", "Export vads"),
    "advancements.guhs.techniek.centrale_export.description": (
        "Verkoop bulk aan een guh-villager voor iets zeldzaams", "Sell bulk to a guh villager for something rare"),
}

BLOKUITLEG = (
    f"Het reuzenkussen van de Mega-knuffelcentrale (5 bij 3). Tamme guhs binnen 8 blokken komen vanzelf liggen, tot {MAX}. "
    f"Elke guh geeft {PER} vadskracht.\\n\\n"
    f"- Klik met kaasknabbels, roze broodjes of kaasknabbelblokken: voer. Elke knuffelende guh eet om de {VOER_S} seconden een "
    f"portie. Zonder voer knuffelen ze maar voor {HONGER} procent.\\n"
    f"- Klik met wol: verse dekentjes. Eén wol per dag; oude dekentjes geven {DEKENS} procent.\\n"
    f"- Buizen en trechters mogen voer en wol erin doen.\\n"
    f"- Eén per vadskracht-net. Lege hand: hoe gaat het met de guhs?",
    f"The giant cushion of the Mega Cuddle Plant (5 by 3). Tamed guhs within 8 blocks come and lie down by themselves, up to "
    f"{MAX}. Every guh gives {PER} vadskracht.\\n\\n"
    f"- Click with kaasknabbels, pink buns or blocks of kaasknabbels: food. Every cuddling guh eats a portion every {VOER_S} "
    f"seconds. Without food they only cuddle at {HONGER} percent.\\n"
    f"- Click with wool: fresh blankets. One wool a day; old blankets give {DEKENS} percent.\\n"
    f"- Pipes and hoppers may put food and wool in.\\n"
    f"- One per vadskracht net. Empty hand: how are the guhs doing?")

# ---------------------------------------------------------------------------------------------------------------- textures
rng = np.random.default_rng(18052)
ROZE, LICHT, NAAD = np.array([236, 150, 190]), np.array([250, 196, 222]), np.array([196, 104, 150])


def quilt():
    t = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            ruit = ((x // 4) + (y // 4)) % 2
            t[y, x, :3] = (LICHT if ruit else ROZE) + rng.normal(0, 5)
            if x % 8 == 0 or y % 8 == 0:
                t[y, x, :3] = NAAD
            t[y, x, 3] = 255
    for (x, y) in ((4, 4), (12, 4), (4, 12), (12, 12)):   # knoopjes
        t[y, x, :3] = (255, 240, 248)
    return t


def gezicht(snoet):
    t = quilt()
    oog = (40, 24, 40)
    if snoet == "slaapt":
        for x in (4, 5, 10, 11):
            t[7, x, :3] = oog
    else:
        for (x, y) in ((4, 6), (5, 6), (4, 7), (5, 7), (10, 6), (11, 6), (10, 7), (11, 7)):
            t[y, x, :3] = oog
        t[6, 5, :3] = t[6, 11, :3] = (255, 255, 255)
    for (x, y) in ((3, 9), (12, 9)):
        t[y, x, :3] = (255, 120, 170)
    if snoet == "vol":
        for (x, y) in ((6, 10), (7, 11), (8, 11), (9, 10)):
            t[y, x, :3] = (120, 40, 80)
    else:
        for (x, y) in ((7, 10), (8, 10)):
            t[y, x, :3] = (120, 40, 80)
    for (x, y) in ((2, 2), (13, 2), (2, 1), (13, 1), (3, 1), (12, 1)):   # oortjes
        t[y, x, :3] = (255, 170, 205)
    return t


def zij():
    t = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            t[y, x, :3] = ROZE - 20 + rng.normal(0, 4)
            t[y, x, 3] = 255
    return t


def _img(t):
    return Image.fromarray(np.clip(t, 0, 255).astype(np.uint8), "RGBA")


def _slab(top, side):
    return {"parent": "minecraft:block/block", "textures": {"particle": side, "top": top, "side": side},
            "elements": [{"from": [0, 0, 0], "to": [16, 6, 16], "faces": {
                "up": {"texture": "#top"}, "down": {"texture": "#side"},
                "north": {"texture": "#side", "uv": [0, 10, 16, 16]}, "south": {"texture": "#side", "uv": [0, 10, 16, 16]},
                "east": {"texture": "#side", "uv": [0, 10, 16, 16]}, "west": {"texture": "#side", "uv": [0, 10, 16, 16]}}}]}


SNOETEN = ("slaapt", "werkt", "vol")
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def build(h):
    A, D, w = h.A, h.D, h.w
    h.save(_img(quilt()), "block", "mega_knuffelcentrale_deel.png")
    h.save(_img(zij()), "block", "mega_knuffelcentrale_zij.png")
    for s in SNOETEN:
        h.save(_img(gezicht(s)), "block", f"mega_knuffelcentrale_{s}.png")
        w(f"{A}/models/block/mega_knuffelcentrale_{s}.json", _slab(f"guhs:block/mega_knuffelcentrale_{s}", "guhs:block/mega_knuffelcentrale_zij"))
    w(f"{A}/models/block/mega_knuffelcentrale_deel.json", _slab("guhs:block/mega_knuffelcentrale_deel", "guhs:block/mega_knuffelcentrale_zij"))
    varianten = {}
    for f, rot in ROT.items():
        for s in SNOETEN:
            v = {"model": f"guhs:block/mega_knuffelcentrale_{s}"}
            if rot:
                v["y"] = rot
            varianten[f"facing={f},snoet={s}"] = v
    w(f"{A}/blockstates/mega_knuffelcentrale.json", {"variants": varianten})
    w(f"{A}/blockstates/mega_knuffelcentrale_deel.json", {"multipart": [{"apply": {"model": "guhs:block/mega_knuffelcentrale_deel"}}]})
    w(f"{A}/models/item/mega_knuffelcentrale.json", {"parent": "guhs:block/mega_knuffelcentrale_werkt"})
    h.self_drop("mega_knuffelcentrale")
    h.shaped("mega_knuffelcentrale", ["KBK", "WGW", "KBK"], {"K": "guhs:knuffelgenerator", "B": "guhs:knabbelbatterij",
                                                             "W": "guhs:pluiswolblok", "G": "guhs:gloeister"}, "guhs:mega_knuffelcentrale", 1)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:mega_knuffelcentrale", "guhs:mega_knuffelcentrale_deel"])
    # advancements: hidden quest/* (the FTB tasks) + visible techniek/*
    for naam, icoon in (("tech_bronnen_mega_knuffelcentrale", "guhs:mega_knuffelcentrale"), ("centrale_export", "minecraft:emerald")):
        w(f"{D}/advancement/quest/{naam}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
        w(f"{D}/advancement/techniek/{naam}.json", {
            "display": {"icon": {"id": icoon}, "title": {"translate": f"advancements.guhs.techniek.{naam}.title"},
                        "description": {"translate": f"advancements.guhs.techniek.{naam}.description"},
                        "frame": "task", "show_toast": True, "announce_to_chat": False, "hidden": False},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}, "parent": "guhs:techniek/root"})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for p in (f"{h.A}/blockstates/mega_knuffelcentrale.json", f"{h.A}/blockstates/mega_knuffelcentrale_deel.json",
              f"{h.A}/models/item/mega_knuffelcentrale.json", f"{h.D}/recipe/mega_knuffelcentrale.json"):
        if not os.path.exists(p):
            problems.append(f"missing {p}")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    if problems:
        raise SystemExit("centrale self-check failed:\n  " + "\n  ".join(problems))


# ------------------------------------------------------------------------------------------------------------------- FTB
FTB_SECTIES = [("centrale", "De Mega-knuffelcentrale", "item:guhs:mega_knuffelcentrale", None)]


def ftb(fq):
    q, adv = fq.q, fq.adv
    q("centrale_knuffel", "Zestien knuffels",
      f"De &dMega-knuffelcentrale&r is een reuzenkussen van 5 bij 3. Tot &d{MAX} tamme guhs&r komen er vanzelf op knuffelen, en elke "
      f"guh geeft &b{PER} vadskracht&r: samen {MAX * PER}! Verzorg ze wel: &6voer&r (kaasknabbels, roze broodjes of kaasknabbelblokken) "
      f"en elke dag &6één wol&r voor verse dekentjes. Eén per net.",
      "guhs:mega_knuffelcentrale", [adv("tech_bronnen_mega_knuffelcentrale")], rewards=(("guhs:kaas_knabbels", 32),), xp=100)
    q("centrale_export", "Export-vads",
      "De &dguh-villagers&r (Vadsmonteur, Mijnwerker, Kristalslijper en Toverguh) kopen elke dag &6bulk&r van je fabrieken: "
      "kaasknabbelblokken, knuffelsteen, gekleurde wol en baksels. Ze betalen met zeldzame dingen: kleertjes, knuffels, weerpotjes, "
      "droomzand en zelfs fossielen. Elke dag nieuwe bestellingen!",
      "minecraft:emerald", [adv("centrale_export")], rewards=(("guhs:droomzand", 4),), xp=50)


FTB_EN = {
    "centrale_knuffel": ("Sixteen cuddles",
                         f"The &dMega Cuddle Plant&r is a giant 5 by 3 cushion. Up to &d{MAX} tamed guhs&r come and cuddle on it by "
                         f"themselves, and every guh gives &b{PER} vadskracht&r: {MAX * PER} together! Do look after them: &6food&r "
                         f"(kaasknabbels, pink buns or blocks of kaasknabbels) and &6one wool&r a day for fresh blankets. One per net."),
    "centrale_export": ("Export vads",
                        "The &dguh villagers&r (Vadsmonteur, Miner, Crystal Cutter and Magic Guh) buy &6bulk&r from your factories every "
                        "day: blocks of kaasknabbels, knuffelsteen, coloured wool and baked goods. They pay with rare things: clothes, "
                        "plushies, weather jars, dream sand and even fossils. New orders every day!"),
}


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    with open(os.path.join("tools", "lang", "en", "c217_centrale.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    ftb_en = {"ftb.guhs_techniek.section.centrale": "The Mega Cuddle Plant"}
    for q, (titel, desc) in FTB_EN.items():
        ftb_en[f"ftb.guhs_techniek.q.{q}.title"] = titel
        ftb_en[f"ftb.guhs_techniek.q.{q}.desc"] = desc
    with open(os.path.join("tools", "lang", "en", "c217_centrale_ftb.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(ftb_en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    with open(os.path.join(a, "mega_knuffelcentrale.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "guhs:item/mega_knuffelcentrale"}}, f, indent=2)
        f.write("\n")
    _blokuitleg()
    print(f"centrale: Mega-knuffelcentrale ({MAX} x {PER} VK), export trades, {len(TEKSTEN)} texts")


def _blokuitleg():
    p = os.path.join("tools", "wiki_site", "blokuitleg.py")
    with open(p, encoding="utf-8") as f:
        s = f.read()
    blok = (f"\n# <centrale>\nUITLEG['mega_knuffelcentrale'] = dict(\n    nl=\"{BLOKUITLEG[0]}\",\n    en=\"{BLOKUITLEG[1]}\",\n"
            f"    meer=[])\n# </centrale>\n")
    s = re.sub(r"\n# <centrale>\n.*?# </centrale>\n", "", s, flags=re.S)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(s.rstrip("\n") + "\n" + blok)


if __name__ == "__main__":
    _direct()
