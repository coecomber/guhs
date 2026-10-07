"""
einde2 (biomes): what the wiki must say about the six biomes of the outer islands, the kristalkaas and the knabbelkoor.
Not a feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (build contract 2.4). The pages
of the blocks, the items and the biomes themselves come from the game data.
"""
WIKI = {
    "systemen": {
        "buiteneilanden": (
            "De zes streken van de buiteneilanden", "block:guhs:gebarsten_korst",
            "Voorbij de ring van niets rond de Knabbelberg liggen de buiteneilanden. Sinds de guhs er zijn gaan kijken weten we: "
            "het is er niet overal hetzelfde. Er zijn zes streken, elk met zijn eigen kaas.",
            ["gatenkaasheuvels", "korstwoestijn", "kruimelwolken", "schimmelkaasvlakte", "kristalkaasklippen", "knabbelkoorbos", "kristalkaas",
             "knabbelkoorvrucht", "lichtstaafje", "mika_vesting"]),
        "knabbelkoor": (
            "Het knabbelkoor", "block:guhs:knabbelkoorbloem",
            "Hoge paarse planten die zachtjes zingen, met een vrucht die je een stukje verder flitst. Van de gepofte vrucht maak je "
            "bouwblokken en lichtstaafjes.",
            ["knabbelkoorplant", "knabbelkoorbloem", "knabbelkoorvrucht", "gepofte_knabbelkoorvrucht", "knabbelkoorblok", "knabbelkoorpilaar",
             "lichtstaafje", "knabbelkoorbos"]),
        "kristalkaas": (
            "Kristalkaas", "item:guhs:kristalkaas",
            "Het erts van de buiteneilanden: gouden kaaskristallen in de oude klipkaas van de Kristalkaasklippen.",
            ["kristalkaas_erts", "kristalkaas_kristal", "kristalkaas_blok", "kristalkaas_brokje", "klipkaas", "kristalkaasklippen"]),
    },
    "entity_home": {},
    "tekst": [
        ("buiteneilanden", "Waar ligt wat?",
         "Het grote eiland in het midden, met de Knabbelberg, is gebleven zoals het was. Pas vanaf ongeveer duizend blokken van het midden "
         "beginnen de streken. Twee ervan zijn groot en kom je overal tegen: de Gatenkaasheuvels en de Korstwoestijn. De andere vier "
         "zijn klein en zeldzaam, een paar honderd blokken groot, en liggen als vlekken in de grote twee: de Kruimelwolken en de "
         "Kristalkaasklippen in de Gatenkaasheuvels, de Schimmelkaasvlakte en het Knabbelkoorbos in de Korstwoestijn. De eilanden zelf "
         "hebben overal dezelfde vorm; wat verschilt is de grond en wat erop staat. Stukken van de buiteneilanden die al verkend waren "
         "voordat de streken er kwamen, blijven zoals ze waren."),
        ("buiteneilanden", "Gatenkaasheuvels",
         "Gele heuvels van kaaskorst vol ronde gaten, grote waar je in kunt zitten en kleine putjes. Hier en daar ligt een meertje "
         "kaassaus, er groeit korstgras en er liggen keien van kruimelrots. De gaten gaan nooit door de onderkant van een eiland heen."),
        ("buiteneilanden", "Korstwoestijn",
         "Vlak, droog en gebarsten. De grond is gebarsten korst met plekken kruimelzand; kruimelzand valt als zand en je zakt er een "
         "stukje in weg, dus je loopt er langzaam. De duinen zijn laagjes kruimelzand. Er staan dorre korststruiken, en heel af en toe "
         "vind je een oase: een vijver met helder water in een ring van verse kaaskorst vol korstgras."),
        ("buiteneilanden", "Kruimelwolken",
         "Tussen en boven de eilanden zweven kleine kruimeleilandjes en kruimelwolken. Door een kruimelwolk zak je langzaam heen en je "
         "landt zacht: geen valschade. Erop staan kan alleen wie heel licht is. Vier kruimelwolken pers je samen tot een dichte "
         "kruimelwolk, een stevig bouwblok waar je nooit valschade op krijgt."),
        ("buiteneilanden", "Schimmelkaasvlakte",
         "Alles is hier schimmelkaas: romige kaas met blauwe aders, tot diep in de grond. Er hangt een blauwgroene mist, er liggen "
         "vijvers en er groeien reuzenpaddenstoelen met een lichtgevende hoed. Kleine schimmelpaddenstoelen geven ook licht; uit een "
         "schimmelhoed valt er soms een."),
        ("buiteneilanden", "Kristalkaasklippen",
         "Hoge, scheve pilaren van oude klipkaas. In de klipkaas zit kristalkaaserts, en op de pilaren en op de grond groeien gouden "
         "kristalkaaskristallen die licht geven. Uit erts en kristal hak je kristalkaas (met Fortuin meer). Negen kristalkaasbrokjes "
         "zijn een kristalkaas, negen kristalkaas een blok."),
        ("buiteneilanden", "Knabbelkoorbos",
         "Een bos van hoge paarse knabbelkoorplanten op koorkorst. Op elke top zit een bloem die af en toe een toon zingt; elke bloem "
         "heeft zijn eigen toon, dus samen klinkt het als een slaperig koor."),
        ("knabbelkoor", "Kweken",
         "Zet een knabbelkoorbloem op de grond van de buiteneilanden (kaaskorst, koorkorst, schimmelkaas, klipkaas, gebarsten korst, "
         "kruimelzand, kruimelrots) of op een knabbelkoorblok. Hij groeit vanzelf omhoog en vertakt, tot de bloemen uitgegroeid zijn. "
         "Met bottenmeel op een jonge bloem schiet er in een keer een hele plant op. Hak je de voet om, dan valt de hele plant uit elkaar."),
        ("knabbelkoor", "De vrucht",
         "Uit een omgehakte stam valt af en toe een knabbelkoorvrucht. Eet je hem op, dan flits je een paar blokken verder: een korter "
         "sprongetje dan je van zulke vruchten gewend bent. Je kunt hem altijd eten, ook als je vol zit. Soms springt er iets kleins uit."),
        ("knabbelkoor", "Poffen en bouwen",
         "In de oven poft een knabbelkoorvrucht open. Vier gepofte knabbelkoorvruchten zijn vier knabbelkoorblokken; daarvan maak je "
         "trappen, platen en pilaren (ook met de steenzaag). Een gepofte vrucht op een guhkristal geeft vier lichtstaafjes."),
    ],
}
