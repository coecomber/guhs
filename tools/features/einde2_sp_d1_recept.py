"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.5), slice sp_d1_recept: part 1 steps 7-10 of sponsguh_recept: Plan Z, the sneak through De
Vetemmer (wink 6), the recipe back, the drum in the Bellenkom and the song; part 1's rewards.
Java: nl.juiced.guhs.feature.einde2.spd1recept (E2SpD1Recept, D1rScenes, Sluipen, Trommel, D1rActeurs, D1rCommando).
Chunk c114_e2_sp_d1_recept (English: tools/lang/en/c114_e2_sp_d1_recept.json).

  lang   gui.guhs.verhalen.sponsguh_recept.{stap,nu,waar,kort}.<7..10>   the step texts (STAPPEN / EXTRA / KORT below: the skelet's
         (+ 8_b, 10_b, klaar)                                             einde2_sp_verhaal writes them with the line)
         gui.guhs.verhalen.sponsguh_recept.beloning.*                     the Guhdex rewards of part 1 (the recipe, the burgers)
         gui.guhs.einde2_sp_d1_recept.*                                   hints, titles, the action bar of the two minigames
         quest.guhs.einde2_sp_d1_recept.*                                 chat: the minigames' rules, Karen, Plankmika, the clicks
         scene.guhs.einde2_sp_d1_recept_*                                 the six scenes and the four look scenes (NOT the song's
                                                                          lyrics: scene...zoete_zege.lied.* are sp_audio's)
  ftb    einde2_sp_d1_recept_r7 .. r10 (the skelet's section einde2_sp_deel1 names them); r10 = the gear of part 1

Comedy, no quote of the show: the narrator with a French touch, Plankmika, Karen, Octo's sighs; the one literal line is the
DESIGN's own ("Bedankt Karen, mijn computervrouw.", CONTRACT_E2_C 5.3).
"""
import os
import re

from features import einde2_kit as kit
from features import verhaal_motor

LIJN = "sponsguh_recept"
SLICE = "einde2_sp_d1_recept"
SCENE = "einde2_sp_d1_recept_"
P = "gui.guhs.einde2_sp_d1_recept"
Q = "quest.guhs.einde2_sp_d1_recept"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True

# =====================================================================================================================
# the step texts (what to do, where, a direction and a visible landmark: playtest 1)
# =====================================================================================================================
STAPPEN = {LIJN: {
        7: ("Plan Z",
            "De alarmbel van De Krokante Knabbel gaat! Ren naar Meneer Krabguh: zijn kantoor is achter in het restaurant, de deur "
            "rechts achter de kassa. Er is iets vreselijks gebeurd met zijn kluis.",
            "Het kantoor van Meneer Krabguh, achter in De Krokante Knabbel (aan de Hoofdstraat, oost van het plein)"),
        8: ("Inbreken in De Vetemmer",
            "Plankmika heeft het geheime recept gestolen! Steek de straat over naar De Vetemmer, de grote grijze emmer tegenover De "
            "Krokante Knabbel. Achterin, in het lab, ligt het recept in een kluisje. Blijf uit Karens camera's (de gele vlekken op "
            "de vloer) en loop pas door een rode laser als hij uit is.",
            "De Vetemmer: de grote grijze emmer tegenover De Krokante Knabbel"),
        9: ("Het recept terug",
            "Je hebt het recept! Breng het naar Meneer Krabguh, in zijn kantoor achter in De Krokante Knabbel, aan de overkant van "
            "de straat.",
            "Meneer Krabguh, in zijn kantoor achter in De Krokante Knabbel"),
        10: ("De Bellenkom",
            "Vanavond is het feest in de Bellenkom, het grote ronde stadion in het noorden van de stad (volg de Bellenlaan vanaf het "
            "plein). Octo moet optreden en jij bent zijn trommelaar! Klim via het trapje aan de zijkant op het podium en ga achter "
            "de trommel staan.",
            "Het podium van de Bellenkom, het stadion in het noorden (de Bellenlaan op)"),
}}
EXTRA = {LIJN: {
        "8_b": ("Je hebt het recept! Plankmika denkt dat je een klant bent en komt eraan met het menu. Ren terug naar buiten, "
                "dezelfde weg die je kwam: door de gang en de lege emmer naar de straat.",
                "De voordeur van De Vetemmer, aan de straat"),
        "10_b": ("Sla op de trommel als een bel erop knapt: klik precies op het moment dat de ring van bellen de trommel raakt. "
                 "Tien van de zestien is genoeg, en missen kost niks: Octo telt gewoon opnieuw af.",
                 "Achter je trommel op het podium van de Bellenkom"),
        "klaar": ("Het geheime recept is terug en Octo is (één avond lang) een ster! Meneer Krabguh heeft binnenkort groot nieuws...",
                  "De Krokante Knabbel"),
}}
KORT = {LIJN: {
        "7": "Naar Krabguh's kantoor",
        "8": "Sluip De Vetemmer in",
        "8_b": "Ren naar buiten!",
        "9": "Recept naar Krabguh",
        "10": "Naar het podium",
        "10_b": "Trommel op de maat!",
        "klaar": "Op naar deel 2",
}}

# =====================================================================================================================
# hints, titles, the minigames (gui.guhs.einde2_sp_d1_recept.*): %s = the direction phrase ("in het oosten, ...")
# =====================================================================================================================
GUI = {
    "hint.7": "De alarmbel! Meneer Krabguh roept je: zijn kantoor is achter in De Krokante Knabbel, %s. Loop naar binnen, langs de kassa, de deur achterin.",
    "hint.8": "De Vetemmer is de grote grijze emmer aan de overkant van de straat, %s. Sponsguh houdt de wacht bij de deur.",
    "hint.9": "Breng het recept naar Meneer Krabguh: zijn kantoor achter in De Krokante Knabbel, %s.",
    "hint.10": "Naar de Bellenkom, het ronde stadion in het noorden, %s. Je trommel staat op het podium; het trapje is aan de zijkant.",
    "sluip.titel": "Sluip naar het recept!",
    "sluip.onder": "Geel = Karen kijkt. Rood = laser. Bukken = stiller.",
    "sluip.rustpunt": "Rustpunt %s van %s: hier begin je weer als Karen je ziet.",
    "sluip.bijna": "Karen ziet je bijna%s Uit de gele vlek!",
    "sluip.stand": "Sluipen: rustpunt %s van %s  ·  Karen zag je %s keer",
    "weg.titel": "Wegwezen!",
    "weg.onder": "Plankmika komt eraan met het menu: ren naar buiten!",
    "trommel.titel": "Trommel op de maat!",
    "trommel.onder": "Klik op de trommel als de bel knapt: %s van de %s.",
    "trommel.nu": "Nu: let op de bellen!",
    "trommel.stand": "Raak %s / %s  ·  Bel %s / %s",
    "trommel.bijna": "Bijna!",
    "trommel.bijna_onder": "%s van de %s. Octo telt opnieuw af...",
    "trommel.weg": "De band wacht op je: ga weer achter je trommel staan.",
    "trommel.gewonnen": "Wat een ritme!",
    "trommel.gewonnen_onder": "%s van de %s bellen raak!",
}
BELONING = {
    f"gui.guhs.verhalen.{LIJN}.beloning.recept": "Het geheime Knabbelburger-recept: de grill bakt voortaan voor jou",
    f"gui.guhs.verhalen.{LIJN}.beloning.burgers": "Acht Knabbelburgers, vers van de grill",
}

# =====================================================================================================================
# chat (quest.guhs.einde2_sp_d1_recept.*)
# =====================================================================================================================
CHAT = {
    "sluip.regel": "Sluipen: blijf uit de gele vlekken op de vloer (daar kijkt een camera van Karen) en loop alleen door een rode laser "
                   "als hij uit is. Bukken maakt je stiller: dan ziet een camera je pas van dichtbij. Gezien? Dan sta je gewoon weer bij "
                   "het laatste rustpunt.",
    "sluip.troost": "(Niks aan de hand: je staat weer bij het laatste rustpunt. Kijk hoe de camera heen en weer draait, en ga als hij de "
                    "andere kant op kijkt.)",
    "sluip.gezien.0": "Bliep. Ik zie iets bewegen, schat. Ach, het waait. Terug naar je plekje.",
    "sluip.gezien.1": "Bliep. Een indringer? Nee hoor, gewoon een kaasblok met oortjes. Huppakee, terug.",
    "sluip.gezien.2": "Bliep-bliep. Ik heb je gezien. Ik zeg het niet tegen Plankmika. Deze keer.",
    "sluip.laser.0": "Bliep! Laser geraakt. Dat kriebelt, hè? Terug naar af, schat.",
    "sluip.laser.1": "Bliep! Rood betekent: even wachten. Probeer het nog eens.",
    "sluip.laser.2": "Bliep! Mijn mooie laser. Hij was net gepoetst.",
    "plankmika.piep.0": "Meneer! Mevrouw! Wacht even! Ik heb een menu!",
    "plankmika.piep.1": "Vetballetjes van de dag! Twee halen, twee betalen!",
    "plankmika.piep.2": "Karen, waarom rent onze klant WEG?!",
    "plankmika.piep.3": "Kom terug! Het vet is nog warm! Ongeveer!",
    "buiten": "Buiten! Achter je klinkt een heel klein, heel boos \"KAREEEN!\". Op naar Meneer Krabguh, aan de overkant.",
    "trommel.regel": "Trommelen: klik op de trommel (linker- of rechtermuisknop) zodra een bel erop knapt. %s van de %s is genoeg. "
                     "Missen kost niks: dan telt Octo gewoon opnieuw af.",
    "beloning": "Voor je guhs: de vierkante broek met stropdas van Sponsguh! En %s Knabbelburgers. De grill in De Krokante Knabbel "
                "bakt voortaan ook voor jou.",
    "klaar": "Deel 1 is klaar: Het geheime recept! Kom gauw terug: Meneer Krabguh heeft groot nieuws, njeg.",
}
# the small talk of a click while you are at one of the four steps (who . step)
KLIK = {
    "krabguh": {7: "Mijn kantoor! Nu! Achterin! Er is iets VRESELIJKS gebeurd!",
                8: "Elke minuut zonder recept kost me... (telt op zijn scharen) ...DRIE kaasknabbels! Opschieten!",
                9: "Heb je het? HEB JE HET? Kom mee naar mijn kantoor, hier hebben de muren oren. En klanten.",
                10: "Vanavond verkoop ik Knabbelburgers in de Bellenkom. Aan de ingang. Voor het dubbele. Feest is feest!"},
    "sponsguh": {7: "Hoor je die bel? Dat is de Krabguh-bel! Die gaat alleen bij brand, bij geld of bij... brand van geld!",
                 8: "Ik zou meegaan, maar ik piep als ik sluip. Hoor maar: (piep) ...zie je?",
                 9: "Je hebt het recept?! Ik wist het! Ik wist het niet, maar ik WIST het!",
                 10: "Mijn zangstem is opgewarmd. Lalalaaa! Ja hoor, hij is warm. Misschien te warm."},
    "patguh": {7: "Ik hoorde een bel. Ik dacht: ijscoman. Het was geen ijscoman.",
               8: "Sluipen? Daar ben ik heel goed in. Ik sluip elke ochtend uit bed. Heel langzaam.",
               9: "Wat is een recept? Is dat een soort boterham?",
               10: "Ik ga vooraan staan. Ik heb een bord gemaakt: 'OCTO'. Ik wist niet hoe je de rest spelt."},
    "octo": {7: "Een gestolen recept. Misschien is het een teken. Een teken dat ik vrij moet hebben.",
             8: "Als Plankmika je vangt, mag ik dan je dienst overnemen? Nee? Zucht.",
             9: "Het recept is terug. Joepie. Ik ben dolblij. Kun je dat zien? Nee? Dat klopt.",
             10: "Vanavond speel ik tegen Octoliam. Met een kaasblok als zanger en een nieuweling op de trommel. Zucht."},
    "plankmika": {7: "Recept? Welk recept? Ik weet van niks. Ik ben maar een heel klein Mikaatje.",
                  8: "Een klant?! In MIJN restaurant? Karen, pak de vetballetjes!",
                  9: "Het recept is WEG! Karen! Wie heeft dat gedaan? ...Waarom kijk je zo naar mij, Karen?",
                  10: "Ik ga NIET naar dat feest. Ik ga thuis een nieuw plan maken. Plan AA. Heel gemeen."},
    "parel": {7: "Papa schreeuwt nog harder dan normaal! Dat betekent: er is iets met geld.",
              8: "Pas op in die emmer. Daar ruikt alles naar vet. Bah.",
              9: "Papa danst! Papa danst NOOIT! Wat heb je gedaan?",
              10: "Ik hoop dat ze iets speelt waar je op kunt dansen. Iets van nu. Niet iets van papa."},
    "gerrit": {7: "Njeg.", 8: "Njeg njeg.", 9: "Njeeeg!", 10: "Njeg! (Gerrit tikt met zijn staart op de maat.)"},
    "bewoner": {7: "Heb je het gehoord? Er is ingebroken in De Krokante Knabbel! Het recept is weg!",
                8: "Zonder Knabbelburgers eet ik vandaag... vet? Nee, dan liever niks.",
                9: "Het recept is terug, zeggen ze! Ik ga meteen in de rij staan!",
                10: "Vanavond: Octo in de Bellenkom! Ik neem oordopjes mee. Of toch niet? Ik neem ze mee. Voor de zekerheid."},
}

# =====================================================================================================================
# the scenes (titel, regels, namen); the speakers' names: verteller = De Verteller, karen = Karen
# =====================================================================================================================
NAMEN = {"verteller": "De Verteller", "karen": "Karen", "krabguh": "Meneer Krabguh", "sponsguh": "Sponsguh", "octo": "Octo",
         "plankmika": "Plankmika", "patguh": "Patguh", "octoliam": "Octoliam", "professor": "Professor Knabbelkloon"}
SCENES = {
    "plan_z": ("Plan Z", {
        "weg": "MIJN RECEPT! Mijn geheime Knabbelburger-recept is WEG!",
        "wat": "Meneer Krabguh! Ik hoorde de bel! Wat is er gebeurd?",
        "kluis": "De kluis is leeg! Gisteravond lag mijn recept er nog in. En nu? Niks! Nul! Noppes!",
        "gemeen": "Wie doet nou zoiets gemeens?!",
        "terug": "Ah, mes amis... laten we even terugspoelen. Naar gisteravond, aan de overkant van de straat, njeg.",
        "mislukt": "Plan A tot en met Y: allemaal mislukt! Ik ben een genie zonder plan, Karen!",
        "la": "Er zit nog één la in de kast, schat.",
        "z": "Plan... Z?!",
        "stappen": "Stap één: wacht tot het donker is. Stap twee: pak het recept. Stap drie: KNABBELBURGERS!",
        "bedankt": "Bedankt Karen, mijn computervrouw.",
        "graag": "Graag gedaan, schat.",
        "nacht": "Die nacht, terwijl heel Bikinibodem lag te snurken...",
        "kraak": "Een kraak. Een piepje. En toen: een heel klein, heel gemeen gegiechel.",
        "hihi": "Hihihi! Het is van mij!",
        "ochtend": "En zo, mes amis, komen we weer uit bij vanochtend.",
        "een": "Er is maar één wezen in heel Bikinibodem dat zo gemeen is... en zo klein...",
        "plankmika": "PLANKMIKA!",
        "vrij": "Betekent dit dat ik vandaag vrij heb?",
        "nee": "NEE.",
        "halen": "Meneer Krabguh, wij halen het terug!",
        "jij": "Jij daar, nieuweling! Plankmika kent jouw gezicht nog niet. Sluip De Vetemmer in, aan de overkant, en haal mijn recept terug!",
        "wacht": "En ik hou de wacht bij de deur! Heel onopvallend! ...HEEL ONOPVALLEND!",
    }),
    "knipoog_kloon": ("Professor Knabbelkloon", {
        "kloon": "Als u het recept kloont, heeft u er twee!",
        "geniaal": "Dat... is eigenlijk geniaal.",
        "nee": "Nee.",
        "nee2": "...Nee.",
    }),
    "sluip_uitleg": ("De slechtste wacht van Bikinibodem", {
        "psst": "Psssst! Hier! Ik ben de wacht. Niemand ziet me. Ik ben een struik.",
        "camera": "Binnen kijkt Karen mee door camera's. Waar de vloer geel oplicht, ziet ze je. Wacht tot ze de andere kant op kijkt.",
        "laser": "De rode lasers knipperen: aan, uit, aan. Loop erdoor als ze uit zijn. En bukken maakt je stiller!",
        "rust": "Word je gezien? Niks aan de hand: dan sta je weer bij het laatste rustpuntje. Het recept ligt helemaal achterin, in een kluisje.",
        "succes": "Succes! Ik zwaai als ik je niet meer zie. ...Ik zwaai alvast.",
    }),
    "recept": ("Het geheime ingrediënt", {
        "open": "Ah! Het kluisje gaat open, en daar ligt het: het geheime recept, njeg.",
        "lees": "Het geheime ingrediënt van de Knabbelburger is...",
        "knabbel": "...een kaasknabbel.",
        "altijd": "Natuurlijk. Het is altijd een kaasknabbel.",
        "iemand": "Bliep. Er staat iemand bij het recept, schat.",
        "klant": "Iemand? Een KLANT?! Eindelijk een klant! Karen, zet de beveiliging uit, ik kom eraan met het menu!",
        "weg": "Dit, mes amis, is het moment om héél snel weg te wezen.",
    }),
    "feest": ("Feest in de Bellenkom", {
        "recept": "MIJN RECEPT! Kom maar bij papa, mijn kleine geldmachientje!",
        "held": "Je hebt het gedaan! Je bent een held! Een sluipheld!",
        "feest": "Dit moet gevierd worden! Vanavond geef ik een feest in de Bellenkom, met gratis Knabbelburgers! ...Halve. Halve gratis.",
        "uitdaging": "Vanavond? In de Bellenkom? Daar daagt Octoliam mij uit. Hij speelt, ik speel, en de stad kiest.",
        "band": "Alleen is mijn band gisteren weggelopen. Ze vonden mijn muziek 'te mooi'. Zeiden ze. Ongeveer.",
        "stad": "Octo! Je hebt geen band nodig. De hele stad is je band!",
        "dom": "Dat is het domste wat ik ooit gehoord heb. ...Hebben jullie iemand voor de trommel?",
        "ritme": "Jij, nieuweling! Jij hebt ritme. Dat zag ik aan je gesluip.",
        "avond": "En zo, mes amis, gingen we die avond naar de Bellenkom. Met een trommel. En héél veel hoop.",
    }),
    "zoete_zege": ("Zoete Knabbelzege", {
        "bellenkom": "Ah, de Bellenkom! Vanavond: de grote muziekwedstrijd. Heel Bikinibodem is er, njeg.",
        "orkest": "Ha! Octo! Waar is je orkest? Ik zie alleen een kaasblok en een nieuweling met een trommel!",
        "vergissing": "Dit is een vergissing. Dit is een enorme vergissing.",
        "vertrouw": "Octo... vertrouw op de knabbel.",
        "tel": "...Een, twee. Een, twee, drie, vier!",
        "stilte": "Een stilte. En dan... gejuich. Héél veel gejuich.",
        "prachtig": "Dit... dit is... prachtig...",
        "mij": "Ze... ze klappen voor MIJ?",
        "ster": "Octo, je bent een ster!",
        "verkocht": "En ik heb vierhonderd Knabbelburgers verkocht! Aan de ingang! Voor het dubbele!",
        "einde": "En zo, mes amis, kwam het geheime recept weer thuis, en was Octo Tentakel één avond lang de held van Bikinibodem. Eén avond, njeg.",
    }),
}
# the speakers per scene whose name differs from NAMEN (the voice from the lab in "recept")
NAMEN_PER_SCENE = {"recept": {"plankmika": "Plankmika (uit het lab)"}}
# the look scenes of the hints: (title, the one run-on caption)
KIJK = {
    "kijk_kantoor": ("Het kantoor van Krabguh", "Daar, achter in De Krokante Knabbel: het kantoor met de kluis."),
    "kijk_vetemmer": ("De Vetemmer", "De grote grijze emmer aan de overkant. Achterin: het lab van Plankmika."),
    "kijk_terug": ("Terug naar Krabguh", "Meneer Krabguh wacht in zijn kantoor, aan de overkant van de straat."),
    "kijk_podium": ("Het podium", "De Bellenkom: je trommel staat rechts op het podium. Het trapje is aan de zijkant."),
}

# =====================================================================================================================
# FTB (one quest per step; r10 = the gear of part 1)
# =====================================================================================================================
FTB = [
    (7, "Plan Z", "De alarmbel van &6De Krokante Knabbel&r gaat! Ren naar het kantoor van &dMeneer Krabguh&r, achter in het restaurant. "
                  "Er is iets met zijn kluis...", "guhs:einde2_bouw_bikinibodem_kluis"),
    (8, "Inbreken in De Vetemmer", "&dPlankmika&r heeft het geheime recept! Sluip door zijn lab in &6De Vetemmer&r, langs de camera's "
                                   "en lasers van &dKaren&r. Gezien worden zet je alleen terug bij het laatste rustpunt.", "guhs:einde2_bouw_bikinibodem_camera"),
    (9, "Het recept terug", "Breng het geheime recept terug naar &dMeneer Krabguh&r. Het geheime ingrediënt verklappen we niet. "
                            "(Het is een kaasknabbel.)", "minecraft:paper"),
    (10, "Zoete Knabbelzege", "Feest in de &6Bellenkom&r! &dOcto&r moet optreden tegen Octoliam en heeft geen band. Jij pakt de "
                              "trommel, de hele stad zingt mee.", "minecraft:note_block"),
]


def build(h):
    lang = lambda k, t: kit.lang(h, k, t)
    for k, t in GUI.items():
        lang(f"{P}.{k}", t)
    for k, t in BELONING.items():
        lang(k, t)
    for k, t in CHAT.items():
        lang(f"{Q}.{k}", t)
    for wie, regels in KLIK.items():
        for stap, t in regels.items():
            lang(f"{Q}.klik.{wie}.{stap}", t)
    for naam, (titel, regels) in SCENES.items():
        namen = {a: n for a, n in NAMEN.items() if a in sprekers(naam)}
        namen.update(NAMEN_PER_SCENE.get(naam, {}))
        verhaal_motor.scene(h, SCENE + naam, titel, regels, namen)
    for naam, (titel, tekst) in KIJK.items():
        lang(f"scene.guhs.{SCENE}{naam}.titel", titel)
        lang(f"scene.guhs.{SCENE}{naam}.tekst", tekst)
    selfcheck(h)


def ftb(fq):
    for i, (stap, titel, tekst, icon) in enumerate(FTB):
        laatste = i == len(FTB) - 1
        fq.q(f"{SLICE}_r{stap}", titel, tekst, icon, [fq.adv(f"{LIJN}_stap_{stap + 1}")],
             rewards=(("guhs:kaas_knabbels", 12 if laatste else 8),), shape="gear" if laatste else None)


# =====================================================================================================================
# the self-check: every line the Java scenes and clicks name has its text
# =====================================================================================================================
_JAVA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "java", "nl", "juiced", "guhs", "feature",
                     "einde2", "spd1recept")


def _java(naam):
    with open(os.path.join(_JAVA, naam), encoding="utf-8") as f:
        return f.read()


def scene_regels():
    """{scene: [(spreker, key)]} from D1rScenes.java (each scene's method holds its zeg calls)."""
    src = _java("D1rScenes.java")
    uit = {}
    for stuk in src.split("private static Scene ")[1:]:
        m = re.search(r'maak\("([a-z_]+)"\)', stuk)
        if m:
            uit[m.group(1)] = re.findall(r'\.zeg\([^,]+,\s*"([a-z_]*)",\s*"([a-z_0-9]+)"', stuk)
    return uit


def sprekers(naam):
    return {s for s, _ in scene_regels().get(naam, [])} | {"verteller"}


def selfcheck(h):
    problems = []
    regels = scene_regels()
    if sorted(regels) != sorted(SCENES):
        problems.append(f"the scenes of D1rScenes.java {sorted(regels)} are not those of SCENES {sorted(SCENES)}")
    for naam, zinnen in regels.items():
        for spreker, key in zinnen:
            if f"scene.guhs.{SCENE}{naam}.{key}" not in h.NL:
                problems.append(f"scene {naam}: no text for the line {key}")
            if spreker and f"scene.guhs.{SCENE}{naam}.naam.{spreker}" not in h.NL and spreker not in ("speler",):
                problems.append(f"scene {naam}: no name for the speaker {spreker}")
    if "Bedankt Karen, mijn computervrouw." != SCENES["plan_z"][1]["bedankt"]:
        problems.append("plan_z: the required line (CONTRACT_E2_C 5.3) must stay literal")
    java = _java("E2SpD1Recept.java") + _java("Sluipen.java") + _java("Trommel.java")
    for key in set(re.findall(r'P \+ "([a-z_.0-9]+)"', java)):
        if f"{P}.{key}" not in h.NL:
            problems.append(f"no text {P}.{key}")
    for key in set(re.findall(r'Q \+ "([a-z_.0-9]+)"', java)):
        if not key.endswith(".") and f"{Q}.{key}" not in h.NL:
            problems.append(f"no text {Q}.{key}")
    for wie in ("krabguh", "sponsguh", "patguh", "octo", "plankmika", "parel", "gerrit", "bewoner"):
        for stap in (7, 8, 9, 10):
            if f"{Q}.klik.{wie}.{stap}" not in h.NL:
                problems.append(f"no click line {wie} {stap}")
    for n in range(3):
        for soort in ("gezien", "laser"):
            if f"{Q}.sluip.{soort}.{n}" not in h.NL:
                problems.append(f"no Karen line sluip.{soort}.{n}")
    for n in range(4):
        if f"{Q}.plankmika.piep.{n}" not in h.NL:
            problems.append(f"no Plankmika line {n}")
    for naam in KIJK:
        if f'ID + "{naam}"' not in _java("D1rScenes.java"):
            problems.append(f"the look scene {naam} is not registered in D1rScenes.java")
    if problems:
        raise SystemExit(f"{SLICE}: " + "; ".join(problems))
