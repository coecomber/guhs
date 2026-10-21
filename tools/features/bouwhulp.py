"""
1.6.0 "Op Safari!" deel 2, F8: de Bouwhulp kern. Java: feature/bouwhulp (kern: the spookbouw; keet: the Bouwkeet).

  - block bouwkeet: a yellow-pink hut of 3 x 3 x 3 (GeckoLib, drawn from the kern block): plank walls, a pink door with a
    round guh face above it, windows with a blueprint behind them, plans pinned to the side walls, a stepped pink roof
    with two guh ears and a little yellow crane on top that turns and hoists a beam while the keet builds
    (animations rust / bouwt). The rest of the box is machine_deel. Recipe: tier 0 (wool, planks, a crafting table,
    kaas_knabbels).
  - items bouwkeet_upgrade_guhrad / _zout / _saus / _gloei: the four level upgrades, one per tech tier (an own choice of
    names and recipes, BOUWHULP_KEUZES.md): Guhrad-tandwieltje (tier 1), Zoutkristal-waterpas (tier 2, zoutkristal),
    Saus-kraanhaakje (tier 3, the Uitvinder-guh's machine card, which stays), Gloei-bouwlamp (tier 4, gloeister)
  - the texts of the keet menu, the ghost's control panel, the bouwguhs and the dev command

  python tools/features/bouwhulp.py      on its own: writes the files and merges the texts into nl_nl.json / en_us.json
"""
import json
import os
import sys

import numpy as np
from PIL import Image

HIER = os.path.dirname(os.path.abspath(__file__))

UPGRADES = ["bouwkeet_upgrade_guhrad", "bouwkeet_upgrade_zout", "bouwkeet_upgrade_saus", "bouwkeet_upgrade_gloei"]

# ======================================================================================================================
# texts (Dutch; English in tools/lang/en/c180_bouwhulp.json)
TEKSTEN = {
    "block.guhs.bouwkeet": "Bouwkeet",
    "item.guhs.bouwkeet": "Bouwkeet",
    "item.guhs.bouwkeet_upgrade_guhrad": "Guhrad-tandwieltje",
    "item.guhs.bouwkeet_upgrade_guhrad.lore": "Klik op een Bouwkeet: bouwguhs en draadloos materiaal. Tandjes, njeg!",
    "item.guhs.bouwkeet_upgrade_zout": "Zoutkristal-waterpas",
    "item.guhs.bouwkeet_upgrade_zout.lore": "Klik op een Bouwkeet: natuurvormen, de egaliseerder en bloktekst. Altijd recht, nooit scheef.",
    "item.guhs.bouwkeet_upgrade_saus": "Saus-kraanhaakje",
    "item.guhs.bouwkeet_upgrade_saus.lore": "Klik op een Bouwkeet: verfmachine, tunnelboor en publiceren in de bibliotheek.",
    "item.guhs.bouwkeet_upgrade_gloei": "Gloei-bouwlamp",
    "item.guhs.bouwkeet_upgrade_gloei.lore": "Klik op een Bouwkeet: vormen tot 64 groot en... POF!",
    "item.guhs.bouwkeet_upgrade.naar": "Maakt een keet %s (trekt %s VK)",
    # --- the keet ---
    "gui.guhs.bouwkeet.niveau.0": "Handwerk",
    "gui.guhs.bouwkeet.niveau.1": "Guhrad-keet",
    "gui.guhs.bouwkeet.niveau.2": "Zoutkeet",
    "gui.guhs.bouwkeet.niveau.3": "Sauskeet",
    "gui.guhs.bouwkeet.niveau.4": "Gloeikeet",
    "gui.guhs.bouwkeet.niveau.vk": "trekt %s VK",
    "gui.guhs.bouwkeet.niveau.wat.0": "Vormstaf en kwasten, vormen tot 16, daken, eigen blauwdrukken tot 16, de bibliotheek",
    "gui.guhs.bouwkeet.niveau.wat.1": "Bouwguhs, vormen tot 24, draadloos materiaal uit alle kisten op het vadsnet",
    "gui.guhs.bouwkeet.niveau.wat.2": "Natuurvormen, de egaliseerder, bloktekst, blauwdrukken tot 32",
    "gui.guhs.bouwkeet.niveau.wat.3": "Verfmachine, tunnelboor, publiceren in de bibliotheek",
    "gui.guhs.bouwkeet.niveau.wat.4": "Vormen en blauwdrukken tot 64, en Pof!",
    "gui.guhs.bouwkeet.geen_kracht": "(geen vadskracht: handwerk)",
    "gui.guhs.bouwkeet.uit": "(uitgezet: handwerk)",
    "gui.guhs.bouwkeet.tab.vormen": "Vormen",
    "gui.guhs.bouwkeet.tab.daken": "Daken",
    "gui.guhs.bouwkeet.tab.blauwdrukken": "Blauwdrukken",
    "gui.guhs.bouwkeet.tab.bouwguhs": "Bouwguhs",
    "gui.guhs.bouwkeet.tab.materiaal": "Materiaal",
    "gui.guhs.bouwkeet.tab.niveau": "Niveau",
    "gui.guhs.bouwkeet.tab.vanaf": "Vanaf %s",
    "gui.guhs.bouwkeet.sluit": "Sluit",
    "gui.guhs.bouwkeet.terug": "↶ Terug",
    "gui.guhs.bouwkeet.pof": "Pof!",
    "gui.guhs.bouwkeet.bediening": "Spook...",
    "gui.guhs.bouwkeet.bediening.uitleg": "Schuif, draai of spiegel het spook, en zie wat je nog mist.",
    "gui.guhs.bouwkeet.loslaten": "Loslaten",
    "gui.guhs.bouwkeet.loslaten.uitleg": "Het spook blijft staan, maar deze keet bouwt het niet meer. Zo kun je een nieuw project kiezen.",
    "gui.guhs.bouwkeet.geen_project": "Nog geen project. Kies een vorm, een dak of een blauwdruk, en de keet zet er een spook van neer.",
    "gui.guhs.bouwkeet.project": "%s: %s×%s×%s, %s blokjes",
    "gui.guhs.bouwkeet.niet_van_jou": "Dit is niet jouw keet, njeg.",
    "gui.guhs.bouwkeet.bank_gekoppeld": "De Bank Guh is gekoppeld: de keet pakt daar ook blokjes uit.",
    "gui.guhs.bouwkeet.upgrade.al": "Deze keet is al %s of beter.",
    "gui.guhs.bouwkeet.upgrade.eerst": "Eerst moet de keet %s worden.",
    "gui.guhs.bouwkeet.upgrade.klaar": "De keet is nu een %s!",
    "gui.guhs.bouwkeet.binnenkort.vormen": "Hier kies je straks een vorm: bol, koepel, toren, boog, hart, guh-silhouet en nog veel meer. Hol of gevuld, met je eigen blokjes.\n\nTot die tijd: probeer de spookbouw met het proefhuisje!",
    "gui.guhs.bouwkeet.binnenkort.daken": "Hier kies je straks een dak: zadeldak, schilddak, tentdak of het guh-oortjesdak.",
    "gui.guhs.bouwkeet.binnenkort.blauwdrukken": "Hier komen straks je eigen blauwdrukken, de catalogus en de bibliotheek van de server.",
    "gui.guhs.bouwkeet.proefhuisje": "Proefhuisje",
    "gui.guhs.bouwkeet.proefhuisje.knop": "Proefhuisje als project",
    "gui.guhs.bouwkeet.proefhuisje.uitleg": "Een klein roze guh-huisje voor de deur van de keet, om de spookbouw te proberen.",
    "gui.guhs.bouwkeet.bank.geen": "Geen Bank Guh gekoppeld (klik met een Banksleutel op de keet).",
    "gui.guhs.bouwkeet.bank.ja": "Bank Guh gekoppeld: de keet pakt er blokjes uit.",
    "gui.guhs.bouwkeet.bank.kwijt": "De gekoppelde Bank Guh staat nergens meer.",
    "gui.guhs.bouwkeet.bank.los": "Bank loskoppelen",
    "gui.guhs.bouwkeet.draadloos.ja": "Draadloos materiaal: %s kisten op het vadsnet",
    "gui.guhs.bouwkeet.draadloos.nee": "Draadloos materiaal: vanaf de Guhrad-keet (met vadskracht)",
    "gui.guhs.bouwkeet.gratis": "Creatief: alles is gratis.",
    "gui.guhs.bouwkeet.nodig": "Nog nodig (heb/nodig):",
    "gui.guhs.bouwkeet.spoken": "Je spoken:",
    "gui.guhs.bouwkeet.spoken.regel": "%s, %s blokjes, %s blokjes ver, %s",
    "gui.guhs.bouwkeet.spoken.toon": "Toon",
    "gui.guhs.bouwkeet.spoken.hier": "Bouw hier",
    "gui.guhs.bouwkeet.zet_aan": "Zet aan",
    "gui.guhs.bouwkeet.zet_uit": "Zet uit",
    "gui.guhs.bouwkeet.vk.geen": "Handwerk trekt geen vadskracht.",
    "gui.guhs.bouwkeet.vk.ja": "Trekt %s VK: de keet draait!",
    "gui.guhs.bouwkeet.vk.nee": "Wil %s VK, maar krijgt niks: hij werkt als handwerk.",
    "gui.guhs.bouwkeet.volgende": "Volgende upgrade: %s",
    "gui.guhs.bouwkeet.commando.geen": "Kijk naar een Bouwkeet.",
    "gui.guhs.bouwkeet.commando.niveau": "De keet is nu niveau %s.",
    # --- bouwguhs ---
    "gui.guhs.bouwguh.uitleg": "Wijs tot %s eigen guhs aan als bouwguh. Nu aan het werk: %s.",
    "gui.guhs.bouwguh.vanaf": "Bouwguhs kunnen vanaf de Guhrad-keet (met vadskracht). Dan zetten ze een helmpje op en bouwen ze mee!",
    "gui.guhs.bouwguh.geen": "Geen eigen guhs in de buurt van de keet.",
    "gui.guhs.bouwguh.kies": "Bouwguh!",
    "gui.guhs.bouwguh.stop": "Klaar",
    "gui.guhs.bouwguh.weg": "(weg)",
    "gui.guhs.bouwguh.kan_niet": "Dat kan niet: al vier bouwguhs, of die guh is te ver weg.",
    "gui.guhs.bouwguh.praat.0": "%s: njeg, nog %s blokjes!",
    "gui.guhs.bouwguh.praat.1": "%s fluit een deuntje... nog %s blokjes.",
    "gui.guhs.bouwguh.praat.2": "%s: vahoeg, dit wordt mooi! Nog %s.",
    "gui.guhs.bouwguh.praat.3": "%s duwt een blokje recht. Nog %s te gaan.",
    # --- the ghost and its panel ---
    "gui.guhs.bouwhulp.project": "Project",
    "gui.guhs.bouwhulp.klaar": "Klaar! %s staat er.",
    "gui.guhs.bouwhulp.pof": "POF!",
    "gui.guhs.bouwhulp.pof.onder": "Daar staat het, in één keer.",
    "gui.guhs.bouwhulp.terug.niets": "Er is niets meer om terug te draaien.",
    "gui.guhs.bouwhulp.spook.titel": "Spook",
    "gui.guhs.bouwhulp.spook.van": "van %s",
    "gui.guhs.bouwhulp.spook.alleen_kijken": "(alleen kijken)",
    "gui.guhs.bouwhulp.spook.maat": "%s×%s×%s, %s blokjes",
    "gui.guhs.bouwhulp.spook.status.0": "Een spook: schuif het op zijn plek en druk op Bouw!",
    "gui.guhs.bouwhulp.spook.status.1": "Aan het bouwen: %s gedaan, nog %s.",
    "gui.guhs.bouwhulp.spook.status.2": "Gestopt: %s gedaan, %s blokjes misten.",
    "gui.guhs.bouwhulp.spook.kort.0": "spook",
    "gui.guhs.bouwhulp.spook.kort.1": "bouwt",
    "gui.guhs.bouwhulp.spook.kort.2": "wacht",
    "gui.guhs.bouwhulp.spook.vooruit": "Vooruit (van jou af)",
    "gui.guhs.bouwhulp.spook.achteruit": "Achteruit (naar je toe)",
    "gui.guhs.bouwhulp.spook.links": "Naar links",
    "gui.guhs.bouwhulp.spook.rechts": "Naar rechts",
    "gui.guhs.bouwhulp.spook.hoger": "Een blokje hoger",
    "gui.guhs.bouwhulp.spook.lager": "Een blokje lager",
    "gui.guhs.bouwhulp.spook.draai": "↻ Draai",
    "gui.guhs.bouwhulp.spook.spiegel": "⇋ Spiegel",
    "gui.guhs.bouwhulp.spook.bouw": "Bouw!",
    "gui.guhs.bouwhulp.spook.pauze": "Pauze",
    "gui.guhs.bouwhulp.spook.terug": "↶ Terug (%s)",
    "gui.guhs.bouwhulp.spook.terug.uitleg": "Draait je laatste bouwactie terug (de laatste 5). Gebruikte blokjes komen terug, en wat er stond ook.",
    "gui.guhs.bouwhulp.spook.weg": "Weghalen",
    "gui.guhs.bouwhulp.spook.weg.uitleg": "Haalt het spook weg. Wat al gebouwd is, blijft staan.",
    "gui.guhs.bouwhulp.spook.tekort": "Je mist:",
    "gui.guhs.bouwhulp.spook.meer": "...en nog %s soorten",
    "gui.guhs.bouwhulp.spook.alles_er": "Alles is er, vahoeg!",
    "gui.guhs.bouwhulp.spook.alsnog.vraag": "Je hebt niet alle blokjes. Toch bouwen? Wat mist, blijft spook tot je het hebt.",
    "gui.guhs.bouwhulp.spook.alsnog.ja": "Ja, alsnog bouwen",
    "gui.guhs.bouwhulp.spook.alsnog.nee": "Nee, nog niet",
    "gui.guhs.bouwhulp.spook.vriend": "Naam van een vriend",
    "gui.guhs.bouwhulp.spook.vriend.erbij": "Deze speler mag ook aan het spook komen",
    "gui.guhs.bouwhulp.spook.vriend.eraf": "Deze speler mag er niet meer aan komen",
    "gui.guhs.bouwhulp.spook.helpers": "Helpen mee: %s",
    "gui.guhs.bouwhulp.spook.niemand": "Er is geen speler die %s heet.",
    "gui.guhs.bouwhulp.spook.niet_van_jou": "Dit spook is van %s.",
    "gui.guhs.bouwhulp.spook.te_ver": "Je bent te ver van het spook.",
    "gui.guhs.bouwhulp.spook.mist": "Klaar, maar %s blokjes misten: die blijven spook. Druk later nog eens op Bouw!",
    "gui.guhs.bouwhulp.spook.nee.leeg": "Er valt niets te bouwen.",
    "gui.guhs.bouwhulp.spook.nee.te_veel": "Je hebt al tien spoken open. Bouw of haal er eerst een weg.",
    "gui.guhs.bouwhulp.spook.nee.beschermd": "Hier mag niet gebouwd worden: dit is een beschermd gebouw.",
}


# ======================================================================================================================
# models
def _bd():
    sys.path.insert(0, HIER)
    sys.path.insert(0, os.path.dirname(HIER))
    try:
        from features import boerderij_dieren as bd
    except ImportError:
        import boerderij_dieren as bd
    return bd


GEEL = (250, 214, 102)
GEEL_DONKER = (214, 168, 64)
ROZE = (242, 140, 186)
ROZE_DONKER = (204, 96, 146)
HOUT = (150, 100, 60)
BLAUW = (78, 128, 196)


def bouwkeet(bd):
    """The Bouwkeet: a 48 x 48 hut (3 x 3 blocks) with a stepped pink roof (to y 40), guh ears, and a crane (bone kraan,
    with arm and haak) on top. Centred on x and z; the front is -z (north)."""
    bd.ATLAS, bd.S = 512, 2
    A = bd.Atlas(181)
    S = bd.S
    hout = bd.plain(HOUT, 8)
    roze = bd.plain(ROZE, 8)
    roze_d = bd.plain(ROZE_DONKER, 6)
    geel = bd.plain(GEEL, 6)
    ijzer = bd.plain((70, 70, 78), 5)
    weg = bd.transparent

    def planken(W, H, rng):
        """Horizontal yellow planks with seams, a few knots."""
        a = bd.plain(GEEL, 8)(W, H, rng).astype(np.float32)
        hoog = 5 * S
        for y in range(0, H, hoog):
            a[y:y + max(1, S // 2), :, :3] *= 0.7
            a[y:y + hoog, :, :3] *= 0.95 + 0.08 * rng.random()
        for _ in range(4):
            bd.disc(a, rng.uniform(4, W - 4), rng.uniform(4, H - 4), 1.0 * S, GEEL_DONKER, 0.8)
        return np.clip(a, 0, 255).astype(np.uint8)

    def raam(a, x0, y0, w, h, blauwdruk=True):
        """A window: a pink frame, glass with a blueprint behind it (white lines on blue)."""
        a[y0:y0 + h, x0:x0 + w, :3] = ROZE_DONKER
        a[y0 + S:y0 + h - S, x0 + S:x0 + w - S, :3] = BLAUW if blauwdruk else (170, 210, 236)
        if blauwdruk:
            for yy in range(y0 + 3 * S, y0 + h - 2 * S, 3 * S):
                a[yy:yy + max(1, S // 2), x0 + 2 * S:x0 + w - 2 * S, :3] = (226, 236, 250)
            a[y0 + 2 * S:y0 + h - 2 * S, x0 + w // 2:x0 + w // 2 + max(1, S // 2), :3] = (226, 236, 250)
        a[y0 + S:y0 + h - S, x0 + w // 2 - S // 2:x0 + w // 2 + S // 2, :3] = ROZE_DONKER
        a[y0 + h // 2 - S // 2:y0 + h // 2 + S // 2, x0 + S:x0 + w - S, :3] = ROZE_DONKER

    def voorkant(W, H, rng):
        """The front: planks, a pink door in the middle with a round guh face above it, two windows."""
        a = planken(W, H, rng).astype(np.float32)
        dw, dh = 12 * S, 20 * S
        dx = (W - dw) // 2
        a[H - dh:H, dx:dx + dw, :3] = ROZE
        a[H - dh:H, dx:dx + S, :3] = ROZE_DONKER
        a[H - dh:H, dx + dw - S:dx + dw, :3] = ROZE_DONKER
        a[H - dh:H - dh + S, dx:dx + dw, :3] = ROZE_DONKER
        bd.disc(a, dx + dw - 3 * S, H - dh / 2, 1.0 * S, (255, 226, 120))       # the door knob
        # the guh face above the door
        face = bd.guh_face(ROZE, eye_y=0.42, eye_dx=0.24, eye_r=0.16)(10 * S, 8 * S, rng)
        fx, fy = (W - 10 * S) // 2, H - dh - 9 * S
        a[fy:fy + 8 * S, fx:fx + 10 * S] = face
        raam(a, 3 * S, 8 * S, 10 * S, 10 * S)
        raam(a, W - 13 * S, 8 * S, 10 * S, 10 * S)
        return np.clip(a, 0, 255).astype(np.uint8)

    def zijkant(W, H, rng):
        """A side: planks, a window and plans pinned to the wall (a sheet with a little house drawn on it)."""
        a = planken(W, H, rng).astype(np.float32)
        raam(a, 4 * S, 8 * S, 12 * S, 10 * S)
        px, py = W - 18 * S, 6 * S
        a[py:py + 14 * S, px:px + 12 * S, :3] = (246, 242, 228)
        a[py + 6 * S:py + 12 * S, px + 3 * S:px + 9 * S, :3] = BLAUW                      # a little house
        for i in range(4):
            a[py + 6 * S - i * S:py + 6 * S - i * S + S, px + 3 * S + i * S:px + 9 * S - i * S, :3] = ROZE_DONKER
        bd.disc(a, px + 6 * S, py + S, 0.8 * S, (230, 60, 80))                          # a pin
        return np.clip(a, 0, 255).astype(np.uint8)

    def achterkant(W, H, rng):
        a = planken(W, H, rng).astype(np.float32)
        raam(a, (W - 14 * S) // 2, 8 * S, 14 * S, 10 * S, blauwdruk=False)
        return np.clip(a, 0, 255).astype(np.uint8)

    def kraanstreep(W, H, rng):
        """Crane steel: yellow with black stripes."""
        a = bd.plain(GEEL, 4)(W, H, rng).astype(np.float32)
        for y in range(0, H, 4 * S):
            a[y:y + 2 * S, :, :3] = (40, 36, 40)
        return np.clip(a, 0, 255).astype(np.uint8)

    keet = [
        bd.cube(A, [-24, 0, -24], [48, 2, 48], hout),                                               # the floor plinth
        bd.cube(A, [-22, 2, -22], [44, 30, 44], geel, {"north": voorkant, "south": achterkant, "east": zijkant, "west": zijkant}),
        bd.cube(A, [-23, 2, -23], [2, 30, 2], roze_d), bd.cube(A, [21, 2, -23], [2, 30, 2], roze_d),   # pink corner posts
        bd.cube(A, [-23, 2, 21], [2, 30, 2], roze_d), bd.cube(A, [21, 2, 21], [2, 30, 2], roze_d),
        bd.cube(A, [-7, 2, -24], [14, 1, 3], hout),                                                 # a doorstep
    ]
    dak = [
        bd.cube(A, [-26, 32, -26], [52, 3, 52], roze, {"down": roze_d}),
        bd.cube(A, [-20, 35, -20], [40, 3, 40], roze, {"down": roze_d}),
        bd.cube(A, [-13, 38, -13], [26, 2, 26], roze, {"down": roze_d}),
    ]
    oor = bd.round_ear(ROZE, (236, 98, 150))
    for sx in (-1, 1):
        dak.append(bd.cube(A, [sx * 17 - 4, 35, -19], [8, 8, 1.5], roze, {"north": oor, "south": oor}))
    kraan = [bd.cube(A, [-1.5, 40, -1.5], [3, 16, 3], kraanstreep, {"up": geel, "down": geel})]
    arm = [bd.cube(A, [-8, 55, -1.2], [32, 2.4, 2.4], kraanstreep, {"north": geel, "south": geel}),
           bd.cube(A, [-11, 53, -2.5], [5, 5, 5], ijzer)]                                           # the counterweight
    haak = [bd.cube(A, [21.75, 41, -0.25], [0.5, 14, 0.5], ijzer),                                  # the cable
            bd.cube(A, [20.5, 39.5, -1], [3, 1.5, 2], ijzer),
            bd.cube(A, [16, 36, -1.5], [12, 3, 3], hout)]                                           # the beam it hoists
    bones = [bd.bone("root", None, [0, 0, 0], []),
             bd.bone("keet", "root", [0, 0, 0], keet),
             bd.bone("dak", "keet", [0, 32, 0], dak),
             bd.bone("kraan", "dak", [0, 40, 0], kraan),
             bd.bone("arm", "kraan", [0, 55, 0], arm),
             bd.bone("haak", "arm", [22, 55, 0], haak)]
    g = bd.geo("bouwkeet", bones, 5, 5)
    anim = {"format_version": "1.8.0", "animations": {
        "animation.bouwkeet.rust": {"loop": True, "animation_length": 6.0, "bones": {
            "kraan": {"rotation": {"0.0": [0, 0, 0], "3.0": [0, 10, 0], "6.0": [0, 0, 0]}},
            "haak": {"position": {"0.0": [0, 0, 0], "3.0": [0, 0.6, 0], "6.0": [0, 0, 0]}}}},
        "animation.bouwkeet.bouwt": {"loop": True, "animation_length": 8.0, "bones": {
            "kraan": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, 180, 0], "8.0": [0, 360, 0]}},
            "haak": {"position": {"0.0": [0, 0, 0], "1.0": [0, 5, 0], "2.0": [0, 0, 0], "3.0": [0, 5, 0], "4.0": [0, 0, 0],
                                  "5.0": [0, 5, 0], "6.0": [0, 0, 0], "7.0": [0, 5, 0], "8.0": [0, 0, 0]}}}}}}
    return g, Image.fromarray(A.img), anim


# ======================================================================================================================
# icons (16 x 16)
PALET = {"k": (70, 40, 30, 255), "y": (250, 214, 102, 255), "d": (214, 168, 64, 255), "p": (242, 140, 186, 255),
         "q": (204, 96, 146, 255), "w": (255, 250, 240, 255), "b": (78, 128, 196, 255), "g": (130, 130, 140, 255),
         "z": (230, 236, 246, 255), "s": (190, 220, 240, 255), "o": (255, 236, 140, 255), "r": (230, 110, 60, 255),
         "h": (150, 100, 60, 255)}

KEET_ICOON = [
    "..........y.....",
    "..........y.....",
    ".........kyyyy..",
    "..........y..k..",
    "...ppppppppp.k..",
    "..pq.....q.pp...",
    ".ppppppppppppp..",
    ".kyyyyyyyyyyyk..",
    ".kybbykppkybbyk.",
    ".kybbykppkybbyk.",
    ".kyyyykppkyyyyk.",
    ".kyyyykpokyyyyk.",
    ".kyyyykppkyyyyk.",
    ".hhhhhhhhhhhhhh.",
    "................",
    "................",
]
ICONEN = {
    "bouwkeet_upgrade_guhrad": [            # a pink cog with a guh face
        "................",
        "......pp........",
        "...pp.pp.pp.....",
        "...ppppppppp....",
        "....pqqqqqp.....",
        "..ppqkqqkqpp....",
        "..ppqqqqqqpp....",
        "....pqqpqqp.....",
        "...ppppppppp....",
        "...pp.pp.pp.....",
        "......pp........",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "bouwkeet_upgrade_zout": [              # a spirit level with a salt crystal bubble
        "................",
        "................",
        "................",
        "................",
        "................",
        "kkkkkkkkkkkkkkkk",
        "kyyyyyysszyyyyyk",
        "kyyyyyszzsyyyyyk",
        "kyyyyyysszyyyyyk",
        "kkkkkkkkkkkkkkkk",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "bouwkeet_upgrade_saus": [              # a crane hook dripping saus
        "......gggg......",
        "......g..g......",
        ".......gg.......",
        ".......gg.......",
        ".......gg.......",
        ".......gg.......",
        ".....g.gg.......",
        "....gg.gg.......",
        "....gg..gg......",
        "....ggg.ggg.....",
        ".....gggggg.....",
        "......gggg......",
        "........r.......",
        "........r.......",
        ".......rrr......",
        "........r.......",
    ],
    "bouwkeet_upgrade_gloei": [             # a glowing work lamp with a pink cage
        "................",
        "......qqqq......",
        ".....q.oo.q.....",
        "....q.oooo.q....",
        "....qoowwooq....",
        "....q.oooo.q....",
        "....qoooooo.....",
        ".....qoooq......",
        "......qqq.......",
        "......kkk.......",
        "......kkk.......",
        ".....kkkkk......",
        "................",
        "................",
        "................",
        "................",
    ],
}


def icoon(rijen):
    img = Image.new("RGBA", (16, 16))
    for y, rij in enumerate(rijen):
        for x, ch in enumerate(rij):
            if ch in PALET:
                img.putpixel((x, y), PALET[ch])
    return img


# ======================================================================================================================
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def bestanden():
    """Every file: {path under src/main/resources: json object or PIL image}."""
    bd = _bd()
    oud = (bd.ATLAS, bd.S)
    try:
        g, tex, anim = bouwkeet(bd)
    finally:
        bd.ATLAS, bd.S = oud
    A, D = "assets/guhs", "data/guhs"
    uit = {
        f"{A}/geckolib/models/block/bouwkeet.geo.json": g,
        f"{A}/textures/block/bouwkeet.png": tex,
        f"{A}/geckolib/animations/block/bouwkeet.animation.json": anim,
        f"{A}/models/block/bouwkeet.json": {"textures": {"particle": "minecraft:block/yellow_terracotta"}},
        f"{A}/blockstates/bouwkeet.json": {"variants": {f"facing={f},snoet={s}": {"model": "guhs:block/bouwkeet"}
                                                        for f in ROT for s in ("slaapt", "werkt", "vol")}},
        f"{A}/textures/item/bouwkeet.png": icoon(KEET_ICOON),
        f"{A}/items/bouwkeet.json": {"model": {"type": "minecraft:model", "model": "guhs:item/bouwkeet"}},
        f"{A}/models/item/bouwkeet.json": {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/bouwkeet"}},
        f"{D}/loot_table/blocks/bouwkeet.json": {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": "guhs:bouwkeet"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
        # tier 0: everybody can make one (design 2.1: alles gewoon te craften)
        f"{D}/recipe/bouwkeet.json": _recept(["YPY", "PCP", "LKL"], {"Y": "minecraft:yellow_wool", "P": "minecraft:pink_wool",
                                                                     "C": "minecraft:crafting_table", "K": "guhs:kaas_knabbels",
                                                                     "L": "#minecraft:logs"}, "guhs:bouwkeet"),
        f"{D}/recipe/bouwkeet_upgrade_guhrad.json": _recept(["IDI", "DKD", "IRI"], {"I": "minecraft:iron_ingot", "D": "guhs:guh_wire",
                                                                                     "K": "guhs:kaas_knabbels", "R": "minecraft:redstone"},
                                                             "guhs:bouwkeet_upgrade_guhrad"),
        f"{D}/recipe/bouwkeet_upgrade_zout.json": _recept(["GGG", "ZKZ", "III"], {"G": "minecraft:glass", "Z": "guhs:zoutkristal",
                                                                                   "K": "guhs:kaas_knabbels", "I": "minecraft:iron_ingot"},
                                                           "guhs:bouwkeet_upgrade_zout"),
        f"{D}/recipe/bouwkeet_upgrade_saus.json": _recept(["XI ", " I ", "KCK"], {"X": "guhs:techquest_recept_machines",
                                                                                   "I": "minecraft:iron_ingot", "C": "minecraft:iron_chain",
                                                                                   "K": "guhs:kaas_knabbels"}, "guhs:bouwkeet_upgrade_saus"),
        f"{D}/recipe/bouwkeet_upgrade_gloei.json": _recept(["PGP", "GSG", "PIP"], {"P": "minecraft:pink_stained_glass",
                                                                                    "G": "minecraft:glowstone", "S": "guhs:gloeister",
                                                                                    "I": "minecraft:gold_ingot"}, "guhs:bouwkeet_upgrade_gloei"),
    }
    for naam in UPGRADES:
        uit[f"{A}/textures/item/{naam}.png"] = icoon(ICONEN[naam])
        uit[f"{A}/items/{naam}.json"] = {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}}
        uit[f"{A}/models/item/{naam}.json"] = {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{naam}"}}
    return uit


def _recept(patroon, sleutel, resultaat):
    return {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": patroon, "key": sleutel,
            "result": {"id": resultaat, "count": 1}}


def _tag(root, pad, waarden):
    """Adds values to a tag file (keeps what is there)."""
    vol = os.path.join(root, *pad.split("/"))
    data = {"replace": False, "values": []}
    if os.path.exists(vol):
        with open(vol, encoding="utf-8") as f:
            data = json.load(f)
    for v in waarden:
        if v not in data["values"]:
            data["values"].append(v)
    os.makedirs(os.path.dirname(vol), exist_ok=True)
    with open(vol, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


TAGS = [("data/minecraft/tags/block/mineable/axe.json", ["guhs:bouwkeet"]),
        ("data/guhs/tags/block/vadskracht.json", ["guhs:bouwkeet"])]


def build(h):
    for pad, obj in bestanden().items():
        if isinstance(obj, Image.Image):
            vol = os.path.join("src", "main", "resources", *pad.split("/"))
            os.makedirs(os.path.dirname(vol), exist_ok=True)
            obj.save(vol)
        else:
            base = h.A if pad.startswith("assets/") else h.D
            h.w(base + pad[pad.index("/", pad.index("/") + 1):], obj)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:bouwkeet"])
    h.add_tag("guhs/tags/block/vadskracht", ["guhs:bouwkeet"])
    for k, v in TEKSTEN.items():
        h.NL[k] = v


def _zelf():
    """On its own: write the files and merge the texts into nl_nl.json / en_us.json."""
    root = os.path.join("src", "main", "resources")
    alles = bestanden()
    for pad, obj in alles.items():
        vol = os.path.join(root, *pad.split("/"))
        os.makedirs(os.path.dirname(vol), exist_ok=True)
        if isinstance(obj, Image.Image):
            obj.save(vol)
        else:
            with open(vol, "w", encoding="utf-8", newline="\n") as f:
                json.dump(obj, f, indent=2, ensure_ascii=False)
                f.write("\n")
    for pad, waarden in TAGS:
        _tag(root, pad, waarden)
    en_pad = os.path.join(HIER, "..", "lang", "en", "c180_bouwhulp.json")
    with open(en_pad, encoding="utf-8") as f:
        en = json.load(f)
    for taal, teksten in (("nl_nl", TEKSTEN), ("en_us", {k: en.get(k, v) for k, v in TEKSTEN.items()})):
        p = os.path.join(root, "assets", "guhs", "lang", taal + ".json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(teksten)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    zonder = [k for k in TEKSTEN if k not in en]
    print(f"bouwhulp: {len(alles)} bestanden, {len(TEKSTEN)} teksten" + (f", {len(zonder)} zonder Engels: {zonder[:5]}" if zonder else ""))


if __name__ == "__main__":
    _zelf()
