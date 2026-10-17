"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.4), slice sp_d1_stad: part 1 steps 5-6: jellyfishing with Patguh (wink 7), the boating exam
with juf Pufguh (wink 5). Java: nl.juiced.guhs.feature.einde2.spd1stad (E2SpD1StadFeature). Chunk c113_e2_sp_d1_stad.

  texts   every Dutch text (einde2_sp_d1_stad_tekst): the six scenes, the four look scenes of the hints, the hints, the two
          minigames, the clicks, Patguh's net and juf Pufguh's lesbootje; the step texts STAPPEN / EXTRA / KORT below (the
          skelet's einde2_sp_verhaal writes them with the line)
  looks   the kwallennetje (item texture + model); the lesbootje wears the vetbootje's model read-only (no files of its own)
  ftb     one quest per step, keys einde2_sp_d1_stad_r5 / _r6 (the skelet's section einde2_sp_deel1 names them), FTB_LINEAIR
The builds of Bikinibodem get nothing from this slice (einde2_sp_d1_stad_plekken stays empty: every spot the scenes and games
need is a phase-A marker).
"""
import os

from PIL import Image

from features import einde2_kit as kit
from features import verhaal_motor as vm
from features import einde2_sp_d1_stad_tekst as tekst

LIJN = "sponsguh_recept"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True
P = tekst.P

# the step texts of this slice: what to do, where, a direction and a landmark (playtest 1)
STAPPEN = {LIJN: {
        5: ("Kwallen vangen",
            "Meneer Krabguh wil kwallenjam op de Knabbelburgers, en Patguh weet alles van kwallen. Loop vanaf De Krokante Knabbel "
            "naar het zuiden, naar de Schelpstraat: Patguh ligt te slapen voor zijn grote bruine kruimelrots (lichtstraal).",
            "Patguh's kruimelrots aan de Schelpstraat, zuid in de stad"),
        6: ("Het bootrijexamen",
            "Sponsguh doet zijn bootrijexamen, en jij bent zijn rij-maatje. Loop naar juf Pufguh bij de startstreep van de "
            "bootschool, aan de westkant van de stad naast het pylonenparcours (lichtstraal).",
            "Juf Pufguh bij de bootschool, west in de stad"),
}}
EXTRA = {LIJN: {
        "5_b": ("Vang acht kruimelkwallen met Patguh's netje: tik een kwal aan (links- of rechtsklik). Het Kwallenveld ligt aan het "
                "westeinde van de Schelpstraat; Patguh wacht aan de rand.",
                "Het Kwallenveld aan het westeinde van de Schelpstraat"),
        "6_b": ("Klik op het lesbootje bij de startstreep en vaar één rondje: volg de lichtjes, over het bruggetje en terug naar de "
                "finish. Hooguit acht keer botsen. Sponsguh gilt genoeg voor twee.",
                "De startstreep van het pylonenparcours, west in de stad"),
}}
KORT = {LIJN: {
        "5": "Naar Patguh's rots (Schelpstraat, zuid)",
        "5_b": "Vang 8 kruimelkwallen (westeinde Schelpstraat)",
        "6": "Naar juf Pufguh (bootschool, west)",
        "6_b": "Vaar het pylonenparcours",
}}

SCENES = list(tekst.SCENES)
NET = "einde2_sp_d1_stad_kwallennet"
BOOT = "einde2_sp_d1_stad_lesboot"


# =====================================================================================================================
# looks
# =====================================================================================================================
def net_textuur():
    """16 x 16: Patguh's jelly net: a pale wooden handle from the bottom left, a round pink hoop with a mesh at the top right."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    hout, donker = (206, 160, 98, 255), (128, 88, 50, 255)
    hoepel, schaduw, gaas = (236, 108, 160, 255), (170, 60, 112, 255), (250, 236, 246, 200)
    # the handle: a diagonal two thick from (1, 14) to (8, 7)
    for i in range(8):
        x, y = 1 + i, 14 - i
        px[x, y] = hout
        if 0 <= x + 1 < 16:
            px[x + 1, y] = donker
    # the hoop: a ring round (11, 4.5), radius 4
    cx, cy = 11.0, 4.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 3.4 <= d <= 4.4:
                px[x, y] = hoepel if (x + y) % 5 else schaduw
            elif d < 3.4 and (x + y) % 2 == 0:
                px[x, y] = gaas
    return img


def looks(h):
    h.save(net_textuur(), "item", f"{NET}.png")
    h.item_model(NET)
    h.w(f"{h.D}/loot_table/entities/{BOOT}.json", {"type": "minecraft:entity", "pools": []})


# =====================================================================================================================
# texts
# =====================================================================================================================
def texts(h):
    for key, text in tekst.TEXTS.items():
        kit.lang(h, key, text)
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        vm.scene(h, f"einde2_sp_d1_stad_{naam}", titel, regels, namen)
    for naam, (titel, regel) in tekst.KIJK.items():
        vm.scene(h, f"einde2_sp_d1_stad_kijk_{naam}", titel, {"tekst": regel})


# =====================================================================================================================
# FTB: one quest per step (quest i is done when the player reaches step i + 1)
# =====================================================================================================================
def ftb(fq):
    vorige = None
    for key, titel, text, icon, stap in tekst.FTB:
        fq.q(key, titel, text, icon, [fq.adv(f"{LIJN}_stap_{stap + 1}")], rewards=(("guhs:kaas_knabbels", 6),),
             deps=[vorige] if vorige else [], xp=50)
        vorige = key


# =====================================================================================================================
def selfcheck(h):
    A = h.A
    problems = []
    for key in tekst.TEXTS:
        if key not in h.NL:
            problems.append(f"no text {key}")
    for naam in SCENES:
        if f"scene.guhs.einde2_sp_d1_stad_{naam}.titel" not in h.NL:
            problems.append(f"no title of the scene {naam}")
    for naam in tekst.KIJK:
        if f"scene.guhs.einde2_sp_d1_stad_kijk_{naam}.tekst" not in h.NL:
            problems.append(f"no look scene {naam}")
    for path in (f"{A}/textures/item/{NET}.png", f"{A}/models/item/{NET}.json", f"{A}/geckolib/models/entity/vetbootje.geo.json",
                 f"{A}/textures/entity/vetbootje_kleur_orange.png", f"{A}/textures/entity/vetbootje_zeil_ster.png"):
        if not os.path.exists(path):
            problems.append(f"missing {path}")
    # the Java side: the scene names, the look scenes and the lang prefix
    J = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "spd1stad")
    scenes = open(os.path.join(J, "D1StadScenes.java"), encoding="utf-8").read()
    if "NAMEN = {" + ", ".join(f'"{n}"' for n in SCENES) + "}" not in scenes:
        problems.append("D1StadScenes.NAMEN does not list the scenes of einde2_sp_d1_stad_tekst.SCENES in order")
    if "KIJK = {" + ", ".join(f'"{n}"' for n in tekst.KIJK) + "}" not in scenes:
        problems.append("D1StadScenes.KIJK does not list the look scenes of einde2_sp_d1_stad_tekst.KIJK")
    kern = open(os.path.join(J, "D1Stad.java"), encoding="utf-8").read()
    if f'P = "{P}.";' not in kern:
        problems.append(f"D1Stad.P is not {P}.")
    if problems:
        raise SystemExit("einde2_sp_d1_stad self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    looks(h)
    texts(h)
    selfcheck(h)
