"""
Op Safari! (1.6.0), slice C: the safari animals' looks (GeckoLib models, animations, textures) and their species data.

The models come from the phase-0 animal studio (safari-ontwerp/dieren/_bron, approved by the user), moved into
tools/features/safari_atelier/ unchanged except for paths: dieren.py = batch 1 (stijlproef), batch2..9.py = the rest.
Every studio module writes per soort into a staging folder (build/safari_atelier/<soort>/); this script copies what the
game needs into the assets:

    assets/guhs/geckolib/models/entity/safari/<soort>[_man|_baby].geo.json
    assets/guhs/geckolib/animations/entity/safari/<soort>.animation.json
    assets/guhs/textures/entity/safari/<soort>[_man|_baby][_glowmask].png

and writes data/guhs/safari/soorten.json (SAFARI_CONTRACT.md): zone, hitbox (measured from the geo), baby scale, own male
model, behaviour flags, the animations that exist, speed, glow, verblijfsbord price class.

    python tools/features/safari_dieren.py [soort ...] [--vellen] [--check]
        no soort: all 51; --vellen: also the proposal sheets (build/safari_atelier/); --check: only the self-check
build(h) does the same for make_resources (all soorten, no sheets).
"""
import json
import os
import shutil
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
ATELIER = os.path.join(HIER, "safari_atelier")
STAGING = os.path.join(ROOT, "build", "safari_atelier")
STAGING_SLAAP = os.path.join(ROOT, "build", "safari_atelier_slaap")   # round 3: the same models with closed eyes
RES = os.path.join(ROOT, "src", "main", "resources")
GEO_DIR = os.path.join(RES, "assets", "guhs", "geckolib", "models", "entity", "safari")
ANIM_DIR = os.path.join(RES, "assets", "guhs", "geckolib", "animations", "entity", "safari")
TEX_DIR = os.path.join(RES, "assets", "guhs", "textures", "entity", "safari")
SOORTEN_JSON = os.path.join(RES, "data", "guhs", "safari", "soorten.json")
BEWONERS_JSON = os.path.join(RES, "data", "guhs", "safari", "bewoners.json")

# the studio module that makes each soort (batch 1 = dieren.py)
MODULES = ["dieren", "batch2", "batch3", "batch4", "batch5", "batch6", "batch7", "batch8", "batch9"]

# ---------------------------------------------------------------------------------------------------------------------
# species data (design 1.6 soortenblad + ronde 38-39). gedrag flags:
#   kudde        moves as a group           verstopper   hides (hole/bush/water/tree) when approached fast
#   territorium  small hard-to-find home    boom         rests/climbs in trees     water   lives in/near water
#   zwemmer      water-only (no legs)       vlieg        can fly                   klim    climbs walls
#   hang         hangs (from a branch/thread) nacht      more active at night (night safari)
#   aaihoek      petting-corner animal      geheim       not in the park (dex reward)   zeldzaam   very rare sighting
#   rijdier / bezorger / bouwdier           (step-2 roles, kept here so the data is complete)
#   wadend       a land animal that may wade into 1-deep water at the edge (never deeper)
# idle_mix = {animation: share}: how much of its standing time a species spends in that animation instead of idle
# snelheid = movement speed attribute (vanilla scale: 0.25 = cow-ish)
# ---------------------------------------------------------------------------------------------------------------------
# S6 (step 2): which call group (sounds/safari/dier/<groep>1-3, event guhs:safari.dier.<groep>) every species uses
GELUID = {
    "leeuw": "grote_kat", "luipaard": "grote_kat", "jachtluipaard": "grote_kat", "sabeltandtijger": "grote_kat",
    "olifant": "olifant", "mammoet": "olifant",
    "neushoorn": "grazer", "kafferbuffel": "grazer", "zebra": "grazer", "giraffe": "grazer", "okapi": "grazer", "edelhert": "grazer",
    "wolharige_neushoorn": "grazer", "kangoeroe": "grazer", "dikdik": "klein", "gouden_dikdik": "klein",
    "stokstaartje": "klein", "capibara": "klein", "bever": "klein", "das": "klein", "egel": "klein", "wombat": "klein",
    "quokka": "klein", "otter": "klein", "rode_panda": "klein",
    "toekan": "vogel", "specht": "vogel", "prieelvogel": "vogel",
    "flamingo": "groot_vogel", "struisvogel": "groot_vogel", "ooievaar": "groot_vogel",
    "pinguin": "pinguin",
    "zeekoe": "zee", "zeehond": "zee", "narwal": "zee", "octopus": "zee",
    "ijsbeer": "beer", "reuzenpanda": "beer", "koala": "beer",
    "orang_oetan": "aap", "gibbon": "aap", "luiaard": "aap",
    "krokodil": "reptiel", "gekko": "reptiel", "vuursalamander": "reptiel", "glimworm": "reptiel",
    "nijlpaard": "nijlpaard",
    "geitje": "geit", "alpaca": "geit", "ezeltje": "ezel", "cavia": "cavia",
}

DATA = {
    # savanne
    "leeuw":          dict(zone="savanne", gedrag=[], snelheid=0.22, bord="middel"),
    "luipaard":       dict(zone="savanne", gedrag=["verstopper", "boom"], snelheid=0.24, bord="middel"),
    "olifant":        dict(zone="savanne", gedrag=["kudde", "wadend"], snelheid=0.18, bord="middel"),
    "neushoorn":      dict(zone="savanne", gedrag=[], snelheid=0.2, bord="middel"),
    "kafferbuffel":   dict(zone="savanne", gedrag=["kudde"], snelheid=0.2, bord="gewoon"),
    "dikdik":         dict(zone="savanne", gedrag=["verstopper"], snelheid=0.28, bord="middel"),
    "giraffe":        dict(zone="savanne", gedrag=["kudde"], snelheid=0.2, bord="middel"),
    "zebra":          dict(zone="savanne", gedrag=["kudde"], snelheid=0.24, bord="gewoon"),
    "stokstaartje":   dict(zone="savanne", gedrag=["kudde", "verstopper"], snelheid=0.26, bord="gewoon",
                           idle_mix={"wacht": 0.7}),      # test round 2: mostly upright on guard, looking around
    "jachtluipaard":  dict(zone="savanne", gedrag=[], snelheid=0.3, bord="middel"),
    "struisvogel":    dict(zone="savanne", gedrag=["kudde", "rijdier"], snelheid=0.3, bord="middel"),
    # rivierdelta
    "zeekoe":         dict(zone="delta", gedrag=["water", "zwemmer"], snelheid=0.12, bord="middel"),
    "nijlpaard":      dict(zone="delta", gedrag=["verstopper", "water"], snelheid=0.17, bord="middel"),
    "flamingo":       dict(zone="delta", gedrag=["kudde", "water"], snelheid=0.2, bord="gewoon"),
    "capibara":       dict(zone="delta", gedrag=["water"], snelheid=0.18, bord="gewoon"),
    "krokodil":       dict(zone="delta", gedrag=["verstopper", "water"], snelheid=0.15, bord="middel"),
    # bamboebergen
    "rode_panda":     dict(zone="bamboe", gedrag=["verstopper", "boom"], snelheid=0.2, bord="middel"),
    "reuzenpanda":    dict(zone="bamboe", gedrag=[], snelheid=0.15, bord="middel"),
    "gibbon":         dict(zone="bamboe", gedrag=["boom", "hang", "bouwdier"], snelheid=0.22, bord="middel"),
    # regenwoud
    "luiaard":        dict(zone="regenwoud", gedrag=["verstopper", "boom", "hang"], snelheid=0.06, bord="middel"),
    "okapi":          dict(zone="regenwoud", gedrag=["verstopper", "territorium"], snelheid=0.2, bord="zeldzaam"),
    "orang_oetan":    dict(zone="regenwoud", gedrag=["boom"], snelheid=0.18, bord="middel"),
    "toekan":         dict(zone="regenwoud", gedrag=["vlieg", "boom"], snelheid=0.22, bord="gewoon"),
    "gekko":          dict(zone="regenwoud", gedrag=["klim", "bouwdier"], snelheid=0.22, bord="gewoon"),
    "glimworm":       dict(zone="regenwoud", gedrag=["hang", "nacht", "bouwdier"], snelheid=0.08, bord="gewoon"),
    # ijskust
    "pinguin":        dict(zone="ijskust", gedrag=["kudde", "water"], snelheid=0.16, bord="gewoon"),
    "ijsbeer":        dict(zone="ijskust", gedrag=["water"], snelheid=0.2, bord="middel"),
    "zeehond":        dict(zone="ijskust", gedrag=["verstopper", "water", "zwemmer"], snelheid=0.14, bord="gewoon"),
    "narwal":         dict(zone="ijskust", gedrag=["water", "zwemmer", "zeldzaam"], snelheid=0.16, bord="zeldzaam"),
    "octopus":        dict(zone="ijskust", gedrag=["water", "zwemmer", "bouwdier"], snelheid=0.12, bord="middel"),
    # oerijstijd
    "sabeltandtijger": dict(zone="oertijd", gedrag=["verstopper", "territorium"], snelheid=0.24, bord="zeldzaam"),
    "mammoet":        dict(zone="oertijd", gedrag=["kudde"], snelheid=0.17, bord="middel"),
    "wolharige_neushoorn": dict(zone="oertijd", gedrag=[], snelheid=0.18, bord="middel"),
    # europees woud
    "bever":          dict(zone="europa", gedrag=["verstopper", "territorium", "water"], snelheid=0.17, bord="zeldzaam"),
    "vuursalamander": dict(zone="europa", gedrag=["verstopper", "territorium", "nacht"], snelheid=0.1, bord="zeldzaam"),
    "egel":           dict(zone="europa", gedrag=["verstopper", "nacht"], snelheid=0.16, bord="gewoon"),
    "otter":          dict(zone="europa", gedrag=["water"], snelheid=0.22, bord="gewoon"),
    "das":            dict(zone="europa", gedrag=["verstopper", "territorium", "nacht"], snelheid=0.18, bord="zeldzaam"),
    "edelhert":       dict(zone="europa", gedrag=["kudde"], snelheid=0.24, bord="gewoon"),
    "ooievaar":       dict(zone="europa", gedrag=["vlieg", "water", "bezorger"], snelheid=0.2, bord="middel"),
    "specht":         dict(zone="europa", gedrag=["boom", "klim", "bouwdier"], snelheid=0.18, bord="middel"),
    # australie
    "koala":          dict(zone="australie", gedrag=["verstopper", "boom"], snelheid=0.1, bord="middel"),
    "kangoeroe":      dict(zone="australie", gedrag=["kudde"], snelheid=0.28, bord="gewoon"),
    "wombat":         dict(zone="australie", gedrag=["verstopper"], snelheid=0.16, bord="middel"),
    "quokka":         dict(zone="australie", gedrag=["verstopper", "territorium"], snelheid=0.2, bord="zeldzaam"),
    "prieelvogel":    dict(zone="australie", gedrag=["vlieg", "bouwdier"], snelheid=0.2, bord="middel"),
    # not in the park: the dex reward
    "gouden_dikdik":  dict(zone=None, gedrag=["geheim"], snelheid=0.28, bord=None),
    # aaihoek (entree)
    "geitje":         dict(zone="entree", gedrag=["aaihoek"], snelheid=0.2, bord=None),
    "alpaca":         dict(zone="entree", gedrag=["aaihoek"], snelheid=0.18, bord=None),
    "cavia":          dict(zone="entree", gedrag=["aaihoek"], snelheid=0.16, bord=None),
    "ezeltje":        dict(zone="entree", gedrag=["aaihoek"], snelheid=0.18, bord=None),
}


# ---------------------------------------------------------------------------------------------------------------------
# round 3 (lively animals, design ch. 5 / ronde 40): what an animal does in its "gedrag" activity (SafariDier):
#   rust        lie down for a while (eyes open)        spuit      trunk up + a water shower
#   kudde_ren   the herd runs together to another spot  spring     a few hops on the way (kangaroo)
#   buikglij    slide on its belly over snow/ice         glijbaan   the slide loop (points "glijbaan" in kijkplekken.json)
#   zwem        into the water and around               draag      carry a branch to the dam (points "beverdam")
#   burcht      into the lodge for a while ("beverburcht")   tunnel   along the badger tunnel ("dassentunnel")
#   drink       to the water's edge and drink
#   anim:<a>    play its own animation <a> standing still
# Every species gets "rust" + "anim:<a>" for its own extra animations (wacht, gaap, slinger, tik, ...); ACTIES adds the
# special ones. Written to soorten.json as "acties".
# ---------------------------------------------------------------------------------------------------------------------
ACTIES = {
    "olifant": ["spuit", "drink", "kudde_ren"], "mammoet": ["spuit", "kudde_ren"],
    "zebra": ["kudde_ren", "drink"], "kafferbuffel": ["kudde_ren", "drink"], "giraffe": ["kudde_ren", "drink"],
    "edelhert": ["kudde_ren", "drink"], "struisvogel": ["kudde_ren"], "wolharige_neushoorn": ["drink"],
    "kangoeroe": ["spring", "drink"], "quokka": ["spring"], "wombat": [],
    "pinguin": ["buikglij", "glijbaan", "zwem"],
    "bever": ["zwem", "draag", "burcht"], "das": ["tunnel"], "otter": ["zwem"],
    "nijlpaard": ["zwem"], "krokodil": ["zwem"], "capibara": ["zwem", "drink"], "flamingo": ["zwem"],
    "ijsbeer": ["zwem"], "zeehond": ["zwem"], "leeuw": ["drink"], "neushoorn": ["drink"], "jachtluipaard": ["drink"],
}
# own animations that are no behaviour of their own (the base set, poses the AI sets itself, and the generated ones)
GEEN_ACTIE_ANIM = {"idle", "walk", "blij", "eet", "hang", "slaap", "rust", "ren", "buikglij", "spuit", "draag", "zit"}

# "bekende bewoners" (ronde 46): one named park animal per entry; the game names the first grown-up of the first home of
# that species in that zone. id = soort + zone (stable).
BEWONERS = [
    ("olifant", "savanne", "Flappie"), ("leeuw", "savanne", "Manenmans"), ("giraffe", "savanne", "Langnek Lotte"),
    ("nijlpaard", "delta", "Gapende Gijs"), ("flamingo", "delta", "Roze Rosa"),
    ("luiaard", "regenwoud", "Slome Sjaak"), ("toekan", "regenwoud", "Snavel Saar"),
    ("reuzenpanda", "bamboe", "Bamboe Bas"), ("rode_panda", "bamboe", "Pluimpje"),
    ("pinguin", "ijskust", "Flapje"), ("ijsbeer", "ijskust", "Sneeuwvlok"),
    ("mammoet", "oertijd", "Wolbol"),
    ("bever", "europa", "Knaagje"), ("otter", "europa", "Otto"),
    ("koala", "australie", "Slaapkop Kees"), ("kangoeroe", "australie", "Hupsje"),
]


def schrijf_bewoners():
    uit = [{"id": f"{s}_{z}", "soort": s, "zone": z, "naam": n} for s, z, n in BEWONERS]
    os.makedirs(os.path.dirname(BEWONERS_JSON), exist_ok=True)
    with open(BEWONERS_JSON, "w", encoding="utf-8", newline="\n") as f:
        json.dump(uit, f, indent=1, ensure_ascii=False)
        f.write("\n")


def _modules():
    sys.path.insert(0, ATELIER)
    import importlib
    mods = []
    for name in MODULES:
        m = importlib.import_module(name)
        m.UIT = STAGING
        mods.append(m)
    import blad
    blad.UIT = STAGING
    return mods


def soorten_per_module(mods):
    uit = {}
    for m in mods:
        for s in m.SOORTEN:
            uit[s] = m
    return uit


def maak(soorten=None, vellen=False):
    mods = _modules()
    per = soorten_per_module(mods)
    missend = sorted(set(DATA) - set(per))
    if missend:
        raise SystemExit(f"safari_dieren: geen studio-module voor {missend}")
    soorten = soorten or list(DATA)
    os.makedirs(STAGING, exist_ok=True)
    cwd = os.getcwd()
    for s in soorten:
        per[s].maak(s)
        os.chdir(cwd)                       # some studio code chdirs (blad -> repo root)
    # round 3: the same studio run once more with closed eyes, into its own folder (only its textures are used)
    import atelier
    atelier.SLAAP = True
    try:
        for m in mods:
            m.UIT = STAGING_SLAAP
        os.makedirs(STAGING_SLAAP, exist_ok=True)
        for s in soorten:
            per[s].maak(s)
            os.chdir(cwd)
    finally:
        atelier.SLAAP = False
        for m in mods:
            m.UIT = STAGING
    kopieer(soorten)
    extra_animaties(soorten)
    if vellen:
        for m in mods:
            mine = [s for s in m.SOORTEN if s in soorten]
            if mine and hasattr(m, "vellen"):
                try:
                    m.vellen(mine, overzicht=False)
                except TypeError:
                    m.vellen(mine)
            os.chdir(cwd)
    schrijf_soorten()
    schrijf_bewoners()


def extra_animaties(soorten):
    """Round 3: slaap/rust and the behaviour animations (safari_atelier/gedrag_anims.py) into the copied animation files."""
    import gedrag_anims
    for s in soorten:
        pad = os.path.join(ANIM_DIR, f"{s}.animation.json")
        geo = os.path.join(GEO_DIR, f"{s}.geo.json")
        if not (os.path.exists(pad) and os.path.exists(geo)):
            continue
        anims = json.load(open(pad, encoding="utf-8"))
        if gedrag_anims.voeg_toe(s, json.load(open(geo, encoding="utf-8")), anims):
            with open(pad, "w", encoding="utf-8") as f:
                json.dump(anims, f, indent=1)


def kopieer(soorten):
    for d in (GEO_DIR, ANIM_DIR, TEX_DIR):
        os.makedirs(d, exist_ok=True)
    for s in soorten:
        bron = os.path.join(STAGING, s)
        for f in os.listdir(bron):
            if f.endswith(".geo.json"):
                shutil.copyfile(os.path.join(bron, f), os.path.join(GEO_DIR, f))
            elif f.endswith(".animation.json"):
                shutil.copyfile(os.path.join(bron, f), os.path.join(ANIM_DIR, f))
            elif f.endswith(".png") and not f.endswith("_voorstel.png") and not f.startswith("batch"):
                # GeckoLib 5's AutoGlowingGeoLayer looks for <texture>_glowmask.png (RenderUtil.getEmissiveResource); the
                # studio writes <texture>_glow.png, which made the glimworm draw the missing texture
                doel = f[:-len("_glow.png")] + "_glowmask.png" if f.endswith("_glow.png") else f
                shutil.copyfile(os.path.join(bron, f), os.path.join(TEX_DIR, doel))
                oud = os.path.join(TEX_DIR, f)
                if doel != f and os.path.exists(oud):
                    os.remove(oud)
        # round 3: the closed-eye textures (<naam>_slaap.png) of every model of this soort
        slaap = os.path.join(STAGING_SLAAP, s)
        if os.path.isdir(slaap):
            for f in os.listdir(slaap):
                if f.endswith(".geo.json"):
                    naam = f[:-len(".geo.json")]
                    png = os.path.join(slaap, naam + ".png")
                    if os.path.exists(png):
                        shutil.copyfile(png, os.path.join(TEX_DIR, naam + "_slaap.png"))


def _geo_maat(pad):
    """(breedte, hoogte) in blocks of the cubes' bounds (model pixels / 16)."""
    g = json.load(open(pad, encoding="utf-8"))["minecraft:geometry"][0]
    lo = [1e9, 1e9, 1e9]
    hi = [-1e9, -1e9, -1e9]
    for b in g.get("bones", []):
        for c in b.get("cubes", []):
            o, sz = c["origin"], c["size"]
            for i in range(3):
                lo[i] = min(lo[i], o[i]); hi[i] = max(hi[i], o[i] + sz[i])
    if lo[0] > hi[0]:
        return 0.5, 0.5
    w = max(hi[0] - lo[0], hi[2] - lo[2]) / 16.0
    h = (hi[1] - max(lo[1], 0)) / 16.0
    return round(min(max(w * 0.8, 0.3), 4.0), 2), round(min(max(h, 0.25), 6.0), 2)


def schrijf_soorten():
    uit = {}
    for s, d in DATA.items():
        geo = os.path.join(GEO_DIR, f"{s}.geo.json")
        if not os.path.exists(geo):
            continue
        b, h = _geo_maat(geo)
        baby_geo = os.path.join(GEO_DIR, f"{s}_baby.geo.json")
        if os.path.exists(baby_geo):
            bb, bh = _geo_maat(baby_geo)
            baby_schaal = 1.0                 # own baby model: already small, render 1:1
        else:
            bb, bh = b * 0.55, h * 0.55
            baby_schaal = 0.55                 # no own model: the adult scaled down
        anims = json.load(open(os.path.join(ANIM_DIR, f"{s}.animation.json"), encoding="utf-8"))["animations"]
        namen = sorted(k.split(".")[-1] for k in anims)
        uit[s] = {
            "zone": d["zone"],
            "breedte": b, "hoogte": h,
            "baby_breedte": round(bb, 2), "baby_hoogte": round(bh, 2), "baby_schaal": baby_schaal,
            "man": os.path.exists(os.path.join(GEO_DIR, f"{s}_man.geo.json")),
            "baby_model": os.path.exists(baby_geo),
            "gedrag": d["gedrag"],
            "animaties": namen,
            "snelheid": d["snelheid"],
            "glow": os.path.exists(os.path.join(TEX_DIR, f"{s}_glowmask.png")),
            "bord": d["bord"],
            "geluid": "safari.dier." + GELUID[s],
        }
        if d.get("idle_mix"):
            uit[s]["idle_mix"] = d["idle_mix"]
        acties = ["rust"] + [a for a in ACTIES.get(s, []) if a != "rust"]
        acties += [f"anim:{a}" for a in namen if a not in GEEN_ACTIE_ANIM]
        if "aaihoek" in d["gedrag"] or "geheim" in d["gedrag"]:
            acties = ["rust"] + [f"anim:{a}" for a in namen if a not in GEEN_ACTIE_ANIM]
        uit[s]["acties"] = acties
    os.makedirs(os.path.dirname(SOORTEN_JSON), exist_ok=True)
    with open(SOORTEN_JSON, "w", encoding="utf-8", newline="\n") as f:
        json.dump(uit, f, indent=1, ensure_ascii=False)
        f.write("\n")


def check():
    problemen = []
    soorten = json.load(open(SOORTEN_JSON, encoding="utf-8"))
    for s in DATA:
        if s not in soorten:
            problemen.append(f"{s}: niet in soorten.json"); continue
        for pad in (os.path.join(GEO_DIR, f"{s}.geo.json"), os.path.join(ANIM_DIR, f"{s}.animation.json"),
                    os.path.join(TEX_DIR, f"{s}.png")):
            if not os.path.exists(pad):
                problemen.append(f"{s}: mist {os.path.relpath(pad, RES)}")
        for v in ("man", "baby"):
            g = os.path.join(GEO_DIR, f"{s}_{v}.geo.json")
            if os.path.exists(g) and not os.path.exists(os.path.join(TEX_DIR, f"{s}_{v}.png")):
                problemen.append(f"{s}: {v}-model zonder {s}_{v}.png")
        pad = os.path.join(ANIM_DIR, f"{s}.animation.json")
        if os.path.exists(pad):
            anims = json.load(open(pad, encoding="utf-8"))["animations"]
            namen = {k.split(".")[-1] for k in anims}
            for a in soorten[s]["animaties"]:
                if a not in namen:
                    problemen.append(f"{s}: animatie {a} niet in {s}.animation.json")
            for a in ("idle", "walk"):
                if a not in namen:
                    problemen.append(f"{s}: basisanimatie {a} ontbreekt")
    if problemen:
        raise SystemExit("safari_dieren check failed:\n  " + "\n  ".join(problemen))
    print(f"safari_dieren check: {len(soorten)} soorten ok")


def build(h):
    maak()
    check()


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if "--check" not in sys.argv:
        maak(args or None, vellen="--vellen" in sys.argv)
    check()
