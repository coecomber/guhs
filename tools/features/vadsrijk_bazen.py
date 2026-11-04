"""
1.10.0 "Het Vadsrijk", the bazen slice (CONTRACT_VADSRIJK.md). Java: feature/vadsrijk/bazen.

Writes everything the three zone-1 bosses need:
  * GeckoLib models + animations (generated here, Blockbench copies in blockbench/vadsrijk/):
      koning_klodder  root, body, zoom, ear_left, ear_right, crown        (anims idle, walk, spring, land, brul)
      kk_klodder      root, body, ear_left, ear_right                     (anims idle, walk)
      opperzwam       root, body, wortels, arm_left, arm_right, hoed      (anims idle, walk, stamp, brul)
      spion_ballon    root, ballon, touw                                  (anim idle)
    The Mika-spion uses the existing mika.geo.json with two new textures (disguised / unmasked).
  * textures in textures/vadsrijk/bazen/, item icons, the light-mushroom + temple-gate blocks, lang (nl = en).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_bazen.py
"""
import base64
import io
import json
import math
import os
import uuid

import numpy as np
from PIL import Image, ImageDraw

S = 4                       # texels per model unit
TEX = "textures/vadsrijk/bazen"
B = "vadsrijk.bazen."

# =====================================================================================================================
# texts (nl; en_us gets the same Dutch)
# =====================================================================================================================
TEKSTEN = {
    # entities
    "entity.guhs.vr_baas_koning_klodder": "Koning Klodder",
    "entity.guhs.vr_baas_kk_klodder": "Kroonklodder",
    "entity.guhs.vr_baas_opperzwam": "De Opperzwam",
    "entity.guhs.vr_baas_mika_spion": "De Mika-spion",
    "entity.guhs.vr_baas_spion_ballon": "Ontsnappingsballon",
    "entity.guhs.vr_baas_hulpslijmpje": "Hulpslijmpje",
    "block.guhs.vr_baas_lichtzwam": "Lichtzwam",
    "block.guhs.vr_baas_zwampoort": "Zwampoort",
    # boss titles (title card + boss bar + respawn board)
    B + "koning_klodder": "KONING KLODDER",
    B + "koning_klodder.onder": "Heerser der Weide",
    B + "opperzwam": "DE OPPERZWAM",
    B + "opperzwam.onder": "Hij die droomt onder de Tempel",
    B + "mika_spion": "DE MIKA-SPION",
    B + "mika_spion.onder": "Meester der Vermommingen",
    B + "zwamtempel": "DE ZWAMTEMPEL",
    B + "zwamtempel.onder": "Vahoeg oud, vahoeg zwammig",
    B + "mika_vermomd": "Akim de Kaasguh",
    # Koning Klodder
    B + "kk.splitst": "Koning Klodder spat uit elkaar in drie Kroonklodders! Vrolijk ze alle drie snel na elkaar op!",
    B + "kk.venster": "Snel! Nog %s s voor de andere Kroonklodders terugsmelten!",
    B + "kk.gelukt": "Alle drie opgevrolijkt! De Koning kloddert wankel weer in elkaar.",
    B + "kk.mislukt": "Te langzaam... %s Kroonklodder(s) smelten terug in de Koning. Hij heelt!",
    B + "kk.hemel": "Koning Klodder springt de lucht in! Zoek een ROZE plek!",
    B + "kk.hemel_raak": "PLETS! Je stond in de schaduw van de Koning.",
    # Opperzwam
    B + "oz.sporen": "Sporenwolken vullen de zaal! Zoek de roze plek!",
    B + "oz.donker": "De Opperzwam blaast alle lichtjes uit... Steek de 4 Lichtzwammen aan!",
    B + "oz.licht": "Lichtzwam %s van 4 brandt!",
    B + "oz.licht_klaar": "Alle Lichtzwammen branden! De Opperzwam knippert verblind!",
    B + "oz.kantel": "De zaal kantelt... de Opperzwam groeit door het plafond!",
    B + "oz.verslagen": "De Opperzwam zucht vads en droomt weer rustig. Het plafond groeit langzaam dicht.",
    # Zwamtempel
    # fix round 1
    "entity.guhs.vr_baas_lok_mika": "De Mika-spion",
    B + "tempel.verhaal": "De grote tempeldeur blijft nog dicht (jij bent level %2$s). Op level %3$s opent een rechtsklik op de deur hem altijd. Eerder? Volg het verhaal tot Tempelwachter Steeltje je hier ontvangt, dan kan het al vanaf level %1$s.",
    B + "kk.kroon_af": "De kroon van Koning Klodder vliegt eraf! Pak hem en gooi hem terug (rechtsklik)!",
    B + "kk.kroon_pak": "Je hebt de kroon! Rechtsklik om hem naar Koning Klodder te gooien!",
    B + "kk.kroon_raak": "%s gooit de kroon tegen Koning Klodder aan! Hij is duizelig!",
    B + "kk.slijm": "De vijvers van de weide worden plakkerig slijm! Pas op waar je loopt.",
    B + "kk.schild": "Schild! Alleen %s breekt het meteen (of nog %s klappen).",
    B + "oz.last": "Sporenlast op %s! Ga er samen GEEL bij staan om de klap te delen!",
    B + "oz.spreid": "Paarse sporen op iedereen! Ga UIT ELKAAR staan!",
    B + "oz.vloer": "De tempelvloer scheurt open: wortelvelden! Daar blijf je plakken.",
    B + "oz.toren": "Een wortel-toren is gegroeid! Klim erop: zijn hoed is zijn zwakke plek!",
    B + "mika.lok_nep": "Pfff! Nep-Mika! Zijn snor glinsterde niet...",
    B + "mika.lok": "Vier Mika's?! Alleen de echte heeft een glinsterende snor!",
    B + "mika.kaas": "Een kaaswiel-lawine! Ga NIET in de rode banen staan!",
    B + "tempel.te_laag": "De grote tempeldeur blijft nog dicht (jij bent level %2$s). Kom terug op level %3$s: dan opent een rechtsklik op de deur hem altijd. Met de verhaalquest van de Zwamtempel kan het al vanaf level %1$s.",
    B + "tempel.open": "De Zwamtempel gaat open...",
    B + "tempel.kamer1": "De Lichtzwamkamer: kijk goed in welke volgorde de zwammen oplichten...",
    B + "tempel.kamer1_fout": "Pfff! Verkeerde volgorde. De zwammen gaan uit en laten het nog een keer zien.",
    B + "tempel.kamer1_goed": "Goed zo! De zwampoort schuift open.",
    B + "tempel.kamer2": "De Hendelkamer: de zwammen aan de overkant kijken je spiegelverkeerd aan. Zet de hendels en stap op de drukplaat.",
    B + "tempel.kamer2_fout": "Pfff! Dat klopt niet. De hendels springen terug.",
    B + "tempel.kamer2_goed": "Klik-klak-vahoeg! De zwampoort schuift open.",
    B + "tempel.ridders": "De Ridderzaal: de Tempelzwam-ridders blokkeren de weg! Golf %s van 3.",
    B + "tempel.ridders_klaar": "De Ridderzaal is opgevrolijkt! De weg naar de Opperzwam is vrij.",
    B + "tempel.ridders_weg": "Iedereen is weg uit de Ridderzaal... de ridders gaan weer op wacht staan.",
    # Mika-spion
    B + "mika.praat.0": "Njeg! Eh... ik bedoel: njeg. Heel gewoon. Kaasje kopen?",
    B + "mika.praat.1": "Vads! Vahoeg! Zo praten wij guhs toch? Haha. Ha.",
    B + "mika.praat.2": "Mijn snor? Die is echt. Volkomen echt. Niet aankomen.",
    B + "mika.praat.3": "Ik ben gewoon Akim. A-K-I-M. Spel het maar eens achterstevoren. Nee! Niet doen!",
    B + "mika.praat.4": "Wat een mooie stad, Zwamzoet. Wie hier de baas is? Gewoon vragen hoor. Voor een kaasje.",
    B + "mika.au": "Au! Eh... njeg? Doe normaal, ik verkoop gewoon kaas.",
    B + "mika.ontmaskerd": "NEE! Mijn vermomming! Jullie krijgen me nooit, guhjes!",
    B + "mika.kap": "De Mika-spion springt naar de volgende hoed! De guhs van Zwamzoet schieten je met een sporenkatapult achter hem aan!",
    B + "mika.ballon": "Hij ontsnapt met een ballon! Schiet of mep die ballon kapot!",
    B + "mika.ontsnapt": "De Mika-spion is ontsnapt... Hij duikt vast weer vermomd op de markt op.",
    B + "mika.gevangen": "Gevangen in een knuffelnet! De Knuffelorde neemt hem mee.",
    B + "mika.cliff": "\"Denken jullie dat ik de enige ben? Njehehe... In het noorden wachten er nog véél meer Mika's. De Opper-Mika weet alles al!\"",
    B + "mika.cliff_titel": "Er zijn er nog meer...",
    B + "mika.cliff_onder": "De Mika's wachten verderop in het Vadsrijk",
    # loot + effects
    "item.guhs.vr_baas_klodderknots": "Koninklijke Klodderknots",
    "item.guhs.vr_baas_pluisboog": "Plakkerige Pluisboog",
    "item.guhs.vr_baas_sporenstaf": "Sporenstaf van de Opperzwam",
    "item.guhs.vr_baas_wortelwambuis": "Wortelwambuis",
    "item.guhs.vr_baas_lichtzwamhelm": "Lichtzwamhelm",
    "item.guhs.vr_baas_rookbomlaarzen": "Rookbommetjeslaarzen",
    "item.guhs.vr_baas_klodderkroontje": "Het Klodderkroontje",
    "item.guhs.vr_baas_neppe_snor": "De Neppe Snor van de Spion",
    "item.guhs.vr_baas_trofee_koning_klodder": "Trofee: Koning Klodder",
    "item.guhs.vr_baas_trofee_opperzwam": "Trofee: De Opperzwam",
    "item.guhs.vr_baas_trofee_mika_spion": "Trofee: De Mika-spion",
    "vadsrijk.loot.baas_klodderknots": "Koninklijke Klodderknots",
    "vadsrijk.loot.baas_pluisboog": "Plakkerige Pluisboog",
    "vadsrijk.loot.baas_sporenstaf": "Sporenstaf van de Opperzwam",
    "vadsrijk.loot.baas_wortelwambuis": "Wortelwambuis",
    "vadsrijk.loot.baas_lichtzwamhelm": "Lichtzwamhelm",
    "vadsrijk.loot.baas_rookbomlaarzen": "Rookbommetjeslaarzen",
    "vadsrijk.loot.baas_klodderkroontje": "Het Klodderkroontje",
    "vadsrijk.loot.baas_neppe_snor": "De Neppe Snor van de Spion",
    "vadsrijk.loot.baas_klodderknots.flavour.0": "\"Elke klap een klodder. Elke klodder een vriendje.\"",
    "vadsrijk.loot.baas_pluisboog.flavour.0": "\"De pijlen blijven overal aan plakken. Ook aan jezelf.\"",
    "vadsrijk.loot.baas_sporenstaf.flavour.0": "\"Niet niezen tijdens het toveren.\"",
    "vadsrijk.loot.baas_wortelwambuis.flavour.0": "\"Groeit een beetje mee. Water geven op zondag.\"",
    "vadsrijk.loot.baas_lichtzwamhelm.flavour.0": "\"Nooit meer bang in het donker. Wel een beetje roze.\"",
    "vadsrijk.loot.baas_rookbomlaarzen.flavour.0": "\"Poef! Weg! ...Waar was ik?\"",
    "vadsrijk.loot.baas_klodderkroontje.flavour.0": "\"Zwaar is het hoofd dat de klodder draagt. Maar ook heel plakkerig.\"",
    "vadsrijk.loot.baas_neppe_snor.flavour.0": "\"Volkomen echt. Niet aankomen.\"",
    B + "effect.titel": "Effect: ",
    B + "effect.baas_klodderknots": "elke 5e klap springt er een mini-slijmpje bij dat 8 s meevecht",
    B + "effect.baas_pluisboog": "je klappen maken je doel 2 s Plakkerig",
    B + "effect.baas_sporenstaf": "kritieke klappen laten een sporenwolkje achter (Gesporen rondom je doel)",
    B + "effect.baas_wortelwambuis": "word je geraakt, dan grijpen wortels soms je aanvaller (1 s Verdoofd)",
    B + "effect.baas_lichtzwamhelm": "duisternis en blindheid hebben geen vat op je, en de mist van de Opperzwam ook niet",
    B + "effect.baas_rookbomlaarzen": "zak je onder 30% Vads, dan poef je 6 blokken weg met Snelpootjes (30 s afkoelen)",
    B + "effect.baas_klodderkroontje": "een glinsterend kroontje boven je hoofd, en elke 30 s krijgen jij en je vriendjes dichtbij 5 s Vahoeg",
    B + "effect.baas_neppe_snor": "geraakt? Soms verlies je vijanden je even uit het oog (poef, Snelpootjes)",
    B + "trofee.uitleg": "Een trofee van een verslagen baas. Neem hem mee naar huis als deco!",
    B + "kist.eerste": "Eerste keer verslagen: een gegarandeerd Legendarisch item!",
    # tips (kern.Tips)
    B + "tip.koning_klodder.titel": "Koning Klodder",
    B + "tip.koning_klodder": "Splitst hij? Vrolijk de drie Kroonklodders vlak na elkaar op, anders smelten ze terug. Springt hij de lucht in: zoek een roze cirkel!",
    B + "tip.opperzwam.titel": "De Opperzwam",
    B + "tip.opperzwam": "Roze cirkels = wortels: wegwezen. Sporenwolk? Ga op de roze plek staan. In het donker: steek de 4 Lichtzwammen aan (rechtsklik).",
    B + "tip.mika_spion.titel": "De Mika-spion",
    B + "tip.mika_spion": "Grijze cirkels = ontvadsings-poeder: eruit! Ontsnapt hij met een ballon, schiet of mep die ballon kapot.",
    B + "tip.lichtzwam.titel": "Lichtzwam",
    B + "tip.lichtzwam": "Rechtsklik een Lichtzwam om hem aan te steken.",
    # commands
    B + "cmd.geen_baas": "Geen baas in de buurt.",
}

# =====================================================================================================================
# painting helpers
# =====================================================================================================================


def _rng(seed):
    return np.random.default_rng(seed)


def vlak(kleur, var=10, seed=1, extra=None, verloop=None):
    """A painter: the colour with soft noise; verloop = (colour at the bottom) for a vertical gradient."""
    def paint(W, H, face):
        r = _rng(seed + W * 7 + H * 13 + sum(map(ord, face)) % 97)
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = kleur
        if verloop is not None:
            t = np.linspace(0, 1, H)[:, None, None]
            a[..., :3] = np.array(kleur) * (1 - t) + np.array(verloop) * t
        a[..., :3] += r.normal(0, var / 2, (H, W, 1))
        a[..., 3] = 255
        if extra:
            extra(a, W, H, face, r)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def disc(a, cx, cy, rx, kleur, ry=None, alpha=1.0):
    H, W = a.shape[:2]
    ry = ry or rx
    y0, y1 = max(0, int(cy - ry - 1)), min(H, int(cy + ry + 2))
    x0, x1 = max(0, int(cx - rx - 1)), min(W, int(cx + rx + 2))
    for y in range(y0, y1):
        for x in range(x0, x1):
            if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                a[y, x, :3] = a[y, x, :3] * (1 - alpha) + np.array(kleur[:3]) * alpha
                a[y, x, 3] = 255


def lijn(a, x0, y0, x1, y1, kleur, dik=1.0):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / max(1, n)
        disc(a, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, dik / 2 + 0.3, kleur)


def guh_gezicht(a, W, H, cy=0.45, oog=0.10, boos=False, glow=None):
    """Round guh eyes with sparkles, blush and a little mouth (boos: slanted brows)."""
    ex = [W * 0.32, W * 0.68]
    r = W * oog
    for x in ex:
        disc(a, x, H * cy, r, (24, 16, 30), ry=r * 1.15)
        if glow:
            disc(a, x, H * cy, r * 0.75, glow, ry=r * 0.9)
            disc(a, x, H * cy, r * 0.32, (40, 30, 20))
        disc(a, x - r * 0.35, H * cy - r * 0.4, r * 0.32, (255, 255, 255))
        disc(a, x + r * 0.3, H * cy + r * 0.35, r * 0.15, (255, 255, 255))
        disc(a, x + (W * 0.1 if x > W / 2 else -W * 0.1), H * cy + r * 1.7, r * 0.9, (255, 120, 170), ry=r * 0.5, alpha=0.75)
    if boos:
        lijn(a, ex[0] - r, H * cy - r * 1.9, ex[0] + r * 0.9, H * cy - r * 1.3, (60, 30, 40), dik=max(1.5, W * 0.025))
        lijn(a, ex[1] + r, H * cy - r * 1.9, ex[1] - r * 0.9, H * cy - r * 1.3, (60, 30, 40), dik=max(1.5, W * 0.025))
    # a small "w" mouth
    my = H * cy + r * 1.9
    lijn(a, W * 0.45, my, W * 0.48, my + r * 0.5, (70, 30, 50), dik=max(1.2, W * 0.018))
    lijn(a, W * 0.48, my + r * 0.5, W * 0.5, my, (70, 30, 50), dik=max(1.2, W * 0.018))
    lijn(a, W * 0.5, my, W * 0.52, my + r * 0.5, (70, 30, 50), dik=max(1.2, W * 0.018))
    lijn(a, W * 0.52, my + r * 0.5, W * 0.55, my, (70, 30, 50), dik=max(1.2, W * 0.018))


def glans(a, W, H, face, r):
    """Slime gloss: a few light streaks and bubbles."""
    if face == "down":
        return
    for _ in range(max(1, W * H // 400)):
        disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(1, 2.5), (255, 255, 255), alpha=0.35)
    if face in ("north", "south", "east", "west"):
        disc(a, W * 0.2, H * 0.18, W * 0.08, (255, 255, 255), ry=H * 0.05, alpha=0.5)


# =====================================================================================================================
# a tiny geo builder with its own texture atlas (per-face uv)
# =====================================================================================================================
class Geo:
    def __init__(self, naam, tw=128, th=128, s=S):
        self.naam, self.tw, self.th, self.s = naam, tw, th, s
        self.img = np.zeros((th * s, tw * s, 4), np.uint8)
        self.x = self.y = self.rij = 0
        self.bones = []
        self.idx = {}

    def _alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > self.tw:
            self.x, self.y, self.rij = 0, self.y + self.rij, 0
        if self.y + h > self.th:
            raise SystemExit(f"vadsrijk_bazen: atlas of {self.naam} is full")
        u, v = self.x, self.y
        self.x += w
        self.rij = max(self.rij, h)
        return u, v

    def bone(self, naam, parent=None, pivot=(0, 0, 0), rotation=None):
        b = {"name": naam, "pivot": list(pivot), "cubes": []}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = list(rotation)
        self.bones.append(b)
        self.idx[naam] = b
        return b

    def cube(self, bone, origin, size, painter, faces=None, inflate=0, rotation=None, pivot=None):
        """painter(W, H, face) -> uint8 array; faces = {face: painter} overrides per face."""
        sx, sy, sz = size
        dims = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
        uv = {}
        for f, (w, h) in dims.items():
            wu, hu = max(1, math.ceil(w)), max(1, math.ceil(h))
            u, v = self._alloc(wu, hu)
            k = self.s
            W, H = wu * k, hu * k
            p = (faces or {}).get(f, painter)
            self.img[v * k:v * k + H, u * k:u * k + W] = p(W, H, f)
            if f in ("up", "down"):
                uv[f] = {"uv": [u + w, v + h], "uv_size": [-w, -h]} if f == "down" else {"uv": [u, v], "uv_size": [w, h]}
            else:
                uv[f] = {"uv": [u, v], "uv_size": [w, h]}
        c = {"origin": list(origin), "size": list(size), "uv": uv}
        if inflate:
            c["inflate"] = inflate
        if rotation:
            c["rotation"] = list(rotation)
            c["pivot"] = list(pivot or (origin[0] + sx / 2, origin[1] + sy / 2, origin[2] + sz / 2))
        self.idx[bone]["cubes"].append(c)
        return c

    def json(self, vb=(3, 3, 0)):
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.naam, "texture_width": self.tw, "texture_height": self.th,
                            "visible_bounds_width": vb[0], "visible_bounds_height": vb[1], "visible_bounds_offset": [0, vb[2], 0]},
            "bones": self.bones}]}

    def png(self):
        return Image.fromarray(self.img)


def kf(paren, loop=True, lengte=None):
    """{bone: {channel: {time: [x,y,z]}}} -> one animation."""
    tijden = [float(t) for b in paren.values() for ch in b.values() for t in ch]
    return {"loop": loop, "animation_length": lengte or (max(tijden) if tijden else 1.0),
            "bones": {b: {ch: {str(t): v for t, v in pts.items()} for ch, pts in kanalen.items()} for b, kanalen in paren.items()}}


# =====================================================================================================================
# the models
# =====================================================================================================================
KK_ROZE = (250, 120, 190)
KK_KLEUREN = {"roze": (255, 130, 200), "blauw": (110, 170, 255), "geel": (255, 214, 90)}
GOUD = (250, 206, 72)


def koning_klodder():
    g = Geo("koning_klodder")
    lijf = vlak(KK_ROZE, 8, 3, extra=glans, verloop=(205, 80, 160))
    gezicht = vlak(KK_ROZE, 8, 4, extra=lambda a, W, H, f, r: (glans(a, W, H, f, r), guh_gezicht(a, W, H, cy=0.42, oog=0.1)), verloop=(205, 80, 160))

    def hermelijn(a, W, H, f, r):
        for i in range(0, W, max(4, W // 9)):
            disc(a, i + 2, H * 0.5, 1.4, (30, 26, 34))
    g.bone("root")
    g.bone("body", "root")
    g.cube("body", (-9, 0, -9), (18, 15, 18), lijf, faces={"north": gezicht})
    g.bone("zoom", "body")
    g.cube("zoom", (-9.4, 0, -9.4), (18.8, 2.4, 18.8), vlak((246, 244, 240), 6, 5, extra=hermelijn))
    oor = vlak((250, 140, 200), 6, 6, extra=lambda a, W, H, f, r: disc(a, W / 2, H * 0.55, W * 0.28, (255, 190, 220), ry=H * 0.3) if f == "north" else None)
    g.bone("ear_left", "body", (6, 15, 0))
    g.cube("ear_left", (4.5, 14, -1.5), (4, 5, 3), oor)
    g.bone("ear_right", "body", (-6, 15, 0))
    g.cube("ear_right", (-8.5, 14, -1.5), (4, 5, 3), oor)
    goud = vlak(GOUD, 10, 7, verloop=(214, 160, 40))
    g.bone("crown", "body", (0, 15, 0))
    g.cube("crown", (-5.5, 15, -5.5), (11, 3, 11), goud)
    for (x, z) in ((-5.5, -5.5), (3.5, -5.5), (-5.5, 3.5), (3.5, 3.5)):
        g.cube("crown", (x, 18, z), (2, 3, 2), goud)
    g.cube("crown", (-1, 18, -5.5), (2, 4, 1), goud)
    g.cube("crown", (-0.8, 22, -5.3), (1.6, 1.6, 0.6), vlak((255, 70, 140), 6, 8))
    g.cube("crown", (-0.8, 16, -5.9), (1.6, 1.6, 0.5), vlak((90, 170, 255), 6, 9))
    g.cube("crown", (-3.8, 16, -5.9), (1.4, 1.4, 0.5), vlak((255, 70, 140), 6, 10))
    g.cube("crown", (2.4, 16, -5.9), (1.4, 1.4, 0.5), vlak((255, 214, 90), 6, 11))
    anims = {"animations": {
        "idle": kf({"body": {"scale": {0: [1, 1, 1], 0.8: [1.04, 0.95, 1.04], 1.6: [1, 1, 1]}},
                    "crown": {"rotation": {0: [0, 0, -3], 0.8: [0, 0, 3], 1.6: [0, 0, -3]}},
                    "ear_left": {"rotation": {0: [0, 0, 0], 0.8: [0, 0, -10], 1.6: [0, 0, 0]}},
                    "ear_right": {"rotation": {0: [0, 0, 0], 0.8: [0, 0, 10], 1.6: [0, 0, 0]}}}),
        "walk": kf({"body": {"scale": {0: [1.08, 0.88, 1.08], 0.3: [0.94, 1.12, 0.94], 0.6: [1.08, 0.88, 1.08]},
                             "position": {0: [0, 0, 0], 0.3: [0, 3, 0], 0.6: [0, 0, 0]}}}),
        "spring": kf({"body": {"scale": {0: [1.15, 0.8, 1.15], 0.25: [0.85, 1.3, 0.85], 0.8: [0.9, 1.15, 0.9]}},
                      "crown": {"position": {0: [0, 0, 0], 0.25: [0, 3, 0], 0.8: [0, 1, 0]}}}, loop=False),
        "land": kf({"body": {"scale": {0: [1.35, 0.65, 1.35], 0.2: [0.95, 1.08, 0.95], 0.45: [1, 1, 1]}},
                    "crown": {"position": {0: [0, -2, 0], 0.2: [0, 2, 0], 0.45: [0, 0, 0]}}}, loop=False),
        "brul": kf({"body": {"scale": {0: [1, 1, 1], 0.2: [1.12, 1.12, 1.12], 0.6: [1.12, 1.12, 1.12], 1.0: [1, 1, 1]}},
                    "crown": {"rotation": {0: [0, 0, 0], 0.15: [0, 0, 12], 0.3: [0, 0, -12], 0.45: [0, 0, 8], 0.6: [0, 0, 0]}}}, loop=False),
    }}
    return g, anims


def kk_klodder(kleur):
    g = Geo("kk_klodder", 64, 64)
    donker = tuple(int(c * 0.8) for c in kleur)
    lijf = vlak(kleur, 8, 21, extra=glans, verloop=donker)
    gezicht = vlak(kleur, 8, 22, extra=lambda a, W, H, f, r: (glans(a, W, H, f, r), guh_gezicht(a, W, H, cy=0.45, oog=0.11)), verloop=donker)
    oor = vlak(tuple(min(255, c + 20) for c in kleur), 6, 23)
    g.bone("root")
    g.bone("body", "root")
    g.cube("body", (-6, 0, -6), (12, 10, 12), lijf, faces={"north": gezicht})
    g.bone("ear_left", "body", (4, 10, 0))
    g.cube("ear_left", (3, 9.5, -1), (3, 3, 2), oor)
    g.bone("ear_right", "body", (-4, 10, 0))
    g.cube("ear_right", (-6, 9.5, -1), (3, 3, 2), oor)
    anims = {"animations": {
        "idle": kf({"body": {"scale": {0: [1, 1, 1], 0.6: [1.05, 0.94, 1.05], 1.2: [1, 1, 1]}}}),
        "walk": kf({"body": {"scale": {0: [1.1, 0.85, 1.1], 0.2: [0.92, 1.15, 0.92], 0.4: [1.1, 0.85, 1.1]},
                             "position": {0: [0, 0, 0], 0.2: [0, 3, 0], 0.4: [0, 0, 0]}}}),
    }}
    return g, anims


OZ_HOED = (232, 86, 150)
OZ_STAM = (240, 226, 206)
OZ_OOG = (236, 255, 140)


def opperzwam(modus="gewoon"):
    """modus: gewoon | glow (only the eyes, for the glow mask) | donker (only the eyes, the rest transparent)."""
    g = Geo("opperzwam", 256, 256, 2)
    leeg = modus != "gewoon"

    def wrap(p):
        if not leeg:
            return p
        return lambda W, H, f: np.zeros((H, W, 4), np.uint8)

    def stip(a, W, H, f, r):
        if f == "down":
            return
        for _ in range(max(2, W * H // 260)):
            disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(2, 5), (255, 240, 230))

    def ogen(W, H, f):
        a = np.zeros((H, W, 4), np.float32)
        if not leeg:
            a[..., :3] = OZ_STAM
            a[..., :3] += _rng(31).normal(0, 4, (H, W, 1))
            a[..., 3] = 255
            guh_gezicht(a, W, H, cy=0.42, oog=0.12, boos=True, glow=OZ_OOG)
        else:
            # only the glowing eyes
            for x in (W * 0.32, W * 0.68):
                disc(a, x, H * 0.42, W * 0.12 * 0.75, OZ_OOG, ry=W * 0.12 * 0.9)
                disc(a, x, H * 0.42, W * 0.12 * 0.32, (60, 50, 20) if modus == "donker" else OZ_OOG)
        return np.clip(a, 0, 255).astype(np.uint8)

    stam = wrap(vlak(OZ_STAM, 6, 32, verloop=(214, 196, 176)))
    hoed = wrap(vlak(OZ_HOED, 10, 33, extra=stip, verloop=(200, 60, 120)))
    lamel = wrap(vlak((250, 214, 220), 6, 34, extra=lambda a, W, H, f, r: [lijn(a, W / 2, H / 2, W / 2 + math.cos(t) * W, H / 2 + math.sin(t) * H,
                                                                                (220, 170, 180)) for t in np.linspace(0, 6.28, 24)] if f == "down" else None))
    wortel = wrap(vlak((200, 168, 130), 8, 35, verloop=(150, 120, 90)))
    g.bone("root")
    g.bone("body", "root")
    g.cube("body", (-5, 0, -5), (10, 20, 10), stam, faces={"north": ogen})
    g.cube("body", (-6, 0, -6), (12, 3, 12), stam)
    g.bone("wortels", "body")
    for o, s in (((-9, 0, -1), (4, 2, 2)), ((5, 0, -1), (4, 2, 2)), ((-1, 0, -9), (2, 2, 4)), ((-1, 0, 5), (2, 2, 4))):
        g.cube("wortels", o, s, wortel)
    g.bone("arm_left", "body", (5, 15, 0))
    g.cube("arm_left", (5, 6, -1.5), (3, 10, 3), wortel)
    g.bone("arm_right", "body", (-5, 15, 0))
    g.cube("arm_right", (-8, 6, -1.5), (3, 10, 3), wortel)
    g.bone("hoed", "body", (0, 20, 0))
    g.cube("hoed", (-13, 19, -13), (26, 1.5, 26), lamel)
    g.cube("hoed", (-14, 20.5, -14), (28, 5, 28), hoed)
    g.cube("hoed", (-11, 25.5, -11), (22, 4, 22), hoed)
    g.cube("hoed", (-7, 29.5, -7), (14, 2, 14), hoed)
    anims = {"animations": {
        "idle": kf({"hoed": {"rotation": {0: [0, 0, -2], 1.5: [0, 0, 2], 3.0: [0, 0, -2]}},
                    "arm_left": {"rotation": {0: [0, 0, -6], 1.5: [0, 0, -14], 3.0: [0, 0, -6]}},
                    "arm_right": {"rotation": {0: [0, 0, 6], 1.5: [0, 0, 14], 3.0: [0, 0, 6]}}}),
        "walk": kf({"body": {"rotation": {0: [0, 0, -4], 0.5: [0, 0, 4], 1.0: [0, 0, -4]}},
                    "arm_left": {"rotation": {0: [-25, 0, -8], 0.5: [25, 0, -8], 1.0: [-25, 0, -8]}},
                    "arm_right": {"rotation": {0: [25, 0, 8], 0.5: [-25, 0, 8], 1.0: [25, 0, 8]}}}),
        "stamp": kf({"arm_left": {"rotation": {0: [0, 0, 0], 0.3: [-160, 0, -10], 0.55: [10, 0, 0], 0.9: [0, 0, 0]}},
                     "arm_right": {"rotation": {0: [0, 0, 0], 0.3: [-160, 0, 10], 0.55: [10, 0, 0], 0.9: [0, 0, 0]}},
                     "body": {"scale": {0: [1, 1, 1], 0.3: [0.95, 1.08, 0.95], 0.55: [1.08, 0.9, 1.08], 0.9: [1, 1, 1]}}}, loop=False),
        "brul": kf({"hoed": {"position": {0: [0, 0, 0], 0.2: [0, 3, 0], 0.8: [0, 3, 0], 1.2: [0, 0, 0]},
                             "rotation": {0: [0, 0, 0], 0.3: [0, 0, 6], 0.5: [0, 0, -6], 0.7: [0, 0, 4], 1.2: [0, 0, 0]}},
                    "arm_left": {"rotation": {0: [0, 0, 0], 0.3: [0, 0, -70], 1.0: [0, 0, -70], 1.2: [0, 0, 0]}},
                    "arm_right": {"rotation": {0: [0, 0, 0], 0.3: [0, 0, 70], 1.0: [0, 0, 70], 1.2: [0, 0, 0]}}}, loop=False),
    }}
    return g, anims


def spion_ballon():
    g = Geo("spion_ballon")

    def mika_m(a, W, H, f, r):
        if f in ("north", "south"):
            c = (220, 40, 60)
            lijn(a, W * 0.25, H * 0.75, W * 0.25, H * 0.25, c, 3)
            lijn(a, W * 0.25, H * 0.25, W * 0.5, H * 0.55, c, 3)
            lijn(a, W * 0.5, H * 0.55, W * 0.75, H * 0.25, c, 3)
            lijn(a, W * 0.75, H * 0.25, W * 0.75, H * 0.75, c, 3)
        for _ in range(3):
            disc(a, r.uniform(0, W), r.uniform(0, H * 0.4), 2, (255, 255, 255), alpha=0.2)
    zwart = vlak((58, 42, 70), 8, 41, extra=mika_m)
    rand = vlak((70, 50, 84), 8, 42)
    touw = vlak((180, 150, 110), 10, 43)
    g.bone("root")
    g.bone("ballon", "root", (0, 8, 0))
    g.cube("ballon", (-8, 8, -8), (16, 16, 16), zwart)
    g.cube("ballon", (-6, 24, -6), (12, 3, 12), rand)
    g.cube("ballon", (-5, 5, -5), (10, 3, 10), rand)
    g.cube("ballon", (-1, 3, -1), (2, 2, 2), vlak((220, 40, 60), 6, 44))
    g.bone("touw", "ballon", (0, 3, 0))
    g.cube("touw", (-0.3, -34, -0.3), (0.6, 37, 0.6), touw)
    anims = {"animations": {
        "idle": kf({"ballon": {"rotation": {0: [0, 0, -4], 1.2: [0, 0, 4], 2.4: [0, 0, -4]},
                               "position": {0: [0, 0, 0], 1.2: [0, 1, 0], 2.4: [0, 0, 0]}},
                    "touw": {"rotation": {0: [0, 0, 5], 1.2: [0, 0, -5], 2.4: [0, 0, 5]}}}),
    }}
    return g, anims


# =====================================================================================================================
# the Mika-spion textures (on the existing mika.geo.json: face = north uv [11,49] 14x11)
# =====================================================================================================================
def mika_texturen(a_dir):
    bron = Image.open(os.path.join(a_dir, "textures", "entity", "mika.png")).convert("RGBA")
    k = bron.width // 128
    fx0, fy0, fx1, fy1 = 11 * k, 49 * k, 25 * k, 60 * k
    # unmasked: a spy suit (grey-purple) + a black mask over the red eyes
    spion = np.asarray(bron, np.float32).copy()
    gezicht = spion[fy0:fy1, fx0:fx1].copy()
    l = spion[..., :3].mean(axis=2, keepdims=True)
    spion[..., :3] = spion[..., :3] * 0.35 + l * 0.35 + np.array([70, 50, 90]) * 0.3
    spion[fy0:fy1, fx0:fx1] = gezicht
    W, H = fx1 - fx0, fy1 - fy0
    vak = spion[fy0:fy1, fx0:fx1]
    oog = vak.copy()
    for y in range(int(H * 0.2), int(H * 0.55)):
        for x in range(W):
            if vak[y, x, 3] > 0:
                r, g_, b = oog[y, x, :3]
                is_oog = r > 150 and g_ < 120 or (r < 60 and g_ < 60 and b < 70)
                if not is_oog:
                    vak[y, x, :3] = (26, 20, 32)
    # mask ties
    for y in range(int(H * 0.3), int(H * 0.42)):
        vak[y, :max(1, int(W * 0.06)), :3] = (26, 20, 32)
        vak[y, W - max(1, int(W * 0.06)):, :3] = (26, 20, 32)
    spion = np.clip(spion, 0, 255).astype(np.uint8)
    # disguised: the normal pink, cute guh eyes, a fake moustache and round glasses
    verm = np.asarray(bron, np.float32).copy()
    vak = verm[fy0:fy1, fx0:fx1]
    huid = np.median(verm[fy0 + int(H * 0.8):fy1, fx0:fx1, :3].reshape(-1, 3), axis=0)
    vak[:, :, :3] = huid
    vak[..., :3] += _rng(51).normal(0, 3, (H, W, 1))
    guh_gezicht(vak, W, H, cy=0.4, oog=0.1)
    for x in (W * 0.32, W * 0.68):  # glasses
        for t in np.linspace(0, 6.28, 40):
            disc(vak, x + math.cos(t) * W * 0.14, H * 0.4 + math.sin(t) * W * 0.14, 0.8, (60, 40, 30))
    lijn(vak, W * 0.42, H * 0.4, W * 0.58, H * 0.4, (60, 40, 30), 1.2)
    snor = (110, 64, 34)
    for s in (-1, 1):
        lijn(vak, W * 0.5, H * 0.66, W * 0.5 + s * W * 0.2, H * 0.7, snor, 3)
        lijn(vak, W * 0.5 + s * W * 0.2, H * 0.7, W * 0.5 + s * W * 0.27, H * 0.62, snor, 2.2)
    verm = np.clip(verm, 0, 255).astype(np.uint8)
    return Image.fromarray(spion, "RGBA"), Image.fromarray(verm, "RGBA")


# =====================================================================================================================
# item icons (16x16 pixel art) and block textures
# =====================================================================================================================
def _icoon(teken):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    teken(d)
    return im


def _iconen():
    O = (40, 26, 40, 255)
    ic = {}
    ic["vr_baas_klodderknots"] = _icoon(lambda d: (d.line([(3, 13), (8, 8)], fill=(150, 100, 60, 255), width=2),
                                                   d.ellipse([7, 2, 14, 9], fill=(110, 170, 255, 255), outline=O),
                                                   d.point([(9, 5), (12, 5)], fill=O), d.rectangle([9, 1, 12, 2], fill=GOUD + (255,))))
    ic["vr_baas_pluisboog"] = _icoon(lambda d: (d.arc([2, 2, 14, 14], 200, 340, fill=(250, 120, 190, 255), width=2),
                                                d.line([(3, 7), (13, 7)], fill=(240, 240, 240, 255)), d.ellipse([10, 9, 13, 12], fill=(150, 220, 120, 255))))
    ic["vr_baas_sporenstaf"] = _icoon(lambda d: (d.line([(3, 14), (10, 7)], fill=(170, 130, 90, 255), width=2),
                                                 d.pieslice([6, 1, 15, 10], 180, 360, fill=OZ_HOED + (255,), outline=O),
                                                 d.point([(9, 3), (12, 4)], fill=(255, 255, 255, 255)), d.point([(4, 6), (6, 4), (13, 10)], fill=(200, 255, 140, 255))))
    ic["vr_baas_wortelwambuis"] = _icoon(lambda d: (d.rectangle([3, 3, 12, 13], fill=(150, 110, 70, 255), outline=O),
                                                    d.rectangle([6, 3, 9, 5], fill=(0, 0, 0, 0)),
                                                    d.line([(4, 12), (7, 7), (11, 12)], fill=(90, 160, 80, 255))))
    ic["vr_baas_lichtzwamhelm"] = _icoon(lambda d: (d.pieslice([2, 2, 14, 14], 180, 360, fill=(255, 150, 210, 255), outline=O),
                                                    d.rectangle([4, 8, 12, 10], fill=(240, 226, 206, 255)),
                                                    d.point([(6, 5), (10, 4), (8, 6)], fill=(255, 255, 180, 255))))
    ic["vr_baas_rookbomlaarzen"] = _icoon(lambda d: (d.rectangle([3, 6, 6, 13], fill=(80, 70, 90, 255), outline=O),
                                                     d.rectangle([9, 6, 12, 13], fill=(80, 70, 90, 255), outline=O),
                                                     d.ellipse([1, 1, 7, 5], fill=(200, 200, 210, 200)), d.ellipse([8, 0, 14, 4], fill=(220, 220, 230, 200))))
    ic["vr_baas_klodderkroontje"] = _icoon(lambda d: (d.polygon([(2, 12), (2, 5), (5, 8), (8, 3), (11, 8), (14, 5), (14, 12)], fill=GOUD + (255,), outline=O),
                                                      d.ellipse([6, 8, 10, 12], fill=(255, 120, 190, 255)), d.point([(4, 10), (12, 10)], fill=(110, 170, 255, 255))))
    ic["vr_baas_neppe_snor"] = _icoon(lambda d: (d.ellipse([2, 3, 7, 8], outline=O), d.ellipse([9, 3, 14, 8], outline=O), d.line([(7, 5), (9, 5)], fill=O),
                                                 d.polygon([(8, 10), (3, 13), (1, 10), (4, 11)], fill=(110, 64, 34, 255)),
                                                 d.polygon([(8, 10), (13, 13), (15, 10), (12, 11)], fill=(110, 64, 34, 255))))

    def trofee(top):
        def t(d):
            d.rectangle([4, 12, 11, 14], fill=(200, 160, 80, 255), outline=O)
            d.rectangle([6, 10, 9, 11], fill=(220, 180, 90, 255))
            top(d)
        return _icoon(t)
    ic["vr_baas_trofee_koning_klodder"] = trofee(lambda d: (d.ellipse([3, 3, 12, 10], fill=KK_ROZE + (255,), outline=O), d.rectangle([5, 1, 10, 3], fill=GOUD + (255,))))
    ic["vr_baas_trofee_opperzwam"] = trofee(lambda d: (d.rectangle([6, 5, 9, 10], fill=OZ_STAM + (255,)), d.pieslice([2, 0, 13, 9], 180, 360, fill=OZ_HOED + (255,), outline=O)))
    ic["vr_baas_trofee_mika_spion"] = trofee(lambda d: (d.ellipse([3, 2, 12, 10], fill=(230, 170, 190, 255), outline=O), d.rectangle([3, 4, 12, 6], fill=(26, 20, 32, 255)),
                                                        d.point([(5, 5), (10, 5)], fill=(255, 60, 60, 255))))
    return ic


def _blokken():
    def vul(kleur, seed, extra=None):
        a = vlak(kleur, 12, seed, extra=extra)(16, 16, "north")
        return Image.fromarray(a, "RGBA")

    def stippen(a, W, H, f, r):
        for _ in range(5):
            disc(a, r.uniform(1, 15), r.uniform(1, 15), r.uniform(1, 1.8), (255, 245, 235))
    t = {
        "lichtzwam_steel": vul((240, 226, 206), 61),
        "lichtzwam_hoed_aan": vul((255, 150, 210), 62, extra=lambda a, W, H, f, r: (stippen(a, W, H, f, r), disc(a, 8, 8, 5, (255, 220, 240), alpha=0.4))),
        "lichtzwam_hoed_uit": vul((120, 104, 116), 63),
        "zwampoort": vul((214, 120, 160), 64, extra=lambda a, W, H, f, r: (stippen(a, W, H, f, r), lijn(a, 0, 15.5, 16, 15.5, (150, 70, 110)))),
    }
    return t


# =====================================================================================================================
# Blockbench project (bedrock format, texture embedded), same as the other generators
# =====================================================================================================================
def bbmodel(geo_file, tex_png, anims=None, naam="model"):
    g = geo_file["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    rnd = __import__("random").Random(1100)

    def uid():
        return str(uuid.UUID(int=rnd.getrandbits(128), version=4))
    buf = io.BytesIO()
    tex_png.save(buf, "PNG")
    elements, groepen = [], {}
    for b in g["bones"]:
        px, py, pz = b["pivot"]
        rot = b.get("rotation", [0, 0, 0])
        groepen[b["name"]] = {"name": b["name"], "origin": [-px, py, pz], "rotation": [-rot[0], -rot[1], rot[2]], "uuid": uid(),
                              "export": True, "isOpen": False, "visibility": True, "children": [], "parent": b.get("parent")}
        for cu in b.get("cubes", []):
            (ox, oy, oz), (sx, sy, sz) = cu["origin"], cu["size"]
            crot = cu.get("rotation", [0, 0, 0])
            piv = cu.get("pivot", [ox + sx / 2, oy + sy / 2, oz + sz / 2])
            e = {"name": b["name"], "type": "cube", "uuid": uid(), "rescale": False, "locked": False,
                 "from": [-(ox + sx), oy, oz], "to": [-ox, oy + sy, oz + sz], "origin": [-piv[0], piv[1], piv[2]],
                 "rotation": [-crot[0], -crot[1], crot[2]], "inflate": cu.get("inflate", 0), "autouv": 0, "color": 0, "box_uv": False,
                 "faces": {f: {"uv": [d["uv"][0], d["uv"][1], d["uv"][0] + d["uv_size"][0], d["uv"][1] + d["uv_size"][1]], "texture": 0}
                           for f, d in cu["uv"].items()}}
            elements.append(e)
            groepen[b["name"]]["children"].append(e["uuid"])
    outliner = []
    for grp in groepen.values():
        parent = grp.pop("parent")
        (groepen[parent]["children"] if parent in groepen else outliner).append(grp)
    animaties_bb = []
    for an, a in ((anims or {}).get("animations") or {}).items():
        animators = {}
        for bn, kanalen in a.get("bones", {}).items():
            if bn not in groepen:
                continue
            kfs = []
            for kanaal, waarde in kanalen.items():
                for t, v in waarde.items():
                    kfs.append({"channel": kanaal, "data_points": [{"x": str(v[0]), "y": str(v[1]), "z": str(v[2])}],
                                "uuid": uid(), "time": float(t), "interpolation": "linear"})
            animators[groepen[bn]["uuid"]] = {"name": bn, "type": "bone", "keyframes": kfs}
        animaties_bb.append({"uuid": uid(), "name": an, "loop": "loop" if a.get("loop") else "once",
                             "length": a.get("animation_length", 1), "snapping": 24, "animators": animators})
    return {"meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": False}, "name": naam,
            "model_identifier": naam.lower(), "visible_box": [3, 3, 0], "resolution": {"width": tw, "height": th},
            "elements": elements, "outliner": outliner,
            "textures": [{"name": naam + ".png", "id": "0", "width": tex_png.width, "height": tex_png.height, "uv_width": tw, "uv_height": th,
                          "uuid": uid(), "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}],
            "animations": animaties_bb}


# =====================================================================================================================
ITEMS_WAPEN = ["vr_baas_klodderknots", "vr_baas_pluisboog", "vr_baas_sporenstaf"]


def _alles(w, a_dir, png):
    """w(path, obj) writes json, png(path, Image) writes a picture."""
    tex = os.path.join(a_dir, *TEX.split("/"))
    geo_dir = os.path.join(a_dir, "geckolib", "models", "entity")
    anim_dir = os.path.join(a_dir, "geckolib", "animations", "entity")
    bb = {}
    g, an = koning_klodder()
    w(os.path.join(geo_dir, "vr_baas_koning_klodder.geo.json"), g.json((5, 5, 2)))
    w(os.path.join(anim_dir, "vr_baas_koning_klodder.animation.json"), {"format_version": "1.8.0", **an})
    png(os.path.join(tex, "koning_klodder.png"), g.png())
    bb["KoningKlodder"] = (g.json(), g.png(), an)
    for kleur, rgb in KK_KLEUREN.items():
        g, an = kk_klodder(rgb)
        png(os.path.join(tex, f"kk_klodder_{kleur}.png"), g.png())
        if kleur == "roze":
            w(os.path.join(geo_dir, "vr_baas_kk_klodder.geo.json"), g.json((2, 2, 0.5)))
            w(os.path.join(anim_dir, "vr_baas_kk_klodder.animation.json"), {"format_version": "1.8.0", **an})
            bb["Kroonklodder"] = (g.json(), g.png(), an)
    g, an = opperzwam()
    w(os.path.join(geo_dir, "vr_baas_opperzwam.geo.json"), g.json((6, 6, 2.5)))
    w(os.path.join(anim_dir, "vr_baas_opperzwam.animation.json"), {"format_version": "1.8.0", **an})
    png(os.path.join(tex, "opperzwam.png"), g.png())
    bb["Opperzwam"] = (g.json(), g.png(), an)
    glow, _ = opperzwam("glow")
    png(os.path.join(tex, "opperzwam_glowmask.png"), glow.png())
    donker, _ = opperzwam("donker")
    png(os.path.join(tex, "opperzwam_donker.png"), donker.png())
    png(os.path.join(tex, "opperzwam_donker_glowmask.png"), glow.png())
    g, an = spion_ballon()
    w(os.path.join(geo_dir, "vr_baas_spion_ballon.geo.json"), g.json((3, 5, 0)))
    w(os.path.join(anim_dir, "vr_baas_spion_ballon.animation.json"), {"format_version": "1.8.0", **an})
    png(os.path.join(tex, "spion_ballon.png"), g.png())
    bb["SpionBallon"] = (g.json(), g.png(), an)
    spion, verm = mika_texturen(a_dir)
    png(os.path.join(tex, "mika_spion.png"), spion)
    png(os.path.join(tex, "mika_spion_vermomd.png"), verm)
    # items
    for item, im in _iconen().items():
        png(os.path.join(a_dir, "textures", "item", item + ".png"), im)
        w(os.path.join(a_dir, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
        w(os.path.join(a_dir, "models", "item", item + ".json"),
          {"parent": "minecraft:item/handheld" if item in ITEMS_WAPEN else "minecraft:item/generated", "textures": {"layer0": "guhs:item/" + item}})
    # blocks
    for naam, im in _blokken().items():
        png(os.path.join(a_dir, "textures", "block", "vr_baas_" + naam + ".png"), im)
    for aan in (True, False):
        hoed = "guhs:block/vr_baas_lichtzwam_hoed_" + ("aan" if aan else "uit")
        w(os.path.join(a_dir, "models", "block", "vr_baas_lichtzwam_" + ("aan" if aan else "uit") + ".json"), {
            "parent": "minecraft:block/block", "textures": {"particle": hoed, "steel": "guhs:block/vr_baas_lichtzwam_steel", "hoed": hoed},
            "elements": [
                {"from": [6, 0, 6], "to": [10, 8, 10], "faces": {f: {"texture": "#steel"} for f in ("north", "south", "east", "west", "down")}},
                {"from": [2, 8, 2], "to": [14, 12, 14], "faces": {f: {"texture": "#hoed"} for f in ("north", "south", "east", "west", "up", "down")}},
                {"from": [4, 12, 4], "to": [12, 14, 12], "faces": {f: {"texture": "#hoed"} for f in ("north", "south", "east", "west", "up")}}]})
    w(os.path.join(a_dir, "blockstates", "vr_baas_lichtzwam.json"), {"variants": {
        "lit=true": {"model": "guhs:block/vr_baas_lichtzwam_aan"}, "lit=false": {"model": "guhs:block/vr_baas_lichtzwam_uit"}}})
    w(os.path.join(a_dir, "models", "block", "vr_baas_zwampoort.json"), {"parent": "minecraft:block/cube_all",
                                                                       "textures": {"all": "guhs:block/vr_baas_zwampoort"}})
    w(os.path.join(a_dir, "blockstates", "vr_baas_zwampoort.json"), {"variants": {"": {"model": "guhs:block/vr_baas_zwampoort"}}})
    return bb


def _blockbench(bb):
    d = os.path.join("blockbench", "vadsrijk")
    os.makedirs(d, exist_ok=True)
    for naam, (geo, im, an) in bb.items():
        with open(os.path.join(d, naam + ".bbmodel"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(bbmodel(geo, im, an, naam), f)


def _png(pad, im):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    im.save(pad)


def build(h):
    bb = _alles(h.w, h.A, _png)
    _blockbench(bb)
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    bb = _alles(_schrijf, os.path.join(a, "assets", "guhs"), _png)
    _blockbench(bb)
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_bazen: models + textures + {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
