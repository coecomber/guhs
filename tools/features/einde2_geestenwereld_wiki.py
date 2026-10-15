"""
einde2 (geestenwereld): what the wiki may say about the Geestenwereld. Not a feature module (not in FEATURES): the docs
step reads WIKI and turns it into pages (build contract 2.4). The Geestenwereld belongs to the Guhvatar story: these texts
tell what the place is like and give nothing of the story away. The pages of the blocks come from the game data.
"""
WIKI = {
    "systemen": {
        "geestenwereld": (
            "De Geestenwereld", "block:guhs:geestengras",
            "Een kleine wereld naast de onze, waar bijna niemand kan komen: eilanden die zweven in lila nevel, geen zon maar een "
            "lucht van traag noorderlicht, en alles een beetje andersom. Het gras is lila, het hout is wit en het water valt omhoog.",
            ["geestengras", "geestenhout", "schaduwhout", "gloeibladeren_cyaan", "gloeibladeren_roze", "maansteen", "nevelwater",
             "geestpaddenstoel_hoed", "lotusblad", "geestlantaarn", "geestenmasker", "meditatiesteen", "gedenksteen", "lotuspuzzeltegel"]),
    },
    "entity_home": {},
    "tekst": [
        ("geestenwereld", "Hoe kom je er?",
         "Niet zomaar. De Geestenwereld is een plek voor de Guhvatar, en voor wie heel dicht naast de Guhvatar staat. Je komt er "
         "vanzelf als het zover is. Terug naar je lijf kan altijd: elke plek heeft een meditatiesteen."),
        ("geestenwereld", "Wat is er te zien?",
         "Zeven plekken, verbonden door zwevende paden van maansteen met lotusbladen erop: de Meditatiecirkel met de "
         "Lantaarnboom, het Gefluisterwoud, Koh's Grot, de Witte Lotus-ruines met de Hal van de Voorgangers (en hoog daarboven "
         "de Top der Voorgangers), de Hemelvijver, het Theepaviljoen en Fang's Hemelpad. Overal staan gedenkstenen: klik erop en "
         "lees wat ze te vertellen hebben."),
        ("geestenwereld", "De Witte Lotus",
         "Achter de draaipuzzel ligt de Witte Lotus: een ommuurd heiligdom met een reusachtig Pai Sho-bord als binnenhof en "
         "daaromheen vijf vleugels, een per meester van de orde, elk met een eigen stijl, een echo van de meester en een proef. "
         "De weg ernaartoe loopt tussen vijf reusachtige, half overgroeide beelden door: de Trap der meesters. De Hal van de "
         "Voorgangers staat nu hoog op de Top der Voorgangers."),
        ("geestenwereld", "Onderweg",
         "Op de lange paden is van alles te zien: een vissende geest, een schrijntje met een knabbel als offer, zwevende stenen "
         "met een raadsel, watervallen die omhoog vallen en, hoog boven de weg naar de Hemelvijver, een reusachtige oude klok. "
         "Wie goed kijkt, ziet soms gloeiende pootjes die ergens heen lopen. En ver weg in de mist ligt iets dat nog niemand "
         "heeft bereikt."),
        ("geestenwereld", "Veilig",
         "In de Geestenwereld breek je niets af en zet je niets neer, en niemand doet je pijn. Wie van een eiland valt, wordt "
         "door de nevel zachtjes teruggetild naar de Meditatiecirkel."),
    ],
}
