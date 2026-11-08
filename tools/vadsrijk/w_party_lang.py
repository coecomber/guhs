"""W-PARTY: the Dutch texts of the Party tab and the nep-spelers, merged into nl_nl.json and en_us.json (en = Dutch copy until the
English pass). Run from the repo root: python tools/vadsrijk/w_party_lang.py"""
import json
import os

T = {
    "entity.guhs.vr_nepspeler": "Nep-speler",
    "vadsrijk.menu.tab.party": "Party",
    "vadsrijk.menu.tab.party.uitleg": "Je party: spelers uitnodigen, leider, samen in de wachtrij, nep-spelers",
    "vadsrijk.menu.instellingen.nepspelers": "Nep-spelers",
    "vadsrijk.menu.instellingen.nepspelers.uitleg": "Fake spelers met gamer-namen in de mobzones. Uit = je ziet ze niet, ze vullen jouw dungeon-wachtrij niet en je kunt ze niet meevragen.",
    # kern party (new results)
    "vadsrijk.kern.party.geweigerd": "Je hebt de uitnodiging afgeslagen.",
    "vadsrijk.kern.party.geen_party": "Je zit niet in een party.",
    "vadsrijk.kern.party.niet_leider": "Alleen de partyleider kan dat.",
    "vadsrijk.kern.party.zelf_weg": "Jezelf eruit zetten? Kies dan Verlaten.",
    "vadsrijk.kern.party.verwijderd": "%s is uit de party gezet.",
    "vadsrijk.kern.party.geen_lid": "Die zit niet in je party.",
    "vadsrijk.kern.party.nieuwe_leider": "%s is nu de partyleider!",
    "vadsrijk.kern.party.uitnodiging": "%s nodigt je uit voor een party! Open het Party-tabblad (H) of typ /guhs vadsrijk party accepteer",
    # invitations
    "vadsrijk.party.uitnodiging": "%s nodigt je uit voor een party!",
    "vadsrijk.party.uitnodiging.kort": "%s nodigt je uit!",
    "vadsrijk.party.knop.accepteer": "Accepteer",
    "vadsrijk.party.knop.accepteer.uitleg": "Ga in de party",
    "vadsrijk.party.knop.weiger": "Weiger",
    "vadsrijk.party.knop.weiger.uitleg": "Nee, dank je",
    "vadsrijk.party.toast.titel": "Party-uitnodiging",
    "vadsrijk.party.toast.tekst": "%s wil met je spelen! Kijk in de chat of in het Party-tabblad.",
    "vadsrijk.party.erbij.ander": "%s zit nu in de party!",
    "vadsrijk.party.geweigerd.ander": "%s heeft je uitnodiging afgeslagen.",
    "vadsrijk.party.weg.ander": "%s heeft de party verlaten.",
    "vadsrijk.party.verwijderd.ander": "%s is uit de party gezet.",
    "vadsrijk.party.verwijderd.jij": "%s heeft je uit de party gezet.",
    # group queue
    "vadsrijk.party.rij.geen_dungeons": "Er zijn (nog) geen dungeons om voor in de rij te gaan.",
    "vadsrijk.party.raid.te_klein": "Voor de raid heb je minstens 2 spelers in het Vadsrijk nodig (nep-spelers tellen niet).",
    "vadsrijk.party.raid.al_bezig": "Iemand van de party zit al in de raid.",
    "vadsrijk.party.raid.al_in_rij": "Jullie staan al in de rij voor de raid.",
    "vadsrijk.party.raid.erin": "De party staat in de rij voor De Sterrendovende Toren! Ga naar De Wachtzaal: als jullie beurt komt, gaat wie daar staat mee naar boven.",
    # roles
    "vadsrijk.party.rol.tank": "Tank",
    "vadsrijk.party.rol.heler": "Heler",
    "vadsrijk.party.rol.schade": "Schade",
    # nep-spelers: system texts
    "vadsrijk.party.nep.nog": "%s blijft nog %s min. bij je.",
    "vadsrijk.party.nep.knop.weg": "Doei!",
    "vadsrijk.party.nep.knop.weg.uitleg": "Laat deze nep-speler gaan",
    "vadsrijk.party.nep.knop.mee": "Kom je mee?",
    "vadsrijk.party.nep.knop.mee.uitleg": "Vraag of deze nep-speler 20 tot 30 minuten met je party meegaat",
    "vadsrijk.party.nep.knop.laat": "(of loop gewoon door)",
    "vadsrijk.party.nep.server_uit": "Nep-spelers staan uit op deze server.",
    "vadsrijk.party.nep.zelf_uit": "Je hebt nep-spelers uitgezet (Vadsrijk-menu > Instellingen).",
    "vadsrijk.party.nep.weg": "Die nep-speler is er niet meer.",
    "vadsrijk.party.nep.bezet": "Die zit al in een andere party.",
    "vadsrijk.party.nep.te_ver": "Loop wat dichterbij om het te vragen.",
    "vadsrijk.party.nep.mee": "%s gaat met je mee!",
    "vadsrijk.party.nep.gevuld": "Niemand anders kwam opdagen: %s nep-speler(s) vullen de rij voor %s.",
    # party tab
    "vadsrijk.party.tab.party": "Mijn party (%s/%s)",
    "vadsrijk.party.tab.geen_party": "Je zit nog niet in een party. Nodig rechts iemand uit, of vraag een nep-speler in een mobzone: \"Kom je mee?\"",
    "vadsrijk.party.tab.offline": "offline",
    "vadsrijk.party.tab.lid": "Lv %s · %s · %s",
    "vadsrijk.party.tab.minuten": " · nog %s min",
    "vadsrijk.party.tab.vuller": " · vult de dungeon",
    "vadsrijk.party.tab.buiten": "buiten het Vadsrijk",
    "vadsrijk.party.tab.knop.weg": "Weg",
    "vadsrijk.party.tab.knop.leider": "Leider",
    "vadsrijk.party.tab.knop.verlaat": "Party verlaten",
    "vadsrijk.party.tab.nep.server_uit": "Nep-spelers: uit (server)",
    "vadsrijk.party.tab.nep.zelf_uit": "Nep-spelers: uit (jij)",
    "vadsrijk.party.tab.spelers": "Spelers in het Vadsrijk",
    "vadsrijk.party.tab.niemand": "Er is verder niemand in het Vadsrijk.",
    "vadsrijk.party.tab.samen": "in je party",
    "vadsrijk.party.tab.uitgenodigd": "uitgenodigd",
    "vadsrijk.party.tab.knop.uitnodigen": "Uitnodigen",
    "vadsrijk.party.tab.rij": "Samen in de wachtrij",
    "vadsrijk.party.tab.rij.in": "In de rij voor: %s",
    "vadsrijk.party.tab.rij.geen": "Dungeons: via de dungeondeur.",
    "vadsrijk.party.tab.knop.rij": "Party in rij",
    "vadsrijk.party.tab.knop.raid": "Raid: party in de rij",
    "vadsrijk.party.tab.rij.leider": "Alleen de partyleider kan de hele party in een wachtrij zetten.",
}

ZEG = {
    "los": ["lfg dungeon??", "wie heeft er een heler", "brb mama roept", "lag...", "waar is de zwamtempel eigenlijk", "xp gaat lekker hier",
            "k ben lvl %s ofzo", "is dit spel p2w?", "kan iemand mij boosten pls", "hoe kom je bij zone 2??", "ff afk", "dit spel is zo vads",
            "iemand ruilen? heb 3 klodderslijm", "wie wil er knuffelen lol"],
    "gevecht": ["pull!", "aggro!!", "focus die grote", "ff kiten", "ik tank wel", "heal pls", "crit!!", "AAAH hij slaat hard"],
    "heal": ["heal incoming", "+heal", "ik heb je!", "niet doodgaan he"],
    "flauw": ["rip", "ik ben flauw lol", "lag!!! niet eerlijk", "brb respawnen"],
    "hoi": ["yo", "hoi!", "sup", "wat is er?", "hallo :)"],
    "ja": ["ok ik ga mee!", "ja sure, %s min dan moet ik eten", "yesss party", "kk ik volg je"],
    "nee": ["sorry zit al in een party", "jullie zijn vol lol"],
    "doei": ["moet eten, doei!", "mama zegt dat ik moet stoppen, gg", "gg wp, cya", "ik ga slapen, doeiii", "bedankt voor de party! gg"],
    "gg": ["gg", "ez", "nice", "gg wp"],
    "vullen": ["hoi ik kom jullie rij vullen", "lfg was ik, hier ben ik", "yo! ik doe mee"],
}
for soort, regels in ZEG.items():
    for i, r in enumerate(regels, 1):
        T[f"vadsrijk.party.nep.zeg.{soort}.{i}"] = r

for naam in ("nl_nl", "en_us"):
    pad = os.path.join("src", "main", "resources", "assets", "guhs", "lang", naam + ".json")
    with open(pad, encoding="utf-8") as f:
        d = json.load(f)
    for k, v in T.items():
        if naam == "nl_nl" or k not in d:   # (en: keep existing English texts)
            d[k] = v
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(naam, len(T), "keys")
