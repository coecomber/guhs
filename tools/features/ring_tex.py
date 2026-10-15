"""
bbq2 (ring-kern) - the textures and block models that are not characters: the items (the Knabbelring, the three gifts, the
Stoofpotje, the Feestknabbel, the Vissenbotje, the two spawn eggs), the Elfentouwhaak and the Rustvuurtje, and the drawn
travel map of the Guhdex ("De reis van de Knabbelring", 256x160: the pixels of the haltes are the ones in RingFeature).
"""
from PIL import Image

from features import uvfix

# --- item icons (16x16) ------------------------------------------------------------------------------------------------
ICONS = {
    # a ring-shaped knabbel: golden, with cheese holes and a few glowing letters
    "knabbelring": ([
        "................", "................", ".....kkkkkk.....", "...kkGGGGGGkk...", "..kGGgGGGGrGGk..", "..kGGkkkkkkGGk..",
        ".kGrk......kGGk.", ".kGGk......kGgk.", ".kgGk......kGGk.", ".kGGk......krGk.", "..kGGkkkkkkGGk..", "..kGGGrGGGGgGk..",
        "...kkGGGGGGkk...", ".....kkkkkk.....", "................", "................"],
        {"k": (140, 86, 18), "G": (250, 196, 60), "g": (255, 236, 150), "r": (255, 120, 40)}),
    # a little flask with a star inside
    "lichtflesje": ([
        "................", ".......bb.......", "......bccb......", ".......kk.......", "......kwwk......", ".....kwWWwk.....",
        "....kwWYYWwk....", "...kwWYyyYWwk...", "...kwWYyyYWwk...", "....kwWYYWwk....", ".....kwWWwk.....", "......kwwk......",
        ".......kk.......", "................", "................", "................"],
        {"k": (90, 130, 170), "w": (190, 232, 250), "W": (226, 246, 255), "Y": (255, 250, 200), "y": (255, 255, 255), "b": (120, 90, 60),
         "c": (170, 130, 90)}),
    # a folded grey-green cloak with a leaf pin
    "elfenmanteltje": ([
        "................", "....kkkkkkkk....", "...kmmmmmmmmk...", "..kmMMmmmmMMmk..", "..kmMmmllmmMmk..", ".kmmMmlLLlmMmmk.",
        ".kmMMmmllmmMMmk.", ".kmMmmmmmmmmMmk.", ".kmMmmmmmmmmMmk.", ".kmMMmmmmmmMMmk.", "..kmMMmmmmMMmk..", "..kmmMMMMMMmmk..",
        "...kkmmmmmmkk...", ".....kkkkkk.....", "................", "................"],
        {"k": (60, 84, 70), "m": (126, 160, 134), "M": (98, 132, 108), "l": (90, 170, 90), "L": (200, 240, 190)}),
    # a coil of silver-grey rope
    "elfentouw": ([
        "................", "................", ".....kkkkkk.....", "...kkttTTttkk...", "..ktTTkkkkTTtk..", "..ktTk....kTtk..",
        ".ktTk......kTtk.", ".ktTk......kTtk.", ".ktTk......kTtk.", "..ktTk....kTtk..", "..ktTTkkkkTTtk..", "...kkttTTttkkk..",
        ".....kkkkkktTk..", "...........ktTk.", "............kk..", "................"],
        {"k": (110, 104, 96), "t": (206, 200, 184), "T": (236, 232, 220)}),
    "elfentouw_haak": ([
        "................", "................", "......kkkk......", ".....kiIIik.....", ".....kik.kik....", "......k..kik....",
        ".........kik....", "........kiik....", ".......kiik.....", ".......kik......", ".......kik......", ".....kkkikkk....",
        "....kiiiIiiik...", "....kkkkkkkkk...", "................", "................"],
        {"k": (70, 74, 84), "i": (170, 176, 188), "I": (220, 226, 236)}),
    # a bowl of stew
    "ring_stoofpotje": ([
        "................", "................", ".......s........", "......s.s.......", ".......s........", "................",
        "..kkkkkkkkkkkk..", ".kbBboBBoBbBBbk.", ".kwwwwwwwwwwwwk.", "..kwWWWWWWWWwk..", "..kwWWWWWWWWwk..", "...kwWWWWWWwk...",
        "....kkwwwwkk....", "......kkkk......", "................", "................"],
        {"k": (110, 76, 50), "w": (196, 150, 104), "W": (222, 180, 130), "b": (150, 96, 50), "B": (176, 116, 62), "o": (240, 140, 40),
         "s": (236, 236, 240)}),
    # a knabbel with a candle and sprinkles
    "ring_feestknabbel": ([
        "................", ".......f........", ".......y........", ".......c........", ".......c........", "....kkkckkkk....",
        "...kGGGGGGGGk...", "..kGrGGgGGbGGk..", "..kGGGGGGGGGGk..", ".kGGgGGrGGGGgGk.", ".kGGGGGGGbGGGGk.", ".kGbGGgGGGGrGGk.",
        "..kGGGGGGGGGGk..", "...kkkkkkkkkk...", "................", "................"],
        {"k": (150, 96, 20), "G": (250, 200, 70), "g": (255, 236, 150), "r": (240, 90, 120), "b": (90, 170, 240), "c": (250, 250, 250),
         "y": (255, 220, 80), "f": (255, 140, 40)}),
    # a gnawed fish bone on a string
    "ring_vissenbotje": ([
        "................", "..............s.", ".............s..", "............s...", "...........ks...", "..kk.....k.kk...",
        ".kwwk.k.kwkwk...", ".kwWwkwkwkwwk...", ".kwwWwwwwwwk....", ".kwWwkwkwkwwk...", ".kwwk.k.kwkwk...", "..kk.....k.kk...",
        "................", "................", "................", "................"],
        {"k": (120, 120, 112), "w": (236, 236, 226), "W": (40, 40, 48), "s": (150, 110, 70)}),
}
EI = ["................", "......kkkk......", "....kkaaaakk....", "...kaaabaaaak...", "..kaaaaaabaaak..", "..kabaaaaaaaak..",
      ".kaaaaabaaaabak.", ".kaaaaaaaaaaaak.", ".kaabaaaaabaaak.", ".kaaaaaaaaaaaak.", ".kaaaabaaaaabak.", "..kaaaaaabaaak..",
      "..kkaabaaaaakk..", "....kkaaaakk....", "......kkkk......", "................"]
EIEREN = {"smikagol_spawn_egg": ((150, 164, 132), (150, 214, 240)), "knekel_ruiter_spawn_egg": ((38, 34, 44), (236, 90, 40))}


def items(h):
    for name, (rows, palet) in ICONS.items():
        h.save(h.grid(rows, {k: (*v, 255) for k, v in palet.items()}), "item", f"{name}.png")
        h.item_model(name)
    for name, (basis, vlek) in EIEREN.items():
        donker = tuple(int(c * 0.55) for c in basis)
        h.save(h.grid(EI, {"k": (*donker, 255), "a": (*basis, 255), "b": (*vlek, 255)}), "item", f"{name}.png")
        h.item_model(name)


# --- the blocks ----------------------------------------------------------------------------------------------------------
def _el(frm, to, tex, faces=None, rot=None, uv=None):
    e = {"from": frm, "to": to, "faces": {f: ({"texture": tex, "uv": uv} if uv else {"texture": tex})
                                          for f in (faces or ("down", "up", "north", "south", "west", "east"))}}
    if rot:
        e["rotation"] = rot
    return e


def haak(h):
    """The Elfentouwhaak (drawn standing on a floor; the blockstate turns it like an end rod): a little plate and an iron hook."""
    i, d = "#ijzer", "#donker"
    elements = [_el([5, 0, 5], [11, 1, 11], d), _el([6, 1, 6], [10, 1.6, 10], i), _el([7.4, 1.6, 7.4], [8.6, 6.4, 8.6], i),
                _el([7.4, 6.4, 7.4], [8.6, 7.6, 11], i), _el([7.4, 3.6, 9.8], [8.6, 6.4, 11], i), _el([7.4, 3.0, 9.0], [8.6, 3.9, 10.4], i)]
    h.w(f"{h.A}/models/block/elfentouw_haak.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "minecraft:block/iron_block", "ijzer": "minecraft:block/iron_block", "donker": "guhs:block/gepolijst_roosterijzer"},
        "elements": uvfix.binnen(elements)})
    h.w(f"{h.A}/blockstates/elfentouw_haak.json", {"variants": {
        "facing=up": {"model": "guhs:block/elfentouw_haak"}, "facing=down": {"model": "guhs:block/elfentouw_haak", "x": 180},
        "facing=north": {"model": "guhs:block/elfentouw_haak", "x": 90}, "facing=south": {"model": "guhs:block/elfentouw_haak", "x": 90, "y": 180},
        "facing=west": {"model": "guhs:block/elfentouw_haak", "x": 90, "y": 270}, "facing=east": {"model": "guhs:block/elfentouw_haak", "x": 90, "y": 90}}})
    h.item_model("elfentouw_haak")
    h.self_drop("elfentouw_haak")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:elfentouw_haak"])


STOOF = ["kkkkkkkkkkkkkkkk", "kbbBbbbbBbbbbbbk", "kbBBbobbbbbBbbbk", "kbbbbbbbgbbbbobk", "kbobbbBbbbbbbbbk", "kbbbbBBbbbobbBbk",
         "kbbgbbbbbbbbbBbk", "kbbbbbbobbbgbbbk", "kbBbbbbbbbBbbbbk", "kbBBbobbbbBBbbbk", "kbbbbbbbgbbbbobk", "kbbbobbbbbbbbbbk",
         "kbbbbbbBbbbobbbk", "kbgbbbBBbbbbbbbk", "kbbbbbbbbbbbgbbk", "kkkkkkkkkkkkkkkk"]


def rustvuur(h):
    """The Rustvuurtje: a ring of stones, two crossed saté logs on glowing coals, flames, and Sam-guh's pot on a spit over it."""
    h.save(h.grid(STOOF, {"k": (60, 62, 70, 255), "b": (150, 96, 50, 255), "B": (176, 120, 66, 255), "o": (240, 140, 40, 255), "g": (120, 170, 80, 255)}),
           "block", "ring_rustvuur_stoof.png")
    s, l, k, v, p, st, ij = "#steen", "#stam", "#kool", "#vuur", "#pot", "#stoof", "#ijzer"
    elements = [_el([4.5, 0, 4.5], [11.5, 1, 11.5], k)]
    for x, z in ((6.5, 1), (11, 2.5), (12.5, 6.5), (11, 11), (6.5, 12.5), (2.5, 11), (1, 6.5), (2.5, 2.5)):
        elements.append(_el([x, 0, z], [x + 2.5, 2 if (x + z) % 2 else 1.6, z + 2.5], s))
    elements += [_el([3.5, 0.6, 7], [12.5, 2.4, 9], l), _el([7, 1.0, 3.5], [9, 2.8, 12.5], l)]
    for hoek in (45, -45):
        elements.append(_el([2.5, 1, 8], [13.5, 8.5, 8], v, faces=("north", "south"),
                            rot={"origin": [8, 8, 8], "axis": "y", "angle": hoek, "rescale": True}, uv=[0, 5, 16, 16]))
    # the spit: two forked posts, a bar, a chain, the pot with the stew
    elements += [_el([0.6, 0, 7.4], [1.8, 12.6, 8.6], ij), _el([14.2, 0, 7.4], [15.4, 12.6, 8.6], ij), _el([0.6, 12.2, 7.6], [15.4, 13.2, 8.4], ij),
                 _el([7.6, 10.2, 7.6], [8.4, 12.2, 8.4], ij),
                 _el([5, 6.2, 5], [11, 10.2, 11], p, faces=("down", "north", "south", "west", "east")), _el([5, 6.2, 5], [11, 10.0, 11], st, faces=("up",)),
                 _el([4.6, 9.6, 4.6], [11.4, 10.4, 5.2], ij), _el([4.6, 9.6, 10.8], [11.4, 10.4, 11.4], ij),
                 _el([4.6, 9.6, 5.2], [5.2, 10.4, 10.8], ij), _el([10.8, 9.6, 5.2], [11.4, 10.4, 10.8], ij)]
    h.w(f"{h.A}/models/block/ring_rustvuur.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": "guhs:block/sate_stam", "steen": "guhs:block/houtskoolsteen_stenen", "stam": "guhs:block/sate_stam", "kool": "guhs:block/gloeikool",
        "vuur": "minecraft:block/campfire_fire", "pot": "guhs:block/gepolijst_roosterijzer", "stoof": "guhs:block/ring_rustvuur_stoof",
        "ijzer": "guhs:block/roosterijzer_pilaar"}, "elements": uvfix.binnen(elements),
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.62, 0.62, 0.62]},
                    "ground": {"translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]}, "fixed": {"scale": [0.5, 0.5, 0.5]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.36, 0.36, 0.36]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]}}})
    h.w(f"{h.A}/blockstates/ring_rustvuur.json", {"variants": {"": {"model": "guhs:block/ring_rustvuur"}}})
    h.w(f"{h.A}/models/item/ring_rustvuur.json", {"parent": "guhs:block/ring_rustvuur"})
    h.self_drop("ring_rustvuur")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:ring_rustvuur"])


# --- the travel map ------------------------------------------------------------------------------------------------------
# the pixels of the haltes (RingFeature: the Reiskaart): chapter -> (x, y)
HALTES = {"ring_h1": (28, 118), "ring_h2": (70, 84), "ring_h3": (104, 56), "ring_h4": (140, 92), "ring_h5": (184, 66), "ring_h6": (224, 46),
          "ring_sausuman": (196, 124)}


def reiskaart():
    """The map: the green Gouw in the Guhmensie on the left, the grill portal, and the dark land of the Barbecuether with the
    elf house in the forest, the mountains over the mine, the tree city on the sauce river, the black gate, the volcano and
    the wizard's tower."""
    from features import verhaal_motor
    k = verhaal_motor.Kaart(seed=2130150)
    # the Guhmensie: soft pink-green hills
    k.land([(4, 156), (4, 96), (16, 88), (34, 92), (48, 104), (54, 124), (50, 156)], (222, 214, 160, 255))
    k.bos(8, 96, 14, 12, 4)
    for x, y in ((20, 126), (30, 132), (38, 122)):
        k.huisje(x, y, (150, 190, 110, 255))                      # hill holes
    k.tekst(8, 144, "Guhmensie")
    # the Barbecuether: charred land, the sauce river through it
    k.land([(58, 152), (56, 100), (62, 62), (84, 34), (128, 20), (180, 16), (236, 22), (250, 52), (250, 132), (228, 152), (150, 156)],
           (176, 150, 132, 255))
    k.water([(230, 150), (250, 134), (250, 156), (232, 156)], (232, 150, 60, 255))
    k.rivier([(118, 152), (128, 120), (146, 100), (160, 104), (172, 126), (176, 152)], (232, 160, 70, 255), 3)
    k.bos(60, 66, 30, 26, 9, (150, 96, 70, 255))                  # the Worstenwoud around Guhvendel
    k.huisje(70, 82, (236, 226, 200, 255))
    for x, y, hoog in ((90, 52, 16), (104, 46, 20), (118, 52, 15), (98, 62, 10)):
        k.berg(x, y, hoog, (112, 104, 110, 255))                  # the mountains over Knabbelmoria
    k.bos(130, 78, 24, 20, 7, (140, 150, 80, 255))                # the tree city in the Satebos
    k.boom(140, 88, (190, 170, 70, 255))
    k.toren(184, 62, 16, (40, 36, 44, 255))                       # the Zwarte Roosterpoort and the tower of the Eye
    k.toren(178, 66, 9, (60, 54, 62, 255))
    k.toren(190, 66, 9, (60, 54, 62, 255))
    k.vulkaan(224, 44, 24)                                        # the Frituurberg
    k.toren(196, 120, 14, (30, 28, 34, 255))                      # the Toren van Sausuman
    k.tekst(150, 142, "Guhbarbecuether")
    # the road, from halte to halte, and the portal between the two worlds
    k.pad([HALTES["ring_h1"], (46, 108), (56, 96), HALTES["ring_h2"], HALTES["ring_h3"], HALTES["ring_h4"], HALTES["ring_h5"], HALTES["ring_h6"]])
    k.d.rectangle((51, 92, 59, 104), outline=(60, 40, 30, 255), fill=(250, 150, 50, 255))
    k.d.rectangle((53, 94, 57, 102), fill=(255, 214, 110, 255))
    for x, y in HALTES.values():
        k.d.ellipse((x - 3, y - 3, x + 3, y + 3), outline=(74, 50, 32, 255), fill=(246, 232, 190, 255))
    k.kompasroos(236, 140)
    return k


def build(h):
    items(h)
    haak(h)
    rustvuur(h)
