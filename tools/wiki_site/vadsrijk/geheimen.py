"""
U-SITE round 3: every hidden thing of the Vadsrijk WITH its location, each behind its own spoiler click (user decision 2026-11-08,
replaces the old "secrets never on the map" rule).

Covered (zones 1-6): hidden knuffels, treasures, clues / torn pages / letters / tablets / reliefs, puzzles (incl. the zone-6 star
beacons and the cave puzzles), cave treasures, viewpoints and campfires. Each entry: what it is, where (area + coordinates), how to get
there (the game's own hint text where it has one), the reward, and a "Toon op kaart" link (kaart.html?x=..&z=..&geheim=1). The page
shows only "<kind> #n: klik om te onthullen" until the reader opens one. The map gets a separate layer "Geheimen tonen (spoilers!)",
OFF by default, and its markers are never in the map search.

Sources: the positions = the POI files (and the committed Z6WereldPlekken.java); which POI is which secret and its name/text = the
mod's own *Plekken.java maps (KNUFFEL, SCHAT, AANWIJZING, RELIEF, MISTBRIEVEN, FLESSENPOST, the zone-6 tablets) + the lang file;
rewards = the find handlers (NpcHaken / Z?Haken / Grotten: coins below, Episch for the one special chest per zone).
"""
import os
import re

from .pages import ESC, t, p

JAVA = ("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
PLEKKEN = {"zone1": "npc/NpcPlekken.java", "zone2": "zone2/npc/Z2NpcPlekken.java", "zone3": "zone3/npc/Z3NpcPlekken.java",
           "zone4": "zone4/verhaal/Z4VerhaalPlekken.java", "zone5": "zone5/verhaal/Z5VerhaalPlekken.java", "zone6": "zone6/verhaal/Z6VerhaalPlekken.java"}
NS = {"zone1": "vadsrijk.npc", "zone2": "vadsrijk.z2npc", "zone3": "vadsrijk.z3npc", "zone4": "vadsrijk.z4verhaal", "zone5": "vadsrijk.z5verhaal",
      "zone6": "vadsrijk.z6verhaal"}
# map name in *Plekken.java -> kind
SOORT_VAN = {"KNUFFEL": "knuffel", "SCHAT": "schat", "AANWIJZING": "aanwijzing", "RELIEF": "relief", "MISTBRIEVEN": "brief", "FLESSENPOST": "brief",
             "TABLET": "brief", "UITKIJK": "uitkijk"}
# treasure coins per zone + the one Episch chest (NpcHaken / Z2Haken ... Z6Haken "schatten")
SCHAT_MUNTEN = {"zone1": 2500, "zone2": 6000, "zone3": 9000, "zone4": 15000, "zone5": 15000, "zone6": 20000}
SCHAT_EPISCH = {"kristalkamer", "burchtdak", "onder_de_tak", "mist", "staartvin", "mesa"}
SOORTEN = [  # kind, en, nl (headers + summaries)
    ("verborgen", "Secret quests, bosses and places", "Geheime quests, bazen en plekken"),
    ("knuffel", "Hidden knuffels", "Verborgen knuffels"), ("schat", "Treasures", "Schatten"),
    ("aanwijzing", "Clues and traces", "Aanwijzingen en sporen"), ("relief", "Reliefs", "Reliëfs"),
    ("brief", "Letters, notes and tablets", "Brieven, briefjes en tabletten"), ("puzzel", "Puzzles and star beacons", "Puzzels en sterrenbakens"),
    ("grotschat", "Cave treasures", "Grotschatten"), ("uitkijk", "Viewpoints", "Uitkijkpunten"), ("kampvuur", "Campfires", "Kampvuren")]
ENKEL = {"knuffel": ("Hidden knuffel", "Verborgen knuffel"), "schat": ("Treasure", "Schat"), "aanwijzing": ("Clue", "Aanwijzing"),
         "relief": ("Relief", "Reliëf"), "brief": ("Letter", "Briefje"), "puzzel": ("Puzzle", "Puzzel"), "grotschat": ("Cave treasure", "Grotschat"),
         "uitkijk": ("Viewpoint", "Uitkijkpunt"), "kampvuur": ("Campfire", "Kampvuur"), "verborgen": ("Secret", "Geheim")}
# the generated map classes (the same lists as POI_*.md; at runtime the *Plekken classes look ids up here first)
WERELD = {"zone1": "wereld/WereldPlekken.java", "zone2": "zone2/wereld/Z2WereldPlekken.java", "zone3": "zone3/wereld/Z3WereldPlekken.java",
          "zone4": "zone4/wereld/Z4WereldPlekken.java", "zone5": "zone5/wereld/Z5WereldPlekken.java", "zone6": "zone6/wereld/Z6WereldPlekken.java"}
Z2_DX, Z2_DZ = 704, 1344   # indeling/VadsrijkZones: zone 2's v1 numbers (Z2NpcPlekken.p) are shifted by this
# 1.10.21: the secret IJsgrot's way in is the scaffolding shaft through the dome's roof (EINDRAPPORT_RONDE_W, SPIEKBRIEF 1.12);
# 1.10.22: the map's geheim_ijsgrot_ingang is now that roof hole too (the spot on the dome top next to the hole 466,113,-1064)
IJSGROT_DAKGAT = (465, 114, -1064)
IJSGROT_TUNNEL = (453, 99, -1066)   # the old tunnel mouth high above the lake (no map id any more)

# ---- 1.10.21: hidden quests, hidden bosses and secret places (kind "verborgen"; truth = the Java named per entry) -------------------
# plek / plekken: (zone, map id or "arena:<runtime arena id>", fallback xyz); the first one that resolves wins.
# quest: its xp/coins/loot come from the quest JSON; baas: its named items come from the *Loot.java Benoemd lines (bron._benoemd);
# huisdier: the pet of its first kill; boek: the Verkenningsboek row (category, row id) that counts it.
VERBORGEN = [
    # ======== zone 1 ========
    dict(zone="zone1", id="mika_spion", quest="geheim_spion", baas="mika_spion", boek=("baas", "baas_mika_spion"),
         soort=("Secret quest + secret boss", "Geheime quest + geheime baas"),
         naam=("Who is Akim the Cheese Guh? (the Mika spy)", "Wie is Akim de Kaasguh? (de Mika-spion)"),
         hint=("Something is off at the Zwamzoet market.", "Er klopt iets niet op de markt van Zwamzoet."),
         plek=("zone1", "mika_spion_kraam", (-28, 64, 52)),
         plekken=[(("Cap south-west", "Hoed zuidwest"), ("zone1", "hoed_zuidw", (-78, 122, 32))),
                  (("Cap west", "Hoed west"), ("zone1", "hoed_west", (-66, 144, -30))),
                  (("Cap east", "Hoed oost"), ("zone1", "hoed_oost", (66, 154, -36)))],
         tekst=("Find one of the five clues of zone 1 (the Clues list above) and the secret quest starts by itself. With all five, "
                "walk to Akim the Cheese Guh at his cheese stall on the Zwamzoet market and talk to him: you unmask him. He is a "
                "Mika (lv 15)! He leaps up the mushroom caps and you chase him over three caps (the Zwamzoet guhs shoot you after "
                "him). Grey circles = de-vads powder: get out. At 15 % he floats off on a balloon: pop it quickly, or he escapes "
                "(he is back in disguise 2 minutes later). After a capture he returns 10 minutes later, so friends can do it too. "
                "Hand the quest in with Grand Master Vadsvoet.",
                "Vind één van de vijf aanwijzingen van zone 1 (zie Aanwijzingen hierboven): dan begint de geheime quest vanzelf. "
                "Heb je ze alle vijf? Loop naar Akim de Kaasguh bij zijn kaaskraam op de markt van Zwamzoet en praat met hem: je "
                "ontmaskert hem. Het is een Mika (lv 15)! Hij springt de paddenstoelhoeden op en jij gaat erachteraan, over drie "
                "hoeden (de guhs van Zwamzoet schieten je met een sporenkatapult achter hem aan). Grijze cirkels = "
                "ontvadsings-poeder: eruit! Bij 15 % zweeft hij weg aan een ballon: prik die snel kapot, anders ontsnapt hij (twee "
                "minuten later staat hij weer verkleed op de markt). Gevangen? Na 10 minuten is hij terug, zodat vriendjes het ook "
                "kunnen doen. Je levert de quest in bij Grootmeester Vadsvoet.")),
    dict(zone="zone1", id="tempelkamer", soort=("Secret room", "Geheime kamer"),
         naam=("The hidden temple room", "De geheime tempelkamer"),
         hint=("A crack in a wall of the Zwamtempel.", "Een barst in een muur van de Zwamtempel."),
         plek=("zone1", "geheim_tempelkamer", (311, 152, -416)),
         plekken=[(("The crumbled gap", "De bres in de muur"), ("zone1", "geheim_tempelkamer_muur", (317, 152, -416)))],
         tekst=("In the first room of the Zwamtempel (the Lichtzwamkamer, behind the big temple door that opens from lv 16) the west wall is cracked. "
                "Low down there is a crumbled gap, two blocks high: squeeze through it and you stand in a quiet little room "
                "with a chest and a lantern.",
                "In de eerste kamer van de Zwamtempel (de Lichtzwamkamer, achter de grote tempeldeur die vanaf lv 16 opengaat) zit een barst in de "
                "westmuur. Onderin is een bres van twee blokken hoog gebrokkeld: wurm je erdoor en je staat in een stil "
                "kamertje met een kist en een lantaarn."),
         beloning=("Just the joy of finding it: the chest is decoration (there is no storage in the Vadsrijk).",
                   "Alleen de pret van het vinden: de kist is versiering (in het Vadsrijk kun je niets opbergen).")),
    dict(zone="zone1", id="kristalkamer", soort=("Secret room", "Geheime kamer"), quest="z09_kristal",
         naam=("The hidden crystal chamber", "De verborgen kristalkamer"),
         hint=("Something sparkles very deep in the Druipgrotten.", "Heel diep in de Druipgrotten glinstert iets."),
         plek=("zone1", "kristalkamer", (-490, 46, -520)),
         plekken=[(("Cave mouth 1", "Grotingang 1"), ("zone1", "grot_ingangen.1", (-300, 90, -220))),
                  (("Cave mouth 2", "Grotingang 2"), ("zone1", "grot_ingangen.2", (-420, 83, -300))),
                  (("Cave mouth 3", "Grotingang 3"), ("zone1", "grot_ingangen.3", (-180, 111, -360)))],
         tekst=("A sparkling cave of amethyst and guh crystal in the farthest corner of the Druipgrotten. Go in through one of "
                "the cave mouths and keep going down and west. Inside: the treasure 'The crystal chamber' (Epic!), the Mika clue "
                "'scratches' (a black carpet and a Mika stone), and elite crystal creatures that guard it. Professor Pluis's side "
                "quest asks you to find it.",
                "Een fonkelende grot van amethist en guhkristal in het verste hoekje van de Druipgrotten. Ga naar binnen via "
                "één van de grotingangen en blijf omlaag en naar het westen lopen. Binnen: de schat 'De kristalkamer' (Episch!), "
                "de Mika-aanwijzing 'krassen' (een zwart kleedje en een Mika-steen) en elite kristalwezens die alles bewaken. De "
                "zijquest van Professor Pluis vraagt je hem te vinden."),
         beloning=("The Epic treasure of zone 1 + a clue for the Mika spy + the side quest goal.",
                   "De Epische schat van zone 1 + een aanwijzing voor de Mika-spion + het doel van de zijquest.")),
    # ======== zone 2 ========
    dict(zone="zone2", id="boormeester", quest="z2_geheim_boormeester", baas="boormeester", boek=("baas", "baas_boormeester"),
         soort=("Secret quest + secret boss", "Geheime quest + geheime baas"),
         naam=("The hum in the mountain (the Mika drill master)", "Het gebrom in de berg (de Mika-boormeester)"),
         hint=("A deep hum under the Splinterkloof.", "Een diep gebrom onder de Splinterkloof."),
         plek=("zone2", "arena_boormeester", (674, 66, -1270)),
         plekken=[(("The hidden drill hall (door)", "De verborgen boorhal (deur)"), ("zone2", "arena_boormeester_deur", (674, 66, -1215))),
                  (("Clue tower (east)", "Boortoren (oost)"), ("zone2", "boortoren_3", (908, 144, -1178)))],
         tekst=("Find one of the five drill traces of zone 2 (the Clues list above) and the secret quest starts by itself. All "
                "five point to the bottom of the Splinterkloof: the hidden drill hall in its north wall opens for you. Inside "
                "sleeps a huge drilling machine with a Mika in it (lv 28): right-click it to wake it. Hit the three drill heads "
                "until they overheat, in the order the control panel plays (light + tone, every 15 s). Wrong order = steam! "
                "Beaten, he races off on a mine cart; the machine is back 10 minutes later (within 30 s if you still need him). "
                "Hand the quest in with Inventor Tandwiel.",
                "Vind één van de vijf boorsporen van zone 2 (zie Aanwijzingen hierboven): dan begint de geheime quest vanzelf. "
                "Alle vijf wijzen naar de bodem van de Splinterkloof: de verborgen boorhal in de noordwand gaat voor je open. "
                "Binnen slaapt een enorme boormachine met een Mika erin (lv 28): rechtsklik om hem te wekken. Sla de drie "
                "boorkoppen tot ze oververhit raken, in de volgorde die het paneel laat zien (licht + toon, elke 15 s). "
                "Verkeerde volgorde = stoom! Verslagen rijdt hij weg in een mijnkarretje; na 10 minuten staat de machine er weer "
                "(binnen 30 s als je hem nog nodig hebt). Je levert de quest in bij Uitvinder Tandwiel.")),
    dict(zone="zone2", id="ijsgrot", soort=("Secret place", "Geheime plek"),
         naam=("The secret Ice Cave", "De geheime IJsgrot"),
         hint=("A dome of blue ice in the Splinterpieken.", "Een koepel van blauw ijs in de Splinterpieken."),
         plek=("zone2", "geheim_ijsgrot", (464, 105, -1066)),
         plekken=[(("Way in: the hole with scaffolding in the roof", "Ingang: het gat met steigerwerk in het dak"), ("zone2", "geheim_ijsgrot_ingang", IJSGROT_DAKGAT)),
                  (("The old tunnel mouth (high above the lake)", "De oude tunnelingang (hoog boven het meer)"),
                   ("zone2", "", IJSGROT_TUNNEL))],
         tekst=("Climb onto the ice dome. In its roof there is a hole with scaffolding going down: on scaffolding, jump = up and "
                "sneak = down. Inside the blue cave lie a forgotten chest of the Kristalwacht and a secret ore vein for your "
                "crafts (Ambachtenboek).",
                "Klim op de ijskoepel. In het dak zit een gat met steigerwerk naar beneden: op een steiger is springen = omhoog "
                "en sluipen = omlaag. In de blauwe grot liggen een vergeten kist van de Kristalwacht en een geheime ertsader "
                "voor je ambachten (Ambachtenboek)."),
         beloning=("The treasure 'The blue ice cave' (a Rare piece + 6000 bronze) and a secret ore vein.",
                   "De schat 'De blauwe ijsgrot' (een Zeldzaam stuk + 6000 brons) en een geheime ertsader.")),
    dict(zone="zone2", id="galmgrot", soort=("Secret place", "Geheime plek"),
         naam=("The Echo Cave", "De Galmgrot"),
         hint=("Behind a frozen waterfall, something sings.", "Achter een bevroren waterval zingt iets."),
         plek=("zone2", "geheim_galmgrot", (940, 121, -1002)),
         plekken=[(("Way in: the snow stairs", "Ingang: de sneeuwtrap"), ("zone2", "geheim_galmgrot_ingang", (927, 118, -1002)))],
         tekst=("A stair of snow blocks leads up to the tunnel behind the frozen waterfall. Follow it into a cave full of "
                "singing crystals.",
                "Een trap van sneeuwblokken gaat omhoog naar de tunnel achter de bevroren waterval. Volg hem naar een grot vol "
                "zingende kristallen."),
         beloning=("The treasure 'The Echo Cave' (a Rare piece + 6000 bronze).", "De schat 'De Galmgrot' (een Zeldzaam stuk + 6000 brons).")),
    # ======== zone 3 ========
    dict(zone="zone3", id="mika_imker", quest="z3_geheim_imker", huisdier="zoempje", boek=("baas", "baas_imker"),
         soort=("Secret quest + secret boss", "Geheime quest + geheime baas"),
         naam=("The grey honey (Mika the Beekeeper)", "De grauwe honing (Mika de Imker)"),
         hint=("Someone hides under the Raattros.", "Iemand verstopt zich onder de Raattros."),
         plek=("zone3", "arena:z3imker:mika_imker", (1459, 145, -573)), waar="Honingtak (onder de Raattros)",
         plekken=[(("The hatch in the Raattros", "Het luik in de Raattros"), ("zone3", "mika_imker_luik", (1457, 157, -565))),
                  (("The Raattros door", "De deur van de Raattros"), ("zone3", "gebouw.raattros.deur", (1462, 157, -566)))],
         tekst=("Find one of the five beekeeper traces in Honingtak (the Clues list above) and the secret quest starts by "
                "itself. With all five the hatch in the floor of the Raattros opens: climb down the ladder into the honeycomb "
                "hall. Mika the Beekeeper (lv 47, Nature) is turning the bee queen grey with cheese honey. Lead his angry swarm "
                "through a smoke pot to put it to sleep, turn the queen's hive so its bee stream stings HIM while he pours, and "
                "stand together in the yellow circle at the hive for his big smoke bomb. After the quest you can fight him again "
                "(every 10 minutes). Hand the quest in with Raatwever Wasje.",
                "Vind één van de vijf imkersporen in Honingtak (zie Aanwijzingen hierboven): dan begint de geheime quest vanzelf. "
                "Met alle vijf gaat het luik in de vloer van de Raattros open: klim de ladder af, de raatzaal in. Mika de Imker "
                "(lv 47, Natuur) maakt de bijenkoningin grauw met kaas-honing. Lok zijn boze zwerm door een rookpotje (dan valt "
                "hij in slaap), draai de korf van de koningin zodat haar bijenstraal HEM steekt terwijl hij giet, en sta samen in "
                "de gele cirkel bij de korf als hij zijn grote rookbom gooit. Na de quest kun je hem opnieuw bevechten (elke 10 "
                "minuten). Je levert de quest in bij Raatwever Wasje.")),
    dict(zone="zone3", id="kruinroofvogel", baas="kruinroofvogel", huisdier="kruinroofvogel", boek=("baas", "baas_kruinroofvogel"),
         soort=("Boss without a quest", "Baas zonder quest"),
         naam=("The Crown Raptor", "De Kruinroofvogel"),
         hint=("Something big circles over the Kale Kruin.", "Op de Kale Kruin cirkelt iets groots."),
         plek=("zone3", "arena:z3bazen:kruinroofvogel", (1259, 226, -530)),
         plekken=[(("Arena entrance", "Ingang van de arena"), ("zone3", "arena_kruinroofvogel_deur", (1288, 229, -549))),
                  (("Perch 1 (he flees here)", "Zitplek 1 (hij vlucht hierheen)"), ("zone3", "kruinroofvogel_zit_1", (1471, 210, -452))),
                  (("Perch 2", "Zitplek 2"), ("zone3", "kruinroofvogel_zit_2", (1530, 200, -587)))],
         tekst=("A world boss (lv 38) that no quest sends you to: just walk onto his nest on the Kale Kruin. Red = his dive, "
                "purple = he grabs or blows. At 35 % he flees to one of his two other perches (a pink light trail shows which): "
                "you may call your mount in his arena then. He waits there for you. Respawn 25 minutes.",
                "Een wereldbaas (lv 38) waar geen quest je heen stuurt: loop gewoon zijn nest op de Kale Kruin op. Rood = zijn "
                "duikvlucht, paars = hij grijpt of blaast. Bij 35 % vlucht hij naar één van zijn twee andere zitplekken (een "
                "roze lichtspoor wijst welke): dan mag je je rijdier roepen in zijn arena. Hij wacht daar op je. Terug na 25 "
                "minuten.")),
    dict(zone="zone3", id="rijdierlooks", soort=("Secret mount looks", "Geheime rijdierlooks"),
         naam=("Secret looks for your mount", "Geheime looks voor je rijdier"),
         hint=("Some looks only show up for real sleuths.", "Sommige looks krijg je alleen als echte speurneus."),
         plek=("zone3", "gebouw.pluimstal.npc", (1378, 75, -672)),
         tekst=("These looks are not for sale: you earn them by finding secrets in all zones together. They appear by themselves "
                "(press N, pick a mount and click Dress up). Hat (tack, every land mount): 8 treasures. Night coat for the "
                "Knuffelhert: 20 hidden knuffels. Night coat for Slakkie: 15 treasures. Glow-mushroom coat for the "
                "Zwamzwijntje: 10 clues.",
                "Deze looks kun je niet kopen: je verdient ze met geheimen uit alle zones samen. Ze komen vanzelf (druk N, kies "
                "een rijdier en klik Aankleden). Hoedje (tuig, elk landrijdier): 8 schatten. Nachtvacht voor het Knuffelhert: "
                "20 verborgen knuffels. Nachtvacht voor Slakkie: 15 schatten. Glimzwamvacht voor het Zwamzwijntje: 10 "
                "aanwijzingen."),
         beloning=("Four secret looks (cosmetic: looks never change speed or jump).",
                   "Vier geheime looks (alleen uiterlijk: een look verandert nooit de snelheid of sprong).")),
    # ======== zone 4 ========
    dict(zone="zone4", id="duinhupper", soort=("Secret mount", "Geheim rijdier"), quest="z4_s15_strandavond",
         naam=("The Dune Hopper", "De Duinhupper"),
         hint=("A springy beach hare that only comes to look for sleuths.", "Een springerige strandhaas die alleen speurneuzen komt bekijken."),
         plek=("zone4", "strandfeest_dansvloer", (1991, 67, -592)),   # (Z4VerhaalPlekken ALIAS npc_strandfeest)
         tekst=("Two ways to get it. The secret way: find 10 hidden knuffels in the Vadsrijk (all zones count) once you have "
                "your first mount: then a Dune Hopper comes to look. Or finish the side quest 'Party at the campfire' of "
                "Feestguh Confetti at the beach party. It jumps the highest of all mounts (almost 6 blocks).",
                "Je kunt hem op twee manieren krijgen. De geheime manier: vind 10 verstopte knuffels in het Vadsrijk (alle "
                "zones tellen mee) als je je eerste rijdier al hebt: dan komt er een Duinhupper kijken. Of maak de zijquest "
                "'Feest bij het kampvuur' af van Feestguh Confetti op het strandfeest. Hij springt het allerhoogst van alle "
                "rijdiers (bijna 6 blokken)."),
         beloning=("The Dune Hopper mount.", "Het rijdier Duinhupper.")),
    # ======== zone 5 ========
    dict(zone="zone5", id="gorgonzola", baas="gorgonzola", huisdier="warmpje",
         soort=("Boss without a quest", "Baas zonder quest"),
         naam=("Overseer Gorgonzola", "Opzichter Gorgonzola"),
         hint=("It is getting cold inside the whale.", "Het wordt koud in de buik van de walvis."),
         plek=("zone5", "arena:z5wezens:gorgonzola", (2792, 166, -520)),
         plekken=[(("Hatch in the cellar of the Grote Soepkeuken", "Luik in de kelder van de Grote Soepkeuken"), ("zone5", "z5w_buik_luik", (2732, 219, -541))),
                  (("Bottom of the ladder", "Onderaan de ladder"), ("zone5", "z5w_buik_ladder_onder", (2733, 165, -539)))],
         tekst=("No quest sends you here. Climb down the hatch in the cellar of the Grote Soepkeuken into De Buikgangen (the "
                "tunnels inside Moeder Wolkwal) and walk to the Ketelkamer at the far end. Gorgonzola (lv 59, Fire) turns the "
                "four warm-air valves shut: right-click a closed (purple) valve 3 times to open it again. At 25 % he shuts them "
                "all. Respawn 5 minutes.",
                "Geen quest stuurt je hierheen. Klim via het luik in de kelder van de Grote Soepkeuken De Buikgangen in (de "
                "gangen binnen in Moeder Wolkwal) en loop naar de Ketelkamer helemaal achterin. Gorgonzola (lv 59, Vuur) draait "
                "de vier warme-luchtkleppen dicht: rechtsklik een dichte (paarse) klep 3 keer om hem weer open te draaien. Bij "
                "25 % draait hij ze allemaal tegelijk dicht. Terug na 5 minuten.")),
    dict(zone="zone5", id="donderbuik", baas="donderbuik", huisdier="donderbuikje",
         soort=("Boss without a quest", "Baas zonder quest"),
         naam=("Donderbuik, the grey thundercloud", "Donderbuik, de Grauwe Onweerswolk"),
         hint=("It rumbles on the whale's tail.", "Op de staart van de walvis rommelt het."),
         plek=("zone5", "arena:z5wezens:donderbuik", (2968, 206, -582)),
         plekken=[(("Arena entrance", "Ingang van de arena"), ("zone5", "z5w_donderbuik_deur", (2951, 207, -569)))],
         tekst=("A world boss (lv 60, Water) on the tail fluke of Moeder Wolkwal (De Staartvlakte), without a quest. Pink "
                "circles = cloud cushions: stand on one when he starts to suck, or he pulls you to his belly. Yellow circle = "
                "stand in it together. Fall off the fluke? A cloud catches you. Beaten, he turns pink and giggles. Respawn 20 "
                "minutes.",
                "Een wereldbaas (lv 60, Water) op de staartvin van Moeder Wolkwal (De Staartvlakte), zonder quest. Roze "
                "cirkels = wolkenkussens: sta erop als hij gaat zuigen, anders trekt hij je naar zijn buik. Gele cirkel = sta "
                "er samen in. Val je van de staart? Een wolkje vangt je op. Verslagen wordt hij roze en giechelt hij. Terug na "
                "20 minuten.")),
    dict(zone="zone5", id="schuilnest", soort=("Secret place", "Geheime plek"),
         naam=("The Hideout Nest", "Het Schuilnest"),
         hint=("Behind the crates of the Luchthaven there is a loose plank.", "Achter de kisten van de Luchthaven zit een losse plank."),
         plek=("zone5", "z5w_schuilnest", (2606, 188, -427)),
         plekken=[(("The loose plank (hatch)", "De losse plank (luik)"), ("zone5", "z5w_schuilnest_luik", (2612, 217, -444))),
                  (("Bottom of the ladder", "Onderaan de ladder"), ("zone5", "z5w_schuilnest_ladder_onder", (2611, 187, -441)))],
         tekst=("A secret Mika hideout inside the whale, under the Luchthaven. Go in through the loose plank behind the crates "
                "of the station and climb down. Barrels, cheese, sacks of grey fluff, and Mika's in disguise (lv 58-60): a red "
                "circle behind you = one appears there, step away. Their leader is Master of Disguise Feta.",
                "Een geheime Mika-schuilplaats binnen in de walvis, onder de Luchthaven. Ga naar binnen via de losse plank "
                "achter de kisten van het station en klim omlaag. Tonnen, kaas, zakken grauwe pluis en vermomde Mika's (lv "
                "58-60): een rode cirkel achter je = daar duikt er een op, stap weg. Hun leider is Meester-Vermommer Feta."),
         beloning=("Elite fights with Mika loot; mostly a great place to discover.", "Elitegevechten met Mika-buit; vooral een geweldig plekje om te ontdekken.")),
    # ======== zone 6 ========
    dict(zone="zone6", id="zoutkolos", baas="zoutkolos", huisdier="zoutklontje", boek=("baas", "baas_zoutkolos"),
         soort=("Boss without a quest", "Baas zonder quest"),
         naam=("The Salt Colossus", "De Zoutkolos"),
         hint=("At night something moves on De Zoutvlakte.", "'s Nachts beweegt er iets op De Zoutvlakte."),
         plek=("zone6", "arena:z6wezens:zoutkolos", (3995, 69, -560)),
         tekst=("A field boss (lv 66) of star salt that guards the Zoutbaken, ONLY at night and without a quest. Phase 2: "
                "his pink crystals sing (listen for the high note and step out of the purple ring, or break them). Phase 3: "
                "look straight at his glowing heart while you hit him for double damage (x2.5 with the Sterrenlens in your "
                "hand). Respawn 20 minutes.",
                "Een veldbaas (lv 66) van sterrenzout die het Zoutbaken bewaakt, ALLEEN 's nachts en zonder quest. Fase 2: zijn "
                "roze kristallen zingen (luister naar de hoge toon en stap uit de paarse ring, of sla ze kapot). Fase 3: kijk "
                "recht naar zijn gloeiende hart terwijl je slaat, dan doe je dubbele schade (x2,5 met de Sterrenlens in je "
                "hand). Terug na 20 minuten.")),
    dict(zone="zone6", id="ringeling", boek=("baas", "baas_ringeling"), pagina=("raid/index.html", ("The raid page", "De raidpagina")),
         soort=("Secret boss", "Geheime baas"),
         naam=("Duchess Ringeling", "Hertogin Ringeling"),
         hint=("At the top of the tower someone sad is waiting.", "Boven in de toren wacht iemand die verdrietig is."),
         plek=("zone6", "raid_toren_poort", (4123, 85, -688)),
         plekken=[(("Her floor (the 4th)", "Haar verdieping (de 4e)"), ("zone6", "arena:z6raid:ringeling", (4140, 52, -688)))],
         tekst=("The last boss of the raid De Sterrendovende Toren at the Moederoase (2-10 players, lv 70). Her book row stays "
                "??? until you cheer her up: climb all four floors with your group. Afterwards she is your friend in "
                "Saffraanpoort.",
                "De laatste baas van de raid De Sterrendovende Toren bij de Moederoase (2-10 spelers, lv 70). Haar regel in het "
                "boek blijft ??? tot je haar opvrolijkt: klim met je groepje alle vier de verdiepingen op. Daarna is ze je "
                "vriendin in Saffraanpoort."),
         beloning=("A raid chest (every clear), the title 'Friend of Ringeling' and her book row.",
                   "Een raidkist (bij elke clear), de titel 'Vriend van Ringeling' en haar regel in het boek.")),
]
ZELD = {"legendarisch": ("Legendary", "Legendarisch"), "guhddelijk": ("Guhddelijk", "Guhddelijk"), "episch": ("Epic", "Episch"),
        "zeldzaam": ("Rare", "Zeldzaam")}


def _koords(body):
    """{x, y, z} rows -> (x, y, z); {x, z} rows (y given in the call) -> None (the POI file has the spot)."""
    return [(int(a), int(b), int(c)) if c else None for a, b, c in re.findall(r"\{(-?\d+), (-?\d+)(?:, (-?\d+))?\}", body)]


def _lees_plekken(tekst):
    """[(MAP, id, poi, (x, y, z) or None)] from the patterns the *Plekken classes use."""
    out = []
    for m in re.finditer(r'\b([A-Z_]+)\.put\("([a-z0-9_]+)", w\("([a-z0-9_]+)"(?:, (-?\d+), (-?\d+), (-?\d+))?\)', tekst):
        out.append((m[1], m[2], m[3], (int(m[4]), int(m[5]), int(m[6])) if m[4] else None))
    for m in re.finditer(r'\b([A-Z_]+)\.put\("([a-z0-9_]+)", w\("([a-z0-9_]+)", (-?\d+), (-?\d+)\)\)', tekst):   # w(id, dx, dz): the spot moved a little
        out.append((m[1], m[2], m[3], ("+", int(m[4]), int(m[5]))))
    ints = {m[1]: _koords(m[2]) for m in re.finditer(r"int\[\]\[\] (\w+) = \{(.*?)\};", tekst, re.S)}
    strs = {m[1]: re.findall(r'"([a-z0-9_]+)"', m[2]) for m in re.finditer(r"String\[\] (\w+) = \{(.*?)\};", tekst, re.S)}
    for m in re.finditer(r'\b([A-Z_]+)\.put\((?:"([a-z_]+)" \+ \(i \+ 1\)|(\w+)\[i\]), w\("([a-z0-9_]+)" \+ \(i \+ 1\), (\w+)\[i\]\[0\]', tekst):
        k = ints.get(m[5], [])
        for i, xyz in enumerate(k):
            nid = (m[2] + str(i + 1)) if m[2] else (strs.get(m[3], [None] * len(k))[i] if i < len(strs.get(m[3], [])) else None)
            if nid:
                out.append((m[1], nid, m[4] + str(i + 1), xyz))
    consts = {m[1]: int(m[2]) for m in re.finditer(r"static final int (\w+) = (-?\d+);", tekst)}
    for m in re.finditer(r'\b([A-Z_]+)\.put\("([a-z0-9_]+)", p\((-?\d+), (-?\d+|\w+), (-?\d+)\)\)', tekst):   # zone 2's clues (no POI)
        y = int(m[4]) if m[4].lstrip("-").isdigit() else consts.get(m[4])
        if y is not None:
            out.append((m[1], m[2], "_p_" + m[2], (int(m[3]), y, int(m[5]))))
    for m in re.finditer(r'poi\("(z[56]_uitkijk_[a-z_]+)", w\("([a-z0-9_]+)", (-?\d+), (-?\d+), (-?\d+)\)\)', tekst):   # zone 5 + 6's viewpoints
        out.append(("UITKIJK", m[1], m[2], (int(m[3]), int(m[4]), int(m[5]))))
    vast = {m[1]: (m[2], (int(m[3]), int(m[4]), int(m[5])))
            for m in re.finditer(r'static final BlockPos (\w+) = w\("([a-z0-9_]+)", (-?\d+), (-?\d+), (-?\d+)\);', tekst)}
    for m in re.finditer(r'poi\("(z[56]_uitkijk_[a-z_]+)", ([A-Z_]+)\)', tekst):   # (1.10.21: z5_uitkijk_oog = the constant BALKON_OOG)
        if m[2] in vast:
            out.append(("UITKIJK", m[1], vast[m[2]][0], vast[m[2]][1]))
    for m in re.finditer(r'vul\((\w+), "([a-z_]+)", "([a-z_]+)", new int\[\]\[\] \{(.*?)\}\);', tekst, re.S):
        for i, xyz in enumerate(_koords(m[4])):
            out.append((m[1], m[2] + str(i + 1), m[3] + str(i + 1), xyz))
    for m in re.finditer(r'vondst\("z6_tabletten", "[^"]+", "([a-z_]+)", new int\[\]\[\] \{(.*?)\}\);', tekst, re.S):
        for i, xyz in enumerate(_koords(m[2])):
            out.append(("TABLET", "tablet_" + str(i + 1), m[1] + str(i + 1), xyz))
    return out


def data(bron, pois, gebieden, punten, puzzels):
    root = bron.root
    nl = bron.nl
    suffix = {}
    for k in nl:
        if k.startswith("vadsrijk."):
            suffix.setdefault(k.rsplit(".", 1)[-1], []).append(k)

    def zoek(ns, soortkey, nid):
        for k in (f"{ns}.{soortkey}.{nid}",):
            if k in nl:
                return bron.schoon(nl[k])
        for k in suffix.get(nid, []):
            if k.startswith(ns + ".") and not k.endswith((".naam", ".titel")) and len(nl[k]) > 3 and ".q." not in k and ".npc.naam" not in k:
                return bron.schoon(nl[k])
        return ""

    java = _wereld_plekken(root)

    def waar(zone, poi, xyz, mapnaam="", nid=""):
        """The game's spot: like the *Plekken classes, the generated map class first (1.10.21: it wins over the POI file), then
        the POI file, then the fallback numbers (zone 2's v1 numbers shifted like VadsrijkZones.z2)."""
        if zone == "zone2" and mapnaam in ("AANWIJZING", "SCHAT"):   # Z2NpcPlekken.volgKaart: "aanwijzing.<id>" / "schat.<id>" win
            k = ("aanwijzing." if mapnaam == "AANWIJZING" else "schat.") + nid
            if (zone, k) in java:
                return list(java[(zone, k)])
        for cand in (poi, poi.replace("_z6_", "_"), poi.replace("geheim_z6_", "geheim_")):
            if (zone, cand) in java:
                b = java[(zone, cand)]
                return [b[0] + xyz[1], b[1], b[2] + xyz[2]] if xyz and xyz[0] == "+" else list(b)
            if (zone, cand) in pois:
                b = pois[(zone, cand)][2]
                return [b[0] + xyz[1], b[1], b[2] + xyz[2]] if xyz and xyz[0] == "+" else list(b)
        if not xyz or xyz[0] == "+":
            return None
        if zone == "zone2":   # (Z2NpcPlekken.p / w: the fallback numbers are v1, shifted like VadsrijkZones.z2)
            return [xyz[0] + Z2_DX, xyz[1], xyz[2] + Z2_DZ]
        return list(xyz)

    def gebied(pos):
        g = bron.gebied_op(gebieden, pos[0], pos[1], pos[2]) if pos else None
        return (g["id"], bron.gebied_naam(g["id"])[0]) if g else (None, "")

    out, gezien = [], set()
    for zone, rel in PLEKKEN.items():
        try:
            tekst = open(os.path.join(root, *JAVA, *rel.split("/")), encoding="utf-8").read()
        except OSError:
            bron.notes.append(f"geheimen: {rel} missing")
            continue
        ns = NS[zone]
        for mapnaam, nid, poi, xyz in _lees_plekken(tekst):
            soort = SOORT_VAN.get(mapnaam)
            if not soort or (zone, soort, nid) in gezien:
                continue
            pos = waar(zone, poi, xyz, mapnaam, nid)
            if not pos:
                continue
            gezien.add((zone, soort, nid))
            gid, gnaam = gebied(pos)
            if soort == "knuffel":
                naam = bron.t(f"{ns}.knuffel.{nid}") or f"Knuffel {nid.split('_')[-1]}"
                tekst_nl = f"Een verloren knuffel{(' in ' + gnaam) if gnaam else ''}. Hij glinstert een beetje; loop ertegenaan of rechtsklik."
                bel = "Telt mee voor de knuffelquest van deze zone en voor je Verkenningsboek."
            elif soort == "schat":
                naam = f"Schat: {gnaam}" if gnaam else "Schat"
                tekst_nl = zoek(ns, "schat", nid)
                ingang = java.get((zone, f"geheim_{nid}_ingang"))   # (1.10.22: the IJsgrot's id is its roof hole now)
                if ingang:   # 1.10.21: how to get in
                    tekst_nl += f" (Ingang: x {ingang[0]}, y {ingang[1]}, z {ingang[2]}.)"
                bel = (f"Een uitrustingsstuk op jouw level ({'Episch' if nid in SCHAT_EPISCH else 'Zeldzaam'}) + "
                       f"{SCHAT_MUNTEN[zone]} brons ({SCHAT_MUNTEN[zone] // 100} zilver).")
            elif soort == "aanwijzing":
                naam = bron.t(f"{ns}.aanwijzing.naam") or "Aanwijzing"
                tekst_nl = zoek(ns, "aanwijzing", nid)
                bel = "Telt mee voor de geheime quest van deze zone (alle vijf = de ontknoping)."
            elif soort == "uitkijk":
                naam = "Uitkijkpunt " + (bron.t(f"vadsrijk.wereld.uitkijk.{nid}") or nid.split("_", 2)[-1].replace("_", " ").title())
                tekst_nl = ""
                bel = "Een panorama + een vinkje in je Verkenningsboek."
            elif soort == "relief":
                naam = bron.t(f"{ns}.relief.naam") or "Reliëf"
                tekst_nl = zoek(ns, "relief", nid)
                bel = "Telt mee voor Het Muurverhaal (alle acht samen vertellen het verhaal van het ei)."
            else:
                sub = {"MISTBRIEVEN": "mistbrief", "FLESSENPOST": "fles", "TABLET": "tablet"}[mapnaam]
                naam = bron.t(f"{ns}.{sub}.naam") or {"fles": "Flessenpost", "mistbrief": "Een briefje", "tablet": "Stenen tablet"}[sub]
                tekst_nl = zoek(ns, sub, nid)
                bel = "Een stukje van het verhaal; telt mee voor de bijbehorende quest en je Verkenningsboek."
            out.append(dict(zone=zone, soort=soort, id=f"{zone}:{soort}:{nid}", naam=naam, tekst=tekst_nl, pos=pos, gebied=gid,
                            gebied_naam=gnaam, beloning=bel))
    # puzzles: anchor POI (+dx/dz); the cave puzzles + treasures from GrotData; zone 6's star beacons
    grotten = _grotten(root)
    for pz in puzzels:
        pos, anker = None, pz.get("_anker") or {}
        plek = anker.get("plek", "")
        if anker.get("bron") == "grot":
            g = grotten.get(re.sub(r"^grot_|_puzzel$", "", plek))
            pos = list(g["puzzel"]) if g and g.get("puzzel") else None
        else:
            for (zone, pid), (_, _, xyz) in pois.items():
                if pid == plek:
                    pos = [xyz[0] + anker.get("dx", 0), xyz[1], xyz[2] + anker.get("dz", 0)]
                    break
        if not pos:
            continue
        gid, gnaam = gebied(pos)
        bel = ", ".join(x for x in ((f"{pz['munten']} brons" if pz.get("munten") else ""), (pz.get("ontgrendel") or ""),
                                    ("een bladzijde voor je Verkenningsboek" if pz.get("bladzijde") else "")) if x) or "De puzzel opent iets in de buurt."
        out.append(dict(zone=pz["zone"], soort="puzzel", id=f"{pz['zone']}:puzzel:{pz['id']}", naam=pz["naam"], tekst=pz.get("uitleg") or "",
                        pos=pos, gebied=gid, gebied_naam=gnaam, beloning=bel + (" (alleen 's nachts)" if pz.get("nacht") else "")))
    for b in ("palmbaken", "zoutbaken", "ruinebaken", "duinbaken", "oasebaken"):
        if ("zone6", b + "_puzzel") in pois:
            pos = list(pois[("zone6", b + "_puzzel")][2])
            gid, gnaam = gebied(pos)
            out.append(dict(zone="zone6", soort="puzzel", id=f"zone6:puzzel:{b}", naam=f"Sterrenbaken: {b[:-5].capitalize()}baken",
                            tekst="Volg de sterren van het sterrenbeeld in de volgorde van de hint (de Sterrenlens laat overdag de nachthemel zien).",
                            pos=pos, gebied=gid, gebied_naam=gnaam, beloning="Het baken brandt weer: een sterrenscherf en een visioen van Moeder Bron."))
    for gid_, g in grotten.items():
        if not g.get("schat"):
            continue
        zone = f"zone{g['zone']}"
        pos = list(g["schat"])
        _, gnaam = gebied(pos)
        naam = bron.t(f"vadsrijk.grotten.naam.{gid_}", gid_)
        out.append(dict(zone=zone, soort="grotschat", id=f"{zone}:grotschat:{gid_}", naam=f"Schat van {naam}",
                        tekst=f"{bron.t('vadsrijk.grotten.onder.' + gid_, '')}. Ingang: x {g['ingang'][0]}, y {g['ingang'][1]}, z {g['ingang'][2]}; de kist ligt achterin de grot."
                        if g.get("ingang") else bron.t("vadsrijk.grotten.onder." + gid_, ""),
                        pos=pos, gebied=None, gebied_naam=naam, beloning=f"Een kist met buit + {600 * g['zone'] ** 2 + 400} brons."))
    if ("zone6", "zoutgrot_schat") in pois:   # zone 6's cave lives in the map itself (u-z6wereld)
        pos = list(pois[("zone6", "zoutgrot_schat")][2])
        out.append(dict(zone="zone6", soort="grotschat", id="zone6:grotschat:zoutgrot", naam="Schat van De Zingende Zoutgrot",
                        tekst="Onder De Zoutvlakte. Zing het karavaanlied op de vijf kristallen (zijquest 'Het lied van de Zoutgrot'): dan gaat de kristalwand open.",
                        pos=pos, gebied=None, gebied_naam="De Zingende Zoutgrot", beloning="De kist van de Eerste Karavaan: het tapijtpatroon 'karavaan'."))
    # viewpoints + campfires (public on the map already; listed here with coordinates too)
    for pt in punten:
        if pt["soort"] in ("uitkijk", "kampvuur"):
            out.append(dict(zone=pt["zone"], soort=pt["soort"], id=pt["id"], naam=pt["naam"], tekst="", pos=pt["pos"], gebied=pt.get("gebied"),
                            gebied_naam=bron.gebied_naam(pt["gebied"])[0] if pt.get("gebied") else "",
                            beloning="Een panorama + een vinkje in je Verkenningsboek." if pt["soort"] == "uitkijk"
                            else "Rechtsklik: je checkpoint (hier word je wakker als je flauwvalt) + een vinkje in je Verkenningsboek."))
    # 1.10.21: one viewpoint can be both a world viewpoint (punten) and a story one (z5/z6_uitkijk_*): list it once
    gezien_pos, uniek = set(), []
    for g in out:
        k = (g["zone"], g["soort"], tuple(g["pos"]))
        if k not in gezien_pos:
            gezien_pos.add(k)
            uniek.append(g)
    out = uniek + _verborgen(bron, java, gebied)
    volgorde = {s[0]: i for i, s in enumerate(SOORTEN)}
    out.sort(key=lambda g: (g["zone"], volgorde.get(g["soort"], 99), g.get("volg", 0), g["naam"]))
    return out


def _wereld_plekken(root):
    """{(zone, id): (x, y, z)} from the generated map classes (Map.entry("id", new BlockPos(x, y, z)))."""
    out = {}
    for zone, rel in WERELD.items():
        try:
            tekst = open(os.path.join(root, *JAVA, *rel.split("/")), encoding="utf-8").read()
        except OSError:
            continue
        for m in re.finditer(r'Map\.entry\("([a-z0-9_.]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)\)', tekst):
            out[(zone, m[1])] = (int(m[2]), int(m[3]), int(m[4]))
    return out


def _quests_json(root):
    """{quest id: quest} from data/guhs/vadsrijk/npc/quests*.json (only what the secret entries need: the reward)."""
    import glob
    import json
    out = {}
    for f in glob.glob(os.path.join(root, "src", "main", "resources", "data", "guhs", "vadsrijk", "npc", "quests*.json")):
        try:
            d = json.load(open(f, encoding="utf-8"))
        except (OSError, ValueError):
            continue
        qs = d if isinstance(d, list) else d.get("quests", d)
        for q in (qs.values() if isinstance(qs, dict) else qs):
            if isinstance(q, dict) and q.get("id"):
                out[q["id"]] = q
    return out


def _verborgen(bron, java, gebied):
    """1.10.21: the hidden quests, hidden bosses and secret places (VERBORGEN), each with its spots from the game's own maps."""
    arenas = {a["id"]: a for a in (bron.rt or {}).get("arenas", [])}
    quests = _quests_json(bron.root)
    benoemd = bron._benoemd()

    def plek(spec):
        zone, pid, terug = spec
        if pid.startswith("arena:") and pid[6:] in arenas:
            return list(arenas[pid[6:]]["centrum"])
        if pid and (zone, pid) in java:
            return list(java[(zone, pid)])
        return list(terug)   # (= the Java class's own fallback numbers)

    def tt(paar):
        return dict(en=paar[0], nl=paar[1])
    out = []
    for i, v in enumerate(VERBORGEN):
        pos = plek(v["plek"])
        gid, gnaam = gebied(pos)
        gnaam = v.get("waar") or gnaam
        bel_nl, bel_en = [], []
        q = quests.get(v.get("quest") or "")
        if q and q.get("soort") == "geheim":
            b = q.get("beloning") or {}
            loot = {"episch": ("an Epic piece", "een Episch stuk"), "zeldzaam": ("a Rare piece", "een Zeldzaam stuk")}.get(b.get("loot") or "")
            bel_nl.append("Quest: " + ", ".join(x for x in (f"{b.get('xp', 0)} xp", f"{b.get('munten', 0)} brons ({b.get('munten', 0) // 100} zilver)",
                                                           loot[1] if loot else "") if x))
            bel_en.append("Quest: " + ", ".join(x for x in (f"{b.get('xp', 0)} xp", f"{b.get('munten', 0)} bronze ({b.get('munten', 0) // 100} silver)",
                                                           loot[0] if loot else "") if x))
        if v.get("baas"):
            items = sorted(((iid, d) for iid, d in benoemd.items() if d["baas"] == v["baas"]), key=lambda x: -x[1]["kans"])
            if items:
                zn = [f'{bron.t("vadsrijk.loot." + iid, iid)} ({ZELD.get(d["zeldzaamheid"], ("", d["zeldzaamheid"]))[1]}, {round(d["kans"] * 100)} %)' for iid, d in items]
                ze = [f'{bron.en.get("vadsrijk.loot." + iid, bron.t("vadsrijk.loot." + iid, iid))} ({ZELD.get(d["zeldzaamheid"], (d["zeldzaamheid"], ""))[0]}, '
                      f'{round(d["kans"] * 100)} %)' for iid, d in items]
                bel_nl.append("Baas: buit + kans op " + ", ".join(zn))
                bel_en.append("Boss: loot + a chance at " + ", ".join(ze))
        if v.get("huisdier"):
            hn, he = bron.tx(f"vadsrijk.huisdier.{v['huisdier']}.naam", v["huisdier"])
            bel_nl.append(f"Je eerste overwinning: het huisdiertje {hn}")
            bel_en.append(f"Your first win: the pet {he}")
        if v.get("beloning"):
            bel_en.append(v["beloning"][0])
            bel_nl.append(v["beloning"][1])
        plekken = [dict(label=tt(lab), pos=plek(spec)) for lab, spec in v.get("plekken", [])]
        out.append(dict(zone=v["zone"], soort="verborgen", id=f'{v["zone"]}:verborgen:{v["id"]}', volg=i, naam=v["naam"][1], naam_en=v["naam"][0],
                        tekst=v["tekst"][1], tekst_en=v["tekst"][0], hint=tt(v["hint"]), label=tt(v["soort"]), pos=pos, plekken=plekken,
                        gebied=gid, gebied_naam=gnaam, beloning=". ".join(x.rstrip(".") for x in bel_nl) + ".", beloning_en=". ".join(x.rstrip(".") for x in bel_en) + ".",
                        quest=v.get("quest"), baas=v.get("baas"), boek=list(v["boek"]) if v.get("boek") else None,
                        pagina=dict(url=v["pagina"][0], en=v["pagina"][1][0], nl=v["pagina"][1][1]) if v.get("pagina") else None))
    return out


def _grotten(root):
    """grotten/GrotData.java -> {id: {zone, ingang, puzzel, schat}}."""
    try:
        tekst = open(os.path.join(root, *JAVA, "grotten", "GrotData.java"), encoding="utf-8").read()
    except OSError:
        return {}
    out = {}
    for m in re.finditer(r'new Grot\("([a-z]+)", (\d+),(.*?)\)\)\)', tekst, re.S):
        d = dict(zone=int(m[2]))
        for k, x, y, z in re.findall(r'Map\.entry\("([a-z_0-9]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)\)', m[3]):
            d[k] = (int(x), int(y), int(z))
        out[m[1]] = d
    return out


def pagina(b):
    """geheimen/index.html: per zone (collapsed), per kind a list of spoiler entries."""
    P = "geheimen/index.html"
    G = b.d.get("geheimen_lijst") or []
    delen = []
    for z in b.d["zones"]:
        mine = [g for g in G if g["zone"] == z["id"]]
        if not mine:
            continue
        inner = ""
        for soort, en, nl in SOORTEN:
            lijst = [g for g in mine if g["soort"] == soort]
            if not lijst:
                continue
            if soort == "puzzel" and z["nr"] < 6:   # X-SITE: the star beacons are zone 6 only
                en, nl = "Puzzles", "Puzzels"
            items = ""
            for i, g in enumerate(lijst, 1):
                x, y, zz = g["pos"]
                kaart = f'<a class="knop licht" href="../kaart.html?x={x}&amp;z={zz}&amp;geheim=1">{t("Show on map", "Toon op kaart")}</a>'
                body = (f'<p><b>{ESC(g["naam"])}</b>{(" &middot; " + ESC(g["gebied_naam"])) if g["gebied_naam"] else ""}</p>'
                        + (f'<p class="game-text">{ESC(g["tekst"])}</p>' if g["tekst"] else "")
                        + f'<p><span class="tagref">x {x}, y {y}, z {zz}</span></p>'
                        + f'<p><b>{t("Reward", "Beloning")}:</b> {ESC(g["beloning"])}</p><p>{kaart}</p>')
                en1, nl1 = ENKEL[soort]
                items += (f'<details class="spoiler geheim" id="{ESC(g["id"].replace(":", "-"))}"><summary>{t(f"{en1} #{i}: click to reveal", f"{nl1} #{i}: klik om te onthullen")}</summary>'
                          f'{body}</details>')
            inner += f'<h3>{t(en, nl)} ({len(lijst)})</h3>{items}'
        delen.append(b.zonevak(z, inner, f'{len(mine)} {t("hidden things", "verborgen dingen")}'))
    main = (f'<h1>{t("Secrets", "Geheimen")}</h1>'
            + p("Every hidden thing of the Vadsrijk WITH its location: knuffels, treasures, clues and letters, puzzles, cave treasures, viewpoints and campfires. Each one stays hidden until you click it, so you only spoil what you want. On the map they are in the layer \"Show secrets (spoilers!)\", which is off by default.",
                "Elk verborgen ding van het Vadsrijk MET de plek: knuffels, schatten, aanwijzingen en briefjes, puzzels, grotschatten, uitkijkpunten en kampvuren. Elk blijft verborgen tot je erop klikt, dus je verklapt alleen wat je zelf wilt. Op de kaart staan ze in de laag \"Geheimen tonen (spoilers!)\", die standaard uit staat.")
            + f'<p class="muted">{t("Click a zone to open it. Puzzle solutions are never on this site; quest walkthroughs are on the quest pages (spoiler button).", "Klik op een zone om hem open te klappen. Oplossingen van puzzels staan nergens op deze site; walkthroughs van quests staan op de questpagina’s (spoilerknop).")}</p>'
            + "".join(delen))
    b.schrijf(P, "Geheimen", "Secrets", main, P, beschrijving="Alle geheimen van Het Vadsrijk met hun plek, elk achter een spoilerklik.")
