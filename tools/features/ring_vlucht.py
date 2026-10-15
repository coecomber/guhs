"""
1.4.2 (rookguh) - de Rookguh-vlucht van "In de ban van de Knabbelring".

The structures of the story lie far apart. Once a player finished the story part of a chapter structure (or of an extra
stop), a Reis-Rookguh hovers at its exit; a click lets the player pick any place of the story they already reached (the next
one on top) and the Rookguh flies them there: a short cutscene, and they stand about 25 blocks in front of its entrance on
solid ground. Java: feature/ringvlucht (RingVluchtFeature, Haltes, Vlucht, Landing, VluchtRookguhEntity, client screen).

This module is the ONE source of the spots: HALTES (template coordinates of each structure's own template, of the whole
build for a tiled guhs:burcht) is written to data/guhs/ringvlucht/haltes.json, which feature/ringvlucht/Haltes reads from the
jar. The self-check reads the generated templates and fails the build when a Rookguh's spot is not free air or an entrance is
no place to stand (a builder that moves something then has to move the spot here too).
"""
import os

from features import bbq2
from features import verhaal_motor

FTB_CHAPTER = "guhs_pad_barbecuether"   # (the chapter of the two stories of the Barbecuether since the Guhpad)
FTB_SECTIES = [("ring_vlucht", "Met de Rookguh", "item:guhs:rookguh_spawn_egg", None)]

# --- the stops ---------------------------------------------------------------------------------------------------------------
# id: the stop; lijn: the questline whose END puts the Rookguh there (and whose chapter number decides "reached", see
# Haltes.java); structuur: guhs:<structuur>; rookguh: where the Rookguh hovers (the block of its feet; a 2 x 2 x 2 box from there
# must be air in the template), None = no Rookguh of its own; ingangen: (the cell you stand in at the entrance, the way OUT of
# the structure as (dx, dz)); afstand: how far in front of the entrance the Rookguh puts you down (0: on the spot itself).
HALTES = [
    # chapter 1: the Gouw. Its spots are those of Guhdalf's camp (ring_h1: template ringh1_kamp; in a Knabbelgouw the camp
    # stands at GOUW_KAMP of its template, unturned); the player lands in front of their OWN Guhdalf (Haltes.java).
    dict(id="gouw", lijn="ring_h1", structuur="knabbelgouw", rookguh=None, ingangen=[], afstand=7),
    dict(id="guhvendel", lijn="ring_h2", structuur="guhvendel", rookguh=(35, 10, 53), ingangen=[((30, 9, 58), (0, 1))], afstand=25),
    dict(id="knabbelmoria", lijn="ring_h3", structuur="knabbelmoria", rookguh=(72, 32, 63), ingangen=[((35, 31, 62), (-1, 0))], afstand=25),
    dict(id="boomstad", lijn="ring_h4", structuur="guhladriel_boomstad", rookguh=(183, 4, 52), ingangen=[((4, 2, 48), (-1, 0))], afstand=25),
    # the elf boats: the landing at the end of the Guhduin (the boomstad's Rookguh hovers there)
    dict(id="aanleg", lijn="ring_h4", structuur="guhladriel_boomstad", rookguh=None, ingangen=[((178, 3, 48), (1, 0))], afstand=0),
    dict(id="sausuman", lijn="ring_sausuman", structuur="sausuman_toren", rookguh=(8, 5, 24), ingangen=[((14, 4, 20), (0, 1))], afstand=25),
    dict(id="roosterpoort", lijn="ring_h5", structuur="zwarte_roosterpoort", rookguh=(24, 8, 87),
         ingangen=[((48, 7, 0), (0, -1)), ((0, 7, 6), (-1, 0)), ((95, 7, 6), (1, 0))], afstand=25),
    dict(id="frituurberg", lijn="ring_h6", structuur="frituurberg", rookguh=(52, 11, 89), ingangen=[((47, 10, 87), (0, 1))], afstand=25),
]
# the Rookguh at a camp of Guhdalf (camp-local: template ringh1_kamp; Guhdalf sits at (10, 1, 6) and looks south, +z)
KAMP_ROOKGUH = (2, 2, 9)
# where the camp stands in the Knabbelgouw's own template (ring_h1: Gouw.GOUW_KAMP)
GOUW_KAMP = (46, 4, 17)

NAMEN = {
    "gouw": "De Knabbelgouw",
    "guhvendel": "Guhvendel",
    "knabbelmoria": "De Mijnen van Knabbelmoria",
    "boomstad": "De boomstad van Guhladriel",
    "aanleg": "De aanlegplaats van de elfenbootjes",
    "sausuman": "De Toren van Sausuman",
    "roosterpoort": "De Zwarte Roosterpoort",
    "frituurberg": "De Frituurberg",
}

TEKSTEN = {
    "entity.guhs.ringvlucht_rookguh": "Reis-Rookguh",
    "gui.guhs.ringvlucht.titel": "Waar vliegen we heen?",
    "gui.guhs.ringvlucht.uitleg": "Ik breng je naar elke plek van je reis waar je al geweest bent. Hou je goed vast, njeg!",
    "gui.guhs.ringvlucht.volgende": "volgende",
    "gui.guhs.ringvlucht.blijf": "Blijf hier",
    "gui.guhs.ringvlucht.wereld.guhmensie": "Guhmensie",
    "gui.guhs.ringvlucht.wereld.barbecuether": "Guhbarbecuether",
    "quest.guhs.ringvlucht.niks": "Je bent nog nergens anders geweest op je reis. Loop eerst maar verder, dan vlieg ik je straks terug!",
    "quest.guhs.ringvlucht.weg_kwijt": "Hmm... ik weet de weg daarheen nog niet. Probeer het straks nog eens, njeg.",
    "quest.guhs.ringvlucht.niet_landen": "Daar kan ik nergens veilig landen. We blijven lekker hier.",
    "quest.guhs.ringvlucht.portaal": "Zo ver mag ik je nog niet brengen: het grillportaal is voor jou nog dicht.",
    "quest.guhs.ringvlucht.bezig": "Even wachten, je bent al met iets anders bezig.",
    "quest.guhs.ringvlucht.spel": "Niet midden in een spelletje, njeg!",
    "quest.guhs.ringvlucht.geland": "De Rookguh zet je zachtjes neer bij %s. Pfoe, vahoeg!",
    "quest.guhs.ringvlucht.er_is_er_een": "Bij de uitgang zweeft nu een Reis-Rookguh. Hij vliegt je naar elke plek van je reis waar je al geweest bent, njeg!",
    "quest.guhs.ringvlucht.er_is_er_een.gouw": "Bij Guhdalfs kamp zweeft nu een Reis-Rookguh. Hij vliegt je zo naar de Guhbarbecuether, en later overal heen waar je al geweest bent!",
}

SCENE_TEKSTEN = {
    "instappen": "Klim maar op mijn rug. Niet kriebelen, njeg!",
    "hou_vast": "Hou je vast aan mijn rookpluimpjes!",
    "weg": "En daar gaan we... VAHOEG!",
}
SCENE_SAM = {
    "sam": "Wacht op mij! Ik wil ook vliegen!",
    "sam_juich": "Vahoeg! We vliegen!",
}

# FTB: what a quest says (the travel tip the story needs: the structures are far apart)
FTB = [
    ("ringvlucht_vlieg", "Taxi, njeg!",
     "Heb je een hoofdstuk van de Knabbelring uitgespeeld? Dan zweeft er bij de &6uitgang&r een &dReis-Rookguh&r. Klik op hem en kies "
     "waar je heen wilt: elke plek van je reis waar je al geweest bent, met de volgende bovenaan. Hij vliegt je erheen en zet je "
     "netjes neer, een stukje voor de ingang. Sam-guh vliegt gewoon mee.",
     "minecraft:feather", "ringvlucht_gevlogen"),
    ("ringvlucht_grens", "Over het grillportaal heen",
     "Een Reis-Rookguh vliegt je ook van de &6Knabbelgouw&r in de Guhmensie naar de Guhbarbecuether (en terug). Daarvoor moet het "
     "grillportaal voor jou wel open zijn, natuurlijk.",
     "guhs:grillkool", "ringvlucht_grens"),
]

# the travel tip in the Guhdex: the "nu" text of the first step of every chapter that is a walk away (the Rookguh at the end of
# the chapter before can fly you there)
TIP = " Of laat je vliegen: bij de uitgang van het vorige hoofdstuk wacht een Reis-Rookguh."

LUCHT = {None, "minecraft:air", "minecraft:cave_air", "minecraft:structure_void", "air", "cave_air", "structure_void"}
TILE = 32


def texts(h):
    for key, tekst in TEKSTEN.items():
        h.lang(key, tekst, tekst)
    for halte, naam in NAMEN.items():
        h.lang(f"gui.guhs.ringvlucht.plek.{halte}", naam, naam)
    verhaal_motor.scene(h, "ringvlucht_vlucht", "Met de Rookguh mee", SCENE_TEKSTEN, namen={"rookguh": "Reis-Rookguh"})
    verhaal_motor.scene(h, "ringvlucht_vlucht_sam", "Met de Rookguh mee", dict(SCENE_TEKSTEN, **SCENE_SAM),
                        namen={"rookguh": "Reis-Rookguh", "sam": "Sam-guh"})


def advancements(h):
    for _key, _titel, _tekst, _icon, adv in FTB:
        bbq2.verborgen(h, adv)


def data(h):
    h.w(f"{h.D}/ringvlucht/haltes.json", {
        "haltes": [dict(id=s["id"], lijn=s["lijn"], structuur=s["structuur"],
                        rookguh=list(s["rookguh"]) if s["rookguh"] else None,
                        ingangen=[dict(plek=list(p), richting=list(r)) for p, r in s["ingangen"]],
                        afstand=s["afstand"]) for s in HALTES],
        "kamp_rookguh": list(KAMP_ROOKGUH),
    })


def _template(h, name):
    """Every block of a template (a single .nbt, or the 32 x 32 tiles <name>/stuk_i_j of a guhs:burcht, in coordinates of the
    whole build): {(x, y, z): block id}."""
    root = os.path.join(h.D, "structure")
    single = os.path.join(root, name + ".nbt")
    files = []
    if os.path.exists(single):
        files.append((single, 0, 0))
    elif os.path.isdir(os.path.join(root, name)):
        for f in sorted(os.listdir(os.path.join(root, name))):
            if f.startswith("stuk_") and f.endswith(".nbt"):
                i, j = f[5:-4].split("_")
                files.append((os.path.join(root, name, f), int(i) * TILE, int(j) * TILE))
    if not files:
        raise SystemExit(f"ring_vlucht: no template {name}")
    blocks = {}
    for path, ox, oz in files:
        t = h.read_nbt(path)
        pal = [p["Name"] for p in t["palette"]]
        for b in t["blocks"]:
            x, y, z = b["pos"]
            blocks[(ox + x, y, oz + z)] = pal[b["state"]]
    return blocks


def _lucht(blocks, p):
    return blocks.get(p) in LUCHT


def selfcheck(h):
    problems = []
    cache = {}

    def tpl(name):
        if name not in cache:
            cache[name] = _template(h, name)
        return cache[name]

    def zweef(name, b, spot, wat):
        x, y, z = spot
        for dx in (0, 1):
            for dy in (0, 1):
                for dz in (0, 1):
                    if not _lucht(b, (x + dx, y + dy, z + dz)):
                        problems.append(f"{wat}: the Rookguh at {spot} of {name} stands in {b.get((x + dx, y + dy, z + dz))} at {(x + dx, y + dy, z + dz)}")
        if _lucht(b, (x, y - 1, z)) and _lucht(b, (x, y - 2, z)) and _lucht(b, (x, y - 3, z)):
            problems.append(f"{wat}: the Rookguh at {spot} of {name} hovers more than two blocks over the floor")

    def staan(name, b, spot, wat):
        x, y, z = spot
        if _lucht(b, (x, y - 1, z)) or not _lucht(b, (x, y, z)) or not _lucht(b, (x, y + 1, z)):
            problems.append(f"{wat}: nobody can stand at the entrance {spot} of {name} ({b.get((x, y - 1, z))} / {b.get((x, y, z))} / {b.get((x, y + 1, z))})")

    for s in HALTES:
        if s["structuur"] == "knabbelgouw":
            continue
        b = tpl(s["structuur"])
        if s["rookguh"]:
            zweef(s["structuur"], b, s["rookguh"], s["id"])
        if not s["ingangen"]:
            problems.append(f"{s['id']}: no entrance")
        for plek, richting in s["ingangen"]:
            staan(s["structuur"], b, plek, s["id"])
            if abs(richting[0]) + abs(richting[1]) != 1:
                problems.append(f"{s['id']}: the way out {richting} is not one of the four directions")
    kamp = tpl("ringh1_kamp")
    zweef("ringh1_kamp", kamp, KAMP_ROOKGUH, "gouw (an old pit's camp)")
    gouw = tpl("knabbelgouw")
    zweef("knabbelgouw", gouw, tuple(GOUW_KAMP[i] + KAMP_ROOKGUH[i] for i in range(3)), "gouw (the Knabbelgouw)")
    # (the Java side keeps the camp's corner in ringh1.Gouw.GOUW_KAMP: the same number)
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ringh1", "Gouw.java")
    with open(java, encoding="utf-8") as f:
        if f"GOUW_KAMP = new BlockPos({GOUW_KAMP[0]}, G, {GOUW_KAMP[2]})" not in f.read() or GOUW_KAMP[1] != 4:
            problems.append("ring_vlucht: GOUW_KAMP differs from feature/ringh1/Gouw.java")
    ids = [s["id"] for s in HALTES]
    for halte in ids:
        if halte not in NAMEN:
            problems.append(f"{halte}: no name")
    if problems:
        raise SystemExit("ring_vlucht self-check:\n  " + "\n  ".join(problems))


def test_veld(h):
    """The game-test field (feature/ringvlucht/RingVluchtGameTests): a stone floor of 64 x 64 with a stone-brick house in the
    middle (26..37 x 26..37, walls 4 high, a roof) with a door south (31..32, z 37) and one west (x 26, z 31..32). Where a
    landing 25 blocks south of the south door would be lies a pool (and magma beside it); 25 blocks west of the west door a
    walled pocket you can't walk into."""
    s = h.Structure((64, 6, 64))
    s.fill(0, 0, 0, 63, 0, 63, "minecraft:stone")
    s.fill(0, 1, 0, 63, 5, 63, "minecraft:air")
    s.fill(26, 1, 26, 37, 5, 37, "minecraft:stone_bricks")
    s.fill(27, 1, 27, 36, 4, 36, "minecraft:air")
    s.fill(31, 1, 37, 32, 2, 37, "minecraft:air")
    s.fill(26, 1, 31, 26, 2, 32, "minecraft:air")
    s.fill(24, 0, 55, 39, 0, 63, "minecraft:water")
    s.fill(22, 0, 52, 23, 0, 58, "minecraft:magma_block")
    for x0, z0, x1, z1 in ((1, 24, 9, 24), (1, 38, 9, 38), (1, 24, 1, 38), (9, 24, 9, 38)):
        s.fill(x0, 1, z0, x1, 4, z1, "minecraft:stone_bricks")
    s.save("ringvlucht_test_veld")


def build(h):
    texts(h)
    advancements(h)
    data(h)
    test_veld(h)
    selfcheck(h)


def ftb(fq):
    for key, titel, tekst, icon, adv in FTB:
        fq.q(key, titel, tekst, icon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 4),))
