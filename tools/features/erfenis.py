"""
1.9 "De Erfenis" (PROOF OF CONCEPT, branch 190-erfenis-poc). Java: feature/erfenis.

  - the hidden void dimension guhs:erfenis (dimension, dimension type, biome): always day, no weather, no mob spawning,
    beds never set the spawn point; every base has its own plot there on a grid (Java: Plot, Stempel);
  - the texts (Dutch; en_us gets the same Dutch texts for the POC: no English overlay file on purpose).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/erfenis.py
"""
import json
import os

DIM_TYPE = {
    "attributes": {
        "minecraft:audio/background_music": {
            "default": {"sound": "minecraft:music.game", "min_delay": 12000, "max_delay": 24000},
            "creative": {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000},
        },
        "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "always"},
        "minecraft:gameplay/can_start_raid": False,
        "minecraft:gameplay/respawn_anchor_works": False,
        "minecraft:visual/ambient_light_color": "#0a0a0a",
        "minecraft:visual/cloud_color": "#ccffffff",
        "minecraft:visual/cloud_height": 110.33,
        "minecraft:visual/fog_color": "#ffe3f0",
        "minecraft:visual/sky_color": "#9ad6ff",
    },
    "has_skylight": True,
    "has_ceiling": False,
    "has_ender_dragon_fight": False,
    "coordinate_scale": 1.0,
    "ambient_light": 0.1,
    "min_y": 0,
    "height": 256,
    "logical_height": 256,
    "infiniburn": "#minecraft:infiniburn_overworld",
    "monster_spawn_light_level": 0,
    "monster_spawn_block_light_limit": 0,
    "has_fixed_time": True,
    "timelines": "#minecraft:universal",
}

DIM = {
    "type": "guhs:erfenis",
    "generator": {"type": "minecraft:flat", "settings": {"biome": "guhs:erfenis", "layers": [], "lakes": False, "features": False,
                                                          "structure_overrides": []}},
}

BIOME = {
    "attributes": {
        "minecraft:audio/background_music": {
            "default": {"sound": "minecraft:music.game", "min_delay": 12000, "max_delay": 24000},
            "creative": {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000},
        },
        "minecraft:visual/fog_color": "#ffe3f0",
        "minecraft:visual/sky_color": "#9ad6ff",
        "minecraft:visual/water_fog_color": "#5aa0e0",
    },
    "has_precipitation": False,
    "temperature": 0.8,
    "downfall": 0.0,
    "effects": {"water_color": "#7ad0ff", "foliage_color": "#62b33b", "grass_color": "#86d45a"},
    "spawners": {},
    "spawn_costs": {},
    "carvers": [],
    "features": [],
}

G = "erfenis."
TEKSTEN = {
    "key.guhs.erfenis": "Bovenaanzicht (Erfenis)",
    # the buildings
    G + "gebouw.stadhuis": "Stadhuis",
    G + "gebouw.stadhuis.uitleg": "Het hart van je erfenis. Zijn level bepaalt hoeveel je mag bouwen en hoe ver je alles mag verbeteren.",
    G + "gebouw.molen": "Bouwspulmolen",
    G + "gebouw.molen.uitleg": "Maalt elke minuut Bouwspul, ook als je er niet bent. Klik hem aan om het op te halen.",
    G + "gebouw.schuurtje": "Schuurtje",
    G + "gebouw.schuurtje.uitleg": "Hier past meer Bouwspul in. Hoe hoger, hoe meer.",
    G + "gebouw.muur": "Muurtje",
    G + "gebouw.muur.uitleg": "Een stukje muur. Sleep een rij om er meteen een hele lijn van te bouwen. Njeg!",
    G + "gebouw.boompje": "Bloesemboompje",
    G + "gebouw.boompje.uitleg": "Puur voor de knusheid. Vahoeg.",
    # errors
    G + "fout.buiten": "Dat past niet op je erfenis.",
    G + "fout.overlap": "Daar staat al iets, njeg.",
    G + "fout.limiet": "Je hebt al het maximum aan %s.",
    G + "fout.slot": "Ontgrendeld op stadhuis-level %s.",
    G + "fout.bouwspul": "Te weinig Bouwspul (nodig: %s).",
    G + "fout.bouwer": "Bouwer is bezig.",
    G + "fout.max": "Al op het hoogste level. Vahoeg!",
    G + "fout.niet_eigen": "Dit is niet jouw erfenis. Alleen kijken, niet aankomen!",
    G + "fout.niks": "Er valt nog niks op te halen.",
    G + "fout.vol": "Je opslag is vol! Bouw of verbeter een schuurtje.",
    G + "fout.weg": "Dat gebouw is er niet (meer).",
    G + "fout.geen_plot": "Je staat niet op een erfenis.",
    # good news
    G + "ok.opgehaald": "+%s Bouwspul. Njeg!",
    G + "ok.klaar": "%s is klaar: level %s! Vahoeg!",
    G + "welkom.eigen": "Welkom op je erfenis, %s! Druk op je Bovenaanzicht-toets (O) om te bouwen. Terug: /guhs erfenis terug",
    G + "welkom.ander": "Je bezoekt de erfenis van %s. Bovenaanzicht (O) = alleen kijken.",
    # commands
    G + "cmd.niet_in": "Je bent niet in De Erfenis.",
    G + "cmd.geen_base": "%s heeft nog geen erfenis.",
    G + "cmd.geen_dim": "De Erfenis-dimensie bestaat niet op deze server.",
    "biome.guhs.erfenis": "De Erfenis",
    G + "cmd.bouwspul": "Bouwspul: %s (opslag %s).",
    G + "cmd.bouwspul_vol": "Bouwspul: %s. Meer past er niet in je opslag (%s): bouw een schuurtje of een hoger stadhuis.",
    G + "cmd.reset": "Je erfenis is weer zoals opa-guh hem achterliet.",
    G + "cmd.snel_aan": "Erfenis: bouwen gaat nu meteen (snel aan).",
    G + "cmd.snel_uit": "Erfenis: bouwen kost weer tijd (snel uit).",
    G + "cmd.npc": "Guhdorp %s (stadhuis %s) gemaakt in cel %s, %s.",
    # the overview
    G + "ui.titel": "Bovenaanzicht",
    G + "ui.van": "Erfenis van %s",
    G + "ui.dorp": "Guhdorp %s",
    G + "ui.kijken": "alleen kijken",
    G + "ui.bouwspul": "Bouwspul",
    G + "ui.knusheid": "Knusheid",
    G + "ui.stadhuis": "Stadhuis",
    G + "ui.bouwer": "Bouwer",
    G + "ui.bouwer_vrij": "vrij",
    G + "ui.bouwer_bezig": "bezig",
    G + "ui.level": "Level %s",
    G + "ui.in_aanbouw": "In aanbouw",
    G + "ui.verbeteren": "Verbeteren",
    G + "ui.verplaatsen": "Verplaatsen",
    G + "ui.draaien": "Draaien",
    G + "ui.ophalen": "Ophalen",
    G + "ui.sluiten": "Sluiten",
    G + "ui.terug": "Terug naar de wereld",
    G + "ui.wereldkaart": "Wereldkaart",
    G + "ui.naar_eigen": "Terug naar mijn erfenis",
    G + "ui.kosten": "Kost %s Bouwspul",
    G + "ui.tijd": "Duurt %s",
    G + "ui.direct": "Meteen klaar",
    G + "ui.volgende": "Naar level %s:",
    G + "ui.max": "Hoogste level bereikt",
    G + "ui.aantal": "%s/%s",
    G + "ui.produceert": "Maakt %s Bouwspul per minuut",
    G + "ui.binnen": "Binnen: %s/%s",
    G + "ui.opslag": "Opslag +%s",
    G + "ui.knus": "Knusheid +%s",
    G + "ui.bouwen": "Bouwen",
    G + "ui.hulp": "WASD / slepen: schuiven  ·  scroll: zoom  ·  Q/E: draaien  ·  Esc: terug",
    G + "ui.hulp_plaats": "Klik: neerzetten  ·  R: draaien  ·  rechtermuis / Esc: stoppen",
    G + "ui.hulp_muur": "Sleep een rij muurtjes  ·  rechtermuis / Esc: stoppen",
    G + "ui.hulp_verplaats": "Klik: hier neerzetten  ·  rechtermuis / Esc: stoppen",
    G + "ui.vraag": "Naar je erfenis?",
    G + "ui.vraag_uitleg": "Opa-guh heeft je een erfenis nagelaten: een eigen plekje om op te knappen. Er even heen?",
    G + "ui.ja": "Ja, njeg!",
    G + "ui.nee": "Nee, straks",
    G + "ui.snel": "SNEL",
}


def build(h):
    h.w(f"{h.D}/dimension_type/erfenis.json", DIM_TYPE)
    h.w(f"{h.D}/dimension/erfenis.json", DIM)
    h.w(f"{h.D}/worldgen/biome/erfenis.json", BIOME)
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "dimension_type", "erfenis.json"), DIM_TYPE)
    _schrijf(os.path.join(d, "dimension", "erfenis.json"), DIM)
    _schrijf(os.path.join(d, "worldgen", "biome", "erfenis.json"), BIOME)
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"erfenis: dimension + {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
