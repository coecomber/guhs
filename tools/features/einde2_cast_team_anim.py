"""
einde2, slice cast_team - the animations of its characters (CONTRACT_E2 11.2 E): one file per CHARACTER, shared by its
looks: `idle` (a 12 second loop with the character's first signature move woven in once), `praat` (24 ticks) and the
named moves of CONTRACT_E2 11.4. A pose that lasts ("houding") is a slow loop. A prop that only shows during a move is a
bone scaled to 0 in every other animation (PROPS).

Signs (GeckoLib, model space): a negative x turn lifts what points forward (a paw, a chin); a positive y turn swings what
points forward towards the character's right; positions are model units.
"""
from features.einde2_cast_team_kit import T, anim, weef

# the prop bones of a character that are hidden unless an animation shows them
PROPS = {
    "aang_guh": ("aang_bal", "aang_vleugels"),
    "katara_guh": ("katara_water", "katara_gloed"),
    "sokka_guh": ("sokka_vlucht",),
    "toph_guh": ("toph_zuil",),
    "zuka": ("zuka_vlam",),
    "oom_miroh": ("miroh_tegel",),
}
# the poses that last (a loop; CastTeam.duur = 0)
HOUDINGEN = {("aang_guh", "zweef"), ("katara_guh", "armen"), ("suki_guh", "wacht"), ("yue_guh", "maan")}
# the names per character, in the order of CONTRACT_E2 11.4 (the first is woven into idle), after "praat"
NAMEN = {
    "aang_guh": ("luchtscooter", "zweef", "lach"),
    "katara_guh": ("waterzweep", "genees", "armen"),
    "sokka_guh": ("boemerang", "plan", "honger"),
    "toph_guh": ("stamp", "wijs", "lach"),
    "zuka": ("mijn_eer", "zucht", "vuur", "buig"),
    "oom_miroh": ("thee", "lach", "wijsheid", "pai_sho"),
    "suki_guh": ("waaier", "groet", "wacht"),
    "yue_guh": ("buig", "maan"),
    "veerguh": ("kaartje",),
    "scheepsbouwerguh": ("timmer",),
    "kroegbaasguh": ("poets",),
    "kapiteinguh": ("verhaal",),
    "ringenraceguh": ("vlag",),
}
MIKAS = ("zuka", "oom_miroh")


def _praat_zit():
    return {"head": {"rotation": T((0, 0, 0, 0), (0.2, -8, 0, 4), (0.45, 4, 0, -3), (0.7, -6, 0, 3), (0.95, 3, 0, 0), (1.2, 0, 0, 0))},
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.25, -45, -10, 0), (0.6, -20, 0, 0), (0.9, -40, -10, 0), (1.2, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.4, -35, 10, 0), (0.75, -10, 0, 0), (1.2, 0, 0, 0))},
            "body": {"position": T((0, 0, 0, 0), (0.3, 0, 0.5, 0), (0.6, 0, 0, 0), (0.9, 0, 0.4, 0), (1.2, 0, 0, 0))}}


def _praat_mika():
    return {"head": {"rotation": T((0, 0, 0, 0), (0.2, -9, 0, 4), (0.45, 3, 0, -4), (0.7, -7, 0, 3), (0.95, 2, 0, 0), (1.2, 0, 0, 0))},
            "tail": {"rotation": T((0, 0, -16, 0), (0.3, 0, 24, 0), (0.6, 0, -24, 0), (0.9, 0, 24, 0), (1.2, 0, -16, 0))},
            "leg_front_right": {"rotation": T((0, 0, 0, 0), (0.3, -40, 0, 0), (0.6, -15, 0, 0), (0.9, -35, 0, 0), (1.2, 0, 0, 0))},
            "body": {"position": T((0, 0, 0, 0), (0.3, 0, 0.5, 0), (0.6, 0, 0, 0), (0.9, 0, 0.4, 0), (1.2, 0, 0, 0))}}


def _lach_zit(lengte=1.6):
    return {"body": {"position": T((0, 0, 0, 0), (0.15, 0, 1.2, 0), (0.3, 0, 0, 0), (0.45, 0, 1.2, 0), (0.6, 0, 0, 0), (0.75, 0, 1.0, 0), (0.9, 0, 0, 0),
                                   (1.05, 0, 0.8, 0), (1.2, 0, 0, 0), (lengte, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.2, -14, 0, 0), (0.6, -16, 0, 5), (1.0, -14, 0, -5), (1.3, -6, 0, 0), (lengte, 0, 0, 0))},
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.2, -50, -20, 0), (0.5, -30, -20, 0), (0.8, -50, -20, 0), (1.1, -30, -20, 0), (lengte, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.2, -50, 20, 0), (0.5, -30, 20, 0), (0.8, -50, 20, 0), (1.1, -30, 20, 0), (lengte, 0, 0, 0))},
            "ear_left": {"rotation": T((0, 0, 0, 0), (0.3, 0, 0, -16), (0.6, 0, 0, 0), (0.9, 0, 0, -16), (1.2, 0, 0, 0))},
            "ear_right": {"rotation": T((0, 0, 0, 0), (0.3, 0, 0, 16), (0.6, 0, 0, 0), (0.9, 0, 0, 16), (1.2, 0, 0, 0))}}


# =====================================================================================================================
# the moves, per character: name -> (length in seconds, bones)
# =====================================================================================================================
def _aang():
    draai = [(0.3 + i * 0.3, 0, i * 120.0, 0) for i in range(9)]
    return {
        "luchtscooter": (3.0, {
            "aang_bal": {"scale": T((0, 0), (0.15, 0), (0.4, 1.05), (2.6, 1.0), (2.85, 0), (3.0, 0)),
                         "rotation": T((0, 0, 0, 0), *draai, (3.0, 0, 1080, 0))},
            "body": {"position": T((0, 0, 0, 0), (0.15, 0, 0, 0), (0.45, 0, 9.5, 0), (1.0, 0, 9.0, 0), (1.5, 0, 9.6, 0), (2.0, 0, 9.0, 0), (2.55, 0, 9.4, 0), (2.85, 0, 0, 0), (3.0, 0, 0, 0)),
                     "rotation": T((0, 0, 0, 0), (0.6, 0, 0, 7), (1.2, 0, 0, -7), (1.8, 0, 0, 7), (2.4, 0, 0, -6), (2.85, 0, 0, 0), (3.0, 0, 0, 0))},
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.45, -60, -55, 0), (2.5, -60, -55, 0), (2.9, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.45, -60, 55, 0), (2.5, -60, 55, 0), (2.9, 0, 0, 0))},
            "foot_left": {"rotation": T((0, 0, 0, 0), (0.45, 25, 0, 0), (2.6, 25, 0, 0), (2.9, 0, 0, 0))},
            "foot_right": {"rotation": T((0, 0, 0, 0), (0.45, 25, 0, 0), (2.6, 25, 0, 0), (2.9, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.6, -6, 0, -5), (1.2, -6, 0, 5), (1.8, -6, 0, -5), (2.4, -6, 0, 4), (3.0, 0, 0, 0))}}),
        # a pose: the staff swings up over his head and opens, he hangs under it
        "zweef": (4.0, {
            "aang_staf": {"rotation": T((0, 90, 0, 0)), "position": T((0, 5.5, 21, 6.7), (2, 5.5, 22, 6.7), (4, 5.5, 21, 6.7))},
            "aang_vleugels": {"scale": T((0, 1))},
            "body": {"position": T((0, 0, 4, 0), (2, 0, 5, 0), (4, 0, 4, 0)), "rotation": T((0, 6, 0, 0))},
            "arm_left": {"rotation": T((0, -150, 10, 0))}, "arm_right": {"rotation": T((0, -150, -10, 0))},
            "foot_left": {"rotation": T((0, 30, 0, 0), (2, 40, 0, 0), (4, 30, 0, 0))},
            "foot_right": {"rotation": T((0, 40, 0, 0), (2, 30, 0, 0), (4, 40, 0, 0))},
            "tail": {"rotation": T((0, 0, -14, 0), (2, 0, 14, 0), (4, 0, -14, 0))}}),
        "lach": (1.6, _lach_zit()),
    }


def _katara():
    return {
        "waterzweep": (2.4, {
            "katara_water": {"scale": T((0, 0), (0.2, 0), (0.5, 1.0), (1.9, 1.0), (2.2, 0), (2.4, 0)),
                             "rotation": T((0, 0, -50, 0), (0.5, 0, -30, 0), (1.0, 0, 35, -8), (1.5, 0, -25, 8), (1.9, 0, 10, 0), (2.4, 0, 0, 0))},
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.4, -70, -30, 0), (1.0, -50, 40, 0), (1.5, -80, -30, 0), (2.0, -30, 0, 0), (2.4, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.4, -40, 0, 0), (1.0, -75, 40, 0), (1.5, -40, -20, 0), (2.0, -20, 0, 0), (2.4, 0, 0, 0))},
            "body": {"rotation": T((0, 0, 0, 0), (0.5, 0, -10, 0), (1.0, 0, 12, 0), (1.5, 0, -8, 0), (2.1, 0, 0, 0))},
            "katara_vlecht": {"rotation": T((0, 0, 0, 0), (0.6, 0, 0, 14), (1.1, 0, 0, -16), (1.6, 0, 0, 10), (2.2, 0, 0, 0))}}),
        "genees": (2.4, {
            "katara_gloed": {"scale": T((0, 0), (0.3, 0), (0.6, 1.0), (0.9, 1.2), (1.2, 1.0), (1.5, 1.2), (1.8, 1.0), (2.2, 0), (2.4, 0))},
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.4, -30, 12, 0), (2.0, -30, 12, 0), (2.4, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.4, -30, -12, 0), (2.0, -30, -12, 0), (2.4, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.5, 12, 0, 0), (2.0, 12, 0, 0), (2.4, 0, 0, 0))}}),
        # a pose: paws on her hips, a foot that taps
        "armen": (4.0, {
            "arm_left": {"rotation": T((0, 25, -70, 0))}, "arm_right": {"rotation": T((0, 25, 70, 0))},
            "head": {"rotation": T((0, 4, 0, 8), (2, 4, 0, 6), (4, 4, 0, 8))},
            "foot_right": {"rotation": T((0, 0, 0, 0), (0.25, -22, 0, 0), (0.5, 0, 0, 0), (1.0, 0, 0, 0), (1.25, -22, 0, 0), (1.5, 0, 0, 0), (2.0, 0, 0, 0),
                                         (2.25, -22, 0, 0), (2.5, 0, 0, 0), (3.0, 0, 0, 0), (3.25, -22, 0, 0), (3.5, 0, 0, 0), (4.0, 0, 0, 0))}}),
    }


def _sokka():
    boog = [(0.5, 0, 0, 0), (0.9, -10, 5, -16), (1.3, -2, 10, -30), (1.7, 12, 6, -18), (2.1, 4, 2, -4), (2.3, 0, 0, 0)]
    tol = [(0.5 + i * 0.15, 0, 0, i * 120.0) for i in range(13)]
    return {
        "boemerang": (2.8, {
            "sokka_boemerang": {"scale": T((0, 1), (0.15, 1), (0.25, 0), (2.4, 0), (2.55, 1), (2.8, 1))},
            "sokka_vlucht": {"scale": T((0, 0), (0.3, 0), (0.4, 1), (2.3, 1), (2.4, 0), (2.8, 0)),
                             "position": T((0, 0, 0, 0), *boog, (2.8, 0, 0, 0)),
                             "rotation": T((0, 0, 0, 0), *tol, (2.8, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.3, -150, 0, 0), (0.5, -20, 0, 0), (0.7, -40, 0, 0), (2.1, -40, 0, 0), (2.3, -90, 0, 0), (2.6, 0, 0, 0))},
            "body": {"rotation": T((0, 0, 0, 0), (0.3, 0, -14, 0), (0.5, 0, 14, 0), (0.8, 0, 0, 0), (2.8, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.9, -8, 8, 0), (1.3, -12, 0, 0), (1.7, -8, -8, 0), (2.3, 0, 0, 0))}}),
        "plan": (2.0, {
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.3, -75, 28, 0), (1.0, -75, 28, 0), (1.2, -165, 0, 0), (1.7, -165, 0, 0), (2.0, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.3, 6, 0, 8), (1.0, 6, 0, 8), (1.2, -12, 0, 0), (1.7, -12, 0, 0), (2.0, 0, 0, 0))},
            "body": {"position": T((0, 0, 0, 0), (1.0, 0, 0, 0), (1.2, 0, 1.4, 0), (1.4, 0, 0, 0), (2.0, 0, 0, 0))}}),
        "honger": (2.0, {
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.3, 30, 22, 0), (0.6, 30, -8, 0), (0.9, 30, 22, 0), (1.2, 30, -8, 0), (1.5, 30, 22, 0), (2.0, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.3, 30, 8, 0), (0.6, 30, -22, 0), (0.9, 30, 8, 0), (1.2, 30, -22, 0), (1.5, 30, 8, 0), (2.0, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.4, 14, 0, 0), (1.5, 14, 0, 4), (2.0, 0, 0, 0))},
            "body": {"scale": T((0, 1, 1, 1), (0.5, 1.05, 0.97, 1.05), (1.0, 1, 1, 1), (1.5, 1.05, 0.97, 1.05), (2.0, 1, 1, 1))}}),
    }


def _toph():
    return {
        "stamp": (1.8, {
            "foot_right": {"rotation": T((0, 0, 0, 0), (0.3, -55, 0, 0), (0.42, 0, 0, 0), (1.8, 0, 0, 0)),
                           "position": T((0, 0, 0, 0), (0.3, 0, 2.2, 0), (0.42, 0, 0, 0), (1.8, 0, 0, 0))},
            "body": {"position": T((0, 0, 0, 0), (0.3, 0, 0.6, 0), (0.44, 0, -0.5, 0), (0.6, 0, 0, 0), (1.8, 0, 0, 0))},
            "toph_zuil": {"scale": T((0, 0, 0, 0), (0.42, 1, 0, 1), (0.6, 1, 1.12, 1), (0.7, 1, 1, 1), (1.4, 1, 1, 1), (1.65, 1, 0, 1), (1.7, 0, 0, 0), (1.8, 0, 0, 0))},
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.42, -35, -20, 0), (0.7, -70, -25, 0), (1.4, -70, -25, 0), (1.8, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.42, -20, 10, 0), (1.4, -20, 10, 0), (1.8, 0, 0, 0))},
            "ear_left": {"rotation": T((0, 0, 0, 0), (0.45, 0, 0, -18), (0.6, 0, 0, 0))},
            "ear_right": {"rotation": T((0, 0, 0, 0), (0.45, 0, 0, 18), (0.6, 0, 0, 0))}}),
        "wijs": (1.6, {
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.25, -95, 0, 0), (1.25, -95, 0, 0), (1.6, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.25, -8, 0, -6), (1.25, -8, 0, -6), (1.6, 0, 0, 0))},
            "body": {"position": T((0, 0, 0, 0), (0.25, 0, 0.8, 0), (0.5, 0, 0, 0), (1.6, 0, 0, 0))}}),
        "lach": (1.6, _lach_zit()),
    }


def _zuka():
    schud = [(0.5 + i * 0.2, -115, 0, 22 if i % 2 else -22) for i in range(7)]
    return {
        "mijn_eer": (2.4, {
            "root": {"rotation": T((0, 0, 0, 0), (0.35, -24, 0, 0), (1.9, -24, 0, 0), (2.3, 0, 0, 0), (2.4, 0, 0, 0)),
                     "position": T((0, 0, 0, 0), (0.35, 0, 3.4, 0), (1.9, 0, 3.4, 0), (2.3, 0, 0, 0), (2.4, 0, 0, 0))},
            "leg_front_right": {"rotation": T((0, 0, 0, 0), (0.35, -115, 0, 0), *schud, (2.0, -115, 0, 0), (2.3, 0, 0, 0)),
                                "scale": T((0, 1), (0.35, 1.5), (1.9, 1.5), (2.3, 1))},
            "head": {"rotation": T((0, 0, 0, 0), (0.35, -20, 0, 0), (1.1, -24, 0, 4), (1.9, -20, 0, -4), (2.3, 0, 0, 0))},
            "ear_left": {"rotation": T((0, 0, 0, 0), (0.35, 0, 0, 16), (1.9, 0, 0, 16), (2.3, 0, 0, 0))},
            "ear_right": {"rotation": T((0, 0, 0, 0), (0.35, 0, 0, -16), (1.9, 0, 0, -16), (2.3, 0, 0, 0))},
            "tail": {"rotation": T((0, 0, -16, 0), (0.4, 20, 30, 0), (0.8, 20, -30, 0), (1.2, 20, 30, 0), (1.6, 20, -30, 0), (2.4, 0, -16, 0))}}),
        "zucht": (2.4, {
            "zuka_dienblad": {"position": T((0, 0, 0, 0), (0.8, 0, -1.6, 0), (1.9, 0, -1.6, 0), (2.4, 0, 0, 0)),
                              "rotation": T((0, 0, 0, 0), (0.8, 0, 0, 10), (1.9, 0, 0, 10), (2.4, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.3, -6, 0, 0), (0.9, 12, 0, 0), (1.9, 12, 0, 0), (2.4, 0, 0, 0))},
            "body": {"scale": T((0, 1, 1, 1), (0.3, 1.05, 1.06, 1.05), (1.0, 0.98, 0.95, 0.98), (1.9, 0.98, 0.95, 0.98), (2.4, 1, 1, 1))},
            "ear_left": {"rotation": T((0, 0, 0, 0), (0.9, 0, 0, 16), (1.9, 0, 0, 16), (2.4, 0, 0, 0))},
            "ear_right": {"rotation": T((0, 0, 0, 0), (0.9, 0, 0, -16), (1.9, 0, 0, -16), (2.4, 0, 0, 0))}}),
        "vuur": (1.6, {
            "zuka_vlam": {"scale": T((0, 0), (0.5, 0), (0.65, 1.1), (0.9, 0.8), (1.1, 0.4), (1.25, 0), (1.6, 0)),
                          "position": T((0, 0, 0, 0), (0.5, 0, 0, 0), (1.2, 0, 1.5, -3.0), (1.6, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.4, -12, 0, 0), (0.6, 6, 0, 0), (1.2, 4, 0, 0), (1.6, 0, 0, 0))},
            "body": {"scale": T((0, 1, 1, 1), (0.4, 1.06, 1.08, 1.06), (0.65, 0.98, 0.96, 0.98), (1.6, 1, 1, 1))}}),
        "buig": (2.0, {
            "head": {"rotation": T((0, 0, 0, 0), (0.5, 26, 0, 0), (1.4, 26, 0, 0), (2.0, 0, 0, 0)),
                     "position": T((0, 0, 0, 0), (0.5, 0, -1.0, 0), (1.4, 0, -1.0, 0), (2.0, 0, 0, 0))},
            "ear_left": {"rotation": T((0, 0, 0, 0), (0.5, 0, 0, 10), (1.4, 0, 0, 10), (2.0, 0, 0, 0))},
            "ear_right": {"rotation": T((0, 0, 0, 0), (0.5, 0, 0, -10), (1.4, 0, 0, -10), (2.0, 0, 0, 0))}}),
    }


def _miroh():
    return {
        "thee": (3.2, {
            "leg_front_left": {"rotation": T((0, 0, 0, 0), (0.6, -30, 0, 0), (2.3, -30, 0, 0), (2.8, 0, 0, 0), (3.2, 0, 0, 0)),
                               "position": T((0, 0, 0, 0), (0.6, 0, 1.6, 0), (2.3, 0, 1.6, 0), (2.8, 0, 0, 0), (3.2, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.6, 0, 0, 0), (1.0, 9, 0, 0), (1.4, 9, 0, 0), (1.7, -5, 0, 0), (2.2, -5, 0, 0), (2.8, -10, 0, 0), (3.2, 0, 0, 0))},
            "body": {"scale": T((0, 1, 1, 1), (2.3, 1, 1, 1), (2.8, 1.05, 1.07, 1.05), (3.2, 1, 1, 1))},
            "ear_left": {"rotation": T((0, 0, 0, 0), (2.5, 0, 0, 0), (2.8, 0, 0, -12), (3.2, 0, 0, 0))},
            "ear_right": {"rotation": T((0, 0, 0, 0), (2.5, 0, 0, 0), (2.8, 0, 0, 12), (3.2, 0, 0, 0))}}),
        "lach": (1.8, {
            "body": {"scale": T((0, 1, 1, 1), (0.15, 1.06, 0.94, 1.06), (0.3, 0.97, 1.06, 0.97), (0.45, 1.06, 0.94, 1.06), (0.6, 0.97, 1.06, 0.97),
                                (0.75, 1.06, 0.94, 1.06), (0.9, 0.97, 1.06, 0.97), (1.05, 1.05, 0.95, 1.05), (1.2, 0.98, 1.04, 0.98), (1.5, 1, 1, 1), (1.8, 1, 1, 1))},
            "head": {"rotation": T((0, 0, 0, 0), (0.2, -14, 0, 0), (0.6, -16, 0, 4), (1.0, -14, 0, -4), (1.4, -6, 0, 0), (1.8, 0, 0, 0)),
                     "position": T((0, 0, 0, 0), (0.15, 0, 0.8, 0), (0.3, 0, 0, 0), (0.45, 0, 0.8, 0), (0.6, 0, 0, 0), (0.75, 0, 0.8, 0), (0.9, 0, 0, 0), (1.8, 0, 0, 0))},
            "tail": {"rotation": T((0, 0, -16, 0), (0.3, 0, 26, 0), (0.6, 0, -26, 0), (0.9, 0, 26, 0), (1.2, 0, -26, 0), (1.8, 0, -16, 0))}}),
        "wijsheid": (2.2, {
            "leg_front_right": {"rotation": T((0, 0, 0, 0), (0.35, -105, 0, 0), (0.7, -95, 0, 0), (1.05, -105, 0, 0), (1.8, -100, 0, 0), (2.2, 0, 0, 0)),
                                "scale": T((0, 1), (0.35, 1.4), (1.8, 1.4), (2.2, 1))},
            "head": {"rotation": T((0, 0, 0, 0), (0.35, -6, 0, -6), (1.8, -6, 0, -6), (2.2, 0, 0, 0))}}),
        "pai_sho": (2.4, {
            "miroh_tegel": {"scale": T((0, 0), (0.3, 0), (0.45, 1), (2.0, 1), (2.2, 0), (2.4, 0)),
                            "position": T((0, 2.5, 5.0, 3.0), (0.45, 2.5, 5.0, 3.0), (0.9, 0, 0, 0), (2.4, 0, 0, 0))},
            "leg_front_right": {"rotation": T((0, 0, 0, 0), (0.45, -70, 0, 0), (0.9, -5, 0, 0), (1.2, 0, 0, 0), (2.4, 0, 0, 0))},
            "head": {"rotation": T((0, 0, 0, 0), (0.9, 8, 0, 0), (1.3, 8, 0, 0), (1.6, -6, 0, 5), (2.0, -6, 0, 5), (2.4, 0, 0, 0))},
            "body": {"scale": T((0, 1, 1, 1), (1.4, 1, 1, 1), (1.7, 1.04, 1.05, 1.04), (2.4, 1, 1, 1))}}),
    }


def _suki():
    fan = {"scale": T((0, 1, 1, 1), (0.25, 0.12, 1, 1), (0.45, 0.12, 1, 1), (0.55, 1.15, 1.1, 1), (0.7, 1, 1, 1), (2.0, 1, 1, 1))}
    return {
        "waaier": (2.0, {
            "suki_waaier_links": dict(fan), "suki_waaier_rechts": dict(fan),
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.45, 20, 20, 0), (0.6, -70, -30, 0), (1.5, -70, -30, 0), (2.0, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.45, 20, -20, 0), (0.6, -70, 30, 0), (1.5, -70, 30, 0), (2.0, 0, 0, 0))},
            "body": {"rotation": T((0, 0, 0, 0), (0.55, 0, 0, 0), (1.05, 0, 180, 0), (1.55, 0, 360, 0), (2.0, 0, 360, 0)),
                     "position": T((0, 0, 0, 0), (0.55, 0, 0, 0), (0.8, 0, 1.5, 0), (1.3, 0, 1.5, 0), (1.55, 0, 0, 0), (2.0, 0, 0, 0))}}),
        "groet": (1.6, {
            "head": {"rotation": T((0, 0, 0, 0), (0.4, 22, 0, 0), (1.1, 22, 0, 0), (1.6, 0, 0, 0))},
            "body": {"rotation": T((0, 0, 0, 0), (0.4, 8, 0, 0), (1.1, 8, 0, 0), (1.6, 0, 0, 0))},
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.4, -20, 30, 0), (1.1, -20, 30, 0), (1.6, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.4, -20, -30, 0), (1.1, -20, -30, 0), (1.6, 0, 0, 0))}}),
        # a pose: arms crossed, the fans shut, she looks about
        "wacht": (4.0, {
            "arm_left": {"rotation": T((0, -10, 40, 0))}, "arm_right": {"rotation": T((0, -10, -40, 0))},
            "suki_waaier_links": {"scale": T((0, 0.12, 1, 1))}, "suki_waaier_rechts": {"scale": T((0, 0.12, 1, 1))},
            "head": {"rotation": T((0, 0, 0, 0), (1.0, 0, 22, 0), (2.0, 0, 0, 0), (3.0, 0, -22, 0), (4.0, 0, 0, 0))},
            "ear_left": {"rotation": T((0, 0, 0, 0), (1.0, 0, 0, -10), (2.0, 0, 0, 0), (4.0, 0, 0, 0))},
            "ear_right": {"rotation": T((0, 0, 0, 0), (3.0, 0, 0, 10), (4.0, 0, 0, 0))}}),
    }


def _yue():
    return {
        "buig": (2.0, {
            "head": {"rotation": T((0, 0, 0, 0), (0.5, 22, 0, 0), (1.4, 22, 0, 0), (2.0, 0, 0, 0))},
            "body": {"rotation": T((0, 0, 0, 0), (0.5, 9, 0, 0), (1.4, 9, 0, 0), (2.0, 0, 0, 0))},
            "arm_left": {"rotation": T((0, 0, 0, 0), (0.5, -15, 28, 0), (1.4, -15, 28, 0), (2.0, 0, 0, 0))},
            "arm_right": {"rotation": T((0, 0, 0, 0), (0.5, -15, -28, 0), (1.4, -15, -28, 0), (2.0, 0, 0, 0))}}),
        # a pose: she rises a little and her hair floats
        "maan": (6.0, {
            "body": {"position": T((0, 0, 3.0, 0), (3, 0, 4.2, 0), (6, 0, 3.0, 0))},
            "yue_haar": {"rotation": T((0, -5, 0, 3), (1.5, -8, 0, 0), (3, -5, 0, -3), (4.5, -8, 0, 0), (6, -5, 0, 3)),
                         "scale": T((0, 1, 1.04, 1), (3, 1, 1.1, 1), (6, 1, 1.04, 1))},
            "arm_left": {"rotation": T((0, -25, -30, 0), (3, -35, -35, 0), (6, -25, -30, 0))},
            "arm_right": {"rotation": T((0, -25, 30, 0), (3, -35, 35, 0), (6, -25, 30, 0))},
            "foot_left": {"rotation": T((0, 30, 0, 0), (3, 38, 0, 0), (6, 30, 0, 0))},
            "foot_right": {"rotation": T((0, 36, 0, 0), (3, 28, 0, 0), (6, 36, 0, 0))},
            "tail": {"rotation": T((0, 0, -12, 0), (3, 0, 12, 0), (6, 0, -12, 0))}}),
    }


def _veerguh():
    return {"kaartje": (1.6, {
        "arm_right": {"rotation": T((0, 0, 0, 0), (0.4, -15, -38, 0), (0.6, -15, -38, 0), (0.7, -5, -38, 0), (0.85, -15, -38, 0), (1.2, -15, -38, 0), (1.6, 0, 0, 0))},
        "arm_left": {"rotation": T((0, 0, 0, 0), (0.4, -15, 12, 0), (1.0, -15, 12, 0), (1.25, -60, -20, 0), (1.45, -60, -20, 0), (1.6, 0, 0, 0))},
        "veerguh_kaartje": {"rotation": T((0, 0, 0, 0), (0.7, 0, 0, 0), (0.78, 0, 0, 14), (0.9, 0, 0, 0), (1.6, 0, 0, 0))},
        "head": {"rotation": T((0, 0, 0, 0), (0.4, 10, 0, 0), (1.0, 10, 0, 0), (1.25, -6, 0, 0), (1.6, 0, 0, 0))}})}


def _scheepsbouwer():
    slag = []
    for i in range(3):
        t = 0.2 + i * 0.55
        slag += [(t, -95, 0, 0), (t + 0.18, 15, 0, 0), (t + 0.3, 5, 0, 0)]
    dip = []
    for i in range(3):
        t = 0.2 + i * 0.55
        dip += [(t, 0, 0.5, 0), (t + 0.18, 0, -0.5, 0), (t + 0.3, 0, 0, 0)]
    return {"timmer": (2.0, {
        "arm_right": {"rotation": T((0, 0, 0, 0), *slag, (2.0, 0, 0, 0))},
        "body": {"position": T((0, 0, 0, 0), *dip, (2.0, 0, 0, 0))},
        "head": {"rotation": T((0, 0, 0, 0), (0.2, 8, 0, -4), (1.6, 8, 0, -4), (2.0, 0, 0, 0))},
        "ear_left": {"rotation": T((0, 0, 0, 0), (0.38, 0, 0, -12), (0.5, 0, 0, 0), (0.93, 0, 0, -12), (1.05, 0, 0, 0), (1.48, 0, 0, -12), (1.6, 0, 0, 0))}})}


def _kroegbaas():
    wrijf = [(0.4 + i * 0.25, -35 + (8 if i % 2 else -8), 48, 0) for i in range(7)]
    return {"poets": (2.4, {
        "arm_right": {"rotation": T((0, 0, 0, 0), (0.4, -30, -14, 0), (2.0, -30, -14, 0), (2.4, 0, 0, 0))},
        "arm_left": {"rotation": T((0, 0, 0, 0), *wrijf, (2.1, -35, 48, 0), (2.4, 0, 0, 0))},
        "head": {"rotation": T((0, 0, 0, 0), (0.4, 10, 0, -6), (2.0, 10, 0, -6), (2.4, 0, 0, 0))},
        "kroegbaasguh_pul": {"rotation": T((0, 0, 0, 0), (0.4, 0, 0, 0), (0.9, 0, 30, 0), (1.5, 0, -20, 0), (2.0, 0, 0, 0), (2.4, 0, 0, 0))}})}


def _kapitein():
    return {"verhaal": (2.4, {
        "arm_left": {"rotation": T((0, 0, 0, 0), (0.4, -40, 15, 0), (0.8, -45, -75, 0), (1.3, -50, -80, 0), (1.8, -45, -75, 0), (2.4, 0, 0, 0))},
        "arm_right": {"rotation": T((0, 0, 0, 0), (0.4, -40, -15, 0), (0.8, -45, 75, 0), (1.3, -50, 80, 0), (1.8, -45, 75, 0), (2.4, 0, 0, 0))},
        "head": {"rotation": T((0, 0, 0, 0), (0.4, 6, 0, 0), (0.8, -12, 0, 0), (1.3, -14, 0, 5), (1.8, -12, 0, -5), (2.4, 0, 0, 0))},
        "body": {"scale": T((0, 1, 1, 1), (0.8, 1.06, 1.05, 1.06), (1.8, 1.06, 1.05, 1.06), (2.4, 1, 1, 1))}})}


def _racemeester():
    zwaai = [(0.3 + i * 0.25, -70, 0, 26 if i % 2 else -26) for i in range(6)]
    wap = [(0.3 + i * 0.25, 0, 0, -12 if i % 2 else 12) for i in range(6)]
    return {"vlag": (2.0, {
        "arm_right": {"rotation": T((0, 0, 0, 0), *zwaai, (1.7, -70, 0, 0), (2.0, 0, 0, 0))},
        "ringenraceguh_vlag": {"rotation": T((0, 0, 0, 0), *wap, (2.0, 0, 0, 0))},
        "ringenraceguh_sjaal_eind": {"rotation": T((0, 0, 0, 0), (0.5, 0, 12, 0), (1.0, 0, -12, 0), (1.5, 0, 10, 0), (2.0, 0, 0, 0))},
        "head": {"rotation": T((0, 0, 0, 0), (0.3, -8, 0, 0), (1.7, -8, 0, 0), (2.0, 0, 0, 0))}})}


MAKERS = {"aang_guh": _aang, "katara_guh": _katara, "sokka_guh": _sokka, "toph_guh": _toph, "zuka": _zuka, "oom_miroh": _miroh,
          "suki_guh": _suki, "yue_guh": _yue, "veerguh": _veerguh, "scheepsbouwerguh": _scheepsbouwer, "kroegbaasguh": _kroegbaas,
          "kapiteinguh": _kapitein, "ringenraceguh": _racemeester}

# what moves all the time in the idle of a character, besides the quiet part
ALTIJD = {
    "oom_miroh": {"miroh_stoom": {
        "position": T(*[(t, 0, (t % 2.0) * 0.9, 0) for t in [i * 0.5 for i in range(24)]] + [(12.0, 0, 0, 0)]),
        "scale": T(*[(t, 1.0 - (t % 2.0) * 0.3) for t in [i * 0.5 for i in range(24)]] + [(12.0, 1.0)])}},
    "ringenraceguh": {"ringenraceguh_sjaal_eind": {
        "rotation": T(*[(float(t), 0, 10 if t % 2 else -10, 3 if t % 2 else -3) for t in range(13)])}},
    "katara_guh": {"katara_vlecht": {"rotation": T(*[(float(t * 2), 0, 0, 5 if t % 2 else -5) for t in range(7)])}},
}


def animaties(basis):
    """{short name: animation} of a character: idle, praat and its named moves."""
    lijf = "mika" if basis in MIKAS else "zit"
    props = PROPS.get(basis, ())
    zetten = MAKERS[basis]()
    out = {}
    for naam in NAMEN[basis]:
        lengte, botten = zetten[naam]
        out[naam] = anim(lengte, botten, lus=(basis, naam) in HOUDINGEN, weg=props)
    eerste = out[NAMEN[basis][0]]
    idle = weef(12.0, lijf, eerste, 5.0, weg=props, extra=ALTIJD.get(basis))
    praat = anim(1.2, _praat_mika() if lijf == "mika" else _praat_zit(), weg=props)
    return {"idle": idle, "praat": praat, **out}


def bestand(basis):
    """The animation file of a character (GeckoLib)."""
    return {"format_version": "1.8.0",
            "animations": {f"animation.guh_sitting.{naam}": a for naam, a in animaties(basis).items()}}


def duur(basis, naam):
    """Ticks of a move; 0 for a pose that lasts."""
    if (basis, naam) in HOUDINGEN:
        return 0
    if naam == "praat":
        return 24
    return int(round(MAKERS[basis]()[naam][0] * 20))
