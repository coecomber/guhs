"""
1.10 round U, slice U-HUIZEN: player housing, Het Knuffelkwartier (Java: feature/vadsrijk/huizen).

  - the dimension guhs:knuffelkwartier (flat pink grass, top at y 63, no mobs, day/night like the overworld) + its biome;
  - every text of the housing (Kwartiermeester, makelaar, brievenbus, prikbord, Mollie, signs, commands), Dutch + real English.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_huizen.py
"""
import json
import os

R = os.path.join("src", "main", "resources")
D = os.path.join(R, "data", "guhs")
A = os.path.join(R, "assets", "guhs")
H = "vadsrijk.huizen."
N = "vadsrijk.npc."

DIMENSIE = {
    "type": "guhs:knuffelkwartier",
    "generator": {
        "type": "minecraft:flat",
        "settings": {
            "biome": "guhs:knuffelkwartier",
            "layers": [
                {"block": "minecraft:bedrock", "height": 1},
                {"block": "minecraft:stone", "height": 59},
                {"block": "minecraft:dirt", "height": 3},
                {"block": "minecraft:grass_block", "height": 1},
            ],
            "lakes": False,
            "features": False,
            "structure_overrides": [],
        },
    },
}

DIMENSIE_TYPE = {
    "attributes": {
        "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "always"},
        "minecraft:gameplay/can_start_raid": False,
        "minecraft:gameplay/respawn_anchor_works": False,
        "minecraft:visual/ambient_light_color": "#2a1a22",
        "minecraft:visual/cloud_color": "#ccffe4f1",
        "minecraft:visual/cloud_height": 140.33,
        "minecraft:visual/fog_color": "#ffe4f1",
        "minecraft:visual/sky_color": "#f7b6d8",
    },
    "has_skylight": True,
    "has_ceiling": False,
    "has_ender_dragon_fight": False,
    "coordinate_scale": 1.0,
    "ambient_light": 0.1,
    "min_y": 0,
    "height": 256,
    "logical_height": 256,
    "infiniburn": "#minecraft:infiniburn_overworld",
    "monster_spawn_light_level": 0,
    "monster_spawn_block_light_limit": 0,
    "has_fixed_time": False,
    "default_clock": "minecraft:overworld",
    "timelines": "#minecraft:in_overworld",
}

BIOOM = {
    "attributes": {
        "minecraft:audio/background_music": {
            "default": {"sound": "minecraft:music.overworld.cherry_grove", "min_delay": 12000, "max_delay": 24000},
            "creative": {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000},
        },
        "minecraft:visual/fog_color": "#ffe4f1",
        "minecraft:visual/sky_color": "#f7b6d8",
        "minecraft:visual/water_fog_color": "#d76aa6",
    },
    "has_precipitation": False,
    "temperature": 0.7,
    "downfall": 0.4,
    "effects": {"water_color": "#9ad8f7", "foliage_color": "#f9b8d6", "grass_color": "#f5b3d2"},
    "spawners": {},
    "spawn_costs": {},
    "carvers": [],
    "features": [],
}

# key -> (Dutch, English)
TEKSTEN = {
    "biome.guhs.knuffelkwartier": ("Het Knuffelkwartier", "The Snuggle Quarter"),   # W-INT: the dimension's biome name
    # NPCs
    N + "naam.kk_poort": ("Kwartiermeester", "Quartermaster"),
    # X-LABELS: the Kwartiermeester of every hub town in Poorten.HUBS has its own name (like zone 6's Rotsdak and Koepeltje);
    # without it the name tag showed "vadsrijk.npc.naam.kk_poort_zwamzoet"
    N + "naam.kk_poort_zwamzoet": ("Kwartiermeester Lamelletje", "Quartermaster Gilly"),
    N + "naam.kk_poort_kandijkom": ("Kwartiermeester Klontje", "Quartermaster Cubey"),
    N + "naam.kk_poort_honingtak": ("Kwartiermeester Wasje", "Quartermaster Waxy"),
    N + "naam.kk_poort_lampionhaven": ("Kwartiermeester Lontje", "Quartermaster Wicky"),
    N + "naam.kk_poort_hoogvads": ("Kwartiermeester Torentje", "Quartermaster Turret"),
    N + "plek.npc.kk_poort_zwamzoet": ("op het plein van Zwamzoet, een paar stappen van het midden",
                                       "on the square of Shroomsbury, a few steps from the middle"),
    N + "plek.npc.kk_poort_kandijkom": ("op het plein van Kandijkom, een paar stappen van het midden",
                                       "on the square of Sugarbowl, a few steps from the middle"),
    N + "plek.npc.kk_poort_honingtak": ("op het plein van Honingtak, een paar stappen van het midden",
                                       "on the square of Honeybough, a few steps from the middle"),
    N + "plek.npc.kk_poort_lampionhaven": ("op het havenplein van Lampionhaven, een paar stappen van het midden",
                                          "on the harbor square of Lanternhaven, a few steps from the middle"),
    N + "plek.npc.kk_poort_hoogvads": ("bij de fontein op de markt van Hoogvads",
                                      "by the fountain on the market of Highchonk"),
    N + "naam.kk_mollie": ("Meubelmeester Mollie", "Furniture Maker Mollie"),
    N + "praat.kk_mollie.1": ("Een huis is pas een thuis met een zitzak erin!", "A house is only a home with a beanbag in it!"),
    N + "praat.kk_mollie.2": ("Alles handgemaakt, met heel veel knuffels.", "All handmade, with lots of cuddles."),
    N + "praat.kk_mollie.3": ("Roze past overal bij. Echt overal.", "Pink goes with everything. Really everything."),
    H + "naam.makelaar": ("Makelaar Muis", "Estate Agent Mouse"),
    H + "naam.brievenbus": ("Brievenbus", "Mailbox"),
    H + "naam.prikbord": ("Prikbord", "Notice board"),
    # the three sizes and looks
    H + "soort.huisje": ("Huisje", "Cottage"),
    H + "soort.huis": ("Huis", "House"),
    H + "soort.villa": ("Villa", "Villa"),
    H + "variant.snoepje": ("Snoepje (kersenroze)", "Candy (cherry pink)"),
    H + "variant.muntje": ("Muntje (wit met mint)", "Mint (white and mint)"),
    H + "variant.lavendel": ("Lavendel (met veranda en schoorsteen)", "Lavender (porch and chimney)"),
    # Kwartiermeester
    H + "poort.hallo": ("Hoi! Ik ben de Kwartiermeester. Achter mijn poortje ligt Het Knuffelkwartier: een buurtje met straatjes, "
                        "een pleintje, een marktje en heel veel kavels. Daar kun je een eigen huisje kopen en helemaal zelf inrichten!",
                        "Hi! I'm the Quartermaster. Behind my little gate lies the Cuddle Quarter: a neighbourhood with streets, a square, a "
                        "little market and lots of plots. You can buy a house of your own there and furnish it all by yourself!"),
    H + "poort.hallo_eigen": ("Welkom terug, buurtgenoot! Naar huis, of even bij iemand op bezoek?",
                              "Welcome back, neighbour! Home, or a little visit to someone?"),
    H + "optie.naar_huis": ("Naar mijn huis", "To my house"),
    H + "optie.naar_wijk": ("Naar mijn wijk (het plein)", "To my ward (the square)"),
    H + "optie.naar_wijk_kies": ("Naar de buurt: een kavel kiezen", "To the neighbourhood: choose a plot"),
    H + "optie.bezoek": ("Bij iemand op bezoek", "Visit someone"),
    H + "optie.uitleg": ("Hoe werkt het?", "How does it work?"),
    H + "optie.dag": ("Doei!", "Bye!"),
    H + "optie.nee": ("Nee, toch niet", "No, never mind"),
    H + "optie.meer": ("Meer spulletjes »", "More things »"),
    H + "uitleg.1": ("Een wijk heeft 24 kavels aan vier straatjes. Loop rond, kies een vrije kavel en klik op het bordje 'Te koop!'. "
                     "Is de wijk vol? Dan bouwen we gewoon een nieuwe.",
                     "A ward has 24 plots along four streets. Walk around, pick a free plot and click its 'For sale!' sign. Is the ward "
                     "full? Then we simply build a new one."),
    H + "uitleg.2": ("Je hebt één huis per account: al je karakters wonen samen. Er zijn drie maten (huisje, huis, villa), elk in "
                     "drie kleurtjes. Binnen en in je tuin mag je alles inrichten; in de rest van het Vadsrijk blijft bouwen verboden.",
                     "You get one house per account: all your characters live together. There are three sizes (cottage, house, villa), "
                     "each in three colours. Inside and in your garden you may furnish everything; the rest of the Vadsrijk stays "
                     "unbuildable."),
    H + "uitleg.3": ("Bij de brievenbus regel je alles: post ophalen, de deur op slot, vrienden toevoegen. Je rijdier staat in het "
                     "stalletje, je huisdieren spelen in de tuin, en wie thuis uitlogt, komt uitgerust terug (dubbele xp)!",
                     "At the mailbox you arrange everything: collect your post, lock the door, add friends. Your mount waits in the "
                     "little stable, your pets play in the garden, and if you log out at home you come back rested (double xp)!"),
    H + "bezoek.wie": ("Bij wie wil je op bezoek? (je vrienden en wie er nu online is)", "Who do you want to visit? (your friends and who is online now)"),
    H + "bezoek.niemand": ("Er is nu niemand met een huis online, en je bent nog nergens vriend.", "Nobody with a house is online now, and you aren't anyone's friend yet."),
    # gate + signs
    H + "poort.bord1": ("Terug naar", "Back to"),
    H + "poort.bord2": ("de stad", "the town"),
    H + "prikbord.bord1": ("Prikbord", "Notice"),
    H + "prikbord.bord2": ("van de wijk", "board"),
    H + "bord.te_koop": ("Te koop!", "For sale!"),
    H + "bord.kavel": ("Kavel %s", "Plot %s"),
    H + "bord.wijk": ("Wijk %s", "Ward %s"),
    H + "bord.klik": ("(klik mij)", "(click me)"),
    H + "straat.east": ("Oostpluislaan", "East Fluff Lane"),
    H + "straat.south": ("Zuidknuffelstraat", "South Cuddle Street"),
    H + "straat.west": ("Westsnoepsteeg", "West Candy Alley"),
    H + "straat.north": ("Noordvadspad", "North Vads Path"),
    # buying
    H + "koop.hallo": ("Kavel %s in wijk %s is te koop! Kies maar een huis:\n%s\nJij hebt %s en bent level %s.",
                       "Plot %s in ward %s is for sale! Pick a house:\n%s\nYou have %s and are level %s."),
    H + "koop.regel": ("%s: %s (vanaf level %s, %s decoblokjes)", "%s: %s (from level %s, %s deco blocks)"),
    H + "koop.variant": ("Een %s, prima keus! Welk kleurtje?", "A %s, great choice! Which colours?"),
    H + "koop.al_een": ("Je hebt al een huis. Eén huis per account!", "You already have a house. One house per account!"),
    H + "koop.al_een_uitleg": ("Jij hebt al een huis in het Knuffelkwartier (één per account). Wil je verhuizen? Verkoop eerst je huis bij je eigen brievenbus.",
                               "You already have a house in the Cuddle Quarter (one per account). Want to move? Sell your house at your own mailbox first."),
    H + "koop.bezet": ("Deze kavel is net verkocht.", "This plot was just sold."),
    H + "koop.level": ("Je moet level %s zijn voor een %s.", "You need to be level %s for a %s."),
    H + "koop.munten": ("Je hebt niet genoeg Vadsmunten (%s nodig).", "You don't have enough Vadscoins (%s needed)."),
    H + "koop.gelukt": ("Gefeliciteerd! Je %s op kavel %s in wijk %s is van jou!", "Congratulations! Your %s on plot %s in ward %s is yours!"),
    H + "groter.gelukt": ("Verbouwd! Je woont nu in een %s.", "Rebuilt! You now live in a %s."),
    H + "deco_terug": ("%s decoblokjes liggen voor je klaar in de brievenbus.", "%s deco blocks are waiting for you in the mailbox."),
    H + "verkocht": ("Je huis is verkocht. Je krijgt %s terug; %s decoblokjes wachten op je bij de brievenbus van je volgende huis.",
                     "Your house is sold. You get %s back; %s deco blocks wait for you at the mailbox of your next house."),
    # arriving, walking around
    H + "welkom": ("Het Knuffelkwartier, wijk %s", "The Cuddle Quarter, ward %s"),
    H + "thuis": ("Thuis! Lekker, %s.", "Home sweet home, %s."),
    H + "bij": ("Op bezoek bij %s", "Visiting %s"),
    H + "kavel.vrij": ("Kavel %s: te koop", "Plot %s: for sale"),
    H + "kavel.eigen": ("Je eigen %s", "Your own %s"),
    H + "kavel.van": ("Het huis van %s (%s)", "%s's home (%s)"),
    H + "rand": ("Hier houdt de wijk op. Terug naar het plein!", "The ward ends here. Back to the square!"),
    H + "geen_dimensie": ("Het Knuffelkwartier is er (nog) niet op deze server.", "The Cuddle Quarter doesn't exist (yet) on this server."),
    # rules
    H + "nee.even": ("Even wachten, je spullen worden nog uitgepakt...", "One moment, your things are still being unpacked..."),
    H + "nee.niet_van_jou": ("Dit is niet jouw kavel: hier mag je niets veranderen.", "This isn't your plot: you can't change anything here."),
    H + "nee.vriend_mag_niet": ("De eigenaar heeft inrichten door vrienden uitgezet.", "The owner has turned off decorating by friends."),
    H + "nee.buiten_kavel": ("Alleen binnen je eigen hekje (en niet te hoog).", "Only inside your own fence (and not too high)."),
    H + "nee.blok": ("Dit blok mag niet in een huis (kisten, zand, tnt en zuigers blijven weg).", "This block can't go in a house (no chests, sand, tnt or pistons)."),
    H + "nee.vol": ("Je huis zit vol: maximaal %s decoblokjes. Een groter huis heeft meer ruimte!", "Your house is full: at most %s deco blocks. A bigger house has more room!"),
    H + "nee.huis_zelf": ("Het huis zelf blijft staan: alleen wat je zelf neerzette kan weg.", "The house itself stays: only what you placed can be removed."),
    H + "deur.dicht": ("De deur van %s zit dicht.", "%s's door is closed."),
    H + "deur.nu": ("Deur: %s", "Door: %s"),
    H + "deur.open": ("open voor iedereen", "open to everyone"),
    H + "deur.vrienden": ("alleen vrienden", "friends only"),
    H + "deur.slot": ("op slot (alleen jij)", "locked (only you)"),
    H + "ja": ("ja", "yes"),
    H + "nee": ("nee", "no"),
    H + "rol.vriend": ("Jij bent hier vriend.", "You are a friend here."),
    H + "rol.bezoeker": ("Jij bent hier bezoeker: kijken mag, aankomen niet.", "You are a visitor here: look, don't touch."),
    # mailbox
    H + "post.eigen": ("Je brievenbus. Post: %s. Deur: %s. Vrienden: %s. Deco: %s van %s. Rust-xp: %s.\nGroter huis: %s",
                       "Your mailbox. Post: %s. Door: %s. Friends: %s. Deco: %s of %s. Rested xp: %s.\nBigger house: %s"),
    H + "post.groter": ("een %s voor %s extra (level %s)", "a %s for %s extra (level %s)"),
    H + "post.van": ("Dit is het huis van %s (%s). Deur: %s. %s", "This is %s's home (%s). Door: %s. %s"),
    H + "post.gehaald": ("Je haalt %s dingen uit de brievenbus.", "You take %s things out of the mailbox."),
    H + "post.leeg": ("De brievenbus is leeg.", "The mailbox is empty."),
    H + "post.uiterlijk": ("Welk kleurtje wil je? Je decoblokjes gaan even naar de brievenbus.", "Which colours would you like? Your deco blocks go to the mailbox for a moment."),
    H + "post.verkoop_zeker": ("Weet je het zeker? Je krijgt %s terug. Je decoblokjes blijven voor je bewaard.", "Are you sure? You get %s back. Your deco blocks are kept for you."),
    H + "optie.post": ("Post ophalen", "Collect post"),
    H + "optie.deur": ("Deur: volgende stand", "Door: next setting"),
    H + "optie.vrienden": ("Vrienden", "Friends"),
    H + "optie.groter": ("Groter huis", "Bigger house"),
    H + "optie.uiterlijk": ("Ander kleurtje", "Other colours"),
    H + "optie.verkoop": ("Huis verkopen", "Sell house"),
    H + "optie.verkoop_ja": ("Ja, verkopen", "Yes, sell"),
    H + "optie.naar_stad": ("Terug naar de stad", "Back to town"),
    H + "optie.vriend_hier": ("Spelers naast mij worden vriend", "Players next to me become friends"),
    H + "optie.inrichten_aan": ("Vrienden mogen inrichten: aanzetten", "Friends may decorate: turn on"),
    H + "optie.inrichten_uit": ("Vrienden mogen inrichten: uitzetten", "Friends may decorate: turn off"),
    H + "optie.vrienden_weg": ("Alle vrienden weghalen", "Remove all friends"),
    H + "vrienden.lijst": ("Vrienden: %s\nMogen inrichten: %s\nTip: /guhs vadsrijk huis vriend <naam>", "Friends: %s\nMay decorate: %s\nTip: /guhs vadsrijk huis vriend <name>"),
    H + "vrienden.erbij": ("%s nieuwe vrienden", "%s new friends"),
    H + "vrienden.jij": ("Je bent nu vriend van het huis van %s!", "You are now a friend of %s's home!"),
    H + "vrienden.toegevoegd": ("%s is nu je huisvriend.", "%s is now your house friend."),
    H + "vrienden.al": ("%s was al je huisvriend.", "%s was already your house friend."),
    H + "vrienden.weg": ("%s is geen huisvriend meer.", "%s is no longer your house friend."),
    H + "vrienden.geen": ("%s was geen huisvriend.", "%s wasn't your house friend."),
    # notice board
    H + "prikbord.kop": ("Wijk %s: nog %s kavels te koop. Er zijn %s wijken.", "Ward %s: %s plots still for sale. There are %s wards."),
    H + "prikbord.bewoners": ("Wie woont hier?\n%s", "Who lives here?\n%s"),
    H + "prikbord.regel": ("Kavel %s: %s (%s)", "Plot %s: %s (%s)"),
    H + "prikbord.leeg": ("Nog niemand! Wees de eerste.", "Nobody yet! Be the first."),
    H + "prikbord.tip": ("Tip: wie in zijn eigen huis uitlogt, komt uitgerust terug. Meubeltjes koop je bij Mollie op de markt.",
                         "Tip: log out in your own house and you come back rested. Furniture is sold by Mollie at the market."),
    # Mollie
    H + "mollie.hallo": ("Kijk eens rond! Alles gaat meteen in je tas.\n%s\nJij hebt %s.", "Have a look! Everything goes straight into your bag.\n%s\nYou have %s."),
    H + "mollie.regel": ("%sx %s: %s", "%sx %s: %s"),
    # rested xp
    H + "rust.erbij": ("Lekker geslapen thuis! +%s rust-xp (nu %s). Je krijgt dubbele xp tot het op is.", "Slept well at home! +%s rested xp (now %s). You get double xp until it runs out."),
    H + "rust.op": ("Je rust-xp is op.", "Your rested xp is used up."),
    # commands
    H + "cmd.reset": ("Huis weggehaald (decoblokjes in de brievenbus).", "House removed (deco blocks in the mailbox)."),
    H + "cmd.geen_huis": ("Geen huis gevonden.", "No house found."),
    H + "cmd.info": ("Wijken: %s, huizen: %s. Jouw kavel (wijk/kavel): %s, %s, deco %s, vrienden %s, deur %s, rust-xp %s, post %s",
                     "Wards: %s, houses: %s. Your plot (ward/plot): %s, %s, deco %s, friends %s, door %s, rested xp %s, post %s"),
    H + "cmd.figuren": ("Alle figuren weg (ze komen vanzelf terug).", "All figures removed (they come back by themselves)."),
    H + "cmd.poorten": ("Kwartiermeesters worden opnieuw geplaatst bij: %s", "Quartermasters are placed again at: %s"),
}


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def _data():
    _schrijf(os.path.join(D, "dimension", "knuffelkwartier.json"), DIMENSIE)
    _schrijf(os.path.join(D, "dimension_type", "knuffelkwartier.json"), DIMENSIE_TYPE)
    _schrijf(os.path.join(D, "worldgen", "biome", "knuffelkwartier.json"), BIOOM)


def build(h):
    _data()
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)   # (make_v2.lang(key, en, nl))


def _direct():
    _data()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: (v[0] if taal == "nl_nl" else v[1]) for k, v in TEKSTEN.items()})
        _schrijf(p, d)
    print(f"vadsrijk_u_huizen: {len(TEKSTEN)} texts, dimension knuffelkwartier")


if __name__ == "__main__":
    _direct()
