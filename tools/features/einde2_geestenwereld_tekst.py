"""
einde2, slice geestenwereld: every Dutch text of the Geestenwereld (block names and their grey line, the lore of the
memorial stones, the messages, the subtitles). English: tools/lang/en/c74_e2_geestenwereld.json.
"""
S = "einde2_geestenwereld"
G = f"gui.guhs.{S}"

NAMEN = {
    "geestengras": ("Geestengras", "Lila gras. Hier is alles andersom, njeg."),
    "geestenaarde": ("Geestenaarde", "Aarde uit de Geestenwereld. Ruikt naar maanlicht."),
    "geestenhout": ("Geestenhout", "Wit hout met een blauwe nerf. Groeit alleen waar niemand kijkt."),
    "geestenplanken": ("Geestenplanken", "Planken van geestenhout. Kraken in een andere taal."),
    "schaduwhout": ("Schaduwhout", "Zwart hout met een witte nerf: geestenhout op een slechte dag."),
    "schaduwplanken": ("Schaduwplanken", "Planken van schaduwhout. Ze kijken een beetje streng."),
    "gloeibladeren_cyaan": ("Cyane gloeibladeren", "Bladeren die zelf licht geven. Handig voor wie bang is in het donker."),
    "gloeibladeren_roze": ("Roze gloeibladeren", "Gloeien zacht roze. De geesten vinden het gezellig."),
    "schaduwbladeren": ("Schaduwbladeren", "Donkere bladeren uit de randen van het woud. Fluisteren het hardst."),
    "maansteen": ("Maansteen", "Bleke steen met een glans van maan en oceaan."),
    "maansteen_tegels": ("Maansteentegels", "Oude tegels van de geestenpaden. Al duizend jaar niet geveegd."),
    "maansteen_trap": ("Maansteentrap", "Een trap van maansteen. Gaat omhoog, en ook omlaag."),
    "maansteen_plaat": ("Maansteenplaat", "Een halve maansteentegel. De andere helft is nog onderweg."),
    "nevelwater": ("Nevelwater", "Water dat omhoog valt. Niet over nadenken, njeg."),
    "geestpaddenstoel_steel": ("Geestpaddenstoelsteel", "De steel van een reuzenpaddenstoel. Zacht en een beetje sponzig."),
    "geestpaddenstoel_hoed": ("Geestpaddenstoelhoed", "Gloeit blauwgroen met roze stippen. Niet opeten."),
    "lotusblad": ("Lotusblad", "Een bleek lotusblad dat in de nevel drijft. Draagt een vadsige guh."),
    "geestlantaarn": ("Geestlantaarn", "Een lantaarn met een koel, blauw licht."),
    "geestenmasker": ("Geestenmasker", "Een gezicht van de wand van Koh's Grot. Kijkt je aan, ook als je wegkijkt."),
    "meditatiesteen": ("Meditatiesteen", "Even stilzitten en klikken: je ziel snuffelt terug naar je lijf."),
    "gedenksteen": ("Gedenksteen", "Een oude steen met een verhaal. Klik om te lezen."),
    "lotuspuzzeltegel": ("Lotuspuzzeltegel", "Een stukje van de Witte Lotus. Klik om hem te draaien."),
}

# the memorial stones (gedenksteen): id -> (title, text); the build puts every one at a prop marker lore_<id>
LORE = {
    "geestenwereld": ("De Geestenwereld",
                      "Een wereld naast de onze, waar bijna niemand kan komen. Behalve de Guhvatar. En wie heel dicht naast de Guhvatar "
                      "staat. Het gras is lila, het hout is wit en het water valt omhoog. Waarom? De geesten zeggen: waarom niet?"),
    "meditatiecirkel": ("De Meditatiecirkel", "Hier slaapt je lijf, maar je ziel snuffelt verder, njeg."),
    "lantaarnboom": ("De Lantaarnboom",
                     "Elke lantaarngeest hoort aan deze boom. Maar lantaarngeesten zijn nieuwsgierig en dwalen af: acht haakjes "
                     "zijn leeg. Ze komen vast terug, als iemand ze even de weg wijst."),
    "gefluisterwoud": ("Het Gefluisterwoud",
                       "De bomen fluisteren. Meestal roddels over de Theegeest. In de donkere randen wonen schaduwgeesten: ze duwen "
                       "je een beetje opzij en verder doen ze niks. Ze zijn gewoon verlegen."),
    "hei_bai": ("Hei Bai-guh",
                "De beschermgeest van een bos in de echte wereld. Toen zijn bos afbrandde, werd hij groot en boos. Een eikeltje van "
                "thuis maakt hem weer klein en lief. Zoals bij iedereen eigenlijk."),
    "koh": ("Koh de Gezichtenrover",
            "Toon geen emotie. Niet lachen, niet schrikken, niet niezen. Koh verzamelt gezichten zoals jij knabbels verzamelt. "
            "Hij maakt nooit grapjes. Maakt hij toch een grapje: niet lachen."),
    "witte_lotus": ("De Witte Lotus",
                    "Een oud genootschap van wijze guhs die Pai Sho spelen, thee drinken en de wereld in stilte beschermen. Vooral "
                    "thee drinken. Leg de tegels als een lotus, dan weet de poort dat je een vriend bent."),
    "aang": ("Aang-guh, Luchtnomade",
             "De Guhvatar voor jou. Vadste honderd jaar in een kaasbol en miste daardoor bijna alles. Gaf het stokje aan jou "
             "door. Lacht veel, eet geen vlees, wel heel veel knabbels."),
    "roku": ("Roku-guh, Vuurnatie",
             "Wijs, rustig en een indrukwekkende baard. Zijn beste vriend werd later een Vuurheer met heel slechte ideeen. "
             "Roku-guh zegt: kies je vrienden goed. En je Vuurheren ook."),
    "kyoshi": ("Kyoshi-guh, Aardrijk",
               "Lang, streng, en met waaiers die je liever niet tegen je oortjes krijgt. Maakte van een schiereiland een eiland, "
               "gewoon omdat het kon. Haar volgelingen dragen nog altijd haar schmink."),
    "kuruk": ("Kuruk-guh, Waterstam",
              "Vocht als jonge Guhvatar tegen honderd boze geesten. Toen stal Koh het gezicht van zijn liefste, Ummi-guh. Sindsdien "
              "vertrouwt hij geen enkele duizendpoot meer. Ook de kleintjes niet."),
    "yangchen": ("Yangchen-guh, Luchtnomade",
                 "Zei altijd: de Guhvatar is er voor de wereld, niet voor zichzelf. Daarna at ze stiekem toch het laatste "
                 "knabbeltje. Niemand is perfect."),
    "szeto": ("Szeto-guh, Vuurnatie",
              "Werkte als Guhvatar vooral aan formulieren. Vulde alles in drievoud in, ook hier in de Geestenwereld. Zijn stapel "
              "ligt nog ergens. Niet aanraken: hij is gesorteerd."),
    "wan": ("Wan-guh, de eerste",
            "De allereerste Guhvatar. Of was het een kaasknabbel met grote plannen? De bronnen spreken elkaar tegen. De bronnen "
            "zijn ook een beetje kaas."),
    "leeg": ("Een lege steen",
             "Hier staat nog niets. Deze steen wacht op de Guhvatar van nu. Dat ben jij, njeg. Niet krassen: hij is net gepoetst."),
    "top": ("De Top der Voorgangers",
            "Hier spreek je de Guhvatars van vroeger. Ze praten meestal door elkaar heen, en Roku-guh begint altijd met: toen ik "
            "jong was..."),
    "hemelvijver": ("De Hemelvijver",
                    "Tui en La, de maan en de oceaan: duwen en trekken. Zolang zij hun rondjes zwemmen, staat het getij goed en "
                    "schijnt de maan. Niet voeren: ze zijn op dieet."),
    "theepaviljoen": ("Het Theepaviljoen",
                      "De Theegeest kwam hier ooit voor een kopje en ging nooit meer weg. Hij zegt: ik ken nog iemand die hier ooit "
                      "wil wonen... rond, baard, zegt wijze dingen over thee."),
    "fang": ("Fang's Hemelpad",
             "Fang, de geestdraak van Roku-guh. Hij vliegt alleen met wie Roku-guh vertrouwt. En met wie lekker ruikt. Was je "
             "oortjes even, voor de zekerheid."),
}

BERICHTEN = {
    "terug": "Je ziel snuffelt terug naar je lijf. Welkom terug, njeg.",
    "geen_terug": "Je lijf stond nergens meer: je wordt bij je bed wakker.",
    "leegte": "De nevel tilt je zachtjes terug naar de Meditatiecirkel. Niet naar beneden kijken, njeg.",
    "beschermd": "In de Geestenwereld verandert niets. Ook niet als je heel hard duwt.",
    "niet_hier": "Mediteren werkt alleen in de Geestenwereld. Hier is het gewoon een steen.",
    "aangekomen": "Je ogen gloeien. Je zweeft... en je landt zachtjes in de Geestenwereld.",
    "lore_kop": "~ %s ~",
    "tegel_draai": "Klik. De tegel draait.",
    "opgelost": "De lotus is heel! Ergens zucht een oude poort... (geduld, njeg)",
    "geen_lore": "Deze steen is zo oud dat zelfs de letters zijn gaan slapen.",
}

SUBTITLES = {
    "sfeer": "De Geestenwereld zoemt zacht",
    "gefluister": "Iets fluistert",
    "meditatie": "Een heldere bel",
    "tegel": "Een tegel draait",
}

TEXTS = {}
for _id, (_naam, _regel) in NAMEN.items():
    TEXTS[f"block.guhs.{_id}"] = _naam
    TEXTS[f"block.guhs.{_id}.lore"] = _regel
for _id, (_titel, _tekst) in LORE.items():
    TEXTS[f"{G}.lore.{_id}.titel"] = _titel
    TEXTS[f"{G}.lore.{_id}.tekst"] = _tekst
for _id, _tekst in BERICHTEN.items():
    TEXTS[f"{G}.{_id}"] = _tekst
for _id, _tekst in SUBTITLES.items():
    TEXTS[f"subtitles.guhs.{S}.{_id}"] = _tekst
TEXTS["biome.guhs.geestenwereld"] = "Geestenwereld"
