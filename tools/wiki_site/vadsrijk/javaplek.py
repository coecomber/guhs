"""
A tiny interpreter for the static part of the mod's *Plekken.java classes (the quest guide + findables pages, vindplekken.py).

The quest targets and findables are Java maps filled in static blocks with helpers like w("<map id>", x, y, z) (the generated
map spot wins, else the fallback numbers), vul(...), vondst(...), loops over int[][] tables, aliases and offsets. Copying those rules
into regexes goes wrong as soon as one class does it differently (zone 4's ALIAS table, zone 2's local frame + volgKaart, zone 3's v2
frame, ...), so this evaluates the class files themselves: a tokenizer, a parser for the Java subset these classes use (fields,
static blocks, small static methods, for / if / return, ternaries, string concatenation, records, List/Map factories, BlockPos/Vec3
math) and an evaluator. Every static member is evaluated on its own: a member this subset cannot run (a lambda, a scene) is skipped
and counted in Klasse.fouten, the rest still works. Reflection lookups (zone 4-6's wereld(id)) are replaced by a direct look-up in
the generated Z?WereldPlekken.ALLE map, exactly what the reflection finds at runtime.
"""
import os
import re

JAVA = ("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")

_TOK = re.compile(r"""
 (?P<ws>\s+)|(?P<lc>//[^\n]*)|(?P<bc>/\*.*?\*/)|
 (?P<str>"(?:\\.|[^"\\])*")|(?P<chr>'(?:\\.|[^'\\])')|
 (?P<num>0[xX][0-9a-fA-F_]+[lL]?|\d[\d_]*\.\d*(?:[eE][-+]?\d+)?[fFdD]?|\.\d+(?:[eE][-+]?\d+)?[fFdD]?|\d[\d_]*(?:[eE][-+]?\d+)?[fFdDlL]?)|
 (?P<id>[A-Za-z_$][\w$]*)|
 (?P<op>->|::|\+\+|--|&&|\|\||==|!=|<=|>=|\+=|-=|\*=|/=|\.\.\.|[{}()\[\];,.=<>+\-*/%!?:@&|^~])
""", re.S | re.X)

PRIM = {"int", "long", "short", "byte", "double", "float", "boolean", "char", "void", "var"}
MODS = {"public", "private", "protected", "static", "final", "synchronized", "abstract", "native", "transient", "volatile", "default", "strictfp"}


class Fout(Exception):
    pass


def tokens(tekst):
    out = []
    for m in _TOK.finditer(tekst):
        k = m.lastgroup
        if k in ("ws", "lc", "bc"):
            continue
        v = m.group()
        if k == "num":
            s = v.replace("_", "")
            if s[:2] in ("0x", "0X"):
                v = int(s.rstrip("lL"), 16)
            elif re.search(r"[.eE]|[fFdD]$", s):
                v = float(s.rstrip("fFdD"))
            else:
                v = int(s.rstrip("lL"))
        elif k == "str":
            v = bytes(v[1:-1], "utf-8").decode("unicode_escape") if "\\" in v else v[1:-1]
        elif k == "chr":
            v = v[1:-1]
        out.append((k, v))
    out.append(("eof", None))
    return out


# ---- values -------------------------------------------------------------------------------------------------------------------
class Pos(tuple):
    """BlockPos (ints) or Vec3 (floats)."""

    def __new__(cls, x, y, z):
        return tuple.__new__(cls, (x, y, z))


def _bp(x, y, z):
    return Pos(int(x), int(y), int(z))


class Rec:
    def __init__(self, naam, velden, waarden):
        self.naam, self.v = naam, dict(zip(velden, waarden))

    def __repr__(self):
        return f"{self.naam}{self.v}"


class Entry:
    def __init__(self, m, k):
        self.m, self.k = m, k


class Pkg:
    def __init__(self, naam):
        self.naam = naam


class Ns:
    """A JDK class used statically (Map, List, Math, BlockPos, Vec3, ...)."""

    def __init__(self, naam):
        self.naam = naam


NS = {"Map", "List", "Set", "Math", "BlockPos", "Vec3", "Integer", "String", "Arrays", "Collections", "Double", "Mth", "Objects", "Boolean"}


class Terug(Exception):
    def __init__(self, v):
        self.v = v


# ---- parser -------------------------------------------------------------------------------------------------------------------
class Parser:
    def __init__(self, toks, i=0, j=None):
        self.t, self.i, self.end = toks, i, len(toks) - 1 if j is None else j

    def k(self, o=0):
        i = self.i + o
        return self.t[i] if i < self.end else ("eof", None)

    def v(self, o=0):
        return self.k(o)[1]

    def is_(self, *vals):
        kd, v = self.k()
        return kd in ("op", "id") and v in vals

    def eat(self, val=None):
        kd, v = self.k()
        if val is not None and v != val:
            raise Fout(f"expected {val!r}, got {v!r}")
        self.i += 1
        return v

    def id_(self):
        kd, v = self.k()
        if kd != "id":
            raise Fout(f"expected a name, got {v!r}")
        self.i += 1
        return v

    def skip_generic(self):
        if self.v() != "<":
            return
        d = 0
        while True:
            v = self.eat()
            if v == "<":
                d += 1
            elif v == ">":
                d -= 1
                if d == 0:
                    return
            elif v is None or v in (";", "{", "="):
                raise Fout("bad generic")

    def type_(self):
        """A type (returned as its base name), or Fout."""
        kd, v = self.k()
        if kd != "id":
            raise Fout("no type")
        naam = self.eat()
        while self.v() == "." and self.k(1)[0] == "id":
            self.eat()
            naam = self.eat()
        self.skip_generic()
        while self.v() == "[" and self.v(1) == "]":
            self.eat()
            self.eat()
            naam += "[]"
        if self.v() == "...":
            self.eat()
            naam += "[]"
        return naam

    def balanced(self, open_, close):
        d = 0
        while True:
            v = self.eat()
            if v is None:
                raise Fout("unbalanced")
            if v == open_:
                d += 1
            elif v == close:
                d -= 1
                if d == 0:
                    return

    # statements
    def block(self):
        self.eat("{")
        out = []
        while self.v() != "}":
            out.append(self.stmt())
        self.eat("}")
        return ("block", out)

    def stmt(self):
        v = self.v()
        if self.k()[0] == "op" and v == "{":
            return self.block()
        if v == ";":
            self.eat()
            return ("leeg",)
        if self.k()[0] == "id":
            if v == "if":
                self.eat()
                self.eat("(")
                c = self.expr()
                self.eat(")")
                a = self.stmt()
                b = None
                if self.v() == "else":
                    self.eat()
                    b = self.stmt()
                return ("if", c, a, b)
            if v == "for":
                self.eat()
                self.eat("(")
                # enhanced for?
                j, d = self.i, 0
                enhanced = False
                while j < self.end:
                    w = self.t[j][1]
                    if w in ("(", "[", "{"):
                        d += 1
                    elif w in (")", "]", "}"):
                        if d == 0:
                            break
                        d -= 1
                    elif w == ";" and d == 0:
                        break
                    elif w == ":" and d == 0:
                        enhanced = True
                        break
                    j += 1
                if enhanced:
                    while self.v() == "final":
                        self.eat()
                    self.type_()
                    naam = self.id_()
                    self.eat(":")
                    over = self.expr()
                    self.eat(")")
                    return ("foreach", naam, over, self.stmt())
                init = None if self.v() == ";" else self.decl_of_expr()
                if self.v() == ";":
                    self.eat()
                cond = None if self.v() == ";" else self.expr()
                self.eat(";")
                upd = []
                while self.v() != ")":
                    upd.append(self.expr())
                    if self.v() == ",":
                        self.eat()
                self.eat(")")
                return ("for", init, cond, upd, self.stmt())
            if v == "while":
                self.eat()
                self.eat("(")
                c = self.expr()
                self.eat(")")
                return ("while", c, self.stmt())
            if v == "return":
                self.eat()
                e = None if self.v() == ";" else self.expr()
                self.eat(";")
                return ("return", e)
            if v in ("break", "continue"):
                self.eat()
                self.eat(";")
                return (v,)
            if v == "throw":
                self.eat()
                self.expr()
                self.eat(";")
                return ("throw",)
            if v == "try":
                self.eat()
                b = self.block()
                while self.v() == "catch":
                    self.eat()
                    self.balanced("(", ")")
                    self.block()
                if self.v() == "finally":
                    self.eat()
                    self.block()
                return b
            if v in ("switch", "do", "class", "assert"):
                raise Fout(f"unsupported {v}")
        s = self.decl_of_expr()
        self.eat(";")
        return s

    def decl_of_expr(self):
        bewaar = self.i
        while self.v() == "final":
            self.eat()
        try:
            self.type_()
            if self.k()[0] == "id" and self.v(1) in ("=", ";", ",", "["):
                decl = []
                while True:
                    naam = self.id_()
                    while self.v() == "[":
                        self.eat("[")
                        self.eat("]")
                    e = None
                    if self.v() == "=":
                        self.eat()
                        e = self.var_init()
                    decl.append((naam, e))
                    if self.v() != ",":
                        break
                    self.eat()
                return ("decl", decl)
        except Fout:
            pass
        self.i = bewaar
        return ("expr", self.expr())

    def var_init(self):
        if self.v() == "{" and self.k()[0] == "op":
            return self.arr_init()
        return self.expr()

    def arr_init(self):
        self.eat("{")
        items = []
        while self.v() != "}":
            items.append(self.var_init())
            if self.v() == ",":
                self.eat()
        self.eat("}")
        return ("arr", items)

    # expressions
    def expr(self):
        if self.k()[0] == "id" and self.v(1) == "->":
            raise Fout("lambda")
        if self.v() == "(" and self.v(1) == ")" and self.v(2) == "->":
            raise Fout("lambda")
        lhs = self.ternary()
        if self.v() in ("=", "+=", "-=", "*=", "/="):
            op = self.eat()
            return ("assign", op, lhs, self.expr())
        return lhs

    def ternary(self):
        c = self.bin(0)
        if self.v() == "?":
            self.eat()
            a = self.ternary()
            self.eat(":")
            b = self.ternary()
            return ("cond", c, a, b)
        return c

    LEVELS = [("||",), ("&&",), ("|",), ("^",), ("&",), ("==", "!="), ("<", ">", "<=", ">=", "instanceof"), ("+", "-"), ("*", "/", "%")]

    def bin(self, n):
        if n >= len(self.LEVELS):
            return self.unary()
        a = self.bin(n + 1)
        while self.k()[0] in ("op", "id") and self.v() in self.LEVELS[n]:
            op = self.eat()
            if op == "instanceof":
                t = self.type_()
                naam = self.id_() if self.k()[0] == "id" else None
                a = ("instanceof", a, t, naam)
                continue
            a = ("bin", op, a, self.bin(n + 1))
        return a

    def unary(self):
        v = self.v()
        if self.k()[0] == "op" and v in ("-", "+", "!", "~"):
            self.eat()
            return ("un", v, self.unary())
        if self.k()[0] == "op" and v in ("++", "--"):
            self.eat()
            return ("preinc", v, self.unary())
        if v == "(" and self.k(1)[0] == "id" and self.v(1) in PRIM and self.v(2) == ")":
            self.eat()
            t = self.eat()
            self.eat(")")
            return ("cast", t, self.unary())
        return self.postfix(self.primary())

    def args(self):
        self.eat("(")
        a = []
        while self.v() != ")":
            a.append(self.expr())
            if self.v() == ",":
                self.eat()
        self.eat(")")
        return a

    def postfix(self, e):
        while True:
            v = self.v()
            if v == "." and self.k()[0] == "op":
                self.eat()
                if self.v() == "<":
                    self.skip_generic()
                naam = self.id_()
                if self.v() == "(":
                    e = ("call", e, naam, self.args())
                else:
                    e = ("field", e, naam)
            elif v == "[" and self.k()[0] == "op":
                self.eat()
                i = self.expr()
                self.eat("]")
                e = ("index", e, i)
            elif v in ("++", "--") and self.k()[0] == "op":
                self.eat()
                e = ("postinc", v, e)
            elif v == "::":
                raise Fout("method ref")
            else:
                return e

    def primary(self):
        kd, v = self.k()
        if kd in ("num", "str", "chr"):
            self.eat()
            return ("lit", v)
        if kd == "op":
            if v == "(":
                self.eat()
                e = self.expr()
                self.eat(")")
                return e
            if v == "{":
                return self.arr_init()
            raise Fout(f"unexpected {v!r}")
        if kd == "id":
            if v in ("true", "false"):
                self.eat()
                return ("lit", v == "true")
            if v == "null":
                self.eat()
                return ("lit", None)
            if v == "new":
                self.eat()
                naam = self.id_()
                while self.v() == "." and self.k(1)[0] == "id":
                    self.eat()
                    naam = self.eat()
                self.skip_generic()
                if self.v() == "[":
                    dims = []
                    while self.v() == "[":
                        self.eat()
                        dims.append(None if self.v() == "]" else self.expr())
                        self.eat("]")
                    init = self.arr_init() if self.v() == "{" else None
                    return ("newarr", naam, dims, init)
                a = self.args()
                if self.v() == "{":
                    raise Fout("anonymous class")
                return ("new", naam, a)
            self.eat()
            if self.v() == "(":
                return ("call", None, v, self.args())
            return ("name", v)
        raise Fout("end of input")


# ---- classes ------------------------------------------------------------------------------------------------------------------
def _match(toks, i):
    """Index of the token that closes the bracket at i."""
    open_, close = toks[i][1], {"{": "}", "(": ")", "[": "]"}[toks[i][1]]
    d = 0
    for j in range(i, len(toks)):
        if toks[j][0] == "op":
            if toks[j][1] == open_:
                d += 1
            elif toks[j][1] == close:
                d -= 1
                if d == 0:
                    return j
    raise Fout("unbalanced")


class Klasse:
    def __init__(self, wereld, naam, pad):
        self.w, self.naam, self.pad = wereld, naam, pad
        self.statics, self.methoden, self.records, self.leden = {}, {}, {}, []
        self.staat = 0   # 0 new, 1 initialising, 2 done
        self.fouten = []
        self.natief = {}
        toks = tokens(open(pad, encoding="utf-8").read())
        self.toks = toks
        # the top-level class body
        i = next((j for j in range(len(toks) - 1) if toks[j] == ("id", "class") or toks[j] == ("id", "interface") or toks[j] == ("id", "enum")), None)
        if i is None:
            return
        while toks[i][1] != "{":
            i += 1
        einde = _match(toks, i)
        self._leden(i + 1, einde)

    def _leden(self, i, einde):
        t = self.toks
        while i < einde:
            j, d, eq = i, 0, False
            while j < einde:
                kd, v = t[j]
                if kd == "op":
                    if v in "([":
                        d += 1
                    elif v in ")]":
                        d -= 1
                    elif v == "=" and d == 0:
                        eq = True
                    elif v == ";" and d == 0:
                        j += 1
                        break
                    elif v == "{":
                        if d == 0 and not eq:
                            j = _match(t, j) + 1
                            break
                        d += 1
                    elif v == "}":
                        d -= 1
                j += 1
            try:
                self._lid(i, j)
            except Fout as e:
                self.fouten.append(f"member at token {i}: {e}")
            i = j

    def _lid(self, i, j):
        p = Parser(self.toks, i, j)
        while p.v() == "@":
            p.eat()
            p.id_()
            while p.v() == ".":
                p.eat()
                p.id_()
            if p.v() == "(":
                p.balanced("(", ")")
        mods = set()
        while p.k()[0] == "id" and p.v() in MODS:
            mods.add(p.eat())
        if p.v() == "{":
            if "static" in mods:
                self.leden.append(("init", p.block()))
            return
        if p.v() in ("class", "interface", "enum"):
            return
        if p.v() == "record":
            p.eat()
            naam = p.id_()
            p.skip_generic()
            p.eat("(")
            velden = []
            while p.v() != ")":
                p.type_()
                velden.append(p.id_())
                if p.v() == ",":
                    p.eat()
            self.records[naam] = velden
            return
        if p.v() == "<":
            p.skip_generic()
        typ = p.type_()
        if p.v() == "(":   # constructor
            return
        naam = p.id_()
        if p.v() == "(":   # method
            p.eat("(")
            params = []
            while p.v() != ")":
                while p.v() in ("final",) or p.v() == "@":
                    if p.v() == "@":
                        p.eat()
                        p.id_()
                    else:
                        p.eat()
                p.type_()
                params.append(p.id_())
                if p.v() == ",":
                    p.eat()
            p.eat(")")
            while p.v() not in ("{", ";", None):
                p.eat()
            body = p.block() if p.v() == "{" else None
            if "static" in mods and body is not None:
                self.methoden.setdefault(naam, []).append((params, body))
            return
        if "static" not in mods:
            return
        decl = []
        while True:
            while p.v() == "[":
                p.eat()
                p.eat("]")
            e = None
            if p.v() == "=":
                p.eat()
                e = p.var_init()
            decl.append((naam, e, typ))
            if p.v() != ",":
                break
            p.eat()
            naam = p.id_()
        self.leden.append(("veld", decl))

    # -- init --
    def init(self):
        if self.staat:
            return
        self.staat = 1
        for soort, x in self.leden:
            if soort == "veld":
                for naam, e, typ in x:
                    try:
                        self.statics[naam] = self.w.ev(e, self, [{}]) if e is not None else (0 if typ in PRIM - {"boolean"} else None)
                    except (Fout, Terug, Exception) as ex:   # noqa: BLE001 (one member at a time, like a class with a broken field)
                        self.statics.setdefault(naam, None)
                        self.fouten.append(f"{naam}: {type(ex).__name__} {ex}")
            else:
                sc = [{}]   # one scope for the whole static block (its locals are used by later statements)
                for st in x[1]:
                    try:
                        self.w.ex(st, self, sc)
                    except (Fout, Exception) as ex:   # noqa: BLE001
                        self.fouten.append(f"static block: {type(ex).__name__} {ex}")
        self.staat = 2

    def get(self, naam):
        self.init()
        if naam in self.statics:
            return self.statics[naam]
        raise Fout(f"{self.naam}.{naam}?")


class Wereld:
    """All classes under feature/vadsrijk by simple name, loaded on first use."""

    def __init__(self, root):
        self.root = root
        basis = os.path.join(root, *JAVA)
        self.paden = {}
        for d, _, fs in os.walk(basis):
            for f in fs:
                if f.endswith(".java") and not f.endswith("GameTests.java"):
                    self.paden.setdefault(f[:-5], os.path.join(d, f))
        self.klassen = {}

    def klasse(self, naam):
        if naam not in self.klassen:
            if naam not in self.paden:
                return None
            k = Klasse(self, naam, self.paden[naam])
            self.klassen[naam] = k
            for n, f in NATIEF.get(naam, {}).items():
                k.natief[n] = f
        return self.klassen[naam]

    def statisch(self, klasse, naam):
        k = self.klasse(klasse)
        if not k:
            return None
        try:
            return k.get(naam)
        except Fout:
            return None

    # -- evaluation --
    def zoek(self, naam, kl, sc):
        for s in reversed(sc):
            if naam in s:
                return s[naam]
        if naam in kl.statics:
            return kl.statics[naam]
        if any(naam == d[0] for soort, x in kl.leden if soort == "veld" for d in x):
            if kl.staat == 1:
                raise Fout(f"{naam} used before it is set")
            kl.init()
            return kl.statics.get(naam)
        if naam in NS:
            return Ns(naam)
        k = self.klasse(naam)
        if k:
            return k
        if naam[:1].islower():
            return Pkg(naam)
        raise Fout(f"unknown name {naam}")

    def roep(self, kl, naam, args):
        if naam in kl.natief:
            return kl.natief[naam](self, kl, *args)
        ms = [m for m in kl.methoden.get(naam, []) if len(m[0]) == len(args)]
        if not ms:
            raise Fout(f"no method {kl.naam}.{naam}/{len(args)}")
        params, body = ms[0]
        try:
            self.ex(body, kl, [dict(zip(params, args))])
        except Terug as t:
            return t.v
        return None

    def ex(self, s, kl, sc):
        soort = s[0]
        if soort == "block":
            sc.append({})
            try:
                for x in s[1]:
                    self.ex(x, kl, sc)
            finally:
                sc.pop()
        elif soort == "decl":
            for naam, e, *_ in s[1]:
                sc[-1][naam] = self.ev(e, kl, sc) if e is not None else None
        elif soort == "expr":
            self.ev(s[1], kl, sc)
        elif soort == "if":
            if self.ev(s[1], kl, sc):
                self.ex(s[2], kl, sc)
            elif s[3] is not None:
                self.ex(s[3], kl, sc)
        elif soort == "for":
            sc.append({})
            try:
                if s[1]:
                    self.ex(s[1], kl, sc)
                n = 0
                while s[2] is None or self.ev(s[2], kl, sc):
                    n += 1
                    if n > 100000:
                        raise Fout("endless loop")
                    try:
                        self.ex(s[4], kl, sc)
                    except _Break:
                        break
                    except _Continue:
                        pass
                    for u in s[3]:
                        self.ev(u, kl, sc)
            finally:
                sc.pop()
        elif soort == "foreach":
            over = self.ev(s[2], kl, sc)
            for x in list(over):
                sc.append({s[1]: x})
                try:
                    self.ex(s[3], kl, sc)
                except _Break:
                    break
                except _Continue:
                    pass
                finally:
                    sc.pop()
        elif soort == "while":
            n = 0
            while self.ev(s[1], kl, sc):
                n += 1
                if n > 100000:
                    raise Fout("endless loop")
                self.ex(s[2], kl, sc)
        elif soort == "return":
            raise Terug(self.ev(s[1], kl, sc) if s[1] is not None else None)
        elif soort == "break":
            raise _Break()
        elif soort == "continue":
            raise _Continue()
        elif soort == "throw":
            raise Fout("throw")
        elif soort == "leeg":
            pass
        else:
            raise Fout(f"statement {soort}")

    def zet(self, doel, waarde, kl, sc):
        if doel[0] == "name":
            for s in reversed(sc):
                if doel[1] in s:
                    s[doel[1]] = waarde
                    return
            kl.statics[doel[1]] = waarde
        elif doel[0] == "index":
            self.ev(doel[1], kl, sc)[self.ev(doel[2], kl, sc)] = waarde
        elif doel[0] == "field":
            t = self.ev(doel[1], kl, sc)
            if isinstance(t, Klasse):
                t.statics[doel[2]] = waarde
            else:
                raise Fout("field assignment")
        else:
            raise Fout("assignment")

    def ev(self, e, kl, sc):
        soort = e[0]
        if soort == "lit":
            return e[1]
        if soort == "name":
            return self.zoek(e[1], kl, sc)
        if soort == "arr":
            return [self.ev(x, kl, sc) for x in e[1]]
        if soort == "cond":
            return self.ev(e[2], kl, sc) if self.ev(e[1], kl, sc) else self.ev(e[3], kl, sc)
        if soort == "bin":
            op = e[1]
            if op == "&&":
                return bool(self.ev(e[2], kl, sc)) and bool(self.ev(e[3], kl, sc))
            if op == "||":
                return bool(self.ev(e[2], kl, sc)) or bool(self.ev(e[3], kl, sc))
            a, b = self.ev(e[2], kl, sc), self.ev(e[3], kl, sc)
            return _bin(op, a, b)
        if soort == "un":
            a = self.ev(e[2], kl, sc)
            return -a if e[1] == "-" else (not a) if e[1] == "!" else (~a) if e[1] == "~" else a
        if soort == "cast":
            a = self.ev(e[2], kl, sc)
            return int(a) if e[1] in ("int", "long", "short", "byte") else float(a) if e[1] in ("double", "float") else a
        if soort == "assign":
            w = self.ev(e[3], kl, sc)
            if e[1] != "=":
                w = _bin(e[1][0], self.ev(e[2], kl, sc), w)
            self.zet(e[2], w, kl, sc)
            return w
        if soort in ("postinc", "preinc"):
            oud = self.ev(e[2], kl, sc)
            nieuw = oud + (1 if e[1] == "++" else -1)
            self.zet(e[2], nieuw, kl, sc)
            return oud if soort == "postinc" else nieuw
        if soort == "index":
            return self.ev(e[1], kl, sc)[self.ev(e[2], kl, sc)]
        if soort == "field":
            t = self.ev(e[1], kl, sc)
            naam = e[2]
            if isinstance(t, Pkg):
                k = self.klasse(naam) if naam[:1].isupper() else None
                return k if k else Pkg(naam)
            if isinstance(t, Klasse):
                return t.get(naam)
            if isinstance(t, list) and naam == "length":
                return len(t)
            if isinstance(t, Rec) and naam in t.v:
                return t.v[naam]
            if isinstance(t, Pos) and naam in ("x", "y", "z"):
                return t["xyz".index(naam)]
            if isinstance(t, Ns) and t.naam == "Integer" and naam in ("MAX_VALUE", "MIN_VALUE"):
                return 2 ** 31 - 1 if naam == "MAX_VALUE" else -2 ** 31
            if isinstance(t, Ns) and t.naam == "BlockPos" and naam == "ZERO":
                return Pos(0, 0, 0)
            if isinstance(t, Ns) and t.naam == "Vec3" and naam == "ZERO":
                return Pos(0.0, 0.0, 0.0)
            raise Fout(f"field {naam} of {type(t).__name__}")
        if soort == "call":
            args = [self.ev(a, kl, sc) for a in e[3]]
            if e[1] is None:
                return self.roep(kl, e[2], args)
            t = self.ev(e[1], kl, sc)
            if isinstance(t, Klasse):
                t.init() if not t.natief.get(e[2]) else None
                return self.roep(t, e[2], args)
            return _methode(t, e[2], args)
        if soort == "new":
            args = [self.ev(a, kl, sc) for a in e[2]]
            naam = e[1]
            if naam == "BlockPos":
                return _bp(*args)
            if naam == "Vec3":
                return Pos(float(args[0]), float(args[1]), float(args[2]))
            if naam in ("LinkedHashMap", "HashMap", "TreeMap", "ConcurrentHashMap", "EnumMap"):
                return dict(args[0]) if args and isinstance(args[0], dict) else {}
            if naam in ("ArrayList", "LinkedList", "CopyOnWriteArrayList", "ArrayDeque"):
                return list(args[0]) if args and isinstance(args[0], (list, tuple)) else []
            if naam in ("LinkedHashSet", "HashSet", "TreeSet"):
                return list(args[0]) if args else []
            for k in (kl,) + tuple(self.klassen.values()):
                if naam in k.records:
                    return Rec(naam, k.records[naam], args)
            raise Fout(f"new {naam}")
        if soort == "newarr":
            if e[3] is not None:
                return self.ev(e[3], kl, sc)
            n = self.ev(e[2][0], kl, sc)
            return [0 if e[1] in PRIM else None] * n
        if soort == "instanceof":
            v = self.ev(e[1], kl, sc)
            ok = v is not None and (e[2] not in ("BlockPos", "Vec3") or isinstance(v, Pos))
            if ok and e[3]:
                sc[-1][e[3]] = v
            return ok
        raise Fout(f"expression {soort}")


class _Break(Exception):
    pass


class _Continue(Exception):
    pass


def _bin(op, a, b):
    if op == "+":
        if isinstance(a, str) or isinstance(b, str):
            return _str(a) + _str(b)
        return a + b
    if op == "-":
        return a - b
    if op == "*":
        return a * b
    if op == "/":
        if isinstance(a, int) and isinstance(b, int):
            q = abs(a) // abs(b)
            return q if (a >= 0) == (b >= 0) else -q
        return a / b
    if op == "%":
        return a - b * int(a / b) if isinstance(a, int) and isinstance(b, int) else a % b
    if op == "==":
        return a is b if (a is None or b is None) else a == b
    if op == "!=":
        return a is not b if (a is None or b is None) else a != b
    if op == "<":
        return a < b
    if op == ">":
        return a > b
    if op == "<=":
        return a <= b
    if op == ">=":
        return a >= b
    if op == "&":
        return a & b
    if op == "|":
        return a | b
    if op == "^":
        return a ^ b
    raise Fout(f"operator {op}")


def _str(v):
    if v is None:
        return "null"
    if v is True or v is False:
        return "true" if v else "false"
    return str(v)


def _methode(t, naam, a):
    """A method on a value (dict = Map, list = List/array, str, Pos, Rec, Entry, Ns = static JDK call)."""
    if isinstance(t, Ns):
        n = t.naam
        if n == "Map":
            if naam == "entry":
                return (a[0], a[1])
            if naam == "ofEntries":
                return dict(a)
            if naam == "of":
                return dict(zip(a[0::2], a[1::2]))
            if naam == "copyOf":
                return dict(a[0])
        if n in ("List", "Set", "Arrays") and naam in ("of", "asList", "copyOf"):
            return list(a[0]) if len(a) == 1 and isinstance(a[0], list) and naam == "copyOf" else list(a)
        if n == "Collections" and naam.startswith(("unmodifiable", "synchronized")):
            return a[0]
        if n in ("Math", "Mth"):
            f = {"max": max, "min": min, "abs": abs, "round": lambda x: int(x + 0.5) // 1 if x >= 0 else -int(-x + 0.5),
                 "floor": lambda x: int(x // 1), "ceil": lambda x: -int(-x // 1), "sqrt": lambda x: x ** 0.5}.get(naam)
            if f:
                return f(*a)
        if n == "Integer" and naam == "parseInt":
            return int(a[0])
        if n == "String" and naam == "valueOf":
            return _str(a[0])
        if n == "Vec3" and naam in ("atCenterOf", "atBottomCenterOf", "atLowerCornerOf"):
            x, y, z = a[0]
            return Pos(x + .5, y + (.5 if naam == "atCenterOf" else 0), z + .5) if naam != "atLowerCornerOf" else Pos(float(x), float(y), float(z))
        if n == "BlockPos" and naam == "containing":
            x, y, z = a if len(a) == 3 else a[0]
            return _bp(x // 1, y // 1, z // 1)
        if n == "Objects" and naam == "requireNonNull":
            return a[0]
        raise Fout(f"{n}.{naam}")
    if isinstance(t, dict):
        if naam == "put":
            old = t.get(a[0])
            t[a[0]] = a[1]
            return old
        if naam == "putIfAbsent":
            return t.setdefault(a[0], a[1])
        if naam == "get":
            return t.get(a[0])
        if naam == "getOrDefault":
            return t.get(a[0], a[1])
        if naam == "containsKey":
            return a[0] in t
        if naam == "putAll":
            t.update(a[0])
            return None
        if naam == "entrySet":
            return [Entry(t, k) for k in list(t)]
        if naam == "keySet":
            return list(t)
        if naam == "values":
            return list(t.values())
        if naam == "size":
            return len(t)
        if naam == "isEmpty":
            return not t
        if naam == "remove":
            return t.pop(a[0], None)
    if isinstance(t, list):
        if naam == "get":
            return t[a[0]]
        if naam == "size":
            return len(t)
        if naam == "add":
            t.append(a[-1]) if len(a) == 1 else t.insert(a[0], a[1])
            return True
        if naam == "addAll":
            t.extend(a[-1])
            return True
        if naam == "isEmpty":
            return not t
        if naam == "contains":
            return a[0] in t
    if isinstance(t, str):
        if naam == "equals":
            return t == a[0]
        if naam == "substring":
            return t[a[0]:a[1]] if len(a) == 2 else t[a[0]:]
        if naam == "startsWith":
            return t.startswith(a[0])
        if naam == "endsWith":
            return t.endswith(a[0])
        if naam == "length":
            return len(t)
        if naam == "lastIndexOf":
            return t.rfind(a[0])
        if naam == "indexOf":
            return t.find(a[0])
        if naam == "replace":
            return t.replace(a[0], a[1])
        if naam == "contains":
            return a[0] in t
        if naam == "isEmpty":
            return not t
        if naam in ("toLowerCase", "toUpperCase"):
            return t.lower() if naam == "toLowerCase" else t.upper()
    if isinstance(t, Pos):
        x, y, z = t
        flt = isinstance(x, float)
        if naam in ("offset", "add") and len(a) == 3:
            return Pos(x + a[0], y + a[1], z + a[2]) if flt or naam == "add" else _bp(x + a[0], y + a[1], z + a[2])
        if naam in ("offset", "add") and len(a) == 1 and isinstance(a[0], Pos):
            return Pos(x + a[0][0], y + a[0][1], z + a[0][2])
        if naam in ("above", "below"):
            n = a[0] if a else 1
            return Pos(x, y + n if naam == "above" else y - n, z)
        if naam in ("north", "south", "east", "west"):
            n = a[0] if a else 1
            return Pos(x + (n if naam == "east" else -n if naam == "west" else 0), y, z + (n if naam == "south" else -n if naam == "north" else 0))
        if naam in ("getX", "x"):
            return x
        if naam in ("getY", "y"):
            return y
        if naam in ("getZ", "z"):
            return z
        if naam == "immutable":
            return t
        if naam == "equals":
            return tuple(t) == tuple(a[0])
    if isinstance(t, Rec):
        if naam in t.v and not a:
            return t.v[naam]
    if isinstance(t, Entry):
        if naam == "getKey":
            return t.k
        if naam == "getValue":
            return t.m[t.k]
        if naam == "setValue":
            t.m[t.k] = a[0]
            return None
    raise Fout(f"method {naam} on {type(t).__name__}")


# ---- the reflection look-ups of zones 4-6 (wereld(id) = the generated map class's plek(id)) -----------------------------------
def _wereld_van(klasse):
    def f(w, kl, id_):
        alle = w.statisch(klasse, "ALLE") or {}
        return alle.get(id_)
    return f


NATIEF = {
    "Z4VerhaalPlekken": {"wereld": _wereld_van("Z4WereldPlekken")},
    "Z5VerhaalPlekken": {"wereld": _wereld_van("Z5WereldPlekken")},
    "Z6VerhaalPlekken": {"wereld": _wereld_van("Z6WereldPlekken")},
}
