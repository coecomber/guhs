"""
Hartjes voor je guh (2.10 "Lieve vadsjes van elkaar", fundament; see guhs_work210/CONTRACT_210.md par. 3-5, 7).

  - particles band_hartje (a little pink heart) and band_groot_hartje (one big heart: a level-up)
  - sounds band.hartjes, band.niveau
  - tags: item guhs:band/snacks (feed your own guh for hearts), guhs:band/favoriet_eten (the favourite-food candidates,
    a subset of the snacks), biome guhs:band/favoriete_plekken
  - the advancement tab guhs:lieve_vadsjes (root + the three levels + the dagboekje), hidden quest advancements
  - the texts of the band (levels, hearts, dagboekje, "waar is mijn guh", the Guhdex tab Mijn guhs, the menu buttons) and
    the shared 2.10 texts other slices build on (chore names + tips, toy names, favourite kinds and colours, the eerste
    keren); later modules may override any key by writing it again
  - the FTB quests of the section "Hartjes voor je guh" and the game test room
"""
import os

import numpy as np
from PIL import Image

# =====================================================================================================================
# particles
# =====================================================================================================================
HART = ["..XX.XX..",
        ".XHHXXXX.",
        "XHHXXXXXX",
        "XHXXXXXXX",
        "XXXXXXXXX",
        ".XXXXXXX.",
        "..XXXXX..",
        "...XXX...",
        "....X...."]


def _hartje(kleur, rand, glans, schaal=1, marge=0):
    h, w = len(HART), len(HART[0])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    x0, y0 = (16 - w) // 2, (16 - h) // 2
    for y, row in enumerate(HART):
        for x, c in enumerate(row):
            if c == ".":
                continue
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < w and 0 <= ny < h) or HART[ny][nx] == ".":
                    img.putpixel((x0 + nx, y0 + ny), rand + (255,))
    for y, row in enumerate(HART):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x0 + x, y0 + y), (glans if c == "H" else kleur) + (255,))
    return img


def _groot_hartje():
    """A big heart (32x32) with a soft rim and two sparkles."""
    size = 32
    a = np.zeros((size, size, 4), np.uint8)
    ys, xs = np.mgrid[0:size, 0:size]
    x = (xs - 15.5) / 12.0
    y = (15.5 - ys) / 11.5
    f = (x ** 2 + y ** 2 - 1) ** 3 - x ** 2 * y ** 3
    binnen = f <= 0
    rand = np.zeros_like(binnen)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            rand |= np.roll(np.roll(binnen, dy, 0), dx, 1)
    rand &= ~binnen
    grad = np.clip((ys - 4) / 24.0, 0, 1)[..., None]
    top, onder = np.array((255, 190, 225)), np.array((236, 80, 150))
    kleur = (top * (1 - grad) + onder * grad).astype(np.uint8)
    a[binnen, :3] = kleur[binnen]
    a[binnen, 3] = 255
    a[rand] = (140, 30, 80, 255)
    for (cx, cy) in ((9, 8), (10, 9), (8, 9)):
        a[cy, cx] = (255, 250, 255, 255)
    for (sx, sy) in ((27, 4), (4, 24)):
        for d in (-2, -1, 0, 1, 2):
            a[sy, sx + d] = (255, 244, 180, 255)
            a[sy + d, sx] = (255, 244, 180, 255)
    return Image.fromarray(a)


def particles(h):
    for i, (kleur, rand, glans) in enumerate([((246, 120, 176), (150, 40, 90), (255, 214, 236)),
                                              ((255, 150, 196), (170, 50, 104), (255, 232, 244)),
                                              ((236, 90, 150), (130, 30, 76), (255, 200, 226))]):
        h.save(_hartje(kleur, rand, glans), "particle", f"band_hartje_{i}.png")
    h.w(f"{h.A}/particles/band_hartje.json", {"textures": [f"guhs:band_hartje_{i}" for i in range(3)]})
    h.save(_groot_hartje(), "particle", "band_groot_hartje.png")
    h.w(f"{h.A}/particles/band_groot_hartje.json", {"textures": ["guhs:band_groot_hartje"]})


# =====================================================================================================================
# sounds, tags
# =====================================================================================================================
SOUNDS = {
    "band.hartjes": [{"name": "minecraft:block.amethyst_block.chime", "type": "event", "pitch": 1.5, "volume": 0.8},
                     {"name": "minecraft:entity.cat.purr", "type": "event", "pitch": 1.4, "volume": 0.9}],
    "band.niveau": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 1.35, "volume": 0.9}],
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


BAKJES = ["knabbelbroodje", "kaaskrakeling", "vadsvlaai", "guhcroissant", "knabbelkoekje", "kaasbolletje", "pluismuffin", "theetaartje",
          "knabbeltompouce", "vadsdonut", "guhwafel", "sterrenkoekje"]
IJSJES = ["appeltaart", "bloesem", "choco", "mint", "roze", "sneeuw", "zonnetje"]
SNACKS = (["guhs:kaas_knabbels", "guhs:gefrituurde_kaasknabbels", "guhs:guh_cupcake", "guhs:kaasfondue", "guhs:kaasknabbel_milkshake",
           "guhs:gebakken_guh_vis", "guhs:kaasknabbelsate", "guhs:gegrilde_kaasknabbelsate", "guhs:knabbelbessen", "guhs:knabbelbessentaartje",
           "guhs:marshmallow_knabbel", "guhs:wolkensuikerspin", "guhs:kaasbrok", "guhs:moeraskaas"]
          + [f"guhs:macaron_{c}" for c in ("roze", "mint", "citroen", "choco")]
          + [f"guhs:{b}" for b in BAKJES] + [f"guhs:kaasijsje_{s}" for s in IJSJES]
          + ["minecraft:cookie", "minecraft:sweet_berries", "minecraft:melon_slice", "minecraft:apple", "minecraft:pumpkin_pie"])
FAVORIET_ETEN = ["guhs:gefrituurde_kaasknabbels", "guhs:guh_cupcake", "guhs:kaasknabbel_milkshake", "guhs:macaron_roze", "guhs:macaron_mint",
                 "guhs:knabbelbessentaartje", "guhs:marshmallow_knabbel", "guhs:wolkensuikerspin", "guhs:kaasijsje_roze", "guhs:kaasijsje_mint",
                 "guhs:knabbelbroodje", "guhs:vadsvlaai", "guhs:guhwafel", "guhs:vadsdonut", "guhs:gegrilde_kaasknabbelsate",
                 "minecraft:sweet_berries", "minecraft:cookie", "minecraft:melon_slice"]
FAVORIETE_PLEKKEN = (["guhs:" + b for b in ("guh_fields", "knabbel_crumbs", "pink_puffs", "kaas_flats", "guh_meadows", "guh_peaks", "vads_cliffs",
                                            "vadswoud", "guhpolder", "knuffeldal")]
                     + ["minecraft:" + b for b in ("flower_forest", "cherry_grove", "meadow", "plains", "sunflower_plains", "beach")])


def tags(h):
    missing = [i for i in SNACKS if i.startswith("guhs:") and not os.path.exists(f"{h.A}/models/item/{i[5:]}.json")]
    if missing:
        raise SystemExit(f"band: snacks without an item: {missing}")
    assert set(FAVORIET_ETEN) <= set(SNACKS) and len(FAVORIET_ETEN) >= 10
    h.add_tag("guhs/tags/item/band/snacks", SNACKS)
    h.add_tag("guhs/tags/item/band/favoriet_eten", FAVORIET_ETEN)
    for b in FAVORIETE_PLEKKEN:
        if b.startswith("guhs:") and not os.path.exists(f"{h.D}/worldgen/biome/{b[5:]}.json"):
            raise SystemExit(f"band: no biome {b}")
    h.add_tag("guhs/tags/worldgen/biome/band/favoriete_plekken", FAVORIETE_PLEKKEN)


# =====================================================================================================================
# advancements (tab guhs:lieve_vadsjes)
# =====================================================================================================================
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
QUEST_ADVANCEMENTS = ["band_gevoerd", "band_geaaid", "band_geknuffeld"]


def visible(h, name, parent, icon, frame, title, desc, hidden=False):
    adv = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.lieve_vadsjes.{name}.title"},
                       "description": {"translate": f"advancements.guhs.lieve_vadsjes.{name}.description"},
                       "frame": frame, "show_toast": True, "announce_to_chat": frame != "task", "hidden": hidden},
           "criteria": IMPOSSIBLE}
    if parent:
        adv["parent"] = f"guhs:lieve_vadsjes/{parent}"
    else:
        adv["display"]["background"] = "minecraft:textures/block/pink_wool.png"
        adv["display"]["announce_to_chat"] = False
    h.w(f"{h.D}/advancement/lieve_vadsjes/{name}.json", adv)
    h.lang(f"advancements.guhs.lieve_vadsjes.{name}.title", title, title)
    h.lang(f"advancements.guhs.lieve_vadsjes.{name}.description", desc, desc)


def advancements(h):
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    visible(h, "root", None, "guhs:guhhuisje_klein", "task", "Lieve vadsjes van elkaar",
            "Tem een guh: jullie hartjesmeter begint te lopen. Hartjes gaan nooit omlaag!")
    visible(h, "band_lief", "root", "minecraft:pink_dye", "task", "Lieve vadsjes van elkaar",
            "Word met een guh lieve vadsjes van elkaar (100 hartjes)")
    visible(h, "band_mega", "band_lief", "minecraft:red_dye", "goal", "Mega lieve vadsjes van elkaar",
            "Word met een guh mega lieve vadsjes van elkaar (600 hartjes)")
    visible(h, "band_zielsguh", "band_mega", "minecraft:nether_star", "challenge", "Zielsguh bff 5evr <3",
            "Word met een guh zielsguh bff 5evr <3 (2000 hartjes). VAHOEG!")
    visible(h, "band_dagboekje", "root", "minecraft:writable_book", "task", "Lief dagboekje",
            "Open het dagboekje van je guh (houd rechtsklik op je guh, knop Dagboekje)")


# =====================================================================================================================
# texts
# =====================================================================================================================
NIVEAUS = {
    "geen": "op weg naar lieve vadsjes...",
    "lief": "lieve vadsjes van elkaar",
    "mega": "mega lieve vadsjes van elkaar",
    "zielsguh": "zielsguh bff 5evr <3",
}
PLEKKEN = {  # %1$s detail, %2$s dimension, %3$s %4$s %5$s x y z
    "wereld": "Loopt lekker rond (%2$s, %3$s %4$s %5$s)",
    "zit": "Zit braaf te wachten (%2$s, %3$s %4$s %5$s)",
    "huisje": "Woont in Guhhuisje %1$s (%2$s, %3$s %4$s %5$s)",
    "slaapt_in_huisje": "Ligt te snurken in Guhhuisje %1$s (%2$s, %3$s %4$s %5$s). Zzz...",
    "rijdt_op": "%1$s zit bovenop (%2$s, %3$s %4$s %5$s). Hup hup!",
    "rijdt_mee": "Rijdt mee op %1$s (%2$s, %3$s %4$s %5$s)",
    "item_speler": "Zit in de zakken van %1$s (%2$s, %3$s %4$s %5$s). Knus!",
    "item_kist": "Ligt in een kist: %1$s (%2$s, %3$s %4$s %5$s)",
    "item_rugzak": "Zit in de rugzak van %1$s (%2$s, %3$s %4$s %5$s)",
    "item_bank": "Ligt te dutten in een Bank Guh (%2$s, %3$s %4$s %5$s)",
    "item_grond": "Ligt als pakketje op de grond (%2$s, %3$s %4$s %5$s). Njeg, raap hem op!",
    "guhwiel": "Rent rondjes in een Guh Wheel (%2$s, %3$s %4$s %5$s). VAHOEG!",
    "guhkamer": "Logeert in de Guhkamer in je Guhmaag",
    "schouder": "Zit op de schouder van %1$s (%2$s, %3$s %4$s %5$s)",
    "in_guh": "Zit in guh %1$s voor een poetsbeurt (%2$s, %3$s %4$s %5$s)",
    "onbekend": "Geen idee, njeg... Kom maar eens langs, dan onthoudt je Guhdex het!",
    "bij_jou": "Gezellig bij jou! (%2$s, %3$s %4$s %5$s)",  # 1.2.5: just called over ("Roep naar mij")
}
DIMENSIES = {
    "minecraft.overworld": "Bovenwereld", "minecraft.the_nether": "de Nether", "minecraft.the_end": "het End",
    "guhs.guhmension": "de Guhmensie", "guhs.guhmaag": "je Guhmaag", "guhs.guheinde": "het Guheinde",
    "guhs.barbecuether": "de Guhbarbecuether",
}
STATS = {
    "blokken_samen": "Blokken samen gereisd", "knuffels": "Knuffels", "knabbels_gegeten": "Hapjes gegeten",
    "minigames_samen": "Minigames samen", "knabbels_opgegraven": "Knabbels opgegraven", "dagen_samen": "Dagen samen",
    "klusjes": "Klusjes gedaan", "speeltjes": "Keer gespeeld",
}
# the eerste keren: (title, the funny line the guh writes)
EERSTE = {
    "getemd": ("Getemd!", "Vandaag kreeg ik een baasje. Ze gaf me een kaasknabbel en toen was ik om. Njeg, zo makkelijk ben ik."),
    "eerste_aai": ("Eerste aai", "Er ging een hand over mijn vachtje. Ik ben er nog steeds een beetje verlegen van. Nog een keer?"),
    "eerste_knuffel": ("Eerste dikke knuffel", "Mijn baasje knuffelde me zo hard dat mijn vads ervan wiebelde. VAHOEG!"),
    "eerste_rit": ("Eerste ritje", "Iemand ging op mijn rug zitten. Ik ben blijkbaar een paard nu. Een heel vadsig paard."),
    "eerste_guhmension": ("Naar de Guhmensie", "Alles is hier roze! Het gras, de wolken, ik. Ik voel me helemaal thuis."),
    "eerste_guheinde": ("In het Guheinde", "Het is hier donker en een beetje eng, njeg. Gelukkig is mijn baasje er ook."),
    "eerste_huisje": ("Een eigen huisje!", "Ik heb een huisje dat op mijn eigen hoofd lijkt. Mijn oren zijn het dak. Heel logisch."),
    "eerste_lief": ("Lieve vadsjes van elkaar", "Wij zijn nu officieel lieve vadsjes van elkaar. Ik heb het in mijn dagboekje geschreven, dus het is echt."),
    "eerste_mega": ("Mega lieve vadsjes", "Mega lieve vadsjes van elkaar! Mijn hartje is zo groot dat het bijna niet meer in mijn vads past."),
    "eerste_zielsguh": ("Zielsguh bff 5evr <3", "Zielsguh bff 5evr <3. Voor altijd. Dit gaat nooit meer weg. Nooit. Njeg njeg njeg."),
    "eerste_klusje": ("Eerste klusje", "Ik heb gewerkt! Echt gewerkt! Ik moet nu wel even uitrusten, een klein dutje of drie."),
    "eerste_speeltje": ("Eerste keer gespeeld", "Speelgoed! Ik wist niet dat dat bestond. Mijn leven is nu compleet."),
    "eerste_guhkamer": ("In de Guhkamer", "Ik logeer in de maag van mijn baasje. Klinkt raar, maar het is er heel gezellig."),
    "eerste_minigame": ("Samen gespeeld", "We deden samen een spelletje. Ik juichte zo hard dat mijn oortjes flapperden."),
    "eerste_vriendje": ("Een vriendje!", "Ik heb een vriendje gemaakt. We zijn nu vadsjes van elkaar. Niet zo vads als met mijn baasje, hoor."),
    "eerste_favoriet": ("Een favorietje ontdekt", "Mijn baasje weet nu een geheimpje van mij. Shh, niet verder vertellen, njeg."),
}
WISTJEDAT = {  # gui.guhs.wistjedat.band.<id>
    "getemd": "Vandaag heb ik %s als baasje gekozen. Goede keuze, al zeg ik het zelf, njeg.",
    "geboren": "Vandaag ben ik geboren! Ik ben nog heel klein, maar mijn vads groeit elke dag.",
    "eerste_knuffel": "Wist je dat een knuffel precies drie kaasknabbels waard is? Dat heb ik net uitgerekend.",
    "eerste_guhmension": "Wist je dat de lucht in de Guhmensie naar suikerspin ruikt? Ik heb het gecontroleerd.",
    "eerste_guheinde": "Vandaag was ik in het Guheinde. Ik was niet bang. Een klein beetje. Njeg.",
    "eerste_huisje": "Mijn huisje heet %s. Ik vind het de mooiste naam van de hele wereld.",
    "niveau_lief": "Vandaag ben ik zo VAHOEG geworden: %s en ik zijn lieve vadsjes van elkaar, njeg!",
    "niveau_mega": "%s en ik zijn nu MEGA lieve vadsjes van elkaar. Ik ben nog nooit zo vahoeg geweest.",
    "niveau_zielsguh": "%s is mijn zielsguh bff 5evr <3. Ik heb een hartje in mijn vachtje gedrukt, voor altijd.",
}
FAVORIET_SOORTEN = {"eten": "Lievelingshapje", "plek": "Lievelingsplek", "knuffel": "Lievelingsknuffel", "liedje": "Lievelingsliedje",
                    "speeltje": "Lievelingsspeeltje", "emote": "Lievelingsemote", "kleur": "Lievelingskleur", "vriend": "Beste guh-vriendje"}
KLEUREN = {"roze": "Roze", "rood": "Rood", "oranje": "Oranje", "geel": "Geel", "groen": "Groen", "mint": "Mint", "blauw": "Blauw",
           "paars": "Paars", "wit": "Wit", "zwart": "Zwart", "bruin": "Bruin", "goud": "Goud"}
# the ten chores (CONTRACT par. 5.4; the klusjes slice may polish them)
KLUSSEN = {
    "opgraven": ("Kaasknabbels opgraven", "Graaft kaasknabbels op in zand, aarde of gras rond het huisje. Soms iets zeldzaams, njeg!"),
    "farmen": ("Farmen", "Oogst rijpe gewassen en plant ze opnieuw: tarwe, wortels, kaasknabbelplantjes en guhtuintjes in de buurt."),
    "opruimen": ("Opruimen & sorteren", "Raapt spulletjes van de grond op en stopt ze in de kist. Met een Bank Guh erbij wordt alles gesorteerd."),
    "dieren": ("Dieren & bijen verzorgen", "Voert guhschaapjes, knabbelkippetjes en guhkoeien en haalt hun spulletjes op. Oogst volle knabbelkorven."),
    "bakken": ("Bakken & molen", "Maalt knabbelgraan in een guh-molentje en bakt in een knabbeloven. Zet ze vlak bij het huisje!"),
    "vissen": ("Vissen", "Vist guhvissen en schelpjes als er water bij het huisje is."),
    "waken": ("Wachten & waarschuwen", "Piept en rent naar jou als er een Mika of monster komt, en duwt Mika's heel zachtjes weg. Nooit vechten!"),
    "plukken": ("Bloemetjes & bessen plukken", "Plukt guhbloemen, knabbelbessen en zoete bessen, en plant soms een nieuw bloemetje."),
    "lampjes": ("Lampjes aan & uit", "Doet 's avonds de guhlampjes rond het huisje aan en 's ochtends weer uit (met een gaap)."),
    "oppas": ("Muisje-oppas & verzorgen", "Zorgt voor pieppiepmuisjes en schildpadjes in de buurt, en geeft een gewonde guh een snackje."),
}
SPEELTJES = {"knabbelbal": "Knabbelbal", "glijbaantje": "Guh-glijbaantje & klimrek", "tunnel": "Pluizige tunnel", "wip_schommel": "Wip & schommel"}

TEXTS = {
    # the levels, hearts, level-ups
    "gui.guhs.band.hartjes_erbij": "+%s ♥ %s  %s",
    # 1.2.0: a tap is only petting (BandEvents.aai): always this line, the first 3 times with the menu hint
    "gui.guhs.band.aai": "Je aait %s! ♥",
    "gui.guhs.band.aai_tip": "(Houd rechtsklik ingedrukt voor het guhmenu)",
    "gui.guhs.band.aai_hartjes": "+%s ♥ %s",
    "gui.guhs.band.hartjes_voortgang": "(%s / %s)",
    "gui.guhs.band.hartjes_max": "(%s: zielsguh!)",
    "gui.guhs.band.niveau_omhoog": "♥ %s en jij zijn nu %s! VAHOEG!",
    "gui.guhs.band.niveau_titel": "♥ %s ♥",
    "gui.guhs.band.knuffel_rust": "%s is nog helemaal warm van de vorige knuffel. Straks weer, njeg!",
    "gui.guhs.band.knuffel_nu_niet": "%s kan nu even niet knuffelen (hij is druk bezig).",
    "subtitles.guhs.band.hartjes": "Hartjes",
    "subtitles.guhs.band.niveau": "Hartjesmeter omhoog",
    # the dagboekje
    "gui.guhs.dagboek.nieuw": "✎ Nieuw in het dagboekje van %s: %s",
    # the menu
    "gui.guhs.menu.knuffelen": "Knuffelen!",
    "gui.guhs.menu.knuffelen.tooltip": "Geef je guh een dikke vadsige knuffel. Dat geeft hartjes! (Daarna moet hij even bijkomen.)",
    "gui.guhs.menu.dagboekje": "Dagboekje",
    "gui.guhs.menu.dagboekje.tooltip": "Open het dagboekje van je guh in de Guhdex: hartjes, favorietjes, vriendjes, waar hij is en wat hij "
                                       "allemaal heeft meegemaakt.",
    # the Guhdex tab Mijn guhs
    "gui.guhs.guhdex.tab.mijn_guhs": "Mijn guhs",
    "gui.guhs.mijnguhs.leeg": "Je hebt nog geen tamme guhs, njeg! Tem er een met kaasknabbels en hier verschijnt zijn dagboekje.",
    "gui.guhs.mijnguhs.terug": "< Alle guhs",
    "gui.guhs.mijnguhs.draai": "sleep om te draaien",
    "gui.guhs.mijnguhs.hartjes": "%s / %s hartjes",
    "gui.guhs.mijnguhs.hartjes_max": "%s hartjes. Zielsguh!",
    "gui.guhs.mijnguhs.open_tip": "Klik voor het dagboekje",
    # 1.2.5: "Roep naar mij" on a guh's page
    "gui.guhs.mijnguhs.roep": "Roep naar mij",
    "gui.guhs.mijnguhs.roep.tip": "Roep %s naar je toe, waar hij ook is (ook in een andere dimensie). Hij komt uit zijn Guhhuisje of de Guhkamer en loopt weer gezellig met je mee.",
    "gui.guhs.mijnguhs.roep.opgepakt": "%s zit opgepakt in een kist, rugzak of zakken - haal hem zelf op!",
    "gui.guhs.mijnguhs.roep.guhwiel": "%s rent rondjes in een Guh Wheel - haal hem zelf op!",
    "gui.guhs.mijnguhs.roep.dood": "%s is in de wolkjes... Roepen helpt niet, maar het Knuffelhart wel.",
    "gui.guhs.mijnguhs.roep.komt": "%s komt eraan! VAHOEG!",
    "gui.guhs.mijnguhs.roep.zoeken": "%s wordt opgehaald... even geduld!",
    "gui.guhs.mijnguhs.roep.kwijt": "Njeg, %s is nergens te vinden. Ga eens kijken waar hij het laatst was!",
    "gui.guhs.mijnguhs.roep.niet_jouw": "Dat is niet jouw guh, njeg!",
    "gui.guhs.mijnguhs.kop.favorietjes": "Favorietjes",
    "gui.guhs.mijnguhs.favorietjes_hint": "Nog geen favorietjes ontdekt. Probeer eens van alles: eten, plekjes, liedjes, speeltjes... Je guh "
                                          "laat het wel merken!",
    "gui.guhs.mijnguhs.kop.vriendjes": "Vriendjes",
    "gui.guhs.mijnguhs.geen_vriendjes": "Nog geen guh-vriendjes. Laat hem eens spelen met je andere guhs!",
    "gui.guhs.mijnguhs.bestie": "★ %s (beste vriendjes!)",
    "gui.guhs.mijnguhs.kop.klusjes": "Huisje & klusjes",
    "gui.guhs.mijnguhs.geen_huisje": "Woont (nog) niet in een Guhhuisje.",
    "gui.guhs.mijnguhs.woont_in": "Woont in Guhhuisje %s",
    "gui.guhs.mijnguhs.geen_klusjes": "Doet (nog) geen klusjes.",
    "gui.guhs.mijnguhs.kop.waar": "Waar is %s?",
    "gui.guhs.mijnguhs.kop.statistieken": "Statistieken",
    "gui.guhs.mijnguhs.sinds": "Samen sinds",
    "gui.guhs.mijnguhs.dagen": "%s dagen",
    "gui.guhs.mijnguhs.kop.eerste": "Eerste keren",
    "gui.guhs.mijnguhs.nog_niks": "Nog niks... maar dat komt vast, njeg!",
    "gui.guhs.mijnguhs.dag": "dag %s",
    "gui.guhs.mijnguhs.kop.wistjedat": "Wist-je-datjes",
    # the clothing source of the band pieces (samen)
    "gui.guhs.kledingbron.band": "Hartjes met je guh",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)
    for k, v in NIVEAUS.items():
        h.lang(f"gui.guhs.band.niveau.{k}", v, v)
    for k, v in PLEKKEN.items():
        h.lang(f"gui.guhs.band.plek.{k}", v, v)
    for k, v in DIMENSIES.items():
        h.lang(f"gui.guhs.band.dim.{k}", v, v)
    for k, v in STATS.items():
        h.lang(f"gui.guhs.dagboek.stat.{k}", v, v)
    for k, (titel, tekst) in EERSTE.items():
        h.lang(f"gui.guhs.dagboek.eerste.{k}", titel, titel)
        h.lang(f"gui.guhs.dagboek.eerste.{k}.tekst", tekst, tekst)
    for k, v in WISTJEDAT.items():
        h.lang(f"gui.guhs.wistjedat.band.{k}", v, v)
    for k, v in FAVORIET_SOORTEN.items():
        h.lang(f"gui.guhs.favoriet.soort.{k}", v, v)
    for k, v in KLEUREN.items():
        h.lang(f"gui.guhs.favoriet.kleur.{k}", v, v)
    for k, (naam, tip) in KLUSSEN.items():
        h.lang(f"gui.guhs.klus.{k}", naam, naam)
        h.lang(f"gui.guhs.klus.{k}.tip", tip, tip)
    for k, v in SPEELTJES.items():
        h.lang(f"gui.guhs.speeltje.{k}", v, v)


# =====================================================================================================================
# the game test room
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((12, 5, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("band_test_wei")


def selfcheck(h):
    missing = []
    for p in (f"{h.A}/particles/band_hartje.json", f"{h.A}/particles/band_groot_hartje.json",
              os.path.join(h.TEX, "particle", "band_groot_hartje.png")):
        if not os.path.exists(p):
            missing.append(p)
    for k in ("gui.guhs.guhdex.tab.mijn_guhs", "gui.guhs.kledingbron.band", "gui.guhs.band.niveau.zielsguh"):
        if k not in h.NL:
            missing.append(k)
    if missing:
        raise SystemExit(f"band: missing {missing}")


def build(h):
    particles(h)
    sounds(h)
    tags(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (section "Hartjes voor je guh" of the chapter guhs_band)
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    q("band_aaien", "Aai aai", "Tik je tamme guh kort aan met rechtsklik: een aaitje! Hij wordt even helemaal plat van geluk, "
      "duwt zijn kopje tegen je hand en er komen hartjes. Elk aaitje is een &dhartje&r waard (maar niet oneindig veel per dag, "
      "anders wordt hij verwend, njeg). Zitten of opstaan doe je in het guhmenu: houd rechtsklik ingedrukt.", "minecraft:pink_dye", [adv("band_geaaid")], shape="circle")
    q("band_voeren", "Een hapje voor je guh", "Geef je eigen guh een snackje: een cupcake, een macaron, een ijsje, een koekje, zoete bessen... "
      "Hij smakt het op en jullie krijgen er &dhartjes&r bij. Misschien vind je zo zelfs zijn lievelingshapje!",
      "guhs:guh_cupcake", [adv("band_gevoerd")], rewards=(("guhs:guh_cupcake", 2),))
    q("band_knuffel", "Een dikke knuffel", "Houd rechtsklik ingedrukt op je guh en kies &dKnuffelen!&r. Een dikke vadsige knuffel is veel hartjes "
      "waard. Daarna moet je guh wel even bijkomen, zo warm is hij ervan.", "minecraft:red_dye", [adv("band_geknuffeld")])
    q("band_eerste_hartjes", "Lieve vadsjes van elkaar", "Verzamel &d100 hartjes&r met een guh: jullie zijn dan &dlieve vadsjes van elkaar&r! "
      "Hartjes krijg je van aaien, voeren, knuffelen en gewoon samen zijn (een minuut in de buurt = een hartje). Ze gaan nooit omlaag!",
      "minecraft:pink_dye", [adv("guhs:lieve_vadsjes/band_lief")], rewards=(("guhs:kaas_knabbels", 16),), xp=100)
    q("band_mega", "Mega lieve vadsjes", "Haal &d600 hartjes&r met een guh: &dmega lieve vadsjes van elkaar&r. Doe klusjes, speel samen en "
      "ontdek zijn favorietjes: dat gaat het snelst.", "minecraft:red_dye", [adv("guhs:lieve_vadsjes/band_mega")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), xp=250)
    q("band_zielsguh", "Zielsguh bff 5evr <3", "&62000 hartjes&r: jullie zijn &6zielsguh bff 5evr <3&r. Voor altijd, echt waar. Je guh krijgt "
      "een glinsterend hartje naast zijn naam. VAHOEG!", "minecraft:nether_star", [adv("guhs:lieve_vadsjes/band_zielsguh")],
      rewards=(("guhs:vahoege_vads_ingot", 1),), shape="gear", xp=500)
    q("band_dagboekje", "Het dagboekje", "Houd rechtsklik op je guh en kies &dDagboekje&r (of kijk in je &dGuhdex&r bij Mijn guhs). Daar staan zijn "
      "hartjes, favorietjes, vriendjes, waar hij is, zijn eerste keren en de wist-je-datjes die hij zelf opschrijft.",
      "minecraft:writable_book", [adv("guhs:lieve_vadsjes/band_dagboekje")], rewards=(("guhs:kaas_knabbels", 8),))
