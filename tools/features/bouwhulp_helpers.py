"""
1.6.0 deel 2 F10b (design 2.8 + 2.9): de helper- en bouwdieren (Java: nl.juiced.guhs.feature.bouwhulp.helpers).

Only texts: the verblijfsbord's Bouwopdracht tab, the templates' "no" messages, "Bouw passend verblijf", the work lines
("knaag knaag..."). Dutch here; English in tools/lang/en/c183_bouwhulp_helpers.json.

  python tools/features/bouwhulp_helpers.py      (from the project root) writes only these texts into nl_nl.json and en_us.json
"""
import json
import os

HIER = os.path.dirname(os.path.abspath(__file__))
EN_BESTAND = os.path.join(HIER, "..", "lang", "en", "c183_bouwhulp_helpers.json")

P = "gui.guhs.bouwopdracht."

NEE = {
    "al_bezig": "Deze familie is al aan het klussen. Eén klus per bord, vads!",
    "geen_water": "Hier stroomt niks, njeg. Laat eerst de graafploeg een beekje graven.",
    "te_breed": "Dat water is te breed om over te gaan. Zoek een smaller stukje.",
    "geen_plek": "Hier in de buurt is geen vlak plekje te vinden.",
    "te_scheef": "Het is hier te scheef. Laat eerst een olifant het vlak stampen.",
    "geen_heuvel": "Hier is geen heuvel om in te graven.",
    "geen_omgevallen": "Hier liggen geen omgevallen bomen.",
    "al_vlak": "Het is hier al mooi vlak!",
    "geen_bomen": "Hier staan geen bomen.",
    "geen_tweede_boom": "Er staat geen tweede boom op 5 tot 20 blokken afstand.",
    "wijs_aan": "Wijs zelf een plek aan met de vormstaf.",
    "wijs_aan_twee": "Wijs zelf twee punten aan met de vormstaf.",
    "niets_te_slopen": "Daar valt niks te slopen.",
    "geen_sneeuw": "Hier ligt geen sneeuw.",
    "niets_te_doen": "Daar valt niks meer te doen: het is al af!",
    "geen_gras": "Hier is geen gras om op te planten.",
    "te_kort": "Dat is te dichtbij.",
    "te_ver": "Te ver weg: een klus mag tot 48 blokken van het bord liggen.",
    "twee_kisten": "Wijs twee kisten aan (eerst de kist waar het uit moet, dan die waar het in moet).",
    "geen_keet": "Er staat geen Bouwkeet in de buurt.",
    "andere_helper": "Daar is al een andere familie aan het werk. Eén helper tegelijk!",
    "geen_hout": "Dat is geen houten muur.",
    "te_klein": "Die muur is te klein voor houtsnijwerk.",
    "geen_kamer": "Dat is geen kamer: wijs de vloer aan van een ruimte met muren en een plafond.",
    "geen_muur": "Wijs een muur aan, met lucht ervoor.",
    "geen_tunnel": "Wijs een tunnel, grot of kelder aan (onder de grond).",
    "geen_soort": "Dit dier weet niet wat het nodig heeft.",
}

UITLEG = {
    "dam": "De bevers zoeken de smalste plek van een beek, bouwen er een dam en het water erachter stijgt tot een beverpoel.",
    "burcht": "Een beverburcht in het water: een hoop takken en modder met een droge kamer en een voordeur onder water.",
    "brug": "Een houten brug over het smalste stukje water, met leuningen.",
    "steiger": "Een steiger vanaf de oever het water in, met lantaarntjes aan het eind.",
    "vlonder": "Een vlonderpad langs de oever, boven het water.",
    "kappen": "De bevers knagen de bomen om. Er blijven gezellige stronkjes staan.",
    "kelder": "Een kelder onder de grond, met een ladder naar beneden en een lampje.",
    "tunnel": "Een tunnel de heuvel in, met houten balken en lantaarns.",
    "vijver": "Een vijver, gelijk met de grond, met waterlelies.",
    "holletjesnet": "Een holletjesnet: kamertjes onder de grond met gangetjes en twee uitgangen.",
    "beekje": "Een beekje dat de heuvel af stroomt, met een grindbedje.",
    "sjouwen": "De olifant sjouwt voor een Bouwkeet in de buurt: dat project gaat sneller.",
    "opruimen": "De olifant ruimt omgevallen bomen op.",
    "stampen": "De olifant stampt de grond vlak.",
    "modderpoel": "Een heerlijke modderpoel.",
    "slopen": "De sloopploeg haalt een gebouw of een rotsblok weg. Wijs het aan met de vormstaf.",
    "zebrapad": "De kudde loopt een pad van het bord naar een plek, over het terrein.",
    "planten": "Bomen, struiken, bloemen of een boomgaard planten.",
    "boomhut": "Een boomhut in een boom, met een ladder en een dakje.",
    "touwbrug": "Een touwbrug tussen twee bomen.",
    "hangbrug": "Een hangbrug die in het midden een beetje doorzakt.",
    "iglo": "Een iglo met een ijsraampje en een gangetje naar binnen.",
    "ijsglijbaan": "Een ijsglijbaan met een trapje omhoog.",
    "ijssculptuur": "Een ijssculptuur van een guh!",
    "ijsvloer": "Het water bevriest tot een ijsvloer.",
    "badje": "Een warm badje met een kampvuurtje ernaast.",
    "vijvertje": "Een klein rond vijvertje.",
    "hottub": "Een ronde hottub met een stenen rand.",
    "stapstenen": "Stapstenen om het water over te steken.",
    "modderglijbaan": "Een modderglijbaan het water in. Wieee!",
    "oever": "Roze bloemen, riet en waterlelies langs de oever.",
    "sneeuwschuiven": "De mammoet schuift de sneeuw weg, in hoopjes aan de rand.",
    "sneeuwlaag": "De mammoet legt een sneeuwlaagje.",
    "buideltransport": "De kangoeroe springt spullen van de ene kist naar de andere.",
    "houtsnijwerk": "De specht tikt een patroon in een houten muur.",
    "inrichten": "De octopus richt een kamer in. Wijs de vloer aan.",
    "slingers": "De gibbon hangt slingers tussen twee punten.",
    "gevel": "De gekko maakt een gevel levendig: klimop en bloempotjes onder de ramen.",
    "licht": "De glimwormpjes verlichten een tunnel, grot of kelder met zacht licht.",
    "decoreren": "De prieelvogel versiert een plekje in een kleurthema.",
}

VARIANT = {
    "planten": ["Bos", "Struiken", "Bloemen", "Boomgaard"],
    "zebrapad": ["Kuddepad", "Gestreept zebrapad"],
    "inrichten": ["Knus", "Safari", "Keuken", "Slaapkamer"],
    "slingers": ["Vlaggetjes", "Lichtjes"],
    "decoreren": ["Roze", "Blauw", "Geel", "Paars", "Regenboog"],
    "houtsnijwerk": ["Guh-gezichtjes", "Hartjes", "Ruitjes"],
    "ijsvloer": ["Ijs", "Pakijs"],
}

WERK = {
    "bever": "knaag knaag...", "olifant": "stamp stamp...", "neushoorn": "duw duw...", "kafferbuffel": "duw duw...",
    "zebra": "trappel trappel...", "stokstaartje": "graaf graaf...", "das": "graaf graaf...", "wombat": "graaf graaf...",
    "flamingo": "pik pik...", "capibara": "plons plons...", "gibbon": "slinger slinger...", "orang_oetan": "klop klop...",
    "toekan": "plant plant...", "koala": "plant plant...", "hommel": "zoem zoem...", "gekko": "klim klim...",
    "glimworm": "gloei gloei...", "pinguin": "waggel waggel...", "octopus": "tik tik...", "mammoet": "schuif schuif...",
    "otter": "spetter spetter...", "specht": "tok tok tok...", "kangoeroe": "hop hop...", "prieelvogel": "schik schik...",
}

TEKSTEN = {
    P + "spook": "%2$s (%1$s)",
    P + "klaar": "De %1$s-familie is klaar: %2$s! Kom kijken.",
    P + "transport.start": "De kangoeroe gaat aan de slag: hop hop, van kist naar kist!",
    P + "sjouwen.start": "De olifant gaat sjouwen voor de Bouwkeet!",
    P + "bank_gekoppeld": "Bank Guh gekoppeld: de familie pakt haar blokken ook uit de bank.",
    P + "wijs.een": "Sluit het menu en klik met de vormstaf op de plek voor de klus.",
    P + "wijs.twee": "Sluit het menu en klik met de vormstaf op twee punten (eerst A, dan B).",
    P + "wijs.nog": "Nog één punt!",
    P + "wijs.uitleg": "Je hebt een vormstaf nodig. Je hebt 2 minuten.",
    P + "zoek": "Zoek plek",
    P + "wijs": "Zelf aanwijzen",
    P + "wijs_twee": "Twee punten aanwijzen",
    P + "bedien": "Spook...",
    P + "herbereken": "Herbereken",
    P + "herbereken.uitleg": "Heb je het spook verschoven? Dan rekenen ze het opnieuw uit op die plek.",
    P + "stop": "Stoppen",
    P + "variant": "Soort: %s",
    P + "grootte.0": "Mopperige familie: ze bouwen het klein.",
    P + "grootte.1": "Tevreden familie: ze bouwen het gewoon.",
    P + "grootte.2": "Blije familie: ze bouwen het groot en extra mooi!",
    P + "loopt.transport": "Bezig: %s",
    P + "loopt.sjouwen": "Bezig: %s",
    P + "onderweg": "Op werkuitje naar de klus: %s",
    P + "status.spook": "Spook staat klaar: %s. Druk op Bouw! in het spook.",
    P + "status.bouwt": "Aan het bouwen: %s",
    P + "status.wacht": "Wacht op blokken: %s",
    P + "bank.ja": "Bank Guh gekoppeld.",
    P + "bank.nee": "Geen Bank Guh (klik het bord met een Banksleutel).",
    P + "klaar_teller": "Klusjes af: %s",
    P + "verblijf.uitleg": "Zet een passend verblijf neer als spook, met alles wat dit dier blij maakt en een hek eromheen. De familie bouwt mee.",
    P + "werk.anders": "druk druk...",
}
for k, v in NEE.items():
    TEKSTEN[P + "nee." + k] = v
for k, v in UITLEG.items():
    TEKSTEN[P + "uitleg." + k] = v
for k, namen in VARIANT.items():
    for i, n in enumerate(namen):
        TEKSTEN[P + f"variant.{k}.{i}"] = n
for k, v in WERK.items():
    TEKSTEN[P + "werk." + k] = v


def build(h):
    for k, v in TEKSTEN.items():
        h.NL[k] = v


def _zelf():
    root = os.path.join("src", "main", "resources")
    with open(EN_BESTAND, encoding="utf-8") as f:
        en = json.load(f)
    for taal, teksten in (("nl_nl", TEKSTEN), ("en_us", {k: en.get(k, v) for k, v in TEKSTEN.items()})):
        p = os.path.join(root, "assets", "guhs", "lang", taal + ".json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(teksten)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    zonder = [k for k in TEKSTEN if k not in en]
    print(f"bouwhulp_helpers: {len(TEKSTEN)} teksten" + (f", {len(zonder)} zonder Engels: {zonder[:5]}" if zonder else ""))


if __name__ == "__main__":
    _zelf()
