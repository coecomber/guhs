"""
bbq2 (ring-h6): chapter 6 of the Knabbelring: De Frituurberg (DESIGN_130 4). Java: feature/ringh6.

  ring_h6_bouw.py    the mountain (structure guhs:frituurberg, a burcht of 3 x 3 tiles) with its self-check, the test mountain
                     of RingH6GameTests, and the pictures
  ring_h6_tekst.py   every Dutch text
  ring_h6_wiki.py    the wiki entry (CONTRACT_130 2.4; not built here)
  here               the lock of the cages, the piece of fried ring, the crispy Smikagol's texture, the structure set, the
                     named spots Java reads (data/guhs/ringh6/berg.json + berg_test.json), the questline, the narrator card,
                     the three cutscenes, the advancements and the FTB section "De Frituurberg"

The structure is the last of the story chain: exactly one per world, in the Rookdelta, 250-500 blocks from the Zwarte
Roosterpoort (gegarandeerd rond). As long as ring-h5 has not declared that structure (this slice built alone) it stands in
sector 13 of the ring 500-900 instead, so a dev world of this branch has a mountain too.
"""
import json
import os

from PIL import Image

from features import bbq2
from features import ring_h6_bouw as bouw
from features import ring_h6_tekst as tekst
from features import verhaal_motor
from features import wereld

FTB_LINEAIR = True
FTB_SECTIES = [("ring_h6", "De Frituurberg", "guh:sam_guh", None)]

LIJN = "ring_h6"
STRUCTUUR = bouw.NAAM
SALT = 21302101
VOORRANG = 260
VORIGE = "zwarte_roosterpoort"            # the chapter before: the mountain lies in a ring around it


# =====================================================================================================================
# textures and models
# =====================================================================================================================
SLOT = ["kkkkkkkkkkkkkkkk", "kiikiikiikiikiik", "kiikiikiikiikiik", "kiikiikggkiikiik", "kiikiigkkgiikiik", "kiikiigkkgiikiik",
        "kiikgggggggkiiik", "kiikgGGGGGgkiiik", "kiikgGGkGGgkiiik", "kiikgGGkGGgkiiik", "kiikgGGGGGgkiiik", "kiikgggggggkiiik",
        "kiikiikiikiikiik", "kiikiikiikiikiik", "kiikiikiikiikiik", "kkkkkkkkkkkkkkkk"]
STUKJE = ["................", "................", "......kkkk......", "....kkGGGGkk....", "...kGGgGGbGGk...", "..kGGbGGGGGGk...",
          "..kGGGkkkkkkk...", ".kGgGk..........", ".kGGGk..........", ".kGGbk..........", ".kGGGGk.........", "..kGgGGk........",
          "...kkGGk........", ".....kk.........", "................", "................"]


def textures(h):
    h.save(h.grid(SLOT, {"k": (38, 36, 44, 255), "i": (86, 88, 100, 255), "g": (150, 100, 20, 255), "G": (250, 200, 70, 255)}), "block", "ringh6_kooislot.png")
    h.w(f"{h.A}/models/block/ringh6_kooislot.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/ringh6_kooislot"}})
    h.w(f"{h.A}/blockstates/ringh6_kooislot.json", {"variants": {f"nr={n}": {"model": "guhs:block/ringh6_kooislot"} for n in (1, 2, 3)}})
    h.w(f"{h.A}/models/item/ringh6_kooislot.json", {"parent": "guhs:block/ringh6_kooislot"})
    # the piece of fried ring: a golden brown wedge with crispy bubbles
    h.save(h.grid(STUKJE, {"k": (120, 70, 16, 255), "G": (226, 150, 44, 255), "g": (250, 206, 110, 255), "b": (170, 100, 26, 255)}),
           "item", "ringh6_stukje_knabbelring.png")
    h.item_model("ringh6_stukje_knabbelring")
    # the crispy Smikagol: ring-kern's Smikagol, deep-fried (golden brown fur, darker crispy edges, his eyes stay as they are)
    bron = os.path.join(h.A, "textures", "entity", "smikagol.png")
    if not os.path.exists(bron):
        raise SystemExit("ring_h6: textures/entity/smikagol.png is missing (ring.py runs before this module and writes it)")
    img = Image.open(bron).convert("RGBA")
    px = img.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            licht = (r * 0.3 + g * 0.59 + b * 0.11) / 255.0
            if licht > 0.9 or licht < 0.1 or (b > r + 30 and b > 120):
                continue                                         # (the whites and pupils of his eyes, the blue of his stare)
            bubbel = ((x * 7 + y * 13) % 11 == 0) or ((x * 5 + y * 3) % 17 == 0)
            basis = (255, 190, 70) if not bubbel else (176, 104, 26)
            f = 0.35 + 0.75 * licht
            px[x, y] = (min(255, int(basis[0] * f)), min(255, int(basis[1] * f)), min(255, int(basis[2] * f)), a)
    h.save(img, "entity", "ringh6_krokante_smikagol.png")


def texts(h):
    for key, text in tekst.TEKSTEN.items():
        h.lang(key, text, text)
    verhaal_motor.verhaallijn(h, LIJN, tekst.LIJN_NAAM, tekst.LIJN_UITLEG, stappen=tekst.STAPPEN, klaar=tekst.KLAAR, kort=tekst.KORT)
    for id, (titel, regels, namen) in tekst.SCENES.items():
        verhaal_motor.scene(h, id, titel, regels, namen)
    verhaal_motor.vertelkaart(h, LIJN, tekst.KAART_TITEL, tekst.KAART_REGELS, kaart())


def kaart():
    """The map of the narrator card: the Asdal with the black gate on the left, the Rookdelta with its smoke vents, the sauce
    sea, and the Frituurberg with the road that winds up to it."""
    k = verhaal_motor.Kaart(seed=2130210)
    k.land([(6, 150), (4, 70), (30, 36), (96, 18), (180, 14), (244, 30), (250, 96), (236, 146), (120, 154)], (176, 150, 132, 255))
    k.water([(150, 154), (176, 118), (214, 110), (250, 122), (250, 156)], (232, 150, 60, 255))
    k.water([(92, 60), (120, 52), (128, 72), (104, 82)], (232, 150, 60, 255))
    k.toren(34, 104, 18, (40, 36, 44, 255))
    k.toren(26, 108, 10, (60, 54, 62, 255))
    k.toren(42, 108, 10, (60, 54, 62, 255))
    k.tekst(10, 128, "Roosterpoort")
    for x, y, hoog in ((78, 110, 9), (96, 124, 7), (118, 100, 8), (70, 78, 7)):
        k.berg(x, y, hoog, (112, 104, 110, 255))
    k.vulkaan(184, 74, 44)
    k.tekst(158, 92, "Frituurberg")
    k.tekst(92, 136, "Rookdelta")
    k.pad([(34, 104), (58, 96), (84, 100), (112, 88), (140, 86), (160, 78), (172, 70), (180, 60), (184, 46)])
    k.kruis(184, 42)
    k.kompasroos(232, 136)
    return k


def advancements(h):
    for name, parent, icon, frame, titel, text in tekst.ADVANCEMENTS:
        bbq2.zichtbaar(h, "knabbelring", name, parent, icon, frame, titel, text)
        bbq2.verborgen(h, name)
    for name in tekst.VERBORGEN:
        bbq2.verborgen(h, name)


def structuur(h):
    b = bouw.berg(h)
    problems = bouw.check(b)
    if problems:
        raise SystemExit("ring_h6: the Frituurberg is not right:\n  " + "\n  ".join(problems))
    nx, nz, _saved = b.save_tiles(STRUCTUUR)
    titel, tooltip = tekst.STRUCTUUR
    keten = VORIGE in wereld.STRUCTUREN and wereld.STRUCTUREN[VORIGE]["gegarandeerd"] is not None and (wereld.STRUCTUREN[VORIGE]["voorrang"] or 0) > VOORRANG
    gegarandeerd = dict(rond=VORIGE, min=250, max=500) if keten else dict(sector=13, min=500, max=900)
    if not keten:
        print(f"ring_h6: {VORIGE} is not declared (yet): the Frituurberg stands in sector 13 instead of around it")
    wereld.bbq_structuur(h, STRUCTUUR, soort="burcht", titel=titel, tooltip=tooltip, biomes=["rookdelta"], salt=SALT, gegarandeerd=gegarandeerd,
                         voorrang=VOORRANG, kompas=None,
                         burcht=dict(placement="paleis", tiles_x=nx, tiles_z=nz, tile_size=bouw.sb.TILE, anchor=bouw.ANKER, min_y=33, max_y=33, reach=30))
    # nothing spawns on the mountain: the coals and Smikagol are trouble enough
    h.patch_json(f"{h.D}/worldgen/structure/{STRUCTUUR}.json", lambda d: d.update(spawn_overrides={
        "monster": {"bounding_box": "piece", "spawns": []}, "creature": {"bounding_box": "piece", "spawns": []}}))
    verhaal_motor.sluier(h, STRUCTUUR)
    h.w(f"{h.D}/ringh6/berg.json", bouw.gegevens(b.plekken, b.routes, (bouw.W, bouw.H, bouw.D), bouw.ANKER))
    s, plekken, routes = bouw.test_berg(h)
    s.save("ringh6_test_berg")
    h.w(f"{h.D}/ringh6/berg_test.json", bouw.gegevens(plekken, routes, s.size, (0, 0, 0)))
    return b


def selfcheck(h, b):
    problems = []
    for key in ("block.guhs.ringh6_kooislot", "item.guhs.ringh6_stukje_knabbelring", "entity.guhs.ringh6_rookguh", f"structure.guhs.{STRUCTUUR}",
                f"gui.guhs.verhalen.{LIJN}.kort.6", f"gui.guhs.verhaal.kaart.{LIJN}.regel.3", "scene.guhs.ringh6_frituur.dutje",
                "scene.guhs.ringh6_feest.einde", "quest.guhs.ring.terug.ringh6_frituur"):
        if key not in h.NL:
            problems.append(f"lang {key}")
    for pad in (f"{h.D}/structure/{STRUCTUUR}/stuk_1_1.nbt", f"{h.D}/structure/ringh6_test_berg.nbt", f"{h.D}/ringh6/berg.json",
                f"{h.D}/ringh6/berg_test.json", f"{h.A}/textures/entity/ringh6_krokante_smikagol.png",
                f"{h.A}/textures/gui/verhaal/kaart_{LIJN}.png", f"{h.D}/worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json"):
        if not os.path.exists(pad):
            problems.append(f"file {pad}")
    # the finale's cutscenes are written against these spots (feature/ringh6/Finale.java): the balcony, the pool, the basket
    blocks = b.s.blocks
    rx, ry, rz = bouw.RAND
    verwacht = {(rx, ry - 1, rz): None, (rx + 2, ry - 1, rz): None, (rx + 7, bouw.POEL_TOP, rz): "guhs:kaasfrituursaus",
                (rx + 4, bouw.POEL_TOP, rz - 3): "guhs:roosterijzer_pilaar", (rx, ry, rz): "minecraft:air", (rx + 7, ry + 16, rz): "guhs:roosterijzer_pilaar"}
    for cel, naam in verwacht.items():
        staat = blocks.get(cel, (None,))[0]
        if staat is None or (naam is not None and staat != naam) or (naam is None and staat in ("minecraft:air",)):
            problems.append(f"the finale expects {naam or 'a floor'} at {cel}, there is {staat}")
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ringh6", "RingH6Feature.java")
    if os.path.exists(java):
        bron = open(java, encoding="utf-8").read()
        if f".stappen({len(tekst.STAPPEN)})" not in bron:
            problems.append(f"RingH6Feature.LIJN does not have {len(tekst.STAPPEN)} steps")
    if problems:
        raise SystemExit("ring_h6 self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    textures(h)
    texts(h)
    advancements(h)
    b = structuur(h)
    selfcheck(h, b)


# =====================================================================================================================
# FTB: the section "De Frituurberg" (chapter guhs_knabbelring). The chapter is ONE chain in quest order (FTB_LINEAIR): the
# first quest names no deps (it comes after the last quest of ring_h5 by itself); the two side quests name a quest of
# this chain, so they are a branch that locks nothing.
# =====================================================================================================================
def ftb(fq):
    laatste = len(tekst.FTB) - 1
    for i, (key, titel, text, icon, adv) in enumerate(tekst.FTB):
        fq.q(key, titel, text, icon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 16 if i == laatste else 8),),
             shape="gear" if i in (0, laatste) else None, xp=250 if i == laatste else 0)
    for key, titel, text, icon, adv, na in tekst.FTB_ZIJ:
        fq.q(key, titel, text, icon, [fq.adv(adv)], deps=[na])
