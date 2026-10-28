"""
1.8.0 Guhdromenland, part F (dromen-ontwerp/BOUWPLAN_180.md; design ontwerp-guhs180-dromen.md 1b, rounds 5, 6, 11). Java:
feature/snoesverhaal (SnoesVerhaalFeature, SnoesVerhaal, SnoesScenes, DroomBronnen, SnoesCommando).

  build(h)    the texts of Snoes' story (Dutch in nl_nl, English in tools/lang/en/c213_snoesverhaal.json): Snoes at your
              bed, Bubbels the dream guide, the hints of the island NPCs, the finale scene; the hidden advancements
              guhs:quest/snoes_* (granted by SnoesVerhaal)
  ftb(fq)     the FTB chapter guhs_dromen of the whole update: the Blub-guh, the Weer vadser, sleeping well, Guhdromenland,
              Snoes' story and De Zingende Weide (tools/make_ftbquests.py: CHAPTERS["guhs_dromen"])

Standalone (from the project root): PYTHONPATH=tools python tools/features/snoesverhaal.py   (texts + advancements)
"""
import json
import os
import sys

STUKJES = {  # id -> (Dutch, English)
    "liedje": ("het liedje", "the song"), "ster": ("de ster", "the star"), "herinnering": ("de herinnering", "the memory"),
    "bad": ("het bad", "the bath"), "droomzand": ("het droomzand", "the dream sand"),
}
ADVANCEMENTS = ["snoes_proloog"] + [f"snoes_{s}" for s in STUKJES] + ["snoes_finale"]

S = "gui.guhs.snoes."
F = "scene.guhs.snoesverhaal_finale."
TEKSTEN = {  # key -> (Dutch, English)
    # --- the prologue -------------------------------------------------------------------------------------------------------
    S + "verschijnt": ("Er zit iemand naast je bed... een verfomfaaide guh met een geknakt oortje. Hij gaapt, maar hij slaapt niet. "
                       "Praat eens met hem.",
                       "Somebody is sitting next to your bed... a ruffled guh with a bent ear. He yawns, but he doesn't sleep. "
                       "Talk to him."),
    S + "proloog.0": ("Njeg... hoi. Ik ben Snoes. Sorry, ik zit gewoon even bij je bed. Het is hier zo knus.",
                      "Njeg... hi. I'm Snoes. Sorry, I'm just sitting by your bed for a bit. It's so cosy here."),
    S + "proloog.1": ("Iedereen heeft een plekje om te slapen. Een mandje, een nestje, een doos... Ik niet. Ik heb nog nooit ergens "
                      "geslapen.",
                      "Everybody has a little place to sleep. A basket, a nest, a box... Not me. I've never slept anywhere."),
    S + "geen_bedtijd": ("Het is nog geen bedtijd, njeg. Kom vanavond terug, dan... misschien...",
                         "It's not bedtime yet, njeg. Come back tonight, then... maybe..."),
    S + "ingestopt": ("Wil je... mij instoppen? Oh. Oh! Het is zacht. Ik doe mijn oogjes dicht. Maar ik slaap vast niet, hoor.",
                      "Will you... tuck me in? Oh. Oh! It's soft. I'll close my eyes. But I won't sleep, you know."),
    S + "ingestopt_vloer": ("Er is hier geen doosje of mandje... maar een dekentje op de vloer is ook lief. Ik doe mijn oogjes dicht. "
                            "Maar ik slaap vast niet, hoor.",
                            "There's no box or basket here... but a little blanket on the floor is nice too. I'll close my eyes. "
                            "But I won't sleep, you know."),
    S + "ga_slapen": ("Snoes ligt ingestopt. Ga nu zelf ook slapen, in je bed naast hem. Misschien kun je in zijn droom...",
                      "Snoes is tucked in. Now go to sleep yourself, in your bed next to him. Maybe you can get into his dream..."),
    S + "wakker_liggen": ("Ik lig hier. Oogjes open. Schaapjes tellen helpt niet: ik tel guhs, en die komen allemaal knuffelen.",
                          "I'm lying here. Eyes open. Counting sheep doesn't help: I count guhs, and they all come for a cuddle."),
    S + "ander": ("Oh, hoi! Ik wacht op iemand anders, njeg. Maar je mag me wel even aaien.",
                  "Oh, hi! I'm waiting for somebody else, njeg. But you may pet me for a bit."),
    S + "thuis.0": ("Je droomt over mij? Echt waar? Hoeveel stukjes heb je al? %s van de vijf? Njeg...",
                    "You dream about me? Really? How many pieces do you have? %s of the five? Njeg..."),
    S + "thuis.1": ("Ik heb vannacht héél even mijn oogjes dicht gehad. Een seconde. Misschien twee. (%s/5 stukjes)",
                    "Last night I had my eyes closed for a moment. One second. Maybe two. (%s/5 pieces)"),
    S + "thuis.2": ("Bubbels de Blub zegt dat je goed zoekt. Nog even, en dan... dan durf ik misschien te slapen. (%s/5)",
                    "Bubbels the Blub says you're searching well. A little longer, and then... then maybe I dare to sleep. (%s/5)"),
    S + "droom.0": ("Is dit mijn droom? Het is zo zacht hier. Je hebt al %s stukjes!",
                    "Is this my dream? It's so soft here. You already have %s pieces!"),
    S + "droom.1": ("Ik zie de eilanden, maar ik durf er niet heen. Jij wel! (%s/5)",
                    "I can see the islands, but I don't dare go there. You do! (%s/5)"),
    S + "droom.2": ("Als alle stukjes er zijn, misschien... misschien slaap ik dan wel. (%s/5)",
                    "When all the pieces are here, maybe... maybe then I'll sleep. (%s/5)"),
    S + "klaar": ("Snoes slaapt. Hij woont nu op het Droomplein, in zijn doos met knabbels, en hij zegt iedereen gedag die komt dromen. "
                  "En de wilde Blub-guhs laten zich nu ook aan jou zien!",
                  "Snoes is asleep. He lives on the Dream Square now, in his box of nibbles, and he greets everybody who comes to dream. "
                  "And the wild Blub-guhs show themselves to you now too!"),
    S + "blij.0": ("%s! Ik heb weer geslapen! In mijn doos! Njeg njeg njeg!", "%s! I slept again! In my box! Njeg njeg njeg!"),
    S + "blij.1": ("Hoi %s. Het kattenhuisje is heel mooi hoor. Ik zit er overdag in. 's Nachts: doos.",
                   "Hi %s. The kitty house is very pretty, you know. I sit in it in the daytime. At night: box."),
    S + "blij.2": ("%s, weet je wat ik nu heb? Een plekje. Mijn eigen plekje.", "%s, do you know what I have now? A place. My very own place."),
    S + "blij.3": ("Welkom op het Droomplein, %s! Wil je een knabbel? Ze liggen in mijn bed.",
                   "Welcome to the Dream Square, %s! Want a nibble? They're in my bed."),
    S + "blij.vreemde": ("Hoi! Ik ben Snoes. Ik woon hier, in die doos. Iemand heeft mij geholpen te slapen. Misschien help jij ook "
                         "iemand?",
                         "Hi! I'm Snoes. I live here, in that box. Somebody helped me to sleep. Maybe you'll help somebody too?"),
    # --- Bubbels, the dream guide ---------------------------------------------------------------------------------------------
    S + "gids.naam": ("Bubbels", "Bubbels"),
    S + "gids.welkom.0": ("Blub! Hoi! Ik ben Bubbels, je droomgids. Welkom in Guhdromenland!",
                          "Blub! Hi! I'm Bubbels, your dream guide. Welcome to Guh Dreamland!"),
    S + "gids.welkom.1": ("Snoes kan niet dromen. Zijn droom is in vijf stukjes over de eilanden gewaaid, blub.",
                          "Snoes can't dream. His dream blew over the islands in five pieces, blub."),
    S + "gids.welkom.2": ("Een liedje, een ster, een herinnering, een bad en droomzand. Vind ze, dan helpen we hem samen!",
                          "A song, a star, a memory, a bath and dream sand. Find them, and we'll help him together!"),
    S + "gids.welkom.3": ("Loop over een steiger naar een eiland, in welke volgorde je maar wilt. Ik huppel mee!",
                          "Walk over a jetty to an island, in any order you like. I'll hop along!"),
    S + "gids.terug": ("Daar ben je weer! Blub! We hebben %s van de vijf stukjes.", "There you are again! Blub! We have %s of the five pieces."),
    S + "gids.stukje.liedje": ("Het liedje! Nu kent Snoes eindelijk een slaapliedje. Blub-blub-bluuub! (%s/5)",
                               "The song! Now Snoes finally knows a lullaby. Blub-blub-bluuub! (%s/5)"),
    S + "gids.stukje.ster": ("De vallende ster! Hij wenst iets... een eigen plekje. (%s/5)",
                             "The falling star! He wishes for something... a place of his own. (%s/5)"),
    S + "gids.stukje.herinnering": ("Nu weet je het: hij zat in de bosjes, en keek. Hij hoorde nergens bij. Blub... (%s/5)",
                                    "Now you know: he sat in the bushes, and watched. He belonged nowhere. Blub... (%s/5)"),
    S + "gids.stukje.bad": ("Zijn eerste bubbelbad ooit! Schoon en warm. Daar slaap je lekker van. (%s/5)",
                            "His first bubble bath ever! Clean and warm. That makes you sleep well. (%s/5)"),
    S + "gids.stukje.droomzand": ("Droomzand, vers gemalen! Een snufje, en je ogen worden zwaar... (%s/5)",
                                  "Dream sand, freshly ground! A pinch, and your eyes grow heavy... (%s/5)"),
    S + "gids.alles": ("Alle vijf! Blub blub blub! Kom snel naar het Droomplein, iedereen komt kijken!",
                       "All five! Blub blub blub! Come to the Dream Square quickly, everybody's coming to watch!"),
    S + "gids.mee": ("Mag ik... met je mee naar huis? Ik wil jouw Blub zijn. Blub?", "May I... come home with you? I want to be your Blub. Blub?"),
    # playtest: the story HUD (top left)
    S + "hud.titel": ("Snoes' droom", "Snoes' dream"),
    S + "hud.bij_bed": ("Praat met Snoes bij je bed.", "Talk to Snoes by your bed."),
    S + "hud.ingestopt": ("Ga in je bed liggen naast Snoes en kies 'Samen dromen'.", "Lie down in your bed next to Snoes and choose 'Dream together'."),
    S + "hud.stukjes": ("Stukjes %s/%s:", "Pieces %s/%s:"),
    S + "hud.zoek": ("Zoek de stukjes op de eilanden (Droompost brengt je snel terug).", "Find the pieces on the islands (Dream Post takes you back quickly)."),
    S + "hud.terug": ("Ga slapen naast Snoes om verder te dromen.", "Sleep next to Snoes to dream on."),
    S + "hud.finale": ("Alles gevonden! Ga naar het Droomplein.", "All found! Go to the Dream Square."),
    S + "gids.thuis_mee": ("Bubbels zat nog bij je (op je hoofd of in je zakken) en gaat gewoon mee naar huis. Blub!",
                           "Bubbels was still with you (on your head or in your pockets) and simply comes home with you. Blub!"),
    S + "gids.thuis": ("Blub! Hier woon jij dus. Mooi! Ik blijf, hoor. Voor altijd.", "Blub! So this is where you live. Nice! I'm staying. Forever."),
    S + "status": ("Snoes' droom: %s/5. Nog: %s", "Snoes' dream: %s/5. Still: %s"),
    S + "status.alles": ("Snoes' droom is compleet! Naar het Droomplein!", "Snoes' dream is complete! To the Dream Square!"),
    # --- hints of the island NPCs ---------------------------------------------------------------------------------------------
    S + "hint.zandguh.nog": ("De molen staat stil, vadsje. Zonder wind geen droomzand. Misschien kunnen de Blubs blazen? Zij zijn goed in "
                             "bellen.",
                             "The mill stands still, little vads. No wind, no dream sand. Maybe the Blubs can blow? They're good at bubbles."),
    S + "hint.zandguh.klaar": ("Snoes' droomzand zit in een zakje met een strikje. Het fijnste zand dat ik ooit maalde.",
                               "Snoes' dream sand is in a little bag with a bow. The finest sand I ever ground."),
    S + "hint.droom_omaguh.nog": ("Raak de herinneringsbelletjes eens aan, lieverd. De familiedag... ik weet nog dat de bosjes ritselden.",
                                  "Touch the memory bubbles, dear. The family day... I remember the bushes rustling."),
    S + "hint.droom_omaguh.klaar": ("Ik had hem wel gezien, toen, in de bosjes. Ik had een koekje voor hem moeten brengen. Nu doe ik dat elke dag.",
                                    "I did see him, back then, in the bushes. I should have brought him a cookie. Now I do it every day."),
    S + "hint.rommelguh.nog": ("Herinneringen? Ik heb er zat! Ergens tussen de glazen. Kijk maar rond op het Herinneringseiland.",
                               "Memories? I've got plenty! Somewhere between the glasses. Have a look around Memory Island."),
    S + "hint.rommelguh.klaar": ("Ik heb voor Snoes een glas bewaard. Nog niet leeg. Zeker weten.",
                                 "I saved a glass for Snoes. Not empty yet. Definitely."),
    S + "hint.kussengevecht_guh.nog": ("Hoger! Stuiter hoger! De ster valt pas als je bovenaan bent. KUSSEN!",
                                       "Higher! Bounce higher! The star only falls when you're at the top. PILLOW!"),
    S + "hint.kussengevecht_guh.klaar": ("Je hebt de ster! Dat vieren we met... KUSSEN! Njeg!", "You have the star! We celebrate that with... PILLOW! Njeg!"),
    S + "hint.wekkerguh": ("Nog %s stukjes, dan mag Snoes uitslapen. Tot dan: ik rinkel alleen als je erom vraagt.",
                           "%s more pieces, then Snoes may sleep in. Until then: I only ring when you ask."),
    # --- the finale scene -----------------------------------------------------------------------------------------------------
    F + "titel": ("Snoes' eigen familiedag", "Snoes' own family day"),
    F + "gids": ("Kijk! Daar is iedereen! Blub!", "Look! There's everybody! Blub!"),
    F + "verteller1": ("Van alle eilanden komen ze naar het Droomplein.", "From every island they come to the Dream Square."),
    F + "oma": ("Daar is hij! Ik zag je wel, toen, in de bosjes. Kom maar, lieverd. Er zijn roze guh-koeken.",
                "There he is! I did see you, back then, in the bushes. Come here, dear. There are pink guh cakes."),
    F + "rommel": ("Ik heb glazen meegenomen! Ze zijn nog niet leeg.", "I brought glasses! They're not empty yet."),
    F + "snoes1": ("Een feestje... voor mij?", "A party... for me?"),
    F + "verteller2": ("Je hebt een prachtig kattenhuisje voor Snoes gebouwd.", "You built a beautiful kitty house for Snoes."),
    F + "snoes2": ("Het is heel mooi... njeg...", "It's very pretty... njeg..."),
    F + "verteller3": ("...maar Snoes kruipt toch in de doos met knabbels.", "...but Snoes crawls into the box of nibbles anyway."),
    F + "lachen": ("Hahaha! Natuurlijk de doos! Een guh kiest altijd de doos! Njeg!", "Hahaha! Of course the box! A guh always picks the box! Njeg!"),
    F + "zand": ("Een snufje droomzand... welterusten, kleine Snoes.", "A pinch of dream sand... sleep tight, little Snoes."),
    F + "liedje": ("~ blub... blub... bluuub... ~", "~ blub... blub... bluuub... ~"),
    F + "snoes3": ("Welterusten... iedereen.", "Good night... everybody."),
    F + "einde": ("Snoes slaapt. Voor het allereerst.", "Snoes is asleep. For the very first time."),
    F + "naam.gids": ("Bubbels", "Bubbels"),
    F + "naam.snoes": ("Snoes", "Snoes"),
    F + "naam.oma": ("Oma-guh", "Grandma Guh"),
    F + "naam.rommel": ("Rommelguh", "Clutter Guh"),
    F + "naam.zand": ("Zandguh", "Sand Guh"),
    F + "naam.kussen1": ("Kussengevecht-guh", "Pillow Fight Guh"),
}
for _id, (_nl, _en) in STUKJES.items():
    TEKSTEN[S + "stukje." + _id] = (_nl, _en)

IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def build(h):
    for a in ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{a}.json", {"criteria": IMPOSSIBLE})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck()


def selfcheck():
    fout = [k for k, (nl, en) in TEKSTEN.items() if nl.count("%s") != en.count("%s")]
    if fout:
        raise SystemExit(f"snoesverhaal self-check failed (placeholders): {fout}")


# =====================================================================================================================
# the FTB chapter guhs_dromen (tools/make_ftbquests.py CHAPTERS["guhs_dromen"])
# =====================================================================================================================
FTB_CHAPTER = "guhs_dromen"
FTB_PORTRAIT = "item:guhs:droomkussen"
FTB_SECTIES = [
    ("dr_blub", "De Blub-guh", "item:guhs:blubguh_item", ["dr_blub_vriend", "dr_blub_badje", "dr_blub_glas",
                                                          "dr_blub_meterkaart_rij", "dr_blub_meterkaart_goud"]),
    ("dr_weer", "De Weer vadser", "item:guhs:weer_vadser", ["dr_weer_vadser", "dr_weer_bellen", "dr_weer_regenboog"]),
    ("dr_slaap", "Slaap lekker, guh", "item:guhs:muziekdoosje",
     ["dr_doos", "dr_kattenhuisje", "dr_muziekdoosje", "dr_mobiel", "dr_maanlampje", "dr_krabpaal"]),
    ("dr_droom", "Guhdromenland", "item:guhs:droomkussen", ["dr_droomland", "dr_droomzand", "dr_zeepbelglas", "dr_slaapliedjesgras"]),
    ("dr_snoes", "De droom van Snoes", "item:guhs:roze_guh_koek",
     ["dr_snoes_proloog"] + [f"dr_snoes_{s}" for s in STUKJES] + ["dr_snoes_finale"]),
    ("dr_weide", "De Zingende Weide", "item:guhs:blubguh_item", ["dr_weide"]),
]

SNOES_QUESTS = {  # piece -> (title, description, icon)
    "liedje": ("Het liedje", "Op de &dSlaapliedjesheuvels&r zijn wilde Blubs verlegen. Breng ze naar hun klokjesheuvel, dan piepen ze "
               "samen het slaapliedje van Snoes.", "guhs:slaapliedjesgras"),
    "ster": ("De vallende ster", "Stuiter over de reuzenkussens van het &eSterrenkussenveld&r, steeds hoger, en vang de vallende ster. "
             "Pas op voor de kussengevecht-guhs!", "guhs:sterrenlantaarn"),
    "herinnering": ("De herinnering", "Raak op het &6Herinneringseiland&r de herinneringsbelletjes aan. Wat gebeurde er op de "
                    "Guh-familiedag? En wie zat er in de bosjes?", "guhs:roze_guh_koek"),
    "bad": ("Het bad", "Zweef in zeepbellen langs de wolken van het &bWolkenbadje&r en haal de badspulletjes. Snoes' eerste "
            "bubbelbad ooit!", "guhs:knuffelbad_badeendje"),
    "droomzand": ("Het droomzand", "De molen van het &7Maanmolentje&r staat stil. Blaas met de Blubs de wieken aan en zet de "
                  "tandwieltjes goed, dan maalt de Zandguh droomzand.", "guhs:droomzand"),
}


def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    # --- the Blub-guh -----------------------------------------------------------------------------------------------------
    q("dr_blub_vriend", "Een Blub als vriendje", "Na het verhaal van Snoes zie je overal wilde &bBlub-guhs&r (vooral in de Zingende "
      "Weide). Tem er een met een kaasknabbel en pak hem op: sluip + klik. Elke Blub heeft zijn eigen Blub-Guh-Meter!",
      "guhs:blubguh_item", [item("guhs:blubguh_item")], xp=100)
    q("dr_blub_badje", "Bubbelbadje", "Maak een &dBlub-bubbelbadje&r. Een Blub gaat er zelf in badderen, en met het klusje "
      "Bubbelbadmeester worden je guhs fris en blij.", "guhs:blub_badje", [item("guhs:blub_badje")], xp=50)
    q("dr_blub_glas", "Nog niet leeg!", "Rommelguh laat overal glazen met een restje staan. Maak er zelf ook een paar: drie "
      "glasruiten in een V.", "guhs:glas_restje", [item("guhs:glas_restje")], xp=50)
    # (1.8.0 dexdromen: the Blub-meterkaart in the Guhdex, on the Blub-guh page)
    q("dr_blub_meterkaart_rij", "Van Blub tot Guh", "Vul op je &dBlub-meterkaart&r (Guhdex, pagina Blub-guh) een hele rij van één "
      "soort: een Blub met meter 0-9, 10-19... tot en met 90-100. Temmen en kweken telt allebei.", "guhs:blubguh_item",
      [adv("blub_meterkaart_rij")], xp=200)
    q("dr_blub_meterkaart_goud", "Goud!", "Een &6gouden vakje&r op je meterkaart: tem of kweek een &dPure Blub&r (meter precies 0) of "
      "een &dVolle Guh&r (meter precies 100). Heel zeldzaam!", "guhs:blubguh_item", [adv("blub_meterkaart_goud")], xp=300)
    # --- the Weer vadser --------------------------------------------------------------------------------------------------
    q("dr_weer_vadser", "Weer vadsen", "Maak een &dWeer vadser&r en zet het weer in een straal van 60 blokken: zon, regen, sneeuw "
      "of onweer. Hij wil 20 vadskracht zolang er een ander weertje aan staat. Ook in de Guhmensie regent het dan!",
      "guhs:weer_vadser", [item("guhs:weer_vadser")], xp=100)
    q("dr_weer_bellen", "Bellenbui", "Vind een &bweerpotje: bellenbui&r (in de kisten van de Guhbubbel en Bikinibodem, of in het "
      "Wolkenbadje van Guhdromenland). Maak hem op en zet hem aan in je Weer vadser.", "guhs:weerpotje_bellen",
      [item("guhs:weerpotje_bellen")], xp=100)
    q("dr_weer_regenboog", "Regenboogweer", "Het &dregenboogweer&r: open het souvenir 'Regenboogje in een potje', haal goud op het "
      "Guh-Circuit, of help Snoes slapen.", "guhs:weerpotje_regenboog", [item("guhs:weerpotje_regenboog")], xp=150)
    # --- sleeping well ----------------------------------------------------------------------------------------------------
    q("dr_doos", "Een guh kiest altijd de doos", "Maak een &6doos met knabbels&r. 's Avonds stop je er je guh in (rechtsklik), en 's "
      "nachts kruipen guhs er zelf in.", "guhs:doos_knabbels", [item("guhs:doos_knabbels")], xp=50)
    q("dr_kattenhuisje", "Kattenhuisje", "Een &dkattenhuisje&r in roze, wit, lavendel of mint. Heel mooi. (De doos wint toch.)",
      "guhs:kattenhuisje_roze", [item("guhs:kattenhuisje_roze")], xp=50)
    q("dr_muziekdoosje", "Slaapliedje", "Met een &dmuziekdoosje&r zing je guhs in de buurt zachtjes in slaap.", "guhs:muziekdoosje",
      [item("guhs:muziekdoosje")], xp=50)
    q("dr_mobiel", "Draai, mobiel, draai", "Hang een &dmobiel&r boven het guhbedje: hij draait en speelt een liedje.", "guhs:mobiel",
      [item("guhs:mobiel")], xp=50)
    q("dr_maanlampje", "Nachtlampje", "Een &dmaannachtlampje&r voor de guhkamer: klik aan, klik uit.", "guhs:maanlampje_maan",
      [item("guhs:maanlampje_maan")], xp=50)
    q("dr_krabpaal", "Krabpaal", "Tamme guhs hupsen overdag op een &dkrabpaal&r en blijven daar trots zitten.", "guhs:krabpaal",
      [item("guhs:krabpaal")], xp=50)
    # --- Guhdromenland ----------------------------------------------------------------------------------------------------
    q("dr_droomland", "Guhdromenland", "Stop 's avonds je guh in (doos, kattenhuisje, bedje of nestje) en ga in een bed ernaast "
      "slapen. Wat je hem meegeeft bepaalt op welk eiland je begint. Wakker worden: je &ddroomkussen&r of de Wekkerguh.",
      "guhs:droomkussen", [fq.dim("guhs:guhdromenland")], shape="hexagon", xp=150)
    q("dr_droomzand", "Droomzand", "In Guhdromenland vind je &ddroomzand&r. Daar maak je droomblokken van.", "guhs:droomzand",
      [item("guhs:droomzand", 8)], xp=50)
    q("dr_zeepbelglas", "Zeepbelglas", "Glas met verschuivende regenboogkleuren: 8 glas en 1 droomzand.", "guhs:zeepbelglas",
      [item("guhs:zeepbelglas", 8)], xp=50)
    q("dr_slaapliedjesgras", "Neuriënd gras", "&dSlaapliedjesgras&r neuriet zachtjes als je eroverheen loopt.", "guhs:slaapliedjesgras",
      [item("guhs:slaapliedjesgras", 4)], xp=50)
    # --- Snoes' dream -----------------------------------------------------------------------------------------------------
    q("dr_snoes_proloog", "Wie zit er bij je bed?", "Heb je een tamme guh? Klik dan 's avonds op je bed. Er zit iemand naast... "
      "Praat met hem, stop hem in, en ga zelf slapen.", "guhs:doos_knabbels", [adv("snoes_proloog")], shape="circle", xp=100)
    for s, (titel, tekst, icoon) in SNOES_QUESTS.items():
        q(f"dr_snoes_{s}", titel, tekst, icoon, [adv(f"snoes_{s}")], xp=150)
    q("dr_snoes_finale", "Welterusten, Snoes", "Met alle vijf stukjes: terug naar het &dDroomplein&r. Iedereen komt kijken. Daarna "
      "laten de wilde Blub-guhs zich aan je zien, en komt Bubbels mee naar huis.", "guhs:roze_guh_koek", [adv("snoes_finale")],
      rewards=(("guhs:kaas_knabbels", 32),), shape="hexagon", xp=500)
    # --- De Zingende Weide ------------------------------------------------------------------------------------------------
    q("dr_weide", "De Zingende Weide", "Ergens in de Guhmensie ligt een zeldzame zalmroze weide met hoge rotspilaren, wolkjesbomen "
      "en poeltjes bubbelsap. Hier wonen de wilde Blub-guhs. Bij schemer zingen ze samen in de Koorkom.", "minecraft:filled_map",
      [fq.biome("zingende_weide")], shape="circle", xp=150)


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c213_snoesverhaal.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"snoesverhaal: {len(TEKSTEN)} texts, {len(ADVANCEMENTS)} advancements")


if __name__ == "__main__":
    _direct()
