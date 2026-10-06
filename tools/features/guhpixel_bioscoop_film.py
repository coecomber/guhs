"""
Guhbioscoop: the film format and the tools to write a film (the nine films themselves are in guhpixel_bioscoop_films.py).

THE FORMAT (assets/guhs/guhbioscoop/films/<id>.json; read by client/FilmData.java, drawn by client/ProjectorRenderer.java)

  A film is a row of scenes on a canvas of 112 x 64 film pixels (a 7 x 4 screen, 16 pixels a block; smaller screens show
  it smaller with black bars). Times are ticks (20 a second). 30 to 60 seconds: "duur" 600..1200.

  {"formaat": 1, "id": "<id>", "duur": <ticks>,
   "atlas": {"textuur": "guhs:textures/guhbioscoop/<id>.png", "breedte": 256, "hoogte": <n>},
   "sprites": {"<name>": {"uv": [u, v, w, h], "frames": 1, "per": 4}, ...},      frames lie side by side, w is ONE frame;
                                                                                 "wit" (a white block) must be there
   "scenes": [{"naam": "...", "duur": <ticks>, "lagen": [<layer>, ...]}, ...],   layers are drawn in order, the first at the back
   "ondertitels": [{"van": t, "tot": t, "tekst": "<lang key>"}],                 film time; at most one at a time
   "geluiden": [{"t": t, "geluid": "<sound event id>", "volume": 1.0, "toon": 1.0}],
   "cues": [{"t": t, "soort": "lach|schrik|juich|snik|slaap|wakker"}]}           what the guhs in the seats do

  A layer is ONE of:
    {"sprite": "<name>", ...}        a picture of the sprite sheet
    {"vlak": "#rrggbb", ...}         a coloured rectangle (w, h)
    {"tekst": "<lang key>", ...}     a line of text ("letterlijk": "100" for a text that is not translated)
  and has a place: "x", "y" (upper left corner in film pixels; for a text its anchor), optional "s" (size, around the
  middle), "r" (degrees, around the middle), for a vlak "w" and "h". Standing still: put them in the layer. Moving:
  "sleutels": [{"t": 0, "x": .., "y": ..}, {"t": 40, "x": .., "e": "zacht"}]: times count from the start of the scene, a
  value a key leaves out stays as it was, "e" says how the values glide TO that key (lin, zacht, uit, in, stap).
  More: "van"/"tot" (scene ticks it shows), "spiegel", "golf": {"ax", "ay", "per", "fase"} (a gentle wave on top),
  "kleur" (a tint; the colour of a text), "frame" (one fixed frame). Text: "schaal" (1 = letters 8 film pixels high),
  "uitlijn" (links | midden | rechts), "schaduw", "teller": {"van", "naar", "t0", "t1"} (a running number: the %s).
  Pictures have no half-transparent pixels (hard()); text always lies on top of the pictures.

  The server only needs the length and the cues: data/guhs/guhbioscoop/films.json (FilmInfo.java), written from the same
  Film objects, so the two can never disagree.

ADDING A FILM LATER (the Knabbelring cutscenes): write a function in guhpixel_bioscoop_films.py that builds a Film, add it
  to FILMS there, add its id to Films.IDS (Java) and an unlock rule (Films.ontgrendel(p, id) when the cutscene was seen, or
  Films.voorwaarde(id, predicate)); the texts gui.guhs.guhbioscoop.film.<id>.naam / .uitleg / .slot come from the Film.

WRITING A FILM (Python)

  f = Film("id", "Name", "one line about it", "how to get it")
  f.sprite("naam", image, frames=1, per=4)           an own picture (PIL); guh(...) gives the name of a guh sprite
  s = f.scene(120, "naam")                            the next scene, 120 ticks long
  s.vlak("#8fd0ff")                                   the whole picture; or s.vlak(kleur, x, y, w, h, pad=[(t, x, y, w, h), ...])
  s.spr("eiland", 20, 40)                             a picture that stands still
  s.spr(guh(ogen="blij"), pad=[(0, -22, 22), (40, 30, 22, "zacht")], golf=(0, 1, 8))      one that walks in
  s.tekst("SKYBLOK", 56, 8, schaal=2, kleur="#ffe27a")                                    Dutch; the key is made here
  s.zeg(10, 70, "Welkom op Skyblok.")                 a subtitle (scene ticks)
  s.geluid(5, "titel"); s.cue(60, "lach")
  preview: python tools/features/guhpixel_bioscoop_films.py <id> [t ...]   writes pictures of those moments
"""
import json
import math
import os

from PIL import Image, ImageDraw, ImageFont

from features import guhpixel_bioscoop_tex as tex

B, H = 112, 64
CUES = ("lach", "schrik", "juich", "snik", "slaap", "wakker")
GELUIDEN = ("titel", "guh", "njeg", "vahoeg", "snurk", "gaap", "fanfare", "boem", "pling", "plof", "spanning", "piep", "mika", "wind", "klik",
            "lach", "oeh", "smak")
EASE = ("lin", "zacht", "uit", "in", "stap")
ATLAS_B = 256
ONDER_SCHAAL = 0.5

_GUHS = {}


def guh(kleur="lila", ogen="open", *draagt, oren="rond", **kw):
    """The name of a guh sprite (made on demand; see tex.guh)."""
    naam = "guh_" + "_".join([str(kleur) if isinstance(kleur, str) else "k%02x%02x%02x" % tuple(kleur), ogen, oren]
                             + [d.replace(":", "-") for d in draagt]
                             + [f"{k}{''.join('%02x' % c for c in v)}" for k, v in sorted(kw.items())])
    if naam not in _GUHS:
        _GUHS[naam] = tex.guh(kleur, ogen, oren, tuple(draagt), **kw)
    return naam


# sprites every film may use by name
def _bib():
    return {
        "wit": (tex.nieuw(4, 4, (255, 255, 255, 255)), 1, 4),
        "zzz": (tex.zzz(), 2, 12),
        "hart": (tex.hart(), 1, 4),
        "ster": (tex.ster(), 1, 4),
        "knabbel": (tex.knabbel(), 1, 4),
        "bed": (tex.bed(), 1, 4),
        "wolk": (tex.wolk(), 1, 4),
        "wolk_groot": (tex.wolk(34, 11), 1, 4),
        "sterren": (tex.sterren(626), 1, 4),
        "ballon": (tex.tekstballon(), 1, 4),
        "denkwolk": (tex.denkwolk(), 1, 4),
        "kruis": (tex.kruis(), 1, 4),
        "vuurwerk_roze": (tex.vuurwerk((255, 120, 170)), 3, 5),
        "vuurwerk_geel": (tex.vuurwerk((255, 220, 100)), 3, 5),
        "vuurwerk_blauw": (tex.vuurwerk((120, 200, 255)), 3, 5),
    }


BIB = _bib()


def _breedte(tekst):
    """About how wide the game's font draws this (font pixels)."""
    w = 0
    for ch in tekst:
        if ch in "i!.,:;'|":
            w += 2
        elif ch in "l":
            w += 3
        elif ch in "tI[] ()*\"":
            w += 4
        elif ch in "fk<>":
            w += 5
        else:
            w += 6
    return w


def regels(tekst, breed):
    """Greedy word wrap like the game's (font pixels)."""
    uit, nu = [], ""
    for woord in tekst.split(" "):
        proef = woord if not nu else nu + " " + woord
        if _breedte(proef) > breed and nu:
            uit.append(nu)
            nu = woord
        else:
            nu = proef
    if nu:
        uit.append(nu)
    return uit


class Scene:
    def __init__(self, film, van, duur, naam):
        self.film, self.van, self.duur, self.naam = film, van, duur, naam
        self.lagen = []

    def _extra(self, laag, kw):
        for k in ("s", "r"):
            if k in kw:
                laag[k] = kw.pop(k)
        if kw.pop("spiegel", False):
            laag["spiegel"] = True
        for k in ("van", "tot", "frame"):
            if k in kw and kw[k] is not None:
                laag[k] = kw.pop(k)
            else:
                kw.pop(k, None)
        golf = kw.pop("golf", None)
        if golf:
            g = {"ax": golf[0], "ay": golf[1], "per": golf[2]}
            if len(golf) > 3:
                g["fase"] = golf[3]
            laag["golf"] = g
        if "kleur" in kw:
            laag["kleur"] = kw.pop("kleur")
        if kw:
            raise SystemExit(f"film {self.film.id}: unknown layer option {sorted(kw)}")
        if "sleutels" in laag:          # a size or turn given for the whole layer belongs to its first key
            for k in ("s", "r"):
                if k in laag:
                    laag["sleutels"][0].setdefault(k, laag.pop(k))
        return laag

    def _sleutels(self, laag, pad, velden):
        """pad: [(t, a, b, ...[, "ease" or {..}])]: the values of velden in order, then optionally the ease or more values."""
        keys = []
        for p in pad:
            k = {"t": p[0]}
            rest = list(p[1:])
            for v in velden:
                if rest and not isinstance(rest[0], (str, dict)):
                    k[v] = rest.pop(0)
            for r in rest:
                if isinstance(r, str):
                    k["e"] = r
                else:
                    k.update(r)
            if k.get("e", "lin") not in EASE:
                raise SystemExit(f"film {self.film.id}: unknown ease {k['e']}")
            keys.append(k)
        if [k["t"] for k in keys] != sorted(k["t"] for k in keys):
            raise SystemExit(f"film {self.film.id} scene {self.naam}: keys out of order {pad}")
        laag["sleutels"] = keys

    def vlak(self, kleur, x=0, y=0, w=B, h=H, pad=None, **kw):
        laag = {"vlak": kleur}
        if pad:
            self._sleutels(laag, pad, ("x", "y", "w", "h"))
        else:
            laag.update({"x": x, "y": y, "w": w, "h": h})
        self.lagen.append(self._extra(laag, kw))
        return self

    def spr(self, naam, x=0, y=0, pad=None, **kw):
        self.film.gebruik(naam)
        laag = {"sprite": naam}
        if pad:
            self._sleutels(laag, pad, ("x", "y"))
        else:
            laag.update({"x": x, "y": y})
        self.lagen.append(self._extra(laag, kw))
        return self

    def tekst(self, nl, x=B // 2, y=0, schaal=1.0, uitlijn="midden", kleur="#ffffff", schaduw=True, pad=None, teller=None, letterlijk=False, **kw):
        laag = {"letterlijk": nl} if letterlijk else {"tekst": self.film.sleutel("t", nl)}
        laag.update({"schaal": schaal, "uitlijn": uitlijn, "kleur": kleur})
        if schaduw:
            laag["schaduw"] = True
        if teller:
            laag["teller"] = {"van": teller[0], "naar": teller[1], "t0": teller[2], "t1": teller[3]}
        if pad:
            self._sleutels(laag, pad, ("x", "y"))
        else:
            laag.update({"x": x, "y": y})
        self.lagen.append(self._extra(laag, kw))
        return self

    def zeg(self, t0, t1, nl):
        if not 0 <= t0 < t1 <= self.duur:
            raise SystemExit(f"film {self.film.id} scene {self.naam}: subtitle {t0}..{t1} outside the scene (0..{self.duur})")
        self.film.ondertitels.append({"van": self.van + t0, "tot": self.van + t1, "tekst": self.film.sleutel("o", nl)})
        return self

    def geluid(self, t, naam, toon=1.0, volume=1.0):
        gid = f"guhs:guhbioscoop.film.{naam}" if ":" not in naam else naam
        if ":" not in naam and naam not in GELUIDEN:
            raise SystemExit(f"film {self.film.id}: unknown cinema sound {naam}")
        self.film.geluiden.append({"t": self.van + t, "geluid": gid, "volume": volume, "toon": toon})
        return self

    def cue(self, t, soort):
        if soort not in CUES:
            raise SystemExit(f"film {self.film.id}: unknown cue {soort}")
        self.film.cues.append({"t": self.van + t, "soort": soort})
        return self


class Film:
    def __init__(self, fid, naam, uitleg, slot):
        self.id = fid
        self.scenes, self.ondertitels, self.geluiden, self.cues = [], [], [], []
        self.sprites = {}          # name -> (image, frames, per)
        self.teksten = {f"gui.guhs.guhbioscoop.film.{fid}.naam": naam, f"gui.guhs.guhbioscoop.film.{fid}.uitleg": uitleg,
                        f"gui.guhs.guhbioscoop.film.{fid}.slot": slot}
        self._tel = {"t": 0, "o": 0}
        self._bekend = {}
        self.t = 0
        self.gebruik("wit")

    def sleutel(self, soort, nl):
        """The lang key of a text in this film (the same Dutch text gets the same key)."""
        if (soort, nl) not in self._bekend:
            self._tel[soort] += 1
            key = f"gui.guhs.guhbioscoop.film.{self.id}.{soort}{self._tel[soort]}"
            self._bekend[(soort, nl)] = key
            self.teksten[key] = nl
        return self._bekend[(soort, nl)]

    def sprite(self, naam, image, frames=1, per=4):
        self.sprites[naam] = (tex.hard(image), frames, per)
        return naam

    def gebruik(self, naam):
        if naam in self.sprites:
            return
        if naam in _GUHS:
            self.sprites[naam] = (tex.hard(_GUHS[naam]), 1, 4)
        elif naam in BIB:
            im, frames, per = BIB[naam]
            self.sprites[naam] = (tex.hard(im), frames, per)
        else:
            raise SystemExit(f"film {self.id}: unknown sprite {naam}")

    def scene(self, duur, naam=""):
        s = Scene(self, self.t, duur, naam or f"scene{len(self.scenes) + 1}")
        self.scenes.append(s)
        self.t += duur
        return s

    @property
    def duur(self):
        return self.t

    # --- output ---------------------------------------------------------------------------------------------------------
    def atlas(self):
        """Packs the sprites on shelves; returns (image, {name: [u, v, w, h]})."""
        namen = sorted(self.sprites, key=lambda n: (-self.sprites[n][0].size[1], n))
        x = y = 1
        rij = 0
        plek = {}
        for n in namen:
            im = self.sprites[n][0]
            w, h = im.size
            if w + 2 > ATLAS_B:
                raise SystemExit(f"film {self.id}: sprite {n} is wider than the sprite sheet")
            if x + w + 1 > ATLAS_B:
                x = 1
                y += rij + 1
                rij = 0
            plek[n] = (x, y)
            x += w + 1
            rij = max(rij, h)
        hoog = y + rij + 1
        hoog = max(64, (hoog + 63) // 64 * 64)
        if hoog > 1024:
            raise SystemExit(f"film {self.id}: the sprite sheet got too high ({hoog})")
        sheet = Image.new("RGBA", (ATLAS_B, hoog), (0, 0, 0, 0))
        uv = {}
        for n in namen:
            im, frames, per = self.sprites[n]
            sheet.paste(im, plek[n])
            uv[n] = [plek[n][0], plek[n][1], im.size[0] // frames, im.size[1]]
        return sheet, uv

    def json(self, uv, atlas_hoog):
        sprites = {}
        for n in sorted(self.sprites):
            _, frames, per = self.sprites[n]
            e = {"uv": uv[n]}
            if frames > 1:
                e["frames"] = frames
                e["per"] = per
            sprites[n] = e
        return {"formaat": 1, "id": self.id, "duur": self.duur,
                "atlas": {"textuur": f"guhs:textures/guhbioscoop/{self.id}.png", "breedte": ATLAS_B, "hoogte": atlas_hoog},
                "sprites": sprites,
                "scenes": [{"naam": s.naam, "duur": s.duur, "lagen": s.lagen} for s in self.scenes],
                "ondertitels": sorted(self.ondertitels, key=lambda o: o["van"]),
                "geluiden": sorted(self.geluiden, key=lambda g: g["t"]),
                "cues": sorted(self.cues, key=lambda c: c["t"])}

    def controleer(self, engels=None):
        """Everything the game would trip over; engels: {key: English} to check the lengths of the English too."""
        fout = []
        if not 600 <= self.duur <= 1200:
            fout.append(f"lasts {self.duur} ticks (must be 600..1200: 30 to 60 seconds)")
        ond = sorted(self.ondertitels, key=lambda o: o["van"])
        for a, b in zip(ond, ond[1:]):
            if a["tot"] > b["van"]:
                fout.append(f"subtitles overlap at {b['van']}")
        if len(self.cues) < 3:
            fout.append("fewer than 3 audience cues")
        for taal, tabel in (("nl", self.teksten), ("en", engels or {})):
            for o in ond:
                t = tabel.get(o["tekst"])
                if t is None:
                    continue
                n = len(regels(t, (B - 6) / ONDER_SCHAAL))
                if n > 2:
                    fout.append(f"subtitle too long ({taal}, {n} lines): {t}")
                if o["tot"] - o["van"] < 20 + len(t) * 0.7:
                    fout.append(f"subtitle shown too short ({taal}, {o['tot'] - o['van']} ticks for {len(t)} letters): {t}")
            for s in self.scenes:
                for l in s.lagen:
                    if "tekst" in l and l["tekst"] in tabel:
                        t = tabel[l["tekst"]].replace("%s", "100")
                        if _breedte(t) * l.get("schaal", 1.0) > B - 2:
                            fout.append(f"text wider than the picture ({taal}): {t}")
        for s in self.scenes:
            for l in s.lagen:
                for k in l.get("sleutels", ()):
                    if k["t"] > s.duur:
                        fout.append(f"scene {s.naam}: a key at {k['t']} after the scene's end {s.duur}")
        if fout:
            raise SystemExit(f"film {self.id}:\n  " + "\n  ".join(fout))


# =====================================================================================================================
# a preview in Python (the same rules as ProjectorRenderer.java): python tools/features/guhpixel_bioscoop_films.py <id> [t...]
# =====================================================================================================================
def _ease(e, f):
    return {"zacht": f * f * (3 - 2 * f), "uit": 1 - (1 - f) * (1 - f), "in": f * f, "stap": 0.0}.get(e, f)


def _op(laag, lt):
    """x, y, s, r, w, h of a layer at scene time lt."""
    keys = laag.get("sleutels") or [laag]
    cur = {"x": 0.0, "y": 0.0, "s": 1.0, "r": 0.0, "w": float(B), "h": float(H)}
    vol = []
    for k in keys:
        cur = dict(cur)
        for v in cur:
            if v in k:
                cur[v] = float(k[v])
        vol.append((float(k.get("t", vol[-1][0] if vol else 0.0)), cur, k.get("e", "lin")))
    if len(vol) == 1 or lt <= vol[0][0]:
        w = dict(vol[0][1])
    elif lt >= vol[-1][0]:
        w = dict(vol[-1][1])
    else:
        i = 0
        while i < len(vol) - 2 and lt >= vol[i + 1][0]:
            i += 1
        f = (lt - vol[i][0]) / max(0.0001, vol[i + 1][0] - vol[i][0])
        f = _ease(vol[i + 1][2], f)
        w = {v: vol[i][1][v] + (vol[i + 1][1][v] - vol[i][1][v]) * f for v in cur}
    g = laag.get("golf")
    if g:
        a = (lt + g.get("fase", 0)) / max(1, g.get("per", 20)) * math.pi * 2
        w["x"] += math.cos(a) * g.get("ax", 0)
        w["y"] += math.sin(a) * g.get("ay", 0)
    return w


def _font(px):
    try:
        return ImageFont.truetype("C:/Windows/Fonts/consolab.ttf", px)
    except OSError:
        try:
            return ImageFont.truetype("DejaVuSansMono-Bold.ttf", px)
        except OSError:
            return ImageFont.load_default()


def beeld(film, t, schaal=6, teksten=None):
    """The picture of this film at tick t, blown up (text is drawn with a stand-in font)."""
    teksten = teksten or film.teksten
    doek = Image.new("RGBA", (B, H), (11, 10, 16, 255))
    tekstops = []
    scene = next((s for s in film.scenes if s.van <= t < s.van + s.duur), None)
    if scene:
        lt = t - scene.van
        for l in scene.lagen:
            if lt < l.get("van", 0) or lt >= l.get("tot", 10 ** 9):
                continue
            w = _op(l, lt)
            if "sprite" in l:
                im, frames, per = film.sprites[l["sprite"]]
                fw = im.size[0] // frames
                fr = min(l["frame"], frames - 1) if l.get("frame", -1) >= 0 else int(lt / per) % frames
                im = im.crop((fr * fw, 0, (fr + 1) * fw, im.size[1]))
                if l.get("spiegel"):
                    im = im.transpose(Image.FLIP_LEFT_RIGHT)
                if "kleur" in l:
                    k = tex.rgb(l["kleur"])
                    r, g, b, a = im.split()
                    im = Image.merge("RGBA", (r.point(lambda v: v * k[0] // 255), g.point(lambda v: v * k[1] // 255), b.point(lambda v: v * k[2] // 255), a))
                cx, cy = w["x"] + fw / 2, w["y"] + im.size[1] / 2
                if w["s"] != 1:
                    im = im.resize((max(1, round(fw * w["s"])), max(1, round(im.size[1] * w["s"]))), Image.NEAREST)
                if w["r"]:
                    im = im.rotate(-w["r"], Image.NEAREST, expand=True)
                doek.alpha_composite(im, (round(cx - im.size[0] / 2), round(cy - im.size[1] / 2))) if _past(im, cx, cy) else None
            elif "vlak" in l:
                x0, y0, x1, y1 = max(0, round(w["x"])), max(0, round(w["y"])), min(B, round(w["x"] + w["w"])), min(H, round(w["y"] + w["h"]))
                if x1 > x0 and y1 > y0:
                    ImageDraw.Draw(doek).rectangle((x0, y0, x1 - 1, y1 - 1), fill=tex.rgb(l["vlak"]))
            else:
                tekst = l["letterlijk"] if "letterlijk" in l else teksten.get(l["tekst"], l["tekst"])
                if "teller" in l:
                    tl = l["teller"]
                    f = min(1.0, max(0.0, (lt - tl["t0"]) / max(1, tl["t1"] - tl["t0"])))
                    tekst = tekst.replace("%s", str(round(tl["van"] + (tl["naar"] - tl["van"]) * f)))
                tekstops.append((tekst, w["x"], w["y"], l.get("schaal", 1.0) * w["s"], l.get("kleur", "#ffffff"), l.get("uitlijn", "links"), l.get("schaduw")))
    o = next((o for o in film.ondertitels if o["van"] <= t < o["tot"]), None)
    if o:
        rr = regels(teksten.get(o["tekst"], o["tekst"]), (B - 6) / ONDER_SCHAAL)[:3]
        hoog = len(rr) * 5 + 3
        ImageDraw.Draw(doek).rectangle((0, H - hoog, B - 1, H - 1), fill=(24, 16, 30, 255))
        for i, r in enumerate(rr):
            tekstops.append((r, B / 2, H - hoog + 2 + i * 5, ONDER_SCHAAL, "#ffffff", "midden", False))
    groot = doek.resize((B * schaal, H * schaal), Image.NEAREST)
    d = ImageDraw.Draw(groot)
    for tekst, x, y, s, kleur, uitlijn, schaduw in tekstops:
        font = _font(max(6, round(9.6 * s * schaal)))
        breed = _breedte(tekst) * s * schaal
        echt = d.textlength(tekst, font=font)
        px = x * schaal - (breed / 2 if uitlijn == "midden" else breed if uitlijn == "rechts" else 0) + (breed - echt) / 2
        py = y * schaal - 1.5 * s * schaal
        if schaduw:
            d.text((px + s * schaal, py + s * schaal), tekst, font=font, fill=(40, 40, 40, 255))
        d.text((px, py), tekst, font=font, fill=tex.rgb(kleur))
    return groot


def _past(im, cx, cy):
    return -im.size[0] < cx < B + im.size[0] and -im.size[1] < cy < H + im.size[1]


def schrijf(h, films, engels=None):
    """Writes every film: its sprite sheet, its JSON, and the server's index; returns {lang key: Dutch}."""
    index = {}
    teksten = {}
    for f in films:
        f.controleer(engels)
        sheet, uv = f.atlas()
        pad = os.path.join(h.TEX, "guhbioscoop", f"{f.id}.png")
        os.makedirs(os.path.dirname(pad), exist_ok=True)
        sheet.save(pad)
        h.w(f"{h.A}/guhbioscoop/films/{f.id}.json", f.json(uv, sheet.size[1]))
        index[f.id] = {"duur": f.duur, "cues": [[c["t"], c["soort"]] for c in sorted(f.cues, key=lambda c: c["t"])]}
        teksten.update(f.teksten)
    h.w(f"{h.D}/guhbioscoop/films.json", {"films": index})
    return teksten


def lees_engels():
    pad = os.path.join("tools", "lang", "en", "c37_px_bioscoop.json")
    return json.load(open(pad, encoding="utf-8")) if os.path.exists(pad) else {}


# =====================================================================================================================
# building blocks most films use
# =====================================================================================================================
def titelkaart(f, titel, onder, achter="#241432", kleur="#ffd27a", duur=70, schaal=2.0, versier=None):
    """The opening card: the title big, a line under it, the cinema jingle. versier(scene) may add pictures behind the text."""
    s = f.scene(duur, "titel")
    s.vlak(achter)
    if versier:
        versier(s)
    s.tekst(titel, B // 2, 10, schaal=schaal, kleur=kleur, pad=[(0, B // 2, -18), (14, B // 2, 10, "uit")])
    s.tekst(onder, B // 2, 32, schaal=0.75, kleur="#ffffff", van=18)
    s.geluid(2, "titel")
    return s


def aftiteling(f, regels_nl, duur=110, achter="#140c1c", slaper=None, einde="EINDE"):
    """The credits: lines rolling up, a sleeping guh in the corner, the audience dozes off."""
    s = f.scene(duur, "aftiteling")
    s.vlak(achter)
    stap = 11
    hoog = len(regels_nl) * stap
    for i, r in enumerate(regels_nl):
        s.tekst(r, B // 2, 0, schaal=0.6, kleur="#ffe6ee", schaduw=False,
                pad=[(0, B // 2, H + 2 + i * stap), (duur - 30, B // 2, 16 + i * stap - max(0, hoog - 44))])
    s.tekst(einde, B // 2, 4, schaal=0.75, kleur="#ffd27a", van=duur - 28)
    if slaper:
        s.spr(slaper, 86, 44, golf=(0, 0.6, 30))
        s.spr("zzz", 100, 34, golf=(0, 1, 24))
    s.geluid(12, "snurk")
    s.cue(16, "slaap")
    return s
