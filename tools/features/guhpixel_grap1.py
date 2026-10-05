"""
Guhpixel slice "grap1" (Java: feature/guhpixel/grap1; namespaces skyblok, bedwars, vadsnite; English: tools/lang/en/c32_px_grap1.json).

The three joke games of DESIGN_PX section 2, each a mini questline of four steps in an arena of your own:
  - Skyblok: arena guhpixel/skyblok_eiland ("the" island in a painted box), keepsake block skyblok_fles (Eilandje-in-een-fles)
  - Bedwars: arena guhpixel/bedwars_eilanden, the stand-in entity bedwars_teamguh, keepsake the four Teamslaapmutsen
    (clothes bedwars_slaapmuts_<rood|blauw|groen|geel> on the existing nightcap bones)
  - Vadsnite: arena guhpixel/vadsnite_eiland (the island and the Vadsbus), keepsake the Parachuterugzakje
    (clothes vadsnite_parachuterugzakje on new bones outfit_vadsnite_rugzak*)
Also: the three lobby NPC kinds with their own models, four sounds, the hidden advancements quest/<id>_stap_<1..4> and
quest/<id>_klaar, three FTB quests and the game test rooms. Helpers: guhpixel_grap1_bouw (templates), _tex (textures and
models), _tekst (every Dutch text). The clothes textures and bones only appear after `python tools/make_guh_variants.py`
(+ make_sleep_eyes.py, make_clothes_icons.py).
"""
import os

import numpy as np

from features import guhpixel_grap1_bouw as bouw
from features import guhpixel_grap1_tekst as tekst
from features import guhpixel_grap1_tex as tex
from features import guhpixel_lib as lib
from features import kleding

TEXTS = tekst.TEXTS

# kleding.py checks that every piece has exactly one source in KledingBronLijst.java, except the pieces inside the marker
# blocks of slices that register their own source (hard-coded sets ANDERE_29 / ANDERE_30). This slice registers its own
# (KledingBronnen.bron in Grap1Slice.register) and may not edit kleding.py, so it tells that check about its marker block
# here (every feature module is imported before the first build runs). Shared edit wanted: a line for the px_* markers
# in kleding.py; then this line can go.
kleding.ANDERE_30.add("px_grap1")

# --- clothes -----------------------------------------------------------------------------------------------------------
# team: (the cap's colour, its stripe, the icon's dark edge)
MUTSEN = {"rood": ((214, 58, 58), (255, 236, 236), (128, 24, 28)), "blauw": ((66, 104, 220), (232, 240, 255), (30, 48, 128)),
          "groen": ((96, 190, 70), (238, 255, 230), (40, 104, 36)), "geel": ((246, 206, 60), (255, 250, 222), (160, 118, 20))}
CLOTHES = [f"bedwars_slaapmuts_{t}" for t in MUTSEN] + ["vadsnite_parachuterugzakje"]
_B = [0, 6, 6]   # body pivot
BONES = {
    # the Parachuterugzakje: a canvas pack on the back, a rolled-up pink parachute on top, a red pull handle on the side
    "outfit_vadsnite_rugzak": ("body", _B, "vadsnite_rugzak", [([-3.6, 11.2, 1.2], [7.2, 3.6, 6.4], 0)]),
    "outfit_vadsnite_rugzak_scherm": ("body", _B, "vadsnite_scherm", [([-3.0, 14.8, 1.8], [6.0, 1.8, 5.2], 0)]),
    "outfit_vadsnite_rugzak_koord": ("body", _B, "vadsnite_koord", [([3.6, 12.4, 3.6], [0.9, 0.9, 0.9], 0), ([3.9, 11.0, 3.8], [0.3, 1.6, 0.5], 0)]),
}
RUGZAK, SCHERM, KOORD = (84, 110, 168), (255, 140, 196), (226, 60, 60)
RUGZAK_ICON = ["................", "......pppp......", ".....pwppwp.....", ".....pppppp.....", "....aaaaaaaa....", "...abbbbbbbba...",
               "...abbbbbbbbar..", "...abccccccbar..", "...abbbyybbba.r.", "...abbbyybbba.r.", "...abccccccba...", "...abbbbbbbba...",
               "...abbbbbbbba...", "....aaaaaaaa....", "................", "................"]


def clothes(rng, v):
    out = {}
    for team, (kleur, streep, _) in MUTSEN.items():
        out[f"bedwars_slaapmuts_{team}"] = {
            "slaapmutsje": (lambda k=kleur, s=streep: np.clip(v.stripes(k, s, rng, 5), 0, 255)),
            "slaapmutsje_pompon": (lambda: v.fabric((252, 252, 255), rng, 6))}

    def rugzak():
        a = v.fabric(RUGZAK, rng, 10)
        a[6:9, :] = (52, 64, 104)                       # two straps and a brass buckle
        a[22:25, :] = (52, 64, 104)
        a[13:18, 13:19] = (250, 214, 90)
        return np.clip(a, 0, 255)

    def scherm():
        return np.clip(v.stripes(SCHERM, (255, 255, 255), rng, 4), 0, 255)
    out["vadsnite_parachuterugzakje"] = {"vadsnite_rugzak": rugzak, "vadsnite_scherm": scherm,
                                         "vadsnite_koord": (lambda: v.fabric(KOORD, rng, 5))}
    return out


def icons(ic):
    out = {f"bedwars_slaapmuts_{team}": ic.shaped("santa", donker, kleur) for team, (kleur, _, donker) in MUTSEN.items()}
    out["vadsnite_parachuterugzakje"] = ic.icon(RUGZAK_ICON, {"a": (44, 60, 104), "b": RUGZAK, "c": (52, 64, 104), "y": (250, 214, 90),
                                                               "p": SCHERM, "w": (255, 255, 255), "r": KOORD})
    return out


# =====================================================================================================================
def build(h):
    lib.teksten(h, TEXTS)
    tex.fles(h, TEXTS["block.guhs.skyblok_fles"], TEXTS["block.guhs.skyblok_fles.lore"])
    tex.npcs(h)
    lib.geluid(h, tekst.SOUNDS)
    for naam in tekst.QUEST_ADVS:
        lib.quest_adv(h, naam)
    bouw.build(h, lib)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_grap1", blokken=("skyblok_fles",), items=("skyblok_fles",) + tuple(CLOTHES), keys=TEXTS,
                   templates=(bouw.SKYBLOK, bouw.BEDWARS, bouw.VADSNITE) + tuple(bouw.TEST_KAMERS))
    problems = []
    for naam in tekst.QUEST_ADVS:
        if not os.path.exists(f"{h.D}/advancement/quest/{naam}.json"):
            problems.append(f"missing advancement quest/{naam}")
    for kind in ("skyblok_guh", "bedwars_guh", "vadsnite_guh"):
        for f in (f"{h.A}/geckolib/models/entity/guh_npc_{kind}.geo.json", f"{h.A}/textures/entity/npc_{kind}.png"):
            if not os.path.exists(f):
                problems.append(f"missing {f}")
    # the Java side: kinds, clothes and the arena constants that mirror guhpixel_grap1_bouw
    J = os.path.join("src", "main", "java", "nl", "juiced", "guhs")
    kinds = open(os.path.join(J, "entity", "GuhNpcEntity.java"), encoding="utf-8").read()
    kleren = open(os.path.join(J, "entity", "GuhClothes.java"), encoding="utf-8").read()
    for kind in ("SKYBLOK_GUH", "BEDWARS_GUH", "VADSNITE_GUH"):
        if f"        {kind}(" not in kinds:
            problems.append(f"NPC kind {kind} is not in GuhNpcEntity.Kind")
    for c in CLOTHES:
        if f"    {c.upper()}(" not in kleren:
            problems.append(f"clothes {c} is not in GuhClothes")

    def java(naam):
        return open(os.path.join(J, "feature", "guhpixel", "grap1", naam), encoding="utf-8").read()
    wil = {"SkyblokSessie.java": [f"new Vec3i{bouw.SKY_MAAT}", f"BED_VOET = new BlockPos{bouw.SKY_BED_VOET}", f"KIST = new BlockPos{bouw.SKY_KIST}",
                                  f"DEUR = new BlockPos{bouw.SKY_DEUR}", f"EILAND_Y = {bouw.SKY_EILAND_Y}"],
           "BedwarsSessie.java": [f"new Vec3i{bouw.BED_MAAT}", f"EILAND_Y = {bouw.BED_Y}, MIDDEN = {bouw.BED_MIDDEN}, BRUG_LENGTE = {bouw.BRUG_LENGTE}"],
           "VadsniteSessie.java": [f"new Vec3i{bouw.VADS_MAAT}", f"EILAND_Y = {bouw.VADS_Y}, BUS_Y = {bouw.BUS_Y}, MIDDEN = {bouw.VADS_MIDDEN}",
                                   f"LUIK_X0 = {bouw.LUIK[0]}, LUIK_X1 = {bouw.LUIK[1]}, LUIK_Z0 = {bouw.LUIK[2]}, LUIK_Z1 = {bouw.LUIK[3]}"]
           + [f"new BlockPos({x}, {y}, {z})" for (x, y, z, _) in bouw.BEDDEN]}
    for naam, stukken in wil.items():
        src = java(naam)
        for stuk in stukken:
            if stuk not in src:
                problems.append(f"{naam} does not say '{stuk}' (guhpixel_grap1_bouw.py changed?)")
    if problems:
        raise SystemExit("guhpixel_grap1 self-check failed:\n  " + "\n  ".join(problems))


# =====================================================================================================================
# FTB quests (section "Grapspelletjes" of the chapter Guhpixel; no dependencies, knabbel rewards, never muntjes)
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("skyblok_klaar", "Skyblok uitgespeeld", "Praat in de lobby van &dGuhpixel&r met de &dSkyblok-guh&r en reis naar hét eiland. De "
      "handleiding telt 4.812 stappen. Niemand heeft ooit stap 2 gehaald... maar er schijnt een kortere weg te zijn, njeg. "
      "De eerste keer krijg je 100 muntjes en een aandenken.", "guhs:skyblok_fles", [fq.adv("skyblok_klaar")],
      rewards=(("guhs:kaas_knabbels", 8),), x=0, y=0, xp=100)
    q("bedwars_klaar", "Bed verdedigd", "Praat in de lobby van &dGuhpixel&r met de &dBedwars-guh&r. Vier teams komen over bruggetjes van "
      "wol naar je eiland. Verdedig je bed, met alles wat je hebt! De eerste keer krijg je 100 muntjes en een aandenken.",
      "guhs:bedwars_slaapmuts_rood", [fq.adv("bedwars_klaar")], rewards=(("guhs:kaas_knabbels", 8),), x=1.5, y=0, xp=100)
    q("vadsnite_klaar", "Vadsoverwinning", "Praat in de lobby van &dGuhpixel&r met de &dVadsnite-guh&r en spring met 99 guhs uit de "
      "vliegende &dVadsbus&r. Wie het langst wakker blijft, wint. De eerste keer krijg je 100 muntjes en een aandenken.",
      "guhs:vadsnite_parachuterugzakje", [fq.adv("vadsnite_klaar")], rewards=(("guhs:kaas_knabbels", 8),), x=3, y=0, xp=100)
