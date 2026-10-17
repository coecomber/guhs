"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.3), slice sp_d1_start: part 1 steps 0-4: the bubble letter, the trip to Bikinibodem, the bubble
stream and the welcome, the job interview, baking Knabbelburgers; the narrator card of part 1.
Java: nl.juiced.guhs.feature.einde2.spd1start (E2SpD1StartFeature). Chunk c112_e2_sp_d1_start.

  STAPPEN / EXTRA / KORT   the step texts of steps 0..4 of sponsguh_recept (the skelet's einde2_sp_verhaal writes them with the
                           line); EXTRA holds the reserved variant 4_b (the grill game on)
  texts                    the narrator card (with its drawn map of Bikinibodem), the six scenes, the two look scenes, the hints,
                           the bubble letter, the grill game, the small talk, the clothes source (einde2_sp_d1_start_tekst)
  ftb                      one quest per step, keys einde2_sp_d1_start_r<stap>, task sponsguh_recept_stap_<stap + 1>; the skelet's
                           section einde2_sp_deel1 names them (FTB_LINEAIR: one chain with the other step owners)
The markers and scene cameras this slice adds to the build of Bikinibodem: einde2_sp_d1_start_plekken.
"""
import features.einde2_kit as kit
from features import verhaal_motor as vm
from features import einde2_sp_d1_start_tekst as tekst

LIJN = "sponsguh_recept"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True

# the step texts of this slice (what to do, where: a direction and a landmark; CONTRACT_E2_C 7.1)
STAPPEN = {LIJN: {
        0: ("De bubbelbrief",
            "Er zweeft een zeepbel met een briefje erin naar je toe, ergens in het Guheinde (hij vindt jou, je hoeft niet te "
            "zoeken). Klik erop om hem te laten knappen.",
            "Overal in het Guheinde (de bubbel vindt jou)"),
        1: ("Naar Bikinibodem",
            "Fluit Appa met je Appa-fluitje en kies Bikinibodem in zijn lijst, of volg het Superkompas (tabblad einde). Land op "
            "het eilandje bovenop de grote zeepbel, diep tussen de buiteneilanden.",
            "Het eilandje bovenop de zeepbel van Bikinibodem, diep tussen de buiteneilanden"),
        2: ("De bubbelstroom",
            "Stap op het eilandje in de bubbelstroom: de glinsterende kolom in het buisje in het midden, de ingang zit aan de "
            "zuidkant. Hij zet je zachtjes af op het plein van Bikinibodem.",
            "De bubbelstroom midden op het eilandje, omlaag naar het plein"),
        3: ("Een baantje",
            "Loop van het plein naar het oosten over de Hoofdstraat naar De Krokante Knabbel (het gebouw met het ronde dak). Ga "
            "naar binnen, door de deur rechts achterin het kantoor in, en klik op Meneer Krabguh.",
            "Het kantoor van Meneer Krabguh, achterin De Krokante Knabbel (oost van het plein)"),
        4: ("Knabbelburgers bakken",
            "Ga in de keuken van De Krokante Knabbel achter de grill staan (de deur helemaal links in de achterwand). Klik de "
            "kratten aan in de volgorde van het bonnetje boven de grill. Vijf bestellingen en je bent een echte knabbelbakker.",
            "De grill in de keuken van De Krokante Knabbel"),
}}
EXTRA = {LIJN: {
        "4_b": ("Bakken maar! Kijk op het bonnetje boven de grill (het pijltje wijst het stukje dat er nu op moet) en klik dat "
                "krat aan. Fout? Plof, en die bestelling gewoon opnieuw.",
                "De grill in de keuken van De Krokante Knabbel"),
}}
KORT = {LIJN: {
        "0": "Klik op de bubbelbrief",
        "1": "Naar Bikinibodem (Appa-fluitje)",
        "2": "De bubbelstroom omlaag",
        "3": "Klik op Meneer Krabguh",
        "4": "Bak vijf Knabbelburgers",
        "4_b": "Volg het bonnetje boven de grill",
}}


# =====================================================================================================================
# the narrator card's map: Bikinibodem from the side (the dish, the dome, the island with the stream, the town)
# =====================================================================================================================
def kaart(k):
    k.water([(0, 0), (256, 0), (256, 160), (0, 160)], kleur=(104, 80, 132, 255))                   # the void, purple mist
    d = k.d
    d.pieslice((28, 44, 228, 204), 180, 360, fill=(170, 206, 230, 255), outline=(220, 240, 255, 255))   # the dome
    k.land([(18, 124), (238, 124), (214, 136), (128, 144), (42, 136)], kleur=(232, 190, 84, 255))   # the dish of kaaskorst
    k.land([(104, 42), (152, 42), (140, 52), (116, 52)], kleur=(214, 170, 96, 255))                  # the island on top
    for y in range(52, 124, 6):                                                                      # the bubble stream
        d.ellipse((126, y, 130, y + 4), outline=(250, 250, 255, 255))
    for x, y, c in ((70, 74, (250, 170, 210, 255)), (176, 70, (250, 200, 120, 255)), (102, 96, (190, 170, 250, 255)),
                    (152, 92, (250, 170, 210, 255)), (200, 100, (170, 230, 180, 255))):              # flower clouds
        d.ellipse((x - 5, y - 3, x + 5, y + 3), fill=c)
    k.huisje(60, 122, kleur=(230, 120, 60, 255))                                                    # Sponsguh's fruit house
    k.huisje(96, 122, kleur=(150, 160, 180, 255))                                                   # the Bellenkom
    k.huisje(160, 122, kleur=(200, 70, 60, 255))                                                    # De Krokante Knabbel
    k.huisje(192, 122, kleur=(120, 120, 130, 255))                                                  # De Vetemmer
    k.kruis(160, 112)
    k.kompasroos()


def texts(h):
    for key, text in tekst.TEXTS.items():
        kit.lang(h, key, text)
    titel, regels = tekst.KAART
    vm.vertelkaart(h, "einde2_sp_d1_start", titel, regels, kaart)
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        vm.scene(h, f"einde2_sp_d1_start_{naam}", titel, regels, namen)
    for naam, (titel, regel) in tekst.KIJK.items():
        vm.scene(h, f"einde2_sp_d1_start_kijk_{naam}", titel, {"tekst": regel})


def selfcheck(h):
    for n in range(5):
        if n not in STAPPEN[LIJN]:
            raise SystemExit(f"einde2_sp_d1_start: no text for step {n}")
    for key, _t, _x, _i, stap in tekst.FTB:
        if key != f"einde2_sp_d1_start_r{stap}":
            raise SystemExit(f"einde2_sp_d1_start: the FTB key {key} does not name its step {stap}")
    for naam, (_t, regels, namen) in tekst.SCENES.items():
        if not regels:
            raise SystemExit(f"einde2_sp_d1_start: the scene {naam} has no lines")


def build(h):
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB: one quest per step (quest i is done when the player reaches step i + 1); the chain goes on in sp_d1_stad's module
# =====================================================================================================================
def ftb(fq):
    vorige = None
    for key, titel, text, icon, stap in tekst.FTB:
        fq.q(key, titel, text, icon, [fq.adv(f"{LIJN}_stap_{stap + 1}")], rewards=(("guhs:kaas_knabbels", 6),),
             deps=[vorige] if vorige else [], xp=50)
        vorige = key
