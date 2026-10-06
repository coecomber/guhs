"""
bbq2 (ring-h4): chapter 4 of the Knabbelring, "De Spiegel van Guhladriel" (DESIGN_130 4). Java: feature/ringh4.

  ring_h4_bouw.py      the structure guhs:guhladriel_boomstad: the tree city Caras Guhladhon, the river Guhduin and the two
                       giant guh statues of the Arguhnath in one build (192 x 52 x 96, tiles of 32); its spots and the boat's
                       path go to data/guhs/ringh4/plekken.json, which the Java side reads (feature/ringh4/Plekken)
  ring_h4_modellen.py  the elf boat (entity guhs:ringh4_elfenbootje) and the Spiegel van Guhladriel (block guhs:ringh4_spiegel)
  ring_h4_tekst.py     every Dutch text
  ring_h4_wiki.py      the wiki entries (CONTRACT_130 2.4; not built here)
  here                 the questline ring_h4 (7 steps), the narrator card and its map, the cutscene's texts, the structure set
                       (its own type guhs:ringh4_boomstad: a guhs:burcht that lies on a cave floor, feature/ringh4/
                       BoomstadStructure), the sluier, the advancements, the FTB section "De Spiegel van Guhladriel"

The city stands exactly once per world: `rond` the Mijnen van Knabbelmoria at 250-500 blocks (CONTRACT_130 3). As long as
ring-h3 has not declared that structure (this branch on its own) the copy lies in sector 13 of the ring around 0,0 instead,
so the chapter can be played and checked without the mine.
"""
import json
import os

from features import bbq2
from features import ring_h4_bouw as bouw
from features import ring_h4_modellen as modellen
from features import ring_h4_tekst as tekst
from features import verhaal_motor
from features import wereld

FTB_LINEAIR = True
FTB_SECTIES = [("ring_h4", tekst.NAAM, "npc:guhladriel", None)]

STRUCTUUR = bouw.NAAM
TYPE = "guhs:ringh4_boomstad"
SALT = 21301901
VOORRANG = 280                # (the story chain: guhvendel 300 ... frituurberg 260, in chapter order)
VORIGE = "knabbelmoria"       # the chapter before: the city lies around its guaranteed copy
# the dry biomes; outside the preferred one (the tag guhs:ringh4_voorkeur) only one start chunk in ANDERS is accepted, so
# the search lands in a Satébos whenever there is one in the ring and still finds a spot when there is none
BIOMES = ["satebos", "worstenwoud", "houtskoolvlakte", "asdal"]
VOORKEUR = ["satebos"]
ANDERS = 12


def kaart():
    """The map of the narrator card: the mountains of the mine in the west, the golden wood with the great tree, the river
    that winds east between the two guh kings."""
    k = verhaal_motor.Kaart(seed=2130190)
    k.land([(6, 150), (4, 60), (30, 22), (110, 12), (200, 18), (250, 44), (252, 130), (214, 152), (90, 156)], (190, 160, 136, 255))
    for x, y, hoog in ((26, 62, 20), (44, 54, 24), (60, 66, 16), (34, 80, 12)):
        k.berg(x, y, hoog, (112, 104, 110, 255))
    k.tekst(14, 86, "Knabbelmoria")
    k.rivier([(150, 96), (172, 104), (190, 92), (206, 78), (226, 86), (250, 100)], (236, 170, 60, 255), 4)
    k.bos(84, 60, 62, 62, 22, (214, 168, 60, 255))
    for x, y in ((112, 96), (124, 84), (104, 82), (132, 100)):
        k.boom(x, y, (236, 196, 80, 255))
    k.huisje(118, 92, (236, 226, 200, 255))
    k.tekst(92, 126, "Guhlórien")
    for x, y in ((204, 70), (208, 96)):
        k.toren(x, y, 14, (120, 126, 120, 255))
    k.tekst(190, 110, "Arguhnath")
    k.pad([(48, 70), (70, 84), (96, 92), (116, 94)])
    k.kruis(118, 96)
    k.kompasroos()
    return k


def texts(h):
    for key, text in tekst.TEKSTEN.items():
        h.lang(key, text, text)
    verhaal_motor.verhaallijn(h, "ring_h4", tekst.NAAM, tekst.UITLEG, stappen=tekst.STAPPEN, klaar=tekst.KLAAR, kort=tekst.KORT)
    verhaal_motor.vertelkaart(h, "ring_h4", tekst.NAAM, tekst.KAART_REGELS, kaart())
    verhaal_motor.scene(h, "ringh4_spiegel", tekst.SCENE_TITEL, tekst.SCENE, namen=tekst.SCENE_NAMEN)


def advancements(h):
    for name, parent, icon, frame, titel, text in tekst.ADVANCEMENTS:
        bbq2.zichtbaar(h, "knabbelring", name, parent, icon, frame, titel, text)
        bbq2.verborgen(h, name)
    for name in tekst.VERBORGEN:
        bbq2.verborgen(h, name)


def structuur(h):
    stad = bouw.bouw(h)
    problems = bouw.check(stad)
    if problems:
        raise SystemExit("ring_h4: the tree city is not right:\n  " + "\n  ".join(problems))
    nx, nz, bewaard = bouw.save(h, stad)
    h.w(f"{h.D}/ringh4/plekken.json", bouw.plekken_json(stad))
    titel, tooltip = tekst.STRUCTUUR
    vorige = wereld.STRUCTUREN.get(VORIGE)
    gegarandeerd = dict(rond=VORIGE, min=250, max=500) if vorige and vorige["gegarandeerd"] is not None else dict(sector=13, min=300, max=800)
    wereld.bbq_structuur(h, STRUCTUUR, soort="burcht", titel=titel, tooltip=tooltip, biomes=BIOMES, salt=SALT, gegarandeerd=gegarandeerd,
                         voorrang=VOORRANG, kompas=None,
                         burcht=dict(placement="paleis", tiles_x=nx, tiles_z=nz, tile_size=bouw.TILE, anchor=bouw.ANKER, min_y=0, max_y=0, reach=0))

    def eigen_type(d):
        # our own placement (BoomstadStructure): on a cave floor, the gate and the way on both at a floor; the land meets its edges
        for k in ("placement", "min_y", "max_y", "reach"):
            d.pop(k, None)
        d.update(type=TYPE, terrain_adaptation="beard_thin", poort=[int(c) for c in stad.plek["poort"]], uitgang=[int(c) for c in stad.plek["uitgang"]],
                 hoogte=bouw.SIZE[1], voorkeur="guhs:ringh4_voorkeur", anders=ANDERS,
                 spawn_overrides={"monster": {"bounding_box": "piece", "spawns": []}})

    h.patch_json(f"{h.D}/worldgen/structure/{STRUCTUUR}.json", eigen_type)
    h.w(f"{h.D}/tags/worldgen/biome/ringh4_voorkeur.json", {"values": [f"guhs:{b}" for b in VOORKEUR]})
    # make_v2.bouwruimte(): the reach of the tiles from the anchor, as for a guhs:burcht
    h.RUIMTE_HOOKS[TYPE] = lambda s, jigsaw_reach: max(s["anchor"][0], s["tiles_x"] * s["tile_size"] - 1 - s["anchor"][0],
                                                       s["anchor"][2], s["tiles_z"] * s["tile_size"] - 1 - s["anchor"][2]) + 1
    verhaal_motor.sluier(h, STRUCTUUR)
    bouw.test_template(h).save("ringh4_test_kamer")
    return stad, (nx, nz, bewaard)


def selfcheck(h, stad, tegels):
    problems = modellen.check(h)
    nx, nz, bewaard = tegels
    for i, j in bewaard:
        if not os.path.exists(f"{h.D}/structure/{STRUCTUUR}/stuk_{i}_{j}.nbt"):
            problems.append(f"tile {i}_{j}")
    if len(bewaard) < 10:
        problems.append(f"only {len(bewaard)} tiles")
    s = json.load(open(f"{h.D}/worldgen/structure/{STRUCTUUR}.json", encoding="utf-8"))
    if s["type"] != TYPE or s["tiles_x"] != nx or s["tiles_z"] != nz or s["anchor"] != list(bouw.ANKER) or "poort" not in s:
        problems.append(f"the structure json is not ours: {s}")
    if not os.path.exists(f"{h.D}/worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json") or os.path.exists(f"{h.D}/worldgen/structure_set/{STRUCTUUR}.json"):
        problems.append("the city must have a guaranteed set and no random spread")
    plekken = json.load(open(f"{h.D}/ringh4/plekken.json", encoding="utf-8"))
    # the spots feature/ringh4 asks for by name
    for naam in ("poort", "leguhlas_poort", "zaal", "gast", "gast_vuur", "spiegel", "dal", "steiger", "leguhlas_steiger", "gimguh_steiger", "aanleg",
                 "boot", "boot_deco_1", "boot_deco_2", "boot_terug", "uitgang"):
        if naam not in plekken["plekken"]:
            problems.append(f"plekken.json misses '{naam}'")
    if len(plekken["route"]) < 10:
        problems.append("the boat's path is too short")
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ringh4", "RingH4Feature.java"), encoding="utf-8").read()
    if f".stappen({len(tekst.STAPPEN)})" not in java:
        problems.append(f"RingH4Feature.LIJN must have {len(tekst.STAPPEN)} steps")
    if f"Verteller.registreer(KAART, {len(tekst.KAART_REGELS)}," not in java:
        problems.append(f"the narrator card must have {len(tekst.KAART_REGELS)} lines in RingH4Feature")
    scene = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ringh4", "Spiegel.java"), encoding="utf-8").read()
    import re
    for key in re.findall(r'\.zeg\(\d+, [^,]+, "([a-z_]+)"', scene):
        if key not in tekst.SCENE:
            problems.append(f"the mirror scene says '{key}', which has no text")
    for key in tekst.SCENE:
        if f'"{key}"' not in scene:
            problems.append(f"the scene text '{key}' is never said")
    for key in ("structure.guhs." + STRUCTUUR, "gui.guhs.verhalen.ring_h4.kort.6", "gui.guhs.verhaal.kaart.ring_h4.regel.3", "scene.guhs.ringh4_spiegel.oog",
                "quest.guhs.ringh4.vaart.koningen", "block.guhs.ringh4_spiegel", "entity.guhs.ringh4_elfenbootje"):
        if key not in h.NL:
            problems.append(f"lang {key}")
    if problems:
        raise SystemExit("ring_h4 self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    texts(h)
    advancements(h)
    stad, tegels = structuur(h)
    selfcheck(h, stad, tegels)


def ftb(fq):
    """The section "De Spiegel van Guhladriel" of the chapter guhs_knabbelring. The chapter is linear (FTB_LINEAIR): one chain in
    quest order, so the first quest names no deps (it comes after the last quest of ring_h3 by itself) and the Toren van
    Sausuman can name the last one here (ring_h4_vaart) as its dependency."""
    for i, (key, titel, text, icon, stap) in enumerate(tekst.FTB):
        laatste = i == len(tekst.FTB) - 1
        fq.q(key, titel, text, icon, [fq.adv(f"ring_h4_stap_{stap}")], rewards=(("guhs:kaas_knabbels", 12 if laatste else 6),),
             shape="gear" if laatste else None, xp=120 if laatste else 0)
