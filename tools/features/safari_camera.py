"""
1.6.0 Op Safari! step 2, slice S1 - the safari camera, photos and the Safari tab of the Guhdex (Java: feature/safari/camera;
design: ../../ontwerp-safaripark-en-bouwen.md 1.7; English: tools/lang/en/c161_safari_camera.json).

Here: the camera and photo-card item textures and models, the viewfinder (textures/misc/safari_zoeker.png: the camera's
scope overlay instead of the spyglass's round one) and the Dutch texts (captions, missions, the dex tab, the picked-up
guh's number and owner, F3b).

  python tools/features/safari_camera.py      (from the project root) writes only this slice's files and merges its texts
                                               into nl_nl.json / en_us.json (quick dev loop; make_resources does it all)
"""
import json
import os
import sys

from PIL import Image, ImageDraw

NAME = "safari_camera"
CLOTHES = []
BONES = {}

# the in-sentence names ("een slapende rode panda"): the species' names, lower case
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
try:
    from features import safari as _safari          # (inside make_resources: tools/ is on the path)
except ImportError:
    import safari as _safari                        # (run as a script)

ACTIES = {   # animation name -> adjective before the animal ("een slapende rode panda")
    "slaap": "slapende", "eet": "etende", "blij": "blije", "wacht": "rondkijkende", "hang": "hangende", "gaap": "gapende",
    "opgerold": "opgerolde", "drijf": "drijvende", "vlieg": "vliegende", "zwem": "zwemmende", "klim": "klimmende",
    "slinger": "slingerende", "ren": "rennende", "zit": "zittende", "tik": "tikkende", "zwaai": "zwaaiende", "dans": "dansende",
    "knabbel": "knabbelende", "spuug": "spugende", "piep": "piepende", "balk": "balkende",
}

TEKSTEN = {
    "item.guhs.safaricamera": "Safaricamera",
    "item.guhs.safaricamera.uitleg": "Houd rechtsklik ingedrukt om in te zoomen, laat los om een foto te maken.",
    "item.guhs.safaricamera.uitleg2": "Hoe groter in beeld, hoe meer sterren. Een slapend of spelend diertje is extra leuk. Vahoeg!",
    "item.guhs.safari_foto": "Safarifoto",
    "item.guhs.safari_foto.van": "Safarifoto: %s",
    "item.guhs.safari_foto.leeg": "Een lege fotokaart. Njeg?",
    "item.guhs.safari_foto.door": "Gefotografeerd door %s",
    "gui.guhs.safari.afdrukken.papier": "Je hebt een vel papier nodig om af te drukken, njeg!",
    "gui.guhs.safari.afdrukken.klaar": "Afgedrukt: een fotokaartje van de %s!",
    "gui.guhs.safari.foto.niks": "Njeg... niks op de foto! Richt op een safaridiertje.",
    "gui.guhs.safari.foto.nieuw": "Nieuw in je Safari-dex!",
    "gui.guhs.safari.foto.beter": "Mooiere foto! Je Safari-dex is bijgewerkt.",
    "gui.guhs.safari.foto.munten": "+%s Safarimunten",
    "gui.guhs.safari.foto.missie": "Fotomissie gehaald! (%s)",
    # the caption: zin.<stars> (an animal, where/with whom)
    "gui.guhs.safari.foto.zin.1": "Een kiekje van %s%s.",
    "gui.guhs.safari.foto.zin.2": "Leuk, %s%s!",
    "gui.guhs.safari.foto.zin.3": "Wauw, %s%s!",
    "gui.guhs.safari.foto.dier": "een %s%s",
    "gui.guhs.safari.foto.baby": "een %s%s-baby",
    "gui.guhs.safari.foto.dichtbij": " van heel dichtbij",
    "gui.guhs.safari.foto.familie": " met de hele familie",
    "gui.guhs.safari.foto.samen": " samen met andere diertjes",
    # the day's photo missions
    "gui.guhs.safari.missie.slapend_3": "Een ★★★-foto van een slapend dier",
    "gui.guhs.safari.missie.baby": "Een foto van een babydiertje",
    "gui.guhs.safari.missie.twee_soorten": "Twee soorten samen op één foto",
    "gui.guhs.safari.missie.familie": "Drie van dezelfde soort op één foto",
    "gui.guhs.safari.missie.etend": "Een foto van een dier dat lekker aan het eten is",
    "gui.guhs.safari.missie.water": "Een foto van een dier in het water",
    "gui.guhs.safari.missie.verstopper": "Een foto van een verstopper (die zijn schuw!)",
    "gui.guhs.safari.missie.zone": "Een foto van een dier uit %s",
    "gui.guhs.safari.missie.dier_2": "Een foto van minstens ★★ van de %s",
    "gui.guhs.safari.missie.drie_ster": "Een ★★★-foto van welk dier dan ook",
    # the Guhdex tab
    "gui.guhs.guhdex.tab.safari": "Safari",
    "gui.guhs.safari.dex.weergave.dieren": "Dieren",
    "gui.guhs.safari.dex.weergave.missies": "Missies",
    "gui.guhs.safari.dex.weergave.geheimpjes": "Geheimpjes",
    # 1.6.0 round 3: the "bekende bewoners"
    "gui.guhs.safari.dex.weergave.bewoners": "Bewoners",
    "gui.guhs.safari.dex.bewoners.kop": "Bekende bewoners %s/%s",
    "gui.guhs.safari.dex.bewoners.uitleg": "In elke zone wonen een paar diertjes met een naam. Zie je een naambordje? Maak er een foto van!",
    "gui.guhs.safari.bewoner.gefotografeerd": "Je hebt %s gefotografeerd! Een bekende bewoner van het park.",
    "gui.guhs.safari.dex.teller": "Dex %s/%s · %s Safarimunten",
    "gui.guhs.safari.dex.afdrukken": "Afdrukken",
    "gui.guhs.safari.dex.afdrukken.tip": "Maak een fotokaartje van je beste foto van dit dier (kost 1 vel papier).",
    "gui.guhs.safari.dex.nog_niet": "Nog niet gefotografeerd. Zoek dit diertje in het park en maak een mooie foto!",
    "gui.guhs.safari.dex.badge.ja": "Zonebadge van %s: binnen! ✔",
    "gui.guhs.safari.dex.badge.klaar": "Alle dieren van deze zone staan erin! Haal je zonebadge op bij de expert-guh in het expertenpaviljoen.",
    "gui.guhs.safari.dex.badge.nog": "Nog %s soort(en) te gaan voor de zonebadge van %s.",
    "gui.guhs.safari.dex.missies.kop": "Fotomissies van vandaag",
    "gui.guhs.safari.dex.missies.loon": "+%s munten",
    "gui.guhs.safari.dex.missies.nieuw": "Nieuwe missies over %su %sm.",
    "gui.guhs.safari.dex.sterren.kop": "Zo verdien je sterren",
    "gui.guhs.safari.dex.sterren.uitleg": "Groot en goed in beeld, niet achter blaadjes: tot 2 punten. Midden in beeld en het diertje kijkt jou aan: "
                                          "1 punt. Iets bijzonders, zoals slapen, eten of spelen: 1 punt. Een baby, een hele familie of twee soorten "
                                          "samen: 1 punt. 0-1 punt is ★, 2-3 is ★★ en 4-5 is ★★★. Houd de camera even vast om in te zoomen!",
    "gui.guhs.safari.dex.geheimpjes.kop": "Geheimpjes: %s/%s",
    "gui.guhs.safari.dex.geheimpjes.uitleg": "Overal in het park liggen geheimpjes verstopt. Kijk goed om je heen, njeg!",
    # F3b: the picked-up guh
    "item.guhs.picked_up_guh.nummer": "#%s in je Guhdex",
    "item.guhs.picked_up_guh.van": "Guh van %s",
}


def teksten():
    out = dict(TEKSTEN)
    for actie, woord in ACTIES.items():
        out[f"gui.guhs.safari.foto.actie.{actie}"] = woord
    for soort, naam in _safari.NAMEN.items():
        out[f"gui.guhs.safari.foto.naam.{soort}"] = naam[0].lower() + naam[1:]
    return out


# --- pictures -----------------------------------------------------------------------------------------------------------

def camera():
    """A chunky pink-and-cream safari camera with a big lens and a little guh-ear on top."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 5, 14, 13], fill=(236, 128, 168, 255))          # body
    d.rectangle([1, 5, 14, 6], fill=(250, 170, 200, 255))           # light top edge
    d.rectangle([1, 12, 14, 13], fill=(190, 90, 130, 255))          # dark bottom edge
    d.rectangle([2, 7, 5, 11], fill=(214, 196, 150, 255))           # khaki grip
    d.rectangle([10, 3, 13, 5], fill=(214, 196, 150, 255))          # the button / flash on top
    d.point([(11, 3), (12, 3)], fill=(255, 250, 220, 255))
    d.ellipse([6, 6, 12, 12], fill=(60, 50, 70, 255))               # lens ring
    d.ellipse([7, 7, 11, 11], fill=(90, 140, 200, 255))             # glass
    d.point([(8, 8)], fill=(220, 240, 255, 255))                    # shine
    d.point([(3, 4), (4, 3), (4, 4)], fill=(236, 128, 168, 255))    # a guh ear
    d.point([(4, 4)], fill=(250, 190, 210, 255))
    return img


def foto():
    """A little photo card: a white border, a savanna picture with an acacia, a corner of tape."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 2, 14, 14], fill=(250, 248, 240, 255))
    d.rectangle([2, 3, 13, 11], fill=(150, 205, 255, 255))          # sky
    d.rectangle([2, 9, 13, 11], fill=(214, 186, 96, 255))           # savanna
    d.ellipse([10, 4, 12, 6], fill=(255, 230, 120, 255))            # sun
    d.line([(6, 9), (6, 7)], fill=(110, 80, 50, 255))               # acacia trunk
    d.rectangle([4, 6, 8, 6], fill=(110, 140, 50, 255))             # its flat crown
    d.point([(3, 10), (4, 10)], fill=(240, 150, 190, 255))          # a pink little animal
    d.rectangle([6, 1, 9, 2], fill=(240, 220, 160, 200))            # tape
    d.line([(2, 13), (9, 13)], fill=(200, 190, 180, 255))           # a scribble under the picture
    return img


def zoeker():
    """The viewfinder (256 x 256, stretched over the square in the middle of the screen; black is drawn around it):
    clear in the middle, soft dark corners, white corner brackets, a focus square and rule-of-thirds lines, a red REC dot."""
    n = 256
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            # a soft vignette towards the edge of the square
            dx, dy = abs(x - n / 2) / (n / 2), abs(y - n / 2) / (n / 2)
            r = max(dx, dy)
            if r > 0.86:
                a = int(min(1.0, (r - 0.86) / 0.14) * 235)
                px[x, y] = (12, 8, 16, a)
    d = ImageDraw.Draw(img)
    wit = (255, 255, 255, 230)
    m, l, t = 22, 34, 3                                              # bracket margin, length, thickness
    for cx, cy, sx, sy in ((m, m, 1, 1), (n - m, m, -1, 1), (m, n - m, 1, -1), (n - m, n - m, -1, -1)):
        d.rectangle([min(cx, cx + sx * l), min(cy, cy + sy * t), max(cx, cx + sx * l), max(cy, cy + sy * t)], fill=wit)
        d.rectangle([min(cx, cx + sx * t), min(cy, cy + sy * l), max(cx, cx + sx * t), max(cy, cy + sy * l)], fill=wit)
    # rule of thirds, faint
    for k in (1, 2):
        p = int(m + (n - 2 * m) * k / 3)
        d.line([(p, m + 10), (p, n - m - 10)], fill=(255, 255, 255, 60), width=1)
        d.line([(m + 10, p), (n - m - 10, p)], fill=(255, 255, 255, 60), width=1)
    # focus square in the middle
    c, s = n // 2, 16
    for a, b in (((c - s, c - s), (c - s + 6, c - s)), ((c - s, c - s), (c - s, c - s + 6)), ((c + s, c - s), (c + s - 6, c - s)),
                 ((c + s, c - s), (c + s, c - s + 6)), ((c - s, c + s), (c - s + 6, c + s)), ((c - s, c + s), (c - s, c + s - 6)),
                 ((c + s, c + s), (c + s - 6, c + s)), ((c + s, c + s), (c + s, c + s - 6))):
        d.line([a, b], fill=(255, 220, 120, 230), width=2)
    # REC dot and a little guh face (two eyes and a smile) top right
    d.ellipse([m + 6, m + 8, m + 14, m + 16], fill=(235, 60, 80, 240))
    d.ellipse([n - m - 30, m + 8, n - m - 26, m + 12], fill=(255, 255, 255, 200))
    d.ellipse([n - m - 20, m + 8, n - m - 16, m + 12], fill=(255, 255, 255, 200))
    d.arc([n - m - 30, m + 10, n - m - 16, m + 20], 20, 160, fill=(255, 180, 210, 220), width=2)
    return img


def _save_pictures(save):
    save(camera(), "item", "safaricamera.png")
    save(foto(), "item", "safari_foto.png")
    save(zoeker(), "misc", "safari_zoeker.png")


def build(h):
    _save_pictures(h.save)
    h.item_model("safaricamera")
    h.item_model("safari_foto")
    for k, v in teksten().items():
        h.NL[k] = v


# --- the quick dev loop ---------------------------------------------------------------------------------------------------

if __name__ == "__main__":
    A = "src/main/resources/assets/guhs"

    def save(img, kind, name):
        path = f"{A}/textures/{kind}/{name}"
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)

    def wj(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    _save_pictures(save)
    for item in ("safaricamera", "safari_foto"):
        wj(f"{A}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{item}"}})
        wj(f"{A}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{item}"}})
    nl = teksten()
    en = json.load(open("tools/lang/en/c161_safari_camera.json", encoding="utf-8"))
    en_namen = json.load(open("tools/lang/en/c160_safari.json", encoding="utf-8"))
    for k, v in en_namen.items():
        if k.startswith("entity.guhs.safari.") and k.count(".") == 3:
            en.setdefault("gui.guhs.safari.foto.naam." + k.split(".")[3], v[0].lower() + v[1:])
    for taal, teksten_ in (("nl_nl", nl), ("en_us", en)):
        path = f"{A}/lang/{taal}.json"
        data = json.load(open(path, encoding="utf-8"))
        data.update(teksten_)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"safari_camera: {len(nl)} NL / {len(en)} EN texts, 3 pictures, 2 item models")
