"""
1.10.0 "Het Vadsrijk", the klassen slice (CONTRACT_VADSRIJK.md). Java: feature/vadsrijk/klassen.

  - the 9 class weapons of this slice (item models + 16x16 pixel-art textures drawn here, textures/vadsrijk/klassen/);
  - every klassen text (Dutch; en_us gets the same Dutch texts): classes, titles, skills, the 3 skill trees, advancement,
    the screens, hints, weapon names + flavour lines.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_klassen.py
"""
import json
import os

K = "vadsrijk.klassen."

# weapon id -> (name, model parent, [3 flavour lines])
WAPENS = {
    "klodderkling": ("Klodderkling", "minecraft:item/handheld", [
        "Een zwaard van gestold slijm. Elke klap slurpt een beetje vads terug.",
        "Plakt niet aan je handen. Wel aan je vijanden.",
        "Gesmeed door een klodder die het zelf niet meer nodig had.",
    ]),
    "pluisboog": ("Pluisboog", "minecraft:item/handheld", [
        "Pijlen met roze pluimpjes. Ze kietelen, en dan doen ze pijn.",
        "De pees is van gespannen knuffelwol. Njeg!",
        "Mikken is makkelijk: gewoon denken aan iets liefs.",
    ]),
    "druppelboog": ("Druppelboog", "minecraft:item/handheld", [
        "Hij lekt een beetje. Dat hoort zo.",
        "Elke pijl zit vol met een spetter vijvervads.",
        "Gevonden onder een waterval die heel hard 'blub' zei.",
    ]),
    "snoepkruisboog": ("Snoepkruisboog", "minecraft:item/handheld", [
        "Schiet zuurstokbouten. Gloeiend heet zuurstok, dat dan weer wel.",
        "Langzaam herladen, keihard verwennen.",
        "Niet aflikken. Echt niet. Ook niet een klein beetje.",
    ]),
    "eikelkatapult": ("Eikelkatapult", "minecraft:item/handheld", [
        "Eikels uit het Sporenwoud. Ze ploffen open in een wolkje zwam.",
        "Een eekhoorn heeft hier ooit heel boos om gekeken.",
        "Plok. Plok. Plok. Vahoeg!",
    ]),
    "rozenstaf": ("Rozenstaf", "minecraft:item/handheld", [
        "De bol bovenop gloeit roze zodra er een ontvadst wezen in de buurt is.",
        "Ruikt naar suikerspin en een beetje naar toverkracht.",
        "Gemaakt van de tak waar de allereerste guh op heeft gedut.",
    ]),
    "vlammenstaf": ("Vlammenstaf", "minecraft:item/handheld", [
        "Warm aan de bovenkant, gezellig aan de onderkant.",
        "Wordt ook gebruikt om marshmallows te roosteren. Vooral daarvoor eigenlijk.",
        "Vuur is sterk tegen zwammen. De zwammen weten dat ook.",
    ]),
    "zwamstokje": ("Zwamstokje", "minecraft:item/handheld", [
        "Een toverstokje met een paddenstoeltje erop. Het groeit nog een beetje.",
        "Zwaaien, wijzen, sporen. In die volgorde.",
        "Klein, snel en verdacht vrolijk.",
    ]),
    "slijmboek": ("Slijmboek", "minecraft:item/generated", [
        "Alle bladzijden plakken aan elkaar. De spreuken werken toch.",
        "Hoofdstuk 1: Blub. Hoofdstuk 2: Blub blub. Hoofdstuk 3: spannend.",
        "Wie eruit voorleest, geneest een beetje. Wie erin bijt, niet.",
    ]),
}

SKILLS = {
    # id: (name or None for kern-named, uitleg, kort)
    "kussenklap": (None, "Zwaai in een kegel voor je (4,5 blokken) voor 1,5x wapenschade met flinke knockback. 20 energie, 3 s.",
                   "Kegelzwaai met knockback."),
    "stormsprong": (None, "Spring naar je doel (of 8 blokken vooruit). De landing is een schokgolf: 1,3x schade rondom. 25 energie, 8 s.",
                    "Sprong naar je doel met een landingsschok."),
    "knuffelschild": (None, "3 s lang doen klappen op jou maar 20 %, de helft kaatst terug en alle vijanden in 10 blokken kijken naar jou. 30 energie, 15 s.",
                      "3 s blokken en terugkaatsen, trekt aggro."),
    "vahoege_plof": (None, "ULTIEM: een reuzenkussen valt op je doel: 3x schade en 3 s verdoofd in 6 blokken, je groep in 8 blokken healt 15 %.",
                     "Reuzenkussen: verdooft alles, healt de groep."),
    "pluizenregen": ("Pluizenregen", "Laat 3 golven pijlen regenen op de plek waar je mikt (straal 4): elke golf 0,6x wapenschade op alles eronder. 25 energie, 6 s.",
                     "Pijlen regenen op een gebied."),
    "rolsalto": ("Rolsalto", "Een achterwaartse salto weg van waar je kijkt, met even Snelpootjes. Je volgende schot binnen 5 s is gegarandeerd kritiek. 20 energie, 8 s.",
                 "Salto achteruit; volgende schot kritiek."),
    "knuffelpijl": ("Knuffelpijl", "Een hartjespijl. Raakt hij een teamgenoot, dan healt die 12 % van zijn Vads + 0,5x je wapenschade. Een vijand krijgt 2x Roze schade. 25 energie, 7 s.",
                    "Healt een teamgenoot of prikt een vijand."),
    "sterrenschot": ("Sterrenschot", "ULTIEM: 1 s opladen (je loopt trager), dan een enorme laserpijl van 40 blokken dwars door alles heen: 4x wapenschade.",
                     "Opladen, dan een laserpijl door alles."),
    "roze_vuurbal": ("Roze Vuurbal", "Een trage bal die ontploft (straal 3,5) voor 2,8x wapenschade in het element van je wapen. Vuur laat branden, Natuur spoort, Water plakt. 25 energie, 4,5 s.",
                     "Ontploft in het element van je staf."),
    "blink": ("Blink", "Teleporteer tot 10 blokken in je kijkrichting (stopt netjes voor muren). 20 energie, 6 s.",
              "Korte teleport waar je kijkt."),
    "vadsbron": ("Vadsbron", "Een heal-cirkel onder je voeten (straal 4, 6 s): iedereen erin healt elke seconde 4 % van zijn Vads + 0,25x je wapenschade. 35 energie, 14 s.",
                 "Heal-cirkel op de grond voor iedereen."),
    "meteoorguh": ("Meteoorguh", "ULTIEM: een gigantische guh-meteoor valt waar je mikt: 5x Roze schade in 6 blokken, daarna 6 s roze vuur op die plek.",
                   "Reuzenmeteoor + een zone roze vuur."),
}

KNOPEN = {
    # Knuffelridder
    "r_pluisvacht": ("Dikke Pluisvacht", "Meer Vads en Verdediging. De basis van elke stevige knuffel."),
    "r_knuffelspier": ("Knuffelspieren", "Meer Kracht en harder kritiek. Knuffels die aankomen."),
    "r_aanloop": ("Aanloopje", "Stormsprong laadt 3 s sneller op en na je sprong heb je 5 s Snelpootjes."),
    "r_vuurkussen": ("Vuurkussen", "Kussenklap zet vijanden 4 s in brand (elke seconde 25 % van je wapenschade). Kan niet samen met Slijmkussen."),
    "r_slijmkussen": ("Slijmkussen", "Kussenklap trekt vijanden naar je toe in plaats van weg en maakt ze 4 s Plakkerig. Kan niet samen met Vuurkussen."),
    "r_doornknuffel": ("Doornenknuffel", "Knuffelschild kaatst 100 % terug in plaats van 50 %, en opsteken healt je 8 % van je Vads."),
    "r_schokgolf": ("Schokgolf", "De landing van Stormsprong verdooft vijanden 1,5 s."),
    "r_taai": ("Taaie Knuffel", "Onder 35 % Vads krijg je 30 % minder schade."),
    "r_beschermknuffel": ("Beschermknuffel", "Knuffelschild geeft je groep in 8 blokken ook 5 s een Knuffelschild van 15 % van jouw max Vads."),
    "r_reuzenkussen": ("Reuzenkussen", "Vahoege Plof healt 30 % in plaats van 15 % en geeft de groep 6 s Vahoeg (+20 % schade)."),
    "r_kussenstorm": ("Kussenstorm", "Kussenklap doet 25 % meer schade, laadt 1,5 s sneller op en geeft 8 energie terug."),
    "r_bolwerk": ("Knuffelbolwerk", "Zolang je Knuffelschild op staat, krijgen jij en je groep in 8 blokken 25 % minder schade van vijanden."),
    # Pluisjager
    "j_scherpoog": ("Scherp Oog", "Meer kans op kritiek en hardere kritieke klappen."),
    "j_lichtvoetig": ("Lichtvoetig", "Sneller lopen en wat extra Vads. Altijd net buiten bereik."),
    "j_snelle_pees": ("Snelle Pees", "15 % Aanvalssnelheid: je schiet merkbaar sneller."),
    "j_vuurregen": ("Vuurregen", "Pluizenregen zet alles eronder 4 s in brand. Kan niet samen met Kleefregen."),
    "j_kleefregen": ("Kleefregen", "Pluizenregen maakt vijanden Plakkerig en regent een 4e golf. Kan niet samen met Vuurregen."),
    "j_doorboor": ("Doorboorpijl", "Je gewone schoten gaan door 1 extra vijand heen (kruisbogen dus door 2)."),
    "j_valkenoog": ("Valkenoog", "Schoten op vijanden verder dan 12 blokken doen 25 % meer schade."),
    "j_plakbom": ("Plakbommetje", "Rolsalto laat een slijmbommetje achter: vijanden in 3 blokken plakken 3 s en krijgen 0,8x schade. Rolsalto laadt 2 s sneller op."),
    "j_hartjespijl": ("Hartjespijl", "Knuffelpijl healt 60 % meer en geeft je teamgenoot 4 s Snelpootjes. Op vijanden 30 % meer schade."),
    "j_vallende_ster": ("Vallende Ster", "Sterrenschot doet 50 % meer schade en geeft 25 % van je ultieme lading terug."),
    "j_pluizenmantel": ("Pluizenmantel", "Na Rolsalto heb je 4 s een Knuffelschild van 20 % van je max Vads."),
    "j_jachtroes": ("Jachtroes", "Elk 4e gewone schot is gegarandeerd kritiek en schiet 2 extra pijlen in een waaier."),
    # Vadsmagier
    "m_wijs": ("Wijze Pluis", "Meer Wijsheid: al je toverkunst wordt sterker."),
    "m_elementkenner": ("Elementkenner", "12 % meer Elementschade. Ken je driehoek: Vuur > Zwam > Slijm > Vuur."),
    "m_zachte_handjes": ("Zachte Handjes", "15 % meer Heal-kracht en wat extra Vads."),
    "m_reuzenbal": ("Reuzenbal", "Roze Vuurbal ontploft 50 % groter en doet 20 % meer schade. Kan niet samen met Drievuur."),
    "m_drievuur": ("Drievuur", "Roze Vuurbal schiet 3 ballen in een waaier (elk 65 %, kleinere ontploffing). Kan niet samen met Reuzenbal."),
    "m_glitterblink": ("Glitterblink", "Waar je vandaan Blinkt, ontploft een glitterbom: 1,2x schade in 3 blokken."),
    "m_energievonk": ("Energievonk", "Elke toverbol die raakt geeft je 3 Vadsenergie terug."),
    "m_grote_bron": ("Grote Bron", "Vadsbron is 50 % groter en geeft iedereen erin Vahoeg (+15 % schade)."),
    "m_bronschild": ("Bronschild", "Wie voor het eerst in je Vadsbron stapt, krijgt 5 s een Knuffelschild van 12 % van jouw max Vads."),
    "m_kettingbolt": ("Kettingbolt", "Je toverbollen springen door naar een tweede vijand in 6 blokken (60 % schade)."),
    "m_meteoorregen": ("Meteoorregen", "Na Meteoorguh vallen er nog 3 kleine meteoren rondom (elk 1,5x schade)."),
    "m_tijdsprong": ("Tijdsprongetje", "Blink geeft 15 energie terug en Roze Vuurbal is meteen weer klaar."),
}

ADV = {
    # klasse: (titel1, titel2, onder1, onder2, uitleg1, uitleg2)
    "knuffelridder": ("Kussenkampioen", "Vahoege Paladijn", "Nieuw: Dubbelklap!", "Nieuw: Paladijnsaura!",
                      "Dubbelklap: Kussenklap zwaait na een tel nog een keer (60 % schade). Plus Verdediging en Kracht.",
                      "Paladijnsaura: jij en je groep in 10 blokken healen elke 2 s 1,5 % Vads. Plus 120 Vads en Heal-kracht."),
    "pluisjager": ("Sterrenschutter", "Vahoege Hemelvalk", "Nieuw: Saltosalvo!", "Nieuw: Valkenblik!",
                   "Saltosalvo: midden in je Rolsalto schiet je 3 pijlen op de dichtstbijzijnde vijand. Plus Behendigheid en Loopsnelheid.",
                   "Valkenblik: je pijlen buigen een beetje af naar vijanden. Plus 8 % kritiek en 15 % kritiekschade."),
    "vadsmagier": ("Sterrenwever", "Vahoege Aartsmagiër", "Nieuw: Elementplas!", "Nieuw: Vadsoverloop!",
                   "Elementplas: Roze Vuurbal laat 3 s een plas van zijn element achter die schade doet. Plus Wijsheid en Vads.",
                   "Vadsoverloop: elke 3e vaardigheid kost geen energie. Plus 10 % Cooldownreductie en 8 % Elementschade."),
}

TEKSTEN = {
    "key.guhs.vr_kl_boom": "Skillboom (Vadsrijk)",
    K + "klasse.pluisjager": "Pluisjager",
    K + "klasse.vadsmagier": "Vadsmagiër",
    K + "kies.rol.knuffelridder": "Zware knuffelbruiser",
    K + "kies.rol.pluisjager": "Snelle boogschutter",
    K + "kies.rol.vadsmagier": "Elementen + genezen",
    K + "kies.kop": "Kies je klasse, rekruut!",
    K + "kies.onderkop": "Je kunt later wisselen in de ordetempel van Zwamzoet. Elke klasse houdt zijn eigen niveau.",
    K + "kies.knop": "Word %s!",
    K + "kies.titel": "%s!",
    K + "kies.onder.knuffelridder": "Voorop in het gevecht, knuffels uitdelen. Vahoeg!",
    K + "kies.onder.pluisjager": "Pluimpjes klaar, pees gespannen. Njeg!",
    K + "kies.onder.vadsmagier": "De roze toverkracht stroomt door je pootjes. Vads!",
    K + "kies.al": "Je bent al %s. Wisselen kan in de ordetempel van Zwamzoet.",
    K + "wissel.onder": "Niveau %s · je wapens van deze klasse werken weer",
    K + "wapen.verkeerd": "Dit wapen is voor een %s. Wissel van klasse in de ordetempel.",
    K + "wapen.te_hoog": "Dit wapen is van niveau %s. Nog even doorgroeien!",
    K + "boom.nee.onbekend": "Die knoop bestaat niet.",
    K + "boom.nee.al": "Die heb je al.",
    K + "boom.nee.uitgesloten": "Je hebt de andere keuze al genomen.",
    K + "boom.nee.vereist": "Leer eerst de knoop erboven.",
    K + "boom.nee.punten": "Te weinig skillpunten.",
    K + "respec.klaar": "Je skillboom is leeg en je punten zijn terug. Kies opnieuw! (Volgende gratis respec over een uur.)",
    K + "respec.nee.cooldown": "Je kunt pas weer resetten over %s.",
    K + "respec.nee.leeg": "Er valt niks te resetten.",
    K + "adv.chat": "✦ Je bent nu %s! %s",
    K + "adv.klaar": "✦ Je bent klaar voor je job-advancement tot %s! Ga naar de ordetempel in Zwamzoet.",
    K + "cmd.info": "%s · niveau %s · %s skillpunten vrij · knopen: %s",
    # the screens
    K + "scherm.boom": "Skillboom",
    K + "scherm.niveau": "Niveau %s %s",
    K + "scherm.nv": "Nv %s",
    K + "scherm.punten": "Skillpunten: %s",
    K + "scherm.punten.tip": "Klik een knoop om te leren",
    K + "scherm.kost": "Kost %s punten",
    K + "scherm.geleerd": "✔ Geleerd",
    K + "scherm.klik": "Klik om te kiezen",
    K + "scherm.bevestig": "Klik nog een keer om te leren!",
    K + "scherm.eerst": "Eerst nodig: %s",
    K + "scherm.uitgesloten": "Kan niet samen met: %s",
    K + "scherm.skills": "Vaardigheden",
    # fix round 2 (g-ui): the skill tooltip (klassen/client/SkillInfo)
    "vadsrijk.klassen.skilltip.energie": "%s energie",
    "vadsrijk.klassen.skilltip.ult": "Kost een volle ultieme lading",
    "vadsrijk.klassen.skilltip.gratis": "Kost niks",
    "vadsrijk.klassen.skilltip.cooldown": "%s s afkoeltijd",
    "vadsrijk.klassen.skilltip.verbeteringen": "Verbeteringen (boom en job-advancement):",
    "vadsrijk.klassen.skilltip.verbetert": "Verbetert: %s",
    "vadsrijk.klassen.skilltip.open_boom": "Open je skillboom (K) om te zien wat je al geleerd hebt.",
    "vadsrijk.klassen.skilluitleg.knuffelsprong": "Een flinke sprong vooruit (zo'n 20 blokken). Elke klasse kan het en je landt altijd zacht.",
    K + "scherm.ordetempel": "Resetten (gratis, 1x per uur) en van klasse wisselen doe je in de ordetempel van Zwamzoet.",
    K + "scherm.balie": "Ordetempel",
    K + "scherm.huidig": "Nu",
    K + "scherm.wissel": "Wissel",
    K + "scherm.wissel.tip": "Elke klasse houdt zijn eigen niveau en skillboom. Wapens horen bij een klasse, harnassen deel je.",
    K + "scherm.respec": "Skillboom resetten",
    K + "scherm.respec.wacht": "Reset over %s",
    K + "scherm.respec.tip": "Al je punten in deze klasse terug. Gratis, maar maar één keer per uur (echte tijd). Advancements blijven.",
    K + "scherm.respec.uitleg": "Gratis reset, 1x per uur:",
    K + "scherm.respec.uitleg_wacht": "Reset weer mogelijk over:",
    K + "scherm.adv": "✦ Ceremonie!",
    K + "scherm.adv.tip": "Je job-advancement: een nieuwe titel en een nieuwe kracht.",
    K + "scherm.adv.kop": "Job-advancement",
    K + "scherm.adv.lv": "op niveau %s",
    K + "scherm.adv.open": "Behaald!",
    K + "scherm.adv.dicht": "Vanaf niveau %s, in de ordetempel.",
    K + "scherm.adv.klaar": "Je bent er klaar voor! Klik op Ceremonie.",
    K + "scherm.adv.alles": "Alle advancements behaald. Vahoeg!",
    K + "scherm.adv.nog": "Volgende advancement op niveau %s.",
    # hints
    K + "tip.welkom.titel": "Kies een klasse",
    K + "tip.welkom": "In het klasse-tempeltje van Knuffelkamp kies je Knuffelridder, Pluisjager of Vadsmagiër. Tot die tijd mep je als Knuffelridder.",
    K + "tip.gekozen.titel": "Je eerste wapen",
    K + "tip.gekozen": "Je startwapen zit in je rugzak. Pluisjagers en magiërs: houd de linkermuis ingedrukt om te schieten.",
    K + "tip.punt.titel": "Skillpunt!",
    K + "tip.punt": "Elk niveau geeft een skillpunt. Druk op K voor je skillboom. Je kunt niet alles leren, dus kies slim!",
    K + "tip.boom.titel": "Skillboom",
    K + "tip.boom": "Klik een knoop twee keer om hem te leren. Van de twee varianten van je R-vaardigheid kies je er één.",
    K + "tip.balie.titel": "De ordetempel",
    K + "tip.balie": "Hier wissel je van klasse (elk houdt zijn eigen niveau) en reset je je skillboom gratis, één keer per uur.",
    K + "tip.adv.titel": "Job-advancement!",
    K + "tip.adv": "Ga naar de ordetempel in Zwamzoet voor je ceremonie: een nieuwe titel en een nieuwe kracht.",
}

for _id, (_naam, _parent, _fl) in WAPENS.items():
    TEKSTEN["item.guhs.vr_kl_" + _id] = _naam
    TEKSTEN["vadsrijk.loot." + _id] = _naam
    for _i, _t in enumerate(_fl):
        TEKSTEN[f"vadsrijk.loot.{_id}.flavour.{_i}"] = _t
for _id, (_naam, _uitleg, _kort) in SKILLS.items():
    if _naam:
        TEKSTEN[K + "skill." + _id] = _naam
    TEKSTEN[K + "skilluitleg." + _id] = _uitleg
    TEKSTEN[K + "kort." + _id] = _kort
for _id, (_naam, _uitleg) in KNOPEN.items():
    TEKSTEN[K + "knoop." + _id] = _naam
    TEKSTEN[K + "knoop." + _id + ".uitleg"] = _uitleg
for _k, (_t1, _t2, _o1, _o2, _u1, _u2) in ADV.items():
    TEKSTEN[f"{K}titel.{_k}.1"] = _t1
    TEKSTEN[f"{K}titel.{_k}.2"] = _t2
    TEKSTEN[f"{K}adv.onder.{_k}.1"] = _o1
    TEKSTEN[f"{K}adv.onder.{_k}.2"] = _o2
    TEKSTEN[f"{K}adv.uitleg.{_k}.1"] = _u1
    TEKSTEN[f"{K}adv.uitleg.{_k}.2"] = _u2
    _p = {"knuffelridder": "r", "pluisjager": "j", "vadsmagier": "m"}[_k]
    TEKSTEN[f"{K}knoop.{_p}_adv1"] = _t1
    TEKSTEN[f"{K}knoop.{_p}_adv2"] = _t2


# ---- pixel art ---------------------------------------------------------------------------------------------------------

def _rgb(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


class _Doek:
    def __init__(self):
        self.p = {}

    def px(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.p[(x, y)] = _rgb(c) if isinstance(c, int) else c

    def lijn(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.px(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def bol(self, cx, cy, r, c, licht=None):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 0.3:
                    self.px(x, y, c)
        if licht is not None:
            self.px(int(cx - r / 2), int(cy - r / 2), licht)

    def rechthoek(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def beeld(self):
        from PIL import Image
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        # a dark outline around everything (darker version of the neighbour), for readability on any background
        rand = {}
        for (x, y), c in self.p.items():
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in self.p and (nx, ny) not in rand:
                    rand[(nx, ny)] = (int(c[0] * 0.35), int(c[1] * 0.3), int(c[2] * 0.38), 255)
        for (x, y), c in rand.items():
            img.putpixel((x, y), c)
        for (x, y), c in self.p.items():
            img.putpixel((x, y), c)
        return img


def _zwaard(blad, licht, greep=0x8A5A3C, pareer=0xFF7AC8):
    d = _Doek()
    for i in range(5, 14):
        d.px(i, 15 - i, blad)
        d.px(i + 1, 15 - i, licht if i % 2 else blad)
    d.px(14, 1, licht)
    d.lijn(2, 10, 6, 14, pareer)
    for i in range(2, 5):
        d.px(i, 15 - i, greep)
    d.px(1, 14, pareer)
    return d


def _boog(hout, licht, pluis=0xFFFFFF, pees=0xF0E0F0):
    d = _Doek()
    pts = [(3, 1), (6, 1), (9, 2), (11, 4), (13, 6), (14, 9), (14, 12)]
    for (a, b), (c, e) in zip(pts, pts[1:]):
        d.lijn(a, b, c, e, hout)
    d.px(9, 3, licht)
    d.px(12, 6, licht)
    d.lijn(3, 2, 13, 12, pees)
    d.bol(2, 1, 1, pluis)
    d.bol(14, 13, 1, pluis)
    # an arrow on the string
    d.lijn(2, 13, 9, 6, 0xC89A6A)
    d.px(10, 5, 0xD8D8E0)
    d.px(10, 4, 0xD8D8E0)
    d.px(11, 5, 0xD8D8E0)
    d.px(2, 12, 0xFF7AC8)
    d.px(1, 13, 0xFF7AC8)
    return d


def _kruisboog():
    d = _Doek()
    # stock diagonal, candy stripes
    for i in range(2, 13):
        d.px(i, 15 - i, 0xFFFFFF if i % 2 else 0xE8304A)
        d.px(i + 1, 15 - i, 0xC8203A if i % 2 else 0xFFE0E8)
    # arms perpendicular near the front
    d.lijn(6, 3, 13, 10, 0x8A4A2A)
    d.lijn(6, 4, 12, 10, 0xB0683C)
    d.lijn(6, 3, 13, 10, 0x8A4A2A)
    d.lijn(7, 2, 14, 9, 0xF0E0F0)
    d.px(13, 2, 0xFFD24A)
    d.px(14, 1, 0xFFD24A)
    return d


def _katapult():
    d = _Doek()
    hout, licht = 0x8A5A3C, 0xB07850
    d.lijn(3, 14, 7, 10, hout)
    d.lijn(4, 14, 8, 10, licht)
    d.lijn(7, 10, 6, 4, hout)
    d.lijn(8, 10, 12, 6, hout)
    d.px(6, 3, licht)
    d.px(12, 5, licht)
    d.lijn(6, 4, 9, 8, 0xFF9AD5)
    d.lijn(12, 6, 9, 8, 0xFF9AD5)
    # the acorn
    d.bol(10, 10, 1.5, 0xC8843C, 0xE8A860)
    d.rechthoek(9, 8, 11, 8, 0x6A4424)
    return d


def _staf(orb, licht, gloed):
    d = _Doek()
    for i in range(1, 11):
        d.px(i, 15 - i, 0x8A5A3C if i % 3 else 0xB07850)
    d.px(2, 14, 0xFFD24A)
    d.bol(12, 3, 2.4, orb, licht)
    d.px(14, 1, gloed)
    d.px(9, 1, gloed)
    d.px(15, 6, gloed)
    d.lijn(10, 5, 9, 6, 0xFFD24A)
    return d


def _stokje():
    d = _Doek()
    for i in range(2, 10):
        d.px(i, 15 - i, 0xF0E8D8)
        d.px(i + 1, 15 - i, 0xD8C8B0)
    # little mushroom cap
    d.bol(11, 4, 2.6, 0xE8406A)
    d.px(10, 3, 0xFFFFFF)
    d.px(12, 2, 0xFFFFFF)
    d.px(13, 5, 0xFFFFFF)
    d.rechthoek(9, 6, 12, 6, 0xF8E8D8)
    d.px(14, 1, 0x9AF07A)
    return d


def _boek():
    d = _Doek()
    d.rechthoek(2, 2, 13, 14, 0x3FA9B8)
    d.rechthoek(3, 3, 12, 13, 0x58C8D8)
    d.rechthoek(13, 3, 14, 14, 0xF8F0E0)
    d.rechthoek(2, 2, 2, 14, 0x2A7A88)
    # a slime drop with a guh face
    d.bol(8, 8, 3.1, 0x8FE07A)
    d.px(8, 4, 0x8FE07A)
    d.px(7, 8, 0x1A1A1A)
    d.px(9, 8, 0x1A1A1A)
    d.px(6, 9, 0xFF9AC8)
    d.px(10, 9, 0xFF9AC8)
    d.px(7, 6, 0xD8FFD0)
    return d


TEKENINGEN = {
    "klodderkling": lambda: _zwaard(0x6FC8F0, 0xC8F0FF, pareer=0x3FA9FF),
    "pluisboog": lambda: _boog(0xE85AA8, 0xFF9AD5),
    "druppelboog": lambda: _boog(0x3F8AD8, 0x8AC8FF, pluis=0xC8F0FF),
    "snoepkruisboog": _kruisboog,
    "eikelkatapult": _katapult,
    "rozenstaf": lambda: _staf(0xFF7AC8, 0xFFE0F2, 0xFFC2E8),
    "vlammenstaf": lambda: _staf(0xFF6A2A, 0xFFE070, 0xFFB040),
    "zwamstokje": _stokje,
    "slijmboek": _boek,
}


def _assets(w, a, png=True):
    for item, (_naam, parent, _fl) in WAPENS.items():
        w(os.path.join(a, "items", "vr_kl_" + item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_kl_" + item}})
        w(os.path.join(a, "models", "item", "vr_kl_" + item + ".json"),
          {"parent": parent, "textures": {"layer0": "guhs:item/vadsrijk/klassen/" + item}})
        if png:
            doel = os.path.join(a, "textures", "item", "vadsrijk", "klassen", item + ".png")   # (the item atlas only takes textures/item; integration fix)
            os.makedirs(os.path.dirname(doel), exist_ok=True)
            TEKENINGEN[item]().beeld().save(doel)


def build(h):
    _assets(h.w, h.A)
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    _assets(_schrijf, os.path.join(a, "assets", "guhs"))
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_klassen: {len(WAPENS)} weapons + {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
