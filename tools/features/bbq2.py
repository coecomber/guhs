"""
bbq2 - the skelet (CONTRACT_130 5; Java: the stub Feature classes, the new GuhNpcEntity kinds, GuhVariant pages and
Brouwsels) and the small helper library every bbq2 slice may import (from features import bbq2).

  stub_npc(h, kind, naam, hue)                       name + recoloured sitting-guh texture npc_<kind>.png of an NPC kind
  pagina(h, id, naam, zeldzaamheid, info)            the Guhdex texts of a creature/variant page + quest/seen_<id>
  verborgen(h, name)                                 a hidden advancement guhs:quest/<name> (for FTB tasks)
  zichtbaar(h, tab, name, parent, icon, frame, titel, tekst)   a visible advancement guhs:<tab>/<name> + its texts
  plaatshouder_blok / plaatshouder_item / plaatshouder_wezen   the resources of a fixed id that its owner has not built yet

  build(h)  the roots of the three new advancement tabs (guhs:techniek/root, guhs:knabbelring/root, guhs:guhrio/root), a
            placeholder name + texture for every new NPC kind, placeholder Guhdex texts for the six new pages, and a
            placeholder model, loot table and name for every fixed id of CONTRACT_130 7, so the recipes, templates and FTB
            tasks of other slices load from day one.

Every slice module runs after this one (FEATURES order) and replaces its placeholders by writing the same files and keys
again; nothing here needs to be edited for that.
"""
import os

from PIL import Image

IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
TABS = ("barbecuether", "techniek", "knabbelring", "guhrio")

# the new NPC kinds (GuhNpcEntity.Kind, in enum order): kind -> (placeholder name, hue of the recoloured sitting guh)
KINDS = {
    "uitvinderguh": ("Uitvinder-guh", 0.55), "wachterguh": ("Wachter-guh", 0.62), "knuffelmakerguh": ("Knuffelmaker-guh", 0.92),
    "mika_oma": ("Mika-oma", 0.75), "stalknechtguh": ("Stalknecht-guh", 0.08), "tolwachter_mika": ("Tolwachter-Mika", 0.70),
    "archeoloogguh": ("Archeoloog-guh", 0.12), "mijnwerkerguh": ("Mijnwerker-guh", 0.58), "verzorgerguh": ("Verzorger-guh", 0.30),
    "kampbaasguh": ("Kampbaas-guh", 0.36), "houthakkerguh": ("Houthakker-guh", 0.04), "marktmeester_mika": ("Marktmeester-Mika", 0.80),
    "torenwachterguh": ("Torenwachter-guh", 0.50), "pepertelerguh": ("Peperteler-guh", 0.0), "guhdalf": ("Guhdalf", 0.60),
    "smikagol": ("Smikagol", 0.22), "araguh": ("Araguh", 0.40), "leguhlas": ("Leguhlas", 0.33), "gimguh": ("Gimguh", 0.06),
    "boromika": ("Boromika", 0.72), "merrie": ("Merrie", 0.16), "pippguh": ("Pippguh", 0.18), "guhrond": ("Guhrond", 0.65),
    "guhladriel": ("Guhladriel", 0.14), "sausuman": ("Sausuman", 0.78), "padguh": ("Pad-guh", 0.98), "perzikguh": ("Prinses Perzikguh", 0.03),
}

# the six new Guhdex pages (GuhVariant, in enum order): id -> (name, rarity, info); sam_guh and guhshi are story guhs
PAGINAS = {
    "sam_guh": ("Sam-guh", "Eén per speler, na het verhaal van de Knabbelring",
                "Sam-guh, de trouwe tuinguh van de Knabbelgouw. Hij loopt de hele reis met je mee, kookt bij elk rustpunt en weet "
                "altijd wat je nu moet doen. Njeg!"),
    "guhshi": ("Guhshi", "Eén per speler, na het duel in het Kasteel van de Grote Nether-Mika",
               "Guhshi komt uit een ei in de kelders van het kasteel. Hij fladdert over grote gaten en hapt met zijn lange tong naar "
               "alles wat lekker lijkt."),
    "sausblubje": ("Sausblubje", "Gewoon (Guhbarbecuether)",
                   "Een stuiterend blubje saus. Geef het een knuffel of een knabbel en het splitst in kleinere blubjes. Het laat "
                   "blubroom achter."),
    "sausloper": ("Sausloper", "Ongewoon (de frituursauszee van de Guhbarbecuether)",
                  "Een lieve guh op heel lange poten die over de frituursaus loopt. Op het droge rilt hij van de kou. Je stuurt hem "
                  "met pindasaus aan een stokje."),
    "worstzwijntje": ("Worstzwijntje", "Ongewoon (de stal van de Mika's in de Guhbarbecuether)",
                      "Een knorrend worstje op pootjes. Kalmeer het met iets lekkers en het is je vriendje voor altijd."),
    "bezorgguhtje": ("Bezorgguhtje", "Ongewoon (woont in een Stepstation)",
                     "Een mini-guh op een step met een veel te grote rugzak. Hij brengt spullen van halte naar halte. Tuut tuut, njeg!"),
}
VERHAAL_GUHS = {"sam_guh": ("ring", "Een eigen Sam-guh", "Neem Sam-guh mee naar huis (eenmalig, alleen voor jou)"),
                "guhshi": ("guhrio", "Een eigen Guhshi", "Guhshi is van jou (eenmalig, alleen voor jou)")}

# CONTRACT_130 7: id -> (kind, placeholder name, colour); the owner slice writes the real thing over it
VASTE_IDS = {
    # tech-bronnen
    "knuffelgenerator": ("block", "Knuffelgenerator", (244, 150, 190)), "disco_dynamo": ("block", "Disco-dynamo", (150, 110, 230)),
    "blubkacheltje": ("block", "Blubkacheltje", (236, 150, 60)), "gloeisterkern": ("block", "Gloeisterkern", (250, 214, 110)),
    "knabbelbatterij": ("block", "Knabbelbatterij", (240, 200, 90)),
    # tech-buizen
    "knabbelbuis": ("block", "Knabbelbuis", (200, 232, 240)), "knabbelbuis_filter": ("block", "Knabbelbuis-filter", (180, 214, 236)),
    "knabbelbuis_richting": ("block", "Knabbelbuis-richtingstuk", (170, 204, 240)), "opzuiger": ("block", "Opzuiger", (232, 140, 180)),
    "voorraadmeter": ("block", "Voorraadmeter", (220, 180, 120)), "snuffelsensor": ("block", "Snuffelsensor", (236, 170, 200)),
    "guhklok": ("block", "Guhklok", (240, 210, 150)), "guhteller": ("block", "Guhteller", (210, 190, 230)),
    # tech-machines
    "oogster": ("block", "Oogster", (170, 214, 120)), "knabbelaar": ("block", "Knabbelaar", (238, 160, 150)),
    "neerzetter": ("block", "Neerzetter", (160, 190, 236)), "knutselmachine": ("block", "Knutselmachine", (226, 176, 120)),
    "tekentafel": ("block", "Tekentafel", (200, 160, 110)), "plantagebak": ("block", "Plantagebak", (150, 190, 110)),
    "vadsmolen": ("block", "Vadsmolen", (230, 200, 170)), "bouwtekening": ("item", "Bouwtekening", (120, 170, 230)),
    # tech-vloeistof
    "sauspomp": ("block", "Sauspomp", (246, 196, 80)), "sausslang": ("block", "Sausslang", (240, 180, 70)),
    "sausvat": ("block", "Sausvat", (214, 160, 90)), "brouwautomaat": ("block", "Brouwautomaat", (150, 150, 170)),
    "frituurautomaat": ("block", "Frituurautomaat", (226, 150, 70)), "grillkoolpers": ("block", "Grillkoolpers", (90, 84, 92)),
    # tech-bezorg
    "stepstation": ("block", "Stepstation", (240, 150, 200)), "haltepaaltje": ("block", "Haltepaaltje", (250, 220, 90)),
    "bezorgguhtje_fluitje": ("item", "Bezorgguhtje-fluitje", (250, 214, 90)), "bezorgguhtje": ("entity", "Bezorgguhtje", None),
    # bank
    "hapluikje": ("block", "Hapluikje", (240, 140, 180)), "bank_sleutel": ("item", "Banksleutel", (250, 210, 80)),
    "bank_upgrade": ("item", "Bank Guh-upgrade", (240, 120, 200)),
    # tech-quests
    "grote_knabbelmachine": ("block", "De Grote Knabbelmachine", (240, 170, 90)), "perfecte_knabbel": ("item", "Perfecte knabbel", (255, 214, 90)),
    "knabbelmachine_beeldje": ("block", "Knabbelmachine-beeldje", (214, 170, 80)),
    # fossiel-mijn, sausdieren, paleizen
    "zoutkristal": ("item", "Zoutkristal", (236, 240, 250)), "blubroom": ("item", "Blubroom", (250, 226, 170)),
    "sausblubje_potje": ("item", "Sausblubje in een potje", (240, 170, 70)), "sausblubje": ("entity", "Sausblubje", None),
    "sausloper": ("entity", "Sausloper", None), "worstzwijntje": ("entity", "Worstzwijntje", None),
    # ring-kern, ring-h3, ring-h5
    "knabbelring": ("item", "De Knabbelring", (255, 200, 60)), "lichtflesje": ("item", "Lichtflesje", (220, 244, 255)),
    "elfenmanteltje": ("item", "Elfenmanteltje", (140, 180, 130)), "elfentouw": ("item", "Elfentouw", (220, 210, 180)),
    "elfentouw_haak": ("block", "Elfentouwhaak", (170, 170, 180)), "smikagol": ("entity", "Smikagol", None),
    "knekel_ruiter": ("entity", "Knekel-Mika-ruiter", None), "barbecuerog": ("entity", "De Barbecuerog", None),
    "oog_van_sausron": ("entity", "Het Oog van Sausron", None),
    "oog_van_sausron_beeldje": ("block", "Beeldje van het Oog van Sausron", (240, 130, 50)),
}
# the reserved Brouwsels (Brouwsel.java): id -> the name of what bubbles in the pan
BROUWSELS = {"blubroom": "Blubroomdrankje", "pepervuur": "Pepervuurdrankje", "peperzoet": "Peperzoetdrankje"}


# =====================================================================================================================
# the helper library
# =====================================================================================================================
def stub_npc(h, kind, naam, hue, sat=1.0, val=1.0):
    """The name and a texture for an NPC kind: the sitting guh with its pink parts turned to `hue` (0..1). A slice that
    gives the kind its own model writes its own npc_<kind>.png afterwards."""
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=hue, sat=sat, val=val, only=h.pinkish), "entity", f"npc_{kind}.png")
    h.lang(f"entity.guhs.guh_npc.{kind}", naam, naam)


def verborgen(h, name):
    """The hidden advancement guhs:quest/<name>: granted with GuhAdvancements.grant(player, "<name>"), FTB task fq.adv("<name>")."""
    h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})


def pagina(h, id, naam, zeldzaamheid, info):
    """The texts of the Guhdex page of a creature (entity guhs:<id>) or a story guh (variant <id>), and quest/seen_<id>."""
    h.lang(f"entity.guhs.{id}", naam, naam)
    h.lang(f"entity.guhs.guh.{id}", naam, naam)
    h.lang(f"gui.guhs.guhdex.rarity.{id}", f"Zeldzaamheid: {zeldzaamheid}", f"Zeldzaamheid: {zeldzaamheid}")
    h.lang(f"gui.guhs.guhdex.info.{id}", info, info)
    verborgen(h, f"seen_{id}")


def zichtbaar(h, tab, name, parent, icon, frame, titel, tekst, criteria=None, background=None, hidden=False):
    """A visible advancement guhs:<tab>/<name> (tab: barbecuether, techniek, knabbelring or guhrio) with its texts. parent:
    a name in the same tab, a full id, or None for the tab's root. Granted with GidsFeature.grant(player, "<tab>/<name>")
    unless `criteria` lets the game do it. A questline ends with frame "goal" or "challenge"."""
    adv = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.{tab}.{name}.title"},
                       "description": {"translate": f"advancements.guhs.{tab}.{name}.description"},
                       "frame": frame, "show_toast": True, "announce_to_chat": frame != "task", "hidden": hidden},
           "criteria": criteria or IMPOSSIBLE}
    if parent:
        adv["parent"] = parent if ":" in parent else f"guhs:{tab}/{parent}"
    else:
        adv["display"]["background"] = background or "minecraft:textures/block/pink_wool.png"
        adv["display"]["announce_to_chat"] = False
        adv["display"]["show_toast"] = False
    h.w(f"{h.D}/advancement/{tab}/{name}.json", adv)
    h.lang(f"advancements.guhs.{tab}.{name}.title", titel, titel)
    h.lang(f"advancements.guhs.{tab}.{name}.description", tekst, tekst)


def _vlak(h, kleur, seed, teken):
    """A flat 16x16 placeholder texture in this colour with a border and a question mark ("not built yet")."""
    img = h.noise_tex(kleur, 8, seed)
    px = img.load()
    donker = tuple(max(0, int(c * 0.55)) for c in kleur) + (255,)
    for i in range(16):
        px[i, 0] = px[i, 15] = px[0, i] = px[15, i] = donker
    for y, row in enumerate(teken):
        for x, c in enumerate(row):
            if c == "X":
                px[5 + x, 4 + y] = donker
    return img


VRAAGTEKEN = [".XXXX.", "X....X", "....X.", "...X..", "..X...", "......", "..X..."]


def plaatshouder_blok(h, id, naam, kleur):
    """A full cube with a question mark, its item, its loot table (drops itself) and its name."""
    h.save(_vlak(h, kleur, sum(map(ord, id)), VRAAGTEKEN), "block", f"{id}.png")
    h.simple_block(id)
    h.self_drop(id)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{id}"])
    h.lang(f"block.guhs.{id}", naam, naam)


def plaatshouder_item(h, id, naam, kleur):
    """A flat item with a question mark and its name."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    vlak = _vlak(h, kleur, sum(map(ord, id)), VRAAGTEKEN).convert("RGBA")
    img.paste(vlak.crop((2, 2, 14, 14)), (2, 2))
    h.save(img, "item", f"{id}.png")
    h.item_model(id)
    h.lang(f"item.guhs.{id}", naam, naam)


def plaatshouder_wezen(h, id, naam):
    """The name of a placeholder entity (it is invisible: feature/wereld/Plaatshouder)."""
    h.lang(f"entity.guhs.{id}", naam, naam)


# =====================================================================================================================
# the skelet's own resources
# =====================================================================================================================
def tabs(h):
    zichtbaar(h, "techniek", "root", None, "guhs:guh_wheel", "task", "Guh-technologie",
              "Maak een Guhrad, Guhdraad of een Guh Oven: vadskracht doet de rest. Njeg, het werkt vanzelf!",
              criteria={k: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{k}"}]}}
                        for k in ("guh_wheel", "guh_wire", "guh_oven")},
              background="minecraft:textures/block/pink_glazed_terracotta.png")
    # (one of the three is enough)
    h.patch_json(f"{h.D}/advancement/techniek/root.json", lambda d: d.update(requirements=[["guh_wheel", "guh_wire", "guh_oven"]]))
    zichtbaar(h, "knabbelring", "root", None, "minecraft:gold_nugget", "task", "In de ban van de Knabbelring",
              "Eén knabbel om ze allemaal te delen. Guhdalf wacht op je bij de grote barbecueput",
              background="guhs:textures/block/houtskoolsteen_stenen.png")
    zichtbaar(h, "guhrio", "root", None, "minecraft:red_mushroom", "task", "Super Guhrio",
              "Vind het Kasteel van de Grote Nether-Mika in de frituursauszee van de Guhbarbecuether",
              background="minecraft:textures/block/bricks.png")


def kinds(h):
    for kind, (naam, hue) in KINDS.items():
        stub_npc(h, kind, naam, hue)


def paginas(h):
    for id, (naam, zeldzaamheid, info) in PAGINAS.items():
        pagina(h, id, naam, zeldzaamheid, info)
    # the two story guhs: what VerhaalGuhs grants (verhalen/<pkg>_getemd, quest/verhaal_vrij_<id>, quest/verhaal_getemd_<id>)
    for id, (pkg, titel, tekst) in VERHAAL_GUHS.items():
        zichtbaar(h, "verhalen", f"{pkg}_getemd", "root", "guhs:kaas_knabbels", "goal", titel, tekst)
        verborgen(h, f"verhaal_vrij_{id}")
        verborgen(h, f"verhaal_getemd_{id}")
        h.lang(f"gui.guhs.verhaal.kopie.{id}", "Njeg? Ik ben druk met mijn verhaal. Kom straks terug!",
               "Njeg? Ik ben druk met mijn verhaal. Kom straks terug!")


def vaste_ids(h):
    for id, (kind, naam, kleur) in VASTE_IDS.items():
        if kind == "block":
            plaatshouder_blok(h, id, naam, kleur)
        elif kind == "item":
            plaatshouder_item(h, id, naam, kleur)
        else:
            plaatshouder_wezen(h, id, naam)


def brouwsels(h):
    for id, naam in BROUWSELS.items():
        h.add_tag(f"guhs/tags/item/brouwsel/{id}", [])   # (empty until its slice adds the ingredient)
        h.lang(f"quest.guhs.guhbrouwketel.brouwsel.{id}", naam, naam)


def kledingbronnen(h):
    for bron, naam in (("paleizen", "De paleizen van de Mika's"), ("bestaand", "De Spiesburcht en het Mika-grillpaleis"),
                       ("camping_markt", "De Grillcamping en de ruilmarkt"), ("toren_peper", "De vuurtoren en de Pepertuin"),
                       ("ring", "In de ban van de Knabbelring"), ("guhrio_beloning", "Super Guhrio")):
        h.lang(f"gui.guhs.kledingbron.{bron}", naam, naam)


def selfcheck(h):
    problems = []
    for kind in KINDS:
        if not os.path.exists(os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            problems.append(f"npc_{kind}.png")
    for tab in ("techniek", "knabbelring", "guhrio"):
        if not os.path.exists(f"{h.D}/advancement/{tab}/root.json"):
            problems.append(f"advancement {tab}/root")
    if problems:
        raise SystemExit("bbq2 self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    tabs(h)
    kinds(h)
    paginas(h)
    vaste_ids(h)
    brouwsels(h)
    kledingbronnen(h)
    selfcheck(h)
