"""
Builds every page of the site from the game data (gamedata.Game), the knowledge base (kb.extract) and the hand tables
(topics). Links are written as tokens (href="@@page id@@", src="@img:name@") that render.py turns into relative paths.
"""
import html
import re

from . import topics as T
from .kb import plain, split_t
from .site import MOD_VERSION, CATEGORIES, CAT_ICON, Page, Site, esc, fold, p, slug, t

SITE_VERSION = MOD_VERSION
# the Minecraft/GeckoLib line of this branch (mc26 = Guhs 1.1.x; main = 1.0.x for Minecraft 1.21.1 / GeckoLib 4.8+)
SITE_MC = "26.1.2"
SITE_GECKOLIB = "5.5.2+"

SLOTS = {"head": ("Head", "Hoofd"), "eyes": ("Eyes", "Ogen"), "body": ("Body", "Lijf"), "neck": ("Neck", "Nek"), "back": ("Back", "Rug"),
         "haar": ("Hair", "Haar"), "oren": ("Ears", "Oren")}
BONES = {"neck": ("a very long neck", "een heel lange nek"), "teckel": ("six legs and an extra long body", "zes pootjes en een extra lang lijf"),
         "ender": ("dragon wings: it flies", "drakenvleugels: hij vliegt"), "koning": ("crown, mane and moustache", "kroon, manen en snorretje"),
         "wolk": ("a little cloud on its head", "een wolkje op zijn hoofd"), "zeemeer": ("a fish tail: it swims", "een vissenstaart: hij zwemt"),
         "asguh": ("glowing cheeks, fire can't hurt it", "gloeiende wangetjes, vuur doet hem niks"), "pluis": ("an extra fluffy tuft", "een extra pluizig kuifje"),
         "pinguh": ("a penguin suit", "een pinguinpakje"), "balto": ("wolf ears and a wolf tail", "wolfsoortjes en een wolfsstaart"),
         "mewtwo": ("a long tail and a neck tube", "een lange staart en een nekbuisje"), "stitch": ("big ears and an extra pair of arms", "grote oren en een extra paar armpjes")}
SPAWN_GROUP = {"creature": ("animal", "dier"), "monster": ("monster", "monster"), "water_creature": ("water animal", "waterdier"),
               "water_ambient": ("water critter", "waterdiertje"), "ambient": ("ambient", "omgeving"), "underground_water_creature": ("cave water", "grotwater"),
               "axolotls": ("axolotl", "axolotl"), "misc": ("other", "overig"), "modifier": ("extra spawn", "extra spawn")}
DIM_NAMES = {k: (v[1], v[0]) for k, v in T.DIMENSIONS.items()}   # id -> (en, nl)
SUPERKOMPAS_TABS = {"avontuur": ("Adventure", "Avontuur"), "quests": ("Quests", "Quests"), "minigames": ("Minigames", "Minigames"),
                    "wonderen": ("Wonders", "Wonderen"), "wonen": ("Homes", "Wonen"), "einde": ("End", "Einde"),
                    "ondergrond": ("Underground", "Ondergrond"), "barbecue": ("Barbecue", "Barbecue"), "knus": ("Cosy", "Knus"),
                    "verhalen": ("Stories", "Verhalen")}
RECIPE_KIND = {"smelting": ("furnace", "oven"), "blasting": ("blast furnace", "hoogoven"), "smoking": ("smoker", "roker"),
               "campfire_cooking": ("campfire", "kampvuur"), "stonecutting": ("stonecutter", "steenzaag"), "smithing_transform": ("smithing", "smeden")}


def L(pid, label=None):
    """A link to a page (label None: the page's own title, filled in by render.py)."""
    return f'<a href="@@{pid}@@">{"@T@" if label is None else label}</a>'


def rarity_label(spacing):
    if spacing is None:
        return None
    for limit, en, nl in ((10, "very common", "heel vaak"), (20, "common", "vaak"), (32, "uncommon", "ongewoon"), (48, "rare", "zeldzaam"),
                          (80, "very rare", "heel zeldzaam")):
        if spacing <= limit:
            return en, nl
    return "legendary", "legendarisch"


def strip_prefix(s, *prefixes):
    for pre in prefixes:
        if s.lower().startswith(pre.lower()):
            return s[len(pre):].strip()
    return s


def first_sentences(text, n=240):
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) <= n:
        return text
    cut = text[:n]
    i = max(cut.rfind(". "), cut.rfind("! "), cut.rfind("? "))
    return cut[:i + 1] if i > 60 else cut.rsplit(" ", 1)[0] + "..."


def clean_title(s):
    """Takes the release framing out of a heading ('New biome: X' -> 'X', 'Barbecueput (2.7)' -> 'Barbecueput')."""
    s = re.sub(r"\s*\((?:new in |nieuw in )?\d\.\d+(?:\.\d+)?\)", "", s)
    s = re.sub(r"^(?:New|Nieuwe?)\s+(?:biome|bioom|\w+)?:\s*", "", s)
    s = re.sub(r"^(?:New in|Nieuw in)\s+\d\.\d+(?:\.\d+)?:?\s*", "", s)
    s = re.sub(r"\b(?:new|nieuwe|nieuw)\s+", "", s)
    s = re.sub(r"^[A-J] &middot; ", "", s)
    return s[:1].upper() + s[1:] if s else s


class Builder:
    def __init__(self, game, chunks, images):
        self.g, self.chunks, self.im = game, chunks, images
        self.site = Site()
        self.report = dict(unassigned=[], dropped=0, assigned=0, notes=[])
        self.item_page = {}          # item id -> page id
        self.ftb_quest_page = {}     # quest key -> page id

    # --- helpers ---------------------------------------------------------------------------------------------------------------
    def img(self, name, alt="", cls="", big=False):
        if not self.im.has(name):
            return ""
        w, h = self.im.size(name)
        if max(w, h) <= 128:
            w, h = (64 * w // max(w, h), 64 * h // max(w, h)) if max(w, h) < 64 else (w, h)
        elif max(w, h) > 1280:
            f = 1280 / max(w, h)
            w, h = int(w * f), int(h * f)
        return (f'<img src="@img:{name}@" alt="{esc(alt)}" width="{w}" height="{h}" loading="lazy"'
                + (f' class="{cls}"' if cls else "") + ">")

    def icon(self, name, title=""):
        if not self.im.has(name):
            return ""
        return f'<img class="px" src="@img:{name}@" alt="{esc(title)}" title="{esc(title)}" width="32" height="32" loading="lazy">'

    def item_icon_name(self, rid):
        ns, path = rid.split(":", 1) if ":" in rid else ("guhs", rid)
        if ns == "guhs":
            return self.im.item_icon(path)
        return self.im.first(f"icon_{path}")

    def item_ref(self, rid, count=None, with_name=True):
        """An item: its icon + its name, linked to its page."""
        if rid.startswith("#"):
            return f'<span class="tagref">#{esc(rid[1:].split(":")[-1].replace("_", " "))}</span>'
        ns, path = rid.split(":", 1) if ":" in rid else ("guhs", rid)
        name = self.g.item_name(rid)
        ic = self.icon(self.item_icon_name(rid) or "", name)
        label = (ic + " " if ic else "") + (esc(name) if with_name else "")
        if count and count != 1:
            label += f' <span class="muted">&times;{count}</span>'
        pid = self.item_page.get(path) if ns == "guhs" else None
        return L(pid, label) if pid else label

    def slot_html(self, rid, count=None, big=False):
        cls = "slot big" if big else "slot"
        if not rid:
            return f'<span class="{cls}"></span>'
        name = self.g.item_name(rid) if not rid.startswith("#") else "#" + rid.split(":")[-1]
        ic = self.item_icon_name(rid) if not rid.startswith("#") else None
        inner = self.icon(ic, name) if ic else f'<span class="slot-txt" title="{esc(name)}">{esc(name[:3])}</span>'
        ns, path = rid.split(":", 1) if ":" in rid else ("guhs", rid)
        pid = self.item_page.get(path) if ns == "guhs" else None
        if pid:
            inner = f'<a href="@@{pid}@@" title="{esc(name)}">{inner}</a>'
        if count and count > 1:
            inner += f'<b class="count">{count}</b>'
        return f'<span class="{cls}">{inner}</span>'

    def recipe_html(self, r):
        res = self.slot_html(r["result"], r["count"], big=True)
        if r["grid"]:
            grid = "".join(self.slot_html(x) for x in r["grid"])
            tag = f'<span class="tag">{t("shapeless", "vormloos")}</span>' if r.get("shapeless") else ""
            return f'<div class="recipe"><div class="craft">{grid}</div><span class="arrow" aria-hidden="true"></span>{res}{tag}</div>'
        ins = "".join(self.slot_html(x) for x in r["inputs"])
        en, nl = RECIPE_KIND.get(r["type"], (r["type"], r["type"]))
        return f'<div class="recipe"><div class="inputs">{ins}</div><span class="arrow" aria-hidden="true"></span>{res}<span class="tag">{t(en, nl)}</span></div>'

    def resolve_pid(self, pid):
        """items/x and blokken/x are the same thing to the hand tables."""
        if pid and pid not in self.site.pages and pid.split("/")[0] in ("items", "blokken", "kleding"):
            iid = pid.split("/", 1)[1]
            return self.item_page.get(iid, pid)
        return pid

    def add(self, page):
        return self.site.add(page)

    def exists(self, pid):
        return pid in self.site.pages

    # --- build --------------------------------------------------------------------------------------------------------------
    def build(self):
        self.home_and_indexes()
        self.variants()
        self.npcs()
        self.clothes()
        self.items()
        self.entities()
        self.structures()
        self.biomes()
        self.dimensions()
        self.minigames()
        self.stories()
        self.ftb_chapters()
        self.systems()
        from .guide import Guide          # (here: guide.py uses this module's helpers)
        self.add(Guide(self).build())
        from .server import ServerPage
        self.add(ServerPage(self).build())
        self.site.index_claims()
        self.assign_chunks()
        self.table_rows()
        self.finish()
        return self.site

    def home_and_indexes(self):
        home = Page("home", "index", "Guhs Wiki", "Guhs Wiki")
        home.no_autolink = True
        self.add(home)
        for cat in CATEGORIES:
            d = CATEGORIES[cat]
            pg = Page(cat, f"{cat}/index", d[3], d[4], "Lijst", "List", CAT_ICON.get(cat))
            pg.lead_nl, pg.lead_en = d[5], d[6]
            pg.no_autolink = False
            pg.aliases = set()
            self.add(pg)
            pg.aliases = set()

    # --- guh variants ---------------------------------------------------------------------------------------------------------
    def variants(self):
        g = self.g
        roll = None
        for v in g.variants:
            if v["kind"] not in ("variant", "verhaal"):
                continue
            roll = roll or getattr(g, "variant_roll_out_of", 1000)
            vid = v["id"]
            name = "Guh" if vid == "normal" else g.lang_nl.get(f"entity.guhs.guh.{vid}", vid)
            name = name[:1].upper() + name[1:]
            if vid == "normal":
                kind = ("Base guh", "Basisguh")
            elif v["kind"] == "verhaal":
                kind = ("Story guh", "Verhaalguh")
            elif v["weight"] > 0:
                kind = ("Variant", "Variant")
            else:
                kind = ("Special guh", "Bijzondere guh")
            pics = [n for n in ("guh" if vid == "normal" else None, f"guh_variant_{vid}", f"guh_variant_{vid}_back",
                                "guh_front" if vid == "normal" else None, "guh_koning_pakje" if vid == "koning" else None) if n and self.im.has(n)]
            pics += sorted(n for n in self.im.available if n.startswith(f"guh_variant_{vid}_") and n not in pics)
            pics += sorted(n for n in self.im.available if re.fullmatch(rf"shot\d*_(?:dex|guhdex)_\d+_{vid}", n))
            pg = Page("guhs", f"guhs/{vid}", name, name, kind[1], kind[0], pics[0] if pics else "guh")
            pg.images = pics
            pg.claims = {n for n in pics if n not in ("guh",)}
            if vid == "pinguh":
                pg.claims |= {"pinguhs", "pinguhs_slapen"}
            rarity = strip_prefix(v["rarity"], "Zeldzaamheid:")
            pg.info("Kind", "Soort", t(*kind))
            pg.info("Rarity", "Zeldzaamheid", esc(rarity))
            if v["weight"] > 0:
                pct = 100 * v["weight"] / roll
                pg.info("Chance", "Kans", t(f"{v['weight']} in {roll} wild Guhmension guhs ({pct:.1f}%)".replace(".0%", "%"),
                                            f"{v['weight']} op {roll} wilde guhs in de Guhmensie ({pct:.1f}%)".replace(".", ",").replace(",0%", "%")))
            home = T.VARIANT_HOME.get(vid) or ("dimensies/guhmension" if v["weight"] > 0 or vid == "normal" else None)
            if home:
                pg.info("Where", "Waar", L(home))
                pg.related.append(home)
            pg.related += ["systemen/temmen", "systemen/guhdex"]
            extras = [BONES[b] for b in v["bones"] if b in BONES]
            if extras:
                pg.info("Special", "Bijzonder", t(", ".join(e[0] for e in extras), ", ".join(e[1] for e in extras)))
            pg.lead_nl = esc(v["info"])
            pg.data["guhdex"] = True
            if v["kind"] == "verhaal":
                pg.related.append("systemen/verhaalguhs")
            pg.data["sort"] = v["order"]
            pg.columns["rarity"] = (v["weight"] if v["weight"] else -1, esc(rarity))
            self.add(pg)
        if self.exists("guhs/normal"):
            self.site.pages["guhs/normal"].aliases |= {"guhs"}

    # --- characters ------------------------------------------------------------------------------------------------------------
    def npcs(self):
        g = self.g
        variant_by_id = {v["id"]: v for v in g.variants}
        minigame_by_npc = {}
        for mid, m in T.MINIGAMES.items():
            minigame_by_npc.setdefault(m.get("npc"), []).append(mid)
        for kind, scale in g.npc_kinds:
            name = g.lang_nl.get(f"entity.guhs.guh_npc.{kind}", kind.replace("_", " ").title())
            pic = self.im.first(f"npc_{kind}", "guh_sitting")
            pg = Page("npcs", f"npcs/{kind}", name, name, "Guh-personage", "Guh character", pic)
            pics = [n for n in [f"npc_{kind}"] if self.im.has(n)]
            pics += sorted(n for n in self.im.available if re.fullmatch(rf"shot\d*_(?:npc_{kind}(_.*)?|dex_\d+_{kind}|guhdex_\d+_{kind})", n))
            pg.images = pics
            pg.claims = {f"npc_{kind}"} & self.im.available
            v = variant_by_id.get(kind)
            pg.info("Kind", "Soort", t("Guh character", "Guh-personage"))
            if v:
                pg.info("Rarity", "Zeldzaamheid", esc(strip_prefix(v["rarity"], "Zeldzaamheid:")))
                pg.lead_nl = esc(v["info"])
            home = T.NPC_HOME.get(kind)
            if home:
                pg.info("Where", "Waar", L(home))
                pg.related.append(home)
            story = T.NPC_STORY.get(kind)
            if story:
                pg.info("Story", "Verhaal", L(f"verhalen/{story}"))
                pg.related.append(f"verhalen/{story}")
            for mid in minigame_by_npc.get(kind, []):
                pg.info("Minigame", "Minigame", L(f"minigames/{mid}"))
                pg.related.append(f"minigames/{mid}")
            if scale != 1.0:
                pg.info("Size", "Grootte", f"{scale:g}&times;".replace(".", ","))
            pg.data["sort"] = kind
            self.add(pg)
        for prof, name in g.villager_professions.items():
            pg = Page("npcs", f"npcs/{prof}", name, name, "Guhdorpeling", "Guh villager", self.im.first(f"villager_{prof}"))
            pg.images = [n for n in [f"villager_{prof}"] if self.im.has(n)]
            pg.claims = set(pg.images)
            pg.info("Kind", "Soort", t("Guh villager (profession)", "Guhdorpeling (beroep)"))
            home = T.NPC_HOME.get(prof, "bouwwerken/guh_village")
            pg.info("Where", "Waar", L(home))
            pg.related.append(home)
            self.add(pg)
        for eid, pic in T.NPC_ENTITIES.items():
            name = g.entity_names.get(eid)
            if not name or eid in T.NO_PAGE:
                continue
            pic = self.im.first(pic or "", eid, "guh_sitting")
            pg = Page("npcs", f"npcs/{eid}", name, name, "Guh-personage", "Guh character", pic)
            pg.images = [n for n in [eid] if self.im.has(n)]
            pg.claims = set(pg.images)
            v = variant_by_id.get(eid)
            pg.info("Kind", "Soort", t("Guh character", "Guh-personage"))
            if v:
                pg.info("Rarity", "Zeldzaamheid", esc(strip_prefix(v["rarity"], "Zeldzaamheid:")))
                pg.lead_nl = esc(v["info"])
            home = T.NPC_HOME.get(eid)
            if home:
                pg.info("Where", "Waar", L(home))
                pg.related.append(home)
            self.add(pg)

    # --- critters and mobs ------------------------------------------------------------------------------------------------------
    def entities(self):
        g = self.g
        variant_by_id = {v["id"]: v for v in g.variants}
        kinds = {k for k, _ in g.npc_kinds}
        for eid, name in sorted(g.entity_names.items()):
            if eid in T.NO_PAGE or eid in T.NPC_ENTITIES or eid in kinds or self.exists(f"guhs/{eid}"):
                continue
            cat = "diertjes" if eid in T.CRITTERS else "wezens"
            pics = [n for n in (f"critter_{eid}", eid, f"entity_{eid}") if self.im.has(n)]
            pics += sorted(n for n in self.im.available if (n.startswith(f"critter_{eid}_") or n.startswith(f"{eid}_")) and n not in pics
                           and not n.startswith(f"{eid}_spawn"))
            if eid == "kikkerguh":
                pics = [n for n in ("kikkerguhs", "kikkerguh_roze", "kikkerguh_mint", "kikkerguh_geel") if self.im.has(n)]
            if eid == "boze_oppernabbel":
                pics = [n for n in ("boze_oppernabbel", "boze_kaasknabbels") if self.im.has(n)]
            if eid == "big_mika":
                pics = ["mika"]
            thumb = pics[0] if pics else ("mika" if "mika" in eid else "guh")
            if eid in T.CRITTERS:
                kind = ("Critter", "Diertje")
            elif eid in T.BOSSES:
                kind = ("Boss", "Baas")
            elif "mika" in eid:
                kind = ("Mika", "Mika")
            else:
                kind = ("Mob", "Wezen")
            pg = Page(cat, f"{cat}/{eid}", name, name, kind[1], kind[0], thumb)
            pg.images = pics
            pg.claims = set(pics) - {"mika"} if eid != "mika" else {"mika"}
            if eid == "big_mika":
                pg.claims = set()
            if eid == "boze_oppernabbel":
                pg.claims = {"boze_oppernabbel"}
            pg.info("Kind", "Soort", t(*kind))
            v = variant_by_id.get(eid)
            if v:
                pg.info("Rarity", "Zeldzaamheid", esc(strip_prefix(v["rarity"], "Zeldzaamheid:")))
                pg.lead_nl = esc(v["info"])
            spawns = g.spawns.get(eid)
            if spawns:
                pg.add_section("spawns", "Where it spawns", "Waar hij spawnt", self.spawn_table(spawns))
                biomes = []
                for s in spawns:
                    biomes += s["biomes"]
                pg.info("Spawns in", "Spawnt in", self.biome_links(biomes, limit=6))
                pg.data["spawn_biomes"] = biomes
            home = T.ENTITY_HOME.get(eid)
            if home:
                pg.info("Where", "Waar", L(home))
                pg.related.append(home)
            loot = g.entity_loot.get(eid if eid != "big_mika" else "")
            if loot:
                pg.add_section("drops", "Drops", "Drops", self.loot_list(loot, entity=True))
                pg.info("Drops", "Drops", " ".join(self.item_ref(x["item"], with_name=False) for x in loot[:8]))
            if f"{eid}_spawn_egg" in g.items:
                pg.info("Spawn egg", "Spawnei", self.icon(self.im.item_icon(f"{eid}_spawn_egg") or "", "spawn egg") or t("yes", "ja"))
            pg.data["sort"] = name
            self.add(pg)

    def spawn_table(self, spawns):
        rows = []
        for s in spawns:
            en, nl = SPAWN_GROUP.get(s["group"], (s["group"], s["group"]))
            group = f'{s["min"]}&ndash;{s["max"]}' if s["min"] != s["max"] else f'{s["min"]}'
            rows.append(f'<tr><td>{self.biome_links(s["biomes"])}</td><td>{t(en, nl)}</td><td class="num">{s["weight"]}</td><td class="num">{group}</td></tr>')
        return ('<div class="tscroll"><table class="data"><thead><tr><th>' + t("Biomes", "Biomen") + "</th><th>" + t("Group", "Groep")
                + "</th><th>" + t("Weight", "Gewicht") + "</th><th>" + t("Group size", "Groepsgrootte") + "</th></tr></thead><tbody>"
                + "".join(rows) + "</tbody></table></div>")

    def biome_links(self, biomes, limit=None):
        out, seen = [], set()
        for b in biomes:
            if b in seen:
                continue
            seen.add(b)
            if b.startswith("#"):
                label = b.lstrip("#").split(":")[-1].replace("is_", "").replace("_", " ")
                out.append(t(f"all {label} biomes", f"alle {label}-biomen") if label == "overworld" else esc(label))
            else:
                ns, bid = b.split(":", 1)
                if ns == "guhs" and bid in self.g.biomes:
                    out.append(L(f"biomen/{bid}", esc(self.g.biomes[bid]["name"])))
                else:
                    out.append(esc(bid.replace("_", " ")))
        if limit and len(out) > limit:
            return ", ".join(out[:limit]) + f" {t(f'and {len(out) - limit} more', f'en nog {len(out) - limit}')}"
        return ", ".join(out)

    def loot_list(self, loot, entity=False):
        rows = []
        for x in sorted(loot, key=lambda x: (x["pool"], -x["share"])):
            n = f'{x["min"]:g}&ndash;{x["max"]:g}' if x["min"] != x["max"] else f'{x["min"]:g}'
            if x["chance"] is not None and isinstance(x["chance"], (int, float)):
                chance = f'{100 * x["chance"]:.0f}%'
            elif entity:
                chance = t("always", "altijd") if x["share"] >= 0.999 else f'{100 * x["share"]:.0f}%'
            else:
                s = x["share"]
                chance = t("often", "vaak") if s >= 0.15 else t("sometimes", "soms") if s >= 0.05 else t("rarely", "zelden")
            extra = f' <span class="muted">({t("player kill", "door speler")})</span>' if x.get("player_kill") else ""
            rows.append(f'<tr><td>{self.item_ref(x["item"])}{extra}</td><td class="num">{n}</td><td>{chance}</td></tr>')
        return ('<div class="tscroll"><table class="data"><thead><tr><th>' + t("Item", "Voorwerp") + "</th><th>" + t("Count", "Aantal")
                + "</th><th>" + t("Chance", "Kans") + "</th></tr></thead><tbody>" + "".join(rows) + "</tbody></table></div>")

    # --- clothes ----------------------------------------------------------------------------------------------------------------
    def clothes(self):
        g = self.g
        by_bron = {}
        for c in g.clothes:
            cid = c["id"]
            name = g.lang_nl.get(f"item.guhs.{cid}", cid.replace("_", " ").capitalize())
            icon = self.im.item_icon(cid)
            hair = self.im.first(f"guh_kapsel_{cid.replace('kapsel_', '')}") if c["slot"] == "haar" else None
            big = self.im.first(f"kleding_{cid}", f"oren_{cid}") or hair
            pg = Page("kleding", f"kleding/{cid}", name, name, "Kledingstuk", "Clothing piece", icon or big)
            pg.images = [n for n in (big, icon) if n]
            pg.claims = {n for n in (f"kleding_{cid}", f"oren_{cid}", f"icon_{cid}") if self.im.has(n)}
            if cid == "guh_backpack":
                pg.claims.add("guh_outfit_backpack")
                pg.images.insert(0, "guh_outfit_backpack")
            slot = SLOTS.get(c["slot"], (c["slot"], c["slot"]))
            pg.info("Slot", "Vak", t(*slot))
            bron = c["bron"] or ("kapper" if c["slot"] == "haar" else None)
            if bron:
                bname = g.kledingbron_names.get(bron, bron)
                pg.info("Source", "Bron", L(f"kleding/set-{bron}", esc(bname)))
                by_bron.setdefault(bron, []).append(c)
                pg.related.append(f"kleding/set-{bron}")
                if bron in T.BRON_PAGE:
                    pg.related.append(T.BRON_PAGE[bron])
            if c["prijs"]:
                pg.info("How to get", "Hoe krijg je het", esc(c["prijs"]))
            if c["wild"]:
                pg.info("Wild guhs", "Wilde guhs", t("sometimes already wear it", "dragen het soms al"))
            info = g.lang_nl.get(f"gui.guhs.knus.kapsels.{cid}.info")
            lore = g.items.get(cid, {}).get("lore") or []
            pg.lead_nl = esc(info or (lore[0] if lore else ""))
            pg.columns["slot"] = (list(SLOTS).index(c["slot"]) if c["slot"] in SLOTS else 9, t(*slot))
            pg.columns["bron"] = (g.kledingbron_names.get(bron or "", bron or "~"), esc(g.kledingbron_names.get(bron or "", bron or "")))
            pg.data["sort"] = name
            self.item_page[cid] = pg.id
            self.add(pg)
        outfit_pics = {}
        for pic, bron in T.OUTFIT_PICS.items():
            if self.im.has(pic):
                outfit_pics.setdefault(bron, []).append(pic)
        for bron, pieces in by_bron.items():
            bname = g.kledingbron_names.get(bron, bron)
            pics = outfit_pics.get(bron, [])
            first_icon = next((self.im.item_icon(c["id"]) for c in pieces if self.im.item_icon(c["id"])), None)
            bname_en = self.english.text(bname)
            pg = Page("kleding", f"kleding/set-{bron}", f"Kledingset: {bname}", f"Clothing set: {bname_en}", "Kledingset", "Clothing set",
                      pics[0] if pics else first_icon)
            pg.no_autolink = True
            pg.images = pics
            pg.claims = set(pics)
            rows = []
            for c in pieces:
                cid = c["id"]
                slot = SLOTS.get(c["slot"], (c["slot"], c["slot"]))
                rows.append(f"<tr><td>{self.item_ref('guhs:' + cid)}</td><td>{t(*slot)}</td><td>{esc(c['prijs'] or '')}</td></tr>")
            pg.add_section("pieces", "Pieces", "Kledingstukken",
                           '<div class="tscroll"><table class="data"><thead><tr><th>' + t("Piece", "Stuk") + "</th><th>" + t("Slot", "Vak")
                           + "</th><th>" + t("Price / how", "Prijs / hoe") + "</th></tr></thead><tbody>" + "".join(rows) + "</tbody></table></div>")
            pg.info("Pieces", "Stukken", str(len(pieces)))
            src = T.BRON_PAGE.get(bron)
            if src:
                pg.info("From", "Waar", L(src))
                pg.related.append(src)
            pg.lead_nl = f"De kleding van {esc(bname)}: {len(pieces)} {'stuk' if len(pieces) == 1 else 'stukken'}."
            pg.lead_en = f"The clothes of {esc(bname_en)}: {len(pieces)} {'piece' if len(pieces) == 1 else 'pieces'}."
            pg.columns["slot"] = (-1, t("Set", "Set"))
            pg.columns["bron"] = (bname, esc(bname))
            pg.data["sort"] = " " + bname
            self.add(pg)

    # --- items and blocks --------------------------------------------------------------------------------------------------------
    def items(self):
        g = self.g
        clothes = {c["id"] for c in g.clothes}
        for iid, it in g.items.items():
            if iid in clothes or iid.endswith("_spawn_egg"):
                continue
            cat = "blokken" if it["block"] else "items"
            self.item_page[iid] = f"{cat}/{iid}"
        by_result, by_input = {}, {}
        for r in g.recipes:
            if r["result"]:
                by_result.setdefault(r["result"].split(":")[-1] if r["result"].startswith("guhs:") else r["result"], []).append(r)
            for x in set(r["inputs"]):
                if x and x.startswith("guhs:"):
                    by_input.setdefault(x.split(":")[1], []).append(r)
        in_chests = {}
        for table, loot in g.chest_loot.items():
            for x in loot:
                if x["item"].startswith("guhs:"):
                    in_chests.setdefault(x["item"].split(":")[1], []).append(table)
        dropped_by = {}
        for eid, loot in g.entity_loot.items():
            for x in loot:
                if x["item"].startswith("guhs:"):
                    dropped_by.setdefault(x["item"].split(":")[1], []).append(eid)
        adv_by_item = {}
        for a in g.advancements:
            for i in a["items"]:
                adv_by_item.setdefault(i, []).append(a)
        knus_info = {}
        for k, v in g.lang_nl.items():
            m = re.fullmatch(r"gui\.guhs\.knus\.(\w+)\.(\w+)\.info", k)
            if m:
                knus_info.setdefault(m.group(2), v)
        self.by_result, self.by_input = by_result, by_input
        for iid, it in g.items.items():
            pid = self.item_page.get(iid)
            if not pid or pid.startswith("kleding/"):
                continue
            cat = pid.split("/")[0]
            icon = self.im.item_icon(iid)
            render = self.im.first(iid, f"block_{iid}")
            pg = Page(cat, pid, it["name"], it["name"], "Blok" if it["block"] else "Item", "Block" if it["block"] else "Item", icon or render)
            pg.images = [n for n in (render, icon) if n]
            pg.claims = {n for n in (f"icon_{iid}", iid, f"block_{iid}") if self.im.has(n)}
            pg.info("Kind", "Soort", t("Block", "Blok") if it["block"] else t("Item", "Item"))
            lore = it["lore"]
            lead = lore[0] if lore else knus_info.get(iid, "")
            pg.lead_nl = esc(lead)
            if lead:
                pg.data["lead_is_game_text"] = True
            made = by_result.get(iid, [])
            if made:
                pg.add_section("recipe", "How to make it", "Zo maak je het", '<div class="recipes">' + "".join(self.recipe_html(r) for r in made) + "</div>")
                pg.info("Recipe", "Recept", t("yes", "ja") + f' <span class="muted">({len(made)})</span>')
            used = by_input.get(iid, [])
            if used:
                seen, links = set(), []
                for r in used:
                    if r["result"] in seen:
                        continue
                    seen.add(r["result"])
                    links.append(f"<li>{self.item_ref(r['result'])}</li>")
                pg.add_section("used", "Used in", "Gebruikt in", '<ul class="chips-list">' + "".join(links[:60]) + "</ul>")
                pg.columns["used"] = (len(seen), str(len(seen)))
            if iid in in_chests:
                links = list(dict.fromkeys(self.loot_table_link(tb) for tb in sorted(set(in_chests[iid]))))
                pg.add_section("chests", "Found in chests", "In schatkisten", "<ul>" + "".join(f"<li>{x}</li>" for x in links) + "</ul>")
                pg.info("Chests", "Kisten", ", ".join(links[:4]))
            if iid in dropped_by:
                pg.info("Dropped by", "Gedropt door", ", ".join(self.entity_link(e) for e in sorted(set(dropped_by[iid]))))
            if iid in adv_by_item:
                pg.add_section("adv", "Advancements", "Vooruitgangen", "<ul>" + "".join(
                    f"<li><b>{esc(a['title'])}</b>: {esc(a['desc'])}</li>" for a in adv_by_item[iid][:12]) + "</ul>")
            if len(lore) > 1:
                pg.add_section("lore", "In-game text", "Tekst in het spel", "".join(f'<p class="game-text">{esc(x)}</p>' for x in lore[1:]))
            pg.columns["recipe"] = (1 if made else 0, t("yes", "ja") if made else "")
            pg.data["sort"] = it["name"]
            self.add(pg)

    def loot_table_link(self, table):
        sid = T.LOOT_STRUCTURE.get(table, table)
        if table in T.LOOT_LABEL:
            nl, en, pid = T.LOOT_LABEL[table]
            return L(pid, t(en, nl)) if pid else t(en, nl)
        if sid and sid in self.g.structures:
            return L(f"bouwwerken/{sid}", esc(self.structure_name(sid)[0]))
        return esc(table.replace("_", " "))

    def entity_link(self, eid):
        for cat in ("diertjes", "wezens", "npcs"):
            if self.exists(f"{cat}/{eid}"):
                return L(f"{cat}/{eid}", esc(self.site.pages[f"{cat}/{eid}"].title))
        if eid == "guh":
            return L("guhs/normal", "Guh")
        return esc(self.g.entity_names.get(eid, eid))

    # --- structures ------------------------------------------------------------------------------------------------------------
    def structure_name(self, sid):
        s = self.g.structures.get(sid, {})
        if s.get("name"):
            return s["name"], s["name"]
        if sid in T.STRUCTURE_NAMES:
            return T.STRUCTURE_NAMES[sid]
        return sid.replace("_", " ").capitalize(), sid.replace("_", " ").capitalize()

    def structures(self):
        g = self.g
        tabs = {}
        for cat, kopjes in g.superkompas:
            for _, ids in kopjes:
                for sid in ids:
                    tabs.setdefault(sid, [])
                    if cat not in tabs[sid]:
                        tabs[sid].append(cat)
        loot_by_structure = {}
        for table in g.chest_loot:
            sid = T.LOOT_STRUCTURE.get(table, table)
            if sid in g.structures:
                loot_by_structure.setdefault(sid, []).append(table)
        for sid, s in g.structures.items():
            nl, en = self.structure_name(sid)
            pics = [n for n in [f"structure_{sid}"] + T.STRUCTURE_PICS.get(sid, []) if self.im.has(n)]
            pics += sorted(n for n in self.im.available if re.fullmatch(rf"shot\d*_(?:structure|gebouw)_{sid}_.*", n) and n not in pics)
            pg = Page("bouwwerken", f"bouwwerken/{sid}", nl, en, "Bouwwerk", "Structure", pics[0] if pics else None)
            pg.images = pics
            pg.claims = set(pics) | {n for n in self.im.available if n.startswith(f"structure_{sid}_")}
            if s.get("tooltip"):
                pg.lead_nl = esc(s["tooltip"])
            tabs_here = tabs.get(sid, [])
            if tabs_here:
                pg.kind_en, pg.kind_nl = SUPERKOMPAS_TABS.get(tabs_here[0], (tabs_here[0],) * 2)
            else:
                pg.kind_en, pg.kind_nl = "Decoration", "Decoratie"
            if tabs_here:
                pg.info("Super compass", "Superkompas", ", ".join(t(*SUPERKOMPAS_TABS.get(c, (c, c))) for c in tabs_here))
                pg.columns["tab"] = (SUPERKOMPAS_TABS.get(tabs_here[0], ("", ""))[1], t(*SUPERKOMPAS_TABS.get(tabs_here[0], (tabs_here[0],) * 2)))
            else:
                pg.columns["tab"] = ("~", "")
            dims = self.dimensions_of(s["biomes"])
            if dims:
                pg.info("Dimension", "Dimensie", ", ".join(dims))
            if s["biomes"]:
                pg.info("Biomes", "Biomen", self.biome_links(s["biomes"], limit=8))
            placement = (s.get("set") or {}).get("placement", {})
            ptype = placement.get("type", "")
            if ptype.endswith("random_spread") and (placement.get("spacing") or 99) <= 6:
                pg.info("Rarity", "Zeldzaamheid", t("one in every area of its biome", "een in elk gebied van zijn bioom"))
                pg.columns["rarity"] = (500, t("one per area", "een per gebied"))
            elif ptype.endswith("random_spread"):
                sp = placement.get("spacing")
                lab = rarity_label(sp)
                pg.info("Rarity", "Zeldzaamheid", t(lab[0], lab[1]) + f' <span class="muted">({t(f"about 1 per {sp}&times;{sp} chunks", f"ongeveer 1 per {sp}&times;{sp} chunks")})</span>')
                pg.columns["rarity"] = (sp, t(*lab))
            elif ptype.endswith("concentric_rings"):
                n = placement.get("count")
                pg.info("Rarity", "Zeldzaamheid", t(f"{n} in the world, in a ring", f"{n} in de wereld, in een ring"))
                pg.columns["rarity"] = (999, t(f"{n} per world", f"{n} per wereld"))
            elif ptype:
                pg.info("Rarity", "Zeldzaamheid", t("one per region of its biome", "een per gebied van zijn bioom"))
                pg.columns["rarity"] = (500, t("one per region", "een per gebied"))
            else:
                pg.columns["rarity"] = (0, "")
            together = [x for x in (s.get("set") or {}).get("together", []) if x != sid]
            if together:
                pg.info("Shares its spot with", "Deelt zijn plek met", ", ".join(L(f"bouwwerken/{x}", esc(self.structure_name(x)[0])) for x in together))
            for mid, m in T.MINIGAMES.items():
                if m.get("structure") == sid:
                    pg.info("Minigame", "Minigame", L(f"minigames/{mid}", esc(m["nl"])))
                    pg.related.append(f"minigames/{mid}")
            for stid, st in T.STORIES.items():
                if st.get("structure") == sid:
                    pg.info("Story", "Verhaal", L(f"verhalen/{stid}", esc(st["nl"])))
                    pg.related.append(f"verhalen/{stid}")
            npcs = [k for k, home in T.NPC_HOME.items() if home == f"bouwwerken/{sid}"]
            if npcs:
                pg.info("Characters", "Personages", ", ".join(L(f"npcs/{k}") for k in npcs))
            tables = loot_by_structure.get(sid, [])
            if tables:
                merged = {}
                for tb in tables:
                    for x in g.chest_loot[tb]:
                        m = merged.get(x["item"])
                        if m is None:
                            merged[x["item"]] = dict(x)
                        else:
                            m["min"], m["max"], m["share"] = min(m["min"], x["min"]), max(m["max"], x["max"]), max(m["share"], x["share"])
                items = list(merged.values())
                pg.add_section("loot", "Loot", "Buit", self.loot_list(items))
                pg.info("Loot", "Buit", " ".join(self.item_ref(x["item"], with_name=False) for x in sorted(items, key=lambda x: -x["share"])[:8]))
            pg.data["sort"] = nl
            self.add(pg)

    def dimensions_of(self, biomes):
        out = []
        for b in biomes:
            if b.startswith("#minecraft") or b.startswith("minecraft:"):
                lab = t("Overworld", "Bovenwereld")
            else:
                bid = b.split(":")[-1]
                d = self.g.biomes.get(bid, {}).get("dimension")
                if not d:
                    continue
                lab = L(f"dimensies/{d}", t(*DIM_NAMES.get(d, (d, d))))
            if lab not in out:
                out.append(lab)
        return out

    # --- biomes ------------------------------------------------------------------------------------------------------------------
    def biomes(self):
        g = self.g
        spawn_by_biome = {}
        for eid, lst in g.spawns.items():
            for s in lst:
                for b in s["biomes"]:
                    spawn_by_biome.setdefault(b, []).append((eid, s))
        struct_by_biome = {}
        for sid, s in g.structures.items():
            for b in s["biomes"]:
                struct_by_biome.setdefault(b, []).append(sid)
        for bid, b in g.biomes.items():
            pics = [n for n in T.BIOME_PICS.get(bid, []) if self.im.has(n)]
            pics += sorted(n for n in self.im.available if re.fullmatch(rf"shot\d*_biome_(?:guhmension_)?{bid}", n) and n not in pics)
            pg = Page("biomen", f"biomen/{bid}", b["name"], b["name"], "Bioom", "Biome", pics[0] if pics else None)
            pg.images = pics
            pg.claims = {n for n in pics if n.startswith("shot") or n in ("diepe_guhzee", "gatenkaas", "borrelende_kaassaus", "vadshout_gezichtjes")}
            d = b["dimension"]
            pg.info("Dimension", "Dimensie", L(f"dimensies/{d}", t(*DIM_NAMES.get(d, (d, d)))))
            pg.related.append(f"dimensies/{d}")
            if b["temperature"] is not None:
                pg.info("Temperature", "Temperatuur", f'{b["temperature"]:g}'.replace(".", ","))
            pg.info("Rain / snow", "Regen / sneeuw", t("yes", "ja") if b["precipitation"] else t("no", "nee"))
            eff = b["effects"]
            sw = []
            for key, en, nl in (("sky_color", "sky", "lucht"), ("fog_color", "fog", "mist"), ("water_color", "water", "water"),
                                ("grass_color", "grass", "gras"), ("foliage_color", "leaves", "bladeren")):
                if isinstance(eff.get(key), int):
                    sw.append(f'<span class="swatch" style="background:#{eff[key]:06x}" title="#{eff[key]:06x}"></span>{t(en, nl)}')
            if sw:
                pg.info("Colours", "Kleuren", " ".join(sw))
            full = f"guhs:{bid}"
            here = spawn_by_biome.get(full, [])
            if here:
                rows = []
                for eid, s in sorted(here, key=lambda x: -(x[1]["weight"] or 0)):
                    en, nl = SPAWN_GROUP.get(s["group"], (s["group"],) * 2)
                    rows.append(f'<tr><td>{self.entity_link(eid)}</td><td>{t(en, nl)}</td><td class="num">{s["weight"]}</td>'
                                f'<td class="num">{s["min"]}' + (f'&ndash;{s["max"]}' if s["max"] != s["min"] else "") + '</td></tr>')
                pg.add_section("spawns", "What spawns here", "Wat spawnt hier",
                               '<div class="tscroll"><table class="data"><thead><tr><th>' + t("Mob", "Wezen") + "</th><th>" + t("Group", "Groep")
                               + "</th><th>" + t("Weight", "Gewicht") + "</th><th>" + t("Group size", "Groepsgrootte") + "</th></tr></thead><tbody>"
                               + "".join(rows) + "</tbody></table></div>")
            structs = struct_by_biome.get(full, [])
            if structs:
                pg.add_section("structures", "Structures", "Bouwwerken", '<ul class="chips-list">' + "".join(
                    f"<li>{L('bouwwerken/' + s, esc(self.structure_name(s)[0]))}</li>" for s in sorted(structs, key=lambda s: self.structure_name(s)[0])) + "</ul>")
                pg.columns["structures"] = (len(structs), str(len(structs)))
            variants = [vid for vid, home in T.VARIANT_HOME.items() if home == f"biomen/{bid}"]
            if variants:
                pg.info("Guhs born here", "Guhs die hier geboren worden", ", ".join(L(f"guhs/{v}") for v in variants))
            pg.columns["dim"] = (d, t(*DIM_NAMES.get(d, (d, d))))
            pg.data["sort"] = b["name"]
            self.add(pg)

    def dimensions(self):
        g = self.g
        for did, (nl, en, pic, lead_nl, lead_en) in T.DIMENSIONS.items():
            pg = Page("dimensies", f"dimensies/{did}", nl, en, "Dimensie", "Dimension", self.im.first(pic))
            pg.images = [n for n in [pic] if self.im.has(n)]
            pg.lead_nl, pg.lead_en = lead_nl, lead_en
            biomes = [bid for bid, b in g.biomes.items() if b["dimension"] == did]
            if biomes:
                pg.info("Biomes", "Biomen", ", ".join(L(f"biomen/{b}", esc(g.biomes[b]["name"])) for b in biomes))
            structs = sorted({sid for sid, s in g.structures.items() for b in s["biomes"] if b.split(":")[-1] in biomes},
                             key=lambda s: self.structure_name(s)[0])
            if structs:
                pg.add_section("structures", "Structures", "Bouwwerken", '<ul class="chips-list">' + "".join(
                    f"<li>{L('bouwwerken/' + s, esc(self.structure_name(s)[0]))}</li>" for s in structs) + "</ul>")
            pg.aliases |= {nl.split(" ", 1)[1] if nl.startswith(("De ", "Het ")) else nl}
            pg.data["sort"] = did
            self.add(pg)

    # --- minigames, stories, mechanics --------------------------------------------------------------------------------------------
    def minigames(self):
        g = self.g
        for mid, m in T.MINIGAMES.items():
            pg = Page("minigames", f"minigames/{mid}", m["nl"], m["en"], "Minigame", "Minigame", self.im.first(m.get("img") or ""))
            pg.images = [n for n in [m.get("img")] if n and self.im.has(n)]
            pg.lead_nl, pg.lead_en = m["lead_nl"], m["lead_en"]
            grp = T.MINIGAME_GROUPS.get(m.get("group"), ("", ""))
            pg.info("Group", "Groep", t(grp[1], grp[0]))
            if m.get("structure"):
                pg.info("Where", "Waar", L(f"bouwwerken/{m['structure']}", esc(self.structure_name(m["structure"])[0])))
                pg.related.append(f"bouwwerken/{m['structure']}")
            if m.get("npc"):
                pg.info("Host", "Spelleider", L(f"npcs/{m['npc']}"))
                pg.related.append(f"npcs/{m['npc']}")
            munt = m.get("munt")
            if munt and munt in g.items:
                pg.info("Prize", "Prijs", self.item_ref("guhs:" + munt))
            elif munt:
                self.report["notes"].append(f"minigame {mid}: currency item '{munt}' not found")
            bron = m.get("bron")
            if bron:
                pieces = [c for c in g.clothes if c["bron"] == bron]
                if pieces:
                    pg.info("Clothes", "Kleding", L(f"kleding/set-{bron}", t(f"{len(pieces)} pieces", f"{len(pieces)} stukken")))
                    pg.related.append(f"kleding/set-{bron}")
                    pg.add_section("prizes", "Prizes: clothes", "Prijzen: kleding", '<ul class="chips-list">' + "".join(
                        f"<li>{self.item_ref('guhs:' + c['id'])} <span class=\"muted\">{esc(c['prijs'] or '')}</span></li>" for c in pieces) + "</ul>")
            gids = m.get("gids")
            if gids:
                rows = [(k.split(".")[-1], v) for k, v in g.lang_nl.items()
                        if k.startswith("gui.guhs.gids.rij.") and (k.split(".")[-1] == gids or k.split(".")[-1].startswith(gids + "_"))]
                if rows:
                    pg.add_section("highscores", "Highscores in the Guhdex", "Highscores in de Guhdex",
                                   "<ul>" + "".join(f"<li>{esc(v)}</li>" for _, v in rows) + "</ul>")
            pg.columns["group"] = (m.get("group", ""), t(grp[1], grp[0]))
            pg.data["sort"] = list(T.MINIGAMES).index(mid)
            self.add(pg)

    def ftb_steps(self, chapter, section):
        ftb = self.g.ftb
        plain_ = ftb["plain"]
        secs = ftb["sections"].get(chapter, [])
        keys = []
        for s in secs:
            if section is None or s.get("sid") == section:
                keys += s["quests"]
        items = []
        for k in keys:
            q = ftb["quests"].get(k)
            if not q:
                continue
            tasks = " ".join(self.task_html(x) for x in q[4])
            items.append(f'<li><b>{esc(plain_(q[1]))}</b><br><span lang="nl">{esc(plain_(q[2]))}</span>'
                         f'<span lang="en" class="muted">{esc(plain_(q[2]))}</span>'
                         + (f'<div class="task">{tasks}</div>' if tasks.strip() else "") + "</li>")
        return items

    def task_html(self, task):
        typ = task.get("type")
        if typ == "item":
            return self.item_ref(task["item"], task.get("count"))
        if typ == "structure":
            sid = task["structure"].split(":")[-1]
            return "&#128205; " + (L(f"bouwwerken/{sid}", esc(self.structure_name(sid)[0])) if sid in self.g.structures else esc(sid))
        if typ == "biome":
            bid = task["biome"].split(":")[-1]
            return "&#127795; " + (L(f"biomen/{bid}", esc(self.g.biomes[bid]["name"])) if bid in self.g.biomes else esc(bid))
        if typ == "dimension":
            did = task["dimension"].split(":")[-1]
            return "&#127776; " + (L(f"dimensies/{did}", t(*DIM_NAMES[did])) if did in DIM_NAMES else esc(did))
        if typ == "kill":
            eid = task["entity"].split(":")[-1]
            return "&#9876; " + self.entity_link(eid) + (f" &times;{task.get('value')}" if task.get("value", 1) != 1 else "")
        return ""

    def stories(self):
        g = self.g
        for stid, st in T.STORIES.items():
            pg = Page("verhalen", f"verhalen/{stid}", st["nl"], st["en"], "Verhaal", "Story", self.im.first(st.get("img") or ""))
            pg.images = [n for n in [st.get("img")] if n and self.im.has(n)]
            pg.lead_nl, pg.lead_en = st["lead_nl"], st["lead_en"]
            pg.info("Kind", "Soort", t("Story / questline", "Verhaal / questline"))
            if st.get("structure"):
                pg.info("Starts at", "Begint bij", L(f"bouwwerken/{st['structure']}", esc(self.structure_name(st["structure"])[0])))
                pg.related.append(f"bouwwerken/{st['structure']}")
            if st.get("npcs"):
                pg.info("Characters", "Personages", ", ".join(L(f"npcs/{k}") if not k == "koning" else L("guhs/koning", "Koningguh")
                                                             for k in st["npcs"]))
            bron = [b for b, page in T.BRON_PAGE.items() if page == f"verhalen/{stid}"]
            for b in bron:
                pg.info("Reward: clothes", "Beloning: kleding", L(f"kleding/set-{b}", esc(g.kledingbron_names.get(b, b))))
            pg.related += st.get("related", [])
            steps = []
            for chapter, section in st.get("ftb", []):
                steps += self.ftb_steps(chapter, section)
            if steps:
                pg.add_section("steps", "Steps (FTB quests)", "Stappen (FTB-quests)",
                               '<p class="note">' + t("The quest texts are in English and Dutch, like everything in the game (Guhs follows your language setting).",
                                                      "De questteksten zijn Nederlands en Engels, net als alles in het spel (Guhs volgt je taalinstelling).")
                               + '</p><ol class="steps">' + "".join(steps) + "</ol>")
                pg.info("Steps", "Stappen", str(len(steps)))
            pg.columns["kind"] = (0, t("Story", "Verhaal"))
            pg.data["sort"] = list(T.STORIES).index(stid)
            self.add(pg)

    def ftb_chapters(self):
        ftb = self.g.ftb
        plain_ = ftb["plain"]
        for i, c in enumerate(ftb["order"]):
            spec = ftb["chapters"][c]
            title = plain_(spec["title"])
            pg = Page("verhalen", f"verhalen/ftb-{c.replace('guhs_', '')}", f"Questboek: {title}", f"Quest book: {title}",
                      "FTB-hoofdstuk", "FTB chapter", "icon_timmerguh_bouwboekje" if self.im.has("icon_timmerguh_bouwboekje") else None)
            pg.no_autolink = True
            pg.lead_nl = esc(plain_(spec.get("sub", "")))
            pg.lead_en = t("A chapter of the Guhs FTB quest book.", "")
            intro = "".join(f"<p>{esc(plain_(x))}</p>" for x in spec.get("intro", []))
            pg.add_section("intro", "How do you get here?", "Hoe kom je hier?", f'<div lang="nl">{intro}</div><div lang="en">{intro}</div>')
            total = 0
            for s in ftb["sections"].get(c, []):
                steps = self.ftb_steps(c, s.get("sid"))
                total += len(steps)
                pg.add_section(f"sec-{s.get('sid')}", plain_(s["title"]), plain_(s["title"]), '<ol class="steps">' + "".join(steps) + "</ol>")
            pg.info("Quests", "Quests", str(total))
            pg.info("Kind", "Soort", t("FTB quest chapter", "FTB-questhoofdstuk"))
            pg.related.append("systemen/ftb-quests")
            pg.columns["kind"] = (1, t("FTB chapter", "FTB-hoofdstuk"))
            pg.data["sort"] = 100 + i
            self.add(pg)

    def systems(self):
        g = self.g
        for sid, (nl, en, pic, lead_nl, lead_en, related) in T.SYSTEMS.items():
            pg = Page("systemen", f"systemen/{sid}", nl, en, "Systeem", "Mechanic", self.im.first(pic))
            pg.images = [n for n in [pic] if self.im.has(n)]
            pg.claims = set(pg.images) if sid in ("guhdex", "superkompas", "guhslee", "brouwen", "hartjes", "favorietjes") else set()
            pg.lead_nl, pg.lead_en = lead_nl, lead_en
            pg.related += related
            pg.data["sort"] = list(T.SYSTEMS).index(sid)
            self.add(pg)
        P = self.site.pages
        # data sections
        rows = "".join(f"<tr><td><b>{esc(x['name'])}</b></td><td>{esc(x['desc'])}</td></tr>" for x in g.personalities)
        P["systemen/karakters"].add_section("list", "All personalities", "Alle karakters", self.table2(("Personality", "Karakter"), ("What it does", "Wat het doet"), rows))
        rows = "".join(f"<tr><td><b>{esc(x['name'])}</b></td><td>{esc(x['desc'])}</td></tr>" for x in g.emotes)
        P["systemen/emotes"].add_section("list", "All emotes", "Alle emotes", self.table2(("Emote", "Emote"), ("What your guh does", "Wat je guh doet"), rows))
        P["systemen/effecten"].add_section("list", "All effects", "Alle effecten", '<ul class="chips-list">' + "".join(
            f"<li>{self.icon(self.im.first(f'effect_{k}') or '', v)}{esc(v)}</li>" for k, v in g.effects.items()) + "</ul>")
        books = []
        for bid, b in g.books.items():
            books.append(f'<details class="book"><summary>{esc(b["title"])}</summary>'
                         + "".join(f'<p class="game-text">{esc(pg_)}</p>' for pg_ in b["pages"]) + "</details>")
        P["systemen/guhboeken"].add_section("books", f"The {len(books)} books", f"De {len(books)} boeken", "".join(books))
        # the superkompas tabs
        out = []
        for cat, kopjes in g.superkompas:
            en_, nl_ = SUPERKOMPAS_TABS.get(cat, (cat, cat))
            items = []
            for kop, ids in kopjes:
                if kop:
                    items.append(f'<li class="sub">{esc(g.lang_nl.get(f"gui.guhs.superkompas.kopje.{kop}", kop))}</li>')
                items += [f"<li>{L('bouwwerken/' + s, esc(self.structure_name(s)[0]))}</li>" for s in ids if s in g.structures]
            tip = g.lang_nl.get(f"gui.guhs.superkompas.{cat}.tooltip", "")
            out.append(f'<h3>{t(en_, nl_)}</h3><p class="muted">{esc(tip)}</p><ul class="chips-list">{"".join(items)}</ul>')
        P["systemen/superkompas"].add_section("tabs", "The tabs", "De tabbladen", "".join(out))
        # the Guhdex
        counts = {}
        for v in g.variants:
            counts[v["kind"]] = counts.get(v["kind"], 0) + 1
        P["systemen/guhdex"].info("Pages", "Pagina's", str(len(g.variants)))
        P["systemen/guhdex"].add_section("pages", "Everything in the Guhdex", "Alles in de Guhdex", "<ul>" + "".join([
            f"<li>{L('guhs/index', t('Guh variants and story guhs', 'Guhvarianten en verhaalguhs'))}: {counts.get('variant', 0) + counts.get('verhaal', 0)}</li>",
            f"<li>{L('npcs/index', t('Guh characters', 'Guh-personages'))}: {counts.get('npc', 0)}</li>",
            f"<li>{L('diertjes/index', t('Critters and creatures', 'Diertjes en wezens'))}: {counts.get('creature', 0)}</li>"]) + "</ul>")
        # clothes
        by_slot = {}
        for c in g.clothes:
            by_slot[c["slot"]] = by_slot.get(c["slot"], 0) + 1
        P["systemen/kleding"].add_section("slots", "The slots", "De vakjes", self.table2(("Slot", "Vak"), ("Pieces", "Stukken"), "".join(
            f"<tr><td>{t(*SLOTS.get(s, (s, s)))}</td><td class=\"num\">{n}</td></tr>" for s, n in by_slot.items())))
        P["systemen/kleding"].info("Pieces", "Kledingstukken", L("kleding/index", str(len(g.clothes))))
        # FTB
        ftb = g.ftb
        items = []
        for c in ftb["order"]:
            pid = f"verhalen/ftb-{c.replace('guhs_', '')}"
            n = sum(len(s["quests"]) for s in ftb["sections"].get(c, []))
            items.append(f"<li>{L(pid, esc(ftb['plain'](ftb['chapters'][c]['title'])))} <span class=\"muted\">({n})</span></li>")
        P["systemen/ftb-quests"].add_section("chapters", "The chapters", "De hoofdstukken", "<ol>" + "".join(items) + "</ol>")
        P["systemen/ftb-quests"].info("Quests", "Quests", str(len(ftb["quests"])))
        P["systemen/ftb-quests"].info("Chapters", "Hoofdstukken", str(len(ftb["order"])))
        # advancements
        tabs = {}
        for a in g.advancements:
            tabs.setdefault(a["tab"], []).append(a)
        parts = []
        for tab, advs in tabs.items():
            root = next((a for a in advs if a["id"].endswith("/root")), None)
            title = root["title"] if root else tab.replace("_", " ").capitalize()
            rows = "".join(f'<tr><td>{self.icon(self.item_icon_name(a["icon"]) or "", a["title"]) if a["icon"] else ""}</td>'
                           f'<td><b>{esc(a["title"])}</b>{" &#9733;" if a["frame"] == "challenge" else ""}</td><td>{esc(a["desc"])}</td></tr>'
                           for a in advs)
            parts.append(f'<details class="book"><summary>{esc(title)} <span class="muted">({len(advs)})</span></summary>'
                         f'<div class="tscroll"><table class="data"><tbody>{rows}</tbody></table></div></details>')
        P["systemen/vooruitgangen"].add_section("tabs", "All advancements", "Alle vooruitgangen", "".join(parts))
        P["systemen/vooruitgangen"].info("Advancements", "Vooruitgangen", str(len(g.advancements)))
        # plushies
        knuffels = [iid for iid in g.items if iid.startswith("knuffel_")]
        if knuffels:
            P["systemen/knuffels"].add_section("list", "All plushies", "Alle knuffels", '<ul class="chips-list">' + "".join(
                f"<li>{self.item_ref('guhs:' + k)}</li>" for k in knuffels) + "</ul>")

    def table2(self, h1, h2, rows):
        return ('<div class="tscroll"><table class="data"><thead><tr><th>' + t(*h1) + "</th><th>" + t(*h2) + "</th></tr></thead><tbody>"
                + rows + "</tbody></table></div>")

    # --- the knowledge base --------------------------------------------------------------------------------------------------------
    def assign_chunks(self):
        site = self.site
        for c in self.chunks:
            key = (c.section, c.title_en)
            target = T.CHUNK_RULES.get(key)
            if target is None and c.kind != "text":
                for name in c.images:
                    pg = site.claimed(name)
                    if pg:
                        target = pg.id
                        break
            if target is None and c.h3:
                for sec, prefix, pid in T.H3_RULES:
                    if (sec == "*" or sec == c.section) and c.h3[0].startswith(prefix):
                        target = pid
                        break
            if target is None:
                target = T.SECTION_RULES.get(c.section)
            if target == T.DROP:
                self.report["dropped"] += 1
                continue
            target = self.resolve_pid(target)
            if target not in site.pages:
                self.report["unassigned"].append((c, target))
                continue
            site.pages[target].chunks.append(c)
            self.report["assigned"] += 1
            # the English names of cards and figures (the one-page wiki's own titles) link here too
            if c.kind in ("card", "figure") and c.section in ("mobs", "structures", "villages", "items", "food") and c.title_en:
                name = clean_title(plain(c.title_en))
                if 3 < len(name) < 40:
                    site.pages[target].aliases.add(name)

    def table_rows(self):
        """Rows of the knowledge-base tables whose first cell is a page's name: a line of text for that page."""
        by_name = {}
        for pg in self.site.pages.values():
            if pg.cat in ("home",) or pg.id.endswith("/index"):
                continue
            by_name.setdefault(fold(pg.title), pg)
        for c in self.chunks:
            if c.kind != "text" or "<table" not in c.body:
                continue
            head = re.search(r"<tr>(.*?)</tr>", c.body, re.S)
            ths = re.findall(r"<th>(.*?)</th>", head.group(1), re.S) if head else []
            for row in re.findall(r"<tr>(.*?)</tr>", c.body, re.S):
                tds = re.findall(r"<td[^>]*>(.*?)</td>", row, re.S)
                if len(tds) < 2:
                    continue
                en, nl = split_t(re.sub(r'<span class="swatch"[^>]*></span>', "", tds[0]))
                pg = by_name.get(fold(plain(nl))) or by_name.get(fold(plain(en)))
                if not pg or c in pg.chunks:
                    continue
                cells = []
                for i, td in enumerate(tds[1:], 1):
                    if not plain(td):
                        continue
                    label = ths[i] if i < len(ths) else ""
                    cells.append(f"<div><dt>{label}</dt><dd>{td}</dd></div>")
                if cells:
                    pg.data.setdefault("table_rows", []).append((c.h3 or (c.title_en, c.title_nl), "".join(cells)))

    # --- the last touches --------------------------------------------------------------------------------------------------------
    def finish(self):
        for pg in self.site.pages.values():
            # stats boxes of the entries -> the infobox
            have = {fold(x[1]) for x in pg.infobox}
            for c in pg.chunks:
                for (k_en, k_nl), v in c.stats or []:
                    if fold(k_nl) not in have:
                        pg.info(k_en, k_nl, v)
                        have.add(fold(k_nl))
            # English lead from the first chunk when the data had none
            if not pg.lead_en:
                for c in pg.chunks:
                    m = re.search(r'<(?:p|span) lang="en">(.*?)</(?:p|span)>', c.body or "", re.S)
                    if m and len(plain(m.group(1))) > 20:
                        pg.lead_en = esc(first_sentences(plain(m.group(1)), 320))
                        pg.data["lead_en_kb"] = True
                        break
            if not pg.lead_nl:
                for c in pg.chunks:
                    m = re.search(r'<(?:p|span) lang="nl">(.*?)</(?:p|span)>', c.body or "", re.S)
                    if m and len(plain(m.group(1))) > 20:
                        pg.lead_nl = esc(first_sentences(plain(m.group(1)), 320))
                        pg.data["lead_nl_kb"] = True
                        break
            if not pg.lead_en and pg.cat not in ("home",):
                pg.lead_en = self.auto_en(pg)
            if not pg.lead_nl and pg.cat not in ("home",):
                pg.lead_nl = self.auto_nl(pg)
            if not pg.thumb:
                imgs = [n for c in pg.chunks for n in c.images if self.im.has(n)]
                pg.thumb = imgs[0] if imgs else CAT_ICON.get(pg.cat)
            pg.related = list(dict.fromkeys(r for r in pg.related if r in self.site.pages and r != pg.id))

    @property
    def english(self):
        if not hasattr(self, "_english"):
            from .english import English
            self._english = English(self.g.root)
        return self._english

    def auto_en(self, pg):
        kind = {"items": "an item", "blokken": "a block", "kleding": "a clothing piece for your guh", "npcs": "a guh character",
                "diertjes": "a critter", "wezens": "a mob", "bouwwerken": "a structure", "biomen": "a biome", "guhs": "a kind of guh"}.get(pg.cat)
        s = f"<b>{esc(pg.title)}</b> is {kind} in Guhs." if kind else ""
        if pg.lead_nl and (pg.data.get("lead_is_game_text") or pg.cat in ("npcs", "diertjes", "wezens", "guhs", "bouwwerken", "kleding")):
            game_text = html.unescape(pg.lead_nl)
            game_text = esc(self.english.text(game_text)) if "<" not in game_text else pg.lead_nl
            s += " " + t("Its in-game text:", "") + f' <i class="game-text">{game_text}</i>'
        return s

    def auto_nl(self, pg):
        kind = {"items": "een item", "blokken": "een blok", "kleding": "een kledingstuk voor je guh", "npcs": "een guh-personage",
                "diertjes": "een diertje", "wezens": "een wezen", "bouwwerken": "een bouwwerk", "biomen": "een bioom", "guhs": "een guhsoort"}.get(pg.cat)
        return f"<b>{esc(pg.title)}</b> is {kind} uit Guhs." if kind else ""
