"""
biomes3 slice "bouw-wolk1": every Dutch text (NL) and the proposed English (EN, for the later overlay in
tools/lang/en/c88_bio_bouw_wolk1.json; nothing reads EN yet). Not in FEATURES.
"""
H, S, L = "wolkenhoeder_hut", "sterrenwacht_ruine", "luchtballon_haven"

NL = {
    # ---- names --------------------------------------------------------------------------------------------------------
    f"structure.guhs.{H}": "Wolkenhoeder-hut",
    f"structure.guhs.{H}.tooltip": "Een groot zwevend eiland met een meertje, een waterval en de hut van de wolkenhoeder. Zeldzaam!",
    f"structure.guhs.{S}": "Sterrenwacht-ruïne",
    f"structure.guhs.{S}.tooltip": "Een half ingestort sterrenwachtje op een hoog eiland. In het donker vallen hier extra veel sterren.",
    f"structure.guhs.{L}": "Luchtballon-haven",
    f"structure.guhs.{L}.tooltip": "Een steiger aan de rand van een eiland met ballonnen. Eén vaart per dag naar de weide.",
    f"entity.guhs.guh_npc.{S}_sterrenkijker": "Sterrenkijkerguh",
    "entity.guhs.guh_npc.ballonvaarderguh": "Ballonvaarder-guh",
    f"entity.guhs.{L}_ballon": "Havenballon",
    f"block.guhs.{S}_sterrenkaart": "Sterrenkaart",
    f"block.guhs.{S}_sterrenkaart.lore": "Alle sterren van de Guhmensie, met de hand nageprikt.",
    # ---- the wolkenhoeder: the lesson ---------------------------------------------------------------------------------
    f"quest.guhs.{H}.hallo": "Njeg! Bezoek, helemaal hierboven! Ik ben de wolkenhoeder en dit zijn mijn wolkenschaapjes. Vahoeg pluizig, njeg? "
                             "Zal ik je leren hoe je van pluis een echte wolk maakt?",
    f"gui.guhs.{H}.optie.ja": "Ja, leer het me!",
    f"gui.guhs.{H}.optie.later": "Straks misschien",
    f"quest.guhs.{H}.later": "Rustig aan, vads. De wolken lopen niet weg. Nou ja... een beetje.",
    f"quest.guhs.{H}.stap1": "Les één: pluis! Knip een van mijn schaapjes met een schaar. Het kietelt alleen maar, en over een paar minuutjes "
                             "is het weer een bolletje.",
    f"quest.guhs.{H}.schaar": "Hier, neem mijn reserveschaar maar. Njeg.",
    f"quest.guhs.{H}.stap1.nog": "Eerst knippen, vads. Rechtsklik met de schaar op een wolkenschaapje.",
    f"quest.guhs.{H}.stap2": "Vahoeg mooi pluis! Les twee: leg vier plukjes wolkenpluis in een vierkantje op een werkbank. Dan krijg je "
                             "wolkenblokken. In mijn hut staat een werkbank.",
    f"quest.guhs.{H}.stap2.nog": "Vier plukjes pluis, twee bij twee. Te weinig? Knip nog een schaapje, of wacht tot het pluis terug is.",
    f"quest.guhs.{H}.stap3": "Een echte wolk! Les drie: bouw er een wolkentrapje mee. Drie wolkenblokken, elk een stapje hoger dan de vorige. "
                             "Zo kom je overal, njeg.",
    f"quest.guhs.{H}.stap3.nog": "Drie wolkenblokken schuin omhoog, als een trapje. Meer is het niet.",
    f"quest.guhs.{H}.stap4": "Daar kun je de hemel mee in! Laatste les, en de belangrijkste: uitrusten. Ga op een wolkenbankje zitten of kruip "
                             "in een wolkenbed. Ze staan hier bij de hut.",
    f"quest.guhs.{H}.stap4.nog": "Niet zo haasten, vads. Zit even op het wolkenbankje. Dat hoort erbij.",
    f"quest.guhs.{H}.terug": "Lekker gezeten, njeg? Kom maar even bij me langs.",
    f"quest.guhs.{H}.klaar": "Njeg! Nu ben je een echte wolkenmaker. Hier: wat pluis om mee te beginnen en een wolkenlampje voor in huis. "
                             "En als je wilt, mag je een schaapje mee naar huis nemen.",
    f"gui.guhs.{H}.optie.schaapje": "Mag ik een schaapje mee naar huis?",
    f"gui.guhs.{H}.optie.dag": "Dag wolkenhoeder!",
    f"quest.guhs.{H}.schaapje": "Dit is een lieve. Hou het lijntje goed vast op de terugweg! Het eet knabbelvoer en geeft je thuis pluis. "
                                "Vahoeg goed voor zorgen, njeg?",
    f"quest.guhs.{H}.schaapje_al": "Jouw schaapje heb je al meegekregen, vads. Twee schaapjes met knabbelvoer krijgen vanzelf een lammetje.",
    f"quest.guhs.{H}.dag": "Dag vads! Kom nog eens pluis knippen.",
    f"quest.guhs.{H}.praatje0": "De schaapjes in de wei blijven hier, die horen bij het eiland. Maar pluis knippen mag altijd.",
    f"quest.guhs.{H}.praatje1": "Wist je dat een wolkenblok zacht is? Je kunt er van heel hoog op vallen. Njeg, niks aan de hand.",
    f"quest.guhs.{H}.praatje2": "Het water van mijn meertje valt in een wolk. Daar komt de regen vandaan, denk ik. Vads.",
    f"quest.guhs.{H}.praatje3": "De wolkenlift brengt je omhoog en de wolkenstroom weer omlaag. Kijk maar naar de pijltjes.",
    f"gui.guhs.{H}.kudde_blijft": "Njeg! Dit schaapje hoort bij de wolkenhoeder. Vraag hem om een eigen schaapje.",
    f"gui.guhs.{H}.les": "Les %s van 4 gedaan!",
    # ---- the sterrenkijkerguh -----------------------------------------------------------------------------------------
    f"quest.guhs.{S}.slaapt": "Zzz... njeg... zzz... nog vijf sterretjes...",
    f"gui.guhs.{S}.slaapt": "De sterrenkijkerguh slaapt. Kom terug als het donker is.",
    f"quest.guhs.{S}.hallo": "O... hallo. Ben je echt, of droom ik je? Het maakt niet uit. Kijk omhoog, vads. Ze zijn er allemaal weer.",
    f"quest.guhs.{S}.nacht0": "Elke ster is een guh die ooit heel hard \"njeg\" heeft gezegd. Dat denk ik. Het zou toch mooi zijn?",
    f"quest.guhs.{S}.nacht1": "Vroeger had dit huisje een heel dak. Toen viel er een ster doorheen. Nu zie ik ze beter. Vahoeg handig.",
    f"quest.guhs.{S}.nacht2": "Soms valt er een. Dan doe ik een wens. Meestal wens ik nog een ster.",
    f"quest.guhs.{S}.nacht3": "Kijk eens door de telescoop. Als je heel stil bent, laat de hemel wat sterrenstof voor je achter.",
    f"quest.guhs.{S}.nacht4": "Overdag slaap ik. De zon is ook een ster, maar ze praat zo hard.",
    f"quest.guhs.{S}.stof": "Sst... zag je dat? Er dwarrelde wat sterrenstof langs de lens. Hou je hand maar op.",
    f"gui.guhs.{S}.stof_al": "Vannacht heb je hier al sterrenstof gevangen. Morgennacht weer, njeg.",
    # ---- the ballonvaarder-guh ----------------------------------------------------------------------------------------
    f"quest.guhs.{L}.hallo": "Ahoy, njeg! Welkom in de Luchtballon-haven. Eén vaart per dag, helemaal naar de weide beneden. Zachtjes, met "
                             "uitzicht. Instappen?",
    f"gui.guhs.{L}.optie.ja": "Ja, vaar me naar beneden!",
    f"gui.guhs.{L}.optie.nee": "Nee, ik blijf nog even",
    f"quest.guhs.{L}.vertrek": "Hou je vast aan het mandje! Nou ja, het gaat vahoeg langzaam. Goede vaart, vads!",
    f"quest.guhs.{L}.al": "Vandaag heb je al gevaren, njeg. De ballon moet ook uitrusten. Morgen weer!",
    f"quest.guhs.{L}.weg": "De ballon is nog onderweg. Even geduld, vads, hij komt zo terug.",
    f"quest.guhs.{L}.geblokkeerd": "Njeg... beneden is nu geen vrij plekje om te landen. Ik durf het niet aan. Probeer het straks nog eens.",
    f"quest.guhs.{L}.nee": "Geeft niks. Kijk gerust rond, het uitzicht is gratis.",
    f"quest.guhs.{L}.geland": "Zacht geland! De ballon vaart vanzelf terug naar de haven. Tot morgen, njeg!",
    f"quest.guhs.{L}.terug": "Njeg, de landingsplek was bezet. We zijn weer boven. Je vaart van vandaag hou je tegoed.",
    f"quest.guhs.{L}.afgebroken": "Njeg, de vaart is afgebroken. Je staat weer op de steiger en je vaart van vandaag hou je tegoed.",
    f"gui.guhs.{L}.blijf_zitten": "Njeg! Blijf lekker in het mandje.",
    f"gui.guhs.{L}.praat": "Praat met de ballonvaarder-guh voor een vaart.",
}

EN = {
    f"structure.guhs.{H}": "Cloud Shepherd's Hut",
    f"structure.guhs.{H}.tooltip": "A big floating island with a little lake, a waterfall and the cloud shepherd's hut. Rare!",
    f"structure.guhs.{S}": "Observatory Ruin",
    f"structure.guhs.{S}.tooltip": "A half-collapsed little observatory on a high island. Extra many stars fall here in the dark.",
    f"structure.guhs.{L}": "Balloon Harbour",
    f"structure.guhs.{L}.tooltip": "A jetty on the edge of an island with balloons. One ride a day down to the meadow.",
    f"entity.guhs.guh_npc.{S}_sterrenkijker": "Stargazer Guh",
    "entity.guhs.guh_npc.ballonvaarderguh": "Balloonist Guh",
    f"entity.guhs.{L}_ballon": "Harbour Balloon",
    f"block.guhs.{S}_sterrenkaart": "Star Chart",
    f"block.guhs.{S}_sterrenkaart.lore": "Every star of the Guhmension, pricked in by hand.",
    f"quest.guhs.{H}.hallo": "Njeg! A visitor, all the way up here! I'm the cloud shepherd and these are my cloud lambs. Vahoeg fluffy, njeg? "
                             "Shall I teach you how to turn fluff into a real cloud?",
    f"gui.guhs.{H}.optie.ja": "Yes, teach me!",
    f"gui.guhs.{H}.optie.later": "Maybe later",
    f"quest.guhs.{H}.later": "Take it easy, vads. The clouds won't walk away. Well... a little.",
    f"quest.guhs.{H}.stap1": "Lesson one: fluff! Shear one of my lambs. It only tickles, and in a few minutes it's a little ball again.",
    f"quest.guhs.{H}.schaar": "Here, take my spare shears. Njeg.",
    f"quest.guhs.{H}.stap1.nog": "Shear first, vads. Right-click a cloud lamb with the shears.",
    f"quest.guhs.{H}.stap2": "Vahoeg nice fluff! Lesson two: put four tufts of cloud fluff in a square on a crafting table. That makes cloud "
                             "blocks. There's a crafting table in my hut.",
    f"quest.guhs.{H}.stap2.nog": "Four tufts of fluff, two by two. Not enough? Shear another lamb, or wait until the fluff is back.",
    f"quest.guhs.{H}.stap3": "A real cloud! Lesson three: build a little cloud stair with it. Three cloud blocks, each one step higher than the "
                             "last. That gets you anywhere, njeg.",
    f"quest.guhs.{H}.stap3.nog": "Three cloud blocks going up at a slant, like a stair. That's all.",
    f"quest.guhs.{H}.stap4": "You can climb the sky with that! Last lesson, and the most important: resting. Sit on a cloud bench or crawl into a "
                             "cloud bed. They're right here by the hut.",
    f"quest.guhs.{H}.stap4.nog": "Not so fast, vads. Sit on the cloud bench for a bit. It's part of it.",
    f"quest.guhs.{H}.terug": "Nice sit, njeg? Come and see me for a moment.",
    f"quest.guhs.{H}.klaar": "Njeg! Now you're a real cloud maker. Here: some fluff to start with and a cloud lamp for your home. And if you "
                             "like, you may take a lamb home.",
    f"gui.guhs.{H}.optie.schaapje": "May I take a lamb home?",
    f"gui.guhs.{H}.optie.dag": "Bye, cloud shepherd!",
    f"quest.guhs.{H}.schaapje": "This is a sweet one. Hold the lead tight on the way back! It eats knabbelvoer and gives you fluff at home. "
                                "Take vahoeg good care of it, njeg?",
    f"quest.guhs.{H}.schaapje_al": "You already got your lamb, vads. Two lambs with knabbelvoer get a little one by themselves.",
    f"quest.guhs.{H}.dag": "Bye vads! Come and shear some fluff again.",
    f"quest.guhs.{H}.praatje0": "The lambs in the fold stay here, they belong to the island. But shearing fluff is always fine.",
    f"quest.guhs.{H}.praatje1": "Did you know a cloud block is soft? You can fall on it from very high. Njeg, nothing happens.",
    f"quest.guhs.{H}.praatje2": "The water of my little lake falls into a cloud. That's where rain comes from, I think. Vads.",
    f"quest.guhs.{H}.praatje3": "The cloud lift takes you up and the cloud stream back down. Just look at the little arrows.",
    f"gui.guhs.{H}.kudde_blijft": "Njeg! This lamb belongs to the cloud shepherd. Ask him for one of your own.",
    f"gui.guhs.{H}.les": "Lesson %s of 4 done!",
    f"quest.guhs.{S}.slaapt": "Zzz... njeg... zzz... five more little stars...",
    f"gui.guhs.{S}.slaapt": "The stargazer guh is asleep. Come back when it's dark.",
    f"quest.guhs.{S}.hallo": "Oh... hello. Are you real, or am I dreaming you? It doesn't matter. Look up, vads. They're all there again.",
    f"quest.guhs.{S}.nacht0": "Every star is a guh that once said \"njeg\" very loudly. I think. Wouldn't that be lovely?",
    f"quest.guhs.{S}.nacht1": "This little house used to have a whole roof. Then a star fell through it. Now I see them better. Vahoeg handy.",
    f"quest.guhs.{S}.nacht2": "Sometimes one falls. Then I make a wish. Mostly I wish for another star.",
    f"quest.guhs.{S}.nacht3": "Have a look through the telescope. If you're very quiet, the sky leaves a little stardust for you.",
    f"quest.guhs.{S}.nacht4": "I sleep by day. The sun is a star too, but she talks so loudly.",
    f"quest.guhs.{S}.stof": "Shh... did you see that? A little stardust drifted past the lens. Hold out your hand.",
    f"gui.guhs.{S}.stof_al": "You already caught stardust here tonight. Again tomorrow night, njeg.",
    f"quest.guhs.{L}.hallo": "Ahoy, njeg! Welcome to the Balloon Harbour. One ride a day, all the way down to the meadow. Gently, with a view. "
                             "All aboard?",
    f"gui.guhs.{L}.optie.ja": "Yes, take me down!",
    f"gui.guhs.{L}.optie.nee": "No, I'll stay a while",
    f"quest.guhs.{L}.vertrek": "Hold on to the basket! Well, it goes vahoeg slowly. Have a good ride, vads!",
    f"quest.guhs.{L}.al": "You already had your ride today, njeg. The balloon needs a rest too. Again tomorrow!",
    f"quest.guhs.{L}.weg": "The balloon is still on its way. A little patience, vads, it'll be right back.",
    f"quest.guhs.{L}.geblokkeerd": "Njeg... there's no free spot to land down there right now. I don't dare. Try again later.",
    f"quest.guhs.{L}.nee": "No worries. Have a look around, the view is free.",
    f"quest.guhs.{L}.geland": "Landed softly! The balloon floats back to the harbour by itself. See you tomorrow, njeg!",
    f"quest.guhs.{L}.terug": "Njeg, the landing spot was taken. We're back up. You keep today's ride.",
    f"quest.guhs.{L}.afgebroken": "Njeg, the ride was broken off. You're back on the jetty and you keep today's ride.",
    f"gui.guhs.{L}.blijf_zitten": "Njeg! Stay in the basket.",
    f"gui.guhs.{L}.praat": "Talk to the balloonist guh for a ride.",
}
assert set(NL) == set(EN)
