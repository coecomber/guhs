"""
bbq2 (ring-h5) - the wiki entries of chapter 5 of the Knabbelring, "De Zwarte Roosterpoort" (CONTRACT_130 2.4; the docs step
of phase 3 turns this into pages; this module is not in FEATURES and builds nothing).
"""
from features import wereld

_VERHAAL = wereld.wiki_questlijn(
    "ring_h5", "De Zwarte Roosterpoort",
    "Hoofdstuk 5 van In de ban van de Knabbelring. De poort naar het land van Sausron is een rooster zo hoog als een huis, en hij zit dicht. "
    "Vanaf zijn toren staart het Oog van Sausron het dal in. Smikagol wijst je een sluipweg: over het Asveld, de Kale Vlakte en door de "
    "Schaduwlaan naar het Roosterpoortje. Wie gezien wordt, staat weer bij het laatste rustvuurtje.",
    "zwarte_roosterpoort", ["boromika", "smikagol", "guhdalf"], [("guhs_knabbelring", "ring_h5")],
    related=["verhalen/knabbelring", "systemen/oog-van-sausron", "systemen/gaven-van-guhladriel", "systemen/rustpunten"])

WIKI = {
    "verhalen": dict([_VERHAAL]),
    "systemen": {
        "oog-van-sausron": ("Het Oog van Sausron", "entity_oog_van_sausron",
                            "Het ene brandende Mika-oog op de toren achter de Zwarte Roosterpoort. Zijn blik is een lichtvlek op de grond die je "
                            "kunt zien aankomen. Hij heeft gewoon trek.", ["verhalen/ring_h5", "verhalen/knabbelring"]),
    },
    "npc_home": {},
    "entity_home": {"oog_van_sausron": "verhalen/ring_h5", "ringh5_roosterwachter": "verhalen/ring_h5"},
    "tekst": [
        ("verhalen/ring_h5", "Waar is het?",
         "In het Asdal van de Guhbarbecuether, een paar honderd blokken lopen vanaf de boomstad van Guhladriel. Er is er maar één per wereld. "
         "Tot je hoofdstuk 4 af hebt, hangt Guhdalfs sluier eromheen. Je Superkompas ('Mijn verhaal') en Sam-guh wijzen de weg; de ingang is de "
         "grotmond bij het kamp."),
        ("verhalen/ring_h5", "Boromika",
         "Bij het kampvuur wacht Boromika, een Mika die echt wil helpen. Eén keer wint de ring het van hem: hij grijpt ernaar, Sam-guh springt "
         "ertussen, en hij schaamt zich diep. Daarna blijft hij bij het kamp de wacht houden."),
        ("verhalen/ring_h5", "Smikagol",
         "De dief bij de proviand is Smikagol. Hij zweert op zijn 'vadsje' dat hij de weg wijst. Vanaf nu rent hij voor je uit en wacht hij "
         "bij elke schuilplek. Klik op hem en hij zegt wat je hier moet doen."),
        ("verhalen/ring_h5", "Het Asveld",
         "Het licht van het Oog loopt heen en weer over het veld. Schuil achter een muurtje, onder een omgevallen rooster of in een gespleten "
         "rots (er moet iets tussen jou en het Oog staan) tot het voorbij is, en ren dan naar de volgende schuilplek. Een halve tel in het licht "
         "en je staat weer bij je laatste rustvuurtje."),
        ("verhalen/ring_h5", "De Kale Vlakte",
         "Niks om achter te zitten. Heb je het Elfenmanteltje bij je, buk dan en sta stil als het licht eraan komt: als rots ziet het Oog je niet."),
        ("verhalen/ring_h5", "De Schaduwlaan",
         "Onder de muur kan het Oog niet kijken. Eerst hangt er rook: laat het Lichtflesje flitsen en hij waait weg (voor jou, voor altijd). "
         "Dan rijden er twee ruiters van de Negen heen en weer: een flits verblindt ze acht tellen, en een rots rijden ze voorbij."),
        ("verhalen/ring_h5", "Het Wachthek",
         "Drie Roosterwachters kijken de laan in. Het zijn Mika's, en Mika's zien niemand die de Knabbelring om heeft. Doe hem bij de "
         "schedelpaal om, loop door het hek en doe hem er meteen weer af: na vier tellen heeft het Oog je, en doe je hem te vroeg om dan ruiken "
         "de ruiters hem."),
        ("verhalen/ring_h5", "Het Roosterpoortje",
         "Het poortje zit op slot en de Negen komen eraan. Dan staat Guhdalf de Witte op de muur: hij verblindt de ruiters en lokt de blik van "
         "het Oog naar zich toe. Het poortje gaat open (alleen voor jou: een vriend die nog niet zover is, ziet tralies) en je rent de gang "
         "door naar het vuurtje achter de muur."),
        ("systemen/oog-van-sausron", "Zo werkt zijn blik",
         "Het Oog kijkt naar één plek tegelijk: de lichtvlek met het oog erin en vlammetjes eromheen. Staat er iemand op het Asveld of de Kale "
         "Vlakte, dan loopt de vlek daar heen en weer over de route. Het Oog ziet je alleen als je in de vlek staat én er niets tussen jou en "
         "de toren staat. Een rots onder het Elfenmanteltje ziet hij niet. De Knabbelring wel: die voelt hij overal in het dal."),
        ("systemen/oog-van-sausron", "Na het verhaal",
         "Wie het verhaal af heeft, laat hij met rust: hij heeft zijn stukje ring gehad. Is er verder niemand, dan doet hij zijn oog dicht "
         "en slaapt. Het beeldje dat je krijgt knippert, geeft een beetje licht en heeft zo zijn gedachten als je erop klikt."),
    ],
}
