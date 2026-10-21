"""
1.6.0 "Op Safari!" deel 2, F9: het gereedschap en de blauwdrukken. Java: feature/bouwhulp (vormen, gereedschap,
blauwdruk; GereedschapFeature).

  - items vormstaf, vervangkwast, mengkwast, patroonkwast, verweerkwast (tier 0), egaliseerder (works from a Zoutkeet),
    verfmachine and tunnelboor (from a Sauskeet), blauwdruk (printed at the Ontwerpbureau or bought in the giftshop)
  - block ontwerpbureau: a drawing desk with a blue sheet (the blueprint browser)
  - the texts of the tools' settings, the radial menu, the capture frame, the blueprint browser and the 25 catalogue
    designs

  python tools/features/bouwhulp_gereedschap.py      on its own: writes the files, merges the texts into nl_nl / en_us
"""
import json
import os

from PIL import Image

HIER = os.path.dirname(os.path.abspath(__file__))
EN_BESTAND = os.path.join(HIER, "..", "lang", "en", "c181_bouwhulp_gereedschap.json")

ITEMS = ["vormstaf", "vervangkwast", "mengkwast", "patroonkwast", "verweerkwast", "egaliseerder", "verfmachine", "tunnelboor",
         "blauwdruk"]

VORMEN = {"bol": "Bol", "koepel": "Koepel", "cilinder": "Cilinder / toren", "boog": "Boog", "tunnel": "Gewelfde tunnel",
          "brug": "Brug", "piramide": "Piramide", "kegel": "Kegel", "ring": "Ring", "rots": "Rots", "heuvel": "Heuvel",
          "boomkruin": "Boomkruin", "eiland": "Zweefeiland", "wenteltrap": "Wenteltrap", "helix": "Helix", "hart": "3D-hart",
          "guh": "Guh-silhouet", "tekst": "Bloktekst", "ei": "Ei", "halve_cilinder": "Halve cilinder", "kasteelmuur": "Kasteelmuur"}
DAKEN = {"zadel": "Zadeldak", "schild": "Schilddak", "tent": "Tent-/kegeldak", "oortjes": "Guh-oortjesdak"}
PATRONEN = {"verloop": "Verloop", "streep": "Strepen", "dambord": "Dambord", "guhgezicht": "Guh-gezichtjes", "zebra": "Zebra",
            "giraffe": "Giraffe", "luipaard": "Luipaard", "jachtluipaard": "Jachtluipaard", "pinguin": "Pinguïn", "panda": "Panda",
            "rode_panda": "Rode panda", "sabeltand": "Sabeltandtijger"}
KLEUREN = {"white": "Wit", "orange": "Oranje", "magenta": "Magenta", "light_blue": "Lichtblauw", "yellow": "Geel", "lime": "Limoen",
           "pink": "Roze", "gray": "Grijs", "light_gray": "Lichtgrijs", "cyan": "Cyaan", "purple": "Paars", "blue": "Blauw",
           "brown": "Bruin", "green": "Groen", "red": "Rood", "black": "Zwart"}
CATALOGUS = {
    "lodge_hutje": "Lodge-hutje op palen", "rangerpost": "Rangerpost", "acacia_uitkijktoren": "Acacia-uitkijktoren",
    "watergat_terras": "Watergat-terras", "safaripoort": "Safaripoort", "rieten_paviljoen": "Rieten picknickpaviljoen",
    "guh_huisje_klein": "Guh-huisje (klein)", "guh_huis_middel": "Guh-huis (middel)", "guh_villa_groot": "Guh-villa (groot)",
    "guh_boerderijtje": "Guh-boerderijtje met schuur", "guh_winkeltje": "Guh-winkeltje met luifel", "guh_rijtje": "Rijtje pastel guh-huisjes",
    "kas_met_oortjes": "Kas met oortjes", "bloemenprieel": "Bloemenprieel", "boogbrug": "Boogbrug", "steiger_botenhuisje": "Steiger + botenhuisje",
    "vijver_stapstenen": "Vijver met stapstenen", "picknickplek": "Picknickplek", "guh_speeltuintje": "Guh-speeltuintje",
    "guh_vuurtoren": "Guh-vuurtoren", "guh_windmolen": "Guh-windmolen", "kasteeltoren": "Kasteeltoren met oortjes",
    "boomhut": "Boomhut met guh-raam", "guhbol_koepel": "Guhbol-uitkijkkoepel", "guh_klokkentoren": "Guh-klokkentoren",
}

# ======================================================================================================================
# texts (Dutch; English in tools/lang/en/c181_bouwhulp_gereedschap.json)
TEKSTEN = {
    "item.guhs.vormstaf": "Vormstaf",
    "item.guhs.vormstaf.lore": "Klik op een blok: je vorm staat er als spook. Sluip-klik: dat blok wordt je materiaal. Houd de Vormstaf-toets in voor het menu.",
    "item.guhs.vervangkwast": "Vervangkwast",
    "item.guhs.vervangkwast.lore": "Verf blokken om naar je eigen blok, in dezelfde vorm (een trap blijft een trap). Sluip-klik: kies het blok.",
    "item.guhs.mengkwast": "Mengkwast",
    "item.guhs.mengkwast.lore": "Een mix van tot vier blokken (70/20/10). Sluip-klik op blokken om ze aan de mix toe te voegen.",
    "item.guhs.patroonkwast": "Patroonkwast",
    "item.guhs.patroonkwast.lore": "Strepen, dambord, verloop, guh-gezichtjes. Dierpatronen gaan open als dat dier in je safari-dex staat.",
    "item.guhs.verweerkwast": "Verweerkwast",
    "item.guhs.verweerkwast.lore": "Mos, barstjes en klimop: maakt alles een beetje oud en knus.",
    "item.guhs.egaliseerder": "Egaliseerder",
    "item.guhs.egaliseerder.lore": "Vlak maken, een helling, een kuil of vijver, of een heuvel. Werkt vanaf een Zoutkeet.",
    "item.guhs.verfmachine": "Verfmachine",
    "item.guhs.verfmachine.lore": "Kleurt wol, beton en terracotta in één keer om, ook als regenboogverloop. Werkt vanaf een Sauskeet.",
    "item.guhs.tunnelboor": "Tunnelboor",
    "item.guhs.tunnelboor.lore": "Rond, vierkant of gewelfd, met bekleding en lampjes. Geen mijnmodus, gewoon een mooie tunnel. Vanaf een Sauskeet.",
    "item.guhs.blauwdruk": "Blauwdruk",
    "item.guhs.blauwdruk.van": "Blauwdruk: %s",
    "item.guhs.blauwdruk.lore": "Klik op een blok: het ontwerp staat er als spook. Raakt niet op.",
    "item.guhs.blauwdruk.maker": "Getekend door %s",
    "block.guhs.ontwerpbureau": "Ontwerpbureau",
    "item.guhs.ontwerpbureau": "Ontwerpbureau",
    "key.guhs.vormstaf_menu": "Vormstaf: rond menu (ingedrukt houden)",
    # --- the tools in general ---
    "gui.guhs.bouwhulp.gekoppeld": "Gekoppeld aan deze keet (%s). Je gereedschap werkt nu op zijn niveau!",
    "gui.guhs.bouwhulp.keet": "Gekoppeld aan een keet: %s",
    "gui.guhs.bouwhulp.geen_keet": "Sluip-klik op een Bouwkeet om te koppelen (anders: handwerk)",
    "gui.guhs.bouwhulp.vanaf": "Werkt vanaf een %s (koppel hem aan zo'n keet)",
    "gui.guhs.bouwhulp.dicht.natuur": "Natuurvormen kunnen vanaf een Zoutkeet, vads.",
    "gui.guhs.bouwhulp.dicht.bloktekst": "Bloktekst kan vanaf een Zoutkeet, vads.",
    "gui.guhs.bouwhulp.dicht.dex": "Fotografeer dit dier eerst in het safaripark (Guhdex), dan mag je zijn patroon gebruiken!",
    "gui.guhs.bouwhulp.pipet.blok": "Blok gekozen: %s",
    "gui.guhs.bouwhulp.pipet.mix": "Aan de mix toegevoegd: %s",
    "gui.guhs.bouwhulp.kwast.niets": "Hier valt niks te verven, njeg.",
    "gui.guhs.bouwhulp.kwast.kies_blok": "Kies eerst een blok: sluip-klik erop met de kwast.",
    "gui.guhs.bouwhulp.kwast.kies_mix": "Kies eerst blokken: sluip-klik erop met de kwast (tot vier).",
    "gui.guhs.bouwhulp.kwast.kies_twee": "Voor dit patroon heb je minstens twee blokken in de mix nodig.",
    "gui.guhs.bouwhulp.aan": "Aan",
    "gui.guhs.bouwhulp.uit": "Uit",
    "gui.guhs.bouwhulp.hol": "Hol",
    "gui.guhs.bouwhulp.gevuld": "Gevuld",
    "gui.guhs.bouwhulp.standaard": "(standaard)",
    "gui.guhs.bouwhulp.kies": "Kies een blok uit je hotbar (of linkerhand):",
    "gui.guhs.bouwhulp.kies.uitleg": "Kies een blok uit je hotbar. Of sluip-klik met het gereedschap op een blok in de wereld.",
    "gui.guhs.bouwhulp.plaats_spook": "Plaats spook",
    "gui.guhs.bouwhulp.plaats_spook.uitleg": "Zet het voor de deur van de keet neer als project. Daarna: schuiven, draaien, Bouw!",
    "gui.guhs.bouwhulp.rond.uitleg": "scroll: maat · shift: hoogte · ctrl: dikte · H: hol",
    "gui.guhs.bouwkeet.bezig": "De keet is nog aan het bouwen. Even wachten, of pauzeer eerst.",
    "gui.guhs.bouwhulp.uitleg.vormstaf": "Klik op een blok om je vorm als spook neer te zetten. Sluip-klik op een blok: wandmateriaal.",
    "gui.guhs.bouwhulp.uitleg.vervangkwast": "Elke klik verft een bolletje blokken om naar je blok, in dezelfde vorm.",
    "gui.guhs.bouwhulp.uitleg.mengkwast": "Elke klik verft een bolletje blokken in je mix (70/20/10).",
    "gui.guhs.bouwhulp.uitleg.patroonkwast": "Elke klik verft een patroon. Dierpatronen kleuren wol, beton en terracotta.",
    "gui.guhs.bouwhulp.uitleg.verweerkwast": "Elke klik maakt het oud: mos, barstjes, klimop.",
    "gui.guhs.bouwhulp.uitleg.egaliseerder": "Klik op de grond: het nieuwe terrein staat er als spook. Bekijk het, en druk op Bouw!",
    "gui.guhs.bouwhulp.uitleg.verfmachine": "Klik midden in je bouwwerk: alle wol, beton en terracotta eromheen krijgt een nieuwe kleur.",
    "gui.guhs.bouwhulp.uitleg.tunnelboor": "Klik op een wand: de tunnel gaat de kant op waar je kijkt. Sluip-klik op een blok: bekleding.",
    # --- the option labels ---
    "gui.guhs.bouwhulp.optie.modus": "Wat",
    "gui.guhs.bouwhulp.optie.vorm": "Vorm",
    "gui.guhs.bouwhulp.optie.dak": "Dak",
    "gui.guhs.bouwhulp.optie.b": "Breedte",
    "gui.guhs.bouwhulp.optie.h": "Hoogte",
    "gui.guhs.bouwhulp.optie.d": "Diepte",
    "gui.guhs.bouwhulp.optie.hol": "Hol",
    "gui.guhs.bouwhulp.optie.dikte": "Wanddikte",
    "gui.guhs.bouwhulp.optie.overstek": "Overstek",
    "gui.guhs.bouwhulp.optie.gevels": "Gevels",
    "gui.guhs.bouwhulp.optie.wand": "Wand",
    "gui.guhs.bouwhulp.optie.rand": "Rand / vloer",
    "gui.guhs.bouwhulp.optie.tekst": "Tekst",
    "gui.guhs.bouwhulp.optie.blok": "Blok",
    "gui.guhs.bouwhulp.optie.mix": "Mix",
    "gui.guhs.bouwhulp.optie.straal": "Straal",
    "gui.guhs.bouwhulp.optie.zelfde": "Alleen dit soort blok",
    "gui.guhs.bouwhulp.optie.buitenkant": "Alleen de buitenkant",
    "gui.guhs.bouwhulp.optie.patroon": "Patroon",
    "gui.guhs.bouwhulp.optie.mos": "Mos",
    "gui.guhs.bouwhulp.optie.barsten": "Barstjes",
    "gui.guhs.bouwhulp.optie.klimop": "Klimop",
    "gui.guhs.bouwhulp.optie.sterkte": "Sterkte",
    "gui.guhs.bouwhulp.optie.stand": "Stand",
    "gui.guhs.bouwhulp.optie.hoogte": "Hoogte",
    "gui.guhs.bouwhulp.optie.omlaag": "Omlaag",
    "gui.guhs.bouwhulp.optie.water": "Met water",
    "gui.guhs.bouwhulp.optie.ruwheid": "Ruwheid",
    "gui.guhs.bouwhulp.optie.kleur": "Kleur",
    "gui.guhs.bouwhulp.optie.verloop": "Verloop",
    "gui.guhs.bouwhulp.optie.kleur2": "Naar kleur",
    "gui.guhs.bouwhulp.optie.as": "Verloop langs",
    "gui.guhs.bouwhulp.optie.breedte": "Breedte",
    "gui.guhs.bouwhulp.optie.lengte": "Lengte",
    "gui.guhs.bouwhulp.optie.bekleding": "Bekleding",
    "gui.guhs.bouwhulp.optie.lampjes": "Lampjes",
    # --- choices ---
    "gui.guhs.bouwhulp.keuze.modus.vorm": "Vorm",
    "gui.guhs.bouwhulp.keuze.modus.dak": "Dak",
    "gui.guhs.bouwhulp.keuze.modus.vastleg": "Vastleggen",
    "gui.guhs.bouwhulp.keuze.stand.vlak": "Vlak",
    "gui.guhs.bouwhulp.keuze.stand.helling": "Helling",
    "gui.guhs.bouwhulp.keuze.stand.kuil": "Kuil / vijver",
    "gui.guhs.bouwhulp.keuze.stand.heuvel": "Heuvel",
    "gui.guhs.bouwhulp.keuze.as.hoogte": "de hoogte",
    "gui.guhs.bouwhulp.keuze.as.lengte": "waar je kijkt",
    "gui.guhs.bouwhulp.keuze.vorm.rond": "Rond",
    "gui.guhs.bouwhulp.keuze.vorm.vierkant": "Vierkant",
    "gui.guhs.bouwhulp.keuze.vorm.gewelfd": "Gewelfd",
    # --- capturing ---
    "gui.guhs.bouwhulp.vastleg.titel": "Blauwdruk vastleggen",
    "gui.guhs.bouwhulp.vastleg.hoek1": "Eerste hoek: %s, %s, %s. Klik nu op de andere hoek.",
    "gui.guhs.bouwhulp.vastleg.maat": "Kader: %s × %s × %s (mag tot %s)",
    "gui.guhs.bouwhulp.vastleg.uitleg": "Het roze kader zie je in de wereld. Lege plekjes binnenin het gebouw gaan mee.",
    "gui.guhs.bouwhulp.vastleg.kant.west": "West",
    "gui.guhs.bouwhulp.vastleg.kant.oost": "Oost",
    "gui.guhs.bouwhulp.vastleg.kant.onder": "Onder",
    "gui.guhs.bouwhulp.vastleg.kant.boven": "Boven",
    "gui.guhs.bouwhulp.vastleg.kant.noord": "Noord",
    "gui.guhs.bouwhulp.vastleg.kant.zuid": "Zuid",
    "gui.guhs.bouwhulp.vastleg.naam": "Naam van je blauwdruk",
    "gui.guhs.bouwhulp.vastleg.naamloos": "Mijn bouwwerk",
    "gui.guhs.bouwhulp.vastleg.opslaan": "Opslaan",
    "gui.guhs.bouwhulp.vastleg.annuleer": "Annuleer",
    "gui.guhs.bouwhulp.vastleg.eigen": "Eigen blauwdrukken: %s van de %s",
    "gui.guhs.bouwhulp.vastleg.te_groot": "Te groot! Dit niveau mag tot %s blokjes.",
    "gui.guhs.bouwhulp.vastleg.leeg": "Er staat niks in het kader, vads.",
    "gui.guhs.bouwhulp.vastleg.vol": "Je hebt al %s blauwdrukken. Gooi er eerst eentje weg (Ontwerpbureau).",
    "gui.guhs.bouwhulp.vastleg.bewaard": "Blauwdruk \"%s\" bewaard (%s blokjes)! Je vindt hem in de Bouwkeet en bij het Ontwerpbureau.",
    # --- the blueprint browser ---
    "gui.guhs.blauwdruk.bron.eigen": "Eigen",
    "gui.guhs.blauwdruk.bron.catalogus": "Catalogus",
    "gui.guhs.blauwdruk.bron.bibliotheek": "Bibliotheek",
    "gui.guhs.blauwdruk.leeg.eigen": "Nog geen eigen blauwdrukken. Zet je vormstaf op Vastleggen en klik op je mooiste bouwwerk!",
    "gui.guhs.blauwdruk.leeg.catalogus": "De catalogus is leeg.",
    "gui.guhs.blauwdruk.leeg.bibliotheek": "Nog niks gepubliceerd op deze server. Wees de eerste (vanaf een Sauskeet)!",
    "gui.guhs.blauwdruk.binnenkort": "Meer hier binnenkort!",
    "gui.guhs.blauwdruk.maat": "%s × %s × %s, %s blokjes",
    "gui.guhs.blauwdruk.maker": "door %s",
    "gui.guhs.blauwdruk.groep.safari": "Safari-gebouwen",
    "gui.guhs.blauwdruk.groep.huizen": "Guh-huizen",
    "gui.guhs.blauwdruk.groep.tuin": "Tuin & buiten",
    "gui.guhs.blauwdruk.groep.torens": "Torens & bijzonder",
    "gui.guhs.blauwdruk.plaats": "Plaats",
    "gui.guhs.blauwdruk.plaats.uitleg": "Wordt het project van deze keet: een spook voor de deur.",
    "gui.guhs.blauwdruk.print": "Print",
    "gui.guhs.blauwdruk.print.uitleg": "Een blauwdruk om mee te nemen (kost een vel papier). Klik ermee op een blok, waar dan ook.",
    "gui.guhs.blauwdruk.papier": "Je hebt een vel papier nodig om te printen.",
    "gui.guhs.blauwdruk.weg": "Weggooien (alleen je eigen, of als op)",
    "gui.guhs.blauwdruk.kwijt": "Deze blauwdruk bestaat niet meer. Weggegooid?",
    "gui.guhs.blauwdruk.tellers": "Eigen: %s/%s · gepubliceerd: %s/%s",
    "gui.guhs.blauwdruk.bureau.uitleg": "Publiceren doe je in een Bouwkeet (vanaf een Sauskeet).",
    "gui.guhs.blauwdruk.publiceer.uitleg": "Publiceer in de bibliotheek van de server, met jouw naam erbij!",
    "gui.guhs.blauwdruk.publiceer.vanaf": "Publiceren kan vanaf een Sauskeet (en niet groter dan wat de keet mag).",
    "gui.guhs.blauwdruk.publiceer.te_groot": "Te groot voor deze keet: hij mag tot %s.",
    "gui.guhs.blauwdruk.publiceer.ok": "Gepubliceerd! Iedereen op de server kan hem nu bouwen.",
    "gui.guhs.blauwdruk.publiceer.vol": "Je hebt er al %s in de bibliotheek staan. Haal er eerst een weg.",
    "gui.guhs.blauwdruk.publiceer.al": "Die staat al in de bibliotheek, vads.",
    "gui.guhs.blauwdruk.publiceer.leeg": "Een lege blauwdruk publiceren? Njeg.",
    "gui.guhs.blauwdruk.winkel.uitleg": "Bouw dit parkgebouw gewoon thuis na: een blauwdruk die je overal kunt neerzetten.",
}
for _id, _n in VORMEN.items():
    TEKSTEN[f"gui.guhs.bouwhulp.keuze.vorm.{_id}"] = _n
for _id, _n in DAKEN.items():
    TEKSTEN[f"gui.guhs.bouwhulp.keuze.dak.{_id}"] = _n
for _id, _n in PATRONEN.items():
    TEKSTEN[f"gui.guhs.bouwhulp.keuze.patroon.{_id}"] = _n
for _id, _n in KLEUREN.items():
    TEKSTEN[f"gui.guhs.bouwhulp.keuze.kleur.{_id}"] = _n
    TEKSTEN[f"gui.guhs.bouwhulp.keuze.kleur2.{_id}"] = _n
for _id, _n in CATALOGUS.items():
    TEKSTEN[f"gui.guhs.blauwdruk.cat.{_id}"] = _n

# ======================================================================================================================
# icons (16 x 16)
PALET = {"k": (70, 40, 30, 255), "h": (150, 100, 60, 255), "l": (196, 146, 92, 255), "p": (242, 140, 186, 255),
         "q": (204, 96, 146, 255), "w": (255, 250, 240, 255), "b": (78, 128, 196, 255), "s": (190, 220, 240, 255),
         "g": (130, 130, 140, 255), "i": (200, 204, 212, 255), "y": (250, 214, 102, 255), "r": (230, 80, 80, 255),
         "o": (250, 160, 60, 255), "m": (120, 190, 90, 255), "n": (70, 130, 60, 255), "z": (40, 40, 48, 255),
         "c": (120, 220, 230, 255), "v": (170, 120, 220, 255), "d": (40, 80, 150, 255)}

ICONEN = {
    "vormstaf": [
        "...........pp...",
        "..........pwwp..",
        ".........pwppwp.",
        "..........pqqp..",
        "...........pp...",
        "..........ly....",
        ".........lh.....",
        "........lh......",
        ".......lh.......",
        "......lh........",
        ".....lh.........",
        "....lh..........",
        "...yh...........",
        "..lh............",
        ".kh.............",
        "................"],
    "vervangkwast": [
        "...........wwb..",
        "..........wwwbb.",
        ".........wwwbbb.",
        "..........iwbb..",
        ".........iii....",
        "........lh......",
        ".......lh.......",
        "......lh........",
        ".....lh.........",
        "....lh..........",
        "...lh...........",
        "..lh............",
        ".kh.............",
        "................",
        "................",
        "................"],
    "mengkwast": [
        "...........rry..",
        "..........rooyy.",
        ".........pomcbb.",
        "..........imcb..",
        ".........iii....",
        "........lh......",
        ".......lh.......",
        "......lh........",
        ".....lh.........",
        "....lh..........",
        "...lh...........",
        "..lh............",
        ".kh.............",
        "................",
        "................",
        "................"],
    "patroonkwast": [
        "...........zwz..",
        "..........wzwzw.",
        ".........zwzwzw.",
        "..........iwzw..",
        ".........iii....",
        "........lh......",
        ".......lh.......",
        "......lh........",
        ".....lh.........",
        "....lh..........",
        "...lh...........",
        "..lh............",
        ".kh.............",
        "................",
        "................",
        "................"],
    "verweerkwast": [
        "...........mnm..",
        "..........nmmnm.",
        ".........mnmgnm.",
        "..........imnn..",
        ".........iii....",
        "........lh......",
        ".......lh.......",
        "......lh........",
        ".....lh.........",
        "....lh..........",
        "...lh...........",
        "..lh............",
        ".kh.............",
        "................",
        "................",
        "................"],
    "egaliseerder": [
        "................",
        "......ws........",
        ".....wssw.......",
        "......ws........",
        ".......h........",
        ".......h........",
        ".......h........",
        ".......h........",
        "...iiiiiiiiii...",
        "..iggggggggggi..",
        "..igwwggggwwgi..",
        "..iggggggggggi..",
        "...iiiiiiiiii...",
        "................",
        "....mmmmmmmm....",
        "...nnnnnnnnnn..."],
    "verfmachine": [
        "................",
        ".....kk.........",
        "....kiik..r.....",
        "....kiik.r.p....",
        "...pppppp..v.c..",
        "...pwwwwp.y.....",
        "...pwpqwp..o....",
        "...pwqpwp.......",
        "...pwwwwp.......",
        "...pqqqqp.......",
        "...pqqqqp.......",
        "...pqqqqp.......",
        "...pppppp.......",
        "....qqqq........",
        "................",
        "................"],
    "tunnelboor": [
        "................",
        "................",
        "..kk............",
        ".kyyk...........",
        ".kyyiiiiii......",
        ".kyyiggggiii....",
        ".kyyiggggiiicc..",
        ".kyyiggggiiiccc.",
        ".kyyiggggiiicc..",
        ".kyyiiiiiii.....",
        ".kyyk...........",
        "..kkh...........",
        "...hh...........",
        "...hh...........",
        "...kk...........",
        "................"],
    "blauwdruk": [
        "................",
        ".dddddddddddd...",
        ".dbbbbbbbbbbbd..",
        ".dbsbbbbbbbsbd..",
        ".dbsssssbbbsbd..",
        ".dbsbbbsbbbsbd..",
        ".dbsbbbsssssbd..",
        ".dbsbbbbbbbsbd..",
        ".dbsssssssssbd..",
        ".dbbbbbbbbbbbd..",
        ".dbbpbbbbbpbbd..",
        ".dbbbppppbbbbd..",
        ".dbbbbbbbbbbbd..",
        "..ddddddddddddd.",
        "................",
        "................"],
    "ontwerpbureau": [
        "................",
        "................",
        "................",
        "....dddddd......",
        "...dbsbsbbd.....",
        "..hhhhhhhhhhhh..",
        ".hllllllllllllh.",
        ".hhhhhhhhhhhhhh.",
        "..hh........hh..",
        "..hh........hh..",
        "..hh........hh..",
        "..hh........hh..",
        "..hh........hh..",
        "..kk........kk..",
        "................",
        "................"],
}


def icoon(rijen):
    img = Image.new("RGBA", (16, 16))
    for y, rij in enumerate(rijen):
        for x, ch in enumerate(rij):
            if ch in PALET:
                img.putpixel((x, y), PALET[ch])
    return img


def bureau_top():
    """The desk's top: birch wood with a blue sheet on it (white lines, a pink guh heart)."""
    img = Image.new("RGBA", (16, 16), (226, 206, 150, 255))
    for y in range(16):
        for x in range(16):
            if (x + y * 3) % 7 == 0:
                img.putpixel((x, y), (210, 190, 134, 255))
    for y in range(2, 13):
        for x in range(2, 14):
            img.putpixel((x, y), (78, 128, 196, 255))
    for x in range(3, 13):
        img.putpixel((x, 4), (190, 220, 240, 255))
        img.putpixel((x, 10), (190, 220, 240, 255))
    for y in range(4, 11):
        img.putpixel((3, y), (190, 220, 240, 255))
        img.putpixel((12, y), (190, 220, 240, 255))
    for (x, y) in [(6, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8)]:
        img.putpixel((x, y), (242, 140, 186, 255))
    for (x, y) in [(13, 13), (14, 12), (12, 14)]:
        img.putpixel((x, y), (70, 40, 30, 255))
    return img


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def _recept(patroon, sleutel, resultaat):
    return {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": patroon, "key": sleutel,
            "result": {"id": resultaat, "count": 1}}


def _los(ingredienten, resultaat):
    return {"type": "minecraft:crafting_shapeless", "category": "equipment", "ingredients": ingredienten,
            "result": {"id": resultaat, "count": 1}}


def bestanden():
    A, D = "assets/guhs", "data/guhs"
    uit = {}
    for naam in ITEMS:
        uit[f"{A}/textures/item/{naam}.png"] = icoon(ICONEN[naam])
        uit[f"{A}/items/{naam}.json"] = {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}}
        uit[f"{A}/models/item/{naam}.json"] = {"parent": "minecraft:item/handheld" if naam != "blauwdruk" else "minecraft:item/generated",
                                               "textures": {"layer0": f"guhs:item/{naam}"}}
    # the desk: a top with a blue sheet, four legs
    uit[f"{A}/textures/block/ontwerpbureau_top.png"] = bureau_top()
    uit[f"{A}/textures/item/ontwerpbureau.png"] = icoon(ICONEN["ontwerpbureau"])
    hout = "minecraft:block/stripped_birch_log"
    poten = [{"from": [x, 0, z], "to": [x + 2, 12, z + 2], "faces": {f: {"texture": "#hout"} for f in ("north", "east", "south", "west", "down")}}
             for x in (1, 13) for z in (1, 13)]
    uit[f"{A}/models/block/ontwerpbureau.json"] = {
        "parent": "minecraft:block/block", "textures": {"particle": "minecraft:block/birch_planks", "top": "guhs:block/ontwerpbureau_top",
                                                        "rand": "minecraft:block/birch_planks", "hout": hout},
        "elements": [{"from": [0, 12, 0], "to": [16, 16, 16], "faces": {
            "up": {"texture": "#top"}, "down": {"texture": "#rand"},
            **{f: {"texture": "#rand", "uv": [0, 0, 16, 4]} for f in ("north", "east", "south", "west")}}}] + poten}
    uit[f"{A}/blockstates/ontwerpbureau.json"] = {"variants": {f"facing={f}": {"model": "guhs:block/ontwerpbureau", "y": r} if r else
                                                               {"model": "guhs:block/ontwerpbureau"} for f, r in ROT.items()}}
    uit[f"{A}/items/ontwerpbureau.json"] = {"model": {"type": "minecraft:model", "model": "guhs:item/ontwerpbureau"}}
    uit[f"{A}/models/item/ontwerpbureau.json"] = {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/ontwerpbureau"}}
    uit[f"{D}/loot_table/blocks/ontwerpbureau.json"] = {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": "guhs:ontwerpbureau"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
    # recipes (design 2.1: everything just craftable; the level tools only work from their keet level)
    uit[f"{D}/recipe/vormstaf.json"] = _recept(["  A", " S ", "S  "], {"A": "minecraft:amethyst_shard", "S": "minecraft:stick"}, "guhs:vormstaf")
    uit[f"{D}/recipe/vervangkwast.json"] = _los(["minecraft:stick", "minecraft:white_wool"], "guhs:vervangkwast")
    uit[f"{D}/recipe/mengkwast.json"] = _los(["minecraft:stick", "minecraft:white_wool", "minecraft:red_dye", "minecraft:blue_dye"], "guhs:mengkwast")
    uit[f"{D}/recipe/patroonkwast.json"] = _los(["minecraft:stick", "minecraft:white_wool", "minecraft:black_dye", "minecraft:white_dye"],
                                                "guhs:patroonkwast")
    uit[f"{D}/recipe/verweerkwast.json"] = _los(["minecraft:stick", "minecraft:white_wool", "minecraft:moss_block"], "guhs:verweerkwast")
    uit[f"{D}/recipe/egaliseerder.json"] = _recept(["IZI", " S ", " S "], {"I": "minecraft:iron_ingot", "Z": "guhs:zoutkristal",
                                                                           "S": "minecraft:stick"}, "guhs:egaliseerder")
    uit[f"{D}/recipe/verfmachine.json"] = _recept(["RYB", "IKI", " I "], {"R": "minecraft:red_dye", "Y": "minecraft:yellow_dye",
                                                                          "B": "minecraft:blue_dye", "I": "minecraft:iron_ingot",
                                                                          "K": "minecraft:bucket"}, "guhs:verfmachine")
    uit[f"{D}/recipe/tunnelboor.json"] = _recept(["DI ", "IRI", " IP"], {"D": "minecraft:diamond", "I": "minecraft:iron_ingot",
                                                                         "R": "minecraft:redstone", "P": "minecraft:piston"}, "guhs:tunnelboor")
    uit[f"{D}/recipe/ontwerpbureau.json"] = _recept(["PBP", "WWW", "S S"], {"P": "minecraft:paper", "B": "minecraft:blue_dye",
                                                                            "W": "#minecraft:planks", "S": "minecraft:stick"}, "guhs:ontwerpbureau")
    return uit


TAGS = [("data/minecraft/tags/block/mineable/axe.json", ["guhs:ontwerpbureau"])]


def _tag(root, pad, waarden):
    vol = os.path.join(root, *pad.split("/"))
    data = {"replace": False, "values": []}
    if os.path.exists(vol):
        with open(vol, encoding="utf-8") as f:
            data = json.load(f)
    for v in waarden:
        if v not in data["values"]:
            data["values"].append(v)
    os.makedirs(os.path.dirname(vol), exist_ok=True)
    with open(vol, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def build(h):
    for pad, obj in bestanden().items():
        if isinstance(obj, Image.Image):
            vol = os.path.join("src", "main", "resources", *pad.split("/"))
            os.makedirs(os.path.dirname(vol), exist_ok=True)
            obj.save(vol)
        else:
            base = h.A if pad.startswith("assets/") else h.D
            h.w(base + pad[pad.index("/", pad.index("/") + 1):], obj)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:ontwerpbureau"])
    for k, v in TEKSTEN.items():
        h.NL[k] = v


def _zelf():
    root = os.path.join("src", "main", "resources")
    alles = bestanden()
    for pad, obj in alles.items():
        vol = os.path.join(root, *pad.split("/"))
        os.makedirs(os.path.dirname(vol), exist_ok=True)
        if isinstance(obj, Image.Image):
            obj.save(vol)
        else:
            with open(vol, "w", encoding="utf-8", newline="\n") as f:
                json.dump(obj, f, indent=2, ensure_ascii=False)
                f.write("\n")
    for pad, waarden in TAGS:
        _tag(root, pad, waarden)
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
    print(f"bouwhulp_gereedschap: {len(alles)} bestanden, {len(TEKSTEN)} teksten"
          + (f", {len(zonder)} zonder Engels: {zonder[:5]}" if zonder else ""))


if __name__ == "__main__":
    _zelf()
