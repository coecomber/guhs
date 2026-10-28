"""
1.8.0 (vadskracht, slice "vadsbasis", dromen-ontwerp/BOUWPLAN_VADS.md): the ground floor of vadskracht made fair and a bit bigger.
Java: feature/vadskracht (BronSoort, VadsNet, VadsUitlezing: afnemende opbrengst of the Guhraden), feature/weer (the
Wolkenantennes, the radius of the Weer vadser), feature/techsaus (Knuffelsteen-generator, Sausbuis, Saustank) and
feature/vadsbasis (the Knabbelketel).

  knabbelketel             a middle source (VadsGetallen.KNABBELKETEL) that burns kaasknabbels and blocks of them
  knuffelsteen_generator   kaassaus + water -> knuffelsteen on vadskracht (a cobblestone generator, but softer)
  saustank                 a Sausvat of 64 buckets (steel hoops)
  sausbuis                 a thick hose for long lines: counts 1/16 of a Sausslang against the hose limit
  wolkenantenne_1/_2/_3    put on top of a Weer vadser (or right-click it with one): radius 20, 35, 60

build(h) makes the looks, loot, tags, recipes and texts; ftb(fq) the section "Vadskracht voor de basis" in Guh-technologie.
Numbers in the texts come from the Java side (VadsGetallen, SausGetallen) through getal(): never type one.

Standalone (from the project root): python tools/features/vadsbasis.py   (English also in tools/lang/en/c217_vadsbasis.json)
"""
import json
import os
import re
import sys

from PIL import Image

try:
    from features import vadskracht
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadskracht

FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [("vadsbasis", "Vadskracht voor de basis", "item:guhs:knabbelketel", None)]

BLOKKEN = ("knabbelketel", "knuffelsteen_generator", "saustank", "sausbuis", "wolkenantenne_1", "wolkenantenne_2", "wolkenantenne_3")
STRALEN = (10, 20, 35, 60)          # Java: WeerZones.STRALEN
KRACHTEN = (20, 30, 45, 70)         # Java: WeerFeature.KRACHTEN
ROMEINS = ("", "I", "II", "III")


def getal(naam):
    return vadskracht.getal(naam)


def saus(naam):
    """A number of techsaus/SausGetallen.java."""
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techsaus", "SausGetallen.java"), encoding="utf-8").read()
    m = re.search(r"\b" + naam + r"\s*=\s*([0-9][0-9_]*)", src)
    if not m:
        raise SystemExit(f"vadsbasis: SausGetallen heeft geen {naam}")
    return int(m.group(1).replace("_", ""))


def teksten():
    K = "gui.guhs.vadskracht."
    vol, af = getal("GUHRAD_VOL"), getal("GUHRAD_AFNAME_PROCENT")
    per_min = 1200 // saus("GEN_TIKKEN")
    t = {
        # --- afnemende opbrengst ---
        K + "telt_deels": ("Telt voor %s%% mee: na %s %s wordt een guh een beetje lui, njeg",
                           "Counts for %s%%: after %s %s a guh gets a bit lazy, nyeg"),
        K + "afnemend": ("%s Guhraden in deze opstelling: samen nog %s%% zo vads. Bouw liever een tweede opstelling!",
                         "%s Guh Wheels in this setup: together still %s%% as chonky. Better build a second setup!"),
        K + "soort.knabbelketel": ("Knabbelketels", "Nibble Boilers"),
        K + "soort.knabbelketel.enkel": ("Knabbelketel", "Nibble Boiler"),
        "block.guhs.guh_wheel.lore.afname": (
            f"De eerste {vol} Guhraden in een opstelling tellen helemaal mee. Elk rad daarna levert {af}% van het vorige: "
            "guhs rennen niet graag in de file.",
            f"The first {vol} Guh Wheels in a setup count in full. Every wheel after that gives {af}% of the one before: "
            "guhs don't like running in a traffic jam."),
        # --- Knabbelketel ---
        "block.guhs.knabbelketel": ("Knabbelketel", "Nibble Boiler"),
        "block.guhs.knabbelketel.lore": (
            "Een ketel met een guh-snoet die kaasknabbels stookt. Rechtsklik met knabbels of een blok kaasknabbels "
            "(dat brandt negen keer zo lang), of laat een Knabbelbuis hem vullen. Ruikt naar gesmolten kaas.",
            "A boiler with a guh face that burns cheese nibbles. Right-click with nibbles or a block of cheese nibbles "
            "(that burns nine times as long), or let a Nibble Pipe fill it. Smells of melted cheese."),
        "block.guhs.knabbelketel.lore.kracht": ("Geeft %s vadskracht en eet %s kaasknabbels per uur",
                                                "Gives %s vadskracht and eats %s cheese nibbles an hour"),
        "block.guhs.knabbelketel.koud": ("De ketel is koud: gooi er kaasknabbels in!", "The boiler is cold: throw in some cheese nibbles!"),
        "block.guhs.knabbelketel.brandt": ("Hij stookt: nog %s knabbels in het bakje, deze knabbel nog %s tellen",
                                           "It burns: %s nibbles left in the tray, this nibble %s more seconds"),
        "block.guhs.knabbelketel.gevoerd": ("Nom! Er liggen nu %s knabbels in het bakje", "Nom! %s nibbles in the tray now"),
        "block.guhs.knabbelketel.vol": ("Het bakje zit vol, vads", "The tray is full, vads"),
        # --- Knuffelsteen-generator ---
        "block.guhs.knuffelsteen_generator": ("Knuffelsteen-generator", "Cuddlestone Generator"),
        "block.guhs.knuffelsteen_generator.lore": (
            f"Kaassaus en water komen samen en er rolt knuffelsteen uit, op {getal('KNUFFELSTEEN_GENERATOR')} vadskracht: "
            f"{per_min} per minuut. Zet hem naast een waterbron, dan hoeft er alleen kaassaus in. De stenen gaan vanzelf in "
            "een kist, de bank of een buis ernaast.",
            f"Cheese sauce and water meet and out rolls cuddlestone, on {getal('KNUFFELSTEEN_GENERATOR')} vadskracht: "
            f"{per_min} a minute. Put it next to a water source and only cheese sauce has to go in. The stones go into a "
            "chest, the bank or a pipe next to it by themselves."),
        "block.guhs.knuffelsteen_generator.water_naast": ("Water uit de bron ernaast: gratis, njeg!", "Water from the source next to it: free, nyeg!"),
        "block.guhs.knuffelsteen_generator.gemaakt": ("Al %s knuffelstenen gemaakt", "%s cuddlestones made so far"),
        # --- Saustank, Sausbuis ---
        "block.guhs.saustank": ("Grote saustank", "Big Sauce Tank"),
        "block.guhs.saustank.lore": (
            f"Een Sausvat met stalen hoepels voor {saus('TANK') // 1000} emmers van één saus. Op zijn buik zit een meter. "
            "Pak je hem op, dan gaat de saus mee. Een zwembad vol kaassaus, bijna.",
            f"A Sauce Vat with steel hoops for {saus('TANK') // 1000} buckets of one sauce. A meter sits on its belly. "
            "Pick it up and the sauce comes along. Almost a swimming pool of cheese sauce."),
        "block.guhs.sausbuis": ("Sausbuis", "Sauce Pipe"),
        "block.guhs.sausbuis.lore": (
            "Een dikke buis voor saus over lange afstanden. Sluit vanzelf aan op pompen, vaten, tanks, slangen en sausmachines, "
            f"en telt maar voor een zestiende slang: tot {saus('SLANG_MAX') * 16} buizen aan één stuk.",
            "A thick pipe for sauce over long distances. Joins pumps, vats, tanks, hoses and sauce machines by itself, "
            f"and counts as only a sixteenth of a hose: up to {saus('SLANG_MAX') * 16} pipes in one line."),
        # --- Wolkenantennes ---
        "block.guhs.weer_vadser.lore.straal": (
            "Straal %s blokken. Met een Wolkenantenne erop of ernaast wordt dat tot %s: rechtsklik de Weer vadser met de antenne",
            "Radius %s blocks. With a Cloud Antenna on top or next to it that grows up to %s: right-click the Weather Vadser with the antenna"),
        "block.guhs.wolkenantenne.lore": ("Weer vadser-straal %s blokken, kost %s vadskracht", "Weather Vadser radius %s blocks, costs %s vadskracht"),
        "block.guhs.wolkenantenne.lore.hoe": (
            "Zet hem op of naast de Weer vadser, of rechtsklik de Weer vadser ermee. Eerst I, dan II, dan III.",
            "Put it on top of or next to the Weather Vadser, or right-click the Weather Vadser with it. First I, then II, then III."),
        "gui.guhs.weer.antenne.al": ("Deze Weer vadser heeft al Wolkenantenne %s of beter", "This Weather Vadser already has Cloud Antenna %s or better"),
        "gui.guhs.weer.antenne.eerst": ("Eerst Wolkenantenne %s, njeg: één wolkje tegelijk", "First Cloud Antenna %s, nyeg: one cloud at a time"),
        "gui.guhs.weer.antenne.erin": ("Wolkenantenne %s erop! Straal nu %s blokken, kost %s vadskracht",
                                       "Cloud Antenna %s on! Radius now %s blocks, costs %s vadskracht"),
        "gui.guhs.weer.uitlezing.straal": ("Straal %s blokken (antenne niveau %s)", "Radius %s blocks (antenna level %s)"),
    }
    for i in (1, 2, 3):
        t[f"block.guhs.wolkenantenne_{i}"] = (f"Wolkenantenne {ROMEINS[i]}", f"Cloud Antenna {ROMEINS[i]}")
    return {k: (nl, en.replace("vadskracht", "chonk power")) for k, (nl, en) in t.items()}     # (GLOSSARY: vadskracht = chonk power)


# the texts of the Weer vadser that named "60 blokken": weer.py says this now too (a full run takes them from there);
# the standalone run puts them in the lang files and in tools/lang/en/c211_weer.json
WEER_TEKSTEN = {
    "block.guhs.weer_vadser.lore": ("Maakt zijn eigen weertje in de rondte. Rechtsklik: kies een weertje!",
                                    "Makes its own weather all around. Right-click: pick a weather!"),
    "gui.guhs.weer.gekozen": ("Weertje: %s! Tot %s blokken in de rondte.", "Weather: %s! Up to %s blocks around."),
}


# =====================================================================================================================
# looks
# =====================================================================================================================
def _ketel_voor(img, staat):
    """Flames under the face, a little grate."""
    for x in range(3, 13):
        img.putpixel((x, 12), (70, 60, 64, 255))
        img.putpixel((x, 14), (70, 60, 64, 255))
    if staat != "slaapt":
        for x, h in ((4, 2), (6, 1), (8, 2), (10, 1), (12, 2)):
            for dy in range(h):
                img.putpixel((x, 13 - dy), (255, 150 + 40 * dy, 40, 255))
        for x in (5, 7, 9, 11):
            img.putpixel((x, 13), (255, 210, 80, 255))


def _generator_voor(img, staat):
    """A little window where the knuffelsteen rolls out: pink stone, a yellow drop of sauce and a blue one of water."""
    for y in range(11, 15):
        for x in range(5, 11):
            img.putpixel((x, y), (226, 170, 190, 255) if (x + y) % 3 else (200, 140, 165, 255))
    img.putpixel((3, 12), (255, 214, 96, 255))
    img.putpixel((3, 13), (255, 214, 96, 255))
    img.putpixel((12, 12), (90, 140, 255, 255))
    img.putpixel((12, 13), (90, 140, 255, 255))


def _tex(naam, kleur, rand, ringen=()):
    img = Image.new("RGBA", (16, 16), kleur + (255,))
    for i in range(16):
        for j in ringen:
            img.putpixel((j, i), rand + (255,))
    return naam, img


def saustank(h):
    """The Sausvat's models with steel hoops and a steel rim (each model a child of the vat's)."""
    T = lambda n: f"guhs:block/{n}"
    vat = json.load(open(f"{h.A}/blockstates/sausvat.json", encoding="utf-8"))
    varianten = {}
    gemaakt = set()
    for k, v in vat["variants"].items():
        bron = v["model"].split("/", 1)[1]
        n = bron.replace("sausvat", "saustank", 1)
        if n not in gemaakt:
            h.w(f"{h.A}/models/block/{n}.json", {"parent": T(bron), "textures": {"hoepel": T("techsaus_staal"), "boven": T("techsaus_staal")}})
            gemaakt.add(n)
        varianten[k] = dict(v, model=T(n))
    h.w(f"{h.A}/blockstates/saustank.json", {"variants": varianten})
    h.w(f"{h.A}/models/block/saustank.json", {"parent": T("sausvat"), "textures": {"hoepel": T("techsaus_staal"), "boven": T("techsaus_staal")}})
    h.w(f"{h.A}/models/item/saustank.json", {"parent": T("saustank")})
    h.w(f"{h.D}/loot_table/blocks/saustank.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:saustank", "functions": [
            {"function": "minecraft:copy_components", "source": "block_entity", "include": ["guhs:techsaus_inhoud"]}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def _doos(frm, to, tex):
    return {"from": list(frm), "to": list(to), "faces": {f: {"texture": tex} for f in ("north", "south", "east", "west", "up", "down")}}


def sausbuis(h):
    """A thick copper pipe: a brass knot and a fat arm per joined side (the Sausslang's multipart, but 8 wide)."""
    T = lambda n: f"guhs:block/{n}"
    img = Image.new("RGBA", (16, 16), (196, 120, 82, 255))
    for i in range(16):
        for j in (0, 5, 10, 15):
            img.putpixel((j, i), (150, 86, 60, 255))
        img.putpixel((i, 2), (226, 156, 112, 255))
    h.save(img, "block", "sausbuis.png")
    tex = {"particle": T("sausbuis"), "buis": T("sausbuis"), "knoop": T("sausslang_knoop")}
    h.w(f"{h.A}/models/block/sausbuis_knoop.json", {"parent": "minecraft:block/block", "textures": tex,
                                                    "elements": [_doos((4, 4, 4), (12, 12, 12), "#knoop")]})
    armen = {"north": ((5, 5, 0), (11, 11, 4)), "south": ((5, 5, 12), (11, 11, 16)), "west": ((0, 5, 5), (4, 11, 11)),
             "east": ((12, 5, 5), (16, 11, 11)), "down": ((5, 0, 5), (11, 4, 11)), "up": ((5, 12, 5), (11, 16, 11))}
    for kant, (frm, to) in armen.items():
        h.w(f"{h.A}/models/block/sausbuis_arm_{kant}.json", {"parent": "minecraft:block/block", "textures": tex,
                                                             "elements": [_doos(frm, to, "#buis")]})
    h.w(f"{h.A}/blockstates/sausbuis.json", {"multipart": [{"apply": {"model": T("sausbuis_knoop")}}] + [
        {"when": {kant: "true"}, "apply": {"model": T(f"sausbuis_arm_{kant}")}} for kant in armen]})
    h.w(f"{h.A}/models/block/sausbuis.json", {"parent": "minecraft:block/block", "textures": tex, "elements": [
        _doos((4, 4, 4), (12, 12, 12), "#knoop"), _doos(*armen["west"], "#buis"), _doos(*armen["east"], "#buis")]})
    h.w(f"{h.A}/models/item/sausbuis.json", {"parent": T("sausbuis")})
    h.self_drop("sausbuis")


def antenne(h, niveau):
    """A thin pole with a lightning-rod tip and as many little clouds as its level."""
    n = f"wolkenantenne_{niveau}"
    tex = {"particle": "minecraft:block/white_wool", "paal": "minecraft:block/iron_block", "wolk": "minecraft:block/white_wool",
           "punt": "minecraft:block/gold_block" if niveau < 3 else "minecraft:block/diamond_block"}
    el = [_doos((7, 0, 7), (9, 14, 9), "#paal"), _doos((7.5, 14, 7.5), (8.5, 16, 8.5), "#punt")]
    wolken = [((4, 9, 4), (12, 12, 12)), ((2, 5, 6), (6, 8, 10)), ((10, 3, 6), (14, 6, 10))]
    for frm, to in wolken[:niveau]:
        el.append(_doos(frm, to, "#wolk"))
    h.w(f"{h.A}/models/block/{n}.json", {"parent": "minecraft:block/block", "textures": tex, "elements": el})
    h.w(f"{h.A}/blockstates/{n}.json", {"variants": {"": {"model": f"guhs:block/{n}"}}})
    h.w(f"{h.A}/models/item/{n}.json", {"parent": f"guhs:block/{n}"})
    h.self_drop(n)


def recipes(h):
    ijzer, draad, spies, slang = "minecraft:iron_ingot", "guhs:guh_wire", "guhs:grillspies", "guhs:sausslang"
    h.shaped("knabbelketel", ["IKI", "IFI", "BDB"], {"I": ijzer, "K": "guhs:block_of_kaasknabbels", "F": "minecraft:blast_furnace",
                                                     "B": "minecraft:bricks", "D": draad}, "guhs:knabbelketel")
    h.shaped("knuffelsteen_generator", ["IKI", "SGS", "IDI"], {"I": ijzer, "K": "guhs:knuffelsteen", "S": slang, "G": spies, "D": draad},
             "guhs:knuffelsteen_generator")
    h.shaped("saustank", ["IVI", "V V", "IVI"], {"I": ijzer, "V": "guhs:sausvat"}, "guhs:saustank")
    h.shaped("sausbuis", ["CCC", "SSS", "CCC"], {"C": "minecraft:copper_ingot", "S": slang}, "guhs:sausbuis", 8)
    h.shaped("wolkenantenne_1", [" R ", "WIW", " I "], {"R": "minecraft:lightning_rod", "W": "minecraft:white_wool", "I": ijzer},
             "guhs:wolkenantenne_1")
    h.shaped("wolkenantenne_2", ["GWG", "WAW", "GDG"], {"G": "minecraft:gold_ingot", "W": "minecraft:white_wool", "A": "guhs:wolkenantenne_1",
                                                        "D": draad}, "guhs:wolkenantenne_2")
    h.shaped("wolkenantenne_3", ["DWD", "WAW", "DBD"], {"D": "minecraft:diamond", "W": "minecraft:white_wool", "A": "guhs:wolkenantenne_2",
                                                        "B": "minecraft:iron_block"}, "guhs:wolkenantenne_3")


def build(h):
    vadskracht.machine(h, "knabbelketel", (214, 120, 70), (255, 200, 90), voor=_ketel_voor)
    vadskracht.machine(h, "knuffelsteen_generator", (236, 160, 190), (255, 214, 96), voor=_generator_voor)
    saustank(h)
    sausbuis(h)
    for i in (1, 2, 3):
        antenne(h, i)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:saustank", "guhs:sausbuis"] + [f"guhs:wolkenantenne_{i}" for i in (1, 2, 3)])
    recipes(h)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for n in BLOKKEN:
        for p in (f"{h.A}/blockstates/{n}.json", f"{h.A}/models/item/{n}.json", f"{h.D}/recipe/{n}.json", f"{h.D}/loot_table/blocks/{n}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    if tuple(vadskracht.bron_max().get(k, 0) for k in ("knabbelketel",)) != (4,):
        problems.append("BronSoort KNABBELKETEL: not 4 per net")
    for k, (nl, en) in {**teksten(), **WEER_TEKSTEN}.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    if problems:
        raise SystemExit("vadsbasis self-check failed:\n  " + "\n  ".join(problems))


# =====================================================================================================================
# FTB: the section "Vadskracht voor de basis" in Guh-technologie
# =====================================================================================================================
FTB_EN = {
    "vb_knabbelketel": ("Cheese in the boiler", "The &6Nibble Boiler&r burns cheese nibbles for &d80 vadskracht&r. A block of cheese "
                        "nibbles burns nine times as long. Hungry, though: hundreds of nibbles an hour. Time for a nibble farm!"),
    "vb_generator": ("Soft stone", "Cheese sauce + water = &dcuddlestone&r. Put the &6Cuddlestone Generator&r next to a water source, "
                     "give it cheese sauce through a hose and vadskracht, and put a chest next to it."),
    "vb_saustank": ("A pool of sauce", "The &6Big Sauce Tank&r holds 64 buckets. The meter on its belly shows how full it is."),
    "vb_sausbuis": ("Pipes all the way", "A &6Sauce Pipe&r carries sauce far: it only counts as a sixteenth of a hose."),
    "vb_antenne": ("Weather all around", "A Weather Vadser only reaches 10 blocks. Put a &bCloud Antenna&r on it: I, II and III make "
                   "the radius 20, 35 and 60. Every step costs more vadskracht."),
}


def ftb(fq):
    q, item = fq.q, fq.item
    q("vb_knabbelketel", "Kaas in de ketel", f"De &6Knabbelketel&r stookt kaasknabbels voor &d{getal('KNABBELKETEL')} vadskracht&r. Een blok "
      "kaasknabbels brandt negen keer zo lang. Wel een veelvraat: honderden knabbels per uur. Tijd voor een knabbelboerderij!",
      "guhs:knabbelketel", [item("guhs:knabbelketel")], rewards=(("guhs:kaas_knabbels", 32),), xp=50)
    q("vb_generator", "Zachte steen", "Kaassaus + water = &dknuffelsteen&r. Zet de &6Knuffelsteen-generator&r naast een waterbron, geef "
      "hem kaassaus via een slang en vadskracht, en zet er een kist naast.", "guhs:knuffelsteen_generator",
      [item("guhs:knuffelsteen_generator")], xp=50)
    q("vb_saustank", "Een zwembad saus", "De &6Grote saustank&r bewaart 64 emmers. De meter op zijn buik laat zien hoe vol hij zit.",
      "guhs:saustank", [item("guhs:saustank")], xp=30)
    q("vb_sausbuis", "Buizen tot de horizon", "Een &6Sausbuis&r brengt saus ver weg: hij telt maar voor een zestiende slang.",
      "guhs:sausbuis", [item("guhs:sausbuis", 16)], xp=30)
    q("vb_antenne", "Weer in de wijde omtrek", "Een Weer vadser reikt maar 10 blokken. Zet er een &bWolkenantenne&r op: I, II en III "
      "maken de straal 20, 35 en 60. Elke stap kost meer vadskracht.", "guhs:wolkenantenne_1", [item("guhs:wolkenantenne_1")], xp=30)


# =====================================================================================================================
# standalone
# =====================================================================================================================
def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    alles = {**teksten(), **WEER_TEKSTEN}
    en = {k: v[1] for k, v in teksten().items()}
    for qk, (titel, desc) in FTB_EN.items():
        en[f"ftb.guhs_techniek.q.{qk}.title"] = titel
        en[f"ftb.guhs_techniek.q.{qk}.desc"] = desc.replace("vadskracht", "chonk power")
    en["ftb.guhs_techniek.section.vadsbasis"] = "Chonk power for your base"
    c211 = os.path.join("tools", "lang", "en", "c211_weer.json")      # (weer.py says the same since 1.8.0 vadsbasis)
    oud = json.load(open(c211, encoding="utf-8"))
    oud.update({k: v[1] for k, v in WEER_TEKSTEN.items()})
    with open(c211, "w", encoding="utf-8", newline="\n") as f:
        json.dump(oud, f, indent=2, ensure_ascii=False)
        f.write("\n")
    with open(os.path.join("tools", "lang", "en", "c217_vadsbasis.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in alles.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    for n in BLOKKEN:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"vadsbasis: {len(BLOKKEN)} blocks, {len(alles)} texts")


if __name__ == "__main__":
    _direct()
