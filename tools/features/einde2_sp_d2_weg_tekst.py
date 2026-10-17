"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.7), slice sp_d2_weg: every Dutch text of part 2 steps 5-8 except the step texts (those are
STAPPEN / EXTRA / KORT in einde2_sp_d2_weg.py, read by the skelet). English: tools/lang/en/c116_e2_sp_d2_weg.json.

Own words only (C-B 6.3): no line, lyric or sound of the show. The required joke "Dennis en de snor" (CONTRACT_E2_C 5.3) is
the scene dennis_snor; its setup ("met een snor herkent niemand ons!") is in snor.
"""

G = "gui.guhs.einde2_sp_d2_weg."

# the seven story scenes: name -> (title, {key: line}, {actor: name}); keys as in D2WegScenes.java
SCENES = {
    "knipoog_appa": ("Knipoog: een vliegende bizon", {
        "vliegen": "Hoi Sponsguh! Hoi Patguh! Jullie kunnen ook gewoon vliegen, hoor!",
        "vleugels": "...Ik heb geen vleugels.",
        "lopen": "En ik heb geen vliegende bizon. Lopen dan maar, njeg!",
    }, {"aang": "Aang-guh", "sponsguh": "Sponsguh", "patguh": "Patguh"}),
    "snor": ("Mannen met een snor", {
        "hallo": "Psst! Daar zijn jullie! Ik ben Mindy-guh, de dochter van koning Neptuguh. Papa mag het niet weten, maar ik kom jullie helpen.",
        "monsters": "Prinses! Wij moeten door die loopgraaf daar. Maar daar wonen monsters... en wij zijn maar... nou ja. Wij.",
        "kinderen": "Meneer Krabguh zegt dat we nog maar kinderen zijn.",
        "snor": "Dan maken we jullie MANNEN. Met een snor van knabbelkoordraad. Even stilhouden, njeg!",
        "harig": "Patguh... ik voel me ineens zo... harig.",
        "wij": "Wij zijn... wij zijn...",
        "mannen": "WIJ ZIJN MANNEN!",
        "herkent": "En het allermooiste: met een snor herkent niemand ons!",
        "ga": "Ga nu, dappere heren! Door de loopgraaf, en dan naar Schelpenstad: daar ligt papa's kroon. Succes, njeg!",
    }, {"mindy": "Mindy-guh", "sponsguh": "Sponsguh", "patguh": "Patguh", "sponsguh_snor": "Sponsguh", "patguh_snor": "Patguh",
        "beiden": "Sponsguh en Patguh"}),
    "monsters": ("De monsters van de loopgraaf", {
        "donker": "Het is hier wel erg donker... Patguh, hou je mijn hand vast?",
        "hand": "Sponsguh... ik hou mijn handen in mijn zakken.",
        "mannen": "N-niet bang zijn, Patguh! Wij zijn MANNEN! ...Mannen met een héél klein stemmetje.",
        "mooi": "Snif... Dat was het mooiste wat ik ooit heb gehoord. Na de ijscowagen.",
        "dank": "Dank jullie wel, lieve monsters! Kom, Patguh: de bodem over, en aan de overkant weer omhoog!",
    }, {"sponsguh": "Sponsguh", "patguh": "Patguh"}),
    "dennis_snor": ("Dennis en de snor", {
        "gehaald": "Boven! We hebben het gehaald, Patguh. En niemand heeft ons herkend!",
        "oei": "Oei. Dat is Dennis-Mika, de huurling van Plankmika. Doe heel... gewoon. Wij zijn gewoon twee heren.",
        "zoek": "Goedemiddag. Ik zoek een vierkant en een ster. Zijn die hier toevallig langsgekomen?",
        "keurig": "Nee meneer. Hier zijn alleen wij: twee keurige heren. Met een snor.",
        "keurig2": "Héél keurige snor.",
        "dag": "Hm. Twee keurige heren met een snor. Dan stoor ik niet langer. Prettige dag nog, heren.",
        "nies": "Ha... ha... HATSJOE!",
        "vierkant": "...Een vierkant.",
        "ster": "...En een ster.",
        "rennen": "RENNEN!",
    }, {"sponsguh": "Sponsguh", "patguh": "Patguh", "patguh_kaal": "Patguh", "dennis": "Dennis-Mika"}),
    "laars": ("De laars", {
        "au": "...Au. Niks aan de hand. Ik ben oké. Ik ben alleen een beetje... platter.",
        "voet": "Wat wás dat? Een hele grote voet?",
        "laars": "Een duikerslaars. Een héle grote duikerslaars... Patguh, ik krijg een raar gevoel over die souvenirwinkel daar.",
        "sleutelhanger": "Ooh, souvenirs! Ik wil een sleutelhanger met mijn eigen naam erop!",
    }, {"dennis_plat": "Dennis-Mika", "sponsguh": "Sponsguh", "patguh_kaal": "Patguh"}),
    "cycloop": ("De souvenirwinkel van de Cycloop", {
        "souvenirs": "Kijk eens wat een souvenirs! Gedroogde zeesterren... gedroogde kaasjes... een gedroogde... guh?",
        "kroon": "Sponsguh! Daar! Op dat kussentje! De kroon van koning Neptuguh!",
        "plankmika": "Die stiekeme Plankmika heeft hem hier verkocht! Snel, we nemen hem mee naar huis, dan is Meneer Krabguh gered!",
        "nieuw": "Aaah. Nieuwe souvenirs. Een gatenkaasje met een snor. Een sterretje. En een vers vadsje. Wat een leuke dag.",
        "help": "Help! Hij pakt ons op als een zakje chips!",
        "lamp": "Even lekker drogen onder de lamp. Morgen liggen jullie in de etalage. Hehehe.",
        "knisperig": "Patguh... ik word zo... knisperig. Ik heb nog nooit zo'n dorst gehad.",
        "huilen": "Niet huilen, Sponsguh. Huilen is zonde van je water. ...Snif. Ik huil zelf ook een heel klein beetje.",
        "traan": "Patguh! Een traan! Water! Als die traan bij het stopcontact komt...",
        "stopcontact": "...dan gaat het stopcontact pfzzzt en springen de sproeiers aan! Help jij hem rollen? Wij kunnen niet meer bewegen. Wij zijn chips.",
    }, {"cycloop": "De Cycloop", "sponsguh": "Sponsguh", "patguh_kaal": "Patguh", "sponsguh_droog": "Sponsguh", "patguh_droog": "Patguh"}),
    "sproeier": ("De sproeiers", {
        "water": "Water! Heerlijk, heerlijk, HEERLIJK water!",
        "ster": "Ik ben weer een ster! Een natte ster!",
        "leven": "Ik leef weer! Ik ben geen sleutelhanger meer, njeg!",
        "nat": "Neeee! Mijn souvenirs! Alles is NAT! Wie heeft er gehuild in mijn winkel?!",
        "ik": "Ik. Sorry. Het was wel een heel mooie traan.",
        "handdoek": "...Is dat een handdoek? Voor mij? ...Dank je wel, kleintje.",
        "kroon": "De kroon van koning Neptuguh! Op naar huis, voor de zon onder... eh, voor het donker... eh. Op naar huis!",
        "thuis": "Maar hoe komen we thuis? De Knabbelburgermobiel staat aan de verkeerde kant van de loopgraaf.",
        "pier": "Bij de pier misschien? Aan de Leegtezee weet vast iemand raad. Kom!",
    }, {"guh1": "Een souvenir-guh", "cycloop": "De Cycloop", "sponsguh": "Sponsguh", "patguh_kaal": "Patguh",
        "sponsguh_droog": "Sponsguh", "patguh_droog": "Patguh"}),
}

# the look scenes of the hints (StapHint, ~3 s, one run-on caption): name -> (title, caption)
KIJK = {
    "rand": ("De rand van de loopgraaf", "Hier houdt de weg op: bij de loopgraaf. Daar, aan de rand, wacht iemand op jullie."),
    "pad": ("De loopgraaf", "Omlaag langs de westwand, over de donkere bodem, en langs de oostwand weer omhoog."),
    "winkel": ("De souvenirwinkel", "Schelpenstad Souvenirs & Zo: het gebouw met de reuzenschelp op het dak."),
}

# what the companions say on a click, per step sleutel (two lines each)
KLIK = {
    "sponsguh": {
        "5": ["Wat zou daar aan het eind van de weg zijn? Een hele diepe kloof, zeggen ze!", "Lopen is net zoiets als rijden, maar dan met je voeten, njeg."],
        "6": ["Met een snor ben ik nergens bang voor. Bijna nergens.", "Hou je snor vast, Patguh. Hier waait het."],
        "6_b": ["Wat een lieve monsters! Ik stuur ze een kaartje.", "Nog even klimmen, dan zijn we boven!"],
        "7": ["Boven aan de rand begint de weg weer. Kom!", "Hoor jij ook een motor?"],
        "7_b": ["Niet praten, RENNEN!", "Naar de stadspoort! Snel!"],
        "8": ["Die enorme laars... Ik zeg niks. Maar ik denk heel veel.", "De souvenirwinkel is het gebouw met de schelp op het dak!"],
        "8_b": ["Kraak... knisper...", "Rol die traan, vriend... ik ben bijna een cracker..."],
    },
    "patguh": {
        "5": ["Ik heb zin in ijs. Ik heb altijd zin in ijs.", "Weet jij waar we heen gaan? Ik ook niet!"],
        "6": ["Mijn snor kriebelt.", "Ik ben een man. Ik ben een man. Ik ben... wat was ik ook alweer?"],
        "6_b": ["Ik wil nog een liedje!", "Die monsters zingen mooier dan Octo."],
        "7": ["Ik voel een niesbui aankomen. Ach nee, toch niet.", "Mijn snor is mijn beste vriend. Na jou."],
        "7_b": ["Waarom rennen we? O ja!", "Mijn snor! Waar is mijn snor?!"],
        "8": ["Souvenirs! Ik wil er een met mijn eigen gezicht erop.", "Zou die winkel ook ijs verkopen?"],
        "8_b": ["Snif... ik huil nog wel een traan, hoor.", "Ik ben zo droog als een korstje."],
    },
}

TEKSTEN = {
    G + "naam.sponsguh": "Sponsguh",
    G + "naam.patguh": "Patguh",
    G + "banter.lopen": "De Knabbelburgermobiel kan niet over de loopgraaf. Vanaf hier lopen we, njeg!",
    G + "banter.benen": "Lopen? Met mijn eigen benen? Spannend.",
    G + "hint.rand": "Loop met Sponsguh en Patguh over de weg naar het oosten, de heuvel op, tot de weg ophoudt bij de loopgraaf: %s.",
    G + "beloning.snor": "Mindy-guh knoopt ook een snor van knabbelkoordraad voor je guhs! (Guh-kleding: de knabbelkoordraadsnor)",
    G + "monsters.omhoog": "Steek de bodem over en klim langs de oostwand omhoog",
    G + "jacht.titel": "Rennen!",
    G + "jacht.regel": "Volg de weg naar de stadspoort. Laat Dennis je niet pakken!",
    G + "jacht.uitleg": "Dennis-Mika zit achter je aan op zijn motor! Ren (sprinten!) over de weg de heuvel af, door de stadspoort van "
                        "Schelpenstad. Pakt hij je, dan duwt hij je alleen terug naar het laatste rustpunt, njeg.",
    G + "jacht.weer": "Daar is Dennis-Mika weer! Rennen, naar de stadspoort!",
    G + "jacht.balk": "Schelpenstad: nog %s blokken",
    G + "jacht.gepakt": "Gepakt!",
    G + "jacht.gepakt_start": "Dennis duwt je terug. Nog een keer: sprinten!",
    G + "jacht.gepakt_half": "Dennis duwt je terug naar halverwege. Nog een keer!",
    G + "jacht.half": "Halverwege! Pakt Dennis je nu, dan begin je hier.",
    G + "jacht.poort": "De stadspoort!",
    G + "jacht.kwijt": "Dennis-Mika is je kwijt... Kom terug op de weg oost van de loopgraaf, dan gaat de achtervolging verder.",
    G + "traan.titel": "De traan!",
    G + "traan.regel": "Kantel de tafel met WASD: rol de traan naar het stopcontact",
    G + "traan.uitleg": "Je zit vast in de hand van de Cycloop, maar de tafel kun je kantelen! W, A, S en D kantelen hem van je af, naar "
                        "links, naar je toe en naar rechts. Rol Patguhs traan over het natte spoor (de blauwe stipjes) naar het "
                        "stopcontact aan de muur. Van het spoor af? Dan huilt Patguh gewoon een nieuwe. Sluipen = loslaten.",
    G + "traan.weer": "Weer onder de drooglamp! Kantel de tafel met WASD en rol de traan naar het stopcontact.",
    G + "traan.los": "Je wurmt je los. Ga terug naar de droogtafel om het opnieuw te proberen.",
    G + "traan.balk": "Traan: %s%%  |  Tranen: %s  |  Droog: %s",
    G + "traan.droog5": "Ik word... een... cracker...",
    G + "traan.droog10": "Ik ben een zoutje, njeg.",
    G + "traan.nieuw.0": "Snif. Hier is er nog een.",
    G + "traan.nieuw.1": "Geen zorgen, ik heb er nog genoeg!",
    G + "traan.nieuw.2": "Buhuu... alsjeblieft, een verse.",
    G + "traan.binnen": "Pfzzzt!",
    G + "klik.monster.0": "Brrroemm... (het monster neuriet zachtjes)",
    G + "klik.monster.1": "(het monster zwaait verlegen met een klauw)",
    G + "klik.monster.2": "Lalala... la? (hij is zijn tekst kwijt)",
    G + "klik.cycloop": "Kom binnen, kom binnen! Alles is gedroogd, en alles is leuk. Hehe.",
    "item.guhs.einde2_sp_d2_weg_snor": "Knabbelkoordraadsnor",
    "gui.guhs.kledingbron.einde2_sp_d2_weg": "Sponsguh Kaasbroek: de loopgraaf",
}
for _wie, _lijnen in KLIK.items():
    for _sleutel, _twee in _lijnen.items():
        for _i, _t in enumerate(_twee):
            TEKSTEN[f"{G}klik.{_wie}.{_sleutel}.{_i}"] = _t

# FTB: one quest per step (key einde2_sp_d2_weg_k<stap>, task: the step advancement <line>_stap_<stap + 1>)
FTB = [
    ("einde2_sp_d2_weg_k5", "Mannen met een snor", "De Knabbelburgermobiel kan niet over de loopgraaf. Loop met Sponsguh en Patguh naar de "
     "westrand: daar wacht prinses Mindy-guh met iets harigs.", "guhs:einde2_sp_d2_weg_snor", "sponsguh_kroon_stap_6"),
    ("einde2_sp_d2_weg_k6", "De monsters van de loopgraaf", "Omlaag, de donkere bodem over en weer omhoog. Brullen de monsters? Of... "
     "zingen ze?", "guhs:loopgraafmonster_spawn_egg", "sponsguh_kroon_stap_7"),
    ("einde2_sp_d2_weg_k7", "Dennis en de snor", "Dennis-Mika zoekt een vierkant en een ster. Gelukkig zijn jullie twee keurige heren met "
     "een snor. Toch?", "guhs:dennis_mika_spawn_egg", "sponsguh_kroon_stap_8"),
    ("einde2_sp_d2_weg_k8", "De Cycloop", "In de souvenirwinkel van Schelpenstad ligt de kroon van koning Neptuguh. En de drooglamp staat al "
     "aan...", "guhs:cycloop_mika_spawn_egg", "sponsguh_kroon_stap_9"),
]
