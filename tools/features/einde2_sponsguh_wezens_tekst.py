"""
einde2, slice sponsguh_wezens - every Dutch text of the slice (the source language; English: tools/lang/en/c65_e2_sponsguh_wezens.json).

  NAMEN     entity id -> (Dutch name, spawn egg base colour, spawn egg spot colour); the whole cast, in the order of the Java table Cast
  TEXTS     lang key -> Dutch text (names of blocks and items, their lore lines, the one fixed line of every character, the
            messages of the buddy, the pet, the ride, the car, the grill and Karen, the clothes, the op commands)
  PAGINAS   the two Guhdex pages (the buddy and the pet): id -> (name, rarity line, info)
  SOUNDS    sound event -> its entries in sounds.json; ONDERTITELS: sound event -> Dutch subtitle

Phase A has no story text: who says what in which scene is phase B. The one line a character says when you click it
(gui.guhs.einde2_sponsguh_wezens.praatje.<id>) is a wink at who it is, nothing of the plot.
"""
P = "gui.guhs.einde2_sponsguh_wezens."

NAMEN = {
    "sponsguh": ("Sponsguh Kaasbroek", 0xFAD64A, 0x88562C),
    "patguh": ("Patguh Ster", 0xFF96B0, 0x70C454),
    "meneer_krabguh": ("Meneer Krabguh", 0xE03E32, 0x96C8EC),
    "parel_guh": ("Parel", 0x96A0B0, 0xFA82B4),
    "sandy_guh": ("Sandy-guh", 0xF8F8FA, 0xAA8CC6),
    "gerrit": ("Gerrit", 0x96C4E2, 0xFA96BE),
    "octo_mika": ("Octo Tentakel", 0x78A8A4, 0x96643A),
    "octoliam": ("Octoliam", 0x84B0A8, 0x961E2E),
    "plankmika": ("Plankmika", 0x2C683E, 0xE22824),
    "juf_pufguh": ("Juf Pufguh", 0xECCE82, 0x3860BE),
    "neptuguh": ("Koning Neptuguh", 0x3EA060, 0xFAC83C),
    "mindy_guh": ("Prinses Mindy-guh", 0x40C4C4, 0xAA8CC6),
    "dennis_mika": ("Dennis-Mika", 0x28262E, 0xE4AABA),
    "cycloop_mika": ("De Cycloop", 0xC48E3E, 0x16282E),
    "david_hasselguh": ("David Hasselguh", 0xAA8CC6, 0xD62C30),
    "loopgraafmonster": ("Loopgraafmonster", 0x2A3C6E, 0xFFEC5A),
    "bikinibodem_guh": ("Bikinibodemguh", 0xFABE5A, 0x78BEEC),
    "spookkapitein": ("De Vliegende Guhlander", 0x78E896, 0x165038),
    "zeemeerman_guh": ("Zeemeerman", 0xF68C32, 0x965AC8),
    "mosselguh": ("Mosselguh", 0x5AC8DC, 0xC8282E),
}

PRAATJES = {
    "sponsguh": "Ik ben er klaar voor! Ik ben er klaar voor! ...Waarvoor ook alweer, njeg?",
    "patguh": "Is knabbelsaus een instrument?",
    "meneer_krabguh": "Hallo, ik hou van knabbels! En blijf met je pootjes van mijn knabbelkluis af, njeg.",
    "parel_guh": "Papaaa! Ik wil een nieuwe strik. En een pony. Een guhpony!",
    "sandy_guh": "Howdy! Niet aan mijn helm komen, daar zit mijn lucht in. Hai-jáh!",
    "gerrit": "Njeg.",
    "octo_mika": "Zucht. Ja, ik ben een Mika. Nee, ik duw niemand. Daar ben ik veel te moe voor.",
    "octoliam": "Ach, een vriendje van Octo. Heb je mijn jasje al bewonderd? Echt fluweel. Octo heeft dat niet.",
    "plankmika": "Ik heb op de Mika-universiteit gezeten! Ooit is dat recept van mij. OOIT!",
    "juf_pufguh": "Pootjes op tien voor twee en NIET in paniek raken. O Neptuguh, waarom doe ik dit werk...",
    "neptuguh": "Kniel voor koning Neptuguh! ...Zit mijn kroon wel recht? Het tocht zo op mijn hoofd.",
    "mindy_guh": "Wees een beetje lief voor elkaar, njeg. Dat geldt ook voor jou, papa.",
    "dennis_mika": "Ik ben Dennis. Ik duw alleen op bestelling.",
    "cycloop_mika": "... *zwaar ademen door een duikhelm* ...",
    "david_hasselguh": "Spring maar achterop, kleine vads. Ik zwem je overal heen.",
    "loopgraafmonster": "GRAAAH! ...Sorry, schrok je? Wil je een knuffel? We zingen zo een liedje.",
    "bikinibodem_guh": "MIJN POOT! ...O nee, hij zit er nog aan. Njeg.",
    "spookkapitein": "Boe! Ik ben de Vliegende Guhlander! Heb jij mijn eetsok ergens gezien?",
    "zeemeerman_guh": "KWAAAD! ...O, je bent gewoon een voorbijganger. Naar de onzichtbare bootmobiel!",
    "mosselguh": "Zeemeerman, je pap wordt koud. En nee, ik heb je riem niet gezien.",
}

TEXTS = {
    # --- the vehicle (no item, no egg)
    "entity.guhs.knabbelburgermobiel": "Knabbelburgermobiel",
    # --- blocks, their lore
    "block.guhs.karen": "Karen",
    "block.guhs.karen.lore": "Plankmika's computervrouw. Heeft overal een mening over, meestal: nee.",
    "block.guhs.krokante_grill": "Krokante grill",
    "block.guhs.krokante_grill.lore": "Bakt van een kaasknabbel en een brood één Knabbelburger. Als je het geheime recept kent, njeg.",
    "block.guhs.besteltafel": "Besteltafel",
    "block.guhs.besteltafel.lore": "Rechtsklik: ting! De bestelling komt eraan. Ooit.",
    "block.guhs.vruchthuis_wand": "Vruchthuiswand",
    "block.guhs.vruchthuis_wand.lore": "De schil van een knabbelkoorvrucht. Wonen in je eten: waarom niet?",
    "block.guhs.vruchthuis_dak": "Vruchthuisdak",
    "block.guhs.vruchthuis_dak.lore": "Het bladerkroontje bovenop het vruchthuis.",
    "block.guhs.vruchthuis_raam": "Vruchthuisraam",
    "block.guhs.vruchthuis_raam.lore": "Een rond raampje. Voor als je wilt zien wie er aan je huis knabbelt.",
    "block.guhs.vruchthuis_deur": "Vruchthuisdeur",
    "block.guhs.vruchthuis_deur.lore": "Een ronde deur met een patrijspoort.",
    "block.guhs.bellenblaas": "Bellenblaas",
    "block.guhs.bellenblaas.lore": "Speelgoed voor je guhs: zij jagen op de bellen. Rechtsklik blaast er een wolk uit.",
    "block.guhs.spooklantaarn": "Spooklantaarn",
    "block.guhs.spooklantaarn.lore": "Geeft griezelig groen licht. Staand of hangend, nooit eng. Nou ja, een beetje.",
    # (the bubble blower's line in the overview of a Guhhuisje: the screen asks this key by the toy's id; it is the one key of
    # the slice outside its own English chunk)
    "gui.guhs.huisje.overzicht.ding.bellenblaas": "Guhs jagen op de bellen en happen ernaar. Plop!",
    # --- food
    "item.guhs.knabbelburger": "Knabbelburger",
    "item.guhs.knabbelburger.lore": "Krokant broodje, vadsige burger. Het geheime ingrediënt blijft geheim.",
    "item.guhs.kwallenjam": "Kwallenjam",
    "item.guhs.kwallenjam.lore": "Jam van Kruimelkwallen. Gloeit nog een beetje na in je buik.",
    # --- the clothes (source: the story)
    "gui.guhs.kledingbron.einde2_sponsguh": "Het verhaal van Sponsguh Kaasbroek",
    "item.guhs.sponsguh_vierkante_broek": "Vierkante broek met stropdas",
    "item.guhs.sponsguh_knabbelpetje": "Krokante Knabbel-petje",
    "item.guhs.sponsguh_bloemetjesbroek": "Patguh's bloemetjesbroek",
    "item.guhs.sponsguh_luchthelm": "Sandy's luchthelm",
    "item.guhs.sponsguh_antennes": "Plankmika-antennes",
    "item.guhs.sponsguh_ijshoedje": "Guhfy Guhber-ijshoedje",
    "item.guhs.sponsguh_heldencape": "Heldencape",
    # --- the buddy
    P + "sponsguh.gekregen": "Sponsguh Kaasbroek is nu jouw maatje! Hij is er klaar voor, njeg.",
    P + "sponsguh.blijf": "%s blijft hier op je wachten.",
    P + "sponsguh.kom": "%s komt weer met je mee. Hij is er klaar voor!",
    P + "sponsguh.van_ander": "%s is het maatje van iemand anders.",
    P + "sponsguh.burger": "%s heeft een Knabbelburger voor je gebakken!",
    # --- the pet
    P + "gerrit.gekregen": "Gerrit is nu jouw slakje. Njeg.",
    P + "gerrit.blijf": "%s blijft hier. Traag, maar trouw.",
    P + "gerrit.kom": "%s komt weer mee. Het kan even duren, njeg.",
    P + "gerrit.van_ander": "%s is het slakje van iemand anders.",
    P + "gerrit.njeg": "Njeg.",
    # --- the ride and the car
    P + "hasselguh.blijf_zitten": "Blijf zitten, njeg! David zwemt nog.",
    P + "mobiel.rand": "Njeg! Daar houdt de weg op.",
    P + "mobiel.niet_van_jou": "Dit is niet jouw Knabbelburgermobiel, njeg.",
    # --- the grill
    P + "grill.bezig": "De grill sist al. Even geduld, njeg.",
    P + "grill.ingredienten": "Zonder kaasknabbel en brood valt er niets te bakken.",
    P + "grill.geheim": "Ssssss... het recept is geheim, njeg.",
    # --- Karen, by her face
    P + "karen.0": "Karen: \"Systeem actief. Wat wil je nu weer?\"",
    P + "karen.1": "Karen: \"Dat was bijna een goed plan. Bijna.\"",
    P + "karen.2": "Karen: \"Nee.\"",
    P + "karen.3": "Karen: \"Zucht. Ik had met een broodrooster moeten trouwen.\"",
    # --- op commands
    P + "commando.maatje": "%s staat bij je.",
    P + "commando.geen_maatje": "Je hebt er geen (probeer geef).",
    P + "commando.mobiel": "Een Knabbelburgermobiel voor jou alleen.",
    P + "commando.ruimop": "Je Knabbelburgermobiel is opgeruimd.",
    P + "commando.draag": "David Hasselguh neemt je mee.",
    P + "commando.geen_rit": "Er kan nu geen rit beginnen.",
    P + "commando.animatie": "%s speelt %s.",
    P + "commando.geen_animatie": "Niemand in de buurt die %s kan.",
    P + "commando.vlag": "%s: %s = %s.",
    P + "commando.geen_vlag": "Niemand in de buurt met een vlag.",
    P + "commando.cast": "%s wezens van het Sponsguh-verhaal staan klaar.",
}
TEXTS.update({f"entity.guhs.{id}": naam for id, (naam, _, _) in NAMEN.items()})
TEXTS.update({f"{P}praatje.{id}": tekst for id, tekst in PRAATJES.items()})

PAGINAS = {
    "sponsguh": ("Sponsguh Kaasbroek", "Eentje: de frietkok van De Krokante Knabbel in Bikinibodem",
                 "Een vierkant blok gatenkaas met guh-oortjes, een vierkante broek en een stropdas. Hij woont in een knabbelkoorvrucht "
                 "diep onder de buiteneilanden, bakt de beste Knabbelburgers van het Guheinde en is er altijd klaar voor. Als hij je maatje "
                 "wordt, loopt hij met je mee, zuigt hij gemorst water en kaassaus op, bakt hij elke dag een Knabbelburger voor je en lacht "
                 "hij zo aanstekelijk dat je guhs ervan gaan springen. Sluip en klik: dan blijft hij thuis op je wachten."),
    "gerrit": ("Gerrit", "Eentje: de slak van Sponsguh Kaasbroek",
               "Een slak met guh-oogjes op steeltjes en een roze huisje met een blauwe krul. Zegt njeg waar een poes miauw zou zeggen, en "
               "meent daar van alles mee. Als hij jouw slakje wordt, kruipt hij rustig achter je aan en laat hij een spoortje slijm "
               "achter. Aai hem voor een njeg, sluip en klik en hij blijft waar hij is."),
}

_GUH = [{"name": f"guhs:guh_ambient{i}", "pitch": 1.0, "volume": 0.8} for i in (2, 5, 9, 12)]
_MIKA = [{"name": f"guhs:mika_ambient{i}", "pitch": 0.8, "volume": 0.7} for i in (1, 4, 8, 11)]
SOUNDS = {
    "einde2_sponsguh_wezens.guh_babbel": _GUH,
    "einde2_sponsguh_wezens.mika_brom": _MIKA,
    "einde2_sponsguh_wezens.sponsguh_lach": [{"name": "minecraft:entity.dolphin.play", "type": "event", "pitch": 1.5},
                                             {"name": "minecraft:entity.dolphin.ambient", "type": "event", "pitch": 1.7}],
    "einde2_sponsguh_wezens.gerrit_njeg": [{"name": "minecraft:entity.cat.ambient", "type": "event", "pitch": 0.7, "volume": 0.7}],
    "einde2_sponsguh_wezens.octo_klarinet": [{"name": "minecraft:block.note_block.flute", "type": "event"},
                                             {"name": "minecraft:block.note_block.didgeridoo", "type": "event", "pitch": 1.6}],
    "einde2_sponsguh_wezens.monster_brul": [{"name": "minecraft:entity.ravager.roar", "type": "event", "pitch": 1.3, "volume": 0.6}],
    "einde2_sponsguh_wezens.monster_zing": [{"name": "minecraft:block.note_block.bass", "type": "event"},
                                            {"name": "minecraft:block.note_block.cow_bell", "type": "event", "pitch": 0.6}],
    "einde2_sponsguh_wezens.hasselguh_plons": [{"name": "minecraft:entity.dolphin.splash", "type": "event", "pitch": 0.7}],
    "einde2_sponsguh_wezens.mobiel_motor": [{"name": "minecraft:entity.minecart.riding", "type": "event", "pitch": 1.5, "volume": 0.4}],
    "einde2_sponsguh_wezens.mobiel_toet": [{"name": "minecraft:entity.goat.horn_break", "type": "event", "pitch": 1.6, "volume": 0.6},
                                           {"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 0.6}],
    "einde2_sponsguh_wezens.grill_sis": [{"name": "minecraft:block.fire.extinguish", "type": "event", "pitch": 1.5, "volume": 0.5}],
    "einde2_sponsguh_wezens.bestelbel": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.6},
                                         {"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.4}],
    "einde2_sponsguh_wezens.karen_piep": [{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 1.3, "volume": 0.6}],
    "einde2_sponsguh_wezens.bellen": [{"name": "minecraft:block.bubble_column.bubble_pop", "type": "event", "pitch": 1.4},
                                      {"name": "minecraft:entity.puffer_fish.blow_out", "type": "event", "pitch": 1.8, "volume": 0.5}],
}
ONDERTITELS = {
    "einde2_sponsguh_wezens.guh_babbel": "Een guh babbelt",
    "einde2_sponsguh_wezens.mika_brom": "Een Mika bromt",
    "einde2_sponsguh_wezens.sponsguh_lach": "Sponsguh lacht aanstekelijk",
    "einde2_sponsguh_wezens.gerrit_njeg": "Gerrit zegt njeg",
    "einde2_sponsguh_wezens.octo_klarinet": "Een klarinet, net naast de toon",
    "einde2_sponsguh_wezens.monster_brul": "Een monster brult (lief bedoeld)",
    "einde2_sponsguh_wezens.monster_zing": "Monsters zingen mee",
    "einde2_sponsguh_wezens.hasselguh_plons": "David Hasselguh plonst",
    "einde2_sponsguh_wezens.mobiel_motor": "Knabbelburgermobiel pruttelt",
    "einde2_sponsguh_wezens.mobiel_toet": "Knabbelburgermobiel toetert",
    "einde2_sponsguh_wezens.grill_sis": "De grill sist",
    "einde2_sponsguh_wezens.bestelbel": "Ting! Een bestelling",
    "einde2_sponsguh_wezens.karen_piep": "Karen piept",
    "einde2_sponsguh_wezens.bellen": "Bellen ploppen",
}
