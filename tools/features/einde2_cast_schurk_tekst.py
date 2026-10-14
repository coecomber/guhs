"""
einde2, slice cast_schurk - every Dutch text of the slice (the source language; English: tools/lang/en/c70_e2_cast_schurk.json).

  KINDS     GuhNpcEntity.Kind id -> its Dutch name (entity.guhs.guh_npc.<kind>)
  WEZENS    entity id -> (Dutch name, spawn egg base colour, spawn egg spot colour); the draakguhs' two own names: DRAKEN
  PRAATJES  kind or entity id -> its two lines of small talk (quest.guhs.einde2_cast_schurk.praatje.<id>.0 / .1)

The Mika's grumble, boast and scheme, and at most threaten to shove; the creatures are lief. Nothing of the plot: a line is a
wink at who someone is, never what happens in the story.
"""
P = "quest.guhs.einde2_cast_schurk.praatje."

KINDS = {
    "vuurheer_mikai": "Vuurheer Mikai",
    "vuurheer_mikai_komeet": "Feniks-koning Mikai",
    "vuurheer_mikai_vadsig": "Vadsige Mikai",
    "azula_mika": "Azula-Mika",
    "ty_lee_mika": "Ty Lee-Mika",
    "zhao_mika": "Admiraal Zhao-Mika",
    "long_feng_mika": "Long Feng-Mika",
    "dai_li_mika": "Dai Li-Mika",
    "vuurnatie_mika": "Vuurnatie-Mika",
    "gevangenis_directeur_mika": "De gevangenisdirecteur",
    "mika_kok": "De spijtige Mika-kok",
    "vestingkapitein_mika": "De vesting-kapitein",
}

WEZENS = {
    "draakguh": ("Draakguh", 0xC42C28, 0x2C56C4),
    "bosco": ("Beer Bosco", 0x7A5232, 0x32824A),
    "wan_shi_guh": ("Wan Shi Guh", 0x242028, 0xF6F4F0),
    "vosguh": ("Vosguh", 0xCE682C, 0xF8F0E6),
    "leeuwschildpad_guh": ("Leeuwschildpad-guh", 0x688C40, 0xEC8CA0),
    "flopsie": ("Flopsie", 0xF0EEE8, 0xB0A082),
    "luchtbizon_guh": ("Luchtbizon-guh", 0xFAF2E2, 0x966448),
    "maanvis": ("Maanvis Tui", 0xF6F4F0, 0x1E1C22),
    "oceaanvis": ("Oceaanvis La", 0x1E1C22, 0xF6F4F0),
}
DRAKEN = {0: "Ran", 1: "Shaw"}

PRAATJES = {
    "vuurheer_mikai": ("Kniel voor Vuurheer Mikai! ...Of ga dan in elk geval opzij. Anders duw ik je. Een beetje.",
                       "Ik ben de machtigste Mika tot nu toe. Alle knabbels van de wereld worden de MIJNE. Njeh-heh-heh."),
    "vuurheer_mikai_komeet": ("De knabbelkomeet is er! Voel mijn vuur! ...Het is lauw. Maar wel heel indrukwekkend lauw.",
                              "Noem mij voortaan Feniks-koning. Dat past beter op mijn kroon, njeg!"),
    "vuurheer_mikai_vadsig": ("Hmpf. De wereld veroveren is zo vermoeiend. Zo'n knabbel daarentegen... nom.",
                              "*boer* ...Dat was niet erg vuurheerlijk. Nog eentje dan. Ik ben nu gewoon vads."),
    "azula_mika": ("Bijna perfect is niet perfect. En mijn twee lokken zitten PERFECT.",
                   "Ik duw je opzij. Heel netjes, heel precies. Njeh-heh-heh."),
    "ty_lee_mika": ("Jouw aura is helemaal roze! ...O nee wacht, dat is mijn vacht.",
                    "Prik, prik, prik! Haha, grapje, ik doe je niks. Zal ik een radslag voor je doen?"),
    "zhao_mika": ("Ik ben Zhao de Onoverwinbare! Dat zeg ik zelf, dus het is waar.",
                  "Die maanvis wil ik in een zakje. Voor mijn verzameling. Grrr."),
    "long_feng_mika": ("Er is geen knabbeltekort in Ba Sing Guh. Hier is het rustig. Hier is iedereen veilig.",
                       "*fluistert* De Dai Li ziet alles. Ook dat jij net een knabbel opat."),
    "dai_li_mika": ("Doorlopen. Niks aan de hand. Er is geen knabbeltekort in Ba Sing Guh.",
                    "Wij zijn de Dai Li. Wij zien alles. ...Waar is mijn hoed? O, die zit over mijn ogen."),
    "vuurnatie_mika": ("Doorlopen, guh! Anders duw ik je met mijn duwstok. Zachtjes, maar toch.",
                       "Lang leve Vuurheer Mikai! ...Is het al pauze? Ik lust wel vuurvlokken."),
    "gevangenis_directeur_mika": ("Uit Het Kokend Rotsblok is nog NOOIT iemand ontsnapt! ...Ik tel voor de zekerheid even mijn sleutels.",
                                  "Een, twee, drie... hoeveel sleutels had ik nou? Niet weglopen, jij!"),
    "mika_kok": ("Sorry... de knabbelsoep is weer zonder knabbel. Sorry.",
                 "Eigenlijk wil ik taartjes bakken voor guhs. Maar hier mag dat niet. Sorry, njeg."),
    "vestingkapitein_mika": ("Ik zie alles door mijn verrekijker! ...Behalve wat er achter me gebeurt.",
                             "Niemand komt mijn vesting binnen! En wie het toch doet, wordt naar buiten GEDUWD!"),
    # the creatures
    "draakguh": ("GRRRAAAH! ...Sorry, zo groeten draakguhs. Laat jij je vuur ook eens zien, kleine vads?",
                 "Wij dansen al eeuwen. Dans je mee? Vuur in alle kleuren, en niemand brandt zich."),
    "bosco": ("Brom. ...Gewoon een beer.", "*snuffelt aan je zakken* Brom? Honing? Knabbels?"),
    "wan_shi_guh": ("Ik ben Hij Die Tienduizend Dingen Weet. Raak mijn boeken niet aan met vette knabbelpootjes.",
                    "Je komt toch voor de kennis, kleine guh? Niet om er iets mee uit te halen? Hoe-hoe."),
    "vosguh": ("*kwispel* Wij zoeken kennis voor de grote uil! Deze rol moet naar plank zes.",
               "*snuffel* Jij ruikt naar knabbels. Dat staat in geen enkel boek."),
    "leeuwschildpad_guh": ("Langzaam... praten... is... ook... een... kunst... njeg.",
                           "Ik ben ouder dan de knabbelkomeet. En nog steeds gewoon een guh: lief, en heel erg groot."),
    "flopsie": ("FLOP! *likt je hele gezicht* Flopsie blij!", "*springt op en neer* Knuffel? Knuffel! KNUFFEL!"),
    "luchtbizon_guh": ("Mrrrooo! (Een luchtbizon-guh zegt hallo. Zes poten, dus zes keer zoveel knuffels.)",
                       "*snuift* Mrrooo. (Hij vindt dat je best even op zijn zachte rug mag liggen.)"),
    "maanvis": ("*blub* Tui zwemt rondjes om La, en La om Tui. Zo blijft de maan aan de hemel.",
                "*blub-njeg* Niet in een zakje stoppen, alsjeblieft."),
    "oceaanvis": ("*blub* La is de zee en Tui is de maan. Samen draaien we rondjes.",
                  "*blub* Zwem je mee? Het water is lekker lauw."),
}


def texts():
    """lang key -> Dutch text, everything of the slice."""
    out = {}
    for kind, naam in KINDS.items():
        out[f"entity.guhs.guh_npc.{kind}"] = naam
    for id, (naam, _, _) in WEZENS.items():
        out[f"entity.guhs.{id}"] = naam
    for n, naam in DRAKEN.items():
        out[f"entity.guhs.draakguh.{n}"] = naam
    for id, regels in PRAATJES.items():
        for i, regel in enumerate(regels):
            out[f"{P}{id}.{i}"] = regel
    return out
