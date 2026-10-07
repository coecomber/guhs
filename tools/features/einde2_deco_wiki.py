"""
einde2 (deco): what the wiki must say about the small decoration buildings of the outer islands.
Not a feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (build contract 2.4). The pages
of the structures and of the four little blocks themselves come from the game data.
"""
WIKI = {
    "systemen": {
        "buiteneilanden_bouwsels": (
            "Kleine bouwsels van de buiteneilanden", "block:guhs:einde2_deco_kool",
            "Wie over de buiteneilanden van het Guheinde zwerft, komt heel af en toe iets kleins tegen: een verlaten stal, een "
            "theekraampje, een kar vol geplette kolen. Er woont niemand en er valt weinig te halen, maar er hangt altijd een bordje.",
            ["deco_luchtbizonstal", "deco_meditatiepaal", "deco_luchtbalveld_ruine", "deco_geestenoase_vijver", "deco_ijsvissershutje",
             "deco_waterstam_kano", "deco_bevroren_kaasbol", "deco_das_molguh_grot", "deco_aardrommel_ring", "deco_koolkraam_wrak",
             "deco_geheime_tunnel", "deco_sintelstrand_huisje", "deco_zonnekrijger_altaar", "deco_theekraampje", "deco_mika_wachtpost",
             "deco_vetschipwrak", "deco_karavaanresten", "deco_pai_sho_tafeltje", "deco_appa_afdruk", "deco_boemerangboom",
             "einde2_deco_kool", "einde2_deco_windgong", "einde2_deco_eeuwige_vlam", "einde2_deco_lotustegel"]),
    },
    "entity_home": {},
    "tekst": [
        ("buiteneilanden_bouwsels", "Waar staat wat?",
         "Elk bouwsel hoort bij een streek. In de Kruimelwolken: de verlaten luchtbizon-stal, de meditatiepaal en de ruïne van een "
         "luchtbalveld. Op de Schimmelkaasvlakte: de geestenoase-vijver met de twee vissen, het ijsvissershutje, de Waterstam-kano op het "
         "droge en de bevroren kaasbol. In de Kristalkaasklippen: de das-molguh-grot, de Aardrommel-ring, het koolkraam-wrak en de geheime "
         "tunnel. In het Knabbelkoorbos: het Sintelstrand-zomerhuisje, het Zonnekrijger-altaar en het theekraampje. In de Gatenkaasheuvels "
         "en de Korstwoestijn: de Mika-wachtpost en het vetschipwrak; alleen in de Gatenkaasheuvels de boemerangboom; alleen in de "
         "Korstwoestijn de karavaanresten bij een oase, het Pai Sho-tafeltje en de Appa-afdruk."),
        ("buiteneilanden_bouwsels", "Hoe vind je ze?",
         "Niet met het Superkompas: ze staan er niet in. Ze zijn zeldzaam en staan ver uit elkaar, dus je loopt er vanzelf een keer "
         "tegenaan. Ze zijn van niemand: je mag alles meenemen, ook de bordjes. Er woont wel eens een dier bij (een das-molguh in zijn "
         "hol, een otter-pinguïnguh bij het wak, schildpad-eendguhs in de vijver): die blijven lief in de buurt."),
        ("buiteneilanden_bouwsels", "Vier dingetjes om mee te nemen",
         "De kool van de koolkraam (spring erop en hij is plat: MIJN KOLEN!), de windgong van de luchtnomaden (tingelt vanzelf, en "
         "harder als je hem aantikt), de eeuwige vlam van de Zonnekrijgers (een regenboogvuurtje dat licht geeft en niemand brandt) en "
         "de witte lotustegel van het Pai Sho-tafeltje. Soms zit er ook eentje in een kist."),
    ],
}
