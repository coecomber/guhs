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
        "aan de luchtschipmast op de westpunt van Honingtak, waar het luchtschip aanmeert"),
    "z3_raatje": ("Imkermoeder Raatje", [
        "Welkom in Honingtak, lieverd. Een stadje op een tak, en de tak aan een boom zo groot als een berg.",
        "Ik ben burgemeester, imker en oma van iedereen hier. Vooral oma.",
        "Honing, bloesem en een warme deken. Meer heeft een guh niet nodig. Nou ja, knuffels ook."],
        "op het Bloesemplein van Honingtak, bij het knuffelbeeld van de imkerguh"),
    "z3_wasja": ("Ordemeester Wasja", [
        "Dit is de Nestkapel, het filiaal van De Knuffelorde in de boom. Hier wissel je van klasse.",
        "De Orde heeft hier vroeger een eiersokkel neergezet. Leeg. Niemand weet meer waarvoor.",
        "Bijen dansen om de weg te wijzen. Ik dans om warm te blijven. Iedereen heeft zo zijn reden."],
        "in de Nestkapel, onderaan de Raattrossen onder Honingtak (neem de kapellift aan de zuidkant van het Bloesemplein omlaag)"),
    "z3_veertje": ("Stalmeester Veertje", [
        "Een Pluimguh borstelen is het fijnste werk van de wereld. Na Pluimguhs voeren. En Pluimguhs knuffelen.",
        "Rijden doe je met je knieën, je hart en een zakje Honingbrokjes.",
        "Hoor je dat getrappel? Dat is de oefenbaan. Of mijn maag. Het is bijna lunchtijd."],
        "in de Pluimstal aan de oostkant van Honingtak, waar de tak de kruin in gaat"),
    "z3_cafe": ("Cafébaas Zoet", [
        "Welkom in De Zoete Raat! Honingthee, honingkoek en honingijs. En voor de durfals: honing met honing.",
        "'s Avonds wordt er hier gedanst. Op de tafels. Niet aan mijn vrouw vertellen.",
        "Een kopje honingthee? Dat warmt je oortjes op."],
        "in café De Zoete Raat, in de Raattrossen onder de markt van Honingtak (de cafélift bij de markt gaat omlaag)"),
    "z3_jager": ("Jagersguh Raap", [
        "Op het premiebord hangen elke dag nieuwe klusjes. Monsters, schimmel, bezorgen. Pak maar wat je leuk vindt.",
        "Ik jaag niet op guhs, hoor. Alleen op wat de boom ziek maakt.",
        "Een premie per dag houdt de schimmel weg. En de verveling."],
        "in de Jagershut aan de westkant van Honingtak, een trap onder de takrug, bij het premiebord"),
    # --- shops ---------------------------------------------------------------------------------------------------------------------
    "z3_smid": ("Smidguh Hamerhoning", [
        "Mijn vuur brandt op honingwas. Ruikt lekker en gloeit roze.",
        "Sterren erop, verpluisd eraf. Hier wordt alles weer stevig.",
        "Ik smeed alleen niet op zondag. Dan bak ik wafels."],
        "in de smidse aan de noordflank van Honingtak, een trap onder de markt"),
    "z3_juwelier": ("Juweliersguh Glinsterhoning", [
        "Amberroze, honingkristal, wortelkristal... de boom zit vol mooie steentjes.",
        "Een sokje in je spullen? Ik zet er zo een kristal in. Klik, klaar.",
        "Hars is het geheugen van een boom. Daarom glinstert het zo."],
        "in de juwelierszaak aan de zuidflank van Honingtak, een trap onder de markt"),
    "z3_winkel": ("Winkelguh Bijenwas", [
        "Drankjes, verband en honingdropjes! Alles voor onderweg.",
        "Een drankje met een honingrandje werkt twee keer zo goed. Zegt mijn oma. Mijn oma is Raatje.",
        "Zoem zoem, kassa!"],
        "in de drankjeswinkel aan de noordflank van Honingtak, een trap onder het Bloesemplein"),
    "z3_wapens": ("Wapenhandelaar Angeltje", [
        "Takkenknotsen, liaanbogen, bloesemstaven. Alles geplukt uit de boom. Met toestemming!",
        "Puntig als een bijenangel. Maar dan lief.",
        "Kies je wapen met je hart. En met je klasse."],
        "in de uitrustingswinkel aan de zuidflank van Honingtak, een trap onder het Bloesemplein"),
    "z3_kleermaker": ("Kleermaker Sluier", [
        "Een imkerhoed met een sluier? Staat iedereen. Zelfs Pluimguhs.",
        "Ik naai alles met zijdedraad van de rupsen hier. Ze vinden het leuk om te helpen.",
        "Strepen maken dun. Daarom draagt de hele stad strepen. Grapje. Het is gewoon gezellig."],
        "in de winkel aan de noordflank van Honingtak, ten westen van het Bloesemplein"),
    "z3_markt": ("Marktguh Zoemsel", [
        "Kopen, verkopen, ruilen! De zoemende markt is altijd open.",
        "Vandaag in de aanbieding: niks. Morgen ook. Kijk zelf maar tussen de kraampjes.",
        "Hier ruil je alles met andere avonturiers. Eerlijk is eerlijk, zegt de bij."],
        "op de zoemende markt, ten oosten van het Bloesemplein van Honingtak"),
    "z3_stalguh": ("Stalguh Hooibaal", [
        "Zadels, kleurtjes en voer. Kijk maar wat je Pluimguh mooi vindt.",
        "Ik ruim de stal op. Elke dag. Pluimguhs verliezen veel veertjes. Heel veel.",
        "Een Pluimguh die niest, laat een wolk dons achter. Ik hou van dat werk."],
        "in de Pluimstal aan de oostkant van Honingtak, bij de voerzolder"),
    "z3_liftguh": ("Liftguh Katrolletje", [
        "De Honinglift gaat helemaal naar beneden, naar Mosvoet. Eén touw, één emmer, veel moed.",
        "Niet naar beneden kijken. Of juist wel. Het uitzicht is prachtig.",
        "Ik draai de slinger al dertig jaar. Mijn linkerarm is heel sterk."],
        "bij het bovenstation van de Honinglift, aan de oostkant van de takrug van Honingtak"),
    # --- citizens ----------------------------------------------------------------------------------------------------------------
    "z3_opa_bast": ("Opa Bast", [
        "Toen ik jong was, klom ik elke dag naar de Kruintop. Nu klim ik elke dag naar mijn stoel.",
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
        "bij de korfhuisjes aan de zuidflank van Honingtak, ten westen van het Bloesemplein"),
    "z3_bloesem": ("Bloesemguh Roosje", [
        "Het bloesemboompje op het plein groeit uit de tak zelf. Is dat niet prachtig?",
        "Als het waait, regent het hier bloemblaadjes. Dan dans ik.",
        "Ruik eens. Dat is lente. Elke dag lente."],
        "bij het bloesemboompje " + _PL),
    "z3_druppel": ("Danseres Druppel", [
        "Vanavond dansen we in De Zoete Raat! Kom je ook?",
        "Een honingdruppel valt heel langzaam. Zo dans ik ook. Heel langzaam. En plakkerig.",
        "Ik heb ooit met een Pluimguh gedanst. Hij trapte op mijn teen. Allebei mijn tenen."],
        "in café De Zoete Raat, in de Raattrossen onder het Bloesemplein van Honingtak"),
    "z3_lampion": ("Lampionguh Gloed", [
        "'s Avonds steek ik alle lampions aan. Allemaal tweehonderdzevenentwintig.",
        "Als de lichtkevers komen, hoef ik bijna niks meer te doen.",
        "Licht houdt de grauwigheid weg. Dat weet iedereen hier."],
        "bij de korfhuisjes aan de zuidflank van Honingtak, onder het Bloesemplein"),
    "z3_imker_1": ("Imker Zoemzang", [
        "Mijn bijen zingen 's ochtends. Ik zing mee. De buren vinden het... bijzonder.",
        "Honing oogsten is een kwestie van geduld. En een sluier. Vooral een sluier.",
        "Hoor je dat? Zo klinkt een tevreden korf."],
        "bij de korfhuisjes aan de zuidoostflank van Honingtak"),
    "z3_imker_2": ("Imker Pollie", [
        "Sinds de Mika-zeppelins komen, zingt de bijenkoningin niet meer. Ik maak me zorgen.",
        "Grauwe honing! In mijn korf! Wie doet nou zoiets?",
        "Een imker zonder bijen is als een boom zonder bladeren. Heel kaal."],
        "bij de korfhuisjes aan de noordflank van Honingtak, onder de markt"),
    "z3_imker_3": ("Imker Honingdauw", [
        "Ik heb ooit een honingraat zo groot als een deur gevonden. Ik woon er nu in.",
        "Wist je dat bijen kunnen tellen? Tot vier. Dan beginnen ze opnieuw.",
        "Een goede imker heeft altijd een snoepje in zijn zak. Voor de bijen. En voor zichzelf."],
        "bij de korfhuisjes aan de noordflank van Honingtak, onder het Bloesemplein"),
    "z3_kuikenhoeder": ("Kuikenhoeder Pip", [
        "Pluimkuikens zijn zó schattig. En zó snel weg. Waar zijn ze nu weer?",
        "Een Pluimkuiken volgt alles wat beweegt. Vooral rijdieren.",
        "Als ze groot zijn, kun je erop rijden. Nu passen ze in een theekopje."],
        "bij de oefenbaan van de Pluimstal aan de oostkant van Honingtak"),
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
        "Hier vlakbij ligt de Kruintop. Daar staat iets heel groots. Een nest."],
        "bij het kampvuur in het noorden van de Kruinzee, tussen de Kruintop en de Mika-zeppelinsteiger"),
    "z3_lusje": ("Kaartenmaakster Lusje", [
        "Kaarten tekenen van een boom is moeilijk. Alles loopt rond. En omhoog. En omlaag.",
        "Hangnest Dauwdrup hangt onder de tak. Je went eraan. Na een week of drie.",
        "Mijn thee is weer koud. Komt door de schrik. Of door mij. Ik vergeet hem altijd."],
        "in Hangnest Dauwdrup, dat helemaal in het oosten onder de lange tak hangt (rijd de Takbaan vanaf de Pluimstal)"),
    "z3_herbergier": ("Herbergier Dauw", [
        "Welkom in de herberg van Dauwdrup! Bedden van mos, soep van bloesem.",
        "Bij ons schommelt de vloer. Dat hoort zo. We hangen.",
        "Drankjes en verband heb ik ook. Een hangnest moet alles in huis hebben."],
        "in de herberg van Hangnest Dauwdrup, helemaal in het oosten onder de lange tak"),
    "z3_knoest": ("Oude Knoest", [
        "...Ik ben het laatste lichtje van het boomvolk. De anderen slapen in het hout.",
        "Wij bouwden deze stad in de stam. Ringen en ringen en ringen. Lang geleden.",
        "Licht is warmte. Warmte is leven. Zo simpel is dat. Vroeger."],
        "binnen in de holle stam, bij de middelste poort van de Stamspiraal (de Schorsstad)"),
    "z3_wortelien": ("Burgemeester Wortelien", [
        "Welkom in Mosvoet! We wonen in een holle wortelknol. Lekker knus, en altijd koel.",
        "Ons waterrad draait op sap. Het ruikt naar ahornsiroop. Iedereen hier heeft altijd honger.",
        "De wortels rotten. Dat merken we aan alles. Zelfs de soep smaakt grauw."],
        "in Mosvoet, het gehucht in de holle wortelknol in het zuidwesten van de Wortelvelden (neem de Honinglift omlaag)"),
    "z3_molenaar": ("Molenaar Rad", [
        "Mijn waterrad draait op boomsap. Langzaam, maar het draait.",
        "Als de schimmel komt, loopt het vast. Dan duw ik. Hard.",
        "Een rondje draaien is goed voor iedereen. Ook voor een rad."],
        "bij het waterrad van Mosvoet, in het zuidwesten van de Wortelvelden"),
    "z3_theeguh": ("Theeguh Kwak", [
        "Honingthee met moerasbloemen. Daar krijg je snelle pootjes van!",
        "Ik ben een kikkerguh. Ik woon graag bij het water. En bij de thee.",
        "Kwak. Dat betekent 'proost'."],
        "in het theehuis van Mosvoet, in het zuidwesten van de Wortelvelden"),
    "z3_gloei": ("Wortelwacht Gloei", [
        "Hier beneden is het donker. Blijf in het licht, dan doen de monsters minder pijn.",
        "Hoor je dat? Die klop? Dat is het hart van de boom.",
        "Mijn lampje is mijn beste vriend. Na mijn moeder."],
        "bij het kampvuur in de Hartslag-zaal, diep in het Wortelduister (via de Grote Wortelpoort in het zuiden)"),
    "z3_nestwachter": ("De Oude Nestwachter", [
        "...Ik hield het ei warm. Heel lang. Ik was het bijna vergeten, waarom.",
        "Het boomvolk droeg het ei naar beneden. Ik bleef. Iemand moest blijven.",
        "Nu ben ik weer roze. Dat voelt... fijn. Dank je."],
        "in de Nestkamer, helemaal achterin het Hart van de Boom (de kerker onder de stam)"),
}
# the Honingwacht (Z3NpcPlekken z3_wacht_*): name, where; they share their lines (like the Kristalwacht of zone 2)
WACHTEN = {
    "z3_wacht_plein_1": ("Zoemwacht", "aan de westkant van het Bloesemplein van Honingtak"),
    "z3_wacht_plein_2": ("Raatwacht", "aan de oostkant van het Bloesemplein van Honingtak"),
    "z3_wacht_kruin_1": ("Bloesemwacht", "bij het kampvuur op de Kruinring, vlak achter de Pluimstal"),
    "z3_wacht_kruin_2": ("Kroonwacht", "bij het kampvuur naast het oude nest op de Kruintop"),
    "z3_wacht_tak_1": ("Takwacht", "bij het kampvuur op de Takbaan, waar de lange tak in het oosten uit de kruin komt"),
    "z3_wacht_tak_2": ("Liaanwacht", "bij het kampvuur op de noordoosttak, op weg naar de Kaaszeppelinwerf"),
    "z3_wacht_spiraal": ("Schorswacht", "bij het kampvuur op ring 4 van de Schorsstad, binnen in de stam"),
    "z3_wacht_wortel_1": ("Keverwacht", "bij het kampvuur in het Lichtkeverbos, in het noorden van de Wortelvelden"),
    "z3_wacht_wortel_2": ("Rotwacht", "bij het kampvuur aan de oostrand van de Wortelvelden"),
    "z3_wacht_wortel_3": ("Paddenwacht", "bij het kampvuur in het Paddenbloesemdal, in het zuidoosten van de Wortelvelden"),
    "z3_wacht_wortel_4": ("Poortwacht", "bij het kampvuur op de Wortelweg bij Mosvoet, in het zuidwesten van de Wortelvelden"),
    "z3_wacht_gewelf": ("Gewelfwacht", "bij het kampvuur onder het Wortelgewelf, in het westen van de Wortelvelden"),
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
    "z3_plein": "midden op het Bloesemplein van Honingtak",
    "z3_dok": "aan de luchtschipmast op de westpunt van Honingtak",
    "z3_pluimstal": "in de Pluimstal aan de oostkant van Honingtak",
    "z3_zweefplatform": "op het zweefplatform achter de oefenbaan van de Pluimstal (aan de oostkant van Honingtak)",
    "z3_nestkapel_balkon": "bij de Nestkapel, onderaan de Raattrossen van Honingtak (de kapellift aan de zuidkant van het plein)",
    "z3_raattrossen": "in de Raattrossen onder de westkant van Honingtak (neem de raattroslift omlaag)",
    "z3_vlek_1": "bij de eerste grauwe plek in de Kruinzee, ten zuidoosten van de Pluimstal",
    "z3_vlek_2": "bij de tweede grauwe plek, in het zuiden van de Kruinzee",
    "z3_vlek_3": "bij de derde grauwe plek, in het noorden van de Kruinzee",
    "z3_zeppelinsteiger": "bij de Mika-zeppelinsteiger in het noordoosten van de Kruinzee (de grauwe spuitinstallatie)",
    "z3_kruintop": "op de Kruintop, het hoogste punt van de boom, bij het oude nest (volg de Kruinring naar het midden van de kruin)",
    "z3_nestsplinter": "tussen de takken van het oude nest op de Kruintop (hij gloeit roze als je dichtbij komt)",
    "z3_kale_kruin": "op de Kale Kruin, de dode kruintop naast de Kruintop",
    "z3_hangnest": "in Hangnest Dauwdrup, dat helemaal in het oosten onder de lange tak hangt (rijd de Takbaan vanaf de Pluimstal)",
    "z3_kaaspost_1": "bij de eerste Kaasschutter-post op de lange tak naar Hangnest, vlak achter de kruin",
    "z3_kaaspost_2": "bij de tweede Kaasschutter-post, op de tak naar de Kaaszeppelinwerf (noordoost)",
    "z3_kaaspost_3": "bij de derde Kaasschutter-post, op de zuidoosttak",
    "z3_werf": "bij de ingang van de Kaaszeppelinwerf, aan de punt van de noordoosttak",
    "z3_kajuit": "in de kajuit van Kapitein Roquefort, op het dek van de vlaggenzeppelin achterin de Kaaszeppelinwerf",
    "z3_spiraalpoort": "bij de middelste poort van de Stamspiraal, halverwege de stam (rijd de spiraal omlaag vanaf de kruin)",
    "z3_lantaarntoren": "in de Lantaarntoren, midden in de holle stam van de Schorsstad (begin onderaan bij ring 1)",
    "z3_ring_1": "bij ring 1 van de Lantaarntoren, onderaan",
    "z3_ring_2": "bij ring 2 van de Lantaarntoren",
    "z3_ring_3": "bij ring 3 van de Lantaarntoren",
    "z3_ring_4": "bij ring 4 van de Lantaarntoren, halverwege",
    "z3_ring_5": "bij ring 5 van de Lantaarntoren",
    "z3_ring_6": "bij ring 6 van de Lantaarntoren",
    "z3_ring_7": "bij ring 7 van de Lantaarntoren, net onder de Pelszaal",
    "z3_pelszaal": "in de Pelszaal, de kroon van de Lantaarntoren bovenin de holle stam",
    "z3_capespeld": "op de vloer van de Pelszaal, bij de troon van kaaskorsten",
    "z3_vingerafdruk": "op de binnenwand van de stam, tegenover de Pelszaal",
    "z3_mosvoet": "in Mosvoet, in het zuidwesten van de Wortelvelden",
    "z3_ketel_1": "bij de eerste schimmelketel van de Mika-schimmelboerderij, in het noordoosten van de Wortelvelden (volg het piepen van het muisje)",
    "z3_ketel_2": "bij de tweede schimmelketel van de Mika-schimmelboerderij, in het noordoosten van de Wortelvelden",
    "z3_wortelpoort": "bij de Grote Wortelpoort in het zuiden van de Wortelvelden, recht onder de stam",
    "z3_hartslagzaal": "in de Hartslag-zaal, aan het eind van de Grote Wortelgang (volg de roze aders omlaag)",
    "z3_grotlantaarn_1": "bij een grotlantaarn links voor de poort van het Hart",
    "z3_grotlantaarn_2": "bij een grotlantaarn rechts voor de poort van het Hart",
    "z3_grotlantaarn_3": "bij een grotlantaarn links naast de poort van het Hart",
    "z3_grotlantaarn_4": "bij een grotlantaarn rechts naast de poort van het Hart",
    "z3_hartpoort": "bij de poort van het Hart van de Boom, in de Hartslag-zaal",
    "z3_nestkamer": "in de Nestkamer, helemaal achterin het Hart van de Boom",
    "z3_uitkijk_kroon": "op uitkijkpunt De Kroon, op de Kruinring in het zuidoosten van de Kruinzee",
    "z3_uitkijk_takpunt": "op uitkijkpunt Takpunt, aan het eind van de zuidoosttak",
    "z3_uitkijk_spiraal": "op het bovenste balkon van de Stamspiraal, aan de westkant van de stam",
    "z3_uitkijk_gewelf": "bovenop het Wortelgewelf, in het westen van de Wortelvelden",
    "z3_uitkijk_mast": "in het kraaiennest van de luchtschipmast van Honingtak",
}
PLEK_GEBIED = {
    "z3_honingtak": "in Honingtak, het stadje op de westtak van de boom",
    "z3_kruinzee": "in de Kruinzee, de bladerkruin boven op de boom",
    "z3_takkenbanen": "op de Takkenbanen, de reuzentakken in het oosten",
    "z3_zeppelinwerf": "in de Kaaszeppelinwerf, aan de punt van de noordoosttak",
    "z3_schorsstad": "in de Schorsstad, in de holle stam",
    "z3_wortelvelden": "in de Wortelvelden, op de grond rond de boom",
    "z3_wortelduister": "in het Wortelduister, de grotten onder de boom",
    "z3_hart": "in het Hart van de Boom, onder de stam",
    "z3_ring": "in De Schemerwortel",
}
PLEK_KAMPVUUR = {
    "z3_honingtak": _PL,
    "z3_kruinzee": "in het noorden van de Kruinzee, bij Kruinwachter Blad",
    "z3_takkenbanen": "op de Takbaan, waar de lange tak in het oosten uit de kruin komt",
    "z3_schorsstad": "op ring 4 van de Schorsstad, binnen in de stam",
    "z3_wortelvelden": "in Mosvoet, in het zuidwesten van de Wortelvelden",
    "z3_wortelduister": "in de Hartslag-zaal, bij Wortelwacht Gloei",
}
PLEK_BAAS = {
    "z3_zeppelinwerf": "in de Kaaszeppelinwerf aan de punt van de noordoosttak (de bazen zitten op de steigers en het dek)",
    "z3_pelszaal": "in de Pelszaal bovenin de Lantaarntoren, midden in de holle stam",
    "z3_hart": "in het Hart van de Boom, achter de poort in de Hartslag-zaal",
    "z3_kruintop": "bij het oude nest op de Kruintop",
    "z3_kale_kruin": "op de Kale Kruin, naast de Kruintop",
}
PLEK_OVERIG = {
    P + "plek.overal.teller:z3_knuffels": "verstopt in De Schemerwortel: in de kruin, op de takken, in de stam en tussen de wortels (hartjes)",
    P + "plek.overal.teller:z3_aanwijzingen": "in Honingtak: op de takrug, bij het huis van Raatje en in de Raattrossen (paarse glinsters)",
    P + "plek.overal.teller:z3_schatten": "op geheime plekjes in De Schemerwortel",
    P + "plek.overal.teller:z3_kuikens": "in de Kruinzee, bij de honingvijvers (roze dons)",
    P + "plek.overal.teller:z3_lichtjes": "in de wand van de Schorsstad, op verschillende hoogtes (groene gloed)",
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
Q("z3_hq01_boom", "hoofd", "z2_kapitein", "Een boom zo groot als een berg",
  "Ordemeester Kristalijn zag het in het oosten: een boom zo groot als een berg. Kapitein Poolster in Kandijkom vliegt er nu "
  "naartoe: naar Honingtak, een imkersstadje op een tak van die boom. Daar wacht Imkermoeder Raatje.",
  [D("meld", "z3:aangekomen", 1, "Vlieg met Kapitein Poolster vanuit Kandijkom naar Honingtak (kies Honingtak bij de luchtschipsteiger)",
     plek="npc:z2_kapitein"),
   D("praat", "z3_raatje", 1, "Praat met Imkermoeder Raatje bij haar korfhuisje, een trapje onder het Bloesemplein van Honingtak",
     plek="npc:z3_raatje")],
  [("npc", "Avonturier! Kristalijn vertelde het me. Een boom, zegt ze. In het oosten. Zo groot als een berg."),
   ("npc", "Ik vlieg er sinds vanochtend naartoe. Er hangt een stadje aan een van de takken: Honingtak. Vol imkers en bijen."),
   ("jij", "Een stadje... aan een tak?"),
   ("npc", "Ja. Niet naar beneden kijken. Stap in, het is een lange vlucht. En hij gaat omhoog. Heel erg omhoog.")],
  "Stap in het luchtschip naar Honingtak. Daar wacht Imkermoeder Raatje.",
  [("npc", "Daar ben je, lieverd! Wat een vlucht, hè? Iedereen kijkt altijd zo verbaasd als ze de boom voor het eerst zien."),
   ("npc", "Ik ben Raatje. Burgemeester, imker en oma van iedereen hier. Welkom in Honingtak."),
   ("jij", "In de wortels... ik zag twee ogen. Grijsblauw. En ik hoorde iets kraken."),
   ("npc", "Ogen? In de wortels? Brrr. Dat moet Ordemeester Wasja horen. Ze zit in de Nestkapel, onderaan de Raattrossen. Neem de "
           "kapellift aan de zuidkant van het plein omlaag.")],
  xp=4000, munten=3000, items=[(item("honingthee"), 2)], lv=30, na=["z2_hq12_hartsplinter"], inleveren="z3_raatje",
  actie_aanname="z3_vlieg:honingtak")

Q("z3_hq02_bezoek", "hoofd", "z3_wasja", "Bezoek uit de lucht",
  "Ordemeester Wasja van de Nestkapel denkt dat deze boom ooit het nest van Vahoegnir was. Maar dan zoemt er een Mika-zeppelin over "
  "de markt. Versla de Bladrovers die hij op het Bloesemplein laat vallen.",
  [D("opvrolijk", "*", 8, "Versla 8 Bladrovers die de Mika-zeppelin op het Bloesemplein van Honingtak laat vallen", plek="poi:z3_plein"),
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
   ("npc", "Wat je nu nodig hebt, is een rijdier. Deze boom is te groot om te lopen. Ga naar Stalmeester Veertje in de Pluimstal, "
           "aan de oostkant van de stad.")],
  xp=5000, munten=3500, loot="zeldzaam", lv=30, na=["z3_hq01_boom"], inleveren="z3_raatje", actie_aanname="z3_bam")

Q("z3_hq03_pluimstal", "hoofd", "z3_veertje", "De Pluimstal",
  "Stalmeester Veertje heeft drie jonge Pluimguhs. Kies er één, geef hem een naam, en maak je eerste rit naar het zweefplatform. "
  "Spring eraf en zweef naar beneden: je eerste Pluimzweef.",
  [D("meld", "rijdier:starter", 1, "Kies je eigen Pluimguh bij Stalmeester Veertje in de Pluimstal (aan de oostkant van Honingtak)",
     plek="npc:z3_veertje"),
   D("meld", "z3:opgestegen", 1, "Roep je Pluimguh (toets X) en stap op, bij de Pluimstal", plek="npc:z3_veertje"),
   D("gebied", "poi:z3_zweefplatform", 1, "Rijd naar het zweefplatform achter de oefenbaan van de Pluimstal", plek="poi:z3_zweefplatform"),
   D("meld", "z3:gezweefd", 1, "Spring met je Pluimguh van het zweefplatform en zweef naar beneden (houd springen ingedrukt)",
     plek="poi:z3_zweefplatform")],
  [("npc", "Een ridder zonder rijdier in een boom van deze maat? Dan loop je hier tot volgend voorjaar."),
   ("npc", "Kijk, daar in de box. Drie jonge Pluimguhs. Roze, munt en lavendel. Lange pootjes, donzig lijfje, een pluimstaart."),
   ("jij", "Wacht eens... grote gele vogels waar je op rijdt... dat ken ik ergens van..."),
   ("npc", "Nee, het is géén choco-dinges uit een of ander Fantasy-spel. Dit is een Pluimguh. Kijk maar naar de oortjes, njeg!"),
   ("npc", "Fluit maar op de Stalfluit. Ze komen alle drie aangerend en laten zien wat ze kunnen. Kies er één. Hij kiest jou ook, "
           "hoor. Dat zie je aan hoe hij snuffelt.")],
  "Kies je Pluimguh, stap op, en zweef van het zweefplatform naar beneden.",
  [("npc", "Zag je dat? Jij en je Pluimguh, door de lucht! Je eerste Pluimzweef!"),
   ("npc", "Vanaf hoog kun je altijd zweven. Alleen omlaag, hoor. Omhoog gaat met trappen, liften en de bloesemwind."),
   ("npc", "Zie je die grauwe plekken in de kruin? Die zijn van de zeppelins. Kruinwachter Blad heeft hulp nodig. Ik vertel je zo waar.")],
  xp=5000, munten=3000, lv=30, na=["z3_hq02_bezoek"], actie_aanname="z3_starter")

Q("z3_hq04_vlekken", "hoofd", "z3_veertje", "Grauwe vlekken",
  "Mika-zeppelins hebben de Kruinzee grauw gespoten. Rijd langs de drie grauwe plekken en maak ze schoon, versla de wezens in de "
  "kruin en kijk bij de Mika-zeppelinsteiger wat ze van plan zijn. Kruinwachter Blad wacht bij zijn kampvuur.",
  [D("gebied", "poi:z3_vlek_1", 1, "Maak de eerste grauwe plek schoon, in de Kruinzee ten zuidoosten van de Pluimstal", plek="poi:z3_vlek_1"),
   D("gebied", "poi:z3_vlek_2", 1, "Maak de tweede grauwe plek schoon, in het zuiden van de Kruinzee", plek="poi:z3_vlek_2"),
   D("gebied", "poi:z3_vlek_3", 1, "Maak de derde grauwe plek schoon, in het noorden van de Kruinzee", plek="poi:z3_vlek_3"),
   D("opvrolijk", "gebied:z3_kruinzee", 12, "Versla 12 wezens in de Kruinzee", plek="gebied:z3_kruinzee"),
   D("gebied", "poi:z3_zeppelinsteiger", 1, "Zoek bij de Mika-zeppelinsteiger in het noordoosten van de Kruinzee naar hun plannen",
     plek="poi:z3_zeppelinsteiger")],
  [("npc", "Kruinwachter Blad bewaakt de Kruinzee. Hij zit bij zijn kampvuur in het noorden, niet ver van de Mika-zeppelinsteiger."),
   ("npc", "Maak onderweg de grauwe plekken schoon. Je hoeft er alleen maar langs te rijden: je Pluimguh schudt bloesem uit zijn "
           "pluim. Echt waar. Daar is hij voor gefokt.")],
  "Drie grauwe plekken, twaalf wezens, en een kijkje bij de zeppelinsteiger. Daarna naar Kruinwachter Blad.",
  [("npc", "Je hebt de kruin schoongemaakt! De bladeren worden al weer groen. Wat vond je bij de steiger?"),
   ("jij", "Een bevel. 'Zoek het ei. Laat het rijpen. Breng het naar de Graaf. De Graaf brengt het naar Hem.'"),
   ("npc", "Hem? Met een hoofdletter? Wie is dat dan?"),
   ("jij", "Geen idee. Niemand weet het."),
   ("npc", "Een ei... Op de Kruintop, hier vlakbij, ligt een oud nest. Enorm. Ik durf er nooit dichtbij. Jij wel, denk ik.")],
  xp=6000, munten=4000, loot="zeldzaam", lv=31, na=["z3_hq03_pluimstal"], inleveren="z3_bladwachter")

Q("z3_hq05_nest", "hoofd", "z3_bladwachter", "Het oude nest",
  "Op de Kruintop, het hoogste punt van de boom, ligt een reusachtig oud nest. Rijd erheen en kijk wat er nog van over is.",
  [D("gebied", "poi:z3_kruintop", 1, "Rijd over de Kruinring naar het oude nest op de Kruintop", plek="poi:z3_kruintop"),
   D("meld", "z3:nest_herinnering", 1, "Kijk rond in het oude nest op de Kruintop", plek="poi:z3_kruintop"),
   D("gebied", "poi:z3_nestsplinter", 1, "Zoek het roze gloeiende ding tussen de takken van het nest", plek="poi:z3_nestsplinter")],
  [("npc", "Volg de Kruinring naar het midden van de kruin, steeds omhoog. Je ziet het nest vanzelf. Het is zo groot als een dorp."),
   ("npc", "Pas op voor de Kruinroofvogel. Hij zit op de Kale Kruin ernaast. Laat hem maar met rust, als je niet wilt vechten.")],
  "Naar het oude nest op de Kruintop. En dan terug naar Ordemeester Wasja in de Nestkapel.",
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
   D("gebied", "poi:z3_raattrossen", 1, "Zoek wat er piept in de Raattrossen, onder de westkant van Honingtak (neem de raattroslift)", plek="poi:z3_raattrossen")],
  [("npc", "Raatje is de oudste van de stad. Als iemand iets weet van het boomvolk, dan zij."),
   ("npc", "En hoor je dat? Al de hele ochtend piept er iets in de Raattrossen. Kijk eens even, wil je?")],
  "Eerst Imkermoeder Raatje, en dan dat gepiep in de Raattrossen.",
  [("npc", "Een pieppiepmuisje! Helemaal kaal! En zo koud... kom maar, kleintje."),
   ("jij", "Hij zei alleen 'de Graaf... de kou...'. Hij komt uit een toren."),
   ("npc", "Een toren. In de stam staat een toren. Die van het boomvolk. Hun oude kaart ligt bij Kaartenmaakster Lusje, in Hangnest "
           "Dauwdrup, helemaal in het oosten onder de lange tak."),
   ("npc", "Neem het muisje maar mee. Het wil niet meer uit je tas. Ik geloof dat het je vertrouwt.")],
  xp=6000, munten=4000, lv=34, na=["z3_hq05_nest"], inleveren="z3_raatje",
  praat={"z3_raatje": [
      ("npc", "Houten voetjes? Ach, het boomvolk. Mijn oma vertelde erover. Kleine houten wezentjes met lichtjes in hun hoofd."),
      ("npc", "Ze bouwden een stad in de stam, zei ze. Met lantaarns. Toen werd het stil, en de lantaarns gingen uit."),
      ("npc", "Ze hadden een kaart van de hele stam. Die ligt nu bij Lusje, in Hangnest Dauwdrup. Denk ik. Hoop ik.")]})

Q("z3_hq07_hangnest", "hoofd", "z3_raatje", "Hangnest Dauwdrup",
  "Kaartenmaakster Lusje woont in Hangnest Dauwdrup, een dorpje dat helemaal in het oosten onder de lange tak hangt. Rijd de Takbaan en help het dorp: "
  "Mika-Kaasschutters hebben drie posten op de tak gebouwd.",
  [D("gebied", "poi:z3_hangnest", 1, "Rijd over de Takbaan naar Hangnest Dauwdrup, helemaal in het oosten onder de lange tak", plek="poi:z3_hangnest"),
   D("praat", "z3_lusje", 1, "Praat met Kaartenmaakster Lusje op het hangbrugplein van Hangnest Dauwdrup", plek="npc:z3_lusje"),
   D("gebied", "poi:z3_kaaspost_1", 1, "Ruim de eerste Kaasschutter-post op, op de lange tak vlak achter de kruin", plek="poi:z3_kaaspost_1"),
   D("gebied", "poi:z3_kaaspost_2", 1, "Ruim de tweede Kaasschutter-post op, op de tak naar de Kaaszeppelinwerf", plek="poi:z3_kaaspost_2"),
   D("gebied", "poi:z3_kaaspost_3", 1, "Ruim de derde Kaasschutter-post op, op de zuidoosttak", plek="poi:z3_kaaspost_3"),
   D("opvrolijk", "gebied:z3_takkenbanen", 12, "Versla 12 wezens of Mika's op de Takkenbanen", plek="gebied:z3_takkenbanen")],
  [("npc", "De Takbaan begint bij de Pluimstal: door de kruin naar de lange tak in het oosten, en dan de tak af. Hangnest Dauwdrup hangt eronder."),
   ("npc", "Niet bang zijn op de tak. Overal waar je kunt vallen, kun je zweven. En je Pluimguh is heel goed in niet-vallen.")],
  "Over de Takbaan naar Hangnest Dauwdrup, Lusje helpen, en de drie Kaasschutter-posten opruimen.",
  [("npc", "De posten zijn weg! En mijn thee is weer warm. Nou ja, lauw. Het is een begin."),
   ("npc", "De Boomvolkkaart... die hebben de Mika's meegenomen. Naar hun werf. De Kaaszeppelinwerf, aan de punt van de noordoosttak."),
   ("npc", "Ik zag ze gaan. Met de kaart in een kaaskist. Ze lachten erbij. Heel onbeleefd.")],
  xp=8000, munten=5000, loot="zeldzaam", lv=35, na=["z3_hq06_voetjes"], inleveren="z3_lusje",
  praat={"z3_lusje": [
      ("npc", "Een bezoeker! Voorzichtig, de brug schommelt. Ik ben Lusje. Ik teken kaarten. Van alles. Ook van dingen die ronddraaien."),
      ("npc", "De Boomvolkkaart? Ach... Er zijn Mika's geweest. Niemand is geraakt, hoor. Maar mijn thee was koud geworden van de "
              "schrik."),
      ("npc", "Ze hebben drie Kaasschutter-posten op de tak gebouwd. Help eerst het dorp, wil je? Dan vertel ik alles.")]})

Q("z3_hq08_werf", "hoofd", "z3_lusje", "De Kaaszeppelinwerf",
  "De Kaaszeppelinwerf hangt aan de punt van de noordoosttak: een Mika-scheepswerf met steigers, kranen en drie half gebouwde "
  "kaaszeppelins. Versla Takelmeester Taleggio, Bombardier Brokkel en Kapitein Roquefort, en zoek in de kajuit van de kapitein naar "
  "de Boomvolkkaart.",
  [D("meld", "z3_taleggio:verslagen", 1, "Versla Takelmeester Taleggio op de steigers van de Kaaszeppelinwerf", plek="baas:z3_zeppelinwerf"),
   D("meld", "z3_brokkel:verslagen", 1, "Versla Bombardier Brokkel in de bewapeningsloods van de werf (draai de kanonnen!)",
     plek="baas:z3_zeppelinwerf"),
   D("meld", "z3_roquefort:verslagen", 1, "Versla Kapitein Roquefort op het dek van de vlaggenzeppelin, achterin de werf",
     plek="baas:z3_zeppelinwerf"),
   D("gebied", "poi:z3_kajuit", 1, "Doorzoek de kajuit van Kapitein Roquefort op de vlaggenzeppelin", plek="poi:z3_kajuit")],
  [("npc", "De werf ligt aan de punt van de noordoosttak. Je ziet de kranen al van hier. En je ruikt de kaas."),
   ("npc", "Ga met vrienden als dat kan. Drie Mika-bazen achter elkaar, dat is veel. Zelfs voor een held met een Pluimguh.")],
  "De Kaaszeppelinwerf aan de punt van de noordoosttak: drie bazen, en dan de kajuit van de kapitein.",
  [("npc", "Je hebt de kaart! En wat is dat... een kistje met een strik?"),
   ("jij", "'Voor de Graaf, voor zijn verzameling.' Er zat een hartsplinter in. En een brief van Baron Bleu."),
   ("npc", "Lees eens voor?"),
   ("jij", "'Mijn schimmel maakt het ei grauw. Een grauwe draak, helemaal van ons. Hij zal tevreden zijn, Uwe Deftigheid. Eindelijk "
           "één ding dat Hij vol vindt.'"),
   ("npc", "...Vol? Wie vindt er nou nooit iets vol? Brrr. Kijk, de kaart. Er loopt een weg om de stam heen, de Stamspiraal. Halverwege "
           "zit een poort naar binnen.")],
  xp=12000, munten=8000, loot="episch", lv=37, na=["z3_hq07_hangnest"], inleveren="z3_lusje")

Q("z3_hq09_spiraal", "hoofd", "z3_lusje", "De Stamspiraal",
  "De Boomvolkkaart toont de weg naar binnen: de Stamspiraal, een brede weg die om de buitenkant van de stam draait. Rijd hem af tot "
  "de middelste poort en stap de holle stam binnen: de Schorsstad.",
  [D("gebied", "poi:z3_spiraalpoort", 1, "Rijd de Stamspiraal omlaag tot de middelste poort, halverwege de stam", plek="poi:z3_spiraalpoort"),
   D("praat", "z3_knoest", 1, "Praat met het lichtje dat binnen bij de middelste poort zweeft", plek="npc:z3_knoest")],
  [("npc", "De Stamspiraal begint bovenaan, waar de kruin op de stam zit. Hij draait tweeënhalf keer om de stam. Je kunt bij de "
           "balkons naar beneden zweven, maar niet terug omhoog!"),
   ("npc", "Halverwege zit een poort. Daarachter... dat staat niet op de kaart. Alleen een tekeningetje van een lichtje.")],
  "De Stamspiraal af tot de middelste poort, en dan naar binnen.",
  [("npc", "...Je ziet me. Al zo lang heeft niemand me gezien. Ik ben Knoest. Het laatste lichtje van het boomvolk."),
   ("npc", "De lantaarns zijn uit sinds de Graaf in de toren woont. Hij houdt van donker. En van kou. Dan vallen de pelsjes sneller uit."),
   ("verteller", "Piepje rilt in je tas."),
   ("npc", "Kijk, daar. De Honinglift. Die heb ik weer laten zakken. Zo kom je snel tussen Honingtak en Mosvoet. Dat ga je nodig hebben.")],
  xp=9000, munten=6000, lv=39, na=["z3_hq08_werf"], inleveren="z3_knoest", actie_klaar="z3_honinglift")

Q("z3_hq10_piepvel", "hoofd", "z3_knoest", "Zeven lantaarns en een graaf",
  "In het midden van de Schorsstad hangt de Lantaarntoren. Steek de zeven ringen aan, van onder naar boven: op elke ring zitten "
  "muisjes in een kooitje. Bovenin, in de Pelszaal, zit Graaf Piepvel, de Pink van de Keizer, in een cape van muizenpelsjes.",
  [D("meld", "z3_lantaarn:ring", 7, "Steek de 7 ringen van de Lantaarntoren aan, van onder naar boven (ga bij elke ringlantaarn staan)",
     plek="poi:z3_lantaarntoren"),
   D("meld", "z3_piepvel:verslagen", 1, "Versla Graaf Piepvel in de Pelszaal bovenin de Lantaarntoren (breek de kooisloten, steek de "
     "lantaarns weer aan)", plek="baas:z3_pelszaal"),
   D("meld", "z3:flash_f1", 1, "Kijk wat er gebeurt in de Pelszaal", plek="poi:z3_pelszaal"),
   D("gebied", "poi:z3_capespeld", 1, "Raap de speld van de cape op, bij de troon in de Pelszaal", plek="poi:z3_capespeld")],
  [("npc", "Zie je de toren? Hij hangt aan wortelkettingen, van de vloer tot bijna het plafond. Zeven ringen. Zeven lantaarns."),
   ("npc", "Op elke ring heeft de Graaf muisjes in een kooitje gezet. In de kou. Tot hun pelsje uitvalt. Voor zijn cape."),
   ("verteller", "Piepje piept heel zachtjes."),
   ("npc", "Steek de ringlantaarns aan. Dan springen de slotjes open. Halverwege geef ik je iets, want bovenin is het pikdonker."),
   ("npc", "En bovenin... daar zit hij. Graaf Piepvel. De Pink van de Keizer.")],
  "Zeven ringen, van onder naar boven. En dan de Pelszaal.",
  [("npc", "...De lantaarns. Allemaal. De hele stam gloeit weer. Zo heb ik hem niet meer gezien sinds... ik weet het niet eens meer."),
   ("npc", "Die arm... Ik ken hem uit oude verhalen. Hij reikt ver. Altijd verder. Hij neemt alles wat warm is."),
   ("npc", "Ik zal je vertellen wat er met het ei gebeurde. Het boomvolk droeg het naar beneden toen de draak grauw werd. Het hart van "
           "de boom zou het warm houden, tot hij weer zichzelf was."),
   ("npc", "Wij gingen slapen in het hout. Het ei bleef wakker."),
   ("npc", "Ga naar Ordemeester Wasja. Vertel haar alles. Neem de Honinglift, dat gaat snel.")],
  xp=15000, munten=10000, loot="episch", lv=40, na=["z3_hq09_spiraal"], inleveren="z3_knoest")

Q("z3_hq11_wortel", "hoofd", "z3_wasja", "Rot aan de wortel",
  "In Mosvoet, het gehucht in een holle wortelknol, rotten de wortels weg: de Mika's hebben schimmelketels in de Wortelvelden "
  "gezet. Praat met Burgemeester Wortelien en zet de twee ketels uit. De bevrijde muisjes piepen de weg.",
  [D("praat", "z3_wortelien", 1, "Praat met Burgemeester Wortelien in Mosvoet (neem de Honinglift omlaag)", plek="npc:z3_wortelien"),
   D("meld", "z3_ketel:een", 1, "Zet de eerste schimmelketel van de Mika-schimmelboerderij uit, in het noordoosten van de Wortelvelden",
     plek="poi:z3_ketel_1"),
   D("meld", "z3_ketel:twee", 1, "Zet de tweede schimmelketel uit, ook bij de Mika-schimmelboerderij", plek="poi:z3_ketel_2")],
  [("npc", "Een keizer. De Mika's hebben een keizer. Dan is dit groter dan een draak, ridder. Veel groter."),
   ("npc", "Maar eerst: het ei. Het ligt in het hart van de boom, onder de stam. En daar beneden rotten de wortels."),
   ("npc", "Neem de Honinglift naar Mosvoet. Burgemeester Wortelien weet meer. En de muisjes die je bevrijdde, die ruiken schimmel van "
           "een kilometer afstand.")],
  "Met de Honinglift naar Mosvoet, Burgemeester Wortelien, en dan de twee schimmelketels uit.",
  [("npc", "De ketels zijn uit! Ruik je dat? Het ruikt weer naar sap. En naar mos. En een beetje naar ahornsiroop."),
   ("npc", "Ik zie het dal al terugkleuren. Groen, roze, en het water weer turquoise."),
   ("npc", "Er is nog één ding. Onder de boom, in het Wortelduister, gloeien de aders roze. Steeds feller. Alsof daar iets leeft.")],
  xp=12000, munten=9000, loot="zeldzaam", lv=42, na=["z3_hq10_piepvel"], inleveren="z3_wortelien",
  praat={"z3_wortelien": [
      ("npc", "Een held met een Pluimguh! En met... muisjes? Ze piepen allemaal tegelijk. Ze willen ergens heen."),
      ("npc", "De Mika's hebben twee schimmelketels in de Wortelvelden gezet. Allebei bij hun schimmelboerderij in het noordoosten van de "
              "Wortelvelden. Daardoor rot alles."),
      ("npc", "Volg de muisjes maar. Ze staan bij elke ingang en piepen de weg. Je hoeft niks te zoeken.")]})

Q("z3_hq12_duister", "hoofd", "z3_wortelien", "Het Wortelduister",
  "Onder de boom liggen grotten tussen de wortels: het Wortelduister. Volg met je Nestlantaarn de roze aders door de Grote Wortelgang "
  "naar de Hartslag-zaal, en steek de vier grotlantaarns bij de poort van het Hart aan.",
  [D("gebied", "poi:z3_wortelpoort", 1, "Ga naar de Grote Wortelpoort in het zuiden van de Wortelvelden", plek="poi:z3_wortelpoort"),
   D("gebied", "poi:z3_hartslagzaal", 1, "Volg de roze aders door de Grote Wortelgang omlaag naar de Hartslag-zaal (de muisjes kennen een "
     "zijtunnel)", plek="poi:z3_hartslagzaal"),
   D("meld", "z3_grotlantaarn:aan", 4, "Steek de 4 grotlantaarns rond de poort van het Hart aan", plek="poi:z3_hartpoort")],
  [("npc", "De Grote Wortelpoort ligt in het zuiden, recht onder de stam. Daarachter is het donker. Echt donker."),
   ("npc", "Blijf in het licht van je lantaarn. In het donker doen de monsters minder pijn, maar je ziet ze ook bijna niet."),
   ("npc", "Bij het kampvuur in de Hartslag-zaal staat Wortelwacht Gloei. Hij weet hoe je het Hart in komt.")],
  "Door de Grote Wortelpoort, de roze aders volgen naar de Hartslag-zaal, en de vier grotlantaarns aan.",
  [("npc", "Je hebt de lantaarns aangestoken! Kijk, de poort van het Hart gaat open. Hoor je die klop?"),
   ("jij", "En er klopt nog iets. Zachter. Sneller."),
   ("npc", "...Dat heb ik nooit gehoord. Een tweede hart. Een klein hartje. Ga, avonturier. Daar binnen ligt wat je zoekt.")],
  xp=14000, munten=10000, loot="episch", lv=45, na=["z3_hq11_wortel"], inleveren="z3_gloei")

Q("z3_hq13_hart", "hoofd", "z3_gloei", "Het Hart van de Boom",
  "Achter de Hartslag-zaal klopt het Hart van de Boom. Versla de Wortelwurger, vrolijk de Oude Nestwachter op en versla Baron Bleu en "
  "zijn Schimmelkolos. In de Nestkamer ligt het Drakenei.",
  [D("meld", "z3_wortelwurger:verslagen", 1, "Versla de Wortelwurger in het Hart van de Boom (kap de 4 voedingswortels door)",
     plek="baas:z3_hart"),
   D("meld", "z3_nestwachter:opgevrolijkt", 1, "Vrolijk de Oude Nestwachter op (sta op de roze jaarring)", plek="baas:z3_hart"),
   D("meld", "z3_bleu:verslagen", 1, "Versla Baron Bleu en zijn Schimmelkolos (vecht in het licht van de lantaarnpalen)", plek="baas:z3_hart"),
   D("meld", "z3:ei", 1, "Pak het Drakenei in de Nestkamer, helemaal achterin het Hart", plek="poi:z3_nestkamer")],
  [("npc", "Het Hart is een kerker. Kloppende wanden, schimmelgordijnen, en drie wachters. Ga met vrienden als het kan."),
   ("npc", "De Wortelwurger is een kluwen grauwe wortels. Kap zijn voedingswortels door als het hart klopt."),
   ("npc", "En de Oude Nestwachter... die is niet slecht. Alleen grauw. Vrolijk hem op, dan laat hij je door.")],
  "Het Hart van de Boom: de Wortelwurger, de Oude Nestwachter, Baron Bleu. En dan het ei.",
  [("npc", "Je hebt het. Het ei. Kijk hoe het gloeit als jij het vasthoudt."),
   ("npc", "Dit hield ik warm voor hem. Het hartje van zijn hart. Die splinter heb je al. Nu nog hém."),
   ("verteller", "De grond begint te trillen. Eerst zacht. Dan harder. Ergens hoog boven je kraakt de hele boom."),
   ("npc", "Hij komt. Hij voelt dat het ei wakker is. Ga! Terug omhoog, naar het oude nest! Snel!")],
  xp=20000, munten=15000, loot="episch", lv=47, na=["z3_hq12_duister"], inleveren="z3_nestwachter", actie_klaar="z3_hq14")

Q("z3_hq14_vahoegnir", "hoofd", "z3_nestwachter", "Vahoegnir komt thuis",
  "Vahoegnir komt zijn ei halen. Ontsnap over de Stamspiraal naar het oude nest op de Kruintop, leg het ei terug en vrolijk de "
  "Vadsloze Draak op: steek bij 75, 50 en 25 procent een hartsplinter terug in zijn borst.",
  [D("meld", "z3:ontsnapt", 1, "Ontsnap over de Stamspiraal naar het oude nest op de Kruintop", plek="poi:z3_kruintop"),
   D("meld", "z3_vahoegnir:opgevrolijkt", 1, "Vrolijk Vahoegnir op bij het oude nest op de Kruintop (sta in de roze cirkel onder zijn "
     "borst)", plek="baas:z3_kruintop"),
   D("meld", "z3:finale", 1, "Kijk wat er met het ei gebeurt", plek="poi:z3_kruintop")],
  [("npc", "Ga!")],
  "Naar het oude nest op de Kruintop. Vrolijk Vahoegnir op. En dan terug naar Ordemeester Wasja in de Nestkapel.",
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
   ("npc", "Ga naar het balkon, ridder. Kijk naar het oosten. Waar de straal eindigde."),
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
  "Kuikenhoeder Pip is zes Pluimkuikens kwijt in de Kruinzee. Ze zitten verstopt bij de honingvijvers, in roze dons. Vind ze allemaal.",
  [D("geheim", "teller:z3_kuikens", 6, "Vind 6 verdwaalde Pluimkuikens bij de honingvijvers in de Kruinzee (roze dons)", plek="gebied:z3_kruinzee")],
  [("npc", "Help! Mijn Pluimkuikens zijn weg! Ze renden achter een vlinder aan. Alle zes. De Kruinzee in."),
   ("npc", "Ze zitten vast bij de honingvijvers in de kruin. Je ziet ze aan het roze dons. En aan het gepiep.")],
  "Zes Pluimkuikens, ergens bij de honingvijvers in de kruin.",
  [("npc", "Ze zijn er allemaal! Eén, twee, drie, vier, vijf... zes! Dank je wel! Hier, een zakje Honingbrokjes voor je Pluimguh.")],
  xp=6000, munten=4000, lv=31, na=_NA)

Q("z3_s02_opa_bast", "zij", "z3_opa_bast", "Nog één keer de top",
  "Opa Bast wil nog één keer de Kruintop zien, zoals vroeger. Hij kan niet meer zo goed lopen. Neem hem mee naar het oude nest.",
  [D("escorte", "z3_opa_bast", 1, "Breng Opa Bast naar de Kruintop (hij loopt met je mee; wezens laten hem met rust)", bron="poi:z3_kruintop",
     plek="poi:z3_kruintop")],
  [("npc", "Vroeger klom ik elke dag naar de Kruintop. Om de draak te horen zingen. Heel zacht."),
   ("npc", "Neem je me nog één keer mee? Ik loop langzaam, maar ik loop. En de monsters doen niks met zo'n ouwe guh als ik.")],
  "Opa Bast loopt met je mee naar de Kruintop.",
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
  "In de wand van de Schorsstad slapen nog vier lichtjes van het boomvolk. Zoek ze op verschillende hoogtes en maak ze wakker.",
  [D("geheim", "teller:z3_lichtjes", 4, "Vind 4 slapende boomvolk-lichtjes in de wand van de Schorsstad (groene gloed)", plek="gebied:z3_schorsstad")],
  [("npc", "...Er slapen nog vrienden van mij in het hout. Vier. Ik voel ze. Hoog en laag."),
   ("npc", "Maak ze wakker, wil je? Een lichtje alleen is maar een lampje. Samen zijn we een stad.")],
  "Vier slapende lichtjes, hoog en laag in de wand van de Schorsstad.",
  [("npc", "...Ze zijn wakker. Hoor je ze? Ze lachen. Ik ben vergeten hoe dat klonk. Dank je.")],
  xp=9000, munten=6000, lv=40, na=["z3_hq09_spiraal"])

Q("z3_s05_kaartjes", "zij", "z3_lusje", "Lusje's kaartjes",
  "Kaartenmaakster Lusje wil vijf uitkijkpunten op haar nieuwe kaart zetten. Maar dan moet er wel iemand echt gestaan hebben.",
  [D("gebied", "poi:z3_uitkijk_kroon", 1, "Sta op uitkijkpunt De Kroon, op de Kruinring", plek="poi:z3_uitkijk_kroon"),
   D("gebied", "poi:z3_uitkijk_takpunt", 1, "Sta op uitkijkpunt Takpunt, aan het eind van de zuidoosttak", plek="poi:z3_uitkijk_takpunt"),
   D("gebied", "poi:z3_uitkijk_spiraal", 1, "Sta op het bovenste balkon van de Stamspiraal", plek="poi:z3_uitkijk_spiraal"),
   D("gebied", "poi:z3_uitkijk_gewelf", 1, "Sta bovenop het Wortelgewelf in de Wortelvelden", plek="poi:z3_uitkijk_gewelf"),
   D("gebied", "poi:z3_uitkijk_mast", 1, "Klim in het kraaiennest van de luchtschipmast van Honingtak", plek="poi:z3_uitkijk_mast")],
  [("npc", "Ik teken kaarten, maar ik kom nooit ergens. Ik hou niet van hoogtes. Raar, voor iemand die in een hangnest woont."),
   ("npc", "Vijf uitkijkpunten. Ga er staan en kijk goed. Dan teken ik ze erbij.")],
  "Vijf uitkijkpunten: De Kroon, Takpunt, de Stamspiraal, het Wortelgewelf en de mast.",
  [("npc", "Alle vijf! Kijk, mijn kaart is af. Met een tekeningetje van jou erop. Op een Pluimguh. Heel klein.")],
  xp=10000, munten=8000, loot="zeldzaam", lv=36, na=["z3_hq07_hangnest"])

# ===== job advancement at class level 50 (klassen + z3-systemen do the ceremony; Advancement step 4) ==========================
KLASSEN = {
    "knuffelridder": ("Knuffelridder", "Nestwachter"),
    "pluisjager": ("Pluisjager", "Kruinvalk"),
    "vadsmagier": ("Vadsmagiër", "Schemerwijze"),
}
for k, (naam, t4) in KLASSEN.items():
    Q(f"z3_adv50_{k}", "klasse", "z3_wasja", f"De proef van de {t4}",
      f"De vierde titel van De Knuffelorde: {t4}. Bereik klasse-level 50 als {naam} en bewijs je in het Wortelduister.",
      [D("level", "*", 50, f"Bereik level 50 als {naam} (premies, het Hart van de Boom en het Wortelduister helpen)",
         plek="gebied:z3_wortelduister"),
       D("opvrolijk", "gebied:z3_wortelduister", 25, "Versla 25 wezens in het Wortelduister, onder de boom", plek="gebied:z3_wortelduister")],
      [("npc", f"Drie titels draag je al. Maar diep in de boom wacht een vierde: {t4}."),
       ("npc", "Bereik level 50 en versla vijfentwintig wezens in het Wortelduister. Dan dansen de imkers in de Nestkapel. Voor jou.")],
      f"Level 50 als {naam}, en vijfentwintig wezens in het Wortelduister.",
      [("npc", f"De bijen zoemen, de bloesem valt, de imkers dansen. Ik benoem je tot {t4}! De hele boom is trots op je.")],
      xp=20000, munten=15000, lv=48, klasse=k, na=["z3_hq13_hart"], actie_klaar=f"advancement:{k}:4")

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
    "imkerpak": ("Een vreemd imkerpak", "Bij het huis van Raatje hangt een imkerpak aan de waslijn. Met gaatjes voor oren. Puntige oren. En een staart."),
    "grauwe_honing": ("Grauwe honing", "In een potje op de markt zit grauwe honing. Het etiket zegt: 'Rijpe Kaas-honing. Niet voor guhs.'"),
    "koningin": ("Een stille koningin", "De bijenkoningin zingt niet meer. Ze zit grauw en stil in haar raat. Iets heeft haar verdrietig "
                                        "gemaakt."),
    "luik": ("Een geheim luik", "Onder in de Raattrossen zit een luik met een kattenpootje erop. Erachter zoemt het boos. En er piept een "
                                "stem: 'Miauw. Niet storen.'"),
}
SCHATTEN = {
    "onder_de_tak": ("Onder de zuidoosttak", "Aan de onderkant van de zuidoosttak hangt een kist in een nest. Hier kom je alleen zwevend!"),
    "nesttop": ("Boven het oude nest", "Op de hoogste tak boven het oude nest ligt een vergeten kist. Zo hoog kwam nog nooit iemand."),
    "schorsstad": ("Hoog in de Schorsstad", "Op een richel hoog in de holle stam staat een kist van het boomvolk. Er zit nog licht in."),
    "wortelduister": ("In een zijtunnel", "In een zijtunnel van het Wortelduister, waar alleen de muisjes kwamen, ligt een kist."),
    "moeras": ("Op een honingeilandje", "Op het kleinste honingeilandje in het Honingmoeras staat een kist. Plakkerig, maar vol."),
}
RELIEFS = {
    "relief_1": ("Het eerste reliëf", "Een draak, roze en groot, cirkelt boven een piepklein boompje. Het boompje groeit."),
    "relief_2": ("Het tweede reliëf", "De boom is gigantisch. In de kruin bouwt de draak een nest. Kleine houten wezentjes zwaaien."),
    "relief_3": ("Het derde reliëf", "Een ei, roze en gloeiend, in het nest. Het boomvolk danst eromheen met lantaarns."),
    "relief_4": ("Het vierde reliëf", "Een grauwe wolk komt uit het oosten. De draak vliegt erop af. Hij is heel boos. Of heel bang."),
    "relief_5": ("Het vijfde reliëf", "De draak komt terug. Grauw. Er steekt een splinter uit zijn borst. Hij gromt naar zijn eigen ei."),
    "relief_6": ("Het zesde reliëf", "Het boomvolk draagt het ei naar beneden, de stam in, tot diep onder de wortels."),
    "relief_7": ("Het zevende reliëf", "Het boomvolk zit in een kring en valt in slaap. Hun lichtjes worden zachter. Het ei gloeit."),
    "relief_8": ("Het achtste reliëf", "Een kat met armen tot aan de rand van de muur. Hij reikt naar het nest. Zijn vingers zijn heel lang."),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Gids pages (questlog J, tab Gids: ids z3_* in QuestlogScherm.GIDS), tips, bestiary pages, other texts
# ---------------------------------------------------------------------------------------------------------------------------
GIDS = {
    "z3_schemerwortel": ("Zone 3: De Schemerwortel", "Na de Splinterpieken en vanaf level 30 vliegt het luchtschip je naar Honingtak, een "
                         "imkersstadje op een tak van een boom zo groot als een berg. Bovenin de Kruinzee (lv 30-34), de Takkenbanen "
                         "(lv 34-38), in de stam de Schorsstad (lv 40-43), op de grond de Wortelvelden (lv 43-46) en eronder het "
                         "Wortelduister (lv 46-49)."),
    "z3_pluimguh": ("Je Pluimguh", "Bij Stalmeester Veertje in de Pluimstal kies je je eerste rijdier: een Pluimguh. Roep hem met X, "
                    "open je rijdierscherm met N. Van hoog kun je met je Pluimguh omlaag zweven; omhoog gaat met trappen, liften en de "
                    "bloesemwind. Als je aanvalt of een baas je raakt, stap je af."),
    "z3_lantaarn": ("Licht in het donker", "In het Wortelduister en het Hart van de Boom is het pikdonker. Wezens buiten het licht zijn "
                    "donkere silhouetten en krijgen de helft minder schade. Je Nestlantaarn, roze aders en aangestoken grotlantaarns "
                    "geven licht. Samen zie je meer."),
    "z3_premiebord": ("Het premiebord", "In de Jagershut van Honingtak hangt het premiebord van Jagersguh Raap: elke dag nieuwe klusjes "
                      "voor level 40-50. Jagen, verzamelen, bezorgen, schimmel opruimen. Geen haast, geen ranglijst: doe wat je leuk "
                      "vindt."),
    "z3_reizen": ("Reizen in de boom", "Kapitein Zoemzeil vliegt je van de mast van Honingtak naar De Haven of Kandijkom. In de boom rijd je: "
                  "de Kruinring, de Takbaan, de Stamspiraal en de Wortelweg. Na de Stamspiraal brengt de Honinglift je in één rit "
                  "van Honingtak naar Mosvoet."),
    "z3_titel": ("De vierde titel", "Op klasse-level 50 wacht je een vierde titel van De Knuffelorde, bij Ordemeester Wasja in de Nestkapel "
                 "van Honingtak: Nestwachter, Kruinvalk of Schemerwijze."),
}
TIPS = {
    "z3_monsters": ("Echte monsters", "De wezens in de boom zijn echte monsters. Versla ze gewoon. Guhs doe je nooit pijn."),
    "z3_lantaarn": ("Pikdonker!", "Blijf in het licht. Buiten het licht zijn wezens silhouetten en doen je klappen de helft minder schade."),
    "z3_zweef": ("Zweven!", "Op je Pluimguh kun je van hoge plekken naar beneden zweven. Spring van het zweefplatform en houd de springtoets "
                            "ingedrukt."),
}
OVERIG = {
    # travel: the destination texts (optie.vlieg.schemerwortel, reis.slot.schemerwortel_*, reis.kies.z3_kapitein) are z3-wereld's
    P + "optie.rol.luchtschip_z3": "Waar vliegen we heen?",
    P + "optie.rol.premiebord": "Het premiebord bekijken",
    P + "optie.rol.honingkraam": "Wat heb je in De Honingkraam?",
    P + "optie.z3_jacht": "Het jachtbord bekijken",
    P + "optie.z3_penningwinkel": "Naar de penningwinkel",
    P + "optie.z3_gouden_veren": "25 Gouden Veren ruilen voor de Gouden Pluimguh",
    P + "optie.z3_advancement": "Ik ben klaar voor mijn vierde titel!",
    P + "optie.rol.honinglift": "Met de Honinglift",
    P + "optie.rol.honingthee": "Een kopje honingthee, graag!",
    P + "adv.knuffelridder.4": "Nestwachter", P + "adv.pluisjager.4": "Kruinvalk", P + "adv.vadsmagier.4": "Schemerwijze",
    # roles
    Z + "honinglift.dicht": "De Honinglift zit nog vast. Iemand in de stam moet hem eerst laten zakken...",
    Z + "honinglift.open": "Ergens beneden ratelt een ketting. De Honinglift tussen Honingtak en Mosvoet doet het weer!",
    Z + "honinglift.af": "Instappen! Langzaam zakt de emmer langs de stam omlaag, naar Mosvoet.",
    Z + "honinglift.op": "Instappen! De emmer zwiept omhoog, langs de stam, naar Honingtak.",
    Z + "thee": "Alsjeblieft, een kopje honingthee. Pas op, heet. En zoet. Vooral zoet.",
    Z + "thee.al": "Je hebt er net al een gehad! Eerst opdrinken, dan krijg je er nog een.",
    # scenes
    Z + "scene.vlucht.titel": "Honingtak",
    Z + "scene.vlucht.onder": "De Schemerwortel",
    Z + "scene.opening.titel": "De Schemerwortel",
    Z + "scene.opening.onder": "Een boom zo groot als een berg",
    Z + "scene.schors": "Een wand van schors schuift langs het luchtschip. Hij houdt niet op. Hij gaat maar omhoog en omhoog...",
    Z + "scene.kruin": "Het schip breekt door de wolken. De kruin ligt in het goud.",
    Z + "scene.zwaaien": "Op een tak zwaaien imkerguhs met hun sluiers. Honingtak!",
    Z + "scene.gekraak": "Diep in de wortels gaan twee grijsblauwe ogen open. Iets kraakt: \"Hij leeft nog... maar niet lang meer.\"",
    Z + "scene.aankomst": "Welkom in Honingtak! Imkermoeder Raatje wacht bij haar korfhuisje, een trapje onder het Bloesemplein.",
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
    Z + "scene.f1.kraak": "De toren kraakt. Door een kwastgat in de stam valt een lange schaduw naar binnen.",
    Z + "scene.f1.nee": "\"Nee nee nee, Keizer, ik kan het uitleggen! Het was de ridder! Het was...\"",
    Z + "scene.f1.floep": "Een lange, magere, grauwe arm, veel te lang, met vijf lange vingers, grijpt de Graaf bij zijn hemdje. Floep. Weg.",
    Z + "scene.f1.afdruk": "Stilte. Op de schors blijft één grauwe vingerafdruk achter. Zo groot als een deur.",
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
    Z + "scene.rit.onder": "Omhoog over de Stamspiraal",
    Z + "scene.rit.schudt": "De boom schudt! Takken vallen, schors scheurt. Vahoegnir slaat door de kruin!",
    Z + "scene.rit.zweef": "Spring! Je Pluimguh zweeft over het gat in de spiraal. De draak scheert langs de stam.",
    Z + "scene.rit.nest": "Het oude nest! Je legt het ei in de eierkuil. Daar landt hij: enorm, grauw, knetterend van zwarte pluis.",
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
    Z + "scene.f2.onder": "Ver in het oosten strekt iets zijn armen.",
    Z + "scene.f2.zaal": "Ergens ver weg. Een grauwe zaal, mager licht. Graaf Piepvel knielt in zijn hemdje en rilt.",
    Z + "scene.f2.krabbel_1": "Een piepklein Mika'tje met een veel te grote ganzenveer leest voor: \"Rapport uit het westen.\"",
    Z + "scene.f2.krabbel_2": "\"De draak is... eh... roze.\"",
    Z + "scene.f2.krabbel_3": "\"Het ei is... eh... uit.\"",
    Z + "scene.f2.krabbel_4": "\"De Pink is... kaal.\"",
    Z + "scene.f2.tik": "Lange grauwe vingers trommelen op een armleuning. Tik. Tik. Tik. Tik. Tik. In het tegenlicht: een kroontje, veel te klein.",
    Z + "scene.f2.stem": "Een trage stem: \"Dan... reik... ik... verder.\"",
    Z + "zone3.klaar": "De draak is weer roze en het ei is uit! De Schemerwortel bloeit weer. (Hoogvads komt binnenkort.)",
    # story moments outside scenes
    Z + "rit.titel": "Je eerste rit!",
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
    Z + "nestlantaarn.1": "Bovenin is het pikdonker. In het licht van deze lantaarn kan niets zich verstoppen. Ook geen graaf.",
    Z + "ketel.uit": "Je draait de schimmelkraan dicht. De ketel sist, sputtert en valt stil. Het dal kleurt een beetje terug.",
    Z + "duister.klopje": "Onder de klop van het hart hoor je nog iets. Een tweede klopje. Zachter. Sneller. Het ei leeft nog!",
    Z + "vahoegnir.zonder": "Vahoegnir landt bij het nest, grauw en knetterend. Je houdt de splinters omhoog...",
    Z + "vingerafdruk": "Een vingerafdruk in de schors, zo groot als een deur. Grauw. Professor Pluis zou hier een week naar willen kijken.",
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
    Z + "relief.alle": "Alle acht reliëfs vertellen samen het verhaal van het nest en het ei. Het boomvolk wist alles. En het laatste reliëf... die armen.",
    Z + "baas.roquefort": "Kapitein Roquefort", Z + "baas.kruinroofvogel": "De Kruinroofvogel", Z + "baas.piepvel": "Graaf Piepvel",
    Z + "baas.bleu": "Baron Bleu", Z + "baas.vahoegnir": "Vahoegnir, de Vadsloze Draak", Z + "baas.imker": "De Mika-imker",
    # the pet + the Verkenningsboek page
    "vadsrijk.huisdier.vahoegje.naam": "Vahoegje",
    "vadsrijk.voltooiing.zone.zone3": "De Schemerwortel",
    "vadsrijk.voltooiing.hint.rijdier.pluimguh": "Bij Stalmeester Veertje in de Pluimstal van Honingtak.",
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


def quests_json():
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
# FTB Quests: zone 3 joins the chapter "Het Vadsrijk" (vadsrijk_npc.py) with its own sections. Nothing is locked.
# ---------------------------------------------------------------------------------------------------------------------------
FTB_CHAPTER = "guhs_vadsrijk"
FTB_PORTRAIT = "guh:golden"
FTB_SECTIES = [
    ("vr_z3_verhaal", "De Schemerwortel (zone 3)", "guh:golden", [f"vr_{q['id']}" for q in QUESTS if q["soort"] == "hoofd"]),
    ("vr_z3_honingtak", "Honingtak helpen", "guh:choco", [f"vr_{q['id']}" for q in QUESTS if q["soort"] in ("zij", "klasse")]),
    ("vr_z3_geheimen", "Geheimen van De Schemerwortel", "guh:mint", ["vr_z3_geheim_imker", "vr_z3_knuffels_alle", "vr_z3_aanwijzingen_alle",
                                                                    "vr_z3_reliefs_alle", "vr_z3_grauwe_brief"]
     + [f"vr_z3_schat_{s}" for s in SCHATTEN]),
]


def ftb(fq):
    q, adv = fq.q, fq.adv
    K = "guhs:kaas_knabbels"
    for d in QUESTS:
        tekst = d["tekst"].replace("Honingtak", "&6Honingtak&r")
        icon = {"hoofd": "minecraft:honeycomb", "klasse": "minecraft:enchanted_book", "geheim": "minecraft:spyglass"}.get(
            d["soort"], "minecraft:writable_book")
        vorm = "gear" if d["soort"] == "hoofd" else ("hexagon" if d["soort"] in ("klasse", "geheim") else None)
        q(f"vr_{d['id']}", d["titel"], tekst + " &7(Vinkt zichzelf af als je hem in het Vadsrijk inlevert.)", icon,
          [fq.met(adv(f"vr_{d['id']}"), d["titel"])], rewards=((K, 8 if d["soort"] == "zij" else 16),), shape=vorm,
          xp=100 if d["soort"] == "hoofd" else 50)
    q("vr_z3_knuffels_alle", "Tien knuffels in de boom", "Vind alle &e10 knuffels&r die in De Schemerwortel verstopt zitten. Drie ervan "
      "bereik je alleen zwevend.", "guhs:knuffel_golden", [fq.met(adv("vr_z3_knuffels_alle"), "10 knuffels")], rewards=((K, 16),), shape="hexagon")
    q("vr_z3_aanwijzingen_alle", "De grauwe honing", "Vind alle &e5 imkersporen&r in Honingtak. Wie verstopt zich in de Raattrossen?",
      "minecraft:spyglass", [fq.met(adv("vr_z3_aanwijzingen_alle"), "5 sporen")], rewards=((K, 16),), shape="hexagon")
    q("vr_z3_reliefs_alle", "Het Muurverhaal", "Vind alle &e8 reliëfs&r van het boomvolk in de wand van de Schorsstad.", "minecraft:chiseled_stone_bricks",
      [fq.met(adv("vr_z3_reliefs_alle"), "8 reliëfs")], rewards=((K, 16),), shape="hexagon")
    q("vr_z3_grauwe_brief", "Uit Nooitgenoeg", "Vind de eerste &8Grauwe brief&r. Wie schrijft er aan de Pink?", "minecraft:paper",
      [fq.met(adv("vr_z3_grauwe_brief"), "Grauwe brief")], rewards=((K, 8),))
    for s, (naam, _t) in SCHATTEN.items():
        q(f"vr_z3_schat_{s}", naam, "Een verborgen schat van De Schemerwortel. Hij glinstert goudgeel als je dichtbij bent.",
          "minecraft:chest", [fq.met(adv(f"vr_z3_schat_{s}"), naam)], rewards=((K, 8),))


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
