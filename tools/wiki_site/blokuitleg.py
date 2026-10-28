"""
"Zo werkt het" on the block pages: what you can do with a block (right-click it, put something in it, open its screen).

Every block you can do something with (gamedata.Game.interactive_blocks: its class overrides useItemOn / useWithoutItem /
getMenuProvider) wants this on its wiki page. pages.Builder lists the ones that have none in the build's notes
("blokuitleg: ..."), so a new block can't slip through: give it an entry here when you add it.

  * UITLEG: {block id: dict(nl=Dutch, en=English, meer=[page ids])} for the blocks you place yourself (in your base, your
    garden, your guh house). Plain text; a blank line starts a new paragraph and lines starting with "- " make a list.
    meer (optional): the pages that tell more (a system, a minigame), shown as "Meer uitleg: ...".
  * LINK: {block id: [page ids]} for the blocks that belong to a minigame, a story or a building: the block page only says
    which page explains it.
A block whose page already gets a text from the knowledge base (tools/make_wiki.py, a feature's *_wiki.py) needs neither.
Write it like the rest of the wiki: short, friendly, in the game's own names; the English with the names of
tools/lang/GLOSSARY.md.
"""

UITLEG = {
    "among_noodknop": dict(
        nl="Koop je bij de Verkoper-guh in Guhpixel voor 60 muntjes, als je minstens één ronde Among Guhs hebt gespeeld."
           "\n"
           "\n"
           "Thuis rechtsklik je erop en roep je een vergadering bij elkaar: tot 8 van je eigen guhs binnen 24 blokken "
           "lopen naar de knop, gaan in een kringetje staan en bespreken drie agendapunten. Besloten wordt er niks. Guhs"
           " die zitten, rijden of ergens anders mee bezig zijn blijven waar ze zijn. Na een vergadering moet de knop "
           "even afkoelen.",
        en="You buy it from the Shopkeeper Guh in Guhpixel for 60 coins, once you have played at least one round of "
           "Among Guhs.\n"
           "\n"
           "At home, right-click it to call a meeting: up to 8 of your own guhs within 24 blocks walk to the button, "
           "stand in a little circle and go through three points on the agenda. Nothing gets decided. Guhs that are "
           "sitting, riding or busy with something else stay where they are. After a meeting the button needs a moment "
           "to cool down.",
        meer=["minigames/among", "npcs/lobby_verkoper_guh"]),
    "among_sus_bord": dict(
        nl="Het aandenken aan het oefenrondje van Among Guhs: je krijgt het als je dat rondje uitspeelt. Kwijt? De "
           "Kapitein-guh geeft je een nieuwe.\n"
           "\n"
           "Hang het aan de muur en rechtsklik erop: dan leest het bord een van zijn SUS-stickers voor.",
        en="The keepsake of the Among Guhs practice round: you get it when you finish that round. Lost it? The Captain "
           "Guh gives you a new one.\n"
           "\n"
           "Hang it on a wall and right-click it: the board reads out one of its SUS stickers.",
        meer=["minigames/among", "npcs/among_kapitein"]),
    "among_taakpaneel": dict(
        nl="Koop je bij de Verkoper-guh in Guhpixel voor 40 muntjes, als je minstens één ronde Among Guhs hebt gespeeld."
           "\n"
           "\n"
           "Thuis rechtsklik je erop en krijg je een van de acht taakjes uit Among Guhs op je scherm, gewoon voor de "
           "lol. Maak je het af, dan krijg je een complimentje. Een beloning is er niet, njeg.",
        en="You buy it from the Shopkeeper Guh in Guhpixel for 40 coins, once you have played at least one round of "
           "Among Guhs.\n"
           "\n"
           "At home, right-click it and one of the eight tasks from Among Guhs opens on your screen, just for fun. "
           "Finish it and you get a compliment. There is no reward, nyeg.",
        meer=["minigames/among", "npcs/lobby_verkoper_guh"]),
    "among_ventilatieluik": dict(
        nl="Koop je bij de Verkoper-guh in Guhpixel voor 40 muntjes, als je minstens één ronde Among Guhs hebt gespeeld."
           "\n"
           "\n"
           "Thuis gluurt er af en toe een guh in een ruimtepakje uit het luik, als er iemand in de buurt is. Rechtsklik "
           "je erop, dan komt er meteen eentje kijken. Hij kijkt even rond en duikt dan weer weg.",
        en="You buy it from the Shopkeeper Guh in Guhpixel for 40 coins, once you have played at least one round of "
           "Among Guhs.\n"
           "\n"
           "At home a guh in a space suit peeks out of the hatch now and then, when someone is nearby. Right-click it "
           "and one comes to look right away. It looks around for a moment and then ducks away again.",
        meer=["minigames/among", "npcs/lobby_verkoper_guh"]),
    "ballonsteiger": dict(
        nl="Het platform waar de guh-luchtballon van Kapitein Wolkje op wacht en weer op landt. Op het Ballonfestival "
           "vlieg je mee door met Kapitein Wolkje te praten; rechtsklik je de steiger zelf, dan zegt hij dat ook.\n"
           "\n"
           "Je kunt hem maken, of bij Kapitein Wolkje 4 ballonsteigers kopen voor 1 ballonmunt. Thuis is het vooral een "
           "vrolijk feestplatform.",
        en="The platform where Captain Cloudy's guh hot air balloon waits and lands again. At the Balloon Festival you "
           "fly along by talking to Captain Cloudy; right-click the dock itself and it tells you the same.\n"
           "\n"
           "You can craft it, or buy 4 balloon docks from Captain Cloudy for 1 balloon coin. At home it is mostly a "
           "cheerful party platform.",
        meer=["bouwwerken/ballonfestival"]),
    "baltoslee_beker": dict(
        nl="Winterdeco uit het winkeltje van Steele-Mika bij de start van de Nomguh-sledesprint. Je koopt hem voor 16 "
           "sledebelletjes, die je verdient met de sledesprint.\n"
           "\n"
           "Zet hem neer waar je wilt: hij kijkt je aan als je hem plaatst en geeft een beetje licht. Verder doet hij "
           "niks, hij staat er gewoon goud te glimmen.",
        en="Winter decoration from Steele Mika's little shop at the start of the Nomguh Sled Sprint. You buy it for 16 "
           "sleigh bells, which you earn in the sled sprint.\n"
           "\n"
           "Put it wherever you like: it faces you when you place it and gives off a little light. Other than that it "
           "does nothing, it just stands there shining gold.",
        meer=["minigames/sledesprint", "npcs/steele_mika"]),
    "baltoslee_hondenmand": dict(
        nl="Winterdeco uit het winkeltje van Steele-Mika bij de start van de Nomguh-sledesprint, voor 8 sledebelletjes "
           "(die verdien je met de sledesprint). Het mandje draait naar je toe als je het neerzet en is verder gewoon "
           "versiering.",
        en="Winter decoration from Steele Mika's little shop at the start of the Nomguh Sled Sprint, for 8 sleigh bells "
           "(you earn those in the sled sprint). The basket turns towards you when you place it and is otherwise just "
           "decoration.",
        meer=["minigames/sledesprint", "npcs/steele_mika"]),
    "baltoslee_lantaarnpaal": dict(
        nl="Winterdeco uit het winkeltje van Steele-Mika bij de start van de Nomguh-sledesprint, voor 5 sledebelletjes "
           "(die verdien je met de sledesprint). Hij geeft net zoveel licht als een fakkel, dus je kunt er een pad of je"
           " basis mee verlichten.",
        en="Winter decoration from Steele Mika's little shop at the start of the Nomguh Sled Sprint, for 5 sleigh bells "
           "(you earn those in the sled sprint). It gives off as much light as a torch, so you can light up a path or "
           "your base with it.",
        meer=["minigames/sledesprint", "npcs/steele_mika"]),
    "baltoslee_minislee": dict(
        nl="Winterdeco uit het winkeltje van Steele-Mika bij de start van de Nomguh-sledesprint, voor 10 sledebelletjes "
           "(die verdien je met de sledesprint).\n"
           "\n"
           "Je kunt erin zitten: rechtsklik met een lege hand. Zit er al iemand in, dan moet je even wachten. Sluip je "
           "terwijl je klikt, dan ga je niet zitten.",
        en="Winter decoration from Steele Mika's little shop at the start of the Nomguh Sled Sprint, for 10 sleigh bells"
           " (you earn those in the sled sprint).\n"
           "\n"
           "You can sit in it: right-click with an empty hand. If someone is already sitting in it, you have to wait. If"
           " you sneak while you click, you don't sit down.",
        meer=["minigames/sledesprint", "npcs/steele_mika"]),
    "baltoslee_sledebellen": dict(
        nl="Winterdeco uit het winkeltje van Steele-Mika bij de start van de Nomguh-sledesprint, voor 4 sledebelletjes "
           "(die verdien je met de sledesprint).\n"
           "\n"
           "Rechtsklik erop en de bellen rinkelen: tingelingeling, net als de slee van Nomguh. Elke keer klinkt het net "
           "een beetje anders.",
        en="Winter decoration from Steele Mika's little shop at the start of the Nomguh Sled Sprint, for 4 sleigh bells "
           "(you earn those in the sled sprint).\n"
           "\n"
           "Right-click it and the bells jingle, just like Nomguh's sled. Every time it sounds a little different.",
        meer=["minigames/sledesprint", "npcs/steele_mika"]),
    "baltoslee_sneeuwguh": dict(
        nl="Winterdeco uit het winkeltje van Steele-Mika bij de start van de Nomguh-sledesprint, voor 6 sledebelletjes "
           "(die verdien je met de sledesprint). Een sneeuwpop in de vorm van een guh, die naar je toe kijkt als je hem "
           "neerzet. Verder is het gewoon versiering.",
        en="Winter decoration from Steele Mika's little shop at the start of the Nomguh Sled Sprint, for 6 sleigh bells "
           "(you earn those in the sled sprint). A snowman shaped like a guh, which looks at you when you place it. "
           "Other than that it is just decoration.",
        meer=["minigames/sledesprint", "npcs/steele_mika"]),
    "beauty_leenkast": dict(
        nl="Een kledingkast vol guh-kleertjes om te lenen. Hij gaat alleen open tijdens je eigen show in de Vads-"
           "wedstrijd: terwijl je het model aankleedt, rechtsklik je erop en kies je kleren uit de kast. Er komt nooit "
           "iets uit als voorwerp.\n"
           "\n"
           "Thuis is hij een mooie decoratie; klik je erop buiten een show, dan stuurt hij je naar de Showguh.",
        en="A wardrobe full of guh clothes to borrow. It only opens during your own show in the Chonk Pageant: while you"
           " dress the model, right-click it and pick clothes from the wardrobe. Nothing ever comes out of it as an "
           "item.\n"
           "\n"
           "At home it is a nice decoration; click it outside a show and it sends you to the Show Guh.",
        meer=["minigames/beauty", "bouwwerken/guh_beauty_theater"]),
    "bellenblaas": dict(
        nl="Speelgoed voor je guhs. Rechtsklik en hij blaast een wolk bellen uit. Guhs in de buurt komen op de bellen af"
           " en jagen erop als er een speler bij is, en de guhs van je guhhuisje kiezen hem net als ander speelgoed.",
        en="A toy for your guhs. Right-click it and it blows out a cloud of bubbles. Guhs nearby come and chase the "
           "bubbles when a player is close, and the guhs of your guh house pick it like any other toy.",
        meer=["systemen/speelgoed", "systemen/guhhuisje"]),
    "beroepen_mengketel": dict(
        nl="Een borrelende roze ketel. Heb je 3 snotkruidjes en een kaasmelk bij je, rechtsklik hem dan: blub blub, je "
           "krijgt een kaasmelkdrankje. Mis je nog iets, dan zegt de ketel hoeveel je hebt.\n"
           "\n"
           "Drink je het kaasmelkdrankje, dan zijn al je effecten weg en herstel je een paar tellen. Snotkruidjes pluk "
           "je in de kruidentuin bij het apotheekje van Dokter Snotneus-guh in het Knuffeldal.",
        en="A bubbling pink cauldron. With 3 sniffle herbs and a cheese milk on you, right-click it: blub blub, you get "
           "a cheese milk potion. If something is missing, the cauldron tells you how much you have.\n"
           "\n"
           "Drinking the cheese milk potion clears all your effects and heals you for a few seconds. You pick sniffle "
           "herbs in the herb garden by Doctor Sniffleguh's pharmacy in Snuggledale.",
        meer=["verhalen/beroepen"]),
    "bestaand_zielig_lantaarntje": dict(
        nl="Een lantaarntje met guh-oortjes en een zielig gezichtje. Zet het op een blok of hang het onder een blok, net"
           " als een gewone lantaarn.\n"
           "\n"
           "Aai het (rechtsklik met een lege hand) en het knapt helemaal op: een blij gezichtje en een warmer, feller "
           "licht. Aai je het nog een keer, dan kijkt het weer zielig.",
        en="A little lantern with guh ears and a sad little face. Put it on a block or hang it under one, just like an "
           "ordinary lantern.\n"
           "\n"
           "Pet it (right-click with an empty hand) and it cheers right up: a happy face and a warmer, brighter light. "
           "Pet it again and it looks sad once more.",
        meer=["verhalen/wachter"]),
    "besteltafel": dict(
        nl="Een tafeltje met een belletje, net als in De Krokante Knabbel. Rechtsklik erop: ting! Het belletje gaat en "
           "er verschijnt een muzieknootje. Leuk naast je eigen Krokante grill.",
        en="A little table with a bell, just like in The Krusty Nibble. Right-click it: ding! The bell rings and a "
           "little note appears. Nice next to your own Krusty Grill.",
        meer=["systemen/knabbelburgers-bakken"]),
    "bieb_geheime_kast": dict(
        nl="Hij ziet eruit als een gewone boekenkast, met één roze boek dat een beetje uitsteekt. Rechtsklik erop en hij"
           " zwaait open, samen met de geheime boekenkasten die hem raken (tot 8 samen). Twee boven elkaar maken zo een "
           "geheime deur.\n"
           "\n"
           "Na 3 seconden zwaait hij vanzelf weer dicht, maar nooit als er iemand in de deuropening staat. Klik je op "
           "een open kast, dan gaat hij meteen dicht.",
        en="It looks like an ordinary bookshelf, with one pink book sticking out a little. Right-click it and it swings "
           "open, together with the secret bookcases touching it (up to 8 together). Two on top of each other make a "
           "secret door.\n"
           "\n"
           "After 3 seconds it swings shut by itself, but never while someone stands in the doorway. Click an open one "
           "and it closes right away.",
        meer=["bouwwerken/guhbibliotheek"]),
    "bieb_guhfauteuil": dict(
        nl="Een roze leesfauteuil in de vorm van een guh. Rechtsklik erop met een lege hand en je gaat erin zitten; er "
           "past er één tegelijk in. Sluip om weer op te staan.",
        en="A pink reading armchair in the shape of a guh. Right-click it with an empty hand and you sit down in it; one"
           " at a time. Sneak to get up again."),
    "blubkacheltje": dict(
        nl="Een kacheltje met een glazen potje erop, dat vadskracht maakt. Rechtsklik erop met een Sausblubje in een "
           "potje om het blubje erin te zetten, en geef het af en toe een kaasknabbel (rechtsklik ermee, of laat een "
           "Knabbelbuis of trechter ze brengen). Zolang het blubje warm is, geeft het kacheltje 6 vadskracht.\n"
           "\n"
           "Eén knabbel houdt het 10 minuten warm en het bakje bewaart tot 16 knabbels. Rechtsklik met een lege hand om "
           "te zien hoe het gaat; sluip met een lege hand en het blubje komt er weer uit, in zijn potje.",
        en="A little stove with a glass jar on top that makes chonk power. Right-click it with a Sauce Blubby in a Jar "
           "to put the blubby in, and give it a cheese nibble now and then (right-click with one, or let a Nibble Tube "
           "or hopper bring them). As long as the blubby is warm, the stove gives 6 chonk power.\n"
           "\n"
           "One nibble keeps it warm for 10 minutes and the little tray holds up to 16 nibbles. Right-click with an "
           "empty hand to see how it is doing; sneak with an empty hand and the blubby comes out again, in its jar.",
        meer=["systemen/vadskrachtbronnen", "systemen/sausdieren"]),
    "brouwautomaat": dict(
        nl="De Guhbrouwketel die zelf brouwt, op 8 vadskracht in plaats van grillspiespoeder. Giet er een emmer kaassaus"
           " in (of vul hem via een Sausslang) en stop er één ingrediënt en 3 glazen flesjes in, door erop te "
           "rechtsklikken of met een trechter. Na 10 seconden borrelen er 3 Guhdrankjes uit; de ingrediënten zijn "
           "dezelfde als bij de Guhbrouwketel.\n"
           "\n"
           "Rechtsklik met een lege hand om de drankjes eruit te pakken; sluip erbij om wat er nog in zit terug te "
           "krijgen.",
        en="The Guh Brewing Cauldron that brews by itself, on 8 chonk power instead of Grill Skewer Powder. Pour in a "
           "bucket of cheese sauce (or fill it through a Sauce Hose) and put in one ingredient and 3 glass bottles, by "
           "right-clicking or with a hopper. After 10 seconds 3 Guh Potions bubble out; the ingredients are the same as "
           "for the Guh Brewing Cauldron.\n"
           "\n"
           "Right-click with an empty hand to take the potions out; sneak while you do it to get back what is still "
           "inside.",
        meer=["systemen/saus", "systemen/guh-technologie"]),
    "bzg_brievenbus": dict(
        nl="De brievenbus uit de studio van Boer zoekt Guh. Je kunt er ook zelf een maken (met ijzer, papier, rode "
           "kleurstof en een stok) en thuis neerzetten.\n"
           "\n"
           "Rechtsklik erop om te kijken of er post is. Thuis is dat nooit zo: je krijgt een grapje te horen, zoals "
           "\"Geen post vandaag, njeg.\"",
        en="The mailbox from the Farmer Wants a Guh studio. You can also craft one yourself (with iron, paper, red dye "
           "and a stick) and put it down at home.\n"
           "\n"
           "Right-click it to see if there is any mail. At home there never is: you get a little joke instead, like \"No "
           "mail today, nyeg.\"",
        meer=["minigames/bzg"]),
    "bzg_ingelijste_brief": dict(
        nl="Het aandenken aan Boer zoekt Guh: het bedankbriefje van Boer Guhrrit in een houten lijstje. Je krijgt het "
           "als je de show uitspeelt.\n"
           "\n"
           "Hang het aan de muur en rechtsklik erop om de brief nog eens te lezen.",
        en="The keepsake of Farmer Wants a Guh: Farmer Guhrrit's thank-you letter in a wooden frame. You get it when you"
           " finish the show.\n"
           "\n"
           "Hang it on a wall and right-click it to read the letter again.",
        meer=["minigames/bzg"]),
    "campingmarkt_hakblok": dict(
        nl="Een hakblok met een stam erop. Rechtsklik erop: tjak, de stam wordt gekloofd, en even later staat er vanzelf"
           " een nieuwe stam klaar. Thuis is dat gewoon voor de lol: er komt geen hout uit.\n"
           "\n"
           "Op de Grillcamping hak je hier in het kampeerverhaal het brandhout voor het grote kampvuur.",
        en="A chopping block with a log on it. Right-click it: chop, the log is split, and a moment later a new log "
           "stands ready by itself. At home that is just for fun: no wood comes out.\n"
           "\n"
           "At the Grill Campground, in the camping story, you chop the firewood for the big campfire here.",
        meer=["verhalen/camping"]),
    "campingmarkt_kampvuur": dict(
        nl="Een groot kampvuur voor je eigen kampeerplek. Rechtsklik erop met vuursteen en staal: het brandt en blijft "
           "branden. Rechtsklik met een schep en het gaat uit.\n"
           "\n"
           "Brandt het, rechtsklik er dan op met een roosterstok terwijl je marshmallowknabbels bij je hebt, en rooster "
           "ze boven het vuur: te vroeg is koud, te laat is zwart. De roosterstok krijg je in het kampeerverhaal op de "
           "Grillcamping.",
        en="A big campfire for your own camping spot. Right-click it with flint and steel: it lights and keeps burning. "
           "Right-click it with a shovel and it goes out.\n"
           "\n"
           "While it burns, right-click it with a Roasting Stick while you carry marshmallow nibbles, and roast them "
           "over the fire: too early is cold, too late is black. You get the Roasting Stick in the camping story at the "
           "Grill Campground.",
        meer=["verhalen/camping"]),
    "campingmarkt_weegschaal": dict(
        nl="Een weegschaal, de beloning van de ruilmarkt in de Waag. Rechtsklik erop: je gewone hand ligt op de "
           "linkerschaal, je andere hand op de rechter. De hand met de meeste spullen erin is het zwaarst; de schaal "
           "zakt naar die kant en zwaait na 3 seconden vanzelf terug. Boven je hotbar zie je hoeveel er aan elke kant "
           "ligt.",
        en="A scale, the reward of the Barter Market in the Weigh House. Right-click it: your main hand lies on the left"
           " pan, your other hand on the right one. The hand with the most items in it is the heaviest; the scale tips "
           "to that side and swings back by itself after 3 seconds. Above your hotbar you see how much lies on each "
           "side.",
        meer=["verhalen/ruilmarkt"]),
    "disco_dynamo": dict(
        nl="Een dansvloer van 3 bij 3 blokken met een draaitafel, die vadskracht maakt. Rechtsklik erop met een "
           "muziekplaat: hij speelt steeds opnieuw en tamme guhs binnen 8 blokken komen dansen. Elke danser geeft 5 "
           "vadskracht, met hooguit 4 guhs (20 vadskracht).\n"
           "\n"
           "Elke plaat heeft zijn eigen lichtshow op de vloer. Rechtsklik met een lege hand om de plaat er weer af te "
           "halen.",
        en="A dance floor of 3 by 3 blocks with a turntable that makes chonk power. Right-click it with a music disc: it"
           " plays over and over and tame guhs within 8 blocks come to dance. Every dancer gives 5 chonk power, with at "
           "most 4 guhs (20 chonk power).\n"
           "\n"
           "Every disc has its own light show on the floor. Right-click with an empty hand to take the disc off again.",
        meer=["systemen/vadskrachtbronnen", "systemen/vadskracht"]),
    "einde2_geest_spel_mediteerkussen": dict(
        nl="Een kussen om op te mediteren. Heb je het hele Guhvatar-verhaal uitgespeeld, rechtsklik er dan op en blijf "
           "even stilzitten: je sluit je ogen en na een paar tellen gaat je ziel naar de Geestenwereld. Loop je weg, dan"
           " word je weer wakker.\n"
           "\n"
           "Na het verhaal krijg je er één cadeau, en je kunt er meer maken. Voor het verhaal klaar is blijft je ziel "
           "nog thuis.",
        en="A cushion to meditate on. Once you have played the whole Guhvatar story, right-click it and sit still for a "
           "moment: you close your eyes and after a few seconds your soul goes to the Spirit World. Walk away and you "
           "wake up again.\n"
           "\n"
           "After the story you get one as a gift, and you can craft more. Before the story is done your soul stays at "
           "home.",
        meer=["systemen/geest-spel", "systemen/geestenwereld"]),
    "einde2_h3_aarde_koolkraam": dict(
        nl="Het kraampje van de koolman, een aandenken uit hoofdstuk 3 van de Guhvatar. Het is vooral versiering. "
           "Rechtsklik erop en je hoort wat de koolman altijd roept.",
        en="The Cabbage Man's stall, a keepsake from chapter 3 of the Guhvatar. It is mostly decoration. Right-click it "
           "and you hear what the Cabbage Man always shouts.",
        meer=["systemen/guhvatar-aarde"]),
    "einde2_reizen_appa_platform": dict(
        nl="Het midden van je eigen landingsplek voor Appa. Zet het neer en het wordt jouw Appa-platform in die dimensie"
           " (één per dimensie; een nieuwe vervangt de oude).\n"
           "\n"
           "Leg er een vloer van 5 bij 5 omheen en houd 3 blokken lucht vrij boven alle 25 blokken. Rechtsklik erop om "
           "te zien of het klaar is: wat er nog mist zie je aan wolkjes. Fluit je binnen 20 blokken van je platform op "
           "het Appa-fluitje, dan landt Appa erop, en je platforms staan bovenaan in Appa's reislijst.\n"
           "\n"
           "Na het hele Guhvatar-verhaal krijg je er één; meer kun je maken.",
        en="The middle of your own landing place for Appa. Place it and it becomes your Appa Platform in that dimension "
           "(one per dimension; a new one replaces the old one).\n"
           "\n"
           "Lay a 5 by 5 floor around it and keep 3 blocks of air free above all 25 blocks. Right-click it to see "
           "whether it is ready: little clouds show what is still missing. Blow the Appa Whistle within 20 blocks of "
           "your platform and Appa lands on it, and your platforms come first in Appa's travel list.\n"
           "\n"
           "After the whole Guhvatar story you get one; you can craft more.",
        meer=["systemen/reizen-met-appa", "systemen/appa-en-momo"]),
    "enderguh_ei": dict(
        nl="Een bijzonder ei dat je als beloning krijgt in het Guheinde. Zet het ergens neer en wacht: het wiebelt, "
           "krijgt barstjes en na drie stapjes springt er een baby Vahoege Enderguh uit.\n"
           "\n"
           "Rechtsklik het ei om te zien hoe ver het is. Het guhtje is nog wild: tem het met kaasknabbels, of laat het "
           "lekker rondhuppelen.",
        en="A special egg you get as a reward in the Guh End. Put it down somewhere and wait: it wobbles, cracks, and "
           "after three steps a baby Wahoog Enderguh hops out.\n"
           "\n"
           "Right-click the egg to see how far along it is. The little guh is still wild: tame it with cheese nibbles, "
           "or let it hop around.",
        meer=["guhs/vahoege_ender", "dimensies/guheinde"]),
    "frituurautomaat": dict(
        nl="De koekenpan die zelf frituurt, in kaasfrituursaus en op 8 vadskracht. Giet er een emmer kaasfrituursaus in "
           "(of vul hem via een Sausslang) en stop er kaasknabbels in: ze komen eruit als gefrituurde kaasknabbels. Een "
           "guhvis wordt een vadsig gebakken guhvis. Eén emmer saus is goed voor 100 stuks.\n"
           "\n"
           "Rechtsklik met een lege hand om te pakken wat klaar is.",
        en="The frying pan that fries by itself, in cheese frying sauce and on 8 chonk power. Pour in a bucket of cheese"
           " frying sauce (or fill it through a Sauce Hose) and put in cheese nibbles: they come out as fried cheese "
           "nibbles. A guhfish becomes a chonky fried guhfish. One bucket of sauce is good for 100 of them.\n"
           "\n"
           "Right-click with an empty hand to take out what is ready.",
        meer=["systemen/saus", "systemen/guh-technologie"]),
    "grillkoolpers": dict(
        nl="Een pers van twee blokken hoog. Op 8 vadskracht perst hij een emmer kaasfrituursaus en een emmer water in 5 "
           "seconden samen tot een blok grillkool, zonder verbrande pootjes. Giet de emmers er met de hand in of laat ze"
           " aanvoeren door een Sausslang.\n"
           "\n"
           "Rechtsklik met een lege hand om de grillkool eruit te pakken.",
        en="A press two blocks tall. On 8 chonk power it squeezes a bucket of cheese frying sauce and a bucket of water "
           "together into a block of grill coal in 5 seconds, without burnt pawsies. Pour the buckets in by hand or let "
           "a Sauce Hose bring them.\n"
           "\n"
           "Right-click with an empty hand to take the grill coal out.",
        meer=["systemen/saus", "systemen/guh-technologie"]),
    "guh_bloempot": dict(
        nl="Een guhtuintje voor één plant. Rechtsklik met zaadjes om iets te planten:\n"
           "- Knabbelzaadjes worden een knabbelplantje vol knabbelgraan (voor het knabbeldeeg van de bakker).\n"
           "- Theekruidzaadjes worden een bosje theekruid (thee in het theepotje).\n"
           "- Guhbloemzaadjes worden een guhbloem met guhbloemetjes (roze thee, of vijf voor een feestboeket).\n"
           "\n"
           "De plant groeit in drie stapjes. Vanzelf gaat dat langzaam; geef je hem water met de guh-gieter, dan gaat "
           "het veel sneller, tot de volgende stap (daarna heeft hij weer dorst). Een getemde guh geeft je plantjes ook "
           "water, en als guhs vlakbij zingen groeit er af en toe een plantje meteen een stapje.\n"
           "\n"
           "Is hij rijp, rechtsklik dan met een lege hand: je krijgt 2 of 3 stuks, en soms een zaadje terug. De plant "
           "blijft staan en groeit opnieuw. Alleen guh-zaadjes passen erin, geen gewone bloemen.",
        en="A little guh garden for one plant. Right-click it with seeds to plant something:\n"
           "- Nibble seeds grow into a nibble plant full of nibble grain (for the baker's nibble dough).\n"
           "- Tea herb seeds grow into a bunch of tea herb (tea in the teapot).\n"
           "- Guh flower seeds grow into a guh flower with little guh flowers (pink tea, or five for a party bouquet).\n"
           "\n"
           "The plant grows in three steps. On its own that is slow; water it with the guh watering can and it goes much"
           " faster, until the next step (then it is thirsty again). A tamed guh waters your plants too, and when guhs "
           "sing close by a plant now and then grows a step right away.\n"
           "\n"
           "When it is ripe, right-click it with an empty hand: you get 2 or 3, and sometimes a seed back. The plant "
           "stays and grows again. Only guh seeds fit in it, no ordinary flowers.",
        meer=["blokken/guh_moestuinbak", "bouwwerken/guhboerderij"]),
    "guh_slaapzak": dict(
        nl="Een roze slaapzak met een guh-capuchon. Leg hem op een stevige ondergrond en rechtsklik erop als het nacht "
           "is: je slaapt erin net als in een bed, en het telt mee om de nacht over te slaan. Je spawnpunt verandert "
           "niet, dus je kunt hem overal neerleggen.\n"
           "\n"
           "Slaap je een hele nacht door, dan word je wakker met het effect Uitgerust. Er past één speler tegelijk in.",
        en="A pink sleeping bag with a guh hood. Put it on solid ground and right-click it at night: you sleep in it "
           "just like in a bed, and it counts for skipping the night. Your spawn point does not change, so you can put "
           "it down anywhere.\n"
           "\n"
           "Sleep through a whole night and you wake up Well-Rested. One player fits in it at a time.",
        meer=["bouwwerken/kampeerplekje"]),
    "guh_telescoop": dict(
        nl="Een kleine telescoop met guh-oortjes, te koop bij Professor Sterretje voor 8 wenssterren. Rechtsklik erop "
           "als het donker is: je ziet een stukje hemel waarin een guh-sterrenbeeld verstopt zit. Klik een ster en dan "
           "de volgende om een lijntje te trekken. Kloppen alle lijntjes, dan krijg je wenssterren, en een nieuw "
           "sterrenbeeld gaat in je sterrenatlas.\n"
           "\n"
           "Je vindt er hooguit 3 per nacht. De drie zeldzame sterrenbeelden zie je alleen tijdens een sterrenregen.",
        en="A little telescope with guh ears, for sale at Professor Twinkle's for 8 Wishing Stars. Right-click it when "
           "it is dark: you see a piece of sky with a guh constellation hidden in it. Click a star and then the next one"
           " to draw a line. When all the lines are right you get Wishing Stars, and a new constellation goes into your "
           "Star Atlas.\n"
           "\n"
           "You can find at most 3 per night. The three rare constellations only show during a Star Shower.",
        meer=["bouwwerken/guh_sterrenwacht"]),
    "guh_voerbak": dict(
        nl="Een roze voerbak voor je boerderijdieren. Rechtsklik met knabbelvoer om er een portie in te doen, tot 4 "
           "porties.\n"
           "\n"
           "Guhschaapjes, knabbelkippetjes en guhkoeien binnen 10 blokken die vandaag nog niet gevoerd zijn, lopen er "
           "vanzelf naartoe en eten een portie. Dat telt gewoon als voeren, dus je hoeft niet elk dier zelf eten te "
           "geven. Een comparator ziet hoe vol de bak is.",
        en="A pink trough for your farm animals. Right-click it with nibble feed to put in a portion, up to 4 portions.\n"
           "\n"
           "Guh lambs, nibble chicks and guh cows within 10 blocks that haven't been fed today walk to it by themselves "
           "and eat a portion. That counts as feeding, so you don't have to feed every animal yourself. A comparator "
           "sees how full it is.",
        meer=["bouwwerken/guhboerderij"]),
    "guh_wastobbe": dict(
        nl="Een houten wastobbe met een douchetje, om je eigen tamme guh in te wassen:\n"
           "- Gebruik guhshampoo op je guh, binnen 6 blokken van een vrije tobbe: hij springt erin.\n"
           "- Schrob hem 5 keer (rechtsklik met een lege hand op de guh): roze schuim.\n"
           "- Spoel hem af: rechtsklik met een lege hand op de tobbe, of met een emmer water.\n"
           "- Houd de guh-föhn op hem tot hij droog is.\n"
           "\n"
           "Dan glanst je guh een hele dag, springt hij blij op en krijg je een klein prijsje. Doet niemand twee minuten"
           " iets, dan springt hij er weer uit.",
        en="A wooden washtub with a little shower, to wash your own tame guh in:\n"
           "- Use Guh Shampoo on your guh within 6 blocks of a free tub: it hops in.\n"
           "- Scrub it 5 times (right-click the guh with an empty hand): pink foam.\n"
           "- Rinse it: right-click the tub with an empty hand, or with a water bucket.\n"
           "- Hold the Guh Hair Dryer on it until it is dry.\n"
           "\n"
           "Then your guh shines for a whole day, does a happy jump and you get a little prize. If nobody does anything "
           "for two minutes, it hops out again.",
        meer=["bouwwerken/knuffelbad"]),
    "guh_wiegje": dict(
        nl="Een wiegje met een guhgezichtje. Rechtsklik het met een lege hand om te wiegen: je hoort een slaapliedje en "
           "er zweven slaapsterretjes op.\n"
           "\n"
           "Babyguhs binnen 4 blokken groeien dan een stukje, net alsof je ze voert. Elke babyguh kan dat één keer per 5"
           " minuten. In de Knuffelcreche slapen de babyguhtjes in deze wiegjes.",
        en="A cradle with a little guh face. Right-click it with an empty hand to rock it: you hear a lullaby and sleepy"
           " stars float up.\n"
           "\n"
           "Baby guhs within 4 blocks then grow up a bit, just as if you fed them. Each baby guh can do that once every "
           "5 minutes. In the Snuggle Daycare the baby guhs sleep in these cradles.",
        meer=["minigames/creche"]),
    "guhbioscoop_popcornmachine": dict(
        nl="Koop je bij de Verkoper-guh in Guhpixel voor 60 muntjes.\n"
           "\n"
           "Staat hij binnen 12 blokken van een Guhbioscoop-projector, dan krijgt elke guh in het publiek een bakje "
           "popcorn bij de film, en de machine poft mee. Rechtsklik erop en je krijgt zelf ook een Bakje popcorn, één "
           "keer per minuut.",
        en="You buy it from the Shopkeeper Guh in Guhpixel for 60 coins.\n"
           "\n"
           "When it stands within 12 blocks of a Guh Cinema Projector, every guh in the audience gets a tub of popcorn "
           "with the movie, and the machine pops along. Right-click it and you get a Tub of Popcorn yourself too, once a"
           " minute.",
        meer=["systemen/guhbioscoop"]),
    "guhbioscoop_projector": dict(
        nl="Zit in de Guhbioscoop-set van de Verkoper-guh in Guhpixel (250 muntjes, een tweede set 125), samen met 28 "
           "stukken Bioscoopdoek en 4 Bioscoopstoeltjes.\n"
           "\n"
           "Bouw een scherm van Bioscoopdoek (tot 7 breed en 4 hoog) en zet de projector neer met de lens naar het doek:"
           " hij kijkt de kant op waar jij naar kijkt als je hem neerzet. Rechtsklik erop en kies een van jouw films. "
           "Die verdien je met spelen. Tijdens de film komen je guhs zelf kijken en gaan ze op de stoeltjes zitten.",
        en="Comes in the Guh Cinema Set from the Shopkeeper Guh in Guhpixel (250 coins, a second set 125), together with"
           " 28 pieces of Cinema Screen and 4 Cinema Seats.\n"
           "\n"
           "Build a screen out of Cinema Screen (up to 7 wide and 4 high) and place the projector with its lens towards "
           "the screen: it points the way you are looking when you place it. Right-click it and pick one of your movies."
           " You earn those by playing. During the movie your guhs come to watch and sit on the seats themselves.",
        meer=["systemen/guhbioscoop"]),
    "guhbioscoop_stoeltje": dict(
        nl="Vier zitten in de Guhbioscoop-set; losse koop je bij de Verkoper-guh in Guhpixel voor 15 muntjes.\n"
           "\n"
           "Rechtsklik met een lege hand om erop te gaan zitten. Tijdens een film zoeken je guhs zelf een vrij stoeltje "
           "binnen 12 blokken van de projector; zit er een guh op, dan is het bezet. Zet het neer terwijl je van het "
           "doek af kijkt, dan kijkt wie erop zit naar het scherm.",
        en="Four come in the Guh Cinema Set; you buy extra ones from the Shopkeeper Guh in Guhpixel for 15 coins.\n"
           "\n"
           "Right-click with an empty hand to sit on it. During a movie your guhs pick a free seat within 12 blocks of "
           "the projector themselves; when a guh sits on it, it is taken. Place it while you look away from the screen, "
           "and whoever sits on it faces the screen.",
        meer=["systemen/guhbioscoop"]),
    "guhbrouwketel": dict(
        nl="Een grote pan op een barbecuetje: hier brouw je guhdrankjes, alles met rechtsklikken.\n"
           "- Stook de barbecue op met grillspiespoeder (elk snufje is goed voor 4 brouwsels).\n"
           "- Giet er een emmer kaassaus in: 3 porties kaasbouillon.\n"
           "- Roer er één ingrediënt door: het borrelt even en wordt een guhdrankje.\n"
           "- Vul er 3 glazen flesjes mee.\n"
           "\n"
           "Rechtsklik met een lege hand om te zien wat er in de pan zit.",
        en="A big pan on a little barbecue: here you brew guh potions, all with right-clicks.\n"
           "- Stoke the barbecue with grill skewer powder (every pinch is good for 4 brews).\n"
           "- Pour in a bucket of cheese sauce: 3 portions of cheese broth.\n"
           "- Stir in one ingredient: it bubbles for a while and becomes a guh potion.\n"
           "- Fill 3 glass bottles from it.\n"
           "\n"
           "Right-click with an empty hand to see what is in the pan.",
        meer=["systemen/brouwen"]),
    "guhkade_kast_flappy": dict(
        nl="Een echte speelkast voor thuis, twee blokken hoog. Je koopt hem bij de Verkoper-guh in Guhpixel: je eerste "
           "kast kost 250 muntjes, elke volgende 150.\n"
           "\n"
           "Rechtsklik erop om Flappy Guh te spelen: één knop, fladderen tussen de kaasknabbelpilaren door. De kast "
           "houdt een top 5 bij. Je guhs (binnen 12 blokken) komen af en toe ook een potje spelen, en hun scores komen "
           "op dezelfde lijst.",
        en="A real arcade cabinet for at home, two blocks tall. You buy it from the Shopkeeper Guh in Guhpixel: your "
           "first cabinet costs 250 coins, every next one 150.\n"
           "\n"
           "Right-click it to play Flappy Guh: one button, flapping between the cheese nibble pillars. The cabinet keeps"
           " a top 5. Your guhs (within 12 blocks) come and play a game now and then too, and their scores go on the "
           "same list.",
        meer=["systemen/guhkade"]),
    "guhkade_kast_pong": dict(
        nl="Een echte speelkast voor thuis, twee blokken hoog. Je koopt hem bij de Verkoper-guh in Guhpixel: je eerste "
           "kast kost 250 muntjes, elke volgende 150.\n"
           "\n"
           "Rechtsklik erop om Mika-Pong te spelen: het balletje is een rollende guh en aan de overkant staat een Mika. "
           "De kast houdt een top 5 bij. Je guhs (binnen 12 blokken) komen af en toe ook een potje spelen, en hun scores"
           " komen op dezelfde lijst.",
        en="A real arcade cabinet for at home, two blocks tall. You buy it from the Shopkeeper Guh in Guhpixel: your "
           "first cabinet costs 250 coins, every next one 150.\n"
           "\n"
           "Right-click it to play Mika Pong: the ball is a rolling guh and a Mika stands on the other side. The cabinet"
           " keeps a top 5. Your guhs (within 12 blocks) come and play a game now and then too, and their scores go on "
           "the same list.",
        meer=["systemen/guhkade"]),
    "guhkantoor_bureautje": dict(
        nl="Eén zit in de Guhkantoor-set; extra bureautjes koop je bij de Verkoper-guh in Guhpixel voor 40 muntjes (als "
           "je de set al hebt).\n"
           "\n"
           "Zet het binnen 8 blokken van een Prikklok: er passen er 4 op één klok. Op elk bureautje kan één guh "
           "\"werken\", wat betekent dat hij op het toetsenbord ligt te slapen. Rechtsklik erop opent het scherm van de "
           "Prikklok.",
        en="One comes in the Guh Office Set; extra desks you buy from the Shopkeeper Guh in Guhpixel for 40 coins (once "
           "you have the set).\n"
           "\n"
           "Place it within 8 blocks of a Time Clock: 4 fit on one clock. On every desk one guh can \"work\", which means "
           "it sleeps on the keyboard. Right-clicking it opens the Time Clock's screen.",
        meer=["systemen/guhkantoor"]),
    "guhkantoor_kwartaalrapport": dict(
        nl="Een papiertje uit het Guhkantoor: bij elk derde Loonstrookje dat je ophaalt krijg je er een Kwartaalrapport "
           "bij.\n"
           "\n"
           "Rechtsklik ermee in de lucht om het te lezen, of rechtsklik op een muur om het op te hangen. Opgehangen lees"
           " je het met een rechtsklik, en als je het eraf haalt blijft de tekst bewaard.",
        en="A piece of paper from the Guh Office: with every third Payslip you collect you get a Quarterly Report too.\n"
           "\n"
           "Right-click with it in the air to read it, or right-click a wall to hang it up. Hung up, you read it with a "
           "right-click, and when you take it down the text is kept.",
        meer=["systemen/guhkantoor"]),
    "guhkantoor_loonstrookje": dict(
        nl="Het loon van een guh die op het Guhkantoor werkt: elke 8 echte uren ligt er een klaar bij de Prikklok "
           "(hoogstens 3 per guh). Je haalt ze op in het scherm van de Prikklok.\n"
           "\n"
           "Rechtsklik ermee in de lucht om het te lezen, of rechtsklik op een muur om het op te hangen. Opgehangen lees"
           " je het met een rechtsklik, en als je het eraf haalt blijft de tekst bewaard.",
        en="The pay of a guh that works at the Guh Office: every 8 real hours one is ready at the Time Clock (at most 3 "
           "per guh). You collect them in the Time Clock's screen.\n"
           "\n"
           "Right-click with it in the air to read it, or right-click a wall to hang it up. Hung up, you read it with a "
           "right-click, and when you take it down the text is kept.",
        meer=["systemen/guhkantoor"]),
    "guhkantoor_oorkonde": dict(
        nl="De prijs voor de Werknemer van de maand: de guh die in een echte maand de meeste uren op het Guhkantoor "
           "heeft geslapen, krijgt na afloop van die maand deze oorkonde.\n"
           "\n"
           "Rechtsklik ermee in de lucht om hem te lezen, of rechtsklik op een muur om hem op te hangen. Opgehangen lees"
           " je hem met een rechtsklik.",
        en="The prize for the Employee of the Month: the guh that slept the most hours at the Guh Office in a real month"
           " gets this certificate when that month is over.\n"
           "\n"
           "Right-click with it in the air to read it, or right-click a wall to hang it up. Hung up, you read it with a "
           "right-click.",
        meer=["systemen/guhkantoor"]),
    "guhkantoor_prikklok": dict(
        nl="Het hart van je Guhkantoor. Je koopt hem in de Guhkantoor-set (met één Bureautje) bij de Verkoper-guh in "
           "Guhpixel voor 200 muntjes.\n"
           "\n"
           "Tot 4 Bureautjes binnen 8 blokken horen bij de klok. Rechtsklik erop voor het scherm: zet een van je guhs "
           "die in de buurt staat aan het werk (hij gaat op het bureautje slapen), stuur hem weer naar huis, en haal de "
           "Loonstrookjes op. Elke 8 echte uren verdient een guh er een. Breek je de klok, dan gaan alle guhs weer naar "
           "huis.",
        en="The heart of your Guh Office. You buy it in the Guh Office Set (with one Little Desk) from the Shopkeeper "
           "Guh in Guhpixel for 200 coins.\n"
           "\n"
           "Up to 4 Little Desks within 8 blocks belong to the clock. Right-click it for the screen: put one of your "
           "guhs standing nearby to work (it goes to sleep on the desk), send it home again, and collect the Payslips. A"
           " guh earns one every 8 real hours. Break the clock and all the guhs go home again.",
        meer=["systemen/guhkantoor"]),
    "guhklok": dict(
        nl="Een sensor die een redstonesignaal geeft. Hij heeft 1 vadskracht nodig; zonder vadskracht is het signaal "
           "uit.\n"
           "\n"
           "Rechtsklik om hem te verzetten (sluip + rechtsklik gaat terug):\n"
           "- een kort tikje elke 1, 2, 5, 10 of 30 seconden, elke minuut of elke 5 minuten;\n"
           "- een signaal zolang het dag is;\n"
           "- een signaal zolang het nacht is.\n"
           "\n"
           "Alle Guhklokken met dezelfde stand tikken tegelijk. Een comparator leest hoe ver de klok is tot het volgende"
           " tikje.",
        en="A sensor that gives a redstone signal. It needs 1 chonk power; without chonk power the signal is off.\n"
           "\n"
           "Right-click to change its setting (sneak + right-click goes back):\n"
           "- a short tick every 1, 2, 5, 10 or 30 seconds, every minute or every 5 minutes;\n"
           "- a signal for as long as it is day;\n"
           "- a signal for as long as it is night.\n"
           "\n"
           "All Guh Clocks with the same setting tick at the same moment. A comparator reads how far the clock is to its"
           " next tick.",
        meer=["systemen/sensoren", "systemen/vadskracht"]),
    "guhmon_badgedoos": dict(
        nl="Het aandenken aan het Guhmon-gevecht, met plek voor acht gymbadges. Je krijgt het als je het gevecht "
           "uitspeelt.\n"
           "\n"
           "Rechtsklik erop met een badge (Dutjesbadge, Njegbadge of Knabbelbadge) om hem erin te leggen; elke badge "
           "past er één keer in. Met een lege hand zie je hoeveel badges erin zitten. Breek je het doosje, dan krijg je "
           "het doosje en de badges terug. De andere vijf plekjes blijven leeg: die gyms zijn dicht wegens dutje.",
        en="The keepsake of the Guhmon Battle, with room for eight gym badges. You get it when you finish the battle.\n"
           "\n"
           "Right-click it with a badge (Nap Badge, Nyeg Badge or Nibble Badge) to put it in; each badge fits in once. "
           "With an empty hand you see how many badges are in it. Break the case and you get the case and the badges "
           "back. The other five spots stay empty: those gyms are closed for a nap.",
        meer=["minigames/guhmon"]),
    "guhparkour_finishpaaltje": dict(
        nl="Het laatste stuk van een Guh-parkour: hier stopt de tijd als een guh het aantikt. Je maakt het met wol, een "
           "houten hek en een houten plaat.\n"
           "\n"
           "Klik het als laatste aan met je Startpaaltje als je de route uitzet. Zelf heeft het verder niks in te "
           "stellen; het Startpaaltje onthoudt alles.",
        en="The last piece of a Guh Parkour: the time stops here when a guh touches it. You craft it from wool, a wooden"
           " fence and a wooden slab.\n"
           "\n"
           "Click it last with your Start Post when you lay out the route. It has nothing to set up itself; the Start "
           "Post remembers everything.",
        meer=["systemen/guhparkour", "blokken/guhparkour_startpaaltje"]),
    "guhparkour_startpaaltje": dict(
        nl="Hier begint een Guh-parkour. Je maakt het met lichtgroene wol, een houten hek en een houten plaat.\n"
           "\n"
           "Met het paaltje in je hand rechtsklik je speelgoed en hindernissen aan in volgorde (1, 2, 3...), tot en met "
           "het Finishpaaltje, en daarna zet je het neer. Een route heeft hoogstens 16 stukken, binnen 32 blokken van de"
           " start. Rechtsklik op het geplaatste paaltje voor het scherm: zet er tot 4 van je guhs op en bekijk hun "
           "rondetijden. Sluip + rechtsklik om meer stukken aan te klikken. Breek je het, dan onthoudt het paaltje de "
           "route.",
        en="A Guh Parkour starts here. You craft it from lime wool, a wooden fence and a wooden slab.\n"
           "\n"
           "With the post in your hand, right-click toys and obstacles in order (1, 2, 3...), ending with the Finish "
           "Post, then place it. A route has at most 16 pieces, within 32 blocks of the start. Right-click the placed "
           "post for its screen: put up to 4 of your guhs on it and see their lap times. Sneak + right-click to click "
           "more pieces. Break it and the post remembers the route.",
        meer=["systemen/guhparkour"]),
    "guhpixel_poort": dict(
        nl="Een oud beige beeldschermpje voor thuis. Rechtsklik erop en je gaat meteen naar de Guhpixel-lobby.\n"
           "\n"
           "Je maakt hem met ijzer, glas, redstone en een Netwerkkabeltje. Dat kabeltje krijg je van de Welkomstguh bij "
           "je eerste bezoek aan de lobby, dus je kunt de poort pas maken als je er al eens bent geweest.",
        en="An old beige monitor for at home. Right-click it and you go straight to the Guhpixel lobby.\n"
           "\n"
           "You craft it from iron, glass, redstone and a Network Cable. You get that cable from the Welcome Guh on your"
           " first visit to the lobby, so you can only make the gate once you have been there.",
        meer=["dimensies/guhpixel", "npcs/lobby_welkomstguh"]),
    "guhriobeloning_pijp": dict(
        nl="Een groene pijp die echt werkt, te koop bij Pad-guh (2 stuks). Stapel blokken op elkaar voor een hogere "
           "pijp; het bovenste blok is de opening. Ga op de opening staan en sluip (of rechtsklik op de pijp terwijl je "
           "erop staat): je glijdt erin en komt eruit bij de dichtstbijzijnde andere pijp van dezelfde kleur, tot 50 "
           "blokken ver.\n"
           "\n"
           "Twee groene pijpen zijn meteen een paar. Rechtsklik met een kleurstof om de hele pijp te verven: zo zet je "
           "meer paren dicht bij elkaar.",
        en="A green pipe that really works, for sale at Toad-guh's (2 pieces). Stack blocks for a taller pipe; the top "
           "block is the opening. Stand on the opening and sneak (or right-click the pipe while you stand on it): you "
           "slide in and pop out at the nearest other pipe of the same color, up to 50 blocks away.\n"
           "\n"
           "Two green pipes are a pair right away. Right-click with a dye to paint the whole pipe: that way you can "
           "place more pairs close together.",
        meer=["systemen/groene-reispijp", "verhalen/pad-guhs-kraam"]),
    "guhriobeloning_vlaggenmast": dict(
        nl="Een vlaggenmast om zelf te bouwen, te koop bij Pad-guh (4 stuks). Stapel de blokken op elkaar en ze worden "
           "vanzelf één mast: onderaan een stenen voet, bovenaan de gouden bol met het roze guh-vlaggetje. Het vlaggetje"
           " hangt naar de kant waar je vandaan keek.\n"
           "\n"
           "Rechtsklik ergens op de mast: het vlaggetje bovenaan viert feest met sterretjes en een fanfaretje. Vahoeg!",
        en="A flagpole to build with, for sale at Toad-guh's (4 pieces). Stack the blocks and they become one pole by "
           "themselves: a stone base at the bottom, the golden ball with the pink guh flag on top. The flag hangs to the"
           " side you looked from.\n"
           "\n"
           "Right-click the pole anywhere: the flag at the top celebrates with stars and a little fanfare. Wahoog!",
        meer=["verhalen/pad-guhs-kraam"]),
    "guhriobeloning_vraagblok": dict(
        nl="Een vraagtekenblok om zelf te bouwen, te koop bij Pad-guh. Spring er met je hoofd tegen de onderkant (of "
           "rechtsklik erop): de eerste keer van de dag springt er een kaasknabbel uit, alleen voor jou. Daarna zegt het"
           " blok alleen nog tok, tot morgen.\n"
           "\n"
           "Het is één knabbel per speler per dag, welk vraagtekenblok je ook raakt: een muur vol is mooi, maar geen "
           "knabbelboerderij.",
        en="A question block to build with, for sale at Toad-guh's. Jump against its underside with your head (or right-"
           "click it): the first time each day a cheese nibble pops out, just for you. After that the block only goes "
           "tonk, until tomorrow.\n"
           "\n"
           "It is one nibble per player per day, whichever question block you bump: a wall full of them looks nice, but "
           "it is no nibble farm.",
        meer=["verhalen/pad-guhs-kraam"]),
    "guhstation": dict(
        nl="Een spelkastje met een guh-snoet, de beloning van de eerste reeks op het Snuffeleiland. Zet het thuis neer "
           "en rechtsklik erop: er opent een klein schermpje met je snuffelrang en je geleerde geuren. Druk op start en "
           "je gaat als hond terug naar de laatste plek waar je op het eiland stond.\n"
           "\n"
           "Iedereen die al op het Snuffeleiland is geweest, kan elk Guhstation gebruiken. Ben je er nog nooit geweest, "
           "dan blijft het schermpje zwart.",
        en="A little game console with a guh snout, the reward of the first series on Sniff Island. Put it down at home "
           "and right-click it: a small screen opens with your sniffing rank and the scents you learned. Press start and"
           " you go back as a dog to the last spot you stood on the island.\n"
           "\n"
           "Anyone who has been to Sniff Island can use any Guhstation. Never been there? Then the little screen stays "
           "black.",
        meer=["systemen/guhstation", "dimensies/snuffeleiland"]),
    "guhteller": dict(
        nl="Een sensor die redstonetikjes telt. Hij heeft 1 vadskracht nodig.\n"
           "\n"
           "Elke keer dat er aan zijn achterkant een signaal aangaat telt hij er één bij. Bij de zoveelste geeft hij "
           "zelf een tikje (naar alle kanten behalve de achterkant) en begint hij weer bij nul. Zet er een Guhklok "
           "achter voor \"elke tiende keer\". Rechtsklik om te kiezen tot hoeveel hij telt (2 tot 64); sluip + rechtsklik "
           "zet de telling terug op nul. Een comparator leest hoe ver hij is.",
        en="A sensor that counts redstone ticks. It needs 1 chonk power.\n"
           "\n"
           "Every time a signal turns on at its back it counts one more. At the chosen number it gives a tick itself (to"
           " every side except the back) and starts again at zero. Put a Guh Clock behind it for \"every tenth time\". "
           "Right-click to choose how far it counts (2 to 64); sneak + right-click puts the count back to zero. A "
           "comparator reads how far it is.",
        meer=["systemen/sensoren", "systemen/vadskracht"]),
    "guhwaii_palm_gezicht": dict(
        nl="Een stuk guh-palmstam met een gezichtje, van de guh-palmen op Guhwai'i. Hak je hem om, dan krijg je hem "
           "gewoon mee. Rechtsklik het gezichtje: hij knipoogt en geeft je een hartje.\n"
           "\n"
           "Met een bijl strip je het gezichtje eraf (dan heb je een gestripte guh-palmstam), en in de werkbank wordt "
           "hij 4 guh-palmplanken.",
        en="A piece of guh palm log with a little face, from the guh palms of Guhwai'i. Chop it and you simply get it. "
           "Right-click the face: it winks and gives you a little heart.\n"
           "\n"
           "An axe strips the face off (you get a stripped guh palm log), and in the crafting table it becomes 4 guh "
           "palm planks.",
        meer=["biomen/guhwaii"]),
    "haarwasbak": dict(
        nl="Een roze wasbak met een gouden kraan, voor je eigen kapsalon. Rechtsklik hem: de kraan loopt en de bak vult "
           "zich met schuim en belletjes.\n"
           "\n"
           "Je maakt hem zelf, of je koopt hem bij Kapper Krulletje voor 4 krulmunten.",
        en="A pink basin with a golden tap, for your own hair salon. Right-click it: the tap runs and the basin fills "
           "with foam and bubbles.\n"
           "\n"
           "You craft it yourself, or buy it from Curly the Barber for 4 curl coins.",
        meer=["minigames/kapper"]),
    "haltepaaltje": dict(
        nl="Een halte voor het Bezorgguhtje. Zet het naast (of op) een kist of machine: het wijst vanzelf naar het blok "
           "met spullen. Het hoort bij het dichtstbijzijnde Stepstation binnen 96 blokken.\n"
           "\n"
           "Rechtsklik het om te kiezen: ophalen (groen bordje) of afleveren (oranje bordje), en welke spullen mee mogen"
           " (tot 9 soorten; leeg is alles). Met \"Ander station\" hang je het aan een ander Stepstation. Het werkt alleen"
           " waar jij zelf mag bouwen.",
        en="A stop for the Delivery Guhling. Put it next to (or on) a chest or machine: it points at the block with the "
           "items by itself. It belongs to the nearest Scooter Station within 96 blocks.\n"
           "\n"
           "Right-click it to choose: pick up (green sign) or drop off (orange sign), and which items may come along (up"
           " to 9 kinds; empty means everything). With \"Other Station\" you move it to another Scooter Station. It only "
           "works where you may build yourself.",
        meer=["systemen/bezorgguhtje"]),
    "blub_badje": dict(
        nl="Een klein roze badje met pootjes. Een Blub-guh in de buurt hupt er af en toe zelf in om te badderen: bubbels!\n"
           "\n"
           "- Woont er een Blub met het klusje Bubbelbadmeester in een guhhuisje vlakbij, dan zet hij het badje klaar met schuim.\n"
           "- Met schuim gaan tamme guhs er af en toe in. Ze worden er fris en blij van: een poosje vaker hartjes en blije dansjes.\n"
           "- Het schuim is na een paar minuten weg; dan schuimt de Blub het weer op.",
        en="A little pink tub on feet. A Blub-guh nearby hops in by itself now and then for a splash: bubbles!\n"
           "\n"
           "- If a Blub with the chore Bubble bath master lives in a guhhuisje nearby, it gets the bath ready with foam.\n"
           "- With foam, tame guhs get in now and then. They feel fresh and happy: hearts and happy little dances for a while.\n"
           "- The foam is gone after a few minutes; then the Blub foams it up again."),
    "doos_knabbels": dict(
        nl="Een kartonnen doos met kaasknabbels erin. Een guh kiest altijd de doos (ook als je een prachtig kattenhuisje "
           "hebt gebouwd).\n"
           "\n"
           "- 's Avonds of 's nachts: rechtsklik op de doos met een tamme guh van jou in de buurt. Hij kruipt erin en valt in "
           "slaap: ingestopt!\n"
           "- Wat je in je hand hebt geef je mee (je krijgt het terug als je wakker wordt). Dat bepaalt waar jullie van "
           "dromen: een knuffel, een sterrenlantaarn, een roze guh koek, een badeendje, een lampion...\n"
           "- Ga daarna zelf slapen in een bed vlakbij: je wordt wakker in Guhdromenland.\n"
           "- Sluip + rechtsklik: je guh weer uitstoppen.",
        en="A cardboard box with cheese nibbles in it. A guh always picks the box (even when you built a beautiful cat "
           "house).\n"
           "\n"
           "- In the evening or at night: right-click the box with a tame guh of yours nearby. It climbs in and falls asleep: "
           "tucked in!\n"
           "- What you hold goes along (you get it back when you wake up). It decides what you dream of: a plush, a star "
           "lantern, a pink guh cake, a rubber duck, a lantern...\n"
           "- Then go to sleep in a bed close by yourself: you wake up in Guh Dreamland.\n"
           "- Sneak + right-click: untuck your guh.",
        meer=[]),
    "droombrievenbus_sterren": dict(
        nl="Een brievenbusje bij het Droomposthuisje op het Droomplein, in Guhdromenland. Klik erop in een droom en "
           "Droompost bezorgt je met een grote zeepbel op het Sterrenkussenveld.\n"
           "\n"
           "- Alleen naar eilanden waar je al eens bent geweest (Droompost bezorgt alleen op bekende adressen).\n"
           "- Thuis doet het niks: dan komt er alleen reclame in.",
        en="A little mailbox by the Dream Post Office on the Dream Square, in Guh Dreamland. Click it in a dream and "
           "Dream Post delivers you to the Star Cushion Field in a big bubble.\n"
           "\n"
           "- Only to islands you have been to before (Dream Post only delivers to known addresses).\n"
           "- At home it does nothing: it only gets junk mail.",
        meer=[]),
    "droombrievenbus_molentje": dict(
        nl="Een brievenbusje bij het Droomposthuisje op het Droomplein, in Guhdromenland. Klik erop in een droom en "
           "Droompost bezorgt je met een grote zeepbel op het Maanmolentje.\n"
           "\n"
           "- Alleen naar eilanden waar je al eens bent geweest (Droompost bezorgt alleen op bekende adressen).\n"
           "- Thuis doet het niks: dan komt er alleen reclame in.",
        en="A little mailbox by the Dream Post Office on the Dream Square, in Guh Dreamland. Click it in a dream and "
           "Dream Post delivers you to the Moon Mill in a big bubble.\n"
           "\n"
           "- Only to islands you have been to before (Dream Post only delivers to known addresses).\n"
           "- At home it does nothing: it only gets junk mail.",
        meer=[]),
    "droombrievenbus_badje": dict(
        nl="Een brievenbusje bij het Droomposthuisje op het Droomplein, in Guhdromenland. Klik erop in een droom en "
           "Droompost bezorgt je met een grote zeepbel op het Wolkenbadje.\n"
           "\n"
           "- Alleen naar eilanden waar je al eens bent geweest (Droompost bezorgt alleen op bekende adressen).\n"
           "- Thuis doet het niks: dan komt er alleen reclame in.",
        en="A little mailbox by the Dream Post Office on the Dream Square, in Guh Dreamland. Click it in a dream and "
           "Dream Post delivers you to the Cloud Bath in a big bubble.\n"
           "\n"
           "- Only to islands you have been to before (Dream Post only delivers to known addresses).\n"
           "- At home it does nothing: it only gets junk mail.",
        meer=[]),
    "droombrievenbus_herinnering": dict(
        nl="Een brievenbusje bij het Droomposthuisje op het Droomplein, in Guhdromenland. Klik erop in een droom en "
           "Droompost bezorgt je met een grote zeepbel op het Herinneringseiland.\n"
           "\n"
           "- Alleen naar eilanden waar je al eens bent geweest (Droompost bezorgt alleen op bekende adressen).\n"
           "- Thuis doet het niks: dan komt er alleen reclame in.",
        en="A little mailbox by the Dream Post Office on the Dream Square, in Guh Dreamland. Click it in a dream and "
           "Dream Post delivers you to Memory Island in a big bubble.\n"
           "\n"
           "- Only to islands you have been to before (Dream Post only delivers to known addresses).\n"
           "- At home it does nothing: it only gets junk mail.",
        meer=[]),
    "droombrievenbus_liedjes": dict(
        nl="Een brievenbusje bij het Droomposthuisje op het Droomplein, in Guhdromenland. Klik erop in een droom en "
           "Droompost bezorgt je met een grote zeepbel op de Slaapliedjesheuvels.\n"
           "\n"
           "- Alleen naar eilanden waar je al eens bent geweest (Droompost bezorgt alleen op bekende adressen).\n"
           "- Thuis doet het niks: dan komt er alleen reclame in.",
        en="A little mailbox by the Dream Post Office on the Dream Square, in Guh Dreamland. Click it in a dream and "
           "Dream Post delivers you to the Lullaby Hills in a big bubble.\n"
           "\n"
           "- Only to islands you have been to before (Dream Post only delivers to known addresses).\n"
           "- At home it does nothing: it only gets junk mail.",
        meer=[]),
    "droombrievenbus_plein": dict(
        nl="Het brievenbusje van het Droomplein zelf, bij het Droomposthuisje. Klik erop in een droom: het vertelt naar welke "
           "eilanden Droompost je al kan bezorgen. Klik daarna op het brievenbusje van zo'n eiland om erheen te zweven.",
        en="The Dream Square's own little mailbox, by the Dream Post Office. Click it in a dream: it tells which islands "
           "Dream Post can already deliver you to. Then click the little mailbox of such an island to float there.",
        meer=[]),
    "kattenhuisje_roze": dict(
        nl="Een kattenhuisje met een rond deurtje en guh-oortjes op het dak. Je kunt er je guh in instoppen, net als in de "
           "doos met knabbels: 's avonds rechtsklikken met een tamme guh in de buurt, en dan zelf gaan slapen in een bed "
           "vlakbij. Er zijn ook witte, lavendel en mint kattenhuisjes.",
        en="A cat house with a round little door and guh ears on the roof. You can tuck your guh in here, just like in the "
           "box of nibbles: right-click in the evening with a tame guh nearby, then go to sleep in a bed close by. There "
           "are white, lavender and mint cat houses too.",
        meer=[]),
    "weer_vadser": dict(
        nl="Maakt zijn eigen weertje, tot 60 blokken in de rondte. Daarbuiten blijft het gewone weer. Hij heeft 20 vadskracht "
           "nodig zolang er een ander weertje dan normaal aanstaat; zonder vadskracht wordt het weer vanzelf weer normaal.\n"
           "\n"
           "- Rechtsklik: kies een weertje. Zon, regen (in koude streken sneeuw) en onweer kan altijd.\n"
           "- De guh-weertjes (pluisjessneeuw, bellenbui, knabbelbuitje, regenboogweer, hartjesregen, sterrenregen, knusmist "
           "en bloesemwind) maak je eerst los met een weerpotje. Het menu zegt waar je dat potje vindt.\n"
           "- Rechtsklik met een weerpotje: meteen ontgrendeld. Met het souvenir 'Regenboogje in een potje': het "
           "regenboogweer, en het souvenir houd je.",
        en="Makes its own weather, up to 60 blocks around. Outside that the normal weather stays. It needs 20 vadskracht "
           "while it makes any weather but normal; without vadskracht the weather goes back to normal by itself.\n"
           "\n"
           "- Right-click: pick a weather. Sun, rain (snow in cold places) and thunderstorm are always there.\n"
           "- The guh weathers (fluff snow, bubble shower, nibble drizzle, rainbow weather, heart rain, star shower, cosy fog "
           "and blossom breeze) need a weather jar first. The menu tells you where to find it.\n"
           "- Right-click with a weather jar: unlocked at once. With the souvenir 'Rainbow in a Jar': the rainbow weather, "
           "and you keep the souvenir."),
    "muziekdoosje": dict(
        nl="Een speeldoosje met een guhtje erop dat rondjes danst. Het speelt het slaapliedje van de Blub-guhs.\n" "\n" "- Rechtsklik: het liedje speelt (ongeveer 24 seconden). Alle guhs tot 8 blokken in de rondte gapen en vallen een voor een in slaap.\n" "- 's Avonds en 's nachts slapen ze tot de ochtend, overdag doen ze een dutje van een minuut.\n" "- Een tamme guh blijft niet liggen als jij ver wegloopt: dan loopt hij je achterna.\n" "- Zolang het liedje speelt, begint het niet nog een keer.",
        en="A little music box with a guh on top that twirls round. It plays the Blub-guhs' lullaby.\n" "\n" "- Right-click: the song plays (about 24 seconds). Every guh up to 8 blocks around yawns and falls asleep, one by one.\n" "- In the evening and at night they sleep until the morning, by day they take a nap of a minute.\n" "- A tame guh doesn't stay asleep when you walk far away: it follows you.\n" "- While the song plays, it doesn't start again."),
    "mobiel": dict(
        nl="Een mobiel met een maantje, een sterretje, een wolkje en een slapend guhtje. Hij draait rustig rond.\n" "\n" "- Hang hem boven een guhbedje.\n" "- Rechtsklik: hij draait sneller en speelt het slaapliedje. Guhs tot 6 blokken in de rondte vallen in slaap, net als bij het Muziekdoosje.",
        en="A mobile with a little moon, a star, a cloud and a sleeping guh. It turns slowly.\n" "\n" "- Hang it above a guh bed.\n" "- Right-click: it turns faster and plays the lullaby. Guhs up to 6 blocks around fall asleep, just like with the Music Box."),
    "nachtkastje": dict(
        nl="Een kastje voor naast het bed, met een lampje erop.\n" "\n" "- Rechtsklik: het lampje aan of uit.\n",
        en="A little table for beside the bed, with a lamp on top.\n" "\n" "- Right-click: the lamp on or off.\n"),
    "maanlampje_maan": dict(
        nl="Een zacht nachtlampje, zodat niemand bang is in het donker.\n" "\n" "- Rechtsklik: aan of uit.\n",
        en="A soft night light, so nobody is scared of the dark.\n" "\n" "- Right-click: on or off.\n"),
    "maanlampje_ster": dict(
        nl="Een zacht nachtlampje, zodat niemand bang is in het donker.\n" "\n" "- Rechtsklik: aan of uit.\n",
        en="A soft night light, so nobody is scared of the dark.\n" "\n" "- Right-click: on or off.\n"),
    "maanlampje_wolkje": dict(
        nl="Een zacht nachtlampje, zodat niemand bang is in het donker.\n" "\n" "- Rechtsklik: aan of uit.\n",
        en="A soft night light, so nobody is scared of the dark.\n" "\n" "- Right-click: on or off.\n"),
    "maanlampje_guh": dict(
        nl="Een zacht nachtlampje, zodat niemand bang is in het donker.\n" "\n" "- Rechtsklik: aan of uit.\n",
        en="A soft night light, so nobody is scared of the dark.\n" "\n" "- Right-click: on or off.\n"),
    "krabpaal": dict(
        nl="Een pluchen krabpaal met guh-oortjes.\n" "\n" "- Tamme guhs in de buurt hupsen er overdag af en toe bovenop, kijken rond als een poesje en zwaaien naar je.\n" "- Zet ze op 'zitten' als je liever hebt dat ze beneden blijven.",
        en="A plush scratching post with guh ears.\n" "\n" "- Tame guhs nearby now and then hop on top by day, look around like a kitty and wave at you.\n" "- Tell them to sit if you'd rather they stay down."),
    "krabpaal_blauw": dict(
        nl="Een pluchen krabpaal met guh-oortjes.\n" "\n" "- Tamme guhs in de buurt hupsen er overdag af en toe bovenop, kijken rond als een poesje en zwaaien naar je.\n" "- Zet ze op 'zitten' als je liever hebt dat ze beneden blijven.",
        en="A plush scratching post with guh ears.\n" "\n" "- Tame guhs nearby now and then hop on top by day, look around like a kitty and wave at you.\n" "- Tell them to sit if you'd rather they stay down."),
    "glas_restje": dict(
        nl="Een glas met een restje sap, cola, limonade, melk of thee: het handelsmerk van de Rommelguh. Welk drankje erin zit "
           "is een verrassing.\n"
           "\n"
           "- Rechtsklik met nog een glas: er komt een glas bij (tot 3).\n"
           "- Rechtsklik met een lege hand: een slokje. Er blijft natuurlijk altijd een restje over.",
        en="A glass with a little bit of juice, cola, lemonade, milk or tea left: the Rommelguh's trademark. Which drink is in "
           "it is a surprise.\n"
           "\n"
           "- Right-click with another glass: one more glass (up to 3).\n"
           "- Right-click with an empty hand: a sip. Of course there's always a little bit left."),
    "hapluikje": dict(
        nl="Een luikje met een bekje dat alles wat je erin stopt meteen in je Bankguh legt, hoe ver die ook weg staat.\n"
           "\n"
           "- Klik eerst met een Banksleutel op je Bankguh en dan op het Hapluikje: nu zijn ze gekoppeld.\n"
           "- Rechtsklik met spullen in je hand: zoveel als de Bankguh nog kan hebben gaat erin, de rest blijft in je "
           "hand.\n"
           "- Ook trechters, Knabbelbuizen en het Bezorgguhtje kunnen het voeren.\n"
           "- Rechtsklik met een lege hand: het luikje zegt hoe het ervoor staat.\n"
           "\n"
           "Het heeft 2 vadskracht nodig, anders slaapt het. Er komt nooit iets uit: het hapt alleen.",
        en="A little hatch with a mouth that puts everything you feed it straight into your Bank Guh, however far away "
           "it stands.\n"
           "\n"
           "- First click your Bank Guh with a Bank Key, then click the Nom Hatch: now they are linked.\n"
           "- Right-click with things in your hand: as much as the Bank Guh can still take goes in, the rest stays in "
           "your hand.\n"
           "- Hoppers, Nibble Tubes and the Delivery Guhling can feed it too.\n"
           "- Right-click with an empty hand: the hatch tells you how it is doing.\n"
           "\n"
           "It needs 2 chonk power, otherwise it sleeps. Nothing ever comes out: it only noms.",
        meer=["systemen/bank-guh-buikje", "systemen/vadskracht"]),
    "kappersstoel": dict(
        nl="Een roze kappersstoel op een glimmende voet. Rechtsklik hem met een lege hand om erop te gaan zitten. In de "
           "kappersshow nemen de klantjes van Kapper Krulletje hier plaats.\n"
           "\n"
           "Je maakt hem zelf, of je koopt hem bij Kapper Krulletje voor 4 krulmunten.",
        en="A pink barber chair on a shiny foot. Right-click it with an empty hand to sit on it. In the barber show, the"
           " customers of Curly the Barber sit here.\n"
           "\n"
           "You craft it yourself, or buy it from Curly the Barber for 4 curl coins.",
        meer=["minigames/kapper"]),
    "kippennestje": dict(
        nl="Een strooien nestje voor knabbelkippetjes. Wordt een knabbelkippetje binnen 10 blokken blij (twee soorten "
           "zorg op een dag), dan legt het zijn knabbelei in het nestje in plaats van op de grond. Er passen 3 eitjes "
           "in.\n"
           "\n"
           "Rechtsklik het nestje om alle knabbeleieren eruit te halen. Een comparator ziet hoeveel er in liggen.",
        en="A straw nest for nibble chicks. When a nibble chick within 10 blocks gets happy (two kinds of care in a "
           "day), it lays its nibble egg in the nest instead of on the ground. It holds 3 eggs.\n"
           "\n"
           "Right-click the nest to take all the nibble eggs out. A comparator sees how many are in it.",
        meer=["bouwwerken/guhboerderij"]),
    "knabbelaar": dict(
        nl="Een guhkop met twee grote tanden die het blok voor zijn snoet wegknabbelt, één blok per twee tellen. Hij "
           "bijt zo hard als een ijzeren houweel en de blokken komen in zijn negen vakjes (rechtsklik voor zijn scherm; "
           "buizen en guhs halen ze er ook uit).\n"
           "\n"
           "Kisten, machines en vloeistoffen laat hij met rust, net als beschermde gebouwen en andermans huisje. Passen "
           "de blokken er niet meer in, dan kijkt hij verbaasd. Hij vraagt 10 vadskracht.",
        en="A guh head with two big teeth that gnaws away the block in front of its snout, one block every two seconds. "
           "It bites as hard as an iron pickaxe and the blocks go into its nine slots (right-click for its screen; pipes"
           " and guhs can take them out too).\n"
           "\n"
           "Chests, machines and fluids it leaves alone, just like protected buildings and other players' houses. When "
           "the blocks no longer fit, it looks surprised. It needs 10 chonk power.",
        meer=["systemen/guhmachines", "systemen/vadskracht"]),
    "knabbelbuis_filter": dict(
        nl="Een Richtingstuk met een kieskeurig guhtje erin. Het heeft 2 vadskracht nodig; zonder vadskracht zit het "
           "dicht.\n"
           "\n"
           "Rechtsklik om zijn lijstje te openen: leg er voorbeelden in van wat erdoor mag (of zet het op \"Alles "
           "behalve\"). Het hapt 4 spullen tegelijk uit wat erachter zit en kan een aantal van elk achterlaten. Wat "
           "achter een Filterstuk zit krijgt die spullen het eerst: zo sorteer je. Het is ook het enige stuk dat spullen"
           " uit een Bankguh haalt. Sluip + rechtsklik met een lege hand draait het om.",
        en="A Direction Piece with a picky little guh inside. It needs 2 chonk power; without chonk power it is shut.\n"
           "\n"
           "Right-click to open its list: put in examples of what may pass (or set it to \"All except\"). It takes 4 items"
           " at a time out of what is behind it and can leave a number of each behind. Whatever is behind a Filter Piece"
           " gets those items first: that is how you sort. It is also the only piece that takes items out of a Bank Guh."
           " Sneak + right-click with an empty hand turns it around.",
        meer=["systemen/knabbelbuizen", "systemen/vadskracht"]),
    "knabbelbuis_richting": dict(
        nl="Een stuk Knabbelbuis met een pijl, zonder vadskracht. Tegen een kist (of iets anders met spullen erin) hapt "
           "het er steeds één ding uit en stuurt het de buis in. Tussen twee buizen is het eenrichtingsverkeer.\n"
           "\n"
           "Rechtsklik met een lege hand om te zien wat het doet; sluip + rechtsklik draait het om. Een redstonesignaal "
           "zet het op slot.",
        en="A piece of Nibble Tube with an arrow, without chonk power. Against a chest (or anything else that holds "
           "items) it takes one item out at a time and sends it into the tube. Between two tubes it is one-way traffic.\n"
           "\n"
           "Right-click with an empty hand to see what it is doing; sneak + right-click turns it around. A redstone "
           "signal locks it.",
        meer=["systemen/knabbelbuizen"]),
    "knabbelkorf": dict(
        nl="Een bijenkorf voor guhbijen (gewone bijen vinden hem ook fijn). De bijtjes vullen hem net als een gewone "
           "bijenkorf.\n"
           "\n"
           "Is de knabbelkorf vol, dan:\n"
           "- Rechtsklik met een schaar: er vallen 3 tot 5 Kaas Knabbels uit.\n"
           "- Rechtsklik met een glazen fles: je krijgt kaashoning.\n"
           "\n"
           "De bijtjes worden er niet boos om.",
        en="A beehive for guh bees (ordinary bees like it too). The bees fill it just like an ordinary beehive.\n"
           "\n"
           "When the nibble hive is full:\n"
           "- Right-click it with shears: 3 to 5 Cheese Nibbles drop out.\n"
           "- Right-click it with a glass bottle: you get cheese honey.\n"
           "\n"
           "The bees don't get angry about it.",
        meer=["diertjes/guh_bee"]),
    "knabbelportaal": dict(
        nl="Een lijstje van kaasknabbelblokken met een draaiend roze-geel kaaswolkje erin. Je zet het als 2 bij 2 neer; "
           "breek je een stukje, dan gaat het hele portaal mee.\n"
           "\n"
           "Rechtsklik erop en je ziet links je guhs thuis en rechts de guhs in De Eeuwige Knabbelvelden. Klik guhs aan "
           "(meerdere mag) en:\n"
           "- met de pijl naar rechts gaan ze met pensioen, waar ze ook zijn (ook ver weg of in een Guhrad). Is een guh "
           "ergens mee bezig, zoals wonen in een Guhhuisje, dan vraagt het portaal het eerst.\n"
           "- met de pijl naar links komen ze terug als opgepakte guh in je zakken (vol? dan bij het portaal).\n"
           "\n"
           "Met de knop Naar de Knabbelvelden ga je zelf kijken: daar zweven je guhs als lieve spookguhjes met een "
           "aureooltje rond. Aaien mag; sluip + rechtsklik om er een mee naar huis te nemen. Njeg, niet dood hoor, gewoon "
           "met pensioen!",
        en="A frame of Cheese Nibble blocks with a swirling pink-and-yellow cheese cloud inside. It is placed as 2 by 2; "
           "break one piece and the whole portal goes.\n"
           "\n"
           "Right-click it to see your guhs at home on the left and the guhs in the Eternal Nibble Fields on the right. "
           "Click guhs (several is fine) and:\n"
           "- the arrow to the right sends them into retirement, wherever they are (far away or in a Guh Wheel too). If a "
           "guh is busy, like living in a Guh House, the portal asks you first.\n"
           "- the arrow to the left brings them back as a picked-up guh in your pockets (full? then at the portal).\n"
           "\n"
           "The button To the Nibble Fields takes you there yourself: your guhs float around as sweet little ghosts with "
           "a halo. You can pet them; sneak + right-click to take one home. Nyeg, not dead at all, just retired!"),
    "knabbeloven": dict(
        nl="Een roze guh-oven om zelf te bakken. Rechtsklik erop: kies een deeg, een vorm en een topping, schuif het "
           "erin en haal het eruit op het goede moment. Laat je het te lang staan, dan verbrandt het.\n"
           "\n"
           "Thuis bak je met wat in je zakken zit: knabbelgraan (of tarwe), kaasmelk (of melk), knabbeleieren (of "
           "eieren), kaasknabbels, suiker, roze kleurstof of kruimels (brood of een koekje), net wat het recept vraagt. "
           "Perfect gebakken geeft 3 stuks, goed 2, rauw 1 en verbrand niets, njeg. Met verse ingrediënten uit het "
           "Knuffeldal krijg je er één extra, en met knabbelmeel in plaats van knabbelgraan bak je dubbel zoveel.",
        en="A pink guh oven to bake in yourself. Right-click it: choose a dough, a shape and a topping, slide it in and "
           "take it out at the right moment. Leave it in too long and it burns.\n"
           "\n"
           "At home you bake with what is in your pockets: nibble grain (or wheat), cheese milk (or milk), nibble eggs "
           "(or eggs), cheese nibbles, sugar, pink dye or crumbs (bread or a cookie), just what the recipe asks for. "
           "Perfectly baked gives 3 pieces, good 2, raw 1 and burnt nothing, nyeg. Fresh ingredients from Snuggledale "
           "give one extra, and with nibble flour instead of nibble grain you bake twice as much.",
        meer=["minigames/bakkerij"]),
    "knus_oorkonde": dict(
        nl="Een oorkonde met een gouden lijstje, voor de Knuffelburgemeester. Je krijgt hem na het Grote Knusfeest. Zet "
           "hem trots neer in je huis: rechtsklik hem en er klinkt een belletje, met hartjes en een bedankje van het "
           "hele Knuffeldal.",
        en="A certificate in a golden frame, for the Snuggle Mayor. You get it after the Great Cozyfest. Put it up "
           "proudly in your house: right-click it and a little bell rings, with hearts and a thank-you from all of "
           "Snuggledale.",
        meer=["verhalen/knusfeest"]),
    "knutselmachine": dict(
        nl="Een machine die voor je craft. Rechtsklik voor zijn scherm en leg er een Bouwtekening in (die maak je op de "
           "Tekentafel). In de negen voorraadvakjes stop jij, een buis of een guh de ingrediënten; hij neemt alleen wat "
           "de tekening vraagt.\n"
           "\n"
           "Zodra alles voor één keer craften er is, maakt hij het in twee tellen en legt hij het resultaat in de drie "
           "uitvakjes. Hij vraagt 10 vadskracht.",
        en="A machine that crafts for you. Right-click it for its screen and put a Blueprint in it (you make those on "
           "the Drawing Table). Into the nine supply slots you, a pipe or a guh put the ingredients; it only takes what "
           "the drawing asks for.\n"
           "\n"
           "As soon as everything for one craft is there, it makes it in two seconds and puts the result in the three "
           "out slots. It needs 10 chonk power.",
        meer=["systemen/guhmachines", "blokken/tekentafel"]),
    "kokosnoot": dict(
        nl="Een kokosnoot hangt onder het guh-palmblad en wordt langzaam rijp: van groen naar bruin. Beendermeel maakt "
           "hem sneller rijp. Rechtsklik een rijpe kokosnoot om hem te plukken; er groeit dan een nieuwe. Wacht je, dan "
           "valt hij soms vanzelf naar beneden. Bonk!\n"
           "\n"
           "Met de kokosnoot in je hand:\n"
           "- Rechtsklik op zand: hij wordt een kiemende kokosnoot, en daaruit groeit een guh-palm.\n"
           "- Rechtsklik onder een guh-palmblad: hij hangt er weer als nieuwe kokosnoot.\n"
           "- Of eet hem op, of geef hem aan je guh.",
        en="A coconut hangs under the guh palm leaves and slowly ripens: from green to brown. Bone meal ripens it "
           "faster. Right-click a ripe coconut to pick it; a new one then grows. If you wait, it sometimes falls down by"
           " itself. Bonk!\n"
           "\n"
           "With the coconut in your hand:\n"
           "- Right-click sand: it becomes a sprouting coconut, and a guh palm grows from it.\n"
           "- Right-click under guh palm leaves: it hangs there again as a new coconut.\n"
           "- Or eat it, or give it to your guh.",
        meer=["biomen/guhwaii"]),
    "krokante_grill": dict(
        nl="De grill waarop je Knabbelburgers bakt. Heb je een kaasknabbel en een brood bij je, rechtsklik dan op de "
           "grill: hij sist, en na ongeveer vijf tellen springt er één Knabbelburger bovenuit.\n"
           "\n"
           "Maar alleen als je het geheime recept kent! Dat leer je in het Sponsguh-verhaal. Zonder recept sist de grill"
           " alleen even. Er kan maar één burger tegelijk bakken.",
        en="The grill where you bake Nibble Patties. Carry a cheese nibble and a bread and right-click the grill: it "
           "sizzles, and after about five seconds one Nibble Patty pops out on top.\n"
           "\n"
           "But only if you know the secret recipe! You learn it in the Sponsguh story. Without the recipe the grill "
           "only sizzles a bit. Only one patty can bake at a time.",
        meer=["systemen/knabbelburgers-bakken", "systemen/sponsguh-verhaal"]),
    "luchtscooter": dict(
        nl="Speelgoed voor je guhs, een beloning uit het eerste Guhvatar-hoofdstuk: een draaiende bal van lucht op een "
           "houten voetje. Een guh in de buurt draaft erheen en rijdt er rondjes omheen op de wind. De guhs van je "
           "guhhuisje kiezen hem net als ander speelgoed. Rechtsklik zelf en er komt een windvlaag uit.",
        en="A toy for your guhs, a reward from the first Guhvatar chapter: a spinning ball of air on a little wooden "
           "base. A guh nearby trots over and rides round and round it on the wind. The guhs of your guh house pick it "
           "like any other toy. Right-click it yourself and a gust comes out.",
        meer=["systemen/speelgoed", "systemen/guhvatar-lucht"]),
    "neerzetter": dict(
        nl="Een machine die blokken neerzet. Rechtsklik voor zijn scherm en stop blokken in zijn negen vakjes (zaadjes "
           "en zaailingen mogen ook). Is de plek voor zijn snoet vrij, dan zet hij daar het eerste blok neer, één per "
           "tel, zoals een speler dat zou doen.\n"
           "\n"
           "Is de plek bezet, dan kijkt hij verbaasd. In beschermde gebouwen en andermans huisje zet hij niks neer. Hij "
           "vraagt 5 vadskracht.",
        en="A machine that places blocks. Right-click it for its screen and put blocks in its nine slots (seeds and "
           "saplings too). When the spot in front of its snout is free, it places the first block there, one per second,"
           " the way a player would.\n"
           "\n"
           "When the spot is taken it looks surprised. It places nothing in protected buildings or other players' "
           "houses. It needs 5 chonk power.",
        meer=["systemen/guhmachines", "systemen/vadskracht"]),
    "oog_van_sausron_beeldje": dict(
        nl="Een beeldje van het Oog van Sausron, een beloning aan het eind van In de ban van de Knabbelring. Het oog "
           "knippert vanzelf en geeft een beetje licht. Aai het (rechtsklik): het spint, er vliegen een paar vonkjes en "
           "boven je hotbar zie je een van zijn gedachten.",
        en="A statuette of the Eye of Sauceron, a reward at the end of The Lord of the Nibble Ring. The eye blinks all "
           "by itself and gives a little light. Pat it (right-click): it purrs, a few sparks fly and above your hotbar "
           "you see one of its thoughts.",
        meer=["systemen/knabbelring"]),
    "oogster": dict(
        nl="Een machine die je veld oogst. Hij kijkt naar het veld van 5 bij 5 voor zijn snoet, maait elk rijp gewas en "
           "plant het meteen weer in. De oogst komt in zijn negen vakjes: rechtsklik voor zijn scherm, of laat buizen en"
           " guhs hem legen.\n"
           "\n"
           "Past de oogst er niet meer in, dan wacht hij en kijkt hij verbaasd. Hij vraagt 8 vadskracht.",
        en="A machine that harvests your field. It watches the 5 by 5 field in front of its snout, cuts every ripe crop "
           "and plants it again right away. The harvest goes into its nine slots: right-click for its screen, or let "
           "pipes and guhs empty it.\n"
           "\n"
           "When the harvest no longer fits, it waits and looks surprised. It needs 8 chonk power.",
        meer=["systemen/guhmachines", "systemen/vadskracht"]),
    "opzuiger": dict(
        nl="Een guhmachine met een grote ronde snoet die alle losse spullen binnen 4 blokken naar zich toe trekt en "
           "opslurpt. Hij heeft 3 vadskracht nodig.\n"
           "\n"
           "Wat hij opzuigt gaat in zijn 9 vakjes; rechtsklik om erin te kijken. Een Richtingstuk, Filterstuk of "
           "trechter haalt de spullen eruit. Spullen die net zijn gevallen, of die in een beschermd gebouw of bij het "
           "Guhhuisje van iemand anders liggen, laat hij liggen.",
        en="A guh machine with a big round snout that pulls in all loose items within 4 blocks and slurps them up. It "
           "needs 3 chonk power.\n"
           "\n"
           "What it slurps goes into its 9 slots; right-click to look inside. A Direction Piece, Filter Piece or hopper "
           "takes the items out. Items that were only just dropped, or that lie in a protected building or near someone "
           "else's Guh House, it leaves alone.",
        meer=["systemen/knabbelbuizen", "systemen/vadskracht"]),
    "pai_sho_tafel": dict(
        nl="Een lage ronde tafel met een Pai Sho-bord, een beloning uit hoofdstuk 3 van de Guhvatar. Rechtsklik erop om "
           "te gaan zitten: klikt binnen 10 tellen een vriend op de tafel, dan speelt die tegen je, anders (of als je "
           "nog een keer klikt) speelt een guh tegen je.\n"
           "\n"
           "Om de beurt leg je een tegel; wie het eerst een lotus legt, wint. Anderen die op de tafel klikken kijken "
           "mee. Het spel blijft op de tafel bewaard.",
        en="A low round table with a Pai Sho board, a reward from chapter 3 of the Guhvatar. Right-click it to sit down:"
           " if a friend clicks the table within 10 seconds they play against you, otherwise (or if you click again) a "
           "guh plays against you.\n"
           "\n"
           "You take turns laying a tile; whoever lays a lotus first wins. Others who click the table watch. The game is"
           " saved with the table.",
        meer=["systemen/pai-sho", "systemen/guhvatar-aarde"]),
    "park_bankje": dict(
        nl="Een bankje met twee guh-oortjes op de leuning, net als in het Knabbelpark. Rechtsklik met een lege hand en "
           "je gaat erop zitten. Het draait met zijn voorkant naar je toe als je het neerzet.",
        en="A bench with two guh ears on the backrest, just like in Nibble Park. Right-click it with an empty hand and "
           "you sit down on it. It faces you when you place it.",
        meer=["bouwwerken/knabbelpark"]),
    "park_prullenbak": dict(
        nl="Een prullenbak met een guhgezichtje. Rechtsklik erop met iets in je hand en hij eet er één van op. NJEG!\n"
           "\n"
           "Mooie spullen spuugt hij weer uit: gereedschap, wapenrusting, dingen met een naam of betovering, en alles "
           "wat niet stapelt. Die blijven gewoon in je hand.",
        en="A litter bin with a guh face. Right-click it with something in your hand and it eats one of it. NYEG!\n"
           "\n"
           "Nice things it spits out again: tools, armour, things with a name or an enchantment, and anything that "
           "doesn't stack. Those simply stay in your hand.",
        meer=["bouwwerken/knabbelpark"]),
    "park_schommel": dict(
        nl="Een schommelzitje aan twee touwen. Hang hem onder een balk: de touwen gaan twee blokken omhoog. Rechtsklik "
           "met een lege hand en je gaat erop zitten; hij schommelt vanzelf heen en weer. Er kan één speler tegelijk op."
           "\n"
           "\n"
           "Je kunt hem maken, of kopen bij de Kraampjesguh in het Knabbelpark.",
        en="A swing seat on two ropes. Hang it under a bar: the ropes go two blocks up. Right-click it with an empty "
           "hand and you sit down; it swings back and forth by itself. One player at a time.\n"
           "\n"
           "You can craft it, or buy it from the Stall Guh in Nibble Park.",
        meer=["bouwwerken/knabbelpark"]),
    "park_wip": dict(
        nl="Een wip voor twee: een lange plank op een bok. Rechtsklik met een lege hand aan de kant waar je wilt zitten "
           "(is die bezet, dan krijg je de andere kant). Op en neer, op en neer: de twee kanten gaan altijd tegen elkaar"
           " in.\n"
           "\n"
           "Je kunt hem maken, of kopen bij de Kraampjesguh in het Knabbelpark.",
        en="A seesaw for two: a long plank on a trestle. Right-click it with an empty hand on the side where you want to"
           " sit (if that side is taken, you get the other one). Up and down, up and down: the two ends always move "
           "against each other.\n"
           "\n"
           "You can craft it, or buy it from the Stall Guh in Nibble Park.",
        meer=["bouwwerken/knabbelpark"]),
    "plantagebak": dict(
        nl="Een bak van 3 bij 3 vol aarde. Rechtsklik met een zaailing of zwammetje om hem erin te doen (of open het "
           "scherm met een lege hand). Hij plant er één in het midden en na 45 tellen staat er een echte boom.\n"
           "\n"
           "Rechtsklik met een bijl op de bak en de hele boom gaat in één keer om; de oogst komt in je zakken en "
           "uitgevallen zaailingen gaan terug in de bak. Daarna begint hij opnieuw. Hij vraagt 6 vadskracht.",
        en="A 3 by 3 box full of dirt. Right-click it with a sapling or a fungus to put it in (or open the screen with "
           "an empty hand). It plants one in the middle and after 45 seconds a real tree stands there.\n"
           "\n"
           "Right-click the box with an axe and the whole tree comes down in one go; the harvest lands in your pockets "
           "and saplings that fall out go back into the box. Then it starts again. It needs 6 chonk power.",
        meer=["systemen/guhmachines", "systemen/klusjes"]),
    "reuzenschelp": dict(
        nl="Een grote roze schelp. Is hij open, rechtsklik hem dan: je krijgt een parel en de schelp gaat dicht. Na een "
           "tijdje groeit er vanzelf een nieuwe parel en gaat hij weer open.\n"
           "\n"
           "Ze liggen in de Guhbubbel en onder water in het Bloesemmeertje. Die in de Guhbubbel blijven daar, maar een "
           "schelp uit het Bloesemmeertje neem je met een houweel mee naar je eigen huis. Met parels betaal je bij de "
           "Zeemeerguh.",
        en="A big pink shell. When it is open, right-click it: you get a pearl and the shell closes. After a while a new"
           " pearl grows by itself and it opens again.\n"
           "\n"
           "They lie in the Guh Bubble and under water in Blossom Lake. The ones in the Guh Bubble stay there, but a "
           "shell from Blossom Lake comes home with you with a pickaxe. Pearls are money for the Merguh.",
        meer=["bouwwerken/onderwater", "npcs/zeemeerguh"]),
    "ring_rustvuur": dict(
        nl="Een kookvuurtje met de pan van Sam-guh erop. Het geeft licht en brandt nooit aan je pootjes.\n"
           "\n"
           "Ben je op reis in In de ban van de Knabbelring, loop dan tot 4 blokken naar een rustvuurtje (of rechtsklik "
           "het): dan is het je rustpunt. Word je gezien of gepakt, dan kom je hier terug, en Sam-guh kookt een "
           "stoofpotje voor je (één keer per vuurtje per dag). Buiten de reis is het gewoon een gezellig vuurtje.",
        en="A cooking fire with Sam-guh's pan on it. It gives light and never burns your paws.\n"
           "\n"
           "When you are on the trip in The Lord of the Nibble Ring, walk up to a rest fire (within 4 blocks) or right-"
           "click it: it becomes your rest stop. If you get spotted or caught, you come back here, and Sam-guh cooks you"
           " a stew (once per fire per day). Outside the trip it is just a cozy fire.",
        meer=["systemen/rustpunten", "verhalen/knabbelring"]),
    "ringh3_rune": dict(
        nl="Een runensteen uit de Mijnen van Knabbelmoria. Als je dat verhaal uitspeelt, krijg je er vier.\n"
           "\n"
           "Thuis klop je erop (rechtsklik): dan krijgt hij het volgende teken, en je ziet welk teken het is. Er zijn "
           "acht tekens, en de njeg-rune geeft licht.",
        en="A runestone from the Mines of Nibblemoria. When you finish that story, you get four.\n"
           "\n"
           "At home you knock on it (right-click): it turns to the next sign, and you see which sign it is. There are "
           "eight signs, and the nyeg rune glows.",
        meer=["verhalen/ring-h3"]),
    "ringsausuman_pannantir": dict(
        nl="Een koekenpan op een standaard, vol saus die dingen laat zien. Aan het eind van het verhaal De Toren van "
           "Sausuman krijg je je eigen Pannantír. Rechtsklik hem om erin te kijken: hij laat je een klein visioen zien, "
           "en nooit twee keer achter elkaar hetzelfde.",
        en="A frying pan on a stand, full of sauce that shows things. At the end of the story The Tower of Sauceruman "
           "you get your own Pannantír. Right-click it to look into it: it shows you a little vision, never the same one"
           " twice in a row.",
        meer=["verhalen/ring-sausuman"]),
    "sauspomp": dict(
        nl="Zet hem bovenop een bronblok van kaassaus, kaasfrituursaus of water. Op 6 vadskracht slurpt hij er elke 2 "
           "seconden een emmer uit en duwt die door de Sausslang naar een Sausvat of een machine. De bron raakt nooit "
           "op, njeg!\n"
           "\n"
           "In zijn tankje passen 4 emmers; met een lege emmer kun je er ook zelf uit tappen.",
        en="Put it on top of a source block of cheese sauce, cheese frying sauce or water. On 6 chonk power it slurps up"
           " a bucket every 2 seconds and pushes it through the Sauce Hose to a Sauce Vat or a machine. The source never"
           " runs out, nyeg!\n"
           "\n"
           "Its little tank holds 4 buckets; with an empty bucket you can tap some out yourself too.",
        meer=["systemen/saus", "systemen/guh-technologie"]),
    "sausvat": dict(
        nl="Een vat met een guh-gezicht voor 16 emmers van één saus: kaassaus, kaasfrituursaus, water of melk. Giet er "
           "een emmer in of tap er een uit; pompen vullen het en machines slurpen eruit via een Sausslang. Het heeft "
           "geen vadskracht nodig.\n"
           "\n"
           "Door het raampje zie je hoeveel erin zit, en zijn gezicht slaapt als het leeg is en kijkt verrast als het "
           "vol is. Pak je het vat op, dan gaat de saus mee.",
        en="A vat with a guh face for 16 buckets of one sauce: cheese sauce, cheese frying sauce, water or milk. Pour a "
           "bucket in or tap one out; pumps fill it and machines slurp from it through a Sauce Hose. It needs no chonk "
           "power.\n"
           "\n"
           "Through the little window you see how much is inside, and its face sleeps when it is empty and looks "
           "surprised when it is full. Pick up the vat and the sauce comes along.",
        meer=["systemen/saus", "systemen/guh-technologie"]),
    "schuildoos": dict(
        nl="Een ronde kaasdoos met 27 vakjes. Rechtsklik om hem te openen. Breek je hem af, dan blijft alles erin "
           "zitten: zo neem je een volle doos in één keer mee. Een Schuildoos past niet in een andere Schuildoos (of in "
           "een shulkerdoos).",
        en="A round cheese box with 27 slots. Right-click it to open it. When you break it, everything stays inside: "
           "that way you take a full box along in one go. A Hidey Box does not fit inside another Hidey Box (or a "
           "shulker box).",
        meer=["systemen/schuildoos"]),
    "seizoensbloembak": dict(
        nl="Een bloembak die meebloeit met de seizoenen: bloesem in de lente, zonnebloemen in de zomer, oranje blaadjes "
           "in de herfst en sneeuw in de winter. Rechtsklik met een lege hand: hij vertelt wat je er nu mee kunt maken.\n"
           "\n"
           "Rechtsklik hem met:\n"
           "- Lente: 3 bloemetjes, voor een bloesemkransje.\n"
           "- Zomer: 3 tarwe, voor een zonnehoedje.\n"
           "- Herfst: 4 bladeren, voor 2 bladerhoopjes.\n"
           "- Winter: 3 wol voor een knus sjaaltje, of 3 sneeuwballen voor een sneeuwguhkopje.",
        en="A planter that blooms along with the seasons: blossom in spring, sunflowers in summer, orange leaves in fall"
           " and snow in winter. Right-click it with an empty hand: it tells you what you can make with it now.\n"
           "\n"
           "Right-click it with:\n"
           "- Spring: 3 flowers, for a blossom wreath.\n"
           "- Summer: 3 wheat, for a little sun hat.\n"
           "- Fall: 4 leaves, for 2 leaf piles.\n"
           "- Winter: 3 wool for a cozy scarf, or 3 snowballs for a Snowguh Head.",
        meer=["systemen/seizoenen"]),
    "snuffelsensor": dict(
        nl="Een neus die ruikt wie er in de buurt is. Hij heeft 1 vadskracht nodig.\n"
           "\n"
           "Ruikt hij iemand, dan geeft hij een redstonesignaal. Rechtsklik om te kiezen waar hij op let: guhs, Mika's, "
           "spelers of iedereen. Sluip + rechtsklik verandert hoe ver hij ruikt: 2, 4 of 8 blokken. Een comparator leest"
           " hoeveel hij er ruikt (tot 15).",
        en="A nose that smells who is nearby. It needs 1 chonk power.\n"
           "\n"
           "When it smells someone it gives a redstone signal. Right-click to choose what it sniffs for: guhs, Mikas, "
           "players or everyone. Sneak + right-click changes how far it smells: 2, 4 or 8 blocks. A comparator reads how"
           " many it smells (up to 15).",
        meer=["systemen/sensoren", "systemen/vadskracht"]),
    "stepstation": dict(
        nl="Het huisje van een Bezorgguhtje. Geef het station vadskracht en zet Haltepaaltjes neer, tot 96 blokken ver "
           "(hooguit 8 per station): het Bezorgguhtje haalt spullen op en brengt ze rond op zijn stepje.\n"
           "\n"
           "Rechtsklik het station: je ziet zijn rugzakje (9 vakjes) en de lijst met haltes.",
        en="The home of a Delivery Guhling. Give the station chonk power and put down Stop Posts, up to 96 blocks away "
           "(at most 8 per station): the Delivery Guhling picks items up and brings them round on his scooter.\n"
           "\n"
           "Right-click the station: you see his backpack (9 slots) and the list of stops.",
        meer=["systemen/bezorgguhtje", "systemen/vadskracht"]),
    "tekentafel": dict(
        nl="Hier maak je Bouwtekeningen voor de Knutselmachine. Rechtsklik voor het scherm: leg een recept in het "
           "rooster van 3 bij 3 en een Bouwtekening in het vakje voor vellen, en pak de getekende Bouwtekening eruit.\n"
           "\n"
           "Tekenen kost alleen het vel: alles van het rooster krijg je terug als je het scherm sluit. Een oude tekening"
           " wordt overgetekend. Hij heeft geen vadskracht nodig.",
        en="This is where you make Blueprints for the Tinker Machine. Right-click it for the screen: lay a recipe in the"
           " 3 by 3 grid and a Blueprint in the sheet slot, and take out the drawn Blueprint.\n"
           "\n"
           "Drawing only costs the sheet: everything on the grid comes back when you close the screen. An old drawing is"
           " drawn over. It needs no chonk power.",
        meer=["systemen/guhmachines", "blokken/knutselmachine"]),
    "theepotje": dict(
        nl="Een theepotje met een guhgezichtje op het deksel. Rechtsklik het met iets lekkers en je krijgt twee kopjes "
           "thee:\n"
           "- Kaas Knabbels: knabbelthee (je wordt gezellig).\n"
           "- Kaasmelk: kaasmelkthee (nare effecten weg).\n"
           "- Theekruid: theekruidthee (je herstelt even).\n"
           "- Guhbloemetjes: guhbloementhee (lang gezellig).\n"
           "\n"
           "Zet of drink alle vier de theesoorten!",
        en="A teapot with a little guh face on its lid. Right-click it with something yummy and you get two cups of tea:"
           "\n"
           "- Cheese Nibbles: nibble tea (you feel snug).\n"
           "- Cheese Milk: cheese milk tea (bad effects gone).\n"
           "- Tea Herb: herb tea (you heal for a bit).\n"
           "- Little Guh Flowers: guh flower tea (snug for a long time).\n"
           "\n"
           "Make or drink all four kinds of tea!",
        meer=["systemen/ba-sing-guh"]),
    "theeset": dict(
        nl="De theeset van Oom Miroh, een beloning uit hoofdstuk 3 van de Guhvatar. Rechtsklik voor een kopje "
           "jasmijnthee: je krijgt even Regeneratie, en anderhalve minuut Haast en Gezellig. Daarna moet de thee 3 "
           "minuten trekken voor je volgende kopje.\n"
           "\n"
           "Zet je hem binnen je eigen guhhuisje, dan komt het team op de thee als jij in de buurt bent.",
        en="Uncle Miroh's Tea Set, a reward from chapter 3 of the Guhvatar. Right-click it for a cup of jasmine tea: you"
           " get a moment of Regeneration, and a minute and a half of Haste and Snug. Then the tea needs 3 minutes to "
           "steep before your next cup.\n"
           "\n"
           "Put it inside your own guh house and the team comes over for tea when you are nearby.",
        meer=["systemen/guhvatar-aarde", "systemen/guhhuisje"]),
    "torenpeper_vuurtorenlamp": dict(
        nl="De lamp van de Rookguh-vuurtoren, die je ook zelf kunt maken. Hij brandt vanzelf, met twee draaiende stralen"
           " vonkjes, zolang er binnen 96 blokken een speler is die in het verhaal De Rookguh-vuurtoren de lamp heeft "
           "aangestoken. Is niemand zo dichtbij, dan is hij donker.",
        en="The lamp of the Smoke Guh Lighthouse, which you can also craft yourself. It lights up by itself, with two "
           "turning beams of sparks, as long as a player who lit the lamp in the story The Smoke Guh Lighthouse is "
           "within 96 blocks. If nobody like that is close by, it stays dark.",
        meer=["verhalen/vuurtoren"]),
    "vadsmolen": dict(
        nl="Het grote zusje van het Guh-molentje, twee blokken hoog. Rechtsklik voor haar scherm en leg er iets in om te"
           " malen:\n"
           "- Knabbelgraan wordt Knabbelmeel, in een tel.\n"
           "- Botten worden beendermeel, suikerriet wordt suiker.\n"
           "- Ook grillspiezen, blazestaven, keien en grind kan ze malen.\n"
           "\n"
           "Ze draait op vadskracht in plaats van wind, dus ze maalt altijd even hard. Is haar zak vol, dan kijkt ze "
           "verbaasd. Ze vraagt 5 vadskracht.",
        en="The big sister of the Guh Windmill, two blocks high. Right-click it for its screen and put something in to "
           "grind:\n"
           "- Nibble Grain becomes Nibble Flour, in a second.\n"
           "- Bones become bone meal, sugar cane becomes sugar.\n"
           "- It also grinds grill skewers, blaze rods, cobblestone and gravel.\n"
           "\n"
           "It runs on chonk power instead of wind, so it always grinds just as fast. When its sack is full it looks "
           "surprised. It needs 5 chonk power.",
        meer=["systemen/guhmachines", "systemen/vadskracht"]),
    "voorraadmeter": dict(
        nl="Telt wat er in de kist, de machine of de Bankguh achter hem zit (of anders eronder of erboven). Hij heeft 1 "
           "vadskracht nodig.\n"
           "\n"
           "Rechtsklik voor zijn scherm: leg er een voorbeeld in en kies een getal. Zijn er minstens zoveel van dat "
           "ding, dan geeft hij een redstonesignaal. Zonder voorbeeld telt hij alles. Zet je hem op \"Minder dan\", dan "
           "geeft hij juist een signaal als er minder is. Een comparator leest hoe ver de telling op weg is naar je "
           "getal.",
        en="Counts what is in the chest, machine or Bank Guh behind it (or otherwise below or above it). It needs 1 "
           "chonk power.\n"
           "\n"
           "Right-click for its screen: put an example in and choose a number. When there are at least that many of that"
           " thing, it gives a redstone signal. Without an example it counts everything. Set it to \"Fewer than\" and it "
           "gives a signal when there are fewer instead. A comparator reads how far the count is on its way to your "
           "number.",
        meer=["systemen/sensoren", "systemen/vadskracht"]),
    "vredesballon": dict(
        nl="De Vredesballon in het klein, op een voetje: de beloning van het verhaal van de Vredesballon. Zet hem neer "
           "en een quest is meteen af. Rechtsklik erop en er dwarrelt een kruimeltje knabbel uit het mandje.",
        en="The Peace Balloon in miniature, on a little plinth: the reward of the Peace Balloon story. Place it and a "
           "quest is done at once. Right-click it and a tiny crumb of nibble floats out of the basket.",
        meer=["systemen/vredesballon"]),
    "wilde_guhweerder": dict(
        nl="Een roze bordje met guhoortjes en een slapend guhgezichtje. Zet het in je huis of basis: in de area eromheen"
           " komen geen wilde guhs meer tevoorschijn. Je eigen guhs, babyguhs en guhs uit een spawn-ei mogen gewoon "
           "komen.\n"
           "\n"
           "Rechtsklik het om de area te kiezen (8, 16, 24, 32 of 48 blokken om het bordje heen; eerst 16) en om hem te "
           "laten zien als een blauwe koepel die alleen jij ziet. Alleen wie het bordje neerzette kan het afbreken.",
        en="A pink sign with guh ears and a sleeping guh face. Put it in your house or base: in the area around it no "
           "more wild guhs pop up. Your own guhs, baby guhs and guhs from a spawn egg may still come.\n"
           "\n"
           "Right-click it to choose the area (8, 16, 24, 32 or 48 blocks around the sign; 16 at first) and to show it "
           "as a blue dome only you can see. Only whoever put the sign down can break it.",
        meer=["systemen/guhhuisje"]),
    "wolkenbank": dict(
        nl="Een bankje van wolk. Rechtsklik met een lege hand en je gaat erop zitten. Hij draait met zijn voorkant naar "
           "je toe als je hem neerzet.",
        en="A bench made of cloud. Right-click it with an empty hand and you sit down on it. It faces you when you place"
           " it.",
        meer=["systemen/bouwblokken-biomen"]),
    "wolkenbed": dict(
        nl="Een echt bed van wolk, twee blokken lang. Rechtsklik er 's nachts op om te slapen: de nacht gaat voorbij en "
           "het bed wordt je startplek, net als een gewoon bed.\n"
           "\n"
           "Waar een gewoon bed zou ontploffen (de Guhbarbecuether, de Guhmaag) zegt het wolkenbed alleen dat je hier "
           "niet kunt slapen. Erop vallen doet nooit pijn.",
        en="A real bed made of cloud, two blocks long. Right-click it at night to sleep: the night passes and the bed "
           "becomes your spawn point, just like an ordinary bed.\n"
           "\n"
           "Where an ordinary bed would explode (the Guhbarbecuether, the Guhbelly) the Cloud Bed only says you can't "
           "sleep here. Landing on it never hurts.",
        meer=["systemen/bouwblokken-biomen"]),
    "safaripoort": dict(
        nl="De grote houten poort van het safaripark: 4 blokken breed en 6 hoog, met een leeuwenschild met guh-oogjes "
           "en roze oortjes. In het park staat er een op elke plek waar de truckroute van de ene zone naar de andere "
           "gaat.\n"
           "\n"
           "- Rechtsklik: open of dicht. Een open poort gaat na 10 seconden vanzelf weer dicht.\n"
           "- Komt er een safaritruck aan, dan zwaaien de deuren op tijd open. Ze gaan pas dicht als de truck en de "
           "guhs die ernaast meerennen erdoor zijn.\n"
           "- Spelers en guhs mogen door een open poort, de dieren nooit: zo blijft elk dier in zijn eigen zone.",
        en="The big wooden gate of the safari park: 4 blocks wide and 6 high, with a lion shield with guh eyes and pink "
           "ears. In the park there is one wherever the truck route goes from one zone into another.\n"
           "\n"
           "- Right-click: open or shut. An open gate shuts itself again after 10 seconds.\n"
           "- When a safari truck comes, the doors swing open in time. They only shut once the truck and the guhs "
           "running alongside it are through.\n"
           "- Players and guhs may go through an open gate, the animals never: that way every animal stays in its own "
           "zone."),
    "infobord": dict(
        nl="Een bordje op een paaltje, zoals bij de verblijven en terraria in het safaripark. Rechtsklik erop en het "
           "bordje vertelt je wat over het dier dat daar woont, met de tekst uit de Guhdex.\n"
           "\n"
           "Een bordje dat je zelf maakt hoort nog bij geen enkel dier: dat staat dan geduldig te wachten.",
        en="A little sign on a post, like the ones at the homes and terraria in the safari park. Right-click it and the "
           "sign tells you something about the animal that lives there, with the text from the Guhdex.\n"
           "\n"
           "A sign you craft yourself doesn't belong to an animal yet: it just waits patiently."),
    "nepkoraal": dict(
        nl="Koraal dat nooit doodgaat, in of uit het water. Rechtsklik erop met een lege hand en het krijgt de "
           "volgende kleur: roze, geel, blauw, oranje en paars. Het geeft ook een klein beetje licht.",
        en="Coral that never dies, in or out of water. Right-click it with an empty hand to give it the next colour: "
           "pink, yellow, blue, orange and purple. It also gives off a little light."),
    "ontwerpbureau": dict(
        nl="Een tekentafel met een blauw vel erop: de plek voor blauwdrukken. Rechtsklik erop en blader door je eigen "
           "blauwdrukken, de catalogus (25 kant-en-klare ontwerpen, van guh-huisje tot vuurtoren) en de bibliotheek "
           "van de server.\n"
           "\n"
           "- Print: je krijgt een blauwdruk mee (kost een vel papier). Klik ermee op een blok en het ontwerp staat er "
           "als spook, waar je ook bent.\n"
           "- In de bibliotheek geef je duimpjes aan de mooiste ontwerpen.\n"
           "- Eigen blauwdrukken maak je met de vormstaf (op Vastleggen). Publiceren doe je in een Bouwkeet, vanaf een Sauskeet.",
        en="A drawing desk with a blue sheet on it: the place for blueprints. Right-click it and browse your own "
           "blueprints, the catalogue (25 ready designs, from guh cottage to lighthouse) and the server's library.\n"
           "\n"
           "- Print: you get a blueprint to take along (costs a sheet of paper). Click a block with it and the design "
           "stands there as a ghost, wherever you are.\n"
           "- In the library you give thumbs up to the nicest designs.\n"
           "- You make your own blueprints with the Shape Wand (set to Capture). Publishing happens in a Building "
           "Shed, from a Sauce Shed on."),
    "bouwkeet": dict(
        nl="Een geel-roze keetje van 3 bij 3 bij 3 met een kraantje op het dak: hier begint het bouwen. Rechtsklik "
           "erop voor het menu met tabbladen (vormen, daken, blauwdrukken, bouwguhs, materiaal en niveau).\n"
           "\n"
           "- Kies iets om te bouwen en de keet zet er een spook van neer: een doorzichtig voorbeeld. Schuif, draai of "
           "spiegel het tot het goed staat, en druk op Bouw!\n"
           "- De blokjes komen uit je inventaris, daarna uit een gekoppelde Bank Guh (klik met een Banksleutel op de "
           "keet). Mist er iets, dan zie je precies wat, en kun je toch bouwen: wat mist blijft spook.\n"
           "- Met een upgrade wordt de keet beter (bouwguhs, draadloos materiaal, Pof!), maar dan trekt hij ook "
           "vadskracht.\n"
           "- De keet bouwt tot 64 blokken ver, en 32 verder per niveau (tot 192). Wat verder ligt wordt rood en "
           "blijft spook; schuif het dichterbij of maak de keet een niveau hoger.\n"
           "- Lijstje geeft je een boek met alle blokken die nog ontbreken, met naam en aantal.\n"
           "- Je ziet hoe lang het nog duurt. Het spook blijft staan als je het menu sluit.\n"
           "- Je laatste 5 bouwacties kun je terugdraaien.",
        en="A yellow and pink shed of 3 by 3 by 3 with a little crane on the roof: this is where building starts. "
           "Right-click it for the menu with tabs (shapes, roofs, blueprints, builder guhs, materials and level).\n"
           "\n"
           "- Pick something to build and the shed puts down a ghost of it: a see-through preview. Move, turn or "
           "mirror it until it's right, and press Build!\n"
           "- The blocks come from your inventory, then from a linked Bank Guh (click the shed with a Bank Key). If "
           "something is missing you see exactly what, and you can build anyway: what's missing stays a ghost.\n"
           "- An upgrade makes the shed better (builder guhs, wireless materials, Poof!), but then it also draws "
           "chonk power.\n"
           "- The shed builds up to 64 blocks away, and 32 further per level (up to 192). What lies further turns red "
           "and stays a ghost; move it closer or raise the shed a level.\n"
           "- List gives you a book with every block that is still missing, with its name and count.\n"
           "- You see how long it still takes. The ghost stays when you close the menu.\n"
           "- You can undo your last 5 building actions."),
    # 1.7.0 "Rondje Guhmension": the golf shop's deco and the golf trophy (tools/features/golf18.py)
    "golf_vlaggetje": dict(
        nl="Deco uit het golfwinkeltje van de Golfguh, voor 6 golfballetjes: een hole met een roze guh-vlaggetje, voor "
           "je eigen kleine green. Rechtsklik erop: vlag eruit, en nog een keer: vlag erin.",
        en="Decoration from the Golfguh's golf shop, for 6 golfballetjes: a hole with a pink guh flag, for your own little "
           "green. Right-click it: flag out, and again: flag back in.",
        meer=["minigames/golf"]),
    "golfbal_tee": dict(
        nl="Deco uit het golfwinkeltje van de Golfguh: twee golfballetjes op een tee voor 3 golfballetjes. Zet er tot 4 "
           "op één blok, net als kaarsjes: klik met nog een balletje op een blok dat er al staat. Echte golfballetjes om "
           "mee te betalen zijn het niet.",
        en="Decoration from the Golfguh's golf shop: two golf balls on tees for 3 golfballetjes. Put up to 4 on one block, "
           "like candles: click a block that already has some with another ball. They are not golfballetjes you can pay with.",
        meer=["minigames/golf"]),
    "golfkarretje": dict(
        nl="Het golfkarretje uit het golfwinkeltje van de Golfguh, voor 64 golfballetjes. Rechtsklik met een lege hand "
           "om erin te gaan zitten (sluipen: uitstappen). Rijden doet hij niet: het is deco.",
        en="The golf cart from the Golfguh's golf shop, for 64 golfballetjes. Right-click it with an empty hand to sit in "
           "it (sneak to get out). It doesn't drive: it is decoration.",
        meer=["minigames/golf"]),
    "golfrek": dict(
        nl="Deco uit het golfwinkeltje van de Golfguh, voor 10 golfballetjes: een roze golftas met guhgolfclubs en twee "
           "balletjes, om tegen de muur te zetten. Verder doet hij niks: echt golfen doe je met de club van de Golfguh.",
        en="Decoration from the Golfguh's golf shop, for 10 golfballetjes: a pink golf bag with guh golf clubs and two balls, "
           "to put against a wall. It does nothing else: real golfing is done with the Golfguh's club.",
        meer=["minigames/golf"]),
    "golftrofee": dict(
        nl="Niet te koop: de Golfguh geeft hem één keer, als je op een golfbaan met 18 holes alle 18 holes op of onder "
           "par speelt. Op de trofee staan je naam en je record. Zet hem neer en rechtsklik erop: dan lees je wie hem "
           "won en met hoeveel slagen. Breek je hem af, dan houdt hij zijn naam en record.",
        en="Not for sale: the Golfguh gives it once, when you play all 18 holes of an 18-hole golf course at or under par. "
           "Your name and your record are on it. Put it down and right-click it to read who won it and in how many strokes. "
           "Break it and it keeps its name and record.",
        meer=["minigames/golf"]),
    # <minigames2> 1.7.0 Onder de Wol: the deco of the Wisselgrot and the Kegelgrot (tools/features/mijnspellen.py)
    "ertskarretje": dict(
        nl="Deco uit het winkeltje van Conducteur Piepwiel in de Wisselgrot, voor 64 ertsbonnetjes: een mijnkarretje vol "
           "glinsterende ertsen. Rechtsklik met een lege hand om erin te gaan zitten (sluipen: uitstappen). Rijden doet hij niet.",
        en="Decoration from Conducteur Piepwiel's shop in the Switch Cave, for 64 ore tickets: a mine cart full of glittering "
           "ore. Right-click it with an empty hand to sit in it (sneak to get out). It doesn't ride."),
    "stationsklokje": dict(
        nl="Deco uit het winkeltje van Conducteur Piepwiel, voor 24 ertsbonnetjes: een stationsklok op een paaltje. Het "
           "is altijd vijf over guh. Verder doet hij niks.",
        en="Decoration from Conducteur Piepwiel's shop, for 24 ore tickets: a station clock on a little pole. It is always "
           "five past guh. It does nothing else."),
    "seinlampje": dict(
        nl="Deco uit het winkeltje van Conducteur Piepwiel, voor 20 ertsbonnetjes: een seinlamp, rood boven groen. Hij "
           "geeft licht; verder doet hij niks.",
        en="Decoration from Conducteur Piepwiel's shop, for 20 ore tickets: a signal lamp, red over green. It gives light; "
           "it does nothing else."),
    "kegeltje": dict(
        nl="Deco uit het winkeltje van De Guhde in de Kegelgrot, voor 20 kegelknopen: een pluchen guh-kegeltje, net zo "
           "een als op de banen. Hier valt hij niet om.",
        en="Decoration from The Guhde's shop in the Bowling Cave, for 20 pin buttons: a plush guh pin, just like the ones on "
           "the lanes. It doesn't fall over here."),
    "wolbalrek": dict(
        nl="Deco uit het winkeltje van De Guhde, voor 40 kegelknopen: een rekje met drie wolballen. Gooien kun je er niet "
           "mee: bowlen doe je met de geleende bal van De Guhde.",
        en="Decoration from The Guhde's shop, for 40 pin buttons: a rack with three wool balls. You can't throw them: you "
           "bowl with The Guhde's loaned ball."),
    "neon_kegelbord": dict(
        nl="Deco uit het winkeltje van De Guhde, voor 32 kegelknopen: een neonbord met een kegel en een bal. Het geeft "
           "licht; verder doet het niks.",
        en="Decoration from The Guhde's shop, for 32 pin buttons: a neon sign with a pin and a ball. It gives light; it "
           "does nothing else."),
    # </minigames2>
}

LINK = {
    "beroepen_knabbelbuit": ["verhalen/beroepen"],
    "bieb_boekaltaar": ["bouwwerken/guhbibliotheek"],
    "campingmarkt_haring": ["verhalen/camping", "bouwwerken/grillcamping"],
    "campingmarkt_kampeerplek": ["verhalen/camping", "bouwwerken/grillcamping"],
    "campingmarkt_vadsstapel": ["verhalen/ruilmarkt", "bouwwerken/mika_ruilmarkt"],
    "disco_milkshake": ["bouwwerken/guh_disco", "minigames/disco"],
    "elftocht_kopjes": ["minigames/elftocht"],
    "fossielmijn_skeletrek": ["bouwwerken/fossiel_opgraving"],
    "gedenksteen": ["systemen/geestenwereld"],
    "glijbaan_start": ["minigames/glijbanen", "bouwwerken/knuffelbad"],
    "grote_knabbelmachine": ["verhalen/knabbelmachine"],
    "guhrio_deur": ["systemen/super-guhrio-spelen", "verhalen/super-guhrio"],
    "guhrio_pijp": ["systemen/super-guhrio-spelen", "verhalen/super-guhrio"],
    "guhrio_poort": ["systemen/super-guhrio-spelen", "verhalen/super-guhrio"],
    "guhrio_startblok": ["systemen/super-guhrio-spelen", "verhalen/super-guhrio"],
    "guhriobeloning_scorebord": ["bouwwerken/guhrio_kasteel", "systemen/super-guhrio-spelen"],
    "guhriow2_warppijp": ["systemen/super-guhrio-spelen", "verhalen/super-guhrio"],
    "guhwaii_rommeltje": ["verhalen/ohana"],
    "kaaskluis": ["bouwwerken/kaasmijn"],
    "karen": ["systemen/sponsguh-verhaal", "bouwwerken/bikinibodem"],
    "karretjesautomaat": ["bouwwerken/kaasmijn"],
    "lotuspuzzeltegel": ["systemen/geest-spel", "systemen/geestenwereld"],
    "meditatiesteen": ["systemen/geestenwereld", "systemen/geest-spel"],
    "mewtwo_kloontank": ["verhalen/kloon-eiland", "bouwwerken/kloon_eiland"],
    "mewtwo_knabbelschaal": ["verhalen/kloon-eiland", "bouwwerken/kloon_eiland"],
    "mewtwo_notitieplek": ["verhalen/kloon-eiland", "bouwwerken/kloon_eiland"],
    "mewtwo_onderdelenkist": ["verhalen/kloon-eiland", "bouwwerken/kloon_eiland"],
    "park_poppenkast": ["bouwwerken/knabbelpark"],
    "park_verrekijker": ["bouwwerken/knabbelpark", "bouwwerken/guh_sterrenwacht"],
    "park_wensstam": ["bouwwerken/knabbelpark"],
    "regenboogbrug_pot": ["bouwwerken/regenboogbrug"],
    "ringh3_hendel": ["verhalen/ring-h3", "bouwwerken/knabbelmoria"],
    "ringh6_kooislot": ["verhalen/ring-h6", "bouwwerken/frituurberg"],
    "torenpeper_kweekbak": ["verhalen/pepertuin", "bouwwerken/pepertuin"],
    "wolkenkasteeltje_schat": ["bouwwerken/wolkenkasteeltje"],
}

# 1.6.0 Op Safari! deel 2 (F11): the building blocks of tools/features/bouwhulp_blokken.py. The roofs, frames, shutters
# and benches come in many materials and colours: one text for each kind.
_DAK = dict(
    nl="Een stukje schuin dak. Je legt het met je gezicht naar de hoge kant. Bouw je verder omhoog de helling op, dan "
       "kiest elk nieuw stukje zelf het volgende deel, zodat het dak mooi doorloopt.\n"
       "\n"
       "- Halve helling: 2 delen per blok hoogte.\n"
       "- Kwart helling: 4 delen per blok hoogte, voor een heel flauw dak.\n"
       "\n"
       "Klopt een stukje net niet? Rechtsklik erop en het wordt het volgende deel.",
    en="A piece of sloped roof. Place it facing the high side. Keep building up the slope and every new piece picks the "
       "next part by itself, so the roof runs on nicely.\n"
       "\n"
       "- Half slope: 2 parts per block of height.\n"
       "- Quarter slope: 4 parts per block of height, for a very gentle roof.\n"
       "\n"
       "A piece not quite right? Right-click it and it becomes the next part.")
_KOZIJN = dict(
    nl="Een raamkozijn voor je guhhuisje. De buitenkant wijst naar jou als je het neerzet. Rechtsklik erop met een lege "
       "hand voor de volgende versiering: oortjes, een bloembak, gordijntjes of helemaal kaal.",
    en="A window frame for your guh house. The outside faces you when you place it. Right-click it with an empty hand "
       "for the next decoration: little ears, a window box, curtains or nothing at all.")
_LUIK = dict(
    nl="Een luik met een hartje erin. Zet het tegen de muur naast of voor je raam. Rechtsklik erop en het zwaait open, "
       "net als een deur; nog een keer en het gaat weer dicht.",
    en="A shutter with a little heart in it. Put it against the wall beside or in front of your window. Right-click it "
       "and it swings open like a door; once more and it closes again.")
_BANKJE = dict(
    nl="Een bankje met oortjes op de leuning. Rechtsklik erop en je gaat lekker zitten. Opstaan doe je met sluipen.",
    en="A bench with little ears on the backrest. Right-click it to sit down. Sneak to get up again.")
for _m in ("eiken", "sparren", "berken", "jungle", "acacia", "donkereiken", "mangrove", "kersen", "bleekeiken", "bamboe", "karmozijn",
           "verwrongen", "baksteen", "steenbaksteen", "diepsteenbaksteen", "modderbaksteen", "dakpan_roze", "dakpan_kersrood",
           "dakpan_hemelsblauw", "dakpan_mint", "dakpan_lavendel", "dakpan_perzik"):
    UITLEG[f"{_m}_dak_half"] = UITLEG[f"{_m}_dak_kwart"] = _DAK
for _k in ("wit", "roze", "mint", "hemelsblauw"):
    UITLEG[f"guh_kozijn_{_k}"] = _KOZIJN
    UITLEG[f"guh_luik_{_k}"] = _LUIK
UITLEG["guh_bankje_hout"] = UITLEG["guh_bankje_roze"] = _BANKJE
UITLEG["guh_schoorsteen"] = dict(
    nl="Een guhschoorsteen met oortjes. Zet hem op je dak en er komt gezellig rook uit. Rechtsklik erop om het vuurtje "
       "uit te doen, en nog een keer om het weer aan te steken.",
    en="A guh chimney with little ears. Put it on your roof and cosy smoke comes out. Right-click it to put the fire "
       "out, and once more to light it again.")

# 1.6.0 deel 2 F10b: het verblijfsbord, met de Bouwopdracht van de helper- en bouwdieren
# G1 (1.6.0): the safari blocks that still had no "Zo werkt het"
UITLEG["safari_schemabord"] = dict(
    nl="Het schemabord van het safaripark, op het dorpsplein. Rechtsklik erop en je ziet het plan van vandaag: wanneer "
       "de dieren gevoerd worden, wanneer de ranger een praatje houdt, of er een nachtsafari is en welk dier jarig is "
       "(dat dier levert vandaag dubbele munten op voor een foto).",
    en="The schedule board of the safari park, on the village square. Right-click it to see today's plan: when the "
       "animals are fed, when the ranger gives a talk, whether there is a night safari and which animal has its "
       "birthday (that animal pays double coins for a photo today).")
UITLEG["safari_ooievaarsbus"] = dict(
    nl="Een brievenbus voor ooievaarspost. Zet hem bij je huis: de ooievaars van je vrienden (een ooievaar van een "
       "verblijfsbord) brengen er af en toe een pakketje in. Rechtsklik erop om de bus te openen en de cadeautjes eruit "
       "te halen. Alleen jij kunt je eigen bus openen.",
    en="A mailbox for stork mail. Put it by your house: your friends' storks (a stork from a home sign) now and then "
       "drop a parcel in it. Right-click it to open the box and take the presents out. Only you can open your own box.")
UITLEG["safari_verblijfsbord"] = dict(
    nl="Een verblijfsbordje uit de giftshop van het safaripark: zet het neer en er komt een familie van dat dier wonen "
       "(papa, mama en twee kleintjes). Je stelt de familie eerst zelf samen: per dier een ouder of een kind, en bij "
       "soorten waar je het ziet ook een mannetje of een vrouwtje. Rechtsklik erop voor het menu.\n"
       "\n"
       "- Familie, Verblijf, Tevredenheid, Cadeautjes en Info: namen, de grootte van hun thuis, wat ze blij maakt.\n"
       "- Familie samenstellen (tab Familie): wissel later nog wie er wonen; wie verandert, wordt met een poef omgewisseld.\n"
       "- Ga met je muis op een knop of een regel staan: er staat altijd wat hij doet, of waarom hij grijs is.\n"
       "- Rand tonen laat de rand van hun thuis zien; Slaapplek aanwijzen: klik daarna op een blok binnen de rand.\n"
       "- Eten zoeken ze zelf. Aaien en hun speeltje maken ze extra blij; voeren is een traktatie (hartjes).\n"
       "- Bouw passend verblijf: eerst een voorstel als spook (met een hek eromheen). Goedgekeurd? Dan bouwen de "
       "dieren het zelf met jouw blokken. Of Annuleren.\n"
       "- Bouwopdracht: veel dieren helpen mee! Bevers bouwen een dam, stokstaartjes graven een kelder, pinguïns een "
       "iglo, de gibbon hangt slingers. Kies Zoek plek of wijs zelf een plek aan met de vormstaf; de familie gaat op "
       "werkuitje (tot 48 blokken ver) en bouwt het spook. Hoe blijer de familie, hoe groter en mooier.\n"
       "- De blokjes komen uit je inventaris, of uit een Bank Guh: klik het bord met een Banksleutel.",
    en="A home sign from the safari park's gift shop: put it down and a family of that animal moves in (dad, mum and "
       "two little ones). You first put the family together yourself: a parent or a little one for each animal, and for "
       "species where you can see it also a male or a female. Right-click it for the menu.\n"
       "\n"
       "- Family, Home, Happiness, Gifts and Info: names, the size of their home, what makes them happy.\n"
       "- Put the family together (Family tab): change later who lives there; who changes is swapped with a puff.\n"
       "- Hover over a button or a line: it always says what it does, or why it is grey.\n"
       "- Show edge shows the edge of their home; Point at sleeping spot: then click a block inside the edge.\n"
       "- They find their own food. Petting and their toy make them extra happy; feeding is a treat (hearts).\n"
       "- Build a fitting home: first a proposal as a ghost (with a fence around it). Approved? Then the animals "
       "build it themselves with your blocks. Or Cancel.\n"
       "- Building job: many animals help! Beavers build a dam, meerkats dig a cellar, penguins an igloo, the gibbon "
       "hangs garlands. Choose Find a spot or point one out with the Shape Wand; the family goes on a work outing (up "
       "to 48 blocks away) and builds the ghost. The happier the family, the bigger and prettier.\n"
       "- The blocks come from your inventory, or from a Bank Guh: click the sign with a Bank Key.")
# 1.7.0 Onder de Wol (feature/mijnhuisje, feature/ringvlucht/RookPlatform; tools/features/mijnrook.py)
UITLEG["mijn_guhhuisje"] = dict(
    nl="Een klein guhhuisje met een mijnhelmpje. Je krijgt het van de Mijnguh als je hem 16 viltsteen, een guhklei en een "
       "Vahoegiet brengt. Klik erop met een opgepakte guh en die trekt erin: hooguit 4 van je eigen tamme guhs.\n"
       "\n"
       "De guhs minen langzaam het gebied onder het huisje leeg, laag voor laag van boven naar beneden. Rechtsklik erop "
       "voor het menu:\n"
       "- Schuifjes: tot 20 blokken naar elke kant (oost-west en noord-zuid) en tot hoe diep (nooit onder y 10).\n"
       "- Laat zien: roze glinsters tonen even de rand van het gebied.\n"
       "- Gratis werkt elke guh een blok per 20 seconden; met kaasknabbels in het vakje vier keer zo snel (een knabbel per 20 blokken).\n"
       "- Wat ze minen gaat in een kist naast het huisje, of in de Bank Guh van een Banksleutel in het andere vakje. Een guh "
       "met een rugzakzadel neemt alle gewone grondblokken mee in zijn rugzak (eindeloos) en brengt ze naar de Bank Guh.\n"
       "- Is alles vol, dan wachten de guhs tot er weer plek is.\n"
       "\n"
       "Ze minen alleen gewone grond (wol, de steenlagen, ertsen, aarde, zand...), nooit gebouwen, nooit naast water of "
       "lava. Onder het huisje blijft een pilaar staan met een ladder naar beneden, en er komen roze lampjes in.",
    en="A little guh house with a miner's helmet. The Miner Guh gives it to you when you bring him 16 felt stone, a guh "
       "clay and a Vahoegite. Click it with a picked-up guh and it moves in: at most 4 of your own tamed guhs.\n"
       "\n"
       "The guhs slowly mine out the area under the house, layer by layer from the top down. Right-click it for the "
       "menu:\n"
       "- Sliders: up to 20 blocks to each side (east-west and north-south) and how deep (never below y 10).\n"
       "- Show: pink sparkles show the edge of the area for a while.\n"
       "- For free each guh mines a block every 20 seconds; with cheese nibbles in the slot four times as fast (one nibble per 20 blocks).\n"
       "- What they mine goes into a chest next to the house, or into the Bank Guh of a Bank Key in the other slot. A guh "
       "wearing a Saddle Bag takes all plain ground blocks along in its bag (endlessly) and brings them to the Bank Guh.\n"
       "- When everything is full, the guhs wait until there is room again.\n"
       "\n"
       "They only mine plain ground (wool, the stone layers, ores, dirt, sand...), never buildings, never next to water "
       "or lava. A pillar stays under the house with a ladder down, and pink lamps go in it.",
    meer=["systemen/guhhuisje"])
UITLEG["rookguh_platform"] = dict(
    nl="Je eigen landingsplek voor de Rookguh. Je krijgt hem als je de hele reis van de Knabbelring hebt uitgespeeld "
       "(wie dat al gedaan had, kreeg hem bij de eerste keer inloggen na de update).\n"
       "\n"
       "Zet hem neer waar je wilt, in elke wereld: dat wordt jouw Rookguh-platform. Je hebt er één; zet je hem ergens anders "
       "neer, dan verhuist je landingsplek mee. Bij elke Reis-Rookguh staat je platform nu in de lijst. Rechtsklik zelf op "
       "je platform en de Rookguh vliegt je naar elke plek van je reis, of naar het Super Guhrio-kasteel (als je wereld 1-1 "
       "hebt uitgespeeld).",
    en="Your own landing place for the Smoke Guh. You get it when you have finished the whole journey of the Nibble Ring "
       "(whoever had done that already got it at their first login after the update).\n"
       "\n"
       "Put it down wherever you like, in any world: that becomes your Smoke Guh Platform. You have one; put it down "
       "somewhere else and your landing place moves along. Every Travel Smoke Guh now lists your platform. Right-click "
       "your platform yourself and the Smoke Guh flies you to any place of your journey, or to the Super Guhrio castle "
       "(once you have finished world 1-1).")
# 1.7.0 Onder de Wol: the blocks that do something (tools/features/onderwol*.py, toveren.py, mijnhulp.py, guhvillagers.py,
# grotspellen.py, golf18.py). The safari terrarium's own text moved into terrariumglas (the tank has no item, so no page).
UITLEG["glinsteraltaar"] = dict(
    nl="De guh-betoveringstafel. Rechtsklik erop: leg er gereedschap, een wapen, een harnasstuk of een boek op, met lapis, en "
       "kies een van de drie spreukjes. Het kost lapis en levels, zoals bij een betoveringstafel.\n"
       "\n"
       "- Hoe sterk de spreukjes zijn, hangt af van de knuffelboekenkasten om het altaar heen (tot 15, net als boekenkasten); "
       "gewone boekenkasten tellen niet mee.\n"
       "- Er zitten nuttige vanilla-betoveringen in en vijf guh-spreukjes: Mika-mepper, Pluisval, Glitterzicht, Vadsvlijt en Bakvads.\n"
       "- Een boek met een guh-spreukje wordt een Spreukjesboekje.\n"
       "- Onbreekbaarheid en herstel komen nooit op spullen die toch al niet kapot gaan.",
    en="The guh enchanting table. Right-click it: put a tool, a weapon, a piece of armour or a book on it, with lapis, and "
       "choose one of the three spells. It costs lapis and levels, like an enchanting table.\n"
       "\n"
       "- How strong the spells are depends on the Cuddle Bookshelves around the altar (up to 15, like bookshelves); ordinary "
       "bookshelves don't count.\n"
       "- It holds useful vanilla enchantments and five guh spells: Mika Whacker, Fluff Fall, Glitter Sight, Vads Diligence and "
       "Baking Vads.\n"
       "- A book with a guh spell becomes a Little Spell Book.\n"
       "- Unbreaking and mending never go on things that don't break anyway.",
    meer=["systemen/glinsteraltaar"])
UITLEG["knuffelboekenkast"] = dict(
    nl="Een boekenkast van roze bloesemhout met pastelboeken en pluche guhtjes ertussen. Zet hem om een Glinsteraltaar (zoals "
       "boekenkasten om een betoveringstafel, met een blok lucht ertussen): elke kast maakt de spreukjes sterker, tot 15. Er vliegen "
       "hartjes van de kast naar het altaar.",
    en="A bookshelf of pink blossom wood with pastel books and little plush guhs in between. Put it around a Glitter Altar (like "
       "bookshelves around an enchanting table, with a block of air in between): every shelf makes the spells stronger, up to 15. "
       "Hearts fly from the shelf to the altar.",
    meer=["systemen/glinsteraltaar"])
UITLEG["wolkenliftje"] = dict(
    nl="Een lift voor je mijn. Zet er een onderin en een boven op de grond, recht boven elkaar (met twee vrije blokken erboven).\n"
       "\n"
       "- Ga erop staan en spring: je zweeft in een roze wolkje naar het volgende Wolkenliftje erboven (tot 256 blokken).\n"
       "- Sluip erop: naar het volgende eronder.\n"
       "- Het werkt dwars door de steen heen.",
    en="An elevator for your mine. Put one at the bottom and one up on the ground, straight above each other (with two free "
       "blocks above them).\n"
       "\n"
       "- Stand on it and jump: you float up in a pink cloud to the next Cloud Lift above (up to 256 blocks).\n"
       "- Sneak on it: down to the next one below.\n"
       "- It works right through the stone.",
    meer=["systemen/mijnhulpjes"])
UITLEG["pluisladder"] = dict(
    nl="Een zachte roze touwladder op een rol. Zet hem tegen een wand aan de rand van een gat: hij rolt vanzelf uit tot de bodem, "
       "en je klimt erin zoals op een ladder. Graaf je de bodem verder weg, dan rolt hij verder uit. Breek je een stukje, dan rolt "
       "de hele ladder op en krijg je je rol terug.",
    en="A soft pink rope ladder on a roll. Put it against a wall at the edge of a hole: it unrolls down to the floor by itself, "
       "and you climb it like a ladder. Dig the floor away further and it unrolls further. Break a piece and the whole ladder rolls "
       "up and you get your roll back.",
    meer=["systemen/mijnhulpjes"])
UITLEG["terrariumglas"] = dict(
    nl="Glas voor terraria. Zet je een terrariumbordje neer, dan verschijnt er een glazen bakje waarin de diertjes wonen; klik "
       "erop voor het menu, net als bij een verblijfsbord, en voer ze met eten in je hand.\n"
       "\n"
       "Sinds 1.7.0 mag een terrarium elke vorm hebben: bouw terrariumglas of terrariumbodem tegen het bakje aan, en alles wat zo "
       "aan elkaar vastzit (een grote glazen bak, gangetjes, omhoog en omlaag) is één leefruimte. De diertjes lopen er overal "
       "doorheen, maar nooit eruit.",
    en="Glass for terraria. When you put down a terrarium sign, a glass tank appears where the critters live; click it for the "
       "menu, like a home sign, and feed them with food in your hand.\n"
       "\n"
       "Since 1.7.0 a terrarium may have any shape: build terrarium glass or terrarium floor onto the tank, and everything joined "
       "to it like that (a big glass tank, tunnels, up and down) is one living space. The critters walk around in all of it, but "
       "never out of it.",
    meer=["systemen/verblijfsborden", "systemen/kriebelhuis"])
UITLEG["terrariumbodem"] = dict(
    nl="Een bodem van zand en hout onder glas, om een terrarium groter te maken. Bouw hem aan een terrarium (samen met "
       "terrariumglas): alles wat aan elkaar vastzit is één leefruimte, en de diertjes lopen er overal doorheen.",
    en="A floor of sand and wood under glass, to make a terrarium bigger. Build it onto a terrarium (together with terrarium "
       "glass): everything joined together is one living space, and the critters walk around in all of it.",
    meer=["systemen/verblijfsborden"])
UITLEG["belletjeskristal"] = dict(
    nl="Een kristal uit de Belletjesgrot dat een toontje speelt als je erop stapt (niet als je sluipt), erop valt, ertegen slaat "
       "of er iets op schiet, met een zachte echo. Elke plek heeft zijn eigen toon: een rijtje kristallen is een toonladder, dus je "
       "kunt er liedjes op lopen en tikken.",
    en="A crystal from the Little Bells Cave that plays a note when you step on it (not when sneaking), land on it, hit it or "
       "shoot it, with a soft echo. Every spot has its own note: a row of crystals is a scale, so you can walk and tap tunes on them.",
    meer=["biomen/belletjesgrot"])
UITLEG["belletjestrosje"] = dict(
    nl="Een klein trosje kristallen belletjes uit de Belletjesgrot, op elke kant van een blok. Klik erop, sla ertegen of schiet "
       "erop en het klingelt zijn toontje, met een echo. De toon hangt af van de plek, zoals bij het belletjeskristal.",
    en="A little bunch of crystal bells from the Little Bells Cave, on any side of a block. Click it, hit it or shoot it and it "
       "chimes its note, with an echo. The note depends on the spot, like the bell crystal.",
    meer=["biomen/belletjesgrot"])
UITLEG["glitterrank_bes"] = dict(
    nl="Het eind van een glitterrank uit de Glitterspelonk, met bessen. Rechtsklik met een lege hand om de glitterbessen te "
       "plukken (een of twee, om te eten); de rank blijft hangen, zonder bessen.",
    en="The end of a glitter vine from the Glitter Cavern, with berries. Right-click with an empty hand to pick the glitter "
       "berries (one or two, to eat); the vine stays, without berries.",
    meer=["biomen/glitterspelonk"])
UITLEG["knuffelwortelaarde"] = dict(
    nl="Zachte aarde uit de Knuffelwortelgrot, zoals wortelaarde. Strooi er beendermeel op en er groeit een knuffelwortel onder.",
    en="Soft soil from the Plush Root Cave, like rooted dirt. Use bone meal on it and a plush root grows under it.",
    meer=["biomen/knuffelwortelgrot"])
UITLEG["pluiskussen"] = dict(
    nl="Een groot zacht kussen van pluis uit de Pluisgrot. Je landt erop zonder je pijn te doen en stuitert een klein beetje "
       "omhoog (zachter dan slijm). Sluip je, dan stuiter je niet.",
    en="A big soft cushion of fluff from the Fluff Cave. You land on it without hurting yourself and bounce up a little (softer "
       "than slime). Sneak and you don't bounce.",
    meer=["biomen/pluisgrot"])
UITLEG["kristalpegel"] = dict(
    nl="Een roze kristalpegel, zoals druipsteen: hij hangt aan een plafond of staat op een vloer, en een rij pegels wordt vanzelf "
       "een dikkere zuil. Het puntje gloeit zacht en druppelt af en toe kaassaus. Pas op: een hangende pegel die zijn houvast "
       "verliest valt naar beneden en doet pijn, en wie op een staande punt valt ook. Op een kristalpegelblok met water of kaassaus "
       "erop groeit een pegel langzaam aan.",
    en="A pink crystal icicle, like dripstone: it hangs from a ceiling or stands on a floor, and a row of them becomes a thicker "
       "column by itself. The tip glows softly and now and then drips cheese sauce. Watch out: a hanging icicle that loses its hold "
       "falls down and hurts, and so does falling onto a standing tip. On a crystal icicle block with water or cheese sauce on top an "
       "icicle slowly grows.",
    meer=["biomen/kristalpegelgrot"])
UITLEG["toverguh_boekenstandaard"] = dict(
    nl="De werkplek van de Toverguh, een nieuw guh-villagerberoep. Zet hem bij een guh-villager zonder beroep en hij wordt "
       "Toverguh: hij verkoopt Spreukjesboekjes, ook met de spreukjes die je niet uit het Glinsteraltaar krijgt.",
    en="The workstation of the Wizard Guh, a new guh villager profession. Put it near a guh villager without a profession and it "
       "becomes a Wizard Guh: it sells Little Spell Books, also with the spells you don't get from the Glitter Altar.",
    meer=["systemen/guh-villagers", "systemen/glinsteraltaar"])
UITLEG["guhrad_werkbank"] = dict(
    nl="De werkplek van de Vadsmonteur, een nieuw guh-villagerberoep. Zet hem bij een guh-villager zonder beroep en hij wordt "
       "Vadsmonteur: hij verkoopt Guhraderen, Guhdraad, Guh Ovens en andere machines voor vadskracht, en koopt vahoege vads.",
    en="The workstation of the Chonk Mechanic, a new guh villager profession. Put it near a guh villager without a profession and "
       "it becomes a Chonk Mechanic: it sells Guh Wheels, Guh Wire, Guh Ovens and other vadskracht machines, and buys vahoege vads.",
    meer=["systemen/guh-villagers"])
UITLEG["ertsenkist"] = dict(
    nl="De werkplek van de Mijnwerker, een nieuw guh-villagerberoep. Zet hem bij een guh-villager zonder beroep en hij wordt "
       "Mijnwerker: hij verkoopt laagstenen, guhklei, kleurstof en lampjes, en koopt je ertsen en Vahoegiet.",
    en="The workstation of the Miner, a new guh villager profession. Put it near a guh villager without a profession and it "
       "becomes a Miner: it sells layer stones, guh clay, dye and lamps, and buys your ores and Vahoegite.",
    meer=["systemen/guh-villagers"])
UITLEG["kristalslijpsteen"] = dict(
    nl="De werkplek van de Kristalslijper, een nieuw guh-villagerberoep. Zet hem bij een guh-villager zonder beroep en hij wordt "
       "Kristalslijper: hij slijpt guhkristal en Vahoegiet tot kristallampen, kristalpegels, belletjeskristallen en meer.",
    en="The workstation of the Crystal Cutter, a new guh villager profession. Put it near a guh villager without a profession and "
       "it becomes a Crystal Cutter: it cuts guh crystal and Vahoegite into crystal lamps, crystal icicles, bell crystals and more.",
    meer=["systemen/guh-villagers"])
UITLEG["lampjestegel"] = dict(
    nl="Deco uit het winkeltje van Meester Knipperlicht (de Lichtjespuzzel), voor 40 lampjesmunten: een vloertegel met een "
       "paddenstoellampje. Rechtsklik erop om hem aan of uit te zetten.",
    en="Decoration from Master Blinky's shop (the Little Lights Puzzle), for 40 lamp coins: a floor tile with a toadstool lamp. "
       "Right-click it to switch it on or off.",
    meer=["minigames/lichtjespuzzel"])
UITLEG["paddenstoelkrukje"] = dict(
    nl="Deco uit het winkeltje van Meester Knipperlicht, voor 32 lampjesmunten: een krukje van een paddenstoel. Rechtsklik met "
       "een lege hand om erop te gaan zitten.",
    en="Decoration from Master Blinky's shop, for 32 lamp coins: a toadstool stool. Right-click it with an empty hand to sit on it.",
    meer=["minigames/lichtjespuzzel"])
UITLEG["gouden_kaasje"] = dict(
    nl="Deco uit het winkeltje van Kapitein Schepsnuit (Schatgraven), voor 24 schatmuntjes: het gouden kaasje, net als de bonusschat "
       "in het veld. Het gloeit een beetje; verder doet het niks.",
    en="Decoration from Captain Shovelsnout's shop (Treasure Dig), for 24 treasure coins: the golden cheese, just like the bonus "
       "treasure in the field. It glows a little; it does nothing else.",
    meer=["minigames/schatgraven"])
UITLEG["schatkistje"] = dict(
    nl="Deco uit het winkeltje van Kapitein Schepsnuit, voor 40 schatmuntjes: een klein schatkistje. Het is versiering: er kan "
       "niets in.",
    en="Decoration from Captain Shovelsnout's shop, for 40 treasure coins: a little treasure chest. It is decoration: nothing "
       "goes in it.",
    meer=["minigames/schatgraven"])
UITLEG["glimpotje"] = dict(
    nl="Deco uit het winkeltje van Juf Glimmie (Glimwormvangen), voor 24 glimmertjes: een potje vol glimwormpjes dat licht geeft. "
       "In de spelletjeszaal zijn dit de lampen: daar worden ze lichter naarmate je meer vangt.",
    en="Decoration from Miss Glimmy's shop (Glowworm Catching), for 24 glimmer coins: a jar full of glowworms that gives light. In "
       "the game hall these are the lamps: there they get brighter the more you catch.",
    meer=["minigames/glimwormvangen"])
UITLEG["glimplant"] = dict(
    nl="Deco uit het winkeltje van Juf Glimmie, voor 20 glimmertjes: een gloeiend struikje waar je doorheen kunt lopen. In de "
       "spelletjeszaal verstoppen de glimwormpjes zich erin.",
    en="Decoration from Miss Glimmy's shop, for 20 glimmer coins: a glowing bush you can walk through. In the game hall the "
       "glowworms hide in it.",
    meer=["minigames/glimwormvangen"])
LINK["golfbaan_ijsvilt"] = ["minigames/golf"]
UITLEG["vadslamp"] = dict(
    nl="Een lantaarntje met een guh-gezicht dat op 1 vadskracht brandt. Hang hem aan je Guhdraad (of zet hem pal naast een Guhrad of "
       "een machine): krijgt hij vadskracht, dan wordt hij wakker, kijkt blij en geeft lichtniveau 15. En hij schijnt veel verder "
       "dan een gewone lamp: tot zo'n 6 blokken om hem heen zet hij onzichtbaar extra licht in de lucht, dus één Vadslamp verlicht "
       "een hele kamer of grot.\n"
       "\n"
       "- Zonder vadskracht slaapt hij en is hij donker; het extra licht verdwijnt dan vanzelf.\n"
       "- Het extra licht gaat niet door muren heen en zit nooit in de weg: je kunt er gewoon blokken in zetten.\n"
       "- Recept: een lantaarn, twee Guhdraad en twee gloeisteenstof.",
    en="A little lantern with a guh face that burns on 1 chonk power. Hook it up to your Guh Wire (or put it right next to a Guh "
       "Wheel or a machine): when it gets chonk power it wakes up, looks happy and gives light level 15. And it shines much further "
       "than a normal lamp: up to about 6 blocks around it, it puts invisible extra light in the air, so one Chonk Lamp lights up a "
       "whole room or cave.\n"
       "\n"
       "- Without chonk power it sleeps and stays dark; the extra light then goes away by itself.\n"
       "- The extra light doesn't go through walls and is never in the way: you can just put blocks in it.\n"
       "- Recipe: a lantern, two Guh Wire and two glowstone dust.",
    meer=["systemen/guh-technologie"])
UITLEG["vadsschakelaar"] = dict(
    nl="Een hendeltje voor in je Guhdraad. Zet hem tussen twee stukken draad en rechtsklik erop om hem om te zetten:\n"
       "\n"
       "- Aan (groen knopje, blij snoetje): de vadskracht gaat erdoorheen, net als door een stuk Guhdraad.\n"
       "- Uit (rood knopje, slapend snoetje): hij knipt de lijn door. Alles achter de schakelaar krijgt geen vadskracht meer en "
       "valt in slaap; de kant van je Guhrad draait gewoon door.\n"
       "\n"
       "Handig om een machine even stil te zetten zonder draad weg te halen. Recept: een hendel boven een Guhdraad.",
    en="A little lever for your Guh Wire. Put it between two pieces of wire and right-click it to flip it:\n"
       "\n"
       "- On (green knob, happy snoot): the chonk power goes through, just like through a piece of Guh Wire.\n"
       "- Off (red knob, sleeping snoot): it cuts the line. Everything behind the switch gets no chonk power and falls asleep; "
       "the side of your Guh Wheel keeps running.\n"
       "\n"
       "Handy for stopping a machine for a while without taking wire away. Recipe: a lever above a Guh Wire.",
    meer=["systemen/guh-technologie"])
