"""
bbq2 (ring-h6) - every Dutch text of chapter 6, "De Frituurberg" (the generators write Dutch only; English: phase 3).
"""

STRUCTUUR = ("De Frituurberg", "De vulkaan waar de Knabbelring gefrituurd moet worden (Guhbarbecuether, Rookdelta)")

# the signs in the template (sign_text prefix sign.guhs.ring_h6): four lines each
BORDEN = {
    "kamp": ["~ Basiskamp ~", "Frituurberg: omhoog", "Eten: hier", "Njeg."],
    "poort": ["DE FRITUURBERG", "Verboden toegang!", "Eigendom van", "het Oog. (Honger!)"],
    "kooi_1": ["AFZUIGKAP 1", "Niet voeren.", "Niet aaien.", "Niet loslaten!"],
    "kooi_2": ["AFZUIGKAP 2", "Zuigt rook weg.", "Zielig? Welnee.", "Slot NIET openen"],
    "kooi_3": ["AFZUIGKAP 3", "Laatste kans:", "afblijven,", "ringdragertje!"],
    "touw": ["Pad ingestort.", "Kijk omhoog:", "blauw lampje =", "haak. Touw erop!"],
    "zwaar": ["Zware ring?", "Rust even uit.", "Of zoek een", "sterke tuinguh."],
    "spleet": ["FRITUURSPLEET", "Niet zwemmen.", "Niet proeven.", "Vooral niet delen"],
}

# --- the questline (verhaal_motor.verhaallijn) ------------------------------------------------------------------------------
LIJN_NAAM = "De Frituurberg"
LIJN_UITLEG = "De Knabbelring moet de frituur in. Boven op de Frituurberg, in de Rookdelta. Sam-guh loopt mee, Smikagol ook. Helaas."
WAAR = "De Frituurberg in de Rookdelta (Guhbarbecuether)"
STAPPEN = [
    ("Naar de Frituurberg", "Loop met Sam-guh naar de voet van de Frituurberg en zoek het basiskamp met het Rustvuurtje.", WAAR),
    ("Het Kronkelpad", "Klim het Kronkelpad op naar de Eerste Richel. Ontwijk de vallende kooltjes (ze duwen alleen) en klik op het slot "
                       "van de kooi om het Rookguhje te bevrijden.", "Het Kronkelpad, om de berg heen"),
    ("Langs het touw omhoog", "Het pad is ingestort. Kijk omhoog naar het blauwe lampje en gebruik het Elfentouw op de haak, twee keer. "
                              "Bevrijd het tweede Rookguhje. Smikagol graait naar de ring: flits hem weg met het Lichtflesje.",
     "De westwand van de Frituurberg"),
    ("De ring wordt zwaar", "Volg het smalle pad naar de oostkant, neem de laatste haak omhoog en bevrijd het derde Rookguhje op de "
                            "Derde Richel.", "Hoog op de Frituurberg"),
    ("Ik kan de ring niet dragen...", "De ring is te zwaar geworden. Klik op Sam-guh: hij draagt jou het laatste stuk naar de Frituurspleet.",
     "De Derde Richel"),
    ("De Frituurspleet", "Loop door de spleet naar het Bakrandje boven de frituur. Het is tijd.", "De krater van de Frituurberg"),
    ("Feest in de Gouw", "De Rookguhs hebben je thuisgebracht. Loop naar je vrienden in de Knabbelgouw: er wordt iemand gekroond en er is "
                         "heel veel eten.", "De Knabbelgouw, bij de grote barbecueput (Guhmensie)"),
]
KLAAR = ("De Knabbelring is gefrituurd en gedeeld. Het Oog doet een dutje. Vahoeg!", "Overal waar het naar friet ruikt")
KORT = {"0": "Ga naar het basiskamp bij de Frituurberg", "1": "Klim het Kronkelpad op en bevrijd het Rookguhje",
        "2": "Elfentouw op de haken; bevrijd het tweede Rookguhje", "3": "Laatste haak; bevrijd het derde Rookguhje",
        "4": "Klik op Sam-guh: hij draagt je", "5": "Loop naar het Bakrandje boven de frituur", "6": "Ga naar het feest in de Knabbelgouw"}

# --- the narrator card ------------------------------------------------------------------------------------------------------
KAART_TITEL = "Hoofdstuk 6: De Frituurberg"
KAART_REGELS = [
    "Voorbij de Zwarte Roosterpoort ligt de Rookdelta. En daar, midden in de damp, staat hij: de Frituurberg.",
    "Hier is de Knabbelring ooit gebakken. Bijna dan. Hij is nooit gefrituurd, en dáárom wil iedereen hem opeten.",
    "Het plan is simpel: naar boven klimmen, ring in de frituur, klaar. Sam-guh heeft er pannen voor meegenomen.",
    "Smikagol loopt ook mee. Hij zegt dat hij helpt. Hij kwijlt een beetje. Njeg.",
]

# --- the cutscenes: id -> (title of the replay button, {key: line}, {actor: name}) -------------------------------------------
SCENES = {
    "ringh6_frituur": ("De Frituurspleet", {
        "aankomst": "De Frituurspleet. Hier is de Knabbelring gebakken. Bijna dan: hij is nooit gefrituurd.",
        "gooi": "Daar is de frituur, baas. Gooi hem erin! ...Hij ruikt wel lekker, njeg.",
        "twijfel": "De ring is zwaar. En warm. En hij ruikt naar verse kaasknabbel. Eén hapje kan toch geen kwaad?",
        "niet_eten": "Niet opeten! Frituren! Dat hadden we afgesproken, njeg!",
        "mijn": "Mijn vadsje... MIJN vadsssje!",
        "gegrepen": "Smikagol grist de Knabbelring zo uit je pootjes!",
        "dans": "Van onsss! Het vadsje is van onsss! Vahoeg, vahoeg, vah...",
        "oeps": "...oeps.",
        "stil": "Plons. Het borrelt. Het sist. Het ruikt... eigenlijk best lekker.",
        "ach": "Ach, arme gluiperd. ...Ruik jij dat ook? Het ruikt naar de kermis, njeg.",
        "krokant": "PLOP! Daar is hij weer. Goudbruin. Knapperig. En stralend.",
        "lekker": "Kijk nou! Het vadsje is krokant! En wij ook! Smikagol heeft het nog nooit zo lekker warm gehad!",
        "delen": "Hij is veel te groot voor Smikagol alleen. Wil... willen jullie ook een stukje?",
        "delen_sam": "Delen! Dát is het! Eén knabbel om ze allemaal te delen, njeg!",
        "iedereen": "Iedereen krijgt een stuk. Het knispert. De hebberigheid smelt weg als kaas in de frituur.",
        "oog": "Hoog boven de krater gluurt het Oog van Sausron naar binnen. Hij had al die tijd alleen maar... trek.",
        "voor_oog": "Hier, groot oog. Voor jou. Nu niet meer zo boos kijken.",
        "oog_eet": "Hap. Het Oog knippert tevreden. Eén keer. Twee keer...",
        "dutje": "...en doet een dutje. Zzz. De Guhbarbecuether is veilig. En ruikt naar friet.",
    }, {"sam": "Sam-guh", "smikagol": "Smikagol", "krokant": "Smikagol (krokant)", "oog": "Het Oog van Sausron"}),
    "ringh6_vlucht": ("De Rookguhs komen", {
        "borrelt": "Eh... baas? De frituur kookt over! De hele berg gaat borrelen, njeg!",
        "komen": "Vahoeg! Wij komen jullie halen! Met papa en mama!",
        "rookguhs": "Daar zijn ze: de Rookguhjes die je bevrijd hebt. Met hun hele, héle grote familie.",
        "ook_mee": "Wacht op onsss! Smikagol is nu een snack, hier blijven is gevaarlijk!",
        "naar_huis": "Omhoog, door de rook, over de Rookdelta. Terug naar de Knabbelgouw.",
    }, {"sam": "Sam-guh", "krokant": "Smikagol (krokant)", "kleintje": "Rookguhje"}),
    "ringh6_feest": ("Het feest in de Gouw", {
        "thuis": "De Knabbelgouw. Het ruikt er naar gras, naar vuurwerk en naar heel veel eten.",
        "welkom": "Daar is onze Ringdrager! Te laat voor het ontbijt, precies op tijd voor het feest. Zo hoort het.",
        "tweede": "Is er ook een tweede feest? En een tweede toetje?",
        "stil": "Sst, Pipp. Eerst de kroon. Dán het toetje.",
        "kroon": "Araguh, zwerver van het Worstenwoud: niet op je kroon knabbelen voor het eind van de dag. Leve de koning!",
        "koning": "Vrienden... jullie buigen voor niemand. Behalve voor het buffet, njeg.",
        "tel": "Drieënveertig knabbels heb ik onderweg gevonden! Drieënveertig!",
        "tel_meer": "Vierenveertig. Ik pakte er net één van je bord.",
        "sorry": "Sorry nog van dat ene keertje met de ring. Ik heb een taart gebakken. Hij is een beetje aangebrand.",
        "vis": "Smikagol heeft visss meegebracht! Gefrituurd. Alles is lekkerder gefrituurd.",
        "naar_huis": "Nou, baas. We zijn er weer. ...Mag ik bij jou blijven wonen? Ik kan koken, njeg.",
        "einde": "En ze knabbelden nog lang en gelukkig. Eén knabbel om ze allemaal te delen.",
    }, {"sam": "Sam-guh", "krokant": "Smikagol", "guhdalf": "Guhdalf de Witte", "araguh": "Araguh", "leguhlas": "Leguhlas", "gimguh": "Gimguh",
        "merrie": "Merrie", "pippguh": "Pippguh", "boromika": "Boromika"}),
}

TEKSTEN = {
    # the block, the item, the creatures
    "block.guhs.ringh6_kooislot": "Kooislot van de Frituurberg",
    "block.guhs.ringh6_kooislot.lore": "Houdt een Rookguhje gevangen. Klik erop om het open te wrikken.",
    "item.guhs.ringh6_stukje_knabbelring": "Stukje gefrituurde Knabbelring",
    "item.guhs.ringh6_stukje_knabbelring.lore": "Goudbruin, knapperig en van iedereen een beetje. Eén knabbel om ze allemaal te delen.",
    "entity.guhs.ringh6_rookguh": "Rookguhje",
    "entity.guhs.ringh6_valkool": "Vallend kooltje",
    "entity.guhs.ringh6_krokante_smikagol": "Smikagol (krokant)",
    # arriving, the camp
    "quest.guhs.ringh6.kamp.sam": "Daar staat hij dan, baas. De Frituurberg. Eerst een hapje bij het vuur, dan naar boven. Njeg.",
    "quest.guhs.ringh6.kamp.smikagol": "Omhoog, omhoog, het lekkere pad! Smikagol wijst de weg. Smikagol draagt ook wel even de ring? Nee? Jammer.",
    "quest.guhs.ringh6.kamp.hint": "Volg het Kronkelpad achter de poort. Kooltjes die vallen duwen je alleen: uitkijken en doorlopen!",
    # the coals
    "quest.guhs.ringh6.kool.raak.0": "Boink! Een kooltje. Het duwt alleen, njeg.",
    "quest.guhs.ringh6.kool.raak.1": "Pats! Heet, maar je hebt niks. Kijk omhoog!",
    "quest.guhs.ringh6.kool.raak.2": "Au! O nee, toch niet au. Gewoon geduwd.",
    "quest.guhs.ringh6.kool.sam": "Kijk uit, baas! De berg gooit met kooltjes!",
    # the cages
    "quest.guhs.ringh6.slot.open": "Krak! Het slot springt open.",
    "quest.guhs.ringh6.slot.al_open": "Dit Rookguhje heb je al bevrijd. Het zwaait vast ergens boven je, njeg.",
    "quest.guhs.ringh6.slot.eerst": "Dit slot zit muurvast. Bevrijd eerst het Rookguhje lager op de berg.",
    "quest.guhs.ringh6.slot.niet_van_jou": "Dit is niet jouw avontuur: dit slot wacht op een ringdrager die hier in het verhaal is.",
    "quest.guhs.ringh6.rookguh.vrij.1": "Vahoeg! Vrij! De Mika's gebruikten mij als afzuigkap! Als je ons nodig hebt, komen we je halen!",
    "quest.guhs.ringh6.rookguh.vrij.2": "Vrij, vrij, vrij! Mijn zusje zit nog hoger. Ik haal papa en mama vast, njeg!",
    "quest.guhs.ringh6.rookguh.vrij.3": "Eindelijk! Wij zijn compleet! Roep maar als de berg gaat borrelen, wij komen aanvliegen!",
    "quest.guhs.ringh6.rookguh.zielig": "Het Rookguhje kijkt je door de tralies aan. Het slot zit aan de voorkant.",
    "quest.guhs.ringh6.rookguh.sam": "Arm ding. Goed gedaan, baas. Verder omhoog!",
    "quest.guhs.ringh6.rookguh.dank.1": "Vahoeg, ringdrager! Wij wonen hier nu gewoon. Zonder kooi. Het ruikt naar friet, njeg!",
    "quest.guhs.ringh6.rookguh.dank.2": "Weet je nog, die vlucht naar huis? Papa heeft er nog spierpijn van. Dank je wel!",
    "quest.guhs.ringh6.rookguh.dank.3": "De Mika's hebben nu een echte afzuigkap gekocht. Die doet het niet. Wij lachen ons rond!",
    # the rope
    "quest.guhs.ringh6.touw.hint": "Het pad houdt op. Kijk omhoog naar het blauwe lampje en rechtsklik met het Elfentouw.",
    "quest.guhs.ringh6.touw.sam": "Ik kan niet klimmen met al die pannen, baas. Ga maar, ik kom wel! (Vraag niet hoe.)",
    "quest.guhs.ringh6.touw.geen": "Je hebt het Elfentouw nodig. Sam-guh had er nog eentje in zijn rugzak: hier!",
    # Smikagol
    "quest.guhs.ringh6.smikagol.loer.0": "Het vadsje wordt zo zwaar... Smikagol draagt het wel even. Geef maar. Geef!",
    "quest.guhs.ringh6.smikagol.loer.1": "Wat glimt daar in je zak? Laat Smikagol eens kijken... van dichtbij...",
    "quest.guhs.ringh6.smikagol.loer.2": "Zo moe, het ringdragertje. Smikagol helpt! Smikagol pakt alleen even het vadsje...",
    "quest.guhs.ringh6.smikagol.mis": "Bijna! Het vadsje glipt weg. Sssorry. Dat was een aai, geen graai.",
    "quest.guhs.ringh6.smikagol.duw": "Smikagol graait naar de ring en duwt je opzij!",
    "quest.guhs.ringh6.smikagol.verblind": "Auw! Het licht! Het prikt in onze oogjesss! Smikagol doet al niks meer!",
    "quest.guhs.ringh6.smikagol.sam": "Poten thuis, gluiperd! Baas, flits hem met het Lichtflesje als hij weer begint.",
    # the weight, Sam carries
    "quest.guhs.ringh6.zwaar.half": "De Knabbelring trekt aan je zak. Hij wordt zwaarder bij elke stap.",
    "quest.guhs.ringh6.zwaar.vol": "De Knabbelring weegt nu als een hele kaaswinkel. Je komt bijna niet meer vooruit.",
    "quest.guhs.ringh6.sam.draag": "Ik kan de ring niet dragen... maar wel jou, njeg! Klim maar op mijn rug, baas.",
    "quest.guhs.ringh6.sam.nog_niet": "Eerst de Rookguhjes, baas. Daarna draag ik je, beloofd.",
    "quest.guhs.ringh6.sam.boven": "Daar is de spleet. Verder moet je zelf, baas. Het is jouw knabbel. Ik sta vlak achter je.",
    "quest.guhs.ringh6.sam.hint": "Klik op Sam-guh: hij draagt je het laatste stuk omhoog.",
    # falling, the pool
    "quest.guhs.ring.terug.ringh6_frituur": "Net niet gefrituurd! De Rookguhjes visten je eruit en zetten je bij je rustpunt. Njeg.",
    "quest.guhs.ringh6.rand.hint": "Loop naar de rand van het Bakrandje, boven de frituur.",
    "quest.guhs.ringh6.rand.geen_ring": "Je hebt de Knabbelring niet bij je. Klik op Sam-guh: hij heeft hem voor je bewaard.",
    # the end
    "quest.guhs.ringh6.stukje": "Je krijgt je eigen stukje gefrituurde Knabbelring. Bewaren mag. Opeten ook.",
    "quest.guhs.ringh6.thuis": "De Rookguhs zetten je zachtjes neer in de Guhmensie. Daar wordt al gezongen: loop naar het feest!",
    "quest.guhs.ringh6.thuis.ver": "Het feest wacht op je in de Knabbelgouw: het kamp van Guhdalf bij de grote barbecueput in de Guhmensie, waar je "
                                   "verhaal begon. Ga erheen (het Superkompas wijst de weg: Mijn verhaal). Zodra je daar op de grond staat, begint het feest.",
    "quest.guhs.ringh6.feest.klaar": "Wat een feest. En vanaf nu is er elke dag eentje, in de Knabbelgouw.",
    "gui.guhs.ringh6.doel.thuis": "Het feest in de Knabbelgouw",
}

# visible advancements (tab knabbelring): (name, parent, icon, frame, title, text)
ADVANCEMENTS = [
    ("ring_h6_berg", "ring_gekregen", "guhs:gloeiend_kooltje", "task", "Daar staat hij dan", "Bereik het basiskamp aan de voet van de Frituurberg"),
    ("ring_h6_rookguhs", "ring_h6_berg", "guhs:roosterijzer_tralies", "task", "Geen afzuigkap meer", "Bevrijd de drie Rookguhjes van de Frituurberg"),
    ("ring_h6_flits", "ring_h6_berg", "guhs:lichtflesje", "task", "Het prikt in onze oogjesss",
     "Flits Smikagol weg met het Lichtflesje als hij naar de ring graait"),
    ("ring_h6_gefrituurd", "ring_h6_rookguhs", "guhs:ringh6_stukje_knabbelring", "goal", "Eén knabbel om ze allemaal te delen",
     "Zie hoe de Knabbelring toch nog gefrituurd wordt. En gedeeld."),
]
VERBORGEN = ["ring_h6_kool"]

# the FTB section (chapter guhs_knabbelring; the quests are a chain in this order: FTB_LINEAIR)
FTB = [
    ("ring_h6_kamp", "Daar staat hij dan", "Voorbij de Zwarte Roosterpoort begint de &6Rookdelta&r. Midden in de damp staat de &cFrituurberg&r. "
     "Loop met Sam-guh naar het &6basiskamp&r aan de zuidkant en warm je op bij het Rustvuurtje. Het Superkompas wijst de weg (Mijn verhaal).",
     "guhs:ring_rustvuur", "ring_h6_stap_1"),
    ("ring_h6_pad", "Het Kronkelpad", "Achter de poort slingert het &6Kronkelpad&r om de berg. De berg gooit met &ckooltjes&r: ze doen geen pijn, "
     "ze duwen je alleen. Op de Eerste Richel zit een &dRookguhje&r in een kooi: de Mika's gebruiken het als afzuigkap! Klik op het slot.",
     "guhs:gloeiend_kooltje", "ring_h6_stap_2"),
    ("ring_h6_touw", "Langs het touw omhoog", "Het pad is ingestort. Kijk omhoog: onder elke &6haak&r hangt een blauw lampje. Rechtsklik met het "
     "&7Elfentouw&r en je wordt omhoog getrokken. Twee haken verder zit het tweede Rookguhje. En Smikagol graait naar je ring: &bflits hem weg&r "
     "met het Lichtflesje.", "guhs:elfentouw", "ring_h6_stap_3"),
    ("ring_h6_zwaar", "Zwaarder en zwaarder", "Hoe hoger, hoe zwaarder de ring. Volg het smalle pad naar de oostkant, neem de laatste haak en "
     "bevrijd het derde Rookguhje. Vallen kan geen kwaad: je komt terug bij je laatste Rustvuurtje.", "guhs:knabbelring", "ring_h6_stap_4"),
    ("ring_h6_sam", "Maar wel jou", "De ring weegt nu als een hele kaaswinkel. Klik op &dSam-guh&r. &dIk kan de ring niet dragen... maar wel jou, "
     "njeg!&r Hij sjouwt je het laatste stuk omhoog, naar de Frituurspleet.", "minecraft:saddle", "ring_h6_stap_5"),
    ("ring_h6_frituur", "Eén knabbel om ze allemaal te delen", "Loop door de spleet naar het &6Bakrandje&r boven de frituur. Wat daar gebeurt, "
     "moet je zelf zien. Daarna brengen de Rookguhs je naar huis.", "guhs:ringh6_stukje_knabbelring", "ring_h6_stap_6"),
    ("ring_h6_feest", "Lang leve de koning", "Terug in de &aKnabbelgouw&r wacht iedereen op je. Araguh krijgt een kroon van knabbels, Pippguh "
     "wil een tweede toetje, en jij krijgt de titel &6Ringdrager&r en alles wat daarbij hoort.", "guhs:ring_feestknabbel", "ring_h6_stap_7"),
]
FTB_ZIJ = [
    ("ring_h6_flits", "Het prikt in onze oogjesss", "Als Smikagol op de berg naar de ring graait, gebruik dan het &bLichtflesje&r. Hij houdt "
     "een hele tijd zijn pootjes thuis.", "guhs:lichtflesje", "ring_h6_flits", "ring_h6_touw"),
    ("ring_h6_rookguhs", "Geen afzuigkap meer", "Alle drie de Rookguhjes zijn vrij. Onthoud ze goed: je ziet ze terug als de berg gaat borrelen.",
     "guhs:roosterijzer_tralies", "ring_h6_rookguhs", "ring_h6_zwaar"),
]
