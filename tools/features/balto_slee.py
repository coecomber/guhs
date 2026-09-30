"""
3.0 (Guhverhalen), slice balto_slee: de sneeuwslee (DESIGN_30 §2; Java: feature/baltoslee).

  - the steered Nomguh sled (entity guhs:baltoslee_slee) for the medicine ride (SleeTocht, started by balto's questline) and the
    sledesprint against Steele-Mika (Steele-Mika with plek "sledesprint", placed by balto at the start line): the route is balto's
    assets/guhs/nomguh/route.json; the storm, gusts, ice bridges, avalanches, vuurkorf rest points and the time limit are ours
  - the guh-sledehondjes (drawn in front of the sleds; entity type guhs:baltoslee_sledehondje, never in the world)
  - your own sneeuwslee (item + entity guhs:sneeuwslee): rides on snow and ice only
  - the coin sledebelletje and Steele-Mika's winter deco (baltoslee_sneeuwguh, _minislee, _sledebellen, _beker, _hondenmand,
    _lantaarnpaal), the sounds baltoslee.* (made by balto_slee_geluid.py, not by make_resources), the particle baltoslee_snuffel
  - advancements verhalen/balto_slee_* (+ hidden quest/balto_slee_*), FTB section "Door de sneeuwstorm", lang (balto_slee_tekst.py),
    game test templates baltoslee_test_*
"""
import os

from features import balto_slee_modellen as modellen
from features import balto_slee_tekst as tekst
from features import balto_slee_tex as tex
from features import verhaal

BONES = {}
CLOTHES = []
FTB_PORTRAIT = "geo:baltoslee_slee:baltoslee_slee"

SOUNDS = {"baltoslee.glijden": ["glijden1", "glijden2"], "baltoslee.bellen": ["bellen1", "bellen2", "bellen3"],
          "baltoslee.woef": ["woef1", "woef2", "woef3"], "baltoslee.windvlaag": ["windvlaag1", "windvlaag2"],
          "baltoslee.lawine": ["lawine"], "baltoslee.plof": ["plof1", "plof2"], "baltoslee.ijs": ["ijs1", "ijs2"],
          "baltoslee.vuurkorf": ["vuurkorf"], "baltoslee.fanfare": ["fanfare"]}
ITEMS = ["sneeuwslee", "sledebelletje"]
QUEST = ["balto_slee_tocht", "balto_slee_rustpunt", "balto_slee_ijsbrug", "balto_slee_lawine", "balto_slee_sprint", "balto_slee_sprint_makkelijk",
         "balto_slee_sprint_medium", "balto_slee_sprint_lastig", "balto_slee_steele", "balto_slee_lastig", "balto_slee_eigen"]


def extra(h):
    for i in ITEMS:
        h.item_model(i)
    h.w(f"{h.A}/particles/baltoslee_snuffel.json", {"textures": [f"guhs:baltoslee_snuffel_{i}" for i in range(3)]})

    def patch(d):
        for k in [k for k in d if k.startswith("baltoslee.")]:
            del d[k]
        for event, files in SOUNDS.items():
            d[event] = {"sounds": [f"guhs:baltoslee/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    # the blocks the guh-sledehondjes run on
    h.w(f"{h.R}/data/guhs/tags/block/baltoslee/sneeuw.json", {"replace": False, "values": [
        "minecraft:snow", "minecraft:snow_block", "minecraft:powder_snow", "#minecraft:ice"]})
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:baltoslee_minislee", "guhs:baltoslee_sledebellen", "guhs:baltoslee_lantaarnpaal",
                                                    "guhs:baltoslee_hondenmand"])
    h.add_tag("minecraft/tags/block/mineable/shovel", ["guhs:baltoslee_sneeuwguh"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:baltoslee_beker"])


def advancements(h):
    for name in QUEST:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    A = tekst.ADVANCEMENTS
    winkel = {f"k{i}": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{b}"}]}}
              for i, b in enumerate(modellen.DECO)}
    tab = [("balto_slee_tocht", "root", "guhs:sneeuwslee", "goal", None),
           ("balto_slee_rustpunt", "balto_slee_tocht", "minecraft:campfire", "task", None),
           ("balto_slee_ijsbrug", "balto_slee_tocht", "minecraft:packed_ice", "task", None),
           ("balto_slee_lawine", "balto_slee_tocht", "minecraft:snow_block", "task", None),
           ("balto_slee_eigen", "balto_slee_tocht", "guhs:sneeuwslee", "goal", None),
           ("balto_slee_sprint", "balto_slee_tocht", "guhs:sledebelletje", "task", None),
           ("balto_slee_steele", "balto_slee_sprint", "guhs:baltoslee_beker", "goal", None),
           ("balto_slee_lastig", "balto_slee_steele", "guhs:baltoslee_beker", "challenge", None),
           ("balto_slee_winkel", "balto_slee_sprint", "guhs:baltoslee_sneeuwguh", "task", winkel)]
    for name, parent, icon, frame, crit in tab:
        title, desc = A[name]
        verhaal.zichtbaar(h, "verhalen", name, parent, icon, frame, title, desc, criteria=crit)
    # (any one of the deco pieces counts)
    h.patch_json(f"{h.D}/advancement/verhalen/balto_slee_winkel.json", lambda d: d.__setitem__("requirements", [list(winkel)]))


def texts(h):
    for key, text in tekst.LANG.items():
        h.lang(key, text, text)


def test_templates(h):
    """The snowy test route (50 x 13: snow blocks, the route along z = 6) and a field half snow, half grass."""
    t = h.Structure((50, 5, 13))
    for x in range(50):
        for z in range(13):
            t.set(x, 0, z, "minecraft:snow_block")
    t.save("baltoslee_test_baan")
    v = h.Structure((24, 5, 14))
    for x in range(24):
        for z in range(14):
            if z < 7:
                v.set(x, 0, z, "minecraft:snow_block")
            else:
                v.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    v.save("baltoslee_test_veld")


def selfcheck(h):
    problems = []
    for key, text in tekst.LANG.items():
        low = text.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {key}")
    for key, (title, desc) in tekst.ADVANCEMENTS.items():
        if "hamster" in (title + desc).lower():
            problems.append(f"lore: {key}")
    A = h.A
    for g in ("baltoslee_slee", "baltoslee_sledehondje"):
        if not os.path.exists(f"{A}/geo/entity/{g}.geo.json"):
            problems.append(f"geo {g}")
    for t in ("baltoslee_slee", "baltoslee_slee_steele", "sneeuwslee", "baltoslee_sledehondje", "baltoslee_sledehondje_steele", "baltoslee_touw"):
        if not os.path.exists(f"{A}/textures/entity/{t}.png"):
            problems.append(f"texture {t}")
    for b in modellen.DECO:
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/block/{b}.json", f"{A}/models/item/{b}.json"):
            if not os.path.exists(p):
                problems.append(p)
        if f"block.guhs.{b}" not in h.NL:
            problems.append(f"lang block.guhs.{b}")
    for i in ITEMS:
        if f"item.guhs.{i}" not in h.NL:
            problems.append(f"lang item.guhs.{i}")
    for files in SOUNDS.values():
        for f in files:
            if not os.path.exists(f"{A}/sounds/baltoslee/{f}.ogg"):
                problems.append(f"sound {f} (run python tools/features/balto_slee_geluid.py)")
    # every bone the Java moves exists
    import json
    slee = json.load(open(f"{A}/geo/entity/baltoslee_slee.geo.json", encoding="utf-8"))
    namen = {b["name"] for b in slee["minecraft:geometry"][0]["bones"]}
    for b in ("kist", "bellen", "lantaarn"):
        if b not in namen:
            problems.append(f"sled bone {b}")
    hond = json.load(open(f"{A}/geo/entity/baltoslee_sledehondje.geo.json", encoding="utf-8"))
    namen = {b["name"] for b in hond["minecraft:geometry"][0]["bones"]}
    for b in ("lijf", "kop", "oor_l", "oor_r", "staart", "tong", "belletje", "been_lv", "been_rv", "been_la", "been_ra"):
        if b not in namen:
            problems.append(f"dog bone {b}")
    if problems:
        raise SystemExit("balto_slee self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    tex.build(h)
    extra(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)
    print("balto_slee: sled, dogs, deco, sounds, advancements ok")


# =====================================================================================================================
# FTB quests: "Door de sneeuwstorm" (chapter guhs_verhalen)
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("balto_slee_tocht", "Door de sneeuwstorm", "Rosy en de babyguhs in Nomguh hebben het medicijn nodig! Stuur de &bslee&r zelf, met "
      "&fBaltoguh&r voorop: door de sneeuwstorm naar de berghut en dan op tijd terug naar het ziekenhuisje. &6W&r = hup hup, "
      "&6S&r = remmen, &6A/D&r = sturen. Zie je niks meer? Volg de gloeiende snuffelsterretjes van Baltoguh.",
      "guhs:sneeuwslee", [adv("balto_slee_tocht")], rewards=(("guhs:kaas_knabbels", 16),), shape="gear", xp=300)
    q("balto_slee_rustpunt", "Warme pootjes", "Onderweg staan &6vuurkorven&r. Rem af en sta er even stil: de sledehondjes warmen "
      "hun pootjes. Koude pootjes zijn trage pootjes!", "minecraft:campfire", [adv("balto_slee_rustpunt")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_slee_tocht"], xp=100)
    q("balto_slee_ijsbrug", "Over de ijsbrug", "De &bijsbrug&r is smal en glad: de slee glijdt door als je stuurt. Rustig aan, en "
      "blijf in het midden. Plof je eraf, dan land je in de zachte sneeuw en mag je het nog eens proberen.", "minecraft:packed_ice",
      [adv("balto_slee_ijsbrug")], rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_slee_tocht"], xp=100)
    q("balto_slee_lawine", "Opzij voor de lawine!", "Rommelt het boven op de helling? Een &flawine&r! Stuur naar de &eandere kant&r "
      "van de route voor de sneeuw eroverheen rolt. Te laat? BOEF, ondergesneeuwd: de sledehondjes graven je uit.",
      "minecraft:snow_block", [adv("balto_slee_lawine")], rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_slee_tocht"], xp=100)
    q("balto_slee_eigen", "Je eigen sneeuwslee", "Na het verhaal van Nomguh krijg je je &deigen sneeuwslee&r met vier "
      "guh-sledehondjes. Zet hem neer op sneeuw en rijd 200 blokken door de witte toendra (of een ander sneeuwland). Ze rennen "
      "alleen over sneeuw en ijs! Een kaasknabbel maakt ze extra blij.", "guhs:sneeuwslee", [adv("balto_slee_eigen")],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["balto_slee_tocht"], xp=200)
    for n, naam, icoon in (("makkelijk", "makkelijk", "minecraft:snowball"), ("medium", "medium", "minecraft:snow_block"),
                           ("lastig", "lastig", "minecraft:blue_ice")):
        q(f"balto_slee_sprint_{n}", f"Sledesprint: {naam}", f"Race tegen &5Steele-Mika&r (bij de startstreep van Nomguh) op &e{naam}&r: "
          "naar de berghut, eromheen en terug. Je tijd komt op het scorebord en in je Guhdex (Minigames).",
          icoon, [adv(f"balto_slee_sprint_{n}")], rewards=(("guhs:sledebelletje", 3),), deps=["balto_slee_tocht"], xp=150)
    q("balto_slee_steele", "Sneller dan Steele-Mika", "Win de sledesprint van &5Steele-Mika&r. Hij zegt vast dat het de wind was. "
      "Alle guhs van Nomguh giechelen lief.", "guhs:baltoslee_beker", [adv("balto_slee_steele")],
      rewards=(("guhs:sledebelletje", 4),), deps=["balto_slee_sprint_makkelijk"], xp=200)
    q("balto_slee_kampioen", "Kampioen van Nomguh", "Win de sledesprint op &clastig&r. Dan ben jij de snelste sledeleider van heel "
      "Nomguh. Njeh-heh... eh, VAHOEG!", "guhs:baltoslee_beker", [adv("balto_slee_lastig")],
      rewards=(("guhs:sledebelletje", 8),), deps=["balto_slee_steele"], shape="gear", xp=400)
    q("balto_slee_winkel", "Winterspulletjes", "Steele-Mika heeft een winkeltje: een sneeuwguh, een minisleetje, sledebellen, een "
      "routelantaarn, een sledehondenmandje en de gouden sledesprintbeker. Koop de &6sledebellenboog&r voor je sledebelletjes.",
      "guhs:baltoslee_sledebellen", [fq.item("guhs:baltoslee_sledebellen")], rewards=(("guhs:kaas_knabbels", 8),),
      deps=["balto_slee_sprint_makkelijk"], xp=100)
