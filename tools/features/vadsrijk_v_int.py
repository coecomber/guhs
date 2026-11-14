"""
1.10 "Het Vadsrijk", round U integration (V-INT): texts of the glue between the round-U slices (Java: feature/vadsrijk/vint):
the Knuffelbond + Mijn huis tabs of the Vadsrijk-menu, the HUD / Specialisatie sub-tabs, the spec line in the Karakter tab,
the menu key in the key overview.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_v_int.py
"""
import json
import os

V = "vadsrijk.vint."


def teksten():
    """key -> (nl, en)."""
    return {
        "vadsrijk.menu.tab.bond": ("Knuffelbond", "Knuffelbond"),
        "vadsrijk.menu.tab.bond.uitleg": ("Je bond (guild): leden, weektaken, perks en jullie kasteel", "Your bond (guild): members, weekly tasks, perks and your castle"),
        "vadsrijk.menu.tab.huis": ("Mijn huis", "My house"),
        "vadsrijk.menu.tab.huis.uitleg": ("Het Knuffelkwartier: je eigen huis in een buurtje", "The Knuffelkwartier: your own house in a neighbourhood"),
        V + "tab.hud": ("HUD aanpassen", "Edit HUD"),
        V + "tab.specs": ("Specialisatie", "Specialisation"),
        V + "karakter.spec": ("Specialisatie: %s", "Specialisation: %s"),
        V + "karakter.spec.geen": ("Specialisatie: nog niet gekozen (vanaf level 17)", "Specialisation: not chosen yet (from level 17)"),
        V + "toets.menu": ("Vadsrijk-menu (alle tabbladen)", "Vadsrijk menu (all tabs)"),
        V + "bond.titel": ("Je Knuffelbond", "Your Knuffelbond"),
        V + "bond.uitleg.1": ("Een Knuffelbond is een club van spelers. Samen verdien je bondsxp met alles wat je toch al doet: monsters, quests, levels en ambachten.",
                              "A Knuffelbond is a club of players. Together you earn bond xp with everything you already do: monsters, quests, levels and professions."),
        V + "bond.uitleg.2": ("Lid worden kan al vanaf zone 1: kijk op een Bondenbord in elke stad. Zelf een bond oprichten kan pas in Hoogvads, bij Bondsklerk Zegeltje.",
                              "You can join from zone 1: look at a bond board in every town. Founding your own bond is only possible in Hoogvads, at clerk Zegeltje."),
        V + "bond.uitleg.3": ("Elke bond krijgt een kasteel in de Bondenvallei, dat jullie samen uitbouwen (niveau 1 t/m 5), met een kluis, trofeeënzaal en werkplaatsen.",
                              "Every bond gets a castle in the Bondenvallei that you build up together (level 1 to 5), with a vault, trophy hall and workshops."),
        V + "bond.voet": ("Knoppen openen het bondspaneel in de chat (ook: /bond, /bc om te chatten)", "Buttons open the bond panel in the chat (also: /bond, /bc to chat)"),
        V + "buit.raak": ("Rijdier-drop: %s is van jou! (na %s keer proberen)", "Mount drop: %s is yours! (after %s tries)"),
        V + "buit.mis": ("Geen %s deze keer (%s%%) · pech %s/%s · nog %s keer tot een zekere drop", "No %s this time (%s%%) · pity %s/%s · %s more tries until a sure drop"),
        # 1.10.3 "pech zichtbaar": the drop chance + pity counter in the mount screen (vint.RijdierBuit.schermData)
        "vadsrijk.vint.buit.mis.zeker": ("Geen %s deze keer (%s%%) · pech %s/%s · de volgende keer is hij zeker van jou!", "No %s this time (%s%%) · pity %s/%s · next time it's guaranteed yours!"),
        "vadsrijk.vint.pech.kans": ("Kans: %s%% per %s · zeker na nog %s keer", "Chance: %s%% per %s · guaranteed after %s more tries"),
        "vadsrijk.vint.pech.zeker": ("Kans: %s%% per %s · zeker bij de volgende %s!", "Chance: %s%% per %s · guaranteed on the next %s!"),
        "vadsrijk.vint.pech.per.kill": ("kill", "kill"),
        "vadsrijk.vint.pech.per.clear": ("clear", "clear"),
        "vadsrijk.vint.pech.per.run": ("run", "run"),
        "vadsrijk.vint.pech.valt_bij": ("Valt bij: %s", "Drops from: %s"),
        "vadsrijk.vint.pech.waar.opperzwam": ("de Opperzwam (Zwamtempel, zone 1)", "the Overshroom (Shroom Temple, zone 1)"),
        "vadsrijk.vint.pech.waar.spiegelzucht": ("Spiegelzucht (De Spiegelburcht, zone 2)", "Mirrorsigh (the Mirror Keep, zone 2)"),
        "vadsrijk.vint.pech.waar.vahoeg": ("het einde van een Vahoeg-run (zone 3)", "the end of a Wahoog run (zone 3)"),
        "vadsrijk.vint.pech.waar.z6raid": ("de raid De Sterrendovende Toren (zone 6)", "the raid The Star-Snuffing Tower (zone 6)"),
        "vadsrijk.vint.pech.waar.grot": ("%s (eindkist)", "%s (end chest)"),
        "vadsrijk.vint.pech.mag_niet": ("Kans: %s%% per clear, maar pas als je een Pluimguh hebt.", "Chance: %s%% per clear, but only once you have a Plumeguh."),
        "vadsrijk.vint.pech.balk": ("Pech: %s/%s keer op rij mis", "Pity: %s/%s misses in a row"),
        "vadsrijk.vint.pech.verborgen": ("Zeldzame kleuren in de maak", "Rare colors on the way"),
        "vadsrijk.vint.pech.kort": ("%s: pech %s/%s · zeker na nog %s keer", "%s: pity %s/%s · guaranteed after %s more"),
        "vadsrijk.vint.pech.kort.zeker": ("%s: zeker bij de volgende!", "%s: guaranteed next time!"),
        # 1.10.3: the Bazen tab of the Vadsrijk-menu (bazenboek.Bazenboek, menu.client.BazenPaneel)
        "vadsrijk.menu.tab.bazen": ("Bazen", "Bosses"),
        "vadsrijk.menu.tab.bazen.uitleg": ("Alle bazen per zone: of je ze nu kunt bevechten, hoe vaak je ze versloeg, je buit en je pech-teller voor zeldzame drops", "Every boss per zone: whether you can fight it now, how often you beat it, your loot and your pity counter for rare drops"),
        "vadsrijk.bazenboek.naam.sterrenbeelden": ("De Sterrenbeelden", "The Constellations"),
        "vadsrijk.bazenboek.naam.sterrennacht": ("Bultguh in Sterrennacht-vacht", "Humpguh in Starry Night coat"),
        "vadsrijk.bazenboek.toegang.zone": ("%s is nog dicht voor je.", "%s is still closed for you."),
        "vadsrijk.bazenboek.toegang.dungeon": ("Via de dungeondeur van %s.", "Through the dungeon door of %s."),
        "vadsrijk.bazenboek.toegang.dungeon.wacht": ("Via de dungeondeur van %s · je moet nog %s wachten.", "Through the dungeon door of %s · you still have to wait %s."),
        "vadsrijk.bazenboek.toegang.ja": ("Toegankelijk.", "Accessible."),
        "vadsrijk.bazenboek.toegang.net_verslagen": ("Net verslagen: hij komt zo terug.", "Just defeated: it will be back soon."),
        "vadsrijk.bazenboek.toegang.terug": ("Verslagen: terug over %s.", "Defeated: back in %s."),
        "vadsrijk.bazenboek.toegang.er_nu": ("Hij is er nu! Ga erheen.", "It's there right now! Go get it."),
        "vadsrijk.bazenboek.toegang.quest": ("Alleen tijdens de quest %s.", "Only during the quest %s."),
        "vadsrijk.bazenboek.toegang.quest.klaar": ("Alleen tijdens de quest %s (die heb je al gedaan).", "Only during the quest %s (you already did it)."),
        "vadsrijk.bazenboek.toegang.quest.nu": ("Nu! Tijdens je quest %s.", "Now! During your quest %s."),
        "vadsrijk.bazenboek.toegang.tempel": ("Achter de tempeldeur: vanaf level %s (jij bent level %s), of al vanaf level %s met de verhaalquest van de Zwamtempel.", "Behind the temple door: from level %s (you are level %s), or from level %s with the Shroom Temple story quest."),
        "vadsrijk.bazenboek.toegang.spiegelburcht": ("Achter de deur van De Spiegelburcht: die gaat pas open als je de quest %s hebt (van Prisma op het voorplein).", "Behind the door of the Mirror Keep: it only opens once you have the quest %s (from Prisma on the forecourt)."),
        "vadsrijk.bazenboek.toegang.boormeester.slaapt": ("Slaapt: vind eerst de %s aanwijzingen in De Splinterpieken (%s/%s).", "Asleep: first find the %s clues in the Splinter Peaks (%s/%s)."),
        "vadsrijk.bazenboek.toegang.boormeester.wakker": ("Wakker: rechtsklik op de boormachine om het gevecht te starten.", "Awake: right-click the drilling machine to start the fight."),
        "vadsrijk.bazenboek.toegang.raid": ("Alleen in de raid De Sterrendovende Toren (met een groep).", "Only in the raid The Star-Snuffing Tower (with a group)."),
        "vadsrijk.bazenboek.toegang.kop.ja": ("✔ Nu te bevechten", "✔ Can be fought now"),
        "vadsrijk.bazenboek.toegang.kop.nee": ("✘ Nu niet te bevechten", "✘ Can't be fought right now"),
        "vadsrijk.bazenboek.drop.heb": ("✔ Al van jou", "✔ Already yours"),
        "vadsrijk.bazenboek.drop.rijdier": ("Rijdier: %s", "Mount: %s"),
        "vadsrijk.bazenboek.drop.huisdier": ("Huisdier: %s", "Pet: %s"),
        "vadsrijk.bazenboek.drop.eerste": ("Zeker bij je eerste kill", "Guaranteed on your first kill"),
        "vadsrijk.bazenboek.zone.dicht": ("Deze zone is nog dicht voor je:", "This zone is still closed for you:"),
        "vadsrijk.bazenboek.waar": ("%s · %s", "%s · %s"),
        "vadsrijk.bazenboek.soort.veld": ("Arenabaas", "Arena boss"),
        "vadsrijk.bazenboek.soort.verhaal": ("Verhaalgevecht", "Story fight"),
        "vadsrijk.bazenboek.soort.geheim": ("Geheime baas", "Secret boss"),
        "vadsrijk.bazenboek.soort.dungeon": ("Dungeonbaas", "Dungeon boss"),
        "vadsrijk.bazenboek.soort.raid": ("Raidbaas", "Raid boss"),
        "vadsrijk.bazenboek.lv": ("Level", "Level"),
        "vadsrijk.bazenboek.respawn": ("Komt terug na", "Respawns after"),
        "vadsrijk.bazenboek.kills": ("Verslagen", "Defeated"),
        "vadsrijk.bazenboek.oud": ("(voor 1.10.3 telde alleen je eerste kill: daarom de +)", "(before 1.10.3 only your first kill was counted: hence the +)"),
        "vadsrijk.bazenboek.snelst": ("Snelste kill", "Fastest kill"),
        "vadsrijk.bazenboek.eerste": ("Eerste keer geteld", "First counted"),
        "vadsrijk.bazenboek.schade": ("Totale schade", "Total damage"),
        "vadsrijk.bazenboek.heal": ("Totaal genezen", "Total healed"),
        "vadsrijk.bazenboek.tank": ("Totaal opgevangen", "Total tanked"),
        "vadsrijk.bazenboek.buit": ("Buit van deze baas", "Loot from this boss"),
        "vadsrijk.bazenboek.buit.niks": ("Nog niks gekregen.", "Nothing yet."),
        "vadsrijk.bazenboek.buit.munten": ("Brons", "Bronze"),
        "vadsrijk.bazenboek.buit.stukken": ("Uitrusting:", "Gear:"),
        "vadsrijk.bazenboek.buit.items": ("Andere spullen", "Other items"),
        "vadsrijk.bazenboek.zeldzaam": ("Zeldzame drops (pech-teller)", "Rare drops (pity counter)"),
        V + "z6raid.halster.al": ("Je hebt de Sterrennacht-bultguh al.", "You already have the Starry Night humpguh."),
        V + "z6raid.halster.ok": ("De Bultguh in Sterrennacht-vacht is van jou! (N = rijdieren)", "The Humpguh in Starry Night coat is yours! (N = mounts)"),
        V + "z6raid.halster.uitleg": ("Rechtsklik: de Bultguh in Sterrennacht-vacht (rijdier + vacht).", "Right-click: the Humpguh in Starry Night coat (mount + coat)."),
        "vadsrijk.uhud.deel.z6raid": ("Raid-frames (Sterrendovende Toren)", "Raid frames (Star-Dousing Tower)"),
        V + "z6raid.hud.voorbeeld": ("Raid-frames (alleen in de raid)", "Raid frames (only in the raid)"),
        V + "huis.titel": ("Mijn huis in het Knuffelkwartier", "My house in the Knuffelkwartier"),
        V + "huis.intro": ("Het Knuffelkwartier is een eigen buurtje met straatjes, buren, een plein en een markt. Je koopt er een kavel met een huisje dat je zelf inricht.",
                           "The Knuffelkwartier is a neighbourhood of its own with streets, neighbours, a square and a market. You buy a plot with a house you furnish yourself."),
        V + "huis.waar": ("Erheen: praat met de Kwartiermeester op het plein van elke hoofdstad (Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads).",
                          "Getting there: talk to the Kwartiermeester on the square of every main town (Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads)."),
    }


# V-INT: the zone-6 rows of the skill tree (klassen.spec.SpecBoom.zijdeRijen): <p>_z6_<kolom>_<rij>, rows 6-7 fill gaps, 8-9 are new
_KOLOM = {"r": ("Schildridder", "Stam", "Reuzenkussen", "Tank"), "j": ("Scherpschutter", "Stam", "Strikkenzetter", "Steun"),
          "m": ("Sterrenwever", "Stam", "Zegenzanger", "Heel")}
_RIJ = {6: ("Rozenzand", "Rose Sand"), 7: ("Palmwind", "Palm Wind"), 8: ("Sterrenzijde", "Star Silk"), 9: ("Moederoase", "Mother Oasis")}


def boom_teksten():
    t = {}
    for pre, kol in _KOLOM.items():
        for k, tak in enumerate(kol):
            for rij, (nl, en) in _RIJ.items():
                key = f"vadsrijk.klassen.knoop.{pre}_z6_{k}_{rij}"
                t[key] = (f"{nl} ({tak})", f"{en} ({tak})")
                lv = {6: 42, 7: 48, 8: 62, 9: 66}[rij]
                t[key + ".uitleg"] = (f"Een Zijdeland-knoop voor de tak {tak}: meer van je hoofdstat en de stats van deze tak. Vanaf level {lv}.",
                                      f"A Zijdeland node for the {tak} branch: more of your main stat and this branch's stats. From level {lv}.")
    return t


def build(h):
    for k, (nl, en) in {**teksten(), **boom_teksten()}.items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs", "lang")
    t = {**teksten(), **boom_teksten()}
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            raw = f.read()
        data = json.loads(raw)
        nieuw = {k: v[i] for k, v in t.items() if data.get(k) != v[i]}
        if not nieuw:
            continue
        # add-only, keeping the file's own formatting: insert before the closing brace
        regels = "".join(f',\n  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in nieuw.items()
                         if k not in data)
        if regels:
            raw = raw.rstrip()
            assert raw.endswith("}")
            raw = raw[:-1].rstrip() + regels + "\n}\n"
        for k, v in nieuw.items():
            if k in data:
                raise SystemExit(f"{k} exists with another text; update it by hand")
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write(raw)
    print(f"vadsrijk_v_int: {len(t)} texts")


if __name__ == "__main__":
    _direct()
