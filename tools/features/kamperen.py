"""
De kampeerplekjes (2.8.0 "Knuffeldal", slice "buiten"; see guhs_work28/KNUFFEL_CONTRACT.md): little campsites in the
Knuffeldal and on the Guhweides, where Opa Guh tells a campfire story every night (twelve different ones, the
verhalenbundel), the guhs wear pyjamas around the fire, and you can sleep in a guh_slaapzak without a bed.

  - the loose structure kampeerplekje (kamperen_bouw.py: 25 x 16 x 25, geometry self-check)
  - the block guh_slaapzak (a bed-like block: sleep, no spawn point) and the effect uitgerust
  - Opa Guh (OPA_GUH): his own model (a nightcap with a pompom, round glasses, a white moustache) and texture
  - the clothes pyjama_pakje (outfit_suit) and slaapmutsje (outfit_slaapmutsje*), also drawn on every guh with the
    PYJAMA flag at night by a campfire (KamperenClient)
  - the stories (kamperen_verhalen.py), the Knus section "kamperen", advancements, FTB row y = 95, lang (all Dutch)
"""
import math
import random

import numpy as np
from PIL import Image

from features import kamperen_bouw as bouw
from features import kamperen_verhalen as verhalen
from features import sterrenwacht_hulp as hulp

NAME = bouw.NAME
CLOTHES = ["pyjama_pakje", "slaapmutsje"]
FTB_Y = 95
PYJAMA = (170, 196, 246)
PYJAMA_STREEP = (250, 250, 255)
MUTS = (122, 150, 226)

_H = [0, 6, -2]   # head pivot
BONES = {
    # the slaapmutsje: a floppy striped nightcap (the ears poke through) with a white pompom at its tip
    "outfit_slaapmutsje": ("head", _H, "slaapmutsje", [([-6.6, 14.2, -11.4], [13.2, 2.4, 10.6], 0), ([-4.8, 16.6, -9.8], [9.6, 2.4, 7.6], 0),
                                                       ([-3.2, 19.0, -8.4], [6.4, 2.0, 5.2], 0), ([0.2, 20.2, -7.6], [4.0, 1.8, 3.6], 0),
                                                       ([3.0, 19.0, -7.2], [2.4, 1.6, 2.8], 0)]),
    "outfit_slaapmutsje_pompon": ("head", _H, "slaapmutsje_pompon", [([4.4, 17.2, -7.6], [3.0, 3.0, 3.0], 0.1)]),
}


def clothes(rng, v):
    def pyjama():
        a = v.stripes(PYJAMA, PYJAMA_STREEP, rng, 4)
        for _ in range(10):                             # little yellow stars and moons
            x, y = rng.integers(1, 29, 2)
            a[y:y + 2, x:x + 2] = (255, 226, 110)
        a[:, 14:18] = (120, 140, 210)                   # buttons strip
        for y in range(3, 32, 7):
            a[y:y + 2, 15:17] = (255, 255, 255)
        return np.clip(a, 0, 255)

    def muts():
        a = v.stripes(MUTS, PYJAMA_STREEP, rng, 5)
        return np.clip(a, 0, 255)

    def pompon():
        return v.fabric((252, 252, 255), rng, 6)
    return {"pyjama_pakje": {"suit": pyjama}, "slaapmutsje": {"slaapmutsje": muts, "slaapmutsje_pompon": pompon}}


def icons(ic):
    return {"pyjama_pakje": ic.shirt(PYJAMA, (100, 120, 190), PYJAMA_STREEP, "stripes"),
            "slaapmutsje": ic.shaped("santa", (70, 90, 170), MUTS)}


# =====================================================================================================================
# textures, blocks
# =====================================================================================================================
def textures(h):
    rng = random.Random(28802)
    save = h.save
    zak = hulp.noisy((246, 160, 200), 6, rng)
    zp = zak.load()
    for x in range(16):
        for y in range(16):
            if x % 5 == 2 or y % 5 == 2:                # quilted
                c = zp[x, y]
                zp[x, y] = (max(0, c[0] - 26), max(0, c[1] - 30), max(0, c[2] - 26), 255)
    for (x, y) in ((4, 4), (11, 9), (6, 12)):          # tiny stars
        zp[x, y] = (255, 236, 130, 255)
    save(zak, "block", "guh_slaapzak.png")
    rand = hulp.noisy((255, 236, 244), 4, rng)
    save(rand, "block", "guh_slaapzak_rand.png")
    kap = hulp.noisy((255, 200, 222), 4, rng)
    save(kap, "block", "guh_slaapzak_kap.png")
    gezicht = hulp.noisy((255, 200, 222), 4, rng)
    hulp.paint_face(gezicht, (0, 0, 16, 16), 1)         # a sleepy guh face
    save(gezicht, "block", "guh_slaapzak_gezicht.png")
    oor = hulp.noisy((246, 160, 200), 5, rng)
    for x in range(5, 11):
        for y in range(5, 11):
            oor.putpixel((x, y), (214, 90, 170, 255))
    save(oor, "block", "guh_slaapzak_oor.png")
    # the effect icon: a sleepy guh face with a little Z
    icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    ip = icon.load()
    for x in range(18):
        for y in range(18):
            if ((x - 8.5) / 7.5) ** 2 + ((y - 10) / 6.5) ** 2 <= 1:
                ip[x, y] = (255, 196, 220, 255)
    for (x, y) in ((3, 4), (4, 3), (13, 3), (14, 4), (3, 5), (14, 5)):
        ip[x, y] = (246, 150, 196, 255)
    for (x, y) in ((4, 10), (5, 11), (6, 11), (7, 10), (10, 10), (11, 11), (12, 11), (13, 10)):
        ip[x, y] = (60, 40, 70, 255)
    ip[8, 13] = ip[9, 13] = (200, 90, 120, 255)
    for (x, y) in ((13, 0), (14, 0), (15, 0), (14, 1), (13, 2), (14, 2), (15, 2)):
        ip[x, y] = (120, 150, 230, 255)
    save(icon, "mob_effect", "uitgerust.png")
    # a campfire spark: two frames
    frames = []
    for i in range(2):
        img = Image.new("RGBA", (4, 4), (0, 0, 0, 0))
        p = img.load()
        pts = [(1, 1), (2, 1), (1, 2), (2, 2)] if i == 0 else [(1, 1), (2, 2), (1, 2)]
        for (x, y) in pts:
            p[x, y] = (255, 200 - 60 * i, 80, 255)
        p[1, 1] = (255, 250, 200, 255)
        frames.append(img)
    hulp.particle_frames(h, "kampvuurvonkje", frames)


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    el = hulp.el
    t = {"particle": "guhs:block/guh_slaapzak", "zak": "guhs:block/guh_slaapzak", "rand": "guhs:block/guh_slaapzak_rand",
         "kap": "guhs:block/guh_slaapzak_kap", "gezicht": "guhs:block/guh_slaapzak_gezicht", "oor": "guhs:block/guh_slaapzak_oor"}
    kap = {"from": [1, 0, 0.5], "to": [15, 4.5, 5.5], "faces": {"north": {"texture": "#kap"}, "south": {"texture": "#kap"},
                                                                "east": {"texture": "#kap"}, "west": {"texture": "#kap"},
                                                                "up": {"texture": "#gezicht", "rotation": 180}, "down": {"texture": "#kap"}}}
    w(f"{A}/models/block/guh_slaapzak.json", {"parent": "minecraft:block/block", "textures": t, "elements": [
        el([1, 0, 5], [15, 3, 16], "#zak"), el([1, 3, 5], [15, 3.6, 8.5], "#rand"), kap,
        el([2, 4.5, 1.5], [5, 7.5, 3.5], "#oor"), el([11, 4.5, 1.5], [14, 7.5, 3.5], "#oor")]})
    w(f"{A}/blockstates/guh_slaapzak.json", {"variants": hulp.facing_variants(None, {
        "occupied=false": "guhs:block/guh_slaapzak", "occupied=true": "guhs:block/guh_slaapzak"})})
    w(f"{A}/models/item/guh_slaapzak.json", {"parent": "guhs:block/guh_slaapzak"})
    h.self_drop("guh_slaapzak")
    h.shaped("guh_slaapzak", ["WWW", "PPP"], {"W": "#guhs:knus/pluiswol", "P": "#minecraft:wool"}, "guhs:guh_slaapzak", 1)
    h.shaped("guh_slaapzak_wol", ["PKP", "PPP"], {"P": "#minecraft:wool", "K": "guhs:kaas_knabbels"}, "guhs:guh_slaapzak", 1)
    w(f"{D}/loot_table/chests/{NAME}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": h.count_fn(6, 14)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 3}, "entries": [
            {"type": "minecraft:item", "name": "guhs:guh_slaapzak", "weight": 2},
            {"type": "minecraft:item", "name": "minecraft:stick", "weight": 3, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:lantern", "weight": 2},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 2, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:cookie", "weight": 3, "functions": h.count_fn(2, 5)}]}]})


# =====================================================================================================================
# Opa Guh: grey-pink, a nightcap with a pompom, round glasses, a white moustache
# =====================================================================================================================
def npc(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_opa_guh")
    sw = hulp.swatches(geo, ["muts", "pompon", "bril", "snor"])
    c = hulp.cube
    geo["bones"].append({"name": "opa_muts", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-6.8, 24.4, -7.2], [13.6, 2.2, 12.4], sw["muts"]), c([-5.0, 26.6, -6.0], [10, 2.4, 9.6], sw["muts"]),
        c([-3.4, 29.0, -5.0], [6.8, 2.2, 7.2], sw["muts"])]})
    geo["bones"].append({"name": "opa_mutspunt", "parent": "opa_muts", "pivot": [0, 31.2, -1.4], "cubes": [
        c([-1.6, 31.2, -3.0], [3.2, 3.0, 3.2], sw["muts"]), c([-2.0, 34.2, -3.4], [4.0, 3.6, 4.0], sw["pompon"], inflate=0.1)]})
    bril = []
    for sx in (-1, 1):
        x = sx * 3.8
        bril += [c([x - 2.8, 22.3, -7.45], [5.6, 0.5, 0.4], sw["bril"]), c([x - 2.8, 17.6, -7.45], [5.6, 0.5, 0.4], sw["bril"]),
                 c([x - 2.8, 18.1, -7.45], [0.5, 4.2, 0.4], sw["bril"]), c([x + 2.3, 18.1, -7.45], [0.5, 4.2, 0.4], sw["bril"])]
    bril.append(c([-1.0, 20.6, -7.45], [2.0, 0.5, 0.4], sw["bril"]))
    geo["bones"].append({"name": "opa_bril", "parent": "head", "pivot": [0, 20, -7], "cubes": bril})
    geo["bones"].append({"name": "opa_snor", "parent": "head", "pivot": [0, 16, -7.8], "cubes": [
        c([-3.4, 15.4, -8.0], [6.8, 1.3, 0.9], sw["snor"]), c([-4.4, 15.9, -7.9], [1.2, 1.2, 0.7], sw["snor"]),
        c([3.2, 15.9, -7.9], [1.2, 1.2, 0.7], sw["snor"])]})
    hulp.save_geo(h, "guh_npc_opa_guh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.93, sat=0.28, val=0.93)
    rng = np.random.default_rng(28803)

    def streep(block):
        for x in range(32):
            if (x // 5) % 2:
                block[:, x, :3] = PYJAMA_STREEP
    hulp.paint_swatch(a, sw["muts"], MUTS, rng, 6, streep)
    hulp.paint_swatch(a, sw["pompon"], (252, 252, 255), rng, 6)
    hulp.paint_swatch(a, sw["bril"], (196, 170, 90), rng, 6)
    hulp.paint_swatch(a, sw["snor"], (250, 248, 244), rng, 8)
    h.save(Image.fromarray(a), "entity", "npc_opa_guh.png")


# =====================================================================================================================
# sounds, advancements, lang
# =====================================================================================================================
SOUNDS = {"kamperen.opa_verhaal": [{"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 0.62, "volume": 0.8}]}
QUEST_ADVANCEMENTS = ["seen_opa_guh", "kamperen_opa", "kamperen_eerste_verhaal", "kamperen_alle_verhalen", "kamperen_verhalenbundel_vol",
                      "kamperen_slaapzak", "kamperen_pyjamafeest", "kamperen_marshmallow"]

LANG = {
    "block.guhs.guh_slaapzak": "Guh-slaapzak",
    "block.guhs.guh_slaapzak.lore": "Slaap zonder bed! (Je spawnpunt blijft waar het was.) Na een hele nacht: uitgerust",
    "effect.guhs.uitgerust": "Uitgerust",
    "item.guhs.pyjama_pakje": "Pyjamapakje", "item.guhs.slaapmutsje": "Slaapmutsje",
    "entity.guhs.guh_npc.opa_guh": "Opa Guh",
    "structure.guhs.kampeerplekje": "Kampeerplekje",
    "structure.guhs.kampeerplekje.tooltip": "Een kampvuur met guhtentjes: 's nachts vertelt Opa Guh er een verhaal",
    "gui.guhs.guhdex.rarity.opa_guh": "Zeldzaamheid: Zeldzaam (kampeerplekjes, en op zijn bankje in het Knuffeldal)",
    "gui.guhs.guhdex.info.opa_guh": "Zit het liefst bij het kampvuur en vertelt elke nacht een verhaal van vroeger: over Mika's, kaasknabbels "
                                    "en waarom guhs zulke grote oren hebben. Twaalf verhalen, één per nacht. Njeg, welterusten!",
    "subtitles.guhs.kamperen.opa_verhaal": "Opa Guh vertelt",
    "gui.guhs.kamperen.beschermd": "Njeg! Dit kampeerplekje is van Opa Guh. Hier niks slopen of bouwen!",
    "gui.guhs.kamperen.slaapzak_bezet": "Er ligt al iemand in deze slaapzak. Zzz...",
    "gui.guhs.kamperen.geen_thuis": "Een slaapzak is geen thuis: je spawnpunt blijft waar het was.",
    "gui.guhs.kamperen.uitgerust": "Wat heb je lekker geslapen in je slaapzak! Je voelt je helemaal uitgerust. VAHOEG!",
    "quest.guhs.kamperen.hallo": "Hé, hallo daar, kleintje. Ik ben Opa Guh. Overdag doe ik een dutje, maar 's avonds bij het kampvuur "
                                 "vertel ik verhalen. Elke nacht een ander. Kom je vanavond luisteren? Neem een slaapzak mee, njeg.",
    "quest.guhs.kamperen.tip0": "Zzz... hè? O, het is nog dag. Kom vanavond terug, als het kampvuur brandt. Dan vertel ik een verhaal.",
    "quest.guhs.kamperen.tip1": "In een slaapzak slaap je net zo lekker als in een bed. En je wordt heel uitgerust wakker, echt waar.",
    "quest.guhs.kamperen.tip2": "'s Nachts trekken de guhs bij het kampvuur hun pyjama aan. Zo schattig. Ik heb er zelf ook een, sst.",
    "quest.guhs.kamperen.tip3": "Twaalf verhalen ken ik. Mijn opa kende er dertien, maar dat laatste is hij vergeten. Njeg.",
    "quest.guhs.kamperen.tip4": "Sluip maar eens naar me toe (sluip + praten): dan laat ik je mijn slaapzakken en pyjama's zien.",
    "quest.guhs.kamperen.winkel": "Kijk maar, kleintje. Slaapzakken, slaapmutsjes en pyjama's. Voor een paar kaasknabbels, njeg.",
    "quest.guhs.kamperen.begin": "Kom maar dichterbij, bij het vuur. Het verhaal van vannacht heet: %s.",
    "quest.guhs.kamperen.aanschuiven": "Schuif maar aan, kleintje. We zijn al begonnen. Sst...",
    "quest.guhs.kamperen.te_laat": "O, je bent laat, kleintje! Het verhaal is al bijna uit. Luister maar mee, maar de kaasknabbels zijn voor wie het "
                                   "hele verhaal hoort. Kom straks nog eens terug voor een nieuw verhaal, njeg.",
    "quest.guhs.kamperen.einde_half": "En dat was het verhaal. Jij hoorde alleen het staartje, hè? Vraag me straks maar om een heel verhaal, njeg.",
    "gui.guhs.kamperen.marshmallow": "Je roostert een marshmallowknabbel boven het kampvuur... goudbruin en kleverig. Mmm, VAHOEG!",
    "gui.guhs.kamperen.marshmallow_overdag": "Marshmallows roosteren doe je 's avonds bij het kampvuur, njeg. Nog even wachten!",
    "quest.guhs.kamperen.marshmallow0": "Mmm, ruik ik daar een marshmallowknabbel? Niet te dicht bij het vuur, kleintje, anders wordt hij zwart. Njeg.",
    "quest.guhs.kamperen.marshmallow1": "Vroeger roosterden wij kaasknabbels aan een stokje. Tot de Mika's ze kwamen pikken... Eet maar gauw op!",
    "quest.guhs.kamperen.marshmallow2": "Kleverige snoet? Dat hoort zo bij een kampvuur. Zo word je lekker VAHOEG, njeg.",
    "quest.guhs.kamperen.marshmallow_sst": "(Opa Guh snuift even, knipoogt en vertelt dan zachtjes verder...)",
    "quest.guhs.kamperen.sst": "Sst... luister maar. Het verhaal is nog niet uit.",
    "quest.guhs.kamperen.morgen": "Eén verhaal per nacht, kleintje. Morgen vertel ik je weer een nieuwe. Nu lekker slapen... zzz.",
    "quest.guhs.kamperen.geen_vuur": "Waar is het kampvuur gebleven? Zonder kampvuur geen verhaal, njeg. Steek je er een aan?",
    "quest.guhs.kamperen.einde_nieuw": "En dat was het verhaal. Het staat nu in je verhalenbundel. Hier, %s kaasknabbels voor onder je kussen.",
    "quest.guhs.kamperen.einde": "En dat was het verhaal. Die kende je al, hè? Toch mooi. Hier, %s kaasknabbels.",
    # the Knus tab
    "gui.guhs.knus.verzameling.verhalenbundel": "Verhalenbundel",
    "gui.guhs.knus.mijlpaal.kamperen_gevonden": "Praat met Opa Guh",
    "gui.guhs.knus.mijlpaal.kamperen_eerste_verhaal": "Je eerste kampvuurverhaal",
    "gui.guhs.knus.mijlpaal.kamperen_zes_verhalen": "6 verhalen in je bundel",
    "gui.guhs.knus.mijlpaal.kamperen_alle_verhalen": "Alle 12 verhalen",
    "gui.guhs.knus.mijlpaal.kamperen_uitgeslapen": "Een hele nacht in een slaapzak",
    "gui.guhs.knus.mijlpaal.kamperen_marshmallows": "5 marshmallowknabbels geroosterd bij het kampvuur",
    "gui.guhs.knus.mijlpaal.kamperen_pyjamafeest": "Pyjamafeest: 5 guhs in pyjama bij een verhaal",
}


def advancements(h):
    hulp.quest_advancements(h, QUEST_ADVANCEMENTS)
    adv = hulp.display_advancement
    adv(h, "kamperen_gevonden", "root", "minecraft:campfire", "task", hulp.in_structure(NAME),
        "Kampvuurtje", "Vind een kampeerplekje in het Knuffeldal of op de Guhweides")
    adv(h, "kamperen_eerste_verhaal", "kamperen_gevonden", "minecraft:book", "task", hulp.IMPOSSIBLE,
        "Er was eens...", "Luister bij het kampvuur naar een verhaal van Opa Guh")
    adv(h, "kamperen_alle_verhalen", "kamperen_eerste_verhaal", "guhs:slaapmutsje", "challenge", hulp.IMPOSSIBLE,
        "De hele verhalenbundel", "Luister naar alle twaalf verhalen van Opa Guh (één per nacht!)")
    adv(h, "kamperen_slaapzak", "kamperen_gevonden", "guhs:guh_slaapzak", "goal", hulp.IMPOSSIBLE,
        "Lekker uitgeslapen", "Slaap een hele nacht in een guh-slaapzak")


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for vid, (titel, regels, info) in verhalen.VERHALEN.items():
        h.lang(f"gui.guhs.knus.verhalenbundel.{vid}", titel, titel)
        h.lang(f"gui.guhs.knus.verhalenbundel.{vid}.info", info, info)
        for i, regel in enumerate(regels):
            h.lang(f"quest.guhs.kamperen.verhaal.{vid}.{i}", regel, regel)


def selfcheck(h):
    import os
    missing = []
    if len(verhalen.VERHALEN) != 12 or any(len(r) != 7 for (_, r, _) in verhalen.VERHALEN.values()):
        missing.append("12 stories of 7 lines")
    for f in ("blockstates/guh_slaapzak.json", "geo/entity/guh_npc_opa_guh.geo.json", "textures/mob_effect/uitgerust.png"):
        if not os.path.exists(f"{h.A}/{f}"):
            missing.append(f)
    if missing:
        raise SystemExit(f"kamperen assets missing: {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    npc(h)
    hulp.sounds(h, SOUNDS)
    advancements(h)
    texts(h)
    s, info = bouw.build(h)
    n = bouw.check(s, info)
    h.TEMPLATE_SIZES[NAME] = 13
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, ["knuffeldal", "guh_meadows"], spacing=16, separation=5, salt=20280603, start_y=-bouw.G, reach=40, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none})
    s.save(NAME)
    selfcheck(h)
    print(f"kamperen: geometry check ok ({n} walkable spots, {len(info['slaapzakken'])} sleeping bags)")


# =====================================================================================================================
# FTB quests (row y = 95), no dependencies
# =====================================================================================================================
def ftb(fq):
    q, y = fq.q, FTB_Y
    q("kamperen_gevonden", "Een kampeerplekje", "In het &dKnuffeldal&r en op de &dGuhweides&r liggen kleine kampeerplekjes: een kampvuur, "
      "guhtentjes en Opa Guh. Het superkompas (categorie Knus) wijst de weg.", "minecraft:campfire", [fq.structure(NAME)],
      rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y, shape="circle", xp=100)
    q("kamperen_opa", "Opa Guh", "Praat met &dOpa Guh&r, bij een kampeerplekje of op zijn bankje in het Knuffeldal.",
      "guhs:slaapmutsje", [fq.adv("kamperen_opa")], rewards=(("guhs:kaas_knabbels", 8),), x=-6.5, y=y, xp=100)
    q("kamperen_verhaal", "Er was eens...", "Kom 's avonds of 's nachts bij het kampvuur en praat met Opa Guh: hij vertelt een verhaal. "
      "Blijf dichtbij tot het uit is!", "minecraft:book", [fq.adv("kamperen_eerste_verhaal")], rewards=(("guhs:guh_slaapzak", 1),),
      x=-5, y=y, xp=150)
    q("kamperen_bundel", "De hele verhalenbundel", "Opa Guh kent twaalf verhalen en vertelt er één per nacht. Verzamel ze allemaal in je "
      "verhalenbundel (Guhdex, tab Knus).", "minecraft:writable_book", [fq.adv("kamperen_alle_verhalen")],
      rewards=(("guhs:vahoege_vads_ingot", 2),), x=-3.5, y=y, shape="gear", xp=500)
    q("kamperen_slaapzak", "Lekker uitgeslapen", "Slaap een hele nacht in een &dguh-slaapzak&r (geen bed nodig!). Je wordt uitgerust wakker.",
      "guhs:guh_slaapzak", [fq.adv("kamperen_slaapzak")], rewards=(("guhs:gefrituurde_kaasknabbels", 3),), x=-2, y=y, xp=150)
    q("kamperen_pyjama", "Pyjamafeest", "'s Nachts dragen guhs bij het kampvuur hun pyjama. Luister naar een verhaal met minstens vijf guhs in "
      "pyjama om je heen!", "guhs:pyjama_pakje", [fq.adv("kamperen_pyjamafeest")], rewards=(("guhs:guh_ballon", 3),), x=-0.5, y=y, xp=200)
    q("kamperen_guhdex", "Een wijs oud gezicht", "Zet Opa Guh in je Guhdex (kom dichtbij genoeg).",
      "guhs:guhdex", [fq.adv("seen_opa_guh")], rewards=(("guhs:kaas_knabbels", 8),), x=1, y=y, shape="rsquare", xp=100)
    q("kamperen_marshmallow", "Kleverig lekker", "Rooster 's avonds een &dmarshmallowknabbel&r boven een brandend kampvuur (rechtsklik het "
      "vuur met de marshmallow in je hand). Mmm! De guhs in de buurt komen er gezellig bij zitten.",
      "minecraft:campfire", [fq.adv("kamperen_marshmallow")], rewards=(("guhs:gefrituurde_kaasknabbels", 2),), x=2.5, y=y, xp=100)
