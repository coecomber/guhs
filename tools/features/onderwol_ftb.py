"""
1.7.0 "Onder de Wol": the FTB Quests chapter guhs_onderwol (the docs step; the English is in tools/lang/en/c198_onderwol_ftb.json).

Not a feature module (not in FEATURES): tools/features/onderwol.py takes FTB_CHAPTER, FTB_SECTIES and ftb() from here, so
the quests count as the module "onderwol" for tools/make_ftbquests.py. One chapter for everything under the wool that has
no chapter of its own: the layers, the ores, the caves and their creatures, the underground buildings, the Mijnguh and the
Vadsmijnstadje, the Glinsteraltaar, the mining tools and the prettier surface. The five cave minigames add their own
sections to this chapter (grotspellen.py, mijnspellen.py: FTB_CHAPTER = "guhs_onderwol"); the back nine of the golf course
sit with the golf course (golf18.py), the feet slot with the wardrobe (voetjes.py), the Mijnguh-hulpje quest in the kaasmijn
section (kaasmijn.py) and the Rookguh-platform in the Knabbelring (ring_vlucht.py).

Rules of the generator: nothing is locked (no real dependencies), every task is something FTB Quests can check by itself
(an item, a hidden advancement, a structure, a biome). Quest keys start with "ow_" (stable: they are the quest ids).
"""
FTB_CHAPTER = "guhs_onderwol"
FTB_PORTRAIT = "guh:grotguh"

# the underground buildings (structure id, title, description), in the order of the design (often, now and then, rare)
GEBOUWEN = [
    ("verlaten_guhmijn", "De verlaten guhmijn", "Een oud gangennet van de mijnguhs, net een mineshaft: kistsleetjes, een schaftkantine, "
     "een sleedepot en hier en daar een ingestort stuk. Kijk in de sleetjes!"),
    ("grotschilderingen", "Grotschilderingen", "Een hoge zaal met een groot fresco rondom: hoe alles roze werd, en het Knuffelhart. "
     "Lees de bordjes bij elk tafereel."),
    ("sausmeer_vissershutje", "Het sausmeer-vissershutje", "Een hutje op palen aan een warm kaassausmeer. De vissen bijten niet, de kaas wel."),
    ("verlaten_sledestation", "Het verlaten sledestation", "Perrons, sleetjes en een loket, diep onder de grond. Vertrek: nooit."),
    ("kaasmakerij", "Kaasmakerij De Gouden Saus", "Sauspompen, kaasvaten en reuzenkaaswielen aan een warm sausmeer. Niet in het vat duiken!"),
    ("verzonken_guhhuisje", "Het verzonken guhhuisje", "Huize Pluis zakte ooit door de wol. Scheef, bemost, en er woont nog steeds iemand."),
    ("reuzenguh_fossiel", "Het reuzenguh-fossiel", "Het skelet van een oer-guh, diep in de kristalsteen. Ja, de oren waren echt zo groot."),
    ("kristalkwekerij", "De kristalkwekerij", "Kweekbedden vol guhkristallen in vier groeistadia. Niet aankomen, ze groeien nog!"),
    ("verdwaalde_bouwploeg", "De verdwaalde bouwploeg", "Een achtergelaten bouwplaats: een halve gang, een kraantje en stapels bouwstoffen. "
     "Waar zijn de bouwguhs gebleven?"),
    ("grotguh_holen", "De Grotguh-holen", "Het knusse thuis van de Grotguhs: slaapholletjes, een kampvuur en een knabbelvoorraad."),
    ("badhuis", "Badhuis De Warme Knabbel", "Een Romeins badhuis onder de wol: drie badzalen, knabbelbadjes en kaasshakes. Niet rennen, "
     "wel vadsen! In de kisten liggen soms een &dbadjasje&r en een &dhanddoek-tulband&r."),
    ("grotdisco", "De Grotdisco", "Een geheime disco in een kristalgeode. DJ Njeg draait, de guhs dansen vadskracht bij elkaar."),
    ("oude_wolspinnerij", "De oude wolspinnerij", "De Wolclub: guhs spinnen gezellig wol rond theetafeltjes, en een waterrad geeft "
     "vadskracht. Pauze is om de vijf minuten."),
    ("sterrengrot", "De Sterrengrot", "Een stille koepelgrot met een sterrenplafond, een picknick en een sterrenvijvertje. Wens iets bij een "
     "vallende ster (liefst kaasknabbels)."),
    ("knuffelslaapzaal", "De Knuffelslaapzaal", "Een ondergrondse slaapzaal vol hemelbedjes en slapende guhs. Sssst! In de kisten ligt "
     "soms een &dslaapmutsje&r."),
    ("paddenstoelendorpje", "Het Paddenstoelendorpje", "Reuzenpaddenstoelen met een huisje in de hoed: ga door het deurtje in de steel "
     "naar boven. Met lampjes aan touwtjes en een glinsterbeekje."),
    ("knuffelkuil", "De Knuffelkuil", "Een ballenbak vol knuffelblokken. Zes guhtjes spelen verstoppertje: vind ze allemaal! Schoenen uit."),
    ("verrassingsfeestje", "Het Verrassingsfeestje", "Een verborgen grot waar guhs een verjaardagsfeestje voorbereiden, met een "
     "reuzentaart. Sssst, niks verklappen! In de kisten liggen soms een &dfeesthoedje&r en een &dslinger-sjaaltje&r."),
    ("oude_guhtempel", "De oude guhtempel", "Heel zeldzaam: het enorme tempelcomplex van de oude guhs, half ingegraven, met De Grote Guh "
     "en de Gouden Kaasknabbel. Pas op voor de lieve valkuiltjes, en zoek het lichtpad naar de geheime kamer.", "hexagon"),
    ("oude_bankkluis", "De oude bankkluis", "Heel zeldzaam: de oude kluis van de Bank Guh. Iemand heeft geprobeerd in te breken. Iemand met "
     "een snor...", "hexagon"),
]

# the cave biomes (biome id, title, description)
GROTTEN = [
    ("pluisgrot", "De Pluisgrot", "Een zachte grot in de viltsteen: pluiswol, pluisgras, pluisbolletjes en pluiskussens waarop je "
     "zacht landt (met een stuitertje)."),
    ("slaapgrot", "De Slaapgrot", "Een grot vol pluiskussens en wol, met nachtlampjes. Er liggen wilde guhs te slapen: zzz... Aai er een "
     "en hij wordt wakker."),
    ("knuffelwortelgrot", "De Knuffelwortelgrot", "Alleen onder bossen: hangende knuffelboomwortels, mos en zachte "
     "knuffelwortelaarde."),
    ("glitterspelonk", "De Glitterspelonk", "Roze lichtmos, glitterranken met glitterbessen, glinsterpaddenstoeltjes en wilde "
     "glimwormpjes. Het glinstert alle kanten op!"),
    ("belletjesgrot", "De Belletjesgrot", "Kristallen die een toontje spelen als je erop stapt of ertegen tikt. Elke plek heeft zijn eigen "
     "toon: speel een liedje!"),
    ("kristalpegelgrot", "De Kristalpegelgrot", "Roze guhkristal-pegels aan het plafond en op de vloer, met druppeltjes kaassaus. Pas op "
     "je hoofd: een vallende pegel doet au!"),
]

FTB_SECTIES = [
    ("ow_lagen", "De lagen onder de wol", "item:guhs:viltsteen",
     ["ow_viltsteen", "ow_viltsteen_bouwen", "ow_knuffelsteen", "ow_roze_knuffelsteen", "ow_kristalsteen", "ow_guhklei", "ow_viltkiezel"]),
    ("ow_ertsen", "Ertsen per laag", "item:guhs:vahoegiet", ["ow_ijzer", "ow_vads", "ow_kaaskorrel", "ow_pigment", "ow_diamant", "ow_vahoegiet"]),
    ("ow_grotten", "Grotten en grotbewoners", "guh:grotguh",
     [f"ow_grot_{b}" for b, _, _ in GROTTEN] + ["ow_glitterbes", "ow_belletjes", "ow_grotguh", "ow_molguh"]),
    ("ow_gebouwen", "Gebouwen onder de grond", "npc:mijnwerkerguh", [f"ow_bouw_{s}" for s, *_ in GEBOUWEN] + ["ow_sjabloon"]),
    ("ow_dorpen", "Het Mijnguhdorp en het Vadsmijnstadje", "npc:mijnguh",
     ["ow_mijningang", "ow_mijnguhdorp", "ow_rugzakzadel", "ow_stadje", "ow_villager", "ow_werkplek"]),
    ("ow_toveren", "Glinsteren en toveren", "item:guhs:glinsteraltaar", ["ow_altaar", "ow_boekenkast"]),
    ("ow_gereedschap", "Mijnspullen", "item:guhs:vahoege_vadsknakker",
     ["ow_vadsknakker", "ow_vahoegietstaaf", "ow_super_vads", "ow_wolkenliftje", "ow_pluisladder", "ow_mijnlampje", "ow_terrarium"]),
    ("ow_boven", "Boven de wol: een vrolijker land", "item:guhs:hartjeslelie",
     ["ow_bloemenvallei", "ow_bronnetje", "ow_roze_zand", "ow_hartjeslelie"]),
]


def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    # --- the layers -------------------------------------------------------------------------------------------------------
    q("ow_viltsteen", "Onder de wol", "Graaf door de roze wol naar beneden. Een eindje onder het oppervlak houdt de wol op en begint de "
      "&dviltsteen&r: zacht, snel te hakken en de eerste laag van de Guhmensie-ondergrond. Hak er 16!", "guhs:viltsteen",
      [item("guhs:viltsteen", 16)], xp=50)
    q("ow_viltsteen_bouwen", "Vilt in alle vormen", "Viltsteen is ook een mooi bouwblok: gepolijst, als bakstenen, tegels of gebeiteld, met "
      "trapjes, plaatjes en muurtjes. Maak ze aan de werkbank of op de steenzaag. Begin met viltsteen bakstenen.",
      "guhs:viltsteen_bakstenen", [item("guhs:viltsteen_bakstenen", 4)], xp=50)
    q("ow_knuffelsteen", "Knuffelsteen in bulk", "Dieper wordt de steen harder: de &dknuffelsteen&r. Vroeger was die duur, nu hak je "
      "hem gewoon uit de grond. Onder bergen begint hij hoger, onder dalen en zee lager.", "guhs:knuffelsteen",
      [item("guhs:knuffelsteen", 16)], xp=50)
    q("ow_roze_knuffelsteen", "Roze aders", "Door de knuffelsteen lopen roze aders van &droze knuffelsteen&r, met hier en daar belletjes "
      "roze wol en rozenkwarts-druppels aan de wanden.", "guhs:roze_knuffelsteen", [item("guhs:roze_knuffelsteen", 8)], xp=50)
    q("ow_kristalsteen", "De diepe laag", "Nog dieper, waar de grotten gigantisch worden: de &dguhkristalsteen&r. Daaronder ligt alleen "
      "nog de &5knuffelnaad&r: daar kom je niet doorheen, njeg.", "guhs:guh_kristalsteen", [item("guhs:guh_kristalsteen", 16)], xp=100)
    q("ow_guhklei", "Guhklei", "In de viltsteen en de knuffelsteen zitten grote klodders &dguhklei&r. Hak je ze gewoon, dan krijg je "
      "kleiballetjes (voor bakstenen). Met Zijdezacht houd je het blok heel, en dat bak je tot roze terracotta. De Mijnwerker in het "
      "Vadsmijnstadje verkoopt het ook.", "guhs:guhklei", [item("guhs:guhklei")], xp=100)
    q("ow_viltkiezel", "Viltkiezel", "Langs beekjes en oevers ligt &dviltkiezel&r: zacht grind van viltbolletjes. Het valt net als grind.",
      "guhs:viltkiezel", [item("guhs:viltkiezel", 8)], xp=50)
    # --- the ores -----------------------------------------------------------------------------------------------------------
    q("ow_ijzer", "IJzer in de viltsteen", "Elke laag heeft zijn eigen ertsen: kolen, koper, ijzer, goud, lapis, redstone, diamant en "
      "smaragd, net als in de overworld, maar dan in viltsteen, knuffelsteen of kristalsteen. IJzer zit het meest rond de grens van "
      "viltsteen en knuffelsteen.", "guhs:ijzererts_vilt", [item("minecraft:raw_iron", 8)], xp=50)
    q("ow_vads", "Vads uit de steen", "Vads-erts zit in de knuffelsteen en de kristalsteen, het meest diep beneden. Dieper is rijker!",
      "guhs:vadserts_knuffel", [item("guhs:vahoege_vads", 4)], xp=100)
    q("ow_kaaskorrel", "Kaaskorrels", "Rond de sausmeren zit kaaskorrelerts. Hak het en je krijgt kaaskorrels.", "guhs:kaaskorrelerts_knuffel",
      [item("guhs:kaaskorrel", 4)], xp=50)
    q("ow_pigment", "Pastel uit de steen", "Pigmenterts in de knuffelsteen geeft pastelkleurstof, en dieper zitten de zeldzamere kleuren. "
      "Precies goed voor pastelbakstenen en pleister! Begin met roze.", "guhs:pigmenterts_knuffel", [item("minecraft:pink_dye", 4)], xp=50)
    q("ow_diamant", "Diamant in de kristalsteen", "Diamanterts zit vooral diep in de guhkristalsteen. Neem een goede houweel mee!",
      "guhs:diamanterts_kristal", [item("minecraft:diamond")], xp=150)
    q("ow_vahoegiet", "VAHOEGIET!", "Het zeldzaamste erts: &5Vahoegiet&r. Alleen in de guhkristalsteen, heel diep, en in de "
      "knuffelhart-geodes. Je hebt het nodig voor het Glinsteraltaar, het rugzakzadel, super vahoege vads en het mijn-guhhuisje.",
      "guhs:vahoegiet", [item("guhs:vahoegiet")], rewards=(("guhs:kaas_knabbels", 16),), shape="hexagon", xp=250)
    # --- the caves and who lives there ------------------------------------------------------------------------------------
    for b, titel, tekst in GROTTEN:
        q(f"ow_grot_{b}", titel, tekst, "minecraft:filled_map", [fq.biome(b)], rewards=(("guhs:kaas_knabbels", 8),), shape="circle", xp=100)
    q("ow_glitterbes", "Glitterbessen", "Pluk een glitterbes van een glitterrank in de Glitterspelonk (rechtsklik). Lekker glinsterig!",
      "guhs:glitterbes", [item("guhs:glitterbes", 4)], xp=50)
    q("ow_belletjes", "Ding dong guh", "Neem een belletjeskristal mee uit de Belletjesgrot en bouw thuis je eigen guh-xylofoon.",
      "guhs:belletjeskristal", [item("guhs:belletjeskristal", 4)], xp=50)
    q("ow_grotguh", "De Grotguh", "In de grotten wonen vooral gewone wilde guhs, maar heel soms zie je een &dGrotguh&r: donkerroze, met "
      "kristalspikkels die in het donker zacht oplichten. In de Glitterspelonk iets vaker. Kom dichtbij voor je Guhdex.",
      "guhs:guhdex", [adv("seen_grotguh")], rewards=(("guhs:kaas_knabbels", 16),), shape="rsquare", xp=150)
    q("ow_molguh", "Kopje!", "In de viltsteen- en knuffelsteengrotten steekt soms een &dMolguh&r met een brilletje zijn kopje uit de grond. "
      "Hij laat wel eens wat achter: guhklei, kleurstof of (heel soms) Vahoegiet. Kom dichtbij voor je Guhdex.",
      "guhs:das_molguh_spawn_egg", [adv("seen_das_molguh")], shape="rsquare", xp=100)
    # --- the buildings ---------------------------------------------------------------------------------------------------------
    for s, titel, tekst, *vorm in GEBOUWEN:
        q(f"ow_bouw_{s}", titel, tekst + " (Superkompas: Ondergrond.)", "guhs:guhmensie_superkompas", [fq.structure(s)],
          rewards=(("guhs:kaas_knabbels", 8),), shape=vorm[0] if vorm else "circle", xp=250 if vorm else 100)
    q("ow_sjabloon", "Een super sjabloon", "In de kisten van de oude guhtempel en de oude bankkluis ligt soms een &5super-vads-sjabloon&r. "
      "Zuinig op zijn! Kopiëren kan: 7 Vahoegiet en een guhkristalsteen.", "guhs:super_vads_sjabloon", [item("guhs:super_vads_sjabloon")],
      shape="hexagon", xp=250)
    # --- the Mijnguhdorp and the Vadsmijnstadje ---------------------------------------------------------------------------------
    q("ow_mijningang", "Het mijningang-hutje", "Boven de grond staat soms een hutje met een &dwolkenliftje&r: spring erop en je zweeft "
      "naar beneden. Hier begint je mijn! (Superkompas: Ondergrond.)", "guhs:wolkenliftje", [fq.structure("mijningang_hutje")], xp=50)
    q("ow_mijnguhdorp", "Het Mijnguhdorp", "Onder de grond ligt het dorp van de &dMijnguh&r (er ligt er altijd een niet al te ver van het midden "
      "van de wereld), rond een warm sausmeer, met een wolkenlift naar boven. Hij heeft een klusje voor je: zie &6Mijnguh-hulpje&r "
      "bij de kaasmijn. (Superkompas: Ondergrond.)", "guhs:kaasmijn_helm", [fq.structure("mijnguhdorp")], shape="octagon", xp=150)
    q("ow_rugzakzadel", "Een rugzak vol grond", "Maak een &6rugzakzadel&r (een zadel, een kist, roze wol en een Vahoegiet) en doe het je "
      "tamme guh om. Hij loopt met je mee en raapt alle gewone grondblokken op die je weghakt: wol, de laagstenen, aarde, zand... In "
      "zijn rugzak past eindeloos veel. In het schermpje haal je het eruit, of je stuurt alles naar je Bank Guh.",
      "guhs:rugzakzadel", [item("guhs:rugzakzadel")], xp=150)
    q("ow_stadje", "Het Vadsmijnstadje", "In een grote grotzaal ligt een heel stadje vol guh-villagers, met een Guhrad-centrale op het "
      "plein die het dorp van vadskracht voorziet. Er wonen vier nieuwe beroepen: de Toverguh, de Vadsmonteur, de Mijnwerker en de "
      "Kristalslijper. En de IJzerguhlem past op, net als een ijzergolem. (Superkompas: Ondergrond.)", "guhs:guhrad_werkbank",
      [fq.structure("vadsmijnstadje")], shape="octagon", xp=200)
    q("ow_villager", "Even oppakken", "Sluip en klik met een lege hand op een villager (een guh-villager of een gewone): je pakt hem op, "
      "met zijn naam, beroep en ruilaanbiedingen. Klik op een blok om hem weer neer te zetten. Handig bij het verhuizen!",
      "guhs:opgepakte_villager", [item("guhs:opgepakte_villager")], xp=100)
    q("ow_werkplek", "Een nieuw beroep", "Maak de werkplek van een van de nieuwe beroepen: de Toverguh-boekenstandaard, de Guhrad-werkbank, "
      "de Ertsenkist of de Kristalslijpsteen. Zet hem bij een villager zonder werk en hij krijgt dat beroep. Begin met de Guhrad-werkbank "
      "van de Vadsmonteur.", "guhs:guhrad_werkbank", [item("guhs:guhrad_werkbank")], xp=50)
    # --- enchanting -----------------------------------------------------------------------------------------------------------
    q("ow_altaar", "Het Glinsteraltaar", "Het guh-betoveraltaar: een boek, twee Vahoegiet, een guhkristal en drie guhkristalsteen. Net als "
      "een betoveringstafel kost het lapis en levels. Je krijgt de nuttige vanilla-betoveringen, en guh-spreuken die alleen hier te "
      "krijgen zijn: &dMika-mepper&r, &dPluisval&r, &dGlitterzicht&r, &dVadsvlijt&r en &dBakvads&r. De Toverguh verkoopt "
      "Spreukjesboekjes met nog meer: &dSpaarguhtje&r, &dAaizacht&r en &dKaasknabbelgeur&r.", "guhs:glinsteraltaar",
      [item("guhs:glinsteraltaar")], shape="hexagon", xp=200)
    q("ow_boekenkast", "Knuffelboekenkasten", "Het Glinsteraltaar wordt sterker van &dknuffelboekenkasten&r om hem heen (gewone "
      "boekenkasten tellen niet mee). Je ziet hartjes van de kasten naar het altaar vliegen. Maak er een paar van guhbloesemplanken, "
      "roze wol en boeken.", "guhs:knuffelboekenkast", [item("guhs:knuffelboekenkast", 6)], xp=100)
    # --- tools and handy things -----------------------------------------------------------------------------------------------
    q("ow_vadsknakker", "De Vahoege Vadsknakker", "Gooi een vahoege vadspaxel, een vahoege vadsschaar en een vahoege vads-schoffel samen "
      "aan de werkbank: houweel, bijl, schep, schoffel en schaar in één! Hun betoveringen gaan mee. Rechtsklik doet slim: gras wordt "
      "een akkertje, stammen worden gestript, een schaap wordt geknipt. Sluipen en rechtsklik op gras: een paadje.",
      "guhs:vahoege_vadsknakker", [item("guhs:vahoege_vadsknakker")], shape="hexagon", xp=200)
    q("ow_vahoegietstaaf", "De Vahoegietstaaf", "Vier Vahoegiet en vier vahoege vads-staven worden een &5Vahoegietstaaf&r.",
      "guhs:vahoegietstaaf", [item("guhs:vahoegietstaaf")], xp=150)
    q("ow_super_vads", "Super vahoege vads", "Op de smidstafel: het super-vads-sjabloon, een stuk vahoege vads en een Vahoegietstaaf. "
      "Zo maak je je vads-spullen super, zo sterk als netherite. Begin met een super vahoege vads-houweel.",
      "guhs:super_vahoege_vads_pickaxe", [item("guhs:super_vahoege_vads_pickaxe")], rewards=(("guhs:kaas_knabbels", 32),),
      shape="gear", xp=400)
    q("ow_wolkenliftje", "Het wolkenliftje", "Zet een wolkenliftje onderin je mijn en een recht erboven op de grond. Spring erop en je "
      "zweeft omhoog naar het volgende; sluip en je gaat omlaag.", "guhs:wolkenliftje", [item("guhs:wolkenliftje", 2)], xp=100)
    q("ow_pluisladder", "De pluisladder", "Zet een pluisladder tegen de wand aan de rand van een gat: hij rolt vanzelf uit tot de bodem.",
      "guhs:pluisladder", [item("guhs:pluisladder", 4)], xp=50)
    q("ow_mijnlampje", "Een lampje voor je guh", "Een roze helmpje met een lampje (lantaarn en roze wol). Zet het je guh op: hij geeft "
      "licht in het donker, waar hij ook loopt. Een lopend lampje!", "guhs:mijnlampje", [item("guhs:mijnlampje")], xp=100)
    q("ow_terrarium", "Een groter terrarium", "Terraria zijn nu zo groot als je wilt: alles wat met terrariumglas en terrariumbodem aan "
      "elkaar vast zit is één leefruimte, ook met gangetjes en in elke vorm. Maak terrariumbodem van terrariumglas, zand en gestripte eik.",
      "guhs:terrariumbodem", [item("guhs:terrariumbodem", 4)], xp=50)
    # --- the prettier surface --------------------------------------------------------------------------------------------------
    q("ow_bloemenvallei", "De Bloemenvallei", "Een kleine, zeldzame vallei vol bloemen in alle guh-kleuren, met bijtjes, vlindertjes en "
      "pluisjes in de lucht.", "minecraft:filled_map", [fq.biome("bloemenvallei")], rewards=(("guhs:kaas_knabbels", 8),), shape="circle",
      xp=100)
    q("ow_bronnetje", "Van bron naar bron", "De Guhmensie heeft nu beekjes: ze kronkelen van het ene bronnetje naar het andere, met "
      "roze glinsters boven het water en glinsterriet langs de kant. Neem een bronnetje mee voor je eigen vijver.", "guhs:bronnetje",
      [item("guhs:bronnetje")], xp=50)
    q("ow_roze_zand", "Een roze strandje", "Naast de gewone stranden liggen nu ook roze strandjes, met schelpjes. Schep wat roze zand.",
      "guhs:roze_zand", [item("guhs:roze_zand", 8)], xp=50)
    q("ow_hartjeslelie", "Hartjesmeertjes", "Van boven zie je het pas: sommige meertjes hebben de vorm van een hartje, met een bloemenrand. "
      "Pluk een hartjeslelie.", "guhs:hartjeslelie", [item("guhs:hartjeslelie")], xp=50)
