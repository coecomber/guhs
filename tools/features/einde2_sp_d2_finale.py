"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.8), slice sp_d2_finale: part 2 steps 9-11 of sponsguh_kroon: home on David Hasselguh's back,
Plankmika's bucket helmets (the bubble blaster game), the rock song "Ik ben een Guhfy Guhber" in the Bellenkom; part 2's rewards
(the Sponsguh buddy, Gerrit, Plankmika's antennas, the title "Guhfy Guhber") and the cast's talk after the story.
Java: nl.juiced.guhs.feature.einde2.spd2finale (E2SpD2FinaleFeature, E2SpD2Finale). Chunk c117_e2_sp_d2_finale.

  lang      the step texts (STAPPEN / EXTRA / KORT, written with the line by einde2_sp_verhaal), gui.guhs.einde2_sp_d2_finale.*
            (hints, the game, the ride's banter, the clicks after the story, the rewards), the four story scenes and the three
            look scenes (scene.guhs.einde2_sp_d2_finale_*), the bubble blaster, the clothes source, the title Guhfy Guhber
  assets    the bubble blaster's icon and item model
  FTB       einde2_sp_d2_finale_k9 .. k11 (the skelet's section einde2_sp_deel2 names them), FTB_LINEAIR, the last a gear

Copyright (CONTRACT_E2_B 6.3, CONTRACT_E2_C 5.1): every line is our own; the song's title "Ik ben een Guhfy Guhber" is the
DESIGN's own; its lyrics and their timing are sp_audio's (scene.guhs.einde2_sp_d2_finale_guhfy_guhber.lied.*, chunk c118).
"""
import os

from PIL import Image, ImageDraw

import features.einde2_kit as kit
from features import verhaal_motor

LIJN = "sponsguh_kroon"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True
S = "einde2_sp_d2_finale_"
G = "gui.guhs.einde2_sp_d2_finale."
ITEM = "einde2_sp_d2_finale_bellenblazer"
TITEL = "guhfy_guhber"

# =====================================================================================================================
# the step texts (what to do, where: a direction and a landmark; playtest 1)
# =====================================================================================================================
STAPPEN = {LIJN: {
        9: ("Een lift naar huis",
            "Breng de kroon naar huis! Loop de souvenirwinkel uit, over het plein naar het strand, en dan de lange pier op die "
            "oostwaarts de leegte in loopt. Prinses Mindy-guh wacht op de kop van de pier.",
            "De kop van de pier van Schelpenstad, oost van het strand"),
        10: ("Plankmika's emmerhelmen",
             "Bikinibodem draagt Plankmika's emmerhelmen! Loop door de stadspoort de Hoofdstraat op en blaas met je bellenblazer "
             "(rechtsklik) de emmers van de zes Bikinibodemguhs onder de gouden lichtzuilen. Daarna naar de Bellenkom in het noorden.",
             "De Hoofdstraat en de Bellenlaan, achter de stadspoort"),
        11: ("Ik ben een Guhfy Guhber",
             "Plankmika staat op het podium van de Bellenkom en wil zich kronen. Klim via het trapje links of rechts vooraan het "
             "podium op: tijd voor het hardste nummer dat Bikinibodem ooit hoorde.",
             "Het podium van de Bellenkom, aan het eind van de Bellenlaan"),
}}
EXTRA = {LIJN: {
        "10_b": ("Iedereen is vrij! Loop met je hele stoet de Bellenlaan af naar het noorden, de Bellenkom in: het grote ovale stadion.",
                 "De Bellenkom, het stadion in het noorden van Bikinibodem"),
        "klaar": ("De kroon is terug, Meneer Krabguh is ontdooid en Sponsguh is manager. En Sponsguh en Gerrit wonen nu een beetje "
                  "bij jou. Vahoeg!",
                  "Bikinibodem"),
}}
KORT = {LIJN: {
        "9": "Naar de kop van de pier",
        "10": "Blaas de emmers eraf",
        "10_b": "Naar de Bellenkom!",
        "11": "Klim op het podium",
        "klaar": "Vahoeg!",
}}

# =====================================================================================================================
# the texts of the slice (gui.guhs.einde2_sp_d2_finale.*)
# =====================================================================================================================
EMMER_GUHS = 6
TEKSTEN = {
    # hints (StapHint; %s = the direction)
    G + "hint.pier": "De kroon moet naar huis, en snel! Loop naar de kop van de lange pier, %s: oostwaarts voorbij het strand, de "
                     "leegte in. Prinses Mindy-guh zwaait daar al.",
    G + "hint.emmers": "Achter de stadspoort lopen Bikinibodemguhs met een emmer op hun hoofd, %s. Ga naar de gouden lichtzuilen en "
                       "blaas met je bellenblazer (rechtsklik) de emmers eraf.",
    G + "hint.bellenkom": "Iedereen is vrij! Loop naar de Bellenkom, %s: de Bellenlaan af naar het noorden.",
    G + "hint.podium": "Plankmika staat op het podium van de Bellenkom, %s. Klim via het trapje links of rechts vooraan het podium op.",
    # the ride (chat: travel banter is fine there)
    G + "rit.op": "David Hasselguh duikt onder de pier. Hop, op zijn rug!",
    G + "rit.weer": "<David Hasselguh> Daar zijn jullie weer! Klim maar op, ik zwem nog steeds die kant op.",
    G + "rit.mislukt": "David Hasselguh kan je nu niet dragen. Ga nog eens op de kop van de pier staan.",
    G + "rit.1": "<David Hasselguh> Zitten jullie goed? Niet aan mijn rugharen trekken, die zijn net gekamd.",
    G + "rit.2": "<Patguh> Kijk! Daar beneden! Onze huizen zijn zo klein! ...O nee, dat is een kruimel.",
    G + "rit.3": "<Sponsguh> Hou vol, Meneer Krabguh! We komen eraan met uw kroon en uw ijsvrije toekomst!",
    G + "rit.4": "<David Hasselguh> Ik zet jullie af bij de steiger voor de stadspoort. Het laatste stukje rennen jullie zelf. Ik ben moe van al dat knap zijn.",
    G + "rit.blijf": "Blijf zitten, njeg: David Hasselguh zwemt je naar huis.",
    # the bucket game
    G + "spel.titel": "Emmerhelmen!",
    G + "spel.regel": "Blaas met je bellenblazer de emmers van %s guhs",
    G + "spel.teller": "Emmers eraf: %s/%s",
    G + "spel.raak": "Plop! Emmer eraf!",
    G + "spel.klaar.titel": "Iedereen vrij!",
    G + "spel.klaar.onder": "Op naar de Bellenkom!",
    G + "spel.naar_bellenkom": "Op naar de Bellenkom, in het noorden",
    G + "spel.blazer": "Sponsguh duwt je een bellenblazer in je poten: \"Richten op de emmer, en blazen maar!\" (rechtsklik)",
    G + "spel.blazer_weer": "Patguh vond je bellenblazer terug in een plas saus. Hier, njeg!",
    G + "spel.mis": "Mis! Kom iets dichterbij en richt op de emmer.",
    # what each freed guh says (one line each, in order of the markers d2f_emmer_1..6)
    G + "vrij.1": "<Bikinibodemguh> Huh? Waar ben ik? Waarom ruik ik naar emmer?",
    G + "vrij.2": "<Bikinibodemguh> Ik zag de hele tijd alleen maar mijn eigen neus. Dank je, njeg!",
    G + "vrij.3": "<Bikinibodemguh> Ik moest steeds 'Plankmika is de baas' zeggen. Ik vond het een stom liedje.",
    G + "vrij.4": "<Bikinibodemguh> Mijn oortjes! Mijn oortjes zijn er nog! Vahoeg!",
    G + "vrij.5": "<Bikinibodemguh> Eindelijk! Ik moet zo nodig plassen. ...Na het concert.",
    G + "vrij.6": "<Bikinibodemguh> Ik loop met je mee! Ik kan heel hard juichen, luister maar: JUUUH!",
    # small talk on a click during the steps of this slice
    G + "klik.sponsguh": "Wat een avontuur! Nog even en ik word helemaal onsponsachtig spannend, njeg.",
    G + "klik.patguh": "Ik loop gewoon achter jou aan. Dat is mijn plan. Al mijn hele leven.",
    G + "klik.emmer": "Plankmika... is... de... baas... (de emmer galmt een beetje)",
    G + "klik.vrij": "Ik loop met je mee naar de Bellenkom!",
    G + "klik.plankmika": "Nog even, en IK ben koning van Bikinibodem! Muahaha! ...Waarom kijk je zo?",
    G + "klik.bewoner": "Psst. Ik heb me verstopt achter mijn brievenbus. Er loopt hier iemand rond met emmers!",
    # after the story: every story character a line or two (gui.guhs.einde2_sp_d2_finale.na.<cast>.<1|2>)
    G + "na.meneer_krabguh.1": "Ah, de held van de kroon! Een Knabbelburger? Houd kaasknabbels in je poot en klik me aan: drie knabbels per burger. Niet onderhandelen.",
    G + "na.meneer_krabguh.2": "Ik heb nooit meer zo'n koude dag gehad. Brrr. Ik slaap nu met drie dekentjes.",
    G + "na.krabguh.verkocht": "<Meneer Krabguh> Eén Knabbelburger, vers van de grill. Het geheime ingrediënt blijft geheim, njeg!",
    G + "na.krabguh.te_weinig": "<Meneer Krabguh> Drie kaasknabbels, jongen. Geen twee. Geen tweeënhalf. Drie.",
    G + "na.sponsguh.1": "Manager zijn is geweldig! Ik heb al zeven vergaderingen gehouden. Met mezelf.",
    G + "na.sponsguh.2": "Kom je zo nog een burger bakken? De grill mist je, njeg.",
    G + "na.patguh.1": "Ik ben nu de officiële beste klant. Ik heb een pasje. Het is een stukje karton.",
    G + "na.patguh.2": "Ik oefen nog steeds op de gitaar. Hij is nog steeds van karton.",
    G + "na.parel_guh.1": "Papa zegt dat jij hem hebt ontdooid. Ik zeg dat ik hem nu een sjaal ga breien!",
    G + "na.parel_guh.2": "Die emmerhelmen waren echt NIET in de mode.",
    G + "na.octo_mika.1": "Ik zat zo lekker rustig onder die emmer. Niemand die tegen me praatte.",
    G + "na.octo_mika.2": "Dat concert? Ik heb het niet gehoord. Ik hoor niks. La la la.",
    G + "na.octoliam.1": "Hmpf. Rocken. Dat kan iedereen. Ik zou het ook kunnen. Als ik zou willen.",
    G + "na.plankmika.1": "Ik zat niet in een emmer. Jij zat in een emmer! ...Wacht, dat was ook niet zo.",
    G + "na.plankmika.2": "Plan A tot en met Z zijn op. Ik begin morgen aan Plan Ä.",
    G + "na.juf_pufguh.1": "Ik hoorde dat jullie op een reuzenguh hebben gereden. Zonder rijbewijs! Ik word al bol als ik eraan denk.",
    G + "na.bikinibodem_guh.1": "Ik kan nog steeds niet geloven dat ik een emmer op had. Ik voel nog een randje op mijn oortjes.",
    G + "na.bikinibodem_guh.2": "Dat concert! Ik heb de hele nacht 'Guhfy Guhber' gezongen. Mijn buren ook. Niet vrijwillig.",
    G + "na.cycloop_mika.1": "Mijn winkel verkoopt nu alleen nog natte souvenirs. Die drogen wel. Ooit.",
    G + "na.david_hasselguh.1": "Nog een ritje? Ik zwem toch altijd wel ergens heen. Ik train wel even door, kijk maar.",
    G + "na.loopgraafmonster.1": "Rrrraaah! ...Sorry, gewoonte. Hoe ging dat liedje ook alweer? La la laaa.",
    G + "na.gerrit.1": "Njeg.",
    # the rewards
    G + "beloning": "Sponsguh deel 2 is af! Je kreeg: Sponsguh als maatje, Gerrit de slak, Plankmika's antennes voor je guhs en de titel Guhfy Guhber. Meneer Krabguh verkoopt je voortaan Knabbelburgers voor kaasknabbels.",
    G + "beloning.al": "Sponsguh en Gerrit komen je opzoeken, njeg!",
    # the bubble blaster
    f"item.guhs.{ITEM}": "Bellenblazer",
    f"item.guhs.{ITEM}.lore": "Eén goed geblazen bel en elke emmer vliegt eraf. Werkt ook gewoon voor de lol.",
    f"item.guhs.{ITEM}.tooltip": "Rechtsklik: een stroom zeepbellen.",
    # clothes source (the Kleding tab) and the title
    "gui.guhs.kledingbron.einde2_sp_d2_finale": "Sponsguh deel 2: De kroon van koning Neptuguh",
    f"gui.guhs.titel.{TITEL}": "Guhfy Guhber",
    f"gui.guhs.titels.hint.{TITEL}": "Rock zo hard dat de emmers eraf vliegen (Sponsguh deel 2).",
}

# =====================================================================================================================
# the scenes: naam -> (titel, {key: tekst}, {acteur: naam}). The narrator speaks as "verteller" (De Verteller, with a
# French touch); title cards are a black fade with his line. Every spoken line waits for a click (1.4.4).
# =====================================================================================================================
NAMEN = {"verteller": "De Verteller", "sponsguh": "Sponsguh", "sponsguh_m": "Sponsguh", "patguh": "Patguh", "mindy": "Prinses Mindy-guh",
         "david": "David Hasselguh", "karen": "Karen", "plankmika_stem": "Plankmika (door de luidspreker)", "plankmika": "Plankmika",
         "neptuguh": "Koning Neptuguh", "neptuguh_k": "Koning Neptuguh", "krabguh": "Meneer Krabguh", "octo_vrij": "Octo Tentakel",
         "gerrit": "Gerrit"}


def _namen(*wie):
    return {w: NAMEN[w] for w in wie}


SCENES = {
    "hasselguh": ("Een lift naar huis", {
        "mindy": "Daar zijn jullie! En... is dat de kroon van mijn vader?!",
        "kroon": "Gered uit de souvenirwinkel! Met maar een heel klein beetje uitdroging, njeg.",
        "rozijn": "Ik ben nog steeds een beetje een rozijntje.",
        "haast": "Snel! Papa wil zijn kroon terug voordat de zeepbelzon ondergaat. Anders blijft Meneer Krabguh voor altijd een ijslolly.",
        "paniek": "Maar Bikinibodem ligt helemaal aan de andere kant van de loopgraaf! Lopend halen we dat NOOIT!",
        "vriend": "Daarom bel ik een vriend.",
        "lift": "Iemand een lift nodig? Ik zwem toch die kant op.",
        "berg": "Wauw. Hij is net een berg. Een berg met spieren.",
        "train": "Dank je. Ik train elke dag. Drie keer. Klim maar op en houd je oortjes vast.",
        "vahoeg": "Vahoeg! Naar huis!",
    }, _namen("mindy", "sponsguh", "patguh", "david")),
    "thuis": ("Emmers op elk hoofd", {
        "titel": "Een ritje later, njeg... Bikinibodem!",
        "ruik": "Thuis! Ik ruik de grill al! En... nog iets. Ruikt dat naar... metaal?",
        "mode": "Waarom heeft iedereen een emmer op zijn hoofd? Is dat nu mode?",
        "piep": "PIEP. Aandacht, burgers van Bikinibodem. Dit is Karen, namens de nieuwe baas.",
        "muahaha": "MUAHAHA! Jullie dragen allemaal een Plankmika-emmerhelm! Nu doen jullie precies wat ik zeg!",
        "limonade": "Hij bedoelt: kom naar de Bellenkom. Daar kroont hij zichzelf tot koning. Er is limonade.",
        "karen": "Karen! Niet de limonade verklappen!",
        "blazer": "Geen tijd te verliezen! Hier, mijn bellenblazer! Eén goed geblazen bel en die emmer vliegt eraf.",
        "plan": "Jij bevrijdt de guhs, en dan rennen we met z'n allen naar de Bellenkom!",
        "aanmoedigen": "En ik... ik ga heel hard aanmoedigen.",
    }, _namen("verteller", "sponsguh", "patguh", "karen", "plankmika_stem")),
    "guhfy_guhber": ("Ik ben een Guhfy Guhber", {
        "te_laat": "Ha! Daar zijn jullie eindelijk. Te laat! Heel Bikinibodem draagt mijn emmers. Ik ben nu de baas van ALLES!",
        "zes": "Niet iedereen, Plankmika! Wij hebben er zes vrijgeblazen!",
        "honderden": "Zes? Ha! Er staan er nog honderden! Ze luisteren alleen naar mij. Tenzij... nee. Dat bestaat niet.",
        "wat": "Wat bestaat niet?",
        "onzin": "Muziek die zo hard rockt dat een emmer er vanzelf afspringt. Pure onzin! Hahaha!",
        "idee": "...Heb jij je trommel nog? Patguh, pak een gitaar! Hij is van karton, maar dat maakt niet uit!",
        "voor_jullie": "Bikinibodem! Dit nummer is voor jullie!",
        "nee": "NEE! Mijn emmers! Mijn prachtige, glimmende emmers!",
        "wie": "WIE durft er in MIJN stad zo hard te rocken?",
        "uw_kroon": "Koning Neptuguh! Uw kroon! We hebben hem helemaal uit Schelpenstad gehaald!",
        "maar_wie": "Mijn kroon! Maar... wie heeft hem dan gestolen, als het die arme Krabguh niet was?",
        "opgenomen": "PIEP. Plankmika. Het was Plankmika. Ik heb alles opgenomen. In drie camerastandpunten.",
        "weg": "Karen!! ...Eh. Ik ga even... ergens anders heen.",
        "vrij": "HAHAHA! Wat een lef, dat kleintje. Krabguh, je bent vrij!",
        "brrr": "Brrr! Wat heb ik gemist? Waren er klanten? Hebben ze betaald?",
        "manager": "Sponsguh, jongen... jij hebt alles gered. Jij wordt de manager van de nieuwe Krokante Knabbel!",
        "ik": "IK?! MANAGER?! Ik ga flauwvallen. Nee, ik ga niet flauwvallen. Ik ga JUICHEN!",
        "klant": "En ik word de klant! De allerbeste klant!",
        "emmer_terug": "Geweldig. Nu heeft hij ook nog een pet. Kan iemand mijn emmer teruggeven?",
    }, _namen("plankmika", "sponsguh", "sponsguh_m", "patguh", "neptuguh", "neptuguh_k", "karen", "krabguh", "octo_vrij")),
    "bedankt": ("Bedankt, maatje", {
        "titel": "Na het feest, njeg...",
        "collega": "Jij was de beste collega, rij-maatje, emmerblazer en drummer die ik ooit had.",
        "logeren": "Op mijn vrije dagen kom ik bij je logeren. Ik neem mijn eigen spatel mee!",
        "njeg": "Njeg.",
        "gerrit": "Gerrit wil ook mee! Hij zegt dat jouw huis vast lekker vochtig is.",
        "waar": "Ik kom ook! ...Waar woon je eigenlijk?",
        "slot": "Et voilà, njeg: Bikinibodem kreeg zijn kroon terug, Meneer Krabguh zijn warmte, en jij twee nieuwe huisgenoten.",
    }, _namen("verteller", "sponsguh_m", "patguh", "gerrit")),
}
# the look scenes of the hints (StapHint.kijkScene(id, true)): naam -> (titel, the one caption)
KIJK = {
    "pier": ("De pier van Schelpenstad", "De kop van de pier: daar wacht prinses Mindy-guh."),
    "emmers": ("Emmerhelmen", "Achter de stadspoort: guhs met een emmer op hun hoofd."),
    "podium": ("Het podium van de Bellenkom", "Het trapje naar het podium."),
}

# =====================================================================================================================
# FTB: one quest per step, in order (FTB_LINEAIR), after sp_d2_weg's; the last one of part 2 a gear with 12 knabbels
# =====================================================================================================================
FTB = [
    ("einde2_sp_d2_finale_k9", "Een lift naar huis",
     "De kroon is terug, maar de zeepbelzon gaat bijna onder! Prinses Mindy-guh roept een heel grote vriend op de pier van "
     "Schelpenstad.", "guhs:david_hasselguh_spawn_egg", "sponsguh_kroon_stap_10"),
    ("einde2_sp_d2_finale_k10", "Plankmika's emmerhelmen",
     "Heel Bikinibodem loopt met een emmer op zijn hoofd. Blaas ze eraf met je bellenblazer en loop met je stoet naar de "
     "Bellenkom.", f"guhs:{ITEM}", "sponsguh_kroon_stap_11"),
    ("einde2_sp_d2_finale_k11", "Ik ben een Guhfy Guhber",
     "Klim op het podium van de Bellenkom en rock zo hard dat elke emmer eraf vliegt. Koning Neptuguh krijgt zijn kroon, "
     "Meneer Krabguh ontdooit, en Sponsguh... wordt manager!", "guhs:knabbelburger", "sponsguh_kroon_stap_12"),
]


def ftb(fq):
    for i, (key, titel, text, icon, adv) in enumerate(FTB):
        laatste = i == len(FTB) - 1
        fq.q(key, titel, text, icon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 12 if laatste else 4),),
             shape="gear" if laatste else None, xp=200 if laatste else 0)


# =====================================================================================================================
# the bubble blaster's icon (16 x 16, drawn): a pink ring on a yellow stick, three bubbles
# =====================================================================================================================
def icoon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    geel, donker = (246, 214, 76, 255), (176, 140, 40, 255)
    for i in range(7):                                                   # the stick, from the bottom left
        d.point((1 + i, 14 - i), fill=geel)
        d.point((2 + i, 14 - i), fill=donker)
    d.ellipse([6, 2, 13, 9], outline=(236, 96, 160, 255))               # the ring
    d.ellipse([7, 3, 12, 8], outline=(255, 170, 210, 255))
    d.point((9, 5), fill=(200, 236, 255, 180))                          # soap film shine
    for (x, y, r) in ((13, 11, 1), (3, 3, 1), (14, 1, 0)):              # the bubbles
        if r:
            d.ellipse([x - r, y - r, x + r, y + r], outline=(170, 220, 255, 255))
        else:
            d.point((x, y), fill=(170, 220, 255, 255))
        d.point((x, y - r), fill=(255, 255, 255, 255))
    return img


# =====================================================================================================================
def build(h):
    for key, text in TEKSTEN.items():
        kit.lang(h, key, text)
    for naam, (titel, regels, namen) in SCENES.items():
        verhaal_motor.scene(h, S + naam, titel, regels, namen)
    for naam, (titel, tekst) in KIJK.items():
        verhaal_motor.scene(h, f"{S}kijk_{naam}", titel, {"tekst": tekst})
    h.save(icoon(), "item", f"{ITEM}.png")
    h.item_model(ITEM)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for naam in SCENES:
        if f"scene.guhs.{S}{naam}.titel" not in h.NL:
            problems.append(f"scene {naam}: no title")
    for naam in KIJK:
        if f"scene.guhs.{S}kijk_{naam}.tekst" not in h.NL:
            problems.append(f"look scene {naam}: no caption")
    for i in range(1, EMMER_GUHS + 1):
        if f"{G}vrij.{i}" not in h.NL:
            problems.append(f"no line of freed guh {i}")
    if not os.path.exists(os.path.join(h.TEX, "item", f"{ITEM}.png")):
        problems.append("no icon of the bubble blaster")
    # the Java side names the same scenes, look scenes and keys
    J = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "spd2finale")
    src = "".join(open(os.path.join(J, f), encoding="utf-8").read() for f in os.listdir(J) if f.endswith(".java"))
    for naam in list(SCENES) + [f"kijk_{k}" for k in KIJK]:
        if f'"{naam}"' not in src:
            problems.append(f"the Java side has no scene {naam}")
    if sorted(STAPPEN[LIJN]) != [9, 10, 11]:
        problems.append("the steps of this slice are 9, 10 and 11")
    if problems:
        raise SystemExit("einde2_sp_d2_finale self-check: " + "; ".join(problems))
