"""
1.10 "Het Vadsrijk", zone 3 "De Schemerwortel" (Z3_BRIEF.md), slice npc. Java: feature/vadsrijk/zone3/npc.

Zone 3 uses the SAME quest system as zones 1 and 2 (feature/vadsrijk/npc). This generator only adds zone-3 data:
  - the NPC guhs of Honingtak and the zone: names, idle lines and WHERE they stand (vadsrijk.npc.plek.npc.<id>, for the tracker);
  - the zone-3 quests -> data/guhs/vadsrijk/npc/quests_z3.json (read by Quests.java after quests_z2.json) + their texts;
  - every objective names WHO, WHERE and WHAT, and carries an explicit "plek" (npc:/poi:/gebied:/baas:) for the tracker + beam;
  - quest items, hidden FTB advancements guhs:quest/vr_z3_*, the FTB sections (ftb()), Gids pages, tips, scene texts;
  - the special pet Vahoegje (a pink recolour of the dragon texture + a tiny animation set) and the zone-3 Verkenningsboek texts.
Ids: NPCs and quests "z3_*", items "vr_z3_npc_*". All texts Dutch; en_us gets the same Dutch texts.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z3_npc.py
"""
import json
import os
import re

P = "vadsrijk.npc."          # (the shared quest system's keys: naam/praat/q/plek/gids/tip/adv...)
Z = "vadsrijk.z3npc."        # (zone-3 own texts: scenes, secrets, roles)
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone3", "npc")

_HT = "in Honingtak"
_PL = "op het Bloesemplein van Honingtak"
# ---------------------------------------------------------------------------------------------------------------------------
# NPCs: name, 3 idle lines, WHERE (a full phrase for the tracker: vadsrijk.npc.plek.npc.<id>). Spots + clothes: Z3Npcs.java /
# Z3NpcPlekken.java.
# ---------------------------------------------------------------------------------------------------------------------------
NPCS = {
    # --- Honingtak: travel + story -----------------------------------------------------------------------------------------------
    "z3_kapitein": ("Kapitein Zoemzeil", [
        "Mijn ballon is gestreept. Net als mijn trui. Net als mijn sokken. Ik hou van strepen.",
        "Terug naar De Haven of naar Kandijkom? Pak de reiskaart maar, ik vlieg overal heen. Behalve tegen de boom aan.",
        "Ik heb ooit een bij als stuurguh gehad. Hij wilde alleen maar naar bloemen. Heel gezellige vluchten waren dat."],
        "aan de luchtschipmast aan het eind van de Imkerstraat, de loopplank in het noordwesten van Honingtak, waar het luchtschip aanmeert"),
    "z3_raatje": ("Imkermoeder Raatje", [
        "Welkom in Honingtak, lieverd. Een stadje op een tak, en de tak aan een boom zo groot als een berg.",
        "Ik ben burgemeester, imker en oma van iedereen hier. Vooral oma.",
        "Honing, bloesem en een warme deken. Meer heeft een guh niet nodig. Nou ja, knuffels ook."],
        "op het Bloesemplein (het Honingplein) aan de zuidkant van het ringdek van Honingtak, bij het knuffelbeeld van de imkerguh"),
    "z3_wasja": ("Ordemeester Wasja", [
        "Dit is de Nestkapel, het filiaal van De Knuffelorde in de boom. Hier wissel je van klasse.",
        "De Orde heeft hier vroeger een eiersokkel neergezet. Leeg. Niemand weet meer waarvoor.",
        "Bijen dansen om de weg te wijzen. Ik dans om warm te blijven. Iedereen heeft zo zijn reden."],
        "in de Nestkapel, de grote raatcel onder de noordkant van het ringdek van Honingtak (neem de kapellift aan de noordkant omlaag)"),
    "z3_veertje": ("Stalmeester Veertje", [
        "Een Pluimguh borstelen is het fijnste werk van de wereld. Na Pluimguhs voeren. En Pluimguhs knuffelen.",
        "Rijden doe je met je knieën, je hart en een zakje Honingbrokjes.",
        "Hoor je dat getrappel? Dat is de oefenbaan. Of mijn maag. Het is bijna lunchtijd."],
        "in de Pluimstal op de grond aan de noordkant van de boom, bij het begin van het rijpad (neem de lift in de binnenhof van de stam naar de Schorsstadvloer en ga door de noordpoort naar buiten)"),
    "z3_cafe": ("Cafébaas Zoet", [
        "Welkom in De Zoete Raat! Honingthee, honingkoek en honingijs. En voor de durfals: honing met honing.",
        "'s Avonds wordt er hier gedanst. Op de tafels. Niet aan mijn vrouw vertellen.",
        "Een kopje honingthee? Dat warmt je oortjes op."],
        "in café De Zoete Raat, de raatcel onder de westkant van het ringdek van Honingtak (de cafélift gaat omlaag)"),
    "z3_jager": ("Jagersguh Raap", [
        "Op het premiebord hangen elke dag nieuwe klusjes. Monsters, schimmel, bezorgen. Pak maar wat je leuk vindt.",
        "Ik jaag niet op guhs, hoor. Alleen op wat de boom ziek maakt.",
        "Een premie per dag houdt de schimmel weg. En de verveling."],
        "in de Jagershut aan de noordoostkant van het ringdek van Honingtak, bij het premiebord"),
    # --- shops ---------------------------------------------------------------------------------------------------------------------
    "z3_smid": ("Smidguh Hamerhoning", [
        "Mijn vuur brandt op honingwas. Ruikt lekker en gloeit roze.",
        "Sterren erop, verpluisd eraf. Hier wordt alles weer stevig.",
        "Ik smeed alleen niet op zondag. Dan bak ik wafels."],
        "in de smidse aan de zuidwestkant van het ringdek van Honingtak, vlak bij het Bloesemplein"),
    "z3_juwelier": ("Juweliersguh Glinsterhoning", [
        "Amberroze, honingkristal, wortelkristal... de boom zit vol mooie steentjes.",
        "Een sokje in je spullen? Ik zet er zo een kristal in. Klik, klaar.",
        "Hars is het geheugen van een boom. Daarom glinstert het zo."],
        "in de juwelierszaak aan de westkant van het ringdek van Honingtak"),
    "z3_winkel": ("Winkelguh Bijenwas", [
        "Drankjes, verband en honingdropjes! Alles voor onderweg.",
        "Een drankje met een honingrandje werkt twee keer zo goed. Zegt mijn oma. Mijn oma is Raatje.",
        "Zoem zoem, kassa!"],
        "in de drankjeswinkel aan de westkant van het ringdek van Honingtak"),
    "z3_wapens": ("Wapenhandelaar Angeltje", [
        "Takkenknotsen, liaanbogen, bloesemstaven. Alles geplukt uit de boom. Met toestemming!",
        "Puntig als een bijenangel. Maar dan lief.",
        "Kies je wapen met je hart. En met je klasse."],
        "in de uitrustingswinkel aan de noordwestkant van het ringdek van Honingtak"),
    "z3_kleermaker": ("Kleermaker Sluier", [
        "Een imkerhoed met een sluier? Staat iedereen. Zelfs Pluimguhs.",
        "Ik naai alles met zijdedraad van de rupsen hier. Ze vinden het leuk om te helpen.",
        "Strepen maken dun. Daarom draagt de hele stad strepen. Grapje. Het is gewoon gezellig."],
        "in de winkel aan de noordkant van het ringdek van Honingtak"),
    "z3_markt": ("Marktguh Zoemsel", [
        "Kopen, verkopen, ruilen! De zoemende markt is altijd open.",
        "Vandaag in de aanbieding: niks. Morgen ook. Kijk zelf maar tussen de kraampjes.",
        "Hier ruil je alles met andere avonturiers. Eerlijk is eerlijk, zegt de bij."],
        "op de zoemende markt op het Bloesemplein, aan de zuidkant van Honingtak"),
    "z3_stalguh": ("Stalguh Hooibaal", [
        "Zadels, kleurtjes en voer. Kijk maar wat je Pluimguh mooi vindt.",
        "Ik ruim de stal op. Elke dag. Pluimguhs verliezen veel veertjes. Heel veel.",
        "Een Pluimguh die niest, laat een wolk dons achter. Ik hou van dat werk."],
        "in de Pluimstal op de grond aan de noordkant van de boom, bij de voerzolder"),
    "z3_liftguh": ("Liftguh Katrolletje", [
        "De Honinglift gaat helemaal naar beneden, naar Mosvoet. Twee kettingen, één honingraatcabine, veel moed.",
        "Niet naar beneden kijken. Of juist wel. Het uitzicht is prachtig.",
        "Ik draai de slinger al dertig jaar. Mijn linkerarm is heel sterk."],
        "bij het bovenstation van de Honinglift, aan het eind van de Lifttak in het zuidoosten van Honingtak"),
    # --- citizens ----------------------------------------------------------------------------------------------------------------
    "z3_opa_bast": ("Opa Bast", [
        "Toen ik jong was, klom ik elke dag de binnentrap op naar het nest. Nu klim ik elke dag naar mijn stoel.",
        "Vroeger zong er een draak in de kruin. Heel zacht. Je hoorde het alleen als je stil was.",
        "Mijn bijen heten allemaal Bea. Dat is makkelijk roepen."],
        "bij het knuffelbeeld op het Bloesemplein van Honingtak"),
    "z3_zoemer": ("Kleine Zoemer", [
        "Ik heb vleugeltjes! Ze doen niks, maar ze zijn wél heel mooi.",
        "Zoem zoem! Ik ben een bij! Een grote bij! Met oortjes!",
        "Weet jij waar de bijenkoningin woont? Ik mag er nooit naartoe."],
        _PL),
    "z3_honingbij": ("Honingbij Bea", [
        "Ik ben geen bij, hoor. Ik heb gewoon voelsprietjes. Op mijn hoofd. Van stof.",
        "De bijen van Honingtak zijn de liefste van het hele rijk. Ze steken nooit. Ze knuffelen.",
        "Wil je een bijendans zien? Kijk: rondje, wiebel, rondje. Dat betekent: daar is taart."],
        _PL),
    "z3_raatwever": ("Raatwever Wasje", [
        "Ik bouw de raten voor de huisjes. Zeshoekjes, de hele dag. Ik droom zelfs in zeshoekjes.",
        "Er hangt een imkerpak aan de waslijn dat niemand kent. Raar, hè?",
        "Een zeshoek is de sterkste vorm. En de gezelligste."],
        "bij de huisjes op de takken van Honingtak, ten westen van het Bloesemplein"),
    "z3_bloesem": ("Bloesemguh Roosje", [
        "Het bloesemboompje op het plein groeit uit het dek zelf. Is dat niet prachtig?",
        "Als het waait, regent het hier bloemblaadjes. Dan dans ik.",
        "Ruik eens. Dat is lente. Elke dag lente."],
        "bij het bloesemboompje " + _PL),
    "z3_druppel": ("Danseres Druppel", [
        "Vanavond dansen we in De Zoete Raat! Kom je ook?",
        "Een honingdruppel valt heel langzaam. Zo dans ik ook. Heel langzaam. En plakkerig.",
        "Ik heb ooit met een Pluimguh gedanst. Hij trapte op mijn teen. Allebei mijn tenen."],
        "in café De Zoete Raat, de raatcel onder de westkant van het ringdek van Honingtak"),
    "z3_lampion": ("Lampionguh Gloed", [
        "'s Avonds steek ik alle lampions aan. Allemaal tweehonderdzevenentwintig.",
        "Als de lichtkevers komen, hoef ik bijna niks meer te doen.",
        "Licht houdt de grauwigheid weg. Dat weet iedereen hier."],
        "bij de huisjes op de takken van Honingtak, ten westen van het Bloesemplein"),
    "z3_imker_1": ("Imker Zoemzang", [
        "Mijn bijen zingen 's ochtends. Ik zing mee. De buren vinden het... bijzonder.",
        "Honing oogsten is een kwestie van geduld. En een sluier. Vooral een sluier.",
        "Hoor je dat? Zo klinkt een tevreden korf."],
        "bij de korfhuisjes aan de zuidoostkant van het ringdek van Honingtak"),
    "z3_imker_2": ("Imker Pollie", [
        "Sinds de Mika-zeppelins komen, zingt de bijenkoningin niet meer. Ik maak me zorgen.",
        "Grauwe honing! In mijn korf! Wie doet nou zoiets?",
        "Een imker zonder bijen is als een boom zonder bladeren. Heel kaal."],
        "bij de korfhuisjes aan de noordkant van het ringdek van Honingtak"),
    "z3_imker_3": ("Imker Honingdauw", [
        "Ik heb ooit een honingraat zo groot als een deur gevonden. Ik woon er nu in.",
        "Wist je dat bijen kunnen tellen? Tot vier. Dan beginnen ze opnieuw.",
        "Een goede imker heeft altijd een snoepje in zijn zak. Voor de bijen. En voor zichzelf."],
        "bij de korfhuisjes aan de noordwestkant van het ringdek van Honingtak"),
    "z3_kuikenhoeder": ("Kuikenhoeder Pip", [
        "Pluimkuikens zijn zó schattig. En zó snel weg. Waar zijn ze nu weer?",
        "Een Pluimkuiken volgt alles wat beweegt. Vooral rijdieren.",
        "Als ze groot zijn, kun je erop rijden. Nu passen ze in een theekopje."],
        "bij de oefenbaan van de Pluimstal, op de grond aan de noordkant van de boom"),
    "z3_kind_1": ("Kleine Raatje", [
        "Ik heet net als oma! Maar dan klein.",
        "Ik ga later Pluimguh-rijder worden. Of draak. Draak mag ook.",
        "Heb jij een rijdier? Mag ik aaien?"],
        _PL),
    "z3_kind_2": ("Kleine Wortel", [
        "Ik kom uit Mosvoet! Daar is alles van wortels. Hier is alles van honing. Ik weet niet wat lekkerder is.",
        "Mijn vleugeltjes zijn van papier. Maar sssst.",
        "Ik ben niet bang voor de Mika's. Een beetje maar."],
        _PL),
    "z3_postguh": ("Postguh Briefje", [
        "Post bezorgen in een boom is lastig. Alles is boven, onder of erachter.",
        "Ik heb een brief voor iemand die 'De Pink' heet. Geen idee wie dat is. Hij ligt nog in mijn tas.",
        "Zoem zoem, post!"],
        _PL),
    # --- out in the zone -------------------------------------------------------------------------------------------------------------
    "z3_bladwachter": ("Kruinwachter Blad", [
        "De Kruinzee is mooi, maar sinds de zeppelins is ze grauw op plekken.",
        "Steek mijn kampvuur aan. Hier word je wakker als het misgaat.",
        "Helemaal bovenop de boom, boven de drie kruinen, ligt iets heel groots. Een nest."],
        "bij het kampvuur aan de oostkant van kruin 1, de onderste kruinverdieping (de Mika-zeppelinsteiger ligt in het noordoosten)"),
    "z3_lusje": ("Kaartenmaakster Lusje", [
        "Kaarten tekenen van een boom is moeilijk. Alles loopt rond. En omhoog. En omlaag.",
        "Zoemrust ligt op de Bijentak. Hoog, wiebelig en vol bijen. Je went eraan. Na een week of drie.",
        "Mijn thee is weer koud. Komt door de schrik. Of door mij. Ik vergeet hem altijd."],
        "in Imkerdorp Zoemrust op de Bijentak, de reuzentak in het zuidoosten (rijd het rijpad op tot boven Honingtak en rijd de tak op)"),
    "z3_herbergier": ("Herbergier Dauw", [
        "Welkom in de herberg van Zoemrust! Bedden van mos, soep van bloesem.",
        "Bij ons wiebelt de vloer. Dat hoort zo. We wonen op een tak.",
        "Drankjes en verband heb ik ook. Een imkerdorp op een tak moet alles in huis hebben."],
        "in de herberg van Imkerdorp Zoemrust, op de Bijentak in het zuidoosten"),
    "z3_knoest": ("Oude Knoest", [
        "...Ik ben het laatste lichtje van het boomvolk. De anderen slapen in het hout.",
        "Wij bouwden deze stad in de stam. Huisjes in de wand, langs de trap, helemaal rond. Lang geleden.",
        "Licht is warmte. Warmte is leven. Zo simpel is dat. Vroeger."],
        "in een huisje in de wand van de holle stam, halverwege de binnentrap van de Schorsstad (de trapdeur aan het Bloesemplein)"),
    "z3_wortelien": ("Burgemeester Wortelien", [
        "Welkom in Mosvoet! We wonen onderaan de boom, tussen de wortels. Lekker knus, en altijd koel.",
        "Ons waterrad draait op sap. Het ruikt naar ahornsiroop. Iedereen hier heeft altijd honger.",
        "De wortels rotten. Dat merken we aan alles. Zelfs de soep smaakt grauw."],
        "in Mosvoet, het gehucht op de grond bij het onderstation van de Honinglift, in het zuidoosten (neem de Honinglift omlaag)"),
    "z3_molenaar": ("Molenaar Rad", [
        "Mijn waterrad draait op boomsap. Langzaam, maar het draait.",
        "Als de schimmel komt, loopt het vast. Dan duw ik. Hard.",
        "Een rondje draaien is goed voor iedereen. Ook voor een rad."],
        "bij het waterrad van Mosvoet, op de grond in het zuidoosten (bij de Honinglift)"),
    "z3_theeguh": ("Theeguh Kwak", [
        "Honingthee met moerasbloemen. Daar krijg je snelle pootjes van!",
        "Ik ben een kikkerguh. Ik woon graag bij het water. En bij de thee.",
        "Kwak. Dat betekent 'proost'."],
        "in het theehuis van Mosvoet, op de grond in het zuidoosten (bij de Honinglift)"),
    "z3_gloei": ("Wortelwacht Gloei", [
        "Hier beneden is het donker. Blijf in het licht, dan doen de monsters minder pijn.",
        "Hoor je dat? Die klop? Dat is het hart van de boom.",
        "Mijn lampje is mijn beste vriend. Na mijn moeder."],
        "bij het kampvuur in de Hartslag-zaal, de wortelhal onder de stam (door de Wortelpoort in het oosten en de Wortelgang omlaag)"),
    "z3_nestwachter": ("De Oude Nestwachter", [
        "...Ik hield het ei warm. Heel lang. Ik was het bijna vergeten, waarom.",
        "Het boomvolk droeg het ei naar beneden. Ik bleef. Iemand moest blijven.",
        "Nu ben ik weer roze. Dat voelt... fijn. Dank je."],
        "bij de wortelmand van het ei, midden in de wortelhal onder de stam (na het Hart van de Boom)"),
    # --- (P-VERHUIS) visible life: the other beekeeper villages, travellers at the camps, guhs on the bridges ----------------------
    "z3_imker_raatweide": ("Imker Wasdruppel", [
        "Welkom in Raatweide! Hier wonen we tussen de raten. Letterlijk. Mijn bed is zeshoekig.",
        "Onze bijen vliegen elke ochtend naar de kruin en elke avond terug. Ik zwaai ze altijd uit.",
        "Als de wind waait, wiebelt de hele tak. De bijen vinden het een feestje."],
        "in Imkerdorp Raatweide, op de Zuidwesttak (rijd het rijpad op tot boven Honingtak en rijd de tak op)"),
    "z3_imker_molenhoek": ("Molenimker Wiek", [
        "In Molenhoek draaien de molentjes op de takwind. Ze malen bloesemmeel voor de honingkoek.",
        "Een molentje dat stilstaat, betekent dat de wind slaapt. Dan slaap ik ook. Heel eerlijk.",
        "Kijk uit voor de wespen op de takken. Ze zijn grauw geworden. Wij niet, gelukkig."],
        "in Imkerdorp Molenhoek, op de Molentak in het noorden (rijd het rijpad op tot boven Honingtak en rijd de tak op)"),
    "z3_reiziger_1": ("Reiziger Rugzak", [
        "Ik loop al drie dagen rond deze boom. Ik ben nog niet eens halverwege. Denk ik.",
        "Het rijpad om de stam? Prachtig. Maar ik heb geen Pluimguh. Dus ik loop. Langzaam.",
        "Een kampvuur, een kop thee, en een boom zo groot als een berg. Meer wil een reiziger niet."],
        "bij het kampvuur aan de zuidkant van de Wortelvelden, op de grond rond de boom"),
    "z3_reiziger_2": ("Reizigster Kompas", [
        "Ik teken waar de schimmelketels staan. Voor Lusje. Ze durft zelf niet. Hoogtevrees, zegt ze. Op de grond.",
        "De Mika's daar in het westen ruiken naar oude sokken. En naar kaas. Vooral naar kaas.",
        "Mijn kompas wijst altijd naar de boom. Of het kompas is kapot, of de boom is heel belangrijk."],
        "bij het kampvuur in het noordwesten van de Wortelvelden, op de grond rond de boom"),
    "z3_reiziger_3": ("Reiziger Zwerfsok", [
        "Zie je die kabelbrug daarboven? Daar gaan de Mika's naar hun werf. Ik blijf lekker hier.",
        "Ik was op weg naar Hoogvads. Toen zag ik deze boom. Nu ben ik hier al een maand.",
        "Kampvuur aan, schoenen uit, tenen warm. Zo hoort reizen."],
        "bij het kampvuur in het noordoosten van de Wortelvelden, op de grond rond de boom"),
    "z3_reiziger_4": ("Reizigster Wolkje", [
        "Ik ben met het rijpad helemaal tot hier boven gereden. Mijn Pluimguh slaapt nu. Ik ook bijna.",
        "Vanaf kruin 1 zie je de werf op haar eigen eiland. Met verrekijker zie je zelfs de kaas.",
        "De windkolommen gaan alleen omhoog. Naar beneden zweef je. Dat is de afspraak in deze boom."],
        "bij het kampvuur aan de oostkant van kruin 1, de onderste kruinverdieping"),
    "z3_brug_1": ("Brugguh Wiebel", [
        "Deze brug wiebelt. Ik ook. We zijn een goed team.",
        "Kijk niet naar beneden. Ik doe het wel. Het is heel ver. Oké, nu ben ik duizelig.",
        "De bijen vliegen onder de brug door. Ze zwaaien. Denk ik."],
        "op de touwbrug naar Imkerdorp Zoemrust, op de Bijentak"),
    "z3_brug_2": ("Brugguh Touwtje", [
        "Ik tel de planken. Honderdtwaalf. Gisteren waren het er honderdelf. Iemand bouwt stiekem bij.",
        "Ik woon niet op de brug, hoor. Ik sta er alleen heel graag.",
        "Raatweide is daar. Volg de lampions."],
        "op de touwbrug naar Imkerdorp Raatweide, op de Zuidwesttak"),
    "z3_brug_3": ("Brugguh Molenwiek", [
        "Hoor je de molentjes? Klik klak klik. Ik vind het muziek.",
        "Molenhoek ligt verderop op de tak. Langzaam lopen, de brug is kriebelig.",
        "Ik ben de brugwachter. Ik bewaak niks, maar ik zwaai wel heel goed."],
        "op de touwbrug naar Imkerdorp Molenhoek, op de Molentak"),
    "z3_brug_4": ("Plankguh Raatje Twee", [
        "Ik heet ook Raatje! Er zijn hier zeven Raatjes. Het is erg verwarrend.",
        "Dit is de loopplank naar de Honinglift. Daarbeneden ligt Mosvoet.",
        "Ik kijk elke dag naar de lift. Op en neer. Op en neer. Heel ontspannend."],
        "op de loopplank naar de Honinglift, in het zuidoosten van Honingtak"),
    "z3_brug_5": ("Plankguh Hoepla", [
        "Deze loopplank gaat naar het luchtschip. Hou je hoed vast!",
        "Ik heb ooit een honingkoek laten vallen. Hij is nooit aangekomen. Zo hoog is het.",
        "Hoepla! Dat zeg ik bij elke stap. Dan val je niet."],
        "op de loopplank in het noordwesten van Honingtak, op weg naar de luchtschipmast"),
    "z3_brug_6": ("Kabelguh Spanning", [
        "Deze kabelbrug gaat naar de Kaaszeppelinwerf. Ik ga er niet naartoe. Ik kijk alleen.",
        "Als de Mika's hun kanonnen testen, trilt de hele brug. Dan houd ik mijn oortjes vast.",
        "Honderdveertig blokken lang. Ik heb ze allemaal geteld. Twee keer."],
        "midden op de kabelbrug tussen Honingtak en de Kaaszeppelinwerf, in het noordoosten"),
}
# the Honingwacht (Z3NpcPlekken z3_wacht_*): name, where; they share their lines (like the Kristalwacht of zone 2)
WACHTEN = {
    "z3_wacht_plein_1": ("Zoemwacht", "aan de westkant van het Bloesemplein van Honingtak"),
    "z3_wacht_plein_2": ("Raatwacht", "aan de oostkant van het Bloesemplein van Honingtak"),
    "z3_wacht_kruin_1": ("Bloesemwacht", "bij het kampvuur op kruin 2, de middelste kruinverdieping (noordwest)"),
    "z3_wacht_kruin_2": ("Kroonwacht", "bij het kampvuur op kruin 3, vlak onder het oude nest"),
    "z3_wacht_tak_1": ("Takwacht", "bij het kampvuur op de Westtak, vlak bij de stam"),
    "z3_wacht_tak_2": ("Liaanwacht", "bij het kampvuur op de Molentak, in het noorden"),
    "z3_wacht_spiraal": ("Schorswacht", "bij het kampvuur halverwege de binnentrap van de Schorsstad, binnen in de stam"),
    "z3_wacht_wortel_1": ("Keverwacht", "bij het kampvuur op de Wortelweg in het zuidoosten van de Wortelvelden"),
    "z3_wacht_wortel_2": ("Rotwacht", "bij het kampvuur op de Wortelweg in het zuidwesten van de Wortelvelden"),
    "z3_wacht_wortel_3": ("Paddenwacht", "bij het kampvuur op de Wortelweg in het noordwesten van de Wortelvelden"),
    "z3_wacht_wortel_4": ("Poortwacht", "bij het kampvuur op de Wortelweg in het noordoosten van de Wortelvelden"),
    "z3_wacht_gewelf": ("Gewelfwacht", "bij het kampvuur bij het Wortelgewelf, de grote ruïne in het noorden van de Wortelvelden"),
}
WACHT_REGELS = [
    "Steek het kampvuur aan: hier word je wakker als je flauwvalt. Warm, veilig, en je verliest niks.",
    "De Honingwacht bewaakt de routes door de boom. Met speren van riet en heel veel honingkoek.",
    "Daarbuiten zijn echte monsters, avonturier. Geen guhs. Wees dapper, en kom terug voor het donker."]
for _id, (_naam, _waar) in WACHTEN.items():
    NPCS[_id] = ("Honingwacht " + _naam, WACHT_REGELS, _waar)

# ---------------------------------------------------------------------------------------------------------------------------
# Where: POIs / areas / campfires / boss arenas (vadsrijk.npc.plek.<kind>.<id>)
# ---------------------------------------------------------------------------------------------------------------------------
PLEK_POI = {
    "z3_plein": "midden op het Bloesemplein, aan de zuidkant van het ringdek van Honingtak",
    "z3_dok": "aan de luchtschipmast aan het eind van de Imkerstraat, in het noordwesten van Honingtak",
    "z3_pluimstal": "in de Pluimstal, op de grond aan de noordkant van de boom",
    "z3_zweefplatform": "op het zweefplatform aan de zuidrand van kruin 1, boven het Bloesemplein",
    "z3_rijpad_start": "bij het begin van het rijpad, op de grond aan de noordkant van de stam (vlak bij de Pluimstal)",
    "z3_rijpad_eind": "aan het eind van het rijpad, bovenop kruin 1 (rijd vier rondjes om de stam omhoog, door de bladtunnel)",
    "z3_honingdeur": "bij de trapdeur in de stamwand aan het Bloesemplein van Honingtak",
    "z3_schorsstad_vloer": "op de vloer van de Schorsstad, onderaan de binnentrap in de holle stam",
    "z3_pelstrap": "bij de trap omlaag naar de Pelszaal, op de vloer van de Schorsstad",
    "z3_kabelbrug": "bij het begin van de kabelbrug naar de Kaaszeppelinwerf, aan de noordoostrand van Honingtak",
    "z3_nestkapel_balkon": "in de Nestkapel, de raatcel onder de noordkant van het ringdek (de kapellift omlaag)",
    "z3_raattrossen": "in de Raattrossen, de raatcel onder de oostkant van het ringdek van Honingtak (neem de raattroslift omlaag)",
    "z3_vlek_1": "bij de eerste grauwe plek, aan de zuidoostkant van kruin 1",
    "z3_vlek_2": "bij de tweede grauwe plek, op kruin 2 aan de zuidwestkant (de windkolom of de binnentrap omhoog)",
    "z3_vlek_3": "bij de derde grauwe plek, aan de noordkant van kruin 1",
    "z3_zeppelinsteiger": "bij de Mika-zeppelinsteiger aan de noordoostrand van kruin 1 (de grauwe spuitinstallatie)",
    "z3_kruintop": "bij het oude nest helemaal bovenop de boom, boven kruin 3 (de windkolommen of de binnentrap omhoog)",
    "z3_nestsplinter": "tussen de takken van het oude nest bovenop de boom (hij gloeit roze als je dichtbij komt)",
    "z3_kale_kruin": "op de Kale Kruin, de dode kruintop die aan de westkant uit kruin 1 steekt",
    "z3_hangnest": "in Imkerdorp Zoemrust op de Bijentak, de reuzentak in het zuidoosten",
    "z3_kaaspost_1": "bij de eerste Kaasschutter-post, op de Westtak",
    "z3_kaaspost_2": "bij de tweede Kaasschutter-post, op de Noordtak aan de oostkant van de boom",
    "z3_kaaspost_3": "bij de derde Kaasschutter-post, op de Zuidwesttak",
    "z3_werf": "bij de havenpoort van de Kaaszeppelinwerf, het eiland in het noordoosten (over de kabelbrug vanaf Honingtak)",
    "z3_kajuit": "in de kajuit van Kapitein Roquefort, bovenop de hoge masttoren van Dok 3 op de Kaaszeppelinwerf",
    "z3_spiraalpoort": "bij de trapdeur in de stamwand aan het Bloesemplein van Honingtak",
    "z3_lantaarntoren": "bij de Lantaarntoren, midden in de holle stam van de Schorsstad (begin onderaan bij ring 1)",
    "z3_ring_1": "bij ring 1 van de Lantaarntoren, onderaan",
    "z3_ring_2": "bij ring 2 van de Lantaarntoren",
    "z3_ring_3": "bij ring 3 van de Lantaarntoren",
    "z3_ring_4": "bij ring 4 van de Lantaarntoren, halverwege",
    "z3_ring_5": "bij ring 5 van de Lantaarntoren",
    "z3_ring_6": "bij ring 6 van de Lantaarntoren",
    "z3_ring_7": "bij ring 7 van de Lantaarntoren, bovenaan",
    "z3_pelszaal": "in de Pelszaal, onder de vloer van de Schorsstad (de trap omlaag vanaf de vloer)",
    "z3_capespeld": "op de vloer van de Pelszaal, bij de troon van kaaskorsten",
    "z3_vingerafdruk": "op de binnenwand van de stam, bij een kwastgat halverwege de Schorsstad (de grauwe rijpvlek)",
    "z3_mosvoet": "in Mosvoet, op de grond in het zuidoosten (bij het onderstation van de Honinglift)",
    "z3_ketel_1": "bij de eerste schimmelketel van de Mika-schimmelboerderij, in het westen van de Wortelvelden (volg het piepen van het muisje)",
    "z3_ketel_2": "bij de tweede schimmelketel van de Mika-schimmelboerderij, in het westen van de Wortelvelden",
    "z3_wortelpoort": "bij de Wortelpoort, op de grond aan de oostkant van de boom",
    "z3_hartslagzaal": "in de Hartslag-zaal, de wortelhal onder de stam (door de Wortelpoort en de Wortelgang omlaag)",
    "z3_grotlantaarn_1": "bij een grotlantaarn voor de poort van het Hart, in de westwand van de wortelhal",
    "z3_grotlantaarn_2": "bij een grotlantaarn voor de poort van het Hart, in de westwand van de wortelhal",
    "z3_grotlantaarn_3": "bij een grotlantaarn naast de poort van het Hart, in de westwand van de wortelhal",
    "z3_grotlantaarn_4": "bij een grotlantaarn naast de poort van het Hart, in de westwand van de wortelhal",
    "z3_hartpoort": "bij de poort van het Hart van de Boom, in de westwand van de wortelhal",
    "z3_nestkamer": "bij de wortelmand van het ei, midden in de wortelhal onder de stam",
    "z3_uitkijk_kroon": "op uitkijkpunt De Kroon, op kruin 2 aan de zuidoostkant",
    "z3_uitkijk_takpunt": "op uitkijkpunt Takpunt, aan het eind van de Westtak",
    "z3_uitkijk_spiraal": "op het eerste balkon van het rijpad, aan de westkant van de stam",
    "z3_uitkijk_gewelf": "bovenop het Wortelgewelf, de grote ruïne in het noorden van de Wortelvelden",
    "z3_uitkijk_mast": "in het kraaiennest van de luchtschipmast van Honingtak",
}
PLEK_GEBIED = {
    "z3_honingtak": "in Honingtak, het ringstadje rond de stam, halverwege de boom",
    "z3_kruinzee": "in de Kruinzee, de drie kruinverdiepingen bovenin de boom",
    "z3_takkenbanen": "op de Takkenbanen, de reuzentakken tussen Honingtak en de kruin",
    "z3_zeppelinwerf": "op de Kaaszeppelinwerf, het eiland in het noordoosten (kabelbrug vanaf Honingtak)",
    "z3_schorsstad": "in de Schorsstad, in de holle stam",
    "z3_stamspiraal": "op het rijpad, de brede weg die buitenom de stam omhoog draait",
    "z3_wortelvelden": "in de Wortelvelden, op de grond rond de boom",
    "z3_wortelduister": "in het Wortelduister, de wortelhal en de gangen onder de boom",
    "z3_hart": "in het Hart van de Boom, de ring van zalen rond de wortelhal",
    "z3_ring": "in De Schemerwortel",
}
PLEK_KAMPVUUR = {
    "z3_honingtak": _PL,
    "z3_kruinzee": "aan de oostkant van kruin 1, bij Kruinwachter Blad",
    "z3_takkenbanen": "op de Westtak, vlak bij de stam",
    "z3_schorsstad": "op de vloer van de Schorsstad, onderaan de binnentrap",
    "z3_wortelvelden": "in Mosvoet, op de grond in het zuidoosten",
    "z3_wortelduister": "in de Hartslag-zaal, bij Wortelwacht Gloei",
}
PLEK_BAAS = {
    "z3_zeppelinwerf": "op de Kaaszeppelinwerf in het noordoosten (drie dokken: op de grond, op de steigertoren en op de hoge masttoren)",
    "z3_pelszaal": "in de Pelszaal, onder de vloer van de Schorsstad",
    "z3_hart": "in het Hart van de Boom, achter de poort in de westwand van de wortelhal",
    "z3_kruintop": "bij het oude nest helemaal bovenop de boom",
    "z3_kale_kruin": "op de Kale Kruin, aan de westkant van kruin 1",
}
PLEK_OVERIG = {
    P + "plek.overal.teller:z3_knuffels": "verstopt in De Schemerwortel: in de kruin, op de takken, in de stam en tussen de wortels (hartjes)",
    P + "plek.overal.teller:z3_aanwijzingen": "in Honingtak: bij het Bloesemplein, op de loopplanken van de takken en in de Raattrossen (paarse glinsters)",
    P + "plek.overal.teller:z3_schatten": "op geheime plekjes in De Schemerwortel",
    P + "plek.overal.teller:z3_kuikens": "op kruin 1, bij de honingvijvers (roze dons)",
    P + "plek.overal.teller:z3_lichtjes": "langs de binnentrap in de stam, net boven Honingtak, op verschillende hoogtes (groene gloed)",
    P + "plek.z3_luchtschip": "bij Kapitein Zoemzeil, aan de luchtschipmast van Honingtak",
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quest items (Z3NpcRegistry.QUEST_ITEMS): name, vanilla texture
# ---------------------------------------------------------------------------------------------------------------------------
QUEST_ITEMS = {
    "hartsplinter": ("Roze hartsplinter van Vahoegnir", "minecraft:item/pink_dye"),
    "piepje": ("Piepje (hij rilt in je tas)", "minecraft:item/rabbit_hide"),
    "nestlantaarn": ("Nestlantaarn", "minecraft:item/lantern"),
    "boomvolkkaart": ("Boomvolkkaart van de stam", "minecraft:item/filled_map"),
    "bleu_brief": ("Brief van Baron Bleu", "minecraft:item/paper"),
    "grauwe_brief": ("Grauwe brief (uit Nooitgenoeg)", "minecraft:item/paper"),
    "drakenei": ("Het Drakenei (warm en roze)", "minecraft:item/egg"),
    "moerasbloem": ("Moerasbloem", "minecraft:block/blue_orchid"),
    "mika_bevel": ("Mika-bevel", "minecraft:item/paper"),
    "honingthee": ("Honingthee", "minecraft:item/honey_bottle"),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quests. D(type, doel, n, text, bron, kans, plek): text = WHAT + WHO + WHERE; plek = where the tracker points.
# ---------------------------------------------------------------------------------------------------------------------------
QUESTS = []


def D(soort, doel, n, tekst, bron="", kans=0.0, plek=""):
    return dict(soort=soort, doel=doel, n=n, tekst=tekst, bron=bron, kans=kans, plek=plek)


def Q(id, soort, gever, titel, tekst, doelen, aanbod, bezig, klaar, xp=0, munten=0, items=(), wapen=False, loot="", lv=30, na=(),
      inleveren=None, klasse=None, auto=False, praat=None, actie_aanname="", actie_klaar="", start_items=()):
    QUESTS.append(dict(id=id, soort=soort, gever=gever, titel=titel, tekst=tekst, doelen=doelen, aanbod=aanbod, bezig=bezig, klaar=klaar,
                       beloning=dict(xp=xp, munten=munten, items=[list(i) for i in items], wapen=wapen, loot=loot), lv=lv, na=list(na),
                       inleveren=inleveren or gever, klasse=klasse, auto=auto, praat=praat or {}, actie_aanname=actie_aanname,
                       actie_klaar=actie_klaar, start_items=[list(i) for i in start_items]))


def item(id):
    return "guhs:vr_z3_npc_" + id


# ===== the main story of zone 3 (15) =========================================================================================
# (P-VERHUIS, ronde 20) adapted to the revamped tree: Honingtak is a ring town round the trunk (Bloesemplein = the south side), the
# Pluimstal stands on the ground at the foot of the OUTER riding spiral (the first ride goes up it to kruin 1), the beekeeper
# village Zoemrust (was Hangnest Dauwdrup) sits on the Bijentak, the werf is its own island (cable bridge), the Schorsstad is
# reached by the inner stair from the Bloesemplein, the Pelszaal lies UNDER the Schorsstad floor, Mosvoet at the foot of the
# Honinglift, the Wortelpoort in the east, and the escape ride goes from the roots up the riding spiral to the nest.
Q("z3_hq01_boom", "hoofd", "z2_kapitein", "Een boom zo groot als een berg",
  "Ordemeester Kristalijn zag het in het oosten: een boom zo groot als een berg. Kapitein Poolster in Kandijkom vliegt er nu "
  "naartoe: naar Honingtak, een imkersstadje dat als een ring om de stam van die boom hangt. Daar wacht Imkermoeder Raatje.",
  [D("meld", "z3:aangekomen", 1, "Vlieg met Kapitein Poolster vanuit Kandijkom naar Honingtak (kies Honingtak bij de luchtschipsteiger)",
     plek="npc:z2_kapitein"),
   D("praat", "z3_raatje", 1, "Praat met Imkermoeder Raatje op het Bloesemplein, aan de zuidkant van het ringdek van Honingtak "
     "(loop vanaf de mast over de Imkerstraat en het ringdek rond de stam)", plek="npc:z3_raatje")],
  [("npc", "Avonturier! Kristalijn vertelde het me. Een boom, zegt ze. In het oosten. Zo groot als een berg."),
   ("npc", "Ik vlieg er sinds vanochtend naartoe. Halverwege de stam hangt een stadje als een ring eromheen: Honingtak. Vol imkers en bijen."),
   ("jij", "Een stadje... om een boom heen?"),
   ("npc", "Ja. Niet naar beneden kijken. Stap in, het is een lange vlucht. En hij gaat omhoog. Heel erg omhoog.")],
  "Stap in het luchtschip naar Honingtak. Daar wacht Imkermoeder Raatje op het Bloesemplein.",
  [("npc", "Daar ben je, lieverd! Wat een vlucht, hè? Iedereen kijkt altijd zo verbaasd als ze de boom voor het eerst zien."),
   ("npc", "Ik ben Raatje. Burgemeester, imker en oma van iedereen hier. Welkom in Honingtak."),
   ("jij", "In de wortels... ik zag twee ogen. Grijsblauw. En ik hoorde iets kraken."),
   ("npc", "Ogen? In de wortels? Brrr. Dat moet Ordemeester Wasja horen. Ze zit in de Nestkapel, de grote raatcel onder de "
           "noordkant van het ringdek. Neem de kapellift omlaag.")],
  xp=4000, munten=3000, items=[(item("honingthee"), 2)], lv=30, na=["z2_hq12_hartsplinter"], inleveren="z3_raatje",
  actie_aanname="z3_vlieg:honingtak")

Q("z3_hq02_bezoek", "hoofd", "z3_wasja", "Bezoek uit de lucht",
  "Ordemeester Wasja van de Nestkapel denkt dat deze boom ooit het nest van Vahoegnir was. Maar dan zoemt er een Mika-zeppelin over "
  "de markt. Versla de Bladrovers die hij op het Bloesemplein laat vallen.",
  [D("opvrolijk", "*", 8, "Versla 8 Bladrovers die de Mika-zeppelin op het Bloesemplein van Honingtak laat vallen (de kapellift "
     "omhoog, dan over het ringdek naar de zuidkant)", plek="poi:z3_plein"),
   D("praat", "z3_raatje", 1, "Kijk of alles goed is met Imkermoeder Raatje op het Bloesemplein", plek="npc:z3_raatje")],
  [("npc", "Een avonturier van de Orde! Welkom in de Nestkapel. Kijk, die lege eiersokkel daar? Niemand weet meer waarvoor hij is."),
   ("npc", "Maar ik heb een vermoeden. Deze boom... de takken, de veren in de kruin... ik denk dat dit ooit het nest van "
           "Vahoegnir was."),
   ("jij", "Het nest van de draak?"),
   ("npc", "Ssst. Hoor je dat? Een propeller. Dat is geen bij. Dat is... een ZEPPELIN! Naar boven, snel, naar het plein!")],
  "De Bladrovers op het Bloesemplein! En daarna even kijken bij Imkermoeder Raatje.",
  [("npc", "Iedereen is veilig. De guhs zaten allemaal in hun korfhuisjes, met de luikjes dicht. Niemand is geraakt."),
   ("npc", "Maar kijk wat de zeppelin liet vallen. Een pamflet. 'Operatie Rijpe Kaas. In opdracht van Baron Bleu, namens Zijne "
           "Deftigheid Graaf Piepvel.'"),
   ("jij", "Een graaf?"),
   ("z3_wasja", "Een graaf? Mika's met titels? Dat is nieuw. En niet goed nieuws."),
   ("npc", "Wat je nu nodig hebt, is een rijdier. Deze boom is te groot om te lopen. Stalmeester Veertje heeft haar Pluimstal "
           "beneden op de grond, aan de noordkant, waar het rijpad begint."),
   ("npc", "Neem de lift in de binnenhof van de stam (door de trapdeur hier aan het plein) naar de vloer van de Schorsstad, en ga "
           "door de noordpoort naar buiten. En hier: de sleutel van de Honinglift. Daarmee ben je altijd snel weer boven.")],
  xp=5000, munten=3500, loot="zeldzaam", lv=30, na=["z3_hq01_boom"], inleveren="z3_raatje", actie_aanname="z3_bam",
  actie_klaar="z3_honinglift")

Q("z3_hq03_pluimstal", "hoofd", "z3_veertje", "De Pluimstal",
  "Stalmeester Veertje heeft drie jonge Pluimguhs. Kies er één, geef hem een naam, en maak je eerste rit: Veertje rijdt voor je uit "
  "het rijpad op, rondje na rondje om de stam, tot bovenop kruin 1. Spring daar van het zweefplatform: je eerste Pluimzweef.",
  [D("meld", "rijdier:starter", 1, "Kies je eigen Pluimguh bij Stalmeester Veertje in de Pluimstal (op de grond aan de noordkant van de boom)",
     plek="npc:z3_veertje"),
   D("meld", "z3:opgestegen", 1, "Roep je Pluimguh (toets X) en stap op, bij de Pluimstal", plek="npc:z3_veertje"),
   D("gebied", "poi:z3_rijpad_eind", 1, "Rijd achter Stalmeester Veertje aan het rijpad op: om de stam heen, langs het Bloesemplein en "
     "door de bladtunnel tot bovenop kruin 1", plek="poi:z3_rijpad_eind"),
   D("meld", "z3:gezweefd", 1, "Rijd naar het zweefplatform aan de zuidrand van kruin 1, spring eraf en zweef naar beneden (houd "
     "springen ingedrukt)", plek="poi:z3_zweefplatform")],
  [("npc", "Een ridder zonder rijdier in een boom van deze maat? Dan loop je hier tot volgend voorjaar."),
   ("npc", "Kijk, daar in de box. Drie jonge Pluimguhs. Roze, munt en lavendel. Lange pootjes, donzig lijfje, een pluimstaart."),
   ("jij", "Wacht eens... grote gele vogels waar je op rijdt... dat ken ik ergens van..."),
   ("npc", "Nee, het is géén choco-dinges uit een of ander Fantasy-spel. Dit is een Pluimguh. Kijk maar naar de oortjes, njeg!"),
   ("npc", "Fluit maar op de Stalfluit. Ze komen alle drie aangerend en laten zien wat ze kunnen. Kies er één. Daarna rijden we samen "
           "het rijpad op. Ik rij voor, jij volgt.")],
  "Kies je Pluimguh, rijd achter Veertje aan het rijpad op tot bovenop kruin 1, en zweef van het zweefplatform naar beneden.",
  [("npc", "Zag je dat? Jij en je Pluimguh, door de lucht! Je eerste Pluimzweef!"),
   ("npc", "Vanaf hoog kun je altijd zweven. Alleen omlaag, hoor. Omhoog gaat met het rijpad, de trap, de liften en de bloesemwind."),
   ("npc", "Zag je die grauwe plekken in de kruin? Die zijn van de zeppelins. Kruinwachter Blad heeft hulp nodig. Hij zit bij zijn "
           "kampvuur aan de oostkant van kruin 1.")],
  xp=5000, munten=3000, lv=30, na=["z3_hq02_bezoek"], actie_aanname="z3_starter")

Q("z3_hq04_vlekken", "hoofd", "z3_veertje", "Grauwe vlekken",
  "Mika-zeppelins hebben de Kruinzee grauw gespoten. Rijd langs de drie grauwe plekken op kruin 1 en kruin 2 en maak ze schoon, "
  "versla de wezens in de kruin en kijk bij de Mika-zeppelinsteiger wat ze van plan zijn. Kruinwachter Blad wacht bij zijn kampvuur.",
  [D("gebied", "poi:z3_vlek_1", 1, "Maak de eerste grauwe plek schoon, aan de zuidoostkant van kruin 1", plek="poi:z3_vlek_1"),
   D("gebied", "poi:z3_vlek_2", 1, "Maak de tweede grauwe plek schoon, op kruin 2 aan de zuidwestkant (de windkolom omhoog)",
     plek="poi:z3_vlek_2"),
   D("gebied", "poi:z3_vlek_3", 1, "Maak de derde grauwe plek schoon, aan de noordkant van kruin 1", plek="poi:z3_vlek_3"),
   D("opvrolijk", "gebied:z3_kruinzee", 12, "Versla 12 wezens in de Kruinzee", plek="gebied:z3_kruinzee"),
   D("gebied", "poi:z3_zeppelinsteiger", 1, "Zoek bij de Mika-zeppelinsteiger aan de noordoostrand van kruin 1 naar hun plannen",
     plek="poi:z3_zeppelinsteiger")],
  [("npc", "Kruinwachter Blad bewaakt de Kruinzee. Hij zit bij zijn kampvuur aan de oostkant van kruin 1, niet ver van de "
           "Mika-zeppelinsteiger."),
   ("npc", "Maak onderweg de grauwe plekken schoon. Je hoeft er alleen maar langs te rijden: je Pluimguh schudt bloesem uit zijn "
           "pluim. Echt waar. Daar is hij voor gefokt. Naar kruin 2 kom je met de roze windkolom.")],
  "Drie grauwe plekken, twaalf wezens, en een kijkje bij de zeppelinsteiger. Daarna naar Kruinwachter Blad.",
  [("npc", "Je hebt de kruin schoongemaakt! De bladeren worden al weer groen. Wat vond je bij de steiger?"),
   ("jij", "Een bevel. 'Zoek het ei. Laat het rijpen. Breng het naar de Graaf. De Graaf brengt het naar Hem.'"),
   ("npc", "Hem? Met een hoofdletter? Wie is dat dan?"),
   ("jij", "Geen idee. Niemand weet het."),
   ("npc", "Een ei... Helemaal bovenop de boom, boven kruin 3, ligt een oud nest. Enorm. Ik durf er nooit dichtbij. Jij wel, denk ik.")],
  xp=6000, munten=4000, loot="zeldzaam", lv=31, na=["z3_hq03_pluimstal"], inleveren="z3_bladwachter")

Q("z3_hq05_nest", "hoofd", "z3_bladwachter", "Het oude nest",
  "Helemaal bovenop de boom, boven de drie kruinverdiepingen, ligt een reusachtig oud nest. Ga erheen en kijk wat er nog van over is.",
  [D("gebied", "poi:z3_kruintop", 1, "Ga naar het oude nest bovenop de boom: de roze windkolommen blazen je van kruin 1 naar kruin 2 en 3 "
     "(of neem de binnentrap tot het einde)", plek="poi:z3_kruintop"),
   D("meld", "z3:nest_herinnering", 1, "Kijk rond in het oude nest", plek="poi:z3_kruintop"),
   D("gebied", "poi:z3_nestsplinter", 1, "Zoek het roze gloeiende ding tussen de takken van het nest", plek="poi:z3_nestsplinter")],
  [("npc", "Zie je die roze zuilen met wervelende bloesem? Dat zijn windkolommen. Stap erin en je zweeft naar de volgende kruin. "
           "Drie keer, en je staat bij het nest."),
   ("npc", "Pas op voor de Kruinroofvogel. Hij zit op de Kale Kruin, de dode top aan de westkant van kruin 1. Laat hem maar met rust, "
           "als je niet wilt vechten.")],
  "Naar het oude nest bovenop de boom. En dan terug naar Ordemeester Wasja in de Nestkapel.",
  [("npc", "Een hartsplinter. Roze, warm... net als de splinter die je in de Splinterpieken vond."),
   ("npc", "Hij is hier teruggekomen. Gewond. Hij liet een stukje van zijn hart achter bij zijn ei."),
   ("jij", "En er waren sleepsporen naar de rand. En kleine houten voetafdrukjes."),
   ("npc", "Houten voetjes? Dan was het niet de Mika's. Iemand heeft dat ei lang geleden weggebracht. Ik moet nadenken. En Raatje "
           "ook.")],
  xp=7000, munten=4500, lv=33, na=["z3_hq04_vlekken"], inleveren="z3_wasja")

Q("z3_hq06_voetjes", "hoofd", "z3_wasja", "Kleine voetjes",
  "De houten voetafdrukjes in het nest zijn van het boomvolk, de oude bewoners van de stam. Vraag Imkermoeder Raatje wat zij weet. "
  "En in de Raattrossen piept iets.",
  [D("praat", "z3_raatje", 1, "Vraag Imkermoeder Raatje op het Bloesemplein naar het boomvolk", plek="npc:z3_raatje"),
   D("gebied", "poi:z3_raattrossen", 1, "Zoek wat er piept in de Raattrossen, de raatcel onder de oostkant van het ringdek (neem de "
     "raattroslift omlaag)", plek="poi:z3_raattrossen")],
  [("npc", "Raatje is de oudste van de stad. Als iemand iets weet van het boomvolk, dan zij."),
   ("npc", "En hoor je dat? Al de hele ochtend piept er iets in de Raattrossen. Kijk eens even, wil je?")],
  "Eerst Imkermoeder Raatje, en dan dat gepiep in de Raattrossen.",
  [("npc", "Een pieppiepmuisje! Helemaal kaal! En zo koud... kom maar, kleintje."),
   ("jij", "Hij zei alleen 'de Graaf... de kou...'. Hij komt van ergens onder ons. Uit het donker."),
   ("npc", "Onder ons? In de stam woonde vroeger het boomvolk. Hun oude kaart ligt bij Kaartenmaakster Lusje, in Imkerdorp "
           "Zoemrust, op de Bijentak in het zuidoosten."),
   ("npc", "Neem het muisje maar mee. Het wil niet meer uit je tas. Ik geloof dat het je vertrouwt.")],
  xp=6000, munten=4000, lv=34, na=["z3_hq05_nest"], inleveren="z3_raatje",
  praat={"z3_raatje": [
      ("npc", "Houten voetjes? Ach, het boomvolk. Mijn oma vertelde erover. Kleine houten wezentjes met lichtjes in hun hoofd."),
      ("npc", "Ze bouwden een stad in de stam, zei ze. Met lantaarns, langs een trap die tot in het nest liep. Toen werd het stil, en "
              "de lantaarns gingen uit."),
      ("npc", "Ze hadden een kaart van de hele stam. Die ligt nu bij Lusje, in Zoemrust. Denk ik. Hoop ik.")]})

Q("z3_hq07_hangnest", "hoofd", "z3_raatje", "Imkerdorp Zoemrust",
  "Kaartenmaakster Lusje woont in Imkerdorp Zoemrust, op de Bijentak in het zuidoosten. Rijd het rijpad op en de tak op, en help "
  "het dorp: Mika-Kaasschutters hebben drie posten op de reuzentakken gebouwd.",
  [D("gebied", "poi:z3_hangnest", 1, "Rijd naar Imkerdorp Zoemrust: het rijpad op tot net boven Honingtak, en aan de zuidoostkant de "
     "Bijentak op", plek="poi:z3_hangnest"),
   D("praat", "z3_lusje", 1, "Praat met Kaartenmaakster Lusje in Imkerdorp Zoemrust", plek="npc:z3_lusje"),
   D("gebied", "poi:z3_kaaspost_1", 1, "Ruim de eerste Kaasschutter-post op, op de Westtak", plek="poi:z3_kaaspost_1"),
   D("gebied", "poi:z3_kaaspost_2", 1, "Ruim de tweede Kaasschutter-post op, op de Noordtak aan de oostkant van de boom",
     plek="poi:z3_kaaspost_2"),
   D("gebied", "poi:z3_kaaspost_3", 1, "Ruim de derde Kaasschutter-post op, op de Zuidwesttak", plek="poi:z3_kaaspost_3"),
   D("opvrolijk", "gebied:z3_takkenbanen", 12, "Versla 12 wezens of Mika's op de Takkenbanen", plek="gebied:z3_takkenbanen")],
  [("npc", "Het rijpad loopt overal langs de voet van de reuzentakken. Waar een tak begint, rij je er zo op. Zoemrust ligt op de "
           "Bijentak, in het zuidoosten."),
   ("npc", "Niet bang zijn op de takken. Overal waar je kunt vallen, kun je zweven. En je Pluimguh is heel goed in niet-vallen.")],
  "Naar Imkerdorp Zoemrust op de Bijentak, Lusje helpen, en de drie Kaasschutter-posten opruimen.",
  [("npc", "De posten zijn weg! En mijn thee is weer warm. Nou ja, lauw. Het is een begin."),
   ("npc", "De Boomvolkkaart... die hebben de Mika's meegenomen. Naar hun werf. De Kaaszeppelinwerf, op het eiland in het noordoosten. "
           "Je komt er over de kabelbrug, vanaf de noordoostrand van Honingtak."),
   ("npc", "Ik zag ze gaan. Met de kaart in een kaaskist. Ze lachten erbij. Heel onbeleefd.")],
  xp=8000, munten=5000, loot="zeldzaam", lv=35, na=["z3_hq06_voetjes"], inleveren="z3_lusje",
  praat={"z3_lusje": [
      ("npc", "Een bezoeker! Voorzichtig, de tak wiebelt. Ik ben Lusje. Ik teken kaarten. Van alles. Ook van dingen die ronddraaien."),
      ("npc", "De Boomvolkkaart? Ach... Er zijn Mika's geweest. Niemand is geraakt, hoor. Maar mijn thee was koud geworden van de "
              "schrik."),
      ("npc", "Ze hebben drie Kaasschutter-posten op de takken gebouwd. Help eerst het dorp, wil je? Dan vertel ik alles.")]})

Q("z3_hq08_werf", "hoofd", "z3_lusje", "De Kaaszeppelinwerf",
  "De Kaaszeppelinwerf ligt op een eigen eiland in het noordoosten: een Mika-scheepswerf met drie dokken, elk met een half gebouwde "
  "kaaszeppelin. Steek de kabelbrug over, versla Takelmeester Taleggio, Bombardier Brokkel en Kapitein Roquefort, en zoek in de "
  "kajuit van de kapitein naar de Boomvolkkaart.",
  [D("meld", "z3_taleggio:verslagen", 1, "Versla Takelmeester Taleggio bij Dok 1, op de grond van de werf (over de kabelbrug vanaf "
     "Honingtak)", plek="baas:z3_zeppelinwerf"),
   D("meld", "z3_brokkel:verslagen", 1, "Versla Bombardier Brokkel op Dok 2, bovenop de steigertoren (draai de kanonnen!)",
     plek="baas:z3_zeppelinwerf"),
   D("meld", "z3_roquefort:verslagen", 1, "Versla Kapitein Roquefort op Dok 3, bovenop de hoge masttoren", plek="baas:z3_zeppelinwerf"),
   D("gebied", "poi:z3_kajuit", 1, "Doorzoek de kajuit van Kapitein Roquefort op Dok 3", plek="poi:z3_kajuit")],
  [("npc", "De kabelbrug begint aan de noordoostrand van Honingtak. Je ziet de kranen al van hier. En je ruikt de kaas."),
   ("npc", "Ga met vrienden als dat kan. Drie Mika-bazen, op drie dokken, de een nog hoger dan de ander. Zelfs voor een held met een "
           "Pluimguh is dat veel.")],
  "De Kaaszeppelinwerf: over de kabelbrug, drie dokken, drie bazen, en dan de kajuit van de kapitein.",
  [("npc", "Je hebt de kaart! En wat is dat... een kistje met een strik?"),
   ("jij", "'Voor de Graaf, voor zijn verzameling.' Er zat een hartsplinter in. En een brief van Baron Bleu."),
   ("npc", "Lees eens voor?"),
   ("jij", "'Mijn schimmel maakt het ei grauw. Een grauwe draak, helemaal van ons. Hij zal tevreden zijn, Uwe Deftigheid. Eindelijk "
           "één ding dat Hij vol vindt.'"),
   ("npc", "...Vol? Wie vindt er nou nooit iets vol? Brrr. Kijk, de kaart. Binnen in de stam loopt een trap, van de grond tot in het "
           "nest. De deur zit aan het Bloesemplein. En halverwege de Schorsstad is een lichtje getekend.")],
  xp=12000, munten=8000, loot="episch", lv=37, na=["z3_hq07_hangnest"], inleveren="z3_lusje")

Q("z3_hq09_spiraal", "hoofd", "z3_lusje", "De binnentrap",
  "De Boomvolkkaart toont de weg naar binnen: de trapdeur aan het Bloesemplein. Daarachter draait de binnentrap omlaag door de holle "
  "stam, langs de huisjes van het boomvolk in de wand: de Schorsstad. Halverwege staat een lichtje op de kaart.",
  [D("gebied", "poi:z3_honingdeur", 1, "Ga door de trapdeur in de stamwand aan het Bloesemplein van Honingtak", plek="poi:z3_honingdeur"),
   D("praat", "z3_knoest", 1, "Daal de binnentrap af en zoek het lichtje in een huisje in de stamwand, halverwege de Schorsstad",
     plek="npc:z3_knoest")],
  [("npc", "Zie je die deur in de stam, aan het plein? Ik dacht altijd dat het een bezemkast was. Volgens de kaart is het een trap."),
   ("npc", "Halverwege staat een tekeningetje van een lichtje. Met een gezichtje. Het knipoogt. Raar, voor een kaart.")],
  "Door de trapdeur aan het Bloesemplein, de binnentrap af, en dan het lichtje zoeken.",
  [("npc", "...Je ziet me. Al zo lang heeft niemand me gezien. Ik ben Knoest. Het laatste lichtje van het boomvolk."),
   ("npc", "De lantaarns zijn uit sinds de Graaf onder onze vloer woont. Hij houdt van donker. En van kou. Dan vallen de pelsjes "
           "sneller uit."),
   ("verteller", "Piepje rilt in je tas."),
   ("npc", "Kijk, daar. De Lantaarntoren, midden in de stam. Zeven ringen. Als die weer branden, kan hij zich nergens meer verstoppen.")],
  xp=9000, munten=6000, lv=39, na=["z3_hq08_werf"], inleveren="z3_knoest", actie_klaar="z3_honinglift")

Q("z3_hq10_piepvel", "hoofd", "z3_knoest", "Zeven lantaarns en een graaf",
  "In het midden van de Schorsstad staat de Lantaarntoren. Steek de zeven ringen aan, van onder naar boven: op elke ring zitten "
  "muisjes in een kooitje. Onder de vloer van de Schorsstad, in de Pelszaal, zit Graaf Piepvel, de Pink van de Keizer, in een cape "
  "van muizenpelsjes.",
  [D("meld", "z3_lantaarn:ring", 7, "Steek de 7 ringen van de Lantaarntoren aan, van onder naar boven (ga op de binnentrap bij elke "
     "ringlantaarn staan)", plek="poi:z3_lantaarntoren"),
   D("meld", "z3_piepvel:verslagen", 1, "Versla Graaf Piepvel in de Pelszaal, onder de vloer van de Schorsstad (breek de kooisloten, "
     "steek de lantaarns weer aan)", plek="baas:z3_pelszaal"),
   D("meld", "z3:flash_f1", 1, "Kijk wat er gebeurt in de Pelszaal", plek="poi:z3_pelszaal"),
   D("gebied", "poi:z3_capespeld", 1, "Raap de speld van de cape op, bij de troon in de Pelszaal", plek="poi:z3_capespeld")],
  [("npc", "Zie je de toren? Hij staat midden in de stam, van de vloer tot de binnenhof van Honingtak. Zeven ringen. Zeven lantaarns."),
   ("npc", "Op elke ring heeft de Graaf muisjes in een kooitje gezet. In de kou. Tot hun pelsje uitvalt. Voor zijn cape."),
   ("verteller", "Piepje piept heel zachtjes."),
   ("npc", "Steek de ringlantaarns aan vanaf de trap. Dan springen de slotjes open. Halverwege geef ik je iets, want onder de vloer is "
           "het pikdonker."),
   ("npc", "En onder de vloer... daar zit hij. Graaf Piepvel. De Pink van de Keizer. De trap naar zijn Pelszaal begint op de vloer.")],
  "Zeven ringen, van onder naar boven. En dan de trap af naar de Pelszaal.",
  [("npc", "...De lantaarns. Allemaal. De hele stam gloeit weer. Zo heb ik hem niet meer gezien sinds... ik weet het niet eens meer."),
   ("npc", "Die mist... Ik ken hem uit oude verhalen. Het is zijn grijpgrage macht. Die reikt ver. Altijd verder. Hij neemt alles wat warm is."),
   ("npc", "Ik zal je vertellen wat er met het ei gebeurde. Het boomvolk droeg het naar beneden toen de draak grauw werd. Het hart van "
           "de boom zou het warm houden, tot hij weer zichzelf was."),
   ("npc", "Wij gingen slapen in het hout. Het ei bleef wakker."),
   ("npc", "Ga naar Ordemeester Wasja. Vertel haar alles. De lift in de binnenhof brengt je zo naar Honingtak.")],
  xp=15000, munten=10000, loot="episch", lv=40, na=["z3_hq09_spiraal"], inleveren="z3_knoest")

Q("z3_hq11_wortel", "hoofd", "z3_wasja", "Rot aan de wortel",
  "In Mosvoet, het gehucht onderaan de boom, rotten de wortels weg: de Mika's hebben schimmelketels in de Wortelvelden gezet. Praat "
  "met Burgemeester Wortelien en zet de twee ketels uit. De bevrijde muisjes piepen de weg.",
  [D("praat", "z3_wortelien", 1, "Praat met Burgemeester Wortelien in Mosvoet (neem de Honinglift omlaag, in het zuidoosten van Honingtak)",
     plek="npc:z3_wortelien"),
   D("meld", "z3_ketel:een", 1, "Zet de eerste schimmelketel van de Mika-schimmelboerderij uit, in het westen van de Wortelvelden",
     plek="poi:z3_ketel_1"),
   D("meld", "z3_ketel:twee", 1, "Zet de tweede schimmelketel uit, ook bij de Mika-schimmelboerderij", plek="poi:z3_ketel_2")],
  [("npc", "Een keizer. De Mika's hebben een keizer. Dan is dit groter dan een draak, ridder. Veel groter."),
   ("npc", "Maar eerst: het ei. Het ligt in het hart van de boom, onder de stam. En daar beneden rotten de wortels."),
   ("npc", "Neem de Honinglift naar Mosvoet. Burgemeester Wortelien weet meer. En de muisjes die je bevrijdde, die ruiken schimmel van "
           "een kilometer afstand.")],
  "Met de Honinglift naar Mosvoet, Burgemeester Wortelien, en dan de twee schimmelketels uit.",
  [("npc", "De ketels zijn uit! Ruik je dat? Het ruikt weer naar sap. En naar mos. En een beetje naar ahornsiroop."),
   ("npc", "Ik zie het veld al terugkleuren. Groen, roze, en het water weer turquoise."),
   ("npc", "Er is nog één ding. Onder de boom, in het Wortelduister, gloeien de aders roze. Steeds feller. Alsof daar iets leeft. De "
           "Wortelpoort ligt aan de oostkant, op de grond.")],
  xp=12000, munten=9000, loot="zeldzaam", lv=42, na=["z3_hq10_piepvel"], inleveren="z3_wortelien",
  praat={"z3_wortelien": [
      ("npc", "Een held met een Pluimguh! En met... muisjes? Ze piepen allemaal tegelijk. Ze willen ergens heen."),
      ("npc", "De Mika's hebben twee schimmelketels in de Wortelvelden gezet. Allebei bij hun schimmelboerderij, aan de westkant van "
              "de boom. Daardoor rot alles."),
      ("npc", "Volg de muisjes maar. Ze staan bij elke ingang en piepen de weg. Je hoeft niks te zoeken.")]})

Q("z3_hq12_duister", "hoofd", "z3_wortelien", "Het Wortelduister",
  "Onder de boom ligt een grote wortelhal: het Wortelduister. Volg met je Nestlantaarn de roze aders door de Wortelgang naar de "
  "Hartslag-zaal, en steek de vier grotlantaarns bij de poort van het Hart aan, aan de overkant van de hal.",
  [D("gebied", "poi:z3_wortelpoort", 1, "Ga naar de Wortelpoort, op de grond aan de oostkant van de boom", plek="poi:z3_wortelpoort"),
   D("gebied", "poi:z3_hartslagzaal", 1, "Volg de roze aders door de Wortelgang omlaag naar de Hartslag-zaal, de wortelhal onder de stam",
     plek="poi:z3_hartslagzaal"),
   D("meld", "z3_grotlantaarn:aan", 4, "Steek de 4 grotlantaarns rond de poort van het Hart aan, in de westwand van de hal",
     plek="poi:z3_hartpoort")],
  [("npc", "De Wortelpoort ligt aan de oostkant van de boom, op de grond. Daarachter gaat een gang omlaag. Daarna is het donker. "
           "Echt donker."),
   ("npc", "Blijf in het licht van je lantaarn. In het donker doen de monsters minder pijn, maar je ziet ze ook bijna niet."),
   ("npc", "Bij het kampvuur in de Hartslag-zaal staat Wortelwacht Gloei. Hij weet hoe je het Hart in komt.")],
  "Door de Wortelpoort, de roze aders volgen naar de Hartslag-zaal, en de vier grotlantaarns aan.",
  [("npc", "Je hebt de lantaarns aangestoken! Kijk, de poort van het Hart gaat open. Hoor je die klop?"),
   ("jij", "En er klopt nog iets. Zachter. Sneller. Daar, in het midden van de hal, in die mand van wortels."),
   ("npc", "...Dat heb ik nooit gehoord. Een tweede hart. Een klein hartje. Maar de wachters van het Hart laten je er niet bij. Ga, "
           "avonturier. Door de poort, de ring van zalen rond.")],
  xp=14000, munten=10000, loot="episch", lv=45, na=["z3_hq11_wortel"], inleveren="z3_gloei")

Q("z3_hq13_hart", "hoofd", "z3_gloei", "Het Hart van de Boom",
  "Achter de poort in de westwand loopt het Hart van de Boom als een ring van zalen om de wortelhal. Versla de Wortelwurger, vrolijk "
  "de Oude Nestwachter op en versla Baron Bleu en zijn Schimmelkolos. Dan kun je bij het Drakenei, midden in de hal.",
  [D("meld", "z3_wortelwurger:verslagen", 1, "Versla de Wortelwurger in het Hart van de Boom (kap de 4 voedingswortels door)",
     plek="baas:z3_hart"),
   D("meld", "z3_nestwachter:opgevrolijkt", 1, "Vrolijk de Oude Nestwachter op (sta op de roze jaarring)", plek="baas:z3_hart"),
   D("meld", "z3_bleu:verslagen", 1, "Versla Baron Bleu en zijn Schimmelkolos (vecht in het licht van de lantaarnpalen)", plek="baas:z3_hart"),
   D("meld", "z3:ei", 1, "Pak het Drakenei uit de wortelmand, midden in de wortelhal", plek="poi:z3_nestkamer")],
  [("npc", "Het Hart is een kerker. Kloppende wanden, schimmelgordijnen, en drie wachters, de ring rond. Ga met vrienden als het kan."),
   ("npc", "De Wortelwurger is een kluwen grauwe wortels. Kap zijn voedingswortels door als het hart klopt."),
   ("npc", "En de Oude Nestwachter... die is niet slecht. Alleen grauw. Vrolijk hem op, dan laat hij je door.")],
  "Het Hart van de Boom: de Wortelwurger, de Oude Nestwachter, Baron Bleu. En dan het ei, midden in de hal.",
  [("npc", "Je hebt het. Het ei. Kijk hoe het gloeit als jij het vasthoudt."),
   ("npc", "Dit hield ik warm voor hem. Het hartje van zijn hart. Die splinter heb je al. Nu nog hém."),
   ("verteller", "De grond begint te trillen. Eerst zacht. Dan harder. Ergens hoog boven je kraakt de hele boom."),
   ("npc", "Hij komt. Hij voelt dat het ei wakker is. Ga! De Wortelgang uit, het rijpad op, terug naar het oude nest! Snel!")],
  xp=20000, munten=15000, loot="episch", lv=47, na=["z3_hq12_duister"], inleveren="z3_nestwachter", actie_klaar="z3_hq14")

Q("z3_hq14_vahoegnir", "hoofd", "z3_nestwachter", "Vahoegnir komt thuis",
  "Vahoegnir komt zijn ei halen. Ontsnap uit de wortels en rijd het rijpad op, de windkolommen omhoog, naar het oude nest bovenop de "
  "boom. Leg het ei terug en vrolijk de Vadsloze Draak op: steek bij 75, 50 en 25 procent een hartsplinter terug in zijn borst.",
  [D("meld", "z3:ontsnapt", 1, "Ontsnap: de Wortelgang uit, het rijpad om de stam omhoog en de windkolommen op, naar het oude nest",
     plek="poi:z3_kruintop"),
   D("meld", "z3_vahoegnir:opgevrolijkt", 1, "Vrolijk Vahoegnir op bij het oude nest bovenop de boom (sta in de roze cirkel onder zijn "
     "borst)", plek="baas:z3_kruintop"),
   D("meld", "z3:finale", 1, "Kijk wat er met het ei gebeurt", plek="poi:z3_kruintop")],
  [("npc", "Ga!")],
  "Naar het oude nest bovenop de boom. Vrolijk Vahoegnir op. En dan terug naar Ordemeester Wasja in de Nestkapel.",
  [("npc", "Je bent terug! En... wat zit er op je schouder? Is dat... een draakje?"),
   ("verteller", "Het babydraakje niest. Er komt een roze wolkje uit."),
   ("npc", "Een roze draakje. In mijn kapel. Ik ga zitten. Nee, ik blijf staan. Ik weet het niet meer."),
   ("npc", "Iedereen heeft het gezien: de roze straal over de hele hemel. Naar het oosten. Kom, ridder. Raatje wil je ook spreken.")],
  xp=25000, munten=20000, loot="episch", lv=48, na=["z3_hq13_hart"], inleveren="z3_wasja", auto=True, actie_aanname="z3_rit")

Q("z3_hq15_hoogvads", "hoofd", "z3_wasja", "Naar de Zwevende Stad",
  "De draak is weer roze en het ei is uit. Zijn roze straal wees naar het oosten, naar Hoogvads, de Zwevende Stad. Zeg Imkermoeder "
  "Raatje gedag en ga terug naar Ordemeester Wasja.",
  [D("praat", "z3_raatje", 1, "Zeg Imkermoeder Raatje gedag, op het Bloesemplein van Honingtak", plek="npc:z3_raatje")],
  [("npc", "Hoogvads. De Zwevende Stad van de Hoge Raad. Daar weten ze meer over keizers dan wij."),
   ("npc", "Ze willen jou zien, ridder. Jou en dat kleintje. Maar zeg eerst Raatje gedag. Ze heeft taart gebakken. Drie.")],
  "Raatje gedag zeggen, en dan terug naar Ordemeester Wasja in de Nestkapel.",
  [("npc", "Kijk eens, met Raatje. En Piepje, met een nieuw donzig pelsje. En dat draakje dat alles opeet wat honing is."),
   ("npc", "Ga naar de rand van het Bloesemplein, ridder. Kijk naar het oosten. Waar de straal eindigde."),
   ("jij", "Daar zweeft een stad. Boven de wolken."),
   ("npc", "Hoogvads. Binnenkort vliegt er een luchtschip naartoe. Tot die tijd: rust uit. Je hebt het verdiend. Drakenvriend.")],
  xp=15000, munten=25000, lv=48, na=["z3_hq14_vahoegnir"], inleveren="z3_wasja", actie_klaar="z3_cliffhanger",
  praat={"z3_raatje": [
      ("npc", "Daar is onze held! Met een draakje! Kom hier, dan knuffel ik jullie allebei. Voorzichtig, hij niest."),
      ("npc", "Honingtak zal je nooit vergeten. Hier is altijd een bedje voor je. En taart. Drie taarten."),
      ("npc", "Ga maar naar Wasja. Ze staat al de hele dag naar het oosten te staren.")]})

# ===== side quests (light: the user prefers few) ==================================================================================
_NA = ["z3_hq03_pluimstal"]
Q("z3_s01_kuikens", "zij", "z3_kuikenhoeder", "Veertjes zoek",
  "Kuikenhoeder Pip is zes Pluimkuikens kwijt in de Kruinzee. Ze zitten verstopt bij de honingvijvers op kruin 1, in roze dons. Vind ze allemaal.",
  [D("geheim", "teller:z3_kuikens", 6, "Vind 6 verdwaalde Pluimkuikens bij de honingvijvers op kruin 1 (roze dons)", plek="gebied:z3_kruinzee")],
  [("npc", "Help! Mijn Pluimkuikens zijn weg! Ze renden achter een vlinder aan. Alle zes. De Kruinzee in."),
   ("npc", "Ze zitten vast bij de honingvijvers op kruin 1, rondom de stam. Je ziet ze aan het roze dons. En aan het gepiep.")],
  "Zes Pluimkuikens, ergens bij de honingvijvers in de kruin.",
  [("npc", "Ze zijn er allemaal! Eén, twee, drie, vier, vijf... zes! Dank je wel! Hier, een zakje Honingbrokjes voor je Pluimguh.")],
  xp=6000, munten=4000, lv=31, na=_NA)

Q("z3_s02_opa_bast", "zij", "z3_opa_bast", "Nog één keer de top",
  "Opa Bast wil nog één keer het oude nest bovenop de boom zien, zoals vroeger. Hij kan niet meer zo goed lopen. Neem hem mee.",
  [D("escorte", "z3_opa_bast", 1, "Breng Opa Bast naar het oude nest bovenop de boom (hij loopt met je mee; wezens laten hem met rust)", bron="poi:z3_kruintop",
     plek="poi:z3_kruintop")],
  [("npc", "Vroeger klom ik elke dag de binnentrap op naar het nest. Om de draak te horen zingen. Heel zacht."),
   ("npc", "Neem je me nog één keer mee? Ik loop langzaam, maar ik loop. En de monsters doen niks met zo'n ouwe guh als ik.")],
  "Opa Bast loopt met je mee naar het oude nest.",
  [("npc", "Kijk nou. Het nest. Net als vroeger. Hoor je dat? Nee? Ik ook niet. Maar ik weet nog hoe het klonk."),
   ("npc", "Dank je, jongen. Of meisje. Of guh. Mijn ogen zijn niet meer zo goed. Mijn hart wel.")],
  xp=8000, munten=5000, lv=33, na=["z3_hq05_nest"], inleveren="z3_opa_bast")

Q("z3_s03_thee", "zij", "z3_theeguh", "Thee in het moeras",
  "Theeguh Kwak in Mosvoet brouwt honingthee met moerasbloemen. Daar krijg je snelle pootjes van. Pluk drie moerasbloemen in de "
  "Wortelvelden (ze vallen soms van de wezens in het moeras).",
  [D("verzamel", item("moerasbloem"), 3, "Verzamel 3 moerasbloemen van de wezens in de Wortelvelden", "gebied:z3_wortelvelden", 0.35,
     plek="gebied:z3_wortelvelden")],
  [("npc", "Kwak! Moerasbloemen, dat heb ik nodig. Drie. Voor de lekkerste thee van de boom."),
   ("npc", "De wezens in de Wortelvelden lopen er vaak mee rond. Op hun kop. Vraag ze maar. Met je zwaard.")],
  "Drie moerasbloemen uit de Wortelvelden.",
  [("npc", "Kwak kwak! Hier, proef maar. En kijk naar je pootjes. Zie je hoe snel ze willen?")],
  xp=7000, munten=5000, items=[(item("honingthee"), 3)], lv=42, na=["z3_hq11_wortel"])

Q("z3_s04_lichtjes", "zij", "z3_knoest", "Knoest z'n vrienden",
  "Langs de binnentrap, boven Honingtak, slapen nog vier lichtjes van het boomvolk. Zoek ze op verschillende hoogtes en maak ze wakker.",
  [D("geheim", "teller:z3_lichtjes", 4, "Vind 4 slapende boomvolk-lichtjes langs de binnentrap, net boven Honingtak (groene gloed)", plek="gebied:z3_schorsstad")],
  [("npc", "...Er slapen nog vrienden van mij in het hout. Vier. Ik voel ze. Hoog en laag."),
   ("npc", "Maak ze wakker, wil je? Een lichtje alleen is maar een lampje. Samen zijn we een stad.")],
  "Vier slapende lichtjes langs de binnentrap, boven Honingtak.",
  [("npc", "...Ze zijn wakker. Hoor je ze? Ze lachen. Ik ben vergeten hoe dat klonk. Dank je.")],
  xp=9000, munten=6000, lv=40, na=["z3_hq09_spiraal"])

Q("z3_s05_kaartjes", "zij", "z3_lusje", "Lusje's kaartjes",
  "Kaartenmaakster Lusje wil vijf uitkijkpunten op haar nieuwe kaart zetten. Maar dan moet er wel iemand echt gestaan hebben.",
  [D("gebied", "poi:z3_uitkijk_kroon", 1, "Sta op uitkijkpunt De Kroon, op kruin 2 aan de zuidoostkant", plek="poi:z3_uitkijk_kroon"),
   D("gebied", "poi:z3_uitkijk_takpunt", 1, "Sta op uitkijkpunt Takpunt, aan het eind van de Westtak", plek="poi:z3_uitkijk_takpunt"),
   D("gebied", "poi:z3_uitkijk_spiraal", 1, "Sta op het eerste balkon van het rijpad, aan de westkant van de stam", plek="poi:z3_uitkijk_spiraal"),
   D("gebied", "poi:z3_uitkijk_gewelf", 1, "Sta bovenop het Wortelgewelf, de grote ruïne in het noorden van de Wortelvelden", plek="poi:z3_uitkijk_gewelf"),
   D("gebied", "poi:z3_uitkijk_mast", 1, "Klim in het kraaiennest van de luchtschipmast van Honingtak", plek="poi:z3_uitkijk_mast")],
  [("npc", "Ik teken kaarten, maar ik kom nooit ergens. Ik hou niet van hoogtes. Raar, voor iemand die op een tak woont."),
   ("npc", "Vijf uitkijkpunten. Ga er staan en kijk goed. Dan teken ik ze erbij.")],
  "Vijf uitkijkpunten: De Kroon, Takpunt, het rijpad, het Wortelgewelf en de mast.",
  [("npc", "Alle vijf! Kijk, mijn kaart is af. Met een tekeningetje van jou erop. Op een Pluimguh. Heel klein.")],
  xp=10000, munten=8000, loot="zeldzaam", lv=36, na=["z3_hq07_hangnest"])

# ===== job advancement 2 at class level 35 (U-SPECS, ronde 4: was step 4 at lv 50) ===========================================
# In zone 3, after the first boss (the Kaaszeppelinwerf, z3_hq08_werf, lv 37-40) and the Pluimguh (z3_hq03_pluimstal). The
# ceremony + the nest touch: klassen.Advancement (step 2) + zone3.systemen.Advancement4 (ceremony touch on step 2). The title
# and the new power depend on the spec (lang vadsrijk.klassen.titel.<spec>.2).
KLASSEN = {
    "knuffelridder": "Knuffelridder",
    "pluisjager": "Pluisjager",
    "vadsmagier": "Vadsmagiër",
}
for k, naam in KLASSEN.items():
    Q(f"z3_adv35_{k}", "klasse", "z3_wasja", f"Het nest van de {naam}",
      f"Je tweede titel van De Knuffelorde. Bereik klasse-level 35 als {naam} en kom terug bij Ordemeester Wasja in de Nestkapel: "
      "dan wordt je specialisatie sterker.",
      [D("level", "*", 35, f"Bereik level 35 als {naam} (de Kruinzee, de werf en de premies helpen)", plek="npc:z3_wasja")],
      [("npc", "Je hebt de werf van de Mika's leeggeveegd, en je Pluimguh kent de boom al beter dan ik. Dat heb ik gezien."),
       ("npc", f"Wie zo ver komt, verdient de tweede titel van de Orde. Bereik level 35 als {naam}, dan dansen de imkers in de "
               "Nestkapel. Voor jou.")],
      f"Level 35 als {naam}, en dan terug naar Ordemeester Wasja in de Nestkapel.",
      [("npc", "De bijen zoemen, de bloesem valt, de imkers dansen. Je specialisatie groeit mee met de boom!")],
      xp=12000, munten=9000, lv=33, klasse=k, na=["z3_hq08_werf", "z3_hq03_pluimstal"], actie_klaar=f"advancement:{k}:2")

# ===== the secret: the Mika-imker (z3-bazen owns the boss: MikaImker.ontgrendel(p) / binding z3_imker) =====================
Q("z3_geheim_imker", "geheim", "z3_raatwever", "De grauwe honing",
  "Overal in Honingtak liggen sporen van een vreemde imker: kapotte rookpotjes, grauwe honing, een imkerpak aan de waslijn. Vind ze "
  "alle vijf, dan weet je waar hij zich verstopt.",
  [D("geheim", "teller:z3_aanwijzingen", 5, "Vind de vijf sporen van de vreemde imker in Honingtak (paarse glinsters)", plek="poi:z3_raattrossen"),
   D("meld", "z3_imker:verslagen", 1, "Versla de Mika-imker in zijn geheime raatzaal, achter het luik in de Raattrossen",
     plek="poi:z3_raattrossen")],
  [("npc", "...")],
  "Vijf sporen, en dan het geheime luik in de Raattrossen.",
  [("npc", "Een Mika! In een imkerpak! En de bijenkoningin is weer goud, zeg je? Dan gaat ze weer zingen!"),
   ("npc", "Dus DAT imkerpak aan de waslijn was van hem. Ik wist het. Nou ja, ik vond het raar. Dat is bijna weten."),
   ("npc", "Hier, een raatje van de koningin zelf. Ze stuurt haar dank. Met gezoem.")],
  xp=20000, munten=15000, loot="episch", lv=45, auto=True)

# ===== quest xp: z3-systemen's Z3Xp (a main step = 45 % of a level, a side quest = 18 %; same curve as kern Levels.xpVoor) =====
def z3xp(lv, hoofd):
    l = max(1, min(49, lv))
    stap = max(1, round(40 + 25 * l ** 1.6))
    return max(1, round(stap * (0.45 if hoofd else 0.18)))


for _q in QUESTS:
    _q["beloning"]["xp"] = z3xp(_q["lv"], _q["soort"] != "zij")


# ---------------------------------------------------------------------------------------------------------------------------
# Secrets: 10 knuffels, 5 clues to the Mika-imker, 5 treasures, 8 reliefs (Z3NpcPlekken.KNUFFEL/AANWIJZING/SCHAT/RELIEF)
# ---------------------------------------------------------------------------------------------------------------------------
KNUFFELS = {   # (Z3NpcPlekken.KNUFFEL: knuffel_1..10 = POI_Z3 geheim_knuffel_1..10)
    "knuffel_1": "Zweefje", "knuffel_2": "Bloesempje", "knuffel_3": "Hangbeer", "knuffel_4": "Kaaskopje", "knuffel_5": "Druppie",
    "knuffel_6": "Raddie", "knuffel_7": "Kevertje", "knuffel_8": "Kikkertje", "knuffel_9": "Lichtje", "knuffel_10": "Sapje",
}
AANWIJZINGEN = {
    "rookpotjes": ("Kapotte rookpotjes", "Naast het questbord liggen drie kapotte rookpotjes. Ze ruiken naar oude kaas. Imkers gebruiken nooit "
                                         "kaas in hun rook."),
    "imkerpak": ("Een vreemd imkerpak", "Op een loopplank van Honingtak hangt een imkerpak aan de waslijn. Met gaatjes voor oren. Puntige oren. En een staart."),
    "grauwe_honing": ("Grauwe honing", "In een potje op de markt zit grauwe honing. Het etiket zegt: 'Rijpe Kaas-honing. Niet voor guhs.'"),
    "koningin": ("Een stille koningin", "De bijenkoningin zingt niet meer. Ze zit grauw en stil in haar raat. Iets heeft haar verdrietig "
                                        "gemaakt."),
    "luik": ("Een geheim luik", "Onder in de Raattrossen zit een luik met een kattenpootje erop. Erachter zoemt het boos. En er piept een "
                                "stem: 'Miauw. Niet storen.'"),
}
SCHATTEN = {
    "onder_de_tak": ("Op de Zuidwesttak", "Helemaal aan het puntje van de Zuidwesttak hangt een kist in een nest. Hier kom je alleen als je durft!"),
    "nesttop": ("In het oude nest", "Tussen de takken van het oude nest, helemaal bovenop de boom, ligt een vergeten kist. Zo hoog kwam nog nooit iemand."),
    "schorsstad": ("Hoog in de Schorsstad", "In een huisje hoog in de stamwand staat een kist van het boomvolk. Er zit nog licht in."),
    "wortelduister": ("In de wortelhal", "Achter een wortelpilaar in de wortelhal, waar alleen de muisjes kwamen, ligt een kist."),
    "moeras": ("Aan de eilandrand", "Aan de oostrand van het eiland, bij het Honingmoeras, staat een kist. Plakkerig, maar vol."),
}
RELIEFS = {
    "relief_1": ("Het eerste reliëf", "Een draak, roze en groot, cirkelt boven een piepklein boompje. Het boompje groeit."),
    "relief_2": ("Het tweede reliëf", "De boom is gigantisch. In de kruin bouwt de draak een nest. Kleine houten wezentjes zwaaien."),
    "relief_3": ("Het derde reliëf", "Een ei, roze en gloeiend, in het nest. Het boomvolk danst eromheen met lantaarns."),
    "relief_4": ("Het vierde reliëf", "Een grauwe wolk komt uit het oosten. De draak vliegt erop af. Hij is heel boos. Of heel bang."),
    "relief_5": ("Het vijfde reliëf", "De draak komt terug. Grauw. Er steekt een splinter uit zijn borst. Hij gromt naar zijn eigen ei."),
    "relief_6": ("Het zesde reliëf", "Het boomvolk draagt het ei naar beneden, de stam in, tot diep onder de wortels."),
    "relief_7": ("Het zevende reliëf", "Het boomvolk zit in een kring en valt in slaap. Hun lichtjes worden zachter. Het ei gloeit."),
    "relief_8": ("Het achtste reliëf", "Een kat met een kroontje, ver weg aan de rand van de muur. Uit zijn kant komt grauwe mist, helemaal tot aan het nest. Zijn invloed reikt ver."),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Gids pages (questlog J, tab Gids: ids z3_* in QuestlogScherm.GIDS), tips, bestiary pages, other texts
# ---------------------------------------------------------------------------------------------------------------------------
GIDS = {
    "z3_schemerwortel": ("Zone 3: De Schemerwortel", "Na de Splinterpieken en vanaf level 30 vliegt het luchtschip je naar Honingtak, een "
                         "imkersstadje als een ring om een boom zo groot als een berg. De boom heeft 4 lagen: bovenin de Kruinzee "
                         "(3 kruinverdiepingen, lv 30-34) met het nest, dan de Takkenbanen met de imkerdorpjes (lv 34-38) en Honingtak, "
                         "in de holle stam de Schorsstad (lv 40-43), en onder de stam het Wortelduister (lv 46-49). Op de grond rond de "
                         "boom liggen de Wortelvelden (lv 43-46). De Kaaszeppelinwerf ligt op een eigen eiland."),
    "z3_pluimguh": ("Je Pluimguh", "Bij Stalmeester Veertje in de Pluimstal kies je je eerste rijdier: een Pluimguh. Roep hem met X, "
                    "open je rijdierscherm met N. Van hoog kun je met je Pluimguh omlaag zweven; omhoog gaat met trappen, liften en de "
                    "bloesemwind. Als je aanvalt of een baas je raakt, stap je af."),
    "z3_lantaarn": ("Licht in het donker", "In het Wortelduister en het Hart van de Boom is het pikdonker. Wezens buiten het licht zijn "
                    "donkere silhouetten en krijgen de helft minder schade. Je Nestlantaarn, roze aders en aangestoken grotlantaarns "
                    "geven licht. Samen zie je meer."),
    "z3_premiebord": ("Het premiebord", "In de Jagershut van Honingtak hangt het premiebord van Jagersguh Raap: elke dag nieuwe klusjes "
                      "voor level 40-50. Jagen, verzamelen, bezorgen, schimmel opruimen. Geen haast, geen ranglijst: doe wat je leuk "
                      "vindt."),
    "z3_reizen": ("Reizen in de boom", "Kapitein Zoemzeil vliegt je van de mast van Honingtak naar De Haven of Kandijkom. In de boom: het rijpad "
                  "draait buitenom de stam van de grond via Honingtak tot op kruin 1; binnenin loopt de binnentrap van de Schorsstad "
                  "tot het nest. De Honinglift gaat van de grond (Mosvoet) naar Honingtak, de Kruinlift van Honingtak naar kruin 1, "
                  "en de roze windkolommen blazen je van de takken naar kruin 1, 2 en 3. Omlaag zweef je met je Pluimguh."),
    "z3_titel": ("De vierde titel", "Op klasse-level 50 wacht je een vierde titel van De Knuffelorde, bij Ordemeester Wasja in de Nestkapel "
                 "van Honingtak: Nestwachter, Kruinvalk of Schemerwijze."),
}
TIPS = {
    "z3_monsters": ("Echte monsters", "De wezens in de boom zijn echte monsters. Versla ze gewoon. Guhs doe je nooit pijn."),
    "z3_lantaarn": ("Pikdonker!", "Blijf in het licht. Buiten het licht zijn wezens silhouetten en doen je klappen de helft minder schade."),
    "z3_zweef": ("Zweven!", "Op je Pluimguh kun je van hoge plekken naar beneden zweven. Spring van het zweefplatform of een balkon van het rijpad en "
                            "houd de springtoets ingedrukt."),
}
OVERIG = {
    # travel: the destination texts (optie.vlieg.schemerwortel, reis.slot.schemerwortel_*, reis.kies.z3_kapitein) are z3-wereld's
    P + "optie.rol.luchtschip_z3": "Waar vliegen we heen?",
    P + "optie.rol.premiebord": "Het premiebord bekijken",
    P + "optie.rol.honingkraam": "Wat heb je in De Honingkraam?",
    P + "optie.z3_jacht": "Het jachtbord bekijken",
    P + "optie.z3_penningwinkel": "Naar de penningwinkel",
    P + "optie.z3_gouden_veren": "25 Gouden Veren ruilen voor de Gouden Pluimguh",
    P + "optie.z3_advancement": "Ik ben klaar voor mijn tweede titel!",
    P + "optie.rol.honinglift": "Met de Honinglift",
    P + "optie.rol.honingthee": "Een kopje honingthee, graag!",
    P + "adv.knuffelridder.4": "Nestwachter", P + "adv.pluisjager.4": "Kruinvalk", P + "adv.vadsmagier.4": "Schemerwijze",
    # roles
    Z + "honinglift.dicht": "De Honinglift zit nog vast. Imkermoeder Raatje heeft de sleutel van het slot...",
    Z + "honinglift.open": "Ergens beneden ratelt een ketting. De Honinglift tussen Honingtak en Mosvoet doet het weer!",
    Z + "honinglift.af": "Instappen! Langzaam zakt de honingraatcabine langs de stam omlaag, naar Mosvoet.",
    Z + "honinglift.op": "Instappen! De honingraatcabine zwiept omhoog, langs de stam, naar Honingtak.",
    Z + "thee": "Alsjeblieft, een kopje honingthee. Pas op, heet. En zoet. Vooral zoet.",
    Z + "thee.al": "Je hebt er net al een gehad! Eerst opdrinken, dan krijg je er nog een.",
    # scenes
    Z + "scene.vlucht.titel": "Honingtak",
    Z + "scene.vlucht.onder": "De Schemerwortel",
    Z + "scene.opening.titel": "De Schemerwortel",
    Z + "scene.opening.onder": "Een boom zo groot als een berg",
    Z + "scene.schors": "Een wand van schors schuift langs het luchtschip. Hij houdt niet op. Hij gaat maar omhoog en omhoog...",
    Z + "scene.kruin": "Het schip breekt door de wolken. De kruin ligt in het goud.",
    Z + "scene.zwaaien": "Op een ringdek rond de stam zwaaien imkerguhs met hun sluiers. Honingtak!",
    Z + "scene.gekraak": "Diep in de wortels gaan twee grijsblauwe ogen open. Iets kraakt: \"Hij leeft nog... maar niet lang meer.\"",
    Z + "scene.aankomst": "Welkom in Honingtak! Imkermoeder Raatje wacht op het Bloesemplein, aan de zuidkant van het ringdek.",
    Z + "scene.bam.titel": "Operatie Rijpe Kaas",
    Z + "scene.bam.onder": "Een Mika-zeppelin boven de markt!",
    Z + "scene.bam.verstop": "De guhs schieten hun korfhuisjes in en doen de luikjes dicht. Niemand wordt geraakt!",
    Z + "scene.bam.pamflet": "Er dwarrelt een pamflet naar beneden. En uit de zeppelin springen Bladrovers!",
    Z + "bam.aanval": "Bladrovers op het Bloesemplein! Versla ze!",
    Z + "scene.nest.titel": "Lang geleden...",
    Z + "scene.nest.onder": "Het nest van Vahoegnir",
    Z + "scene.nest.leeg": "Het nest is enorm. Grauwe veren, en ertussen een paar roze. In het midden: een lege eierkuil.",
    Z + "scene.nest.vroeger": "Je ziet het voor je: Vahoegnir, nog roze, landt in zijn nest...",
    Z + "scene.nest.ei": "...en legt een gloeiend ei. Hij slaat zijn vleugels er beschermend omheen.",
    Z + "scene.f1.cape": "De cape valt in stukken. Graaf Piepvel staat in zijn hemdje: \"Mijn cape! Weet je wel hoeveel winters dat gekost heeft?!\"",
    Z + "scene.f1.kraak": "Het wordt koud. Door een kwastgat in de wand van de Pelszaal kruipt grauwe mist naar binnen.",
    Z + "scene.f1.nee": "\"Nee nee nee, Keizer, ik kan het uitleggen! Het was de ridder! Het was...\"",
    Z + "scene.f1.floep": "Uit de mist stappen twee Mika's. Ze pakken de Graaf bij zijn hemdje en trekken hem mee de mist in. Floep. Weg.",
    Z + "scene.f1.afdruk": "Stilte. Op de schors blijft een grauwe, koude rijpvlek achter. Zo groot als een deur.",
    Z + "scene.f1.piepje": "Piepje kruipt uit je tas en duwt iets glimmends naar je toe: de speld van de cape.",
    Z + "scene.ei.titel": "Het Drakenei",
    Z + "scene.ei.onder": "Warm, roze, en het klopt",
    Z + "scene.ei.warm": "Je pakt het ei op. Het is warm. Het klopje gaat even sneller.",
    # K-INT: Bleu's egg was a decoy; the Mika-imker secret ends in a lore scene (the boss itself is not built)
    Z + "scene.ei.nep": "Het ei in de kamer van Baron Bleu? Nep! Een kaaswiel met roze verf. Het echte ei lag hier al die tijd veilig.",
    Z + "imker.open": "Onder in de Raattrossen klinkt gezoem... en iemand die heel zachtjes \"au, au, au\" zegt.",
    Z + "imker.naam": "De Mika-imker",
    Z + "imker.scene.0": "In de raatzaal staat een Mika in een veel te groot imkerpak, met een rookpotje in zijn ene hand en een lepel in de andere.",
    Z + "imker.scene.1": "\"Niks aan de hand! Gewoon... honing proeven. Voor het rapport. Kwaliteitscontrole!\"",
    Z + "imker.scene.2": "\"Wat? Grauwe honing? Nee hoor, die is gewoon een beetje... rijp. Net als kaas. Mika's houden van rijp.\"",
    Z + "imker.scene.3": "De bijtjes zoemen boos om zijn hoofd. Het rookpotje sputtert, hoest één wolkje uit en gaat uit.",
    Z + "imker.scene.4": "\"Oké, oké! Ik ga al! Dit komt in mijn rapport!\"",
    Z + "imker.scene.5": "Hij vlucht met een volle pot honing door het luik. De pot laat hij in zijn haast achter. De bijtjes zijn weer blij.",
    Z + "scene.rit.titel": "Ontsnap!",
    Z + "scene.rit.onder": "De Wortelgang uit en het rijpad op",
    Z + "scene.rit.schudt": "De boom schudt! Takken vallen, schors scheurt. Vahoegnir slaat door de kruin!",
    Z + "scene.rit.zweef": "Rondje na rondje om de stam, langs Honingtak, door de bladtunnel. De draak scheert langs de schors!",
    Z + "scene.rit.nest": "De windkolommen blazen je omhoog, kruin na kruin. Het oude nest! Je legt het ei in de eierkuil. Daar landt hij: enorm, grauw, knetterend van zwarte pluis.",
    Z + "scene.finale.titel": "Opgevrolijkt!",
    Z + "scene.finale.onder": "Vahoegnir is weer roze",
    Z + "scene.finale.splinters": "Je houdt de laatste splinter omhoog. Alle vijf zweven ze samen tot één roze hartkristal, en glijden zijn borst in.",
    Z + "scene.finale.roze": "De zwarte pluis waait van hem af als paardenbloemzaad. Pluizige vleugels, guh-oortjes, roze. Hij is het weer.",
    Z + "scene.finale.ei": "Vahoegnir legt zijn kop om het nest. Een drakenei komt pas uit als de ouder het warm kan houden...",
    Z + "scene.finale.nies": "Krak! Er rolt een piepklein roze draakje uit. Het niest meteen.",
    Z + "scene.finale.giechel": "Vahoegnir giechelt. Een draak die giechelt laat de hele kruin trillen.",
    Z + "scene.finale.schouder": "Het draakje ziet jou als eerste, en kruipt op je schouder. Vahoegje!",
    Z + "scene.finale.straal": "Vahoegnir blaast een lange roze lichtstraal over de horizon, naar het oosten. Waar hij eindigt, zweeft een stad boven de wolken.",
    Z + "scene.finale.weg": "Hij kijkt je aan, buigt zijn kop, en vliegt de straal achterna.",
    Z + "bloei.titel": "De Schemerwortel bloeit weer",
    Z + "bloei.onder": "Vahoegnir is thuis",
    Z + "scene.cliff.oosten": "Ver in het oosten, waar de roze straal eindigde, zweeft een stad boven de wolken. Hoogvads.",
    Z + "scene.f2.titel": "Wordt vervolgd...",
    Z + "scene.f2.onder": "Ver in het oosten wil iets steeds meer.",
    Z + "scene.f2.zaal": "Ergens ver weg. Een grauwe zaal, mager licht. Graaf Piepvel knielt in zijn hemdje en rilt.",
    Z + "scene.f2.krabbel_1": "Een piepklein Mika'tje met een veel te grote ganzenveer leest voor: \"Rapport uit het westen.\"",
    Z + "scene.f2.krabbel_2": "\"De draak is... eh... roze.\"",
    Z + "scene.f2.krabbel_3": "\"Het ei is... eh... uit.\"",
    Z + "scene.f2.krabbel_4": "\"De Pink is... kaal.\"",
    Z + "scene.f2.tik": "Uit de schaduw achter de troon klinkt ongeduldig getik op een armleuning. Tik. Tik. Tik. Tik. Tik. In het tegenlicht: een kroontje, veel te klein.",
    Z + "scene.f2.stem": "Een trage stem: \"Dan... reik... ik... verder.\"",
    Z + "zone3.klaar": "De draak is weer roze en het ei is uit! De Schemerwortel bloeit weer. (Hoogvads komt binnenkort.)",
    # story moments outside scenes
    Z + "rit.titel": "Je eerste rit!",
    Z + "gids.start": "Stalmeester Veertje springt op haar Pluimguh: \"Volg mij maar! Het rijpad op, rondje na rondje om de stam, helemaal tot bovenop kruin 1!\"",
    Z + "rit.onder": "Jij en je Pluimguh",
    Z + "gezweefd": "Je zweeft! Je Pluimguh spreidt zijn verenoortjes en jullie dalen zachtjes.",
    Z + "bevel": "Aan de steiger hangt een Mika-bevel: \"Zoek het ei. Laat het rijpen. Breng het naar de Graaf. De Graaf brengt het naar Hem.\"",
    Z + "vlek.schoon": "Je Pluimguh schudt bloesem uit zijn pluim. De grauwe plek wordt weer groen en roze!",
    Z + "piepje.naam": "Piepje",
    Z + "piepje.0": "Tussen de raten zit een mager pieppiepmuisje. Het heeft geen winterpelsje meer, en het rilt.",
    Z + "piepje.1": "Piep... de Graaf... de kou... piep.",
    Z + "piepje.2": "Het muisje kruipt in je tas, rolt zich op en valt in slaap. Het wil niet meer weg.",
    Z + "ring.aan": "Ring %s van de Lantaarntoren brandt! Het kooitje springt open en de muisjes rennen piepend naar beneden.",
    Z + "nestlantaarn.0": "Halverwege, ridder. Hier, neem dit. De Nestlantaarn van het boomvolk.",
    Z + "nestlantaarn.1": "Onder de vloer is het pikdonker. In het licht van deze lantaarn kan niets zich verstoppen. Ook geen graaf.",
    Z + "ketel.uit": "Je draait de schimmelkraan dicht. De ketel sist, sputtert en valt stil. Het dal kleurt een beetje terug.",
    Z + "duister.klopje": "Onder de klop van het hart hoor je nog iets. Een tweede klopje. Zachter. Sneller. Het ei leeft nog!",
    Z + "vahoegnir.zonder": "Vahoegnir landt bij het nest, grauw en knetterend. Je houdt de splinters omhoog...",
    Z + "vingerafdruk": "Een grauwe rijpvlek in de schors, zo groot als een deur. Koud, en het ruikt naar niks. Professor Pluis zou hier een week naar willen kijken.",
    Z + "splinter.titel": "Hartsplinter %s van 5",
    Z + "splinter.nest": "Hij lag tussen de takken van zijn oude nest, bij zijn ei.",
    Z + "splinter.werf": "In een kistje met een strik: 'Voor de Graaf, voor zijn verzameling.'",
    Z + "splinter.cape": "De speld van de cape van Graaf Piepvel. De grootste splinter tot nu toe.",
    Z + "splinter.wachter": "\"Dit hield ik warm voor hem. Het hartje van zijn hart.\"",
    # secrets
    Z + "knuffel.gevonden": "Knuffel gevonden: %s! (%s/%s)",
    Z + "kuiken.gevonden": "Pluimkuiken gevonden! Het piept blij. (%s/%s)",
    Z + "lichtje.gevonden": "Een boomvolk-lichtje wordt wakker en knippert naar je. (%s/%s)",
    Z + "aanwijzing.naam": "Imkerspoor",
    Z + "aanwijzing.teller": "Imkersporen gevonden: %s van de %s. Raatwever Wasje wil dit vast weten!",
    Z + "aanwijzing.alle": "Alle vijf de sporen wijzen naar het luik onder in de Raattrossen. Het luik staat op een kier!",
    Z + "schat.titel": "Schat gevonden!",
    Z + "relief.naam": "Het Muurverhaal",
    Z + "relief.teller": "Reliëfs gevonden: %s van de %s.",
    Z + "relief.alle": "Alle acht reliëfs vertellen samen het verhaal van het nest en het ei. Het boomvolk wist alles. En het laatste reliëf... die mist, helemaal uit het oosten.",
    Z + "baas.roquefort": "Kapitein Roquefort", Z + "baas.kruinroofvogel": "De Kruinroofvogel", Z + "baas.piepvel": "Graaf Piepvel",
    Z + "baas.bleu": "Baron Bleu", Z + "baas.vahoegnir": "Vahoegnir, de Vadsloze Draak", Z + "baas.imker": "De Mika-imker",
    # the pet + the Verkenningsboek page
    "vadsrijk.huisdier.vahoegje.naam": "Vahoegje",
    "vadsrijk.voltooiing.zone.zone3": "De Schemerwortel",
    "vadsrijk.voltooiing.hint.rijdier.pluimguh": "Bij Stalmeester Veertje in de Pluimstal, op de grond aan de noordkant van de boom.",
    "gui.guhs.titels.naam.vr_kenner_van_de_schemerwortel": "Kenner van de Schemerwortel",
    "gui.guhs.titels.hint.vr_kenner_van_de_schemerwortel": "Verken De Schemerwortel helemaal (Verkenningsboek, toets B).",
}


# ---------------------------------------------------------------------------------------------------------------------------
# building
# ---------------------------------------------------------------------------------------------------------------------------
def _java_ids():
    """NPC/POI/area ids of Z3NpcPlekken.java (+ zone 1/2's NPCs that zone 3 quests use, such as the haven pilot)."""
    with open(os.path.join(JAVA, "Z3NpcPlekken.java"), encoding="utf-8") as f:
        src = f.read()
    npc = set(re.findall(r'\bnpc\("(z3_[a-z_0-9]+)"', src))
    if "z3_brug_" in npc:   # (P-VERHUIS) the bridge guhs are made in a loop: z3_brug_1..6
        npc.discard("z3_brug_")
        npc |= {f"z3_brug_{i}" for i in range(1, 7)}
    poi = set(re.findall(r'\bpoi\("(z3_[a-z_0-9]+)"', src))
    for lus, n in (("z3_ring_", 7), ("z3_grotlantaarn_", 4)):
        poi.discard(lus)
        poi |= {f"{lus}{i}" for i in range(1, n + 1)}
    gebied = set(re.findall(r'GEBIED_DOEL\.put\("([a-z_0-9]+)"', src))
    baas = set(re.findall(r'BAAS\.put\("([a-z_0-9]+)"', src))
    andere = set()
    for pad in (os.path.join(JAVA, "..", "..", "npc", "NpcPlekken.java"), os.path.join(JAVA, "..", "..", "zone2", "npc", "Z2NpcPlekken.java")):
        with open(pad, encoding="utf-8") as f:
            andere |= set(re.findall(r'NPC\.put\("([a-z_0-9]+)"', f.read()))
    andere |= {"luchtschipper", "z2_kapitein"}
    return npc, poi, gebied, baas, andere


def teksten():
    t = dict(OVERIG)
    t.update(PLEK_OVERIG)
    npc_ids, poi_ids, gebied_ids, baas_ids, _andere = _java_ids()
    for id, (naam, regels, waar) in NPCS.items():
        assert len(regels) == 3, id
        t[P + "naam." + id] = naam
        t[P + "plek.npc." + id] = waar
        for i, r in enumerate(regels, 1):
            t[P + f"praat.{id}.{i}"] = r
    assert set(NPCS) == npc_ids, f"NPC ids differ from Z3NpcPlekken: {set(NPCS) ^ npc_ids}"
    for id, x in PLEK_POI.items():
        t[P + "plek.poi." + id] = x
    assert set(PLEK_POI) == poi_ids, f"POI ids differ from Z3NpcPlekken: {set(PLEK_POI) ^ poi_ids}"
    for id, x in PLEK_GEBIED.items():
        t[P + "plek.gebied." + id] = x
    for id, x in PLEK_KAMPVUUR.items():
        t[P + "plek.kampvuur." + id] = x
    for id, x in PLEK_BAAS.items():
        t[P + "plek.baas." + id] = x
    assert gebied_ids <= set(PLEK_GEBIED) and baas_ids <= set(PLEK_BAAS), "area/boss without a plek text"
    for id, (naam, _tex) in QUEST_ITEMS.items():
        t[f"item.guhs.vr_z3_npc_{id}"] = naam
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
    for id, naam in KNUFFELS.items():
        t[Z + "knuffel." + id] = naam
    for id, (titel, tekst) in AANWIJZINGEN.items():
        t[Z + "aanwijzing." + id] = tekst
        t[Z + "aanwijzing.titel." + id] = titel
    for id, (naam, tekst) in SCHATTEN.items():
        t[Z + "schat." + id] = tekst
        t[Z + "schat.naam." + id] = naam
    for id, (titel, tekst) in RELIEFS.items():
        t[Z + "relief." + id] = tekst
        t[Z + "relief.titel." + id] = titel
    for id, (titel, tekst) in TIPS.items():
        t[P + "tip." + id] = tekst
        t[P + "tip." + id + ".titel"] = titel
    for id, (titel, tekst) in GIDS.items():
        t[P + "gids." + id + ".titel"] = titel
        t[P + "gids." + id + ".tekst"] = tekst
    for k, v in t.items():
        assert "%" not in v.replace("%s", ""), f"stray % in {k}"
    return t


_PLEK_SOORTEN = ("npc:", "poi:", "gebied:", "baas:")


# X-ZONE1: quest xp for a smooth chain (main-only reaches every next zone's entry level with ~0-15 min grinding; side quests
# are a small extra so main+side stays near the band; the level cap 70 falls in the last part of zone 6). Computed with
# tools/vadsrijk/xpbalans.py (see XP_BALANS.md, round X); only the xp of these quests changes.
X_ZONE1_XP = {
    "z3_hq01_boom": 3500, "z3_hq02_bezoek": 3500, "z3_hq03_pluimstal": 3500, "z3_hq04_vlekken": 3650, "z3_hq05_nest": 9000,
    "z3_hq06_voetjes": 10650, "z3_hq07_hangnest": 11150, "z3_hq08_werf": 12150, "z3_hq09_spiraal": 13250, "z3_hq10_piepvel": 13800,
    "z3_hq11_wortel": 14900, "z3_hq12_duister": 16600, "z3_hq13_hart": 7150, "z3_hq14_vahoegnir": 7350, "z3_hq15_hoogvads": 7350,
    "z3_s01_kuikens": 490, "z3_s02_opa_bast": 540, "z3_s03_thee": 790, "z3_s04_lichtjes": 730, "z3_s05_kaartjes": 620,
}

# 1101-balans: every zone-3 main ("hoofd") quest gets x0.8 on top of X_ZONE1_XP, rounded to 10; side, mount and class
# quests keep their xp.
BALANS_1101_HOOFD = 0.8
_BALANS_1101_BASIS = {}   # (base xp per quest, so calling quests_json() twice never compounds)


def quests_json():
    for _q in QUESTS:   # X-ZONE1: the quest xp above
        if _q["id"] in X_ZONE1_XP:
            _q["beloning"]["xp"] = X_ZONE1_XP[_q["id"]]
    for _q in QUESTS:   # 1101-balans: the factor above
        if _q["soort"] == "hoofd":
            basis = _BALANS_1101_BASIS.setdefault(_q["id"], _q["beloning"]["xp"])
            _q["beloning"]["xp"] = int(basis * BALANS_1101_HOOFD / 10 + 0.5) * 10
    npc_ids, poi_ids, gebied_ids, baas_ids, andere = _java_ids()
    alle_npc = npc_ids | andere
    ids = [q["id"] for q in QUESTS]
    assert len(ids) == len(set(ids)), "duplicate quest id"
    assert all(i.startswith("z3_") for i in ids), "zone-3 quest ids start with z3_"
    uit = []
    for q in QUESTS:
        for wie in (q["gever"], q["inleveren"]):
            assert wie in alle_npc, f"{q['id']}: unknown NPC {wie}"
        for na in q["na"]:
            assert na in ids or not na.startswith("z3_"), f"{q['id']}: unknown previous quest {na}"
        for d in q["doelen"]:
            # the zone-1 playtest lesson: every step says WHO/WHERE, and points somewhere
            assert d["plek"].startswith(_PLEK_SOORTEN), f"{q['id']}: objective without a plek: {d['tekst']}"
            soort, _, x = d["plek"].partition(":")
            assert x in {"npc": alle_npc, "poi": poi_ids, "gebied": set(PLEK_GEBIED), "baas": set(PLEK_BAAS)}[soort], \
                f"{q['id']}: unknown plek {d['plek']}"
            for y in (d["doel"], d["bron"]):
                if y.startswith("poi:"):
                    assert y[4:] in poi_ids, f"{q['id']}: unknown poi {y}"
            if d["soort"] in ("praat", "escorte"):
                assert d["doel"] in alle_npc, f"{q['id']}: unknown NPC {d['doel']}"
            if d["soort"] == "verzamel":
                assert d["doel"][len("guhs:vr_z3_npc_"):] in QUEST_ITEMS, f"{q['id']}: unknown quest item {d['doel']}"
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
    assert len(hoofd) == 15, len(hoofd)
    return {"_": "generated by tools/features/vadsrijk_z3_npc.py; do not edit", "quests": uit}


def advancements():
    namen = ([f"vr_{q['id']}" for q in QUESTS] + ["vr_z3_knuffels_alle", "vr_z3_aanwijzingen_alle", "vr_z3_reliefs_alle", "vr_z3_grauwe_brief"]
             + [f"vr_z3_schat_{s}" for s in SCHATTEN])
    return {f"advancement/quest/{n}.json": {"criteria": {"done": {"trigger": "minecraft:impossible"}}} for n in namen}


# ---- Vahoegje: a pink recolour of the dragon + a tiny animation set (idle / hop / sprong) ------------------------------------
def _kf(pairs):
    return {str(t): list(v) for t, v in pairs}


def vahoegje_anims():
    vl = lambda a: {"vleugel_l": {"rotation": _kf([(0, (0, 0, -a)), (0.25, (0, 0, a)), (0.5, (0, 0, -a))])},
                    "vleugel_r": {"rotation": _kf([(0, (0, 0, a)), (0.25, (0, 0, -a)), (0.5, (0, 0, a))])}}
    idle = {"vleugel_l": {"rotation": _kf([(0, (0, 0, -6)), (1.0, (0, 0, 8)), (2.0, (0, 0, -6))])},
            "vleugel_r": {"rotation": _kf([(0, (0, 0, 6)), (1.0, (0, 0, -8)), (2.0, (0, 0, 6))])},
            "staart1": {"rotation": _kf([(0, (0, -12, 0)), (1.0, (0, 12, 0)), (2.0, (0, -12, 0))])},
            "kop": {"rotation": _kf([(0, (0, 0, 0)), (0.6, (-6, 4, 0)), (1.4, (4, -4, 0)), (2.0, (0, 0, 0))])},
            "oor_l": {"rotation": _kf([(0, (0, 0, 0)), (1.8, (0, 0, -10)), (2.0, (0, 0, 0))])},
            "oor_r": {"rotation": _kf([(0, (0, 0, 0)), (1.8, (0, 0, 10)), (2.0, (0, 0, 0))])}}
    hop = dict(vl(25))
    hop["root"] = {"position": _kf([(0, (0, 0, 0)), (0.25, (0, 6, 0)), (0.5, (0, 0, 0))])}
    sprong = dict(vl(40))
    return {"idle": {"animation_length": 2.0, "loop": True, "bones": idle},
            "hop": {"animation_length": 0.5, "loop": True, "bones": hop},
            "sprong": {"animation_length": 0.5, "loop": True, "bones": sprong}}


def vahoegje_textuur(a):
    """The dragon texture shifted to soft pink (keeps the shading; the glow texture stays the dragon's own)."""
    from PIL import Image
    import colorsys
    bron = Image.open(os.path.join(a, "textures", "vadsrijk", "wezens", "draak.png")).convert("RGBA")
    px = bron.load()
    for y in range(bron.height):
        for x in range(bron.width):
            r, g, b, al = px[x, y]
            if al == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            l2 = min(0.92, 0.35 + l * 0.7)
            r2, g2, b2 = colorsys.hls_to_rgb(0.92, l2, max(0.45, min(0.85, s + 0.35)))
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), al)
    return bron


def _assets(w, a, png=None):
    for id, (_naam, tex) in QUEST_ITEMS.items():
        w(os.path.join(a, "items", f"vr_z3_npc_{id}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_z3_npc_{id}"}})
        w(os.path.join(a, "models", "item", f"vr_z3_npc_{id}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
    # K-INT: Vahoegje's animation + texture are z3-bazen's (vadsrijk_z3_bazen_modellen.py); one pet registration.


def build(h):
    h.w(f"{h.D}/vadsrijk/npc/quests_z3.json", quests_json())
    for pad, obj in advancements().items():
        h.w(f"{h.D}/{pad}", obj)
    _assets(h.w, h.A)
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


# ---------------------------------------------------------------------------------------------------------------------------
# Y-FTB: the FTB chapter "Het Vadsrijk" is the compact one of tools/features/vadsrijk_y_ftb.py now (rekruut, 6 zones, raid,
# secrets); this generator adds no FTB quests of its own any more. Its hidden advancements guhs:quest/vr_* stay (NpcFtb grants them).
# ---------------------------------------------------------------------------------------------------------------------------


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
    return (k.startswith(Z) or k.startswith("item.guhs.vr_z3_npc_") or (k.startswith(P) and ".z3_" in k and not k.startswith(P + "reis."))
            or k.startswith(P + "plek.overal.teller:z3_"))   # (K-INT: vadsrijk.npc.reis.* is z3-wereld's travel text)


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "vadsrijk", "npc", "quests_z3.json"), quests_json())
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
        data.update(t)
        _schrijf(p, data)
    print(f"vadsrijk_z3_npc: {len(QUESTS)} quests ({sum(q['soort'] == 'hoofd' for q in QUESTS)} main, "
          f"{sum(q['soort'] == 'zij' for q in QUESTS)} side), {len(NPCS)} NPCs, {len(t)} texts")


if __name__ == "__main__":
    _direct()
