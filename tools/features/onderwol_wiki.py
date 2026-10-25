"""
1.7.0 "Onder de Wol": what the wiki says about the update (design: MineScapeStory/ontwerp-guhs170-mijnen.md): the stone
layers and ores of the Guhmension, the caves and the six cave biomes, the buildings under the ground, the guh villagers,
the Glinsteraltaar and its spells, the mining helpers, the Mijn-guhhuisje, the FEET slot, the cheerful surface, the five
cave minigames, the back 9 of the golf course, the Barbecuether without lava, bigger terraria and the Rookguh-platform.
Not a feature module (not in FEATURES): the docs step (tools/wiki_bbq2) reads WIKI; the English is in
tools/wiki_bbq2/en_onderwol.py. Every number comes from the code (tools/features/onderwol*.py, grotspellen.py, mijnspellen.py,
golf18.py, voetjes.py, toveren.py, mijnhulp.py, mijnrook.py, guhvillagers.py and feature/... in Java).
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "onder-de-wol": (
            "Onder de Wol: de ondergrond", "block:guhs:viltsteen",
            "Onder de roze wol van de Guhmensie zit nu een hele knuffel-ondergrond: steenlagen die dieper steeds harder worden, "
            "ertsen per laag, grotten met zes eigen grotbiomen, sausmeren, ruim twintig gebouwtjes onder de grond, een "
            "ondergronds villagerstadje en vijf grot-minigames. Alleen in nieuwe chunks.",
            ["steenlagen-en-ertsen", "grotten", "ondergrondse-bouwwerken", "guh-villagers", "glinsteraltaar", "mijnhulpjes",
             "mijn-guhhuisje", "grot-minigames", "vrolijk-oppervlak", "dimensies/guhmension"]),
        "steenlagen-en-ertsen": (
            "Steenlagen en ertsen", "block:guhs:knuffelsteen",
            "Hoe dieper je graaft, hoe steniger het wordt: roze wol, viltsteen, knuffelsteen, guhkristalsteen en helemaal "
            "onderin de onbreekbare knuffelnaad. Elke laag heeft zijn eigen ertsen, en dieper is rijker.",
            ["onder-de-wol", "grotten", "mijnhulpjes", "viltsteen", "knuffelsteen", "guh_kristalsteen", "knuffelnaad", "guhklei",
             "vahoegiet"]),
        "grotten": (
            "Grotten en grotbiomen", "block:guhs:glitterrank",
            "Kronkelgangen, grote kaasgat-zalen en af en toe een gigantische zaal, sausmeren en sausvallen, knuffelhart-geodes en "
            "zes grotbiomen met elk hun eigen planten, licht en vrolijke muziek. Niets vijandigs: alleen lieve grotbewoners.",
            ["onder-de-wol", "steenlagen-en-ertsen", "ondergrondse-bouwwerken", "biomen/glitterspelonk", "biomen/pluisgrot",
             "biomen/slaapgrot", "biomen/knuffelwortelgrot", "biomen/belletjesgrot", "biomen/kristalpegelgrot", "guhs/grotguh",
             "wezens/das_molguh", "wezens/grotglimworm"]),
        "ondergrondse-bouwwerken": (
            "Gebouwen onder de grond", "block:guhs:viltsteen_bakstenen",
            "Van een verlaten guhmijn tot een badhuis vol warme saus: overal onder de Guhmensie staan gebouwtjes in hun eigen "
            "grot, met bordjes, bewoners, buit en soms guhkleding. Het Mijnguhdorp ligt altijd in de buurt van het begin van je wereld.",
            ["onder-de-wol", "grotten", "guh-villagers", "mijn-guhhuisje", "bouwwerken/mijnguhdorp", "bouwwerken/vadsmijnstadje",
             "bouwwerken/mijningang_hutje", "bouwwerken/verlaten_guhmijn", "bouwwerken/oude_guhtempel"]),
        "guh-villagers": (
            "Guh-villagers en het Vadsmijnstadje", "block:guhs:toverguh_boekenstandaard",
            "Echte villagers in guh-stijl: een nieuw schattig model, vier nieuwe beroepen, een eigen roze IJzerguhlem en een "
            "ondergronds stadje vol guh-villagers. En elke villager kun je nu oppakken.",
            ["bouwwerken/vadsmijnstadje", "glinsteraltaar", "ijzerguhlem", "opgepakte_villager", "toverguh_boekenstandaard",
             "guhrad_werkbank", "ertsenkist", "kristalslijpsteen", "bouwwerken/guh_village"]),
        "glinsteraltaar": (
            "Het Glinsteraltaar en de spreukjes", "block:guhs:glinsteraltaar",
            "De guh-versie van de betoveringstafel: een glinsterend kristallen altaar met een zwevend boekje, aangedreven door "
            "knuffelboekenkasten. Met nuttige vanilla-betoveringen en acht eigen guh-spreukjes, en Spreukjesboekjes om ze op "
            "een aambeeld over te zetten.",
            ["glinsteraltaar", "knuffelboekenkast", "spreukjesboekje", "guh-villagers", "toverguh_boekenstandaard"]),
        "mijnhulpjes": (
            "Mijnhulpjes: gereedschap en handigheidjes", "vahoege_vadsknakker",
            "De Vahoege Vadsknakker (vijf gereedschappen in één), super vahoege vads, het Wolkenliftje voor je mijnschacht, de "
            "Pluisladder die vanzelf uitrolt en een Mijnlampje voor je guh.",
            ["vahoege_vadsknakker", "vahoegietstaaf", "super_vads_sjabloon", "wolkenliftje", "pluisladder", "mijnlampje",
             "mijn-guhhuisje", "steenlagen-en-ertsen", "systemen/vahoege-vads"]),
        "mijn-guhhuisje": (
            "Het Mijn-guhhuisje en het Rugzakzadel", "block:guhs:mijn_guhhuisje",
            "Help de Mijnguh en je krijgt een Mijn-guhhuisje: tot vier van je eigen guhs trekken erin en minen langzaam een gebied "
            "onder het huisje leeg. Met een Rugzakzadel draagt een guh eindeloos veel gewone grondblokken mee.",
            ["mijn_guhhuisje", "rugzakzadel", "mijnlampje", "npcs/mijnguh", "bouwwerken/mijnguhdorp", "systemen/guhhuisje", "mijnhulpjes"]),
        "voetjes": (
            "Voetjes: het zevende kledingslot", "kaassauslopers",
            "De kledingkast van je guh heeft een slot voor de voetjes: slofjes, laarsjes, golfschoentjes en twee paar lopers waarmee "
            "je tamme guhs over kaassaus en zelfs over lava lopen.",
            ["pluisslofjes", "regenlaarsjes", "golfschoentjes", "mijnwerkerslaarsjes", "kaassauslopers", "lavalopers", "systemen/kleding",
             "dimensies/barbecuether"]),
        "vrolijk-oppervlak": (
            "Een vrolijker Guhmensie", "block:guhs:hartjeslelie",
            "Ook bovengronds is de Guhmensie mooier geworden: een lappendeken van pastelwol, bloemenweitjes, beekjes van bronnetje "
            "naar bronnetje, roze strandjes, eilandjes, hartjesmeertjes, regenboogheuvels en de zeldzame Bloemenvallei.",
            ["biomen/bloemenvallei", "dimensies/guhmension", "onder-de-wol", "bronnetje", "glinsterriet", "roze_zand", "hartjeslelie",
             "viltkiezel"]),
        "nieuw-in-1-7": (
            "Nieuw in 1.7.0: Onder de Wol", "block:guhs:glinsteraltaar",
            "Wat er nieuw is in Guhs 1.7.0 \"Onder de Wol\": een hele ondergrond onder de Guhmensie, gebouwen, villagers, toveren, "
            "mijnhulpjes, vijf grot-minigames, vadskracht in de gebouwen, de Guhtechniek-beurs, 18 holes golf, voetjes en een vrolijker "
            "oppervlak. De volledige lijst staat in het CHANGELOG van de mod.",
            ["onder-de-wol", "steenlagen-en-ertsen", "grotten", "ondergrondse-bouwwerken", "guh-villagers", "glinsteraltaar",
             "mijnhulpjes", "mijn-guhhuisje", "grot-minigames", "voetjes", "vrolijk-oppervlak", "minigames/golf",
             "bouwwerken/guhtechniek_beurs", "vadslamp", "vadsschakelaar", "bouwwerken/vadsmijnstadje"]),
        "grot-minigames": (
            "De vijf grot-minigames", "schatmuntje",
            "Onder roze hutjes in de Guhmensie liggen vijf spelletjeszalen: Schatgraven, Glimwormvangen, de Lichtjespuzzel, de "
            "Karretjes-wissels in de Wisselgrot en guh-bowlen in de Kegelgrot. Elk met een eigen munt, een winkeltje, highscores en guhkleding.",
            ["minigames/schatgraven", "minigames/glimwormvangen", "minigames/lichtjespuzzel", "minigames/karretjes", "minigames/kegelen",
             "wolkenliftje", "systemen/superkompas"]),
    },
    "npc_home": {"schatgraafguh": "minigames/schatgraven", "snuffelhulpje": "minigames/schatgraven", "glimwormguh": "minigames/glimwormvangen",
                 "lampjesguh": "minigames/lichtjespuzzel", "wisselguh": "minigames/karretjes", "kegelguh": "minigames/kegelen"},
    "entity_home": {"grotglimworm": "biomen/glitterspelonk", "ijzerguhlem": "systemen/guh-villagers", "vangglimworm": "minigames/glimwormvangen"},
    "tekst": [
        # --- what's new (the changelog on the wiki) ---
        ("systemen/nieuw-in-1-7", "Onder de grond",
         "Steenlagen (viltsteen, knuffelsteen, guhkristalsteen en de knuffelnaad), ertsen per laag, guhklei, pigment en Vahoegiet, "
         "grotten met zes grotbiomen en vrolijke grotmuziek, een sauszee, ruim twintig gebouwen onder de grond, het Mijnguhdorp en "
         "het Vadsmijnstadje. Niets vijandigs onder de grond: wel de Grotguh, de Molguh en glimwormpjes. Alleen in nieuwe chunks."),
        ("systemen/nieuw-in-1-7", "Spullen en systemen",
         "Het Glinsteraltaar met knuffelboekenkasten, acht guh-spreukjes en Spreukjesboekjes; guh-villagers met een nieuw model, vier "
         "nieuwe beroepen, de IJzerguhlem en villagers oppakken; de Vahoege Vadsknakker, super vahoege vads, het Wolkenliftje, de "
         "Pluisladder en het Mijnlampje; het Mijn-guhhuisje met de quest Mijnguh-hulpje en het Rugzakzadel; het kledingslot voetjes "
         "met kaassauslopers en lavalopers; grotere terraria; een eigen Rookguh-platform."),
        ("systemen/nieuw-in-1-7", "Spelletjes",
         "Vijf grot-minigames (Schatgraven, Glimwormvangen, de Lichtjespuzzel, Karretjes-wissels en de Kegelgrot) met eigen munten en "
         "winkeltjes, en nieuwe golfbanen met 18 holes, een golfwinkeltje en de golftrofee."),
        ("systemen/nieuw-in-1-7", "Bovengronds",
         "Een vrolijker Guhmensie (lappendeken, bloemenweitjes, beekjes, roze strandjes, eilandjes, hartjesmeertjes, regenboogheuvels "
         "en de Bloemenvallei) en een Barbecuether zonder lava: alles is ongevaarlijke kaasfrituursaus."),
        # --- overview ---
        ("systemen/onder-de-wol", "Waarom naar beneden?",
         "Vroeger was alles onder de Guhmensie roze wol, en minen was dus niet zo heel spannend. Nu vind je onder de wol steen om mee "
         "te bouwen (viltsteen en knuffelsteen in bulk), ertsen, guhklei en pigment voor de bouwblokken van de Bouwhulp, Vahoegiet "
         "voor het beste gereedschap, grotten om te ontdekken en gebouwtjes met buit."),
        ("systemen/onder-de-wol", "Nieuwe chunks",
         "De ondergrond, de grotten, de gebouwen en het mooiere oppervlak komen alleen in nieuwe chunks. Wat er al gegenereerd is, "
         "blijft zoals het was: ga dus een eindje verder op ontdekkingstocht."),
        # --- layers and ores ---
        ("systemen/steenlagen-en-ertsen", "De lagen",
         "Van boven naar beneden:\n\n"
         "- Roze wol: zoals altijd, met een dik pak bovenop.\n"
         "- Viltsteen (ongeveer y 45 tot 64): zacht en snel te hakken, met een hele familie (gepolijst, bakstenen, tegels, gebeiteld, "
         "trappen, platen en muurtjes).\n"
         "- Knuffelsteen (ongeveer y 20 tot 45): nu eindelijk in bulk, met aders roze knuffelsteen, belletjes wol in de steen, roze "
         "kristalpuntjes en pastel strepen.\n"
         "- Guhkristalsteen (ongeveer y 5 tot 20): de diepste laag, met de rijkste ertsen.\n"
         "- Knuffelnaad (y 0 tot 4): de onbreekbare bodem.\n\n"
         "De grenzen golven een paar blokken op en neer en volgen het land: onder bergen liggen ze hoger, onder dalen en de zee lager. "
         "Op het oppervlak blijft altijd een laagje biome liggen, met hier en daar een kaal plekje viltsteen."),
        ("systemen/steenlagen-en-ertsen", "Ertsen per laag",
         "Alle vanilla-ertsen (kolen, koper, ijzer, goud, lapis, redstone, diamant en smaragd) en de guh-ertsen (vadserts, "
         "kaasknabbelerts, kaaskorrelerts en pigmenterts) hebben een versie in viltsteen, knuffelsteen en guhkristalsteen. Ze laten "
         "hetzelfde vallen als in vanilla (fortuin en smelten werken gewoon). Kolen en kaasknabbels zitten vooral bovenin, ijzer in "
         "het midden, goud, vads en kaaskorrels dieper, redstone en diamant alleen in de guhkristalsteen. Smaragd vind je alleen onder "
         "de Guhpieken en de Vadskliffen. In de roze wol zitten alleen de bekende kaasknabbel-klodders."),
        ("systemen/steenlagen-en-ertsen", "Guhklei en pigment",
         "Guhklei zit in grote klodders in de viltsteen en de knuffelsteen. Hij geeft kleiballetjes (die bak je tot bakstenen), en "
         "het blok zelf bak je tot roze terracotta: de grondstof voor de dakpannen en het pleister van de Bouwhulp. Pigmenterts zit in "
         "de knuffelsteen en de guhkristalsteen en laat pastelkleurstof vallen: de gewone kleuren bovenin, de zeldzamere dieper."),
        ("systemen/steenlagen-en-ertsen", "Vahoegiet",
         "Het zeldzaamste erts: alleen in de guhkristalsteen, tussen y 5 en 18, en in de knuffelhart-geodes. Vahoegiet heb je nodig "
         "voor het Glinsteraltaar, het Rugzakzadel, de Mijnguh-hulpje-quest en de Vahoegietstaaf van super vahoege vads."),
        # --- caves ---
        ("systemen/grotten", "Grotten per laag",
         "In de roze wol zitten weinig grotten: smalle slingergangen en grotmonden die naar beneden leiden. In de viltsteen worden "
         "het slingergangen, in de knuffelsteen grote kaasgat-zalen met sausmeertjes en sausvallen, en in de guhkristalsteen "
         "gigantische zalen, knuffelhart-geodes (holle kristalbollen) en grote sausmeren."),
        ("systemen/grotten", "De sauszee",
         "In ongeveer een kwart van de ondergrond, in grote vlekken, lopen de diepste grotten vol warme kaassaus tot een vast niveau "
         "(rond y 10): een ondergrondse sauszee. Hoger liggen losse sausmeertjes, en uit de wanden stromen sausvallen."),
        ("systemen/grotten", "Water onder de grond",
         "Hier en daar ligt een stil grotmeertje met een gloeiend randje van lichtmos en glinsterpaddenstoeltjes, en uit sommige "
         "wanden komt een watervalletje. Minder vaak dan in een gewone wereld, maar wel extra lief."),
        ("systemen/grotten", "Zes grotbiomen",
         "Elk grotbiome ligt in zijn eigen laag en heeft eigen planten, licht en een vrolijk muziekje:\n\n"
         "- Pluisgrot (viltsteen): pluiswol, pluisbolletjes en zachte pluiskussens.\n"
         "- Slaapgrot (viltsteen): wilde guhs liggen te slapen tussen nachtlampjes.\n"
         "- Knuffelwortelgrot (viltsteen, alleen onder bossen): hangende knuffelwortels en mos.\n"
         "- Glitterspelonk (knuffelsteen): glitterranken, lichtmos, glinsterpaddenstoeltjes en glimwormpjes.\n"
         "- Belletjesgrot (knuffelsteen): kristallen die een toontje spelen.\n"
         "- Kristalpegelgrot (knuffelsteen en dieper): roze kristalpegels aan het plafond en op de vloer.\n\n"
         "De Gatenkaasgrotten en de Guhkristalmijn zijn er ook nog, aangepast aan de nieuwe lagen. Een bijzondere grot vind je om de "
         "paar honderd blokken; gewone grotten zijn er het meest."),
        ("systemen/grotten", "Vrolijke grotmuziek",
         "Onder de grond van de Guhmensie hoor je geen enge grotgeluiden meer. Elk grotbiome heeft een eigen vrolijk, schattig "
         "muziekje, en in plaats van het gerommel in het donker hoor je zachte druppeltjes en belletjes. Andere dimensies klinken "
         "zoals altijd."),
        ("systemen/grotten", "Wie woont er onder de grond?",
         "Niets vijandigs: in de Guhmensie spawnen monsters niet vanzelf onder de grond (behalve in hun eigen gebouwen). In de "
         "grotten lopen vooral gewone wilde guhs, en heel af en toe (net zo zeldzaam als een chocoladeguh) een Grotguh met een "
         "donkerroze vacht vol kristalspikkels die in het donker zacht oplichten. De Molguh steekt zijn kopje uit de viltsteen en de "
         "knuffelsteen, graaft er weer in en laat soms guhklei, kleurstof of (heel zelden) Vahoegiet achter. In de Glitterspelonk "
         "hangen glimwormpjes aan een draadje van het plafond."),
        ("systemen/grotten", "Een grot zoeken",
         "Voor beheerders (en in creatief): /guhs grot <naam> zoekt het dichtstbijzijnde grotbiome van die soort en brengt je erheen. "
         "De gewone /locate biome kijkt maar op een paar hoogtes en mist de dunne grotlagen vaak."),
        # --- the cave biomes' own pages ---
        ("biomen/glitterspelonk", "In de Glitterspelonk",
         "Een grotbiome in de knuffelsteen. Aan het plafond hangen gloeiende glitterranken (sommige met glitterbessen om te plukken en "
         "te eten), de vloer ligt vol roze lichtmos en glinsterpaddenstoeltjes, en in kleine groepjes hangen glimwormpjes aan hun "
         "draadje. Grotguhs komen hier wat vaker voor."),
        ("biomen/pluisgrot", "In de Pluisgrot",
         "Een grotbiome in de viltsteen, bekleed met pluiswol en pluisgras. Loop je door de pluisbolletjes, dan waaien er pluisjes op. "
         "Op een pluiskussen land je zacht, met een klein stuitertje."),
        ("biomen/slaapgrot", "In de Slaapgrot",
         "Een grotbiome in de viltsteen met een bodem van pluiskussens en wol. Wilde guhs liggen hier opgekruld te slapen (zzz) en "
         "snurken af en toe. Aai er een (rechtsklik met een lege hand) of geef hem een kaasknabbel: hij gaapt, rekt zich uit en is "
         "daarna een gewone wilde guh. Glinsterpaddenstoeltjes zijn de nachtlampjes."),
        ("biomen/knuffelwortelgrot", "In de Knuffelwortelgrot",
         "Een grotbiome in de viltsteen, alleen onder bossen. Van het plafond hangen zachte knuffelwortels, en op de knuffelwortelaarde "
         "groeit mos. Met beendermeel op knuffelwortelaarde groeit er een nieuwe knuffelwortel onder."),
        ("biomen/belletjesgrot", "In de Belletjesgrot",
         "Een grotbiome in de knuffelsteen vol belletjeskristallen en belletjestrosjes. Stap erop, sla ertegen of klik op een trosje "
         "en hij speelt een toontje, met een zachte echo. Elke plek heeft zijn eigen toon: een rijtje kristallen is een toonladder, "
         "dus je kunt er liedjes op tikken."),
        ("biomen/kristalpegelgrot", "In de Kristalpegelgrot",
         "Een grotbiome in de knuffelsteen en dieper, zoals een druipsteengrot maar dan roze: kristalpegels hangen aan het plafond en "
         "staan op de vloer, hun puntjes gloeien zacht, en af en toe druppelt er een druppeltje kaassaus af. Pas op: een pegel die "
         "loslaat valt naar beneden en doet pijn, en wie op een pegelpunt valt ook."),
        # --- buildings ---
        ("systemen/ondergrondse-bouwwerken", "Altijd in de buurt: het Mijnguhdorp",
         "Elke wereld heeft één Mijnguhdorp tussen ongeveer 300 en 1000 blokken van het midden (in nieuw land), en verder af en toe "
         "nog een. Het ligt rond een warm sausmeer, met huisjes op beide oevers, terrasjes, de kraam van de Mijnguh en zijgangen naar "
         "aangekoppelde gebouwen (zoals de Knuffelslaapzaal, de Kaasmakerij, de Wolspinnerij en de Kristalkwekerij). Een "
         "Mijningang-hutje bovengronds brengt je met een wolkenlift naar beneden."),
        ("systemen/ondergrondse-bouwwerken", "Vaak, af en toe en zeldzaam",
         "Vaak: de Verlaten guhmijn (een willekeurig gangennet met bijzondere kamers en kistsleetjes), Grotschilderingen en het "
         "Sausmeer-vissershutje.\n\n"
         "Af en toe: het Verlaten sledestation, de Kaasmakerij De Gouden Saus, het Verzonken guhhuisje, het Reuzenguh-fossiel, de "
         "Kristalkwekerij, de Verdwaalde bouwploeg, de Grotguh-holen, Badhuis De Warme Knabbel, de Grotdisco, de Oude wolspinnerij, "
         "de Sterrengrot, de Knuffelslaapzaal, het Paddenstoelendorpje, de Knuffelkuil, het Verrassingsfeestje en het Mijningang-hutje.\n\n"
         "Zeldzaam: de Oude guhtempel (een enorm tempelcomplex) en de Oude bankkluis. Om de zoveel honderd blokken ligt ook het "
         "Vadsmijnstadje en een van de grot-minigames."),
        ("systemen/ondergrondse-bouwwerken", "Wat vind je er?",
         "Bordjes met guh-grapjes en bewoners die vertellen wat er is, bouwstoffen, kaasknabbels en vads, en per plek een paar eigen "
         "deco-blokjes. Een paar gebouwen waar extra veel moeite in zit verstoppen ook guhkleding. In de kisten van de diepste "
         "gebouwen ligt heel zelden een Super-vads-sjabloon, en hier en daar een Spreukjesboekje. De oude gebouwen van vroeger "
         "(zoals de guhgrotten en de kaasmijn) staan nu in de steen van hun diepte."),
        ("bouwwerken/mijnguhdorp", "De Mijnguh",
         "In zijn kraam aan het sausmeer zit de Mijnguh. Hij vraagt je om hulp (de quest Mijnguh-hulpje) en geeft je daarna een "
         "Mijn-guhhuisje."),
        ("bouwwerken/vadsmijnstadje", "Een stadje van echte villagers",
         "Een grote grotzaal in de knuffelsteen, ongeveer 60 bij 60 blokken, met tien huisjes van viltsteen en pastelbaksteen rond "
         "een plein. Op het plein draait de Guhrad-centrale: twee guhs in Guhraden die het stadje van vadskracht voorzien. Er wonen "
         "tien guh-villagers (met de vier nieuwe beroepen en een paar oude guh-beroepen), twee kindjes en twee IJzerguhlems. Een "
         "Mijningang-hutje bovengronds brengt je met de wolkenlift naar beneden. Je vindt er een om de ongeveer 1500 blokken."),
        # --- villagers ---
        ("systemen/guh-villagers", "Een nieuw model",
         "Alle guh-villagers (ook die in de bovengrondse guhdorpen) hebben een nieuw, schattiger guh-model: een ronde guhkop voor de "
         "volwassenen en een klein knuffel-villagertje voor de kindjes. Hun ruilhandel blijft hetzelfde."),
        ("systemen/guh-villagers", "Vier nieuwe beroepen",
         "- Toverguh (werkplek: de Toverguh-boekenstandaard): verkoopt Spreukjesboekjes, ook met de spreukjes die je niet uit het "
         "Glinsteraltaar krijgt.\n"
         "- Vadsmonteur (de Guhrad-werkbank): Guhraderen, Guhdraad, Guh Ovens en meer machines voor vadskracht; hij koopt vahoege vads.\n"
         "- Mijnwerker (de Ertsenkist): laagstenen, guhklei, kleurstof, viltkiezel, lichtmos en lampjes; hij koopt je ertsen en Vahoegiet.\n"
         "- Kristalslijper (de Kristalslijpsteen): slijpt guhkristal en Vahoegiet tot kristallampen, kristalpegels, belletjeskristallen "
         "en meer glinsterblokken."),
        ("systemen/guh-villagers", "De IJzerguhlem",
         "Guh-villagers roepen geen ijzergolem op maar een IJzerguhlem: lavendelkleurig ijzer, een ronde guhkop, een pluizige roze kraag "
         "en een bloemenkroontje. Hij doet precies wat een ijzergolem doet en verschijnt volgens dezelfde regels, dus een "
         "ijzerboerderij met guh-villagers werkt gewoon. Hij laat ijzerstaven en pluisgras vallen in plaats van papavers."),
        ("systemen/guh-villagers", "Villagers oppakken",
         "Sluip en rechtsklik met een lege hand op een villager (een gewone of een guh-villager, groot of klein) en je pakt hem op, "
         "net als een tamme guh. Hij houdt zijn beroep, zijn niveau, zijn ruilaanbiedingen en zijn naam. Rechtsklik op de grond om "
         "hem weer neer te zetten. Niet terwijl hij aan het handelen is, en niet de villagers die bij een gebouw of minigame horen."),
        # --- the altar ---
        ("systemen/glinsteraltaar", "Zo werkt het",
         "Het Glinsteraltaar werkt als een betoveringstafel: leg er een stuk gereedschap, een wapen, een harnasstuk of een boek op, "
         "met lapis, en kies een van de drie spreukjes. Het kost lapis en levels, net als in vanilla. Hoe sterk de spreukjes zijn, hangt "
         "af van de knuffelboekenkasten om het altaar heen (zoals boekenkasten om een betoveringstafel, tot 15); gewone boekenkasten "
         "tellen hier niet mee. Uit de kasten vliegen hartjes naar het altaar."),
        ("systemen/glinsteraltaar", "Wat zit erin?",
         "De nuttige vanilla-betoveringen: scherpte, bescherming, efficiëntie, onbreekbaarheid, kracht, fortuin, buit, zijden aanraking, "
         "veervallen, ademhaling, diepteloper, snelle sluip en (zeldzaam) herstel. Plus vijf guh-spreukjes: Mika-mepper, Pluisval, "
         "Glitterzicht, Vadsvlijt en Bakvads. Onbreekbaarheid en herstel komen nooit op spullen die toch al niet kapot gaan (zoals "
         "vahoege vads en de Mika-mepper)."),
        ("systemen/glinsteraltaar", "De acht guh-spreukjes",
         "- Mika-mepper (wapens): dubbele schade tegen Mika's. Niet op de Mika-mepper zelf.\n"
         "- Pluisval (laarzen): geen valschade als je op wol, vilt of pluis landt. Poef!\n"
         "- Glitterzicht (helm): een zacht glinsterend nachtzicht, alleen onder de grond.\n"
         "- Vadsvlijt (gereedschap, tot niveau 3): veel sneller door wol, vilt en kaasknabbelblokken.\n"
         "- Bakvads (mijngereedschap): wat je hakt komt al gesmolten uit de grond.\n"
         "- Spaarguhtje (mijngereedschap): wat je hakt en oppakt gaat meteen naar de Bank Guh van de Banksleutel in je inventaris. Klingel!\n"
         "- Aaizacht (wapens): je klappen raken nooit je eigen huisdieren of villagers.\n"
         "- Kaasknabbelgeur (elk harnasstuk): alle wilde guhs in de buurt komen naar je toe en lopen je achterna.\n\n"
         "Spaarguhtje, Aaizacht en Kaasknabbelgeur krijg je nooit uit het altaar: alleen als Spreukjesboekje, van de Toverguh of uit "
         "een kist in een gebouw onder de grond."),
        ("systemen/glinsteraltaar", "Spreukjesboekjes",
         "Een Spreukjesboekje is het guh-betoverde boek: een lila boekje met hartjes. Je krijgt het als je een boek op het altaar "
         "betovert met een guh-spreukje, van de Toverguh of uit kisten. Op een aambeeld zet je het spreukje over op je spullen, net als "
         "een betoverd boek."),
        # --- mining helpers ---
        ("systemen/mijnhulpjes", "De Vahoege Vadsknakker",
         "Houweel, bijl, schep, schoffel en schaar in één. Je maakt hem van een vadspaxel, een vahoege vadsschaar en een vahoege "
         "vads-schoffel; hun betoveringen gaan mee. Rechtsklik doet slim wat bij het blok past: een stam wordt gestript, gras en aarde "
         "worden een akkertje, sluipen op gras maakt een paadje, en een schaap scheer je. Guhs houden hun wol. Hij gaat nooit kapot."),
        ("systemen/mijnhulpjes", "Super vahoege vads",
         "De sterkste versie van het vads-gereedschap en -harnas (zo sterk als netheriet). Maak een Vahoegietstaaf van 4 Vahoegiet en "
         "4 vahoege vads-staven, en leg op de smidstafel het Super-vads-sjabloon, een vahoege vads-item en de staaf. Het sjabloon ligt "
         "heel zelden in de kisten van de diepste gebouwen onder de grond (zoals de Oude guhtempel en de Oude bankkluis); kopiëren kan "
         "met 7 Vahoegiet en guhkristalsteen. Alles kan in de Guhmensie: je hoeft er niet voor naar de Barbecuether."),
        ("systemen/mijnhulpjes", "Het Wolkenliftje",
         "Een lift voor je mijn: zet een Wolkenliftje onderin en een boven op de grond, recht boven elkaar. Ga erop staan en spring: je "
         "zweeft in een roze wolkje naar het volgende Wolkenliftje erboven (tot 256 blokken, met twee vrije blokken erboven). Sluipen "
         "brengt je naar het volgende eronder. Het werkt dwars door de steen heen."),
        ("systemen/mijnhulpjes", "De Pluisladder",
         "Een zachte roze touwladder op een rol. Zet hem tegen een wand aan de rand van een gat en hij rolt vanzelf uit tot de bodem. "
         "Breek je een stukje, dan rolt de hele ladder weer op en krijg je je rol terug."),
        ("systemen/mijnhulpjes", "Het Mijnlampje",
         "Een roze helmpje met een lampje voor je tamme guh (guhkleding op het hoofd). Waar hij ook loopt, hij geeft licht in het "
         "donker: handig als hij je volgt in de mijn."),
        # --- the mine house ---
        ("systemen/mijn-guhhuisje", "De quest Mijnguh-hulpje",
         "Praat met de Mijnguh (in het Mijnguhdorp onder de grond, of in de kaasmijn) en hij vraagt om 16 viltsteen, een guhklei en "
         "een Vahoegiet. Breng ze en je krijgt het Mijn-guhhuisje en een Mijnlampje voor je guh. Wil je nog een huisje? Breng hem "
         "hetzelfde nog eens, met de Vahoegiet in je hand."),
        ("systemen/mijn-guhhuisje", "Zo werkt het huisje",
         "Klik met een opgepakte guh op het huisje en die trekt erin: hooguit vier van je eigen tamme guhs. In het menu stel je met "
         "schuifjes het gebied in (tot 20 blokken naar elke kant en zo diep als je wilt, nooit onder y 10); roze glinsters laten de "
         "rand zien. Gratis mijnt elke guh een blok per 20 seconden, met kaasknabbels vier keer zo snel. Wat ze minen gaat in een kist "
         "naast het huisje, of in de Bank Guh van een Banksleutel. Gebouwen, water en lava laten ze met rust."),
        ("systemen/mijn-guhhuisje", "Het Rugzakzadel",
         "Guhkleding voor op de rug, te maken met een zadel, roze wol, een kist en een Vahoegiet. In de rugzak zit een guhtje dat alles "
         "in zijn buikje bewaart, dus er past eindeloos veel in: gewone grondblokken zoals wol, de steenlagen, aarde, steen, zand, grind "
         "en guhklei (geen ertsen of zeldzame dingen). Woont een guh met een Rugzakzadel in een Mijn-guhhuisje, dan stopt hij de "
         "gewone grond in zijn rugzak en brengt hij die naar de Bank Guh (zoveel als past; de rest blijft in de rugzak)."),
        # --- feet ---
        ("systemen/voetjes", "Wat er is",
         "- Pluisslofjes en regenlaarsjes: bij de kleermaker, voor 6 en 8 smaragden.\n"
         "- Golfschoentjes: in het golfwinkeltje van de Golfguh, voor 12 golfballetjes.\n"
         "- Mijnwerkerslaarsjes: zeldzaam in de kisten van de Verlaten kaasmijnschacht.\n"
         "- Kaassauslopers: zeldzaam in de kisten van het Mika-grillpaleis en de Spiesburcht.\n"
         "- Lavalopers: laat de kleermaker je kaassauslopers opwaarderen met 4 magmacrème en een vahoege vads."),
        ("systemen/voetjes", "Lopers",
         "Een tamme guh met kaassauslopers loopt bovenop alle kaassaus, ook op de hete kaasfrituursaus van de Barbecuether, alsof het "
         "vloer is. Hij zakt er niet in, verbrandt niet en volgt je gewoon naar de overkant. Met lavalopers kan dat ook op echte lava."),
        ("systemen/kleding", "Voetjes",
         "Sinds 1.7.0 heeft de kledingkast een zevende slot: de voetjes, voor slofjes, laarsjes, golfschoentjes en lopers. Zie Voetjes: "
         "het zevende kledingslot."),
        # --- surface ---
        ("systemen/vrolijk-oppervlak", "Bloemen, keien en vijvertjes",
         "Een lappendeken van lila, witte en perzikkleurige wol, bloemenweitjes vol guhbloemen en vlindertjes, wolkeien met mos, kleine "
         "vijvertjes met waterlelies en riet, watervalletjes uit de hellingen, paddenstoelenkringen die 's nachts licht geven (soms "
         "danst er een guh in), omgevallen stammetjes met paddenstoeltjes, boomgroepjes, bloeiende struikjes, heggetjes en pluisjes in "
         "de lucht. Langs het water ligt viltkiezel: zacht grind van viltbolletjes."),
        ("systemen/vrolijk-oppervlak", "Beekjes en strandjes",
         "Kronkelbeekjes en beekjes die van een bronnetje op een heuvel naar beneden lopen, met hier en daar een stapje, roze glinsters "
         "boven het water, glinsterriet en waterlelies. Langs meren en zeeën liggen roze strandjes (met schelpjes) naast de gewone, "
         "en in grotere meren en voor de kust liggen eilandjes met een boompje."),
        ("systemen/vrolijk-oppervlak", "Pareltjes",
         "Heel af en toe: een hartjesmeertje (van boven een hartje, met een bloemenrand en hartjeslelies), regenboogheuvels met "
         "pastel strepen in de hellingen, en in de Kaasvlakte en de Knabbelkruimels een kaaslappendeken van gatenkaas, kaasmos, "
         "kaasbloemen en plasjes kaassaus. En de Bloemenvallei: een kleine, zeldzame biome vol bloemen in alle guh-kleuren, met bijen, "
         "vlindertjes en pluisjes. 's Avonds is de lucht zachter roze, en 's nachts vliegen er glimguhtjes boven de velden en het water."),
        ("biomen/bloemenvallei", "De Bloemenvallei",
         "Een kleine, zeldzame biome van de Guhmensie (sinds 1.7.0), vol bloemen in alle guh-kleuren, met bijen, vlindertjes en "
         "pluisjes in de lucht. Soms ligt er een hartjesmeertje in."),
        # --- the cave minigames ---
        ("systemen/grot-minigames", "Hoe kom je er?",
         "Elke spelletjeszaal ligt diep onder een roze hutje ergens in de Guhmensie, ongeveer om de 500 blokken. Ga in het hutje op "
         "het Wolkenliftje staan en sluip: je zweeft naar beneden; springen op het liftje in de zaal brengt je weer naar boven. Elke "
         "zaal heeft ook twee of drie extra ingangen: een tunneltje met een deur en een bordje \"Ingang\", dus graaf je vanuit een "
         "grot naar een zaal, dan kom je er ook in. Alleen de zaal zelf is beschermd, de steen eromheen niet. Het Superkompas "
         "wijst de dichtstbijzijnde aan."),
        ("systemen/grot-minigames", "Munten en winkeltjes",
         "Elk spel heeft zijn eigen munt: schatmuntjes, glimmertjes, lampjesmunten, ertsbonnetjes en kegelknopen. Hoe beter je speelt, "
         "hoe meer munten, en een nieuw persoonlijk record geeft er nog wat bij. In het winkeltje van de spelleider koop je er "
         "guhkleding, bouwblokken (vier voor één munt) en deco mee. Alle vijf de munten werken ook in de grijpmachine. Je allereerste "
         "potje geeft een cadeautje: 16 kaasknabbels en iets moois voor je huisje."),
        ("minigames/schatgraven", "Zo speel je",
         "Kapitein Schepsnuit leent je zijn zachte schepje. Je hebt 90 seconden om de schatten onder het pluisveld op te graven "
         "(rechts- of linksklik op het veld). Snuffie snuffelt mee: KOUD, warm of HEET (dan springt hij!). Elke schat is 10 punten, het "
         "gouden kaasje 25, en een piepkaas kost je 10 seconden. Alles gevonden? Dan tellen de seconden die over zijn ook mee. "
         "Makkelijk is een klein veld met veel hulp van Snuffie, lastig het hele veld, en Snuffie snuffelt minder vaak."),
        ("minigames/schatgraven", "Het winkeltje",
         "Het oogklepje (40 schatmuntjes) en het piratenhoedje (60), schatzandsteen, schatkistplanken, pluiswol en viltkiezel, en als "
         "deco het gouden kaasje (24) en het schatkistje (40)."),
        ("minigames/glimwormvangen", "Zo speel je",
         "Juf Glimmie geeft je een netje en een potje op je rug. Je hebt 60 seconden: zwaai met je netje (rechtsklik) naar de "
         "glimwormpjes vlak voor je. Sommige zijn snel, sommige verstoppen zich in de glimplanten, en een gouden glimworm is 5 punten. "
         "Hoe meer je vangt, hoe lichter je potje en de zaal. Lastig: meer en snellere glimwormen, en het begint donkerder."),
        ("minigames/glimwormvangen", "Het winkeltje",
         "Het vlindernetje (50 glimmertjes) en de glimworm-antennetjes (60), nachtvilt en glimmos, en als deco de glimplant (20) en "
         "het glimpotje (24)."),
        ("minigames/lichtjespuzzel", "Zo speel je",
         "Meester Knipperlicht heeft een groot raster van 7 bij 7 paddenstoellampjes. Spring op een lampje (of buk erop): hij en zijn "
         "vier buurtjes schakelen om, aan wordt uit en uit wordt aan. Maak ze allemaal aan voor de tijd op is. Elke puzzel kan, en hij "
         "weet hoeveel sprongetjes het minstens kost: hoe dichter je daarbij zit en hoe sneller je bent, hoe meer lampjesmunten. "
         "Lastig: een moeilijkere puzzel en minder tijd."),
        ("minigames/lichtjespuzzel", "Het winkeltje",
         "Het gloeiende paddenstoelhoedje (70 lampjesmunten), paddenstoelsteen en steeltjessteen, en als deco het paddenstoellampje "
         "(28), het paddenstoelkrukje (32) en de lampjestegel (40)."),
        ("minigames/karretjes", "Zo speel je",
         "Je staat met Conducteur Piepwiel op het balkon boven een sporennet. De karretjes rollen uit de tunnel, elk vol met één erts: "
         "de kleur van de lading zegt in welke bak hij moet. Boven elke wissel staat een hendel op de reling (aan is rechtsaf, uit is "
         "linksaf). Een karretje in de verkeerde bak is weg; drie weg en je dienst zit erop. Ze komen steeds sneller. Er zijn drie "
         "banen: 2, 3 of 5 wissels."),
        ("minigames/karretjes", "Het winkeltje",
         "Het conducteurspetje (50 ertsbonnetjes), bielzenhout, ertsgruissteen en ertsgruisbakstenen, en als deco het ertskarretje "
         "(64, je kunt erin zitten), het stationsklokje (24) en het seinlampje (20)."),
        ("minigames/kegelen", "Zo speel je",
         "Echt bowlen bij De Guhde: tien frames, twee ballen per frame, op tien pluchen guh-kegeltjes die piepen. Een strike telt 10 plus "
         "je volgende twee ballen, een spare 10 plus de volgende, en in het tiende frame krijg je extra ballen. Kijk waar de wolbal heen "
         "moet, houd rechtsklik vast voor kracht en laat los. Sta links of rechts op de aanloop voor een draaitje, of sluip voor een "
         "rechte bal. Drie banen: recht, bochtig en een met hindernisjes."),
        ("minigames/kegelen", "Het winkeltje",
         "Het bowlingshirtje (70 kegelknopen) en het zweetbandje (40), kegelbaanhout en retrotegels, en als deco het kegeltje (20), het "
         "wolbalrek (40) en het neon-kegelbord (32)."),
        # --- golf ---
        ("minigames/golf", "De achterste 9",
         "Nieuwe golfbanen (sinds 1.7.0) hebben 18 holes; banen die al bestonden houden er 9. De achterste 9 zijn een Rondje Guhmensie: "
         "elke hole is een mini-versie van een plek in de Guhmensie, met bewegende obstakels, schansen, loopings, bumpers, glad ijsvilt "
         "en plakkerige kaasmodder, en de Grote Guhmond als finale. Ze gaan open als je de voorste 9 één keer op of onder par hebt "
         "gespeeld. Par: 25 op makkelijk, 38 op medium en 47 op lastig. Kies bij de Golfguh de voorste 9, de achterste 9 of alle 18."),
        ("minigames/golf", "Meer golfballetjes",
         "Een hole van de achterste 9 geeft meer golfballetjes dan een van de voorste: 8 voor een hole-in-one, 6 voor een eagle, 5 voor "
         "een birdie, 4 voor par, 2 voor een bogey en anders 1. Na alle 18 holes krijg je er 5 bij, nog 8 als je op of onder par zat en "
         "nog 4 voor een nieuw 18-holes-record."),
        ("minigames/golf", "Het golfwinkeltje",
         "Naast het golfpakje verkoopt de Golfguh nu golfschoentjes (12), golfbaanvilt en ruitjesvilt in drie kleuren (vier blokken voor "
         "één golfballetje), en deco: golfballetjes op een tee (3), een hole met een vlaggetje (6), een golfrek (10) en een golfkarretje "
         "om in te zitten (64)."),
        ("minigames/golf", "De golftrofee",
         "Niet te koop: speel alle 18 holes op of onder par en de Golfguh geeft je eenmalig de golftrofee, met je naam en je record erop."),
        # --- elsewhere ---
        ("dimensies/barbecuether", "Geen lava meer",
         "Sinds 1.7.0 is er in de Barbecuether geen echte lava meer: alles is kaasfrituursaus, en die is ongevaarlijk (geen brandwonden, "
         "geen vuurtjes). Lava in stukken die al bestonden wordt vanzelf omgezet als ze laden; je wereld wordt niet opnieuw gemaakt. "
         "Met kaassauslopers lopen je tamme guhs over de saus."),
        ("verhalen/knabbelring", "Je eigen Rookguh-platform",
         "Speel je de hele reis van de Knabbelring uit, dan krijg je van de Rookguh een eigen Rookguh-platform (wie dat al gedaan had, "
         "kreeg het bij de eerste keer inloggen na 1.7.0). Zet het neer waar je wilt, in elke wereld: de Rookguh vliegt je er altijd "
         "heen, en vanaf je platform vliegt hij je naar elke plek van je reis, of naar het Super Guhrio-kasteel als je wereld 1-1 hebt "
         "uitgespeeld. Je hebt er één; zet je hem ergens anders neer, dan verhuist je landingsplek mee."),
        ("systemen/verblijfsborden", "Grotere terraria",
         "Sinds 1.7.0 mag een terrarium elke vorm hebben: bouw terrariumglas of terrariumbodem tegen het glazen bakje aan, en alles wat "
         "zo aan elkaar vastzit (een grote glazen bak, gangetjes van één blok breed, omhoog en omlaag) is één leefruimte. De diertjes "
         "lopen overal doorheen, maar nooit eruit. Twee terraria die met glas verbonden zijn delen niet: de ene krijgt de hele ruimte."),
        ("systemen/commandos", "Grotten zoeken (1.7.0)",
         "/guhs grot <naam> (beheerders): zoekt het dichtstbijzijnde grotbiome van die soort in de Guhmensie en brengt je erheen. Namen: "
         "glitterspelonk, pluisgrot, slaapgrot, knuffelwortelgrot, belletjesgrot, kristalpegelgrot, gatenkaasgrotten en guh_kristalmijn."),
        ("dimensies/guhmension", "Onder de Wol (1.7.0)",
         "Onder de wol van de Guhmensie zitten nu steenlagen, ertsen, grotten met zes grotbiomen en ruim twintig gebouwen onder de grond. "
         "Zie Onder de Wol: de ondergrond."),
        # --- 1.7.0 ronde 45-47: vadskracht in the buildings ---
        ("systemen/nieuw-in-1-7", "Vadskracht",
         "De Vadslamp, een guhlantaarn op 1 vadskracht met licht 15 en extra licht eromheen, en de Vadsschakelaar, het "
         "aan/uit-knopje in een lijn Guhdraad. De Guhtechniek-beurs: een beschermde glazen hal op het land van de "
         "Guhmensie met een werkende stand voor elk onderdeel van de Guh-technologie, beursguhs en in het midden de "
         "Droomfabriek. En echte vadskracht in gebouwen: het Vadsmijnstadje, de vijf grot-minigamezalen (met de "
         "Kegelzetter in de Kegelgrot), de Kaasmakerij, de Oude wolspinnerij en nieuwe exemplaren van oude gebouwen: de "
         "Guhbakkerij in het guhdorp, de kaasmijn, de keukens en de Grillkoolfabriek van de Barbecuether en de "
         "oogsthoek van de guhboerderij."),
        ("bouwwerken/vadsmijnstadje", "Vadskracht in het stadje",
         "De Guhrad-centrale op het plein heeft twee Guhraden rug aan rug, samen 30 vadskracht. Alle 18 lantaarns van "
         "het stadje zijn Vadslampen, verbonden met Guhdraad langs de straten, met een Vadsschakelaar voor de hele "
         "lijn. Het net vraagt 28 vadskracht: zet je de hendel voor een van de Guhraden om, dan krijgt die guh pauze en "
         "gaan alle lampen uit. Bij Huize Glinster staat de Glasslijperij van de Kristalslijper (een Vadsmolen maalt "
         "grind tot zand, een Guhoven bakt er glas van) en bij het ertsdepot de Ertslijn: een kistkarretje vol ruw erts "
         "boven een trechter, een Guhoven die het erts smelt en een Vadsmolen die mijngruis maalt, alles in de "
         "Mijnwerkerskist bij Huize Houweel, op een eigen Guhrad. Bij elke opstelling staat een bordje met hoeveel "
         "vadskracht het gebruikt. Het stadje is niet beschermd, dus er staan alleen eenvoudige machines. Een verstopte "
         "bijvuller vult elke drie minuten de Vadsmolens bij met grind en ruimt de Mijnwerkerskist en de glasoven op (er "
         "blijft altijd wat liggen om te pakken). Het kistkarretje krijgt het erts van de opgeruimde baren terug, maar nooit "
         "nieuw erts. Een machine die je sloopt blijft weg."),
        ("systemen/grot-minigames", "Vadskracht in de zalen",
         "Elke zaal heeft een eigen Guhrad met een guh, achter een Vadsschakelaar en met een hendel ervoor (aan = de "
         "guh heeft pauze). Het geeft vadskracht aan Vadslampen in de zaal en aan twee scorebordlampjes naast de "
         "spelleider. Bij Schatgraven staan acht lampen rond het veld, bij de Lichtjespuzzel zijn de twaalf "
         "paddenstoellampen aan de muur Vadslampen en in de Wisselgrot de lantaarns op het balkon. Glimwormvangen "
         "blijft expres donker (de glimpotjes zijn het licht), daar branden alleen de scorebordlampjes. De spelletjes "
         "zelf werken ook zonder vadskracht."),
        ("minigames/kegelen", "De Kegelzetter",
         "Boven het einde van de banen hangt de Kegelzetter: een brug met een eigen Guhrad en een Vadslamp per baan. "
         "Met vadskracht komt er een vers rek kegels met vonkjes en een klak. Zonder vadskracht staat er op het bordje "
         "van de baan \"pauze\" en zet De Guhde de kegels met de hand: je kunt dus altijd verder spelen. Achter De Guhde "
         "draait nog een Guhrad voor de lampen van de zaal."),
        ("bouwwerken/guh_village", "Vadskracht (1.7.0)",
         "In nieuwe guhdorpen staat aan de rand van het plein de Guhbakkerij, onder een gestreepte luifel: een Guhrad "
         "met een guh, een Guhoven die aardappels bakt en de Vadsmolen van het plein, die knabbelgraan maalt. Bordjes "
         "vertellen hoeveel vadskracht alles gebruikt, en een hendel naast het Guhrad geeft de guh pauze. Guhdorpen die "
         "al bestonden blijven zoals ze zijn."),
        ("bouwwerken/kaasmijn", "Vadskracht (1.7.0)",
         "In nieuwe kaasmijnen pompen twee Sauspompen in de Knabbelgrot kaassaus uit de poel in vijf Sausvaten, en een "
         "Knabbelaar knabbelt aan een kaasader (die groeit weer aan); een Richtingstuk stuurt de kaasbrokken door een "
         "Knabbelbuis naar een kist. Alles draait op een Guhrad, met bordjes en een hendel die de guh pauze geeft."),
        ("bouwwerken/spiesburcht", "Vadskracht (1.7.0)",
         "In nieuwe spiesburchten staat in de grote hal de Spieskeuken: twee Guhovens die saté grillen en een "
         "Vadsmolen, op een Guhrad met bordjes en een hendel die de guh pauze geeft."),
        ("bouwwerken/mika_grillpaleis", "Vadskracht (1.7.0)",
         "In nieuwe grillpaleizen draait de grillkeuken op vadskracht: een Guhoven en een Sauspomp op frituursaus die "
         "vijf Sausvaten vult, op een Guhrad met bordjes en een hendel die de guh pauze geeft."),
        ("bouwwerken/mika_woonblokken", "Vadskracht (1.7.0)",
         "Op de binnenplaats van nieuwe woonblokken staat de Grillkoolfabriek: twee Guhraden met één hendel ertussen, "
         "Sauspompen op frituursaus en water, de Grillkoolpers en de Frituurautomaat, met buizen naar een kist. Bordjes "
         "vertellen hoeveel vadskracht alles gebruikt."),
        ("bouwwerken/guhboerderij", "Vadskracht (1.7.0)",
         "Nieuwe boerderijen hebben een oogsthoek op vadskracht: een Oogster boven een omheind graanveldje (de oogst "
         "gaat door een buis naar een kist) en een Plantagebak, waarvan de klusjes-guhs van de boerderij de boom "
         "omhakken. Met bordjes en een hendel die de guh van het Guhrad pauze geeft."),
        ("bouwwerken/kaasmakerij", "Het pomphuis",
         "Aan de steiger staat het pomphuis: twee Guhraden met blije guhs, met tegen elk een Sauspomp op het sausmeer. "
         "Slangen brengen de saus naar acht Sausvaten, die vollopen terwijl je kijkt, en Guhdraad over redstonelampen "
         "loopt naar een Guhoven die saté grilt."),
        ("bouwwerken/oude_wolspinnerij", "De krachtcentrale",
         "Op de noordelijke loopbrug voor het waterrad draaien drie Guhraden. Een gloeiende Guhdraad loopt over lampen "
         "langs de hal naar vier spinmolens die knabbelgraan malen in de spinkringen, en naar een Guhoven bij de "
         "theetafel."),
    ],
}
