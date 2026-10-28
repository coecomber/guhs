"""
1.8.0 Guhdromenland, part A (dromen-ontwerp/BOUWPLAN_180.md; design ontwerp-guhs180-dromen.md). Java: feature/dromen
(DromenFeature, DroomSlapen, DroomReis, DroomEilanden, DroomRegels, DroomStart, DroomVoortgang, Wekkerguh, DroomCommando,
SlaapplekBlock).

  build(h)    the dimension guhs:guhdromenland (one flat layer of the temporary droomzee at y 0, nothing else: the islands are
              stamped by DroomEilanden; a fixed-time sky in lavender and pink, low pink clouds, the soft hemel music), the
              sleeping places (doos met knabbels, four kattenhuisjes) + their tag guhs:guh_slaapplekken (with the existing
              guhbedje and guhnestje), the droomkussen, the start-island item tags guhs:droom_start/<eiland>, the recipes and
              the texts (Dutch in nl_nl, English in tools/lang/en/c212_dromen.json)

Standalone (from the project root): PYTHONPATH=tools python tools/features/dromen.py   (writes only part A's own files,
including the 26.1 item definitions)
"""
import json
import os
import random
import sys

from PIL import Image, ImageDraw

KATTENHUISJES = {  # colour -> (body, dark, recipe wool)
    "roze": ((255, 182, 213), (226, 128, 172), "minecraft:pink_wool"),
    "wit": ((250, 246, 242), (214, 204, 198), "minecraft:white_wool"),
    "lavendel": ((206, 182, 240), (160, 132, 206), "minecraft:purple_wool"),
    "mint": ((180, 236, 206), (120, 196, 160), "minecraft:lime_wool"),
}
ZEE = "droomzee_tijdelijk"
DOZEN = ["verhuisdoos", "schoenendoos", "pizzadoos"]   # the other boxes (tools/features/droomspul.py)

# what you give your guh -> the island the dream starts on (DroomStart; "required": false for ids of other parts)
START = {
    "badje": ["guhs:knuffelbad_badeendje", "guhs:zeepje"],                     # zeepje: tools/features/droomspul.py
    "sterren": ["guhs:sterrenlantaarn", "guhs:sterrenkoekje", "guhs:knuffel_starry", "guhs:sterlampje", "guhs:maanlampje_ster"],
    "herinnering": ["guhs:fotootje", "guhs:roze_guh_koek", "guhs:knabbelkoekje", "guhs:reisbureau_kaart_lingsesdijk", "guhs:reisbureau_kaart_kaasmarkt",
                    "guhs:reisbureau_kaart_vadswoud", "guhs:reisbureau_kaart_knuffeldal", "guhs:reisbureau_kaart_bloesemmeertje"],
    "molentje": ["guhs:droomzand", "guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint",
                 "guhs:maanlampje_maan", "guhs:maanlampje_wolkje", "guhs:maanlampje_guh"],   # maanlampjes: slaapzorg.py
    "liedjes": ["guhs:knuffel_normal", "guhs:knuffel_mint", "guhs:knuffel_choco", "guhs:knuffel_snow", "guhs:knuffel_rainbow",
                "guhs:knuffel_wolk", "guhs:knuffel_ghost", "guhs:knuffel_golden", "guhs:knuffel_mager", "guhs:pluiswolblok"]
               + [f"guhs:{k}_kussen" for k in ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                                                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")],
}

EILANDEN = {"plein": ("het Droomplein", "the Dream Square"), "liedjes": ("de Slaapliedjesheuvels", "the Lullaby Hills"),
            "sterren": ("het Sterrenkussenveld", "the Star Cushion Field"), "herinnering": ("het Herinneringseiland", "Memory Island"),
            "badje": ("het Wolkenbadje", "the Cloud Bath"), "molentje": ("het Maanmolentje", "the Moon Mill")}

P = "gui.guhs.dromen."
TEKSTEN = {  # key -> (Dutch, English)
    "block.guhs.doos_knabbels": ("Doos met knabbels", "Box of Nibbles"),
    "block.guhs.doos_knabbels.lore": ("Een guh kiest altijd de doos. Altijd. Rechtsklik 's avonds: stop je guh erin in.",
                                      "A guh always picks the box. Always. Right-click in the evening: tuck your guh in."),
    "block.guhs.droomzee_tijdelijk": ("Droomzee", "Dream Sea"),
    "item.guhs.droomkussen": ("Droomkussen", "Dream Pillow"),
    "item.guhs.droomkussen.lore": ("In een droom: rechtsklik om wakker te worden. Thuis: gewoon heel zacht.",
                                   "In a dream: right-click to wake up. At home: just very soft."),
    "entity.guhs.wekkerguh": ("Wekkerguh", "Alarm Guh"),
    "biome.guhs.guhdromenland": ("Guhdromenland", "Guh Dreamland"),
    P + "titel": ("Guhdromenland", "Guh Dreamland"),
    P + "wakker.titel": ("Goedemorgen!", "Good morning!"),
    P + "wakker.sub": ("Wat een droom... njeg.", "What a dream... njeg."),
    P + "reis.geen_droom": ("Guhdromenland bestaat niet in deze wereld. Zelfs niet in je dromen.",
                            "Guh Dreamland does not exist in this world. Not even in your dreams."),
    P + "reis.spel_bezig": ("Eerst je spelletje afmaken, dan pas dromen!", "Finish your game first, then you can dream!"),
    P + "reis.kussen": ("Je hebt een droomkussen gekregen: rechtsklik ermee om wakker te worden.",
                        "You got a Dream Pillow: right-click with it to wake up."),
    P + "kussen.thuis": ("Zo zacht... maar je droomt niet. Stop eerst een guh in en ga slapen.",
                         "So soft... but you're not dreaming. Tuck a guh in first and go to sleep."),
    P + "instoppen.met": ("Je stopt %1$s in, met %2$s erbij. Welterusten, njeg! Ga nu zelf slapen in een bed hier vlakbij: "
                          "jullie dromen van %3$s.",
                          "You tuck %1$s in, with %2$s. Sleep tight, njeg! Now go to sleep in a bed close by: you'll dream of %3$s."),
    P + "instoppen.zonder": ("Je stopt %1$s in. Welterusten, njeg! Ga nu zelf slapen in een bed hier vlakbij: jullie dromen van %2$s. "
                             "(Geef iets mee voor een andere droom!)",
                             "You tuck %1$s in. Sleep tight, njeg! Now go to sleep in a bed close by: you'll dream of %2$s. "
                             "(Give something along for another dream!)"),
    P + "instoppen.uit": ("%s is weer wakker. Toch nog even spelen?", "%s is awake again. Playing a bit longer after all?"),
    P + "instoppen.geen_bedtijd": ("Het is nog geen bedtijd, vads! Kom 's avonds terug.", "It's not bedtime yet, vads! Come back in the evening."),
    P + "instoppen.bezet": ("Hier ligt al een guh te slapen. Sst!", "A guh is already sleeping here. Shh!"),
    P + "instoppen.geen_guh": ("Geen guh van jou in de buurt om in te stoppen.", "No guh of yours nearby to tuck in."),
    P + "regels.niet_bouwen": ("In een droom kun je niks bouwen of slopen. Alleen dromen, njeg.",
                               "You can't build or break anything in a dream. Only dream, njeg."),
    P + "regels.bubbel": ("Plop! Een Blub-bubbel heeft je opgevangen.", "Plop! A Blub bubble caught you."),
    P + "wekkerguh.grap0": ("Wekkerguh: TRIIIING! Goedemorgen, slaapkop!", "Alarm Guh: RIIIING! Good morning, sleepyhead!"),
    P + "wekkerguh.grap1": ("Wekkerguh: Nog vijf minuutjes? Nee hoor. TRING!", "Alarm Guh: Five more minutes? Nope. RING!"),
    P + "wekkerguh.grap2": ("Wekkerguh: Ik heb mezelf op 'vahoeg vroeg' gezet. Tring tring!",
                            "Alarm Guh: I set myself to 'vahoeg early'. Ring ring!"),
    P + "wekkerguh.grap3": ("Wekkerguh: Wakker worden! De knabbels worden koud!", "Alarm Guh: Wake up! The nibbles are getting cold!"),
    P + "wekkerguh.grap4": ("Wekkerguh: Sorry, ik moet wel. Het staat in mijn functieomschrijving. TRING!",
                            "Alarm Guh: Sorry, I have to. It's in my job description. RING!"),
}
for kleur in KATTENHUISJES:
    nl_k = {"roze": "Roze", "wit": "Wit", "lavendel": "Lavendel", "mint": "Mint"}[kleur]
    en_k = {"roze": "Pink", "wit": "White", "lavendel": "Lavender", "mint": "Mint"}[kleur]
    TEKSTEN[f"block.guhs.kattenhuisje_{kleur}"] = (f"{nl_k} kattenhuisje", f"{en_k} Cat House")
for eid, (nl, en) in EILANDEN.items():
    TEKSTEN[P + "eiland." + eid] = (nl[0].upper() + nl[1:], en[0].upper() + en[1:])


# =====================================================================================================================
# textures
# =====================================================================================================================
def _ruis(base, var, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base) + (255,))
    return img


def karton(seed):
    img = _ruis((196, 150, 104), 7, seed)
    d = ImageDraw.Draw(img)
    for y in (5, 11):                                  # corrugation lines
        d.line([0, y, 15, y], fill=(176, 130, 88, 255))
    return img


def doos_binnen():
    img = _ruis((172, 126, 84), 6, 3)
    d = ImageDraw.Draw(img)
    for (x, y) in ((3, 3), (11, 4), (6, 10), (12, 12), (2, 12)):   # kaasknabbels on the floor
        d.rectangle([x, y, x + 1, y + 1], fill=(244, 196, 80, 255))
        d.point((x + 1, y + 1), fill=(214, 160, 52, 255))
    return img


def doos_zijkant():
    img = karton(5)
    d = ImageDraw.Draw(img)
    d.rectangle([5, 7, 10, 10], fill=(250, 238, 222, 255))        # a label: a little guh face
    d.point([(6, 8), (9, 8)], fill=(40, 30, 40, 255))
    d.point((7, 9), fill=(240, 140, 170, 255))
    d.point((8, 9), fill=(240, 140, 170, 255))
    return img


def huisje_tex(kleur, deel):
    body, donker, _ = KATTENHUISJES[kleur]
    img = _ruis(body, 5, hash((kleur, deel)) & 0xffff)
    d = ImageDraw.Draw(img)
    if deel == "voor":                                 # a round door, a little guh face above it
        d.ellipse([4, 6, 11, 17], fill=(70, 46, 60, 255))
        d.ellipse([5, 7, 10, 17], fill=(98, 66, 84, 255))
        d.point([(5, 3), (10, 3)], fill=(40, 30, 40, 255))
        d.line([7, 4, 8, 4], fill=(240, 120, 160, 255))
    elif deel == "dak":
        for x in range(0, 16, 4):
            d.line([x, 0, x, 15], fill=donker + (255,))
    else:                                              # side: a little round window
        d.ellipse([5, 4, 10, 9], fill=(190, 226, 250, 255))
        d.line([7, 4, 7, 9], fill=donker + (255,))
    d.rectangle([0, 15, 15, 15], fill=donker + (255,))
    return img


def oortje(kleur):
    body, donker, _ = KATTENHUISJES[kleur]
    img = Image.new("RGBA", (16, 16), body + (255,))
    ImageDraw.Draw(img).rectangle([4, 4, 11, 11], fill=(255, 160, 196, 255))
    return img


def zee():
    rng = random.Random(9)
    img = _ruis((196, 168, 236), 6, 8)
    for _ in range(9):
        x, y = rng.randrange(16), rng.randrange(16)
        img.putpixel((x, y), (255, 246, 255, 255))
    return img


def kussen_icoon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([1, 4, 14, 12], 3, fill=(206, 182, 240, 255), outline=(120, 92, 170, 255))
    d.line([3, 6, 12, 6], fill=(232, 216, 252, 255))
    for (x, y) in ((4, 9), (8, 8), (11, 10)):          # little stars on it
        d.point((x, y), fill=(255, 236, 140, 255))
    d.text((10, 0), "z", fill=(255, 255, 255, 255))
    return img


# =====================================================================================================================
# blocks, items, tags, the dimension
# =====================================================================================================================
def _el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
    e = {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}
    if uv:
        for f in e["faces"].values():
            f["uv"] = uv
    return e


def _facing(name):
    return {"variants": {f"facing={f}": {"model": f"guhs:block/{name}", **({"y": y} if y else {})}
                         for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}


def blokken(h):
    A, D, w = h.A, h.D, h.w
    # doos met knabbels
    h.save(karton(1), "block", "doos_knabbels_karton.png")
    h.save(doos_binnen(), "block", "doos_knabbels_binnen.png")
    h.save(doos_zijkant(), "block", "doos_knabbels_zijkant.png")
    els = [_el([1, 0, 1], [15, 1, 15], "#binnen"),
           _el([1, 1, 1], [15, 8, 2], "#karton"), _el([1, 1, 14], [15, 8, 15], "#zij"),
           _el([1, 1, 2], [2, 8, 14], "#karton"), _el([14, 1, 2], [15, 8, 14], "#karton"),
           _el([2, 8, 1], [7, 8.5, 2], "#karton"),                      # a flap folded open
           _el([3, 1, 3], [5, 2.5, 5], "#knabbel"), _el([10, 1, 9], [12, 2.5, 11], "#knabbel")]
    w(f"{A}/models/block/doos_knabbels.json", {"parent": "minecraft:block/block", "textures": {
        "karton": "guhs:block/doos_knabbels_karton", "binnen": "guhs:block/doos_knabbels_binnen", "zij": "guhs:block/doos_knabbels_zijkant",
        "knabbel": "guhs:block/block_of_kaasknabbels", "particle": "guhs:block/doos_knabbels_karton"}, "elements": els})
    w(f"{A}/blockstates/doos_knabbels.json", _facing("doos_knabbels"))
    w(f"{A}/models/item/doos_knabbels.json", {"parent": "guhs:block/doos_knabbels"})
    h.self_drop("doos_knabbels")
    h.shaped("doos_knabbels", ["P P", "PKP"], {"P": "minecraft:paper", "K": "guhs:kaas_knabbels"}, "guhs:doos_knabbels")
    # kattenhuisjes
    for kleur, (_, _, wol) in KATTENHUISJES.items():
        n = f"kattenhuisje_{kleur}"
        for deel in ("voor", "zij", "dak"):
            h.save(huisje_tex(kleur, deel), "block", f"{n}_{deel}.png")
        h.save(oortje(kleur), "block", f"{n}_oor.png")
        els = [{"from": [1, 0, 1], "to": [15, 12, 15], "faces": {
                   "north": {"texture": "#voor"}, "south": {"texture": "#zij"}, "east": {"texture": "#zij"}, "west": {"texture": "#zij"},
                   "up": {"texture": "#dak"}, "down": {"texture": "#dak"}}},
               _el([0, 12, 0], [16, 14, 16], "#dak"),                                   # the roof slab
               _el([2, 14, 6], [5, 17, 9], "#oor"), _el([11, 14, 6], [14, 17, 9], "#oor")]   # guh ears on the roof
        w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/block", "textures": {
            "voor": f"guhs:block/{n}_voor", "zij": f"guhs:block/{n}_zij", "dak": f"guhs:block/{n}_dak", "oor": f"guhs:block/{n}_oor",
            "particle": f"guhs:block/{n}_zij"}, "elements": els})
        w(f"{A}/blockstates/{n}.json", _facing(n))
        w(f"{A}/models/item/{n}.json", {"parent": f"guhs:block/{n}"})
        h.self_drop(n)
        h.shaped(n, ["W W", "WKW", "PPP"], {"W": wol, "K": "guhs:kaas_knabbels", "P": "#minecraft:planks"}, f"guhs:{n}")
    # the temporary dream sea and the droomkussen
    h.save(zee(), "block", f"{ZEE}.png")
    h.simple_block(ZEE)
    h.save(kussen_icoon(), "item", "droomkussen.png")
    h.item_model("droomkussen")
    h.shapeless("droomkussen", ["guhs:pink_kussen", "minecraft:amethyst_shard"], "guhs:droomkussen")
    # tags
    w(f"{D}/tags/block/guh_slaapplekken.json", {"replace": False, "values": ["guhs:doos_knabbels"] + [f"guhs:{d}" for d in DOZEN]
                                                 + [f"guhs:kattenhuisje_{k}" for k in KATTENHUISJES] + ["guhs:huisje_bedje", "guhs:guhnestje"]})
    for eiland, items in START.items():
        w(f"{D}/tags/item/droom_start/{eiland}.json", {"replace": False, "values": items})


def zee_blok(h):
    """The sea floor of the dimension: part D's guhs:droomzee (a lavender sea you sink through slowly) once it is in this
    tree, else the temporary droomzee_tijdelijk of part A. Run this module again after merging part D."""
    return "guhs:droomzee" if os.path.exists(os.path.join(h.A, "blockstates", "droomzee.json")) else f"guhs:{ZEE}"


def dimensie(h):
    D, w = h.D, h.w
    muziek = {"sound": "guhs:hemel.muziek", "min_delay": 600, "max_delay": 2400, "replace_current_music": True}
    w(f"{D}/worldgen/biome/guhdromenland.json", {
        "attributes": {
            "minecraft:audio/background_music": {"default": muziek, "creative": muziek},
            "minecraft:visual/fog_color": "#e9b4e0",
            "minecraft:visual/sky_color": "#8e6cc8",
            "minecraft:visual/water_fog_color": "#b89ae6",
        },
        "has_precipitation": False,
        "temperature": 0.8,
        "downfall": 0.0,
        "effects": {"water_color": "#c8b0f0", "foliage_color": "#f2a6d2", "grass_color": "#e8b2e6"},
        "spawners": {}, "spawn_costs": {}, "carvers": [], "features": [],
    })
    w(f"{D}/dimension/guhdromenland.json", {"type": "guhs:guhdromenland", "generator": {
        "type": "minecraft:flat",
        "settings": {"biome": "guhs:guhdromenland", "layers": [{"block": zee_blok(h), "height": 1}],
                     "lakes": False, "features": False, "structure_overrides": []}}})
    w(f"{D}/dimension_type/guhdromenland.json", {
        "attributes": {
            "minecraft:audio/background_music": {"default": muziek, "creative": muziek},
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never"},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/ambient_light_color": "#1c1028",
            "minecraft:visual/cloud_color": "#ccffd2ee",
            "minecraft:visual/cloud_height": 64.33,
            "minecraft:visual/fog_color": "#e9b4e0",
            "minecraft:visual/sky_color": "#8e6cc8",
            "neoforge:custom_skybox": "guhs:guhdromenland",   # (droomsfeer: client DroomLucht, moon + stars + falling stars)
        },
        "has_skylight": True,
        "has_ceiling": False,
        "has_ender_dragon_fight": False,
        "coordinate_scale": 1.0,
        "ambient_light": 0.25,
        "min_y": 0,
        "height": 256,
        "logical_height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "monster_spawn_light_level": 0,
        "monster_spawn_block_light_limit": 0,
        "has_fixed_time": True,
        "timelines": "#minecraft:universal",
    })


ITEMS = ["doos_knabbels", "droomkussen", ZEE] + [f"kattenhuisje_{k}" for k in KATTENHUISJES]


def build(h):
    blokken(h)
    dimensie(h)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    A, D = h.A, h.D
    nodig = [f"{D}/dimension/guhdromenland.json", f"{D}/dimension_type/guhdromenland.json", f"{D}/worldgen/biome/guhdromenland.json",
             f"{D}/tags/block/guh_slaapplekken.json"] + [f"{A}/models/item/{n}.json" for n in ITEMS]
    weg = [p for p in nodig if not os.path.exists(p)]
    weg += [k for k, (nl, en) in TEKSTEN.items() if nl.count("%") != en.count("%")]
    if weg:
        raise SystemExit(f"dromen self-check failed: {weg}")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c212_dromen.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
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
    for n in ITEMS:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"dromen: dimension, {len(KATTENHUISJES)} kattenhuisjes, doos, droomkussen, {len(START)} start tags, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
