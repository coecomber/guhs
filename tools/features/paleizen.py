"""
bbq2 (paleizen): three new Mika palaces in the Guhbarbecuether, each with a character and a per-player questline.
Java: src/main/java/nl/juiced/guhs/feature/paleizen/.

  - guhs:mika_woonblokken (paleizen_bouw.woonblokken): the flats of the Nether-Mika's. Mika-oma (NPC mika_oma, questline
    mika_oma): bring her worstsoep to three grumpy neighbours (entity paleizen_mopper_mika), find her knitting on the roof
    garden (block paleizen_breiwerk). Reward: the knitted Mika hat (clothes piece paleizen_mikamuts, marker <paleizen>, source
    "paleizen") and a discount when bartering with Nether-Mika's (every third trade is free).
  - guhs:mika_stal (paleizen_bouw.stal): the stable of the Worstzwijntjes (entity worstzwijntje, Guhdex page WORSTZWIJNTJE: a
    sweet hoglin parody, a farm animal of the Guhboerderij kind). The Stalknecht-guh (NPC stalknechtguh, questline stalknecht):
    pet three restless Worstzwijntjes calm, fill the voerbak, catch the runaway. Reward: two Worstzwijntjes in a basket.
  - guhs:mika_brugpaleis (paleizen_bouw.brugpaleis): a toll bridge through a giant Mika's mouth. The Tolwachter-Mika (NPC
    tolwachter_mika, questline tolwachter): guess three riddles (the only way through), lay the five missing rows of planks (they fall
    out again after a minute, for the next player), ring the tolbel. Reward: free passage, the bridge building set (the blocks
    paleizen_brugplank and paleizen_brugleuning and their recipe card paleizen_recept_brug).

Everything is per player (Verhaallijn); the buildings are protected (Bescherming) and in the Superkompas tab "Barbecue".
Hooks: build(h), ftb(fq), FTB_SECTIES, BONES / CLOTHES / clothes / icons. Wiki: paleizen_wiki.py.
"""
import json
import os
import re

from features import bbq2, verhaal_motor, wereld
from features import paleizen_bouw as bouw
from features import paleizen_modellen as modellen
from features import paleizen_tekst as tekst

LOANED = ["paleizen_omasoep", "paleizen_breiwerkje", "paleizen_zwijnenvoer", "paleizen_gevangen_zwijntje", "paleizen_losse_plank"]
ITEMS = LOANED + ["paleizen_worstzwijntje_mandje", "paleizen_recept_brug", "worstzwijntje_spawn_egg"]
BLOCKS = ["paleizen_brugplank", "paleizen_brugleuning", "paleizen_breiwerk"]
SOUNDS = {
    "paleizen.worstzwijntje_knor": [{"name": "minecraft:entity.pig.ambient", "type": "event", "pitch": 1.5, "volume": 0.8}],
    "paleizen.worstzwijntje_gil": [{"name": "minecraft:entity.pig.hurt", "type": "event", "pitch": 1.7, "volume": 0.7}],
    "paleizen.mopper": [{"name": "guhs:entity.mika.ambient", "type": "event", "pitch": 0.75, "volume": 0.7}],
    "paleizen.plank": [{"name": "minecraft:block.wood.place", "type": "event", "pitch": 0.8, "volume": 1.0}],
}
# salts of slice 09 (CONTRACT_130 3): the structure sets end in 1, 11, 21; the guaranteed sets are those + 7
SALT = {"mika_woonblokken": 21300901, "mika_stal": 21300911, "mika_brugpaleis": 21300921}

# =====================================================================================================================
# the outfit (make_guh_variants: BONES / clothes; make_clothes_icons: icons; make_resources: CLOTHES item models)
# =====================================================================================================================
_H = [0, 6, -2]
BONES = {
    # the knitted Mika hat: a dark red beanie with a cream rolled rim, two little knitted horns with red tips
    "outfit_paleizen_muts": ("head", _H, "paleizen_muts", [([-5.3, 14.8, -11.3], [10.6, 2.2, 9.8], 0), ([-4.3, 17.0, -10.3], [8.6, 1.1, 7.8], 0)]),
    "outfit_paleizen_muts_rand": ("head", _H, "paleizen_muts_rand", [([-5.7, 13.6, -11.7], [11.4, 1.4, 10.6], 0)]),
    "outfit_paleizen_muts_hoorn": ("head", _H, "paleizen_muts_hoorn", [([-4.8, 17.6, -7.6], [1.8, 1.6, 1.8], 0), ([3.0, 17.6, -7.6], [1.8, 1.6, 1.8], 0)]),
    "outfit_paleizen_muts_punt": ("head", _H, "paleizen_muts_punt", [([-5.4, 19.2, -7.2], [1.2, 1.4, 1.2], 0), ([4.2, 19.2, -7.2], [1.2, 1.4, 1.2], 0)]),
}
CLOTHES = ["paleizen_mikamuts"]
MUTS = (150, 52, 60)
MUTS_RAND = (246, 232, 208)


def _steken(rng, v, kleur, licht):
    a = v.fabric(kleur, rng, 8)
    for y in range(0, a.shape[0], 4):
        for x in range(0, a.shape[1], 4):
            a[y, x] = licht
            a[min(y + 1, a.shape[0] - 1), min(x + 1, a.shape[1] - 1)] = licht
            a[y, min(x + 2, a.shape[1] - 1)] = licht
    return a


def clothes(rng, v):
    return {"paleizen_mikamuts": {"paleizen_muts": lambda: _steken(rng, v, MUTS, (190, 84, 90)),
                                  "paleizen_muts_rand": lambda: _steken(rng, v, MUTS_RAND, (255, 250, 236)),
                                  "paleizen_muts_hoorn": lambda: _steken(rng, v, (70, 50, 56), (110, 84, 90)),
                                  "paleizen_muts_punt": lambda: v.fabric((226, 60, 60), rng, 8)}}


MUTS_ICON = ["................", "...p........p...", "...h........h...", "...hh......hh...", "....hmmmmmmh....", "...mmmMmmMmmm...",
             "..mmMmmmmmmMmm..", "..mmmmMmmMmmmm..", "..mMmmmmmmmmMm..", "..mmmmMmmMmmmm..", ".rrrrrrrrrrrrrr.", ".rRrRrRrRrRrRrr.",
             ".rrrrrrrrrrrrrr.", "................", "................", "................"]


def icons(ic):
    return {"paleizen_mikamuts": ic.icon(MUTS_ICON, {"m": MUTS, "M": (190, 84, 90), "r": MUTS_RAND, "R": (220, 200, 170), "h": (70, 50, 56),
                                                     "p": (226, 60, 60)})}


# =====================================================================================================================
# the buildings
# =====================================================================================================================
def structuren(h):
    built = bouw.build_all(h)
    problems = [p for _b, probs, _t in built.values() for p in probs]
    if problems:
        print("paleizen geometry check found problems:\n  " + "\n  ".join(problems[:60]))
        raise SystemExit(f"paleizen: fix the building templates ({len(problems)} problems, see above)")
    print("paleizen: geometry checks ok (" + ", ".join(f"{n}: {len(b.s.blocks)} blocks" for n, (b, _p, _t) in built.items()) + ")")
    S = tekst.STRUCTUREN
    nx, nz, _ = built["mika_woonblokken"][2]
    wereld.bbq_structuur(h, "mika_woonblokken", soort="burcht", titel=S["mika_woonblokken"][0], tooltip=S["mika_woonblokken"][1],
                         biomes=["houtskoolvlakte", "asdal", "satebos", "worstenwoud"], salt=SALT["mika_woonblokken"], spacing=34, separation=12,
                         gegarandeerd=dict(sector=1, min=250, max=900),
                         burcht=dict(placement="paleis", tiles_x=nx, tiles_z=nz, tile_size=bouw.sb.TILE, anchor=bouw.WB_ANKER, min_y=33, max_y=33, reach=24))
    wereld.bbq_structuur(h, "mika_stal", soort="grot", titel=S["mika_stal"][0], tooltip=S["mika_stal"][1],
                         biomes=["worstenwoud", "satebos", "houtskoolvlakte", "asdal"], salt=SALT["mika_stal"], templates=[("mika_stal", 1)],
                         spacing=28, separation=10, gegarandeerd=dict(sector=2, min=250, max=900), grootte=26, vlak=8, hoogte=12)
    nx, nz, _ = built["mika_brugpaleis"][2]
    wereld.bbq_structuur(h, "mika_brugpaleis", soort="burcht", titel=S["mika_brugpaleis"][0], tooltip=S["mika_brugpaleis"][1],
                         biomes=wereld.BBQ, salt=SALT["mika_brugpaleis"], spacing=34, separation=12, gegarandeerd=dict(sector=3, min=250, max=900),
                         burcht=dict(placement="brug", tiles_x=nx, tiles_z=nz, tile_size=bouw.sb.TILE, anchor=bouw.BR_ANKER, min_y=40, max_y=62, reach=40))
    # no monsters spawn inside the three palaces: the neighbours are grumpy enough
    for name in SALT:
        h.patch_json(f"{h.D}/worldgen/structure/{name}.json",
                     lambda d: d.update(spawn_overrides={"monster": {"bounding_box": "piece", "spawns": []}}))
    return built


def test_templates(h):
    """paleizen_test_kamer (36 x 12 x 36): a floor of houtskoolsteen stenen, for PaleizenGameTests."""
    t = h.Structure((36, 12, 36))
    for x in range(36):
        for z in range(36):
            t.set(x, 0, z, "guhs:houtskoolsteen_stenen")
    t.save("paleizen_test_kamer")


# =====================================================================================================================
# loot, recipes, tags, sounds
# =====================================================================================================================
def _tabel(entries, rolls):
    return {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": [
        {"type": "minecraft:item", "name": item, "weight": wgt, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]} for item, wgt, lo, hi in entries]}


def loot(h):
    D = h.D
    kisten = {  # (modest: snacks and odds and ends, nothing a quest needs)
        "paleizen_woonblok": ([("guhs:kaas_knabbels", 30, 2, 6), ("minecraft:string", 12, 1, 4), ("minecraft:pink_wool", 8, 1, 2),
                               ("guhs:guhbraadworst", 12, 1, 2), ("guhs:kaasknabbelsate", 10, 1, 2), ("minecraft:iron_nugget", 10, 2, 6),
                               ("guhs:pindascheutjes", 8, 1, 3), ("minecraft:bowl", 6, 1, 2)], (2, 4)),
        "paleizen_stal": ([("guhs:knabbelvoer", 30, 2, 6), ("minecraft:hay_block", 12, 1, 2), ("guhs:kaas_knabbels", 20, 2, 5),
                           ("guhs:guhborstel", 4, 1, 1), ("minecraft:lead", 6, 1, 1), ("guhs:sate_zwammetje", 8, 1, 2)], (2, 4)),
        "paleizen_tolhuis": ([("guhs:kaas_knabbels", 40, 3, 8), ("minecraft:gold_nugget", 15, 2, 6), ("minecraft:iron_nugget", 15, 2, 6),
                              ("guhs:gefrituurde_kaasknabbels", 8, 1, 2), ("minecraft:paper", 10, 1, 3), ("guhs:gloeikoolgruis", 8, 1, 3)], (2, 4)),
        "paleizen_brug": ([("guhs:kaas_knabbels", 30, 4, 10), ("guhs:grillkool", 12, 1, 2), ("guhs:gloeikoolgruis", 12, 2, 4),
                           ("minecraft:gold_nugget", 12, 3, 8), ("guhs:vahoege_vads_ingot", 3, 1, 1), ("guhs:gegrilde_kaasknabbelsate", 10, 1, 2)], (3, 5)),
    }
    for name, (entries, rolls) in kisten.items():
        h.w(f"{D}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [_tabel(entries, rolls)]})
    # what a content Worstzwijntje sniffs up once a day
    h.w(f"{D}/loot_table/gameplay/paleizen_snuffelvondst.json", {"type": "minecraft:gift", "pools": [_tabel([
        ("guhs:kaas_knabbels", 40, 2, 4), ("guhs:sate_zwammetje", 12, 1, 2), ("guhs:worst_zwammetje", 10, 1, 1), ("guhs:pindascheutjes", 12, 1, 2),
        ("guhs:grillkool", 8, 1, 1), ("guhs:gloeikoolgruis", 8, 1, 2), ("guhs:zoutkristal", 4, 1, 1)], (1, 1))]})
    h.w(f"{D}/loot_table/entities/worstzwijntje.json", {"type": "minecraft:entity", "pools": []})
    h.w(f"{D}/loot_table/entities/paleizen_mopper_mika.json", {"type": "minecraft:entity", "pools": []})


def recepten(h):
    # the bridge building set: both recipes need the Tolwachter's building plan, which stays in the grid
    kaart = "guhs:paleizen_recept_brug"
    h.shaped("paleizen_brugplank", ["PPP", "SKS"], {"P": "#minecraft:planks", "S": "minecraft:string", "K": kaart}, "guhs:paleizen_brugplank", 6)
    h.shaped("paleizen_brugleuning", ["TST", "TKT"], {"T": "minecraft:stick", "S": "minecraft:string", "K": kaart}, "guhs:paleizen_brugleuning", 4)


def tags(h):
    add = h.add_tag
    add("guhs/tags/item/loaned", [f"guhs:{i}" for i in LOANED])
    add("minecraft/tags/block/mineable/axe", ["guhs:paleizen_brugplank", "guhs:paleizen_brugleuning"])
    add("minecraft/tags/block/fences", ["guhs:paleizen_brugleuning"])
    add("minecraft/tags/item/fences", ["guhs:paleizen_brugleuning"])
    add("minecraft/tags/entity_type/fall_damage_immune", ["guhs:worstzwijntje"])


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# texts, questlines, advancements
# =====================================================================================================================
def teksten(h):
    for key, nl in tekst.TEXTS.items():
        h.lang(key, nl, nl)
    bbq2.pagina(h, "worstzwijntje", *tekst.PAGINA)
    for id, lijn in tekst.LIJNEN.items():
        verhaal_motor.verhaallijn(h, id, lijn["naam"], lijn["uitleg"], lijn["stappen"], klaar=lijn.get("klaar"), extra=lijn.get("extra"),
                                  kort=lijn.get("kort"))


def advancements(h):
    for name, (parent, icon, frame, titel, tekst_, structuur) in tekst.ADVANCEMENTS.items():
        criteria = None
        if structuur:
            criteria = {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{structuur}"}}}}}
        bbq2.zichtbaar(h, "barbecuether", name, parent, icon, frame, titel, tekst_, criteria=criteria)
    for name in tekst.VERBORGEN:
        bbq2.verborgen(h, name)


# =====================================================================================================================
# the self-check
# =====================================================================================================================
def _java_plekken():
    """The constants of PaleisPlekken.java: {structure: {name: tuple}} (written by hand there, compared here)."""
    path = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "paleizen", "PaleisPlekken.java")
    if not os.path.exists(path):
        return None
    src = open(path, encoding="utf-8").read()
    out = {}
    for m in re.finditer(r'//\s*<(\w+)>(.*?)//\s*</\1>', src, re.S):
        plekken = out.setdefault(m.group(1), {})
        for p in re.finditer(r'(?:BlockPos|BoundingBox)\s+(\w+)\s*=\s*new\s+(?:BlockPos|BoundingBox)\(([^)]*)\)', m.group(2)):
            plekken[p.group(1).lower()] = tuple(int(v) for v in p.group(2).split(","))
    return out


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for b in BLOCKS:
        for p in (f"{A}/blockstates/{b}.json",):
            if not os.path.exists(p):
                missing.append(p)
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS:
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(os.path.join(h.TEX, "item", f"{i}.png")):
            missing.append(f"item model / texture {i}")
        if f"item.guhs.{i}" not in h.NL:
            missing.append(f"lang item.guhs.{i}")
    for e in ("worstzwijntje", "paleizen_mopper_mika"):
        for p in (f"{A}/geckolib/models/entity/{e}.geo.json", os.path.join(h.TEX, "entity", f"{e}.png")):
            if not os.path.exists(p):
                missing.append(p)
        if f"entity.guhs.{e}" not in h.NL:
            missing.append(f"lang entity.guhs.{e}")
    for kind in ("mika_oma", "stalknechtguh", "tolwachter_mika"):
        for p in (f"{A}/geckolib/models/entity/guh_npc_{kind}.geo.json", os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            if not os.path.exists(p):
                missing.append(p)
    for name in SALT:
        for p in (f"{D}/worldgen/structure/{name}.json", f"{D}/worldgen/structure_set/{name}.json", f"{D}/worldgen/structure_set/{name}_gegarandeerd.json"):
            if not os.path.exists(p):
                missing.append(p)
    # every sign of the templates has its text
    for name in ("mika_brugpaleis", "mika_woonblokken", "mika_stal"):
        pass
    for key in bouw.SIGN_KEYS:
        if f"sign.guhs.{key}" not in h.NL:
            missing.append(f"lang sign.guhs.{key}")
    # the riddles: three answers each, a right one
    for i, (_v, antwoorden, goed) in enumerate(tekst.RAADSELS):
        if len(antwoorden) != 3 or not 0 <= goed < 3:
            missing.append(f"riddle {i}")
    # the template coordinates of the Java side are the builder's
    java = _java_plekken()
    if java is not None:
        for structuur, plekken in bouw.PLEKKEN.items():
            for naam, pos in plekken.items():
                if java.get(structuur, {}).get(naam) != tuple(pos):
                    missing.append(f"PaleisPlekken.java <{structuur}> {naam.upper()} must be {tuple(pos)} (is {java.get(structuur, {}).get(naam)})")
        for structuur, plekken in java.items():
            for naam in plekken:
                if naam not in bouw.PLEKKEN.get(structuur, {}):
                    missing.append(f"PaleisPlekken.java <{structuur}> {naam.upper()} is not a spot of the builder")
    if missing:
        raise SystemExit("paleizen self-check failed:\n  " + "\n  ".join(missing))


def build(h):
    modellen.build(h)
    structuren(h)
    test_templates(h)
    loot(h)
    recepten(h)
    tags(h)
    sounds(h)
    teksten(h)
    advancements(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_barbecuether: one section per building, its character as the portrait)
# =====================================================================================================================
FTB_SECTIES = [
    ("paleizen_woonblokken", "De Mika-woonblokken", "npc:mika_oma", None),
    ("paleizen_stal", "De Mika-stal", "npc:stalknechtguh",
     ["paleizen_stal_vind", "paleizen_stalknecht_1", "paleizen_stalknecht_2", "paleizen_stalknecht_3", "paleizen_stalknecht_4", "paleizen_zwijntje_dex",
      "paleizen_zwijntje_thuis", "paleizen_zwijntje_snuffel"]),
    ("paleizen_brugpaleis", "Het Mika-brugpaleis", "npc:tolwachter_mika",
     ["paleizen_brug_vind", "paleizen_tolwachter_1", "paleizen_tolwachter_2", "paleizen_tolwachter_3", "paleizen_tolwachter_4", "paleizen_tolwachter_5",
      "paleizen_raadsels", "paleizen_brug_bouwen"]),
]


def ftb(fq):
    q, item, adv, structure = fq.q, fq.item, fq.adv, fq.structure
    # --- De Mika-woonblokken ---
    q("paleizen_woon_vind", "Bij de Mika's thuis", "Ergens in de Guhbarbecuether staan de &6Mika-woonblokken&r: flats van roosterijzer met "
      "galerijen, waslijnen en een rokend ketelhuis. Het superkompas (Barbecue > Mika-woonblokken) wijst de weg. Er spawnen geen monsters.",
      "minecraft:orange_stained_glass_pane", [structure("mika_woonblokken")], rewards=(("guhs:kaas_knabbels", 12),), deps=["bbq_aan"],
      shape="hexagon", xp=100)
    wereld.ftb_questlijn(fq, "paleizen", "mika_oma", [
        ("Mika-oma", "Op de &dbovenste galerij&r van de hoge flat woont &6Mika-oma&r. Zij jat geen knabbels: ze breit en kookt "
                     "worstsoep. Neem de trap in de westelijke toren en zeg eens hallo.", "guhs:paleizen_omasoep"),
        ("Soep voor de mopperaars", "Breng een kommetje soep naar &cBrom-Mika&r (westflat, begane grond), &cZeur-Mika&r (oostflat, "
                                    "tweede verdieping) en &cSnurk-Mika&r (hoge flat, tweede verdieping). Rechtsklik ze met de soep.",
         "minecraft:bowl"),
        ("Weggewaaid!", "Het breiwerk van Mika-oma is van de galerij gewaaid. Het ligt op de &ddaktuin&r van de lage oostflat: neem de "
                        "trap in de oostelijke toren. Rechtsklik het breimandje.", "minecraft:pink_wool"),
        ("Soep van oma", "Breng het breiwerk terug naar Mika-oma. Ze breit er meteen een muts van, en de Nether-Mika's geven je "
                         "voortaan &6korting&r: elke derde ruil krijg je je staaf terug.", "guhs:paleizen_mikamuts"),
    ], na=["paleizen_woon_vind"], eind=(("guhs:kaas_knabbels", 16),))
    q("paleizen_muts", "Gebreide Mika-muts", "Een donkerrode muts met een crèmekleurig randje en twee gebreide hoorntjes. Houd hem vast "
      "(rechtsklik ingedrukt) om hem te ontgrendelen en trek hem je guh aan in de kledingkast!", "guhs:paleizen_mikamuts",
      [item("guhs:paleizen_mikamuts")], rewards=(("guhs:kaas_knabbels", 6),), deps=["paleizen_mika_oma_4"])
    q("paleizen_korting", "Dat hoort oma!", "Ruil na de soep van Mika-oma drie keer met een &cNether-Mika&r (een vahoege-vadsstaaf "
      "geven): de derde keer gooit hij je staaf terug.", "guhs:vahoege_vads_ingot", [adv("paleizen_korting")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["paleizen_mika_oma_4"])
    # --- De Mika-stal ---
    q("paleizen_stal_vind", "Knor!", "De &6Mika-stal&r: een rode schuur met een modderige wei, waar de Mika's hun &dWorstzwijntjes&r "
      "houden. Het superkompas (Barbecue > Mika-stal) weet er een.", "minecraft:hay_block", [structure("mika_stal")],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["bbq_aan"], shape="hexagon", xp=100)
    wereld.ftb_questlijn(fq, "paleizen", "stalknecht", [
        ("De Stalknecht-guh", "Voor de grote staldeur staat de &6Stalknecht-guh&r. Een guh, bij de Mika's! Praat met hem.",
         "minecraft:hay_block"),
        ("Rustig maar, njeg", "De Worstzwijntjes stuiteren van de zenuwen. &dAai&r er drie kalm: rechtsklik met een lege hand, drie "
                              "aaitjes per zwijntje.", "guhs:worstzwijntje_spawn_egg"),
        ("Smakken maar!", "Schep de zak &6zwijnenvoer&r van de Stalknecht-guh leeg in de voerbak, achter in het gangpad van de stal.",
         "guhs:paleizen_zwijnenvoer"),
        ("Knorretje is zoek!", "Knorretje is ontsnapt. Zoek hem bij de wei (achter de hooibalen, bij de kar of op de hooizolder), "
                               "&dsluip&r naar hem toe en til hem op. Breng hem naar de Stalknecht-guh: je krijgt twee "
                               "Worstzwijntjes in een mandje!", "guhs:paleizen_worstzwijntje_mandje"),
    ], na=["paleizen_stal_vind"], eind=(("guhs:knabbelvoer", 8),))
    q("paleizen_zwijntje_dex", "Het Worstzwijntje in de Guhdex", "Ga vlak naast een &dWorstzwijntje&r staan: een braadworstje op pootjes "
      "met een streep mosterd over zijn rug. Het komt in je Guhdex.", "guhs:guhdex", [adv("seen_worstzwijntje")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["paleizen_stal_vind"])
    q("paleizen_zwijntje_thuis", "Twee nieuwe huisgenoten", "Zet de mandjes thuis neer: je Worstzwijntjes zijn boerderijdiertjes. Aaien, "
      "borstelen (guhborstel) en voeren (knabbelvoer of een gevulde voerbak) maakt ze blij. Met knabbelvoer krijgen ze kleintjes.",
      "guhs:paleizen_worstzwijntje_mandje", [item("guhs:paleizen_worstzwijntje_mandje")], rewards=(("guhs:knabbelvoer", 6),),
      deps=["paleizen_stalknecht_4"])
    q("paleizen_zwijntje_snuffel", "Snuf snuf", "Een &dblij&r Worstzwijntje (twee soorten zorg op één dag) snuffelt elke dag iets voor je "
      "op: knabbels, zwammetjes, soms zelfs iets zeldzaams uit de Barbecuether.", "guhs:kaas_knabbels", [adv("paleizen_snuffel")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["paleizen_zwijntje_thuis"])
    # --- Het Mika-brugpaleis ---
    q("paleizen_brug_vind", "Door de bek van de Mika", "Het &6Mika-brugpaleis&r is een lange tolbrug hoog boven de saus. Je loopt er "
      "binnen door de open bek van een reusachtige Mika. Het superkompas (Barbecue > Mika-brugpaleis) wijst de weg.",
      "guhs:paleizen_brugplank", [structure("mika_brugpaleis")], rewards=(("guhs:kaas_knabbels", 12),), deps=["bbq_aan"], shape="hexagon", xp=100)
    wereld.ftb_questlijn(fq, "paleizen", "tolwachter", [
        ("HALT! Tol!", "In de bek zit de &cTolwachter-Mika&r. Praat met hem.", "minecraft:gold_nugget"),
        ("Raadsels, geen knabbels", "De tol is hier geen knabbel maar een raadsel: raad &ddrie raadsels&r goed. Fout geraden? Dan "
                                    "krijg je gewoon een ander raadsel.", "minecraft:writable_book"),
        ("Mika-kwaliteit", "De brug is kapot (alweer). Leg de vijf planken van de Tolwachter in het gat: rechtsklik met een plank, "
                           "vlak bij het gat. Erin gevallen? Onder het gat hangt een steiger met een ladder.", "guhs:paleizen_losse_plank"),
        ("DONG!", "Loop over de gemaakte brug naar de klokkentoren en luid de &6tolbel&r. Schiet op: na een minuutje vallen de planken "
                  "er weer uit.", "minecraft:bell"),
        ("Tolvrij!", "Ga terug naar de Tolwachter-Mika. Je mag voortaan altijd gratis door, en je krijgt de &6bouwtekening&r van de "
                     "brug met planken en touwleuning.", "guhs:paleizen_recept_brug"),
    ], na=["paleizen_brug_vind"], eind=(("guhs:kaas_knabbels", 16),))
    q("paleizen_raadsels", "Raadselkoning", "Raad drie raadsels van de Tolwachter-Mika goed. Hij had liever knabbels gehad. Njeg njeg njeg!",
      "minecraft:writable_book", [adv("paleizen_raadsels")], rewards=(("guhs:kaas_knabbels", 8),), deps=["paleizen_tolwachter_1"])
    q("paleizen_brug_bouwen", "Je eigen Mika-brug", "Met de &6bouwtekening&r in je werkbank maak je &dMika-brugplanken&r (planken en "
      "touw) en &dtouwleuning&r (stokjes en touw). De tekening blijft gewoon liggen.", "guhs:paleizen_brugleuning",
      [item("guhs:paleizen_brugplank", 12), item("guhs:paleizen_brugleuning", 4)], rewards=(("guhs:kaas_knabbels", 8),),
      deps=["paleizen_tolwachter_5"])
