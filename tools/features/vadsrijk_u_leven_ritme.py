"""
1.10.0 "Het Vadsrijk", round U, U-LEVEN-RITME: day/night rhythm in the towns (shops close, night bell, sleepers, owls,
night market), weather with charm per zone, reactions to emotes + the knuffelmeter, the Pluizenblaadje (news + chores).
Java: feature/vadsrijk/ritme (+ client). Texts only (nl + real English).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_leven_ritme.py
"""
import json
import os

P = "vadsrijk.uritme."

STEDEN = {
    # id: (nl name, en name, nl newspaper, en newspaper)
    "knuffelkamp": ("Knuffelkamp", "Knuffelkamp", "Het Knuffelkampse Pluizenblaadje", "The Knuffelkamp Fluff Gazette"),
    "zwamzoet": ("Zwamzoet", "Zwamzoet", "Het Zwamzoetse Pluizenblaadje", "The Zwamzoet Fluff Gazette"),
    "haven": ("De Haven", "The Harbour", "Het Havense Pluizenblaadje", "The Harbour Fluff Gazette"),
    "kandijkom": ("Kandijkom", "Kandijkom", "Het Kandijkomse Pluizenblaadje", "The Kandijkom Fluff Gazette"),
    "honingtak": ("Honingtak", "Honingtak", "Het Honingtakse Pluizenblaadje", "The Honingtak Fluff Gazette"),
    "lampionhaven": ("Lampionhaven", "Lampionhaven", "Het Lampionhavense Pluizenblaadje", "The Lampionhaven Fluff Gazette"),
    "hoogvads": ("Hoogvads", "Hoogvads", "Het Hoogvadse Pluizenblaadje", "The Hoogvads Fluff Gazette"),
    "rozenkloof": ("Rozenkloof", "Rozenkloof", "Het Rozenkloofse Pluizenblaadje", "The Rozenkloof Fluff Gazette"),
    "saffraanpoort": ("Saffraanpoort", "Saffraanpoort", "Het Saffraanpoortse Pluizenblaadje", "The Saffraanpoort Fluff Gazette"),
}

# Per town: 6 silly news items (nl, en).
NIEUWS = {
    "zwamzoet": [
        ("Paddenstoel op het plein een centimeter gegroeid. Burgemeester: 'Wij zijn trots.'",
         "Mushroom on the square grew one centimetre. Mayor: 'We are proud.'"),
        ("Bakker Bram bakt per ongeluk een brood zo groot als een guh. Het brood weigert te vertrekken.",
         "Baker Bram accidentally bakes a loaf as big as a guh. The loaf refuses to leave."),
        ("Ordetempel meldt: de grote bel klinkt nu 3%% vrolijker.", "Order temple reports: the big bell now sounds 3%% happier."),
        ("Klodder gespot die netjes op zijn beurt wachtte bij de smid. Niemand weet wat hij wilde.",
         "Slime spotted politely waiting its turn at the smith. Nobody knows what it wanted."),
        ("Oma Truus wint voor de elfde keer de wedstrijd 'Zachtste Sjaal'. De jury moest even huilen.",
         "Oma Truus wins the 'Softest Scarf' contest for the eleventh time. The jury needed a little cry."),
        ("Lantaarnopsteker vraagt de lantaarns voortaan zelf aan te gaan. Lantaarns: geen commentaar.",
         "Lamplighter asks the lanterns to switch on by themselves from now on. Lanterns: no comment."),
    ],
    "haven": [
        ("Meeuw steelt een frietje, geeft het daarna beschaamd terug.", "Seagull steals a chip, then hands it back, ashamed."),
        ("Luchtschip landt zo zacht dat de passagiers het niet merkten en nog een rondje bleven zitten.",
         "Airship lands so softly the passengers didn't notice and stayed on for another lap."),
        ("Havenmeester telt de golfjes: 'Vandaag 4.812, gisteren waren het er meer.'",
         "Harbourmaster counts the little waves: 'Today 4,812, yesterday there were more.'"),
        ("Visje springt uit het water, zwaait, en springt er weer in. Getuigen zijn ontroerd.",
         "Little fish jumps out of the water, waves, and jumps back in. Witnesses are moved."),
        ("Nieuw op de kade: een bankje dat terug knuffelt. Wachtrij reikt tot het water.",
         "New on the quay: a bench that hugs back. The queue reaches the water."),
        ("Touw in de knoop. Touw wil er niet over praten.", "Rope in a knot. Rope doesn't want to talk about it."),
    ],
    "kandijkom": [
        ("Sneeuwguh gebouwd die beter zingt dan het koor. Koor overweegt sneeuw te worden.",
         "Snow guh built that sings better than the choir. Choir considers becoming snow."),
        ("Warme chocomelk nu met extra schuim. 'Het schuim is ook extra warm', bevestigt de herbergier.",
         "Hot chocolate now with extra foam. 'The foam is extra warm too', the innkeeper confirms."),
        ("IJspegel valt van een dak en landt precies in een beker. Applaus.",
         "Icicle falls off a roof and lands exactly in a mug. Applause."),
        ("Wanten-epidemie: overal losse wanten gevonden. Niemand mist er een.",
         "Mitten epidemic: single mittens found everywhere. Nobody is missing one."),
        ("Gondel maakt een extra rondje 'omdat het uitzicht zo mooi was'.",
         "Gondola does an extra lap 'because the view was so pretty'."),
        ("Kristalnaald in de bergen gaat 's nachts zachtjes neuriën. Wetenschappers neuriën mee.",
         "Crystal needle in the mountains hums softly at night. Scientists hum along."),
    ],
    "honingtak": [
        ("Bij landt op de neus van de burgemeester en blijft daar een uur. 'Het voelde als een eer.'",
         "Bee lands on the mayor's nose and stays for an hour. 'It felt like an honour.'"),
        ("Honingthee zo zoet dat de lepel bleef staan.", "Honey tea so sweet the spoon stood up by itself."),
        ("Tak kraakt een beetje. Tak zegt dat het niets is.", "Branch creaks a little. Branch says it's nothing."),
        ("Raatje telt 1.000.001 bijen. 'Die ene extra ken ik niet.'", "Raatje counts 1,000,001 bees. 'I don't know that one extra.'"),
        ("Bloesemblad valt in iemands thee. De thee is nu 'deftig'.", "Blossom petal falls into someone's tea. The tea is now 'posh'."),
        ("Hangnest wiegt zo lief dat drie avonturiers in slaap vielen tijdens een gesprek.",
         "Hanging nest sways so sweetly that three adventurers fell asleep mid-conversation."),
    ],
    "lampionhaven": [
        ("Lampion zweeft weg en komt een uur later terug met een vriendje.",
         "Lampion floats away and comes back an hour later with a friend."),
        ("Soep van de dag: vissoep. Soep van gisteren: ook vissoep. Morgen: verrassing (vissoep).",
         "Soup of the day: fish soup. Soup of yesterday: also fish soup. Tomorrow: surprise (fish soup)."),
        ("Krabbetje loopt voor het eerst rechtdoor. Hele strand juicht.", "Little crab walks straight for the first time. The whole beach cheers."),
        ("Zeilbootje vaart een rondje om de fontein. Niemand weet hoe het daar kwam.",
         "Little sailboat sails a lap around the fountain. Nobody knows how it got there."),
        ("Limonadekraam verkoopt limonade die naar zonsondergang smaakt.", "Lemonade stall sells lemonade that tastes like sunset."),
        ("Vuurtoren knipoogt naar de haven. Haven bloost.", "Lighthouse winks at the harbour. Harbour blushes."),
    ],
    "hoogvads": [
        ("Wolkje blijft hangen in de kathedraal en luistert de hele preek.",
         "Little cloud gets stuck in the cathedral and listens to the whole sermon."),
        ("Universiteit ontdekt: guhs worden 12%% pluiziger boven de wolken.",
         "University discovers: guhs get 12%% fluffier above the clouds."),
        ("Paleiswacht vergeet zijn wachtwoord. Het wachtwoord was 'pluis'.",
         "Palace guard forgets his password. The password was 'fluff'."),
        ("Marktlaan: bakker verkoopt broodjes die zachtjes zweven. 'Snel opeten.'",
         "Market lane: baker sells buns that gently float. 'Eat quickly.'"),
        ("Academie geeft les in 'deftig zwaaien'. Iedereen haalt een tien.", "Academy teaches 'posh waving'. Everybody gets top marks."),
        ("Staartpark: wolkwal zwaait met zijn staart. Halve stad krijgt een briesje.",
         "Tail park: cloud whale waves its tail. Half the city gets a breeze."),
    ],
    "rozenkloof": [
        ("Roos in de kloof bloeit zo hard dat ze ervan moet niezen.", "Rose in the gorge blooms so hard it has to sneeze."),
        ("Muurtje gevonden dat nog nooit iemand heeft beklommen. Muurtje is opgelucht.",
         "Little wall found that nobody has ever climbed. Little wall is relieved."),
        ("Rozenwater nu ook in de smaak 'extra roze'.", "Rose water now also in the flavour 'extra pink'."),
        ("Echo in de kloof zegt elke ochtend 'goedemorgen' terug. Zelfs als niemand iets zei.",
         "The echo in the gorge says 'good morning' back every day. Even when nobody said anything."),
        ("Kameel weigert de trap te nemen. Trap begrijpt het.", "Camel refuses to take the stairs. Stairs understand."),
        ("Plein krijgt nieuwe tegels in de vorm van hartjes. Oude tegels mogen blijven logeren.",
         "Square gets new tiles shaped like hearts. Old tiles may stay over."),
    ],
    "saffraanpoort": [
        ("Bazaar verkoopt een tapijt dat zelf terug naar de kraam vliegt. Geen retourbeleid nodig.",
         "Bazaar sells a carpet that flies back to the stall by itself. No return policy needed."),
        ("Kruidenkraam niest. Halve markt niest mee.", "Spice stall sneezes. Half the market sneezes along."),
        ("Paleis zoekt een nieuwe kussenschudder. Ervaring met pluis een pré.",
         "Palace seeks a new cushion fluffer. Experience with fluff a plus."),
        ("Zandkorrel gevonden in iemands schoen. Geen nieuws, maar toch.", "Grain of sand found in someone's shoe. Not news, but still."),
        ("Pottenbakker maakt een pot zo rond dat hij wegrolde naar de poort.",
         "Potter makes a pot so round it rolled away to the gate."),
        ("Karavaan komt aan met 300 kisten dadels en één verdwaalde guh. De guh mag blijven.",
         "Caravan arrives with 300 crates of dates and one lost guh. The guh may stay."),
    ],
}
NIEUWS["knuffelkamp"] = [
    ("Trainingspop zegt 'au' maar bedoelt 'dank je'.", "Training dummy says 'ouch' but means 'thank you'."),
    ("Kampvuur knettert een liedje. Kok zingt mee.", "Campfire crackles a song. Cook sings along."),
    ("Nieuwe avonturier gearriveerd. Iedereen zwaait. Avonturier zwaait terug. Iedereen zwaait nog harder.",
     "New adventurer arrived. Everybody waves. Adventurer waves back. Everybody waves harder."),
    ("Visser Dobber vangt een laars. De laars vangt een visje. Iedereen tevreden.",
     "Angler Dobber catches a boot. The boot caught a fish. Everybody happy."),
    ("Tent waait om, wordt weer opgezet, waait weer om. Tent heeft het naar zijn zin.",
     "Tent blows over, is put back up, blows over again. Tent is having fun."),
    ("Kampwacht ziet een klodder. Klodder ziet de kampwacht. Ze knikken naar elkaar.",
     "Camp guard sees a slime. Slime sees the camp guard. They nod at each other."),
]

# General items: %1$s = a citizen, %2$s = an object, %3$s = a number.
ALG = [
    ("%1$s heeft %2$s gevonden onder een kussen. Het kussen ontkent alles.",
     "%1$s found %2$s under a cushion. The cushion denies everything."),
    ("Enquête: %3$s van de 10 guhs vinden knuffelen 'best wel fijn'. De rest was aan het knuffelen.",
     "Survey: %3$s out of 10 guhs find hugging 'quite nice'. The rest were busy hugging."),
    ("%1$s organiseert een wedstrijd 'langste gaap'. Winnaar: %1$s (viel in slaap).",
     "%1$s organises a 'longest yawn' contest. Winner: %1$s (fell asleep)."),
    ("Gezocht: %2$s. Laatst gezien op de schoot van een klodder.", "Wanted: %2$s. Last seen on a slime's lap."),
    ("%1$s heeft %3$s keer 'njeg' gezegd vandaag. Record!", "%1$s said 'njeg' %3$s times today. A record!"),
    ("Wetenschap: een guh die gezwaaid wordt, zwaait %3$s procent vrolijker terug.",
     "Science: a guh that is waved at waves back %3$s percent happier."),
    ("%1$s ruilt %2$s voor een liedje. Iedereen vindt het een goede deal.",
     "%1$s trades %2$s for a song. Everybody thinks it's a good deal."),
    ("Mysterie: wie legt elke ochtend een bloemetje bij de deur van %1$s?",
     "Mystery: who leaves a little flower at %1$s's door every morning?"),
    ("Pieppiepmuisje gezien met %2$s. Het muisje had er duidelijk plannen mee.",
     "Squeaky mouse seen carrying %2$s. The mouse clearly had plans."),
    ("%1$s geeft gratis knuffels. Er zijn er nog %3$s over.", "%1$s gives free hugs. %3$s left."),
    ("Weetje van de dag: een klodder kan %3$s keer stuiteren voordat hij giechelt.",
     "Fact of the day: a slime can bounce %3$s times before it giggles."),
    ("%1$s heeft een nieuw kapsel. Niemand ziet het verschil, iedereen zegt 'mooi!'.",
     "%1$s has a new haircut. Nobody sees the difference, everybody says 'lovely!'."),
    ("Verloren en gevonden: %2$s. Af te halen bij %1$s, na een knuffel.",
     "Lost and found: %2$s. Collect at %1$s's, after a hug."),
    ("%1$s oefent 's nachts op de trompet. Buren: 'Het wordt al beter.'",
     "%1$s practises the trumpet at night. Neighbours: 'It's getting better.'"),
    ("Dringend: %1$s zoekt iemand die %2$s kan vasthouden terwijl %1$s niest.",
     "Urgent: %1$s needs someone to hold %2$s while %1$s sneezes."),
    ("Ingezonden brief: 'Mag het iets minder roze? Groetjes, niemand.'", "Letter to the editor: 'Could it be a bit less pink? Regards, nobody.'"),
]

DINGEN = [
    ("haar breinaald", "her knitting needle"), ("de reservesok van de kat", "the cat's spare sock"),
    ("een pot kaasknabbels", "a jar of cheese nibbles"), ("zijn leesbril", "his reading glasses"),
    ("een verdwaald knuffeltje", "a lost cuddly toy"), ("de sleutel van de schuur", "the shed key"),
    ("een sneeuwbolletje", "a little snow globe"), ("een halve taart", "half a cake"),
    ("het lievelingskussen", "the favourite cushion"), ("een fluitje", "a little whistle"),
    ("een bolletje wol", "a ball of wool"), ("een liefdesbrief (niet lezen!)", "a love letter (don't read it!)"),
    ("de kaasschaaf", "the cheese slicer"), ("een slaapmutsje", "a nightcap"),
    ("een glimmend knoopje", "a shiny button"), ("een potje honing", "a little jar of honey"),
    ("de theepot met het oortje", "the teapot with the little handle"), ("een pluizige pantoffel", "a fluffy slipper"),
    ("een schelp die ruist", "a shell that whooshes"), ("een vlieger zonder touw", "a kite without a string"),
    ("een receptenboekje", "a little recipe book"), ("een paraplu met gaatjes", "an umbrella with holes"),
    ("de dagboeksleutel", "the diary key"), ("een gelukssteentje", "a lucky pebble"),
]

KOEK = [
    ("Vandaag knuffelt iemand je terug. Misschien een bankje.", "Today someone hugs you back. Maybe a bench."),
    ("Je volgende loot is roze. Of niet. Maar wel lief.", "Your next loot is pink. Or not. But sweet."),
    ("Een klodder denkt aan je. Hij weet alleen niet meer waarom.", "A slime is thinking of you. It just forgot why."),
    ("Zwaai naar drie guhs en je dag wordt 3x zo zacht.", "Wave at three guhs and your day gets 3x softer."),
    ("Je gaat iets verliezen. Het is een kruimel. Van dit koekje.", "You will lose something. It's a crumb. Of this cookie."),
    ("Morgen regent het misschien. Neem een regenboog mee.", "Maybe it rains tomorrow. Bring a rainbow."),
    ("Je bent een held. Vooral voor dit koekje, dat je net opat.", "You are a hero. Especially to this cookie, which you just ate."),
    ("Kijk omhoog vannacht. De sterren zwaaien.", "Look up tonight. The stars are waving."),
]


def teksten():
    """key -> (nl, en)"""
    t = {}
    for sid, (nl, en, knl, ken) in STEDEN.items():
        t[P + "stad." + sid] = (nl, en)
        t[P + "krant.titel." + sid] = (knl, ken)
    for sid, regels in NIEUWS.items():
        for i, (nl, en) in enumerate(regels, 1):
            t[P + "krant.nieuws.%s.%d" % (sid, i)] = (nl, en)
    for i, (nl, en) in enumerate(ALG, 1):
        t[P + "krant.nieuws.alg.%d" % i] = (nl, en)
    for i, (nl, en) in enumerate(DINGEN, 1):
        t[P + "krant.ding.%d" % i] = (nl, en)
    for i, (nl, en) in enumerate(KOEK, 1):
        t[P + "markt.koek.spreuk.%d" % i] = (nl, en)
    t.update({
        # the night: closed shops, the bell, sleepers
        P + "dicht.bord": ("Gesloten, morgen weer open!", "Closed, open again tomorrow!"),
        P + "dicht.bel": ("✦ Klik voor de nachtbel ✦", "✦ Click for the night bell ✦"),
        P + "dicht.slaperig": ("%s doet slaperig open: 'Komt eraan... *gaap*'", "%s opens up sleepily: 'Coming... *yawn*'"),
        P + "dicht.niemand": ("De bel klinkt, maar %s slaapt heel diep. Probeer het morgen nog eens.",
                              "The bell rings, but %s is fast asleep. Try again tomorrow."),
        P + "dicht.gewekt": ("%s wrijft in de oogjes: 'Vooruit, omdat jij het bent...'", "%s rubs its eyes: 'Alright, because it's you...'"),
        P + "slaper.klik.1": ("Zzz... *snurk*... nog vijf minuutjes...", "Zzz... *snore*... five more minutes..."),
        P + "slaper.klik.2": ("De guh mompelt iets over kaasknabbels en slaapt verder.", "The guh mumbles something about cheese nibbles and sleeps on."),
        P + "slaper.klik.3": ("Sst! Hij droomt dat hij een wolkje is.", "Shh! It's dreaming that it's a little cloud."),
        # the night market
        P + "markt.bord": ("✦ Nachtmarkt ✦", "✦ Night market ✦"),
        P + "markt.naam.snoep": ("Lotje Lampion", "Lotje Lampion"),
        P + "markt.naam.koek": ("Opa Sterrenkoek", "Grandpa Starcookie"),
        P + "markt.snoep.hallo": ("Lampionsnoepjes! Gloeien in je buikje en je pootjes gaan er sneller van! Maar %s brons.",
                                  "Lampion sweets! They glow in your tummy and make your feet go faster! Only %s bronze."),
        P + "markt.koek.hallo": ("Een sterrenkoekje, jongen? Met een spreuk erin. Ze komen altijd uit. Bijna altijd. %s brons.",
                                 "A star cookie, dear? With a fortune inside. They always come true. Almost always. %s bronze."),
        P + "markt.snoep.koop": ("Eén lampionsnoepje, graag!", "One lampion sweet, please!"),
        P + "markt.koek.koop": ("Eén sterrenkoekje, graag!", "One star cookie, please!"),
        P + "markt.nee": ("Nee, dank je!", "No, thank you!"),
        P + "markt.te_weinig": ("Oei, daar heb je net niet genoeg munten voor.", "Oops, you don't quite have enough coins for that."),
        P + "markt.snoep.op": ("Mmm! Je pootjes tintelen: een minuut Snelpootjes!", "Mmm! Your feet tingle: a minute of Snelpootjes!"),
        P + "markt.koek.op": ("Knabbel... de spreuk zegt: \"%s\" Je voelt je ook wat beter.",
                              "Nibble... the fortune says: \"%s\" You feel a bit better too."),
        # knuffelmeter
        P + "knuffel.drempel": ("Knuffelmeter %1$s: %2$s guhs blij gemaakt! Als dank: %3$s brons.",
                                "Knuffelmeter %1$s: %2$s guhs made happy! As thanks: %3$s bronze."),
        P + "knuffel.titel": ("Nieuwe titel verdiend: %s!", "New title earned: %s!"),
        P + "knuffel.bril": ("%s guhs blij gemaakt! Er komt een hartjesbril voor je guh met de post (thuis af te halen).",
                             "%s guhs made happy! A pair of heart glasses for your guh is on its way by post (collect at home)."),
        P + "knuffel.karakter": ("Knuffelmeter: %s guhs blij gemaakt", "Knuffelmeter: %s guhs made happy"),
        P + "knuffel.karakter.steden": ("%s", "%s"),
        P + "knuffel.terug": ("%s knuffelt je terug!", "%s hugs you back!"),
        "gui.guhs.titels.naam.uritme_knuffelvriend": ("Knuffelvriend", "Hug Friend"),
        "gui.guhs.titels.hint.uritme_knuffelvriend": ("Maak in één stad van het Vadsrijk 25 guhs blij (zwaai, knuffel, dans!).",
                                                       "Make 25 guhs happy in one town of the Vadsrijk (wave, hug, dance!)."),
        "gui.guhs.titels.naam.uritme_grote_knuffelaar": ("Grote Knuffelaar", "Great Hugger"),
        "gui.guhs.titels.hint.uritme_grote_knuffelaar": ("Maak in de steden van het Vadsrijk samen 120 guhs blij.",
                                                          "Make 120 guhs happy across the towns of the Vadsrijk."),
        # chores
        P + "klus.genomen.zoek": ("Klusje aangenomen: zoek %2$s voor %1$s. Het ligt ergens bij %3$s (het glinstert!).",
                                  "Chore taken: find %2$s for %1$s. It's somewhere near %3$s (it sparkles!)."),
        P + "klus.genomen.bezorg": ("Klusje aangenomen: breng een pakketje van %1$s naar %3$s.", "Chore taken: bring a parcel from %1$s to %3$s."),
        P + "klus.genomen.haal": ("Klusje aangenomen: haal %2$s op bij %3$s en breng het naar %1$s.",
                                  "Chore taken: pick up %2$s at %3$s and bring it to %1$s."),
        P + "klus.gedaan": ("Klusje klaar! %1$s is heel blij. Je krijgt %2$s brons (en een knuffelpunt).",
                            "Chore done! %1$s is very happy. You get %2$s bronze (and a hug point)."),
        P + "klus.bezorgd": ("Een pakketje van %1$s? Voor mij? Oh, %2$s! Wat lief!", "A parcel from %1$s? For me? Oh, %2$s! How sweet!"),
        P + "klus.opgehaald": ("Ah, je komt %1$s halen voor %2$s? Alsjeblieft, en doe de groetjes!",
                               "Ah, you've come to fetch %1$s for %2$s? Here you go, and say hi!"),
        P + "klus.dankjewel.zoek": ("Je hebt %s gevonden! Waar lag het? ...Nee, zeg het maar niet.",
                                    "You found %s! Where was it? ...No, don't tell me."),
        P + "klus.dankjewel.bezorg": ("Is %s goed aangekomen? Fijn!", "Did %s arrive safely? Lovely!"),
        P + "klus.dankjewel.haal": ("Daar is %s weer! Dank je wel, lieverd.", "There's %s again! Thank you, dear."),
        P + "klus.gevonden": ("Gevonden: %1$s! Breng het terug naar %2$s (of lever het in bij het Pluizenblaadje).",
                              "Found: %1$s! Bring it back to %2$s (or hand it in at the Fluff Gazette)."),
        P + "klus.warm.warm": ("Warm... het glinstert hier ergens!", "Warm... it's sparkling somewhere here!"),
        P + "klus.warm.heet": ("Heel warm!", "Very warm!"),
        # the newspaper
        P + "krant.bord.sub": ("Nieuws & klusjes (klik!)", "News & chores (click!)"),
        P + "krant.kop": ("%1$s, dag %2$s. Vers van de pers en nog een beetje warm!", "%1$s, day %2$s. Hot off the press, still a bit warm!"),
        P + "krant.iemand": ("Iemand", "Someone"),
        P + "krant.gisteren.knuffel": ("Knuffelkampioen van gisteren: %1$s, met %2$s blije guhs! Hoera!",
                                       "Yesterday's hug champion: %1$s, with %2$s happy guhs! Hooray!"),
        P + "krant.gisteren.klodders": ("Gisteren werden er in de buurt %s klodders opgevrolijkt. Goed bezig, avonturiers!",
                                        "Yesterday %s slimes were cheered up nearby. Well done, adventurers!"),
        P + "krant.gisteren.rustig.1": ("Gisteren gebeurde er niets. Het was heerlijk.", "Nothing happened yesterday. It was lovely."),
        P + "krant.gisteren.rustig.2": ("Rustige dag gisteren. Zelfs de klodders deden een dutje.", "Quiet day yesterday. Even the slimes had a nap."),
        P + "krant.gisteren.rustig.3": ("Gisteren: twee wolkjes, één regenboogje, nul problemen.", "Yesterday: two clouds, one little rainbow, zero problems."),
        P + "krant.gisteren.rustig.4": ("De redactie had gisteren vrij. Dit stukje is dus van eergisteren.",
                                        "The editors had yesterday off. So this bit is from the day before."),
        P + "krant.weer.droog": ("Weerbericht: vandaag droog en zonnig. Perfect knuffelweer.", "Weather: dry and sunny today. Perfect hugging weather."),
        P + "krant.weer.droog.mist": ("Weerbericht: droog. Morgenvroeg een zacht ochtendmistje.", "Weather: dry. A soft morning mist early tomorrow."),
        P + "krant.knuffel": ("Jouw knuffelmeter in %1$s: %2$s blije guhs. Volgende beloning bij %3$s!",
                              "Your knuffelmeter in %1$s: %2$s happy guhs. Next reward at %3$s!"),
        P + "krant.knuffel.vol": ("Jouw knuffelmeter in %1$s: %2$s blije guhs. Alle beloningen binnen, knuffelheld!",
                                  "Your knuffelmeter in %1$s: %2$s happy guhs. All rewards collected, hug hero!"),
        P + "krant.geen_klusjes": ("Klusjes: vandaag even geen. Iedereen heeft alles al gevonden!", "Chores: none today. Everybody already found everything!"),
        P + "krant.klus.zoek": ("Klusje %1$s: %2$s zoekt %3$s! Het ligt ergens bij %4$s. Beloning: %5$s brons. [%6$s]",
                                "Chore %1$s: %2$s is looking for %3$s! It's somewhere near %4$s. Reward: %5$s bronze. [%6$s]"),
        P + "krant.klus.bezorg": ("Klusje %1$s: %2$s wil een pakketje laten bezorgen bij %4$s. Beloning: %5$s brons. [%6$s]",
                                  "Chore %1$s: %2$s wants a parcel delivered to %4$s. Reward: %5$s bronze. [%6$s]"),
        P + "krant.klus.haal": ("Klusje %1$s: %2$s heeft %3$s uitgeleend aan %4$s. Wil jij het even ophalen? Beloning: %5$s brons. [%6$s]",
                                "Chore %1$s: %2$s lent %3$s to %4$s. Could you fetch it? Reward: %5$s bronze. [%6$s]"),
        P + "krant.staat.0": ("nog te doen", "to do"),
        P + "krant.staat.1": ("bezig", "in progress"),
        P + "krant.staat.2": ("klaar om in te leveren!", "ready to hand in!"),
        P + "krant.staat.3": ("gedaan ✔", "done ✔"),
        P + "krant.optie.neem.1": ("Klusje 1 aannemen", "Take chore 1"),
        P + "krant.optie.neem.2": ("Klusje 2 aannemen", "Take chore 2"),
        P + "krant.optie.lever.1": ("Klusje 1 inleveren", "Hand in chore 1"),
        P + "krant.optie.lever.2": ("Klusje 2 inleveren", "Hand in chore 2"),
        P + "krant.optie.sluit": ("Krant dichtvouwen", "Fold up the paper"),
        # dev
        P + "dev.weer": ("Weer '%1$s' getoond (zone %2$s).", "Weather '%1$s' shown (zone %2$s)."),
        P + "dev.geen_stad": ("Die stad ken ik niet: %s", "Unknown town: %s"),
        P + "dev.nacht": ("Stadsritme: NACHT (geforceerd). Winkels dicht, slapers, uilen, nachtmarkt, vuurvliegjes.",
                          "Town rhythm: NIGHT (forced). Shops closed, sleepers, owls, night market, fireflies."),
        P + "dev.dag": ("Stadsritme: DAG (geforceerd). Iedereen wakker.", "Town rhythm: DAY (forced). Everybody awake."),
        P + "dev.auto": ("Stadsritme: volgt de klok weer.", "Town rhythm: follows the clock again."),
        P + "dev.geen": ("geen stad", "no town"),
        P + "dev.is_nacht": ("nacht", "night"),
        P + "dev.is_dag": ("dag", "day"),
        P + "dev.status": ("Stad: %1$s · %2$s (tijd %3$s, forceer %4$s) · zone %5$s · winkeliers in bed %6$s · nachtbellen %7$s · bedden bekend %8$s · slapers %9$s · uilen %10$s · nachtmarkten %11$s · prikborden %12$s",
                           "Town: %1$s · %2$s (time %3$s, forced %4$s) · zone %5$s · shopkeepers in bed %6$s · night bells %7$s · beds known %8$s · sleepers %9$s · owls %10$s · night markets %11$s · notice boards %12$s"),
        P + "dev.winkel.sluit": ("  %1$s (%2$s): gaat 's nachts naar bed (bed: %3$s)", "  %1$s (%2$s): goes to bed at night (bed: %3$s)"),
        P + "dev.winkel.quest": ("  %1$s (%2$s): blijft 24/7 (zit in een quest)", "  %1$s (%2$s): stays 24/7 (in a quest)"),
        P + "dev.winkel.blijft": ("  %1$s (%2$s): blijft 24/7 (reis/extra keuzes/geen bel-actie)", "  %1$s (%2$s): stays 24/7 (travel/extra choices/no bell action)"),
        P + "dev.winkels": ("%s winkeliers.", "%s shopkeepers."),
        P + "dev.geen_markt": ("Hier is geen nachtmarkt (Zwamzoet, Kandijkom, Lampionhaven, Saffraanpoort).",
                               "No night market here (Zwamzoet, Kandijkom, Lampionhaven, Saffraanpoort)."),
        P + "dev.markt": ("Nachtmarkt van %1$s staat er (%2$s). Stadsritme staat op nacht.", "Night market of %1$s is up (%2$s). Town rhythm set to night."),
        P + "dev.markt_mislukt": ("Nachtmarkt van %1$s lukte niet (geen vrije plek bij %2$s?).", "Night market of %1$s failed (no free spot near %2$s?)."),
        P + "dev.knuffel": ("Knuffelmeter: %s in totaal", "Knuffelmeter: %s in total"),
        P + "dev.geen_krant": ("Hier is geen Pluizenblaadje (ga in een stad staan).", "No Fluff Gazette here (stand in a town)."),
        # settings
        "vadsrijk.menu.instellingen.weer": ("Weer met charme", "Weather with charm"),
        "vadsrijk.menu.instellingen.weer.uitleg": ("Buitjes met regenboog, sneeuw, bloesemregen, lampiongloed, wolkenslierten, zandwervels en ochtendmist. 0 = uit.",
                                                   "Showers with rainbows, snow, blossom rain, lampion glow, cloud wisps, sand swirls and morning mist. 0 = off."),
    })
    # 1.10.1 (p1101-tijd): /guhs vadsrijk tijd (feature/vadsrijk/tijd); English in tools/lang/en/c470_vr_tijd.json
    t.update({
        "vadsrijk.tijd.status": ("Vadsrijk-tijd: cyclus %1$s min · dag %2$s min · avond + nacht + ochtend %3$s min (dag_deel %4$s)",
                                 "Chonkdom time: cycle %1$s min · day %2$s min · dusk + night + dawn %3$s min (dag_deel %4$s)"),
        "vadsrijk.tijd.status.nu": ("Nu %1$s in het Vadsrijk (%2$s, dag %3$s, kloktijd %4$s) · tempo %5$s · overworld %6$s",
                                    "Now %1$s in the Chonkdom (%2$s, day %3$s, clock time %4$s) · speed %5$s · overworld %6$s"),
        "vadsrijk.tijd.status.config": ("Instellingen: config/%s (of /guhs vadsrijk tijd cyclus|dag)",
                                        "Settings: config/%s (or /guhs vadsrijk tijd cyclus|dag)"),
        "vadsrijk.tijd.is_dag": ("dag", "day"),
        "vadsrijk.tijd.is_nacht": ("nacht", "night"),
        "vadsrijk.tijd.cyclus.gezet": ("De Vadsrijk-cyclus duurt nu %1$s minuten (dag %2$s min, avond + nacht + ochtend %3$s min).",
                                       "The Chonkdom cycle now lasts %1$s minutes (day %2$s min, dusk + night + dawn %3$s min)."),
        "vadsrijk.tijd.dag.gezet": ("De dag is nu %1$s van de Vadsrijk-cyclus (dag %2$s min, avond + nacht + ochtend %3$s min).",
                                    "Day is now %1$s of the Chonkdom cycle (day %2$s min, dusk + night + dawn %3$s min)."),
        "vadsrijk.tijd.zet.dag": ("Goedemorgen, Vadsrijk! Het is nu dag.", "Good morning, Chonkdom! It is day now."),
        "vadsrijk.tijd.zet.nacht": ("Welterusten, Vadsrijk! Het is nu nacht.", "Good night, Chonkdom! It is night now."),
        "vadsrijk.tijd.herlaad": ("config/%s opnieuw gelezen.", "Read config/%s again."),
        "vadsrijk.tijd.geen_klok": ("Het Vadsrijk heeft geen eigen klok (mist data/guhs/world_clock/vadsrijk.json?).",
                                    "The Chonkdom has no clock of its own (data/guhs/world_clock/vadsrijk.json missing?)."),
    })
    weer = {
        "roze": ("een roze buitje", "a pink shower"),
        "sneeuw": ("een sneeuwbuitje", "a snow shower"),
        "bloesem": ("bloesemregen", "blossom rain"),
        "regen": ("een lief regenbuitje", "a sweet rain shower"),
        "wolken": ("een optocht van wolkenslierten", "a parade of cloud wisps"),
        "zand": ("warme zandwervels", "warm sand swirls"),
    }
    for s, (nl, en) in weer.items():
        boog_nl = ", daarna misschien een regenboog!" if s in ("roze", "sneeuw", "bloesem", "regen") else "."
        boog_en = ", then maybe a rainbow!" if s in ("roze", "sneeuw", "bloesem", "regen") else "."
        t[P + "krant.weer." + s] = ("Weerbericht: vandaag rond %s " + nl + boog_nl, "Weather: around %s today " + en + boog_en)
        t[P + "krant.weer." + s + ".mist"] = ("Weerbericht: vandaag rond %s " + nl + boog_nl + " Morgenvroeg ochtendmist.",
                                              "Weather: around %s today " + en + boog_en + " Morning mist early tomorrow.")
    return t


def build(h):
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for k, v in t.items():
            data[k] = v[i]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    _direct()
