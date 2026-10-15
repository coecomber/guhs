"""
einde2 (zij): what the wiki must say about the side places of the Guhvatar story, the Leegtehaven, the ring race, the four
lasting chonk arts and the two scenes of the Guheinde itself. Not a feature module (not in FEATURES): the docs step reads
WIKI and turns it into pages (CONTRACT_E2 2.4). The pages of the items (kristalsnoep-ring, schub, spijtsoep) and of the
places come from the game data by themselves.
"""

WIKI = {
    "systemen": {
        "vadskunsten": (
            "De vier blijvende vadskunsten", "item:minecraft:feather",
            "Elk element dat je leert in het verhaal van de Guhvatar laat iets blijvends achter: een luchtsprong, over water lopen, "
            "een klein aardzuiltje en een handvlam. Eén toets: tikken gebruikt, vasthouden opent het elementenwiel.",
            ["omashguh", "westelijke_luchttempel", "vuurtempel_roku", "kyoshi_dorp"]),
        "zijpaden_guhvatar": (
            "Zijpaden van de Guhvatar", "item:guhs:einde2_zij_kristalsnoep_ring",
            "Zes plekken op de buiteneilanden met elk een eigen kort verhaal: koning Bumi-guh in Omashguh, de stille Westelijke "
            "Luchttempel, het vuur van Roku-guh, de waaiers van Kyoshi, de bibliotheek van Wan Shi Guh en de zegen van de "
            "Leeuwschildpad.",
            ["omashguh", "westelijke_luchttempel", "vuurtempel_roku", "kyoshi_dorp", "bibliotheek_wan_shi_guh", "leeuwschildpad_eiland"]),
        "leegtehaven_leven": (
            "Leven in de Leegtehaven", "item:guhs:flitskruimel",
            "De zwevende haven van het Guheinde: ruilen met Ender-Mika's, de gratis veerboot De Vadsige Golf, de scheepswerf voor je "
            "eigen vetbootje en het keukentje van de spijtige Mika-kok.",
            ["leegtehaven", "mika_vesting", "vetbootje_fluitje"]),
        "ringenrace": (
            "De Guhvleugel-ringenrace", "item:guhs:guhvleugels",
            "Achttien ringen, één landingsbaan en een klok. Je eigen record en het serverrecord staan op het bord; onder de 45 "
            "seconden word je Ringenracer.",
            ["guhvleugel_ringenrace", "guhvleugels"]),
    },
    "entity_home": {},
    "tekst": [
        ("vadskunsten", "De toets",
         "De vadskunsten hebben geen item. Je gebruikt ze met één instelbare toets (standaard H, bij Opties > Besturing > Guhs). "
         "Tik je op de toets, dan gebruik je de gekozen vadskunst. Houd je hem vast, dan opent het elementenwiel: wijs met de muis "
         "een vadskunst aan en laat de toets los om hem te kiezen. Naast je hotbar staat een icoontje van wat je gekozen hebt, met "
         "de afkoeltijd. In de grote verhaalgebouwen staan er soms meer kunsten in het wiel: die werken alleen daar."),
        ("vadskunsten", "Lucht, water, aarde, vuur",
         "Luchtsprong: een grote sprong omhoog, en nog één keer in de lucht (je landt altijd zacht). Over water lopen: vijftien tellen "
         "lang loop je over water; sluip om erin te zakken. Aardzuiltje: een zuiltje van drie blokken "
         "rijst op waar je naar kijkt, of onder je eigen voeten, en verkruimelt na acht tellen; waar de grond beschermd is gooit de "
         "aarde je alleen een stukje omhoog. Handvlam: steekt een kaars of kampvuur aan waar je naar kijkt, of geeft je een "
         "vlammetje in je poot dat de duisternis om je heen verlicht."),
        ("zijpaden_guhvatar", "Eerst het element",
         "Bij Omashguh (aarde), de Westelijke Luchttempel (lucht), de Vuurtempel van Roku-guh (vuur) en het Kyoshi-dorp (water) "
         "begint het verhaal pas als je dat element in het verhaal van de Guhvatar geleerd hebt. Tot die tijd zegt de bewoner "
         "vriendelijk: kom terug als je kunt ...sturen, njeg. De bibliotheek en de Leeuwschildpad vragen geen element."),
        ("zijpaden_guhvatar", "Wat je krijgt",
         "Omashguh: de kristalsnoep-ring (lik eraan en je graaft een tijdje extra snel). Westelijke Luchttempel: twee windgongen. "
         "Vuurtempel: het eeuwige vlammetje. Kyoshi-dorp: Kyoshi-schmink voor je guh. Bibliotheek: boeken, en daarna elke dag een "
         "boek in ruil voor een stukje kennis. Leeuwschildpad: zijn schub, die je opvangt als je in de leegte valt."),
        ("leegtehaven_leven", "De markt",
         "Vier Ender-Mika's staan achter hun kraam. Elke kraam heeft elke dag drie andere ruilen, en elke ruil kun je vier keer per "
         "dag doen. Flitskruimels, doosschillen, knabbelkoorvruchten, kristalkaas en meer."),
        ("leegtehaven_leven", "De veerboot",
         "De veerman-guh vaart je gratis naar elke plek van het Guheinde die je kent (de verhaalgebouwen die open zijn voor jou en "
         "de gebouwen die je ontdekt hebt) en naar het hoofdeiland."),
        ("leegtehaven_leven", "De spijtige Mika-kok en je eigen vetbootje",
         "In de keuken van elke Mika-vesting kookt een spijtige Mika-kok stiekem voor de gevangen guhs. Help hem, en hij verhuist "
         "naar zijn eigen keukentje in de Leegtehaven, waar hij je elke dag een kom Spijtsoep geeft. Aan de steiger van elke "
         "Mika-vesting hangt een briefje over een eigen vetbootje: de scheepsbouwer-guh in de Leegtehaven bouwt het voor je."),
        ("ringenrace", "Zo werkt het",
         "Praat met de racemeester-guh op het startplatform. Na het aftellen vlieg je door de achttien ringen, op volgorde (de "
         "volgende ring fonkelt), en je landt op de landingsbaan. Raak je onderweg de grond, dan ben je uit de race: geen probleem, "
         "begin gewoon opnieuw."),
        ("guheinde", "De Opper-Mika verschijnt",
         "Begint er een gevecht tegen de Opper-Mika, dan zie je hem de eerste keer in een korte film uit de leegte opstijgen op de "
         "Hongerige Enderguh. Herbekijken kan in de Guhdex."),
        ("guheinde", "Het gedicht van het Guheinde",
         "De eerste keer dat je het Guheinde verlaat nadat je de Opper-Mika hebt verslagen, praten twee stemmen over jou, en "
         "daarna rolt de aftiteling. Herbekijken kan in de Guhdex."),
    ],
}
