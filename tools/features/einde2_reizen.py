"""
einde2 phase B (CONTRACT_E2_B 7.10), slice reizen: travelling in the Guheinde.
Java: nl.juiced.guhs.feature.einde2.reizen (E2ReizenFeature); it also owns the phase-A packages appa and zwevers.

  the story flights   E2Reizen.vlieg: a real flight on Appa between the chapters, the team on his back talking in the chat
                      (gui.guhs.einde2_reizen.chat.<vlucht>.<n>; who says which line is Reis.REGELS in Java: keep both in
                      step, CHAT below), Zuka's ship chase with a short dodge part on h1_h2 / h2_h3 (.jacht.*)
  the whistle list    Fluitlijst: blow the Appa-fluitje on Appa's head (.fluit.*)
  the team            E2Team: Team Guhvatar at the team_* markers, two lines of small talk per member per structure (.team.*)
  the whale           line "leegtewalvis" (gui.guhs.verhalen.leegtewalvis.*): the old captain, waiting in the void, scene
                      einde2_reizen_walvis, the proof, scene einde2_reizen_walvis_kroeg, a whale of your own (Walvis: the horn,
                      steering, the furnished back; the furniture is part of the whale's model, einde2_zwevers_modellen.py)
  models              einde2_reizen_modellen.py (Zuka's ship, the horn's picture)
  FTB                 one section "Reizen door het Guheinde" in guhs_guheinde
Chunk c98_e2_reizen (English: tools/lang/en/c98_e2_reizen.json).
"""
import os

from features import einde2_kit as kit
from features import verhaal_motor
from features import einde2_reizen_modellen as modellen

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTIES = [("einde2_reizen", "Reizen door het Guheinde", "npc:kapiteinguh", None)]

STRUCTUREN = ["zuidelijke_luchttempel", "noordelijke_waterstam", "ba_sing_guh", "vuurnatie_hoofdstad"]
LEDEN = ["aang", "katara", "sokka", "toph", "zuka"]

# =====================================================================================================================
# the chat on Appa's back: (vlucht, n) -> text. The speaker and the moment are in Java (Reis.REGELS / Reis.VRIJ); the
# numbers here must match. Lines of "momo" and "" are narrator lines (grey, in italics).
# =====================================================================================================================
CHAT = {
    "start": [
        "Jip jip! Hou je goed vast, nieuwe Guhvatar. Appa vliegt sneller dan hij eruitziet.",
        "Honderd jaar in een kaasbol. Mijn benen slapen nog. Mijn buik is gelukkig klaarwakker, njeg.",
        "Momo-guh snuffelt aan je oor, besluit dat jouw schouder van hem is en gaat er tevreden op zitten.",
        "Zie je die wolken daar? Daarachter hangt de Zuidelijke Luchttempel. Mijn thuis. Nu ook een beetje het jouwe.",
        "Water, aarde, vuur, lucht. Vier elementen. Klinkt als veel? Het is ook veel. Maar je hebt tijd. En knabbels.",
        "Weet je wat het fijnste is aan vliegen? Niemand vraagt je hier iets. Alleen de wind, en die zegt altijd hoi.",
        "Daar is ze! Appa ruikt zijn stal al. Hou je oren vast, we gaan landen.",
    ],
    "h1_h2": [
        "Op naar de Noordelijke Waterstam! Daar leer je watersturen. Brr, ik hoop dat jouw vacht warm genoeg is.",
        "De watermeester daar heet Pakguh. Hij is heel streng. En hij heeft een heel grote snor. Dat hoort bij elkaar, denk ik.",
        "Dat was Zuka. Hij jaagt op me sinds... nou ja, sinds gisteren. Hij wil zijn eer terug, zegt hij.",
        "Ik snap hem wel, een beetje. Iedereen zoekt iets. Hij zoekt alleen op de verkeerde plek.",
        "Momo-guh kruipt onder Appa's vacht vandaan. Hij piept pas weer nu het schip echt weg is.",
        "IJs! Kilometers ijs! Ik ga zo meteen op mijn buik glijden. Doe je mee?",
    ],
    "h2_h3": [
        "Oké team, het plan. Stap één: we landen in Ba Sing Guh. Stap twee: ...daar werk ik nog aan.",
        "Aang, je zit scheef op Appa's kop. Leun iets naar voren, dan stuurt hij rustiger.",
        "Ik vlieg al honderd jaar op hem, Katara!",
        "Je hebt honderd jaar geslapen, Aang. Dat is iets anders.",
        "Zie je? Ik zei toch dat hij terug zou komen! Niemand luistert ooit naar Sokka-guh.",
        "Momo-guh komt tevoorschijn met een gedroogd visje in zijn bek. Het noodvisje van Sokka-guh.",
        "Hé! Dat was mijn noodvisje! Voor noodgevallen! Dit WAS een noodgeval!",
        "Zuka zag er... moe uit. Bijna verdrietig. Vonden jullie dat ook?",
        "Iedereen verdient een tweede kans. Zelfs prinsen met een raar staartje.",
        "De muur! Kijk hoe groot die muur is! Daar kan ik een heel plan omheen maken.",
    ],
    "h3_h4": [
        "Ik haat vliegen. Er is hier geen GROND. Ik voel helemaal niks, Sokka!",
        "Daarom heet het vliegen, Toph.",
        "Iedereen klaar voor de Vuurnatie? We moeten opgaan in de menigte. Geen gekke dingen.",
        "Ik ga gewoon ergens zitten en doe alsof ik een steen ben. Daar ben ik heel goed in.",
        "Ik ben eerlijk gezegd een beetje bang. Maar met jullie erbij... een stuk minder.",
        "Plan voor de vermomming: ik heb alvast een snor gemaakt. Van Appa-pluis.",
        "Appa bromt heel diep en heel beledigd. Momo-guh probeert de snor op te eten.",
        "Zijn we er al? ...Zijn we er al? Ik ga Appa's vacht knuffelen tot we er zijn.",
        "Wat er daarbeneden ook gebeurt: we doen het samen. Afgesproken?",
        "Iedereen legt een hand op Appa's vacht. Zelfs Toph. Appa bromt, zacht nu.",
        "Daar is ze. De krater. Blijf dicht bij elkaar, team.",
    ],
    "vrij": [
        "Appa vindt deze route mooi. Ik zie het aan zijn oren.",
        "Weet je wat ik zou doen als ik geen Guhvatar was? Luchtscooteren. De hele dag.",
        "Kijk, een Kruimelkwal! Of een wolk. Of een kwal die een wolk wil zijn.",
        "Zit je wel goed? Er ligt een dekentje in de zadelmand, als je het koud hebt.",
        "Het is zo stil hierboven. Even nergens aan denken. Dat mag ook, weet je.",
        "Ik heb onderweg een kaart getekend. Hij hangt op zijn kop, maar hij is wél mooi.",
        "Iemand honger? Ik heb... nee. Ik had. MOMO!",
        "Als ik ooit een eigen luchtbizon krijg, noem ik hem Appa Twee. Of Bizonbert. Bizonbert is beter.",
        "Ik voel geen grond. Ik voel geen grond. Ik voel geen grond...",
        "Zeg maar wanneer we landen. Dan doe ik weer alsof ik dit leuk vond.",
        "Op Appa vliegen is beter dan op mijn oude schip. Hier staat tenminste niemand te schreeuwen.",
        "Oom zou nu zeggen dat de reis belangrijker is dan waar je aankomt. En dan zou hij om thee vragen.",
        "Momo-guh rolt zich op in je capuchon en snurkt heel zacht.",
        "Momo-guh jat een knabbel uit de tas van Sokka-guh. Sokka-guh merkt het pas veel later.",
        "Appa's zes poten bewegen langzaam mee met de wind, alsof hij door de lucht zwemt.",
        "Ergens ver weg zingt iets, laag en lang. De leegte is minder leeg dan je denkt.",
    ],
}

# =====================================================================================================================
# the team round the campfire: structure -> member -> (line 1, line 2)
# =====================================================================================================================
TEAM = {
    "zuidelijke_luchttempel": {
        "aang": ("Hier leerde ik vliegen. En vallen. Vooral vallen, njeg.",
                 "Monnik Guhatso zei altijd: met een lege maag zweef je niet. Daarom at hij zoveel taart."),
        "katara": ("Aang laat me elke hoek zien. Hij lacht, maar bij de lege kamers worden zijn oren heel stil.",
                   "Een tempel in de wolken... Bij ons thuis hadden we alleen sneeuw. Heel veel sneeuw."),
        "sokka": ("Ik heb een plattegrond van de tempel gemaakt. Hij klopt niet, maar hij is wel heel mooi.",
                  "Waarom zijn alle deuren hier rond? Wie bouwt er nou ronde deuren? ...Oké. Ze zijn best handig."),
        "toph": ("Een tempel die zweeft. Geweldig. Een hele tempel waar ik helemaal niks van voel.",
                 "Zeg maar als we weer op echte grond staan. Ik wacht hier. Op een steen. Heel stil."),
        "zuka": ("Hier stond ik ooit met een schip vol plannen. Nu sta ik hier met een kop thee. Dat voelt beter.",
                 "Aang wil me de luchtbaltoren laten zien. Ik heb het gevoel dat ik ga verliezen."),
    },
    "noordelijke_waterstam": {
        "aang": ("Weet je nog, mijn eerste waterzweep? Ik raakte Pakguh's snor. Per ongeluk! ...Grotendeels per ongeluk.",
                 "Het ijs hier glijdt zo lekker. Ik heb Appa al drie keer laten uitglijden. Hij vond het niet grappig."),
        "katara": ("Hier leerde ik dat ik niets hoef te bewijzen aan wie niet wil kijken. Alleen aan mezelf.",
                   "Oma's ketting vangt het maanlicht hier zo mooi. Dan voelt ze heel dichtbij."),
        "sokka": ("Ik kijk elke avond even naar de maan. En de maan kijkt terug, denk ik. Dat is toch normaal?",
                  "Plan voor vandaag: vissen. Plan voor morgen: meer vissen. Ik word steeds beter in plannen."),
        "toph": ("IJs. Het is alsof iemand de grond in een glas water heeft gestopt. Ik voel alles... wazig.",
                 "Ze zeggen dat het hier mooi is. Ik zeg: koud. We hebben allebei gelijk."),
        "zuka": ("Ik ben hier ooit stiekem geweest, met een blauw masker op. Niemand weet het. ...Nou ja, jij nu wel. Ssst.",
                 "De mensen hier kijken me nog steeds een beetje schuin aan. Terecht. Ik doe mijn best."),
    },
    "ba_sing_guh": {
        "aang": ("Er is hier geen knabbeltekort, zeggen ze. Ik heb het gecontroleerd. Er is wél een knabbeltekort. In mijn buik.",
                 "Ik mis Appa nog elke keer als ik deze muren zie. Gelukkig is hij terug. Ik knuffel hem dubbel."),
        "katara": ("Deze stad heeft geheimen in haar muren. Ik vertrouw ze nog steeds niet helemaal.",
                   "Het theehuis van Oom Miroh ruikt naar jasmijn tot op dit plein. Dat maakt alles een beetje zachter."),
        "sokka": ("Ik heb hier een hele koolkraam zien sneuvelen. Arme man. Ik heb er een plan voor bedacht: minder kool.",
                  "De monorail! Ik wil nog een keer. En nog een keer. Voor onderzoek, natuurlijk."),
        "toph": ("Dit is mijn stad niet, maar de stenen kennen me. Ze zeggen dat jij goed op je voeten staat.",
                 "Wil je zien hoe De Blinde Bandiet een muur verzet? ...Grapje. Of niet. Let maar op je tenen."),
        "zuka": ("Ik schonk hier thee aan vreemden. Het was het rustigste wat ik ooit gedaan heb. Zeg het niet tegen Oom.",
                 "Oom zegt dat een goed kopje thee geen haast heeft. Ik leer het. Langzaam."),
    },
    "vuurnatie_hoofdstad": {
        "aang": ("Ik dacht dat ik hier bang zou zijn. Maar de mensen... dat zijn gewoon mensen. Met heel lekkere broodjes.",
                 "De schildpad-eendjes in de paleisvijver herkennen me al. Ik heb ze misschien iets te veel brood gegeven."),
        "katara": ("We kwamen hier als vreemden met een vermomming. Nu groeten de bakkers ons. Dingen kunnen veranderen.",
                   "Je moet Zuka zien als niemand kijkt. Dan voert hij de eendjes. Met een heel klein lachje."),
        "sokka": ("Weet je wat ik hier het lekkerst vind? Alles. Behalve het extra hete vuurvlees. Mijn tong is nog boos op me.",
                  "Ik heb het toneelstuk over ons gezien. In het echt was ik veel grappiger. Echt waar."),
        "toph": ("Hier is de grond warm. Daar kan ik mee leven. Warme voeten, blije Toph.",
                 "Ik hoor overal mensen fluisteren over de Guhvatar. Je bent beroemd, njeg. Probeer niet te zweten."),
        "zuka": ("Dit was een thuis vol angst. Ik wil dat het een thuis vol thee wordt. Oom helpt.",
                 "Ik loop hier nu mét jullie in plaats van achter jullie aan. De straten voelen breder."),
    },
}

JACHT = {
    "komt": "Achter jullie klinkt een stoomfluit. Uit de wolken duikt een grijs schip met een rode vlag op.",
    "zuka.h1_h2": "GUHVATAR! Je dacht toch niet dat ik je zomaar liet gaan?! Vuur!",
    "zuka.h2_h3": "Daar ben je weer! Waar jij heen vliegt, vlieg ik achteraan. Dat beloof ik je!",
    "team.h1_h2": "Zuka! Appa, ontwijken! Hou je vast!",
    "team.h2_h3": "Hij weer?! Heeft die jongen nou echt geen hobby's?",
    "uitleg": "Vuurballen! Druk op een looptoets en Appa duikt opzij: links, rechts, springen (omhoog) of sluipen (omlaag).",
    "eer": "MIJN... EER...!",
    "weg": "Uit Zuka's schoorsteen komt een zielig wolkje. Hoestend zakt het schip achter jullie weg. Niemand heeft ook maar een schrammetje.",
    "score": "Appa ontweek %s van de %s vuurballen. Hij snuift trots.",
    "vuurbal": "VUURBAL! Duik weg: links, rechts, springen of sluipen!",
    "geraakt": "Boem! Appa wordt opzij geduwd. Hij schudt zijn vacht uit: alles goed, alleen een barbecuegeurtje.",
    "ontweken": "Appa duikt weg. De vuurbal sist onschuldig voorbij.",
}

KAPITEIN = {
    "verhaal.1": "Kom zitten, jonkie. De rest van de kroeg heeft het al honderd keer gehoord. En honderd keer gelachen.",
    "verhaal.2": "Zestig jaar geleden voer ik met een vetschip door de leegte. Ik zocht de Witte Knabbel (vraag daar maar niet "
                 "naar). Midden in de nacht hoorde ik iets zingen. Laag. Zó laag dat mijn knabbels ervan trilden.",
    "verhaal.3": "Toen zag ik hem. Een walvis zo groot als een eiland, met sterretjes op zijn vel en oren als schoteltjes. Hij keek me "
                 "aan. En toen was hij weg.",
    "verhaal.4": "Ik heb hem nooit meer gezien. Ze zeggen dat hij zich alleen laat zien aan wie in hem gelooft. En ik... ik ben het "
                 "geloven misschien een beetje kwijtgeraakt.",
    "verhaal.5": "Dus. Lach je mee met de rest? Of geloof je een ouwe kapitein?",
    "ja": "Ik geloof je.",
    "nee": "Sterk verhaal, opa.",
    "ongelovig": "Hm. Jij ook al. Kom maar terug als je ooit wilt geloven. Ik zit hier toch, njeg.",
    "geloof": "Je gelooft me? Echt? ...Dan moet je hem zien. Vlieg naar x %s, z %s, ver buiten de haven, boven de leegte. Wacht daar. "
              "Stil. Hij komt als je luistert.",
    "wacht": "Vlieg naar x %s, z %s, boven de leegte, en wacht daar heel stil. Niet ongeduldig worden: walvissen hebben geen horloge.",
    "bewijs": "Je hebt hem gezien?! Mijn oren tintelen ervan! Maar de kroeg gelooft het pas met bewijs. Voer hem een knabbelkoorvrucht: "
              "hij bedankt je met een leegteparel. Breng mij die.",
    "temmen": "Hij zwemt nog rond, hè? Voer hem vaak, dan gaat hij je vertrouwen. %s van de %s. Dan wordt hij de jouwe.",
    "klaar.1": "Mijn hoorn, op jouw walvis. Mooier kan een verhaal niet verdergaan, jonkie.",
    "klaar.2": "Groet hem van me, als je hem ziet. Zeg maar: de ouwe kapitein lacht weer.",
    "klaar.3": "Ik zit hier elke avond met een glas knabbelsap. En nu luisteren ze tenminste, njeg.",
    "hoorn": "De kapitein drukt zijn oude hoorn in je handen. 'Zestig jaar heb ik erop geblazen en nooit kwam er iemand. Voer hem vaak, "
             "dan wordt hij de jouwe. En dan komt hij als jij blaast.'",
}

KROEGBAAS = {
    "spot.1": "Heeft Opa Walvis je weer aan zijn bankje gekregen? Zestig jaar hetzelfde verhaal. Hij is lief, hoor. Maar een walvis? Kom nou.",
    "spot.2": "Wat mag het zijn? Knabbelsap? En als je naar die walvis vraagt, krijg je er gratis een lach bij.",
    "twijfel.1": "Jij zegt dat je hem gezien hebt? ...Zonder bewijs is het gewoon een tweede sterk verhaal, maatje.",
    "twijfel.2": "Een walvis zo groot als een eiland. Natuurlijk. En ik ben de Koningguh. Breng bewijs, dan geloof ik het.",
    "spijt.1": "Zestig jaar heb ik om hem gelachen. Nu krijgt hij elke avond gratis knabbelsap. Het minste wat ik kan doen.",
    "spijt.2": "Soms hoor ik 's nachts iets zingen, buiten in de leegte. Dan glimlach ik. De kapitein ook.",
}

WALVIS = {
    "gevoerd": "Walvis gevoerd",
    "eigen": "Een eigen Leegtewalvisguh",
    "van_ander": "Deze walvis zwemt al met iemand anders. Zoek een vrije, njeg.",
    "vertrouwen": "De walvis zingt een zacht dankjewel. Hij vertrouwt je weer een beetje meer: %s van de %s.",
    "getemd": "De walvis zingt een nieuw lied. Alleen voor jou. Dit is nu JOUW walvis!",
    "uitleg": "Blaas op de hoorn en hij komt. Sta op zijn rug met de hoorn in je hand en hij zwemt waar jij kijkt (kijk omhoog of "
              "omlaag: hij stijgt of zakt). Sluip + blaas: richt zijn rug in.",
    "kist": "Walviskist %s",
    "kist_niet": "Deze kist is van de baas van deze walvis. De walvis knipoogt, maar laat je er niet in.",
    "tuin_niet": "Dit tuintje is van de baas van deze walvis. Kijken mag.",
    "tuin": "Je plukt twee knabbelkoorvruchten uit het walvistuintje. Je walvis smakt alvast.",
    "tuin_morgen": "Het tuintje rust. Morgen groeit er weer wat, njeg.",
    "dutje": "Je doet een dutje op de zachte walvisrug. Zijn lied wiegt je. Je voelt je helemaal uitgerust.",
    "wakker": "Je bent nog helemaal uitgerust. Straks weer een dutje.",
    "huisje": "Je kijkt in het huisje: een hangmat, een theepot en een kaart met al je reizen erop. Het ruikt naar knabbels.",
}

LANG = {
    # the ship and the horn
    "entity.guhs.einde2_reizen_zuka_schip": "Zuka's schip",
    "item.guhs.einde2_reizen_walvishoorn": "Hoorn van de kapitein",
    "item.guhs.einde2_reizen_walvishoorn.lore": "Zestig jaar heeft hij erop geblazen. Nu komt er eindelijk iemand.",
    "item.guhs.einde2_reizen_walvishoorn.tooltip": "Blaas (in het Guheinde): je eigen walvis komt naar je toe.",
    "item.guhs.einde2_reizen_walvishoorn.tooltip.stuur": "Op zijn rug met de hoorn in je hand: hij zwemt waar jij kijkt.",
    "item.guhs.einde2_reizen_walvishoorn.tooltip.inrichting": "Sluip + blaas: richt zijn rug in.",
    "gui.guhs.einde2_reizen.hoorn.geen": "Je blaast. Een diepe toon rolt door de leegte... maar er is nog geen walvis die naar jou luistert.",
    "gui.guhs.einde2_reizen.hoorn.niet_hier": "Je walvis zwemt alleen in de leegte van het Guheinde. Daar hoort hij je.",
    "gui.guhs.einde2_reizen.hoorn.komt": "Je toetert op de hoorn. Ver weg antwoordt een laag lied: je walvis komt eraan!",
    # a story flight that waits
    "gui.guhs.einde2_reizen.wacht": "Je bent nog niet bij %s. Klim op Appa's kop en blaas op het Appa-fluitje: dan vliegen jullie verder.",
    # the whistle list
    "gui.guhs.einde2_reizen.fluit.titel": "Waar vliegen we heen?",
    "gui.guhs.einde2_reizen.fluit.uitleg": "Appa kent de weg naar elk gebouw dat jij kent. Kies, en hij vliegt (sturen mag). Bij de ster gaat je verhaal verder.",
    "gui.guhs.einde2_reizen.fluit.leeg": "Je kent nog geen gebouwen in het Guheinde. Ga op ontdekking, dan onthoudt Appa ze.",
    "gui.guhs.einde2_reizen.fluit.afstand": "%s blokken",
    "gui.guhs.einde2_reizen.fluit.verder": "(verder met het verhaal)",
    "gui.guhs.einde2_reizen.fluit.blijf": "Blijf hier",
    "gui.guhs.einde2_reizen.fluit.niet_hier": "In de Geestenwereld kan Appa je niet volgen. Keer eerst terug naar je lichaam.",
    "gui.guhs.einde2_reizen.fluit.niet_op_appa": "Ga eerst op Appa's kop zitten, njeg.",
    "gui.guhs.einde2_reizen.fluit.kan_niet": "Appa kan daar nu niet heen vliegen.",
    "gui.guhs.einde2_reizen.fluit.op_weg": "Jip jip! Op weg naar %s.",
    # einde2 fix-reizen: his menu on a right-click, the story's place, through the void from another dimension
    "gui.guhs.einde2_reizen.fluit.opstappen": "Opstappen (zelf vliegen)",
    "gui.guhs.einde2_reizen.fluit.verhaal": "(hier gaat je verhaal verder)",
    "gui.guhs.einde2_reizen.fluit.leegte": "(door de leegte)",
    "gui.guhs.einde2_reizen.fluit.ver": "Appa staat te ver weg. Roep hem eerst met het Appa-fluitje.",
    "gui.guhs.einde2_reizen.fluit.nog_niet": "Appa snuffelt de weg daarheen nog uit. Probeer het zo nog eens.",
    "gui.guhs.einde2_reizen.leegte.op_weg": "Appa bromt, zwiept met zijn staart en duikt met je de leegte in, op weg naar %s...",
    "gui.guhs.einde2_reizen.leegte.aangekomen": "Daar zijn jullie: %s!",
    # the whale questline: where to go, waiting in the void
    "gui.guhs.einde2_reizen.doel.kroeg": "naar de havenkroeg van de Leegtehaven",
    "gui.guhs.einde2_reizen.doel.wachten": "naar de plek van de kapitein, boven de leegte",
    "gui.guhs.einde2_reizen.wachten.0": "De leegte is stil. Je wacht...",
    "gui.guhs.einde2_reizen.wachten.1": "Geen wind. Geen vogels. Je wacht nog even...",
    "gui.guhs.einde2_reizen.wachten.2": "Was dat... iets? Je houdt je adem in.",
    "gui.guhs.einde2_reizen.wachten.3": "Diep onder je trilt de stilte.",
    "gui.guhs.einde2_reizen.wachten.4": "Daar. Een lied, heel ver weg.",
    "gui.guhs.einde2_reizen.wacht_weg": "Je bent niet meer op de plek van de kapitein. Ga terug en wacht boven de leegte.",
    # the furniture screen
    "gui.guhs.einde2_reizen.inrichting.titel": "De rug van je walvis",
    "gui.guhs.einde2_reizen.inrichting.uitleg": "Kies wat er op zijn rug staat. Spullen in de kisten blijven bewaard, ook als je de kisten opbergt.",
    "gui.guhs.einde2_reizen.inrichting.deel.huisje": "Huisje",
    "gui.guhs.einde2_reizen.inrichting.deel.kisten": "Twee kisten",
    "gui.guhs.einde2_reizen.inrichting.deel.bed": "Bed",
    "gui.guhs.einde2_reizen.inrichting.deel.tuin": "Tuintje",
    "gui.guhs.einde2_reizen.inrichting.deel.lantaarns": "Lantaarns",
    "gui.guhs.einde2_reizen.inrichting.deel.vlag": "Vlag",
    "gui.guhs.einde2_reizen.inrichting.aan": "staat erop",
    "gui.guhs.einde2_reizen.inrichting.uit": "opgeborgen",
    "gui.guhs.einde2_reizen.inrichting.vlag_kleur": "Kleur van de vlag",
    "gui.guhs.einde2_reizen.inrichting.kleur.0": "Rood",
    "gui.guhs.einde2_reizen.inrichting.kleur.1": "Blauw",
    "gui.guhs.einde2_reizen.inrichting.kleur.2": "Geel",
    "gui.guhs.einde2_reizen.inrichting.kleur.3": "Groen",
    "gui.guhs.einde2_reizen.inrichting.kleur.4": "Paars",
    "gui.guhs.einde2_reizen.inrichting.kleur.5": "Roze",
    "gui.guhs.einde2_reizen.inrichting.klaar": "Klaar",
    # sounds
    "subtitles.guhs.einde2_reizen.hoorn": "Walvishoorn toetert",
    "subtitles.guhs.einde2_reizen.vuurbal": "Vuurbal suist",
    "subtitles.guhs.einde2_reizen.vuurbal_plof": "Vuurbal ploft",
    "subtitles.guhs.einde2_reizen.schip": "Stoomfluit van Zuka's schip",
    "subtitles.guhs.einde2_reizen.proost": "Glazen klinken",
}

SOUNDS = {
    "einde2_reizen.hoorn": [{"name": "minecraft:item.goat_horn.sound.0", "type": "event", "pitch": 0.6},
                            {"name": "minecraft:item.goat_horn.sound.4", "type": "event", "pitch": 0.55}],
    "einde2_reizen.vuurbal": [{"name": "minecraft:entity.blaze.shoot", "type": "event", "pitch": 0.7},
                              {"name": "minecraft:item.firecharge.use", "type": "event", "pitch": 0.8}],
    "einde2_reizen.vuurbal_plof": [{"name": "minecraft:entity.firework_rocket.blast", "type": "event", "pitch": 0.6},
                                   {"name": "minecraft:block.fire.extinguish", "type": "event", "pitch": 0.9}],
    "einde2_reizen.schip": [{"name": "minecraft:item.goat_horn.sound.3", "type": "event", "pitch": 1.4}],
    "einde2_reizen.proost": [{"name": "minecraft:block.amethyst_block.chime", "type": "event", "pitch": 1.5},
                             {"name": "minecraft:entity.villager.celebrate", "type": "event", "pitch": 1.4}],
}

# the whale questline (Java: Leegtewalvis)
LIJN = "leegtewalvis"
STAPPEN = [
    ("Luister naar de kapitein", "Praat met de oude kapitein-guh in havenkroeg De Vadsige Walvis. Iedereen lacht om zijn verhaal. Jij luistert.",
     "Havenkroeg De Vadsige Walvis, in de Leegtehaven"),
    ("Wacht in de leegte", "Vlieg naar de plek die de kapitein noemde en wacht daar, boven de leegte. Stil. Luister.",
     "De leegte, ver buiten de Leegtehaven (je kompas wijst de plek)"),
    ("Breng bewijs", "Voer de walvis een knabbelkoorvrucht. Breng de leegteparel die je krijgt naar de kapitein.",
     "Havenkroeg De Vadsige Walvis, in de Leegtehaven"),
    ("Een eigen walvis", "Voer een walvis acht keer een knabbelkoorvrucht. Dan vertrouwt hij je en wordt hij de jouwe.",
     "Bij een Leegtewalvisguh, tussen de buiteneilanden"),
]
KLAAR = ("Je hebt je eigen walvis! Blaas op de hoorn en hij komt.", "Overal in de leegte van het Guheinde")
KORT = {"0": "Luister naar de oude kapitein in de havenkroeg", "1": "Wacht boven de leegte, op de plek van de kapitein",
        "2": "Breng een leegteparel naar de kapitein", "3": "Voer een walvis knabbelkoorvruchten"}

SCENE_WALVIS = ("einde2_reizen_walvis", "De grote zingende", {
    "stil": "Het is doodstil in de leegte. Geen wind. Geen vogels. Alleen jij, en je eigen adem.",
    "lied": "Dan hoor je het. Laag, heel ver weg... en toch overal tegelijk.",
    "omhoog": "Uit de diepte onder je komt iets omhoog. Iets groots. Iets heel, héél groots.",
    "oogjes": "Twee enorme guh-oogjes kijken je aan. Twee ronde oren flapperen. En hij zingt. Voor jou.",
    "gelijk": "De oude kapitein had gelijk. Al die jaren. De grote zingende bestaat echt.",
    "gelooft": "En nu weet hij dat jij in hem gelooft. Njeg.",
}, {})
SCENE_KROEG = ("einde2_reizen_walvis_kroeg", "De kroeg gelooft", {
    "spot": "Kijk eens wie daar is! Kom je ook een sterk verhaal halen bij Opa Walvis?",
    "lach": "Hahaha! De grote zingende! Vertel nog eens over die keer dat hij...",
    "parel": "Je legt de leegteparel op de lessenaar. Hij gloeit, zacht en paars. De hele kroeg valt stil.",
    "fluister": "Dat... dat is een leegteparel. Die krijg je alleen van een...",
    "stil": "...van de grote zingende.",
    "zestig": "Zestig jaar heb ik het verteld. Zestig jaar hebben jullie gelachen. Maar deze hier... deze geloofde me.",
    "sorry": "Kapitein... het spijt me. Echt waar. De volgende ronde is van het huis. Voor iedereen!",
    "proost": "OP DE GROTE ZINGENDE!",
    "jij": "En op jou, jonkie. Kom straks even bij me zitten. Ik heb nog iets voor je.",
}, {"kapitein": "Oude kapitein-guh", "kroegbaas": "Kroegbaas", "veerguh": "Veerman-guh", "bouwer": "Scheepsbouwer-guh",
    "racer": "Racemeester-guh"})

VERBORGEN = ["einde2_reizen_fluitvlucht", "einde2_reizen_gast", "einde2_reizen_jacht", "einde2_reizen_ingericht"]


def texts(h):
    for key, text in LANG.items():
        kit.lang(h, key, text)
    for vlucht, regels in CHAT.items():
        for i, t in enumerate(regels, 1):
            kit.lang(h, f"gui.guhs.einde2_reizen.chat.{vlucht}.{i}", t)
    for s, leden in TEAM.items():
        for lid, (een, twee) in leden.items():
            kit.lang(h, f"gui.guhs.einde2_reizen.team.{s}.{lid}.1", een)
            kit.lang(h, f"gui.guhs.einde2_reizen.team.{s}.{lid}.2", twee)
    for key, t in JACHT.items():
        kit.lang(h, f"gui.guhs.einde2_reizen.jacht.{key}", t)
    for key, t in KAPITEIN.items():
        kit.lang(h, f"gui.guhs.einde2_reizen.kapitein.{key}", t)
    for key, t in KROEGBAAS.items():
        kit.lang(h, f"gui.guhs.einde2_reizen.kroegbaas.{key}", t)
    for key, t in WALVIS.items():
        kit.lang(h, f"gui.guhs.einde2_reizen.walvis.{key}", t)
    verhaal_motor.verhaallijn(h, LIJN, "De grote zingende",
                              "In havenkroeg De Vadsige Walvis vertelt een oude kapitein-guh al zestig jaar over een walvis die zingt. "
                              "Iedereen lacht hem uit. Alleen wie gelooft, ziet hem.", STAPPEN, klaar=KLAAR, kort=KORT)
    for sid, titel, regels, namen in (SCENE_WALVIS, SCENE_KROEG):
        verhaal_motor.scene(h, sid, titel, regels, namen)


def items_and_sounds(h):
    h.item_model("einde2_reizen_walvishoorn")

    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    for naam in VERBORGEN:
        kit.verborgen(h, naam)


# the number of lines Java's Reis.REGELS / Reis.VRIJ name (keep in step)
CHAT_AANTAL = {"start": 7, "h1_h2": 6, "h2_h3": 10, "h3_h4": 11, "vrij": 16}


def selfcheck(h):
    problems = modellen.check(h)
    for vlucht, n in CHAT_AANTAL.items():
        if len(CHAT[vlucht]) != n:
            problems.append(f"chat {vlucht}: {len(CHAT[vlucht])} lines, Java names {n}")
    for s in STRUCTUREN:
        for lid in LEDEN:
            if lid not in TEAM.get(s, {}):
                problems.append(f"team: no small talk for {lid} in {s}")
    every = list(LANG.items()) + [(k, v) for d in (JACHT, KAPITEIN, KROEGBAAS, WALVIS) for k, v in d.items()]
    every += [(f"chat.{v}.{i}", t) for v, r in CHAT.items() for i, t in enumerate(r, 1)]
    every += [(f"team.{s}.{l}", " ".join(t)) for s, d in TEAM.items() for l, t in d.items()]
    for key, text in every:
        low = text.lower()
        for woord in ("laatste", "allerlaatste", "het einde van", "het einde"):   # (C-A 1: open ends)
            if woord in low:
                problems.append(f"open ends: {key} says '{woord}'")
    for key in ("entity.guhs.einde2_reizen_zuka_schip", "item.guhs.einde2_reizen_walvishoorn", f"gui.guhs.verhalen.{LIJN}.naam",
                f"scene.guhs.{SCENE_WALVIS[0]}.titel", f"scene.guhs.{SCENE_KROEG[0]}.titel"):
        if key not in h.NL:
            problems.append(f"missing lang {key}")
    for p in (f"{h.A}/models/item/einde2_reizen_walvishoorn.json", f"{h.A}/textures/item/einde2_reizen_walvishoorn.png",
              f"{h.D}/advancement/quest/{LIJN}_stap_4.json"):
        if not os.path.exists(p):
            problems.append(f"missing {p}")
    if problems:
        raise SystemExit("einde2_reizen self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    items_and_sounds(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: "Reizen door het Guheinde" (no locks; the whale's steps are its questline's advancements)
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("einde2_reizen_fluit", "Waar vliegen we heen?", "Ga op de kop van je &dAppa-guh&r zitten en blaas op het &6Appa-fluitje&r. Je "
      "krijgt een lijstje van elk gebouw dat je in het Guheinde kent. Kies er een en Appa vliegt erheen: sturen mag, loslaten ook, "
      "dan vliegt hij zelf. Je team zit gezellig mee op zijn rug.", "guhs:appa_fluitje", [adv("einde2_reizen_fluitvlucht")],
      rewards=(("guhs:kaas_knabbels", 8),), shape="gear", xp=50)
    q("einde2_reizen_gast", "Mee op Appa", "Klim bij een vriend in de &dzadelmand&r van zijn Appa-guh en vlieg samen ergens heen. "
      "Zolang je met hem meereist ben je zijn &bgast&r: je mag mee waar hij mag komen, en je ziet zijn team. Je eigen verhaal blijft "
      "gewoon van jou.", "guhs:appa_guh_spawn_egg", [adv("einde2_reizen_gast")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("einde2_reizen_jacht", "Ontwijk Zuka!", "Onderweg naar het volgende hoofdstuk duikt soms &cZuka's schip&r op. Hij schiet "
      "vuurballen die alleen duwen. Druk op een looptoets als er een aankomt en Appa duikt opzij. Hoeveel ontwijk jij er?",
      "minecraft:fire_charge", [adv("einde2_reizen_jacht")], rewards=(("guhs:kaas_knabbels", 12),), xp=80)
    q("einde2_reizen_walvis_1", "De grote zingende", "In havenkroeg &6De Vadsige Walvis&r in de &eLeegtehaven&r vertelt een oude "
      "kapitein-guh over een walvis die zingt. De hele kroeg lacht. Luister naar zijn verhaal... en geloof hem.",
      "guhs:leegtewalvisguh_spawn_egg", [adv(f"{LIJN}_stap_1")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("einde2_reizen_walvis_2", "Wachten in de leegte", "Vlieg naar de plek die de kapitein noemde en wacht daar boven de leegte. "
      "Heel stil. Eerst hoor je het lied...", "guhs:leegteparel", [adv(f"{LIJN}_stap_2")], rewards=(("guhs:knabbelkoorvrucht", 4),),
      deps=("einde2_reizen_walvis_1",), xp=100)
    q("einde2_reizen_walvis_3", "Bewijs voor de kroeg", "Voer de walvis een &5knabbelkoorvrucht&r en breng de &dleegteparel&r naar de "
      "kapitein. Nu moet de kroeg wel geloven!", "guhs:leegteparel", [adv(f"{LIJN}_stap_3")], rewards=(("guhs:kaas_knabbels", 16),),
      deps=("einde2_reizen_walvis_2",), xp=150)
    q("einde2_reizen_walvis_4", "Een eigen walvis", "Voer een walvis acht keer een knabbelkoorvrucht. Dan vertrouwt hij je en wordt "
      "hij de jouwe: blaas op de &6hoorn van de kapitein&r en hij komt, sta op zijn rug met de hoorn in je hand en hij zwemt waar jij "
      "kijkt.", "guhs:einde2_reizen_walvishoorn", [adv(f"{LIJN}_stap_4")], rewards=(("guhs:knabbelkoorvrucht", 8),),
      deps=("einde2_reizen_walvis_3",), shape="gear", xp=250)
    q("einde2_reizen_ingericht", "Een huisje op een walvis", "Sluip en blaas op de hoorn: richt de rug van je walvis in. Een huisje, "
      "twee kisten die echt spullen bewaren, een bed voor een dutje, een tuintje met knabbelkoorvruchten, lantaarns en een vlag in "
      "jouw kleur.", "minecraft:red_bed", [adv("einde2_reizen_ingericht")], rewards=(("guhs:kaas_knabbels", 16),),
      deps=("einde2_reizen_walvis_4",), xp=100)
