"""
The page model of the wiki site: every page, the categories, which pictures belong to which page ("claims", used to put
the knowledge-base chunks on the right page) and the names that get auto-linked in texts.
"""
import html
import re
import unicodedata

# category key -> (dir, NL name, EN name, NL list title, EN list title, NL intro, EN intro)
CATEGORIES = {
    "guhs": ("guhs", "Guhs", "Guhs", "Alle guh-varianten", "All guh variants",
             "Elke guhsoort uit de Guhdex: de gewone guh, de kleurvarianten, de zeldzame en unieke guhs en de verhaalguhs.",
             "Every kind of guh in the Guhdex: the normal guh, the colour variants, the rare and unique guhs and the story guhs."),
    "npcs": ("npcs", "Personages", "Characters", "Alle NPC's", "All NPCs",
             "Alle guh-personages: spelleiders, winkeliers, verhaalfiguren en de guhdorpelingen met hun beroep.",
             "All guh characters: game hosts, shopkeepers, story characters and the guh villagers with their jobs."),
    "diertjes": ("diertjes", "Diertjes", "Critters", "Alle diertjes", "All critters",
                 "De lieve (en een paar stoute) diertjes van de Guhmensie: vogeltjes, waterdiertjes, landdiertjes en boerderijdieren.",
                 "The sweet (and a few naughty) little animals of the Guhmension: birds, water critters, land critters and farm animals."),
    "wezens": ("wezens", "Wezens", "Mobs", "Alle wezens", "All mobs",
               "Mika's, bazen en andere wezens die je in de Guhmensie, het Guheinde en de Barbecuether tegenkomt.",
               "Mikas, bosses and the other mobs you meet in the Guhmension, the Guheinde and the Barbecuether."),
    "items": ("items", "Items", "Items", "Alle items", "All items",
              "Elk voorwerp uit de mod, met recepten, waar je het vindt en waar je het voor gebruikt.",
              "Every item in the mod, with recipes, where to find it and what it is used for."),
    "blokken": ("blokken", "Blokken", "Blocks", "Alle blokken", "All blocks",
                "Elk blok uit de mod: bouwblokken, meubels, deco, planten en speciale blokken.",
                "Every block in the mod: building blocks, furniture, decoration, plants and special blocks."),
    "bouwwerken": ("bouwwerken", "Bouwwerken", "Structures", "Alle bouwwerken", "All structures",
                   "Alle gebouwen en plekken die in de wereld verschijnen, van hamsterhuisjes tot het Guh-Circuit.",
                   "All buildings and places that generate in the world, from hamster houses to the Guh-Circuit."),
    "biomen": ("biomen", "Biomen", "Biomes", "Alle biomen", "All biomes",
               "De biomen van de Guhmensie, het Guheinde, de Guhmaag en de Barbecuether.",
               "The biomes of the Guhmension, the Guheinde, the Guhmaag and the Barbecuether."),
    "dimensies": ("dimensies", "Dimensies", "Dimensions", "Alle dimensies", "All dimensions",
                  "De vier werelden van Guhs.", "The four worlds of Guhs."),
    "minigames": ("minigames", "Minigames", "Minigames", "Alle minigames", "All minigames",
                  "Alle spelletjes: de klassiekers, de Knuffeldal-spelletjes, De Grote Guhspelen en de spellen uit de guhverhalen.",
                  "All the games: the classics, the Knuffeldal games, the Grote Guhspelen and the games from the guh stories."),
    "verhalen": ("verhalen", "Verhalen", "Stories", "Alle verhalen", "All stories",
                 "De guhverhalen en questlines, stap voor stap, en de hoofdstukken van het FTB-questboek.",
                 "The guh stories and questlines, step by step, and the chapters of the FTB quest book."),
    "systemen": ("systemen", "Systemen", "Mechanics", "Alle systemen", "All mechanics",
                 "Hoe alles werkt: temmen, hartjes, het guhhuisje, kleding, de Guhdex, het superkompas, emotes en meer.",
                 "How everything works: taming, hearts, the guh house, clothes, the Guhdex, the super compass, emotes and more."),
    "kleding": ("kleding", "Kleding", "Clothes", "Alle kleding", "All clothes",
                "Elk kledingstuk en elke outfit voor je guh, met waar je het krijgt en wat het kost.",
                "Every piece of clothing and every outfit for your guh, with where to get it and what it costs."),
}
CAT_ORDER = ["guhs", "npcs", "diertjes", "wezens", "bouwwerken", "biomen", "dimensies", "minigames", "verhalen", "systemen",
             "kleding", "items", "blokken"]
CAT_ICON = {"guhs": "guh", "npcs": "npc_reisguh", "diertjes": "critter_pluisvinkje", "wezens": "mika", "items": "icon_kaas_knabbels",
            "blokken": "block_of_kaasknabbels", "bouwwerken": "structure_guh_kasteel", "biomen": "roze_gras",
            "dimensies": "guh_portal", "minigames": "icon_discomunt", "verhalen": "icon_timmerguh_bouwboekje",
            "systemen": "icon_guhdex", "kleding": "guh_outfit_party"}


def t(en, nl):
    """Inline text in both languages (the CSS shows the chosen one)."""
    if en == nl:
        return nl
    return f'<span lang="en">{en}</span><span lang="nl">{nl}</span>'


def p(en, nl):
    if en == nl:
        return f"<p>{nl}</p>"
    return f'<p lang="en">{en}</p><p lang="nl">{nl}</p>'


def esc(s):
    return html.escape(s or "", quote=True)


def fold(s):
    """Lower case, no accents: for search and matching."""
    s = unicodedata.normalize("NFKD", s or "")
    return "".join(c for c in s if not unicodedata.combining(c)).lower()


def slug(s):
    s = fold(s)
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return s or "pagina"


# Public page names: a few ids keep an old internal name (for save compatibility); the page URL uses the in-game name.
PUBLIC_SLUG = [(re.compile(r"mewtwo"), "guhtwo"), (re.compile(r"(?<![a-z])mew(?![a-z])|(?<=_)mew(?=_)|^mew(?=_)|(?<=-)mew(?=-)"), "mieuwguh"),
               (re.compile(r"shuckle"), "sjokkel")]


def public_slug(s):
    for rx, new in PUBLIC_SLUG:
        s = rx.sub(new, s)
    return s


class Page:
    def __init__(self, cat, pid, title, title_en=None, kind_nl="", kind_en="", thumb=None):
        self.cat = cat
        self.id = pid                   # unique: "<cat>/<slug>"
        self.title = title              # the (Dutch, in-game) name
        self.title_en = title_en or title
        self.kind_nl, self.kind_en = kind_nl, kind_en
        self.thumb = thumb              # picture name for lists
        self.images = []                # extra pictures for the gallery
        self.infobox = []               # [(label_en, label_nl, value html)]
        self.lead_en, self.lead_nl = "", ""   # the first paragraph (plain-ish html)
        self.sections = []              # [(key, h_en, h_nl, html)]
        self.chunks = []                # knowledge-base chunks put on this page
        self.related = []               # page ids
        self.aliases = set()            # names that link here
        self.claims = set()             # picture names that identify this page
        self.columns = {}               # extra columns in the category table: {key: (sort value, html)}
        self.search_extra = ""
        self.data = {}                  # anything the builders want to keep
        self.parent = None              # (page id) for the breadcrumb
        self.no_autolink = False

    @property
    def dir(self):
        return CATEGORIES[self.cat][0] if self.cat in CATEGORIES else ""

    @property
    def path(self):
        if self.cat == "home":
            return "index.html"
        return f"{self.dir}/{public_slug(self.id.split('/', 1)[1])}.html"

    def add_section(self, key, h_en, h_nl, body):
        if body and body.strip():
            self.sections.append((key, h_en, h_nl, body))

    def info(self, en, nl, value):
        if value not in (None, "", []):
            self.infobox.append((en, nl, value))

    def __repr__(self):
        return f"<Page {self.id}>"


class Site:
    def __init__(self):
        self.pages = {}

    def add(self, page):
        if page.id in self.pages:
            raise ValueError("duplicate page " + page.id)
        self.pages[page.id] = page
        page.aliases.add(page.title)
        return page

    def get(self, pid):
        return self.pages.get(pid)

    def by_cat(self, cat):
        return [p for p in self.pages.values() if p.cat == cat]

    def claimed(self, image):
        return self._claims.get(image)

    def index_claims(self):
        self._claims = {}
        order = ["guhs", "npcs", "diertjes", "wezens", "bouwwerken", "biomen", "dimensies", "minigames", "verhalen", "systemen", "kleding",
                 "blokken", "items"]
        for pg in sorted(self.pages.values(), key=lambda x: order.index(x.cat) if x.cat in order else 99):
            for c in pg.claims:
                self._claims.setdefault(c, pg)
