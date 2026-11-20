"""
1.10.10: Het Slijmhuisje (Java: feature/slijmhuisje). A sweet little house of 3 x 3 (about 3 high) for one guh slime; on 4
vadskracht the slime makes a guh slimeball every 45 seconds into a bowl of 16. Plus the item "Opgepakte guh-slime" and the
chore "Slijmpjes verzorgen".

This module writes:
  * MECHANICS (stable): the loot table, the recipe, the tags (guhs:vadskracht for the hover readout, mineable/axe), the two
    advancements (quest/ + techniek/tech_machines_slijmhuisje), the client item definitions and every text (Dutch here,
    English in TEKSTEN too, and in tools/lang/en/c511_slijmhuisje.json, which this module writes as well);
  * THE LOOK (placeholder "Zeepbelkoepel", the user may still pick another one): every texture, block model and blockstate,
    in the section "the look" below and nothing else. The renderer (client/SlijmhuisjeRenderer.java) draws the slime on
    the cushion and the slimeballs in the bowl at the positions noted there.

Pieces (block slijmhuisje_deel, properties stuk + dy): layer 0 = the base (corners "hoek", edges "rand", the middle with the
cushion), the kern (slijmhuisje, facing + snoet) is the middle of the front edge with the round door, the snoet and the bowl;
layer 1 middle = the soap-bubble dome (translucent, it spans the 3 x 3 and two layers) with a flower on top; the rest shows
nothing.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/slijmhuisje.py
"""
import json
import os

import numpy as np
from PIL import Image

try:
    from features import vadskracht
except ImportError:  # (standalone with only tools/features on the path)
    import vadskracht

STATEN = ("slaapt", "werkt", "vol")
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
EN_FILE = os.path.join("tools", "lang", "en", "c511_slijmhuisje.json")

# =====================================================================================================================
# texts: key -> (Dutch, English)
# =====================================================================================================================
G = "gui.guhs.slijmhuisje."
O = "gui.guhs.huisje.overzicht.klus.slijmpjes."
TEKSTEN = {
    "block.guhs.slijmhuisje": ("Slijmhuisje", "Slime House"),
    "block.guhs.slijmhuisje.lore": ("Een lief huisje voor één guh-slime. Op 4 vadskracht maakt hij elke 45 tellen een guh-slijmbal in zijn schaaltje. Blub!",
                                    "A sweet little house for one guh slime. On 4 chonk power it makes a guh slimeball every 45 seconds, into its little bowl. Blub!"),
    "block.guhs.slijmhuisje_deel": ("Slijmhuisje", "Slime House"),
    "item.guhs.opgepakte_guh_slime": ("Opgepakte guh-slime", "Picked-up Guh Slime"),
    "item.guhs.opgepakte_guh_slime.tooltip": ("Rechtsklik op een blok: hij stuitert eruit. Rechtsklik op een Slijmhuisje: hij trekt erin.",
                                              "Right-click a block: it bounces out. Right-click a Slime House: it moves in."),
    "item.guhs.opgepakte_guh_slime.grootte.1": ("Grootte: klein slijmpje (1)", "Size: little slime (1)"),
    "item.guhs.opgepakte_guh_slime.grootte.2": ("Grootte: middelste slijm (2)", "Size: medium slime (2)"),
    "item.guhs.opgepakte_guh_slime.grootte.4": ("Grootte: dikke vadsslijm (4)", "Size: big chonky slime (4)"),
    G + "opgepakt": ("Je hebt %s opgepakt. Wiebel wiebel!", "You picked up %s. Wobble wobble!"),
    G + "neergezet": ("%s stuitert er weer vrolijk vandoor.", "%s happily bounces off again."),
    G + "erin": ("%s woont nu in het Slijmhuisje. Njeg, wat knus!", "%s lives in the Slime House now. Aww, so cosy!"),
    G + "bezet": ("Hier woont al een slijmpje. Eén per huisje, anders wordt het plakkerig!",
                  "A slime lives here already. One per house, or it gets sticky!"),
    G + "eruit": ("%s komt even naar buiten.", "%s comes out for a bit."),
    G + "niemand": ("Er woont nog niemand. Pak een guh-slime op (sluip + rechtsklik) en breng hem hier.",
                    "Nobody lives here yet. Pick up a guh slime (sneak + right-click) and bring it here."),
    G + "gepakt": ("Je pakt %s guh-slijmballen uit het schaaltje.", "You take %s guh slimeballs from the little bowl."),
    G + "leeg_klik": ("Het huisje is leeg. Breng er een guh-slime naartoe!", "The house is empty. Bring a guh slime here!"),
    G + "slaapt_klik": ("%s ligt te slapen. Zonder vadskracht maakt hij niks, zzz...", "%s is asleep. Without chonk power it makes nothing, zzz..."),
    G + "schaal_leeg": ("Het schaaltje is nog leeg. %s stuitert: nog %s tellen tot de volgende slijmbal.",
                        "The little bowl is still empty. %s is bouncing: %s more seconds until the next slimeball."),
    G + "leeg": ("Er woont nog geen slijmpje in dit huisje", "No slime lives in this house yet"),
    G + "bewoner": ("Hier woont %s (grootte %s)", "%s lives here (size %s)"),
    G + "schaal_vol": ("Het schaaltje is vol (%s): tevreden wacht hij tot iemand het leeghaalt", "The little bowl is full (%s): it waits contentedly until someone empties it"),
    G + "slaapt": ("Slaapt (geen vadskracht). In het schaaltje: %s/%s", "Asleep (no chonk power). In the little bowl: %s/%s"),
    G + "maakt": ("In het schaaltje: %s/%s. Volgende slijmbal over %s tellen", "In the little bowl: %s/%s. Next slimeball in %s seconds"),
    "advancements.guhs.techniek.tech_machines_slijmhuisje.title": ("Thuis is waar de slijm is", "Home Is Where the Slime Is"),
    "advancements.guhs.techniek.tech_machines_slijmhuisje.description": ("Laat een guh-slime in een Slijmhuisje een slijmbal maken",
                                                                          "Let a guh slime in a Slime House make a slimeball"),
    # the chore
    "gui.guhs.klus.slijmpjes": ("Slijmpjes verzorgen", "Caring for Slimes"),
    "gui.guhs.klus.slijmpjes.tip": ("Loopt naar je Slijmhuisje, aait het slijmpje en brengt de guh-slijmballen uit het schaaltje naar de kist. (Guhs)",
                                    "Walks to your Slime House, pets the slime and brings the guh slimeballs from the little bowl to the chest. (Guhs)"),
    O + "ophalen": ("Slijmhuisjes met slijmballen in het schaaltje: %s", "Slime Houses with slimeballs in the little bowl: %s"),
    O + "geen_slijm": ("Slijmhuisjes zonder slijmpje: %s. Breng er een guh-slime naartoe", "Slime Houses without a slime: %s. Bring a guh slime there"),
    O + "geen_kracht": ("Slijmhuisjes waar het slijmpje slaapt (geen vadskracht): %s", "Slime Houses where the slime is asleep (no chonk power): %s"),
    O + "maakt": ("Nu nog niets op te halen. Slijmhuisjes waar een slijmpje stuitert: %s", "Nothing to fetch yet. Slime Houses where a slime is bouncing: %s"),
    O + "geen": ("Geen Slijmhuisje van jou", "No Slime House of yours"),
    O + "nodig": ("een Slijmhuisje dat jij zelf hebt neergezet in de klus-area, met een guh-slime erin en vadskracht. Liggen er een paar "
                  "slijmballen in het schaaltje, dan aaien ze het slijmpje en brengen ze de slijmballen naar de kist naast het huisje of de Bank Guh.",
                  "a Slime House you placed yourself in the chore area, with a guh slime in it and chonk power. When there are a few "
                  "slimeballs in the little bowl, they pet the slime and bring the slimeballs to the chest next to the house or the Bank Guh."),
    "gui.guhs.dagboek.eerste.klusjes_slijmpjes": ("Slijmvriendje", "Slime Buddy"),
    "gui.guhs.dagboek.eerste.klusjes_slijmpjes.tekst": ("Ik heb een slijmpje geaaid. Hij voelde koud en wiebelig. Hij zei blub. Ik zei njeg. Nu zijn wij vrienden.",
                                                        "I petted a slime. It felt cold and wobbly. It said blub. I said nyeg. Now we're friends."),
    "gui.guhs.wistjedat.klusjes.slijmpjes": ("Wist je dat er bij %s een slijmpje in een zeepbel woont? Ik aai hem elke dag. Hij is plakkerig, maar wel lief.",
                                             "Did you know a slime lives in a soap bubble at %s? I pet it every day. It's sticky, but sweet."),
}

# =====================================================================================================================
# the look (PLACEHOLDER "Zeepbelkoepel"): textures, models, blockstates. Positions shared with SlijmhuisjeRenderer.java:
#   the cushion's top at y 19 of the middle base block, the bowl on the kern's top at x 5..11, z 0.5..4
# =====================================================================================================================
ROZE = (248, 186, 208)
MINT = (170, 232, 204)
MINT_DONKER = (110, 190, 160)
WIT = (255, 248, 250)


def _ruis(kleur, var, seed):
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    n = rng.normal(0, var, (16, 16))
    for c in range(3):
        a[..., c] = np.clip(kleur[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _png(a):
    return Image.fromarray(a)


def texturen():
    """name -> PIL image (block textures; plus the item texture under 'item/...')."""
    t = {}
    zij = _ruis(ROZE, 4, 101)
    zij[13:16, :, :3] = MINT
    zij[13, :, :3] = WIT
    t["zij"] = zij
    boven = _ruis(ROZE, 3, 102)
    boven[0, :, :3] = boven[15, :, :3] = boven[:, 0, :3] = boven[:, 15, :3] = MINT
    t["boven"] = boven
    for staat in STATEN:
        a = zij.copy()
        for y in range(7, 16):                      # the round door: an arch of mint with a darker rim
            for x in range(5, 11):
                dx = x - 7.5
                if y < 9 and dx * dx + (9 - y) ** 2 > 7:
                    continue
                a[y, x, :3] = MINT_DONKER if x in (5, 10) or y == 7 else MINT
        a[11, 9, :3] = (250, 214, 92)               # the little door knob
        img = _png(a)
        vadskracht.snoet(img, 2, 1, staat)
        t[f"voor_{staat}"] = img
    koepel = np.zeros((16, 16, 4), np.uint8)
    koepel[..., :3] = (214, 236, 255)
    koepel[..., 3] = 70
    for i in range(3, 9):                           # a shine streak
        koepel[i, i - 1, :] = (255, 255, 255, 170)
        koepel[i, i, :] = (255, 255, 255, 120)
    koepel[12:14, 11:13, :] = (255, 214, 240, 140)
    t["koepel"] = koepel
    kussen = _ruis((255, 226, 236), 3, 103)
    kussen[0, :, :3] = kussen[15, :, :3] = kussen[:, 0, :3] = kussen[:, 15, :3] = ROZE
    kussen[7:9, 7:9, :3] = ROZE                     # the button in the middle
    t["kussen"] = kussen
    t["schaal"] = _ruis(MINT, 4, 104)
    bloem = np.zeros((16, 16, 4), np.uint8)
    bloem[...] = (255, 170, 200, 255)
    bloem[6:10, 6:10, :3] = (255, 226, 110)
    t["bloem"] = bloem
    slijm = np.zeros((16, 16, 4), np.uint8)
    slijm[...] = (240, 140, 190, 255)
    slijm[4:12, 4:12, :3] = (226, 110, 170)         # the darker core seen through the jelly
    t["slijm"] = slijm
    voor = slijm.copy()
    voor[5:8, 4:6, :3] = voor[5:8, 10:12, :3] = (58, 28, 60)    # eyes
    voor[5, 4, :3] = voor[5, 10, :3] = (255, 255, 255)
    voor[10, 6:10, :3] = (150, 60, 110)                          # a little smile
    voor[9, 6, :3] = voor[9, 9, :3] = (150, 60, 110)
    voor[9, 2:4, :3] = voor[9, 12:14, :3] = (255, 120, 170)      # blush
    t["slijm_voor"] = voor
    bal = np.zeros((16, 16, 4), np.uint8)
    bal[...] = (236, 120, 180, 255)
    bal[0:4, 0:4, :3] = (255, 200, 228)
    t["balletje"] = bal
    # the item: a little pink slime cube in your hand
    item = np.zeros((16, 16, 4), np.uint8)
    for y in range(4, 15):
        for x in range(2, 14):
            item[y, x] = (240, 140, 190, 255)
    item[4, 2:14, :3] = item[14, 2:14, :3] = item[4:15, 2, :3] = item[4:15, 13, :3] = (190, 80, 140)
    item[6:12, 4:12, :3] = (226, 110, 170)
    item[7:9, 5, :3] = item[7:9, 10, :3] = (58, 28, 60)
    item[7, 5, :3] = item[7, 10, :3] = (255, 255, 255)
    item[11, 7:9, :3] = (150, 60, 110)
    item[5, 3:6, :3] = (255, 210, 232)
    t["item/opgepakte_guh_slime"] = item
    return {k: (v if isinstance(v, Image.Image) else _png(v)) for k, v in t.items()}


VOL = [0, 0, 16, 16]


def doos(van, tot, tex, zonder=()):
    faces = {}
    for vlak in ("north", "south", "west", "east", "up", "down"):
        if vlak in zonder:
            continue
        t = tex.get(vlak, tex.get("*")) if isinstance(tex, dict) else tex
        faces[vlak] = {"texture": t, "uv": VOL}
    return {"from": van, "to": tot, "faces": faces}


def _model(textures, elements, doorzichtig=False, parent="minecraft:block/block"):
    """A block model in the 26.1 shape: no render_type (26.1 picks the layer from the texture: alpha 0/255 = cutout);
    doorzichtig = every texture forced translucent (what tools/mc26.py makes of render_type translucent)."""
    if doorzichtig:
        textures = {k: {"sprite": v, "force_translucent": True} if not v.startswith("#") else v for k, v in textures.items()}
    return {"parent": parent, "textures": textures, "elements": elements}


def modellen():
    """path under models/ -> model json."""
    tx = lambda **meer: {"particle": "guhs:block/slijmhuisje_zij", "zij": "guhs:block/slijmhuisje_zij", "boven": "guhs:block/slijmhuisje_boven",
                         **{k: f"guhs:block/slijmhuisje_{v}" for k, v in meer.items()}}
    blok = lambda voor="#zij": doos([0, 0, 0], [16, 16, 16], {"north": voor, "up": "#boven", "*": "#zij"})
    schaal = [doos([5, 16, 0.5], [11, 17, 4], "#schaal"),
              doos([5, 17, 0.5], [11, 17.5, 1], "#schaal"), doos([5, 17, 3.5], [11, 17.5, 4], "#schaal")]
    m = {}
    for staat in STATEN:
        m[f"block/slijmhuisje_{staat}"] = _model(tx(voor=f"voor_{staat}", schaal="schaal"), [blok("#voor")] + schaal)
    m["block/slijmhuisje_rand"] = _model(tx(), [blok()])
    m["block/slijmhuisje_hoek"] = _model(tx(schaal="schaal"), [blok(), doos([0.5, 16, 0.5], [3.5, 18, 3.5], "#schaal")])
    m["block/slijmhuisje_midden"] = _model(tx(kussen="kussen"), [blok(), doos([3, 16, 3], [13, 19, 13], "#kussen")])
    m["block/slijmhuisje_leeg"] = {"textures": {"particle": "guhs:block/slijmhuisje_zij"}}
    koepel = [doos([-12, 0, -12], [28, 10, 28], "#koepel", zonder=("down",)),
              doos([-9, 10, -9], [25, 18, 25], "#koepel", zonder=("down",)),
              doos([-4, 18, -4], [20, 24, 20], "#koepel", zonder=("down",)),
              doos([6, 24, 6], [10, 26, 10], "#bloem"),
              doos([5, 26, 5], [11, 27, 11], "#bloem")]
    m["block/slijmhuisje_koepel"] = _model({"particle": "guhs:block/slijmhuisje_koepel", "koepel": "guhs:block/slijmhuisje_koepel",
                                            "bloem": "guhs:block/slijmhuisje_bloem"}, koepel, doorzichtig=True)
    m["block/slijmhuisje_slijm"] = _model({"particle": "guhs:block/slijmhuisje_slijm", "slijm": "guhs:block/slijmhuisje_slijm",
                                           "voor": "guhs:block/slijmhuisje_slijm_voor"},
                                          [doos([0, 0, 0], [16, 16, 16], {"north": "#voor", "*": "#slijm"})])
    m["block/slijmhuisje_balletje"] = _model({"particle": "guhs:block/slijmhuisje_balletje", "bal": "guhs:block/slijmhuisje_balletje"},
                                             [doos([6.5, 0, 6.5], [9.5, 2.5, 9.5], "#bal")])
    # the item: the house in small (base, dome, flower) as you see it from the front
    m["item/slijmhuisje"] = _model({"particle": "guhs:block/slijmhuisje_zij", "voor": "guhs:block/slijmhuisje_voor_werkt",
                                    "zij": "guhs:block/slijmhuisje_zij", "boven": "guhs:block/slijmhuisje_boven",
                                    "koepel": "guhs:block/slijmhuisje_koepel", "bloem": "guhs:block/slijmhuisje_bloem",
                                    "slijm": "guhs:block/slijmhuisje_slijm_voor"},
                                   [doos([1, 0, 1], [15, 7, 15], {"north": "#voor", "up": "#boven", "*": "#zij"}),
                                    doos([6, 7, 6], [10, 10, 10], "#slijm"),
                                    doos([2.5, 7, 2.5], [13.5, 11, 13.5], "#koepel", zonder=("down",)),
                                    doos([4.5, 11, 4.5], [11.5, 14, 11.5], "#koepel", zonder=("down",)),
                                    doos([7, 14, 7], [9, 15.5, 9], "#bloem")], doorzichtig=True)
    m["item/opgepakte_guh_slime"] = {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/opgepakte_guh_slime"}}
    return m


def blockstates():
    kern = {"variants": {f"facing={f},snoet={s}": {"model": f"guhs:block/slijmhuisje_{s}", **({"y": r} if r else {})}
                         for f, r in ROT.items() for s in STATEN}}
    deel = [{"apply": {"model": "guhs:block/slijmhuisje_leeg"}}]
    hoek = {"nw": 0, "no": 90, "zo": 180, "zw": 270}
    rand = {"o": 90, "z": 180, "w": 270}
    for stuk, r in hoek.items():
        deel.append({"when": {"dy": "0", "stuk": stuk}, "apply": {"model": "guhs:block/slijmhuisje_hoek", **({"y": r} if r else {})}})
    for stuk, r in rand.items():
        deel.append({"when": {"dy": "0", "stuk": stuk}, "apply": {"model": "guhs:block/slijmhuisje_rand", "y": r}})
    deel.append({"when": {"dy": "0", "stuk": "midden"}, "apply": {"model": "guhs:block/slijmhuisje_midden"}})
    deel.append({"when": {"dy": "1", "stuk": "midden"}, "apply": {"model": "guhs:block/slijmhuisje_koepel"}})
    return {"slijmhuisje": kern, "slijmhuisje_deel": {"multipart": deel}}


# =====================================================================================================================
# the mechanics' data
# =====================================================================================================================
RECEPT = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GGG", "PWP", "PDP"],
          "key": {"G": "minecraft:glass", "P": "minecraft:pink_wool", "W": "minecraft:white_wool", "D": "guhs:guh_wire"},
          "result": {"id": "guhs:slijmhuisje"}}
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _tag(r, pad, waarden, w):
    vol = os.path.join(r, "data", *pad.split("/")) + ".json"
    data = {"replace": False, "values": []}
    if os.path.exists(vol):
        with open(vol, encoding="utf-8") as f:
            data = json.load(f)
    for v in waarden:
        if v not in data["values"]:
            data["values"].append(v)
    w(vol, data)


def schrijf(a, r, w=_schrijf_json):
    """Everything but the lang files. a = assets/guhs, r = src/main/resources."""
    d = os.path.join(r, "data", "guhs")
    # the look
    for naam, img in texturen().items():
        pad = os.path.join(a, "textures", *(naam.split("/") if "/" in naam else ("block", f"slijmhuisje_{naam}"))) + ".png"
        os.makedirs(os.path.dirname(pad), exist_ok=True)
        img.save(pad)
    for pad, model in modellen().items():
        w(os.path.join(a, "models", *pad.split("/")) + ".json", model)
    for naam, staat in blockstates().items():
        w(os.path.join(a, "blockstates", naam + ".json"), staat)
    # the mechanics
    for item in ("slijmhuisje", "opgepakte_guh_slime"):
        w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{item}"}})
    w(os.path.join(d, "loot_table", "blocks", "slijmhuisje.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:slijmhuisje"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    w(os.path.join(d, "recipe", "slijmhuisje.json"), RECEPT)
    w(os.path.join(d, "advancement", "quest", "tech_machines_slijmhuisje.json"), {"criteria": IMPOSSIBLE})
    w(os.path.join(d, "advancement", "techniek", "tech_machines_slijmhuisje.json"), {
        "display": {"icon": {"id": "guhs:slijmhuisje"},
                    "title": {"translate": "advancements.guhs.techniek.tech_machines_slijmhuisje.title"},
                    "description": {"translate": "advancements.guhs.techniek.tech_machines_slijmhuisje.description"},
                    "frame": "task", "show_toast": True, "announce_to_chat": False, "hidden": False},
        "criteria": IMPOSSIBLE, "parent": "guhs:techniek/tech_machines_boom"})
    _tag(r, "guhs/tags/block/vadskracht", ["guhs:slijmhuisje", "guhs:slijmhuisje_deel"], w)
    _tag(r, "minecraft/tags/block/mineable/axe", ["guhs:slijmhuisje", "guhs:slijmhuisje_deel"], w)


def build(h):
    schrijf(h.A, h.R, h.w)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    r = os.path.join("src", "main", "resources")
    a = os.path.join(r, "assets", "guhs")
    schrijf(a, r)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf_json(p, data)
    _schrijf_json(EN_FILE, {k: v[1] for k, v in sorted(TEKSTEN.items())})
    print(f"slijmhuisje: {len(texturen())} textures, {len(modellen())} models, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
