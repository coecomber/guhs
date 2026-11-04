"""
1.10 "Het Vadsrijk", zone 3 "De Schemerwortel", the bazen slice (Z3_BRIEF.md). Java: feature/vadsrijk/zone3/bazen.

The texts (nl; en_us gets the same Dutch) + the build(h) hook. The art (models, textures, animations, blocks, icons) is in
vadsrijk_z3_bazen_modellen.py (alles()).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z3_bazen.py
"""
import json
import os

try:
    from features import vadsrijk_z3_bazen_modellen as modellen
except ImportError:  # standalone
    import vadsrijk_z3_bazen_modellen as modellen

Z = "vadsrijk.z3bazen."

BAZEN = {
    # id: (entity name, TITLE, subtitle)
    "kruinroofvogel": ("De Kruinroofvogel", "DE KRUINROOFVOGEL", "Hij pikte het oude nest in. En jouw lunch."),
    "taleggio": ("Takelmeester Taleggio", "TAKELMEESTER TALEGGIO", "Alles aan een touwtje. Ook jij."),
    "brokkel": ("Bombardier Brokkel", "BOMBARDIER BROKKEL", "Kaaswielen, kanonnen en heel veel kabaal"),
    "roquefort": ("Kapitein Roquefort", "KAPITEIN ROQUEFORT", "Kapitein van de Kaaszeppelinvloot"),
    "piepvel": ("Graaf Piepvel", "GRAAF PIEPVEL", "De Pink van de Keizer"),
    "wortelwurger": ("De Wortelwurger", "DE WORTELWURGER", "Een kluwen grauwe wortels om een rotte kern"),
    "nestwachter": ("De Oude Nestwachter", "DE OUDE NESTWACHTER", "Hij bewaakt het ei. Tegen iedereen."),
    "bleu": ("Baron Bleu", "BARON BLEU EN DE SCHIMMELKOLOS", "De Knokkel van de Graaf. Rijpt alles. Ook eieren."),
    "vahoegnir": ("Vahoegnir", "VAHOEGNIR", "De Vadsloze Draak"),
}

TEKSTEN = {
    # entities
    "entity.guhs.vr_z3_baas_kooi": "Kooitje",
    "entity.guhs.vr_z3_baas_ballon": "Ballonsegment",
    "entity.guhs.vr_z3_baas_voedingswortel": "Voedingswortel",
    "entity.guhs.vr_z3_baas_kanon": "Dekkanon (rechtsklik)",
    "entity.guhs.vr_z3_baas_ketel": "Schimmelketel (rechtsklik)",
    "entity.guhs.vr_z3_baas_schimmelkolos": "De Schimmelkolos",
    "entity.guhs.vr_z3_baas_matroos": "Mika-dekmatroos",
    "entity.guhs.vr_z3_baas_schimmelbol": "Schimmelbolletje",
    "entity.guhs.vr_z3_baas_kloddertje": "Grauw kloddertje",
    "entity.guhs.vr_z3_baas_figurant": "Figurant",
    # blocks
    "block.guhs.vr_z3_baas_lantaarnpaal": "Lantaarnpaal",
    "block.guhs.vr_z3_baas_altaar": "Vahoeg-altaar",
    "block.guhs.vr_z3_baas_poort": "Schimmelkorstpoort",
    "block.guhs.vr_z3_baas_hendel": "Hendel",
    "block.guhs.vr_z3_baas_ringknop": "Jaarringknop",
    "block.guhs.vr_z3_baas_nerf": "Roze nerf",
    "block.guhs.vr_z3_baas_nesttak": "Grote nesttak",
    # speakers
    Z + "krabbel": "Hofschrijver Krabbel",
    Z + "keizer": "???",
    Z + "te_ver": "Je staat buiten het gevecht: van daar raak je hem niet!",
    Z + "immuun": "Hij is nu onkwetsbaar: doe eerst de opdracht!",
    Z + "enrage": "ALLES OF NIKS",
    Z + "enrage.onder": "Hij is het zat. Snel nu!",
    # Vahoeg mode
    Z + "vahoeg.intro": "VAHOEG-VORM! Weekzegel: %s.",
    Z + "vahoeg.aan.werf": "Het Vahoeg-altaar gloeit: de bazen van de Kaaszeppelinwerf worden Vahoeg (zegel: %s). Een half uur, of tot Kapitein Roquefort valt.",
    Z + "vahoeg.aan.hart": "Het Vahoeg-altaar gloeit: de bazen van het Hart van de Boom worden Vahoeg (zegel: %s). Een half uur, of tot Baron Bleu vlucht.",
    Z + "vahoeg.uitleg": "Het Vahoeg-altaar. Leg er een Vahoegzegel op (rechtsklik met een zegel in je tas, vanaf lv 50): de bazen worden sterker, en ze laten Gouden Veren vallen. Weekzegel nu: %s.",
    Z + "vahoeg.loopt": "De Vahoeg-modus loopt al (zegel: %s), nog %s min.",
    Z + "zegel.donker": "Donker (minder tijd om weg te stappen)",
    Z + "zegel.haastig": "Haastig (ze worden eerder boos)",
    Z + "zegel.splitsend": "Splitsend (dubbel zoveel hulpjes)",
    Z + "zegel.taai": "Taai (een schild in de eerste fase)",
    # held
    Z + "vast.klauw": "De Kruinroofvogel grijpt je! Klik of druk op shift om je los te wurmen, of laat je team hem raken!",
    Z + "vast.wortel": "Een wortel wurgt je vast! Klik of druk op shift, of laat een teamgenoot naast je sluipen.",
    Z + "vast.kamer": "Opgesloten in de Rijpingskamer! Klik of druk op shift, of laat een teamgenoot naast je sluipen.",
    Z + "vast.klik": "Loswurmen: %s / %s",
    Z + "vast.los": "Los!",
    # Kruinroofvogel
    Z + "kr.grijp": "De Kruinroofvogel duikt naar %s! Weg uit de paarse cirkel!",
    Z + "kr.mis": "Mis! Zijn klauwen grijpen in de lucht.",
    Z + "kr.wind": "Vleugelwind! Hij blaast je naar de rand.",
    Z + "kr.losgeschoten": "Raak! Hij laat los en tuimelt even.",
    Z + "kr.losgelaten": "Hij laat je vallen... je zweeft zachtjes terug het nest in.",
    Z + "kr.fase2": "GRIJPKLAUW",
    Z + "kr.fase2.onder": "Wie hij pakt, schiet je los!",
    Z + "kr.fase3": "DE ACHTERVOLGING",
    Z + "kr.fase3.onder": "Hij vlucht! Volg het roze spoor. Fluit je Pluimguh: hier mag je rijden!",
    Z + "kr.achtervolg": "De Kruinroofvogel vlucht naar een andere kruintop! Roep je rijdier en volg het roze lichtspoor. Wie er snel is, krijgt Vahoeg!",
    Z + "kr.geland": "HIJ IS GELAND",
    Z + "kr.geland.onder": "Het gevecht gaat hier verder",
    Z + "kr.snel": "Snel erbij! 10 s Vahoeg!",
    Z + "kr.vlucht_immuun": "Hij vliegt weg: volg hem!",
    Z + "kr.verslagen": "De Kruinroofvogel valt uiteen in grauwe veren die wegwaaien over de kruin.",
    # Taleggio
    Z + "ta.snij": "Taleggio snijdt het touwwerk los! Weg uit het rood, spring naar het roze!",
    Z + "ta.fase2": "AAN DE TOUWEN",
    Z + "ta.fase2.onder": "Schiet hem of trek hem met een hendel naar beneden",
    Z + "ta.hangt": "Taleggio slingert hoog aan zijn touwen! Afstand en magie raken hem; trek aan een hendel (roze) als hij erboven zwaait.",
    Z + "ta.gevallen": "Krak! Taleggio valt naar beneden. Nu slaan!",
    Z + "ta.klimt": "Taleggio klautert terug in het touwwerk.",
    Z + "ta.te_ver": "Hij zwaait niet boven deze hendel. Wacht tot hij erboven is!",
    Z + "ta.vlucht": "Dit komt in mijn rapport! Ik ben weg! (Hij verdwijnt in een rookpluim.)",
    # Brokkel
    Z + "br.wielen": "Kaaswielbommen! Uit de rode banen!",
    Z + "br.plaat": "Een pantserplaat vliegt eraf! Nog %s.",
    Z + "br.laden": "Het kanon laadt nog... %s s",
    Z + "br.pantser": "Zijn pantserplaten vangen het meeste op: schiet ze eraf met de dekkanonnen!",
    Z + "br.fase2": "MEER KABAAL",
    Z + "br.fase2.onder": "Meer banen, meer bommen",
    Z + "br.fase3": "VOLLE LADING",
    Z + "br.fase3.onder": "Hij gooit alles wat hij heeft",
    Z + "br.vlucht": "Kaboem! ...Nee wacht, dat was ik zelf. Ik ga ervandoor!",
    # Roquefort
    Z + "rq.fase2": "HET DEK KANTELT",
    Z + "rq.fase2.onder": "Prik de ballonnen door (paars)!",
    Z + "rq.fase3": "DE ZEPPELIN ZINKT",
    Z + "rq.fase3.onder": "Spring naar het wrak (roze)!",
    Z + "rq.ballonnen": "Vier ballonsegmenten met paarse merktekens: prik ze door, dan zakt hij!",
    Z + "rq.ballonnen_eerst": "Zijn zeppelin houdt hem omhoog: prik eerst de ballonnen door!",
    Z + "rq.geprikt": "PLOF! Ballon %s van 4.",
    Z + "rq.kantel": "Het dek kantelt! Losse kisten glijden naar beneden!",
    Z + "rq.matrozen": "Alle hens aan dek! Mika-dekmatrozen!",
    Z + "rq.zinkt": "De zeppelin zinkt! Spring naar het wrak (roze cirkel)!",
    Z + "rq.vlucht": "Dit is nog niet voorbij, ridder! De Graaf hoort hiervan! (Hij zweeft weg aan zijn paraplu.)",
    # Piepvel
    Z + "pv.intro": "Een ridder? In MIJN Pelszaal? Piep! Brrr, doe de deur dicht, het tocht.",
    Z + "pv.intro_weer": "Nog steeds koud hier. Dat komt door jóú. Kijk, een nieuwe cape. Van sokken. Gebreid. Door mij.",
    Z + "pv.zwier": "Capezwier! Uit de rode kegel!",
    Z + "pv.entree": "Ahem. Mijn deftige entree!",
    Z + "pv.schild_op": "Piep piep! Mijn muisjes houden mij warm. En veilig.",
    Z + "pv.fase2": "DE KOOITJES",
    Z + "pv.fase2.onder": "Breek de kooisloten: de muisjes voeden zijn schild",
    Z + "pv.schild": "Een vadsschild van muizenverdriet! Bevrijd de muisjes in de kooitjes!",
    Z + "pv.kooi_open": "Een kooitje springt open! De muisjes knagen een gat in zijn cape. (%s van 4)",
    Z + "pv.au": "Au! Niet aan de cape komen! Die is antiek!",
    Z + "pv.schild_weg": "ZIJN SCHILD IS WEG",
    Z + "pv.schild_weg.onder": "En zijn cape zit vol gaten",
    Z + "pv.fase3": "DONKER",
    Z + "pv.fase3.onder": "Steek de lantaarns weer aan: alleen in het licht raak je hem goed",
    Z + "pv.donker": "Fffffuuu! Zo. Lekker donker. Lekker koud.",
    Z + "pv.blaas": "Hij blaast naar een lantaarn! Sta in de paarse cirkel om de vlam te beschermen!",
    Z + "pv.beschermd": "De vlam blijft branden!",
    Z + "pv.grote_kooi": "De grote kooi naast de troon rammelt! Rechtsklik hem open!",
    Z + "pv.nog_niet": "De grote kooi zit nog vast. Eerst moet de Graaf verder verzwakt.",
    Z + "pv.finale": "De muisjes stormen op hem af!",
    Z + "pv.nee": "Nee... nee nee nee! Niet de grote kooi! PIEP!",
    Z + "pv.mijn_cape": "Mijn cape! Weet je wel hoeveel winters dat gekost heeft?!",
    Z + "pv.enrage": "DE ZAAL BEVRIEST",
    Z + "pv.enrage.onder": "Hij houdt van kou. Jij niet.",
    # flash F1
    Z + "f1.uitleggen": "Nee nee nee, Keizer, ik kan het uitleggen! Het was de ridder! Het was —",
    Z + "f1.floep": "Floep.",
    Z + "f1.stilte": "Stilte. Op de schors blijft één grauwe vingerafdruk achter.",
    Z + "f1.titel": "FLOEP.",
    Z + "f1.onder": "Een arm. Veel te lang. Met vijf lange vingers.",
    # Wortelwurger
    Z + "ww.grijp": "Grauwe wortels schieten uit de muur! Uit de rode stroken!",
    Z + "ww.gekapt": "Een voedingswortel is doorgekapt! (%s van 4)",
    Z + "ww.groei": "Een voedingswortel groeit terug!",
    Z + "ww.fase2": "DE KLUWEN KNIJPT",
    Z + "ww.fase2.onder": "Meer grijpwortels",
    Z + "ww.fase3": "HET HART BONST",
    Z + "ww.fase3.onder": "Sneller, donkerder",
    Z + "ww.verslagen": "De Wortelwurger valt uiteen in grijze pluis. Ergens in de muur klopt het hart rustiger.",
    # Nestwachter
    Z + "nw.golf": "Jaarring-golf! Ga op een ROZE ring staan!",
    Z + "nw.fase2": "DE JAREN TELLEN",
    Z + "nw.fase2.onder": "Snellere golven",
    Z + "nw.fase3": "HONDERD WINTERS",
    Z + "nw.fase3.onder": "Twee golven achter elkaar, ze wisselen",
    Z + "nw.dank": "...Roze. Ik ben weer roze. Het grauw is weg. Dank je, kleine ridder.",
    Z + "nw.splinter": "Ga maar. Het ei wacht. En neem dit mee: ik hield het warm voor hem. Het hartje van zijn hart.",
    # Baron Bleu
    Z + "bb.intro": "De Graaf is weg. Dus als íkzelf het ei breng... dan ben ík de nieuwe Pink!",
    Z + "bb.spray": "De Kolos spuit naar een lantaarn! Steek hem straks weer aan (rechtsklik).",
    Z + "bb.zelf": "Genoeg! Dan rijp ik je zelf wel!",
    Z + "bb.fase2": "DE BARON ZELF",
    Z + "bb.fase2.onder": "Degen en Rijpingskamer",
    Z + "bb.kamer": "Rijpingskamer! Uit de paarse cirkel!",
    Z + "bb.fase3": "DE GRIJPARM",
    Z + "bb.fase3.onder": "Sta SAMEN in de gele cirkel bij het ei",
    Z + "bb.ei": "Kolos! Pak het ei! Pak het EI!",
    Z + "bb.grijparm": "De grijparm reikt naar het ei! Ga samen in de gele cirkel!",
    Z + "bb.geblokt": "Geblokt! De arm komt niet bij het ei.",
    Z + "bb.gegrepen": "De arm graait aan het ei! De Baron lacht en knapt op.",
    Z + "bb.vlucht": "Dit hoeft de Keizer niet te weten! ...Toch? (Hij ontsnapt in zijn zeppelinnetje door een gat in de wortels.)",
    # Vahoegnir
    Z + "vn.splinter1": "HIJ LANDT",
    Z + "vn.splinter1.onder": "Een roze cirkel onder zijn borst: zet de eerste splinter terug",
    Z + "vn.splinter2": "HIJ LANDT WEER",
    Z + "vn.splinter2.onder": "De tweede splinter",
    Z + "vn.splinter3": "NOG ÉÉN KEER",
    Z + "vn.splinter3.onder": "De derde splinter",
    Z + "vn.splinter_cirkel": "Ga 3 s in de roze cirkel onder zijn borst staan: zet een hartsplinter terug!",
    Z + "vn.splinter_bezig": "Hartsplinter terugzetten... %s%%",
    Z + "vn.splinter_eerst": "Hij wacht... zet eerst een hartsplinter terug (roze cirkel)!",
    Z + "vn.te_hoog": "Hij vliegt te hoog. Bescherm het ei!",
    Z + "vn.terug1": "SPLINTER TERUG",
    Z + "vn.terug1.onder": "Zijn vleugelpunten worden roze",
    Z + "vn.terug2": "SPLINTER TERUG",
    Z + "vn.terug2.onder": "Zijn vleugels kleuren roze",
    Z + "vn.terug3": "SPLINTER TERUG",
    Z + "vn.terug3.onder": "Hij stijgt op... naar het ei!",
    Z + "vn.pluis": "Zwarte pluis regent neer! Vrolijk de grauwe kloddertjes op voor ze het ei bereiken!",
    Z + "vn.klodder_ei": "Een grauw kloddertje rolt tegen het ei. Het ei wordt een beetje grauw...",
    Z + "vn.huil": "KRISTALHUIL! Verstop je achter een grote nesttak!",
    Z + "vn.beschut": "De nesttak vangt de huil op... en kraakt.",
    Z + "vn.duik": "Hij duikt naar het ei! Sta SAMEN in de gele cirkel!",
    Z + "vn.remt": "Het ei gloeit roze! Hij remt af... en aarzelt.",
    Z + "vn.ei_grauw": "Hij scheert langs het ei. Het wordt grauwer...",
    Z + "vn.enrage": "HET NEST WORDT GRAUW",
    Z + "vn.enrage.onder": "Het ei houdt het niet lang meer vol",
    Z + "vn.hoogvads": "Vahoegnir blaast een lange roze lichtstraal over de horizon. Waar hij eindigt, zweeft een stad boven de wolken: Hoogvads.",
    Z + "vn.einde.titel": "OPGEVROLIJKT!",
    Z + "vn.einde.onder": "De Schemerwortel bloeit weer",
    # flash F2
    Z + "f2.rillen": "Een grauwe zaal. Mager licht. Graaf Piepvel knielt in zijn hemdje en rilt.",
    Z + "f2.rapport1": "Rapport uit het westen. De draak is... eh... roze.",
    Z + "f2.rapport2": "Het ei is... eh... uit. De Pink is... kaal.",
    Z + "f2.tik": "Lange grauwe vingers trommelen op een armleuning. Tik. Tik. Tik. Tik. Tik.",
    Z + "f2.reik": "Dan... reik... ik... verder.",
    Z + "f2.titel": "WORDT VERVOLGD...",
    Z + "f2.onder": "Ver in het oosten strekt iets zijn armen.",
    # dungeons
    Z + "werf.net": "Een vangnet van touw vangt je op en slingert je terug naar de ingang van de werf.",
    Z + "werf.loods_open": "BOEM! De kanonnen blazen de poort open!",
    Z + "hart.bewogen": "Je bewoog in het donker! De wortels voelen het...",
    Z + "hart.ketel_uit": "De schimmelketel sist uit!",
    Z + "hart.kwekerij_open": "Alle schimmelketels zijn uit: het schimmelgordijn valt weg!",
    Z + "hart.ringen_open": "De roze nerf loopt in één lijn: de jaarringen schuiven open!",
    # loot
    Z + "loot.guhddelijk": "%s vond iets GUHDDELIJKS: %s!",
    Z + "loot.goud": "%s heeft de GOUDEN PLUIMGUH! Wat een glans!",
    Z + "veer.nog": "Gouden Veren: %s / %s. Met 25 krijg je de Gouden Pluimguh.",
    Z + "veer.al": "Je hebt de Gouden Pluimguh al. Bewaar je veren maar voor de sier.",
    # tips
    Z + "tip.kruinroofvogel.titel": "De Kruinroofvogel",
    Z + "tip.kruinroofvogel": "Rood = zijn duikvlucht. Paars = hij grijpt of blaast. Gegrepen? Klik of shift je los. Bij zijn vlucht mag je je Pluimguh roepen!",
    Z + "tip.taleggio.titel": "Takelmeester Taleggio",
    Z + "tip.taleggio": "Rood = dat platform valt, roze = veilig touwwerk. Hangt hij hoog? Trek aan een hendel als hij erboven zwaait.",
    Z + "tip.brokkel.titel": "Bombardier Brokkel",
    Z + "tip.brokkel": "Zijn pantser vangt veel op. Rechtsklik een dekkanon en kijk naar hem: zo schiet je de platen eraf.",
    Z + "tip.roquefort.titel": "Kapitein Roquefort",
    Z + "tip.roquefort": "Rood = zijn enteraanval. Paars = ballonnen doorprikken. Zinkt het schip? Spring naar het roze wrak!",
    Z + "tip.piepvel.titel": "Graaf Piepvel",
    Z + "tip.piepvel": "Bevrijd de muisjes: breek de kooisloten om zijn schild te breken. In het donker: steek lantaarns aan en vecht in het licht.",
    Z + "tip.wortelwurger.titel": "De Wortelwurger",
    Z + "tip.wortelwurger": "Zijn 4 voedingswortels zijn alleen kwetsbaar als het hart klopt (het wordt dan even donker). Kap ze door!",
    Z + "tip.nestwachter.titel": "De Oude Nestwachter",
    Z + "tip.nestwachter": "Jaarring-golf: sta op een ROZE ring. Rood = schouderstoot, ga opzij.",
    Z + "tip.bleu.titel": "Baron Bleu",
    Z + "tip.bleu": "Een brandende lantaarnpaal brandt de schimmel weg: vecht in het licht. Geel bij het ei = SAMEN erin staan.",
    Z + "tip.vahoegnir.titel": "Vahoegnir",
    Z + "tip.vahoegnir": "Je verslaat hem niet: je vrolijkt hem op. Zet de hartsplinters terug in de roze cirkel. Grijs? Vrolijk de kloddertjes op. Geel bij het ei = samen!",
    Z + "tip.lantaarnpaal.titel": "Lantaarnpaal",
    Z + "tip.lantaarnpaal": "Rechtsklik steekt hem aan. Licht helpt: in het donker raak je minder.",
    Z + "tip.altaar.titel": "Vahoeg-altaar",
    Z + "tip.altaar": "Met een Vahoegzegel in je tas (vanaf lv 50): rechtsklik. De bazen worden sterker en laten Gouden Veren vallen.",
    Z + "tip.kanon.titel": "Dekkanon",
    Z + "tip.kanon": "Rechtsklik vuurt waar je kijkt. Alleen herlaadt het sneller.",
    Z + "tip.loods.titel": "De Bewapeningsloods",
    Z + "tip.loods": "Elke hendel draait twee kanonnen een kwartslag. Richt ze alle drie op de poort!",
    Z + "tip.aderloop.titel": "De Aderloop",
    Z + "tip.aderloop": "Als het hart klopt, wordt het donker. Sta dan stil!",
    Z + "tip.ketel.titel": "Schimmelketel",
    Z + "tip.ketel": "Rechtsklik brandt hem uit (drie keer). Pas op voor de paarse sporenwolk.",
    Z + "tip.ringen.titel": "De Ringenzaal",
    Z + "tip.ringen": "Elke knop draait twee jaarringen een stukje. Leg de roze nerf in één lijn!",
    # items
}

ITEMS = {
    # type: (name, flavour, effect)
    "touwknots": ("Takelmeesters Touwknots", "\"Eén ruk en ze staan allemaal netjes op een rij.\"", "elke 4e klap trekt de wezens rond je doelwit naar je toe"),
    "lontstaf": ("Brokkels Lontstaf", "\"Niet aan het topje likken.\"", "kritieke treffers zetten het doelwit 3 s in brand"),
    "roquefortsabel": ("Roquefortsabel", "\"Ruikt naar zee. En naar kaas. Vooral naar kaas.\"", "+20 % schade op een brandend wezen"),
    "paraplu": ("De Paraplu van de Kapitein", "\"Een boog die ook tegen regen helpt.\"", "+15 % schade terwijl je in de lucht zweeft"),
    "kooisleutel": ("Kooisleutel", "\"Wie de kooi opent, krijgt vrienden voor het leven.\"", "elke 5e klap opent een kooitje: 3 s knabbelt een vriendelijke muisjeszwerm mee"),
    "sokkencape": ("Cape van gebreide sokken", "\"Een beetje kriebelig. Heel erg warm.\"", "12 % kans als je geraakt wordt: 5 % Vads terug (20 s afkoelen)"),
    "roofklauw": ("Roofvogelklauw", "\"Hij ziet je van verder dan jij hem.\"", "+20 % schade van meer dan 10 blokken afstand"),
    "kruinveren": ("Kruinverenmantel", "\"Zacht als dons, licht als een veertje.\"", "onder 30 % Vads: 3 s Snelpootjes (30 s afkoelen)"),
    "hartwortel": ("Wortelwurgers Hartwortel", "\"Hij klopt nog. Een beetje.\"", "3 % van je schade komt terug als Vads"),
    "lantaarnstaf": ("Knoest z'n Lantaarnstaf", "\"Het licht van het boomvolk dooft nooit.\"", "kritieke treffers geven je 4 s een Knuffelschild (20 s afkoelen)"),
    "schimmelbreker": ("Schimmelbreker", "\"Voor het rijpen te laat.\"", "+25 % schade op wezens met Gesporen of Plakkerig"),
    "bleumonocle": ("Monocle van Baron Bleu", "\"Hij zag alles. Behalve jou aankomen.\"", "10 % kans bij een treffer: het doelwit raakt Gesporen"),
    "eierwarmer": ("De Eierwarmer", "\"Warm als een ei in een nest.\"", "onder 25 % Vads gloeit je team roze en krijgt 15 % Vads terug (60 s afkoelen)"),
}

TROFEEEN = {b: n for b, (n, _, _) in BAZEN.items()}

HUISDIEREN = {
    "kruinroofvogel": ("Kruinroofvogeltje", "Een piepkleine roofvogel met verfomfaaide veertjes. Pikt graag naar je schoenveters.",
                       "Versla de Kruinroofvogel op de Kale Kruin"),
    "piepje": ("Piepje", "Het pieppiepmuisje uit de toren. Zijn wintervachtje is helemaal teruggegroeid.", "Versla Graaf Piepvel in de Pelszaal"),
    "vahoegje": ("Vahoegje", "Het babydraakje uit het ei. Niest roze vonkjes en ziet jou als zijn mama.",
                 "Vrolijk Vahoegnir op in het oude nest"),
}

FASES = {
    "kruinroofvogel": {2: "Grijpklauw!", 3: "De achtervolging!"},
    "taleggio": {2: "Aan de touwen!"},
    "brokkel": {2: "Meer kabaal!", 3: "Volle lading!"},
    "roquefort": {2: "Het dek kantelt!", 3: "De zeppelin zinkt!"},
    "piepvel": {2: "De kooitjes!", 3: "Donker!"},
    "wortelwurger": {2: "De kluwen knijpt!", 3: "Het hart bonst!"},
    "nestwachter": {2: "De jaren tellen!", 3: "Honderd winters!"},
    "bleu": {2: "De Baron zelf!", 3: "De grijparm!"},
    "vahoegnir": {2: "Zwarte pluis!", 3: "Kristalhuil!", 4: "Het hart!"},
}


def alle_teksten():
    t = dict(TEKSTEN)
    for b, (naam, titel, onder) in BAZEN.items():
        t["entity.guhs.vr_z3_baas_" + b] = naam
        t[Z + b] = titel
        t[Z + b + ".onder"] = onder
        t["item.guhs.vr_z3_baas_trofee_" + b] = "Trofee: " + naam
        for f, regel in FASES.get(b, {}).items():
            t[f"vadsrijk.kern.beleving.fase.vr_z3_baas_{b}.{f}"] = f"Fase {f}: {regel}"
    for typ, (naam, flavour, effect) in ITEMS.items():
        t["item.guhs.vr_z3_baas_" + typ] = naam
        t["vadsrijk.loot.z3baas_" + typ] = naam
        t["vadsrijk.loot.z3baas_" + typ + ".flavour.0"] = flavour
        t["vadsrijk.bazen.effect.z3baas_" + typ] = effect
    for h, (naam, tekst, bron) in HUISDIEREN.items():
        t[f"vadsrijk.huisdier.{h}.naam"] = naam
        t[f"vadsrijk.huisdier.{h}.tekst"] = tekst
        t[f"vadsrijk.huisdier.{h}.bron"] = bron
    return t


def build(h):
    bb = modellen.alles(h.w, h.A, modellen._png)
    modellen.blockbench(bb)
    for k, nl in alle_teksten().items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    teksten = alle_teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(teksten)
        _schrijf(p, data)
    print(f"vadsrijk_z3_bazen: {len(teksten)} texts (run vadsrijk_z3_bazen_modellen.py for the art)")


if __name__ == "__main__":
    _direct()
