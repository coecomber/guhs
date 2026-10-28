"""
1.8.0 Guhdromenland, island agent "plein" (dromen-ontwerp/BOUWPLAN_180.md part E). Java: feature/droomplein
(DroompleinFeature, DroomBrievenbusBlock, DroomBezocht, DroomFontein).

  build(h)    the brievenbusjes of the Droomposthuisje (one per island + the plein's own), the reuzenwekker's little clocks
              (droomwekker), their texts (Dutch in nl_nl, English in tools/lang/en/c213_droomplein.json)
  plein()     the Droomplein itself: the approved render dromen-ontwerp/_bron/r01_droomplein.py is run without rendering,
              its Scene turned into the template data/guhs/structure/dromen/eiland_plein.nbt (concept blocks -> the real
              blocks of parts A/B/D and this feature) and data/guhs/dromen/eiland/plein.json written (DroomEilanden's format:
              midden, aankomst, a steiger per island at the end of its cloud path, the Wekkerguh's spot behind its stand)

Standalone (from the project root): PYTHONPATH=tools python tools/features/droomplein.py
The design folder (dromen-ontwerp, next to the guhs folder) and its concept zip are only needed to (re)build the template
and to copy the brievenbus/wekker textures; without them the committed files stay as they are.
"""
import json
import math
import os
import re
import sys
import zipfile

BUSSEN = ["sterren", "molentje", "badje", "herinnering", "liedjes", "plein"]       # = DroompleinFeature.BUSSEN
HOEK = {"sterren": 0, "molentje": 72, "badje": 144, "herinnering": 216, "liedjes": 288}   # = DroomEilanden.HOEK
VERSIE = 2                       # 2 = playtest fix: ground in front of the Droomposthuisje; plein.json "versie": raise it when the template changes (a live world stamps it again)
TEMPLATE = "dromen/eiland_plein"
C, T = 42, 30                    # r01_droomplein.py: the plein's centre (x, z) and the height of its top
STEIGER_K = 31                   # how far along each cloud path its steiger spot lies (the path runs from 25 to 33)

P = "gui.guhs.droomplein."
EILAND = {"plein": ("het Droomplein", "the Dream Square"), "liedjes": ("de Slaapliedjesheuvels", "the Lullaby Hills"),
          "sterren": ("het Sterrenkussenveld", "the Star Cushion Field"), "herinnering": ("het Herinneringseiland", "Memory Island"),
          "badje": ("het Wolkenbadje", "the Cloud Bath"), "molentje": ("het Maanmolentje", "the Moon Mill")}

TEKSTEN = {  # key -> (Dutch, English)
    "block.guhs.droomwekker": ("Droomwekker", "Dream Alarm Clock"),
    P + "bus.wakker": ("Dit brievenbusje werkt alleen in een droom. Thuis komt er alleen reclame in.",
                       "This little mailbox only works in a dream. At home it only gets junk mail."),
    P + "bus.plein": ("Droompost bezorgt je naar: %s. Klik op het brievenbusje van een eiland om erheen te zweven!",
                      "Dream Post delivers you to: %s. Click an island's little mailbox to float there!"),
    P + "bus.nergens": ("nog nergens! Loop eerst over de wolkenpaadjes naar een eiland, njeg.",
                        "nowhere yet! First walk the cloud paths to an island, njeg."),
    P + "bus.onbekend": ("Daar ben je nog nooit geweest! Droompost bezorgt alleen op bekende adressen. Neem eerst het wolkenpaadje naar %s.",
                         "You've never been there! Dream Post only delivers to known addresses. Take the cloud path to %s first."),
    P + "bus.post0": ("Je stapt in de brievenbus. Postzegel op je neus, en... plop! Aangetekend verzonden.",
                      "You climb into the mailbox. Stamp on your nose, and... plop! Sent by registered mail."),
    P + "bus.post1": ("Droompost, snel en zacht. Niet kreuken, njeg!", "Dream Post, fast and soft. Do not crumple, njeg!"),
    P + "bus.post2": ("Een grote zeepbel neemt je mee. Afzender: het Droomplein.", "A big bubble takes you along. Sender: the Dream Square."),
    P + "fontein.kop": ("[Droomfontein]", "[Dream Fountain]"),
    P + "fontein.wie": ("%1$s (de guh van %2$s)", "%1$s (%2$s's guh)"),
    P + "fontein.wie_iemand": ("Een guh van %s", "A guh of %s's"),
    P + "fontein.droomt": ("%1$s droomt %2$s", "%1$s is dreaming %2$s"),
    P + "fontein.droom0": ("van een kaasknabbel zo groot als een huis. Hij woont erin.", "of a cheese nibble as big as a house. It lives inside."),
    P + "fontein.droom1": ("dat hij kan vliegen, maar alleen een klein stukje, en dan landt hij in een doos.",
                           "that it can fly, but only a little bit, and then it lands in a box."),
    P + "fontein.droom2": ("dat alle kussens van de wereld van hem zijn. Njeg.", "that all the pillows in the world are its own. Njeg."),
    P + "fontein.droom3": ("van een Blub die 'blub' zegt. Verder gebeurt er niks. Prachtige droom.",
                           "of a Blub that says 'blub'. Nothing else happens. Beautiful dream."),
    P + "fontein.droom4": ("dat hij de baas is en jij de guh. Je krijgt wel elke dag knabbels.",
                           "that it's the owner and you're the guh. You do get nibbles every day."),
    P + "fontein.droom5": ("van een regenboog van kaas. Hij heeft er al een hap uit genomen.",
                           "of a rainbow made of cheese. It already took a bite."),
    P + "fontein.droom6": ("dat het Guhdromenland is en hij droomt dat hij droomt. Vahoeg.",
                           "that it's in Guh Dreamland, dreaming that it's dreaming. Vahoeg."),
    P + "fontein.droom7": ("van een hele lange guhslee, met alle guhs van de server erin. Iemand zit op zijn oor.",
                           "of a very long guh sled with every guh on the server in it. Somebody sits on its ear."),
    P + "fontein.droom8": ("dat hij een heel kattenhuisje kreeg... en toch in de doos ging liggen.",
                           "that it got a whole cat house... and still lay down in the box."),
    P + "fontein.droom9": ("van een badeendje dat 'njeg' zegt in plaats van 'kwak'. Of andersom.",
                           "of a rubber duck that says 'njeg' instead of 'quack'. Or the other way round."),
    P + "fontein.droom10": ("dat het altijd zaterdagochtend is, met tekenfilms en pannenkoeken.",
                            "that it's always Saturday morning, with cartoons and pancakes."),
    P + "fontein.droom11": ("dat hij vadsiger is dan ooit. Onberekenbaar vahoeg zelfs.", "that it's vadsier than ever. Incalculably vahoeg even."),
    P + "fontein.alleen0": ("De fontein borrelt. Iedereen slaapt al, behalve jij. Zij droomt dat jij een guh bent.",
                            "The fountain bubbles. Everybody's asleep already, except you. It dreams you're a guh."),
    P + "fontein.alleen1": ("De fontein droomt van een knabbel. Oh wacht, die heb je er net in gegooid. Dank je!",
                            "The fountain dreams of a nibble. Oh wait, you just threw one in. Thank you!"),
    P + "fontein.alleen2": ("Sst... de fontein hoort alleen een heel zacht 'njeg' in de verte.",
                            "Shh... the fountain only hears a very soft 'njeg' far away."),
}
for e, (nl, en) in EILAND.items():
    TEKSTEN[f"block.guhs.droombrievenbus_{e}"] = (f"Droombrievenbusje: {nl[nl.index(' ') + 1:]}", f"Dream Mailbox: {en[en.index(' ') + 1:]}")

ITEMS = [f"droombrievenbus_{e}" for e in BUSSEN] + ["droomwekker"]


def _ontwerp():
    """The dromen-ontwerp/_bron folder (next to the guhs folder or two levels up from a worktree), or None."""
    d = os.path.abspath(os.getcwd())
    for _ in range(4):
        p = os.path.join(d, "dromen-ontwerp", "_bron")
        if os.path.isdir(p):
            return p
        d = os.path.dirname(d)
    return None


def _facing(name):
    return {"variants": {f"facing={f}": {"model": f"guhs:block/{name}", **({"y": y} if y else {})}
                         for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}


def blokken(h):
    A, w = h.A, h.w
    bron = _ontwerp()
    zipp = os.path.join(bron, "jars", "concept-1.8.0.zip") if bron else None
    z = zipfile.ZipFile(zipp) if zipp and os.path.exists(zipp) else None
    tex = os.path.join(A, "textures", "block")
    os.makedirs(tex, exist_ok=True)

    def kopie(van, naar):
        if z is not None:
            with open(os.path.join(tex, naar + ".png"), "wb") as f:
                f.write(z.read(f"assets/guhs/textures/block/{van}.png"))
        elif not os.path.exists(os.path.join(tex, naar + ".png")):
            raise SystemExit(f"droomplein: no concept zip and no texture {naar}.png")

    kopie("c_paal", "droombrievenbus_paal")
    kopie("c_vlag", "droombrievenbus_vlag")
    for e in BUSSEN:
        n = f"droombrievenbus_{e}"
        kopie(f"c_bus_{e}", n)
        bus, paal, vlag = f"guhs:block/{n}", "guhs:block/droombrievenbus_paal", "guhs:block/droombrievenbus_vlag"
        zij = ("north", "south", "east", "west")
        w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/block",
            "textures": {"bus": bus, "paal": paal, "vlag": vlag, "particle": bus}, "elements": [
                {"from": [7, 0, 7], "to": [9, 8, 9], "faces": {f: {"texture": "#paal"} for f in zij}},
                {"from": [4, 8, 3], "to": [12, 15, 13], "faces": {f: {"texture": "#bus"} for f in zij + ("up", "down")}},
                {"from": [12, 11, 8], "to": [13, 17, 9], "faces": {f: {"texture": "#vlag"} for f in zij + ("up",)}},
                {"from": [12, 15, 8.5], "to": [12.1, 17, 12], "faces": {"east": {"texture": "#vlag"}, "west": {"texture": "#vlag"}}}]})
        w(f"{A}/blockstates/{n}.json", _facing(n))
        w(f"{A}/models/item/{n}.json", {"parent": f"guhs:block/{n}"})
        h.self_drop(n)
    for van, naar in (("c_wekker", "droomwekker"), ("c_wekker_kast", "droomwekker_kast"), ("c_wekker_bel", "droomwekker_bel")):
        kopie(van, naar)
    if z is not None:
        m = json.loads(z.read("assets/guhs/models/block/c_wekker.json"))
        m["textures"] = {"voor": "guhs:block/droomwekker", "kast": "guhs:block/droomwekker_kast", "bel": "guhs:block/droomwekker_bel",
                         "particle": "guhs:block/droomwekker"}
        m["parent"] = "minecraft:block/block"
        w(f"{A}/models/block/droomwekker.json", m)
    w(f"{A}/blockstates/droomwekker.json", _facing("droomwekker"))
    w(f"{A}/models/item/droomwekker.json", {"parent": "guhs:block/droomwekker"})
    h.self_drop("droomwekker")


# ---------------------------------------------------------------------------------------------------------------------
# the plein template
# ---------------------------------------------------------------------------------------------------------------------
# concept block (render) -> real block; a tuple adds/overrides properties
CONCEPT = {
    "guhs:c_zeepbelglas": "guhs:zeepbelglas",
    "guhs:c_droomzee": "guhs:droomzee",
    "guhs:c_sterrenhemel": "guhs:sterrenplafond",          # the existing star ceiling (no second star block)
    "guhs:c_maanbloemetje": ("guhs:maanbloemetje", {"open": "true"}),
    "guhs:c_maanlampje": ("guhs:maanlampje_maan", {"facing": "south", "lit": "true"}),
    "guhs:c_droomzand": "guhs:droomzandblok",
    "guhs:c_slaapliedjesgras": "guhs:slaapliedjesgras",
    "guhs:c_doos_knabbels": "guhs:doos_knabbels",
    "guhs:c_kattenhuisje": "guhs:kattenhuisje_roze",
    "guhs:c_wekker": "guhs:droomwekker",
}
for _e in BUSSEN:
    CONCEPT[f"guhs:c_brievenbus_{_e}"] = f"guhs:droombrievenbus_{_e}"


def _naam(s):
    """'guhs:x[a=b,c=d]' -> ('guhs:x', {'a': 'b', 'c': 'd'})."""
    m = re.match(r"([^\[]+)(?:\[(.*)\])?$", s)
    props = dict(kv.split("=", 1) for kv in m.group(2).split(",")) if m.group(2) else {}
    return m.group(1), props


def _bestaat(naam, A):
    ns, pad = naam.split(":")
    return ns == "minecraft" or os.path.exists(os.path.join(A, "blockstates", pad + ".json")) or pad in {
        f"droombrievenbus_{e}" for e in BUSSEN} | {"droomwekker"}


def scene():
    """Runs r01_droomplein.py without rendering and returns its Scene."""
    bron = _ontwerp()
    if not bron:
        return None
    sys.path.insert(0, bron)
    import droomkit as Dk
    Dk.render = lambda *a, **k: None
    pad = os.path.join(bron, "r01_droomplein.py")
    oud = sys.argv
    sys.argv = [pad, "1"]
    try:
        ns = {"__name__": "r01_droomplein", "__file__": pad}
        with open(pad, encoding="utf-8") as f:
            exec(compile(f.read(), pad, "exec"), ns)
    finally:
        sys.argv = oud
    return ns["S"]


def _speeltest_fix(S):
    """Playtest fixes (testbuild 2) on top of the approved render: the Droomposthuisje stood half beyond the island's rim,
    so its door hung above nothing and the letterboxes in front of it floated. A patch of ground in front of the house
    (level with the plein, so the door is a plain walk in), the house floor resting on a fluffy underside, and no rim
    fence inside the house or on the new patch."""
    hx1, hx2, hz1, hz2 = 21, 31, 15, 22                  # the house (r01_droomplein.py)
    for x in range(hx1 - 2, hx2 + 4):
        for z in range(hz1 - 1, hz2 + 6):
            for y in (T + 1, T + 2):
                if "hekje" in str(S.get(x, y, z)):
                    S.air(x, y, z, x, y, z)
    for x in range(hx1 - 2, hx2 + 4):                     # the ground patch in front of the house (z hz2+1 .. hz2+5)
        for z in range(hz2 + 1, hz2 + 6):
            if S.is_air(x, T, z):
                S.set(x, T, z, "knuffelgras")
            for y in range(T - 3, T):
                if S.is_air(x, y, z):
                    S.set(x, y, z, "pluiswolblok")
    for x in range(hx1 - 1, hx2 + 2):                     # a fluffy underside below the house floor
        for z in range(hz1 - 1, hz2 + 2):
            diep = 4 - max(0, max(hx1 - x, x - hx2, hz1 - z, z - hz2))
            for y in range(T - diep, T):
                if S.is_air(x, y, z):
                    S.set(x, y, z, "pluiswolblok")
    S.air(26, T + 1, hz2 + 1, 26, T + 2, hz2 + 1)       # the door step stays free


def _rel(x, z):
    return [round(x - C, 2), 1, round(z - C, 2)]


def plein(h):
    import make_structures as ms
    S = scene()
    A, D = h.A, h.D
    if S is None:
        print("droomplein: dromen-ontwerp not found, the committed template stays")
        return
    _speeltest_fix(S)
    X, Y, Z = S.v.shape
    st = ms.Structure((X, Y, Z))
    vervangen, weg = {}, set()
    for x in range(X):
        for y in range(Y):
            for z in range(Z):
                i = int(S.v[x, y, z])
                if i == 0:
                    continue
                naam, props = _naam(S.pal[i])
                if naam in ("minecraft:air", "minecraft:cave_air"):
                    continue
                if naam in CONCEPT:
                    c = CONCEPT[naam]
                    naam, extra = (c, {}) if isinstance(c, str) else c
                    props = {**props, **extra}
                    if naam == "guhs:maanbloemetje":
                        props = {"open": "true"}
                    elif naam in ("guhs:zeepbelglas", "guhs:droomzee", "guhs:sterrenplafond", "guhs:droomzandblok",
                                  "guhs:slaapliedjesgras"):
                        props = {}
                    vervangen[naam] = vervangen.get(naam, 0) + 1
                if not _bestaat(naam, A):
                    weg.add(naam)
                    naam, props = "guhs:pluiswolblok", {}
                st.set(x, y, z, naam, props)
    st.save(TEMPLATE)
    if weg:
        print(f"droomplein: replaced missing blocks by pluiswolblok: {sorted(weg)}")
    steigers = {}
    for e, a in HOEK.items():
        r = math.radians(a)
        dx, dz = math.sin(r), -math.cos(r)
        steigers[e] = [math.floor(dx * STEIGER_K) + 0.5, 1, math.floor(dz * STEIGER_K) + 0.5]
    opzet = {"versie": VERSIE, "template": "guhs:" + TEMPLATE, "midden": [C, T, C],
             "aankomst": {"plek": [0.5, 1, 10.5], "yaw": 180},       # just south of the fountain, looking at it
             "steigers": steigers,
             "wekkerguh": {"plek": _rel(58.5, 45.5), "yaw": 90}}      # behind its stand, looking west at the plein
    h.w(f"{D}/dromen/eiland/plein.json", opzet)
    print(f"droomplein: template {X}x{Y}x{Z}, concept blocks turned real: {vervangen}")


def build(h):
    blokken(h)
    plein(h)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    A, D = h.A, h.D
    nodig = [f"{D}/dromen/eiland/plein.json", f"{D}/structure/{TEMPLATE}.nbt"] + [f"{A}/models/item/{n}.json" for n in ITEMS] \
        + [f"{A}/blockstates/{n}.json" for n in ITEMS]
    weg = [p for p in nodig if not os.path.exists(p)]
    weg += [k for k, (nl, en) in TEKSTEN.items() if nl.count("%") != en.count("%")]
    if weg:
        raise SystemExit(f"droomplein self-check failed: {weg}")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c213_droomplein.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in ITEMS:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"droomplein: {len(BUSSEN)} brievenbusjes, droomwekker, plein template + json, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
