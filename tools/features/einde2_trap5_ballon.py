"""
einde2 phase C (CONTRACT_E2_B 7.12), slice trap5_ballon: tech tier 5's big project, the Vredesballon (DESIGN 6), its rows of
the FTB chapter Guh-technologie, and (in features/tech_quests.py, its FTB_SLOT only) the new open end: tier 6.
Java: nl.juiced.guhs.feature.einde2.ballon (E2BallonFeature; the line and the deliveries: Vredesballon.java).

  the line "vredesballon"   group techniek, after "werktuigguh" (trap5_machines). Steps: 0 not begun; 1..5 the five stages
                            (doek, mand, brander, knabbelvoorraad, versiering: FASEN, factory output, handed in at the
                            progress board on the building site, in portions); 6 ready; 7 it rose (the scene) = done.
  per player                each player sees their OWN balloon grow on the building site (the overlays laag_vredesballon_1..5
                            of the temple's build, shown through Schijn) and their own board (a text display only they see).
  the scene                 einde2_trap5_ballon_vredesballon: the launch, a flight over the temple, knabbels raining on the team.
  rewards                   the title Vredesbrenger, the statuette (block guhs:vredesballon), Heteluchtballon.geef (vetbootje's),
                            and every day after it a little bag of knabbels at the board.
  FTB                       one section "De Vredesballon" in guhs_techniek (FTB_SECTIES), its quests have no dependency on
                            another module's quest.

Dutch here; English: tools/lang/en/c101_e2_trap5_ballon.json. Looks: einde2_trap5_ballon_model.py. Wiki: einde2_trap5_ballon_wiki.py.
"""
import os
import re

import features.einde2_kit as kit
from features import einde2_trap5_ballon_model as model
from features import verhaal_motor, wereld

SLICE = "einde2_trap5_ballon"
LIJN = "vredesballon"
G = "gui.guhs.einde2_trap5_ballon."
SCENE = "einde2_trap5_ballon_vredesballon"
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "ballon")

FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [(SLICE, "De Vredesballon", f"geo:{model.NAAM}:{model.NAAM}", None)]

# the five stages: (Dutch name of the stage, [(counter name, Dutch name, amount), ...]); Java: Vredesballon.FASEN (selfcheck)
FASEN = [
    ("Het doek", [("tentdoek", "Tentdoek (elke kleur)", 128, "&dtentdoek&r (elke kleur)"), ("draad", "Guhdraad", 64, "&dGuhdraad&r")]),
    ("De mand", [("hout", "Boomstammen", 256, "&dboomstammen&r"), ("doosschil", "Doosschillen", 16, "&ddoosschillen&r")]),
    ("De brander", [("vuurkorf", "Vuurkorven", 2, "&dVuurkorven&r"), ("ontvanger", "Vadsontvangers", 1, "&dVadsontvanger&r"),
                    ("grillkool", "Grillkool", 96, "&dgrillkool&r"), ("kristalkaas", "Kristalkaas", 16, "&dkristalkaas&r")]),
    ("De knabbelvoorraad", [("knabbels", "Kaasknabbels", 512, "&dkaasknabbels&r"),
                            ("frituur", "Gefrituurde kaasknabbels", 128, "&dgefrituurde kaasknabbels&r")]),
    ("De versiering", [("lichtstaafjes", "Lichtstaafjes", 64, "&dlichtstaafjes&r"), ("flitskruimel", "Flitskruimels", 32, "&dflitskruimels&r")]),
]


def _breng(fase, ftb=True):
    """"Breng 128 tentdoek (elke kleur) en 64 Guhdraad" (ftb=False: without the FTB colour codes)."""
    delen = [f"{n} {zin if ftb else zin.replace('&d', '').replace('&r', '')}" for _, _, n, zin in FASEN[fase - 1][1]]
    return "Breng " + (", ".join(delen[:-1]) + " en " + delen[-1] if len(delen) > 1 else delen[0])


WAAR = "Zuidelijke Luchttempel: de bouwplaats achter de werkplaats"

TEKST = {
    # --- the board (a text display only its player sees) ---------------------------------------------------------------
    "bord.titel": "De Vredesballon",
    "bord.dicht": "Nog niet af, njeg!",
    "bord.begin": "Klik op het bord: de Werktuigguh heeft een plan.",
    "bord.fase": "Fase %1$s van 5: %2$s",
    "bord.regel": "%1$s: %2$s / %3$s",
    "bord.klaar": "Klaar om op te stijgen! Klik op het bord.",
    "bord.af": "De Vredesballon zweeft over het Guheinde.",
    "bord.zakje": "Er ligt een zakje knabbels voor je klaar!",
    "bord.morgen": "Morgen weer een zakje knabbels.",
    # --- the Werktuigguh (chat lines and the talking screen) -----------------------------------------------------------
    "wg.dicht": "Nog niet af, njeg! Eerst mijn werkplaats en mijn projecten. Daarna bouwen we samen iets heel groots.",
    "wg.roep": "Hé, daar! Kom eens bij het bord aan de muur. Ik heb een plan. Een plan met oortjes!",
    "wg.plan1": "Weet je wat ik vroeger deed? Ballonnen tekenen. Prachtige ballonnen, voor de Vuurnatie.",
    "wg.plan2": "Ze maakten er oorlogsballonnen van. Sindsdien hangen mijn oortjes een beetje als ik een ballon zie.",
    "wg.plan3": "Maar in jouw wereld staat nu een fabriek die nooit moe wordt. Dus bouwen we samen een ballon die alleen maar "
                "knabbels laat vallen. Een Vredesballon!",
    "wg.plan4": "Vijf fasen: doek, mand, brander, knabbelvoorraad en versiering. Jouw fabriek levert, ik naai en schroef. Breng "
                "alles naar dit bord. In porties mag, ik tel alles. Njeg!",
    "wg.dank": "Dank je! Weer %s stuks erbij.",
    "wg.vraag": "Fase %1$s van 5, %2$s. Ik heb nog nodig:",
    "wg.fase1": "Het doek! Zacht, sterk en een beetje kaasgeel. Ik heb het meteen aan de kraan gehangen.",
    "wg.fase2": "Een mand van echt hout, met doosschil in de bodem. Die breekt nooit, zelfs niet als er een vadsige guh in springt.",
    "wg.fase3": "De brander zit erin! Hij draait op vadskracht uit jouw eigen fabriek. Geen kolen, geen rook. En kijk: hij heeft "
                "een snoet. Hij is blij.",
    "wg.fase4": "Zakken vol knabbels. Ik heb er maar drie geproefd. Of vijf. Voor de kwaliteit, njeg.",
    "wg.fase5": "Oortjes erop, vlaggetjes eraan, een snoet op het doek... EUREKA! Hij is af!",
    "wg.klaar": "Hij is af. De brander snort, de touwen zijn strak, de zakken zitten vol. Durf jij mee de lucht in?",
    "wg.lanceren": "Lanceren!",
    "wg.wacht": "Nog even niet",
    "wg.wachten": "Geen haast. Een ballon wacht graag op een mooi moment.",
    "wg.af1": "Heb je ze gezien? Overal knabbels, overal blije oortjes. Zo hoort een ballon te zijn.",
    "wg.af2": "Hier, een beeldje voor op je kast. En je eigen kleine heteluchtballon, om rustig mee te zweven. Jij bent nu een "
              "echte Vredesbrenger.",
    "wg.zakje": "De Vredesballon is terug van zijn rondje. Er zat nog een zakje knabbels in de mand. Voor jou!",
    "wg.morgen": "Het zakje van vandaag heb je al. Morgen strooit hij weer, njeg.",
    "wg.kwijt": "Ben je je eigen kleine ballon kwijt?",
    "wg.kwijt_ja": "Ja, kwijt...",
    "wg.kwijt_nee": "Nee hoor",
    "wg.kwijt_nieuw": "Geen nood, ik had nog een reserve. De oude is lek, die zweeft nergens meer heen.",
    "wg.kwijt_nooit": "Je hebt nog geen ballonnetje van mij. Eerst de Vredesballon!",
    "wg.wijs_begin": "Mijn werkplaats is af, maar ik heb nog één plan. Een groot plan! Kom maar mee naar het bord op de bouwplaats.",
    "wg.wijs_fase": "Hoe gaat het met de ballon? We zitten bij fase %1$s, %2$s. Breng het maar naar het bord op de bouwplaats, njeg.",
    "wg.wijs_klaar": "Hij is af! Hij wacht op je bij het bord op de bouwplaats. Klik erop en dan gaan we de lucht in.",
    "wg.tip0": "Een tip: een Knutselmachine met een Bouwtekening van tentdoek naait sneller dan ik.",
    "wg.tip1": "Doosschil? Geef een Schuilguh een kaasknabbel, dan schuift hij er één naar buiten. Eén per dag, per Schuilguh.",
    "wg.tip2": "Lichtstaafjes maak je van gepofte knabbelkoorvruchten en een guhkristal. Flitskruimels laten Ender-Mika's vallen.",
    # --- the rest ----------------------------------------------------------------------------------------------------------
    "hint.bord": "Breng alles naar het bord op de bouwplaats. In porties mag.",
    "groeit": "De Vredesballon groeit!",
    "titel": "Nieuwe titel: %s! Kies hem in je Guhdex.",
    "fase.1": "Het doek", "fase.2": "De mand", "fase.3": "De brander", "fase.4": "De knabbelvoorraad", "fase.5": "De versiering",
}
for _f, (_naam, _dingen) in enumerate(FASEN, 1):
    for _id, _naam_ding, _n, _zin in _dingen:
        TEKST[f"levering.{_id}"] = _naam_ding

SCENE_REGELS = {
    "klaar": "Touwen los? Oortjes binnenboord? Dan gaan we. Brander aan!",
    "loeit": "De brander snort op vadskracht. Het doek bolt op, en bovenop gaan twee grote oren langzaam rechtop staan.",
    "vroeger": "Vroeger tekende ik ballonnen voor de oorlog. En kijk nou. Deze vliegt alleen maar voor de lol.",
    "luiken": "Boven de Zuidelijke Luchttempel gaan de luiken van de knabbelzakken open...",
    "regen": "Het regent knabbels! VAHOEG!",
    "sokka": "Twaalf! Nee, dertien! Momo, die was van mij!",
    "toph": "Ik zie er niks van, maar ik voel overal kruimels. Beste regen ooit.",
    "zuka": "Een ballon zonder één enkel kanon. Oom zou er meteen thee bij zetten.",
    "slot": "Weet je wat het mooiste is aan een ballon? Er is altijd nog een plek verder om naartoe te zweven.",
    "einde": "En zo dreef de Vredesballon over het Guheinde. Overal waar hij kwam, regende het knabbels. Njeg.",
}
SCENE_NAMEN = {"werktuigguh": "De Werktuigguh", "aang": "Aang-guh", "sokka": "Sokka-guh", "toph": "Toph-guh", "zuka": "Zuka"}


def teksten(h):
    for k, v in TEKST.items():
        kit.lang(h, G + k, v)
    # the statuette, the actor, the title
    kit.lang(h, "block.guhs.vredesballon", "Vredesballon-beeldje")
    kit.lang(h, "block.guhs.vredesballon.lore", "De Vredesballon, maar dan klein. Klik erop en hij strooit een kruimel.")
    kit.lang(h, "block.guhs.vredesballon.klik", "Pff! Er dwarrelt een kruimeltje knabbel uit het mandje.")
    kit.lang(h, f"entity.guhs.{model.NAAM}", "De Vredesballon")
    kit.lang(h, "gui.guhs.titel.vredesbrenger", "Vredesbrenger")
    kit.lang(h, "gui.guhs.titels.hint.vredesbrenger", "Bouw samen met de Werktuigguh iets dat alleen maar knabbels laat vallen.")
    # the line
    stappen = [("Een plan met oortjes", "Klik op het bord op de bouwplaats achter de werkplaats van de Werktuigguh.", WAAR)]
    for f, (naam, _) in enumerate(FASEN, 1):
        stappen.append((naam, _breng(f, False) + " naar het bord op de bouwplaats.", WAAR))
    stappen.append(("Opstijgen!", "De Vredesballon is af. Klik op het bord en stijg op!", WAAR))
    verhaal_motor.verhaallijn(h, LIJN, "De Vredesballon",
                              "Het grote project van de Werktuigguh: een reusachtige ballon die knabbels over het Guheinde strooit. Jouw "
                              "fabriek levert, hij naait en schroeft. Iedereen bouwt zijn eigen ballon.",
                              stappen, klaar=("De Vredesballon zweeft. Kom elke dag terug bij het bord voor een zakje knabbels.", WAAR),
                              kort={"0": "Naar het bord op de bouwplaats", "1": "Vredesballon: het doek", "2": "Vredesballon: de mand",
                                    "3": "Vredesballon: de brander", "4": "Vredesballon: de knabbels", "5": "Vredesballon: de versiering",
                                    "6": "Laat de Vredesballon opstijgen!"})
    verhaal_motor.scene(h, SCENE, "De Vredesballon stijgt op", SCENE_REGELS, SCENE_NAMEN)


def advancements(h):
    kit.verborgen(h, "einde2_trap5_ballon_beeldje")
    kit.verborgen(h, "einde2_trap5_ballon_dagelijks")
    kit.zichtbaar(h, "einde2_trap5_ballon_vredesbrenger", "guheinde_winst", "guhs:vredesballon", "challenge", "Vredesbrenger",
                  "Laat samen met de Werktuigguh de Vredesballon opstijgen boven het Guheinde.")


def blok(h):
    h.self_drop("vredesballon")
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:vredesballon"])


def ftb(fq):
    q, adv = fq.q, fq.adv
    breng = {f: _breng(f) for f in range(1, 6)}
    wereld.ftb_questlijn(fq, SLICE, LIJN, [
        ("Een plan met oortjes",
         "De &dWerktuigguh&r heeft nog één groot plan. Op de &6bouwplaats&r achter zijn werkplaats in de Zuidelijke Luchttempel hangt "
         "een bord: klik erop. Iedereen bouwt zijn eigen ballon, en alleen jij ziet de jouwe groeien.", "guhs:vredesballon"),
        ("Het doek", breng[1] + ". Tip: een Knutselmachine met een Bouwtekening van tentdoek.", "guhs:campingmarkt_tentdoek_geel"),
        ("De mand", breng[2] + ". Tip: een bos in een bak, en een &dSchuilguh&r schuift je elke dag een doosschil toe voor een "
                               "kaasknabbel.", "minecraft:oak_log"),
        ("De brander", breng[3] + ". De brander draait op &bvadskracht&r uit je eigen fabriek: geen kolen, geen rook.", "guhs:grillkool"),
        ("De knabbelvoorraad", breng[4] + ". Tip: de frituurlijn en een akker die zichzelf oogst.", "guhs:kaas_knabbels"),
        ("De versiering", breng[5] + ". Dan zet de Werktuigguh er oortjes op.", "guhs:lichtstaafje"),
        ("Opstijgen!", "Klik op het bord en laat de &dVredesballon&r opstijgen. Je krijgt het &dVredesballon-beeldje&r, je eigen "
                       "&dheteluchtballon&r en de titel &6Vredesbrenger&r.", "guhs:vredesballon")],
        na=["intro_guhs_techniek"], eind=(("guhs:kaas_knabbels", 32),))
    klaar = f"{SLICE}_{LIJN}_7"
    q(f"{SLICE}_beeldje", "Een ballon op de kast",
      "Zet het &dVredesballon-beeldje&r neer waar iedereen het ziet. Klik erop: er dwarrelt een kruimeltje uit het mandje.",
      "guhs:vredesballon", [adv("einde2_trap5_ballon_beeldje")], deps=[klaar], shape="diamond")
    q(f"{SLICE}_zweven", "Zelf zweven",
      "Zet je eigen &dheteluchtballon&r op de grond en stap in. Houd springen ingedrukt: de brander loeit en je stijgt. De wind doet "
      "de rest. Kwijt? Sluip en klik op het bord van de bouwplaats.",
      "guhs:heteluchtballon", [adv("einde2_vetbootje_ballon")], deps=[klaar], shape="circle")
    q(f"{SLICE}_zakje", "Elke dag knabbelregen",
      "De Vredesballon maakt elke dag een rondje boven het Guheinde. Kom daarna langs bij het bord op de bouwplaats: er zit nog een "
      "zakje knabbels in de mand, speciaal voor jou.",
      "guhs:kaas_knabbels", [adv("einde2_trap5_ballon_dagelijks")], deps=[klaar], shape="circle")
    q(f"{SLICE}_titel", "Vredesbrenger",
      "Kies de titel &6Vredesbrenger&r in je Guhdex. Wie had gedacht dat een fabriek zoveel blije oortjes kon maken?",
      "guhs:guhdex", [adv("guhs:guheinde/einde2_trap5_ballon_vredesbrenger")], deps=[klaar], shape="rsquare")


# =====================================================================================================================
# the self-check: the amounts of the Java side, the model, the files
# =====================================================================================================================
def _java_fasen():
    pad = os.path.join(JAVA, "Vredesballon.java")
    if not os.path.exists(pad):
        return None
    tekst = open(pad, encoding="utf-8").read()
    return [(m.group(1), int(m.group(2))) for m in re.finditer(r'[Ll]evering\("([a-z_]+)",\s*"[^"]+",\s*(\d+)', tekst)]


def selfcheck(h):
    p = list(model.controle())
    java = _java_fasen()
    py = [(n, a) for _, dingen in FASEN for n, _, a, _ in dingen]
    if java is None:
        p.append("Vredesballon.java not found")
    elif java != py:
        p.append(f"the deliveries differ: Java {java}, python {py}")
    for f in (f"{h.A}/geckolib/models/entity/{model.NAAM}.geo.json", f"{h.A}/textures/entity/{model.NAAM}.png",
              f"{h.A}/models/block/vredesballon.json", f"{h.D}/advancement/quest/{LIJN}_stap_7.json"):
        if not os.path.exists(f):
            p.append(f"missing {f}")
    if p:
        raise SystemExit("einde2_trap5_ballon: " + "; ".join(p))


def build(h):
    model.build(h)
    teksten(h)
    advancements(h)
    blok(h)
    selfcheck(h)
