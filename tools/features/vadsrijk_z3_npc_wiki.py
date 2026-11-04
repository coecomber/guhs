"""
1.10 "Het Vadsrijk" zone 3 "De Schemerwortel", npc slice: what the wiki says about Honingtak, the story, the quests and the secrets
of zone 3 (the in-game version is the "Gids" tab of the questlog, J: pages z3_*). Not a feature module (not in FEATURES): the docs step
(tools/wiki_bbq2) reads WIKI, like vadsrijk_npc_wiki.py / vadsrijk_z2_npc_wiki.py; integration adds this module to that step.
Spoiler-free: it tells how zone 3 starts, never what lies in the nest, the tower or the heart of the tree, or how the zone ends.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "vadsrijk-schemerwortel": (
            "De Schemerwortel (zone 3)", "item:minecraft:honeycomb",
            "Een boom zo groot als een berg, voor avonturiers van level 30 tot 50. Na het verhaal van de Splinterpieken vliegt het "
            "luchtschip je vanuit De Haven of Kandijkom naar Honingtak, een imkersstadje op een tak. Hier krijg je je eerste rijdier.",
            ["het-vadsrijk", "vadsrijk-honingtak", "vadsrijk-z3-quests", "vadsrijk-z3-geheimen"]),
        "vadsrijk-honingtak": (
            "Honingtak", "item:minecraft:honey_bottle",
            "Een gezellig stadje op een reuzentak: bijenkorfhuisjes met raatgevels, de zoemende markt op het Bloesemplein, de Raattrossen "
            "die onder de tak hangen met café De Zoete Raat en de Nestkapel, en de Pluimstal waar de Pluimguhs wonen.",
            ["vadsrijk-schemerwortel", "vadsrijk-z3-quests"]),
        "vadsrijk-z3-quests": (
            "Quests in De Schemerwortel", "item:minecraft:writable_book",
            "Het verhaal van zone 3 (de gouden !) leidt je van de kruin over de takken en door de holle stam tot diep onder de wortels. "
            "Elke stap zegt bij wie en waar je moet zijn; rechtsboven en in je questlog (J) zie je altijd je volgende stap.",
            ["vadsrijk-schemerwortel", "vadsrijk-honingtak", "vadsrijk-z3-geheimen"]),
        "vadsrijk-z3-geheimen": (
            "Geheimen van De Schemerwortel", "item:minecraft:spyglass",
            "Tien knuffels (drie alleen zwevend te bereiken), vijf vreemde imkersporen in Honingtak, vijf schatten en acht oude reliëfs "
            "van het boomvolk in de wand van de stam. Ze glinsteren als je dichtbij bent.",
            ["vadsrijk-schemerwortel", "vadsrijk-z3-quests"]),
    },
    "tekst": [
        ("systemen/vadsrijk-schemerwortel", "Erheen",
         "Maak eerst het verhaal van de Splinterpieken af en word level 30. Dan vliegen Stuurguh Zeppie (De Haven) en Kapitein Poolster "
         "(Kandijkom) naar Honingtak. De eerste vlucht gaat langs de stam omhoog, door de wolken. Kijk goed."),
        ("systemen/vadsrijk-schemerwortel", "De lagen",
         "Bovenin de Kruinzee (lv 30-34), dan de Takkenbanen (lv 34-38), in de holle stam de Schorsstad (lv 40-43), op de grond de "
         "Wortelvelden (lv 43-46) en onder de boom het Wortelduister (lv 46-49). Er zijn twee kerkers: de Kaaszeppelinwerf en het Hart "
         "van de Boom."),
        ("systemen/vadsrijk-schemerwortel", "Je Pluimguh",
         "Bij Stalmeester Veertje in de Pluimstal kies je je eerste rijdier uit drie jonge Pluimguhs. Roep hem met X. Van hoge plekken "
         "zweef je met hem naar beneden; omhoog gaat met trappen, liften en de bloesemwind."),
        ("systemen/vadsrijk-honingtak", "Wie woont er?",
         "Imkermoeder Raatje (de burgemeester), Ordemeester Wasja in de Nestkapel (klasse wisselen en op klasse-level 50 je vierde "
         "titel), Stalmeester Veertje, Cafébaas Zoet, Jagersguh Raap met het premiebord, Smidguh Hamerhoning, Juweliersguh Glinsterhoning, "
         "Liftguh Katrolletje van de Honinglift en heel veel imkers met hoedjes en sluiers."),
        ("systemen/vadsrijk-honingtak", "Het premiebord",
         "Vanaf level 40 hangt er in de Jagershut elke dag een handvol premies: jagen, verzamelen, bezorgen en schimmel opruimen. Geen "
         "haast en geen ranglijst."),
        ("systemen/vadsrijk-z3-quests", "Zijquests",
         "Een paar korte zijquests naast het verhaal: verdwaalde Pluimkuikens zoeken, Opa Bast nog één keer naar de top brengen, "
         "honingthee met moerasbloemen, slapende lichtjes wakker maken en vijf uitkijkpunten voor Lusje's kaart."),
        ("systemen/vadsrijk-z3-geheimen", "Glinsters",
         "Hartjes = een knuffel, paarse sterretjes = een imkerspoor, goudgeel = een schat, wit licht = een reliëf van het boomvolk. Het "
         "FTB-hoofdstuk \"Het Vadsrijk\" heeft een eigen stukje voor De Schemerwortel; niks staat op slot. Je Verkenningsboek (B) houdt "
         "alles bij."),
    ],
}
