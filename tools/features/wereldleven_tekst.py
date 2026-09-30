"""
Het guhleven (2.8, wereldleven) - every text (Dutch everywhere, also in en_us: h.lang(key, text, text)).
"""
KNUFFELS = {
    "normal": ("Guhknuffel", "De allereerste knuffel: een gewone, zachte, vadsige guh. Knijp erin en hij piept."),
    "mint": ("Muntguhknuffel", "Fris als een muntje en net zo zacht."),
    "choco": ("Chocoguhknuffel", "Ruikt een beetje naar chocolade. Niet opeten, njeg."),
    "snow": ("Sneeuwguhknuffel", "Wit als verse sneeuw, warm als een dekentje."),
    "brontosaurus": ("Brontosaurusguhknuffel", "Met een lange nek, zodat hij over de andere knuffels heen kan kijken."),
    "golden": ("Gouden Guhknuffel", "Een glimmende gouden guh. De kermis-guhs zijn er jaloers op."),
    "rainbow": ("Regenboogguhknuffel", "Alle kleuren tegelijk. VAHOEG!"),
    "starry": ("Sterrenhemelguhknuffel", "Paars met sterretjes: om mee in slaap te vallen."),
    "ghost": ("Spookguhknuffel", "Een beetje doorzichtig. Boe! (Hij is heel lief.)"),
    "teckel": ("Teckelguhknuffel", "Extra lang, dus extra veel om te knuffelen."),
    "brococolief": ("Brococoliefknuffel", "In zijn roze onesie. Wat er in het briefje stond, zegt hij niet."),
    "ender": ("Enderguhknuffel", "Met kleine vleugeltjes. Vliegen doet hij niet, knuffelen wel."),
    "koning": ("Koningguhknuffel", "Met een kroontje en een witte manen. Zijne Vadsigheid."),
    "wolk": ("Wolkguhknuffel", "Een wolkje op zijn hoofd en zo licht als een pluisje."),
    "zeemeerguh": ("Zeemeerguhknuffel", "Met een vissenstaart. Past precies in je badje."),
    "mager": ("Magere-guhknuffel", "Een beetje grijs en dun... geef hem gauw een kaasknabbel!"),
    "vahoege_ender": ("Vahoege Enderguhknuffel", "Goud en roze, met vleugeltjes: de vahoegste knuffel van het Guheinde."),
    "kaasmoerasguh": ("Kaasmoerasguhknuffel", "Groen-geel gevlekt, en hij ruikt heel zacht naar kaassaus."),
    "asguh": ("Asguhknuffel", "Grijs met gloeiende wangetjes, maar hij is helemaal niet heet."),
    "pluisguh": ("Pluisguhknuffel", "Superpluizig, met een kuifje. Uit het Knuffeldal!"),
    "pinguh": ("Pinguhknuffel", "Zwart-wit met een oranje snoetje. Waggelen doet hij niet, knuffelen wel. Uit de Guhpolder!"),
    "glitter": ("Glitterguhknuffel", "Heel zeldzaam: een knuffel vol glitter die in het donker een beetje straalt. Hij glipt makkelijk uit de klauw!"),
}

SMAKEN = {
    "roze": ("Roze kaasijsje", "Aardbei-kaas. Je krijgt er blosjes van.", "Blosjes! %s kijkt heel verlegen en blij. VAHOEG!"),
    "mint": ("Munt-kaasijsje", "Fris munt met kaasschilfers. Je rent er even harder van.", "%s draagt nu een ijshoedje. Zo fris, njeg!"),
    "choco": ("Choco-kaasijsje", "Chocolade-kaas: je voelt je ineens zo licht als een pluisje.", "%s zweeft een beetje, met een choco-ijshoedje op!"),
    "bloesem": ("Bloesem-kaasijsje", "Alleen in de lente: roze bloesem met kaas. Blosjes en een warm gevoel.",
                "%s krijgt blosjes en een bloesem-ijshoedje. Lenteguh!"),
    "zonnetje": ("Zonnetje-kaasijsje", "Alleen in de zomer: citroen-kaas met een zonnetje erop. Zweverig en snel!",
                 "%s zweeft in de zon met een geel ijshoedje op. VAHOEG!"),
    "appeltaart": ("Appeltaart-kaasijsje", "Alleen in de herfst: appeltaart met kaas en kruimels. Warm van binnen.",
                   "%s smult en krijgt blosjes. Herfstknus!"),
    "sneeuw": ("Sneeuw-kaasijsje", "Alleen in de winter: sneeuwwit vanille-kaas. Brr... en toch lekker.",
               "%s zweeft als een sneeuwvlokje, met blosjes en een wit ijshoedje!"),
}

LIEDJES = {
    "toonladder": ("De VAHOEGE Toonladder", "Do re mi fa sol la si do: van de laagste staaf naar de hoogste. Het eerste liedje van elke guh."),
    "vader_guh": ("Vader Guh", "Vader Guh, vader Guh, slaapt gij nog, slaapt gij nog? Alle knabbels luiden... (In het middagdutje zingt niemand mee.)"),
    "altijd_vads": ("Altijd is Guhtje vads", "Altijd is Guhtje vads, midden in de knabbelzak. Nee hoor, niet te vads: gewoon nog niet vahoeg genoeg!"),
    "alle_guhtjes": ("Alle guhtjes zwemmen in de kaassaus", "Alle guhtjes zwemmen in de kaassaus, fal-de-ral-de-riere, fal-de-ral-de-raus!"),
    "maneschijn": ("In de knabbelmaneschijn", "In de knabbelmaneschijn, onder de roze maan van de Guhmensie."),
    "ode_aan_de_knabbel": ("Ode aan de Knabbel", "Het plechtigste guhlied van allemaal: over de allereerste kaasknabbel. Opa Guh krijgt er tranen van."),
}

MIJLPALEN = {
    "wereldleven_ijscoguh": "IJscoguh Tingeling ontmoet",
    "wereldleven_ijsjes": "Zeven kaasijsjes gegeten of gegeven",
    "wereldleven_gezwaaid": "Tien keer gezwaaid door een guh",
    "wereldleven_dutjes": "Vijf middagdutjes gezien",
    "wereldleven_marshmallows": "Vijf marshmallows bij het kampvuur",
    "wereldleven_koortjes": "Tien koortjes",
    "wereldleven_grijpen": "Tien knuffels gegrepen",
    "wereldleven_knuffels": "Tien keer een knuffel geknuffeld",
}

ADVANCEMENTS = {
    "wereldleven_ijsje": ("Tingeling!", "Eet een kaasijsje van IJscoguh Tingeling, of geef er een aan een guh"),
    "wereldleven_koortje": ("Het koortje", "Speel een liedje op de guh-xylofoon (of je fluitje) en laat guhs meezingen"),
    "wereldleven_liedjesboek": ("Het hele liedjesboekje", "Speel alle zes de liedjes op de guh-xylofoon"),
    "wereldleven_grijpmachine": ("Grijpen maar!", "Grijp een knuffel uit de grijpmachine"),
    "wereldleven_knuffelkast_vol": ("De volle knuffelkast", "Verzamel alle 22 guhknuffels, ook de glitterknuffel"),
}

LANG = {
    # blocks and items
    "block.guhs.guh_xylofoon": "Guh-xylofoon",
    "block.guhs.grijpmachine": "Grijpmachine",
    "item.guhs.guh_fluitje": "Guh-fluitje",
    "item.guhs.guh_fluitje.tooltip": "Fluit je laatste liedje: alle guhs in de buurt zingen mee (en de tuintjes groeien ervan).",
    "item.guhs.wereldleven_liedjesboekje": "Liedjesboekje",
    "item.guhs.wereldleven_liedjesboekje.tooltip": "Zes guhliedjes, met de staafjes van de guh-xylofoon. Oefenen mag altijd!",
    "item.guhs.marshmallow_knabbel": "Marshmallowknabbel",
    "item.guhs.ijscoguh_spawn_egg": "IJscoguh-spawnei",
    "item.guhs.koorstrikje": "Koorstrikje",
    "item.guhs.ijscopetje": "IJscopetje",
    "entity.guhs.ijscoguh": "IJscoguh Tingeling",
    "effect.guhs.blosjes": "Blosjes",
    "effect.guhs.zweverig": "Zweverig",
    "gui.guhs.wereldleven.seizoensijsje": "Seizoensijsje: alleen in de %s bij IJscoguh Tingeling",
    "gui.guhs.wereldleven.ijsje_tip": "Rechtsklik een guh ermee: hij krijgt het ijsje!",
    # the Guhdex page
    "gui.guhs.guhdex.rarity.ijscoguh": "Zeldzaamheid: komt langs (Guhmensie, ook op het plein van het Knuffeldal)",
    "gui.guhs.guhdex.info.ijscoguh": "IJscoguh Tingeling fietst met zijn ijscokar door de Guhmensie. Tingeling! Hij verkoopt kaasijsjes voor "
                                     "kaasknabbels, in elk seizoen een extra smaak. De guhs rennen hem achterna. Na een dag fietst hij weer verder.",
    # IJscoguh Tingeling
    "quest.guhs.ijscoguh.hallo.0": "Tingeling! Kaasijsjes! Zo koud dat je er vahoeg van wordt!",
    "quest.guhs.ijscoguh.hallo.1": "Njeg, wat een warme dag! Een kaasijsje erbij? Voor je guh ook eentje?",
    "quest.guhs.ijscoguh.hallo.2": "De Mika's willen altijd mijn ijsjes pikken. Maar ik fiets harder! Tingeling!",
    "quest.guhs.ijscoguh.hallo.3": "Geef een ijsje aan je guh: dan krijgt hij een ijshoedje. Zo schattig, njeg!",
    "quest.guhs.ijscoguh.alsjeblieft": "Alsjeblieft! Niet te snel likken, anders krijg je koude oortjes.",
    "gui.guhs.wereldleven.ijscoguh_komt": "Tingeling! Tingeling! IJscoguh Tingeling komt eraan!",
    "gui.guhs.wereldleven.ijscoguh_geen_plek": "IJscoguh Tingeling kan hier nergens staan, njeg.",
    # the day rhythm
    "gui.guhs.wereldleven.marshmallow": "%s roostert de marshmallowknabbel boven het kampvuur en smult hem op. Mmmm, VAHOEG!",
    # the koortje
    "gui.guhs.wereldleven.liedjesboekje": "Liedjesboekje",
    "gui.guhs.wereldleven.oefenen": "Oefenen! Op een echte guh-xylofoon zingen guhs mee.",
    "gui.guhs.wereldleven.xylofoon_tip": "Klik de staafjes of 1-8 / A-K",
    "gui.guhs.wereldleven.koortje": "♪ %s ♪ - %s guhs zingen mee! VAHOEG!",
    "gui.guhs.wereldleven.koortje_leeg": "♪ %s ♪ - speel het bij guhs in de buurt, dan zingen ze mee!",
    "gui.guhs.wereldleven.nieuw_liedje": "Nieuw in je liedjesboek: %s! Je krijgt %s kaasknabbels.",
    "gui.guhs.wereldleven.liedjesboek_vol": "Alle zes de liedjes! Je bent nu de koordirigent van de Guhmensie: hier is je koorstrikje. VAHOEG!",
    "gui.guhs.wereldleven.fluit": "Je fluit %s... %s guhs zingen mee!",
    "gui.guhs.wereldleven.fluit_leeg": "Je fluit %s. Er is geen guh in de buurt om mee te zingen, njeg.",
    # the grijpmachine
    "gui.guhs.wereldleven.grijp_kaartje": "Je hebt een kaartje nodig: een kermisbon of een munt van een minigame. Njeg!",
    "gui.guhs.wereldleven.grijp_glipt": "Njeg! Hij glipt uit de klauw... Net niet!",
    "gui.guhs.wereldleven.grijp_mis": "De klauw grijpt in de lucht. Niks eronder, njeg!",
    "gui.guhs.wereldleven.grijp_gepakt": "Gegrepen: %s! Plus een kermisbon. VAHOEG!",
    "gui.guhs.wereldleven.grijp_glitter": "WAUW! De GLITTERKNUFFEL: %s! Plus een kermisbon. SUPERVAHOEG!",
    "gui.guhs.wereldleven.grijp_gestopt": "De grijpmachine is gestopt. Je kaartje is op, njeg.",
    "gui.guhs.wereldleven.grijp_knop": "Grijp!",
    "gui.guhs.wereldleven.grijp_opnieuw": "Nog een keer!",
    "gui.guhs.wereldleven.grijp_goot": "Uitgifte",
    "gui.guhs.wereldleven.grijp_stuur": "WASD of pijltjes: sturen. Spatie: grijpen!",
    "gui.guhs.wereldleven.grijp_gewonnen": "Gegrepen! Hij ligt in je zak (of voor je voeten). VAHOEG!",
    "gui.guhs.wereldleven.grijp_spannend": "Spannend... spannend...",
    "gui.guhs.wereldleven.grijp_kaart": "Van boven",
    # the Knus tab
    "gui.guhs.knus.verzameling.knuffelkast": "Knuffelkast",
    "gui.guhs.knus.verzameling.liedjesboek": "Liedjesboek",
    "gui.guhs.knus.verzameling.ijsjes": "Kaasijsjes",
    # subtitles
    "subtitles.guhs.wereldleven.ijscobel": "Tingeling!",
    "subtitles.guhs.wereldleven.xylofoon": "Guh-xylofoon",
    "subtitles.guhs.wereldleven.fluitje": "Guh-fluitje",
    "subtitles.guhs.wereldleven.grijpklauw": "Grijpklauw",
}
for kid, (naam, info) in KNUFFELS.items():
    LANG[f"block.guhs.knuffel_{kid}"] = naam
    LANG[f"gui.guhs.knus.knuffelkast.{kid}"] = naam
    LANG[f"gui.guhs.knus.knuffelkast.{kid}.info"] = info
for sid, (naam, info, guh) in SMAKEN.items():
    LANG[f"item.guhs.kaasijsje_{sid}"] = naam
    LANG[f"item.guhs.kaasijsje_{sid}.tooltip"] = info
    LANG[f"gui.guhs.knus.ijsjes.{sid}"] = naam
    LANG[f"gui.guhs.knus.ijsjes.{sid}.info"] = info
    LANG[f"gui.guhs.wereldleven.ijsje_guh.{sid}"] = guh
for lid, (naam, info) in LIEDJES.items():
    LANG[f"gui.guhs.knus.liedjesboek.{lid}"] = naam
    LANG[f"gui.guhs.knus.liedjesboek.{lid}.info"] = info
for mid, naam in MIJLPALEN.items():
    LANG[f"gui.guhs.knus.mijlpaal.{mid}"] = naam
