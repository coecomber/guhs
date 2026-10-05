"""
De guhbibliotheek (2.4): a very rare, grand library in the Guhmension with the Bibliothecaris behind the lending desk.

  - 13 guh lore books (vanilla written books with translated pages: book.guhs.bieb.<id>.<page>): 11 lie open on the
    lecterns of the reading room, the secret one on the book stand in the secret room behind the bookshelf door, and
    (2.7) the diary of the Voorraadmika in the Stille Voorraadkelder (ELSEWHERE). Read them
    all right away; take one copy of each home, once per player. A lore quiz (Guhkwis) for boekenbonnen and a shop
    (spare copies, the scholar's outfit - only sold here - and the guh reading armchair)
  - blocks: bieb_geheime_kast (a bookshelf that swings open), bieb_boekaltaar (the stand with the secret book),
    bieb_guhfauteuil (a guh-shaped armchair); item: boekenbon
  - the structure guhbibliotheek (88 x 40 x 76): three floors around an atrium under a glass dome with guh ears, a
    stained-glass guh face window, a globe of the Guhmension, a guh face floor mosaic, balconies, reading nooks and the
    secret room; plus a geometry self-check (check_structure)

The Java side (nl.juiced.guhs.feature.bibliotheek.Guhboek) must list the same books with the same page counts: build()
checks that.
"""
import json
import math
import os
import random
import re

import make_structures as ms

# =====================================================================================================================
# the books: id -> (cover colour, English title, Dutch title, [(en, nl) pages], [quiz questions])
# a quiz question: (en, nl, [(en, nl) answers]); the FIRST answer is the right one (the Bibliothecaris shuffles them)
# =====================================================================================================================
BOOKS = [
    ("eerste_guh", (240, 140, 180), "The Very First Guh", "De allereerste guh", [
        ("Long, long ago there was nothing at all. Just a soft pink nothing, as soft as a pillow. And right in the middle of that nothing lay one single crumb. A cheese snack crumb.",
         "Lang, lang geleden was er nog niks. Alleen een zacht roze niks, zo zacht als een kussen. En midden in dat niks lag één kruimel. Een kaasknabbelkruimel."),
        ("The crumb lay there for a very long time. It got warm, it got round, and it got... chubby. One day it opened two big blue eyes and said the very first word ever: 'Guh.'",
         "De kruimel lag daar heel lang. Hij werd warm, hij werd rond en hij werd... vadsig. Op een dag deed hij twee grote blauwe ogen open en zei het allereerste woord ooit: 'Guh.'"),
        ("And so the first guh was born. His name was Oerguh. Oerguh was alone, so he did what every guh does when it feels lonely: he ate a kaasknabbel. And another. And another.",
         "Zo werd de eerste guh geboren. Hij heette Oerguh. Oerguh was alleen, dus deed hij wat elke guh doet als hij zich alleen voelt: hij at een kaasknabbel. En nog een. En nog een."),
        ("From every kaasknabbel Oerguh shared with the pink nothing, a new guh grew. That is why guhs still say: whoever shares, gets friends. Guh!",
         "Uit elke kaasknabbel die Oerguh deelde met het roze niks, groeide een nieuwe guh. Daarom zeggen guhs nog steeds: wie deelt, krijgt vriendjes. Guh!"),
    ], [
        ("Where did the very first guh come from?", "Waar kwam de allereerste guh uit?",
         [("A cheese snack crumb", "Een kaasknabbelkruimel"), ("A pink egg", "Een roze ei"), ("A guh cake", "Een guhtaart")]),
        ("What was the name of the very first guh?", "Hoe heette de allereerste guh?",
         [("Oerguh", "Oerguh"), ("Guhbert", "Guhbert"), ("Big Guh", "Grote Guh")]),
    ]),
    ("moeder_vadsig", (214, 90, 140), "How Mother Vadsig Got So Vadsig", "Hoe Moeder Vadsig zo vadsig werd", [
        ("Mother Vadsig used to be a perfectly ordinary guh. A bit round, sure. But not THAT round. Not shrine-round. How did that happen?",
         "Moeder Vadsig was vroeger een heel gewone guh. Een beetje rond, dat wel. Maar niet ZO rond. Niet heiligdom-rond. Hoe kan dat nou?"),
        ("It started on a rainy Tuesday. Mother Vadsig baked a guh cake for every little guh in the Guhmension. That is a LOT of guhs. And of course she had to taste every cake first.",
         "Het begon op een regenachtige dinsdag. Moeder Vadsig bakte een guhtaart voor elk guhtje in de Guhmensie. Dat zijn er heel veel. En elke taart moest ze natuurlijk eerst proeven."),
        ("One bite per cake. But there were a thousand cakes. After cake five hundred she no longer fit through the door. After cake one thousand she no longer fit through the village. VADS!",
         "Eén hapje per taart. Maar er waren duizend taarten. Na taart vijfhonderd paste ze niet meer door de deur. Na taart duizend paste ze niet meer door het dorp. VADS!"),
        ("So the guhs built a shrine around her, so she wouldn't have to walk anymore. Ever since, she sits there, sweet and enormous, looking after every little guh. Especially Guhbert.",
         "Toen bouwden de guhs een heiligdom om haar heen, zodat ze niet meer hoefde te lopen. Sindsdien zit ze daar, lief en enorm, en past ze op alle guhtjes. Vooral op Guhbert."),
        ("Her belly is so big that a whole guh stomach fits inside, with a Dentist Guh and a Stomach Enzyme Guh. Help her, and she gives you a belly whistle. Njeg!",
         "Haar buik is zo groot dat er een hele guhmaag in past, met een Tandarts-guh en een Maagenzym-guh. Wie haar helpt, krijgt een buikfluitje. Njeg!"),
    ], [
        ("Why did Mother Vadsig get so vadsig?", "Waarom werd Moeder Vadsig zo vadsig?",
         [("She tasted a thousand guh cakes", "Ze proefde duizend guhtaarten"), ("She ate a Mika", "Ze at een Mika op"),
          ("She slept for a hundred years", "Ze sliep honderd jaar")]),
        ("What do you get when you help Mother Vadsig?", "Wat krijg je als je Moeder Vadsig helpt?",
         [("A belly whistle", "Een buikfluitje"), ("A crown", "Een kroon"), ("A sled", "Een slee")]),
    ]),
    ("kaasknabbels", (250, 205, 70), "The Secret of the Kaasknabbel", "Het geheim van de kaasknabbel", [
        ("Why are kaasknabbels so tasty? Learned guhs did a hundred years of research. They ate an awful lot of kaasknabbels doing it. For science, of course.",
         "Waarom zijn kaasknabbels zo lekker? Geleerde guhs hebben er honderd jaar onderzoek naar gedaan. Ze aten daarbij heel veel kaasknabbels. Voor de wetenschap, natuurlijk."),
        ("Their conclusion: a kaasknabbel is a little bit of cheese, a little bit of crunch and a whole lot of love. Give a guh a knabbel, and you give it a little piece of love.",
         "Hun conclusie: een kaasknabbel is een klein beetje kaas, een klein beetje knabbel en heel veel liefde. Wie een guh een knabbel geeft, geeft hem een stukje liefde."),
        ("That is why a guh becomes your friend when you feed it knabbels. And why a fried kaasknabbel (with Mika fat!) heals a guh right away. Extra love. Extra fat.",
         "Daarom word je vrienden met een guh als je hem knabbels voert. En daarom geneest een gefrituurde kaasknabbel (met Mika's vet!) een guh meteen helemaal. Extra liefde. Extra vet."),
        ("Nine kaasknabbels together make a block. And a frame of kaasknabbel blocks makes a gate to the Guhmension. Nobody knows why. It smells delicious in there, anyway.",
         "Negen kaasknabbels samen maken een blok. En een lijst van blokken kaasknabbels maakt een poort naar de Guhmensie. Niemand weet waarom. Het ruikt er in elk geval heerlijk."),
    ], [
        ("According to the learned guhs, what is inside a kaasknabbel?", "Wat zit er volgens de geleerde guhs in een kaasknabbel?",
         [("Cheese, crunch and a lot of love", "Kaas, knabbel en heel veel liefde"), ("Only cheese", "Alleen kaas"),
          ("Mika fat and sugar", "Mika's vet en suiker")]),
        ("How many kaasknabbels go into one block?", "Hoeveel kaasknabbels gaan er in één blok?",
         [("Nine", "Negen"), ("Four", "Vier"), ("Sixty-two", "Tweeënzestig")]),
    ]),
    ("guhmensie", (238, 141, 173), "A Travel Guide to the Guhmension", "Reisgids voor de Guhmensie", [
        ("Welcome to the Guhmension, the pink land of the guhs! Grab your compass and your knabbels: we are going on a trip. Tip: bring a Guhmension super compass, and you will never get lost.",
         "Welkom in de Guhmensie, het roze land van de guhs! Pak je kompas en je knabbels: we gaan op reis. Tip: neem een Guhmensie-superkompas mee, dan verdwaal je nooit."),
        ("The Guh Fields and the Guh Meadows: soft, pink and full of guhs. The Cheese Flats: yellow and a little bit smelly. The Pink Puffs: so soft you want to sleep in them.",
         "De Guhvelden en de Guhweides: zacht, roze en vol guhs. De Kaasvlakte: geel en een beetje stinkerig. De Roze Pluisjes: zo zacht dat je erin wilt slapen."),
        ("The Guh Peaks: cold and high. The Ender guhs used to fly there, but they moved to the Guheinde. The Vads Cliffs: steep, so mind your belly. The Guh Sea: blue and full of guh fish. Underground: shiny guh crystals.",
         "De Guhpieken: koud en hoog. Vroeger vlogen daar de Enderguhs, maar die zijn naar het Guheinde verhuisd. De Vadskliffen: steil, dus pas op je buikje. De Guhzee: blauw en vol guhvisjes. Onder de grond: glimmende guhkristallen."),
        ("And then there is Mika's biome. Dark, scary and full of Mikas. Only go there with a good sword and an empty stomach. Or a full one. Mikas don't care. Njeg.",
         "En dan is er Mika's bioom. Donker, eng en vol Mika's. Daar ga je alleen heen met een goed zwaard en een lege maag. Of een volle. Dat maakt Mika's niks uit. Njeg."),
    ], [
        ("Where did the Ender guhs use to fly?", "Waar vlogen de Enderguhs vroeger?",
         [("On the Guh Peaks", "Op de Guhpieken"), ("Over the Guh Sea", "Boven de Guhzee"), ("On the Cheese Flats", "Op de Kaasvlakte")]),
        ("Which biome is dark and scary?", "Welk bioom is donker en eng?",
         [("Mika's biome", "Mika's bioom"), ("The Pink Puffs", "De Roze Pluisjes"), ("The Guh Meadows", "De Guhweides")]),
    ]),
    ("mika_oorlog", (70, 45, 70), "The Great Mika War", "De Grote Mika-oorlog", [
        ("Not everyone in the Guhmension is sweet. There are Mikas too: dark, grumpy creatures that say NJEG in a very angry way. They steal knabbels and kidnap little guhs.",
         "Niet iedereen in de Guhmensie is lief. Er zijn ook Mika's: donkere, chagrijnige wezens die NJEG zeggen op een heel boze manier. Ze stelen knabbels en ontvoeren guhtjes."),
        ("Long ago the Mikas challenged the guhs to a war. But guhs don't like fighting. They are far too vadsig for that. So they suggested: rock-paper-scissors!",
         "Lang geleden daagden de Mika's de guhs uit voor een oorlog. Maar guhs houden niet van vechten. Daar zijn ze veel te vadsig voor. Dus stelden ze voor: steen-papier-schaar!"),
        ("The Mikas kept winning. Because the Mika boss cheated: he knew the secret VADS gesture, which beats everything. Until one clever guh copied the gesture and did it right back.",
         "De Mika's wonnen steeds. Want de Mika-baas speelde vals: hij kende het geheime VADS-gebaar, dat alles verslaat. Tot een slimme guh het gebaar afkeek en het terugdeed."),
        ("'I HAVE BEEN VADSED!' cried the Mika boss, and the war was over. Since then the Mikas live in their own camp. Sometimes they still grab a little guh. Then you know what to do. VADS!",
         "'IK BEN GEVADST!' riep de Mika-baas, en de oorlog was voorbij. Sindsdien wonen de Mika's in hun eigen kamp. Soms pakken ze nog een guhtje. Dan weet je wat je moet doen. VADS!"),
    ], [
        ("How was the Great Mika War decided?", "Hoe werd de Grote Mika-oorlog beslist?",
         [("With rock-paper-scissors", "Met steen-papier-schaar"), ("With a pillow fight", "Met een kussengevecht"),
          ("With an eating contest", "Met een eetwedstrijd")]),
        ("Which gesture beats everything?", "Welk gebaar verslaat alles?",
         [("The VADS gesture", "Het VADS-gebaar"), ("The double scissors", "De dubbele schaar"), ("The guh ear", "Het guhoortje")]),
    ]),
    ("koningskroon", (120, 40, 160), "The Crown of the Koningguh", "De kroon van de Koningguh", [
        ("In a castle very far away lives the Koningguh. Purple fur, a white mane, a posh little moustache. He sits on a golden throne and rules over all guhs. A little bit.",
         "In een kasteel heel ver weg woont de Koningguh. Paarse vacht, witte manen, een deftig snorretje. Hij zit op een gouden troon en regeert over alle guhs. Een beetje."),
        ("His crown is the Vahoege Guh King's Crown. It is made of the very first gold of the Guhmension, found by the Golden Guh himself, and polished by a thousand little guh teeth.",
         "Zijn kroon heet de Vahoege guhkoningskroon. Hij is gemaakt van het allereerste goud uit de Guhmensie, gevonden door de Gouden Guh zelf, en gepoetst door duizend guhtandjes."),
        ("Whoever wears the crown must promise three things: cuddle every guh, never take the last knabbel, and take a nap every afternoon. The King usually manages two.",
         "Wie de kroon draagt, moet drie dingen beloven: alle guhs knuffelen, nooit de laatste knabbel pakken en elke middag een dutje doen. De Koning lukt er meestal twee."),
        ("The gate guards only let true friends of the guhs in. How do you prove it? By sitting very vadsig, or by saying the secret guh word. It starts with an N...",
         "De poortwachters laten alleen echte guhvrienden binnen. Hoe bewijs je dat? Door heel vadsig te zitten, of door het geheime guhwoord te zeggen. Het begint met een N..."),
    ], [
        ("What must you promise when you wear the crown?", "Wat moet je beloven als je de kroon draagt?",
         [("Never take the last knabbel", "Nooit de laatste knabbel pakken"), ("Never sleep again", "Nooit meer slapen"),
          ("Cuddle a Mika every day", "Elke dag een Mika knuffelen")]),
        ("With which letter does the secret guh word start?", "Met welke letter begint het geheime guhwoord?",
         [("N", "N"), ("V", "V"), ("G", "G")]),
    ]),
    ("vahoeg", (255, 120, 190), "VAHOEG! The Guh Dictionary", "VAHOEG! Het guhwoordenboek", [
        ("GUH: hello, goodbye, yes, yummy, and basically everything.\n\nNJEG: I am happy! (Or very angry, when a Mika says it.) So listen carefully to who is saying it.",
         "GUH: hallo, dag, ja, lekker, en eigenlijk alles.\n\nNJEG: ik ben blij! (Of heel boos, als een Mika het zegt.) Let dus goed op wie het zegt."),
        ("VADS: lazy and round. A guh that vadses lies on the couch with a knabbel.\n\nVADSIG: chubby, plump, cuddly. The nicest compliment you can give a guh.",
         "VADS: lekker lui en rond. Een guh die vadst, ligt op de bank met een knabbel.\n\nVADSIG: mollig, dik, knuffelbaar. Het mooiste compliment dat je een guh kunt geven."),
        ("VAHOEG: the happiest word there is. You shout it when you do something amazing: a coaster ride, finding a Golden Guh, or eating a whole bag of knabbels in one go.",
         "VAHOEG: het allerblijste woord. Je roept het als je iets geweldigs doet: een kermisrit, een Gouden Guh vinden, of een hele zak knabbels in één keer opeten."),
        ("VAHOEGE VADS: the strongest metal of the Guhmension. Very happy and very chubby at the same time. Tools made of it never break. Just like the love of a guh. Aww.",
         "VAHOEGE VADS: het sterkste metaal van de Guhmensie. Heel blij en heel vadsig tegelijk. Gereedschap daarvan gaat nooit kapot. Net als de liefde van een guh. Aww."),
    ], [
        ("What does 'vadsig' mean?", "Wat betekent 'vadsig'?",
         [("Chubby and cuddly", "Mollig en knuffelbaar"), ("Fast and strong", "Snel en sterk"), ("Angry and grumpy", "Boos en chagrijnig")]),
        ("What do you shout when you do something amazing?", "Wat roep je als je iets geweldigs doet?",
         [("VAHOEG!", "VAHOEG!"), ("NJEG!", "NJEG!"), ("BLUB!", "BLUB!")]),
    ]),
    ("roze_maan", (190, 150, 230), "Why the Moon Is Pink", "Waarom de maan roze is", [
        ("When you look up at night in the Guhmension, you see a pink moon and pink stars. But once, the moon there was plain white. Boring white. How did that change?",
         "Als je 's nachts in de Guhmensie omhoogkijkt, zie je een roze maan en roze sterren. Maar vroeger was de maan daar gewoon wit. Saai wit. Hoe kwam dat zo?"),
        ("A little guh called Guhmmie couldn't sleep. The moon was far too bright. So she climbed the highest Guh Peak and blew an enormous pink bubble gum bubble. It stuck to the moon.",
         "Een klein guhtje, Guhmmie, kon niet slapen. De maan was veel te fel. Dus klom ze op de hoogste Guhpiek en blies een enorme roze kauwgombel. Die bleef aan de maan plakken."),
        ("Since then the moon is pink and shines softly, like a night light. The stars? Those are the crumbs of the knabbels Guhmmie ate afterwards. She never came down again...",
         "Sindsdien is de maan roze en schijnt hij zacht, als een nachtlampje. De sterren? Dat zijn de kruimels van de knabbels die Guhmmie daarna opat. Ze is nooit meer naar beneden gekomen..."),
        ("...just kidding. She slept very well after that. Watch the sky over the Guhmension for a whole night. Maybe you'll see a tiny pink dot waving at you.",
         "...grapje. Ze sliep daarna heerlijk. Kijk maar eens een hele nacht naar de lucht in de Guhmensie. Misschien zie je een klein roze stipje naar je zwaaien."),
    ], [
        ("Why is the moon in the Guhmension pink?", "Waarom is de maan in de Guhmensie roze?",
         [("A bubble gum bubble is stuck to it", "Er plakt een kauwgombel aan"), ("It is made of cheese", "Hij is van kaas"),
          ("The Mikas painted it", "De Mika's hebben hem geverfd")]),
        ("What are the pink stars?", "Wat zijn de roze sterren?",
         [("Knabbel crumbs", "Knabbelkruimels"), ("Guh eyes", "Guhoogjes"), ("Fireflies", "Vuurvliegjes")]),
    ]),
    ("gouden_guh", (245, 196, 60), "The Golden Guh and the Guhramid", "De Gouden Guh en de Guhramide", [
        ("Of all the guhs there is one that shines like the sun: the Golden Guh. Whoever sees him may make a wish. Most guhs wish for knabbels. Very predictable.",
         "Van alle guhs is er één die glimt als de zon: de Gouden Guh. Wie hem ziet, mag een wens doen. De meeste guhs wensen knabbels. Heel voorspelbaar."),
        ("The Golden Guh loves hiding. Deep inside the pink Guhramid he has a secret room, where he lies vadsing among the treasures. Sometimes he lets a treasure hunter in.",
         "De Gouden Guh houdt van verstoppen. Diep in de roze Guhramide heeft hij een geheime kamer, waar hij tussen de schatten ligt te vadsen. Soms laat hij een schatzoeker binnen."),
        ("He sometimes plays hide-and-seek with Verstopguhtje, but then he always wins: you can't see him anywhere, and yet he glitters. Nobody understands how he does it.",
         "Soms speelt hij verstopguh met Verstopguhtje, maar dan wint hij altijd: je ziet hem nergens, en toch glimt hij. Niemand snapt hoe hij dat doet."),
        ("Will you ever find him? Then very carefully bring him to the Dentist Guh. He knows exactly what to do with that much shiny vadsigness. Open wide!",
         "Vind je hem ooit? Neem hem dan heel voorzichtig mee naar de Tandarts-guh. Die weet precies wat je met zoveel glimmende vadsigheid moet doen. Wijd open!"),
    ], [
        ("Where does the Golden Guh hide?", "Waar verstopt de Gouden Guh zich?",
         [("In the Guhramid", "In de Guhramide"), ("In the Guh Sea", "In de Guhzee"), ("In Mika's biome", "In Mika's bioom")]),
        ("Who do you bring the Golden Guh to?", "Naar wie breng je de Gouden Guh?",
         [("The Dentist Guh", "De Tandarts-guh"), ("The Kermis Guh", "De Kermis-guh"), ("The Sled Guh", "De Slee-guh")]),
    ]),
    ("guhmaag", (230, 110, 120), "A Journey Through the Guh Stomach", "Een reis door de guhmaag", [
        ("Did you know you can live inside a guh? Well, inside a guh stomach. Mother Vadsig gives you a belly whistle, and toot: you are in a soft pink room with a tongue for a rug.",
         "Wist je dat je in een guh kunt wonen? Nou ja, in een guhmaag. Moeder Vadsig geeft je een buikfluitje, en tuut: je staat in een zachte roze kamer met een tong als vloerkleed."),
        ("In every stomach lives a Stomach Enzyme Guh. It only listens to the owner of the stomach. Blub. It keeps things tidy and digests whatever doesn't belong. Unwanted guests too.",
         "In elke maag woont een Maagenzym-guh. Die luistert alleen naar de baas van de maag. Blub. Hij houdt alles netjes en verteert wat er niet hoort. Ook ongewenste gasten."),
        ("Want a bigger stomach? Go to the Dentist Guh. For kaasknabbels, vads ingots and crystals he stretches your stomach. Stretch, stretch... VADS! Until it really can't go any further.",
         "Wil je een grotere maag? Ga naar de Tandarts-guh. Voor kaasknabbels, vadsstaven en kristallen rekt hij je maag op. Rekken, rekken... VADS! Tot het echt niet verder kan."),
        ("A stomach is the coziest home there is. There is always food nearby. And remember: the more vadsig, the better. For the stomach, that is. Your own belly stays just your belly.",
         "Een maag is het gezelligste huis dat er is. Er is altijd eten in de buurt. En onthoud: hoe vadsiger, hoe beter. Voor de maag dan. Je eigen buik blijft gewoon je eigen buik."),
    ], [
        ("Who lives in every guh stomach?", "Wie woont er in elke guhmaag?",
         [("A Stomach Enzyme Guh", "Een Maagenzym-guh"), ("A Mika", "Een Mika"), ("The Koningguh", "De Koningguh")]),
        ("Who makes your stomach bigger?", "Wie maakt je maag groter?",
         [("The Dentist Guh", "De Tandarts-guh"), ("Mother Vadsig", "Moeder Vadsig"), ("Verstopguhtje", "Verstopguhtje")]),
    ]),
    ("slee_kermis", (110, 190, 230), "From Sled Hut to Kermis", "Van sleehut tot kermis", [
        ("Guhs are far too vadsig to walk. That is why they invented the sled. It started with the Sled Guh, high up on the Guh Peaks, in a little hut that smells of hot chocolate.",
         "Guhs zijn veel te vadsig om te lopen. Daarom vonden ze de slee uit. Het begon bij de Slee-guh, hoog op de Guhpieken, in een hutje dat naar warme chocomelk ruikt."),
        ("His sled was broken, until a kind stranger found a sled runner, a guh bell and a pink ribbon. Then the Sled Guh taught everyone to build rails. Ding-a-ling!",
         "Zijn slee was kapot, tot een vriendelijke vreemdeling een sleeglijder, een guhbelletje en een roze lint vond. Toen leerde de Slee-guh iedereen rails bouwen. Tingeling!"),
        ("Soon the guhs built a whole kermis, with a roller coaster full of vads drops and guh corkscrews. Every lap: a kermis ticket. The Kermis Guh sells top hats for them. Fancy!",
         "Al snel bouwden de guhs een hele kermis, met een achtbaan vol vadsdrops en guhkurkentrekkers. Elk rondje: een kermisbon. De Kermis-guh verkoopt er hoge hoeden voor. Chic!"),
        ("And whoever finds Verstopguhtje's house can play hide-and-guh: tiny guhs hiding in big rooms. The guh motto: first play, then vads, then play some more. VAHOEG!",
         "En wie het huis van Verstopguhtje vindt, kan verstopguh spelen: kleine guhs zoeken in grote kamers. Het motto van de guhs: eerst spelen, dan vadsen, dan nog eens spelen. VAHOEG!"),
    ], [
        ("What did the Sled Guh need to fix his sled?", "Wat had de Slee-guh nodig voor zijn slee?",
         [("A runner, a bell and a ribbon", "Een glijder, een belletje en een lint"), ("Four wheels", "Vier wielen"), ("A rocket", "Een raket")]),
        ("What do you get for a lap on the roller coaster?", "Wat krijg je voor een rondje in de achtbaan?",
         [("A kermis ticket", "Een kermisbon"), ("A kaasknabbel", "Een kaasknabbel"), ("A guh crystal", "Een guhkristal")]),
    ]),
    ("geheim", (40, 30, 60), "The Secret Guh Book", "Het Geheime Guhboek", [
        ("If you are reading this, you found the secret bookcase. Well done, clever sleuth! This is the most secret book in the library. Tell nobody. Except guhs.",
         "Als je dit leest, heb je de geheime kast gevonden. Goed gedaan, slimme speurneus! Dit is het allergeheimste boek van de bibliotheek. Vertel het aan niemand. Behalve aan guhs."),
        ("The great secret: Mikas used to be guhs too. They kept all their kaasknabbels to themselves and never shared. Their fur turned dark, their eyes red, and their 'guh' became 'NJEG'.",
         "Het grote geheim: Mika's waren vroeger ook guhs. Ze hielden al hun kaasknabbels voor zichzelf en deelden nooit. Hun vacht werd donker, hun ogen rood, en hun 'guh' werd 'NJEG'."),
        ("But deep inside, every Mika is still a little bit guh. That is why the oldest guhs say: if a Mika ever shares a knabbel, it might turn pink again. It has never happened. Not yet.",
         "Maar diep vanbinnen is elke Mika nog een beetje guh. Daarom zeggen de oudste guhs: als een Mika ooit een knabbel deelt, wordt hij misschien weer roze. Het is nog nooit gebeurd. Nog niet."),
        ("The second secret: the Guhmension only exists because someone once gave a guh a kaasknabbel. Every knabbel you share makes the pink land a little bit bigger. Really.",
         "Het tweede geheim: de Guhmensie bestaat alleen omdat iemand ooit een guh een kaasknabbel gaf. Elke knabbel die jij deelt, maakt het roze land een beetje groter. Echt waar."),
        ("So: share your knabbels, cuddle your guhs and take a nap now and then. Then everything will be fine. Signed, the Librarian. (Psst: I secretly read comics too.) VAHOEG!",
         "Dus: deel je knabbels, knuffel je guhs en doe af en toe een dutje. Dan komt alles goed. Getekend, de Bibliothecaris. (Psst: stiekem lees ik ook weleens een strip.) VAHOEG!"),
    ], [
        ("What were Mikas long ago?", "Wat waren Mika's vroeger?",
         [("Guhs who never shared", "Guhs die nooit deelden"), ("Angry clouds", "Boze wolken"), ("Lost fish", "Verdwaalde vissen")]),
        ("What makes the Guhmension bigger?", "Wat maakt de Guhmensie groter?",
         [("Every knabbel you share", "Elke knabbel die je deelt"), ("Shouting NJEG very loudly", "Heel hard NJEG roepen"),
          ("Stacking blocks", "Blokken stapelen")]),
    ]),
    # (2.6, the Guheinde: after the secret book, so the book bits in old saves stay the same; Dutch only)
    ("guheinde", (232, 184, 60), "Het Guheinde", "Het Guheinde", [(t, t) for t in [
        "Lang geleden deelden alle guhs hun kaasknabbels. Toen kwam Opper-Mika. Hij was de grootste, de gemeenste en de hongerigste Mika van allemaal.",
        "Opper-Mika bedacht een plan: als hij alle knabbels had, zouden de guhs grauw en mager worden en de Mika's de baas. Hij noemde het 'het Grote Knabbelroof'. Njeg.",
        "Hij groef kaaskelders onder de Guhmensie om knabbels te verstoppen, en stal de laatste grote Enderguh. Die kreeg nooit een knabbel. Arm beest.",
        "Op zijn Enderguh vloog hij naar een eiland ver weg: het Guheinde. Daar stopte hij de knabbels in kristallen op hoge kaaspilaren, zodat niemand erbij kon.",
        "Maar de guhs vertellen dat een held met twaalf Ogen van Vadsig de weg zal vinden. Sla de kristallen stuk, voer de Enderguh... en de knabbels komen thuis. VAHOEG!",
    ]], [
        ("Wat stopte Opper-Mika in de kristallen?", "Wat stopte Opper-Mika in de kristallen?",
         [("Kaasknabbels", "Kaasknabbels"), ("Mika's vet", "Mika's vet"), ("Guhkristallen", "Guhkristallen")]),
        ("Wat moet je de uitgeputte Enderguh geven?", "Wat moet je de uitgeputte Enderguh geven?",
         [("Een kaasknabbel", "Een kaasknabbel"), ("Een Mika-traan", "Een Mika-traan"), ("Een zadel", "Een zadel")]),
    ]),
    # --- 2.7: found in the Mika-voorraadschuur of the Stille Voorraadkelder (tools/features/gatenkaas.py), not in the library ---
    ("voorraadkelder", (138, 90, 43), "The Diary of the Larder Mika", "Het dagboek van de Voorraadmika", [
        ("Diary of Mika Knabbelgraag, head keeper of the Silent Larder. Down here under the holey cheese we keep ALL the kaasknabbels of the Guhmension. Njeg, the guhs will never find them!",
         "Dagboek van Mika Knabbelgraag, opperbewaarder van de Stille Voorraadkelder. Hier onder de gatenkaas bewaren wij ALLE kaasknabbels van de Guhmensie. Njeg, de guhs vinden ze nooit!"),
        ("Rule 1 of the larder: NO NIBBLING. Nibbling makes noise. The knabbelsensors hear everything: steps, digging and above all chewing. So always sneak, like a proper Mika.",
         "Regel 1 van de kelder: NIET KNABBELEN. Knabbelen maakt geluid. De knabbelsensoren horen alles: stappen, hakken en vooral kauwen. Dus altijd sluipen, net als een echte Mika."),
        ("Our guard is the Vadswaker. Once he ate so many stolen knabbels that he fell asleep, deep in the cheese. Now he is blind from all that sleeping, but his ears... they hear a crumb drop a mile away.",
         "Onze bewaker is de Vadswaker. Hij at ooit zoveel gestolen knabbels dat hij in slaap viel, diep in de kaas. Nu is hij blind van al dat slapen, maar zijn oren... die horen een kruimel vallen op een kilometer."),
        ("The knabbelschreeuwers scream three times, then he wakes up. That's our secret, njeg! A guh who reads this should run away fast. Or be very, very quiet. Guhs are never quiet. Hihihi.",
         "Drie keer schreeuwen de knabbelschreeuwers, dan wordt hij wakker. Dat is ons geheim, njeg! Een guh die dit leest, moet hard wegrennen. Of heel, heel stil zijn. Guhs zijn nooit stil. Hihihi."),
        ("PS: I secretly ate one knabbel. Very quietly. The Vadswaker heard me anyway... Whoever finds this: take the knabbels back to the guhs, then they'll finally be VAHOEG again. Njeg... sorry.",
         "PS: ik heb stiekem een knabbel gegeten. Heel zachtjes. De Vadswaker hoorde me toch... Wie dit vindt: breng de knabbels terug naar de guhs, dan worden ze eindelijk weer VAHOEG. Njeg... sorry."),
    ], [
        ("What does the Vadswaker hear best?", "Wat hoort de Vadswaker het best?",
         [("Chewing", "Kauwen"), ("Singing", "Zingen"), ("Drawing", "Tekenen")]),
        ("How many times do the knabbelschreeuwers scream before the Vadswaker wakes up?", "Hoe vaak schreeuwen de knabbelschreeuwers voordat de Vadswaker wakker wordt?",
         [("Three times", "Drie keer"), ("Never", "Nooit"), ("Ten times", "Tien keer")]),
    ]),
]
SECRET = "geheim"
# books that are found somewhere else in the world, not in the library (2.7: the diary of the Voorraadmika lies in the
# Mika-voorraadschuur of the Stille Voorraadkelder, tools/features/gatenkaas.py): no lectern, not in the archive chests
ELSEWHERE = ("voorraadkelder",)


def on_lecterns():
    """The books that lie open on the lecterns of the reading room."""
    return [i for i, b in enumerate(BOOKS) if b[0] != SECRET and b[0] not in ELSEWHERE]
PAGE_MAX = 245   # characters that fit on one book page (about 14 lines)

CLOTHES = ["bieb_leesbril", "bieb_vest", "bieb_hoed"]
# the scholar's outfit: little golden reading glasses, the bookworm cardigan and the scholar's beret (id bieb_hoed): a
# floppy round beret a bit to one side, with a little stalk on top (the swatches of other hats are reused: the texture of
# this piece only paints them in its own colours)
_H = [0, 6, -2]
BONES = {
    "outfit_bieb_hoed": ("head", _H, "cap", [([-3.5, 15, -8.5], [7, 1, 5.5], 0), ([-5.8, 16, -10.6], [10.8, 1.4, 9], 0),
                                            ([-4.8, 17.4, -9.6], [8.8, 0.5, 7], 0)]),
    "outfit_bieb_steeltje": ("head", _H, "pom", [([-0.9, 17.9, -6.6], [0.9, 0.9, 0.9], 0)]),
}


def leesbril_frame(v, rng):
    """A thin golden frame with see-through lenses (each lens of the glasses bone shows the whole swatch)."""
    import numpy as np
    frame = v.fabric((225, 180, 70), rng, 5)
    n = frame.shape[0]
    rgba = np.zeros((n, n, 4), np.uint8)
    rgba[..., :3] = frame
    edge = max(2, n // 8)
    rgba[:edge, :, 3] = rgba[-edge:, :, 3] = rgba[:, :edge, 3] = rgba[:, -edge:, 3] = 255
    return rgba


def clothes(rng, v):
    f = lambda c, n=6: (lambda: v.fabric(c, rng, n))
    return {
        "bieb_leesbril": {"glasses": lambda: leesbril_frame(v, rng)},
        "bieb_vest": {"suit": lambda: v.buttons((150, 70, 110), (245, 205, 90), rng)},
        "bieb_hoed": {"cap": f((135, 35, 70), 6), "pom": f((85, 20, 45), 4)},
    }


def icons(ic):
    shapes = {
        "baret": ic.pad(["......cc........", "...aaaaaaaaaa...", ".aabbbbbbbbbbaa.", "abbbbbbbbbbbbbba", ".abbbbbbbbbbbba.",
                         "..aaaaaaaaaaaa..", "...adddddddda..."]),
        "leesbril": ic.pad(["..aaaa....aaaa..", ".a....a..a....a.", ".a.bb.aaaa.bb.a.", ".a....a..a....a.", "..aaaa....aaaa..",
                            "a..............a"]),
    }
    return {
        "bieb_leesbril": ic.icon(shapes["leesbril"], {"a": (200, 150, 40), "b": (220, 240, 255)}),
        "bieb_vest": ic.shirt((150, 70, 110), (95, 40, 70), (245, 205, 90), "buttons"),
        "bieb_hoed": ic.icon(shapes["baret"], {"a": (80, 18, 40), "b": (150, 40, 78), "c": (80, 18, 40), "d": (110, 28, 55)}),
    }


# =====================================================================================================================
# resources
# =====================================================================================================================
def check_java(h):
    """The books in Guhboek.java must be these books, with these page counts, in this order."""
    path = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bibliotheek", "Guhboek.java")
    java = re.findall(r'^\s{4}[A-Z_]+\("([a-z_]+)", (\d+), (\d+), 0x[0-9A-F]{6}\)', open(path, encoding="utf-8").read(), re.M)
    ours = [(bid, str(len(pages)), str(len(quiz))) for bid, _c, _en, _nl, pages, quiz in BOOKS]
    assert java == ours, f"Guhboek.java {java} != bibliotheek.py {ours}"
    for bid, _c, en, nl, pages, quiz in BOOKS:
        assert len(en) <= 40 and len(nl) <= 40, bid
        for n, (pen, pnl) in enumerate(pages):
            for text in (pen, pnl):
                assert len(text) <= PAGE_MAX, f"page {bid}.{n} is too long ({len(text)}): {text[:40]}..."
        for q in quiz:
            assert len(q[2]) == 3, bid


def build(h):
    check_java(h)
    lang = h.lang
    A, D = h.A, h.D

    # --- the books (pages, titles) and the quiz ---
    for i, (bid, colour, en, nl, pages, quiz) in enumerate(BOOKS):
        lang(f"book.guhs.bieb.{bid}.title", en, nl)
        for n, (pen, pnl) in enumerate(pages):
            lang(f"book.guhs.bieb.{bid}.{n}", pen, pnl)
        for n, (qen, qnl, answers) in enumerate(quiz):
            lang(f"gui.guhs.bieb.kwis.{bid}.{n}", qen, qnl)
            for a, (aen, anl) in enumerate(answers):
                lang(f"gui.guhs.bieb.kwis.{bid}.{n}.{a}", aen, anl)
    lang("item.guhs.bieb_boek.lore", "Guh book %s of %s - from the guh library", "Guhboek %s van %s - uit de guhbibliotheek")
    lang("item.guhs.bieb_boek.lore_secret", "The secret guh book - tell nobody!", "Het geheime guhboek - vertel het niemand!")

    # --- boekenbon (the library's currency): a pink voucher with a little open book on it ---
    pal = dict(h.ITEM_PAL)
    pal.update({"b": (250, 245, 235, 255), "B": (200, 185, 160, 255), "r": (214, 90, 140, 255), "y": (245, 200, 60, 255)})
    rows = ["................", "................", "................", "..kkkkkkkkkkkk..", ".kCCCCCCCCCCCCk.", ".kCyCCCCCCCCyCk.",
            ".kCC.bbbrbbb.Ck.", ".kCC.bBbrbBb.Ck.", ".kCC.bbbrbbb.Ck.", ".kCC.bBbrbBb.Ck.", ".kCC..rrrrr..Ck.", ".kCyCCCCCCCCyCk.",
            ".kCCCCCCCCCCCCk.", "..kkkkkkkkkkkk..", "................", "................"]
    h.save(h.grid(rows, pal), "item", "boekenbon.png")
    h.item_model("boekenbon")

    # --- the secret bookshelf door: a bookshelf with one pink book sticking out (closed) / swung open (a thin panel) ---
    shelf = h.vanilla("block/bookshelf").convert("RGBA")
    px = shelf.load()
    for y in range(9, 15):                                   # one book on the lower shelf is pink... and sticks out a bit
        for x in (10, 11):
            px[x, y] = (236, 110, 170, 255) if x == 10 else (200, 80, 140, 255)
    px[10, 8] = (255, 170, 210, 255); px[11, 8] = (236, 110, 170, 255)
    h.save(shelf, "block", "bieb_geheime_kast.png")
    tex = {"particle": "guhs:block/bieb_geheime_kast", "side": "guhs:block/bieb_geheime_kast", "end": "minecraft:block/oak_planks"}
    w = h.w
    w(f"{A}/models/block/bieb_geheime_kast.json", {"parent": "minecraft:block/cube_column", "textures": {
        "particle": tex["particle"], "side": tex["side"], "end": tex["end"]}})
    w(f"{A}/models/block/bieb_geheime_kast_open.json", {"parent": "minecraft:block/block", "textures": tex, "elements": [
        {"from": [0, 0, 0], "to": [2, 16, 16], "faces": {"east": {"texture": "#side"}, "west": {"texture": "#side"},
                                                          "north": {"texture": "#end", "uv": [0, 0, 2, 16]}, "south": {"texture": "#end", "uv": [0, 0, 2, 16]},
                                                          "up": {"texture": "#end", "uv": [0, 0, 2, 16]}, "down": {"texture": "#end", "uv": [0, 0, 2, 16]}}}]})
    w(f"{A}/blockstates/bieb_geheime_kast.json", {"variants": {
        f"facing={f},open={o}": {"model": "guhs:block/bieb_geheime_kast" + ("_open" if o == "true" else ""), **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for o in ("false", "true")}})
    w(f"{A}/models/item/bieb_geheime_kast.json", {"parent": "guhs:block/bieb_geheime_kast"})
    h.self_drop("bieb_geheime_kast")
    h.shaped("bieb_geheime_kast", ["BRB"], {"B": "minecraft:bookshelf", "R": "minecraft:redstone"}, "guhs:bieb_geheime_kast", 2)

    # --- the book stand of the secret room: a golden stand with the secret book lying open on it ---
    el = h.el
    stand_tex = {"particle": "minecraft:block/gold_block", "gold": "minecraft:block/gold_block", "wood": "minecraft:block/dark_oak_planks",
                 "cover": "minecraft:block/purple_wool", "paper": "minecraft:block/white_wool", "glow": "minecraft:block/amethyst_block"}
    h.furniture_model("bieb_boekaltaar", [
        el([3, 0, 3], [13, 2, 13], "#wood"), el([6, 2, 6], [10, 10, 10], "#gold"), el([4, 10, 4], [12, 12, 12], "#wood"),
        el([3, 12, 4], [8, 13, 12], "#cover", rot={"angle": 22.5, "axis": "z", "origin": [8, 12, 8]}),
        el([8, 12, 4], [13, 13, 12], "#cover", rot={"angle": -22.5, "axis": "z", "origin": [8, 12, 8]}),
        el([3.5, 13, 4.5], [8, 13.6, 11.5], "#paper", rot={"angle": 22.5, "axis": "z", "origin": [8, 12, 8]}),
        el([8, 13, 4.5], [12.5, 13.6, 11.5], "#paper", rot={"angle": -22.5, "axis": "z", "origin": [8, 12, 8]}),
        el([7.5, 12, 4], [8.5, 14, 12], "#glow")], stand_tex)
    # (no recipe: a crafted stand would hand out the secret book anywhere; it only stands in the secret room)
    stale = f"{D}/recipe/bieb_boekaltaar.json"
    if os.path.exists(stale):
        os.remove(stale)

    # --- the guh armchair: a pink guh to sit in (round belly seat, paws for armrests, the head with ears as backrest) ---
    ft = {"particle": "minecraft:block/pink_wool", "fur": "minecraft:block/pink_wool", "light": "minecraft:block/white_wool",
          "dark": "minecraft:block/magenta_wool", "eye": "minecraft:block/black_wool", "wood": "minecraft:block/cherry_planks"}
    h.furniture_model("bieb_guhfauteuil", [
        el([1, 0, 1], [15, 3, 15], "#wood"),
        el([1, 3, 1], [15, 8, 15], "#fur"), el([3, 8, 2], [13, 9, 12], "#light"),                    # the seat (a round guh belly)
        el([0, 3, 1], [3, 12, 12], "#fur"), el([13, 3, 1], [16, 12, 12], "#fur"),                    # the paws as armrests
        el([0, 12, 1], [3, 13, 4], "#light"), el([13, 12, 1], [16, 13, 4], "#light"),
        el([1, 3, 12], [15, 22, 16], "#fur"),                                                        # the head: the backrest
        el([1, 22, 13], [5, 27, 15], "#fur"), el([11, 22, 13], [15, 27, 15], "#fur"),                  # the ears
        el([2, 23, 12.9], [4, 26, 13], "#dark", faces=("north",)), el([12, 23, 12.9], [14, 26, 13], "#dark", faces=("north",)),
        el([3, 16, 16], [6, 19, 16.1], "#eye", faces=("south",)), el([10, 16, 16], [13, 19, 16.1], "#eye", faces=("south",)),
        el([7, 14, 16], [9, 15, 16.1], "#dark", faces=("south",))], ft)
    h.shaped("bieb_guhfauteuil", ["P  ", "WWW", "PPP"], {"P": "minecraft:cherry_planks", "W": "minecraft:pink_wool"}, "guhs:bieb_guhfauteuil")

    # --- the Bibliothecaris: a lavender guh with golden reading glasses painted on ---
    src = h.Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    img = h.recolour(src, hue=0.74, sat=0.75, val=1.02, only=h.pinkish).convert("RGBA")
    reading_glasses(img, h.np)
    h.save(img, "entity", "npc_bibliothecaris.png")

    # --- the archive chest: paper, books, snacks, and sometimes a lore book (never the secret one) ---
    w(f"{D}/loot_table/chests/guhbibliotheek_archief.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            {"type": "minecraft:item", "name": "minecraft:paper", "weight": 6, "functions": h.count_fn(2, 8)},
            {"type": "minecraft:item", "name": "minecraft:book", "weight": 5, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:feather", "weight": 2, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:ink_sac", "weight": 2, "functions": h.count_fn(1, 2)},
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": h.count_fn(4, 12)},
            {"type": "minecraft:item", "name": "guhs:boekenbon", "weight": 2}]},
        {"rolls": 1, "entries": [lore_book_entry(i) for i in on_lecterns()],
         "conditions": [{"condition": "minecraft:random_chance", "chance": 0.6}]}]})

    # --- the structure, very rare; nothing spawns in it ---
    h.TEMPLATE_SIZES["guhbibliotheek"] = 88
    h.FLATNESS["guhbibliotheek"] = 30
    none = {"bounding_box": "full", "spawns": []}
    h.structure("guhbibliotheek", h.GUHMENSION_LAND, spacing=90, separation=30, salt=20240199,
                spawn_overrides={"creature": none, "monster": none, "ambient": none})
    s = library_structure(h)
    problems = check_structure(s, h)
    if problems:
        raise SystemExit("guhbibliotheek self-check failed:\n  " + "\n  ".join(problems[:40]))
    s.save("guhbibliotheek")

    # --- advancements: hidden quest ones (granted by the mod) and three you can see ---
    for name in ("bieb_eerste_boek", "bieb_zes_boeken", "bieb_alle_boeken", "bieb_geheim", "bieb_kwis", "bieb_kwismeester"):
        w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, crit, en_t, nl_t, en_d, nl_d in [
        ("find_guhbibliotheek", "enter_guhmension", "minecraft:bookshelf", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:guhbibliotheek"]}}}},
         "Ssssst!", "Ssssst!", "Find the very rare guh library", "Vind de zeer zeldzame guhbibliotheek"),
        ("bieb_boekenwurm", "find_guhbibliotheek", "minecraft:written_book", "challenge",
         {"trigger": "minecraft:impossible"},
         "Bookworm Guh", "Boekenwurmguh", f"Collect all {len(BOOKS)} guh books, the secret one too",
         f"Verzamel alle {len(BOOKS)} guhboeken, ook het geheime"),
        ("bieb_outfit", "find_guhbibliotheek", "guhs:bieb_hoed", "goal",
         None,
         "Professor Guh", "Professor Guh", "Buy the whole scholar's outfit with book vouchers",
         "Koop het hele geleerdenpakje met boekenbonnen"),
    ]:
        w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            # (the outfit: one criterion per piece, each remembered on its own: you may dress your guh in between)
            "criteria": {"done": crit} if crit else {
                piece: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{piece}"}]}}
                for piece in CLOTHES}})
        lang(f"advancements.guhs.guhmension.{name}.title", en_t, nl_t)
        lang(f"advancements.guhs.guhmension.{name}.description", en_d, nl_d)

    # --- names and texts ---
    for key, en, nl in [
        ("structure.guhs.guhbibliotheek", "Guh library", "Guhbibliotheek"),
        ("structure.guhs.guhbibliotheek.tooltip", f"Very rare: {len(on_lecterns())} guh books to read on lecterns, the Bibliothecaris, a quiz and a secret room",
         f"Zeer zeldzaam: {len(on_lecterns())} guhboeken om te lezen op lessenaars, de Bibliothecaris, een kwis en een geheime kamer"),
        ("entity.guhs.guh_npc.bibliothecaris", "Bibliothecaris", "Bibliothecaris"),
        ("item.guhs.boekenbon", "Book voucher", "Boekenbon"),
        ("item.guhs.boekenbon.lore", "Spend it at the Bibliothecaris in the guh library", "Te besteden bij de Bibliothecaris in de guhbibliotheek"),
        ("block.guhs.bieb_geheime_kast", "Secret bookcase", "Geheime boekenkast"),
        ("block.guhs.bieb_boekaltaar", "Secret book stand", "Geheim boekaltaar"),
        ("block.guhs.bieb_guhfauteuil", "Guh reading armchair", "Guhleesfauteuil"),
        ("item.guhs.bieb_leesbril", "Little scholar's glasses", "Geleerdenbrilletje"),
        ("item.guhs.bieb_vest", "Bookworm cardigan", "Boekenwurmvest"),
        ("item.guhs.bieb_hoed", "Scholar's beret", "Geleerdenbaret"),
        ("gui.guhs.bieb.no_build", "Ssst! This is a library: no breaking and no building here. Njeg!",
         "Ssst! Dit is een bibliotheek: hier wordt niet gesloopt en niet gebouwd. Njeg!"),
        ("gui.guhs.bieb.kast_closed", "The bookcase swings shut again. Creak...", "De boekenkast zwaait weer dicht. Krrr..."),
        # the Bibliothecaris
        ("quest.guhs.bieb.hello", "Ssst... welcome back, reader! All guh books lie open on the lecterns in the reading room. Shall I quiz you?",
         "Ssst... welkom terug, lezer! Alle guhboeken liggen open op de lessenaars in de leeszaal. Zin in een kwisvraag?"),
        ("quest.guhs.bieb.hello_first", "Ooh, a new reader! Ssst, not so loud. Every guh book lies open on a lectern in the reading room: read them all, right away. You may take one copy of each home. Once! Read well, and I will quiz you. Vads!",
         "Ooh, een nieuwe lezer! Ssst, niet zo hard. Elk guhboek ligt open op een lessenaar in de leeszaal: lees ze maar meteen allemaal. Van elk boek mag je één exemplaar meenemen. Eén keer! Lees goed, dan stel ik je kwisvragen. Vads!"),
        ("quest.guhs.bieb.taken", "You take a copy of '%s'. Guh-enjoy your reading!", "Je neemt een exemplaar van '%s' mee. Guhlezen maar!"),
        ("quest.guhs.bieb.taken_already", "You already took your copy of '%s'. Lost it? The Bibliothecaris sells spare ones. Njeg!",
         "Je hebt je exemplaar van '%s' al meegenomen. Kwijt? De Bibliothecaris verkoopt reserve-exemplaren. Njeg!"),
        ("quest.guhs.bieb.right", "VAHOEG! Correct! You really read it. Here is a book voucher.", "VAHOEG! Goed zo! Jij hebt echt gelezen. Hier is een boekenbon."),
        ("quest.guhs.bieb.right_again", "Correct again! You already got a voucher for this one, so here are some knabbels.",
         "Weer goed! Voor deze vraag had je al een boekenbon, dus je krijgt een paar knabbels."),
        ("quest.guhs.bieb.wrong", "Njeg... that's not it. The answer was: %s. Read '%s' again!",
         "Njeg... dat is het niet. Het antwoord was: %s. Lees '%s' nog maar eens!"),
        ("quest.guhs.bieb.kwismeester", "You answered every question correctly! You are a real Guhkwis master. VAHOEGE VADS!",
         "Je hebt alle vragen goed beantwoord! Jij bent een echte Guhkwismeester. VAHOEGE VADS!"),
        ("quest.guhs.bieb.collected", "New guh book in your collection: '%s' (%s / %s)", "Nieuw guhboek in je verzameling: '%s' (%s / %s)"),
        ("quest.guhs.bieb.all_collected", "You collected every guh book! Real bookworm guh, VAHOEG!",
         "Je hebt alle guhboeken verzameld! Echte boekenwurmguh, VAHOEG!"),
        ("quest.guhs.bieb.altar", "The book on the stand glows... You found the Secret Guh Book!",
         "Het boek op de standaard gloeit... Je hebt het Geheime Guhboek gevonden!"),
        # the screen
        ("gui.guhs.bieb.books", "Your guh books: %s / %s", "Jouw guhboeken: %s / %s"),
        ("gui.guhs.bieb.bonnen", "Book vouchers: %s", "Boekenbonnen: %s"),
        ("gui.guhs.bieb.shop", "Library shop", "Bibliotheekwinkeltje"),
        ("gui.guhs.bieb.shop.tooltip", "Spare books, the scholar's outfit (only sold here!) and the guh armchair, for book vouchers",
         "Reserveboeken, het geleerdenpakje (alleen hier te koop!) en de guhfauteuil, voor boekenbonnen"),
        ("gui.guhs.bieb.missing", "??? (not read yet: it lies on a lectern in the reading room)", "??? (nog niet gelezen: hij ligt op een lessenaar in de leeszaal)"),
        ("gui.guhs.bieb.missing_secret", "??? (not on any lectern... ssst)", "??? (ligt op geen enkele lessenaar... ssst)"),
        ("gui.guhs.bieb.spine.have", "In your collection. Vadsig!", "In je verzameling. Vadsig!"),
        ("gui.guhs.bieb.spine.take", "Read, but no copy yet: take one at its lectern", "Gelezen, maar nog geen exemplaar: neem er een mee bij de lessenaar"),
        ("gui.guhs.bieb.spine.read", "Read (your copy is gone: buy a spare one)", "Gelezen (je exemplaar is weg: koop een reserve-exemplaar)"),
        ("gui.guhs.bieb.lessenaars", "All books lie open on the lecterns in the reading room", "Alle boeken liggen open op de lessenaars in de leeszaal"),
        ("gui.guhs.bieb.take", "Take a copy", "Exemplaar meenemen"),
        ("gui.guhs.bieb.take.tooltip", "One copy per book per reader. Only once, so guh-mind it!", "Eén exemplaar per boek per lezer. Maar één keer, dus pas er goed op, guh!"),
        ("gui.guhs.bieb.taken", "Copy taken", "Exemplaar al mee"),
        ("gui.guhs.bieb.taken.tooltip", "You already took this one. Lost it? The Bibliothecaris sells spare copies.",
         "Deze heb je al meegenomen. Kwijt? De Bibliothecaris verkoopt reserve-exemplaren."),
        ("gui.guhs.bieb.kwis", "Guhkwis", "Guhkwis"),
        ("gui.guhs.bieb.kwis.none", "Read a guh book first, then I will ask you something about it!",
         "Lees eerst een guhboek, dan stel ik je er een vraag over!"),
        ("gui.guhs.bieb.kwis.wait", "Next question in %s seconds...", "Volgende vraag over %s seconden..."),
        ("gui.guhs.bieb.kwis.score", "Answered correctly: %s / %s", "Goed beantwoord: %s / %s"),
        ("gui.guhs.bieb.kwis.about", "(about '%s')", "(over '%s')"),
    ]:
        lang(key, en, nl)


def lore_book_entry(i):
    bid, _c, en, nl, pages, _q = BOOKS[i]
    return {"type": "minecraft:item", "name": "minecraft:written_book", "functions": [
        {"function": "minecraft:set_book_cover", "title": f"Guhboek {i + 1}", "author": "De Bibliothecaris", "generation": 0},
        {"function": "minecraft:set_written_book_pages", "mode": "replace_all",
         "pages": [json.dumps({"translate": f"book.guhs.bieb.{bid}.{n}"}) for n in range(len(pages))]},
        {"function": "minecraft:set_name", "target": "custom_name",
         "name": {"translate": f"book.guhs.bieb.{bid}.title", "italic": False, "color": "#F7B6CB"}},
        {"function": "minecraft:set_lore", "mode": "replace_all",
         "lore": [{"translate": "item.guhs.bieb_boek.lore", "with": [str(i + 1), str(len(BOOKS))], "color": "gray", "italic": False}]},
        {"function": "minecraft:set_components", "components": {"minecraft:enchantment_glint_override": False}},
        {"function": "minecraft:set_custom_data", "tag": "{GuhsBoek:\"%s\"}" % bid}]}


def reading_glasses(img, np):
    """Paints golden round reading glasses around the two eyes of the sitting guh texture (found by their blue irises)."""
    a = np.asarray(img).astype(np.int32).copy()
    blue = (a[..., 2] > 150) & (a[..., 2] - a[..., 0] > 60) & (a[..., 3] > 0)
    ys, xs = np.nonzero(blue)
    if len(xs) == 0:
        return
    mid = (xs.min() + xs.max()) / 2
    boxes = []
    for side in (xs < mid, xs >= mid):
        boxes.append((xs[side].min() - 2, ys[side].min() - 2, xs[side].max() + 2, ys[side].max() + 2))
    gold = (225, 180, 60, 255)
    for (x0, y0, x1, y1) in boxes:
        cx, cy, rx, ry = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2, (y1 - y0) / 2
        for y in range(int(y0) - 1, int(y1) + 2):
            for x in range(int(x0) - 1, int(x1) + 2):
                d = math.hypot((x - cx) / rx, (y - cy) / ry)
                if 0.93 <= d <= 1.12 and 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
                    a[y, x] = gold
    (lx0, ly0, lx1, ly1), (rx0, ry0, rx1, ry1) = boxes
    y = int((ly0 + ly1) / 2) - 1
    for x in range(int(lx1), int(rx0) + 1):
        a[y, x] = gold
    img.paste(h_image(a, img))


def h_image(a, img):
    from PIL import Image
    return Image.fromarray(a.astype("uint8"), img.mode)


# =====================================================================================================================
# the structure
# =====================================================================================================================
W, H, D = 88, 40, 86
BX0, BX1, BZ0, BZ1 = 12, 75, 14, 61          # outer walls of the library
G0, G1, G2, ROOF = 1, 8, 15, 22              # floor levels (block y) and the roof
AX0, AX1, AZ0, AZ1 = 28, 59, 15, 49          # the atrium (open from the ground to the glass roof)
CX = 44                                      # the middle (x)
SECRET_ROOM = (66, 74, 15, 21)               # x0, x1, z0, z1 of the secret room (on gallery 1)
DOOR = (70, 22)                              # the secret bookcase door (x, z), 2 high on gallery 1
NPC = (CX, G0 + 1, 54)                       # the Bibliothecaris behind the lending desk
LECTERN_Z = 47                               # the row of lecterns with the guh books (the reading room)


def props_fence(name):
    return {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}


def library_structure(h):
    Structure, mc = h.Structure, h.mc
    s = Structure((W, H, D))
    rng = random.Random(2404)
    stair = lambda facing, half="bottom": {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"}
    fp = [(x, z) for x in range(W) for z in range(D)]

    # --- the grounds: grass, a pink path to the door, hedges and flower beds, lamp posts ---
    for x, z in fp:
        s.set(x, 0, z, mc("grass_block"), {"snowy": "false"})
    for x in range(BX0 - 2, BX1 + 3):                          # a white stone rim round the building
        for z in range(BZ0 - 2, BZ1 + 3):
            if not (BX0 <= x <= BX1 and BZ0 <= z <= BZ1):
                s.set(x, 0, z, mc("smooth_quartz"))
    for z in range(BZ1 + 3, D):                                # the path to the entrance
        for x in range(CX - 3, CX + 4):
            s.set(x, 0, z, mc("pink_concrete_powder") if abs(x - CX) < 3 else mc("white_concrete"))
    fountain(s, h, CX, 74)
    for x, z in fp:                                            # hedge round the grounds (with a gap for the path)
        ring = min(x, z, W - 1 - x, D - 1 - z)
        if ring == 0 and not (z == D - 1 and abs(x - CX) <= 3):
            s.set(x, 1, z, mc("flowering_azalea_leaves"), {"persistent": "true", "distance": "7", "waterlogged": "false"})
        elif ring == 1 and s.get(x, 0, z) == mc("grass_block") and (x * 7 + z * 3) % 4 == 0:
            s.set(x, 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
    for (x, z) in [(CX - 5, 63), (CX + 5, 63), (CX - 5, D - 2), (CX + 5, D - 2), (4, 4), (W - 5, 4), (4, D - 5), (W - 5, D - 5),
                   (4, 38), (W - 5, 38)]:
        lamp_post(s, mc, x, z)
    for (x, z, facing) in [(CX - 13, 71, "east"), (CX - 13, 77, "east"), (CX + 13, 71, "west"), (CX + 13, 77, "west")]:
        s.set(x, 1, z, "guhs:guh_bank", {"facing": facing})
    for (x0, z0) in [(18, 67), (62, 67), (18, 77), (62, 77), (20, 4), (60, 4)]:     # flower beds
        for x in range(x0, x0 + 8):
            for z in range(z0, z0 + 5):
                s.set(x, 0, z, mc("moss_block"))
                if (x + z) % 2:
                    s.set(x, 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes",
                                               "minecraft:pink_tulip", "minecraft:allium"]))

    # --- the shell: foundation, floor, walls with pilasters and tall windows ---
    s.fill(BX0, 0, BZ0, BX1, 0, BZ1, mc("stone_bricks"))
    s.fill(BX0 + 1, 1, BZ0 + 1, BX1 - 1, ROOF + 1, BZ1 - 1, mc("air"))     # nothing of the hill inside
    for x in range(BX0 + 1, BX1):
        for z in range(BZ0 + 1, BZ1):
            if glow_tile(x, z):
                s.set(x, G0, z, mc("pearlescent_froglight"), {"axis": "y"})     # glowing tiles: light everywhere
            else:
                s.set(x, G0, z, mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
    for y in range(G0, ROOF + 1):
        for x in range(BX0, BX1 + 1):
            for z in (BZ0, BZ1):
                s.set(x, y, z, wall_block(mc, x - BX0, y))
        for z in range(BZ0, BZ1 + 1):
            for x in (BX0, BX1):
                s.set(x, y, z, wall_block(mc, z - BZ0, y))
    for (i0, i1, fixed, along_x) in [(BX0, BX1, BZ1, True), (BZ0, BZ1, BX0, False), (BZ0, BZ1, BX1, False)]:
        for i in range(i0 + 3, i1 - 2):
            if (i - i0) % 6 in (2, 3):                         # pairs of tall windows on every floor
                for fy in (G0, G1, G2):
                    for y in range(fy + 2, fy + 6):
                        x, z = (i, fixed) if along_x else (fixed, i)
                        if along_x and abs(x - CX) <= 7:
                            continue                           # (the entrance and the balcony door)
                        s.set(x, y, z, mc("pink_stained_glass"))
    # the entrance: a wide door in the south wall under a guh-ear arch, steps up to it
    for x in range(CX - 3, CX + 4):
        for y in range(G0 + 1, G0 + 6):
            s.set(x, y, BZ1, mc("air"))
        s.set(x, G0, BZ1, mc("pink_concrete"))
        s.set(x, G0, BZ1 + 1, mc("quartz_stairs"), stair("north"))
    for x in (CX - 4, CX + 4):
        for y in range(G0 + 1, G0 + 7):
            s.set(x, y, BZ1 + 1, mc("quartz_pillar"), {"axis": "y"})
        s.set(x, G0 + 7, BZ1 + 1, mc("gold_block"))
    for x in range(CX - 3, CX + 4):                            # the arch, with two round guh ears on top
        s.set(x, G0 + 6, BZ1, mc("gold_block") if abs(x - CX) == 3 else mc("pink_concrete"))
    s.set(CX - 3, G0 + 5, BZ1, mc("quartz_stairs"), stair("east", "top"))
    s.set(CX + 3, G0 + 5, BZ1, mc("quartz_stairs"), stair("west", "top"))

    # --- the façade above the door: a big guh face (with ears) in the wall ---
    facade_face(s, mc)

    # --- galleries: two floors round the atrium, with a railing along the edge ---
    for fy in (G1, G2):
        for x in range(BX0 + 1, BX1):
            for z in range(BZ0 + 1, BZ1):
                if not in_atrium(x, z) and glow_tile(x, z):
                    s.set(x, fy, z, mc("pearlescent_froglight"), {"axis": "y"})
                elif not in_atrium(x, z):
                    s.set(x, fy, z, mc("stripped_cherry_wood"), {"axis": "y"} if (x + z) % 3 else {"axis": "x"})
        for x in range(BX0 + 1, BX1):
            for z in range(BZ0 + 1, BZ1):
                if not in_atrium(x, z) and any(in_atrium(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    s.set(x, fy + 1, z, mc("yellow_stained_glass_pane") if (x + z) % 6 == 0 else mc("white_stained_glass_pane"))

    # --- the roof: flat with a parapet, glass over the atrium, a dome with guh ears on it ---
    for x in range(BX0, BX1 + 1):
        for z in range(BZ0, BZ1 + 1):
            if in_atrium(x, z):
                s.set(x, ROOF, z, mc("white_concrete") if (x - AX0) % 8 == 0 or (z - AZ0) % 8 == 0 else mc("pink_stained_glass"))
            else:
                s.set(x, ROOF, z, mc("smooth_quartz"))
            edge = x in (BX0, BX1) or z in (BZ0, BZ1)
            if edge:
                s.set(x, ROOF + 1, z, mc("gold_block") if (x + z) % 8 == 0 else mc("quartz_block"))
    dome(s, mc)
    for (x, z) in [(BX0, BZ0), (BX1, BZ0), (BX0, BZ1), (BX1, BZ1)]:
        turret(s, mc, x, z)

    # --- the stained-glass guh face window in the north wall ---
    window_face(s, mc)

    # --- stairs: on both sides, ground -> gallery 1 -> gallery 2 ---
    for (sx, sy0, z_hi) in [(25, G0, 45), (61, G0, 45), (25, G1, 37), (61, G1, 37)]:
        stairs(s, mc, sx, sy0, z_hi)

    # --- bookshelves along the walls on every floor, and rows of them in the side wings ---
    shelves(s, h, rng)
    # lamps under the galleries (and in the corners of the ground floor)
    for fy in (G1, G2, ROOF):
        for x in range(BX0 + 3, BX1 - 1, 5):
            for z in range(BZ0 + 3, BZ1 - 1, 5):
                if not in_atrium(x, z) and s.get(x, fy - 1, z) == mc("air") and not passable(s.get(x, fy, z)):
                    s.set(x, fy - 1, z, "guhs:lampion_roze" if (x + z) % 2 else "guhs:lampion_geel", {"hanging": "true", "waterlogged": "false"})

    # --- the ground floor: lending desk + the Bibliothecaris, the mosaic, reading tables, the globe ---
    desk(s, mc)
    s.entity(NPC[0] + 0.5, NPC[1] + 0.0, NPC[2] + 0.5, {"id": "guhs:guh_npc", "Kind": "bibliothecaris", "PersistenceRequired": h.Byte(1),
                                                    "Rotation": h.floats(0.0, 0.0)})
    mosaic(s, mc, CX, 36)
    for x0 in (30, 53):                                        # long reading tables left and right of the mosaic
        for z in range(28, 45, 4):
            for dx in range(0, 4):
                s.set(x0 + dx, G0 + 1, z, "guhs:guh_tafel", {"facing": "north"})
                s.set(x0 + dx, G0 + 1, z - 1, "guhs:guh_stoel", {"facing": "south"})
                s.set(x0 + dx, G0 + 1, z + 1, "guhs:guh_stoel", {"facing": "north"})
            s.set(x0 + 1, G0 + 2, z, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    reading_room(s, h)
    globe(s, mc, rng, CX, 20)
    for (x, z) in [(AX0 + 1, 17), (AX1 - 1, 17), (AX0 + 1, 47), (AX1 - 1, 47)]:   # crystal lamps on the atrium floor
        s.set(x, G0 + 1, z, "guhs:guh_kristal_lamp")
    for (x, z) in [(30, 24), (57, 24), (30, 48), (57, 48), (CX - 6, 24), (CX + 6, 24)]:
        s.set(x, G0 + 1, z, "guhs:guh_kristal_lamp")
    chandeliers(s, mc)

    # --- reading nooks with guh armchairs in the corners and on the balconies ---
    for (x0, z0, x1, z1) in [(14, 52, 22, 59), (65, 52, 73, 59), (14, 16, 22, 21), (65, 16, 73, 21)]:
        nook(s, mc, rng, x0, G0, z0, x1, z1)
    for (x0, z0, x1, z1) in [(14, 52, 22, 59), (65, 52, 73, 59), (14, 16, 22, 21)]:
        nook(s, mc, rng, x0, G1, z0, x1, z1)
    for (x0, z0, x1, z1) in [(14, 52, 22, 59), (65, 52, 73, 59), (14, 16, 22, 21), (65, 16, 73, 21)]:
        nook(s, mc, rng, x0, G2, z0, x1, z1)
    balcony(s, mc, AX0, 1)           # west balcony sticks out into the atrium (gallery 1)
    balcony(s, mc, AX1, -1)          # east one
    front_balcony(s, mc)

    # --- gallery 2: the map room and the archive (loot) ---
    for x in range(34, 54, 5):
        s.set(x, G2 + 1, 57, mc("cartography_table"))
        s.set(x + 1, G2 + 1, 57, mc("lectern"), {"facing": "north", "has_book": "false", "powered": "false"})
    for (x, facing) in [(30, "south"), (57, "south")]:
        h.chest(s, x, G2 + 1, 59, "north", "guhs:chests/guhbibliotheek_archief")

    # --- the secret room behind the bookcase (gallery 1, north-east) ---
    secret_room(s, h, mc)

    fix_connections(s, mc)
    outside = [(x, z) for x, z in fp if not (BX0 - 2 <= x <= BX1 + 2 and BZ0 - 2 <= z <= BZ1 + 2)]
    s.clear_above(outside, 1)
    return s


def lectern_book(i):
    """The block entity data of a lectern with guh book i lying open on it (a full written book, like Guhboek.stack())."""
    bid, _c, en, nl, pages, _q = BOOKS[i]
    B = ms.Byte
    return {"id": "minecraft:lectern", "Page": 0, "Book": {"id": "minecraft:written_book", "count": 1, "components": {
        "minecraft:written_book_content": {"title": {"raw": f"Guhboek {i + 1}"}, "author": "De Bibliothecaris", "generation": 0,
                                           "pages": ms.NbtList(8, [json.dumps({"translate": f"book.guhs.bieb.{bid}.{n}"}) for n in range(len(pages))]),
                                           "resolved": B(1)},
        "minecraft:custom_name": json.dumps({"translate": f"book.guhs.bieb.{bid}.title", "italic": False, "color": "#F7B6CB"}),
        "minecraft:lore": ms.NbtList(8, [json.dumps({"translate": "item.guhs.bieb_boek.lore", "with": [str(i + 1), str(len(BOOKS))],
                                                     "color": "gray", "italic": False})]),
        "minecraft:enchantment_glint_override": B(0),
        "minecraft:custom_data": {"GuhsBoek": bid}}}}


def reading_room(s, h):
    """The reading room: a row of lecterns on the atrium floor between the mosaic and the desk, each with one guh book lying
    open on it (all but the secret one), a pink carpet to stand on in front of each and a lamp at either end."""
    mc = h.mc
    books = on_lecterns()
    x0 = CX - (len(books) - 1)
    for k, i in enumerate(books):
        x = x0 + 2 * k
        s.set(x, G0 + 1, LECTERN_Z, mc("lectern"), {"facing": "south", "has_book": "true", "powered": "false"}, lectern_book(i))
        s.set(x, G0 + 1, LECTERN_Z + 1, mc("pink_carpet"))
    for x in (x0 - 2, x0 + 2 * len(books)):
        s.set(x, G0 + 1, LECTERN_Z, "guhs:guh_kristal_lamp")


def glow_tile(x, z):
    return x % 6 == 3 and z % 6 == 3


def in_atrium(x, z):
    return AX0 <= x <= AX1 and AZ0 <= z <= AZ1


def wall_block(mc, i, y):
    if y in (G1, G2):
        return mc("pink_concrete")                             # bands at the floors
    if y == ROOF:
        return mc("chiseled_quartz_block")
    if i % 6 == 0:
        return mc("quartz_pillar")                             # pilasters
    return mc("quartz_block") if y > G0 else mc("pink_terracotta")


def lamp_post(s, mc, x, z):
    for y in (1, 2, 3):
        s.set(x, y, z, mc("cherry_fence"))
    s.set(x, 4, z, mc("gold_block"))
    s.set(x, 5, z, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})


def guh_face_mask(w, h):
    """A guh face drawn on a w x h grid (x right, y up): {(x, y): part} with parts head, ear, inner_ear, eye_white, iris,
    pupil, cheek, nose, mouth. The head is a wide oval, the round ears stick out at the top."""
    parts = {}
    cx, hy = (w - 1) / 2, h * 0.42
    rx, ry = w / 2 - 0.5, h * 0.40
    for x in range(w):
        for y in range(h):
            if ((x - cx) / rx) ** 2 + ((y - hy) / ry) ** 2 <= 1:
                parts[(x, y)] = "head"
    er = w * 0.14
    for ex in (cx - w * 0.30, cx + w * 0.30):
        ey = h - er - 0.5
        for x in range(w):
            for y in range(h):
                d = math.hypot(x - ex, y - ey)
                if d <= er and (x, y) not in parts:
                    parts[(x, y)] = "ear"
                if d <= er * 0.55:
                    parts[(x, y)] = "inner_ear"
    eye_r = w * 0.11
    for sx in (-1, 1):
        ex, ey = cx + sx * w * 0.21, hy + h * 0.06
        for x in range(w):
            for y in range(h):
                d = math.hypot(x - ex, (y - ey) * 0.9)
                if d <= eye_r:
                    parts[(x, y)] = "pupil" if d <= eye_r * 0.35 else "iris" if d <= eye_r * 0.7 else "eye_white"
        for x in range(w):                                     # rosy cheeks under the eyes
            for y in range(h):
                if math.hypot(x - (cx + sx * w * 0.30), y - (hy - h * 0.14)) <= w * 0.06 and parts.get((x, y)) == "head":
                    parts[(x, y)] = "cheek"
    for x in range(w):
        for y in range(h):
            if abs(x - cx) <= max(0.6, w * 0.03) and abs(y - (hy - h * 0.06)) <= 0.6:
                parts[(x, y)] = "nose"
            # a little 'w' mouth (the middle and the ends go up)
            dx = abs(x - cx)
            lift = 0.9 if dx < w * 0.04 or dx > w * 0.10 else 0
            if parts.get((x, y)) == "head" and dx <= w * 0.14 and abs(y - (hy - h * 0.18 + lift)) <= 0.5:
                parts[(x, y)] = "mouth"
    return parts


def window_face(s, mc):
    """The big stained-glass window: a guh face in the north wall, from just above the floor to under the roof."""
    colours = {"head": "pink_stained_glass", "ear": "pink_stained_glass", "inner_ear": "magenta_stained_glass",
               "eye_white": "white_stained_glass", "iris": "light_blue_stained_glass", "pupil": "black_stained_glass",
               "cheek": "red_stained_glass", "nose": "magenta_stained_glass", "mouth": "purple_stained_glass"}
    w, hgt = 27, 19
    x0, y0 = CX - w // 2, G0 + 2
    parts = guh_face_mask(w, hgt)
    for (x, y), part in parts.items():
        s.set(x0 + x, y0 + y, BZ0, mc(colours[part]))
    # a golden frame round the face
    for (x, y) in parts:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in parts and 0 <= y0 + y + dy < ROOF:
                p = (x0 + x + dx, y0 + y + dy, BZ0)
                if s.get(*p) not in (None,) and "glass" not in s.get(*p):
                    s.set(*p, mc("gold_block"))


def facade_face(s, mc):
    """Above the entrance, on the outside of the south wall: a guh face in concrete (ears on the roof edge)."""
    colours = {"head": "pink_concrete", "ear": "pink_concrete", "inner_ear": "magenta_concrete", "eye_white": "white_concrete",
               "iris": "light_blue_concrete", "pupil": "black_concrete", "cheek": "red_concrete", "nose": "magenta_concrete",
               "mouth": "purple_concrete"}
    w, hgt = 15, 13
    x0, y0 = CX - w // 2, G1 + 3
    for (x, y), part in guh_face_mask(w, hgt).items():
        y_ = y0 + y
        if y_ <= ROOF + 3:
            s.set(x0 + x, y_, BZ1, mc(colours[part]))


def fountain(s, h, cx, cz):
    """A fountain in the front garden, shaped like a guh face seen from above: a round basin with two ear basins, lily
    pad eyes and a little guh statue spouting water in the middle."""
    mc = h.mc
    parts = guh_face_mask(15, 13)
    for (x, y), part in parts.items():
        wx, wz = cx - 7 + x, cz + 6 - y
        s.set(wx, 0, wz, mc("smooth_quartz"))
        s.set(wx, 1, wz, mc("water"), {"level": "0"})
        if part in ("pupil", "iris"):
            s.set(wx, 0, wz, mc("light_blue_concrete"))
        if part == "pupil":
            s.set(wx, 2, wz, "guhs:guh_waterlelie")
    for (x, y) in parts:                                       # the rim
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            if (x + dx, y + dy) not in parts:
                wx, wz = cx - 7 + x + dx, cz + 6 - y - dy
                s.set(wx, 0, wz, mc("smooth_quartz"))
                s.set(wx, 1, wz, mc("quartz_slab"), {"type": "bottom", "waterlogged": "false"})
    # the path goes round the fountain
    for x in range(cx - 10, cx + 11):
        for z in range(cz - 9, cz + 10):
            d = math.hypot(x - cx, (z - cz) * 1.1)
            if 8.5 <= d <= 11 and s.get(x, 1, z) is None:
                s.set(x, 0, z, mc("pink_concrete_powder"))
    for y in range(1, 4):
        s.set(cx, y, cz, mc("pink_concrete"))
    s.set(cx, 4, cz, mc("pink_wool"))                          # the statue: a round guh head with ears
    s.set(cx - 1, 5, cz, mc("pink_wool")); s.set(cx + 1, 5, cz, mc("pink_wool"))
    s.set(cx, 5, cz, mc("water"), {"level": "0"})


def dome(s, mc):
    """A glass dome over the middle of the atrium, with white ribs, and two pink guh ears on top."""
    cz, r = 32, 10
    for x in range(CX - r - 1, CX + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            for y in range(ROOF, ROOF + r + 2):
                d = math.dist((x, y, z), (CX, ROOF, cz))
                if r - 1.1 <= d <= r + 0.4:
                    rib = x == CX or z == cz or abs(x - CX) == abs(z - cz)
                    s.set(x, y, z, mc("white_concrete") if rib else mc("pink_stained_glass"))
                elif d < r - 1.1 and y > ROOF:
                    s.set(x, y, z, mc("air"))
    for x in range(CX - r + 1, CX + r):                        # the dome opens up the flat glass roof under it
        for z in range(cz - r + 1, cz + r):
            if math.dist((x, ROOF, z), (CX, ROOF, cz)) < r - 1.1:
                s.set(x, ROOF, z, mc("air"))
    for ex in (CX - 5, CX + 5):                                # guh ears (a round-ish shape standing on the dome)
        top = max(y for y in range(ROOF, ROOF + r + 2) if s.get(ex, y, cz) not in (None, mc("air")))
        for dy in range(0, 5):
            for dx in (-2, -1, 0, 1, 2):
                if abs(dx) + max(0, dy - 2) <= 2 or (dy <= 2 and abs(dx) <= 2):
                    if not (dy == 4 and abs(dx) == 2):
                        s.set(ex + dx, top + dy, cz, mc("magenta_concrete") if abs(dx) <= 1 and 1 <= dy <= 3 else mc("pink_concrete"))
    s.set(CX, ROOF + r + 1, cz, mc("gold_block"))
    # and a face on the south side of the dome: the dome is a giant guh peeking over the roof
    for sx in (-1, 1):
        ex, ey = CX + sx * 4, ROOF + 5
        for x in range(ex - 2, ex + 3):
            for y in range(ey - 2, ey + 3):
                d = math.hypot(x - ex, y - ey)
                if d > 2.3:
                    continue
                z = max((z for z in range(cz, cz + r + 2) if s.get(x, y, z) not in (None, mc("air"))), default=None)
                if z is not None:
                    s.set(x, y, z, mc("black_concrete") if d < 0.8 else mc("light_blue_concrete") if d < 1.6 else mc("white_concrete"))
        cx_, cy_ = CX + sx * 6, ROOF + 2                        # rosy cheeks
        z = max((z for z in range(cz, cz + r + 2) if s.get(cx_, cy_, z) not in (None, mc("air"))), default=None)
        if z is not None:
            s.set(cx_, cy_, z, mc("red_concrete"))


def turret(s, mc, cx, cz):
    """A round corner tower (solid) with a pink pointed roof and a golden tip."""
    r = 3
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            inside = BX0 < x < BX1 and BZ0 < z < BZ1
            if math.hypot(x - cx, z - cz) <= r + 0.3 and 0 <= x < W and 0 <= z < D and not inside:
                for y in range(0, ROOF + 4):
                    s.set(x, y, z, mc("quartz_block") if y % 7 else mc("pink_concrete"))
    for i in range(0, 7):
        rr = r + 0.8 - i * 0.65
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                if math.hypot(x - cx, z - cz) <= rr and not (BX0 < x < BX1 and BZ0 < z < BZ1):
                    s.set(x, ROOF + 4 + i, z, mc("pink_terracotta") if i % 2 else mc("magenta_terracotta"))
    s.set(cx, ROOF + 10, cz, mc("gold_block"))


def stairs(s, mc, x0, y0, z_hi):
    """A straight 2-wide staircase going north: 7 steps from floor y0 up to the gallery 7 higher, between two solid side
    walls with a fence on top, and an opening in the gallery floor above it (with a railing round it)."""
    for k in range(1, 8):
        z = z_hi - k
        for x in (x0, x0 + 1):
            if k < 7:
                s.set(x, y0 + k, z, mc("cherry_stairs"), {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            else:
                s.set(x, y0 + k, z, mc("stripped_cherry_wood"), {"axis": "y"})
            for y in range(y0 + 1, y0 + k):
                s.set(x, y, z, mc("cherry_planks"))
        if k < 7:
            for x in (x0 - 1, x0 + 2):                         # the side walls, with a railing on top
                for y in range(y0 + 1, y0 + k + 1):
                    s.set(x, y, z, mc("cherry_planks"))
                s.set(x, y0 + k + 1, z, mc("cherry_fence"))
    for z in range(z_hi - 6, z_hi):                            # the opening in the floor above
        for x in (x0, x0 + 1):
            s.set(x, y0 + 7, z, mc("air"))
            s.set(x, y0 + 8, z, mc("air"))
    for z in range(z_hi - 6, z_hi + 1):                        # railing round the opening upstairs
        for x in (x0 - 1, x0 + 2):
            s.set(x, y0 + 8, z, mc("cherry_fence"))
    for x in (x0, x0 + 1):
        s.set(x, y0 + 8, z_hi, mc("cherry_fence"))


def shelf_block(rng, mc):
    r = rng.random()
    if r < 0.15:
        occupied = {f"slot_{i}_occupied": str(rng.random() < 0.7).lower() for i in range(6)}
        return mc("chiseled_bookshelf"), {"facing": "north", **occupied}
    return mc("bookshelf"), None


def shelves(s, h, rng):
    mc = h.mc
    for fy in (G0, G1, G2):
        top = fy + 5
        # along the outer walls, between the windows
        for x in range(BX0 + 1, BX1):
            for z in (BZ0 + 1, BZ1 - 1):
                if (z == BZ1 - 1 and abs(x - CX) <= 8) or (z == BZ0 + 1 and AX0 <= x <= AX1):
                    continue
                window = any("glass" in (s.get(x, y, BZ0 if z == BZ0 + 1 else BZ1) or "") for y in range(fy + 1, top + 1))
                for y in range(fy + 1, top):
                    if s.get(x, y, z) == mc("air") and not window:
                        b, p = shelf_block(rng, mc)
                        s.set(x, y, z, b, facing(p, "south" if z == BZ0 + 1 else "north"))
        for z in range(BZ0 + 1, BZ1):
            for x in (BX0 + 1, BX1 - 1):
                window = any("glass" in (s.get(BX0 if x == BX0 + 1 else BX1, y, z) or "") for y in range(fy + 1, top + 1))
                for y in range(fy + 1, top):
                    if s.get(x, y, z) == mc("air") and not window:
                        b, p = shelf_block(rng, mc)
                        s.set(x, y, z, b, facing(p, "east" if x == BX0 + 1 else "west"))
        # rows of bookshelves in the side wings (east-west rows with aisles), 3 high
        for (x0, x1) in ((15, 22), (65, 72)):
            for z in range(24, 50, 4):
                for x in range(x0, x1 + 1):
                    for y in range(fy + 1, fy + 4):
                        if s.get(x, y, z) == mc("air"):
                            b, p = shelf_block(rng, mc)
                            s.set(x, y, z, b, facing(p, "south"))
                    s.set(x, fy + 4, z, mc("cherry_slab"), {"type": "bottom", "waterlogged": "false"})
                s.set(x0 + (x1 - x0) // 2, fy + 5, z, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})


def facing(props, f):
    if props is None:
        return None
    return dict(props, facing=f)


def desk(s, mc):
    """The lending desk: a U of cherry and quartz round the Bibliothecaris, with a bell, books and a guh cake."""
    nx, ny, nz = NPC
    for x in range(nx - 3, nx + 4):
        s.set(x, ny, nz + 2, mc("cherry_planks"))
        s.set(x, ny + 1, nz + 2, mc("smooth_quartz_slab"), {"type": "bottom", "waterlogged": "false"})
    for z in (nz, nz + 1):
        for x in (nx - 3, nx + 3):
            s.set(x, ny, z, mc("cherry_planks"))
            s.set(x, ny + 1, z, mc("smooth_quartz_slab"), {"type": "bottom", "waterlogged": "false"})
    s.set(nx - 2, ny + 2, nz + 2, mc("bell"), {"attachment": "floor", "facing": "south", "powered": "false"})
    s.set(nx + 2, ny + 2, nz + 2, "guhs:guh_taart", {"bites": "0"})
    s.set(nx + 1, ny + 2, nz + 2, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    s.set(nx - 3, ny + 2, nz, mc("lectern"), {"facing": "east", "has_book": "false", "powered": "false"})
    for x in (nx - 1, nx, nx + 1):                             # a shelf behind the Bibliothecaris
        for y in (ny, ny + 1, ny + 2):
            s.set(x, y, nz - 2, mc("bookshelf"))
    s.set(nx, ny + 3, nz - 2, mc("gold_block"))
    s.set(nx, ny - 1, nz, mc("gold_block"))                    # (the spot the Bibliothecaris sits on)


def mosaic(s, mc, cx, cz):
    """The floor mosaic in the atrium: a big guh face in concrete, looking at the door."""
    colours = {"head": "pink_concrete", "ear": "pink_concrete", "inner_ear": "magenta_concrete", "eye_white": "white_concrete",
               "iris": "light_blue_concrete", "pupil": "black_concrete", "cheek": "red_concrete", "nose": "magenta_concrete",
               "mouth": "purple_concrete"}
    w, hgt = 19, 17
    parts = guh_face_mask(w, hgt)
    for (x, y), part in parts.items():
        s.set(cx - w // 2 + x, G0, cz + hgt // 2 - y, mc(colours[part]))
    for (x, y) in parts:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in parts:
                fx, fz = cx - w // 2 + x + dx, cz + hgt // 2 - y - dy
                if (fx + fz) % 3 == 0:
                    s.set(fx, G0, fz, mc("ochre_froglight"), {"axis": "y"})   # a glowing golden frame
                else:
                    s.set(fx, G0, fz, mc("gold_block"))


def globe(s, mc, rng, cx, cz):
    """A globe of the Guhmension: pink guh fields, yellow cheese flats, white peaks, a blue guh sea and dark Mika land,
    on a golden stand, with a golden ring round it."""
    cy, r = G0 + 7, 3.6
    for y in range(G0 + 1, G0 + 4):
        s.set(cx, y, cz, mc("gold_block"))
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            s.set(x, G0 + 1, z, mc("quartz_block"))
    lands = ["pink_wool", "pink_wool", "yellow_wool", "white_wool", "magenta_wool", "light_blue_concrete", "light_blue_concrete",
             "light_blue_concrete", "black_wool"]
    seeds = [(rng.uniform(-1, 1), rng.uniform(-1, 1), rng.uniform(-1, 1), rng.choice(lands)) for _ in range(16)]
    for x in range(cx - 5, cx + 6):
        for y in range(G0 + 2, G0 + 13):
            for z in range(cz - 5, cz + 6):
                d = math.dist((x, y, z), (cx, cy, cz))
                if d <= r + 0.2:
                    v = [(x - cx) / r, (y - cy) / r, (z - cz) / r]
                    best = max(seeds, key=lambda sd: sd[0] * v[0] + sd[1] * v[1] + sd[2] * v[2])
                    s.set(x, y, z, mc(best[3]))
    # a golden ring (the meridian) round it, standing on the golden column
    for y in range(cy - 6, cy + 7):
        for z in range(cz - 6, cz + 7):
            if 4.4 <= math.hypot(y - cy, z - cz) <= 5.6:
                s.set(cx, y, z, mc("gold_block"))
    s.set(cx, G0 + 4, cz, mc("gold_block"))


def chandeliers(s, mc):
    """Chains from the glass roof with a ring of lampgions: light for the whole atrium."""
    for (x, z) in [(CX - 9, 23), (CX + 9, 23), (CX - 9, 42), (CX + 9, 42), (CX, 32)]:
        top = ROOF - 1
        if s.get(x, ROOF, z) == mc("air"):                     # under the dome: hang from the dome
            top = max(y for y in range(ROOF, ROOF + 12) if s.get(x, y, z) == mc("air"))
        bottom = G1 + 4
        for y in range(bottom, top + 1):
            s.set(x, y, z, mc("chain"), {"axis": "y", "waterlogged": "false"})
        s.set(x, bottom - 1, z, mc("gold_block"))
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            s.set(x + dx, bottom - 1, z + dz, mc("gold_block"))
            s.set(x + dx, bottom - 2, z + dz, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
        s.set(x, bottom - 2, z, "guhs:guh_kristal_lamp")


def nook(s, mc, rng, x0, fy, z0, x1, z1):
    """A reading nook: a rug, two guh armchairs facing each other over a little table with a lamp, and a stack of books."""
    colour = rng.choice(["pink", "magenta", "white", "light_blue", "yellow"])
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if s.get(x, fy + 1, z) == mc("air"):
                s.set(x, fy + 1, z, mc(f"{colour}_carpet"))
    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
    s.set(mx - 1, fy + 1, mz, "guhs:bieb_guhfauteuil", {"facing": "east"})
    s.set(mx + 1, fy + 1, mz, "guhs:bieb_guhfauteuil", {"facing": "west"})
    s.set(mx, fy + 1, mz, "guhs:guh_tafel", {"facing": "north"})
    s.set(mx, fy + 2, mz, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    s.set(mx, fy + 1, mz + 2, rng.choice(["guhs:pink_zitzak", "guhs:white_zitzak", "guhs:magenta_zitzak"]), {"facing": "north"})
    s.set(mx - 2, fy + 1, mz - 2, rng.choice(["guhs:pink_kussen", "guhs:yellow_kussen"]), {"facing": "south"})


def balcony(s, mc, edge_x, out):
    """A half-round balcony on gallery 1 sticking out into the atrium, with a guh armchair and a railing."""
    cz = 24
    for x in range(edge_x, edge_x + out * 5, out):
        for z in range(cz - 5, cz + 6):
            d = math.hypot(x - edge_x, (z - cz) * 1.0)
            if d <= 4.6:
                s.set(x, G1, z, mc("stripped_cherry_wood"), {"axis": "y"})
                s.set(x, G1 + 1, z, mc("air"))
                if d > 3.6:
                    s.set(x, G1 + 1, z, mc("white_stained_glass_pane"))
    for z in range(cz - 2, cz + 3):                            # open to the gallery
        s.set(edge_x - out, G1 + 1, z, mc("air"))
    s.set(edge_x + out, G1 + 1, cz, "guhs:bieb_guhfauteuil", {"facing": "east" if out > 0 else "west"})
    s.set(edge_x + 2 * out, G1 + 1, cz - 1, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    s.set(edge_x + out, G1 - 1, cz, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})


def front_balcony(s, mc):
    """A balcony above the door (outside, gallery 1), with a door from the south gallery."""
    for x in range(CX - 7, CX + 8):
        for z in range(BZ1 + 1, BZ1 + 4):
            s.set(x, G1, z, mc("smooth_quartz"))
            s.set(x, G1 + 1, z, mc("air"))
            if z == BZ1 + 3 or abs(x - CX) == 7:
                s.set(x, G1 + 1, z, mc("quartz_slab") if (x + z) % 2 else mc("gold_block"), {"type": "bottom", "waterlogged": "false"} if (x + z) % 2 else None)
        s.set(x, G1 - 1, BZ1 + 3, mc("quartz_stairs"), {"facing": "south", "half": "top", "shape": "straight", "waterlogged": "false"})
    # railing: a solid parapet 1.5 blocks high is safer: slabs on quartz
    for x in range(CX - 7, CX + 8):
        for z in range(BZ1 + 1, BZ1 + 4):
            if z == BZ1 + 3 or abs(x - CX) == 7:
                s.set(x, G1 + 1, z, mc("quartz_block"))
                s.set(x, G1 + 2, z, mc("quartz_slab"), {"type": "bottom", "waterlogged": "false"})
    for dx in (-5, 5):
        s.set(CX + dx, G1 + 3, BZ1 + 3, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    for x in (CX - 6, CX + 6):                                 # doors in the wall (either side of the guh face)
        for y in (G1 + 1, G1 + 2):
            s.set(x, y, BZ1, mc("air"))
        for y in (G1 + 1, G1 + 2):
            s.set(x, y, BZ1 - 1, mc("air"))
    s.set(CX - 3, G1 + 1, BZ1 + 2, "guhs:bieb_guhfauteuil", {"facing": "south"})
    s.set(CX + 3, G1 + 1, BZ1 + 2, "guhs:bieb_guhfauteuil", {"facing": "south"})


def secret_room(s, h, mc):
    x0, x1, z0, z1 = SECRET_ROOM
    fy = G1
    # walls of bookshelves (the room is in the corner: the outer walls are there already)
    for y in range(fy + 1, G2):
        for z in range(z0, z1 + 2):
            s.set(x0 - 1, y, z, mc("bookshelf"))
        for x in range(x0 - 1, x1 + 1):
            s.set(x, y, z1 + 1, mc("bookshelf"))
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(fy + 1, G2):
                s.set(x, y, z, mc("air"))
            s.set(x, fy, z, mc("purple_concrete") if (x + z) % 2 else mc("magenta_concrete"))
            s.set(x, fy + 1, z, mc("purple_carpet") if (x + z) % 2 else mc("magenta_carpet"))
    # the door: two secret bookcases in the south wall
    dx, dz = DOOR
    for y in (fy + 1, fy + 2):
        s.set(dx, y, dz, "guhs:bieb_geheime_kast", {"facing": "south", "open": "false"})
    s.set(dx, fy + 1, dz - 1, mc("air"))
    s.set(dx, fy + 1, dz + 1, mc("air")) if s.get(dx, fy + 1, dz + 1) in (None, mc("air")) else None
    # inside: the stand with the secret book in the middle, candles, a guh armchair, crystal lamps
    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2 - 1
    s.set(mx, fy + 1, mz, "guhs:bieb_boekaltaar", {"facing": "south"})
    for (x, z) in [(x0, z0), (x1, z0)]:
        s.set(x, fy + 1, z, "guhs:guh_kristal_lamp")
    s.set(x0, fy + 1, z1, "guhs:bieb_guhfauteuil", {"facing": "east"})
    s.set(x1, fy + 1, z1 - 2, "guhs:mikatrofee", {"facing": "west"})
    s.set(mx - 2, fy + 1, mz, mc("candle"), {"candles": "3", "lit": "true", "waterlogged": "false"})
    s.set(mx + 2, fy + 1, mz, mc("candle"), {"candles": "2", "lit": "true", "waterlogged": "false"})
    s.set(mx, G2 - 1, mz, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
    s.set(x1 - 1, G2 - 1, z1, "guhs:lampion_mint", {"hanging": "true", "waterlogged": "false"})
    s.set(x1 - 1, G2 - 1, z1 + 2, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})   # (outside, by the door)


def fix_connections(s, mc):
    """Fences and panes connect to their neighbours (fences, panes, glass, full blocks)."""
    thin = ("fence", "pane")
    not_solid = ("air", "carpet", "lampion", "stairs", "slab", "chain", "candle", "flower", "tulip", "allium", "guhbloem",
                 "knabbelroos", "kaasbloem", "guhoortjes", "water", "tafel", "stoel", "fauteuil", "zitzak", "kussen", "lectern",
                 "cartography", "bell", "taart", "altaar", "kristal_lamp", "leaves", "waterlelie", "chest", "bank", "trofee",
                 "geheime_kast")
    for (x, y, z), (name, props, nbt) in list(s.blocks.items()):
        if not any(t in name for t in thin):
            continue
        p = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
        for side, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
            n = s.get(x + dx, y, z + dz)
            if n is None:
                continue
            if any(t in n for t in thin) or not any(t in n for t in not_solid):
                if "fence" in name and "pane" in n or "pane" in name and "fence" in n:
                    continue
                p[side] = "true"
        s.blocks[(x, y, z)] = (name, p, nbt)


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
PASSABLE = ("minecraft:air", "carpet", "flower", "tulip", "allium", "guhbloem", "knabbelroos", "kaasbloem", "guhoortjes", "water",
            "lampion", "candle", "waterlelie", "chain")
LIGHT = {"froglight": 15, "lampion_roze": 15, "lampion_geel": 15, "lampion_mint": 15, "guh_kristal_lamp": 15, "candle": 9}


def passable(name):
    return name is None or any(t in name for t in PASSABLE)


def check_structure(s, h):
    """Checks the template: the entrance leads everywhere that matters (galleries, the Bibliothecaris, the secret room,
    the balconies), no walkable spot has an unprotected drop of more than 3 blocks inside the building, the NPC sits on
    something solid, nothing floats, and every walkable spot inside is lit (at least 6; nothing spawns there anyway)."""
    mc = h.mc
    problems = []
    get = s.get

    def standable(x, y, z):
        below, feet, head = get(x, y - 1, z), get(x, y, z), get(x, y + 1, z)
        if not (passable(feet) and passable(head)) or "water" in (feet or "") or "water" in (head or ""):
            return False
        return not passable(below) and not any(t in below for t in ("fence", "pane", "wall"))

    def is_door(x, y, z):
        return "geheime_kast" in (get(x, y, z) or "")

    # walk from the garden gate
    start = (CX, 1, D - 1)
    seen, todo = {start}, [start]
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if not (0 <= nx < W and 0 <= nz < D):
                continue
            for dy in (0, 1, -1, -2, -3):
                ny = y + dy
                if dy == 1 and not passable(get(x, y + 2, z)):
                    continue
                door = is_door(nx, ny, nz) and is_door(nx, ny + 1, nz)
                if (standable(nx, ny, nz) or door) and (nx, ny, nz) not in seen:
                    if dy < 0 and not all(passable(get(nx, yy, nz)) for yy in range(ny + 1, y + 2)):
                        continue
                    seen.add((nx, ny, nz))
                    todo.append((nx, ny, nz))
                    break
    s.seen = seen
    must = {"the Bibliothecaris' desk (front)": (NPC[0], NPC[1], NPC[2] + 3),
            "gallery 1 (west)": (16, G1 + 1, 30), "gallery 1 (east)": (70, G1 + 1, 30),
            "gallery 2 (west)": (16, G2 + 1, 30), "gallery 2 (east)": (70, G2 + 1, 30),
            "gallery 2 (south, archive)": (31, G2 + 1, 58), "the front balcony": (CX, G1 + 1, BZ1 + 2),
            "the west balcony": (AX0 + 2, G1 + 1, 24), "the east balcony": (AX1 - 2, G1 + 1, 24),
            "the globe": (CX, G0 + 1, 25), "the secret room": (SECRET_ROOM[0] + 1, G1 + 1, SECRET_ROOM[2] + 2)}
    for what, p in must.items():
        if p not in seen:
            problems.append(f"not reachable: {what} {p}")
    # the book stand in the secret room: reachable next to it
    ax = [(x, y, z) for (x, y, z), b in s.blocks.items() if b[0] == "guhs:bieb_boekaltaar"]
    if len(ax) != 1 or not any((ax[0][0] + dx, ax[0][1], ax[0][2] + dz) in seen for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
        problems.append("the secret book stand is missing or can't be reached")
    # the reading room: 11 lecterns with a guh book, each with a reachable spot in front of it
    lecterns = [(p, b) for p, b in s.blocks.items() if b[0] == "minecraft:lectern" and b[2]]
    if len(lecterns) != len(on_lecterns()) or len({b[2]["Book"]["components"]["minecraft:custom_data"]["GuhsBoek"] for _p, b in lecterns}) != len(on_lecterns()):
        problems.append(f"expected {len(on_lecterns())} lecterns with a different guh book each, found {len(lecterns)}")
    for (x, y, z), b in lecterns:
        if (x, y, z + 1) not in seen:
            problems.append(f"can't stand in front of the book lectern at {(x, y, z)}")
    # drops inside the building
    for (x, y, z) in seen:
        if not (BX0 <= x <= BX1 and BZ0 <= z <= BZ1) or is_door(x, y, z):
            continue
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if not (passable(get(nx, y, nz)) and passable(get(nx, y + 1, nz))):
                continue
            drop = 0
            while drop < 12 and passable(get(nx, y - 1 - drop, nz)) and y - 1 - drop > 0:
                drop += 1
            if drop > 3:
                problems.append(f"an unprotected drop of {drop} next to {(x, y, z)} towards {(nx, nz)}")
    # holes in the floors: every cell of a gallery floor (outside the atrium and the stair openings) is solid
    for fy in (G0, G1, G2):
        for x in range(BX0 + 1, BX1):
            for z in range(BZ0 + 1, BZ1):
                if fy != G0 and in_atrium(x, z):
                    continue
                if passable(get(x, fy, z)):
                    below = get(x, fy - 1, z) or ""
                    if not any(t in below for t in ("stairs", "planks", "stripped")) and not any(
                            "stairs" in (get(x, yy, z) or "") for yy in range(fy - 7, fy)):
                        problems.append(f"hole in floor y={fy} at {(x, z)}")
    # the NPC sits on something solid, in the air
    nx, ny, nz = NPC
    if passable(get(nx, ny - 1, nz)) or not passable(get(nx, ny, nz)) or not passable(get(nx, ny + 1, nz)):
        problems.append("the Bibliothecaris is not sitting on solid ground with room above")
    # nothing floats: every block is connected to the ground layer
    solid = {p for p, b in s.blocks.items() if b[0] != "minecraft:air"}
    grounded = {p for p in solid if p[1] == 0}
    todo = list(grounded)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in solid and n not in grounded:
                grounded.add(n)
                todo.append(n)
    floating = solid - grounded
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {sorted(floating)[:5]}")
    # light: a simple block light spread; every walkable spot inside the building gets at least 6
    light = {}
    q = []
    for p, b in s.blocks.items():
        for key, level in LIGHT.items():
            if b[0].endswith(key):
                light[p] = level
                q.append(p)
    head = 0
    while head < len(q):
        p = q[head]
        head += 1
        lv = light[p] - 1
        if lv <= 0:
            continue
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (p[0] + d[0], p[1] + d[1], p[2] + d[2])
            nb = get(*n)
            if not (0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < D):
                continue
            if not passable(nb) and not any(t in (nb or "") for t in ("glass", "fence", "slab", "stairs", "tafel", "stoel", "fauteuil",
                                                                     "zitzak", "kussen", "lectern", "bell", "taart", "chest", "altaar",
                                                                     "cartography", "trofee", "bank")):
                continue
            if light.get(n, 0) < lv:
                light[n] = lv
                q.append(n)
    s.light = light
    inside = [p for p in seen if BX0 < p[0] < BX1 and BZ0 < p[2] < BZ1]
    dark = [p for p in inside if light.get(p, 0) < 6 and not is_door(*p)]
    if dark:
        problems.append(f"{len(dark)} dark walkable spots inside (light < 6), e.g. {sorted(dark)[:6]}")
    print(f"guhbibliotheek light: {sum(1 for p in inside if light.get(p, 0) >= 8)} of {len(inside)} walkable spots inside have light 8+")
    print(f"guhbibliotheek self-check: {len(seen)} walkable spots reachable, {len(solid)} blocks, "
          f"{len(problems)} problem(s)")
    return problems


# =====================================================================================================================
# FTB quests
# =====================================================================================================================
def ftb(fq):
    q, item, adv, structure = fq.q, fq.item, fq.adv, fq.structure
    y = 44
    q("bieb_vind", "Ssssst!", "Ergens in de Guhmensie staat de zeer zeldzame &dguhbibliotheek&r: drie verdiepingen vol boeken onder een glazen koepel met guhoren. Het &6Guhmensie-superkompas&r (Wonderen > Guhbibliotheek) wijst de weg.",
      "minecraft:bookshelf", [structure("guhbibliotheek")], rewards=(("guhs:kaas_knabbels", 16),), x=-8, y=y, shape="octagon", xp=100)
    q("bieb_eerste_boek", "Lezen is vadsig", "Alle guhboeken liggen open op de &dlessenaars&r in de leeszaal: lees ze meteen! Van elk boek mag je &6één keer&r een exemplaar meenemen (knop &eExemplaar meenemen&r). Neem je eerste guhboek mee.",
      "minecraft:written_book", [adv("bieb_eerste_boek")], x=-6, y=y)
    q("bieb_zes_boeken", "Halve boekenkast", "Verzamel 6 verschillende guhboeken van de lessenaars. Ook in de archiefkisten boven liggen er soms een paar!",
      "minecraft:book", [adv("bieb_zes_boeken")], rewards=(("guhs:boekenbon", 2),), x=-4, y=y)
    q("bieb_geheim", "Achter de boekenkast", "Ergens op de eerste verdieping steekt een roze boek een beetje uit een boekenkast... Vind de geheime kamer en het &5Geheime Guhboek&r.",
      "guhs:bieb_geheime_kast", [adv("bieb_geheim")], rewards=(("guhs:gefrituurde_kaasknabbels", 6),), x=-2, y=y, shape="diamond", xp=200)
    q("bieb_alle_boeken", "Boekenwurmguh", f"Verzamel alle {len(BOOKS)} guhboeken, het geheime ook (en het dagboek uit de Stille Voorraadkelder). VAHOEG!",
      "minecraft:enchanted_book", [adv("bieb_alle_boeken")], rewards=(("guhs:boekenbon", 4), ("guhs:bieb_guhfauteuil", 2)), x=0, y=y,
      shape="gear", xp=500)
    q("bieb_kwis", "Guhkwis", "Beantwoord een kwisvraag van de Bibliothecaris goed. Hij vraagt alleen over boeken die jij gelezen of verzameld hebt, dus lees ze goed! Elk goed antwoord (de eerste keer) = een &6boekenbon&r.",
      "guhs:boekenbon", [adv("bieb_kwis")], x=2, y=y)
    q("bieb_kwismeester", "Guhkwismeester", f"Beantwoord alle {2 * len(BOOKS)} kwisvragen goed.", "guhs:boekenbon", [adv("bieb_kwismeester")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 12),), x=4, y=y, shape="gear", xp=400)
    q("bieb_pakje", "Professor Guh", "Koop het geleerdenpakje voor je guh bij de Bibliothecaris (alleen daar te koop!): het geleerdenbrilletje, het boekenwurmvest en de geleerdenbaret, met boekenbonnen.",
      "guhs:bieb_hoed", [item("guhs:bieb_leesbril"), item("guhs:bieb_vest"), item("guhs:bieb_hoed")], x=6, y=y, shape="diamond", xp=300)
