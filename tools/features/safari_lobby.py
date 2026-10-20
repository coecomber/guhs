"""
1.6.0 Op Safari! step 2, slice S2: the lobby of Safaripark Vadse Bergen (Java: feature/safari/lobby; English:
tools/lang/en/c161_safari_lobby.json).

Here: the Dutch texts of the lobby's characters, giftshop, aaihoek, lodge and welcome; the skins of the 13 lobby guhs
(the sitting guh recoloured: khaki for the rangers and staff, the zone colour for the eight experts); the items komkommer,
paprika and the eight zone badges (textures + models); and data/guhs/safari/winkel.json, the giftshop's item list (an entry
whose item does not exist is simply not shown, so S6 only has to make the items it promises here).
The spots of the characters (data/guhs/safari/lobby.json) come from the park generator (safari_park/zone_s2_lobby.py).
"""
import json
import os

from PIL import Image, ImageDraw

NAME = "safari_lobby"
CLOTHES = []
BONES = {}

ZONES = ["savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie"]
ZONE_NAAM = {"savanne": "Savanne", "delta": "Rivierdelta", "regenwoud": "Regenwoud", "bamboe": "Bamboebergen",
             "ijskust": "IJskust", "oertijd": "Oerijstijd", "europa": "Europees Woud", "australie": "Australië"}
# zone: (hue, sat, val) for the expert's uniform, and the badge colour
ZONE_KLEUR = {
    "savanne": ((0.13, 0.85, 1.0), (232, 196, 70)), "delta": ((0.50, 0.75, 0.9), (60, 180, 190)),
    "regenwoud": ((0.33, 0.85, 0.75), (60, 150, 60)), "bamboe": ((0.24, 0.8, 0.95), (140, 200, 80)),
    "ijskust": ((0.56, 0.55, 1.0), (140, 200, 240)), "oertijd": ((0.58, 0.12, 0.9), (200, 205, 210)),
    "europa": ((0.38, 0.7, 0.6), (70, 120, 60)), "australie": ((0.07, 0.9, 1.0), (220, 120, 50)),
}

NPCS = {
    # kind: (name, hue, sat, val)
    "safari_hoofdranger": ("Hoofdranger Guhmbo", 0.11, 0.55, 0.75),
    "safari_fotograaf": ("Fotograaf Flitsguh", 0.11, 0.45, 0.85),
    "safari_verzorger": ("Dierenverzorger Knuffelien", 0.30, 0.45, 0.7),
    "safari_verkoper": ("Giftshop-guh Kassie", 0.12, 0.35, 0.95),
    "safari_lodgebeheerder": ("Lodgebeheerder Snurkguh", 0.08, 0.45, 0.65),
}
for _z in ZONES:
    NPCS[f"safari_expert_{_z}"] = (f"Expert {ZONE_NAAM[_z]}",) + ZONE_KLEUR[_z][0]

TEKSTEN = {
    # --- characters ---
    "gui.guhs.safari.optie.doei": "Doei!",
    "gui.guhs.safari.optie.dank": "Dankjewel, vads!",
    "gui.guhs.safari.ranger.hallo": "Hallo daar, junior-ranger! Ik ben Guhmbo, de hoofdranger van Safaripark Vadse Bergen. Kan ik je ergens mee helpen, njeg?",
    "gui.guhs.safari.ranger.optie.tip": "Heb je een tip van de dag?",
    "gui.guhs.safari.ranger.optie.uitleg": "Wat kan ik hier allemaal doen?",
    "gui.guhs.safari.ranger.tip": "Psst... vanochtend zag ik de %1$s in de %2$s, ergens bij x %3$s, z %4$s. Sluip er voorzichtig heen, vads, anders verstopt hij zich!",
    "gui.guhs.safari.ranger.niks": "alle dieren",
    "gui.guhs.safari.ranger.uitleg": "Spot en fotografeer alle dieren voor je Safari-dex! Bij het expertenpaviljoen haal je een badge als een zone vol is. In de aaihoek kun je helpen, in de giftshop kun je met Safarimunten winkelen, en in de lodge slaap je lekker. Vahoeg!",
    "gui.guhs.safari.fotograaf.hallo": "Klik-klik! Ik ben Flitsguh. Hoe dichterbij, hoe scherper, en een dier dat iets leuks doet geeft extra sterren. Njeg, en niet bewegen!",
    "gui.guhs.safari.fotograaf.camera": "Jij hebt nog geen camera! Hier, deze mag je hebben. Foto's van dieren komen in je Safari-dex. Zeg eens kaas... of eigenlijk: zeg eens knabbel!",
    "gui.guhs.safari.verzorger.hallo": "Hoi! Ik ben Knuffelien van de aaihoek. Vandaag hebben ze trek in:\n%s\nHelp je mee? Voeren, borstelen, of de alpaca uitlaten aan het touwtje!",
    "gui.guhs.safari.verzorger.optie.voer": "Mag ik een zakje voer?",
    "gui.guhs.safari.verzorger.voer": "Alsjeblieft: een beetje van alles, plus een borstel en een touwtje als je die nog niet had. Raad maar wat ze vandaag lekker vinden, njeg!",
    "gui.guhs.safari.verzorger.voer.al": "Je hebt vandaag al een zakje gehad, vads. Morgen weer!",
    "gui.guhs.safari.verkoper.hallo": "Welkom in de giftshop! Alles voor Safarimunten, en niks is te vads.",
    "gui.guhs.safari.lodge.hallo": "Welkom in de lodge! Pak gerust een hutje: slaap je hier, dan word je ook hier wakker. En wie hier vroeg opstaat, krijgt voor zijn eerste foto van de dag dubbele munten. Snurk lekker, njeg!",
    "gui.guhs.safari.lodge.vroege_vogel": "Goedemorgen, vroege vogel! Je eerste foto van vandaag levert dubbele Safarimunten op.",
    # --- welcome (first visit) ---
    "gui.guhs.safari.welkom.1": "Welkom in Safaripark Vadse Bergen! Ik ben Guhmbo, de hoofdranger. Wat fijn dat je er bent, vads!",
    "gui.guhs.safari.welkom.2": "Hier wonen meer dan veertig soorten dieren, van leeuwen tot luiaards. Sommigen verstoppen zich heel goed, dus kijk goed om je heen.",
    "gui.guhs.safari.welkom.3": "Fotografeer ze voor je Safari-dex: hoe mooier de foto, hoe meer Safarimunten.",
    "gui.guhs.safari.welkom.3.camera": "Hier is je camera! Fotografeer de dieren voor je Safari-dex: hoe mooier de foto, hoe meer Safarimunten.",
    "gui.guhs.safari.welkom.4": "De experts in het paviljoen, de aaihoek, de giftshop en de lodge: kijk overal eens rond. Njeg, veel spotplezier!",
    "gui.guhs.safari.welkom.optie": "Op safari, vahoeg!",
    # --- experts ---
    **{f"gui.guhs.safari.expert.{z}.nog": f"Ik ben de expert van de {ZONE_NAAM[z]}. Je hebt %1$s van de %2$s soorten hier gefotografeerd. Nog %3$s te gaan, vads! Kom terug als ze er allemaal in zitten." for z in ZONES if z != "australie"},
    **{f"gui.guhs.safari.expert.{z}.al": f"Je bent een echte {ZONE_NAAM[z]}-kenner! Al %1$s van de %2$s soorten in je dex. Vahoeg!" for z in ZONES if z != "australie"},
    **{f"gui.guhs.safari.expert.{z}.beloning": "Alle soorten van de %s in je dex? Wauw! Hier is je zonebadge, een schilderij voor thuis en 100 Safarimunten. Njeg, wat ben ik trots!" for z in ZONES if z != "australie"},
    "gui.guhs.safari.expert.australie.nog": "G'day, njeg mate! Je hebt %1$s van de %2$s outback-soorten gekiekt. Nog %3$s te gaan, no worries, vads!",
    "gui.guhs.safari.expert.australie.al": "Strewth, wat een vahoege outback-kenner! %1$s van de %2$s soorten. Good on ya, mate!",
    "gui.guhs.safari.expert.australie.beloning": "Crikey! Alle soorten van %s in de pocket! Hier, mate: je badge, een schilderij en 100 Safarimunten. No worries, njeg!",
    # --- aaihoek ---
    "gui.guhs.safari.aaihoek.hint": "%1$s: %2$s",
    "gui.guhs.safari.aaihoek.voer.wortel": "iets oranjes en knapperigs",
    "gui.guhs.safari.aaihoek.voer.hooi": "iets droogs en geels",
    "gui.guhs.safari.aaihoek.voer.appel": "iets ronds en sappigs",
    "gui.guhs.safari.aaihoek.voer.komkommer": "iets groens en frisjes",
    "gui.guhs.safari.aaihoek.voer.paprika": "iets felroods met een steeltje",
    "gui.guhs.safari.aaihoek.voer.suikerklontje": "iets wits en zoets",
    "gui.guhs.safari.aaihoek.smul": "%s smult ervan! Vahoeg!",
    "gui.guhs.safari.aaihoek.geborsteld": "%s glanst helemaal na het borstelen!",
    "gui.guhs.safari.aaihoek.bah.niet": "Bah! Dat eet %s helemaal niet, njeg.",
    "gui.guhs.safari.aaihoek.bah.vandaag": "Bah! Daar heeft %s vandaag geen zin in. Vraag de verzorger maar.",
    "gui.guhs.safari.aaihoek.al_gedaan": "Dat heb je vandaag al gedaan. Morgen weer, vads!",
    "gui.guhs.safari.aaihoek.genoeg": "(Je hebt vandaag al genoeg verdiend, maar het diertje vindt het nog steeds fijn.)",
    "gui.guhs.safari.aaihoek.tip.geitje": "Het geitje knabbelt aan je mouw. Voer, borstel, of vraag de verzorger wat het lekker vindt!",
    "gui.guhs.safari.aaihoek.tip.alpaca": "De alpaca kijkt je aan. Met een touwtje kun je hem uitlaten!",
    "gui.guhs.safari.aaihoek.tip.cavia": "Piep piep! De cavia hoopt op iets lekkers.",
    "gui.guhs.safari.aaihoek.tip.ezeltje": "Iiee-aah! Het ezeltje wil geborsteld worden.",
    "gui.guhs.safari.aaihoek.wandel.begin": "Je laat de alpaca uit! Loop het rondje langs alle punten en breng hem terug naar zijn hok.",
    "gui.guhs.safari.aaihoek.wandel.stap": "Punt %1$s van %2$s, de alpaca dribbelt vrolijk mee!",
    "gui.guhs.safari.aaihoek.wandel.klaar": "Rondje klaar! De alpaca is moe maar blij.",
    "gui.guhs.safari.aaihoek.wandel.weg": "De alpaca is terug naar zijn hok gedribbeld.",
    "gui.guhs.safari.aaihoek.wandel.al": "Je laat al een alpaca uit, njeg.",
    "gui.guhs.safari.aaihoek.wandel.bezet": "Deze alpaca wordt al uitgelaten.",
    # --- visitor guhs ---
    "gui.guhs.safari.bezoeker.0": "Njeg, ik heb net een leeuw gezien!",
    "gui.guhs.safari.bezoeker.1": "Ik ben op safari, ik ga niet mee naar huis, vads.",
    "gui.guhs.safari.bezoeker.2": "Heb jij de okapi al gevonden? Ik zoek al de hele dag!",
    "gui.guhs.safari.bezoeker.3": "Die pinguïns zijn nóg vadsiger dan ik. Vahoeg!",
    "gui.guhs.safari.bezoeker.4": "Ssst... ik spot een stokstaartje.",
    # --- giftshop ---
    "gui.guhs.safari.winkel.titel": "Giftshop Vadse Bergen",
    "gui.guhs.safari.winkel.label": "SAFARI",
    "gui.guhs.safari.winkel.saldo": "%s Safarimunten",
    "gui.guhs.safari.winkel.grap.0": "Alles op voorraad, behalve de olifant.",
    "gui.guhs.safari.winkel.grap.1": "Knuffels niet in de bek nemen. Ook niet als je een leeuw bent.",
    "gui.guhs.safari.winkel.grap.2": "Nieuw: pinguïnhoedjes voor pinguïns (geen pinguïns gevonden die ze willen).",
    "gui.guhs.safari.winkel.grap.3": "Betalen kan met Safarimunten. Knabbeltjes worden opgegeten.",
    "gui.guhs.safari.winkel.grap.4": "De giraffe vond deze winkel te laag.",
    "gui.guhs.safari.winkel.grap.5": "Gratis inpakken? Nee, njeg. Vadsig inpakken: ja!",
    "gui.guhs.safari.winkel.kleine_lettertjes": "Kleine lettertjes: verblijfsborden alleen voor dieren uit je Safari-dex, één per soort per 7 dagen. Dieren niet terugbrengen.",
    "gui.guhs.safari.winkel.groep.kleding": "Safarikleding",
    "gui.guhs.safari.winkel.groep.borden": "Verblijfsborden",
    "gui.guhs.safari.winkel.groep.blokken": "Safari-bouwblokken",
    "gui.guhs.safari.winkel.groep.kunst": "Posters en schilderijen",
    "gui.guhs.safari.winkel.groep.voer": "Voer",
    "gui.guhs.safari.winkel.groep.blauwdrukken": "Blauwdrukken",
    "gui.guhs.safari.winkel.leeg": "De schappen zijn leeg. Kom later terug, njeg!",
    "gui.guhs.safari.winkel.aantal": "%s dingen",
    "gui.guhs.safari.winkel.aantal.1": "1 ding",
    "gui.guhs.safari.winkel.prijs": "%1$s Safarimunten",
    "gui.guhs.safari.winkel.prijs_al": "%1$s Safarimunten · al %2$s gekocht",
    "gui.guhs.safari.winkel.heb_je": "Heb je al, vahoeg!",
    "gui.guhs.safari.winkel.knop.koop": "Koop",
    "gui.guhs.safari.winkel.knop.op": "Heb je",
    "gui.guhs.safari.winkel.max": "Gekocht: %1$s van %2$s",
    "gui.guhs.safari.winkel.gekocht": "Gekocht: %s. Vahoeg!",
    "gui.guhs.safari.winkel.tekort": "Nog %s Safarimunten tekort. Ga nog wat dieren kieken, njeg!",
    "gui.guhs.safari.winkel.nee.onbekend": "Dat verkoop ik niet (meer), njeg.",
    "gui.guhs.safari.winkel.nee.niet_hier": "Kopen kan alleen in Safaripark Vadse Bergen, njeg.",
    "gui.guhs.safari.winkel.nee.max": "Daar heb je er al genoeg van, njeg.",
    "gui.guhs.safari.winkel.nee.eis": "Dat kun je nog niet kopen, njeg.",
    "gui.guhs.safari.winkel.nee.te_duur": "Daar heb je nog niet genoeg Safarimunten voor, njeg.",
    "gui.guhs.safari.winkel.nee.mislukt": "Dat lukte niet. Je munten heb je nog, njeg.",
    "gui.guhs.safari.winkel.binnenkort": "Binnenkort in de winkel!",
    "gui.guhs.safari.winkel.blauwdrukken": "Safari-blauwdrukken",
    "gui.guhs.safari.winkel.blauwdrukken.uitleg": "Bouw de lodge, de rangerpost of de uitkijktoren gewoon thuis na. Binnenkort, vads!",
    "gui.guhs.safari.winkel.aaihoekvoer": "Een handje voer voor de aaihoek (4 stuks).",
    "gui.guhs.safari.winkel.bord": "Verblijfsbord: %s",
    "gui.guhs.safari.winkel.bord.uitleg.gewoon": "Een familie van 4 voor je eigen base (2 volwassenen + 2 jongen). Een gewoon dier.",
    "gui.guhs.safari.winkel.bord.uitleg.middel": "Een familie van 4 voor je eigen base (2 volwassenen + 2 jongen). Een bijzonder dier.",
    "gui.guhs.safari.winkel.bord.uitleg.zeldzaam": "Een familie van 4 voor je eigen base (2 volwassenen + 2 jongen). Een zeldzaam dier!",
    "gui.guhs.safari.winkel.bord.wacht_dagen": "Nog %s dag(en) wachten voor een nieuw bord van deze soort.",
    "gui.guhs.safari.winkel.bord.wacht_uren": "Nog %s uur wachten voor een nieuw bord van deze soort.",
    # --- items ---
    "item.guhs.safari_komkommer": "Komkommer",
    "item.guhs.safari_paprika": "Paprika",
    **{f"item.guhs.safari_badge_{z}": f"Zonebadge: {ZONE_NAAM[z]}" for z in ZONES},
}

# the giftshop's item list: S6 makes these items (an entry whose item does not exist is not shown)
WINKEL = [
    {"id": "kleding_safarihoed", "groep": "kleding", "item": "guhs:safari_hoed", "prijs": 25, "max": 1},
    {"id": "kleding_verrekijker", "groep": "kleding", "item": "guhs:safari_verrekijker", "prijs": 40, "max": 1},
    {"id": "kleding_hoedje", "groep": "kleding", "item": "guhs:safari_hoedje", "prijs": 15},
    {"id": "kleding_vestje", "groep": "kleding", "item": "guhs:kaki_vestje", "prijs": 20},
    {"id": "kleding_rangerpet", "groep": "kleding", "item": "guhs:rangerpet", "prijs": 15},
    {"id": "voer_safarivoer", "groep": "voer", "item": "guhs:safarivoer", "prijs": 5, "aantal": 4},
    {"id": "blok_rieten_dak", "groep": "blokken", "item": "guhs:rieten_dak", "prijs": 5, "aantal": 16},
    {"id": "blok_bamboewand", "groep": "blokken", "item": "guhs:bamboewand", "prijs": 5, "aantal": 16},
    {"id": "blok_safarihek", "groep": "blokken", "item": "guhs:safarihek", "prijs": 5, "aantal": 16},
    {"id": "blok_dierentuinbordje", "groep": "blokken", "item": "guhs:dierentuinbordje", "prijs": 8, "aantal": 4},
    {"id": "kunst_poster", "groep": "kunst", "item": "guhs:safari_poster", "prijs": 10},
    {"id": "kunst_schilderij", "groep": "kunst", "item": "guhs:safari_schilderij", "prijs": 30},
]


def _badge(kleur):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    donker = tuple(int(c * 0.55) for c in kleur)
    d.ellipse([1, 1, 14, 14], fill=(240, 200, 80, 255), outline=(140, 100, 30, 255))
    d.ellipse([3, 3, 12, 12], fill=kleur + (255,), outline=donker + (255,))
    # a little paw print
    d.ellipse([6, 7, 9, 10], fill=(255, 250, 235, 255))
    for (x, y) in ((5, 5), (7, 4), (9, 4), (10, 6)):
        d.point((x, y), fill=(255, 250, 235, 255))
    d.point((7, 14), fill=(200, 60, 60, 255))
    d.point((8, 14), fill=(200, 60, 60, 255))
    return img


def _komkommer():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([(3, 12), (12, 3)], fill=(40, 110, 40, 255), width=4)
    d.line([(4, 12), (12, 4)], fill=(80, 160, 60, 255), width=2)
    for (x, y) in ((6, 9), (9, 6), (11, 5)):
        d.point((x, y), fill=(190, 230, 150, 255))
    d.point((12, 2), fill=(120, 90, 40, 255))
    return img


def _paprika():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([3, 5, 12, 14], fill=(210, 40, 40, 255), outline=(140, 20, 20, 255))
    d.line([(7, 6), (7, 12)], fill=(150, 25, 25, 255))
    d.point((5, 8), fill=(255, 140, 140, 255))
    d.rectangle([7, 2, 8, 5], fill=(60, 130, 40, 255))
    return img


def build(h):
    for k, v in TEKSTEN.items():
        h.NL[k] = v
    # the lobby's guhs: the sitting guh recoloured (stub_npc of bbq2)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    for kind, (naam, hue, sat, val) in NPCS.items():
        h.save(h.recolour(src, hue=hue, sat=sat, val=val, only=h.pinkish), "entity", f"npc_{kind}.png")
        h.NL[f"entity.guhs.guh_npc.{kind}"] = naam
    # items
    h.save(_komkommer(), "item", "safari_komkommer.png")
    h.item_model("safari_komkommer")
    h.save(_paprika(), "item", "safari_paprika.png")
    h.item_model("safari_paprika")
    for z in ZONES:
        h.save(_badge(ZONE_KLEUR[z][1]), "item", f"safari_badge_{z}.png")
        h.item_model(f"safari_badge_{z}")
    h.w(f"{h.D}/safari/winkel.json", WINKEL)


# --- direct mode -----------------------------------------------------------------------------------------------------
# python tools/features/safari_lobby.py --direct   (from the project root): writes only this slice's files without the
# full make_resources run: the Dutch texts into nl_nl.json, the English overlay (tools/lang/en/c161_safari_lobby.json) into
# en_us.json, the NPC skins, the item textures, models and 26.1 item definitions, and winkel.json.
def _direct():
    import sys
    import numpy as np  # noqa: F401  (recolour)
    sys.path.insert(0, "tools")
    R = os.path.join("src", "main", "resources")
    A = os.path.join(R, "assets", "guhs")

    class H:
        NL = {}
        TEX = os.path.join(A, "textures")
        D = os.path.join(R, "data", "guhs")

        @staticmethod
        def w(path, obj):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                json.dump(obj, f, indent=2, ensure_ascii=False)
                f.write("\n")

        @staticmethod
        def save(img, *path):
            full = os.path.join(H.TEX, *path)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            img.save(full)

        @staticmethod
        def item_model(name):
            H.w(os.path.join(A, "models", "item", f"{name}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{name}"}})
            H.w(os.path.join(A, "items", f"{name}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})

    import make_v2 as _v2_helpers  # noqa: F401  only for recolour/pinkish (cheap module functions)
    H.recolour = staticmethod(_v2_helpers.recolour)
    H.pinkish = staticmethod(_v2_helpers.pinkish)
    build(H)
    with open(os.path.join("tools", "lang", "en", "c161_safari_lobby.json"), encoding="utf-8") as f:
        en = json.load(f)
    for taal, extra in (("nl_nl", H.NL), ("en_us", {k: en.get(k, v) for k, v in H.NL.items()})):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(extra)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"safari_lobby: {len(H.NL)} texts, {len(NPCS)} skins, {2 + len(ZONES)} items")


if __name__ == "__main__" and "--direct" in __import__("sys").argv:
    _direct()
