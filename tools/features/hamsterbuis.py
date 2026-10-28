"""
1.8.0 (Dromen en vadskracht): the hamsterbuis network. Java: feature/hamsterbuis (HamsterbuisFeature, HamsterNet,
Knooppunt, Station, HamsterPayloads, client/HamsterbuisScreen).

  build(h)   the Hamsterknooppunt (a machine block: facing x snoet, a pink frame with see-through tubes and a guh in a wheel)
             and the hamsterbuis station (a see-through tube entrance on a pink ring), their items, loot, recipes, texts
             (Dutch in nl_nl, English in tools/lang/en/c217_hamsterbuis.json), blokuitleg entries
  ftb(fq)    a section of its own in Guh-technologie (guhs_techniek): "Het Hamsterbuis-net"

Standalone (from the project root): PYTHONPATH=tools python tools/features/hamsterbuis.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

KNOOP, STATION = "hamsterknooppunt", "hamsterbuis_station"
G = "gui.guhs.hamsterbuis."
BK, BS = f"block.guhs.{KNOOP}", f"block.guhs.{STATION}"

FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [("tech_hamsterbuis", "Het Hamsterbuis-net", f"item:guhs:{STATION}", None)]

# key -> (Dutch, English)
TEKSTEN = {
    BK: ("Hamsterknooppunt", "Hamster Junction"),
    BK + ".lore": ("Het hoofdstation van jouw hamsterbuis-net. Hangt aan vadskracht en vult een buffer; elke reis kost %s VK "
                   "(buffer: %s VK). Zonder vadskracht ligt je hele net stil, njeg.",
                   "The main station of your hamster tube network. Runs on vadskracht and fills a buffer; every trip costs %s VK "
                   "(buffer: %s VK). Without vadskracht your whole network stands still, njeg."),
    BK + ".nieuw": ("Dit is nu je Hamsterknooppunt! Leg er een guhdraad naartoe, dan rollen de hamsterbuizen.",
                    "This is your Hamster Junction now! Run a guh wire to it and the hamster tubes start rolling."),
    BK + ".verhuisd": ("Je Hamsterknooppunt is verhuisd naar hier. Het oude doet nu niks meer.",
                       "Your Hamster Junction moved here. The old one does nothing any more."),
    BK + ".niet_meer": ("Dit is niet (meer) je Hamsterknooppunt. Er staat ergens een nieuwere.",
                        "This is not (or no longer) your Hamster Junction. A newer one stands somewhere."),
    BK + ".stand": ("Hamsterknooppunt: %s / %s VK in de buffer, een reis kost %s VK. Stations in je net: %s.",
                    "Hamster Junction: %s / %s VK in the buffer, a trip costs %s VK. Stations in your network: %s."),
    BK + ".lading": ("Buffer: %s / %s VK", "Buffer: %s / %s VK"),
    BK + ".reis": ("Een reis kost %s VK", "A trip costs %s VK"),
    BS: ("Hamsterbuis-station", "Hamster Tube Station"),
    BS + ".lore": ("Zet hem overal neer, in elke dimensie. Stap in de buis en kies waarheen: woesj! Heeft zelf geen vadskracht "
                   "nodig, je Hamsterknooppunt betaalt (%s VK per reis).",
                   "Put it anywhere, in any dimension. Step into the tube and pick where to go: whoosh! Needs no vadskracht itself, "
                   "your Hamster Junction pays (%s VK per trip)."),
    BS + ".naam_n": ("Hamsterbuis %s", "Hamster Tube %s"),
    BS + ".geen_knooppunt": ("Dit station is ingeschreven, maar je hebt nog geen Hamsterknooppunt. Zonder knooppunt rolt er niks.",
                             "This station is registered, but you have no Hamster Junction yet. Without a junction nothing rolls."),
    BS + ".niet_van_jou": ("Dit is iemands anders hamsterbuis. Vraag of je mag (droomwolkje-vriendjes mogen altijd)!",
                           "This is someone else's hamster tube. Ask if you may (dream cloud friends always may)!"),
    BS + ".weg": ("Dat station bestaat niet meer. Het is uit het net gehaald.", "That station doesn't exist any more. It was taken out of the network."),
    BS + ".stil": ("Het hamsterbuis-net ligt stil: de buffer van het knooppunt heeft minder dan %s VK. Geef het knooppunt vadskracht!",
                   "The hamster tube network stands still: the junction's buffer has less than %s VK. Give the junction vadskracht!"),
    BS + ".woesj": ("Woesj! Welkom bij %s.", "Whoosh! Welcome to %s."),
    G + "naam": ("Naam", "Name"),
    G + "openbaar": ("Openbaar", "Public"),
    G + "prive": ("Privé", "Private"),
    G + "openbaar.tooltip": ("Openbaar: iedereen mag met dit station reizen. Privé: alleen jij en je droomwolkje-vriendjes.",
                             "Public: everyone may travel with this station. Private: only you and your dream cloud friends."),
    G + "rij": ("%s  (%s, %s, %s)", "%s  (%s, %s, %s)"),
    G + "ga": ("Woesj naar %s (%s)", "Whoosh to %s (%s)"),
    G + "ga_stil": ("Het net van %s ligt stil (%s): het knooppunt heeft te weinig vadskracht.",
                    "The network of %s stands still (%s): the junction has too little vadskracht."),
    G + "lading": ("Knooppunt: %s / %s VK, reis: %s VK", "Junction: %s / %s VK, trip: %s VK"),
    G + "geen_knooppunt": ("Geen Hamsterknooppunt: dit net ligt stil.", "No Hamster Junction: this network stands still."),
    G + "geen": ("Nog geen andere stations. Zet er nog een neer, ergens ver weg, en kom terug!",
                 "No other stations yet. Put another one down, somewhere far away, and come back!"),
}

BLOKUITLEG = {
    KNOOP: dict(
        nl="Het hoofdstation van je hamsterbuis-net. Leg een guhdraad naar het knooppunt: wat je net overhoudt, vult zijn buffer "
           "(tot 4000 VK). Elke reis met een hamsterbuis-station kost 100 VK uit die buffer. Je hebt er één: zet je een nieuwe "
           "neer, dan verhuist het knooppunt mee.\n\n- Rechtsklik: hoe vol de buffer is en hoeveel stations je net heeft.",
        en="The main station of your hamster tube network. Run a guh wire to the junction: what your net has left over fills its "
           "buffer (up to 4000 VK). Every trip with a hamster tube station costs 100 VK from that buffer. You have one: place a "
           "new one and the junction moves along.\n\n- Right-click: how full the buffer is and how many stations your network has."),
    STATION: dict(
        nl="Een doorzichtige hamsterbuis-ingang, voor overal: in de Guhmension, de gewone wereld, de Barbecuether, het Guheinde... "
           "Stap in de buis (of klik erop) en kies een ander station: woesj, daar sta je. Heeft zelf geen vadskracht nodig.\n\n"
           "- Jij hernoemt je stations en zet ze op Openbaar of Privé.\n- Je droomwolkje-vriendjes mogen altijd mee.\n"
           "- Sluipend in de buis stappen: dan gebeurt er niks.",
        en="A see-through hamster tube entrance, for anywhere: the Guhmension, the normal world, the Barbecuether, the Guheinde... "
           "Step into the tube (or click it) and pick another station: whoosh, there you are. Needs no vadskracht itself.\n\n"
           "- You rename your stations and set them Public or Private.\n- Your dream cloud friends may always come along.\n"
           "- Sneaking into the tube: nothing happens."),
}

ROZE, ROZE_D, ROZE_L = (238, 150, 190), (196, 104, 150), (252, 206, 226)
GLAS, GLAS_L, RAND = (190, 230, 246, 110), (236, 250, 255, 170), (120, 70, 104)
VACHT, VACHT_D, OOG = (250, 196, 214), (220, 150, 180), (40, 30, 40)


def _frame(slaapt=False):
    img = Image.new("RGBA", (16, 16), (ROZE_D if slaapt else ROZE) + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=RAND + (255,))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.point((x, y), fill=ROZE_L + (255,))
    return img


def knoop_voor(staat):
    """The face: a hamster wheel with a guh in it behind a round window; slaapt = dim, werkt = spinning lines, vol = sparkle."""
    img = _frame(staat == "slaapt")
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=(70, 50, 70, 255), outline=RAND + (255,))
    d.ellipse([3, 3, 12, 12], outline=(255, 214, 110, 255))                     # the wheel
    if staat != "slaapt":
        for (a, b) in (((7, 3), (8, 12)), ((3, 7), (12, 8))):
            d.line([a, b], fill=(214, 166, 60, 255))
    d.ellipse([5, 6, 10, 11], fill=VACHT + (255,))                              # the guh
    d.point([(5, 5), (10, 5)], fill=VACHT_D + (255,))                           # ears
    if staat == "slaapt":
        d.line([6, 8, 7, 8], fill=OOG + (255,))
        d.line([8, 8, 9, 8], fill=OOG + (255,))
    else:
        d.point([(6, 8), (9, 8)], fill=OOG + (255,))
        d.point([(7, 10), (8, 10)], fill=(214, 110, 140, 255))
    if staat == "vol":
        d.point([(3, 3), (12, 4), (4, 12)], fill=(255, 255, 255, 255))
    return img


def knoop_zij():
    img = _frame()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 0, 12, 15], fill=GLAS)                                      # a see-through tube
    d.line([5, 0, 5, 15], fill=GLAS_L)
    d.line([3, 0, 3, 15], fill=RAND + (255,))
    d.line([12, 0, 12, 15], fill=RAND + (255,))
    return img


def knoop_boven():
    img = _frame()
    d = ImageDraw.Draw(img)
    d.ellipse([3, 3, 12, 12], fill=GLAS, outline=RAND + (255,))
    d.ellipse([5, 5, 10, 10], outline=GLAS_L)
    return img


def buis():
    """The station's tube wall: see-through glass with a highlight and little pink hamster paw prints."""
    img = Image.new("RGBA", (16, 16), GLAS)
    d = ImageDraw.Draw(img)
    d.line([3, 0, 3, 15], fill=GLAS_L)
    d.line([4, 0, 4, 15], fill=GLAS_L)
    for (x, y) in ((10, 3), (12, 9), (9, 13)):
        d.point([(x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)], fill=ROZE + (200,))
    return img


def ring():
    img = _frame()
    d = ImageDraw.Draw(img)
    d.line([0, 7, 15, 7], fill=ROZE_L + (255,))
    for x in (3, 8, 13):
        d.point((x, 4), fill=(255, 214, 110, 255))
    return img


def station_icoon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 1, 12, 14], fill=GLAS, outline=RAND + (255,))
    d.line([5, 2, 5, 13], fill=GLAS_L)
    d.rectangle([2, 13, 13, 15], fill=ROZE + (255,), outline=RAND + (255,))
    d.rectangle([2, 0, 13, 2], fill=ROZE + (255,), outline=RAND + (255,))
    d.ellipse([7, 6, 10, 9], fill=VACHT + (255,))
    d.point([(8, 7)], fill=OOG + (255,))
    return img


def _el(frm, to, faces):
    return {"from": frm, "to": to, "faces": faces}


def build(h):
    A, w = h.A, h.w
    for staat in ("slaapt", "werkt", "vol"):
        h.save(knoop_voor(staat), "block", f"{KNOOP}_voor_{staat}.png")
    h.save(knoop_zij(), "block", f"{KNOOP}_zij.png")
    h.save(knoop_boven(), "block", f"{KNOOP}_boven.png")
    h.save(buis(), "block", f"{STATION}_buis.png")
    h.save(ring(), "block", f"{STATION}_ring.png")
    h.save(station_icoon(), "item", f"{STATION}.png")
    # the knooppunt: facing x snoet (a MachineBlock)
    variants = {}
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for staat in ("slaapt", "werkt", "vol"):
        w(f"{A}/models/block/{KNOOP}_{staat}.json", {"parent": "minecraft:block/orientable", "textures": {
            "front": f"guhs:block/{KNOOP}_voor_{staat}", "side": f"guhs:block/{KNOOP}_zij", "top": f"guhs:block/{KNOOP}_boven",
            "particle": f"guhs:block/{KNOOP}_zij"}})
        for f, y in rot.items():
            v = {"model": f"guhs:block/{KNOOP}_{staat}"}
            if y:
                v["y"] = y
            variants[f"facing={f},snoet={staat}"] = v
    w(f"{A}/blockstates/{KNOOP}.json", {"variants": variants})
    w(f"{A}/models/item/{KNOOP}.json", {"parent": f"guhs:block/{KNOOP}_werkt"})
    h.self_drop(KNOOP)
    # the station: a ring at the bottom and the top, four glass walls (a tube you walk into)
    b, r = {"texture": "#buis"}, {"texture": "#ring"}
    elementen = [
        _el([0, 0, 0], [16, 2, 16], {k: dict(r) for k in ("north", "south", "east", "west", "up", "down")}),
        _el([0, 14, 0], [16, 16, 16], {k: dict(r) for k in ("north", "south", "east", "west", "up", "down")}),
        _el([1, 2, 1], [15, 14, 2], {"north": dict(b), "south": dict(b)}),
        _el([1, 2, 14], [15, 14, 15], {"north": dict(b), "south": dict(b)}),
        _el([1, 2, 2], [2, 14, 14], {"east": dict(b), "west": dict(b)}),
        _el([14, 2, 2], [15, 14, 14], {"east": dict(b), "west": dict(b)}),
    ]
    h.furniture_model(STATION, elementen, {"buis": f"guhs:block/{STATION}_buis", "ring": f"guhs:block/{STATION}_ring",
                                           "particle": f"guhs:block/{STATION}_ring"})
    w(f"{A}/models/item/{STATION}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{STATION}"}})
    # recipes
    h.shaped(KNOOP, ["GWG", "BRB", "GKG"], {"G": "minecraft:glass", "W": "guhs:guh_wheel", "B": "guhs:knabbelbatterij",
                                             "R": "guhs:guh_wire", "K": "guhs:block_of_kaasknabbels"}, f"guhs:{KNOOP}")
    h.shaped(STATION, ["P P", "G G", "PKP"], {"P": "minecraft:pink_wool", "G": "minecraft:glass_pane", "K": "guhs:kaas_knabbels"},
             f"guhs:{STATION}", 2)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


# =====================================================================================================================
# FTB: a section of its own in Guh-technologie (guhs_techniek)
# =====================================================================================================================
FTB = [
    ("tech_hamsterknooppunt", "Het Hamsterknooppunt", "Bouw een &dHamsterknooppunt&r en leg er een guhdraad naartoe. Het vult een "
     "buffer met vadskracht: daarmee rollen al je hamsterbuizen. Eentje per guhbaas!", f"guhs:{KNOOP}",
     "The Hamster Junction", "Build a &dHamster Junction&r and run a guh wire to it. It fills a buffer with vadskracht: that keeps "
     "all your hamster tubes rolling. One per guh owner!", 0, 0),
    ("tech_hamsterbuis_station", "Woesj!", "Zet twee &dhamsterbuis-stations&r neer, ver uit elkaar (mag in een andere dimensie!). "
     "Stap in de buis, kies waarheen, en woesj!", f"guhs:{STATION}",
     "Whoosh!", "Place two &dhamster tube stations&r, far apart (another dimension is fine!). Step into the tube, pick where to "
     "go, and whoosh!", 2, 0),
]


def ftb(fq):
    vorige = None
    for key, titel, tekst, icoon, _, _, x, y in FTB:
        aantal = 2 if key == "tech_hamsterbuis_station" else 1
        fq.q(key, titel, tekst, icoon, [fq.item(icoon, aantal)], rewards=(("guhs:kaas_knabbels", 16),),
             deps=[vorige] if vorige else [], x=x, y=y, xp=100)
        vorige = key


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en = {k: e for k, (n, e) in TEKSTEN.items()}
    for key, _, _, _, titel_en, tekst_en, _, _ in FTB:
        en[f"ftb.{FTB_CHAPTER}.q.{key}.title"] = titel_en
        en[f"ftb.{FTB_CHAPTER}.q.{key}.desc"] = tekst_en
    en[f"ftb.{FTB_CHAPTER}.section.tech_hamsterbuis"] = "The Hamster Tube Network"
    with open(os.path.join("tools", "lang", "en", "c217_hamsterbuis.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for blok in (KNOOP, STATION):
        with open(os.path.join(a, blok + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{blok}"}}, f, indent=2)
            f.write("\n")
    # mineable with a pickaxe (the knooppunt needs a tool to drop; the glass station too)
    tag = os.path.join("src", "main", "resources", "data", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    with open(tag, encoding="utf-8") as f:
        t = json.load(f)
    for blok in (KNOOP, STATION):
        if f"guhs:{blok}" not in t["values"]:
            t["values"].append(f"guhs:{blok}")
    with open(tag, "w", encoding="utf-8", newline="\n") as f:
        json.dump(t, f, indent=2)
        f.write("\n")
    # vadskracht: the knooppunt is part of a net (tag guhs:vadskracht)
    tag = os.path.join("src", "main", "resources", "data", "guhs", "tags", "block", "vadskracht.json")
    with open(tag, encoding="utf-8") as f:
        t = json.load(f)
    if f"guhs:{KNOOP}" not in t["values"]:
        t["values"].append(f"guhs:{KNOOP}")
    with open(tag, "w", encoding="utf-8", newline="\n") as f:
        json.dump(t, f, indent=2)
        f.write("\n")
    print(f"hamsterbuis: knooppunt + station, {len(TEKSTEN)} texts, {len(FTB)} quests")


if __name__ == "__main__":
    _direct()
