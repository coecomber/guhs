"""
1.10.0 "Het Vadsrijk", night round T, slice t-ambacht: the Ambachten (professions). Java: feature/vadsrijk/ambacht.

  - pixel art (16x16, the economie painter `Doek`) for the materials, boss cores, the Ambachtskist, the dishes, the potions and the
    Maatwerk/Meesterwerk gear; item models + item definitions;
  - every text (Dutch; en_us gets real English for the short UI texts, the same Dutch for names and stories).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_ambacht.py
"""
import json
import math
import os

from vadsrijk_economie import Doek, _licht, zwaard, boog, staf, _orb, helm, borst, broek, sloffen, drankje

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
ITEM_TEX = "vadsrijk/ambacht"


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _hex(k):
    return ((k >> 16) & 255, (k >> 8) & 255, k & 255)


# ======================================================================================================================
# data (same ids as the Java)
# ======================================================================================================================
AMBACHTEN = {
    "mijnen": ("Mijnen", "Mining"), "houthakken": ("Houthakken", "Woodcutting"), "plukken": ("Plukken", "Picking"),
    "vissen": ("Vissen", "Fishing"), "imkeren": ("Imkeren", "Beekeeping"), "smeden": ("Smeden", "Smithing"),
    "naaien": ("Naaien", "Sewing"), "koken": ("Koken", "Cooking"), "brouwen": ("Brouwen", "Brewing"),
}

# id: (soort, kleur, naam, uitleg)
MATERIALEN = {
    "pluisijzer": ("erts", 0xC8B0C0, "Pluisijzer", "Zacht roze ijzer uit de Druipgrotten. Voor de smidse (T1)."),
    "kandijzilver": ("erts", 0xD8E8F8, "Kandijzilver", "Zilver met suikerglans uit de Splinterkloof (T2)."),
    "amberkoper": ("erts", 0xE8904A, "Amberkoper", "Warm koper uit de wortelgangen van De Schemerwortel (T3)."),
    "glinstergruis": ("gruis", 0xFFE070, "Glinstergruis", "Zeldzaam: uit een glinsterende ader. Voor Meesterwerk en stralende kristallen."),
    "zwamsteel": ("hout", 0xE8D8C0, "Zwamsteel", "Een stuk steel van een reuzenzwam uit het Sporenwoud (T1)."),
    "kristalden": ("hout", 0x8AC8D8, "Kristalden", "Dennenhout met ijskristallen uit het Kristalbos (T2)."),
    "schemerhout": ("hout", 0x6A4A7A, "Schemerhout", "Donker, licht glanzend hout uit de Wortelvelden (T3)."),
    "roze_hars": ("hars", 0xFF8AB0, "Roze Hars", "Kleverig roze sap van schemerhout (T3)."),
    "oerbast": ("hout", 0x8A6A3A, "Oerbast", "Zeldzaam: bast van een heel oude boom. Ruikt naar vroeger."),
    "klodderklaver": ("kruid", 0x6AD86A, "Klodderklaver", "Vier blaadjes en een beetje plakkerig. Voor de keuken en de brouwerij (T1)."),
    "pluiskatoen": ("vezel", 0xFFF0F8, "Pluiskatoen", "Zachte pluizen van de Klodderweide. Voor het naaiatelier (T1)."),
    "ijsbloem": ("bloem", 0xA8E8FF, "IJsbloem", "Een bloem die nooit smelt, van de Galmhellingen (T2)."),
    "kristalvlas": ("vezel", 0xC8D8FF, "Kristalvlas", "Glinsterend vlas van de hellingen. Voor het naaiatelier (T2)."),
    "lichtkeverkruid": ("kruid", 0xD8FF6A, "Lichtkeverkruid", "Een kruid dat zachtjes licht geeft (T3)."),
    "schemerzijde": ("vezel", 0xB89AE8, "Schemerzijde", "Paarse zijde uit De Schemerwortel. Voor het naaiatelier (T3)."),
    "gouden_klaver": ("kruid", 0xFFD84A, "Gouden Klaver", "Zeldzaam: een klavertje van goud. Brengt geluk (zegt men)."),
    "kloddervisje": ("vis", 0x7AB8FF, "Kloddervisje", "Een blauw visje met een klodderbuikje (T1)."),
    "maanbaarsje": ("vis", 0xC8C8FF, "Maanbaarsje", "Bijt alleen 's nachts (T1)."),
    "plonsguppie": ("vis", 0xFF9AC8, "Plonsguppie", "Komt boven als het regent (T1)."),
    "spiegelforel": ("vis", 0xE0F0FF, "Spiegelforel", "Je ziet jezelf in haar schubben (T2)."),
    "sterrenzalm": ("vis", 0xFFB89A, "Sterrenzalm", "Bijt alleen 's nachts (T2)."),
    "sneeuwsnoek": ("vis", 0xF0FAFF, "Sneeuwsnoek", "Komt boven als het regent of sneeuwt (T2)."),
    "wortelpaling": ("vis", 0x8A6A4A, "Wortelpaling", "Kronkelt als een wortel (T3)."),
    "lantaarnvis": ("vis", 0xFFD06A, "Lantaarnvis", "Bijt alleen 's nachts. Heeft haar eigen lampje (T3)."),
    "honingkarper": ("vis", 0xFFB830, "Honingkarper", "Komt boven als het regent. Smaakt naar honing (T3)."),
    "parelschelp": ("schelp", 0xFFF0FF, "Parelschelp", "Zeldzaam: een schelp met een roze parel."),
    "bloesemhoning": ("honing", 0xFFB8D8, "Bloesemhoning", "Roze honing van het Paddenbloesemdal (zone 3)."),
    "schemerhoning": ("honing", 0xE8A040, "Schemerhoning", "Goudbruine honing van de Raatweide (zone 3)."),
    "wortelhoning": ("honing", 0xA86A2A, "Wortelhoning", "Donkere honing van het Lichtkeverbos (zone 3)."),
    "bijenwas": ("was", 0xFFE8A0, "Bijenwas", "Voor kaarsen, deco en het naaiatelier."),
    "koninginnengelei": ("gelei", 0xFFF8E0, "Koninginnengelei", "Zeldzaam: het lekkerste van de korf. Voor het Honingelixer."),
}
VIS_WANNEER = {"nacht": ("Bijt alleen 's nachts.", "Only bites at night."), "regen": ("Bijt vooral als het regent.", "Bites mostly when it rains.")}
KERNEN = {
    "zwamkern": (0xFF6A7A, "Zwamkern", "Een bazenkern uit zone 1. Nodig voor Meesterwerk-sieraden (T1)."),
    "splinterkern": (0x9AD8FF, "Splinterkern", "Een bazenkern uit zone 2. Nodig voor Meesterwerk-sieraden (T2)."),
    "wortelkern": (0xE8A040, "Wortelkern", "Een bazenkern uit zone 3. Nodig voor Meesterwerk-sieraden (T3)."),
}
ETEN = {
    "klodderpudding": ("pudding", 0xFF8AC8, "Klodderpudding", "Vads +60, Kracht +3."),
    "visstoofje": ("kom", 0x7AB8FF, "Visstoofje", "Verdediging +4, Vads +40."),
    "verzamelaarshapje": ("broodje", 0x6AD86A, "Verzamelaarshapje", "Meer ambachts-xp en vaker dubbele oogst."),
    "kandijsoep": ("kom", 0xA8E8FF, "Kandijsoep", "Elementweerstand +6%, Vads +120."),
    "forelbroodje": ("broodje", 0xE0F0FF, "Forelbroodje", "Kritiek +3%, Behendigheid +6."),
    "ridderontbijt": ("bord", 0xFFC86A, "Ridderontbijt", "Kracht +6 en 5% meer gevechts-xp."),
    "honingpannenkoek": ("pannenkoek", 0xFFB830, "Honingpannenkoek", "Heal-kracht +5%, Wijsheid +10, Vads +150."),
    "wortelstamppot": ("bord", 0xA87A4A, "Wortelstamppot", "Verdediging +14, Vads +200."),
    "feestmaal": ("feest", 0xFF6FB5, "Feestmaal", "Voor je hele party: een grote buff plus Gezellig."),
}
DRANKEN = {
    "vadsdrankje": (0xFF6FB5, "Gebrouwen Vadsdrankje"), "energiedrankje": (0x6AD8FF, "Gebrouwen Energiedrankje"),
    "grote_vadsdrank": (0xFF3F9A, "Gebrouwen Groot Vadsdrankje"), "grote_energiedrank": (0x3AB8FF, "Gebrouwen Groot Energiedrankje"),
    "reuzen_vadsdrank": (0xE0206A, "Gebrouwen Reuzen-Vadsdrank"), "reuzen_energiedrank": (0x2A88E8, "Gebrouwen Reuzen-Energiedrank"),
    "honingelixer": (0xFFC830, "Honingelixer"),
}
MW = {
    "zwaard": ("Maatwerkzwaard", "Maatwerk Sword"), "boog": ("Maatwerkboog", "Maatwerk Bow"), "staf": ("Maatwerkstaf", "Maatwerk Staff"),
    "helm": ("Maatwerkhelm", "Maatwerk Helmet"), "borstplaat": ("Maatwerkborstplaat", "Maatwerk Breastplate"),
    "broek": ("Maatwerkbroek", "Maatwerk Trousers"), "schoentjes": ("Maatwerkschoentjes", "Maatwerk Shoes"),
    "ring": ("Meesterring", "Master Ring"), "ketting": ("Meesterketting", "Master Necklace"), "oorbel": ("Meesteroorbel", "Master Earring"),
}
TIER_NAAM = {1: ("Pluisijzeren", "Pluiskatoenen", "Zwamkern-"), 2: ("Kandijzilveren", "Kristalvlassen", "Splinterkern-"),
             3: ("Amberkoperen", "Schemerzijden", "Wortelkern-")}
MW_TIER = {
    "zwaard": "{0} Zwaard", "boog": "{0} Boog", "staf": "{0} Staf", "helm": "{0} Helm", "borstplaat": "{0} Borstplaat",
    "broek": "{1} Broek", "schoentjes": "{1} Schoentjes", "ring": "{2}Meesterring", "ketting": "{2}Meesterketting", "oorbel": "{2}Meesteroorbel",
}

GROEPEN = {
    "z1_pluk_kamp": "de wei bij het kampvuur van Knuffelkamp", "z1_pluk_weide_1": "de Klodderweide (west)", "z1_pluk_weide_2": "de Klodderweide (midden)",
    "z1_pluk_weide_3": "de Klodderweide (oost)", "z1_hout_bos_1": "het Sporenwoud (noord)", "z1_hout_bos_2": "het Sporenwoud (midden)",
    "z1_hout_bos_3": "het Sporenwoud (zuid)", "z1_mijn_grot_1": "de Druipgrotten (ingang)", "z1_mijn_grot_2": "de Druipgrotten (diep)",
    "z1_mijn_grot_3": "de Druipgrotten (zuid)", "z1_mijn_tempel": "de Zwamtempelberg", "z1_vis_vijver": "de vijver van Knuffelkamp",
    "z1_vis_waterval": "de waterval", "z1_vis_haven": "De Haven", "z1_vis_druipmeer": "het meer in de Druipschacht",
    "z2_pluk_helling_1": "de Galmhellingen (laag)", "z2_pluk_helling_2": "de Galmhellingen (midden)", "z2_pluk_helling_3": "de Galmhellingen (hoog)",
    "z2_hout_kristalbos_1": "het Kristalbos (noord)", "z2_hout_kristalbos_2": "het Kristalbos (midden)", "z2_hout_kristalbos_3": "het Kristalbos (zuid)",
    "z2_mijn_kloof_1": "de Splinterkloof (west)", "z2_mijn_kloof_2": "de Splinterkloof (midden)", "z2_mijn_kloof_3": "de Splinterkloof (oost)",
    "z2_mijn_ijsgrot": "de geheime IJsgrot", "z2_vis_meer_1": "het Knuffelmeer (west)", "z2_vis_meer_2": "het Knuffelmeer (oost)",
    "z2_vis_bron": "de warmwaterbron", "z3_pluk_kever": "het Lichtkeverbos", "z3_pluk_bloesemdal": "het Paddenbloesemdal",
    "z3_pluk_mosvoet": "de velden bij Mosvoet", "z3_hout_kever": "het Lichtkeverbos", "z3_hout_wortelweg": "langs de Wortelweg",
    "z3_hout_bloesemdal": "het Paddenbloesemdal", "z3_mijn_gewelf": "het Wortelgewelf", "z3_mijn_wortelgang_1": "de Wortelgang (voor)",
    "z3_mijn_wortelgang_2": "de Wortelgang (achter)", "z3_vis_moeras": "het Honingmoeras", "z3_vis_waterrad": "het waterrad van Mosvoet",
    "z3_vis_honingvijver": "de honingvijvers in de kruin",
    "z4_vis_haven": "de visplek van Lampionhaven", "z4_vis_kade": "de steigers van Lampionhaven", "z4_vis_koraal": "de Koraalbaai", "z3_korf_bloesemdal": "de bijenwei in het Paddenbloesemdal",
    "z3_korf_raatweide": "de Raatweide", "z3_korf_kever": "de bijenwei in het Lichtkeverbos",
}

RECEPTEN = {
    "klodderpudding": "Klodderpudding", "visstoofje": "Visstoofje", "verzamelaarshapje": "Verzamelaarshapje", "kandijsoep": "Kandijsoep",
    "forelbroodje": "Forelbroodje", "ridderontbijt": "Ridderontbijt", "honingpannenkoek": "Honingpannenkoek", "wortelstamppot": "Wortelstamppot",
    "feestmaal": "Feestmaal", "vadsdrankje": "Vadsdrankje", "energiedrankje": "Energiedrankje", "slijp_geslepen": "Kristal slijpen (geslepen)",
    "grote_vadsdrank": "Groot Vadsdrankje", "grote_energiedrank": "Groot Energiedrankje", "reuzen_vadsdrank": "Reuzen-Vadsdrank",
    "reuzen_energiedrank": "Reuzen-Energiedrank", "slijp_stralend": "Kristal slijpen (stralend)", "honingelixer": "Honingelixer",
    "kern_zwam": "Zwamkern (uit splinters)", "kern_splinter": "Splinterkern (uit splinters)", "kern_wortel": "Wortelkern (uit splinters)",
    "post_mijnhelm": "Mijnhelmpje (naar huis)", "post_opperzwambeeld": "Opperzwambeeld (naar huis)", "post_mini_koning": "Mini Koning Klodder (naar huis)",
    "post_kokschort": "Kokschort (naar huis)", "post_vaandel": "Vadsrijk-vaandel (naar huis)", "post_vissershoedje": "Vissershoedje (naar huis)",
    "post_tuinhoedje": "Tuinhoedje (naar huis)", "post_imkershoed": "Imkershoed (naar huis)", "post_klodderknuffel": "Klodderreus-knuffel (naar huis)",
}
for _s in ("zwaard", "boog", "staf", "helm", "borstplaat", "broek", "schoentjes", "ring", "ketting", "oorbel"):
    for _t in (1, 2, 3):
        RECEPTEN[f"{_s}_t{_t}"] = MW_TIER[_s].format(*TIER_NAAM[_t]).replace("- ", "-")

EIGENSCHAPPEN = {
    "bazen": ("Schade tegen bazen +%s%%", "Damage against bosses +%s%%"),
    "elites": ("Schade tegen elites +%s%%", "Damage against elites +%s%%"),
    "eerste_klap": ("Eerste klap op een wezen +%s%% schade", "First hit on a creature +%s%% damage"),
    "taai": ("Minder schade van bazen: -%s%%", "Less damage from bosses: -%s%%"),
    "noodkracht": ("Onder 30%% Vads: schade +%s%%", "Below 30%% Vads: damage +%s%%"),
}

ROLLEN = {  # rol: (hallo, uitleg (gathering masters), idle lines)
    "smid": ("Welkom in de smidse! Hier smeed ik wapens, helmen en borstplaten op maat. Jij kiest de bonussen, ik zorg voor een extra sokje.",
             None, ("Klink, klonk, klunk!", "Een goed zwaard is als een knuffel: stevig maar lief.", "Wie een bazenkern heeft, maakt een Meesterwerk!")),
    "naai": ("Kom binnen in het naaiatelier! Broeken, schoentjes, en hoedjes en deco voor thuis. Alles met liefde gestikt.",
             None, ("Steek, steek, knoop!", "Vingerhoedje op, daar gaan we.", "Pluiskatoen is het zachtste wat er is.")),
    "kok": ("Ruik je dat? In de keuken maken we eten met buffs: een half uur sterker, en het blijft als je flauwvalt!",
            None, ("Proeven mag, snoepen niet!", "Een feestmaal is pas lekker met je hele party.", "Oma Truus zegt: altijd een snufje vads.")),
    "brouw": ("Blub blub! In de brouwerij maak ik drankjes, en ik slijp kristallen: drie ruwe worden één geslepen.",
              None, ("Bubbel, bubbel, borrel...", "Niet aan de ketel zitten, die is heet!", "Waarom worden de kristallen toch grauw?")),
    "mijn": ("Hé, mijnwerkertje! Overal in het Vadsrijk liggen ertsplekken. Alleen jij ziet de jouwe.",
             "Klik met rechts op een ertsplek en tik drie keer op het glinsterende stukje. Goed getikt is vadsig erts! Sluipklik slaat het spelletje over.",
             ("Bonk! Bonk! Bonk!", "Glinstert daar iets?", "Een helm op je hoofd is geen overbodige luxe.")),
    "hout": ("Hallo houthakkertje! Bij de bomen staan stronkjes die jij mag hakken. Hak op het ritme, dan krijg je vadsig hout.",
             "Rechtsklik op een stronk en tik twee keer op het roze. Elke stronk komt na een minuutje terug, maar dan ergens anders!",
             ("Njeg-njeg, hak-hak!", "Een boom die je knuffelt, groeit harder.", "Spaan voor spaan.")),
    "pluk": ("Dag plukkertje! Op de weiden groeien kruiden en vezels. Pluk ze zachtjes, dan blijven ze heel.",
             "Rechtsklik op een plantje, HOUD de muisknop vast en laat los in het groene stukje. Te lang? Dan is het geplet.",
             ("Zachtjes, zachtjes...", "Een gouden klaver? Dan heb je geluk!", "Ruik eens aan de klodderklaver.")),
    "vis": ("Ahoy visvriendje! Waar het water borrelt, zit vis. 's Nachts en bij regen bijten er andere soorten.",
            "Rechtsklik op een borrelplek, wacht tot je 'Beet!' ziet en klik dan meteen. Te vroeg? Dan schrikt de vis.",
            ("Stil... stil... beet!", "De grootste vis zwemt altijd net weg.", "Ooit vis ik in de Lampionkust.")),
    "imker": ("Zzzoem! Welkom bij de bijenstal. Zet je eigen korf bij de bloemen, dan halen jouw bijen nectar.",
              "Klik op een lege korfplaats om je korf neer te zetten. Kom later terug: hoe meer bloemen, hoe sneller de raten vol zijn. Oogsten = rook geven (houd vast, laat los in het groen).",
              ("Zzzoem zzzoem.", "Bijen zijn guhs met vleugeltjes, zeg ik altijd.", "Pas op, die Mika-imker zuigt alles leeg!")),
    "bord": ("Hallo! Hier hangen de bestellingen van de stad, en de ranglijst van de beste ambachtsguhs.",
             None, ("Drie bestellingen per dag, niet meer en niet minder.", "Kijk eens wie er bovenaan staat!", "Bestellen, leveren, blij maken.")),
}
NPC_NAMEN = {
    "amb_smid_z1": "Smidguh Hamertje", "amb_naai_z1": "Naaiguh Vingerhoedje", "amb_kok_z1": "Kok Kruimeltje", "amb_brouw_z1": "Brouwguh Bubbel",
    "amb_mijn_z1": "Mijnguh Bonk", "amb_hout_z1": "Houtguh Spaan", "amb_pluk_z1": "Kruidenguh Madelief", "amb_vis_z1": "Visguh Dobbertje",
    "amb_bord_z1": "Klerk Krabbel", "amb_smid_z2": "Smidguh IJzerpootje", "amb_naai_z2": "Naaiguh Wolwantje", "amb_kok_z2": "Kok Kandijkoek",
    "amb_brouw_z2": "Brouwguh Bruisje", "amb_bord_z2": "Klerk Kriebel", "amb_smid_z3": "Hamerhoning", "amb_naai_z3": "Naaiguh Zijdezoem",
    "amb_kok_z3": "Kok Honingtaartje", "amb_brouw_z3": "Brouwguh Nectarine", "amb_imker_z3": "Imkerguh Zoemie", "amb_bord_z3": "Klerk Kraaltje",
}

# the stories: per ambacht 5 steps x (teacher, you, teacher)
VERHAAL = {
    "smeden": [
        ("Weet je wat dit is? Een splinter van het Aambeeld van de Orde. Ooit smeedde de hele stad erop.", "Wat is er met het aambeeld gebeurd?",
         "De Grauwe Keizer liet het in stukken springen. Vind de stukken, en word een echte smid. Hier, een beetje ijzer om te oefenen."),
        ("Je slagen worden zekerder! Ik hoorde dat er een stuk van het aambeeld in de Druipgrotten ligt.", "Dan ga ik daar mijnen!",
         "Goed zo. En onthoud: een zwaard op maat is net zo lief als een knuffel op maat."),
        ("Het tweede stuk zat in de Spiegelburcht. Het weerkaatste mijn eigen gezicht... en dat van een blije smid.", "Hoeveel stukken zijn er nog?",
         "Nog een paar. Elk stuk maakt het aambeeld warmer. Voel maar."),
        ("Met amberkoper uit De Schemerwortel zingt het aambeeld bijna. Bijna!", "Wat ontbreekt er nog?",
         "Een smid die het met hart smeedt. Dat ben jij, denk ik."),
        ("Het aambeeld is heel! Hoor je het? KLIIING. Dat is het geluid van de Orde die weer lacht.", "Dankzij ons!",
         "Dankzij jou, Gezel. Ik stuur je een mijnhelmpje naar huis, als aandenken."),
    ],
    "naaien": [
        ("Ik naai een reuzenquilt voor Hoogvads, met een lapje uit elke zone.", "Een quilt van het hele Vadsrijk?",
         "Precies! Begin maar met pluiskatoen. Zacht, roze en een beetje eigenwijs."),
        ("Het lapje van Zwamzoet is klaar. Er zit een klein Klodderkoninkje op geborduurd.", "Schattig!",
         "Nu de Splinterpieken. Daar heb ik kristalvlas voor nodig, dat glinstert zo mooi."),
        ("Het ijslapje knispert als je erover aait. Hoogvads gaat dit prachtig vinden.", "Wie slaapt er onder die quilt?",
         "Iedereen die het koud heeft. Zelfs de Keizer, als hij ooit wil."),
        ("Schemerzijde! Kijk hoe paars het glanst. Het lapje van De Schemerwortel wordt het mooiste.", "Mag ik helpen met de steken?",
         "Jij doet al de helft, lieverd. Jouw steken zijn net zo netjes als de mijne."),
        ("De quilt is af! Drie zones, drie lapjes, en nog plek voor meer.", "Op naar Hoogvads!",
         "Eerst een cadeautje: een Klodderreus-knuffel, voor thuis. Gezel Naaier, dat ben je nu."),
    ],
    "koken": [
        ("Oma Truus had een receptenboek. 'Toen de klodders nog roze waren', staat erin. De bladzijden zijn weggewaaid.", "Over het hele Vadsrijk?",
         "Over het hele Vadsrijk! Begin met haar klodderpudding. Dat recept ken ik nog uit mijn hoofd."),
        ("Ik vond een bladzijde in een visnet! Er staat een visstoofje op, met een vlek van saus.", "Smaakt het naar vroeger?",
         "Het smaakt naar zondag. Blijf koken, dan vinden we er meer."),
        ("Deze bladzijde lag bevroren in de Splinterpieken: Kandijsoep. Oma schreef erbij: 'tegen koude pootjes'.", "Ik maak er meteen een pan van!",
         "Dat is de goede houding, kokkie."),
        ("Honingpannenkoek! Met een tekeningetje van een bijtje. Oma kon niet tekenen, kijk die pootjes.", "Zes pootjes... of acht?",
         "Het telt niet. Wat telt is dat het lekker is."),
        ("De laatste bladzijde. 'Het koekje dat de Keizer nooit kreeg.' De rest is uitgeveegd...", "Misschien kunnen we het ooit bakken?",
         "Ooit, Gezel Kok. Ooit. Hier, een kokschort voor thuis, zodat je het niet vergeet."),
    ],
    "brouwen": [
        ("Blub! Weet je waarom de kristallen grauw worden? Ik ook niet. Maar ik ga het uitzoeken!", "Kan ik helpen?",
         "Brouw eerst een vadsdrankje. Wie roze kan brouwen, kan grauw verslaan."),
        ("Ik heb een grauw kristal in de ketel gedaan. Het werd... een beetje minder grauw!", "Wat deed je erbij?",
         "Klodderklaver en een hele grote knuffelgedachte. Wetenschap!"),
        ("Geslepen kristallen houden het grauw beter buiten. Daarom slijpen we ze!", "Drie ruw voor één geslepen?",
         "Precies. Kwaliteit boven kwantiteit, blub."),
        ("Met honing uit De Schemerwortel blijft het roze zelfs 's nachts! Het ont-grauw-drankje komt dichterbij.", "Hoe noemen we het?",
         "Het Honingelixer. Klinkt deftig, smaakt naar zomer."),
        ("Het werkt! Een druppel Honingelixer en een grauw kristal begint te stralen. De Vadsleidingen zullen blij zijn.", "Dat hebben we samen gedaan!",
         "Zeker weten, Gezel Brouwer. Hier, een Mini Koning Klodder voor op je kast."),
    ],
    "mijnen": [
        ("Bonk! Elke ader in het Vadsrijk is een beetje vads die onder de grond slaapt.", "En wij maken hem wakker?",
         "Precies! Hier, wat pluisijzer om te beginnen. Tik op het glinsterende stukje."),
        ("Er is een Mika die aders leegzuigt met een kaasgaspomp. Heb jij hem gezien?", "Een kaasgaspomp?!",
         "Ik zweer het. Het ruikt er nog naar. Hou je ogen open."),
        ("In de Splinterkloof glinstert het kandijzilver. En diep in een ijsgrot zit een geheime ader...", "Een geheime?",
         "Zoek maar. Het Verkenningsboek houdt bij welke plekken je vond."),
        ("Amberkoper! Warm als een kampvuur. De wortels van de boom beschermen het.", "Ik voel het zoemen.",
         "Dat is de boom die bromt. Hij vindt je aardig."),
        ("Je bent een echte mijnwerker. Glinsteraders vind je nu als de beste.", "Bonk! Bonk! Bonk!",
         "Haha, precies zo! Gezel Mijner. Ik stuur je een mijnhelmpje naar huis."),
    ],
    "houthakken": [
        ("Hout hakken doe je niet zomaar. Je bedankt de boom eerst.", "Dankjewel, boom!",
         "Goed zo. Hier, wat zwamsteel om te oefenen. Hak op het ritme: njeg-njeg."),
        ("Elke stronk die je hakt, groeit ergens anders weer terug. Het bos danst een beetje.", "Dus ze verhuizen?",
         "Ze verhuizen! Daarom moet je goed rondkijken."),
        ("Kristalden uit de Splinterpieken. Het klinkt als een klokje als je erop tikt.", "Tink!",
         "Tink! Ja. Mooi hout voor een boog."),
        ("Schemerhout huilt roze hars. Geen verdriet, hoor, gewoon blij sap.", "Blij sap?",
         "Blij sap. Smiden gebruiken het voor hun beste werk."),
        ("Je hakt als een echte bosguh. Het bos fluistert je naam.", "Wat zegt het?",
         "'Dank je.' Gezel Houthakker! Hier, een Opperzwambeeld voor thuis."),
    ],
    "plukken": [
        ("Plantjes zijn net guhs: als je ze zachtjes vasthoudt, blijven ze heel.", "Zachtjes, zachtjes...",
         "Precies! Hier, wat klodderklaver en pluiskatoen om mee te beginnen."),
        ("Op de Klodderweide groeit soms een gouden klaver. Op glinsterplekjes, als je geluk hebt.", "Ik ga zoeken!",
         "Neem je tijd. Haast plet de blaadjes."),
        ("IJsbloemen smelten nooit. Ze denken dat het altijd winter is.", "Arme bloemetjes?",
         "Nee hoor, ze vinden het heerlijk. Net als sneeuwguhs."),
        ("Lichtkeverkruid geeft licht, omdat de lichtkevers er 's nachts op slapen.", "Daarom gloeit het!",
         "Slim! Jij weet al bijna net zoveel als ik."),
        ("Mijn tuinhoedje is te klein geworden voor al jouw kennis. Gezel Plukker!", "Dankjewel, Madelief!",
         "Hier, je eigen tuinhoedje, voor thuis."),
    ],
    "vissen": [
        ("Vissen is wachten, en wachten is knuffelen met de tijd.", "Hoe weet ik wanneer ik moet klikken?",
         "Als je 'Beet!' ziet. Niet eerder. De vis is net zo verlegen als jij."),
        ("'s Nachts bijten andere vissen. Het maanbaarsje bijvoorbeeld.", "En als het regent?",
         "Dan komt de plonsguppie boven om te dansen."),
        ("In de Splinterpieken zwemt de spiegelforel. Als je goed kijkt, zwaait je spiegelbeeld terug.", "Ik zwaai terug!",
         "Haha. Dat doen alle goede vissers."),
        ("De lantaarnvis heeft haar eigen lampje. 's Nachts zie je haar al van ver.", "Ik vang haar!",
         "Als je geduldig bent, komt zij naar jou."),
        ("Je bent een echte visser. Weet je wat er achter De Schemerwortel ligt? De Lampionkust. Daar duiken ze naar koraal...", "Gaan we daarheen?",
         "Ooit! Gezel Visser. Hier, een vissershoedje voor thuis."),
    ],
    "imkeren": [
        ("Bijen zijn guhs met vleugeltjes. Ze delen alles, net als wij.", "Mag ik ook een korf?",
         "Natuurlijk! Zet hem bij veel bloemen. Hoe meer bloemen, hoe sneller de honing."),
        ("Elke wei geeft een andere honing: bloesemhoning, schemerhoning, wortelhoning.", "Ik wil ze allemaal proeven!",
         "Dan moet je overal een korf zetten. Op naar de volgende wei!"),
        ("Pas op voor de Mika-imker. Hij zuigt de raten leeg met een kaaspomp.", "Gemeen!",
         "Gemeen, ja. Maar onze bijen zijn slimmer. Ze verstoppen de beste honing."),
        ("Je bijen kennen je stem al. Hoor je ze zoemen als je eraan komt?", "Zzzoem!",
         "Precies zo. Soms vind je koninginnengelei: het lekkerste van de korf."),
        ("Je bent een echte imker. De bijen hebben je tot erelid benoemd.", "Een erelid!",
         "Gezel Imker! Hier, een imkershoed voor thuis. Zzzoem."),
    ],
}

TEKSTEN = {}
EN = {}


def t(k, nl, en=None):
    TEKSTEN[k] = nl
    if en is not None:
        EN[k] = en


S = "vadsrijk.ambacht."
t("key.guhs.vr_ambachten", "Ambachten", "Professions (Ambachten)")
t(S + "titel", "Ambachten", "Professions")
for _id, (_nl, _en) in AMBACHTEN.items():
    t(S + "naam." + _id, _nl, _en)
t(S + "xp", "+%s %s-xp", "+%s %s xp")
t(S + "xp_rust", "+%s %s-xp (uitgerust: dubbel!)", "+%s %s xp (rested: double!)")
t(S + "level_op", "Je %s is nu niveau %s!", "Your %s is now level %s!")
t(S + "kwaliteit.naam.vadsig", "Vadsig %s", "Vadsig %s")
t(S + "kwaliteit.naam.extra_vadsig", "Extra Vadsig %s", "Extra Vadsig %s")
t(S + "kwaliteit.gewoon", "Kwaliteit: gewoon", "Quality: plain")
t(S + "kwaliteit.vadsig", "Kwaliteit: vadsig", "Quality: vadsig")
t(S + "kwaliteit.extra_vadsig", "Kwaliteit: extra vadsig", "Quality: extra vadsig")
t(S + "item.tier", "%s · tier %s", "%s · tier %s")
t(S + "item.zeldzaam", "%s · zeldzaam", "%s · rare")
for _w, (_nl, _en) in VIS_WANNEER.items():
    t(S + "vis." + _w, _nl, _en)
t(S + "vis.nieuw", "Nieuwe vis gevangen: %s! (Verkenningsboek)", "New fish caught: %s! (Exploration book)")
t(S + "oogst.niet_hier", "Dat kan alleen in het Vadsrijk.", "Only in the Vadsrijk.")
t(S + "oogst.weg", "Die plek is er niet meer.", "That spot is gone.")
t(S + "oogst.te_ver", "Te ver weg.", "Too far away.")
t(S + "oogst.leeg", "Deze plek is even leeg. Hij komt straks ergens anders terug.", "This spot is empty for now. It comes back elsewhere soon.")
t(S + "oogst.te_laag", "Daar heb je %s niveau %s voor nodig.", "You need %s level %s for that.")
t(S + "oogst.rustig", "Rustig aan...", "Easy now...")
t(S + "oogst.zeldzaam", "Zeldzaam! Je vond: %s", "Rare! You found: %s")
t(S + "oogst.afk", "Je staat al een tijdje op dezelfde plek: de oogst is gehalveerd. Loop even rond!",
  "You have been standing in one spot for a while: the yield is halved. Walk around a bit!")
t(S + "korf.vol", "Je hebt al genoeg korven (max %s op dit niveau).", "You already have enough hives (max %s at this level).")
t(S + "korf.neergezet", "Je korf staat! Je bijen gaan nectar halen voor %s.", "Your hive is placed! Your bees fetch nectar for %s.")
t(S + "korf.leeg", "De raten zijn nog niet vol. Kom straks terug.", "The combs aren't full yet. Come back later.")
t(S + "korf.boos", "Te snel! De bijen zijn even chagrijnig (je plakt een beetje).", "Too hasty! The bees are grumpy (you're a bit sticky).")
t(S + "korf.nieuwe_honing", "Nieuwe honingsoort: %s! (Verkenningsboek)", "New honey kind: %s! (Exploration book)")
t(S + "tip.korf", "Je bijen vliegen naar de bloemen rond je korf. Hoe meer bloemen, hoe sneller de raten vol zijn.",
  "Your bees fly to the flowers round your hive. More flowers = faster honey.")
t(S + "tip.korf.titel", "Je eerste bijenkorf", "Your first beehive")
t(S + "alleen_rijk", "Dit eten werkt alleen in het Vadsrijk.", "This food only works in the Vadsrijk.")
t(S + "eten.feest_mee", "%s deelt een feestmaal met je! Gezellig!", "%s shares a feast with you! Cosy!")
t(S + "eten.op", "Smakelijk! %s: je buff duurt 30 minuten.", "Enjoy! %s: your buff lasts 30 minutes.")
t(S + "eten.stat", "%s +%s", "%s +%s")
t(S + "eten.ambachtxp", "Ambachts-xp +%s%%", "Profession xp +%s%%")
t(S + "eten.dubbel", "Kans op dubbele oogst +%s%%", "Chance of a double harvest +%s%%")
t(S + "eten.gevechtxp", "Gevechts-xp +%s%%", "Combat xp +%s%%")
t(S + "eten.duur", "30 minuten · één eten-buff tegelijk · blijft bij flauwvallen", "30 minutes · one food buff at a time · stays when you faint")
t(S + "eten.feest", "Voor je hele party binnen 10 blokken + Gezellig (30 min)", "For your whole party within 10 blocks + Cosy (30 min)")
t(S + "vadsigheid", "Vadsigheid %s %s%%", "Vadsigheid %s %s%%")
t(S + "gemaakt_door", "Gemaakt door %s", "Made by %s")
t(S + "drank.vads", "Vads +%s", "Vads +%s")
t(S + "drank.energie", "Energie +%s", "Energy +%s")
t(S + "drank.wast", "Wast Plakkerig en Gesporen af", "Washes off Plakkerig and Gesporen")
for _id, (_nl, _en) in EIGENSCHAPPEN.items():
    t(S + "eigenschap." + _id, _nl, _en)
t(S + "mw.maatwerk", "Maatwerk", "Maatwerk (custom made)")
t(S + "mw.meesterwerk", "Meesterwerk", "Meesterwerk (masterwork)")
t(S + "mw.gekozen", "Zelf gekozen:", "Chosen:")
t(S + "mw.geen_set", "Maatwerk telt niet voor een setbonus.", "Custom gear never counts for a set bonus.")
t(S + "vadsig", "Vadsig", "Vadsig")
t(S + "werk.onbekend", "Dat recept ken ik niet.", "Unknown recipe.")
t(S + "werk.te_laag", "Je niveau is nog te laag voor dit recept.", "Your level is too low for this recipe.")
t(S + "werk.geen_massa", "Maatwerk maak je één voor één.", "Custom gear is made one at a time.")
t(S + "werk.kies", "Kies eerst je bonussen.", "Choose your bonuses first.")
t(S + "werk.eigenschap", "Kies eerst een eigenschap.", "Choose a property first.")
t(S + "werk.kristal", "Kies eerst een kristalsoort.", "Choose a crystal kind first.")
t(S + "werk.te_weinig", "Je hebt niet genoeg materiaal.", "Not enough materials.")
t(S + "werk.post", "%s gaat met de Post naar huis!", "%s is sent home by Post!")
t(S + "werk.vadsig", "Gelukt: 100%% vadsig!", "Done: 100%% vadsig!")
t(S + "werk.klaar", "Gemaakt!", "Made!")
t(S + "werk.niet_hier", "Dat kan alleen aan deze werkbank.", "Only at this workshop.")
t(S + "werk.te_ver", "Je bent te ver van de werkbank.", "You're too far from the workshop.")
t(S + "werk.kristal_in.0", "3 ruwe kristallen van één soort", "3 raw crystals of one kind")
t(S + "werk.kristal_in.1", "3 geslepen kristallen van één soort", "3 cut crystals of one kind")
t(S + "werk.titel.smeden", "De Smidse", "The Smithy")
t(S + "werk.titel.naaien", "Het Naaiatelier", "The Sewing Room")
t(S + "werk.titel.koken", "De Keuken", "The Kitchen")
t(S + "werk.titel.brouwen", "De Brouwerij", "The Brewery")
t(S + "werk.level", "Jouw niveau: %s", "Your level: %s")
t(S + "werk.kies_n", "Kies %s bonussen (%s gekozen):", "Choose %s bonuses (%s chosen):")
t(S + "werk.maak", "Maak", "Make")
t(S + "werk.maak10", "Maak 10x (60%)", "Make 10x (60%)")
t(S + "werk.nu", "Nu!", "Now!")
t(S + "bestel.breng", "%s wil %s %s.", "%s wants %s %s.")
t(S + "bestel.maak", "%s wil dat je %s keer iets maakt met %s.", "%s wants you to make something %s times with %s.")
t(S + "bestel.verzamel", "%s wil dat je %s dingen verzamelt met %s.", "%s wants you to gather %s things with %s.")
t(S + "bestel.niks", "Die bestelling is er niet.", "No such order.")
t(S + "bestel.nog_niet", "Die bestelling is nog niet klaar.", "That order isn't done yet.")
t(S + "bestel.weekkist", "Twaalf bestellingen deze week: een Ambachtskist voor jou!", "Twelve orders this week: an Ambachtskist for you!")
t(S + "bestel.geclaimd", "Geleverd! Dankjewel!", "Delivered! Thank you!")
t(S + "bestel.geen_ruil", "Je kunt vandaag niet meer ruilen.", "No more swaps today.")
t(S + "bestel.geruild", "Geruild voor een nieuwe bestelling.", "Swapped for a new order.")
t(S + "bestel.kist_open", "In de kist: kernsplinters, iets zeldzaams en 5 zilver!", "In the chest: core splinters, something rare and 5 silver!")
t(S + "bestel.leeg", "Nog geen bestellingen voor jou. Word eerst een beetje beter in een ambacht!",
  "No orders for you yet. Get a bit better at a profession first!")
t(S + "bestel.gedaan", "Geleverd", "Delivered")
t(S + "bestel.lever", "Leveren", "Deliver")
t(S + "bestel.ruil", "Ruilen", "Swap")
t(S + "bestel.week", "Deze week: %s/%s bestellingen (dan een Ambachtskist)", "This week: %s/%s orders (then an Ambachtskist)")
t(S + "bestel.week_klaar", "Deze week: %s/%s, je weekkist is binnen!", "This week: %s/%s, you got your weekly chest!")
t(S + "bestel.reset", "Nieuwe bestellingen over %su %sm", "New orders in %sh %sm")
t(S + "npc.waar", "Kijk eens bij %s, rond x %s, z %s.", "Have a look at %s, around x %s, z %s.")
for _r, (_hallo, _uitleg, _idle) in ROLLEN.items():
    t(S + "npc." + _r + ".hallo", _hallo)
    if _uitleg:
        t(S + "npc." + _r + ".uitleg", _uitleg)
t(S + "optie.bestel", "Bestelbord", "Order board")
t(S + "optie.rang", "Ambachtsbord (ranglijst)", "Rankings")
t(S + "optie.waar", "Waar vind ik plekken?", "Where are the spots?")
t(S + "optie.werk", "Werkplaats", "Workshop")
t(S + "optie.boek", "Receptenboek", "Recipe book")
t(S + "optie.verhaal", "Vertel eens...", "Tell me...")
t(S + "optie.dag", "Dag!", "Bye!")
for _id, _n in GROEPEN.items():
    t(S + "groep." + _id, _n)
for _a, _stappen in VERHAAL.items():
    for _i, _regels in enumerate(_stappen):
        for _k, _r in enumerate(_regels):
            t(f"{S}verhaal.{_a}.{_i}.{_k}", _r)
t(S + "verhaal.uit", "Je kent mijn hele verhaal al. Ga maar lekker verder met je ambacht!", "You know my whole story. Go on with your craft!")
t(S + "verhaal.wacht", "Kom terug als je %2$s niveau %1$s is, dan vertel ik verder.", "Come back at %2$s level %1$s and I'll tell you more.")
t(S + "verhaal.post", "Er komt een aandenken met de Post naar huis: %s", "A keepsake comes home by Post: %s")
t(S + "verhaal.stap", "Verhaal %s: stap %s van %s.", "Story %s: step %s of %s.")
for _a, (_nl, _en) in {"mijnen": ("Pluisader! Tik op het glinsterende!", "Glitter vein! Tap the sparkle!"),
                       "houthakken": ("Kerfritme! Tik op het roze!", "Chop rhythm! Tap the pink!"),
                       "plukken": ("Zachtjes! Houd vast en laat los in het groen", "Gently! Hold and let go in the green"),
                       "vissen": ("Wacht op de beet...", "Wait for the bite..."),
                       "imkeren": ("Rook! Houd vast en laat los in het groen", "Smoke! Hold and let go in the green")}.items():
    t(S + "mini." + _a, _nl, _en)
t(S + "mini.perfect", "Perfect: extra vadsig!", "Perfect: extra vadsig!")
t(S + "mini.goed", "Goed: vadsig!", "Good: vadsig!")
t(S + "mini.gewoon", "Gewoon", "Plain")
t(S + "mini.beet", "BEET! Klik!", "BITE! Click!")
t(S + "mini.wacht", "Nog niet...", "Not yet...")
t(S + "mini.los", "Laat los in het groen!", "Let go in the green!")
t(S + "mini.houd", "Houd rechtsklik vast", "Hold right-click")
t(S + "mini.tik", "Rechtsklik! (%s/%s)", "Right-click! (%s/%s)")
t(S + "hud.korf", "Jouw korf: %s raten (%s)", "Your hive: %s combs (%s)")
t(S + "hud.korf_vrij", "Lege korfplaats (%2$s): klik om je korf te zetten", "Free hive stand (%2$s): click to place your hive")
t(S + "hud.plek", "%s · tier %s (rechtsklik)", "%s · tier %s (right-click)")
t(S + "hud.te_laag", "%s · tier %s (niveau %s nodig)", "%s · tier %s (needs level %s)")
t(S + "hud.zeldzaam", "Glinsterplek: driedubbele oogst!", "Glittering spot: triple harvest!")
t(S + "hud.buff", "Eten: %s (%sm %ss)", "Food: %s (%sm %ss)")
t(S + "hud.gezellig", "Gezellig (%sm)", "Cosy (%sm)")
t(S + "hud.uitgerust", "Uitgerust: %s min dubbele ambachts-xp", "Rested: %s min of double profession xp")
for _tab, (_nl, _en) in {"overzicht": ("Overzicht", "Overview"), "recepten": ("Recepten", "Recipes"), "bestel": ("Bestellingen", "Orders"),
                         "rang": ("Ranglijst", "Rankings"), "plekken": ("Plekken", "Spots")}.items():
    t(S + "tab." + _tab, _nl, _en)
t(S + "lv", "Niveau %s / %s", "Level %s / %s")
t(S + "xp_balk", "%s / %s xp", "%s / %s xp")
t(S + "boek.geen_buff", "Geen eten-buff. Eet iets uit de keuken voor 30 minuten extra kracht!", "No food buff. Eat something from the kitchen!")
t(S + "boek.buff", "Eten: %s (nog %s min)", "Food: %s (%s min left)")
t(S + "boek.uitgerust", "Uitgerust: %s / %s min dubbele ambachts-xp (bouwt op als je offline bent)",
  "Rested: %s / %s min of double profession xp (builds up while offline)")
t(S + "boek.zone", "Zone %s", "Zone %s")
t(S + "boek.vissen", "Vissen gevangen: %s / %s", "Fish caught: %s / %s")
t(S + "boek.honing", "Honingsoorten: %s / %s", "Honey kinds: %s / %s")
t(S + "boek.recepten", "Recepten gemaakt: %s / %s", "Recipes made: %s / %s")
t(S + "boek.meer", "Alles staat ook in het Verkenningsboek (B), op de pagina Ambachten.", "Everything is also in the Exploration book (B).")
t(S + "boek.titel", "Ambachten", "Professions")
for _z in (1, 2, 3):
    t(S + f"boek.cat.plekken.{_z}", f"Plekken zone {_z}", f"Spots zone {_z}")
t(S + "boek.cat.vissen", "Vissen", "Fish")
t(S + "boek.cat.honing", "Honing", "Honey")
t(S + "boek.cat.recepten", "Recepten", "Recipes")
t(S + "boek.hint.plek", "Een plek voor %s. Vraag de meesterguh in Zwamzoet waar.", "A spot for %s. Ask the master guh in Zwamzoet.")
t(S + "boek.hint.vis.altijd", "Altijd te vangen (%s).", "Can always be caught (%s).")
t(S + "boek.hint.vis.nacht", "Alleen 's nachts (%s).", "Only at night (%s).")
t(S + "boek.hint.vis.regen", "Vooral als het regent (%s).", "Mostly when it rains (%s).")
t(S + "boek.hint.honing", "Zet een korf in de juiste bijenwei.", "Place a hive in the right meadow.")
t(S + "boek.hint.recept", "Een recept van %s.", "A %s recipe.")
for _z in (1, 2, 3):
    t(S + f"tier.{_z}", f"tier {_z}", f"tier {_z}")
t(S + "recept.lv", "Niveau %s · tier %s", "Level %s · tier %s")
t(S + "recept.lv_kan", "Niv. %s · kan: %s", "Lv %s · can: %s")
t(S + "recept.of", "%s / %s (een van een paar soorten mag)", "%s / %s (one of a few kinds)")
t(S + "recept.of_kort", " (of vergelijkbaar)", " (or similar)")
t(S + "recept.heeft", "%s / %s", "%s / %s")
t(S + "rang.totaal", "Totaal", "Total")
t(S + "rang.lv", "niveau %s · %s xp", "level %s · %s xp")
t(S + "rang.eigen", "Jij staat op plek %s van de %s.", "You are number %s of %s.")
t(S + "rang.geen", "Je staat nog niet op deze lijst.", "You're not on this list yet.")
for _id, _n in RECEPTEN.items():
    t(S + "recept." + _id, _n)
# items
for _id, (_s, _k, _n, _u) in MATERIALEN.items():
    t(f"item.guhs.vr_amb_{_id}", _n)
    t(f"item.guhs.vr_amb_{_id}.uitleg", _u)
for _id, (_k, _n, _u) in KERNEN.items():
    t(f"item.guhs.vr_amb_kern_{_id}", _n)
    t(f"item.guhs.vr_amb_kern_{_id}.uitleg", _u)
t("item.guhs.vr_amb_kernsplinter", "Kernsplinter", "Core Splinter")
t("item.guhs.vr_amb_kernsplinter.uitleg", "Vijf splinters maak je in de smidse tot een bazenkern.", "Five splinters make a boss core at the smithy.")
t("item.guhs.vr_amb_ambachtskist", "Ambachtskist", "Profession Chest")
t("item.guhs.vr_amb_ambachtskist.uitleg", "De weekkist van het Bestelbord. Rechtsklik om te openen.", "The Order board's weekly chest. Right-click to open.")
for _id, (_s, _k, _n, _u) in ETEN.items():
    t(f"item.guhs.vr_amb_eten_{_id}", _n)
for _id, (_k, _n) in DRANKEN.items():
    t(f"item.guhs.vr_amb_drank_{_id}", _n)
for _id, (_nl, _en) in MW.items():
    t(f"item.guhs.vr_amb_mw_{_id}", _nl, _en)
    for _t in (1, 2, 3):
        t(f"vadsrijk.loot.mw_t{_t}_{_id}", MW_TIER[_id].format(*TIER_NAAM[_t]).replace("- ", "-"))
# NPCs (the npc slice's keys: name + 3 idle lines)
for _npc, _naam in NPC_NAMEN.items():
    t("vadsrijk.npc.naam." + _npc, _naam)
    _rol = _npc.split("_")[1]
    for _i, _r in enumerate(ROLLEN[_rol][2]):
        t(f"vadsrijk.npc.praat.{_npc}.{_i + 1}", _r)
# titles
for _id, (_nl, _en) in AMBACHTEN.items():
    t(f"gui.guhs.titels.naam.vr_amb_{_id}_leerling", f"Leerling-{_nl.lower()}" if _id not in ("vissen",) else "Leerling-visser",
      f"Apprentice ({_en})")
    t(f"gui.guhs.titels.naam.vr_amb_{_id}_gezel", f"Gezel-{_nl.lower()}" if _id not in ("vissen",) else "Gezel-visser", f"Journeyman ({_en})")
    # (every title needs a hint: TitelsGameTests.titelsTekstenBestaan)
    t(f"gui.guhs.titels.hint.vr_amb_{_id}_leerling", f"Haal niveau 25 in {_nl.lower()}.", f"Reach level 25 in {_en}.")
    t(f"gui.guhs.titels.hint.vr_amb_{_id}_gezel", f"Haal niveau 50 in {_nl.lower()}.", f"Reach level 50 in {_en}.")
t("gui.guhs.titels.naam.vr_ambachtskenner", "Ambachtskenner", "Master of Crafts")
t("gui.guhs.titels.hint.vr_ambachtskenner", "Vul de pagina Ambachten helemaal (Verkenningsboek, toets B).",
  "Complete the Professions page (Exploration Book, key B).")


# ======================================================================================================================
# icons
# ======================================================================================================================
def erts(c):
    d = Doek()
    d.disc(8, 9, 5.5, (120, 110, 130))
    d.disc(7, 8, 4.5, (150, 140, 160))
    for x, y in ((6, 7), (9, 9), (7, 11), (10, 6)):
        d.disc(x, y, 1.3, c)
        d.px(x - 0.5, y - 0.5, _licht(c, 1.3))
    d.schaduw()
    return d.rand()


def gruis(c):
    d = Doek()
    for x, y, r in ((5, 10, 2.2), (9, 8, 2.6), (11, 12, 1.8), (7, 13, 1.5)):
        d.disc(x, y, r, c)
    for x, y in ((3, 3), (12, 3), (8, 4), (13, 7)):
        d.px(x, y, (255, 255, 230))
    d.schaduw()
    return d.rand()


def hout(c):
    d = Doek()
    d.poly([(2, 10), (11, 3), (14, 6), (5, 13)], _licht(c, 0.8))
    d.disc(12.5, 4.5, 2.4, _licht(c, 1.2))
    d.disc(12.5, 4.5, 1.2, _licht(c, 0.9))
    d.lijn(4, 11, 11, 5, _licht(c, 0.65))
    d.schaduw()
    return d.rand()


def hars(c):
    d = Doek()
    d.disc(8, 10, 4.5, c)
    d.poly([(8, 2), (5.5, 8), (10.5, 8)], c)
    d.disc(6.5, 9, 1.4, _licht(c, 1.3))
    d.schaduw()
    return d.rand()


def kruid(c):
    d = Doek()
    d.lijn(8, 15, 8, 8, (70, 140, 60))
    for dx, dy in ((-2.5, -1), (2.5, -1), (0, -3.5), (0, 1.5)):
        d.disc(8 + dx, 6 + dy, 2.3, c)
    d.px(8, 6, _licht(c, 1.3))
    d.schaduw()
    return d.rand()


def bloem(c):
    d = Doek()
    d.lijn(8, 15, 8, 8, (70, 140, 60))
    for i in range(5):
        h = i / 5 * 2 * math.pi
        d.disc(8 + math.cos(h) * 2.8, 6 + math.sin(h) * 2.8, 1.8, c)
    d.disc(8, 6, 1.4, (255, 240, 140))
    d.schaduw()
    return d.rand()


def vezel(c):
    d = Doek()
    d.disc(8, 8, 5.5, c)
    d.lijn(3, 6, 13, 10, _licht(c, 0.8))
    d.lijn(3, 10, 13, 6, _licht(c, 0.85))
    d.disc(6, 6, 1.5, _licht(c, 1.15))
    d.schaduw()
    return d.rand()


def vis(c):
    d = Doek()
    d.disc(8, 8, 4.5, c)
    d.poly([(12, 8), (15, 4), (15, 12)], _licht(c, 0.8))
    d.disc(7, 9, 2.5, _licht(c, 1.2))
    d.px(5, 7, (40, 26, 40))
    d.px(4, 9, (255, 140, 180))
    d.schaduw()
    return d.rand()


def schelp(c):
    d = Doek()
    d.disc(8, 9, 5.5, c, alleen_boven=10)
    d.rect(3, 10, 13, 11, _licht(c, 0.85))
    for x in (5, 8, 11):
        d.lijn(8, 11, x, 5, _licht(c, 0.8))
    d.disc(8, 11, 1.6, (255, 150, 200))
    d.schaduw()
    return d.rand()


def pot(c):
    d = Doek()
    d.rect(4, 5, 11, 14, (230, 230, 240))
    d.rect(5, 7, 10, 13, c)
    d.rect(5, 3, 10, 4, (180, 120, 70))
    d.px(6, 8, _licht(c, 1.3))
    d.schaduw()
    return d.rand()


def was(c):
    d = Doek()
    for i in range(3):
        for j in range(2):
            cx, cy = 5 + i * 3, 6 + j * 4 + (i % 2) * 2
            d.disc(cx, cy, 1.8, c)
            d.px(cx, cy, _licht(c, 0.8))
    d.schaduw()
    return d.rand()


def kern(c):
    d = Doek()
    d.disc(8, 8, 5.5, _licht(c, 0.6))
    d.disc(8, 8, 4.2, c)
    d.disc(7, 7, 1.8, _licht(c, 1.4))
    for x, y in ((2, 2), (13, 3), (3, 13)):
        d.px(x, y, (255, 250, 220))
    d.schaduw()
    return d.rand()


def splinter():
    d = Doek()
    d.poly([(6, 14), (8, 2), (11, 13)], (220, 150, 255))
    d.poly([(8, 2), (8.5, 14), (6, 14)], (250, 200, 255))
    d.schaduw()
    return d.rand()


def kist():
    d = Doek()
    d.rect(2, 6, 13, 14, (170, 110, 70))
    d.rect(2, 4, 13, 7, (200, 130, 80))
    d.rect(7, 7, 8, 9, (255, 210, 80))
    d.rect(2, 10, 13, 10, (120, 80, 50))
    d.px(4, 5, (255, 140, 190))
    d.px(11, 5, (255, 140, 190))
    d.schaduw()
    return d.rand()


def eten(soort, c):
    d = Doek()
    if soort == "pudding":
        d.rect(2, 12, 13, 13, (240, 240, 250))
        d.disc(8, 9, 4.5, c, alleen_boven=11)
        d.rect(4, 6, 11, 11, c)
        d.disc(8, 5, 1.5, (255, 60, 90))
    elif soort == "kom":
        d.disc(8, 8, 6, (240, 230, 220), alleen_boven=None)
        d.disc(8, 7, 4.6, c)
        d.rect(2, 8, 13, 9, (240, 230, 220))
        d.px(6, 6, _licht(c, 1.3))
    elif soort == "broodje":
        d.disc(8, 9, 5.5, (230, 170, 100), alleen_boven=9)
        d.rect(2, 9, 13, 10, c)
        d.rect(2, 11, 13, 12, (220, 160, 90))
    elif soort == "pannenkoek":
        d.disc(8, 10, 6, (240, 190, 110))
        d.disc(8, 9, 5, (250, 210, 130))
        d.disc(8, 8, 2.5, c)
    elif soort == "feest":
        d.disc(8, 10, 7, (240, 240, 250))
        d.disc(5, 9, 2.4, (255, 150, 90))
        d.disc(10, 8, 2.4, c)
        d.disc(9, 11, 2, (120, 200, 120))
        d.disc(6, 12, 1.6, (255, 220, 120))
    else:  # bord
        d.disc(8, 9, 6.5, (240, 240, 250))
        d.disc(8, 9, 4.5, c)
        d.disc(6, 8, 1.4, (255, 220, 120))
        d.disc(10, 10, 1.3, (120, 200, 120))
    d.schaduw()
    return d.rand()


def ring(c):
    d = Doek()
    d.disc(8, 9, 5, (250, 200, 80))
    d.disc(8, 9, 3.2, (0, 0, 0))
    d.wis(6, 7, 10, 11)
    d.disc(8, 4, 2.2, c)
    d.px(7, 3, (255, 255, 255))
    return d.rand()


def ketting(c):
    d = Doek()
    for i in range(14):
        h = math.pi * 0.1 + i / 13 * math.pi * 0.8
        d.px(8 + math.cos(h) * 6, 3 + math.sin(h) * 7, (250, 200, 80))
    d.disc(8, 11, 2.4, c)
    d.px(7, 10, (255, 255, 255))
    return d.rand()


def oorbel(c):
    d = Doek()
    d.disc(8, 3, 1.5, (250, 200, 80))
    d.lijn(8, 4, 8, 8, (250, 200, 80))
    d.poly([(8, 7), (5, 11), (8, 15), (11, 11)], c)
    d.px(7, 10, (255, 255, 255))
    return d.rand()


def _item(naam, doek, handheld=False):
    doek.png(os.path.join(A, "textures", "item", ITEM_TEX, f"{naam}.png"))
    parent = "minecraft:item/handheld" if handheld else "minecraft:item/generated"
    _schrijf(os.path.join(A, "models", "item", f"{naam}.json"), {"parent": parent, "textures": {"layer0": f"guhs:item/{ITEM_TEX}/{naam}"}})
    _schrijf(os.path.join(A, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


SCHILDER = {"erts": erts, "gruis": gruis, "hout": hout, "hars": hars, "kruid": kruid, "bloem": bloem, "vezel": vezel, "vis": vis,
            "schelp": schelp, "honing": pot, "gelei": pot, "was": was}


def _assets():
    n = 0
    for _id, (s, k, _n, _u) in MATERIALEN.items():
        _item(f"vr_amb_{_id}", SCHILDER[s](_hex(k)))
        n += 1
    for _id, (k, _n, _u) in KERNEN.items():
        _item(f"vr_amb_kern_{_id}", kern(_hex(k)))
        n += 1
    _item("vr_amb_kernsplinter", splinter())
    _item("vr_amb_ambachtskist", kist())
    for _id, (s, k, _n, _u) in ETEN.items():
        _item(f"vr_amb_eten_{_id}", eten(s, _hex(k)))
    for _id, (k, _n) in DRANKEN.items():
        _item(f"vr_amb_drank_{_id}", drankje(_hex(k)))
    zilver, koper = (220, 220, 240), (230, 150, 90)
    _item("vr_amb_mw_zwaard", zwaard(zilver, (255, 120, 190)), True)
    _item("vr_amb_mw_boog", boog((180, 120, 80), (255, 120, 190)), True)
    _item("vr_amb_mw_staf", staf(_orb), True)
    _item("vr_amb_mw_helm", helm(zilver))
    _item("vr_amb_mw_borstplaat", borst(zilver, (255, 120, 190)))
    _item("vr_amb_mw_broek", broek((200, 170, 230)))
    _item("vr_amb_mw_schoentjes", sloffen((255, 170, 210)))
    _item("vr_amb_mw_ring", ring((255, 90, 170)))
    _item("vr_amb_mw_ketting", ketting((120, 220, 255)))
    _item("vr_amb_mw_oorbel", oorbel((255, 200, 60)))
    return n


def build(h):
    _assets()
    for k, nl in TEKSTEN.items():
        h.lang(k, EN.get(k, nl), nl)   # (T-INT: make_v2.lang(key, en, nl))


def _direct():
    _assets()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        if taal == "nl_nl":
            data.update(TEKSTEN)
        else:
            data.update({k: EN.get(k, v) for k, v in TEKSTEN.items()})
        _schrijf(p, data)
    print(f"vadsrijk_t_ambacht: {len(TEKSTEN)} texts ({len(EN)} in English)")


if __name__ == "__main__":
    _direct()
