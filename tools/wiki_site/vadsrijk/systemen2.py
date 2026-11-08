"""
U-SITE: the systems of the night round (1.10.0) on guhs.nl/vadsrijk: Ambachten (t-ambacht), Puzzels (t-puzzels), the Kledingkast
(t-kleding), Karakters + spelmodi (t-karakter) and Uitrusting 2.0 (t-items). Data where the game has it (bron.py: recipes, puzzles,
cosmetics), otherwise short player-facing prose from the slices' cheat sheets. Never a puzzle solution or a secret's spot.
"""
from .pages import ESC, t, p, feiten, spoiler, zbadge

KAARTEN = [
    ("ambachten.html", "Crafts", "Ambachten", "Gathering, cooking, brewing, smithing and sewing: 9 crafts up to level 50.",
     "Verzamelen, koken, brouwen, smeden en naaien: 9 ambachten tot niveau 50."),
    ("uitrusting.html", "Gear 2.0", "Uitrusting 2.0", "New slots, clear stats, heirlooms and the smith's safety nets.",
     "Nieuwe vakken, duidelijke stats, erfstukken en de vangnetten van de smid."),
    ("karakters.html", "Characters and game modes", "Karakters en spelmodi", "Up to 3 knights per account; Eénvads and Knuffelmodus.",
     "Tot 3 ridders per account; Eénvads en Knuffelmodus."),
    ("kledingkast.html", "Wardrobe", "Kledingkast", "Wings, capes, hats, name colours and cheer-up effects.",
     "Vleugels, capes, hoofddingen, naamkleuren en opvrolijk-effecten."),
    ("puzzels.html", "Puzzles and secrets", "Puzzels en geheimpjes", "22 puzzles near the campfires, each with a reward.",
     "22 puzzels bij de kampvuren, elk met een beloning."),
]

SOORT_KLEDING = {"vleugel": ("Wings", "Vleugels"), "kaap": ("Capes", "Capes"), "accessoire": ("Head things", "Hoofddingen"),
                 "naamkleur": ("Name colours", "Naamkleuren"), "verf": ("Paint (wings + head thing)", "Verf (vleugels + hoofdding)"),
                 "plof": ("Cheer-up effects", "Opvrolijk-effecten"), "spoor": ("Trails", "Sporen"), "aura": ("Auras", "Aura's")}
AMBACHT_EN = {"mijnen": "Mining", "houthakken": "Woodcutting", "plukken": "Picking", "vissen": "Fishing", "imkeren": "Beekeeping",
              "smeden": "Smithing", "naaien": "Sewing", "koken": "Cooking", "brouwen": "Brewing"}
UIT_EN = {"eten": ("Dish", "Gerecht"), "drank": ("Potion", "Drankje"), "gear": ("Maatwerk gear", "Maatwerk"), "meester": ("Masterwork jewellery", "Meesterwerk"),
          "slijp": ("Cut crystals", "Kristal slijpen"), "kern": ("Boss core", "Bazenkern"), "post": ("Decoration (sent home)", "Versiering (naar huis)")}


def tabel(koppen, rijen, klasse="data"):
    return ('<div class="tscroll"><table class="' + klasse + '"><thead><tr>' + "".join(f"<th{' class=num' if n else ''}>{k}</th>" for k, n in koppen)
            + "</tr></thead><tbody>" + "".join(rijen) + "</tbody></table></div>")


def alles(b):
    ambachten(b)
    uitrusting(b)
    karakters(b)
    kledingkast(b)
    puzzels(b)


def ambachten(b):
    P = "systemen/ambachten.html"
    a = b.d.get("ambachten") or {}
    namen = a.get("namen", {})
    verzamel = [k for k in ("mijnen", "houthakken", "plukken", "vissen", "imkeren") if k in namen]
    maak = [k for k in ("koken", "brouwen", "smeden", "naaien") if k in namen]
    # gathering spots per zone (names only; the game shows the spots themselves)
    plek_rijen = []
    for k in verzamel:
        per = a.get("plekken", {}).get(k, {})
        for z in sorted(per, key=int):
            plek_rijen.append(f'<tr data-zone="{z}"><td>{t(AMBACHT_EN.get(k, k), namen[k])}</td><td>{zbadge("zone" + str(z), b.zone_naam("zone" + str(z)))}</td>'
                              f'<td>{ESC(", ".join(per[z]))}</td></tr>')
    rec = {}
    for r in a.get("recepten", []):
        rec.setdefault(r["ambacht"], []).append(r)
    blok = ""
    for k in maak:
        rijen = [f'<tr data-zone="{b.tekst_zone(r["naam"], " ".join(x for _, x in r["in_"]))}"><td class="num">{r["level"]}</td><td>{ESC(r["naam"])}</td><td>{t(*UIT_EN.get(r["soort"], (r["soort"], r["soort"])))}</td>'
                 f'<td class="num">{r["tier"]}</td><td>{ESC(", ".join(f"{n}x {x}" for n, x in r["in_"]) or "-")}</td></tr>' for r in rec.get(k, [])]
        blok += (f'<h3 id="{k}">{t(AMBACHT_EN.get(k, k), namen[k])} <small class="muted">({len(rijen)})</small></h3>'
                 + tabel([(t("Level", "Niveau"), 1), (t("Recipe", "Recept"), 0), (t("Makes", "Maakt"), 0), ("Tier", 1), (t("Needs", "Nodig"), 0)], rijen))
    main = f"""<h1>{t("Crafts", "Ambachten")}</h1>
<p class="lead">{t("Nine crafts, each up to level 50, with their own book (key Y). Gathering gives materials; the workshops in the towns turn them into food, potions and gear.",
                   "Negen ambachten, elk tot niveau 50, met een eigen boek (toets Y). Verzamelen geeft materialen; de werkplaatsen in de steden maken er eten, drankjes en uitrusting van.")}</p>
<div class="tw"><div>
<h2>{t("Gathering", "Verzamelen")}</h2>
{p("Gathering spots only show up for YOU (every player has their own spots). Look at one and right-click: a little game of 2-4 seconds. Perfect = extra vadsig, good = vadsig, doing nothing = normal. Sneak + right-click harvests at once. A used spot comes back elsewhere after 1-2 minutes.",
   "Verzamelplekken zie alleen JIJ (elke speler heeft zijn eigen plekken). Kijk ernaar en rechtsklik: een spelletje van 2-4 seconden. Perfect = extra vadsig, goed = vadsig, niks doen = gewoon. Sluipen + rechtsklik oogst meteen. Een geoogste plek komt na 1-2 minuten ergens anders terug.")}
<ul>
<li>{t("Picking: hold right-click, let go in the green part.", "Plukken: houd rechtsklik vast, laat los in het groene stuk.")}</li>
<li>{t("Mining: tap three times on the pink.", "Mijnen: drie keer tikken op het roze.")}</li>
<li>{t("Woodcutting: tap twice, quickly.", "Houthakken: twee keer snel tikken.")}</li>
<li>{t("Fishing: wait for BITE! and click at once. Other fish bite at night and in the rain.", "Vissen: wacht op BEET! en klik meteen. 's Nachts en bij regen bijten andere soorten.")}</li>
<li>{t("Beekeeping (zone 3): place your hive near the flowers; combs fill in real time. Harvest with smoke: too early = grumpy bees (sticky, never damage). More hives at level 15, 30 and 45.",
       "Imkeren (zone 3): zet je korf bij de bloemen; de raten vullen in echte tijd. Oogsten met rook: te vroeg = chagrijnige bijen (plakkerig, nooit schade). Meer korven op niveau 15, 30 en 45.")}</li>
<li>{t("A golden, glittering spot is rare: triple harvest and a chance on something special.", "Een gouden, glinsterende plek is zeldzaam: driedubbele oogst en kans op iets bijzonders.")}</li>
<li>{t("Rested: offline time builds up double craft xp (max 2 hours).", "Uitgerust: offline bouw je dubbele ambachts-xp op (max 2 uur).")}</li>
</ul>
<h3>{t("Where", "Waar")}</h3>{tabel([(t("Craft", "Ambacht"), 0), ("Zone", 0), (t("Spots", "Plekken"), 0)], plek_rijen)}
<h2>{t("Workshops", "Werkplaatsen")}</h2>
{p("The master guhs stand in a ring around the town squares (first in Zwamzoet). Click one: Workshop, Recipe book, Tell me... (a little story in 5 steps, with a keepsake sent home at the end).",
   "De leermeester-guhs staan in een ring rond de stadspleinen (eerst in Zwamzoet). Klik er een: Werkplaats, Receptenboek, Vertel eens... (een verhaaltje in 5 stapjes, met aan het eind een aandenken met de Post naar huis).")}
<div data-zone="5">{p("Every town has them: Zwamzoet, Kandijkom, Honingtak, Lampionhaven and the guild in Hoogvads.", "Elke stad heeft ze: Zwamzoet, Kandijkom, Honingtak, Lampionhaven en het gilde in Hoogvads.")}</div>
<ul>
<li>{t("Making something is a little timing game: stop the bar in the pink for a higher Vadsigheid (better food, stronger potions). Make 10x skips the game (always 60%).",
       "Iets maken is een timingspelletje: stop het streepje in het roze voor een hogere Vadsigheid (beter eten, sterkere drankjes). Maak 10x slaat het spelletje over (altijd 60%).")}</li>
<li>{t("Food: one buff at a time, 30 minutes, it stays when you faint. A feast gives the whole party nearby the buff plus Gezellig.",
       "Eten: één buff tegelijk, 30 minuten, hij blijft bij flauwvallen. Een feestmaal geeft de hele party in de buurt de buff plus Gezellig.")}</li>
<li>{t("Maatwerk (smithing + sewing): choose 2 bonuses yourself; you get an Epic piece with at least one socket. It does not count for a set bonus.",
       "Maatwerk (smeden + naaien): kies zelf 2 bonussen; je krijgt een Episch stuk met minstens één sokje. Het telt niet voor een setbonus.")}</li>
<li>{t("Masterwork jewellery: 3 bonuses + 1 property (e.g. Damage against bosses +8%), needs a boss core. Boss cores: 15% per boss per helper, or 5 core splinters from elites.",
       "Meesterwerk-sieraden: 3 bonussen + 1 eigenschap (bijv. Schade tegen bazen +8%), met een bazenkern. Bazenkernen: 15% per baas per helper, of 5 kernsplinters van elites.")}</li>
<li>{t("Order board: 3 orders a day (new at 06:00); 12 in a week = the Craft chest.", "Bestelbord: 3 bestellingen per dag (nieuw om 06:00); 12 in een week = de Ambachtskist.")}</li>
<li>{t("Titles: Apprentice at level 25, Journeyman at 50, Ambachtskenner for the whole book. Highscores per craft on the Highscores page.",
       "Titels: Leerling op niveau 25, Gezel op 50, Ambachtskenner voor het hele boek. Ranglijsten per ambacht op de Highscores-pagina.")}</li>
</ul>
<h2>{t("Recipes", "Recepten")}</h2>{blok or '<p class="leeg">-</p>'}
</div><aside class="zij"><div class="kaartje">{feiten([(t("Key", "Toets"), "<kbd>Y</kbd>"), (t("Crafts", "Ambachten"), str(len(namen))),
                                                    (t("Max level", "Max niveau"), "50"), (t("Recipes", "Recepten"), str(len(a.get("recepten", []))))])}</div></aside></div>"""
    b.schrijf(P, "Ambachten", "Crafts", main, "systemen/index.html", beschrijving="De 9 ambachten van Het Vadsrijk: verzamelen, koken, brouwen, smeden, naaien.")


def uitrusting(b):
    P = "systemen/uitrusting.html"
    main = f"""<h1>{t("Gear 2.0", "Uitrusting 2.0")}</h1>
<h2>{t("New slots", "Nieuwe vakken")}</h2>
{p("Next to your inventory (E) is a column of 7 slots: Ring, Ring 2, Necklace, Earring, Bracelet, Gloves, Cape. Shift-click a piece or right-click it in your hand. They only count inside the Vadsrijk. Bracelets/earrings give 70% of a ring's numbers, gloves and capes 60%. Set gloves and capes count as a set piece.",
   "Naast je inventaris (E) staat een kolom van 7 vakjes: Ring, Ring 2, Ketting, Oorbel, Armband, Handschoenen, Cape. Shift+klik een stuk of rechtsklik het in je hand. Ze tellen alleen in het Vadsrijk. Armbanden/oorbellen geven 70% van een ring, handschoenen en capes 60%. Set-handschoenen en -capes tellen als setstuk.")}
<h2>{t("Clear stats + 4 new ones", "Duidelijke stats + 4 nieuwe")}</h2>
{p("Every line on gear reads like plain Dutch, e.g. +3 Kracht (schade van dichtbij). New: Ignore defence % (max 50%), Damage against bosses %, Vads per hit, and Chance on effect % (your element puts its status: fire = Burning, nature = Spored, water = Sticky, pink = briefly Stunned; max 40%). They only roll on weapons, jewellery, gloves and capes.",
   "Elke regel op uitrusting is gewoon te lezen, bijv. +3 Kracht (schade van dichtbij). Nieuw: Verdediging negeren % (max 50%), Schade tegen bazen %, Leven per treffer, en Kans op effect % (je element zet zijn status: vuur = Brandend, natuur = Gesporen, water = Plakkerig, roze = kort Verdoofd; max 40%). Ze vallen alleen op wapens, sieraden, handschoenen en capes.")}
<h2>{t("The smith", "De smid")}</h2>
<ul>
<li>{t("Tabs: Stars, Bonuses, Grow, Overvads. Every star shows its chance, risk, price and what changes.", "Tabs: Sterren, Bonussen, Groeien, Overvadsen. Elke ster toont de kans, het risico, de prijs en wat er verandert.")}</li>
<li>{t("Patience: every failed star gives +5% on the next (up to star 7).", "Geduld: elke mislukte ster geeft +5% op de volgende (tot ster 7).")}</li>
<li>{t("Knuffeltalisman: protects a star once when it would be lost.", "Knuffeltalisman: beschermt één keer een ster die je anders kwijt zou raken.")}</li>
<li>{t("Polish: a new value for one bonus within its range (it can also go down!).", "Polijsten: een nieuwe waarde voor één bonus binnen zijn bereik (kan ook lager!).")}</li>
</ul>
<h2>{t("Heirlooms", "Erfstukken")}</h2>
<ul>
<li>{t("Trusted: gear you wear earns item xp (elite 3, boss 15) up to Trusted 5, with a little more base stats per level.", "Vertrouwd: wat je draagt krijgt item-xp (elite 3, baas 15) tot Vertrouwd 5, met per niveau wat meer basisstats.")}</li>
<li>{t("Grow: a Trusted 5 Legendary grows 10 levels (twice), a Guhddelijk always (up to level 50). Groeipluis drops from every boss.", "Doorgroeien: een Legendarisch stuk op Vertrouwd 5 groeit 10 levels (2x), een Guhddelijk altijd (tot level 50). Groeipluis valt bij elke baas.")}</li>
<li>{t("Overvads: move one bonus from an old piece onto your favourite (the old piece is used up; 15% chance on a slightly lower value).", "Overvadsen: zet één bonus van een oud stuk op je lievelingsstuk (het oude stuk verdwijnt; 15% kans op een iets lagere waarde).")}</li>
</ul>
<h2>{t("Potion belt", "Drankjesriem")}</h2>
{p("Three belt slots for potions: Alt+1 / Alt+2 / Alt+3 drinks one without switching your hotbar.", "Drie riemvakjes voor drankjes: Alt+1 / Alt+2 / Alt+3 drinkt er een zonder van hotbarvakje te wisselen.")}
<p><a href="../items/index.html">{t("Item database", "Itemdatabase")}</a> &middot; <a href="sets.html">Sets</a> &middot; <a href="kristallen.html">{t("Rarities, stars and crystals", "Zeldzaamheid, sterren en kristallen")}</a></p>"""
    b.schrijf(P, "Uitrusting 2.0", "Gear 2.0", main, "systemen/index.html", beschrijving="Nieuwe vakken, stats, erfstukken en de smid in Het Vadsrijk.")


def karakters(b):
    P = "systemen/karakters.html"
    main = f"""<h1>{t("Characters and game modes", "Karakters en spelmodi")}</h1>
{p("You can have up to 3 knights per account. Each knight has their own levels, class levels, skill trees, Vadsrijk inventory, quests, story films, Exploration book, campfire and Vadsmunten.",
   "Je hebt tot 3 ridders per account. Elke ridder heeft eigen levels, klasse-levels, skillbomen, Vadsrijk-inventaris, quests, verhaalfilmpjes, Verkenningsboek, kampvuur en Vadsmunten.")}
{p("Shared by all your knights: mounts (with their levels), pets, titles, the Wardrobe, the bank and the material bag.",
   "Gedeeld door al je ridders: rijdieren (met hun levels), huisdieren, titels, de Kledingkast, de bank en de materiaaltas.")}
<h2>{t("Switching", "Wisselen")}</h2>
{p("Talk to the Grootmeester (order temple in Zwamzoet), the Rekruteerder (Knuffelkamp) or a camp guard at a campfire: “I want to play another knight”. Switching works in a town, within 8 blocks of a campfire, or in the first minute after you arrive; not in a boss fight.",
   "Praat met de Grootmeester (ordetempel Zwamzoet), de Rekruteerder (Knuffelkamp) of een kampwacht bij een kampvuur: “Ik wil een andere ridder spelen”. Wisselen kan in een stad, binnen 8 blokken van een kampvuur, of in de eerste minuut na binnenkomst; niet in een baasgevecht.")}
<h2>{t("Game modes", "Spelmodi")}</h2>
<div class="grid">
<div class="kaartje"><h3>{t("Normal", "Normaal")}</h3><p>{t("As always.", "Zoals altijd.")}</p></div>
<div class="kaartje"><h3>Eénvads &#10084;</h3><p>{t("One life: faint OUTSIDE a town and your knight retires. Not deleted: a statue in the Rustende Ridders hall in Zwamzoet, then plays on as Normal with a golden star. While alive: +10% xp.",
  "Eén leven: flauwvallen BUITEN een stad = je ridder gaat met pensioen. Niet gewist: een standbeeld in de Rustende Ridders-zaal in Zwamzoet, daarna speelt hij verder als Normaal met een gouden sterretje. Zolang hij leeft: +10% xp.")}</p></div>
<div class="kaartje"><h3>{t("Cuddle mode", "Knuffelmodus")} &#9729;</h3><p>{t("Creatures do half damage to you; you get 10% less xp.", "Monsters doen maar de helft schade bij jou; je krijgt 10% minder xp.")}</p></div>
</div>
{p("You choose the mode when you make the knight; later it can only become EASIER.", "Je kiest de modus bij het maken; later kan het alleen MAKKELIJKER.")}"""
    b.schrijf(P, "Karakters en spelmodi", "Characters and game modes", main, "systemen/index.html", beschrijving="Tot 3 ridders per account en de spelmodi van Het Vadsrijk.")


def kledingkast(b):
    P = "systemen/kledingkast.html"
    k = b.d.get("kleding") or {}
    per = {}
    for d in k.get("dingen", []):
        per.setdefault(d["soort"], []).append(d)
    blok = ""
    for s, (en, nl) in SOORT_KLEDING.items():
        if s not in per:
            continue
        rijen = [f'<tr data-zone="{b.tekst_zone(d["naam"], d["bron"] or "")}"><td>{ESC(d["naam"])}</td><td>{spoiler("<p>" + ESC(d["bron"]) + "</p>", "How to get it", "Hoe krijg je het") if d["bron"] else ""}</td></tr>' for d in per[s]]
        blok += f'<h3 id="{s}">{t(en, nl)} <small class="muted">({len(rijen)})</small></h3>' + tabel([(t("Name", "Naam"), 0), (t("How", "Hoe"), 0)], rijen)
    kr = "".join(f"<li>{ESC(v)}</li>" for kk, v in k.get("krachten", {}).items() if kk != "alleen_rijk")
    main = f"""<h1>{t("Wardrobe", "Kledingkast")}</h1>
<p class="lead">{t("Key O (everywhere, also at home), or a tailor. Everything is per ACCOUNT: what you unlock stays for all your knights. Looks only: no combat stats.",
                   "Toets O (overal, ook thuis), of bij een kleermaker. Alles is per ACCOUNT: wat je ontgrendelt blijft voor al je ridders. Alleen uiterlijk: geen gevechtsstats.")}</p>
<h2>{t("Wings and gliding", "Vleugels en zweven")}</h2>
{p("In the Vadsrijk: jump off something high and HOLD space: you sink very slowly (never up), no fall damage. Not in boss arenas and dungeons. Every pair of wings has one movement trick:",
   "In het Vadsrijk: spring van iets hoogs en HOUD spatie ingedrukt: je zakt heel langzaam (nooit omhoog), geen valschade. Niet in bazen-arena's en kerkers. Elk paar vleugels heeft één beweegtrucje:")}
<ul>{kr}</ul>
{p("Armour looks (transmog): every Vadsrijk armour you ever had becomes a look, per piece, only visible inside the Vadsrijk. The effect sliders (others / own: all, less, off) sit at the bottom left.",
   "Harnas-looks (transmog): elk Vadsrijk-harnas dat je ooit had wordt een uiterlijk, per stuk, alleen zichtbaar in het Vadsrijk. De schuifjes voor effecten (anderen / eigen: alle, minder, uit) staan links onder.")}
<h2>{t("Everything in the wardrobe", "Alles in de kledingkast")}</h2>{blok}"""
    b.schrijf(P, "Kledingkast", "Wardrobe", main, "systemen/index.html", beschrijving="De Kledingkast van Het Vadsrijk: vleugels, capes, hoofddingen en effecten.")


def puzzels(b):
    P = "systemen/puzzels.html"
    pz = b.d.get("puzzels") or {}
    soorten = pz.get("soorten", {})
    lijst = pz.get("lijst", [])
    delen = ""
    for z in b.d["zones"]:
        hier = [x for x in lijst if x["zone"] == z["id"]]
        if not hier:
            continue
        geheim = sum(1 for x in hier if x["geheim"])
        rijen = []
        for x in hier:
            if x["geheim"]:
                continue
            bel = ", ".join([f'{x["munten"] / 100:g} {t("silver", "zilver")}'] if x["munten"] else []) + ((", " if x["munten"] else "") + ESC(x["ontgrendel"]) if x["ontgrendel"] else "")
            extra = (" " + t("(at night)", "('s nachts)") if x["nacht"] else "") + (f' ({x["tijd"]} s)' if x.get("tijd") else "")
            rijen.append(f'<tr><td>{ESC(x["naam"])}</td><td>{ESC(soorten.get(x["soort"], x["soort"]))}{extra}</td><td>{ESC(x["uitleg"])}</td><td>{bel}</td></tr>')
        delen += (f'<section data-zone="{z["nr"]}"><h2>{ESC(z["naam"])}</h2>'
                  + tabel([(t("Puzzle", "Puzzel"), 0), (t("Kind", "Soort"), 0), (t("What you see", "Wat je ziet"), 0), (t("Reward", "Beloning"), 0)], rijen)
                  + (f'<p class="muted">{t(f"Plus {geheim} hidden secret(s): find them yourself!", f"Plus {geheim} verstopte geheimpje(s): zoek ze zelf!")}</p>' if geheim else "")
                  + f'</section><div class="verstopt-zone" data-zone="{z["nr"]}">{t("Hidden by your spoiler setting.", "Verborgen door je spoilerinstelling.")}</div>')
    main = f"""<h1>{t("Puzzles and secrets", "Puzzels en geheimpjes")}</h1>
{p("A puzzle appears the first time you come within ~70 blocks of its spot, near a campfire or a landmark. At the bottom of your screen you always see what to do. After 3 wrong tries you get hint 1, after 6 hint 2. Solved: everyone nearby gets the reward, once per player. They are in the Exploration book (B) under Puzzels en geheimpjes and count for the zone %.",
   "Een puzzel verschijnt de eerste keer dat je binnen ~70 blokken van zijn plek komt, bij een kampvuur of een bezienswaardigheid. Onderaan je scherm staat steeds wat je moet doen. Na 3 foute pogingen krijg je tip 1, na 6 tip 2. Opgelost: iedereen in de buurt krijgt de beloning, één keer per speler. Ze staan in het Verkenningsboek (B) onder Puzzels en geheimpjes en tellen mee voor het zonepercentage.")}
<p class="note">{t("No solutions here: that is the fun. The 9 torn pages tell the legend of the Empty Bowl.", "Geen oplossingen hier: daar is de lol aan. De 9 gescheurde bladzijdes vertellen de legende van de Lege Schaal.")}</p>
{delen}"""
    b.schrijf(P, "Puzzels en geheimpjes", "Puzzles and secrets", main, "systemen/index.html", beschrijving="De puzzels en geheimpjes van Het Vadsrijk.")
