"""
Guhpixel slice "guhkade" (Java: feature/guhpixel/guhkade; namespace guhkade; English: tools/lang/en/c35_px_guhkade.json).

The Guhkade: two arcade cabinets for home from the Guhpixel shop (DESIGN_PX section 4), each a block of two halves with a
real little game on its screen:
  - guhkade_kast_flappy   Flappy Guh: a guh with tiny wings between kaasknabbel pillars, one button
  - guhkade_kast_pong     Mika-Pong: the ball is a rolling guh, the other paddle is a Mika's
This module makes the two blocks (blockstates, the two halves' models, item, loot), the sprite sheet of the games
(textures/guhkade/sprites.png, see guhpixel_guhkade_tex.py), the sounds (the cabinet's beeps: sounds.json entries on vanilla
note block sounds), the hidden advancements of the FTB quests, a test room, and every text. No recipe: a cabinet only comes
from the Verkoper-guh (the first one 250 muntjes, every next one 150).
Every text is Dutch here.
"""
import os
import re

from features import guhpixel_guhkade_tex as tex
from features import guhpixel_lib as lib
from features import uvfix

SPELLEN = ("flappy", "pong")
ROT = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
ADVANCEMENTS = ("guhkade_kast", "guhkade_gespeeld", "guhkade_flappy_mijlpaal", "guhkade_pong_mijlpaal", "guhkade_guh_verslagen")
# the same numbers as Guhkade.FLAPPY_MIJLPAAL / PONG_MIJLPAAL and GuhkadeSlice.PRIJS_* (checked in selfcheck)
FLAPPY_MIJLPAAL, PONG_MIJLPAAL, PRIJS_EERSTE, PRIJS_VOLGENDE = 10, 30, 250, 150

TEXTS = {
    # --- the two cabinets ---
    "block.guhs.guhkade_kast_flappy": "Guhkade-kast: Flappy Guh",
    "block.guhs.guhkade_kast_flappy.lore": "Rechtsklik om te spelen. Eén knop: fladderen! Je guhs komen ook een potje doen, njeg.",
    "block.guhs.guhkade_kast_pong": "Guhkade-kast: Mika-Pong",
    "block.guhs.guhkade_kast_pong.lore": "Rechtsklik om te spelen. Rol de guh langs de Mika! Je guhs komen ook een potje doen, njeg.",
    "gui.guhs.guhkade.spel.flappy": "Flappy Guh",
    "gui.guhs.guhkade.spel.pong": "Mika-Pong",
    "gui.guhs.guhkade.winkel.flappy": "Een echte speelkast voor thuis. Een guh met piepkleine vleugeltjes fladdert tussen de kaasknabbelpilaren door. "
                                      "Eén knop, meer niet! Je guhs spelen ook mee en houden hun eigen scores bij. "
                                      "Je eerste kast kost 250 muntjes, elke volgende 150.",
    "gui.guhs.guhkade.winkel.pong": "Een echte speelkast voor thuis. Het balletje is een rollende guh en aan de overkant staat een Mika te duwen. "
                                    "Je guhs spelen ook mee en houden hun eigen scores bij. "
                                    "Je eerste kast kost 250 muntjes, elke volgende 150.",
    # --- the words on the cabinet's own screen (a pixel font: capitals, digits and - ! ? . : + only) ---
    "gui.guhs.guhkade.scherm.naam.flappy": "FLAPPY GUH",
    "gui.guhs.guhkade.scherm.naam.pong": "MIKA-PONG",
    "gui.guhs.guhkade.scherm.top": "TOP",
    "gui.guhs.guhkade.scherm.start": "START",
    "gui.guhs.guhkade.scherm.af": "NJEG!",
    "gui.guhs.guhkade.scherm.record": "VAHOEG!",
    "gui.guhs.guhkade.scherm.tijd": "TIJD!",
    "gui.guhs.guhkade.scherm.demo": "DEMO",
    # --- the game screen ---
    "gui.guhs.guhkade.knop.start": "Start!",
    "gui.guhs.guhkade.knop.opnieuw": "Nog een keer!",
    "gui.guhs.guhkade.knop.doei": "Doei",
    "gui.guhs.guhkade.uitleg.flappy": "Spatie, W of klik: fladderen. Niet tegen de kaasknabbels, njeg!",
    "gui.guhs.guhkade.uitleg.pong": "W en S of de pijltjes: je batje. Terugrollen = 1 punt, langs de Mika = 5.",
    "gui.guhs.guhkade.tellen": "De kast telt je punten na...",
    "gui.guhs.guhkade.geen_antwoord": "De kast zegt niks terug. Zet hem even uit en weer aan, njeg.",
    "gui.guhs.guhkade.uit.verslagen": "Guhs verslagen: %s. Die gaan vast extra oefenen, njeg!",
    "gui.guhs.guhkade.uit.plaats": "Plek %s op deze kast. Vahoeg!",
    "gui.guhs.guhkade.uit.gewoon": "Net niet je beste potje. Nog een keer, njeg?",
    "gui.guhs.guhkade.top5": "Top 5 van deze kast",
    "gui.guhs.guhkade.jouw_best": "Jouw record: %s",
    "gui.guhs.guhkade.guh_tip": "Guhs in de buurt komen hier ook spelen. Versla je er een, dan kijkt hij sip en gaat hij extra oefenen.",
    # --- messages ---
    "gui.guhs.guhkade.bezet.speler": "%s staat al aan de knoppen. Even wachten, njeg!",
    "gui.guhs.guhkade.bezet.guh": "%s is net lekker bezig. Laat hem even uitspelen, njeg!",
    "gui.guhs.guhkade.verslagen": "Je hebt %s verslagen! Hij kijkt een beetje sip... en gaat extra oefenen, njeg.",
    "gui.guhs.guhkade.verslagen.weg": "Je hebt de score van %s verbroken! Wacht maar tot hij het ziet, njeg.",
    "gui.guhs.guhkade.guh.voorbij": "%1$s heeft je score bij %2$s verbroken: %3$s punten! Njeg njeg njeg.",
    # --- the Guhdex section ---
    "gui.guhs.guhkade.gids.kop": "De Guhkade",
    "gui.guhs.guhkade.gids.uitleg": "Speelkasten voor thuis uit de Guhpixel-winkel. Elke kast houdt zijn eigen top 5 bij, ook van je guhs.",
    "gui.guhs.guhkade.gids.best": "Record %s",
    "gui.guhs.guhkade.gids.best.waarde": "%1$s (%2$s potjes)",
    "gui.guhs.guhkade.gids.nog_niet": "Nog niet gespeeld, njeg",
    "gui.guhs.guhkade.gids.verslagen": "Guhs verslagen",
    "gui.guhs.guhkade.gids.tip": "Elke soort guh heeft zijn eigen plafond: een Wolkguh is een kei in Flappy Guh, een Teckelguh in Mika-Pong. "
                                 "Lastig, maar te verslaan!",
}

SOUNDS = {
    "guhkade.muntje": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.6, "volume": 0.7}],
    "guhkade.piep": [{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 1.4, "volume": 0.5},
                     {"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 1.7, "volume": 0.5}],
    "guhkade.punt": [{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 2.0, "volume": 0.7}],
    "guhkade.af": [{"name": "minecraft:block.note_block.didgeridoo", "type": "event", "pitch": 0.7, "volume": 0.7}],
    "guhkade.record": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 1.7, "volume": 0.5}],
    "guhkade.sip": [{"name": "guhs:guh_ambient3", "pitch": 0.7, "volume": 0.7}, {"name": "guhs:guh_ambient7", "pitch": 0.7, "volume": 0.7}],
}
ONDERTITELS = {
    "guhkade.muntje": "Muntje valt in de speelkast",
    "guhkade.piep": "Speelkast piept",
    "guhkade.punt": "Speelkast: punt erbij",
    "guhkade.af": "Speelkast: njeg!",
    "guhkade.record": "Speelkast: nieuw record",
    "guhkade.sip": "Guh snikt zachtjes",
}


def build(h):
    textures(h)
    blokken(h)
    lib.teksten(h, TEXTS)
    lib.geluid(h, SOUNDS, ONDERTITELS)
    for a in ADVANCEMENTS:
        lib.quest_adv(h, a)
    test_kamer(h)
    selfcheck(h)


# =====================================================================================================================
# textures
# =====================================================================================================================
def textures(h):
    h.save(tex.sprites(), "guhkade", "sprites.png")
    h.save(tex.kast_scherm(), "block", "guhkade_kast_scherm.png")
    h.save(tex.kast_rand(), "block", "guhkade_kast_rand.png")
    h.save(tex.knop((235, 70, 96)), "block", "guhkade_knop_rood.png")
    h.save(tex.knop((246, 196, 74)), "block", "guhkade_knop_geel.png")
    for spel in SPELLEN:
        h.save(tex.kast_zij(spel), "block", f"guhkade_kast_{spel}_zij.png")
        h.save(tex.kast_voor(spel), "block", f"guhkade_kast_{spel}_voor.png")
        h.save(tex.kast_bord(spel), "block", f"guhkade_kast_{spel}_bord.png")
        h.save(tex.kast_paneel(spel), "block", f"guhkade_kast_{spel}_paneel.png")
        h.save(tex.icoon(spel), "item", f"guhkade_kast_{spel}.png")


# =====================================================================================================================
# the blocks: two halves, the screen on the north side of the model (the blockstate turns it)
# =====================================================================================================================
ALLE = ("down", "up", "north", "south", "west", "east")


def el(frm, to, tex_, faces=ALLE, uv=None, face_tex=None, face_uv=None):
    e = {"from": list(frm), "to": list(to), "faces": {}}
    for f in faces:
        spec = {"texture": (face_tex or {}).get(f, tex_)}
        u = (face_uv or {}).get(f, uv)
        if u:
            spec["uv"] = u
        e["faces"][f] = spec
    return e


def kast_onder(spel):
    e = [
        el([1, 0, 3], [15, 16, 15], "#zij", face_tex={"north": "#voor", "south": "#rand", "down": "#rand", "up": "#rand"},
           face_uv={"north": [0, 0, 16, 16], "west": [0, 0, 16, 16], "east": [0, 0, 16, 16]}),
        el([1, 0, 2], [15, 2, 3], "#rand", faces=("north", "west", "east", "up", "down")),                       # the toe kick
        el([1, 10, 0], [15, 12, 3], "#rand", face_tex={"up": "#paneel"}, face_uv={"up": [0, 0, 16, 4]},
           faces=("north", "west", "east", "up", "down")),                                                         # the control shelf
    ]
    if spel == "flappy":
        e.append(el([6, 12, 0.4], [10, 12.9, 2.6], "#rood", faces=("north", "south", "west", "east", "up")))       # the one big button
    else:
        e += [
            el([4, 12, 1.1], [4.8, 14.6, 1.9], "#rand", faces=("north", "south", "west", "east")),                 # the stick and its knob
            el([3.6, 14.6, 0.7], [5.2, 16, 2.3], "#rood"),
            el([9, 12, 0.7], [10.8, 12.6, 2.5], "#geel", faces=("north", "south", "west", "east", "up")),
            el([11.8, 12, 0.7], [13.6, 12.6, 2.5], "#rood", faces=("north", "south", "west", "east", "up")),
        ]
    return e


def kast_boven(spel):
    return [
        el([1, 0, 5], [15, 14, 15], "#zij", face_tex={"south": "#rand", "down": "#rand", "up": "#rand", "north": "#rand"},
           face_uv={"west": [0, 0, 16, 16], "east": [0, 0, 16, 16]}),
        el([1, 0, 4], [15, 12, 5], "#rand", faces=("north", "west", "east", "up", "down")),                       # the bezel
        el([2, 1.5, 3.95], [14, 10.5, 4], "#scherm", faces=("north",), uv=[0, 0, 16, 16]),                        # the glass
        el([1, 0, 2], [2, 14, 5], "#zij", faces=("north", "south", "west", "east", "up", "down"),
           face_tex={"north": "#rand", "east": "#rand", "south": "#rand"}),                                        # the two cheeks
        el([14, 0, 2], [15, 14, 5], "#zij", faces=("north", "south", "west", "east", "up", "down"),
           face_tex={"north": "#rand", "west": "#rand", "south": "#rand"}),
        el([1, 11.5, 1], [15, 15.5, 5], "#rand", face_tex={"north": "#bord"}, face_uv={"north": [0, 4, 16, 12]}),  # the lit marquee
        el([1, 14, 5], [15, 15.5, 15], "#rand"),                                                                  # the cap
    ]


def blokken(h):
    A, D = h.A, h.D
    for spel in SPELLEN:
        bid = f"guhkade_kast_{spel}"
        tx = {"particle": f"guhs:block/{bid}_zij", "zij": f"guhs:block/{bid}_zij", "voor": f"guhs:block/{bid}_voor",
              "bord": f"guhs:block/{bid}_bord", "paneel": f"guhs:block/{bid}_paneel", "scherm": "guhs:block/guhkade_kast_scherm",
              "rand": "guhs:block/guhkade_kast_rand", "rood": "guhs:block/guhkade_knop_rood", "geel": "guhs:block/guhkade_knop_geel"}
        h.w(f"{A}/models/block/{bid}_onder.json", {"parent": "minecraft:block/block", "textures": tx, "elements": uvfix.binnen(kast_onder(spel))})
        h.w(f"{A}/models/block/{bid}_boven.json", {"parent": "minecraft:block/block", "textures": tx, "elements": uvfix.binnen(kast_boven(spel))})
        h.w(f"{A}/blockstates/{bid}.json", {"variants": {
            f"facing={f},half={half}": {"model": f"guhs:block/{bid}_{'onder' if half == 'lower' else 'boven'}", **({"y": r} if r else {})}
            for f, r in ROT for half in ("lower", "upper")}})
        h.item_model(bid)
        # the cabinet drops itself, from its lower half only
        h.w(f"{D}/loot_table/blocks/{bid}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
            {"type": "minecraft:item", "name": f"guhs:{bid}"}], "conditions": [
            {"condition": "minecraft:block_state_property", "block": f"guhs:{bid}", "properties": {"half": "lower"}},
            {"condition": "minecraft:survives_explosion"}]}]})
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:guhkade_kast_{s}" for s in SPELLEN])
    h.add_tag("guhs/tags/item/guhkade_kasten", [f"guhs:guhkade_kast_{s}" for s in SPELLEN])


# =====================================================================================================================
# the test room: a floor with both cabinets against the north wall, their screens to the south
# =====================================================================================================================
TEST = (16, 8, 16)
TEST_FLAPPY, TEST_PONG = (5, 1, 3), (10, 1, 3)


def test_kamer(h):
    s = lib.test_kamer(h, "guhkade_test_kasten", TEST)
    for (x, y, z), spel in ((TEST_FLAPPY, "flappy"), (TEST_PONG, "pong")):
        s.set(x, y, z, f"guhs:guhkade_kast_{spel}", {"facing": "south", "half": "lower"})
        s.set(x, y + 1, z, f"guhs:guhkade_kast_{spel}", {"facing": "south", "half": "upper"})
    s.save("guhkade_test_kasten")


# =====================================================================================================================
def selfcheck(h):
    lib.controleer(h, "guhpixel_guhkade", items=[f"guhkade_kast_{s}" for s in SPELLEN], keys=list(TEXTS) + [f"subtitles.guhs.{e}" for e in SOUNDS],
                   templates=("guhkade_test_kasten",))
    problems = []
    for spel in SPELLEN:
        for f in (f"blockstates/guhkade_kast_{spel}.json", f"models/block/guhkade_kast_{spel}_onder.json", f"models/block/guhkade_kast_{spel}_boven.json"):
            if not os.path.exists(f"{h.A}/{f}"):
                problems.append(f"missing {f}")
        if not os.path.exists(f"{h.D}/loot_table/blocks/guhkade_kast_{spel}.json"):
            problems.append(f"no loot table for guhkade_kast_{spel}")
    for a in ADVANCEMENTS:
        if not os.path.exists(f"{h.D}/advancement/quest/{a}.json"):
            problems.append(f"missing advancement quest/{a}")
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "guhkade")
    # the sprite table here and in Sprite.java are the same
    src = open(os.path.join(java, "spel", "Sprite.java"), encoding="utf-8").read()
    in_java = {m[0]: tuple(int(v) for v in m[1:]) for m in re.findall(r"^\s+([A-Z_0-9]+)\((\d+), (\d+), (\d+), (\d+)\)", src, re.M)}
    if in_java != tex.SPRITES:
        problems.append(f"Sprite.java and guhpixel_guhkade_tex.SPRITES differ: {sorted(set(in_java.items()) ^ set(tex.SPRITES.items()))}")
    for naam, (u, v, b, hh) in tex.SPRITES.items():
        for other, (u2, v2, b2, h2) in tex.SPRITES.items():
            if naam < other and u < u2 + b2 and u2 < u + b and v < v2 + h2 and v2 < v + hh:
                problems.append(f"sprites {naam} and {other} overlap")
        if u + b > tex.BLAD or v + hh > tex.BLAD:
            problems.append(f"sprite {naam} sticks out of the sheet")
    # the words on the cabinet's screen fit its pixel font
    for key, tekst in TEXTS.items():
        if key.startswith("gui.guhs.guhkade.scherm.") and not re.fullmatch(r"[0-9A-Z\-!?.:+ ]+", tekst):
            problems.append(f"{key}: '{tekst}' has letters the pixel font does not know")
    # numbers that the texts and the quests mention
    src = open(os.path.join(java, "Guhkade.java"), encoding="utf-8").read()
    if f"FLAPPY_MIJLPAAL = {FLAPPY_MIJLPAAL}, PONG_MIJLPAAL = {PONG_MIJLPAAL}" not in src:
        problems.append("the FTB milestones differ from Guhkade.java")
    src = open(os.path.join(java, "GuhkadeSlice.java"), encoding="utf-8").read()
    if f"PRIJS_EERSTE = {PRIJS_EERSTE}, PRIJS_VOLGENDE = {PRIJS_VOLGENDE}" not in src:
        problems.append("the prices differ from GuhkadeSlice.java")
    for event in SOUNDS:
        if f'"{event}"' not in src:
            problems.append(f"sound {event} is not registered in GuhkadeSlice.java")
    if problems:
        raise SystemExit("guhpixel_guhkade self-check failed:\n  " + "\n  ".join(problems))


# =====================================================================================================================
# FTB quests (chapter Guhpixel, section "Voor thuis"); no dependencies, knabbel rewards, never muntjes
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("guhkade_kast", "Een speelkast voor thuis", "De &dVerkoper-guh&r in de Guhpixel-lobby verkoopt echte &dGuhkade-kasten&r: &6Flappy Guh&r en "
      "&6Mika-Pong&r. Je eerste kast kost 250 muntjes, elke volgende 150. Koop er een en zet hem thuis neer: twee blokken hoog, met een scherm "
      "dat echt beweegt.", "guhs:guhkade_kast_flappy", [fq.adv("guhkade_kast")], rewards=(("guhs:kaas_knabbels", 8),), x=0, y=0, shape="gear", xp=100)
    q("guhkade_gespeeld", "Muntje erin!", "Rechtsklik op je kast en speel een potje. &6Flappy Guh&r: spatie, W of klik om te fladderen. "
      "&6Mika-Pong&r: W en S (of de pijltjes) voor je batje. Elke kast houdt zijn eigen top 5 bij, met namen.", "guhs:guhkade_kast_pong",
      [fq.adv("guhkade_gespeeld")], rewards=(("guhs:kaas_knabbels", 6),), x=1.5, y=0, xp=50)
    q("guhkade_flappy_mijlpaal", "Fladderkampioen", f"Haal &6{FLAPPY_MIJLPAAL} punten&r in Flappy Guh: {FLAPPY_MIJLPAAL} kaasknabbelpilaren voorbij "
      "zonder er eentje aan te tikken. De pilaren komen steeds sneller, njeg!", "guhs:guhkade_kast_flappy", [fq.adv("guhkade_flappy_mijlpaal")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=3, y=0, xp=200)
    q("guhkade_pong_mijlpaal", "De Mika is er klaar mee", f"Haal &6{PONG_MIJLPAAL} punten&r in Mika-Pong. De guh terugrollen is 1 punt, hem langs de "
      "Mika rollen is er 5. Drie keer langs jou en het potje is voorbij.", "guhs:guhkade_kast_pong", [fq.adv("guhkade_pong_mijlpaal")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=4.5, y=0, xp=200)
    q("guhkade_guh_verslagen", "Sorry, vadsje", "Je tamme guhs lopen af en toe zelf naar een kast om te spelen, en ze worden er steeds beter in "
      "(tot hun plafond: dat hangt af van hun soort). Haal een hogere score dan een guh op dezelfde kast. Hij kijkt even sip... en gaat dan "
      "extra oefenen.", "guhs:guh_spawn_egg", [fq.adv("guhkade_guh_verslagen")], rewards=(("guhs:kaas_knabbels", 12),), x=6, y=0,
      shape="gear", xp=300)
