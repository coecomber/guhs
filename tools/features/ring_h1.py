"""
bbq2 (ring-h1): chapter 1 of the Knabbelring, "Een langverwacht knabbelfeest" (DESIGN_130 4). Java: feature/ringh1.

  ring_h1_bouw.py    the Knabbelgouw (the big barbecueput + the heuvelholletjes as one template), Guhdalf's camp (the prop for
                     old pits), the test room
  ring_h1_tex.py     the four quest props (models, two textures), the drawn map of the narrator card
  ring_h1_tekst.py   every Dutch text
  ring_h1_wiki.py    the wiki entries (CONTRACT_130 2.4; not built here)
  here               the structure guhs:knabbelgouw (Guhmensie; in the structure set guhs:barbecueput, see
                     barbecuether.structures, plus its own guaranteed copy), the questline, card and scene texts,
                     advancements, the FTB section

The Knabbelgouw only comes in chunks made with this update. At a big barbecueput that stands already, Bezetting (Java: Gouw)
puts Guhdalf's camp (template ringh1_kamp) on the free strip around it, and the chapter plays there just the same.
"""
import json
import os

from features import bbq2
from features import ring_h1_bouw as bouw
from features import ring_h1_tekst as tekst
from features import ring_h1_tex as tex
from features import verhaal_motor
from features import wereld

FTB_LINEAIR = True
FTB_SECTIES = [("ring_h1", "Een langverwacht knabbelfeest", "npc:guhdalf", None)]

STRUCTUUR = "knabbelgouw"
SALT = 21301601
# the flatness the Knabbelgouw asks: the surface on a 5 x 5 grid over 81 x 81 blocks around its middle
GROOTTE, VLAK = 40, 16
# its own guaranteed copy (new terrain only): a walk from spawn, in the ring of the minigames
GEGARANDEERD = dict(sector=5, min=600, max=1400)
# who goes first where buildings would touch: as the barbecueput itself (so nothing changes for the pits of the
# Guhbarbecuether, which are in the same structure set); the guaranteed copy goes before every normal building anyway
VOORRANG = 19


def structuur(h):
    g, problems = bouw.bouw_gouw(h)
    if problems:
        raise SystemExit("ring_h1: the Knabbelgouw is not right:\n  " + "\n  ".join(problems[:40]))
    g.s.save(STRUCTUUR)
    kamp, problems = bouw.bouw_kamp(h)
    if problems:
        raise SystemExit("ring_h1: Guhdalf's camp is not right:\n  " + "\n  ".join(problems[:40]))
    kamp.save("ringh1_kamp")
    bouw.test_kamer(h).save("ringh1_test_kamer")
    titel, tooltip = tekst.STRUCTUUR
    wereld.bbq_structuur(h, STRUCTUUR, soort="grot", titel=titel, tooltip=tooltip, biomes=list(h.GUHMENSION_LAND), salt=SALT,
                         templates=[(STRUCTUUR, 1)], gegarandeerd=GEGARANDEERD, grootte=GROOTTE, vlak=VLAK, voorrang=VOORRANG)

    # the centre jigsaw is in layer 0 and the pool says where the ground really is (as fossiel-mijn does): the lawn lands on
    # the surface and the land around is smoothed towards it, not towards the bottom of the template
    def grond(pool):
        for e in pool["elements"]:
            el = {"element_type": "guhs:grond_single_pool_element"}
            el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
            el["ground_level_delta"] = bouw.G + 1
            e["element"] = el
    h.patch_json(f"{h.D}/worldgen/template_pool/{STRUCTUUR}/start.json", grond)
    # a story place: never within 600 blocks of 0,0 (like the barbecueput itself)
    h.add_tag("guhs/tags/worldgen/structure/verhaal", [f"guhs:{STRUCTUUR}"])
    # the advancement "find a broken barbecueput" also counts at the pit of a Knabbelgouw
    def put_ook(adv):
        loc = adv["criteria"]["done"]["conditions"]["player"]["location"]
        loc["structures"] = ["guhs:barbecueput", f"guhs:{STRUCTUUR}"]
    pad = f"{h.D}/advancement/barbecuether/barbecueput.json"
    if os.path.exists(pad):
        h.patch_json(pad, put_ook)


def texts(h):
    for key, text in tekst.TEKSTEN.items():
        h.lang(key, text, text)
    verhaal_motor.verhaallijn(h, "ring_h1", tekst.LIJN_NAAM, tekst.LIJN_UITLEG, stappen=tekst.STAPPEN, klaar=tekst.KLAAR, kort=tekst.KORT)
    verhaal_motor.vertelkaart(h, "ring_h1", tekst.KAART_TITEL, tekst.KAART_REGELS, tex.kaart())
    for scene, (titel, regels, namen) in (("ringh1_aankomst", tekst.SCENE_AANKOMST), ("ringh1_feest", tekst.SCENE_FEEST)):
        verhaal_motor.scene(h, scene, titel, regels, namen)


def advancements(h):
    for name, parent, icon, frame, titel, text in tekst.ADVANCEMENTS:
        bbq2.zichtbaar(h, "knabbelring", name, parent, icon, frame, titel, text)
        bbq2.verborgen(h, name)


def selfcheck(h):
    problems = []
    for key in (f"structure.guhs.{STRUCTUUR}", "gui.guhs.verhalen.ring_h1.kort.5", "scene.guhs.ringh1_feest.geheim", "gui.guhs.verhaal.kaart.ring_h1.regel.3",
                "block.guhs.ringh1_proviand", "quest.guhs.ringh1.bewoner.7", "advancements.guhs.knabbelring.ring_h1_klaar.title"):
        if key not in h.NL:
            problems.append(f"lang {key}")
    for name in (STRUCTUUR, "ringh1_kamp", "ringh1_test_kamer"):
        if not os.path.exists(f"{h.D}/structure/{name}.nbt"):
            problems.append(f"template {name}")
    for name in tex.BLOKKEN:
        if not os.path.exists(f"{h.A}/blockstates/{name}.json"):
            problems.append(f"blockstate {name}")
    pool = json.load(open(f"{h.D}/worldgen/template_pool/{STRUCTUUR}/start.json", encoding="utf-8"))
    if any(e["element"].get("element_type") != "guhs:grond_single_pool_element" or e["element"].get("ground_level_delta") != bouw.G + 1
           for e in pool["elements"]):
        problems.append("the start pool of the Knabbelgouw has no ground_level_delta")
    # the Knabbelgouw is the second structure of the barbecueput set, and the barbecueput makes no big pits in the open
    put_set = json.load(open(f"{h.D}/worldgen/structure_set/barbecueput.json", encoding="utf-8"))
    if [e["structure"] for e in put_set["structures"]] != ["guhs:barbecueput", f"guhs:{STRUCTUUR}"]:
        problems.append("the structure set guhs:barbecueput does not hold the barbecueput and the Knabbelgouw")
    klein = json.load(open(f"{h.D}/worldgen/template_pool/barbecueput/klein.json", encoding="utf-8"))
    if any("groot" in e["element"]["location"] for e in klein["elements"]):
        problems.append("the surface pool of the barbecueput holds the big pit")
    biomes = json.load(open(f"{h.R}/data/guhs/tags/worldgen/biome/has_structure/{STRUCTUUR}.json", encoding="utf-8"))["values"]
    bbq = json.load(open(f"{h.R}/data/guhs/tags/worldgen/biome/is_barbecuether.json", encoding="utf-8"))["values"]
    if set(biomes) & set(bbq):
        problems.append("the Knabbelgouw may never stand in the Guhbarbecuether")
    # the numbers Java knows (feature/ringh1/Gouw.java) are the numbers of the templates
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ringh1", "Gouw.java"), encoding="utf-8").read()
    g, _ = bouw.bouw_gouw(h)

    def pos(c):
        return f"new BlockPos({c[0]}, {c[1]}, {c[2]})"

    def gpos(c):
        return f"new BlockPos({c[0]}, {'G' if c[1] == bouw.G else 'G + ' + str(c[1] - bouw.G)}, {c[2]})"
    wil = {"KAMP_MAAT": pos(bouw.KAMP), "GUHDALF": pos(bouw.KAMP_GUHDALF), "TAFEL": pos(bouw.KAMP_TAFEL), "SAM": pos(bouw.KAMP_SAM),
           "GOUW_KAMP": gpos((bouw.GOUW_KAMP[0], bouw.G, bouw.GOUW_KAMP[1])), "GOUW_SAM": gpos(bouw.GOUW_SAM),
           "GOUW_PUT": pos((bouw.PUT[0], 0, bouw.PUT[1]))}
    for naam, tekst_java in wil.items():
        if f"{naam} = {tekst_java}" not in java:
            problems.append(f"Gouw.java: {naam} should be {tekst_java}")
    bewoners = ", ".join(pos(c) for c in bouw.bewoner_plekken(g))
    if f"BEWONERS = List.of({bewoners})" not in java:
        problems.append(f"Gouw.java: BEWONERS should be List.of({bewoners})")
    if bouw.GOUW_KAMP_KWART != 0:
        problems.append("Gouw.java reads the camp of a Knabbelgouw as not turned: GOUW_KAMP_KWART must stay 0")
    if problems:
        raise SystemExit("ring_h1 self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    tex.build(h)
    texts(h)
    advancements(h)
    for name in tekst.VERBORGEN:
        bbq2.verborgen(h, name)
    structuur(h)
    selfcheck(h)


# =====================================================================================================================
# FTB: the section "Een langverwacht knabbelfeest" (chapter guhs_knabbelring: one chain; the first quest names no deps, it
# comes after ring-kern's ring_wegwijs by itself; the last one is ring_h1_portaal)
# =====================================================================================================================
def ftb(fq):
    vorige = None
    for i, (key, titel, text, icon, stap) in enumerate(tekst.FTB):
        laatste = i == len(tekst.FTB) - 1
        fq.q(key, titel, text, icon, [fq.adv(f"ring_h1_stap_{stap}")], rewards=(("guhs:kaas_knabbels", 12 if laatste else 6),),
             deps=[vorige] if vorige else [], shape="gear" if laatste else None, xp=150 if laatste else 0)
        vorige = key
