"""
bbq2 (ring-h2): chapter 2 of the Knabbelring, "De Raad van Guhrond" (DESIGN_130 4). Java: feature/ringh2.

  ring_h2_bouw.py   Guhvendel: the template of structure guhs:guhvendel (an elf house with three kaassaus waterfalls in a
                    rock cirque of the Worstenwoud), its cast, its self-check and its pictures
  ring_h2_wiki.py   the wiki entries (CONTRACT_130 2.4; not built here)
  here              the structure set (exactly one copy per world: the first halte of the story chain in the Barbecuether),
                    the questline's texts, the narrator card with its drawn map, the two cutscenes' texts, what everybody
                    says, the advancements, the game test room, the FTB section "De Raad van Guhrond"

The chapter owns no block, item or entity: the cast, the ring and the Rustvuurtje are ring-kern's (features/ring.py).
"""
import json
import os
import re

from features import bbq2
from features import ring
from features import ring_h2_bouw as bouw
from features import verhaal_motor
from features import wereld

# The chapter guhs_knabbelring is one chain in quest order (make_ftbquests.py FTB_LINEAIR): the first quest of this module
# comes after the last of ring_h1 by itself. Never give the first quest a deps (slice_ring-kern.md 10).
FTB_LINEAIR = True
FTB_SECTIES = [("ring_h2", "De Raad van Guhrond", "npc:guhrond", None)]

STRUCTUUR = bouw.NAAM
SALT = 21301701
LIJN = "ring_h2"
KAART = "ring_h2"
TITEL = "De Raad van Guhrond"

# =====================================================================================================================
# the questline (Java: RingH2Feature.LIJN, the steps of Guhvendel.java)
# =====================================================================================================================
UITLEG = ("In Guhvendel, het huis van Guhrond, komt een raad bijeen over de Knabbelring. Iedereen wil hem opeten. "
          "Iemand moet hem naar de Frituurberg brengen. Njeg, wie zou dat nou zijn?")
STAPPEN = [
    ("Reis naar Guhvendel",
     "Loop met Sam-guh door de Guhbarbecuether naar Guhvendel, het huis van Guhrond. Je Superkompas (Mijn verhaal) wijst de weg. "
     "Kom je een Rustvuurtje tegen, loop er dan even langs.",
     "Guhvendel, in het Worstenwoud van de Guhbarbecuether"),
    ("Guhrond heet je welkom",
     "Je bent er! Praat met Guhrond. Hij staat op de stoep van het grote witte huis met het groene dak.",
     "Guhvendel: de stoep van de hal"),
    ("Maak kennis met het gezelschap",
     "Maak kennis met Araguh, Leguhlas, Gimguh, Boromika, Merrie en Pippguh. Ze zijn overal in Guhvendel: bij de poort, in de tuin, "
     "in de hal en in de keuken. Guhrond weet wie je nog mist.",
     "Guhvendel: bij de poort, in de tuin, in de hal en in de keuken"),
    ("Luid de raadsbel",
     "Iedereen is er. Steek de brug over naar de raadskring (de ronde kring van zuilen) en luid de bel. Dan begint de Raad van Guhrond.",
     "Guhvendel: de bel in de raadskring"),
    ("Wie draagt de ring?",
     "De raad maakt ruzie over wie de ring mag opeten. Zeg tegen Guhrond, op zijn hoge zetel in de raadskring, dat jij hem wel naar de "
     "Frituurberg brengt.",
     "Guhvendel: Guhrond in de raadskring"),
    ("Op weg met het Reisgenootschap",
     "Je hebt proviand en acht metgezellen. Kijk gerust nog even rond. Praat met Guhdalf in de raadskring als je klaar bent voor vertrek.",
     "Guhvendel: Guhdalf in de raadskring"),
]
KLAAR = ("Het Reisgenootschap van de Knabbelring is op weg! Guhrond blijft thuis en zwaait. Kom gerust nog eens langs.", "Guhvendel")
KORT = {
    "0": "Loop naar Guhvendel (je Superkompas wijst de weg)",
    "1": "Praat met Guhrond op de stoep van de hal",
    "2": "Maak kennis met het hele gezelschap (zes guhs, overal in Guhvendel)",
    "3": "Luid de bel in de raadskring",
    "4": "Zeg tegen Guhrond dat jij de ring wel draagt",
    "5": "Praat met Guhdalf: klaar voor vertrek",
}

# =====================================================================================================================
# the narrator card (Java: Verteller "ring_h2", 4 lines)
# =====================================================================================================================
KAART_TITEL = "Hoofdstuk 2: De Raad van Guhrond"
KAART_REGELS = [
    "Achter het grillportaal ligt de Guhbarbecuether: rook, kooltjes en overal de geur van iets lekkers.",
    "Diep in het Worstenwoud, waar de kaassaus in watervallen van de rotsen stroomt, staat Guhvendel: het Laatste Knusse Huis.",
    "Daar woont Guhrond. Hij weet al drieduizend jaar wat er met de Knabbelring moet gebeuren. Hij zoekt alleen nog iemand die het doet.",
    "Loop erheen met Sam-guh. Je Superkompas wijst de weg. En niet aan de ring likken, njeg!",
]


def kaart():
    """The drawn map of the card: from the grill portal through the smoking plain to the house under the falls."""
    k = verhaal_motor.Kaart(seed=17)
    d = k.d
    rots, saus, mosterd = (120, 100, 92, 255), (236, 170, 52, 255), (214, 180, 78, 255)
    # the plain around the portal, and the mustard-yellow Worstenwoud in the north-east
    k.land([(8, 150), (14, 96), (58, 78), (110, 92), (150, 128), (132, 152)], (190, 172, 150, 255))
    k.land([(96, 88), (120, 40), (176, 18), (236, 30), (248, 96), (214, 132), (150, 128)], mosterd)
    # the sauce sea in the south-east
    k.water([(150, 152), (160, 134), (214, 136), (250, 110), (252, 152)], saus)
    # the grill portal (a dark frame with a glow) with the road away from it
    d.rectangle((26, 112, 40, 132), fill=(60, 48, 46, 255), outline=k.INKT)
    d.rectangle((29, 115, 37, 131), fill=(244, 150, 50, 255))
    # smoking coals on the plain
    for (x, y) in ((62, 122), (84, 108), (104, 132), (52, 98)):
        d.ellipse((x - 3, y - 2, x + 3, y + 2), fill=(70, 56, 54, 255), outline=k.INKT)
        d.line((x, y - 3, x + 1, y - 7, x - 1, y - 10), fill=(120, 110, 108, 255))
    # the worst trees of the forest: fat brown sausages with a dab of mustard
    for (x, y) in ((128, 62), (146, 96), (164, 44), (226, 60), (214, 104), (118, 82), (236, 88), (140, 34)):
        d.rounded_rectangle((x - 2, y - 11, x + 2, y), radius=2, fill=(150, 72, 44, 255), outline=k.INKT)
        d.point((x, y - 12), fill=(244, 206, 70, 255))
        d.point((x - 1, y - 12), fill=(244, 206, 70, 255))
    # Guhvendel: a horseshoe of rock, three falls, the white house with its green roof in the middle
    gx, gy = 188, 74
    d.arc((gx - 26, gy - 30, gx + 26, gy + 16), 160, 380, fill=k.INKT, width=9)
    d.arc((gx - 26, gy - 30, gx + 26, gy + 16), 162, 378, fill=rots, width=7)
    for fx in (gx - 14, gx, gx + 14):
        top = gy - 28 if fx == gx else gy - 23
        d.line((fx, top, fx, gy - 6), fill=saus, width=3 if fx == gx else 2)
        d.ellipse((fx - 3, gy - 8, fx + 3, gy - 4), fill=saus)
    d.line((gx, gy - 4, gx, gy + 20), fill=saus, width=2)
    d.rectangle((gx - 12, gy + 0, gx - 4, gy + 8), fill=(244, 240, 232, 255), outline=k.INKT)
    d.polygon([(gx - 14, gy + 0), (gx - 8, gy - 7), (gx - 2, gy + 0)], fill=(84, 170, 148, 255), outline=k.INKT)
    d.ellipse((gx + 4, gy + 0, gx + 13, gy + 8), fill=(244, 240, 232, 255), outline=k.INKT)
    for i in range(4):
        d.point((gx + 6 + i * 2, gy + 4), fill=k.INKT)
    k.pad([(42, 124), (70, 112), (98, 104), (128, 100), (160, 104), (184, 98)])
    k.kruis(gx, gy + 22)
    k.kompasroos(24, 28)
    return k


# =====================================================================================================================
# the two cutscenes (Java: RingH2Scenes)
# =====================================================================================================================
NAMEN = {"guhrond": "Guhrond", "guhdalf": "Guhdalf", "araguh": "Araguh", "leguhlas": "Leguhlas", "gimguh": "Gimguh", "boromika": "Boromika",
         "merrie": "Merrie", "pippguh": "Pippguh", "sam": "Sam-guh"}
SCENE_RAAD = {
    "begin": "De bel galmt door Guhvendel. De Raad van Guhrond komt bijeen.",
    "welkom": "Vreemdelingen uit verre streken, oude vrienden. Jullie zijn hier vanwege één knabbel. Eén héle lekkere.",
    "leg": "Leg de ring op de steen, kleintje.",
    "ring": "Daar ligt hij. Hij ruikt naar gesmolten kaas. Iedereen slikt.",
    "geef": "Dus het is waar. De Knabbelring! Geef hem aan mij. Ik eet hem wel op. Voor de veiligheid, njeg.",
    "boromika": "BOROMIKA! Pootjes thuis.",
    "frituur": "De ring mag niet zomaar worden opgegeten. Hij moet gefrituurd worden in de Frituurberg, waar hij gebakken is. En dan eerlijk gedeeld.",
    "wachten": "Waar wachten we dan nog op?",
    "tand": "AU! Mijn tand! Die ring is keihard, njeg!",
    "nul": "Dat is knabbel nummer nul voor jou, Gimguh. Ik sta nog steeds op zeventien.",
    "half": "Zeventien en een half! Die halve telt wél!",
    "wandel": "Men wandelt niet zomaar naar de Frituurberg. Daar is een Oog dat nooit knippert. Nou ja. Bijna nooit.",
    "ik": "Ik draag hem wel. Ik ben tenslotte bijna koning.",
    "ogen": "Mijn elfenogen zagen hem het eerst!",
    "stukken": "Ik hak hem in stukken. Eerlijk! Het grootste stuk is voor mij.",
    "dwazen": "Dwazen! Er wordt hier níét gesnoept voor het eten!",
    "eind": "Ze ruzieden en ruzieden. De ring lag er maar. Iemand moest iets zeggen...",
}
SCENE_GENOOTSCHAP = {
    "ik": "Ik neem de ring wel mee! Al weet ik de weg niet.",
    "stil": "Het werd heel stil. Zelfs de kaassaus hield even op met klateren.",
    "last": "Ik help je deze last te dragen, zolang jij hem dragen moet. En ik draag de koekjes.",
    "zwaard": "Als ik je met mijn leven kan beschermen, dan doe ik dat. Je hebt mijn zwaard.",
    "boog": "En mijn boog.",
    "bijl": "En mijn bijl! En mijn ene goede tand.",
    "broodjes": "Jij draagt ons aller lot, kleintje. Als dit de wil van de raad is, dan draag ik de broodjes.",
    "hee": "Hé! Meneer gaat nergens heen zonder mij, njeg!",
    "scheiden": "Nee, jullie zijn inderdaad niet te scheiden. Zelfs niet als hij voor een geheime raad is uitgenodigd en jij niet.",
    "ook": "Wij gaan ook mee! Je moet ons in een zak stoppen om ons tegen te houden.",
    "verstand": "Je hebt trouwens iemand met verstand nodig op zo'n... tocht. Zoektocht. Ding.",
    "afvallen": "Dan val jij dus af, Pip.",
    "negen": "Negen metgezellen. Het zij zo. Jullie zijn het Reisgenootschap van de Knabbelring!",
    "waarheen": "Vahoeg! ... Waar gaan we heen?",
}

# =====================================================================================================================
# what everybody says (quest.guhs.ringh2.*; Java: GuhvendelRol, Guhvendel)
# =====================================================================================================================
T = "quest.guhs.ringh2."
PRAAT = {
    "en": " en ",
    "aankomst": "Guhvendel! Het Laatste Knusse Huis. Hier ben je veilig: geen Mika, geen Oog, alleen heel veel kaassaus.",
    "sam.aankomst": "Kijk nou toch, die watervallen! Van échte kaassaus. Ik zet alvast een pannetje bij het Rustvuurtje, njeg.",
    "kennis.teller": "Kennisgemaakt: %s van de %s",
    "kennis.klaar": "Je kent nu iedereen. Luid de bel in de raadskring, aan de overkant van de brug!",
    "bel.te_vroeg": "De raad begint pas als je iedereen kent. Je mist nog: %s.",
    "bel.eerst_guhrond": "Niet zo snel met die bel! Praat eerst met Guhrond, op de stoep van de hal.",
    "bel.galm": "De bel galmt over de watervallen. Mooi geluid, njeg.",
    "raad.na": "Zeg tegen Guhrond dat jij de ring wel draagt. Hij zit op zijn hoge zetel in de raadskring.",
    "genootschap.na": "Guhrond geeft je proviand voor onderweg: kaasknabbels en twee stoofpotjes van het huis. Niet alles in één keer opeten!",
    "genootschap.hint": "Praat met Guhdalf als je klaar bent voor vertrek.",
    "klaar": "Hoofdstuk 2 is klaar! Het Reisgenootschap is op weg naar de Mijnen van Knabbelmoria. Je reiskaart staat in de Guhdex.",
    # Guhrond
    "guhrond.welkom.0": "Welkom in Guhvendel, ringdrager. Ik ben Guhrond. Dit huis is drieduizend jaar oud. De kaassaus ook. Proef maar niet.",
    "guhrond.welkom.1": "Vanavond is er raad over die knabbel in je zak. Maak eerst kennis met de anderen. Ze lopen hier overal rond en eten mijn voorraad op.",
    "guhrond.welkom.hint": "Maak kennis met Araguh, Leguhlas, Gimguh, Boromika, Merrie en Pippguh.",
    "guhrond.nog": "Je hebt nog niet met iedereen kennisgemaakt. Je mist nog: %s. Kijk bij de poort, in de tuin, in de hal en in de keuken.",
    "guhrond.bel": "Iedereen is er. Luid de bel in de raadskring, aan de overkant van de brug. Dan komen ze vanzelf. Meestal.",
    "guhrond.wie": "Ze ruziën al uren. Iemand moet de ring naar de Frituurberg brengen. Iemand met kleine pootjes en een groot hart. Wie o wie?",
    "guhrond.nog_niet": "Neem de tijd. Ze ruziën nog wel even door. Dat kunnen ze drieduizend jaar volhouden, njeg.",
    "guhrond.vertrek": "Negen metgezellen. Ga met Guhdalf mee als je klaar bent. En neem je bord mee, die krijg ik anders nooit terug.",
    # Guhdalf
    "guhdalf.voor.0": "Ha, daar ben je. Heb je de ring nog? Niet opgegeten? Ook geen hoekje? Mooi zo, njeg.",
    "guhdalf.voor.1": "Guhrond en ik kennen elkaar al eeuwen. Hij heeft nog een koekenpan van mij.",
    "guhdalf.raad": "Ze luisteren niet naar een oude tovenaar. Zeg jij maar iets tegen Guhrond. Je weet wel wat.",
    "guhdalf.vertrek": "Onze weg gaat over de Houtskoolvlakte naar de Mijnen van Knabbelmoria. Het is er donker en er woont iets met heel veel trek. Ben je klaar voor vertrek?",
    "guhdalf.nog_niet": "Kijk gerust nog even rond. Een tovenaar vertrekt nooit te vroeg. Hij vertrekt precies als de koekjes op zijn.",
    "guhdalf.op_weg": "Dan gaan we! Het Reisgenootschap loopt met je mee, ook als je ze niet ziet. Volg je Superkompas, njeg.",
    # Araguh (at the gate)
    "araguh.les": "Ik ben Araguh. Ze noemen mij Guhstapper. Wie met mij reist, moet kunnen sluipen. Laat maar zien: buk (sluipen) en klik mij dan nog eens aan.",
    "araguh.niet_gebukt": "Je staat nog rechtop. Zo ziet het Oog je van drie bergen ver. Buk, en klik mij dan aan.",
    "araguh.kennis": "Kijk, dát is sluipen. Buik in, oren plat en geen 'njeg' zeggen. Dat komt nog van pas bij de Zwarte Roosterpoort.",
    "araguh.praat": "Ik hou de poort in de gaten. En de koekjestrommel. Vooral de koekjestrommel.",
    "araguh.bel": "De bel? Ik kom eraan. Sluipend. Je hoort me niet aankomen.",
    "araguh.raad": "Ik zeg alleen: wie de ring draagt, moet kunnen sluipen. En dat kan hier maar één iemand. Twee, met mij erbij.",
    "araguh.klaar": "Je hebt mijn zwaard. En mijn lunchpakket, als je lief bent.",
    # Leguhlas and Gimguh (at the knabbel pile in the garden)
    "leguhlas.kennis": "Ik ben Leguhlas, van de boomguhs. Mijn elfenogen hebben vandaag al zeventien knabbels gevonden. Die dwergguh daar zegt zeventien en een half. Halve tellen niet.",
    "leguhlas.praat": "Zie je die stapel? Zeventien van mij. De rest lag er al.",
    "leguhlas.bel": "Ik hoorde de bel al voordat hij luidde. Elfenoren, njeg.",
    "leguhlas.raad": "Mijn elfenogen zagen hem het eerst. Dus is hij van mij. Zo werkt dat.",
    "leguhlas.klaar": "Je hebt mijn boog. Ik schiet er alleen kaas mee. Plakjes.",
    "gimguh.kennis": "Gimguh, zoon van Gluhin! Zeventien en een half, heb ik. Die halve lag onder het bankje en die telt wél. Vraag maar aan niemand.",
    "gimguh.praat": "Niemand gooit zomaar een dwergguh. Maar je mag mij wel een knabbel toegooien.",
    "gimguh.bel": "Een raad! Mooi. Daar is altijd eten bij.",
    "gimguh.raad": "Mijn tand doet nog pijn. Maar als ik hem in stukken hak, kan iedereen proeven. Ik eerst.",
    "gimguh.klaar": "Je hebt mijn bijl. Die is voor de kaas. Dikke plakken, vahoeg!",
    # Boromika (in the hall, at the shards of Knabsil)
    "boromika.kennis": "Ik ben Boromika. Ja, een Mika. Maar ik steel niet! Is dat hem, in je zak? Mag ik hem even vasthou... Nee. Nee, je hebt gelijk. Sorry, njeg.",
    "boromika.praat": "De scherven van Knabsil. Iemand heeft eraan geknabbeld. Ik was het niet. Deze keer niet.",
    "boromika.bel": "Ik kom zo. Ik kijk alleen nog héél even naar deze scherven. Kijken mag.",
    "boromika.raad": "Ik wil hem alleen bewaren. In mijn mond. Heel even. Voor de veiligheid!",
    "boromika.klaar": "Ik draag de broodjes. Er zitten er nog bijna evenveel in als vanmorgen.",
    # Merrie and Pippguh (in the kitchen)
    "merrie.kennis": "Ik ben Merrie, dat is Pippguh. Wij zijn hier voor het tweede ontbijt. En het derde. Wat een raad is weten we niet, maar er is vast eten bij.",
    "merrie.snoep": "Oeps. Er zat een kaasknabbel in je tas. Zát. Hij rolde er zó uit, recht in mijn mond.",
    "merrie.lege_tas": "Je tas is leeg! Hoe moet dat nou met het tweede ontbijt?",
    "merrie.praat": "Het buffet is van Guhrond. Wij passen erop. Van heel dichtbij.",
    "merrie.bel": "Was dat de etensbel? Nee? Jammer.",
    "merrie.raad": "Sssst! Wij zijn er niet. Wij zijn een struik.",
    "merrie.klaar": "Wij gaan mee! Iemand moet op de proviand passen. Van heel dichtbij.",
    "pippguh.kennis": "Pippguh! Aangenaam. Heb jij die bel al gezien, in de raadskring? Ik mag er niet aankomen van Guhrond. Ik weet niet waarom.",
    "pippguh.snoep": "Dat was Merrie. Ik heb alleen het papiertje opgegeten. O. Er zat geen papiertje om.",
    "pippguh.lege_tas": "Er zit niks in je tas. Ik heb drie keer gekeken. Met mijn pootjes.",
    "pippguh.praat": "Ik raak nergens aan. Dat theepotje viel helemaal vanzelf. Bijna.",
    "pippguh.bel": "Jij mag wél aan de bel komen? Dat is niet eerlijk, njeg.",
    "pippguh.raad": "Ik ben ook een struik. Een struik met trek.",
    "pippguh.klaar": "Waar gaan we eigenlijk heen? Maakt niet uit. Is er onderweg een tweede ontbijt?",
}
GUI = {
    "gui.guhs.ringh2.optie.ik": "Ik neem de ring wel mee!",
    "gui.guhs.ringh2.optie.nog_niet": "Eh... ik denk er nog even over na",
    "gui.guhs.ringh2.optie.op_weg": "Op weg!",
    "gui.guhs.ringh2.optie.rondkijken": "Ik kijk nog even rond",
    "gui.guhs.verhalen.ring_h2.nodig.kennis": "Kennisgemaakt met het gezelschap",
    "gui.guhs.verhalen.ring_h2.beloning.proviand": "Proviand van Guhrond: 6 kaasknabbels en 2 stoofpotjes",
    "gui.guhs.verhalen.ring_h2.beloning.genootschap": "Het Reisgenootschap van de Knabbelring: acht metgezellen voor onderweg",
}
# (name, parent, icon, frame, title, text): visible in the tab knabbelring, each with a hidden twin (Ring.behaald)
ADVANCEMENTS = [
    ("ring_h2_raad", "ring_gekregen", "minecraft:bell", "task", "De Raad van Guhrond", "Luid de raadsbel in Guhvendel en hoor wie de ring allemaal wil opeten"),
    ("ring_h2_genootschap", "ring_h2_raad", "guhs:ring_stoofpotje", "goal", "Het Reisgenootschap", "Neem de ring op je en vertrek met acht metgezellen uit Guhvendel"),
]
STRUCTUUR_TEKST = ("Guhvendel", "Het Laatste Knusse Huis: het elfenhuis van Guhrond onder drie kaassauswatervallen, in het Worstenwoud (Guhbarbecuether)")


# =====================================================================================================================
# the build
# =====================================================================================================================
def texts(h):
    for key, text in PRAAT.items():
        h.lang(T + key, text, text)
    for key, text in GUI.items():
        h.lang(key, text, text)
    verhaal_motor.verhaallijn(h, LIJN, TITEL, UITLEG, stappen=STAPPEN, klaar=KLAAR, kort=KORT)
    verhaal_motor.vertelkaart(h, KAART, KAART_TITEL, KAART_REGELS, kaart())
    verhaal_motor.scene(h, "ringh2_raad", "De Raad van Guhrond", SCENE_RAAD, namen=NAMEN)
    verhaal_motor.scene(h, "ringh2_genootschap", "Het Reisgenootschap", SCENE_GENOOTSCHAP, namen=NAMEN)
    for name, parent, icon, frame, titel, tekst in ADVANCEMENTS:
        bbq2.zichtbaar(h, "knabbelring", name, parent, icon, frame, titel, tekst)
        bbq2.verborgen(h, name)


GEBOUWD = {}     # the blocks of the template of this run (for the camera check of the self-check)


def structuur(h):
    s, problems = bouw.bouw(h)
    if problems:
        raise SystemExit("ring_h2: Guhvendel is not right:\n  " + "\n  ".join(problems[:40]))
    s.save(STRUCTUUR)
    GEBOUWD.clear()
    GEBOUWD.update(s.blocks)
    titel, tooltip = STRUCTUUR_TEKST
    # The cave floors of the Guhbarbecuether are rugged and the Worstenwoud is only about a twentieth of the ring: with the
    # flatness numbers of the other buildings (26-40 / 8-10) two of six seeds had NO spot at all (RingH2GameTests
    # ringh2GuhvendelVindtEenPlek). The template brings its own floor, foot and dome, so it only asks for a cave floor with
    # some room in the middle and floors within 8 blocks of it 8 blocks around.
    # exactly one per world (no random spread): the first of the story chain, in its own slice of the ring around 0,0; the
    # next chapters lie "rond" this one. In the Superkompas (tab barbecue) once the sluier is open for the player.
    wereld.bbq_structuur(h, STRUCTUUR, soort="grot", titel=titel, tooltip=tooltip, biomes=["worstenwoud"], salt=SALT, templates=[(STRUCTUUR, 1)],
                         gegarandeerd=dict(sector=12, min=250, max=700), voorrang=300, kompas="barbecue", grootte=16, vlak=16, hoogte=12)
    # the centre jigsaw is in layer 0 and the pool says where the ground really is (the trick of fossiel_mijn / toren_peper):
    # the floor of the cirque lands on the cave floor and the terrain is smoothed towards it

    def grond(pool, delta=bouw.G + 1):
        for e in pool["elements"]:
            el = {"element_type": "guhs:grond_single_pool_element"}
            el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
            el["ground_level_delta"] = delta
            e["element"] = el
    h.patch_json(f"{h.D}/worldgen/template_pool/{STRUCTUUR}/start.json", grond)
    # no monsters in the Last Homely House
    h.patch_json(f"{h.D}/worldgen/structure/{STRUCTUUR}.json",
                 lambda d: d.update(spawn_overrides={"monster": {"bounding_box": "piece", "spawns": []}}))
    verhaal_motor.sluier(h, STRUCTUUR)
    # the game test room: a bare floor
    t = h.Structure((25, 9, 25))
    for x in range(25):
        for z in range(25):
            t.set(x, 0, z, "minecraft:smooth_quartz")
    t.save("ringh2_test_kamer")


def camera_check(scenes_java, blocks):
    """Nobody has seen these scenes in the game yet, so at least this: no camera stands (or glides) inside a block of the
    house, nothing of the house stands between a camera and what it looks at, and no actor is put inside a block. The
    positions are read from RingH2Scenes.java: scene coordinates, relative to the stone table (bouw.KRING)."""
    import math
    problems = []
    ax, ay, az = bouw.KRING
    vrij = set(bouw.DUN) | {bouw.KETTING, bouw.LANTAARN}
    getal = r"(-?[\d.]+)"
    vec = rf"new Vec3\({getal}, {getal}, {getal}\)"

    def blok(x, y, z):
        b = blocks.get((ax + math.floor(x), ay + math.floor(y), az + math.floor(z)))
        return b[0] if b else None

    def dicht(x, y, z):
        """Something of the house fills this point (a slab or a cushion only its lower half)."""
        n = blok(x, y, z)
        if n is None or n in vrij:
            return False
        if n.endswith("_slab") or "kussen" in n:
            return (y % 1.0) < 0.5
        return True
    # 1. every position of the script: not inside a block (the ring waits inside the stone on purpose)
    for x, y, z in sorted({tuple(float(v) for v in m) for m in re.findall(vec, scenes_java)}):
        if (x, y, z) != (0.5, 0.25, 0.5) and dicht(x, y + 0.01, z):
            problems.append(f"scene position {(x, y, z)} is inside {blok(x, y + 0.01, z)}")
    # 2. the cameras, per scene
    for naam in ("raad", "genootschap"):
        a = scenes_java.index(f"private static Cutscene {naam}()")
        b = scenes_java.index("return s.registreer();", a)
        punten = []
        for m in re.finditer(rf"\.camera(Knip)?(Volgt)?\((\d+), {vec}, (?:{vec}|(\w+))\)", scenes_java[a:b]):
            knip, tijd = m.group(1), int(m.group(3))
            pos = tuple(float(v) for v in m.group(4, 5, 6))
            kijk = tuple(float(v) for v in m.group(7, 8, 9)) if m.group(7) else None
            punten.append((tijd, pos, kijk, bool(knip)))
        punten.sort()
        if len(punten) < 6:
            problems.append(f"scene {naam}: only {len(punten)} camera points were read")
        for i, (tijd, pos, kijk, knip) in enumerate(punten):
            stappen = [pos]
            if i + 1 < len(punten) and not punten[i + 1][3]:
                volgende = punten[i + 1][1]
                stappen += [tuple(pos[k] + (volgende[k] - pos[k]) * n / 12.0 for k in range(3)) for n in range(1, 12)]
            for (x, y, z) in stappen:
                raak = [blok(x + dx, y + dy, z + dz) for dx, dz in ((0, 0), (0.3, 0), (-0.3, 0), (0, 0.3), (0, -0.3)) for dy in (0, -0.3)
                        if dicht(x + dx, y + dy, z + dz)]
                if raak:
                    problems.append(f"scene {naam}: the camera of tick {tijd} is in or against {raak[0]} at {tuple(round(v, 1) for v in (x, y, z))}")
                    break
            if kijk:
                d = [kijk[k] - pos[k] for k in range(3)]
                lang = sum(v * v for v in d) ** 0.5
                n = int(lang / 0.2)
                for j in range(1, n):
                    f = j / n
                    if lang * (1 - f) < 0.9:
                        break       # (the last bit: what it looks at)
                    q = tuple(pos[k] + d[k] * f for k in range(3))
                    if dicht(*q):
                        problems.append(f"scene {naam}: the camera of tick {tijd} looks at {kijk} through {blok(*q)} at {tuple(round(v, 1) for v in q)}")
                        break
    return problems


def blok_check(h, blocks):
    """Every block of the template exists (a guhs block: its blockstate file; a vanilla one: in the 1.21.1 jar the templates
    are written for), and every property the blockstate file knows has a value it knows."""
    import zipfile
    problems = []
    jar = zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))
    bekend = {}

    def eigenschappen(name):
        if name not in bekend:
            ns, pad = name.split(":")
            try:
                if ns == "guhs":
                    data = json.load(open(f"{h.A}/blockstates/{pad}.json", encoding="utf-8"))
                else:
                    data = json.loads(jar.read(f"assets/minecraft/blockstates/{pad}.json"))
            except (OSError, KeyError):
                bekend[name] = None
                return None
            props = {}

            def neem(k, v):
                for deel in str(v).split("|"):
                    props.setdefault(k, set()).add(deel)
            for key in data.get("variants", {}):
                for paar in filter(None, key.split(",")):
                    k, v = paar.split("=")
                    neem(k, v)
            for part in data.get("multipart", []):
                wanneer = part.get("when", {})
                for w in wanneer.get("OR", wanneer.get("AND", [wanneer])):
                    for k, v in w.items():
                        neem(k, v)
            bekend[name] = props
        return bekend[name]
    gezien = set()
    for (name, props, _nbt) in blocks.values():
        sleutel = (name, tuple(sorted(props.items())))
        if sleutel in gezien:
            continue
        gezien.add(sleutel)
        props_bekend = eigenschappen(name)
        if props_bekend is None:
            problems.append(f"the template uses {name}, which has no blockstate file")
            continue
        for k, v in props.items():
            if k in props_bekend and v not in props_bekend[k]:
                problems.append(f"the template uses {name}[{k}={v}]: {k} is one of {sorted(props_bekend[k])}")
    return problems


def selfcheck(h):
    problems = []
    for key in ([T + k for k in PRAAT] + list(GUI) + [f"structure.guhs.{STRUCTUUR}", f"gui.guhs.verhalen.{LIJN}.kort.5", f"gui.guhs.verhaal.kaart.{KAART}.regel.3",
                                                       "scene.guhs.ringh2_raad.eind", "scene.guhs.ringh2_genootschap.waarheen"]):
        if key not in h.NL:
            problems.append(f"lang {key}")
    D = h.D
    for path in (f"{D}/structure/{STRUCTUUR}.nbt", f"{D}/structure/ringh2_test_kamer.nbt", f"{D}/worldgen/structure/{STRUCTUUR}.json",
                 f"{D}/worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json", f"{h.A}/textures/gui/verhaal/kaart_{KAART}.png"):
        if not os.path.exists(path):
            problems.append(path)
    if os.path.exists(f"{D}/worldgen/structure_set/{STRUCTUUR}.json"):
        problems.append("Guhvendel has a random-spread set: it must exist exactly once per world")
    pool = json.load(open(f"{D}/worldgen/template_pool/{STRUCTUUR}/start.json", encoding="utf-8"))
    if any(e["element"].get("element_type") != "guhs:grond_single_pool_element" for e in pool["elements"]):
        problems.append("the start pool has no ground_level_delta")
    # the Java side knows the same geometry and the same cast (feature/ringh2/Guhvendel.java), and plays the same lines
    pkg = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ringh2")
    java = open(os.path.join(pkg, "Guhvendel.java"), encoding="utf-8").read()
    for naam, (x, y, z) in (("KRING", bouw.KRING), ("BEL", bouw.BEL), ("MIDDEN", (bouw.CX, bouw.G + 1, bouw.CZ))):
        if f"{naam} = new BlockPos({x}, {y}, {z})" not in java:
            problems.append(f"Guhvendel.{naam} is not {(x, y, z)}")
    if f"KOM = {int(bouw.RAND) + 1}," not in java or f"KOEPEL = {bouw.KOEPEL};" not in java:
        problems.append(f"Guhvendel.KOM / KOEPEL are not {int(bouw.RAND) + 1} / {bouw.KOEPEL}")
    rijen = re.findall(r'new Bewoner\("(\w+)", GuhNpcEntity\.Kind\.(\w+), (\d+), (\d+), (\d+), ([\d.]+), ([\d.]+)f, (\d+), (\d+)\)', java)
    javaset = {(i, k.lower(), int(x), int(y), int(z), float(dy), float(yaw), int(van), int(tot)) for i, k, x, y, z, dy, yaw, van, tot in rijen}
    pyset = {(i, k, x, y, z, float(dy), float(yaw), van, tot) for (i, k, x, y, z, dy, yaw, van, tot) in bouw.BEWONERS}
    for b in sorted(pyset - javaset):
        problems.append(f"Guhvendel.BEWONERS misses {b}")
    for b in sorted(javaset - pyset):
        problems.append(f"Guhvendel.BEWONERS has {b}, the template does not")
    if f'PLEK = "{bouw.PLEK}"' not in java:
        problems.append("Guhvendel.PLEK is not the template's plek")
    scenes = open(os.path.join(pkg, "RingH2Scenes.java"), encoding="utf-8").read()
    problems += camera_check(scenes, GEBOUWD)
    problems += blok_check(h, GEBOUWD)
    for scene_id, regels in (("ringh2_raad", SCENE_RAAD), ("ringh2_genootschap", SCENE_GENOOTSCHAP)):
        a = scenes.index(f'"{scene_id}"')
        b = scenes.index("return s.registreer();", a)
        gebruikt = set(re.findall(r'\.zeg\(\d+, [\w".]+, "(\w+)"', scenes[a:b]))
        for key in sorted(gebruikt - set(regels)):
            problems.append(f"scene {scene_id} says '{key}', which has no text")
        for key in sorted(set(regels) - gebruikt):
            problems.append(f"scene {scene_id}: the text '{key}' is never said")
    rol = open(os.path.join(pkg, "GuhvendelRol.java"), encoding="utf-8").read() + java
    for key in re.findall(r'"quest\.guhs\.ringh2\.([\w.]+)"', rol):
        if key not in PRAAT:
            problems.append(f"the Java says quest.guhs.ringh2.{key}, which has no text")
    if f'Verteller.registreer(Guhvendel.KAART, {len(KAART_REGELS)},' not in open(os.path.join(pkg, "RingH2Feature.java"), encoding="utf-8").read():
        problems.append(f"the narrator card has {len(KAART_REGELS)} lines, RingH2Feature registers another number")
    if f"STAPPEN = {len(STAPPEN)};" not in java:
        problems.append(f"the questline has {len(STAPPEN)} steps, Guhvendel.STAPPEN says otherwise")
    if problems:
        raise SystemExit("ring_h2 self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    texts(h)
    structuur(h)
    selfcheck(h)


# =====================================================================================================================
# FTB: the section "De Raad van Guhrond" (chapter guhs_knabbelring; one quest per step, in order)
# =====================================================================================================================
def ftb(fq):
    """One quest per step of the questline, in story order (the chapter is linear: the first one follows ring_h1 by itself)."""
    quests = [
        ("ring_h2_reis", "Het Laatste Knusse Huis",
         "Je bent door het grillportaal. Loop met &dSam-guh&r door de Guhbarbecuether naar &6Guhvendel&r, het huis van Guhrond in het "
         "Worstenwoud. Je herkent het aan de drie watervallen van kaassaus. Het &6Superkompas&r (Mijn verhaal) wijst de weg.",
         "guhs:mosterd_blok"),
        ("ring_h2_guhrond", "Guhrond",
         "Praat met &dGuhrond&r. Hij staat op de stoep van het grote witte huis. Dat huis is drieduizend jaar oud. De kaassaus ook, njeg.",
         "minecraft:quartz_pillar"),
        ("ring_h2_gezelschap", "Het gezelschap",
         "Maak kennis met &dAraguh&r (bij de poort: hij wil zien of je kunt sluipen), &dLeguhlas&r en &dGimguh&r (in de tuin, bij hun "
         "stapel knabbels), &dBoromika&r (in de hal) en &dMerrie&r en &dPippguh&r (in de keuken, waar anders). Pas op je tas.",
         "guhs:block_of_kaasknabbels"),
        ("ring_h2_raad", "De Raad van Guhrond",
         "Luid de &6bel&r in de raadskring, de ronde kring van zuilen aan de overkant van de brug. Dan begint de raad. "
         "Iedereen wil de ring opeten. Gimguh probeert het zelfs.",
         "minecraft:bell"),
        ("ring_h2_ringdrager", "Ik neem de ring wel mee!",
         "De raad maakt ruzie. Zeg tegen &dGuhrond&r dat jij de Knabbelring naar de Frituurberg brengt. Je krijgt proviand mee. "
         "En acht metgezellen. Niemand had om acht gevraagd.",
         "guhs:knabbelring"),
        ("ring_h2_genootschap", "Het Reisgenootschap",
         "Praat met &dGuhdalf&r als je klaar bent voor vertrek. Daarna gaat de reis verder naar de &6Mijnen van Knabbelmoria&r. "
         "Guhrond blijft in Guhvendel wonen: je mag altijd terugkomen.",
         "guhs:ring_stoofpotje"),
    ]
    vorige = None
    for i, (key, titel, tekst, icon) in enumerate(quests, start=1):
        laatste = i == len(quests)
        fq.q(key, titel, tekst, icon, [fq.adv(f"{LIJN}_stap_{i}")], rewards=(("guhs:kaas_knabbels", 12 if laatste else 4),),
             deps=[vorige] if vorige else [], shape="gear" if laatste else None, xp=100 if laatste else 0)
        vorige = key
