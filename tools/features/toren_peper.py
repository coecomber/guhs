"""
bbq2 (toren-peper): the Rookguh-vuurtoren and the Pepertuin met kas (with two Guhdrankjes). Java: feature/torenpeper.

  - de Rookguh-vuurtoren (structure guhs:rookguh_vuurtoren: toren_peper_bouw.py, a "grot" of wereld.bbq_structuur with its
    guaranteed copy in sector 9) with the Torenwachter-guh and his questline "vuurtoren" (5 steps, per player): the lamp
    (block torenpeper_vuurtorenlamp), the lost Rookguhs (entity torenpeper_verdwaalde_rookguh, the Rookguh's own model), the
    seinlantaarn, the lampkooltje; reward: the Bezorgguhtje-fluitje (tech-bezorg's item) and the keeper's coat
  - de Pepertuin met kas (structure guhs:pepertuin, sector 10) with the Peperteler-guh and his questline "pepertuin" (4 steps):
    the kweekbak (per-player plants), the peperplant (one crop, three peppers by the ground it stands on), the three
    peppers, the two Guhdrankjes of Brouwsel PEPERVUUR / PEPERZOET (effect torenpeper_peperadem), the peperslinger
  - both guhs' models (toren_peper_modellen.py), the two clothes pieces (marker toren_peper), loot, tags, sounds,
    advancements, FTB quests (chapter guhs_barbecuether, two sections)

Hooks: build(h), ftb(fq), FTB_SECTIES, BONES / CLOTHES / clothes / icons. The wiki entries are in toren_peper_wiki.py.
"""
import json
import os
import re

import numpy as np
from PIL import Image

from . import bbq2
from . import spiesburcht_tex
from . import toren_peper_bouw as bouw
from . import toren_peper_modellen as modellen
from . import verhaal_motor
from . import wereld

NAME = "toren_peper"
SALT_TOREN, SALT_TUIN = 21301401, 21301411          # slice 14 (CONTRACT_130 3); the guaranteed sets are these + 7
TOREN, TUIN = "rookguh_vuurtoren", "pepertuin"
SOORTEN = {"groen": "njegpeper", "rood": "vahoegpeper", "roze": "snoeppeper"}

FTB_SECTIES = [("toren_peper_vuurtoren", "De Rookguh-vuurtoren", "npc:torenwachterguh",
                ["toren_peper_toren", "toren_peper_vuurtoren_1", "toren_peper_vuurtoren_2", "toren_peper_vuurtoren_3", "toren_peper_vuurtoren_4",
                 "toren_peper_vuurtoren_5", "toren_peper_fluitje", "toren_peper_jas", "toren_peper_lamp"]),
               ("toren_peper_pepertuin", "De Pepertuin", "npc:pepertelerguh", None)]

# =====================================================================================================================
# the outfit (make_guh_variants: BONES / clothes; make_clothes_icons: icons; make_resources: CLOTHES item models)
# =====================================================================================================================
_B = [0, 6, 6]
_H = [0, 6, -2]
BONES = {
    # the keeper's coat is the suit in navy with gold buttons; on top of it a red and white striped collar round the neck
    "outfit_torenpeper_jas_kraag": ("body", _B, "torenpeper_kraag", [([-6.8, 10.4, -2.0], [13.6, 1.6, 2.2], 0), ([-7.4, 3.2, -2.0], [1.4, 7.4, 2.2], 0),
                                                                    ([6.0, 3.2, -2.0], [1.4, 7.4, 2.2], 0)]),
    # the peperslinger: a string all round the neck with peppers hanging from it under the chin
    "outfit_torenpeper_slinger": ("body", _B, "torenpeper_touw", [([-6.6, 10.6, -1.8], [13.2, 1.0, 1.4], 0), ([-7.6, 2.4, -1.8], [1.0, 8.4, 1.4], 0),
                                                                ([6.6, 2.4, -1.8], [1.0, 8.4, 1.4], 0)]),
    "outfit_torenpeper_slinger_voor": ("head", _H, "torenpeper_touw", [([-5.6, 0.6, -11.6], [11.2, 0.8, 1.0], 0)]),
    "outfit_torenpeper_slinger_rood": ("head", _H, "torenpeper_peper_rood", [([-4.8, -1.6, -11.9], [1.4, 2.4, 1.4], 0), ([0.2, -2.2, -11.9], [1.4, 3.0, 1.4], 0)]),
    "outfit_torenpeper_slinger_groen": ("head", _H, "torenpeper_peper_groen", [([-2.4, -1.9, -11.9], [1.4, 2.7, 1.4], 0), ([5.2, -1.2, -11.9], [1.2, 2.0, 1.2], 0)]),
    "outfit_torenpeper_slinger_roze": ("head", _H, "torenpeper_peper_roze", [([2.7, -1.5, -11.9], [1.6, 2.2, 1.6], 0), ([-6.6, -1.0, -11.9], [1.2, 1.8, 1.2], 0)]),
}
CLOTHES = ["torenpeper_wachtersjas", "torenpeper_peperslinger"]
JAS, JAS_DONKER, GOUD = (40, 56, 96), (28, 40, 72), (250, 208, 84)
ROOD, GROEN, ROZE = (214, 44, 36), (86, 170, 62), (242, 122, 150)


def clothes(rng, v):
    def jas():
        a = v.fabric(JAS, rng, 7)
        a[:, 14:18] = JAS_DONKER                      # the double-breasted front
        for y in range(4, 30, 8):
            a[y:y + 3, 10:13] = GOUD
            a[y:y + 3, 19:22] = GOUD
        a[28:, :] = JAS_DONKER                        # the hem
        return a

    def peper(kleur):
        def paint():
            a = v.fabric(kleur, rng, 6)
            a[:5, :] = (70, 140, 56)                  # the green cap where it hangs from the string
            a[8:14, 8:12] = tuple(min(255, c + 60) for c in kleur)
            return a
        return paint

    return {"torenpeper_wachtersjas": {"suit": jas, "torenpeper_kraag": lambda: v.stripes((206, 50, 46), (240, 240, 236), rng, 4)},
            "torenpeper_peperslinger": {"torenpeper_touw": lambda: v.fabric((176, 140, 84), rng, 8), "torenpeper_peper_rood": peper(ROOD),
                                        "torenpeper_peper_groen": peper(GROEN), "torenpeper_peper_roze": peper(ROZE)}}


SLINGER_ICON = ["................", "..tt........tt..", ".t............t.", ".t............t.", "t..............t", "t..............t",
                ".t............t.", ".tsS........sSt.", "..rs..tttt..sp..", ".rRr.sSttsS.pPp.", ".rRr.gG..rR.pPp.", ".rr..gG..rR..p..",
                "..r..gg..rr.....", ".....g....r.....", "................", "................"]


def icons(ic):
    jacket = [r[:4] + "".join(("g" if (i in (6, 9) and ch == "b" and y % 2 == 0) else ch) for i, ch in enumerate(r[4:12], 4)) + r[12:]
              for y, r in enumerate(ic.SHIRT)]
    return {"torenpeper_wachtersjas": ic.icon(ic.pad(jacket), {"a": JAS_DONKER, "b": JAS, "g": GOUD}),
            "torenpeper_peperslinger": ic.icon(SLINGER_ICON, {"t": (176, 140, 84), "s": (70, 140, 56), "S": (110, 180, 80), "r": (170, 30, 26), "R": ROOD,
                                                              "g": (56, 120, 44), "G": GROEN, "p": (200, 84, 116), "P": ROZE})}


# =====================================================================================================================
# sounds
# =====================================================================================================================
SOUNDS = {
    "torenpeper.lamp_aan": [{"name": "minecraft:block.beacon.activate", "type": "event", "pitch": 1.3, "volume": 0.9},
                            {"name": "minecraft:item.firecharge.use", "type": "event", "pitch": 0.8, "volume": 0.6}],
    "torenpeper.misthoorn": [{"name": "minecraft:item.goat_horn.sound.6", "type": "event", "pitch": 0.6, "volume": 1.0}],
    "torenpeper.pluk": [{"name": "minecraft:block.sweet_berry_bush.pick_berries", "type": "event", "pitch": 0.8},
                        {"name": "minecraft:block.nether_wart.break", "type": "event", "pitch": 1.2}],
    "torenpeper.heet": [{"name": "minecraft:block.fire.extinguish", "type": "event", "pitch": 1.4, "volume": 0.6},
                        {"name": "guhs:entity.guh.hurt", "type": "event", "pitch": 1.3, "volume": 0.7}],
}
SUBTITLES = {"torenpeper.lamp_aan": "Vuurtorenlamp floept aan", "torenpeper.misthoorn": "Misthoorn toetert",
             "torenpeper.pluk": "Peper geplukt", "torenpeper.heet": "Iemand heeft een hete peper op"}

# =====================================================================================================================
# Dutch texts
# =====================================================================================================================
W = "quest.guhs.torenpeper.torenwachter."
P = "quest.guhs.torenpeper.peperteler."
HINT = "quest.guhs.torenpeper.hint."
GUI = "gui.guhs.torenpeper."
LANG = {
    "block.guhs.torenpeper_vuurtorenlamp": "Vuurtorenlamp",
    "block.guhs.torenpeper_peperplant": "Peperplant",
    "block.guhs.torenpeper_kweekbak": "Kweekbak",
    "item.guhs.torenpeper_peperzaadjes": "Peperzaadjes",
    "item.guhs.torenpeper_peperzaadjes.lore": "Eén plantje, drie pepers: de grond bepaalt welke",
    "item.guhs.torenpeper_peperzaadjes.grond.groen": "As-aarde: groene Njegpeper",
    "item.guhs.torenpeper_peperzaadjes.grond.rood": "Gloeikool: rode Vahoegpeper",
    "item.guhs.torenpeper_peperzaadjes.grond.roze": "Pindasaus-nylium: roze Snoeppeper",
    "item.guhs.torenpeper_peperzaadjes.glas": "Onder glas groeit hij drie keer zo snel",
    "item.guhs.torenpeper_njegpeper": "Njegpeper",
    "item.guhs.torenpeper_njegpeper.lore": "Groen en mild. Nou ja, mild... njeg",
    "item.guhs.torenpeper_vahoegpeper": "Vahoegpeper",
    "item.guhs.torenpeper_vahoegpeper.lore": "Rood en heet! Rauw opeten op eigen risico. Voor het Pepervuurdrankje",
    "item.guhs.torenpeper_snoeppeper": "Snoeppeper",
    "item.guhs.torenpeper_snoeppeper.lore": "Roze en zoet als een knuffel. Voor het Peperzoetdrankje",
    "item.guhs.torenpeper_pepervuurdrankje": "Pepervuurdrankje",
    "item.guhs.guhdrankje.pepervuur.lore": "Kaasbouillon met Vahoegpeper. Je hakt en graaft als een vuurtje, met vlammetjes uit je snoet. Njeg!",
    "item.guhs.torenpeper_peperzoetdrankje": "Peperzoetdrankje",
    "item.guhs.guhdrankje.peperzoet.lore": "Kaasbouillon met Snoeppeper. Warm en zoet vanbinnen: twee hartjes erbij",
    "quest.guhs.guhbrouwketel.brouwsel.pepervuur": "Pepervuurdrankje",
    "quest.guhs.guhbrouwketel.brouwsel.peperzoet": "Peperzoetdrankje",
    "item.guhs.torenpeper_seinlantaarn": "Seinlantaarn",
    "item.guhs.torenpeper_seinlantaarn.lore": "Verdwaalde Rookguhs volgen wie hem vasthoudt. Rood-wit, net als de toren",
    "item.guhs.torenpeper_lampkooltje": "Lampkooltje",
    "item.guhs.torenpeper_lampkooltje.lore": "Geperst gloeikoolgruis voor de vuurtorenlamp. Nog warm",
    "item.guhs.torenpeper_wachtersjas": "Wachtersjas van de vuurtoren",
    "item.guhs.torenpeper_peperslinger": "Peperslinger",
    "gui.guhs.kledingbron.toren_peper": "De Torenwachter-guh en de Peperteler-guh (Guhbarbecuether)",
    "entity.guhs.torenpeper_verdwaalde_rookguh": "Verdwaalde Rookguh",
    "effect.guhs.torenpeper_peperadem": "Peperadem",
    # the lamp and the lost Rookguhs
    GUI + "lamp.uit": "De lamp is uit. Vraag de Torenwachter-guh wat hij nodig heeft",
    GUI + "lamp.geen_kooltje": "Je hebt een lampkooltje nodig. De Torenwachter-guh heeft er vast nog een",
    GUI + "lamp.aan": "Floep! De lamp brandt. TOEEET, daar gaat de misthoorn",
    GUI + "lamp.brandt_zoek": "De lamp brandt. Rookguhs thuis: %s van %s",
    GUI + "lamp.brandt": "De lamp brandt zolang jij in de buurt bent. Njeg!",
    GUI + "rookguh.thuis": "VAHOEG! Rookguh %s van %s is thuis",
    GUI + "rookguh.van_ander": "%s zoekt het licht van iemand anders",
    GUI + "rookguh.volgt": "%s volgt je seinlantaarn. Loop maar naar de vuurtoren",
    GUI + "rookguh.pak_lantaarn": "%s ziet je niet in de rook. Houd de seinlantaarn vast",
    GUI + "nodig.rookguhs": "Verdwaalde Rookguhs thuis",
    GUI + "nodig.peperdrankje": "Een peperdrankje (Pepervuur of Peperzoet)",
    GUI + "beloning.drankje": "Het andere peperdrankje",
    GUI + "heet": "HEET! HEET! HEEEET! Njeg njeg njeg!",
    # the kweekbakken
    GUI + "kweek.eerst_praten": "Dit is een kweekbak van de Peperteler-guh. Vraag hem eerst om peperzaadjes",
    GUI + "kweek.leeg": "Een lege kweekbak. Plant er een peperzaadje in (houd het vast)",
    GUI + "kweek.geplant": "Geplant! Onder glas is hij over %s seconden rijp",
    GUI + "kweek.groeit": "Je plantje groeit. Nog %s seconden",
    GUI + "kweek.geplukt": "Geplukt: %s! Je zaadje krijg je terug",
    GUI + "optie.helpen": "Ik steek hem aan!",
    GUI + "optie.rookguhs": "Hoe gaat het met de Rookguhs?",
    GUI + "optie.fluitje": "Ik ben mijn fluitje kwijt",
    GUI + "optie.kweken": "Ik wil pepers kweken!",
    GUI + "optie.uitleg": "Welke peper groeit waar?",
    GUI + "optie.zaadjes": "Ik heb geen zaadjes meer",
    # the Torenwachter-guh
    W + "hallo": "Ahoi, njeg! Ik ben de Torenwachter-guh. Zie je die lamp daarboven? Uit. Al weken. En zonder licht vinden de Rookguhs de weg "
                 "naar huis niet door al die rook. Ze dwalen maar rond, dun van de honger. Help je me de lamp weer aan te krijgen?",
    W + "gruis": "Vahoeg! De lamp brandt op gloeikool. Breng me %s gloeikoolgruis, dan pers ik er een lampkooltje van. Gloeikool hangt hier "
                 "overal aan het plafond te gloeien.",
    W + "gruis_tekort": "Je hebt %s gloeikoolgruis, ik heb er %s nodig voor één lampkooltje. Hak een brok gloeikool stuk, njeg.",
    W + "kooltje": "Kijk eens, een lampkooltje! Nog warm. Klim de toren in, helemaal naar boven, en leg het in de lamp. En neem mijn "
                   "seinlantaarn mee: die heb je straks nodig.",
    W + "kooltje_kwijt": "Lampkooltje kwijt? Die dingen rollen ook overal heen. Hier, ik had er nog eentje in mijn zuidwester.",
    W + "lamp": "De trap is binnen, rondje na rondje. Boven leg je het lampkooltje in de lamp. Ik blijf hier, mijn pootjes zijn te kort "
                "voor al die treden.",
    W + "lantaarn_kwijt": "Seinlantaarn kwijt? Zonder lantaarn volgt geen Rookguh je. Hier is mijn reserve, njeg.",
    W + "zoek": "Hij brandt! Maar de verdwaalde Rookguhs durven niet alleen. Nog %s zwerven er rond de toren. Loop naar ze toe met de "
                "seinlantaarn in je poot, dan zweven ze achter je aan tot ze het licht zien.",
    W + "klaar": "Alle drie thuis, en ze hebben de hele knabbelbak leeggegeten. VAHOEG! Hier: een Bezorgguhtje-fluitje. Die kleine "
                 "stepguhtjes luisteren ernaar. En deze wachtersjas is voor je guh, dan is hij ook een beetje torenwachter.",
    W + "na": "Ahoi, lampaansteker! Zolang jij in de buurt bent, brandt de lamp. De Rookguhs zwaaien elke avond. Nou ja, het is hier "
              "altijd avond.",
    W + "rookguhs": "Dik en roze, zoals het hoort! Kom je in de rook nog eentje tegen die mager is? Geef hem zes kaasknabbels, dan zweeft "
                    "hij zo naar huis.",
    W + "fluitje_heb_je": "Je fluitje zit gewoon in je zak, njeg. Kijk maar.",
    W + "fluitje_morgen": "Ik heb je vandaag al een nieuw fluitje gegeven. Morgen heb ik er weer eentje gesneden.",
    W + "fluitje_nieuw": "Weggewaaid? Dat hebben fluitjes. Hier is een nieuwe, wees er zuinig op!",
    HINT + "gruis": "breng de Torenwachter-guh vier gloeikoolgruis",
    HINT + "lamp": "klim de vuurtoren in en leg het lampkooltje in de lamp",
    HINT + "rookguhs": "zoek de drie verdwaalde Rookguhs rond de toren en breng ze met de seinlantaarn naar het licht",
    HINT + "allemaal_thuis": "alle Rookguhs zijn thuis: ga terug naar de Torenwachter-guh",
    HINT + "klaar_toren": "zet een Stepstation neer en fluit je Bezorgguhtje",
    # the Peperteler-guh
    P + "hallo": "Njeg! Welkom in de Pepertuin. Ik kweek hier de enige plant die het in al die rook naar zijn zin heeft: de peper. Eén "
                 "plantje, drie pepers. Welke je krijgt, hangt af van de grond. Wil je het zelf proberen? Ik heb drie kweekbakken voor je "
                 "klaarstaan in de kas.",
    P + "kweek": "Hier zijn drie peperzaadjes. In de kas staan drie kweekbakken: as-aarde, gloeikool en pindasaus-nylium. Plant in elke bak "
                 "een zaadje. Onder glas zijn ze zo rijp, en jouw plantjes zijn echt van jou: niemand plukt ze voor je neus weg.",
    P + "kweek_nog": "Nog %s soort(en) te plukken. Rijpe pepers hangen er knalrood, groen of roze bij. Gewoon op de bak klikken, njeg.",
    P + "zaadjes_kwijt": "Zaadjes op? Opgegeten zeker. Ze zijn niet eens lekker! Hier, nieuwe. Deze gaan de kweekbak in.",
    P + "brouw": "Drie soorten, in één keer goed! Nu het echte werk: peperdrank. Hier is een snufje grillspiespoeder, een emmer kaassaus "
                 "en drie flesjes. Achter in de kas staat mijn Guhbrouwketel. Stook hem op, giet de saus erin, roer er een Vahoegpeper "
                 "of een Snoeppeper door en tap een flesje. Groene Njegpepers zijn om op te knabbelen, niet om te brouwen.",
    P + "brouw_nog": "De ketel staat achter in de kas. Poeder erin, kaassaus erin, een rode of roze peper erdoor, even laten borrelen en dan "
                     "een flesje vullen. Ik wil proeven, njeg!",
    P + "brouw_weer": "Poeder op en geen saus meer? Tja, mijn ketel is van iedereen: wie het eerst roert, die brouwt. Hier, een nieuw "
                      "snufje grillspiespoeder en een verse emmer kaassaus. Zodra JOUW peper erdoor gaat, telt het voor jou, njeg.",
    P + "peper_op": "Geen rode of roze peper meer? Hier is een zaadje. Plant het in de bak met gloeikool of pindasaus-nylium: je "
                    "kweekbakken blijven van jou.",
    P + "klaar": "Laat eens ruiken... Oeh. OEH. Dat is een goeie! Jij bent een echte peperbrouwer. Hier: een zak zaadjes voor thuis, een "
                 "flesje van het andere drankje en een peperslinger voor je guh. Op as-aarde groeien ze overal, maar onder glas gaat "
                 "het drie keer zo snel.",
    P + "na": "Daar is mijn beste leerling! Je kweekbakken staan altijd voor je klaar. En pluk gerust een peper uit de kijkbedden, daar "
              "staan ze voor.",
    P + "uitleg": "As-aarde: de groene Njegpeper, lekker mild. Gloeikool: de rode Vahoegpeper, heet! Die gaat in het Pepervuurdrankje. "
                  "Pindasaus-nylium: de roze Snoeppeper, zoet. Die gaat in het Peperzoetdrankje. Zet er glas boven en ze groeien drie "
                  "keer zo snel. Rijp? Rechtsklik om te plukken, dan groeit de plant gewoon door.",
    P + "zaadjes_genoeg": "Je hebt nog zaadjes in je zak, njeg. Die doen het prima.",
    P + "zaadjes_morgen": "Vandaag heb je al zaadjes van me gehad. Morgen heb ik weer nieuwe gedroogd.",
    P + "zaadjes_nieuw": "Helemaal door je zaadjes heen? Hier zijn er twee. Tip: wie een rijpe plant uitgraaft in plaats van plukt, krijgt "
                         "er zaadjes bij.",
    HINT + "kweek": "plant een peperzaadje in elk van de drie kweekbakken in de kas en pluk de rijpe pepers",
    HINT + "nog_plukken": "weer een peper geplukt: nu de andere kweekbakken nog",
    HINT + "geplukt": "je hebt alle drie de pepers gekweekt: ga terug naar de Peperteler-guh",
    HINT + "brouw": "brouw een peperdrankje in de Guhbrouwketel achter in de kas",
    HINT + "gebrouwen": "je eerste peperdrankje! Laat het de Peperteler-guh proeven",
    HINT + "borrelt": "jouw peper borrelt in de ketel: vul straks een flesje en ga terug naar de Peperteler-guh",
    HINT + "klaar_tuin": "plant je peperzaadjes thuis op as-aarde, gloeikool of pindasaus-nylium, het liefst onder glas",
}

ADV = [  # name, parent, icon, frame, title, description
    ("toren_peper_vuurtoren", "binnen", "guhs:torenpeper_seinlantaarn", "goal", "Licht in de rook",
     "Steek de lamp van de Rookguh-vuurtoren aan en breng drie verdwaalde Rookguhs thuis"),
    ("toren_peper_pepertuin", "binnen", "guhs:torenpeper_vahoegpeper", "goal", "Peperbrouwer",
     "Kweek de drie pepers in de kas van de Pepertuin en brouw je eerste peperdrankje"),
    ("toren_peper_heet", "toren_peper_pepertuin", "guhs:torenpeper_vahoegpeper", "task", "Heet! Heet! Heet!",
     "Eet een rauwe Vahoegpeper. Niemand zei dat het slim was"),
]
VERBORGEN = ["toren_peper_geplukt", "toren_peper_heet"]

STAPPEN_TOREN = [  # (stapnaam, nu, waar): the questline "vuurtoren" in the Guhdex
    ("Praat met de Torenwachter-guh", "Praat met de Torenwachter-guh. Zijn lamp is uit.", "De Rookguh-vuurtoren in de Guhbarbecuether"),
    ("Breng gloeikoolgruis", "Breng de Torenwachter-guh vier gloeikoolgruis. Hij perst er een lampkooltje van.",
     "Gloeikool hangt aan de plafonds van de Guhbarbecuether"),
    ("Steek de lamp aan", "Klim de wenteltrap op en leg het lampkooltje in de lamp.", "Boven in de Rookguh-vuurtoren"),
    ("Breng drie Rookguhs thuis", "Zoek de verdwaalde Rookguhs rond de toren. Met de seinlantaarn in je poot volgen ze je naar het licht.",
     "Rond de Rookguh-vuurtoren"),
    ("Terug naar de Torenwachter-guh", "Alle Rookguhs zijn thuis. De Torenwachter-guh heeft iets voor je.", "De Rookguh-vuurtoren in de Guhbarbecuether"),
]
STAPPEN_TUIN = [
    ("Praat met de Peperteler-guh", "Praat met de Peperteler-guh. Hij heeft zaadjes voor je.", "De Pepertuin in de Guhbarbecuether"),
    ("Kweek drie pepers", "Plant een zaadje in elk van de drie kweekbakken in de kas en pluk de rijpe pepers.", "De kas van de Pepertuin"),
    ("Brouw een peperdrankje", "Roer een Vahoegpeper of een Snoeppeper door de kaasbouillon in een Guhbrouwketel en tap een flesje.",
     "De brouwhoek achter in de kas"),
    ("Laat de Peperteler-guh proeven", "Breng je peperdrankje naar de Peperteler-guh.", "De Pepertuin in de Guhbarbecuether"),
]

# =====================================================================================================================
# textures
# =====================================================================================================================
PEPER_ICONS = {
    "njegpeper": ["................", "................", "........gg......", ".......gG.......", "......gGGg......", ".....ohhhho.....", "....ohHHhhho....",
                  "....ohHhhhdo....", "....ohhhhhdo....", "....ohhhhhdo....", "....ohhhhddo....", ".....ohhhddo....", ".....ohhddo.....",
                  "......oddo......", ".......oo.......", "................"],
    "vahoegpeper": ["................", "..........gg....", ".........gG.....", "........gGg.....", ".......ohhg.....", "......ohHhho....",
                    ".....ohHhhho....", ".....ohhhhdo....", "....ohhhhdo.....", "....ohhhddo.....", "...ohhhddo......", "...ohhddo.......",
                    "..ohhddo........", "..ohddo.........", "..oddo..........", "...oo..........."],
    "snoeppeper": ["................", ".......gg.......", ".......gG.......", "......gGGg......", "....oohhhhoo....", "...ohHHhhhhho...",
                   "..ohHHhhhhhhdo..", "..ohHhhhhhhhdo..", "..ohhhhhhhhhdo..", "..ohhhhhhhhddo..", "...ohhhhhhhdo...", "...ohhhhhhddo...",
                   "....ohhhhddo....", ".....ohhddo.....", "......oddo......", ".......oo......."],
}
PEPER_PAL = {
    "njegpeper": {"o": (36, 84, 30, 255), "h": (86, 170, 62, 255), "H": (160, 220, 120, 255), "d": (56, 124, 44, 255)},
    "vahoegpeper": {"o": (110, 16, 16, 255), "h": (214, 44, 36, 255), "H": (255, 140, 110, 255), "d": (158, 26, 24, 255)},
    "snoeppeper": {"o": (150, 56, 90, 255), "h": (242, 122, 150, 255), "H": (255, 204, 216, 255), "d": (206, 88, 122, 255)},
}
STEEL = {"g": (50, 110, 44, 255), "G": (96, 170, 70, 255)}
ZAADJES = ["................", "................", "................", "......ss........", ".....sSs..ss....", "......s..sSs....", "..ss......s.....",
           ".sSs...ss.......", "..s...sSs...ss..", ".......s...sSs..", "....ss......s...", "...sSs..ss......", "....s..sSs......", "........s.......",
           "................", "................"]
LANTAARN = ["................", ".......kk.......", "......k..k......", "......k..k......", ".....kkkkkk.....", "....kMMMMMMk....",
            "....krrYYwwk....", "....krYYYYwk....", "....krYOOYwk....", "....krYOOYwk....", "....krYYYYwk....", "....krrYYwwk....",
            "....kMMMMMMk....", ".....kkkkkk.....", "......kkkk......", "................"]
KOOLTJE = ["................", "................", "................", "......xxxx......", "....xxoOoxxx....", "...xoOyyOoxxx...",
           "..xxOyYYyOoxx...", "..xoOyYYyyOox...", "..xoOyyYYyOox...", "..xxoOyyyOoxx...", "...xxoOOOoxx....", "....xxxooxx.....",
           "......xxxx......", "................", "................", "................"]
ITEM_PAL = {"s": (196, 160, 96, 255), "S": (250, 236, 180, 255), "k": (52, 46, 52, 255), "M": (196, 110, 78, 255), "r": (206, 50, 46, 255),
            "w": (240, 240, 236, 255), "Y": (255, 226, 120, 255), "O": (255, 160, 60, 255), "x": (44, 36, 38, 255), "o": (214, 96, 30, 255),
            "y": (255, 214, 96, 255)}
# the plant, as a cross: four stages (the last one with three peppers: P, p dark, h light)
PLANT = [
    ["................"] * 11 + [".......l........", "......lLs.l.....", ".......ssLl.....", "........s.......", "........s......."],
    ["................"] * 7 + ["........l.......", ".......lLl......", "....ll..s..l....", "...lLLl.s.lLl...", "....lldss.dll...", "......dssd......",
                                ".....lLssLl.....", "......l.s.l.....", "........s......."],
    ["................"] * 3 + [".......ll.......", "......lLLl..l...", "..ll...ls..lLl..", ".lLLl..ss.dll...", "..lldd.ss.d.....", "....dssssd..ll..",
                                "...l..dss.dlLLl.", "..lLl..ssdd.ll..", "...lld.ssf......", ".....dsssd......", "....lLsssLl.....", ".....l.ss.l.....",
                                ".......ss......."],
    ["................", ".......ll.......", "......lLLl.ll...", "..ll..lLLl.lLl..", ".lLLl..ls.dll...", ".lLLdd.ss.d.....", "..ld.dssssd.ll..",
     "..h..l.ss.dlLLl.", ".hPp.lLssdd.ll..", ".PPp..dssd..h...", ".PPp...ss..hPp..", "..Pp.h.ss..PPp..", "..p.hPpssd.PPp..", "....PPpssLl.Pp..",
     "....PPp.s.l.p...", ".....p..ss......"],
]
PLANT_PAL = {"s": (60, 120, 50, 255), "l": (78, 160, 62, 255), "L": (126, 200, 94, 255), "d": (44, 96, 40, 255), "f": (250, 250, 236, 255)}


def _grid(h, rows, pal):
    for r in rows:
        assert len(r) == 16, (r, len(r))
    assert len(rows) == 16, len(rows)
    return h.grid(rows, pal)


def lamp_texture(aan, boven=False):
    """The lamp: an iron cage (dark frame, rivets on the corners) round glass; lit: a white-hot heart in a yellow glow."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            rand_ = min(x, y, 15 - x, 15 - y)
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if rand_ == 0:
                c = (40, 36, 42)
            elif rand_ == 1 or (boven and d > 5.5):
                c = (70, 64, 72)
            elif x in (7, 8) and not boven and rand_ < 4:
                c = (58, 52, 60)                                  # a bar up the middle of each pane, above and below the heart
            elif aan:
                t = max(0.0, min(1.0, d / 6.5))
                c = (255, int(252 - 60 * t), int(214 - 140 * t))
            else:
                c = (74, 84, 96) if (x + y) % 5 else (98, 110, 122)
                if d < 2.6:
                    c = (36, 30, 34)                              # the cold coal
            px[x, y] = c + (255,)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[x, y] = (120, 112, 120, 255)
    return img


def kweekbak_textures(h):
    """Side: dark planks with an iron band; top: a wooden rim round the soil of each kind."""
    plank = h.ramp(h.vanilla("block/spruce_planks"), (58, 40, 30), (128, 92, 62))
    zij = plank.copy()
    px = zij.load()
    for x in range(16):
        for y in (9, 10):
            px[x, y] = (60, 56, 62, 255)
    for x in (2, 13):
        px[x, 9] = (130, 124, 130, 255)
    h.save(zij, "block", "torenpeper_kweekbak_zij.png")
    h.save(plank, "block", "torenpeper_kweekbak_onder.png")
    for soort, grond in (("groen", "as_aarde"), ("rood", "gloeikool"), ("roze", "pindasaus_nylium")):
        top = Image.open(os.path.join(h.TEX, "block", f"{grond}.png")).convert("RGBA").crop((0, 0, 16, 16))
        tp = top.load()
        for x in range(16):
            for y in range(16):
                rand_ = min(x, y, 15 - x, 15 - y)
                if rand_ == 0:
                    tp[x, y] = plank.getpixel((x, y))
                elif rand_ == 1:
                    tp[x, y] = tuple(int(c * 0.7) for c in plank.getpixel((x, y))[:3]) + (255,)
        h.save(top, "block", f"torenpeper_kweekbak_{soort}.png")


def textures(h):
    for naam, rows in PEPER_ICONS.items():
        h.save(_grid(h, rows, {**PEPER_PAL[naam], **STEEL}), "item", f"torenpeper_{naam}.png")
        h.item_model(f"torenpeper_{naam}")
    for naam, rows in (("peperzaadjes", ZAADJES), ("seinlantaarn", LANTAARN), ("lampkooltje", KOOLTJE)):
        h.save(_grid(h, rows, ITEM_PAL), "item", f"torenpeper_{naam}.png")
        h.item_model(f"torenpeper_{naam}")
    for drankje in ("pepervuur", "peperzoet"):
        h.save(spiesburcht_tex.drankje(spiesburcht_tex.BROUWSELS[drankje]), "item", f"torenpeper_{drankje}drankje.png")
        h.item_model(f"torenpeper_{drankje}drankje")
    # the plant: three green stages, and a ripe one per kind of pepper
    for stage in range(3):
        h.save(_grid(h, PLANT[stage], PLANT_PAL), "block", f"torenpeper_peperplant_{stage}.png")
    for soort, peper in SOORTEN.items():
        pal = dict(PLANT_PAL)
        pal.update({"P": PEPER_PAL[peper]["h"], "p": PEPER_PAL[peper]["d"], "h": PEPER_PAL[peper]["H"]})
        h.save(_grid(h, PLANT[3], pal), "block", f"torenpeper_peperplant_3_{soort}.png")
    for aan in (False, True):
        h.save(lamp_texture(aan), "block", f"torenpeper_vuurtorenlamp{'_aan' if aan else ''}.png")
        h.save(lamp_texture(aan, boven=True), "block", f"torenpeper_vuurtorenlamp_boven{'_aan' if aan else ''}.png")
    kweekbak_textures(h)
    # the effect icon: a little flame out of a round snoet
    icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    ip = icon.load()
    for x in range(18):
        for y in range(18):
            if ((x - 5.5) / 4.6) ** 2 + ((y - 10.5) / 4.6) ** 2 <= 1:
                ip[x, y] = (244, 150, 180, 255)
    for (x, y) in ((4, 9), (7, 9)):
        ip[x, y] = (40, 24, 44, 255)
    vlam = {(10, 11): "o", (11, 10): "o", (11, 11): "y", (11, 12): "o", (12, 9): "o", (12, 10): "y", (12, 11): "w", (12, 12): "y", (12, 13): "o",
            (13, 8): "r", (13, 9): "o", (13, 10): "y", (13, 11): "y", (13, 12): "o", (14, 9): "r", (14, 10): "o", (14, 11): "o", (15, 10): "r",
            (16, 9): "r"}
    kleur = {"r": (214, 44, 36, 255), "o": (255, 140, 40, 255), "y": (255, 220, 100, 255), "w": (255, 250, 220, 255)}
    for (x, y), c in vlam.items():
        ip[x, y] = kleur[c]
    h.save(icon, "mob_effect", "torenpeper_peperadem.png")
    # the two guhs: their names here, their models and textures in toren_peper_modellen
    bbq2.stub_npc(h, "torenwachterguh", "Torenwachter-guh", 0.58, sat=0.36, val=0.92)
    bbq2.stub_npc(h, "pepertelerguh", "Peperteler-guh", 0.02, sat=1.7, val=0.98)
    modellen.torenwachter(h)
    modellen.peperteler(h)


# =====================================================================================================================
# block models
# =====================================================================================================================
def kruis(tex, y0=0.0):
    """Two crossed planes with this texture, standing on y0 (pixels): the plant in a kweekbak stands on the trough's soil."""
    def vlak(frm, to):
        return {"from": frm, "to": to, "shade": False, "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
                "faces": {f: {"uv": [0, 0, 16, 16], "texture": tex} for f in (("north", "south") if frm[2] == to[2] else ("west", "east"))}}
    return [vlak([0.8, y0, 8], [15.2, y0 + 16, 8]), vlak([8, y0, 0.8], [8, y0 + 16, 15.2])]


def modellen_blokken(h):
    A = h.A
    # the lamp
    for aan in ("", "_aan"):
        h.w(f"{A}/models/block/torenpeper_vuurtorenlamp{aan}.json", {
            "parent": "minecraft:block/cube_column", "render_type": "minecraft:cutout",
            "textures": {"end": f"guhs:block/torenpeper_vuurtorenlamp_boven{aan}", "side": f"guhs:block/torenpeper_vuurtorenlamp{aan}"}})
    h.w(f"{A}/blockstates/torenpeper_vuurtorenlamp.json", {"variants": {"lit=false": {"model": "guhs:block/torenpeper_vuurtorenlamp"},
                                                                         "lit=true": {"model": "guhs:block/torenpeper_vuurtorenlamp_aan"}}})
    h.w(f"{A}/models/item/torenpeper_vuurtorenlamp.json", {"parent": "guhs:block/torenpeper_vuurtorenlamp_aan"})
    # the plant: a cross per stage; the ripe one per kind
    def plant_tex(age, soort):
        return f"guhs:block/torenpeper_peperplant_{age}" + (f"_{soort}" if age == 3 else "")
    for age in range(3):
        h.w(f"{A}/models/block/torenpeper_peperplant_{age}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                                  "textures": {"cross": plant_tex(age, None)}})
    for soort in SOORTEN:
        h.w(f"{A}/models/block/torenpeper_peperplant_3_{soort}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                                      "textures": {"cross": plant_tex(3, soort)}})
    h.w(f"{A}/blockstates/torenpeper_peperplant.json", {"variants": {
        f"age={age},soort={soort}": {"model": plant_tex(age, soort)} for age in range(4) for soort in SOORTEN}})
    # the kweekbak: a trough (half a block) with its soil on top; with a plant: the same cross standing on the soil
    for soort in SOORTEN:
        bak = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
               "textures": {"zij": "guhs:block/torenpeper_kweekbak_zij", "onder": "guhs:block/torenpeper_kweekbak_onder",
                            "boven": f"guhs:block/torenpeper_kweekbak_{soort}", "particle": "guhs:block/torenpeper_kweekbak_zij"},
               "elements": [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {
                   "down": {"uv": [0, 0, 16, 16], "texture": "#onder", "cullface": "down"}, "up": {"uv": [0, 0, 16, 16], "texture": "#boven"},
                   **{f: {"uv": [0, 8, 16, 16], "texture": "#zij", "cullface": f} for f in ("north", "south", "west", "east")}}}]}
        h.w(f"{A}/models/block/torenpeper_kweekbak_{soort}_0.json", bak)
        for groei in range(1, 5):
            model = dict(bak)
            model["textures"] = dict(bak["textures"], plant=plant_tex(groei - 1, soort))
            model["elements"] = bak["elements"] + kruis("#plant", 8.0)
            h.w(f"{A}/models/block/torenpeper_kweekbak_{soort}_{groei}.json", model)
    h.w(f"{A}/blockstates/torenpeper_kweekbak.json", {"variants": {
        f"groei={groei},soort={soort}": {"model": f"guhs:block/torenpeper_kweekbak_{soort}_{groei}"} for groei in range(5) for soort in SOORTEN}})
    h.w(f"{A}/models/item/torenpeper_kweekbak.json", {"parent": "guhs:block/torenpeper_kweekbak_rood_4"})


# =====================================================================================================================
# data: loot, recipes, tags, sounds
# =====================================================================================================================
def data(h):
    D = h.D
    rijp = {"condition": "minecraft:block_state_property", "block": "guhs:torenpeper_peperplant", "properties": {"age": "3"}}
    # the plant: always its seed; a ripe one also its peppers and often a second seed
    h.w(f"{D}/loot_table/blocks/torenpeper_peperplant.json", {"type": "minecraft:block", "functions": [{"function": "minecraft:explosion_decay"}], "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:torenpeper_peperzaadjes"}]},
        {"rolls": 1, "bonus_rolls": 0, "conditions": [rijp, {"condition": "minecraft:random_chance", "chance": 0.6}],
         "entries": [{"type": "minecraft:item", "name": "guhs:torenpeper_peperzaadjes"}]}] + [
        {"rolls": 1, "bonus_rolls": 0,
         "conditions": [{"condition": "minecraft:block_state_property", "block": "guhs:torenpeper_peperplant", "properties": {"age": "3", "soort": soort}}],
         "entries": [{"type": "minecraft:item", "name": f"guhs:torenpeper_{peper}", "functions": h.count_fn(2, 3)}]}
        for soort, peper in SOORTEN.items()]})
    h.self_drop("torenpeper_vuurtorenlamp")
    h.self_drop("torenpeper_kweekbak")
    h.w(f"{D}/loot_table/entities/torenpeper_verdwaalde_rookguh.json", {"type": "minecraft:entity", "pools": []})

    def kist(naam, rolls, inhoud):
        h.w(f"{D}/loot_table/chests/{naam}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": [
                {"type": "minecraft:item", "name": item, "weight": wgt, "functions": h.count_fn(lo, hi)} for item, wgt, lo, hi in inhoud]}]})
    # the keeper's chest: what a lighthouse keeper has lying around; the kas: bottles and a handful of the harvest
    kist("toren_peper_wachter", (3, 5), (("guhs:kaas_knabbels", 12, 3, 8), ("guhs:gloeikoolgruis", 8, 1, 3), ("minecraft:lantern", 5, 1, 1),
                                          ("minecraft:string", 6, 1, 3), ("minecraft:cod", 5, 1, 2), ("minecraft:spyglass", 1, 1, 1),
                                          ("minecraft:glass_bottle", 4, 1, 2)))
    kist("toren_peper_kas", (3, 5), (("minecraft:glass_bottle", 10, 1, 3), ("guhs:kaas_knabbels", 10, 2, 6), ("guhs:torenpeper_njegpeper", 8, 1, 3),
                                      ("minecraft:bone_meal", 6, 1, 3), ("guhs:pindascheutjes", 5, 1, 3), ("guhs:torenpeper_vahoegpeper", 3, 1, 2),
                                      ("guhs:torenpeper_snoeppeper", 3, 1, 2), ("guhs:grillspiespoeder", 2, 1, 1)))
    # a lamp of your own: it burns whenever somebody who lit the lighthouse is near (so: for you, after the questline)
    h.shaped("torenpeper_vuurtorenlamp", ["IGI", "GKG", "IGI"], {"I": "guhs:roosterijzer_tralies", "G": "minecraft:glass", "K": "guhs:gloeikool"},
             "guhs:torenpeper_vuurtorenlamp")
    h.add_tag("guhs/tags/block/torenpeper/pepergrond", ["guhs:as_aarde", "guhs:as_blok", "guhs:gloeikool", "guhs:pindasaus_nylium", "guhs:mosterd_nylium",
                                                        "minecraft:soul_sand", "minecraft:soul_soil", "minecraft:magma_block"])
    h.add_tag("guhs/tags/block/torenpeper/hete_grond", ["guhs:gloeikool", "minecraft:magma_block"])
    h.add_tag("guhs/tags/block/torenpeper/zoete_grond", ["guhs:pindasaus_nylium"])
    h.add_tag("guhs/tags/item/brouwsel/pepervuur", ["guhs:torenpeper_vahoegpeper"])
    h.add_tag("guhs/tags/item/brouwsel/peperzoet", ["guhs:torenpeper_snoeppeper"])
    h.add_tag("guhs/tags/item/loaned", ["guhs:torenpeper_lampkooltje"])
    h.add_tag("minecraft/tags/block/crops", ["guhs:torenpeper_peperplant"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:torenpeper_vuurtorenlamp"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:torenpeper_kweekbak"])

    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# the two buildings
# =====================================================================================================================
def structuren(h):
    problems = []
    for naam, maak in ((TOREN, bouw.toren), (TUIN, bouw.tuin)):
        s, fout = maak(h)
        problems += [f"{naam}: {p}" for p in fout]
        s.save(naam)
    if problems:
        raise SystemExit("toren_peper: the buildings are not right:\n  " + "\n  ".join(problems[:40]))
    wereld.bbq_structuur(h, TOREN, soort="grot", titel="Rookguh-vuurtoren",
                         tooltip="De vuurtoren die de Rookguhs de weg naar huis wijst, met de Torenwachter-guh (Guhbarbecuether)",
                         biomes=["houtskoolvlakte", "asdal", "rookdelta"], salt=SALT_TOREN, templates=[(TOREN, 1)], spacing=34, separation=12,
                         gegarandeerd=dict(sector=9, min=250, max=900), grootte=28, vlak=10, hoogte=26)
    wereld.bbq_structuur(h, TUIN, soort="grot", titel="Pepertuin",
                         tooltip="Een kas vol pepers, met de Peperteler-guh en zijn Guhbrouwketel (Guhbarbecuether)",
                         biomes=["satebos", "worstenwoud", "houtskoolvlakte"], salt=SALT_TUIN, templates=[(TUIN, 1)], spacing=30, separation=11,
                         gegarandeerd=dict(sector=10, min=250, max=900), grootte=26, vlak=8, hoogte=13)
    # the centre jigsaw is in layer 0 and the pool says where the ground really is (toren_peper_bouw.Bouw.midden): the ground
    # layer lands on the cave floor and the terrain is smoothed towards it
    for naam in (TOREN, TUIN):
        def grond(pool, delta=bouw.G + 1):
            for e in pool["elements"]:
                el = {"element_type": "guhs:grond_single_pool_element"}
                el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
                el["ground_level_delta"] = delta
                e["element"] = el
        h.patch_json(f"{h.D}/worldgen/template_pool/{naam}/start.json", grond)
    # the game test room: a bare floor of houtskoolsteen
    t = h.Structure((21, 12, 21))
    for x in range(21):
        for z in range(21):
            t.set(x, 0, z, "guhs:houtskoolsteen")
    t.save("torenpeper_test_kamer")


# =====================================================================================================================
# texts, advancements, the questlines
# =====================================================================================================================
def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for event, text in SUBTITLES.items():
        h.lang(f"subtitles.guhs.{event}", text, text)
    for name in VERBORGEN:
        bbq2.verborgen(h, name)
    for name, parent, icon, frame, titel, tekst in ADV:
        bbq2.zichtbaar(h, "barbecuether", name, parent, icon, frame, titel, tekst)
    verhaal_motor.verhaallijn(
        h, "vuurtoren", "De Rookguh-vuurtoren",
        "De lamp van de vuurtoren is uit, en de Rookguhs vinden de weg naar huis niet meer. Steek hem aan en breng drie verdwaalde Rookguhs "
        "naar het licht.", STAPPEN_TOREN,
        klaar=("De lamp brandt zolang jij in de buurt bent, en de Rookguhs zijn thuis. Vahoeg!", "De Rookguh-vuurtoren"),
        kort={"0": "Praat met de Torenwachter-guh", "1": "Breng vier gloeikoolgruis", "2": "Steek de vuurtorenlamp aan",
              "3": "Breng drie verdwaalde Rookguhs naar het licht", "4": "Ga terug naar de Torenwachter-guh"})
    verhaal_motor.verhaallijn(
        h, "pepertuin", "De Pepertuin",
        "De Peperteler-guh leert je pepers kweken: één plantje, drie pepers. En wat je ervan brouwt.", STAPPEN_TUIN,
        klaar=("Je bent een peperbrouwer. Kweek thuis verder: onder glas gaat het het snelst.", "Thuis, of in de kweekbakken van de Pepertuin"),
        kort={"0": "Praat met de Peperteler-guh", "1": "Kweek de drie pepers in de kas", "2": "Brouw een peperdrankje",
              "3": "Laat de Peperteler-guh proeven"})


# =====================================================================================================================
# self-check
# =====================================================================================================================
def java_plekken(bestand):
    """The template coordinates in a Java file of feature/torenpeper: ({name: [(x, y, z), ...]}, G, NPC_YAW)."""
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "torenpeper", bestand), encoding="utf-8").read()
    uit = {}
    for m in re.finditer(r"public static final [\w<>]+ (\w+) = (.*?);", src, re.S):
        punten = [tuple(int(v) for v in p) for p in re.findall(r"new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)", m.group(2))]
        if punten:
            uit[m.group(1)] = punten
    g = re.search(r"public static final int G = (\d+);", src)
    yaw = re.search(r"public static final float NPC_YAW = (-?[\d.]+)f;", src)
    return uit, int(g.group(1)) if g else None, float(yaw.group(1)) if yaw else None


def java_getal(bestand, naam):
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "torenpeper", bestand), encoding="utf-8").read()
    m = re.search(rf"\b{naam} = (\d+)", src)
    return int(m.group(1)) if m else None


def selfcheck(h):
    problems = modellen.check(h)
    # the geometry Java uses is the geometry of the templates
    for bestand, plekken, yaw_py in (("Vuurtoren.java", bouw.PLEKKEN_TOREN, bouw.NPC_YAW_TOREN), ("Pepertuin.java", bouw.PLEKKEN_TUIN, bouw.NPC_YAW_TUIN)):
        java, g, yaw = java_plekken(bestand)
        for naam, wat in plekken.items():
            py = [tuple(wat)] if isinstance(wat, tuple) else [tuple(p) for p in wat]
            if java.get(naam) != py:
                problems.append(f"{bestand} {naam} = {java.get(naam)}, the template has {py}")
        if g != bouw.G:
            problems.append(f"{bestand} G = {g}, the template has {bouw.G}")
        if yaw != yaw_py:
            problems.append(f"{bestand} NPC_YAW = {yaw}, the template has {yaw_py}")
    # the numbers the texts name
    if java_getal("Vuurtoren.java", "GRUIS_NODIG") != 4 or java_getal("Vuurtoren.java", "ROOKGUHS") != 3:
        problems.append("the texts say four gloeikoolgruis and three Rookguhs: Vuurtoren.java says otherwise")
    if java_getal("PeperplantBlock.java", "GROEI_BUITEN") != 3 * java_getal("PeperplantBlock.java", "GROEI_KAS"):
        problems.append('the texts say "three times as fast under glass": PeperplantBlock.java says otherwise')
    for key in list(LANG) + [f"subtitles.guhs.{e}" for e in SOUNDS] + ["structure.guhs.rookguh_vuurtoren", "structure.guhs.pepertuin",
                                                                     "entity.guhs.guh_npc.torenwachterguh", "entity.guhs.guh_npc.pepertelerguh",
                                                                     "gui.guhs.verhalen.vuurtoren.naam", "gui.guhs.verhalen.pepertuin.naam"]:
        if key not in h.NL:
            problems.append(f"missing lang {key}")
    for key, text in LANG.items():
        if "hamster" in text.lower():
            problems.append(f"lore: {key}")
    # every text key the Java sources name exists
    javadir = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "torenpeper")
    for f in os.listdir(javadir):
        if not f.endswith(".java") or f.endswith("GameTests.java"):
            continue
        src = open(os.path.join(javadir, f), encoding="utf-8").read()
        prefixen = dict(re.findall(r'String (\w+) = "((?:quest|gui)\.guhs\.torenpeper\.[\w.]*)";', src))
        for m in re.finditer(r'(?:(\b[A-Z]\w*) \+ )?"((?:(?:quest|gui)\.guhs\.torenpeper\.)?[a-z_][\w.]*)"', src):
            pre, rest = m.groups()
            key = prefixen[pre] + rest if pre in prefixen else rest
            if key.startswith(("quest.guhs.torenpeper.", "gui.guhs.torenpeper.")) and not key.endswith(".") and key not in h.NL:
                problems.append(f"{f} names the text {key}, which does not exist")
    for p in ([f"{h.D}/structure/{n}.nbt" for n in (TOREN, TUIN, "torenpeper_test_kamer")]
              + [f"{h.D}/worldgen/structure/{n}.json" for n in (TOREN, TUIN)]
              + [f"{h.D}/worldgen/structure_set/{n}_gegarandeerd.json" for n in (TOREN, TUIN)]
              + [f"{h.D}/advancement/quest/vuurtoren_stap_5.json", f"{h.D}/advancement/quest/pepertuin_stap_4.json",
                 f"{h.D}/loot_table/blocks/torenpeper_peperplant.json"]
              + [os.path.join(h.TEX, "item", f"torenpeper_{t}.png") for t in ("njegpeper", "vahoegpeper", "snoeppeper", "peperzaadjes", "seinlantaarn",
                                                                             "lampkooltje", "pepervuurdrankje", "peperzoetdrankje")]
              + [os.path.join(h.TEX, "mob_effect", "torenpeper_peperadem.png")]):
        if not os.path.exists(p):
            problems.append(f"missing file {p}")
    for naam in (TOREN, TUIN):
        pool = json.load(open(f"{h.D}/worldgen/template_pool/{naam}/start.json", encoding="utf-8"))
        if any(e["element"].get("ground_level_delta") != bouw.G + 1 for e in pool["elements"]):
            problems.append(f"the start pool of {naam} does not say where the ground is")
    sounds = json.load(open(f"{h.A}/sounds.json", encoding="utf-8"))
    for e in SOUNDS:
        if e not in sounds:
            problems.append(f"sounds.json misses {e}")
    if problems:
        raise SystemExit("toren_peper self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    textures(h)
    modellen_blokken(h)
    data(h)
    structuren(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_barbecuether; nothing locked)
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    # --- the lighthouse
    q("toren_peper_toren", "De Rookguh-vuurtoren",
      "Ergens in de rook staat een rood-witte toren met twee roze guh-oren op zijn dak. Daar woont de &6Torenwachter-guh&r. Zijn lamp is "
      "uit... Je &dSuperkompas&r wijst de weg (tab Barbecue).",
      "guhs:torenpeper_seinlantaarn", [fq.structure(TOREN)], deps=["bbq_aan"], shape="hexagon")
    wereld.ftb_questlijn(fq, "toren_peper", "vuurtoren", [
        ("Ahoi, njeg!", "Praat met de &6Torenwachter-guh&r. Zonder licht vinden de Rookguhs de weg naar huis niet meer.",
         "guhs:torenpeper_vuurtorenlamp"),
        ("Kooltjes voor de lamp", "Breng hem vier &6gloeikoolgruis&r (hak gloeikool stuk). Hij perst er een &6lampkooltje&r van, en je mag "
                                  "zijn &6seinlantaarn&r lenen.", "guhs:gloeikoolgruis"),
        ("Floep!", "Klim de wenteltrap op, helemaal tot in het glazen lampenhuis, en leg het lampkooltje in de lamp. TOEEET!",
         "guhs:torenpeper_lampkooltje"),
        ("Drie verdwaalde Rookguhs", "Rond de toren zweven drie magere &6verdwaalde Rookguhs&r, speciaal van jou (met een sterretje erboven). "
                                     "Loop ernaartoe met de seinlantaarn in je poot: ze zweven achter je aan tot ze het licht zien. "
                                     "Zes kaasknabbels voeren mag ook.", "guhs:rookguh_spawn_egg"),
        ("Lampaansteker", "Ga terug naar de Torenwachter-guh. Je krijgt een &6Bezorgguhtje-fluitje&r en een &6wachtersjas&r voor je guh.",
         "guhs:bezorgguhtje_fluitje"),
    ], na=["toren_peper_toren"], eind=(("guhs:kaas_knabbels", 12),))
    q("toren_peper_fluitje", "Fiet-fiew!",
      "Het &6Bezorgguhtje-fluitje&r roept het Bezorgguhtje van je eigen Stepstation naar je toe. Sluipen en fluiten stuurt ze allemaal "
      "naar huis. Kwijt? De Torenwachter-guh snijdt elke dag een nieuwe.", "guhs:bezorgguhtje_fluitje", [item("guhs:bezorgguhtje_fluitje")],
      rewards=(("guhs:kaas_knabbels", 6),), deps=["toren_peper_vuurtoren_5"])
    q("toren_peper_jas", "Wachtersjas",
      "Een donkerblauwe jas met gouden knopen en een rood-witte kraag. Houd hem vast (rechtsklik ingedrukt) om hem te ontgrendelen en "
      "trek hem je guh aan in de kledingkast!", "guhs:torenpeper_wachtersjas", [item("guhs:torenpeper_wachtersjas")],
      rewards=(("guhs:kaas_knabbels", 6),), deps=["toren_peper_vuurtoren_5"])
    q("toren_peper_lamp", "Een lamp voor thuis",
      "Maak je eigen &6vuurtorenlamp&r (roosterijzer tralies, glas en gloeikool). Hij floept aan zodra er iemand in de buurt is die de "
      "echte vuurtoren heeft aangestoken. Jij dus.", "guhs:torenpeper_vuurtorenlamp", [item("guhs:torenpeper_vuurtorenlamp")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["toren_peper_vuurtoren_5"])
    # --- the pepper garden
    q("toren_peper_tuin", "De Pepertuin",
      "Een glazen kas midden in de Guhbarbecuether, met een tuin vol pepers ernaast en een guh-vogelverschrikker. Daar woont de "
      "&6Peperteler-guh&r. Je &dSuperkompas&r wijst de weg (tab Barbecue).",
      "guhs:torenpeper_vahoegpeper", [fq.structure(TUIN)], deps=["bbq_aan"], shape="hexagon")
    wereld.ftb_questlijn(fq, "toren_peper", "pepertuin", [
        ("Eén plantje, drie pepers", "Praat met de &6Peperteler-guh&r. Hij geeft je drie &6peperzaadjes&r.", "guhs:torenpeper_peperzaadjes"),
        ("Groen, rood en roze", "In de kas staan drie &6kweekbakken&r. Plant in elke bak een zaadje, wacht tot je plantje rijp is en pluk "
                                "het. Jouw plantjes zijn alleen van jou.", "guhs:torenpeper_kweekbak"),
        ("Vuur in je buik", "Brouw je eerste &6peperdrankje&r in de Guhbrouwketel achter in de kas: poeder, kaassaus, een rode of roze "
                            "peper erdoor en een flesje vullen. De Peperteler-guh geeft je alles wat je nodig hebt.", "guhs:guhbrouwketel"),
        ("Peperbrouwer", "Laat de Peperteler-guh proeven. Je krijgt &6zaadjes&r voor thuis, het &6andere drankje&r en een &6peperslinger&r "
                         "voor je guh.", "guhs:torenpeper_peperslinger"),
    ], na=["toren_peper_tuin"], eind=(("guhs:kaas_knabbels", 12),))
    q("toren_peper_pluk", "Pluktuin",
      "Een rijpe peperplant pluk je met een &6rechtsklik&r: de plant blijft staan en groeit gewoon door. Probeer het in de kijkbedden "
      "van de kas.", "guhs:torenpeper_njegpeper", [adv("toren_peper_geplukt")], deps=["toren_peper_tuin"])
    q("toren_peper_thuis", "Je eigen pepertuin",
      "Plant peperzaadjes op &6as-aarde&r (groene Njegpeper), &6gloeikool&r (rode Vahoegpeper) en &6pindasaus-nylium&r (roze "
      "Snoeppeper). Zet er &bglas&r boven: dan groeien ze drie keer zo snel. Zaadjes krijg je als je een rijpe plant uitgraaft.",
      "guhs:torenpeper_peperzaadjes", [item("guhs:torenpeper_njegpeper", 4), item("guhs:torenpeper_vahoegpeper", 4), item("guhs:torenpeper_snoeppeper", 4)],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["toren_peper_pepertuin_4"], xp=50)
    q("toren_peper_pepervuur", "Pepervuurdrankje",
      "Kaasbouillon met een &cVahoegpeper&r. Drie minuten lang hak en graaf je als een vuurtje, met vlammetjes uit je snoet "
      "(&6Peperadem&r: je hebt het nooit meer koud).", "guhs:torenpeper_pepervuurdrankje", [item("guhs:torenpeper_pepervuurdrankje")],
      deps=["toren_peper_pepertuin_4"])
    q("toren_peper_peperzoet", "Peperzoetdrankje",
      "Kaasbouillon met een &dSnoeppeper&r. Een warm, zoet gevoel: je geneest een beetje en krijgt twee hartjes erbij.",
      "guhs:torenpeper_peperzoetdrankje", [item("guhs:torenpeper_peperzoetdrankje")], deps=["toren_peper_pepertuin_4"])
    q("toren_peper_heet", "Heet! Heet! Heet!",
      "Eet een &crauwe Vahoegpeper&r. Je rent er tien seconden vahoeg van, met rook uit je oren. Niemand zei dat het slim was, njeg.",
      "guhs:torenpeper_vahoegpeper", [adv("toren_peper_heet")], rewards=(("guhs:kaas_saus_bucket", 1),), deps=["toren_peper_pluk"], shape="octagon")
    q("toren_peper_slinger", "Peperslinger",
      "Een slinger van rode, groene en roze pepers. Houd hem vast (rechtsklik ingedrukt) om hem te ontgrendelen en hang hem je guh om "
      "in de kledingkast!", "guhs:torenpeper_peperslinger", [item("guhs:torenpeper_peperslinger")], rewards=(("guhs:kaas_knabbels", 6),),
      deps=["toren_peper_pepertuin_4"])
