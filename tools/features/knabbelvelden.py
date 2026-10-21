"""
1.6.0: De Eeuwige Knabbelvelden (design ../../ontwerp-safaripark-en-bouwen.md ch. 7; Java: feature/knabbelvelden;
English: tools/lang/en/c167_knabbelvelden.json).

Retired guhs ("Njeg, niet dood hoor, gewoon met pensioen!") live on as see-through little ghosts in their own dimension.
This module makes:
  - the dimension guhs:knabbelvelden: flat, one layer of kaasknabbel blocks at y 0, the biome guhs:knabbelvelden (pink sky
    and fog, no rain, the soft hemel music), a dimension type where it is always day, low pink-white clouds
  - the block knabbelportaal: a 2 x 2 x 1 frame (4 parts x 2 axes; per part only its outer sides), with a swirling
    pink-and-yellow cheese cloud in the middle (one animated texture per part, together one swirl); item model, loot (drops
    itself), recipe: 4 kaasknabbel blocks around kaasknabbels and a pink dye
  - the texts (screen, ghosts, "Waar is mijn guh?", the dimension's name) and the FTB quest
"""
import math
import os

import numpy as np
from PIL import Image

NAME = "knabbelportaal"
FRAMES = 16
KAAS = (243, 196, 78)
KAAS_DONKER = (205, 150, 46)
KAAS_GAT = (222, 168, 58)
ROZE = (255, 143, 203)
GEEL = (255, 226, 122)
WIT = (255, 246, 250)


# =====================================================================================================================
# textures: one 32 x 32 portal, cut into its four parts (deel 0 = lower left ... 3 = upper right, seen from the south)
# =====================================================================================================================
def _portaal_frame(t):
    a = np.zeros((32, 32, 4), np.uint8)
    fase = t / FRAMES * math.tau
    for gy in range(32):
        for gx in range(32):
            rand = min(gx, gy, 31 - gx, 31 - gy)
            if rand < 3:
                kleur = KAAS_DONKER if rand == 0 else KAAS
                if rand >= 1 and (gx * 7 + gy * 13) % 11 == 0:
                    kleur = KAAS_GAT                       # little cheese holes in the frame
                a[gy, gx] = (*kleur, 255)
                continue
            dx, dy = gx - 15.5, gy - 15.5
            r = math.hypot(dx, dy)
            hoek = math.atan2(dy, dx)
            v = 0.5 + 0.5 * math.sin(hoek * 2 + r * 0.42 - fase)            # the swirl
            w = 0.5 + 0.5 * math.sin(r * 0.9 - fase * 2 + hoek)            # soft rings of light
            c = [ROZE[i] * v + GEEL[i] * (1 - v) for i in range(3)]
            c = [c[i] * (1 - 0.25 * w) + WIT[i] * 0.25 * w for i in range(3)]
            alpha = 200 + int(40 * w) if r > 3 else 245
            a[gy, gx] = (int(c[0]), int(c[1]), int(c[2]), min(255, alpha))
    return a


def textures(h):
    frames = [_portaal_frame(t) for t in range(FRAMES)]
    for deel in range(4):
        dx, dy = deel & 1, deel >> 1
        x0, y0 = dx * 16, (1 - dy) * 16                    # (the upper parts are the top half of the picture)
        strip = np.concatenate([f[y0:y0 + 16, x0:x0 + 16] for f in frames], axis=0)
        naam = f"{NAME}_{deel}.png"
        h.save(Image.fromarray(strip, "RGBA"), "block", naam)
        with open(os.path.join(h.TEX, "block", naam + ".mcmeta"), "w", encoding="utf-8", newline="\n") as f:
            f.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": true\n  }\n}\n')


# =====================================================================================================================
# block: models (per part only its outer sides), blockstate, item, loot, recipe
# =====================================================================================================================
def _model(deel):
    dx, dy = deel & 1, deel >> 1
    portaal = {"sprite": f"guhs:block/{NAME}_{deel}", "force_translucent": True}
    vlak = {"uv": [0, 0, 16, 16], "texture": "#portaal"}
    faces = {"south": dict(vlak), "north": {"uv": [16, 0, 0, 16], "texture": "#portaal"}}
    # the cheese sides only where the portal ends (inside the portal the parts touch)
    if dx == 0:
        faces["west"] = {"uv": [4, 0, 12, 16], "texture": "#kaas"}
    else:
        faces["east"] = {"uv": [4, 0, 12, 16], "texture": "#kaas"}
    if dy == 0:
        faces["down"] = {"uv": [0, 4, 16, 12], "texture": "#kaas", "cullface": "down"}
    else:
        faces["up"] = {"uv": [0, 4, 16, 12], "texture": "#kaas"}
    return {"textures": {"particle": "guhs:block/block_of_kaasknabbels", "kaas": "guhs:block/block_of_kaasknabbels", "portaal": portaal},
            "elements": [{"from": [0, 0, 4], "to": [16, 16, 12], "shade": False, "faces": faces}]}


def blocks_and_items(h):
    A = h.A
    for deel in range(4):
        h.w(f"{A}/models/block/{NAME}_{deel}.json", _model(deel))
    # axis z: the model turned a quarter (east -> south, so "to the right" is +z like KnabbelportaalBlock.langs)
    h.w(f"{A}/blockstates/{NAME}.json", {"variants": {
        f"axis={ax},deel={d}": {"model": f"guhs:block/{NAME}_{d}", **({"y": 90} if ax == "z" else {})}
        for ax in ("x", "z") for d in range(4)}})
    # the item: the whole portal in small (the four parts side by side, like a little picture frame)
    h.w(f"{A}/models/item/{NAME}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{NAME}"}})
    icoon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    eerste = _portaal_frame(0)
    klein = Image.fromarray(eerste, "RGBA").resize((14, 14), Image.NEAREST)
    icoon.paste(klein, (1, 1))
    h.save(icoon, "item", f"{NAME}.png")
    h.self_drop(NAME)
    h.shaped(NAME, ["BKB", "KPK", "BKB"], {"B": "guhs:block_of_kaasknabbels", "K": "guhs:kaas_knabbels", "P": "minecraft:pink_dye"},
             f"guhs:{NAME}", 1)
    h.add_tag("minecraft/tags/block/mineable/hoe", [f"guhs:{NAME}"])


# =====================================================================================================================
# the dimension
# =====================================================================================================================
def dimension(h):
    D = h.D
    h.w(f"{D}/worldgen/biome/knabbelvelden.json", {
        "attributes": {
            "minecraft:audio/background_music": {
                "default": {"sound": "guhs:hemel.muziek", "min_delay": 600, "max_delay": 2400, "replace_current_music": True},
                "creative": {"sound": "guhs:hemel.muziek", "min_delay": 600, "max_delay": 2400, "replace_current_music": True},
            },
            "minecraft:visual/fog_color": "#ffd6ea",
            "minecraft:visual/sky_color": "#ffb3d9",
            "minecraft:visual/water_fog_color": "#f7a8cf",
        },
        "has_precipitation": False,
        "temperature": 0.8,
        "downfall": 0.0,
        "effects": {"water_color": "#ffc4e1", "foliage_color": "#ffd27a", "grass_color": "#ffd27a"},
        "spawners": {},
        "spawn_costs": {},
        "carvers": [],
        "features": [],
    })
    h.w(f"{D}/dimension/knabbelvelden.json", {"type": "guhs:knabbelvelden", "generator": {
        "type": "minecraft:flat",
        "settings": {"biome": "guhs:knabbelvelden", "layers": [{"block": "guhs:block_of_kaasknabbels", "height": 1}],
                     "lakes": False, "features": False, "structure_overrides": []}}})
    h.w(f"{D}/dimension_type/knabbelvelden.json", {
        "attributes": {
            "minecraft:audio/background_music": {
                "default": {"sound": "guhs:hemel.muziek", "min_delay": 600, "max_delay": 2400, "replace_current_music": True},
                "creative": {"sound": "guhs:hemel.muziek", "min_delay": 600, "max_delay": 2400, "replace_current_music": True},
            },
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "always"},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/ambient_light_color": "#1a0f14",
            "minecraft:visual/cloud_color": "#ccfff0f8",
            "minecraft:visual/cloud_height": 40.33,
            "minecraft:visual/fog_color": "#ffd6ea",
            "minecraft:visual/sky_color": "#ffb3d9",
        },
        "has_skylight": True,
        "has_ceiling": False,
        "has_ender_dragon_fight": False,
        "coordinate_scale": 1.0,
        "ambient_light": 0.2,
        "min_y": 0,
        "height": 128,
        "logical_height": 128,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "monster_spawn_light_level": 0,
        "monster_spawn_block_light_limit": 0,
        "has_fixed_time": True,
        "timelines": "#minecraft:universal",
    })


# =====================================================================================================================
# texts (Dutch; the English is in tools/lang/en/c167_knabbelvelden.json)
# =====================================================================================================================
P = "gui.guhs.knabbelvelden."
TEXTS = {
    f"block.guhs.{NAME}": "Knabbelportaal",
    f"block.guhs.{NAME}.lore": "Een wolkje van roze kaas. Rechtsklik: stuur je guhs met pensioen naar De Eeuwige Knabbelvelden, of haal ze weer op.",
    "biome.guhs.knabbelvelden": "De Eeuwige Knabbelvelden",
    "gui.guhs.band.dim.guhs.knabbelvelden": "De Eeuwige Knabbelvelden",
    "gui.guhs.band.plek.in_de_knabbelvelden": "Met pensioen in De Eeuwige Knabbelvelden. Njeg, niet dood hoor, gewoon met pensioen!",
    "gui.guhs.band.plek.in_de_knabbelvelden.kort": "Met pensioen",
    P + "naam": "De Eeuwige Knabbelvelden",
    P + "storing": "De Knabbelvelden zijn even zoek (storing). Probeer het straks nog eens, njeg.",
    P + "welkom": "Welkom in De Eeuwige Knabbelvelden! Hier knabbelen, dutten en spelen je gepensioneerde guhs. Voor altijd. Vahoeg!",
    P + "beschermd": "Niks slopen of bouwen in de Knabbelvelden: hier is alles al precies goed. Njeg!",
    P + "te_ver": "Je staat te ver van het Knabbelportaal.",
    P + "aangekomen": "%s is aangekomen in De Eeuwige Knabbelvelden. Lekker met pensioen!",
    P + "zakken_vol": "Je zakken zitten vol: %s wacht bij het portaal op je.",
    P + "mee": "%s gaat mee naar huis! Zet hem neer waar je wilt. Vahoeg!",
    P + "aai.1": "%s spint als een kaasknabbel in de zon.",
    P + "aai.2": "%s zweeft een stukje omhoog van geluk.",
    P + "aai.3": "%s knabbelt aan je mouw. Njeg!",
    P + "aai.4": "%s geeft je een spookknuffel. Koud, maar lief.",
    P + "aai.5": "%s: \"Pensioen is vadsig, hoor!\"",
    P + "aai.tip": "(Sluip + rechtsklik: mee naar huis?)",
    P + "vraag.titel": "Mee naar huis?",
    P + "vraag.tekst": "Neem %s mee naar huis?",
    P + "vraag.uitleg": "Hij komt in je zakken als opgepakte guh, met alles erop en eraan.",
    P + "vraag.ja": "Ja, kom maar!",
    P + "vraag.nee": "Nee, geniet nog maar",
    P + "melding.weg": "%s guh(s) op weg naar de Knabbelvelden. Doei doei!",
    P + "melding.niet": " (%s kon(den) niet mee)",
    P + "melding.niks": "Er gebeurde niks, njeg.",
    P + "melding.thuis": "%s guh(s) weer thuis! In je zakken (of bij het portaal).",
    P + "melding.kwijt": "Een guh kon niet gevonden worden. Waar is hij nou weer?",
    P + "niet.al": "Zit al in de Knabbelvelden.",
    P + "niet.niet_jouw": "Dit is niet jouw guh.",
    P + "niet.dood": "In de wolkjes... njeg. Daar helpt het Knuffelhart, niet het portaal.",
    P + "niet.opgepakt": "Opgepakt (een item ergens). Zet hem eerst neer.",
    P + "niet.weg": "Op vakantie of op kantoor. Haal hem eerst terug.",
    P + "niet.kwijt": "Niemand weet waar hij is.",
    P + "bezig.huisje": "Woont in een Guhhuisje. Hij gaat er dan uit.",
    P + "bezig.guhwiel": "Rent in een Guhrad. Het rad staat dan stil.",
    P + "bezig.guhkamer": "Logeert in de Guhkamer.",
    P + "bezig.rijdt": "Rijdt ergens op of mee.",
    P + "scherm.titel": "De Eeuwige Knabbelvelden",
    P + "scherm.sub": "Njeg, niet dood hoor, gewoon met pensioen! Kies guhs en stuur ze heen of haal ze terug.",
    P + "scherm.zoek": "Zoek een guh...",
    P + "scherm.sorteer": "Sorteer: %s",
    P + "scherm.stuur": "Met pensioen sturen",
    P + "scherm.haal": "Mee naar huis",
    P + "scherm.thuis": "In je Guhdex (%s)",
    P + "scherm.velden": "In de Knabbelvelden (%s)",
    P + "scherm.naar_velden": "Naar de Knabbelvelden",
    P + "scherm.naar_huis": "Terug naar huis",
    P + "scherm.leeg_thuis": "Je hebt geen guhs thuis. Tem er eentje met een kaasknabbel!",
    P + "scherm.leeg_velden": "Nog niemand met pensioen. Het is hier nog heel rustig.",
    P + "scherm.niks_gevonden": "Geen guh gevonden met die naam.",
    P + "scherm.kies": "Klik op een guh om alles over hem te zien.",
    P + "scherm.aard": "Aard: %s",
    P + "scherm.kleding": "Kleding:",
    P + "scherm.geen_kleding": "geen (lekker bloot)",
    P + "scherm.van_jou_sinds": "Van jou sinds dag %s",
    P + "scherm.al_lang": "Al heel lang van jou",
    P + "scherm.pensioen_sinds": "Met pensioen sinds dag %s",
    P + "scherm.pensioen_vandaag": "Vandaag met pensioen gegaan",
    P + "scherm.onderweg": "Komt eraan...",
    P + "scherm.vraag.titel": "Even checken, njeg!",
    P + "scherm.vraag.tekst": "Deze guhs zijn ergens mee bezig: %s",
    P + "scherm.vraag.uitleg": "Ze stoppen ermee en gaan met pensioen (uit hun huisje, uit het Guhrad...).",
    P + "scherm.vraag.ja": "Toch met pensioen",
    P + "scherm.vraag.nee": "Laat maar",
    P + "sorteer.nummer": "nummer",
    P + "sorteer.naam": "naam",
    P + "sorteer.vacht": "vacht",
    P + "sorteer.datum": "datum",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def selfcheck(h):
    A, D = h.A, h.D
    missing = [p for p in [f"{A}/blockstates/{NAME}.json", f"{A}/models/item/{NAME}.json", f"{D}/loot_table/blocks/{NAME}.json",
                           f"{D}/recipe/{NAME}.json", f"{D}/dimension/knabbelvelden.json", f"{D}/dimension_type/knabbelvelden.json",
                           f"{D}/worldgen/biome/knabbelvelden.json"] + [f"{A}/models/block/{NAME}_{d}.json" for d in range(4)]
               if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    if missing:
        raise SystemExit(f"knabbelvelden: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    dimension(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quest (no lock)
# =====================================================================================================================
def ftb(fq):
    fq.q("knabbelvelden_portaal", "Njeg, gewoon met pensioen!", "Te veel guhs om mee te nemen? Bouw een &dKnabbelportaal&r (4 blokken "
         "kaasknabbels, 4 kaasknabbels en een roze kleurstof). Rechtsklik erop: links je guhs thuis, rechts &dDe Eeuwige "
         "Knabbelvelden&r. Stuur ze met pensioen of haal ze weer op. Niet dood hoor: in de Knabbelvelden knabbelen, dutten en "
         "zweven ze als lieve spookguhjes rond, met een aureooltje. Ga ze opzoeken met de knop &dNaar de Knabbelvelden&r!",
         f"guhs:{NAME}", [fq.item(f"guhs:{NAME}")], rewards=(("guhs:kaas_knabbels", 12),), shape="circle")
