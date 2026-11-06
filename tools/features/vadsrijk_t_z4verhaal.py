"""
1.10 "Het Vadsrijk", zone 4 "De Lampionkust" (lv 50-56), night round T, slice t-z4verhaal. Java: feature/vadsrijk/zone4/verhaal.

Zone 4 uses the SAME quest system as zones 1-3 (feature/vadsrijk/npc). This generator only adds zone-4 data:
  - the NPC guhs of Lampionhaven and the islands: names, idle lines and WHERE they stand (vadsrijk.npc.plek.npc.<id>);
  - the zone-4 quests -> data/guhs/vadsrijk/npc/quests_z4.json (read by Quests.java after quests_z3.json) + their texts;
  - every objective names WHO, WHERE and WHAT and carries an explicit "plek" (npc:/poi:/gebied:/baas:) for the tracker + beam;
  - the Knuffeltij knuffels (a FIXED number, only inside the story: 6 on the beach, 4 big ones on the islands, 2 in the wreck),
    their little stories (soft flashes of the grey east), the secrets, scene texts, Gids pages, tips, the Verkenningsboek texts;
  - quest items vr_z4_npc_*, hidden FTB advancements guhs:quest/vr_z4_*, the FTB sections (ftb()).
Main line "De Grote Knuffeltij" (7 quests), 16 side quests. Ids: NPCs and quests "z4_*", items "vr_z4_npc_*".
All texts Dutch; en_us gets the same Dutch texts (T_BRIEF: English only where cheap).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z4verhaal.py
"""
import json
import os
import re

P = "vadsrijk.npc."           # (the shared quest system's keys: naam/praat/q/plek/gids/tip...)
Z = "vadsrijk.z4verhaal."     # (zone-4 own texts: scenes, knuffels, secrets, roles, mini games)
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone4", "verhaal")

_LH = "in Lampionhaven"
_PL = "op het Havenplein van Lampionhaven"
_KT = "op het Knuffeltij-strand, het eiland ten zuidoosten van Lampionhaven"
_SF = "op het Strandfeesteiland, het eiland ten noordoosten van Lampionhaven"
_BE = "op het Bloemeneiland, midden in de eilandslinger"
# ---------------------------------------------------------------------------------------------------------------------------
# NPCs: name, 3 idle lines, WHERE (a full phrase for the tracker). Spots: Z4VerhaalPlekken.java; clothes/roles: Z4Npcs.java.
# ---------------------------------------------------------------------------------------------------------------------------
NPCS = {
    # --- Lampionhaven: the story ------------------------------------------------------------------------------------------------
    "z4_havenmeester": ("Havenmeester Ankertje", [
        "Welkom in Lampionhaven! Huisjes op palen, lampions aan elk touw, en de zee is roze. Echt waar. Kijk maar.",
        "Zie je die stad boven de zee? Hoogvads. Op de rug van een walvis. Ik kijk er elke ochtend naar met mijn koffie.",
        "Een haven zonder bootjes is een badkuip. Een haven zonder lampions is... nou ja, ook een badkuip. Maar donker."],
        "op de kade van Lampionhaven, bij de middelste steiger aan de haveninham (de oostkant van het stadje)"),
    "z4_zeilverhuur": ("Botenguh Zeiltje", [
        "Een zeilbootje met oortjes! Het zeil heeft een gezichtje. Het knipoogt als de wind goed staat.",
        "Vaar maar rond, hoor. Alleen niet te ver: aan de rand draait de Kringstroom je zachtjes terug. Net een draaimolen.",
        "Ik heb ooit een bootje gebouwd van een theekopje. Het zonk. Maar heel gezellig."],
        "aan de noordelijke steiger van de haveninham van Lampionhaven, bij de bootjes"),
    "z4_luchthaven": ("Luchthavenguh Windvaan", [
        "Vanaf dit platform vliegen de luchtschepen terug naar de Boom en verder. Pak de reiskaart maar.",
        "Het luchtschip naar Hoogvads? Dat gaat niet van hier. Daarvoor moet je naar de Opstijgpier, helemaal in het oosten.",
        "Wind uit het westen: goed vliegweer. Wind uit het oosten: grauw weer. Daar hou ik niet van."],
        "op het luchthavenplatform aan de westkant van Lampionhaven, bij het aangemeerde luchtschip"),
    # --- Lampionhaven: shops and services ---------------------------------------------------------------------------------------
    "z4_lampionmaker": ("Lampionmaakster Gloeiwiek", [
        "Roze, geel en mint. Meer kleuren heeft een lampion niet nodig. Nou ja, misschien lila. Voor zondag.",
        "Een lampion is een klein zonnetje aan een touwtje. Je kunt het overal mee naartoe nemen.",
        "Als de knuffeltij komt, hangen we er duizend op. Dan zien de knuffels de weg naar huis."],
        "in de lampionwerkplaats aan de westkant van het Havenplein van Lampionhaven"),
    "z4_bankier": ("Bankier Muntschelp", [
        "Je spullen veilig bewaren? Bij mij ligt alles in een grote schelp. Een hele grote. Met een slot.",
        "Rente betaal ik in schelpjes. Grapje. Of niet. Kijk maar in je kluis.",
        "Een slimme guh spaart. Een nog slimmere guh spaart in een kluis die niet kan zinken."],
        "in de bank op de noordkant van het Havenplein van Lampionhaven"),
    "z4_werkplaats": ("Werkmeester Krul", [
        "Hier wordt geknutseld, gezaagd en geknoopt. Net als op een schip. Maar dan op het droge.",
        "Netten knopen, vliegers bespannen, bootjes lakken. Ik doe het allemaal. Behalve afwassen.",
        "Wat je zelf maakt, gaat langer mee. En het ruikt naar zaagsel. Heerlijk."],
        "in de werkplaats achter de lampionwerkplaats, aan de westkant van het Havenplein van Lampionhaven"),
    "z4_winkel": ("Winkelguh Strandtas", [
        "Drankjes, verband en zonnebrandsmeersel voor je oortjes! Alles voor een dagje kust.",
        "Een guh zonder strandtas is als een strand zonder zand. Raar.",
        "Kijk gerust rond. Pas op voor de krab, die woont in de kassa."],
        "in de winkel aan de zuidkant van het Havenplein van Lampionhaven"),
    "z4_smid": ("Smidguh Ankerslag", [
        "Ik smeed ankers, haken en zwaarden. Vooral ankers. De zee is groot.",
        "Zout water roest alles. Daarom poets ik elke avond mijn hamer. Hij glimt als een parel.",
        "Kom maar met je spullen. Sterren erop, roest eraf."],
        "in de smidse bij het begin van de zuidarm van Lampionhaven, aan de kade"),
    "z4_wapens": ("Wapenhandelaar Harpoentje", [
        "Harpoenen, netten en schelpenschilden. Alles wat een kustridder nodig heeft.",
        "Mijn harpoenen zijn heel lief. Ze prikken alleen in monsters. En in worstjes.",
        "Kies je wapen met je hart. Maar ook met je klasse, anders past het niet."],
        "in de uitrustingswinkel aan de zuidkant van het Havenplein van Lampionhaven"),
    "z4_kleermaker": ("Kleermaker Zeemeeuw", [
        "Gestreepte zeemanstruien, zonnehoedjes en badmutsjes! Wie wil er niet chic de zee op?",
        "Ik naai met zeilgaren. Dat scheurt nooit. Behalve als je heel hard lacht.",
        "Een sjaaltje tegen de zeewind, en je bent klaar voor alles."],
        "in de kledingwinkel aan de noordkant van het Havenplein van Lampionhaven"),
    "z4_markt": ("Marktguh Schelpje", [
        "Kopen, verkopen, ruilen! De kustmarkt is altijd open, ook als het regent. Dan hebben we parasols.",
        "Vandaag in de aanbieding: zand. Gratis. Het ligt overal.",
        "Hier ruil je met andere avonturiers. Eerlijk is eerlijk, zegt de schelp."],
        "op de kustmarkt midden op het Havenplein van Lampionhaven"),
    "z4_kok": ("Kok Garnaal", [
        "Welkom in De Blije Bot! Vissoep, garnalenkoekjes en zeewierijs. Dat laatste is beter dan het klinkt.",
        "Ik kook alleen met vis die zelf wilde meedoen. Heel eerlijke vissoep.",
        "Een kom soep? Dat warmt je oortjes op na een dag op zee."],
        "in visrestaurant De Blije Bot, aan het eind van de noordarm van Lampionhaven"),
    "z4_visser": ("Visser Dobber", [
        "Vissen is wachten. En wachten is ook fijn. Vooral met een broodje.",
        "Gisteren ving ik een laars. Vandaag een vis in een laars. Morgen misschien de andere laars.",
        "Hengel uitgooien, dobber kijken, niet in slaap vallen. Dat laatste is het moeilijkst."],
        "aan het eind van de zuidelijke steiger van de haveninham van Lampionhaven, met zijn hengel"),
    "z4_duinwachter": ("Duinwachter Helm", [
        "Vanaf de Lampionduin zie je alles: de Boom achter je, Hoogvads voor je, en alle eilandjes ertussen.",
        "Steek het kampvuur aan. Hier word je wakker als het misgaat. Warm, veilig, en je verliest niks.",
        "De wezens hier zijn grauw en verdrietig. Vrolijk ze op, avonturier. Ze worden er zelf ook blij van."],
        "bij het kampvuur op de top van de Lampionduin, het hoge duin aan de noordkant van Lampionhaven"),
    # --- Lampionhaven: citizens --------------------------------------------------------------------------------------------------
    "z4_oma_zeeschuim": ("Oma Zeeschuim", [
        "Toen ik klein was, spoelde er elk jaar een knuffel aan voor mij. Een eendje. Hij staat nog op mijn vensterbank.",
        "De Grote Knuffeltij komt één keer per jaar. Als de lampions aangaan. Dan kijk ik altijd naar het water.",
        "Dit jaar zijn ze grauw. Dat heb ik nog nooit gezien. Nooit."],
        _PL),
    "z4_kind_lampje": ("Kleine Lampje", [
        "Ik heb een lampion gemaakt! Hij is scheef. Maar hij brandt!",
        "Weet jij waarom de zee roze is? Ik denk dat er een heel grote aardbei in ligt.",
        "Later word ik vliegerkampioen. Of walvis. Walvis mag ook."],
        _PL),
    "z4_kind_golfje": ("Kleine Golfje", [
        "Ik kan al zwemmen! Met zwembandjes. En met mama ernaast. Maar wel zelf!",
        "Er zit een krab onder de steiger. Hij heet Knijp. Ik heb hem zelf zo genoemd.",
        "Heb jij een eigen bootje? Mag ik mee? Ik ben heel licht."],
        "op de kade van Lampionhaven, bij de steigers"),
    "z4_postguh": ("Postguh Flessenpost", [
        "Post op de eilandjes gaat met flessenpost. Ik gooi hem in het water en de stroming doet de rest. Meestal.",
        "Soms spoelt er flessenpost aan van heel ver weg. Uit het oosten. Die briefjes zijn... grauw.",
        "Zoem zoem, post! O nee, dat was de Boom. Hier is het: plons plons, post!"],
        _PL),
    "z4_danseres": ("Dansguh Wiegel", [
        "'s Avonds dansen we op de kade, onder de lampions. Kom je ook?",
        "Een golf dansen gaat zo: heen, weer, heen, en dan plons.",
        "Ik dans voor de knuffels die thuiskomen. Dan voelen ze zich welkom."],
        "op de kade van Lampionhaven, onder de lampionsnoeren"),
    "z4_schipper": ("Schipper Kotter", [
        "Ik vaar al veertig jaar tussen de eilandjes. Ik ken elke golf bij naam.",
        "Pas op bij de Grauwe Mistbank in het zuidoosten. Daar raak je je kompas kwijt. En je goede humeur.",
        "Een goed bootje is als een goede vriend. Het brengt je overal heen en zegt nooit nee."],
        "op de zuidarm van Lampionhaven, bij de paalwoningen"),
    # --- Knuffeltij-strand ---------------------------------------------------------------------------------------------------------
    "z4_opvang": ("Opvangmoeder Zachtje", [
        "Welkom in het opvangcentrum. Hier is iedereen welkom: guhs, knuffels, en verdrietige wezentjes.",
        "Een kopje warme melk, een dekentje en een knuffel. Daar knapt iedereen van op.",
        "De guhs uit het oosten zijn mager en stil. Maar elke dag lachen ze een beetje meer."],
        "in het opvangcentrum op het Knuffeltij-strand, het eiland ten zuidoosten van Lampionhaven"),
    "z4_wasguh": ("Wasguh Sopje", [
        "In de knuffelwas worden grauwe knuffels weer roze. Met zeep, warm water en heel veel liefde.",
        "Daarna hangen ze aan de droogwaslijn. Kijk maar, de helft is al weer roze!",
        "Ik was alles. Knuffels, sokken, en soms per ongeluk de kat."],
        "bij de knuffelwas naast het opvangcentrum op het Knuffeltij-strand"),
    "z4_soepguh": ("Soepguh Lepeltje", [
        "Soep voor iedereen! Ook als je geen honger hebt. Dan krijg je een klein kommetje.",
        "De guhs uit het oosten hebben zo lang niks warms gehad. Ik maak elke dag een grote pan.",
        "Mijn geheime ingrediënt? Een knuffel. Niet in de soep, hoor. Na de soep."],
        "bij het soepkraampje op het Knuffeltij-strand, naast het opvangcentrum"),
    "z4_grijsje": ("Grijsje", [
        "Ik kom uit het oosten. Daar is alles grauw. Hier is alles roze. Ik moet er nog aan wennen.",
        "Mijn knuffel heet Muis. Ik ben hem kwijt geraakt op de vlucht. Misschien spoelt hij aan.",
        "Opvangmoeder Zachtje geeft me elke avond warme melk. Met een rietje!"],
        "bij het opvangcentrum op het Knuffeltij-strand, op een dekentje in het zand"),
    "z4_opa_dun": ("Opa Dun", [
        "Waar ik vandaan kom, mag je niet delen. Delen is dom, zeggen ze daar. Hier deelt iedereen alles.",
        "Ik ben mijn vads bijna kwijt. Maar de soep hier helpt. Elke dag een lepel vads erbij.",
        "Weet je wat ik het meest gemist heb? Lachen. Gewoon lachen. Om niks."],
        "op een bankje bij het soepkraampje op het Knuffeltij-strand"),
    "z4_vaaltje": ("Vaaltje", [
        "We kwamen met een bootje door de mist. Het was heel koud. En heel stil.",
        "Ik heb nog nooit een lampion gezien. Mag ik er één vasthouden?",
        "Hier zijn de wezens grauw, maar ze willen opgevrolijkt worden. Daar zijn ze anders. Daar mag niks blij zijn."],
        "bij de droogwaslijn op het Knuffeltij-strand"),
    "z4_mama_pluis": ("Mama Pluis", [
        "Mijn pluis is bijna weer terug. Kijk, mijn oortjes worden al een beetje roze.",
        "Ik had ooit een hele kast vol knuffels. Ze namen ze allemaal mee. Voor de Schaal, zeiden ze.",
        "Dank je wel dat je hier bent. Het voelt hier als thuis. Een nieuw thuis."],
        "bij de ingang van het opvangcentrum op het Knuffeltij-strand"),
    # --- the islands -----------------------------------------------------------------------------------------------------------------
    "z4_limonade": ("Limonadeguh Bubbel", [
        "Schelpenlimonade! Roze, zoet en met een rietje van zeegras. Proef maar!",
        "Het geheim zit in de schelpjes. Die geven een zeesmaakje. Een héél klein zeesmaakje.",
        "Een glaasje limonade en een parasol. Meer heeft een strand niet nodig."],
        "aan de schelpenlimonadebar op het Strandfeesteiland, het eiland ten noordoosten van Lampionhaven"),
    "z4_volleybal": ("Volleybalguh Smash", [
        "Beachvolleybal! Bal over het net, niet in de zee. Dat laatste lukt me nooit.",
        "Ik heb nog nooit verloren. Ik heb ook nog nooit gewonnen. Ik speel altijd alleen.",
        "Kom op, een potje! Ik geef je een voorsprong. Van nul punten."],
        "bij het volleybalveld aan de oostkant van het Strandfeesteiland"),
    "z4_feestguh": ("Feestguh Confetti", [
        "Hier is het altijd feest! 's Ochtends een klein feestje, 's middags een middelgroot feestje en 's avonds een groot feest.",
        "Het kampvuur gaat nooit uit. Daar zorg ik voor. Met marshmallows.",
        "Dansen rond het kampvuur? Je voeten weten vanzelf hoe het moet."],
        "bij het kampvuur midden op het Strandfeesteiland"),
    "z4_vliegermaker": ("Vliegermaker Wimpel", [
        "Een guh-vlieger heeft oortjes, een gezichtje en een staart met strikjes. Anders vliegt hij niet. Echt waar.",
        "Op dit eiland waait het altijd. Daarom woon ik hier. En omdat mijn hoed hier steeds wegwaait, dus ik kan niet weg.",
        "Lat, stof, touw en een lint. Meer heb je niet nodig. En geduld. Veel geduld."],
        "in de vliegerwerkplaats op het Vliegereiland, het winderige eiland in het noordoosten"),
    "z4_windmeester": ("Windmeester Zwiep", [
        "Ik ben de scheidsrechter van de grote vliegerwedstrijd. Ik let op de wind. En op valsspelers.",
        "Wie zijn vlieger het langst in de wind houdt bij de wedstrijdpaal, wint. Zo simpel is het.",
        "De grote wedstrijdvlieger daarboven? Die is van mij. Hij heet Henk."],
        "bij de wedstrijdpaal op de top van het Vliegereiland"),
    "z4_duikmeester": ("Duikmeester Parel", [
        "Onder water is de Koraalbaai nog mooier dan erboven. Roze koraal, glimmende vissen en drie geheime grotjes.",
        "Duikbril op, adem in, en plons! Kom weer boven voor je blauw wordt. Of roze.",
        "In het diepste grotje ligt iets groots en verdrietigs. Ik durf er niet dichtbij."],
        "op de duiksteiger aan de noordkant van de ring van de Koraalbaai, het atol midden in de zee"),
    "z4_wachter": ("Opa Lichtstraal", [
        "Ik pas al zestig jaar op de vuurtoren. Elke avond steek ik het licht aan. Ook als er geen bootjes zijn.",
        "Een vuurtoren zegt: hier is land, hier is warmte, kom maar. Dat is alles. Meer hoeft hij niet te zeggen.",
        "Van bovenaf zie ik alle stromingen. De zee vertelt me alles. Als ik goed luister."],
        "in het huisje van de wachter onder de grote vuurtoren op het Vuurtoreneiland, in het oosten"),
    "z4_imker": ("Imker Nectar", [
        "Mijn bijen maken roze honing. Van de bloemen hier. Proef maar, het smaakt naar zomer.",
        "Een bijenboerderij op een eiland is fijn. De bijen kunnen niet weglopen. Ze kunnen wel wegvliegen. Dat doen ze ook.",
        "Zoem zoem. Dat is bijentaal voor 'goedemorgen'. Of voor 'pas op'. Het hangt ervan af."],
        "bij de bijenboerderij op het Bloemeneiland, midden in de eilandslinger"),
    "z4_madelief": ("Kransenmaakster Madelief", [
        "Een bloemenkrans maakt iedereen mooi. Zelfs een Mika. Nou ja, bijna.",
        "Ik vlecht kransen voor het knuffelfeest. Eén voor elke knuffel die thuiskomt.",
        "Ruik eens. Dat is de zee en de bloemen samen. Mijn lievelingsgeur."],
        "bij de bloemenvelden aan de zuidkant van het Bloemeneiland"),
    "z4_schelpie": ("Kluizenaar Schelpie", [
        "Ik woon op Oma Dobber. Die slaapt al honderd jaar. Haar man, Ouwe Dobber, zwemt rondjes om haar heen en komt elk rondje even bij de steiger liggen.",
        "Elke ochtend word ik ergens anders wakker. Heel avontuurlijk, voor een kluizenaar.",
        "Ouwe Dobber is honderd jaar. Of tweehonderd. Hij zegt het niet. Hij zegt sowieso weinig."],
        "in het hutje op de rug van Oma Dobber, de slapende reuzenschildpad ten oosten van het Bloemeneiland"),
    "z4_nevel": ("Verdwaalde Nevel", [
        "Ik kwam door de mist. Uit het oosten. Ik durf nog niet verder. Hier is het... tussenin.",
        "In de mist hoor je stemmen. Ze zeggen: 'Stil is snel.' Ik wil niet meer stil zijn.",
        "Achter de mist begint het grauwe land. Ik ben er weg. Ik ga er nooit meer terug."],
        "bij de vervallen grauwe poort op het Mysterie-eiland, het mistige eiland in het zuidoosten"),
    "z4_pierwachter": ("Pierwachter Wolkje", [
        "De Opstijgpier. Hier stijgen de luchtschepen op naar Hoogvads. Nou ja, straks. Als de walvis laag genoeg komt.",
        "Ik wacht al jaren op het eerste luchtschip. Ik heb mijn mooiste pet al op.",
        "Kijk omhoog. Die schaduw op het water? Dat is Moeder Wolkwal. Ze zwemt door de lucht."],
        "bij het begin van de Opstijgpier op de Pierkop, het puntje helemaal in het oosten van de eilandslinger"),
    "z4_hemelkapitein": ("Kapitein Hemelhoog", [
        "Mijn luchtschip is gebouwd om omhoog te gaan. Heel erg omhoog. Tot boven de wolken.",
        "Hoogvads is de mooiste stad van de wereld. Ik ben er geboren. Op de rug van een walvis. Raar, hè?",
        "Als de walvis laag komt, stijgen we op. Eerder niet. Zo hoog kan zelfs ik niet."],
        "aan het eind van de Opstijgpier, op het ronde platform bij de meermast met oortjes"),
}

# the Kustwacht at the campfires (Z4VerhaalPlekken z4_wacht_*): name, where; they share their lines
WACHTEN = {
    "z4_wacht_haven": ("Golfwacht", "op het Havenplein van Lampionhaven, bij het kampvuur"),
    "z4_wacht_knuffeltij": ("Knuffelwacht", "bij het kampvuur op het Knuffeltij-strand"),
    "z4_wacht_strandfeest": ("Parasolwacht", "bij het kampvuur op het Strandfeesteiland"),
    "z4_wacht_bloemen": ("Bloemwacht", "bij het kampvuur aan de noordkant van het Bloemeneiland"),
    "z4_wacht_koraal": ("Koraalwacht", "bij het kampvuur op de noordring van de Koraalbaai"),
    "z4_wacht_vlieger": ("Windwacht", "bij het kampvuur aan de voet van het Vliegereiland"),
    "z4_wacht_vuurtoren": ("Lichtwacht", "bij het kampvuur bij het steigertje van het Vuurtoreneiland"),
    "z4_wacht_pier": ("Pierwacht", "bij het kampvuur op de Pierkop, vlak bij de Opstijgpier"),
}
WACHT_REGELS = [
    "Steek het kampvuur aan: hier word je wakker als je flauwvalt. Warm, veilig, en je verliest niks.",
    "De Kustwacht houdt de lampions brandend en de bootjes heel. En we delen onze koekjes.",
    "De wezens aan de kust zijn grauw en verdrietig. Vrolijk ze op, avonturier. Een roze plof en ze zijn weer blij."]
for _id, (_naam, _waar) in WACHTEN.items():
    NPCS[_id] = ("Kustwacht " + _naam, WACHT_REGELS, _waar)

# ---------------------------------------------------------------------------------------------------------------------------
# Where: POIs / areas / campfires / boss spots (vadsrijk.npc.plek.<kind>.<id>)
# ---------------------------------------------------------------------------------------------------------------------------
PLEK_POI = {
    "z4_havenplein": "midden op het Havenplein van Lampionhaven",
    "z4_luchthaven": "op het luchthavenplatform aan de westkant van Lampionhaven",
    "z4_steiger": "aan de noordelijke steiger van de haveninham van Lampionhaven",
    "z4_lampionduin": "op de top van de Lampionduin, het hoge duin aan de noordkant van Lampionhaven",
    "z4_knuffeltij_strand": "op het Knuffeltij-strand, het eiland ten zuidoosten van Lampionhaven (vaar met je bootje of steek over via de zandbank)",
    "z4_opvangcentrum": "bij het opvangcentrum op het Knuffeltij-strand",
    "z4_knuffelwas": "bij de knuffelwas naast het opvangcentrum op het Knuffeltij-strand",
    "z4_droogwaslijn": "bij de droogwaslijn op het Knuffeltij-strand",
    "z4_feestplek": "midden op het Knuffeltij-strand, tussen het opvangcentrum en de vloedlijn (daar wordt het feest gevierd)",
    "z4_grote_knuffel_1": "half in de branding aan de zuidkust van het Strandfeesteiland, bij de parasols",
    "z4_grote_knuffel_2": "aan de westkust van het Bloemeneiland, tussen de bloemen in het zand",
    "z4_grote_knuffel_3": "op de zuidring van de Koraalbaai, bij de opening van het atol",
    "z4_grote_knuffel_4": "op de rug van Oma Dobber, de slapende reuzenschildpad, naast het hutje van Kluizenaar Schelpie",
    "z4_mysterie_poort": "bij de vervallen grauwe poort op het Mysterie-eiland, in de mist in het zuidoosten",
    "z4_wrak": "bij het Wrak van de Vliegervanger, het gestrande Mika-vliegerschip op een rotsje ten noorden van de Koraalbaai",
    "z4_wrak_ruim": "in het ruim van het Wrak van de Vliegervanger (het luik op het dek omlaag)",
    "z4_wedstrijdpaal": "bij de wedstrijdpaal op de top van het Vliegereiland",
    "z4_vliegerwerkplaats": "bij de vliegerwerkplaats op het Vliegereiland",
    "z4_duiksteiger": "op de duiksteiger aan de noordkant van de ring van de Koraalbaai",
    "z4_grotje_1": "in het diepste onderwatergrotje van de Koraalbaai, in het midden van de lagune",
    "z4_grotje_2": "in het tweede onderwatergrotje van de Koraalbaai, aan de zuidkant van de lagune",
    "z4_grotje_3": "in het derde onderwatergrotje van de Koraalbaai, aan de noordkant van de lagune",
    "z4_vuurtoren_top": "boven in de grote vuurtoren op het Vuurtoreneiland (de wenteltrap op)",
    "z4_bijenboerderij": "bij de bijenboerderij op het Bloemeneiland",
    "z4_strandfeest_kampvuur": "bij het kampvuur midden op het Strandfeesteiland",
    "z4_limonadebar": "bij de schelpenlimonadebar op het Strandfeesteiland",
    "z4_volleybalveld": "op het volleybalveld aan de oostkant van het Strandfeesteiland",
    "z4_schildpad": "op de rug van Oma Dobber, de slapende reuzenschildpad ten oosten van het Bloemeneiland (Ouwe Dobber zwemt eromheen)",
    "z4_pierkop": "op de Pierkop, het puntje helemaal in het oosten van de eilandslinger",
    "z4_opstijgpier": "aan het eind van de Opstijgpier, op het ronde platform bij de meermast (in het oosten, onder de walvis)",
    "z4_kringstroom": "bij de schuimstrepen van de Kringstroom, ten zuiden van het Knuffeltij-strand (vaar met je bootje naar de rand)",
}
PLEK_GEBIED = {
    "z4_lampionhaven": "in Lampionhaven, het havenstadje op palen en duinen in het westen van de Lampionkust",
    "z4_knuffeltij": "op het Knuffeltij-strand, het eiland ten zuidoosten van Lampionhaven",
    "z4_strandfeest": "op het Strandfeesteiland, ten noordoosten van Lampionhaven",
    "z4_bloemeneiland": "op het Bloemeneiland, midden in de eilandslinger",
    "z4_koraalbaai": "in de Koraalbaai, het roze atol midden in de zee",
    "z4_vliegereiland": "op het Vliegereiland, het winderige eiland in het noordoosten",
    "z4_vuurtoreneiland": "op het Vuurtoreneiland, in het oosten",
    "z4_schildpad": "op Oma Dobber, de slapende reuzenschildpad ten oosten van het Bloemeneiland",
    "z4_mysterie": "op het Mysterie-eiland, in de grauwe mist in het zuidoosten",
    "z4_pierkop": "op de Pierkop met de Opstijgpier, helemaal in het oosten",
    "z4_lampionkust": "aan De Lampionkust: de eilandjes en de roze zee tussen de Boom en Hoogvads",
}
# (fallback area names: t-z4wereld owns vadsrijk.kern.gebied.z4_*; written here only so the tracker/book never shows raw keys)
GEBIED_NAMEN = {
    "z4_lampionhaven": "Lampionhaven", "z4_knuffeltij": "Het Knuffeltij-strand", "z4_strandfeest": "Het Strandfeesteiland",
    "z4_bloemeneiland": "Het Bloemeneiland", "z4_koraalbaai": "De Koraalbaai", "z4_vliegereiland": "Het Vliegereiland",
    "z4_vuurtoreneiland": "Het Vuurtoreneiland", "z4_schildpadeiland": "Het Schildpadeiland", "z4_mysterie": "Het Mysterie-eiland",
    "z4_pierkop": "De Pierkop", "z4_lampionkust": "De Lampionkust",
}
PLEK_KAMPVUUR = {
    "z4_lampionhaven": _PL,
    "z4_knuffeltij": _KT,
    "z4_strandfeest": "midden op het Strandfeesteiland",
    "z4_bloemeneiland": "aan de noordkant van het Bloemeneiland",
    "z4_koraalbaai": "op de noordring van de Koraalbaai",
    "z4_vliegereiland": "aan de voet van het Vliegereiland",
    "z4_vuurtoreneiland": "bij het steigertje van het Vuurtoreneiland",
    "z4_pierkop": "op de Pierkop, vlak bij de Opstijgpier",
}
PLEK_BAAS = {
    "z4_koraalbaai": "in het diepste onderwatergrotje van de Koraalbaai (duik vanaf de duiksteiger de lagune in)",
    "z4_wrak": "in het Wrak van de Vliegervanger, ten noorden van de Koraalbaai",
}
PLEK_OVERIG = {
    P + "plek.overal.teller:z4_tijknuffels": "langs de vloedlijn van het Knuffeltij-strand (grauwe pluisjes in de branding)",
    P + "plek.overal.teller:z4_vliegerspul": "op het Vliegereiland (lat, stof, touw en lint waaien overal rond)",
    P + "plek.overal.teller:z4_parels": "onder water in de lagune van de Koraalbaai (glinstertjes)",
    P + "plek.overal.teller:z4_schelpen": "op de stranden van het Strandfeesteiland",
    P + "plek.overal.teller:z4_bijtjes": "op het Bloemeneiland, tussen de bloemen",
    P + "plek.overal.teller:z4_bloemen": "in de bloemenvelden van het Bloemeneiland",
    P + "plek.overal.teller:z4_lampionhaken": "in Lampionhaven: bij de steigers, op de kade en op het plein (gouden glinsters)",
    P + "plek.overal.teller:z4_mistbrieven": "op het Mysterie-eiland, in de mist",
    P + "plek.overal.teller:z4_knuffels": "verstopt aan De Lampionkust: op de eilandjes, onder water en in de mist (hartjes)",
    P + "plek.overal.teller:z4_flessenpost": "aangespoeld op de stranden van De Lampionkust (blauwe glinsters)",
    P + "plek.z4_luchtschip": "bij Luchthavenguh Windvaan, op het luchthavenplatform van Lampionhaven",
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quest items (Z4VerhaalRegistry.QUEST_ITEMS): name, vanilla texture
# ---------------------------------------------------------------------------------------------------------------------------
QUEST_ITEMS = {
    "vlieger": ("Guh-vlieger (met oortjes)", "minecraft:item/paper"),
    "limonade": ("Schelpenlimonade", "minecraft:item/honey_bottle"),
    "vissoep": ("Vissoep van De Blije Bot", "minecraft:item/mushroom_stew"),
    "soepkom": ("Kom soep voor een vluchteling", "minecraft:item/beetroot_soup"),
    "krans": ("Bloemenkrans voor het feest", "minecraft:block/pink_tulip"),
    "zeewier": ("Lekker zeewier", "minecraft:item/kelp"),
    "flesje": ("Flessenpost van Grijsje", "minecraft:item/glass_bottle"),
    "knuffelzak": ("Zak met gestolen knuffels", "minecraft:item/bundle"),
    "lampion": ("Feestlampion", "minecraft:block/lantern"),
}

# ---------------------------------------------------------------------------------------------------------------------------
# The Knuffeltij knuffels: a FIXED number, only inside the story (Z4VerhaalPlekken.TIJKNUFFEL / GROTE_KNUFFEL / WRAKKNUFFEL).
# Each tells a tiny story; the big ones are soft flashes of the grey east.
# ---------------------------------------------------------------------------------------------------------------------------
TIJKNUFFELS = {   # (6 on the beach, z4_hq02)
    "strand_1": ("Knuffel Muis", "Ik ben van Grijsje. Ze hield me elke nacht vast. Toen kwam er een Mika met een zak. Ik was zo bang."),
    "strand_2": ("Knuffel Eendje", "Kwak. Ik dreef heel lang. Het water werd steeds warmer. En toen... roze! Ben ik thuis?"),
    "strand_3": ("Knuffel Beertje Bas", "In mijn oude huis mocht ik niet op de vensterbank. 'Knuffels zijn zonde van de vads,' zeiden ze."),
    "strand_4": ("Knuffel Konijn Hop", "Ik heb één oor. Het andere ligt nog in het grauwe land. Maar met één oor hoor ik jou ook."),
    "strand_5": ("Knuffel Olifantje", "We lagen met honderden in een kar. Allemaal stil. Toen viel ik eraf. Plons. Gelukkig."),
    "strand_6": ("Knuffel Sterretje", "Mijn lichtje deed het niet meer. Kijk... nu knippert het weer een beetje!"),
}
GROTE_KNUFFELS = {   # (4 big ones on the islands, z4_hq03: soft flashes)
    "groot_1": ("De Grote Knuffelwalrus", [
        "Ik kom uit een stad waar alle ramen dicht zijn. Daar hangen posters: MAGER IS MOOI. STIL IS SNEL.",
        "Mijn guh woonde in een huisje aan een grauwe buis. Elke nacht zoemde die buis. Hij zoog iets weg. Het warme.",
        "Op een dag kwamen de Mika's met zakken. Alle knuffels moesten mee. 'Voor de Schaal.' Mijn guh huilde niet. Dat mocht niet."]),
    "groot_2": ("De Grote Knuffelgiraf", [
        "Ik zag de weg. Een lange grauwe weg, recht naar het oosten. Er reden karren over. Dag en nacht.",
        "In de karren zaten knuffels, lampions, muziekdoosjes. Alles wat warm is. Alles ging naar het oosten.",
        "Aan het eind van de weg stond een burcht. Zo groot dat de wolken eromheen moesten. Daar ging alles naar binnen. En er kwam nooit iets uit."]),
    "groot_3": ("De Grote Knuffelkwal", [
        "Ik werd in een schip geladen. Een grauw schip, met grauwe zeilen. Er waren er heel veel. Een hele vloot.",
        "De kapitein droeg ringen om haar tenen. Ze telde alles. Elke knuffel. Elke lampion. Twee keer.",
        "Er kwam een storm. Het schip helde over. Ik gleed eruit, de zee in. Ik dreef weg. Ik was blij."]),
    "groot_4": ("De Grote Knuffeldraak", [
        "Ik ben maar een knuffel. Maar ik heb iets gezien wat niemand mocht zien.",
        "Een zaal. Grauw licht. Een troon. En op de armleuning... vijf magere vingertjes. Tik. Tik. Tik. Tik. Tik.",
        "Een trage stem zei: 'Meer.' Alleen dat. 'Meer.' En toen werd ik in een zak gestopt. Dag, troon."]),
}
WRAKKNUFFELS = {   # (2 stolen ones in the wreck, z4_hq05)
    "wrak_1": ("Knuffel Pinguïn Pip", "Die Mika met de vlieger pakte me van de waslijn. Ik was nog niet eens droog!"),
    "wrak_2": ("Knuffel Lammetje Wol", "Hij wilde ons naar het oosten vliegen. 'Een bonus van de Ringvinger,' zei hij. Wie is dat?"),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quests. D(type, doel, n, text, bron, kans, plek): text = WHAT + WHO + WHERE; plek = where the tracker points.
# ---------------------------------------------------------------------------------------------------------------------------
QUESTS = []


def D(soort, doel, n, tekst, bron="", kans=0.0, plek=""):
    return dict(soort=soort, doel=doel, n=n, tekst=tekst, bron=bron, kans=kans, plek=plek)


def Q(id, soort, gever, titel, tekst, doelen, aanbod, bezig, klaar, xp=0, munten=0, items=(), wapen=False, loot="", lv=50, na=(),
      inleveren=None, klasse=None, auto=False, praat=None, actie_aanname="", actie_klaar="", start_items=()):
    QUESTS.append(dict(id=id, soort=soort, gever=gever, titel=titel, tekst=tekst, doelen=doelen, aanbod=aanbod, bezig=bezig, klaar=klaar,
                       beloning=dict(xp=xp, munten=munten, items=[list(i) for i in items], wapen=wapen, loot=loot), lv=lv, na=list(na),
                       inleveren=inleveren or gever, klasse=klasse, auto=auto, praat=praat or {}, actie_aanname=actie_aanname,
                       actie_klaar=actie_klaar, start_items=[list(i) for i in start_items]))


def item(id):
    return "guhs:vr_z4_npc_" + id


# ===== the main story of zone 4: "De Grote Knuffeltij" (7) ====================================================================
Q("z4_hq01_lampionkust", "hoofd", "z3_wasja", "Lampions aan de kust",
  "Ordemeester Wasja kreeg een brief uit Lampionhaven, het havenstadje aan De Lampionkust. Elk jaar spoelen daar knuffels uit de hele "
  "wereld aan: de Grote Knuffeltij. Maar dit jaar zijn ze grauw. Vlieg vanaf de Boom naar Lampionhaven en meld je bij Havenmeester Ankertje.",
  [D("meld", "z4:aangekomen", 1, "Vlieg naar Lampionhaven: neem het luchtschip vanaf de Boom (Kapitein Zoemzeil, of de reiskaart: kies De Lampionkust)",
     plek="npc:z3_kapitein"),
   D("praat", "z4_havenmeester", 1, "Meld je bij Havenmeester Ankertje op de kade van Lampionhaven, bij de middelste steiger aan de haveninham",
     plek="npc:z4_havenmeester")],
  [("npc", "Ridder! Drakenvriend! Er kwam vanochtend een brief. Met een lampionstempel erop. Uit Lampionhaven, aan de kust in het oosten."),
   ("npc", "Elk jaar, als de lampions aangaan, spoelen daar knuffels aan. Uit de hele wereld. Ze noemen het de Grote Knuffeltij."),
   ("jij", "Knuffels die aanspoelen? Dat klinkt... heel lief eigenlijk."),
   ("npc", "Dat is het ook. Normaal. Maar Havenmeester Ankertje schrijft: 'Dit jaar zijn ze grauw. En verdrietig. Kan de Orde helpen?'"),
   ("npc", "Grauw... dat ruikt naar Mika's. Vlieg erheen. En kijk goed naar de lucht boven de zee. Je zult zien waarom.")],
  "Vlieg naar Lampionhaven en meld je bij Havenmeester Ankertje op de kade.",
  [("npc", "Daar ben je! Een echte ridder van de Orde! Welkom in Lampionhaven, het roze puntje van de wereld."),
   ("npc", "Kijk, daarboven, boven de zee. Die stad op die wolk... dat is geen wolk. Dat is Moeder Wolkwal. En op haar rug: Hoogvads."),
   ("jij", "Een stad. Op een walvis. Die vliegt."),
   ("npc", "Je went eraan. Na een jaar of tien. Maar luister: de knuffels. Ze liggen op het Knuffeltij-strand, ten zuidoosten van hier."),
   ("npc", "Opvangmoeder Zachtje vangt ze op. En niet alleen knuffels. Er komen ook guhs uit het oosten. Mager, stil, grauw. Ga je helpen?")],
  munten=6000, items=[(item("vissoep"), 2)], lv=50, na=["z3_hq15_hoogvads"], inleveren="z4_havenmeester", actie_aanname="z4_vlieg")

Q("z4_hq02_knuffeltij", "hoofd", "z4_havenmeester", "De Grote Knuffeltij",
  "Op het Knuffeltij-strand liggen grauwe, verdrietige knuffels in de branding. Haal eerst je eigen zeilbootje bij Botenguh Zeiltje, "
  "vaar naar het strand en vraag Wasguh Sopje hoe het zit. Vrolijk zes knuffels langs de vloedlijn op en meld je bij Opvangmoeder Zachtje.",
  [D("meld", "z4:zeilboot", 1, "Haal je eigen guh-zeilbootje bij Botenguh Zeiltje aan de noordelijke steiger van Lampionhaven",
     plek="npc:z4_zeilverhuur"),
   D("gebied", "poi:z4_knuffeltij_strand", 1, "Vaar met je zeilbootje naar het Knuffeltij-strand, het eiland ten zuidoosten van Lampionhaven",
     plek="poi:z4_knuffeltij_strand"),
   D("praat", "z4_wasguh", 1, "Praat met Wasguh Sopje bij de knuffelwas naast het opvangcentrum op het Knuffeltij-strand", plek="npc:z4_wasguh"),
   D("geheim", "teller:z4_tijknuffels", 6, "Vrolijk 6 grauwe knuffels op langs de vloedlijn van het Knuffeltij-strand (loop naar de grauwe pluisjes in de branding)",
     plek="poi:z4_knuffeltij_strand")],
  [("npc", "Het Knuffeltij-strand ligt op een eilandje. Lopen gaat niet, zwemmen is koud. Je hebt een bootje nodig."),
   ("npc", "Botenguh Zeiltje aan de noordelijke steiger heeft er eentje voor je. Met oortjes op het zeil. Echt waar."),
   ("npc", "Vaar naar het strand en help Zachtje. Ze zit in het opvangcentrum. En wees lief voor de knuffels. Ze zijn zo grauw.")],
  "Haal je zeilbootje bij Botenguh Zeiltje, vaar naar het Knuffeltij-strand en vrolijk zes knuffels op voor Opvangmoeder Zachtje.",
  [("npc", "Zes knuffels, en alle zes weer een beetje roze. Kijk eens hoe ze naar je kijken."),
   ("npc", "Maar ze vertelden dingen... over een grauw land. Karren. Zakken. Een Schaal. Ik word er koud van."),
   ("jij", "Ze komen uit het oosten. Dat zeiden ze allemaal."),
   ("npc", "Er liggen nog vier grote knuffels op de andere eilandjes. Op het Strandfeesteiland, het Bloemeneiland, de Koraalbaai en... op de schildpad."),
   ("npc", "Haal ze op en luister naar hun verhaal. Daarna weet Opa Lichtstraal in de vuurtoren vast waar ze vandaan drijven. Hij kent de zee.")],
  munten=7000, loot="zeldzaam", lv=50, na=["z4_hq01_lampionkust"], inleveren="z4_opvang",
  praat={"z4_wasguh": [
      ("npc", "O, een ridder! Precies op tijd. Kijk daar, langs de vloedlijn. Grauwe knuffels. Ze spoelen al de hele week aan."),
      ("npc", "Normaal zijn ze vrolijk en kleurig. Zo de was in, zo weer roze. Maar deze... ze willen niet eens dat je ze oppakt. Ze rillen."),
      ("npc", "Ga er gewoon naast zitten. Zeg iets liefs. Dan vertellen ze je wel hun verhaal. Zes liggen er hier, in de branding."),
      ("npc", "Daarna wil Opvangmoeder Zachtje je zien, in het opvangcentrum. Zij houdt alles bij.")]})

Q("z4_hq03_waarvandaan", "hoofd", "z4_opvang", "Waar kom jij vandaan?",
  "Vier grote knuffels zijn op andere eilandjes aangespoeld. Vaar erheen, vrolijk ze op en luister naar hun verhaal. Daarna wil Opa "
  "Lichtstraal in de vuurtoren je zien: hij weet waar de stroming ze vandaan brengt.",
  [D("gebied", "poi:z4_grote_knuffel_1", 1, "Vrolijk de grote knuffel op die half in de branding ligt aan de zuidkust van het Strandfeesteiland",
     plek="poi:z4_grote_knuffel_1"),
   D("gebied", "poi:z4_grote_knuffel_2", 1, "Vrolijk de grote knuffel op aan de westkust van het Bloemeneiland, midden in de eilandslinger",
     plek="poi:z4_grote_knuffel_2"),
   D("gebied", "poi:z4_grote_knuffel_3", 1, "Vrolijk de grote knuffel op op de zuidring van de Koraalbaai, bij de opening van het atol",
     plek="poi:z4_grote_knuffel_3"),
   D("gebied", "poi:z4_grote_knuffel_4", 1, "Vrolijk de grote knuffel op op de rug van Oma Dobber, de slapende reuzenschildpad ten oosten van het Bloemeneiland",
     plek="poi:z4_grote_knuffel_4"),
   D("praat", "z4_wachter", 1, "Praat met Opa Lichtstraal in het huisje onder de grote vuurtoren op het Vuurtoreneiland, in het oosten",
     plek="npc:z4_wachter")],
  [("npc", "Vier grote knuffels. Te groot voor de branding hier: ze zijn bij de andere eilandjes blijven steken."),
   ("npc", "Vaar maar rond met je bootje. Op het Strandfeesteiland, het Bloemeneiland, de Koraalbaai en op de rug van de reuzenschildpad."),
   ("npc", "Grote knuffels hebben grote verhalen. Luister goed. En ga daarna naar Opa Lichtstraal, in de vuurtoren in het oosten.")],
  "Vier grote knuffels op vier eilandjes, en daarna naar Opa Lichtstraal in de vuurtoren.",
  [("npc", "Kom binnen, kom binnen. Ik zag je bootje al vanaf boven. Rondjes, rondjes, van eiland naar eiland."),
   ("jij", "De grote knuffels... ze zagen een grauwe stad, een lange weg, een vloot en een troon. Alles ging naar het oosten."),
   ("npc", "Hmm. Weet je wat ik zie, vanaf mijn toren? Alle knuffels drijven uit dezelfde hoek. Uit het zuidoosten."),
   ("npc", "Uit de Grauwe Mistbank. Daar komt geen bootje doorheen. Al jaren niet. En toch... sinds kort komen er wél bootjes uit. Met guhs erin.")],
  munten=8000, lv=51, na=["z4_hq02_knuffeltij"], inleveren="z4_wachter")

Q("z4_hq04_mist", "hoofd", "z4_wachter", "De Grauwe Mistbank",
  "Alle knuffels drijven uit het zuidoosten, uit de Grauwe Mistbank. Op de rand van de mist ligt het Mysterie-eiland. Vaar erheen en kijk "
  "bij de vervallen poort. Daar zou een guh wonen die door de mist is gekomen.",
  [D("gebied", "poi:z4_mysterie_poort", 1, "Vaar naar het Mysterie-eiland in de mist in het zuidoosten en loop naar de vervallen grauwe poort",
     plek="poi:z4_mysterie_poort"),
   D("meld", "z4:mistflash", 1, "Kijk door de grauwe poort, de mist in", plek="poi:z4_mysterie_poort"),
   D("praat", "z4_nevel", 1, "Praat met Verdwaalde Nevel bij de grauwe poort op het Mysterie-eiland", plek="npc:z4_nevel"),
   D("praat", "z4_opvang", 1, "Breng het nieuws naar Opvangmoeder Zachtje bij het opvangcentrum op het Knuffeltij-strand", plek="npc:z4_opvang")],
  [("npc", "Het Mysterie-eiland ligt op de rand van de mist. Er staat een oude poort. Grauw. Niemand weet wie hem gebouwd heeft."),
   ("npc", "Mijn licht reikt niet tot daar. De mist slikt het op. Neem je eigen licht mee, ridder. In je hart, bedoel ik."),
   ("npc", "En daarna: terug naar Zachtje. Ze maakt zich zorgen. Dat merk ik aan haar soep. Die is te zout als ze zich zorgen maakt.")],
  "Naar de grauwe poort op het Mysterie-eiland, praat met Verdwaalde Nevel, en dan terug naar Opvangmoeder Zachtje.",
  [("npc", "Een vloot vol knuffels... en een Mika met ringen om haar tenen. Arme knuffels. Arme guhs."),
   ("npc", "Maar wacht, er is iets anders. Iets ergs. Vanochtend hingen er acht roze knuffels aan de droogwaslijn. Nu nog zes."),
   ("jij", "Gestolen?"),
   ("npc", "Iemand zag een vlieger. Een grote grauwe vlieger met een Mika eraan. Hij vloog naar het noorden, naar dat oude wrak bij de Koraalbaai.")],
  munten=8000, loot="zeldzaam", lv=52, na=["z4_hq03_waarvandaan"], inleveren="z4_opvang", actie_klaar="start:z4_hq05_vliegervanger",
  praat={"z4_nevel": [
      ("npc", "...Jij bent geen Mika. Gelukkig. Ik dacht even... Sorry. Ik schrik nog steeds van alles."),
      ("npc", "Ik kom uit het oosten. Door de mist. Op een vlot van knuffels, geloof het of niet. Ze hielden me drijvend."),
      ("npc", "Daarginds varen grauwe schepen. De Grauwe Vloot. Vol met alles wat warm is: knuffels, lampions, liedjes in doosjes."),
      ("npc", "Ze worden aangevoerd door een Mika met ringen om al haar tenen. Ze noemen haar de Ringvinger. Ze telt alles. Twee keer."),
      ("npc", "Als er een storm is, valt er wat overboord. Dat zijn jullie knuffels. Het is de enige weg naar buiten. Via de zee.")]})

Q("z4_hq05_vliegervanger", "hoofd", "z4_opvang", "De Vliegervanger",
  "Een Mika aan een grote grauwe oorlogsvlieger heeft twee roze knuffels van de droogwaslijn gestolen. Hij verstopt zich in het Wrak van de "
  "Vliegervanger, een gestrand Mika-vliegerschip ten noorden van de Koraalbaai. Haal de knuffels terug.",
  [D("gebied", "poi:z4_wrak", 1, "Vaar naar het Wrak van de Vliegervanger, het gestrande Mika-vliegerschip op een rotsje ten noorden van de Koraalbaai",
     plek="poi:z4_wrak"),
   D("meld", "z4_vliegervanger:verslagen", 1, "Versla Kootje de Vliegervanger in het ruim van het wrak (het luik op het dek omlaag)",
     plek="baas:z4_wrak"),
   D("geheim", "teller:z4_wrakknuffels", 2, "Pak de 2 gestolen knuffels terug uit het ruim van het wrak", plek="poi:z4_wrak_ruim"),
   D("praat", "z4_opvang", 1, "Breng de knuffels terug naar Opvangmoeder Zachtje op het Knuffeltij-strand", plek="npc:z4_opvang")],
  [("npc", "Twee knuffels gestolen. Van mijn waslijn! Ze waren nog niet eens droog."),
   ("npc", "Het wrak ligt op een rotsje ten noorden van de Koraalbaai. Een oud Mika-schip met een gescheurde vlieger in de mast."),
   ("npc", "Wees voorzichtig. Maar wees ook een beetje boos. Dat mag best, als iemand knuffels steelt.")],
  "Naar het Wrak van de Vliegervanger, Kootje verslaan en de twee knuffels terug naar Zachtje brengen.",
  [("npc", "Pip! Wol! Daar zijn jullie! Kom maar, het droogt veel beter als je niet in een Mika-zak zit."),
   ("jij", "Kootje zei dat hij ze naar het oosten wilde vliegen. Voor een bonus van de Ringvinger."),
   ("npc", "Die naam weer. Ik wil hem nooit meer horen. Weet je wat we nu gaan doen? Iets wat geen Mika snapt."),
   ("npc", "Een feest. Het grote terug-naar-huis-feest. Twaalf knuffels, allemaal weer roze. En de guhs uit het oosten krijgen een nieuw thuis.")],
  munten=10000, loot="episch", lv=53, na=["z4_hq04_mist"], inleveren="z4_opvang", auto=True)

Q("z4_hq06_feest", "hoofd", "z4_opvang", "Het terug-naar-huis-feest",
  "Opvangmoeder Zachtje geeft een groot feest op het Knuffeltij-strand: de twaalf knuffels gaan naar huis, en de vluchtelingen krijgen een "
  "nieuw thuis. Haal lampions, feestvliegers en schelpenlimonade, en vier het feest mee.",
  [D("praat", "z4_lampionmaker", 1, "Haal feestlampions bij Lampionmaakster Gloeiwiek in de lampionwerkplaats van Lampionhaven", plek="npc:z4_lampionmaker"),
   D("praat", "z4_vliegermaker", 1, "Haal feestvliegers bij Vliegermaker Wimpel in de vliegerwerkplaats op het Vliegereiland", plek="npc:z4_vliegermaker"),
   D("praat", "z4_limonade", 1, "Haal een vat schelpenlimonade bij Limonadeguh Bubbel op het Strandfeesteiland", plek="npc:z4_limonade"),
   D("meld", "z4:feest", 1, "Vier het feest op het Knuffeltij-strand: loop naar de feestplek tussen het opvangcentrum en de vloedlijn",
     plek="poi:z4_feestplek"),
   D("meld", "z4:wolkwal", 1, "Kijk omhoog, boven de zee", plek="poi:z4_feestplek")],
  [("npc", "Een feest heeft drie dingen nodig: licht, iets in de lucht en iets lekkers. Zo staat het in mijn feestboek."),
   ("npc", "Lampions van Gloeiwiek in Lampionhaven. Vliegers van Wimpel op het Vliegereiland. En limonade van Bubbel op het Strandfeesteiland."),
   ("npc", "Kom dan terug naar het strand. Iedereen is er. Ook de knuffels. Vooral de knuffels.")],
  "Lampions, vliegers en limonade halen, en dan het feest op het Knuffeltij-strand.",
  [("npc", "Ridder... heb je dat gezien? Ze is zo laag. Zo laag is ze in honderd jaar niet geweest."),
   ("npc", "Moeder Wolkwal. Ze hoorde het feest. Walvissen houden van liedjes, weet je."),
   ("jij", "Haar ogen... ze leken moe. En ze trok een grauw sliertje achter zich aan."),
   ("npc", "Dat zag ik ook. Ik weet niet wat het betekent. Maar Kapitein Hemelhoog weet het wel. Hij wacht op de Opstijgpier, helemaal in het oosten."),
   ("npc", "Als de walvis zo laag hangt, kan zijn luchtschip eindelijk omhoog. Naar Hoogvads.")],
  munten=12000, items=[(item("lampion"), 1)], lv=54, na=["z4_hq05_vliegervanger"], inleveren="z4_havenmeester",
  praat={
      "z4_lampionmaker": [
          ("npc", "Lampions voor het knuffelfeest? Ik heb er al driehonderd klaar! Roze, geel en mint. En eentje lila. Voor de zekerheid."),
          ("npc", "Ik breng ze zelf naar het strand. Zeg maar tegen Zachtje dat ze om zes uur aangaan.")],
      "z4_vliegermaker": [
          ("npc", "Feestvliegers? Met oortjes en strikjes? Ik vlieg er twaalf naar het strand. Eén voor elke knuffel."),
          ("npc", "En de grote, Henk, mag ook mee. Hij houdt van feestjes.")],
      "z4_limonade": [
          ("npc", "Een vat limonade voor het knuffelfeest? Een vat?! Ik pak er drie. Met extra schelpjes."),
          ("npc", "Bubbel bezorgt alles. Per bootje. Het rammelt een beetje, maar het komt aan.")]})

Q("z4_hq07_opstijgpier", "hoofd", "z4_havenmeester", "De Opstijgpier",
  "Moeder Wolkwal hangt laag boven de zee. Voor het eerst in honderd jaar kan er een luchtschip omhoog naar Hoogvads. Vaar naar de Pierkop "
  "in het oosten en meld je bij Kapitein Hemelhoog aan het eind van de Opstijgpier.",
  [D("gebied", "poi:z4_pierkop", 1, "Vaar naar de Pierkop, het puntje helemaal in het oosten van de eilandslinger", plek="poi:z4_pierkop"),
   D("praat", "z4_pierwachter", 1, "Praat met Pierwachter Wolkje bij het begin van de Opstijgpier", plek="npc:z4_pierwachter"),
   D("gebied", "poi:z4_opstijgpier", 1, "Loop de Opstijgpier op tot het ronde platform aan het eind", plek="poi:z4_opstijgpier")],
  [("npc", "De walvis hangt zo laag dat mijn hoed er bijna aan blijft haken. Hoogvads! Zo dichtbij!"),
   ("npc", "Kapitein Hemelhoog wacht al jaren op dit moment. Zijn luchtschip ligt aan het eind van de Opstijgpier, helemaal in het oosten."),
   ("npc", "Vaar erheen, ridder. Lampionhaven zal je missen. Maar Hoogvads... Hoogvads heeft je nodig. Dat voel ik in mijn oortjes.")],
  "Naar de Opstijgpier in het oosten, bij Kapitein Hemelhoog.",
  [("npc", "Daar ben je! De Orde-ridder van de Knuffeltij! Heel Lampionhaven praat over je. En Hoogvads ook al, denk ik."),
   ("npc", "Kijk omhoog. Moeder Wolkwal. Ze hangt zo laag dat ik haar buik kan aaien. Dat doe ik niet. Dat is onbeleefd."),
   ("jij", "En dat grauwe sliertje? Achter haar staart?"),
   ("npc", "...Dat zie jij ook. Daar praten we boven over. Niet hier. Stap in, ridder. We gaan omhoog. Heel erg omhoog.")],
  munten=15000, loot="episch", lv=55, na=["z4_hq06_feest"], inleveren="z4_hemelkapitein", actie_klaar="z4_cliffhanger",
  praat={"z4_pierwachter": [
      ("npc", "Het eerste luchtschip naar Hoogvads! Ik heb er mijn mooiste pet voor opgezet. En mijn tweede mooiste pet eronder."),
      ("npc", "Kapitein Hemelhoog staat aan het eind van de pier. Loop maar door. Niet over de rand kijken. Of juist wel. Prachtig, hè?")]})

# ===== side quests (16, sweet) ==================================================================================================
_NA1 = ["z4_hq01_lampionkust"]
_NA2 = ["z4_hq02_knuffeltij"]   # (from here you have your sailboat)
Q("z4_s01_vlieger", "zij", "z4_vliegermaker", "Een vlieger met oortjes",
  "Vliegermaker Wimpel leert je een echte guh-vlieger bouwen. Maar de spullen zijn weggewaaid over het Vliegereiland: een lat, een lap stof, "
  "een rol touw en een staartlint.",
  [D("geheim", "teller:z4_vliegerspul", 4, "Zoek de lat, de stof, het touw en het staartlint die over het Vliegereiland zijn weggewaaid",
     plek="gebied:z4_vliegereiland")],
  [("npc", "Wil je een vlieger bouwen? Een echte? Met oortjes en een gezichtje? Natuurlijk wil je dat."),
   ("npc", "Maar de wind heeft mijn spullen weer meegenomen. Een lat, een lap stof, een rol touw en een lint. Ze liggen ergens op het eiland.")],
  "De lat, de stof, het touw en het lint: ergens op het Vliegereiland.",
  [("npc", "Alles is er! Kijk: lat kruis, stof erover, touw eraan, lint eronder... en dan de oortjes. Klaar!"),
   ("npc", "Je eigen guh-vlieger. Hij kijkt een beetje scheel. Dat hoort zo. Zo ziet hij de wind aankomen.")],
  munten=5000, items=[(item("vlieger"), 1)], lv=50, na=_NA2)

Q("z4_s02_wedstrijd", "zij", "z4_windmeester", "De grote vliegerwedstrijd",
  "Windmeester Zwiep organiseert de grote vliegerwedstrijd. Wie zijn vlieger het langst in de wind houdt bij de wedstrijdpaal, wint.",
  [D("meld", "z4:vliegerwedstrijd", 1, "Houd je guh-vlieger een halve minuut in de wind: blijf met de vlieger in je tas bij de wedstrijdpaal op de top van het Vliegereiland",
     plek="poi:z4_wedstrijdpaal")],
  [("npc", "Een deelnemer! Heb je een vlieger? Een guh-vlieger met oortjes? Laat zien... prima. Scheel, maar prima."),
   ("npc", "Ga bij de wedstrijdpaal staan en houd hem in de wind. Een halve minuut. Niet weglopen, anders telt het niet. Ik tel mee.")],
  "Een halve minuut je vlieger in de wind houden bij de wedstrijdpaal.",
  [("npc", "Een halve minuut! En hij maakte zelfs een looping! Dat heeft Henk nooit gedaan."),
   ("npc", "Je bent de winnaar van de grote vliegerwedstrijd van dit jaar. Hier is je prijs. En vertel het aan iedereen.")],
  munten=9000, loot="zeldzaam", lv=51, na=["z4_s01_vlieger"])

Q("z4_s03_vissen", "zij", "z4_visser", "Wat bijt er vandaag?",
  "Visser Dobber wil weten wat er bijt aan De Lampionkust. Vang vijf vissen met een hengel, waar je maar wilt in de roze zee.",
  [D("meld", "z4_vis:gevangen", 5, "Vang 5 vissen met een hengel aan De Lampionkust (vanaf een steiger of vanuit je bootje)",
     plek="npc:z4_visser")],
  [("npc", "Vissen is wachten, zei ik al. Maar ik wacht nu al drie dagen. Er bijt niks. Bij mij."),
   ("npc", "Misschien bijten ze wel bij jou. Vang er vijf, dan weet ik dat er nog vis in de zee zit. Hengel uitgooien en geduld hebben.")],
  "Vijf vissen vangen, waar je maar wilt aan De Lampionkust.",
  [("npc", "Vijf! En ze waren roze? Allemaal roze? Dan is de zee gezond. Wat een opluchting."),
   ("npc", "Hier, een kom vissoep van Garnaal. Ik heb hem van hem gekregen, maar ik krijg er elke dag één. Ik zit vol.")],
  munten=5000, items=[(item("vissoep"), 3)], lv=50, na=_NA1)

Q("z4_s04_parels", "zij", "z4_duikmeester", "Parels in de lagune",
  "Duikmeester Parel mist vier glimmende parels uit haar verzameling. Ze rolden van de duiksteiger de lagune in. Duik ze op.",
  [D("geheim", "teller:z4_parels", 4, "Duik 4 parels op uit de lagune van de Koraalbaai (glinstertjes onder water)", plek="gebied:z4_koraalbaai")],
  [("npc", "Oei, oei. Mijn parelketting is geknapt. Vier parels, plons, de lagune in."),
   ("npc", "Duik je ze op? Je ziet ze glinsteren tussen het koraal. Duikbril op, adem in, en niet te lang onder water blijven.")],
  "Vier parels uit de lagune van de Koraalbaai.",
  [("npc", "Alle vier! En niet eens nat geworden. O wacht, wel. Heel nat. Hier, een handdoek. En een beloning.")],
  munten=6000, loot="zeldzaam", lv=51, na=_NA2)

Q("z4_s05_zeeslurf", "zij", "z4_duikmeester", "Het verdrietige zeemonster",
  "In het diepste onderwatergrotje van de Koraalbaai ligt de Grauwe Zeeslurf: een reuzenzeekoe die heel verdrietig is geworden. Vrolijk hem op.",
  [D("meld", "z4_zeeslurf:opgevrolijkt", 1, "Vrolijk de Grauwe Zeeslurf op in het diepste onderwatergrotje van de Koraalbaai",
     plek="baas:z4_koraalbaai")],
  [("npc", "Er woont iets in het diepste grotje. Groot, grauw, met een lange slurf. Het blaast grauwe bellen. Heel verdrietige bellen."),
   ("npc", "Vroeger blies hij roze bellen. Dan speelden de visjes ermee. Kun jij hem weer blij maken? Neem vrienden mee, hij is heel groot.")],
  "De Grauwe Zeeslurf in het diepste grotje van de Koraalbaai opvrolijken.",
  [("npc", "Roze bellen! Hij blaast weer roze bellen! Kijk, de visjes spelen ermee!"),
   ("npc", "Je bent een held, voor de lagune en voor mij. Hier, een parel. Een echte. Van de Zeeslurf zelf.")],
  munten=12000, loot="episch", lv=54, na=["z4_s04_parels"])

Q("z4_s06_volleybal", "zij", "z4_volleybal", "Beachvolleybal!",
  "Volleybalguh Smash speelt altijd alleen. Speel een potje met hem: raad waar zijn bal heen gaat en scoor drie punten.",
  [D("meld", "z4:volleybal", 1, "Win een potje beachvolleybal van Volleybalguh Smash bij het volleybalveld op het Strandfeesteiland (klik op Smash)",
     plek="npc:z4_volleybal")],
  [("npc", "Een tegenstander! Eindelijk! Ik speel al vier jaar tegen de zee. De zee wint altijd."),
   ("npc", "Ik sla, jij raadt waar de bal heen gaat: links, midden of rechts. Wie het eerst drie punten heeft, wint. Ik let niet op mijn ogen. Echt niet.")],
  "Een potje volleybal tegen Smash: drie punten!",
  [("npc", "Drie punten! Je las me als een boek. Een heel dun boek, met plaatjes. Nog een potje? Morgen. Ik moet bijkomen.")],
  munten=5000, items=[(item("limonade"), 2)], lv=50, na=_NA2)

Q("z4_s07_limonade", "zij", "z4_limonade", "Schelpenlimonade",
  "Limonadeguh Bubbel heeft zes mooie schelpjes nodig voor een nieuwe pot schelpenlimonade. Ze liggen op de stranden van het Strandfeesteiland.",
  [D("geheim", "teller:z4_schelpen", 6, "Raap 6 mooie schelpjes op de stranden van het Strandfeesteiland", plek="gebied:z4_strandfeest")],
  [("npc", "Mijn schelpjes zijn op! Zonder schelpjes is het gewoon limonade. Gewone limonade! Dat kan niet."),
   ("npc", "Zes mooie schelpjes, van de stranden hier. Roze, wit, gestreept, maakt niet uit. Als ze maar glimmen.")],
  "Zes schelpjes van de stranden van het Strandfeesteiland.",
  [("npc", "Zes! Kijk hoe ze glimmen. Even roeren... proeven... perfect. Hier, de eerste flesjes zijn voor jou.")],
  munten=5000, items=[(item("limonade"), 4)], lv=50, na=_NA2)

Q("z4_s08_vuurtoren", "zij", "z4_wachter", "Het licht van Opa Lichtstraal",
  "Opa Lichtstraal krijgt de wenteltrap niet meer op. Klim naar boven in de vuurtoren en poets de grote lens, zodat het licht weer ver reikt.",
  [D("gebied", "poi:z4_vuurtoren_top", 1, "Klim de wenteltrap op naar boven in de grote vuurtoren op het Vuurtoreneiland", plek="poi:z4_vuurtoren_top"),
   D("meld", "z4:lens", 1, "Poets de grote lens boven in de vuurtoren (blijf even bij de lens staan)", plek="poi:z4_vuurtoren_top")],
  [("npc", "Honderdvierenveertig treden. Vroeger nam ik ze twee tegelijk. Nu neem ik ze... niet meer."),
   ("npc", "De lens is vies van het zout. Poets je hem voor me? Dan reikt mijn licht weer tot aan de mist.")],
  "Boven in de vuurtoren de grote lens poetsen.",
  [("npc", "Kijk, het licht! Het reikt weer tot aan de mist. Verder zelfs. Dank je, jongen. Of meisje. Of guh."),
   ("npc", "Weet je wat ik ooit zag, met dat licht? Lang geleden. Grauwe zeilen in de mist. Honderd schepen. Ze voeren naar het oosten."),
   ("npc", "Op het voorste schip stond een Mika die naar mijn licht wees. Met één vinger. Toen ging de mist dicht. Ik heb het nooit aan iemand verteld.")],
  munten=7000, loot="zeldzaam", lv=52, na=["z4_hq03_waarvandaan"])

Q("z4_s09_bijen", "zij", "z4_imker", "Zoemende zwervers",
  "Vijf bijtjes van Imker Nectar zijn ontsnapt en zoemen ergens tussen de bloemen van het Bloemeneiland. Breng ze terug naar de korf.",
  [D("geheim", "teller:z4_bijtjes", 5, "Vind 5 ontsnapte bijtjes tussen de bloemen van het Bloemeneiland (gele glinsters)", plek="gebied:z4_bloemeneiland")],
  [("npc", "Zoem zoem... dat betekent: help! Vijf van mijn bijtjes zijn ontsnapt. Ze zijn verliefd geworden op de bloemen."),
   ("npc", "Loop er gewoon naartoe en zeg 'zoem'. Dan volgen ze je terug. Bijtjes zijn heel beleefd.")],
  "Vijf ontsnapte bijtjes tussen de bloemen van het Bloemeneiland.",
  [("npc", "Ze zijn er weer! Alle vijf! Hoor je hoe blij ze zoemen? Hier, een potje roze honing. Van hen, voor jou.")],
  munten=6000, lv=51, na=_NA2)

Q("z4_s10_kransen", "zij", "z4_madelief", "Kransen voor het feest",
  "Kransenmaakster Madelief vlecht bloemenkransen voor het knuffelfeest. Ze heeft nog zes bloemen nodig uit de bloemenvelden.",
  [D("geheim", "teller:z4_bloemen", 6, "Pluk 6 bloemen in de bloemenvelden van het Bloemeneiland (roze glinsters)", plek="gebied:z4_bloemeneiland")],
  [("npc", "Twaalf knuffels gaan naar huis. Dan verdienen ze twaalf kransen. Ik heb er al zes."),
   ("npc", "Pluk je nog zes bloemen voor me? De mooiste staan in de velden aan de zuidkant. Niet de bijtjes plukken, hè.")],
  "Zes bloemen uit de velden van het Bloemeneiland.",
  [("npc", "Zes prachtige bloemen. Even vlechten... kijk, een krans. Die is voor jou. Jij bent ook een beetje een knuffel.")],
  munten=5000, items=[(item("krans"), 1)], lv=51, na=_NA2)

Q("z4_s11_schildpad", "zij", "z4_schelpie", "Een snack voor Ouwe Dobber",
  "Kluizenaar Schelpie woont op de slapende Oma Dobber; haar man Ouwe Dobber zwemt rondjes om haar heen. Ouwe Dobber heeft honger: hij lust alleen het lekkerste zeewier, "
  "en dat hebben de grauwe wezens aan de kust.",
  [D("verzamel", item("zeewier"), 5, "Verzamel 5 lekker zeewier van de grauwe wezens aan De Lampionkust (vrolijk ze op)",
     # U-QUESTROBOT: "z4_lampionkust" is no area (Gebieden): it never dropped; now any zone-4 area (pastMob takes a|b|c)
     "gebied:z4_lampionhaven|z4_knuffeltij|z4_strandfeest|z4_bloemeneiland|z4_koraalbaai|z4_vliegereiland|z4_vuurtoreneiland|z4_pierkop|z4_mysterie|z4_wrak|z4_schildpad|z4_zee", 0.4,
     plek="gebied:z4_lampionkust")],
  [("npc", "Ouwe Dobber heeft honger. Hoor je zijn maag? Dat gerommel? Nee, dat is geen onweer."),
   ("npc", "Hij lust alleen het lekkerste zeewier. De grauwe wezens aan de kust hebben het vaak aan zich hangen. Vrolijk ze op, dan laten ze het vallen.")],
  "Vijf lekker zeewier voor Ouwe Dobber.",
  [("npc", "Kijk hem smullen! Hij zwemt meteen een rondje sneller. Nou ja, iets sneller. Een slakkengang sneller."),
   ("npc", "Dank je. Hier, een schelp van zijn schild. Die laat hij elk jaar los. Hij brengt geluk.")],
  munten=7000, loot="zeldzaam", lv=52, na=["z4_hq03_waarvandaan"])

Q("z4_s12_mistbrieven", "zij", "z4_nevel", "Briefjes uit de mist",
  "Verdwaalde Nevel heeft op zijn vlucht drie briefjes verloren op het Mysterie-eiland. Ze vertellen over het grauwe land. Hij wil ze terug, "
  "om nooit te vergeten waar hij vandaan komt.",
  [D("geheim", "teller:z4_mistbrieven", 3, "Vind 3 verloren briefjes van Verdwaalde Nevel op het Mysterie-eiland, in de mist", plek="gebied:z4_mysterie")],
  [("npc", "Ik schreef alles op, onderweg. Wat ik zag. Zodat iemand het zou weten. Maar de briefjes waaiden weg in de mist."),
   ("npc", "Drie briefjes, ergens op dit eiland. Wil je ze zoeken? Je mag ze lezen. Eigenlijk moet je ze lezen.")],
  "Drie briefjes van Nevel op het Mysterie-eiland.",
  [("npc", "Alle drie. Je hebt ze gelezen, hè? Dan weet je het nu ook. Het grauwe land. De weg. De Schaal."),
   ("npc", "Dank je. Nu ben ik niet meer de enige die het weet. Dat voelt... lichter. Bijna roze.")],
  munten=8000, loot="zeldzaam", lv=53, na=["z4_hq04_mist"])

Q("z4_s13_soep", "zij", "z4_soepguh", "Soep voor iedereen",
  "Soepguh Lepeltje heeft vier kommen warme soep klaarstaan voor de guhs uit het oosten. Breng ze rond op het Knuffeltij-strand.",
  [D("praat", "z4_grijsje", 1, "Breng een kom soep naar Grijsje, op een dekentje bij het opvangcentrum", plek="npc:z4_grijsje"),
   D("praat", "z4_opa_dun", 1, "Breng een kom soep naar Opa Dun, op het bankje bij het soepkraampje", plek="npc:z4_opa_dun"),
   D("praat", "z4_vaaltje", 1, "Breng een kom soep naar Vaaltje, bij de droogwaslijn", plek="npc:z4_vaaltje"),
   D("praat", "z4_mama_pluis", 1, "Breng een kom soep naar Mama Pluis, bij de ingang van het opvangcentrum", plek="npc:z4_mama_pluis")],
  [("npc", "Vier kommen, vier guhs. Ze zijn nog te verlegen om zelf te komen. Breng jij ze rond?"),
   ("npc", "Grijsje, Opa Dun, Vaaltje en Mama Pluis. Zeg erbij dat er altijd meer is. Altijd.")],
  "Vier kommen soep rondbrengen op het Knuffeltij-strand.",
  [("npc", "Ze hebben alles opgegeten! Zelfs Opa Dun. Hij at nooit alles op. Dat mocht daar niet, zei hij."),
   ("npc", "Hier, een kom voor jou. Je hebt hem verdiend. Er is altijd meer.")],
  munten=5000, items=[(item("soepkom"), 1)], lv=50, na=_NA2, start_items=[(item("soepkom"), 4)],
  praat={
      "z4_grijsje": [("npc", "Soep! Voor mij? Met een lepel? Dank je wel... het is warm. Zo warm.")],
      "z4_opa_dun": [("npc", "Een hele kom. Voor mij alleen. Ik... ik weet niet wat ik moet zeggen. Dank je, ridder.")],
      "z4_vaaltje": [("npc", "Het ruikt naar wortels en zon. Waar ik vandaan kom, ruikt soep naar niks.")],
      "z4_mama_pluis": [("npc", "Zie je mijn oortjes? Ze worden roze van de soep. Echt waar. Kijk maar.")]})

Q("z4_s14_lampionnen", "zij", "z4_lampionmaker", "Lampions ophangen",
  "Lampionmaakster Gloeiwiek wil vijf nieuwe lampions ophangen in Lampionhaven, zodat de knuffels de weg naar huis zien. Hang ze op bij de "
  "lege haken.",
  [D("geheim", "teller:z4_lampionhaken", 5, "Hang 5 lampions op bij de lege haken in Lampionhaven (gouden glinsters bij de steigers, de kade en het plein)",
     plek="gebied:z4_lampionhaven")],
  [("npc", "Vijf haken zijn nog leeg. Bij de steigers, op de kade en op het plein. Vijf lege haken is vijf donkere plekjes."),
   ("npc", "Hier, vijf lampions. Loop naar een lege haak en ze hangen zichzelf op. Ze zijn heel behulpzaam.")],
  "Vijf lampions ophangen bij de lege haken in Lampionhaven.",
  [("npc", "Kijk eens hoe het straalt! Nu ziet iedereen de weg. Knuffels, bootjes, en walvissen ook.")],
  munten=5000, items=[(item("lampion"), 2)], lv=50, na=_NA1)

Q("z4_s15_strandavond", "zij", "z4_feestguh", "Feest bij het kampvuur",
  "Feestguh Confetti geeft een feestje bij het kampvuur op het Strandfeesteiland. Dans mee: blijf even bij het kampvuur staan en laat je "
  "voeten het werk doen.",
  [D("meld", "z4:dans", 1, "Dans mee bij het kampvuur op het Strandfeesteiland (blijf een tijdje bij het vuur staan)", plek="poi:z4_strandfeest_kampvuur")],
  [("npc", "Een feestje! Nu! Want feestjes wachten niet. Nou ja, soms wel. Maar dit niet."),
   ("npc", "Kom bij het kampvuur staan. Je voeten gaan vanzelf. Dat doet het vuur. Het is een dansvuur.")],
  "Even dansen bij het kampvuur op het Strandfeesteiland.",
  [("npc", "Wat een dansje! Je deed de golf, de krab én de walvis. Die laatste ken ik niet eens!"),
   ("npc", "Hier, wat marshmallows. Geroosterd. Een beetje zwart. Dat is de lekkerste kant.")],
  munten=5000, items=[(item("limonade"), 1)], lv=50, na=_NA2)

Q("z4_s16_flessenpost", "zij", "z4_grijsje", "Een briefje naar oma",
  "Grijsje heeft een briefje geschreven voor haar oma, die nog in het oosten woont. Gooi de flessenpost in de Kringstroom aan de rand van de "
  "zee: die draait langs alle kusten, ook die in het oosten.",
  [D("gebied", "poi:z4_kringstroom", 1, "Vaar met de flessenpost naar de schuimstrepen van de Kringstroom, ten zuiden van het Knuffeltij-strand",
     plek="poi:z4_kringstroom")],
  [("npc", "Ik heb een briefje geschreven. Voor oma. Ze woont nog daar. In het grauwe land."),
   ("npc", "Er staat in: 'Lieve oma, hier is alles roze. Ik heb soep gehad. Kom ook.' Gooi je hem in de Kringstroom? Die gaat overal heen. Zegt Zachtje.")],
  "De flessenpost in de Kringstroom gooien, ten zuiden van het Knuffeltij-strand.",
  [("npc", "Hij drijft! Hij drijft naar het oosten! Denk je dat oma hem krijgt?"),
   ("jij", "De zee bracht jouw knuffel naar hier. Dan brengt ze dit briefje ook naar oma."),
   ("npc", "...Dat is het liefste wat iemand ooit tegen me gezegd heeft. Hier, ik heb een tekening voor je gemaakt. Dat ben jij. Op een walvis.")],
  munten=6000, loot="zeldzaam", lv=52, na=["z4_hq05_vliegervanger"], start_items=[(item("flesje"), 1)])


# ===== quest xp: a main step = 45 % of a level, a side quest = 18 % (the curve of kern Levels.xpVoor, as Z3Xp) ====================
def z4xp(lv, hoofd):
    l = max(1, min(99, lv))
    stap = max(1, round(40 + 25 * l ** 1.6))
    return max(1, round(stap * (0.45 if hoofd else 0.18)))


for _q in QUESTS:
    _q["beloning"]["xp"] = z4xp(_q["lv"], _q["soort"] != "zij")

# ---------------------------------------------------------------------------------------------------------------------------
# Secrets (NOT the Knuffeltij: that is story only): 8 hidden knuffels, 6 flessenpost notes (lore about the Keizer), 4 treasures,
# 4 viewpoints (the Boom behind you, Hoogvads ahead). Spots: Z4VerhaalPlekken.KNUFFEL/FLESSENPOST/SCHAT/UITZICHT.
# ---------------------------------------------------------------------------------------------------------------------------
KNUFFELS = {
    "knuffel_1": "Ankertje Junior", "knuffel_2": "Lampionnetje", "knuffel_3": "Zandkasteeltje", "knuffel_4": "Koraaltje",
    "knuffel_5": "Wimpeltje", "knuffel_6": "Bijtje Bob", "knuffel_7": "Schildpadje", "knuffel_8": "Mistig Muisje",
}
FLESSENPOST = {
    "fles_1": ("Een briefje zonder naam", "'Ze zeggen dat de Keizer nooit slaapt. Hij zit op zijn troon en wacht tot de Schaal vol is. Ze is nog nooit vol geweest.'"),
    "fles_2": ("Een kinderbriefje", "'Lieve wie dit vindt: mogen knuffels bij jullie wel? Hier niet. Ze zeggen dat het zonde is van de vads.'"),
    "fles_3": ("Een verkreukeld bevel", "'Bevel van de Ringvinger: alle lampions van de westkust tellen. Twee keer. Wat warm is, gaat naar het oosten.'"),
    "fles_4": ("Een recept", "'Soep zonder vads: water, een steen, en stilte. Roeren tot je vergeet hoe soep smaakte.' Iemand heeft er een hartje onder getekend."),
    "fles_5": ("Een oud liedje", "'Als je je knabbel deelt, komt de Keizer je halen...' Daaronder, in een ander handschrift: 'Ik deel toch. Lekker.'"),
    "fles_6": ("Een kaartje van de oostkust", "Een getekend kaartje: een lange weg naar het oosten, een burcht als een kom, en een klein kroontje erboven. Er staat bij: 'Nooitgenoeg.'"),
}
SCHATTEN = {
    "duintop": ("Op de Lampionduin", "Onder het helmgras op de top van de Lampionduin ligt een kist. Met zand erin. En onder het zand: schatten."),
    "lagune": ("In de lagune", "Op de bodem van de lagune, tussen het roze koraal, staat een kistje met een slot in de vorm van een schelp."),
    "vliegertop": ("Aan de wedstrijdpaal", "Aan de wedstrijdpaal hangt een kistje aan een vlieger. Het waait al jaren. Niemand dacht eraan om hem binnen te halen."),
    "mist": ("In de mist", "Achter de grauwe poort, half in de mist, staat een kist die iemand achterliet op zijn vlucht. Er zit nog iets warms in."),
}
UITZICHTEN = {
    "duin": ("Uitzicht op de Lampionduin", "Achter je staat de Boom, zo groot als een berg. Voor je glinsteren de eilandjes. En boven de zee zweeft Hoogvads."),
    "vlieger": ("Uitzicht vanaf het Vliegereiland", "Hier waait het zo hard dat je oortjes flapperen. Je ziet alle eilandjes tegelijk, als kraaltjes aan een ketting."),
    "vuurtoren": ("Uitzicht vanuit de vuurtoren", "Ver in het zuidoosten hangt de Grauwe Mistbank als een muur. Daarachter... begint iets grauws."),
    "pierkop": ("Uitzicht vanaf de Pierkop", "Boven je hoofd zwemt Moeder Wolkwal door de lucht. Op haar rug: torens, bruggetjes en lichtjes. Hoogvads."),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Gids pages (questlog J, tab Gids: ids z4_* in QuestlogScherm.GIDS), tips, other texts
# ---------------------------------------------------------------------------------------------------------------------------
GIDS = {
    "z4_lampionkust": ("Zone 4: De Lampionkust", "Na De Schemerwortel en vanaf level 50 vlieg je naar Lampionhaven, een havenstadje op palen aan "
                       "een roze zee. De Lampionkust is een slinger van eilandjes (lv 50-56): het Knuffeltij-strand, het Strandfeesteiland, het "
                       "Bloemeneiland, de Koraalbaai, het Vliegereiland, het Vuurtoreneiland, de reuzenschildpad, het Mysterie-eiland in de mist "
                       "en de Pierkop met de Opstijgpier. Boven de zee zweeft Hoogvads op Moeder Wolkwal. Het is hier rustig: veel te doen, "
                       "weinig te vechten."),
    "z4_zeilboot": ("Je zeilbootje", "Bij Botenguh Zeiltje aan de noordelijke steiger van Lampionhaven krijg je je eigen guh-zeilbootje. Daarmee vaar je "
                    "vrij tussen de eilandjes. Aan de rand van de zee draait de Kringstroom je zachtjes terug."),
    "z4_knuffeltij": ("De Grote Knuffeltij", "Eens per jaar spoelen er knuffels uit de hele wereld aan op het Knuffeltij-strand. Dit jaar zijn ze "
                      "grauw: ze komen uit het keizerrijk. Ga er gewoon naartoe: dan vrolijk je ze op en vertellen ze hun verhaal."),
    "z4_reizen": ("Reizen aan de kust", "Luchthavenguh Windvaan in Lampionhaven heeft de reiskaart: terug naar de Boom en de andere eilanden. Tussen "
                  "de eilandjes vaar je met je eigen zeilbootje. Het luchtschip naar Hoogvads vertrekt alleen van de Opstijgpier, helemaal in het oosten."),
}
TIPS = {
    "z4_opvrolijken": ("Grauwe wezens", "De wezens aan De Lampionkust zijn grauw en verdrietig. Versla ze en ze ploffen roze: dan zijn ze weer blij. "
                                        "Guhs doe je nooit pijn."),
    "z4_knuffel": ("Een grauwe knuffel!", "Loop naar de grauwe pluisjes toe: dan vrolijk je de knuffel op en vertelt hij zijn verhaal."),
    "z4_boot": ("Je eigen bootje!", "Je zeilbootje ligt aan de steiger. Vaar ermee naar alle eilandjes. Het Knuffeltij-strand ligt ten zuidoosten van Lampionhaven."),
}
OVERIG = {
    # roles + travel (the airship line itself is t-luchtschip's; this is the luxe travel-map place "lampionkust")
    P + "optie.rol.z4_zeilboot": "Mijn zeilbootje, graag!",
    P + "optie.rol.z4_vissoep": "Een kom vissoep, graag!",
    P + "optie.rol.z4_limonade": "Een glaasje limonade, graag!",
    P + "optie.rol.z4_soep": "Een kommetje soep, graag!",
    P + "optie.rol.werkplaats": "Naar de werkplaats",
    P + "optie.rol.luchtschip_z4": "Waar vliegen we heen?",
    P + "optie.vlieg.lampionkust": "Naar Lampionhaven (De Lampionkust)",
    P + "optie.vlieg.lampionkust.slot": "Naar Lampionhaven (nog op slot)",
    P + "reis.slot.lampionkust_verhaal": "De Lampionkust? Eerst moet De Schemerwortel weer bloeien. Help de draak, ridder!",
    P + "reis.slot.lampionkust_verhaal.kort": "Eerst De Schemerwortel afmaken.",
    P + "reis.slot.lampionkust_level": "De Lampionkust is voor avonturiers vanaf level 50. Jij bent level %s. Nog even oefenen!",
    P + "reis.slot.lampionkust_level.kort": "Vanaf level 50 (jij: %s).",
    P + "reis.kies.z4_luchthaven": "Terug naar de Boom of verder weg? Kies hieronder waarheen of bekijk de reiskaart.",
    P + "reiskaart.plek.lampionkust": "De Lampionkust",
    Z + "rol.zeilboot": "Je zeilbootje ligt klaar aan de steiger! (Als het bootje nog niet in deze versie zit: het komt eraan.)",
    Z + "rol.zeilboot.nog": "Het zeilbootje wordt nog gebouwd. Tot die tijd: zwem maar, of steek over via de zandbankjes. Ik let op je.",
    Z + "rol.vissoep": "Alsjeblieft, een kom vissoep. Pas op, heet. Met een garnaal erin. Die zwaait.",
    Z + "rol.limonade": "Alsjeblieft, een glaasje schelpenlimonade. Met een rietje van zeegras.",
    Z + "rol.soep": "Alsjeblieft, een kommetje soep. Er is altijd meer.",
    Z + "rol.al": "Je hebt er net al eentje gehad! Eerst opmaken, dan krijg je er nog een.",
    # the airship / flight (fallback when t-luchtschip's line is not in the tree)
    Z + "scene.vlucht.titel": "De Lampionkust",
    Z + "scene.vlucht.onder": "Lampionhaven",
    Z + "scene.opening.titel": "De Lampionkust",
    Z + "scene.opening.onder": "Een roze zee vol eilandjes",
    Z + "scene.opening.wolkenval": "Het luchtschip duikt door de Wolkenval. Onder de wolken: een roze zee, en lampions zo ver je kunt kijken.",
    Z + "scene.opening.eilanden": "Eilandjes als kraaltjes aan een ketting. Vliegers. Parasols. Een vuurtoren met oortjes.",
    Z + "scene.opening.walvis": "En boven de zee, in het oosten, zweeft een stad. Op de rug van een walvis.",
    Z + "scene.opening.knuffels": "Op een strandje in het zuidoosten liggen grauwe vlekjes in de branding. Knuffels. Grauwe knuffels.",
    Z + "aankomst": "Welkom in Lampionhaven! Havenmeester Ankertje wacht op de kade, bij de middelste steiger.",
    # the knuffels (story only)
    Z + "knuffel.verteller": "Een grauwe knuffel",
    Z + "knuffel.opgevrolijkt": "De knuffel wordt een beetje roze. (%s van de %s knuffels van de Knuffeltij)",
    Z + "knuffel.alle": "Alle twaalf knuffels van de Knuffeltij zijn weer roze!",
    Z + "groot.titel": "Een herinnering uit het oosten",
    Z + "groot.roze": "De grote knuffel zucht, en wordt langzaam roze. Hij voelt zich lichter nu zijn verhaal verteld is.",
    # the mist flash (z4_hq04)
    Z + "scene.mist.titel": "Achter de mist",
    Z + "scene.mist.onder": "Het grauwe oosten",
    Z + "scene.mist.0": "Door de poort zie je... niks. Alleen mist. En dan, heel zacht, het geluid van karrenwielen.",
    Z + "scene.mist.1": "Een lange grauwe weg. Karren vol knuffels en lampions, getrokken door Mika's met zakken. Ze rijden allemaal naar het oosten.",
    Z + "scene.mist.2": "Langs de weg zoemt een grauwe buis. Hij zuigt. Waar hij langs loopt, verliezen de bloemen hun kleur.",
    Z + "scene.mist.3": "Ver weg, aan de horizon, staat een burcht als een kom. Daarboven hangt een maan die geen maan is. Hij kijkt.",
    Z + "scene.mist.4": "De mist trekt weer dicht. Je oortjes zijn koud. Achter je hoor je een stem: \"...Ben jij een Mika?\"",
    # the Vliegervanger (fallback when no boss is bound)
    Z + "wrak.naam": "Kootje de Vliegervanger",
    Z + "wrak.0": "Hé! Wie stoort Kootje de Vliegervanger in zijn eigen wrak? Dit is privé-wrak!",
    Z + "wrak.1": "Die knuffels? Die heb ik... gevonden. Aan een waslijn. Ze hingen daar maar. Dat is bijna weggooien.",
    Z + "wrak.2": "De Ringvinger betaalt een bonus voor elke knuffel. Twee knuffels is twee bonussen. Ik ben heel goed in rekenen.",
    Z + "wrak.3": "Kootje hijst zijn grauwe oorlogsvlieger, maar het touw zit vast aan zijn eigen staart. Hij vliegt een rondje, en plonst in de zee.",
    Z + "wrak.4": "\"Dit komt in mijn rapport!\" Hij zwemt weg, zo snel als een Mika zwemt. Dat is niet snel. In het ruim liggen de knuffels.",
    # the Zeeslurf (fallback when no boss is bound)
    Z + "zeeslurf.naam": "De Grauwe Zeeslurf",
    Z + "zeeslurf.0": "In het diepste grotje ligt een reuzenzeekoe met een lange slurf. Hij blaast grauwe bellen. Ze knappen zonder geluid.",
    Z + "zeeslurf.1": "Je zwemt dichterbij en aait zijn slurf. Hij schrikt. Dan... knippert hij. Eén roze bel. Twee. Tien!",
    Z + "zeeslurf.2": "De Zeeslurf zingt een laag, blij liedje. De visjes komen terug en spelen met zijn bellen.",
    # the party + Moeder Wolkwal (z4_hq06)
    Z + "scene.feest.titel": "Het terug-naar-huis-feest",
    Z + "scene.feest.onder": "Twaalf knuffels gaan naar huis",
    Z + "scene.feest.0": "De lampions gaan aan. Driehonderd stuks, roze, geel en mint. En eentje lila.",
    Z + "scene.feest.1": "Twaalf vliegers met oortjes stijgen op, en aan elke vlieger hangt een knuffel die naar beneden zwaait.",
    Z + "scene.feest.2": "Grijsje ziet een knuffel aan een vlieger. \"MUIS!\" Ze rent erheen. Muis is roze. Grijsje ook, een beetje.",
    Z + "scene.feest.3": "Iedereen danst. Opa Dun ook. Hij lacht. Om niks. Gewoon lachen.",
    Z + "scene.feest.4": "Opvangmoeder Zachtje tikt op haar limonadeglas: \"Wie hier aanspoelt, is thuis. Knuffel of guh. Welkom thuis.\"",
    Z + "scene.wolkwal.titel": "Moeder Wolkwal",
    Z + "scene.wolkwal.onder": "Ze komt naar beneden",
    Z + "scene.wolkwal.0": "Het wordt stil. Over het water schuift een enorme schaduw.",
    Z + "scene.wolkwal.1": "Moeder Wolkwal zakt door de wolken naar beneden. Laag. Heel laag. Op haar rug flonkeren de lichtjes van Hoogvads.",
    Z + "scene.wolkwal.2": "Ze zingt. Een lang, diep walvislied dat je in je buik voelt. De lampions wiegen mee.",
    Z + "scene.wolkwal.3": "Haar oog is groot en vriendelijk. En moe. Achter haar staart wappert een dun, grauw sliertje.",
    # the cliffhanger (z4_hq07): the first airship up to Hoogvads
    Z + "scene.cliff.titel": "Naar Hoogvads",
    Z + "scene.cliff.onder": "Het eerste luchtschip stijgt op",
    Z + "scene.cliff.0": "Het luchtschip maakt los van de Opstijgpier. Langzaam stijgt het op, langs de kop van Moeder Wolkwal.",
    Z + "scene.cliff.1": "Haar oog draait mee. Ze kijkt je aan. Zo groot als een huis, zo zacht als een deken.",
    Z + "scene.cliff.2": "Boven de wolken: torens met vlaggetjes, bruggetjes, een kathedraal van wolkensteen. Hoogvads.",
    Z + "scene.cliff.3": "Beneden op de pier kijkt een guh in een wit-met-gouden cape het schip na. Uit zijn mouw dwarrelt een grauw pluisje.",
    Z + "scene.cliff.4": "Niemand ziet het. Behalve jij.",
    Z + "scene.cliff.vervolg": "Wordt vervolgd... Hoogvads, de Zwevende Stad.",
    Z + "zone4.klaar": "De Knuffeltij is voorbij en alle knuffels zijn roze! Het luchtschip naar Hoogvads vaart vanaf nu vanaf de Opstijgpier.",
    # mini games and small moments
    Z + "zeilboot.klaar": "Je hebt een eigen guh-zeilbootje! Het zeil knipoogt naar je.",
    Z + "volley.naam": "Volleybalguh Smash",
    Z + "volley.serve": "Smash gooit de bal op... (%s - %s) Waar slaat hij heen?",
    Z + "volley.hint.0": "Smash kijkt naar rechts... maar zijn linkeroortje wiebelt.",
    Z + "volley.hint.1": "Smash kijkt recht voor zich uit en fluit een liedje. Heel onschuldig.",
    Z + "volley.hint.2": "Smash kijkt naar links. Heel opvallend naar links. Te opvallend. En zijn rechteroortje wiebelt.",
    Z + "volley.uitleg": "(Tip: let op de oortjes van Smash. Oortjes liegen nooit.)",
    Z + "volley.links": "Links!", Z + "volley.midden": "Midden!", Z + "volley.rechts": "Rechts!",
    Z + "volley.punt": "Raak! Je duikt in het zand en slaat de bal terug. Punt voor jou!",
    Z + "volley.mis": "Mis! De bal ploft in het zand. Smash juicht. Tegen zichzelf.",
    Z + "volley.gewonnen": "Gewonnen! Drie punten! Smash valt achterover in het zand van verbazing.",
    Z + "vlieger.start": "Je laat je guh-vlieger op bij de wedstrijdpaal. Hij schiet omhoog! Houd hem in de wind...",
    Z + "vlieger.windstoot": "Een windstoot! Je vlieger maakt een looping. Windmeester Zwiep klapt. (%s seconden)",
    Z + "vlieger.weg": "Je liep bij de wedstrijdpaal weg. De vlieger zakt. Begin maar opnieuw!",
    Z + "vlieger.gewonnen": "Een halve minuut! Je vlieger danst hoog boven het eiland. Je hebt de wedstrijd gewonnen!",
    Z + "vlieger.geen": "Je hebt geen guh-vlieger bij je. Vliegermaker Wimpel helpt je er eentje te bouwen.",
    Z + "lens.bezig": "Je poetst de grote lens. Zout eraf, glans erop...",
    Z + "lens.klaar": "De lens glanst! Het licht van de vuurtoren reikt weer tot aan de mist.",
    Z + "dans.start": "Het kampvuur knettert. Je voeten beginnen vanzelf te bewegen...",
    Z + "dans.klaar": "Je danst de golf, de krab en de walvis! Iedereen rond het kampvuur klapt.",
    Z + "flessenpost.weg": "Je gooit de flessenpost in de Kringstroom. Hij dobbert weg, naar het oosten. Ga maar, flesje. Naar oma.",
    Z + "vis.gevangen": "Een roze vis! (%s van de %s voor Visser Dobber)",
    Z + "vind.vliegerspul": "Gevonden! Weer een stukje voor je vlieger. (%s/%s)",
    Z + "vind.parels": "Een parel! Hij glimt roze. (%s/%s)",
    Z + "vind.schelpen": "Een mooi schelpje! (%s/%s)",
    Z + "vind.bijtjes": "Zoem! Een bijtje vliegt met je mee terug. (%s/%s)",
    Z + "vind.bloemen": "Een prachtige bloem voor de kransen. (%s/%s)",
    Z + "vind.lampionhaken": "Je hangt een lampion op. Hij gaat vanzelf aan! (%s/%s)",
    Z + "vind.mistbrieven": "Een briefje van Nevel, nat van de mist. (%s/%s)",
    Z + "mistbrief.naam": "Een briefje van Nevel",
    Z + "mistbrief.mistbrief_1": "'Dag 1. We lopen 's nachts. Overdag staan de Mika's bij de buizen. Mama zegt: niet kijken, niet praten, gewoon lopen.'",
    Z + "mistbrief.mistbrief_2": "'Dag 9. We zagen een dorp aan een buis. Alles grauw. Een guh zat op een stoep en keek naar niks. Hij zwaaide niet terug.'",
    Z + "mistbrief.mistbrief_3": "'Dag 20. De zee! Knuffels in het water, ze dreven allemaal dezelfde kant op. Naar het westen. Wij gaan mee.'",
    # secrets
    Z + "knuffel.gevonden": "Knuffel gevonden: %s! (%s/%s)",
    Z + "fles.naam": "Flessenpost",
    Z + "fles.teller": "Flessenpost gevonden: %s van de %s.",
    Z + "fles.alle": "Alle flessenpost gelezen. Samen vertellen de briefjes over een Keizer die nooit genoeg heeft, ver in het oosten.",
    Z + "schat.titel": "Schat gevonden!",
    Z + "uitzicht.naam": "Uitzichtpunt",
    Z + "uitzicht.teller": "Uitzichtpunten: %s van de %s.",
    Z + "baas.zeeslurf": "De Grauwe Zeeslurf", Z + "baas.vliegervanger": "Kootje de Vliegervanger",
    # the Verkenningsboek page
    "vadsrijk.voltooiing.zone.zone4": "De Lampionkust",
    "vadsrijk.voltooiing.cat.uitzicht": "Uitzichtpunten",
    "vadsrijk.voltooiing.cat.flessenpost": "Flessenpost",
    "gui.guhs.titels.naam.vr_kenner_van_de_lampionkust": "Kenner van de Lampionkust",
    "gui.guhs.titels.hint.vr_kenner_van_de_lampionkust": "Verken De Lampionkust helemaal (Verkenningsboek, toets B).",
}


# ---------------------------------------------------------------------------------------------------------------------------
# building
# ---------------------------------------------------------------------------------------------------------------------------
def _java_ids():
    """NPC/POI/area ids of Z4VerhaalPlekken.java (+ the other zones' NPCs zone-4 quests use, such as Wasja and Zoemzeil)."""
    with open(os.path.join(JAVA, "Z4VerhaalPlekken.java"), encoding="utf-8") as f:
        src = f.read()
    npc = set(re.findall(r'\bnpc\("(z4_[a-z_0-9]+)"', src))
    poi = set(re.findall(r'\bpoi\("(z4_[a-z_0-9]+)"', src))
    gebied = set(re.findall(r'GEBIED_DOEL\.put\("([a-z_0-9]+)"', src))
    baas = set(re.findall(r'BAAS\.put\("([a-z_0-9]+)"', src))
    andere = {"z3_wasja", "z3_kapitein", "luchtschipper", "kapitein", "z2_kapitein"}
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
    assert set(NPCS) == npc_ids, f"NPC ids differ from Z4VerhaalPlekken: {set(NPCS) ^ npc_ids}"
    for id, x in PLEK_POI.items():
        t[P + "plek.poi." + id] = x
    assert set(PLEK_POI) == poi_ids, f"POI ids differ from Z4VerhaalPlekken: {set(PLEK_POI) ^ poi_ids}"
    for id, x in PLEK_GEBIED.items():
        t[P + "plek.gebied." + id] = x
    try:   # T-INT: t-z4wereld owns the area names; only fill ids it does not name
        from features import vadsrijk_t_z4wereld as _z4w
        _eigen = set(_z4w.GEBIEDEN)
    except Exception:  # noqa: BLE001
        _eigen = set()
    for id, x in GEBIED_NAMEN.items():
        if id not in _eigen:
            t["vadsrijk.kern.gebied." + id] = x
    for id, x in PLEK_KAMPVUUR.items():
        t[P + "plek.kampvuur." + id] = x
    for id, x in PLEK_BAAS.items():
        t[P + "plek.baas." + id] = x
    assert gebied_ids <= set(PLEK_GEBIED) and baas_ids <= set(PLEK_BAAS), "area/boss without a plek text"
    for id, (naam, _tex) in QUEST_ITEMS.items():
        t[f"item.guhs.vr_z4_npc_{id}"] = naam
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
    for id, (naam, tekst) in {**TIJKNUFFELS, **WRAKKNUFFELS}.items():
        t[Z + f"tijknuffel.{id}.naam"] = naam
        t[Z + f"tijknuffel.{id}.verhaal"] = tekst
    for id, (naam, regels) in GROTE_KNUFFELS.items():
        assert len(regels) == 3, id
        t[Z + f"tijknuffel.{id}.naam"] = naam
        for i, r in enumerate(regels):
            t[Z + f"tijknuffel.{id}.verhaal.{i}"] = r
    for id, naam in KNUFFELS.items():
        t[Z + "knuffel." + id] = naam
    for id, (titel, tekst) in FLESSENPOST.items():
        t[Z + "fles." + id] = tekst
        t[Z + "fles.titel." + id] = titel
    for id, (naam, tekst) in SCHATTEN.items():
        t[Z + "schat." + id] = tekst
        t[Z + "schat.naam." + id] = naam
    for id, (naam, tekst) in UITZICHTEN.items():
        t[Z + "uitzicht." + id] = tekst
        t[Z + "uitzicht.titel." + id] = naam
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
LV_CAP = 60   # (T-INT: kern cap raised to 60)


def quests_json():
    npc_ids, poi_ids, gebied_ids, baas_ids, andere = _java_ids()
    alle_npc = npc_ids | andere
    ids = [q["id"] for q in QUESTS]
    assert len(ids) == len(set(ids)), "duplicate quest id"
    assert all(i.startswith("z4_") for i in ids), "zone-4 quest ids start with z4_"
    uit = []
    for q in QUESTS:
        for wie in (q["gever"], q["inleveren"]):
            assert wie in alle_npc, f"{q['id']}: unknown NPC {wie}"
        for na in q["na"]:
            assert na in ids or not na.startswith("z4_"), f"{q['id']}: unknown previous quest {na}"
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
            if d["soort"] == "verzamel":
                assert d["doel"][len("guhs:vr_z4_npc_"):] in QUEST_ITEMS, f"{q['id']}: unknown quest item {d['doel']}"
        for groep in (q["aanbod"], q["klaar"], *q["praat"].values()):
            for wie, _x in groep:
                assert wie in ("npc", "jij", "verteller") or wie in alle_npc, f"{q['id']}: unknown speaker {wie}"
        # (kern Levels.MAX is 50 in this tree: a zone-4 quest asks at most lv 50, else it could never be offered; the design level
        # (50-55) still sets its xp. Raise LV_CAP when the level cap goes up.)
        o = dict(id=q["id"], soort=q["soort"], gever=q["gever"], inleveren=q["inleveren"], lv=min(q["lv"], LV_CAP), na=q["na"],
                 doelen=[dict(soort=d["soort"], doel=d["doel"], n=d["n"], bron=d["bron"], kans=d["kans"], plek=d["plek"])
                         for d in q["doelen"]],
                 beloning=q["beloning"], aanbod=[w for w, _x in q["aanbod"]], klaar=[w for w, _x in q["klaar"]],
                 praat={npc: [w for w, _x in r] for npc, r in q["praat"].items()}, actie_aanname=q["actie_aanname"],
                 actie_klaar=q["actie_klaar"], start_items=q["start_items"], auto=q["auto"])
        if q["klasse"]:
            o["klasse"] = q["klasse"]
        uit.append(o)
    hoofd = [q for q in QUESTS if q["soort"] == "hoofd"]
    assert len(hoofd) == 7, len(hoofd)
    assert sum(q["soort"] == "zij" for q in QUESTS) >= 15
    return {"_": "generated by tools/features/vadsrijk_t_z4verhaal.py; do not edit", "quests": uit}


def advancements():
    namen = ([f"vr_{q['id']}" for q in QUESTS] + ["vr_z4_knuffels_alle", "vr_z4_flessenpost_alle", "vr_z4_knuffeltij_alle", "vr_z4_uitzichten_alle"]
             + [f"vr_z4_schat_{s}" for s in SCHATTEN])
    return {f"advancement/quest/{n}.json": {"criteria": {"done": {"trigger": "minecraft:impossible"}}} for n in namen}


def _assets(w, a):
    for id, (_naam, tex) in QUEST_ITEMS.items():
        w(os.path.join(a, "items", f"vr_z4_npc_{id}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_z4_npc_{id}"}})
        w(os.path.join(a, "models", "item", f"vr_z4_npc_{id}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})


def build(h):
    h.w(f"{h.D}/vadsrijk/npc/quests_z4.json", quests_json())
    for pad, obj in advancements().items():
        h.w(f"{h.D}/{pad}", obj)
    _assets(h.w, h.A)
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


# ---------------------------------------------------------------------------------------------------------------------------
# FTB Quests: zone 4 joins the chapter "Het Vadsrijk" with its own sections. Nothing is locked.
# ---------------------------------------------------------------------------------------------------------------------------
FTB_CHAPTER = "guhs_vadsrijk"
FTB_PORTRAIT = "guh:mint"
FTB_SECTIES = [
    ("vr_z4_verhaal", "De Lampionkust (zone 4)", "guh:mint", [f"vr_{q['id']}" for q in QUESTS if q["soort"] == "hoofd"]),
    ("vr_z4_kust", "De kust helpen", "guh:golden", [f"vr_{q['id']}" for q in QUESTS if q["soort"] == "zij"]),
    ("vr_z4_geheimen", "Geheimen van De Lampionkust", "guh:normal", ["vr_z4_knuffeltij_alle", "vr_z4_knuffels_alle", "vr_z4_flessenpost_alle",
                                                                      "vr_z4_uitzichten_alle"] + [f"vr_z4_schat_{s}" for s in SCHATTEN]),
]


def ftb(fq):
    q, adv = fq.q, fq.adv
    K = "guhs:kaas_knabbels"
    for d in QUESTS:
        tekst = d["tekst"].replace("Lampionhaven", "&dLampionhaven&r")
        icon = {"hoofd": "minecraft:lantern"}.get(d["soort"], "minecraft:writable_book")
        vorm = "gear" if d["soort"] == "hoofd" else None
        q(f"vr_{d['id']}", d["titel"], tekst + " &7(Vinkt zichzelf af als je hem in het Vadsrijk inlevert.)", icon,
          [fq.met(adv(f"vr_{d['id']}"), d["titel"])], rewards=((K, 8 if d["soort"] == "zij" else 16),), shape=vorm,
          xp=100 if d["soort"] == "hoofd" else 50)
    q("vr_z4_knuffeltij_alle", "De Grote Knuffeltij", "Vrolijk alle &e12 knuffels&r van de Knuffeltij op: zes op het strand, vier grote op de "
      "eilandjes en twee uit het wrak.", "guhs:knuffel_golden", [fq.met(adv("vr_z4_knuffeltij_alle"), "12 knuffels")], rewards=((K, 16),), shape="hexagon")
    q("vr_z4_knuffels_alle", "Acht knuffels aan de kust", "Vind alle &e8 knuffels&r die aan De Lampionkust verstopt zitten.", "guhs:knuffel_golden",
      [fq.met(adv("vr_z4_knuffels_alle"), "8 knuffels")], rewards=((K, 16),), shape="hexagon")
    q("vr_z4_flessenpost_alle", "Flessenpost uit het oosten", "Vind alle &e6 flessenpostbriefjes&r op de stranden. Wie schrijft ze?",
      "minecraft:glass_bottle", [fq.met(adv("vr_z4_flessenpost_alle"), "6 briefjes")], rewards=((K, 16),), shape="hexagon")
    q("vr_z4_uitzichten_alle", "De Boom en de walvis", "Sta op alle &e4 uitzichtpunten&r van De Lampionkust.", "minecraft:spyglass",
      [fq.met(adv("vr_z4_uitzichten_alle"), "4 uitzichten")], rewards=((K, 8),))
    for s, (naam, _t) in SCHATTEN.items():
        q(f"vr_z4_schat_{s}", naam, "Een verborgen schat van De Lampionkust. Hij glinstert goudgeel als je dichtbij bent.",
          "minecraft:chest", [fq.met(adv(f"vr_z4_schat_{s}"), naam)], rewards=((K, 8),))


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
    return (k.startswith(Z) or k.startswith("item.guhs.vr_z4_npc_") or (k.startswith(P) and ".z4_" in k)
            or k.startswith(P + "plek.overal.teller:z4_"))


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "vadsrijk", "npc", "quests_z4.json"), quests_json())
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
    print(f"vadsrijk_t_z4verhaal: {len(QUESTS)} quests ({sum(q['soort'] == 'hoofd' for q in QUESTS)} main, "
          f"{sum(q['soort'] == 'zij' for q in QUESTS)} side), {len(NPCS)} NPCs, {len(t)} texts")


if __name__ == "__main__":
    _direct()
