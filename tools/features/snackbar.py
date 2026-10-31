"""
1.8.1: De Guhsnackbar. Java: feature/snackbar (SnackbarFeature, SnackbarGuh, SnackbarBescherming, SnackbarBordBlock, SnackbarParasolBlock).

  build(h)    the structure guhs:snackbar (De Patatzak, tools/features/snackbar_bouw.py), the wide signs (the tile block
              snackbar_bord: one texture per sign from tools/features/snackbar_borden, a model per tile), the parasol, the
              Snackbarkompas (recoloured frames of the Heiligdomkompas), the Snackbar-guh's skin, the chest loot and the texts
              (Dutch in nl_nl, English in tools/lang/en/c218_snackbar.json)
  ftb(fq)     a section in the Knuffeldal chapter: find the building, get the compass

The signs are textures, so they read Dutch in both languages.
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/snackbar.py
"""
import colorsys
import json
import os
import shutil
import sys

from PIL import Image

FTB_CHAPTER = "guhs_knuffeldal"
FTB_SECTION = "De Guhsnackbar"

NAAM = "snackbar"
BORDEN_MAP = os.path.join("tools", "features", "snackbar_borden")
BORDEN = [("menu", 11, 4), ("naam", 17, 4), ("snackbar", 10, 2)]      # name, width, height (the order numbers the tiles)
PRIJZEN = {"patatje_vads": 24, "patatje_oorlog": 24, "bitterguhllen": 32, "kaassoufflguh": 32, "kaaskroket": 32,
           "patatje_guhspeciaal": 40, "frikandel_guhspeciaal": 48, "vetste_hap": 256}    # same as SnackbarGuh.PRIJZEN
RECEPTNAAM = {"patatje_vads": ("Patatje Vads", "Chonky Fries"), "patatje_oorlog": ("Patatje Oorlog-maar-lief", "War-but-Sweet Fries"),
              "bitterguhllen": ("Bitterguhllen", "Bitterguhllen"), "kaassoufflguh": ("Kaassoufflguh", "Cheese Soufflguh"),
              "kaaskroket": ("Kaaskroket", "Cheese Croquette"), "patatje_guhspeciaal": ("Patatje Guhspeciaal", "Guh Special Fries"),
              "frikandel_guhspeciaal": ("Frikandel Guhspeciaal", "Guh Special Frikandel"), "vetste_hap": ("De Vetste Hap", "The Chonkiest Bite")}
T = "gui.guhs.snackbar."


def teksten():
    t = {
        "structure.guhs.snackbar": ("De Guhsnackbar", "The Guhsnackbar"),
        "structure.guhs.snackbar.tooltip": ("De Patatzak: snacks bestellen en frituurrecepten leren", "The Fry Cone: order snacks and learn frying recipes"),
        "entity.guhs.guh_npc.snackbarguh": ("Snackbar-guh", "Snackbar Guh"),
        "entity.guhs.reisguh.plek.guhsnackbar": ("Guhsnackbar", "Guhsnackbar"),
        "item.guhs.snackbarkompas": ("Snackbarkompas", "Snackbar Compass"),
        "item.guhs.snackbarkompas.lore": ("Wijst naar de dichtstbijzijnde Guhsnackbar in de Guhmensie. Te koop bij de vadstemmer.",
                                          "Points to the nearest Guhsnackbar in the Guhmension. Sold by the vads tamer."),
        "block.guhs.snackbar_bord": ("Snackbarbord", "Snackbar Sign"),
        "block.guhs.snackbar_parasol": ("Snackbarparasol", "Snackbar Parasol"),
        "item.guhs.snackbar_parasol": ("Snackbarparasol", "Snackbar Parasol"),
        T + "no_build": ("Njeg! Van de Patatzak blijf je af: hier mag je niks slopen of bouwen. Bestel lekker een snackje!",
                         "Njeg! Hands off the Fry Cone: no breaking or building here. Order a nice snack instead!"),
        T + "guh.hallo": ("NJEG! Welkom bij De Patatzak! Wat mag het zijn, vadsje? Alles vers uit het vet!",
                          "NJEG! Welcome to the Fry Cone! What will it be, little chonk? Everything fresh from the fat!"),
        T + "guh.optie.bestellen": ("Ik wil bestellen", "I'd like to order"),
        T + "guh.optie.recepten": ("Leer me een recept", "Teach me a recipe"),
        T + "guh.optie.uitleg": ("Hoe werkt dat, zo'n recept?", "How does a recipe work?"),
        T + "guh.optie.terug": ("Iets anders", "Something else"),
        T + "guh.optie.volgende": ("Meer recepten", "More recipes"),
        T + "guh.optie.ander": ("Een ander recept", "Another recipe"),
        T + "guh.optie.doei": ("Doei!", "Bye!"),
        T + "guh.uitleg": ("Betaal me in Kaas Knabbels en je kent het recept voor altijd. Daarna frituur je het gewoon thuis in je eigen frituurpan, je hoeft nooit meer terug te komen. Maar kom gerust, hoor, vads!",
                           "Pay me in Cheese Nibbles and you know the recipe forever. After that you just fry it at home in your own frying pan, you never have to come back. But do come by, chonk!"),
        T + "guh.recepten": ("Welk recept zal ik je leren? Betalen doe je in Kaas Knabbels.", "Which recipe shall I teach you? You pay in Cheese Nibbles."),
        T + "guh.alles": ("Je kent ze allemaal, vahoeg! Ik heb je niks meer te leren. Jij zou hier achter de toonbank moeten staan!",
                          "You know them all, wahoog! I have nothing left to teach you. You should be behind this counter!"),
        T + "guh.te_duur": ("Njeg, dat recept kost %s Kaas Knabbels en die heb je niet bij je. Kom straks terug!",
                            "Njeg, that recipe costs %s Cheese Nibbles and you don't have them on you. Come back later!"),
        T + "guh.geleerd": ("Je kent nu het recept van %s! Frituur het maar thuis in je pan.", "You now know the recipe for %s! Fry it at home in your pan."),
    }
    for id_, (nl, en) in RECEPTNAAM.items():
        t[f"{T}recept.{id_}"] = (f"{nl} ({PRIJZEN[id_]} knabbels)", f"{en} ({PRIJZEN[id_]} nibbles)")
    return t


def eerste_tegels():
    uit, n = {}, 0
    for naam, b, h_ in BORDEN:
        uit[naam] = n
        n += b * h_
    return uit, n


def borden(h):
    """Every tile a model with its piece of the sign's texture, a blockstate for every direction."""
    eerste, totaal = eerste_tegels()
    assert totaal == 132, totaal
    variants = {}
    for naam, b, ho in BORDEN:
        tex = f"snackbar_bord_{naam}"
        h.save(Image.open(os.path.join(BORDEN_MAP, naam + ".png")).convert("RGBA"), "block", tex + ".png")
        for rij in range(ho):
            for kol in range(b):
                nr = eerste[naam] + rij * b + kol
                u0, u1 = 16 * kol / b, 16 * (kol + 1) / b
                v0, v1 = 16 * rij / ho, 16 * (rij + 1) / ho
                vlak = {"uv": [u0, v0, u1, v1], "texture": "#t"}
                h.w(f"{h.A}/models/block/snackbar_bord_{nr}.json", {
                    "textures": {"t": f"guhs:block/{tex}", "particle": f"guhs:block/{tex}"},
                    "elements": [{"from": [0, 0, 0], "to": [16, 16, 1], "faces": {"south": vlak, "north": vlak}}]})
                for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
                    v = {"model": f"guhs:block/snackbar_bord_{nr}"}
                    if y:
                        v["y"] = y
                    variants[f"facing={facing},tegel={nr}"] = v
    h.w(f"{h.A}/blockstates/snackbar_bord.json", {"variants": variants})


def parasol(h):
    for naam, kleur in (("roze", (232, 92, 132)), ("geel", (250, 214, 90))):
        img = Image.new("RGBA", (16, 16), (255, 250, 240, 255))
        for x in range(16):
            if x // 4 % 2:
                for y in range(16):
                    img.putpixel((x, y), kleur + (255,))
        h.save(img, "block", f"snackbar_parasol_{naam}.png")
        doek = {"uv": [0, 0, 16, 16], "texture": "#d"}
        stok = {"uv": [0, 0, 2, 16], "texture": "#s"}
        h.w(f"{h.A}/models/block/snackbar_parasol_{naam}.json", {
            "textures": {"d": f"guhs:block/snackbar_parasol_{naam}", "s": "minecraft:block/birch_planks", "particle": f"guhs:block/snackbar_parasol_{naam}"},
            "elements": [
                {"from": [7, 0, 7], "to": [9, 28, 9], "faces": {f: stok for f in ("north", "south", "east", "west")}},
                {"from": [-8, 26, -8], "to": [24, 28, 24], "faces": {f: doek for f in ("north", "south", "east", "west", "up", "down")}},
                {"from": [-2, 28, -2], "to": [18, 30, 18], "faces": {f: doek for f in ("north", "south", "east", "west", "up", "down")}},
                {"from": [3, 30, 3], "to": [13, 32, 13], "faces": {f: doek for f in ("north", "south", "east", "west", "up", "down")}}]})
    h.w(f"{h.A}/blockstates/snackbar_parasol.json", {"variants": {
        "geel=false": {"model": "guhs:block/snackbar_parasol_roze"}, "geel=true": {"model": "guhs:block/snackbar_parasol_geel"}}})
    h.w(f"{h.A}/models/item/snackbar_parasol.json", {"parent": "guhs:block/snackbar_parasol_roze"})
    h.w(f"{h.D}/loot_table/blocks/snackbar_parasol.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:snackbar_parasol"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def kompas(h):
    """The frames of the Heiligdomkompas, the case turned fry yellow."""
    bron = os.path.join(h.A, "textures", "item")
    for i in range(32):
        img = Image.open(os.path.join(bron, f"heiligdom_kompas_{i:02d}.png")).convert("RGBA")
        for x in range(img.width):
            for y in range(img.height):
                r, g, b, a = img.getpixel((x, y))
                if a:
                    tint, licht, verz = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
                    if verz > 0.15:
                        r2, g2, b2 = colorsys.hls_to_rgb(0.13, licht, min(1.0, verz + .1))
                        img.putpixel((x, y), (int(r2 * 255), int(g2 * 255), int(b2 * 255), a))
        n = f"snackbarkompas_{i:02d}"
        h.save(img, "item", n + ".png")
        h.w(f"{h.A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{n}"}})


def loot(h):
    def e(item, gewicht, lo, hi):
        return {"type": "minecraft:item", "name": item, "weight": gewicht,
                "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    h.w(f"{h.D}/loot_table/chests/snackbar.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
        e("guhs:kaas_knabbels", 6, 8, 24), e("guhs:gefrituurde_kaasknabbels", 4, 4, 12), e("guhs:mika_vet", 3, 2, 6),
        e("guhs:patatje_kaassaus", 3, 1, 3), e("minecraft:potato", 4, 4, 12)]}]})


def gebouw(h):
    from features import snackbar_bouw as B
    eerste, _ = eerste_tegels()
    s = B.bouw(h, eerste)
    fout = B.check(s)
    if fout:
        raise SystemExit("snackbar check failed:\n  " + "\n  ".join(fout))
    s.save(NAAM)
    h.TEMPLATE_SIZES[NAAM] = 26      # the flatness check reaches over the cone and the terrace
    h.FLATNESS[NAAM] = 8             # no half mountains: rough ground gets no snackbar
    h.structure(NAAM, h.GUHMENSION_LAND, spacing=32, separation=12, salt=20261031, start_y=-B.G, reach=60, centre="guhs:snackbar_midden",
                spawn_overrides={"monster": {"bounding_box": "piece", "spawns": []}})
    print(f"snackbar: De Patatzak ok ({len(s.blocks)} blocks)")


def build(h):
    gebouw(h)
    borden(h)
    parasol(h)
    kompas(h)
    loot(h)
    bron = os.path.join(h.A, "textures", "entity", "npc_grillguh.png")
    if os.path.exists(bron):
        shutil.copyfile(bron, os.path.join(h.A, "textures", "entity", "npc_snackbarguh.png"))
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def ftb(fq):
    q = fq.q
    q("snackbar_kompas", "Snackbarkompas", "Koop bij de &dvadstemmer&r een Snackbarkompas: het wijst naar de dichtstbijzijnde Guhsnackbar.",
      "guhs:snackbarkompas", [fq.item("guhs:snackbarkompas")], rewards=(("guhs:kaas_knabbels", 8),), x=0, y=0, xp=100)
    q("snackbar_vinden", "De Patatzak", "Vind &6De Guhsnackbar&r, de reuzenpuntzak in de Guhmensie. De &dSnackbar-guh&r verkoopt snacks en leert je recepten.",
      "guhs:patatje_kaassaus", [fq.structure("snackbar")], rewards=(("guhs:kaas_knabbels", 16),), x=1.5, y=0, xp=150)


FTB_EN = {
    "snackbar_kompas": ("Snackbar Compass", "Buy a Snackbar Compass from the &dvads tamer&r: it points to the nearest Guhsnackbar."),
    "snackbar_vinden": ("The Fry Cone", "Find &6The Guhsnackbar&r, the giant cone of fries in the Guhmension. The &dSnackbar Guh&r sells snacks and teaches recipes."),
}


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    T_ = teksten()
    ftb_en = {"ftb.guhs_knuffeldal.section.extra_snackbar": "The Guhsnackbar"}
    for key, (titel, tekst) in FTB_EN.items():
        ftb_en[f"ftb.guhs_knuffeldal.q.{key}.title"] = titel
        ftb_en[f"ftb.guhs_knuffeldal.q.{key}.desc"] = tekst
    with open(os.path.join("tools", "lang", "en", "c218_snackbar_ftb.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(ftb_en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    with open(os.path.join("tools", "lang", "en", "c218_snackbar.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in T_.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as fh:
            d = json.load(fh)
        d.update({k: v[i] for k, v in T_.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(d, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    with open(os.path.join(a, "heiligdom_kompas.json"), encoding="utf-8") as f:
        k = f.read().replace("heiligdom_kompas", "snackbarkompas")
    with open(os.path.join(a, "snackbarkompas.json"), "w", encoding="utf-8", newline="\n") as f:
        f.write(k)
    with open(os.path.join(a, "snackbar_parasol.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "guhs:block/snackbar_parasol_roze"}}, f, indent=2)
        f.write("\n")
    print(f"snackbar: {len(T_)} texts")


if __name__ == "__main__":
    _direct()
