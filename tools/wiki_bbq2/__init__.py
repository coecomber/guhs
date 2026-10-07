"""
The wiki of bbq2 (Guh-technologie, the Guhbarbecuether buildings, In de ban van de Knabbelring, Super Guhrio): the docs step
that turns the slices' notes into wiki pages.

Every slice wrote tools/features/<module>_wiki.py with one dict WIKI (Dutch only; CONTRACT_130 2.4):
    "verhalen":    {slug: dict(nl=, img=, lead_nl=, ftb=[(chapter, sid)], structure=, npcs=[...], related=[...])}
    "systemen":    {slug: (Dutch title, picture, Dutch lead, [related])}
    "npc_home":    {kind: page}          "entity_home": {entity: page}
    "tekst":       [(page, "Kopje", "Dutch paragraphs, plain text")]
load(root) reads EVERY tools/features/*_wiki.py (a later slice's file joins in by itself), puts the page names in one shape,
adds the English of tools/wiki_bbq2/en.py and the docs step's own tables below (spoiler rules, better pictures, a few extra
paragraphs), and hands the result to
  * tools/make_wiki.py bbq2_sections(): the knowledge base (one section per page, so tools/wiki_site/kb.py cuts it into chunks);
  * tools/wiki_site/topics.py extend(): the story and mechanic pages themselves and where the chunks go.
The pictures are made by tools/wiki_bbq2/renders.py (python tools/wiki_renders.py docs/wiki/img --only-bbq2).

English: tools/wiki_bbq2/en.py holds a hand-written English text for every Dutch title, lead, heading and paragraph, with the
names of tools/lang/GLOSSARY.md section 24. A Dutch text without English is shown in Dutch on the English side and listed in
the build's notes; so is an English text whose Dutch changed since it was written (tools/wiki_bbq2/en_hash.json remembers the
Dutch it was written for: `python tools/wiki_bbq2/check.py` lists both, `--stamp` records the present Dutch after a review).

THE SPOILER RULE (user decision): a story page tells how to start and follow a story, not its surprises. So
  * SPOILER_TEKST paragraphs (puzzle answers, what happens at the end, who comes back) sit behind a toggle;
    a SPOILER_KOP paragraph takes its heading with it behind the toggle (the heading is the surprise);
  * the quest lists of the later chapters sit behind a toggle (SPOILER_STAPPEN);
  * SPOILER_PAGINA pages (the later chapters' places, the Barbecuerog, the Eye, the castle's boss) show no picture in any
    list, card or search result, and their own picture sits behind a toggle;
  * a story page's own picture is never a later chapter's place: PLAATJE gives it a character or an item instead.
"""
import hashlib
import importlib
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))

# the notes in page order (a *_wiki.py that is not listed here comes after these, in alphabetical order)
VOLGORDE = ["vadskracht", "tech_bronnen", "tech_buizen", "tech_machines", "tech_vloeistof", "tech_bezorg", "bank", "tech_klusjes", "tech_quests",
            "fossiel_mijn", "bestaand", "paleizen", "sausdieren", "camping_markt", "toren_peper",
            "ring", "ring_h1", "ring_h2", "ring_h3", "ring_h4", "ring_h5", "ring_h6", "ring_sausuman", "ring_knipogen",
            "guhrio", "guhrio_w1", "guhrio_w2", "guhrio_w3", "guhrio_beloning"]
# the four groups of the one-page wiki: (id, English, Dutch, the modules in it); a module in no group goes to the last one
GROEPEN = [("bbq2_tech", "Guh Technology", "Guh-technologie",
            ["vadskracht", "tech_bronnen", "tech_buizen", "tech_machines", "tech_vloeistof", "tech_bezorg", "bank", "tech_klusjes", "tech_quests"]),
           ("bbq2_gebouwen", "The Guh Barbecuether: new buildings", "De Guhbarbecuether: nieuwe gebouwen",
            ["fossiel_mijn", "bestaand", "paleizen", "sausdieren", "camping_markt", "toren_peper"]),
           ("bbq2_ring", "The Lord of the Nibble Ring", "In de ban van de Knabbelring",
            ["ring", "ring_h1", "ring_h2", "ring_h3", "ring_h4", "ring_h5", "ring_h6", "ring_sausuman", "ring_knipogen"]),
           ("bbq2_guhrio", "Super Guhrio", "Super Guhrio", ["guhrio", "guhrio_w1", "guhrio_w2", "guhrio_w3", "guhrio_beloning"]),
           ("bbq2_meer", "More new things", "Nog meer nieuws", [])]

# --- the docs step's own corrections to the notes ----------------------------------------------------------------------------
# a page the notes name in another way -> the page
ANDERS = {"dieren/worstzwijntje": "diertjes/worstzwijntje", "bank-guh-buikje": "systemen/bank-guh-buikje"}
# page -> picture (instead of the note's): a story's own picture is never a later chapter's place, the duel's is not the boss
PLAATJE = {"verhalen/knabbelring": "icon_knabbelring", "verhalen/ring-h2": "npc_guhrond", "verhalen/ring-h3": "npc_gimguh",
           "verhalen/ring-h4": "npc_guhladriel", "verhalen/ring-h5": "npc_boromika", "verhalen/ring-h6": "guh_variant_sam_guh",
           "verhalen/ring-sausuman": "npc_sausuman", "verhalen/super-guhrio-duel": "npc_perzikguh", "verhalen/pad-guhs-kraam": "npc_padguh",
           "systemen/oog-van-sausron": "icon_oog_van_sausron_beeldje", "systemen/super-guhrio-spelen": "guhmba",
           "systemen/bezorgguhtje": "bezorgguhtje", "systemen/sausdieren": "sausloper", "systemen/rustpunten": "structure_ring_rustpunt",
           "systemen/guh-technologie": "structure_oude_guhrad_centrale", "systemen/guhmachines": "block_knabbelaar",
           "systemen/vadskrachtbronnen": "block_knuffelgenerator", "systemen/knabbelbuizen": "block_knabbelbuis_filter",
           "systemen/sensoren": "block_snuffelsensor", "systemen/saus": "block_sauspomp", "systemen/bank-guh-buikje": "block_hapluikje",
           "systemen/zoutkristal": "icon_zoutkristal", "systemen/pepers": "icon_torenpeper_vahoegpeper",
           "systemen/gaven-van-guhladriel": "icon_lichtflesje", "systemen/knabbelring": "icon_knabbelring",
           "systemen/tentdoek": "block_campingmarkt_tentdoek_geel_trap", "systemen/groene-reispijp": "block_guhriobeloning_pijp",
           "verhalen/knabbelmachine": "block_knabbelmachine_beeldje"}
# page -> the structure its story starts at (instead of the note's)
BEGINT_BIJ = {"verhalen/knabbelring": "knabbelgouw"}
# page -> a Dutch lead without the story's surprises (instead of the note's); its English is in en.py like any lead
LEAD = {
    "verhalen/ring-h3": "Hoofdstuk 3 van In de ban van de Knabbelring. Onder de Houtskoolvlakte ligt de oude mijn van de dwerg-guhs. Je komt er "
                        "alleen in met het goede woord, en je komt er alleen door met vier hefbomen, een geheime deur en een brug over een "
                        "diepe kloof. Met het hele gezelschap, in het donker.",
    "verhalen/ring-h5": "Hoofdstuk 5 van In de ban van de Knabbelring. De poort naar het land van Sausron is een rooster zo hoog als een huis, en "
                        "hij zit dicht. Vanaf zijn toren staart het Oog van Sausron het dal in. Je sluipt eromheen: over het Asveld, de Kale "
                        "Vlakte en door de Schaduwlaan naar het Roosterpoortje. Wie gezien wordt, staat weer bij het laatste rustvuurtje.",
    "verhalen/ring-h6": "Het laatste hoofdstuk van In de ban van de Knabbelring. In de Rookdelta staat de Frituurberg: een vulkaan met een korst "
                        "van gefrituurd beslag. Je klimt het Kronkelpad op terwijl de berg met kooltjes gooit, bevrijdt drie Rookguhjes die de "
                        "Mika's als afzuigkap gebruiken, trekt jezelf met het Elfentouw langs de westwand omhoog en laat je het laatste stuk "
                        "door Sam-guh dragen. Wat er boven gebeurt, moet je zelf zien.",
    "verhalen/ring-sausuman": "De extra halte van In de ban van de Knabbelring. In een zwarte toren vol sputterende machines woont Sausuman van "
                              "de Vele Sauzen, een tovenaar-Mika die ook een hapje van de ring wil. Hij krijgt het niet, dus wil hij er zelf "
                              "een bakken. Jij mag de ingrediënten halen.",
    "verhalen/super-guhrio-duel": "Achter de grote poort van de levelhal wacht het duel met de Grote Nether-Mika: drie rondes op zijn roosterbrug "
                                  "boven de frituursaus. Hij duwt alleen maar, njeg: wie geraakt wordt, staat weer bij zijn vlaggetje.",
}
# (page, Kopje) of the paragraphs behind a spoiler toggle
SPOILER_TEKST = {
    ("verhalen/knabbelring", "Na het verhaal"),
    ("verhalen/ring-h3", "De Hal van de Hefbomen"), ("verhalen/ring-h3", "De put en de geheime doorgang"),
    ("verhalen/ring-h3", "De Barbecuerog"), ("verhalen/ring-h3", "Het Brokkelpad en de brug"),
    ("verhalen/ring-h5", "Boromika"), ("verhalen/ring-h5", "Het Wachthek"), ("verhalen/ring-h5", "Het Roosterpoortje"),
    ("verhalen/ring-h6", "Het einde"),
    ("verhalen/ring-sausuman", "De questlijn"), ("verhalen/ring-sausuman", "Wat krijg je?"),
    ("systemen/oog-van-sausron", "Na het verhaal"),
    ("verhalen/super-guhrio", "De geheimen van de binnentuin"), ("verhalen/super-guhrio", "De vadsmunt in de rode kooi"),
    ("verhalen/super-guhrio", "De warpkamer"),
    ("verhalen/super-guhrio-duel", "Ronde 1: de hendel"), ("verhalen/super-guhrio-duel", "Ronde 2 en 3: het schild"),
    ("verhalen/super-guhrio-duel", "Hoe het afloopt"),
    ("verhalen/pad-guhs-kraam", "Bedankt! Maar de prinses..."), ("bouwwerken/guhrio_kasteel", "De torenkamer"),
}
# (page, Kopje) whose heading itself is the surprise: it goes inside the toggle too
SPOILER_KOP = {("verhalen/ring-h3", "De Barbecuerog")}
# story pages whose list of FTB quests sits behind a toggle (every step of a later chapter is a spoiler)
SPOILER_STAPPEN = {"verhalen/knabbelring", "verhalen/ring-h2", "verhalen/ring-h3", "verhalen/ring-h4", "verhalen/ring-h5", "verhalen/ring-h6",
                   "verhalen/ring-sausuman", "verhalen/super-guhrio", "verhalen/super-guhrio-duel", "verhalen/pad-guhs-kraam"}
# FTB chapters whose quest-book page hides every section behind a toggle
SPOILER_FTB = {"guhs_knabbelring", "guhs_guhrio"}
# pages without a picture in lists, cards and the search, and with their own picture behind a toggle
SPOILER_PAGINA = {"bouwwerken/guhvendel", "bouwwerken/knabbelmoria", "bouwwerken/guhladriel_boomstad", "bouwwerken/zwarte_roosterpoort",
                  "bouwwerken/frituurberg", "bouwwerken/sausuman_toren", "wezens/barbecuerog", "wezens/grote_nether_mika",
                  "wezens/oog_van_sausron"}
# entities without a page: thrown things, moving parts of a level, and the one that would give the ending away
GEEN_PAGINA = {"ringh6_valkool", "ringh6_krokante_smikagol", "guhrio_grillspies", "guhrio_knabbel", "guhrio_platform", "guhrio_valblok",
               "guhriow3_kooltje", "guhriow3_taart"}
DIERTJES = ["worstzwijntje"]                                  # (farm animals: the rest of the new creatures are "wezens")
BAZEN = {"barbecuerog", "grote_nether_mika"}
VERHAALGUHS = {"sam_guh": "verhalen/knabbelring", "guhshi": "verhalen/pad-guhs-kraam"}     # Guhdex creatures that are tameable guhs
# entity -> where it is, on top of the notes
ENTITY_HOME = {"knekel_ruiter": "systemen/knabbelring", "guhmba": "systemen/super-guhrio-spelen", "schild_mika": "systemen/super-guhrio-spelen",
               "plof_mika": "systemen/super-guhrio-spelen", "hapbloem": "systemen/super-guhrio-spelen", "sausblubje": "systemen/sausdieren",
               "sausloper": "systemen/sausdieren", "bezorgguhtje": "systemen/bezorgguhtje", "barbecuerog": "verhalen/ring-h3",
               "grote_nether_mika": "verhalen/super-guhrio-duel", "ringh6_rookguh": "verhalen/ring-h6"}
# NPC kind -> its story, on top of the notes' own "npcs" lists (the first story that names a character wins)
NPC_VERHAAL = {"guhdalf": "knabbelring", "smikagol": "knabbelring", "araguh": "knabbelring", "leguhlas": "knabbelring", "gimguh": "knabbelring",
               "boromika": "knabbelring", "merrie": "knabbelring", "pippguh": "knabbelring", "padguh": "pad-guhs-kraam",
               "perzikguh": "pad-guhs-kraam", "uitvinderguh": "techniek"}
# the structures the features add to the Superkompas tab "Barbecue" from code (SuperkompasItem.voegToe; the hidden story places
# only show up there once the player's story has reached them)
SUPERKOMPAS = {"barbecue": ["knabbelgouw", "ring_rustpunt", "oude_guhrad_centrale", "zoutkristalmijn", "fossiel_opgraving", "sausloper_stal",
                            "grillcamping", "mika_ruilmarkt", "rookguh_vuurtoren", "pepertuin", "mika_woonblokken", "mika_stal",
                            "mika_brugpaleis", "guhrio_kasteel", "guhvendel", "sausuman_toren"]}
# clothing source -> the page it comes from
BRON_PAGINA = {"ring": "verhalen/knabbelring", "guhrio_beloning": "verhalen/pad-guhs-kraam", "paleizen": "verhalen/mika-oma",
               "bestaand": "verhalen/wachter", "camping_markt": "verhalen/camping", "toren_peper": "verhalen/vuurtoren"}
# pictures that belong to a structure page but are not called structure_<id>
STRUCTURE_PICS = {}
# page -> more "see also" pages than its note names (the notes were written side by side and do not know each other)
VERWANT = {
    "systemen/vadskracht": ["systemen/vadskrachtbronnen", "systemen/guhmachines", "systemen/knabbelbuizen", "systemen/sensoren", "systemen/saus",
                            "systemen/bezorgguhtje", "systemen/guh-technologie", "blokken/guh_wheel", "blokken/guh_wire", "blokken/guh_oven"],
    "systemen/guh-technologie": ["systemen/vadskracht", "systemen/vadskrachtbronnen", "systemen/guhmachines", "systemen/knabbelbuizen",
                                 "systemen/sensoren", "systemen/saus", "systemen/bezorgguhtje", "systemen/bank-guh-buikje", "systemen/zoutkristal",
                                 "bouwwerken/oude_guhrad_centrale"],
    "systemen/vadskrachtbronnen": ["blokken/guh_wheel", "blokken/knuffelgenerator", "blokken/disco_dynamo", "blokken/blubkacheltje",
                                   "blokken/gloeisterkern", "blokken/knabbelbatterij"],
    "systemen/bank-guh-buikje": ["systemen/guhhuisje"],
    "verhalen/knabbelring": ["verhalen/ring-h1", "verhalen/ring-h2", "verhalen/ring-h3", "verhalen/ring-h4", "verhalen/ring-h5", "verhalen/ring-h6",
                             "verhalen/ring-sausuman", "guhs/sam_guh", "dimensies/barbecuether"],
    "verhalen/ring-h1": ["verhalen/ring-h2"], "verhalen/ring-h2": ["verhalen/ring-h3"], "verhalen/ring-h4": ["verhalen/ring-h5", "verhalen/ring-sausuman"],
    "verhalen/ring-h5": ["verhalen/ring-h6"],
    "verhalen/super-guhrio": ["verhalen/super-guhrio-duel", "verhalen/pad-guhs-kraam", "bouwwerken/guhrio_kasteel", "guhs/guhshi"],
    "verhalen/pad-guhs-kraam": ["guhs/guhshi"],
    "verhalen/stalknecht": ["diertjes/worstzwijntje"],
    "verhalen/sausloper": ["wezens/sausloper", "wezens/sausblubje"],
    "systemen/sausdieren": ["wezens/sausloper", "wezens/sausblubje", "bouwwerken/sausloper_stal"],
    "systemen/bezorgguhtje": ["wezens/bezorgguhtje", "blokken/stepstation", "blokken/haltepaaltje", "verhalen/vuurtoren"],
}
# dimension -> pages under its "see also"
DIMENSIE_VERWANT = {"barbecuether": ["verhalen/knabbelring", "verhalen/super-guhrio", "systemen/guh-technologie", "systemen/sausdieren",
                                     "systemen/zoutkristal", "systemen/pepers", "systemen/rustpunten", "verhalen/grillguh"]}


def sha(text):
    return hashlib.sha1(re.sub(r"\s+", " ", text).strip().encode("utf-8")).hexdigest()[:10]


class Bbq2:
    """The joined notes. verhalen / systemen: {page id: dict}; tekst: [dict(pagina, kopje, kopje_en, nl, en, spoiler, module)];
    npc_home / entity_home: {id: page ref}; notes: what the build should tell (no English, unknown page...)."""

    def __init__(self):
        self.verhalen, self.systemen = {}, {}
        self.npc_home, self.entity_home, self.npc_verhaal = {}, dict(ENTITY_HOME), {}
        self.tekst, self.notes, self.modules = [], [], []
        self.bron = {}                  # hash key -> the Dutch an English text belongs to (check.py --stamp)

    def pagina(self, ref):
        """A page name of the notes in one shape: 'verhalen/x', 'systemen/x', 'bouwwerken/x'...; a bare name is a story or a
        mechanic of ours when there is one, else an item or block (items/<id>: the site knows which of the two it is)."""
        ref = ANDERS.get(ref, ref)
        if "/" in ref:
            cat, slug = ref.split("/", 1)
            if cat == "entity":
                return f"wezens/{slug}"
            return f"{cat}/{slug.replace('_', '-')}" if cat in ("verhalen", "systemen") else ref
        slug = ref.replace("_", "-")
        if f"verhalen/{slug}" in self.verhalen:
            return f"verhalen/{slug}"
        if f"systemen/{slug}" in self.systemen or slug in BESTAANDE_SYSTEMEN:
            return f"systemen/{slug}"
        return f"items/{ref}"

    def groep(self, module):
        for gid, _, _, mods in GROEPEN:
            if module in mods:
                return gid
        return GROEPEN[-1][0]


BESTAANDE_SYSTEMEN = {"guhhuisje", "klusjes", "guhdex", "superkompas", "brouwen", "ftb-quests", "temmen", "kleding"}


def _modules(root):
    d = os.path.join(root, "tools", "features")
    found = sorted(f[:-8] for f in os.listdir(d) if f.endswith("_wiki.py"))
    return [m for m in VOLGORDE if m in found] + [m for m in found if m not in VOLGORDE]


def load(root):
    """Reads every tools/features/*_wiki.py of the project at root (from root: the notes read numbers from the Java sources)."""
    from . import en as EN
    from . import eigen
    tools = os.path.join(root, "tools")
    if tools not in sys.path:
        sys.path.insert(0, tools)
    here = os.getcwd()
    b = Bbq2()
    raw = []
    try:
        os.chdir(root)
        for m in _modules(root):
            try:
                raw.append((m, importlib.import_module(f"features.{m}_wiki").WIKI))
            except Exception as e:  # noqa: BLE001  (a note that does not load must not stop the whole wiki)
                b.notes.append(f"bbq2: tools/features/{m}_wiki.py could not be read: {e!r}")
    finally:
        os.chdir(here)
    raw.append(("eigen", eigen.WIKI))
    b.modules = [m for m, _ in raw]
    # the pages first (so a bare name in a text or a "related" list can be told apart), then everything that points at them
    for m, w in raw:
        for slug, st in (w.get("verhalen") or {}).items():
            b.verhalen[f"verhalen/{slug.replace('_', '-')}"] = dict(st, module=m)
        for slug, sy in (w.get("systemen") or {}).items():
            b.systemen[f"systemen/{slug.replace('_', '-')}"] = dict(nl=sy[0], img=sy[1], lead_nl=sy[2], related=list(sy[3]), module=m)
    stamps = hashes()
    stale = []

    def english(key, nl, what):
        e = EN.TEKST.get(key) if what == "tekst" else EN.PAGINA.get(key)
        hk = "|".join(key) if isinstance(key, tuple) else key
        b.bron[hk] = nl
        if e is None:
            b.notes.append(f"bbq2: no English for the {what} of {hk} (shown in Dutch on the English side)")
            return None
        if hk in stamps and stamps[hk] != sha(nl):
            stale.append(hk)
        return e

    for pid, pg in list(b.verhalen.items()) + list(b.systemen.items()):
        pg["lead_nl"] = LEAD.get(pid, pg["lead_nl"])
        e = english(pid, pg["nl"] + "\n" + pg["lead_nl"], "page")
        pg["en"], pg["lead_en"] = e if e else (pg["nl"], "")
        pg["img"] = PLAATJE.get(pid, pg.get("img"))
        pg["related"] = [b.pagina(r) for r in pg.get("related", [])] + VERWANT.get(pid, [])
        if pid in BEGINT_BIJ:
            pg["structure"] = BEGINT_BIJ[pid]
        for kind in pg.get("npcs", []):
            b.npc_verhaal.setdefault(kind, pid.split("/", 1)[1])
    for kind, slug in NPC_VERHAAL.items():
        b.npc_verhaal[kind] = slug
    for m, w in raw:
        for kind, ref in (w.get("npc_home") or {}).items():
            b.npc_home[kind] = b.pagina(ref)
        for eid, ref in (w.get("entity_home") or {}).items():
            b.entity_home.setdefault(eid, b.pagina(ref))
        for row in w.get("tekst") or []:
            ref, kopje, nl = row[:3]
            pid = b.pagina(ref)
            if m == "eigen":            # the docs step's own paragraphs carry their English with them (eigen.py)
                kopje_en, en = row[3], row[4]
            else:
                e = english((pid, kopje), kopje + "\n" + nl, "tekst")
                kopje_en, en = e if e else (kopje, None)
            b.tekst.append(dict(pagina=pid, kopje=kopje, kopje_en=kopje_en, nl=nl, en=en, spoiler=(pid, kopje) in SPOILER_TEKST,
                                kop_verborgen=(pid, kopje) in SPOILER_KOP, module=m))
    for hk in stale:
        b.notes.append(f"bbq2: the Dutch of {hk} changed since its English was written: check tools/wiki_bbq2/en.py, "
                       f"then python tools/wiki_bbq2/check.py --stamp")
    return b


def sectie(pid):
    """The knowledge-base section of a page's bbq2 texts (tools/make_wiki.py bbq2_sections, tools/wiki_site/topics.py extend)."""
    return "bbq2_" + re.sub(r"[^a-z0-9]+", "_", pid.lower())


# pages of the wiki that are not ours but get paragraphs: their name in the one-page wiki (English, Dutch)
ANDERE_PAGINA = {
    "systemen/guhhuisje": ("The guh house: new chores", "Het Guhhuisje: nieuwe klusjes"),
    "systemen/guhdex": ("The Guhdex: what is new", "De Guhdex: wat is er nieuw"),
    "systemen/superkompas": ("The Super Compass: My Story", "Het Superkompas: Mijn verhaal"),
    "dimensies/barbecuether": ("The Guh Barbecuether: what is new", "De Guhbarbecuether: wat is er nieuw"),
    "bouwwerken/barbecueput": ("The big barbecue pit", "De grote barbecueput"),
    "bouwwerken/spiesburcht": ("The Skewer Keep: the Guard Guh", "De Spiesburcht: de Wachter-guh"),
    "bouwwerken/mika_grillpaleis": ("The Mika grill palace: the Plushie Maker Guh", "Het Mika-grillpaleis: de Knuffelmaker-guh"),
    "bouwwerken/mika_woonblokken": ("The Mika Apartments", "De Mika-woonblokken"),
    "bouwwerken/mika_stal": ("The Mika Stable", "De Mika-stal"),
    "bouwwerken/mika_brugpaleis": ("The Mika Bridge Palace", "Het Mika-brugpaleis"),
    "bouwwerken/guhvendel": ("Guhvendell", "Guhvendel"),
    "bouwwerken/guhrio_kasteel": ("Big Nether Mika's Castle", "Het Kasteel van de Grote Nether-Mika"),
    "diertjes/worstzwijntje": ("The Sausage Piglet", "Het Worstzwijntje"),
}


def hashes():
    path = os.path.join(HERE, "en_hash.json")
    if not os.path.exists(path):
        return {}
    with open(path, encoding="utf-8") as f:
        return json.load(f)


_cache = {}


def cached(root):
    root = os.path.abspath(root)
    if root not in _cache:
        _cache[root] = load(root)
    return _cache[root]
