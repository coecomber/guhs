"""
Guhpixel slice "parkour" (Java: feature/guhpixel/parkour; namespace guhparkour; English: tools/lang/en/c39_px_parkour.json).

Het Guh-parkour (DESIGN_PX section 6; it has nothing to do with the guhpixel dimension):
  - guhparkour_startpaaltje / guhparkour_finishpaaltje   the posts a route starts and ends at
  - guhparkour_horde, _springplank, _kruiptunnel, _slalompaaltjes, _evenwichtsbalk, _knabbeltafeltje   the six obstacles
    (the "wip" of a route is the existing guhs:guh_wip; the glijbaantje, the schommel and the pluizige tunnel are pieces too)
  - guhparkour_scorebord                                  laps and best lap time per guh
Models and textures: guhpixel_parkour_modellen.py. Here: blockstates, items, loot, recipes, sounds, the hidden quest
advancements, the texts (Dutch), the game test rooms, the FTB quests and the self-check.
"""
import os

from features import guhpixel_lib as lib
from features import guhpixel_parkour_modellen as modellen
from features import uvfix

BLOKKEN = ["guhparkour_startpaaltje", "guhparkour_finishpaaltje", "guhparkour_horde", "guhparkour_springplank", "guhparkour_kruiptunnel",
           "guhparkour_slalompaaltjes", "guhparkour_evenwichtsbalk", "guhparkour_knabbeltafeltje", "guhparkour_scorebord"]
ADVANCEMENTS = ["guhparkour_rondje", "guhparkour_groot"]
BOBBELS = 5

# id: (name, lore (pink), uitleg (grey))
NAMEN = {
    "guhparkour_startpaaltje": ("Startpaaltje", "Klaar voor de start? Af! ...na een dutje, njeg.",
                                "Klik er speelgoed en hindernissen mee aan: 1, 2, 3... tot het Finishpaaltje. Zet het daarna neer."),
    "guhparkour_finishpaaltje": ("Finishpaaltje", "Wie hier aankomt heeft een knabbel verdiend.",
                                 "Het laatste stuk van een Guh-parkour: hier stopt de tijd."),
    "guhparkour_horde": ("Guh-horde", "Hup, eroverheen! Buikje intrekken helpt niet.",
                         "Hindernis voor een Guh-parkour: je guh springt eroverheen."),
    "guhparkour_springplank": ("Guh-springplank", "Boing! Heel even vliegt een vadsje.",
                               "Hindernis: aanloop, boing, en een heel eind door de lucht. Laat erachter drie blokken vrij."),
    "guhparkour_kruiptunnel": ("Kruiptunnel", "Er gaat een guh in en er loopt een bobbel doorheen.",
                               "Hindernis van drie blokken lang: je guh kruipt er als een bobbel doorheen."),
    "guhparkour_slalompaaltjes": ("Slalompaaltjes", "Links, rechts, links... welke kant was het ook alweer, njeg?",
                                  "Hindernis van drie blokken lang: zigzag tussen de paaltjes door."),
    "guhparkour_evenwichtsbalk": ("Evenwichtsbalk", "Voorzichtig... voorzichtig... oeps!",
                                  "Hindernis van drie blokken lang: je guh schuifelt eroverheen en valt er soms af. Geeft niks: hij klimt er zo weer op."),
    "guhparkour_knabbeltafeltje": ("Knabbeltafeltje", "Verplichte pitstop. Eerst eten, dan pas verder.",
                                   "Hindernis: je guh eet hier eerst rustig een knabbel. Dat kost tijd, maar het moet, njeg."),
    "guhparkour_scorebord": ("Parkour-scorebord", "Wie is het snelste vadsje van het land?",
                             "Laat per guh de rondjes en de beste rondetijd zien van het Startpaaltje in de buurt."),
}

TEXTS = {
    "gui.guhs.guhparkour.tijd": "%1$s,%2$s s",
    "gui.guhs.guhparkour.hint.obstakel": "Dit hoort bij een Guh-parkour: klik het aan met een Startpaaltje in je hand, njeg.",
    "gui.guhs.guhparkour.hint.finish": "Het Finishpaaltje: klik het als laatste aan met je Startpaaltje.",
    "gui.guhs.guhparkour.stuk.weg": "(weg)",
    "gui.guhs.guhparkour.item.route": "Onthoudt een route van %1$s stukken",
    "gui.guhs.guhparkour.niet_van_jou": "Dit Startpaaltje is van %1$s. Kijken mag, njeg.",
    # laying out
    "gui.guhs.guhparkour.uitzet.erbij": "Stuk %1$s: %2$s",
    "gui.guhs.guhparkour.uitzet.finish": "Finish erbij! De route telt %1$s stukken. Vahoeg!",
    "gui.guhs.guhparkour.uitzet.eruit": "%1$s is uit de route gehaald",
    "gui.guhs.guhparkour.uitzet.vol": "De route zit vol: meer dan %1$s stukken onthoudt een guh niet, njeg.",
    "gui.guhs.guhparkour.uitzet.te_ver": "Dat is verder dan %1$s blokken van de start. Zo ver draaft een guh niet, njeg.",
    "gui.guhs.guhparkour.uitzet.geplaatst": "Startpaaltje staat! Route van %1$s stukken. Rechtsklik om er guhs op te zetten.",
    "gui.guhs.guhparkour.uitzet.geplaatst_te_ver": "Startpaaltje staat, met %1$s stukken. Er lagen er %2$s te ver weg: die doen niet mee.",
    "gui.guhs.guhparkour.uitzet.leeg_geplaatst": "Startpaaltje staat! Sluip + rechtsklik en klik dan de stukken aan: 1, 2, 3...",
    "gui.guhs.guhparkour.uitzet.modus_aan": "Route uitzetten: klik de stukken aan in volgorde (1, 2, 3...) en als laatste het Finishpaaltje. "
                                            "Nog een keer klikken haalt een stuk eruit. Klik op het Startpaaltje als je klaar bent.",
    "gui.guhs.guhparkour.uitzet.modus_uit": "Klaar met uitzetten: de route telt %1$s stukken.",
    # laps
    "gui.guhs.guhparkour.rondje": "%1$s: rondje %2$s in %3$s",
    "gui.guhs.guhparkour.rondje.record": "%1$s: rondje %2$s in %3$s. Nieuw record, vahoeg!",
    # the scorebord
    "gui.guhs.guhparkour.bord.geen_paal": "Dit scorebord hoort nog nergens bij: zet het binnen 32 blokken van een Startpaaltje.",
    "gui.guhs.guhparkour.bord.gekoppeld": "Scorebord gekoppeld aan het Startpaaltje.",
    "gui.guhs.guhparkour.bord.leeg": "Nog geen rondjes gelopen. De guhs zijn vast nog aan het vadsen.",
    "gui.guhs.guhparkour.bord.kop": "Guh-parkour: de stand",
    "gui.guhs.guhparkour.bord.rij": "%1$s. %2$s - beste %3$s, %4$s rondjes (laatste %5$s)",
    "gui.guhs.guhparkour.bord.titel": "GUH-PARKOUR",
    "gui.guhs.guhparkour.bord.los_1": "Zet me bij een",
    "gui.guhs.guhparkour.bord.los_2": "Startpaaltje, njeg",
    "gui.guhs.guhparkour.bord.leeg_1": "Nog geen rondjes.",
    "gui.guhs.guhparkour.bord.leeg_2": "Iedereen vadst nog.",
    "gui.guhs.guhparkour.bord.score": "%1$s  x%2$s",
    "gui.guhs.guhparkour.bord.meer": "en nog %1$s guhs",
    # the post screen
    "gui.guhs.guhparkour.scherm.titel": "Guh-parkour",
    "gui.guhs.guhparkour.scherm.onder.klaar": "Guhs lopen hun rondjes tot je ze eraf haalt",
    "gui.guhs.guhparkour.scherm.onder.geen_finish": "Nog geen Finishpaaltje: de tijd stopt nu bij het laatste stuk",
    "gui.guhs.guhparkour.scherm.onder.van": "Het parkour van %1$s (kijken mag)",
    "gui.guhs.guhparkour.scherm.kop.route": "Route (%1$s/%2$s)",
    "gui.guhs.guhparkour.scherm.kop.guhs": "Guhs op de route (%1$s/%2$s)",
    "gui.guhs.guhparkour.scherm.leeg": "Nog geen stukken. Klik op 'Stukken aanklikken' en klik dan in de wereld speelgoed en hindernissen aan: "
                                       "1, 2, 3... en als laatste het Finishpaaltje. Het glijbaantje, de wip, de schommel en de pluizige tunnel "
                                       "doen ook mee!",
    "gui.guhs.guhparkour.scherm.geen_guhs": "Kies hierboven een guh en zet hem op de route.",
    "gui.guhs.guhparkour.scherm.nog_geen_rondje": "nog geen rondje",
    "gui.guhs.guhparkour.scherm.guh_onbekend": "Een guh",
    "gui.guhs.guhparkour.scherm.knop.op": "Zet op de route",
    "gui.guhs.guhparkour.scherm.knop.af": "Haal van de route",
    "gui.guhs.guhparkour.scherm.knop.uitzetten": "Stukken aanklikken",
    "gui.guhs.guhparkour.scherm.knop.wis": "Scores wissen",
    "gui.guhs.guhparkour.scherm.tip.knop_0": "Eerder in de route",
    "gui.guhs.guhparkour.scherm.tip.knop_1": "Later in de route",
    "gui.guhs.guhparkour.scherm.tip.knop_2": "Uit de route halen",
    "gui.guhs.guhparkour.scherm.tip.weg": "Dit stuk is weg. Guhs slaan het over; haal het uit de route.",
    "gui.guhs.guhparkour.scherm.tip.hapert": "Een guh kon hier niet bij komen: dat rondje telde niet. Staat er iets in de weg?",
    "gui.guhs.guhparkour.scherm.tip.finish": "Het Finishpaaltje blijft altijd het laatste stuk.",
    "gui.guhs.guhparkour.scherm.uit.ver": "Niet in de buurt van het Startpaaltje",
    "gui.guhs.guhparkour.scherm.uit.andere_route": "Loopt al een ander parkour",
    "gui.guhs.guhparkour.scherm.melding.op": "%1$s draaft naar de start. Hup, vadsje!",
    "gui.guhs.guhparkour.scherm.melding.op_leeg": "%1$s staat klaar... maar er is nog geen route, njeg.",
    "gui.guhs.guhparkour.scherm.melding.af": "%1$s mag uitrusten.",
    "gui.guhs.guhparkour.scherm.melding.al_op": "Die guh loopt hier al.",
    "gui.guhs.guhparkour.scherm.melding.vol": "Er passen maar %1$s guhs tegelijk op een parkour.",
    "gui.guhs.guhparkour.scherm.melding.gewist": "Alle scores zijn gewist. Schone lei!",
    # the Guhdex section
    "gui.guhs.guhparkour.gids.kop": "Guh-parkour",
    "gui.guhs.guhparkour.gids.uitleg": "Zet met een Startpaaltje een route uit langs speelgoed en hindernissen en laat je guhs rondjes lopen.",
    "gui.guhs.guhparkour.gids.routes": "Routes gebouwd",
    "gui.guhs.guhparkour.gids.rondjes": "Rondjes gelopen",
    "gui.guhs.guhparkour.gids.langste": "Langste route (stukken)",
    "gui.guhs.guhparkour.gids.nog_niets": "Nog geen guh heeft een rondje gelopen. Hup, njeg!",
    "gui.guhs.guhparkour.gids.beste": "De snelste rondjes van je guhs:",
    "gui.guhs.guhparkour.gids.guh": "%1$s (%2$s rondjes)",
}

SOUNDS = {
    "guhparkour.klik": [{"name": "minecraft:block.note_block.pling", "type": "event", "volume": 0.6}],
    "guhparkour.start": [{"name": "guhs:guh_ambient5", "pitch": 1.35}, {"name": "guhs:guh_ambient9", "pitch": 1.4}],
    "guhparkour.finish": [{"name": "guhs:guh_ambient3", "pitch": 1.4}, {"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.4}],
    "guhparkour.record": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 1.7, "volume": 0.5}],
    "guhparkour.hup": [{"name": "guhs:guh_ambient9", "pitch": 1.7}, {"name": "guhs:guh_ambient12", "pitch": 1.6}],
    "guhparkour.boing": [{"name": "minecraft:entity.slime.jump", "type": "event", "pitch": 1.4},
                         {"name": "minecraft:block.slime_block.fall", "type": "event", "pitch": 1.5}],
    "guhparkour.oeps": [{"name": "guhs:guh_ambient12", "pitch": 0.85}, {"name": "guhs:guh_ambient5", "pitch": 0.8}],
    "guhparkour.smak": [{"name": "minecraft:entity.generic.eat", "type": "event", "pitch": 1.3},
                        {"name": "minecraft:entity.player.burp", "type": "event", "pitch": 1.6, "volume": 0.5}],
}
ONDERTITELS = {
    "guhparkour.klik": "Stuk aangeklikt",
    "guhparkour.start": "Guh gaat van start",
    "guhparkour.finish": "Guh komt over de finish",
    "guhparkour.record": "Guh loopt een record",
    "guhparkour.hup": "Guh springt",
    "guhparkour.boing": "Springplank veert",
    "guhparkour.oeps": "Guh valt van de balk",
    "guhparkour.smak": "Guh smikkelt",
}

RECEPTEN = {
    "guhparkour_startpaaltje": ([" G ", " F ", " S "], {"G": "minecraft:lime_wool", "F": "#minecraft:wooden_fences", "S": "#minecraft:wooden_slabs"}, 1),
    "guhparkour_finishpaaltje": ([" WP", " F ", " S "], {"W": "minecraft:white_wool", "P": "minecraft:pink_wool", "F": "#minecraft:wooden_fences",
                                                       "S": "#minecraft:wooden_slabs"}, 1),
    "guhparkour_horde": (["SWS", "F F"], {"S": "minecraft:stick", "W": "minecraft:pink_wool", "F": "#minecraft:wooden_fences"}, 2),
    "guhparkour_springplank": (["CCC", "PBP"], {"C": "minecraft:pink_carpet", "P": "#minecraft:planks", "B": "minecraft:slime_ball"}, 1),
    "guhparkour_kruiptunnel": (["WWW", "S S"], {"W": "minecraft:light_blue_wool", "S": "minecraft:stick"}, 1),
    "guhparkour_slalompaaltjes": (["W W", "FWF", "SSS"], {"W": "minecraft:pink_wool", "F": "#minecraft:wooden_fences", "S": "#minecraft:wooden_slabs"}, 1),
    "guhparkour_evenwichtsbalk": (["LLL", "W W"], {"L": "#minecraft:logs", "W": "minecraft:pink_wool"}, 1),
    "guhparkour_knabbeltafeltje": (["KBK", "SSS", "F F"], {"K": "guhs:kaas_knabbels", "B": "minecraft:bowl", "S": "#minecraft:wooden_slabs",
                                                          "F": "#minecraft:wooden_fences"}, 1),
    "guhparkour_scorebord": (["PPP", "PCP", "F F"], {"P": "#minecraft:planks", "C": "minecraft:clock", "F": "#minecraft:wooden_fences"}, 1),
}

# the game test rooms (a floor on y 0): the demo course (ParkourCommando.proefbaan: 30 blocks long) and a small one
TEST_BAAN = (36, 8, 9)
TEST_KLEIN = (18, 8, 12)


def _model(h, name, model):
    model = dict(model)
    model["elements"] = uvfix.binnen(model["elements"])
    h.w(f"{h.A}/models/block/{name}.json", model)


def blocks(h):
    A = h.A
    modellen.textures(h)
    for bid in BLOKKEN:
        _model(h, bid, modellen.MODELLEN[bid]())
        h.w(f"{A}/models/item/{bid}.json", {"parent": f"guhs:block/{bid}"})
        h.self_drop(bid)
        naam, lore, uitleg = NAMEN[bid]
        h.lang(f"block.guhs.{bid}", naam, naam)
        h.lang(f"block.guhs.{bid}.lore", lore, lore)
        h.lang(f"block.guhs.{bid}.uitleg", uitleg, uitleg)
        if bid != "guhparkour_kruiptunnel":
            h.w(f"{A}/blockstates/{bid}.json", {"variants": h.facing_states(bid)})
    # the kruiptunnel: its model, plus the bump of the guh inside (block state bobbel 1..5)
    parts = []
    for n in range(1, BOBBELS + 1):
        _model(h, f"guhparkour_kruiptunnel_bobbel_{n}", modellen.kruiptunnel_bobbel(n))
    for facing, y in lib.ROT:
        apply = {"model": "guhs:block/guhparkour_kruiptunnel"}
        if y:
            apply["y"] = y
        parts.append({"when": {"facing": facing}, "apply": apply})
        for n in range(1, BOBBELS + 1):
            apply = {"model": f"guhs:block/guhparkour_kruiptunnel_bobbel_{n}"}
            if y:
                apply["y"] = y
            parts.append({"when": {"facing": facing, "bobbel": str(n)}, "apply": apply})
    h.w(f"{A}/blockstates/guhparkour_kruiptunnel.json", {"multipart": parts})
    for bid, (pattern, key, count) in RECEPTEN.items():
        h.shaped(bid, pattern, key, f"guhs:{bid}", count)
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{b}" for b in BLOKKEN])


def build(h):
    blocks(h)
    lib.geluid(h, SOUNDS, ONDERTITELS)
    for adv in ADVANCEMENTS:
        lib.quest_adv(h, adv)
    lib.teksten(h, TEXTS)
    lib.test_kamer(h, "guhparkour_test_baan", TEST_BAAN)
    lib.test_kamer(h, "guhparkour_test_klein", TEST_KLEIN)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_parkour", blokken=BLOKKEN, items=BLOKKEN, keys=TEXTS, templates=("guhparkour_test_baan", "guhparkour_test_klein"))
    problems = []
    for bid in BLOKKEN:
        for f in (f"{h.D}/loot_table/blocks/{bid}.json", f"{h.D}/recipe/{bid}.json"):
            if not os.path.exists(f):
                problems.append(f"missing {f}")
        for sub in ("lore", "uitleg"):
            if f"block.guhs.{bid}.{sub}" not in h.NL:
                problems.append(f"no {sub} for {bid}")
        for e in modellen.MODELLEN[bid]()["elements"]:
            if min(e["from"]) < -16 or max(e["to"]) > 32:
                problems.append(f"{bid}: element out of range {e['from']} {e['to']}")
            if "rotation" in e and e["rotation"]["angle"] not in (-45, -22.5, 0, 22.5, 45):
                problems.append(f"{bid}: bad rotation {e['rotation']}")
    for name in modellen.EIGEN:
        if not os.path.exists(os.path.join(h.TEX, "block", f"guhparkour_{name}.png")):
            problems.append(f"missing texture guhparkour_{name}")
    for name in modellen.SPEELGOED:
        if not os.path.exists(os.path.join(h.TEX, "block", f"speelgoed_{name}.png")):
            problems.append(f"missing speelgoed texture {name}")
    for adv in ADVANCEMENTS:
        if not os.path.exists(f"{h.D}/advancement/quest/{adv}.json"):
            problems.append(f"missing advancement quest/{adv}")
    # the Java side knows the same obstacles, and the demo course fits in its test room
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "parkour")
    src = open(os.path.join(java, "Obstakel.java"), encoding="utf-8").read()
    for bid in BLOKKEN[2:8]:
        if f'"{bid[len("guhparkour_"):]}"' not in src:
            problems.append(f"{bid} is not an Obstakel in Obstakel.java")
    src = open(os.path.join(java, "ParkourSlice.java"), encoding="utf-8").read()
    for event in SOUNDS:
        if f'"{event}"' not in src:
            problems.append(f"sound {event} is not registered in ParkourSlice.java")
    if problems:
        raise SystemExit("guhpixel_parkour self-check failed:\n  " + "\n  ".join(problems))


# =====================================================================================================================
# FTB quests (chapter Guhpixel, section "Guh-parkour")
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    q("guhparkour_paaltjes", "Op uw plaatsen...", "Maak een &dStartpaaltje&r en een &dFinishpaaltje&r. Daartussen komt het &dGuh-parkour&r: "
      "een route langs speelgoed en hindernissen waar je guhs rondjes over lopen. Houd het Startpaaltje in je hand en klik de stukken aan in "
      "volgorde: 1, 2, 3... en als laatste het Finishpaaltje. Zet het Startpaaltje daarna neer (binnen 32 blokken van alle stukken).",
      "guhs:guhparkour_startpaaltje", [item("guhs:guhparkour_startpaaltje"), item("guhs:guhparkour_finishpaaltje")],
      rewards=(("guhs:kaas_knabbels", 8),), shape="circle")
    q("guhparkour_hindernissen", "Hup, boing, oeps", "Bouw hindernissen: de &dGuh-horde&r (hup, eroverheen) en de &dGuh-springplank&r (boing!). "
      "Er zijn ook de &dKruiptunnel&r (je guh loopt er als een bobbel doorheen), de &dSlalompaaltjes&r, de &dEvenwichtsbalk&r (oeps, eraf... en "
      "er weer op) en het &dKnabbeltafeltje&r: de verplichte pitstop. Het glijbaantje, de wip, de schommel en de pluizige tunnel mogen ook in de route!",
      "guhs:guhparkour_horde", [item("guhs:guhparkour_horde"), item("guhs:guhparkour_springplank")], rewards=(("guhs:kaas_knabbels", 10),))
    q("guhparkour_rondje", "Af!", "Rechtsklik op het Startpaaltje, kies een van je guhs en zet hem &dop de route&r (er passen er vier tegelijk op). "
      "Hij draaft naar de start, gebruikt elk stuk en begint dan gewoon opnieuw, tot je hem eraf haalt. Laat een guh &6een heel rondje&r lopen. "
      "In hetzelfde scherm zet je stukken eerder of later in de route, of haal je ze eruit.",
      "guhs:guhparkour_finishpaaltje", [adv("guhparkour_rondje")], rewards=(("guhs:kaas_knabbels", 12),), xp=50)
    q("guhparkour_scorebord", "De snelste vads", "Zet een &dParkour-scorebord&r binnen 32 blokken van je Startpaaltje. Daarop staat per guh het "
      "aantal rondjes en de &6beste rondetijd&r. Wie van je guhs is de snelste? (En wie valt het vaakst van de balk, njeg?)",
      "guhs:guhparkour_scorebord", [item("guhs:guhparkour_scorebord")], rewards=(("guhs:kaas_knabbels", 8),))
    q("guhparkour_groot", "Het grote Guh-parkour", "Bouw een route van &6minstens acht stukken&r met een Finishpaaltje aan het eind en laat een guh "
      "hem helemaal uitlopen. Er passen zestien stukken in een route. Daarna heeft hij wel een dutje verdiend.",
      "guhs:guhparkour_knabbeltafeltje", [adv("guhparkour_groot")], rewards=(("guhs:gefrituurde_kaasknabbels", 3),), shape="gear", xp=200)
