"""
1.8.0 (Guhdromenland): De Zingende Weide, the rare Blub biome of the Guhmension (ontwerp-guhs180-dromen.md, "Blub-biome:
De Zingende Weide"; renders dromen-ontwerp/weide/). Java: feature/zingendeweide (ZingendeWeideFeature, WeideBlokken,
WeideDeel, Bubbelsap, WeideFeatures, WeideBouw(+Structure, Piece), KoorkomPodiumBlockEntity, WeideCommando,
client/WeideClient + WeideLucht).

  build(h)    the blocks (zalmklei, zeegroen, the pink grasses, zingbloemetjes, schildjeszand, pastel crystals, the wolkjes
              sliert), the wolkjeshout wood set (logs, planks, stairs, slab, fence, gate, door, trapdoor, button, pressure
              plate, sign, hanging sign, white and pink leaves, sapling, the sleepy guh face), Bubbelsap (a fluid, its bucket),
              the Koorkom podium, the handdoekje; the textures (from the concept zips of the renders, dromen-ontwerp/_bron/jars)
              and models, loot, recipes, tags; the biome, its region (a low noise of its own, masked off the older regions
              and the biomes3 regions), its surface, features, the three structures; the texts and a self-check

How the region works (like the Bleekwoud: features/bleekwoud.py). The low noise guhs:zingende_weide (512 blocks) gives
patches; where it is above TERM (and away from the seas, the Knuffeldal, the Guhpolder, the tundra, Guhwai'i, the
Bleekwoud and the three biomes3 regions) the multi-noise TEMPERATURE and HUMIDITY rise and the EROSION drops by SHIFT, to
the biome's own entry in the corner (2, 2, -2) that nothing else uses. The term is one named density function
(worldgen/density_function/zingende_weide_term.json), so the router only grows by one reference per slot. The terrain stays
what it is (the epic parts are features and structures); below y 40-48 the term fades out, so the cave biomes stay.
The five parts (Roze Zangweide, Groene Wiegweide, Schildjeszand, Zingkloofjes, Wolkjesbos) are zones of a second noise,
guhs:zingende_weide_deel, read by the surface rule and by Java (WeideDeel) with the same thresholds (DELEN).

Standalone (from the project root): PYTHONPATH=tools python tools/features/zingendeweide.py
"""
import io
import json
import os
import random
import sys
import zipfile

from PIL import Image, ImageDraw, ImageEnhance

try:
    from features import bio_wereld
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import bio_wereld

BIOME = "zingende_weide"
NOISE = "zingende_weide"
DEEL_NOISE = "zingende_weide_deel"
NOISE_OCTAVE = -10                  # (1024 blocks: few, big meadows)
TERM = (0.610, 0.620)               # (tuned with ZingendeWeideGameTests.weideAandeel: about as rare as the Wolkenweide, ~1.3-1.5%)
SHIFT = 12.0
ENTRY = (2.0, 2.0, -2.0)            # temperature, humidity, erosion of the biome's own entry
KELDER_Y = (40, 48)
# the five parts: (id, Dutch, English, lowest deel-noise value, highest); Java WeideDeel has the same numbers
DELEN = [
    ("wolkjesbos", "Wolkjesbos", "Cloudlet Wood", -10.0, -0.30),
    ("wiegweide", "Groene Wiegweide", "Green Cradle Meadow", -0.30, -0.10),
    ("zangweide", "Roze Zangweide", "Pink Song Meadow", -0.10, 0.22),
    ("schildjeszand", "Schildjeszand", "Shellsand", 0.22, 0.38),
    ("zingkloofjes", "Zingkloofjes", "Singing Gorges", 0.38, 10.0),
]

CONCEPT = os.path.join("..", "..", "dromen-ontwerp", "_bron", "jars")
VANILLA = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "libraries", "com", "mojang", "minecraft", "26.1.2",
                       "minecraft-26.1.2-client.jar")

HOUT = "wolkjeshout"
WOOD = {f"{HOUT}_trap": "oak_stairs", f"{HOUT}_plaat": "oak_slab", f"{HOUT}_hek": "oak_fence", f"{HOUT}_poort": "oak_fence_gate",
        f"{HOUT}_deur": "oak_door", f"{HOUT}_luik": "oak_trapdoor", f"{HOUT}_knop": "oak_button", f"{HOUT}_drukplaat": "oak_pressure_plate"}
STEEN = {"zeegroen_steen_trap": ("stone_stairs", "zeegroen_steen"), "zeegroen_steen_plaat": ("stone_slab", "zeegroen_steen"),
         "zeegroen_steen_muur": ("cobblestone_wall", "zeegroen_steen"),
         "zeegroen_bakstenen_trap": ("stone_brick_stairs", "zeegroen_bakstenen"), "zeegroen_bakstenen_plaat": ("stone_brick_slab", "zeegroen_bakstenen"),
         "zeegroen_bakstenen_muur": ("stone_brick_wall", "zeegroen_bakstenen"),
         "gepolijste_zeegroen_steen_trap": ("polished_andesite_stairs", "gepolijste_zeegroen_steen"),
         "gepolijste_zeegroen_steen_plaat": ("polished_andesite_slab", "gepolijste_zeegroen_steen")}
KLEUREN = ["cyaan", "lila", "roze"]
BLOEMEN = ["roze", "geel", "blauw"]

# key -> (Dutch, English)
TEKSTEN = {
    f"biome.guhs.{BIOME}": ("Zingende Weide", "Singing Meadow"),
    "block.guhs.zalmklei_gras": ("Zalmklei-gras", "Salmon Clay Grass"),
    "block.guhs.zalmklei": ("Zalmklei", "Salmon Clay"),
    "block.guhs.zalmklei_streep_a": ("Gestreepte zalmklei", "Striped Salmon Clay"),
    "block.guhs.zalmklei_streep_b": ("Donker gestreepte zalmklei", "Dark Striped Salmon Clay"),
    "block.guhs.zeegroen_gras": ("Zeegroen gras", "Seagreen Grass"),
    "block.guhs.zeegroen_steen": ("Zeegroene steen", "Seagreen Stone"),
    "block.guhs.zeegroen_steen_trap": ("Zeegroene steentrap", "Seagreen Stone Stairs"),
    "block.guhs.zeegroen_steen_plaat": ("Zeegroene steenplaat", "Seagreen Stone Slab"),
    "block.guhs.zeegroen_steen_muur": ("Zeegroen steenmuurtje", "Seagreen Stone Wall"),
    "block.guhs.zeegroen_bakstenen": ("Zeegroene bakstenen", "Seagreen Bricks"),
    "block.guhs.zeegroen_bakstenen_trap": ("Zeegroene bakstenentrap", "Seagreen Brick Stairs"),
    "block.guhs.zeegroen_bakstenen_plaat": ("Zeegroene bakstenenplaat", "Seagreen Brick Slab"),
    "block.guhs.zeegroen_bakstenen_muur": ("Zeegroen bakstenenmuurtje", "Seagreen Brick Wall"),
    "block.guhs.gepolijste_zeegroen_steen": ("Gepolijste zeegroene steen", "Polished Seagreen Stone"),
    "block.guhs.gepolijste_zeegroen_steen_trap": ("Gepolijste zeegroene steentrap", "Polished Seagreen Stone Stairs"),
    "block.guhs.gepolijste_zeegroen_steen_plaat": ("Gepolijste zeegroene steenplaat", "Polished Seagreen Stone Slab"),
    "block.guhs.weide_roze_gras": ("Roze gras", "Pink Grass"),
    "block.guhs.weide_lichtroze_gras": ("Lichtroze gras", "Light Pink Grass"),
    "block.guhs.weide_hoog_roze_gras": ("Hoog roze gras", "Tall Pink Grass"),
    "block.guhs.weide_hoog_lichtroze_gras": ("Hoog lichtroze gras", "Tall Light Pink Grass"),
    "block.guhs.zingbloemetje_roze": ("Roze zingbloemetje", "Pink Singing Flower"),
    "block.guhs.zingbloemetje_geel": ("Geel zingbloemetje", "Yellow Singing Flower"),
    "block.guhs.zingbloemetje_blauw": ("Blauw zingbloemetje", "Blue Singing Flower"),
    "block.guhs.schildjeszand": ("Schildjeszand", "Shellsand"),
    "block.guhs.wolkjes_sliert": ("Wolkjessliert", "Cloudlet Strand"),
    f"block.guhs.{HOUT}_stam": ("Wolkjeshoutstam", "Cloudlet Log"),
    f"block.guhs.{HOUT}_gestript": ("Gestripte wolkjeshoutstam", "Stripped Cloudlet Log"),
    f"block.guhs.{HOUT}_hout": ("Wolkjeshout", "Cloudlet Wood"),
    f"block.guhs.{HOUT}_gestript_hout": ("Gestript wolkjeshout", "Stripped Cloudlet Wood"),
    f"block.guhs.{HOUT}_gezicht": ("Slaperig guhgezichtje in wolkjeshout", "Sleepy Guh Face in Cloudlet Wood"),
    f"block.guhs.{HOUT}_planken": ("Wolkjeshouten planken", "Cloudlet Planks"),
    f"block.guhs.{HOUT}_trap": ("Wolkjeshouten trap", "Cloudlet Stairs"),
    f"block.guhs.{HOUT}_plaat": ("Wolkjeshouten plaat", "Cloudlet Slab"),
    f"block.guhs.{HOUT}_hek": ("Wolkjeshouten hek", "Cloudlet Fence"),
    f"block.guhs.{HOUT}_poort": ("Wolkjeshouten poort", "Cloudlet Fence Gate"),
    f"block.guhs.{HOUT}_deur": ("Wolkjeshouten deur", "Cloudlet Door"),
    f"block.guhs.{HOUT}_luik": ("Wolkjeshouten luik", "Cloudlet Trapdoor"),
    f"block.guhs.{HOUT}_knop": ("Wolkjeshouten knopje", "Cloudlet Button"),
    f"block.guhs.{HOUT}_drukplaat": ("Wolkjeshouten drukplaat", "Cloudlet Pressure Plate"),
    f"block.guhs.{HOUT}_bord": ("Wolkjeshouten bordje", "Cloudlet Sign"),
    f"block.guhs.{HOUT}_wandbord": ("Wolkjeshouten wandbordje", "Cloudlet Wall Sign"),
    f"block.guhs.{HOUT}_hangbord": ("Wolkjeshouten hangbordje", "Cloudlet Hanging Sign"),
    f"block.guhs.{HOUT}_wandhangbord": ("Wolkjeshouten wandhangbordje", "Cloudlet Wall Hanging Sign"),
    f"block.guhs.{HOUT}_bladeren": ("Wolkjesbladeren", "Cloudlet Leaves"),
    f"block.guhs.{HOUT}_bladeren_roze": ("Roze wolkjesbladeren", "Pink Cloudlet Leaves"),
    f"block.guhs.{HOUT}_zaailing": ("Wolkjesboompje", "Cloudlet Sapling"),
    "block.guhs.bubbelsap": ("Bubbelsap", "Bubble Sap"),
    "item.guhs.bubbelsap_emmer": ("Emmer bubbelsap", "Bucket of Bubble Sap"),
    "fluid_type.guhs.bubbelsap": ("Bubbelsap", "Bubble Sap"),
    "block.guhs.koorkom_podium": ("Koorkom-podiumpje", "Choir Bowl Stage"),
    "block.guhs.koorkom_podium.lore": ("Bij schemer komen de wilde Blubs hier samen om hun slaapliedje te piepen. Sst!",
                                       "At dusk the wild Blubs gather here to squeak their lullaby. Shh!"),
    "item.guhs.handdoekje": ("Bubbelhanddoekje", "Bubble Towel"),
    "item.guhs.handdoekje.lore": ("Rechtsklik: lekker afdrogen. Je voelt je fris en blij, vads!", "Right-click: dry off. You feel fresh and happy, vads!"),
    "gui.guhs.weide.handdoekje": ("Heerlijk afgedroogd. Fris en blij!", "Nicely dried off. Fresh and happy!"),
    "effect.guhs.bubbelig": ("Bubbelig", "Bubbly"),
    f"block.guhs.{HOUT}_gezicht.lore": ("Hij slaapt. Of doet hij alsof? Njeg...", "He's asleep. Or is he pretending? Njeg..."),
    "block.guhs.bubbelsap.lore": ("Glanzend regenboogsap. Blubs badderen er dolgraag in.", "Shiny rainbow sap. Blubs love to bathe in it."),
    "structure.guhs.weide_koorkom": ("De Koorkom", "The Choir Bowl"),
    "structure.guhs.weide_poeltjes": ("De Bubbelsap-poeltjes", "The Bubble Sap Pools"),
    "structure.guhs.weide_guhwand": ("De Guhwand", "The Guh Wall"),
    "entity.guhs.guh_npc.bubbelbadguh": ("Badmeester Bubbels", "Lifeguard Bubbles"),
    "gui.guhs.weide.badguh.1": ("Welkom bij de Bubbelsap-poeltjes! Pak een handdoekje, vads. Niet in het sap plassen, njeg!",
                                "Welcome to the Bubble Sap Pools! Take a towel, vads. No peeing in the sap, njeg!"),
    "gui.guhs.weide.badguh.2": ("Alsjeblieft, een warm handdoekje. Vers van de wolkjesboom!", "Here you go, a warm towel. Fresh from the cloudlet tree!"),
    "gui.guhs.weide.badguh.3": ("Je hebt er al een, vahoegerd! Morgen weer een vers handdoekje.", "You already have one, you vahoeger! A fresh towel tomorrow."),
    "gui.guhs.weide.badguh.4": ("Sssst... de Blubs badderen. Ze worden er bubbelig van. Jij ook trouwens.",
                                "Shhh... the Blubs are bathing. It makes them bubbly. You too, by the way."),
    "gui.guhs.weide.tp": ("Op naar de Zingende Weide! (%s, %s)", "Off to the Singing Meadow! (%s, %s)"),
    "gui.guhs.weide.tp.geen": ("Geen Zingende Weide gevonden in de buurt. Njeg!", "No Singing Meadow found nearby. Njeg!"),
    "gui.guhs.weide.plaats": ("Geplaatst: %s", "Placed: %s"),
    "gui.guhs.weide.alleen_guhmension": ("Dat kan alleen in de Guhmension, vads.", "That only works in the Guhmension, vads."),
    "gui.guhs.weide.koor": ("De Blubs piepen hun slaapliedje...", "The Blubs squeak their lullaby..."),
}
for _d, _nl, _en, _a, _b in DELEN:
    TEKSTEN[f"gui.guhs.weide.deel.{_d}"] = (_nl, _en)
for _k in KLEUREN:
    _nl = {"cyaan": "Cyaan", "lila": "Lila", "roze": "Roze"}[_k]
    _en = {"cyaan": "Cyan", "lila": "Lilac", "roze": "Pink"}[_k]
    TEKSTEN[f"block.guhs.weidekristal_{_k}"] = (f"{_nl} weidekristal", f"{_en} Meadow Crystal")
    TEKSTEN[f"block.guhs.weidekristalletjes_{_k}"] = (f"{_nl}e weidekristalletjes", f"{_en} Meadow Crystal Cluster")


# =====================================================================================================================
# textures
# =====================================================================================================================
_ZIPS = None


def concept(naam):
    """A texture of the render concepts (c_<naam>.png in one of the concept zips)."""
    global _ZIPS
    if _ZIPS is None:
        _ZIPS = [zipfile.ZipFile(os.path.join(CONCEPT, z)) for z in ("weide-concept-1.8.0.zip", "wolkjeshout-concept-1.8.0.zip")]
    for z in _ZIPS:
        p = f"assets/guhs/textures/block/c_{naam}.png"
        if p in z.namelist():
            return Image.open(io.BytesIO(z.read(p))).convert("RGBA")
    raise SystemExit(f"zingendeweide: concept texture c_{naam}.png is missing")


def vanilla_png(pad):
    with zipfile.ZipFile(VANILLA) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/{pad}.png"))).convert("RGBA")


def tint(img, rgb, keep=0.0):
    """Recolour by luminance onto rgb (keep: how much of the original colour stays)."""
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            n = tuple(int(min(255, c * (0.45 + 0.75 * l))) for c in rgb)
            px[x, y] = tuple(int(keep * o + (1 - keep) * c) for o, c in zip((r, g, b), n)) + (a,)
    return out


def lichter(img, f):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            px[x, y] = (int(r + (255 - r) * f), int(g + (255 - g) * f), int(b + (255 - b) * f), a)
    return out


def glans(img, punten):
    """The Guhwand eyes in the face log: a small shine (two light pixels per eye)."""
    out = img.copy()
    for (x, y) in punten:
        out.putpixel((x, y), (255, 255, 255, 255))
    return out


def textures(h):
    B = lambda img, n: h.save(img, "block", f"{n}.png")
    for n in ("zalmklei", "zalmklei_streep_a", "zalmklei_streep_b", "zeegroen_steen", "zeegroen_bakstenen", "schildjeszand"):
        B(concept(n), n)
    B(concept("zeegroen_steen_gepolijst"), "gepolijste_zeegroen_steen")
    for n in ("zalmklei_gras", "zeegroen_gras"):
        B(concept(f"{n}_top"), f"{n}_top")
        B(concept(f"{n}_zij"), f"{n}_zij")
    # the grasses: the user wants lief roze and lief licht roze growth
    roze_onder, roze_boven = concept("hoog_roze_gras_onder"), concept("hoog_roze_gras_boven")
    B(roze_onder, "weide_hoog_roze_gras_onder")
    B(roze_boven, "weide_hoog_roze_gras_boven")
    B(lichter(roze_onder, 0.45), "weide_hoog_lichtroze_gras_onder")
    B(lichter(roze_boven, 0.45), "weide_hoog_lichtroze_gras_boven")
    kort = vanilla_png("block/short_grass")
    B(tint(kort, (246, 140, 186)), "weide_roze_gras")
    B(lichter(tint(kort, (246, 140, 186)), 0.45), "weide_lichtroze_gras")
    for k in BLOEMEN:
        B(concept(f"zingbloemetje_{k}"), f"zingbloemetje_{k}")
    for k in KLEUREN:
        B(concept(f"kristal_{k}"), f"weidekristal_{k}")
        B(concept(f"kristalletjes_{k}"), f"weidekristalletjes_{k}")
    sliert = concept("wolkjes_sliert")
    B(sliert, "wolkjes_sliert")
    punt = sliert.copy()
    ImageDraw.Draw(punt).rectangle([0, 11, 15, 15], fill=(0, 0, 0, 0))
    B(punt, "wolkjes_sliert_punt")
    # wood
    B(concept("wolkjesboom_stam_zij"), f"{HOUT}_stam")
    B(concept("wolkjesboom_stam_top"), f"{HOUT}_stam_top")
    B(concept("wolkjes_gestript_zij"), f"{HOUT}_gestript")
    B(concept("wolkjes_gestript_top"), f"{HOUT}_gestript_top")
    gezicht = concept("wolkjesboom_stam_gezicht_zij")
    B(gezicht, f"{HOUT}_gezicht")
    B(concept("wolkjes_planken"), f"{HOUT}_planken")
    B(concept("wolkjes_deur_onder"), f"{HOUT}_deur_bottom")
    B(concept("wolkjes_deur_boven"), f"{HOUT}_deur_top")
    B(concept("wolkjes_luik"), f"{HOUT}_luik")
    B(concept("wolkjesboom_blad"), f"{HOUT}_bladeren")
    B(concept("wolkjesboom_blad_roze"), f"{HOUT}_bladeren_roze")
    zaailing = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(zaailing)
    d.rectangle([7, 8, 8, 15], fill=(150, 146, 152, 255))
    for (x, y, r) in ((8, 6, 4), (5, 7, 3), (11, 7, 3), (8, 3, 3)):
        d.ellipse([x - r, y - r, x + r, y + r], fill=(252, 244, 248, 255))
    d.point([(6, 6), (10, 5)], fill=(255, 210, 228, 255))
    B(zaailing, f"{HOUT}_zaailing")
    # sign textures (entity) and the hanging sign's edit screen: vanilla's oak, made cloudlet grey-white
    planken_kleur = (226, 222, 228)
    h.save(tint(vanilla_png("entity/signs/oak"), planken_kleur, 0.15), "entity", "signs", f"{HOUT}.png")
    h.save(tint(vanilla_png("entity/signs/hanging/oak"), planken_kleur, 0.15), "entity", "signs", "hanging", f"{HOUT}.png")
    h.save(tint(vanilla_png("gui/hanging_signs/oak"), planken_kleur, 0.15), "gui", "hanging_signs", f"{HOUT}.png")
    # items
    h.save(tint(vanilla_png("item/oak_door"), planken_kleur, 0.1), "item", f"{HOUT}_deur.png")
    h.save(tint(vanilla_png("item/oak_sign"), planken_kleur, 0.1), "item", f"{HOUT}_bord.png")
    h.save(tint(vanilla_png("item/oak_hanging_sign"), planken_kleur, 0.1), "item", f"{HOUT}_hangbord.png")
    # bubbelsap: an animated rainbow sheen (still: 16 frames of the concept texture hue-shifted; flow: 32x32 frames)
    sap = concept("bubbelsap")
    frames = [_hue(sap, i / 16.0) for i in range(16)]
    still = Image.new("RGBA", (16, 16 * 16))
    for i, f in enumerate(frames):
        still.paste(f, (0, 16 * i))
    B(still, "bubbelsap_still")
    flow = Image.new("RGBA", (32, 32 * 16))
    for i, f in enumerate(frames):
        big = f.resize((32, 32), Image.NEAREST)
        flow.paste(big, (0, 32 * i))
    B(flow, "bubbelsap_flow")
    for n in ("bubbelsap_still", "bubbelsap_flow"):
        with open(os.path.join(h.TEX, "block", f"{n}.png.mcmeta"), "w", encoding="utf-8", newline="\n") as fh:
            json.dump({"animation": {"frametime": 3, "interpolate": True}}, fh)
    emmer = vanilla_png("item/water_bucket")
    px = emmer.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a and b > r + 30 and b > g:        # the water in the bucket -> rainbow sap
                kl = [(255, 140, 190), (255, 210, 120), (150, 230, 170), (130, 200, 255), (200, 160, 255)][(x + y) % 5]
                px[x, y] = kl + (a,)
    h.save(emmer, "item", "bubbelsap_emmer.png")
    # the Koorkom podium (polished seagreen with a music note) and the towel
    pod = concept("zeegroen_steen_gepolijst").copy()
    d = ImageDraw.Draw(pod)
    d.rectangle([7, 3, 8, 10], fill=(255, 190, 220, 255))
    d.rectangle([9, 3, 11, 4], fill=(255, 190, 220, 255))
    d.ellipse([4, 9, 8, 13], fill=(255, 150, 200, 255))
    B(pod, "koorkom_podium_top")
    B(concept("zeegroen_bakstenen"), "koorkom_podium_zij")
    doek = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(doek)
    d.rounded_rectangle([2, 4, 13, 12], 2, fill=(170, 220, 250, 255), outline=(110, 170, 220, 255))
    for x in range(3, 13, 3):
        d.line([x, 5, x, 11], fill=(250, 250, 255, 255))
    d.ellipse([10, 1, 14, 5], fill=(255, 255, 255, 230))
    d.ellipse([1, 11, 4, 14], fill=(255, 255, 255, 230))
    h.save(doek, "item", "handdoekje.png")
    # the particles: a trillertje (a little coloured light; the client tints it) and the light pillar's dot
    tr = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(tr)
    d.ellipse([1, 1, 6, 6], fill=(255, 255, 255, 120))
    d.ellipse([2, 2, 5, 5], fill=(255, 255, 255, 255))
    h.save(tr, "particle", "trillertje.png")
    eff = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(eff)
    for (x, y, r, kl) in ((6, 10, 5, (150, 220, 255, 255)), (12, 6, 4, (255, 180, 220, 255)), (13, 13, 3, (200, 255, 210, 255))):
        d.ellipse([x - r, y - r, x + r, y + r], outline=kl, width=1)
        d.point([(x - r // 2, y - r // 2)], fill=(255, 255, 255, 255))
    h.save(eff, "mob_effect", "bubbelig.png")


def _hue(img, shift):
    import colorsys
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hls_to_rgb((hh + shift) % 1.0, ll, min(1.0, ss * 1.15))
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), max(a, 200))
    return out


# =====================================================================================================================
# blockstates, models, items
# =====================================================================================================================
def copy_set(h, z, mapping, tex, group, base_item):
    """Vanilla's blockstates, block models, loot and recipes of `mapping` (ours -> (vanilla, our base block)), retextured."""
    A, D, w = h.A, h.D, h.w
    names = z.namelist()
    for ours, (van, basis) in mapping.items():
        state = json.dumps(json.loads(z.read(f"assets/minecraft/blockstates/{van}.json")))
        state = state.replace(f"minecraft:block/{van}", f"guhs:block/{ours}")
        state = state.replace(f'"minecraft:block/{base_item(van)}"', f'"guhs:block/{basis}"')      # (a double slab: the full block)
        w(f"{A}/blockstates/{ours}.json", json.loads(state))
        for path in names:
            if not path.startswith(f"assets/minecraft/models/block/{van}") or not path.endswith(".json"):
                continue
            rest = path[len(f"assets/minecraft/models/block/{van}"):-5]
            if van == "oak_fence" and rest.startswith("_gate"):
                continue
            if van == "oak_pressure_plate" and rest not in ("", "_down"):
                continue
            if van == "oak_button" and rest not in ("", "_pressed", "_inventory"):
                continue
            model = json.loads(z.read(path))
            model["textures"] = {k: tex.get(v, v) for k, v in model.get("textures", {}).items()}
            if van in ("oak_door", "oak_trapdoor"):
                model["render_type"] = "minecraft:cutout"
            w(f"{A}/models/block/{ours}{rest}.json", model)
        loot = json.loads(z.read(f"data/minecraft/loot_table/blocks/{van}.json"))
        w(f"{D}/loot_table/blocks/{ours}.json", json.loads(json.dumps(loot).replace(f"minecraft:{van}", f"guhs:{ours}")))
        recipe = json.loads(z.read(f"data/minecraft/recipe/{van}.json"))
        tekst = json.dumps(recipe).replace(f'"minecraft:{base_item(van)}"', f'"guhs:{basis}"').replace(f'"minecraft:{van}"', f'"guhs:{ours}"')
        tekst = tekst.replace('"group": "wooden_', f'"group": "{group}_')
        w(f"{D}/recipe/{ours}.json", json.loads(tekst))
        item = json.loads(z.read(f"assets/minecraft/items/{van}.json"))
        item_tekst = json.dumps(item).replace(f"minecraft:block/{van}", f"guhs:block/{ours}").replace(f"minecraft:item/{van}", f"guhs:item/{ours}")
        w(f"{A}/items/{ours}.json", json.loads(item_tekst))


def _item(h, naam, model):
    h.w(f"{h.A}/items/{naam}.json", {"model": {"type": "minecraft:model", "model": model}})


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w

    def cube(n, tex=None, render=None):
        h.simple_block(n, tex, render)
        _item(h, n, f"guhs:block/{n}")

    for n in ("zalmklei", "zalmklei_streep_a", "zalmklei_streep_b", "zeegroen_steen", "zeegroen_bakstenen", "gepolijste_zeegroen_steen",
              "schildjeszand"):
        cube(n)
    for k in KLEUREN:
        cube(f"weidekristal_{k}", render="minecraft:translucent")
    for n in ("zalmklei_gras", "zeegroen_gras"):
        w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"guhs:block/{n}_top", "side": f"guhs:block/{n}_zij", "bottom": "guhs:block/zalmklei"}})
        w(f"{A}/blockstates/{n}.json", {"variants": {"": [{"model": f"guhs:block/{n}"}, {"model": f"guhs:block/{n}", "y": 90},
                                                         {"model": f"guhs:block/{n}", "y": 180}, {"model": f"guhs:block/{n}", "y": 270}]}})
        _item(h, n, f"guhs:block/{n}")
    # crosses: the short grasses, the flowers, the sapling
    for n in ("weide_roze_gras", "weide_lichtroze_gras", *[f"zingbloemetje_{k}" for k in BLOEMEN], f"{HOUT}_zaailing"):
        w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{n}"}})
        w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": f"guhs:block/{n}"}}})
        h.item_model(n, f"guhs:block/{n}")
        _item(h, n, f"guhs:item/{n}")
    for n in ("weide_hoog_roze_gras", "weide_hoog_lichtroze_gras"):
        for half, t in (("lower", "onder"), ("upper", "boven")):
            w(f"{A}/models/block/{n}_{t}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                 "textures": {"cross": f"guhs:block/{n}_{t}"}})
        w(f"{A}/blockstates/{n}.json", {"variants": {"half=lower": {"model": f"guhs:block/{n}_onder"}, "half=upper": {"model": f"guhs:block/{n}_boven"}}})
        h.item_model(n, f"guhs:block/{n}_boven")
        _item(h, n, f"guhs:item/{n}")
    # the crystal clusters: amethyst's shape, facing
    for k in KLEUREN:
        n = f"weidekristalletjes_{k}"
        w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{n}"}})
        rot = {"down": {"x": 180}, "up": {}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "west": {"x": 90, "y": 270}, "east": {"x": 90, "y": 90}}
        w(f"{A}/blockstates/{n}.json", {"variants": {f"facing={f},waterlogged={wl}": {"model": f"guhs:block/{n}", **r}
                                                     for f, r in rot.items() for wl in ("true", "false")}})
        h.item_model(n, f"guhs:block/{n}")
        _item(h, n, f"guhs:item/{n}")
    # the strand (hanging, like pale hanging moss)
    for n, t in (("wolkjes_sliert", "wolkjes_sliert"), ("wolkjes_sliert_punt", "wolkjes_sliert_punt")):
        w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{t}"}})
    w(f"{A}/blockstates/wolkjes_sliert.json", {"variants": {"tip=false": {"model": "guhs:block/wolkjes_sliert"},
                                                            "tip=true": {"model": "guhs:block/wolkjes_sliert_punt"}}})
    h.item_model("wolkjes_sliert", "guhs:block/wolkjes_sliert_punt")
    _item(h, "wolkjes_sliert", "guhs:item/wolkjes_sliert")
    # logs and wood
    for n, side, top in ((f"{HOUT}_stam", f"{HOUT}_stam", f"{HOUT}_stam_top"), (f"{HOUT}_gestript", f"{HOUT}_gestript", f"{HOUT}_gestript_top"),
                         (f"{HOUT}_hout", f"{HOUT}_stam", f"{HOUT}_stam"), (f"{HOUT}_gestript_hout", f"{HOUT}_gestript", f"{HOUT}_gestript")):
        t = {"end": f"guhs:block/{top}", "side": f"guhs:block/{side}"}
        w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_column", "textures": t})
        w(f"{A}/models/block/{n}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": t})
        w(f"{A}/blockstates/{n}.json", {"variants": {
            "axis=y": {"model": f"guhs:block/{n}"},
            "axis=z": {"model": f"guhs:block/{n}_horizontal", "x": 90},
            "axis=x": {"model": f"guhs:block/{n}_horizontal", "x": 90, "y": 90}}})
        _item(h, n, f"guhs:block/{n}")
    w(f"{A}/models/block/{HOUT}_gezicht.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": f"guhs:block/{HOUT}_stam_top", "front": f"guhs:block/{HOUT}_gezicht", "side": f"guhs:block/{HOUT}_stam"}})
    w(f"{A}/blockstates/{HOUT}_gezicht.json", {"variants": h.facing_states(f"{HOUT}_gezicht")})
    _item(h, f"{HOUT}_gezicht", f"guhs:block/{HOUT}_gezicht")
    cube(f"{HOUT}_planken")
    for n in (f"{HOUT}_bladeren", f"{HOUT}_bladeren_roze"):
        cube(n, render="minecraft:cutout_mipped")
    for b, p in ((f"{HOUT}_bord", f"{HOUT}_planken"), (f"{HOUT}_wandbord", f"{HOUT}_planken"), (f"{HOUT}_hangbord", f"{HOUT}_gestript"),
                 (f"{HOUT}_wandhangbord", f"{HOUT}_gestript")):
        w(f"{A}/models/block/{b}.json", {"textures": {"particle": f"guhs:block/{p}"}})
        w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"guhs:block/{b}"}}})
    for b in (f"{HOUT}_bord", f"{HOUT}_hangbord"):
        h.item_model(b)
        _item(h, b, f"guhs:item/{b}")
    # bubbelsap (the fluid's model only gives the particle) and its bucket
    w(f"{A}/models/block/bubbelsap.json", {"textures": {"particle": "guhs:block/bubbelsap_still"}})
    w(f"{A}/blockstates/bubbelsap.json", {"variants": {"": {"model": "guhs:block/bubbelsap"}}})
    h.item_model("bubbelsap_emmer")
    _item(h, "bubbelsap_emmer", "guhs:item/bubbelsap_emmer")
    # the podium and the towel
    w(f"{A}/models/block/koorkom_podium.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/koorkom_podium_top", "side": "guhs:block/koorkom_podium_zij", "bottom": "guhs:block/zeegroen_bakstenen"}})
    w(f"{A}/blockstates/koorkom_podium.json", {"variants": {"": {"model": "guhs:block/koorkom_podium"}}})
    _item(h, "koorkom_podium", "guhs:block/koorkom_podium")
    h.item_model(f"{HOUT}_deur")
    h.item_model("handdoekje")
    _item(h, "handdoekje", "guhs:item/handdoekje")
    # vanilla-shaped sets
    with zipfile.ZipFile(VANILLA) as z:
        hout_tex = {"minecraft:block/oak_planks": f"guhs:block/{HOUT}_planken", "minecraft:block/oak_door_top": f"guhs:block/{HOUT}_deur_top",
                    "minecraft:block/oak_door_bottom": f"guhs:block/{HOUT}_deur_bottom", "minecraft:block/oak_trapdoor": f"guhs:block/{HOUT}_luik"}
        copy_set(h, z, {k: (v, f"{HOUT}_planken") for k, v in WOOD.items()}, hout_tex, HOUT, lambda van: "oak_planks")
        basis = {"stone_stairs": "stone", "stone_slab": "stone", "cobblestone_wall": "cobblestone", "stone_brick_stairs": "stone_bricks",
                 "stone_brick_slab": "stone_bricks", "stone_brick_wall": "stone_bricks", "polished_andesite_stairs": "polished_andesite",
                 "polished_andesite_slab": "polished_andesite"}
        for ours, (van, ons) in STEEN.items():
            tex = {f"minecraft:block/{basis[van]}": f"guhs:block/{ons}"}
            if van == "stone_slab":
                tex.update({"minecraft:block/smooth_stone": f"guhs:block/{ons}", "minecraft:block/smooth_stone_slab_side": f"guhs:block/{ons}"})
            copy_set(h, z, {ours: (van, ons)}, tex, "zeegroen", lambda v, b=basis: b[v])
        # the hanging sign and the sign: vanilla's recipes with our items
        for ours, van, base in ((f"{HOUT}_bord", "oak_sign", f"{HOUT}_planken"), (f"{HOUT}_hangbord", "oak_hanging_sign", f"{HOUT}_gestript")):
            tekst = json.dumps(json.loads(z.read(f"data/minecraft/recipe/{van}.json")))
            tekst = tekst.replace('"minecraft:oak_planks"', f'"guhs:{base}"').replace('"minecraft:stripped_oak_log"', f'"guhs:{base}"')
            tekst = tekst.replace(f'"minecraft:{van}"', f'"guhs:{ours}"').replace('"group": "wooden_', f'"group": "{HOUT}_').replace('"group": "hanging_sign"', f'"group": "{HOUT}_hanging_sign"')
            w(f"{D}/recipe/{ours}.json", json.loads(tekst))
        leaves = json.dumps(json.loads(z.read("data/minecraft/loot_table/blocks/cherry_leaves.json")))
        for n in (f"{HOUT}_bladeren", f"{HOUT}_bladeren_roze"):
            w(f"{D}/loot_table/blocks/{n}.json", json.loads(leaves.replace("minecraft:cherry_leaves", f"guhs:{n}")
                                                            .replace("minecraft:cherry_sapling", f"guhs:{HOUT}_zaailing")))
        tall = json.dumps(json.loads(z.read("data/minecraft/loot_table/blocks/tall_grass.json")))
        short = json.dumps(json.loads(z.read("data/minecraft/loot_table/blocks/short_grass.json")))
        for n in ("weide_hoog_roze_gras", "weide_hoog_lichtroze_gras"):
            w(f"{D}/loot_table/blocks/{n}.json", json.loads(tall.replace("minecraft:tall_grass", f"guhs:{n}").replace(
                "minecraft:short_grass", f"guhs:{n.replace('hoog_', '')}")))
        for n in ("weide_roze_gras", "weide_lichtroze_gras"):
            w(f"{D}/loot_table/blocks/{n}.json", json.loads(short.replace("minecraft:short_grass", f"guhs:{n}")))
        hang = json.dumps(json.loads(z.read("data/minecraft/loot_table/blocks/pale_hanging_moss.json")))
        w(f"{D}/loot_table/blocks/wolkjes_sliert.json", json.loads(hang.replace("minecraft:pale_hanging_moss", "guhs:wolkjes_sliert")))
        for k in KLEUREN:
            cl = json.dumps(json.loads(z.read("data/minecraft/loot_table/blocks/amethyst_cluster.json")))
            w(f"{D}/loot_table/blocks/weidekristalletjes_{k}.json", json.loads(cl.replace("minecraft:amethyst_cluster", f"guhs:weidekristalletjes_{k}")
                                                                              .replace("minecraft:amethyst_shard", f"guhs:weidekristalletjes_{k}")))
        for n in ("zalmklei_gras", "zeegroen_gras"):
            g = json.dumps(json.loads(z.read("data/minecraft/loot_table/blocks/grass_block.json")))
            w(f"{D}/loot_table/blocks/{n}.json", json.loads(g.replace("minecraft:grass_block", f"guhs:{n}").replace("minecraft:dirt", "guhs:zalmklei")))


def loot_recipes_tags(h):
    D = h.D
    for n in ("zalmklei", "zalmklei_streep_a", "zalmklei_streep_b", "zeegroen_steen", "zeegroen_bakstenen", "gepolijste_zeegroen_steen",
              "schildjeszand", *[f"weidekristal_{k}" for k in KLEUREN], *[f"zingbloemetje_{k}" for k in BLOEMEN],
              f"{HOUT}_stam", f"{HOUT}_gestript", f"{HOUT}_hout", f"{HOUT}_gestript_hout", f"{HOUT}_gezicht", f"{HOUT}_planken",
              f"{HOUT}_zaailing", "koorkom_podium"):
        h.self_drop(n)
    for b, item in ((f"{HOUT}_bord", f"{HOUT}_bord"), (f"{HOUT}_wandbord", f"{HOUT}_bord"), (f"{HOUT}_hangbord", f"{HOUT}_hangbord"),
                    (f"{HOUT}_wandhangbord", f"{HOUT}_hangbord")):
        h.self_drop(b, item)
    # recipes
    h.shapeless(f"{HOUT}_planken", [f"#guhs:{HOUT}_stammen"], f"guhs:{HOUT}_planken", 4)
    h.shaped(f"{HOUT}_hout", ["##", "##"], {"#": f"guhs:{HOUT}_stam"}, f"guhs:{HOUT}_hout", 3)
    h.shaped(f"{HOUT}_gestript_hout", ["##", "##"], {"#": f"guhs:{HOUT}_gestript"}, f"guhs:{HOUT}_gestript_hout", 3)
    h.shaped("zeegroen_bakstenen", ["##", "##"], {"#": "guhs:zeegroen_steen"}, "guhs:zeegroen_bakstenen", 4)
    h.shaped("gepolijste_zeegroen_steen", ["##", "##"], {"#": "guhs:zeegroen_bakstenen"}, "guhs:gepolijste_zeegroen_steen", 4)
    h.shaped("koorkom_podium", ["NNN", "GBG"], {"N": "minecraft:note_block", "G": "guhs:gepolijste_zeegroen_steen", "B": "guhs:zingbloemetje_roze"},
             "guhs:koorkom_podium")
    for k in BLOEMEN:
        kleur = {"roze": "pink", "geel": "yellow", "blauw": "light_blue"}[k]
        h.shapeless(f"{kleur}_dye_van_zingbloemetje_{k}", [f"guhs:zingbloemetje_{k}"], f"minecraft:{kleur}_dye", 1)
    # tags
    T = lambda pad, vals: h.add_tag(pad, [f"guhs:{v}" if ":" not in v else v for v in vals])
    stammen = [f"{HOUT}_stam", f"{HOUT}_gestript", f"{HOUT}_hout", f"{HOUT}_gestript_hout", f"{HOUT}_gezicht"]
    T(f"guhs/tags/block/{HOUT}_stammen", stammen)
    T(f"guhs/tags/item/{HOUT}_stammen", stammen)
    for soort in ("block", "item"):
        T(f"minecraft/tags/{soort}/logs_that_burn", [f"#guhs:{HOUT}_stammen"])
        T(f"minecraft/tags/{soort}/planks", [f"{HOUT}_planken"])
        T(f"minecraft/tags/{soort}/wooden_stairs", [f"{HOUT}_trap"])
        T(f"minecraft/tags/{soort}/wooden_slabs", [f"{HOUT}_plaat"])
        T(f"minecraft/tags/{soort}/wooden_fences", [f"{HOUT}_hek"])
        T(f"minecraft/tags/{soort}/fence_gates", [f"{HOUT}_poort"])
        T(f"minecraft/tags/{soort}/wooden_doors", [f"{HOUT}_deur"])
        T(f"minecraft/tags/{soort}/wooden_trapdoors", [f"{HOUT}_luik"])
        T(f"minecraft/tags/{soort}/wooden_buttons", [f"{HOUT}_knop"])
        T(f"minecraft/tags/{soort}/wooden_pressure_plates", [f"{HOUT}_drukplaat"])
        T(f"minecraft/tags/{soort}/leaves", [f"{HOUT}_bladeren", f"{HOUT}_bladeren_roze"])
        T(f"minecraft/tags/{soort}/saplings", [f"{HOUT}_zaailing"])
        T(f"minecraft/tags/{soort}/small_flowers", [f"zingbloemetje_{k}" for k in BLOEMEN])
        T(f"minecraft/tags/{soort}/stairs", ["zeegroen_steen_trap", "zeegroen_bakstenen_trap", "gepolijste_zeegroen_steen_trap"])
        T(f"minecraft/tags/{soort}/slabs", ["zeegroen_steen_plaat", "zeegroen_bakstenen_plaat", "gepolijste_zeegroen_steen_plaat"])
        T(f"minecraft/tags/{soort}/walls", ["zeegroen_steen_muur", "zeegroen_bakstenen_muur"])
        T(f"minecraft/tags/{soort}/sand", ["schildjeszand"])
    T("minecraft/tags/item/signs", [f"{HOUT}_bord"])
    T("minecraft/tags/item/hanging_signs", [f"{HOUT}_hangbord"])
    T("minecraft/tags/block/standing_signs", [f"{HOUT}_bord"])
    T("minecraft/tags/block/wall_signs", [f"{HOUT}_wandbord"])
    T("minecraft/tags/block/ceiling_hanging_signs", [f"{HOUT}_hangbord"])
    T("minecraft/tags/block/wall_hanging_signs", [f"{HOUT}_wandhangbord"])
    T("minecraft/tags/block/flowers", [f"zingbloemetje_{k}" for k in BLOEMEN])
    T("minecraft/tags/block/dirt", ["zalmklei", "zalmklei_gras", "zeegroen_gras"])
    T("minecraft/tags/block/animals_spawnable_on", ["zalmklei_gras", "zeegroen_gras", "schildjeszand"])
    T("minecraft/tags/block/replaceable_by_trees", ["weide_roze_gras", "weide_lichtroze_gras", "weide_hoog_roze_gras", "weide_hoog_lichtroze_gras", "wolkjes_sliert"])
    T("minecraft/tags/block/replaceable", ["weide_roze_gras", "weide_lichtroze_gras", "weide_hoog_roze_gras", "weide_hoog_lichtroze_gras"])
    T("minecraft/tags/block/sword_efficient", ["weide_roze_gras", "weide_lichtroze_gras", "weide_hoog_roze_gras", "weide_hoog_lichtroze_gras"])
    T("minecraft/tags/block/mineable/shovel", ["zalmklei", "zalmklei_gras", "zeegroen_gras", "schildjeszand", "zalmklei_streep_a", "zalmklei_streep_b"])
    T("minecraft/tags/block/mineable/pickaxe", ["zeegroen_steen", "zeegroen_bakstenen", "gepolijste_zeegroen_steen", *STEEN.keys(),
                                                 *[f"weidekristal_{k}" for k in KLEUREN], *[f"weidekristalletjes_{k}" for k in KLEUREN], "koorkom_podium"])
    T("minecraft/tags/block/mineable/axe", [*stammen, f"{HOUT}_planken", *WOOD.keys(), f"{HOUT}_bord", f"{HOUT}_wandbord", f"{HOUT}_hangbord",
                                             f"{HOUT}_wandhangbord"])
    T("minecraft/tags/block/mineable/hoe", [f"{HOUT}_bladeren", f"{HOUT}_bladeren_roze", "wolkjes_sliert"])
    T("minecraft/tags/item/stone_crafting_materials", ["zeegroen_steen"])
    T("minecraft/tags/item/stone_tool_materials", ["zeegroen_steen"])
    T(f"minecraft/tags/block/overworld_natural_logs", [f"{HOUT}_stam"])


# =====================================================================================================================
# the world: region, biome, surface, features, structures
# =====================================================================================================================
def _noise(name):
    return {"type": "minecraft:noise", "noise": f"guhs:{name}", "xz_scale": 1.0, "y_scale": 0.0}


def _spline(name, punten):
    return {"type": "minecraft:spline", "spline": {"coordinate": _noise(name), "points": [
        {"location": loc, "value": val, "derivative": 0.0} for loc, val in punten]}}


def _mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def _add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def term():
    """0 outside the Zingende Weide, 1 inside (from y KELDER_Y up), off the older regions and the biomes3 regions."""
    from features import knuffeldal_wereld as kw
    from features import verhaal_wereld as vw
    buiten = _mul(_mul(kw.geen_zee(), vw.geen_knuffel()), _mul(vw.geen_polder(), _mul(
        vw.geen_toendra(), _mul(_spline(vw.GUHWAII_NOISE, [(0.34, 1.0), (0.40, 0.0)]), _spline("bleekwoud", [(0.56, 1.0), (0.60, 0.0)])))))
    for soort in ("klaterdal", "bloesemmeertje", "wolkenweide"):         # never in a biomes3 region (they shift the climate too)
        buiten = _mul(buiten, _add(1.0, _mul(-1.0, bio_wereld._regio(soort))))
    vlak = {"type": "minecraft:cache_2d", "argument": _mul(_spline(NOISE, [(TERM[0], 0.0), (TERM[1], 1.0)]), buiten)}
    return _mul(vlak, {"type": "minecraft:y_clamped_gradient", "from_y": KELDER_Y[0], "to_y": KELDER_Y[1], "from_value": 0.0, "to_value": 1.0})


def _streepjes():
    """Striped cliffs: bands of salmon clay by height (two light, one dark, a stripe)."""
    banden = []
    for y in range(250, 28, -2):
        n = (y // 2) % 5
        blok = ["zalmklei", "zalmklei_streep_a", "zalmklei", "zalmklei_streep_b", "zalmklei_streep_a"][n]
        banden.append({"type": "minecraft:condition", "if_true": {"type": "minecraft:y_above", "anchor": {"absolute": y},
                                                                   "surface_depth_multiplier": 0, "add_stone_depth": False},
                       "then_run": {"type": "minecraft:block", "result_state": {"Name": f"guhs:{blok}"}}})
    return {"type": "minecraft:sequence", "sequence": banden}


def _deel(d):
    for i, a, b in ((n[0], n[3], n[4]) for n in DELEN):
        if i == d:
            return {"type": "minecraft:noise_threshold", "noise": f"guhs:{DEEL_NOISE}", "min_threshold": a, "max_threshold": b}
    raise KeyError(d)


def surface():
    blok = lambda n, props=None: {"type": "minecraft:block", "result_state": {"Name": f"guhs:{n}", **({"Properties": props} if props else {})}}
    cond = lambda c, r: {"type": "minecraft:condition", "if_true": c, "then_run": r}
    vloer = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
    onder = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True, "secondary_depth_range": 0, "surface_type": "floor"}
    diep = {"type": "minecraft:stone_depth", "offset": 8, "add_surface_depth": True, "secondary_depth_range": 4, "surface_type": "floor"}
    droog = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}
    steil = {"type": "minecraft:steep"}
    top = {"type": "minecraft:sequence", "sequence": [
        cond(steil, _streepjes()),
        cond(_deel("schildjeszand"), blok("schildjeszand")),
        cond(_deel("wolkjesbos"), blok("zeegroen_gras")),
        cond(_deel("wiegweide"), blok("zeegroen_gras")),
        blok("zalmklei_gras")]}
    return {"type": "minecraft:sequence", "sequence": [
        cond(vloer, cond(droog, top)),
        cond(vloer, blok("zalmklei")),                                  # (under water: plain clay)
        cond(onder, {"type": "minecraft:sequence", "sequence": [cond(_deel("schildjeszand"), blok("schildjeszand")), blok("zalmklei")]}),
        cond(diep, _streepjes())]}


FEATURES = {  # placed feature -> (Java feature type, placement)
    "weide_kloof": ("weide_kloof", [{"type": "minecraft:rarity_filter", "chance": 2}]),
    "weide_poeltje": ("weide_poeltje", [{"type": "minecraft:rarity_filter", "chance": 9}]),
    "weide_pilaar": ("weide_pilaar", [{"type": "minecraft:rarity_filter", "chance": 3}]),
    "weide_boog": ("weide_boog", [{"type": "minecraft:rarity_filter", "chance": 45}]),
    "weide_schildrots": ("weide_schildrots", [{"type": "minecraft:count", "count": 2}]),
    "weide_kristallen": ("weide_kristallen", [{"type": "minecraft:rarity_filter", "chance": 4}]),
    "weide_reuzenboom": ("weide_reuzenboom", [{"type": "minecraft:rarity_filter", "chance": 7}]),
    "weide_boompjes": ("weide_boompjes", [{"type": "minecraft:count", "count": 3}]),
    "weide_holletje": ("weide_holletje", [{"type": "minecraft:rarity_filter", "chance": 7}]),
    "weide_begroeiing": ("weide_begroeiing", [{"type": "minecraft:count", "count": 6}]),
}
STAP = {"weide_kloof": 2, "weide_poeltje": 2, "weide_pilaar": 4, "weide_boog": 4, "weide_schildrots": 4, "weide_kristallen": 4,
        "weide_reuzenboom": 9, "weide_boompjes": 9, "weide_holletje": 9, "weide_begroeiing": 9}
STRUCTUREN = {  # id -> (spacing, separation, salt)
    "weide_koorkom": (14, 6, 20180801),
    "weide_poeltjes": (12, 5, 20180802),
    "weide_guhwand": (28, 12, 20180803),
}


def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    w(f"{wg}/noise/{NOISE}.json", {"firstOctave": NOISE_OCTAVE, "amplitudes": [1.0]})
    w(f"{wg}/noise/{DEEL_NOISE}.json", {"firstOctave": -7, "amplitudes": [1.0, 0.5]})
    w(f"{wg}/density_function/{BIOME}_term.json", term())
    ref = f"guhs:{BIOME}_term"

    def router(d):
        r = d["noise_router"]
        for slot, teken in (("temperature", 1), ("vegetation", 1), ("erosion", -1)):
            if ref not in json.dumps(r[slot]):
                r[slot] = _add(r[slot], _mul(ref, SHIFT * teken))
    h.patch_json(f"{wg}/noise_settings/guhmension.json", router)

    def biome_source(d):
        alles = [-2.0, 2.0]
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] != f"guhs:{BIOME}"]
        entries.append({"biome": f"guhs:{BIOME}", "parameters": {"temperature": ENTRY[0], "humidity": ENTRY[1], "continentalness": alles,
                                                                 "erosion": ENTRY[2], "weirdness": alles, "depth": alles, "offset": 0.0}})
        d["generator"]["biome_source"]["biomes"] = entries
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    def surf(d):
        rules = [r for r in d["surface_rule"]["sequence"] if r.get("if_true", {}).get("biome_is") != [f"guhs:{BIOME}"]]
        rules.append({"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]}, "then_run": surface()})
        d["surface_rule"]["sequence"] = rules
    h.patch_json(f"{wg}/noise_settings/guhmension.json", surf)

    stappen = [[] for _ in range(11)]
    for naam, (soort, plaatsing) in FEATURES.items():
        w(f"{wg}/configured_feature/{naam}.json", {"type": f"guhs:{soort}", "config": {}})
        w(f"{wg}/placed_feature/{naam}.json", {"feature": f"guhs:{naam}", "placement": [
            *plaatsing, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
        stappen[STAP[naam]].append(f"guhs:{naam}")
    # the trees for the sapling (one small, the big one from four)
    w(f"{wg}/configured_feature/{HOUT}_boom.json", {"type": "guhs:weide_boompje", "config": {}})
    w(f"{wg}/configured_feature/{HOUT}_reuzenboom.json", {"type": "guhs:weide_reuzenboom", "config": {}})
    base = json.load(open(f"{wg}/biome/pink_puffs.json", encoding="utf-8"))
    stappen[6] = base["features"][6]
    leeg = {k: [] for k in base["spawners"]}
    w(f"{wg}/biome/{BIOME}.json", {
        "attributes": {
            "minecraft:audio/background_music": {"default": {"sound": "minecraft:music.overworld.cherry_grove", "min_delay": 6000, "max_delay": 14000},
                                                 "creative": {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000}},
            "minecraft:visual/ambient_particles": [{"particle": {"type": "guhs:trillertje"}, "probability": 0.0035}],
            "minecraft:visual/fog_color": "#f6d6dc", "minecraft:visual/sky_color": "#8fd8cc", "minecraft:visual/water_fog_color": "#6fc8c4",
            "neoforge:custom_skybox": f"guhs:{BIOME}"},
        "has_precipitation": True, "temperature": 0.7, "downfall": 0.5,
        "effects": {"water_color": "#7fe0d4", "foliage_color": "#f8c4d8", "grass_color": "#f4b4cc", "dry_foliage_color": "#e8b8c8"},
        "creature_spawn_probability": 0.12,
        "spawners": {**leeg, "creature": [{"type": "guhs:blubguh", "weight": 14, "minCount": 2, "maxCount": 4},
                                          {"type": "guhs:guh", "weight": 4, "minCount": 1, "maxCount": 2}]},
        "spawn_costs": {}, "carvers": [], "features": stappen})
    # structures: the biome tag, the structure (our own type), its set
    h.add_tag(f"guhs/tags/worldgen/biome/has_structure/{BIOME}", [f"guhs:{BIOME}"])
    for sid, (spacing, sep, salt) in STRUCTUREN.items():
        w(f"{wg}/structure/{sid}.json", {"type": "guhs:weide_bouw", "biomes": f"#guhs:has_structure/{BIOME}", "step": "surface_structures",
                                         "terrain_adaptation": "none", "spawn_overrides": {}, "soort": sid.removeprefix("weide_")})
        w(f"{wg}/structure_set/{sid}.json", {"structures": [{"structure": f"guhs:{sid}", "weight": 1}],
                                             "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": sep, "salt": salt}})
    ids = [f"guhs:{BIOME}"]
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({b: {"villager_type": "guhs:guh"} for b in ids}))
    h.add_tag("guhs/tags/worldgen/biome/has_structure/knabbelkelder", ids)
    h.add_tag("guhs/tags/worldgen/biome/band/favoriete_plekken", ids)


def particles(h):
    h.w(f"{h.A}/particles/trillertje.json", {"textures": ["guhs:trillertje"]})


def remove_tijdelijke_blub_spawn(h):
    p = os.path.join(h.R, "data", "guhs", "neoforge", "biome_modifier", "blubguh_tijdelijk.json")
    if os.path.exists(p):
        os.remove(p)


def badguh_textuur(h):
    """Badmeester Bubbels wears Badmeester Bubbel's model (WeideClient); his own skin is that texture in Bubbelsap colours:
    the swim cap, buoy and suit hue-shifted from red to bubbly lavender-blue, the pink guh fur left alone."""
    import colorsys
    bron = Image.open(f"{h.A}/textures/entity/npc_badmeesterguh.png").convert("RGBA")
    uit = bron.copy()
    px = uit.load()
    for y in range(uit.height):
        for x in range(uit.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            # strong reds/oranges (the kit) -> bubbelsap lavender-blue; the soft pink fur (low saturation) stays
            if ss > 0.45 and (hh < 0.09 or hh > 0.93):
                hh, ss, vv = 0.62, min(0.55, ss * 0.8), min(1.0, vv * 1.08)
                r, g, b = (int(c * 255) for c in colorsys.hsv_to_rgb(hh, ss, vv))
                px[x, y] = (r, g, b, a)
    uit.save(f"{h.A}/textures/entity/npc_bubbelbadguh.png")


def build(h):
    textures(h)
    badguh_textuur(h)
    blocks_and_items(h)
    loot_recipes_tags(h)
    worldgen(h)
    particles(h)
    remove_tijdelijke_blub_spawn(h)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    problems = []
    dim = json.load(open(f"{h.D}/dimension/guhmension.json", encoding="utf-8"))
    names = [e["biome"] for e in dim["generator"]["biome_source"]["biomes"]]
    if names.count(f"guhs:{BIOME}") != 1:
        problems.append(f"guhs:{BIOME} is not exactly once in the biome source")
    ns = json.load(open(f"{h.D}/worldgen/noise_settings/guhmension.json", encoding="utf-8"))
    for slot in ("temperature", "vegetation", "erosion"):
        if json.dumps(ns["noise_router"][slot]).count(f"guhs:{BIOME}_term") != 1:
            problems.append(f"{slot}: our term is missing or doubled")
    for n in list(WOOD) + list(STEEN):
        if not os.path.exists(f"{h.A}/blockstates/{n}.json"):
            problems.append(f"no blockstate for {n}")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    if problems:
        raise SystemExit("zingendeweide self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    """Standalone: only the Zingende Weide's own files (nl_nl gets the Dutch texts, en_us the English ones; the English also goes
    into tools/lang/en/c213_zingendeweide.json so a full make_resources run keeps it)."""
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c213_zingendeweide.json")
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
    print(f"zingendeweide: {len(TEKSTEN)} texts, {len(FEATURES)} features, {len(STRUCTUREN)} structures")


if __name__ == "__main__":
    _direct()
