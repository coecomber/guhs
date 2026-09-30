"""
3.0 (Guhverhalen), slice mewtwo: Het kloon-eiland (DESIGN_30 §3, except Sjokkel): professor Knabbelkloon, Mieuwguh en de
Guhtwo. Java: feature/mewtwo (MewtwoFeature and friends).

  - the structure guhs:kloon_eiland (mewtwo_bouw.py): a rock island with a lab in the Diepe Guhzee (a regio "zee" piece):
    the koepelhal with the cracked kloontank, the rommelige kantoortje, the tower with the arena on top, the steiger with
    the boothuisje (Reisguh "Kloon-eiland"), shuckle plekjes on the coast (landdiertjes)
  - the NPC KNABBELKLOON (own model: a guh professor with a crooked little pair of glasses, a lab coat, a clipboard)
  - the questline (per player, GuhQuests.saved: guhs_mewtwo_stap ...): the 6 labnotities (mewtwo_labnotitie) -> the 4
    tankonderdelen (mewtwo_tankonderdeel) in the kloontank -> Mieuwguh appears -> the grote knabbelmaaltijd (a double portion
    in the grote knabbelschaal on the arena) -> the Guhtwo is tameable once (VerhaalGuhs)
  - the Guhtwo variant (bones mewtwo_staart + mewtwo_buisje, the lilac fur with a purple belly, the glow), Mieuwguh
    (entity guhs:mew, own model), outfits (marker "mewtwo": trainerpetje with our own knabbelbal logo, trainerpakje with a
    rugzakje and a knabbelbal, Guhtwo-staartje + nekbuisje, Mieuwguh-ballonnetje)
  - blocks: mewtwo_kloontank (+ the invisible mewtwo_tankwand), mewtwo_notitieplek, mewtwo_onderdelenkist,
    mewtwo_knabbelschaal, deco mewtwo_computer, mewtwo_reageerbuisjes, mewtwo_papieren; particles mewtwo_gloed, mewtwo_x2,
    mewtwo_bubbel; sounds mewtwo.*
  - advancements (tab guhs:verhalen/, mewtwo_*), FTB section "Het kloon-eiland", Guhdex pages (mewtwo, mew, knabbelkloon),
    the game test templates mewtwo_test_*.
Textures and models: mewtwo_tex.py.
"""
import os

from features import mewtwo_bouw as bouw
from features import mewtwo_tex as tex
from features import verhaal, verhaal_wereld

NAME = "kloon_eiland"
SALT = 20300301
BLOKKEN = ["mewtwo_kloontank", "mewtwo_tankwand", "mewtwo_notitieplek", "mewtwo_onderdelenkist", "mewtwo_knabbelschaal",
           "mewtwo_computer", "mewtwo_reageerbuisjes", "mewtwo_papieren"]
ITEMS = ["mewtwo_labnotitie", "mewtwo_tankonderdeel", "mew_spawn_egg"]
NOTITIES = 6
ONDERDELEN = 4

# =====================================================================================================================
# the Guhtwo (variant bones: shown only on GuhVariant MEWTWO, prefix "mewtwo") and the outfits (marker "mewtwo")
# =====================================================================================================================
_H = [0, 6, -2]
_B = [0, 6, 6]
_T = [0, 1.5, 12]
BONES = {
    # the thick purple tail that curls up, with a round bulb at the end (over the guh's own little tail)
    "mewtwo_staart": ("tail", [0, 3.5, 12], "mewtwo_staart", [([-1.3, 2.4, 11.2], [2.6, 2.4, 5.2], 0), ([-1.1, 3.0, 16.0], [2.2, 2.2, 3.4], 0),
                                                           ([-1.0, 4.6, 18.4], [2.0, 3.2, 2.0], 0), ([-0.9, 7.4, 18.0], [1.8, 2.0, 1.8], 0)]),
    "mewtwo_bolletje": ("mewtwo_staart", [0, 9.5, 18.9], "mewtwo_staart", [([-1.9, 8.8, 17.1], [3.8, 3.4, 3.6], 0)]),
    # the little tube from the back of the head down to the neck (lilac, like the fur)
    "mewtwo_buisje": ("head", _H, "mewtwo_buis", [([-1.1, 11.4, -1.4], [2.2, 2.0, 2.6], 0), ([-1.0, 10.4, 0.9], [2.0, 1.8, 2.4], 0)]),
}
# the outfits (bone prefix outfit_mewtwo_*)
BONES.update({
    # the trainerpetje: a pink crown with a white front panel, a dark purple peak, our own logo (the knabbelbal with guh ears)
    "outfit_mewtwo_petje": ("head", _H, "mewtwo_pet", [([-5.4, 14.9, -10.4], [10.8, 2.6, 9.0], 0), ([-4.4, 17.5, -9.4], [8.8, 0.9, 7.0], 0)]),
    "outfit_mewtwo_petje_klep": ("head", _H, "mewtwo_klep", [([-4.8, 14.9, -14.6], [9.6, 0.6, 4.4], 0), ([-0.7, 18.3, -6.6], [1.4, 0.5, 1.4], 0)]),
    "outfit_mewtwo_petje_logo": ("head", _H, "mewtwo_logo", [([-2.4, 15.3, -10.9], [4.8, 2.0, 0.5], 0)]),
    # the trainerpakje: the vest is the body suit (painted), plus a little backpack with a knabbelbal clipped to its side
    "outfit_mewtwo_rugzakje": ("body", _B, "mewtwo_rugzak", [([-3.4, 11.0, 1.6], [6.8, 3.8, 5.6], 0), ([-3.6, 14.4, 1.4], [7.2, 0.6, 3.2], 0),
                                                          ([-2.2, 11.4, 7.1], [4.4, 2.6, 0.6], 0)]),
    "outfit_mewtwo_knabbelbal": ("body", _B, "mewtwo_bal", [([3.5, 11.6, 3.0], [2.4, 2.4, 2.4], 0)]),
    # the Guhtwo-staartje + nekbuisje: a costume tail (with a pink bow) and a soft tube on a little collar
    "outfit_mewtwo_nepstaart": ("tail", [0, 3.5, 12], "mewtwo_nepstaart", [([-1.4, 2.3, 11.0], [2.8, 2.6, 5.4], 0.1),
                                                                         ([-1.2, 3.0, 16.0], [2.4, 2.4, 3.4], 0.1),
                                                                         ([-1.1, 4.6, 18.4], [2.2, 3.2, 2.2], 0.1),
                                                                         ([-2.0, 7.6, 17.0], [4.0, 3.6, 3.8], 0.1)]),
    "outfit_mewtwo_nepstaart_strik": ("tail", [0, 3.5, 12], "mewtwo_strik", [([-2.2, 4.8, 11.2], [4.4, 1.6, 1.0], 0)]),
    "outfit_mewtwo_nekbuis": ("head", _H, "mewtwo_nepbuis", [([-1.2, 11.4, -1.6], [2.4, 2.2, 2.8], 0.1), ([-1.1, 10.3, 0.8], [2.2, 2.0, 2.6], 0.1)]),
    "outfit_mewtwo_kraagje": ("head", _H, "mewtwo_strik", [([-4.8, 1.4, -11.4], [9.6, 1.0, 0.6], 0)]),
    # the Mieuwguh-ballonnetje: a pink Mieuwguh head balloon on a white string, tied to the back
    "outfit_mewtwo_ballontouw": ("body", [0, 11, 5], "mewtwo_touw", [([-0.25, 11.0, 4.6], [0.5, 12.0, 0.5], 0), ([-0.25, 22.8, 4.4], [0.5, 4.0, 0.5], 0)]),
    "outfit_mewtwo_ballon": ("body", [0, 30, 4], "mewtwo_ballon", [([-3.4, 26.6, 1.2], [6.8, 6.2, 6.2], 0), ([-2.6, 32.6, 2.0], [5.2, 0.8, 4.6], 0)]),
    "outfit_mewtwo_ballon_oortjes": ("body", [0, 30, 4], "mewtwo_ballon", [([-3.6, 32.4, 3.6], [1.6, 2.2, 0.8], 0), ([2.0, 32.4, 3.6], [1.6, 2.2, 0.8], 0)]),
})
CLOTHES = ["mewtwo_trainerpetje", "mewtwo_trainerpakje", "mewtwo_staartje", "mew_ballonnetje"]

MEWTWO_FUR = (214, 204, 226)          # lilac grey
MEWTWO_PAARS = (128, 86, 168)         # the tail and the belly
PINK = (246, 150, 196)


def variants(rng, v):
    """The Guhtwo's fur and its two variant swatches (mewtwo_tex paints the belly, the eyes and the glow afterwards)."""
    def staart():
        a = v.fabric(MEWTWO_PAARS, rng, 6)
        a[: len(a) // 5] = a[: len(a) // 5] * 0.92                   # a slightly darker tip ring
        return a
    return {"mewtwo": (MEWTWO_FUR, {"mewtwo_staart": staart, "mewtwo_buis": lambda: v.fabric((200, 188, 214), rng, 5)})}


def clothes(rng, v):
    import numpy as np

    def pet():
        a = v.fabric((238, 96, 160), rng, 6)
        a[:, :] = a[:, :]
        return a

    def klep():
        return v.fabric((88, 50, 120), rng, 5)

    def logo():
        a = np.zeros((32, 32, 3), np.float32)
        a[:, :] = (250, 250, 248)                                     # the white front panel
        for y in range(32):
            for x in range(32):
                d = ((x - 15.5) ** 2 + ((y - 17) * 1.6) ** 2) ** 0.5
                if d < 9:
                    a[y, x] = (250, 206, 70)                          # the knabbelbal
                    for hx, hy, hr in ((12, 16, 2.2), (19, 19, 2.6), (17, 13, 1.6)):
                        if ((x - hx) ** 2 + ((y - hy) * 1.6) ** 2) ** 0.5 < hr:
                            a[y, x] = (236, 150, 50)                  # cheese holes
                for ex in (9, 22):
                    if abs(x - ex) + abs((y - 6) * 1.2) < 4.2 and y < 12:
                        a[y, x] = (240, 110, 170)                     # the two guh ears
        return a

    def vest():
        # a lilac trainer vest: a purple middle stripe, white collar edges, two pockets
        a = v.fabric((150, 120, 210), rng, 5)
        a[:, 14:18] = (96, 60, 150)
        a[:3, :] = (248, 248, 250)
        a[18:24, 4:10] = (126, 96, 186)
        a[18:24, 22:28] = (126, 96, 186)
        return a

    def rugzak():
        a = v.fabric((248, 206, 70), rng, 6)
        a[12:22, 10:22] = (236, 170, 60)
        a[15:18, 14:18] = (240, 110, 170)
        return a

    def bal():
        a = np.zeros((32, 32, 3), np.float32)
        for y in range(32):
            a[y, :] = (250, 206, 70) if y < 15 else ((80, 60, 90) if y < 18 else (250, 250, 248))
        a[13:19, 13:19] = (250, 250, 248)
        a[14:18, 14:18] = (240, 110, 170)
        return a

    def nepstaart():
        a = v.fabric(MEWTWO_PAARS, rng, 7)
        for y in range(0, 32, 4):                                     # soft plush ribs
            a[y, :] = a[y, :] * 0.93
        return a

    def strik():
        return v.fabric((240, 110, 170), rng, 5)

    def nepbuis():
        return v.fabric((206, 192, 222), rng, 6)

    def touw():
        return v.fabric((246, 246, 250), rng, 3)

    def ballon():
        a = v.fabric((250, 170, 206), rng, 4)
        # a shiny spot and a cute Mieuwguh face (big blue eyes, a smile, blushes) on every side
        a[4:8, 4:9] = (255, 228, 240)
        for ex in (10, 21):
            a[12:18, ex - 2:ex + 2] = (60, 110, 200)
            a[12:14, ex - 1:ex + 1] = (250, 250, 255)
        a[20, 14:18] = (150, 60, 110)
        a[21, 15:17] = (150, 60, 110)
        a[19:21, 6:9] = (255, 120, 170)
        a[19:21, 23:26] = (255, 120, 170)
        return a

    return {
        "mewtwo_trainerpetje": {"mewtwo_pet": pet, "mewtwo_klep": klep, "mewtwo_logo": logo},
        "mewtwo_trainerpakje": {"suit": vest, "mewtwo_rugzak": rugzak, "mewtwo_bal": bal},
        "mewtwo_staartje": {"mewtwo_nepstaart": nepstaart, "mewtwo_strik": strik, "mewtwo_nepbuis": nepbuis},
        "mew_ballonnetje": {"mewtwo_touw": touw, "mewtwo_ballon": ballon},
    }


def icons(ic):
    petje = ["................", "................", ".....aaaaaa.....", "....abbbbbba....", "...abbwwwwbba...", "...abwwllwwba...",
             "...abwlyylwba...", "...abwwyywwba...", "..aabbbbbbbbaa..", ".kkkkkkkkkaaa...", "kkkkkkkkk.......", "................"]
    rugzak = ["................", "....aaaaaa......", "...abbbbbba.....", "..abbbbbbbba....", "..abbccccbba....", "..abbcppcbbaggg.",
              "..abbccccbbagwwg", "..abbbbbbbbagwwg", "..abbbbbbbba.ggg", "..abbbbbbbba....", "...aaaaaaaa.....", "................"]
    staart = ["...........aaa..", "..........abbba.", "..........abbba.", "...........aaa..", "............a...", "............a...",
              "...........a....", "..........a.....", "...ppp...a......", "..pbbbaaa.......", "..pbbba.........", "...ppp.........."]
    ballon = ["....o....o......", "...ooo..ooo.....", "..oooooooooo....", ".oooooooooooo...", ".oowbooooowbo...", ".oobboooooobbo..",
              ".ooooommooooo...", "..oooooooooo....", "...oooooooo.....", ".....oooo.......", "......t.........", "......t.........",
              ".......t........", "......t.........", ".......t........", "......t........."]
    return {
        "mewtwo_trainerpetje": ic.icon(ic.pad(petje), {"a": (170, 60, 120), "b": (238, 96, 160), "w": (250, 250, 248), "l": (240, 110, 170),
                                                       "y": (250, 206, 70), "k": (88, 50, 120)}),
        "mewtwo_trainerpakje": ic.icon(ic.pad(rugzak), {"a": (170, 120, 40), "b": (248, 206, 70), "c": (236, 170, 60), "p": (240, 110, 170),
                                                        "g": (80, 60, 90), "w": (250, 250, 248)}),
        "mewtwo_staartje": ic.icon(ic.pad(staart), {"a": (80, 50, 110), "b": MEWTWO_PAARS, "p": (240, 110, 170)}),
        "mew_ballonnetje": ic.icon(ballon, {"o": (250, 170, 206), "w": (250, 250, 255), "b": (60, 110, 200), "m": (150, 60, 110),
                                            "t": (230, 230, 236)}),
    }


# =====================================================================================================================
# texts (Dutch, also in en_us)
# =====================================================================================================================
NOTITIE_TEKST = {
    1: "Dag 1. Mijn grote plan: een guh die DUBBEL zoveel kaasknabbels kan eten. Waarom? Omdat het kan. En omdat ik dan nooit meer "
       "restjes hoef op te ruimen. Njeg!",
    2: "Dag 12. Het recept: één pluisje guhhaar, een scheutje roze knabbelsap en heel veel liefde. De kloontank borrelt vrolijk. "
       "Ik heb mijn bril al drie keer kwijt gehad.",
    3: "Dag 30. Er zweeft een klein roze guhtje rond het eiland. Het giechelt elke keer als ik iets laat vallen (dus heel vaak). "
       "Ze lijkt op een heel oude tekening... Mieuwguh?",
    4: "Dag 44. Oeps. Ik heb per ongeluk DUBBEL knabbelsap in de tank gedaan. Het borrelt nu... paars? Dat stond niet in het recept. "
       "Of wel? Ik kan mijn recept niet vinden.",
    5: "Dag 45. BOEM! De tank is gebarsten! Er stapte een lichtpaarse guh uit, met een staart met een bolletje en een buisje in zijn "
       "nek. Hij keek me aan en zei: 'Njeg...?'",
    6: "Dag 46. Hij heeft ALLE kaasknabbels van het eiland opgegeten. Dubbel zoveel! Mijn wens is uitgekomen, VAHOEG! Maar hij kijkt "
       "zo verdrietig. Hij zit boven in de arena en wil niemand zien.",
}
ONDERDEEL_NAAM = {1: "Glazen tankpaneel", 2: "Koperen knabbelbuisje", 3: "Borrelpompje", 4: "Fles roze knabbelsap"}
ONDERDEEL_LORE = {1: "Een gebogen stuk glas, precies passend in de kloontank. Voorzichtig, niet laten vallen!",
                  2: "Een glimmend koperen buisje. Hier stroomt straks het knabbelsap doorheen.",
                  3: "Een klein pompje dat blubblubblub doet. Zonder borrels geen vrolijke tank!",
                  4: "Een fles vol roze, geurig knabbelsap. Ruikt naar kaas en aardbei. Njeg!"}
NOTITIE_PLEK = {1: "op het bureau van de professor", 2: "tussen de boeken in het kantoortje", 3: "bij een computer in de koepelhal",
                4: "naast de kloontank", 5: "boven aan de torentrap", 6: "in het boothuisje bij de steiger"}
ONDERDEEL_PLEK = {1: "in het boothuisje bij de steiger", 2: "in het kantoortje", 3: "halverwege de torentrap", 4: "op de tribune van de arena"}
PORTIE_KNABBELS = 32
PORTIE_SNACKS = 2

LANG = {
    # names
    "entity.guhs.guh.mewtwo": "Guhtwo",
    "entity.guhs.mewtwo": "Guhtwo",
    "entity.guhs.mew": "Mieuwguh",
    "entity.guhs.guh_npc.knabbelkloon": "Professor Knabbelkloon",
    "structure.guhs.kloon_eiland": "Het kloon-eiland",
    "structure.guhs.kloon_eiland.tooltip": "Een rotseiland in de Diepe Guhzee, met een lab onder een glazen koepel en een arena bovenop",
    # Guhdex
    "gui.guhs.guhdex.rarity.mewtwo": "Zeldzaamheid: Eén per speler (na het verhaal van het kloon-eiland)",
    "gui.guhs.guhdex.info.mewtwo": "Professor Knabbelkloon wilde een guh klonen die dubbel zoveel kaasknabbels kon eten. Het werd geen "
                                   "kloon, maar iets nieuws: de Guhtwo! Lichtgrijs-paars, met een paarse staart met een bolletje en "
                                   "een buisje van zijn achterhoofd naar zijn nek, maar met dikke guhwangen en guhoortjes. Hij zweeft een "
                                   "beetje boven de grond in een zachte paarse gloed, zweeft met jou op zijn rug over kleine gaatjes, "
                                   "trekt met knabbel-telekinese kaasknabbels en spulletjes binnen 8 blokken naar zich toe, en eet alles "
                                   "x2: dubbele hartjes van elke snack! Eén per speler, na het verhaal van het kloon-eiland. VAHOEG!",
    "gui.guhs.guhdex.rarity.mew": "Zeldzaamheid: Alleen rond het kloon-eiland (na het verhaal)",
    "gui.guhs.guhdex.info.mew": "Een piepklein roze zwevend guhtje met grote blauwe oogjes en een lange dunne staart met een bolletje. "
                                "Mieuwguh giechelt om alles, maakt koprolletjes in de lucht en is de beste vriendin van de Guhtwo. "
                                "Ze woont alleen rond het kloon-eiland en laat zich pas zien als je het verhaal van de kloontank kent. "
                                "Temmen kan niet (ze is van iedereen), zwaaien wel! Hihi, njeg!",
    "gui.guhs.guhdex.rarity.knabbelkloon": "Zeldzaamheid: Eén (in zijn kantoortje op het kloon-eiland)",
    "gui.guhs.guhdex.info.knabbelkloon": "Professor Knabbelkloon is de slimste en meest verstrooide guh van de Guhzee. Zijn brilletje staat "
                                         "altijd scheef, zijn labjas zit vol knabbelsapvlekken en zijn notities liggen overal. Hij wilde "
                                         "een guh klonen die dubbel zoveel kaasknabbels eet... en op een heel rare manier is dat gelukt. Njeg!",
    # blocks and items
    "block.guhs.mewtwo_kloontank": "Kloontank",
    "block.guhs.mewtwo_kloontank.lore": "De grote glazen tank van Professor Knabbelkloon, vol borrelend roze knabbelsap.",
    "block.guhs.mewtwo_tankwand": "Kloontank (glas)",
    "block.guhs.mewtwo_notitieplek": "Stapeltje labnotities",
    "block.guhs.mewtwo_onderdelenkist": "Onderdelenkist",
    "block.guhs.mewtwo_knabbelschaal": "Grote knabbelschaal",
    "block.guhs.mewtwo_knabbelschaal.lore": "Een reusachtige schaal voor een DUBBELE portie. Past er precies 32 kaasknabbels in. Of meer.",
    "block.guhs.mewtwo_computer": "Guh-computer",
    "block.guhs.mewtwo_computer.lore": "Een oude labcomputer met een knipperend guhgezichtje op het scherm. Bliep-njeg!",
    "block.guhs.mewtwo_reageerbuisjes": "Rekje reageerbuisjes",
    "block.guhs.mewtwo_reageerbuisjes.lore": "Roze, paars en lila knabbelsap in buisjes. Niet opdrinken! (Tenzij je dubbel honger hebt.)",
    "block.guhs.mewtwo_papieren": "Rommelige papieren",
    "block.guhs.mewtwo_papieren.lore": "Vol krabbels, formules en getekende kaasknabbels. Heel belangrijk. Waarschijnlijk.",
    "item.guhs.mewtwo_labnotitie": "Labnotitie",
    "item.guhs.mewtwo_labnotitie.nummer": "Labnotitie %s van 6",
    "item.guhs.mewtwo_labnotitie.lore": "Rechtsklik om te lezen. Een krabbel van Professor Knabbelkloon.",
    "item.guhs.mewtwo_tankonderdeel": "Tankonderdeel",
    "item.guhs.mewtwo_tankonderdeel.lore": "Bouw hem in bij de kloontank in de koepelhal (rechtsklik op de tank).",
    "item.guhs.mew_spawn_egg": "Mieuwguh-spawnei",
    "item.guhs.mewtwo_trainerpetje": "Trainerpetje",
    "item.guhs.mewtwo_trainerpakje": "Trainerpakje",
    "item.guhs.mewtwo_staartje": "Guhtwo-staartje + nekbuisje",
    "item.guhs.mew_ballonnetje": "Mieuwguh-ballonnetje",
    "item.guhs.mewtwo_trainerpetje.lore": "Met ons eigen logo: de knabbelbal met guhoortjes. Ik kies jou, njeg!",
    "item.guhs.mewtwo_trainerpakje.lore": "Een lila trainersvestje en een rugzakje met een knabbelbal eraan.",
    "item.guhs.mewtwo_staartje.lore": "Een zacht paars staartje met een bolletje en een nekbuisje. Nu ben jij ook een beetje Guhtwo!",
    "item.guhs.mew_ballonnetje.lore": "Een roze ballonnetje dat op Mieuwguh lijkt. Hij giechelt als het waait. Hihi!",
    # the guh menu button of the Guhtwo
    "gui.guhs.mewtwo.telekinese": "Telekinese",
    "gui.guhs.mewtwo.telekinese.tooltip": "Knabbel-telekinese aan of uit: trekt kaasknabbels en spulletjes binnen 8 blokken naar zich (en naar jou) toe",
    "gui.guhs.mewtwo.telekinese.aan": "%s doet de knabbel-telekinese aan: alles binnen 8 blokken zweeft naar jullie toe! VAHOEG!",
    "gui.guhs.mewtwo.telekinese.uit": "%s doet even geen telekinese. Hij laat alles lekker liggen, njeg.",
    "gui.guhs.mewtwo.x2": "x2!",
    # the story copy (VerhaalGuhs.kopie) and the questline texts
    "gui.guhs.verhaal.kopie.mewtwo": "Ik heb... dubbel zoveel honger. Dat is best vahoeg, eigenlijk.",
    "gui.guhs.mewtwo.kopie.mokt": "...Njeg. (De Guhtwo draait zich om en zweeft een beetje verdrietig heen en weer.)",
    "gui.guhs.mewtwo.kopie.honger": "Een maaltijd? Voor mij? Dubbel? ...Mijn maag zegt ja. Heel hard. GRRROMMEL.",
    "gui.guhs.mewtwo.kopie.al_getemd": "Jij hebt al een Guhtwo bij je! Ik blijf hier, bij Mieuwguh. Zeg maar dag van me. x2!",
    "gui.guhs.mewtwo.kopie.getemd_hier": "Kom je nog eens langs? Mieuwguh en ik bewaren een dubbele portie voor je. Njeg!",
    "gui.guhs.mewtwo.tem.vraag": "Jij hebt me geholpen. Mag ik... met jou mee? Dan ben ik nooit meer alleen. En ik eet netjes. Dubbel netjes.",
    "gui.guhs.mewtwo.tem.ja": "Ja! Kom maar mee, Guhtwo!",
    "gui.guhs.mewtwo.tem.nee": "Nog even niet",
    "gui.guhs.mewtwo.tem.gelukt": "De Guhtwo zweeft blij naar je toe: hij is nu van jou! Hij eet alles x2. VAHOEG!",
    "gui.guhs.mewtwo.tem.later": "Goed. Ik wacht hier. Zwevend. Met honger. (Hij knipoogt.)",
    # the professor
    "gui.guhs.mewtwo.prof.1": "O! Een bezoeker! Welkom op mijn kloon-eiland! Ik ben Professor Knabbelkloon. Let niet op de rommel. "
                              "En ook niet op de roze plasjes.",
    "gui.guhs.mewtwo.prof.2": "Ik had een droom: een guh die DUBBEL zoveel kaasknabbels kan eten. Dus ik probeerde er eentje te klonen, "
                              "in mijn prachtige kloontank.",
    "gui.guhs.mewtwo.prof.3": "Maar er ging iets mis. Er was een BOEM, de tank barstte, en al mijn labnotities vlogen door het hele lab! "
                              "Nu weet ik niet eens meer wat er precies gebeurd is. Njeg...",
    "gui.guhs.mewtwo.prof.4": "Wil jij mijn zes labnotities zoeken? Ze liggen overal: in mijn kantoortje, in de koepelhal, in de toren "
                              "en bij de steiger. Dan lezen we samen wat er gebeurd is!",
    "gui.guhs.mewtwo.prof.ja": "Ik zoek ze voor je!",
    "gui.guhs.mewtwo.prof.nee": "Straks misschien",
    "gui.guhs.mewtwo.prof.later": "Geen probleem! Ik ga intussen mijn bril zoeken. Die staat vast weer scheef. Njeg.",
    "gui.guhs.mewtwo.prof.zoek": "Je hebt %1$s van de 6 labnotities! Er liggen er nog %2$s ergens op het eiland: kijk %3$s.",
    "gui.guhs.mewtwo.prof.notities.1": "Alle zes! Wat een speurneus ben jij. Even lezen... hmm, dag 45... BOEM... O! Nu weet ik het weer!",
    "gui.guhs.mewtwo.prof.notities.2": "Het werd geen kloon. Het werd iets nieuws: de Guhtwo! En hij kan WEL dubbel zoveel eten. "
                                       "Mijn wens is dus toch uitgekomen, VAHOEG!",
    "gui.guhs.mewtwo.prof.notities.3": "Maar hij schrok zo van de knal, en van zijn eigen honger, dat hij boven in de arena is gaan zitten "
                                       "mokken. Arme guh.",
    "gui.guhs.mewtwo.prof.notities.4": "Als we de kloontank repareren, gaat er vast iets moois gebeuren. Er missen vier onderdelen. Ze "
                                       "liggen in de onderdelenkisten: in het boothuisje, in mijn kantoortje, in de toren en op de tribune.",
    "gui.guhs.mewtwo.prof.notities.5": "Hier, een cadeautje voor mijn beste lab-assistent: een echt trainerpetje, met ons eigen logo! "
                                       "Bouw de onderdelen in bij de tank in de koepelhal.",
    "gui.guhs.mewtwo.prof.onderdelen": "Nog %1$s onderdelen voor de kloontank. Je hebt er %2$s bij je: bouw ze in bij de tank in de koepelhal!",
    "gui.guhs.mewtwo.prof.maaltijd": "Een DUBBELE portie voor de grote knabbelschaal in de arena: nog %1$s kaasknabbels en %2$s lekkere "
                                     "snacks. Guhtwo en Mieuwguh wachten!",
    "gui.guhs.mewtwo.prof.klaar": "Kijk ze eens, Guhtwo en Mieuwguh: beste vriendjes! En jij bent de beste lab-assistent die ik ooit "
                                  "had. Mijn bril staat zelfs recht. Bijna.",
    "gui.guhs.mewtwo.prof.klaar_niet_getemd": "Guhtwo wacht boven in de arena op je. Volgens mij wil hij iets vragen... Njeg!",
    # finding things
    "gui.guhs.mewtwo.notitie.gevonden": "Labnotitie %1$s gevonden! (%2$s van de 6)",
    "gui.guhs.mewtwo.notitie.al": "Deze labnotitie heb je al. Er staat een getekende kaasknabbel op de achterkant.",
    "gui.guhs.mewtwo.notitie.titel": "Labnotitie %s van 6",
    "gui.guhs.mewtwo.notitie.alle": "Alle zes de labnotities! Breng ze naar Professor Knabbelkloon in zijn kantoortje.",
    "gui.guhs.mewtwo.kist.gevonden": "%s gevonden! Bouw hem in bij de kloontank in de koepelhal.",
    "gui.guhs.mewtwo.kist.al": "Deze kist is leeg. Het onderdeel heb je al!",
    "gui.guhs.mewtwo.kist.nog_niet": "Een kist vol schroefjes en buisjes... maar welk onderdeel heb je nodig? Vraag het eerst aan Professor "
                                     "Knabbelkloon.",
    "gui.guhs.mewtwo.kist.klaar": "Een lege kist. De kloontank is al helemaal gerepareerd, VAHOEG!",
    # the tank
    "gui.guhs.mewtwo.tank.kapot": "De kloontank is gebarsten. Er druppelt roze knabbelsap uit. Professor Knabbelkloon weet vast wat eraan "
                                  "moet gebeuren.",
    "gui.guhs.mewtwo.tank.ingebouwd": "%1$s ingebouwd! Nog %2$s te gaan.",
    "gui.guhs.mewtwo.tank.mist": "Er missen nog %s onderdelen. Zoek ze in de onderdelenkisten op het eiland.",
    "gui.guhs.mewtwo.tank.heel": "De kloontank borrelt vrolijk. Blub, blub, njeg!",
    "gui.guhs.mewtwo.tank.scene.1": "(Het laatste onderdeel klikt vast. De tank vult zich met roze knabbelsap. Blub... blub... BLUBBLUB!)",
    "gui.guhs.mewtwo.tank.scene.2": "Hihihi! Njeg-njeg!",
    "gui.guhs.mewtwo.tank.scene.3": "Dat... dat is Mieuwguh! Het allereerste roze guhtje, zeggen ze. Ze kwam kijken wat al dat gebubbel was!",
    "gui.guhs.mewtwo.tank.scene.4": "Guhtwo is nog steeds verdrietig. En wat helpt ALTIJD tegen verdriet? Juist: een grote "
                                    "knabbelmaaltijd. Een DUBBELE portie!",
    "gui.guhs.mewtwo.tank.scene.5": "Breng 32 kaasknabbels en 2 lekkere snacks naar de grote knabbelschaal in de arena. Hier, trek dit "
                                    "trainerpakje aan: dan zie je er heel officieel uit!",
    "gui.guhs.mewtwo.naam.mew": "Mieuwguh",
    "gui.guhs.mewtwo.naam.prof": "Professor Knabbelkloon",
    "gui.guhs.mewtwo.naam.mewtwo": "Guhtwo",
    "gui.guhs.mewtwo.naam.verteller": "Het kloon-eiland",
    # the meal
    "gui.guhs.mewtwo.schaal.nog_niet": "Een grote lege schaal. Voor wie zou die zijn? (Praat eerst met Professor Knabbelkloon.)",
    "gui.guhs.mewtwo.schaal.erin": "In de schaal: %1$s van de %2$s kaasknabbels en %3$s van de %4$s snacks.",
    "gui.guhs.mewtwo.schaal.meer": "Nog %1$s kaasknabbels en %2$s snacks voor een dubbele portie!",
    "gui.guhs.mewtwo.schaal.klaar": "Een smulschaal vol! Guhtwo en Mieuwguh zijn al gesmuld. Njeg!",
    "gui.guhs.mewtwo.maal.scene.1": "Is dat... allemaal voor mij? Een DUBBELE portie?",
    "gui.guhs.mewtwo.maal.scene.2": "Hihi! Samen! Samen smullen!",
    "gui.guhs.mewtwo.maal.scene.3": "(Guhtwo en Mieuwguh smullen samen van de grote knabbelschaal. Knabbel knabbel, x2! x2!)",
    "gui.guhs.mewtwo.maal.scene.4": "Ik dacht dat ik raar was, omdat ik dubbel zoveel wil eten. Maar dubbel zoveel eten... is eigenlijk "
                                    "best vahoeg.",
    "gui.guhs.mewtwo.maal.scene.5": "Mieuwguh zegt dat ik haar beste vriendje ben. En jij bent ook mijn vriendje. Wil je... straks nog "
                                    "even bij me komen? Ik wil je iets vragen. Njeg!",
    "gui.guhs.mewtwo.maal.cadeau": "Mieuwguh geeft je giechelend een ballonnetje dat op haar lijkt, en Guhtwo een zacht staartje. "
                                   "Klik de Guhtwo aan in de arena!",
    # Mieuwguh
    "gui.guhs.mewtwo.mew.giechel": "Hihihi!",
    "gui.guhs.mewtwo.mew.zwaai": "Mieuwguh maakt een koprolletje in de lucht en zwaait naar je. Hihi, njeg!",
    "gui.guhs.mewtwo.niet_bouwen": "Niet verbouwen, njeg! Dit is het lab van Professor Knabbelkloon (hij vindt zijn spullen al zo slecht terug).",
    # the whole set
    "gui.guhs.mewtwo.klaar": "Het verhaal van het kloon-eiland is af! De Guhtwo mag nu met je mee (eenmalig). VAHOEG!",
    # sounds
    "subtitles.guhs.mewtwo.mew_giechel": "Mieuwguh giechelt",
    "subtitles.guhs.mewtwo.tank_borrel": "Kloontank borrelt",
    "subtitles.guhs.mewtwo.tank_klik": "Onderdeel klikt vast",
    "subtitles.guhs.mewtwo.tank_heel": "Kloontank gerepareerd",
    "subtitles.guhs.mewtwo.telekinese": "Knabbel-telekinese",
    "subtitles.guhs.mewtwo.x2": "Guhtwo smult x2",
    "subtitles.guhs.mewtwo.notitie": "Papier ritselt",
}
for n, t in NOTITIE_TEKST.items():
    LANG[f"gui.guhs.mewtwo.notitie.{n}"] = t
for n, t in ONDERDEEL_NAAM.items():
    LANG[f"item.guhs.mewtwo_tankonderdeel.{n}"] = t
    LANG[f"item.guhs.mewtwo_tankonderdeel.{n}.lore"] = ONDERDEEL_LORE[n]
for n, t in NOTITIE_PLEK.items():
    LANG[f"gui.guhs.mewtwo.plek.notitie.{n}"] = t
    # (the talking screen gets its arguments as plain text: one sentence per spot)
    LANG[f"gui.guhs.mewtwo.prof.zoek.{n}"] = ("Je hebt %1$s van de 6 labnotities! Er liggen er nog %2$s ergens op het eiland. "
                                             f"Kijk eens {t}. Daar leg ik altijd dingen neer... denk ik. Njeg!")
for n, t in ONDERDEEL_PLEK.items():
    LANG[f"gui.guhs.mewtwo.plek.onderdeel.{n}"] = t
    LANG[f"gui.guhs.mewtwo.prof.onderdelen_zoek.{n}"] = (f"Nog %1$s onderdelen voor de kloontank. Kijk eens in de onderdelenkist {t}!")

SOUNDS = {  # event: (sound, pitch, volume)
    "mewtwo.mew_giechel": ("guhs:creche.babygiechel", 1.45, 0.9),
    "mewtwo.tank_borrel": ("minecraft:block.bubble_column.upwards_ambient", 1.2, 0.8),
    "mewtwo.tank_klik": ("minecraft:block.smithing_table.use", 1.5, 0.8),
    "mewtwo.tank_heel": ("minecraft:block.beacon.activate", 1.3, 1.0),
    "mewtwo.telekinese": ("minecraft:block.amethyst_block.chime", 1.6, 0.7),
    "mewtwo.x2": ("guhs:entity.guh.eat", 1.3, 1.0),
    "mewtwo.notitie": ("minecraft:item.book.page_turn", 1.1, 1.0),
}


def geluiden(h):
    def patch(d):
        for event, (sound, pitch, volume) in SOUNDS.items():
            d[event] = {"sounds": [{"name": sound, "type": "event", "pitch": pitch, "volume": volume}], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements: the tab Guhverhalen (verhalen/mewtwo_*) and the hidden quest/mewtwo_* for FTB
# =====================================================================================================================
QUEST = ["mewtwo_welkom", "mewtwo_notities", "mewtwo_tank", "mewtwo_mew", "mewtwo_maaltijd", "mewtwo_telekinese", "mewtwo_x2"]


def advancements(h):
    z = verhaal.zichtbaar
    z(h, "verhalen", "mewtwo_eiland", "root", "minecraft:purple_stained_glass", "task", "Het kloon-eiland",
      "Vind het rotseiland met het lab onder de glazen koepel, ergens in de Diepe Guhzee",
      criteria={"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{NAME}"}}}}})
    z(h, "verhalen", "mewtwo_welkom", "mewtwo_eiland", "guhs:mewtwo_papieren", "task", "Een verstrooide professor",
      "Beloof Professor Knabbelkloon dat je zijn zes labnotities zoekt")
    z(h, "verhalen", "mewtwo_notities", "mewtwo_welkom", "guhs:mewtwo_labnotitie", "task", "Speurneus van het lab",
      "Vind alle zes de labnotities en lees wat er met de kloontank gebeurd is")
    z(h, "verhalen", "mewtwo_tank", "mewtwo_notities", "guhs:mewtwo_tankonderdeel", "task", "Blub, blub, gerepareerd!",
      "Bouw de vier onderdelen in bij de gebarsten kloontank")
    z(h, "verhalen", "mewtwo_mew", "mewtwo_tank", "minecraft:pink_dye", "task", "Hihihi!",
      "Ontmoet Mieuwguh, het kleine roze zwevende guhtje van het kloon-eiland")
    z(h, "verhalen", "mewtwo_maaltijd", "mewtwo_mew", "guhs:mewtwo_knabbelschaal", "goal", "Een dubbele portie",
      "Vul de grote knabbelschaal in de arena: Guhtwo en Mieuwguh smullen samen en worden beste vriendjes")
    z(h, "verhalen", "mewtwo_getemd", "mewtwo_maaltijd", "minecraft:amethyst_shard", "challenge", "x2, njeg!",
      "Neem de Guhtwo mee: hij zweeft, trekt kaasknabbels naar zich toe en eet alles dubbel")
    z(h, "verhalen", "mewtwo_telekinese", "mewtwo_getemd", "minecraft:ender_pearl", "task", "Knabbel-telekinese",
      "Laat je Guhtwo een kaasknabbel naar zich toe laten zweven")
    z(h, "verhalen", "mewtwo_x2", "mewtwo_getemd", "guhs:kaas_knabbels", "task", "Dubbel lekker",
      "Geef je Guhtwo een snack: dubbele hartjes, x2!")
    z(h, "verhalen", "mewtwo_trainer", "mewtwo_maaltijd", "guhs:mewtwo_trainerpetje", "task", "Guh-trainer",
      "Verzamel het hele kloon-eiland-pakje: trainerpetje, trainerpakje, Guhtwo-staartje en het Mieuwguh-ballonnetje",
      criteria={c: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{c}"}]}} for c in CLOTHES})
    for q in QUEST:
        h.w(f"{h.D}/advancement/quest/{q}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for page in ("mewtwo", "mew", "knabbelkloon"):
        h.w(f"{h.D}/advancement/quest/seen_{page}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


# =====================================================================================================================
# blocks, items, loot, tags, spawn egg
# =====================================================================================================================
def data(h):
    D = h.D
    for b in BLOKKEN:
        # quest blocks drop nothing (they belong to the island); deco drops itself
        if b in ("mewtwo_computer", "mewtwo_reageerbuisjes", "mewtwo_papieren", "mewtwo_knabbelschaal"):
            h.self_drop(b)


# =====================================================================================================================
# game test templates
# =====================================================================================================================
def test_templates(h):
    # a lab floor of 14 x 14 with a kloontank (+ its glass parts) at (4, 1, 4), a knabbelschaal at (10, 1, 10), a note spot and a
    # parts crate: MewtwoGameTests
    t = h.Structure((14, 7, 14))
    for x in range(14):
        for z in range(14):
            t.set(x, 0, z, "minecraft:smooth_quartz")
    for x in range(3, 6):
        for z in range(3, 6):
            for y in range(1, 4):
                if (x, y, z) != (4, 1, 4):
                    t.set(x, y, z, "guhs:mewtwo_tankwand")
            t.set(x, 4, z, "minecraft:polished_deepslate")
    t.set(4, 1, 4, "guhs:mewtwo_kloontank", {"facing": "south"})
    t.set(10, 1, 10, "guhs:mewtwo_knabbelschaal")
    t.set(10, 1, 3, "guhs:mewtwo_notitieplek", {"nummer": "3", "facing": "south"})
    t.set(11, 1, 3, "guhs:mewtwo_onderdelenkist", {"soort": "2", "facing": "south"})
    t.save("mewtwo_test_lab")
    w = h.Structure((20, 5, 20))
    for x in range(20):
        for z in range(20):
            w.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    w.save("mewtwo_test_wei")


# =====================================================================================================================
# the self-check
# =====================================================================================================================
def selfcheck(h):
    A = h.A
    missing = []
    for b in BLOKKEN:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block {b}")
    for i in ITEMS + CLOTHES:
        if f"item.guhs.{i}" not in h.NL:
            missing.append(f"lang item {i}")
        if not os.path.exists(f"{A}/models/item/{i}.json") and i not in CLOTHES:
            missing.append(f"item model {i}")
    for root, _dirs, files in os.walk(f"{A}/models"):
        for f in files:
            if f.startswith("mewtwo") or f.startswith("mew_"):
                import json
                model = json.load(open(os.path.join(root, f), encoding="utf-8"))
                for t in list(model.get("textures", {}).values()) + [layer for layer in []]:
                    if isinstance(t, str) and t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    for g in ("mew", "guh_npc_knabbelkloon"):
        if not os.path.exists(f"{A}/geo/entity/{g}.geo.json"):
            missing.append(f"geo {g}")
    # every text key the Java code uses exists
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "mewtwo")
    import re
    for root, _dirs, files in os.walk(java):
        for f in files:
            for key in re.findall(r'"((?:gui|item|block|entity)\.guhs\.mewtwo[a-z0-9_.]*[a-z0-9_])"', open(os.path.join(root, f), encoding="utf-8").read()):
                if key not in h.NL and not any(k.startswith(key + ".") for k in h.NL):
                    missing.append(f"lang {key} ({f})")
    # lore: never "te vads", never "hamster"
    for key, text in LANG.items():
        low = text.lower()
        if "te vads" in low or "hamster" in low or "pok" in low and "mon" in low:
            missing.append(f"lore: {key}")
    if missing:
        raise SystemExit("mewtwo self-check failed:\n  " + "\n  ".join(sorted(set(missing))))


def build(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    tex.build(h)
    pool, b = bouw.build(h)
    verhaal_wereld.regio_structuur(h, NAME, pool, "guhmension_zee", "zee", SALT, hoek=0, afstand=160, reach=bouw.REACH,
                                   voorrang=850, grond_y=bouw.G, ring=bouw.REACH - 2)
    geluiden(h)
    advancements(h)
    data(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter Guhverhalen, section "Het kloon-eiland"); nothing is locked
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("mewtwo_eiland", "Het kloon-eiland", "Ergens in de &9Diepe Guhzee&r staat een rotseiland met een glazen koepel en een arena "
      "bovenop. Vind het &5kloon-eiland&r (superkompas: Verhalen) en klim het steigertje op.", "minecraft:purple_stained_glass",
      [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 8),), shape="circle", xp=100)
    q("mewtwo_professor", "Professor Knabbelkloon", "In het rommelige kantoortje naast de koepelhal zit &6Professor Knabbelkloon&r, "
      "met zijn brilletje scheef. Zeg hallo en beloof dat je zijn labnotities zoekt.", "guhs:mewtwo_papieren",
      [fq.adv("seen_knabbelkloon"), fq.adv("mewtwo_welkom")], rewards=(("guhs:kaas_knabbels", 6),), xp=100)
    q("mewtwo_notities", "Zes labnotities", "Zoek de zes &6labnotities&r: in het kantoortje, in de koepelhal, bij de kloontank, boven "
      "aan de torentrap en in het boothuisje. Lees ze (rechtsklik) en breng ze naar de professor. Je krijgt het &dtrainerpetje&r!",
      "guhs:mewtwo_labnotitie", [fq.adv("mewtwo_notities")], rewards=(("guhs:kaas_knabbels", 12),), xp=200)
    q("mewtwo_tank", "Blub, blub, gerepareerd!", "Vind de vier &6tankonderdelen&r in de onderdelenkisten (boothuisje, kantoortje, "
      "toren, tribune) en bouw ze in bij de gebarsten &5kloontank&r in de koepelhal (rechtsklik op de tank).",
      "guhs:mewtwo_tankonderdeel", [fq.adv("mewtwo_tank")], rewards=(("guhs:kaas_knabbels", 12),), xp=200)
    q("mewtwo_mew", "Hihihi!", "Als de tank weer borrelt, komt er iemand kijken: &dMieuwguh&r, een klein roze zwevend guhtje. Zet haar "
      "in je Guhdex! Na het verhaal zweeft ze altijd rond het kloon-eiland.", "minecraft:pink_dye",
      [fq.adv("mewtwo_mew"), fq.adv("seen_mew")], rewards=(("guhs:kaas_knabbels", 8),), xp=150)
    q("mewtwo_maaltijd", "Een dubbele portie", "Breng &632 kaasknabbels&r en &62 snacks&r naar de grote knabbelschaal in de arena "
      "(rechtsklik op de schaal). Guhtwo en Mieuwguh smullen samen: x2! Je krijgt het Guhtwo-staartje en het Mieuwguh-ballonnetje.",
      "guhs:mewtwo_knabbelschaal", [fq.adv("mewtwo_maaltijd")], rewards=(("guhs:kaas_knabbels", 16),), shape="gear", xp=300)
    q("mewtwo_getemd", "x2, njeg!", "Klik daarna de &5Guhtwo&r in de arena aan en neem hem mee (één per speler). Hij zweeft, zweeft "
      "met jou op zijn rug over kleine gaatjes, trekt kaasknabbels binnen 8 blokken naar zich toe en eet alles dubbel.",
      "minecraft:amethyst_shard", [fq.adv("verhaal_getemd_mewtwo"), fq.adv("seen_mewtwo")], rewards=(("guhs:vahoege_vads_ingot", 1),),
      shape="heart", xp=400)
    q("mewtwo_telekinese", "Knabbel-telekinese", "Gooi een kaasknabbel een paar blokken van je Guhtwo vandaan en kijk hoe hij hem "
      "naar zich toe laat zweven. (Met de knop Telekinese in zijn guhmenu zet je het aan en uit.)", "minecraft:ender_pearl",
      [fq.adv("mewtwo_telekinese")], rewards=(("guhs:kaas_knabbels", 8),), xp=100)
    q("mewtwo_x2", "Dubbel lekker", "Geef je Guhtwo een snack: hij smult x2 en krijgt dubbele hartjes. VAHOEG!", "guhs:guh_cupcake",
      [fq.adv("mewtwo_x2")], rewards=(("guhs:kaas_knabbels", 8),), xp=100)
    q("mewtwo_petje", "Het trainerpetje", "Van de professor, voor de zes labnotities. Met ons eigen logo: de knabbelbal met guhoortjes!",
      "guhs:mewtwo_trainerpetje", [fq.item("guhs:mewtwo_trainerpetje")], rewards=(("guhs:kaas_knabbels", 4),), xp=50)
    q("mewtwo_pakje", "Het trainerpakje", "Van de professor, als de kloontank weer borrelt: een lila vestje en een rugzakje met een "
      "knabbelbal.", "guhs:mewtwo_trainerpakje", [fq.item("guhs:mewtwo_trainerpakje")], rewards=(("guhs:kaas_knabbels", 4),), xp=50)
    q("mewtwo_staartje", "Het Guhtwo-staartje", "Van de Guhtwo, na de grote knabbelmaaltijd: een paars staartje met een bolletje en "
      "een nekbuisje.", "guhs:mewtwo_staartje", [fq.item("guhs:mewtwo_staartje")], rewards=(("guhs:kaas_knabbels", 4),), xp=50)
    q("mewtwo_ballonnetje", "Het Mieuwguh-ballonnetje", "Van Mieuwguh, na de grote knabbelmaaltijd: een roze ballonnetje dat op haar lijkt.",
      "guhs:mew_ballonnetje", [fq.item("guhs:mew_ballonnetje")], rewards=(("guhs:kaas_knabbels", 4),), xp=50)
