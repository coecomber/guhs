"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.10), slice sp_zij: the side lines of Bikinibodem (Sandy-guh's tree dome, De Vliegende
Guhlander, Zeemeerman and Mosselguh) and Octo's house (atmosphere).
Java: nl.juiced.guhs.feature.einde2.spzij (E2SpZijFeature). Chunk c119_e2_sp_zij (English: tools/lang/en/c119_e2_sp_zij.json).

  lang      the three lines (verhaal_motor: names, explanation, step texts, the variant 3_b of zeemeerman), the five scenes,
            the hints and their look scenes, every line of the characters, the minigames' counters and titles, Octo's house,
            the clothes source einde2_sp_zij (the luchthelm and the heldencape)
  data      the step advancements of the three lines (verhaal_motor)
  ftb       section "Bikinibodem: zijdingen": one quest per step, keys einde2_sp_zij_<lijn>_<n> (wereld.ftb_questlijn)
The markers this slice adds to the build: einde2_sp_zij_plekken.py. Wiki: einde2_sp_zij_wiki.py.
"""
import os
import re

from features import einde2_kit as kit
from features import verhaal_motor, wereld

P = "gui.guhs.einde2_sp_zij"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True
SANDY, GUHLANDER, ZEEMEERMAN = "sandy_boomkoepel", "vliegende_guhlander", "zeemeerman"
# its own section (CONTRACT_E2_C 7.10): the side lines of the town; keys einde2_sp_zij_<lijn>_<n>
FTB_SECTIES = [("einde2_sp_zij", "Bikinibodem: zijdingen", "geo:sandy_guh:sandy_guh", None)]
ICOON = {SANDY: "guhs:sponsguh_luchthelm", GUHLANDER: "guhs:spooklantaarn", ZEEMEERMAN: "guhs:sponsguh_heldencape"}

# =====================================================================================================================
# the three lines (step i = index i; the Java numbers: SpZijLijnen)
# =====================================================================================================================
LIJNEN = {
    SANDY: dict(
        naam="Sandy-guh's boomkoepel",
        uitleg="In het oosten van Bikinibodem staat een glazen koepel vol echte lucht, met een Guhmensie-boom erin. Daar woont "
               "Sandy-guh: karatekampioen, uitvinder, en stiekem een beetje heimwee naar de roze lucht van thuis.",
        stappen=[
            ("Op bezoek bij Sandy-guh",
             "Ga naar de glazen boomkoepel in het oosten van de stad (zuid van het rusthuis), stap door de luchtsluis aan de "
             "westkant naar binnen en loop naar Sandy-guh.",
             "De boomkoepel van Sandy-guh, oost in Bikinibodem"),
            ("Karate op de mat",
             "Stap op de karatemat in de koepel. Sandy-guh duwt: roept ze HAI, stap dan opzij of spring. Ontwijk haar tien keer. "
             "Word je geduwd, dan telt die keer gewoon niet mee.",
             "De karatemat in de boomkoepel"),
            ("Drie raketonderdelen",
             "Bij de laatste BOEM vlogen drie onderdelen van Sandy's raket de stad in: de neuskegel (bij het Kwallenveld in het "
             "noordoosten), de straalpijp (achter de huisjes aan de Westring) en de antenne (tussen de huizen van de "
             "Schelpstraat). Loop eroverheen om ze op te rapen.",
             "Overal in Bikinibodem (volg de lichtzuil)"),
            ("De raket",
             "Breng de drie onderdelen naar Sandy's kapotte raket, in de boomkoepel naast de boom.",
             "De raket in de boomkoepel"),
        ],
        klaar=("Klaar! Sandy's raket ging omhoog. Een klein stukje. Je kreeg haar luchthelm.", "De boomkoepel van Sandy-guh"),
        kort={"0": "Bezoek Sandy-guh in haar koepel", "1": "Ontwijk Sandy-guh 10 keer", "2": "Vind 3 raketonderdelen",
              "3": "Naar de raket in de koepel", "klaar": "Sandy's raket: klaar!"},
    ),
    GUHLANDER: dict(
        naam="De Vliegende Guhlander",
        uitleg="Soms drijft er een groen spookschip boven de Schelpstraat. Wie geduldig wacht bij de aanlegplaats, mag aan boord "
               "bij de spookkapitein. Die probeert al honderd jaar iemand te laten schrikken. Het lukt hem nooit.",
        stappen=[
            ("Wachten op het spookschip",
             "Ga naar de aanlegplaats van De Vliegende Guhlander: de ring van stenen met het bordje, oost van de rots van Patguh "
             "aan de Schelpstraat. Blijf op de stenen staan tot het schip komt: tien tellen, ze staan boven je werkbalk.",
             "De aanlegplaats aan de Schelpstraat, oost van Patguh's rots"),
            ("Aan boord",
             "Stap in de bubbelstroom bij de aanlegplaats: hij brengt je omhoog naar het dek. Loop daar naar de spookkapitein.",
             "Het dek van De Vliegende Guhlander, boven de Schelpstraat"),
            ("Drie spooklantaarns",
             "De drie lantaarns van de kapitein zijn weggewaaid: één tussen de Kruimelkwallen in het veld oost van de "
             "aanlegplaats, één achter De Vetemmer (noord van de aanlegplaats) en één bij de knabbelkoorvrucht van Sponsguh, in "
             "het westen. Loop eroverheen.",
             "Overal in Bikinibodem (volg de lichtzuil)"),
            ("Terug aan boord",
             "Ga terug naar de aanlegplaats. Het schip komt weer (vijf tellen): neem de bubbelstroom omhoog en breng de "
             "kapitein zijn lantaarns.",
             "De aanlegplaats aan de Schelpstraat"),
        ],
        klaar=("Klaar! De spookkapitein vindt de weg weer. Je kreeg een spooklantaarn en het recept ervan.", "De aanlegplaats aan de Schelpstraat"),
        kort={"0": "Wacht bij de aanlegplaats", "1": "Omhoog naar het dek", "2": "Vind 3 spooklantaarns",
              "3": "De lantaarns terug naar de kapitein", "klaar": "De spookkapitein: klaar!"},
    ),
    ZEEMEERMAN: dict(
        naam="Zeemeerman en Mosselguh",
        uitleg="In het rusthuis aan de oostkant van de stad wonen twee bejaarde superhelden. Ze redden Bikinibodem nog steeds. "
               "Na hun middagdutje. Als ze hun spullen kunnen vinden.",
        stappen=[
            ("De oude helden",
             "Ga naar het rusthuis in het oosten (noord van de boomkoepel; de deur zit aan de westkant) en loop naar Zeemeerman "
             "en Mosselguh.",
             "Het rusthuis, oost in Bikinibodem"),
            ("De verloren riem",
             "De riem van Zeemeerman ligt in de onzichtbare bootmobiel, op de parkeerplaats aan de zuidkant van het rusthuis. Zoek "
             "tussen niets naar iets en raap de riem op.",
             "De parkeerplaats aan de zuidkant van het rusthuis"),
            ("De riem terug",
             "Breng de riem terug naar Zeemeerman, binnen in het rusthuis.",
             "Het rusthuis, oost in Bikinibodem"),
            ("Een heldendaad",
             "Rijd met de onzichtbare bootmobiel naar de voordeur van De Krokante Knabbel (west van het rusthuis, aan de "
             "Hoofdstraat) en haal de heldenlunch op.",
             "De voordeur van De Krokante Knabbel"),
        ],
        extra={"3_b": ("Breng de heldenlunch terug naar Zeemeerman en Mosselguh in het rusthuis. Toet toet!",
                       "Het rusthuis, oost in Bikinibodem")},
        klaar=("Klaar! De lunch is gered. Je kreeg een echte heldencape.", "Het rusthuis, oost in Bikinibodem"),
        kort={"0": "Bezoek de oude helden", "1": "Zoek de riem in de bootmobiel", "2": "De riem terug naar Zeemeerman",
              "3": "Haal de heldenlunch", "3_b": "De lunch terug naar het rusthuis", "klaar": "De oude helden: klaar!"},
    ),
}

# =====================================================================================================================
# the five scenes (SpZijScenes): titel, regels {key: tekst}, namen {acteur: naam}
# =====================================================================================================================
SCENES = {
    "einde2_sp_zij_sandy": dict(
        titel="Sandy-guh's boomkoepel",
        regels={
            "hallo": "Hé, buurguh! Welkom in mijn boomkoepel! Kijk eens rond: hier binnen is lucht. Echte lucht!",
            "lucht": "Ik heb hem helemaal zelf gebouwd. Een glazen koepel, een luchtsluis, en een Guhmensie-boom om onder te slapen.",
            "helm": "Wacht, eerst je luchthelm op. *ploep* Zo!",
            "grap": "Haha, geen zorgen: guhs ademen overal. Maar je ziet er zó schattig uit met een vissenkom op je oortjes.",
            "roze": "Ik kom uit de Guhmensie. Daar is de lucht roze en zacht. Hier beneden mis ik dat soms een beetje, njeg.",
            "karate": "Maar genoeg gezeurd! Zin in een potje karate? Op de mat daar. Ik duw, jij ontwijkt. HAI-JA!",
        },
        namen={"sandy": "Sandy-guh"}),
    "einde2_sp_zij_sandy_raket": dict(
        titel="Omhoog! (een klein stukje)",
        regels={
            "onderdelen": "De neuskegel, de straalpijp, de antenne! Alle drie! Even vastschroeven...",
            "aftellen": "Klaar voor de start! Drie... twee... één... VAHOEG!",
            "sprongetje": "Hij ging omhoog! Echt waar! Een heel klein stukje. Een sprongetje is toch ook een beetje vliegen?",
            "roze": "Op een dag vlieg ik hem helemaal naar boven, door de leegte, tot de lucht weer roze wordt.",
            "helm": "Hier, hou die luchthelm maar. Dan heb je altijd een stukje van mijn koepel bij je. Dank je, maatje!",
        },
        namen={"sandy": "Sandy-guh"}),
    "einde2_sp_zij_guhlander": dict(
        titel="De Vliegende Guhlander",
        regels={
            "mist": "Ah... de mist komt op, mes amis. Kijk omhoog. Daar, uit de mist... een schip!",
            "boe": "BOEOEOE! Wie verstoort de eeuwige rust van De Vliegende Guhlander?!",
            "levend": "Een levende guh! Al honderd jaar is hier niemand meer langsgekomen. Ik ben nogal uit vorm met boe-roepen.",
            "boord": "Kom aan boord, als je durft! Mijn bubbelstroom brengt je naar het dek. BOE. Dat was een uitnodiging.",
            "kolom": "Vous voyez? Er borrelt een bubbelstroom omhoog, recht naar het dek. Het spookschip wacht op je.",
        },
        namen={"kapitein": "De spookkapitein", "verteller": "De Verteller"}),
    "einde2_sp_zij_hakker": dict(
        titel="De Nacht van de Hakkende Hakker",
        regels={
            "kaars": "Even later, op het dek... bij kaarslicht, njeg.",
            "verhaal": "Ga zitten, levende guh. Ik vertel je het engste verhaal van de zeven leegtes: De Nacht van de Hakkende Hakker.",
            "nacht": "Het was een donkere nacht. In een stille keuken. En toen klonk het: tok... tok... TOK.",
            "dichter": "Het kwam dichterbij. Tok. Tok. Een mes. Een plank. En een gesnik, een hartverscheurend gesnik...",
            "boe": "HET WAS... DE HAKKENDE HAKKER!",
            "uien": "Hij hakte uien. Voor de soep. Daarom huilde hij zo. Hahaa! Eng hè? Je oortjes trillen nog!",
            "lantaarns": "Maar nu iets dat écht eng is: mijn drie spooklantaarns zijn weggewaaid. De stad in. Allemaal.",
            "vraag": "Zonder hun licht vind ik nooit de weg terug door de mist. Breng je ze terug? Ik blaas je wel even naar beneden.",
        },
        namen={"kapitein": "De spookkapitein", "verteller": "De Verteller"}),
    "einde2_sp_zij_helden": dict(
        titel="Zeemeerman en Mosselguh",
        regels={
            "ta_daa": "ZEEMEERMAN...",
            "en": "...EN MOSSELGUH! Ta-daa!",
            "bezoek": "Een bezoeker! Kom je voor een handtekening? Of... heb je soms een SUPERSCHURK gezien?!",
            "riem": "Hij heeft vandaag zijn riem nog niet om, jonge held. Zonder riem doet hij niks heldhaftigs. Behalve slapen.",
            "kwijt": "Mijn riem! Die moet in de onzichtbare bootmobiel liggen. Op de parkeerplaats, aan de zuidkant.",
            "onzichtbaar": "Het probleem, chef: de bootmobiel is onzichtbaar. We vinden hem nooit terug.",
            "ga": "Jonge held! Vind mijn riem! Ga, ga, GA! ...Wij doen even een dutje.",
        },
        namen={"zeemeerman": "Zeemeerman", "mosselguh": "Mosselguh"}),
}

# =====================================================================================================================
# the hints (SpZijHints; %s = the direction) and their look scenes (titel, onderschrift)
# =====================================================================================================================
HINTS = {
    "sandy.0": "Sandy-guh woont in de glazen boomkoepel %s. Ga door de luchtsluis aan de westkant naar binnen.",
    "sandy.1": "Stap op de karatemat in de boomkoepel, %s. Roept Sandy HAI? Stap opzij of spring!",
    "sandy.2": "Het dichtstbijzijnde raketonderdeel ligt %s. Volg de lichtzuil en loop eroverheen.",
    "sandy.3": "Breng de onderdelen naar Sandy's raket in de boomkoepel, %s.",
    "guhlander.0": "De aanlegplaats van De Vliegende Guhlander is %s: de ring van stenen oost van Patguh's rots. Wacht daar op het schip.",
    "guhlander.1": "Stap in de bubbelstroom bij de aanlegplaats, %s: hij brengt je naar het dek van het spookschip.",
    "guhlander.2": "De dichtstbijzijnde spooklantaarn is %s. Volg de lichtzuil en loop eroverheen.",
    "guhlander.3": "Terug naar de aanlegplaats, %s. Het schip komt weer: omhoog naar de kapitein!",
    "zeemeerman.0": "Zeemeerman en Mosselguh wonen in het rusthuis %s. De deur zit aan de westkant.",
    "zeemeerman.1": "De onzichtbare bootmobiel staat op de parkeerplaats %s, aan de zuidkant van het rusthuis. De riem ligt erin.",
    "zeemeerman.2": "Breng de riem naar Zeemeerman in het rusthuis, %s.",
    "zeemeerman.3": "Toet toet! Rijd met de onzichtbare bootmobiel naar de voordeur van De Krokante Knabbel, %s.",
}
KIJK = {
    "koepel": ("Sandy's boomkoepel", "Een glazen koepel vol lucht, met een luchtsluis aan de westkant."),
    "mat": ("De karatemat", "Hier oefent Sandy-guh. Ze duwt, jij ontwijkt."),
    "onderdeel": ("Een raketonderdeel!", "Weggeslingerd bij de laatste BOEM. Loop eroverheen."),
    "raket": ("Sandy's raket", "Nog even vastschroeven, dan... VAHOEG?"),
    "trap": ("De aanlegplaats", "Een ring van stenen en een bordje: soms. Boe."),
    "stroom": ("De bubbelstroom", "Recht omhoog naar het dek van het spookschip."),
    "lantaarn": ("Een spooklantaarn!", "Weggewaaid uit de mist. Loop eroverheen."),
    "rusthuis": ("Het rusthuis", "Hier wonen twee helden met pensioen. De deur zit aan de westkant."),
    "parkeer": ("De parkeerplaats", "Hier staat de onzichtbare bootmobiel. Waarschijnlijk."),
    "knabbel": ("De Krokante Knabbel", "Hier haal je de heldenlunch. Heldhaftig, toch?"),
}

# =====================================================================================================================
# every other text (Java: SpZij.P + ...)
# =====================================================================================================================
TEXTS = {
    f"{P}.open": "Bikinibodem leeft! Sandy-guh in haar boomkoepel, de spookkapitein van De Vliegende Guhlander en de oude helden "
                 "in het rusthuis kunnen je hulp best gebruiken. Kijk in je Guhdex bij Verhalen, onder Sponsguh Kaasbroek.",
    f"{P}.beloning.spooklantaarn": "Een spooklantaarn en het recept",
    # Sandy-guh
    f"{P}.sandy.later": "Hé daar! Ik ben Sandy-guh. Eerst je baantje bij Meneer Krabguh regelen, dan doen we karate, njeg!",
    f"{P}.sandy.helm_op": "Je luchthelm zit op. Blub.",
    f"{P}.sandy.karate.titel": "Karate met Sandy-guh",
    f"{P}.sandy.karate.regel": "Roept ze HAI? Stap opzij of spring! Ontwijk haar 10 keer.",
    f"{P}.sandy.karate.hai": "HAI...! Stap opzij of spring! (ontweken: %s/%s)",
    f"{P}.sandy.karate.ontweken": "Ontweken! %s/%s",
    f"{P}.sandy.karate.geraakt": "Zachtjes geduwd! Die telt niet, verder niks aan de hand. (%s/%s)",
    f"{P}.sandy.karate.pauze": "Pauze: stap weer op de mat om verder te gaan. (%s/%s)",
    f"{P}.sandy.karate.klaar": "HAI-JA! %s/%s",
    f"{P}.sandy.karate.klaar_onder": "Je bent sneller dan een kwal op rolschaatsen!",
    f"{P}.sandy.na_karate.1": "Wauw! Jij bent snel! Sneller dan een kwal op rolschaatsen!",
    f"{P}.sandy.na_karate.2": "Zeg, help je me even? Mijn raket ging vorige week BOEM, en drie onderdelen vlogen de koepel uit, de stad in.",
    f"{P}.sandy.na_karate.3": "De neuskegel, de straalpijp en de antenne. Vind je ze? Dan bouwen we hem samen af!",
    f"{P}.sandy.onderdeel.titel": "Raketonderdeel! %s/%s",
    f"{P}.sandy.onderdeel.1": "De neuskegel",
    f"{P}.sandy.onderdeel.2": "De straalpijp",
    f"{P}.sandy.onderdeel.3": "De antenne",
    f"{P}.sandy.onderdeel.alle": "Alle drie! Breng ze naar Sandy's raket in de boomkoepel.",
    f"{P}.sandy.klik.karate": "Op de mat, maatje! Ontweken: %s van de %s. HAI!",
    f"{P}.sandy.klik.onderdelen": "Gevonden: %s van de %s. Volg de lichtzuil, ze liggen overal in de stad!",
    f"{P}.sandy.klik.raket": "Je hebt ze alle drie? Kom mee naar de raket!",
    f"{P}.sandy.na.1": "Elke dag een beetje karate houdt je oortjes soepel!",
    f"{P}.sandy.na.2": "Ik heb de raket nog een keer geprobeerd. Hij ging weer een klein stukje omhoog. Vooruitgang!",
    f"{P}.sandy.na.3": "Mooi helmpje heb je op. Staat je beter dan mij, njeg.",
    f"{P}.sandy.klaar.titel": "Sandy-guh's boomkoepel: klaar!",
    f"{P}.sandy.klaar.onder": "Je kreeg de luchthelm (guhkleding)",
    # De Vliegende Guhlander
    f"{P}.guhlander.wacht": "Het spookschip komt eraan... %s. Blijf op de aanlegplaats staan.",
    f"{P}.guhlander.terug.titel": "De Vliegende Guhlander is terug!",
    f"{P}.guhlander.terug.onder": "Neem de bubbelstroom omhoog naar het dek.",
    f"{P}.guhlander.lantaarn.titel": "Spooklantaarn! %s/%s",
    f"{P}.guhlander.lantaarn.onder": "De kapitein telt mee vanaf zijn dek: %s van de %s. Boe.",
    f"{P}.guhlander.lantaarn.alle": "Alle drie! Terug naar de aanlegplaats: het schip komt je weer halen.",
    f"{P}.guhlander.dank.1": "Mijn lantaarns! Alle drie! Nu vind ik de weg weer door de mist.",
    f"{P}.guhlander.dank.2": "En het mooiste: toen ik ze zag gloeien, schrok ik me zelf een hoedje. Eindelijk heb ik iemand laten schrikken!",
    f"{P}.guhlander.dank.3": "Hier, neem er een mee, en het recept erbij. Dan spookt het ook een beetje bij jou thuis. BOE! Dag!",
    f"{P}.guhlander.klik": "Boe. Op het dek praten we verder, levende guh.",
    f"{P}.guhlander.na.1": "BOE! ...Schrok je? Nee? Ik oefen nog.",
    f"{P}.guhlander.na.2": "Mijn lantaarns branden als nooit tevoren. Spookachtig mooi, hè?",
    f"{P}.guhlander.klaar.titel": "De Vliegende Guhlander: klaar!",
    f"{P}.guhlander.klaar.onder": "Je kreeg een spooklantaarn en het recept",
    # Zeemeerman and Mosselguh
    f"{P}.helden.later.zeemeerman": "ZEEMEERMAN! Eh... kom later terug, jonge held. Na mijn dutje. En na je eerste werkdag.",
    f"{P}.helden.later.mosselguh": "Hij bedoelt: we zijn er nog niet klaar voor. Kom terug als je in de stad werkt.",
    f"{P}.helden.bonk": "BONK! Je loopt tegen iets aan dat er niet is. De onzichtbare bootmobiel!",
    f"{P}.helden.riem.titel": "De heldenriem!",
    f"{P}.helden.riem.onder": "Breng hem terug naar Zeemeerman in het rusthuis.",
    f"{P}.helden.band.1": "MIJN RIEM! Klik... Ik voel het al: mijn krachten komen terug! Ik voel me twintig jaar jonger!",
    f"{P}.helden.band.2": "Dertig, chef. U had hem al dertig jaar niet om.",
    f"{P}.helden.band.3": "Tijd voor een heldendaad! De allergevaarlijkste van de dag: de lunch halen bij De Krokante Knabbel!",
    f"{P}.helden.band.4": "Neem de onzichtbare bootmobiel, jonge held. Hij staat buiten. Denk ik. Toet toet!",
    f"{P}.helden.lunch.titel": "Heldenlunch opgehaald!",
    f"{P}.helden.lunch.onder": "Twee Knabbelburgermenu's. Terug naar het rusthuis!",
    f"{P}.helden.lunch.terug": "Breng de heldenlunch terug naar het rusthuis, %s.",
    f"{P}.helden.toet": "Toet toet! Je rijdt in de onzichtbare bootmobiel.",
    f"{P}.helden.dank.1": "De lunch! Gered! Precies op tijd voor ons middagdutje.",
    f"{P}.helden.dank.2": "Weer een stad vol tevreden buiken. Goed werk, jonge held.",
    f"{P}.helden.dank.3": "Jij verdient dit: een echte heldencape. Draag hem met trots! ...En was hem af en toe.",
    f"{P}.helden.klik.riem.zeemeerman": "Mijn riem! Hij ligt in de onzichtbare bootmobiel, buiten op de parkeerplaats. Ga, ga!",
    f"{P}.helden.klik.riem.mosselguh": "De parkeerplaats aan de zuidkant. Je voelt het vanzelf. Letterlijk: bonk.",
    f"{P}.helden.klik.lunch.zeemeerman": "De lunch, jonge held! Bij de voordeur van De Krokante Knabbel! Toet toet!",
    f"{P}.helden.klik.lunch.mosselguh": "Twee menu's. Voor mij zonder augurk, graag.",
    f"{P}.helden.na.zeemeerman.1": "ZEEMEERMAN! ...Sorry, dat moet ik soms even roepen.",
    f"{P}.helden.na.zeemeerman.2": "Die cape staat je goed, jonge held. Bijna net zo goed als mij.",
    f"{P}.helden.na.mosselguh.1": "Hij is weer in slaap gevallen met zijn riem om. Vooruitgang.",
    f"{P}.helden.na.mosselguh.2": "Bedankt voor de lunch. Het was het spannendste wat we in weken hebben gedaan.",
    f"{P}.helden.klaar.titel": "Zeemeerman en Mosselguh: klaar!",
    f"{P}.helden.klaar.onder": "Je kreeg de heldencape (guhkleding)",
    # Octo's house
    f"{P}.octo.binnen": "Overal hangen schilderijen van Octo. Allemaal van Octo. Gemaakt door Octo.",
    f"{P}.octo.portret.0": "Octo, ernstig, met zijn klarinet. De lijst is groter dan het schilderij.",
    f"{P}.octo.portret.1": "Octo, nog ernstiger, op een zonsondergang. Hij zucht, zelfs op het doek.",
    f"{P}.octo.portret.2": "Octo als beeld van marmer. Er staat onder: Meesterwerk.",
    f"{P}.octo.portret.3": "Octo die een schilderij schildert van Octo die een schilderij schildert.",
    f"{P}.octo.stem.1": "(van boven) Wie zit er aan mijn klarinet?! Dat is een kunstwerk, geen speelgoed!",
    f"{P}.octo.stem.2": "(van boven) Zucht. Zelfs mijn klarinet klinkt beter als ík hem bespeel. Een klein beetje.",
}

CLOTHES_SOURCE = {"gui.guhs.kledingbron.einde2_sp_zij": "Bikinibodem: zijdingen"}


def build(h):
    for lijn, d in LIJNEN.items():
        verhaal_motor.verhaallijn(h, lijn, d["naam"], d["uitleg"], d["stappen"], klaar=d.get("klaar"), extra=d.get("extra"), kort=d.get("kort"))
    for sid, d in SCENES.items():
        verhaal_motor.scene(h, sid, d["titel"], d["regels"], d["namen"])
    for k, v in HINTS.items():
        kit.lang(h, f"{P}.hint.{k}", v)
    for naam, (titel, onder) in KIJK.items():
        verhaal_motor.scene(h, f"einde2_sp_zij_kijk_{naam}", titel, {"tekst": onder})
    for key, text in {**TEXTS, **CLOTHES_SOURCE}.items():
        kit.lang(h, key, text)
    selfcheck(h)


def ftb(fq):
    for lijn, d in LIJNEN.items():
        stappen = [(naam, nu, ICOON[lijn]) for naam, nu, _waar in d["stappen"]]
        wereld.ftb_questlijn(fq, "einde2_sp_zij", lijn, stappen, eind=(("guhs:kaas_knabbels", 12),))


# =====================================================================================================================
# self-check: every text key the Java code names is written here, every scene line has a text
# =====================================================================================================================
JAVA = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "spzij")


def selfcheck(h):
    problems = []
    prefix = {"SpZij.P": f"{P}.", "P": f"{P}.", "T": None}
    for f in sorted(os.listdir(JAVA)):
        if not f.endswith(".java"):
            continue
        src = open(os.path.join(JAVA, f), encoding="utf-8").read()
        m = re.search(r'static final String T = SpZij\.P \+ "([a-z_]+\.)"', src)
        prefix["T"] = f"{P}." + m.group(1) if m else None
        for naam, rest in re.findall(r'\b(SpZij\.P|T) \+ "([a-z0-9_.]+)"', src):
            if rest.endswith(".") or prefix.get(naam) is None:
                continue
            if f == "SpZijHints.java" and naam == "T":
                key = f"{P}.hint.{rest}"
            else:
                key = prefix[naam] + rest
            if key not in h.NL:
                problems.append(f"{f}: {key}")
        # the scene lines: zeg(t, spreker, key, ...) of every scene of this slice
    jsrc = open(os.path.join(JAVA, "SpZijScenes.java"), encoding="utf-8").read()
    for blok in re.split(r'Cutscene\.maak\(VOOR \+ "', jsrc)[1:]:
        sid = "einde2_sp_zij_" + blok.split('"', 1)[0]
        for spreker, key in re.findall(r'\.zeg\(\d+, "([a-z]*)", "([a-z_]+)"', blok.split("registreer()")[0]):
            if f"scene.guhs.{sid}.{key}" not in h.NL:
                problems.append(f"scene {sid}: line {key}")
            if spreker and f"scene.guhs.{sid}.naam.{spreker}" not in h.NL:
                problems.append(f"scene {sid}: speaker {spreker}")
    for lijn in LIJNEN:
        if f"gui.guhs.verhalen.{lijn}.naam" not in h.NL:
            problems.append(f"line {lijn}")
    if problems:
        raise SystemExit("einde2_sp_zij self-check: " + "; ".join(problems[:20]))
