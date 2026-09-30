"""
Het Knuffelbad (2.8.0 "Knuffeldal", slice knuffelbad): a big bath house with three water slides on the guhzee coast.

  - the structure knuffelbad (knuffelbad_bouw.py): the Badhuis (a giant guh head with the bath hall inside, between two
    wings), the pool, the foam bath, three slide towers with guh heads, and the three slides built from the very paths the
    rides follow (knuffelbad_glij.py, written to assets/guhs/knuffelbad/<slide>.json)
  - Badmeester Bubbel (NPC BADMEESTERGUH): his model (swim cap, lifebuoy, whistle), texts, Guhdex page, shop
  - the washing ritual: guh_wastobbe, guhshampoo, guh_fohn (and the shimmer of a washed guh: guh_glans.png)
  - the slides' blocks: glijgoot (8 layers, 5 colours, a moving water film), trechtertegel, glimtegel (glow in the dark:
    a full-bright star layer), badtegel, roze badschuim; the start gates (glijbaan_start); the zwembandje and the ducks
  - eendjesmunt (and in guhs:knus/grijptickets), a rubber duck to keep, the clothes badmutsje and badjasje
  - sounds (knuffelbad_geluid.py makes the OGGs; here only sounds.json), particles, advancements (tab knuffeldal),
    FTB quests (row y = 96.5), lang (Dutch everywhere: knuffelbad_tekst.py), the game test rooms
Run the structure's self-check on its own:  python tools/features/knuffelbad_bouw.py   (from the project root)
"""
import json
import os

from features import knuffelbad_bouw as bouw
from features import knuffelbad_modellen as modellen
from features import knuffelbad_tekst as tekst
from features import knuffelbad_tex as tex

NAME = "knuffelbad"
CLOTHES = ["badmutsje", "badjasje"]
FTB_Y = 96.5
SOUNDS = {"knuffelbad.plons": ["plons1", "plons2"], "knuffelbad.spetter": ["spetter1", "spetter2"], "knuffelbad.glijden": ["glijden"],
          "knuffelbad.eendje_piep": ["piep1", "piep2", "piep3"], "knuffelbad.schuim": ["schuim1", "schuim2"], "knuffelbad.fohn": ["fohn"],
          "knuffelbad.fluit": ["fluit"]}
BLOCKS = ["guh_wastobbe", "glijbaan_start", "glimtegel", "trechtertegel", "knuffelbad_glijgoot", "knuffelbad_schuim", "knuffelbad_badtegel"]
ITEMS = ["eendjesmunt", "guhshampoo", "guh_fohn", "knuffelbad_badeendje"]
QUEST_ADVANCEMENTS = ["knuffelbad_bezocht", "knuffelbad_gewassen", "knuffelbad_roze_trechter", "knuffelbad_glimtunnel", "knuffelbad_grote_plons",
                      "knuffelbad_alle_glijbanen", "knuffelbad_eendjes_100", "knuffelbad_badeendjes_vol", "knuffelbad_record", "seen_badmeesterguh"]

# =====================================================================================================================
# guh clothes (make_guh_variants.py / make_clothes_icons.py): a swim cap with a flower, a fluffy bathrobe
# =====================================================================================================================
_H = [0, 6, -2]   # head pivot
BONES = {
    "outfit_badmutsje": ("head", _H, "badmuts", [([-7.2, 11.6, -11.4], [14.4, 3.6, 11.6], 0.1), ([-5.8, 15.1, -10.0], [11.6, 1.3, 8.8], 0)]),
    "outfit_badmutsje_bloem": ("head", _H, "badmuts_bloem", [([4.2, 14.0, -8.6], [2.6, 2.6, 2.6], 0), ([4.8, 14.6, -9.2], [1.4, 1.4, 0.8], 0)]),
}


def clothes(rng, v):
    np = __import__("numpy")

    def muts():
        a = v.fabric((128, 204, 248), rng, 6)
        for _ in range(10):                                  # little pink bubbles
            x, y = rng.integers(1, 30, 2)
            a[y:y + 2, x:x + 2] = (255, 190, 222)
        a[26:32, :] = (230, 246, 255)                         # the rim
        return np.clip(a, 0, 255)

    def bloem():
        a = v.fabric((255, 150, 200), rng, 6)
        a[12:20, 12:20] = (255, 230, 110)
        return a

    def jas():
        a = v.fabric((248, 178, 212), rng, 12)                 # soft pink towel
        for y in range(0, 32, 4):
            for x in range((y // 4 % 2) * 2, 32, 4):
                a[y, x] = (255, 226, 240)                     # the towel's little loops
        a[:, 14:18] = (236, 140, 186)                         # where it closes
        a[18:21, :] = (255, 244, 250)                         # the belt
        return np.clip(a, 0, 255)

    return {"badmutsje": {"badmuts": muts, "badmuts_bloem": bloem},
            "badjasje": {"suit": jas}}


def icons(ic):
    muts = ic.icon(ic.pad(["....aaaaaaa.....", "...abbbbbbba....", "..abbpbbbbbbap..", "..abbbbbbbbapyp.", ".abbbbbpbbbbap..",
                           ".abbbbbbbbbbba..", ".wwwwwwwwwwwwww."]),
                   {"a": (80, 150, 210), "b": (128, 204, 248), "p": (255, 150, 200), "y": (255, 230, 110), "w": (230, 246, 255)})
    return {"badmutsje": muts, "badjasje": ic.shirt((248, 178, 212), (220, 130, 176), (255, 244, 250), "buttons")}


# =====================================================================================================================
# loot, recipes, tags, sounds
# =====================================================================================================================
def extra(h):
    for b in ("guh_wastobbe", "glimtegel", "trechtertegel", "knuffelbad_glijgoot", "knuffelbad_schuim", "knuffelbad_badtegel"):
        h.self_drop(b)
    h.shaped("guh_wastobbe", ["P P", "PKP", "PPP"], {"P": "#minecraft:planks", "K": "minecraft:pink_dye"}, "guhs:guh_wastobbe", 1)
    h.shaped("trechtertegel", ["PW", "WP"], {"P": "minecraft:pink_concrete", "W": "minecraft:white_concrete"}, "guhs:trechtertegel", 4)
    h.shapeless("knuffelbad_badtegel", ["minecraft:white_concrete", "minecraft:white_concrete", "minecraft:white_concrete", "minecraft:white_concrete",
                                       "minecraft:light_blue_dye"], "guhs:knuffelbad_badtegel", 4)
    h.shaped("glimtegel", ["TTT", "TGT", "TTT"], {"T": "guhs:knuffelbad_badtegel", "G": "minecraft:glow_ink_sac"}, "guhs:glimtegel", 8)
    h.shaped("knuffelbad_glijgoot", ["TT", "TS"], {"T": "guhs:trechtertegel", "S": "minecraft:slime_ball"}, "guhs:knuffelbad_glijgoot", 4)
    h.shapeless("knuffelbad_schuim", ["minecraft:snowball", "minecraft:snowball", "minecraft:pink_dye", "guhs:kaas_knabbels"], "guhs:knuffelbad_schuim", 4)
    h.shapeless("guhshampoo", ["minecraft:glass_bottle", "minecraft:pink_dye", "minecraft:slime_ball"], "guhs:guhshampoo", 1)
    h.shaped("guh_fohn", ["PPI", " R ", " P "], {"P": "minecraft:pink_concrete", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "guhs:guh_fohn", 1)
    h.shapeless("knuffelbad_badeendje", ["minecraft:yellow_wool", "minecraft:orange_dye", "guhs:kaas_knabbels"], "guhs:knuffelbad_badeendje", 2)
    add = h.add_tag
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:glimtegel", "guhs:trechtertegel", "guhs:knuffelbad_glijgoot", "guhs:knuffelbad_badtegel"])
    add("minecraft/tags/block/mineable/axe", ["guhs:guh_wastobbe"])
    add("minecraft/tags/block/mineable/hoe", ["guhs:knuffelbad_schuim"])
    add("guhs/tags/item/knus/grijptickets", ["guhs:eendjesmunt"])        # (a grijpmachine play costs one, contract par. 7)

    def patch(d):
        for event, files in SOUNDS.items():
            d[event] = {"sounds": [f"guhs:knuffelbad/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    impossible = {"done": {"trigger": "minecraft:impossible"}}
    tab = [
        ("knuffelbad_gevonden", "root", "guhs:knuffelbad_badeendje", "task",
         {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{NAME}"}}}}}),
        ("knuffelbad_gewassen", "knuffelbad_gevonden", "guhs:guhshampoo", "goal", impossible),
        ("knuffelbad_glijbaan", "knuffelbad_gevonden", "guhs:eendjesmunt", "goal", impossible),
        ("knuffelbad_alle_glijbanen", "knuffelbad_glijbaan", "guhs:glijbaan_start", "challenge", impossible),
        ("knuffelbad_badeendjes", "knuffelbad_glijbaan", "guhs:knuffelbad_badeendje", "challenge", impossible),
    ]
    for name, parent, icon, frame, crit in tab:
        title, desc = tekst.ADVANCEMENTS[name]
        h.w(f"{D}/advancement/knuffeldal/{name}.json", {
            "parent": f"guhs:knuffeldal/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                        "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": crit})
        h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
        h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


def texts(h):
    for key, text in tekst.LANG.items():
        h.lang(key, text, text)


def test_templates(h):
    """A little bath room for the game tests: bath tiles, a wash tub facing south."""
    s = h.Structure((9, 5, 9))
    for x in range(9):
        for z in range(9):
            s.set(x, 0, z, "guhs:knuffelbad_badtegel")
    s.set(4, 1, 4, "guhs:guh_wastobbe", {"facing": "south", "vulling": "leeg"})
    s.save("knuffelbad_test_wastobbe")


def selfcheck_assets(h):
    """check_assets.py only reads the registry classes: this checks our own blocks, items, entities and paths."""
    A = h.A
    missing = []
    for b in BLOCKS:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS + CLOTHES:
        if not os.path.exists(f"{A}/models/item/{i}.json") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for g in ("zwembandje", "badeendje", "guh_npc_badmeesterguh"):
        if not os.path.exists(f"{A}/geckolib/models/entity/{g}.geo.json"):
            missing.append(f"geo {g}")
    for t in ["zwembandje", "npc_badmeesterguh", "guh_glans"] + [f"badeendje_{e}" for e in modellen.EENDSOORTEN]:
        if not os.path.exists(f"{A}/textures/entity/{t}.png"):
            missing.append(f"texture {t}")
    for sid in bouw.SLIDES:
        if not os.path.exists(f"{A}/knuffelbad/{sid}.json"):
            missing.append(f"path {sid}")
    for files in SOUNDS.values():
        for f in files:
            if not os.path.exists(f"{A}/sounds/knuffelbad/{f}.ogg"):
                missing.append(f"sound {f}")
    for root, _dirs, files in os.walk(f"{A}/blockstates"):
        for f in files:
            if f[:-5] in BLOCKS:
                state = json.load(open(os.path.join(root, f), encoding="utf-8"))
                for v in state["variants"].values():
                    for m in (v if isinstance(v, list) else [v]):
                        path = f"{A}/models/{m['model'].split(':')[1]}.json"
                        if not os.path.exists(path):
                            missing.append(f"model {m['model']}")
                            continue
                        for t in json.load(open(path, encoding="utf-8")).get("textures", {}).values():
                            if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                                missing.append(f"texture {t}")
    if missing:
        raise SystemExit(f"knuffelbad assets missing: {sorted(set(missing))[:40]}")


# =====================================================================================================================
# build
# =====================================================================================================================
def build(h):
    sw_ring, sw_eend = modellen.build(h)
    tex.textures(h, sw_ring, sw_eend, modellen.EENDSOORTEN)
    extra(h)
    advancements(h)
    texts(h)
    # the structure: only on the guhzee coast, anchored in its middle (the pool), everything in one big piece
    h.TEMPLATE_SIZES[NAME] = 40
    h.FLATNESS[NAME] = 14
    h.KEEP_CLEAR[NAME] = 58
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, ["guh_sea"], spacing=18, separation=6, salt=20280701, start_y=-bouw.G, reach=80, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none, "ambient": none})
    _s, _info, summary = bouw.build_all(h, os.path.join(h.A, "knuffelbad"))
    test_templates(h)
    selfcheck_assets(h)
    print(f"knuffelbad: geometry check ok, slides {summary}")


# =====================================================================================================================
# FTB quests (row y = 96.5), no dependencies
# =====================================================================================================================
def ftb(fq):
    q, y = fq.q, FTB_Y
    q("knuffelbad_vinden", "Het Knuffelbad", "Aan de guhzee staat een groot badhuis in de vorm van een guhkop, met drie grote glijbanen: het &bKnuffelbad&r. "
      "Het superkompas (categorie Knus) wijst de weg.", "guhs:knuffelbad_badeendje", [fq.structure(NAME)],
      rewards=(("guhs:kaas_knabbels", 12),), x=-8, y=y, shape="gear", xp=150)
    q("knuffelbad_badmeester", "Badmeester Bubbel", "In de badzaal, in de guhkop, zit &bBadmeester Bubbel&r met zijn fluitje. Tuuut! Praat met hem "
      "en zet hem in je Guhdex.", "guhs:guhdex", [fq.adv("seen_badmeesterguh")], rewards=(("guhs:eendjesmunt", 2),), x=-6.5, y=y, xp=100)
    q("knuffelbad_wassen", "In bad, guh!", "Was je eigen tamme guh in een &dguh-wastobbe&r: guhshampoo erop, schrobben tot het schuimt, douchen en "
      "föhnen. Dan glanst hij een hele dag! (Badmeester Bubbel geeft je de eerste shampoo en föhn.)",
      "guhs:guhshampoo", [fq.adv("knuffelbad_gewassen")], rewards=(("guhs:eendjesmunt", 3),), x=-5, y=y, xp=150)
    q("knuffelbad_roze_trechter", "De Roze Trechter", "Glijd door de twee trechters met een guhgezicht: rondjes draaien, en dan PLOF in het roze schuim.",
      "guhs:trechtertegel", [fq.adv("knuffelbad_roze_trechter")], rewards=(("guhs:eendjesmunt", 3),), x=-3.5, y=y, xp=150)
    q("knuffelbad_glimtunnel", "De Glimtunnel", "Glijd door de donkere buis vol gloeiende sterretjes, rondjes om de toren van de Sterrenguh.",
      "guhs:glimtegel", [fq.adv("knuffelbad_glimtunnel")], rewards=(("guhs:eendjesmunt", 3),), x=-2, y=y, xp=150)
    q("knuffelbad_grote_plons", "De Grote Plons", "Uit de mond van de Reuzeguh, over zijn tong naar beneden en dan door de lucht het bad in. PLONS!",
      "guhs:knuffelbad_glijgoot", [fq.adv("knuffelbad_grote_plons")], rewards=(("guhs:eendjesmunt", 3),), x=-0.5, y=y, xp=150)
    q("knuffelbad_kampioen", "Glijbaankampioen", "Glijd van alle drie de glijbanen van het Knuffelbad.", "guhs:glijbaan_start",
      [fq.adv("knuffelbad_alle_glijbanen")], rewards=(("guhs:vahoege_vads_ingot", 1),), x=1, y=y, shape="gear", xp=300)
    q("knuffelbad_eendjes", "Eendjesvanger", "Pak in totaal 100 badeendjes op de glijbanen (stuur met A en D).", "guhs:knuffelbad_badeendje",
      [fq.adv("knuffelbad_eendjes_100")], rewards=(("guhs:eendjesmunt", 8),), x=2.5, y=y, xp=200)
    q("knuffelbad_verzameling", "Alle badeendjes", "Er drijven twaalf bijzondere badeendjes op de glijbanen, van het Pluiseendje tot het Gouden eendje. "
      "Vind ze allemaal (ze staan in je Guhdex, onder Knus).", "guhs:knuffelbad_badeendje", [fq.adv("knuffelbad_badeendjes_vol")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=4, y=y, shape="gear", xp=500)
    q("knuffelbad_badpakje", "Klaar voor het bad", "Koop het &bbadmutsje&r en het &dbadjasje&r bij Badmeester Bubbel. Kopen is genoeg; trek ze je guh aan en hij waggelt extra knus en vahoeg naar het bad.",
      "guhs:badmutsje", [fq.item("guhs:badmutsje"), fq.item("guhs:badjasje")], rewards=(("guhs:eendjesmunt", 4),), x=5.5, y=y, shape="rsquare", xp=150)
    q("knuffelbad_record", "Topglijder", "Haal 250 punten op één glijbaan (eendjes in een combo tellen extra!).", "minecraft:clock",
      [fq.adv("knuffelbad_record")], rewards=(("guhs:eendjesmunt", 6),), x=7, y=y, xp=300)
