"""
Guhpixel slice "grap2" (Java: feature/guhpixel/grap2; namespaces guhmon, bzg; English: tools/lang/en/c33_px_grap2.json).

Two joke games of the Guhpixel lobby (DESIGN_PX section 2):

  Guhmon-gevecht    Gymleider Dutjes, one gym (template guhpixel/guhmon_gym), one battle with SLEEP bars. Keepsakes: the
                    Badgedoosje (guhmon_badgedoos) with three gymbadges (guhmon_badge_dutjes / _njeg / _knabbel) and the
                    outfit Guhmon-trainerspet.
  Boer zoekt Guh    Presentatrice Guhvon, a TV studio with a farm set (template guhpixel/bzg_studio), three letters, the
                    logeerweek, the choice, the credits. Keepsakes: the outfit Strohoed + Overall van Boer Guhrrit and the
                    decoration Ingelijste brief (bzg_ingelijste_brief). The Brievenbus (bzg_brievenbus) can also be crafted.

Helper modules: guhpixel_grap2_bouw (the two arenas + their checks), guhpixel_grap2_tex (textures, block and NPC models),
guhpixel_grap2_tekst (every Dutch text). Clothes: CLOTHES / clothes() / icons() are read by make_resources.py,
make_guh_variants.py and make_clothes_icons.py (all three pieces reuse existing bones: cap, rain hat, suit).
"""
import os

from features import guhpixel_grap2_bouw as bouw
from features import guhpixel_grap2_tekst as tekst
from features import guhpixel_grap2_tex as tex
from features import guhpixel_lib as lib
from features import kleding

# kleding.py's self-check only knows the marker blocks of the older slices: the pieces of this slice register their own
# source in Grap2Slice (KledingBronnen.bron), like the 3.0 story slices do. (All modules are imported before the first build.)
kleding.ANDERE_30.add("px_grap2")

TEXTS = tekst.TEXTS
CLOTHES = ["guhmon_trainerspet", "bzg_strohoed", "bzg_overall"]
BLOKKEN = ["guhmon_badgedoos", "bzg_brievenbus", "bzg_ingelijste_brief"]
ITEMS = BLOKKEN + [f"guhmon_badge_{b}" for b in tex.BADGES]
STAPPEN = {"guhmon": 4, "bzg": 5}
QUESTS = [f"{g}_stap_{i}" for g, n in STAPPEN.items() for i in range(1, n + 1)] + ["guhmon_klaar", "bzg_klaar", "guhmon_badges"]

GELUIDEN = {
    "guhmon.gevecht": [{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 1.2}],
    "guhmon.zet": [{"name": "minecraft:block.note_block.harp", "type": "event", "pitch": 1.3}],
    "guhmon.slaapt": [{"name": "guhs:huisje.snurk", "type": "event"}],
    "guhmon.gewonnen": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 1.2}],
    "bzg.tune": [{"name": "minecraft:block.bell.use", "type": "event", "pitch": 1.3}],
    "bzg.brief": [{"name": "minecraft:item.book.page_turn", "type": "event"}],
}


def build(h):
    lib.teksten(h, TEXTS)
    tex.build(h, TEXTS)
    bouw.build(h)
    lib.geluid(h, GELUIDEN)
    for q in QUESTS:
        lib.quest_adv(h, q)
    selfcheck(h)


# =====================================================================================================================
# clothes: three new ids on existing bones (outfit_cap, outfit_rain_hat, outfit_suit)
# =====================================================================================================================
def clothes(rng, v):
    import numpy as np

    def pet():
        # red, a white band along the bottom, a little cheese-yellow knabbel in the middle
        a = v.fabric((216, 52, 62), rng, 8)
        a[22:, :] = v.fabric((250, 250, 248), rng, 4)[22:, :]
        for y in range(32):
            for x in range(32):
                if ((x - 15.5) ** 2 + (y - 11) ** 2) ** 0.5 < 5:
                    a[y, x] = (250, 206, 70)
        a[9:11, 13:15] = (232, 150, 50)
        return np.clip(a, 0, 255)

    def stro():
        a = v.straw(rng)
        for y in range(20, 28):                              # the red checkered band of Boer Guhrrit
            for x in range(32):
                a[y, x] = (204, 52, 60) if (x // 4 + y // 4) % 2 else (250, 250, 248)
        return np.clip(a, 0, 255)

    def overall():
        a = v.fabric((62, 104, 176), rng, 8)
        a[:, 4:6] = (150, 186, 232)                          # the stitching
        a[:, 26:28] = (150, 186, 232)
        a[6:10, 9:13] = (236, 196, 76)                       # two brass buttons
        a[6:10, 19:23] = (236, 196, 76)
        a[16:24, 11:21] = (52, 90, 156)                      # the chest pocket
        return np.clip(a, 0, 255)

    return {"guhmon_trainerspet": {"cap": pet}, "bzg_strohoed": {"rain_hat": stro}, "bzg_overall": {"suit": overall}}


def icons(ic):
    pet = ["................", "................", ".....aaaaaa.....", "....abbbbbba....", "...abbbyybbba...", "...abbyyyybba...",
           "...abbbyybbba...", "...awwwwwwwwa...", "..aawwwwwwwwaa..", ".kkkkkkkkkaaa...", "kkkkkkkkk.......", "................"]
    hoed = ["................", "......aaaa......", ".....abbbba.....", "....abbbbbba....", "....arwrwrwa....", "....awrwrwra....",
            ".aaabbbbbbbbaaa.", "abbbbbbbbbbbbbba", ".aaaaaaaaaaaaaa.", "................"]
    overall = ["................", "....aa....aa....", "....ab....ba....", "....ay....ya....", "...abbbbbbbba...", "...abbccccbba...",
               "...abbccccbba...", "...abbbbbbbba...", "...abbbbbbbba...", "...abbbaabbba...", "...abbba.abba...", "...aaaa..aaaa..."]
    return {
        "guhmon_trainerspet": ic.icon(ic.pad(pet), {"a": (150, 30, 44), "b": (216, 52, 62), "y": (250, 206, 70), "w": (250, 250, 248),
                                                    "k": (150, 30, 44)}),
        "bzg_strohoed": ic.icon(ic.pad(hoed), {"a": (160, 124, 56), "b": (226, 196, 110), "r": (204, 52, 60), "w": (250, 250, 248)}),
        "bzg_overall": ic.icon(ic.pad(overall), {"a": (38, 66, 120), "b": (62, 104, 176), "c": (52, 90, 156), "y": (236, 196, 76)}),
    }


# =====================================================================================================================
# checks
# =====================================================================================================================
def selfcheck(h):
    lib.controleer(h, "guhpixel_grap2", blokken=BLOKKEN, items=ITEMS, keys=TEXTS, templates=[bouw.GYM_NAAM, bouw.STUDIO_NAAM])
    problems = []
    for b in BLOKKEN:
        if not os.path.exists(f"{h.D}/loot_table/blocks/{b}.json"):
            problems.append(f"no loot table for {b}")
    for kind in ("guhmon_gymleider", "bzg_presentatrice", "bzg_boer"):
        for p in (f"{h.A}/geckolib/models/entity/guh_npc_{kind}.geo.json", os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    for g, n in STAPPEN.items():
        for key in [f"gui.guhs.{g}.grap.naam", f"gui.guhs.{g}.grap.uitleg", f"gui.guhs.{g}.grap.clou"] + [f"gui.guhs.{g}.grap.stap.{i}" for i in range(1, n + 1)]:
            if key not in h.NL:
                problems.append(f"missing lang key {key}")
    for c in CLOTHES:
        if f"item.guhs.{c}" not in h.NL:
            problems.append(f"no name for the outfit {c}")
    for i in range(1, tekst.AFTITELING + 1):
        for soort in ("rol", "naam"):
            if f"gui.guhs.bzg.aftiteling.{soort}.{i}" not in h.NL:
                problems.append(f"credits line {soort}.{i} is missing")
    # the numbers the Java side repeats
    basis = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "grap2")
    for bestand, verwacht in (("Bzg.java", [f"AFTITELING_REGELS = {2 * tekst.AFTITELING}"]),
                              ("Grap2Slice.java", [f"new Grap(Guhmon.ID, {STAPPEN['guhmon']},", f"new Grap(Bzg.ID, {STAPPEN['bzg']},"]
                               + [f'geluid("{g}")' for g in GELUIDEN])):
        pad = os.path.join(basis, bestand)
        if os.path.exists(pad):
            src = open(pad, encoding="utf-8").read()
            problems += [f"{bestand} does not have '{v}'" for v in verwacht if v not in src]
    for key, nl in TEXTS.items():
        if key.startswith("book.guhs.bzg.") and key.endswith(".tekst") and len(nl) > 430:
            problems.append(f"{key} is too long for the letter screen ({len(nl)} characters)")
    if problems:
        raise SystemExit("guhpixel_grap2 self-check failed:\n  " + "\n  ".join(problems))


# =====================================================================================================================
# FTB quests (section "Grapspelletjes" of the chapter Guhpixel; no locking, knabbel rewards, never muntjes)
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("guhmon_klaar", "Guhmon-gevecht", "Praat in de lobby van Guhpixel met &dGymleider Dutjes&r en daag hem uit met een van je eigen "
      "guhs (of de leenguh van de gym). Hier geen HP, maar een &9SLAAPbalk&r: wie het eerst slaapt, wint. Verliezen kost niets.",
      "guhs:guhmon_badge_dutjes", [adv("guhmon_klaar")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("guhmon_badges", "Alle drie de gymbadges", "In het &6Badgedoosje&r past meer dan één badge. Win nog eens en verdien de "
      "&dNjegbadge&r (laat een Dutje van Snurkel mislukken met jouw Njeg) en de &6Knabbelbadge&r (val in slaap met een vol buikje). "
      "De andere vijf gyms zijn dicht wegens dutje.",
      "guhs:guhmon_badgedoos", [adv("guhmon_badges")], rewards=(("guhs:kaas_knabbels", 12),), deps=["guhmon_klaar"], xp=50)
    q("bzg_klaar", "Boer zoekt Guh", "&dPresentatrice Guhvon&r neemt in de lobby van Guhpixel haar tv-programma op. Lees de drie brieven "
      "voor &6Boer Guhrrit&r, stop iedereen in tijdens de logeerweek en help hem kiezen. Het is maar televisie, njeg.",
      "guhs:bzg_ingelijste_brief", [adv("bzg_klaar")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
