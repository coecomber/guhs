"""
bbq2 (bestaand): two questlines in the two EXISTING buildings of the Guhbarbecuether (DESIGN_130 par. 3; Java: feature/bestaand).
Nothing here is worldgen: the NPCs and their props come through Bezetting, so they also appear at the Spiesburchten and
Mika-grillpaleizen that were generated long ago (the official server world is never reset).

  - the Wachter-guh (NPC wachterguh) in his wachthokje next to the statue of every Spiesburcht: "De wacht bij de Spiesburcht"
    (Verhaallijn wachter, 5 steps): light the four bridge fires with his loaned aansteekspies, weed the Mikakruid out of the
    pindasaus-tuintje. Reward: the recipe card of the Zielig lantaarntje (the soul lantern parody), two lantaarntjes and the
    outfit (wachtershelm with a saté plume, wachtersmantel);
  - the Knuffelmaker-guh (NPC knuffelmakerguh) in his naaihoek on the first floor of every Mika-grillpaleis: "De gestolen
    knuffels" (Verhaallijn knuffelmaker, 3 steps): free the three plush guhs from their cages (pay the ransom of one vahoege
    vads per cage, as the old sign there already said, or sneak and pick the lock while no Nether-Mika looks), bring thread.
    Reward: the knuffelpatroon (recipe card) and the three plush deco blocks.
  - Every fire, weed and plush is PER PLAYER: the world never changes. A player who lit a fire sees it burn (a block state
    shown to that player only, Java Schijn), everybody else still finds it cold and can do the quest. So an unlimited number
    of players can do both questlines at the same building at the same time.
  - The empty throne sign of the grillpaleis stays as it is: the Grote Nether-Mika lives in the castle of Super Guhrio.

Parts: bestaand_tex.py (textures, block models), bestaand_bouw.py (the props, the spots, the test rooms, the geometry check),
bestaand_wiki.py (the wiki entries). build(h) makes everything.
"""
import os

import numpy as np

from features import bbq2, bestaand_bouw, bestaand_tex, verhaal_motor, wereld

# the sections of this module in the chapter guhs_barbecuether (make_ftbquests.py FTB_SECTIES)
FTB_SECTIES = [
    ("bestaand_wachter", "De wacht bij de Spiesburcht", "npc:wachterguh",
     ["bestaand_wachter_1", "bestaand_wachter_2", "bestaand_wachter_3", "bestaand_wachter_4", "bestaand_wachter_5", "bestaand_lantaarntje",
      "bestaand_wachterspak"]),
    ("bestaand_knuffelmaker", "De gestolen knuffels", "npc:knuffelmakerguh",
     ["bestaand_knuffelmaker_1", "bestaand_knuffelmaker_2", "bestaand_knuffelmaker_3", "bestaand_knuffels", "bestaand_sluiper"]),
]

VUREN, KRUID, KOOIEN, DRAAD = 4, 6, 3, 4

# =====================================================================================================================
# the outfit (marker <bestaand> in GuhClothes, source "bestaand")
# =====================================================================================================================
_H = [0, 6, -2]
BONES = {
    # the wachtershelm: a dark iron dome with a rim and a nose guard, and a saté skewer as its plume
    "outfit_wachtershelm": ("head", _H, "wachtershelm", [([-5.4, 14.4, -11.4], [10.8, 2.6, 10.0], 0), ([-4.4, 17.0, -10.4], [8.8, 1.0, 8.0], 0),
                                                        ([-6.0, 14.4, -12.0], [12.0, 0.5, 11.2], 0), ([-0.6, 11.8, -12.3], [1.2, 2.8, 0.5], 0)]),
    "outfit_wachtershelm_spies": ("head", _H, "wachtershelm_spies", [([-0.3, 18.0, -6.8], [0.6, 4.6, 0.6], 0)]),
    "outfit_wachtershelm_vlees": ("head", _H, "wachtershelm_vlees", [([-1.0, 18.5, -7.5], [2.0, 1.2, 2.0], 0), ([-0.9, 20.1, -7.4], [1.8, 1.1, 1.8], 0),
                                                                    ([-0.7, 21.6, -7.2], [1.4, 0.9, 1.4], 0)]),
}
CLOTHES = ["bestaand_wachtershelm", "bestaand_wachtersmantel"]
IJZER = (74, 72, 84)
ROET = (52, 48, 56)
GLOED = (236, 110, 40)
ROZE = (255, 150, 200)


def _mantel(rng, v):
    """Charcoal wool with a glowing ember hem and a pink guh face on the back."""
    a = v.fabric(ROET, rng, 8)
    px = a.shape[0]
    a[px - 4:, :] = GLOED
    a[px - 5, ::2] = (255, 200, 90)
    m = px // 2
    for y in range(px):
        for x in range(px):
            u, w = x - m + 0.5, y - m + 2.5
            if u * u + w * w < 30:                                   # the round face
                a[y, x] = ROZE
    for (ex, ey) in ((m - 6, m - 9), (m + 3, m - 9)):                  # two ears on top
        a[ey:ey + 3, ex:ex + 3] = ROZE
    for ex in (m - 3, m + 1):                                         # eyes
        a[m - 4:m - 2, ex:ex + 2] = (40, 24, 40)
    a[m, m - 1:m + 1] = (214, 70, 140)                                # nose
    return np.clip(a, 0, 255)


def clothes(rng, v):
    return {
        "bestaand_wachtershelm": {"wachtershelm": lambda: v.metal(IJZER, rng),
                                  "wachtershelm_spies": lambda: v.fabric((190, 150, 96), rng, 6),
                                  "wachtershelm_vlees": lambda: v.fabric((176, 96, 54), rng, 16)},
        "bestaand_wachtersmantel": {"cape": lambda: _mantel(rng, v)},
    }


HELM_ICON = ["................", ".......s........", "......vvv.......", ".......s........", "......vvv.......", ".......s........",
             "....aaaaaaaa....", "...abbbbbbbba...", "..abbbbbbbbbba..", "..abbbbccbbbba..", "..abbbbccbbbba..", ".aaaaaaccaaaaaa.",
             ".......cc.......", ".......cc.......", "................", "................"]


def icons(ic):
    return {
        "bestaand_wachtershelm": ic.icon(HELM_ICON, {"a": (40, 38, 48), "b": IJZER, "c": (110, 108, 122), "s": (190, 150, 96), "v": (176, 96, 54)}),
        "bestaand_wachtersmantel": ic.shaped("cape", (28, 24, 32), ROET, GLOED),
    }


# =====================================================================================================================
# the two NPCs (sitting guhs with their own bones; beroepen_tex._npc paints the swatches)
# =====================================================================================================================
def npcs(h):
    from features import beroepen_tex
    H = [0, 13, 0]
    B = [0, 12, -4]

    def embleem(block):
        """A pink guh face on the breastplate."""
        block[..., :3] = IJZER
        for yy in range(32):
            for xx in range(32):
                if (xx - 15.5) ** 2 + (yy - 17.5) ** 2 < 110:
                    block[yy, xx, :3] = ROZE
        block[2:8, 5:11, :3] = ROZE
        block[2:8, 21:27, :3] = ROZE
        block[13:17, 9:13, :3] = (40, 24, 40)
        block[13:17, 19:23, :3] = (40, 24, 40)
        block[20:23, 14:18, :3] = (214, 70, 140)

    def stippen(block):
        """A red pincushion with white dots."""
        for yy in range(2, 32, 8):
            for xx in range(2 + (yy // 8 % 2) * 4, 32, 8):
                block[yy:yy + 3, xx:xx + 3, :3] = (250, 244, 236)

    def lint(block):
        """A yellow measuring tape: a mark every few pixels."""
        for xx in range(0, 32, 4):
            block[:, xx, :3] = (60, 40, 30)
        for xx in range(0, 32, 16):
            block[:, xx:xx + 2, :3] = (200, 40, 40)

    # the Wachter-guh: a sooty grey guh in a dark iron helmet with a nose guard and a saté plume, a breastplate with a pink
    # guh face, and a tall grill skewer as his halberd
    beroepen_tex._npc(h, "wachterguh", 0.93, 0.34, 0.86, {
        "wachter_helm": (("head", H), [([-7.4, 25.0, -7.8], [14.8, 0.9, 14.0], "helm", 0), ([-6.4, 25.9, -6.4], [12.8, 3.6, 11.6], "helm", 0),
                                       ([-5.0, 29.5, -5.0], [10.0, 1.0, 8.8], "helm", 0), ([-0.8, 20.6, -8.3], [1.6, 5.2, 0.7], "helm", 0),
                                       ([-7.6, 21.2, -6.0], [0.8, 4.4, 4.0], "helm", 0), ([6.8, 21.2, -6.0], [0.8, 4.4, 4.0], "helm", 0)]),
        "wachter_pluim": (("head", H), [([-0.4, 30.5, -1.0], [0.8, 7.2, 0.8], "stokje", 0), ([-1.6, 31.4, -2.2], [3.2, 1.8, 3.2], "vlees", 0),
                                        ([-1.4, 33.6, -2.0], [2.8, 1.6, 2.8], "vlees", 0), ([-1.1, 35.6, -1.7], [2.2, 1.3, 2.2], "vlees", 0)]),
        "wachter_harnas": (("body", B), [([-5.3, 2.0, -4.9], [10.6, 9.0, 1.0], "harnas", 0), ([-2.4, 4.0, -5.3], [4.8, 4.8, 0.5], "embleem", 0),
                                         ([-5.6, 9.6, -5.0], [11.2, 1.4, 9.4], "harnas", 0)]),
        "wachter_spies": (("body", B), [([6.6, 0.0, -6.2], [0.9, 24.0, 0.9], "steel", 0), ([6.2, 24.0, -6.6], [1.7, 1.2, 1.7], "punt", 0),
                                        ([6.7, 25.2, -6.1], [0.7, 2.6, 0.7], "punt", 0), ([5.9, 18.0, -6.9], [2.3, 2.0, 2.3], "vlees", 0)]),
    }, {"helm": (IJZER, 8, None), "stokje": ((190, 150, 96), 5, None), "vlees": ((176, 96, 54), 14, None), "harnas": ((86, 84, 98), 8, None),
        "embleem": (IJZER, 3, embleem), "steel": ((120, 78, 46), 6, None), "punt": ((170, 172, 186), 5, None)}, 13031)
    # the Knuffelmaker-guh: a warm pink guh with a pincushion on his head, little round glasses, a measuring tape round his
    # neck, a cream apron with a heart, and a half-finished plush guh under his arm
    beroepen_tex._npc(h, "knuffelmakerguh", 0.92, 0.9, 1.0, {
        "knuffelmaker_kussen": (("head", H), [([-3.2, 25.4, -3.6], [6.4, 2.6, 6.4], "kussen", 0), ([-1.9, 28.0, -1.5], [0.4, 2.4, 0.4], "speld", 0),
                                              ([1.2, 28.0, 0.6], [0.4, 2.0, 0.4], "speld", 0), ([-0.4, 28.0, -2.6], [0.4, 1.6, 0.4], "speld", 0),
                                              ([-2.2, 30.4, -1.8], [1.0, 1.0, 1.0], "knop", 0), ([0.9, 30.0, 0.3], [1.0, 1.0, 1.0], "knop2", 0),
                                              ([-0.7, 29.6, -2.9], [1.0, 1.0, 1.0], "knop", 0)]),
        "knuffelmaker_bril": (("head", H), [([-5.2, 18.6, -7.6], [3.8, 0.6, 0.5], "bril", 0), ([-5.2, 21.4, -7.6], [3.8, 0.6, 0.5], "bril", 0),
                                            ([-5.2, 19.2, -7.6], [0.6, 2.2, 0.5], "bril", 0), ([-2.0, 19.2, -7.6], [0.6, 2.2, 0.5], "bril", 0),
                                            ([1.4, 18.6, -7.6], [3.8, 0.6, 0.5], "bril", 0), ([1.4, 21.4, -7.6], [3.8, 0.6, 0.5], "bril", 0),
                                            ([1.4, 19.2, -7.6], [0.6, 2.2, 0.5], "bril", 0), ([4.6, 19.2, -7.6], [0.6, 2.2, 0.5], "bril", 0),
                                            ([-1.4, 20.2, -7.6], [2.8, 0.6, 0.5], "bril", 0)]),
        "knuffelmaker_schort": (("body", B), [([-4.0, 2.0, -4.7], [8.0, 7.0, 0.8], "schort", 0), ([-1.4, 4.2, -5.0], [2.8, 2.4, 0.4], "hart", 0),
                                              ([-4.2, 8.6, -4.7], [1.0, 3.0, 0.7], "schort", 0), ([3.2, 8.6, -4.7], [1.0, 3.0, 0.7], "schort", 0)]),
        "knuffelmaker_meetlint": (("body", B), [([-5.6, 10.6, -5.0], [11.2, 0.9, 9.6], "lint", 0), ([-5.0, 3.6, -5.4], [1.0, 7.0, 0.5], "lint", 0),
                                                ([4.0, 5.6, -5.4], [1.0, 5.0, 0.5], "lint", 0)]),
        "knuffelmaker_knuffel": (("body", B), [([5.2, 1.0, -7.4], [3.6, 3.4, 3.2], "pluche", 0), ([4.9, 4.4, -7.7], [4.2, 3.6, 3.8], "pluche", 0),
                                               ([4.9, 8.0, -6.4], [1.4, 1.4, 1.0], "pluche", 0), ([7.7, 8.0, -6.4], [1.4, 1.4, 1.0], "pluche", 0),
                                               ([5.6, 5.8, -7.9], [0.7, 0.7, 0.3], "oogje", 0), ([7.7, 5.8, -7.9], [0.7, 0.7, 0.3], "oogje", 0)]),
    }, {"kussen": ((214, 50, 70), 8, stippen), "speld": ((200, 204, 214), 3, None), "knop": ((250, 220, 90), 4, None), "knop2": ((120, 200, 250), 4, None),
        "bril": ((70, 50, 60), 3, None), "schort": ((250, 240, 222), 6, None), "hart": ((236, 70, 130), 4, None), "lint": ((250, 214, 70), 3, lint),
        "pluche": ((255, 196, 222), 10, None), "oogje": ((40, 24, 40), 2, None)}, 13032)
    for key, nl in NPC_TEXTS.items():
        h.lang(key, nl, nl)


NPC_TEXTS = {
    "entity.guhs.guh_npc.wachterguh": "Wachter-guh",
    "entity.guhs.guh_npc.knuffelmakerguh": "Knuffelmaker-guh",
}


# =====================================================================================================================
# loot, recipes, tags
# =====================================================================================================================
KNUFFEL_WOL = {"knuffelguh": "minecraft:pink_wool", "knuffelmika": "minecraft:black_wool", "knuffelrookguh": "minecraft:light_gray_wool"}


def data(h):
    for naam in KNUFFEL_WOL:
        h.self_drop(f"bestaand_{naam}")
    h.self_drop("bestaand_zielig_lantaarntje")
    # (the fire bowl and the weed never drop: quest props; no loot tables: the blocks say noLootTable)
    h.shaped("bestaand_zielig_lantaarntje", ["NNN", "DGR", "NNN"],
             {"N": "minecraft:iron_nugget", "D": "minecraft:light_blue_dye", "G": "guhs:gloeikoolgruis", "R": "guhs:bestaand_recept_lantaarn"},
             "guhs:bestaand_zielig_lantaarntje", 2)
    for naam, wol in KNUFFEL_WOL.items():
        h.shaped(f"bestaand_{naam}", [" W ", "WSW", "WRW"], {"W": wol, "S": "minecraft:string", "R": "guhs:bestaand_recept_knuffel"},
                 f"guhs:bestaand_{naam}")
    h.add_tag("guhs/tags/item/loaned", ["guhs:bestaand_aansteekspies"])
    h.add_tag("guhs/tags/item/bestaand_losgeld", ["guhs:vahoege_vads", "guhs:vahoege_vads_ingot"])
    h.add_tag("guhs/tags/item/bestaand_aanstekers", ["guhs:bestaand_aansteekspies", "guhs:aanmaakblokje", "minecraft:flint_and_steel",
                                                    "minecraft:fire_charge"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:bestaand_zielig_lantaarntje"])


# =====================================================================================================================
# texts
# =====================================================================================================================
W = "quest.guhs.bestaand.wachter."
K = "quest.guhs.bestaand.knuffelmaker."
G = "gui.guhs.bestaand."
TEXTS = {
    # blocks and items
    "block.guhs.bestaand_vuurkorf": "Brugvuurkorf",
    "block.guhs.bestaand_mikakruid": "Mikakruid",
    "block.guhs.bestaand_zielig_lantaarntje": "Zielig lantaarntje",
    "block.guhs.bestaand_zielig_lantaarntje.lore": "Het kijkt altijd een beetje zielig. Aai het (rechtsklik) en het knapt helemaal op. Njeg!",
    "block.guhs.bestaand_knuffelguh": "Knuffelguh",
    "block.guhs.bestaand_knuffelguh.lore": "Met liefde genaaid door de Knuffelmaker-guh. Knijp er maar in (rechtsklik)!",
    "block.guhs.bestaand_knuffelmika": "Knuffel-Mika",
    "block.guhs.bestaand_knuffelmika.lore": "Kijkt gemeen, voelt zacht. Jat gegarandeerd geen knabbels.",
    "block.guhs.bestaand_knuffelrookguh": "Knuffel-Rookguh",
    "block.guhs.bestaand_knuffelrookguh.lore": "Een zielig rookwolkje om tegenaan te slapen. Hoeft niet gevoerd.",
    "item.guhs.bestaand_aansteekspies": "Aansteekspies van de Wachter-guh",
    "item.guhs.bestaand_aansteekspies.lore": "Geleend. Rechtsklik er een koude brugvuurkorf mee: fwoesj!",
    "item.guhs.bestaand_recept_lantaarn": "Lantaarnrecept van de Wachter-guh",
    "item.guhs.bestaand_recept_lantaarn.lore": "Hiermee maak je Zielige lantaarntjes. Het recept blijft gewoon in je werkbank liggen.",
    "item.guhs.bestaand_recept_knuffel": "Knuffelpatroon",
    "item.guhs.bestaand_recept_knuffel.lore": "Het patroon van de Knuffelmaker-guh: wol, draad en een beetje liefde. Blijft in je werkbank liggen.",
    "item.guhs.bestaand_wachtershelm": "Wachtershelm",
    "item.guhs.bestaand_wachtersmantel": "Wachtersmantel",
    # the sign in front of the naaihoek (written by the Mikas)
    "sign.guhs.bestaand.naai1": "NAAIHOEK",
    "sign.guhs.bestaand.naai2": "Sokken stoppen!",
    "sign.guhs.bestaand.naai3": "Pauze = njeg",
    "sign.guhs.bestaand.naai4": "- de Mika's",
    # the bridge fires
    G + "richting.oost": "oost", G + "richting.zuid": "zuid", G + "richting.west": "west", G + "richting.noord": "noord",
    G + "vuur.aan": "FWOESJ! Het brugvuur van de %s-brug brandt weer (%s/%s).",
    G + "vuur.al_aan": "Dit brugvuur brandt al. Lekker warm, njeg!",
    G + "vuur.spies": "Zonder vlammetje geen vuur: rechtsklik de vuurkorf met de aansteekspies van de Wachter-guh.",
    G + "vuur.uit": "Een koude vuurkorf. De Wachter-guh in de hal van de Spiesburcht weet er vast meer van.",
    G + "vuur.alle": "Alle vier de brugvuren branden! Ga het de Wachter-guh vertellen.",
    # the weeds
    G + "kruid.weg": "Rrrts! Mikakruid eruit (%s/%s). Het siste nog even vals.",
    G + "kruid.alle": "Het tuintje is weer netjes! De pindascheutjes halen opgelucht adem. Ga terug naar de Wachter-guh.",
    # the cages
    G + "kooi.onbekend": "Gestolen knuffelguhs... De gevangen Knuffelmaker-guh op deze verdieping weet er vast meer van.",
    G + "kooi.slot": "De kooi zit op slot. Schuif een vahoege vads door de tralies, of sluip (bukken) en peuter het slot los als geen Mika kijkt.",
    G + "kooi.peuter": "Peuter, peuter... (%s/%s) Blijf bukken, njeg!",
    G + "kooi.betaald": "De Mika's grissen je vads weg en kijken heel opvallend de andere kant op. De knuffel wurmt zich tussen de tralies door! (%s/%s)",
    G + "kooi.open": "Klik! Het slot springt open en de knuffel wurmt zich tussen de tralies door! (%s/%s)",
    G + "kooi.betrapt": "HÉ! AFBLIJVEN, NJEG! Een Nether-Mika heeft je gezien en geeft je een duw.",
    G + "kooi.al_vrij": "Deze kooi is leeg: jouw knuffel is al terug bij de Knuffelmaker-guh.",
    G + "kooi.alle": "Alle drie de knuffels zijn vrij! Ga naar de Knuffelmaker-guh.",
    # the lantern
    G + "lantaarntje.blij": "Het lantaarntje knapt helemaal op van je aai. Njeg!",
    G + "lantaarntje.zielig": "Het lantaarntje kijkt weer een beetje zielig. Zo hoort het ook.",
    # what the Guhdex says you need / get
    G + "nodig.vuren": "Brandende brugvuren",
    G + "nodig.kruid": "Gewied Mikakruid",
    G + "nodig.knuffels": "Bevrijde knuffels",
    G + "beloning.wachterspak": "Het wachterspak (helm en mantel)",
    G + "beloning.knuffels": "Drie knuffels: guh, Mika en Rookguh",
    # the op command
    G + "commando.stand": "Wachter: stap %s (vuren %s, kruid %s). Knuffelmaker: stap %s (kooien %s).",
    G + "commando.gewist": "De questlijnen van de Wachter-guh en de Knuffelmaker-guh zijn gewist voor %s.",
    # --- the Wachter-guh ---
    W + "hallo": "HALT! Wie daar?! ... O, bezoek. Sorry, njeg: beroepsdeformatie. Ik ben de Wachter-guh en ik bewaak dit beeld al "
                 "sinds het hier nog lauw was. Maar kijk nou toch: de vier brugvuren zijn uit! Zonder vuur ziet geen guh waar de brug "
                 "ophoudt en de frituursaus begint.",
    W + "wat": "De Vonk-Mika's! Ze blazen ze uit, voor de lol. 'Dan branden WIJ het felst,' zeggen ze. Opscheppers. En ik mag mijn "
               "post niet verlaten: wie past er anders op het beeld? Nou? Precies.",
    W + "start": "Vahoeg! Hier, mijn aansteekspies. Op elke brug staat een vuurkorf op een sokkel: vier bruggen, vier vuren. "
                 "Rechtsklik ze met de spies. En niet in de saus vallen, njeg!",
    W + "hint.vuren": "steek de vier brugvuren aan: op elke brug van de Spiesburcht staat een vuurkorf (rechtsklik met de aansteekspies)",
    W + "vuren_nog": "Nog %s van de %s brugvuren. Ik zie het vanaf hier: daar is het zo donker als een aangebrande knabbel.",
    W + "spies_kwijt": "Mijn aansteekspies kwijt? Njeg... gelukkig heb ik er een heel rek van. Hier.",
    W + "brug_dicht": "Op één brug is de vuurkorf zoek (ingemetseld? opgegeten?). Die reken ik goed, njeg.",
    W + "vuren_klaar": "Ze branden! Alle vier! Ik zie de bruggen weer, en de bruggen zien mij. Vahoeg! Maar nu ik toch iemand heb "
                       "die WEL van zijn post af mag... het pindasaus-tuintje boven staat vol Mikakruid. Dat zaaien de Mika's, uit "
                       "pure njeg. Trek jij het eruit?",
    W + "tuin_start": "Het tuintje is één trap omhoog, midden in de burcht. Mikakruid herken je meteen: paars, prikkerig en het "
                      "kijkt je vals aan. Ik tel %s pollen. Gewoon eruit trekken: klik erop!",
    W + "hint.tuin": "wied de pollen Mikakruid in het pindasaus-tuintje (één trap omhoog, midden in de Spiesburcht): klik erop",
    W + "tuin_nog": "Nog %s pollen Mikakruid in het tuintje. De pindascheutjes durven er niet meer langs te groeien.",
    W + "klaar": "Vier vuren aan, tuintje netjes: zó hoort een burcht! Jij bent vanaf nu ere-wachter, njeg. Hier: mijn reservehelm "
                 "(die met de satépluim), een wachtersmantel en het recept van mijn lantaarntje. Het kijkt altijd een beetje "
                 "zielig, maar als je het aait knapt het op. Net als ik.",
    W + "bedankt0": "De vuren branden nog, hoor. Ik kijk elke vijf minuten. Nou ja, ik kijk altijd. Ik ben een wachter.",
    W + "bedankt1": "Weet je wat ik hier bewaak? Het beeld. En weet je wat het beeld doet? Niks. Beste collega die ik ooit had, njeg.",
    W + "bedankt2": "Als de Vonk-Mika's weer komen blazen, blaas ik terug. Vahoeg hard.",
    W + "recept_kwijt": "Het recept kwijt? Een wachter bewaart alles dubbel. Hier, njeg!",
    W + "optie.help": "Ik steek ze weer aan!",
    W + "optie.wat": "Hoe zijn ze uitgegaan?",
    W + "optie.wieden": "Ik ga wieden!",
    W + "optie.dank": "Dankjewel, Wachter-guh!",
    # --- the Knuffelmaker-guh ---
    K + "hallo": "Psst! Hé, jij daar! Niet zo hard... Ik ben de Knuffelmaker-guh. De Mika's hebben me meegenomen, mét al mijn "
                 "knuffels. Nu moet ik de hele dag Mika-sokken stoppen. SOKKEN! En mijn knuffelguhs zitten in die kooien, helemaal "
                 "alleen. Help je ze eruit?",
    K + "waarom": "Weglopen? De deur staat open, ja. Maar ik ga niet zonder mijn knuffels, njeg! En eerlijk... de Grote Nether-Mika "
                  "is hier nooit. Die woont in zijn eigen kasteel, ergens in de sauszee. Hier past alleen een bordje op zijn troon.",
    K + "start": "Drie kooien, drie knuffels. Het losgeld is één vahoege vads per knuffel: rechtsklik de kooi met een vahoege vads "
                 "(of een vadsstaaf) in je hand. Geen vads? Sluip dan (bukken) naar een kooi en peuter het slot los als geen Mika "
                 "kijkt. Betrapt? Dan krijg je een duw. Meer niet, njeg.",
    K + "hint.kooien": "bevrijd de drie knuffels uit de kooien op deze verdieping: betaal een vahoege vads per kooi, of sluip (bukken) en "
                       "peuter het slot los als geen Mika kijkt",
    K + "kooien_nog": "Nog %s van de %s knuffels zitten vast. Ik hoor ze piepen, njeg. Nou ja, knuffels piepen niet. Maar ik HOOR het.",
    K + "kooi_weg": "Eén kooi is al kapot (wie doet zoiets?). Die knuffel is vast ontsnapt: die reken ik goed.",
    K + "draad": "Ze zijn er alle drie! Kom hier, mijn pluizige vadsjes... O nee. De Mika's hebben eraan getrokken: alle naadjes "
                 "zijn los. Breng me %s draad, dan naai ik ze weer dicht. Nether-Mika's ruilen het soms voor een vadsstaaf, de dieven.",
    K + "hint.draad": "breng draad naar de Knuffelmaker-guh (Nether-Mika's ruilen draad voor een vahoege-vadsstaaf)",
    K + "draad_tekort": "Ik zie %s van de %s draad. Zonder garen geen knuffel, njeg.",
    K + "klaar": "Prik, prik, klaar! Zo goed als nieuw. Weet je wat? Neem jij er drie mee, ik heb er nog zat in mijn hoofd. En hier "
                 "is mijn knuffelpatroon: daarmee naai je ze zelf, zoveel je wilt. Ik blijf nog even. De Mika's hebben óók een "
                 "knuffel nodig. Die vooral.",
    K + "bedankt0": "Sinds jij langs bent geweest, naai ik stiekem oortjes aan alle Mika-sokken. Ze hebben nog niks door, njeg.",
    K + "bedankt1": "Een knuffel is pas af als hij een beetje scheef kijkt. Dat is geen fout, dat is karakter.",
    K + "bedankt2": "De Grote Nether-Mika? Nooit gezien. Zijn troon staat hier maar te staan. Ze zeggen dat hij een eigen kasteel "
                    "heeft, in de sauszee.",
    K + "patroon_kwijt": "Mijn patroon kwijt? Ik teken zo een nieuwe. Hier, njeg!",
    K + "optie.help": "Ik bevrijd ze!",
    K + "optie.waarom": "Waarom loop je niet gewoon weg?",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def verhaallijnen(h):
    burcht = "De hal van de Spiesburcht (Guhbarbecuether)"
    verhaal_motor.verhaallijn(
        h, "wachter", "De wacht bij de Spiesburcht",
        "De Wachter-guh bewaakt het beeld in de Spiesburcht, maar de brugvuren zijn uit en zijn tuintje staat vol Mikakruid.",
        stappen=[
            ("Maak kennis met de Wachter-guh", "Praat met de Wachter-guh. Hij staat in zijn wachthokje naast het beeld, in de hal van een Spiesburcht.",
             "Een Spiesburcht in de Guhbarbecuether (Superkompas: Barbecue)"),
            ("Steek de vier brugvuren aan", "Op elke brug van de Spiesburcht staat een koude vuurkorf op een sokkel. Rechtsklik ze alle vier met de "
                                            "aansteekspies.", "De vier bruggen van de Spiesburcht"),
            ("Meld je bij de Wachter-guh", "De vuren branden! Ga terug naar de Wachter-guh in de hal.", burcht),
            ("Wied het pindasaus-tuintje", "Trek de pollen Mikakruid uit het pindasaus-tuintje: klik erop. Het tuintje ligt één trap omhoog, "
                                           "midden in de burcht.", "Het pindasaus-tuintje van de Spiesburcht"),
            ("Haal je beloning", "Alles is weer netjes. Ga terug naar de Wachter-guh.", burcht),
        ],
        klaar=("De vuren branden en het tuintje is netjes. Jij bent ere-wachter, njeg!", burcht))
    paleis = "De eerste verdieping van het Mika-grillpaleis (Guhbarbecuether)"
    verhaal_motor.verhaallijn(
        h, "knuffelmaker", "De gestolen knuffels",
        "De Mika's van het grillpaleis hebben de Knuffelmaker-guh meegenomen, mét al zijn knuffelguhs.",
        stappen=[
            ("Vind de Knuffelmaker-guh", "Praat met de gevangen Knuffelmaker-guh. Hij zit in zijn naaihoek op de eerste verdieping van een "
                                         "Mika-grillpaleis.", "Een Mika-grillpaleis in de Guhbarbecuether (Superkompas: Barbecue)"),
            ("Bevrijd de drie knuffels", "Open de drie kooien: betaal een vahoege vads per kooi, of sluip (bukken) en peuter het slot los als "
                                         "geen Mika kijkt.", paleis),
            ("Breng draad voor de naadjes", "De knuffels zijn stuk getrokken. Breng draad naar de Knuffelmaker-guh.", "De naaihoek in het Mika-grillpaleis"),
        ],
        klaar=("De knuffels zijn vrij en weer heel. Vahoeg!", paleis))


def advancements(h):
    bbq2.verborgen(h, "bestaand_gesloten")        # (a cage picked open by sneaking: the FTB task of "Op kousenvoetjes")
    z = bbq2.zichtbaar
    z(h, "barbecuether", "bestaand_brugvuren", "spiesburcht", "minecraft:campfire", "task", "Fwoesj!",
      "Steek alle vier de brugvuren van een Spiesburcht weer aan")
    z(h, "barbecuether", "bestaand_wachter", "bestaand_brugvuren", "guhs:bestaand_zielig_lantaarntje", "goal", "Ere-wachter",
      "Help de Wachter-guh van de Spiesburcht: vier vuren aan, tuintje netjes")
    z(h, "barbecuether", "bestaand_sluiper", "grillpaleis", "minecraft:tripwire_hook", "task", "Op kousenvoetjes",
      "Peuter een knuffelkooi open zonder dat een Nether-Mika het ziet")
    z(h, "barbecuether", "bestaand_knuffelmaker", "grillpaleis", "guhs:bestaand_knuffelguh", "goal", "Knuffels horen bij guhs",
      "Bevrijd de gestolen knuffels voor de Knuffelmaker-guh in het Mika-grillpaleis")


# =====================================================================================================================
# the self-check
# =====================================================================================================================
BLOKKEN = ["bestaand_vuurkorf", "bestaand_mikakruid", "bestaand_zielig_lantaarntje", "bestaand_knuffelguh", "bestaand_knuffelmika",
           "bestaand_knuffelrookguh"]
ITEMS = ["bestaand_aansteekspies", "bestaand_recept_lantaarn", "bestaand_recept_knuffel"]
BLOK_ITEMS = ["bestaand_zielig_lantaarntje", "bestaand_knuffelguh", "bestaand_knuffelmika", "bestaand_knuffelrookguh"]


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for b in BLOKKEN:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS + BLOK_ITEMS:
        if not os.path.exists(f"{A}/models/item/{i}.json"):
            missing.append(f"item model {i}")
    for i in ITEMS:
        for key in (f"item.guhs.{i}", f"item.guhs.{i}.lore"):
            if key not in h.NL:
                missing.append(f"lang {key}")
    for b in BLOK_ITEMS:
        if not os.path.exists(f"{D}/loot_table/blocks/{b}.json") or not os.path.exists(f"{D}/recipe/{b}.json"):
            missing.append(f"loot table / recipe of {b}")
    for kind in ("wachterguh", "knuffelmakerguh"):
        for p in (f"{A}/geckolib/models/entity/guh_npc_{kind}.geo.json", os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            if not os.path.exists(p):
                missing.append(p)
    for name in ["bestaand_wachthokje", "bestaand_naaihoek", "bestaand_test_brug", "bestaand_test_burcht", "bestaand_test_paleis"] + \
                [f"bestaand_brugvuur_{k}" for k in range(4)]:
        if not os.path.exists(f"{D}/structure/{name}.nbt"):
            missing.append(f"template {name}")
    for key in TEXTS:
        if key not in h.NL:
            missing.append(f"lang {key}")
    if missing:
        raise SystemExit(f"bestaand assets missing: {missing}")


def build(h):
    bestaand_tex.build(h)
    npcs(h)
    data(h)
    texts(h)
    verhaallijnen(h)
    advancements(h)
    bestaand_bouw.build(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_barbecuether: one section per building, the NPC as its portrait)
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    wereld.ftb_questlijn(fq, "bestaand", "wachter", [
        ("De Wachter-guh", "In de hal van elke &6Spiesburcht&r staat naast het beeld een wachthokje. Daarin staat de &6Wachter-guh&r: helm op, "
                           "spies in de poot. Zeg eens hallo (hij schrikt een beetje).", "guhs:gebeitelde_houtskoolsteen_stenen"),
        ("Vier brugvuren", "De Vonk-Mika's hebben de brugvuren uitgeblazen! Op elke brug staat een vuurkorf op een sokkel. Rechtsklik ze alle "
                           "vier met de &6aansteekspies&r van de Wachter-guh. Jouw vuren blijven voor jou branden.", "guhs:bestaand_aansteekspies"),
        ("Melden bij de wachter", "Alle vier de vuren branden. Ga het de Wachter-guh vertellen: hij heeft nog een klusje.", "minecraft:campfire"),
        ("Mikakruid wieden", "Het &dpindasaus-tuintje&r (één trap omhoog, midden in de burcht) staat vol paars, prikkerig &5Mikakruid&r. Klik op "
                             "elke pol om hem eruit te trekken.", "guhs:pindascheutjes"),
        ("Ere-wachter", "Vuren aan, tuintje netjes. Haal bij de Wachter-guh je beloning: het &blantaarnrecept&r, twee Zielige lantaarntjes en "
                        "het wachterspak voor je guh.", "guhs:bestaand_recept_lantaarn"),
    ], na=["sb_burcht"], eind=(("guhs:gegrilde_kaasknabbelsate", 4),))
    q("bestaand_lantaarntje", "Zielig lantaarntje", "Met het &blantaarnrecept&r in je werkbank (het blijft liggen): 6 ijzerklompjes, gloeikoolgruis "
      "en lichtblauwe verf. Het lantaarntje kijkt zielig, tot je het aait (rechtsklik).", "guhs:bestaand_zielig_lantaarntje",
      [item("guhs:bestaand_zielig_lantaarntje")], rewards=(("guhs:gloeikoolgruis", 4),), deps=["bestaand_wachter_5"])
    q("bestaand_wachterspak", "Het wachterspak", "Een ijzeren helm met een satépluim en een roetzwarte mantel met een gloeiende zoom. Houd ze "
      "vast (rechtsklik ingedrukt) om ze te ontgrendelen voor de kledingkast.", "guhs:bestaand_wachtershelm",
      [item("guhs:bestaand_wachtershelm"), item("guhs:bestaand_wachtersmantel")], rewards=(("guhs:kaas_knabbels", 8),), deps=["bestaand_wachter_5"])
    wereld.ftb_questlijn(fq, "bestaand", "knuffelmaker", [
        ("De gevangen Knuffelmaker-guh", "Op de eerste verdieping van elk &cMika-grillpaleis&r, naast de kooien met gestolen knuffelguhs, zit de "
                                         "&dKnuffelmaker-guh&r in zijn naaihoek. Hij moet Mika-sokken stoppen. Praat met hem (zachtjes).",
         "minecraft:loom"),
        ("Drie knuffels bevrijden", "Open de drie kooien. Het losgeld is &6één vahoege vads&r per knuffel (rechtsklik de kooi ermee). Of &7sluip&r "
                                    "(bukken) en peuter het slot los als geen Nether-Mika kijkt: drie keer klikken. Betrapt? Dan krijg je alleen "
                                    "een duw. Een Sluipknabbeldrankje helpt!", "guhs:roosterijzer_tralies"),
        ("Draad voor de naadjes", "De Mika's hebben aan de knuffels getrokken. Breng de Knuffelmaker-guh &f4 draad&r: je krijgt drie knuffels en "
                                  "zijn &dknuffelpatroon&r.", "minecraft:string"),
    ], na=["sb_paleis"], eind=(("guhs:kaas_knabbels", 16),))
    q("bestaand_knuffels", "Een bed vol knuffels", "Met het &dknuffelpatroon&r in je werkbank naai je ze zelf: 5 wol (roze, zwart of lichtgrijs) "
      "en een draad. Verzamel de Knuffelguh, de Knuffel-Mika en de Knuffel-Rookguh. Knijp er maar in!", "guhs:bestaand_knuffelguh",
      [item("guhs:bestaand_knuffelguh"), item("guhs:bestaand_knuffelmika"), item("guhs:bestaand_knuffelrookguh")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["bestaand_knuffelmaker_3"])
    q("bestaand_sluiper", "Op kousenvoetjes", "Peuter een knuffelkooi open &7zonder te betalen&r en zonder dat een Nether-Mika je ziet.",
      "minecraft:tripwire_hook", [adv("bestaand_gesloten")], rewards=(("guhs:sluipknabbeldrankje", 1),), deps=["bestaand_knuffelmaker_1"],
      shape="diamond")
