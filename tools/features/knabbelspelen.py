"""
De Knabbelspelen (2.9, De Grote Guhspelen; Java: nl.juiced.guhs.feature.knabbelspelen).

  - the building knabbelspelen (the circus tent with guh-ear tops and a guh face, six play fields): knabbelspelen_bouw.py
  - the pixel art and Juf Vahoegsakee's look: knabbelspelen_tex.py
  - here: block models (the Mika tin with its half-block shifts, the kaasmelk bottle), items, sounds, tags, the sports
    outfit (BONES / clothes / icons / CLOTHES), advancements (tab De Grote Guhspelen), lang (Dutch in both files),
    FTB quests (section "knabbelspelen") and a self-check of the assets.
"""
import numpy as np

from features import doolhof
from features import knabbelspelen_bouw as bouw
from features import knabbelspelen_tex as tex
from features import sterrenwacht_hulp as hulp

NAME = bouw.NAME
BLOKKEN = ["knabbelspelen_blik", "knabbelspelen_kaasmelkfles"]
ITEMS = ["spelenlintje", "guh_zak", "knabbelei_lepel", "knabbelspijker", "guhguhtje_staartje", "blik_pluisbal"]
GELEEND = ["guh_zak", "knabbelei_lepel", "knabbelspijker", "guhguhtje_staartje", "blik_pluisbal"]
ONDERDELEN = ["knabbelhappen", "zaklopen", "blikgooien", "eierlopen", "spijkerpoepen", "guhguhtje_prik"]

# ======================================================================================================================
# the sports outfit (Juf Vahoegsakee's shop)
# ======================================================================================================================
_H = [0, 6, -2]
BONES = {
    # a sweatband round the head, just under the ears' tips
    "outfit_spelen_zweetband": ("head", _H, "spelen_zweetband", [([-7.3, 11.4, -12.35], [14.6, 1.4, 0.4], 0), ([-7.3, 11.4, -0.05], [14.6, 1.4, 0.4], 0),
                                                                ([6.95, 11.4, -12.35], [0.4, 1.4, 12.7], 0), ([-7.35, 11.4, -12.35], [0.4, 1.4, 12.7], 0)]),
    # a whistle on a red cord
    "outfit_spelen_fluitje": ("head", _H, "spelen_koord", [([-4.5, 1.6, -11.5], [9, 0.5, 0.5], 0), ([-4.5, 0.6, -11.5], [0.5, 1, 0.5], 0),
                                                         ([4, 0.6, -11.5], [0.5, 1, 0.5], 0), ([-0.25, -0.6, -11.6], [0.5, 1.4, 0.5], 0)]),
    "outfit_spelen_fluitje_fluit": ("head", _H, "spelen_fluit", [([-1.0, -1.8, -12.6], [2.0, 1.3, 2.4], 0)]),
}
CLOTHES = ["spelen_zweetbandje", "spelen_sportshirtje", "spelen_fluitje"]


def clothes(rng, v):
    def band():
        a = v.fabric((230, 50, 70), rng, 5)
        for x in range(0, 32, 8):
            a[:, x:x + 4] = (250, 250, 250)
        return a

    def shirt():
        a = v.fabric((90, 170, 230), rng, 6)
        a[:, 4:7] = (250, 250, 250)                               # white side stripes
        a[:, 25:28] = (250, 250, 250)
        a[10:18, 13:19] = (246, 200, 70)                          # a little medal
        a[12:16, 15:17] = (236, 104, 164)
        return a

    return {
        "spelen_zweetbandje": {"spelen_zweetband": band},
        "spelen_sportshirtje": {"suit": shirt},
        "spelen_fluitje": {"spelen_koord": lambda: v.fabric((230, 50, 70), rng, 6), "spelen_fluit": lambda: v.fabric((214, 216, 226), rng, 5)},
    }


def icons(ic):
    band = ["................", "..aaaaaaaaaaaa..", ".abcbbcbbcbbcba.", ".abcbbcbbcbbcba.", "..aaaaaaaaaaaa.."]
    shirt = ["...aaa....aaa...", "..abcbaaaabcba..", ".abcbbbbbbbbcba.", ".abcbbbbbbbbcba.", ".aacbbbddbbbcaa.", "..acbbbddbbbca..",
             "...cbbbbbbbbc...", "...cbbbbbbbbc...", "...abbbbbbbba...", "...aaaaaaaaaa..."]
    fluit = ["a..........a....", ".a........a.....", "..a......a......", "...a....a.......", "....a..a........", ".....aa.........",
             "....bbbbb.......", "...bcccccb......", "...bcccccbb.....", "....bbbbb.......".replace(".", ".")]
    return {
        "spelen_zweetbandje": ic.icon(ic.pad(band), {"a": (140, 30, 40), "b": (230, 50, 70), "c": (250, 250, 250)}),
        "spelen_sportshirtje": ic.icon(ic.pad(shirt), {"a": (40, 90, 140), "b": (90, 170, 230), "c": (250, 250, 250), "d": (246, 200, 70)}),
        "spelen_fluitje": ic.icon(ic.pad(fluit), {"a": (230, 50, 70), "b": (110, 110, 120), "c": (214, 216, 226)}),
    }


# ======================================================================================================================
# blocks, items, sounds, tags
# ======================================================================================================================
SOUNDS = {
    "knabbelspelen.fluit": ("minecraft:block.note_block.flute", 2.0, 1.0),
    "knabbelspelen.juich": ("guhs:entity.guh.happy", 1.2, 1.0),
    "knabbelspelen.hap": ("guhs:entity.guh.eat", 1.2, 1.0),
    "knabbelspelen.hop": ("minecraft:block.wool.step", 0.7, 1.0),
    "knabbelspelen.blik": ("minecraft:block.chain.break", 1.4, 1.0),
    "knabbelspelen.ei_kapot": ("minecraft:entity.turtle.egg_crack", 1.2, 1.0),
    "knabbelspelen.plonk": ("minecraft:block.amethyst_block.chime", 0.8, 1.0),
}


def blocks_and_items(h):
    A = h.A
    # the Mika tin: 10 x 14 pixels; "rechts"/"links": shifted half a block to its own right / left
    for schuif, dx in (("geen", 0), ("rechts", 8), ("links", -8)):
        h.w(f"{A}/models/block/knabbelspelen_blik_{schuif}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": "guhs:block/knabbelspelen_blik_zij", "voor": "guhs:block/knabbelspelen_blik_voor",
                         "zij": "guhs:block/knabbelspelen_blik_zij", "boven": "guhs:block/knabbelspelen_blik_boven"},
            "elements": [{"from": [3 + dx, 0, 3], "to": [13 + dx, 14, 13], "faces": {
                "north": {"uv": [3, 1, 13, 15], "texture": "#voor"}, "south": {"uv": [3, 1, 13, 15], "texture": "#zij"},
                "east": {"uv": [3, 1, 13, 15], "texture": "#zij"}, "west": {"uv": [3, 1, 13, 15], "texture": "#zij"},
                "up": {"uv": [3, 3, 13, 13], "texture": "#boven"}, "down": {"uv": [3, 3, 13, 13], "texture": "#boven"}}}]})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    h.w(f"{A}/blockstates/knabbelspelen_blik.json", {"variants": {
        f"facing={f},schuif={s}": ({"model": f"guhs:block/knabbelspelen_blik_{s}", "y": r} if r else {"model": f"guhs:block/knabbelspelen_blik_{s}"})
        for f, r in rot.items() for s in ("geen", "rechts", "links")}})
    h.w(f"{A}/models/item/knabbelspelen_blik.json", {"parent": "guhs:block/knabbelspelen_blik_geen"})
    # the kaasmelk bottle (full: the spijker sticks out of its neck)
    fles = [hulp.el([2, 0, 2], [14, 10, 14], "#fles", uv=[2, 6, 14, 16]), hulp.el([5, 10, 5], [11, 14, 11], "#hals", uv=[5, 0, 11, 4])]
    vol = fles + [hulp.el([6, 14, 6], [10, 15, 10], "#kop", uv=[0, 0, 4, 1]), hulp.el([7.5, 12, 7.5], [8.5, 14.5, 8.5], "#kop", uv=[6, 6, 7, 8])]
    for naam, elems in (("knabbelspelen_kaasmelkfles", fles), ("knabbelspelen_kaasmelkfles_vol", vol)):
        h.w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                              "textures": {"particle": "guhs:block/knabbelspelen_kaasmelkfles", "fles": "guhs:block/knabbelspelen_kaasmelkfles",
                                                           "hals": "guhs:block/knabbelspelen_kaasmelkfles_hals", "kop": "guhs:block/knabbelspelen_spijkerkop"},
                                              "elements": elems})
    h.w(f"{A}/blockstates/knabbelspelen_kaasmelkfles.json", {"variants": {
        "vol=false": {"model": "guhs:block/knabbelspelen_kaasmelkfles"}, "vol=true": {"model": "guhs:block/knabbelspelen_kaasmelkfles_vol"}}})
    h.w(f"{A}/models/item/knabbelspelen_kaasmelkfles.json", {"parent": "guhs:block/knabbelspelen_kaasmelkfles"})
    # the invisible anchor
    h.w(f"{A}/models/block/knabbelspelen_anker.json", {"textures": {"particle": "minecraft:block/pink_stained_glass"}})
    h.w(f"{A}/blockstates/knabbelspelen_anker.json", {"variants": {f"facing={f}": {"model": "guhs:block/knabbelspelen_anker"}
                                                                  for f in ("north", "east", "south", "west")}})
    for i in ITEMS:
        h.item_model(i)
    for b in BLOKKEN:
        h.self_drop(b)
    h.shaped("knabbelspelen_blik", ["I", "P", "I"], {"I": "minecraft:iron_nugget", "P": "minecraft:purple_dye"}, "guhs:knabbelspelen_blik", 3)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:knabbelspelen_blik", "guhs:knabbelspelen_kaasmelkfles"])
    h.add_tag("guhs/tags/item/loaned", [f"guhs:{i}" for i in GELEEND])
    doolhof.geluiden(h, SOUNDS)


# ======================================================================================================================
# advancements
# ======================================================================================================================
QUEST = ["knabbelspelen_gevonden", "knabbelspelen_gespeeld", "knabbelspelen_zeskamp", "knabbelspelen_zeskamp_5000", "knabbelspelen_raak",
         "knabbelspelen_samen", "knabbelspelen_winnaar", "seen_spelleiderguh"]
TAB = [
    ("knabbelspelen_gevonden", "root", "guhs:spelenlintje", "task", "Hooggeëerd publiek!",
     "Vind De Knabbelspelen: de grote circustent met de guhoren (Guhweides, Roze pluisjes)"),
    ("knabbelspelen_gespeeld", "knabbelspelen_gevonden", "guhs:guh_zak", "task", "Op je plaatsen...",
     "Doe mee aan een onderdeel van De Knabbelspelen"),
    ("knabbelspelen_samen", "knabbelspelen_gespeeld", "guhs:blik_pluisbal", "task", "Samen VAHOEG",
     "Speel De Knabbelspelen met minstens één vriend tegelijk"),
    ("knabbelspelen_raak", "knabbelspelen_gespeeld", "guhs:guhguhtje_staartje", "goal", "Precies op z'n kontje!",
     "Prik het staartje blind (bijna) precies op de goede plek: 950 punten of meer"),
    ("knabbelspelen_zeskamp", "knabbelspelen_gespeeld", "guhs:knabbelspelen_blik", "goal", "De Grote Zeskamp",
     "Speel alle zes onderdelen achter elkaar in de Grote Zeskamp"),
    ("knabbelspelen_winnaar", "knabbelspelen_zeskamp", "guhs:spelenlintje", "goal", "Kampioen van de tent",
     "Win de Grote Zeskamp tegen minstens één vriend"),
    ("knabbelspelen_zeskamp_5000", "knabbelspelen_zeskamp", "guhs:knabbelspelen_kaasmelkfles", "challenge", "Vahoegste van de Zeskamp",
     "Haal 5000 punten of meer in de Grote Zeskamp"),
    ("knabbelspelen_kleding", "knabbelspelen_gespeeld", "guhs:spelen_zweetbandje", "task", "Sportief gekleed",
     "Koop het hele sportpakje bij Juf Vahoegsakee: zweetbandje, fluitje en sportshirtje"),
]


# ======================================================================================================================
# lang (Dutch in both files)
# ======================================================================================================================
NAMEN = {"knabbelhappen": "Knabbelhappen", "zaklopen": "Zaklopen", "blikgooien": "Mika-blikgooien", "eierlopen": "Eierlopen met knabbelei",
         "spijkerpoepen": "Spijkerpoepen", "guhguhtje_prik": "Guhguhtje prik"}
KORT = {"knabbelhappen": "Meeste knabbels", "zaklopen": "Snelste hoppers", "blikgooien": "Meeste blikken", "eierlopen": "Snelste eierlopers",
        "spijkerpoepen": "Snelste spijkerpoepers", "guhguhtje_prik": "Beste prikkers"}
UITLEG = {
    "knabbelhappen": "Kaasknabbels zwaaien aan touwtjes naar je toe. Blijf op je matje en hap (klik) ze als ze dichtbij zijn: "
                     "een gewone is 1 punt, een gouden 3. 45 seconden lang smullen: je guh wordt lekker VAHOEG!",
    "zaklopen": "Je staat in een guhzak: lopen gaat bijna niet! Spring (spatie) om te hoppen, en kijk waar je heen wilt. "
                "Over de pluisbulten naar de finish. Hoe sneller, hoe beter.",
    "blikgooien": "Op de tafel staan zes blikken met Mika-koppies (3-2-1). Gooi je 8 pluisballen (rechtsklik): elk blik dat valt is 10 punten, "
                  "alles om is 20 extra en een nieuwe stapel!",
    "eierlopen": "Een knabbelei op een lepel, langs de vier vlaggetjes naar de finish. Rustig lopen! Rennen, scherpe bochten en springen "
                 "laten het ei wiebelen. Valt het, dan terug naar het laatste vlaggetje.",
    "spijkerpoepen": "Een knabbelspijker bungelt aan een touwtje achter je guhriem en zwiept mee als je beweegt. Ga met je achterste "
                     "boven een kaasmelkfles staan, hou je stil en buk (sluipen) om hem erin te laten zakken. Plonk! Drie flessen.",
    "guhguhtje_prik": "Blinddoek om, drie keer rond... en dan zoeken! Loop naar het guhbord en prik het staartje (rechtsklik) waar "
                      "het hoort: op zijn kontje, bij het roze vlekje. Hoe dichterbij, hoe meer punten (max 1000).",
}
LANG = {
    # names
    "entity.guhs.guh_npc.spelleiderguh": "Juf Vahoegsakee",
    "gui.guhs.guhdex.rarity.spelleiderguh": "Zeldzaamheid: Uniek (De Knabbelspelen, Guhweides en Roze pluisjes)",
    "gui.guhs.guhdex.info.spelleiderguh": "Juf Vahoegsakee fluit de Grote Zeskamp. Ze heeft altijd haar klembord bij zich en weet precies wie er "
                                          "het snelst hopt, het verst gooit en het mooist prikt. De Mika's hebben de kaasknabbels gepikt, dus: "
                                          "zes spellen, duizend punten per spel, en wie wordt de vahoegste guh?",
    "entity.guhs.knabbelspelen_ding": "Spelding",
    "block.guhs.knabbelspelen_blik": "Mika-blik",
    "block.guhs.knabbelspelen_kaasmelkfles": "Kaasmelkfles",
    "block.guhs.knabbelspelen_anker": "Knabbelspelen-anker",
    "item.guhs.spelenlintje": "Spelenlintje",
    "item.guhs.guh_zak": "Guhzak (geleend)",
    "item.guhs.guh_zak.lore": "Voor het zaklopen: spring om te hoppen!",
    "item.guhs.knabbelei_lepel": "Lepel met knabbelei (geleend)",
    "item.guhs.knabbelei_lepel.lore": "Rustig lopen, anders valt hij! Juf Vahoegsakee wil haar lepel terug.",
    "item.guhs.knabbelspijker": "Knabbelspijker (geleend)",
    "item.guhs.knabbelspijker.lore": "Hij bungelt aan je guhriem. Plonk hem in de kaasmelkfles!",
    "item.guhs.guhguhtje_staartje": "Guhguhtje-staartje (geleend)",
    "item.guhs.blik_pluisbal": "Pluisbal (geleend)",
    "item.guhs.spelen_zweetbandje": "Sportzweetbandje",
    "item.guhs.spelen_sportshirtje": "Knabbelspelen-sportshirtje",
    "item.guhs.spelen_fluitje": "Scheidsrechtersfluitje",
    "structure.guhs.knabbelspelen": "De Knabbelspelen",
    "structure.guhs.knabbelspelen.tooltip": "Een grote circustent met zes speelvelden eromheen: de Grote Zeskamp (Guhweides, Roze pluisjes)",
    "subtitles.guhs.knabbelspelen.fluit": "Juf Vahoegsakee fluit",
    "subtitles.guhs.knabbelspelen.juich": "Guhs juichen",
    "subtitles.guhs.knabbelspelen.hap": "Hap!",
    "subtitles.guhs.knabbelspelen.hop": "Hop in de zak",
    "subtitles.guhs.knabbelspelen.blik": "Blikken vallen om",
    "subtitles.guhs.knabbelspelen.ei_kapot": "Knabbelei valt",
    "subtitles.guhs.knabbelspelen.plonk": "Plonk in de fles",
    "gui.guhs.scorebord.knabbelspelen": "De Grote Zeskamp",
    # the screen
    "gui.guhs.knabbelspelen.zeskamp": "De Grote Zeskamp",
    "gui.guhs.knabbelspelen.zeskamp.tooltip": "Alle zes onderdelen achter elkaar! Per onderdeel 0 tot 1000 punten, samen hooguit 6000. Jouw record: %s",
    "gui.guhs.knabbelspelen.rules": "Hooggeëerd publiek! Zes spellen rond mijn tent: kies er eentje, of doe de Grote Zeskamp. "
                                    "Vriendjes mogen meedoen (tot vier tegelijk, ieder in een eigen baan): wie het spel opent, wacht even op ze. "
                                    "Elk onderdeel geeft 0 tot 1000 zeskamppunten.",
    "gui.guhs.knabbelspelen.mee": "Doe mee!",
    "gui.guhs.knabbelspelen.nu": "Start nu!",
    "gui.guhs.knabbelspelen.nu.tooltip": "Niet meer wachten op vriendjes: we beginnen!",
    "gui.guhs.knabbelspelen.tabel": "Scoretabel",
    "gui.guhs.knabbelspelen.tabel.dicht": "Terug",
    "gui.guhs.knabbelspelen.tabel.uitleg": "Zo tellen de zeskamppunten (0 - 1000 per onderdeel):\n"
                                           "- Knabbelhappen: 30 punten = 1000 (een gouden knabbel telt 3).\n"
                                           "- Zaklopen: finish in 7 seconden of sneller = 1000; twee keer zo lang = 500.\n"
                                           "- Mika-blikgooien: 120 punten = 1000 (10 per blik, 20 extra voor alles om).\n"
                                           "- Eierlopen: finish in 11 seconden of sneller = 1000; twee keer zo lang = 500.\n"
                                           "- Spijkerpoepen: drie flessen in 20 seconden of sneller = 1000; twee keer zo lang = 500.\n"
                                           "- Guhguhtje prik: je prikscore zelf (1000 precies raak, 0 van 2,5 blok ver).\n"
                                           "Niet gehaald = 0. De Grote Zeskamp: alles bij elkaar, hooguit 6000!",
    "gui.guhs.knabbelspelen.shop": "Winkeltje",
    "gui.guhs.knabbelspelen.shop.tooltip": "Het sportpakje en spelletjesspullen voor spelenlintjes",
    "gui.guhs.knabbelspelen.kapot": "Njeg... mijn speelvelden zijn zoek. Hier kan ik geen spelen houden.",
    "gui.guhs.knabbelspelen.inschrijven": "%s begint zo! Mee: %s. Nog %s seconden om mee te doen.",
    "gui.guhs.knabbelspelen.bezig": "Nu bezig: %s (%s). Kijk maar mee!",
    "gui.guhs.knabbelspelen.free": "De velden zijn vrij. Wat wordt het?",
    "gui.guhs.knabbelspelen.nog_geen_record": "Nog geen record.",
    "gui.guhs.knabbelspelen.jouw_record": "Jouw record: %s",
    "gui.guhs.knabbelspelen.blinddoek": "Blinddoek om! Zoek het guhbord en prik (rechtsklik)...",
    "gui.guhs.knabbelspelen.no_build": "Dit zijn de velden van Juf Vahoegsakee: niet verbouwen, sportief blijven!",
    # Juf Vahoegsakee talks
    "quest.guhs.knabbelspelen.hello": "Hallo, sportieveling! Ik ben Juf Vahoegsakee. De Mika's hebben de kaasknabbels gepikt, en nu zijn de guhs "
                                      "niet VAHOEG genoeg... Met een potje zeskamp krijgen we ze weer vrolijk. Doe je mee?",
    "quest.guhs.knabbelspelen.hello_again": "Daar is mijn sporter weer! Welk onderdeel doen we?",
    "quest.guhs.knabbelspelen.wacht": "Je doet mee met %s! Even wachten op de anderen... of druk op Start nu!",
    "quest.guhs.knabbelspelen.doe_mee": "%s begint zo! Druk op Doe mee! om erbij te zijn.",
    "quest.guhs.knabbelspelen.bezig": "Sssst, er wordt gespeeld (%s)! Straks mag jij.",
    "quest.guhs.knabbelspelen.shop": "Mijn winkeltje! Met het sportpakje ben je klaar voor elke zeskamp.",
    "quest.guhs.knabbelspelen.broken": "Njeg... ik kan mijn speelvelden niet vinden.",
    "quest.guhs.knabbelspelen.open": "%s! Wie doet er mee? Over %s seconden fluit ik!",
    "quest.guhs.knabbelspelen.te_laat": "Te laat, het is al begonnen! Kijk maar mee, de volgende keer ben je erbij.",
    "quest.guhs.knabbelspelen.vol": "Alle vier de banen zijn vol! De volgende keer, vads.",
    "quest.guhs.knabbelspelen.erbij": "%s doet mee! (%s/%s)",
    "quest.guhs.knabbelspelen.uitnodiging": "%s begint zo (nog %s s)! Praat met mij om mee te doen!",
    "quest.guhs.knabbelspelen.weggelopen": "Je liep weg van de spelen. Juf Vahoegsakee fluit je af, njeg.",
    "quest.guhs.knabbelspelen.gestopt": "De spelen zijn voor jou gestopt. Alle geleende spulletjes gaan terug naar de juf.",
    "quest.guhs.knabbelspelen.title.go": "PRRRT!",
    "quest.guhs.knabbelspelen.title.klaar": "KLAAR!",
    "quest.guhs.knabbelspelen.title.tijd_op": "TIJD OP!",
    "quest.guhs.knabbelspelen.niet_gehaald": "Niet gehaald... 0 zeskamppunten, maar wel lekker gesport, vads!",
    "quest.guhs.knabbelspelen.jouw_uitslag": "%s: %s (%s zeskamppunten)",
    "quest.guhs.knabbelspelen.uitslag": "Uitslag %s:",
    "quest.guhs.knabbelspelen.dnf": "niet gehaald",
    "quest.guhs.knabbelspelen.punten": "%s punten",
    "quest.guhs.knabbelspelen.record": "Nieuw record bij %s: %s!",
    "quest.guhs.knabbelspelen.volgende": "Op naar het volgende onderdeel: %s (%s/%s)!",
    "quest.guhs.knabbelspelen.title.zeskamp": "ZESKAMP KLAAR!",
    "quest.guhs.knabbelspelen.title.winnaar": "KAMPIOEN!",
    "quest.guhs.knabbelspelen.title.totaal": "%s zeskamppunten",
    "quest.guhs.knabbelspelen.end.super": "%s punten! Wat een kampioen, de hele tent juicht: VAHOEG!",
    "quest.guhs.knabbelspelen.end.good": "%s punten, goed gedaan! Je guh is lekker VAHOEG.",
    "quest.guhs.knabbelspelen.end.ok": "%s punten! Meedoen is belangrijker dan winnen, vads. Nog een keer?",
    "quest.guhs.knabbelspelen.end.super1": "Super gedaan bij %s! Daar krijg je extra lintjes voor.",
    "quest.guhs.knabbelspelen.end.ok1": "Lekker gesport bij %s! Probeer ook eens de Grote Zeskamp.",
    "quest.guhs.knabbelspelen.lintjes": "Je krijgt %s spelenlintjes!",
    # the events
    "quest.guhs.knabbelspelen.hap.bar": "%s punten · nog %s s",
    "quest.guhs.knabbelspelen.hap.mat": "Blijf op je matje! De knabbels komen naar jou toe.",
    "quest.guhs.knabbelspelen.hap.te_ver": "Te ver weg... wacht tot hij terugzwaait!",
    "quest.guhs.knabbelspelen.hap.goud": "Een GOUDEN knabbel! +3 (%s punten)",
    "quest.guhs.knabbelspelen.zak.bar": "Tijd %s · nog %s blokjes hoppen",
    "quest.guhs.knabbelspelen.blik.bar": "%s punten · %s pluisballen over",
    "quest.guhs.knabbelspelen.blik.streep": "Achter de streep blijven, vads!",
    "quest.guhs.knabbelspelen.blik.nog_niet": "Nog even wachten op het fluitje!",
    "quest.guhs.knabbelspelen.blik.alles": "ALLES OM! +%s",
    "quest.guhs.knabbelspelen.blik.raak": "Raak! %s blik(ken) om (%s punten)",
    "quest.guhs.knabbelspelen.ei.vlag": "Vlaggetje %s/%s!",
    "quest.guhs.knabbelspelen.ei.gemist": "Je miste vlaggetje %s! Terug, anders telt de finish niet.",
    "quest.guhs.knabbelspelen.ei.bar": "Tijd %s · wiebel %s · vlaggetjes %s/%s",
    "quest.guhs.knabbelspelen.ei.valt": "Oei, je knabbelei valt! Terug naar het laatste vlaggetje, met een nieuw ei.",
    "quest.guhs.knabbelspelen.spijker.bar": "Tijd %s · fles %s/%s · %s",
    "quest.guhs.knabbelspelen.spijker.plonk": "PLONK! Fles %s/%s is raak!",
    "quest.guhs.knabbelspelen.prik.zoek": "Waar is dat guhbord nou... voel maar!",
    "quest.guhs.knabbelspelen.prik.bar": "Blind zoeken... nog %s s",
    "quest.guhs.knabbelspelen.prik.geen_bord": "Dat is niet het guhbord... voel verder!",
    "quest.guhs.knabbelspelen.prik.raak": "PRECIES RAAK! (%s blokjes naast) %s punten!",
    "quest.guhs.knabbelspelen.prik.naast": "Geprikt! %s blokjes naast zijn kontje: %s punten.",
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for o in ONDERDELEN:
        h.lang(f"gui.guhs.knabbelspelen.onderdeel.{o}", NAMEN[o], NAMEN[o])
        h.lang(f"gui.guhs.knabbelspelen.kort.{o}", KORT[o], KORT[o])
        h.lang(f"gui.guhs.knabbelspelen.uitleg.{o}", UITLEG[o], UITLEG[o])
    # (2.9 visual QA: the short name on Juf Vahoegsakee's event button, where the full one doesn't fit)
    h.lang("gui.guhs.knabbelspelen.onderdeel.eierlopen.kort", "Eierlopen", "Eierlopen")


def build(h):
    tex.build(h)
    blocks_and_items(h)
    doolhof.advancements(h, NAME, QUEST, TAB, CLOTHES)
    texts(h)
    b = bouw.Bouw(h).bouw()
    problems, walkable = bouw.check(b)
    if problems:
        raise SystemExit("knabbelspelen geometry check failed:\n  " + "\n  ".join(problems[:40]))
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[NAME] = 48
    h.FLATNESS[NAME] = 14
    h.structure(NAME, ["guh_meadows", "pink_puffs"], spacing=44, separation=16, salt=20290401, start_y=-bouw.G, reach=80, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none, "ambient": none})
    b.s.save(NAME)
    doolhof.selfcheck(h, "knabbelspelen", BLOKKEN + ["knabbelspelen_anker"], ITEMS + BLOKKEN, CLOTHES,
                      ["entity/npc_spelleiderguh"] + [f"item/{i}" for i in ITEMS], ["guh_npc_spelleiderguh"], LANG)
    print(f"knabbelspelen: building ok ({len(b.s.blocks)} blocks, {walkable} walkable, {b.gezichten} guh faces)")


# ======================================================================================================================
# FTB quests (section "knabbelspelen")
# ======================================================================================================================
def ftb(fq):
    q, y = fq.q, 0
    q("knabbelspelen_vinden", "Hooggeëerd publiek!", "In de &dGuhweides&r en de &dRoze pluisjes&r staat soms een grote circustent met twee "
      "guhoren als tentpunten: &6De Knabbelspelen&r. Zeg hallo tegen &aJuf Vahoegsakee&r midden in de piste.",
      "guhs:spelenlintje", [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 8),), x=0, y=y, shape="circle", xp=100)
    q("knabbelspelen_spelen", "Op je plaatsen...", "Kies een onderdeel bij Juf Vahoegsakee en doe mee. Neem vriendjes mee: tot vier "
      "guhs spelen tegelijk, ieder in een eigen baan!", "guhs:guh_zak", [fq.adv("knabbelspelen_gespeeld")],
      rewards=(("guhs:spelenlintje", 2),), deps=["knabbelspelen_vinden"], x=1.5, y=y, xp=100)
    q("knabbelspelen_prik", "Precies op z'n kontje!", "Guhguhtje prik: blinddoek om, drie keer rond, en dan het staartje op de goede plek. "
      "Haal 950 punten of meer!", "guhs:guhguhtje_staartje", [fq.adv("knabbelspelen_raak")], rewards=(("guhs:spelenlintje", 3),),
      deps=["knabbelspelen_spelen"], x=3, y=y + 1.5, xp=200)
    q("knabbelspelen_samen", "Samen VAHOEG", "Speel De Knabbelspelen met minstens één vriend tegelijk. Wie opent, wacht even op de anderen.",
      "guhs:blik_pluisbal", [fq.adv("knabbelspelen_samen")], rewards=(("guhs:spelenlintje", 2),), deps=["knabbelspelen_spelen"], x=1.5, y=y + 1.5, xp=150)
    q("knabbelspelen_zeskamp", "De Grote Zeskamp", "Alle zes onderdelen achter elkaar: knabbelhappen, zaklopen, Mika-blikgooien, eierlopen, "
      "spijkerpoepen en guhguhtje prik. Elk onderdeel 0 tot 1000 punten!", "guhs:knabbelspelen_blik", [fq.adv("knabbelspelen_zeskamp")],
      rewards=(("guhs:spelenlintje", 4),), deps=["knabbelspelen_spelen"], x=3, y=y, shape="gear", xp=300)
    q("knabbelspelen_5000", "Vahoegste van de Zeskamp", "Haal 5000 punten of meer in de Grote Zeskamp. De scoretabel bij Juf Vahoegsakee "
      "vertelt hoe het telt.", "guhs:knabbelspelen_kaasmelkfles", [fq.adv("knabbelspelen_zeskamp_5000")],
      rewards=(("guhs:vahoege_vads_ingot", 2),), deps=["knabbelspelen_zeskamp"], x=4.5, y=y, shape="hexagon", xp=500)
    q("knabbelspelen_pakje", "Sportief gekleed", "Koop het sportzweetbandje, het scheidsrechtersfluitje en het sportshirtje bij Juf "
      "Vahoegsakee (spelenlintjes) en ontgrendel ze voor je guhs.", "guhs:spelen_zweetbandje",
      [fq.item("guhs:spelen_zweetbandje"), fq.item("guhs:spelen_fluitje"), fq.item("guhs:spelen_sportshirtje")],
      rewards=(("guhs:spelenlintje", 2),), deps=["knabbelspelen_spelen"], x=4.5, y=y + 1.5, xp=150)
