"""
bbq2: the verhaal engine (Java: feature/verhaal: Verhaallijn, Cutscene(s), Verteller, Reiskaart(en), Doelen, Sluiers,
Duwtje, Rustpunten; client: CutsceneSpeler, VertelScherm, VerhaalHud, SluierRook; the Guhdex tab Verhalen: feature/gids).
This module is also the helper library of every slice that tells a story:

  verhaallijn(h, id, naam, uitleg, stappen=[(stapnaam, nu, waar), ...], klaar=(nu, waar), extra={sleutel: (nu, waar)}, kort={sleutel: tekst})
      the texts of a registered Verhaallijn (gui.guhs.verhalen.<id>.naam/.uitleg/.stap.<i>/.nu.<s>/.waar.<s>/.kort.<s>) and
      its hidden advancements quest/<id>_stap_<i> (i = 1..n; FTB task fq.adv("<id>_stap_<i>"))
  vertelkaart(h, id, titel, regels, kaart)
      a narrator card: textures/gui/verhaal/kaart_<id>.png (256x160) + gui.guhs.verhaal.kaart.<id>.titel/.regel.<i>
  reiskaart(h, id, kaart, naam=None)
      a travel map: textures/gui/verhaal/reiskaart_<id>.png (256x160) + gui.guhs.verhaal.reiskaart.<id>
  scene(h, id, titel, regels, namen=None)
      the texts of a Cutscene: scene.guhs.<id>.titel, scene.guhs.<id>.<key> and scene.guhs.<id>.naam.<acteur>
  sluier(h, structuur)
      puts guhs:<structuur> on the hide list of the live map (data/guhs/kaart/verborgen.json)
  groep(h, groep, kop)
      the heading of a group in the Guhdex tab Verhalen (gui.guhs.verhalen.kop.<groep>)
  Kaart(seed)
      a little map painter for the two kinds of pictures (parchment, land, water, hills, trees, houses, a dotted path,
      a cross, labels); `kaart` may also be any 256x160 PIL image, or a function painter(Kaart)

Dutch is the source (h.lang(key, nl, nl)); the keys keep the prefixes gui.guhs.verhalen.*, gui.guhs.verhaal.* and
scene.guhs.* (CONTRACT_130 §2.3). build(h): the engine's own texts, the headings of the four groups of this update and the
demo story (Java: VerhaalDemo, dev only). Until the skeleton puts this module in FEATURES, gids_verhalen.build runs it.
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
KAART_W, KAART_H = 256, 160

# what build() wrote this run: {id: (stappen, sleutels)} for the self-check, and the map's hide list
LIJNEN = {}
VERBORGEN = []

ALGEMEEN = {
    # the Guhdex tab Verhalen: the headings of the groups of this update
    "gui.guhs.verhalen.kop.knabbelring": "In de ban van de Knabbelring",
    "gui.guhs.verhalen.kop.guhrio": "Super Guhrio",
    "gui.guhs.verhalen.kop.techniek": "Guh-technologie",
    "gui.guhs.verhalen.kop.barbecue": "De Guhbarbecuether",
    "gui.guhs.verhalen.kop.herbekijk": "Opnieuw bekijken",
    # the Guhdex tab Verhalen: following a story, the objective line, secrets, the travel map
    "gui.guhs.verhalen.doelregel.aan": "Doel op het scherm: aan",
    "gui.guhs.verhalen.doelregel.uit": "Doel op het scherm: uit",
    "gui.guhs.verhalen.doelregel.tooltip": "Zet het regeltje linksboven in beeld aan of uit: daar staat wat je nu moet doen in het verhaal dat je volgt.",
    "gui.guhs.verhalen.volg": "Volg dit verhaal",
    "gui.guhs.verhalen.volg.gekozen": "✔ Je volgt dit verhaal (klik: weer vanzelf)",
    "gui.guhs.verhalen.volg.vanzelf": "✔ Je volgt dit verhaal vanzelf (klik: vastzetten)",
    "gui.guhs.verhalen.volg.tooltip": "Het doel op je scherm en \"Mijn verhaal\" in het Superkompas wijzen dan naar dit verhaal. Njeg, handig!",
    "gui.guhs.verhalen.volgt": "Dit verhaal volg je nu",
    "gui.guhs.verhalen.geheim": "???",
    "gui.guhs.verhalen.geheim.tooltip": "Dit komt later in het verhaal. Nog even geduld, njeg!",
    "gui.guhs.verhalen.reiskaart": "Reiskaart: %s",
    "gui.guhs.verhalen.reiskaart.tooltip": "Klik voor de kaart van je reis: waar je bent en wat je nu moet doen",
    "gui.guhs.verhalen.kaart.hier": "Je bent hier",
    "gui.guhs.verhalen.kaart.nu": "Dit moet je nu doen",
    "gui.guhs.verhalen.kaart.klaar": "De hele reis is gemaakt. Vahoeg!",
    "gui.guhs.verhalen.herbekijk": "opnieuw »",
    "gui.guhs.verhalen.herbekijk.scene": "Klik om dit filmpje opnieuw te bekijken",
    "gui.guhs.verhalen.herbekijk.kaart": "Klik om deze vertelkaart opnieuw te lezen",
    # narrator cards and cutscenes
    "gui.guhs.verhaal.verder": "Verder »",
    # the Superkompas: "Mijn verhaal"
    "structure.guhs.@doel": "Mijn verhaal",
    "structure.guhs.@doel.tooltip": "Het kompas wijst vanzelf naar het volgende doel van het verhaal dat je volgt. Moet je naar een andere wereld, dan wijst het naar het portaal.",
    "gui.guhs.verhaal.kompas.geen": "Je verhaal wijst nu nergens heen",
    "gui.guhs.verhaal.kompas.afstand": "%s: nog ongeveer %s blokken",
    "gui.guhs.verhaal.kompas.portaal": "%s: ga eerst door het portaal waar het kompas naar wijst",
    "gui.guhs.verhaal.kompas.elders": "%s: dat is in een andere wereld. Zoek een portaal, njeg!",
    "gui.guhs.verhaal.kompas.zoek": "%s: het kompas kan het hier niet vinden",
    # Guhdalfs sluier
    "quest.guhs.verhaal.sluier": "Guhdalf vindt dat je hier nog niet aan toe bent, njeg",
    "gui.guhs.verhaal.beschermd": "Hier valt niets te slopen of te bouwen: dit hoort bij het verhaal, njeg",
    # the client config (Mods, Guhs, Config)
    "guhs.configuration.objectiveLine": "Doel op het scherm",
    "guhs.configuration.objectiveLine.tooltip": "Laat linksboven in beeld zien wat je nu moet doen in het verhaal dat je volgt. De Guhdex-tab Verhalen heeft er ook een knop voor.",
}


# =====================================================================================================================
# the helpers of the slices
# =====================================================================================================================
def groep(h, groep_id, kop):
    """The heading of a group of questlines in the Guhdex tab Verhalen."""
    h.lang(f"gui.guhs.verhalen.kop.{groep_id}", kop, kop)


def verhaallijn(h, id, naam, uitleg, stappen, klaar, extra=None, kort=None):
    """
    The texts of a Verhaallijn. stappen = [(stapnaam, nu, waar), ...] in order (step i = index i), klaar = (nu, waar) for when
    it is done, extra = {sleutel: (nu, waar)} for every variant sleutel (Verhaallijn.Builder.extraSleutels), kort =
    {sleutel: the short objective line} (default: the nu text). Also writes the hidden advancements quest/<id>_stap_<i>.
    """
    base = f"gui.guhs.verhalen.{id}"
    h.lang(f"{base}.naam", naam, naam)
    h.lang(f"{base}.uitleg", uitleg, uitleg)
    sleutels = {}
    for i, (stapnaam, nu, waar) in enumerate(stappen):
        h.lang(f"{base}.stap.{i}", stapnaam, stapnaam)
        sleutels[str(i)] = (nu, waar)
    sleutels["klaar"] = klaar
    sleutels.update(extra or {})
    for s, (nu, waar) in sleutels.items():
        h.lang(f"{base}.nu.{s}", nu, nu)
        h.lang(f"{base}.waar.{s}", waar, waar)
    for s, tekst in (kort or {}).items():
        if s not in sleutels:
            raise SystemExit(f"verhaal_motor.verhaallijn({id}): kort for an unknown sleutel {s}")
        h.lang(f"{base}.kort.{s}", tekst, tekst)
    for i in range(1, len(stappen) + 1):
        h.w(f"{h.D}/advancement/quest/{id}_stap_{i}.json", {"criteria": IMPOSSIBLE})
    LIJNEN[id] = (len(stappen), sorted(sleutels))


def _plaatje(kaart):
    """A 256x160 RGBA image from an image, a Kaart or a painter function."""
    if callable(kaart) and not isinstance(kaart, Kaart):
        k = Kaart()
        kaart(k)
        kaart = k
    img = kaart.img if isinstance(kaart, Kaart) else kaart
    if img.size != (KAART_W, KAART_H):
        raise SystemExit(f"verhaal_motor: a map picture is {KAART_W}x{KAART_H}, not {img.size}")
    return img.convert("RGBA")


def vertelkaart(h, id, titel, regels, kaart):
    """A narrator card (Java: Verteller.registreer(id, len(regels)[, lijn]))."""
    h.save(_plaatje(kaart), "gui", "verhaal", f"kaart_{id}.png")
    h.lang(f"gui.guhs.verhaal.kaart.{id}.titel", titel, titel)
    for i, r in enumerate(regels):
        h.lang(f"gui.guhs.verhaal.kaart.{id}.regel.{i}", r, r)


def reiskaart(h, id, kaart, naam=None):
    """A travel map (Java: Reiskaarten.registreer(new Reiskaart(id, groep, haltes)); the haltes' pixels are on this picture)."""
    h.save(_plaatje(kaart), "gui", "verhaal", f"reiskaart_{id}.png")
    naam = naam or id.capitalize()
    h.lang(f"gui.guhs.verhaal.reiskaart.{id}", naam, naam)


def scene(h, id, titel, regels, namen=None):
    """The texts of a Cutscene: its title (the replay button), its lines {key: tekst} and its speakers' names {acteur: naam}."""
    h.lang(f"scene.guhs.{id}.titel", titel, titel)
    for key, tekst in regels.items():
        h.lang(f"scene.guhs.{id}.{key}", tekst, tekst)
    for acteur, naam in (namen or {}).items():
        h.lang(f"scene.guhs.{id}.naam.{acteur}", naam, naam)


def sluier(h, structuur):
    """Guhdalfs sluier: the structure is hidden on the live map (the server's marker script reads this file)."""
    sid = structuur if ":" in structuur else f"guhs:{structuur}"
    if sid not in VERBORGEN:
        VERBORGEN.append(sid)
    h.w(f"{h.D}/kaart/verborgen.json", {"structures": sorted(VERBORGEN)})


# =====================================================================================================================
# a little map painter
# =====================================================================================================================
class Kaart:
    """
    A drawn map on parchment (256x160). Everything is deterministic (seed). Coordinates are pixels; the same pixels go
    into the Java Halte(lijn, nr, kaartX, kaartY, structuur).
    """
    INKT = (74, 50, 32, 255)

    def __init__(self, seed=7):
        self.rng = random.Random(seed)
        self.img = Image.new("RGBA", (KAART_W, KAART_H), (236, 219, 180, 255))
        self.d = ImageDraw.Draw(self.img)
        px = self.img.load()
        for y in range(KAART_H):
            for x in range(KAART_W):
                # mottled paper, darker towards the edges
                rand = min(x, y, KAART_W - 1 - x, KAART_H - 1 - y)
                schaduw = max(0, 10 - rand) * 4
                vlek = int(8 * math.sin(x * 0.11 + y * 0.07) + 6 * math.sin(x * 0.031 - y * 0.05)) + self.rng.randint(-4, 4)
                r, g, b, a = px[x, y]
                px[x, y] = (max(0, r + vlek - schaduw), max(0, g + vlek - schaduw), max(0, b + vlek - schaduw * 5 // 4), a)
        self.d.rectangle((2, 2, KAART_W - 3, KAART_H - 3), outline=(122, 90, 52, 255))

    def land(self, punten, kleur=(214, 198, 140, 255)):
        """A filled area with an ink outline (a field, an island)."""
        self.d.polygon(punten, fill=kleur, outline=self.INKT)
        return self

    def water(self, punten, kleur=(150, 188, 196, 255)):
        """A lake or a sea (or a sauce sea: give its colour), with little waves."""
        self.d.polygon(punten, fill=kleur, outline=(70, 110, 124, 255))
        xs, ys = [p[0] for p in punten], [p[1] for p in punten]
        for _ in range(max(3, (max(xs) - min(xs)) * (max(ys) - min(ys)) // 260)):
            x, y = self.rng.randint(min(xs) + 3, max(xs) - 6), self.rng.randint(min(ys) + 3, max(ys) - 3)
            if self.img.getpixel((x, y))[:3] == kleur[:3]:
                self.d.line((x, y, x + 2, y - 1, x + 4, y), fill=(232, 244, 244, 255))
        return self

    def rivier(self, punten, kleur=(150, 188, 196, 255), breed=3):
        self.d.line(punten, fill=(70, 110, 124, 255), width=breed + 2, joint="curve")
        self.d.line(punten, fill=kleur, width=breed, joint="curve")
        return self

    def berg(self, x, y, hoog=14, kleur=(168, 150, 124, 255)):
        """A hill or a mountain with its foot at (x, y)."""
        b = hoog * 3 // 4
        self.d.polygon([(x - b, y), (x, y - hoog), (x + b, y)], fill=kleur, outline=self.INKT)
        self.d.line((x, y - hoog, x + b // 3, y - hoog // 3, x + b // 6, y), fill=self.INKT)
        return self

    def vulkaan(self, x, y, hoog=22):
        """A smoking mountain."""
        self.berg(x, y, hoog, (120, 96, 88, 255))
        self.d.polygon([(x - 3, y - hoog + 1), (x + 3, y - hoog + 1), (x, y - hoog + 5)], fill=(226, 120, 40, 255))
        for i in range(4):
            self.d.ellipse((x - 3 + i * 2, y - hoog - 6 - i * 4, x + 3 + i * 3, y - hoog - 1 - i * 4), outline=(96, 86, 84, 255))
        return self

    def boom(self, x, y, kleur=(104, 140, 78, 255)):
        self.d.line((x, y, x, y - 3), fill=self.INKT)
        self.d.ellipse((x - 3, y - 9, x + 3, y - 3), fill=kleur, outline=self.INKT)
        return self

    def bos(self, x, y, breed, hoog, n=None, kleur=(104, 140, 78, 255)):
        """Trees scattered over a box (drawn back to front)."""
        plekken = sorted(((self.rng.randint(x, x + breed), self.rng.randint(y, y + hoog)) for _ in range(n or breed * hoog // 60)),
                         key=lambda p: p[1])
        for px, py in plekken:
            self.boom(px, py, kleur)
        return self

    def huisje(self, x, y, kleur=(232, 150, 170, 255)):
        """A little house (a guh hole, a village) with its door at (x, y)."""
        self.d.rectangle((x - 4, y - 5, x + 4, y), fill=(240, 226, 200, 255), outline=self.INKT)
        self.d.polygon([(x - 6, y - 5), (x, y - 11), (x + 6, y - 5)], fill=kleur, outline=self.INKT)
        self.d.rectangle((x - 1, y - 3, x + 1, y), fill=self.INKT)
        return self

    def toren(self, x, y, hoog=18, kleur=(96, 84, 96, 255)):
        self.d.rectangle((x - 3, y - hoog, x + 3, y), fill=kleur, outline=self.INKT)
        self.d.polygon([(x - 5, y - hoog), (x, y - hoog - 7), (x + 5, y - hoog)], fill=kleur, outline=self.INKT)
        self.d.point((x, y - hoog + 4), fill=(240, 200, 80, 255))
        return self

    def pad(self, punten, kleur=None):
        """A dotted path through the points."""
        kleur = kleur or (122, 40, 72, 255)
        for (x0, y0), (x1, y1) in zip(punten, punten[1:]):
            n = max(1, int(math.hypot(x1 - x0, y1 - y0) / 4))
            for i in range(n + 1):
                if i % 2 == 0:
                    x, y = x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n
                    self.d.rectangle((x, y, x + 1, y + 1), fill=kleur)
        return self

    def kruis(self, x, y, kleur=(196, 40, 40, 255)):
        """X marks the spot."""
        self.d.line((x - 3, y - 3, x + 3, y + 3), fill=kleur, width=2)
        self.d.line((x - 3, y + 3, x + 3, y - 3), fill=kleur, width=2)
        return self

    def kompasroos(self, x=232, y=24):
        self.d.ellipse((x - 9, y - 9, x + 9, y + 9), outline=self.INKT)
        self.d.polygon([(x, y - 12), (x - 3, y), (x + 3, y)], fill=(196, 40, 40, 255), outline=self.INKT)
        self.d.polygon([(x, y + 12), (x - 3, y), (x + 3, y)], fill=(240, 226, 200, 255), outline=self.INKT)
        self.tekst(x - 2, y - 22, "N")
        return self

    def tekst(self, x, y, tekst, kleur=None):
        """A small label (PIL's built-in font: plain letters only; real names belong in the lang files, not in the picture)."""
        self.d.text((x, y), tekst, fill=kleur or self.INKT, font=ImageFont.load_default())
        return self


# =====================================================================================================================
# the demo story (Java: VerhaalDemo, dev only)
# =====================================================================================================================
def demo_kaart():
    k = Kaart(11)
    k.water([(150, 100), (256, 84), (256, 160), (120, 160)], kleur=(232, 176, 72, 255))
    k.land([(10, 150), (24, 84), (70, 66), (120, 92), (104, 150)])
    k.land([(150, 74), (176, 30), (232, 26), (246, 66), (206, 84)], kleur=(206, 188, 150, 255))
    k.bos(20, 96, 36, 30, n=9)
    k.berg(196, 46, 12).berg(214, 50, 16).berg(180, 52, 10)
    k.huisje(52, 112)
    k.toren(196, 60, 14)
    k.pad([(52, 112), (92, 104), (128, 82), (160, 66), (196, 52)])
    k.kruis(196, 52)
    k.kompasroos()
    return k


def demo(h):
    groep(h, "demo", "Demo (alleen in de ontwikkelomgeving)")
    verhaallijn(
        h, "demo", "Het demoverhaal", "Een piepklein verhaaltje dat alles van de verhaalmotor laat zien. Njeg!",
        stappen=[("Lees de vertelkaart", "Typ /guhs verhaal demo en lees de vertelkaart.", "Waar je maar wilt, op een vlak stukje"),
                 ("Kijk het filmpje", "Typ /guhs verhaal demo en kijk het filmpje af.", "Op de plek van de vertelkaart"),
                 ("Door de sluier", "Loop naar de rookmuur: Guhdalf laat je er nog niet door. Typ /guhs verhaal demo open.",
                  "De rookmuur, tien blokken naar het zuiden")],
        klaar=("Klaar! De sluier is opgelost en het vervolg staat open.", "Voorbij de rookmuur"),
        extra={"2_knabbel": ("Je hebt een kaasknabbel bij je, vahoeg! Typ /guhs verhaal demo open.", "De rookmuur, tien blokken naar het zuiden")},
        kort={"0": "Lees de vertelkaart", "1": "Kijk het filmpje", "2": "Loop naar de rookmuur", "2_knabbel": "Laat Guhdalf de sluier oplossen"})
    for key, tekst in {"beloning.kijker": "Een kijkje achter de schermen", "doel.start": "De plek van het demoverhaal",
                       "doel.sluier": "De rookmuur van het demoverhaal"}.items():
        h.lang(f"gui.guhs.verhalen.demo.{key}", tekst, tekst)
    verhaallijn(
        h, "demo_vervolg", "Het vervolg van de demo", "Wat er achter de sluier ligt.",
        stappen=[("Kijk achter de sluier", "Loop door waar eerst de rookmuur stond.", "Voorbij de rookmuur")],
        klaar=("Dat was het. Njeg!", "Overal en nergens"))
    kaart = demo_kaart()
    vertelkaart(h, "demo", "Een demo, lang geleden...",
                ["Er was eens een guh die alles wilde uitproberen.",
                 "Een kaart, een filmpje, een rookmuur en een reis: het moest er allemaal in.",
                 "En zo begon het kleinste avontuur van de hele Guhmensie. Njeg!"], kaart)
    reiskaart(h, "demo", kaart, "De demoreis")
    scene(h, "verhaal_demo", "Het demofilmpje",
          {"begin": "Op een dag kwam er een guh aangewandeld...", "hallo": "Njeg! Daar ben je. Ik heb op je gewacht!",
           "antwoord": "Vahoeg, een guh! Hallo!"},
          namen={"guh": "De guh"})
    # (the demo's smoke wall "demo_plek" is no structure: it is not on the live map's hide list)


def selfcheck(h):
    """Every line written here has the texts the Guhdex asks for; the map pictures are there."""
    problems = []
    for vid, (n, sleutels) in LIJNEN.items():
        base = f"gui.guhs.verhalen.{vid}"
        for key in [f"{base}.naam", f"{base}.uitleg"] + [f"{base}.stap.{i}" for i in range(n)] \
                + [f"{base}.{soort}.{s}" for s in sleutels for soort in ("nu", "waar")]:
            if key not in h.NL:
                problems.append(f"no text {key}")
        for i in range(1, n + 1):
            if not os.path.exists(f"{h.D}/advancement/quest/{vid}_stap_{i}.json"):
                problems.append(f"no advancement quest/{vid}_stap_{i}")
    for key in ALGEMEEN:
        if key not in h.NL:
            problems.append(f"no text {key}")
    for f in ("kaart_demo.png", "reiskaart_demo.png"):
        if not os.path.exists(os.path.join(h.TEX, "gui", "verhaal", f)):
            problems.append(f"no picture {f}")
    hidden = json.load(open(f"{h.D}/kaart/verborgen.json", encoding="utf-8"))
    if hidden.get("structures") != sorted(VERBORGEN):
        problems.append("kaart/verborgen.json is not what sluier() wrote")
    if problems:
        raise SystemExit("verhaal_motor self-check:\n  " + "\n  ".join(problems))


def build(h):
    for key, tekst in ALGEMEEN.items():
        h.lang(key, tekst, tekst)
    # the smoke cloud of Guhdalfs sluier (Java: VerhaalFeature.SLUIERROOK, client.SluierRook.Wolk): vanilla's big puffs
    h.w(f"{h.A}/particles/verhaal_sluierrook.json", {"textures": [f"minecraft:big_smoke_{i}" for i in range(4)]})
    del VERBORGEN[:]
    h.w(f"{h.D}/kaart/verborgen.json", {"structures": []})   # (every sluier(h, ...) of a later module adds its structure)
    demo(h)
    selfcheck(h)
