"""
einde2 phase B (geest_spel): what the wiki may say about the play of the Geestenwereld. Not a feature module (not in
FEATURES): the docs step reads WIKI and turns it into pages (build contract C-A 2.4). These texts tell what there is to do
and give the big secrets away as little as possible (the Oude Reiger, the eighth island and Wan-guh stay a surprise).
"""
WIKI = {
    "systemen": {
        "geest_spel": (
            "Spelen in de Geestenwereld", "item:guhs:pai_sho_tegel_witte_lotus",
            "De Geestenwereld is meer dan mooi uitzicht. Er zijn proeven, verdwaalde lantaarns, een boze bosgeest, raadsels van "
            "Koh, een chakra-pad en dingen die je maar heel af en toe ziet. Wat er open is, hangt af van hoe ver je in het "
            "Guhvatar-verhaal bent; na het verhaal is alles vrij.",
            ["pai_sho_tegel_bumi", "pai_sho_tegel_pakguh", "pai_sho_tegel_piandao", "pai_sho_tegel_jeong_jeong", "pai_sho_tegel_miroh",
             "pai_sho_tegel_witte_lotus", "einde2_geest_spel_knabbelkoor_eikel", "einde2_geest_spel_koh_masker",
             "einde2_geest_spel_mediteerkussen"]),
    },
    "entity_home": {},
    "tekst": [
        ("geest_spel", "Welke plekken zijn open?",
         "Het verhaal opent de Geestenwereld stap voor stap. In het hoofdstuk van de Waterstam kom je bij de Meditatiecirkel, "
         "Koh's Grot en de Hemelvijver. Voor de finale vliegt Fang je naar de Top, en dan gaat ook de Witte Lotus open. Na het "
         "hele verhaal is alles vrij. Een plek die nog dicht is herken je aan de gedoofde lantaarns langs het pad: de mist "
         "duwt je daar zachtjes terug."),
        ("geest_spel", "Samen",
         "Sta je binnen vijf blokken van iemand die gaat mediteren, dan reis je mee als gast, en mag je overal komen waar je "
         "gids mag komen. Ieder houdt zijn eigen voortgang."),
        ("geest_spel", "De Witte Lotus",
         "Achter de poort met de draaipuzzel liggen vijf vleugels, elk met de echo van een meester en een proef: Bumi-guh's "
         "draaiende kamers (het voor de hand liggende antwoord is fout), Pakguh's ijsspiegels, Piandao-guh's ritmeduel, "
         "Jeong Jeong-guh's kaarsen en Oom Miroh's theetuin, waar je zijn wijsheden afmaakt. Elke proef geeft een Pai Sho-tegel. "
         "Wat de tegels openen, vertellen we hier niet."),
        ("geest_spel", "Kleine zijpaden",
         "Acht lantaarngeestjes zijn verdwaald: klik erop en breng ze terug naar de Lantaarnboom. Hei Bai-guh in het "
         "Gefluisterwoud heeft een eikeltje van thuis nodig (een knabbelkoorvrucht en een kaasknabbel in je knutseltafel). "
         "Koh heeft drie raadsels, maar alleen voor wie niets voelt. En Guru Pathik-guh wijst je het chakra-pad: zeven "
         "meditaties, beloning de Guhvatar-gloed."),
        ("geest_spel", "Mysterie",
         "Soms kleurt de mist diepblauw. Soms wordt het licht zilver. Soms lopen er gloeiende pootjes voor je uit, of staat er "
         "ver weg iemand met een kap. Kijk goed om je heen. En luid de klok eens."),
        ("geest_spel", "Terugkomen",
         "Na het verhaal krijg je een mediteerkussen: ga erop zitten en je ziel gaat naar de Geestenwereld (je kunt er meer "
         "maken). Houd shift ingedrukt en klik op een meditatiesteen, dan word je wakker bij een Guhvatar-gebouw dat je kent."),
    ],
}
