"""
De guhtuintjes (2.8.0 "Knuffeldal", slice boerderij): grow your own guh plants (see guhs_work28/KNUFFEL_CONTRACT.md, par. 4.5).

  - blocks guh_bloempot and guh_moestuinbak (a guh face on the front; block states facing, plant, groei 0..3, gewaterd):
    multipart models (the pot or bed + dry or wet soil, and the plant in its growth step on top)
  - the three plants knabbelplantje, theekruid, guhbloem (4 steps each); seeds knabbelzaadjes, theekruidzaadjes,
    guhbloemzaadjes; harvests knabbelgraan, theekruid, guhbloemetje; the feestboeket (5 guhbloemetjes: the FEESTBLOEMEN
    task of the Knusfeest); the guh_gieter
  - tags guhs:knus/theekruid, knabbelgraan, guhbloem, oogst, feestbloemen; clothes tuinhoedje, tuinschortje
  - particles gieterdruppel, groeisprankel; sounds tuintjes.gieter, tuintjes.oogst
  - advancements (tab guhs:knuffeldal: tuintjes_eerste_oogst, tuintjes_tuinboek_vol), the Knus section + tuinboek texts,
    FTB quests (row y = 90.5), the game test room
"""
import json
import os

from features import tuintjes_tex as tex

FTB_Y = 90.5
CLOTHES = ["tuinhoedje", "tuinschortje"]
PLANTS = ["knabbelplantje", "theekruid", "guhbloem"]
SEEDS = {"knabbelplantje": "knabbelzaadjes", "theekruid": "theekruidzaadjes", "guhbloem": "guhbloemzaadjes"}
HARVEST = {"knabbelplantje": "knabbelgraan", "theekruid": "theekruid", "guhbloem": "guhbloemetje"}
SOUNDS = {
    "tuintjes.gieter": [{"name": "minecraft:item.bucket.empty", "type": "event", "pitch": 1.7, "volume": 0.6},
                        {"name": "minecraft:block.pointed_dripstone.drip_water", "type": "event", "pitch": 1.2}],
    "tuintjes.oogst": [{"name": "minecraft:block.sweet_berry_bush.pick_berries", "type": "event", "pitch": 1.2}],
}
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# =====================================================================================================================
# clothes
# =====================================================================================================================
_H = [0, 6, -2]
BONES = {
    "outfit_tuinhoedje": ("head", _H, "tuinhoed", [([-7.5, 15.1, -12.5], [15, 0.5, 12], 0), ([-4, 15.6, -9.2], [8, 2.6, 6.4], 0)]),
    "outfit_tuinhoedje_band": ("head", _H, "tuinhoed_band", [([-4, 15.6, -9.2], [8, 0.9, 6.4], 0.12)]),
    "outfit_tuinhoedje_bloem": ("head", _H, "tuinhoed_bloem", [([2.2, 16.2, -10.2], [2.2, 2.2, 1.2], 0), ([2.7, 16.7, -10.5], [1.2, 1.2, 0.4], 0)]),
}


def clothes(rng, v):
    np = __import__("numpy")

    def hoed():
        return v.fabric((236, 214, 150), rng, 8)

    def band():
        return v.fabric((120, 190, 110), rng, 6)

    def bloem():
        a = v.fabric((246, 150, 196), rng, 8)
        a[12:20, 12:20] = (255, 236, 150)
        return a

    def schort():
        a = v.fabric((120, 186, 110), rng, 8)
        a[:, 14:18] = (96, 150, 90)
        a[18:28, 6:14] = (246, 150, 196)                   # a pink pocket with a flower
        a[20:24, 8:12] = (255, 236, 150)
        a[0:3, :] = (250, 240, 220)
        return np.clip(a, 0, 255)

    return {"tuinhoedje": {"tuinhoed": hoed, "tuinhoed_band": band, "tuinhoed_bloem": bloem},
            "tuinschortje": {"suit": schort}}


def icons(ic):
    return {"tuinhoedje": ic.shaped("rim_hat", (170, 150, 90), (236, 214, 150), (120, 190, 110)),
            "tuinschortje": ic.shirt((120, 186, 110), (80, 130, 70), (246, 150, 196), "buttons")}


# =====================================================================================================================
# blocks and items
# =====================================================================================================================
def el(frm, to, texture, faces=None, uv=None):
    return {"from": frm, "to": to, "faces": {f: ({"texture": texture, "uv": uv} if uv else {"texture": texture})
                                              for f in (faces or ("down", "up", "north", "south", "west", "east"))}}


def cross(y0, height, texture, offsets=((8, 8),), size=6.5):
    """Crossed planes (like a plant) standing at y0, `height` high, around each (x, z) offset."""
    els = []
    for (ox, oz) in offsets:
        for ang in (45, -45):
            els.append({"from": [ox - size, y0, oz], "to": [ox + size, y0 + height, oz], "shade": False,
                        "rotation": {"origin": [ox, y0, oz], "axis": "y", "angle": ang, "rescale": True},
                        "faces": {"north": {"texture": texture, "uv": [0, 0, 16, 16]}, "south": {"texture": texture, "uv": [0, 0, 16, 16]}}})
    return els


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # --- the pot: a round-ish pink pot with a guh face on the front (north), soil on top ---
    for nat in (False, True):
        soil = "guhs:block/tuin_aarde_nat" if nat else "guhs:block/tuin_aarde"
        sfx = "_nat" if nat else ""
        w(f"{A}/models/block/guh_bloempot{sfx}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": "guhs:block/guh_bloempot_zijkant", "kant": "guhs:block/guh_bloempot_zijkant", "voor": "guhs:block/guh_bloempot_voorkant",
            "aarde": soil}, "elements": [
            {"from": [3, 0, 3], "to": [13, 6, 13], "faces": {"north": {"texture": "#voor", "uv": [3, 6, 13, 16]}, "south": {"texture": "#kant"},
                                                           "west": {"texture": "#kant"}, "east": {"texture": "#kant"}, "down": {"texture": "#kant"}}},
            el([2.5, 6, 2.5], [13.5, 8, 13.5], "#kant", faces=("north", "south", "west", "east", "down")),
            el([2.5, 8, 2.5], [13.5, 8, 3.5], "#kant", faces=("up",)), el([2.5, 8, 12.5], [13.5, 8, 13.5], "#kant", faces=("up",)),
            el([3.5, 7.5, 3.5], [12.5, 7.5, 12.5], "#aarde", faces=("up",))]})
        w(f"{A}/models/block/guh_moestuinbak{sfx}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": "guhs:block/guh_moestuinbak_zijkant", "kant": "guhs:block/guh_moestuinbak_zijkant",
            "voor": "guhs:block/guh_moestuinbak_voorkant", "aarde": soil}, "elements": [
            {"from": [0, 0, 0], "to": [16, 10, 16], "faces": {"north": {"texture": "#voor", "uv": [0, 3, 16, 13]}, "south": {"texture": "#kant"},
                                                             "west": {"texture": "#kant"}, "east": {"texture": "#kant"}, "down": {"texture": "#kant"},
                                                             "up": {"texture": "#aarde"}}},
            el([0, 10, 0], [16, 11, 1.5], "#kant", faces=("north", "south", "up", "west", "east")),
            el([0, 10, 14.5], [16, 11, 16], "#kant", faces=("north", "south", "up", "west", "east"))]})
    # --- the plants: one model per plant and step, for the pot (one plant) and the bed (a row of three) ---
    for plant in PLANTS:
        for stap in range(4):
            t = f"guhs:block/{plant}_{stap}"
            height = 7 + 3 * stap
            w(f"{A}/models/block/tuin_pot_{plant}_{stap}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                                 "ambientocclusion": False, "textures": {"particle": t, "plant": t},
                                                                 "elements": cross(7.5, height, "#plant")})
            w(f"{A}/models/block/tuin_bak_{plant}_{stap}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                                 "ambientocclusion": False, "textures": {"particle": t, "plant": t},
                                                                 "elements": cross(10, height, "#plant", offsets=((3.5, 8), (8, 8), (12.5, 8)),
                                                                                   size=4.5)})
    for blk, kind in (("guh_bloempot", "pot"), ("guh_moestuinbak", "bak")):
        parts = []
        for f, r in ROT.items():
            for nat in ("false", "true"):
                parts.append({"when": {"facing": f, "gewaterd": nat},
                              "apply": {"model": f"guhs:block/{blk}{'_nat' if nat == 'true' else ''}", **({"y": r} if r else {})}})
        for plant in PLANTS:
            for stap in range(4):
                parts.append({"when": {"plant": plant, "groei": str(stap)}, "apply": {"model": f"guhs:block/tuin_{kind}_{plant}_{stap}"}})
        w(f"{A}/blockstates/{blk}.json", {"multipart": parts})
        w(f"{A}/models/item/{blk}.json", {"parent": f"guhs:block/{blk}"})
        # loot: the pot or bed itself, and the seeds when something is planted (a ripe plant gives its harvest too)
        entries = [{"type": "minecraft:item", "name": f"guhs:{blk}"}]
        pools = [{"rolls": 1, "entries": entries, "conditions": [{"condition": "minecraft:survives_explosion"}]}]
        for plant in PLANTS:
            cond = {"condition": "minecraft:block_state_property", "block": f"guhs:{blk}", "properties": {"plant": plant}}
            pools.append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{SEEDS[plant]}"}], "conditions": [cond]})
            ripe = {"condition": "minecraft:block_state_property", "block": f"guhs:{blk}", "properties": {"plant": plant, "groei": "3"}}
            pools.append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{HARVEST[plant]}", "functions": h.count_fn(1, 2)}],
                          "conditions": [ripe]})
        w(f"{D}/loot_table/blocks/{blk}.json", {"type": "minecraft:block", "pools": pools})
    # --- items ---
    for i in ("knabbelzaadjes", "theekruidzaadjes", "guhbloemzaadjes", "knabbelgraan", "theekruid", "guhbloemetje", "feestboeket"):
        h.item_model(i)
    h.item_model("guh_gieter", parent="minecraft:item/handheld")
    # --- recipes ---
    h.shaped("guh_bloempot", ["T T", " T "], {"T": "minecraft:terracotta"}, "guhs:guh_bloempot", 2)
    h.shaped("guh_moestuinbak", ["PDP", "PPP"], {"P": "#minecraft:planks", "D": "minecraft:dirt"}, "guhs:guh_moestuinbak")
    h.shaped("guh_gieter", ["I  ", "IBI", " I "], {"I": "minecraft:iron_ingot", "B": "minecraft:pink_dye"}, "guhs:guh_gieter")
    for plant, seed in SEEDS.items():
        h.shapeless(f"{seed}_uit_oogst", [f"guhs:{HARVEST[plant]}"], f"guhs:{seed}", 2)
    h.shapeless("knabbelzaadjes_uit_kaasknabbels", ["guhs:kaas_knabbels", "minecraft:wheat_seeds"], "guhs:knabbelzaadjes", 2)
    h.shapeless("guhbloemzaadjes_uit_bloem", ["guhs:roze_guhbloem"], "guhs:guhbloemzaadjes", 2)
    h.shapeless("theekruidzaadjes_uit_gras", ["minecraft:short_grass", "minecraft:short_grass", "minecraft:wheat_seeds"], "guhs:theekruidzaadjes", 1)
    h.shaped("feestboeket", ["GGG", " G ", " G "], {"G": "guhs:guhbloemetje"}, "guhs:feestboeket")
    # --- tags ---
    add = h.add_tag
    add("guhs/tags/item/knus/theekruid", ["guhs:theekruid"])
    add("guhs/tags/item/knus/knabbelgraan", ["guhs:knabbelgraan"])
    add("guhs/tags/item/knus/guhbloem", ["guhs:guhbloemetje"])
    add("guhs/tags/item/knus/oogst", ["guhs:knabbelgraan", "guhs:theekruid", "guhs:guhbloemetje"])
    add("guhs/tags/item/knus/feestbloemen", ["guhs:feestboeket"])
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:guh_bloempot"])
    add("minecraft/tags/block/mineable/axe", ["guhs:guh_moestuinbak"])


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements, texts, the game test room
# =====================================================================================================================
QUEST_ADVANCEMENTS = ["tuintjes_eerste_oogst", "tuintjes_guh_goot", "tuintjes_zang", "tuintjes_feestboeket", "tuintjes_tuinboek_vol"]
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    for name, parent, icon, frame, title, desc in [
        ("tuintjes_eerste_oogst", "root", "guhs:knabbelgraan", "task", "Eigen kweek", "Oogst je eerste plantje uit een guhbloempot of guhmoestuinbak"),
        ("tuintjes_tuinboek_vol", "tuintjes_eerste_oogst", "guhs:guhbloemetje", "goal", "Groene guhvingers",
         "Vul het tuinboek: plant en oogst knabbelplantjes, theekruid en guhbloemen"),
    ]:
        h.w(f"{D}/advancement/knuffeldal/{name}.json", {
            "parent": f"guhs:knuffeldal/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                        "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": IMPOSSIBLE})
        h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
        h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


TEXTS = {
    "block.guhs.guh_bloempot": "Guhbloempot",
    "block.guhs.guh_bloempot.lore": "Doe er zaadjes in en geef water. Rechtsklik als het rijp is!",
    "block.guhs.guh_moestuinbak": "Guhmoestuinbak",
    "block.guhs.guh_moestuinbak.lore": "Een hele rij plantjes in een bak. Tamme guhs geven hem water!",
    "item.guhs.guh_gieter": "Guhgieter",
    "item.guhs.guh_gieter.lore": "Rechtsklik op een tuintje: water! Leeg? Vul hem bij water.",
    "item.guhs.guh_gieter.water": "Water: %s / %s slokjes",
    "item.guhs.knabbelzaadjes": "Knabbelzaadjes",
    "item.guhs.knabbelzaadjes.lore": "Groeit uit tot een knabbelplantje vol knabbelgraan",
    "item.guhs.theekruidzaadjes": "Theekruidzaadjes",
    "item.guhs.theekruidzaadjes.lore": "Groeit uit tot een geurig bosje theekruid",
    "item.guhs.guhbloemzaadjes": "Guhbloemzaadjes",
    "item.guhs.guhbloemzaadjes.lore": "Groeit uit tot een guhbloem (met een gezichtje!)",
    "item.guhs.knabbelgraan": "Knabbelgraan",
    "item.guhs.knabbelgraan.lore": "Knapperig graan voor het knabbeldeeg van de bakker",
    "item.guhs.theekruid": "Theekruid",
    "item.guhs.theekruid.lore": "Ruikt naar mint en kaasknabbels. Voor het theehuis!",
    "item.guhs.guhbloemetje": "Guhbloemetje",
    "item.guhs.guhbloemetje.lore": "Een roze bloemetje dat terugkijkt. Vijf maken een feestboeket",
    "item.guhs.feestboeket": "Feestboeket",
    "item.guhs.feestboeket.lore": "Voor het Grote Knusfeest van Burgemeester Vadsema. VAHOEG!",
    "item.guhs.tuinhoedje": "Tuinhoedje",
    "item.guhs.tuinschortje": "Tuinschortje",
    "subtitles.guhs.tuintjes.gieter": "Gieter klatert",
    "subtitles.guhs.tuintjes.oogst": "Tuintje geoogst",
    "gui.guhs.tuintjes.gieter_leeg": "De gieter is leeg! Rechtsklik op water om hem bij te vullen.",
    "gui.guhs.tuintjes.gieter_vol": "Klok klok klok... de gieter is weer vol!",
    "gui.guhs.tuintjes.feestboeket_klaar": "Wat een feestboeket! Breng het naar Burgemeester Vadsema voor het Grote Knusfeest. VAHOEG!",
    # the Knus tab
    "gui.guhs.knus.mijlpaal.tuintjes_eerste_oogst": "Je eerste oogst",
    "gui.guhs.knus.mijlpaal.tuintjes_gieter": "Twintig keer water gegeven",
    "gui.guhs.knus.mijlpaal.tuintjes_guhs": "Tien keer water van een tamme guh",
    "gui.guhs.knus.mijlpaal.tuintjes_zang": "Vijf plantjes gegroeid van guhgezang",
    "gui.guhs.knus.mijlpaal.tuintjes_oogst": "Dertig keer geoogst",
    "gui.guhs.knus.mijlpaal.tuintjes_feestboeket": "Een feestboeket gemaakt",
    "gui.guhs.knus.verzameling.tuinboek": "Tuinboek",
    "gui.guhs.knus.tuinboek.knabbelzaadjes": "Knabbelzaadjes",
    "gui.guhs.knus.tuinboek.knabbelzaadjes.info": "Plant ze in een guhbloempot of guhmoestuinbak. Water helpt, en guhs die zingen ook!",
    "gui.guhs.knus.tuinboek.knabbelgraan": "Knabbelgraan",
    "gui.guhs.knus.tuinboek.knabbelgraan.info": "Gouden aren vol knapperige kaasknabbelbolletjes. Bakker Korstje maakt er deeg van.",
    "gui.guhs.knus.tuinboek.theekruidzaadjes": "Theekruidzaadjes",
    "gui.guhs.knus.tuinboek.theekruidzaadjes.info": "Klein en groen. Een tamme guh geeft ze graag water, njeg.",
    "gui.guhs.knus.tuinboek.theekruid": "Theekruid",
    "gui.guhs.knus.tuinboek.theekruid.info": "Een bosje muntgroen kruid met witte bloemetjes. Mevrouw Theelepel zet er thee van.",
    "gui.guhs.knus.tuinboek.guhbloemzaadjes": "Guhbloemzaadjes",
    "gui.guhs.knus.tuinboek.guhbloemzaadjes.info": "Roze zaadjes met stipjes. Wat zou eruit komen? Een bloem die terugkijkt!",
    "gui.guhs.knus.tuinboek.guhbloemetje": "Guhbloemetje",
    "gui.guhs.knus.tuinboek.guhbloemetje.info": "Een roze bloem met oortjes en een gezichtje. Vijf maken een feestboeket voor het Knusfeest.",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def test_templates(h):
    t = h.Structure((12, 6, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("tuintjes_test_tuin")


def selfcheck_assets(h):
    A, D = h.A, h.D
    missing = []
    for b in ("guh_bloempot", "guh_moestuinbak"):
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(p):
                missing.append(p)
        for part in json.load(open(f"{A}/blockstates/{b}.json", encoding="utf-8"))["multipart"]:
            m = part["apply"]["model"]
            if not os.path.exists(f"{A}/models/{m[5:]}.json"):
                missing.append(m)
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ("guh_gieter", *SEEDS.values(), *HARVEST.values(), "feestboeket"):
        if not os.path.exists(f"{A}/models/item/{i}.json") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for f in os.listdir(f"{A}/models/block"):
        if f.startswith(("guh_bloempot", "guh_moestuinbak", "tuin_")):
            for t in json.load(open(f"{A}/models/block/{f}", encoding="utf-8")).get("textures", {}).values():
                if t.startswith("guhs:") and not os.path.exists(os.path.join(h.TEX, t[5:] + ".png")):
                    missing.append(f"texture {t} of {f}")
    if missing:
        raise SystemExit(f"tuintjes assets missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    sounds(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck_assets(h)


# =====================================================================================================================
# FTB quests (row y = 90.5, no dependencies)
# =====================================================================================================================
def ftb(fq):
    q, item, adv, y = fq.q, fq.item, fq.adv, FTB_Y
    q("tuintjes_pot", "Een eigen tuintje", "Maak een &dguhbloempot&r (drie terracotta) of een &dguhmoestuinbak&r, of koop er een bij Boerin Hooibaal.",
      "guhs:guh_bloempot", [item("guhs:guh_bloempot")], rewards=(("guhs:knabbelzaadjes", 4),), x=-8, y=y, shape="circle", xp=50)
    q("tuintjes_zaadjes", "Zaadjes", "Zaadjes krijg je bij Boerin Hooibaal, in haar boerderijkist, of van je eigen oogst. Knabbelzaadjes maak je ook "
      "van kaasknabbels en tarwezaad.", "guhs:knabbelzaadjes", [item("guhs:knabbelzaadjes")], rewards=(("guhs:theekruidzaadjes", 4),), x=-6.5, y=y)
    q("tuintjes_gieter", "Guhgieter", "Met een &bguhgieter&r geef je je tuintjes water (3 x 3 tegelijk). Gewaterde plantjes groeien veel sneller! "
      "Leeg? Rechtsklik op water.", "guhs:guh_gieter", [item("guhs:guh_gieter")], rewards=(("guhs:guhbloemzaadjes", 4),), x=-5, y=y)
    q("tuintjes_oogst", "Eigen kweek", "Rechtsklik op een rijp plantje om te oogsten. Het plantje blijft staan en groeit weer aan!",
      "guhs:knabbelgraan", [adv("tuintjes_eerste_oogst")], rewards=(("guhs:kaas_knabbels", 8),), x=-3.5, y=y, xp=100)
    q("tuintjes_graan", "Knabbelgraan", "Oogst knabbelgraan: voor het deeg van de bakker (en voor knabbelvoer).", "guhs:knabbelgraan",
      [item("guhs:knabbelgraan", 4)], rewards=(("guhs:knabbelvoer", 8),), x=-2, y=y)
    q("tuintjes_theekruid", "Theekruid", "Oogst theekruid: Mevrouw Theelepel zet er thee van.", "guhs:theekruid", [item("guhs:theekruid", 4)],
      rewards=(("guhs:kaas_knabbels", 8),), x=-0.5, y=y)
    q("tuintjes_guhbloem", "Guhbloemetjes", "Oogst guhbloemetjes: roze bloemetjes met een gezichtje.", "guhs:guhbloemetje", [item("guhs:guhbloemetje", 5)],
      rewards=(("guhs:kaas_knabbels", 8),), x=1, y=y)
    q("tuintjes_guhs", "Guhs helpen mee", "Tamme guhs die vlakbij je tuintjes rondlopen, geven ze vanzelf water (kijk maar naar de druppeltjes!). "
      "Laat je tamme guhs samen 10 keer een plantje water geven: VAHOEG, die gieterguhs!", "guhs:guh_gieter", [adv("tuintjes_guh_goot")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),),
      x=2.5, y=y, xp=150)
    q("tuintjes_zang", "Zingende tuin", "Plantjes groeien van muziek! Laat een guh zingen (emote Zingen, of het koortje) vlak bij je tuintjes, tot er 5 keer een plantje van gegroeid is. "
      "Na een liedje moet een plantje wel even uitrusten: blijf ook gewoon water geven!",
      "minecraft:note_block", [adv("tuintjes_zang")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=4, y=y, xp=150)
    q("tuintjes_feestboeket", "Feestboeket", "Maak een feestboeket van vijf guhbloemetjes. Burgemeester Vadsema wil er een voor het Grote Knusfeest!",
      "guhs:feestboeket", [item("guhs:feestboeket")], rewards=(("guhs:kaas_knabbels", 12),), x=5.5, y=y, xp=100)
    q("tuintjes_tuinboek", "Groene guhvingers", "Vul het tuinboek in je Guhdex (tab Knus): plant en oogst alle drie de guhplantjes.",
      "guhs:guhbloemetje", [adv("tuintjes_tuinboek_vol")], rewards=(("guhs:vahoege_vads_ingot", 1),), x=7, y=y, shape="gear", xp=300)
