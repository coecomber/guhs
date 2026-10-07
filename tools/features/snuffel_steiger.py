"""
Het Snuffeleiland, the DOCK and the OPENING (DESIGN_VERHALENPAD C "Getting there"). Java: feature/snuffelsteiger. Builds
on the kern (snuffel.py: the questline "snuffeleiland", whose first two steps are played here).

  snuffel_steiger_bouw.py       the template guhs:steigerhuisje (the cottage on its quay, the pier) and the game tests' floor
  snuffel_steiger_modellen.py   the captain's boat (model, animations, texture) and the puppies' lying down
  snuffel_steiger_wiki.py       the wiki entries (CONTRACT_130 2.4; not built here)
  here                          the structure guhs:steigerhuisje of the Guhmensie (type guhs:kust_steiger: on a shore of a
                                Diepe Guhzee, the pier out to sea), its normal set and its one guaranteed copy in new
                                terrain, the Superkompas texts, the sounds, every Dutch text (who lives there, the sickbed,
                                the captain, the four cutscenes), the self-check against Steiger.java

The numbers of the shore come from diepzee.py (the sea's noise, its dam, where its water is): this module only says which
band of that noise is "near a shore" and what a spot must look like (LAND, WATER, OPEN_ZEE below).
"""
import json
import os
import re

from . import diepzee as dz
from . import snuffel_steiger_bouw as bouw
from . import snuffel_steiger_modellen as modellen
from . import verhaal_motor
from . import wereld

NAME = "snuffel_steiger"
STRUCTUUR = "steigerhuisje"
TYPE = "guhs:kust_steiger"
SALT = 21305201
# the normal set: a candidate chunk every SPACING chunks; only the ones on a fitting shore become a dock
SPACING, SEPARATION = 6, 2
# the one guaranteed copy, in terrain that does not exist yet (alleen_nieuw): the ring grows x1.5 and x2 when it is all old
# land (a server whose Guhmensie exists out to 3000 blocks finds its spot between 3000 and 4000)
GEGARANDEERD = dict(sector=3, min=900, max=2000)

# --- what a spot must look like (KustStructure.java) -----------------------------------------------------------------------
# the band of the sea's noise that is "near a shore" (the middle of a start chunk), and the contour the walk goes to
BAND = (round(dz.DAM[0] - 0.06, 3), dz.DEEP_TO)
OEVER = round((dz.DAM[2] + dz.DAM[3]) / 2, 4)
# land: [x, z, lo, hi] = the ground's top block lies lo..hi above the quay's layer at this template column
# (the plot brings its own foundation, bouw.FUNDERING deep, and a retaining wall that follows the land up to bouw.KEERMUUR:
# a hole or a hill within those is no problem; right beside the pier the land must be low, or the quay ends in a cliff)
LAAG, HOOG = -(bouw.FUNDERING - 2), bouw.KEERMUUR
LAND = [[1, 0, LAAG, HOOG], [11, 0, LAAG, HOOG], [21, 0, LAAG, HOOG], [1, 6, LAAG, HOOG], [21, 6, LAAG, HOOG], [11, 6, LAAG, HOOG],
        [6, 3, LAAG, HOOG], [16, 3, LAAG, HOOG], [1, 12, LAAG, HOOG - 2], [21, 12, LAAG, HOOG - 2], [6, 12, LAAG, 3], [15, 12, LAAG, 3]]
# water: [x, z, diepte] = sea at least this deep at this template column (the pier, its platform, the boat's berth and way out)
WATER = [[10, 18, 2], [10, 24, 3], [10, 31, 4], [5, 30, 3], [15, 28, 3], [15, 33, 4]]
# still sea this many blocks past the anchor, straight out: the crossing's storm plays 46 blocks out
OPEN_ZEE = [24, 34, 44, 54, 62]
# how far the pieces can lie from the middle of the start chunk: the walk to the contour, the search for the water's edge,
# and the template's own reach from its anchor (BouwRuimte: keep_clear)
LOOP, RAND = 40, 16


def reach():
    ax, _, az = bouw.ANKER
    return LOOP + RAND + max(ax, bouw.SX - 1 - ax, az, bouw.SZ - 1 - az) + 1


# =====================================================================================================================
# the structure
# =====================================================================================================================
def processor():
    """The rule processor of the start pool: every marker of the plot's outer ring (snuffel_steiger_bouw.MARKERS) becomes
    its flat-land block where the world has air on that spot, and a block of the retaining wall where the land stands
    there (the rules are tried in order, the first that fits wins)."""
    def is_blok(name):
        return {"predicate_type": "minecraft:block_match", "block": name}

    rules = []
    for marker, (vlak, muur) in bouw.MARKERS.items():
        uit = {"Name": "minecraft:air"} if vlak is None else ({"Name": vlak[0], "Properties": dict(vlak[1])} if vlak[1] else {"Name": vlak[0]})
        rules.append({"input_predicate": is_blok(marker), "location_predicate": is_blok("minecraft:air"), "output_state": uit})
        rules.append({"input_predicate": is_blok(marker), "location_predicate": {"predicate_type": "minecraft:always_true"},
                      "output_state": {"Name": muur}})
    return {"processor_type": "minecraft:rule", "rules": rules}


def structuur(h):
    s, problems = bouw.steigerhuisje(h)
    if problems:
        raise SystemExit("snuffel_steiger: the steigerhuisje is not right:\n  " + "\n  ".join(problems[:40]))
    s.save(STRUCTUUR)
    bouw.test_vloer(h).save("snuffelsteiger_test_vloer")
    D = h.D
    h.w(f"{D}/worldgen/structure/{STRUCTUUR}.json", {
        "type": TYPE, "biomes": f"#guhs:has_structure/{STRUCTUUR}", "step": "surface_structures", "spawn_overrides": {},
        "terrain_adaptation": "none", "start_pool": f"guhs:{STRUCTUUR}/start",
        "zee_noise": f"guhs:{dz.SEA_NOISE}", "band_van": BAND[0], "band_tot": BAND[1], "oever": OEVER, "water_van": dz.WATER_FROM,
        "water_y": dz.WATER_LEVEL - 1,
        "anker": list(bouw.ANKER), "y": bouw.DEK_Y, "land": LAND, "water": WATER, "open_zee": OPEN_ZEE})
    h.w(f"{D}/worldgen/template_pool/{STRUCTUUR}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{STRUCTUUR}",
                                  "projection": "rigid", "processors": {"processors": [processor()]}}}]})
    entry = [{"structure": f"guhs:{STRUCTUUR}", "weight": 1}]
    h.w(f"{D}/worldgen/structure_set/{STRUCTUUR}.json", {"structures": entry, "placement": {
        "type": "minecraft:random_spread", "spacing": SPACING, "separation": SEPARATION, "salt": SALT}})
    g = GEGARANDEERD
    h.w(f"{D}/worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json", {"structures": entry, "placement": {
        "type": "guhs:gegarandeerd", "salt": SALT + wereld.GEGARANDEERD_SALT, "min_afstand": g["min"], "max_afstand": g["max"],
        "alleen_nieuw": True, "sector": g["sector"], "sectoren": wereld.SECTOREN}})
    h.w(f"{D}/tags/worldgen/biome/has_structure/{STRUCTUUR}.json", {"values": [f"guhs:{dz.BIOME}"]})
    # how far its pieces reach (make_v2.bouwruimte() writes keep_clear and moves the step behind the sea's water)
    h.RUIMTE_HOOKS[TYPE] = lambda s_json, jigsaw_reach: reach()
    h.VOORRANG.pop(STRUCTUUR, None)
    # a story place: never within 600 blocks of 0,0
    h.add_tag("guhs/tags/worldgen/structure/verhaal", [f"guhs:{STRUCTUUR}"])
    # for the merge step's cross-check (every new structure has a Superkompas entry or a sluier): tab Verhalen
    wereld.STRUCTUREN[STRUCTUUR] = dict(soort="land", kompas="verhalen", gegarandeerd=g, voorrang=None, salt=SALT, spacing=SPACING,
                                        biomes=[dz.BIOME])
    wereld._schrijf_overzicht(h)


# =====================================================================================================================
# sounds (vanilla sounds, pitched; no new sound files, no music)
# =====================================================================================================================
SOUNDS = {
    "snuffelsteiger.piep": [{"name": "minecraft:entity.wolf_cute.whine", "type": "event", "pitch": 1.5, "volume": 0.8},
                            {"name": "minecraft:entity.wolf.whine", "type": "event", "pitch": 1.7, "volume": 0.7}],
    "snuffelsteiger.plof": [{"name": "minecraft:block.wool.fall", "type": "event", "pitch": 0.8}],
    "snuffelsteiger.donder": [{"name": "minecraft:entity.lightning_bolt.thunder", "type": "event", "volume": 0.5}],
    "snuffelsteiger.bel": [{"name": "minecraft:block.bell.use", "type": "event"}],
    "snuffelsteiger.plank": [{"name": "minecraft:block.wood.step", "type": "event"}],
    "snuffelsteiger.riem": [{"name": "minecraft:entity.boat.paddle_water", "type": "event"}],
    "snuffelsteiger.golf": [{"name": "minecraft:entity.generic.splash", "type": "event", "pitch": 0.6},
                            {"name": "minecraft:entity.player.splash.high_speed", "type": "event", "pitch": 0.7}],
}
SUBTITLES = {
    "snuffelsteiger.piep": "Pup piept", "snuffelsteiger.plof": "Plof", "snuffelsteiger.donder": "Donder in de verte",
    "snuffelsteiger.bel": "Scheepsbel", "snuffelsteiger.plank": "Stap op de planken", "snuffelsteiger.riem": "Boot vaart",
    "snuffelsteiger.golf": "Grote golf",
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# Dutch texts
# =====================================================================================================================
Q, O = "quest.guhs.snuffelsteiger.", "gui.guhs.snuffelsteiger.optie."
LANG = {
    f"structure.guhs.{STRUCTUUR}": "Steigerhuisje",
    f"structure.guhs.{STRUCTUUR}.tooltip": "Een huisje met een steiger aan de Diepe Guhzee. Hier begint het verhaal van het Snuffeleiland",
    "entity.guhs.steiger_bewoner": "Steigerhond",
    "entity.guhs.steiger_bewoner.pup": "Kleine Wiebel",
    "entity.guhs.steiger_bewoner.buur": "Buurvrouw Mandje",
    "entity.guhs.steiger_boot": "De Natte Neus",
    # the start
    Q + "feest_zo": "Sst, het lantaarnfeest begint zo. Kijk maar mee, njeg!",
    Q + "hint.ziekbed": "Kleine Wiebel ligt in bed in het huisje. Ga gauw kijken",
    Q + "hint.kapitein": "Praat met Kapitein Zoutsnoet aan het eind van de steiger",
    # the sickbed (six pages; the fourth is the puppy)
    Q + "ziekbed.1": "Daar ben je. Schrik maar niet: Wiebel gloeit als een kacheltje en lust niet eens een knabbeltje. En een pup die geen "
                     "knabbel lust... dat is de snuffelkoorts, njeg.",
    Q + "ziekbed.2": "Daar helpt maar één ding tegen: de geneesbloem. Die groeit alleen op het Snuffeleiland, ver weg over de Diepe Guhzee.",
    Q + "ziekbed.3": "Je papa is hem weken geleden gaan zoeken. Weken! En sindsdien: geen brief, geen blaf, niks. Ik maak me zorgen om die "
                     "ouwe Zwerfpoot.",
    Q + "ziekbed.4": "Piep... ik wil papa... en een knabbel... maar eerst papa...",
    Q + "ziekbed.5": "Luister goed. Hier in de Guhmensie zie jij eruit zoals altijd. Maar op het Snuffeleiland ben je gewoon wie je bent: een "
                     "hond, net als Wiebel en je papa. En alleen een hond met een goede neus vindt die bloem.",
    Q + "ziekbed.6": "Ik pas op Wiebel, dag en nacht. Maar iemand moet de bloem gaan halen. En papa zoeken.",
    O + "achterna": "Ik ga papa achterna!",
    O + "nadenken": "Ik moet er even over nadenken",
    Q + "buur.dapper": "Dapper, njeg! Kapitein Zoutsnoet ligt met De Natte Neus aan de steiger. Hij is de enige die de weg naar het eiland weet.",
    Q + "buur.ga": "Ga maar gauw naar de kapitein, aan het eind van de steiger. Ik blijf bij Wiebel.",
    Q + "buur.terug": "%s! Wiebel slaapt onrustig, maar ik ben erbij. Heb je de bloem al? Nog niet? Dan gauw terug naar het eiland, njeg.",
    Q + "buur.spoor": "Een spoor van papa? Echt waar, %s? Dan komt het vast goed. Wiebel droomt er al van, kijk maar.",
    Q + "pup.ga": "Piep... ga je papa halen? En de bloem? En... een knabbel voor onderweg?",
    Q + "pup.slaapt": "Zzz... %s... papa... knabbel... zzz...",
    Q + "pup.spoor": "Piep! %s, heb je papa geroken? Echt echt? Dan word ik gauw beter. Njeg...",
    # the captain
    Q + "kapitein.eerst_ziekbed": "Ahoi. Wat een schrik, hè, met die kleine. Ga eerst maar bij het ziekbed kijken, in het huisje. Ik loop niet "
                                  "weg, njeg.",
    Q + "kapitein.wie": "Naar het Snuffeleiland? Dat is een woeste overtocht, landrot. En daar lopen alleen honden rond: zodra je voet aan wal "
                        "zet, ben je er zelf weer een. Dus vertel eens: welke hond ben jij, onder die jas? En welk maatje gaat er met je mee?",
    O + "kies": "Dat zal ik je laten zien",
    O + "nog_niet": "Nog even niet",
    Q + "kapitein.gekozen": "Een %s die %s heet, met %s als maatje. Kijk aan, njeg! Zeg het maar als je klaar bent om uit te varen.",
    Q + "kapitein.klaar": "Alles aan boord, %s? Het wordt ruig weer, dat voel ik aan mijn snorharen. Varen we uit?",
    O + "uitvaren": "Hijs de zeilen!",
    O + "andere_hond": "Ik wil toch een andere hond of een ander maatje",
    Q + "kapitein.weer": "Ahoi, %s! De Natte Neus ligt klaar. Terug naar het Snuffeleiland? Dit keer blijf je in de boot, afgesproken?",
    O + "varen": "Vaar maar, kapitein!",
    O + "blijven": "Ik blijf nog even hier",
}

NAMEN = {"buur": "Buurvrouw Mandje", "kapitein": "Kapitein Zoutsnoet", "pup": "Kleine Wiebel"}
SCENES = {  # id: (titel, regels, namen)
    "snuffelsteiger_feest": ("Het lantaarnfeest", {
        "avond": "Het is de avond van het lantaarnfeest. Het hele dorpje staat op de steiger.",
        "ahoi": "Ahoi! Lampions aan, botjes op tafel. Feest, njeg!",
        "bed": "Kleine Wiebel hoort allang in bed te liggen. Maar feest is feest...",
        "kef": "Kef! Lampions! Njeg njeg njeg!",
        "mandje": "Wiebel! Jij hoort in je mandje!",
        "duizelig": "Piep... alles draait zo...",
        "gloeit": "O nee! Wiebel gloeit helemaal!",
        "binnen": "Vlug, naar binnen. In bed ermee!",
    }, NAMEN),
    "snuffelsteiger_overtocht": ("De overtocht", {
        "los": "Alle poten aan boord? Dan gooien we los. Op naar het Snuffeleiland, njeg!",
        "uit": "En zo vaar je uit, de roze Guhzee op. Voor Wiebel. En voor papa.",
        "lucht": "Zie je die donkere lucht daar? Daar houd ik niet van. Helemaal niet, njeg.",
        "golven": "Hou je vast! Golven zo hoog als een guhhuisje!",
        "terug": "Dit wordt te wild! Ik keer om. Morgen proberen we het weer.",
        "nee": "Morgen is te laat voor Wiebel! Dan zwem ik wel!",
        "landrot": "Landrot! Kom terug! ... Njeg. Die is dapper. Of knettergek.",
        "zwart": "De golven worden hoger en hoger... en dan wordt alles zwart.",
    }, NAMEN),
    "snuffelsteiger_vaart": ("Naar het Snuffeleiland", {
        "weer": "Los die trossen! En denk erom: dit keer niet overboord springen, njeg.",
    }, NAMEN),
    "snuffelsteiger_thuiskomst": ("Weer thuis", {
        "daar": "Daar zijn ze! Welkom thuis, njeg!",
        "thuis": "Veilig aan de steiger, landrot. Het eiland loopt niet weg.",
    }, NAMEN),
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for event, text in SUBTITLES.items():
        h.lang(f"subtitles.guhs.{event}", text, text)
    for scene, (titel, regels, namen) in SCENES.items():
        verhaal_motor.scene(h, scene, titel, regels, namen)


# =====================================================================================================================
# self-check
# =====================================================================================================================
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "snuffelsteiger")


def java_sleutels():
    """Every lang key the Java of this slice names: in full, or as Q/O + "name" in SteigerVerhaal; and every scene line."""
    keys = set()
    src = open(os.path.join(JAVA, "SteigerVerhaal.java"), encoding="utf-8").read()
    for m in re.finditer(r'\b([QO]) \+ "([\w.]+)"', src):
        keys.add((Q if m.group(1) == "Q" else O) + m.group(2))
    for f in os.listdir(JAVA):
        if f.endswith(".java") and not f.endswith("GameTests.java"):
            for m in re.finditer(r'"((?:gui|quest|entity|structure)\.guhs\.[\w.]*[\w])"', open(os.path.join(JAVA, f), encoding="utf-8").read()):
                keys.add(m.group(1))
    scenes = open(os.path.join(JAVA, "SteigerScenes.java"), encoding="utf-8").read()
    per_scene = re.split(r'Cutscene\.maak\("', scenes)[1:]
    for deel in per_scene:
        sid = deel.split('"', 1)[0]
        for m in re.finditer(r'\.zeg\(\d+, (?:"\w*"|Cutscene\.SPELER), "(\w+)"', deel):
            keys.add(f"scene.guhs.{sid}.{m.group(1)}")
        keys.add(f"scene.guhs.{sid}.titel")
    return keys


def selfcheck(h):
    problems = list(modellen.check(h))
    src = open(os.path.join(JAVA, "Steiger.java"), encoding="utf-8").read()

    def java(patroon, wat, verwacht):
        m = re.search(patroon, src)
        if not m:
            problems.append(f"Steiger.java: {wat} not found")
            return
        gevonden = tuple(float(x) for x in m.groups())
        if any(abs(a - b) > 1e-6 for a, b in zip(gevonden, verwacht)) or len(gevonden) != len(verwacht):
            problems.append(f"Steiger.java {wat} = {gevonden}, the template says {tuple(verwacht)}")

    G = bouw.G
    java(r"G = (\d+), DEK_Y = (\d+);", "G, DEK_Y", (G, bouw.DEK_Y))
    java(r"ANKER = new BlockPos\((\d+), G, (\d+)\)", "ANKER", (bouw.ANKER[0], bouw.ANKER[2]))
    java(r"SX = (\d+), SZ = (\d+), PLOT_Z = (\d+);", "SX, SZ, PLOT_Z", (bouw.SX, bouw.SZ, bouw.PLOT_Z))
    num = r"(-?[\d.]+)"
    for naam, (plek, yaw) in (("PUP", bouw.PLEK_PUP), ("BUUR", bouw.PLEK_BUUR), ("KAPITEIN", bouw.PLEK_KAPITEIN), ("BOOT", bouw.PLEK_BOOT)):
        java(rf"\b{naam} = new Vec3\({num}, {num}, {num}\)", naam, (plek[0], plek[1] - (G + 1), plek[2]))
        java(rf"\b{naam}_YAW = {num}f", naam + "_YAW", (yaw,))
    java(rf"BOOT_DEK = {num},", "BOOT_DEK", (modellen.DEK / 16.0,))
    java(rf"WATER = {num};", "WATER", (dz.WATER_LEVEL - 1 + 0.875 - (bouw.DEK_Y + 1),))
    # the boat floats: its keel as deep under the surface as the model says
    diepgang = (dz.WATER_LEVEL - 1 + 0.875) - (bouw.DEK_Y - G + bouw.PLEK_BOOT[0][1])
    if abs(diepgang - modellen.DIEPGANG / 16.0) > 0.02:
        problems.append(f"the boat's keel lies {diepgang:.3f} under the surface, the model wants {modellen.DIEPGANG / 16.0:.3f}")
    kust = open(os.path.join(JAVA, "KustStructure.java"), encoding="utf-8").read()
    if f"LOOP = {LOOP};" not in kust or f"RAND = {RAND};" not in kust:
        problems.append("KustStructure.java LOOP / RAND differ from snuffel_steiger")
    scenes = open(os.path.join(JAVA, "SteigerScenes.java"), encoding="utf-8").read()
    m = re.search(r"ZEE_Z = (\d+);", scenes)
    if not m or int(m.group(1)) - bouw.ANKER[2] + 14 > max(OPEN_ZEE):
        problems.append("the storm of the crossing plays further out than OPEN_ZEE checks")
    for key in sorted(java_sleutels() | set(LANG) | {f"subtitles.guhs.{e}" for e in SOUNDS}):
        if key not in h.NL:
            problems.append(f"missing lang {key}")
    for p in [f"{h.D}/{f}" for f in (f"structure/{STRUCTUUR}.nbt", "structure/snuffelsteiger_test_vloer.nbt", f"worldgen/structure/{STRUCTUUR}.json",
                                     f"worldgen/structure_set/{STRUCTUUR}.json", f"worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json",
                                     f"worldgen/template_pool/{STRUCTUUR}/start.json", f"tags/worldgen/biome/has_structure/{STRUCTUUR}.json")]:
        if not os.path.exists(p):
            problems.append(f"missing file {p}")
    geluiden = json.load(open(f"{h.A}/sounds.json", encoding="utf-8"))
    for e in SOUNDS:
        if e not in geluiden:
            problems.append(f"sounds.json misses {e}")
    if problems:
        raise SystemExit("snuffel_steiger self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    structuur(h)
    modellen.build(h)
    sounds(h)
    texts(h)
    selfcheck(h)
