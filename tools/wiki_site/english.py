"""
The English side of the wiki uses the English names of Guhs 1.2.0 (tools/lang/GLOSSARY.md, en_us.json, the FTB Quests
en_us files). The builders (pages.py, topics.py, guide.py, make_wiki.py) still write the in-game names in Dutch, also in
their English texts; this last step, run on every finished page, puts the English in:

  * a text in an English element (lang="en") that is a whole in-game text (a lang value, a quest line) becomes the English
    in-game text, and every Dutch name in it (glossary + lang names, longest first) becomes the English name;
  * a short text without a language (a table cell, a link label: the in-game name) that changes that way becomes
    <span lang="en">English</span><span lang="nl">Dutch</span>, so the language switch shows the right one.
Dutch elements (lang="nl"), tags, attributes, scripts and the <title> are never touched.
"""
import html
import json
import os
import re

NAME_KEYS = ("item", "block", "entity", "structure", "biome", "effect", "dimension")
# glossary sections that hold names (not the style rules, heart-level phrases, emotes or the word-builder)
SKIP_SECTIONS = {1, 3, 4, 20}
# Dutch words that are also English (or would turn an English sentence wrong)
# (glossary 25 has the scene title "Naar huis" = Homeward: as a name it made the lobby's sign "Terug naar huis" into "Terug homeward")
NOT_A_NAME = {"Mika's", "Variant", "medium", "Medium", "Guh!", "Brrr", "Hup hup!", "Ohana", "Aloha", "Naar huis"}
# words the hand-written English texts use that are in no table (checked against en_us.json and the glossary)
EXTRA = [("snoet", "snout"), ("snoetje", "snoot"), ("Knabbelberg", "Nibble Mountain"), ("knabbelsap", "nibble juice"),
         ("knabbelvlotje", "nibble raft"), ("Knabbelvlotje", "Nibble Raft"), ("kermisbonnen", "fair tickets"), ("kermisbon", "fair ticket"),
         ("glitterknuffel", "glitter plushie"), ("kaas saus", "cheese sauce"), ("kaas knabbels", "cheese nibbles"), ("citroen", "lemon"),
         ("lavendel", "lavender"), ("Vads temmer", "Chonk Tamer"), ("dagboekje", "diary"), ("sjoelen", "shuffleboard"),
         ("Sjoelen", "Shuffleboard"), ("sjoelbak", "shuffleboard"), ("huisjes", "guh houses"), ("Huisjes", "Guh houses"),
         ("babyguhtjes", "baby guhs"), ("Babyguhtjes", "Baby guhs"), ("bouwboekje", "building booklet"), ("Sjoelhuisje", "Shuffle House"),
         ("Knabbelkatapult", "Nibble Catapult"), ("Knabbelspelen", "Nibble Games"), ("Knuffelhart", "Snuggleheart"),
         ("wolkenhoeder", "Cloud Shepherd"), ("pieppiepmuisjes", "squeaksqueak mice"), ("pieppiepmuisje", "squeaksqueak mouse"),
         ("muisjes", "mousies"), ("Moederboom", "Mother Tree"), ("Gloeisteenstof", "Glowstone Dust"), ("rijtje", "row"),
         ("klinker", "brick"), ("guhkermis", "Guh Fair"), ("guhportaal", "guh portal"), ("superkompas", "super compass"),
         ("Superkompas", "Super Compass"), ("knabbels", "nibbles"), ("vads", "chonk"), ("Vads", "Chonk"), ("VADS", "CHONK"),
         ("VAHOEG", "WAHOOG"), ("NJEG", "NYEG"), ("Opoe", "Granny"), ("koeken", "cookies"), ("koek", "cookie"),
         ("kaasmos", "cheese moss"), ("knabbelbes", "nibbleberry"), ("kontpoetser", "bottom polisher"), ("kontpoetsen", "bottom polishing"),
         ("Kontpoetsen", "Bottom polishing"), ("poetsbeurt", "polish"), ("mijlpalen", "milestones"), ("Floepguh", "Floopguh"),
         ("krullen", "curls"), ("kuifje", "quiff"), ("knotjes", "buns"), ("strikjes", "bows"), ("pluisbol", "puffball"),
         ("vlechtjes", "braids"), ("hanenkam", "mohawk"), ("matje", "mullet"), ("knabbelkorven", "nibble hives"),
         ("knuffeldansje", "snuggle dance"), ("bloembak", "flower box"), ("knabbelkluis", "nibble vault"), ("knuffelcel", "cuddle cell"),
         ("glijbaantje", "guh slide"), ("glijbanen", "slides"), ("Vadsema", "Chonkworth"), ("vadspluisjes", "chonk puffs"),
         ("Vadsschild", "Chonk Shield"), ("Knabbelherstel", "Nibble Mending"), ("Kaasmijnschacht", "Cheese Mineshaft"),
         ("Kaasberg", "Cheese Mountain"), ("Verrekijker", "Spyglass"), ("Koekje", "Cookie"), ("koepelhal", "dome hall"),
         ("Knabbelroof", "Nibble Heist"), ("knabbelspijker", "nibble nail"), ("guhpaddenstoelen", "guh mushrooms"),
         ("Moerasheks", "Swamp Witch"), ("sjoelschijven", "shuffle pucks"), ("Kaasgeel", "Cheese yellow"), ("grijs", "gray"),
         ("polijsten", "polishing"), ("gepolijste", "polished"), ("kaasknabbelerts", "cheese nibble ore"), ("kaaspilaren", "cheese pillars"),
         ("Auuuhoe", "Awooo"), ("wastobbe", "washtub"), ("voerbak", "feeding trough"), ("bloempot", "flower pot"),
         ("muisjesdansje", "mousie dance"), ("zoetdeeg", "sweet dough"), ("glazuur", "icing"), ("Sjoel", "Shuffleboard"),
         ("konijntjes", "bunnies"), ("Egeltje", "Hedgehog"), ("eekhoorntje", "squirrel"), ("huisje", "guh house"), ("Huisje", "Guh house"),
         ("the Knuffeldal", "Snuggledale"), ("The Knuffeldal", "Snuggledale"), ("plein", "square"), ("Burgemeester", "Mayor"),
         ("pluizenbomen", "fluff trees"), ("Cosy", "Cozy"), ("cosy", "cozy"), ("Terug naar huis", "Back home")]
VOID = {"br", "img", "meta", "link", "input", "hr", "source", "wbr", "col", "area", "base", "embed", "param", "track"}
RAW = {"script", "style", "title", "textarea", "option", "code"}
TOKEN = re.compile(r"(<!--.*?-->|<[^>]+>)", re.S)
CODES = re.compile(r"(?:&|§)[0-9a-fk-or]")


def norm(s):
    return re.sub(r"\s+", " ", CODES.sub("", (s or "").replace("\\&", "&"))).strip()


def read_json5(path):
    s = open(path, encoding="utf-8").read()
    s = re.sub(r",(\s*[\]}])", r"\1", s)
    return json.loads(s)


class English:
    def __init__(self, root):
        self.root = root
        lang = os.path.join(root, "src", "main", "resources", "assets", "guhs", "lang")
        nl = json.load(open(os.path.join(lang, "nl_nl.json"), encoding="utf-8"))
        en = json.load(open(os.path.join(lang, "en_us.json"), encoding="utf-8"))
        self.values = {}            # normalised Dutch in-game text -> English
        names = []                  # (Dutch name, English name), the glossary first
        for k, v in nl.items():
            e = en.get(k)
            if isinstance(v, str) and isinstance(e, str) and e != v:
                self.values.setdefault(norm(v), norm(e))
                if k.split(".")[0] in NAME_KEYS and k.count(".") == 2 and "%" not in v and len(v.split()) <= 5:
                    names.append((v, e))
                pv, pe = re.fullmatch(r"%s ([^%]{4,30})", v), re.fullmatch(r"%s ([^%]{4,30})", e)
                if pv and pe:           # "%s spelenlintjes" -> "%s Games Ribbons": a plural for the prose
                    names.append((pv.group(1), pe.group(1).lower() if pv.group(1).islower() else pe.group(1)))
        ftb = os.path.join(root, "src", "main", "resources", "ftbquests", "lang")
        if os.path.isdir(os.path.join(ftb, "nl_nl")):
            for f in sorted(os.listdir(os.path.join(ftb, "nl_nl"))):
                fe = os.path.join(ftb, "en_us", f)
                if not os.path.exists(fe):
                    continue
                qn, qe = read_json5(os.path.join(ftb, "nl_nl", f)), read_json5(fe)
                for k, v in qn.items():
                    e = qe.get(k)
                    if e is None:
                        continue
                    if isinstance(v, list) and isinstance(e, list):
                        self.values.setdefault(norm(" ".join(v)), norm(" ".join(e)))
                        self.values.setdefault(norm("\n".join(v)), norm("\n".join(e)))
                        if len(v) == len(e):
                            for a, b in zip(v, e):
                                if norm(a) and norm(a) != norm(b):
                                    self.values.setdefault(norm(a), norm(b))
                    elif isinstance(v, str) and isinstance(e, str) and norm(v) != norm(e):
                        self.values.setdefault(norm(v), norm(e))
                        if k.endswith(".title"):
                            names.append((norm(v), norm(e)))
        names = self.glossary() + EXTRA + names
        self.names = {}
        for a, b in names:
            a, b = a.strip(), b.strip()
            if len(a) < 4 or a == b or a in NOT_A_NAME:
                continue
            forms = [(a, b)]
            if a[:1].islower():
                forms.append((a[:1].upper() + a[1:], b[:1].upper() + b[1:]))
            elif len(a) >= 6 and a.lower() != a:
                forms.append((a.lower(), b if b.startswith(("Mika", "Guhmension", "Guhdex")) else b.lower()))
            m = re.match(r"(?:De|Het) (\S.*)", a)
            if m and b.startswith("The ") and len(m.group(1)) >= 4:     # "Het Knuffelhart" -> also "Knuffelhart"
                forms.append((m.group(1), b[4:]))
            if " " not in a and len(a) >= 5 and not a.endswith(("s", "!", "?", ".")):     # Dutch plurals in the prose
                pl = b + "es" if b.endswith(("s", "x", "ch", "sh")) else b[:-1] + "ies" if re.search(r"[^aeiou]y$", b) else b + "s"
                for x, _ in list(forms):
                    if " " not in x:
                        forms += [(x + "s", pl if x[:1].isupper() else pl.lower()), (x + "en", pl if x[:1].isupper() else pl.lower()),
                                  (x + x[-1] + "en", pl if x[:1].isupper() else pl.lower())]
            for x, y in list(forms):
                if not x.startswith(("De ", "Het ", "de ", "het ")):
                    the = y if y.startswith(("The ", "the ")) else "The " + y
                    forms += [("De " + x, the), ("Het " + x, the)]
            for x, y in forms:
                self.names.setdefault(x, y)
        self.prefix = {}            # the first 15 characters of an in-game text -> the texts (for a text cut short)
        self.tails = {}             # "Zeldzaamheid: Zeldzaam" -> "Zeldzaam": "Rare" (the part after a label)
        for k, e in list(self.values.items()):
            if len(k) > 15:
                self.prefix.setdefault(k[:15], []).append(k)
            if ": " in k and ": " in e:
                self.tails.setdefault(k.split(": ", 1)[1], e.split(": ", 1)[1])
        words = sorted(self.names, key=len, reverse=True)
        self.rx = re.compile(r"(?<![\w-])(" + "|".join(map(re.escape, words)) + r")(?![\w])") if words else None
        self.changed = 0

    def glossary(self):
        out, sec = [], 0
        path = os.path.join(self.root, "tools", "lang", "GLOSSARY.md")
        if not os.path.exists(path):
            return out
        for line in open(path, encoding="utf-8"):
            m = re.match(r"## (\d+)\.", line)
            if m:
                sec = int(m.group(1))
            if sec in SKIP_SECTIONS or not line.startswith("|") or line.startswith("|---") or line.startswith("| Dutch"):
                continue
            cols = [c.strip().replace("*", "") for c in line.strip().strip("|").split("|")]
            if len(cols) < 2:
                continue
            a = [x.strip() for x in cols[0].split(" / ")]
            b = [x.strip() for x in cols[1].split(" / ")]
            if len(a) != len(b):
                if len(b) == 1:
                    b = b * len(a)
                elif len(a) == 1:
                    b = b[:1]
                else:
                    continue
            b = [re.sub(r"^to ", "", y) for y in b]
            for x, y in zip(a, b):
                x = x.replace("(-stadje)", "")
                if x.endswith("(s)") and y.endswith("(s)"):      # kaasknabbel(s) -> cheese nibble(s): both forms
                    out.append((x[:-3] + "s", y[:-3] + "s"))
                x = re.sub(r"\s*\([^)]*\)", "", x).strip()
                y = re.sub(r"\s*\([^)]*\)", "", y).strip()
                if x and y:
                    out.append((x, y))
        for m in re.finditer(r"([A-Z][\w']+) -> \*\*([^*]+)\*\*", open(path, encoding="utf-8").read()):
            out.append((m.group(1), m.group(2)))
        return out

    # --- text ---------------------------------------------------------------------------------------------------------------
    def whole(self, s):
        """The English of a whole in-game text (also in quotes, or cut off with '...'), else None."""
        k = norm(s)
        if k in self.values:
            return self.values[k]
        q = re.fullmatch(r"([\"'“‘(]+)(.*?)([\"'”’)]+[.!?:]?)", k, re.S)
        if q and q.group(2):
            inner = self.whole(q.group(2))
            if inner is not None:
                return q.group(1) + inner + q.group(3)
        if k in self.tails:
            return self.tails[k]
        m = re.fullmatch(r"([:;,\-–—(]+\s*)(.+)", k, re.S)    # ": in-game text" after a label in another tag
        if m:
            inner = self.whole(m.group(2))
            if inner is not None:
                return m.group(1) + inner
        if len(k) >= 15:            # the first sentence(s) of a longer text, or a text cut off with "..."
            dots = k.endswith(("...", "…"))
            cut = k.rstrip(".…").rstrip() if dots else k
            for full in self.prefix.get(cut[:15], []):
                if full.startswith(cut) and full != cut:
                    e = self.values[full]
                    if dots:
                        short = e[:max(len(cut), 20)].rsplit(" ", 1)[0] if len(e) > len(cut) + 3 else e
                        return short.rstrip(",;: ") + ("..." if short != e else "")
                    n = len(re.findall(r"[.!?](?:\s|$)", cut))
                    if n and full[len(cut):len(cut) + 1] == " ":
                        return " ".join(re.split(r"(?<=[.!?])\s+", e)[:n])
        return None

    def text(self, s):
        """A plain (unescaped) text -> its English: a whole in-game text (also as part of a list or a 'Label: text'),
        else the Dutch names in it."""
        core = s.strip()
        if not core:
            return s
        lead, trail = s[:len(s) - len(s.lstrip())], s[len(s.rstrip()):]
        w = self.whole(core)
        if w is not None:
            return lead + w + trail
        for sep in (" · ", ": ", ", ", ". "):
            if sep in core:
                bits = core.split(sep)
                if any(self.whole(x) is not None for x in bits):
                    return lead + sep.join(self.text(x) for x in bits) + trail
        if self.rx is None:
            return s
        return self.rx.sub(lambda m: self.names[m.group(1)], s)

    def is_whole(self, s):
        return self.whole(s) is not None

    # --- html ---------------------------------------------------------------------------------------------------------------
    def html(self, h):
        out, stack = [], []          # stack: [(tag, lang)]
        for part in TOKEN.split(h):
            if not part:
                continue
            if part.startswith("<!--"):
                out.append(part)
                continue
            if part.startswith("<"):
                m = re.match(r"<\s*(/)?\s*([a-zA-Z0-9]+)", part)
                if m:
                    tag = m.group(2).lower()
                    if m.group(1):
                        for i in range(len(stack) - 1, -1, -1):
                            if stack[i][0] == tag:
                                del stack[i:]
                                break
                    elif tag not in VOID and not part.rstrip().endswith("/>"):
                        lm = re.search(r'\slang="([a-z]+)"', part)
                        lang = lm.group(1) if lm and tag != "html" else (stack[-1][1] if stack else None)
                        stack.append((tag, lang))
                out.append(part)
                continue
            if any(t in RAW for t, _ in stack):
                out.append(part)
                continue
            lang = stack[-1][1] if stack else None
            if lang == "nl" or not part.strip():
                out.append(part)
                continue
            plain = html.unescape(part)
            if lang == "en":
                new = self.text(plain)
                if new != plain:
                    self.changed += 1
                    part = html.escape(new, quote=False)
                out.append(part)
                continue
            # no language: only a whole in-game text or a short name-ish text gets an English twin;
            # a list of names or in-game texts ("A · B · C") goes piece by piece
            pieces = re.split(r"(\s*·\s*)", plain) if not self.is_whole(plain) else [plain]
            new = "".join(x if i % 2 else (self.text(x) if self.is_whole(x) or len(x.split()) <= 6
                                           or any(self.is_whole(y) for y in x.split(", ")) else x)
                          for i, x in enumerate(pieces))
            if new != plain:
                self.changed += 1
                lead, trail = part[:len(part) - len(part.lstrip())], part[len(part.rstrip()):]
                part = (f'{lead}<span lang="en">{html.escape(new.strip(), quote=False)}</span>'
                        f'<span lang="nl">{part.strip()}</span>{trail}')
            out.append(part)
        return "".join(out)
