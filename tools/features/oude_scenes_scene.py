"""
bbq2 (oude-scenes) - the six camera scenes of the older stories (DESIGN_VERHALENPAD B), written ONCE, here, in the coordinates of
the story's own template (the .nbt in data/guhs/structure):

  balto     the Wolvenrots of Nomguh: the storm closes in, the white wolf-guh is there, the three of them howl
  mewtwo    the koepelhal of the kloon-eiland, that stormy night of day 45: the tank cracks, two purple eyes, he floats up
  ohana     the fire pit on the beach of the paalhuisje, at night: 626-guh alone with the picture book, Lilo-guh, the hug, the stars
  hemel     the Hemelkapelletje: the clouds part, a beam of light through the heart window, the Knuffelhart starts to beat
  grill     the big barbecueput: the flame runs through the grillkool frame, the Grillguh cheers, and far away somebody watches
  timmer    the bouwplaats: the last tuft of roof, the flag, and the first little guh trots in through the front door

oude_scenes_java.py turns these scripts into feature/oudescenes/Scenes.java (the Cutscene builders of the verhaal engine plus
what only this slice's own client draws: weather, lightning, streams of particles); oude_scenes.py writes their texts and
checks every camera and every actor against the template (a camera in a wall, an actor in the air); oude_scenes_beeld.py draws
frames. So what a scene says and where its building is can't drift apart.

Every staging here is our own: our characters, our buildings, our words (the one line that the ohana story already had is
used again, as the design asks). Sounds are sound effects only (the mod's own and vanilla's): no music.

Camera shake is calm everywhere (the engine's swing goes with the SQUARE of the strength): nothing above 0.6.
"""

# how far under the anchor an actor waits that is not in the picture yet
WEG = 40.0


class Acteur:
    def __init__(self, naam, soort, arg, start, yaw, kleur, maat=1.0, vorm="guh", gloeit=False, nbt=None):
        self.naam, self.soort, self.arg, self.start, self.yaw = naam, soort, arg, tuple(float(v) for v in start), float(yaw)
        self.kleur, self.maat, self.vorm, self.gloeit, self.nbt = kleur, maat, vorm, gloeit, nbt


class Scene:
    KANALEN = ("zicht", "sneeuw", "regen", "donder", "nacht")

    def __init__(self, kort, titel, lijn, template, structuur, stuk, anker, duur, verberg, grond=None, lucht=(140, 190, 240)):
        """kort: the short name (the scene is oudescenes_<kort>); lijn: the questline of the Guhdex tab Verhalen that gets the
        replay row; template / structuur / stuk: the template file, the structure id and the jigsaw piece it is (None: the
        start piece); anker: the template block the scene is anchored on (the block somebody stands IN); grond: see Wereld."""
        self.kort, self.id, self.titel, self.lijn = kort, f"oudescenes_{kort}", titel, lijn
        self.template, self.structuur, self.stuk, self.anker = template, structuur, stuk, tuple(anker)
        self.duur, self.verberg, self.grond, self.lucht = duur, verberg, grond, lucht
        self.acteurs = []
        self.camera = []         # (t, pos, kijk or None, volgt or None, knip)
        self.lopen = []          # (acteur, t0, t1, naar)
        self.kijken = []         # (acteur, t, naar)
        self.animaties = []      # (acteur, t, naam)
        self.zinnen = []         # (t, spreker, key, ticks)
        self.teksten = {}        # key -> Dutch
        self.namen = {}          # acteur -> the name under the bars
        self.geluiden = []       # (t, sound, volume, pitch)       sound: a key of oude_scenes_java.GELUID
        self.deeltjes = []       # (t, particle, pos, aantal, spreiding)       vanilla particles only (the engine's bursts)
        self.schudden = []       # (t, kracht, ticks)
        self.zwart = []          # (t0, t1)
        # what this slice's own client does (client.OudeScenesClient), for this viewer only:
        self.weer = []           # (t, {kanaal: value}): keyframes, linear in between
        self.flitsen = []        # t: lightning (the sky flashes)
        self.stromen = []        # (t0, t1, particle, van, naar, per_tick, spreiding, snelheid): an emitter that moves from van to naar
        self.momenten = {}       # NAME -> tick: moments the server side acts on (Scenes.<KORT>_<NAME>)
        self.plekken = {}        # NAME -> template block: spots the server side needs (Scenes.<KORT>_<NAME>)
        self.als = {}            # (pictures only) {part of a block name: colour}: drawn as a solid block of that colour
        self.vliegt = set()      # the actors that don't stand on anything (the check leaves them alone)

    # --- building -----------------------------------------------------------------------------------------------------------
    def weg(self, x, z):
        """A spot out of sight (deep under the anchor) above/below (x, z)."""
        return (x, self.anker[1] - WEG, z)

    def speler(self, start, yaw):
        self.acteurs.append(Acteur("speler", "speler", None, start, yaw, (120, 190, 255)))

    def npc(self, naam, kind, start, yaw, toon=None, kleur=(255, 150, 200), maat=1.0, gloeit=False):
        self.acteurs.append(Acteur(naam, "npc", kind, start, yaw, kleur, maat, gloeit=gloeit))
        if toon:
            self.namen[naam] = toon

    def guh(self, naam, variant, start, yaw, toon=None, kleur=(255, 150, 200), maat=1.0, nbt=None):
        self.acteurs.append(Acteur(naam, "guh", variant, start, yaw, kleur, maat, nbt=nbt))
        if toon:
            self.namen[naam] = toon

    def wezen(self, naam, veld, start, yaw, toon=None, kleur=(255, 150, 200), maat=1.0, vorm="guh", nbt=None, gloeit=False):
        """Any other entity type: veld = a Java expression that gives a Supplier of its EntityType; nbt = the name of a static
        method of feature/oudescenes/OudeScenes that fills its data (Consumer of CompoundTag)."""
        self.acteurs.append(Acteur(naam, "wezen", veld, start, yaw, kleur, maat, vorm, gloeit, nbt))
        if toon:
            self.namen[naam] = toon

    def cam(self, t, pos, kijk, knip=False):
        self.camera.append((t, tuple(pos), tuple(kijk), None, knip))

    def camv(self, t, pos, acteur, knip=False):
        self.camera.append((t, tuple(pos), None, acteur, knip))

    def loop(self, acteur, t0, t1, naar):
        self.lopen.append((acteur, t0, t1, tuple(naar)))

    def verschijn(self, acteur, t, pos):
        """The actor is at pos from tick t on (it waited out of sight)."""
        self.lopen.append((acteur, t - 1, t, tuple(pos)))

    def kijk(self, acteur, t, naar):
        self.kijken.append((acteur, t, tuple(naar)))

    def anim(self, acteur, t, naam):
        self.animaties.append((acteur, t, naam))

    def zeg(self, t, spreker, key, ticks, tekst):
        self.zinnen.append((t, spreker, key, ticks))
        self.teksten[key] = tekst

    def geluid(self, t, sound, volume=1.0, pitch=1.0):
        self.geluiden.append((t, sound, volume, pitch))

    def deeltje(self, t, soort, pos, aantal, spreiding):
        self.deeltjes.append((t, soort, tuple(pos), aantal, spreiding))

    def schud(self, t, kracht, ticks):
        assert kracht <= 0.6, "calm shake only"
        self.schudden.append((t, kracht, ticks))

    def donker(self, t0, t1):
        self.zwart.append((t0, t1))

    def weer_op_tick(self, t, **kanalen):
        self.weer.append((t, kanalen))

    def flits(self, t):
        self.flitsen.append(t)

    def stroom(self, t0, t1, soort, van, naar=None, per_tick=1.0, spreiding=0.0, snelheid=(0.0, 0.0, 0.0)):
        self.stromen.append((t0, t1, soort, tuple(van), tuple(naar if naar is not None else van), float(per_tick), float(spreiding), tuple(snelheid)))

    def stoot(self, t, soort, pos, aantal, spreiding, snelheid=(0.0, 0.02, 0.0)):
        """A burst of one of the mod's own particles (the engine's bursts are for vanilla particles)."""
        self.stroom(t, t + 1, soort, pos, pos, aantal, spreiding, snelheid)

    # --- the script, evaluated (the same rules as feature/verhaal/Cutscene) ------------------------------------------------------
    def acteur(self, naam):
        for a in self.acteurs:
            if a.naam == naam:
                return a
        raise KeyError(naam)

    def plek(self, acteur, t):
        pos = self.acteur(acteur).start
        for a, t0, t1, naar in sorted((l for l in self.lopen if l[0] == acteur), key=lambda l: l[1]):
            if t < t0:
                continue
            if t >= t1:
                pos = naar
            else:
                f = (t - t0) / (t1 - t0)
                return tuple(pos[i] + (naar[i] - pos[i]) * f for i in range(3))
        return pos

    def loopt(self, acteur, t):
        return any(a == acteur and t0 <= t < t1 for a, t0, t1, naar in self.lopen)

    def zichtbaar(self, acteur, t):
        return self.plek(acteur, t)[1] > self.anker[1] - WEG / 2

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

    def weer_rijen(self):
        """The weather keyframes as full rows (t, zicht, sneeuw, regen, donder, nacht): a channel keeps its last value."""
        rijen, nu = [], {k: 0.0 for k in self.KANALEN}
        for t, kanalen in sorted(self.weer, key=lambda w: w[0]):
            nu = dict(nu)
            nu.update(kanalen)
            rijen.append((t,) + tuple(float(nu[k]) for k in self.KANALEN))
        return rijen

    def weer_op(self, t):
        """What the frames draw at tick t: the channels, eased like the client does, plus the lightning of that moment."""
        rijen = self.weer_rijen()
        uit = {k: 0.0 for k in self.KANALEN}
        if rijen:
            if t <= rijen[0][0]:
                rij = rijen[0]
            elif t >= rijen[-1][0]:
                rij = rijen[-1]
            else:
                for a, b in zip(rijen, rijen[1:]):
                    if a[0] <= t < b[0]:
                        f = (t - a[0]) / (b[0] - a[0])
                        rij = (t,) + tuple(a[i] + (b[i] - a[i]) * f for i in range(1, len(a)))
                        break
            uit = dict(zip(self.KANALEN, rij[1:]))
        uit["mist"] = 1.0 if uit["zicht"] > 0 else 0.0
        uit["flits"] = max([0.0] + [0.7 * (1 - (t - f) / 6.0) for f in self.flitsen if 0 <= t - f < 6])
        return uit

    def zwart_op(self, t):
        return max([0.0] + [max(0.0, min(1.0, (t - t0) / 8.0, (t1 + 8 - t) / 8.0)) for t0, t1 in self.zwart])

    def zin_op(self, t):
        nu = None
        for t0, spreker, key, ticks in sorted(self.zinnen):
            if t0 <= t < t0 + ticks:
                nu = (self.namen.get(spreker, spreker) + ": " if spreker else "") + self.teksten[key]
        return nu


# =====================================================================================================================
# Balto: the white wolf-guh on the Wolvenrots
# =====================================================================================================================
def balto():
    # The strip between the two hills is flat (the snow's top is y 20: feet at 21); the track runs east-west at z 12..13, the
    # Wolvenrots is the ledge north of it (x 181..187, z 5..8), its carved paw print the top (184, 24, 6..7: feet at 25).
    Y = 21.0
    s = Scene("balto", "De witte wolf-guh", "balto", "nomguh", "nomguh", None, (184, 21, 12), 420, 26)
    ROTS = (184.5, 25.0, 7.0)
    s.speler((186.0, Y, 13.6), 90)
    s.guh("balto", "BALTOGUH", (184.9, Y, 12.9), 90, "Baltoguh", kleur=(150, 140, 130))
    s.npc("wolf", "WITTE_WOLFGUH", s.weg(184.5, 7.0), 0, "De witte wolf-guh", kleur=(250, 250, 255), gloeit=True)
    s.kijk("speler", 0, (183.0, Y, 12.5))

    # ---- A (0 - 150): the storm closes in. Right behind the two of them, low: further than three blocks there is only white ----
    s.donker(-10, 4)
    s.weer_op_tick(0, zicht=16, sneeuw=0.75)
    s.weer_op_tick(70, zicht=4.2, sneeuw=1.0)
    s.weer_op_tick(150, zicht=3.6, sneeuw=1.0)
    s.cam(0, (187.3, Y + 1.5, 16.4), (185.2, Y + 0.8, 13.0))
    s.cam(150, (186.7, Y + 1.3, 15.8), (185.1, Y + 0.8, 13.0))
    s.geluid(4, "WIND", 0.9, 0.9)
    s.geluid(64, "WIND", 1.0, 0.8)
    s.zeg(12, "", "storm", 62, "De storm wordt dikker en dikker. Verder dan drie blokken zie je alleen nog maar wit.")
    # Baltoguh sniffs about: a few steps into the white (he all but disappears), and back, none the wiser
    s.loop("balto", 34, 62, (182.6, Y, 11.6))
    s.geluid(40, "SNUIF", 0.9, 1.0)
    s.loop("balto", 70, 96, (185.0, Y, 14.3))
    s.geluid(74, "SNUIF", 0.8, 1.1)
    s.kijk("balto", 98, (186.0, Y, 13.6))
    s.zeg(84, "balto", "kwijt", 60, "Snuf... snuf... alleen maar sneeuw. Ik ben de weg kwijt, njeg.")
    s.kijk("speler", 96, (185.0, Y, 14.3))

    # ---- B (150 - 262): she is there. From the snow at their feet, looking up at the ledge: the white opens round her ----
    s.verschijn("wolf", 150, ROTS)
    s.kijk("wolf", 150, (184.4, Y, 12.6))
    s.geluid(150, "KLINGEL", 1.0, 0.7)
    s.stoot(152, "WOLFGLANS", (ROTS[0], ROTS[1] + 0.9, ROTS[2]), 40, 0.6)
    s.stroom(160, 330, "WOLFGLANS", (ROTS[0], ROTS[1] + 0.8, ROTS[2]), None, 1.0, 0.7, (0.0, 0.02, 0.0))
    s.weer_op_tick(151, zicht=3.6, sneeuw=1.0)
    s.weer_op_tick(196, zicht=14, sneeuw=0.8)
    s.cam(150, (183.3, Y + 0.7, 12.3), (184.5, 25.9, 7.0), knip=True)
    s.cam(262, (183.6, Y + 0.9, 11.3), (184.5, 25.8, 7.0))
    s.kijk("balto", 156, ROTS)
    s.kijk("speler", 160, ROTS)
    s.loop("balto", 176, 204, (184.4, Y, 10.4))
    s.zeg(160, "", "wolf", 58, "En dan staat ze daar, op de Wolvenrots. Een witte wolf-guh. Ze gloeit zachtjes, als een sterretje.")
    s.zeg(222, "boris", "boris", 38, "Een gewone guh haalt dit nooit. Maar een Baltoguh...")
    s.namen["boris"] = "Boris (in je hoofd)"

    # ---- C (262 - 344): the howl. From the side, the three of them in one picture: guh, wolf-guh, you ----
    s.loop("speler", 236, 258, (185.6, Y, 10.9))
    s.kijk("speler", 259, ROTS)
    s.cam(262, (180.6, Y + 1.5, 12.6), (184.6, Y + 2.2, 9.0), knip=True)
    s.cam(344, (181.0, Y + 1.7, 13.0), (184.6, Y + 2.3, 9.0))
    s.zeg(266, "balto", "half", 40, "Half guh, half wolf... en dat is precies goed. Huil mee!")
    s.anim("balto", 304, "spring")
    s.geluid(306, "HUIL", 1.0, 1.0)
    s.anim("wolf", 314, "spring")
    s.geluid(316, "HUIL", 0.9, 1.25)
    s.anim("speler", 322, "spring")
    s.zeg(308, "speler", "huil", 36, "AUUUHOE-NJEG!")
    s.schud(306, 0.5, 26)
    s.deeltje(308, "NOTE", (184.4, Y + 1.6, 10.4), 4, 0.3)
    s.deeltje(324, "NOTE", (185.6, Y + 2.3, 10.9), 4, 0.3)
    s.stoot(316, "WOLFGLANS", (ROTS[0], ROTS[1] + 1.2, ROTS[2]), 60, 0.9)

    # ---- D (344 - 420): the storm lies down. High over the track: the red poles are back, one after the other ----
    s.weer_op_tick(330, zicht=14, sneeuw=0.8)
    s.weer_op_tick(400, zicht=64, sneeuw=0.12)
    s.cam(344, (190.4, Y + 6.2, 17.6), (184.4, Y + 1.4, 10.6), knip=True)
    s.cam(420, (192.0, Y + 7.6, 19.0), (182.0, Y + 1.0, 10.8))
    s.geluid(350, "BELLETJES", 0.8, 1.0)
    s.zeg(352, "", "klaart", 52, "De storm gaat liggen. En kijk: daar staan de paaltjes weer!")
    s.kijk("balto", 350, (174.0, Y, 13.0))
    s.loop("wolf", 372, 373, s.weg(184.5, 7.0))            # she is gone as she came
    s.stoot(372, "WOLFGLANS", (ROTS[0], ROTS[1] + 0.9, ROTS[2]), 50, 0.7)
    s.geluid(372, "KLINGEL", 0.7, 1.0)
    s.donker(406, 420)
    return s


# =====================================================================================================================
# Mewtwo: day 45
# =====================================================================================================================
def mewtwo():
    # The koepelhal: floor top y 37 (feet at 38), the kloontank 3 x 3 x 3 on its middle (39..41, 38..40, 43..45) with a lid at
    # y 41; the glass dome over it (radius 12 round (40, 44)).
    F = 38.0
    TANK = (40.5, F, 44.5)
    s = Scene("mewtwo", "Dag 45: de nacht van de knal", "mewtwo", "kloon_eiland", "kloon_eiland", None, (40, 38, 44), 440, 34)
    s.guh("guhtwo", "MEWTWO", s.weg(40.5, 44.5), 0, "Guhtwo", kleur=(200, 170, 220), maat=1.1)
    s.wezen("mew", "MewtwoFeature.MEW", s.weg(49.5, 49.5), 90, "Mieuwguh", kleur=(255, 170, 210), maat=0.6)
    s.namen["prof"] = "Professor Knabbelkloon"
    s.vliegt = {"guhtwo", "mew"}
    s.momenten["KRAK"] = 164

    # the whole night: a thunderstorm over the island, for this viewer only
    s.donker(-10, 6)
    s.weer_op_tick(0, regen=1.0, donder=1.0, nacht=1.0)
    for t in (34, 164, 300):
        s.flits(t)
        s.geluid(t + 3, "DONDER", 1.0, 0.9)

    # ---- A (0 - 112): outside, on the plateau: rain on the glass dome, lightning behind it ----
    s.cam(0, (40.5, F + 9.6, 67.0), (40.5, F + 5.0, 55.5))
    s.cam(112, (40.5, F + 8.2, 63.5), (40.5, F + 5.0, 55.5))
    s.zeg(10, "prof", "dag45", 80, "Dag 45. Het onweerde boven het kloon-eiland. En in de kloontank... borrelde het.")
    s.geluid(70, "TANK_BORREL", 0.7, 0.9)

    # ---- B (112 - 236): inside, in front of the tank: bubbles, the crack, two purple eyes ----
    s.cam(112, (40.5, F + 1.1, 50.3), (40.5, F + 1.6, 44.5), knip=True)
    s.cam(236, (40.5, F + 1.2, 48.6), (40.5, F + 1.7, 44.5))
    s.stroom(112, 300, "BUBBEL", (40.5, F + 0.6, 46.2), (40.5, F + 2.6, 46.2), 2.0, 0.5)
    s.geluid(118, "TANK_BORREL", 1.0, 1.0)
    s.geluid(146, "TANK_BORREL", 1.0, 1.2)
    s.geluid(164, "GLAS", 1.0, 0.8)
    s.geluid(166, "TANK_KLIK", 1.0, 0.7)
    s.schud(164, 0.6, 10)
    s.deeltje(165, "FLASH", (40.5, F + 1.6, 46.3), 1, 0.0)
    s.deeltje(166, "END_ROD", (40.5, F + 1.6, 46.3), 14, 0.5)
    s.zeg(168, "", "krak", 40, "KRAK! Dwars door het glas schoot een barst.")
    # the eyes: two points of purple light behind the south pane, at guh height
    for dx in (-0.22, 0.22):
        s.stroom(198, 252, "OOG", (40.5 + dx, F + 1.55, 46.06), None, 2.0, 0.0)
    s.geluid(198, "TELEKINESE", 0.8, 0.7)
    s.zeg(210, "", "ogen", 44, "En in het roze knabbelsap gingen twee paarse ogen open.")

    # ---- C (236 - 346): he floats up out of the tank ----
    s.verschijn("guhtwo", 240, (40.5, F + 1.2, 44.5))
    s.loop("guhtwo", 242, 300, (40.5, F + 4.3, 44.5))
    s.kijk("guhtwo", 240, (40.5, F, 52.0))
    s.cam(236, (37.4, F + 1.4, 49.4), (40.5, F + 2.2, 44.5), knip=True)
    s.cam(300, (37.0, F + 1.9, 49.8), (40.5, F + 5.0, 44.5))
    s.cam(346, (36.8, F + 2.2, 50.0), (40.5, F + 5.2, 44.5))
    s.stoot(242, "GLOED", (40.5, F + 3.2, 44.5), 40, 0.7)
    s.stroom(244, 300, "GLOED", (40.5, F + 1.6, 44.5), (40.5, F + 4.6, 44.5), 2.0, 0.5)
    s.geluid(242, "TANK_HEEL", 0.9, 0.7)
    s.geluid(250, "TELEKINESE", 1.0, 0.9)
    s.schud(244, 0.4, 14)
    s.zeg(272, "guhtwo", "wie", 70, "Wie ben ik... en waar zijn mijn knabbels?")

    # ---- D (346 - 440): Mieuwguh giggles past ----
    s.verschijn("mew", 346, (49.0, F + 4.6, 50.0))
    s.loop("mew", 348, 420, (32.0, F + 5.4, 40.0))
    s.cam(346, (42.6, F + 1.5, 51.2), (40.5, F + 4.6, 44.5), knip=True)
    s.cam(440, (42.0, F + 1.7, 51.6), (40.2, F + 4.8, 44.5))
    s.geluid(352, "MEW_GIECHEL", 1.0, 1.0)
    s.zeg(354, "mew", "hihi", 36, "Hihihi! Njeg-njeg!")
    s.kijk("guhtwo", 356, (46.0, F + 4, 48.0))
    s.kijk("guhtwo", 376, (40.0, F + 4, 52.0))
    s.kijk("guhtwo", 396, (33.0, F + 4, 41.0))
    s.geluid(392, "MEW_GIECHEL", 0.6, 1.2)
    s.zeg(396, "prof", "begon", 40, "En toen... toen wist ik het ineens weer. Njeg!")
    s.donker(426, 440)
    return s


# =====================================================================================================================
# Lilo & Stitch: ohana, at the fire pit on the beach
# =====================================================================================================================
def ohana():
    # The fire pit west of the stilt house: a lit campfire sunk into the sand at (10, 4, 32) with four log seats round it
    # (the sand's top is y 4: feet at 5).
    Y = 5.0
    STITCH = (11.2, Y, 33.9)
    LILO = (12.05, Y, 33.25)
    VUUR = (10.5, Y, 32.5)
    s = Scene("ohana", "Ohana, bij het kampvuurtje", "guhwaii", "guhwaii_ohana", "guhwaii_ohana", None, (11, 5, 33), 420, 40)
    s.guh("stitch", "STITCH626", STITCH, 0, "626-guh", kleur=(90, 140, 230), maat=0.9)
    s.npc("lilo", "LILO_GUH", s.weg(LILO[0], LILO[2]), 0, "Lilo-guh", kleur=(250, 120, 120), maat=0.85)
    s.wezen("boek", "() -> EntityType.ITEM_DISPLAY", (10.9, Y + 0.03, 33.3), 135, kleur=(170, 110, 60), vorm="boek", nbt="prentenboek")
    s.kijk("stitch", 0, VUUR)

    # the whole scene: night, for this viewer only
    s.donker(-10, 8)
    s.weer_op_tick(0, nacht=1.0)

    # ---- A (0 - 124): alone. From the beach in the west: a small blue guh by a small fire, the dark house behind him ----
    s.cam(0, (2.6, Y + 2.6, 30.6), (11.0, Y + 0.6, 33.2))
    s.cam(124, (5.0, Y + 1.8, 31.2), (11.0, Y + 0.6, 33.3))
    s.zeg(12, "", "nacht", 66, "Als iedereen slaapt, zit 626-guh in zijn eentje bij het kampvuurtje. Met het prentenboek.")
    s.geluid(30, "KAMPVUUR", 0.8, 1.0)
    s.geluid(84, "GUH_AMBIENT", 0.5, 0.7)
    s.zeg(86, "stitch", "alleen", 38, "626... alleen. Blub.")

    # ---- B (124 - 246): Lilo-guh sits down next to him. Close, from across the fire ----
    s.verschijn("lilo", 124, LILO)
    s.kijk("lilo", 124, STITCH)
    s.anim("lilo", 128, "spring")
    s.cam(124, (9.5, Y + 0.9, 31.5), (11.6, Y + 0.6, 33.6), knip=True)
    s.cam(246, (9.3, Y + 1.0, 31.3), (11.6, Y + 0.6, 33.6))
    s.geluid(128, "GUH_AMBIENT", 0.8, 1.35)
    s.zeg(132, "lilo", "erbij", 34, "Schuif eens op, vadsje. Ik kom erbij zitten.")
    s.kijk("stitch", 138, LILO)
    s.kijk("lilo", 170, VUUR)
    s.zeg(172, "lilo", "citaat", 74, "Ohana betekent familie. Familie betekent dat niemand wordt achtergelaten... of vergeten. Njeg.")

    # ---- C (246 - 330): the hug ----
    s.cam(246, (10.9, Y + 1.3, 31.5), (11.65, Y + 0.6, 33.6), knip=True)
    s.cam(330, (10.7, Y + 1.5, 31.2), (11.65, Y + 0.6, 33.6))
    s.kijk("lilo", 248, STITCH)
    s.loop("stitch", 254, 270, (11.62, Y, 33.56))
    s.geluid(270, "GUH_HAPPY", 0.9, 1.1)
    s.stoot(272, "HARTJE", (11.85, Y + 1.2, 33.4), 10, 0.35)
    s.stroom(280, 326, "HARTJE", (11.85, Y + 1.25, 33.4), None, 0.25, 0.3)
    s.anim("lilo", 276, "spring")
    s.zeg(276, "stitch", "ohana", 46, "O... ha... na. NJEG!")

    # ---- D (330 - 420): the stars. The camera lets go of them and looks up ----
    s.cam(330, (14.6, Y + 1.5, 36.4), (11.7, Y + 0.7, 33.5), knip=True)
    s.cam(372, (14.8, Y + 1.6, 36.7), (11.0, Y + 3.0, 32.6))
    s.cam(420, (15.0, Y + 1.7, 37.0), (9.5, Y + 30.0, 32.0))
    s.geluid(366, "KLINGEL", 0.6, 1.4)
    s.zeg(350, "", "sterren", 56, "En boven Guhwai'i keken duizend sterren mee. Niemand vergeten. Niemand.")
    s.donker(406, 420)
    return s


# =====================================================================================================================
# Hemelkapelletje: the Knuffelhart starts to beat
# =====================================================================================================================
def hemel():
    # The chapel on the islet: its floor is y 45 (feet at 46), the aisle runs north along x 20 to the quartz altar; the
    # Knuffelhart stands on it at (20, 47, 9), the heart window is in the north wall behind it (z 7, x 16..24, y 47..54).
    F = 46.0
    HART = (20.5, 47.55, 9.5)
    HOEDER = (23.5, F, 11.5)
    s = Scene("hemel", "Het Knuffelhart klopt", "hemel", "hemelkapelletje", "hemelkapelletje", None, (20, 46, 10), 380, 22, grond=3)
    s.npc("hoeder", "WOLKENHOEDER", HOEDER, 0, "De wolkenhoeder", kleur=(250, 250, 255))
    s.speler((20.5, F, 13.5), 180)
    s.kijk("hoeder", 0, HART)
    s.kijk("speler", 0, HART)
    s.momenten["KLOP"] = 196

    # ---- A (0 - 104): outside, north of the islet: the clouds over the chapel part, and a beam of light comes down ----
    s.donker(-10, 6)
    s.cam(0, (27.5, 60.5, -6.5), (20.5, 55.5, 9.0))
    s.cam(104, (26.0, 59.0, -5.0), (20.5, 54.5, 9.0))
    s.zeg(10, "", "drie", 56, "Een kristal, een gouden knabbel en een veertje. Meer had het Knuffelhart niet nodig.")
    # two banks of cloud drift apart
    s.stroom(0, 40, "CLOUD", (20.5, 64.0, 2.0), None, 5.0, 2.4)
    s.stroom(30, 100, "CLOUD", (19.0, 64.0, 2.0), (11.0, 65.0, 0.0), 4.0, 1.8, (-0.03, 0.0, 0.0))
    s.stroom(30, 100, "CLOUD", (22.0, 64.0, 2.0), (30.0, 65.0, 0.0), 4.0, 1.8, (0.03, 0.0, 0.0))
    s.geluid(34, "WIND", 0.5, 1.4)
    s.zeg(70, "", "wolken", 40, "Hoog boven het kapelletje schoven de wolken open...")
    # the beam: from the gap in the clouds, slanting down through the heart window onto the heart
    BOVEN = (20.5, 64.0, -3.9)
    s.stroom(56, 104, "STRAAL", BOVEN, (20.5, 51.0, 6.6), 6.0, 0.12)
    s.stroom(100, 362, "STRAAL", BOVEN, HART, 9.0, 0.1)
    s.geluid(60, "KLINGEL", 0.8, 0.8)

    # ---- B (104 - 246): inside, in the aisle: the beam lands on the heart, chimes, and then: bonk ----
    s.cam(104, (20.5, F + 1.5, 17.5), (20.5, 48.0, 9.5), knip=True)
    s.cam(246, (20.5, F + 1.4, 15.2), (20.5, 48.0, 9.5))
    s.loop("speler", 104, 105, (19.2, F, 12.6))            # (a step aside: the aisle is the camera's)
    s.kijk("speler", 106, HART)
    for i, t in enumerate((110, 128, 146, 164)):
        s.geluid(t, "KLINGEL", 0.8, 0.9 + i * 0.12)
    s.geluid(120, "STER", 0.8, 1.0)
    s.stroom(110, 362, "STERRETJE", HART, None, 1.2, 0.45)
    s.zeg(124, "", "straal", 50, "...en door het hartjesraam viel een straal licht, precies op het hart.")
    # the beats: slow, then steady
    for i, t in enumerate((196, 226, 250, 272, 292, 312, 332, 352)):
        s.geluid(t, "HARTKLOP", 0.8 if i == 0 else 1.0, 0.9 if i == 0 else 1.0)
        s.schud(t, 0.25, 2)
    s.deeltje(198, "HEART", (20.5, 48.4, 9.5), 6, 0.4)
    s.zeg(200, "", "bonk", 44, "Bonk... bonk. Het Knuffelhart klopt weer!")

    # ---- C (246 - 380): the wolkenhoeder can't sit still ----
    s.cam(246, (21.2, F + 1.7, 13.6), (22.0, 47.4, 10.4), knip=True)
    s.cam(380, (20.6, F + 2.2, 16.6), (21.6, 47.6, 10.2))
    s.anim("hoeder", 250, "spring")
    s.anim("hoeder", 264, "spring")
    s.geluid(250, "GUH_HAPPY", 0.9, 1.25)
    s.kijk("hoeder", 270, (19.2, F, 12.6))
    s.zeg(254, "hoeder", "vahoeg", 60, "VAHOEG, het klopt! Voor jou, en voor al je guhs die in de wolkjes op je wachten.")
    s.deeltje(274, "HEART", (20.5, 48.6, 9.5), 10, 0.6)
    s.zeg(322, "", "nooit", 40, "Vanaf nu hoeft niemand ooit nog echt afscheid te nemen.")
    s.donker(366, 380)
    return s


# =====================================================================================================================
# Grillguh: the first portal burns
# =====================================================================================================================
def grill():
    # The big pit (ground top y 4: feet at 5). The long barbecue stands at x 12..24, z 19..23 (up to y 8, the sausages to 10);
    # behind it the oven wall with the grillkool frame at z 26 (x 16..19, y 5..9; the fire is x 17..18, y 6..8); the Grillguh
    # has his counter in the east (27, 5, 14); four charred guh statues stand guard, the north-west one is 11 high.
    Y = 5.0
    GUH = (27.5, Y, 14.5)
    BEELD = (7.5, 12.0, 7.5)                     # on the head of the north-west statue
    s = Scene("grill", "Hij brandt weer!", "grillguh", "barbecueput_groot", "barbecueput", "barbecueput_groot", (27, 5, 14), 360, 30, grond=4)
    s.speler((18.0, Y, 24.7), 0)
    s.npc("grillguh", "GRILLGUH", GUH, 90, "Grillguh", kleur=(250, 240, 230))
    s.npc("guhdalf", "GUHDALF", BEELD, 0, "???", kleur=(176, 178, 190), maat=1.15)
    s.kijk("speler", 0, (18.0, 7.0, 26.5))
    s.kijk("guhdalf", 0, (18.0, 7.0, 26.5))
    s.kijk("grillguh", 0, (18.0, 7.0, 26.5))
    s.momenten["AAN"] = 100
    s.plekken["FRAME"] = (17, 6, 26)             # the lowest fire block of the frame (the fire: 2 wide to +x, 3 high)

    # ---- A (0 - 112): the frame, cold and dark, seen from behind the oven wall: through it stands whoever holds the
    # Aanmaakblokje. It catches; the flame runs round the inside of the frame; WHOEF ----
    s.donker(-10, 6)
    s.cam(0, (18.0, 7.5, 31.6), (18.0, 7.5, 26.5))
    s.cam(112, (18.0, 7.4, 30.4), (18.0, 7.5, 26.5))
    s.anim("speler", 18, "zwaai")
    s.geluid(20, "VUURSTEEN", 1.0, 1.0)
    s.deeltje(22, "FLAME", (17.2, 6.1, 26.5), 6, 0.08)
    s.als = {"gebarsten_houtskoolsteen_stenen": (38, 30, 30)}
    s.zeg(8, "", "blokje", 44, "Eén Aanmaakblokje in het grillkoolframe. Tsss...")
    # (the inside of the frame: x 17..19, y 6..9 on the plane z 26.5)
    hoeken = [(17.05, 6.05), (18.95, 6.05), (18.95, 8.95), (17.05, 8.95), (17.05, 6.05)]
    t = 30
    for (x0, y0), (x1, y1) in zip(hoeken, hoeken[1:]):
        s.stroom(t, t + 17, "FLAME", (x0, y0, 26.5), (x1, y1, 26.5), 3.0, 0.05, (0.0, 0.01, 0.0))
        s.stroom(t, t + 17, "SMALL_FLAME", (x0, y0, 26.5), (x1, y1, 26.5), 2.0, 0.08)
        s.geluid(t, "VUUR", 0.7, 0.9 + (t - 30) / 100.0)
        t += 17
    s.zeg(54, "", "loopt", 40, "...en daar loopt het vlammetje, het hele frame rond!")
    # WHOEF: the fire fills the frame
    s.geluid(100, "WHOEF", 1.0, 0.8)
    s.deeltje(100, "FLAME", (18.0, 7.5, 26.3), 60, 0.6)
    s.deeltje(101, "LAVA", (18.0, 6.6, 26.2), 10, 0.4)
    s.schud(100, 0.5, 10)
    s.anim("speler", 102, "spring")

    # ---- B (112 - 226): the Grillguh, at his counter, beside himself ----
    s.cam(112, (24.2, Y + 1.5, 12.6), (27.4, Y + 0.9, 14.5), knip=True)
    s.cam(226, (24.6, Y + 1.4, 12.2), (27.4, Y + 0.9, 14.5))
    s.kijk("grillguh", 112, (18.0, 7.0, 26.5))
    for t in (116, 130, 144):
        s.anim("grillguh", t, "spring")
    s.geluid(116, "GUH_HAPPY", 1.0, 0.8)
    s.zeg(118, "grillguh", "brandt", 52, "VAHOEG, hij brandt weer!")
    s.deeltje(132, "FLAME", (27.5, Y + 1.3, 14.5), 16, 0.5)
    s.kijk("grillguh", 172, (24.2, Y, 12.6))
    s.zeg(174, "grillguh", "ruik", 48, "Ruik je dat? Kaasfrituursaus, helemaal van de andere kant! Njeg njeg njeg.")

    # ---- C (226 - 360): and far away, on the head of a charred statue, somebody with a pointed hat is watching ----
    s.cam(226, (18.5, 11.4, 24.8), (7.5, 13.0, 7.5), knip=True)
    s.cam(318, (14.0, 12.6, 16.0), (7.5, 13.1, 7.5))
    s.cam(360, (13.4, 12.8, 14.8), (7.5, 13.1, 7.5))
    s.zeg(236, "", "verte", 50, "En in de verte, boven op een verkoold guhbeeld, stond iemand met een punthoed te kijken.")
    s.zeg(296, "guhdalf", "begonnen", 44, "Hm. Het vuur brandt weer. Dan is het begonnen, njeg.")
    s.kijk("guhdalf", 336, (7.5, 12.0, -6.0))
    s.geluid(338, "MANTEL", 0.7, 0.8)
    s.donker(346, 360)
    return s


# =====================================================================================================================
# Timmerguh: the roof is on, and somebody moves in
# =====================================================================================================================
def timmer():
    # The bouwplaats (ground top y 4: feet at 5). The huisje is a guh head, x 1..11, z 11..21; its face looks east at the
    # street (z 15..17), the front door is its mouth (11, 5..6, 16); the dome's top is y 14, the ears go to 15. The Timmerguh
    # stands in the street by the gate (15, 5, 16).
    Y = 5.0
    TIMMER = (15.5, Y, 16.5)
    DEUR = (11.5, Y, 16.5)
    s = Scene("timmer", "Het dak zit erop", "timmerguh", "knuffeldal_stadje/bouwplaats", "knuffeldal_stadje", "bouwplaats", (15, 5, 16), 340, 24, grond=4)
    s.npc("timmerguh", "TIMMERGUH", TIMMER, 90, "De Timmerguh", kleur=(250, 210, 120))
    s.guh("bewoner", "NORMAL", s.weg(21.5, 16.5), 90, "Het eerste bewonertje", kleur=(255, 170, 200), maat=0.7, nbt="bewonertje")
    s.speler((13.6, Y, 14.4), 90)
    s.kijk("timmerguh", 0, (6.0, 13.0, 16.0))
    s.kijk("speler", 0, (6.0, 13.0, 16.0))
    s.momenten["DEUR_OPEN"] = 262
    s.als = {"dakplek": (240, 170, 205)}         # (the roof is whole when this plays)
    s.plekken["DEUR"] = (11, 5, 16)

    # ---- A (0 - 104): the roof. From over the street, looking up at the dome: the last tuft, the flag ----
    s.donker(-10, 6)
    s.cam(0, (19.0, 10.5, 22.5), (6.5, 13.4, 16.2))
    s.cam(104, (17.6, 11.4, 21.4), (6.5, 14.0, 16.2))
    for i, t in enumerate((10, 22, 34)):
        s.geluid(t, "HAMER", 0.9, 1.0 + i * 0.1)
        s.deeltje(t + 1, "HAPPY_VILLAGER", (6.5, 14.6, 16.5), 6, 0.5)
    s.zeg(8, "", "tok", 46, "Tok, tok, TOK! Het allerlaatste dakpluisje zit erop.")
    s.geluid(52, "VUURWERK", 0.8, 1.0)
    s.deeltje(52, "FIREWORK", (6.5, 17.2, 16.5), 40, 0.9)
    s.zeg(60, "", "vlag", 38, "En daar wappert hij: de vlag in top!")

    # ---- B (104 - 204): the Timmerguh ----
    s.cam(104, (17.6, Y + 1.3, 14.4), (15.5, Y + 0.9, 16.5), knip=True)
    s.cam(204, (17.3, Y + 1.3, 14.7), (15.5, Y + 0.9, 16.5))
    s.kijk("timmerguh", 104, (17.6, Y, 14.4))
    s.anim("timmerguh", 108, "spring")
    s.geluid(108, "GUH_HAPPY", 1.0, 0.9)
    s.zeg(110, "timmerguh", "dak", 50, "VAHOEG! Met een dak erop is het pas echt een huisje.")
    s.zeg(164, "timmerguh", "wie", 38, "Nu nog iemand die erin wil wonen... Njeg?")

    # ---- C (204 - 340): the first one to move in ----
    # (from the gate, straight down the street at the face of the huisje: the Timmerguh has stepped out of the way)
    s.loop("timmerguh", 203, 205, (15.6, Y, 18.3))
    s.verschijn("bewoner", 206, (22.4, Y, 16.5))
    s.loop("bewoner", 208, 262, (12.5, Y, 16.5))
    s.loop("bewoner", 266, 286, (10.3, Y, 16.5))
    s.kijk("bewoner", 288, (16.0, Y, 16.5))
    s.cam(204, (19.8, Y + 1.6, 16.9), (11.5, Y + 1.2, 16.5), knip=True)
    s.cam(262, (18.6, Y + 1.5, 16.8), (11.5, Y + 1.1, 16.5))
    s.cam(340, (17.3, Y + 1.3, 16.7), (11.0, Y + 0.9, 16.5))
    s.kijk("timmerguh", 206, (21.0, Y, 16.5))
    s.kijk("timmerguh", 236, (16.0, Y, 16.5))
    s.kijk("timmerguh", 262, DEUR)
    s.kijk("speler", 206, (21.0, Y, 16.5))
    s.kijk("speler", 240, (15.0, Y, 16.5))
    s.kijk("speler", 262, DEUR)
    s.geluid(210, "GUH_AMBIENT", 0.8, 1.5)
    s.zeg(216, "", "trippel", 44, "En wie komt daar aangetrippeld, met een heel klein koffertje vol knabbels?")
    s.geluid(262, "DEUR_OPEN", 0.9, 1.0)
    s.anim("bewoner", 294, "spring")
    s.geluid(294, "GUH_HAPPY", 0.9, 1.5)
    s.deeltje(296, "HEART", (10.6, Y + 1.2, 16.5), 8, 0.4)
    s.zeg(284, "", "knus", 44, "Het eerste bewonertje. Klein huisje, kleine guh. Knus!")
    s.donker(326, 340)
    return s


def alle():
    return [balto(), mewtwo(), ohana(), hemel(), grill(), timmer()]
