"""
einde2, slice cast_team - every Dutch text of the slice (the source language; English: tools/lang/en/c68_e2_cast_team.json).

  NAMEN      kind id -> Dutch name (entity.guhs.guh_npc.<kind>; a look may carry another name than its base)
  PRAATJES   kind id -> its two lines of small talk (quest.guhs.einde2_cast_team.praatje.<kind>.0 / .1): Dutch with the guh /
             vads / njeg humour, one of the two with the character's catchphrase of the show, guh-ified (CONTRACT_E2 11.2 G)

Phase A has no story text: the lines are a wink at who someone is, nothing of the plot. Guhs are always lief; Zuka and
Oom Miroh are Mika's, and Zuka grumbles at most.
"""
P = "quest.guhs.einde2_cast_team.praatje."

NAMEN = {
    "aang_guh": "Aang-guh",
    "katara_guh": "Katara-guh",
    "sokka_guh": "Sokka-guh",
    "toph_guh": "Toph-guh",
    "zuka": "Zuka",
    "zuka_prins": "Prins Zuka",
    "zuka_vuurheer": "Vuurheer Zuka",
    "zuka_balling": "Prins Zuka, verbannen",
    "zuka_blauwe_geest": "De Blauwe Geest",
    "zuka_team": "Zuka",
    "oom_miroh": "Oom Miroh",
    "oom_miroh_gevangene": "Oom Miroh",
    "oom_miroh_lotus": "Oom Miroh",
    "suki_guh": "Suki-guh",
    "suki_guh_gevangene": "Suki-guh",
    "yue_guh": "Prinses Yue-guh",
    "yue_guh_maan": "Yue-guh, de Maangeest",
    "veerguh": "Veerman-guh",
    "scheepsbouwerguh": "Scheepsbouwer-guh",
    "kroegbaasguh": "Kroegbaas",
    "kapiteinguh": "Oude kapitein-guh",
    "ringenraceguh": "Racemeester-guh",
}

PRAATJES = {
    "aang_guh": ("Hoi! Ik ben Aang-guh! Zullen we pinguïnsleeën? Of knabbels eten? Of allebei, njeg!",
                 "Jip jip! ...O, Appa-guh doet een dutje. Dan wachten we gewoon even, guh."),
    "katara_guh": ("Ik laat nooit een guh in de steek die me nodig heeft. Ook niet als hij alleen maar honger heeft.",
                   "Sokka-guh! Stop eens met knabbelen en help me met de tent!"),
    "sokka_guh": ("Ik ben gewoon de knabbel-en-sarcasme-guh. Iemand moet het doen, njeg.",
                  "Boemerang! ...Hij komt altijd terug. Behalve die ene keer. Daar praten we niet over."),
    "toph_guh": ("Ik ben de grootste aardestuurder van de wereld! Vraag maar aan de grond, die is het met me eens.",
                 "Ik zie niks, maar ik voel je pootjes trillen. Jij hebt honger, hè? Ik ook, njeg."),
    "zuka": ("Welkom in De Jasmijn Draak. Thee? Ja. Glimlachen? Nee.",
             "MIJN EER! ...Sorry. Oom zegt dat ik dat niet meer tegen de gasten mag roepen. Knabbel erbij?"),
    "zuka_prins": ("Ik ben eindelijk weer thuis in het paleis. Waarom voelt het dan niet als thuis, njeg?",
                   "Iedereen zegt dat ik mijn eer terug heb. Ik weet het niet zo zeker meer. Grmbl."),
    "zuka_vuurheer": ("Als Vuurheer beloof ik vrede, thee en knabbels voor iedereen. Oom heeft de thee al gezet.",
                      "Ik heb mijn eer niet teruggevonden. Ik heb hem zelf gemaakt. Met een beetje hulp, guh."),
    "zuka_balling": ("Ik MOET de Guhvatar vangen! Alleen dan krijg ik mijn eer terug!",
                     "Oom, ik wil geen thee! ...Nou ja. Eén kopje dan. Grmbl."),
    "zuka_blauwe_geest": ("...*De Blauwe Geest zegt niets en wijst zwijgend naar de uitgang.*",
                          "...*Een knikje onder het masker. Heel even lijkt het of de grijns echt lacht.*"),
    "zuka_team": ("Eh... hallo. Zuka hier. Ik ben nu goed, hoor. Echt waar.",
                  "Ik leer Aang-guh vuurstuurderen. Hij nieste net een vlammetje. Ik ben best trots, njeg."),
    "oom_miroh": ("Ah, een gast! Kom zitten. Niets maakt een dag zo goed als een kopje jasmijnthee.",
                  "Een knabbel smaakt het lekkerst als je hem deelt. Hohoho, neem er gerust nog een."),
    "oom_miroh_gevangene": ("De thee hier is heet water met een blaadje. Maar het water is heet, en dat is al heel wat.",
                            "Ik doe elke dag mijn oefeningen: opdrukken, buikjes en héél veel geduld, guh."),
    "oom_miroh_lotus": ("De Witte Lotus bloeit voor wie de juiste tegel weet te leggen. Een potje Pai Sho?",
                        "Zelfs in de donkerste tijd vind je ergens licht. Of een knabbel. Meestal allebei."),
    "suki_guh": ("Ik ben Suki-guh, krijgster van Kyoshi. Wij vechten met waaiers, en we zijn er heel goed in.",
                 "Dit is geen make-up, dit is krijgsverf. ...Al zit hij wel héél mooi vandaag, njeg."),
    "suki_guh_gevangene": ("Uit Het Kokend Rotsblok ontsnapt niemand. Tenzij je een plan hebt. En vrienden. En knabbels.",
                           "Mijn waaiers zijn ze kwijt, maar mijn moed niet, guh."),
    "yue_guh": ("Welkom in de Noordelijke Waterstam! Ik mag gasten niet lastigvallen, zegt papa. Dus: hallo!",
                "Ik kijk elke avond naar de maan. Ik geloof dat hij een beetje terugkijkt."),
    "yue_guh_maan": ("Ik ben er altijd, Sokka-guh. Kijk 's nachts maar omhoog.",
                     "De maan en de oceaan houden elkaar in evenwicht. Net als knabbels en een dutje, njeg."),
    "veerguh": ("Kaartjes, alstublieft! Kaartjes! ...Een knabbel mag ook. Twee is nog beter.",
                "De boot vertrekt pas als iedereen aan boord is. Of als ik zin heb in thee, guh."),
    "scheepsbouwerguh": ("Tik, tik, tik! Een boot bouwen is net knabbels stapelen: de onderste moet stevig zijn.",
                         "Dit wordt mijn mooiste schip tot nu toe. Tot ik het volgende bouw, njeg."),
    "kroegbaasguh": ("Welkom in de kroeg! Hier drink je knabbelsap en vertel je sterke verhalen.",
                     "*poetst een pul* Deze is al schoon. Maar poetsen is zo gezellig, guh."),
    "kapiteinguh": ("Ik heb ooit een vis gevangen die zo groot was als... nee, groter. VEEL groter.",
                    "De leegte is net een zee, maar dan zonder water. En zonder vissen. Wel met knabbels, njeg."),
    "ringenraceguh": ("Door de ringen vliegen, en er niet tegenaan! Dat laatste deed ik vroeger. Vaak.",
                      "Klaar? Af! ...O, je hebt nog geen vlieger. Dan wachten we even, guh."),
}
