"""
bbq2 (ring-h3) - the two camera scenes of the mine, written ONCE, here, in template coordinates:

  ringh3_emmer   the well room: Pippguh, a bucket, a very long silence, drums.
  ringh3_brug    the bridge: the showpiece of the update (DESIGN_130 4, chapter 3): the slow reveal out of the dark, the flight
                 over the Brokkelpad and the bridge, Guhdalf alone on the span, "YOU.. SHALL.. NOT.. VADS!" word by word, the
                 flash, the bridge breaking, the whip, "Vlucht, dwazen... njeg".

ring_h3_java.py turns these scripts into feature/ringh3/Scenes.java (the Cutscene builders of the verhaal engine); ring_h3.py
writes their texts; ring_h3_bouw / ring_h3_beeld check every camera and every actor against the template (a camera in the
rock, an actor in the air) and draw frames of them. So what the scene says and where the mine is can't drift apart.

A scene is a Scene object: actors, and a list of events (t in ticks, positions in template coordinates, feet level).
"""
from features import ring_h3_bouw as B

D = float(B.DIEP)            # the floor of the great hall, the path and the bridge
M = float(B.MIDDEL)

ZIN_YOU = "YOU.. SHALL.. NOT.. VADS!"       # English in BOTH languages (DESIGN_130 4): the one line of the mod that is never translated


class Scene:
    def __init__(self, id, titel, anker, duur, verberg):
        self.id, self.titel, self.anker, self.duur, self.verberg = id, titel, anker, duur, verberg
        self.acteurs = []        # (naam, soort, arg, start, yaw)   soort: speler | npc | guh | wezen
        self.camera = []         # (t, pos, kijk or None, volgt or None, knip)
        self.lopen = []          # (acteur, t0, t1, naar)
        self.kijken = []         # (acteur, t, naar)
        self.animaties = []      # (acteur, t, naam)
        self.zinnen = []         # (t, spreker, key, ticks)
        self.teksten = {}        # key -> Dutch
        self.namen = {}          # acteur -> the name under the bars
        self.geluiden = []       # (t, sound field of RingH3Feature or a vanilla SoundEvents field, volume, pitch)
        self.deeltjes = []       # (t, ParticleTypes field, pos, aantal, spreiding)
        self.schudden = []       # (t, kracht, ticks)
        self.zwart = []          # (t0, t1)
        self.flitsen = []        # (t, ticks, sterkte 0..1): a white flash (drawn by the client of this slice)

    # --- building -----------------------------------------------------------------------------------------------------------
    def speler(self, start, yaw):
        self.acteurs.append(("speler", "speler", None, tuple(start), yaw))

    def npc(self, naam, kind, start, yaw, toon=None):
        self.acteurs.append((naam, "npc", kind, tuple(start), yaw))
        if toon:
            self.namen[naam] = toon

    def guh(self, naam, variant, start, yaw, toon=None):
        self.acteurs.append((naam, "guh", variant, tuple(start), yaw))
        if toon:
            self.namen[naam] = toon

    def wezen(self, naam, veld, start, yaw, toon=None):
        self.acteurs.append((naam, "wezen", veld, tuple(start), yaw))
        if toon:
            self.namen[naam] = toon

    def cam(self, t, pos, kijk, knip=False):
        self.camera.append((t, tuple(pos), tuple(kijk), None, knip))

    def camv(self, t, pos, acteur, knip=False):
        self.camera.append((t, tuple(pos), None, acteur, knip))

    def loop(self, acteur, t0, t1, naar):
        self.lopen.append((acteur, t0, t1, tuple(naar)))

    def ren(self, acteur, t0, punten, snelheid=0.26):
        """Along a row of points at `snelheid` blocks per tick, starting at t0; returns when it arrives."""
        hier = self.plek(acteur, t0)
        t = float(t0)
        for p in punten:
            lang = ((p[0] - hier[0]) ** 2 + (p[1] - hier[1]) ** 2 + (p[2] - hier[2]) ** 2) ** 0.5
            t1 = t + max(2.0, lang / snelheid)
            self.loop(acteur, int(round(t)), int(round(t1)), p)
            t, hier = t1, p
        return int(round(t))

    def kijk(self, acteur, t, naar):
        self.kijken.append((acteur, t, tuple(naar)))

    def anim(self, acteur, t, naam):
        self.animaties.append((acteur, t, naam))

    def zeg(self, t, spreker, key, ticks, tekst):
        self.zinnen.append((t, spreker, key, ticks))
        self.teksten[key] = tekst

    def geluid(self, t, veld, volume=1.0, pitch=1.0):
        self.geluiden.append((t, veld, volume, pitch))

    def deeltje(self, t, soort, pos, aantal, spreiding):
        self.deeltjes.append((t, soort, tuple(pos), aantal, spreiding))

    def schud(self, t, kracht, ticks):
        self.schudden.append((t, kracht, ticks))

    def donker(self, t0, t1):
        self.zwart.append((t0, t1))

    def flits(self, t, ticks, sterkte):
        self.flitsen.append((t, ticks, sterkte))

    # --- the script, evaluated (the same rules as feature/verhaal/Cutscene) ------------------------------------------------------
    def start(self, acteur):
        for naam, soort, arg, start, yaw in self.acteurs:
            if naam == acteur:
                return start
        raise KeyError(acteur)

    def plek(self, acteur, t):
        pos = self.start(acteur)
        for a, t0, t1, naar in sorted((l for l in self.lopen if l[0] == acteur), key=lambda l: l[1]):
            if t < t0:
                continue
            if t >= t1:
                pos = naar
            else:
                f = (t - t0) / (t1 - t0)
                return tuple(pos[i] + (naar[i] - pos[i]) * f for i in range(3))
        return pos

    def camera_op(self, t):
        """(pos, look at) at time t (linear between keyframes of one shot; the game glides on a spline)."""
        cams = sorted(self.camera, key=lambda c: c[0])
        i = 0
        while i + 1 < len(cams) and cams[i + 1][0] <= t:
            i += 1
        a = cams[i]

        def doel(c):
            return tuple(v + (1.0 if k == 1 else 0.0) for k, v in enumerate(self.plek(c[3], t))) if c[3] else c[2]
        if i + 1 >= len(cams) or t <= a[0] or cams[i + 1][4]:
            return a[1], doel(a)
        b = cams[i + 1]
        f = (t - a[0]) / max(1, b[0] - a[0])
        zacht = f * f * (3 - 2 * f)
        return (tuple(a[1][k] + (b[1][k] - a[1][k]) * f for k in range(3)),
                tuple(doel(a)[k] + (doel(b)[k] - doel(a)[k]) * zacht for k in range(3)))

    def animatie_op(self, acteur, t):
        nu = ("", 0)
        for a, t0, naam in sorted(self.animaties, key=lambda x: x[1]):
            if a == acteur and t0 <= t:
                nu = (naam, t0)
        return nu


# =====================================================================================================================
# the well
# =====================================================================================================================
def emmer():
    s = Scene("ringh3_emmer", "De emmer van Pippguh", B.PLEKKEN["PUT"], 470, 14)
    s.speler((27.5, M, 35.5), 90)
    s.npc("pippguh", "PIPPGUH", (24.5, M, 34.5), 100, "Pippguh")
    s.npc("merrie", "MERRIE", (24.5, M, 37.5), 60, "Merrie")
    s.npc("guhdalf", "GUHDALF", (17.5, M, 32.5), 300, "Guhdalf")
    s.npc("gimguh", "GIMGUH", (16.5, M, 37.5), 280, "Gimguh")
    s.guh("sam", "SAM_GUH", (26.5, M, 37.5), 90, "Sam-guh")
    # a tired party looks round an old guard room
    s.cam(0, (27.2, M + 4.2, 41.2), (21.5, M + 1.0, 35.5))
    s.cam(50, (26.2, M + 3.4, 40.2), (21.5, M + 1.0, 35.5))
    s.zeg(8, "", "begin", 44, "Een oude wachtkamer. Een put. En op de rand... een emmertje.")
    # Pippguh can't leave it alone
    s.cam(55, (25.8, M + 1.5, 32.7), (24.4, M + 0.8, 34.6), knip=True)
    s.cam(118, (26.0, M + 1.3, 33.1), (24.3, M + 0.8, 34.7))
    s.kijk("pippguh", 55, (21.5, M + 1.0, 35.5))
    s.anim("pippguh", 60, "kijk")
    s.zeg(62, "pippguh", "emmertje", 52, "Ooo. Zou daar nog knabbel in zitten?")
    s.anim("pippguh", 104, "grijp")
    # ... and down it goes
    s.geluid(120, "EMMER", 1.0, 1.0)
    s.anim("pippguh", 122, "schrik")
    s.cam(122, (22.4, M + 3.5, 34.5), (21.5, 6.0, 35.5), knip=True)
    s.cam(196, (22.3, M + 2.6, 34.6), (21.5, 6.0, 35.5))
    s.zeg(124, "", "kleng", 70, "KLENG... kleng... kleng...")
    s.geluid(146, "EMMER", 0.6, 0.8)
    s.geluid(174, "EMMER", 0.3, 0.62)
    s.schud(120, 0.6, 8)
    # the longest silence in the history of the dwarf-guhs
    s.cam(200, (27.0, M + 1.7, 39.0), (20.5, M + 1.0, 34.5), knip=True)
    s.cam(300, (26.4, M + 1.7, 38.4), (20.5, M + 1.0, 34.5))
    for naam in ("merrie", "gimguh", "guhdalf", "sam", "speler"):
        s.kijk(naam, 200, (24.5, M + 0.5, 34.5))
    s.zeg(206, "", "stil", 36, "...")
    # drums in the deep
    for i, t in enumerate((246, 266, 286)):
        s.geluid(t, "TROMMEL", 1.0, 0.9 - i * 0.04)
        s.schud(t, 0.7, 6)
    s.zeg(248, "", "doem", 50, "Doem... doem... doem.")
    s.anim("merrie", 250, "schrik")
    s.anim("gimguh", 268, "schrik")
    # Guhdalf has something to say about it
    s.cam(304, (19.6, M + 1.5, 34.2), (17.5, M + 1.2, 32.5), knip=True)
    s.cam(384, (19.9, M + 1.5, 34.6), (17.5, M + 1.2, 32.5))
    s.kijk("guhdalf", 300, (24.5, M + 0.5, 34.5))
    s.anim("guhdalf", 304, "ruzie")
    s.zeg(306, "guhdalf", "dwaas", 80, "Dwaas van een Pippguh! Gooi de volgende keer jezelf erin, dan zijn we van je gesnoep af!")
    s.anim("pippguh", 330, "schaam")
    # Gimguh knows the way
    s.cam(388, (22.2, M + 1.6, 39.6), (16.5, M + 1.0, 37.3), knip=True)
    s.cam(455, (21.4, M + 1.6, 39.2), (16.5, M + 1.0, 37.3))
    s.geluid(392, "TROMMEL", 1.0, 0.8)
    s.geluid(424, "TROMMEL", 1.0, 0.8)
    s.anim("gimguh", 392, "wijs")
    s.kijk("gimguh", 388, (13.0, M + 1.0, 35.5))
    s.zeg(392, "gimguh", "weg", 64, "Trommels in de diepte. Ze komen! Er is hier een dwergendeur, alleen... welke rune was het ook alweer?")
    s.donker(456, 470)
    return s


# =====================================================================================================================
# the bridge
# =====================================================================================================================
ROG = (11.5, D, 14.5)
RAND = (41.5, D, 14.5)              # the edge of the hall
HOOFD = (63.5, D, 14.5)             # the bridgehead
STAAT = (73.5, D, 14.5)             # where Guhdalf makes his stand: the first stone east of what breaks
OEVER = 79.5
# the way of the fleeing fellowship: down the nave, over the Brokkelpad from pillar to pillar, over the bridge
VLUCHT = [(45.5, D, 14.0), (53.0, D, 14.0), (53.0, D, 8.0), (58.0, D, 8.0), (58.0, D, 20.0), (62.0, D, 20.0), (62.5, D, 16.5), (64.5, D, 14.5),
          (66.5, D, 14.5), (76.5, D, 14.5)]
BREEKT = 1300
SNEL = 0.31                         # blocks per tick of the flight


def brug():
    s = Scene("ringh3_brug", "De brug van Knabbel-dûm", B.PLEKKEN["BRUG_ANKER"], 1770, 70)
    s.wezen("rog", "BARBECUEROG", ROG, 270, "De Barbecuerog")
    s.npc("guhdalf", "GUHDALF", (40.5, D, 14.5), 90, "Guhdalf")
    gezelschap = [("araguh", "npc", "ARAGUH", (37.5, D, 12.5), "Araguh", (81.5, D, 12.5)),
                  ("leguhlas", "npc", "LEGUHLAS", (36.5, D, 16.5), "Leguhlas", (82.5, D, 17.5)),
                  ("speler", "speler", None, (38.5, D, 15.5), None, (80.5, D, 15.5)),
                  ("sam", "guh", "SAM_GUH", (37.5, D, 14.5), "Sam-guh", (81.5, D, 16.5)),
                  ("gimguh", "npc", "GIMGUH", (35.5, D, 13.5), "Gimguh", (83.5, D, 13.5)),
                  ("boromika", "npc", "BOROMIKA", (34.5, D, 15.5), "Boromika", (84.5, D, 15.5)),
                  ("merrie", "npc", "MERRIE", (33.5, D, 13.5), "Merrie", (82.5, D, 11.5)),
                  ("pippguh", "npc", "PIPPGUH", (32.5, D, 15.5), "Pippguh", (83.5, D, 18.5))]
    for naam, soort, arg, start, toon, einde in gezelschap:
        if soort == "speler":
            s.speler(start, 90)
        elif soort == "guh":
            s.guh(naam, arg, start, 90, toon)
        else:
            s.npc(naam, arg, start, 90, toon)
        s.kijk(naam, 0, ROG)
    s.kijk("guhdalf", 0, ROG)

    # ---- A. the dark (0 - 200): the nave, seen from the chasm end; far away, in the Diepe Poort, something opens its eyes ----
    s.anim("rog", 1, "donker")
    s.donker(-10, 6)
    s.cam(0, (43.5, D + 3.4, 14.5), (11.5, D + 5.0, 14.5))
    s.cam(200, (33.0, D + 2.6, 14.5), (11.5, D + 6.0, 14.5))
    for t in (12, 44, 76, 108, 140, 172):
        s.geluid(t, "TROMMEL", 1.0, 0.8)
        s.schud(t, 0.5, 5)
    s.zeg(24, "", "trommels", 66, "Trommels. Trommels in de diepte.")
    s.zeg(112, "", "ogen", 70, "En in het donker van de Diepe Poort gingen twee ogen open.")
    s.anim("pippguh", 120, "schrik")
    s.anim("merrie", 128, "schrik")

    # ---- B. he comes (200 - 420): from below, at his feet: flame by flame, then to his full height, wings wide ----
    s.anim("rog", 200, "opkomst")                     # 7 s: flames from 1.2 s, the blade at 2.6 s, rising from 3.4 s, the roar at 5 s
    s.cam(200, (21.0, D + 0.7, 12.4), (11.5, D + 5.5, 14.5), knip=True)
    s.cam(300, (24.0, D + 0.8, 12.6), (11.5, D + 7.5, 14.5))
    s.cam(345, (26.5, D + 0.9, 12.8), (11.5, D + 8.0, 14.5))
    s.geluid(224, "ONTBRAND", 1.0, 0.8)
    s.geluid(252, "ONTBRAND", 1.0, 1.0)
    s.deeltje(226, "FLAME", (11.5, D + 9.5, 14.5), 50, 1.4)
    s.deeltje(254, "FLAME", (11.5, D + 8.0, 14.5), 70, 2.0)
    s.deeltje(254, "LAVA", (11.5, D + 6.0, 14.5), 24, 1.6)
    s.deeltje(280, "LARGE_SMOKE", (11.5, D + 9.0, 14.5), 60, 2.4)
    s.geluid(296, "BRUL", 1.0, 1.0)
    s.schud(298, 2.6, 46)
    s.flits(298, 6, 0.35)
    s.deeltje(300, "FLAME", (13.0, D + 8.5, 14.5), 120, 3.0)
    s.deeltje(300, "LAVA", (13.0, D + 4.0, 14.5), 40, 2.6)
    s.zeg(214, "", "barbecuerog", 76, "Een Barbecuerog. Een demon van houtskool en vuur uit de oude wereld.")
    # Guhdalf knows what it is
    s.cam(350, (43.6, D + 1.3, 12.9), (40.5, D + 1.3, 14.5), knip=True)
    s.cam(418, (44.0, D + 1.4, 13.2), (40.5, D + 1.3, 14.5))
    s.zeg(352, "guhdalf", "vijand", 66, "Dit is een vijand die jullie niet aankunnen. Naar de brug! Ren!")
    s.kijk("guhdalf", 372, (60.0, D, 14.5))
    s.anim("guhdalf", 374, "wijs")

    # ---- C. the flight (420 - 705): he walks; they run, over stone that falls away under them ----
    s.anim("rog", 420, "loop")
    s.loop("rog", 420, 690, RAND)
    for t in range(432, 690, 24):
        s.geluid(t, "STAP", 0.9, 0.7)
        s.schud(t, 0.9, 6)
    aankomst = {}
    # (they leave one after the other and all of them are over the bridge before the demon leaps; Guhdalf lets the last one
    # pass him and comes last: nobody runs through anybody, nobody crosses a camera that looks at the demon)
    for i, (naam, soort, arg, start, toon, einde) in enumerate(gezelschap):
        t0 = 396 + i * 9
        aankomst[naam] = s.ren(naam, t0, VLUCHT + [einde], SNEL)
        s.kijk(naam, aankomst[naam] + 2, STAAT)
    t_g = s.ren("guhdalf", 396 + (len(gezelschap) - 1) * 9 + 34, VLUCHT[:-1] + [STAAT], SNEL)
    assert max(aankomst.values()) < 700 and t_g < 700, (aankomst, t_g)
    s.kijk("guhdalf", t_g + 2, HOOFD)
    # high over the chasm: little figures hopping from pillar to pillar, the fire coming down the nave behind them
    s.cam(420, (56.0, D + 11.5, 4.6), (52.0, D + 0.5, 13.0), knip=True)
    s.cam(520, (60.0, D + 10.5, 4.8), (57.0, D + 0.5, 14.0))
    s.zeg(430, "araguh", "brug", 56, "Over het Brokkelpad! Blijf lopen, het valt weg onder je pootjes!")
    # on the path, low, looking back at what follows
    s.cam(524, (59.5, D + 1.2, 21.4), (46.0, D + 5.0, 14.5), knip=True)
    s.cam(600, (60.5, D + 1.3, 21.6), (44.0, D + 6.5, 14.5))
    s.zeg(532, "pippguh", "omkijken", 54, "Niet omkijken, niet omkijken... IK KIJK OM! NJEG!")
    # from the east bank: they come over the bridge one by one; the last one stops
    # (the camera stands at the lip of the bank, WEST of where they gather and over their heads: nobody ends up in front of
    # the lens, whoever passes it passes under it)
    s.cam(604, (78.6, D + 2.4, 18.8), (68.0, D + 1.0, 14.2), knip=True)
    s.cam(700, (78.0, D + 2.4, 18.4), (69.5, D + 1.3, 14.4))
    s.zeg(612, "gimguh", "gooien", 52, "Niemand gooit een dwerg-guh! ...Vooruit, één keertje dan.")
    s.zeg(672, "araguh", "guhdalf", 34, "Guhdalf! Kom!")

    # ---- D. the bridge (705 - 1075): the leap, the two of them face to face ----
    s.anim("rog", 700, "brul")                        # the wings are open at 0.95 s: then he jumps
    s.geluid(702, "BRUL", 1.0, 0.9)
    s.loop("rog", 722, 792, HOOFD)
    # (low over the chasm beside the span, west of where Guhdalf stands: nothing between the lens and what comes)
    s.cam(705, (71.5, D + 0.4, 17.0), (54.0, D + 7.0, 14.5), knip=True)
    s.cam(792, (71.8, D + 0.45, 17.1), (63.5, D + 6.0, 14.5))
    s.cam(812, (71.8, D + 0.45, 17.1), (63.5, D + 6.5, 14.5))
    s.schud(792, 3.2, 34)
    s.geluid(792, "STAP", 1.0, 0.5)
    s.deeltje(792, "LARGE_SMOKE", (63.5, D + 0.4, 14.5), 90, 2.6)
    s.deeltje(792, "LAVA", (63.5, D + 0.4, 14.5), 36, 2.2)
    s.anim("rog", 800, "")
    # THE shot: side on, the whole span: a guh of one block, a demon of ten. From down in the chasm, looking up: the wall is
    # only eleven blocks from the bridge, and from deck height the picture cut him off at the chest
    s.cam(816, (68.5, D - 3.0, 3.9), (67.8, D + 5.0, 14.5), knip=True)
    s.cam(930, (68.9, D - 2.4, 4.4), (68.0, D + 4.8, 14.5))
    s.anim("rog", 824, "zwaard")
    s.geluid(838, "ONTBRAND", 1.0, 0.7)
    s.zeg(832, "guhdalf", "dienaar", 92, "Ik ben een dienaar van het Geheime Vuur, hoeder van de vlam van de Grote Knabbel!")
    s.anim("guhdalf", 836, "praat")
    # low on Guhdalf, the staff
    s.cam(934, (71.2, D + 0.8, 15.7), (73.5, D + 1.45, 14.5), knip=True)
    s.cam(1000, (70.9, D + 0.8, 15.5), (73.5, D + 1.5, 14.5))
    s.zeg(940, "guhdalf", "schaduw", 58, "Ga terug naar de schaduw, vlam van de Barbecue!")
    # he answers: one step onto the bridge, the blade of fire comes down; a wall of white light throws it back
    # (over his hat, from the south side: the hat in the lower right corner, all of the demon above it)
    s.cam(1004, (76.2, D + 2.6, 16.6), (66.0, D + 5.2, 14.0), knip=True)
    s.cam(1070, (76.5, D + 2.7, 16.7), (66.3, D + 5.0, 14.0))
    s.loop("rog", 1004, 1030, (66.5, D, 14.5))
    s.geluid(1006, "STAP", 1.0, 0.6)
    s.schud(1006, 1.4, 8)
    s.anim("rog", 1008, "zwaard")
    s.anim("guhdalf", 1016, "toover")
    s.flits(1029, 9, 0.75)
    s.geluid(1029, "STAF", 1.0, 1.2)
    s.schud(1029, 2.2, 16)
    # (a burst of light where the blade meets his shield, sparks flying off it: no slow white dots that hang in the air)
    for dt in (0, 2, 5):
        s.deeltje(1029 + dt, "FLITS", (71.6, D + 3.0, 14.5), 2, 0.5)
    s.deeltje(1029, "ELECTRIC_SPARK", (71.6, D + 3.0, 14.5), 70, 1.1)
    s.deeltje(1031, "FLAME", (70.5, D + 3.5, 14.5), 60, 1.4)

    # ---- E. YOU.. SHALL.. NOT.. VADS! (1075 - 1250): word by word, a beat on each ----
    # low behind his shoulder, from the north side: the pointed hat and the raised staff in the corner of the picture and
    # over them, filling it, the demon (not a close-up from under his chin with nothing behind him)
    s.cam(1075, (76.6, D + 0.35, 12.6), (70.5, D + 4.2, 15.2), knip=True)
    s.cam(1166, (76.2, D + 0.35, 12.4), (70.3, D + 4.4, 15.2))
    s.anim("guhdalf", 1078, "toover")
    for i, t in enumerate((1086, 1114, 1142)):
        s.zeg(t, "guhdalf", f"you_{i + 1}", 28, " ".join(ZIN_YOU.split(" ")[:i + 1]))
        s.geluid(t, "STAF", 0.9, 0.7 + i * 0.08)
        s.schud(t, 0.9, 6)
    s.zeg(1170, "guhdalf", "you_4", 76, ZIN_YOU)
    # the staff comes down: light, thunder, a crack runs through the span
    # (from the bank, higher and off the line of the bridge: his hat does not stand in front of the demon)
    s.cam(1170, (78.0, D + 2.4, 16.4), (66.5, D + 5.5, 14.8), knip=True)
    s.cam(1250, (77.6, D + 2.4, 16.3), (66.5, D + 6.0, 14.8))
    s.anim("guhdalf", 1168, "toover")
    s.flits(1172, 18, 1.0)
    s.geluid(1172, "DONDER", 1.0, 0.9)
    s.geluid(1172, "STAF", 1.0, 0.5)
    s.schud(1172, 3.6, 44)
    for dt in (0, 2, 4, 7):
        s.deeltje(1172 + dt, "FLITS", (72.6, D + 0.6, 14.5), 3, 0.7)
    s.deeltje(1172, "ELECTRIC_SPARK", (72.6, D + 0.5, 14.5), 120, 1.5)
    s.deeltje(1178, "ELECTRIC_SPARK", (70.0, D + 0.3, 14.5), 60, 2.0)       # the crack runs west through the span
    s.deeltje(1178, "FLITS", (70.0, D + 0.3, 14.5), 1, 0.4)
    # he laughs at it: a roar, another step
    s.anim("rog", 1236, "brul")
    s.geluid(1238, "BRUL", 1.0, 1.1)
    s.schud(1256, 1.6, 30)

    # ---- F. the bridge breaks (1250 - 1430) ----
    # from high in the north-east corner of the chasm: the fellowship on the bank below, Guhdalf, the demon, the whole span
    # (from the wall straight across he was too near to fit), then down after him
    s.cam(1254, (78.5, D + 7.0, 4.5), (69.0, D + 5.0, 14.5), knip=True)
    s.cam(1296, (78.5, D + 6.8, 4.6), (69.0, D + 4.0, 14.5))
    s.cam(1384, (78.2, D + 5.0, 4.8), (69.0, D - 8.0, 14.5))
    # nobody stands still for this: they flinch at his roar, the stone goes, he falls - and they cheer
    for naam, t in (("merrie", 1240), ("pippguh", 1244), ("boromika", 1248)):
        s.anim(naam, t, "schrik")
    s.anim("araguh", BREEKT + 4, "wijs")
    s.anim("guhdalf", BREEKT + 10, "buig")                    # he leans over the edge to see him go
    for naam, t in (("sam", BREEKT + 22), ("gimguh", BREEKT + 26), ("pippguh", BREEKT + 30), ("merrie", BREEKT + 34), ("leguhlas", BREEKT + 40)):
        s.anim(naam, t, "juich")
    s.anim("boromika", BREEKT + 36, "knik")
    s.loop("rog", 1270, 1298, (69.0, D, 14.5))
    s.geluid(1272, "STAP", 1.0, 0.6)
    s.schud(1272, 1.2, 8)
    s.geluid(BREEKT, "BREUK", 1.0, 0.8)
    s.schud(BREEKT, 3.4, 40)
    s.flits(BREEKT, 4, 0.3)
    s.deeltje(BREEKT, "LARGE_SMOKE", (69.0, D - 0.5, 14.5), 120, 2.4)
    s.deeltje(BREEKT, "CAMPFIRE_COSY_SMOKE", (69.0, D - 1.0, 14.5), 40, 2.0)
    s.deeltje(BREEKT + 2, "LAVA", (69.0, D - 1.0, 14.5), 50, 2.4)
    s.anim("rog", BREEKT + 2, "wankel")
    s.loop("rog", BREEKT + 4, 1392, (69.0, -10.0, 14.5))
    s.geluid(1318, "BRUL", 1.0, 0.7)
    s.geluid(1380, "BRUL", 0.5, 0.55)
    s.deeltje(1388, "FLAME", (69.0, 3.5, 14.5), 160, 3.0)
    s.deeltje(1390, "LAVA", (69.0, 3.5, 14.5), 60, 3.0)
    s.schud(1390, 1.8, 20)
    # that was that
    s.cam(1396, (76.3, D + 1.4, 15.6), (73.5, D + 1.3, 14.5), knip=True)
    s.cam(1430, (76.5, D + 1.4, 15.7), (73.5, D + 1.3, 14.5))
    s.anim("guhdalf", 1394, "")
    s.kijk("guhdalf", 1398, (80.0, D, 14.5))
    s.zeg(1400, "guhdalf", "zo", 32, "Zo. Dat was dat. Njeg.")

    # ---- G. the whip (1434 - 1660) ----
    s.geluid(1434, "ZWEEP", 1.0, 1.0)
    for i in range(6):
        s.deeltje(1434 + i, "FLAME", (71.0 + i * 0.45, 4.0 + i * 1.4, 14.5), 14, 0.25)
    s.deeltje(1440, "LAVA", (73.2, D + 0.3, 14.5), 14, 0.4)
    s.schud(1438, 1.2, 8)
    s.anim("guhdalf", 1438, "val")
    s.loop("guhdalf", 1438, 1452, (72.55, D - 0.75, 14.5))
    s.kijk("guhdalf", 1454, (80.0, D, 14.5))
    s.cam(1436, (77.4, D + 0.55, 15.9), (72.9, D + 0.3, 14.5), knip=True)
    s.cam(1548, (76.6, D + 0.5, 15.6), (72.8, D + 0.2, 14.5))
    for naam in ("araguh", "speler", "sam", "gimguh", "pippguh", "merrie", "leguhlas", "boromika"):
        s.anim(naam, 1444, "schrik")                          # (the cheering stops dead)
    s.zeg(1446, "araguh", "nee", 26, "Guhdalf!")
    s.zeg(1484, "guhdalf", "vlucht", 62, "Vlucht, dwazen... njeg.")
    # he lets go
    s.loop("guhdalf", 1550, 1612, (72.3, -6.0, 14.5))
    s.cam(1552, (72.8, D + 2.2, 16.7), (72.3, 3.0, 14.6), knip=True)
    s.cam(1640, (72.9, D + 1.9, 16.9), (72.3, 2.0, 14.6))
    s.geluid(1554, "VAL", 0.9, 0.8)
    s.zeg(1600, "", "weg", 54, "En weg was hij. Met punthoed en al.")

    # ---- H. out (1660 - 1770) ----
    s.cam(1660, (85.2, D + 2.2, 20.0), (80.5, D + 1.0, 14.5), knip=True)
    s.cam(1760, (84.6, D + 2.0, 19.4), (80.5, D + 1.0, 14.5))
    s.kijk("araguh", 1660, (83.0, D, 25.0))
    s.anim("araguh", 1666, "wijs")
    s.anim("pippguh", 1662, "huil")
    s.anim("merrie", 1664, "huil")
    s.anim("gimguh", 1668, "huil")
    s.zeg(1668, "araguh", "verder", 70, "Kom. Hij zou willen dat we doorliepen. De lange trap op, naar buiten!")
    s.donker(1754, 1770)
    return s


def alle():
    return [emmer(), brug()]
