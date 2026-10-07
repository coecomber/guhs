"""
Kleding (2.9, "De Grote Guhspelen"; nl.juiced.guhs.feature.kleding): clothing becomes a one-time UNLOCK per player. Hold
right-click on a clothing item to use it up (plop, a chime, confetti, "Ontgrendeld: X! VAHOEG!"); from then on all your
tamed guhs can wear it. The new wardrobe (client.screen.GuhWardrobeScreen) picks from your unlocks, with a big 3D preview,
favourite outfits, a dice button and a filter per source. Every piece has exactly one source (KledingBronLijst); the
kleermaker sells a fixed full offer: his everyday set plus the three new ear bows (the OREN slot).

This module makes: the ear-bow bones, textures and icons (OORSTRIKJE_ROZE/MINT/GEEL, the make_guh_variants /
make_clothes_icons hooks), the unlock chime (sound kleding.ontgrendel, synthesised), the confetti particle, the
advancements (tab grote_guhspelen: kleding_*), all texts (Dutch in both languages, plus texts of other features that
talked about the old clothes-as-items), the FTB quests (chapter guhs_basis), and a self-check: every clothing piece has
exactly ONE source, and loot tables only hold the pieces whose source is that loot.
"""
import os
import re

import numpy as np
from PIL import Image

from features import spelen

CLOTHES = ["oorstrikje_roze", "oorstrikje_mint", "oorstrikje_geel"]
FTB_SECTION = "Kleding voor je guh"

# =====================================================================================================================
# the ear bows: a little satin bow on the front of each ear (bones on the ears, so they wiggle along)
# the left ear: x 4.5..11.5, y 10..17, z -6.25..-4.75 (its front at z -6.25)
# =====================================================================================================================
STRIK = [([6.35, 14.55, -7.25], [1.3, 1.5, 1.1], 0.05),     # the knot
         ([4.55, 14.2, -7.0], [1.9, 2.2, 0.8], 0.0),        # the left loop
         ([7.55, 14.2, -7.0], [1.9, 2.2, 0.8], 0.0),        # the right loop
         ([5.75, 12.75, -6.95], [0.75, 1.85, 0.6], 0.0),    # the tails
         ([7.5, 12.75, -6.95], [0.75, 1.85, 0.6], 0.0)]
BONES = spelen.oren("outfit_oren_strik", "oor_strik", cubes=STRIK)

KLEUREN = {"roze": ((255, 146, 196), (214, 84, 150), (255, 214, 234)),
           "mint": ((140, 226, 192), (64, 168, 132), (214, 255, 238)),
           "geel": ((255, 222, 104), (214, 160, 40), (255, 246, 196))}


def _satijn(v, rng, basis, donker, licht):
    """Shiny satin: soft fabric, a light sheen band, a darker seam round the edge, tiny white polka dots."""
    a = v.fabric(basis, rng, 6)
    n = a.shape[0]
    for y in range(n):
        for x in range(n):
            t = (x + y) / (2 * n)
            glans = max(0.0, 1 - abs(t - 0.35) * 7)
            a[y, x] = a[y, x] * (1 - 0.55 * glans) + np.array(licht, np.float32) * 0.55 * glans
    a[:2, :] = donker
    a[-2:, :] = donker
    a[:, :2] = donker
    a[:, -2:] = donker
    for y in range(5, n - 4, 8):
        for x in range(5 + (y // 8 % 2) * 4, n - 4, 8):
            a[y:y + 2, x:x + 2] = (255, 255, 255)
    return np.clip(a, 0, 255)


def clothes(rng, v):
    return {f"oorstrikje_{k}": {"oor_strik": (lambda c=c: _satijn(v, rng, *c))} for k, c in KLEUREN.items()}


STRIK_ICON = ["................", "................", "................", ".aa..........aa.", "abba........abba", "abbba......abbba",
              "abcbba....abbcba", "abccbbaaaabbccba", "abccbbacbabbccba", "abcbbbaaaabbbcba", "abbba.abba.abbba", "abba.abbbba.abba",
              ".aa..abaaba..aa.", "......a..a......", "................", "................"]


def icons(ic):
    return {f"oorstrikje_{k}": ic.icon(STRIK_ICON, {"a": d, "b": b, "c": l}) for k, (b, d, l) in KLEUREN.items()}


# =====================================================================================================================
# the unlock chime: a soft "plop", then a sparkly little guh jingle going up (synthesised, mono ogg)
# =====================================================================================================================
SR = 44100


def _chime():
    t_total = 1.25
    n = int(SR * t_total)
    out = np.zeros(n)
    # the plop: a quick falling sine blip with a round body
    d = int(0.09 * SR)
    t = np.arange(d) / SR
    f = 820 * np.exp(-t * 28) + 240
    out[:d] += np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 38) * 0.9
    # the jingle: C6 E6 G6 C7 E7, little bells (a fundamental and a soft inharmonic partial)
    for i, freq in enumerate((1046.5, 1318.5, 1568.0, 2093.0, 2637.0)):
        start = int((0.1 + i * 0.085) * SR)
        length = n - start
        t = np.arange(length) / SR
        bel = (np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-t * 9)) * np.exp(-t * (5.5 - i * 0.5))
        attack = np.minimum(1, t / 0.004)
        out[start:] += bel * attack * (0.32 - i * 0.03)
    # a shimmer of glitter on top (tiny high sparkles)
    rng = np.random.default_rng(29)
    for _ in range(26):
        s = int(rng.uniform(0.15, 0.95) * SR)
        ln = int(0.03 * SR)
        t = np.arange(ln) / SR
        out[s:s + ln] += np.sin(2 * np.pi * rng.uniform(3500, 6500) * t) * np.exp(-t * 120) * 0.06
    out *= np.minimum(1, (n - np.arange(n)) / (0.12 * SR))    # a soft end
    return out / max(1e-6, np.abs(out).max()) * 0.85


def geluid(h):
    path = os.path.join(h.A, "sounds", "kleding", "ontgrendel.ogg")
    if not os.path.exists(path):             # (made once: vorbis encoding isn't byte-for-byte stable)
        import soundfile as sf
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, _chime().astype(np.float32), SR, format="OGG", subtype="VORBIS")

    def patch(d):
        d["kleding.ontgrendel"] = {"sounds": [{"name": "guhs:kleding/ontgrendel", "volume": 0.8}],
                                   "subtitle": "subtitles.guhs.kleding.ontgrendel"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# the confetti particle: little paper snippers (and a tiny heart) in guh colours
# =====================================================================================================================
CONFETTI = [(255, 140, 190), (140, 226, 192), (255, 222, 104), (190, 160, 255), (140, 200, 255), (255, 170, 110)]


def confetti(h):
    for i, c in enumerate(CONFETTI):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        px = img.load()
        donker = tuple(max(0, v - 50) for v in c)
        shape = [(2, 3), (3, 3), (4, 3), (5, 3), (2, 4), (3, 4), (4, 4), (5, 4)] if i % 2 == 0 else \
            [(3, 2), (4, 2), (3, 3), (4, 3), (3, 4), (4, 4), (3, 5), (4, 5)]
        if i % 3 == 2:   # a tiny heart
            shape = [(2, 2), (3, 2), (5, 2), (6, 2), (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (2, 4), (3, 4), (4, 4), (5, 4),
                     (6, 4), (3, 5), (4, 5), (5, 5), (4, 6)]
        for (x, y) in shape:
            px[min(7, x), y] = c + (255,)
        for (x, y) in shape[-2:]:
            px[min(7, x), y] = donker + (255,)
        h.save(img, "particle", f"kleding_confetti_{i}.png")
    h.w(f"{h.A}/particles/kleding_confetti.json", {"textures": [f"guhs:kleding_confetti_{i}" for i in range(len(CONFETTI))]})


# =====================================================================================================================
# advancements (the Grote Guhspelen tab) and the hidden one for FTB
# =====================================================================================================================
ADVANCEMENTS = [  # name, parent, icon, frame, title, description
    ("kleding_eerste", "root", "guhs:oorstrikje_roze", "task", "Ontgrendeld!",
     "Houd een kledingstuk rechtsklik ingedrukt: nu kunnen al je guhs het aan"),
    ("kleding_oren", "kleding_eerste", "guhs:oorstrikje_mint", "task", "Oren op steeltjes",
     "Ontgrendel iets voor de oren: strikjes, oorbelletjes of oorwarmers"),
    ("kleding_favoriet", "kleding_eerste", "guhs:guh_backpack", "task", "Mijn lievelingsoutfit",
     "Bewaar een favoriete outfit in de kledingkast van je guh (shift-klik op een nummertje)"),
    ("kleding_set", "kleding_eerste", "guhs:oorstrikje_geel", "goal", "Van top tot teen",
     "Ontgrendel alle stukken van een setje (bijvoorbeeld alles van de kleermaker, of het hele sjoelpakje)"),
    ("kleding_veel", "kleding_set", "guhs:striped_sweater", "goal", "Een vadsige kledingkast",
     "Ontgrendel 25 kledingstukken"),
    ("kleding_heel_veel", "kleding_veel", "guhs:koning_kroon", "challenge", "De chicste guh van de Guhmensie",
     "Ontgrendel 60 kledingstukken. VAHOEG!"),
]


def advancements(h):
    h.w(f"{h.D}/advancement/quest/kleding_eerste.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, title, desc in ADVANCEMENTS:
        h.w(f"{h.D}/advancement/grote_guhspelen/{name}.json", {
            "parent": f"guhs:grote_guhspelen/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.grote_guhspelen.{name}.title"},
                        "description": {"translate": f"advancements.guhs.grote_guhspelen.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.title", title, title)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.description", desc, desc)


# =====================================================================================================================
# texts (Dutch in both languages)
# =====================================================================================================================
TEKSTEN = {
    "item.guhs.oorstrikje_roze": "Roze oorstrikjes",
    "item.guhs.oorstrikje_mint": "Mintgroene oorstrikjes",
    "item.guhs.oorstrikje_geel": "Gele oorstrikjes",
    "item.guhs.guh_clothes.ontgrendel": "Houd rechtsklik ingedrukt om te ontgrendelen: dan kunnen al je guhs hem aan!",
    "item.guhs.guh_clothes.al_ontgrendeld": "Al ontgrendeld! Geef hem aan een vriend, njeg.",
    "item.guhs.guh_clothes.bron": "Komt van: %s",
    "subtitles.guhs.kleding.ontgrendel": "Kledingstuk ontgrendeld",
    "gui.guhs.kleding.ontgrendeld": "Ontgrendeld: %s! VAHOEG!",
    "gui.guhs.kleding.al_gehad": "Deze heb je al, njeg! Geef hem aan een vriend.",
    "gui.guhs.kleding.eerste_tip": "Tip: rechtsklik je tamme guh en kies Kledingkast. Al je guhs kunnen je nieuwe stuk nu aan, zo vaak als je wil!",
    "gui.guhs.kleding.aangekleed": "%s is aangekleed. Zo vadsig chique!",
    "gui.guhs.kleding.nee.niet_jouw_guh": "Alleen de baas van deze guh mag hem aankleden, njeg.",
    "gui.guhs.kleding.nee.niet_ontgrendeld": "Dat stuk heb je nog niet ontgrendeld, njeg! Houd het rechtsklik ingedrukt.",
    "gui.guhs.kleding.nee.rugzak_niet_leeg": "Maak de rugzak eerst leeg, njeg! Er zit nog van alles in.",
    "gui.guhs.kleding.nee.verkeerd_vakje": "Dat past daar niet, njeg.",
    "gui.guhs.kleding.onesie": "De Brococolief-guh geeft je zijn reserve-onesie! Houd hem rechtsklik ingedrukt om hem te ontgrendelen.",
    "gui.guhs.kleding.hooibaal.strohoed": "Boerin Hooibaal: \"Wat een harde werker! Hier, mijn oude strohoed. Staat je guh vast schattig!\"",
    "gui.guhs.kleding.hooibaal.overall": "Boerin Hooibaal: \"Jij bent een echte boerenguh! Pak aan: een tuinbroek, voor je vadsigste guh.\"",
    # the wardrobe
    "gui.guhs.kleding.tab.kleding": "Kleding",
    "gui.guhs.kleding.tab.rugzak": "Rugzak & harnas",
    "gui.guhs.kleding.jouw_spullen": "Jouw spullen",
    "gui.guhs.kleding.zoek": "Zoeken...",
    "gui.guhs.kleding.alle_bronnen": "Alle bronnen",
    "gui.guhs.kleding.bron_filter.tooltip": "Laat alleen stukken zien van: %s (klik: volgende, shift-klik: vorige)",
    "gui.guhs.kleding.dobbel": "Dobbel!",
    "gui.guhs.kleding.dobbel.tooltip": "Een willekeurige outfit uit al je ontgrendelde stukken. Bevalt hij? Klik Aantrekken!",
    "gui.guhs.kleding.gedobbeld": "Gedobbeld! Bevalt hij? Klik Aantrekken!",
    "gui.guhs.kleding.terug": "Terug",
    "gui.guhs.kleding.terug.tooltip": "Weer wat je guh nu echt aanheeft",
    "gui.guhs.kleding.aantrekken": "Aantrekken!",
    "gui.guhs.kleding.aantrekken.tooltip": "Trek je guh aan wat hij nu past",
    "gui.guhs.kleding.favorieten": "Favoriet:",
    "gui.guhs.kleding.favoriet": "Favoriet %s: klik om te passen, shift-klik om je outfit hier te bewaren",
    "gui.guhs.kleding.favoriet.leeg": "Favoriet %s is nog leeg. Shift-klik om je outfit hier te bewaren!",
    "gui.guhs.kleding.favoriet.bewaard": "Outfit bewaard als favoriet %s. VAHOEG!",
    "gui.guhs.kleding.draai": "Sleep om te draaien",
    "gui.guhs.kleding.pas_aan": "Aan het passen...",
    "gui.guhs.kleding.niets": "Niets",
    "gui.guhs.kleding.draagt": "draagt hij",
    "gui.guhs.kleding.leeg": "Nog niks ontgrendeld voor dit plekje, njeg! Houd een kledingstuk rechtsklik ingedrukt om het te ontgrendelen.",
    "gui.guhs.kleding.niks_gevonden": "Niks gevonden, njeg. Probeer een ander woord of een andere bron.",
    "gui.guhs.kleding.ontgrendeld_aantal": "Ontgrendeld: %s van %s kledingstukken",
    "gui.guhs.kleding.bron": "Komt van: %s",
    "gui.guhs.kleding.klik_passen": "Klik om te passen",
    "gui.guhs.kleding.geen_rugzak": "Geen rugzak om. Trek je guh de guhrugzak aan (tabblad Kleding) voor 18 extra vakjes!",
    "gui.guhs.kleding.rugzak_vol": "(kan pas af als hij leeg is)",
    "gui.guhs.wardrobe.no_backpack": "Rugzak (geen rugzak om)",
    "gui.guhs.menu.wardrobe": "Kledingkast",
    "gui.guhs.menu.wardrobe.tooltip": "Kies kleertjes uit alles wat je ontgrendeld hebt, met pantser en rugzak erbij",
    # texts of other features that still talked about clothes as things you take off a guh
    "quest.guhs.grillguh.klaar2": "Hier: mijn geheime grillrecept (daarmee maak je zelf Aanmaakblokjes) en een zak gefrituurde kaasknabbels, "
                                  "zodat je guh lekker VAHOEG wordt. Mijn koksmuts? Die verkoop ik in mijn winkeltje!",
    "advancements.guhs.guhmension.koning_pakje.description": "Vind het hele koningspakje in de schatkamer van het guhkasteel",
    "advancements.guhs.guhmension.wolkenpakje.description": "Vind de wolkenmuts en de wolkenkraag in de wolkenkist op het Wolkje",
    "quest.guhs.eilanden.tamed": "De Wolkguh zweeft naar je toe: hij is van jou! Zolang hij dichtbij is, vangt hij je op als je valt. "
                                 "Zijn wolkenmuts en wolkenkraag? Die liggen in de wolkenkist op het Wolkje, bovenaan de wolkentrap.",
    "gui.guhs.guhdex.info.koning": "De Koningguh op zijn troon in het legendarische guhkasteel. Koningspaars, witte manen, een snorretje. "
                                   "Zijn pakje ligt in de schatkamer van zijn kasteel: zoek maar!",
}


def teksten(h):
    for k, v in TEKSTEN.items():
        h.lang(k, v, v)


# =====================================================================================================================
# the self-check: exactly one source per piece, and loot only where the source says loot
# =====================================================================================================================
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs")


def _pieces():
    """{id: (slot, marker block or None)} from GuhClothes.java."""
    src = open(os.path.join(JAVA, "entity", "GuhClothes.java"), encoding="utf-8").read()
    out, marker = {}, None
    for line in src.splitlines():
        m = re.match(r"\s*// <(\w+)>", line)
        if m:
            marker = m.group(1)
            continue
        if re.match(r"\s*// </(\w+)>", line):
            marker = None
            continue
        m = re.match(r"\s+([A-Z][A-Z0-9_]*)\(Slot\.([A-Z]+)", line)
        if m:
            out[m.group(1).lower()] = (m.group(2), marker)
    return out


def _bronnen():
    """{id: [source ids]} from KledingBronLijst.java."""
    src = open(os.path.join(JAVA, "feature", "kleding", "KledingBronLijst.java"), encoding="utf-8").read()
    out = {}
    for bron, piece in re.findall(r'b\("(\w+)", ([A-Z][A-Z0-9_]*),', src):
        out.setdefault(piece.lower(), []).append(bron)
    return out


# 2.9 slices register their own pieces (their marker blocks); the kleding slice registers everything else
ANDERE_29 = {"sjoelen", "doolhof", "katapult", "knabbelspelen", "elftocht", "circuit", "disco29", "samen"}
# 3.0 (Guhverhalen): the story slices register their own pieces too (KledingBronnen.bron in their Feature.register)
ANDERE_30 = {"timmerguh", "balto", "mewtwo", "hemel", "guhwaii"}
# bbq2: the building, ring and guhrio slices register their own pieces too (the marker names of CONTRACT_130 5.4)
ANDERE_BBQ2 = {"paleizen", "bestaand", "camping_markt", "toren_peper", "ring", "guhrio_beloning"}
# loot inside a feature's OWN building counts as that feature (the same logical source; 2.8 sets stay as 2.8 made them)
# guhpixel: every slice registers the sources of its own pieces (marker blocks px_<slice> in GuhClothes)
def _eigen_bron(marker):
    return marker in ANDERE_29 or marker in ANDERE_30 or marker in ANDERE_BBQ2 or (marker or "").startswith("px_")


EIGEN_LOOT = {("boerderij_zakdoek", "guhboerderij.json")}


def selfcheck(h):
    problems = []
    pieces, bronnen = _pieces(), _bronnen()
    for pid, (slot, marker) in pieces.items():
        if slot == "HAAR" or _eigen_bron(marker):
            continue
        n = len(bronnen.get(pid, []))
        if n != 1:
            problems.append(f"{pid}: {n} sources in KledingBronLijst (must be exactly one)")
    for pid in bronnen:
        if pid not in pieces:
            problems.append(f"{pid}: in KledingBronLijst but not in GuhClothes")
    # loot tables: a piece may only be in loot when its source is a loot_* source (the knight set: the caves and the castle)
    loot_dir = os.path.join(h.D, "loot_table")
    for root, _, files in os.walk(loot_dir):
        for f in files:
            if not f.endswith(".json"):
                continue
            txt = open(os.path.join(root, f), encoding="utf-8").read()
            for pid in re.findall(r'"guhs:([a-z0-9_]+)"', txt):
                if pid not in pieces or pieces[pid][0] == "HAAR" or _eigen_bron(pieces[pid][1]):
                    continue
                bron = (bronnen.get(pid) or ["?"])[0]
                if not bron.startswith("loot_") and (pid, f) not in EIGEN_LOOT:
                    problems.append(f"{pid} (source {bron}) is still in loot table {f}")
    for must, table in (("koning_kroon", "guh_kasteel_schat"), ("wolkenmuts", "zwevende_eilanden_wolkenkist"), ("party_hat", "guh_picnic")):
        path = os.path.join(loot_dir, "chests", f"{table}.json")
        if not os.path.exists(path) or f'"guhs:{must}"' not in open(path, encoding="utf-8").read():
            problems.append(f"{must} missing from its loot table {table}")
    if problems:
        raise SystemExit("kleding self-check failed:\n  " + "\n  ".join(problems))
    print(f"kleding: self-check ok ({len(bronnen)} pieces with one source each)")


def build(h):
    geluid(h)
    confetti(h)
    advancements(h)
    teksten(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_basis, section "Kleding voor je guh")
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    # the old "Aankleden!" quest (make_ftbquests.py core) still talked about clothing slots: clothes are unlocks now
    for i, quest in enumerate(fq.QUESTS):
        if quest[0] == "wardrobe":
            fq.QUESTS[i] = (quest[0], "Aankleden!",
                            "Kleding is nu een &dontgrendeling&r: houd een kledingstuk rechtsklik ingedrukt en al je guhs kunnen het aan. "
                            "Rechtsklik je tamme guh en kies &dKledingkast&r: pas alles wat je hebt, draai je guh rond en klik &6Aantrekken!&r. "
                            "Een guh heeft ook een pantservakje en (met de guhrugzak, zelf te maken) 18 extra vakjes.",
                            *quest[3:])
    q("kleding_eerste", "Ontgrendeld!", "Koop een kledingstuk (bij de &dguh-kleermaker&r in een guhdorp, of in een van de minigames) en "
      "&dhoud rechtsklik ingedrukt&r. Plop! Nu kunnen al je tamme guhs het aan, zo vaak als je wil.",
      "guhs:oorstrikje_roze", [fq.adv("kleding_eerste")], rewards=(("guhs:kaas_knabbels", 8),), deps=["tame"], xp=100)
    q("kleding_oren", "Oren op steeltjes", "Guhs hebben nu een plekje voor de &doren&r! De kleermaker verkoopt roze, mint en gele oorstrikjes. "
      "Ontgrendel er een en zet hem op in de kledingkast.",
      "guhs:oorstrikje_mint", [fq.adv("guhs:grote_guhspelen/kleding_oren")], rewards=(("guhs:kaas_knabbels", 12),), deps=["kleding_eerste"], xp=100)
    q("kleding_favoriet", "Mijn lievelingsoutfit", "Maak in de kledingkast een mooie outfit (of klik &eDobbel!&r) en &eshift-klik&r op een "
      "nummertje: zo bewaar je hem als favoriet. Een klik op het nummertje en je guh past hem weer.",
      "guhs:guh_backpack", [fq.adv("guhs:grote_guhspelen/kleding_favoriet")], rewards=(("guhs:gefrituurde_kaasknabbels", 2),), deps=["kleding_eerste"],
      xp=100)
    q("kleding_set", "Van top tot teen", "Ontgrendel alle stukken van een setje: alles van de kleermaker, of het hele pakje van een minigame. "
      "In de Guhdex (tabblad Kleding) zie je per plek wat je nog mist.",
      "guhs:oorstrikje_geel", [fq.adv("guhs:grote_guhspelen/kleding_set")], rewards=(("guhs:kaas_knabbels", 24),), deps=["kleding_eerste"], xp=200,
      shape="hexagon")
    q("kleding_veel", "Een vadsige kledingkast", "Ontgrendel &e25 kledingstukken&r. Elke minigame heeft zijn eigen pakje, en in schatkisten "
      "liggen er nog veel meer!", "guhs:striped_sweater", [fq.adv("guhs:grote_guhspelen/kleding_veel")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 6),), deps=["kleding_set"], xp=300, shape="hexagon")
    q("kleding_heel_veel", "De chicste guh van de Guhmensie", "Ontgrendel &660 kledingstukken&r. Zo veel kleertjes, je guhs weten niet meer wat ze "
      "aan moeten. VAHOEG!", "guhs:koning_kroon", [fq.adv("guhs:grote_guhspelen/kleding_heel_veel")],
      rewards=(("guhs:guh_kristal", 16),), deps=["kleding_veel"], xp=800, shape="gear")
