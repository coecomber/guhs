"""
einde2 (the Guheinde expansion), slice biomeguhs: the six tameable guh variants of the outer islands and their tricks.
Java: nl.juiced.guhs.feature.einde2.biomeguhs (E2BiomeguhsFeature).

  - Korstguh (Gatenkaasheuvels): rolls up into a ball you can push, naps in a kaasgat;
  - Duinguh (Korstwoestijn): digs in until only its ears show, never sinks in kruimelzand;
  - Kruimelguh (Kruimelwolken): floats down slowly, stands on the kruimelwolken;
  - Schimmelguh (Schimmelkaasvlakte): glows like a lamp (the invisible light block schimmelguh_licht follows it);
  - Kristalguh (Kristalkaasklippen): now and then finds a kristalkaasbrokje;
  - Koorguh (Knabbelkoorbos): sings along with a jukebox.

Their bones, fur and glow masks: einde2_biomeguhs_tex.py (the variants() and BONES hooks of make_guh_variants.py are passed
on from there). Here: the texts, the sounds, the advancements, the light block, the test rooms and the FTB quests.
"""
import features.einde2_kit as kit
import features.einde2_biomeguhs_tex as tex

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "Guhs van de buiteneilanden"
FTB_PORTRAIT = "guh:korstguh"                              # (shown once the picture tools know our bone prefixes: einde2_biomeguhs_wiki.py)
P = "einde2_biomeguhs"
BONES = tex.BONES

# variant id: (name, biome id, biome name with its article, quest icon, rarity, Guhdex info, quest title, quest text)
GUHS = {
    "korstguh": ("Korstguh", "gatenkaasheuvels", "de Gatenkaasheuvels", "guhs:kruimelrots", "Ongewoon (alleen in de Gatenkaasheuvels)",
                 "Een gatenkaasgele guh met een schild van gebakken korst, waar hij helemaal in past. Getemd rolt hij zich op tot een "
                 "balletje als je naast hem bukt: geef het balletje een duwtje of een tikje en daar gaat hij! "
                 "Zijn dutjes doet hij het liefst in een kaasgat.",
                 "Kaas met een korstje",
                 "In de &6Gatenkaasheuvels&r van het Guheinde rollen de &6Korstguhs&r rond: gatenkaasgeel, met een schild van korst. Tem "
                 "er een met kaasknabbels. Buk naast hem en hij rolt zich op tot een balletje dat je kunt wegduwen. Njeg, hij vindt het leuk!"),
    "duinguh": ("Duinguh", "korstwoestijn", "de Korstwoestijn", "guhs:kruimelzand", "Ongewoon (alleen in de Korstwoestijn)",
                "Een zandkleurige guh met enorme oren. In kruimelzand zakt hij niet weg: hij rent er zo overheen. Getemd graaft hij zich "
                "op kruimelzand in tot je alleen zijn oortjes nog ziet. Zo houdt hij de wacht. Of doet hij een dutje, wie zal het zeggen.",
                "Alleen de oortjes",
                "In de droge &6Korstwoestijn&r woont de &6Duinguh&r, met oren waar je een zeil van kunt maken. Tem er een. Laat hem "
                "stilstaan op &7kruimelzand&r en hij graaft zich in: alleen zijn oortjes steken er nog bovenuit."),
    "kruimelguh": ("Kruimelguh", "kruimelwolken", "de Kruimelwolken", "guhs:kruimelwolk", "Zeldzaam (alleen in de Kruimelwolken)",
                   "Een roomwitte guh vol kruimels, met een wolkje op zijn rug. Hij is zo licht dat hij op kruimelwolken blijft staan en "
                   "heel langzaam omlaag zweeft als hij van een eilandje stapt. Vallen kan hij niet eens, hoe hard hij het ook probeert.",
                   "Lichter dan een kruimel",
                   "Hoog in de &6Kruimelwolken&r dwarrelt de &6Kruimelguh&r van eilandje naar eilandje. Hij staat gewoon op de "
                   "kruimelwolken waar jij doorheen zakt, en zweeft heel langzaam omlaag. Tem er een!"),
    "schimmelguh": ("Schimmelguh", "schimmelkaasvlakte", "de Schimmelkaasvlakte", "guhs:schimmelkaas", "Zeldzaam (alleen op de Schimmelkaasvlakte)",
                    "Een bleke guh met blauwe adertjes, net blauwaderkaas, en lichtgevende paddenstoeltjes op zijn kop en rug. Getemd "
                    "gloeit hij als een lampje dat met je meeloopt. Hij ruikt een beetje sterk. Zeg daar maar niks van, njeg.",
                    "Een lampje op pootjes",
                    "In de mist van de &6Schimmelkaasvlakte&r gloeit iets. Het is een &6Schimmelguh&r: blauwaderkaas met "
                    "paddenstoeltjes. Tem er een en je hebt nooit meer een fakkel nodig: hij gloeit als een lamp en loopt met je mee."),
    "kristalguh": ("Kristalguh", "kristalkaasklippen", "de Kristalkaasklippen", "guhs:kristalkaas", "Zeldzaam (alleen op de Kristalkaasklippen)",
                   "Een klipkaasgrijze guh waar gouden kaaskristallen uit groeien. Hij heeft een neus voor alles wat glimt: getemd "
                   "snuffelt hij af en toe een kristalkaasbrokje op, hooguit één per dag. Hij geeft het nog af ook. Meestal.",
                   "Een neus voor glimmers",
                   "Op de &6Kristalkaasklippen&r glinstert de &6Kristalguh&r, met een kroontje van kaaskristal. Tem er een. Af en toe "
                   "snuffelt hij een &6kristalkaasbrokje&r voor je op: hooguit één per dag, dus wees er zuinig op."),
    "koorguh": ("Koorguh", "knabbelkoorbos", "het Knabbelkoorbos", "guhs:knabbelkoorvrucht", "Zeldzaam (alleen in het Knabbelkoorbos)",
                "Een paarse guh met een knabbelkoorbloem op zijn kop en een keurig wit koorkraagje. Getemd zingt hij uit volle borst mee "
                "zodra er een muziekplaat draait. Vals, maar met gevoel. Op het feest van de Disco-dynamo telt hij voor twee.",
                "Eerste stem: njeg",
                "Tussen de zingende planten van het &6Knabbelkoorbos&r zingt er eentje terug: de &6Koorguh&r, met koorkraagje en al. "
                "Tem er een, zet een &7muziekplaat&r op en luister. Njeg-njeg-njeeeeg!"),
}

TEXTS = {
    # what a freshly tamed one tells its owner (once per guh)
    f"gui.guhs.{P}.getemd.korstguh": "%s is van jou! Buk naast hem en hij rolt zich op. Duw het balletje maar weg, njeg.",
    f"gui.guhs.{P}.getemd.duinguh": "%s is van jou! Op kruimelzand graaft hij zich in tot zijn oortjes.",
    f"gui.guhs.{P}.getemd.kruimelguh": "%s is van jou! Hij zweeft langzaam omlaag en staat op kruimelwolken.",
    f"gui.guhs.{P}.getemd.schimmelguh": "%s is van jou! Hij gloeit nu als een lampje dat met je meeloopt.",
    f"gui.guhs.{P}.getemd.kristalguh": "%s is van jou! Af en toe snuffelt hij een kristalkaasbrokje voor je op.",
    f"gui.guhs.{P}.getemd.koorguh": "%s is van jou! Zet een muziekplaat op en hij zingt mee.",
    f"gui.guhs.{P}.brokje": "%s heeft iets glimmends opgesnuffeld: een kristalkaasbrokje!",
    f"gui.guhs.{P}.rolt": "%s rolt! Njeeeeg!",
    f"gui.guhs.{P}.zingt": "%s zingt mee. Vals, maar met gevoel.",
    # the invisible light of the Schimmelguh (never in your hand, but every block has a name)
    "block.guhs.schimmelguh_licht": "Schimmelguh-gloed",
    # the dev commands
    f"gui.guhs.{P}.commando.gezet": "Een getemde %s staat naast je.",
    f"gui.guhs.{P}.commando.wild": "Er kwam een wilde %s aan.",
    f"gui.guhs.{P}.commando.truc": "%s doet zijn trucje.",
    f"gui.guhs.{P}.commando.geen": "Er staat geen eigen biome-guh bij je in de buurt.",
    f"gui.guhs.{P}.commando.niets": "Hier komt nu geen biome-guh (geen streek van de buiteneilanden, geen eilandgrond of al vol).",
}

SOUNDS = {  # our event -> (what it plays, subtitle)
    f"{P}.korstguh.rol": ([{"name": "minecraft:entity.armadillo.roll", "type": "event", "pitch": 1.2}], "Korstguh rolt zich op"),
    f"{P}.korstguh.uitrol": ([{"name": "minecraft:entity.armadillo.unroll_finish", "type": "event", "pitch": 1.2}], "Korstguh rolt zich uit"),
    f"{P}.korstguh.tik": ([{"name": "minecraft:entity.armadillo.land", "type": "event", "pitch": 1.3},
                           {"name": "guhs:entity.guh.happy", "type": "event", "pitch": 1.3}], "Korstbal rolt weg"),
    f"{P}.duinguh.graaf": ([{"name": "minecraft:block.sand.break", "type": "event", "pitch": 0.9},
                            {"name": "minecraft:block.sand.hit", "type": "event", "pitch": 0.8}], "Duinguh graaft zich in"),
    f"{P}.duinguh.plop": ([{"name": "minecraft:block.sand.place", "type": "event", "pitch": 1.4}], "Duinguh plopt omhoog"),
    f"{P}.kruimelguh.plof": ([{"name": "minecraft:block.wool.fall", "type": "event", "pitch": 1.5}], "Kruimelguh landt zacht"),
    f"{P}.schimmelguh.gloei": ([{"name": "minecraft:block.amethyst_block.resonate", "type": "event", "pitch": 0.7}], "Schimmelguh begint te gloeien"),
    f"{P}.kristalguh.vondst": ([{"name": "minecraft:block.amethyst_block.chime", "type": "event", "pitch": 1.3}], "Kristalguh vindt iets glimmends"),
    f"{P}.koorguh.zing": ([{"name": "minecraft:block.note_block.flute", "type": "event", "pitch": p} for p in (0.84, 1.0, 1.12, 1.26, 1.5)]
                          + [{"name": "guhs:entity.guh.happy", "type": "event", "pitch": 1.5, "volume": 0.7}], "Koorguh zingt mee"),
}

# visible advancements (Guheinde tab): name -> (parent, icon, frame, title, text)
ADV = {
    f"{P}_eerste": ("guheinde_binnen", "guhs:kaas_knabbels", "task", "Vriendje van ver", "Tem een guh van de buiteneilanden van het Guheinde"),
    f"{P}_bal": (f"{P}_eerste", "guhs:kruimelrots", "task", "Balletje balletje", "Geef je opgerolde Korstguh een duwtje"),
    f"{P}_oortjes": (f"{P}_eerste", "guhs:kruimelzand", "task", "Ik zie, ik zie...", "Laat je Duinguh zich ingraven tot je alleen zijn oortjes ziet"),
    f"{P}_koor": (f"{P}_eerste", "minecraft:jukebox", "task", "Het Guhkoor", "Laat je Koorguh meezingen met een muziekplaat"),
    f"{P}_alle": (f"{P}_eerste", "guhs:kristalkaas", "challenge", "Zes streken, zes vriendjes",
                  "Tem een Korstguh, Duinguh, Kruimelguh, Schimmelguh, Kristalguh en Koorguh"),
}
VERBORGEN = [f"{P}_brokje", f"{P}_gloei", f"{P}_zweef"]     # hidden: guhs:quest/<name>, granted from code


def texts(h):
    for id, (naam, _biome, _waar, _icon, zeldzaam, info, *_rest) in GUHS.items():
        kit.pagina(h, id, naam, zeldzaam, info, variant=True)
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def sounds(h):
    def patch(d):
        for event in [e for e in d if e.startswith(P + ".")]:
            del d[event]
        for event, (entries, _sub) in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    for event, (_entries, sub) in SOUNDS.items():
        kit.lang(h, f"subtitles.guhs.{event}", sub)


def advancements(h):
    tamed = lambda id: {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{
        "condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"%s\"}" % id}}]}}
    for name, (parent, icon, frame, titel, tekst) in ADV.items():
        criteria = None
        if name == f"{P}_alle":
            criteria = {id: tamed(id) for id in GUHS}
        kit.zichtbaar(h, name, parent, icon, frame, titel, tekst, criteria=criteria)
    for name in VERBORGEN:
        kit.verborgen(h, name)


def licht(h):
    """schimmelguh_licht: an invisible, walk-through light that follows a tame Schimmelguh (no item, no drops, no model)."""
    h.w(f"{h.A}/blockstates/schimmelguh_licht.json", {"variants": {"": {"model": "minecraft:block/air"}}})


def test_kamers(h):
    """The rooms of E2BiomeguhsGameTests: a floor of kaaskorst (island ground), a strip of kruimelzand, a jukebox corner."""
    def kamer(hoog, vloer=1):
        """A floor of kaaskorst (`vloer` thick) with a rim two blocks high, so nothing rolls or strolls out of the test."""
        s = h.Structure((9, hoog, 9))
        s.fill(0, 0, 0, 8, vloer - 1, 8, "guhs:kaaskorst")
        for x0, z0, x1, z1 in ((0, 0, 8, 0), (0, 8, 8, 8), (0, 0, 0, 8), (8, 0, 8, 8)):
            s.fill(x0, vloer, z0, x1, vloer + 1, z1, "guhs:kaaskorst")
        return s
    kamer(5).save(f"{P}_test_kamer")
    s = kamer(5)
    s.fill(2, 0, 2, 5, 0, 5, "guhs:kruimelzand")            # a patch of kruimelzand in one corner
    s.save(f"{P}_test_zand")
    s = kamer(9, vloer=2)                                   # room above to float down through, a hole to nap in
    s.set(4, 1, 4, "minecraft:air")                        # the kaasgat: one block deep, closed all round
    s.save(f"{P}_test_heuvel")


def selfcheck(h):
    import json
    import os
    for id in GUHS:
        for pad in (f"{h.TEX}/entity/guh_{id}.png", f"{h.TEX}/entity/guh_slaap/guh_{id}.png", f"{h.D}/advancement/quest/tamed_{id}.json"):
            if not os.path.exists(pad):
                raise SystemExit(f"einde2_biomeguhs: missing {pad}")
    for pad in ("guh_schimmelguh_gloed.png", "guh_schimmelguh_lamp.png", "guh_kristalguh_gloed.png"):
        if not os.path.exists(f"{h.TEX}/entity/{pad}"):
            raise SystemExit(f"einde2_biomeguhs: missing glow mask {pad}")
    geluid = json.load(open(f"{h.A}/sounds.json", encoding="utf-8"))
    for event in SOUNDS:
        if event not in geluid:
            raise SystemExit(f"einde2_biomeguhs: sound {event} is not in sounds.json")
    vreemd = sorted({swatch for _p, _pv, swatch, _c in BONES.values()} - {
        "pluis_kuif", "pluis_wang", "wolk_pluis", "koning_goud", "koning_manen", "ender_bone", "asguh_wang", "zeemeer_vin",
        "zeemeer_schubben", "pinguh_vleugel"})
    if vreemd:                                              # the guh sheet has no room: only swatch places of older variants
        raise SystemExit(f"einde2_biomeguhs: bones use a swatch that is not an older variant's: {vreemd}")
    for bone in BONES:
        if bone.split("_")[0] not in GUHS:
            raise SystemExit(f"einde2_biomeguhs: bone {bone} does not start with a variant id")


def build(h):
    texts(h)
    sounds(h)
    advancements(h)
    licht(h)
    test_kamers(h)
    tex.vacht(h)
    selfcheck(h)


def variants(rng, v):
    # (never the rng handed in here: its seed is this module's place in FEATURES, which moves at the merge onto bbq2)
    return tex.variants(v)


def ftb(fq):
    q, adv = fq.q, fq.adv
    for i, (id, (_naam, biome, _waar, icon, _zeldzaam, _info, titel, tekst)) in enumerate(GUHS.items()):
        q(f"{P}_{id}", titel, tekst, icon, [adv(f"tamed_{id}")], rewards=(("guhs:kaas_knabbels", 8),), x=i * 2, y=0, xp=100)
    q(f"{P}_alle", "Zes streken, zes vriendjes", "Elke streek van de buiteneilanden heeft zijn eigen guh. Tem ze alle zes: de &6Korstguh&r, "
      "de &6Duinguh&r, de &6Kruimelguh&r, de &6Schimmelguh&r, de &6Kristalguh&r en de &6Koorguh&r. Ze krijgen elk hun eigen bladzijde in "
      "je Guhdex.", "guhs:guhdex", [adv(f"tamed_{id}") for id in GUHS], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=5, y=2,
      shape="gear", xp=300)

