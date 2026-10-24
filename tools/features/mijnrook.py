"""
1.7.0 "Onder de Wol", ronde 37-40 (design: MineScapeStory/ontwerp-guhs170-mijnen.md; Java: feature/mijnhuisje and
feature/ringvlucht/RookPlatform): resources of

  - the Mijn-guhhuisje (block guhs:mijn_guhhuisje: a little pink guh head with a yellow miner's helmet and a lamp; the
    block tag guhs:mijnhuisje_mijnbaar of what its guhs may mine), and its menu texts, the quest Mijnguh-hulpje;
  - the Rugzakzadel (guh clothes, back slot: the guh backpack recoloured to a brown saddle bag with pink straps; recipe;
    the item tag guhs:rugzakzadel_grond of what goes in);
  - the Rookguh-platform (block guhs:rookguh_platform: a round pink-stone landing place with a smoke ring on top) and the
    texts of the two new Rookguh-vlucht destinations.

Standalone and idempotent (writes its own files, adds its values to shared tags, merges its texts into nl_nl/en_us):

    python tools/features/mijnrook.py        (from the project root)
"""
import json
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))


def g(*names):
    return [f"guhs:{n}" for n in names]


# =====================================================================================================================
# textures (deterministic)
# =====================================================================================================================
def _img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def _vlak(kleur, seed, ruis=14):
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.float32)
    a[..., :3] = kleur
    a[..., :3] += rng.integers(-ruis, ruis + 1, (16, 16, 1))
    a[..., 3] = 255
    return a


def _pluis(a, seed, licht=(255, 196, 222)):
    """Little fluffy curls: lighter dots here and there."""
    rng = np.random.default_rng(seed)
    for _ in range(18):
        y, x = rng.integers(0, 16, 2)
        a[y, x, :3] = licht
    return a


ROZE, DONKER_ROZE, OOG = (240, 150, 190), (196, 98, 146), (58, 30, 46)
GEEL, GEEL_DONKER = (242, 196, 58), (196, 146, 30)


def huisje_voor():
    a = _pluis(_vlak(ROZE, 1), 2)
    for y in range(9, 16):                                       # the door: a dark arch
        for x in range(5, 11):
            if not (y == 9 and x in (5, 10)):
                a[y, x, :3] = (70, 40, 52) if y > 9 else (96, 56, 72)
    for x, y in ((4, 4), (11, 4), (4, 5), (11, 5)):              # the eyes
        a[y, x, :3] = OOG
    for x in (3, 12):                                            # blushing cheeks
        a[6, x, :3] = (250, 110, 150)
    a[7, 7, :3] = a[7, 8, :3] = (226, 104, 150)                  # the snoet
    return a


def huisje_zij():
    a = _pluis(_vlak(ROZE, 3), 4)
    for y in range(5, 9):                                        # a round little window, warm light inside
        for x in range(6, 10):
            if not ((y in (5, 8)) and (x in (6, 9))):
                a[y, x, :3] = (255, 226, 150)
    return a


def huisje_onder():
    return _vlak(DONKER_ROZE, 5, 8)


def helm():
    a = _vlak(GEEL, 6, 10)
    for x in range(16):
        a[15, x, :3] = GEEL_DONKER
        if x % 5 == 2:
            a[7, x, :3] = (255, 226, 120)                        # a shiny streak
    return a


def lamp():
    a = _vlak((255, 246, 200), 7, 4)
    a[6:10, 6:10, :3] = (255, 255, 236)
    return a


def platform_boven():
    """Pale pink-grey stone with a ring of white smoke and a little pink cloud in the middle."""
    a = _vlak((206, 182, 196), 8, 10)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.2 <= d <= 6.6:
                a[y, x, :3] = (246, 242, 248) if (x + y) % 3 else (226, 218, 232)
            elif d <= 2.4:
                a[y, x, :3] = (250, 170, 206)
    for x in range(16):
        a[0, x, :3] = a[15, x, :3] = (172, 142, 160)
        a[x, 0, :3] = a[x, 15, :3] = (172, 142, 160)
    return a


def platform_zij():
    a = _vlak((196, 168, 184), 9, 10)
    for x in range(16):
        a[0, x, :3] = (246, 242, 248)                            # the smoke rim
        a[1, x, :3] = (226, 218, 232) if x % 2 else (246, 242, 248)
        a[8, x, :3] = (160, 130, 148)                            # the joints of the stones
    for y in range(2, 16):
        a[y, 0 if y < 8 else 8, :3] = (160, 130, 148)
    return a


def platform_onder():
    return _vlak((168, 140, 156), 10, 8)


def zadel(src):
    """The guh backpack as a brown saddle bag; its brightest pixels (straps, flap edge) pink."""
    a = src.copy()
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            r, gg, b, al = a[y, x]
            if al == 0:
                continue
            v = (r + gg + b) / (3 * 255)
            if v > 0.80:
                a[y, x, :3] = (246, 160, 200)
            else:
                t = 0.45 + 0.75 * v
                a[y, x, :3] = (150 * t + 20, 96 * t + 10, 62 * t + 6)
    return a


def textures(h):
    A = h.A

    def zet(a, *pad):
        p = os.path.join(A, "textures", *pad)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        _img(a).save(p)

    def lees(*pad):
        return np.asarray(Image.open(os.path.join(A, "textures", *pad)).convert("RGBA")).astype(np.float32)
    zet(huisje_voor(), "block", "mijn_guhhuisje_voor.png")
    zet(huisje_zij(), "block", "mijn_guhhuisje_zij.png")
    zet(huisje_onder(), "block", "mijn_guhhuisje_onder.png")
    zet(helm(), "block", "mijn_guhhuisje_helm.png")
    zet(lamp(), "block", "mijn_guhhuisje_lamp.png")
    zet(platform_boven(), "block", "rookguh_platform_boven.png")
    zet(platform_zij(), "block", "rookguh_platform_zij.png")
    zet(platform_onder(), "block", "rookguh_platform_onder.png")
    zet(zadel(lees("item", "guh_backpack.png")), "item", "rugzakzadel.png")
    zet(zadel(lees("entity", "guh_clothes", "guh_backpack.png")), "entity", "guh_clothes", "rugzakzadel.png")


# =====================================================================================================================
# models, loot, tags, recipes
# =====================================================================================================================
def models(h):
    A, w = h.A, h.w
    t = {"particle": "guhs:block/mijn_guhhuisje_zij", "voor": "guhs:block/mijn_guhhuisje_voor", "zij": "guhs:block/mijn_guhhuisje_zij",
         "onder": "guhs:block/mijn_guhhuisje_onder", "helm": "guhs:block/mijn_guhhuisje_helm", "lamp": "guhs:block/mijn_guhhuisje_lamp"}

    def doos(van, tot, vlakken):
        return {"from": van, "to": tot, "faces": {d: {"texture": tex} for d, tex in vlakken.items()}}
    alle = ("north", "south", "east", "west", "up", "down")
    w(f"{A}/models/block/mijn_guhhuisje.json", {"parent": "minecraft:block/block", "textures": t, "elements": [
        doos([2, 0, 2], [14, 10, 14], {"north": "#voor", "south": "#zij", "east": "#zij", "west": "#zij", "up": "#zij", "down": "#onder"}),
        doos([2.5, 10, 2.5], [13.5, 13, 13.5], {d: "#helm" for d in alle}),
        doos([4, 13, 4], [12, 14, 12], {d: "#helm" for d in alle}),
        doos([2, 9.6, 1], [14, 10.4, 2.5], {d: "#helm" for d in alle}),
        doos([6.5, 10.6, 1.8], [9.5, 12.8, 2.5], {d: "#lamp" for d in alle}),
        doos([3, 10, 6], [4.5, 12.5, 9], {d: "#zij" for d in alle}),       # the ears, poking out under the helmet
        doos([11.5, 10, 6], [13, 12.5, 9], {d: "#zij" for d in alle}),
    ]})
    draai = {"north": 0, "east": 90, "south": 180, "west": 270}
    w(f"{A}/blockstates/mijn_guhhuisje.json", {"variants": {f"facing={f}": {"model": "guhs:block/mijn_guhhuisje", **({"y": r} if r else {})}
                                                            for f, r in draai.items()}})
    w(f"{A}/models/item/mijn_guhhuisje.json", {"parent": "guhs:block/mijn_guhhuisje"})
    w(f"{A}/models/block/rookguh_platform.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/rookguh_platform_boven", "side": "guhs:block/rookguh_platform_zij", "bottom": "guhs:block/rookguh_platform_onder"}})
    w(f"{A}/blockstates/rookguh_platform.json", {"variants": {"": {"model": "guhs:block/rookguh_platform"}}})
    w(f"{A}/models/item/rookguh_platform.json", {"parent": "guhs:block/rookguh_platform"})
    h.item_model("rugzakzadel")


def loot(h):
    h.self_drop("mijn_guhhuisje")
    h.self_drop("rookguh_platform")


MIJNBAAR = ["minecraft:pink_wool", "minecraft:magenta_wool", "minecraft:white_wool", "#guhs:onderwol/laagsteen", "#c:ores",
            "#minecraft:base_stone_overworld", "#minecraft:dirt", "minecraft:grass_block", "minecraft:gravel", "minecraft:sand",
            "minecraft:red_sand", "minecraft:sandstone", "minecraft:red_sandstone", "minecraft:clay", "minecraft:snow_block",
            "minecraft:calcite", "minecraft:smooth_basalt", "minecraft:dripstone_block", "minecraft:pink_terracotta",
            "minecraft:magenta_terracotta", "minecraft:pink_concrete_powder", "minecraft:terracotta",
            "guhs:viltkiezel", "guhs:guhklei", "guhs:gatenkaas", "guhs:block_of_kaasknabbels", "guhs:kaasknabbel_stone",
            "guhs:kaasknabbel_dirt", "guhs:kaasknabbel_cobblestone", "guhs:kaasknabbel_deepslate", "guhs:rijpgras", "guhs:knuffelgras",
            "guhs:vadsmos", "guhs:bleekmos", "guhs:kaasmodder", "guhs:modderig_kaasgras", "guhs:gladde_knuffelsteen"]
GROND = ["minecraft:pink_wool", "minecraft:magenta_wool", "minecraft:white_wool", "guhs:viltsteen", "guhs:knuffelsteen",
         "guhs:roze_knuffelsteen", "guhs:guh_kristalsteen", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:stone",
         "minecraft:cobblestone", "minecraft:deepslate", "minecraft:cobbled_deepslate", "minecraft:granite", "minecraft:diorite",
         "minecraft:andesite", "minecraft:tuff", "minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:sandstone",
         "minecraft:clay_ball", "minecraft:snowball", "guhs:guhklei", "guhs:viltkiezel", "guhs:gatenkaas"]


def tags(h):
    h.add_tag("guhs/tags/block/mijnhuisje_mijnbaar", MIJNBAAR)
    h.add_tag("guhs/tags/item/rugzakzadel_grond", GROND)


def recipes(h):
    h.shaped("rugzakzadel", [" V ", "WSW", " C "], {"V": "guhs:vahoegiet", "W": "minecraft:pink_wool", "S": "minecraft:saddle",
                                                    "C": "minecraft:chest"}, "guhs:rugzakzadel")


# =====================================================================================================================
# texts: key -> (English, Dutch)
# =====================================================================================================================
M = "gui.guhs.mijnhuisje."
TEKSTEN = {
    "block.guhs.mijn_guhhuisje": ("Mine Guh House", "Mijn-guhhuisje"),
    "block.guhs.mijn_guhhuisje.lore": ("Up to 4 of your guhs live in it and slowly mine out the ground under it. Click it with a picked-up guh",
                                       "Tot 4 van je guhs wonen erin en minen langzaam de grond eronder leeg. Klik erop met een opgepakte guh"),
    M + "van": ("This is %s's Mine Guh House", "Dit is het mijn-guhhuisje van %s"),
    M + "vol": ("Full! At most %s guhs live in a Mine Guh House", "Vol! In een mijn-guhhuisje wonen hooguit %s guhs"),
    M + "trekt_in": ("%s moves into the Mine Guh House. Pickaxe at the ready, njeg!", "%s trekt in het mijn-guhhuisje. Houweeltje in de aanslag, njeg!"),
    M + "niet_jouw_guh": ("Only your own tamed guhs can live here", "Alleen je eigen tamme guhs kunnen hier wonen"),
    M + "x": ("East and west: %s blocks", "Oost en west: %s blokken"),
    M + "z": ("North and south: %s blocks", "Noord en zuid: %s blokken"),
    M + "bodem": ("Down to y %s", "Tot y %s diep"),
    M + "laat_zien": ("Show", "Laat zien"),
    M + "laat_zien.tooltip": ("Pink sparkles show the edge of the area for a while", "Roze glinsters laten even de rand van het gebied zien"),
    M + "pauze": ("Pause", "Pauze"),
    M + "verder": ("Carry on", "Verder"),
    M + "rugzak_legen": ("Empty bags", "Rugzak legen"),
    M + "rugzak_legen.tooltip": ("The Saddle Bags of the residents go into the chest (as much as fits)",
                                 "De rugzakzadels van de bewoners gaan in de kist (zoveel als past)"),
    M + "uit_huis": ("%s moves out", "%s gaat uit huis"),
    M + "vak.knabbels": ("Cheese nibbles: the guhs work four times as fast (one nibble per 20 blocks)",
                         "Kaasknabbels: de guhs werken vier keer zo snel (een knabbel per 20 blokken)"),
    M + "vak.sleutel": ("A Bank Key: what is mined goes into that Bank Guh", "Een Banksleutel: wat gemined wordt gaat in die Bank Guh"),
    M + "status.0": ("No guhs live here yet. Click with a picked-up guh!", "Er wonen nog geen guhs. Klik met een opgepakte guh!"),
    M + "status.1": ("Busy mining: %s of %s guhs at work", "Druk aan het minen: %s van de %s guhs aan het werk"),
    M + "status.2": ("Paused: the chest is full!", "Pauze: de kist is vol!"),
    M + "status.3": ("All done! Nothing left to mine", "Klaar! Er valt niks meer te minen"),
    M + "status.4": ("Nobody is here to work", "Er is niemand in de buurt om te werken"),
    M + "status.5": ("Paused", "Even pauze"),
    M + "status.6": ("Put a chest next to the house, or a Bank Key in it", "Zet een kist naast het huisje, of een Banksleutel erin"),
    M + "status.7": ("Waiting: part of the area isn't loaded", "Even wachten: een stukje van het gebied is niet geladen"),
    M + "status.8": ("Too deep: the guhs never mine below y 10", "Te diep: de guhs minen nooit onder y 10"),
    M + "knabbels": ("Nibbles", "Knabbels"),
    M + "knabbels.aan": ("Nibbles: fast!", "Knabbels: snel!"),
    M + "bank": ("Bank Guh", "Bank Guh"),
    M + "bank.aan": ("Bank Guh: linked", "Bank Guh: gekoppeld"),
    M + "bewoners": ("Guhs %s/%s", "Guhs %s/%s"),
    M + "geen_bewoners": ("Nobody yet", "Nog niemand"),
    M + "info": ("Mined: %s  Working at y %s", "Gemined: %s  Bezig op y %s"),
    M + "opslag": ("Chest: %s %s", "Kist: %s %s"),
    M + "ja": ("yes", "ja"),
    M + "nee": ("no", "nee"),
    M + "rugzak": ("Bags: %s", "Rugzak: %s"),
    "item.guhs.rugzakzadel": ("Saddle Bag", "Rugzakzadel"),
    # the quest Mijnguh-hulpje
    "quest.guhs.mijnhulpje.vraag": ("Hey, you there! Fancy giving me a hand? I'm building a little house for guhs that like to dig, but I'm short of a few things.",
                                    "Hee, jij daar! Help je me even? Ik bouw een huisje voor guhs die graag graven, maar ik kom wat spulletjes tekort."),
    "quest.guhs.mijnhulpje.vraag.2": ("Bring me %s felt stone, a lump of guh clay and a Vahoegite. Then the Mine Guh House is yours, with a little helmet for your guh!",
                                      "Breng me %s viltsteen, een guhklei en een Vahoegiet. Dan is het mijn-guhhuisje van jou, met een helmpje voor je guh!"),
    "quest.guhs.mijnhulpje.nog_niet": ("Not everything yet: felt stone %s/%s, guh clay %s/1, Vahoegite %s/1. Dig on, njeg!",
                                       "Nog niet alles: viltsteen %s/%s, guhklei %s/1, Vahoegiet %s/1. Graaf maar door, njeg!"),
    "quest.guhs.mijnhulpje.klaar": ("VAHOEG, that's everything! Here's your Mine Guh House. Put it on or under the ground and let your guhs move in.",
                                    "VAHOEG, dat is alles! Hier is je mijn-guhhuisje. Zet het op of onder de grond en laat je guhs erin trekken."),
    "quest.guhs.mijnhulpje.klaar.2": ("And this little lamp helmet is for your guh. Give the diggers some nibbles and they'll work four times as fast!",
                                      "En dit lampenhelmpje is voor je guh. Geef de gravers wat knabbels, dan werken ze vier keer zo snel!"),
    "quest.guhs.mijnhulpje.nog_een": ("Another one? Coming right up! Here's another Mine Guh House.",
                                      "Nog eentje? Komt eraan! Hier is nog een mijn-guhhuisje."),
    # the Rookguh-platform and the two new destinations
    "block.guhs.rookguh_platform": ("Smoke Guh Platform", "Rookguh-platform"),
    "block.guhs.rookguh_platform.lore": ("Your own landing place for the Smoke Guh, in any world. Click it: he flies you anywhere",
                                         "Je eigen landingsplek voor de Rookguh, in elke wereld. Klik erop: hij vliegt je overal heen"),
    "gui.guhs.ringvlucht.plek.guhrio": ("The Super Guhrio castle", "Het Super Guhrio-kasteel"),
    "gui.guhs.ringvlucht.plek.platform": ("Your Smoke Guh Platform", "Jouw Rookguh-platform"),
    "gui.guhs.ringvlucht.guhrio_eerst": ("Finish world 1-1 first!", "Speel eerst wereld 1-1 uit!"),
    "gui.guhs.ringvlucht.wereld.guheinde": ("Guheinde", "Guheinde"),
    "gui.guhs.ringvlucht.wereld.overworld": ("Overworld", "Bovenwereld"),
    "gui.guhs.ringvlucht.wereld.nether": ("Nether", "Nether"),
    "gui.guhs.ringvlucht.wereld.end": ("The End", "The End"),
    "gui.guhs.ringvlucht.wereld.elders": ("Elsewhere", "Ergens anders"),
    "quest.guhs.ringvlucht.platform.geschenk": ("You did it, the whole journey of the Nibble Ring! Here's a Smoke Guh Platform of your very own. Put it wherever you like and I'll always fly you there.",
                                                "Het is je gelukt, de hele reis van de Knabbelring! Hier heb je een eigen Rookguh-platform. Zet het neer waar je wilt, dan vlieg ik je er altijd heen."),
    "quest.guhs.ringvlucht.platform.geschenk_later": ("Hi, it's me, the Smoke Guh! Because you finished the whole journey of the Nibble Ring, here's a Smoke Guh Platform of your very own. Put it wherever you like and I'll always fly you there. Njeg!",
                                                      "Hoi, ik ben het, de Rookguh! Omdat jij de hele reis van de Knabbelring hebt uitgespeeld, krijg je een eigen Rookguh-platform. Zet het neer waar je wilt, dan vlieg ik je er altijd heen. Njeg!"),
    "quest.guhs.ringvlucht.platform.geschenk.2": ("(One per player, in any world: put it down somewhere else and your landing place moves along. Click it to fly.)",
                                                  "(Eén per speler, in elke wereld: zet je hem ergens anders neer, dan verhuist je landingsplek mee. Klik erop om te vliegen.)"),
    "quest.guhs.ringvlucht.platform.neergezet": ("This is your Smoke Guh Platform now. The Smoke Guh will land here!", "Dit is nu jouw Rookguh-platform. Hier landt de Rookguh!"),
    "quest.guhs.ringvlucht.platform.verhuisd": ("Your Smoke Guh Platform has moved here. The old one doesn't count any more.",
                                                "Je Rookguh-platform is hierheen verhuisd. Het oude telt niet meer."),
    "quest.guhs.ringvlucht.platform.opgeruimd": ("Your Smoke Guh Platform is gone. Put one down again for a landing place.",
                                                 "Je Rookguh-platform is weg. Zet er weer een neer voor een landingsplek."),
    "quest.guhs.ringvlucht.platform.te_vroeg": ("The Smoke Guh only knows you once you've finished the journey of the Nibble Ring.",
                                                "De Rookguh kent je pas als je de reis van de Knabbelring hebt uitgespeeld."),
    "quest.guhs.ringvlucht.platform.weg": ("Hmm, your platform isn't there any more. Put it down again!", "Hmm, je platform staat er niet meer. Zet hem opnieuw neer!"),
}


def texts(h):
    for k, (en, nl) in TEKSTEN.items():
        h.lang(k, en, nl)


def build(h):
    textures(h)
    models(h)
    loot(h)
    tags(h)
    recipes(h)
    texts(h)


ITEMS = ["mijn_guhhuisje", "rookguh_platform", "rugzakzadel"]


# (FTB quests: mijnhuisje_hulpje in kaasmijn.py's ftb(), ringvlucht_platform in ring_vlucht.py's ftb(); both modules are in features.FEATURES)


def _direct():
    import types
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
    NL, EN = {}, {}

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def add_tag(path, values):
        full = f"{R}/data/{path}.json"
        data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
        for v in values:
            if v not in data["values"]:
                data["values"].append(v)
        w(full, data)

    def item_model(name, tex_=None, parent="minecraft:item/generated"):
        w(f"{A}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": tex_ or f"guhs:item/{name}"}})

    def self_drop(name, item=None):
        w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{item or name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    def shaped(name, pattern, key, result, count=1):
        w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": dict(key),
                                      "result": {"id": result, "count": count}})

    def lang(key, en, nl):
        EN[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=R, A=A, D=D, w=w, add_tag=add_tag, item_model=item_model, self_drop=self_drop, shaped=shaped, lang=lang)
    build(h)
    for i in ITEMS:
        w(f"{A}/items/{i}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{i}"}})
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    print(f"mijnrook: {len(ITEMS)} items, {len(NL)} texts")


if __name__ == "__main__":
    _direct()
