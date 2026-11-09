"""
1.10 "Het Vadsrijk", zone 6 "Het Zijdeland" (U_BRIEF round U, slice u-z6verhaal). Java: feature/vadsrijk/zone6/verhaal.

Het Zijdeland is the first part of the Grote Vasteland (ZONE6_PLAN.md + the user decisions Z6-6 at its end): an old-Middle-East
inspired land of pink sandstone, dunes, oases, caravans and floating carpets, lv 60-70. Two cities: Rozenkloof (the cliff city,
arrival of airship line E) and Saffraanpoort (the river capital). Zone 6 uses the SAME quest system as zones 1-5
(feature/vadsrijk/npc); this generator adds the zone-6 data:
  - the NPC guhs of both cities and the five areas: names, idle lines, WHERE they stand (vadsrijk.npc.plek.npc.<id>);
  - the main line "De Sterrenkaravaan" (10 steps), 15 side quests -> data/guhs/vadsrijk/npc/quests_z6.json (Quests.BESTAND_Z6, read
    after zone 5) + texts; every objective says WHO + WHERE + WHAT and carries a "plek" for the tracker;
  - the puzzles (star beacons at night, the exam, the haggling, the cave song) -> data/guhs/vadsrijk/z6verhaal/puzzels.json;
  - quest items, hidden FTB advancements guhs:quest/vr_z6_*, FTB sections, Gids pages, tips, scene texts, the Verkenningsboek page.
Ids: NPCs and quests "z6_*" (plus the hub NPCs kk_poort_<stad> / bond_bord_<stad> of u-huizen / u-gilden), items "vr_z6_npc_*", own
texts "vadsrijk.z6verhaal.*". In-game text is Dutch only for now (English comes at the end, all at once): en_us gets the same Dutch.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_z6verhaal.py
"""
import json
import os
import random
import re

P = "vadsrijk.npc."             # (the shared quest system's keys)
Z = "vadsrijk.z6verhaal."       # (zone-6 own texts)
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone6", "verhaal")

_PLEIN = "op het Rozenplein, de bodem van de Rozenkloof bij de stervormige fontein"
_KLOOF = "in Rozenkloof, de kloofstad in de roze rotsen (westkust van het Zijdeland)"
_HAVEN = "op de Luchthaven van Rozenkloof, de ronde stenen meertoren op de noordrand van de kloof"
_BAZAAR = "in de Grote Bazaar, de overdekte straat op de zuidoever van Saffraanpoort"
_PALEIS = "in het Rozijnenpaleis op de noordoever van Saffraanpoort (de gouden uienkoepel)"
_SERAI = "in de Karavanserai bij de Westpoort van Saffraanpoort (het vierkante hof met de arcaden)"
_WACHT = "in de Sterrenwacht van Twinkel, de lavendel toren met de koperen kijker in het noordoosten van Saffraanpoort"
_TUIN = "in de Paradijstuin achter het Rozijnenpaleis (de vier vakken met kanalen, noordkant van Saffraanpoort)"
_STAD = "in Saffraanpoort, de hoofdstad aan de Saffraanrivier"

# ---------------------------------------------------------------------------------------------------------------------------
# NPCs: name, 3 idle lines, WHERE. Spots + clothes: Z6Npcs.java / Z6VerhaalPlekken.java.
# ---------------------------------------------------------------------------------------------------------------------------
NPCS = {
    # --- Rozenkloof -----------------------------------------------------------------------------------------------------------
    "z6_zandloper": ("Havenmeester Zandloper", [
        "Welkom op de Luchthaven van Rozenkloof! Hier kijk je zo de kloof in. Niet te ver voorover leunen.",
        "Ik tel de schepen met mijn zandloper. Eén schip per zandloper. Soms valt hij om. Dan tel ik opnieuw.",
        "Wil je reizen? Lijn E vliegt naar Hoogvads. En ik heb de reiskaart."],
        _HAVEN + ", bij de balie onder de mast met de vaandels"),
    "z6_roosje": ("Kloofmoeder Roosje", [
        "Ik ben de oudste guh van de kloof. Ik was er al toen deze rotsen nog zachtjes roze bloosden. Nu blozen ze nog steeds.",
        "De Schatkamer is uit één stuk rots gehouwen. Door mijn overgrootmoeder. Met een lepeltje. Het duurde even.",
        "'s Nachts keken we altijd naar de sterrenbakens. Nu zijn ze uit. De kloof is zo donker zonder."],
        "in de Schatkamer, de grote uitgehouwen gevel in de eindwand aan de oostkant van de Rozenkloof"),
    "z6_dadel": ("Oma Dadel", [
        "Ik ben Oma Dadel en dit is mijn karavaan. Klein maar lief. Net als ik. Nou ja, ik ben niet zo klein.",
        "Bobbel, mijn bultguh, draagt alles. Behalve mij. Ik loop graag. Dan heb ik een excuus voor een dutje.",
        "La-la-LAA-la-lo... Dat is het karavaanlied. Elke karavaan zingt het. Dan vinden de sterren je terug."],
        _PLEIN + ", bij de karavaan met de gestreepte tassen"),
    "z6_hobbel": ("Stalmeester Hobbel", [
        "Bultguhs! Ze hebben twee bulten. In de ene zit water. In de andere zit gezelligheid.",
        "Een bultguh loopt over zand alsof het een kussen is. Sneller dan elke Pluimguh. Zeg dat niet tegen een Pluimguh.",
        "Een bultguh heeft twee zitjes. Neem een vriend mee. Of een grote tas."],
        _PLEIN + ", in de bultguh-stal met de gestreepte luifel"),
    "z6_franje": ("Tapijtkoopman Franje", [
        "Tapijten! Om op te zitten, om op te liggen en, als je heel lief bent, om op te vliegen.",
        "Vliegen? Dat kan alleen een tapijt van sterrenzijde. Die maakt Knoopje in Saffraanpoort. Ik verkoop de gewone.",
        "Dit tapijt is honderd jaar oud. Dat zie je aan de koffievlek. Die is ook honderd jaar oud."],
        _PLEIN + ", in de tapijtwinkel met de opgerolde tapijten voor de deur"),
    "z6_roosmarijn": ("Tuinguh Roosmarijn", [
        "De hangende tuinen hangen over de randen van de richels. Ze hangen graag. Ik hang er soms bij.",
        "Water omhoog dragen is zwaar werk. Maar een roos die je bedankt, is het waard.",
        "Kijk uit voor de wandtrappen. Ze zijn steil. En ze tellen hardop terug als je naar beneden gaat."],
        "op de eerste richel van de Rozenkloof, bij de hangende tuinen boven de westelijke wandtrap"),
    "z6_stempel": ("Postguh Stempel", [
        "Post! Post voor de rotswoningen! Iedereen woont hier op een andere hoogte. Mijn pootjes zijn heel sterk.",
        "Ik heb een tas vol brieven en een hoofd vol trappen.",
        "Soms stuur ik een brief aan mezelf. Dan heb ik ook post."],
        _PLEIN + ", bij het postkantoortje met de gele deur onder de eerste richel"),
    "z6_bankier_kloof": ("Bankier Kopergulden", [
        "De bank van Rozenkloof zit in de rots. De veiligste kluis van het Zijdeland. Er zit vooral zand in. En geld.",
        "Je spullen zijn hier net zo veilig als in Hoogvads. Het is dezelfde kluis. Dat is magie. Of boekhouding.",
        "Rente? Ik geef je een dadel per jaar. Een grote."],
        _PLEIN + ", in de bank, de rotsgevel met de koperen deur"),
    "kk_poort_rozenkloof": ("Kwartiermeester Rotsdak", [
        "Een eigen huisje? In het Knuffelkwartier is nog plek. Ik breng je erheen.",
        "Rotswoningen zijn mooi, maar een eigen tuintje is mooier.",
        "Je buren in het Knuffelkwartier zijn heel aardig. Ik heb ze zelf uitgekozen."],
        _PLEIN + ", naast de fontein"),
    "bond_bord_rozenkloof": ("Bondenbord-guh Spijker", [
        "Zoek je een Knuffelbond? Op mijn bord hangen alle bonden die nieuwe leden zoeken.",
        "Samen reizen is gezelliger. En de Sterrendovende Toren doe je echt niet alleen.",
        "Prik je eigen briefje maar op. Ik heb punaises genoeg."],
        _PLEIN + ", bij het prikbord naast de bank"),
    # --- Saffraanpoort: the palace ------------------------------------------------------------------------------------------------
    "z6_rozijntje": ("Sultane Rozijntje", [
        "Welkom in mijn Spiegelzaal, ridder. Kijk niet te lang in de spiegels. Dan ga je jezelf aardig vinden.",
        "Saffraanpoort leeft van het water uit de Moederoase. En van thee. Vooral van thee.",
        "Een sultane moet streng zijn. Ik oefen elke ochtend. Het lukt nog niet."],
        "op de troon achterin de Spiegelzaal, onder de gouden koepel van het Rozijnenpaleis (noordoever van Saffraanpoort)"),
    "z6_kaneelstok": ("Grootvizier Kaneelstok", [
        "Ik ben de Grootvizier. Ik regel alles voor de Sultane. Ook dingen die niet geregeld hoeven te worden.",
        "Audiëntie? Dat kan. Na de thee. En na de thee daarna.",
        "Er zijn de laatste tijd twee van mij in het paleis gezien. Dat is vreemd. Er is er maar één van mij. Toch?"],
        _PALEIS + ", bij de iwan met de turkoois tegels voor de Spiegelzaal"),
    "z6_glans": ("Meester Glans", [
        "Ik poets de spiegels van de Spiegelzaal. Alle duizend. Elke dag. Ze glimmen als je erin kijkt. Ik ook.",
        "In een spiegel zie je alles, ridder. Ook wat er niet is.",
        "Ik ben maar een eenvoudige spiegelpoetser. Heel eenvoudig. Niks bijzonders. Doorlopen."],
        "in de Spiegelzaal van het Rozijnenpaleis, bij de spiegelboog links van de loper"),
    "z6_twinkel": ("Sterrenkijker Twinkel", [
        "De sterren zijn landkaarten die 's nachts aan gaan. Wie ze kan lezen, verdwaalt nooit.",
        "Mijn kijker is van koper. Hij is zo groot dat ik er een trapje voor nodig heb. En een kussen.",
        "Vijf bakens. Vijf sterren. Vijf keer zo donker als vroeger. Dat is te veel donker."],
        _WACHT + ", naast de grote koperen kijker"),
    "z6_dadel_serai": ("Oma Dadel", [
        "Hier rust mijn karavaan uit. De bultguhs drinken, de guhs eten, en ik doe een dutje. Zo gaat dat.",
        "De Karavanserai is het huis van alle karavanen. Er is altijd thee, en altijd iemand met een verhaal.",
        "La-la-LAA-la-lo. Neurie maar mee. Dan vinden de sterren ons."],
        _SERAI + ", bij de fontein in het midden van het hof"),
    # --- Saffraanpoort: the city ----------------------------------------------------------------------------------------------------
    "z6_knoopje": ("Tapijtweefster Knoopje", [
        "Ik weef tapijten. Gewone, mooie en vliegende. De vliegende moet je goed vasthouden.",
        "Een vliegend tapijt maak je van sterrenzijde. Zijde van de moerbeibomen, met een snufje sterrenstof.",
        "Mijn kwastjes waaien steeds weg. Ze willen ook vliegen."],
        _BAZAAR + ", in het naaiatelier met de tapijten aan de pijlers"),
    "z6_kardemom": ("Koopvrouw Kardemom", [
        "Kruiden! Kardemom, kaneel, saffraan en een geheim kruid dat naar knuffels ruikt.",
        "Afdingen hoort erbij. Maar wie lief afdingt, krijgt meer korting. Zo werkt dat in de bazaar.",
        "Bij mij koop je drankjes en lekkers voor onderweg."],
        _BAZAAR + ", bij de kraam met de kruidenbergjes onder de grote gouden koepel"),
    "z6_pistache": ("Koopman Pistache", [
        "Alles is te koop. Behalve mijn snor. Die is van mijn opa geweest.",
        "Wil je iets verkopen? Het veilinghuis van Saffraanpoort zit bij mij.",
        "Hoe meer je lacht, hoe lager de prijs. Dat is een geheim. Niet doorvertellen."],
        _BAZAAR + ", bij de kraam met de potten en de manden"),
    "z6_lokum": ("Juwelier Lokum", [
        "Edelstenen inzetten? Uithalen? Ik doe het met een pincet en een lokumpje voor de rust.",
        "Saffraangoud is het mooiste goud van de wereld. Het ruikt een beetje naar rijst.",
        "Ze zeggen dat er een hertogin is met tien ringen. Om haar tenen. Ik zou zo graag eens kijken."],
        _BAZAAR + ", bij het glazen kraampje aan de pijler met de lampionnen"),
    "z6_koperkop": ("Smid Koperkop", [
        "Tink, tonk, tink. Mijn hamer zingt in de bazaar. Mijn buren zingen terug. Iets minder blij.",
        "Saffraangoud is zacht van buiten en sterk van binnen. Net als een guh.",
        "Kom maar binnen als je iets wilt laten maken."],
        _BAZAAR + ", bij de smidse aan het westeinde"),
    "z6_bankier_poort": ("Bankier Dinar", [
        "De bank van Saffraanpoort. De kluis staat op palen, voor als de rivier eens te enthousiast wordt.",
        "Alles wat je bij een bankier stopt, kun je bij elke bankier terugpakken. Ook hier.",
        "Ik tel munten in mijn slaap. Gisteren kwam ik op elf. Toen werd ik wakker."],
        "aan de balie van de bank, bij de Saffraanbrug op de zuidoever van Saffraanpoort"),
    "z6_knars": ("Molenguh Knars", [
        "De norias scheppen het water uit de rivier omhoog. Ze kraken en zingen. Of ze zongen.",
        "Een noria zonder lied is een droevig rad. Ik hoor het in mijn tanden.",
        "Knars. Knars. Dat is geen naam, dat is een geluid. Maar het is ook mijn naam."],
        "aan de kade van de Saffraanrivier, bij de grote noria naast de Saffraanbrug"),
    "z6_pluk": ("Kindje Pluk", [
        "Ik heb tien badeendjes! Nou ja, had. Ze zijn weggedreven door de Kanaalwijk.",
        "Als je over het water vliegt, kun je ze vangen. Met een tapijt. Of met een hele lange arm.",
        "Mijn lievelingseendje heet Kwak. De andere heten ook Kwak. Dat is makkelijk."],
        "in de Kanaalwijk, bij het bruggetje met de bloembakken (zuidwesten van Saffraanpoort)"),
    "z6_ringeling": ("Gesluierde Dame", [
        "Ken ik jou? Nog niet, schat. Nog niet.",
        "Wat een mooie stad. Zo... gedeeld. Ik moet er nog aan wennen.",
        "Ik heb tien tenen. Dat is normaal. Tien blote tenen. Dat is nieuw."],
        _TUIN + ", bij de fontein met de vier kanaaltjes"),
    "kk_poort_saffraanpoort": ("Kwartiermeester Koepeltje", [
        "Een huis met een koepeltje? In het Knuffelkwartier kan dat. Bijna. Ik breng je erheen.",
        "Saffraanpoort is vol. Maar het Knuffelkwartier heeft nog kavels.",
        "Een tuin met een fontein? Daar heb je wel een grote gieter voor nodig."],
        "op het paleisplein van Saffraanpoort, bij de stenen bank onder de palmen"),
    "bond_bord_saffraanpoort": ("Bondenbord-guh Punaise", [
        "Bonden zoeken leden! Kijk maar op mijn bord.",
        "De Sterrendovende Toren doe je met twee of meer. Hier vind je die meer.",
        "Een Knuffelbond is een karavaan die nooit uit elkaar gaat."],
        _SERAI + ", bij het guild-karavaanbord onder de arcade"),
    # --- profession teachers (they open t-ambacht's workshops: QuestMotor.EXTRA) ---------------------------------------------------
    "z6_leermeester_smeden": ("Leermeester Smidguh Saffraan", [
        "Saffraangoud smeden is als honing kneden. Warm, langzaam en een beetje plakkerig.",
        "Met Sterrenijzer maak je de mooiste sieraden. Zeldzaam, maar je vindt het in het Ruïnedal.",
        "Kom maar mee naar de werkplaats."], _BAZAAR + ", in de smidse naast Smid Koperkop"),
    "z6_leermeester_naaien": ("Leermeester Naaiguh Zijdeworm", [
        "Sterrenzijde! Cocons van de moerbeibomen en een snufje sterrenstof. Zo zacht dat je erin wegzakt.",
        "Ik naai zadeldekens voor bultguhs. Ze zijn kieskeurig. Gestreept, graag.",
        "Kom maar mee naar de werkplaats."], _BAZAAR + ", in het naaiatelier naast Knoopje"),
    "z6_leermeester_koken": ("Leermeester Kookguh Saffie", [
        "Saffraanrijst met dadels. Rozenwaterlokum. En thee met kardemom. Ik leer je alles.",
        "Een Karavaanmaal maak je voor iedereen tegelijk. Dat is de enige goede manier.",
        "Kom maar mee naar de keuken."], _SERAI + ", in de keuken achter de arcaden"),
    "z6_leermeester_brouwen": ("Leermeester Brouwguh Rozewater", [
        "Rozenwater, sterrenstofdrank, en zoutkristal slijpen. Mijn brouwerij ruikt naar alles tegelijk.",
        "Een drankje met sterrenstof laat je 's nachts zachtjes gloeien. Handig in de woestijn.",
        "Kom maar mee naar de werkplaats."], "in de brouwerij onder de Sterrenwacht van Twinkel (noordoosten van Saffraanpoort)"),
    "z6_leermeester_imkeren": ("Leermeester Imker Oranjebloesem", [
        "Oranjebloesemhoning. Saffraanhoning. De bijen in de Paradijstuin zijn heel deftig.",
        "Een bij in de woestijn heeft dorst. Daarom wonen ze bij de fonteinen.",
        "Kom maar mee, dan leg ik je alles uit."], _TUIN + ", bij de bijenkasten tussen de oranjebomen"),
    # --- out in the land -------------------------------------------------------------------------------------------------------------
    "z6_dadelpit": ("Palmguh Dadelpit", [
        "Welkom in de Palmenkloven! Hier groeien de zoetste dadels van de wereld. Als de aapjes ze niet pikken.",
        "De watervallen zingen hier dag en nacht. Ik zing mee. Niemand hoort het.",
        "Boven op het plateau staat het Palmbaken. Het is al weken uit. Mijn palmen worden er verdrietig van."],
        "bij de onderste waterval in de Palmenkloven, de rivierkloof ten zuidwesten van Saffraanpoort"),
    "z6_spetter": ("Koerier Spetter", [
        "Ik breng post langs de watervallen. Snel en nat. Vooral nat.",
        "Met een tapijt ben je sneller. Met een tapijt én een paraplu ben je droog.",
        "Ik heb een record. Ik vertel niet welk. Je moet het zelf proberen."],
        "aan de monding van de Palmenkloven, waar de Saffraanrivier in zee stroomt"),
    "z6_korrel": ("Zoutzieder Korrel", [
        "Zout! Wit zout, roze zout, en 's nachts glinsterend zout. Dat laatste smaakt naar sterren.",
        "Mijn ziedershuisje is half ingestort. Zoutkreeftjes hebben er een feestje gevierd.",
        "Onder de Zoutvlakte zit een grot die zingt. Ik hoor haar 's nachts door de vloer."],
        "in de Zoutvlakte, bij het zoutziedershuisje aan de zuidrand (zuidoost van Saffraanpoort)"),
    "z6_rimpel": ("Duinguh Rimpel", [
        "Ik woon in de Roze Duinen. Elke dag ligt mijn huisje op een andere plek. Het zand schuift.",
        "Koningin Wormelien zwemt door de duinen. Vroeger zong ze. Nu bromt ze alleen nog.",
        "Het Duinbaken staat op de hoogste duin. Als hij weer brandt, kan ik weer slapen."],
        "op de kam van de Roze Duinen, bij de tent met de roze wimpel (ten zuiden van Saffraanpoort)"),
    "z6_stof": ("Professor Stof", [
        "Ik ben archeoloog. Ik graaf in oude dingen. En in koekjestrommels. Dat is ook een soort archeologie.",
        "De Sterrenguhs bouwden hier tempels voor de sterren. Ze telden ze allemaal. Eén voor één.",
        "In het Ruïnedal ligt de Verzonken Sterrenwacht. Half in het zand. Niet alleen naar binnen gaan."],
        "in het archeologenkamp in het Ruïnedal, tussen de mesa's ten noordoosten van Saffraanpoort"),
    "z6_bron": ("Moeder Bron", [
        "...zzz... water... de sterren... zzz...",
        "Ik ben de bron van alle zoete water. Ik was moe. Heel moe. Nu niet meer.",
        "Wie het sterrenpad volgt, vindt altijd water. Dat beloof ik."],
        "aan de oever van de Moederoase, onder de grote Moederpalm (het oosten van het Zijdeland)"),
    "z6_mientje": ("Mager Mientje", [
        "Ik ben het, Mientje! Uit de soepkeuken van Hoogvads. Ik ben meegekomen. Er komen er nog meer.",
        "Langs de Oostroute komen ze. Mager en grauw. Net als ik vroeger.",
        "Soep helpt. Soep helpt altijd."],
        "bij de vluchtelingententen aan de oostkant van de Moederoase, waar de Oostroute begint"),
}
VLUCHTELINGEN = {
    "z6_vluchteling_1": ("Uitgevadste Grafiet", [
        "Wij komen van ver. Van waar de wegen allemaal één kant op lopen.",
        "Hier is water. Zoet water. Ik wist niet dat water zoet kon zijn.",
        "Dank je. Echt. Dank je."]),
    "z6_vluchteling_2": ("Uitgevadste Gruis", [
        "De Mika's zeiden dat de oase leeg was. Hij is vol! Ze logen.",
        "Mijn kleintjes slapen eindelijk. Bij het water.",
        "Mag ik nog een kom? Voor later. Voor de reis."]),
    "z6_vluchteling_3": ("Uitgevadste Leisteen", [
        "Ik heb lopend de Oostroute afgelegd. Mijn voeten zijn nu officieel plat.",
        "Er staat een grauwe toren hier vlakbij. Daar kijk ik niet naar.",
        "Morgen ga ik naar Saffraanpoort. Ze zeggen dat er thee is."]),
}
for _id, (_naam, _r) in VLUCHTELINGEN.items():
    NPCS[_id] = (_naam, _r, "bij de vluchtelingententen aan de oostkant van de Moederoase")
BURGERS = {
    "z6_burger_1": ("Kloofguh Zandkorrel", _PLEIN, [
        "Ik woon op de tweede richel. Elke ochtend loop ik naar beneden. Elke avond loop ik naar boven. Mijn kuiten zijn beroemd.",
        "Als de zon in de kloof valt, wordt alles roze. Nog rozer dan het al was.",
        "Heb je de walvis gezien? Je ziet haar vanaf de Zeemond. Als een wolkje met een staart."]),
    "z6_burger_2": ("Ezelguh Balkonnetje", "op de eerste richel van de Rozenkloof, bij de rotswoningen met de balkonnetjes", [
        "Mijn balkon kijkt uit op het balkon van de buren. We zwaaien elke dag.",
        "Mijn bloembak heeft woestijnrozen. Ze hebben bijna geen water nodig. Net als ik. Bijna.",
        "Vroeger zagen we 's nachts de bakens branden. Nu zien we alleen de buren."]),
    "z6_burger_3": ("Bazaarguh Fluitje", _BAZAAR, [
        "In de bazaar kun je verdwalen. Ik woon hier al dertig jaar en ik ben nog steeds een beetje verdwaald.",
        "Onder de koepels vallen lichtbundels naar binnen. Ik dans er soms in.",
        "Ruik je dat? Kardemom. En kaneel. En een beetje bultguh."]),
    "z6_burger_4": ("Kadeguh Feloek", "aan de kade van de Saffraanrivier, bij de feloekjes", [
        "De rivier wordt elke week een beetje lager. Dat zie je aan de streepjes op de kade.",
        "Als de Moederoase opdroogt, droogt alles op. Dan moeten we verhuizen. Naar waar?",
        "Een feloek is een bootje met een driehoekig zeil. Ik heb er drie. Ze heten Een, Twee en Drie."]),
    "z6_burger_5": ("Tuinguh Granaatje", _TUIN, [
        "In de Paradijstuin groeit alles. Granaatappels, oranjes, rozen en heel veel thee.",
        "De vier kanalen staan voor de vier windstreken. Of voor vier soorten thee. Dat weten we niet meer.",
        "Er zit hier soms een gesluierde dame. Ze zegt niks. Ze kijkt naar haar tenen."]),
    "z6_burger_6": ("Karavaanguh Belletje", _SERAI, [
        "Ik ben karavaanguh. Ik kan slapen op een bultguh in volle galop.",
        "Zonder de bakens durven we niet meer te reizen. We zitten hier al weken.",
        "Oma Dadel is de dapperste van allemaal. Ze reist gewoon. Met een theepot."]),
}
for _id, (_naam, _waar, _r) in BURGERS.items():
    NPCS[_id] = (_naam, _r, _waar)
WACHTEN = {
    "z6_wacht_1": ("Kloofwacht Zandsteen", "bij het kampvuur op het Rozenplein van de Rozenkloof"),
    "z6_wacht_2": ("Kloofwacht Richel", "bij het kampvuur onder de Luchthaven van Rozenkloof"),
    "z6_wacht_3": ("Poortwacht Saffraan", "bij het kampvuur bij de Westpoort van Saffraanpoort"),
    "z6_wacht_4": ("Poortwacht Koepel", "bij het kampvuur op het paleisplein van Saffraanpoort"),
    "z6_wacht_5": ("Karavaanwacht Kompas", "bij het kampvuur in het karavaankamp op de Zoutvlakte"),
    "z6_wacht_6": ("Karavaanwacht Palmblad", "bij het kampvuur aan de oever van de Moederoase"),
}
WACHT_REGELS = [
    "Steek het kampvuur aan: hier word je wakker als je flauwvalt. Ook in de woestijn.",
    "Wij bewaken de weg. Met een lans, een lantaarn en een thermosfles thee.",
    "Overdag is het zand te heet voor bultguh-pootjes. Daarom reizen karavanen 's nachts."]
for _id, (_naam, _waar) in WACHTEN.items():
    NPCS[_id] = (_naam, WACHT_REGELS, _waar)

# ---------------------------------------------------------------------------------------------------------------------------
# Where: POIs / areas / campfires (vadsrijk.npc.plek.<kind>.<id>)
# ---------------------------------------------------------------------------------------------------------------------------
PLEK_POI = {
    "z6_staarthaven": "op de Staarthaven van Hoogvads, de pier helemaal achteraan op de staart van Moeder Wolkwal (oostkant)",
    "z6_luchthaven": _HAVEN,
    "z6_plein": _PLEIN,
    "z6_schatkamer": "voor de Schatkamer, de grote uitgehouwen gevel in de eindwand aan de oostkant van de Rozenkloof",
    "z6_klus_dadels": _PLEIN + ", bij de manden met dadels naast de karavaan",
    "z6_klus_water": _PLEIN + ", bij de stervormige fontein (waterzakken vullen)",
    "z6_klus_tapijten": _PLEIN + ", bij de tapijtwinkel met de opgerolde tapijten",
    "z6_siq": "boven aan de Siq, de smalle kloof die vanuit de Rozenkloof schuin omhoog naar het plateau loopt (noordoosten)",
    "z6_rustplek": "bij de rustplek van de karavaan aan de duinweg, op de westrand van de Roze Duinen",
    "z6_bobbeltje": "achter de hoge roze duin ten zuiden van de rustplek (Roze Duinen)",
    "z6_wormelien_zicht": "op de duinkam ten oosten van de rustplek, met uitzicht over de Roze Duinen",
    "z6_westpoort": "bij de Westpoort van Saffraanpoort, de iwan tussen twee torens aan het eind van de karavaanweg",
    "z6_kijker": _WACHT + ", bovenaan bij de grote koperen kijker",
    "z6_palmbaken": "bij het Palmbaken, boven op het plateau aan de kop van de Palmenkloven (de watervallen op)",
    "z6_saffraanbrug": "op het paviljoen met het turkoois koepeltje midden op de Saffraanbrug in Saffraanpoort",
    "z6_zoutkamp": "in het karavaankamp bij het kampvuur op de Zoutvlakte (zuidoost van Saffraanpoort)",
    "z6_zoutbaken": "bij het Zoutbaken, midden op de witte Zoutvlakte",
    "z6_mikakamp": "in het verlaten Mika-kamp aan de oostrand van de Zoutvlakte (de grauwe tentjes)",
    "z6_sterrenwacht_ingang": "bij de ingang van De Verzonken Sterrenwacht, de half in het zand gezakte koepel in het Ruïnedal",
    "z6_ruinebaken": "bij het Ruïnebaken, op de hoogste mesa van het Ruïnedal",
    "z6_spiegelzaal": "midden in de Spiegelzaal van het Rozijnenpaleis, op de loper onder de gouden koepel",
    "z6_dak_1": "op het dak van de Grote Bazaar, bij de grote gouden koepel van de kruising",
    "z6_dak_2": "op de daken van de woonwijk achter de bazaar, bij de windtoren met de roze koepel",
    "z6_dak_3": "op de Oostpoort van Saffraanpoort, de toren met de guh-oortjes aan het eind van de daken",
    "z6_duinbaken": "bij het Duinbaken, op de hoogste duin van de Roze Duinen (ten zuiden van Saffraanpoort)",
    "z6_luchtspiegeling": "op de Oostroute tussen de Zoutvlakte en de Moederoase, waar de luchtspiegeling begint",
    "z6_oasebaken": "bij het Oasebaken, op het rotsje aan de westoever van de Moederoase",
    "z6_moederoase": "aan de oever van de Moederoase, onder de grote Moederpalm (het oosten van het Zijdeland)",
    "z6_torenvoet": "aan de voet van De Sterrendovende Toren, de grauwe toren op het rotsje naast de Moederoase",
    "z6_zeemond": "op het uitkijkpunt aan het eind van de pier bij de Zeemond, de westelijke opening van de Rozenkloof",
    # side quests
    "z6_feest": _PLEIN + ", bij de feesttafel naast de fontein",
    "z6_tuin_1": "bij de hangende tuin op de eerste richel van de Rozenkloof (westwand)",
    "z6_tuin_2": "bij de hangende tuin op de tweede richel van de Rozenkloof (oostwand)",
    "z6_tuin_3": "bij de hangende tuin op de rand van het plateau, boven de Rozenkloof (westwand, de touwbrug over)",
    "z6_noria_1": "bij de westelijke noria op de zuidoever van de Saffraanrivier",
    "z6_noria_2": "bij de grote noria naast de Saffraanbrug (noordoever)",
    "z6_noria_3": "bij de oostelijke noria op de noordoever, voorbij de Paradijstuin",
    "z6_examen_1": "op het dak van de Sterrenwacht van Twinkel",
    "z6_examen_2": "op de bovenste galerij van de Saffraanbrug",
    "z6_examen_3": "op de westelijke minaret met guh-oortjes van het Rozijnenpaleis",
    "z6_examen_4": "op de Zeemond-pier in de Rozenkloof",
    "z6_waterval_1": "bij de onderste waterval van de Palmenkloven, waar Koerier Spetter begint",
    "z6_waterval_2": "bij de middelste waterval van de Palmenkloven, achter het watergordijn",
    "z6_waterval_3": "bij de bovenste waterval van de Palmenkloven, onder het Palmbaken",
    "z6_ziedershuisje": "bij het zoutziedershuisje aan de zuidrand van de Zoutvlakte",
    "z6_zoutgrot": "bij de ingang van de Zingende Zoutgrot, de spleet in het zout aan de zuidrand van de Zoutvlakte",
    "z6_grot_kristallen": "bij de zingende kristallen diep in de Zingende Zoutgrot, aan het lichtgevende meertje",
    "z6_grot_schat": "achter de kristalwand in de Zingende Zoutgrot, bij de kist van de Eerste Karavaan",
    "z6_wormelien": "in het hart van de Roze Duinen, waar Koningin Wormelien door het zand zwemt",
    "z6_kanaalwijk": "in de Kanaalwijk van Saffraanpoort (zuidwesten), de kanaaltjes met de bruggetjes",
    "z6_dadelpalmen": "in het palmenbos onder de watervallen van de Palmenkloven",
    "z6_ruinedal": "tussen de mesa's van het Ruïnedal, ten noordoosten van Saffraanpoort",
    "z6_zoutvlakte": "op de witte Zoutvlakte, zuidoost van Saffraanpoort",
    "z6_moerbeibomen": "bij de moerbeibomen in de Palmenkloven, langs de rivier onder de watervallen",
    # lookouts
    "z6_uitkijk_zeemond": "op het uitkijkpunt aan het eind van de Zeemond-pier (Rozenkloof, west)",
    "z6_uitkijk_schatkamer": "boven op het koepeltje van de Schatkamer (de wandtrap op tot het plateau, dan de rotsrug over)",
    "z6_uitkijk_minaret": "boven in de hoogste minaret met guh-oortjes van het Rozijnenpaleis (de wenteltrap op)",
    "z6_uitkijk_mesa": "boven op de Tafelmesa in het Ruïnedal, met uitzicht tot de grauwe toren",
}
PLEK_GEBIED = {
    "z6_zijdeland": "in het Zijdeland, het roze land aan de westkust van het Grote Vasteland",
    "z6_rozenkloof": _KLOOF,
    "z6_saffraanpoort": _STAD,
    "z6_roze_duinen": "in de Roze Duinen, het duinland ten zuiden van Saffraanpoort",
    "z6_palmenkloven": "in de Palmenkloven, de rivierkloof met watervallen ten zuidwesten van Saffraanpoort",
    "z6_zoutvlakte": "op de Zoutvlakte, het droge zoutmeer ten zuidoosten van Saffraanpoort",
    "z6_ruinedal": "in het Ruïnedal, tussen de mesa's ten noordoosten van Saffraanpoort",
    "z6_moederoase": "bij de Moederoase, helemaal in het oosten van het Zijdeland",
}
GEBIED_NAMEN = {
    "z6_zijdeland": "Het Zijdeland", "z6_rozenkloof": "Rozenkloof", "z6_saffraanpoort": "Saffraanpoort", "z6_roze_duinen": "De Roze Duinen",
    "z6_palmenkloven": "De Palmenkloven", "z6_zoutvlakte": "De Zoutvlakte", "z6_ruinedal": "Het Ruïnedal", "z6_moederoase": "De Moederoase",
}
PLEK_KAMPVUUR = {
    "z6_rozenkloof": "op het Rozenplein, naast de fontein, bij Kloofwacht Zandsteen",
    "z6_saffraanpoort": "op het paleisplein van Saffraanpoort, bij Poortwacht Koepel",
    "z6_roze_duinen": "bij de rustplek van de karavaan aan de duinweg",
    "z6_palmenkloven": "bij de onderste waterval van de Palmenkloven",
    "z6_zoutvlakte": "in het karavaankamp op de Zoutvlakte",
    "z6_ruinedal": "in het archeologenkamp van Professor Stof",
    "z6_moederoase": "aan de oever van de Moederoase",
}
UITKIJKEN = {
    "z6_uitkijk_zeemond": "De Zeemond",
    "z6_uitkijk_schatkamer": "Het Schatkamerkoepeltje",
    "z6_uitkijk_minaret": "De Oortjesminaret",
    "z6_uitkijk_mesa": "De Tafelmesa",
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quest items (Z6VerhaalRegistry.QUEST_ITEMS): name, vanilla texture
# ---------------------------------------------------------------------------------------------------------------------------
QUEST_ITEMS = {
    "sterrenlens": ("De Sterrenlens (rechtsklik: hoeveel bakens branden)", "minecraft:item/spyglass"),
    "sterrenscherf": ("Sterrenscherfje", "minecraft:item/amethyst_shard"),
    "sterrenkompas": ("Het Sterrenkompas van de Sterrenguhs", "minecraft:item/compass_00"),
    "grauwe_brief": ("Grauwe brief (aan de Ringvinger)", "minecraft:item/paper"),
    "cocon": ("Zijdecocon", "minecraft:item/string"),
    "dadeltaart": ("Dadeltaart voor Bobbeltje", "minecraft:item/pumpkin_pie"),
    "ingredient": ("Taartspulletje", "minecraft:item/sugar"),
    "brief": ("Brief voor een rotswoning", "minecraft:item/paper"),
    "kwastje": ("Tapijtkwastje", "minecraft:item/string"),
    "badeendje": ("Badeendje Kwak", "minecraft:item/egg"),
    "dadels": ("Mandje gepikte dadels", "minecraft:item/sweet_berries"),
    "zoutkreeftje": ("Zoutkreeftje", "minecraft:item/rabbit_foot"),
    "tablet": ("Stenen tablet van de Sterrenguhs", "minecraft:item/brick"),
    "ring": ("De ring van de Keizer", "minecraft:item/gold_nugget"),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Puzzles (puzzels.json + texts). Every step: a question, options, the right one. Star puzzles: you follow the stars of a
# constellation in the order the hint describes; a wrong star lets the line snap: start again. Built deterministically.
# ---------------------------------------------------------------------------------------------------------------------------
STERRENBEELDEN = {   # id: (name, hint (the order), stars in order, decoys)
    "oefen": ("Het Knuffeltje", "Twinkel: \"Een guh-hoofdje teken je altijd zo: eerst het linkeroortje, dan het rechteroortje, en dan het snuitje.\"",
              ["het linkeroortje", "het rechteroortje", "het snuitje"], ["een vallende ster", "de maan"]),
    "palm": ("De Dadelpalm", "Moeder Bron fluistert: \"Een palm groeit van onder naar boven. Begin bij de voet, klim langs de stam naar de kruin, "
             "en laat dan de bladeren hangen: eerst links, dan rechts.\"",
             ["de voet van de stam", "het midden van de stam", "de kruin", "het linkerblad", "het rechterblad"], ["een dadel die valt", "de waterval"]),
    "zout": ("Het Guhhoofdje", "Oma Dadel neuriet: \"Oortjes eerst, links en rechts. Dan de rechterwang, het snuitje in het midden, "
             "en de linkerwang om de cirkel rond te maken.\"",
             ["het linkeroortje", "het rechteroortje", "de rechterwang", "het snuitje", "de linkerwang"], ["een zoutkristal", "het kampvuur"]),
    "ruine": ("De Sterrenguh", "Op de muur van de tempel staat het, half weggesleten: \"De Sterrenguh begint bij zijn kroontje, dan zijn hart, "
              "dan zijn linkerpoot en zijn rechterpoot.\"",
              ["het kroontje", "het hart", "de linkerpoot", "de rechterpoot"], ["een meteorietbrokje", "de telescoop"]),
    "duin": ("De Bultguh", "Moeder Bron, nu luider: \"Een bultguh volg je van achter naar voren. Het staartje, de achterste bult, de voorste "
             "bult, en dan zijn lieve kop.\"",
             ["het staartje", "de achterste bult", "de voorste bult", "de kop"], ["een zandkorrel", "Koningin Wormelien"]),
    "oase": ("De Moederpalm", "Moeder Bron, helder als water: \"Ik ben de bron. Begin bij het water, volg de wortels omhoog naar de stam, "
             "open de kruin, en eindig bij de ster bovenin. Daar wacht ik.\"",
             ["het water", "de wortels", "de stam", "de kruin", "de ster bovenin"], ["de grauwe toren", "een ring"]),
    "examen_1": ("De Kleine Pluis", "Twinkel: \"De Kleine Pluis is rond. Begin bovenaan en ga met de klok mee: boven, rechts, onder, links.\"",
                 ["de bovenste ster", "de rechterster", "de onderste ster", "de linkerster"], ["het midden"]),
    "examen_2": ("De Grote Knuffel", "Twinkel: \"De Grote Knuffel spreidt zijn armen. Linkerhand, linkerschouder, rechterschouder, rechterhand.\"",
                 ["de linkerhand", "de linkerschouder", "de rechterschouder", "de rechterhand"], ["het hoofd"]),
    "examen_3": ("De Vliegende Bultguh", "Twinkel: \"De Vliegende Bultguh vliegt naar het oosten. Begin bij zijn staart in het westen, dan de "
                 "vleugel, en eindig bij zijn snuit in het oosten.\"",
                 ["de staart in het westen", "de vleugel", "de snuit in het oosten"], ["de bult", "de maan"]),
    "examen_4": ("Het Kroontje", "Professor Stof heeft het opgeschreven: \"Een kat met een kroontje telde de sterren. Begin bij de "
                 "linkerpunt, dan de middelste, dan de rechter. Drie punten. Nooit genoeg, schreef hij erbij.\"",
                 ["de linkerpunt", "de middelste punt", "de rechterpunt"], ["de kat", "een vierde punt"]),
}
VRAAG_STER = "Welke ster komt nu?"
ANDERE_PUZZELS = {   # id: (title, intro, [(question, [options], right index)], wrong, done)
    "lied": ("Het karavaanlied", "De kristallen zingen elk één toon. Oma Dadel neuriede het karavaanlied: \"La-la-LAA-la-lo.\" Laag, laag, "
             "heel hoog, laag, en dan het allerlaagst.",
             [("Welk kristal tik je eerst aan? (La)", ["het lage roze kristal", "het hoge witte kristal", "het allerlaagste blauwe kristal"], 0),
              ("En dan? (la)", ["het hoge witte kristal", "het lage roze kristal", "het allerlaagste blauwe kristal"], 1),
              ("En dan? (LAA!)", ["het lage roze kristal", "het allerlaagste blauwe kristal", "het hoge witte kristal"], 2),
              ("En dan? (la)", ["het lage roze kristal", "het hoge witte kristal", "het allerlaagste blauwe kristal"], 0),
              ("En als laatste? (lo)", ["het hoge witte kristal", "het allerlaagste blauwe kristal", "het lage roze kristal"], 1)],
             "Pliiing! Een valse toon. De kristallen giechelen. Begin het lied opnieuw.",
             "De kristallen zingen het karavaanlied mee, steeds harder. Dan schuift de kristalwand opzij."),
    "afdingen_kardemom": ("Afdingen bij Koopvrouw Kardemom", "Koopvrouw Kardemom: \"Dit zakje saffraan kost vijftig munten. Vijftig! Een koopje.\"",
                          [("Wat zeg je?", ["\"Vijftig? Dat is diefstal!\"", "\"Wat ruikt dat heerlijk. Mag ik even ruiken?\"", "\"Ik geef je er twee.\""], 1),
                           ("Kardemom bloost. \"Ruik maar. Hij komt van de beste krokussen.\" Wat nu?",
                            ["\"Voor veertig neem ik er twee, eentje voor jou.\"", "\"Dan koop ik hem ergens anders.\"", "\"Mag ik hem gratis?\""], 0),
                           ("\"Eentje voor MIJ? Niemand geeft mij ooit iets...\" Wat zeg je?",
                            ["\"Grapje. Gewoon veertig.\"", "\"Echt waar. Voor bij je thee.\"", "\"Zeg maar dertig dan.\""], 1)],
                          "Kardemom trekt een pruillip. \"Zo dingen we hier niet af, schat. Probeer het nog eens. Liever.\"",
                          "Kardemom lacht zo hard dat haar kruidenbergjes stuiven. \"Voor jou: altijd korting. Zeg maar dat Kardemom het zei.\""),
    "afdingen_pistache": ("Afdingen bij Koopman Pistache", "Koopman Pistache: \"Deze pot is van mijn opa geweest. Hij is onbetaalbaar. Honderd munten.\"",
                          [("Wat zeg je?", ["\"Wat een prachtige snor heeft u.\"", "\"Die pot heeft een barst.\"", "\"Honderd? Pfff.\""], 0),
                           ("Pistache aait zijn snor. \"Ook van mijn opa.\" Wat nu?",
                            ["\"Dan moet die pot ook van een heel lieve opa zijn.\"", "\"Mag ik de snor ook kopen?\"", "\"Tachtig, laatste bod.\""], 0),
                           ("Pistache snift. \"Hij was heel lief. Hij gaf altijd korting.\" Wat zeg je?",
                            ["\"Dan doen we het zoals opa.\"", "\"Dus zestig?\"", "\"Ik moet gaan.\""], 0)],
                          "Pistache draait zijn snor de verkeerde kant op. \"Hmpf. Opa zou dat niet zo gezegd hebben.\" Probeer het nog eens.",
                          "Pistache geeft je een knipoog en een knuffel. \"Zoals opa. Voor jou altijd korting.\""),
    "afdingen_lokum": ("Afdingen bij Juwelier Lokum", "Juwelier Lokum: \"Dit ringetje van saffraangoud kost tweehonderd munten. Het past om een teen.\"",
                       [("Wat zeg je?", ["\"Om een TEEN?\"", "\"Wat een lieve lokumpjes heeft u daar.\"", "\"Honderd.\""], 1),
                        ("Lokum biedt je er een aan. Wat nu?",
                         ["\"Ik neem er één, als u er ook één neemt.\"", "\"Ik neem ze allemaal.\"", "\"Nee, dank u.\""], 0),
                        ("Jullie kauwen samen. \"Mmm. Waar hadden we het over?\" Wat zeg je?",
                         ["\"Over een ringetje voor een heel lieve prijs.\"", "\"Over tweehonderd munten.\"", "\"Over tenen.\""], 0)],
                       "Lokum schuift het lokum weer weg. \"Een juwelier dingt niet af met een kale kraai.\" Probeer het nog eens.",
                       "Lokum lacht met poedersuiker op zijn snuit. \"Voor jou: de lieve prijs. Altijd.\""),
}
PUZZEL_TEKST = {
    "ster.fout": "De lijn tussen de sterren knipt los. De sterren knipperen geduldig. Begin opnieuw bij de eerste ster.",
    "ster.goed": "Je trekt een gloeiende lijn naar %s.",
    "ster.klaar": "Het sterrenbeeld %s staat stralend aan de hemel!",
    "ster.lens": "Het is nog licht. Je houdt de Sterrenlens voor je oog: de lucht wordt diepblauw en vol sterren.",
    "optie.stop": "Ik probeer het straks",
}


def _sterpuzzel(id, naam, hint, sterren, lokkers):
    r = random.Random("z6:" + id)
    stappen = []
    for i, goed in enumerate(sterren):
        pool = [s for s in sterren if s != goed and s not in sterren[:i]] + lokkers
        r.shuffle(pool)
        opties = [goed] + pool[:2]
        r.shuffle(opties)
        stappen.append((VRAAG_STER if i else f"Waar begint {naam}?", opties, opties.index(goed)))
    return (naam, hint, stappen, PUZZEL_TEKST["ster.fout"], PUZZEL_TEKST["ster.klaar"] % naam)


PUZZELS = {id: _sterpuzzel(id, *v) for id, v in STERRENBEELDEN.items()}
PUZZELS.update(ANDERE_PUZZELS)

# The five star beacons: id -> (puzzle, name, poi)
BAKENS = {
    "palm": ("palm", "Het Palmbaken", "z6_palmbaken"),
    "zout": ("zout", "Het Zoutbaken", "z6_zoutbaken"),
    "ruine": ("ruine", "Het Ruïnebaken", "z6_ruinebaken"),
    "duin": ("duin", "Het Duinbaken", "z6_duinbaken"),
    "oase": ("oase", "Het Oasebaken", "z6_oasebaken"),
}
# Moeder Bron's visions after the n-th beacon (stronger each time)
VISIOENEN = {
    0: ["In het water van de fontein trilt iets. Een stem, heel zacht, als een druppel: \"...hier... ik ben hier...\"",
        "\"...volg de sterren... naar mij toe...\"",
        "Het water is weer stil. Twinkel kijkt je met grote ogen aan."],
    1: ["Het baken vlamt op! Een straal sterrenlicht schiet de lucht in, en de Sterrenlens vangt een sterrenscherfje.",
        "In de straal zie je heel even een meer, verborgen achter trillende lucht. Een palm. Een grauwe vlek ernaast.",
        "Een stem, nog zwak: \"Dank je... één ster... ik voel hem... ik ben Bron. Moeder Bron. Kom...\""],
    2: ["Het tweede baken brandt. Twee stralen kruisen elkaar boven het zand.",
        "Moeder Bron, iets duidelijker: \"Ik lig te slapen onder mijn palm. Iemand drinkt mij leeg. Met een rietje van grauw.\"",
        "\"Ze heeft ringen. Tien. Ze glimmen van mijn sterren.\""],
    3: ["Drie stralen! Het sterrenpad begint te tekenen: een gloeiende lijn naar het oosten.",
        "Moeder Bron, nu als een beekje: \"Je bent dichtbij. De luchtspiegeling die mij verbergt, wordt dunner.\"",
        "\"Pas op voor wie in spiegels woont. Spiegels laten niet zien wie je bent. Alleen hoe je eruitziet.\""],
    4: ["Het vierde baken. Vier stralen, en de nacht zingt zachtjes mee.",
        "Moeder Bron, helder als een fontein: \"Nog één ster. Mijn eigen ster. Breng de karavaan. Breng iedereen.\"",
        "\"Ik ben zo moe, ridder. Maar ik word wakker. Ik voel het.\""],
    5: ["Alle vijf de bakens branden! Vijf stralen ontmoeten elkaar recht boven het meer.",
        "De luchtspiegeling scheurt open als een gordijn. Daar ligt de Moederoase, zo blauw dat het pijn doet aan je ogen.",
        "En aan de oever, onder de palm, ligt een grote, grauwe guh van water te slapen."],
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quests. D(type, doel, n, text, bron, kans, plek): text = WHAT + WHO + WHERE; plek = where the tracker points.
# ---------------------------------------------------------------------------------------------------------------------------
QUESTS = []


def D(soort, doel, n, tekst, bron="", kans=0.0, plek=""):
    return dict(soort=soort, doel=doel, n=n, tekst=tekst, bron=bron, kans=kans, plek=plek)


def Q(id, soort, gever, titel, tekst, doelen, aanbod, bezig, klaar, munten=0, items=(), wapen=False, loot="", lv=60, na=(),
      inleveren=None, klasse=None, auto=False, praat=None, actie_aanname="", actie_klaar="", start_items=()):
    QUESTS.append(dict(id=id, soort=soort, gever=gever, titel=titel, tekst=tekst, doelen=doelen, aanbod=aanbod, bezig=bezig, klaar=klaar,
                       beloning=dict(xp=0, munten=munten, items=[list(i) for i in items], wapen=wapen, loot=loot), lv=lv, na=list(na),
                       inleveren=inleveren or gever, klasse=klasse, auto=auto, praat=praat or {}, actie_aanname=actie_aanname,
                       actie_klaar=actie_klaar, start_items=[list(i) for i in start_items]))


def item(id):
    return "guhs:vr_z6_npc_" + id


HQ = ["z6_hq01_land", "z6_hq02_karavaan", "z6_hq03_duinen", "z6_hq04_sultane", "z6_hq05_palmbaken", "z6_hq06_tapijt",
      "z6_hq07_zoutvlakte", "z6_hq08_sterrenwacht", "z6_hq09_spiegelzaal", "z6_hq10_moederoase"]
VORIGE = "z5_hq10_stormrand"   # (zone 5's last main quest; Z6Haken also starts hq01 on the flag z6_lijn_e_open)


def baken(b):
    puz, naam, poi = BAKENS[b]
    return D("meld", "z6_baken:" + b, 1, f"Steek {naam} weer aan: volg de sterren van het sterrenbeeld (bij het baken, met je Sterrenlens)",
             plek="poi:" + poi)


# ===== the main story of zone 6: "De Sterrenkaravaan" (10) ===================================================================
Q(HQ[0], "hoofd", "z5_havenmeester", "Land in zicht!",
  "Moeder Wolkwal zwemt met heel Hoogvads naar het oosten, tot de roze kliffen van het Grote Vasteland opdoemen. Daar ligt het "
  "Zijdeland. Lijn E vliegt van de Staarthaven van Hoogvads naar de Luchthaven van Rozenkloof.",
  [D("meld", "z6:wolkwaltocht", 1, "Kijk hoe Moeder Wolkwal naar het oosten zwemt (de film start vanzelf; anders bij de Staarthaven, "
     "helemaal achteraan op de staart van Hoogvads)", plek="poi:z6_staarthaven"),
   D("meld", "z6:aangekomen", 1, "Neem lijn E vanaf de Staarthaven van Hoogvads naar de Luchthaven van Rozenkloof", plek="poi:z6_staarthaven"),
   D("praat", "z6_zandloper", 1, "Meld je bij Havenmeester Zandloper aan de balie van de Luchthaven van Rozenkloof (noordrand van de kloof)",
     plek="npc:z6_zandloper"),
   D("praat", "z6_roosje", 1, "Daal de wandtrappen af naar de bodem van de kloof en loop naar de Schatkamer in de oostelijke eindwand: "
     "Kloofmoeder Roosje", plek="npc:z6_roosje")],
  [("npc", "Ridder! Kijk naar het oosten! De Wolkenzee is opengetrokken. Daar, die roze streep. Dat is land!"),
   ("npc", "Moeder Wolkwal zwemt er al naartoe. Voor jou. Ze heeft het gevoeld, zegt de Koningin. Er gaan daar sterren uit."),
   ("npc", "Zodra ze stilligt, vliegt lijn E vanaf de Staarthaven. Achteraan, op haar staart. Naar Rozenkloof.")],
  "Naar de Staarthaven, met lijn E naar Rozenkloof, en dan naar Kloofmoeder Roosje in de Schatkamer.",
  [("npc", "Een ridder! Uit de lucht! Kom binnen, kind. Pas op de drempel, die is drieduizend jaar oud en een beetje chagrijnig."),
   ("npc", "Ik ben Roosje, de kloofmoeder. Ik zag de walvis vanochtend. Een wolkje met een staart, aan de horizon. Wat een gezicht."),
   ("jij", "Waarom zijn de nachten hier zo donker?"),
   ("npc", "Ah. Je hebt het gezien. De sterrenbakens zijn uit. Alle vijf. Eén voor één gedoofd, de afgelopen weken."),
   ("npc", "Karavanen varen op die bakens naar de Moederoase, de bron van al ons water. Zonder bakens verdwalen ze. En de rivier zakt."),
   ("npc", "Oma Dadel wil toch vertrekken. Zij is de dapperste karavaanguh die ik ken. Ze staat op het Rozenplein. Help haar.")],
  munten=26000, lv=60, na=[VORIGE], inleveren="z6_roosje", auto=True)

Q(HQ[1], "hoofd", "z6_dadel", "De karavaan van Oma Dadel",
  "Oma Dadel vertrekt met haar kleine karavaan naar Saffraanpoort, ook al zijn de sterrenbakens uit. Ze kan wel een paar handen "
  "gebruiken. En jij een bultguh.",
  [D("gebied", "poi:z6_klus_dadels", 1, "Pak de manden met dadels in, naast de karavaan op het Rozenplein", plek="poi:z6_klus_dadels"),
   D("gebied", "poi:z6_klus_water", 1, "Vul de waterzakken bij de stervormige fontein op het Rozenplein", plek="poi:z6_klus_water"),
   D("gebied", "poi:z6_klus_tapijten", 1, "Haal de opgerolde tapijten bij de tapijtwinkel van Tapijtkoopman Franje (Rozenplein)",
     plek="poi:z6_klus_tapijten"),
   D("meld", "z6:bultguh", 1, "Kies je eigen bultguh bij Stalmeester Hobbel in de bultguh-stal op het Rozenplein", plek="npc:z6_hobbel")],
  [("npc", "Daar ben je. Roosje zei al dat er een ridder kwam. Je ziet eruit alsof je kunt tillen. Mooi."),
   ("npc", "Mijn karavaan gaat naar Saffraanpoort. Zonder bakens, ja. Ik heb een theepot en een goed gevoel. Dat is genoeg."),
   ("npc", "Help me inpakken: dadels, water, tapijten. En haal bij Hobbel een bultguh. Je gaat toch niet lopen, hè?")],
  "Dadels, waterzakken, tapijten, en een bultguh bij Stalmeester Hobbel.",
  [("npc", "Alles ingepakt! En kijk jou eens op die bultguh. Jullie passen bij elkaar. Allebei een beetje bultig."),
   ("npc", "Dit is Bobbel, mijn bultguh. En dat kleine bolletje dat achter haar aan huppelt is Bobbeltje. Haar kindje."),
   ("npc", "Morgen vertrekken we. Door de Siq omhoog, en dan langs de duinen. La-la-LAA-la-lo.")],
  munten=26000, lv=60, na=[HQ[0]], inleveren="z6_dadel",
  praat={"z6_hobbel": [
      ("npc", "Een ridder voor een bultguh! Kijk ze staan. Zandroze, saffraan, wolkwit, kaneel. Ze zijn allemaal even lief."),
      ("npc", "Een bultguh loopt over zand als over een kussen. En hij heeft twee zitjes. Neem eens iemand mee."),
      ("npc", "Deze kiest jou, zie ik. Hij likt aan je oor. Dat betekent: ja.")]})

Q(HQ[2], "hoofd", "z6_dadel", "Door de Roze Duinen",
  "De karavaan van Oma Dadel trekt door de Siq omhoog en langs de Roze Duinen naar Saffraanpoort. Zonder bakens. Blijf dichtbij.",
  [D("gebied", "poi:z6_siq", 1, "Rij met de karavaan door de Siq omhoog naar het plateau (de smalle kloof in het noordoosten van de "
     "Rozenkloof)", plek="poi:z6_siq"),
   D("gebied", "poi:z6_rustplek", 1, "Volg de duinweg naar de rustplek van de karavaan aan de westrand van de Roze Duinen", plek="poi:z6_rustplek"),
   D("meld", "z6:bobbeltje", 1, "Bobbeltje is zoek! Zoek het babybultguhtje achter de hoge duin ten zuiden van de rustplek", plek="poi:z6_bobbeltje"),
   D("meld", "z6:wormelien", 1, "Kijk vanaf de duinkam ten oosten van de rustplek wat daar door het zand zwemt", plek="poi:z6_wormelien_zicht"),
   D("gebied", "poi:z6_westpoort", 1, "Breng de karavaan naar de Westpoort van Saffraanpoort, aan het eind van de karavaanweg",
     plek="poi:z6_westpoort"),
   D("praat", "z6_dadel_serai", 1, "Zoek Oma Dadel op in de Karavanserai bij de Westpoort van Saffraanpoort", plek="npc:z6_dadel_serai")],
  [("npc", "Klaar? Dan gaan we. Overdag is het zand te heet, dus we reizen in de schemer. Zonder sterren. Spannend."),
   ("npc", "Door de Siq omhoog, en dan langs de duinen. Blijf in de buurt. En let op Bobbeltje, die rent overal achteraan.")],
  "Door de Siq, langs de duinen, Bobbeltje zoeken, en naar de Westpoort van Saffraanpoort.",
  [("npc", "We zijn er! Saffraanpoort! Ruik je dat? Kardemom, rozen en rivierwater."),
   ("npc", "Dat ding in de duinen... Koningin Wormelien. Ze zong vroeger, voor de karavanen. Nu is ze grauw en hongerig."),
   ("npc", "Alles gaat mis sinds de bakens uit zijn. Ga naar Sultane Rozijntje in het paleis. Zij moet weten dat er een ridder is.")],
  munten=27000, lv=61, na=[HQ[1]], inleveren="z6_dadel_serai")

Q(HQ[3], "hoofd", "z6_dadel_serai", "Audiëntie bij Sultane Rozijntje",
  "Sultane Rozijntje bewaart de oude sterrenkaart van het Zijdeland. Grootvizier Kaneelstok regelt de audiëntie. En Sterrenkijker "
  "Twinkel kan je leren hoe je sterren leest.",
  [D("praat", "z6_kaneelstok", 1, "Vraag Grootvizier Kaneelstok om een audiëntie, bij de iwan voor de Spiegelzaal in het Rozijnenpaleis "
     "(noordoever)", plek="npc:z6_kaneelstok"),
   D("praat", "z6_rozijntje", 1, "Ga naar Sultane Rozijntje op de troon achterin de Spiegelzaal", plek="npc:z6_rozijntje"),
   D("meld", "z6_puzzel:oefen", 1, "Leer sterren lezen: verbind het sterrenbeeld Het Knuffeltje bij de grote koperen kijker, bovenin de "
     "Sterrenwacht van Twinkel (noordoosten van Saffraanpoort)", plek="poi:z6_kijker"),
   D("praat", "z6_twinkel", 1, "Praat met Sterrenkijker Twinkel naast de grote kijker in de Sterrenwacht", plek="npc:z6_twinkel")],
  [("npc", "De Sultane moet het weten, ridder. Bij de Westpoort over de brug, dan het paleis met de gouden koepel. Je kunt hem niet missen."),
   ("npc", "Vraag naar Grootvizier Kaneelstok. Hij regelt alles. Ook dingen die niet geregeld hoeven te worden.")],
  "Grootvizier Kaneelstok, de Sultane in de Spiegelzaal, en de kijker van Twinkel.",
  [("npc", "Je hebt het! Het Knuffeltje, van oor tot snuitje. Zo lees je sterren."),
   ("npc", "Het visioen in het water... Moeder Bron. De oude geest van de Moederoase. Ik dacht dat ze een sprookje was."),
   ("npc", "Neem mijn Sterrenlens mee. Hij is leeg, maar bij elk baken dat je aansteekt, vangt hij een scherfje sterrenlicht. "
           "Dan zie je ook overdag de nachthemel."),
   ("npc", "Begin bij het Palmbaken, boven de watervallen van de Palmenkloven. Palmguh Dadelpit woont daar. Hij weet de weg.")],
  munten=28000, lv=62, na=[HQ[2]], inleveren="z6_twinkel", items=[(item("sterrenlens"), 1)],
  praat={"z6_kaneelstok": [
      ("npc", "Audiëntie? Natuurlijk. Na de thee. Nou ja, een ridder mag voor. Ik zet de thee wel even weg."),
      ("npc", "Loop maar door de iwan, de Spiegelzaal in. Kijk niet te lang in de spiegels. Meester Glans poetst ze. Erg... grondig."),
      ("npc", "Vreemd, trouwens. Gisteren zag ik mezelf door de gang lopen. Terwijl ik hier stond. Ik moet minder thee drinken.")],
       "z6_rozijntje": [
      ("npc", "Een ridder uit de lucht. Op de rug van een walvis. Wat een tijd om sultane te zijn."),
      ("npc", "Kijk. De sterrenkaart van het Zijdeland. Vijf bakens: Palm, Zout, Ruïne, Duin en Oase. Samen wijzen ze naar de Moederoase."),
      ("npc", "Ze zijn allemaal gedoofd. De rivier zakt elke week. Als de Moederoase opdroogt, droogt Saffraanpoort op."),
      ("npc", "Ga naar Sterrenkijker Twinkel in de Sterrenwacht. Wie de bakens wil aansteken, moet eerst sterren leren lezen.")]})

Q(HQ[4], "hoofd", "z6_twinkel", "Het Palmbaken",
  "Het eerste baken staat boven de watervallen van de Palmenkloven. 's Nachts moet je het sterrenbeeld De Dadelpalm volgen, "
  "anders vlamt het niet op.",
  [D("praat", "z6_dadelpit", 1, "Praat met Palmguh Dadelpit bij de onderste waterval van de Palmenkloven (zuidwesten van Saffraanpoort)",
     plek="npc:z6_dadelpit"),
   D("gebied", "poi:z6_palmbaken", 1, "Klim langs de watervallen omhoog naar het Palmbaken, boven op het plateau", plek="poi:z6_palmbaken"),
   baken("palm"),
   D("praat", "z6_twinkel", 1, "Vertel Sterrenkijker Twinkel in de Sterrenwacht van Saffraanpoort dat het Palmbaken brandt", plek="npc:z6_twinkel")],
  [("npc", "De Palmenkloven liggen ten zuidwesten van de stad, langs de rivier. Volg het water naar de zee, tot je de watervallen hoort."),
   ("npc", "Dadelpit weet de weg naar boven. En pas op voor de Mika's met de grote doverhoedjes. Lampendovers. Zij doven de bakens.")],
  "Palmguh Dadelpit, de watervallen op, het Palmbaken aansteken, en terug naar Twinkel.",
  [("npc", "Het Palmbaken brandt! Ik zag de straal vanaf mijn kijker. Kijk, de lens heeft een scherfje gevangen."),
   ("npc", "En die stem in het licht... Moeder Bron. Dus ze bestaat echt. Wat zei ze? Een rietje van grauw? Brrr."),
   ("npc", "Zeg, je hebt sterrenstof op je jas. Tapijtweefster Knoopje in de bazaar kan daar iets moois mee. Iets wat vliegt...")],
  munten=29000, lv=62, na=[HQ[3]], inleveren="z6_twinkel",
  praat={"z6_dadelpit": [
      ("npc", "Een ridder met een Sterrenlens! Twinkel heeft je gestuurd, zie ik. Dan ben je hier voor het Palmbaken."),
      ("npc", "Volg het water omhoog. Langs de drie watervallen. Boven op het plateau staat het baken, tussen de oude palmen."),
      ("npc", "Er sluipen Mika's rond met doverhoedjes. Lampendovers. Als je ze ziet: wegjagen. Ze houden niet van licht.")]})

Q(HQ[5], "hoofd", "z6_knoopje", "Een tapijt van sterrenzijde",
  "Met zijdecocons van de moerbeibomen en het sterrenstof van het Palmbaken kan Tapijtweefster Knoopje een vliegend tapijt weven.",
  [D("geheim", "teller:z6_cocons", 5, "Pluk 5 zijdecocons van de moerbeibomen langs de rivier in de Palmenkloven: zoek de witte glinsters",
     plek="poi:z6_moerbeibomen"),
   D("meld", "z6:geweven", 1, "Breng de cocons naar Tapijtweefster Knoopje in het naaiatelier van de Grote Bazaar (zuidoever van Saffraanpoort)",
     plek="npc:z6_knoopje"),
   D("gebied", "poi:z6_saffraanbrug", 1, "Proefvlucht! Zweef met je tapijt over de daken naar het paviljoen op de Saffraanbrug",
     plek="poi:z6_saffraanbrug"),
   D("praat", "z6_knoopje", 1, "Vlieg terug naar Knoopje in de Grote Bazaar", plek="npc:z6_knoopje")],
  [("npc", "Sterrenstof! Op je jas! Wie heeft je verteld dat ik daar dol op ben? Twinkel. Die kletskous."),
   ("npc", "Met sterrenstof en zijde weef ik een tapijt dat vliegt. Maar ik heb geen zijde meer. De cocons zijn op."),
   ("npc", "Moerbeibomen groeien langs de rivier in de Palmenkloven. Ze zitten vol cocons. Pluk er vijf, dan weef ik.")],
  "Vijf cocons, naar Knoopje in de bazaar, en een proefvlucht naar de Saffraanbrug.",
  [("npc", "Je vliegt! Nou ja, zweeft. Hoog vliegen kan een tapijt niet, het wordt duizelig. Maar over water en zand: heerlijk."),
   ("npc", "Laat hem lekker uitglijden als je stopt. Een tapijt remt niet graag. Het is net als ik op een gladde vloer."),
   ("npc", "En nu: Oma Dadel zoekt je, in de Karavanserai. De karavaan wil naar de Zoutvlakte. Daar staat het tweede baken.")],
  munten=30000, lv=63, na=[HQ[4]], inleveren="z6_knoopje",
  praat={"z6_knoopje": [
      ("npc", "Cocons! Vijf! Ga zitten. Wacht even. Ritsel, klik, zoef..."),
      ("npc", "Klaar. Een tapijt van sterrenzijde. Met kwastjes. Kwastjes zijn belangrijk."),
      ("npc", "Ga erop zitten. Benen gekruist. Nu leun je naar voren, heel zachtjes... Zie je? Hij zweeft! Vlieg eens naar de Saffraanbrug.")]})

Q(HQ[6], "hoofd", "z6_dadel_serai", "De Zoutvlakte bij nacht",
  "De karavaan van Oma Dadel kampeert op de Zoutvlakte. Daar ligt het Zoutbaken, en daar slaapt iets groots onder het zout.",
  [D("gebied", "poi:z6_zoutkamp", 1, "Reis naar het karavaankamp bij het kampvuur op de Zoutvlakte (zuidoost van Saffraanpoort)",
     plek="poi:z6_zoutkamp"),
   baken("zout"),
   D("meld", "z6:zoutkolos", 1, "Ontwijk De Zoutkolos, die wakker wordt bij het Zoutbaken (je hoeft hem niet te verslaan)", plek="poi:z6_zoutbaken"),
   D("gebied", "poi:z6_mikakamp", 1, "Doorzoek het verlaten Mika-kamp aan de oostrand van de Zoutvlakte", plek="poi:z6_mikakamp"),
   D("praat", "z6_twinkel", 1, "Breng wat je gevonden hebt naar Sterrenkijker Twinkel in de Sterrenwacht van Saffraanpoort",
     plek="npc:z6_twinkel")],
  [("npc", "Daar ben je! Met een tapijt, zie ik. Chic. Mijn karavaan gaat vannacht naar de Zoutvlakte. Het Zoutbaken staat daar."),
   ("npc", "We kamperen bij het kampvuur. Ik neurie het karavaanlied, jij steekt het baken aan. Goede verdeling.")],
  "Het karavaankamp, het Zoutbaken, ontsnappen aan de Zoutkolos, het Mika-kamp, en terug naar Twinkel.",
  [("npc", "Een grauwe brief. 'Aan de Ringvinger.' Laat zien... oh nee. Oh nee nee nee."),
   ("npc", "'Mevrouw de Hertogin. De Lege Lens op de toren werkt. Bij elk gedoofd baken drinkt hij meer sterrenlicht. Uw ringen "
           "zijn bijna vol. De oase is bijna verborgen genoeg om leeg te pompen. Uw Knokkel, G.'"),
   ("npc", "Een toren. Bij de Moederoase. Iemand dooft de sterren om de oase leeg te drinken. Voor de Keizer."),
   ("npc", "Er is nog een baken: het Ruïnebaken. Maar de weg erheen staat op een kompas van de oude Sterrenguhs. "
           "En dat ligt in De Verzonken Sterrenwacht.")],
  munten=32000, lv=65, na=[HQ[5]], inleveren="z6_twinkel", items=[(item("grauwe_brief"), 1)])

Q(HQ[7], "hoofd", "z6_twinkel", "De Verzonken Sterrenwacht",
  "In het Ruïnedal ligt De Verzonken Sterrenwacht van de oude Sterrenguhs. Daarin: het Sterrenkompas, dat de weg naar het "
  "Ruïnebaken en naar de oase kent.",
  [D("praat", "z6_stof", 1, "Praat met Professor Stof in het archeologenkamp in het Ruïnedal (noordoosten van Saffraanpoort)", plek="npc:z6_stof"),
   D("gebied", "poi:z6_sterrenwacht_ingang", 1, "Ga naar de ingang van De Verzonken Sterrenwacht, de half in het zand gezakte koepel",
     plek="poi:z6_sterrenwacht_ingang"),
   D("meld", "z6_sterrenwacht:kompas", 1, "Haal het Sterrenkompas uit De Verzonken Sterrenwacht (een kerker: neem vrienden mee)",
     plek="poi:z6_sterrenwacht_ingang"),
   baken("ruine"),
   D("praat", "z6_twinkel", 1, "Breng het Sterrenkompas naar Sterrenkijker Twinkel in de Sterrenwacht van Saffraanpoort", plek="npc:z6_twinkel")],
  [("npc", "Professor Stof graaft al jaren in het Ruïnedal. Hij weet alles van de Sterrenguhs. Vooral wat er onder het zand ligt."),
   ("npc", "Ga eerst naar zijn kamp. En neem vrienden mee naar de Sterrenwacht. Daar zit iets wat de vuurschalen uitblaast.")],
  "Professor Stof, De Verzonken Sterrenwacht, het Sterrenkompas, het Ruïnebaken, en terug naar Twinkel.",
  [("npc", "Het Sterrenkompas! Na zeshonderd jaar. Professor Stof zal wel jaloers zijn. Mag ik het even vasthouden? Heel even?"),
   ("npc", "Kijk: het wijst niet naar het noorden. Het wijst naar waar de sterren samenkomen. Maar daar heb je spiegels voor nodig."),
   ("npc", "Veel spiegels. Duizend spiegels... De Spiegelzaal van de Sultane! Breng het kompas daarheen.")],
  munten=34000, loot="episch", lv=67, na=[HQ[6]], inleveren="z6_twinkel",
  praat={"z6_stof": [
      ("npc", "Een ridder! In mijn kamp! Pas op voor dat potje, daar zit een meteorietbrokje in. Of jam. Ik weet het niet meer."),
      ("npc", "De Verzonken Sterrenwacht ligt daar, half in het zand. De Sterrenguhs bewaarden er hun kompas. Maar er zit nu iets in."),
      ("npc", "Een Mika met een reuzendover. En een oude reuzen-guh van sterrensteen, heel grauw. Wees lief voor hem. Hij is niet boos. Alleen moe.")]})

Q(HQ[8], "hoofd", "z6_twinkel", "De Spiegelzaal",
  "In de Spiegelzaal kaatsen duizend spiegels het licht van de bakens. Met het Sterrenkompas laten ze het verborgen pad naar de "
  "Moederoase zien.",
  [D("gebied", "poi:z6_spiegelzaal", 1, "Breng het kompas naar de Spiegelzaal van het Rozijnenpaleis (noordoever) en zet het midden op de "
     "loper, onder de gouden koepel. De Sultane kijkt mee", plek="poi:z6_spiegelzaal"),
   D("meld", "z6:ontmaskerd", 1, "Kijk wat de spiegels laten zien", plek="poi:z6_spiegelzaal"),
   D("gebied", "poi:z6_dak_1", 1, "Achtervolg de Spiegelvizier over de daken: eerst naar de grote gouden koepel van de bazaar", plek="poi:z6_dak_1"),
   D("gebied", "poi:z6_dak_2", 1, "Achter hem aan naar de windtoren met de roze koepel in de woonwijk", plek="poi:z6_dak_2"),
   D("gebied", "poi:z6_dak_3", 1, "En door naar de Oostpoort, de toren met de guh-oortjes", plek="poi:z6_dak_3"),
   D("meld", "z6:ontsnapt", 1, "Kijk waar de Spiegelvizier heen vlucht, vanaf de Oostpoort", plek="poi:z6_dak_3")],
  [("npc", "Breng het kompas naar de Spiegelzaal. De Sultane weet ervan, ik heb een duif gestuurd. De spiegels doen de rest."),
   ("npc", "Ik zou mee willen, maar ik ben bang voor spiegels. Ze kijken altijd terug.")],
  "De Sultane, het kompas in de Spiegelzaal, en achter de Spiegelvizier aan over de daken.",
  [("npc", "Meester Glans. Twintig jaar heeft hij mijn spiegels gepoetst. Twintig jaar keek hij mee, in elke spiegel."),
   ("npc", "Meester Glans was de Spiegelvizier. Een Mika-illusionist, de Knokkel van de Ringvinger. Hij heeft het spiegelbeeld van het pad gestolen. Maar niet alles."),
   ("npc", "Kijk. In de hoek van de grootste spiegel is nog een stukje blijven hangen. Een pad over het duin, naar het oosten."),
   ("npc", "Het Duinbaken en het Oasebaken zijn de laatste twee. Neem de hele karavaan mee. Oma Dadel wacht al.")],
  munten=36000, loot="episch", lv=68, na=[HQ[7]], inleveren="z6_rozijntje")

Q(HQ[9], "hoofd", "z6_rozijntje", "De Moederoase",
  "De grote karavaan reist bij nacht over het sterrenpad naar de Moederoase. Steek het Duinbaken en het Oasebaken aan, vind Moeder "
  "Bron en kijk de toren van de Ringvinger in de ogen.",
  [D("praat", "z6_dadel_serai", 1, "Haal Oma Dadel en de grote karavaan op in de Karavanserai bij de Westpoort", plek="npc:z6_dadel_serai"),
   baken("duin"),
   D("gebied", "poi:z6_luchtspiegeling", 1, "Volg het sterrenpad over de Oostroute door de luchtspiegeling (tussen de Zoutvlakte en de oase)",
     plek="poi:z6_luchtspiegeling"),
   baken("oase"),
   D("meld", "z6_bron:opgevrolijkt", 1, "Vrolijk Moeder Bron op, de grauwe watergeest onder de Moederpalm aan de oever van de oase",
     plek="npc:z6_bron"),
   D("meld", "z6:toren", 1, "Loop naar de voet van De Sterrendovende Toren, op het rotsje naast de Moederoase", plek="poi:z6_torenvoet"),
   D("praat", "z6_bron", 1, "Ga terug naar Moeder Bron aan de oever van de Moederoase", plek="npc:z6_bron")],
  [("npc", "Twee bakens nog. Het Duinbaken op de hoogste duin, en het Oasebaken bij de oase zelf. Dan is het pad helemaal zichtbaar."),
   ("npc", "Oma Dadel wil de hele karavaan meenemen. Alle karavanen van de stad, zelfs. Dat wordt een optocht. Ga maar. Ik zwaai.")],
  "Oma Dadel, het Duinbaken, de luchtspiegeling, het Oasebaken, Moeder Bron, en de grauwe toren.",
  [("z6_bron", "Ze is naar boven gevlucht. Naar haar Lege Lens. Daar zit ze nu, met haar ringen vol van mijn sterren."),
   ("z6_bron", "Die deur gaat niet open voor één ridder alleen. Een ringslot. Daar zijn er minstens twee voor nodig. Meer is beter."),
   ("z6_bron", "Maar kijk om je heen. Het water stroomt weer. De karavanen zingen. De sterren zijn terug."),
   ("z6_bron", "Kniel even. Ik heb geen kroon, maar ik heb water. Dat is beter."),
   ("z6_bron", "Bij deze ben je Ridder van het Zijdeland. Met een spetter. Die is verplicht.")],
  munten=40000, loot="episch", lv=69, na=[HQ[8]], inleveren="z6_bron", actie_klaar="z6_klaar",
  praat={"z6_dadel_serai": [
      ("npc", "Daar ben je! Kijk eens om je heen. Alle karavanen van Saffraanpoort. Allemaal. Ze gaan mee."),
      ("npc", "Met drie bakens aan durven ze weer. En met jou erbij durven ze alles. Bobbel heeft haar mooiste belletjes om."),
      ("npc", "Eerst het Duinbaken, op de hoogste duin. Dan volgen we de sterren. La-la-LAA-la-lo. Vooruit!")]})

# ===== side quests (15) =====================================================================================================
Q("z6_s01_verjaardag", "zij", "z6_dadel", "Bobbeltjes verjaardag",
  "Bobbeltje, het babybultguhtje van Oma Dadel, is jarig. Er moet een dadeltaart komen. En een feestje.",
  [D("geheim", "teller:z6_taart", 3, "Verzamel 3 taartspulletjes op het Rozenplein (dadels, honing, meel): zoek de gouden glinsters",
     plek="poi:z6_plein"),
   D("gebied", "poi:z6_feest", 1, "Breng de dadeltaart naar de feesttafel naast de fontein op het Rozenplein", plek="poi:z6_feest")],
  [("npc", "Bobbeltje wordt vandaag één! Nou ja, één bult. Ze telt in bulten."),
   ("npc", "Ik bak een dadeltaart, maar ik mis drie dingen: dadels, honing en meel. Ze liggen ergens op het plein. Ik vergeet alles.")],
  "Drie taartspulletjes op het Rozenplein, en dan naar de feesttafel.",
  [("npc", "Kijk haar smullen! Ze heeft taart op haar bult. Op allebei, eigenlijk. Ze heeft er maar één."),
   ("npc", "Dank je, ridder. Bobbeltje vindt je lief. Ze heeft je schoen opgegeten. Dat is een compliment.")],
  munten=9000, lv=60, na=[HQ[1]], actie_klaar="z6_huisdier:springpiepje")

Q("z6_s02_tuinen", "zij", "z6_roosmarijn", "Water voor de hangende tuinen",
  "De hangende tuinen van de Rozenkloof hangen over drie richels. Ze hebben dorst, en de wandtrappen zijn steil.",
  [D("gebied", "poi:z6_tuin_1", 1, "Geef water aan de hangende tuin op de eerste richel (westwand)", plek="poi:z6_tuin_1"),
   D("gebied", "poi:z6_tuin_2", 1, "Geef water aan de hangende tuin op de tweede richel (oostwand, de stenen boogbrug over)", plek="poi:z6_tuin_2"),
   D("gebied", "poi:z6_tuin_3", 1, "Geef water aan de hangende tuin op de rand van het plateau (de touwbrug over)", plek="poi:z6_tuin_3")],
  [("npc", "Drie tuinen, drie richels, één gieter. En ik heb hoogtevrees. Wat een beroepskeuze."),
   ("npc", "Wil jij de wandtrappen op? Eerst de westwand, dan de boogbrug over naar de oostwand, en dan helemaal naar boven.")],
  "De drie hangende tuinen op de richels van de Rozenkloof.",
  [("npc", "Ze bloeien! Kijk, ze buigen naar je toe. Dat is een rozenknuffel."),
   ("npc", "Neem deze zaadjes mee. Woestijnrozen. Ze groeien overal. Behalve waar je ze plant.")],
  munten=9000, lv=60, na=[HQ[1]])

Q("z6_s03_post", "zij", "z6_stempel", "Post in de rots",
  "Postguh Stempel heeft zes brieven voor rotswoningen op verschillende richels. Haar pootjes zijn moe.",
  [D("geheim", "teller:z6_post", 6, "Bezorg 6 brieven bij de rotswoningen op de richels van de Rozenkloof: zoek de brievenbusjes met de gele glinster",
     plek="gebied:z6_rozenkloof")],
  [("npc", "Zes brieven. Zes deuren. Drie richels. Twee pootjes. Dat klopt niet."),
   ("npc", "Wil jij ze bezorgen? De brievenbusjes glinsteren geel als je in de buurt komt.")],
  "Zes brieven bij de rotswoningen op de richels.",
  [("npc", "Alles bezorgd! Er kwam zelfs een brief terug. Voor jou. 'Dank je wel, van de buren.'"),
   ("npc", "Hier, een postzegel met een bultguh erop. Heel zeldzaam. Ik heb er maar zeshonderd.")],
  munten=9500, lv=61, na=[HQ[1]])

Q("z6_s04_kwastjes", "zij", "z6_knoopje", "De verloren tapijtkwastjes",
  "De kwastjes van Tapijtweefster Knoopje zijn weggewaaid over Saffraanpoort: op daken, in de bazaar en langs het kanaal.",
  [D("geheim", "teller:z6_kwastjes", 5, "Vind 5 tapijtkwastjes in Saffraanpoort (daken, bazaar, kanaal): zoek de regenboogglinsters",
     plek="gebied:z6_saffraanpoort")],
  [("npc", "Mijn kwastjes! Ze zijn weggewaaid. Ze wilden vliegen, net als de tapijten."),
   ("npc", "Vijf stuks. Op de daken, in de bazaar en bij het kanaal. Met je tapijt kom je er makkelijk bij.")],
  "Vijf kwastjes in Saffraanpoort.",
  [("npc", "Alle vijf! Ze zijn een beetje stoffig, maar ze zijn thuis. Ik maak er iets moois van. Voor jouw tapijt."),
   ("npc", "Regenboogkwastjes. Kijk ze wapperen. Je tapijt is nu het mooiste van de stad. Na het mijne.")],
  munten=10000, lv=63, na=[HQ[5]], actie_klaar="z6_kwastjes")

Q("z6_s05_afdingen", "zij", "z6_kardemom", "Afdingen!",
  "In de Grote Bazaar hoort afdingen erbij. Maar wie lief afdingt, krijgt de meeste korting. Probeer het bij drie kooplieden.",
  [D("meld", "z6_puzzel:afdingen_kardemom", 1, "Ding lief af bij Koopvrouw Kardemom, bij de kruidenkraam onder de grote koepel van de bazaar",
     plek="npc:z6_kardemom"),
   D("meld", "z6_puzzel:afdingen_pistache", 1, "Ding lief af bij Koopman Pistache, bij de kraam met de potten en manden in de bazaar",
     plek="npc:z6_pistache"),
   D("meld", "z6_puzzel:afdingen_lokum", 1, "Ding lief af bij Juwelier Lokum, bij het glazen kraampje in de bazaar", plek="npc:z6_lokum")],
  [("npc", "Ah, een nieuw gezicht! Weet je hoe je afdingt in Saffraanpoort? Niet? Dan leer ik het je."),
   ("npc", "Wie boos afdingt, betaalt meer. Wie lief afdingt, krijgt korting en een vriend. Probeer het maar. Bij mij, bij Pistache en bij Lokum.")],
  "Lief afdingen bij Kardemom, Pistache en Lokum (praat met ze: de tracker wijst ze aan).",
  [("npc", "Drie kooplieden, drie vrienden, en geen boze snor. Jij bent een echte Sjacheraar."),
   ("npc", "Die titel mag je dragen. Iedereen in de bazaar weet nu: met jou valt te praten.")],
  munten=10000, lv=62, na=[HQ[3]], actie_klaar="z6_sjacheraar")

Q("z6_s06_norias", "zij", "z6_knars", "De norias zingen niet meer",
  "De drie grote waterraderen van Saffraanpoort kraken niet meer. Ze staan stil. Molenguh Knars hoort het in zijn tanden.",
  [D("gebied", "poi:z6_noria_1", 1, "Smeer de westelijke noria op de zuidoever van de Saffraanrivier", plek="poi:z6_noria_1"),
   D("gebied", "poi:z6_noria_2", 1, "Zet de tandwielen recht van de grote noria naast de Saffraanbrug", plek="poi:z6_noria_2"),
   D("gebied", "poi:z6_noria_3", 1, "Haal het zand uit de oostelijke noria op de noordoever, voorbij de Paradijstuin", plek="poi:z6_noria_3")],
  [("npc", "Hoor je dat? Niks. Dat is het probleem. Een noria hoort te zingen. Krrr-iiie, krrr-iiie."),
   ("npc", "De rivier is zo laag dat de raderen vol zand zitten. Wil jij ze nakijken? Drie stuks. Ik ben te oud voor de kade.")],
  "De drie norias langs de rivier.",
  [("npc", "Krrr-iiie! Krrr-iiie! Ze zingen weer! Hoor je het? Het is het mooiste lied van de stad."),
   ("npc", "Mijn tanden doen geen pijn meer. Dank je, ridder.")],
  munten=10000, lv=62, na=[HQ[3]])

Q("z6_s07_eendjes", "zij", "z6_pluk", "Badeendjes in de Kanaalwijk",
  "De tien badeendjes van Kindje Pluk zijn weggedreven door de kanaaltjes van de Kanaalwijk.",
  [D("geheim", "teller:z6_eendjes", 10, "Vang 10 badeendjes in de kanaaltjes van de Kanaalwijk (met je tapijt laag over het water): zoek de "
     "gele spetters", plek="poi:z6_kanaalwijk")],
  [("npc", "Kwak! Kwak! Kwak! Ze heten allemaal Kwak. En ze zijn allemaal weg."),
   ("npc", "Ze drijven in de kanaaltjes. Als je met je tapijt laag over het water vliegt, kun je ze pakken.")],
  "Tien badeendjes in de Kanaalwijk.",
  [("npc", "Alle tien! Kwak, Kwak, Kwak, Kwak, Kwak, Kwak, Kwak, Kwak, Kwak en... Kwak! Je bent de beste."),
   ("npc", "Je mag er eentje houden. Hij heet Kwak.")],
  munten=10500, lv=63, na=[HQ[5]])

Q("z6_s08_examen", "zij", "z6_twinkel", "Sterrenkijkersexamen",
  "Twinkel wil weten of je echt een sterrenkijker bent. Vier sterrenbeelden, vier hoge plekken.",
  [D("meld", "z6_puzzel:examen_1", 1, "Verbind De Kleine Pluis op het dak van de Sterrenwacht van Twinkel", plek="poi:z6_examen_1"),
   D("meld", "z6_puzzel:examen_2", 1, "Verbind De Grote Knuffel op de bovenste galerij van de Saffraanbrug", plek="poi:z6_examen_2"),
   D("meld", "z6_puzzel:examen_3", 1, "Verbind De Vliegende Bultguh op de westelijke minaret van het Rozijnenpaleis", plek="poi:z6_examen_3"),
   D("meld", "z6_puzzel:examen_4", 1, "Verbind Het Kroontje op de Zeemond-pier in de Rozenkloof", plek="poi:z6_examen_4")],
  [("npc", "Een echte sterrenkijker kent de sterrenbeelden uit zijn hoofd. Nou ja, uit een boekje. Dat mag ook."),
   ("npc", "Vier plekken, vier sterrenbeelden. De laatste is in de Rozenkloof. Daar zie je het Kroontje het best.")],
  "Vier sterrenbeelden op vier hoge plekken.",
  [("npc", "Geslaagd! Met een tien. Nou ja, een negen. Bij de Grote Knuffel stond je even op mijn teen."),
   ("npc", "Wist je dat de Kleine Pluis, de Grote Knuffel en de Vliegende Bultguh de oudste sterrenbeelden zijn? In de toren zijn ze grauw "
           "geworden, zeggen ze. Dat is niet om aan te denken.")],
  munten=11000, lv=64, na=[HQ[4]])

Q("z6_s09_dadelapen", "zij", "z6_dadelpit", "De Dadelapenbende",
  "Een bende dadelaapjes heeft de dadeloogst van Dadelpit gepikt en in de palmen verstopt.",
  [D("geheim", "teller:z6_dadels", 6, "Vind 6 mandjes gepikte dadels in het palmenbos onder de watervallen van de Palmenkloven: zoek de "
     "bruine glinsters", plek="poi:z6_dadelpalmen")],
  [("npc", "Mijn dadels! Weg! De dadelaapjes hebben ze gepikt. Ze gooien ze naar me als ik kom kijken."),
   ("npc", "Ze verstoppen ze in mandjes boven in de palmen. Zes mandjes. Haal ze terug. Bukken!")],
  "Zes mandjes dadels in het palmenbos.",
  [("npc", "Mijn dadels! Allemaal! En je hebt zelfs geen dadel tegen je hoofd gekregen. Of wel? Je hebt een bult."),
   ("npc", "Een van de aapjes volgt je, zie ik. Hij vindt je leuk. Dat is een goed teken.")],
  munten=11000, lv=63, na=[HQ[4]], actie_klaar="z6_huisdier:dadelaapje")

Q("z6_s10_koerier", "zij", "z6_spetter", "Watervalkoerier",
  "Koerier Spetter brengt de post langs de watervallen van de Palmenkloven. Kun jij het sneller?",
  [D("gebied", "poi:z6_waterval_1", 1, "Start bij de onderste waterval van de Palmenkloven", plek="poi:z6_waterval_1"),
   D("gebied", "poi:z6_waterval_2", 1, "Door naar de middelste waterval, achter het watergordijn", plek="poi:z6_waterval_2"),
   D("gebied", "poi:z6_waterval_3", 1, "En helemaal naar boven, naar de bovenste waterval onder het Palmbaken", plek="poi:z6_waterval_3")],
  [("npc", "Drie watervallen, van onder naar boven. Ik doe het in drie minuten. Met een natte staart."),
   ("npc", "Met een tapijt ben je sneller. Of je verdrinkt. Een van de twee. Klaar? Spetteren maar!")],
  "Langs de drie watervallen omhoog.",
  [("npc", "Je bent er al? En je bent bijna droog! Dat is een record. Een nieuw record. Mijn record."),
   ("npc", "Neem dit mee. Een spettervlaggetje. Voor de snelste koerier van de kloven.")],
  munten=11500, lv=64, na=[HQ[4]])

Q("z6_s11_zoutkreeft", "zij", "z6_korrel", "Zout op de staart",
  "Zoutkreeftjes hebben een feestje gevierd in het ziedershuisje van Zoutzieder Korrel. Het huisje is half ingestort.",
  [D("geheim", "teller:z6_zoutkreeft", 5, "Vang 5 zoutkreeftjes op de Zoutvlakte: zoek de roze glinsters in het zout", plek="poi:z6_zoutvlakte"),
   D("gebied", "poi:z6_ziedershuisje", 1, "Herstel het zoutziedershuisje aan de zuidrand van de Zoutvlakte", plek="poi:z6_ziedershuisje")],
  [("npc", "Zoutkreeftjes! Ze lopen zijwaarts, ze knippen alles stuk, en ze vinden mijn huisje een feestzaal."),
   ("npc", "Vang er vijf en breng ze ver weg. Draai ze om, dan zijn ze heel lief. En dan moeten we mijn huisje opknappen.")],
  "Vijf zoutkreeftjes, en het ziedershuisje opknappen.",
  [("npc", "Mijn huisje staat weer! Scheef, maar dat was het altijd al. Het is een karaktervol huisje."),
   ("npc", "Hoor je dat? Onder de vloer zingt iets. De Zoutgrot. Kom morgen terug, dan vertel ik je erover.")],
  munten=12000, lv=65, na=[HQ[6]])

Q("z6_s12_zoutgrot", "zij", "z6_korrel", "Het lied van de Zoutgrot",
  "Onder de Zoutvlakte ligt de Zingende Zoutgrot. Achter een wand van kristal ligt de kist van de Eerste Karavaan.",
  [D("gebied", "poi:z6_zoutgrot", 1, "Ga de Zingende Zoutgrot in, door de spleet aan de zuidrand van de Zoutvlakte", plek="poi:z6_zoutgrot"),
   D("meld", "z6_puzzel:lied", 1, "Speel het karavaanlied op de zingende kristallen aan het lichtgevende meertje", plek="poi:z6_grot_kristallen"),
   D("gebied", "poi:z6_grot_schat", 1, "Open de kist van de Eerste Karavaan achter de kristalwand", plek="poi:z6_grot_schat")],
  [("npc", "De Zoutgrot zingt. Elk kristal één toon. En achter een wand van kristal ligt een kist, zeggen ze. Van de Eerste Karavaan."),
   ("npc", "De wand gaat alleen open voor het goede lied. Het karavaanlied. Ken jij het? Oma Dadel neuriet het altijd."),
   ("npc", "En pas op voor de Zoutslang. Die zwemt door de wanden. Wees zacht.")],
  "De Zingende Zoutgrot, het karavaanlied op de kristallen, en de kist van de Eerste Karavaan.",
  [("npc", "De kist van de Eerste Karavaan! Wat zat erin? Een tapijtpatroon? Het patroon van de Eerste Karavaan. Wat een eer."),
   ("npc", "En een briefje: 'Wie dit vindt, deelt het. Zo begon het. Zo blijft het.' Mooi, hè?")],
  munten=14000, loot="zeldzaam", lv=66, na=["z6_s11_zoutkreeft"], actie_klaar="z6_eerste_karavaan")

Q("z6_s13_wormelien", "zij", "z6_rimpel", "Koningin Wormelien",
  "Koningin Wormelien, de grote roze zandworm die vroeger voor de karavanen zong, zwemt grauw en hongerig door de Roze Duinen.",
  [D("meld", "z6_baas:wormelien", 1, "Vrolijk Koningin Wormelien op, in het hart van de Roze Duinen (veldbaas: neem vrienden mee)",
     plek="poi:z6_wormelien")],
  [("npc", "Ze zong altijd. Als een karavaan verdwaalde, zong Wormelien de weg. Nu bromt ze alleen nog."),
   ("npc", "Er zit een grauwe zoutkorst op haar rug. Klop die eraf. Spring vanaf een duin. Ze is groot. Neem vrienden mee.")],
  "Koningin Wormelien opvrolijken in de duinen.",
  [("npc", "Ze zingt weer! Hoor je het? Ze ligt te slapen in het zand en ze zingt in haar slaap."),
   ("npc", "Kijk, ze heeft dit voor je achtergelaten. Een zadeldeken in roze. Voor je bultguh.")],
  munten=16000, loot="episch", lv=63, na=[HQ[2]])

Q("z6_s14_tabletten", "zij", "z6_stof", "De tabletten van de Sterrenguhs",
  "Professor Stof zoekt zes stenen tabletten van de oude Sterrenguhs. Ze liggen verspreid tussen de mesa's van het Ruïnedal.",
  [D("geheim", "teller:z6_tabletten", 6, "Vind 6 stenen tabletten in het Ruïnedal: zoek de blauwe glinsters tussen de mesa's",
     plek="poi:z6_ruinedal")],
  [("npc", "Zes tabletten. Samen vertellen ze het verhaal van de Sterrenguhs. Ik heb er nul. Dat is weinig."),
   ("npc", "Ze glinsteren blauw als je dichtbij bent. De sterren hebben ze gemerkt, denk ik. Of ik. Ik weet het niet meer.")],
  "Zes tabletten tussen de mesa's.",
  [("npc", "Alle zes! Ik lees: 'Wij telden de sterren. Eén voor één. En toen we klaar waren, deelden we ze uit.'"),
   ("npc", "En deze: 'Er kwam een kat met een kroontje. Hij telde ook. Maar hij deelde niet. Hij telde alleen wat hij had. Nooit genoeg.'"),
   ("npc", "Een kat met een kroontje... Dat klinkt als een sprookje dat Mika's elkaar vertellen. Vreemd.")],
  munten=14000, lv=67, na=[HQ[7]])

Q("z6_s15_oostroute", "zij", "z6_mientje", "Vluchtelingen langs de Oostroute",
  "Langs de Oostroute komen Uitgevadsten uit het oosten naar de Moederoase. Mager Mientje uit Hoogvads vangt ze op.",
  [D("meld", "z6_soep:kom", 3, "Geef een kom soep aan 3 Uitgevadsten bij de vluchtelingententen aan de oostkant van de Moederoase",
     plek="npc:z6_vluchteling_1")],
  [("npc", "Ridder! Ik ben het, Mientje! Ik kwam met de walvis mee, en toen met een karavaan. Ik wilde helpen."),
   ("npc", "Ze komen uit het oosten, langs de Oostroute. Net zo mager als ik toen. Geef ze soep. Soep helpt altijd.")],
  "Een kom soep voor drie Uitgevadsten.",
  [("npc", "Kijk ze eten. Ze worden al een beetje roze. Net als ik toen."),
   ("npc", "Er komen er nog meer. Ik blijf hier. Iemand moet de soep roeren. En ik ben er goed in.")],
  munten=16000, lv=69, na=[HQ[9]])

# ===== quest xp (the same curve as zones 3-5; lv up to 70) =====================================================================
# W-BALANS: a main step = 80 % of a level (was 45 %), a side quest = 22 % (was 18 %). Zone 6 spans 9 levels (60 -> 69) with 10
# main steps whose gates climb ~1 level per step; with 45 % a main-only player ground ~2 h and even main+side ~45 min between
# steps. See tools/vadsrijk/xpbalans.py and XP_BALANS.md.
Z6_HOOFD, Z6_ZIJ = 0.80, 0.22


def z6xp(lv, hoofd):
    l = max(1, min(99, lv))
    stap = max(1, round(40 + 25 * l ** 1.6))
    return max(1, round(stap * (Z6_HOOFD if hoofd else Z6_ZIJ)))


LV_CAP = 70   # (U-Z6VERHAAL: kern Levels.MAX / MAX_GENERIEK raised to 70)
for _q in QUESTS:
    _q["beloning"]["xp"] = z6xp(_q["lv"], _q["soort"] != "zij")
    _q["lv"] = min(_q["lv"], LV_CAP)

# ---------------------------------------------------------------------------------------------------------------------------
# Secrets: 8 knuffels, 3 treasures (Z6VerhaalPlekken.KNUFFEL / SCHAT) + the side-quest finds (VONDST)
# ---------------------------------------------------------------------------------------------------------------------------
KNUFFELS = {
    "knuffel_1": "Rotspluisje", "knuffel_2": "Duinbolletje", "knuffel_3": "Dadeltje", "knuffel_4": "Koepelkatje", "knuffel_5": "Zoutvlokje",
    "knuffel_6": "Sterrenstofje", "knuffel_7": "Spiegelbeertje", "knuffel_8": "Palmpje",
}
SCHATTEN = {
    "schatkamer": ("In de Schatkamer", "Achter de zuilen van de Schatkamer zit een nis die niemand ziet. Behalve jij. Er staat een kist in, "
                   "met een strikje."),
    "minaret": ("Boven in de minaret", "Boven in de minaret met de guh-oortjes, tussen de oortjes in, zit een kistje vastgeknoopt. "
                "Zo hoog durft niemand. Alleen ridders en duiven."),
    "mesa": ("Op de Tafelmesa", "Op de Tafelmesa, onder een platte steen, ligt een kist van de oude Sterrenguhs. Er zit een briefje in: "
             "'Voor wie hoog genoeg kijkt.'"),
}
TABLETTEN = {
    "tablet_1": "Wij, de Sterrenguhs, telden de sterren. Eén voor één. Het duurde honderd jaar en drie theepotten.",
    "tablet_2": "Toen we klaar waren, deelden we ze uit. Elke karavaan kreeg een ster. Zo vond iedereen de weg naar het water.",
    "tablet_3": "Wij bouwden vijf bakens om de sterren vast te houden als het bewolkt was. Palm, Zout, Ruïne, Duin en Oase.",
    "tablet_4": "Onder de Moederpalm woont Bron. Ze geeft water aan wie deelt. Ze heeft nog nooit nee gezegd.",
    "tablet_5": "Er kwam een kat met een kroontje. Hij telde ook. Maar hij deelde niet. Hij telde alleen wat hij had.",
    "tablet_6": "Hij noemde het: nooit genoeg. Wij noemden het: verdrietig. Toen gingen wij slapen onder het zand.",
}
VONDST_TEKST = {   # the found-message per side-quest find kind (%s/%s)
    "z6_taart": "Een taartspulletje! (%s/%s)",
    "z6_post": "Brief bezorgd! (%s/%s)",
    "z6_kwastjes": "Een tapijtkwastje! (%s/%s)",
    "z6_eendjes": "Badeendje Kwak gevangen! (%s/%s)",
    "z6_dadels": "Een mandje dadels terug! (%s/%s)",
    "z6_zoutkreeft": "Een zoutkreeftje omgedraaid en gevangen! (%s/%s)",
    "z6_tabletten": "Een stenen tablet! (%s/%s)",
    "z6_cocons": "Een zijdecocon! (%s/%s)",
}

# ---------------------------------------------------------------------------------------------------------------------------
# Gids pages (questlog J, tab Gids: ids z6_* in QuestlogScherm.GIDS), tips, scene and other texts
# ---------------------------------------------------------------------------------------------------------------------------
GIDS = {
    "z6_zijdeland": ("Zone 6: Het Zijdeland", "Na Hoogvads en vanaf level 60 vliegt lijn E van de Staarthaven van Hoogvads naar de Luchthaven van "
                     "Rozenkloof, op de westkust van het Grote Vasteland (lv 60-70). Het Zijdeland heeft twee steden: Rozenkloof (de kloofstad, "
                     "aankomst) en Saffraanpoort (de hoofdstad aan de rivier), en vijf gebieden: de Roze Duinen (60-62), de Palmenkloven (62-64), "
                     "de Zoutvlakte (64-66), het Ruïnedal (66-68) en de Moederoase (68-70)."),
    "z6_bakens": ("De sterrenbakens", "Vijf bakens wijzen de karavanen de weg naar de Moederoase. Bij een baken verbind je het sterrenbeeld: "
                  "volg de sterren in de volgorde van de hint. Een verkeerde ster? Geen probleem: je begint gewoon opnieuw. Overdag laat "
                  "de Sterrenlens je de nachthemel zien. Rechtsklik de lens om te zien hoeveel bakens al branden."),
    "z6_rijdieren": ("Bultguh en vliegend tapijt", "In het Zijdeland krijg je twee nieuwe rijdieren. De bultguh (bij Stalmeester Hobbel, in "
                     "'De karavaan van Oma Dadel') is snel op zand en heeft twee zitjes. Het vliegende tapijt (Tapijtweefster Knoopje, "
                     "'Een tapijt van sterrenzijde') zweeft over zand en water en glijdt uit als je stopt."),
    "z6_steden": ("Rozenkloof en Saffraanpoort", "In beide steden vind je een bankier, een Kwartiermeester (naar het Knuffelkwartier), een "
                  "Bondenbord (Knuffelbonden) en kampvuren. In de Grote Bazaar van Saffraanpoort staan de kooplieden en de leermeesters van "
                  "smeden en naaien; koken leer je in de Karavanserai, brouwen onder de Sterrenwacht en imkeren in de Paradijstuin."),
    "z6_toren": ("De Sterrendovende Toren", "Aan het eind van 'De Sterrenkaravaan' verzegelt Hertogin Ringeling haar toren met een ringslot. "
                 "Het verhaal van het Zijdeland is dan af. De toren zelf is een raid voor 2 tot 10 spelers: daar wacht de echte eindbaas."),
}
TIPS = {
    "z6_veilig": ("Veilig!", "Rozenkloof en Saffraanpoort zijn veilig: in de steden vallen geen monsters aan. Kampvuren werken als rustpunt."),
    "z6_puzzel": ("Sterren lezen", "Lees de hint goed: die vertelt in welke volgorde je de sterren verbindt. Fout? Dan begin je opnieuw."),
    "z6_lens": ("De Sterrenlens", "Rechtsklik de Sterrenlens: je ziet hoeveel bakens al branden en waar het volgende staat."),
}
OVERIG = {
    "vadsrijk.voltooiing.zone.zone6": "Het Zijdeland",
    "gui.guhs.titels.naam.vr_kenner_van_het_zijdeland": "Kenner van het Zijdeland",
    "gui.guhs.titels.hint.vr_kenner_van_het_zijdeland": "Verken het Zijdeland helemaal (Verkenningsboek, toets B).",
    "gui.guhs.titels.naam.vr_ridder_van_het_zijdeland": "Ridder van het Zijdeland",
    "gui.guhs.titels.hint.vr_ridder_van_het_zijdeland": "Speel 'De Sterrenkaravaan' uit.",
    "gui.guhs.titels.naam.vr_sjacheraar": "Sjacheraar",
    "gui.guhs.titels.hint.vr_sjacheraar": "Ding lief af bij drie kooplieden in de Grote Bazaar van Saffraanpoort.",
    "gui.guhs.titels.naam.vr_vriend_van_ringeling": "Vriend van Ringeling",
    "gui.guhs.titels.hint.vr_vriend_van_ringeling": "Vrolijk Hertogin Ringeling op, boven in De Sterrendovende Toren.",
    # travel
    P + "optie.vlieg.zijdeland": "Naar het Zijdeland (Rozenkloof)",
    P + "optie.vlieg.zijdeland.slot": "Naar het Zijdeland (nog dicht)",
    P + "reis.slot.zijdeland": "Lijn E naar het Zijdeland gaat pas open als Moeder Wolkwal naar het oosten gezwommen is (na 'De Stormrand' in "
                               "Hoogvads) en je level 60 bent (jij: level %s).",
    P + "reis.slot.zijdeland.kort": "Na Hoogvads, vanaf level 60 (jij: %s)",
    P + "reiskaart.plek.zijdeland": "Het Zijdeland",
    P + "reis.naam.zijdeland": "het Zijdeland",
    "vadsrijk.luchtschip.haven.haven_z5_z6_vertrek": "De Staarthaven (Hoogvads)",
    "vadsrijk.luchtschip.haven.haven_z6_z5_vertrek": "Luchthaven Rozenkloof",
    "vadsrijk.luchtschip.lijn.z5_z6": "Lijn E: Hoogvads - Rozenkloof",
    "vadsrijk.luchtschip.slot.z6": "Lijn E gaat pas open als Moeder Wolkwal naar het oosten gezwommen is (na Hoogvads) en je level 60 bent.",
    # roles
    P + "optie.rol.z6_tapijtwinkel": "Wat verkoop je?",
    # hub NPCs without their slice in the tree
    Z + "binnenkort.kwartiermeester": "Het Knuffelkwartier gaat binnenkort open. Dan breng ik je erheen. Beloofd.",
    Z + "binnenkort.bondenbord": "Mijn bord is nog leeg. De Knuffelbonden komen binnenkort. Dan prikken we alles vol.",
    Z + "tapijtwinkel": "Gewone tapijten verkoop ik. Vliegende maakt Knoopje, in de Grote Bazaar van Saffraanpoort. Patronen voor je tapijt "
                        "leer je straks met Naaien. Of je vindt ze. Op de gekste plekken.",
    Z + "leermeester.binnenkort": "Mijn werkplaats is bijna klaar. Kom straks terug.",
    # areas, bosses
    Z + "baas.wormelien": "Koningin Wormelien",
    Z + "baas.zoutkolos": "De Zoutkolos",
    Z + "baas.sterrenguh": "De Verzonken Sterrenguh",
    Z + "baas.ringeling": "Hertogin Ringeling",
    # finds
    Z + "knuffel.gevonden": "Knuffel gevonden: %s! (%s/%s)",
    Z + "schat.titel": "Schat gevonden!",
    Z + "tablet.naam": "Stenen tablet",
    Z + "lens.stand": "Sterrenlens: %s van de 5 bakens branden.",
    Z + "lens.volgende": "Het volgende baken: %s.",
    Z + "lens.alle": "Alle vijf de bakens branden. De Moederoase is zichtbaar.",
    Z + "bakens.palm": "Het Palmbaken", Z + "bakens.zout": "Het Zoutbaken", Z + "bakens.ruine": "Het Ruïnebaken",
    Z + "bakens.duin": "Het Duinbaken", Z + "bakens.oase": "Het Oasebaken",
    Z + "baken.brandt": "%s brandt weer!",
    Z + "baken.al": "%s brandt al. De sterren knipogen naar je.",
    Z + "baken.nog_niet": "Dit baken is nog niet aan de beurt. Volg eerst het verhaal (je tracker wijst de weg).",
    # spots
    Z + "klus.dadels": "Je tilt de manden met dadels op de karavaan. Ze ruiken zoet.",
    Z + "klus.water": "Je vult de waterzakken bij de fontein. Bobbel drinkt er stiekem eentje leeg.",
    Z + "klus.tapijten": "Tapijtkoopman Franje rolt drie tapijten op je armen. \"Niet laten vallen. Ze zijn honderd jaar oud.\"",
    Z + "bobbeltje": "Achter de duin zit Bobbeltje, met zand op haar bult en een dadel in haar snuitje. Ze huppelt mee terug.",
    Z + "tuin.water": "Je geeft de hangende tuin water. Rozenblaadjes dwarrelen naar beneden de kloof in.",
    Z + "noria": "Je knutselt aan de noria. Krrr... krrr-iiie! Hij draait weer.",
    Z + "ziedershuisje": "Je zet de muurtjes van het ziedershuisje recht. Het staat. Scheef, maar het staat.",
    Z + "waterval": "Spetter! Je komt langs de waterval.",
    Z + "grot.schat": "De kist van de Eerste Karavaan gaat open. Er ligt een tapijtpatroon in, en een briefje.",
    Z + "feest": "Bobbeltje springt op de taart. Iedereen juicht. Oma Dadel zingt.",
    Z + "kompas.fallback": "Diep in De Verzonken Sterrenwacht vind je, na de vuurschalen en de ringen, een grote grauwe guh van sterrensteen. "
                           "Je geeft hem een knuffel. Hij gloeit, heel zacht, en legt het Sterrenkompas in je poten.",
    Z + "kerker.binnen": "De Verzonken Sterrenwacht. Het ruikt naar oud koper en sterrenstof. Er klinkt gesnurk van steen.",
    Z + "soep.kom": "Je geeft een kom soep. De Uitgevadste wordt een klein beetje roze. (%s/3)",
    Z + "soep.al": "Deze guh heeft al soep. Hij geeft je een dankbaar knikje.",
    Z + "bron.slaapt": "Moeder Bron slaapt, grauw en uitgedroogd. Ze mompelt: \"...sterren... nog niet... allemaal...\"",
    Z + "glans.spiegel": "Waar Meester Glans stond, staat nu een spiegel in de vorm van een guh. Hij was nooit echt hier.",
    Z + "glans.spiegel.naam": "Een spiegel",
    Z + "toren.dicht": "De poort van De Sterrendovende Toren zit dicht met een ringslot. Daar zijn er minstens twee voor nodig.",
    # the end of the story, the epilogue after the raid, rewards that wait for their slice
    Z + "zone6.klaar.titel": "Ridder van het Zijdeland",
    Z + "zone6.klaar.onder": "De sterren branden weer",
    Z + "zone6.klaar": "Het verhaal van het Zijdeland is af! De karavanen vinden de Moederoase weer. Boven in De Sterrendovende Toren "
                       "wacht Hertogin Ringeling nog: dat is de raid, voor 2 tot 10 spelers.",
    Z + "epiloog.0": "Bij de Moederoase komen de karavanen terug, één voor één, met belletjes en liedjes. Het water van de Saffraanrivier stijgt weer.",
    Z + "epiloog.1": "Oma Dadel zet thee. Bobbeltje springt in de oase. En in Saffraanpoort, in de Paradijstuin, zit een gesluierde dame met tien "
                     "blote tenen naar de sterren te kijken.",
    Z + "beloning.wacht.bultguh": "(Je bultguh staat klaar bij Hobbel. Hij komt naar je toe zodra de bultguhs in het spel zitten.)",
    Z + "beloning.wacht.tapijt": "(Je tapijt is geweven. Het komt naar je toe zodra de vliegende tapijten in het spel zitten.)",
    Z + "beloning.wacht.uiterlijk": "(Dit tapijtpatroon komt naar je toe zodra de vliegende tapijten in het spel zitten.)",
    Z + "wormelien.zicht": "In de verte breekt een duin open. Een reusachtige roze zandworm met wimpers rolt door het zand, grauw en "
                           "hongerig, en duikt weer onder. Koningin Wormelien.",
    # the Grauwe brief (hq07)
    Z + "brief.naam": "Een grauwe brief",
    Z + "brief.naam.ringvinger": "Aan de Ringvinger",
    Z + "brief.0": "In een grauw tentje ligt een envelop met een zegel: een ring. Erop staat: 'Aan de Ringvinger.'",
    Z + "brief.1": "\"Mevrouw de Hertogin. De Lege Lens op de toren werkt. Bij elk gedoofd baken drinkt hij meer sterrenlicht. Uw ringen zijn "
                   "bijna vol. De oase is bijna verborgen genoeg om leeg te pompen. Uw Knokkel, G.\"",
    Z + "brief.2": "Eronder, in een sierlijk handschrift met een glinstertje: \"Heerlijk. Tien tenen, tien ringen, alle sterren. - R.\"",
    # Ringeling as an ally (after the raid; she wears a veil until then)
    Z + "ringeling.voor.0": "De gesluierde dame kijkt naar haar tenen. Tien blote tenen. \"Ken ik jou? Nog niet, schat. Nog niet.\"",
    Z + "ringeling.na.0": "\"Jij! Mijn ridder! Kom hier. Ik ben het, Ringeling. Die sluier? Niet iedereen weet al dat ik nu aardig ben.\"",
    Z + "ringeling.na.1": "\"Weet je wat ik ontdekte toen je mijn ringen afpakte? Dat ik ze niet miste. Ik voelde iets warms. Vads. Van niemand "
                          "gestolen.\"",
    Z + "ringeling.na.2": "\"De Keizer heeft niks meer aan mij. Ik heb hem een briefje gestuurd. Met een hartje. Hij haat hartjes.\"",
    Z + "ringeling.na.3": "\"Als je ooit naar het oosten gaat, naar zijn Vingers: ik weet waar ze hun sokken bewaren. Kom maar langs.\"",
    Z + "ringeling.naam": "Hertogin Ringeling",
}
# Scene texts (Z6Verhaal.SCENES): id -> (title, subtitle, [lines])
SCENES = {
    "wolkwaltocht": ("De grote zwemtocht", "Moeder Wolkwal zwemt naar het oosten", [
        "Moeder Wolkwal laat een diepe, blije brom horen. Heel Hoogvads trilt. De Koningin zwaait vanaf haar balkon.",
        "Ze zwemt. Langzaam eerst, dan sneller. Wolken stromen langs haar flanken. De Wolkenzee schuift onder je door.",
        "In het oosten doemt een roze streep op. Kliffen, zo roze als een wang. Daarachter: duinen, koepels, palmen.",
        "Moeder Wolkwal blaast een wolkje uit haar spuitgat. Het vormt twee woorden: \"Tot hier.\"",
        "Ze legt zich stil boven de zeestraat. Op de Staarthaven gaan de lampen aan: lijn E naar het Zijdeland is open!"]),
    "aankomst": ("Rozenkloof", "Het Zijdeland", [
        "Het luchtschip daalt over de roze kliffen. De Zijdewind ruikt naar rozen en kaneel.",
        "Onder je opent een kloof, diep en roze. In de wanden: deuren, balkonnetjes, hangende tuinen, lampionnen.",
        "Op de bodem glinstert een stervormige fontein. Een karavaan met bultguhs maakt zich klaar.",
        "Welkom in Rozenkloof! Havenmeester Zandloper staat al te zwaaien met zijn zandloper."]),
    "visioen": ("Moeder Bron", "een stem uit het water", []),
    "zoutkolos": ("De Zoutkolos", "is wakker", [
        "Het zout kraakt. Barsten schieten als bliksem over de vlakte, naar het baken toe.",
        "Een reus van samengekoekt sterrenzout komt overeind. Zijn binnenste gloeit grauw.",
        "Oma Dadel fluistert: \"Niet vechten. Rennen. Hij is langzaam, en wij zijn lief en snel.\"",
        "De Zoutkolos stampt naar het baken, ziet het branden en blijft verward staan. Jullie glippen weg."]),
    "kerker": ("De Verzonken Sterrenwacht", "het Sterrenkompas", []),
    "ontmasker": ("Ontmaskerd!", "Meester Glans, de Spiegelvizier", [
        "Je zet het Sterrenkompas op de loper. Duizend spiegels vangen het licht van de bakens. Een gloeiend pad schiet door de zaal.",
        "In elke spiegel staat Meester Glans. Ook in de spiegels waar hij niet voor staat.",
        "\"Wat een mooi pad. Mag ik het even lenen?\" Meester Glans pakt het spiegelbeeld van het pad, rolt het op als een tapijt...",
        "...en zijn gezicht schuift weg als een raampje. Daaronder: een Mika met een snor van spiegelscherfjes.",
        "\"Meester Glans, de Spiegelvizier. Knokkel van de Ringvinger. Twintig jaar heb ik gepoetst. Twintig jaar! Mijn poten zijn glimmend.\"",
        "Hij springt door een spiegel en komt op het dak eruit. Achter hem aan!"]),
    "ontsnapt": ("Gevlucht", "naar de toren", [
        "Vanaf de Oostpoort zie je hem gaan: de Spiegelvizier, op een tapijt van spiegelglas, recht naar het oosten.",
        "Ver weg, achter de duinen, staat iets wat er niet hoort. Een grauwe toren. Gouden ringen draaien eromheen.",
        "Bovenin glimt een lens. Hij zuigt het laatste beetje schemer uit de lucht, in spiralen naar binnen."]),
    "luchtspiegeling": ("De luchtspiegeling", "het sterrenpad", [
        "De lucht trilt. Overal zie je meren, palmen en torens. Ze zijn er allemaal niet.",
        "Oma Dadel zingt het karavaanlied. De hele karavaan zingt mee. La-la-LAA-la-lo.",
        "Vier sterrenstralen boven je wijzen dezelfde kant op. Volg ze. Niet de meren. De sterren."]),
    "bron": ("Moeder Bron", "wordt wakker", [
        "Je knielt bij de grauwe guh van water en legt je poot op haar hoofd. Ze is koud. En zo licht als een droge spons.",
        "Oma Dadel giet de laatste waterzak over haar heen. De karavaan zingt. Bobbeltje likt aan haar neus.",
        "Moeder Bron rimpelt. Ze wordt blauw. Dan roze. Dan zo helder dat je de vissen in haar buik ziet zwemmen.",
        "\"Dank je, ridder. Wat een vreemde droom. Iemand dronk mij leeg met een rietje van grauw.\""]),
    "toren": ("De Sterrendovende Toren", "Hertogin Ringeling", [
        "De toren is grauw. Het gras rond de voet is grauw. Zelfs de schaduw is grauwer dan een schaduw hoort te zijn.",
        "Bovenin, naast de Lege Lens, staat een dame met een parasol. Tien gouden ringen glanzen om tien tenen.",
        "\"Een ridder! Wat schattig. Je hebt mijn sterren weer aangestoken. Dat was heel... onbeleefd.\"",
        "Ze schopt één ring van haar teen. Hij klikt als een slot op de poort van de toren.",
        "\"Daar zijn er minstens twee voor nodig, ridder. Kom maar terug met vriendjes. Ik heb thee.\"",
        "En ze danst de trap op, naar boven, naar haar lens. Haar lach klinkt als tien belletjes."]),
    "ringeling": ("Opgevrolijkt!", "Hertogin Ringeling", [
        "Ringeling zakt op de vloer van haar toren. Haar ringen rollen over de stenen. Tien. Leeg.",
        "\"Mijn TENEN! Ze zijn zo... koud. En zo licht.\" Ze wiebelt ermee. Ze giechelt. Ze schrikt van haar eigen giechel.",
        "Je geeft haar een knuffel. Ze verstijft. Dan wordt ze roze. Eerst haar oren, dan haar wangen, dan haar tenen.",
        "\"Wat is dit? Het is warm. Het is van niemand gestolen. Het is gewoon... gekregen.\"",
        "Ze pakt de laatste ring, de grote, die van de Keizer, en legt hem in je poot. \"Hou jij die maar. Ik wil hem niet meer.\""]),
    "f4": ("De Keizer", "verliest een Vinger", [
        "Ergens ver in het oosten. Een troonzaal, grauw en stil. Een lange, magere schaduw op een veel te grote troon.",
        "Hofschrijver Krabbel schuifelt naar voren met een briefje. Er zit een hartje op. Zijn ganzenveer trilt.",
        "Krabbel: \"De Ringvinger... eh... komt niet. Ze schrijft: 'Beste Keizer. Ik stop. Ik heb sterren gezien. Groetjes, Ringeling.'\"",
        "Krabbel: \"En eh... 'Uw ring ligt bij de ridder.' Er staat nog een tekeningetje bij. Van een tong.\"",
        "Een lange grauwe vinger tikt op de leuning. Tik. Tik. Tik. Dan: \"Dan... kijk... ik... zelf.\"",
        "Buiten, hoog boven de wereld, draait het Oog langzaam. Weg van het Zijdeland. Naar het westen. Naar Hoogvads."]),
}
VISIOEN_TITEL = {n: ("Moeder Bron", ["een stem uit het water", "fluistert", "roept zacht", "zingt", "roept", "is wakker"][n]) for n in range(6)}

EN = {}   # (English comes at the end, all at once: en_us gets the same Dutch for now)


# ---------------------------------------------------------------------------------------------------------------------------
# building
# ---------------------------------------------------------------------------------------------------------------------------
_HUB_NPC = re.compile(r'^(kk_poort|bond_bord)_[a-z]+$')


def _java_ids():
    with open(os.path.join(JAVA, "Z6VerhaalPlekken.java"), encoding="utf-8") as f:
        src = f.read()
    npc = set(re.findall(r'\bnpc\("((?:z6|kk_poort|bond_bord)_[a-z_0-9]+)"', src))
    for lus, n in (("z6_vluchteling_", 3), ("z6_burger_", 6), ("z6_wacht_", 6)):
        npc.discard(lus)
        npc |= {f"{lus}{i}" for i in range(1, n + 1)}
    poi = set(re.findall(r'\bpoi\("(z6_[a-z_0-9]+)"', src))
    for lus, n in (("z6_tuin_", 3), ("z6_noria_", 3), ("z6_examen_", 4), ("z6_waterval_", 3), ("z6_dak_", 3)):
        poi.discard(lus)
        poi |= {f"{lus}{i}" for i in range(1, n + 1)}
    gebied = set(re.findall(r'GEBIED_DOEL\.put\("([a-z_0-9]+)"', src))
    return npc, poi, gebied


def teksten():
    t = dict(OVERIG)
    npc_ids, poi_ids, gebied_ids = _java_ids()
    for id, (naam, regels, waar) in NPCS.items():
        assert len(regels) == 3, id
        t[P + "naam." + id] = naam
        t[P + "plek.npc." + id] = waar
        for i, r in enumerate(regels, 1):
            t[P + f"praat.{id}.{i}"] = r
    assert set(NPCS) == npc_ids, f"NPC ids differ from Z6VerhaalPlekken: {set(NPCS) ^ npc_ids}"
    for id, x in PLEK_POI.items():
        t[P + "plek.poi." + id] = x
    assert set(PLEK_POI) == poi_ids, f"POI ids differ from Z6VerhaalPlekken: {set(PLEK_POI) ^ poi_ids}"
    for id, x in PLEK_GEBIED.items():
        t[P + "plek.gebied." + id] = x
        t["vadsrijk.kern.gebied." + id] = GEBIED_NAMEN[id]
    assert gebied_ids <= set(PLEK_GEBIED), "area without a plek text"
    for id, x in PLEK_KAMPVUUR.items():
        t[P + "plek.kampvuur." + id] = x
    for id, naam in UITKIJKEN.items():
        t["vadsrijk.wereld.uitkijk." + id] = naam
    for id, (naam, _tex) in QUEST_ITEMS.items():
        t[f"item.guhs.vr_z6_npc_{id}"] = naam
    for q in QUESTS:
        s = P + "q." + q["id"]
        t[s + ".titel"] = q["titel"]
        t[s + ".tekst"] = q["tekst"]
        t[s + ".bezig"] = q["bezig"]
        for i, d in enumerate(q["doelen"]):
            t[s + f".doel.{i}"] = d["tekst"]
        for i, (_w, x) in enumerate(q["aanbod"]):
            t[s + f".aanbod.{i}"] = x
        for i, (_w, x) in enumerate(q["klaar"]):
            t[s + f".klaar.{i}"] = x
        for npc, regels in q["praat"].items():
            for i, (_w, x) in enumerate(regels):
                t[s + f".praat.{npc}.{i}"] = x
    for id, (titel, intro, stappen, fout, klaar) in PUZZELS.items():
        t[Z + f"puzzel.{id}.titel"] = titel
        t[Z + f"puzzel.{id}.intro"] = intro
        t[Z + f"puzzel.{id}.fout"] = fout
        t[Z + f"puzzel.{id}.klaar"] = klaar
        for s, (vraag, opties, _goed) in enumerate(stappen):
            t[Z + f"puzzel.{id}.{s}.vraag"] = vraag
            for o, x in enumerate(opties):
                t[Z + f"puzzel.{id}.{s}.{o}"] = x
    for k, v in PUZZEL_TEKST.items():
        t[Z + "puzzel." + k] = v
    for n, regels in VISIOENEN.items():
        for i, r in enumerate(regels):
            t[Z + f"visioen.{n}.{i}"] = r
        t[Z + f"visioen.{n}.titel"], t[Z + f"visioen.{n}.onder"] = VISIOEN_TITEL[n]
    for id, (titel, onder, regels) in SCENES.items():
        t[Z + f"scene.{id}.titel"] = titel
        t[Z + f"scene.{id}.onder"] = onder
        for i, r in enumerate(regels):
            t[Z + f"scene.{id}.{i}"] = r
    for id, naam in KNUFFELS.items():
        t[Z + "knuffel." + id] = naam
    for id, (naam, tekst) in SCHATTEN.items():
        t[Z + "schat." + id] = tekst
        t[Z + "schat.naam." + id] = naam
    for id, tekst in TABLETTEN.items():
        t[Z + "tablet." + id] = tekst
    for id, tekst in VONDST_TEKST.items():
        t[Z + "vondst." + id] = tekst
    for id, (titel, tekst) in TIPS.items():
        t[P + "tip." + id] = tekst
        t[P + "tip." + id + ".titel"] = titel
    for id, (titel, tekst) in GIDS.items():
        t[P + "gids." + id + ".titel"] = titel
        t[P + "gids." + id + ".tekst"] = tekst
    for k, v in t.items():
        assert "%" not in v.replace("%s", ""), f"stray % in {k}"
    return t


_PLEK_SOORTEN = ("npc:", "poi:", "gebied:")


def quests_json():
    npc_ids, poi_ids, gebied_ids = _java_ids()
    andere = {"z5_havenmeester"}
    alle_npc = npc_ids | andere
    ids = [q["id"] for q in QUESTS]
    assert len(ids) == len(set(ids)), "duplicate quest id"
    assert all(i.startswith("z6_") for i in ids), "zone-6 quest ids start with z6_"
    uit = []
    for q in QUESTS:
        for wie in (q["gever"], q["inleveren"]):
            assert wie in alle_npc, f"{q['id']}: unknown NPC {wie}"
        for na in q["na"]:
            assert na in ids or not na.startswith("z6_"), f"{q['id']}: unknown previous quest {na}"
        for d in q["doelen"]:
            assert d["plek"].startswith(_PLEK_SOORTEN), f"{q['id']}: objective without a plek: {d['tekst']}"
            soort, _, x = d["plek"].partition(":")
            assert x in {"npc": alle_npc, "poi": poi_ids, "gebied": set(PLEK_GEBIED)}[soort], f"{q['id']}: unknown plek {d['plek']}"
            if d["doel"].startswith("poi:"):
                assert d["doel"][4:] in poi_ids, f"{q['id']}: unknown poi {d['doel']}"
            if d["soort"] == "praat":
                assert d["doel"] in alle_npc, f"{q['id']}: unknown NPC {d['doel']}"
            if d["doel"].startswith("z6_puzzel:"):
                assert d["doel"][10:] in PUZZELS, f"{q['id']}: unknown puzzle {d['doel']}"
            if d["doel"].startswith("z6_baken:"):
                assert d["doel"][9:] in BAKENS, f"{q['id']}: unknown beacon {d['doel']}"
            if d["doel"].startswith("teller:"):
                assert d["doel"][7:] in VONDST_TEKST, f"{q['id']}: unknown find kind {d['doel']}"
        for groep in (q["aanbod"], q["klaar"], *q["praat"].values()):
            for wie, _x in groep:
                assert wie in ("npc", "jij", "verteller") or wie in alle_npc, f"{q['id']}: unknown speaker {wie}"
        o = dict(id=q["id"], soort=q["soort"], gever=q["gever"], inleveren=q["inleveren"], lv=q["lv"], na=q["na"],
                 doelen=[dict(soort=d["soort"], doel=d["doel"], n=d["n"], bron=d["bron"], kans=d["kans"], plek=d["plek"])
                         for d in q["doelen"]],
                 beloning=q["beloning"], aanbod=[w for w, _x in q["aanbod"]], klaar=[w for w, _x in q["klaar"]],
                 praat={npc: [w for w, _x in r] for npc, r in q["praat"].items()}, actie_aanname=q["actie_aanname"],
                 actie_klaar=q["actie_klaar"], start_items=q["start_items"], auto=q["auto"])
        if q["klasse"]:
            o["klasse"] = q["klasse"]
        uit.append(o)
    hoofd = [q for q in QUESTS if q["soort"] == "hoofd"]
    assert [q["id"] for q in hoofd] == HQ, "main quests in story order"
    assert sum(q["soort"] == "zij" for q in QUESTS) == 15, "15 side quests"
    return {"_": "generated by tools/features/vadsrijk_u_z6verhaal.py; do not edit", "quests": uit}


def puzzels_json():
    uit = {}
    for id, (_titel, _intro, stappen, _fout, _klaar) in PUZZELS.items():
        for s, (_v, opties, goed) in enumerate(stappen):
            assert 0 <= goed < len(opties) and len(set(opties)) == len(opties), (id, s)
        uit[id] = {"stappen": [{"opties": len(o), "goed": g} for _v, o, g in stappen]}
    return {"_": "generated by tools/features/vadsrijk_u_z6verhaal.py; do not edit", "puzzels": uit,
            "bakens": {b: {"puzzel": v[0], "poi": v[2]} for b, v in BAKENS.items()}}


def advancements():
    namen = ([f"vr_{q['id']}" for q in QUESTS] + ["vr_z6_knuffels_alle", "vr_z6_grauwe_brief", "vr_z6_uitkijken_alle", "vr_z6_bakens_alle",
                                                   "vr_z6_ringeling"]
             + [f"vr_z6_schat_{s}" for s in SCHATTEN])
    return {f"advancement/quest/{n}.json": {"criteria": {"done": {"trigger": "minecraft:impossible"}}} for n in namen}


def _assets(w, a):
    for id, (_naam, tex) in QUEST_ITEMS.items():
        w(os.path.join(a, "items", f"vr_z6_npc_{id}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_z6_npc_{id}"}})
        w(os.path.join(a, "models", "item", f"vr_z6_npc_{id}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})


def build(h):
    h.w(f"{h.D}/vadsrijk/npc/quests_z6.json", quests_json())
    h.w(f"{h.D}/vadsrijk/z6verhaal/puzzels.json", puzzels_json())
    for pad, obj in advancements().items():
        h.w(f"{h.D}/{pad}", obj)
    _assets(h.w, h.A)
    for k, nl in teksten().items():
        h.lang(k, EN.get(k, nl), nl)


# ---------------------------------------------------------------------------------------------------------------------------
# FTB Quests: zone 6 joins the chapter "Het Vadsrijk" with its own sections. Nothing is locked.
# ---------------------------------------------------------------------------------------------------------------------------
FTB_CHAPTER = "guhs_vadsrijk"
FTB_PORTRAIT = "guh:golden"
FTB_SECTIES = [
    ("vr_z6_verhaal", "Het Zijdeland (zone 6)", "guh:golden", [f"vr_{q}" for q in HQ]),
    ("vr_z6_land", "Het Zijdeland helpen", "guh:mint", [f"vr_{q['id']}" for q in QUESTS if q["soort"] == "zij"]),
    ("vr_z6_geheimen", "Geheimen van het Zijdeland", "guh:snow", ["vr_z6_knuffels_alle", "vr_z6_grauwe_brief", "vr_z6_uitkijken_alle",
                                                                 "vr_z6_bakens_alle", "vr_z6_ringeling"] + [f"vr_z6_schat_{s}" for s in SCHATTEN]),
]


def ftb(fq):
    q, adv = fq.q, fq.adv
    K = "guhs:kaas_knabbels"
    for d in QUESTS:
        tekst = d["tekst"].replace("Zijdeland", "&dZijdeland&r")
        icon = {"hoofd": "minecraft:writable_book"}.get(d["soort"], "minecraft:paper")
        vorm = "gear" if d["soort"] == "hoofd" else None
        q(f"vr_{d['id']}", d["titel"], tekst + " &7(Vinkt zichzelf af als je hem in het Vadsrijk inlevert.)", icon,
          [fq.met(adv(f"vr_{d['id']}"), d["titel"])], rewards=((K, 8 if d["soort"] == "zij" else 16),), shape=vorm,
          xp=100 if d["soort"] == "hoofd" else 50)
    q("vr_z6_knuffels_alle", "Acht knuffels in het zand", "Vind alle &e8 knuffels&r die in het Zijdeland verstopt zitten.", "guhs:knuffel_golden",
      [fq.met(adv("vr_z6_knuffels_alle"), "8 knuffels")], rewards=((K, 16),), shape="hexagon")
    q("vr_z6_grauwe_brief", "Aan de Ringvinger", "Vind de &8Grauwe brief&r in het Mika-kamp op de Zoutvlakte.", "minecraft:paper",
      [fq.met(adv("vr_z6_grauwe_brief"), "Grauwe brief")], rewards=((K, 8),))
    q("vr_z6_uitkijken_alle", "Hoog boven het zand", "Ontdek alle &e4 uitkijkpunten&r van het Zijdeland.", "minecraft:spyglass",
      [fq.met(adv("vr_z6_uitkijken_alle"), "4 uitkijkpunten")], rewards=((K, 16),), shape="hexagon")
    q("vr_z6_bakens_alle", "Vijf sterren", "Steek alle &e5 sterrenbakens&r van het Zijdeland weer aan.", "minecraft:beacon",
      [fq.met(adv("vr_z6_bakens_alle"), "5 bakens")], rewards=((K, 16),), shape="hexagon")
    q("vr_z6_ringeling", "Tien blote tenen", "Vrolijk &dHertogin Ringeling&r op, boven in De Sterrendovende Toren (raid).", "minecraft:gold_nugget",
      [fq.met(adv("vr_z6_ringeling"), "Ringeling")], rewards=((K, 32),), shape="hexagon")
    for s, (naam, _t) in SCHATTEN.items():
        q(f"vr_z6_schat_{s}", naam, "Een verborgen schat van het Zijdeland. Hij glinstert goudgeel als je dichtbij bent.",
          "minecraft:chest", [fq.met(adv(f"vr_z6_schat_{s}"), naam)], rewards=((K, 8),))


# ---------------------------------------------------------------------------------------------------------------------------
# standalone
# ---------------------------------------------------------------------------------------------------------------------------
def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _mijn_sleutel(k):
    """The lang keys this generator owns (so a standalone run can drop the ones it no longer writes)."""
    return (k.startswith(Z) or k.startswith("item.guhs.vr_z6_npc_") or (k.startswith(P) and ".z6_" in k)
            or k.startswith("vadsrijk.wereld.uitkijk.z6_") or k.startswith("vadsrijk.kern.gebied.z6_")
            or any(k.startswith(P + s) and k.endswith(n) for s in ("naam.", "plek.npc.") for n in ("kk_poort_rozenkloof", "kk_poort_saffraanpoort",
                                                                                                   "bond_bord_rozenkloof", "bond_bord_saffraanpoort"))
            or re.match(r'^vadsrijk\.npc\.praat\.(kk_poort|bond_bord)_(rozenkloof|saffraanpoort)\.\d$', k) is not None)


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "vadsrijk", "npc", "quests_z6.json"), quests_json())
    _schrijf(os.path.join(d, "vadsrijk", "z6verhaal", "puzzels.json"), puzzels_json())
    for pad, obj in advancements().items():
        _schrijf(os.path.join(d, *pad.split("/")), obj)
    _assets(_schrijf, os.path.join(a, "assets", "guhs"))
    t = teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for k in [k for k in data if _mijn_sleutel(k) and k not in t]:
            del data[k]
        for k, v in t.items():
            if k in data and not _mijn_sleutel(k) and data[k] != v:
                continue   # (never overwrite another slice's existing text)
            data[k] = EN.get(k, v) if taal == "en_us" else v
        _schrijf(p, data)
    print(f"vadsrijk_u_z6verhaal: {len(QUESTS)} quests ({sum(q['soort'] == 'hoofd' for q in QUESTS)} main, "
          f"{sum(q['soort'] == 'zij' for q in QUESTS)} side), {len(NPCS)} NPCs, {len(PUZZELS)} puzzles, {len(t)} texts")


if __name__ == "__main__":
    _direct()
