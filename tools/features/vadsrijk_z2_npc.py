"""
1.10 "Het Vadsrijk", zone 2 "De Splinterpieken" (Z2_BRIEF.md), slice npc. Java: feature/vadsrijk/zone2/npc.

Zone 2 uses the SAME quest system as zone 1 (feature/vadsrijk/npc: Quests, Questlog, QuestMotor, the questlog J, the markers,
the tracker). This generator only adds zone-2 data:
  - the NPC guhs of Kandijkom and the zone: names, idle lines and WHERE they stand (vadsrijk.npc.plek.npc.<id>, for the tracker);
  - the zone-2 quests -> data/guhs/vadsrijk/npc/quests_z2.json (read by Quests.java after quests.json) + their texts;
  - every objective names WHO, WHERE and WHAT, and carries an explicit "plek" (npc:/poi:/gebied:/baas:) for the tracker + beam;
  - quest items, hidden FTB advancements guhs:quest/vr_z2_*, the FTB sections (ftb()), Gids pages, tips, scene texts.
Ids: NPCs and quests "z2_*", items "vr_z2_npc_*". All texts Dutch; en_us gets the same Dutch texts.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z2_npc.py
"""
import json
import os
import re

P = "vadsrijk.npc."          # (the shared quest system's keys: naam/praat/q/plek/gids/tip/adv...)
Z = "vadsrijk.z2npc."        # (zone-2 own texts: scenes, secrets, roles)
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone2", "npc")

_KK = "in Kandijkom"
# ---------------------------------------------------------------------------------------------------------------------------
# NPCs: name, 3 idle lines, WHERE (a full phrase for the tracker: vadsrijk.npc.plek.npc.<id>). Spots + clothes: Z2Npcs.java /
# Z2NpcPlekken.java.
# ---------------------------------------------------------------------------------------------------------------------------
NPCS = {
    # --- Kandijkom: travel, the order, the guard ------------------------------------------------------------------------------
    "z2_kapitein": ("Kapitein Poolster", [
        "Ijsaanslag op de romp, sneeuw in mijn snor. Heerlijk! Nou ja. Een beetje heerlijk.",
        "Terug naar De Haven? Trek je sjaal maar vast aan, het tocht boven de wolken.",
        "Ik heb ooit een ijsschots gevangen met mijn hoed. Hij smolt. De schots, niet de hoed."],
        "bij het luchtschip aan de steiger op de zuidrand van Kandijkom"),
    "z2_cafe": ("Cafébaas Mallow", [
        "Chocomelk! Met slagroom! Met marshmallows! Met nog meer slagroom!",
        "Wie koude pootjes heeft, krijgt van mij een beker. Zo werkt dat in Kandijkom.",
        "Mijn geheime recept? Een snufje kaneel en heel veel knuffels in de pan."],
        "in het chocomelkcafé aan de zuidwestkant van het plein " + _KK),
    "z2_burgemeester": ("Burgemeester Kandij", [
        "Welkom in Kandijkom, de warmste plek van de koudste bergen!",
        "Ons stadje ligt in een reuzengeode. Het roze kwarts houdt de warmte vast. En de sneeuw buiten.",
        "Ik heb een ambtsketting van kandijsuiker. Niet aan likken. Ik zie je kijken."],
        "aan de noordkant van het plein " + _KK + ", bij de kristallen klokkentoren"),
    "z2_kwarts": ("Kapitein Kwarts", [
        "De Kristalwacht bewaakt Kandijkom. Dag en nacht. Met warme sokken.",
        "Daarbuiten versteent de Vadsloosheid alles tot grauw kristal. Wij laten het hier niet binnen.",
        "Rug recht, avonturier. Een wacht is als een kristal: hard aan de buitenkant, roze vanbinnen."],
        "bij het questbord aan de noordoostkant van het plein van Kandijkom (de Kristalwacht)"),
    "z2_ordemeester": ("Ordemeester Kristalijn", [
        "Dit is het filiaal van De Knuffelorde in de bergen. Klein, maar met verwarming.",
        "Hier wissel je van klasse, net als bij de Grootmeester. En op level 30 wacht je hier een nieuwe titel.",
        "Kristal onthoudt alles. Licht, warmte, liedjes. Daarom luister ik er graag naar."],
        "in de Kristalkapel, hoog op het bovenste terras in het noordwesten van Kandijkom"),
    # --- Kandijkom: shops and services -----------------------------------------------------------------------------------------
    "z2_juwelier": ("Juweliersguh Glinster", [
        "Ruwe kristallen uit de bergen? Ik zet ze zo in je spullen. Klik, klaar.",
        "Een sokje in je zwaard, een steentje erin, en hop: sterker. Eruit halen kan ook, er gaat niks kapot.",
        "Niet alles wat glinstert is een edelsteen. Soms is het gewoon mijn bril."],
        "in de juwelierszaak aan de noordwestkant van het plein " + _KK),
    "z2_smid": ("Smid Gloeikool", [
        "Hier is het altijd warm. Dat komt door mijn oven. En door mijn humeur.",
        "Sterren erop, verpluisd eraf. Ik klop alles weer recht.",
        "IJzer smeden in de sneeuw? Pfff. Kandijkom heeft de warmste smidse van het rijk."],
        "in de smidse aan de noordoostkant van het plein van Kandijkom"),
    "z2_winkelier": ("Winkelguh Pepermunt", [
        "Warme drankjes, wollen sokken en verband met sneeuwvlokjes erop!",
        "Een pepermuntje voor onderweg? Ze zijn gratis. De drankjes niet.",
        "Mijn winkeltje ruikt naar munt. Dat houdt de verkoudheid buiten."],
        "in het kandijwinkeltje op het eerste terras aan de oostkant " + _KK),
    "z2_wapenhandelaar": ("Wapenhandelaar IJspegel", [
        "Zwaarden met een ijsrandje, bogen met wanten-grip. Alles voor de bergen!",
        "Puntig is goed. Te puntig is gevaarlijk. Ik verkoop precies goed puntig.",
        "Een avonturier zonder goed wapen is als chocomelk zonder slagroom. Treurig."],
        "in de smidse aan de noordoostkant van het plein van Kandijkom, naast Smid Gloeikool"),
    "z2_breister": ("Breister Wolwinter", [
        "Een trui voor jou, een trui voor je guh, een trui voor de trui.",
        "Alle guhs in Kandijkom dragen mijn truien. Behalve de burgemeester. Die draagt er twee.",
        "Brei, brei, brei. Dat is mijn liedje. Het heeft maar één woord."],
        "in de wantjeswinkel op het eerste terras aan de westkant " + _KK),
    "z2_veilingmeester": ("Veilingmeester Suikerspin", [
        "Eenmaal, andermaal... VERKOCHT aan de guh met de warmste muts!",
        "Ook in de bergen kun je handelen met andere avonturiers. Gebonden spullen niet, hoor.",
        "Mijn hamer is van suikerspin. Hij maakt geen geluid, maar hij ruikt wel lekker."],
        "in het veilinghuis (de oude bank) aan de zuidoostkant van het plein " + _KK),
    "z2_schaatsjuf": ("Schaatsjuf Zwierig", [
        "Rondje links, rondje rechts, pootjes van elkaar en... ZWIER!",
        "Vallen is niet erg. Opstaan is het belangrijkste. Daarna weer vallen.",
        "Ik schaats al sinds ik een guhtje was. Toen was het ijs nog roze."],
        "aan de schaatsvijver midden op het plein " + _KK),
    "z2_gondelier": ("Gondelguh Kabeltje", [
        "De gondel naar de top! Uitzicht gegarandeerd, wiebelen ook.",
        "Niet uit het raampje hangen. Ook niet als je een sneeuwhaas ziet.",
        "Mijn kabel is honderd jaar oud. Hij heeft nog nooit losgelaten. Nog nooit!"],
        "bij het gondelstation op het bovenste terras in het noordoosten van Kandijkom"),
    # --- Kandijkom: citizens with side quests -----------------------------------------------------------------------------------
    "z2_opa_sneeuwbal": ("Opa Sneeuwbal", [
        "In mijn tijd liepen we elke dag door de sneeuw naar school. Bergop. Beide kanten op.",
        "Mijn wanten zijn weg. Mijn pootjes zijn nu ijsklontjes.",
        "Kom eens hier, ik vertel je over de winter van '32. Die was koud. Einde verhaal.",
        # (WERELDVERHAAL §10 #12: a 4th idle line, NpcDef.VIER_REGELS)
        "Die winter kwam er een guh over de pieken van heel ver weg. Mager als een breinaald. Hij at drie weken onze pannenkoeken en "
        "zei geen woord over waar hij vandaan kwam."],
        "op het terras van het chocomelkcafé, aan de zuidwestkant van het plein " + _KK),
    "z2_bakker": ("Bakker Speculaas", [
        "Kandijkoekjes! Knapperig van buiten, kristal van binnen!",
        "Mijn oven is de tweede warmste plek van Kandijkom. Na het café. En de smidse. Derde dus.",
        "Wie wil er een koekje met een sneeuwvlokje erop?"],
        "bij zijn bakkerij op het terras in het zuidwesten van Kandijkom"),
    "z2_vlokje": ("Kleine Vlokje", [
        "Mijn knuffels zijn gaan schaatsen en nu zitten ze vast onder het ijs!",
        "Ik ben Vlokje. Ik ben zes. En driekwart.",
        "Als ik groot ben, word ik Kristalwacht. Met een kandijspeer!"],
        "midden op het plein " + _KK + ", bij de schaatsvijver"),
    # (fix round 2) town life: skaters, snowball kids, penguin guhs, the sledge family, market folk
    "z2_schaatser_1": ("Zwiertje", [
        "Kijk! Een pirouette! Bijna dan.",
        "Schaatsen is gewoon heel snel glijden met stijl.",
        "Als je valt, sta je op en doe je alsof het een trucje was."],
        "op de schaatsvijver midden op het plein " + _KK),
    "z2_schaatser_2": ("Glijbert", [
        "Rondje links, rondje rechts, rondje chocomelk.",
        "Ik heb het ijs gisteren nog gepoetst. Met mijn buik.",
        "Schaats je mee? Er is plek genoeg!"],
        "op de schaatsvijver midden op het plein " + _KK),
    "z2_sneeuwbal_1": ("Bolletje", [
        "Pas op! Sneeuwbal! ...Gemist. Expres natuurlijk.",
        "Wij hebben een sneeuwballenfort achter de kerstboom.",
        "Wil jij in ons team? Wij winnen altijd. Soms."],
        "bij het plein " + _KK + ", vlak bij de grote kerstboom"),
    "z2_sneeuwbal_2": ("Plofje", [
        "Raak! O nee, dat was de burgemeester...",
        "Een goede sneeuwbal is rond, zacht en een beetje roze.",
        "Bolletje gooit altijd mis. Niet zeggen dat ik dat zei."],
        "bij het plein " + _KK + ", vlak bij de grote kerstboom"),
    "z2_pinguguh_1": ("Pinguguh Waggel", [
        "Waggel waggel. Dat is mijn manier van lopen. En van dansen.",
        "Pinguguhs hebben het nooit koud. Behalve aan hun voetjes.",
        "Die kerstboom is mooi, maar ik vind de ballen het lekkerst. Om naar te kijken."],
        "bij de grote kerstboom op het plein " + _KK),
    "z2_pinguguh_2": ("Pinguguh Flapper", [
        "Ik kan niet vliegen, maar ik kan wel heel hard glijden.",
        "Schaatsen? Ik heb ingebouwde schaatsen. Mijn buik.",
        "Een visje bij de chocomelk? Nee? Jammer."],
        "bij de schaatsvijver op het plein " + _KK),
    "z2_pinguguh_3": ("Pinguguh Smoking", [
        "Ik draag altijd een smoking. Je weet nooit wanneer er een feestje is.",
        "Die kraampjes ruiken naar warme koekjes. Hmm.",
        "Waggel mee naar de markt!"],
        "op de kerstmarkt op het plein " + _KK),
    "z2_slee_1": ("Sleetje", [
        "Wij gaan zo met de slee naar beneden! Wiiiii!",
        "Papa trekt, mama remt, en ik roep wiiii.",
        "De beste helling is achter de stal. Niet verder vertellen."],
        "bij de stal langs de weg naar de haven, ten zuiden van " + _KK),
    "z2_slee_2": ("Papa Slee", [
        "Een slee, drie guhs en een thermosfles chocomelk. Dat is een goede dag.",
        "Trekken is zwaar, maar ze lachen zo hard.",
        "Pas op voor de bocht bij het dok!"],
        "bij de stal langs de weg naar de haven, ten zuiden van " + _KK),
    "z2_marktguh_1": ("Marktguh Kaneel", [
        "Warme kandijkoekjes! Twee voor de prijs van een glimlach!",
        "Alles op mijn kraam is zelfgebakken. Behalve de kaarsjes.",
        "De kerstmarkt is pas echt open als de lampions branden."],
        "op de kerstmarkt rond het plein " + _KK),
    "z2_marktguh_2": ("Marktguh Pepernoot", [
        "Sjaals! Wantjes! Mutsen met een pompon zo groot als je hoofd!",
        "Ik brei alles zelf. Ook in mijn slaap.",
        "Roze staat iedereen. Dat is wetenschap."],
        "op de kerstmarkt rond het plein " + _KK),
    "z2_marktguh_3": ("Marktguh Hulst", [
        "Kristallen kerstballen! Ze zingen zachtjes als je ze aanraakt.",
        "Ik heb ze zelf uit de Galmhellingen gehaald. Heel voorzichtig.",
        "Eentje voor in je boom? Of in je huisje?"],
        "op de kerstmarkt rond het plein " + _KK),
    "z2_uitvinder": ("Uitvinder Tandwiel", [
        "Hoor je dat gebrom? Diep in de bergen? Dat is geen lawine. Dat is een machine!",
        "Ik heb een sneeuwschuiver uitgevonden die zelf sneeuwt. Hij werkt niet helemaal goed.",
        "Tandwielen, veertjes, moertjes. Daar wordt een uitvinder blij van."],
        "bij zijn werkplaats in het noordwesten van Kandijkom"),
    "z2_galmpje": ("Koorzanger Galmpje", [
        "La la laaa... en de bergen zingen laaa terug!",
        "De kristalnaalden op de Galmhellingen zingen mee als je goed zingt. Of heel hard.",
        "Ik zing in het Kandijkoor. Wij zijn met z'n tweeën. Ik en de echo."],
        "aan de noordwestkant van het plein " + _KK),
    "z2_postguh": ("Postguh Sleetje", [
        "Post! Per slee! Wie wil er post?",
        "De post in de bergen gaat per slee. Bergaf heel snel. Bergop... minder snel.",
        "Ik heb een brief voor de berggids, de visser en de kloofwachter. Ik heb alleen geen zin om te klimmen."],
        "op het terras aan de oostkant " + _KK + ", bij het postkantoortje"),
    "z2_sneeuwpop": ("Sneeuwpop-Sjaak", [
        "Ik bouw de grootste sneeuwpop van het Vadsrijk! Hij heet ook Sjaak.",
        "Een goede sneeuwpop heeft een wortel. Een geweldige heeft er twee.",
        "Sneeuwballen gooien? Altijd! Maar nooit naar Opa Sneeuwbal. Die gooit terug."],
        "bij de sneeuwhoop op het terras in het zuidoosten van Kandijkom"),
    "z2_kasguh": ("Kasguh Groentje", [
        "Mijn kas is warm, mijn tomaatjes zijn rood, en mijn bloemetjes zijn... grijs. Hm.",
        "In de geode groeit alles. Behalve geduld. Dat moet je zelf meenemen.",
        "Groen in de sneeuw! Dat is pas toveren."],
        "bij de glazen kas aan de westrand van Kandijkom"),
    "z2_winterslaap": ("Winterslaap Wim", [
        "Zzz... is het al lente? Nee? Zzz...",
        "Ik hou van de winter. Je mag de hele tijd slapen. Alleen het is te koud om te slapen.",
        "Een warme kruik, een dikke deken, een knuffel. Meer heb je niet nodig."],
        "voor zijn huisje op het terras aan de noordwestkant van Kandijkom"),
    "z2_poollicht": ("Poollicht Pien", [
        "'s Nachts danst het licht boven de pieken. Groen en roze. Prachtig!",
        "Ik teken sterrenkaarten. De sterren hier hebben guh-oortjes.",
        "Vanaf de hoge uitkijk zie je ver weg een reuzenboom. Niemand weet wat daar is.",
        # (WERELDVERHAAL §10 #9: a 4th idle line, NpcDef.VIER_REGELS)
        "Ik teken sterrenbeelden met guh-oortjes. Maar aan de verre rand hangt een grauwe mistbank waar geen ster doorheen komt. Hij komt elk jaar een "
        "stukje dichterbij."],
        "voor haar huisje op het terras aan de noordoostkant van Kandijkom"),
    # --- out in the zone -------------------------------------------------------------------------------------------------------
    "z2_gondel_boven": ("Gondelguh Katrol", [
        "Welkom op de top! Pas op, het waait hier een beetje. Hou je oortjes vast.",
        "Terug naar beneden? De gondel staat klaar. Hij wiebelt maar een klein beetje.",
        "Mijn broer Kabeltje zit beneden. Ik zit boven. Wij praten via de kabel. Hij hoort nooit wat."],
        "bij het bovenste gondelstation, hoog op de Galmtop in het noordoosten van de Galmhellingen"),
    "z2_berggids": ("Berggids Klimpoot", [
        "Volg altijd de vlaggetjes. En nooit de sneeuwhazen: die weten de weg ook niet.",
        "Als de naalden zingen, komt er iets aan. Luister goed.",
        "Ik heb elke top beklommen. Twee keer. De tweede keer om mijn muts te halen."],
        "bij het eerste kampvuur aan de westrand van de Galmhellingen, ten oosten van Kandijkom"),
    "z2_visser": ("IJsvisser Wak", [
        "Een gat in het ijs, een hengel erin, en wachten. Heel lang wachten.",
        "Onder het ijs zwemmen vissen. En knuffels. Die bijten niet. Jammer.",
        "Mijn wak is het mooiste wak van het meer. Rond en alles."],
        "op het eilandje midden op het Bevroren Knuffelmeer, ten westen van Kandijkom"),
    "z2_beeldhouwer": ("IJsbeeldhouwer Beitel", [
        "Tik, tik, tik. Uit een ijsblok komt altijd een guh. Als je maar lang genoeg tikt.",
        "Mijn beelden smelten in de zomer. Daarom is het hier altijd winter. Handig.",
        "Ik heb rare dingen gezien. Kleine figuurtjes met sleeën vol kristal. Richting de kloof."],
        "aan de noordoever van het Bevroren Knuffelmeer, bij het kampvuur en zijn ijsbeelden"),
    "z2_touwtje": ("Kloofwachter Touwtje", [
        "Hangbruggen wiebelen. Dat hoort zo. Denk ik.",
        "Ik ken elke brug van de Splinterkloof. En elk touwtje. Ik heet er niet voor niks zo.",
        "Daar beneden bromt het. Al weken. Mika-machines, zeg ik je."],
        "bij de brede hangbrug naar de Spiegelburcht, in het oosten van de Splinterkloof"),
    "z2_prisma": ("Lichtwachter Prisma", [
        "De Spiegelburcht was ooit het lichtste paleis van het rijk. Nu is ze grauw.",
        "In de burcht moet je het licht laten kaatsen. Spiegel na spiegel.",
        "Vrouwe Spiegelzucht... ze was ooit zo vrolijk. Ze bewonderde zichzelf de hele dag. Nu ziet ze alleen nog grijs."],
        "bij de poort van de Spiegelburcht, op de hoogste piek in het noordoosten"),
    # --- the petrified guhs at the lake (grey crystal; you free them, never hurt) ------------------------------------------------
    "z2_versteend_zwier": ("Schaatser Zwier (versteend)", [
        "...(Een guh van grauw kristal, midden in een pirouette. Je hoort heel zachtjes: 'help...')",
        "...(Zijn schaatsjes glinsteren nog een beetje roze.)",
        "...(Het kristal is koud, maar vanbinnen klopt iets warms.)"],
        "midden op het ijs van het Bevroren Knuffelmeer"),
    "z2_versteend_snoek": ("Visser Snoek (versteend)", [
        "...(Een visser van grauw kristal, met een hengel in zijn pootjes. 'Beet... gehad...')",
        "...(Aan zijn hengel hangt een kristallen visje.)",
        "...(Je hoort een heel klein snurkje.)"],
        "bij de ijsschotsen in het zuiden van het Bevroren Knuffelmeer"),
    "z2_versteend_pluk": ("Knuffelmaker Pluk (versteend)", [
        "...(Een guh van grauw kristal, met een half gebreide knuffel in haar pootjes.)",
        "...(Op haar kristallen schort staat 'Pluk'.)",
        "...('Mijn... knuffels... onder het ijs...')"],
        "aan de westkant van het ijs van het Bevroren Knuffelmeer"),
    "z2_versteend_ijsje": ("Kleine IJsje (versteend)", [
        "...(Een heel klein guhtje van grauw kristal. Het heeft een sneeuwbal in zijn pootje.)",
        "...(Zijn oortjes staan nog recht van schrik.)",
        "...('Mama...?')"],
        "op het eilandje midden op het Bevroren Knuffelmeer, naast IJsvisser Wak"),
}
# the Kristalwacht (Z2NpcPlekken z2_wacht_*): name, where; they share their lines (like zone 1's campfire knights)
WACHTEN = {
    "z2_wacht_plein_west": ("Brokje", "aan de westkant van het plein " + _KK),
    "z2_wacht_plein_oost": ("IJzel", "aan de oostkant van het plein " + _KK),
    "z2_wacht_dok": ("Splinter", "bij het kampvuur aan de luchtschipsteiger van Kandijkom"),
    "z2_wacht_galm_1": ("Rijp", "bij het kampvuur aan de westrand van de Galmhellingen"),
    "z2_wacht_galm_2": ("Sneeuwvlok", "bij een kampvuur in het zuiden van de Galmhellingen"),
    "z2_wacht_galm_3": ("Poolwind", "bij een kampvuur in het oosten van de Galmhellingen"),
    "z2_wacht_galm_4": ("Ijspegel", "bij een kampvuur in het noorden van de Galmhellingen"),
    "z2_wacht_meer_1": ("Hagel", "bij het kampvuur aan de zuidoostoever van het Bevroren Knuffelmeer"),
    "z2_wacht_meer_2": ("Wak", "bij een kampvuur aan de westoever van het Bevroren Knuffelmeer"),
    "z2_wacht_meer_3": ("Dooi", "bij het kampvuur aan de noordoever van het Bevroren Knuffelmeer"),
    "z2_wacht_meer_4": ("Rijm", "bij een kampvuur in het noordwesten van het Bevroren Knuffelmeer"),
    "z2_wacht_kloof_1": ("Gruis", "bij een kampvuur in het westen van de Splinterkloof"),
    "z2_wacht_kloof_2": ("Kei", "bij een kampvuur in het oosten van de Splinterkloof"),
    "z2_wacht_kloof_3": ("Kwartsje", "bij het kampvuur bij de middelste hangbrug van de Splinterkloof"),
    "z2_wacht_kloof_4": ("Diepje", "bij het kampvuur op de bodem van de Splinterkloof"),
    "z2_wacht_kloof_5": ("Hoogop", "bij een kampvuur hoog in het noordwesten van de Splinterkloof"),
    "z2_wacht_burcht": ("Glans", "bij het kampvuur onder de trap van de Spiegelburcht"),
}
WACHT_REGELS = [
    "Steek het kampvuur aan: hier word je wakker als je flauwvalt. Warm en veilig. Je verliest niks.",
    "De Kristalwacht bewaakt de Splinterpieken. Met speren van kandij en heel warme sokken.",
    "Hoor je die naalden zingen? Als ze vals zingen, komt er iets aan. Blijf alert, avonturier."]
for _id, (_naam, _waar) in WACHTEN.items():
    NPCS[_id] = ("Kristalwacht " + _naam, WACHT_REGELS, _waar)

VERSTEEND_VRIJ = {   # what a freed guh says to you afterwards (KLIK_VOOR in Z2Haken)
    "z2_versteend_zwier": "Ik kan weer zwieren! Kijk: rondje links, rondje rechts... oei. Dank je, avonturier!",
    "z2_versteend_snoek": "Ik heb drie dagen stilgezeten. Dat was mijn beste visdag ooit. Nee hoor, grapje. Dank je wel!",
    "z2_versteend_pluk": "Mijn pootjes! Ik kan weer breien! De knuffels onder het ijs... die zijn van mij. Wil je ze helpen?",
    "z2_versteend_ijsje": "Hihi, het kriebelde toen het kristal brak! Ik ga naar huis, naar Kandijkom. Dank je!",
}

# ---------------------------------------------------------------------------------------------------------------------------
# Where: POIs / areas / campfires / boss arenas (vadsrijk.npc.plek.<kind>.<id>), phrases for the tracker and the questlog.
# ---------------------------------------------------------------------------------------------------------------------------
PLEK_POI = {
    "z2_dok": "aan de luchtschipsteiger op de zuidrand van Kandijkom",
    "z2_plein": "midden op het plein van Kandijkom",
    "z2_schaatsvijver": "op het ijs van de schaatsvijver, midden op het plein van Kandijkom",
    "z2_galmnaald": "bij de grote gebarsten Galmnaald boven het hol van de Galmwolf, in het oosten van de Galmhellingen",
    "z2_naald_do": "bij de lage zingende naald in het zuiden van de Galmhellingen",
    "z2_naald_re": "bij de middelste zingende naald, midden op de Galmhellingen",
    "z2_naald_mi": "bij de hoge zingende naald in het noorden van de Galmhellingen",
    "z2_lawinepad": "aan de voet van het grote lawinepad, in het noorden van de Galmhellingen",
    "z2_sneeuwpophelling": "op de zachte sneeuwhelling helemaal in het zuiden van de Galmhellingen",
    "z2_warmwaterbron": "bij de warmwaterbron in het zuidoosten van de Galmhellingen (de stoom wijst de weg)",
    "z2_gondel_top": "bij het bovenste gondelstation, hoog in het noorden van de Galmhellingen (neem de gondel aan de noordoostkant van Kandijkom)",
    "z2_meer_scheur": "bij de grote scheur in het ijs, in het noorden van het Bevroren Knuffelmeer",
    "z2_brug_1": "op de middelste hangbrug van de Splinterkloof, bij Kloofwachter Touwtje",
    "z2_brug_2": "op de westelijke hangbrug van de Splinterkloof",
    "z2_brug_3": "op de verste hangbrug, helemaal in het westen van de Splinterkloof",
    "z2_boortoren": "bij de grote Mika-boortoren in het oosten van de Splinterkloof",
    "z2_boortoren_2": "bij de Mika-boortoren in het westen van de Splinterkloof",
    "z2_drakenravijn": "op het bergzadel ten noorden van het Bevroren Knuffelmeer, aan de zuidwestrand van de Splinterkloof",
    "z2_boorhal": "in de verborgen boorhal op de bodem van de Splinterkloof, helemaal in het westen (zoek de spleet waar warme lucht uit komt)",
    "z2_spiegelpoort": "bij de poort van de Spiegelburcht, op de hoogste piek in het noordoosten (achter de Splinterkloof)",
    "z2_uitkijk_poollicht": "bovenop de hoge kristalnaald in het noordwesten van de Splinterkloof (de Kloofkijk)",
    "z2_kampvuur_galm": "bij het kampvuur van Berggids Klimpoot, aan de westrand van de Galmhellingen",
    "z2_kampvuur_meer": "bij het kampvuur aan de oostoever van het Bevroren Knuffelmeer",
    "z2_kampvuur_kloof": "bij het kampvuur aan de zuidrand van de Splinterkloof",
    "z2_wolfje": "op de bodem van de Splinterkloof, onderaan de tweede trap naar beneden (ten westen van de middelste hangbrug)",
}
PLEK_GEBIED = {
    "z2_kandijkom": "in Kandijkom, de warme geode van de Splinterpieken",
    "z2_galmhellingen": "op de Galmhellingen, ten oosten van Kandijkom",
    "z2_knuffelmeer": "bij het Bevroren Knuffelmeer, ten westen van Kandijkom",
    "z2_splinterkloof": "in de Splinterkloof, ten noorden van Kandijkom",
    "z2_spiegelburcht": "in de Spiegelburcht, op de hoogste piek in het noordoosten",
    "z2_kandijkom_dok": "bij de luchtschipsteiger van Kandijkom",
    "z2_ring": "in de Splinterpieken",
}
PLEK_KAMPVUUR = {
    "z2_kandijkom": "bij de luchtschipsteiger van Kandijkom",
    "z2_kandijkom_dok": "bij de luchtschipsteiger van Kandijkom",
    "z2_galmhellingen": "aan de westrand van de Galmhellingen, bij Berggids Klimpoot",
    "z2_knuffelmeer": "aan de oostoever van het Bevroren Knuffelmeer",
    "z2_splinterkloof": "aan de zuidrand van de Splinterkloof",
    "z2_spiegelburcht": "onderaan de trap naar de Spiegelburcht",
}
PLEK_BAAS = {
    "z2_galmhellingen": "in het hol van de Galmwolf, in het oosten van de Galmhellingen (op het bord bij de ingang zie je of hij wakker is)",
    "z2_spiegelburcht": "in de Spiegelzaal achterin de Spiegelburcht: door de spiegelgangen naar achteren, kaats het licht naar elke deur",
    "z2_splinterkloof": "in de verborgen boorhal op de bodem van de Splinterkloof, helemaal in het westen",
}
PLEK_OVERIG = {
    P + "plek.overal.teller:z2_knuffels": "onder het ijs van het Bevroren Knuffelmeer, ten westen van Kandijkom (ze glinsteren met hartjes)",
    P + "plek.overal.teller:z2_aanwijzingen": "in de Splinterkloof, op de Galmhellingen en in Kandijkom (paarse glinsters)",
    P + "plek.overal.teller:z2_schatten": "op geheime plekjes in de Splinterpieken",
    P + "plek.z2_luchtschip": "bij Stuurguh Zeppie, aan het luchtschip in De Haven (ten zuiden van Zwamzoet)",
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quest items (Z2NpcRegistry.QUEST_ITEMS): name, vanilla texture
# ---------------------------------------------------------------------------------------------------------------------------
# NPCs with a 4th idle line (WERELDVERHAAL §10); keep in step with NpcDef.VIER_REGELS in Java
VIER_REGELS = {"z2_poollicht", "z2_opa_sneeuwbal"}

QUEST_ITEMS = {
    "galmscherf": ("Galmscherf (hij zoemt een beetje)", "minecraft:item/amethyst_shard"),
    "wantje": ("Want van Opa Sneeuwbal", "minecraft:item/leather"),
    "kandijkristal": ("Kandijkristal", "minecraft:item/quartz"),
    "sneeuwbal": ("Extra stevige sneeuwbal", "minecraft:item/snowball"),
    "gloeikristal": ("Gloeikristal", "minecraft:item/glowstone_dust"),
    "tandwiel": ("Mika-tandwieltje", "minecraft:item/iron_nugget"),
    "boorbeitel": ("Mika-boorbeitel", "minecraft:item/iron_ingot"),
    "ijsbloem": ("IJsbloem", "minecraft:block/blue_orchid"),
    "ijsvis": ("Zilveren ijsvis", "minecraft:item/cod"),
    "wolpluk": ("Plukje bergwol", "minecraft:item/string"),
    "hartsplinter": ("Roze hartsplinter van Vahoegnir", "minecraft:item/pink_dye"),
    "drakensplinter": ("Grauwe drakensplinter", "minecraft:item/flint"),
    "chocomelk": ("Warme chocomelk", "minecraft:item/honey_bottle"),
}
# flavour lines under a quest item (item.guhs.vr_z2_npc_<id>.lore; Z2NpcRegistry gives these items an ItemLore). WERELDVERHAAL §10 #8.
QUEST_ITEM_LORE = {
    "drakensplinter": "Grauw, hard en een beetje gebogen. Net een nagelknipsel. Van iets héél groots. Bah.",
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quests. D(type, doel, n, text, bron, kans, plek): text = WHAT + WHO + WHERE; plek = where the tracker points.
# ---------------------------------------------------------------------------------------------------------------------------
QUESTS = []


def D(soort, doel, n, tekst, bron="", kans=0.0, plek=""):
    return dict(soort=soort, doel=doel, n=n, tekst=tekst, bron=bron, kans=kans, plek=plek)


def Q(id, soort, gever, titel, tekst, doelen, aanbod, bezig, klaar, xp=0, munten=0, items=(), wapen=False, loot="", lv=20, na=(),
      inleveren=None, klasse=None, auto=False, praat=None, actie_aanname="", actie_klaar="", start_items=()):
    QUESTS.append(dict(id=id, soort=soort, gever=gever, titel=titel, tekst=tekst, doelen=doelen, aanbod=aanbod, bezig=bezig, klaar=klaar,
                       beloning=dict(xp=xp, munten=munten, items=[list(i) for i in items], wapen=wapen, loot=loot), lv=lv, na=list(na),
                       inleveren=inleveren or gever, klasse=klasse, auto=auto, praat=praat or {}, actie_aanname=actie_aanname,
                       actie_klaar=actie_klaar, start_items=[list(i) for i in start_items]))


def item(id):
    return "guhs:vr_z2_npc_" + id


# ===== the main story of zone 2 (12) ========================================================================================
Q("z2_hq01_splinterpieken", "hoofd", "luchtschipper", "Naar de Splinterpieken",
  "Grootmeester Vadsvoet zag Vahoegnir boven de kristalspitsen in het noorden cirkelen. Stuurguh Zeppie in De Haven heeft een "
  "nieuwe luchtschiplijn: naar Kandijkom, het warme stadje in de Splinterpieken. Vlieg mee en praat daar met Cafébaas Mallow.",
  [D("gebied", "z2_kandijkom|z2_kandijkom_dok", 1, "Vlieg met Stuurguh Zeppie vanuit De Haven naar Kandijkom", plek="npc:luchtschipper"),
   D("praat", "z2_cafe", 1, "Praat met Cafébaas Mallow in het chocomelkcafé aan de zuidoostkant van het plein van Kandijkom",
     plek="npc:z2_cafe")],
  [("npc", "Avonturier! Heb je het al gehoord? De draak is gezien boven de kristalspitsen in het noorden. De Splinterpieken."),
   ("npc", "Daar ligt een stadje, Kandijkom, in een reuzengeode van roze kwarts. Warm vanbinnen, ijskoud erbuiten. Ik vlieg er "
           "sinds kort naartoe."),
   ("jij", "Is het daar gevaarlijk?"),
   ("npc", "Een beetje. Er zit ijs op de route. En sneeuwstormen. En... nou ja. Stap maar in, dan zie je het vanzelf. Doe je sjaal om!")],
  "Stap in bij mij, hier in De Haven, en vlieg naar Kandijkom. Daar wacht Cafébaas Mallow met chocomelk.",
  [("npc", "Daar ben je! Helemaal bevroren! Wat een vlucht, hè? Ik zag het luchtschip binnenglijden met een ijspegel aan zijn neus."),
   ("npc", "Hier, een beker warme chocomelk. Met slagroom. Met marshmallows. Langzaam drinken, hij is heet."),
   ("jij", "Er stond een wolf op een kristalnaald. Hij huilde, en toen barstte de naald..."),
   ("npc", "Een Galmwolf. Brrr. Dat moet de burgemeester weten. Loop naar de burgemeester aan de noordkant van het plein, en meld "
           "je daarna bij Kapitein Kwarts van de Kristalwacht, bij het questbord.")],
  xp=3000, munten=2000, items=[(item("chocomelk"), 2)], lv=20, na=["hq12_schaduw"], inleveren="z2_cafe",
  actie_aanname="z2_vlieg:kandijkom")

Q("z2_hq02_kandijkom", "hoofd", "z2_cafe", "Welkom in Kandijkom",
  "Kandijkom ligt veilig in zijn roze geode, maar buiten wordt de Vadsloosheid sterker: wezens versteenen tot grauw kristal. Praat "
  "met Burgemeester Kandij op het plein en meld je bij Kapitein Kwarts van de Kristalwacht.",
  [D("praat", "z2_burgemeester", 1, "Praat met Burgemeester Kandij aan de noordkant van het plein van Kandijkom, voor de grote sneeuwguh-spar",
     plek="npc:z2_burgemeester"),
   D("praat", "z2_kwarts", 1, "Meld je bij Kapitein Kwarts van de Kristalwacht, bij het questbord op het plein van Kandijkom", plek="npc:z2_kwarts")],
  [("npc", "Drink eerst je chocomelk op. Zo. En nu: naar de burgemeester. Hij weet alles van de bergen."),
   ("npc", "De burgemeester staat aan de noordkant van het plein, voor de grote sneeuwguh-spar. Daarna Kapitein Kwarts van de "
           "Kristalwacht, bij het questbord.")],
  "De burgemeester aan de noordkant van het plein, en daarna Kapitein Kwarts bij het questbord.",
  [("npc", "Zo. Jij bent de avonturier van de Orde. De burgemeester stuurde een briefje. Met hartjes. Hij overdrijft altijd."),
   ("npc", "Luister. Sinds kort zingen de kristalnaalden op de Galmhellingen vals. Wezens worden grauw en hard als steen. Splinterlingen, "
           "noemen we ze. En gisteren huilde er een Galmwolf."),
   ("npc", "De Kristalwacht houdt de poorten. Maar wij kunnen niet naar buiten. Jij wel. Begin bij de Galmhellingen, in het oosten.")],
  xp=2500, munten=1500, na=["z2_hq01_splinterpieken"], inleveren="z2_kwarts",
  praat={"z2_burgemeester": [
      ("npc", "Welkom, welkom! In Kandijkom, de warmste plek van de koudste bergen! Ik ben Burgemeester Kandij."),
      ("npc", "Onze geode houdt de kou buiten. Maar niet de zorgen. Er zijn guhs versteend bij het meer. Grauw kristal, van top tot "
              "teen. Ze leven nog, hoor. Ze kunnen alleen niet meer bewegen."),
      ("jij", "Versteend? Door wie?"),
      ("npc", "Door de Vadsloosheid, zegt men. Kapitein Kwarts van de Kristalwacht weet meer. Hij staat bij het questbord, aan de noordoostkant van het plein.")]})

Q("z2_hq03_galmhellingen", "hoofd", "z2_kwarts", "De Galmhellingen",
  "Ten oosten van Kandijkom liggen de Galmhellingen: sneeuwvelden met kristalnaalden die terugzingen als je ze raakt. Steek het "
  "kampvuur van Berggids Klimpoot aan, praat met hem en versla de kristalwezens die daar rondlopen.",
  [D("kampvuur", "z2_galmhellingen", 1, "Steek het kampvuur aan bij Berggids Klimpoot: door de oostpoort van Kandijkom, dan het pad "
                                          "omhoog naar de westrand van de Galmhellingen",
     plek="poi:z2_kampvuur_galm"),
   D("praat", "z2_berggids", 1, "Praat met Berggids Klimpoot bij dat kampvuur", plek="npc:z2_berggids"),
   D("opvrolijk", "gebied:z2_galmhellingen", 12, "Versla 12 wezens op de Galmhellingen, ten oosten van Kandijkom",
     plek="gebied:z2_galmhellingen")],
  [("npc", "De Galmhellingen. Ga de geode uit door de oostpoort, aan het eind van de straat langs de Kandijkraam, en volg het pad omhoog. Bij het eerste kampvuur zit Berggids Klimpoot."),
   ("npc", "Steek dat vuur aan: daar word je wakker als het misgaat. En pas op voor de Splinterlingen. Ze schieten scherven als je "
           "ze raakt."),
   ("npc", "Hier geldt geen opvrolijk-plof, avonturier. Dit zijn echte monsters. Grauw kristal zonder hart. Als je ze verslaat, "
           "spatten ze uit elkaar in scherven die wegsmelten.")],
  "De geode uit door de oostpoort (de straat langs de Kandijkraam), het pad omhoog, naar het kampvuur van Berggids Klimpoot.",
  [("npc", "Twaalf! Goed werk. De helling is al een beetje stiller. Alleen de naalden zingen nog vals."),
   ("npc", "Weet je, die naalden zingen altijd. Maar sinds kort klinken ze... boos. Ik denk dat er iets in de scherven zit.")],
  xp=4000, munten=2500, loot="zeldzaam", lv=20, na=["z2_hq02_kandijkom"], inleveren="z2_berggids",
  praat={"z2_berggids": [
      ("npc", "Een avonturier! Mooi. Ik ben Klimpoot, berggids. Ik ken hier elke steen. En elke sneeuwhaas bij naam."),
      ("npc", "Zie je die grauwe wezens tussen de naalden? Splinterlingen en IJzelgeesten. Vroeger waren het gewone sneeuwbeestjes. "
              "Versla er een stuk of twaalf, dan kunnen we verder kijken.")]})

Q("z2_hq04_naalden", "hoofd", "z2_berggids", "Het lied van de naalden",
  "Berggids Klimpoot wil weten waarom de kristalnaalden vals zingen. Verzamel galmscherven van de wezens op de Galmhellingen en "
  "klim naar de gebarsten Galmnaald, waar de Galmwolf huilde.",
  [D("verzamel", item("galmscherf"), 6, "Verzamel 6 galmscherven van de wezens op de Galmhellingen", "gebied:z2_galmhellingen", 0.4,
     plek="gebied:z2_galmhellingen"),
   D("gebied", "poi:z2_galmnaald", 1, "Klim naar de gebarsten Galmnaald boven het hol van de Galmwolf, in het oosten van de Galmhellingen",
     plek="poi:z2_galmnaald")],
  [("npc", "Als je een Splinterling verslaat, blijft er soms een scherf liggen die zoemt. Een galmscherf. Breng er zes."),
   ("npc", "En klim dan naar de Galmnaald. Die grote, in het oosten, boven het hol van de Galmwolf. Die is gebarsten toen de Galmwolf erop huilde. Kijk wat "
           "je daar ziet.")],
  "Zes galmscherven, en een kijkje bij de gebarsten Galmnaald in het oosten.",
  [("npc", "Laat zien... Hm. Hoor je dat? Ze zoemen allemaal hetzelfde toontje. Laag. Grauw. Als een grom."),
   ("jij", "Bij de naald lagen pootafdrukken. Heel groot. En de sneeuw was grijs."),
   ("npc", "De Galmwolf. Hij huilt de Vadsloosheid de bergen in, en de naalden zingen het na. Zolang hij huilt, versteent alles.")],
  xp=4500, munten=3000, lv=21, na=["z2_hq03_galmhellingen"])

Q("z2_hq05_galmwolf", "hoofd", "z2_berggids", "De Galmwolf",
  "De Galmwolf huilt de Vadsloosheid de Galmhellingen in. Versla hem in zijn hol bovenaan de helling. Als hij huilt, rollen er "
  "kristalgolven over de sneeuw: schuil achter de bevroren pilaren. Maar pas op, ze breken één voor één. Daarna terug naar "
  "Kapitein Kwarts in Kandijkom.",
  [D("baas", "gebied:z2_galmhellingen", 1, "Versla de Galmwolf in zijn hol in het oosten van de Galmhellingen (schuil achter de bevroren pilaren "
     "als hij huilt)", plek="baas:z2_galmhellingen")],
  [("npc", "Er is maar één manier om de naalden weer zuiver te laten zingen. De Galmwolf moet stil."),
   ("npc", "Zijn hol ligt in het oosten van de helling. Als hij huilt, rolt er een kristalgolf over de sneeuw. Schuil achter een bevroren "
           "pilaar! Maar die pilaren breken, één voor één. Wees snel."),
   ("npc", "Ga met vrienden als dat kan. Ik wacht hier met warme thee.")],
  "Het hol van de Galmwolf ligt in het oosten van de Galmhellingen. Schuil achter de pilaren als hij huilt!",
  [("npc", "Je hebt de Galmwolf verslagen! Ik hoorde het tot hier: de hele berg zuchtte."),
   ("npc", "De naalden zingen weer zuiver. Nou ja, bijna. De Kristalwacht is je dank verschuldigd."),
   ("npc", "Maar er is meer. Bij het Bevroren Knuffelmeer, in het westen, zijn guhs versteend. Echte guhs. Wil jij gaan kijken?")],
  xp=7000, munten=5000, loot="episch", lv=23, na=["z2_hq04_naalden"], inleveren="z2_kwarts")

_VERSTEEND = [("z2_versteend_zwier", "Schaatser Zwier", "midden op het ijs van het meer"),
              ("z2_versteend_snoek", "Visser Snoek", "bij de ijsschotsen in het zuiden van het meer"),
              ("z2_versteend_pluk", "Knuffelmaker Pluk", "aan de westkant van het ijs"),
              ("z2_versteend_ijsje", "Kleine IJsje", "op het eilandje midden in het meer, naast Wak")]
_BEVRIJD_ZIN = {
    "z2_versteend_zwier": [("verteller", "Je legt je pootje op het grauwe kristal. Het is koud. Dan voel je iets kloppen, heel "
                                         "zachtjes. Je denkt aan iets fijns: warme chocomelk, een knuffel, een kampvuur..."),
                           ("verteller", "KRRRAK! Het kristal barst en valt als grijze sneeuw van hem af. Hij knippert met zijn ogen."),
                           ("npc", "...Oei. Hé! Ik kan weer bewegen! Was ik... een standbeeld? Een mooi standbeeld, hoop ik?")],
    "z2_versteend_snoek": [("verteller", "Je klopt zachtjes op het kristal en denkt aan iets vrolijks. Het kristal begint roze te "
                                         "gloeien, en dan... KRAK!"),
                           ("npc", "...Beet! BEET! O. Nee. Ik ben gewoon wakker. Hoe lang stond ik hier?")],
    "z2_versteend_pluk": [("verteller", "Het kristal rond Pluk is dik en grauw. Je drukt je oortjes ertegen en neuriet een liedje. "
                                        "Het kristal neuriet terug... en springt in stukjes."),
                          ("npc", "Mijn knuffels... Ik was knuffels aan het breien voor de kinderen van Kandijkom. Ze zijn onder het "
                                  "ijs gegleden toen het kristal kwam. Dank je wel, lieverd.")],
    "z2_versteend_ijsje": [("verteller", "Het kleinste beeldje van allemaal. Je geeft het grauwe kristal een knuffel. Een hele grote."),
                           ("verteller", "Krrrk... krak... PLOF! Het kristal valt eraf als poedersuiker."),
                           ("npc", "Hihi! Dat kriebelde! Ben jij een ridder? Je ruikt naar chocomelk.")],
}
Q("z2_hq06_versteend", "hoofd", "z2_kwarts", "Versteend!",
  "Bij het Bevroren Knuffelmeer, ten westen van Kandijkom, zijn vier guhs versteend tot grauw kristal. Praat met IJsvisser Wak bij "
  "zijn wak en bevrijd de vier guhs: leg je pootje op het kristal en denk aan iets fijns. Guhs doe je nooit pijn.",
  [D("praat", "z2_visser", 1, "Praat met IJsvisser Wak op het eilandje midden op het Bevroren Knuffelmeer, ten westen van Kandijkom",
     plek="npc:z2_visser")]
  + [D("praat", id, 1, f"Bevrijd {naam}: klik op het grauwe kristal {waar}", plek="npc:" + id) for id, naam, waar in _VERSTEEND],
  [("npc", "Het Bevroren Knuffelmeer ligt in het westen. Ga de geode uit naar het westen. Daar zit IJsvisser Wak, midden op het ijs."),
   ("npc", "Vier guhs zijn daar versteend. Ze leven nog. Ze zitten alleen vast in het kristal."),
   ("jij", "Hoe krijg ik ze eruit?"),
   ("npc", "Niet met je wapen! Nooit. Vraag het Wak. Hij zag het gebeuren.")],
  "Het meer ligt in het westen. Eerst IJsvisser Wak, dan de vier versteende guhs.",
  [("npc", "Alle vier vrij! Zwier zwiert alweer, Snoek vist alweer, Pluk breit alweer en IJsje is naar huis gerend."),
   ("npc", "Weet je wat raar is? Toen het kristal kwam, kraakte het ijs. En onder het ijs bewoog iets. Heel veel kleine dingen. "
           "En iets groots.")],
  xp=7500, munten=5000, loot="zeldzaam", lv=24, na=["z2_hq05_galmwolf"], inleveren="z2_visser",
  praat=dict({"z2_visser": [
      ("npc", "Pssst. Zachtjes. Het ijs is dun daar. En de wezens zijn wakker."),
      ("npc", "Ik zag het gebeuren. Een grauwe wind waaide over het meer, en waar hij langskwam werden guhs stil. Grijs. Hard. Als "
              "kristal."),
      ("jij", "Hoe bevrijd ik ze?"),
      ("npc", "Kristal onthoudt wat je voelt. Leg je pootje erop en denk aan iets fijns. Dan barst het. Ik heb het al geprobeerd, "
              "maar ik dacht aan vis. Dat werkte niet.")]},
      **{id: _BEVRIJD_ZIN[id] for id, _n, _w in _VERSTEEND}))

Q("z2_hq07_onder_het_ijs", "hoofd", "z2_visser", "Onder het ijs",
  "Er beweegt iets onder het ijs van het Knuffelmeer. Versla de ijswezens rond het meer, en een sterke elite, en bekijk de grote "
  "scheur in het noorden van het ijs. IJsbeeldhouwer Beitel, op de noordoever, heeft iets gezien.",
  [D("opvrolijk", "gebied:z2_knuffelmeer", 15, "Versla 15 wezens rond het Bevroren Knuffelmeer, ten westen van Kandijkom",
     plek="gebied:z2_knuffelmeer"),
   D("elite", "gebied:z2_knuffelmeer", 1, "Versla een elite bij het Bevroren Knuffelmeer (een groot wezen met een gloeiende naam)",
     plek="gebied:z2_knuffelmeer"),
   D("gebied", "poi:z2_meer_scheur", 1, "Bekijk de grote scheur in het ijs, in het noorden van het Bevroren Knuffelmeer",
     plek="poi:z2_meer_scheur"),
   D("praat", "z2_beeldhouwer", 1, "Praat met IJsbeeldhouwer Beitel bij zijn ijsbeelden op de noordoever van het meer",
     plek="npc:z2_beeldhouwer")],
  [("npc", "Die grote dingen onder het ijs... ze komen boven als je hard slaat. Het ijs scheurt dan. Pas op waar je staat!"),
   ("npc", "Versla ze, en kijk bij de grote scheur in het noorden. Op de noordoever zit IJsbeeldhouwer Beitel. Die ziet alles, hij "
           "kijkt de hele dag naar ijs.")],
  "Versla de wezens rond het meer, bekijk de scheur in het noorden en praat met Beitel op de noordoever.",
  [("npc", "Tik. Tik. O, hallo. Je bent bij de scheur geweest? Dan heb je de sleepsporen gezien."),
   ("npc", "Ik heb ze 's nachts zien lopen. Kleine figuurtjes met puntoortjes en helmpjes. Met sleeën vol grauw kristal. Ze trokken "
           "ze naar het noorden, naar de Splinterkloof."),
   ("jij", "Mika's!"),
   ("npc", "Mika's? Die van de kaas? Ze miauwden wel. En ze zongen een liedje over boren. Heel vals.")],
  xp=8000, munten=5500, loot="zeldzaam", lv=25, na=["z2_hq06_versteend"], inleveren="z2_beeldhouwer")

Q("z2_hq08_splinterkloof", "hoofd", "z2_beeldhouwer", "De Splinterkloof",
  "De sleepsporen leiden naar de Splinterkloof, ten noorden van Kandijkom: steile wanden, wiebelende hangbruggen en Mika-boorinstallaties "
  "die kristal oogsten. Praat met Kloofwachter Touwtje, verjaag de Mika-mijnwerkers en vind de grote boortoren.",
  [D("praat", "z2_touwtje", 1, "Praat met Kloofwachter Touwtje bij de middelste hangbrug van de Splinterkloof, ten noorden van Kandijkom",
     plek="npc:z2_touwtje"),
   D("opvrolijk", "gebied:z2_splinterkloof", 12, "Versla 12 wezens of Mika-mijnwerkers in de Splinterkloof (verslagen Mika's vluchten in "
     "een rookwolk naar huis)", plek="gebied:z2_splinterkloof"),
   D("verzamel", item("boorbeitel"), 3, "Pak 3 Mika-boorbeitels af van de Mika-mijnwerkers in de Splinterkloof", "gebied:z2_splinterkloof", 0.35,
     plek="gebied:z2_splinterkloof"),
   D("gebied", "poi:z2_boortoren", 1, "Vind de grote Mika-boortoren in het oosten van de Splinterkloof", plek="poi:z2_boortoren")],
  [("npc", "De kloof ligt in het noorden. Ga de geode uit naar het noorden en volg het pad omhoog. Bij de middelste hangbrug staat "
           "Kloofwachter Touwtje."),
   ("npc", "Neem een paar boorbeitels mee als bewijs. Dan geloven ze je in Kandijkom wel.")],
  "De Splinterkloof ligt in het noorden. Eerst Kloofwachter Touwtje bij de middelste hangbrug.",
  [("npc", "Drie boorbeitels. Met Mika-pootafdrukjes erop. En je hebt de grote toren gezien?"),
   ("jij", "Ze boren het grauwe kristal uit de bergen. Zakken vol."),
   ("npc", "Dus dat is het gebrom. Weken al. Maar er is nog iets, avonturier. Iets wat ik niemand durfde te vertellen.")],
  xp=9000, munten=6000, loot="episch", lv=26, na=["z2_hq07_onder_het_ijs"], inleveren="z2_touwtje",
  praat={"z2_touwtje": [
      ("npc", "Hoi! Niet schrikken als de brug wiebelt. Dat doet hij altijd. Ik ben Touwtje."),
      ("npc", "Mika's? O ja. Overal. Met helmpjes en boren en lunchtrommeltjes. Als je ze verslaat, verdwijnen ze in een rookwolk. "
              "Dan rennen ze terug naar hun fort, ergens ver weg."),
      ("npc", "Hun grootste boortoren staat in het oosten van de kloof. Je hoort hem van een kilometer afstand.")]})

Q("z2_hq09_draak", "hoofd", "z2_touwtje", "Iets groots in de diepte",
  "Kloofwachter Touwtje zag iets groots neerstorten op het bergzadel ten noorden van het Bevroren Knuffelmeer. Iets dat ademt. "
  "Ga voorzichtig kijken wat het is.",
  [D("gebied", "poi:z2_drakenravijn", 1, "Ga naar het bergzadel ten noorden van het Bevroren Knuffelmeer, aan de zuidwestrand van de Splinterkloof",
     plek="poi:z2_drakenravijn"),
   D("meld", "z2:draak_geholpen", 1, "Help wat daar op het bergzadel ligt", plek="poi:z2_drakenravijn")],
  [("npc", "Drie nachten geleden viel er iets op het bergzadel in het westen, boven het meer. Iets groots. Het bromde niet. Het... zuchtte."),
   ("npc", "Ik durf niet te kijken. Jij wel? Ga voorzichtig. En niet meppen, alsjeblieft. Eerst kijken.")],
  "Het bergzadel in het westen, boven het meer. Langzaam, en eerst kijken!",
  [("npc", "Was het... was het echt de draak? Vahoegnir? En jij hebt hem GEHOLPEN?"),
   ("jij", "Er zat een grauwe kristalsplinter in zijn vleugel. Ik heb hem eruit getrokken. Hij keek me aan. Heel lang. En toen vloog "
           "hij weg."),
   ("npc", "Ik dacht dat hij de vijand was. De bron van alle grauwheid. Maar hij had pijn... Misschien is het allemaal anders dan "
           "we denken."),
   ("npc", "Die splinter... Lichtwachter Prisma bij de Spiegelburcht weet alles van kristal. Breng hem daarheen. De burcht staat "
           "op de hoogste piek, in het noordoosten.")],
  xp=10000, munten=7000, items=[(item("drakensplinter"), 1)], lv=27, na=["z2_hq08_splinterkloof"])

Q("z2_hq10_spiegelburcht", "hoofd", "z2_touwtje", "De Spiegelburcht",
  "De Spiegelburcht staat op de hoogste piek in het noordoosten, achter de Splinterkloof: een paleis van grauw kristal vol "
  "spiegels. Klim naar de poort en praat met Lichtwachter Prisma.",
  [D("gebied", "poi:z2_spiegelpoort", 1, "Klim naar de poort van de Spiegelburcht, op de hoogste piek in het noordoosten",
     plek="poi:z2_spiegelpoort"),
   D("praat", "z2_prisma", 1, "Praat met Lichtwachter Prisma bij de poort van de Spiegelburcht", plek="npc:z2_prisma")],
  [("npc", "Volg de kloof naar het noorden, over deze middelste hangbrug, en dan de trap omhoog. Heel ver omhoog. Je ziet de burcht glinsteren."),
   ("npc", "Neem een warme sjaal mee. En je moed.")],
  "Over de middelste hangbrug naar het noorden, en dan de trap op naar de Spiegelburcht.",
  [("npc", "Een grauwe drakensplinter... Laat zien. Kijk, als ik hem in het licht houd, zie je een heel klein roze puntje. Diep "
           "vanbinnen."),
   ("npc", "Dit is geen gewoon kristal. Dit komt van een hart. Van zijn hart. Het roze hartkristal van Vahoegnir."),
   ("jij", "Zijn hart?"),
   ("npc", "Zijn hart is in stukken gebroken. Zo begon de Vadsloosheid. En een groot stuk ervan... ligt daarbinnen. Bij Vrouwe "
           "Spiegelzucht.")],
  xp=8000, munten=6000, lv=28, na=["z2_hq09_draak"], inleveren="z2_prisma",
  praat={"z2_prisma": [
      ("npc", "Halt, reiziger. Je staat voor de Spiegelburcht. Ooit het lichtste paleis van het rijk. Nu..."),
      ("npc", "...grauw. Ik ben Prisma. Ik bewaak het laatste beetje licht hier.")]})

Q("z2_hq11_spiegelzucht", "hoofd", "z2_prisma", "Vrouwe Spiegelzucht",
  "In de Spiegelzaal achterin de Spiegelburcht zit Vrouwe Spiegelzucht, grauw geworden door het splinter van het hartkristal. Ga "
  "door de spiegelgangen naar achteren: kaats het licht van spiegel naar spiegel om de deuren te openen. In het gevecht splitst ze zich in "
  "spiegelbeelden: alleen het beeld zonder schaduw kun je raken, en de lichtval draait per fase.",
  [D("opvrolijk", "gebied:z2_spiegelburcht", 10, "Versla 10 spiegelwezens in de gangen van de Spiegelburcht", plek="gebied:z2_spiegelburcht"),
   D("baas", "gebied:z2_spiegelburcht", 1, "Versla Vrouwe Spiegelzucht in de Spiegelzaal achterin de burcht (raak alleen het beeld zonder "
     "schaduw)", plek="baas:z2_spiegelburcht")],
  [("npc", "Vrouwe Spiegelzucht bewonderde vroeger de hele dag zichzelf. Onschuldig. Ijdel, maar onschuldig."),
   ("npc", "Toen het hartkristal brak, viel er een splinter in haar spiegels. Nu ziet ze alleen nog grijs, en ze wil dat iedereen "
           "grijs ziet."),
   ("npc", "Kaats het licht naar elke deur, dan kom je erdoor. En in het gevecht: kijk naar de schaduwen! Het spiegelbeeld zonder "
           "schaduw is de echte.")],
  "Door de spiegelgangen naar achteren naar de Spiegelzaal. Raak alleen het spiegelbeeld zonder schaduw!",
  [("npc", "Je hebt het gedaan. De spiegels zijn weer helder. En kijk... wat je in je pootjes hebt."),
   ("npc", "De splinter van het hartkristal. Roze. Warm. Hij klopt. Kijk er maar eens goed in...")],
  xp=15000, munten=10000, loot="episch", lv=29, na=["z2_hq10_spiegelburcht"], actie_klaar="z2_flashback")

Q("z2_hq12_hartsplinter", "hoofd", "z2_prisma", "Een roze splinter",
  "Je hebt de eerste splinter van het roze hartkristal van Vahoegnir gevonden. Breng hem naar Ordemeester Kristalijn, in het "
  "de Kristalkapel aan de noordkant van Kandijkom.",
  [D("praat", "z2_ordemeester", 1, "Breng de roze hartsplinter naar Ordemeester Kristalijn in de Kristalkapel aan de noordkant van Kandijkom",
     plek="npc:z2_ordemeester")],
  [("npc", "Ga, avonturier. De Orde moet dit weten.")],
  "Naar Ordemeester Kristalijn in de Kristalkapel aan de noordkant van Kandijkom (achter de grote sneeuwguh-spar).",
  [("npc", "Een splinter van het hartkristal... Ik heb er alleen over gelezen. In de oudste boeken van de Orde."),
   ("npc", "Er stond ook een plaatje in van een kat met een kroontje, en grauwe mist die over de hele bladzijde kroop. Die bladzijde is eruit gescheurd. "
           "Niet door mij."),
   ("npc", "Toen Vahoegnir nog roze was, droeg hij een hart van roze kristal. Zolang het heel was, beschermde hij het hele rijk."),
   ("npc", "Iemand heeft het gebroken. De Mika's? Ze boren het grauwe kristal uit de bergen... ze zoeken iets."),
   ("jij", "Dan zoeken wij de andere splinters. Voordat de Mika's ze vinden."),
   ("npc", "Kijk eens naar het oosten. Zie je dat, heel ver weg, achter de wolken? Een boom. Zo groot als een berg.")],
  xp=12000, munten=15000, lv=30, na=["z2_hq11_spiegelzucht"], inleveren="z2_ordemeester", auto=True,
  start_items=[(item("hartsplinter"), 1)], actie_klaar="z2_cliffhanger")

# ===== side quests (~20) ====================================================================================================
_NA = ["z2_hq02_kandijkom"]
Q("z2_s01_wanten", "zij", "z2_opa_sneeuwbal", "Opa's wanten",
  "De IJzelgeesten op de Galmhellingen hebben de wanten van Opa Sneeuwbal gepikt. Versla ze en breng de wanten terug.",
  [D("verzamel", item("wantje"), 4, "Pak 4 wanten af van de IJzelgeesten op de Galmhellingen, ten oosten van Kandijkom",
     "gebied:z2_galmhellingen", 0.4, plek="gebied:z2_galmhellingen")],
  [("npc", "Ik zat te dutten op het caféterras, en toen zweefde er zo'n IJzelgeest langs. Floep! Mijn wanten. Alle vier."),
   ("npc", "Ja, vier. Twee voor de pootjes, twee voor de oortjes. Ik ben niet gek. Haal je ze terug?")],
  "Vier wanten. De IJzelgeesten op de Galmhellingen hebben ze. Naar het oosten!",
  [("npc", "Mijn wanten! Een beetje bevroren, maar dat geeft niks. Hier, een warm koekje. Ik heb het in mijn want bewaard. Nou ja, "
           "in jouw want nu.")],
  xp=3500, munten=2000, items=[("minecraft:cookie", 3)], lv=20, na=_NA)

Q("z2_s02_kandijkoekjes", "zij", "z2_bakker", "Kandijkoekjes",
  "Bakker Speculaas heeft kandijkristallen nodig voor zijn beroemde kandijkoekjes. De wezens rond het Bevroren Knuffelmeer laten "
  "ze vallen.",
  [D("verzamel", item("kandijkristal"), 6, "Verzamel 6 kandijkristallen van de wezens bij het Bevroren Knuffelmeer, ten westen van "
     "Kandijkom", "gebied:z2_knuffelmeer", 0.45, plek="gebied:z2_knuffelmeer")],
  [("npc", "Ramp! Mijn kandijpot is leeg! Zonder kandij geen kandijkoekjes, en zonder kandijkoekjes geen Kandijkom!"),
   ("npc", "Bij het meer in het westen lopen wezens met kandijkristal in hun buik. Zes stuks, alsjeblieft.")],
  "Zes kandijkristallen van de wezens bij het meer in het westen.",
  [("npc", "Kandij! Kijk hoe ze glimmen. Hier, de eerste koekjes van de nieuwe bakplaat. Pas op, de kristallen knappen in je mond.")],
  xp=5000, munten=3000, items=[("minecraft:cookie", 6)], lv=25, na=_NA)

Q("z2_s03_knuffels", "zij", "z2_vlokje", "Tien knuffels onder het ijs",
  "De knuffels van Kleine Vlokje (gebreid door Knuffelmaker Pluk) zijn onder het ijs gegleden. Ze zitten vast onder het ijs van het "
  "Bevroren Knuffelmeer. Ze glinsteren met hartjes: loop er dichtbij, dan breek je ze voorzichtig los.",
  [D("geheim", "teller:z2_knuffels", 10, "Bevrijd de 10 knuffels onder het ijs (op het Bevroren Knuffelmeer, ten westen van "
     "Kandijkom)", plek="gebied:z2_knuffelmeer")],
  [("npc", "Mijn knuffels gingen schaatsen op het meer. Zonder mij! En toen... krak. Nu zitten ze onder het ijs."),
   ("npc", "Ze liggen overal op het meer, onder het ijs. Ze glinsteren met hartjes. Bevrijd je ze? Pleeeease?")],
  "Ze glinsteren met hartjes onder het ijs van het meer in het westen!",
  [("npc", "ALLEMAAL! Sneeuwbal, Ijsbeertje, Kapitein Kwartsje, Wantje... iedereen is er weer!"),
   ("npc", "Hier, deze vond ik in de sneeuw. Hij glimt. Jij mag hem hebben.")],
  xp=9000, munten=6000, loot="zeldzaam", lv=20, na=_NA)

Q("z2_s04_schaatsen", "zij", "z2_schaatsjuf", "Schaatsles",
  "Schaatsjuf Zwierig geeft iedere nieuwe avonturier schaatsles. Ga op het ijs van de schaatsvijver staan en kom terug zonder te "
  "vallen. Nou ja. Zonder veel te vallen.",
  [D("gebied", "poi:z2_schaatsvijver", 1, "Ga op het ijs van de schaatsvijver staan, midden op het plein van Kandijkom",
     plek="poi:z2_schaatsvijver")],
  [("npc", "Nieuw in Kandijkom? Dan moet je schaatsen! Iedereen schaatst hier. Zelfs de burgemeester. Vooral op zijn buik."),
   ("npc", "Stap maar op het ijs. Pootjes van elkaar. Armpjes wijd. En... ZWIER!")],
  "Op het ijs van de vijver, midden op het plein. Pootjes van elkaar!",
  [("npc", "Prachtig! Je viel maar drie keer. Dat is een record voor een avonturier. Hier, een diploma. Het is een koekje.")],
  xp=2500, munten=1500, items=[("minecraft:cookie", 1)], lv=20, na=_NA)

Q("z2_s05_echokoor", "zij", "z2_galmpje", "Het echokoor",
  "Koorzanger Galmpje wil weer met de kristalnaalden zingen. Loop langs de drie zingende naalden op de Galmhellingen (do, re, mi): "
  "als je er dichtbij staat, zingen ze hun toon.",
  [D("gebied", "poi:z2_naald_do", 1, "Zing bij de lage naald (do), in het zuiden van de Galmhellingen",
     plek="poi:z2_naald_do"),
   D("gebied", "poi:z2_naald_re", 1, "Zing bij de middelste naald (re), midden op de Galmhellingen", plek="poi:z2_naald_re"),
   D("gebied", "poi:z2_naald_mi", 1, "Zing bij de hoge naald (mi), in het noorden van de Galmhellingen", plek="poi:z2_naald_mi")],
  [("npc", "Do, re, miiii... Hoor je dat? Niks. Vroeger zongen de naalden mee. Nu zijn ze te verlegen."),
   ("npc", "Wil jij bij ze langsgaan? De lage, de middelste en de hoge. Op de Galmhellingen. Zing maar een beetje. Of neurie. Of "
           "doe gewoon 'laaa'.")],
  "Do, re, mi: drie naalden op de Galmhellingen, van laag naar hoog.",
  [("npc", "Ik hoorde ze! Helemaal tot hier! Do... re... MIIII! Het Kandijkoor is weer compleet. Met de echo erbij zijn we met z'n "
           "drieën!")],
  xp=5000, munten=3000, lv=21, na=["z2_hq03_galmhellingen"])

Q("z2_s06_post", "zij", "z2_postguh", "Post voor de bergen",
  "Postguh Sleetje heeft drie brieven voor de bergen, maar geen zin om te klimmen. Breng ze naar Berggids Klimpoot, IJsvisser Wak en "
  "Kloofwachter Touwtje.",
  [D("praat", "z2_berggids", 1, "Breng een brief naar Berggids Klimpoot, bij het kampvuur aan de westrand van de Galmhellingen",
     plek="npc:z2_berggids"),
   D("praat", "z2_visser", 1, "Breng een brief naar IJsvisser Wak, midden op het Bevroren Knuffelmeer", plek="npc:z2_visser"),
   D("praat", "z2_touwtje", 1, "Breng een brief naar Kloofwachter Touwtje, bij de middelste hangbrug van de Splinterkloof",
     plek="npc:z2_touwtje")],
  [("npc", "Drie brieven. Eén voor boven op de helling, één op het ijs, één bij de kloof. Ik ben postguh, geen berggeit."),
   ("npc", "Jij loopt toch die kant op? Ja? Fijn. Hier. Niet openmaken. Behalve als het heel spannend is.")],
  "Klimpoot op de Galmhellingen, Wak op het meer, Touwtje bij de kloof.",
  [("npc", "Alle drie bezorgd? Je bent een postheld. Hier, je loon. En een postzegel met een guhgezichtje.")],
  xp=6000, munten=4000, lv=28, na=["z2_hq07_onder_het_ijs"],
  praat={"z2_berggids": [("npc", "Een brief? Van mijn moeder! 'Lieve Klimpoot, draag je wel je muts?' Ja mam. Bijna altijd.")],
         "z2_visser": [("npc", "Post! Op het ijs! Een folder van de visboer. Eh. Fijn. Bedankt.")],
         "z2_touwtje": [("npc", "Een brief voor mij? 'Geachte heer Touwtje, uw nieuwe touw is besteld.' EINDELIJK!")]})

Q("z2_s07_sneeuwpop", "zij", "z2_sneeuwpop", "De grootste sneeuwpop",
  "Sneeuwpop-Sjaak bouwt de grootste sneeuwpop van het Vadsrijk, maar de sneeuw in Kandijkom is te zacht. Haal extra stevige "
  "sneeuwballen van de wezens op de Galmhellingen en zoek de zachte sneeuwhelling waar hij wil bouwen.",
  [D("verzamel", item("sneeuwbal"), 8, "Verzamel 8 stevige sneeuwballen van de wezens op de Galmhellingen", "gebied:z2_galmhellingen", 0.5,
     plek="gebied:z2_galmhellingen"),
   D("gebied", "poi:z2_sneeuwpophelling", 1, "Zoek de zachte sneeuwhelling in het zuiden van de Galmhellingen",
     plek="poi:z2_sneeuwpophelling")],
  [("npc", "De sneeuw hier in de geode is te warm. Hij plakt niet. Een sneeuwpop van smeltsneeuw is een plasje met een wortel."),
   ("npc", "Op de Galmhellingen hebben de wezens van die harde, knisperende sneeuwballen. Acht stuks! En kijk even of die zachte helling "
           "in het zuiden nog vrij is. Daar wil ik bouwen.")],
  "Acht stevige sneeuwballen, en een kijkje op de zachte helling in het zuiden van de Galmhellingen.",
  [("npc", "Perfect! Ik bouw hem zo groot dat hij vanaf het luchtschip te zien is. Ik noem hem Sjaak. Net als ik. En als mijn opa.")],
  xp=5000, munten=3000, items=[("minecraft:carrot", 2)], lv=21, na=["z2_hq03_galmhellingen"])

Q("z2_s08_kas", "zij", "z2_kasguh", "Een warme kas",
  "De bloemetjes in de kas van Kasguh Groentje worden grijs. Gloeikristallen uit de Splinterkloof houden de kas warm en roze.",
  [D("verzamel", item("gloeikristal"), 5, "Verzamel 5 gloeikristallen van de wezens in de Splinterkloof, ten noorden van Kandijkom",
     "gebied:z2_splinterkloof", 0.4, plek="gebied:z2_splinterkloof")],
  [("npc", "Mijn kas is mijn trots. Tomaatjes, worteltjes, bloemetjes. Maar de bloemetjes worden grijs. Grijs!"),
   ("npc", "Gloeikristallen geven roze warmte. In de Splinterkloof hebben de wezens ze. Vijf, en mijn bloemetjes lachen weer.")],
  "Vijf gloeikristallen uit de Splinterkloof in het noorden.",
  [("npc", "Kijk! Kijk dan! Ze worden al roze! Hier, een tomaatje. Het is een beetje kristal, maar heel gezond.")],
  xp=7000, munten=4500, items=[("minecraft:apple", 3)], lv=30, na=["z2_hq08_splinterkloof"])

Q("z2_s09_winterslaap", "zij", "z2_winterslaap", "Te koud om te slapen",
  "Winterslaap Wim kan niet slapen: het is te koud. Hij heeft gehoord van een warmwaterbron op de Galmhellingen. Zoek hem op en "
  "vertel Wim hoe warm hij is.",
  [D("gebied", "poi:z2_warmwaterbron", 1, "Zoek de warmwaterbron in het zuidoosten van de Galmhellingen (volg de stoom)",
     plek="poi:z2_warmwaterbron")],
  [("npc", "Zzz... Ik wil een winterslaap houden. Maar mijn pootjes zijn ijsjes. Zo kan een guh toch niet slapen?"),
   ("npc", "Men zegt dat er een warmwaterbron is, op de Galmhellingen. Zoek jij hem? Ik zou het zelf doen, maar... zzz.")],
  "Een warmwaterbron in het zuidoosten van de Galmhellingen. De stoom wijst de weg.",
  [("npc", "Warm als een bad? Met bubbeltjes? Morgen ga ik erheen. Of overmorgen. Na mijn dutje. Hier, een warme kruik voor jou.")],
  xp=4500, munten=2500, lv=22, na=["z2_hq03_galmhellingen"])

Q("z2_s10_poollicht", "zij", "z2_poollicht", "Het poollicht",
  "Poollicht Pien tekent sterrenkaarten. Ze wil weten wat je ziet vanaf de hoge kristalnaald in het noordwesten van de Splinterkloof.",
  [D("gebied", "poi:z2_uitkijk_poollicht", 1, "Klim op de hoge kristalnaald in het noordwesten van de Splinterkloof",
     plek="poi:z2_uitkijk_poollicht")],
  [("npc", "Vanaf de hoogste naald in de kloof zie je het hele rijk. Het poollicht. De sterren met oortjes. En iets... groots, in "
           "het oosten."),
   ("npc", "Wil jij voor mij kijken? Mijn pootjes zijn te kort om te klimmen.")],
  "De hoge kristalnaald in het noordwesten van de Splinterkloof. Kijk goed naar het oosten!",
  [("npc", "Een boom? Zo groot als een berg? Met lichtjes in de takken? Dat... staat op geen enkele kaart. Ik ga hem tekenen!")],
  xp=6000, munten=3500, lv=30, na=["z2_hq08_splinterkloof"])

Q("z2_s11_tandwielen", "zij", "z2_uitvinder", "Tandwieltjes",
  "Uitvinder Tandwiel wil weten wat voor machines de Mika's gebruiken. Pak tandwieltjes af van de Mika-mijnwerkers in de "
  "Splinterkloof.",
  [D("verzamel", item("tandwiel"), 5, "Pak 5 Mika-tandwieltjes af van de Mika-mijnwerkers in de Splinterkloof", "gebied:z2_splinterkloof",
     0.4, plek="gebied:z2_splinterkloof")],
  [("npc", "Dat gebrom in de bergen! Dat is een machine, ik weet het zeker. Een grote. Met heel veel tandwielen."),
   ("npc", "Breng me wat tandwieltjes van die Mika-mijnwerkers. Vijf. Dan kan ik uitrekenen hoe groot hun machine is.")],
  "Vijf tandwieltjes van de Mika-mijnwerkers in de Splinterkloof.",
  [("npc", "Hmmm. Hmmmmm. Volgens mijn berekening is hun machine... heel groot. Met drie koppen. Die heel heet worden."),
   ("npc", "Als je ooit sporen van die machine vindt, breng ze dan bij mij! Ik wil weten waar hij staat.")],
  xp=7000, munten=4500, loot="zeldzaam", lv=29, na=["z2_hq08_splinterkloof"])

Q("z2_s12_kandijfeest", "zij", "z2_burgemeester", "Het Kandijfeest",
  "Burgemeester Kandij wil een feest om de Galmwolf te vieren. Nodig Cafébaas Mallow, Bakker Speculaas en Koorzanger Galmpje uit.",
  [D("praat", "z2_cafe", 1, "Nodig Cafébaas Mallow uit, in het chocomelkcafé aan de zuidoostkant van het plein", plek="npc:z2_cafe"),
   D("praat", "z2_bakker", 1, "Nodig Bakker Speculaas uit, bij zijn bakkerij aan de oostrand van Kandijkom", plek="npc:z2_bakker"),
   D("praat", "z2_galmpje", 1, "Nodig Koorzanger Galmpje uit, aan de noordwestkant van het plein", plek="npc:z2_galmpje")],
  [("npc", "De Galmwolf is verslagen! Dat moet gevierd worden! Met chocomelk, koekjes en zang!"),
   ("npc", "Wil jij Mallow, Speculaas en Galmpje uitnodigen? Ze staan allemaal op het plein. Ik moet mijn ambtsketting poetsen.")],
  "Mallow, Speculaas en Galmpje. Allemaal op het plein.",
  [("npc", "Iedereen komt! Het Kandijfeest wordt het warmste feest in de koudste bergen. Jij bent eregast, natuurlijk.")],
  xp=4000, munten=3000, items=[("minecraft:cake", 1)], lv=23, na=["z2_hq05_galmwolf"],
  praat={"z2_cafe": [("npc", "Een feest? Ik neem de grootste pan chocomelk mee. De HELE pan.")],
         "z2_bakker": [("npc", "Feest! Ik bak een kandijtaart in de vorm van een Galmwolf. Een lieve Galmwolf.")],
         "z2_galmpje": [("npc", "Zingen op een feest? La la LAAA! Ik oefen al!")]})

Q("z2_s13_ijsbloemen", "zij", "z2_cafe", "IJsbloemen voor de chocomelk",
  "Cafébaas Mallow strooit altijd gemalen ijsbloem over de slagroom. Op het Bevroren Knuffelmeer laten de wezens ze vallen.",
  [D("verzamel", item("ijsbloem"), 5, "Verzamel 5 ijsbloemen van de wezens bij het Bevroren Knuffelmeer, ten westen van Kandijkom",
     "gebied:z2_knuffelmeer", 0.45, plek="gebied:z2_knuffelmeer")],
  [("npc", "Mijn geheime ingrediënt is op! Gemalen ijsbloem. Knispert op de slagroom. Heerlijk."),
   ("npc", "Bij het meer hebben de wezens ze soms in hun vacht. Vijf, alsjeblieft!")],
  "Vijf ijsbloemen van de wezens bij het meer in het westen.",
  [("npc", "Ruik eens. Dat is de geur van de beste chocomelk van het rijk. Hier, een beker voor onderweg. En nog een.")],
  xp=5000, munten=3000, items=[(item("chocomelk"), 3)], lv=23, na=_NA)

for _i, (_g, _naam, _lv, _na) in enumerate([("z2_galmhellingen", "de Galmhellingen", 21, "z2_hq03_galmhellingen"),
                                             ("z2_knuffelmeer", "het Bevroren Knuffelmeer", 26, "z2_hq06_versteend"),
                                             ("z2_splinterkloof", "de Splinterkloof", 30, "z2_hq08_splinterkloof")], 14):
    Q(f"z2_s{_i}_premie_{_g[3:]}", "zij", "z2_kwarts", f"Premie: {_naam}",
      f"De Kristalwacht betaalt een premie aan wie de wezens van {_naam} terugdringt.",
      [D("opvrolijk", "gebied:" + _g, 25, f"Versla 25 wezens {PLEK_GEBIED[_g]}", plek="gebied:" + _g)],
      [("npc", f"De Kristalwacht heeft een premie uitgeschreven. Vijfentwintig wezens {PLEK_GEBIED[_g]}. Daarna betaal ik je uit."),
       ("npc", "Netjes werken, avonturier. En terugkomen. Dat vooral.")],
      f"Vijfentwintig wezens {PLEK_GEBIED[_g]}.",
      [("npc", "Vijfentwintig! Hier is je premie. De Kristalwacht groet je.")],
      xp=5000 + 1500 * (_i - 14), munten=4000 + 1500 * (_i - 14), loot="zeldzaam", lv=_lv, na=[_na])

Q("z2_s17_model", "zij", "z2_beeldhouwer", "Een woest model",
  "IJsbeeldhouwer Beitel wil een ijsbeeld maken van een écht woest monster. Versla drie elites bij het Bevroren Knuffelmeer, dan kan "
  "hij goed kijken.",
  [D("elite", "gebied:z2_knuffelmeer", 3, "Versla 3 elites bij het Bevroren Knuffelmeer (grote wezens met een gloeiende naam)",
     plek="gebied:z2_knuffelmeer")],
  [("npc", "Ik maak altijd guhtjes. Lieve guhtjes. Maar ik wil eens iets woests maken. Iets met tanden!"),
   ("npc", "Versla drie van die grote wezens rond het meer. Dan kijk ik heel goed hoe ze eruitzien. Van een veilige afstand.")],
  "Drie elites rond het meer. Ik kijk mee. Van achter mijn beeld.",
  [("npc", "Ik heb het! Kijk: woest, met tanden, met klauwen... en een strikje. Het moest toch een beetje lief blijven.")],
  xp=7000, munten=4500, loot="zeldzaam", lv=27, na=["z2_hq07_onder_het_ijs"])

Q("z2_s18_lawine", "zij", "z2_berggids", "Lawinegevaar",
  "Op het grote lawinepad in het noorden van de Galmhellingen maken twee elites de sneeuw los. Versla ze voor er een lawine komt.",
  [D("gebied", "poi:z2_lawinepad", 1, "Ga naar de voet van het grote lawinepad, in het noorden van de Galmhellingen", plek="poi:z2_lawinepad"),
   D("elite", "gebied:z2_galmhellingen", 2, "Versla 2 elites op de Galmhellingen", plek="poi:z2_lawinepad")],
  [("npc", "Hoor je dat gerommel? Op het lawinepad springen twee grote wezens rond. Nog even en de hele helling komt naar beneden."),
   ("npc", "Het pad ligt in het noorden. Versla ze, en loop niet midden op het pad. Altijd aan de kant!")],
  "Het lawinepad in het noorden. Twee elites. Aan de kant blijven!",
  [("npc", "Stil. Doodstil. Geen lawine. Jij hebt net een heel dorp onder de sneeuw vandaan gehouden. Nou ja, mij.")],
  xp=6000, munten=4000, loot="zeldzaam", lv=24, na=["z2_hq04_naalden"])

Q("z2_s19_ijsvis", "zij", "z2_visser", "De zilveren ijsvis",
  "IJsvisser Wak vangt al jaren niks. De wezens bij het meer hebben zilveren ijsvissen opgeslokt. Breng er drie, dan heeft hij "
  "eindelijk een visverhaal.",
  [D("verzamel", item("ijsvis"), 3, "Verzamel 3 zilveren ijsvissen van de wezens bij het Bevroren Knuffelmeer", "gebied:z2_knuffelmeer",
     0.3, plek="gebied:z2_knuffelmeer")],
  [("npc", "Twintig jaar vis ik hier. Nul vissen. Maar die wezens daar? Die vangen ze zomaar. Hap!"),
   ("npc", "Breng me drie zilveren ijsvissen. Dan vertel ik iedereen dat ik ze zelf gevangen heb. Grapje. Een beetje.")],
  "Drie zilveren ijsvissen, van de wezens rond het meer.",
  [("npc", "DRIE! Zilver! Ze glimmen! Ik ga ze niet opeten. Ik ga ze inlijsten. Hier, een beloning voor de beste visser van het meer. "
           "Na mij.")],
  xp=6000, munten=4000, lv=25, na=["z2_hq06_versteend"])

Q("z2_s20_bruggen", "zij", "z2_touwtje", "Wiebelbruggen",
  "Kloofwachter Touwtje moet zijn drie hangbruggen controleren, maar hij heeft hoogtevrees. Loop over alle drie.",
  [D("gebied", "poi:z2_brug_1", 1, "Loop over de middelste hangbrug, bij Kloofwachter Touwtje", plek="poi:z2_brug_1"),
   D("gebied", "poi:z2_brug_2", 1, "Loop over de westelijke hangbrug van de Splinterkloof", plek="poi:z2_brug_2"),
   D("gebied", "poi:z2_brug_3", 1, "Loop over de verste hangbrug, helemaal in het westen van de Splinterkloof", plek="poi:z2_brug_3")],
  [("npc", "Psst. Niemand mag het weten. Maar ik heb hoogtevrees. Als kloofwachter. Het is een beetje onhandig."),
   ("npc", "Wil jij over mijn drie bruggen lopen en kijken of ze nog heel zijn? Deze hier, de westelijke en de verste.")],
  "Drie bruggen: deze hier, de westelijke en de verste in het westen.",
  [("npc", "Allemaal heel? Ze wiebelden? Dat hoort zo. Pfoe. Dank je wel. Ik ga even zitten. Op de grond. Heel laag.")],
  xp=6500, munten=4000, lv=29, na=["z2_hq08_splinterkloof"])

Q("z2_s21_truien", "zij", "z2_breister", "Truien voor de Kristalwacht",
  "Breister Wolwinter breit warme truien voor de Kristalwacht, maar haar wol is op. De wezens op de Galmhellingen hebben bergwol "
  "in hun vacht.",
  [D("verzamel", item("wolpluk"), 6, "Verzamel 6 plukjes bergwol van de wezens op de Galmhellingen", "gebied:z2_galmhellingen", 0.45,
     plek="gebied:z2_galmhellingen"),
   D("praat", "z2_kwarts", 1, "Breng de eerste trui naar Kapitein Kwarts, bij het questbord op het plein van Kandijkom", plek="npc:z2_kwarts")],
  [("npc", "De Kristalwacht staat de hele dag in de kou. Ze verdienen een trui. Een dikke. Met een kristalpatroon."),
   ("npc", "Zes plukjes bergwol van de Galmhellingen. Dan brei ik er een. En jij brengt hem naar Kapitein Kwarts.")],
  "Zes plukjes bergwol, en dan de trui naar Kapitein Kwarts bij het questbord op het plein.",
  [("npc", "Hij had hem meteen aan? Over zijn harnas heen? Haha! Dan maak ik er nog elf. Hier, voor jou ook iets warms.")],
  xp=5000, munten=3500, lv=21, na=["z2_hq03_galmhellingen"],
  praat={"z2_kwarts": [("npc", "Een trui? Voor mij? ...Hij is roze. Met kristallen. Ik ga hem nooit meer uitdoen.")]})

Q("z2_s22_edelsteen", "zij", "z2_juwelier", "Je eerste edelsteen",
  "Juweliersguh Glinster laat je zien hoe sokjes werken: sommige spullen hebben 0, 1 of 2 sokjes. Ruwe kristallen uit de bergen "
  "zet je er bij haar zo in, en je kunt ze er ook weer uithalen. Er gaat nooit iets kapot.",
  [D("meld", "edelsteen:ingezet", 1, "Zet een kristal in een sokje bij Juweliersguh Glinster, aan de noordwestkant van het plein "
     "van Kandijkom", plek="npc:z2_juwelier")],
  [("npc", "Kijk eens naar je spullen. Zie je die kleine rondjes? Dat zijn sokjes. Niet alle spullen hebben ze. Sommige een, sommige twee."),
   ("npc", "Ruwe kristallen uit de bergen gaan er zo in. Klik, klaar. Geen geslijp, geen gedoe. Halen we hem eruit? Ook klik, klaar."),
   ("npc", "Kom maar bij me met een kristal en een spulletje met een sokje. Dan doen we het samen.")],
  "Klik op mij met een kristal en een spulletje met een sokje.",
  [("npc", "Kijk hoe hij glanst! Dat is jouw eerste edelsteen. Ze gaan nooit kapot, en je kunt ze altijd wisselen. Veel plezier ermee!")],
  xp=3000, munten=2000, lv=20, na=_NA)

# fix round 2 (g-werver): the mount return quest of zone 2, available once you own any mount (vadsrijk_npc.RIJDIER_POORT)
Q("z2_rijdier_guhwolf", "rijdier", "z2_breister", "De verdwaalde Guhwolf",
  "Breister Wolwinter hoort 's nachts iets huilen in de Splinterkloof: een sneeuwwolfje, helemaal alleen. Zoek het op de bodem van "
  "de kloof, vrolijk de knorrige wezens eromheen op en breng het veilig naar Kandijkom.",
  [D("gebied", "poi:z2_wolfje", 1, "Zoek het huilende wolfje op de bodem van de Splinterkloof, onderaan de tweede trap naar beneden",
     plek="poi:z2_wolfje"),
   D("opvrolijk", "gebied:z2_splinterkloof", 5, "Vrolijk 5 knorrige wezens in de Splinterkloof op, zodat het wolfje veilig mee kan",
     plek="gebied:z2_splinterkloof")],
  [("npc", "Psst. Jij hebt een rijdier, hè? Dan snap jij dit vast. Hoor je dat? 's Nachts huilt er iets in de Splinterkloof."),
   ("npc", "Een sneeuwwolfje! Zo klein als een kussentje, helemaal alleen op de bodem van de kloof. Ik kan niet klimmen, ik brei."),
   ("jij", "Ik ga het zoeken!"),
   ("npc", "Fijn! Neem het mee hierheen. Ik brei vast een sjaaltje. Een heel klein sjaaltje. Voor nu.")],
  "Het wolfje zit op de bodem van de Splinterkloof, onderaan de tweede trap. Breng het veilig hier!",
  [("npc", "Daar is hij! Kom maar, kleintje. Hier, je sjaaltje. Staat je prachtig."),
   ("verteller", "Een paar dagen, heel veel chocomelk en drie sjaaltjes later..."),
   ("npc", "Kijk eens hoe groot hij is geworden! Hij past niet meer in je capuchon. Maar hij wil wel met je mee. Op zijn rug, zelfs!"),
   ("npc", "Hier, zijn sjaaltje-zadel. Pas goed op elkaar, jullie twee. En kom af en toe langs voor een nieuwe sjaal.")],
  xp=6000, munten=3000, lv=22, na=["z2_hq02_kandijkom", "@rijdier"], actie_klaar="rijdier_geef:guhwolf")

Q("z2_s23_gondel", "zij", "z2_gondelier", "Naar de top!",
  "Gondelguh Kabeltje vindt dat iedereen een keer met de gondel naar de top moet. Neem de gondel en zeg Gondelguh Katrol boven gedag.",
  [D("gebied", "poi:z2_gondel_top", 1, "Neem de gondel bij Gondelguh Kabeltje (noordoostkant van Kandijkom) naar het bovenste station",
     plek="npc:z2_gondelier"),
   D("praat", "z2_gondel_boven", 1, "Zeg Gondelguh Katrol gedag, bij het bovenste gondelstation, hoog in het noorden van de Galmhellingen",
     plek="npc:z2_gondel_boven")],
  [("npc", "Ben je al boven geweest? Nee? Dan heb je de bergen nog niet gezien! Stap in."),
   ("npc", "Zeg mijn broer Katrol gedag. Hij zit boven. Hij hoort mij nooit via de kabel.")],
  "Stap in de gondel bij mij, en zeg Katrol boven gedag.",
  [("npc", "Katrol zegt gedag terug? Via jou? Eindelijk een gesprek! Dank je, avonturier.")],
  xp=3000, munten=2000, lv=20, na=_NA, inleveren="z2_gondelier",
  praat={"z2_gondel_boven": [("npc", "Een groet van Kabeltje? Zeg maar: 'Hoi Kabeltje! Hoor je me nu wel?' Haha. Hij hoort nooit wat.")]})

# fix round 2 (g-z2): zone 2 keeps the main line + only these short side quests (the other 18 definitions stay above for
# reference/reuse but are not written: no quests_z2.json entry, no texts, no advancement, no FTB quest).
ZIJ_HOUDEN = ("z2_s03_knuffels", "z2_s04_schaatsen", "z2_s05_echokoor", "z2_s13_ijsbloemen", "z2_s23_gondel")
QUESTS[:] = [q for q in QUESTS if q["soort"] != "zij" or q["id"] in ZIJ_HOUDEN]

# ===== job advancement at lv 30 per class (klassen + z2-systemen do the ceremony) ===========================================
KLASSEN = {
    "knuffelridder": ("Knuffelridder", "Kristalkampioen"),
    "pluisjager": ("Pluisjager", "Poolstervalk"),
    "vadsmagier": ("Vadsmagiër", "Kristalorakel"),
}
for k, (naam, t3) in KLASSEN.items():
    Q(f"z2_adv30_{k}", "klasse", "z2_ordemeester", f"De proef van de {t3}",
      f"De derde titel van De Knuffelorde: {t3}. Bereik level 30 als {naam} en bewijs je in de Splinterkloof.",
      [D("level", "*", 30, f"Bereik level 30 als {naam} (vecht op de Galmhellingen, bij het meer en in de kloof)",
         plek="gebied:z2_splinterkloof"),
       D("opvrolijk", "gebied:z2_splinterkloof", 20, "Versla 20 wezens in de Splinterkloof, ten noorden van Kandijkom",
         plek="gebied:z2_splinterkloof")],
      [("npc", f"Je draagt al twee titels van de Orde. Maar in de bergen wacht een derde: {t3}."),
       ("npc", "Bereik level 30 en versla twintig wezens in de Splinterkloof. Dan houd ik hier, in de kleinste kapel van het "
               "rijk, de grootste ceremonie.")],
      f"Level 30 als {naam}, en twintig wezens in de Splinterkloof.",
      [("npc", f"De kristallen zingen, de chocomelk dampt. Ik benoem je tot {t3}! Grootmeester Vadsvoet zal trots zijn.")],
      xp=10000, munten=8000, lv=28, klasse=k, na=["z2_hq02_kandijkom"], actie_klaar=f"advancement:{k}:3")

# ===== the secret: the Mika-boormeester (bazen owns the boss: Boormeester.ontgrendel(p)) ===================================
Q("z2_geheim_boormeester", "geheim", "z2_uitvinder", "Het gebrom in de berg",
  "Overal in de Splinterpieken liggen sporen van een enorme Mika-machine. Vind ze alle vijf: dan weet je waar hij staat, en hoe je "
  "zijn drie boorkoppen moet laten oververhitten.",
  [D("geheim", "teller:z2_aanwijzingen", 5, "Vind de vijf boorsporen (paarse glinsters: in de Splinterkloof, op de Galmhellingen en in "
     "Kandijkom)", plek="poi:z2_boortoren"),
   D("meld", "z2_boormeester:verslagen", 1, "Versla de Mika-boormeester in de verborgen boorhal op de bodem van de Splinterkloof, helemaal in het westen",
     plek="poi:z2_boorhal")],
  [("npc", "...")],
  "Vijf boorsporen, en dan de verborgen boorhal op de bodem van de kloof, helemaal in het westen.",
  [("npc", "De boormeester! Verslagen! In een rookwolk naar huis gevlucht, zeg je? Met zijn helmpje scheef?"),
   ("npc", "Dus DAT was het gebrom. Een boormachine met drie koppen, diep in de berg. Ze boorden naar het grauwe kristal. Naar "
           "de splinters van het hart, denk ik."),
   ("npc", "Je bent de slimste speurneus van de bergen. Hier, een tandwiel van zijn machine. Ik heb het verguld. Met kandij.")],
  xp=15000, munten=12000, loot="episch", lv=28, auto=True)

# ---------------------------------------------------------------------------------------------------------------------------
# Secrets: 10 knuffels under the ice, 5 clues to the Mika-boormeester, 5 treasures (Z2NpcPlekken.KNUFFEL/AANWIJZING/SCHAT)
# ---------------------------------------------------------------------------------------------------------------------------
KNUFFELS = {   # (Z2NpcPlekken.KNUFFEL: ijs_1..10 = POI:knuffel_ijs_1..10)
    "ijs_1": "Sneeuwbal", "ijs_2": "IJsbeertje", "ijs_3": "Kapitein Kwartsje", "ijs_4": "Wantje", "ijs_5": "Pinguïntje",
    "ijs_6": "Visje Vlok", "ijs_7": "Mutsje", "ijs_8": "Snorretje", "ijs_9": "Pegeltje", "ijs_10": "Brokje Junior",
}
AANWIJZINGEN = {
    "olievlek": ("Een olievlek met kattenpootjes", "Bij de grote boortoren ligt een plas zwarte olie. Er lopen pootafdrukjes doorheen. "
                                                    "Vier teentjes en een kussentje. En een bonnetje: 'Boorkop 2: NIET te heet maken!'"),
    "bouwtekening": ("Een gescheurde bouwtekening", "Een tekening van een machine met drie boorkoppen: links, midden, rechts. Eronder "
                                                     "staat gekrabbeld: 'Ze worden heet in de volgorde waarin ze gaan gloeien. Let op het "
                                                     "lampje!'"),
    "boorgaten": ("Boorgaten in de sneeuw", "Op het lawinepad zitten ronde gaten in de sneeuw, netjes op een rij. Iets heeft hier "
                                              "proefgeboord. Ze lopen naar het noorden, naar de kloof."),
    "boorschema": ("Een boorschema in de prullenbak", "Achter de werkplaats van Uitvinder Tandwiel ligt een kaasbroodje met een briefje: "
                                                       "'Boorhal west: geheime spleet. Wachtwoord: miauw. NIET aan de guhs vertellen.'"),
    "warme_spleet": ("Een warme spleet", "Op de bodem van de Splinterkloof, helemaal in het westen, blaast warme lucht uit een spleet. Je hoort gebrom. En "
                                         "heel zacht... een kaastoeter. Hier is iets verborgen."),
}
SCHATTEN = {
    "ijsgrot": ("De blauwe ijsgrot", "Diep in een grot van blauw ijs ligt een vergeten kist van de Kristalwacht!"),
    "galmgrot": ("De Galmgrot", "In een grot vol zingende kristallen ligt een vergeten kist. Hij zoemt een liedje."),
    "chocokelder": ("De chocokelder", "Onder het chocomelkcafé, in een holte van roze kwarts. Hier bewaart Mallow zijn geheimste cacao."),
    "burchtdak": ("Op het dak van de Spiegelburcht", "Helemaal bovenop de Spiegelburcht, tussen de spiegels. Hoe ben je hier gekomen?"),
    "onder_de_brug": ("Onder de westelijke hangbrug", "Aan een touwtje onder de westelijke hangbrug bungelt een schatkistje. Niet naar beneden "
                                                       "kijken!"),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Gids pages (questlog J, tab Gids: ids z2_* in QuestlogScherm.GIDS), tips, bestiary pages, other texts
# ---------------------------------------------------------------------------------------------------------------------------
GIDS = {
    "z2_splinterpieken": ("Zone 2: De Splinterpieken", "Na zone 1 en vanaf level 20 vliegt Stuurguh Zeppie je vanuit De Haven naar "
                          "Kandijkom, een warm stadje in een reuzengeode. Daaromheen: de Galmhellingen (oost, lv 20-23), het Bevroren "
                          "Knuffelmeer (west, lv 23-26), de Splinterkloof (noord, lv 26-29) en de Spiegelburcht (lv 28-30)."),
    "z2_kristalwezens": ("Kristalwezens", "In de bergen vecht je tegen echte monsters: grauw kristal zonder hart. Verslagen spatten ze "
                         "uit elkaar in scherven die wegsmelten. Mika's vluchten in een rookwolk naar huis. Guhs doe je nooit pijn: "
                         "versteende guhs bevrijd je met een klik."),
    "z2_bevroren": ("Bevroren", "Bevroren is een status: je bent trager, en zware klappen doen extra schade. Ga bij een kampvuur staan "
                    "of wacht tot het ontdooit. Wezens kun je ook laten bevriezen: geef ze dan een flinke klap!"),
    "z2_edelstenen": ("Edelstenen en sokjes", "Sommige spullen hebben 0, 1 of 2 sokjes. Ruwe kristallen uit de bergen zet je erin bij "
                      "Juweliersguh Glinster in Kandijkom. Uithalen kan ook, en een edelsteen gaat nooit kapot."),
    "z2_reizen": ("Luchtschip en gondel", "Kapitein Poolster vliegt je van Kandijkom terug naar De Haven, en Stuurguh Zeppie weer heen. "
                  "Gondelguh Kabeltje brengt je met de gondel naar de top van de Galmhellingen."),
    "z2_titel": ("De derde titel", "Op level 30 wacht je een derde titel van De Knuffelorde, bij Ordemeester Kristalijn in het "
                 "Kristalkapel van Kandijkom: Kristalkampioen, Poolstervalk of Kristalorakel."),
}
TIPS = {
    "z2_versteend": ("Versteend", "Een versteende guh is van grauw kristal, maar leeft nog. Klik erop als je quest erom vraagt: dan "
                                  "bevrijd je hem. Guhs doe je nooit pijn."),
    "z2_kristal": ("Kristalwezens", "Deze wezens zijn echte monsters. Verslagen spatten ze in scherven uit elkaar. Pas op voor "
                                    "Splinterlingen: die schieten scherven als je ze raakt."),
    "z2_bevroren": ("Bevroren!", "Je bent bevroren: trager, en zware klappen doen extra pijn. Even uit de kou, of wachten tot het ontdooit."),
    "z2_edelsteen": ("Een ruw kristal!", "Ruwe kristallen zet je in de sokjes van je spullen bij Juweliersguh Glinster in Kandijkom."),
}
WEZENS = {   # (mob ids of the z2-wezens/z2-bazen slices: guessed, integration aligns them; NOTITIES_Z2_NPC.md)
    "vr_z2_splinterling": "Een kristalgolem van grauw kristal. Bij elke klap schiet hij scherven rond. Verslagen spat hij uit elkaar.",
    "vr_z2_galmvleermuis": "Wordt onzichtbaar en is alleen te horen. Luister naar het gefladder!",
    "vr_z2_ijzelgeest": "Een zwevende ijsgeest die de grond laat bevriezen. Pikt graag wanten.",
    "vr_z2_mika_mijnwerker": "Een Mika met een helmpje en een boor. Verslagen vlucht hij in een rookwolk naar zijn fort.",
    "vr_z2_galmwolf": "De Galmwolf, wereldbaas van de Galmhellingen. Huilt kristalgolven: schuil achter de bevroren pilaren.",
    "vr_z2_spiegelzucht": "Vrouwe Spiegelzucht, vrouwe van de Spiegelburcht. Alleen het spiegelbeeld zonder schaduw is echt.",
    "vr_z2_boormeester": "De Mika-boormeester en zijn machine met drie boorkoppen. Laat ze in de goede volgorde oververhitten.",
}
OVERIG = {
    Z + "optie.naar_kandijkom": "Naar Kandijkom (De Splinterpieken)",
    Z + "optie.naar_haven": "Terug naar De Haven",
    P + "optie.rol.luchtschip_z2": "Terug naar De Haven",
    # (fix round 2) the Spiegelburcht's puzzle rooms: a line at the bottom of the screen while you are inside (npc/KamerTips)
    P + "kamer.z2_lichtkamer": "Lichtkamer: draai de spiegels met rechtsklik (/ , \\ of plat) tot de lichtstraal het kristalslot raakt. "
                               "Dan gaat de kristalpoort open.",
    P + "kamer.z2_prismazaal": "Prismazaal: twee stralen, twee sloten. Draai de spiegels (rechtsklik) tot BEIDE sloten tegelijk gloeien.",
    P + "kamer.z2_spiegelgang": "Spiegelgang: overleef drie golven kristalwezens. Rode banen = vliegende scherven: stap eruit voor de "
                                "baan vol is!",
    P + "kamer.z2_opgelost": "Opgelost! De kristalpoort is open: ga door naar de volgende zaal.",
    P + "stap.z2_poort": "%s. Eerst de geode van Kandijkom uit: %s.",
    P + "plek.z2_poort_oost": "door de oostpoort, aan het eind van de straat langs de Kandijkraam",
    P + "plek.z2_poort_west": "door de westpoort, aan het eind van de straat langs Warme Wantjes",
    P + "plek.z2_poort_noord": "door de noordpoort, achter de Kristalkapel",
    P + "reis.kies.z2_kapitein": "Terug naar het warme zuiden? Kies hieronder waarheen of bekijk de reiskaart. (De scheepsbel op de steiger "
                                 "opent de reiskaart ook.)",
    P + "optie.rol.juwelier": "Edelstenen inzetten of uithalen",
    P + "optie.rol.gondel": "Met de gondel naar de top",
    P + "optie.rol.gondel_terug": "Met de gondel naar beneden",
    P + "optie.rol.chocomelk": "Een beker chocomelk, graag!",
    Z + "luchtschip.te_laag": "Brrr. In de Splinterpieken is het ijskoud en gevaarlijk, avonturier. Kom terug als je level 20 bent. "
                              "(nu level %s)",
    Z + "juwelier.later": "Mijn sokjesbank is nog in de maak (dat komt met de systemen van zone 2). Kom straks terug!",
    Z + "chocomelk": "Alsjeblieft, een warme beker chocomelk. Met een marshmallow. Nou ja, twee.",
    Z + "chocomelk.al": "Je hebt er net al een gehad! Eerst opdrinken, dan krijg je er nog een.",
    Z + "gondel.omhoog": "Instappen! De gondel gaat omhoog. Niet wiebelen. Oké, een beetje wiebelen.",
    Z + "bevrijd": "Je hebt %s bevrijd!",
    Z + "draak.splinter.vraag": "Er steekt een grauwe kristalsplinter in zijn vleugel. Hij ademt zwaar. Hij kijkt naar je, maar hij "
                                "beweegt niet.",
    Z + "draak.splinter.optie": "Trek de splinter er heel voorzichtig uit",
    Z + "draak.splinter.0": "Op het bergzadel ligt iets enorms. Grauwe veren, pluizige vleugels, oortjes als hoorns. Vahoegnir.",
    Z + "draak.splinter.1": "Hij is gewond. Uit zijn vleugel steekt een grote grauwe kristalsplinter. Om hem heen is de sneeuw grijs.",
    Z + "draak.splinter.2": "Hij ziet je. Hij gromt niet. Hij zucht alleen. Heel diep.",
    Z + "draak.weg": "Vahoegnir kijkt je lang aan. Heel even zie je iets roze in zijn ogen. Dan slaat hij zijn vleugels uit en vliegt weg.",
    Z + "scene.opening.titel": "De Splinterpieken",
    Z + "scene.opening.onder": "Kandijkom, de warme geode",
    Z + "scene.storm": "Een sneeuwstorm! IJsschotsen slaan tegen de romp!",
    Z + "scene.licht_uit": "De lichten vallen uit...",
    Z + "scene.wolf": "Op een kristalnaald staat een wolf van grauw kristal. Hij huilt.",
    Z + "scene.naald": "De naald barst! Het schip glijdt net op tijd de warme geode in.",
    Z + "scene.aankomst": "Welkom in Kandijkom! Cafébaas Mallow wacht met warme chocomelk in het café aan het plein.",
    Z + "scene.draak.titel": "Vahoegnir",
    Z + "scene.draak.onder": "Gewond, op het bergzadel",
    Z + "scene.flashback.titel": "Lang geleden...",
    Z + "scene.flashback.onder": "Vahoegnir, de roze beschermdraak van het Vadsrijk",
    Z + "scene.einde.titel": "Wordt vervolgd...",
    Z + "scene.einde.onder": "Ver in het oosten, achter de wolken, staat een boom zo groot als een berg.",
    Z + "scene.gondel.titel": "De Galmhellingen",
    Z + "scene.gondel.onder": "Het bovenste gondelstation",
    Z + "scene.vlucht.kandijkom.titel": "Kandijkom",
    Z + "scene.vlucht.kandijkom.onder": "De Splinterpieken",
    Z + "scene.vlucht.haven.titel": "De Haven",
    Z + "scene.vlucht.haven.onder": "Zwamzoet",
    Z + "zone2.klaar": "De eerste splinter van het hartkristal is gevonden! De Splinterpieken zingen weer zuiver.",
    Z + "knuffel.gevonden": "Knuffel bevrijd onder het ijs: %s! (%s/%s)",
    Z + "aanwijzing.naam": "Boorspoor",
    Z + "aanwijzing.teller": "Boorsporen gevonden: %s van de %s. Uitvinder Tandwiel wil dit vast weten!",
    Z + "aanwijzing.alle": "Alle vijf de boorsporen wijzen naar de bodem van de Splinterkloof, helemaal in het westen. De boorhal staat open!",
    Z + "schat.titel": "Schat gevonden!",
    P + "adv.knuffelridder.3": "Kristalkampioen", P + "adv.pluisjager.3": "Poolstervalk", P + "adv.vadsmagier.3": "Kristalorakel",
}


# ---------------------------------------------------------------------------------------------------------------------------
# building
# ---------------------------------------------------------------------------------------------------------------------------
def _java_ids():
    """NPC/POI/area ids of Z2NpcPlekken.java (+ zone 1's NpcPlekken for the shared NPCs such as the haven pilot)."""
    with open(os.path.join(JAVA, "Z2NpcPlekken.java"), encoding="utf-8") as f:
        src = f.read()
    npc = set(re.findall(r'NPC\.put\("([a-z_0-9]+)"', src))
    poi = set(re.findall(r'POI\.put\("([a-z_0-9]+)"', src))
    gebied = set(re.findall(r'GEBIED_DOEL\.put\("([a-z_0-9]+)"', src))
    baas = set(re.findall(r'BAAS\.put\("([a-z_0-9]+)"', src))
    with open(os.path.join(JAVA, "..", "..", "npc", "NpcPlekken.java"), encoding="utf-8") as f:
        npc1 = set(re.findall(r'NPC\.put\("([a-z_0-9]+)"', f.read()))
    return npc, poi, gebied, baas, npc1


def teksten():
    t = dict(OVERIG)
    t.update(PLEK_OVERIG)
    npc_ids, poi_ids, gebied_ids, baas_ids, _npc1 = _java_ids()
    for id, (naam, regels, waar) in NPCS.items():
        assert len(regels) == (4 if id in VIER_REGELS else 3), id
        t[P + "naam." + id] = naam
        t[P + "plek.npc." + id] = waar
        for i, r in enumerate(regels, 1):
            t[P + f"praat.{id}.{i}"] = r
    assert set(NPCS) == npc_ids, f"NPC ids differ from Z2NpcPlekken: {set(NPCS) ^ npc_ids}"
    for id, x in VERSTEEND_VRIJ.items():
        t[Z + "vrij." + id] = x
    for id, x in PLEK_POI.items():
        t[P + "plek.poi." + id] = x
    assert set(PLEK_POI) == poi_ids, f"POI ids differ from Z2NpcPlekken: {set(PLEK_POI) ^ poi_ids}"
    for id, x in PLEK_GEBIED.items():
        t[P + "plek.gebied." + id] = x
    for id, x in PLEK_KAMPVUUR.items():
        t[P + "plek.kampvuur." + id] = x
    for id, x in PLEK_BAAS.items():
        t[P + "plek.baas." + id] = x
    assert gebied_ids <= set(PLEK_GEBIED) and baas_ids <= set(PLEK_BAAS), "area/boss without a plek text"
    for id, (naam, _tex) in QUEST_ITEMS.items():
        t[f"item.guhs.vr_z2_npc_{id}"] = naam
    for id, regel in QUEST_ITEM_LORE.items():
        assert id in QUEST_ITEMS, id
        t[f"item.guhs.vr_z2_npc_{id}.lore"] = regel
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
    for id, (titel, tekst) in TIPS.items():
        t[P + "tip." + id] = tekst
        t[P + "tip." + id + ".titel"] = titel
    for id, (titel, tekst) in GIDS.items():
        t[P + "gids." + id + ".titel"] = titel
        t[P + "gids." + id + ".tekst"] = tekst
    for id, tekst in WEZENS.items():
        t[P + "wezen." + id] = tekst
    for k, v in t.items():
        assert "%" not in v.replace("%s", ""), f"stray % in {k}"
    return t


_PLEK_SOORTEN = ("npc:", "poi:", "gebied:", "baas:")


def quests_json():
    npc_ids, poi_ids, gebied_ids, baas_ids, npc1 = _java_ids()
    alle_npc = npc_ids | npc1
    ids = [q["id"] for q in QUESTS]
    assert len(ids) == len(set(ids)), "duplicate quest id"
    assert all(i.startswith("z2_") for i in ids), "zone-2 quest ids start with z2_"
    uit = []
    for q in QUESTS:
        for wie in (q["gever"], q["inleveren"]):
            assert wie in alle_npc, f"{q['id']}: unknown NPC {wie}"
        for na in q["na"]:
            assert na in ids or not na.startswith("z2_"), f"{q['id']}: unknown previous quest {na}"
        for d in q["doelen"]:
            # the zone-1 playtest lesson: every step says WHO/WHERE, and points somewhere
            assert d["plek"].startswith(_PLEK_SOORTEN), f"{q['id']}: objective without a plek: {d['tekst']}"
            soort, _, x = d["plek"].partition(":")
            assert x in {"npc": alle_npc, "poi": poi_ids, "gebied": set(PLEK_GEBIED), "baas": set(PLEK_BAAS)}[soort], \
                f"{q['id']}: unknown plek {d['plek']}"
            for y in (d["doel"], d["bron"]):
                if y.startswith("poi:"):
                    assert y[4:] in poi_ids, f"{q['id']}: unknown poi {y}"
            if d["soort"] == "kampvuur":
                assert d["doel"] in PLEK_KAMPVUUR, f"{q['id']}: unknown campfire area {d['doel']}"
            if d["soort"] in ("praat", "escorte"):
                assert d["doel"] in alle_npc, f"{q['id']}: unknown NPC {d['doel']}"
            if d["soort"] == "verzamel":
                assert d["doel"][len("guhs:vr_z2_npc_"):] in QUEST_ITEMS, f"{q['id']}: unknown quest item {d['doel']}"
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
    zij = [q for q in QUESTS if q["soort"] == "zij"]
    assert len(hoofd) >= 12 and len(zij) == len(ZIJ_HOUDEN), (len(hoofd), len(zij))
    return {"_": "generated by tools/features/vadsrijk_z2_npc.py; do not edit", "quests": uit}


def advancements():
    namen = [f"vr_{q['id']}" for q in QUESTS] + ["vr_z2_knuffels_alle", "vr_z2_aanwijzingen_alle"] + [f"vr_z2_schat_{s}" for s in SCHATTEN]
    return {f"advancement/quest/{n}.json": {"criteria": {"done": {"trigger": "minecraft:impossible"}}} for n in namen}


def _assets(w, a):
    for id, (_naam, tex) in QUEST_ITEMS.items():
        w(os.path.join(a, "items", f"vr_z2_npc_{id}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_z2_npc_{id}"}})
        w(os.path.join(a, "models", "item", f"vr_z2_npc_{id}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})


def build(h):
    h.w(f"{h.D}/vadsrijk/npc/quests_z2.json", quests_json())
    for pad, obj in advancements().items():
        h.w(f"{h.D}/{pad}", obj)
    _assets(h.w, h.A)
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


# ---------------------------------------------------------------------------------------------------------------------------
# FTB Quests: zone 2 joins the chapter "Het Vadsrijk" (vadsrijk_npc.py) with its own sections. Nothing is locked.
# ---------------------------------------------------------------------------------------------------------------------------
FTB_CHAPTER = "guhs_vadsrijk"
FTB_PORTRAIT = "guh:snow"
FTB_SECTIES = [
    ("vr_z2_verhaal", "De Splinterpieken (zone 2)", "guh:snow", [f"vr_{q['id']}" for q in QUESTS if q["soort"] == "hoofd"]),
    ("vr_z2_kandijkom", "Kandijkom helpen", "guh:choco", [f"vr_{q['id']}" for q in QUESTS if q["soort"] in ("zij", "klasse", "rijdier")]),
    ("vr_z2_geheimen", "Geheimen van de Splinterpieken", "guh:mint", ["vr_z2_geheim_boormeester", "vr_z2_knuffels_alle",
                                                                      "vr_z2_aanwijzingen_alle"] + [f"vr_z2_schat_{s}" for s in SCHATTEN]),
]


def ftb(fq):
    q, adv = fq.q, fq.adv
    K = "guhs:kaas_knabbels"
    for d in QUESTS:
        tekst = d["tekst"].replace("Kandijkom", "&bKandijkom&r")
        icon = {"hoofd": "minecraft:amethyst_shard", "klasse": "minecraft:enchanted_book", "geheim": "minecraft:spyglass",
                "rijdier": "minecraft:saddle"}.get(
            d["soort"], "minecraft:writable_book")
        vorm = "gear" if d["soort"] == "hoofd" else ("hexagon" if d["soort"] in ("klasse", "geheim") else None)
        q(f"vr_{d['id']}", d["titel"], tekst + " &7(Vinkt zichzelf af als je hem in het Vadsrijk inlevert.)", icon,
          [fq.met(adv(f"vr_{d['id']}"), d["titel"])], rewards=((K, 8 if d["soort"] == "zij" else 16),), shape=vorm,
          xp=100 if d["soort"] == "hoofd" else 50)
    q("vr_z2_knuffels_alle", "Tien knuffels onder het ijs", "Bevrijd alle &e10 knuffels&r van Kleine Vlokje onder het ijs van de "
      "Splinterpieken. Ze glinsteren met hartjes.", "guhs:knuffel_snow", [fq.met(adv("vr_z2_knuffels_alle"), "10 knuffels")],
      rewards=((K, 16),), shape="hexagon")
    q("vr_z2_aanwijzingen_alle", "Het gebrom in de berg", "Vind alle &e5 boorsporen&r in de Splinterpieken. Waar staat die machine?",
      "minecraft:spyglass", [fq.met(adv("vr_z2_aanwijzingen_alle"), "5 boorsporen")], rewards=((K, 16),), shape="hexagon")
    for s, (naam, _t) in SCHATTEN.items():
        q(f"vr_z2_schat_{s}", naam, "Een verborgen schat van de Splinterpieken. Hij glinstert goudgeel als je dichtbij bent.",
          "minecraft:chest", [fq.met(adv(f"vr_z2_schat_{s}"), naam)], rewards=((K, 8),))


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
    # (J-INT: the mount teasers incl. z2_rijdier_guhwolf are written by vadsrijk_npc.py (i-mounts), so not ours to drop)
    return ((k.startswith(Z) or k.startswith("item.guhs.vr_z2_npc_") or (k.startswith(P) and ".z2_" in k)
            or k.startswith(P + "plek.overal.teller:z2_")) and not k.startswith(P + "rijdierquest.teaser."))


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "vadsrijk", "npc", "quests_z2.json"), quests_json())
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
    oud = os.path.join(d, "advancement", "quest")
    for f in os.listdir(oud):
        if f.startswith("vr_z2_") and f"advancement/quest/{f}" not in advancements():
            os.remove(os.path.join(oud, f))
    print(f"vadsrijk_z2_npc: {len(QUESTS)} quests ({sum(q['soort'] == 'hoofd' for q in QUESTS)} main, "
          f"{sum(q['soort'] == 'zij' for q in QUESTS)} side), {len(NPCS)} NPCs, {len(t)} texts")


if __name__ == "__main__":
    _direct()
