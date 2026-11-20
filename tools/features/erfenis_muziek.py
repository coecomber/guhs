"""
1.10.10: the Erfenis Muziekdoosje plays a dance tune (Java: feature/erfenis/ErfenisMuziek + slaapzorg/SlaapBlokken.Speeldoos).

build(h) writes the sounds.json entry of the tune (the OGG is made once by erfenis_muziek_geluid.py and committed), the texts
(Dutch in nl_nl; the English is in tools/lang/en/c512_erfenis_muziek.json, which _direct() writes, so a full make_resources
run keeps it) and checks that every Muziektent level (vormen/muziektent/L1..L10, exported by
renders-erfenis/_bron/export_vormen.py from kern.py) has its Muziekdoosje, quiet (speelt=0: a box stamped as "playing" has
no tick waiting and used to ignore clicks).

Also the tune as a music disc (music_disc_dansje, jukebox song guhs:dansje on the SAME sound event; Java:
ErfenisMuziek.MUZIEKPLAAT): its jukebox song, texture, item model + definition, the c:music_discs and Disco-dynamo
rare-disc tags, and a pool in three party chests (PLAAT_KISTEN: the Ballonfestival, the guh picnic, which also fills the
mini picnic's chest, and DJ Njeg's secret cave disco under the wool).

Standalone (no full regeneration; from the project root):  PYTHONPATH=tools python tools/features/erfenis_muziek.py
"""
import json
import math
import os
import sys

from PIL import Image

SOUNDS = {"muziekdoosje.dansje": ["dansje"]}

# (Dutch, English)
TEKSTEN = {
    "subtitles.guhs.muziekdoosje.dansje": ("Muziekdoosje speelt een guhdansje", "Music Box plays a guh dance"),
    "gui.guhs.muziekdoosje.dansen": ("♪ Iedereen dansen, vadsjes! ♪", "♪ Everybody dance, vadsjes! ♪"),
    "gui.guhs.muziekdoosje.speelt_al": ("Het muziekdoosje speelt al!", "The music box is already playing!"),
    "gui.guhs.muziekdoosje.niet_eigen": ("Dit muziekdoosje is van de boer van dit dorp. Alleen die mag het opwinden, njeg!",
                                         "This music box belongs to this village's farmer. Only they may wind it up!"),
    # the music disc
    "item.guhs.music_disc_dansje": ("Muziekplaat: Guhdansje", "Music Disc: Guh Dance"),
    "item.guhs.music_disc_dansje.lore": ("Het vrolijke deuntje van het muziekdoosje in De Erfenis. Iedereen dansen!",
                                         "The happy tune of the music box in The Inheritance. Everybody dance!"),
    "jukebox_song.guhs.dansje": ("Guhs - Dansje", "Guhs - Little Dance"),
}

# The music disc: the same sound event as the Muziekdoosje (the OGG is 31.0 s: 30 s of music plus a 1 s tail)
PLAAT = "dansje"                                     # the jukebox song; the item is music_disc_<PLAAT>
PLAAT_ITEM = f"music_disc_{PLAAT}"
PLAAT_SECONDEN = 31.0
PLAAT_COMPARATOR = 4                                 # (ze_hangen 7, snuffeleiland 10)
# party chests: (loot table, chance per chest). The other guh discs: ~16% per Stille voorraadkelder chest, 13 in ~20% of
# the Ruige Kroeg's.
PLAAT_KISTEN = [("ballonfestival", 0.15), ("guh_picnic", 0.12), ("onderwol/grotdisco", 0.25)]   # (the secret cave disco is rare)
MUZIEKTENT = os.path.join("src", "main", "resources", "data", "guhs", "erfenis", "vormen", "muziektent")


def sounds(h):
    def patch(d):
        for event, files in SOUNDS.items():
            # streamed (30 seconds); heard up to about 20 blocks (the dancers are within 15; ErfenisMuziek.VOLUME = 1.25)
            d[event] = {"sounds": [{"name": f"guhs:muziekdoosje/{f}", "stream": True, "attenuation_distance": 20} for f in files],
                        "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def texts(h):
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def muziekplaat():
    """The disc: black vinyl with grooves (like the other guh discs), a pastel pink label with guh ears and a music note."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    roze, roze_licht, oor_binnen = (255, 176, 212, 255), (255, 214, 232, 255), (255, 140, 190, 255)
    noot, mint = (150, 60, 120, 255), (150, 230, 205, 255)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 7.6:
                px[x, y] = (30, 26, 34, 255) if int(d) % 2 else (48, 42, 54, 255)
            if d <= 3.6:
                px[x, y] = roze
    for x, y in ((4, 3), (5, 3), (4, 4), (10, 3), (11, 3), (11, 4)):    # guh ears on top of the label
        px[x, y] = roze
    px[4, 3] = px[11, 3] = oor_binnen
    px[5, 5] = px[6, 4] = roze_licht                                     # a little shine on the label
    for y in (5, 6, 7, 8):                                               # the note: a stem with a flag
        px[8, y] = noot
    px[9, 5] = px[10, 6] = noot
    for x, y in ((6, 9), (7, 9), (6, 10), (7, 10), (8, 9)):              # its head
        px[x, y] = noot
    px[3, 9] = px[12, 9] = mint                                          # mint sparkles on the vinyl
    px[11, 12] = px[12, 11] = (110, 100, 120, 255)                       # shine on the vinyl
    return img


def plaat(h):
    """The tune as a music disc: jukebox song, texture, item model/definition, tags and the party chests."""
    h.w(f"{h.D}/jukebox_song/{PLAAT}.json", {"sound_event": "guhs:muziekdoosje.dansje", "description": {"translate": f"jukebox_song.guhs.{PLAAT}"},
                                             "length_in_seconds": PLAAT_SECONDEN, "comparator_output": PLAAT_COMPARATOR})
    h.save(muziekplaat(), "item", f"{PLAAT_ITEM}.png")
    h.item_model(PLAAT_ITEM)
    h.w(f"{h.A}/items/{PLAAT_ITEM}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{PLAAT_ITEM}"}})
    h.add_tag("c/tags/item/music_discs", [f"guhs:{PLAAT_ITEM}"])
    h.add_tag("guhs/tags/item/techbron/zeldzame_plaat", [f"guhs:{PLAAT_ITEM}"])   # (the Disco-dynamo's rare disc bonus)
    for tafel, kans in PLAAT_KISTEN:
        h.add_loot(tafel, [{"type": "minecraft:item", "name": f"guhs:{PLAAT_ITEM}"}], chance=kans)


def selfcheck(h):
    problems = []
    for event, files in SOUNDS.items():
        if f"subtitles.guhs.{event}" not in TEKSTEN:
            problems.append(f"missing subtitle of {event}")
        for f in files:
            if not os.path.exists(os.path.join(h.A, "sounds", "muziekdoosje", f + ".ogg")):
                problems.append(f"missing sound muziekdoosje/{f}.ogg (run tools/features/erfenis_muziek_geluid.py)")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    if not os.path.exists(os.path.join(h.A, "sounds", "muziekdoosje", "dansje.ogg")):
        problems.append("the disc's sound is missing")
    for tafel, kans in PLAAT_KISTEN:
        with open(f"{h.D}/loot_table/chests/{tafel}.json", encoding="utf-8") as f:
            pools = json.load(f)["pools"]
        if sum(any(e.get("name") == f"guhs:{PLAAT_ITEM}" for e in p["entries"]) for p in pools) != 1:
            problems.append(f"chests/{tafel}: the disc must be in exactly one pool")
    for k in (f"item.guhs.{PLAAT_ITEM}", f"item.guhs.{PLAAT_ITEM}.lore", f"jukebox_song.guhs.{PLAAT}"):
        if k not in TEKSTEN:
            problems.append(f"missing text {k}")
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "erfenis", "ErfenisMuziek.java")
    if os.path.exists(java):
        with open(java, encoding="utf-8") as f:
            src = f.read()
        for stuk in (f'"{PLAAT_ITEM}"', f'Guhs.id("{PLAAT}")', '"muziekdoosje.dansje"'):
            if stuk not in src:
                problems.append(f"ErfenisMuziek.java lacks {stuk}")
    for n in range(1, 11):
        p = os.path.join(MUZIEKTENT, f"L{n}.json")
        if not os.path.exists(p):
            problems.append(f"missing Muziektent shape L{n}")
            continue
        with open(p, encoding="utf-8") as f:
            pal = json.load(f)["pal"]
        dozen = [b for b in pal if b.startswith("guhs:muziekdoosje")]
        if not dozen:
            problems.append(f"Muziektent L{n} has no Muziekdoosje")
        elif any("speelt=0" not in b for b in dozen):
            problems.append(f"Muziektent L{n}: the Muziekdoosje must be quiet (speelt=0): {dozen}")
    if problems:
        raise SystemExit("erfenis_muziek self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    sounds(h)
    texts(h)
    plaat(h)
    selfcheck(h)


def _direct():
    """Only this module's outputs (no full make_resources run): sounds.json, the disc's files, its lang keys in nl_nl/en_us, its
    English overlay."""
    sys.path.insert(0, "tools")
    import make_v2 as h   # (its helpers: patch_json, lang; importing it runs nothing)
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c512_erfenis_muziek.json")
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
    print(f"erfenis_muziek: {len(SOUNDS)} sound event, {len(TEKSTEN)} texts, the music disc ({len(PLAAT_KISTEN)} chests), "
          "Muziektent L1..L10 checked")


if __name__ == "__main__":
    _direct()
