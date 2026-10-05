"""
Guhpixel slice "lobby" (Java: feature/guhpixel/lobby; namespaces lobby, internetcafe; English: tools/lang/en/c31_px_lobby.json).

Makes:
  - the lobby island guhs:guhpixel/lobby (guhpixel_lobby_bouw.py) and data/guhs/guhpixel/lobby_kaart.json: where the golden
    knabbels, the lobby guhs and the parkour's blocks are (Java: LobbyKaart reads it; one source of truth)
  - the structure "internetcafe", the Guh-internetcafé "De Trage Verbinding" (guhpixel_lobby_cafe.py), with its random_spread
    set and its guaranteed copy (ring 4300-5600, sector 0 of 2)
  - blocks: lobby_gouden_knabbel (nummer 0..9, no item), lobby_parkour_start / _tussenpunt / _finish (no items),
    internetcafe_computer (a decoration with an item)
  - the NPC kinds lobby_welkomstguh, lobby_verkoper_guh, lobby_chatguh, internetcafe_beheerder, internetcafe_slaper
  - sounds, hidden quest advancements, the block tag entries for guhpixel_bruikbaar, the test room lobby_test_plein
  - every Dutch text of the slice (signs come from the two builders through tools/sign_text.py), the FTB quests
"""
import os

import numpy as np
from PIL import Image

from features import guhpixel_lib as lib
from features import guhpixel_lobby_bouw as bouw
from features import guhpixel_lobby_cafe as cafe

CAFE = "internetcafe"
CAFE_SALT = 20301001
GEGARANDEERD = (4300, 5600)      # outside the live server's pregenerated square (half-width 3000: its corners reach 4243)
KNABBELS, MUNTJES_KNABBEL, MUNTJES_PARKOUR, TUSSENPUNTEN = 10, 10, 50, 3

CHAT_NAMEN = ["xX_Vadsje_Xx", "Knabbel2009", "NjegMaster", "SlaapKopGuh", "GuhGamer_NL", "KaasKoning77"]
CHAT_REGELS = [
    "gg", "iemand party?", "njeg ez", "lag!!", "wie heeft mijn knabbel", "afk (slaap)",
    "hoe kom ik uit de lobby", "eerste!", "brb knabbel halen", "wie doet er Bedwars? ik verdedig wel (slapend)",
    "admin!!! mijn kussen is weg", "ik zit vast in mijn mandje", "GUH GUH GUH", "noob (lief bedoeld)", "ruilen? ik heb kaas",
    "zzz", "hoe maak je een cobblestone generator", "rood is sus", "iemand een kabeltje over?", "mijn ping is 9999. vahoeg.",
    "wie heeft de parkour onder de minuut? ik lig er nog", "1v1 dutje doen bij de AFK-hoek",
]
VERKOPER_ROEP = [
    "Alleen cosmetisch, echt waar, njeg!", "Nu 0% korting! Op = op! (Het is nooit op.)", "Kijken, kijken, WEL kopen, njeg!",
    "Alles voor muntjes, niks voor niks. Zo werkt een winkel.", "Volgens de EULA mag dit allemaal. Denk ik. Njeg.",
    "Vandaag in de aanbieding: alles. Voor de gewone prijs!", "Niet goed? Muntjes weg! Grapje. Nee, echt.",
    "Je wint er niks mee, maar je huis wordt er wel vadsiger van!",
]
VERKOPER_ONDER = ["Nu 0% korting!", "Alleen cosmetisch, njeg", "UITVERKOOP (altijd)", "Klik om te shoppen"]
WINKEL_GRAPPEN = ["UITVERKOOP! Alles even duur als altijd", "2 halen = 2 betalen", "EULA gelezen? Wij ook niet, njeg",
                  "Geen loot boxes. Wel dozen. Er zit niks in.", "Pay-to-vads bestaat niet: vadsen is gratis", "Op = op (het is nooit op)"]
SLAPER = ["Zzz... njeg... nog vijf minuutjes bufferen...", "Zzz... 3 procent... zzz...", "*snurk* ...wachtwoord... njegnjeg... zzz",
          "Zzz... de pagina laadt... al sinds dinsdag...", "*knor* ...niet de stekker... zzz...", "Zzz... gg... zzz..."]

TEXTS = {
    f"structure.guhs.{CAFE}": "Guh-internetcafé \"De Trage Verbinding\"",
    f"structure.guhs.{CAFE}.tooltip": "Guhs slapen achter oude beige computers. Loop door het grote beeldscherm naar Guhpixel!",
    # --- blocks ---
    "block.guhs.lobby_gouden_knabbel": "Gouden lobbyknabbel",
    "block.guhs.lobby_parkour_start": "Parkour-startplaat",
    "block.guhs.lobby_parkour_tussenpunt": "Parkour-tussenpunt",
    "block.guhs.lobby_parkour_finish": "Parkour-finishplaat",
    # --- the golden knabbels ---
    "gui.guhs.lobby.knabbel.op_slot": "Deze knabbel glimt alleen voor wie door het grote beeldscherm kwam, njeg.",
    "gui.guhs.lobby.knabbel.al": "Die had je al, njeg! (%1$s van de %2$s gevonden)",
    "gui.guhs.lobby.knabbel.gevonden": "Gouden knabbel gevonden! Dat is nummer %1$s van de %2$s. Zoek maar lekker verder, njeg.",
    "gui.guhs.lobby.knabbel.alle.titel": "ALLE KNABBELS!",
    "gui.guhs.lobby.knabbel.alle.onder": "Knabbelspeurder van Guhpixel. Vahoeg!",
    "gui.guhs.lobby.knabbel.alle.chat": "Alle tien de gouden knabbels gevonden! Je hebt nu de titel Knabbelspeurder (zie je Guhdex). VAHOEG!",
    # --- the parkour ---
    "gui.guhs.lobby.parkour.klaar": "Klaar? Stap van de plaat en de tijd loopt, njeg!",
    "gui.guhs.lobby.parkour.vliegen": "Vliegen telt niet, njeg! Gewoon springen. Begin maar opnieuw.",
    "gui.guhs.lobby.parkour.weg": "Je bent van de route af. Geeft niks: begin gewoon opnieuw bij de startplaat.",
    "gui.guhs.lobby.parkour.gevallen": "Plof! Van de route af. Geeft niks: terug naar de startplaat, njeg.",
    "gui.guhs.lobby.parkour.te_lang": "Ben je onderweg in slaap gevallen? Begin maar opnieuw, vadsje.",
    "gui.guhs.lobby.parkour.tussenpunt": "Tussenpunt %1$s van de %2$s!",
    "gui.guhs.lobby.parkour.sluiproute": "Geen sluiproutes, njeg! Je hebt pas %1$s van de %2$s tussenpunten.",
    "gui.guhs.lobby.parkour.loopt": "Lobby-parkour  %1$s  ·  tussenpunt %2$s/%3$s",
    "gui.guhs.lobby.parkour.finish.titel": "FINISH!",
    "gui.guhs.lobby.parkour.finish.record": "%s  ·  nieuw record, vahoeg!",
    "gui.guhs.lobby.parkour.finish.tijd": "%s  ·  netjes, njeg",
    "gui.guhs.lobby.parkour.chat.eerste": "Lobby-parkour gehaald in %1$s! Je eerste keer: +%2$s muntjes. Vahoeg!",
    "gui.guhs.lobby.parkour.chat.record": "Lobby-parkour in %1$s: een nieuw record! (Je oude tijd was %2$s.)",
    "gui.guhs.lobby.parkour.chat.tijd": "Lobby-parkour in %1$s. Je record blijft %2$s, njeg.",
    # --- boards and labels ---
    "gui.guhs.lobby.bord.online.kop": "GUHPIXEL",
    "gui.guhs.lobby.bord.online.spelers": "Spelers online: %1$s (en %2$s guhs)",
    "gui.guhs.lobby.bord.online.waar": "In de lobby: %1$s  ·  in een spel: %2$s",
    "gui.guhs.lobby.bord.online.ping": "Ping: 9999 ms (de server doet een dutje)",
    "gui.guhs.lobby.bord.parkour.kop": "LOBBY-PARKOUR",
    "gui.guhs.lobby.bord.parkour.top": "Snelste daklopers",
    "gui.guhs.lobby.bord.parkour.eigen": "Jouw record: %1$s (%2$s keer gehaald)",
    "gui.guhs.lobby.bord.parkour.eigen_geen": "Stap op de plaat en ren! Eerste keer: %s muntjes",
    "gui.guhs.lobby.bord.uitgang": "TERUG NAAR HUIS",
    "gui.guhs.lobby.bord.reserve": "BINNENKORT, njeg",
    "gui.guhs.lobby.bord.reserve.onder": "0 spelers · 1 slapende bouwguh",
    "gui.guhs.lobby.bord.stats.kop": "JOUW STATS",
    "gui.guhs.lobby.bord.stats.muntjes": "Muntjes: %1$s (ooit verdiend: %2$s)",
    "gui.guhs.lobby.bord.stats.rang_top": "Hoogste rang. Vahoeg!",
    "gui.guhs.lobby.bord.stats.rang_volgende": "Volgende rang: %1$s over %2$s muntjes",
    "gui.guhs.lobby.bord.stats.grappen": "Spellen uitgespeeld: %1$s van de %2$s",
    "gui.guhs.lobby.bord.stats.knabbels": "Gouden knabbels: %1$s van de %2$s",
    "gui.guhs.lobby.bord.stats.parkour": "Lobby-parkour: %s",
    "gui.guhs.lobby.bord.stats.parkour_geen": "Lobby-parkour: nog niet gehaald",
    # --- the Welkomstguh ---
    "gui.guhs.lobby.welkom.kop": "WELKOM!",
    "gui.guhs.lobby.welkom.onder": "Klik voor je Netwerkkabeltje",
    "gui.guhs.lobby.welkom.op_slot": "Hé, hoe ben JIJ hier gekomen? Je moet eerst door het grote beeldscherm in het Guh-internetcafé, njeg. Regels zijn regels!",
    "gui.guhs.lobby.welkom.eerste": "WELKOM OP GUHPIXEL, %s! De grootste en vadsigste minigame-server van de hele Guhmensie. Hier: een "
                                    "Netwerkkabeltje. Daarmee maak je thuis een Guhpixel-poort (het recept staat nu in je receptenboek). Njeg!",
    "gui.guhs.lobby.welkom.terug": "Daar is %s weer! Lekker spelen, vadsje. Typ /lobby om naar huis te gaan, of loop door de voordeur aan de zuidkant.",
    "gui.guhs.lobby.welkom.wat": "Klik op een guh bij een kraampje om te spelen: elk spel geeft de eerste keer 100 muntjes. Verder: een parkour over de "
                                 "daken, tien verstopte gouden knabbels en de winkel. Naar huis? De voordeur in het zuiden, of typ /lobby.",
    "gui.guhs.lobby.welkom.rangen": "Je rang hangt af van alle muntjes die je ooit verdiende: [GUH] vanaf 0, [VADS] vanaf 250, [VADS+] vanaf 750, [MVG] "
                                    "vanaf 1250 en [MVG++] vanaf 1750. Uitgeven mag: je rang blijft. MVG? Meest Vadsige Guh, njeg.",
    "gui.guhs.lobby.welkom.kabel.nieuw": "Kwijt? Njeg njeg njeg. Hier is een nieuwe. Niet weer op knabbelen!",
    "gui.guhs.lobby.welkom.kabel.heb_je": "Je hebt er nog een, vadsje! Kijk eens goed in je zakken. Eén kabeltje per guh, regels zijn regels.",
    "gui.guhs.lobby.welkom.optie.wat": "Wat kan ik hier doen?",
    "gui.guhs.lobby.welkom.optie.rangen": "Hoe werken die rangen?",
    "gui.guhs.lobby.welkom.optie.kabel": "Mijn kabeltje is kwijt, njeg",
    # --- the Verkoper-guh ---
    "gui.guhs.lobby.verkoper.kop": "DE WINKEL",
    # --- the shop screen's dressing (client: feature/guhpixel/client/WinkelScherm) ---
    "gui.guhs.lobby.winkel.korting": "-0%",
    "gui.guhs.lobby.winkel.kleine_lettertjes": "* Alleen cosmetisch. Je wint er niks mee, behalve een vadsiger huis. Ruilen kan niet, njeg.",
    "gui.guhs.lobby.winkel.aantal": "%s dingen",
    "gui.guhs.lobby.winkel.aantal.1": "1 ding",
    "gui.guhs.lobby.winkel.tekort": "Nog %s muntjes tekort. Speel nog een potje, njeg!",
    # --- the Beheerder-guh and the sleepers ---
    "gui.guhs.internetcafe.beheerder.hallo": "Welkom bij De Trage Verbinding. Werkt het niet? Heb je hem al uit en weer aan gezet, njeg? Nee? "
                                             "Doe dat dan eerst. Daarna mag je door het grote beeldscherm.",
    "gui.guhs.internetcafe.beheerder.terug": "Ah, een vaste klant. Het beeldscherm staat nog aan. Voor de zekerheid: heb je hem al uit en weer aan gezet, njeg?",
    "gui.guhs.internetcafe.beheerder.hoe": "Zie je dat reuzenbeeldscherm achterin? Gewoon erdoorheen lopen. Aan de andere kant is Guhpixel: spelletjes, "
                                           "een winkel en een lobby vol guhs. Daarna kun je er altijd heen met /lobby.",
    "gui.guhs.internetcafe.beheerder.traag": "Traag? Het staat op de gevel, njeg. Onze verbinding is zo traag dat de guhs erbij in slaap vallen. "
                                             "Dat noemen wij service.",
    "gui.guhs.internetcafe.beheerder.slapers": "Die downloaden één plaatje van een kaasknabbel. Al drie weken. Het staat op 3 procent. Niet wakker "
                                               "maken: dan begint het opnieuw.",
    "gui.guhs.internetcafe.beheerder.optie.hoe": "Hoe kom ik op Guhpixel?",
    "gui.guhs.internetcafe.beheerder.optie.traag": "Het internet is hier wel erg traag",
    "gui.guhs.internetcafe.beheerder.optie.slapers": "Wat doen al die slapende guhs?",
    # --- the Guhdex section ---
    "gui.guhs.lobby.gids.kop": "De Guhpixel-lobby",
    "gui.guhs.lobby.gids.uitleg": "Typ /lobby, /hub of /l om naar de lobby te gaan (of klik thuis op je Guhpixel-poort). In de lobby brengt /lobby "
                                  "je weer precies terug naar waar je vandaan kwam.",
    "gui.guhs.lobby.gids.rang": "Je rang",
    "gui.guhs.lobby.gids.naar_rang": "Op weg naar %s",
    "gui.guhs.lobby.gids.rangen": "Alle rangen",
    "gui.guhs.lobby.gids.rangen.lijst": "%1$s 0 · %2$s %3$s · %4$s %5$s · %6$s %7$s · %8$s %9$s",
    "gui.guhs.lobby.gids.kabeltje": "Netwerkkabeltje",
    "gui.guhs.lobby.gids.kabeltje.ja": "Gekregen van de Welkomstguh",
    "gui.guhs.lobby.gids.kabeltje.nee": "Haal hem bij de Welkomstguh, njeg",
    "gui.guhs.lobby.gids.parkour": "Lobby-parkour",
    "gui.guhs.lobby.gids.parkour.tijd": "%1$s (%2$s keer gehaald)",
    "gui.guhs.lobby.gids.parkour.geen": "Nog niet gehaald, njeg",
    "gui.guhs.lobby.gids.knabbels": "Gouden knabbels",
    "gui.guhs.lobby.gids.knabbels.tip.0": "Tip: kijk eens ACHTER dingen. Guhs verstoppen alles achter dingen.",
    "gui.guhs.lobby.gids.knabbels.tip.1": "Tip: sommige knabbels liggen op de daken. Daar kom je alleen met de parkour.",
    "gui.guhs.lobby.gids.knabbels.tip.2": "Tip: er zit een luikje in de grond, ergens achter een bord. Njeg.",
    # --- titles ---
    "gui.guhs.titels.naam.lobby_knabbelspeurder": "Knabbelspeurder",
    "gui.guhs.titels.hint.lobby_knabbelspeurder": "Vind alle tien de gouden knabbels in de Guhpixel-lobby.",
    "gui.guhs.titels.naam.lobby_dakhaas": "Dakhaas",
    "gui.guhs.titels.hint.lobby_dakhaas": "Haal de finish van de lobby-parkour.",
    "gui.guhs.titels.naam.lobby_mvg": "Meest Vadsige Guh",
    "gui.guhs.titels.hint.lobby_mvg": "Bereik de hoogste rang van Guhpixel.",
}
for _i, _naam in enumerate(CHAT_NAMEN):
    TEXTS[f"gui.guhs.lobby.chat.naam.{_i}"] = _naam
for _i, _regel in enumerate(CHAT_REGELS):
    TEXTS[f"gui.guhs.lobby.chat.regel.{_i}"] = _regel
for _i, _regel in enumerate(VERKOPER_ROEP):
    TEXTS[f"gui.guhs.lobby.verkoper.roep.{_i}"] = _regel
for _i, _regel in enumerate(VERKOPER_ONDER):
    TEXTS[f"gui.guhs.lobby.verkoper.onder.{_i}"] = _regel
for _i, _regel in enumerate(WINKEL_GRAPPEN):
    TEXTS[f"gui.guhs.lobby.winkel.grap.{_i}"] = _regel
for _i, _regel in enumerate(SLAPER):
    TEXTS[f"gui.guhs.internetcafe.slaper.{_i}"] = _regel

NPCS = {   # kind: (name, hue, sat, val)
    "lobby_welkomstguh": ("Welkomstguh", 0.13, 0.75, 1.0), "lobby_verkoper_guh": ("Verkoper-guh", 0.33, 0.6, 1.0),
    "lobby_chatguh": ("Lobbyguh", 0.55, 0.55, 1.0), "internetcafe_beheerder": ("Beheerder-guh", 0.1, 0.3, 0.92),
    "internetcafe_slaper": ("Slapende surfer", 0.93, 0.6, 1.0),
}
SOUNDS = {
    "lobby.knabbel": [{"name": "minecraft:block.amethyst_block.chime", "type": "event", "pitch": 1.3}],
    "lobby.parkour_start": [{"name": "minecraft:block.note_block.pling", "type": "event"}],
    "lobby.parkour_finish": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 1.2, "volume": 0.8}],
    "lobby.chat": [{"name": "minecraft:block.note_block.hat", "type": "event", "pitch": 1.5, "volume": 0.5}],
}
SUBTITLES = {"lobby.knabbel": "Gouden knabbel glinstert", "lobby.parkour_start": "Parkourplaat piept", "lobby.parkour_finish": "Finish gehaald",
             "lobby.chat": "Lobbychat tikt"}
ADVANCEMENTS = ("lobby_welkom", "lobby_parkour", "lobby_knabbels", "lobby_rang_mvg", "internetcafe_beheerder")
TEST_PLEIN = "lobby_test_plein"


# =====================================================================================================================
# textures
# =====================================================================================================================
def _vlak(basis, var, seed):
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    n = rng.normal(0, var, (16, 16))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _plaat(basis, rand, teken, seed):
    """A parkour plate: a coloured field with a light border and a white symbol (10 x 10 pixels in the middle)."""
    a = _vlak(basis, 5, seed)
    a[0, :, :3] = rand
    a[15, :, :3] = rand
    a[:, 0, :3] = rand
    a[:, 15, :3] = rand
    for y, rij in enumerate(teken):
        for x, ch in enumerate(rij):
            if ch == "#":
                a[3 + y, 3 + x, :3] = (255, 255, 255)
    return Image.fromarray(a)


def textures(h):
    pijl = ["..#.......", "..##......", "..###.....", "..####....", "..#####...", "..#####...", "..####....", "..###.....", "..##......", "..#......."]
    vlag = ["..#.......", "..######..", "..######..", "..######..", "..######..", "..#.......", "..#.......", "..#.......", "..#.......", ".###......"]
    h.save(_plaat((90, 200, 90), (190, 255, 190), pijl, 7201), "block", "lobby_parkour_start.png")
    h.save(_plaat((240, 190, 40), (255, 235, 150), vlag, 7202), "block", "lobby_parkour_tussenpunt.png")
    a = _vlak((245, 245, 245), 4, 7203)                        # the finish: a chequered flag
    for y in range(16):
        for x in range(16):
            if (x // 4 + y // 4) % 2:
                a[y, x, :3] = (26, 26, 30)
    h.save(Image.fromarray(a), "block", "lobby_parkour_finish.png")
    a = _vlak((250, 196, 52), 12, 7204)                        # the golden knabbel: a shiny cheese puff
    rng = np.random.default_rng(7205)
    for _ in range(14):
        x, y = rng.integers(0, 16, 2)
        a[y, x, :3] = (255, 240, 170)
    for _ in range(8):
        x, y = rng.integers(0, 16, 2)
        a[y, x, :3] = (214, 140, 30)
    h.save(Image.fromarray(a), "block", "lobby_gouden_knabbel.png")
    # the café's screen: a dark tube with a loading bar that never moves, and a little "Zzz"
    beige, donker, licht = (214, 200, 168), (168, 152, 120), (236, 226, 200)
    a = _vlak(beige, 4, 7206)
    a[0, :, :3] = licht
    a[15, :, :3] = donker
    for y in range(2, 12):
        for x in range(2, 14):
            if (x in (2, 13)) and (y in (2, 11)):
                continue
            a[y, x, :3] = (22, 30, 60) if y % 2 else (30, 40, 78)
    for x in range(4, 12):                                     # the bar's outline, 1 pixel filled
        a[8, x, :3] = (120, 140, 200)
    a[8, 4, :3] = (110, 230, 130)
    for (x, y) in ((8, 4), (9, 4), (10, 4), (9, 5), (8, 6), (9, 6), (10, 6)):   # a little "Z"
        a[y, x, :3] = (255, 150, 200)
    a[13, 12, :3] = (240, 190, 60)                             # the (orange: "busy") light
    for x in range(3, 8):
        a[13, x, :3] = donker
    h.save(Image.fromarray(a), "block", "internetcafe_scherm.png")
    a = _vlak(beige, 4, 7207)                                  # the keyboard: rows of keys
    for y in range(2, 14, 3):
        for x in range(1, 15, 2):
            a[y, x, :3] = licht
            a[y + 1, x, :3] = donker
    h.save(Image.fromarray(a), "block", "internetcafe_toetsen.png")


# =====================================================================================================================
# blocks
# =====================================================================================================================
def _kubus(fr, to, tex, faces=("down", "up", "north", "south", "west", "east"), **per_face):
    return {"from": list(fr), "to": list(to), "faces": {f: {"texture": per_face.get(f, tex)} for f in faces}}


def blokken(h):
    A = h.A
    for naam in ("lobby_parkour_start", "lobby_parkour_tussenpunt", "lobby_parkour_finish"):
        h.w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{naam}"}})
        h.w(f"{A}/blockstates/{naam}.json", {"variants": {"": {"model": f"guhs:block/{naam}"}}})
    # the golden knabbel: three little puffs on a heap (the same model for every nummer)
    h.w(f"{A}/models/block/lobby_gouden_knabbel.json", {
        "parent": "minecraft:block/block", "textures": {"particle": "guhs:block/lobby_gouden_knabbel", "k": "guhs:block/lobby_gouden_knabbel"},
        "elements": [_kubus((5, 0, 5), (11, 4, 11), "#k"), _kubus((4, 0, 8), (7, 3, 12), "#k"), _kubus((9, 0, 4), (12, 3, 7), "#k"),
                     _kubus((6, 4, 6), (10, 7, 10), "#k")]})
    h.w(f"{A}/blockstates/lobby_gouden_knabbel.json", {"variants": {"": {"model": "guhs:block/lobby_gouden_knabbel"}}})
    # the old beige computer (the screen faces north): a monitor on a foot, a keyboard in front of it
    kast, scherm, toets = "#kast", "#scherm", "#toetsen"
    lib.deco(h, "internetcafe_computer", [
        _kubus((2, 2, 6), (14, 13, 15), kast, north=scherm),
        _kubus((5, 0, 8), (11, 2, 13), kast, faces=("north", "south", "west", "east", "down")),
        _kubus((2, 0, 1), (14, 1, 5), kast, up=toets),
    ], {"particle": "guhs:block/guhpixel_poort_kast", "kast": "guhs:block/guhpixel_poort_kast", "scherm": "guhs:block/internetcafe_scherm",
        "toetsen": "guhs:block/internetcafe_toetsen"},
        "Oude beige computer", "Uit het Guh-internetcafé. Hij laadt nog steeds. Hij staat op 3 procent, njeg.")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:internetcafe_computer"])
    # what a player may right-click in guhpixel
    h.add_tag("guhs/tags/block/guhpixel_bruikbaar", ["guhs:lobby_gouden_knabbel"])


# =====================================================================================================================
# templates
# =====================================================================================================================
def kaart(h):
    """data/guhs/guhpixel/lobby_kaart.json: the tables of guhpixel_lobby_bouw.py for the Java side (LobbyKaart)."""
    h.w(f"{h.D}/guhpixel/lobby_kaart.json", {
        "knabbels": [list(bouw.KNABBELS[i]) for i in range(KNABBELS)],
        "chatguhs": [list(g) for g in bouw.CHATGUHS],
        "parkour": {"start": list(bouw.PARKOUR_START), "finish": list(bouw.PARKOUR_FINISH), "tussen": [list(t) for t in bouw.PARKOUR_TUSSEN]}})


def test_plein(h):
    """The game test room of the slice (16 x 12 x 16): a little parkour (start, three checkpoints, finish) in a row and the
    ten golden knabbels in another row. (The floor is template y 0 = relative y 1 in a test.)"""
    s = lib.test_kamer(h, TEST_PLEIN, (16, 12, 16))
    s.set(2, 0, 2, "guhs:lobby_parkour_start")
    for x in (5, 8, 11):
        s.set(x, 0, 2, "guhs:lobby_parkour_tussenpunt")
    s.set(13, 0, 2, "guhs:lobby_parkour_finish")
    for n in range(KNABBELS):
        s.set(3 + n, 1, 8, "guhs:lobby_gouden_knabbel", {"nummer": str(n)})
    s.save(TEST_PLEIN)


def build(h):
    textures(h)
    blokken(h)
    bouw.build(h)
    kaart(h)
    cafe.build(h, spacing=56, separation=20, salt=CAFE_SALT)
    lib.gegarandeerd(h, CAFE, CAFE_SALT, sector=0, ring=GEGARANDEERD)
    for kind, (naam, hue, sat, val) in NPCS.items():
        lib.npc(h, kind, naam, hue, sat, val)
    lib.teksten(h, TEXTS)
    lib.geluid(h, SOUNDS, SUBTITLES)
    for naam in ADVANCEMENTS:
        lib.quest_adv(h, naam)
    test_plein(h)
    selfcheck(h)


# =====================================================================================================================
def selfcheck(h):
    lib.controleer(h, "guhpixel_lobby", blokken=("lobby_gouden_knabbel", "lobby_parkour_start", "lobby_parkour_tussenpunt", "lobby_parkour_finish",
                                                 "internetcafe_computer"),
                   items=("internetcafe_computer",), keys=TEXTS, templates=(bouw.NAME, CAFE, TEST_PLEIN))
    problems = []
    for f in ("guhpixel/lobby_kaart.json", "guhpixel/lobby_versie.json", f"worldgen/structure/{CAFE}.json", f"worldgen/structure_set/{CAFE}.json",
              f"worldgen/structure_set/{CAFE}_gegarandeerd.json", "recipe/guhpixel_poort.json") + tuple(f"advancement/quest/{a}.json" for a in ADVANCEMENTS):
        if not os.path.exists(f"{h.D}/{f}"):
            problems.append(f"missing data/guhs/{f}")
    for kind in NPCS:
        if f"entity.guhs.guh_npc.{kind}" not in h.NL or not os.path.exists(os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            problems.append(f"NPC kind {kind} has no name or texture")
    # the numbers the Java side uses
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "lobby")

    def bron(naam):
        return open(os.path.join(java, naam), encoding="utf-8").read()

    for naam, zoek in (("Knabbels.java", f"AANTAL = {KNABBELS}, MUNTJES = {MUNTJES_KNABBEL}"),
                       ("LobbyParkour.java", f"MUNTJES = {MUNTJES_PARKOUR}, TUSSENPUNTEN = {TUSSENPUNTEN}"),
                       ("Chatguhs.java", f"NAMEN = {len(CHAT_NAMEN)}, REGELS = {len(CHAT_REGELS)}"),
                       ("LobbyRollen.java", f"ROEPEN = {len(VERKOPER_ROEP)}, SNURKEN = {len(SLAPER)}")):
        if zoek not in bron(naam):
            problems.append(f"{naam} does not say '{zoek}'")
    scherm = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "client", "WinkelScherm.java"), encoding="utf-8").read()
    if f"GRAPPEN_LOBBY = {len(WINKEL_GRAPPEN)};" not in scherm:
        problems.append("WinkelScherm.GRAPPEN_LOBBY is not the number of WINKEL_GRAPPEN")
    if len(bouw.KNABBELS) != KNABBELS or len(bouw.PARKOUR_TUSSEN) != TUSSENPUNTEN or len(bouw.CHATGUHS) != len(CHAT_NAMEN):
        problems.append("the tables of guhpixel_lobby_bouw.py do not match the numbers of this module")
    kinds = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhNpcEntity.java"), encoding="utf-8").read()
    for kind in NPCS:
        if kind.upper() + "(" not in kinds:
            problems.append(f"NPC kind {kind} is not in GuhNpcEntity.Kind")
    if problems:
        raise SystemExit("guhpixel_lobby self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("internetcafe_vinden", "De Trage Verbinding", "Ergens in de Guhmensie staat een beige gebouwtje met een schotel op het dak: het "
      "&dGuh-internetcafé \"De Trage Verbinding\"&r. Je superkompas (tab &dMinigames&r) wijst de weg. Binnen slapen guhs achter oude computers, "
      "en de &fBeheerder-guh&r vraagt of je hem al uit en weer aan hebt gezet. Njeg.", "guhs:internetcafe_computer", [fq.structure(CAFE)],
      rewards=(("guhs:kaas_knabbels", 10),), shape="gear", xp=100)
    q("lobby_binnen", "Verbinding gelukt!", "Loop in het internetcafé dwars door het &dreuzenbeeldscherm&r. Piiieeep krrr... en je staat in de "
      "lobby van &dGuhpixel&r! Vanaf nu werkt &e/lobby&r (of &e/hub&r, of &e/l&r) overal. Praat met de &eWelkomstguh&r: die geeft je een "
      "&bNetwerkkabeltje&r.", "guhs:guhpixel_netwerkkabeltje", [fq.dim("guhs:guhpixel")], deps=["internetcafe_vinden"], shape="octagon", xp=150)
    q("lobby_poort", "Een poort voor thuis", "Met het &bNetwerkkabeltje&r van de Welkomstguh maak je thuis een &dGuhpixel-poort&r: ijzer, glas, "
      "redstone en het kabeltje in het midden. Rechtsklik op het beeldschermpje en hup, je bent in de lobby. Kabeltje kwijt? De Welkomstguh "
      "heeft er nog een.", "guhs:guhpixel_poort", [item("guhs:guhpixel_poort")], deps=["lobby_binnen"])
    q("lobby_parkour", "Dakhaas", "Aan de oostkant van de lobby ligt een groene &astartplaat&r. Stap eraf en de tijd loopt: over de AFK-hoek, "
      "de zwevende kussens en de daken van alle kraampjes naar de &ffinish&r op het dak van de winkel. Vallen doet geen pijn. De eerste keer "
      "krijg je 50 muntjes.", "minecraft:feather", [adv("lobby_parkour")], deps=["lobby_binnen"], xp=100)
    q("lobby_knabbels", "Knabbelspeurder", "In de lobby liggen &6tien gouden knabbels&r verstopt: achter dingen, op daken en... onder de grond. "
      "Rechtsklik erop: elke knabbel is 10 muntjes. Ze blijven liggen voor iedereen, dus niemand pikt ze voor je neus weg.",
      "guhs:gouden_kaasknabbel", [adv("lobby_knabbels")], deps=["lobby_binnen"], xp=150)
    q("lobby_rang_mvg", "Meest Vadsige Guh", "Je &drang&r staat voor je naam en hangt af van alle muntjes die je ooit verdiende: &7[GUH]&r, "
      "&a[VADS]&r (250), &b[VADS+]&r (750), &6[MVG]&r (1250) en &d[MVG++]&r (1750). Word minstens &6[MVG]&r. Uitgeven mag gewoon!",
      "minecraft:gold_ingot", [adv("lobby_rang_mvg")], rewards=(("guhs:kaas_knabbels", 24),), deps=["lobby_binnen"], shape="hexagon", xp=300)
