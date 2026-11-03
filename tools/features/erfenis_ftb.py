"""
1.9 "Een opknapper!": the FTB Quests chapter guhs_erfenis ("Bouwkundig rapport: vads"), the 76-quest version of
erfenis-voorstellen/ftb.md (round 15) plus one quest per Sherlock case (round 15: "Sherlock-zaken komen gewoon mee").

Nothing is locked (no deps): the sections only show the logical order. Task kinds:
  adv   hidden advancement guhs:quest/<key>, granted by Java (feature/erfenis/ftb/ErfenisFtb: it looks at the village state
        every second and on every ErfenisMomenten moment);
  stat  a custom statistic guhs:erfenis_<x> that the same Java keeps up to date (the quest book shows "n / goal");
  item  / dim  as everywhere.
Village rewards: Guhldenzakje (klein 100 / middel 500 / groot 2000 Guhlden) and Decobon (koppeling's items).

Run on its own (advancements + stat names into the lang files):  PYTHONPATH="tools;tools/features" python tools/features/erfenis_ftb.py
The chapter itself comes from tools/make_ftbquests.py (CHAPTERS["guhs_erfenis"]).
"""
import json
import os

FTB_CHAPTER = "guhs_erfenis"
FTB_PORTRAIT = "npc:makelaar_guhrrit"

K, GK, KR, VS = "guhs:kaas_knabbels", "guhs:gefrituurde_kaasknabbels", "guhs:guh_kristal", "guhs:vahoege_vads_ingot"
GZK, GZM, GZG, DB = "guhs:guhldenzakje_klein", "guhs:guhldenzakje", "guhs:guhldenzakje_groot", "guhs:decobon"

# the custom statistics (Java: ErfenisFtbFeature.STATS, same ids) -> their name in the statistics screen
STATS = {
    "erfenis_plukjes": "Plukjes opgeruimd",
    "erfenis_bewoners": "Bewoners in je dorp",
    "erfenis_bestellingen": "Bestellingen geleverd",
    "erfenis_hoeve": "Level van je Hoeve",
    "erfenis_kavels": "Kavels gekocht",
    "erfenis_planken": "Planken gezaagd in je dorp",
    "erfenis_baksteen": "Bakstenen gebakken in je dorp",
    "erfenis_naar_huis": "Bouwspul naar huis gestuurd",
    "erfenis_burenstempels": "Burenstempels",
    "erfenis_ansichtkaarten": "Ansichtkaarten van de makelaar",
    "erfenis_knusheid": "Knusheid van je dorp",
}

# the Sherlock cases in order (sherlock/Zaken.ALLE): id, title, what starts it (the last one is the finale: erfenis_bezwaar)
ZAKEN = [
    ("eieren", "De verdwenen eieren", "het Detectivebureau staat er", ((GZK, 1),)),
    ("kruimels", "Het kruimelspoor", "je bouwt een Bakkerij", ((K, 16),)),
    ("spook", "Het spook in het schuurtje", "je Hoeve wordt level 4", ((GZK, 1),)),
    ("wol", "De verdwenen wol", "een Weverij en 10 bestellingen", ((K, 16),)),
    ("kelder", "De kelder van Opa Knabbel", "je Hoeve wordt level 5", ((KR, 8),)),
    ("koeienbel", "De gestolen koeienbel", "je bouwt een Muziektent", ((GZM, 1),)),
    ("paaltjes", "De verschoven paaltjes", "je Hoeve wordt level 7", ((K, 32),)),
    ("licht", "Licht in de kelder", "je bouwt een Kaarsenmakerij", ((GZM, 1),)),
    ("blikje", "Het blikje onder het boompje", "je Hoeve wordt level 8", ((KR, 8),)),
    ("voorbereiding", "De voorbereiding", "meteen na het blikje", ((GZM, 1),)),
]

FTB_SECTIES = [
    ("erfenis_brief", "Een brief van de makelaar", "item:guhs:makelaarsbrief",
     ["erfenis_brief", "erfenis_koop", "erfenis_hekje", "erfenis_aankomst", "erfenis_overzicht", "erfenis_eerste_pluk",
      "erfenis_kippenhok", "erfenis_eerste_bewoner", "erfenis_eerste_bestelling"]),
    ("erfenis_hoeve", "De Hoeve", "npc:makelaar_guhrrit", [f"erfenis_hoeve_{n}" for n in range(2, 11)]),
    ("erfenis_bouwen", "Bouwen en land", "item:minecraft:iron_shovel",
     ["erfenis_timmer", "erfenis_ring_1", "erfenis_ring_2", "erfenis_ring_3", "erfenis_kavel", "erfenis_kavels_alle"]),
    ("erfenis_dieren", "Dieren", "item:minecraft:egg",
     ["erfenis_schaap", "erfenis_konijn", "erfenis_eend", "erfenis_koe", "erfenis_bij", "erfenis_wolkenschaap", "erfenis_mammoet"]),
    ("erfenis_akkers", "Akkers en ketens", "item:minecraft:wheat",
     ["erfenis_akker", "erfenis_boomgaard", "erfenis_molen", "erfenis_brood", "erfenis_koek", "erfenis_stof", "erfenis_kaas",
      "erfenis_kaars", "erfenis_reuzenwol"]),
    ("erfenis_blokken", "Bouwblokken voor thuis", "item:minecraft:bricks",
     ["erfenis_bosje", "erfenis_planken", "erfenis_groeve", "erfenis_baksteen", "erfenis_glas", "erfenis_kleurglas",
      "erfenis_werkplaats"]),
    ("erfenis_thuis", "Verbinding met thuis", "item:guhs:erfhekje",
     ["erfenis_leverkist", "erfenis_naar_huis", "erfenis_bulk_thuis", "erfenis_loket", "erfenis_gekleurde_wol",
      "erfenis_dorpszender", "erfenis_vadsmast"]),
    ("erfenis_dorp", "Bewoners en dorp", "item:minecraft:cake",
     ["erfenis_bewoners_10", "erfenis_bewoners_50", "erfenis_speeltuin", "erfenis_pension", "erfenis_cafe", "erfenis_feest"]),
    ("erfenis_buren", "Buren en bestellingen", "item:minecraft:map",
     ["erfenis_kaart", "erfenis_bezoek", "erfenis_bestellingen_100", "erfenis_bestellingen_400", "erfenis_hoger_dorp",
      "erfenis_sterbestelling", "erfenis_handje", "erfenis_goede_buur"]),
    ("erfenis_sherlock", "Sherlock Guh", "npc:sherlock_guh",
     ["erfenis_detective"] + [f"erfenis_zaak_{z[0]}" for z in ZAKEN] + ["erfenis_bezwaar"]),
    ("erfenis_deco", "Deco en Knusheid", "item:minecraft:pink_tulip",
     ["erfenis_deco", "erfenis_vondst", "erfenis_bedankje", "erfenis_kaartjes", "erfenis_fontein", "erfenis_knus_1000",
      "erfenis_knus_2500"]),
]

FTB_SLOT = ("Mika-strooptochten: binnenkort!", "npc:morris_mika",
            ["Morris-Mika heeft zijn Knabbelparkinkje...", "maar hij kijkt nog steeds heel vads naar jouw dorp.", "Njeg. Wordt vervolgd!"])

# every adv(...) key below: the hidden advancements guhs:quest/<key> (Java grants them: ErfenisFtb)
QUEST_ADV = ["erfenis_koop", "erfenis_overzicht", "erfenis_kippenhok", "erfenis_timmer",
             "erfenis_schaap", "erfenis_konijn", "erfenis_eend", "erfenis_koe", "erfenis_bij", "erfenis_wolkenschaap", "erfenis_mammoet",
             "erfenis_akker", "erfenis_boomgaard", "erfenis_molen", "erfenis_brood", "erfenis_koek", "erfenis_stof", "erfenis_kaas",
             "erfenis_kaars", "erfenis_reuzenwol",
             "erfenis_bosje", "erfenis_groeve", "erfenis_glas", "erfenis_kleurglas", "erfenis_werkplaats",
             "erfenis_naar_huis", "erfenis_loket", "erfenis_gekleurde_wol", "erfenis_vadsmast",
             "erfenis_speeltuin", "erfenis_pension", "erfenis_cafe", "erfenis_feest",
             "erfenis_kaart", "erfenis_bezoek", "erfenis_hoger_dorp", "erfenis_sterbestelling", "erfenis_handje",
             "erfenis_detective"] + [f"erfenis_zaak_{z[0]}" for z in ZAKEN] + ["erfenis_bezwaar",
             "erfenis_deco", "erfenis_vondst", "erfenis_bedankje", "erfenis_fontein"]

HOEVE = {  # level -> (title, text, rewards)
    2: ("Doei, makelaar!", "Je Hoeve op &elevel 2&r. Makelaar Guhrrit vindt het wel mooi zo en gaat met vakantie. Met een sombrero.",
        ((K, 16), (DB, 1))),
    3: ("Roze dakpannen", "Je Hoeve op &elevel 3&r: roze dakpannen! Vergeet je &dOpslagschuur&r niet: meer ruimte voor al je spulletjes.",
        ((GZK, 1),)),
    4: ("Een echt boerderijtje", "Je Hoeve op &elevel 4&r. Het begint ergens op te lijken, guh.", ((K, 16),)),
    5: ("Vakwerk met knuffeltjes", "Je Hoeve op &elevel 5&r: vakwerk, en overal knuffeltjes. Zo knus!", ((GZM, 1),)),
    6: ("Hoeve met allure", "Je Hoeve op &elevel 6&r. De buren kijken een beetje jaloers.", ((KR, 8),)),
    7: ("Een vleugje vads", "Je Hoeve op &elevel 7&r: goud en vads. Heel vahoeg.", ((VS, 4), (GZM, 1))),
    8: ("Herenboerderij", "Je Hoeve op &elevel 8&r. Een echte herenboerderij, met een guh als heer.", ((KR, 16),)),
    9: ("Pastel en lichtjes", "Je Hoeve op &elevel 9&r: pastelkleuren en lichtjes overal.", ((GZG, 1),)),
    10: ("De Grote Knuffelhoeve", "Je Hoeve op &elevel 10&r: de Grote Knuffelhoeve! Opa Knabbel zou zo trots zijn.",
         ((VS, 8), (GK, 32), (DB, 1))),
}


def ftb(fq):
    q, item, adv, stat = fq.q, fq.item, fq.adv, fq.stat

    def st(naam, n):
        return fq.met(stat(naam, n), STATS[naam])

    # --- 1. a letter from the estate agent ---------------------------------------------------------------------------------
    q("erfenis_brief", "Post voor guh!", "Ongeveer een halfuurtje speeltijd na je eerste bezoek aan de Guhmensie komt de &dPostguh&r "
      "langs met een brief van makelaar &dGuhrrit&r. Voor jou? Njeg!", "guhs:makelaarsbrief", [item("guhs:makelaarsbrief")])
    q("erfenis_koop", "Eén knabbel, wat een koopje", "Lees de brief en klik op &eKopen voor 1 kaasknabbel&r. Een hele boerderij voor "
      "één kaasknabbel! Bouwkundig rapport: vads. Emotioneel rapport: heel vads.", "guhs:kaas_knabbels", [adv("erfenis_koop")])
    q("erfenis_hekje", "Hekje naar je boerderij", "Bij de koop krijg je een &dErfhekje&r (en een Leverkist). Zet het hekje in je basis: "
      "dat is je deurtje naar je eigen boerderij.", "guhs:erfhekje", [item("guhs:erfhekje")])
    q("erfenis_aankomst", "Bouwkundig rapport: vads", "Loop door het Erfhekje. Tja... het is een opknappertje. Struiken, brandnetels, "
      "molshopen. Maar de &dHoeve&r staat er nog!", "guhs:erfhekje", [fq.dim("guhs:erfenis")], rewards=((GZK, 1),), shape="gear", xp=100)
    q("erfenis_overzicht", "Van bovenaf", "Druk op &eO&r: het &dbovenaanzicht&r van je boerderij. Daar bouw, ruim en regel je alles.",
      "minecraft:spyglass", [adv("erfenis_overzicht")])
    q("erfenis_eerste_pluk", "Wieden maar", "Ruim in het bovenaanzicht een &dplukje&r op: een struik, een rotsblok, een oude kar. "
      "Soms ligt er iets leuks onder!", "minecraft:sweet_berries", [st("erfenis_plukjes", 1)])
    q("erfenis_kippenhok", "Tok tok, guh", "Bouw je eerste &dKippenhok&r. Het eerste is gratis!", "minecraft:egg", [adv("erfenis_kippenhok")])
    q("erfenis_eerste_bewoner", "Een nieuwe buurguh", "Bouw een &dGuhhutje&r: dan trekt er vanzelf een bewoner in. Bewoners werken in "
      "je gebouwen.", "guhs:guh_spawn_egg", [st("erfenis_bewoners", 1)])
    q("erfenis_eerste_bestelling", "Karretje op pad", "Lever een eerste bestelling aan een buurdorpje (het &dMarktplein&r, of klik op een "
      "karretje op de wereldkaart).", "minecraft:minecart", [st("erfenis_bestellingen", 1)], rewards=((GZK, 1),))

    # --- 2. the Hoeve ------------------------------------------------------------------------------------------------------
    for n, (titel, tekst, beloning) in HOEVE.items():
        q(f"erfenis_hoeve_{n}", titel, tekst, "minecraft:hay_block", [st("erfenis_hoeve", n)], rewards=beloning,
          shape="gear" if n == 10 else None, xp=50 * n)

    # --- 3. building and land ----------------------------------------------------------------------------------------------
    q("erfenis_timmer", "Twee handjes extra", "Bouw een &dTimmerschuur&r: een tweede bouwer! Op level 7 komt er nog een derde bij "
      "(met koffiepauze).", "minecraft:crafting_table", [adv("erfenis_timmer")], rewards=((GZK, 1),))
    q("erfenis_ring_1", "Opgeruimd staat netjes", "Ruim &e16 plukjes&r op: de hele binnenring is schoon.", "minecraft:sweet_berries",
      [st("erfenis_plukjes", 16)], rewards=((K, 16),))
    q("erfenis_ring_2", "Tot aan de bosrand", "Ruim &e40 plukjes&r op.", "minecraft:oak_sapling", [st("erfenis_plukjes", 40)],
      rewards=((GZM, 1),))
    q("erfenis_ring_3", "Geen brandnetel meer", "Ruim &ealle 72 plukjes&r op. Geen brandnetel te bekennen!", "minecraft:short_grass",
      [st("erfenis_plukjes", 72)], rewards=((KR, 8),), shape="hexagon")
    q("erfenis_kavel", "Grond erbij!", "Koop in het bovenaanzicht een eerste &dkavel&r grond erbij.", "minecraft:grass_block",
      [st("erfenis_kavels", 1)], rewards=((K, 16),))
    q("erfenis_kavels_alle", "Grootgrondguh", "Koop &ealle 16 kavels&r. Zo ver je kunt kijken: allemaal van jou!", "minecraft:filled_map",
      [st("erfenis_kavels", 16)], rewards=((GZG, 1),), shape="gear", xp=300)

    # --- 4. animals --------------------------------------------------------------------------------------------------------
    q("erfenis_schaap", "Bleh zei het schaapje", "Bouw een &dSchaapskooi&r: wol!", "minecraft:white_wool", [adv("erfenis_schaap")])
    q("erfenis_konijn", "Pluizige oortjes", "Bouw een &dKonijnenren&r: angorapluis.", "minecraft:rabbit_hide", [adv("erfenis_konijn")])
    q("erfenis_eend", "Kwak, guh", "Bouw een &dEendenvijver&r: dons en eendeneitjes.", "minecraft:feather", [adv("erfenis_eend")])
    q("erfenis_koe", "Boe-guh", "Bouw een &dKoeienstal&r: melk!", "minecraft:milk_bucket", [adv("erfenis_koe")])
    q("erfenis_bij", "Zoemguhs", "Bouw een &dBijenhof&r: honing en was.", "minecraft:honeycomb", [adv("erfenis_bij")], rewards=((K, 16),))
    q("erfenis_wolkenschaap", "Wolkjes op de wei", "Een &dSchaapskooi op level 7&r (en je Hoeve ook op 7): dan komen de "
      "&dwolkenschaapjes&r, met extra pluis.", "minecraft:white_wool", [adv("erfenis_wolkenschaap")], rewards=((K, 16),))
    q("erfenis_mammoet", "Een heel wollige verrassing", "Bouw de &dMammoetwei&r. Met genoeg vadskracht slapen de mammoetjes niet, en "
      "maken ze bergen reuzenwol.", "minecraft:brown_wool", [adv("erfenis_mammoet")], rewards=((VS, 8),), shape="gear", xp=300)

    # --- 5. fields and chains ----------------------------------------------------------------------------------------------
    q("erfenis_akker", "Zaaien maar", "Bouw een &dAkker&r en kies een gewas: graan, roze graan, wortels, bloemen of pluisbolletjes.",
      "minecraft:wheat_seeds", [adv("erfenis_akker")])
    q("erfenis_boomgaard", "Bessen plukken", "Bouw een &dBoomgaard&r: knabbelbessen en fruit.", "guhs:knabbelbessen",
      [adv("erfenis_boomgaard")], rewards=((K, 16),))
    q("erfenis_molen", "De molen draait", "Maal je eerste &dmeel&r in de &dMolen&r.", "minecraft:sugar", [adv("erfenis_molen")])
    q("erfenis_brood", "Vers van de bakker", "Bak je eerste &dbrood&r in de &dBakkerij&r.", "minecraft:bread", [adv("erfenis_brood")])
    q("erfenis_koek", "Een koekje bij de koffie", "Bak je eerste &dkoek&r in de Bakkerij.", "minecraft:cookie", [adv("erfenis_koek")],
      rewards=((K, 16),))
    q("erfenis_stof", "Weef, weef", "Weef je eerste &dstof&r in de &dWeverij&r.", "minecraft:white_carpet", [adv("erfenis_stof")])
    q("erfenis_kaas", "Kaasmaker-guh", "Maak je eerste &dkaas&r in de &dKaasmakerij&r.", "guhs:gatenkaas", [adv("erfenis_kaas")],
      rewards=((K, 16),))
    q("erfenis_kaars", "Kaarsjes aan", "Maak je eerste &dkaarsen&r in de &dKaarsenmakerij&r (die werkt op vadskracht).",
      "minecraft:candle", [adv("erfenis_kaars")], rewards=((KR, 4),))
    q("erfenis_reuzenwol", "Zestien in één keer", "Je eerste &dreuzenwol&r van de mammoetjes.", "minecraft:brown_wool",
      [adv("erfenis_reuzenwol")], rewards=((KR, 8),))

    # --- 6. building blocks for home ---------------------------------------------------------------------------------------
    q("erfenis_bosje", "Hout voor de kachel", "Bouw een &dBosje&r en een &dHoutzagerij&r.", "minecraft:oak_log", [adv("erfenis_bosje")])
    q("erfenis_planken", "Zaagsel overal", "Zaag &e500 planken&r in je Houtzagerij.", "minecraft:oak_planks", [st("erfenis_planken", 500)],
      rewards=((K, 16),))
    q("erfenis_groeve", "Steen voor steen", "Bouw een &dGroeve&r: steen en klei.", "minecraft:cobblestone", [adv("erfenis_groeve")])
    q("erfenis_baksteen", "Gebakken en wel", "Bak &e200 bakstenen&r in de &dSteenbakkerij&r.", "minecraft:bricks",
      [st("erfenis_baksteen", 200)], rewards=((K, 16),))
    q("erfenis_glas", "Glashelder", "Bouw een &dZandkuil&r en een &dGlasblazerij&r.", "minecraft:glass", [adv("erfenis_glas")],
      rewards=((K, 16),))
    q("erfenis_kleurglas", "Een kleurtje erbij", "Kies in de Kaarsenmakerij een &dkleur&r voor je kaarsjes. Kleur maken kost "
      "vadskracht!", "minecraft:pink_candle", [adv("erfenis_kleurglas")], rewards=((KR, 4),))
    q("erfenis_werkplaats", "Kies je blokjes", "Kies in de &dBouwblokkenwerkplaats&r welke Bouwkeet-blokken hij maakt (iets anders "
      "dan alleen de roze pastelbaksteen).", "guhs:pastelbaksteen_roze", [adv("erfenis_werkplaats")], rewards=((K, 16),))

    # --- 7. the link with home ---------------------------------------------------------------------------------------------
    q("erfenis_leverkist", "Een Leverkist", "Een &dLeverkist&r krijg je bij de koop, en je kunt er meer maken. Daar komt de post uit "
      "je dorp in.", "guhs:leverkist", [item("guhs:leverkist")])
    q("erfenis_naar_huis", "Naar huis sturen", "Stuur spulletjes uit je dorp naar je &dLeverkist&r (knop &eNaar huis sturen&r).",
      "guhs:leverkist", [adv("erfenis_naar_huis")], rewards=((K, 16),))
    q("erfenis_bulk_thuis", "Bouwpakket", "Stuur &e5.000&r stammen, klei, zand, glasscherven en Bouwkeet-blokken naar huis.",
      "minecraft:oak_log", [st("erfenis_naar_huis", 5000)], rewards=((KR, 8),), shape="hexagon")
    q("erfenis_loket", "Aan het loket", "Lever iets uit je zakken in bij het &dBouwmateriaal-loket&r. Grote upgrades vragen spullen "
      "uit de echte wereld.", "minecraft:chest", [adv("erfenis_loket")], rewards=((GZM, 1),))
    q("erfenis_gekleurde_wol", "Plons, kleurtje terug", "Verf witte wol uit je dorp thuis (verfbad, kleurmolen) en lever de &dgekleurde "
      "wol&r in bij het loket.", "minecraft:pink_wool", [adv("erfenis_gekleurde_wol")], rewards=((KR, 4),))
    q("erfenis_dorpszender", "De Dorpszender", "Maak een &dDorpszender&r (een Vadszender met kristallen en een blok kaasknabbels) en zet "
      "hem op je vadskrachtnet.", "guhs:dorpszender", [item("guhs:dorpszender")], rewards=((VS, 4),))
    q("erfenis_vadsmast", "Vonkjes in het dorp", "Bouw de &dVadsmast&r en laat er &e40 VK&r aankomen van je Dorpszender.",
      "guhs:dorpszender", [adv("erfenis_vadsmast")], rewards=((KR, 8),))

    # --- 8. residents and village ------------------------------------------------------------------------------------------
    q("erfenis_bewoners_10", "Een gezellig straatje", "&e10 bewoners&r in je dorp.", "guhs:guh_spawn_egg", [st("erfenis_bewoners", 10)],
      rewards=((K, 16),))
    q("erfenis_bewoners_50", "Een echt dorp", "&e50 bewoners&r! Een echt dorp.", "guhs:guh_spawn_egg", [st("erfenis_bewoners", 50)],
      rewards=((GZM, 1),))
    q("erfenis_speeltuin", "Wie wil er schommelen?", "Bouw een &dSpeeltuin&r: daar komen nog meer bewoners op af.", "minecraft:oak_fence",
      [adv("erfenis_speeltuin")], rewards=((K, 16),))
    q("erfenis_pension", "Op logeerpartij", "Bouw een &dGuhpension&r en laat een eigen guh logeren.", "minecraft:red_bed",
      [adv("erfenis_pension")], rewards=((K, 16),))
    q("erfenis_cafe", "Koffie in De Vadse Koe", "Bouw het &dDorpscafé&r: iedereen werkt een beetje harder met koffie.",
      "minecraft:bell", [adv("erfenis_cafe")], rewards=((K, 16),))
    q("erfenis_feest", "Dorpsfeest!", "Bouw de &dMuziektent&r. Muziek, taart, feest! (En alles gaat net iets sneller.)",
      "minecraft:jukebox", [adv("erfenis_feest")], rewards=((GZM, 1),))

    # --- 9. neighbours and orders ------------------------------------------------------------------------------------------
    q("erfenis_kaart", "Op de kaart", "Druk op &eM&r: de &dwereldkaart&r met alle dorpjes en karretjes.", "minecraft:map",
      [adv("erfenis_kaart")])
    q("erfenis_bezoek", "Even kijken bij de buren", "Ga op bezoek in een ander dorp.", "minecraft:oak_door", [adv("erfenis_bezoek")])
    q("erfenis_bestellingen_100", "Vaste klant", "Lever &e100 bestellingen&r.", "minecraft:minecart", [st("erfenis_bestellingen", 100)],
      rewards=((GZM, 1),))
    q("erfenis_bestellingen_400", "Leverancier van het jaar", "Lever &e400 bestellingen&r. Wat een karretjes!", "minecraft:minecart",
      [st("erfenis_bestellingen", 400)], rewards=((GZG, 1),), shape="hexagon", xp=300)
    q("erfenis_hoger_dorp", "Voor de grote buren", "Lever een bestelling aan een dorp dat &ehoger&r is dan het jouwe.",
      "minecraft:chest_minecart", [adv("erfenis_hoger_dorp")], rewards=((K, 16),))
    q("erfenis_sterbestelling", "Het gouden karretje", "Elke dag is er één &6gouden ster&r-bestelling. Lever er een!",
      "minecraft:gold_ingot", [adv("erfenis_sterbestelling")], rewards=((DB, 1),))
    q("erfenis_handje", "Handje helpen", "Klik op de wereldkaart bij het dorp van een andere speler op &eHandje helpen&r.",
      "minecraft:poppy", [adv("erfenis_handje")], rewards=((K, 16),))
    q("erfenis_goede_buur", "Goede buur", "Verzamel &e25 burenstempels&r met Handje helpen.", "minecraft:poppy",
      [st("erfenis_burenstempels", 25)], rewards=((DB, 1), (KR, 4)), shape="hexagon")

    # --- 10. Sherlock Guh --------------------------------------------------------------------------------------------------
    q("erfenis_detective", "Een eigen speurbureau", "Bouw een &dDetectivebureau&r. Sherlock Guh en Dokter Vadson trekken in, en je "
      "krijgt een vergrootglas!", "guhs:sherlock_vergrootglas", [adv("erfenis_detective")], rewards=((K, 16),))
    for i, (zid, titel, wanneer, beloning) in enumerate(ZAKEN):
        q(f"erfenis_zaak_{zid}", f"Zaak {i + 1}: {titel}", f"Los de zaak &d{titel}&r op. Hij begint als {wanneer} (en de zaak ervoor "
          "is opgelost). Speuren, getuigen horen, draadjes spannen: wie was het?", "guhs:sherlock_vergrootglas",
          [adv(f"erfenis_zaak_{zid}")], rewards=beloning, xp=100 + 25 * i)
    q("erfenis_bezwaar", "BEZWAAR, NJEG!", "De &dGrote Knabbelzaak&r: de rechtszaak tegen Morris-Mika (Hoeve 9 en De voorbereiding). "
      "Kies het juiste bewijs en roep: BEZWAAR, NJEG!", "guhs:detective_pet", [adv("erfenis_bezwaar")], rewards=((GZG, 1), (DB, 1)),
      shape="gear", xp=500)

    # --- 11. deco and Knusheid ---------------------------------------------------------------------------------------------
    q("erfenis_deco", "Een vleugje knus", "Zet je eerste &ddecostukje&r neer.", "minecraft:pink_tulip", [adv("erfenis_deco")])
    q("erfenis_vondst", "Wat lag hier nou?", "Vind bij het opruimen een oud &ddecostukje&r (of de oude tractor).", "minecraft:brush",
      [adv("erfenis_vondst")], rewards=((K, 16),))
    q("erfenis_bedankje", "Een bedankje van de buren", "Soms stuurt een buurdorpje na een bestelling een &ddecostukje&r als "
      "bedankje. Krijg er een!", "minecraft:poppy", [adv("erfenis_bedankje")], rewards=((K, 16),))
    q("erfenis_kaartjes", "Groetjes van de makelaar", "Verzamel alle &e8 ansichtkaarten&r van makelaar Guhrrit: 3 onder de plukjes, 5 "
      "stuurt hij per Postguh vanaf zijn vakantie (Hoeve 3 t/m 7). Ze komen op het kaartjesbord.", "minecraft:paper", [st("erfenis_ansichtkaarten", 8)], rewards=((DB, 1),))
    q("erfenis_fontein", "Spuitguh", "Zet de &dSpuitguh-fontein&r in je dorp (decowinkel, vanaf Hoeve 6). Pfffft!", "minecraft:water_bucket",
      [adv("erfenis_fontein")], rewards=((K, 16),))
    q("erfenis_knus_1000", "Heel knus", "&e1.000 Knusheid&r: gebouwen, paadjes en deco.", "minecraft:pink_tulip",
      [st("erfenis_knusheid", 1000)], rewards=((GZM, 1),))
    q("erfenis_knus_2500", "Knusser bestaat niet", "&e2.500 Knusheid&r. Knusser bestaat echt niet.", "minecraft:pink_tulip",
      [st("erfenis_knusheid", 2500)], rewards=((GZG, 1), (DB, 1)), shape="gear", xp=500)


TEKSTEN = {f"stat.guhs.{k}": v for k, v in STATS.items()}


def bestanden():
    return {f"data/guhs/advancement/quest/{naam}.json": {"criteria": {"done": {"trigger": "minecraft:impossible"}}} for naam in QUEST_ADV}


def build(h):
    for pad, obj in bestanden().items():
        h.w(h.D + pad[pad.index("/", pad.index("/") + 1):], obj)
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _zelf():
    root = os.path.join("src", "main", "resources")
    for pad, obj in bestanden().items():
        vol = os.path.join(root, *pad.split("/"))
        os.makedirs(os.path.dirname(vol), exist_ok=True)
        with open(vol, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(root, "assets", "guhs", "lang", taal + ".json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"erfenis_ftb: {len(QUEST_ADV)} advancements, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _zelf()
