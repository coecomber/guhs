"""
De Grote Guhspelen (2.9) - phase 1 ("fundament"): the resources of the shared framework (feature/spelen + the data part of
feature/kleding), and the helpers the 2.9 stub generators use until each slice replaces its stub.

  build(h)           lang (Dutch in both files): the difficulty levels (Niveau), the eras and groups of the Guhdex's Minigames
                     tab (SpelGroepen: names + where to find them), the Highscores rows of every 2.9 board, the clothing
                     sources (KledingBronnen), the OREN wardrobe slot; the advancement tab "De Grote Guhspelen" (its root)
  oren(...)          (helper) guh model bones for an OREN piece: one bone per ear, parented to the ear bones
  stub_npc(...)      (helper) placeholders of a 2.9 guh character: name, Guhdex texts, recoloured npc_<kind>.png, quest/seen_<kind>
  stub_structuur(...)(helper) a small placeholder structure with its NPC (make_v2 h.structure, centre jigsaw guhs:<name>_midden)
"""
from features import knuffeldal_stub as _stub

# --- the difficulty levels ------------------------------------------------------------------------------------------------
NIVEAUS = {"makkelijk": "Makkelijk", "medium": "Medium", "lastig": "Lastig"}

# --- the groups of the Minigames tab (same ids and order as SpelenFeature.registerGroepen) ------------------------------------
TIJDPERKEN = {"klassiekers": "Klassiekers", "knuffeldal": "Knuffeldal", "grote_guhspelen": "De Grote Guhspelen", "verhalen": "Verhalen"}
LAND = "de Guhvelden, Guhweides, Roze pluisjes, Knabbelkruimels of Kaasvlakte van de Guhmensie"
GROEPEN = {
    # klassiekers (2.4)
    "beauty": ("Guh Beauty Theater", f"Op {LAND}: het Guh Beauty Theater, bij de Showguh. Vads op de catwalk!"),
    "race": ("De guhracebaan", f"Op {LAND}: de guhracebaan, bij de Raceguh in de pitstraat."),
    "meppen": ("De Mika-mephal", f"Op {LAND}: de Mika-mephal, bij de Mepguh. Mep die knabbeldieven!"),
    "disco": ("De Guhdisco", f"Op {LAND}: de Guhdisco met de lichtjesvloer, bij de DJ-guh."),
    "golf": ("De guhgolfbaan", f"Op {LAND}: de guhgolfbaan, bij de Golfguh."),
    "smul": ("Het Vadsig eetfestijn", f"Op {LAND}: het Vadsig eetfestijn, bij de Smulguh. Word lekker vahoeg!"),
    "vissen": ("De guhvisvijver", f"Op {LAND}: de guhvisvijver, bij de Visguh op de steiger."),
    "verstop": ("Het Verstopguhhuis", f"Op {LAND}: het Verstopguhhuis, bij Verstopguhtje op het dak."),
    "kermis": ("De guhkermis", f"Op {LAND}: de guhkermis, bij de Kermis-guh."),
    # Knuffeldal (2.8)
    "bakkerij": ("De Knabbelbakkerij", "In het Knuffeldal-stadje (biome Knuffeldal): de Knabbelbakkerij, bij Bakker Korstje achter de toonbank."),
    "creche": ("De Knuffelcreche", "In het Knuffeldal-stadje (biome Knuffeldal): de Knuffelcreche, bij Juf Knuffel."),
    "kapper": ("Knip & Vads", "In het Knuffeldal-stadje (biome Knuffeldal): de kapsalon Knip & Vads, bij Kapper Krulletje."),
    "knuffelbad": ("Het Knuffelbad", "In de Guhzee: het Knuffelbad met de grote glijbanen, bij Badmeester Bubbel."),
    "grijpmachine": ("De grijpmachine", "In het Knuffeldal-stadje (biome Knuffeldal): de grijpmachine op het plein. Grijp een knuffel!"),
    # De Grote Guhspelen (2.9)
    "sjoelen": ("Het Sjoelhuisje", "In de Guhweides: het Sjoelhuisje met de reuzensjoelbak, bij Opoe Njegschuif."),
    "doolhof": ("Het Guhdoolhof", "In de Guhvelden: het grote heggendoolhof, bij Meneer Vadskronkel aan de ingang."),
    "katapult": ("De Knabbelkatapult", "Op de Vadskliffen: het guhkasteel met de katapult, bij Kapitein Floepguh."),
    "knabbelspelen": ("De Knabbelspelen", "In de Guhweides en de Roze pluisjes: de grote circustent, bij Juf Vahoegsakee."),
    "elftocht": ("De Elf-Guhjestocht", "In de ijskoude Guhpolder: start en finish in Guhwarden, bij Schaatsmeester Guhglij."),
    "circuit": ("Het Guh-Circuit", "In de Guhvelden en de Kaasvlakte: het Guh-Circuit, bij Coach Vahoegvroem in de pitstraat."),
    # Guhverhalen (3.0)
    "sledesprint": ("De Nomguh-sledesprint", "In de witte Sneeuwguhtoendra: Nomguh, aan de startstreep bij Steele-Mika. Wie is de snelste sledeguh?"),
    "guhwaii_spellen": ("Surfen & hula", "Op de eilandjes van Guhwai'i in de Diepe Guhzee: het surfstrand, bij Lilo-guh. Aloha, njeg!"),
}

# --- the Highscores rows of 2.9 (Highscores.GAMES; the older rows have their names in make_v2 / knuffeldal_tekst) --------------
KLASSIEK = {"beauty": "Vads-wedstrijd", "race": "Guhrace", "meppen": "Mika meppen", "golf": "Guhgolf", "smul": "Vadsig eetfestijn",
            "vissen": "Guhvis-wedstrijd", "vissen_zwaarste": "Zwaarste vis"}
BANEN = {"regenboog": "Regenboogbaan", "vads": "Vadsbaan", "kaasberg": "Kaasbergbaan"}
SPELEN = {"knabbelhappen": "Knabbelhappen", "zaklopen": "Zaklopen", "blikgooien": "Mika-blikgooien", "eierlopen": "Eierlopen met knabbelei",
          "spijkerpoepen": "Spijkerpoepen", "guhguhtje_prik": "Guhguhtje prik", "zeskamp": "De Grote Zeskamp"}


def highscore_rijen():
    """{row id: Dutch name} of every 2.9 Highscores row (CONTRACT_29 §5.3)."""
    rows = {}
    for base, name in KLASSIEK.items():
        for lvl in ("makkelijk", "lastig"):
            rows[f"{base}_{lvl}"] = f"{name}: {lvl}"
    for lvl in ("makkelijk", "lastig"):
        rows[f"race_lap_{lvl}"] = f"Guhrace {lvl}: snelste ronde"
    rows["disco_makkelijk"] = "Guhdisco: Vadsige Tango"
    rows["disco_lastig"] = "Guhdisco: Mika-Mambo"
    rows["disco_boogie"] = "Guhdisco: Njeg-Njeg Boogie"
    rows["sjoelen"] = "Guh-sjoelen"
    for lvl in NIVEAUS:
        rows[f"doolhof_{lvl}"] = f"Guhdoolhof: {lvl}"
        rows[f"katapult_{lvl}"] = f"Knabbelkatapult: {lvl}"
    for sid, name in SPELEN.items():
        rows[f"spelen_{sid}"] = name
    rows["elfguhjestocht"] = "Elf-Guhjestocht"
    for baan, name in BANEN.items():
        for lvl in NIVEAUS:
            rows[f"circuit_{baan}_{lvl}"] = f"{name}: {lvl}"
            rows[f"circuit_{baan}_{lvl}_ronde"] = f"{name} {lvl}: snelste ronde"
    return rows


# --- the clothing sources (KledingBronnen; the group ids above are sources too) -------------------------------------------------
BRONNEN = {
    "kleermaker": "De guh-kleermaker", "loot_picknick": "Picknickmanden", "loot_hamsterhuis": "Hamsterhuizen", "loot_guhramid": "De Guhramide",
    "loot_mikahuis": "Mikahuizen", "loot_grotten": "Guhgrotten", "loot_kasteel": "Het guhkasteel", "loot_eilanden": "De zwevende guh-eilandjes",
    "guhdex": "Guhdex-mijlpalen", "brococolief": "De Brococolief-guh", "crafting": "Zelf maken", "vadsparade": "De Vadsparade",
    "kaasmijn": "De kaasmijn", "bibliotheek": "De guhbibliotheek", "onderwater": "De Guhbubbel", "vadswoud": "Het Vadswoud",
    "barbecuether": "De Guhbarbecuether", "knuffeldal": "Knuffeldal", "theehuis": "Het Knabbelthee-huisje", "boerderij": "De Guhboerderij",
    "tuintjes": "De tuintjes", "sterrenwacht": "De Guh-Sterrenwacht", "ballon": "Het Ballonfestival", "kamperen": "Kamperen bij Opa Guh",
    "wereldleven": "Het guhleven", "beroep_brandweer": "De brandweer", "beroep_politie": "De politie", "beroep_apotheek": "De apotheek",
    "beroep_bouw": "Bob de Guhbouwer",
}

# --- the OREN slot: bones on the guh ears (guh.geo.json: ear_left / ear_right, children of head, they wiggle) --------------------
EAR_LEFT_PIVOT, EAR_RIGHT_PIVOT = [7.5, 11, -5.5], [-7.5, 11, -5.5]
EAR_LEFT_CUBES = [([4.5, 11, -6], [7, 5, 1]), ([5.5, 10, -6.25], [5, 7, 1.5])]


def _mirror(cubes):
    """Left-ear cubes -> right-ear cubes (mirrored in x)."""
    return [([-(o[0] + s[0]), o[1], o[2]], list(s), inflate) for o, s, inflate in cubes]


def oren(name, swatch, cubes=None, inflate=0.3):
    """BONES entries (make_guh_variants format) for an OREN piece: <name>_links on ear_left and <name>_rechts on ear_right,
    both named with the piece's bone prefix (use GuhClothes bones "<name>", e.g. "outfit_oren_warmer"). cubes: the cubes of the
    LEFT ear [(origin, size, inflate)] in model space (the right ear gets them mirrored); default: a cover around the ear cubes
    (inflated), like ear warmers. They are parented to the ear bones (not the head) so they wiggle along with the ears."""
    assert name.startswith("outfit_oren_"), name
    left = cubes if cubes is not None else [(o, s, inflate) for o, s in EAR_LEFT_CUBES]
    return {f"{name}_links": ("ear_left", EAR_LEFT_PIVOT, swatch, left),
            f"{name}_rechts": ("ear_right", EAR_RIGHT_PIVOT, swatch, _mirror(left))}


# --- placeholders for the stub generators ---------------------------------------------------------------------------------------
def stub_npc(h, kind, name, rarity, info, hue):
    """A 2.9 guh character's placeholders (the slice replaces them): name, Guhdex texts, npc_<kind>.png, quest/seen_<kind>."""
    _stub.npc(h, kind, name, rarity, info, hue)


def stub_structuur(h, name, biomes, spacing, sep, salt, kind, title, tooltip):
    """A small placeholder structure (13 x 10 x 13, a kiosk with the NPC) so /locate and the Superkompas work (the slice replaces it)."""
    _stub.loose(h, name, biomes, spacing, sep, salt, kind, title, tooltip)


def build(h):
    for nid, name in NIVEAUS.items():
        h.lang(f"gui.guhs.niveau.{nid}", name, name)
    for tid, name in TIJDPERKEN.items():
        h.lang(f"gui.guhs.spelgroep.tijdperk.{tid}", name, name)
    for gid, (name, waar) in GROEPEN.items():
        h.lang(f"gui.guhs.spelgroep.{gid}", name, name)
        h.lang(f"gui.guhs.spelgroep.{gid}.waar", waar, waar)
        h.lang(f"gui.guhs.kledingbron.{gid}", name, name)
    for rid, name in highscore_rijen().items():
        h.lang(f"gui.guhs.highscores.game.{rid}", name, name)
    for bid, name in BRONNEN.items():
        h.lang(f"gui.guhs.kledingbron.{bid}", name, name)
    h.lang("gui.guhs.menu.clothes.oren", "Oren", "Oren")
    h.lang("gui.guhs.spelgroep.bezocht", "Bezocht, vahoeg!", "Bezocht, vahoeg!")
    h.lang("gui.guhs.spelgroep.niet_bezocht", "Nog niet bezocht, njeg...", "Nog niet bezocht, njeg...")
    # the advancement tab "De Grote Guhspelen" (the slices hang their advancements under this root)
    h.w(f"{h.D}/advancement/grote_guhspelen/root.json", {
        "display": {"icon": {"id": "minecraft:oak_pressure_plate"},
                    "title": {"translate": "advancements.guhs.grote_guhspelen.root.title"},
                    "description": {"translate": "advancements.guhs.grote_guhspelen.root.description"},
                    "frame": "task", "show_toast": True, "announce_to_chat": False,
                    "background": "minecraft:textures/block/pink_wool.png"},
        "criteria": {"done": {"trigger": "minecraft:changed_dimension", "conditions": {"to": "guhs:guhmension"}}}})
    title = "De Grote Guhspelen"
    desc = "Sjoelen, doolhoven, katapulten, schaatsen en racen: word de vahoegste guh van de Guhmensie!"
    h.lang("advancements.guhs.grote_guhspelen.root.title", title, title)
    h.lang("advancements.guhs.grote_guhspelen.root.description", desc, desc)
    selfcheck()


def selfcheck():
    """The lang tables match the Java data (SpelenFeature groups, the Highscores rows) - a quick sanity check."""
    rows = highscore_rijen()
    problems = []
    if len(rows) != len(set(rows)):
        problems.append("duplicate Highscores rows")
    expected = 2 * 7 + 2 + 3 + 1 + 3 + 3 + 7 + 1 + 18
    if len(rows) != expected:
        problems.append(f"{len(rows)} Highscores rows, expected {expected}")
    if len(GROEPEN) != 22:
        problems.append(f"{len(GROEPEN)} groups, expected 22")
    b = oren("outfit_oren_test", "test")
    if b["outfit_oren_test_rechts"][3][0][0] != [-11.5, 11, -6]:
        problems.append("the right ear isn't mirrored right: " + str(b["outfit_oren_test_rechts"][3][0]))
    if problems:
        raise SystemExit("spelen self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    pass


BONES = {}
CLOTHES = []
