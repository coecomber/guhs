"""
biomes3 slice "systemen" (Java: feature/bio/systemen; English: tools/lang/en/c90_bio_systemen.json; contract: CONTRACT_BIO.md).

Closing: what ties the three new biomes into the rest of the mod.
  - the FTB quests of everything the ten other slices built (ftb(fq) below; the three biome quests themselves stand in
    BIOMES of tools/make_ftbquests.py, the sections in its SECTIONS). No chapter of its own; nothing is locked;
  - five hidden proofs no building slice made (feature/bio/systemen/Bewijzen.java);
  - four titles (BioTitels.java): Weeb, Hoofd in de wolken, Gezondheid!, Koifluisteraar;
  - the Guhdex section of the weebhuisje (BioGids.java).
The four Reisbureau destinations of biomes3 (bloesemmeertje, klaterdal, wolkenweide, japan) live in the Reisbureau's own
tables: tools/features/guhpixel_reisbureau_tekst.py / _modellen.py / _tex.py and feature/guhpixel/reisbureau.

It is the LAST biomes3 module: its build ends with the kern's check over everything the slices made (keep that line).
"""
import os

from features import bio
from features import bio_lib as lib

N = "biosystemen"
ADVANCEMENTS = ("kompas_biome", "kompas_gevonden", "kikker_op_blad", "hoogste_eiland", "reus_drie")
TITELS = {
    "weeb": ("Weeb", "Maak de Japan-verzameling van het weebhuisje compleet"),
    "wolkentop": ("Hoofd in de wolken", "Klim naar het allerhoogste eilandje boven een Wolkenweide"),
    "gezondheid": ("Gezondheid!", "Laat je drie keer een wolkenkasteeltje uit niezen"),
    "koifluisteraar": ("Koifluisteraar", "Leer alles over het meer van de visser-guh"),
}
G = f"gui.guhs.{N}.gids."
TEXTS = {
    G + "kop": "Het weebhuisje",
    G + "uitleg": "Elke keer dat je Evivads en Nielsvads uitzwaait, nemen ze een cadeautje voor je mee uit Japan. Een dubbel stuk "
                  "van de verzameling kun je bij hen ruilen voor eentje die je nog mist. Njeg!",
    G + "cadeaus": "Cadeautjes gekregen",
    G + "reeks": "Japan-verzameling",
    G + "outfits": "Japanse pakjes voor je guh",
    G + "tip": "Uit de Japan-verzameling van Evivads en Nielsvads",
}
for _id, (_naam, _hint) in TITELS.items():
    TEXTS[f"gui.guhs.titels.naam.{N}_{_id}"] = _naam
    TEXTS[f"gui.guhs.titels.hint.{N}_{_id}"] = _hint

# the twelve pieces of the Japan collection (bouw-dal's hidden proofs have the block ids as names)
REEKS = ("japan_geluksguh", "japan_lampion", "japan_mini_torii", "japan_ramenkom", "japan_daruma_guh", "japan_waaier", "japan_theeservies",
         "japan_kokeshi_guh", "japan_koinobori", "japan_bonsai_schaaltje", "japan_windgong", "japan_maneki_knabbel")

# FTB: section id -> the quest keys in it (tools/make_ftbquests.py reads this; all in the existing chapter "De Guhmensie")
FTB_SECTIES = {
    "bio_meer": ["bio_botenhuisje", "bio_visser", "bio_koivoer", "bio_koi_voeren", "bio_koi_emmer", "bio_roeien", "bio_visser_klaar",
                 "bio_picknickeilandje", "bio_mand", "bio_hanami", "bio_bloesemguh", "bio_kikker_blad", "bio_bloesemblaadjes"],
    "bio_dal": ["bio_torii", "bio_tanukiguh", "bio_theehuisje", "bio_bouw_torii", "bio_bouw_dak", "bio_bouw_shoji", "bio_bouw_zen"],
    "bio_weeb": ["bio_weebhuisje", "bio_weeb_ontmoet", "bio_weeb_reis", "bio_weeb_cadeau", "bio_japan_eten", "bio_japan_outfits", "bio_japan_reeks"],
    "bio_weide": ["bio_wolkenhoeder_hut", "bio_schaapje_scheren", "bio_hoeder_les", "bio_schaapje_thuis", "bio_wolkguh", "bio_luchtballon_haven",
                  "bio_vaart", "bio_sterrenwacht_ruine", "bio_sterrenstof", "bio_bouw_wolkenbank"],
    "bio_lucht": ["bio_regenboogbrug", "bio_pot", "bio_bouw_regenboog", "bio_bliksemsmidse", "bio_smid_ruil", "bio_wolkenkasteeltje", "bio_sluipen",
                  "bio_top"],
}
FTB_PORTAAL = ["bio_kompas_biomes", "bio_kompas_gevonden"]   # (added to the existing section "Het guhportaal", after the Superkompas)


def build(h):
    lib.teksten(h, TEXTS)
    for a in ADVANCEMENTS:
        # (a hidden proof guhs:quest/<name>, granted with GuhAdvancements.grant; FTB tasks check it)
        h.w(f"{h.D}/advancement/quest/{N}_{a}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    selfcheck(h)
    bio.selfcheck(h)


def selfcheck(h):
    problems = []
    for a in ADVANCEMENTS:
        if not os.path.exists(f"{h.D}/advancement/quest/{N}_{a}.json"):
            problems.append(f"missing data/guhs/advancement/quest/{N}_{a}.json")
    # every quest below asks for something that exists: a proof advancement, an item, a structure
    eisen = _Opnemer()
    ftb(eisen)
    keys = [x[0] for x in eisen.quests]
    geplaatst = [k for ks in FTB_SECTIES.values() for k in ks] + FTB_PORTAAL
    if sorted(keys) != sorted(geplaatst):
        problems.append(f"FTB_SECTIES / FTB_PORTAAL and ftb() differ: {sorted(set(keys) ^ set(geplaatst))}")
    for key, _titel, _desc, icoon, taken, beloning in eisen.quests:
        for t in taken:
            if t["type"] == "advancement":
                ns, pad = t["advancement"].split(":", 1)
                if ns == "guhs" and not os.path.exists(f"{h.D}/advancement/{pad}.json"):
                    problems.append(f"quest {key}: no advancement {t['advancement']}")
            elif t["type"] == "structure":
                if not os.path.exists(f"{h.D}/worldgen/structure/{t['structure'].split(':', 1)[1]}.json"):
                    problems.append(f"quest {key}: no structure {t['structure']}")
            elif t["type"] == "item":
                if not _item(h, t["item"]):
                    problems.append(f"quest {key}: no item {t['item']}")
        for item in [icoon] + [r[0] for r in beloning]:
            if not _item(h, item):
                problems.append(f"quest {key}: no item {item}")
    for _id in TITELS:
        if f"gui.guhs.titels.naam.{N}_{_id}" not in h.NL or f'"{N}_{_id}"' not in _java("BioTitels.java"):
            problems.append(f"title {N}_{_id}: no text, or not in BioTitels.java")
    if problems:
        raise SystemExit("bio_systemen self-check failed:\n  " + "\n  ".join(problems))


def _java(naam):
    return open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bio", "systemen", naam), encoding="utf-8").read()


def _item(h, ref):
    ns, pad = ref.split(":", 1)
    return ns != "guhs" or os.path.exists(f"{h.A}/models/item/{pad}.json") or os.path.exists(f"{h.A}/items/{pad}.json")


class _Opnemer:
    """Stands in for tools/make_ftbquests.py in the self-check: records what every quest asks."""

    def __init__(self):
        self.quests = []

    def q(self, key, title, desc, icon, tasks, rewards=(("guhs:kaas_knabbels", 8),), deps=(), x=0.0, y=0.0, shape=None, xp=0):
        self.quests.append((key, title, desc, icon, tasks, rewards))

    @staticmethod
    def item(i, count=1):
        return {"type": "item", "item": i, "count": count}

    @staticmethod
    def adv(a):
        return {"type": "advancement", "advancement": a if ":" in a else f"guhs:quest/{a}"}

    @staticmethod
    def structure(s):
        return {"type": "structure", "structure": f"guhs:{s}"}


def ftb(fq):
    """The quests of biomes3 (the three biome quests are in make_ftbquests.BIOMES). Nothing is locked: deps only place them."""
    q, adv, item, st = fq.q, fq.adv, fq.item, fq.structure
    kn = "guhs:kaas_knabbels"

    # ---- the Superkompas tab "Biomes" (section Het guhportaal) ---------------------------------------------------------------
    q("bio_kompas_biomes", "Het tabblad Biomes", "Je &6Superkompas&r heeft een nieuw tabblad: &dBiomes&r. Per wereld staat er een "
      "rijtje met alle biomen. Een rijtje met een slotje gaat open zodra je in die wereld bent geweest. Klap de Guhmensie open, "
      "kies een bioom en houd het kompas vast: het wijst naar de dichtstbijzijnde plek.", "guhs:guhmensie_superkompas",
      [adv(f"{N}_kompas_biome")], rewards=((kn, 8),), xp=50)
    q("bio_kompas_gevonden", "Daar is het!", "Loop met je Superkompas naar het bioom dat je gekozen hebt, tot je er middenin "
      "staat. Achter elk bioom waar je al was staat in het tabblad een vinkje. Njeg, wat veel plekken!", "minecraft:filled_map",
      [adv(f"{N}_kompas_gevonden")], rewards=((kn, 12),), xp=100)

    # ---- het Bloesemmeertje ---------------------------------------------------------------------------------------------------
    q("bio_botenhuisje", "Het botenhuisje", "Aan de oever van een &dBloesemmeertje&r staat een klein botenhuisje met een steiger. "
      "Het &6Superkompas&r wijst de weg (tabblad Knus).", "guhs:botenhuisje_steigerlantaarn", [st("botenhuisje")], rewards=((kn, 12),), shape="circle", xp=100)
    q("bio_visser", "De visser-guh", "Op de steiger zit de &dvisser-guh&r met zijn hengel. Hij vangt nooit iets en dat vindt hij "
      "prima. Praat met hem.", "minecraft:fishing_rod", [adv("botenhuisje_visser")], rewards=((kn, 8),), xp=50)
    q("bio_koivoer", "Een handje koivoer", "De visser-guh geeft je elke dag een handje &6koivoer&r. Vraag er maar om.",
      "guhs:koivoer", [adv("botenhuisje_koivoer")], rewards=((kn, 8),), xp=50)
    q("bio_koi_voeren", "Hap!", "Houd koivoer vast bij het water: de &dkoi&r komen naar je toe gezwommen. Rechtsklik op het water "
      "om een handje te strooien, of geef het aan één koi. Er zijn rood-witte, driekleurige, roze, blauwe en gouden.",
      "guhs:koivoer", [adv("koi_gevoerd")], rewards=(("guhs:koivoer", 4),), xp=50)
    q("bio_koi_emmer", "Een koi voor thuis", "Schep een koi op met een emmer water. Thuis in je eigen vijver blijft hij van jou, "
      "in zijn eigen kleur.", "guhs:koi_emmer", [adv("koi_emmer")], rewards=((kn, 12),), xp=100)
    q("bio_roeien", "Een rondje roeien", "Bij de steiger ligt een &6roeibootje&r. Stap in en roei een rondje over het meer. Het "
      "bootje blijft bij het meer horen.", "minecraft:oak_boat", [adv("roeibootje_gevaren")], rewards=((kn, 8),), xp=50)
    q("bio_visser_klaar", "Het meer leren kennen", "De visser-guh wil je alles over zijn meer leren: de koi, de bloesemblaadjes, "
      "het eilandje. Vier lesjes, in je eigen tempo. Aan het eind krijg je spulletjes voor je eigen plekje aan het water.",
      "minecraft:fishing_rod", [adv("botenhuisje_visser_klaar")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), shape="gear", xp=200)
    q("bio_picknickeilandje", "Hanami!", "Heel soms ligt er op een eilandje in het meer een picknickkleed onder de grootste "
      "bloesemboom: het &dpicknickeilandje&r (Superkompas: Knus).", "minecraft:pink_carpet", [st("picknickeilandje")],
      rewards=((kn, 12),), shape="circle", xp=100)
    q("bio_mand", "Lekkers uit de mand", "Op het kleed staat een &6picknickmand&r. Er zit voor iedereen één keer iets lekkers in. Kijk maar.",
      "guhs:guh_taart", [adv("picknickeilandje_mand")], rewards=((kn, 8),), xp=50)
    q("bio_hanami", "Bloesem kijken", "De guhs op het eilandje vieren &dhanami&r: bloesem kijken. Overdag feesten ze, 's avonds "
      "slapen ze bij het water. Praat met een van hen.", "guhs:guhbloesem_sapling", [adv("hanami_gesproken")], rewards=((kn, 8),), xp=50)
    q("bio_bloesemguh", "De Bloesemguh", "Bij het Bloesemmeertje worden soms &dBloesemguhs&r geboren: bloesemwit, met roze "
      "blaadjes in hun vacht. Kom dichtbij er een voor je Guhdex.", "guhs:guhdex", [adv("seen_bloesemguh")], rewards=((kn, 16),),
      shape="rsquare", xp=100)
    q("bio_kikker_blad", "Kwaak op een blad", "Bij het meer zitten &dkikkerguhs&r op de lelies en de bloesemblaadjes. Kom je te "
      "dichtbij, dan plonzen ze weg. Sluip maar: zie er eentje zitten op zijn blad.", "minecraft:lily_pad",
      [adv(f"{N}_kikker_op_blad")], rewards=((kn, 8),), xp=50)
    q("bio_bloesemblaadjes", "Bloesem op het water", "Op het meer drijven roze &6bloesemblaadjes&r. Pak er een paar mee: je kunt "
      "ze thuis op je eigen vijver leggen, net als een lelieblad.", "guhs:drijvende_bloesemblaadjes",
      [item("guhs:drijvende_bloesemblaadjes", 4)], rewards=((kn, 8),), shape="diamond", xp=50)

    # ---- het Klaterdal --------------------------------------------------------------------------------------------------------
    q("bio_torii", "Door de poort", "In het &dKlaterdal&r staan roze poorten met guhoortjes op de bovenste balk: &dtorii&r. Zoek er "
      "een en loop eronderdoor.", "guhs:roze_lakhout_balk", [st("dal_torii")], rewards=((kn, 8),), shape="circle", xp=50)
    q("bio_tanukiguh", "De Tanukiguh", "In het Klaterdal worden soms &dTanukiguhs&r geboren: grijsbruin, met een maskertje, een "
      "geringde pluimstaart en een blaadje op hun kop. Kom dichtbij er een voor je Guhdex.", "guhs:guhdex", [adv("seen_tanukiguh")],
      rewards=((kn, 16),), shape="rsquare", xp=100)
    q("bio_theehuisje", "Thee bij de waterval", "Op een rots naast een hoge waterval staat een open &dtheehuisje&r. Ga er even "
      "zitten en luister naar het klateren.", "guhs:tatami", [st("dal_theehuisje")], rewards=((kn, 8),), shape="circle", xp=50)
    q("bio_bouw_torii", "Je eigen torii", "Bouw thuis een poort van &6roze lakhout&r: twee palen van vier &6balken&r hoog, daarop "
      "een dwarsbalk van vijf balken en als dakje vijf &6platen&r. Roze lakhoutplanken maak je van esdoornstammen, of door acht "
      "planken roze te lakken met roze verf.",
      "guhs:roze_lakhout_balk", [item("guhs:roze_lakhout_balk", 13), item("guhs:roze_lakhout_plaat", 5)], rewards=((kn, 12),),
      shape="diamond", xp=100)
    q("bio_bouw_dak", "Een dak met een krul", "Leg een dak van &6guh-dakpannen&r (roze, wit of zachtgrijs): trappen voor de "
      "schuine kanten en op elke hoek een &6krulhoek&r, zodat de dakrand omhoog krult.", "guhs:guh_dakpan_roze_hoek",
      [item("guhs:guh_dakpan_roze_trap", 8), item("guhs:guh_dakpan_roze_hoek", 4)], rewards=((kn, 12),), shape="diamond", xp=100)
    q("bio_bouw_shoji", "Schuif maar open", "Maak een kamertje met &6shoji&r als wanden (rechtsklik: ze schuiven echt open en "
      "laten licht door) en een vloer van &6tatami&r.", "guhs:shoji", [item("guhs:shoji", 4), item("guhs:tatami", 9)],
      rewards=((kn, 12),), shape="diamond", xp=100)
    q("bio_bouw_zen", "Een zenhoekje", "Leg een vlakje &6geharkt zand&r met één plek met &6ringen&r, zet er drie gladde "
      "knuffelstenen op en een &6stenen guh-lantaarn&r ernaast. Die gaat vanzelf aan als het donker wordt.", "guhs:geharkt_zand_ring",
      [item("guhs:geharkt_zand", 8), item("guhs:geharkt_zand_ring"), item("guhs:gladde_knuffelsteen", 3), item("guhs:toro")],
      rewards=((kn, 12),), shape="diamond", xp=100)

    # ---- het weebhuisje -------------------------------------------------------------------------------------------------------
    q("bio_weebhuisje", "Het weebhuisje", "In een Klaterdal staat soms één guh-Japans huisje met een luchtballon ernaast: het "
      "&dweebhuisje&r (Superkompas: Knus). Schoenen uit!", "guhs:japan_lampion", [st("weebhuisje")], rewards=((kn, 12),),
      shape="circle", xp=100)
    q("bio_weeb_ontmoet", "Ohayo, guh-chan!", "Praat met &dEvivads&r en &dNielsvads&r. Hun huisje staat vol figuurtjes. Niet "
      "aankomen: dat is een limited edition.", "guhs:japan_kokeshi_guh", [adv("weeb_ontmoet")], rewards=((kn, 8),), xp=50)
    q("bio_weeb_reis", "Ittekimasu!", "Praat nog eens met ze. Wacht... het is kersenbloesemtijd. In Japan. Zwaai ze uit als hun "
      "ballon vertrekt: ze zijn een guhdag weg, met een briefje op de deur.", "guhs:guh_ballon", [adv("weeb_eerste_reis")],
      rewards=((kn, 12),), xp=100)
    q("bio_weeb_cadeau", "Tadaima!", "Kom een dag later terug: ze zijn er weer, met verhalen en een &6cadeautje&r voor iedereen "
      "die ze uitzwaaide. Dat kan zo vaak als je wilt.", "guhs:japan_geluksguh", [adv("weeb_cadeau")], rewards=((kn, 12),),
      shape="gear", xp=100)
    q("bio_japan_eten", "Itadakimasu!", "Soms nemen ze eten mee: &6guh-sushi&r, &6ramen&r, &6mochi&r of &6onigiri&r. Lekker voor "
      "jou en voor je guhs. Proef er eentje.", "guhs:japan_sushi", [item("guhs:japan_sushi")], rewards=((kn, 8),), xp=50)
    q("bio_japan_outfits", "Kawaii!", "Vier pakjes voor je guh komen uit Japan: de &6kimono&r, de &6hachimaki&r, de "
      "&6kattenoortjes&r en het &6strikje&r. Verzamel ze alle vier.", "guhs:japan_waaier", [adv("japan_outfits_compleet")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), xp=200)
    q("bio_japan_reeks", "De hele verzameling", "Twaalf stukken telt de &dJapan-verzameling&r, van de zwaaiende geluksguh tot de "
      "maneki-knabbel. Een dubbele ruil je bij Evivads en Nielsvads voor eentje die je mist. Heb je ze alle twaalf gekregen, dan "
      "ben je een echte &dWeeb&r: die titel mag je dragen. Je verzameling staat in de Guhdex.", "guhs:japan_maneki_knabbel",
      [adv(a) for a in REEKS], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), shape="hexagon", xp=500)

    # ---- de Wolkenweide ---------------------------------------------------------------------------------------------------------
    q("bio_wolkenhoeder_hut", "Het grote eiland", "Heel zelden zweeft er boven een &dWolkenweide&r een groot eiland met een heuvel, "
      "een meertje en een waterval over de rand. Daar staat de hut van de &dwolkenhoeder&r (Superkompas: Knus).",
      "guhs:wolkenblok_wit", [st("wolkenhoeder_hut")], rewards=((kn, 16),), shape="circle", xp=150)
    q("bio_schaapje_scheren", "Pluis!", "&dWolkenschaapjes&r zweven een handje boven het gras. Knip er eentje met een schaar: je "
      "krijgt &6wolkenpluis&r en na een poosje is hij weer net zo pluizig.", "guhs:wolkenpluis", [adv("wolkenschaapje_geschoren")],
      rewards=(("guhs:wolkenpluis", 4),), xp=50)
    q("bio_hoeder_les", "Wolkenmaker", "De wolkenhoeder leert je hoe je van wolkenpluis &6wolkenblokken&r maakt: zacht, je zakt er "
      "een beetje in en je valt er nooit hard op. Volg zijn les tot het eind.", "guhs:wolkenblok_roze", [adv("wolkenhoeder_hut_les")],
      rewards=(("guhs:wolkenpluis", 8),), shape="gear", xp=200)
    q("bio_schaapje_thuis", "Een schaapje voor thuis", "Wie zijn les af heeft, mag één wolkenschaapje van de kudde mee naar huis "
      "nemen. Het blijft bij je, als je goed voor hem zorgt.", "guhs:wolkenschaapje_spawn_egg", [adv("wolkenhoeder_hut_schaapje")],
      rewards=((kn, 12),), xp=100)
    q("bio_wolkguh", "Wolkguhs in het wild", "De &dWolkguh&r woonde alleen op de zwevende guh-eilandjes. In de Wolkenweide loopt "
      "hij nu ook gewoon in het wild rond. Kom dichtbij er een.", "guhs:guhdex", [adv("seen_wolk")], rewards=((kn, 12),),
      shape="rsquare", xp=50)
    q("bio_luchtballon_haven", "De haven in de lucht", "Aan de rand van een eiland ligt een steiger met luchtballonnen: de "
      "&dluchtballon-haven&r (Superkompas: Knus). De ballonvaarder-guh wacht op passagiers.", "guhs:guh_ballon",
      [st("luchtballon_haven")], rewards=((kn, 12),), shape="circle", xp=100)
    q("bio_vaart", "Zachtjes naar beneden", "Eén keer per dag neemt de ballonvaarder-guh je mee in zijn ballon: in een "
      "grote boog over de rand en zachtjes naar de weide beneden. Stap in en geniet tot je geland bent.", "guhs:guh_ballon", [adv("luchtballon_haven_vaart")], rewards=((kn, 12),), xp=100)
    q("bio_sterrenwacht_ruine", "De slapende sterrenkijker", "Op een hoog eiland staat een oude &dsterrenwacht&r, half ingestort "
      "(Superkompas: Wonderen). De sterrenkijker-guh daar slaapt overdag.", "minecraft:spyglass", [st("sterrenwacht_ruine")],
      rewards=((kn, 12),), shape="circle", xp=100)
    q("bio_sterrenstof", "Sterrenstof", "Kom 's nachts terug bij de ruïne: daar vallen meer sterren dan waar ook en de "
      "sterrenkijker is wakker. Kijk door de telescoop: elke nacht krijg je een beetje &6sterrenstof&r.", "guhs:sterrenstof", [adv("sterrenwacht_ruine_sterrenstof")], rewards=((kn, 12),), xp=100)
    q("bio_bouw_wolkenbank", "Zitten op een wolk", "Zet thuis een &6wolkenbank&r neer (maak hem van wolkenblokken en wolkenpluis, of ruil "
      "hem bij de smid-guh) en ga erop zitten. Een wolkenbed en een wolkenlamp passen er mooi bij.", "guhs:wolkenbank", [item("guhs:wolkenbank")],
      rewards=((kn, 12),), shape="diamond", xp=100)

    # ---- hoog in de wolken ------------------------------------------------------------------------------------------------------
    q("bio_regenboogbrug", "Het einde van de regenboog", "Heel zeldzaam: een echte &dregenboog&r als brug tussen twee eilanden "
      "(Superkompas: Wonderen). Je kunt eroverheen lopen.", "guhs:regenboogblok", [st("regenboogbrug")], rewards=((kn, 16),),
      shape="circle", xp=150)
    q("bio_pot", "De pot aan het eind", "Aan het einde van de regenboog staat geen pot goud, maar een pot &6kaasknabbels&r. Elke "
      "dag is hij weer vol.", "guhs:block_of_kaasknabbels", [adv("regenboogbrug_pot")], rewards=((kn, 8),), xp=50)
    q("bio_bouw_regenboog", "Je eigen regenboog", "&6Regenboogblokken&r zijn zacht en een beetje doorzichtig. Je maakt ze van wolkenpluis "
      "met rode, gele en blauwe verf. Bouw er thuis een boog mee: blokken voor de poten en trappen voor de bocht.", "guhs:regenboogblok_trap",
      [item("guhs:regenboogblok", 12), item("guhs:regenboogblok_trap", 4)], rewards=((kn, 12),), shape="diamond", xp=100)
    q("bio_bliksemsmidse", "De smid in de donderwolk", "Een donker wolkje dat zachtjes rommelt: de &dbliksemsmidse&r (Superkompas: "
      "Wonderen). Niet schrikken: flits is lief.", "guhs:wolkenlamp", [st("bliksemsmidse")], rewards=((kn, 12),), shape="circle", xp=100)
    q("bio_smid_ruil", "Tik. Tik.", "De &dsmid-guh&r perst wolken tot blokken. Breng hem wolkenpluis en ruil het voor een "
      "wolkenbank, een wolkenbed of een wolkenlamp.", "guhs:wolkenpluis", [adv("smidguh_eerste_ruil")], rewards=(("guhs:wolkenpluis", 4),), xp=100)
    q("bio_wolkenkasteeltje", "Het kasteel op de hoogste wolk", "Op de hoogste wolk staat een half ingestort &dwolkenkasteeltje&r "
      "(Superkompas: Wonderen). Binnen snurkt iets heel groots.", "guhs:wolkenblok_wit_trap", [st("wolkenkasteeltje")],
      rewards=((kn, 16),), shape="circle", xp=150)
    q("bio_sluipen", "Sssst", "In het kasteel slaapt een &dreuzenguh&r op zijn schat. Sluip erlangs, niet rennen en niet springen, "
      "en pak één gouden kruimel. Wordt hij wakker, dan niest hij je alleen maar zachtjes het kasteel uit.",
      "guhs:wolkenkasteeltje_kruimel", [adv("wolkenkasteeltje_langs_reus")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),),
      shape="gear", xp=200)
    q("bio_top", "Hoofd in de wolken", "Boven de weide hangen stapels eilandjes, steeds hoger. Klim, spring en lift naar het "
      "&dallerhoogste eilandje&r van zo'n stapel en kijk naar beneden. Wie daar gestaan heeft, mag de titel &bHoofd in de "
      "wolken&r dragen.", "guhs:wolkenblok_roze_trap", [adv(f"{N}_hoogste_eiland")], rewards=(("guhs:guh_kristal", 4),),
      shape="hexagon", xp=300)
