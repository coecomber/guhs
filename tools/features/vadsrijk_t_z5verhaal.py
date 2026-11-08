"""
1.10 "Het Vadsrijk", zone 5 "Hoogvads" (T_BRIEF night round T, slice t-z5verhaal). Java: feature/vadsrijk/zone5/verhaal.

Hoogvads is the big, festive capital of the guhs on the back of Moeder Wolkwal (WERELDVERHAAL 4.2, ZONE4_PLAN round Z5-1). Zone 5
uses the SAME quest system as zones 1-3 (feature/vadsrijk/npc); this generator adds the zone-5 data:
  - the NPC guhs of Hoogvads: names, idle lines, WHERE they stand (vadsrijk.npc.plek.npc.<id>);
  - the main line "Verraad in de stad" (a detective story: a Mika spy in the Hoge Raad, the whale is the stake), the side quests,
    the profession hooks and the 4th-class teasers -> data/guhs/vadsrijk/npc/quests_z5.json (Quests.BESTAND_Z5, read after every
    earlier zone) + texts; every objective says WHO + WHERE + WHAT and carries a "plek" for the tracker;
  - the detective data (clues, suspects, interrogations with choices) -> data/guhs/vadsrijk/z5verhaal/speurwerk.json;
  - quest items, hidden FTB advancements guhs:quest/vr_z5_*, FTB sections, Gids pages, tips, scene texts, the Verkenningsboek
    page texts (lookouts, campfires, knuffels, treasures, areas).
Ids: NPCs and quests "z5_*", items "vr_z5_npc_*", own texts "vadsrijk.z5verhaal.*". In-game text is Dutch; en_us gets the same
Dutch text except a few cheap English titles.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z5verhaal.py
"""
import json
import os
import re

P = "vadsrijk.npc."             # (the shared quest system's keys)
Z = "vadsrijk.z5verhaal."       # (zone-5 own texts)
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone5", "verhaal")

_LAAN = "op de Zachtmoedlaan, de brede laan van het paleis naar de kathedraal, midden in Hoogvads"
_SOEP = "in de Grote Soepkeuken, het vakwerkhuis met de dakruiter en de stoom, aan de zuidkant van de markt van Hoogvads"
_HAVEN = "op de Luchthaven aan de noordflank van Hoogvads, het station met het glazen dak en de drie pieren"
_ACAD = "in de Academie van de Wolkwacht, de witte vesting met het oefenplein aan de zuidkant van Hoogvads"
_UNI = "in de universiteit aan de noordkant van Hoogvads (de binnenhof met de kloostergang)"
_RAAD = "in de Raadszaal, de witte zuilentempel boven aan de grote trap op de paleisheuvel (oostkant van Hoogvads)"
_TROON = "in de troonzaal van het paleis, helemaal achterin op de paleisheuvel aan de oostkant van Hoogvads"
_MARKT = "op de markt van Hoogvads, rond de fontein van de koningin midden op de Zachtmoedlaan"
_GILDE = "in het Ambachtsgilde, de werkplaatsen naast de universiteit aan de noordkant van Hoogvads"

# ---------------------------------------------------------------------------------------------------------------------------
# NPCs: name, 3 idle lines, WHERE (a full phrase for the tracker). Spots + clothes: Z5Npcs.java / Z5VerhaalPlekken.java.
# ---------------------------------------------------------------------------------------------------------------------------
NPCS = {
    # --- the story -----------------------------------------------------------------------------------------------------------------
    "z5_koningin": ("Koningin Zachtmoed de Derde", [
        "Kom eens hier, ridder. Een koningin mag knuffelen wie ze wil. Dat staat in de wet. Ik heb hem zelf geschreven.",
        "Mijn kroon is gebreid. Goud is koud, wol is warm. Een koningin moet warm zijn.",
        "Moeder Wolkwal zwemt al eeuwen door de lucht. Zolang wij zingen, blijft ze drijven."],
        _TROON),
    "z5_inspecteur": ("Inspecteur Snuffelaar", [
        "Elementair, beste ridder. Nou ja, bijna. Ik moet nog uitzoeken wat 'elementair' betekent.",
        "Een goede speurder kijkt naar de kleine dingen. Kruimels. Haartjes. Theevlekken. En soms naar koekjes. Voor de energie.",
        "Mijn vergrootglas heeft al drie zaken opgelost. En één keer per ongeluk een tafelkleed aangestoken."],
        "in de zijkamer van de Raadszaal, bij het grote bewijsbord, boven aan de grote trap op de paleisheuvel (oostkant van Hoogvads)"),
    "z5_havenmeester": ("Havenmeester Windvaan", [
        "Welkom op de Luchthaven van Hoogvads! Drie pieren, honderd luchtschepen per dag, en één heel strenge duif.",
        "Ik weet precies wie er aankomt en wie er vertrekt. Het staat allemaal in mijn logboek. Nou ja, bijna allemaal.",
        "Wil je reizen? Ik heb de reiskaart. Kies maar waarheen."],
        "aan de balie van de Luchthaven, het station met het glazen dak aan de noordflank van Hoogvads"),
    "z5_veerkracht": ("Kapitein Veerkracht", [
        "De Wolkwacht beschermt Hoogvads. Met schild, met pluim en met heel goede manieren.",
        "Op het oefenplein marcheren we elke ochtend. Links, rechts, links, knuffel. Dat is de laatste stap.",
        "Een ridder van de Orde in mijn Academie? Wat een eer. Zeg maar Veer."],
        "op het oefenplein van de Academie van de Wolkwacht, de witte vesting aan de zuidkant van Hoogvads"),
    "z5_plukje": ("Kadet Plukje", [
        "Ik ben de jongste kadet van de Wolkwacht. Mijn helm is nog te groot. Hij zakt steeds over mijn ogen.",
        "Mijn familie woont nog in het oosten. Ik mis ze heel erg.",
        "Ik oefen elke dag mijn salueren. Kijk. Au. Mijn helm."],
        "bij de kazerne van de Academie van de Wolkwacht, aan de zuidkant van Hoogvads"),
    "z5_professor": ("Professor Brilsma", [
        "De universiteit van Hoogvads is de oudste van de wereld. Het oudste boek is zo oud dat het niest als je het opent.",
        "Ik bestudeer Moeder Wolkwal. Haar adem, haar hartslag, haar dromen. Vooral haar dromen.",
        "Wist je dat een wolkwal slaapt met één oog open? Ik ook. Tijdens vergaderingen."],
        "in de bibliotheek-rotonde van de universiteit aan de noordkant van Hoogvads"),
    "z5_kok": ("Kok Pollepel", [
        "Soep! Warme soep! Voor iedereen die mager uit het oosten komt. Een kom vol vads, met een beetje peterselie.",
        "Mijn ketel is zo groot dat ik er een trapje voor nodig heb. En een lange pollepel. Vandaar mijn naam.",
        "Wie mager binnenkomt, gaat vol weg. Dat is de regel van de Grote Soepkeuken."],
        "bij de reuzenketel in de Grote Soepkeuken, aan de zuidkant van de markt van Hoogvads"),
    "z5_mientje": ("Mager Mientje", [
        "Ik kom uit het oosten. Daar is alles grauw. Hier is alles roze. Ik moet nog wennen aan al die kleur.",
        "Deze soep is het lekkerste wat ik ooit gegeten heb. Ik eet hem heel langzaam. Zodat hij langer duurt.",
        "'s Nachts kan ik niet zo goed slapen. Dan kijk ik naar de sterren. En naar wie er rondsluipt."],
        "aan de lange tafel in de Grote Soepkeuken, aan de zuidkant van de markt van Hoogvads"),
    # --- the Hoge Raad: the four suspects ----------------------------------------------------------------------------------------
    "z5_gouddraad": ("Raadsheer Gouddraad", [
        "Ik ben de penningmeester van de Hoge Raad. Ik tel alles. Munten, knopen, sterren. Gisteren telde ik mijn tenen. Tien!",
        "De bank van Hoogvads heeft de dikste kluis van de wereld. Er zit vooral snoep in.",
        "Ik vlieg vaak weg. Maar dat is privé. Heel privé. Hmpf."],
        "aan de balie van de bank, aan de noordkant van de markt van Hoogvads"),
    "z5_inktvlek": ("Raadsvrouwe Inktvlek", [
        "Ik ben de archivaris. Ik bewaar alle oude boeken van de Orde. Ssst. Boeken slapen.",
        "Thee en boeken gaan niet samen. Dat weet iedereen. Ook ik. Nu wel.",
        "De oudste boeken liggen in de Raadszaal. De sleutel heb ik. En de Kanselarij heeft de reserve."],
        "in het archief van de universiteit, aan de noordkant van Hoogvads"),
    "z5_donsveer": ("Generaal Donsveer", [
        "Ik ben de bevelhebber van de Wolkwacht. Ik heb geen tijd voor praatjes. Ik heb wel tijd voor koekjes.",
        "Hmpf. Wat moet je? Ik ben bezig. Met... bevelen. Ja. Heel belangrijke bevelen.",
        "La la laaa... Wat? Niks. Ik zong niet. Generaals zingen niet."],
        "boven in de toren van de Academie van de Wolkwacht, aan de zuidkant van Hoogvads"),
    "z5_zachtpels": ("Kanselier Zachtpels", [
        "Ah, de dappere ridder! Wat een eer. Een koekje? Ik heb er altijd een paar bij me. Voor vrienden.",
        "Ik ben de rechterhand van de Koningin. Haar linkerhand is ook heel aardig, maar die heeft minder te doen.",
        "Hoogvads is zo... warm. Zo roze. Zo... gedeeld. Heerlijk. Echt. Heerlijk."],
        "in de Kanselarij, de werkkamer naast de troonzaal in het paleis, aan de oostkant van Hoogvads"),
    # --- the refugees (De Uitgevadsten) ----------------------------------------------------------------------------------------
    "z5_pluisje": ("Pluisje", [
        "Ik heb mijn knuffel kwijt. Hij heet Flap. Hij heeft één oor. Het andere oor is ook kwijt.",
        "Thuis was alles grijs. Hier mag ik roze zijn. Kijk, mijn wangen worden al roze!",
        "Mama zegt dat we hier veilig zijn. Omdat de walvis zo hoog zwemt."],
        "in de hoek van de Grote Soepkeuken, aan de zuidkant van de markt van Hoogvads"),
    "z5_grauwsnor": ("Opa Grauwsnor", [
        "Ik heb lopend de Bezette Landen overgestoken. Met mijn kleinkinderen op mijn rug. Allemaal tegelijk.",
        "Mijn snor was vroeger roze. Hij wordt weer roze, zeggen ze. Met genoeg soep.",
        "Ik schrijf elke dag een brief naar huis. Ik weet niet of ze aankomen. Maar schrijven helpt."],
        "op een bank in de Grote Soepkeuken, aan de zuidkant van de markt van Hoogvads"),
    "z5_vluchteling_1": ("Uitgevadste Grijsje", [
        "Is dit echt soep? Met smaak? Ik dacht dat soep altijd naar niks smaakte.",
        "In het oosten zeggen de posters dat delen dom is. Hier delen ze alles. Ik snap het nog niet helemaal.",
        "Dank je wel. Echt. Dank je wel."], "in de rij voor de ketel in de Grote Soepkeuken van Hoogvads"),
    "z5_vluchteling_2": ("Uitgevadste Stofje", [
        "We zijn over de Tolbrug gevlucht. Aan een vlieger. Niet naar beneden kijken, zei mama.",
        "Mijn buik maakt een geluid. Dat is geen honger meer. Dat is blijdschap.",
        "Mag ik straks nog een kom? Voor mijn broertje. Hij is heel klein."], "in de rij voor de ketel in de Grote Soepkeuken van Hoogvads"),
    "z5_vluchteling_3": ("Uitgevadste Mergel", [
        "Ik was vroeger bakker. Nu ben ik vooral moe. Maar morgen misschien weer bakker.",
        "De Mika's namen al ons meel mee. En onze liedjes. Hoe neem je een liedje mee?",
        "Hier ruikt alles naar soep en bloemen. Ik huil een beetje. Dat is van het goede soort."], "in de rij voor de ketel in de Grote Soepkeuken van Hoogvads"),
    "z5_vluchteling_4": ("Uitgevadste Kiezel", [
        "Ik heb een steentje meegenomen van thuis. Het is grijs. Maar het is van thuis.",
        "Op de walvis is het zo stil en zacht. Alsof je op een wolk woont. Dat doe je ook.",
        "Ik ga later bij de Wolkwacht. Dan bescherm ik de soepkeuken."], "in de rij voor de ketel in de Grote Soepkeuken van Hoogvads"),
    # --- the city ------------------------------------------------------------------------------------------------------------------
    "z5_zuster": ("Zuster Zachtzang", [
        "Welkom in de Ordekathedraal. Hier zingen we elke avond het lied van de Eerste Knuffel.",
        "Kijk omhoog. Het licht door het glas valt in alle kleuren. Ook in kleuren die nog geen naam hebben.",
        "De Grote Poort is zo hoog dat de wolken er soms doorheen drijven. Dan zingen we harder."],
        "bij het altaar van de Eerste Knuffel, helemaal achterin de Ordekathedraal (westkant van Hoogvads)"),
    "z5_hofdame": ("Hofdame Breinaald", [
        "Ik brei de kroon van de Koningin. Elke winter een nieuwe. Deze heeft een extra bolletje. Voor de gezelligheid.",
        "Een koningin met een koude kop is een chagrijnige koningin. Daarom brei ik.",
        "Er is een draadje los uit de kroon. Niet aan de Koningin vertellen."],
        "op de binnenplaats van het paleis, aan de oostkant van Hoogvads"),
    "z5_stekje": ("Tuinguh Stekje", [
        "Het Staartpark ligt op de staart van Moeder Wolkwal. Als ze zwemt, wiebelen de bomen een beetje mee.",
        "Bloesembomen hebben dorst. Ik ook. Ik deel mijn gieter met ze.",
        "Kijk uit voor de vijver. De eendjes zijn van wolk. Ze kwaken in mist."],
        "bij de vijver in het Staartpark, helemaal achterin Hoogvads op de staart van de walvis (oostkant)"),
    "z5_lientje": ("Kraamguh Lientje", [
        "Markt! Markt! Hier vind je alles. Behalve rust. Rust is uitverkocht.",
        "Wil je iets verkopen? Of kopen? Het veilinghuis is hier, bij mij.",
        "Op de markt van Hoogvads verkopen ze zelfs wolken. In zakjes. Ze zijn wel snel op."],
        _MARKT + ", bij de kraam met de gestreepte luifel"),
    "z5_bakker": ("Bakker Wolkbol", [
        "Wolkbollen! Zo luchtig dat ze wegzweven als je niet oplet!",
        "Ik bak elke ochtend duizend broodjes voor de soepkeuken. En één voor mij. Een grote.",
        "Bij mij koop je drankjes en lekkers voor onderweg."],
        _MARKT + ", bij het bakkerskraampje met het dakje"),
    "z5_kleermaker": ("Kleermaker Zoompje", [
        "Kleren maken de guh. Of de guh maakt de kleren. Ik ben er nog niet uit.",
        "Wil je er deftig uitzien voor de Koningin? Kom maar even passen.",
        "Ik heb een cape gemaakt voor de Kanselier. Hij wilde hem extra lang. Tot op de grond. Vreemd."],
        _MARKT + ", bij de kraam met de stoffen"),
    "z5_smid": ("Smid Aambeeld", [
        "Mijn hamer zingt. Tink, tink, tonk. De tonk is een vals noot. Die hoort erbij.",
        "Wapens van de Wolkwacht? Ik maak ze allemaal. Zacht van buiten, sterk van binnen.",
        "Kom maar binnen als je iets wilt laten maken."],
        _MARKT + ", bij de smidse aan de zuidkant"),
    "z5_juwelier": ("Juwelier Glimmertje", [
        "Edelstenen inzetten? Uithalen? Ik doe het met een pincet en heel veel geduld.",
        "Glimmen is niet ijdel. Glimmen is gezellig.",
        "De Hertogin van de Grauwe Vloot schijnt tien ringen te dragen. Aan haar TENEN. Wat een smaak."],
        _MARKT + ", bij het glazen kraampje aan de noordkant"),
    "z5_stalmeester": ("Stalmeester Hoefje", [
        "Je rijdier mag hier uitrusten. Wolkenhooi, warme stallen, en een liedje voor het slapengaan.",
        "Pluimguhs vinden het hier heerlijk. Zo dicht bij de wolken.",
        "Wil je naar de stal? Kom maar."],
        _HAVEN + ", bij de stallen naast het station"),
    "z5_kadet_dons": ("Kadet Dons", [
        "Ik ben verdwaald. Weer. Hoogvads heeft zoveel straten. En ze lopen allemaal omhoog.",
        "Weet jij waar de Academie is? Ik moet om twaalf uur salueren.",
        "Ik ben heel dapper. Alleen niet als ik verdwaald ben."],
        "op de westelijke pier van de Luchthaven, aan de noordflank van Hoogvads"),
    # --- the Ambachtsgilde (professions capital; T-INT: the teachers open t-ambacht's workshops, HoogvadsGilde) --------------------------------------------------------------------
    "z5_gildemeester": ("Gildemeester Duimdop", [
        "Welkom in het Ambachtsgilde! Hoogvads is de hoofdstad van alle ambachten. Hier leer je de Meesterwerken.",
        "Mijnen, houthakken, plukken, vissen, imkeren, smeden, naaien, koken en brouwen. Negen ambachten, één gilde.",
        "De werkplaatsen zijn open! Praat met een leermeester en je staat zo aan de werkbank."],
        _GILDE + ", in de grote hal"),
    "z5_leermeester_koken": ("Leermeester Kookguh Peper", [
        "Koken is liefde in een pan. Met zout. Niet te veel zout.",
        "De recepten van Hoogvads zijn de lekkerste van de wereld. Dat zeg ik, en ik ben eerlijk.",
        "Kom maar mee naar de keuken. Daar leer ik je de Meesterrecepten."], _GILDE + ", in de keuken"),
    "z5_leermeester_brouwen": ("Leermeester Brouwguh Bubbel", [
        "Een drankje brouwen is als een liedje zingen. Als het borrelt, zit je goed.",
        "Mijn ketel doet 'plop'. Dat betekent dat hij blij is.",
        "Kom maar mee naar de brouwerij. Daar leer ik je de Meesterdrankjes."], _GILDE + ", in de brouwerij"),
    "z5_leermeester_smeden": ("Leermeester Smidguh Vonk", [
        "Een goed zwaard is zacht voor guhs en hard voor grauw.",
        "Maatwerk! Zelfgemaakte uitrusting met je eigen bonussen. Daar draait het om.",
        "Kom maar mee naar de smidse. Daar leer ik je het Meestersmeden."], _GILDE + ", in de smidse"),
    "z5_leermeester_naaien": ("Leermeester Naaiguh Vingerhoedje", [
        "Ik naai een reuzenquilt van alle zones. Elke zone een lapje. Hoogvads wordt het roze lapje.",
        "Een steek, een knoop, een knuffel. Zo naai je.",
        "Kom maar mee naar het atelier. Daar leer ik je het Meesternaaien."], _GILDE + ", in het naaiatelier"),
    "z5_leermeester_imkeren": ("Leermeester Imker Zoemzoet", [
        "Ik kom uit Honingtak. De bijen van Hoogvads maken wolkenhoning. Die is zo licht dat hij omhoog druppelt.",
        "Een eigen bijenkorf? Dat kan! Kom maar mee naar de bijenstal.",
        "Bijen zijn de hardste werkers van de wereld. Na Kok Pollepel."], _GILDE + ", bij de bijenstal"),
    # --- the 4th-class teasers (not playable) -----------------------------------------------------------------------------------
    "z5_vadszanger": ("Vadszanger Lieflied", [
        "La-la-laaa! Ik ben een Vadszanger. Mijn liedjes maken guhs sterker en wezens zachter.",
        "Met mijn guhfluit speel ik een liedje dat je wonden dicht neuriet. Nou ja, bijna. Ik oefen nog.",
        "Binnenkort mag jij ook een Vadszanger worden. Kijk maar eens in mijn vitrine."],
        "voor de trappen van de Ordekathedraal, bij de glazen vitrine met de guhfluit (westkant van Hoogvads)"),
    "z5_pluisninja": ("Pluisninja Schaduwdons", [
        "...",
        "Je zag me niet. Dat is goed. Een Pluisninja zie je pas als het te laat is. Voor een knuffel.",
        "Snel, zacht, onzichtbaar. Binnenkort kun jij het ook. Kijk in mijn vitrine. Stil."],
        "in een donker hoekje bij het oefenplein van de Academie van de Wolkwacht, bij de vitrine met de pluisdolkjes"),
}
BURGERS = {
    "z5_burger_1": ("Feestguh Confetti", "op de Zachtmoedlaan, bij het plein voor de kathedraal", [
        "Elke dag is feest in Hoogvads. Vandaag vieren we dinsdag!",
        "Ik heb confetti in mijn oren. Al drie weken. Het hoort erbij.",
        "Heb je de walvis al zien knipogen? Ze doet het één keer per dag. Ik wacht al uren."]),
    "z5_burger_2": ("Wandelguh Zonnig", _MARKT, [
        "Hoogvads is zo mooi. Ik woon hier al veertig jaar en ik ben nog steeds verbaasd.",
        "Als je over de rand kijkt, zie je de zee. Heel, heel diep onder ons.",
        "Ik heb gehoord dat er een spion in de stad is. Spannend. Eng. Maar vooral spannend."]),
    "z5_burger_3": ("Wolkguh Nevel", "bij het plein voor de kathedraal", [
        "Ik ben een wolkguh. Ik ben hier geboren, op de rug van de walvis.",
        "Soms voel je haar ademen. De straten gaan dan heel zachtjes op en neer.",
        "Moeder Wolkwal slaapt de laatste tijd zo onrustig. Ze bromt in haar slaap."]),
    "z5_burger_4": ("Kleintje Kwispel", "bij de vijver in het Staartpark", [
        "Ik vang wolkeendjes! Ze lossen op als je ze knuffelt. Dan maak ik nieuwe.",
        "Mijn mama zegt dat de staart van de walvis een glijbaan is. Ik mag er nog niet op.",
        "Ben jij een ridder? Mag ik je zwaard vasthouden? Het is een kussen! Leuk!"]),
    "z5_burger_5": ("Uitkijkguh Verrekijk", "in de Ooggaarde, bij het balkon boven het oog van de walvis", [
        "Vanaf hier zie je het oog van Moeder Wolkwal. Het is zo groot als een vijver. Met wimpers.",
        "Als je zwaait, knippert ze terug. Echt waar.",
        "In het oosten hangt een grauwe streep aan de horizon. Ik kijk er liever niet naar."]),
    "z5_burger_6": ("Reizigster Koffertje", _HAVEN, [
        "Ik vlieg vandaag naar Kandijkom. Of morgen. Of overmorgen. Ik kan niet kiezen.",
        "De Luchthaven heeft een klokkentoren. Hij slaat elk uur. Behalve als de klokkenluider slaapt.",
        "Wist je dat je vanaf de pieren zo de wolken in kunt springen? Niet doen. Echt niet doen."]),
}
for _id, (_naam, _waar, _r) in BURGERS.items():
    NPCS[_id] = (_naam, _r, _waar)
WACHTEN = {
    "z5_wacht_1": ("Wolkwacht Schildpluim", "aan de noordkant van de grote trap naar de paleisheuvel"),
    "z5_wacht_2": ("Wolkwacht Veerhelm", "aan de zuidkant van de grote trap naar de paleisheuvel"),
    "z5_wacht_3": ("Wolkwacht Poortdons", "bij de Grote Poort van de Ordekathedraal"),
    "z5_wacht_4": ("Wolkwacht Windpluis", "bij het kampvuur van de Luchthaven"),
    "z5_wacht_5": ("Wolkwacht Staartveer", "bij het kampvuur in het Staartpark"),
    "z5_wacht_6": ("Wolkwacht Ooglid", "bij het kampvuur in de Ooggaarde"),
}
WACHT_REGELS = [
    "Steek het kampvuur aan: hier word je wakker als je flauwvalt. Ook in de veiligste stad van de wereld.",
    "De Wolkwacht bewaakt Hoogvads. Met schild, pluim en een fluitje voor als het spannend wordt.",
    "Hoogvads is veilig, ridder. Hier vecht niemand. Behalve om het laatste koekje."]
for _id, (_naam, _waar) in WACHTEN.items():
    NPCS[_id] = (_naam, WACHT_REGELS, _waar + " in Hoogvads")

# ---------------------------------------------------------------------------------------------------------------------------
# Where: POIs / areas / campfires / boss arenas (vadsrijk.npc.plek.<kind>.<id>)
# ---------------------------------------------------------------------------------------------------------------------------
PLEK_POI = {
    "z5_opstijgpier": "op de Opstijgpier van Lampionhaven, de lange pier onder de kop van de walvis (zone 4)",
    "z5_aankomst": "aan de middelste pier van de Luchthaven, aan de noordflank van Hoogvads",
    "z5_laan": _LAAN,
    "z5_troonzaal": _TROON,
    "z5_raadszaal": _RAAD,
    "z5_lessenaar": "bij de lessenaar met de oude boeken, midden in de Raadszaal op de paleisheuvel",
    "z5_kastje": "bij de kastjes van de kadetten in de kazerne van de Academie van de Wolkwacht (zuidkant van Hoogvads)",
    "z5_soepkeuken": _SOEP,
    "z5_logboek": "bij het grote logboek op de balie van de Luchthaven, aan de noordflank van Hoogvads",
    "z5_spuitgat": "bij het middelste spuitgat van Moeder Wolkwal, tussen de kathedraal en de Ooggaarde (westkant van Hoogvads)",
    "z5_wolkwaldroom": "in De Wolkwaldroom, de droomzaal diep in Moeder Wolkwal (door het middelste spuitgat naar beneden)",
    "z5_ooggaarde_balkon": "op het balkon van de Ooggaarde, boven het oog van Moeder Wolkwal (westpunt van Hoogvads, zuidkant)",
    "z5_pier_oost": "aan het eind van de oostelijke pier van de Luchthaven, aan de noordflank van Hoogvads",
    "z5_kanselarij": "bij het bureau in de Kanselarij, de werkkamer naast de troonzaal in het paleis",
    "z5_sterrenwacht": "in de sterrenwacht op de hoek van de universiteit (noordoostkant van Hoogvads)",
    "z5_spuitgat_noord": "bij het noordelijke spuitgat van Moeder Wolkwal, tussen de kathedraal en de Ooggaarde",
    "z5_spuitgat_zuid": "bij het zuidelijke spuitgat van Moeder Wolkwal, tussen de kathedraal en de Ooggaarde",
    "z5_parcours_1": "bij de eerste paal van het Wolkenparcours, in de noordwesthoek van het oefenplein van de Academie",
    "z5_parcours_2": "bij de tweede paal van het Wolkenparcours, in de noordoosthoek van het oefenplein van de Academie",
    "z5_parcours_3": "bij de derde paal van het Wolkenparcours, in de zuidoosthoek van het oefenplein van de Academie",
    "z5_parcours_4": "bij de vierde paal van het Wolkenparcours, in de zuidwesthoek van het oefenplein van de Academie",
    "z5_academie": "op het oefenplein van de Academie van de Wolkwacht, aan de zuidkant van Hoogvads",
    "z5_bloesemboom_1": "bij de eerste bloesemboom in het Staartpark, zuidwest van de vijver",
    "z5_bloesemboom_2": "bij de tweede bloesemboom in het Staartpark, zuidoost van de vijver",
    "z5_bloesemboom_3": "bij de derde bloesemboom in het Staartpark, noordoost van de vijver",
    "z5_bloesemboom_4": "bij de vierde bloesemboom in het Staartpark, noordwest van de vijver",
    "z5_flap": "achter het beeld van de Eerste Knuffel, in de apsis helemaal achterin de Ordekathedraal",
    "z5_roosvenster_noord": "onder het noordelijke roosvenster in het transept van de Ordekathedraal",
    "z5_roosvenster_zuid": "onder het zuidelijke roosvenster in het transept van de Ordekathedraal",
    "z5_eerste_knuffel": "bij het gouden beeld van de Eerste Knuffel, achterin de Ordekathedraal",
    "z5_uitkijk_oog": "op het balkon van de Ooggaarde, boven het oog van Moeder Wolkwal",
    "z5_uitkijk_geveltoren": "bovenin de zuidelijke geveltoren van de Ordekathedraal (de wenteltrap op)",
    "z5_uitkijk_staartvin": "bovenop de staartvin van Moeder Wolkwal, achter het Staartpark",
    "z5_uitkijk_sterrenwacht": "op het dak van de sterrenwacht van de universiteit",
}
PLEK_GEBIED = {
    "z5_hoogvads": "in Hoogvads, de stad op de rug van Moeder Wolkwal",
    "z5_luchthaven": "op de Luchthaven aan de noordflank van Hoogvads",
    "z5_staartpark": "in het Staartpark, op de staart van Moeder Wolkwal (oostkant van Hoogvads)",
    "z5_ooggaarde": "in de Ooggaarde, de tuin boven het oog van Moeder Wolkwal (westkant van Hoogvads)",
    "z5_wolkwaldroom": "in De Wolkwaldroom, diep in Moeder Wolkwal (door het middelste spuitgat)",
}
GEBIED_NAMEN = {
    "z5_hoogvads": "Hoogvads", "z5_luchthaven": "De Luchthaven", "z5_staartpark": "Het Staartpark", "z5_ooggaarde": "De Ooggaarde",
    "z5_wolkwaldroom": "De Wolkwaldroom",
}
PLEK_KAMPVUUR = {
    "z5_luchthaven": "bij het station van de Luchthaven, bij Wolkwacht Windpluis",
    "z5_hoogvads": "op de markt van Hoogvads, naast de fontein",
    "z5_staartpark": "bij de vijver in het Staartpark",
    "z5_ooggaarde": "in de Ooggaarde, vlak bij het balkon boven het oog",
}
PLEK_BAAS = {
    "z5_wolkwaldroom": "in De Wolkwaldroom, diep in Moeder Wolkwal (spring in het middelste spuitgat tussen de kathedraal en de Ooggaarde)",
}
UITKIJKEN = {
    "z5_uitkijk_oog": "Het Ooggaarde-balkon",
    "z5_uitkijk_geveltoren": "De Geveltoren",
    "z5_uitkijk_staartvin": "De Staartvin",
    "z5_uitkijk_sterrenwacht": "De Sterrenwacht",
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quest items (Z5VerhaalRegistry.QUEST_ITEMS): name, vanilla texture
# ---------------------------------------------------------------------------------------------------------------------------
QUEST_ITEMS = {
    "speurdersboekje": ("Speurdersboekje (rechtsklik: het bewijsbord)", "minecraft:item/writable_book"),
    "bladzijde": ("De gescheurde bladzijde", "minecraft:item/paper"),
    "bevelbrief": ("Bevelbriefje met een pootjesstempel", "minecraft:item/paper"),
    "grauwe_brief": ("Grauwe brief (aan de Ringvinger)", "minecraft:item/paper"),
    "wolkbes": ("Wolkbes", "minecraft:item/glow_berries"),
    "wolbol": ("Bolletje roze wol", "minecraft:block/pink_wool"),
    "brieven": ("Brieven van Opa Grauwsnor", "minecraft:item/paper"),
    "flap": ("Knuffelkonijn Flap (één oor)", "minecraft:item/rabbit_foot"),
    "kaasje": ("Stukje rijpe kaas (voor een kaastest)", "minecraft:item/cookie"),
}

# ---------------------------------------------------------------------------------------------------------------------------
# The detective work (speurwerk.json): clues, suspects, interrogations. Speakers: "npc" (the one interrogated), "jij".
# ---------------------------------------------------------------------------------------------------------------------------
BEWIJS = {   # id: (title, text, where found)
    "haren": ("Grijze haartjes", "Op de lessenaar in de Raadszaal liggen lange, zilvergrijze haartjes. Geen guh-pluis: dat is korter en krulliger."),
    "theevlek": ("De theevlek", "Raadsvrouwe Inktvlek morste thee op bladzijde twaalf en durfde het niet te zeggen. De gescheurde bladzijde heeft ze "
                                "niet aangeraakt. De reservesleutel van het archief ligt bij de Kanselarij."),
    "bevelbrief": ("Het bevelbriefje", "In het kastje van Kadet Plukje: 'Laat vannacht de achterdeur van de Raadszaal open. De Hoge Raad.' Eronder een "
                                       "stempel: een rond pootje met kussentjes. Guhs hebben geen kussentjes."),
    "soepkom": ("Onder de soepkom", "Kadet Plukje kreeg de briefjes in de Grote Soepkeuken, onder zijn soepkom. Ze beloofden dat zijn familie uit het "
                                    "oosten mocht komen."),
    "kaasvoorraad": ("De verdwenen kaas", "Kok Pollepel: uit de voorraad verdwijnt elke nacht kaas. Alleen kaas. Zoveel kaas eet geen guh."),
    "staart": ("Een guh met een staart", "Mager Mientje zag 's nachts een deftige guh in een lange cape uit de voorraadkamer sluipen. Onder de cape "
                                          "zwiepte een lange, grijze staart."),
    "logboek": ("Het nachtlogboek", "In het logboek van de Luchthaven: elke donderdagnacht vertrekt een stil bootje naar het oosten, over de Wolkenzee. Geboekt door "
                                    "'Z.P.'. Geen naam, alleen letters."),
    "zegel": ("Het zegel van de Kanselarij", "Raadsheer Gouddraad vliegt elke week op zijn eigen naam naar zijn moeder in Lampionhaven. Maar de "
                                             "Z.P.-boeking draagt het zegel van de Kanselarij. Dat zegel gebruikt maar één guh."),
    "liedje": ("Het geheime liedje", "Generaal Donsveer was 's nachts niet in de kazerne: hij oefende stiekem een verjaardagslied voor de Koningin. Zijn "
                                     "vacht is sneeuwwit, niet grijs."),
    "kaastest": ("De kaastest", "Kanselier Zachtpels sloeg de soep af ('te warm, te... gedeeld'), maar griste het stukje rijpe kaas uit je poot. Zijn "
                                "snorharen trilden en zijn monocle besloeg."),
}
VERDACHTEN = {   # id: (npc, line on the board, cleared by clue | None = the traitor)
    "gouddraad": ("z5_gouddraad", "Raadsheer Gouddraad, de penningmeester. Vliegt vaak en stiekem weg.", "zegel"),
    "inktvlek": ("z5_inktvlek", "Raadsvrouwe Inktvlek, de archivaris. Heeft de sleutel van de oude boeken.", "theevlek"),
    "donsveer": ("z5_donsveer", "Generaal Donsveer, de bevelhebber. De mol zit in zijn Wolkwacht, en hij is nergens 's nachts.", "liedje"),
    "zachtpels": ("z5_zachtpels", "Kanselier Zachtpels, de rechterhand van de Koningin. Altijd lief, altijd koekjes.", None),
}
ZEKER = ["staart", "zegel", "kaastest"]   # (the board says these three point at the traitor)

VERHOREN = {
    "inktvlek": dict(npc="z5_inktvlek", bewijs="theevlek", sleutel=1, intro=[
        ("npc", "Ssst! Fluisteren in het archief. De boeken slapen. Wat kom je doen?"),
        ("jij", "De gescheurde bladzijde is gestolen uit de Raadszaal. U heeft de sleutel."),
        ("npc", "Gestolen? Oh nee. Oh nee, oh nee. Vraag maar wat je wilt. Zachtjes.")], vragen=[
        ("Wat staat er op die bladzijde?", [
            ("npc", "Het oudste verhaal van de Orde. Over een kat aan de rand van de wereld die nooit genoeg had."),
            ("npc", "En op de achterkant... het slaaplied van Moeder Wolkwal. Wie het achterstevoren zingt, geeft haar een grauwe droom. "
                    "Daarom lag hij achter slot.")]),
        ("Waarom bent u zo zenuwachtig?", [
            ("npc", "Ik... oké. Oké! Ik heb thee gemorst. Op bladzijde twaalf. Van het Grote Knuffelboek. Een vlek zo groot als een koekje."),
            ("npc", "Ik durfde het niet te zeggen. Maar de gescheurde bladzijde heb ik nooit aangeraakt. Echt niet."),
            ("npc", "En de sleutel? Ik heb er één. De reservesleutel ligt bij de Kanselarij. Altijd al.")]),
        ("Waar was u vannacht?", [
            ("npc", "Hier. Tussen de boeken. Ik slaap in de leesstoel. De boeken vinden dat gezellig.")])]),
    "plukje": dict(npc="z5_plukje", bewijs="soepkom", sleutel=2, intro=[
        ("jij", "Kadet Plukje. In jouw kastje lag een bevelbriefje. 'Laat de achterdeur van de Raadszaal open.'"),
        ("npc", "...Mijn helm zakt weer over mijn ogen. Dan hoef ik je niet aan te kijken."),
        ("jij", "Niemand is boos. Ik wil alleen weten wat er gebeurd is.")], vragen=[
        ("Wie heeft je dat briefje gegeven?", [
            ("npc", "Dat weet ik niet! Er stond 'De Hoge Raad' onder. Dus ik dacht dat het mocht. De Raad is de baas van iedereen.")]),
        ("Waarom deed je het?", [
            ("npc", "Mijn familie woont nog in het oosten. In het grauw. In het eerste briefje stond dat ze mochten komen. Als ik goed luisterde."),
            ("npc", "Ik wilde ze zo graag weer zien...")]),
        ("Hoe kreeg je de briefjes?", [
            ("npc", "In de Grote Soepkeuken! Elke avond eet ik daar soep. En dan lag er een briefje onder mijn kom. Ik heb nooit gezien wie het neerlegde."),
            ("npc", "Het spijt me zo. Ik meld me straks zelf bij Kapitein Veerkracht.")])]),
    "kok": dict(npc="z5_kok", bewijs="kaasvoorraad", sleutel=1, intro=[
        ("npc", "Een ridder in mijn keuken! Soep? Natuurlijk wil je soep. Iedereen wil soep."),
        ("jij", "Graag. En ik heb een paar vragen. Over wat er 's nachts in de soepkeuken gebeurt.")], vragen=[
        ("Wie komt hier allemaal eten?", [
            ("npc", "Iedereen! Vluchtelingen, kadetten, raadsleden. De Kanselier komt vaak kijken. Lief van hem. Al eet hij nooit iets.")]),
        ("Mist u weleens iets?", [
            ("npc", "Kaas. Elke nacht. Alleen kaas! Geen brood, geen soep, geen koekjes. Alleen de rijpste, stinkendste kaas."),
            ("npc", "Zoveel kaas eet geen guh. Het luikje van de voorraadkamer piept als iemand het opendoet. Mientje slaapt er vlak naast.")]),
        ("Hoe groot is die ketel eigenlijk?", [
            ("npc", "Zo groot dat er vorige week een kadet in viel. Hij zwom er tevreden rondjes in. Daarna heb ik er een deksel op gedaan.")])]),
    "mientje": dict(npc="z5_mientje", bewijs="staart", sleutel=0, intro=[
        ("npc", "Jij hebt soep uitgedeeld. Aan Grijsje en Stofje en Mergel en Kiezel. Dat zag ik. Jij bent lief."),
        ("jij", "Kok Pollepel zegt dat jij 's nachts naast de voorraadkamer slaapt.")], vragen=[
        ("Heb je 's nachts iemand gezien?", [
            ("npc", "Ja. Een deftige guh met een lange cape. Hij sloop uit de voorraadkamer met zijn armen vol kaas."),
            ("npc", "En onder zijn cape... zwiepte een staart. Een lange, grijze staart. Guhs hebben geen staart. Toch?"),
            ("npc", "Ik zei niks. In het oosten leer je stil te zijn.")]),
        ("Hoe ben je ontsnapt uit het oosten?", [
            ("npc", "Dat is een lang verhaal. Ik vertel het je straks. Als je wilt luisteren. Niemand vroeg het nog.")]),
        ("Gaat het nu goed met je?", [
            ("npc", "Steeds beter. Mijn wangen worden roze. Kijk. Een beetje.")])]),
    "gouddraad": dict(npc="z5_gouddraad", bewijs="zegel", sleutel=1, intro=[
        ("npc", "Een verhoor? Van MIJ? De penningmeester van de Hoge Raad? Hmpf. Vraag maar. Snel. Ik moet nog knopen tellen."),
        ("jij", "U vliegt vaak 's nachts weg. Waarheen?")], vragen=[
        ("Waarom vliegt u stiekem weg?", [
            ("npc", "Stiekem? Ik vlieg naar mijn moeder. In Lampionhaven. Elke week. Ze is oud en ze bakt de lekkerste lampionkoekjes."),
            ("npc", "Ik zeg het niet tegen de Raad, omdat ze me dan een moederskindje noemen. Wat ik ben. Met trots.")]),
        ("Wie is 'Z.P.' in het logboek?", [
            ("npc", "Z.P.? Ik boek altijd op mijn eigen naam. G-O-U-D-D-R-A-A-D. Met drie A's. Nou ja, twee."),
            ("npc", "Laat zien. Hmm. Die boeking heeft het zegel van de Kanselarij. Het kroontje met de veer. Dat gebruikt alleen de Kanselier."),
            ("npc", "Maar de Kanselier... die is toch zo aardig? Hij geeft iedereen koekjes.")]),
        ("Wat zit er in de kluis?", [
            ("npc", "Goud, zilver en vooral snoep. Een penningmeester moet ook iets lekkers hebben om te tellen.")])]),
    "donsveer": dict(npc="z5_donsveer", bewijs="liedje", sleutel=2, intro=[
        ("npc", "Hmpf. Een ridder. In mijn toren. Ik heb het druk."),
        ("jij", "Generaal, er zit een mol in uw Wolkwacht. En u was vannacht niet in de kazerne.")], vragen=[
        ("Wist u van Kadet Plukje?", [
            ("npc", "Plukje? Dat kind kan nog niet eens salueren zonder zijn helm te laten vallen. Een mol? Hmpf. Een muisje, hooguit.")]),
        ("Waarom bent u zo grijs?", [
            ("npc", "GRIJS? Mijn vacht is SNEEUWWIT. Kijk dan. Hier. Het is wit als verse wolk. Grijs. Pff.")]),
        ("Waar was u vannacht?", [
            ("npc", "...Kun je een geheim bewaren?"),
            ("npc", "Ik oefende. Een liedje. Voor de verjaardag van de Koningin. Op het dak van de toren, waar niemand het hoort."),
            ("npc", "Ik zing vals. Heel vals. Maar met heel mijn hart. Niet doorvertellen. Hmpf.")])]),
    "zachtpels": dict(npc="z5_zachtpels", bewijs="kaastest", sleutel=0, intro=[
        ("npc", "De dappere ridder! In mijn Kanselarij! Wat een eer. Een koekje? Ga zitten. Niet op die stoel, daar zit mijn cape."),
        ("jij", "Ik heb een paar vragen, Kanselier. En ik heb iets meegenomen uit de Grote Soepkeuken.")], vragen=[
        ("Bied hem een kom soep en een stukje rijpe kaas aan", [
            ("npc", "Soep? Nee nee, dank je. Te warm. Te... gedeeld."),
            ("npc", "Maar is dat... rijpe kaas? Mmm. Mag ik? Even proeven. Voor de beleefdheid."),
            ("jij", "(Hij grist de kaas uit je poot. Zijn snorharen trillen. Zijn monocle beslaat.)"),
            ("npc", "Ahem. Heerlijk. Ik bedoel: gewoon. Gewone kaas. Waar waren we?")]),
        ("Waar was u vannacht?", [
            ("npc", "Hier, natuurlijk. Brieven schrijven. Aan... vrienden. Verre vrienden. Heel verre vrienden.")]),
        ("Wie heeft de reservesleutel van het archief?", [
            ("npc", "De Kanselarij. Dus ik. Maar die ligt veilig in mijn la. Kijk maar, hier... hm. Vreemd. Hij ligt er niet."),
            ("npc", "Iemand moet hem gestolen hebben! Wat erg. Wat een verraad. Koekje?")])]),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quests. D(type, doel, n, text, bron, kans, plek): text = WHAT + WHO + WHERE; plek = where the tracker points.
# ---------------------------------------------------------------------------------------------------------------------------
QUESTS = []


def D(soort, doel, n, tekst, bron="", kans=0.0, plek=""):
    return dict(soort=soort, doel=doel, n=n, tekst=tekst, bron=bron, kans=kans, plek=plek)


def Q(id, soort, gever, titel, tekst, doelen, aanbod, bezig, klaar, munten=0, items=(), wapen=False, loot="", lv=56, na=(),
      inleveren=None, klasse=None, auto=False, praat=None, actie_aanname="", actie_klaar="", start_items=()):
    QUESTS.append(dict(id=id, soort=soort, gever=gever, titel=titel, tekst=tekst, doelen=doelen, aanbod=aanbod, bezig=bezig, klaar=klaar,
                       beloning=dict(xp=0, munten=munten, items=[list(i) for i in items], wapen=wapen, loot=loot), lv=lv, na=list(na),
                       inleveren=inleveren or gever, klasse=klasse, auto=auto, praat=praat or {}, actie_aanname=actie_aanname,
                       actie_klaar=actie_klaar, start_items=[list(i) for i in start_items]))


def item(id):
    return "guhs:vr_z5_npc_" + id


HQ = ["z5_hq01_stad", "z5_hq02_verraad", "z5_hq03_mol", "z5_hq04_soep", "z5_hq05_logboek", "z5_hq06_verhoren", "z5_hq07_bewijsbord",
      "z5_hq08_wolkwaldroom", "z5_hq09_slaaplied", "z5_hq10_stormrand"]
# (the previous zone: zone 4's last main quest is not known in this tree; Z5Haken also starts hq01 on the flag zone4_klaar)
VORIGE = "z4_hq07_opstijgpier"   # (T-INT: zone 4's last main quest, t-z4verhaal)

# ===== the main story of zone 5: "Verraad in de stad" (10) ===================================================================
Q(HQ[0], "hoofd", "z5_havenmeester", "De Zwevende Stad",
  "Moeder Wolkwal is laag over de zee gezakt. Vanaf de Opstijgpier van Lampionhaven vaart het eerste luchtschip omhoog: naar "
  "Hoogvads, de hoofdstad van alle guhs, op de rug van de walvis. Koningin Zachtmoed wacht op je.",
  [D("meld", "z5:aangekomen", 1, "Vlieg vanaf de Opstijgpier van Lampionhaven met het eerste luchtschip omhoog naar Hoogvads",
     plek="poi:z5_opstijgpier"),
   D("praat", "z5_havenmeester", 1, "Meld je bij Havenmeester Windvaan aan de balie van de Luchthaven (noordflank van Hoogvads)",
     plek="npc:z5_havenmeester"),
   D("gebied", "poi:z5_laan", 1, "Loop van de Luchthaven naar het zuiden, naar de Zachtmoedlaan midden in de stad", plek="poi:z5_laan"),
   D("praat", "z5_koningin", 1, "Ga naar Koningin Zachtmoed in de troonzaal: volg de laan naar het oosten, de grote trap op de "
     "paleisheuvel op en het paleis in", plek="npc:z5_koningin")],
  [("npc", "Welkom op de Luchthaven van Hoogvads! Eerste keer? Dat zie ik aan je mond. Die staat open."),
   ("npc", "De Koningin verwacht je. Loop naar het zuiden, de Zachtmoedlaan op, en dan naar het oosten tot de grote trap. Het paleis staat bovenop.")],
  "Naar Koningin Zachtmoed, in de troonzaal op de paleisheuvel.",
  [("npc", "Daar ben je. Kom eens hier. Nee, dichterbij. Zo. Een knuffel van een koningin, dat krijg je niet elke dag."),
   ("npc", "Vahoegnir zei dat je zou komen. Hij landde vorige week op het dak. De dakpannen zijn nog roze."),
   ("jij", "Wat een stad. Ik wist niet dat er zoveel guhs bij elkaar konden wonen."),
   ("npc", "Op de rug van Moeder Wolkwal. Ze zwemt al eeuwen door de lucht, en ze draagt ons. Omdat vads licht is."),
   ("npc", "Maar ik heb je niet alleen geroepen voor een knuffel. Er is iets ergs gebeurd. Ga zitten. Op een kussen. Er liggen er genoeg.")],
  munten=12000, items=[(item("kaasje"), 1)], lv=55, na=[VORIGE], inleveren="z5_koningin", auto=True)

Q(HQ[1], "hoofd", "z5_koningin", "Verraad in de stad",
  "Uit de Raadszaal is de gescheurde bladzijde gestolen: het oudste verhaal van de Orde. Er zit een verrader in de Hoge Raad. "
  "Inspecteur Snuffelaar helpt je speuren.",
  [D("meld", "z5:inspecteur", 1, "Meld je bij Inspecteur Snuffelaar in de zijkamer van de Raadszaal, naast het paleis op de paleisheuvel",
     plek="npc:z5_inspecteur"),
   D("gebied", "poi:z5_lessenaar", 1, "Onderzoek de lessenaar met de oude boeken, midden in de Raadszaal", plek="poi:z5_lessenaar"),
   D("meld", "z5_verhoor:inktvlek", 1, "Verhoor Raadsvrouwe Inktvlek, de archivaris, in het archief van de universiteit (noordkant van Hoogvads)",
     plek="npc:z5_inktvlek")],
  [("npc", "Vannacht is de gescheurde bladzijde gestolen. Uit de Raadszaal. Achter slot en grendel."),
   ("npc", "Op die bladzijde staat het oudste verhaal van de Orde. En op de achterkant... iets wat niemand mag zingen."),
   ("jij", "Wie kan erbij?"),
   ("npc", "Alleen de Hoge Raad. Vier guhs die ik al jaren vertrouw. Eén van hen is een verrader."),
   ("npc", "Inspecteur Snuffelaar zit in de zijkamer van de Raadszaal. Speur met hem. Stil. Niemand mag het merken.")],
  "Inspecteur Snuffelaar, de lessenaar in de Raadszaal, en dan Raadsvrouwe Inktvlek in het archief.",
  [("npc", "Grijze haartjes op de lessenaar. Een theevlek. En een reservesleutel bij de Kanselarij. Hmmm."),
   ("npc", "Je Speurdersboekje heb je al? Mooi. Alles wat je vindt, schrijf je erin. Rechtsklik en je ziet het bewijsbord."),
   ("npc", "Nog iets: Kapitein Veerkracht van de Academie zegt dat er een mol in de Wolkwacht zit. Daar moeten we heen.")],
  munten=12000, lv=56, na=[HQ[0]], inleveren="z5_inspecteur", start_items=[(item("speurdersboekje"), 1)],
  praat={"z5_inspecteur": [
      ("npc", "Aha! De ridder van de Orde. De Koningin zei al dat je kwam. Inspecteur Snuffelaar, aangenaam. Mijn snor is echt."),
      ("npc", "Hier, een Speurdersboekje. Alles wat je vindt, komt erin. Begin bij de lessenaar, daar lag de bladzijde."),
      ("npc", "En verhoor daarna Raadsvrouwe Inktvlek. Zij heeft de sleutel van de oude boeken. Ze zit in het archief van de universiteit.")]})

Q(HQ[2], "hoofd", "z5_inspecteur", "De mol in de Wolkwacht",
  "Iemand heeft de achterdeur van de Raadszaal opengelaten. Kapitein Veerkracht denkt dat er een mol in de Wolkwacht zit.",
  [D("praat", "z5_veerkracht", 1, "Praat met Kapitein Veerkracht op het oefenplein van de Academie van de Wolkwacht (zuidkant van Hoogvads)",
     plek="npc:z5_veerkracht"),
   D("gebied", "poi:z5_kastje", 1, "Doorzoek de kastjes van de kadetten in de kazerne van de Academie", plek="poi:z5_kastje"),
   D("meld", "z5_verhoor:plukje", 1, "Verhoor Kadet Plukje bij de kazerne van de Academie", plek="npc:z5_plukje")],
  [("npc", "De achterdeur van de Raadszaal stond open. Zonder braaksporen. Iemand met een sleutel... of iemand van de Wolkwacht."),
   ("npc", "Kapitein Veerkracht wacht op je op het oefenplein van de Academie. Kijk in de kastjes van de kadetten.")],
  "Kapitein Veerkracht, de kastjes in de kazerne, en dan Kadet Plukje.",
  [("npc", "Een bevelbriefje met een pootjesstempel. Pootjes met KUSSENTJES. En briefjes onder een soepkom in de Grote Soepkeuken."),
   ("npc", "Arme Plukje. Hij wilde alleen zijn familie terug. Iemand heeft hem gebruikt."),
   ("npc", "Op naar de Grote Soepkeuken. Kok Pollepel weet alles wat daar gebeurt. Neem honger mee.")],
  munten=13000, lv=56, na=[HQ[1]],
  praat={"z5_veerkracht": [
      ("npc", "Een ridder van de Orde! Welkom in mijn Academie. Zeg maar Veer."),
      ("npc", "Iemand in de Wolkwacht heeft de achterdeur van de Raadszaal opengelaten. Ik weet het zeker: de wacht stond er, en hij liet hem open."),
      ("npc", "Kijk in de kastjes van de kadetten, in de kazerne. Ik hoop dat ik ongelijk heb.")]})

Q(HQ[3], "hoofd", "z5_inspecteur", "Soep en geruchten",
  "De bevelbriefjes kwamen onder een soepkom in de Grote Soepkeuken. Daar eten de Uitgevadsten, de magere vluchtelingen uit het "
  "oosten. Wie daar 's nachts rondsluipt, wordt gezien.",
  [D("meld", "z5_verhoor:kok", 1, "Verhoor Kok Pollepel bij de reuzenketel in de Grote Soepkeuken (zuidkant van de markt)", plek="npc:z5_kok"),
   D("meld", "z5_soep:kom", 4, "Deel een kom soep uit aan de vier Uitgevadsten in de rij voor de ketel (klik op ze)", plek="poi:z5_soepkeuken"),
   D("meld", "z5_verhoor:mientje", 1, "Verhoor Mager Mientje aan de lange tafel in de Grote Soepkeuken", plek="npc:z5_mientje"),
   D("meld", "z5:herinnering", 1, "Luister naar het verhaal van Mager Mientje: hoe ze uit het oosten ontsnapte", plek="npc:z5_mientje")],
  [("npc", "De Grote Soepkeuken. Daar komt iedereen. Kadetten, raadsleden, en de Uitgevadsten uit het oosten."),
   ("npc", "Praat met Kok Pollepel. En help een handje: wie soep uitdeelt, hoort de geruchten.")],
  "Kok Pollepel, vier kommen soep, en Mager Mientje in de Grote Soepkeuken.",
  [("npc", "Een guh met een STAART. Die kaas steelt. Hmm. Hmmmm. Ik ken maar één soort wezen dat zo dol is op rijpe kaas."),
   ("npc", "Maar wie in de Raad zou er een staart onder zijn cape verstoppen?"),
   ("npc", "Volgende spoor: de Luchthaven. Havenmeester Windvaan houdt een logboek bij. Wie vertrekt er 's nachts stiekem?")],
  munten=13000, lv=57, na=[HQ[2]])

Q(HQ[4], "hoofd", "z5_inspecteur", "Wie vertrekt er stiekem?",
  "Een verrader moet zijn geheimen ergens heen brengen. De Luchthaven is de enige weg van de walvis af.",
  [D("praat", "z5_havenmeester", 1, "Praat met Havenmeester Windvaan aan de balie van de Luchthaven (noordflank van Hoogvads)",
     plek="npc:z5_havenmeester"),
   D("gebied", "poi:z5_logboek", 1, "Lees het nachtlogboek op de balie van de Luchthaven", plek="poi:z5_logboek"),
   D("meld", "z5_verhoor:gouddraad", 1, "Verhoor Raadsheer Gouddraad, de penningmeester, aan de balie van de bank op de markt",
     plek="npc:z5_gouddraad")],
  [("npc", "De Luchthaven. Havenmeester Windvaan schrijft elke vlucht op. Ook die van 's nachts."),
   ("npc", "En Raadsheer Gouddraad vliegt vaak weg. Heel vaak. Verhoor hem daarna, bij de bank op de markt.")],
  "Havenmeester Windvaan, het nachtlogboek, en Raadsheer Gouddraad bij de bank.",
  [("npc", "Gouddraad bezoekt zijn moeder. Wat een lieve penningmeester. Die kunnen we van de lijst halen."),
   ("npc", "Maar het zegel van de Kanselarij... op een stiekeme boeking naar het oosten. Over de Wolkenzee. Waar niemand heen vliegt."),
   ("npc", "Nog twee raadsleden over: Generaal Donsveer en Kanselier Zachtpels. We verhoren ze allebei.")],
  munten=14000, lv=57, na=[HQ[3]],
  praat={"z5_havenmeester": [
      ("npc", "Het nachtlogboek? Natuurlijk. Alles staat erin. Wie, wanneer, waarheen. Het ligt op de balie."),
      ("npc", "Al is er één boeking die ik nooit snapte. Elke donderdagnacht. Naar het oosten, over de Wolkenzee. Daar wil niemand heen. Kijk zelf maar.")]})

Q(HQ[5], "hoofd", "z5_inspecteur", "De laatste twee verhoren",
  "Twee raadsleden over. Generaal Donsveer was 's nachts nergens te vinden. Kanselier Zachtpels heeft het zegel van de Kanselarij.",
  [D("meld", "z5_verhoor:donsveer", 1, "Verhoor Generaal Donsveer, boven in de toren van de Academie van de Wolkwacht", plek="npc:z5_donsveer"),
   D("meld", "z5_verhoor:zachtpels", 1, "Verhoor Kanselier Zachtpels in de Kanselarij, naast de troonzaal in het paleis (neem het stukje kaas mee)",
     plek="npc:z5_zachtpels")],
  [("npc", "Twee verhoren nog. Generaal Donsveer in zijn toren. En Kanselier Zachtpels in de Kanselarij."),
   ("npc", "Tip van een oude speurder: neem dat stukje rijpe kaas mee dat de Koningin je gaf. Een kaastest. Je weet maar nooit.")],
  "Generaal Donsveer in de toren van de Academie, en Kanselier Zachtpels in de Kanselarij.",
  [("npc", "Een generaal die stiekem zingt. Een kanselier die soep weigert maar kaas grist. Mijn snor trilt. Dat is een goed teken."),
   ("npc", "Alle sporen liggen er. Tijd voor het bewijsbord.")],
  munten=14000, lv=58, na=[HQ[4]])

Q(HQ[6], "hoofd", "z5_inspecteur", "Het bewijsbord",
  "Alle sporen zijn gevonden. Leg ze naast elkaar op het bewijsbord, en wijs de verrader aan. Voor de hele Hoge Raad.",
  [D("meld", "z5_beschuldigd:juist", 1, "Wijs de verrader aan op het bewijsbord: bij Inspecteur Snuffelaar in de zijkamer van de Raadszaal "
     "(of rechtsklik je Speurdersboekje)", plek="npc:z5_inspecteur"),
   D("meld", "z5:ontmaskerd", 1, "Ontmasker de verrader in de Raadszaal", plek="poi:z5_raadszaal")],
  [("npc", "Kijk naar het bord. Tien sporen. Vier verdachten. Drie sporen wijzen allemaal dezelfde kant op."),
   ("npc", "Wie heeft een staart, het zegel van de Kanselarij, en een zwak voor rijpe kaas? Wijs hem aan. Ik heb de Raad al bij elkaar geroepen.")],
  "Wijs de verrader aan op het bewijsbord.",
  [("npc", "Een Mika. In de Hoge Raad. Al drie jaar. En niemand die het zag, omdat hij zulke lekkere koekjes uitdeelde."),
   ("npc", "Kootje Zilverstaart, van de Ringvinger. Hij is weg met de bladzijde. Naar het spuitgat! Als hij het slaaplied achterstevoren zingt..."),
   ("npc", "...krijgt Moeder Wolkwal een grauwe droom. En een walvis die nare dingen droomt, zakt. Met de hele stad erop. Achter hem aan!")],
  munten=15000, lv=58, na=[HQ[5]], actie_klaar="start:" + HQ[7])

Q(HQ[7], "hoofd", "z5_inspecteur", "De Wolkwaldroom",
  "Kootje Zilverstaart sprong met de gescheurde bladzijde in het middelste spuitgat, diep de walvis in. Daar zingt hij het slaaplied "
  "achterstevoren. Moeder Wolkwal droomt grauw, en ze zakt.",
  [D("gebied", "poi:z5_spuitgat", 1, "Ren Zilverstaart achterna naar het middelste spuitgat, tussen de kathedraal en de Ooggaarde (westkant)",
     plek="poi:z5_spuitgat"),
   D("meld", "z5_zilverstaart:verslagen", 1, "Versla Kootje Zilverstaart in De Wolkwaldroom, diep in de walvis", plek="baas:z5_wolkwaldroom"),
   D("meld", "z5_droom:opgevrolijkt", 1, "Vrolijk de Grauwe Droom van Moeder Wolkwal op, in De Wolkwaldroom", plek="baas:z5_wolkwaldroom")],
  [("npc", "Naar het spuitgat! Snel!")],
  "Achter Zilverstaart aan, het spuitgat in. En vrolijk de grauwe droom op.",
  [("npc", "Je bent terug! En de walvis zwemt weer recht. De straten wiebelen niet meer. Hoor je dat? Ze neuriet."),
   ("npc", "En je hebt de bladzijde. Mag ik? Dank je. Ik lees hem voor. Iedereen mag het nu weten."),
   ("npc", "'Lang geleden woonde aan de rand van de wereld een gewone, kleine Mika. Alleen hongeriger dan de rest.'"),
   ("npc", "'Hij stal een kaasknabbel van een guh, en voelde één tel iets warms: vads. Toen was het weg. Dus pakte hij er nog een. En nog een.'"),
   ("npc", "'Want vads groeit alleen als je hem deelt. Wat je pakt, smelt in je pootjes. Zijn greep reikte steeds verder. Hij zette een gestolen "
           "kroontje op en noemde zich Keizer. Hij bouwde een burcht rond een schaal zo groot als een meer: de Lege Schaal. Nooit vol.'"),
   ("npc", "De Grauwe Keizer-Mika. Burcht Nooitgenoeg. Daar komt alle grauwheid vandaan. Dat stond al die tijd op deze bladzijde."),
   ("npc", "Maar eerst: Moeder Wolkwal moet wakker worden. Helemaal. Zing het slaaplied, de GOEDE kant op, bij haar oog.")],
  munten=20000, loot="episch", lv=59, na=[HQ[6]], inleveren="z5_koningin", items=[(item("bladzijde"), 1)])

Q(HQ[8], "hoofd", "z5_koningin", "Het slaaplied van Moeder Wolkwal",
  "Het slaaplied staat op de achterkant van de bladzijde. Zing het de goede kant op, bij het oog van Moeder Wolkwal, en ze wordt "
  "helemaal wakker.",
  [D("gebied", "poi:z5_ooggaarde_balkon", 1, "Ga naar het balkon van de Ooggaarde, boven het oog van Moeder Wolkwal (westpunt, zuidkant)",
     plek="poi:z5_ooggaarde_balkon"),
   D("meld", "z5:wolkwal_wakker", 1, "Zing het slaaplied voor Moeder Wolkwal, op het balkon van de Ooggaarde", plek="poi:z5_ooggaarde_balkon")],
  [("npc", "Het oog van Moeder Wolkwal ligt onder de Ooggaarde, helemaal aan de westpunt. Er is een balkon."),
   ("npc", "Zing het slaaplied de goede kant op. Je mag vals zingen. Generaal Donsveer doet het ook.")],
  "Naar het balkon van de Ooggaarde, en zing.",
  [("npc", "Ze knipoogde naar je. Dat doet ze maar één keer per eeuw. Of per dag, volgens Feestguh Confetti."),
   ("npc", "Hoogvads is veilig. Dankzij jou. Kniel even. Nee, niet zo diep, je kussen valt eruit."),
   ("npc", "Bij deze ben je Ridder van Hoogvads. Met een knuffel. Die is verplicht.")],
  munten=24000, loot="episch", lv=59, na=[HQ[7]], actie_klaar="z5_klaar")

Q(HQ[9], "hoofd", "z5_koningin", "Sterren in het oosten",   # U-Z6VERHAAL: was "De Stormrand" (zone 6 is now Het Zijdeland)
  "Zilverstaart werkte voor de Ringvinger, een generaal van de Keizer. In zijn Kanselarij ligt nog post. En in het oosten, achter "
  "de Wolkenzee, gaan sterren uit.",
  [D("gebied", "poi:z5_kanselarij", 1, "Doorzoek het bureau in de Kanselarij, naast de troonzaal", plek="poi:z5_kanselarij"),
   D("gebied", "poi:z5_pier_oost", 1, "Kijk naar het oosten vanaf het eind van de oostelijke pier van de Luchthaven", plek="poi:z5_pier_oost"),
   D("meld", "z5:cliffhanger", 1, "Kijk goed naar de sterren in het oosten", plek="poi:z5_pier_oost"),
   D("praat", "z5_havenmeester", 1, "Vraag Havenmeester Windvaan naar een luchtschip naar het oosten", plek="npc:z5_havenmeester")],
  [("npc", "Zilverstaart had een bureau in de Kanselarij. Daar ligt vast nog post van zijn baas. Kijk er even in."),
   ("npc", "En kijk daarna eens naar het oosten, vanaf de pieren van de Luchthaven. Daar gaan al dagen sterren uit. Eén voor één.")],
  "Het bureau in de Kanselarij, de oostelijke pier, en Havenmeester Windvaan.",
  [("npc", "Naar het oosten? Daar vliegt niemand heen. Te ver voor een luchtschip. Daar ligt het Grote Vasteland."),
   ("npc", "...Tenzij Moeder Wolkwal er zelf naartoe zwemt. Ze heeft het gevoeld, zegt de Koningin. Die sterren. Ze wil helpen."),
   ("npc", "Voel je dat? De straten trillen. Ze gaat zwemmen. Hou je vast!")],   # U-Z6VERHAAL: the great swim follows (Z6Verhaal.wolkwaltocht)
  munten=16000, lv=60, na=[HQ[8]], inleveren="z5_havenmeester")

# ===== job advancement 3 at class level 58 (U-SPECS, ronde 4): at the end of Hoogvads, after the finale ======================
# Kapitein Veerkracht of the Wolkwacht Academie holds the ceremony (klassen.Advancement step 3; the crystal touch of
# zone2.systemen.Advancement3 plays on step 3). Title + power per spec (lang vadsrijk.klassen.titel.<spec>.3).
for _k, _naam in (("knuffelridder", "Knuffelridder"), ("pluisjager", "Pluisjager"), ("vadsmagier", "Vadsmagiër")):
    Q(f"z5_adv58_{_k}", "klasse", "z5_veerkracht", f"De hoogste titel van de {_naam}",
      f"Hoogvads is gered, Ridder. De Wolkwacht wil je de hoogste titel van De Knuffelorde geven. Bereik klasse-level 58 als {_naam} "
      "en kom terug bij Kapitein Veerkracht op het oefenplein van de Academie.",
      [D("level", "*", 58, f"Bereik level 58 als {_naam}", plek="npc:z5_veerkracht")],
      [("npc", "Een Ridder van Hoogvads met maar twee titels? Dat kan niet. Dat staat niet in het reglement. Ik heb het net nagekeken."),
       ("npc", f"Bereik level 58 als {_naam}. Dan marcheert de hele Wolkwacht voor je. Links, rechts, links, knuffel.")],
      f"Level 58 als {_naam}, en dan naar Kapitein Veerkracht op het oefenplein van de Academie.",
      [("npc", "Wolkwacht, gééf acht! Voor de held van Hoogvads: de hoogste titel van De Knuffelorde!"),
       ("npc", "Je specialisatie is nu op zijn sterkst. Zeg maar Veer. Nee, zeg maar 'Kapitein'. Nee, toch Veer.")],
      munten=25000, loot="episch", lv=56, klasse=_k, na=[HQ[8]], actie_klaar=f"advancement:{_k}:3")

# ===== side quests (11): city life, refugees, university research, academy trials ===========================================
Q("z5_s01_bessen", "zij", "z5_kok", "Een kom voor iedereen",
  "De ketel van de Grote Soepkeuken raakt nooit leeg, maar de wolkbessen wel. Ze groeien in het Staartpark en de Ooggaarde.",
  [D("geheim", "teller:z5_bessen", 8, "Pluk 8 wolkbessen in het Staartpark (oostkant) en de Ooggaarde (westkant): zoek de roze glinsters",
     plek="gebied:z5_staartpark")],
  [("npc", "Wolkbessen maken de soep roze. En zoet. En een beetje zweverig."),
   ("npc", "Ze groeien in het Staartpark en in de Ooggaarde. Pluk er acht, dan krijgt iedereen een roze kom.")],
  "Acht wolkbessen uit het Staartpark en de Ooggaarde.",
  [("npc", "Kijk eens hoe roze! Grijsje lacht. Grijsje lachte nog nooit. Dank je wel, ridder.")],
  munten=6000, lv=56, na=[HQ[3]])
Q("z5_s02_flap", "zij", "z5_pluisje", "Pluisje's knuffel",
  "Pluisje is haar knuffelkonijn Flap kwijt. Ze was in de kathedraal om naar het licht te kijken.",
  [D("gebied", "poi:z5_flap", 1, "Zoek knuffelkonijn Flap achter het beeld van de Eerste Knuffel, achterin de Ordekathedraal", plek="poi:z5_flap"),
   D("praat", "z5_pluisje", 1, "Breng Flap terug naar Pluisje in de Grote Soepkeuken", plek="npc:z5_pluisje")],
  [("npc", "Flap is weg. Hij heeft één oor. Ik was in de grote kerk. Naar het licht kijken. Toen was hij er nog."),
   ("npc", "Wil jij hem zoeken? Hij is heel zacht. En een beetje grijs. Net als ik vroeger.")],
  "Flap ligt ergens in de Ordekathedraal.",
  [("npc", "FLAP! Je bent terug! Hij ruikt naar kerk. Dat vindt hij lekker, zie ik."),
   ("npc", "Dank je wel. Flap zegt het ook. Hij is alleen een beetje verlegen.")],
  munten=5000, lv=56, na=[HQ[1]])
Q("z5_s03_brieven", "zij", "z5_grauwsnor", "Brieven naar huis",
  "Opa Grauwsnor schrijft elke dag brieven. Ze moeten naar de juiste guhs in Hoogvads: die kunnen ze verder sturen.",
  [D("praat", "z5_havenmeester", 1, "Breng een brief naar Havenmeester Windvaan op de Luchthaven (voor de post naar het oosten)",
     plek="npc:z5_havenmeester"),
   D("praat", "z5_zuster", 1, "Breng een brief naar Zuster Zachtzang bij het altaar van de Ordekathedraal (voor het avondlied)", plek="npc:z5_zuster"),
   D("praat", "z5_pluisje", 1, "Breng het laatste briefje naar Pluisje in de Grote Soepkeuken (haar opa schreef het)", plek="npc:z5_pluisje")],
  [("npc", "Drie brieven. Eén voor de post naar het oosten, één voor het avondlied in de kathedraal, en één voor mijn kleindochter."),
   ("npc", "Ze zit hier vlakbij, maar ze leest liever post dan dat ze naar haar opa luistert. Kinderen.")],
  "Havenmeester Windvaan, Zuster Zachtzang en Pluisje.",
  [("npc", "Alle drie bezorgd? Dan ben jij de snelste postguh van de walvis. Mijn snor wordt er roze van. Kijk. Een beetje.")],
  munten=6000, lv=57, na=[HQ[3]], start_items=[(item("brieven"), 3)],
  praat={"z5_havenmeester": [("npc", "Een brief naar het oosten? Die gaat mee met het volgende schip. Dat beloof ik.")],
         "z5_zuster": [("npc", "Een brief voor het avondlied? We zingen hem vanavond. Iedereen in de kathedraal denkt dan even aan het oosten.")],
         "z5_pluisje": [("npc", "Een briefje van opa! 'Lief Pluisje, eet je soep op en knuffel Flap.' Dat doe ik al. Allebei.")]})
Q("z5_s04_oog", "zij", "z5_professor", "Het Oog meten",
  "Professor Brilsma wil weten hoe groot de tweede maan, het Oog, aan de hemel staat. Vanaf de sterrenwacht kun je hem meten.",
  [D("gebied", "poi:z5_sterrenwacht", 1, "Klim naar de sterrenwacht op de hoek van de universiteit (noordoostkant van Hoogvads)",
     plek="poi:z5_sterrenwacht"),
   D("meld", "z5:oog_gemeten", 1, "Meet het Oog door de grote telescoop in de sterrenwacht (klik als je er staat)", plek="poi:z5_sterrenwacht")],
  [("npc", "De tweede maan. Het Oog. Elk jaar staat hij groter aan de hemel. En grauwer. Dat bevalt me niets."),
   ("npc", "Meet hem voor me, door de telescoop in de sterrenwacht. Ik ben te oud voor die trap.")],
  "De telescoop in de sterrenwacht.",
  [("npc", "Twaalf duim? Vorig jaar was het tien. Hij komt dichterbij. Of hij wordt groter. Allebei geen goed nieuws."),
   ("npc", "Ik schrijf het op. Wetenschap is: opschrijven wat eng is, zodat je het later minder eng kunt maken.")],
  munten=6000, lv=57, na=[HQ[1]])
Q("z5_s05_adem", "zij", "z5_professor", "De adem van Moeder Wolkwal",
  "Moeder Wolkwal ademt door drie spuitgaten. Professor Brilsma wil weten of ze nog gezond ademt.",
  [D("gebied", "poi:z5_spuitgat_noord", 1, "Luister bij het noordelijke spuitgat, tussen de kathedraal en de Ooggaarde", plek="poi:z5_spuitgat_noord"),
   D("gebied", "poi:z5_spuitgat", 1, "Luister bij het middelste spuitgat", plek="poi:z5_spuitgat"),
   D("gebied", "poi:z5_spuitgat_zuid", 1, "Luister bij het zuidelijke spuitgat", plek="poi:z5_spuitgat_zuid")],
  [("npc", "Een walvis ademt door haar spuitgaten. Drie stuks, tussen de kathedraal en de Ooggaarde."),
   ("npc", "Ga bij elk spuitgat staan en luister. Een gezonde wolkwal zucht als een kussen dat je opschudt.")],
  "Drie spuitgaten, tussen de kathedraal en de Ooggaarde.",
  [("npc", "Ze zucht als een kussen? Allemaal? Goed zo. Gezond en tevreden. Net als ik na een dutje.")],
  munten=7000, lv=58, na=[HQ[8]])
Q("z5_s06_parcours", "zij", "z5_veerkracht", "Proef van de Wolkwacht: het Wolkenparcours",
  "Elke kadet van de Wolkwacht rent het Wolkenparcours: vier palen op het oefenplein. Kapitein Veerkracht wil zien of jij het kunt.",
  [D("gebied", "poi:z5_parcours_1", 1, "Ren naar de eerste paal, noordwest op het oefenplein van de Academie", plek="poi:z5_parcours_1"),
   D("gebied", "poi:z5_parcours_2", 1, "Ren naar de tweede paal, noordoost op het oefenplein", plek="poi:z5_parcours_2"),
   D("gebied", "poi:z5_parcours_3", 1, "Ren naar de derde paal, zuidoost op het oefenplein", plek="poi:z5_parcours_3"),
   D("gebied", "poi:z5_parcours_4", 1, "Ren naar de vierde paal, zuidwest op het oefenplein", plek="poi:z5_parcours_4")],
  [("npc", "De eerste proef van de Wolkwacht: het Wolkenparcours. Vier palen, in volgorde. Klaar? Af!")],
  "Vier palen op het oefenplein, in volgorde.",
  [("npc", "Prachtig! Sneller dan Donsveer, en die rent alsof er een koekje aan de finish ligt.")],
  munten=6000, lv=57, na=[HQ[2]])
Q("z5_s07_kadet", "zij", "z5_veerkracht", "Proef van de Wolkwacht: de verdwaalde kadet",
  "Kadet Dons is weer verdwaald. Hij staat op een pier van de Luchthaven. Breng hem terug naar de Academie.",
  [D("escorte", "z5_kadet_dons", 1, "Haal Kadet Dons op van de westelijke pier van de Luchthaven en breng hem naar het oefenplein van de Academie",
     bron="poi:z5_academie", plek="npc:z5_kadet_dons")],
  [("npc", "De tweede proef: zorg voor je kameraden. Kadet Dons is weer kwijt. Hij staat vast op een pier van de Luchthaven, naar wolken te kijken."),
   ("npc", "Breng hem terug. Langzaam. Hij heeft korte pootjes.")],
  "Kadet Dons, van de Luchthaven naar de Academie.",
  [("npc", "Dons! Daar ben je. Bedank de ridder. ...Nee, niet salueren, je helm... Te laat.")],
  munten=7000, lv=58, na=["z5_s06_parcours"])
Q("z5_s08_markt", "zij", "z5_lientje", "Marktdrukte",
  "Het is druk op de markt van Hoogvads. Kraamguh Lientje heeft pakjes voor drie andere kramen.",
  [D("praat", "z5_bakker", 1, "Breng een pakje meel naar Bakker Wolkbol, bij het bakkerskraampje op de markt", plek="npc:z5_bakker"),
   D("praat", "z5_kleermaker", 1, "Breng een rol stof naar Kleermaker Zoompje op de markt", plek="npc:z5_kleermaker"),
   D("praat", "z5_juwelier", 1, "Breng een doosje glimmers naar Juwelier Glimmertje op de markt", plek="npc:z5_juwelier")],
  [("npc", "Help! Drie pakjes, drie kramen, één Lientje. Meel voor de bakker, stof voor de kleermaker, glimmers voor de juwelier.")],
  "De bakker, de kleermaker en de juwelier op de markt.",
  [("npc", "Alles bezorgd? Je bent een held. Een markt-held. Dat is bijna net zo goed als een gewone held.")],
  munten=5000, lv=56, na=[HQ[0]],
  praat={"z5_bakker": [("npc", "Meel! Daar bak ik wolkbollen van. Hier, eentje voor jou. Snel opeten, voor hij wegzweeft.")],
         "z5_kleermaker": [("npc", "Fluweelzachte stof! Daar maak ik een nieuwe cape van. Niet zo lang als die van de Kanselier. Niemand wil zo'n lange cape.")],
         "z5_juwelier": [("npc", "Glimmers! Oooh. Ze glimmen. Dat is het hele punt.")]})
Q("z5_s09_bloesem", "zij", "z5_stekje", "Water voor het Staartpark",
  "De bloesembomen in het Staartpark hebben dorst. Tuinguh Stekje heeft één gieter en vier bomen.",
  [D("gebied", "poi:z5_bloesemboom_1", 1, "Geef de eerste bloesemboom water, zuidwest van de vijver in het Staartpark", plek="poi:z5_bloesemboom_1"),
   D("gebied", "poi:z5_bloesemboom_2", 1, "Geef de tweede bloesemboom water, zuidoost van de vijver", plek="poi:z5_bloesemboom_2"),
   D("gebied", "poi:z5_bloesemboom_3", 1, "Geef de derde bloesemboom water, noordoost van de vijver", plek="poi:z5_bloesemboom_3"),
   D("gebied", "poi:z5_bloesemboom_4", 1, "Geef de vierde bloesemboom water, noordwest van de vijver", plek="poi:z5_bloesemboom_4")],
  [("npc", "Vier bomen rond de vijver. Ze wiebelen als de walvis zwemt, en dan morsen ze hun water. Geef ze een slokje?")],
  "Vier bloesembomen rond de vijver in het Staartpark.",
  [("npc", "Kijk, ze bloeien alweer! De blaadjes dwarrelen helemaal tot in de zee.")],
  munten=5000, lv=57, na=[HQ[0]])
Q("z5_s10_wol", "zij", "z5_hofdame", "De kroon van wol",
  "Uit de gebreide kroon van de Koningin zijn drie bolletjes roze wol gerold. Hofdame Breinaald wil ze terug voor de Koningin het merkt.",
  [D("geheim", "teller:z5_bollen", 3, "Vind 3 bolletjes roze wol: op de markt, op de binnenplaats van het paleis en in de universiteit (roze pluisjes)",
     plek="poi:z5_troonzaal")],
  [("npc", "Ssst. De kroon van de Koningin rafelt. Drie bolletjes zijn weggerold. Eén over de markt, één over de paleishof, en één..."),
   ("npc", "...is met een professor mee de universiteit in gelopen. Vraag niet hoe.")],
  "Drie bolletjes roze wol.",
  [("npc", "Alle drie! Ik brei ze er vanavond in, als ze slaapt. Ze merkt er niks van. Ze merkt het altijd. Ze zegt alleen niks.")],
  munten=6000, lv=58, na=[HQ[1]])
Q("z5_s11_knuffellied", "zij", "z5_zuster", "Het lied van de Eerste Knuffel",
  "In de Ordekathedraal zingen ze elke avond het lied van de Eerste Knuffel. Zuster Zachtzang wil dat je het licht ziet zoals de "
  "eerste ridders het zagen.",
  [D("gebied", "poi:z5_roosvenster_noord", 1, "Ga onder het noordelijke roosvenster staan, in het transept van de Ordekathedraal",
     plek="poi:z5_roosvenster_noord"),
   D("gebied", "poi:z5_roosvenster_zuid", 1, "Ga onder het zuidelijke roosvenster staan", plek="poi:z5_roosvenster_zuid"),
   D("gebied", "poi:z5_eerste_knuffel", 1, "Ga bij het gouden beeld van de Eerste Knuffel staan, achterin de kathedraal", plek="poi:z5_eerste_knuffel")],
  [("npc", "De eerste ridders van de Orde kwamen hier, lang geleden. Ze stonden onder de twee rozen en daarna bij de Eerste Knuffel."),
   ("npc", "Doe het ze na. Je hoeft niet te zingen. Mag wel.")],
  "Het noordelijke en zuidelijke roosvenster, en de Eerste Knuffel.",
  [("npc", "Je hebt het gezien, hè? Het licht valt precies op het beeld. Alsof de zon zelf iemand knuffelt."),
   ("npc", "Zo begon de Orde. Met één knuffel. En toen nog een. Zo groeit vads.")],
  munten=7000, lv=59, na=[HQ[8]])

# ===== quest xp (the same curve as z3 Z3Xp: a main step = 45 % of a level, a side quest = 18 %; lv up to 60) ===============
def z5xp(lv, hoofd):
    l = max(1, min(99, lv))
    stap = max(1, round(40 + 25 * l ** 1.6))
    return max(1, round(stap * (0.45 if hoofd else 0.18)))


# The level cap of this tree is still 50 (kern Levels.MAX / MAX_GENERIEK): a quest above it could never be offered. The design
# levels (55-60) stay in the xp formula; the GATE is clamped to LV_CAP. Raise LV_CAP together with the kern cap (integration).
LV_CAP = 60   # (T-INT: kern cap raised to 60)
for _q in QUESTS:
    _q["beloning"]["xp"] = z5xp(_q["lv"], _q["soort"] != "zij")
    _q["lv"] = min(_q["lv"], LV_CAP)

# ---------------------------------------------------------------------------------------------------------------------------
# Secrets: 8 knuffels, 3 treasures (Z5VerhaalPlekken.KNUFFEL / SCHAT)
# ---------------------------------------------------------------------------------------------------------------------------
KNUFFELS = {
    "knuffel_1": "Torenpluisje", "knuffel_2": "Vinnetje", "knuffel_3": "Wimpertje", "knuffel_4": "Pierpoes", "knuffel_5": "Sterretje",
    "knuffel_6": "Raadsbeertje", "knuffel_7": "Kroontje", "knuffel_8": "Glasbloempje",
}
SCHATTEN = {
    "staartvin": ("Op de staartvin", "Helemaal achteraan, waar de staart van Moeder Wolkwal omhoog krult, zit een kist vastgeknoopt. Zo hoog durft niemand."),
    "troonzaal": ("Achter de troon", "Achter de troon van de Koningin staat een kist met 'NIET OPENEN' erop. Er zit een briefje in: 'Behalve ridders.'"),
    "ooggaarde": ("Tussen de wimpers", "Onder het balkon van de Ooggaarde, tussen de wimpers van de walvis, glimt een kist. Ze knippert niet. Gelukkig."),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Gids pages (questlog J, tab Gids: ids z5_* in QuestlogScherm.GIDS), tips, other texts
# ---------------------------------------------------------------------------------------------------------------------------
GIDS = {
    "z5_hoogvads": ("Zone 5: Hoogvads", "Na de Lampionkust en vanaf level 55 vaart het eerste luchtschip van de Opstijgpier omhoog naar Hoogvads, de "
                    "hoofdstad van alle guhs, op de rug van Moeder Wolkwal (lv 56-60). Hier is het veilig: geen monsters in de stad. De Luchthaven "
                    "ligt aan de noordflank, de Zachtmoedlaan loopt van de kathedraal (west) naar het paleis (oost), de universiteit in het "
                    "noorden, de Academie van de Wolkwacht in het zuiden, het Staartpark helemaal oostelijk en de Ooggaarde westelijk."),
    "z5_speuren": ("Speuren", "In 'Verraad in de stad' ben je speurder. Verhoor verdachten (kies je vragen goed: één vraag levert een spoor op), "
                   "zoek sporen op de plekken die de tracker aanwijst, en kijk op het bewijsbord: bij Inspecteur Snuffelaar in de Raadszaal of "
                   "met je Speurdersboekje (rechtsklik). Een verkeerde beschuldiging kost niks: het bord legt uit waarom het niet klopt."),
    "z5_soepkeuken": ("De Grote Soepkeuken", "In de Grote Soepkeuken eten de Uitgevadsten weer vads: magere guhs die uit het oosten zijn gevlucht. "
                      "Hoe verder het verhaal komt, hoe voller en vrolijker ze worden. Help Kok Pollepel, en luister naar hun verhalen."),
    "z5_ambachten": ("Het Ambachtsgilde", "Hoogvads is de hoofdstad van alle ambachten. In het Ambachtsgilde naast de universiteit staan de "
                     "leermeesters van koken, brouwen, smeden, naaien en imkeren. De Meesterwerken komen binnenkort."),
    "z5_reizen": ("Reizen vanaf Hoogvads", "Havenmeester Windvaan op de Luchthaven vliegt je naar alle eilanden van de Eilandzee: klik op hem "
                  "voor de reiskaart. Na het verhaal van Hoogvads gaat lijn E naar het Zijdeland (zone 6) open, vanaf de Staarthaven."),
}
TIPS = {
    "z5_veilig": ("Veilig!", "Hoogvads is veilig: in de stad vallen geen monsters aan. Kampvuren werken wel als rustpunt."),
    "z5_verhoor": ("Verhoren", "Bij een verhoor kies je je vragen. Eén vraag levert een spoor op voor je Speurdersboekje. Je mag alles vragen."),
    "z5_bord": ("Het bewijsbord", "Rechtsklik je Speurdersboekje om het bewijsbord te openen: alle sporen en verdachten op een rij."),
}
OVERIG = {
    # roles + travel
    P + "optie.rol.luchtschip_z5": "Waar vliegen we heen?",
    P + "optie.rol.z5_bewijsbord": "Laat het bewijsbord zien",
    P + "optie.rol.z5_vitrine": "Wat staat er in de vitrine?",
    P + "optie.rol.ambacht_gilde": "Wat is het Ambachtsgilde?",
    P + "optie.rol.ambacht_koken": "Leer me koken",
    P + "optie.rol.ambacht_brouwen": "Leer me brouwen",
    P + "optie.rol.ambacht_smeden": "Leer me smeden",
    P + "optie.rol.ambacht_naaien": "Leer me naaien",
    P + "optie.rol.ambacht_imkeren": "Leer me imkeren",
    P + "reis.kies.z5_havenmeester": "Waarheen? Kies hieronder, of bekijk de reiskaart.",
    P + "optie.vlieg.hoogvads": "Naar Hoogvads",
    P + "optie.vlieg.hoogvads.slot": "Naar Hoogvads (nog op slot)",
    P + "reis.slot.hoogvads": "Hoogvads zweeft nog te hoog. Eerst moet Moeder Wolkwal laag over de zee zakken (het verhaal van de Lampionkust), en je "
                              "moet minstens level 55 zijn. Je bent nu level %s.",
    P + "reis.slot.hoogvads.kort": "Na de Lampionkust, vanaf level 55",
    P + "reiskaart.plek.hoogvads": "Hoogvads",
    Z + "ambacht.binnenkort": "De werkplaatsen van het Ambachtsgilde gaan binnenkort open. Kom dan terug!",
    Z + "ambacht.gilde": "Het Ambachtsgilde leert je de Meesterwerken van negen ambachten. Binnenkort gaan de deuren open. Tot die tijd: oefen in de "
                         "werkplaatsen van je eigen stad.",
    # the teasers
    Z + "vitrine.naam": "De vitrine",
    Z + "vitrine.z5_vadszanger.0": "In de glazen vitrine ligt een roze guhfluit op een kussentje. Op het bordje: 'De Guhfluit. Wapen van de Vadszanger.'",
    Z + "vitrine.z5_vadszanger.1": "Eronder, in sierlijke letters: 'Binnenkort.'",
    Z + "vitrine.z5_pluisninja.0": "In de vitrine liggen twee piepkleine pluisdolkjes, zo zacht als een wattenstaafje. 'De Pluisdolkjes. Wapen van de Pluisninja.'",
    Z + "vitrine.z5_pluisninja.1": "Eronder, heel klein, bijna onzichtbaar: 'Binnenkort.'",
    # the detective board
    Z + "bord.naam": "Het bewijsbord",
    Z + "bord.sporen": "Sporen gevonden: %s van %s.%s",
    Z + "bord.verdachten": "De verdachten:%s",
    Z + "bord.vrij": "vrijgepleit",
    Z + "bord.onbekend": "???",
    Z + "bord.zeker": "Drie sporen wijzen dezelfde kant op: een staart, het zegel van de Kanselarij, en een zwak voor rijpe kaas.",
    Z + "bord.nog_niet": "Nog te weinig bewijs om iemand aan te wijzen. Speur verder!",
    Z + "bord.optie.verder": "Verder speuren",
    Z + "bord.fout": "Inspecteur Snuffelaar schudt zijn hoofd. \"Nee nee. Kijk naar het bewijs: %s\"",
    Z + "bord.juist.0": "Je wijst naar Kanselier Zachtpels. Het wordt stil in de Raadszaal. Heel stil.",
    Z + "bord.juist.1": "\"Ik? Ik? Maar ik deel altijd koekjes uit!\" Zijn staart zwiept onder zijn cape. Iedereen ziet het.",
    Z + "bewijs.gevonden": "Nieuw spoor in je Speurdersboekje: %s (%s/%s)",
    Z + "bewijs.naam": "Speurdersboekje",
    # interrogation
    Z + "verhoor.klaar": "Dat was het. Dank u.",
    Z + "verhoor.nog_niet": "Je hebt nog niks om dit raadslid naar te vragen. Volg eerst het spoor van Inspecteur Snuffelaar.",
    Z + "soep.kom": "Je schept een kom soep op. Hij dampt roze. \"Dank je wel...\" (%s/4)",
    Z + "soep.al": "Deze Uitgevadste heeft al een kom. Ze lacht naar je met soep op haar snuit.",
    Z + "zachtpels.karton": "Dit is een kartonnen Kanselier. De Wolkwacht heeft hem neergezet zodat de toeristen niet schrikken. Er hangt een koekje aan.",
    # scenes
    Z + "scene.aankomst.titel": "Hoogvads",
    Z + "scene.aankomst.onder": "De Zwevende Stad",
    Z + "scene.aankomst.zee": "Het luchtschip stijgt op van de Opstijgpier. Boven je hangt een kop zo groot als een berg. Met wimpers.",
    Z + "scene.aankomst.oog": "Moeder Wolkwal opent haar oog en kijkt je aan. Ze glimlacht. Een walvis die glimlacht, voel je in je buik.",
    Z + "scene.aankomst.stad": "Over de rand van haar rug: torens, vaandels, lampions en een kathedraal met een poort tot in de wolken. Hoogvads!",
    Z + "scene.aankomst.welkom": "Welkom in Hoogvads! Meld je bij Havenmeester Windvaan aan de balie van de Luchthaven.",
    Z + "scene.ontmasker.titel": "Ontmaskerd!",
    Z + "scene.ontmasker.onder": "Kootje Zilverstaart, van de Ringvinger",
    Z + "scene.ontmasker.0": "Inspecteur Snuffelaar trekt aan de cape. Er komt een rits. Een RITS. In een guh.",
    Z + "scene.ontmasker.1": "Het guh-pak zakt op de grond. Daar staat een magere, zilvergrijze Mika met een monocle en een staart zo lang als een sjaal.",
    Z + "scene.ontmasker.2": "\"Kootje Zilverstaart, tot uw dienst. Van de Ringvinger. Mijn collega de Vliegervanger mocht de lampionnen van de kust jatten. "
                                 "Ik krijg het grootste lampionnetje van allemaal: deze walvis. Met stad en al.\"",
    Z + "scene.ontmasker.3": "\"En deze bladzijde neem ik mee!\" Hij springt door het raam, over de daken, naar het spuitgat!",
    Z + "scene.herinnering.titel": "Herinnering",
    Z + "scene.herinnering.onder": "De vlucht van Mager Mientje",
    Z + "scene.herinnering.0": "Grauwhoven. Alles is grijs. De straten, de lucht, de guhs. Op elke muur een poster: \"DELEN IS DOM\".",
    Z + "scene.herinnering.1": "Mika's lopen voorbij met zakken vol grauwe pluis. Niemand kijkt op. Kijken is gevaarlijk.",
    Z + "scene.herinnering.2": "Mientje bindt een oude lampionvlieger aan haar rug. Ze rent de Tolbrug op. De wind pakt haar.",
    Z + "scene.herinnering.3": "Onder haar: de grauwe landen, de zoemende Vadsleidingen, allemaal naar het oosten. Boven haar: één roze stip. Hoogvads.",
    Z + "scene.herinnering.4": "\"Ik heb drie dagen gevlogen. Toen rook ik soep.\"",
    Z + "scene.droom.titel": "De Grauwe Droom",
    Z + "scene.droom.onder": "Moeder Wolkwal droomt grauw",
    Z + "scene.droom.0": "Je springt het spuitgat in. Warme mist, zacht als dons. Je zakt omlaag, de walvis in, haar droom in.",
    Z + "scene.droom.1": "Kootje Zilverstaart zingt het slaaplied achterstevoren. Zijn staart zwiept in de maat. De wolken worden grauw.",
    Z + "scene.droom.2": "Hij struikelt over zijn eigen staart. Floep. De Wolkwacht springt achter je aan en zet hem in een wolkenkooi.",
    Z + "scene.droom.3": "Maar de droom blijft grauw: een grote, grijze walvis van mist zwemt rondjes en huilt zachtjes.",
    Z + "scene.droom.4": "Je pakt de bladzijde en zingt het slaaplied. De goede kant op. Vals. Maar met heel je hart.",
    Z + "scene.droom.5": "De grijze walvis stopt met huilen. Hij kleurt roze, van zijn staart tot zijn neus. De droom wordt een fijne droom.",
    Z + "scene.ontwaken.titel": "Moeder Wolkwal",
    Z + "scene.ontwaken.onder": "is wakker",
    Z + "scene.ontwaken.0": "Je zingt. Het oog onder het balkon gaat langzaam open. Blauw, met wimpers zo lang als bomen.",
    Z + "scene.ontwaken.1": "Moeder Wolkwal blaast een wolkenpluim uit alle drie haar spuitgaten. Heel Hoogvads juicht.",
    Z + "scene.ontwaken.2": "Ze knipoogt. Naar jou.",
    Z + "zone5.klaar.titel": "Ridder van Hoogvads",
    Z + "zone5.klaar.onder": "Moeder Wolkwal zwemt weer",
    Z + "zone5.klaar": "Hoogvads is gered! Koningin Zachtmoed noemt je Ridder van Hoogvads. Het Staartpark bloeit, de Uitgevadsten zingen mee.",
    Z + "brief.0": "In de la van Zilverstaarts bureau ligt een grauwe envelop met een zegel: een ring.",
    Z + "brief.1": "\"Aan de Ringvinger, Hertogin Ringeling, De Sterrendovende Toren, het Zijdeland. Mevrouw. De walvis is bijna rijp. Het lampionnetje "
                   "is van u. Uw dienaar, Z.\"",
    Z + "brief.2": "Op de achterkant, in een ander handschrift, heel klein: \"Uitstekend. Ik wil ALLE lampionnen. - R.\"",
    Z + "brief.naam": "Een grauwe brief",
    Z + "brief.naam.ringvinger": "Aan de Ringvinger",
    Z + "scene.cliff.titel": "Wordt vervolgd...",
    Z + "scene.cliff.onder": "Het Zijdeland",
    Z + "scene.cliff.0": "In het oosten, achter de Wolkenzee, ligt een roze streep land. Daarboven staan vijf sterren laag aan de hemel.",
    Z + "scene.cliff.1": "Terwijl je kijkt, gaat er één uit. Floep. Alsof iemand hem uitblaast. Er blijven er vier over.",
    Z + "scene.cliff.2": "Naast de gedoofde ster glinstert iets. Tien kleine ringetjes. Aan tien tenen.",
    Z + "scene.cliff.3": "Een stem over de wolken, zoet als stroop: \"Wat een mooie sterren. Die wil ik. Allemaal.\"",
    Z + "oog.gemeten": "Je kijkt door de telescoop. Het Oog vult de lens: grauw, rond, en het kijkt... terug. Twaalf duim.",
    Z + "spuitgat.adem": "Uit het spuitgat komt een zachte zucht. Als een kussen dat je opschudt.",
    Z + "bloesem.water": "Je geeft de bloesemboom water. Er dwarrelen roze blaadjes over je heen.",
    Z + "roosvenster": "Het licht door het roosvenster valt in alle kleuren over je heen. Ook in kleuren die nog geen naam hebben.",
    Z + "flap.gevonden": "Achter het beeld zit een grijs knuffelkonijntje met één oor. Flap!",
    Z + "bes.gevonden": "Wolkbes geplukt! (%s/%s)",
    Z + "bol.gevonden": "Een bolletje roze wol! (%s/%s)",
    Z + "knuffel.gevonden": "Knuffel gevonden: %s (%s/%s)",
    Z + "schat.titel": "Schat gevonden!",
    Z + "finale.wacht": "Kootje Zilverstaart is het spuitgat in gesprongen. Spring hem achterna, De Wolkwaldroom in!",
    # the Verkenningsboek
    "vadsrijk.voltooiing.zone.zone5": "Hoogvads",
    "gui.guhs.titels.naam.vr_kenner_van_hoogvads": "Kenner van Hoogvads",
    "gui.guhs.titels.hint.vr_kenner_van_hoogvads": "Verken Hoogvads helemaal (Verkenningsboek, toets B).",
    Z + "baas.zilverstaart": "Kootje Zilverstaart",
    Z + "baas.droom": "De Grauwe Droom van Moeder Wolkwal",
}
EN = {   # cheap English for a few titles (the rest: the same Dutch, as zones 1-3)
    Z + "scene.aankomst.onder": "The Floating City",
    Z + "scene.ontmasker.titel": "Unmasked!",
    Z + "scene.herinnering.titel": "Memory",
    Z + "scene.droom.titel": "The Grey Dream",
    Z + "scene.ontwaken.onder": "is awake",
    Z + "zone5.klaar.titel": "Knight of Hoogvads",
    Z + "scene.cliff.titel": "To be continued...",
    Z + "schat.titel": "Treasure found!",
    "gui.guhs.titels.naam.vr_kenner_van_hoogvads": "Expert of Hoogvads",
    Z + "bord.naam": "The evidence board",
}


# ---------------------------------------------------------------------------------------------------------------------------
# building
# ---------------------------------------------------------------------------------------------------------------------------
def _java_ids():
    with open(os.path.join(JAVA, "Z5VerhaalPlekken.java"), encoding="utf-8") as f:
        src = f.read()
    npc = set(re.findall(r'\bnpc\("(z5_[a-z_0-9]+)"', src))
    for lus, n in (("z5_vluchteling_", 4), ("z5_burger_", 6), ("z5_wacht_", 6)):
        npc.discard(lus)
        npc |= {f"{lus}{i}" for i in range(1, n + 1)}
    poi = set(re.findall(r'\bpoi\("(z5_[a-z_0-9]+)"', src))
    for lus, n in (("z5_parcours_", 4), ("z5_bloesemboom_", 4)):
        poi.discard(lus)
        poi |= {f"{lus}{i}" for i in range(1, n + 1)}
    gebied = set(re.findall(r'GEBIED_DOEL\.put\("([a-z_0-9]+)"', src))
    baas = set(re.findall(r'BAAS\.put\("([a-z_0-9]+)"', src))
    return npc, poi, gebied, baas


def teksten():
    t = dict(OVERIG)
    npc_ids, poi_ids, gebied_ids, baas_ids = _java_ids()
    for id, (naam, regels, waar) in NPCS.items():
        assert len(regels) == 3, id
        t[P + "naam." + id] = naam
        t[P + "plek.npc." + id] = waar
        for i, r in enumerate(regels, 1):
            t[P + f"praat.{id}.{i}"] = r
    assert set(NPCS) == npc_ids, f"NPC ids differ from Z5VerhaalPlekken: {set(NPCS) ^ npc_ids}"
    for id, x in PLEK_POI.items():
        t[P + "plek.poi." + id] = x
    assert set(PLEK_POI) == poi_ids, f"POI ids differ from Z5VerhaalPlekken: {set(PLEK_POI) ^ poi_ids}"
    for id, x in PLEK_GEBIED.items():
        t[P + "plek.gebied." + id] = x
        t["vadsrijk.kern.gebied." + id] = GEBIED_NAMEN[id]
    for id, x in PLEK_KAMPVUUR.items():
        t[P + "plek.kampvuur." + id] = x
    for id, x in PLEK_BAAS.items():
        t[P + "plek.baas." + id] = x
    assert gebied_ids <= set(PLEK_GEBIED) and baas_ids <= set(PLEK_BAAS), "area/boss without a plek text"
    for id, naam in UITKIJKEN.items():
        t["vadsrijk.wereld.uitkijk." + id] = naam
    for id, (naam, _tex) in QUEST_ITEMS.items():
        t[f"item.guhs.vr_z5_npc_{id}"] = naam
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
    for id, (titel, tekst) in BEWIJS.items():
        t[Z + "bewijs." + id + ".titel"] = titel
        t[Z + "bewijs." + id] = tekst
    for id, (npc, regel, _vrij) in VERDACHTEN.items():
        t[Z + "verdachte." + id] = regel
        t[Z + "bord.optie.beschuldig." + id] = "Beschuldig " + NPCS[npc][0]
    for id, v in VERHOREN.items():
        for i, (_w, x) in enumerate(v["intro"]):
            t[Z + f"verhoor.{id}.intro.{i}"] = x
        for k, (vraag, antw) in enumerate(v["vragen"]):
            t[Z + f"verhoor.{id}.vraag.{k}"] = vraag
            for i, (_w, x) in enumerate(antw):
                t[Z + f"verhoor.{id}.antwoord.{k}.{i}"] = x
    for id, naam in KNUFFELS.items():
        t[Z + "knuffel." + id] = naam
    for id, (naam, tekst) in SCHATTEN.items():
        t[Z + "schat." + id] = tekst
        t[Z + "schat.naam." + id] = naam
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
    npc_ids, poi_ids, gebied_ids, baas_ids = _java_ids()
    andere = {"z2_kapitein", "luchtschipper", "z3_kapitein"}
    alle_npc = npc_ids | andere
    ids = [q["id"] for q in QUESTS]
    assert len(ids) == len(set(ids)), "duplicate quest id"
    assert all(i.startswith("z5_") for i in ids), "zone-5 quest ids start with z5_"
    uit = []
    for q in QUESTS:
        for wie in (q["gever"], q["inleveren"]):
            assert wie in alle_npc, f"{q['id']}: unknown NPC {wie}"
        for na in q["na"]:
            assert na in ids or not na.startswith("z5_"), f"{q['id']}: unknown previous quest {na}"
        for d in q["doelen"]:
            assert d["plek"].startswith(_PLEK_SOORTEN), f"{q['id']}: objective without a plek: {d['tekst']}"
            soort, _, x = d["plek"].partition(":")
            assert x in {"npc": alle_npc, "poi": poi_ids, "gebied": set(PLEK_GEBIED), "baas": set(PLEK_BAAS)}[soort], \
                f"{q['id']}: unknown plek {d['plek']}"
            for y in (d["doel"], d["bron"]):
                if y.startswith("poi:"):
                    assert y[4:] in poi_ids, f"{q['id']}: unknown poi {y}"
            if d["soort"] in ("praat", "escorte"):
                assert d["doel"] in alle_npc, f"{q['id']}: unknown NPC {d['doel']}"
            if d["doel"].startswith("z5_verhoor:"):
                assert d["doel"][11:] in VERHOREN, f"{q['id']}: unknown interrogation {d['doel']}"
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
    return {"_": "generated by tools/features/vadsrijk_t_z5verhaal.py; do not edit", "quests": uit}


def speurwerk_json():
    alle = set(BEWIJS)
    for v in VERHOREN.values():
        assert v["bewijs"] in alle and 0 <= v["sleutel"] < len(v["vragen"]), v
        assert v["npc"] in NPCS
    for id, (npc, _r, vrij) in VERDACHTEN.items():
        assert npc in NPCS and (vrij is None or vrij in alle)
    assert sum(1 for x in VERDACHTEN.values() if x[2] is None) == 1, "exactly one traitor"
    return {"_": "generated by tools/features/vadsrijk_t_z5verhaal.py; do not edit",
            "bewijs": list(BEWIJS),
            "zeker": ZEKER,
            "verdachten": {id: {"npc": npc, "vrij": vrij or ""} for id, (npc, _r, vrij) in VERDACHTEN.items()},
            "verhoren": {id: {"npc": v["npc"], "bewijs": v["bewijs"], "sleutel": v["sleutel"], "intro": [w for w, _x in v["intro"]],
                              "vragen": [[w for w, _x in antw] for _vr, antw in v["vragen"]]} for id, v in VERHOREN.items()}}


def advancements():
    namen = ([f"vr_{q['id']}" for q in QUESTS] + ["vr_z5_knuffels_alle", "vr_z5_grauwe_brief", "vr_z5_uitkijken_alle"]
             + [f"vr_z5_schat_{s}" for s in SCHATTEN])
    return {f"advancement/quest/{n}.json": {"criteria": {"done": {"trigger": "minecraft:impossible"}}} for n in namen}


def _assets(w, a):
    for id, (_naam, tex) in QUEST_ITEMS.items():
        w(os.path.join(a, "items", f"vr_z5_npc_{id}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_z5_npc_{id}"}})
        w(os.path.join(a, "models", "item", f"vr_z5_npc_{id}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})


def build(h):
    h.w(f"{h.D}/vadsrijk/npc/quests_z5.json", quests_json())
    h.w(f"{h.D}/vadsrijk/z5verhaal/speurwerk.json", speurwerk_json())
    for pad, obj in advancements().items():
        h.w(f"{h.D}/{pad}", obj)
    _assets(h.w, h.A)
    for k, nl in teksten().items():
        h.lang(k, EN.get(k, nl), nl)   # (T-INT: make_v2.lang(key, en, nl))


# ---------------------------------------------------------------------------------------------------------------------------
# FTB Quests: zone 5 joins the chapter "Het Vadsrijk" with its own sections. Nothing is locked.
# ---------------------------------------------------------------------------------------------------------------------------
FTB_CHAPTER = "guhs_vadsrijk"
FTB_PORTRAIT = "guh:snow"
FTB_SECTIES = [
    ("vr_z5_verhaal", "Hoogvads (zone 5)", "guh:snow", [f"vr_{q}" for q in HQ]),
    ("vr_z5_stad", "Hoogvads helpen", "guh:golden", [f"vr_{q['id']}" for q in QUESTS if q["soort"] == "zij"]),
    ("vr_z5_geheimen", "Geheimen van Hoogvads", "guh:mint", ["vr_z5_knuffels_alle", "vr_z5_grauwe_brief", "vr_z5_uitkijken_alle"]
     + [f"vr_z5_schat_{s}" for s in SCHATTEN]),
]


def ftb(fq):
    q, adv = fq.q, fq.adv
    K = "guhs:kaas_knabbels"
    for d in QUESTS:
        tekst = d["tekst"].replace("Hoogvads", "&dHoogvads&r")
        icon = {"hoofd": "minecraft:writable_book"}.get(d["soort"], "minecraft:paper")
        vorm = "gear" if d["soort"] == "hoofd" else None
        q(f"vr_{d['id']}", d["titel"], tekst + " &7(Vinkt zichzelf af als je hem in het Vadsrijk inlevert.)", icon,
          [fq.met(adv(f"vr_{d['id']}"), d["titel"])], rewards=((K, 8 if d["soort"] == "zij" else 16),), shape=vorm,
          xp=100 if d["soort"] == "hoofd" else 50)
    q("vr_z5_knuffels_alle", "Acht knuffels op de walvis", "Vind alle &e8 knuffels&r die in Hoogvads verstopt zitten.", "guhs:knuffel_golden",
      [fq.met(adv("vr_z5_knuffels_alle"), "8 knuffels")], rewards=((K, 16),), shape="hexagon")
    q("vr_z5_grauwe_brief", "Aan de Ringvinger", "Vind de &8Grauwe brief&r in het bureau van de verrader.", "minecraft:paper",
      [fq.met(adv("vr_z5_grauwe_brief"), "Grauwe brief")], rewards=((K, 8),))
    q("vr_z5_uitkijken_alle", "Hoog boven Hoogvads", "Ontdek alle &e4 uitkijkpunten&r van Hoogvads.", "minecraft:spyglass",
      [fq.met(adv("vr_z5_uitkijken_alle"), "4 uitkijkpunten")], rewards=((K, 16),), shape="hexagon")
    for s, (naam, _t) in SCHATTEN.items():
        q(f"vr_z5_schat_{s}", naam, "Een verborgen schat van Hoogvads. Hij glinstert goudgeel als je dichtbij bent.",
          "minecraft:chest", [fq.met(adv(f"vr_z5_schat_{s}"), naam)], rewards=((K, 8),))


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
    return (k.startswith(Z) or k.startswith("item.guhs.vr_z5_npc_") or (k.startswith(P) and ".z5_" in k)
            or k.startswith("vadsrijk.wereld.uitkijk.z5_") or k.startswith("vadsrijk.kern.gebied.z5_"))


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "vadsrijk", "npc", "quests_z5.json"), quests_json())
    _schrijf(os.path.join(d, "vadsrijk", "z5verhaal", "speurwerk.json"), speurwerk_json())
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
                continue   # (never overwrite another slice's existing text, e.g. vadsrijk.wereld.uitkijk.onder)
            data[k] = EN.get(k, v) if taal == "en_us" else v
        _schrijf(p, data)
    print(f"vadsrijk_t_z5verhaal: {len(QUESTS)} quests ({sum(q['soort'] == 'hoofd' for q in QUESTS)} main, "
          f"{sum(q['soort'] == 'zij' for q in QUESTS)} side), {len(NPCS)} NPCs, {len(BEWIJS)} clues, {len(VERHOREN)} interrogations, "
          f"{len(t)} texts")


if __name__ == "__main__":
    _direct()
