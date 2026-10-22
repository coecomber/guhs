"""
1.6.0 Op Safari! - the kern of Safaripark Vadse Bergen (Java: feature/safari; contract: SAFARI_CONTRACT.md; design:
../../ontwerp-safaripark-en-bouwen.md; English: tools/lang/en/c160_safari.json).

Here: the dimension guhs:safaripark (an empty noise world whose biome source guhs:safari_zones follows the park's zones),
its dimension type (a normal day and night), the ten biomes guhs:safari_<zone> (colours per zone, no rain or snow: the
weather is per-zone atmosphere later), the empty fallback animation safari/_leeg, and the Dutch texts of the kern and of
every species' name. The park itself (tiles, zones.json, plekken.json, park.json) comes from the park generator, the
species (models, soorten.json) from the species generator.
"""
import os

NAME = "safari"
CLOTHES = []
BONES = {}

ZONES = ["entree", "savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie"]

# zone: (grass, foliage, water, water fog, sky, fog)
KLEUREN = {
    "entree": ("#8cc152", "#6ba63b", "#3fb5d8", "#2f7fa0", "#8fcfff", "#f2e6c8"),
    "savanne": ("#c8b54a", "#a5a83a", "#4aa8c8", "#2f7890", "#9ad0ff", "#f5e2b8"),
    "delta": ("#6fbf4f", "#4f9f2f", "#3a9ca8", "#22646c", "#8fc8f0", "#d8ead0"),
    "regenwoud": ("#3f9a2f", "#2f8a1f", "#2f8f9a", "#1c5a60", "#82c4e0", "#c8e0c0"),
    "bamboe": ("#7fc85a", "#6fbf4a", "#5ab8d8", "#3a7890", "#a8d4f0", "#e4eef0"),
    "ijskust": ("#9fc8b0", "#7fb09a", "#3f76e4", "#2a4fa0", "#b8dcff", "#eaf4ff"),
    "oertijd": ("#a8b8a0", "#8fa08a", "#5080c0", "#304f80", "#c0d8f0", "#e8ecf0"),
    "europa": ("#79c05a", "#59ae30", "#3f76e4", "#2a4fa0", "#78a7ff", "#c0d8ff"),
    "australie": ("#c09a50", "#8a9a50", "#4a9ab8", "#2f6a80", "#9ad0ff", "#f5d8b8"),
    "decor": ("#b8b050", "#98a040", "#4aa8c8", "#2f7890", "#9ad0ff", "#f0e4c4"),
}
BIOME_NAMEN = {
    "entree": "Safaripark: Entree", "savanne": "Safaripark: Savanne", "delta": "Safaripark: Rivierdelta",
    "regenwoud": "Safaripark: Regenwoud", "bamboe": "Safaripark: Bamboebergen", "ijskust": "Safaripark: IJskust",
    "oertijd": "Safaripark: Oerijstijd-vallei", "europa": "Safaripark: Europees Woud", "australie": "Safaripark: Australië",
    "decor": "Safaripark: in de verte",
}

# every species (design 1.6, 51 = 46 park animals + the golden dikdik + the four of the aaihoek): the Dutch name
NAMEN = {
    "leeuw": "Leeuw", "luipaard": "Luipaard", "olifant": "Olifant", "neushoorn": "Neushoorn", "kafferbuffel": "Kafferbuffel",
    "dikdik": "Dikdik", "giraffe": "Giraffe", "zebra": "Zebra", "stokstaartje": "Stokstaartje", "jachtluipaard": "Jachtluipaard",
    "struisvogel": "Struisvogel", "gouden_dikdik": "Gouden dikdik",
    "zeekoe": "Zeekoe", "nijlpaard": "Nijlpaard", "flamingo": "Flamingo", "capibara": "Capibara", "krokodil": "Krokodil",
    "rode_panda": "Rode panda", "reuzenpanda": "Reuzenpanda", "gibbon": "Gibbon",
    "luiaard": "Luiaard", "okapi": "Okapi", "orang_oetan": "Orang-oetan", "toekan": "Toekan", "gekko": "Gekko", "glimworm": "Glimworm",
    "pinguin": "Pinguïn", "ijsbeer": "IJsbeer", "zeehond": "Zeehond", "narwal": "Narwal", "octopus": "Octopus",
    "sabeltandtijger": "Sabeltandtijger", "mammoet": "Mammoet", "wolharige_neushoorn": "Wolharige neushoorn",
    "bever": "Bever", "vuursalamander": "Vuursalamander", "egel": "Egel", "otter": "Otter", "das": "Das", "edelhert": "Edelhert",
    "ooievaar": "Ooievaar", "specht": "Specht",
    "koala": "Koala", "kangoeroe": "Kangoeroe", "wombat": "Wombat", "quokka": "Quokka", "prieelvogel": "Prieelvogel",
    "geitje": "Geitje", "alpaca": "Alpaca", "cavia": "Cavia", "ezeltje": "Ezeltje",
    # round 4 (L4): the Kriebelhuis (35 animals + the anemone) and the pelican
    "kameleon": "Kameleon", "reuzenschildpad": "Reuzenschildpad", "slang": "Opgerold slangetje", "hoornkikker": "Hoornkikker",
    "kraaghagedis": "Kraaghagedis", "gifkikker": "Gifkikkertje", "baardagaam": "Baardagaam", "mier": "Mierenkolonie",
    "springspin": "Springspinnetje", "wandelende_tak": "Wandelende tak", "mestkever": "Mestkevertje", "slak": "Slakje",
    "vogelspin": "Vogelspinnetje", "hommel": "Hommel", "kiwi": "Kiwi", "schubdier": "Schubdier",
    "spookdiertje": "Spookdiertje", "plompe_lori": "Plompe lori", "fruitvleermuis": "Fruitvleermuisje", "galago": "Galago",
    "suikereekhoorn": "Suikereekhoorn", "fennek": "Fennek", "zeepaardje": "Zeepaardje", "pijlstaartrog": "Pijlstaartrog",
    "zeeschildpad": "Zeeschildpad", "clownvis": "Clownvisje", "anemoon": "Anemoon", "kwal": "Kwalletjes",
    "kogelvis": "Kogelvis", "zeester": "Zeesterren en zee-egeltjes", "zee_egel": "Zee-egeltje", "babyhaai": "Babyhaaitje",
    "regenboogvis": "Regenboogschool", "lantaarnvisje": "Lantaarnvisje", "dumbo_octopus": "Dumbo-octopus", "pelikaan": "Pelikaan",
}

TEKSTEN = {
    "entity.guhs.safari_dier": "Safaridier",
    "gui.guhs.safari.beschermd": "Dit is Safaripark Vadse Bergen, njeg: hier mag je niks slopen of bouwen!",
    "gui.guhs.safari.storing": "Het safaripark is even dicht (storing). Probeer het straks nog eens.",
    "gui.guhs.safari.welkom": "Welkom in Safaripark Vadse Bergen! Kijk goed om je heen: de diertjes verstoppen zich graag. Vahoeg!",
    "gui.guhs.safari.guhdex_gekregen": "Je had nog geen Guhdex, njeg! Hier, eentje van de rangers: daarin staat je Safari-dex.",
    "commands.guhs.safari.herbouw": "Het safaripark wordt helemaal opnieuw neergezet; daarna komen alle dieren terug.",
    "commands.guhs.safari.dieren": "Alle %s dierenverblijven worden opnieuw gevuld.",
    "commands.guhs.safari.status": "Park versie %s (gebouwd: %s, bezig: %s) · dieren generatie %s (%s) · %s",
    "commands.guhs.safari.kaartwand": "Kaartwand: de %s kaarten gaan opnieuw aan de muur zodra het entreegebouw geladen is",
    "gui.guhs.safari.munten.erbij": "+%s Safarimunten! Vahoeg!",
    "commands.guhs.safari.munten": "Je hebt %s Safarimunten.",
    "commands.guhs.safari.wis": "Alles wat je in het safaripark deed is vergeten (dex, munten, geheimpjes...).",
    # round 4 (L4): the Kriebelhuis chapter of the dex and its sections
    "biome.guhs.safari_kriebelhuis": "Het Kriebelhuis",
    "gui.guhs.safari.dex.sectie": "%s · %s",
    "gui.guhs.safari.dex.sectie.terrarium": "Terrarium",
    "gui.guhs.safari.dex.sectie.kriebelzaal": "Kriebelzaal",
    "gui.guhs.safari.dex.sectie.nachtzaal": "Nachtzaal",
    "gui.guhs.safari.dex.sectie.aquarium": "Aquarium",
}


def _biome(h, zone):
    gras, blad, water, waterMist, lucht, mist = KLEUREN[zone]
    return {
        "attributes": {
            "minecraft:audio/background_music": {
                "default": {"sound": "minecraft:music.game", "min_delay": 12000, "max_delay": 24000},
                "creative": {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000},
            },
            "minecraft:visual/fog_color": mist,
            "minecraft:visual/sky_color": lucht,
            "minecraft:visual/water_fog_color": waterMist,
        },
        "has_precipitation": False,
        "temperature": 0.5,
        "downfall": 0.0,
        "effects": {"water_color": water, "foliage_color": blad, "grass_color": gras},
        "spawners": {},
        "spawn_costs": {},
        "carvers": [],
        "features": [],
    }


def build(h):
    D = h.D
    for zone in ZONES + ["decor"]:
        h.w(f"{D}/worldgen/biome/safari_{zone}.json", _biome(h, zone))
        h.NL[f"biome.guhs.safari_{zone}"] = BIOME_NAMEN[zone]
    h.w(f"{D}/dimension/safaripark.json", {
        "type": "guhs:safaripark",
        "generator": {
            "type": "minecraft:noise",
            "settings": "guhs:safaripark",
            "biome_source": {"type": "guhs:safari_zones", "zones": {z: f"guhs:safari_{z}" for z in ZONES}, "decor": "guhs:safari_decor"},
        },
    })
    h.w(f"{D}/dimension_type/safaripark.json", {
        "attributes": {
            "minecraft:audio/background_music": {
                "default": {"sound": "minecraft:music.game", "min_delay": 12000, "max_delay": 24000},
                "creative": {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000},
            },
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "always", "can_sleep": "when_dark",
                                            "error_message": {"translate": "block.minecraft.bed.no_sleep"}},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/ambient_light_color": "#0a0a0a",
            "minecraft:visual/cloud_color": "#ccffffff",
            "minecraft:visual/cloud_height": 192.33,
            "minecraft:visual/fog_color": "#f2e6c8",
            "minecraft:visual/sky_color": "#8fcfff",
        },
        "has_skylight": True,
        "has_ceiling": False,
        "has_ender_dragon_fight": False,
        "coordinate_scale": 1.0,
        "ambient_light": 0.0,
        "min_y": 0,
        "height": 256,
        "logical_height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "monster_spawn_light_level": 0,
        "monster_spawn_block_light_limit": 0,
        "default_clock": "minecraft:overworld",
        "timelines": "#minecraft:in_overworld",
    })
    nul = 0.0
    h.w(f"{D}/worldgen/noise_settings/safaripark.json", {
        "sea_level": 0,
        "disable_mob_generation": True,
        "aquifers_enabled": False,
        "ore_veins_enabled": False,
        "legacy_random_source": True,
        "default_block": {"Name": "minecraft:air"},
        "default_fluid": {"Name": "minecraft:air"},
        "noise": {"min_y": 0, "height": 256, "size_horizontal": 2, "size_vertical": 1},
        "noise_router": {k: nul for k in ["barrier", "continents", "depth", "erosion", "fluid_level_floodedness", "fluid_level_spread", "lava",
                                          "preliminary_surface_level", "ridges", "temperature", "vegetation", "vein_gap", "vein_ridged", "vein_toggle"]}
                        | {"final_density": -1.0},
        "spawn_target": [],
        "surface_rule": {"type": "minecraft:sequence", "sequence": []},
    })
    # the empty animation every species falls back to (a missing file: idle and walk do nothing)
    h.w(f"{h.A}/geckolib/animations/entity/safari/_leeg.animation.json", {
        "format_version": "1.8.0",
        "animations": {"idle": {"loop": True, "animation_length": 1.0, "bones": {}}, "walk": {"loop": True, "animation_length": 1.0, "bones": {}}},
    })
    for k, v in TEKSTEN.items():
        h.NL[k] = v
    for soort, naam in NAMEN.items():
        h.NL[f"entity.guhs.safari.{soort}"] = naam
