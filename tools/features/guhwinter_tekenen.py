"""
Guhs 1.10.9 "Guhwinter", slice TEKENEN (Java: feature/guhwinter/tekenen): the drawing editor, the hangable 2x2 drawing, the
kerstboom drawings and the Kerstalbum.

  - textures: item/guhwinter_tekening (a little framed doodle), item/guhwinter_kerstalbum (a red book with a tree),
    entity/guhwinter_tekening_leeg (the empty 72x72 frame shown while a drawing is on its way; the SAME frame as
    client/TekeningCache.lijst in Java)
  - item models + item definitions
  - lang: Dutch into nl_nl, the English into the overlay tools/lang/en/c480_guhwinter_tekenen.json (+ its chunk)
  - the blokuitleg of the drawing and the album (tools/wiki_site/blokuitleg.py, between markers)

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/guhwinter_tekenen.py
"""
import json
import os
import re
import sys

from PIL import Image

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
TEX = os.path.join(A, "textures")
K = "guhwinter.tekenen."
CHUNK = "c480_guhwinter_tekenen"

# =====================================================================================================================
# texts: key -> (Dutch, English)
# =====================================================================================================================
TEKSTEN = {
    "item.guhs.guhwinter_tekening": ("Tekening", "Drawing"),
    "item.guhs.guhwinter_tekening.van": ("Tekening van %s", "Drawing by %s"),
    "item.guhs.guhwinter_tekening.lore": ("Hang hem aan een muur: altijd 2x2, net een schilderij. Slaan = weer van de muur.",
                                          "Hang it on a wall: always 2x2, just like a painting. Hit it to take it down again."),
    "item.guhs.guhwinter_tekening.leeg": ("Een leeg vel. Er staat (nog) niks op, njeg.", "A blank sheet. Nothing on it (yet), nyeg."),
    "item.guhs.guhwinter_kerstalbum": ("Kerstalbum", "Christmas Album"),
    "item.guhs.guhwinter_kerstalbum.lore": ("Alle tekeningen uit de Guhwinter-kerstboom. Rechtsklik om erin te bladeren.",
                                            "All the drawings from the Guhwinter Christmas tree. Right-click to browse."),
    "entity.guhs.guhwinter_tekening": ("Tekening", "Drawing"),
    "entity.guhs.guhwinter_boomkaartje": ("Kerstboomkaartje", "Christmas Tree Card"),
    # the editor
    K + "editor.titel.kerstboom": ("Tekenen voor de kerstboom", "Drawing for the Christmas tree"),
    K + "editor.titel.lootje": ("Tekenen voor je lootje", "Drawing for the name you drew"),
    K + "editor.opslaan": ("Opslaan", "Save"),
    K + "editor.annuleren": ("Annuleren", "Cancel"),
    K + "editor.ja": ("Ja", "Yes"),
    K + "editor.nee": ("Nee", "No"),
    K + "editor.wissen": ("Wissen", "Clear"),
    K + "editor.wissen_zeker": ("Alles wissen?", "Clear everything?"),
    K + "editor.weg_zeker": ("Weggooien zonder opslaan?", "Throw away without saving?"),
    K + "editor.bezig": ("Even opslaan... njeg", "Saving... nyeg"),
    K + "editor.uitleg": ("Linksklik tekent, rechtsklik gumt. Ctrl+Z maakt ongedaan.", "Left-click draws, right-click erases. Ctrl+Z undoes."),
    K + "editor.kleurtjes": ("Kleurtjes", "Colours"),
    K + "editor.recent": ("Net gebruikt", "Just used"),
    K + "editor.tool.potlood": ("Potlood (P)", "Pencil (P)"),
    K + "editor.tool.gum": ("Gum (G)", "Eraser (G)"),
    K + "editor.tool.emmer": ("Verfemmer: vul een vlak (E)", "Paint bucket: fill an area (E)"),
    K + "editor.tool.pipet": ("Pipet: pak een kleur uit je tekening (I)", "Eyedropper: pick a colour from your drawing (I)"),
    K + "editor.tool.lijn": ("Lijn: klik, sleep en laat los (L)", "Line: click, drag and let go (L)"),
    K + "editor.tool.raster": ("Hulplijntjes aan of uit", "Grid on or off"),
    K + "editor.tool.maat": ("Dikte %s (toets 1, 2 of 4)", "Size %s (key 1, 2 or 4)"),
    K + "editor.tool.ongedaan": ("Ongedaan maken (Ctrl+Z): nog %s stappen", "Undo (Ctrl+Z): %s steps left"),
    K + "editor.tool.opnieuw": ("Opnieuw (Ctrl+Y): %s stappen", "Redo (Ctrl+Y): %s steps"),
    # saving
    K + "fout.sessie": ("Dit tekenvel is verlopen. Open het opnieuw, njeg.", "This drawing sheet has expired. Open it again, nyeg."),
    K + "fout.rustig": ("Rustig aan, vads! Even wachten en dan nog eens opslaan.", "Easy there, vads! Wait a moment and save again."),
    K + "fout.kapot": ("Er ging iets mis met je tekening. Probeer het nog eens.", "Something went wrong with your drawing. Try again."),
    K + "fout.geen_antwoord": ("Geen antwoord van de server. Probeer nog eens op te slaan.", "No answer from the server. Try saving again."),
    K + "past_niet": ("Hier past je tekening niet: hij is 2 bij 2 blokken.", "Your drawing doesn't fit here: it is 2 by 2 blocks."),
    # the kerstboom
    K + "kerstboom.dicht": ("De kerstboom-tekeningen zijn al uitgedeeld. Kijk in je Kerstalbum!",
                            "The Christmas tree drawings have already been handed out. Look in your Christmas Album!"),
    K + "kerstboom.opgehangen": ("Je tekening hangt in de kerstboom! Je mag hem tot en met 25 december nog aanpassen. Vahoeg!",
                                 "Your drawing hangs in the Christmas tree! You can still change it until 25 December. Wahoog!"),
    K + "kerstboom.bijgewerkt": ("Je kerstboom-tekening is bijgewerkt. Njeg, nog mooier!", "Your Christmas tree drawing has been updated. Nyeg, even prettier!"),
    # the album
    K + "album.gekregen": ("Kijk eens in je tas: een Kerstalbum met alle tekeningen uit de kerstboom! Rechtsklik om erin te bladeren.",
                           "Look in your bag: a Christmas Album with all the drawings from the Christmas tree! Right-click to browse it."),
    K + "album.gepakt": ("Je hebt de tekening van %s om op te hangen. Vahoeg!", "You got the drawing by %s to hang up. Wahoog!"),
    K + "album.pakken": ("Ophangen-versie pakken", "Take a copy to hang"),
    K + "album.al_gepakt": ("Al gepakt", "Already taken"),
    K + "album.leeg": ("Er hangen nog geen tekeningen in de kerstboom. Njeg...", "There are no drawings in the Christmas tree yet. Nyeg..."),
    K + "album.van": ("Tekening van %s", "Drawing by %s"),
    K + "album.pagina": ("Bladzijde %s van %s", "Page %s of %s"),
    K + "album.uitleg": ("Pak een versie om op te hangen: altijd 2x2, net een schilderij. Dat kan één keer per tekening.",
                         "Take a copy to hang up: always 2x2, just like a painting. Once per drawing."),
    K + "album.uitleg_gepakt": ("Deze heb je al gepakt. Hangt hij al ergens?", "You already took this one. Is it hanging somewhere yet?"),
}

UITLEG = {
    "guhwinter_tekening": (
        "Een tekening van 64 bij 64 pixels die iemand zelf maakte in de teken-interface van Guhwinter: voor de kerstboom op het "
        "Winterplein of als cadeautje voor een lootje. De naam van de maker staat erop.\n\nRechtsklik ermee op een muur: hij hangt "
        "altijd als schilderij van 2 bij 2 blokken, met de echte tekening in een houten lijstje met roze hoekjes. Sla erop en hij "
        "komt weer van de muur, als item. Hij smelt niet: je mag hem voor altijd houden.",
        "A drawing of 64 by 64 pixels that someone made themselves in the Guhwinter drawing screen: for the Christmas tree on the "
        "Winter Square, or as a present for the name they drew. The maker's name is on it.\n\nRight-click a wall with it: it always "
        "hangs as a 2 by 2 painting, showing the real drawing in a little wooden frame with pink corners. Hit it and it comes down "
        "again as an item. It never melts: you can keep it forever."),
    "guhwinter_kerstalbum": (
        "Iedereen die tussen 26 december en 2 januari inlogt krijgt er één. Rechtsklik om te bladeren: op elke bladzijde staat een "
        "tekening uit de kerstboom met de naam van de maker.\n\nMet \"Ophangen-versie pakken\" krijg je die tekening als schilderij "
        "van 2x2 om op te hangen, één keer per tekening. Het album zelf blijft voor altijd.",
        "Everyone who logs in between 26 December and 2 January gets one. Right-click to browse: every page shows a drawing from "
        "the Christmas tree with the name of its maker.\n\nWith \"Take a copy to hang\" you get that drawing as a 2x2 painting to "
        "hang up, once per drawing. The album itself stays forever."),
}

# =====================================================================================================================
# textures
# =====================================================================================================================
RAND, B = 4, 72
HOUT_DONKER, HOUT, HOUT_LICHT = (0x6B, 0x42, 0x26), (0xA8, 0x73, 0x3F), (0xC0, 0x8A, 0x52)
ROZE, ROZE_HOEK = (0xF7, 0xA8, 0xC8), (0xFF, 0x7E, 0xB6)
PAPIER = (0xFF, 0xFA, 0xF0)


def lijst(x, y):
    """The frame pixel at x, y: exactly client/TekeningCache.lijst."""
    r = min(min(x, y), min(B - 1 - x, B - 1 - y))
    hoek = (x < RAND or x >= B - RAND) and (y < RAND or y >= B - RAND)
    if r == 0:
        return HOUT_DONKER
    if hoek:
        return ROZE_HOEK if r == 1 else ROZE
    if r == RAND - 1:
        return ROZE
    nerf = (x * 7 + y * 13) % 11
    return HOUT_LICHT if nerf < 2 else HOUT_DONKER if nerf == 5 else HOUT


def leeg_lijst():
    img = Image.new("RGBA", (B, B))
    for y in range(B):
        for x in range(B):
            c = lijst(x, y) if min(x, y, B - 1 - x, B - 1 - y) < RAND else PAPIER
            img.putpixel((x, y), c + (255,))
    # a faint pencil "..." in the middle: on its way
    for i in range(3):
        for dx in range(2):
            for dy in range(2):
                img.putpixel((30 + i * 5 + dx, 35 + dy), (0xD8, 0xC8, 0xC0, 255))
    return img


def grid(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


def icoon_tekening():
    return grid([
        "................",
        ".dddddddddddddd.",
        ".dhhhhhhhhhhhhd.",
        ".dhpppppppppphd.",
        ".dhpwwwwwwwwphd.",
        ".dhwwwwwwwwwwhd.",
        ".dhwwwRRRRwwwhd.",
        ".dhwwRRRRRRwwhd.",
        ".dhwwRkRRkRwwhd.",
        ".dhwwRRRRRRwwhd.",
        ".dhwwwRRRRwwwhd.",
        ".dhwgggwwwwwwhd.",
        ".dhpwwwwwwwwphd.",
        ".dhpppppppppphd.",
        ".dddddddddddddd.",
        "................"], {
        "d": (0x6B, 0x42, 0x26, 255), "h": (0xA8, 0x73, 0x3F, 255), "p": (0xF7, 0xA8, 0xC8, 255), "w": (0xFF, 0xFA, 0xF0, 255),
        "R": (0xFF, 0x8F, 0xBF, 255), "k": (0x3A, 0x2A, 0x36, 255), "g": (0x6F, 0xB8, 0x6A, 255)})


def icoon_album():
    return grid([
        "................",
        "..rrrrrrrrrrrr..",
        ".rRRRRRRRRRRRRr.",
        ".rRyyyyyyyyyyRrw",
        ".rRyRRRRgRRRyRrw",
        ".rRyRRRgggRRyRrw",
        ".rRyRRgggggRyRrw",
        ".rRyRRRgggRRyRrw",
        ".rRyRRgggggRyRrw",
        ".rRyRgggggggyRrw",
        ".rRyRRRRbRRRyRrw",
        ".rRyyyyyyyyyyRrw",
        ".rRRRRRRRRRRRRrw",
        ".rrrrrrrrrrrrrrw",
        "..wwwwwwwwwwwwww",
        "................"], {
        "r": (0x7E, 0x24, 0x33, 255), "R": (0xB2, 0x3A, 0x48, 255), "y": (0xE8, 0xB8, 0x4A, 255), "g": (0x2E, 0x7D, 0x4F, 255),
        "b": (0x8D, 0x5A, 0x3B, 255), "w": (0xFF, 0xF8, 0xE8, 255)})


# =====================================================================================================================
# writing
# =====================================================================================================================
def _w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def textures_en_modellen():
    _save(leeg_lijst(), "entity", "guhwinter_tekening_leeg.png")
    for n, maak in (("guhwinter_tekening", icoon_tekening), ("guhwinter_kerstalbum", icoon_album)):
        _save(maak(), "item", n + ".png")
        _w(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{n}"}})
        _w(os.path.join(A, "items", n + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}})


def engels():
    """The English overlay of these keys and its chunk in tools/lang/chunks.json (added once, as text: no reformatting)."""
    _w(os.path.join("tools", "lang", "en", CHUNK + ".json"), {k: en for k, (nl, en) in TEKSTEN.items()})
    p = os.path.join("tools", "lang", "chunks.json")
    with open(p, encoding="utf-8") as f:
        src = f.read()
    if f'"id": "{CHUNK}"' in src:
        return
    prefixes = [K, "item.guhs.guhwinter_tekening", "item.guhs.guhwinter_kerstalbum", "entity.guhs.guhwinter_tekening",
                "entity.guhs.guhwinter_boomkaartje"]
    blok = ('    {\n      "id": "%s",\n      "title": "1.10.9 Guhwinter, slice tekenen: the drawing editor, the 2x2 drawing, the kerstboom '
            'drawings, the Kerstalbum",\n      "prefixes": [\n%s\n      ]\n    }' % (
                CHUNK, ",\n".join(f'        "{x}"' for x in prefixes)))
    i = src.rstrip().rfind("]")
    j = src.rstrip()[:i].rstrip()
    assert j.endswith("}"), "chunks.json: unexpected end"
    src = j + ",\n" + blok + "\n  ]\n}\n"
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(src)


BLOKUITLEG = os.path.join("tools", "wiki_site", "blokuitleg.py")
_MARK = "# <guhwinter_tekenen> (1.10.9, tools/features/guhwinter_tekenen.py: rewritten on every run)"


def blokuitleg():
    with open(BLOKUITLEG, encoding="utf-8") as f:
        src = f.read()
    src = re.sub(r"\n" + re.escape(_MARK) + r".*?# </guhwinter_tekenen>\n", "\n", src, flags=re.S)
    regels = [_MARK] + [f"UITLEG[{k!r}] = dict(nl={nl!r}, en={en!r})" for k, (nl, en) in UITLEG.items()] + ["# </guhwinter_tekenen>"]
    if not src.endswith("\n"):
        src += "\n"
    with open(BLOKUITLEG, "w", encoding="utf-8", newline="\n") as f:
        f.write(src + "\n".join(regels) + "\n")


def build(h):
    textures_en_modellen()
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    engels()
    blokuitleg()


def _direct():
    textures_en_modellen()
    engels()
    blokuitleg()
    p = os.path.join(A, "lang", "nl_nl.json")
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    d.update({k: nl for k, (nl, en) in TEKSTEN.items()})
    _w(p, d)
    p = os.path.join(A, "lang", "en_us.json")
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    d.update({k: en for k, (nl, en) in TEKSTEN.items()})
    _w(p, d)
    print(f"guhwinter_tekenen: 2 items, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
