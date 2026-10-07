"""
Guhpixel slice "among" (Java: feature/guhpixel/among; namespace among; English: tools/lang/en/c34_px_among.json).

Among Guhs: the ship "De Vadsvaarder" (guhpixel_among_bouw.py: the arena template and the layout table
data/guhs/guhpixel/among_schip.txt that Java reads), the three ship blocks (task panel, emergency button, vent), the three
game items (pillow, sabotage map, voting slip), the space suit texture of the guh NPCs, the Kapitein-guh and the Logboek-guh,
the sounds, the hidden FTB advancements, every text and the FTB quests.
With the task mini-games, the oefenrondje and the shop (guhpixel_among_tex.py): the sprite sheet of the task panels, the
SUS-stickerbord (keepsake of the oefenrondje, joke game "among"), and the shop's clothes: eight Ruimtepakjes and six hoedjes
(BONES / clothes / icons / CLOTHES; they only appear after `python tools/make_guh_variants.py`, make_sleep_eyes.py and
make_clothes_icons.py). Every text is Dutch here (guhpixel_among_tekst.py).
"""
import os

import numpy as np
from PIL import Image

from features import guhpixel_among_bouw as bouw
from features import guhpixel_among_tekst as tekst
from features import guhpixel_among_tex as tex
from features import guhpixel_lib as lib

TEXTS = tekst.TEXTS
BLOKKEN = ("among_taakpaneel", "among_noodknop", "among_ventilatieluik", "among_sus_bord")
ITEMS = ("among_kussen", "among_saboteerkaart", "among_stembriefje")
OEFEN_STAPPEN = 4
ADVANCEMENTS = ("among_ronde", "among_crew_winst", "among_mika_winst", "among_lastig", "among_klaar") + tuple(
    f"among_stap_{i}" for i in range(1, OEFEN_STAPPEN + 1))
# the shop's clothes (make_guh_variants.py, make_clothes_icons.py and make_resources.py read these four names)
CLOTHES = tex.CLOTHES
BONES = tex.BONES
clothes = tex.clothes
icons = tex.icons
SOUNDS = {
    "among.begin": [{"name": "minecraft:block.bell.use", "type": "event", "pitch": 1.3}],
    "among.duw": [{"name": "minecraft:block.wool.fall", "type": "event", "pitch": 0.7}, {"name": "guhs:guh_ambient5", "pitch": 0.7}],
    "among.vergadering": [{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 0.6}],
    "among.stem": [{"name": "minecraft:item.book.page_turn", "type": "event", "pitch": 1.4}],
    "among.weggestemd": [{"name": "minecraft:entity.firework_rocket.launch", "type": "event", "pitch": 0.8}],
    "among.sabotage": [{"name": "minecraft:block.beacon.deactivate", "type": "event", "pitch": 1.2}],
    "among.alarm": [{"name": "minecraft:block.note_block.pling", "type": "event", "pitch": 0.5}],
    "among.taak": [{"name": "minecraft:entity.experience_orb.pickup", "type": "event", "pitch": 0.9}],
    "among.luik": [{"name": "minecraft:block.iron_trapdoor.open", "type": "event", "pitch": 0.8}],
    "among.paneel": [{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 1.6}],
    "among.snurk": [{"name": "minecraft:entity.fox.sleep", "type": "event", "pitch": 0.8}],
}
ONDERTITELS = {
    "among.begin": "De Vadsvaarder vertrekt", "among.duw": "Guh wordt in slaap geduwd", "among.vergadering": "Noodknop loeit",
    "among.stem": "Stembriefje ritselt", "among.weggestemd": "Kussen lanceert een guh", "among.sabotage": "Er gaat iets stuk",
    "among.alarm": "Knabbelalarm piept", "among.taak": "Taak is klaar", "among.luik": "Luik klappert", "among.paneel": "Paneel piept",
    "among.snurk": "Gesnurk uit een luik",
}


def build(h):
    textures(h)
    tex.vel(h)
    blokken(h)
    tex.sus_bord(h, TEXTS["block.guhs.among_sus_bord"], TEXTS["block.guhs.among_sus_bord.lore"])
    for item in ITEMS:
        h.item_model(item)
    lib.npc(h, "among_kapitein", "Kapitein-guh", hue=0.60, sat=0.55)
    lib.npc(h, "among_logboekguh", "Logboek-guh", hue=0.12, sat=0.55)
    lib.teksten(h, TEXTS)
    lib.geluid(h, SOUNDS, ONDERTITELS)
    for adv in ADVANCEMENTS:
        lib.quest_adv(h, adv)
    bouw.build(h)
    selfcheck(h)


# =====================================================================================================================
# textures
# =====================================================================================================================
def _ruis(basis, var, seed, size=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


WIT, WIT_D, ROZE, DONKER = (232, 232, 238), (188, 188, 198), (242, 140, 190), (40, 22, 48)


def _kast(seed):
    """The casing of a ship thing: white metal with a darker edge and a pink stripe."""
    a = _ruis(WIT, 3, seed)
    a[0, :, :3] = a[:, 0, :3] = (246, 246, 250)
    a[15, :, :3] = a[:, 15, :3] = WIT_D
    a[12, 1:15, :3] = ROZE
    for x, y in ((2, 2), (13, 2), (2, 9), (13, 9)):
        a[y, x, :3] = WIT_D         # rivets
    return a


def _paneel_voor():
    """The screen of the task panel: a dark screen with a little task list (ticked and open boxes) and two buttons."""
    a = _kast(8401)
    for y in range(2, 11):
        for x in range(2, 14):
            a[y, x, :3] = DONKER if y % 2 else (52, 30, 62)
    for i, y in enumerate((3, 5, 7, 9)):
        a[y, 3, :3] = (110, 220, 130) if i < 2 else (255, 150, 200)     # the boxes: two done, two to do
        for x in range(5, 12 - i):
            a[y, x, :3] = (200, 180, 215)
    a[13, 3, :3] = a[14, 3, :3] = (110, 220, 130)
    a[13, 4, :3] = a[14, 4, :3] = (110, 220, 130)
    a[13, 11, :3] = a[14, 11, :3] = (255, 110, 110)
    a[13, 12, :3] = a[14, 12, :3] = (255, 110, 110)
    return Image.fromarray(a)


def _knop_voet():
    a = _kast(8402)
    for i in range(16):             # hazard stripes on the lower edge
        a[13:16, i, :3] = (250, 210, 60) if (i // 2) % 2 == 0 else (60, 50, 50)
    return Image.fromarray(a)


def _knop_rood():
    a = _ruis((224, 54, 48), 5, 8403)
    a[0:2, :, :3] = (255, 130, 120)
    a[:, 0:2, :3] = (255, 130, 120)
    a[14:16, :, :3] = (150, 30, 30)
    a[:, 14:16, :3] = (150, 30, 30)
    for x, y in ((6, 6), (7, 5), (8, 5), (9, 6)):       # a tiny white "!" shine
        a[y, x, :3] = (255, 235, 235)
    return Image.fromarray(a)


def _luik():
    """The grate of a vent: dark slits in grey metal, a bolt in each corner."""
    a = _ruis((150, 152, 162), 4, 8404)
    a[0, :, :3] = a[:, 0, :3] = (196, 198, 208)
    a[15, :, :3] = a[:, 15, :3] = (96, 98, 108)
    for y in range(3, 13, 2):
        a[y, 3:13, :3] = (28, 24, 34)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        a[y, x, :3] = (70, 72, 82)
    return Image.fromarray(a)


def _item(rows, kleuren):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, rij in enumerate(rows):
        for x, c in enumerate(rij):
            if c != ".":
                img.putpixel((x, y), kleuren[c] + (255,))
    return img


KUSSEN = [
    "................",
    "................",
    "...oooooooooo...",
    "..orrrrrrrrrro..",
    ".orrwrrrrrrrrro.",
    ".orwrrrrrrrrrro.",
    ".orrrrrhrhrrrro.",
    ".orrrrhhhhhrrro.",
    ".orrrrhhhhhrrro.",
    ".orrrrrhhhrrrdo.",
    ".orrrrrrhrrrddo.",
    ".orrrrrrrrrdddo.",
    "..odddddddddddo.",
    "...oooooooooo...",
    "................",
    "................",
]
KAART = [
    "................",
    ".oooooooooooooo.",
    ".opppppppppppppo",
    ".opbbbpbbbpbbbpo",
    ".opbbbpbbbpbbbpo",
    ".opppgppppgppppo",
    ".opbbbpbbbpbbbpo",
    ".opbxbpbbbpbbbpo",
    ".opppgppppgppppo",
    ".opbbbpbbbpbbbpo",
    ".opbbbpbbbpbxbpo",
    ".opppppppppppppo",
    ".oooooooooooooo.",
    "................",
    "................",
    "................",
]
BRIEFJE = [
    "................",
    "...oooooooooo...",
    "...owwwwwwwwo...",
    "...owkkkkkkwo...",
    "...owwwwwwwwo...",
    "...owrwkkkkwo...",
    "...owwwwwwwwo...",
    "...owbwkkkkwo...",
    "...owwwwwwwwo...",
    "...owgwkkkkwo...",
    "...owwwwwwgwo...",
    "...owwwwwgwwo...",
    "...owwwgwgwwo...",
    "...owwwwgwwwo...",
    "...oooooooooo...",
    "................",
]


def textures(h):
    h.save(_paneel_voor(), "block", "among_taakpaneel_voor.png")
    h.save(Image.fromarray(_kast(8405)), "block", "among_taakpaneel_zij.png")
    h.save(_knop_voet(), "block", "among_noodknop_voet.png")
    h.save(_knop_rood(), "block", "among_noodknop_knop.png")
    h.save(_luik(), "block", "among_ventilatieluik.png")
    h.save(_item(KUSSEN, {"o": (120, 50, 90), "r": (245, 150, 200), "w": (255, 215, 235), "d": (205, 105, 160), "h": (200, 60, 110)}),
           "item", "among_kussen.png")
    h.save(_item(KAART, {"o": (90, 70, 50), "p": (240, 226, 190), "b": (150, 200, 235), "g": (190, 170, 130), "x": (224, 54, 48)}),
           "item", "among_saboteerkaart.png")
    h.save(_item(BRIEFJE, {"o": (120, 110, 130), "w": (250, 248, 252), "k": (170, 165, 180), "r": (216, 54, 47), "b": (47, 91, 216),
                           "g": (47, 168, 74)}), "item", "among_stembriefje.png")
    # the space suit of the guh NPCs: an existing suit texture without its colour (the renderer tints it per participant),
    # and a pale blue visor on the glasses bones
    pak = np.asarray(Image.open(os.path.join(h.TEX, "entity", "guh_clothes", "pink_onesie.png")).convert("RGBA")).astype(np.float32)
    grijs = pak[..., :3].max(-1)
    licht = np.clip(150 + (grijs - grijs[pak[..., 3] > 0].mean()) * 0.8 + 70, 120, 255)
    uit = pak.copy()
    for c in range(3):
        uit[..., c] = licht
    h.save(Image.fromarray(uit.astype(np.uint8)), "entity", "among_pakje.png")
    bril = np.asarray(Image.open(os.path.join(h.TEX, "entity", "guh_clothes", "sunglasses.png")).convert("RGBA")).astype(np.float32)
    v = bril[..., :3].max(-1) / 255.0
    vizier = bril.copy()
    vizier[..., 0] = 120 + 90 * v
    vizier[..., 1] = 200 + 50 * v
    vizier[..., 2] = 235 + 20 * v
    h.save(Image.fromarray(np.clip(vizier, 0, 255).astype(np.uint8)), "entity", "among_vizier.png")


# =====================================================================================================================
# blocks
# =====================================================================================================================
def blokken(h):
    el = h.el
    T = TEXTS
    # the task panel: a full block; the screen on the front (north), casing all around
    lib.deco(h, "among_taakpaneel", [
        el([0, 0, 0], [16, 16, 16], "#zij", faces=("down", "up", "south", "west", "east")),
        el([0, 0, 0], [16, 16, 16], "#voor", faces=("north",)),
    ], {"zij": "guhs:block/among_taakpaneel_zij", "voor": "guhs:block/among_taakpaneel_voor", "particle": "guhs:block/among_taakpaneel_zij"},
        T["block.guhs.among_taakpaneel"], T["block.guhs.among_taakpaneel.lore"])
    # the emergency button: a hazard-striped foot with a big red button on it
    lib.deco(h, "among_noodknop", [
        el([3, 0, 3], [13, 3, 13], "#voet"),
        el([5, 3, 5], [11, 5, 11], "#voet"),
        el([4.5, 5, 4.5], [11.5, 8, 11.5], "#knop"),
    ], {"voet": "guhs:block/among_noodknop_voet", "knop": "guhs:block/among_noodknop_knop", "particle": "guhs:block/among_noodknop_voet"},
        T["block.guhs.among_noodknop"], T["block.guhs.among_noodknop.lore"])
    # the vent: a thin grate on the floor
    lib.deco(h, "among_ventilatieluik", [
        el([1, 0, 1], [15, 2, 15], "#luik"),
    ], {"luik": "guhs:block/among_ventilatieluik", "particle": "guhs:block/among_ventilatieluik"},
        T["block.guhs.among_ventilatieluik"], T["block.guhs.among_ventilatieluik.lore"])


# =====================================================================================================================
def selfcheck(h):
    lib.controleer(h, "guhpixel_among", blokken=BLOKKEN, items=BLOKKEN + ITEMS + tuple(CLOTHES), keys=TEXTS, templates=(bouw.NAME,))
    problems = []
    for f in [f"advancement/quest/{a}.json" for a in ADVANCEMENTS] + ["guhpixel/among_schip.txt"] + [f"loot_table/blocks/{b}.json" for b in BLOKKEN]:
        if not os.path.exists(f"{h.D}/{f}"):
            problems.append(f"missing data/guhs/{f}")
    for t in ("entity/among_pakje.png", "entity/among_vizier.png", "entity/npc_among_kapitein.png", "entity/npc_among_logboekguh.png",
              "gui/among_taken.png", "block/among_sus_bord.png"):
        if not os.path.exists(os.path.join(h.TEX, t)):
            problems.append(f"missing texture {t}")
    for kid in bouw.KAMERS:
        if f"gui.guhs.among.kamer.{kid}" not in TEXTS:
            problems.append(f"no name for room {kid}")
    for soort in bouw.TAAK_SOORTEN + ("herstel_licht", "herstel_alarm"):
        for key in (f"gui.guhs.among.taak.{soort}", f"gui.guhs.among.taak.{soort}.bezig"):
            if key not in TEXTS:
                problems.append(f"missing {key}")
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "among")
    # the task mini-games: the kinds of the ship table are the kinds Java knows, each with its texts
    taken_src = open(os.path.join(java, "Taken.java"), encoding="utf-8").read()
    for soort in bouw.TAAK_SOORTEN:
        if f'"{soort}"' not in taken_src:
            problems.append(f"task kind {soort} has no mini-game in Taken.java")
    for i in range(1, OEFEN_STAPPEN + 1):
        if f"gui.guhs.among.grap.stap.{i}" not in TEXTS:
            problems.append(f"the oefenrondje has no text for step {i}")
    if f"STAPPEN = {OEFEN_STAPPEN};" not in open(os.path.join(java, "OefenSessie.java"), encoding="utf-8").read():
        problems.append("OefenSessie.STAPPEN is not the number of steps of the oefenrondje")
    kleren = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    for c in CLOTHES:
        if f"    {c.upper()}(" not in kleren:
            problems.append(f"clothes {c} is not in GuhClothes")
        if f"item.guhs.{c}" not in TEXTS:
            problems.append(f"clothes {c} has no name")
    thuis_src = open(os.path.join(java, "AmongThuis.java"), encoding="utf-8").read()
    agenda = sum(1 for k in TEXTS if k.startswith("gui.guhs.among.thuis.agenda."))
    besluiten = sum(1 for k in TEXTS if k.startswith("gui.guhs.among.thuis.besluit.") and k[-1].isdigit())
    if f"AGENDA_TEKSTEN = {agenda}," not in thuis_src or f"BESLUITEN = {besluiten};" not in thuis_src:
        problems.append("AmongThuis does not know how many agenda points and decisions there are")
    if "STICKERS = %d;" % sum(1 for k in TEXTS if k.startswith("gui.guhs.among.sus_bord.")) not in open(
            os.path.join(java, "AmongBlokken.java"), encoding="utf-8").read():
        problems.append("AmongBlokken.SusBord.STICKERS is not the number of sticker lines")
    slice_src = open(os.path.join(java, "AmongSlice.java"), encoding="utf-8").read()
    if f"new Vec3i({bouw.W}, {bouw.H}, {bouw.D})" not in slice_src:
        problems.append("AmongSlice.MAAT is not the size of the ship")
    if f"new Vec3({bouw.START[0]}, {bouw.START[1]}, {bouw.START[2]})" not in slice_src:
        problems.append("AmongSlice.ARENA does not start where the ship table says")
    kleur_src = open(os.path.join(java, "model", "Kleur.java"), encoding="utf-8").read()
    for kleur in tekst.KLEUREN:
        if f'"{kleur}"' not in kleur_src:
            problems.append(f"colour {kleur} is not in Kleur.java")
    uitspraak_src = open(os.path.join(java, "model", "Uitspraak.java"), encoding="utf-8").read()
    for soort in tekst.UITSPRAKEN:
        if soort.upper() + "," not in uitspraak_src and soort.upper() + ";" not in uitspraak_src:
            problems.append(f"statement kind {soort} is not in Uitspraak.Soort")
    if problems:
        raise SystemExit("guhpixel_among self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    q, adv = fq.q, fq.adv
    y = 0
    q("among_klaar", "Het oefenrondje", "Praat in de lobby van &dGuhpixel&r met de &bKapitein-guh&r en vlieg een oefenrondje mee op "
      "&dDe Vadsvaarder&r. Doe je taak, kijk wat de rest van de crew uitspookt en zoek de Mika. Er schijnt er een aan boord te zijn, njeg. "
      "De eerste keer krijg je 100 muntjes en een aandenken, en daarna mag je het echte spel in.",
      "guhs:among_sus_bord", [adv("among_klaar")], rewards=(("guhs:kaas_knabbels", 8),), x=-2, y=y, xp=100)
    q("among_ronde", "Aan boord van De Vadsvaarder", "Praat met de &bKapitein-guh&r in de Guhpixel-lobby en speel een hele ronde &dAmong Guhs&r. "
      "Crew: doe je taken en stem de Mika weg. Mika: duw iedereen in slaap zonder dat iemand het ziet. Lege plekken worden gevuld met guhs in "
      "ruimtepakjes. Niemand doet elkaar pijn: wie slaapt, droomt gewoon verder.",
      "guhs:among_noodknop", [adv("among_ronde")], rewards=(("guhs:kaas_knabbels", 8),), x=0, y=y, shape="circle", xp=50)
    q("among_crew_winst", "Taakjes af, Mika weg", "Win een ronde als &bcrew&r: maak met zijn allen alle taken af, of stem de Mika weg in een "
      "vergadering. Luister goed naar wat de guhs zeggen: ze vertellen wat ze echt gezien hebben. Meestal, njeg.",
      "guhs:among_taakpaneel", [adv("among_crew_winst")], rewards=(("guhs:kaas_knabbels", 12),), x=2, y=y - 1, xp=50)
    q("among_mika_winst", "Stiekem een Mika", "Win een ronde als &cMika&r. Duw alleen als niemand kijkt, kruip door de luiken, doe het licht uit "
      "en zeg in de vergadering met een stalen snoet dat je in de Kantine was.",
      "guhs:among_kussen", [adv("among_mika_winst")], rewards=(("guhs:kaas_knabbels", 12),), x=2, y=y + 1, xp=50)
    q("among_lastig", "Twee Mika's aan boord", "Speel een ronde op &6Lastig&r: tien deelnemers, twee Mika's en anderhalf keer zoveel muntjes. "
      "De leider van de wachtrij kiest het niveau.",
      "guhs:among_ventilatieluik", [adv("among_lastig")], rewards=(("guhs:kaas_knabbels", 16),), x=4, y=y, shape="hexagon", xp=100)
    q("among_pakje", "Rood is sus", "Na je eerste echte ronde verkoopt de &dVerkoper-guh&r &druimtepakjes&r in acht kleuren en zes hoedjes "
      "voor je guhs. Koop het &cRode ruimtepakje&r en trek het een guh aan. Die heeft vanaf nu altijd alles gedaan, njeg.",
      "guhs:among_ruimtepakje_rood", [fq.item("guhs:among_ruimtepakje_rood")], rewards=(("guhs:kaas_knabbels", 8),), x=6, y=y - 1, xp=50)
    q("among_thuis", "De Vadsvaarder thuis", "Haal het schip in huis: koop bij de &dVerkoper-guh&r de &dNoodknop&r, het &dVentilatieluik&r en "
      "het &dTaakjes-paneel&r. De Noodknop roept je guhs bij elkaar voor een vergadering (er wordt niets besloten), uit het luik gluurt af en "
      "toe een guh, en op het paneel doe je een taakje. Beloning: niks, njeg.",
      "guhs:among_noodknop", [fq.item("guhs:among_noodknop"), fq.item("guhs:among_ventilatieluik"), fq.item("guhs:among_taakpaneel")],
      rewards=(("guhs:kaas_knabbels", 12),), x=6, y=y + 1, xp=100)
