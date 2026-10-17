"""
einde2 phase B/C (CONTRACT_E2_B 7.3), slice h2_water: chapter 2 of the Guhvatar story, "De Noordelijke Waterstam" (the
training with Meester Pakguh, the Blue Spirit, the moon fish, the trip into the Geestenwereld, Yue-guh, the wave, the
blizzard). Java: nl.juiced.guhs.feature.einde2.h2water (E2H2WaterFeature, E2H2Water, E2H2WaterScenes ...).

This module writes:
  - the texts of the questline guhvatar_water (verhaal_motor.verhaallijn: name, steps, "nu" / "waar" / short lines) and its
    hidden advancements quest/guhvatar_water_stap_<i> (the FTB tasks), the narrator card einde2_h2_water with its map, the
    texts of the thirteen scenes, every chat line of the story and of the characters (einde2_h2_water_tekst.py);
  - the boemerang (item model and texture), the names of the boemerang's flight, the gondola and the sled otter;
  - the clothes Waterstam-parka and Blauwe Geest-masker (BONES, clothes(), icons(): make_guh_variants / make_clothes_icons);
  - the FTB section "De Noordelijke Waterstam" (chapter guhs_guheinde, one quest per step, in order: FTB_LINEAIR);
  - the self-check: every text the Java asks for is here, and every camera of the scenes in the Waterstam stands in the open
    with a free view of what it looks at (proved against the build of einde2_bouw_water, which ran just before).
The Waterstam itself and the snow plain of the blizzard are the build of einde2_bouw_water (this slice owns it too).
"""
import math
import os
import re

import numpy as np
from PIL import Image

import features.einde2_kit as kit
from features import einde2_h2_water_tekst as tekst
from features import verhaal_motor as vm

LIJN = "guhvatar_water"
KAART = "einde2_h2_water"
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "h2water")

# --- FTB (CONTRACT_E2_B 7.1) --------------------------------------------------------------------------------------------------------
FTB_CHAPTER = "guhs_guheinde"
FTB_SECTIES = [("einde2_h2_water", "De Noordelijke Waterstam", "npc:pakguh", None)]
FTB_LINEAIR = True

# --- the clothes ---------------------------------------------------------------------------------------------------------------------
CLOTHES = ["waterstam_parka", "blauwe_geest_masker"]
_H = [0, 6, -2]      # the head pivot of the guh model (make_guh_variants.HEAD_PIVOT)
BONES = {
    # the Blue Spirit mask: a plate over the whole face (the eyes sit at x +-1.25..7, y 7.25..12.5, just in front of z -12),
    # a jaw that sticks out a little, and two short horns above the brows
    "outfit_blauwe_geest_masker": ("head", _H, "blauwe_geest_masker", [([-7.8, 1.6, -13.0], [15.6, 13.8, 0.6], 0),
                                                                      ([-6.0, 1.0, -13.5], [12.0, 3.0, 0.6], 0)]),
    "outfit_blauwe_geest_masker_hoorns": ("head", _H, "blauwe_geest_hoorn", [([-6.2, 14.6, -12.9], [1.4, 2.6, 1.0], 0),
                                                                           ([4.8, 14.6, -12.9], [1.4, 2.6, 1.0], 0)]),
}
BLAUW, DIEPBLAUW, WIT, BONT = (46, 92, 196), (24, 44, 108), (246, 246, 250), (236, 232, 222)


def clothes(rng, v):
    """The textures of the two pieces (kit.rng: the module's place in FEATURES does not matter)."""
    r = kit.rng("h2_water/kleding")
    px = 32

    def parka():
        a = v.fabric(BLAUW, r, 10)
        a[26:32, :] = v.fabric(BONT, r, 6)[26:32, :]            # the fur at the hem
        a[0:4, :] = v.fabric(BONT, r, 6)[0:4, :]                # and at the neck
        for x in range(2, px, 8):                               # the stitched wave pattern of the Waterstam
            a[14:16, x:x + 4] = (180, 210, 240)
            a[16:18, x + 4:x + 8] = (180, 210, 240)
        return np.clip(a, 0, 255)

    def bont():
        a = v.fabric(BONT, r, 12)
        a[::4, ::3] = (214, 208, 196)
        return np.clip(a, 0, 255)

    def masker():
        a = np.zeros((px, px, 3), np.float32)
        a[...] = BLAUW
        a[0:6, :] = DIEPBLAUW                                    # the brow
        for x0 in (4, 19):                                       # the dark eye holes with a white rim
            a[8:15, x0:x0 + 9] = WIT
            a[9:14, x0 + 1:x0 + 8] = (14, 14, 22)
        a[12:16, 14:18] = DIEPBLAUW                              # the nose
        a[21:28, 4:28] = (190, 30, 40)                           # the grin...
        for x in range(5, 27, 3):                                # ...full of white teeth
            a[21:24, x:x + 2] = WIT
            a[25:28, x + 1:x + 3] = WIT
        a[2:5, 2:12] = WIT                                       # the white brows
        a[2:5, 20:30] = WIT
        return np.clip(a, 0, 255)

    def hoorn():
        return np.clip(v.fabric(WIT, r, 6), 0, 255)

    return {
        "waterstam_parka": {"suit": parka, "scarf": bont},
        "blauwe_geest_masker": {"blauwe_geest_masker": masker, "blauwe_geest_hoorn": hoorn},
    }


MASKER_ICON = ["................", "..ww........ww..", ".wbbbbbbbbbbbbw.", ".bbwwwbbbbwwwbb.", ".bwkkkwbbwkkkwb.", ".bwkkkwbbwkkkwb.",
               ".bbwwwbddbwwwbb.", ".bbbbbbddbbbbbb.", ".bbbbbbbbbbbbbb.", ".brwrwrwrwrwrwb.", ".brrrrrrrrrrrrb.", ".bwrwrwrwrwrwrb.",
               "..bbbbbbbbbbbb..", "...bbbbbbbbbb...", "................", "................"]


def icons(ic):
    return {
        "waterstam_parka": ic.shirt(BLAUW, DIEPBLAUW, BONT, "stripes"),
        "blauwe_geest_masker": ic.icon(MASKER_ICON, {"w": WIT, "b": BLAUW, "d": DIEPBLAUW, "k": (14, 14, 22), "r": (190, 30, 40)}),
    }


# =====================================================================================================================
# the boemerang
# =====================================================================================================================
def boemerang_textuur():
    """16 x 16: a bent boomerang of pale wood with the blue wave band of the Waterstam and a white tip."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    hout, donker, band, punt = (214, 176, 120, 255), (150, 110, 64, 255), (52, 104, 200, 255), (246, 246, 250, 255)
    # the two arms: from the elbow (4, 4) to (13, 6) and to (6, 13), each three thick
    for t in np.linspace(0, 1, 40):
        for ax, ay, bx, by in ((4.0, 4.0, 13.5, 6.5), (4.0, 4.0, 6.5, 13.5)):
            x, y = ax + (bx - ax) * t, ay + (by - ay) * t
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    xx, yy = int(round(x + dx * 0.8)), int(round(y + dy * 0.8))
                    if 0 <= xx < 16 and 0 <= yy < 16:
                        px[xx, yy] = band if 0.42 < t < 0.58 else (punt if t > 0.9 else hout)
    # an outline
    rand = []
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] > 0 and px[x + dx, y + dy] != donker
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                rand.append((x, y))
    for x, y in rand:
        px[x, y] = donker
    return img


def boemerang(h):
    h.save(boemerang_textuur(), "item", "boemerang.png")
    h.item_model("boemerang")
    # the flight is drawn as the item (ThrownItemRenderer): no model of its own


# =====================================================================================================================
# the texts
# =====================================================================================================================
def kaart(k):
    """The map of the narrator card: the iceberg with its wall, the canals, the palace, the oasis, the sea and the fleet."""
    k.water([(4, 4), (252, 4), (252, 156), (4, 156)], kleur=(178, 200, 214, 255))
    k.land([(40, 140), (34, 60), (70, 20), (186, 18), (222, 60), (216, 140)], kleur=(238, 242, 246, 255))
    k.water([(64, 128), (192, 128), (196, 150), (60, 150)], kleur=(150, 188, 196, 255))
    k.d.rectangle((60, 104, 196, 112), fill=(206, 214, 226, 255), outline=k.INKT)            # the ice wall
    k.toren(120, 104, 10, (206, 214, 226, 255)).toren(136, 104, 10, (206, 214, 226, 255))
    k.rivier([(76, 92), (180, 92)]).rivier([(76, 78), (180, 78)]).rivier([(128, 76), (128, 130)])
    for x in range(84, 180, 16):
        k.huisje(x, 88, (120, 160, 210, 255))
    k.d.rectangle((108, 40, 148, 60), fill=(220, 230, 244, 255), outline=k.INKT)              # the palace
    k.toren(128, 40, 16, (190, 210, 236, 255))
    k.water([(118, 22), (138, 22), (142, 32), (114, 32)], kleur=(140, 200, 160, 255))          # the oasis
    for x in (70, 104, 150, 184):                                                            # the fleet
        k.d.polygon([(x - 7, 140), (x + 7, 140), (x + 4, 146), (x - 4, 146)], fill=(150, 60, 50, 255), outline=k.INKT)
    k.berg(196, 46, 16, (226, 232, 240, 255)).berg(56, 52, 14, (226, 232, 240, 255))
    k.pad([(250, 156), (210, 138), (150, 126), (128, 108)])
    k.kruis(128, 108)
    k.kompasroos()


def texts(h):
    for key, text in tekst.TEKSTEN.items():
        kit.lang(h, key, text)
    vm.verhaallijn(h, LIJN, tekst.LIJN_NAAM, tekst.LIJN_UITLEG, tekst.STAPPEN, klaar=tekst.LIJN_KLAAR, kort=tekst.LIJN_KORT)
    vm.vertelkaart(h, KAART, tekst.KAART_TITEL, tekst.KAART_REGELS, kaart)
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        vm.scene(h, f"einde2_h2_water_{naam}", titel, regels, namen)
    # fix2-h2: the step hints, their look scenes, the titles and the gondola stops (H2Hints)
    for k, v in tekst.HINTS.items():
        kit.lang(h, f"{HINT_P}hint.{k}", v)
    for k, v in tekst.HALTES.items():
        kit.lang(h, f"{HINT_P}halte.{k}", v)
    for k, (titel, onder) in tekst.TITELS.items():
        kit.lang(h, f"{HINT_P}titel.{k}", titel)
        kit.lang(h, f"{HINT_P}titel.{k}.onder", onder)
    for naam, (titel, regel) in tekst.KIJK.items():
        vm.scene(h, f"einde2_h2_water_kijk_{naam}", titel, {"tekst": regel})
    vm.sluier(h, "noordelijke_waterstam")


HINT_P = "gui.guhs.einde2_h2_water."


# =====================================================================================================================
# FTB: one quest per step, in story order (the chapter is linear)
# =====================================================================================================================
FTB_QUESTS = [
    ("aankomst", "De Noordelijke Waterstam", "Vlieg met &dAppa&r naar de &6Noordelijke Waterstam&r: een stad van ijs en grachten achter een reusachtige ijsmuur. "
     "De poortwachter-guh is streng, maar heel vriendelijk als je je poten veegt.", "guhs:einde2_bouw_water_ijssteen"),
    ("pakguh", "Een snor, alstublieft", "Praat met &dMeester Pakguh&r op het trainingsplein. Hij leert het alleen aan guhs met een snor. "
     "Gelukkig is &dKatara-guh&r er ook.", "minecraft:water_bucket"),
    ("zweep", "De waterzweep", "Raak de &6vijf doelen&r op het trainingsplein met je kracht &bWaterzweep&r (de krachtentoets).", "minecraft:target"),
    ("golf", "Golven, geen gespetter", "Maak &6drie golven&r in het golfbassin naast het plein met &bWatergolf&r.", "minecraft:heart_of_the_sea"),
    ("ijsbrug", "De ijsbrug", "Ga op het eind van de steiger staan, kijk naar het eilandje en bevries een brug met &bIJsbrug&r. Loop dan naar de overkant.",
     "minecraft:packed_ice"),
    ("slee", "Otter-surfen", "Klim op de ijsmuur west van de poort. Stap met een lege poot op de &dotter-pinguinguh&r bij de Glijkansel en glijd naar beneden.",
     "minecraft:snowball"),
    ("gondel", "De gondel", "Vanavond is het &6Maanfeest&r. Stap in de gondel in het bekken onder de grote trap en breng met &bWaterstroom&r de "
     "maanlantaarns naar de &6drie haltes&r.",
     "minecraft:oak_boat"),
    ("yue", "Prinses Yue-guh", "Breng een bezoek aan &dprinses Yue-guh&r in het paleis. &dSokka-guh&r gaat mee. Toevallig.", "minecraft:white_wool"),
    ("haven", "Rook boven de zee", "Hoofdman Arnook-guh stuurt je naar de haven: kijk wat &cZhao-Mika&r met zijn schip van plan is. Kijk uit voor "
     "netten.", "minecraft:campfire"),
    ("blauwe_geest", "De Blauwe Geest", "Iemand met een blauw masker heeft je cel opengedaan. Sluip van het schip af. Blijf uit het licht van de wachters.",
     "guhs:blauwe_geest_masker"),
    ("maanvis", "De maanvis", "Ga naar de &6geestenoase&r achter het paleis, door het deurtje in het ijs.", "minecraft:tropical_fish"),
    ("mediteer", "Mediteren", "Ga bij de vijver zitten (sluipen) en mediteer met &dAang-guh&r. Pas op voor verkeerde dimensies.", "minecraft:lily_pad"),
    ("koh", "Toon geen emotie", "Zoek &dKoh de Gezichtenrover&r in de Geestenwereld: het pad zonder roze nevel leidt naar hem. Als hij je aankijkt: "
     "niet bewegen, niet springen, nergens op klikken.",
     "minecraft:carved_pumpkin"),
    ("hemelvijver", "De Hemelvijver", "Ga naar de &6Hemelvijver&r, waar Tui en La horen te zwemmen. Er zwemt er maar een.", "minecraft:prismarine_crystals"),
    ("vis_halen", "Tui terughalen", "&6Tui, de maanvis&r, zit in een kooi aan boord van het schip van &cZhao-Mika&r. Sluip aan boord en steel hem "
     "terug.", "minecraft:iron_bars"),
    ("vis_terug", "De maan", "Breng de maanvis terug naar de vijver in de oase. &dYue-guh&r wacht daar.", "minecraft:sea_lantern"),
    ("golf_kansel", "De grote golf", "Klim naar de &6Golfkansel&r boven de poort. De oceaan wacht. Zachtjes, he.", "minecraft:trident"),
    ("storm", "De sneeuwstorm", "Volg het lichtje door de storm. Luister naar &dAppa&r. Hoofdstuk 2 is dan klaar: &dKatara-guh&r en &dSokka-guh&r reizen met je mee.",
     "guhs:boemerang"),
]


def ftb(fq):
    """One quest per step of the line, in story order (the chapter is linear: the first one follows chapter 1 by itself)."""
    vorige = None
    for i, (naam, titel, tekst_, icon) in enumerate(FTB_QUESTS, start=1):
        key = f"einde2_h2_water_{naam}"
        laatste = i == len(FTB_QUESTS)
        fq.q(key, titel, tekst_, icon, [fq.adv(f"{LIJN}_stap_{i}")], rewards=(("guhs:kaas_knabbels", 16 if laatste else 4),),
             deps=[vorige] if vorige else [], shape="gear" if laatste else None, xp=100 if laatste else 0)
        vorige = key


# =====================================================================================================================
# the self-check
# =====================================================================================================================
def _java(naam):
    return open(os.path.join(JAVA, naam), encoding="utf-8").read()


def java_sleutels():
    """Every literal text key of the slice's Java (quest.guhs.einde2_h2_water.*, gui.guhs.einde2_h2_water.*, entity/item names)."""
    keys = set()
    for f in os.listdir(JAVA):
        if not f.endswith(".java"):
            continue
        src = _java(f)
        keys |= set(re.findall(r'"((?:quest|gui)\.guhs\.einde2_h2_water\.[a-z0-9_.]+[a-z0-9_])"', src))
        keys |= set(re.findall(r'"(entity\.guhs\.einde2_h2_water_[a-z0-9_]+)"', src))
    # the roles: K + "..." (E2H2WaterRol)
    keys |= {"quest.guhs.einde2_h2_water.rol." + k for k in re.findall(r'K \+ "([a-z0-9_.]+)"', _java("E2H2WaterRol.java"))}
    # the keys built from a number or a word
    keys |= {f"quest.guhs.einde2_h2_water.sluip.gezien.{i}" for i in range(3)}
    keys |= {f"quest.guhs.einde2_h2_water.koh.gezicht.{i}" for i in range(6)}
    keys |= {f"quest.guhs.einde2_h2_water.koh.fout.{w}" for w in ("beweeg", "spring", "klik")}
    keys |= {f"gui.guhs.einde2_h2_water.kracht.{k}" for k in re.findall(r'"(einde2_h2_water_[a-z]+)"', _java("E2H2WaterKrachten.java"))}
    return {k for k in keys if not k.endswith(".")}


def java_scenes():
    """{scene naam: (anchor or None, [lines keys], [(t, cam, kijk)])} read from E2H2WaterScenes.java."""
    src = _java("E2H2WaterScenes.java")
    uit = {}
    delen = re.split(r"\n    private static Cutscene ", src)
    for deel in delen[1:]:
        m = re.search(r'new S\("(\w+)", \d+, ([-\d.]+), ([-\d.]+), ([-\d.]+)\)', deel)
        r = re.search(r'S\.rel\("(\w+)", \d+\)', deel)
        if m:
            naam, anker = m.group(1), tuple(float(m.group(i)) for i in (2, 3, 4))
        elif r:
            naam, anker = r.group(1), None
        else:
            continue
        regels = re.findall(r'\.zeg\(\d+, (?:"\w*"|Cutscene\.SPELER), "(\w+)", \d+\)', deel)
        regels += re.findall(r'\.lied\(\d+, "(\w+)", \d+\)', deel)
        cams = []
        for c in re.finditer(r'\.(?:cam|knip)\((\d+), ([-\d.]+), ([-\d.]+), ([-\d.]+), ([-\d.]+), ([-\d.]+), ([-\d.]+)\)', deel):
            v = [float(c.group(i)) for i in range(2, 8)]
            cams.append((int(c.group(1)), tuple(v[:3]), tuple(v[3:])))
        uit[naam] = (anker, regels, cams)
    return uit


DOORZICHTIG = ("glass", "_pane", "iron_bars", "tralies", "_fence", "lantern", "chain", "banner", "sign", "candle", "torch", "carpet")


def _ziet_door(b, naam):
    return naam is None or naam in ("minecraft:air", "minecraft:cave_air") or naam in kit.WATER or b.is_dun(naam) or any(t in naam for t in DOORZICHTIG)


# the scenes written relative to a marker of the Waterstam (the snow plain): checked at that marker
REL_ANKER = {"sneeuwstorm": "sneeuwstorm_start", "team": "sneeuwstorm_vind"}


def controleer_cameras(b, scenes):
    """Every camera of a Waterstam scene stands in the open and sees what it looks at (the last 0.8 blocks excepted)."""
    p = []
    blocks = b.s.blocks
    scenes = dict(scenes)
    for naam, marker in REL_ANKER.items():
        if naam in scenes and marker in b.plekken:
            ax, ay, az = b.plekken[marker]["pos"]
            _, regels, cams = scenes[naam]
            scenes[naam] = ((ax, ay, az), regels, [(t, (c[0] + ax, c[1] + ay, c[2] + az), (k[0] + ax, k[1] + ay, k[2] + az)) for t, c, k in cams])

    def naam(q):
        c = tuple(int(math.floor(v)) for v in q)
        bl = blocks.get(c)
        return None if bl is None else bl[0]

    for scene, (anker, _, cams) in scenes.items():
        if anker is None:
            continue
        for t, cam, kijk in cams:
            if not _ziet_door(b, naam(cam)):
                p.append(f"{scene} t={t}: the camera {cam} stands in {naam(cam)}")
                continue
            d = [kijk[i] - cam[i] for i in range(3)]
            lengte = math.sqrt(sum(x * x for x in d))
            n = int(lengte * 4)
            for i in range(1, n):
                if lengte * (1 - i / n) < 0.8:
                    break
                q = [cam[j] + d[j] * i / n for j in range(3)]
                if not _ziet_door(b, naam(q)):
                    p.append(f"{scene} t={t}: from {cam} to {kijk} {naam(q)} stands in the way at {tuple(round(v, 1) for v in q)}")
                    break
    return p


def selfcheck(h):
    problems = []
    for key in sorted(java_sleutels()):
        if key not in h.NL:
            problems.append(f"no Dutch text for {key}")
    scenes = java_scenes()
    for naam, (_, regels, _) in scenes.items():
        if naam not in tekst.SCENES:
            problems.append(f"scene {naam}: no texts")
            continue
        for r in regels:
            if f"scene.guhs.einde2_h2_water_{naam}.{r}" not in h.NL:
                problems.append(f"scene {naam}: no text for the line {r}")
    for naam in tekst.SCENES:
        if naam not in scenes:
            problems.append(f"texts of scene {naam}, which the Java does not have")
    if len(tekst.STAPPEN) != 18 or len(FTB_QUESTS) != 18:
        problems.append("the line has 18 steps: the texts and the FTB quests must say so too")
    if not os.path.exists(os.path.join(h.TEX, "item", "boemerang.png")):
        problems.append("no texture for the boemerang")
    kleren = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    blok = kleren[kleren.find("// <einde2_h2_water>"):kleren.find("// </einde2_h2_water>")]
    for stuk in CLOTHES:
        if stuk.upper() + "(" not in blok:
            problems.append(f"clothes {stuk} is not in GuhClothes between the einde2_h2_water markers")
        if f"item.guhs.{stuk}" not in h.NL:
            problems.append(f"no name for the clothes piece {stuk}")
    from features import einde2_bouw_water
    if einde2_bouw_water.GEBOUWD is not None:
        problems += controleer_cameras(einde2_bouw_water.GEBOUWD, scenes)
    # fix2-h2: H2Hints - a text per hint, a look scene per name of H2Hints.KIJK, a title per gehaald("..."), the markers it points at
    hints = _java("H2Hints.java")
    if "KIJK = {" + ", ".join(f'"{n}"' for n in tekst.KIJK) not in hints.replace("\n           ", ""):
        problems.append("H2Hints.KIJK does not list the look scenes of einde2_h2_water_tekst.KIJK")
    for naam in tekst.KIJK:
        if f'kijk("{naam}")' not in hints:
            problems.append(f"H2Hints never uses the look scene {naam}")
    for naam in re.findall(r'tekst\(T \+ "(\w+)"\)', hints) + ["geest_balk", "halte_balk"]:
        if f"{HINT_P}hint.{naam}" not in h.NL:
            problems.append(f"no Dutch text for the hint {naam}")
    alles = "".join(_java(n) for n in os.listdir(JAVA) if n.endswith(".java"))
    for naam in set(re.findall(r'H2Hints\.gehaald\(p, "(\w+)"\)', alles)):
        if f"{HINT_P}titel.{naam}" not in h.NL or f"{HINT_P}titel.{naam}.onder" not in h.NL:
            problems.append(f"no title for gehaald({naam})")
    for m in re.findall(r'marker\("(\w+)"\)', hints):
        if m not in kit_plekken():
            problems.append(f"H2Hints points at the marker {m}, which the Waterstam does not have")
    for m in ("sneeuwvlakte", "sneeuwstorm_start", "sneeuwstorm_vind", "sluip_route", "maanvis_kooi", "golf_plek", "yue_maan"):
        if m not in kit_plekken():
            problems.append(f"the Waterstam has no marker {m}")
    if problems:
        raise SystemExit("einde2_h2_water self-check failed:\n  " + "\n  ".join(problems[:60]))
    print(f"einde2_h2_water: self-check ok ({len(java_sleutels())} keys of the Java, {len(scenes)} scenes)")


def kit_plekken():
    import json
    pad = os.path.join("src", "main", "resources", "data", "guhs", "einde2", "plekken", "noordelijke_waterstam.json")
    return json.load(open(pad, encoding="utf-8"))["plekken"] if os.path.exists(pad) else {}


def build(h):
    texts(h)
    boemerang(h)
    selfcheck(h)
