"""
1.10 "Het Vadsrijk", zone 5 "Hoogvads": the DROOM phase of the finale (T-DROOM). Java: feature/vadsrijk/zone5/droom.

De Grauwe Droom van Moeder Wolkwal (the part-2 deelbaas, a victim you cheer up). Everything generated, on the zone-1/2 helpers
(vadsrijk_wezens.Model/kf/anim, vadsrijk_z2_wezens anims) and t-z5wezens' painter (schilder):
  * GeckoLib models + animations (assets/guhs/geckolib/{models,animations}/entity/vadsrijk/zone5/droom/):
      grauwe_droom  a big whale of grey mist with tears (grauwe_droom.png), lilac after the humming (_lila), pink + happy at the end
                    (_roze; also the pet Droomwalvisje), glowmask _gloed
      wiegnoot      a lullaby note on a cloud-stone plinth, 4 colours (wiegnoot_0..3), glowmask
      grauwe_rank   a soft grey tendril of the Keizer's grip (no thorns, no gore)
  * item icons for the 3 named weapons, the Slaapmuts and the trophy; all texts (nl + en).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_droom.py
"""
import json
import os
import sys

try:
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features import vadsrijk_t_z5wezens as z5
    from features.vadsrijk_economie import Doek, zwaard, boog, staf
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features import vadsrijk_t_z5wezens as z5
    from features.vadsrijk_economie import Doek, zwaard, boog, staf

Model, kf, anim, anim_bestand = vw.Model, vw.kf, vw.anim, vw.anim_bestand
een, stil = z2.een, z2.stil
SUB = "vadsrijk/zone5/droom"

NOOT_KLEUREN = [(255, 138, 208), (255, 214, 42), (138, 208, 255), (192, 144, 255)]   # = GrauweDroom.NOOT_KLEUR


# =====================================================================================================================
# models
# =====================================================================================================================
def m_grauwe_droom():
    m = Model("grauwe_droom", px=2, breedte=256, bounds=(6, 4, 1))
    m.bone("root")
    m.bone("lijf", "root", (0, 14, 0))
    m.cube("lijf", "mist", (-16, 4, -32), (32, 24, 56), gezicht={"mood": "boos", "ey": 0.42})
    m.cube("lijf", "buik", (-13, 2, -27), (26, 3, 46))
    m.cube("lijf", "mist2", (-10, 27, -16), (14, 6, 14))                      # cloud puffs on the back
    m.cube("lijf", "mist2", (2, 28, 0), (12, 5, 12))
    m.cube("lijf", "mist2", (-6, 27, 12), (10, 4, 10))
    m.bone("tranen", "lijf", (0, 14, -32))
    m.cube("tranen", "traan", (-9, 9, -33), (2, 4, 1))
    m.cube("tranen", "traan", (7, 9, -33), (2, 4, 1))
    m.bone("spuit", "lijf", (0, 28, -20))
    m.cube("spuit", "mist2", (-2, 28, -22), (4, 6, 4))
    m.cube("spuit", "mist2", (-5, 33, -24), (10, 3, 8))
    m.bone("vin_l", "lijf", (-16, 10, -8))
    m.cube("vin_l", "mist2", (-27, 8, -13), (11, 3, 12))
    m.bone("vin_r", "lijf", (16, 10, -8))
    m.cube("vin_r", "mist2", (16, 8, -13), (11, 3, 12))
    m.bone("staart", "lijf", (0, 16, 24))
    m.cube("staart", "mist", (-8, 10, 24), (16, 12, 16))
    m.bone("vinstaart", "staart", (0, 16, 40))
    m.cube("vinstaart", "mist2", (-19, 14, 38), (38, 4, 10))
    vinnen = {"vin_l": {"rotation": kf([(0, (0, 0, -10)), (1.5, (0, 0, 12)), (3, (0, 0, -10))])},
              "vin_r": {"rotation": kf([(0, (0, 0, 10)), (1.5, (0, 0, -12)), (3, (0, 0, 10))])}}
    staart = {"staart": {"rotation": kf([(0, (-6, 0, 0)), (1.5, (6, 0, 0)), (3, (-6, 0, 0))])},
              "vinstaart": {"rotation": kf([(0, (-10, 0, 0)), (1.5, (10, 0, 0)), (3, (-10, 0, 0))])}}
    bob = {"lijf": {"position": kf([(0, (0, 0, 0)), (1.5, (0, 1.5, 0)), (3, (0, 0, 0))])},
           "tranen": {"position": kf([(0, (0, 0, 0)), (1.0, (0, -1.5, 0)), (1.01, (0, 0, 0)), (3, (0, -1, 0))])}}
    swim = z2.samen(bob, vinnen, staart)
    a = {
        "idle": anim(3.0, swim),
        "walk": anim(3.0, swim),
        "zing": een(1.2, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.6, (1.08, 1.12, 1.05)), (1.2, (1, 1, 1))]),
                                   "rotation": kf([(0, (0, 0, 0)), (0.6, (-8, 0, 0)), (1.2, (0, 0, 0))])},
                          "spuit": {"scale": kf([(0, (1, 1, 1)), (0.6, (1.4, 1.6, 1.4)), (1.2, (1, 1, 1))])}}),
        "zwiep": een(0.8, {"staart": {"rotation": kf([(0, (0, 0, 0)), (0.3, (0, 35, 0)), (0.8, (0, 0, 0))])},
                           "vinstaart": {"rotation": kf([(0, (0, 0, 0)), (0.3, (0, 20, 15)), (0.8, (0, 0, 0))])}}),
        "schrik": een(0.8, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.15, (0, 3, 0)), (0.8, (0, 0, 0))]),
                                     "scale": kf([(0, (1, 1, 1)), (0.15, (1.06, 1.1, 1.06)), (0.8, (1, 1, 1))])}}),
        "blij": anim(1.5, z2.samen({"root": {"position": kf([(0, (0, 0, 0)), (0.75, (0, 4, 0)), (1.5, (0, 0, 0))])}}, vinnen)),
    }
    pal = {"mist": ((128, 130, 146), "plekjes", (110, 112, 128)), "mist2": ((146, 148, 162), "plekjes", (124, 126, 140)),
           "buik": ((170, 172, 184), "strepen", (150, 152, 166)), "traan": ((150, 196, 255), None, None)}
    return m, a, pal, {"traan": (60, 110, 200)}


def droom_versie(pal, soort):
    """lila: hums along (lilac mist, 'o' face); roze: cheered up (pink, happy, no tears)."""
    p = dict(pal)
    if soort == "lila":
        p["mist"] = ((200, 172, 236), "plekjes", (180, 150, 222))
        p["mist2"] = ((214, 190, 244), "plekjes", (196, 168, 232))
        p["buik"] = ((232, 214, 250), "strepen", (214, 194, 240))
    else:
        p["mist"] = ((255, 190, 220), "plekjes", (250, 166, 204))
        p["mist2"] = ((255, 212, 232), "plekjes", (252, 186, 216))
        p["buik"] = ((255, 236, 244), "strepen", (255, 214, 230))
        p["traan"] = p["mist"]
    return p


def m_wiegnoot():
    m = Model("wiegnoot", px=4, breedte=64, bounds=(2, 3, 0.8))
    m.bone("root")
    m.cube("root", "wolksteen", (-5, 0, -5), (10, 3, 10))
    m.cube("root", "wolksteen", (-3, 3, -3), (6, 9, 6))
    m.cube("root", "goud", (-4, 12, -4), (8, 1, 8))
    m.bone("noot", "root", (0, 18, 0))
    m.cube("noot", "noot", (-4, 13, -2), (6, 5, 4))                       # the note head
    m.cube("noot", "noot", (1, 17, -1), (2, 9, 2))                        # the stem
    m.cube("noot", "noot", (2, 23, -1), (4, 2, 2))                        # the flag
    m.cube("noot", "noot", (4, 21, -1), (2, 2, 2))
    a = {"idle": anim(2.4, {"noot": {"position": kf([(0, (0, 0, 0)), (1.2, (0, 1.2, 0)), (2.4, (0, 0, 0))]),
                                     "rotation": kf([(0, (0, 0, 0)), (2.4, (0, 360, 0))])}}),
         "klop": een(0.5, {"noot": {"scale": kf([(0, (1, 1, 1)), (0.12, (1.45, 1.45, 1.45)), (0.5, (1, 1, 1))])}})}
    pal = {"wolksteen": ((236, 238, 246), "plekjes", (214, 218, 232)), "goud": ((240, 200, 90), "glans", None)}
    return m, a, pal, {"noot": (200, 200, 210)}


def m_grauwe_rank():
    m = Model("grauwe_rank", px=4, breedte=64, bounds=(2, 3, 0.8))
    m.bone("root")
    m.cube("root", "wortel", (-4, 0, -4), (8, 2, 8))
    m.bone("rank1", "root", (0, 2, 0))
    m.cube("rank1", "rank", (-2, 2, -2), (4, 12, 4))
    m.cube("rank1", "blad", (2, 8, -1), (3, 1, 2))
    m.bone("rank2", "rank1", (0, 14, 0))
    m.cube("rank2", "rank", (-1.5, 14, -1.5), (3, 11, 3))
    m.cube("rank2", "blad", (-4.5, 19, -1), (3, 1, 2))
    m.bone("rank3", "rank2", (0, 25, 0))
    m.cube("rank3", "rank", (-1, 25, -1), (2, 9, 2))
    m.cube("rank3", "krul", (-1, 33, -3), (2, 2, 4))
    zwaai = {"rank1": {"rotation": kf([(0, (0, 0, -6)), (1.2, (0, 0, 6)), (2.4, (0, 0, -6))])},
             "rank2": {"rotation": kf([(0, (0, 0, -10)), (1.2, (0, 0, 10)), (2.4, (0, 0, -10))])},
             "rank3": {"rotation": kf([(0, (0, 0, -14)), (1.2, (0, 0, 14)), (2.4, (0, 0, -14))])}}
    a = {"idle": anim(2.4, zwaai),
         "grijp": een(0.7, {"rank1": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-25, 0, 0)), (0.7, (0, 0, 0))])},
                            "rank2": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-35, 0, 0)), (0.7, (0, 0, 0))])},
                            "rank3": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-50, 0, 0)), (0.7, (0, 0, 0))])}})}
    pal = {"wortel": ((110, 110, 124), "plekjes", (94, 94, 108)), "rank": ((128, 126, 142), "strepen", (108, 106, 122)),
           "blad": ((150, 150, 162), None, None), "krul": ((140, 138, 154), None, None)}
    return m, a, pal, {}


MODELLEN = [("grauwe_droom", m_grauwe_droom), ("wiegnoot", m_wiegnoot), ("grauwe_rank", m_grauwe_rank)]
NODIG = {"grauwe_droom": ("idle", "walk", "zing", "zwiep", "schrik", "blij"), "wiegnoot": ("idle", "klop"), "grauwe_rank": ("idle", "grijp")}


# =====================================================================================================================
# items
# =====================================================================================================================
def noottop(d):
    d.disc(10.5, 5, 2.2, (255, 138, 208))
    d.lijn(12.5, 5, 12.5, 0.5, (255, 138, 208))
    d.lijn(12.5, 0.5, 14.5, 1.5, (255, 138, 208))
    d.px(9.8, 4.3, (255, 230, 245))


def muts_icoon():
    d = Doek()
    d.poly([(3, 12), (13, 12), (11, 5), (14, 2), (8, 3), (5, 6)], (176, 196, 255))
    d.lijn(3, 12, 13, 12, (255, 255, 255), dik=2)
    d.disc(14, 2, 1.6, (255, 236, 120))
    for x, y in ((6, 9), (9, 7), (10, 10)):
        d.px(x, y, (255, 236, 120))
    d.schaduw()
    return d.rand()


def potje_icoon():
    d = Doek()
    d.rect(4, 5, 12, 14, (200, 226, 250))
    d.rect(4, 3, 12, 5, (200, 150, 110))
    d.disc(8, 10, 2.6, (255, 190, 220))
    d.disc(6.5, 9, 1.4, (255, 212, 232))
    d.px(8, 7, (255, 255, 255))
    d.schaduw()
    return d.rand()


ITEMS = {
    # item id: (type or None, nl, en, icon maker, flavour nl, flavour en, handheld)
    "vr_z5_droom_wiegeliedstaf": ("z5droom_wiegeliedstaf", "Wiegeliedstaf", "Lullaby Staff", lambda: staf(noottop),
                                  "Hij neuriet zachtjes als je hem vasthoudt. De goede kant op.", "It hums softly when you hold it. The right way round.", True),
    "vr_z5_droom_dromenzwaard": ("z5droom_dromenzwaard", "Zwaard van de Zoete Droom", "Sword of Sweet Dreams",
                                 lambda: zwaard((255, 200, 228), (200, 170, 240)), "Wie ermee geraakt wordt, gaapt even.",
                                 "Whoever it hits yawns for a moment.", True),
    "vr_z5_droom_sterrenboog": ("z5droom_sterrenboog", "Slaapsterrenboog", "Sleepy Star Bow", lambda: boog((150, 170, 230), (255, 236, 120)),
                                "Elke pijl is een vallende ster. Doe een wens.", "Every arrow is a shooting star. Make a wish.", True),
    "vr_z5_droom_slaapmuts": ("z5droom_slaapmuts", "Moeder Wolkwals Slaapmuts", "Mother Cloudwhale's Nightcap", muts_icoon,
                              "Hij past op elk hoofd. En op een walvis.", "It fits any head. And a whale.", False),
    "vr_z5_droom_trofee": (None, "Een Fijne Droom (in een potje)", "A Sweet Dream (in a jar)", potje_icoon, None, None, False),
}


# =====================================================================================================================
# texts
# =====================================================================================================================
Z = "vadsrijk.z5droom."
TEKSTEN = {
    "entity.guhs.vr_z5_baas_grauwe_droom": ("De Grauwe Droom", "The Grey Dream"),
    "entity.guhs.vr_z5_baas_wiegnoot": ("Slaapliedjesnoot", "Lullaby Note"),
    "entity.guhs.vr_z5_baas_grauwe_rank": ("Grauwe Rank", "Grey Tendril"),
    Z + "baas.grauwe_droom": ("De Grauwe Droom", "The Grey Dream"),
    Z + "baas.grauwe_droom.onder": ("De nare droom van Moeder Wolkwal", "Mother Cloudwhale's bad dream"),
    "vadsrijk.z2bazen.tip.grauwe_droom.titel": ("De Grauwe Droom", "The Grey Dream"),
    "vadsrijk.z2bazen.tip.grauwe_droom": ("Je bent in de droom van Moeder Wolkwal. Grijze ranken? Sla ze stuk: elke rank maakt de droom taaier en houdt je vast.",
                                          "You are inside Mother Cloudwhale's dream. Grey tendrils? Cut them: each one makes the dream tougher and holds you."),
    "vadsrijk.z3bazen.tip.grauwe_droom.titel": ("Het slaapliedje", "The lullaby"),
    "vadsrijk.z3bazen.tip.grauwe_droom": ("Lichten de noten op? Onthoud de volgorde en rechtsklik ze daarna precies zo. Gele cirkel: ga er samen in staan en neurie!",
                                          "Do the notes light up? Remember the order and right-click them the same way. Yellow circle: stand in it together and hum!"),
    "vadsrijk.kern.gebied.z5d_droom": ("De Droom van Moeder Wolkwal", "Mother Cloudwhale's Dream"),
    Z + "inslapen": ("Je zakt in een droom...", "You sink into a dream..."),
    Z + "inslapen.onder": ("De droom van Moeder Wolkwal is grauw", "Mother Cloudwhale's dream is grey"),
    Z + "inslapen.herinnering": ("Je droomt het nog een keer", "You dream it once more"),
    Z + "wakker": ("Je wordt wakker", "You wake up"),
    Z + "wakker.onder": ("Moeder Wolkwal slaapt weer zoet", "Mother Cloudwhale sleeps sweetly again"),
    Z + "vangnet": ("Een droomwolkje vangt je op en zet je terug.", "A dream cloud catches you and puts you back."),
    Z + "herinnering.bezig": ("Je ogen worden zwaar... (blijf even hurken)", "Your eyes grow heavy... (keep sneaking)"),
    Z + "fase2": ("De droom draait om!", "The dream turns over!"),
    Z + "fase2.onder": ("Sta op de roze wolkjes. Straks: het slaapliedje", "Stand on the pink clouds. Soon: the lullaby"),
    Z + "fase3": ("Samen neuriën!", "Hum together!"),
    Z + "fase3.onder": ("Ga in de gele cirkel staan tot de neurie-meter vol is", "Stand in the yellow circle until the hum meter is full"),
    Z + "praat.fase2": ("Alles is grauw... alles is zwaar... waar is mijn liedje?", "Everything is grey... everything is heavy... where is my song?"),
    Z + "praat.fase3": ("Ik... ik hoor iets. Zachtjes. Neurie nog eens?", "I... I hear something. Softly. Hum it again?"),
    Z + "praat.achterstevoren": ("Ei-wo... lei-wo... ze zingen het achterstevoren...", "Bye-lul... by-baby... they sing it backwards..."),
    Z + "praat.neurie": ("Mmmmm... dat liedje ken ik. Dat zong mijn mama.", "Mmmmm... I know that song. My mum used to sing it."),
    Z + "praat.blij": ("Wat een fijne droom... dank je wel, kleine ridder.", "What a lovely dream... thank you, little knight."),
    Z + "wiegelied.luister": ("Luister! De noten lichten op in de GOEDE volgorde...", "Listen! The notes light up in the RIGHT order..."),
    Z + "wiegelied.nu": ("Nu jij! Rechtsklik de %s noten in dezelfde volgorde.", "Your turn! Right-click the %s notes in the same order."),
    Z + "wiegelied.goed": ("♪ Goed zo! (%s/%s)", "♪ Well done! (%s/%s)"),
    Z + "wiegelied.vals": ("Vals! Begin weer bij de eerste noot.", "Off-key! Start again from the first note."),
    Z + "wiegelied.tijd": ("Nog %s s! (%s/%s noten)", "%s s left! (%s/%s notes)"),
    Z + "getroost": ("Het slaapliedje klinkt weer goed!", "The lullaby sounds right again!"),
    Z + "getroost.onder": ("De droom luistert: 8 s stil en +50% schade", "The dream listens: 8 s still and +50% damage"),
    Z + "nachtmerrie": ("Nachtmerrie!", "Nightmare!"),
    Z + "nachtmerrie.onder": ("Te laat: het achterstevoren liedje wint even", "Too late: the backwards song wins for now"),
    Z + "ranken": ("Grauwe ranken groeien uit de wolk! Sla ze stuk.", "Grey tendrils grow out of the cloud! Cut them."),
    Z + "geknipt": ("Doorgeknipt!", "Snipped!"),
    Z + "omkeer": ("De droom draait om! Alleen de roze wolkjes zijn veilig.", "The dream turns over! Only the pink clouds are safe."),
    Z + "omkeer_weg": ("De droom wordt weer licht.", "The dream turns light again."),
    Z + "neurie.meter": ("Neurie-meter: %s%%", "Hum meter: %s%%"),
    Z + "neurie.klaar": ("Moeder Wolkwal neuriet mee!", "Mother Cloudwhale hums along!"),
    Z + "neurie.klaar.onder": ("De droom wordt lila: +25% schade", "The dream turns lilac: +25% damage"),
    Z + "neurie_eerst": ("De droom is te zwaar: neurie eerst samen in de gele cirkel!", "The dream is too heavy: hum together in the yellow circle first!"),
    Z + "loot.guhddelijk": ("✦ %s vond iets GUHDDELIJKS: %s!", "✦ %s found something GUHDLIKE: %s!"),
    "vadsrijk.huisdier.droomwalvisje.naam": ("Droomwalvisje", "Dream Whaleling"),
    "vadsrijk.huisdier.droomwalvisje.tekst": ("Een piepklein roze wolkwalvisje dat 's nachts zachtjes neuriet.",
                                              "A tiny pink cloud whale that hums softly at night."),
    "vadsrijk.huisdier.droomwalvisje.bron": ("Vrolijk de Grauwe Droom op in de droom van Moeder Wolkwal.", "Cheer up the Grey Dream in Mother Cloudwhale's dream."),
}


def teksten():
    t = dict(TEKSTEN)
    for item, (typ, nl, en, _maak, fl_nl, fl_en, _h) in ITEMS.items():
        t["item.guhs." + item] = (nl, en)
        if typ:
            t["vadsrijk.loot." + typ] = (nl, en)
            t[f"vadsrijk.loot.{typ}.flavour.0"] = ("\"" + fl_nl + "\"", "\"" + fl_en + "\"")
    return t


# =====================================================================================================================
# writing
# =====================================================================================================================
def controleer(naam, m, anims):
    bones = {b["name"] for b in m.bones}
    for an, a in anims.items():
        for b in a["bones"]:
            assert b in bones, f"{naam}: anim {an} moves unknown bone {b}"
    for nodig in NODIG[naam]:
        assert nodig in anims, f"{naam}: missing anim {nodig}"
    assert "root" in bones, f"{naam}: no root bone (the pet animation needs it)"


def _met_mood(m, mood):
    for reg in m.regios:
        if reg["gezicht"]:
            reg["gezicht"] = dict(reg["gezicht"], mood=mood)


def assets(a, w=vw._schrijf_json):
    geo_dir = os.path.join(a, "geckolib", "models", "entity", *SUB.split("/"))
    anim_dir = os.path.join(a, "geckolib", "animations", "entity", *SUB.split("/"))
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    for i, (naam, maak) in enumerate(MODELLEN):
        m, anims, pal, glow = maak()
        controleer(naam, m, anims)
        w(os.path.join(geo_dir, naam + ".geo.json"), m.geo())
        w(os.path.join(anim_dir, naam + ".animation.json"), anim_bestand(anims))
        vw._png(os.path.join(tex_dir, naam + "_gloed.png"), z5.schilder(m, pal, 6300 + i, glow, glow=True))
        if naam == "wiegnoot":
            for k, kleur in enumerate(NOOT_KLEUREN):
                p = dict(pal, noot=(kleur, "glans", None))
                vw._png(os.path.join(tex_dir, f"wiegnoot_{k}.png"), z5.schilder(m, p, 6200 + k))
            continue
        vw._png(os.path.join(tex_dir, naam + ".png"), z5.schilder(m, pal, 6200 + i))
        if naam == "grauwe_droom":
            _met_mood(m, "o")
            vw._png(os.path.join(tex_dir, "grauwe_droom_lila.png"), z5.schilder(m, droom_versie(pal, "lila"), 6210))
            _met_mood(m, "blij")
            vw._png(os.path.join(tex_dir, "grauwe_droom_roze.png"), z5.schilder(m, droom_versie(pal, "roze"), 6211))
    for item, (typ, nl, en, maak, _f1, _f2, handheld) in ITEMS.items():
        tex = item.replace("vr_z5_droom_", "")
        z5._bewaar(os.path.join(a, "textures", "item", *SUB.split("/"), tex + ".png"), maak())
        w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
        w(os.path.join(a, "models", "item", item + ".json"), {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
                                                              "textures": {"layer0": f"guhs:item/{SUB}/{tex}"}})
    return len(MODELLEN)


def build(h):
    assets(h.A, h.w)
    for k, (nl, en) in teksten().items():
        h.lang(k, nl, en)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    n = assets(a)
    t = teksten()
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        vw._schrijf_json(p, data)
    print(f"vadsrijk_t_droom: {n} models, {len(ITEMS)} items, {len(t)} texts")


if __name__ == "__main__":
    _direct()
