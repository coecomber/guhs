"""
bbq2 (ring-h5): chapter 5 of the Knabbelring, "De Zwarte Roosterpoort". Java: feature/ringh5. Manual:
guhs_work130/reports/slice_ring-h5.md.

  ring_h5_bouw.py      the valley (structure guhs:zwarte_roosterpoort, a guhs:burcht in 3 x 3 tiles) and PLEKKEN, the single
                       source of every spot the Java side works with; the game test room
  ring_h5_modellen.py  the Eye and the Roosterwachter (models, animations, textures), the grate of the gate, the light of the
                       Eye on the ground, the statuette that blinks
  ring_h5_tekst.py     every Dutch text
  ring_h5_wiki.py      the wiki entries (CONTRACT_130 2.4; not built here)
  here                 the structure set (one copy per world, a walk away from the tree city of chapter 4), the sluier's map
                       entry, the questline's texts, the narrator card with its drawn map, the three scenes, the
                       advancements, the FTB section

Fixed ids of CONTRACT_130 7 that this module owns: oog_van_sausron (entity), oog_van_sausron_beeldje (block).
"""
import json
import os

from features import bbq2
from features import ring_h5_bouw as bouw
from features import ring_h5_modellen as modellen
from features import ring_h5_tekst as tekst
from features import verhaal_motor
from features import wereld

# (the chapter guhs_knabbelring is one chain of quests in quest order: ring.py; this module's first quest names no deps)
FTB_LINEAIR = True
FTB_SECTIES = [("ring_h5", "De Zwarte Roosterpoort", "npc:smikagol", None)]

STRUCTUUR = bouw.NAAM
SALT = 21302001
VOORRANG = 270                 # (CONTRACT_130 3: the ring structures 300 .. 260 in chapter order)
VORIGE = "guhladriel_boomstad"  # the structure of chapter 4: this one stands 250-500 blocks from it
# where the valley floor may come to stand (world y of the ground layer): its roof then reaches 57 higher at most
MIN_Y, MAX_Y = 40, 58


def structuur(h):
    b, problems = bouw.bouw(h)
    if problems:
        raise SystemExit("ring_h5: the Zwarte Roosterpoort is not right:\n  " + "\n  ".join(problems))
    nx, nz, saved = b.save_tiles(STRUCTUUR)
    titel, tooltip = tekst.STRUCTUUR
    if VORIGE in wereld.STRUCTUREN and wereld.STRUCTUREN[VORIGE]["gegarandeerd"] is not None:
        plek = dict(rond=VORIGE, min=250, max=500)
    else:
        # (this branch alone: chapter 4 is not built yet. A spot of its own in the ring, so the valley can be looked at;
        # once ring_h4 declares its tree city, which runs before this module, the valley hangs on it by itself)
        plek = dict(sector=13, min=250, max=700)
    wereld.bbq_structuur(h, STRUCTUUR, soort="burcht", titel=titel, tooltip=tooltip, biomes=["asdal"], salt=SALT, gegarandeerd=plek,
                         voorrang=VOORRANG, kompas=None,
                         burcht=dict(placement="brug", tiles_x=nx, tiles_z=nz, tile_size=bouw.sb.TILE, anchor=bouw.ANKER, min_y=MIN_Y, max_y=MAX_Y, reach=44))
    # nothing spawns by itself in the valley: its creatures are the Eye, the guards and the two riders
    h.patch_json(f"{h.D}/worldgen/structure/{STRUCTUUR}.json", lambda d: d.update(spawn_overrides={
        cat: {"bounding_box": "piece", "spawns": []} for cat in ("monster", "creature", "ambient")}))
    verhaal_motor.sluier(h, STRUCTUUR)
    bouw.test_template(h).save("ringh5_test_kamer")
    return saved


def kaart():
    """The drawn map of the narrator card: the valley from the mouth (below) to the wall with its gate and the tower."""
    k = verhaal_motor.Kaart(seed=21302070)
    as_ = (196, 190, 180, 255)
    k.land([(40, 152), (30, 100), (44, 52), (84, 30), (172, 30), (212, 52), (226, 100), (216, 152)], kleur=as_)
    # the cliffs
    for x, y, hoog in ((26, 120, 16), (30, 84, 18), (44, 50, 16), (228, 120, 16), (224, 84, 18), (212, 50, 16), (70, 30, 12), (186, 30, 12)):
        k.berg(x, y, hoog, (120, 104, 100, 255))
    # the wall with the gate and its two towers, the tower of the Eye behind it
    k.d.rectangle((52, 54, 204, 62), fill=(70, 60, 66, 255), outline=k.INKT)
    for x in range(54, 204, 6):
        k.d.rectangle((x, 51, x + 2, 54), fill=(70, 60, 66, 255))
    k.d.rectangle((118, 46, 138, 62), fill=(232, 150, 60, 255), outline=k.INKT)
    for x in range(121, 138, 4):
        k.d.line((x, 47, x, 61), fill=k.INKT)
    k.toren(110, 62, 16)
    k.toren(146, 62, 16)
    k.d.rectangle((122, 16, 134, 46), fill=(60, 50, 58, 255), outline=k.INKT)
    k.d.ellipse((114, 4, 124, 14), fill=(60, 50, 58, 255), outline=k.INKT)
    k.d.ellipse((132, 4, 142, 14), fill=(60, 50, 58, 255), outline=k.INKT)
    k.d.rectangle((116, 10, 140, 26), fill=(60, 50, 58, 255), outline=k.INKT)
    k.d.ellipse((120, 13, 136, 23), fill=(250, 170, 40, 255), outline=(150, 40, 20, 255))
    k.d.line((128, 14, 128, 22), fill=(20, 10, 16, 255), width=2)
    # the ridge, the camp, the huts, the way
    k.d.line((50, 128, 206, 128), fill=k.INKT, width=2)
    for x in range(54, 206, 9):
        k.d.polygon([(x, 128), (x + 4, 121), (x + 8, 128)], fill=(120, 104, 100, 255), outline=k.INKT)
    k.huisje(128, 146, (214, 120, 60, 255))
    k.huisje(56, 104, (150, 130, 120, 255))
    k.huisje(202, 82, (150, 130, 120, 255))
    k.pad([(128, 142), (128, 126), (100, 114), (60, 104), (110, 92), (198, 84), (190, 70), (80, 68), (64, 62), (64, 44), (76, 40)])
    k.kruis(76, 40)
    k.kompasroos(30, 28)
    return k


def texts(h):
    for key, text in tekst.TEKSTEN.items():
        h.lang(key, text, text)
    verhaal_motor.verhaallijn(h, "ring_h5", tekst.LIJN_NAAM, tekst.LIJN_UITLEG, stappen=tekst.STAPPEN, klaar=tekst.KLAAR, kort=tekst.KORT)
    verhaal_motor.vertelkaart(h, "ring_h5", tekst.KAART_TITEL, tekst.KAART_REGELS, kaart())
    for scene, (titel, regels, namen) in tekst.SCENES.items():
        verhaal_motor.scene(h, scene, titel, regels, namen=namen)


def advancements(h):
    for name, parent, icon, frame, titel, text in tekst.ADVANCEMENTS:
        bbq2.zichtbaar(h, "knabbelring", name, parent, icon, frame, titel, text)
        bbq2.verborgen(h, name)
    for name in tekst.VERBORGEN:
        bbq2.verborgen(h, name)


def selfcheck(h, saved):
    problems = modellen.check(h)
    java_dir = os.path.join(os.path.dirname(h.R), "java", "nl", "juiced", "guhs", "feature", "ringh5")
    java = "".join(open(os.path.join(java_dir, f), encoding="utf-8").read() for f in sorted(os.listdir(java_dir)) if f.endswith(".java"))
    # every text the Java side names exists
    import re
    for key in set(re.findall(r'"((?:quest|gui)\.guhs\.ringh5\.[a-z0-9_.]+?)\.?"', java)):
        if key not in h.NL and not any(k.startswith(key + ".") for k in h.NL):
            problems.append(f"lang {key} (named in feature/ringh5)")
    for key in ("quest.guhs.ring.terug.poortwachter", "entity.guhs.oog_van_sausron", "entity.guhs.ringh5_roosterwachter", "block.guhs.ringh5_blik",
                "block.guhs.ringh5_poortrooster", "block.guhs.oog_van_sausron_beeldje", f"structure.guhs.{STRUCTUUR}", "gui.guhs.verhalen.ring_h5.kort.9",
                "gui.guhs.verhaal.kaart.ring_h5.regel.3"):
        if key not in h.NL:
            problems.append(f"lang {key}")
    for stap in range(3, 10):
        if f"quest.guhs.ringh5.smikagol.stap.{stap}" not in h.NL:
            problems.append(f"Smikagol has nothing to say at step {stap}")
    for i in range(10):
        if f"gui.guhs.ringh5.doel.{i}" not in h.NL:
            problems.append(f"no goal name for step {i}")
    # the scenes: every line the Java scene says, and every actor that speaks has a name
    for scene, (_titel, regels, namen) in tekst.SCENES.items():
        blok = java[java.index(f'Cutscene.maak("{scene}")'):]
        blok = blok[:blok.index(".registreer()")]
        for spreker, key in re.findall(r'\.zeg\(\d+,\s*(?:"([a-z0-9_]*)"|Cutscene\.SPELER),\s*"([a-z0-9_]+)"', blok):
            if key not in regels:
                problems.append(f"scene {scene}: no text for the line '{key}'")
            if spreker and spreker not in namen:
                problems.append(f"scene {scene}: the speaker '{spreker}' has no name")
        for key in regels:
            if f'"{key}"' not in blok:
                problems.append(f"scene {scene}: the line '{key}' is never said")
    if f"STAPPEN = {len(tekst.STAPPEN)};" not in java:
        problems.append(f"Hoofdstuk.STAPPEN is not {len(tekst.STAPPEN)}")
    if len(tekst.FTB) != len(tekst.STAPPEN):
        problems.append("one FTB quest per step")
    if f"KAART_REGELS = {len(tekst.KAART_REGELS)};" not in java:
        problems.append("Scenes.KAART_REGELS is not the number of lines of the card")
    # the tiles, the structure, the sluier's map entry
    if len(saved) != 9:
        problems.append(f"{len(saved)} tiles saved, 9 expected")
    s = json.load(open(f"{h.D}/worldgen/structure/{STRUCTUUR}.json", encoding="utf-8"))
    if s.get("type") != "guhs:burcht" or list(s.get("anchor", [])) != list(bouw.ANKER):
        problems.append("the structure json is not the burcht with the build's anchor")
    if s["max_y"] + bouw.SY - 1 - bouw.G > 118:
        problems.append("the valley would poke through the roof of the Guhbarbecuether")
    if "guhs:" + STRUCTUUR not in json.load(open(f"{h.D}/kaart/verborgen.json", encoding="utf-8"))["structures"]:
        problems.append("the valley is not on the map-hide list")
    if not os.path.exists(f"{h.D}/structure/ringh5_test_kamer.nbt"):
        problems.append("template ringh5_test_kamer")
    if problems:
        raise SystemExit("ring_h5 self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    texts(h)
    advancements(h)
    saved = structuur(h)
    selfcheck(h, saved)


# =====================================================================================================================
# FTB: the section "De Zwarte Roosterpoort" (chapter guhs_knabbelring): one quest per step, in story order
# =====================================================================================================================
def ftb(fq):
    wereld.ftb_questlijn(fq, "ring_h5", "ring_h5", tekst.FTB, eind=(("guhs:kaas_knabbels", 12),))
