"""
Guhpixel: the foundation ("kern") of the minigame-server parody (Java: feature/guhpixel; the nine slices have their own
modules tools/features/guhpixel_<slice>.py, the shared helpers are in guhpixel_lib.py).

This module makes:
  - the void dimension guhs:guhpixel (dimension, dimension type, biome): fixed daylight, no weather, no mob spawning, and
    beds that work by day without setting the spawn (the dimension type is written in the 26.1 shape on purpose: the old
    shape has no "always sleep, never set spawn" bed rule and mc26.py leaves a finished file alone)
  - block guhpixel_portaal (soort=in|uit, no item), block + item guhpixel_poort (the home gate, recipe with a Netwerkkabeltje),
    item guhpixel_netwerkkabeltje
  - the block tag guhs:guhpixel_bruikbaar (blocks that can be right-clicked in guhpixel; the slices add theirs)
  - the sounds guhpixel.muntje / .ontgrendeld / .portaal, the hidden advancement quest/guhpixel_ontgrendeld
  - the game test rooms px_test_16 / px_test_48 / px_test_96 and the kern's own test arena guhpixel/guhpixel_test_arena
  - every text of the kern (ranks, muntjes, shop screen, Guhdex tab, commands, rules)
"""
import os

import numpy as np
from PIL import Image

from features import guhpixel_lib as lib

FTB_CHAPTER = "guhs_guhpixel"

RANGEN = {"guh": "[GUH]", "vads": "[VADS]", "vads_plus": "[VADS+]", "mvg": "[MVG]", "mvg_plus_plus": "[MVG++]"}
WINKEL_GROEPEN = {"guhkade": "Guhkade", "guhkantoor": "Guhkantoor", "guhbioscoop": "Guhbioscoop", "among_pakjes": "Ruimtepakjes",
                  "among_hoedjes": "Hoedjes", "among_deco": "Scheepsdeco"}

TEXTS = {
    "dimension.guhs.guhpixel": "Guhpixel",
    "biome.guhs.guhpixel": "Guhpixel",
    "gui.guhs.guhdex.tab.guhpixel": "Guhpixel & uitjes",
    # --- blocks and items ---
    "block.guhs.guhpixel_portaal": "Guhpixel-portaal",
    "block.guhs.guhpixel_poort": "Guhpixel-poort",
    "block.guhs.guhpixel_poort.lore": "Rechtsklik: hup, naar de Guhpixel-lobby. Even inbellen, njeg... piiieeep krrr!",
    "item.guhs.guhpixel_netwerkkabeltje": "Netwerkkabeltje",
    "item.guhs.guhpixel_netwerkkabeltje.lore": "Van de Welkomstguh. Hiermee maak je thuis een Guhpixel-poort. Niet op knabbelen!",
    "subtitles.guhs.guhpixel.muntje": "Muntje rinkelt",
    "subtitles.guhs.guhpixel.ontgrendeld": "Guhpixel gaat open",
    "subtitles.guhs.guhpixel.portaal": "Modem piept en kraakt",
    # --- ranks and muntjes ---
    "gui.guhs.guhpixel.rang.nieuw": "Nieuwe Guhpixel-rang: %s! Vahoeg!",
    "gui.guhs.guhpixel.muntjes.erbij": "+%s muntjes",
    "gui.guhs.guhpixel.hud.muntjes": "Muntjes: %s",
    # --- getting in ---
    "gui.guhs.guhpixel.ontgrendeld.titel": "GUHPIXEL",
    "gui.guhs.guhpixel.ontgrendeld.onder": "Verbinding gelukt, njeg!",
    "gui.guhs.guhpixel.ontgrendeld.chat": "Guhpixel is nu open voor jou! Typ voortaan /lobby, /hub of /l om erheen te gaan. Vahoeg!",
    "commands.guhs.guhpixel.lobby.op_slot": "Zoek eerst het Guh-internetcafé \"De Trage Verbinding\" in de Guhmensie (je superkompas wijst de weg) en loop door het grote beeldscherm.",
    "commands.guhs.guhpixel.lobby.op_slot.1": "Onbekend commando. Of nou ja... njeg. Bijna bekend!",
    "commands.guhs.guhpixel.lobby.op_slot.2": "Verbinden met Guhpixel... mislukt. Heb je hem al uit en weer aan gezet, njeg?",
    "commands.guhs.guhpixel.lobby.op_slot.3": "De lobby is vol. Vol met slapende guhs. En jij hebt nog geen kaartje, njeg.",
    "commands.guhs.guhpixel.lobby.op_slot.4": "Fout 404: lobby niet gevonden. De guh die de weg weet doet net een dutje.",
    "gui.guhs.guhpixel.binnen.spel_bezig": "Maak eerst je spelletje af, njeg!",
    "gui.guhs.guhpixel.binnen.afstappen": "Eerst afstappen: guhs en ritjes blijven lekker thuis, njeg.",
    "gui.guhs.guhpixel.binnen.wakker": "Eerst wakker worden, vadsje!",
    "gui.guhs.guhpixel.binnen.storing": "Storing bij Guhpixel... probeer het zo nog eens, njeg.",
    "gui.guhs.guhpixel.kabeltje.heb_je_al": "Je hebt al een Netwerkkabeltje, njeg!",
    "gui.guhs.guhpixel.roep.niet_hier": "In Guhpixel komen geen guhs: ze passen niet door de kabel, njeg.",
    "gui.guhs.guhpixel.roep.niet_thuis": "%s is er even niet en komt vanzelf weer terug, njeg.",
    "gui.guhs.band.plek.op_vakantie": "Op vakantie in %1$s, njeg",
    "gui.guhs.band.plek.op_kantoor": "Aan het werk op het Guhkantoor",
    "gui.guhs.band.dim.guhs.guhpixel": "Guhpixel",
    # --- games ---
    "gui.guhs.guhpixel.spel.aantal": "Dit spel is voor %1$s tot %2$s spelers, njeg.",
    "gui.guhs.guhpixel.spel.op_slot": "%s heeft Guhpixel nog niet ontdekt, njeg.",
    "gui.guhs.guhpixel.spel.bezig": "%s is nog met iets anders bezig, njeg.",
    "gui.guhs.guhpixel.spel.niet_hier": "%s is niet in de lobby, njeg.",
    "gui.guhs.guhpixel.kluis.terug": "Je eigen spullen zijn terug: precies zoals je ze had. Niks kwijt, njeg!",
    "gui.guhs.guhpixel.kluis.rest": "Je zakken zitten vol. Wacht nog op een vrij plekje: %s stapeltje(s). Niks kwijt, njeg!",
    "gui.guhs.guhpixel.regels.niet_bouwen": "Hier wordt niks gesloopt of gebouwd, njeg. Alleen gevadst.",
    "gui.guhs.guhpixel.regels.void": "Oeps, van het randje gevallen! Hup, terug.",
    "gui.guhs.guhpixel.grap.stap": "Stap %1$s/%2$s: %3$s",
    "gui.guhs.guhpixel.grap.klaar": "+100 muntjes en een aandenken. Vahoeg!",
    "gui.guhs.guhpixel.grap.opnieuw": "Nog een keer! Net zo leuk, njeg.",
    "gui.guhs.guhpixel.film.nieuw": "Nieuwe film in de Guhbioscoop: %s!",
    "gui.guhs.guhpixel.aandenken.kleding": "Nieuw pakje voor je guhs: %s!",
    "gui.guhs.guhpixel.aandenken.item": "Aandenken: %s!",
    "gui.guhs.guhpixel.aandenken.heb_je_al": "Die heb je nog, njeg! Kijk eens in je zakken.",
    "gui.guhs.guhpixel.npc.klik": "KLIK OM TE SPELEN",
    "gui.guhs.guhpixel.npc.spelers": "%1$s spelers · %2$s guhs",
    # --- the shop ---
    "gui.guhs.guhpixel.winkel.titel": "Guhpixel-winkel",
    "gui.guhs.guhpixel.winkel.leeg": "Alles is op. Kom later terug, njeg!",
    "gui.guhs.guhpixel.winkel.grap.1": "Alleen cosmetisch, echt waar, njeg",
    "gui.guhs.guhpixel.winkel.grap.2": "Nu 0% korting!",
    "gui.guhs.guhpixel.winkel.grap.3": "Niet goed? Geld blijft weg, njeg",
    "gui.guhs.guhpixel.winkel.prijs": "%1$s muntjes",
    "gui.guhs.guhpixel.winkel.prijs_al": "%1$s muntjes · al %2$s gekocht",
    "gui.guhs.guhpixel.winkel.heb_je": "Heb je al, vahoeg!",
    "gui.guhs.guhpixel.winkel.knop.koop": "Koop",
    "gui.guhs.guhpixel.winkel.knop.op": "Heb je",
    "gui.guhs.guhpixel.winkel.max": "Gekocht: %1$s van %2$s",
    "gui.guhs.guhpixel.winkel.gekocht": "Gekocht: %s. Vahoeg!",
    "gui.guhs.guhpixel.winkel.nee.onbekend": "Dat verkoop ik niet meer, njeg.",
    "gui.guhs.guhpixel.winkel.nee.niet_hier": "Kopen kan alleen in de Guhpixel-lobby, njeg.",
    "gui.guhs.guhpixel.winkel.nee.max": "Daar heb je er al genoeg van, njeg.",
    "gui.guhs.guhpixel.winkel.nee.eis": "Dat kun je nog niet kopen, njeg.",
    "gui.guhs.guhpixel.winkel.nee.te_duur": "Daar heb je nog niet genoeg muntjes voor, njeg.",
    "gui.guhs.guhpixel.winkel.nee.mislukt": "Dat lukte niet. Je muntjes heb je nog, njeg.",
    # --- the Guhdex tab ---
    "gui.guhs.guhpixel.gids.zoek_cafe": "Zoek het Guh-internetcafé, njeg! Je superkompas (tab Minigames) wijst de weg. Loop daar door het grote beeldscherm.",
    "gui.guhs.guhpixel.gids.laden": "Even inbellen... piiieeep krrr...",
    "gui.guhs.guhpixel.gids.totaal": "Ooit verdiend: %s",
    "gui.guhs.guhpixel.gids.rang_hoogste": "Hoogste rang. Vahoeg!",
    "gui.guhs.guhpixel.gids.rang_volgende": "%1$s over %2$s muntjes",
    "gui.guhs.guhpixel.gids.rang.tip.titel": "Je Guhpixel-rang",
    "gui.guhs.guhpixel.gids.rang.tip": "Je rang hangt af van alle muntjes die je ooit verdiend hebt (uitgeven mag!). Hij staat voor je naam: in de chat, in de spelerslijst en boven je hoofd.",
    "gui.guhs.guhpixel.gids.rang.tip.uit": "Klik: rang niet meer laten zien",
    "gui.guhs.guhpixel.gids.rang.tip.aan": "Klik: rang weer laten zien",
    "gui.guhs.guhpixel.gids.stappen": "Stappen",
    "gui.guhs.guhpixel.gids.geheim": "??? (njeg, nog geheim)",
    "gui.guhs.guhpixel.gids.gespeeld": "Uitgespeeld",
    "gui.guhs.guhpixel.gids.nog_niet": "Nog niet gevonden, njeg",
    # --- picking a guh ---
    "gui.guhs.guhpixel.kiezer.kies": "Kies een guh uit de lijst",
    "gui.guhs.guhpixel.kiezer.geen": "Je hebt hier nog geen guh voor, njeg.",
    "gui.guhs.guhpixel.kiezer.bezet.rijdt": "Zit net ergens op (of iemand op hem), njeg.",
    "gui.guhs.guhpixel.kiezer.bezet.huisje": "Ligt te snurken in zijn Guhhuisje. Laat maar even, njeg.",
    "gui.guhs.guhpixel.kiezer.bezet.guhkamer": "Logeert in de Guhkamer.",
    "gui.guhs.guhpixel.kiezer.bezet.baby": "Nog te klein, njeg. Eerst groeien!",
    "gui.guhs.guhpixel.kiezer.bezet.bezig": "Is al ergens anders druk mee, njeg.",
}
for _id, _naam in RANGEN.items():
    TEXTS[f"gui.guhs.guhpixel.rang.{_id}"] = _naam
for _id, _naam in WINKEL_GROEPEN.items():
    TEXTS[f"gui.guhs.guhpixel.winkel.groep.{_id}"] = _naam

SOUNDS = {
    "guhpixel.muntje": [{"name": "minecraft:entity.experience_orb.pickup", "type": "event", "pitch": 1.4, "volume": 0.6}],
    "guhpixel.ontgrendeld": [{"name": "minecraft:ui.toast.challenge_complete", "type": "event", "pitch": 1.3, "volume": 0.7}],
    "guhpixel.portaal": [{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 0.6},
                         {"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 1.7}],
}

BEIGE, BEIGE_D, BEIGE_L = (214, 200, 168), (168, 152, 120), (236, 226, 200)


def build(h):
    dimensie(h)
    textures(h)
    blokken(h)
    lib.teksten(h, TEXTS)
    lib.geluid(h, SOUNDS)
    lib.quest_adv(h, "guhpixel_ontgrendeld")
    h.add_tag("guhs/tags/block/guhpixel_bruikbaar", [])
    for n in (16, 48, 96):
        lib.test_kamer(h, f"px_test_{n}", (n, 12 if n < 96 else 16, n))
    test_arena(h)
    selfcheck(h)


# =====================================================================================================================
# the dimension
# =====================================================================================================================
def dimensie(h):
    D = h.D
    h.w(f"{D}/dimension/guhpixel.json", {"type": "guhs:guhpixel", "generator": {
        "type": "minecraft:flat", "settings": {"biome": "guhs:guhpixel", "layers": [], "lakes": False, "features": False,
                                               "structure_overrides": []}}})
    # written in the 26.1 shape (see the module comment): bright fixed noon, beds work by day and never set the spawn
    h.w(f"{D}/dimension_type/guhpixel.json", {
        "attributes": {
            "minecraft:audio/background_music": {
                "default": {"sound": "minecraft:music.game", "min_delay": 12000, "max_delay": 24000},
                "creative": {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000}},
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "always"},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/ambient_light_color": "#0a0a0a",
            "minecraft:visual/cloud_color": "#ccffffff",
            "minecraft:visual/cloud_height": 150.33,
            "minecraft:visual/fog_color": "#ffd6ea",
            "minecraft:visual/sky_color": "#8fd0ff",
        },
        "has_skylight": True, "has_ceiling": False, "has_ender_dragon_fight": False, "coordinate_scale": 1.0,
        "ambient_light": 0.1, "min_y": 0, "height": 256, "logical_height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld", "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "has_fixed_time": True, "timelines": "#minecraft:universal"})
    # (the biome is written in the old shape like every other biome: mc26.py converts its effects)
    h.w(f"{D}/worldgen/biome/guhpixel.json", {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.0,
        "effects": {"sky_color": 0x8FD0FF, "fog_color": 0xFFD6EA, "water_color": 0x7AD0FF, "water_fog_color": 0x5AA0E0},
        "spawners": {}, "spawn_costs": {}, "carvers": {"air": []}, "features": []})


# =====================================================================================================================
# textures
# =====================================================================================================================
def _ruis(basis, var, seed, size=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _portaal(kleur, licht, seed):
    """A translucent screen full of scan lines and a few bright pixels."""
    a = _ruis(kleur, 10, seed)
    for y in range(16):
        if y % 2 == 0:
            a[y, :, :3] = np.clip(a[y, :, :3].astype(int) + 26, 0, 255)
    rng = np.random.default_rng(seed + 1)
    for _ in range(9):
        x, y = rng.integers(1, 15, 2)
        a[y, x, :3] = licht
    a[..., 3] = 176
    return Image.fromarray(a)


def _kast():
    a = _ruis(BEIGE, 4, 7101)
    a[0, :, :3] = BEIGE_L
    a[15, :, :3] = BEIGE_D
    a[:, 0, :3] = BEIGE_L
    a[:, 15, :3] = BEIGE_D
    for x in range(3, 13, 2):                   # cooling slits
        a[4:7, x, :3] = BEIGE_D
    return Image.fromarray(a)


def _scherm():
    a = _ruis(BEIGE, 4, 7102)
    a[0, :, :3] = BEIGE_L
    a[15, :, :3] = BEIGE_D
    # the tube: a dark screen with a pink GUH face and scan lines
    for y in range(2, 12):
        for x in range(2, 14):
            if (x in (2, 13)) and (y in (2, 11)):
                continue
            a[y, x, :3] = (40, 18, 46) if y % 2 else (58, 28, 66)
    for x, y in ((5, 5), (10, 5)):              # eyes
        a[y, x, :3] = (255, 150, 200)
        a[y + 1, x, :3] = (255, 150, 200)
    for x in (6, 7, 8, 9):                      # a smile
        a[9, x, :3] = (255, 150, 200)
    a[8, 5, :3] = (255, 150, 200)
    a[8, 10, :3] = (255, 150, 200)
    a[13, 12, :3] = (110, 220, 130)             # the power light
    for x in range(3, 8):
        a[13, x, :3] = BEIGE_D                  # a floppy slot
    return Image.fromarray(a)


def _kabeltje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    blauw, donker, plug, goud = (70, 150, 235, 255), (36, 90, 170, 255), (232, 232, 240, 255), (255, 214, 90, 255)
    pad = [(3, 12), (4, 11), (5, 10), (5, 9), (6, 8), (7, 8), (8, 7), (9, 7), (10, 6), (10, 5), (11, 4), (12, 3)]
    for x, y in pad:
        img.putpixel((x, y), blauw)
        img.putpixel((x, y + 1), donker)
    for x0, y0 in ((1, 12), (12, 1)):           # two little plugs
        for dx in range(3):
            for dy in range(3):
                img.putpixel((x0 + dx, y0 + dy), plug)
        img.putpixel((x0 + 1, y0 + 1), goud)
    return img


def textures(h):
    h.save(_portaal((255, 122, 200), (255, 240, 250), 7001), "block", "guhpixel_portaal_in.png")
    h.save(_portaal((110, 230, 160), (240, 255, 245), 7002), "block", "guhpixel_portaal_uit.png")
    h.save(_kast(), "block", "guhpixel_poort_kast.png")
    h.save(_scherm(), "block", "guhpixel_poort_scherm.png")
    h.save(_kabeltje(), "item", "guhpixel_netwerkkabeltje.png")


# =====================================================================================================================
# blocks and items
# =====================================================================================================================
def blokken(h):
    A = h.A
    for soort in ("in", "uit"):
        h.w(f"{A}/models/block/guhpixel_portaal_{soort}.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                                                  "textures": {"all": f"guhs:block/guhpixel_portaal_{soort}"}})
    h.w(f"{A}/blockstates/guhpixel_portaal.json", {"variants": {f"soort={s}": {"model": f"guhs:block/guhpixel_portaal_{s}"} for s in ("in", "uit")}})
    # the gate: a little beige CRT monitor (the screen on the north side)
    h.w(f"{A}/models/block/guhpixel_poort.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "guhs:block/guhpixel_poort_kast", "kast": "guhs:block/guhpixel_poort_kast", "scherm": "guhs:block/guhpixel_poort_scherm"},
        "elements": [
            {"from": [1, 2, 1], "to": [15, 14, 15], "faces": {
                "north": {"texture": "#scherm", "uv": [0, 0, 16, 16]}, "south": {"texture": "#kast"}, "west": {"texture": "#kast"},
                "east": {"texture": "#kast"}, "up": {"texture": "#kast"}, "down": {"texture": "#kast"}}},
            {"from": [4, 0, 4], "to": [12, 2, 12], "faces": {f: {"texture": "#kast"} for f in ("north", "south", "west", "east", "down")}},
        ]})
    h.w(f"{A}/blockstates/guhpixel_poort.json", {"variants": h.facing_states("guhpixel_poort")})
    h.w(f"{A}/models/item/guhpixel_poort.json", {"parent": "guhs:block/guhpixel_poort"})
    h.self_drop("guhpixel_poort")
    h.item_model("guhpixel_netwerkkabeltje")
    # the gate needs the cable the lobby greeter gives (so it can only be made after the first visit); the cable is used up
    h.shaped("guhpixel_poort", ["III", "GKG", "IRI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "K": "guhs:guhpixel_netwerkkabeltje",
                                                      "R": "minecraft:redstone"}, "guhs:guhpixel_poort", 1)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:guhpixel_poort"])


# =====================================================================================================================
# test templates
# =====================================================================================================================
TEST_ARENA = (9, 6, 9)


def test_arena(h):
    """The kern's own arena for the game tests: a floor, a gold block in the middle of it and one armor stand (a template's
    entities are spawned at every stamp)."""
    s = h.Structure(TEST_ARENA)
    for x in range(TEST_ARENA[0]):
        for z in range(TEST_ARENA[2]):
            s.set(x, 0, z, "minecraft:smooth_stone")
    s.set(4, 0, 4, "minecraft:gold_block")
    s.entity(1.5, 1.0, 1.5, {"id": "minecraft:armor_stand", "NoGravity": h.ms.Byte(1), "Invulnerable": h.ms.Byte(1)})
    s.save("guhpixel/guhpixel_test_arena")


# =====================================================================================================================
def selfcheck(h):
    lib.controleer(h, "guhpixel", blokken=("guhpixel_poort",), items=("guhpixel_poort", "guhpixel_netwerkkabeltje"), keys=TEXTS,
                   templates=("px_test_16", "px_test_48", "px_test_96", "guhpixel/guhpixel_test_arena"))
    problems = []
    for f in ("dimension/guhpixel.json", "dimension_type/guhpixel.json", "worldgen/biome/guhpixel.json", "recipe/guhpixel_poort.json",
              "advancement/quest/guhpixel_ontgrendeld.json", "tags/block/guhpixel_bruikbaar.json"):
        if not os.path.exists(f"{h.D}/{f}"):
            problems.append(f"missing data/guhs/{f}")
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "Rang.java"), encoding="utf-8").read()
    for rid in RANGEN:
        if rid.upper() + "(" not in src:
            problems.append(f"rank {rid} is not in Rang.java")
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "Winkel.java"), encoding="utf-8").read()
    for gid in WINKEL_GROEPEN:
        if f'"{gid}"' not in src:
            problems.append(f"shop group {gid} is not in Winkel.GROEPEN")
    if problems:
        raise SystemExit("guhpixel self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    pass
