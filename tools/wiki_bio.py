"""
The wiki part of the update "biomes3": the three new Guhmension biomes (Blossom Lake, Babbledale, the Cloud Meadow), the
Weeb House, the new block sets and the Super Compass tab Biomes. One section ("biomes3") of the one-page wiki.

body(w) returns the HTML, built with the helpers of tools/make_wiki.py (w.p, w.ul, w.table, w.entry, w.img, w.h3); every
text is written twice, (English, Dutch), the English by hand following tools/lang/GLOSSARY.md. The pictures
docs/wiki/img/shot133_*.png come from the slices' own screenshots (no renders of their own). The wiki site puts every
entry on its own page: tools/wiki_site/topics.py (CHUNK_RULES "biomes3").
"""


def body(w):
    p, ul, table, entry, h3 = w.p, w.ul, w.table, w.entry, w.h3
    shot = lambda naam, alt: w.img("shot133_" + naam, alt, "shot")
    intro = p("Three new biomes lie far out in the <b>Guhmension</b>, in chunks nobody has visited yet. The Super Compass tab <b>Biomes</b> "
              "points the way to each of them.",
              "Ver weg in de <b>Guhmensie</b> liggen drie nieuwe biomen, in chunks waar nog niemand geweest is. Het superkompas-tabblad "
              "<b>Biomes</b> wijst de weg naar elk ervan.")
    meer = entry(shot("biome_guhmension_bloesemmeertje", "Blossom Lake"), "Blossom Lake", "Het Bloesemmeertje", ul([
        ("<b>Looks like</b>: a calm lake with islands, guh blossom trees on the shore, reeds, and pink petals floating on the water.",
         "<b>Hoe het eruitziet</b>: een rustig meer met eilandjes, guhbloesembomen aan de oever, riet en roze blaadjes die op het water drijven."),
        ("<b>Lives here</b>: koi in five colors, frogguhs on lily pads (they plop away when you come close) and the <b>Blossom Guh</b>.",
         "<b>Wie er woont</b>: koi in vijf kleuren, kikkerguhs op lelies (ze plonzen weg als je dichtbij komt) en de <b>Bloesemguh</b>."),
        ("<b>Boathouse</b> (Super Compass: Cozy): the Fisher Guh gives a handful of koi food every day, lends you his rowboat and teaches you "
         "the lake in four little lessons. Reward: two jetty lanterns, a fishing rod on a stand and a koi windsock.",
         "<b>Botenhuisje</b> (superkompas: Knus): de visser-guh geeft elke dag een handje koivoer, leent je zijn roeibootje en leert je het "
         "meer kennen in vier lesjes. Beloning: twee steigerlantaarns, een hengel op een standaard en een koi-windzak."),
        ("<b>Picnic Island</b> (rare): guhs celebrating hanami under the biggest blossom tree. The picnic basket holds one treat for everyone.",
         "<b>Picknickeilandje</b> (zeldzaam): guhs die hanami vieren onder de grootste bloesemboom. In de picknickmand zit voor iedereen één "
         "keer iets lekkers."),
        ("<b>To take home</b>: a koi in a bucket of water (it keeps its color and stays in your pond), floating blossom petals, and lake silt "
         "from the bottom (smelt it into lake tiles).",
         "<b>Voor thuis</b>: een koi in een emmer water (hij houdt zijn kleur en blijft in je vijver), drijvende bloesemblaadjes, en meerslib "
         "van de bodem (smelt het tot meertegels).")]), wide=True)
    dal = entry(shot("biome_guhmension_klaterdal", "Babbledale"), "Babbledale", "Het Klaterdal", ul([
        ("<b>Looks like</b>: a terraced valley full of brooks and waterfalls, with red and orange maple trees, guh bamboo, bonsai and babble moss.",
         "<b>Hoe het eruitziet</b>: een dal in terrassen vol beekjes en watervallen, met rode en oranje esdoorns, guh-bamboe, bonsai en klatermos."),
        ("<b>Lives here</b>: the <b>Tanuki Guh</b>, koi in the ponds and frogguhs on the lily pads.",
         "<b>Wie er woont</b>: de <b>Tanukiguh</b>, koi in de vijvers en kikkerguhs op de lelies."),
        ("<b>To find</b>: pink torii gates with guh ears, a tea house by a tall waterfall (with the Tea Guh), an arched bridge, stone guh "
         "lanterns, a zen corner and stepping stones. Sometimes one <b>Weeb House</b> (see below).",
         "<b>Te vinden</b>: roze torii met guhoortjes, een theehuisje bij een hoge waterval (met de thee-guh), een boogbrug, stenen "
         "guh-lantaarns, een zenhoekje en staptreden. Soms één <b>weebhuisje</b> (zie hieronder)."),
        ("<b>To take home</b>: maple logs and saplings (logs make pink lacquerwood planks), guh bamboo and babble moss.",
         "<b>Voor thuis</b>: esdoornstammen en -zaailingen (van stammen maak je roze lakhoutplanken), guh-bamboe en klatermos.")]), wide=True)
    weide = entry(shot("biome_guhmension_wolkenweide", "The Cloud Meadow"), "The Cloud Meadow", "De Wolkenweide", ul([
        ("<b>Looks like</b>: a pale meadow under stacks of floating islets and clouds, with waterfalls over the rims. A <b>cloud lift</b> takes "
         "you up and a <b>cloud stream</b> back down (follow the little arrows). Cloud blocks are soft: you never land hard on them.",
         "<b>Hoe het eruitziet</b>: een bleke weide onder stapels zwevende eilandjes en wolken, met watervallen over de randen. Een "
         "<b>wolkenlift</b> brengt je omhoog en een <b>wolkenstroom</b> weer omlaag (volg de pijltjes). Wolkenblokken zijn zacht: je valt er nooit hard op."),
        ("<b>Lives here</b>: <b>Cloud Lambs</b> (snip them with shears for cloud fluff; feed nibble feed or use a lead to keep one) and wild "
         "<b>Cloud Guhs</b>.",
         "<b>Wie er woont</b>: <b>wolkenschaapjes</b> (knip ze met een schaar voor wolkenpluis; met knabbelvoer of een leidtouw blijft er een "
         "bij je) en wilde <b>Wolkguhs</b>."),
        ("<b>Cloud Shepherd's Hut</b> (rare, Cozy): four lessons from fluff to cloud blocks. Reward: fluff, a cloud lamp and a lamb of your own.",
         "<b>Wolkenhoeder-hut</b> (zeldzaam, Knus): vier lessen van pluis tot wolkenblok. Beloning: pluis, een wolkenlamp en een eigen schaapje."),
        ("<b>Balloon Harbor</b> (Cozy): one gentle balloon ride a day down to the meadow.",
         "<b>Luchtballon-haven</b> (Knus): één zachte ballonvaart per dag naar de weide beneden."),
        ("<b>Observatory Ruin</b> (Wonders): the stargazer guh is awake at night; the telescope gives a little stardust every night.",
         "<b>Sterrenwacht-ruïne</b> (Wonderen): de sterrenkijkerguh is 's nachts wakker; de telescoop geeft elke nacht een beetje sterrenstof."),
        ("<b>Rainbow Bridge</b> (very rare, Wonders): walk across; the pot at the end gives cheese nibbles and a chunk of rainbow every day.",
         "<b>Regenboogbrug</b> (heel zeldzaam, Wonderen): loop eroverheen; de pot aan het eind geeft elke dag kaasknabbels en een brokje regenboog."),
        ("<b>Cloud Castle</b> (Wonders): sneak past the sleeping Giant Guh (no running, no jumping) and take one <b>Golden Nibble Crumb</b> a day. "
         "If he wakes up he only sneezes you outside.",
         "<b>Wolkenkasteeltje</b> (Wonderen): sluip langs de slapende reuzenguh (niet rennen, niet springen) en pak één <b>gouden "
         "knabbelkruimel</b> per dag. Wordt hij wakker, dan niest hij je alleen maar naar buiten."),
        ("<b>Lightning Forge</b> (Wonders): the Smith Guh trades cloud fluff for a cloud bench, a cloud bed or a cloud lamp.",
         "<b>Bliksemsmidse</b> (Wonderen): de smid-guh ruilt wolkenpluis voor een wolkenbank, een wolkenbed of een wolkenlamp.")]), wide=True)
    weeb = entry(shot("structure_weebhuisje_buiten", "The Weeb House"), "The Weeb House", "Het weebhuisje", ul([
        ("A guh-Japanese little house in Babbledale with a hot-air balloon beside it (Super Compass: Cozy). <b>Evichonk</b> and "
         "<b>Nielschonk</b> live there. The house and its figurines cannot be broken.",
         "Een guh-Japans huisje in het Klaterdal met een luchtballon ernaast (superkompas: Knus). <b>Evivads</b> en <b>Nielsvads</b> wonen "
         "er. Het huisje en de figuurtjes kun je niet afbreken."),
        ("<b>The trip</b>: talk to them, and talk again. They leave for Japan by balloon and stay away for one guh day (a note hangs on the "
         "door). <b>Wave them off</b> when they leave.",
         "<b>De reis</b>: praat met ze, en nog een keer. Ze vertrekken met de ballon naar Japan en blijven een guhdag weg (er hangt een "
         "briefje op de deur). <b>Zwaai ze uit</b> als ze vertrekken."),
        ("<b>The presents</b>: when they are back, everyone who waved gets a present. You can do this as often as you like.",
         "<b>De cadeautjes</b>: als ze terug zijn, krijgt iedereen die zwaaide een cadeautje. Dat kan zo vaak als je wilt."),
        ("The <b>Japan Collection</b>, twelve decorations: Waving Lucky Guh, Paper Lantern, Mini Torii, Ramen Bowl, Daruma Guh, Fan, Tea Set, "
         "Kokeshi Guh, Koinobori Flag, Bonsai Dish, Wind Chime and Maneki Nibble.",
         "De <b>Japan-verzameling</b>, twaalf decoraties: zwaaiende geluksguh, papieren lampion, mini-torii, ramenkom, daruma-guh, waaier, "
         "theeservies, kokeshi-guh, koinobori-vlag, bonsai-schaaltje, windgong en maneki-knabbel."),
        ("Food for you and your guhs: Guh Sushi, Ramen, Mochi and Onigiri. Four outfits for your guh: Guh Kimono, Hachimaki, Cat Ears and "
         "Japanese Bow.",
         "Eten voor jou en je guhs: guh-sushi, ramen, mochi en onigiri. Vier pakjes voor je guh: guh-kimono, hachimaki, kattenoortjes en "
         "Japans strikje."),
        ("Got a double? Swap it with them for a piece you still miss. All twelve gives the title <b>Weeb</b>. The Guhdex keeps your collection.",
         "Een dubbele? Ruil hem bij hen voor een stuk dat je nog mist. Alle twaalf geeft de titel <b>Weeb</b>. Je verzameling staat in de Guhdex.")]),
        wide=True)
    rij = lambda en, nl, hoe_en, hoe_nl: [(f"<b>{en}</b>", f"<b>{nl}</b>"), (hoe_en, hoe_nl)]
    blokken = entry(shot("bio_blokken_kamer", "A room of shoji and tatami"), "The new block sets", "De nieuwe bouwblokken",
                    p("Everything below is made at a crafting table unless it says otherwise. Planks, tiles, stone and clouds also come as "
                      "<b>slab</b> and <b>stairs</b> (roof tiles and stone on the stonecutter too).",
                      "Alles hieronder maak je op een werkbank, tenzij er iets anders staat. Planken, dakpannen, steen en wolken zijn er ook als "
                      "<b>plaat</b> en <b>trap</b> (dakpannen en steen ook op de steenzaag).") + table(
        [("Block", "Blok"), ("How", "Hoe")], [
            rij("Pink Lacquerwood", "Roze lakhout", "1 maple log &rarr; 4 planks, or 8 planks of any wood around 1 pink dye. Also: fence, gate, door, trapdoor "
                "and beam (3 planks in a column &rarr; 3).", "1 esdoornstam &rarr; 4 planken, of 8 planken van welk hout ook rond 1 roze verf. Ook: hek, poort, "
                "deur, luik en balk (3 planken boven elkaar &rarr; 3)."),
            rij("Smooth Snugglestone", "Gladde knuffelsteen", "Smelt snugglestone.", "Smelt knuffelsteen."),
            rij("Guh Roof Tiles", "Guh-dakpannen", "4 smooth snugglestone (2&times;2) &rarr; 4 white; 8 tiles around pink, white or light gray dye recolors "
                "them. The <b>Roof Curl</b> (3 tiles in an L) curls up by itself on the end and the corner of a roof edge.",
                "4 gladde knuffelsteen (2&times;2) &rarr; 4 witte; 8 dakpannen rond roze, witte of lichtgrijze verf kleurt ze om. De <b>dakkrul</b> "
                "(3 dakpannen in een L) krult vanzelf omhoog aan het eind en op de hoek van een dakrand."),
            rij("Guh Bamboo", "Guh-bamboe", "Bamboo + pink dye (or from Babbledale).", "Bamboe + roze verf (of uit het Klaterdal)."),
            rij("Shoji Sliding Panel", "Shoji-schuifpaneel", "A column of paper between two columns of guh bamboo &rarr; 4. Click: it slides open.",
                "Een kolom papier tussen twee kolommen guh-bamboe &rarr; 4. Klik: het schuift open."),
            rij("Tatami Mat", "Tatamimat", "6 guh bamboo (2 rows) &rarr; 3. Two side by side become one mat.",
                "6 guh-bamboe (2 rijen) &rarr; 3. Twee naast elkaar worden één mat."),
            rij("Stone Guh Lantern", "Stenen guh-lantaarn", "Smooth snugglestone slab, torch, smooth snugglestone (top to bottom). Lights up by itself at dusk.",
                "Gladde knuffelsteenplaat, fakkel, gladde knuffelsteen (van boven naar beneden). Gaat vanzelf aan als het donker wordt."),
            rij("Raked Sand", "Geharkt zand", "2 sand &rarr; 2; rake it with a hoe for another pattern. One raked sand &harr; one with rings.",
                "2 zand &rarr; 2; hark met een schoffel voor een ander patroon. Eén geharkt zand &harr; één met ringen."),
            rij("Potted Bonsai", "Bonsai in een potje", "A sapling above a flower pot. Snip it with shears for another shape.",
                "Een zaailing boven een bloempot. Knip met een schaar voor een andere vorm."),
            rij("Babble Moss", "Klatermos", "Moss block + pink petals; 2 moss &rarr; 3 carpets.", "Mosblok + roze blaadjes; 2 mos &rarr; 3 tapijtjes."),
            rij("Lake Tiles", "Meertegels", "Smelt lake silt (from the bottom of a Blossom Lake).", "Smelt meerslib (van de bodem van een Bloesemmeertje)."),
            rij("Floating Blossom Petals", "Drijvende bloesemblaadjes", "1 guh blossom leaves &rarr; 4, or pink petals &rarr; 2. They lie on water like a lily pad.",
                "1 guhbloesembladeren &rarr; 4, of roze blaadjes &rarr; 2. Ze liggen op water, net als een lelieblad."),
            rij("Cloud Block", "Wolkenblok", "4 cloud fluff (2&times;2) &rarr; 4 white; 4 fluff + pink dye &rarr; 4 pink. Soft: no fall damage.",
                "4 wolkenpluis (2&times;2) &rarr; 4 witte; 4 pluis + roze verf &rarr; 4 roze. Zacht: geen valschade."),
            rij("Thundercloud", "Onweerswolk", "8 white cloud blocks around gray dye &rarr; 8.", "8 witte wolkenblokken rond grijze verf &rarr; 8."),
            rij("Rainbow Block", "Regenboogblok", "Red, yellow and blue dye above 6 cloud fluff &rarr; 6.", "Rode, gele en blauwe verf boven 6 wolkenpluis &rarr; 6."),
            rij("Cloud Bench, Cloud Bed, Cloud Lamp", "Wolkenbank, wolkenbed, wolkenlamp",
                "Bench: 4 white cloud blocks over 3 fluff. Bed: 3 pink over 3 white cloud blocks. Lamp: 4 fluff around a guh crystal &rarr; 2. "
                "Or trade fluff with the Smith Guh.",
                "Bank: 4 witte wolkenblokken boven 3 pluis. Bed: 3 roze boven 3 witte wolkenblokken. Lamp: 4 pluis rond een guhkristal &rarr; 2. "
                "Of ruil pluis bij de smid-guh."),
            rij("Koi Food", "Koivoer", "Cheese nibbles + wheat seeds &rarr; 6.", "Kaasknabbels + tarwezaden &rarr; 6."),
            rij("Jetty Lantern, Fishing Rod on a Stand, Koi Windsock", "Steigerlantaarn, hengel op een standaard, koi-windzak",
                "Lantern: pink carpet, torch, palewood fence (top to bottom) &rarr; 2. Rod: fishing rod + palewood slab. Windsock: stick + white wool + orange dye.",
                "Lantaarn: roze tapijt, fakkel, bleekhouthek (van boven naar beneden) &rarr; 2. Hengel: hengel + bleekhoutplaat. Windzak: stok + witte wol + oranje verf."),
            rij("Star Chart, Star Lantern", "Sterrenkaart, sterrenlantaarn", "Chart: stardust above 3 paper. Lantern: stardust + cloud lamp.",
                "Kaart: sterrenstof boven 3 papier. Lantaarn: sterrenstof + wolkenlamp.")]), wide=True)
    kompas = entry(shot("bio_kompas_biomes", "The Super Compass tab Biomes"), "The Super Compass tab Biomes", "Het superkompas-tabblad Biomes", ul([
        ("A row per dimension with all its biomes. A row with a little lock opens once you have been to that dimension.",
         "Per dimensie een rijtje met alle biomen. Een rijtje met een slotje gaat open zodra je in die dimensie bent geweest."),
        ("Click a row to unfold it, pick a biome and hold the compass: it points to the nearest spot and tells you about how far it is.",
         "Klik op een rijtje om het open te klappen, kies een bioom en houd het kompas vast: het wijst naar de dichtstbijzijnde plek en zegt "
         "ongeveer hoe ver het is."),
        ("Biomes you have visited get a check mark, and the tab counts how many you have seen. A biome in another dimension tells you where it lies.",
         "Biomen waar je geweest bent krijgen een vinkje, en het tabblad telt hoeveel je er gezien hebt. Bij een bioom in een andere dimensie "
         "staat waar het ligt.")]), wide=True)
    titels = h3("New titles", "Nieuwe titels") + ul([
        ("<b>Weeb</b>: complete the Japan Collection of the Weeb House.", "<b>Weeb</b>: maak de Japan-verzameling van het weebhuisje compleet."),
        ("<b>Head in the Clouds</b>: climb to the very highest islet above a Cloud Meadow.",
         "<b>Hoofd in de wolken</b>: klim naar het allerhoogste eilandje boven een Wolkenweide."),
        ("<b>Bless You!</b>: get sneezed out of a Cloud Castle three times.", "<b>Gezondheid!</b>: laat je drie keer een wolkenkasteeltje uit niezen."),
        ("<b>Koi Whisperer</b>: finish the Fisher Guh's four lessons about the lake.",
         "<b>Koifluisteraar</b>: maak de vier lesjes van de visser-guh over het meer af.")]) + \
        p("The Travel Agency also has four new destinations: Hanami at Blossom Lake, Tea Break in Babbledale, Cloud Watching on the Cloud "
          "Meadow and Japan, with Evichonk and Nielschonk (see The Travel Agency).",
          "Het Reisbureau heeft ook vier nieuwe bestemmingen: Hanami bij het Bloesemmeertje, Theepauze in het Klaterdal, Wolkjes kijken op de "
          "Wolkenweide en Japan, met Evivads en Nielsvads (zie Het Reisbureau).")
    return intro + meer + dal + weide + weeb + blokken + kompas + titels
