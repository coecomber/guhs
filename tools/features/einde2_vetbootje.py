"""
einde2 (the Guheinde expansion), slice vetbootje: the player's own vetbootje and the personal heteluchtballon.
Java: nl.juiced.guhs.feature.einde2.vetbootje (E2VetbootjeFeature).

  einde2_vetbootje_modellen.py   the two GeckoLib models, their animations and textures (the boat in layers: base,
                                 sixteen colours, six sail pictures, the lampion's glow), the screen's small sail pictures
  einde2_vetbootje_geluid.py     the sounds (a script of its own: the OGGs are committed)
  here                           the three item icons (vetfluitje, werfboekje, the folded heteluchtballon), the texts,
                                 sounds.json, the two hidden advancements, the test room, the self-check

No Guhdex pages and no FTB quests: the questline of the vetbootje (phase B) and the Vredesballon (phase C) bring them.
"""
import os

from PIL import Image

from features import einde2_kit as kit
from features import einde2_vetbootje_geluid as geluid
from features import einde2_vetbootje_modellen as modellen

G = "gui.guhs.einde2_vetbootje."

# names the scheepswerf can make up (the "Verzin" button; WerfScreen.IDEEEN = how many)
NAAM_IDEEEN = ["De Vette Knabbel", "Glibberguh", "Mika's Nachtmerrie", "De Dobberende Vadsbaal", "Frituurpiraat", "Njegvaart",
               "SS Kaaskorst", "De Slome Guh", "Vadsig door de Lucht", "Knabbelkruiser", "Het Vette Hapje", "Kruimeldief"]
# where the wind blows to: HeteluchtballonEntity.windstreek (0 = south, then by 45 degrees through west)
WINDSTREKEN = ["zuiden", "zuidwesten", "westen", "noordwesten", "noorden", "noordoosten", "oosten", "zuidoosten"]
ZEIL_NAMEN = {"mika": "Mika-snoet", "knabbel": "Knabbelzak", "streep": "Vetstrepen", "hart": "Hartje", "ster": "Vahoeg-ster", "kruimel": "Kruimels"}

TEXTS = {
    "entity.guhs.vetbootje": "Vetbootje",
    "entity.guhs.heteluchtballon": "Heteluchtballon",
    "item.guhs.vetbootje_fluitje": "Vetfluitje",
    "item.guhs.vetbootje_fluitje.lore": "Fluit en je vetbootje komt aangegleden, hoe ver het ook dobbert.",
    "item.guhs.vetbootje_fluitje.lore2": "Ruikt naar frituur. Niet aflikken, njeg.",
    "item.guhs.vetbootje_werfboekje": "Werfboekje",
    "item.guhs.vetbootje_werfboekje.lore": "Kies de kleur, het zeil en de naam van je vetbootje.",
    "item.guhs.vetbootje_werfboekje.lore2": "Met een vetvlek op elke bladzijde.",
    "item.guhs.heteluchtballon": "Heteluchtballon",
    "item.guhs.heteluchtballon.lore": "Zet hem op de grond en hij vouwt zich uit. Sluipen + klikken vouwt hem weer op.",
    "item.guhs.heteluchtballon.lore2": "De wind stuurt. Jij hoeft alleen maar te vadsen.",
    # the vetbootje
    G + "naam.standaard": "Vetbootje van %s",
    G + "naam.leen": "Leenbootje",
    G + "gekregen": "Je hebt een eigen vetbootje! Het ruikt naar frituur en avontuur. Kwijt? Fluit op je Vetfluitje.",
    G + "uitleg": "Vooruit en achteruit varen, links en rechts sturen. %1$s = omhoog, %2$s = omlaag, een klik op het dek = de kist. "
                  "Het is een vetbootje, geen raceguh: rustig aan, njeg.",
    G + "vol": "Vol! Er past echt niemand meer bij.",
    G + "niet_van_jou": "Dit vetbootje is van %s. Pootjes thuis, njeg!",
    G + "naar_huis": "%s dobbert naar huis. Fluit maar als je het weer nodig hebt.",
    G + "kist": "Ruim van %s",
    G + "leen.geen_kist": "Een leenbootje heeft geen kist. Wel een Mika op het zeil.",
    G + "fluit.geen": "Je hebt nog geen vetbootje, njeg. Kijk eens goed rond op een vetschip bij een Mika-vesting.",
    G + "fluit.aan_boord": "Je staat er al op. Vadsig, maar waar.",
    G + "fluit.geen_plek": "Hier past je vetbootje niet. Zoek wat meer lucht, njeg.",
    G + "fluit.komt": "%s komt aangegleden. Glibber, glibber!",
    G + "guh.aan_boord": "%s springt aan boord. Vahoeg!",
    G + "guh.te_vads": "%s is te vads voor het kussentje. Njeg!",
    G + "guh.van_boord": "Iedereen van boord! Pootjes op het droge.",
    G + "guh.geen": "Geen eigen guh in de buurt die mee wil varen.",
    # the scheepswerf screen
    G + "werf.titel": "De Scheepswerf",
    G + "werf.kleur": "Verf",
    G + "werf.zeil": "Zeil",
    G + "werf.naam": "Naam",
    G + "werf.naam_hint": "Vetbootje van jou",
    G + "werf.verzin": "Verzin",
    G + "werf.verf": "Verf het!",
    G + "werf.voet": "Mika's keurmerk: \"Vet genoeg.\"",
    G + "werf.klaar": "Vers geverfd! Niet aankomen, het plakt nog.",
    # the heteluchtballon
    G + "ballon.gekregen": "Je hebt een eigen heteluchtballon! Zet hem op de grond om hem uit te vouwen.",
    G + "ballon.neergezet": "Stap maar in het mandje. Sluipen + klikken vouwt de ballon weer op.",
    G + "ballon.uitleg": "%s ingedrukt = de brander aan = omhoog. Loslaten = zachtjes zakken. De wind doet de rest: op elke hoogte "
                         "waait hij een andere kant op. Kijk naar het vaantje op het mandje.",
    G + "ballon.wind": "De wind waait naar het %s",
    G + "ballon.vol": "Het mandje is vol.",
    G + "ballon.niet_van_jou": "Deze ballon is van %s. Pootjes thuis, njeg!",
    G + "ballon.opgevouwen": "Opgevouwen. Past precies in je zak, njeg.",
    G + "ballon.lek": "Deze oude ballon is lek. Pssst... weg. Je nieuwe doet het gewoon.",
    G + "ballon.geen_plek": "Hier is geen plek om de ballon uit te vouwen.",
    # the op commands
    G + "commando.geef": "%s heeft nu een vetbootje: %s",
    G + "commando.roep": "%s komt eraan",
    G + "commando.stel": "Geverfd: %s, met het zeil %s",
    G + "commando.leen": "Een leenbootje neergezet",
    G + "commando.geen": "Dat lukt niet (geen vetbootje, geen plek, of een onbekende kleur of zeil)",
    G + "commando.ballon": "%s heeft nu een heteluchtballon",
    G + "commando.ballon_opnieuw": "%s heeft een nieuwe heteluchtballon (de oude is lek)",
    # subtitles
    "subtitles.guhs.einde2_vetbootje.fluitje": "Een vetfluitje toetert",
    "subtitles.guhs.einde2_vetbootje.aankomst": "Een vetbootje glijdt aan",
    "subtitles.guhs.einde2_vetbootje.kraak": "Een vetbootje kraakt",
    "subtitles.guhs.einde2_vetbootje.verf": "Verf klotst",
    "subtitles.guhs.einde2_vetbootje.brander": "Een brander loeit",
    "subtitles.guhs.einde2_vetbootje.vouw": "Ballondoek ritselt",
}
TEXTS.update({G + f"naam.idee.{i}": n for i, n in enumerate(NAAM_IDEEEN)})
TEXTS.update({G + f"wind.{i}": n for i, n in enumerate(WINDSTREKEN)})
TEXTS.update({G + f"zeil.{z}": ZEIL_NAMEN[z] for z in modellen.ZEILEN})

VERBORGEN = ["einde2_vetbootje_gekregen", "einde2_vetbootje_ballon"]     # = Vetbootje.GEKREGEN, Heteluchtballon.GEKREGEN
TEST_KAMER = "einde2_vetbootje_test_kamer"

# --- the item icons (16 x 16) -------------------------------------------------------------------------------------------------
ICON_PALET = {
    "k": (46, 30, 28, 255),        # outline
    "g": (246, 200, 70, 255), "G": (255, 232, 140, 255), "d": (190, 136, 34, 255),          # the whistle: greasy gold
    "t": (124, 78, 50, 255), "T": (160, 106, 68, 255),                                       # cord and leather
    "h": (246, 180, 46, 255), "H": (255, 228, 130, 255),                                     # a drop of vet
    "o": (212, 122, 54, 255), "O": (232, 152, 78, 255),                                      # the booklet's cover
    "w": (255, 244, 222, 255), "p": (236, 160, 190, 255), "r": (214, 40, 46, 255),           # paper / sail, Mika pink, red
    "m": (72, 48, 34, 255),                                                                  # the mast
    "y": (250, 206, 84, 255), "z": (246, 160, 196, 255), "c": (255, 244, 222, 255),          # balloon cloth: yellow, pink, cream
    "b": (206, 160, 96, 255), "B": (150, 104, 58, 255),                                      # the basket
}
ICONS = {
    # a fat golden whistle on a brown cord, a drop of vet hanging from it
    "vetbootje_fluitje": [
        "................", "....ttt.........", "...t...t........", "..t.....t.......", "..t......t......", "...t......t.....",
        "....t...kkkkkk..", ".....t.kGGGGGgk.", "......kGgggkkgkk", "......kggggkkggk", "......kgggggggdk", ".......kdddddkk.",
        "........kkkkk.h.", "..............H.", "..............h.", "................"],
    # the werfboekje: an orange booklet with a little Mika sail on its cover, a vet stain, a ribbon
    "vetbootje_werfboekje": [
        "................", "..kkkkkkkkkkk...", ".kOOOOOOOOOOok..", ".kOoooomooooowk.", ".kOowwwmwwwoowk.", ".kOowpwmwpwoowk.",
        ".kOowwpppwwoowk.", ".kOowrpmprwoowk.", ".kOowwpppwwoowk.", ".kOoooomooooowk.", ".kOoohhmooooowk.", ".kOohHhooooorwk.",
        ".kOoohooooooowk.", ".kooooooooorrwk.", "..kkkkkkkkkrkk..", "...........r...."],
    # the folded balloon: a bundle of yellow, pink and cream cloth tied with a rope, the little basket on top
    "heteluchtballon": [
        "................", ".....kkkkkk.....", "....kbBbBbBk....", "....kBbBbBbk....", "....kbBbBbBk....", "...kkkkkkkkkk...",
        "..kyyyyzzzzcck..", ".kyyyyzzzzcccck.", ".kyyyttttttccck.", ".kzzzzcccyyyyyk.", ".kzzzccctyyyyzk.", ".kcccyyytzzzzzk.",
        ".kccyyyytzzzcck.", "..kyyyzzzzccck..", "...kkkkkkkkkk...", "................"],
}


def textures(h):
    modellen.build(h)
    for naam, rows in ICONS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), naam
        h.save(h.grid(rows, ICON_PALET), "item", f"{naam}.png")
        h.item_model(naam)


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def sounds(h):
    def patch(d):
        for k in [k for k in d if k.startswith("einde2_vetbootje.")]:
            del d[k]
        for event, files in geluid.BESTANDEN.items():
            d[f"einde2_vetbootje.{event}"] = {"sounds": [f"guhs:einde2_vetbootje/{f}" for f in files],
                                              "subtitle": f"subtitles.guhs.einde2_vetbootje.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    for name in VERBORGEN:
        kit.verborgen(h, name)


def test_kamer(h):
    """The game tests' room: a stone floor of 15 x 15 with 14 blocks of air above it (room to rise, sink and drift)."""
    s = h.Structure((15, 15, 15))
    s.fill(0, 0, 0, 14, 0, 14, "minecraft:smooth_stone")
    s.save(TEST_KAMER)


def selfcheck(h):
    problems = modellen.check(h)
    for event, files in geluid.BESTANDEN.items():
        for f in files:
            if not os.path.exists(f"{h.A}/sounds/einde2_vetbootje/{f}.ogg"):
                problems.append(f"sound file {f}.ogg is missing: run python tools/features/einde2_vetbootje_geluid.py")
        if f"subtitles.guhs.einde2_vetbootje.{event}" not in TEXTS:
            problems.append(f"no subtitle for {event}")
    for naam in ICONS:
        if Image.open(os.path.join(h.TEX, "item", f"{naam}.png")).size != (16, 16):
            problems.append(f"item icon {naam} is not 16 x 16")
    for zeil in modellen.ZEILEN:
        if G + f"zeil.{zeil}" not in TEXTS:
            problems.append(f"no name for the sail {zeil}")
        if not os.path.exists(os.path.join(h.TEX, "gui", f"einde2_vetbootje_zeil_{zeil}.png")):
            problems.append(f"no screen picture for the sail {zeil}")
    for i, n in enumerate(NAAM_IDEEEN):
        if len(n) > 24:
            problems.append(f"the name idea {n!r} is longer than Vetbootje.NAAM_MAX")
    if len(NAAM_IDEEEN) != 12 or len(WINDSTREKEN) != 8:
        problems.append("WerfScreen.IDEEEN (12) / the eight wind directions do not match the lists here")
    if problems:
        raise SystemExit("einde2_vetbootje:\n  " + "\n  ".join(problems))


def build(h):
    textures(h)
    texts(h)
    sounds(h)
    advancements(h)
    test_kamer(h)
    selfcheck(h)
