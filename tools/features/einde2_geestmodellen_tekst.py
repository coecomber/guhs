"""
einde2, slice geestmodellen - every Dutch text of the slice (the source language; English: tools/lang/en/c73_e2_geestmodellen.json).

  WEZENS    entity id -> (Dutch name, spawn egg base colour, spawn egg spot colour); the koi's two own names: KOI
  KINDS     GuhNpcEntity.Kind id -> its Dutch name (entity.guhs.guh_npc.<kind>)
  PRAATJES  entity or kind id -> its two lines of small talk (quest.guhs.einde2_geestmodellen.praatje.<id>.0 / .1)

The spirits are mysterious and never mean (a schaduwgeest grumbles and shoves, nothing more); a line is a wink at who someone
is, never what happens in the story. Nothing calls a story, a tier or a place the last.
"""
P = "quest.guhs.einde2_geestmodellen.praatje."

WEZENS = {
    "koh": ("Koh de Gezichtenrover", 0x2C2634, 0xEEE6DA),
    "hei_bai_guh": ("Hei Bai-guh", 0xF6F4F0, 0x222026),
    "fang": ("Fang", 0xCE2E2C, 0xFCECCE),
    "geest_koi": ("Geest-koi", 0xF6F4F0, 0x1E1C22),
    "libelkonijnguh": ("Libelkonijnguh", 0xE8E2F8, 0x6ED6FF),
    "lantaarngeest": ("Lantaarngeest", 0xD63A32, 0xF0C446),
    "schaduwgeest": ("Schaduwgeest", 0x181420, 0xF6F8FF),
}
KOI = {0: "Tui, de maangeest", 1: "La, de oceaangeest"}

KINDS = {
    "theegeest_guh": "De Theegeest",
    "bestelguh": "De Bestelguh",
}

PRAATJES = {
    "koh": ("Welkom, kleine guh. Wat een... interessant gezichtje heb je. Blijf vooral heel, heel kalm.",
            "Ik heb al duizend gezichten, njeg. Eentje erbij zou mij zo... vadsig staan."),
    "hei_bai_guh": ("*snuffelt aan je hand* ...Ruik jij een beetje naar knabbelkoor? Mijn bos ruikt al zo lang naar as.",
                    "Ssst. Hoor je het woud fluisteren? Het fluistert jouw naam. ...Nee, grapje. Het fluistert 'knabbel'."),
    "fang": ("Grrrom. Roku-guh en ik vlogen elke ochtend boven de wolken. Ik mis vooral het ontbijt erna.",
             "Een geest-draak vliegt waar zijn hart heen wil. Mijn hart wil meestal naar het theepaviljoen, njeg."),
    "geest_koi": ("Wij zwemmen al eeuwen rondjes. Duw en trek, licht en donker, Tui en La. Duizelig? Nooit.",
                  "Zonder de maan geen getij, zonder de oceaan geen maan. ...En zonder knabbels geen guhs."),
    "libelkonijnguh": ("Bzzzz-hop! *kijkt je héél nieuwsgierig aan* ...Bzzzz-HOP!",
                       "*snuffelt aan je neus en zoeft weg* ...*komt terug* ...*zoeft weer weg*"),
    "lantaarngeest": ("Ik ben een beetje verdwaald. Maar zolang ik brand, ben ik nooit helemaal in het donker, njeg!",
                      "*knipoogt* Een lantaarn die knipoogt? In de Geestenwereld is dat heel gewoon, hoor."),
    "schaduwgeest": ("Ssst... ga weg. Dit is mijn donkere randje. Ik duw maar een beetje. *duw*",
                     "Ik was ooit een gedachte die niemand wilde denken. Nu duw ik guhs opzij. Mooie carrière, njeg."),
    "theegeest_guh": ("Ik kwam hier ooit voor één kopje thee. Dat was driehonderd jaar geleden. De thee is nog steeds héérlijk.",
                      "Ik ken nog iemand die hier ooit wil wonen... rond, baard, zegt wijze dingen over thee. Ik hou een kussentje voor hem vrij."),
    "bestelguh": ("Eén bestelling? Komt eraan! *rent naar de bank* ...Bonnetje erbij, njeg?",
                  "Zeg maar wat je nodig hebt: ik haal alles uit je bank en laat de machines draaien. Snel, vrolijk en vads!"),
}


def texts():
    """{lang key: Dutch} of the slice (the spawn egg names are written by kit.spawn_ei)."""
    out = {}
    for id, (naam, _, _) in WEZENS.items():
        out[f"entity.guhs.{id}"] = naam
    for soort, naam in KOI.items():
        out[f"entity.guhs.geest_koi.{soort}"] = naam
    for kind, naam in KINDS.items():
        out[f"entity.guhs.guh_npc.{kind}"] = naam
    for id, (een, twee) in PRAATJES.items():
        out[f"{P}{id}.0"] = een
        out[f"{P}{id}.1"] = twee
    return out
