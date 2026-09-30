"""
Het Guh-Circuit (2.9, De Grote Guhspelen): one big circuit in the Guhvelden and the Kaasvlakte with three race tracks
(Regenboogbaan, Vadsbaan, Kaasbergbaan) around a Pitpaleis with two grandstands, and Coach Vahoegvroem (CIRCUITGUH) who
lends you a race guh on the track and level of your choice. Coin: circuitbeker; outfit: circuit helmpje, vahoeg-racepak,
geblokte vlagcape (only at her shop).

build(h)  textures, models, tags, lang, advancements, Coach Vahoegvroem's looks, the guh_circuit structure (+ self-check)
ftb(fq)   the FTB quests (section "circuit" in guhs_minigames)
Helpers: circuit_banen.py (the tracks), circuit_bouw.py (everything around them), circuit_tex.py (textures, models).
The Java side lives in nl.juiced.guhs.feature.circuit (and the race engine in nl.juiced.guhs.feature.race).
"""
import random

from features import circuit_banen as cb
from features import circuit_bouw as bouw
from features import circuit_tex as tex

NAME = "guh_circuit"
BIOMES = ["guh_fields", "kaas_flats"]
SALT = 20290501
CLOTHES = ["circuit_helmpje", "circuit_racepak", "circuit_vlagcape"]

# --- guh model bones (make_guh_variants format: parent, pivot, swatch, [(origin, size, inflate)]) ---------------------------
_H = [0, 6, -2]
_B = [0, 6, 6]
BONES = {
    # a round racing helmet over the top of the head, a stripe over the middle and a tinted visor in front
    "outfit_circuit_helm": ("head", _H, "circuit_helm", [([-6.2, 13.6, -11.6], [12.4, 3.6, 10.8], 0), ([-5.2, 17.2, -10.6], [10.4, 1.4, 8.8], 0)]),
    "outfit_circuit_helm_streep": ("head", _H, "circuit_streep", [([-1.2, 17.3, -11.0], [2.4, 1.5, 9.6], 0.05)]),
    "outfit_circuit_helm_vizier": ("head", _H, "circuit_vizier", [([-6.0, 12.6, -12.4], [12.0, 2.2, 1.0], 0)]),
    # the chequered flag cape: on the back, hanging down behind the tail
    "outfit_circuit_vlagcape": ("body", _B, "circuit_vlag", [([-6.6, 11.6, -1.2], [13.2, 0.4, 12.2], 0), ([-6.6, 3.2, 10.8], [13.2, 8.8, 0.4], 0)]),
    "outfit_circuit_vlagcape_stok": ("body", _B, "circuit_stok", [([-0.4, 11.9, 9.6], [0.8, 4.8, 0.8], 0)]),
}


def clothes(rng, v):
    px = v.SWATCH * 4
    import numpy as np

    def chequer(a_col, b_col, cell=8):
        def paint():
            a = np.zeros((px, px, 3), np.float32)
            for y in range(px):
                for x in range(px):
                    a[y, x] = a_col if ((x // cell) + (y // cell)) % 2 == 0 else b_col
            return np.clip(a + rng.normal(0, 3, (px, px, 1)), 0, 255)
        return paint

    def racepak():
        a = np.zeros((px, px, 3), np.float32)
        a[:, :] = (248, 246, 250)
        for x in range(px):
            if 10 <= x < 14 or 18 <= x < 22:
                a[:, x] = (230, 70, 150)                               # two pink racing stripes
            if 14 <= x < 18:
                a[:, x] = (250, 200, 60)                               # a golden stripe between them
        for y in range(0, px, 16):                                     # little chequered cuffs
            for x in range(px):
                a[y:y + 3, x] = (30, 30, 40) if (x // 3) % 2 else (250, 250, 250)
        return np.clip(a + rng.normal(0, 3, (px, px, 1)), 0, 255)

    def vizier():
        a = np.zeros((px, px, 3), np.float32)
        for y in range(px):
            a[y, :] = (90 + y * 2, 60 + y, 150 + y)
        a[4:7, 4:20] = (230, 220, 255)
        return a

    return {
        "circuit_helmpje": {"circuit_helm": lambda: v.fabric((245, 90, 160), rng, 6), "circuit_streep": chequer((250, 250, 250), (30, 30, 40), 4),
                            "circuit_vizier": vizier},
        "circuit_racepak": {"suit": racepak},
        "circuit_vlagcape": {"circuit_vlag": chequer((250, 250, 250), (30, 30, 40), 8), "circuit_stok": lambda: v.fabric((250, 200, 60), rng, 8)},
    }


def icons(ic):
    pink, dark, white, gold, black, visor = (245, 90, 160), (150, 40, 100), (250, 250, 250), (250, 200, 60), (35, 35, 45), (120, 80, 170)
    helm = ic.pad(["....aaaaaaaa....", "...abbbwkbbba...", "..abbbbkwbbbba..", "..abbbbwkbbbba..", ".abbbbbkwbbbbba.", ".abbbbbwkbbbbba.",
                   ".avvvvvvvvvvvva.", ".avvvvvvvvvvvva.", "..aaaaaaaaaaaa.."])
    suit = [r[:4] + "".join(("c" if ch == "b" and i in (7, 8) else ("g" if ch == "b" and i in (6, 9) else ch)) for i, ch in enumerate(r[4:12], 4)) + r[12:]
            for r in ic.SHIRT]
    cape = ic.pad(["gg..............", "gwkwkwkwkwkwkw..", "gkwkwkwkwkwkwk..", "gwkwkwkwkwkwkw..", "gkwkwkwkwkwkwk..", "gwkwkwkwkwkwkw..",
                   "gkwkwkwkwkwkwk..", "gwkwkwkwkwkwkw..", "g...............", "g...............", "g..............."])
    return {
        "circuit_helmpje": ic.icon(helm, {"a": dark, "b": pink, "w": white, "k": black, "v": visor}),
        "circuit_racepak": ic.icon(ic.pad(suit), {"a": (170, 170, 180), "b": white, "c": pink, "g": gold}),
        "circuit_vlagcape": ic.icon(cape, {"g": gold, "w": white, "k": black}),
    }


# ======================================================================================================================
# texts (Dutch, also in en_us)
# ======================================================================================================================
TEXTS = {
    # blocks, items, entities
    "block.guhs.circuit_regenboogweg": "Regenboogweg",
    "block.guhs.circuit_regenboogweg_plaat": "Regenboogwegplaat",
    "block.guhs.circuit_kaasweg": "Kaasweg",
    "block.guhs.circuit_kaasweg_plaat": "Kaaswegplaat",
    "block.guhs.circuit_kaassaus": "Glibberige kaassaus",
    "block.guhs.circuit_bergijs": "Kaasbergijs",
    "block.guhs.circuit_boostring": "Regenboog-boostring",
    "block.guhs.circuit_stuiterpaddenstoel": "Stuiterpaddenstoel",
    "block.guhs.circuit_mikaplek": "Mika-pikkerplek",
    "block.guhs.circuit_rolplek": "Rolknabbelplek",
    "block.guhs.circuit_looping": "Begin van de Vadslooping",
    "item.guhs.circuitbeker": "Circuitbeker",
    "item.guhs.circuit_helmpje": "Circuithelmpje",
    "item.guhs.circuit_racepak": "Vahoeg-racepak",
    "item.guhs.circuit_vlagcape": "Geblokte vlagcape",
    "entity.guhs.circuit_mikapikker": "Mika-pikker",
    "entity.guhs.circuit_rolknabbel": "Rollende kaasknabbel",
    "entity.guhs.guh_npc.circuitguh": "Coach Vahoegvroem",
    f"structure.guhs.{NAME}": "Het Guh-Circuit",
    f"structure.guhs.{NAME}.tooltip": "Minigame: drie racebanen (Regenboogbaan, Vadsbaan, Kaasbergbaan) met Coach Vahoegvroem - makkelijk, medium of lastig",
    "gui.guhs.guhdex.rarity.circuitguh": "Zeldzaamheid: Uniek (in het Pitpaleis van het Guh-Circuit)",
    "gui.guhs.guhdex.info.circuitguh": "Coach Vahoegvroem kent elke bocht van haar drie banen uit haar hoofd. Met haar koptelefoon op en haar gouden stopwatch om roept ze de hele dag \"VAHOEG! Nog een rondje!\". Ze leent je een renguh op de Regenboogbaan, de Vadsbaan of de Kaasbergbaan. Pas op voor de Mika-pikkers: die pikken je VAHOEG-vaart, hihihi!",
    # the tracks' names (the race panel, the screens, the boards)
    "gui.guhs.race.baan.regenboog": "Regenboogbaan",
    "gui.guhs.race.baan.vads": "Vadsbaan",
    "gui.guhs.race.baan.kaasberg": "Kaasbergbaan",
    # Coach Vahoegvroem talking
    "quest.guhs.circuit.hello_new": "VAHOEG! Welkom op het Guh-Circuit, vads! Ik ben Coach Vahoegvroem. Drie banen, drie niveaus: de Regenboogbaan zweeft door de lucht, de Vadsbaan heeft een LOOPING en de Kaasbergbaan gaat de berg op. Kies maar, ik leen je een renguh!",
    "quest.guhs.circuit.hello": "Daar is mijn favoriete coureur weer! Welke baan wordt het vandaag? Je renguh heeft al een kaasknabbel op. VAHOEG!",
    "quest.guhs.circuit.hello_busy": "Ssst, er wordt geracet! Ga op de tribune zitten en juich maar hard: VAHOEG! Straks ben jij aan de beurt.",
    "quest.guhs.circuit.go_go": "Waarom sta je met mij te kletsen? RACEN! Je renguh wacht op je! VAHOEG!",
    "quest.guhs.circuit.go.regenboog": "De Regenboogbaan! Hou je vast: we gaan de lucht in. Rij door de regenboogringen voor een VAHOEG-zet en spring over de gaten!",
    "quest.guhs.circuit.go.vads": "De Vadsbaan! Glibberige kaassaus in de haarspeldbochten, stuiterpaddenstoelen en... DE VADSLOOPING! Gewoon doorrennen, je renguh doet de rest!",
    "quest.guhs.circuit.go.kaasberg": "De Kaasbergbaan! Omhoog over de Knabbelhelling: pas op, de Mika's rollen kaasknabbels naar beneden! Bovenop is het glad van het ijs.",
    "quest.guhs.circuit.shop": "Het circuitpakje! Een helmpje, een vahoeg-racepak en een geblokte vlagcape. Alleen hier, alleen voor circuitbekers. Dan is jouw guh een echte coureur!",
    "quest.guhs.circuit.result": "Over de finish in %s (rondes: %s). %s! Je krijgt %s circuitbeker(s).",
    "quest.guhs.circuit.record": "NIEUW PERSOONLIJK RECORD: %s (was %s)! Je geest rijdt voortaan deze race. +2 circuitbekers",
    "quest.guhs.circuit.first": "Je allereerste race op deze baan! Een welkomstzakje: 4 extra circuitbekers en een handvol kaasknabbels. VAHOEG!",
    "quest.guhs.circuit.track_record": "BAANRECORD! %s racete de %s (%s) in %s: plek 1 op het scorebord! Die race rijdt voortaan mee als gouden geest!",
    "quest.guhs.circuit.gepikt": "Hihihi! Een Mika-pikker pikte je VAHOEG-vaart!",
    "quest.guhs.circuit.rolknabbel": "BOEM! Een rollende kaasknabbel! Boven op de berg giechelen de Mika's...",
    "quest.guhs.circuit.looping": "VADSLOOPING!",
    "quest.guhs.circuit.looping.sub": "Wieeeeeee! VAHOEG!",
    "quest.guhs.circuit.gouden_geest": "Je was sneller dan de gouden geest! Het baanrecord is van jou!",
    "quest.guhs.circuit.pikker.hallo": "Hihihi! Ik pik alleen maar vaart, hoor. Knabbels zijn voor later, njeg!",
    "quest.guhs.circuit.duwer.hallo": "Hihi, deze kaasknabbel rolt zo naar beneden! Duwen maar!",
    # her screen
    "gui.guhs.circuit.baan.regenboog": "Een zwevende regenboogweg door de lucht: twee sprongen over het niets, een slingerende chicane en regenboogringen die je een VAHOEG-zet geven. Achter je renguh een regenboogspoor!",
    "gui.guhs.circuit.baan.vads": "Door Vadsland: glibberige kaassaus in de haarspeldbochten, stuiterpaddenstoelen op het rechte stuk en de beroemde VADSLOOPING. Hou je vast!",
    "gui.guhs.circuit.baan.kaasberg": "De Kaasberg op! Mika's rollen kaasknabbels over de Knabbelhelling naar beneden: ontwijk ze! Boven is het ijs glad, dan zoef je weer naar beneden.",
    "gui.guhs.circuit.baan.regenboog.kort": "Zwevend, sprongen, regenboogringen",
    "gui.guhs.circuit.baan.vads.kort": "Kaassaus, stuiterpaddenstoelen, de Vadslooping",
    "gui.guhs.circuit.baan.kaasberg.kort": "Omhoog, rollende kaasknabbels, ijs op de top",
    "gui.guhs.circuit.niveau.makkelijk": "Een rustige renguh, weinig Mika-pikkers, en val je van de baan dan zet ik je een klein stukje terug",
    "gui.guhs.circuit.niveau.medium": "Een gewone renguh en een paar Mika-pikkers",
    "gui.guhs.circuit.niveau.lastig": "Een supersnelle maar eigenwijze renguh, veel Mika-pikkers en rolknabbels, en de zilveren VAHOEG-pads doen het niet. +50 % circuitbekers!",
    "gui.guhs.circuit.start": "RACEN: %s (%s)",
    "gui.guhs.circuit.start.tooltip": "Je krijgt een huur-renguh; na de race gaat hij terug naar de pitstop",
    "gui.guhs.circuit.shop.tooltip": "Het circuitpakje voor je guh, voor circuitbekers",
    "gui.guhs.circuit.busy": "%s racet nu op de %s (ronde %s). Even wachten, of juich mee vanaf de tribune!",
    "gui.guhs.circuit.jouw": "Jouw records (%s):",
    "gui.guhs.circuit.medailles": "Goud %s  Zilver %s  Brons %s  (%s rondes)",
    "gui.guhs.circuit.munten": "Goud %s circuitbekers ... uitgereden %s (+2 bij een record)",
    "gui.guhs.scorebord.circuit": "%s: snelste races",
    "gui.guhs.scorebord.circuit.ronde": "%s: snelste rondes",
    "gui.guhs.circuit.no_build": "Njeg! Niet bouwen of graven op het Guh-Circuit: dan struikelen de renguhs.",
}

ADVANCEMENTS = [  # name, parent, icon, frame, criteria (None: code-granted), title, description
    ("circuit_gevonden", "root", "guhs:circuitbeker", "task",
     {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{NAME}"}}}}},
     "Welkom op het circuit!", "Vind het Guh-Circuit"),
    ("circuit_eerste", "circuit_gevonden", "guhs:circuit_regenboogweg", "task", None,
     "Brrrm, VAHOEG!", "Rij je eerste race op het Guh-Circuit"),
    ("circuit_regenboog", "circuit_eerste", "guhs:circuit_boostring", "task", None,
     "Boven de wolken", "Rij de Regenboogbaan uit"),
    ("circuit_vads", "circuit_eerste", "guhs:circuit_kaassaus", "task", None,
     "Wieeee, de looping!", "Rij de Vadsbaan uit"),
    ("circuit_kaasberg", "circuit_eerste", "guhs:circuit_bergijs", "task", None,
     "Bovenop de Kaasberg", "Rij de Kaasbergbaan uit"),
    ("circuit_alle_banen", "circuit_eerste", "guhs:circuitbeker", "goal", None,
     "Circuitkampioen", "Rij alle drie de banen van het Guh-Circuit uit"),
    ("circuit_lastig", "circuit_eerste", "guhs:circuit_stuiterpaddenstoel", "goal", None,
     "Eigenwijze renguh", "Rij een race op lastig op het Guh-Circuit"),
    ("circuit_goud_lastig", "circuit_lastig", "minecraft:gold_block", "challenge", None,
     "Goud op lastig! VAHOEG!", "Haal goud op lastig op een baan van het Guh-Circuit"),
    ("circuit_gouden_geest", "circuit_eerste", "minecraft:gold_ingot", "challenge", None,
     "Sneller dan de legende", "Versla de gouden geest en pak zelf het baanrecord"),
    ("circuit_kleding", "circuit_gevonden", "guhs:circuit_helmpje", "goal",
     {"items": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
         {"items": "guhs:circuit_helmpje"}, {"items": "guhs:circuit_racepak"}, {"items": "guhs:circuit_vlagcape"}]}},
      "done": {"trigger": "minecraft:impossible"}},
     "Echte coureur", "Verzamel het hele circuitpakje: helmpje, racepak en vlagcape"),
]


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def advancements(h):
    h.w(f"{h.D}/advancement/quest/seen_circuitguh.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, criteria, title, desc in ADVANCEMENTS:
        crit = criteria or {"done": {"trigger": "minecraft:impossible"}}
        adv = {"parent": f"guhs:grote_guhspelen/{parent}",
               "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.grote_guhspelen.{name}.title"},
                           "description": {"translate": f"advancements.guhs.grote_guhspelen.{name}.description"},
                           "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
               "criteria": crit}
        if len(crit) > 1:
            adv["requirements"] = [list(crit)]
        h.w(f"{h.D}/advancement/grote_guhspelen/{name}.json", adv)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.title", title, title)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.description", desc, desc)


def tags(h):
    h.add_tag("guhs/tags/block/race_track", cb.TRACK_TAG)
    h.add_tag("guhs/tags/block/race_boost", ["guhs:circuit_boostring"])
    h.add_tag("guhs/tags/block/race_marker", ["guhs:circuit_mikaplek", "guhs:circuit_rolplek", "guhs:circuit_looping"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:circuit_regenboogweg", "guhs:circuit_regenboogweg_plaat", "guhs:circuit_kaasweg",
                                                         "guhs:circuit_kaasweg_plaat", "guhs:circuit_bergijs"])
    h.add_tag("minecraft/tags/block/mineable/shovel", ["guhs:circuit_kaassaus"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:circuit_stuiterpaddenstoel"])
    h.add_tag("minecraft/tags/block/slabs", ["guhs:circuit_regenboogweg_plaat", "guhs:circuit_kaasweg_plaat"])


# ======================================================================================================================
# the structure
# ======================================================================================================================
def build_structure(h):
    mc = h.mc
    s = h.Structure((bouw.W, bouw.H, bouw.D))
    rng = random.Random(2905)
    bouw.ground(s)
    bouw.kaasberg(s, rng)
    banen = cb.all_banen()
    info = cb.build_tracks(s, banen, mc)
    byname = {b.name: b for b in banen}
    bouw.kaasberg_face_and_flag(s, rng)
    info["kaasberg"]["rollen"], _ = bouw.mikapoort(s, byname["kaasberg"], mc)
    bouw.clouds_under_deck(s, info, byname["regenboog"], rng)
    # the hub
    bouw.tribune(s, rng, 70, 122, 70, "north")
    bouw.tribune(s, rng, 70, 122, 116, "south")
    bouw.pitpaleis(s, rng)
    bouw.boulevard(s, rng)
    # Vadsland
    bouw.kaasfabriekje(s, rng, 21, 158)
    bouw.kaassaus_vijver(s, 46, 158, 54, 184)
    bouw.cheese_wedge(s, 24, 132, 14, 7)
    bouw.cheese_wedge(s, 48, 133, 12, 6)
    for z in (148, 160, 172):
        bouw.guh_paddenstoel(s, 4, z, 4 + (z % 3), 2)
    bouw.guh_paddenstoel(s, 88, 170, 6, 3)
    bouw.guh_paddenstoel(s, 28, 173, 5, 2)
    for y in range(bouw.G, bouw.G + 4):
        s.set(90, y, 146, mc("quartz_pillar"), {"axis": "y"})
    bouw.face_disc(s, 90.5, bouw.G + 6, 146.5, 4, "west", skin=mc("yellow_wool"), cheek=mc("orange_wool"))
    # the Regenboog field
    bouw.rainbow_arch(s, 70, 44, 9)
    bouw.rainbow_arch(s, 128, 44, 9)
    bouw.cloud_hill(s, 100, 45, 7, 3, 3)
    bouw.cloud_hill(s, 44, 44, 5, 3, 2)
    # lamps, flowers, trees; the air above; Coach Vahoegvroem; the anchor
    bouw.road_lamps(s, banen, rng)
    bouw.flowers_and_trees(s, rng, banen)
    s.clear_above([(x, z) for x in range(bouw.W) for z in range(bouw.D)], bouw.G)
    s.entity(bouw.NPC[0], float(bouw.NPC[1]), bouw.NPC[2], {"id": "guhs:guh_npc", "Kind": "circuitguh", "PersistenceRequired": h.Byte(1),
                                                            "Rotation": h.floats(0.0, 0.0)})
    # 2.10.1: a Reisguh in the Pitpaleis just inside its west door, looking at who comes in: the circuit's travel waypoint
    from features import reisguh_plek
    reisguh_plek.zet(s, reisguh_plek.rondom(75, bouw.G, 97), "Guhcircuit", 90.0, h.Byte, h.floats, "(Guhcircuit)")
    ax, ay, az = bouw.ANCHOR_POS
    floor = s.get(ax, ay, az)
    s.set(ax, ay, az, mc("jigsaw"), {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": bouw.ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": floor or mc("pink_concrete"), "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    return s, banen, info


def structure(h):
    s, banen, info = build_structure(h)
    problems = []
    for b in banen:
        problems += cb.check(s, b)
    problems += bouw.check(s, banen, info)
    if problems:
        raise SystemExit("guh circuit self-check failed:\n  " + "\n  ".join(problems[:40]))
    laps = ", ".join(f"{b.name} {b.length:.0f} x {b.laps}" for b in banen)
    print(f"guh circuit: laps {laps}; self-check ok")
    h.TEMPLATE_SIZES[NAME] = bouw.W
    h.FLATNESS[NAME] = 48                    # (sampled 192 wide: the fields roll a bit; the template is sunk and cleared)
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, BIOMES, spacing=42, separation=14, salt=SALT, start_y=-(bouw.G - 1), reach=100, centre=bouw.ANCHOR,
                spawn_overrides={"creature": none, "monster": none, "ambient": none})
    s.save(NAME)


def build(h):
    tex.textures(h)
    tex.models(h)
    tex.coach(h)
    tags(h)
    texts(h)
    advancements(h)
    structure(h)


# ======================================================================================================================
# FTB quests (section "circuit" in guhs_minigames)
# ======================================================================================================================
def ftb(fq):
    fq.q("circuit_vinden", "Welkom op het circuit!", "Vind &dhet Guh-Circuit&r in de Guhvelden of de Kaasvlakte (superkompas: Minigames > Guh-Circuit). "
         "In het Pitpaleis wacht Coach Vahoegvroem met een huur-renguh voor je.",
         "guhs:guhmensie_superkompas", [fq.structure(NAME)], x=0, y=0)
    fq.q("circuit_eerste", "Brrrm, VAHOEG!", "Rij je eerste race op het Guh-Circuit. Kies een baan en een niveau bij Coach Vahoegvroem: je hoeft niks mee te nemen.",
         "guhs:circuitbeker", [fq.adv("guhs:grote_guhspelen/circuit_eerste")], rewards=(("guhs:kaas_knabbels", 16),), deps=("circuit_vinden",),
         x=2, y=0, xp=100)
    fq.q("circuit_regenboog", "Boven de wolken", "Rij de &dRegenboogbaan&r uit. Rij door de regenboogringen voor een VAHOEG-zet en spring over de gaten in de weg!",
         "guhs:circuit_boostring", [fq.adv("guhs:grote_guhspelen/circuit_regenboog")], rewards=(("guhs:circuitbeker", 2),), deps=("circuit_eerste",),
         x=4, y=-1, xp=100)
    fq.q("circuit_vads", "Wieeee, de looping!", "Rij de &6Vadsbaan&r uit: glibberige kaassaus, stuiterpaddenstoelen en de Vadslooping. Gewoon doorrennen!",
         "guhs:circuit_kaassaus", [fq.adv("guhs:grote_guhspelen/circuit_vads")], rewards=(("guhs:circuitbeker", 2),), deps=("circuit_eerste",),
         x=4, y=0, xp=100)
    fq.q("circuit_kaasberg", "Bovenop de Kaasberg", "Rij de &bKaasbergbaan&r uit. Ontwijk de rollende kaasknabbels van de Mika's op de Knabbelhelling!",
         "guhs:circuit_bergijs", [fq.adv("guhs:grote_guhspelen/circuit_kaasberg")], rewards=(("guhs:circuitbeker", 2),), deps=("circuit_eerste",),
         x=4, y=1, xp=100)
    fq.q("circuit_alle_banen", "Circuitkampioen", "Rij alle drie de banen van het Guh-Circuit uit.",
         "guhs:circuitbeker", [fq.adv("guhs:grote_guhspelen/circuit_alle_banen")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),),
         deps=("circuit_regenboog", "circuit_vads", "circuit_kaasberg"), x=6, y=0, shape="gear", xp=300)
    fq.q("circuit_lastig", "Eigenwijze renguh", "Rij een race op &clastig&r: een supersnelle maar eigenwijze renguh, veel Mika-pikkers, en de zilveren VAHOEG-pads doen het niet.",
         "guhs:circuit_stuiterpaddenstoel", [fq.adv("guhs:grote_guhspelen/circuit_lastig")], rewards=(("guhs:circuitbeker", 3),), deps=("circuit_eerste",),
         x=6, y=-1, xp=200)
    fq.q("circuit_gouden_geest", "Sneller dan de legende", "Versla de &6gouden geest&r: het baanrecord van de wereld rijdt met je mee. Ben jij sneller?",
         "minecraft:gold_ingot", [fq.adv("guhs:grote_guhspelen/circuit_gouden_geest")], rewards=(("guhs:circuitbeker", 4),), deps=("circuit_eerste",),
         x=6, y=1, shape="gear", xp=300)
    fq.q("circuit_kleding", "Echte coureur", "Koop het hele circuitpakje bij Coach Vahoegvroem: circuithelmpje, vahoeg-racepak en geblokte vlagcape. Alleen daar!",
         "guhs:circuit_helmpje", [fq.adv("guhs:grote_guhspelen/circuit_kleding")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),),
         deps=("circuit_eerste",), x=8, y=0, shape="gear", xp=300)
