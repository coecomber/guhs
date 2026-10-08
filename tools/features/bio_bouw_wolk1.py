"""
biomes3 slice "bouw-wolk1" (Java: feature/bio/bouwwolk1; English: tools/lang/en/c88_bio_bouw_wolk1.json; contract: CONTRACT_BIO.md).

Three findable structures high above the Wolkenweide, each on its own island (guhs:bio_plek, kind "lucht"):
  wolkenhoeder_hut     the rare big island (hill, trees, flower meadow, a lake whose water falls into a catch island's pool
                       and on into a cloud), the hut, the fold with its herd, the wolkenhoeder who teaches making cloud blocks
  sterrenwacht_ruine   a half-fallen observatory on a high island: telescope, star chart, the sterrenkijkerguh (awake at night)
  luchtballon_haven    a jetty with three balloons and the ballonvaarder-guh: one ride a day down to the meadow
This module writes the templates (bio_bouw_wolk1_bouw.py), the structures and their sets, the processor list that keeps the
cloud feet out of the ground, the two new NPC kinds' names and skins, the star chart block, two recipes with sterrenstof,
the hidden proof advancements for slice systemen, the test rooms and every Dutch text (bio_bouw_wolk1_tekst.py).
"""
import os
import re

from features import bio_bouw_wolk1_bouw as bouw
from features import bio_bouw_wolk1_tekst as tekst
from features import bio_lib as lib
from features import bio_wereld_plek as bio_plek
from features import guhpixel_lib as px

H, S, L = tekst.H, tekst.S, tekst.L
# name: (builder, spacing, separation, salt, ruimte)   spacing in chunks; a Wolkenweide is roughly 200 chunks
STRUCTUREN = {
    # biomes3 fix-plaatsing: one per Wolkenweide (bio_wereld_plek.PER_REGIO: a haven and a ruin in every one, the hut in
    # three of four); the set is only the grid whose cell the chosen spot lies in, and only the buildings that really
    # come keep air free (the shape of their own template), so the spacing no longer costs natural islands
    H: (bouw.hoeder, 4, 0, 21500801, 46),
    S: (bouw.sterrenwacht, 4, 0, 21500811, 26),
    L: (bouw.haven, 4, 0, 21500821, 36),
}
PROCESSOR = f"{H}_wolkvoet"
ADVANCEMENTS = (f"{H}_gevonden", f"{H}_les", f"{H}_schaapje", f"{S}_gevonden", f"{S}_sterrenstof", f"{L}_gevonden", f"{L}_vaart")
KAART = f"{S}_sterrenkaart"
TEST_WEI, TEST_VAART = f"{H}_test_wei", f"{L}_test_vaart"


def structuren(h):
    h.w(f"{h.D}/worldgen/processor_list/{PROCESSOR}.json", {"processors": [{"processor_type": f"guhs:{PROCESSOR}", "onder_y": bouw.M}]})
    for naam, (bouwer, spacing, separation, salt, ruimte) in STRUCTUREN.items():
        s, info = bouwer(h)
        bouw.controleer_water(s, naam)
        cx, top, cz = info["midden"]
        bereik = max(cx, s.size[0] - 1 - cx, cz, s.size[2] - 1 - cz)
        assert bereik + 2 <= ruimte <= 64, f"{naam}: the template reaches {bereik} from its anchor, ruimte is {ruimte}"
        assert top == bouw.M + bouw.HOOGTE[naam]
        s.save(naam)
        h.structure(naam, ["wolkenweide"], spacing=spacing, separation=separation, salt=salt, reach=64, centre=f"guhs:{naam}_midden")
        bio_plek.plek(h, naam, "lucht", hoogte=bouw.HOOGTE[naam], ruimte=ruimte)

        def patch(pool):
            for e in pool["elements"]:
                e["element"]["processors"] = f"guhs:{PROCESSOR}"
        h.patch_json(f"{h.D}/worldgen/template_pool/{naam}/start.json", patch)


def npcs(h):
    """The two new kinds wear the skins of their colleagues: the sterrenkijker Professor Sterretje's (the same model),
    the ballonvaarder Kapitein Wolkje's with a mint coat instead of a pink one."""
    src = h.Image.open(os.path.join(h.TEX, "entity", "npc_sterrenkijkerguh.png")).convert("RGBA")
    h.save(src, "entity", f"npc_{S}_sterrenkijker.png")
    src = h.Image.open(os.path.join(h.TEX, "entity", "npc_ballonguh.png")).convert("RGBA")
    h.save(h.recolour(src, hue=0.45, sat=0.75, val=1.0, only=h.pinkish).convert("RGBA"), "entity", "npc_ballonvaarderguh.png")


def sterrenkaart(h):
    rnd = lib.rng("bio_bouw_wolk1/sterrenkaart")
    img = h.Image.new("RGBA", (16, 16), (214, 190, 150, 255))
    px_ = img.load()
    for x in range(1, 15):
        for y in range(1, 15):
            d = int(rnd.integers(-6, 7))
            px_[x, y] = (26 + d, 30 + d, 78 + y * 3 + d, 255)
    lijn = [(3, 11), (4, 8), (6, 6), (8, 7), (10, 5), (12, 8), (11, 11)]          # a guh's two ears and its back
    for (a, b) in zip(lijn, lijn[1:]):
        n = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
        for i in range(n + 1):
            px_[round(a[0] + (b[0] - a[0]) * i / n), round(a[1] + (b[1] - a[1]) * i / n)] = (96, 120, 200, 255)
    for (x, y) in lijn:
        px_[x, y] = (255, 244, 170, 255)
    for (x, y) in ((2, 3), (13, 2), (7, 12), (5, 3), (13, 13), (9, 2), (2, 13)):
        px_[x, y] = (236, 240, 255, 255)
    h.save(img, "block", f"{KAART}.png")
    rand = {"texture": "#rand", "uv": [0, 0, 16, 1]}
    px.muurdeco(h, KAART, [{"from": [1, 1, 15], "to": [15, 15, 16], "faces": {
        "north": {"texture": "#kaart", "uv": [1, 1, 15, 15]}, "south": {"texture": "#kaart", "uv": [1, 1, 15, 15]},
        "up": rand, "down": rand, "east": {"texture": "#rand", "uv": [0, 0, 1, 16]}, "west": {"texture": "#rand", "uv": [0, 0, 1, 16]}}}],
        {"kaart": f"guhs:block/{KAART}", "rand": f"guhs:block/{KAART}", "particle": f"guhs:block/{KAART}"},
        tekst.NL[f"block.guhs.{KAART}"], tekst.NL[f"block.guhs.{KAART}.lore"])
    # sterrenstof (the telescope's gift here, and what a fallen star leaves) is good for two decorations
    h.shaped(KAART, [" S ", "PPP"], {"S": "guhs:sterrenstof", "P": "minecraft:paper"}, f"guhs:{KAART}", 1)
    h.shapeless(f"{S}_sterrenlantaarn", ["guhs:sterrenstof", lib.blok(h, "wolkenlamp", "minecraft:lantern")], "guhs:sterrenlantaarn", 1)


def testkamers(h):
    px.test_kamer(h, TEST_WEI, (13, 8, 13))
    s = h.Structure((13, 28, 25))
    for x in range(13):
        for z in range(25):
            s.set(x, 0, z, "minecraft:pink_concrete" if (x + z) % 2 else "minecraft:white_concrete")
    for x in range(4, 9):
        for z in range(1, 6):
            s.set(x, 20, z, "guhs:guhbloesem_planks")
    s.save(TEST_VAART)


def build(h):
    lib.teksten(h, tekst.NL)
    sterrenkaart(h)
    npcs(h)
    structuren(h)
    testkamers(h)
    for a in ADVANCEMENTS:
        px.quest_adv(h, a)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for naam in STRUCTUREN:
        for f in (f"structure/{naam}.nbt", f"worldgen/structure/{naam}.json", f"worldgen/structure_set/{naam}.json",
                  f"worldgen/template_pool/{naam}/start.json", f"tags/worldgen/biome/has_structure/{naam}.json"):
            if not os.path.exists(f"{h.D}/{f}"):
                problems.append(f"missing data/guhs/{f}")
        for key in (f"structure.guhs.{naam}", f"structure.guhs.{naam}.tooltip"):
            if key not in h.NL:
                problems.append(f"missing lang key {key}")
    for f in [f"advancement/quest/{a}.json" for a in ADVANCEMENTS] + [f"worldgen/processor_list/{PROCESSOR}.json", f"recipe/{KAART}.json",
                                                                       f"recipe/{S}_sterrenlantaarn.json", f"structure/{TEST_WEI}.nbt",
                                                                       f"structure/{TEST_VAART}.nbt", f"loot_table/blocks/{KAART}.json"]:
        if not os.path.exists(f"{h.D}/{f}"):
            problems.append(f"missing data/guhs/{f}")
    for t in (f"entity/npc_{S}_sterrenkijker", "entity/npc_ballonvaarderguh", f"block/{KAART}"):
        if not os.path.exists(os.path.join(h.TEX, *t.split("/")) + ".png"):
            problems.append(f"missing texture {t}")
    for key in tekst.NL:
        if key not in h.NL:
            problems.append(f"missing lang key {key}")
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs")
    kinds = open(os.path.join(java, "entity", "GuhNpcEntity.java"), encoding="utf-8").read()
    blok = kinds[kinds.index("// <bio_bouw_wolk1>"):kinds.index("// </bio_bouw_wolk1>")]
    for kind in (f"{S}_sterrenkijker".upper(), "BALLONVAARDERGUH"):
        if kind + "(" not in blok:
            problems.append(f"GuhNpcEntity.Kind.{kind} is not in the bio_bouw_wolk1 marker block")
    kudde = open(os.path.join(java, "feature", "bio", "bouwwolk1", "Kudde.java"), encoding="utf-8").read()
    if not re.search(rf"AANTAL = {bouw.KUDDE}\b", kudde) or f'"{bouw.KUDDE_TAG}"' not in kudde:
        problems.append("Kudde.java: AANTAL / TAG differ from bio_bouw_wolk1_bouw.py")
    haven = open(os.path.join(java, "feature", "bio", "bouwwolk1", "BouwWolk1Slice.java"), encoding="utf-8").read()
    for a in ADVANCEMENTS + (PROCESSOR, KAART, f"{L}_ballon"):
        if f'"{a}"' not in haven and a not in open(os.path.join(java, "feature", "bio", "bouwwolk1", "Bewijs.java"), encoding="utf-8").read():
            problems.append(f"the Java side does not name {a}")
    if problems:
        raise SystemExit("bio_bouw_wolk1 self-check failed:\n  " + "\n  ".join(problems))
