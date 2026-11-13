"""
1.10.0 "Het Vadsrijk", round U, slice u-ambacht2: the Ambachten grow with zones 4 and 5 (T4/T5, cap 60), coral diving, the
Hoogvads Ambachtsgilde story steps, the Knutselpenningen shop, the Opdrachtenbord (player crafting orders), the Meesterschort path
and the zone-6 hooks ("zone 6 volgt"). Java: feature/vadsrijk/ambacht (on top of t-ambacht's generator vadsrijk_t_ambacht.py).

  - pixel art for the new materials, boss cores, dishes, potions and the guh-kite (the t-ambacht painters); the Meesterschort cape
    texture (64x32, t-kleding's cape layout);
  - every new text (Dutch; en_us real English for the short UI texts, the same Dutch for names and stories).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_ambacht2.py
"""
import json
import os

from vadsrijk_economie import Doek, _licht, drankje
import vadsrijk_t_ambacht as T

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")

# id: (soort, kleur, naam, uitleg)   (same ids as Materialen.java)
MATERIALEN = {
    "lampionbrons": ("erts", 0xF0A050, "Lampionbrons", "Warm brons uit de kliffen van De Lampionkust. Gloeit een beetje, net als een lampion (T4)."),
    "hemelzilver": ("erts", 0xE8F0FF, "Hemelzilver", "Zilver uit de vinnen van Moeder Wolkwal. Licht als een wolkje (T5)."),
    "drijfhout": ("hout", 0xC8B49A, "Drijfhout", "Hout dat de roze zee heeft gladgeaaid (T4)."),
    "vliegerbamboe": ("hout", 0xB8D86A, "Vliegerbamboe", "Buigzaam bamboe van het Vliegereiland. Perfect voor vliegers (T4)."),
    "wolkenhout": ("hout", 0xF0EAF8, "Wolkenhout", "Bijna gewichtloos hout uit het Staartpark van Hoogvads (T5)."),
    "zeekraal": ("kruid", 0x5AB87A, "Zeekraal", "Een zout, knapperig strandplantje. Lekker bij vis (T4)."),
    "zeegaren": ("vezel", 0x9AE0E0, "Zeegaren", "Garen gesponnen van zeegras. Voor het naaiatelier (T4)."),
    "wolkbes": ("bloem", 0xB8C8FF, "Wolkbes", "Een blauwe bes die naar regen smaakt, uit de tuinen van Hoogvads (T5)."),
    "wolkenwol": ("vezel", 0xFAFAFF, "Wolkenwol", "Plukjes wolk, zo zacht als een knuffel. Voor het naaiatelier (T5)."),
    "koraalbaars": ("vis", 0xFF7A8A, "Koraalbaars", "Een roze baars die tussen het koraal woont (T4)."),
    "maanrog": ("vis", 0x8A8AE0, "Maanrog", "Bijt alleen 's nachts. Zwemt als een vliegertje (T4)."),
    "regenboogmakreel": ("vis", 0x7AE0C8, "Regenboogmakreel", "Komt boven als het regent. Glimt in alle kleuren (T4)."),
    "roze_koraal": ("koraal", 0xFF6FA8, "Roze Koraal", "Losgewrikt bij het duiken in de Koraalbaai. Groeit weer aan (T4)."),
    "zeesponsje": ("spons", 0xFFD87A, "Zeesponsje", "Een zacht geel sponsje van de zeebodem. Duikvondst (T4)."),
    "wolkenvis": ("vis", 0xE8F4FF, "Wolkenvis", "Een doorzichtig visje uit de vijvers van Hoogvads (T5)."),
    "sterrenspiering": ("vis", 0xFFF0A0, "Sterrenspiering", "Bijt alleen 's nachts. Twinkelt als een sterretje (T5)."),
    "donderforel": ("vis", 0x8AA0D8, "Donderforel", "Komt boven als het regent. Rommelt in je hengel (T5)."),
    "lampionhoning": ("honing", 0xFF9A3A, "Lampionhoning", "Oranje honing van de Bijenboerderij op het Bloemeneiland (zone 4)."),
    "duinhoning": ("honing", 0xF0D8A0, "Duinhoning", "Lichte honing van de duinbloemen bij het Strandfeest (zone 4)."),
    "wolkenhoning": ("honing", 0xFFF8F0, "Wolkenhoning", "Bijna witte honing uit het Staartpark van Hoogvads (zone 5)."),
    "hemelhoning": ("honing", 0xFFE070, "Hemelhoning", "Gouden honing uit de Ooggaarde van Hoogvads (zone 5)."),
}
KERNEN = {
    "lampionkern": (0xFF9A3A, "Lampionkern", "Een bazenkern uit zone 4. Nodig voor Meesterwerk-sieraden (T4)."),
    "wolkkern": (0xE8F0FF, "Wolkkern", "Een bazenkern uit zone 5. Nodig voor Meesterwerk-sieraden (T5)."),
}
ETEN = {
    "zeekraalsalade": ("kom", 0x5AB87A, "Zeekraalsalade", "Elementweerstand +8%, Vads +260."),
    "koraalvissoep": ("kom", 0xFF7A8A, "Koraalvissoep", "Verdediging +18, Vads +280."),
    "zeemanshapje": ("broodje", 0x9AE0E0, "Zeemanshapje", "Nog meer ambachts-xp en nog vaker dubbele oogst."),
    "lampionspiesjes": ("bord", 0xFF9A3A, "Lampionspiesjes", "Kritiek +4%, Kracht +9."),
    "wolkbessentaart": ("pannenkoek", 0xB8C8FF, "Wolkbessentaart", "Heal-kracht +7%, Wijsheid +14, Vads +320."),
    "donderstoofpot": ("bord", 0x8AA0D8, "Donderstoofpot", "Verdediging +22, Vads +360."),
    "hemelfeestmaal": ("feest", 0xFFE070, "Hemelfeestmaal", "Voor je hele party: de grootste buff plus Gezellig."),
}
DRANKEN = {
    "zee_vadsdrank": (0xFF4F8A, "Gebrouwen Zee-Vadsdrank"), "zee_energiedrank": (0x3AC8C8, "Gebrouwen Zee-Energiedrank"),
    "wolken_vadsdrank": (0xFFB8DC, "Gebrouwen Wolken-Vadsdrank"), "wolken_energiedrank": (0xA8C8FF, "Gebrouwen Wolken-Energiedrank"),
    "hemelelixer": (0xFFE070, "Hemelelixer"),
}
TIER_NAAM = {4: ("Lampionbronzen", "Zeegaren", "Lampionkern-"), 5: ("Hemelzilveren", "Wolkenwollen", "Wolkkern-")}
RECEPTEN = {
    "zeekraalsalade": "Zeekraalsalade", "koraalvissoep": "Koraalvissoep", "zeemanshapje": "Zeemanshapje", "lampionspiesjes": "Lampionspiesjes",
    "wolkbessentaart": "Wolkbessentaart", "donderstoofpot": "Donderstoofpot", "hemelfeestmaal": "Hemelfeestmaal",
    "zee_vadsdrank": "Zee-Vadsdrank", "zee_energiedrank": "Zee-Energiedrank", "wolken_vadsdrank": "Wolken-Vadsdrank",
    "wolken_energiedrank": "Wolken-Energiedrank", "hemelelixer": "Hemelelixer",
    "kern_lampion": "Lampionkern (uit splinters)", "kern_wolk": "Wolkkern (uit splinters)", "post_vlieger": "Guh-vlieger (naar huis)",
}
for _s in ("zwaard", "boog", "staf", "helm", "borstplaat", "broek", "schoentjes", "ring", "ketting", "oorbel"):
    for _t in (4, 5):
        RECEPTEN[f"{_s}_t{_t}"] = T.MW_TIER[_s].format(*TIER_NAAM[_t]).replace("- ", "-")

GROEPEN = {
    "z4_pluk_bloemeneiland": "het Bloemeneiland", "z4_pluk_knuffeltij": "het eiland van het Knuffeltij", "z4_pluk_strandfeest": "de duinen bij het Strandfeest",
    "z4_hout_vliegereiland": "het Vliegereiland", "z4_hout_vuurtoren": "het Vuurtoreneiland", "z4_hout_mysterie": "het Mysterie-eiland",
    "z4_mijn_pierkop": "de kliffen van Pierkop", "z4_mijn_vuurtoren": "de rotsen van het Vuurtoreneiland", "z4_mijn_mysterie": "het strand van het Mysterie-eiland",
    "z4_vis_haven": "de visplek van Lampionhaven", "z4_vis_kade": "de steigers van Lampionhaven", "z4_vis_koraal": "de Koraalbaai",
    "z4_vis_wrak": "bij het wrak", "z4_vis_vuurtoren": "de steiger van het Vuurtoreneiland",
    "z4_duik_koraalbaai": "duiken bij de duiksteiger (Koraalbaai)", "z4_duik_grotjes": "duiken rond de Koraalbaai",
    "z4_korf_bloemeneiland": "de Bijenboerderij op het Bloemeneiland", "z4_korf_duinen": "de duinbloemen bij het Strandfeest",
    "z5_pluk_staartpark": "het Staartpark", "z5_pluk_ooggaarde": "de Ooggaarde", "z5_pluk_wolkbessen": "de wolkbessenstruiken",
    "z5_hout_staartpark_1": "de bomen van het Staartpark (zuid)", "z5_hout_staartpark_2": "de bomen van het Staartpark (noord)",
    "z5_hout_ooggaarde": "de bomen van de Ooggaarde", "z5_mijn_noordvin": "de noordvin van Moeder Wolkwal", "z5_mijn_zuidvin": "de zuidvin van Moeder Wolkwal",
    "z5_mijn_staartvin": "de staartvin van Moeder Wolkwal", "z5_vis_staartvijver": "de vijver in het Staartpark", "z5_vis_fontein": "de fontein op de Marktlaan",
    "z5_vis_spuitgat": "het spuitgat van Moeder Wolkwal", "z5_korf_staartpark": "de bijenwei in het Staartpark", "z5_korf_ooggaarde": "de bijenwei in de Ooggaarde",
}

# the Gilde story steps (Hoogvads' Ambachtsgilde): per ambacht 2 steps (lv 55 / 60) x (teacher, you, teacher)
GILDE = {
    "smeden": [
        ("Welkom in de smidse van het Gilde! Het Aambeeld van de Orde is heel, maar Hoogvads heeft er nog een: het Wolkaambeeld.",
         "Nog een aambeeld?", "Het hangt aan kettingen boven de stad. Smeed eerst iets moois van lampionbrons, dan mag je erop."),
        ("Hoor je dat? Het Wolkaambeeld zingt als jij erop slaat. Dat doet het alleen voor meesters.", "Ben ik nu een meester?",
         "Een Meester-smid! Hier in Hoogvads hangen we je hamer aan de muur. Naast die van de oude Orde."),
    ],
    "naaien": [
        ("Daar ben je! De quilt hangt nu in de Gildehal. Er kan nog een lapje bij: eentje van De Lampionkust.", "Van zeegaren?",
         "Van zeegaren, met een vliegertje erop geborduurd. Dan wappert de quilt een beetje."),
        ("En nu het laatste lapje: wolkenwol. Zo zacht dat de Keizer er misschien zelf onder wil slapen.", "Zou hij dat willen?",
         "Iedereen wil een knuffel, lieverd. Zelfs keizers. Jij bent nu Meester-naaier."),
    ],
    "koken": [
        ("Kijk eens wat de duikers vonden: een bladzijde van Oma Truus, in een fles, in de Koraalbaai!", "Wat staat erop?",
         "'Visjes van de roze zee, met een snufje zeekraal.' Maak het maar, dan proeven we samen."),
        ("De allerlaatste bladzijde lag in de Kathedraal: 'Het koekje dat de Keizer nooit kreeg... begint met wolkbessen.'", "Wolkbessen!",
         "Wolkbessen. De rest mogen we zelf verzinnen. Meester-kok, dat ben je nu. Oma zou trots zijn."),
    ],
    "brouwen": [
        ("Blub! De zee maakt de kristallen hier niet grauw, maar zout. Zout kristal blijft langer roze, wist je dat?", "Dus zeekraal helpt?",
         "Zeekraal en een sponsje uit de diepte. Het ont-grauw-drankje wordt steeds beter."),
        ("Met hemelhoning blijft een kristal zelfs in de mist roze. Het Hemelelixer! Mijn meesterwerk... nee, ONS meesterwerk.", "Ons meesterwerk!",
         "Blub blub! Meester-brouwer. Laat de Vadsleidingen maar stralen."),
    ],
    "imkeren": [
        ("Zzzoem, welkom in de bijenstal van het Gilde. Onze bijen vliegen helemaal naar De Lampionkust en terug.", "Zo ver?",
         "Ze willen lampionhoning! Zet een korf op het Bloemeneiland, dan hoeven ze niet zo ver te vliegen."),
        ("De bijen van Hoogvads dansen in de wolken. Ze maken hemelhoning, de zoetste van het hele Vadsrijk.", "Mag ik een dansje leren?",
         "Je kent het al: ze dansen voor jou. Meester-imker. Zzzoem!"),
    ],
    "mijnen": [
        ("Bonk is de mijnmeester van Zwamzoet, ik ben die van Hoogvads. Hij schreef dat jij goed tikt.", "Bonk! Bonk! Bonk!",
         "Haha, ja, precies zo. Op Pierkop ligt lampionbrons. Tik zachtjes, het gloeit nog na."),
        ("Weet je waar hemelzilver vandaan komt? Uit de vinnen van Moeder Wolkwal. Ze vindt het niet erg, het kriebelt haar.", "Dankjewel, Moeder Wolkwal!",
         "Ze knipoogt terug, zie je? Meester-mijner, dat ben je nu."),
    ],
    "houthakken": [
        ("Op het Vliegereiland groeit vliegerbamboe. Buigzaam, licht, en het fluit in de wind.", "Mag ik een vlieger maken?",
         "Hak eerst wat bamboe, en vraag de naaiers. Samen maken jullie de mooiste vliegers."),
        ("Wolkenhout weegt bijna niks. Je kunt er een huis van bouwen dat blijft zweven.", "Een zwevend huisje!",
         "Ooit, misschien. Eerst ben je Meester-houthakker. Het bos van het Staartpark fluistert je naam."),
    ],
    "plukken": [
        ("Zeekraal! Proef maar, het knispert en het smaakt naar de zee.", "Zout!",
         "Zout en lief, net als de zeeguhs. Pluk het zachtjes, dan groeit het terug."),
        ("De wolkbessen in de Ooggaarde zijn rijp. Je moet ze plukken voordat ze wegdrijven.", "Wegdrijven?!",
         "Hihi, echt waar. Snel zijn en zacht zijn: dat is een Meester-plukker. Dat ben jij."),
    ],
    "vissen": [
        ("Ahoy! Je bent in de Koraalbaai gaan duiken, hoorde ik. Dobbertje uit Zwamzoet wist het al: jij komt ver.", "Het koraal was zo roze!",
         "En het groeit weer aan als je het zachtjes losmaakt. Zo zorgen we voor de zee."),
        ("Er zwemmen vissen in de fontein van de Marktlaan. Wolkenvisjes. Niemand weet hoe ze daar komen.", "Misschien regende het vis?",
         "Misschien wel! Meester-visser, jij mag dat raadsel houden."),
    ],
}

TEKSTEN = {}
EN = {}


def t(k, nl, en=None):
    TEKSTEN[k] = nl
    if en is not None:
        EN[k] = en


S = "vadsrijk.ambacht."
# ---- tabs, options, workshop gates, the zone-4 diving
t(S + "tab.opdracht", "Opdrachten", "Orders")
t(S + "tab.winkel", "Knutselwinkel", "Craft shop")
t(S + "optie.opdracht", "Opdrachtenbord (van spelers)", "Player order board")
t(S + "optie.winkel", "Knutselwinkel", "Craft shop")
t(S + "werk.gilde", "Alleen in het Ambachtsgilde van Hoogvads.", "Only at the Ambachtsgilde in Hoogvads.")
t(S + "werk.gilde4", "Alleen in het Ambachtsgilde van Hoogvads (of in Lampionhaven).", "Only at the Ambachtsgilde in Hoogvads (or in Lampionhaven).")
t(S + "vis.duik", "Een duikvondst (geen vis).", "A diving find (not a fish).")
t(S + "mini.duiken", "Duiken! Houd vast en laat los in het groen", "Diving! Hold and let go in the green")
for _z in (4, 5):
    t(S + f"boek.cat.plekken.{_z}", f"Plekken zone {_z}", f"Spots zone {_z}")
for _z in (4, 5, 6):
    t(S + f"tier.{_z}", f"tier {_z}", f"tier {_z}")
t(S + "boek.hint.plek", "Een plek voor %s. Vraag de meesterguh in Zwamzoet, of het Gilde in Hoogvads, waar.",
  "A spot for %s. Ask the master guh in Zwamzoet, or the Gilde in Hoogvads.")
t(S + "bestel.kist_open", "In de kist: kernsplinters, iets zeldzaams, 5 zilver en Knutselpenningen!",
  "In the chest: core splinters, something rare, 5 silver and craft coins!")
# ---- the Gilde story
t(S + "verhaal.naar_gilde", "Mijn verhaal is uit, maar het jouwe niet! Ga naar het Ambachtsgilde in Hoogvads, daar vertellen ze verder.",
  "My story is done, but yours isn't! Go to the Ambachtsgilde in Hoogvads, they'll tell you more.")
t(S + "gilde.eerst", "Eerst het verhaal van je leermeester afmaken: %s. Die woont in de hubs beneden. Kom daarna terug!",
  "First finish your teacher's story: %s. They live in the hubs below. Then come back!")
t(S + "gilde.uit", "Je kent het hele verhaal van het Gilde. Als het Vadsrijk groeit, groeit ons verhaal mee!",
  "You know the Gilde's whole story. When the Vadsrijk grows, our story grows too!")
for _a, _stappen in GILDE.items():
    for _i, _regels in enumerate(_stappen):
        for _k, _r in enumerate(_regels):
            t(f"{S}gilde.{_a}.{_i}.{_k}", _r)
# ---- Knutselpenningen + the shop
t(S + "penning.erbij", "+%s Knutselpenningen (je hebt er nu %s)", "+%s craft coins (you now have %s)")
t(S + "penning.stand", "Knutselpenningen: %s", "Craft coins: %s")
t(S + "winkel.kop", "De Knutselwinkel", "The Craft Shop")
t(S + "winkel.uitleg", "Knutselpenningen verdien je met bestellingen, opdrachten van spelers en verhalen.",
  "You earn craft coins with orders, player orders and stories.")
t(S + "winkel.prijs", "%s penningen", "%s coins")
t(S + "winkel.koop", "Koop", "Buy")
t(S + "winkel.naar_huis", " (naar huis)", " (home)")
t(S + "winkel.thee", "Uitrustthee (+30 min uitgerust)", "Resting tea (+30 min rested)")
t(S + "winkel.gekocht", "Gekocht!", "Bought!")
t(S + "winkel.post", "Gekocht! Het gaat met de Post naar huis.", "Bought! It goes home by Post.")
t(S + "winkel.te_weinig", "Daar heb je niet genoeg Knutselpenningen voor.", "Not enough craft coins.")
t(S + "winkel.onbekend", "Dat verkopen we niet.", "We don't sell that.")
t(S + "winkel.uitgerust_vol", "Je bent al helemaal uitgerust.", "You're already fully rested.")
# ---- the Opdrachtenbord
t(S + "opdracht.kop", "Opdrachten van spelers", "Orders from players")
t(S + "opdracht.leeg", "Er hangen nog geen opdrachten. Zet er zelf een op!", "No orders yet. Post one yourself!")
t(S + "opdracht.van", "gevraagd door %s", "asked by %s")
t(S + "opdracht.door", "gemaakt door %s: haal het op!", "made by %s: pick it up!")
t(S + "opdracht.jij", "jou", "you")
t(S + "opdracht.klaar", "Klaar!", "Done!")
t(S + "opdracht.trek", "Intrekken", "Withdraw")
t(S + "opdracht.lever", "Lever", "Deliver")
t(S + "opdracht.heeft", "Jij hebt er %s van de %s.", "You have %s of %s.")
t(S + "opdracht.kent_niet_tip", "Dit recept ken je nog niet (je hebt er %s van de %s).", "You don't know this recipe yet (you have %s of %s).")
t(S + "opdracht.nieuw", "Nieuwe opdracht", "New order")
t(S + "opdracht.kan_maken", "%s niveau %s", "%s level %s")
t(S + "opdracht.aantal_n", "Aantal: %s", "Amount: %s")
t(S + "opdracht.beloning_label", "Beloning (gaat nu al in de pot):", "Reward (goes into the pot now):")
t(S + "opdracht.plaats", "Ophangen", "Post")
t(S + "opdracht.open_n", "Jouw open opdrachten: %s / %s", "Your open orders: %s / %s")
t(S + "opdracht.ophalen", "Ophalen (%s klaar)", "Pick up (%s done)")
t(S + "opdracht.geplaatst", "Je opdracht hangt op het bord! De beloning zit in de pot.", "Your order is on the board! The reward is in the pot.")
t(S + "opdracht.geleverd", "Geleverd! De beloning is voor jou.", "Delivered! The reward is yours.")
t(S + "opdracht.ingetrokken", "Ingetrokken: je krijgt je munten terug.", "Withdrawn: you get your coins back.")
t(S + "opdracht.opgehaald", "Opgehaald!", "Picked up!")
t(S + "opdracht.niks_op_te_halen", "Er ligt niks voor je klaar.", "Nothing to pick up.")
t(S + "opdracht.klaar_melding", "%s heeft je opdracht gemaakt: %s. Haal het op bij een bordguh of in het Ambachtenboek!",
  "%s made your order: %s. Pick it up at a board guh or in the Professions book!")
t(S + "opdracht.tegoed", "Een oude opdracht is verlopen: %s terug.", "An old order expired: %s back.")
t(S + "opdracht.onbekend", "Dat kun je niet bestellen.", "That can't be ordered.")
t(S + "opdracht.te_veel", "Je hebt al genoeg open opdrachten.", "You already have enough open orders.")
t(S + "opdracht.aantal", "Dat aantal kan niet.", "That amount isn't possible.")
t(S + "opdracht.beloning", "De beloning is te laag (minstens 25 brons per stuk).", "The reward is too low (at least 25 bronze each).")
t(S + "opdracht.geen_geld", "Je hebt niet genoeg munten voor die beloning.", "You don't have enough coins for that reward.")
t(S + "opdracht.weg", "Die opdracht is er niet meer.", "That order is gone.")
t(S + "opdracht.eigen", "Je eigen opdracht kun je niet zelf leveren.", "You can't deliver your own order.")
t(S + "opdracht.kent_niet", "Dit recept ken je nog niet.", "You don't know this recipe yet.")
t(S + "opdracht.niet_genoeg", "Je hebt niet genoeg van wat er gevraagd wordt bij je.", "You don't have enough of what's asked.")
# ---- the Meesterschort path
t(S + "meester.nu", "Je bent nu Meester in %s (niveau %s)! De Meesterschort ligt in je Kledingkast.",
  "You are now a Master of %s (level %s)! The Master's Apron is in your wardrobe.")
t(S + "meester.kort", "Meester", "Master")
for _id, (_nl, _en) in T.AMBACHTEN.items():
    t(f"gui.guhs.titels.naam.vr_amb_{_id}_meester", f"Meester-{_nl.lower()}" if _id != "vissen" else "Meester-visser", f"Master ({_en})")
    t(f"gui.guhs.titels.hint.vr_amb_{_id}_meester", f"Haal het hoogste niveau in {_nl.lower()}.", f"Reach the highest level in {_en}.")
t("gui.guhs.titels.naam.vr_amb_grootmeester", "Grootmeester der Ambachten", "Grand Master of Crafts")
t("gui.guhs.titels.hint.vr_amb_grootmeester", "Haal het hoogste niveau in alle negen ambachten.", "Reach the highest level in all nine professions.")
t("vadsrijk.kleding.ding.kaap.meesterschort", "Meesterschort", "Master's Apron")
t("vadsrijk.kleding.ding.kaap.meesterschort.bron", "Haal het hoogste niveau in een ambacht.", "Reach the highest level in a profession.")
# ---- names
for _id, _n in GROEPEN.items():
    t(S + "groep." + _id, _n)
for _id, _n in RECEPTEN.items():
    t(S + "recept." + _id, _n)
for _id, (_s, _k, _n, _u) in MATERIALEN.items():
    t(f"item.guhs.vr_amb_{_id}", _n)
    t(f"item.guhs.vr_amb_{_id}.uitleg", _u)
for _id, (_k, _n, _u) in KERNEN.items():
    t(f"item.guhs.vr_amb_kern_{_id}", _n)
    t(f"item.guhs.vr_amb_kern_{_id}.uitleg", _u)
for _id, (_s, _k, _n, _u) in ETEN.items():
    t(f"item.guhs.vr_amb_eten_{_id}", _n)
for _id, (_k, _n) in DRANKEN.items():
    t(f"item.guhs.vr_amb_drank_{_id}", _n)
t("item.guhs.vr_amb_vlieger", "Guh-vlieger", "Guh Kite")
t("item.guhs.vr_amb_vlieger.uitleg", "Gemaakt van vliegerbamboe en zeegaren. Voor thuis: hang hem op, hij lacht naar je.",
  "Made of kite bamboo and sea yarn. For home: hang it up, it smiles at you.")
for _id in T.MW:
    for _t in (4, 5):
        t(f"vadsrijk.loot.mw_t{_t}_{_id}", T.MW_TIER[_id].format(*TIER_NAAM[_t]).replace("- ", "-"))
# the board guhs of Lampionhaven and Hoogvads
for _npc, _naam in {"amb_bord_z4": "Klerk Kwastje", "amb_bord_z5": "Klerk Kroontje"}.items():
    t("vadsrijk.npc.naam." + _npc, _naam)
    for _i, _r in enumerate(("Bestellingen, opdrachten en penningen!", "Wie maakt er iets moois voor een ander?", "Knutselpenningen? Die verdien je hier!")):
        t(f"vadsrijk.npc.praat.{_npc}.{_i + 1}", _r)
t(S + "npc.bord.hallo", "Hallo! Hier hangen de bestellingen van de stad en de opdrachten van spelers. En in de Knutselwinkel ruil je je penningen.",
  "Hello! Here are the town's orders and the players' orders. And in the craft shop you trade your coins.")
# ---- zone 6 volgt: names ready for the T6 hooks (Zone6Haken), not used yet
ZONE6 = {
    "sterrengoud": ("Sterrengoud", "Goud uit de sterrenduinen van het Zijdeland (T6)."),
    "rozenzandsteen": ("Rozenzandsteen", "Roze steen uit de Rozenkloof (T6)."),
    "dadelpalmhout": ("Dadelpalmhout", "Hout van de palmen bij de oases (T6)."),
    "wierookhars": ("Wierookhars", "Geurige hars uit het Zijdeland (T6)."),
    "saffraan": ("Saffraan", "Het oranje goud van de saffraanvelden (T6)."),
    "zijdedraad": ("Zijdedraad", "De zachtste draad van de Zijderoute (T6)."),
    "oasekarper": ("Oasekarper", "Een karper uit een oase (T6)."),
    "sterrenschubvis": ("Sterrenschubvis", "Bijt alleen 's nachts, onder de sterren (T6)."),
    "zandaal": ("Zandaal", "Komt boven als het (zelden) regent (T6)."),
    "saffraanhoning": ("Saffraanhoning", "Oranje honing van de dadeltuinen (zone 6)."),
}
for _id, (_n, _u) in ZONE6.items():
    t(f"item.guhs.vr_amb_{_id}", _n)
    t(f"item.guhs.vr_amb_{_id}.uitleg", _u)
t("item.guhs.vr_amb_kern_sterrenkern", "Sterrenkern")
t("item.guhs.vr_amb_kern_sterrenkern.uitleg", "Een bazenkern uit zone 6. Nodig voor Meesterwerk-sieraden (T6).")


# ======================================================================================================================
# icons
# ======================================================================================================================
def koraal(c):
    d = Doek()
    d.lijn(8, 15, 8, 7, c)
    d.lijn(8, 10, 4, 5, c)
    d.lijn(8, 9, 12, 4, c)
    d.lijn(4, 5, 3, 2, _licht(c, 1.2))
    d.lijn(12, 4, 13, 1, _licht(c, 1.2))
    d.disc(8, 6, 1.4, _licht(c, 1.25))
    d.schaduw()
    return d.rand()


def spons(c):
    d = Doek()
    d.disc(8, 9, 5.5, c)
    for x, y in ((6, 7), (10, 8), (7, 11), (10, 12), (8, 5)):
        d.disc(x, y, 0.9, _licht(c, 0.7))
    d.schaduw()
    return d.rand()


def vlieger():
    d = Doek()
    roze, licht = (255, 140, 200), (255, 220, 240)
    d.poly([(8, 1), (14, 7), (8, 13), (2, 7)], roze)
    d.lijn(8, 1, 8, 13, licht)
    d.lijn(2, 7, 14, 7, licht)
    d.poly([(4, 3), (5.5, 1), (6.5, 3.5)], roze)     # guh ears
    d.poly([(12, 3), (10.5, 1), (9.5, 3.5)], roze)
    d.px(6, 6, (40, 26, 40))
    d.px(10, 6, (40, 26, 40))
    for i, (x, y) in enumerate(((8, 14), (7, 15), (9, 15))):
        d.px(x, y, (120, 200, 255) if i else (255, 255, 255))
    d.schaduw()
    return d.rand()


SCHILDER = dict(T.SCHILDER)
SCHILDER.update({"koraal": koraal, "spons": spons})


def kaap_meesterschort(pad):
    """64x32 cape (t-kleding's vanilla cape layout): a pink apron with a gold rim, a hammer-and-needle badge and a pocket."""
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("vadsrijk_u_ambacht2: no PIL, the Meesterschort texture is skipped")
        return
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    roze, goud, wit, donker = (255, 150, 200, 255), (255, 200, 70, 255), (255, 250, 252, 255), (200, 80, 140, 255)
    d.rectangle([0, 0, 22, 17], fill=roze)
    d.rectangle([12, 1, 21, 16], fill=(255, 210, 230, 255))   # inside
    for x in range(1, 11):
        im.putpixel((x, 16), goud)
        im.putpixel((x, 1), goud)
    for y in range(1, 17):
        im.putpixel((1, y), goud)
        im.putpixel((10, y), goud)
    d.rectangle([3, 10, 8, 14], fill=wit)                     # the pocket
    d.rectangle([3, 10, 8, 10], fill=donker)
    for (x, y) in ((4, 4), (5, 5), (6, 6), (7, 7)):           # a needle
        im.putpixel((x, y), wit)
    d.rectangle([6, 3, 8, 4], fill=goud)                      # a little hammer
    im.putpixel((7, 5), donker)
    im.putpixel((7, 6), donker)
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    im.save(pad)


def _assets():
    for _id, (s, k, _n, _u) in MATERIALEN.items():
        T._item(f"vr_amb_{_id}", SCHILDER[s](T._hex(k)))
    for _id, (k, _n, _u) in KERNEN.items():
        T._item(f"vr_amb_kern_{_id}", T.kern(T._hex(k)))
    for _id, (s, k, _n, _u) in ETEN.items():
        T._item(f"vr_amb_eten_{_id}", T.eten(s, T._hex(k)))
    for _id, (k, _n) in DRANKEN.items():
        T._item(f"vr_amb_drank_{_id}", drankje(T._hex(k)))
    T._item("vr_amb_vlieger", vlieger())
    kaap_meesterschort(os.path.join(A, "textures", "vadsrijk", "kleding", "kaap_meesterschort.png"))


def build(h):
    _assets()
    for k, nl in TEKSTEN.items():
        h.lang(k, EN.get(k, nl), nl)   # (make_v2.lang(key, en, nl))


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
        T._schrijf(p, data)
    print(f"vadsrijk_u_ambacht2: {len(TEKSTEN)} texts ({len(EN)} in English)")


if __name__ == "__main__":
    _direct()
