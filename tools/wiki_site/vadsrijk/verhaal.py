"""
U-SITE: the story page of guhs.nl/vadsrijk (verhaal.html): the long journey in chapters per part and zone, with illustrations,
character cards, a timeline of the world and the world map of De Knuffelorde.

Sources: guhs1100-avontuur/WERELDVERHAAL.md + ZONE6_PLAN.md (written out by hand below, spoiler-safe), the game's own lore prose
(b.d["lore"], from the npc wiki helpers) and pictures in docs/wiki/vadsrijk_verhaal/ (concept renders from renders-*, the world map
regenerated with zone 6 = Het Zijdeland; copied here as .webp). Boss renders (img/renders/<id>.webp) are used when the build made them.

Spoilers: every block carries data-zone="N" (the "toon tot zone X" switch hides it); twists (who the victims really are, the Keizer,
what the Vingers do) sit behind spoiler buttons as well. Zone 6 (Het Zijdeland) is in the game since round 3 of the preview: its
chapter, Ringeling and Moeder Bron carry data-zone="6" (Ringeling's ending in the raid is a spoiler).
"""
import os
import shutil

from .pages import ESC, t, p, spoiler

BRON = ("docs", "wiki", "vadsrijk_verhaal")

CSS = """<style>
.vh-hero{position:relative;border-radius:18px;overflow:hidden;margin:6px 0 18px;background:#2b1d33}
.vh-hero img{display:block;width:100%;height:auto}
.vh-hero figcaption{padding:8px 14px;color:#f3e6f0;font-size:13.5px}
.vh-delen{display:flex;flex-wrap:wrap;gap:8px;margin:10px 0 18px}
.vh-delen a{flex:1 1 150px;text-decoration:none;border-radius:14px;padding:10px 12px;color:#fff;font:700 14px/1.3 var(--body);
  background:var(--dk,#c2577f);box-shadow:0 2px 0 rgba(0,0,0,.15)}
.vh-delen a small{display:block;font-weight:400;opacity:.9}
.vh-delen a.later{background:#8d8892}
.vh-deel{margin:28px 0 8px;padding:14px 18px;border-radius:18px;color:#fff;background:linear-gradient(120deg,var(--dk,#c2577f),color-mix(in srgb,var(--dk,#c2577f) 55%,#2b1d33))}
.vh-deel h2{margin:0;color:#fff}.vh-deel p{margin:6px 0 0;opacity:.95}
.vh-hoofdstuk{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(0,1fr);gap:18px;align-items:start;margin:18px 0;padding:14px;border:1.5px solid var(--line2);
  border-radius:18px;background:var(--card)}
.vh-hoofdstuk:nth-of-type(even){grid-template-columns:minmax(0,1fr) minmax(0,1.1fr)}
.vh-hoofdstuk:nth-of-type(even) .vh-beeld{order:2}
.vh-beeld img{width:100%;height:auto;border-radius:12px;display:block}
.vh-beeld figcaption{font-size:12.5px;color:var(--muted);margin-top:4px}
.vh-hoofdstuk h3{margin:0 0 4px}
.vh-hoofdstuk .nr{display:inline-block;font:700 12px/1.6 var(--body);padding:0 9px;border-radius:999px;background:var(--zk,#c2577f);color:#fff;margin-bottom:6px}
.vh-hoofdstuk details.meer{margin-top:8px}.vh-hoofdstuk details.meer>summary{cursor:pointer;font-weight:700;color:var(--vr)}
.vh-komt{border-style:dashed;opacity:.92}
.vh-kaarten{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:14px;margin:12px 0}
.vh-kaart{border:1.5px solid var(--line2);border-radius:16px;background:var(--card);overflow:hidden;display:flex;flex-direction:column}
.vh-kaart .pic{height:150px;display:flex;align-items:center;justify-content:center;background:linear-gradient(160deg,#f6e3ee,#e3e4f8)}
:root[data-theme="dark"] .vh-kaart .pic{background:linear-gradient(160deg,#3a2a40,#2a2d44)}
.vh-kaart .pic img{max-width:100%;max-height:150px;object-fit:contain}
.vh-kaart .pic .sil{font:700 64px/1 var(--body);color:rgba(90,60,100,.35)}
.vh-kaart .tx{padding:10px 12px}.vh-kaart h4{margin:0}.vh-kaart .rol{font-size:12.5px;color:var(--muted);margin:2px 0 6px}
.vh-tijd{list-style:none;margin:12px 0;padding:0 0 0 22px;border-left:3px solid var(--vr)}
.vh-tijd li{position:relative;margin:0 0 14px;padding-left:6px}
.vh-tijd li::before{content:"";position:absolute;left:-31px;top:4px;width:14px;height:14px;border-radius:50%;background:var(--card);border:3px solid var(--vr)}
.vh-tijd b{display:block}.vh-tijd .wanneer{font:600 12px var(--mono);color:var(--muted)}
.vh-trappen{display:flex;flex-wrap:wrap;gap:6px;margin:8px 0}
.vh-trappen span{flex:1 1 120px;border-radius:12px;padding:8px 10px;font-size:13.5px;color:#fff}
@media (max-width:760px){.vh-hoofdstuk,.vh-hoofdstuk:nth-of-type(even){grid-template-columns:1fr}.vh-hoofdstuk:nth-of-type(even) .vh-beeld{order:0}}
</style>"""

# (part nr, en, nl, level range, colour, zone nrs on the site, the part's "victim" boss id)
DELEN = [
    (1, "Part I: The Dragon", "Deel I: De Draak", "lv 1-50", "#e0559a", (1, 2, 3)),
    (2, "Part II: The Floating City", "Deel II: De Zwevende Stad", "lv 50-70", "#d9a03a", (4, 5, 6)),
]
LATER = [("Part III: The Soot Front", "Deel III: Het Roetfront", "lv 62-72"), ("Part IV: The Silent Land", "Deel IV: Het Stille Land", "lv 72-80"),
         ("Part V: The Occupied Lands", "Deel V: De Bezette Landen", "lv 80-90"), ("Part VI: The Grey Empire", "Deel VI: Het Grauwe Keizerrijk", "lv 90-100")]

# chapters: (zone nr, picture, caption en, caption nl, title en, title nl, levels, text en, text nl, twist en, twist nl)
HOOFDSTUKKEN = [
    (1, "knuffelkamp", "Knuffelkamp, the training island", "Knuffelkamp, het oefeneiland",
     "Knuffelkamp and Zwamzoet", "Knuffelkamp en Zwamzoet", "lv 1-20",
     "Behind an old portal in the Guhmension lies a whole other world. Its creatures have gone grey and grumpy, and De Knuffelorde is looking for recruits who cuddle them happy again: with a pillow, an arrow or a fireball, very lovingly. You start as a recruit on Knuffelkamp, then fly to Zwamzoet, the town of giant mushrooms, where klodders lie scattered over the fields and grey scars run through the land.",
     "Achter een oud portaal in de Guhmensie ligt een hele andere wereld. De wezens zijn er grauw en knorrig geworden, en De Knuffelorde zoekt rekruten die ze weer blij knuffelen: met een kussen, een pijl of een vuurbal, heel liefdevol. Je begint als rekruut op Knuffelkamp en vliegt dan naar Zwamzoet, de stad van de reuzenzwammen, waar klodders over de velden liggen en grauwe littekens door het land lopen.",
     "Someone is collecting the grey fluff that grumpy creatures leak. Sacks full of it. A Mika in a cheese disguise knows more than he says.",
     "Iemand verzamelt de grauwe pluis die knorrige wezens lekken. Zakken vol. Een Mika in kaasvermomming weet meer dan hij zegt."),
    (2, None, "", "", "The Splinter Peaks", "De Splinterpieken", "lv 20-30",
     "High up in the crystal mountains lies Kandijkom, a town of candy-coloured ice. The crystals have gone hard and grey, the Galmwolf howls over the slopes and in the Spiegelburcht Vrouwe Spiegelzucht only looks at herself.",
     "Hoog in de kristalbergen ligt Kandijkom, een stad van snoepkleurig ijs. De kristallen zijn hard en grauw geworden, de Galmwolf huilt over de hellingen en in de Spiegelburcht kijkt Vrouwe Spiegelzucht alleen nog naar zichzelf.",
     "Mika miners drill grey crystal out of the mountain and pull it away on sledges. It is not just any crystal: they are splinters of something that broke.",
     "Mika-mijnwerkers boren grauw kristal uit de berg en trekken het op sleeën weg. Het is niet zomaar kristal: het zijn splinters van iets dat gebroken is."),
    (3, "schemerwortel", "De Schemerwortel, the world tree", "De Schemerwortel, de wereldboom",
     "The Schemerwortel", "De Schemerwortel", "lv 30-50",
     "A tree so tall that a whole town lives in its branches: Honingtak, full of honey and lanterns. Above it the crown sea, below it the dark roots where mould grows. At the top lies an old nest. Something big used to live there.",
     "Een boom zo hoog dat er een hele stad in zijn takken woont: Honingtak, vol honing en lantaarns. Daarboven de kruinzee, daaronder de donkere wortels waar schimmel groeit. Bovenin ligt een oud nest. Daar woonde vroeger iets groots.",
     "The Mika's have built a cheese zeppelin yard on their own island, and a tiny count in a cape of mouse fur gives the orders. He calls himself the Pink: one of five.",
     "De Mika's hebben een kaaszeppelinwerf op een eigen eiland gebouwd, en een piepklein graafje in een cape van muizenpels deelt de bevelen uit. Hij noemt zich de Pink: een van de vijf."),
    (4, "lampionkust", "De Lampionkust, the island sea", "De Lampionkust, de eilandzee",
     "The Lantern Coast", "De Lampionkust", "lv 50-56",
     "A coast of little islands, kites and lanterns, with your own little sailing boat between them. Lampionhaven takes in the first refugees: thin, grey guhs who fled from the east. High above the sea floats a whole city.",
     "Een kust van eilandjes, vliegers en lampionnen, met je eigen zeilbootje ertussen. Lampionhaven vangt de eerste vluchtelingen op: magere, grauwe guhs die uit het oosten zijn gevlucht. Hoog boven de zee zweeft een hele stad.",
     "A Mika war kite steals the lanterns, and the refugees speak of a grey mist that creeps over the sea.",
     "Een Mika-oorlogsvlieger steelt de lampionnen, en de vluchtelingen vertellen over een grauwe mist die over de zee kruipt."),
    (5, "hoogvads", "Hoogvads on the back of Moeder Wolkwal", "Hoogvads op de rug van Moeder Wolkwal",
     "Hoogvads, the Floating City", "Hoogvads, de Zwevende Stad", "lv 56-60",
     "The floating castle you have seen on the horizon since zone 1 is no dragon castle: it is Hoogvads, the capital of the resistance, on the back of Moeder Wolkwal, a giant sky whale. Pink towers, cloud bridges, the cathedral of the Order, the Great Soup Kitchen and the High Council. Queen Zachtmoed the Third says little and cuddles a lot.",
     "Het zwevende kasteel dat je al vanaf zone 1 aan de horizon zag, is geen drakenkasteel: het is Hoogvads, de hoofdstad van het verzet, op de rug van Moeder Wolkwal, een reusachtige luchtwalvis. Roze torens, wolkenbruggen, de kathedraal van de Orde, de Grote Soepkeuken en de Hoge Raad. Koningin Zachtmoed de Derde zegt weinig en knuffelt veel.",
     "Because vads is light, the grey could never reach the whale. Until now: Moeder Wolkwal is having a bad dream, and in the Council Hall lies a torn page from the oldest books of the Order.",
     "Omdat vads licht is, kon de grauwheid de walvis nooit bereiken. Tot nu: Moeder Wolkwal heeft een nare droom, en in de Raadszaal ligt een gescheurde bladzijde uit de oudste boeken van de Orde."),
    # U-SITE round 3: zone 6 (u-z6verhaal's "De Sterrenkaravaan")
    (6, "zijdeland", "Rozenkloof, the canyon town of rose sandstone", "Rozenkloof, de kloofstad van roze zandsteen",
     "Het Zijdeland", "Het Zijdeland", "lv 60-70",
     "Moeder Wolkwal swims you east over the Cloud Sea to the pink cliffs, and from Hoogvads' Staarthaven airship line E flies to Rozenkloof: a town carved into a canyon of rose sandstone. Oma Dadel's caravan gives you a bultguh, Knoopje in Saffraanpoort weaves you a flying carpet of star silk, and together you follow the dunes, the palm canyons and the salt flat by night. The five star beacons that guide the caravans have gone out: light them again with the star puzzles, and the voice of Moeder Bron, the spirit of the Mother Oasis, grows from a drip to a fountain.",
     "Moeder Wolkwal zwemt je naar het oosten over de Wolkenzee naar de roze kliffen, en vanaf de Staarthaven van Hoogvads vliegt luchtschiplijn E naar Rozenkloof: een stad uitgehakt in een kloof van roze zandsteen. De karavaan van Oma Dadel geeft je een bultguh, Knoopje in Saffraanpoort weeft je een vliegend tapijt van sterrenzijde, en samen volg je de duinen, de palmenkloven en de zoutvlakte bij nacht. De vijf sterrenbakens die de karavanen de weg wijzen, zijn gedoofd: steek ze weer aan met de sterrenpuzzels, en de stem van Moeder Bron, de geest van de Moederoase, groeit van een druppel tot een fontein.",
     "The Grand Vizier in the palace is not who he seems: in every mirror of the Mirror Hall sits a bit of Meester Glans, a Mika illusionist. And at the foot of De Sterrendovende Toren, Hertogin Ringeling, the Ring Finger of the Keizer, flees upward and seals the door. Behind it waits the first raid.",
     "De Grootvizier in het paleis is niet wie hij lijkt: in elke spiegel van de Spiegelzaal zit een stukje van Meester Glans, een Mika-illusionist. En aan de voet van De Sterrendovende Toren vlucht Hertogin Ringeling, de Ringvinger van de Keizer, naar boven en verzegelt de deur. Daarachter wacht de eerste raid."),
]

# character cards: (id, name, role en, role nl, zone nr, picture (verhaal picture or "render:<boss id>"), intro en, intro nl, twist en, twist nl, later)
PERSONEN = [
    ("vahoegnir", "Vahoegnir", "The dragon of the Vadsrijk", "De draak van het Vadsrijk", 3, "verhaal:vahoegnir_grijs",
     "A huge dragon, grey and lean, that the whole Vadsrijk is afraid of. Its old nest lies at the top of the Schemerwortel.",
     "Een reusachtige draak, grauw en mager, waar het hele Vadsrijk bang voor is. Zijn oude nest ligt boven in de Schemerwortel.",
     "Vahoegnir is not the villain: it is the first big victim. A splinter broke its heart crystal and the vads leaked out. Cheer it up and it turns pink again; its baby Vahoegje becomes your pet.",
     "Vahoegnir is niet de schurk: hij is het eerste grote slachtoffer. Een splinter brak zijn hartkristal en de vads lekte eruit. Vrolijk hem op en hij wordt weer roze; zijn babydraakje Vahoegje wordt je huisdier.", False),
    ("piepvel", "Graaf Piepvel", "The Pink, the smallest Finger", "De Pink, de kleinste Vinger", 3, "render:piepvel",
     "The smallest, vainest Mika you will ever meet: a cape of mouse fur, a squeaky posh voice and gloomy mice in little cages.",
     "De kleinste, ijdelste Mika die je ooit tegenkomt: een cape van muizenpels, een piepende deftige stem en sombere muisjes in kooitjes.",
     "He answers to someone much bigger. He was the one responsible for the dragon.",
     "Hij luistert naar iemand die veel groter is. Hij was verantwoordelijk voor de draak.", False),
    ("moeder_wolkwal", "Moeder Wolkwal", "The sky whale under Hoogvads", "De luchtwalvis onder Hoogvads", 5, "verhaal:hoogvads",
     "A giant sky whale that has drifted over the Cloud Sea for centuries, with the capital on her back.",
     "Een reusachtige luchtwalvis die al eeuwen boven de Wolkenzee drijft, met de hoofdstad op haar rug.",
     "The grey reached her after all: her bad dream (De Grauwe Droom) is the boss of part II. Comfort her and Hoogvads can sail again.",
     "De grauwheid bereikte haar toch: haar nare droom (De Grauwe Droom) is de baas van deel II. Troost haar en Hoogvads kan weer varen.", False),
    ("zachtmoed", "Koningin Zachtmoed de Derde", "The queen of the guhs", "De koningin van de guhs", 5, "verhaal:raadszaal",
     "The old, sweet queen of the guhs, with a crown of knitted wool. She once knighted Grootmeester Vadsvoet.",
     "De oude, lieve koningin van de guhs, met een kroon van gebreide wol. Ze ridderde ooit Grootmeester Vadsvoet.", "", "", False),
    ("keizer", "De Grauwe Keizer-Mika", "The one who never has enough", "Die nooit genoeg heeft", 5, "sil:?",
     "Mika's scare each other with an old legend at night: if you share your snack, the Keizer comes to get you.",
     "Mika's maken elkaar 's nachts bang met een oude legende: als je je knabbel deelt, komt de Keizer je halen.",
     "At the far edge of the world sits a lean grey cat as long as a mountain range, in his castle Nooitgenoeg. He spreads the Vadsloosheid on purpose: what goes grey leaks its vads, and his Mika's carry it east to the Empty Bowl. The second moon is his Eye. Five generals, his Fingers, do his work.",
     "Aan de verste rand van de wereld zit een magere grauwe kat zo lang als een bergketen, in zijn burcht Nooitgenoeg. Hij verspreidt de Vadsloosheid expres: wat grauw wordt, lekt zijn vads, en zijn Mika's brengen die naar het oosten, naar de Lege Schaal. De tweede maan is zijn Oog. Vijf generaals, zijn Vingers, doen zijn werk.", False),
    ("ringeling", "Hertogin Ringeling", "The Ring Finger", "De Ringvinger", 6, "raid:ringeling",
     "The treasurer of the Keizer, with his rings on all ten of her toes.",
     "De schatbewaarder van de Keizer, met zijn ringen om alle tien haar tenen.",
     "In Het Zijdeland she puts out the star beacons and catches the starlight in her rings, from the Sterrendovende Toren (the first raid). Kootje Zilverstaart in Hoogvads was her spy. At the top of the tower she is not beaten but cheered up: she writes the Keizer that she quits ('I have seen stars') and leaves his ring with you.",
     "In Het Zijdeland dooft ze de sterrenbakens en vangt ze het sterrenlicht in haar ringen, vanuit De Sterrendovende Toren (de eerste raid). Kootje Zilverstaart in Hoogvads was haar spion. Boven in de toren wordt ze niet verslagen maar opgevrolijkt: ze schrijft de Keizer dat ze stopt ('Ik heb sterren gezien') en laat zijn ring bij jou achter.", False),
    ("moeder_bron", "Moeder Bron", "The spirit of the Mother Oasis", "De geest van de Moederoase", 6, "verhaal:zijdeland",
     "Somewhere in the rose sand of Het Zijdeland lies a hidden oasis that gives the caravans their water.",
     "Ergens in het roze zand van Het Zijdeland ligt een verborgen oase die de karavanen hun water geeft.",
     "The oasis is being pumped empty towards the east. Cheer up Moeder Bron and she shows the star road again.",
     "De oase wordt leeggepompt naar het oosten. Vrolijk Moeder Bron op en ze wijst de sterrenweg weer.", False),
]
VINGERS = [("De Pink", "Graaf Piepvel", "zone 3"), ("De Ringvinger", "Hertogin Ringeling", "zone 6"), ("De Lange Vinger", "Generaal Brie van Bluf", "zone 9"),
           ("De Wijsvinger", "Kanselier Wijsneus", "zone 12"), ("De Duim", "Maarschalk Drukduim", "zone 16")]

# the world's timeline: (zone nr, when en, when nl, title en, title nl, text en, text nl, spoiler)
TIJD = [
    (1, "Long ago", "Lang geleden", "A pink world", "Een roze wereld",
     "The klodders were pink and the Vadsrijk had only one moon, says Oma Truus.", "De klodders waren roze en het Vadsrijk had maar één maan, zegt Oma Truus.", False),
    (1, "Then", "Toen", "The grey comes", "De grauwheid komt",
     "Flowers wilt grey, creatures go thin and grumpy. A second moon hangs in the west.", "Bloemen verwelken grijs, wezens worden mager en knorrig. In het westen hangt een tweede maan.", False),
    (3, "Not so long ago", "Nog niet zo lang geleden", "The dragon falls silent", "De draak wordt stil",
     "The great dragon leaves its nest at the top of the world tree, grey and angry.", "De grote draak verlaat zijn nest boven in de wereldboom, grauw en boos.", False),
    (5, "Recently", "Kort geleden", "A bad dream", "Een nare droom",
     "Moeder Wolkwal, who has always been safe above the clouds, starts to dream grey.", "Moeder Wolkwal, die boven de wolken altijd veilig was, begint grauw te dromen.", False),
    (6, "Lately", "Onlangs", "The stars go out", "De sterren doven",
     "Over Het Zijdeland the star beacons go out one by one, and the caravans lose their way in the rose sand.", "Boven Het Zijdeland gaan de sterrenbakens een voor een uit, en de karavanen raken de weg kwijt in het roze zand.", False),
    (5, "The legend", "De legende", "The Empty Bowl", "De Lege Schaal",
     "A small, hungry Mika stole his first snack and felt something warm: vads. Then it was gone. He took more, and more, reached further and further, and built a castle around a bowl as big as a lake that has never been full. What every guh knows: vads only grows when you share it.",
     "Een kleine, hongerige Mika stal zijn eerste knabbel en voelde iets warms: vads. Toen was het weg. Hij pakte er meer, en meer, reikte steeds verder, en bouwde een burcht om een schaal zo groot als een meer die nog nooit vol is geweest. Wat elke guh weet: vads groeit alleen als je hem deelt.", True),
    (1, "Now", "Nu", "You", "Jij",
     "A new recruit of De Knuffelorde arrives on Knuffelkamp.", "Een nieuwe rekruut van De Knuffelorde komt aan op Knuffelkamp.", False),
]
TRAPPEN = [("Grey", "Grauw", "#9a94a0"), ("Thin", "Mager", "#8e7f8f"), ("Grumpy", "Knorrig", "#7d6378"), ("Hardened", "Verhard", "#5f5470"), ("Hollow", "Uitgehold", "#3c3646")]


def _kopieer(b):
    bron = os.path.join(b.root, *BRON)
    doel = os.path.join(b.out, "img", "verhaal")
    os.makedirs(doel, exist_ok=True)
    namen = set()
    if os.path.isdir(bron):
        for f in os.listdir(bron):
            if f.endswith(".webp"):
                shutil.copyfile(os.path.join(bron, f), os.path.join(doel, f))
                namen.add(f[:-5])
    else:
        b.notities.append("verhaal: docs/wiki/vadsrijk_verhaal missing (no story pictures)")
    return namen


def _beeld(b, namen, ref, alt=""):
    """ref: 'verhaal:<naam>', 'render:<boss id>' (only when the build made it), 'sil:<char>' -> <img> html or a silhouette."""
    soort, _, naam = ref.partition(":")
    if soort == "render" and os.path.exists(os.path.join(b.out, "img", "renders", "baas_" + naam + ".webp")):
        return f'<img src="img/renders/baas_{ESC(naam)}.webp" alt="{ESC(alt)}" loading="lazy">'
    if soort == "raid" and os.path.exists(os.path.join(b.out, "img", "renders", "raid_" + naam + ".webp")):
        return f'<img src="img/renders/raid_{ESC(naam)}.webp" alt="{ESC(alt)}" loading="lazy">'
    if soort == "raid":
        naam, soort = "sterrentoren", "verhaal"
    if soort == "render":
        naam, soort = {"piepvel": "nest"}.get(naam, ""), "verhaal"
    if soort == "verhaal" and naam in namen:
        return f'<img src="img/verhaal/{ESC(naam)}.webp" alt="{ESC(alt)}" loading="lazy">'
    return f'<span class="sil" aria-hidden="true">{ESC(naam if soort == "sil" else "?")}</span>'


def verhaal(b):
    P = "verhaal.html"
    namen = _kopieer(b)
    zonenaam = {z["nr"]: z["naam"] for z in b.d["zones"]}
    zonekleur = {z["nr"]: z["kleur"] for z in b.d["zones"]}
    lore = {}
    for l in b.d.get("lore", []):
        lore.setdefault(int(l["zone"].replace("zone", "")), []).append(l)
    verstopt = lambda n: f'<div class="verstopt-zone" data-zone="{n}">{t("Hidden by your spoiler setting (top right).", "Verborgen door je spoilerinstelling (rechtsboven).")}</div>'  # noqa: E731

    # the map
    kaart = ""
    if "wereldkaart" in namen and getattr(b, "open_zones", 6) >= 6:   # X-SITE: the lore world map names every later land
        kaart = (f'<figure class="vh-hero"><a href="img/verhaal/wereldkaart.webp"><img src="img/verhaal/wereldkaart.webp" alt="De Lange Reis: de wereldkaart van De Knuffelorde" width="1600" height="853"></a>'
                 f'<figcaption><span data-zone="6">' + f'{t("The Long Journey: the world map of De Knuffelorde. From Knuffelkamp (left) through the Island Sea and the Floating City to the Great Mainland and Burcht Nooitgenoeg (right). Zones 1-6 are in the game (up to Het Zijdeland); further east is still coming.", "De Lange Reis: de wereldkaart van De Knuffelorde. Van Knuffelkamp (links) via de Eilandzee en de Zwevende Stad naar het Grote Vasteland en Burcht Nooitgenoeg (rechts). Zone 1-6 zitten in het spel (tot en met Het Zijdeland); verder naar het oosten komt nog.")} ' + '</span>'
                 f'<span class="verstopt-zone" data-zone="6">{t("The Long Journey: the world map of De Knuffelorde. Pick a higher zone at the top right to read where it all goes.", "De Lange Reis: de wereldkaart van De Knuffelorde. Kies rechtsboven een hogere zone om te lezen waar het allemaal heen gaat.")}</span>'
                 f'<a href="kaart.html" style="color:#ffd3e6">{t("The real map of zones 1-6", "De echte kaart van zone 1-6")}</a></figcaption></figure>')

    # part navigation
    nav = "".join(f'<a href="#deel{d[0]}" data-zone="{d[5][0]}" style="--dk:{d[4]}">{t(d[1], d[2])}<small>{d[3]}</small></a>' for d in DELEN)
    nav += "".join(f'<a class="later" data-zone="6" href="#later">{t(en, nl)}<small>{lv} &middot; {t("coming", "komt nog")}</small></a>' for en, nl, lv in LATER)

    # chapters
    delen = ""
    for nr, en, nl, lvr, kleur, zones in DELEN:
        delen += (f'<section id="deel{nr}" data-zone="{zones[0]}"><div class="vh-deel" style="--dk:{kleur}"><h2>{t(en, nl)}</h2><p>{lvr} &middot; '
                  + t("zones " + "-".join(str(z) for z in (zones[0], zones[-1])), "zones " + "-".join(str(z) for z in (zones[0], zones[-1]))) + "</p></div>")
        for (zn, pic, cen, cnl, ten, tnl, lvz, txen, txnl, twen, twnl) in [h for h in HOOFDSTUKKEN if h[0] in zones]:
            beeld = ""
            if pic and pic in namen:
                beeld = f'<figure class="vh-beeld"><img src="img/verhaal/{pic}.webp" alt="{ESC(cnl)}" loading="lazy"><figcaption>{t(cen, cnl)}</figcaption></figure>'
            elif os.path.exists(os.path.join(b.out, "img", f"kop_zone{zn}.webp")):
                beeld = f'<figure class="vh-beeld"><img src="img/kop_zone{zn}.webp" alt="" loading="lazy"></figure>'
            meer = ""
            if lore.get(zn):
                meer = ('<details class="meer"><summary>' + t("More from the game", "Meer uit het spel") + "</summary>"
                        + "".join(f'<h4>{ESC(l["titel"])}</h4><p>{ESC(l["tekst"])}</p>' + "".join(f"<h5>{ESC(k)}</h5><p>{ESC(v)}</p>" for k, v in l["stukken"]) for l in lore[zn])
                        + "</details>")
            delen += (f'<article class="vh-hoofdstuk" data-zone="{zn}" style="--zk:{zonekleur.get(zn, "#c2577f")}">{beeld}<div>'
                      f'<span class="nr">{t("Zone", "Zone")} {zn} &middot; {lvz}</span><h3>{t(ten, tnl)}</h3>{p(ESC(txen), ESC(txnl))}'
                      f'{spoiler(p(ESC(twen), ESC(twnl)), "What is really going on? (spoiler)", "Wat is er echt aan de hand? (spoiler)")}'
                      f'<p><a href="zones/zone{zn}.html">{t("Zone page", "Zonepagina")}: {ESC(zonenaam.get(zn, ""))}</a></p>{meer}</div></article>'
                      + verstopt(zn))
        if nr == 2 and b.d.get("raid"):   # U-SITE round 3: the raid closes part II
            delen += (f'<article class="vh-hoofdstuk" data-zone="6" style="--zk:{zonekleur.get(6, "#e07a5f")}"><div><span class="nr">{t("Zone 6 · raid", "Zone 6 · raid")} &middot; lv 70</span>'
                      f'<h3>{ESC(b.d["raid"]["naam"])}</h3>'
                      + p("The tower at the Mother Oasis where the stars were put out: four floors for 2 to 10 players, with the Sand Guard, the Mirror Vizier, the Dimmed Constellations and, on the roof, Duchess Ringeling.",
                          "De toren bij de Moederoase waar de sterren zijn gedoofd: vier verdiepingen voor 2 tot 10 spelers, met de Zandwachter, de Spiegelvizier, de Gedoofde Sterrenbeelden en op het dak Hertogin Ringeling.")
                      + f'<p><a href="raid/index.html">{t("The raid page", "De raidpagina")}</a></p></div></article>' + verstopt(6))
        delen += "</section>" + verstopt(zones[0])
    later = ('<section id="later" data-zone="6"><div class="vh-deel" style="--dk:#7d7884"><h2>' + t("And further east...", "En verder naar het oosten...") + "</h2>"
             + p("After Het Zijdeland the Great Mainland goes on, greyer and leaner the further east you go: the Soot Front, the Silent Land, the Occupied Lands and finally the Grey Empire. Twenty zones, up to level 100. Coming later.",
                 "Na Het Zijdeland loopt het Grote Vasteland door, grauwer en magerder hoe verder naar het oosten: het Roetfront, het Stille Land, de Bezette Landen en tot slot het Grauwe Keizerrijk. Twintig zones, tot level 100. Komt later.")
             + "</div></section>" + verstopt(6))

    # what is vads
    vads = (f'<section data-zone="1"><h2 id="vads">{t("Vads and the grey", "Vads en de grauwheid")}</h2>'
            + p("Vads is the warm, soft, chubby joy of life in every creature. You see it as colour, feel it as warmth and hear it as sound: squeaks, songs and laughter. It grows when you SHARE it: cuddles, food, songs, light. That is why guhs have so much of it, and why they never fight. Vads is light, grey is heavy: guh airships fly on warm air and good cheer.",
                "Vads is de warme, zachte, mollige levensvreugde in elk wezen. Je ziet hem als kleur, je voelt hem als warmte en je hoort hem als geluid: piepjes, liedjes en gelach. Hij groeit als je hem DEELT: knuffels, eten, liedjes, licht. Daarom hebben guhs er zo veel van, en daarom vechten ze nooit. Vads is licht, grauw is zwaar: guh-luchtschepen vliegen op warme lucht en goede zin.")
            + p("The Vadsloosheid comes in steps:", "De Vadsloosheid gaat in trappen:")
            + '<div class="vh-trappen">' + "".join(f'<span style="background:{k}">{i + 1}. {t(en, nl)}</span>' for i, (en, nl, k) in enumerate(TRAPPEN)) + "</div>"
            + p("Every knight of De Knuffelorde fights by giving: cheering a creature up gives its vads back.", "Elke ridder van De Knuffelorde vecht door te geven: een wezen opvrolijken geeft zijn vads terug.")
            + "</section>")

    # characters
    kaarten = ""
    for pid, naam, ren, rnl, zn, pic, ien, inl, twen, twnl, later_ in PERSONEN:
        tw = spoiler(p(ESC(twen), ESC(twnl)), "Spoiler: click to show", "Spoiler: klik om te kijken") if twen else ""
        kaarten += (f'<div class="vh-kaart" data-zone="{zn}"><div class="pic">{_beeld(b, namen, pic, naam)}</div><div class="tx"><h4>{ESC(naam)}</h4>'
                    f'<div class="rol">{t(ren, rnl)} &middot; {t("zone", "zone")} {zn}</div>{p(ESC(ien), ESC(inl))}{tw}</div></div>')
    vingers = ('<div data-zone="5">' + spoiler('<p>' + t("Waar mijn armen niet reiken, reiken mijn Vingers. (The Keizer's five generals; 'Finger' is only a nickname.)",
                                                          "Waar mijn armen niet reiken, reiken mijn Vingers. (De vijf generaals van de Keizer; 'Vinger' is alleen een bijnaam.)") + "</p>"
                                              + '<div class="tscroll"><table class="data"><thead><tr><th>' + t("Finger", "Vinger") + "</th><th>" + t("Name", "Naam") + "</th><th>Zone</th></tr></thead><tbody>"
                                              + "".join(f"<tr><td>{ESC(a)}</td><td>{ESC(n)}</td><td>{ESC(z)}</td></tr>" for a, n, z in VINGERS) + "</tbody></table></div>",
                                              "The five Fingers (big spoiler)", "De vijf Vingers (grote spoiler)") + "</div>")

    # timeline
    tijd = '<ol class="vh-tijd">' + "".join(
        f'<li data-zone="{zn}"><span class="wanneer">{t(wen, wnl)}</span><b>{t(ten, tnl)}</b>'
        + (spoiler(p(ESC(xen), ESC(xnl)), "Read the legend (spoiler)", "Lees de legende (spoiler)") if sp else p(ESC(xen), ESC(xnl))) + "</li>"
        for zn, wen, wnl, ten, tnl, xen, xnl, sp in TIJD) + "</ol>"

    main = f"""<h1>{t("The story of the Vadsrijk", "Het verhaal van het Vadsrijk")}</h1>
<p class="lead">{t("The Long Journey of De Knuffelorde, part by part. How every story begins; the twists sit behind spoiler buttons, and the switch at the top right hides later zones.",
                   "De Lange Reis van De Knuffelorde, deel voor deel. Hoe elk verhaal begint; de wendingen zitten achter spoilerknoppen, en de schakelaar rechtsboven verbergt latere zones.")}</p>
{kaart}
<nav class="vh-delen" aria-label="Delen / Parts">{nav}</nav>
{vads}
<h2 id="tijdlijn">{t("Timeline of the world", "Tijdlijn van de wereld")}</h2>{tijd}
<h2 id="personages">{t("Who is who", "Wie is wie")}</h2>
<div class="vh-kaarten">{kaarten}</div>{vingers}
<h2 id="reis">{t("The Long Journey", "De Lange Reis")}</h2>
{delen}{later}"""
    b.schrijf(P, "Verhaal", "Lore", main, P, head=CSS, beschrijving="Het verhaal van Het Vadsrijk: de Lange Reis, de personages en de wereldkaart.")
