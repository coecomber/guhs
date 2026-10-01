"""
Builds the "Guhs" chapter group for FTB Quests: thirteen themed chapters in src/main/resources/ftbquests/
(chapters/<file>.json5, lang/nl_nl/<file>.json5 and lang/en_us/<file>.json5, index.txt for the installer), plus their pictures
(textures/ftbquests, drawn by tools/make_ftbquests_art.py). The mod copies them into config/ftbquests/quests when FTB Quests
is installed (see compat/FtbQuestsChapter.java). Bump CHAPTER_VERSION when you change the chapters, so packs get them.

Which chapter a quest lands in (so quests added later find their place by themselves):
  1. a SECTIONS entry that names the quest (keys=[...]) or its feature module (module="...");
  2. the feature module's FTB_CHAPTER = "guhs_..." (optional; FTB_SECTION = "title" names its section), or MODULE_CHAPTER;
  3. the quest key's prefix (PREFIX_CHAPTER);
  4. otherwise: De Guhmensie (quests of this file) or Minigames & bijzondere plekken (quests of a feature module).
Quests that no section names get a section of their own at the end of their chapter (one per feature module), and a
feature's quests without dependencies follow each other. Nothing is locked (1.1.3): only the stomach sizes (maag_64 ...
maag_128) get real FTB Quests dependencies ("linear"). Every other quest has none, because a dependency also holds back
completion: FTB Quests remembers the progress of a quest whose dependencies aren't done yet, but only ticks it off through a
fragile chain once they are (an un-claimed "Guh!" kept a whole Guhdex at 0). The logical order (deps) still places the
quests and puts "Komt na: ..." on a section's header; compat/FtbQuestsRepair ticks off what older worlds left stuck.

Texts (1.2.0): the Dutch below is the source (lang/nl_nl); the English (lang/en_us) comes from the overlay in
tools/lang/en/*.json, with readable keys: ftb.group.title, ftb.<chapter>.title / .sub / .section.<sid>,
ftb.<chapter>.q.<quest>.title / .desc (the description as one string, a newline between the lines), ftb.komt_na(_elders).
A key without English stays Dutch. The Dutch source of all those keys goes to tools/lang/source_ftb.json (for
tools/lang/check_en.py). The section headers are pictures with the Dutch title painted in (make_ftbquests_art).

Run from the project root:  python tools/make_ftbquests.py   (--art: redraw all pictures)
"""
import hashlib
import os

CHAPTER_VERSION = 22   # 20 = 1.1.0: JSON5 for FTB Quests 26.1; 21 = 1.1.3: no locks (only the stomach sizes); 22 = 1.2.0: English
OUT = os.path.join("src", "main", "resources", "ftbquests")


class Long(int):
    """Written with an L (FTB Quests uses longs for task counts and kill values)."""


def qid(key):
    """Stable 16-hex ids; all of ours start with 475548 ("GUH" in hex)."""
    return "475548" + hashlib.md5(key.encode()).hexdigest()[:10].upper()


def item(i, count=1):
    return {"type": "item", "item": i, "count": count}


def adv(a):
    return {"type": "advancement", "advancement": a if ":" in a else f"guhs:quest/{a}"}


def structure(s):
    return {"type": "structure", "structure": f"guhs:{s}"}


def biome(b):
    return {"type": "biome", "biome": f"guhs:{b}"}


def dim(d):
    return {"type": "dimension", "dimension": d}


def kill(e, n=1):
    return {"type": "kill", "entity": e, "value": n}


QUESTS = []  # (key, title, description, icon, [tasks], [rewards], [deps], x, y, shape)


def q(key, title, desc, icon, tasks, rewards=(("guhs:kaas_knabbels", 8),), deps=(), x=0.0, y=0.0, shape=None, xp=0):
    QUESTS.append((key, title, desc, icon, tasks, rewards, deps, x, y, shape, xp))


# --- begin ------------------------------------------------------------------------------------------------------------
q("start", "Guh!", "Welkom bij de &dGuhs&r! Mollige roze knuffelmuisjes die overal rondlopen. Alles begint met &6kaasknabbels&r: daarmee tem, genees en fok je guhs.",
  "guhs:kaas_knabbels", [item("guhs:kaas_knabbels")], x=0, y=0, shape="gear")
q("tame", "Een vadsig vriendje", "Voer een wilde guh kaasknabbels tot hij tam is (1 op 3 kans per knabbel). Een tamme guh krijgt 1000 levens!",
  "guhs:guh_spawn_egg", [adv("guhs:quest/tamed_normal")], deps=["start"], x=2, y=0)
q("ride", "Hop hop, guh!", "Een grote guh (minstens ~1,7 blok) met een zadel kun je berijden.", "minecraft:saddle",
  [adv("guhs:guhmension/ride_guh")], deps=["tame"], x=4, y=0)
q("launch", "VAHOEG, lanceren!", "Rijd op je eigen guh met een lege hand en rechtsklik: hij zuigt lucht naar binnen en... schiet weg! Rechtsklik nog eens om de afstand vast te zetten.",
  "minecraft:wind_charge", [adv("launched")], deps=["ride"], x=6, y=0)
q("wardrobe", "Aankleden!", "Een guh heeft vijf kledingvakjes, een pantservakje en (met rugzak) 18 extra vakjes. Koop de rugzak bij de kleermaker of maak hem zelf.",
  "guhs:guh_backpack", [item("guhs:guh_backpack")], deps=["tame"], x=2, y=2)
q("portal_block", "Blok kaasknabbels", "Negen kaasknabbels in een blok. Bouw er een portaalframe van (zoals een netherportaal).",
  "guhs:block_of_kaasknabbels", [item("guhs:block_of_kaasknabbels", 10)], deps=["start"], x=0, y=2)
q("guhmension", "Welkom in de Guhmensie!", "Stap door je guhportaal: een wereld van roze wol, kaassaus en heel veel guhs.",
  "guhs:block_of_kaasknabbels", [dim("guhs:guhmension")], rewards=(("guhs:guhdex", 1),), deps=["portal_block"], x=0, y=4, shape="octagon", xp=100)
q("frying", "Frituurvads", "Vul de guh-koekenpan met Mika's vet en frituur kaasknabbels. Gefrituurde kaasknabbels maken een tamme guh meteen helemaal beter.",
  "guhs:frying_pan", [item("guhs:gefrituurde_kaasknabbels", 4)], deps=["start"], x=-2, y=0)

# --- de Guhmensie: biomen ----------------------------------------------------------------------------------------------
BIOMES = [("guh_fields", "Guhvelden"), ("knabbel_crumbs", "Knabbelkruimels"), ("pink_puffs", "Roze pluisjes"), ("kaas_flats", "Kaasvlakte"),
          ("guh_meadows", "Guhweides"), ("guh_peaks", "Guhpieken"), ("vads_cliffs", "Vadskliffen"), ("mikas_biome", "Mika's bioom"),
          ("guh_sea", "Guhzee"), ("guh_kristalmijn", "Guhkristalmijn")]
for i, (b, name) in enumerate(BIOMES):
    q(f"biome_{b}", name, f"Bezoek het bioom &d{name}&r in de Guhmensie.", "minecraft:filled_map", [biome(b)],
      rewards=(("guhs:kaas_knabbels", 4),), deps=["guhmension"], x=-6 + i * 1.5, y=6, shape="circle")
q("pink_moon", "Roze maan", "Blijf een nacht in de Guhmensie en kijk omhoog: een roze maan en roze sterren!", "minecraft:clock",
  [dim("guhs:guhmension")], deps=["guhmension"], x=2, y=4, shape="circle")

# --- guhsoorten: een quest per soort ------------------------------------------------------------------------------------
VARIANTS = [("normal", "Gewone guh"), ("mint", "Muntguh"), ("choco", "Chocoguh"), ("snow", "Sneeuwguh"), ("brontosaurus", "Brontosaurusguh"),
            ("teckel", "Teckelguh"), ("ghost", "Spookguh"), ("starry", "Sterrenguh"), ("rainbow", "Regenboogguh"), ("ender", "Enderguh"), ("koning", "Koningguh"), ("golden", "Gouden Guh"),
            ("brococolief", "Brococolief"), ("reisguh", "Reisguh"), ("poortwachter", "Poortwachter")]
for i, (v, name) in enumerate(VARIANTS):
    q(f"variant_{v}", name, f"Kom dichtbij een &d{name}&r (binnen 3 blokjes) zodat hij in je Guhdex komt." +
      (" Heel zeldzaam!" if v in ("golden", "rainbow", "starry") else "") + (" Alleen 's nachts!" if v == "ghost" else "") +
      (" Een geheimzinnige guh met een briefje..." if v == "brococolief" else "") +
      (" Vliegt rond op de Guhpieken!" if v == "ender" else "") +
      (" Zit op zijn troon in het legendarische guhkasteel." if v == "koning" else ""),
      "guhs:guhdex", [adv(f"seen_{v}")], rewards=(("guhs:kaas_knabbels", 16),), deps=["guhmension"], x=-7 + i * 1.5, y=9,
      shape="rsquare")
q("guhdex_full", "Guhdex vol!", "Alle guhsoorten gezien. Echte vahoege guhkenner!", "guhs:guh_kristal_verrekijker",
  [adv(f"seen_{v}") for v, _ in VARIANTS], rewards=(("guhs:guh_kristal", 16),), deps=[f"variant_{v}" for v, _ in VARIANTS],
  x=0, y=10.5, shape="gear", xp=500)

# --- wezens ----------------------------------------------------------------------------------------------------------------
q("bee", "Zoemzoem guh", "Vind een &dguhbij&r (Guhmensie of bloemrijke overworld). Ze worden nooit boos!", "guhs:guh_bee_spawn_egg",
  [adv("found_guh_bee")], deps=["guhmension"], x=6, y=4)
q("korf", "Knabbelkorf", "Maak een knabbelkorf. Is hij vol: schaar = kaasknabbels, glazen fles = kaashoning.", "guhs:knabbelkorf",
  [item("guhs:kaashoning")], deps=["bee"], x=8, y=4)
q("slime", "Blubberguh", "Vind een roze &dguhslijm&r. Ze doen niemand pijn.", "guhs:guh_slime_spawn_egg", [adv("found_guh_slime")],
  deps=["guhmension"], x=6, y=5.5)
q("slimeblock", "Plakvads", "Negen guhslijmballen worden een roze guhslijmblok: verend en plakkerig.", "guhs:roze_slijmblok",
  [item("guhs:roze_slijmblok")], deps=["slime"], x=8, y=5.5)
q("fish", "Guhvis", "Vang een guhvis in de guhzee (emmer of hengel) en bak hem.", "guhs:guh_vis_bucket",
  [adv("found_guh_vis"), item("guhs:gebakken_guh_vis")], deps=["biome_guh_sea"], x=6, y=7)
q("mika", "NJEG!", "Kom een &cMika&r tegen: de boze guh. Hij duwt je weg, maar doet geen pijn.", "guhs:mika_spawn_egg",
  [adv("found_mika")], deps=["guhmension"], x=-4, y=4)
q("mepper", "Mika-mepper", "Koop de Mika-mepper bij de Mika-jager: vijf keer zo hard tegen Mika's, en hij gaat nooit kapot.", "guhs:mika_mepper",
  [item("guhs:mika_mepper")], deps=["mika"], x=-6, y=4)
q("big_mika", "Hoe groter de Mika...", "Versla &cGrote Mika&r in een uitdagende guhgrot.", "guhs:guhmensie_superkompas",
  [adv("guhs:guhmension/defeat_big_mika")], rewards=(("guhs:vahoege_vads_ingot", 4),), deps=["mepper"], x=-8, y=4, shape="hexagon", xp=300)
q("nether_mika", "Hete Mika", "Vind een &cNether-Mika&r in de Nether: helemaal vuurbestendig.", "guhs:nether_mika_spawn_egg",
  [adv("found_nether_mika")], deps=["mika"], x=-4, y=2.5)

# --- de guhmaag -----------------------------------------------------------------------------------------------------------
q("heiligdom", "Het Vadsig-heiligdom", "Ergens in de Guhmensie woont &dMoeder Vadsig&r. Het heiligdomkompas (vadstemmer) wijst de weg.",
  "guhs:heiligdom_kompas", [structure("vadsig_heiligdom")], deps=["guhmension"], x=0, y=13, shape="octagon")
q("vadsig_met", "Moeder Vadsig", "Praat met Moeder Vadsig. Haar kleine Guhbert is ontvoerd door de Mika's!", "guhs:mika_spoorkompas",
  [adv("vadsig_met")], deps=["heiligdom"], x=2, y=13)
q("guhbert", "Bevrijd Guhbert", "Volg het Mika-spoorkompas naar het Mika-kamp en win steen-papier-schaar van de Mika-baas. Drie keer op rij...",
  "minecraft:iron_bars", [structure("mika_kamp"), adv("guhbert_free")], deps=["vadsig_met"], x=4, y=13)
q("cake", "De verloren taart", "Volg de taartkruimels naar een guh-picknick en vind de taart. Vergeet de 3 guh-ballonnen niet!",
  "guhs:verloren_guh_taart", [adv("cake_found"), item("guhs:guh_ballon", 3)], deps=["guhbert"], x=6, y=13)
q("maag", "NJEG... HAP!", "Het feest bij Moeder Vadsig: ze slikt je in! Welkom in je eigen guhmaag, in haar buik. Buikfluitje of G om te reizen.",
  "guhs:guh_buikfluitje", [adv("maag_unlocked")], rewards=(("guhs:guh_ballon", 3),), deps=["cake"], x=8, y=13, shape="gear", xp=500)
for i, (size, title, desc) in enumerate([
        (64, "Maagkrampjes", "Help de Tandarts-guh in haar mond: 64 kaasknabbels, 16 vadsstaven en 8 guhkristallen."),
        (80, "De Mika-geur", "Versla een Grote Mika en breng 16 guhkristallen naar de tandarts."),
        (96, "Gouden visite", "Neem een tamme Gouden Guh mee naar de tandarts, met 32 guhkristallen."),
        (112, "Zachte maagwand", "32 guhslijmballen en 8 kaashoning maken haar maagwand zacht."),
        (128, "Een tuin in haar buik", "16 guhbloesemboompjes, 48 kristallen en een volle Guhdex: de allergrootste maag!")]):
    q(f"maag_{size}", f"{title} ({size}x{size})", desc, "guhs:maagwand", [adv(f"maag_{size}")],
      rewards=(("guhs:guh_kristal", 8),), deps=["maag" if i == 0 else f"maag_{size - 16}"], x=10 + i * 1.5, y=13, xp=200)

# --- de guh-slee --------------------------------------------------------------------------------------------------------
q("sleehut", "De sleehut", "Op de Guhpieken staat een besneeuwd hutje met guhbloesembomen. De Slee-guh is verdrietig...", "guhs:guh_slee",
  [structure("sleehut")], deps=["biome_guh_peaks"], x=-2, y=15)
q("sled", "De kapotte slee", "Breng de Slee-guh een sleeglijder (guhgrotten), een guh-belletje (guhdorpen) en een roze lint (kleermaker).",
  "guhs:sleeglijder", [adv("sled_repaired")], rewards=(("guhs:sleerail_recht", 8), ("guhs:sleerail_bocht", 4)), deps=["sleehut"], x=0, y=15)
q("ride_sled", "WIEEE!", "Bouw een baan en rijd met je guhslee (vier guhs trekken hem!).", "guhs:sleerail_bocht", [adv("ride_sled")],
  deps=["sled"], x=2, y=15, xp=200)

q("ender_tame", "Drakenvriendje", "Tem een &5Enderguh&r (Guhpieken): 1 op 8 kaasknabbels, of in een keer met een gefrituurde kaasknabbel.",
  "minecraft:dragon_egg", [adv("guhs:quest/tamed_ender")], rewards=(("minecraft:saddle", 1),), deps=["variant_ender"], x=10, y=10.5, xp=300)
q("ender_ride", "Vadsvlucht", "Zadel je Enderguh en vlieg! Kijk waar je heen wilt, W = vooruit, spatie = omhoog.", "minecraft:elytra",
  [adv("guhs:quest/ride_ender")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), deps=["ender_tame"], x=11.5, y=10.5, shape="gear", xp=300)

# --- Reisguhs (2.3.0) -----------------------------------------------------------------------------------------------------
q("superkompas", "Het superkompas", "Maak (of koop bij de vadstemmer) een &eGuhmensie-superkompas&r. Rechtsklik: kies wat je zoekt, per categorie. Het wijst naar de dichtstbijzijnde.",
  "guhs:guhmensie_superkompas", [item("guhs:guhmensie_superkompas")], deps=["guhmension"], x=9.5, y=22.5)
q("reisguhs", "Tuut tuut!", "Bij elk guhportaal in de Guhmensie (en bij het guhkasteel) zit een blauwe &bReisguh&r. Ook in de grote plekken woont er een: Guhwarden bij de Elf-Guhjestocht, het Knuffeldal-stadje, de Guhkermis, Guhland, het Ballonfestival en het Guhcircuit. Ontdek er 3 door ze te rechtsklikken; daarna reis je in een klik tussen je ontdekte Reisguhs. Met een Reisguh-fluitje zet je er zelf een neer.",
  "guhs:reisguh_fluitje", [adv("reisguhs")], rewards=(("guhs:reisguh_fluitje", 2),), deps=["guhmension"], x=8, y=22.5, xp=200)

# --- het guhkasteel (2.3.0) ---------------------------------------------------------------------------------------------
q("kasteel", "Lang leve de Koning!", "Het legendarische &dguhkasteel&r: 256x256, met een guhhoofd op de toren. Het &5Guhmensie-superkompas&r (kies: Wonderen > Guhkasteel) wijst de weg; het kan heel ver zijn!",
  "guhs:guhmensie_superkompas", [structure("guh_kasteel")], rewards=(("guhs:gefrituurde_kaasknabbels", 16),), deps=["guhmension"], x=8, y=19.5, shape="gear", xp=500)
q("guhvriend", "Guhvriend", "De poortwachters van het guhkasteel laten alleen guhvrienden binnen. Bewijs het: ga lekker vadsig op het bankje zitten, of zeg het geheime guhwoord in de chat...",
  "guhs:guh_bank", [adv("guhvriend")], rewards=(("guhs:kaas_knabbels", 32),), deps=["kasteel"], x=8, y=21, xp=200)
q("koning_tem", "Vahoege Majesteit", "Tem de Koningguh op zijn troon met kaasknabbels en neem zijn koningspakje mee (kroon, mantel, medaillon). Na een paar dagen komt er een nieuwe koning!",
  "guhs:koning_kroon", [adv("guhs:guhmension/koning_pakje")], rewards=(("minecraft:diamond", 5),), deps=["kasteel"], x=9.5, y=19.5, xp=500)
q("kasteel_schat", "De schatkamer", "Vind de schatkamer achter de troon (via de galerij) en neem de &6koninklijke guhtroon&r mee naar huis.",
  "guhs:koningstroon", [item("guhs:koningstroon")], rewards=(("guhs:vahoege_vads_ingot", 4),), deps=["kasteel"], x=11, y=19.5, xp=300)

# --- verstopguh (2.2.0) --------------------------------------------------------------------------------------------------
q("verstop_huis", "Wie verstopt zich daar?", "Zoek het zeldzame &dverstopguhhuis&r (superkompas: Minigames > Verstopguhhuis). Op het glazen dak woont Verstopguhtje.",
  "guhs:guhmensie_superkompas", [structure("verstopguh_huis")], deps=["guhmension"], x=8, y=18)
for i, (lvl, name, n) in enumerate((("makkelijk", "Makkelijk", 5), ("medium", "Medium", 8), ("moeilijk", "Moeilijk", 12))):
    q(f"verstop_{lvl}", f"Verstopguh: {name}", f"Vind alle {n} verstopte guhs op {name.lower()}. Rechtermuisklik op een guh als je hem vindt; luister naar hun zachte geluidjes!",
      "guhs:verstopguhticket", [adv(f"verstop_{lvl}")], rewards=(("guhs:kaas_knabbels", 16 * (i + 1)),), deps=["verstop_huis"],
      x=9.5 + i * 1.5, y=18, xp=100 * (i + 1), shape="gear" if lvl == "moeilijk" else None)
q("verstop_detective", "Guhlock Holmes", "Koop de Guhlock-pet, het Vergroot-vadsglas en de detectivejas bij Verstopguhtje (verstopguhtickets).",
  "guhs:detective_pet", [adv("guhs:guhmension/verstop_detective")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), deps=["verstop_huis"],
  x=14, y=18, xp=300)

# --- de guhkermis (2.1.0) ------------------------------------------------------------------------------------------------
q("kermis", "Kermis!", "Ergens in de Guhmensie staat de zeldzame &dGuhkermis&r met een echte achtbaan. Het &6Guhmensie-superkompas&r (Minigames > Guhkermis) wijst de weg.",
  "guhs:guhmensie_superkompas", [structure("guh_kermis")], deps=["guhmension"], x=4, y=15)
q("kermis_rit", "WIEEEEE!", "Stap in een slee op het station, klik op de knopjes en start. Elk rondje over de finish = een &6kermisbon&r. Je eerste rondje geeft een prijzenzakje!",
  "guhs:kermisbon", [adv("guhs:guhmension/kermis_rit")], rewards=(("guhs:kaas_knabbels", 16),), deps=["kermis"], x=6, y=15, xp=200)
q("kermis_bonnen", "Bonnenvadser", "Spaar 12 kermisbonnen (rondjes rijden!).", "guhs:kermisbon", [item("guhs:kermisbon", 12)],
  rewards=(("guhs:guh_ballon", 3),), deps=["kermis"], x=6, y=16.5)
q("kermis_outfit", "Vahoeg op de kermis", "Koop bij de Kermis-guh de kermishoed, het kermisjasje en de kermisstrik voor je guh.",
  "guhs:kermis_hoed", [adv("guhs:guhmension/kermis_outfit")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), deps=["kermis"],
  x=8, y=15, shape="gear", xp=300)
q("coaster", "Eigen achtbaan", "Maak een vadsdrop, een guhkurkentrekker en een vahoegschans (sleebouwersboek) en bouw je eigen achtbaan.",
  "guhs:sleerail_kurkentrekker", [item("guhs:sleerail_drop"), item("guhs:sleerail_kurkentrekker"), item("guhs:sleerail_schans")],
  deps=["sled"], x=2, y=16.5, shape="diamond")

# --- bouwwerken ------------------------------------------------------------------------------------------------------------
STRUCTS = [("hamster_house", "Hamsterhuis"), ("hamster_house_medium", "Groot hamsterhuis"), ("hamster_house_large", "Hamsterstad"),
           ("hamster_house_extra_extra_large", "Guhland"), ("evil_mika_home", "Evil Mika-huis"), ("guh_picnic", "Guh-picknick"),
           ("guh_caves", "Guhgrotten"), ("challenging_guh_caves", "Uitdagende guhgrotten"), ("cheese_fountain", "Kaasfontein"),
           ("grand_cheese_fountain", "Grote kaasfontein"), ("guh_statue", "Guhstandbeeld"), ("guhramid", "Guhramide"),
           ("guh_village", "Guhdorp"), ("giant_cake", "Reuzentaart"), ("giant_kaasknabbel", "Reuzenkaasknabbel"),
           ("kaasknabbel_arch", "Kaasknabbelboog"), ("guh_fossil", "Guhfossiel"), ("block_guh", "Blokguh"),
           ("mini_picnic", "Minipicknick"), ("quartz_statue", "Kwartsbeeldje")]
for i, (s, name) in enumerate(STRUCTS):
    q(f"struct_{s}", name, f"Vind een &d{name}&r." + (" Heel heel zeldzaam!" if s in ("hamster_house_extra_extra_large", "guhramid") else ""),
      "guhs:guhmensie_superkompas" if "caves" in s else "minecraft:map", [structure(s)],
      rewards=(("guhs:kaas_knabbels", 12),) if i % 3 else (("guhs:gefrituurde_kaasknabbels", 4),), deps=["guhmension"],
      x=-7 + (i % 10) * 1.5, y=17 + (i // 10) * 1.5, shape="square")

# --- dorpen, eten, deco, vads, redstone ---------------------------------------------------------------------------------------
EXTRAS = [
    ("knabbelboer", "Knabbelboer", "Zet een vadszaadbak neer in een guhdorp en plant kaasknabbelzaadjes op akkergrond.", "guhs:zaadbak",
     [item("guhs:kaasknabbelzaadjes", 8)]),
    ("taart", "Guhtaart", "Een roze taart met guhogen, 7 happen.", "guhs:guh_taart", [item("guhs:guh_taart")]),
    ("sweets", "Guhcarons", "Alle vier de guhcarons!", "guhs:macaron_roze",
     [item("guhs:macaron_roze"), item("guhs:macaron_mint"), item("guhs:macaron_citroen"), item("guhs:macaron_choco")]),
    ("fondue", "Kaasfondguh", "Een warme kom kaasfondguh.", "guhs:kaasfondue", [item("guhs:kaasfondue")]),
    ("shake", "Vahoege kaasknabbelshake", "Haalt al je effecten weg, net als melk.", "guhs:kaasknabbel_milkshake", [item("guhs:kaasknabbel_milkshake")]),
    ("furniture", "Guhmeubels", "Een guhstoel en een vadszetel: ga lekker zitten.", "guhs:guh_stoel", [item("guhs:guh_stoel"), item("guhs:guh_bank")]),
    ("vadszak", "Vadszak", "Een vadszak in je lievelingskleur.", "guhs:pink_zitzak", [item("guhs:pink_zitzak")]),
    ("lampgion", "Lampgion", "Licht in de guhtuin.", "guhs:lampion_roze", [item("guhs:lampion_roze")]),
    ("bloesem", "Guhbloesem", "Vind een guhbloesemboom en pak een boompje.", "guhs:guhbloesem_sapling", [item("guhs:guhbloesem_sapling")]),
    ("ze_hangen", "Ze hangen aan me veh", "Bij de guh-picknick speelt een jukebox een guhplaat. Pak hem mee (rechtsklik op de jukebox)!",
     "guhs:music_disc_ze_hangen", [item("guhs:music_disc_ze_hangen")]),
    ("crystal", "Guhkristal", "Graaf in de Guhmensie naar een kristalmijn (y30-60).", "guhs:guh_kristal", [item("guhs:guh_kristal", 4)]),
    ("spyglass", "Guhkristalverrekijker", "Laat zeldzame guhs oplichten, zelfs door muren.", "guhs:guh_kristal_verrekijker",
     [item("guhs:guh_kristal_verrekijker")]),
    ("vads", "Super vahoege!", "Hak samengeperst super vahoege vads (ijzeren houweel) en smelt het.", "guhs:vahoege_vads_ingot",
     [item("guhs:vahoege_vads_ingot")]),
    ("paxel", "Vadspaxel", "Houweel, bijl en schep in een, en hij gaat nooit kapot.", "guhs:vahoege_vads_paxel", [item("guhs:vahoege_vads_paxel")]),
    ("wheel", "Guhrad", "Zet je tamme guh in een guhrad: volle redstonestroom!", "guhs:guh_wheel", [item("guhs:guh_wheel")]),
    ("bank", "Bankguh", "Voer de Hongerige Guh 10 gefrituurde kaasknabbels.", "guhs:bank_guh", [adv("guhs:guhmension/feed_hungry_guh")]),
]
for i, (key, title, desc, icon, tasks) in enumerate(EXTRAS):
    q(key, title, desc, icon, tasks, deps=["guhmension"] if key not in ("wheel", "furniture", "taart") else ["start"],
      x=-7 + (i % 8) * 2, y=21 + (i // 8) * 1.5, shape="diamond")


# --- the 2.4+ features add their own quests (tools/features/*.py) -----------------------------------------------------------
import json  # noqa: E402
import math  # noqa: E402
import re  # noqa: E402
import sys  # noqa: E402
import features as _features  # noqa: E402

SOURCE = {x[0]: "core" for x in QUESTS}  # quest key -> the feature module that added it ("core": this file)
MODULES = {}
for _name, _module in zip(_features.FEATURES, _features.modules()):
    MODULES[_name] = _module
    if hasattr(_module, "ftb"):
        _n0 = len(QUESTS)
        _module.ftb(sys.modules[__name__])
        for _quest in QUESTS[_n0:]:
            SOURCE[_quest[0]] = _name


# --- the chapters ------------------------------------------------------------------------------------------------------------
GROUP_TITLE = "&dGuhs"
READING = ("&dGuhs & basis&r > &dDe Guhmensie&r > &6Minigames & bijzondere plekken&r > &9Onderwater&r > &cDe Guhmaag&r > "
           "&5Het Guheinde&r > &6De Guhbarbecuether&r > &eGrotten, moeras & woud&r > &dKnuffeldal&r > &dPiep!&r > &dLieve vadsjes&r"
           " > &dGuhverhalen&r > &aDiertjes&r")
NIKS_OP_SLOT = ("Niks zit op slot: elke quest vinkt zichzelf af zodra je hem gedaan hebt, ook als je dat al eerder deed. "
                "De kopjes laten zien wat logisch na elkaar komt. Vink dit af en ga lekker vadsig aan de slag!")
# file: title, subtitle, ribbon colour, icon, banner renders (left, right), welcome picture (renders, structure behind them),
#       links (the quests in other chapters this chapter comes after), the "Hoe kom je hier?" text (one paragraph per line)
CHAPTERS = {
    "guhs_basis": dict(
        title="&dGuhs & basis", sub="Tem, kleed en knuffel je guh. VAHOEG!", colour="pink", icon="guh:normal",
        banner=("guh:mint", "guh:choco"), welcome=(["guh:normal", "guh:snow", "guh:golden", "guh:teckel"], None), links=[],
        intro=["Je bent er al! Dit is het allereerste hoofdstuk: hier begint elk guh-avontuur, met een handje &6kaasknabbels&r.",
               "Lees de hoofdstukken van boven naar beneden: " + READING + ".",
               "Bovenaan elk hoofdstuk staat zo'n &dHoe kom je hier?&r-quest, met een linkje naar de quest in een ander hoofdstuk waar het begint.",
               NIKS_OP_SLOT + " (Alleen de maaggroottes in De Guhmaag komen echt na elkaar.)"]),
    "guhs_guhmensie": dict(
        title="&dDe Guhmensie", sub="Roze wol, kaassaus en heel veel guhs", colour="magenta", icon="npc:reisguh",
        banner=("npc:reisguh", "geo:guh_bee:guh_bee"), welcome=(["npc:reisguh", "guh:starry", "geo:mika:mika"], "wiki:guh_portal"),
        links=["start"],
        intro=["Dit hoofdstuk komt na &dGuh!&r in &dGuhs & basis&r: zodra je kaasknabbels hebt, maak je er een &6blok kaasknabbels&r van.",
               "Bouw daarmee een portaalframe (zoals een netherportaal) en stap erdoor: welkom in de &dGuhmensie&r, de roze wereld "
               "waar bijna alle andere hoofdstukken beginnen.",
               "Daarna wijst het &6Guhmensie-superkompas&r je de weg naar (bijna) alles. " + NIKS_OP_SLOT]),
    "guhs_minigames": dict(
        title="&6Minigames & bijzondere plekken", sub="Spelen, winnen en vadsig verkleden", colour="gold", icon="npc:djguh",
        banner=("npc:djguh", "npc:raceguh"), welcome=(["npc:showguh", "npc:mepguh", "npc:golfguh", "npc:visguh"], "wiki:structure_guh_disco"),
        links=["superkompas"],
        intro=["Dit hoofdstuk komt na &6Het superkompas&r in &dDe Guhmensie&r.",
               "Alle minigamegebouwen en de zeldzame plekken (verstophuis, bibliotheek, kaasmijn, zwevende eilandjes, kermis...) "
               "vind je met het superkompas (Minigames of Wonderen). Elk rijtje hieronder begint bij zo'n gebouw.",
               "Speel ze in elke volgorde die je wilt. Njeg, wat een keuze! " + NIKS_OP_SLOT]),
    "guhs_onderwater": dict(
        title="&9Onderwater", sub="Blub blub guh: de Diepe Guhzee", colour="blue", icon="npc:zeemeerguh",
        banner=("npc:zeemeerguh", "geo:guh_vis:guh_vis"), welcome=(["npc:zeemeerguh", "geo:guh_vis:guh_vis"], "wiki:structure_onderwater"),
        links=["superkompas"],
        intro=["Dit hoofdstuk komt na &6Het superkompas&r in &dDe Guhmensie&r (categorie Wonderen).",
               "Ergens in de Guhmensie ligt de &9Diepe Guhzee&r, en in het midden daarvan, op de bodem: de &bGuhbubbel&r. "
               "Loop de Duikpost binnen door zijn mond en neem de wenteltrap naar beneden.",
               "Vergeet niet af en toe belletjes te happen! " + NIKS_OP_SLOT]),
    "guhs_maag": dict(
        title="&cDe Guhmaag", sub="NJEG... HAP! Welkom in de buik", colour="red", icon="npc:moeder_vadsig",
        banner=("npc:moeder_vadsig", "npc:tandarts"), welcome=(["npc:moeder_vadsig", "guh:normal", "npc:tandarts"], "wiki:structure_vadsig_heiligdom"),
        links=["guhmension"],
        intro=["Dit hoofdstuk komt na &dWelkom in de Guhmensie!&r in &dDe Guhmensie&r.",
               "Zoek daar het &dVadsig-heiligdom&r (het heiligdomkompas wijst de weg) en help Moeder Vadsig haar kleine Guhbert terug te vinden. "
               "Als dank slikt ze je in: HAP!",
               "Dit is het enige hoofdstuk met een slotje: de maaggroottes (64 tot 128) komen echt na elkaar. Eerst smullen, dan groeien!"]),
    "guhs_guheinde": dict(
        title="&5Het Guheinde", sub="Opper-Mika heeft alle knabbels, njeg!", colour="purple", icon="guh:ender",
        banner=("guh:vahoege_ender", "geo:mika:opper_mika"), welcome=(["guh:koning", "guh:ender", "geo:mika:mika"], "wiki:structure_guheinde_knabbelberg"),
        links=["big_mika", "kasteel"],
        intro=["Dit hoofdstuk komt na twee quests in &dDe Guhmensie&r:",
               "- &cHoe groter de Mika...&r: Mika's (vooral Grote Mika) huilen &5Mika-tranen&r. Die heb je nodig voor de Ogen van Vadsig.",
               "- &dLang leve de Koning!&r: de Koningguh in het guhkasteel vertelt je waar alle kaasknabbels gebleven zijn.",
               "Daarna: twaalf Ogen van Vadsig, de Knabbelkelder en... Opper-Mika. Zonder knabbels is je guh niet VAHOEG, dus haal ze terug! "
               + NIKS_OP_SLOT]),
    "guhs_barbecuether": dict(
        title="&6De Guhbarbecuether", sub="Heet, heter, njeg! Help de Grillguh", colour="orange", icon="npc:grillguh",
        banner=("npc:grillguh", "wiki:rookguh"), welcome=(["npc:grillguh", "guh:asguh", "wiki:rookguh"], "wiki:structure_barbecueput_groot"),
        links=["guhmension"],
        intro=["Dit hoofdstuk komt na &dWelkom in de Guhmensie!&r in &dDe Guhmensie&r.",
               "Daar liggen kapotte barbecues: de &6barbecueputten&r (superkompas: Barbecue > Barbecueput). In de grote put zit de &dGrillguh&r: "
               "de Mika's hebben zijn Aanmaakblokjes gejat!",
               "Maak zijn barbecue weer heel, steek dan zelf een grillkoolframe aan en stap de &6Guhbarbecuether&r in. Daar staan de Spiesburchten. "
               + NIKS_OP_SLOT]),
    "guhs_extra27": dict(
        title="&eGrotten, moeras & woud", sub="Gatenkaas, moeras, woud en guhfamilies", colour="lime", icon="geo:kikkerguh:kikkerguh_mint",
        banner=("geo:kikkerguh:kikkerguh_roze", "npc:boswachterguh"),
        welcome=(["geo:kikkerguh:kikkerguh_geel", "npc:boswachterguh", "guh:kaasmoerasguh", "geo:vadswaker:vadswaker"], "wiki:structure_boomhutdorp"),
        links=["guhmension"],
        intro=["Dit hoofdstuk komt na &dWelkom in de Guhmensie!&r in &dDe Guhmensie&r.",
               "Diep onder de Guhmensie liggen de &eGatenkaasgrotten&r. Ergens anders hangt gele mist boven het &eKaasmoeras&r, en mintgroene mist "
               "in het &aVadswoud&r, waar de guhfamilies wonen.",
               NIKS_OP_SLOT]),
    "guhs_knuffeldal": dict(
        title="&dKnuffeldal", sub="Het Grote Knusfeest komt eraan!", colour="peach", icon="npc:cocotje",
        banner=("npc:burgemeesterguh", "npc:cocotje"),
        welcome=(["npc:bakkerguh", "npc:burgemeesterguh", "npc:cocotje", "npc:juf_knuffel", "npc:theeguh"], None),
        links=["superkompas"],
        intro=["Dit hoofdstuk komt na &6Het superkompas&r in &dDe Guhmensie&r: kies de categorie &dKnus&r en hij wijst je naar het &dKnuffeldal&r.",
               "Help Burgemeester Vadsema met het Grote Knusfeest. Elk gebouw aan het plein (en daarbuiten) heeft hieronder zijn eigen rijtje. "
               "En zoek &dCocotje&r eens op!",
               "Alles is knus. " + NIKS_OP_SLOT]),
    "guhs_piep": dict(
        title="&dPiep!", sub="Pieppiepmuisjes en zielige knabbels", colour="pink", icon="geo:pieppiepmuisje:pieppiepmuisje",
        banner=("geo:pieppiepmuisje:pieppiepmuisje", "geo:boze_kaasknabbel:boze_kaasknabbel"),
        welcome=(["geo:pieppiepmuisje:pieppiepmuisje", "geo:boze_kaasknabbel:boze_kaasknabbel"], None),
        links=["superkompas"],
        intro=["Dit hoofdstuk komt na &6Het superkompas&r in &dDe Guhmensie&r.",
               "In de lieve guhhuizen van de Guhmensie wonen piepkleine &5pieppiepmuisjes&r, en ergens staat een kaaskorstheuvel vol "
               "&6boze kaasknabbels&r (superkompas: Avontuur). &2Schilly&r en &2Poepschilly&r vind je bij &9Onderwater&r!",
               "Piep piep! " + NIKS_OP_SLOT]),
    # 2.10 (Lieve vadsjes van elkaar): hearts, huisjes, chores, toys, the Guhkamer, together and favourites
    "guhs_band": dict(
        title="&dLieve vadsjes van elkaar", sub="Hartjes, huisjes, klusjes en favorietjes. Njeg!", colour="pink", icon="guh:normal",
        banner=("guh:mint", "guh:golden"),
        welcome=(["guh:normal", "guh:mint", "geo:pieppiepmuisje:pieppiepmuisje", "guh:golden"], None),
        links=["tame"],
        intro=["Dit hoofdstuk komt na &dEen vadsig vriendje&r in &dGuhs & basis&r: zodra je een guh getemd hebt, begint jullie "
               "&dhartjesmeter&r te lopen.",
               "Geef je guh snacks, aai hem (kort rechtsklikken) en geef hem een dikke &dKnuffel&r (houd rechtsklik ingedrukt: het menu). "
               "Zo worden jullie &dlieve vadsjes van elkaar&r, dan &dmega lieve vadsjes van elkaar&r en uiteindelijk "
               "&6zielsguh bff 5evr <3&r. Hartjes gaan nooit omlaag!",
               "Bouw een &dGuhhuisje&r (een huisje in de vorm van een guhhoofd): daar wonen je guhs en maatjes, slapen ze 's nachts "
               "en doen ze overdag klusjes. In je &dGuhdex&r (tab Mijn guhs) staat het dagboekje van elke guh.",
               NIKS_OP_SLOT]),
    # 3.0 (Guhverhalen): the stories, and the little animals of the Guhmensie
    "guhs_verhalen": dict(
        title="&dGuhverhalen", sub="Baltoguh, Guhtwo, 626-guh, het Knuffelhart en de Timmerguh. Njeg!", colour="pink", icon="guh:baltoguh",
        banner=("guh:mewtwo", "guh:stitch626"),
        welcome=(["guh:baltoguh", "npc:timmerguh", "guh:mewtwo", "guh:stitch626"], None),
        links=["superkompas"],
        intro=["Dit hoofdstuk komt na &6Het superkompas&r in &dDe Guhmensie&r: kies daar de nieuwe tab &dVerhalen&r.",
               "Daar vind je de plekken van de grote guhverhalen: &fNomguh&r in de witte Sneeuwguhtoendra (Baltoguh!), het "
               "&5kloon-eiland&r in de Diepe Guhzee (Guhtwo!), het &dHemelkapelletje&r hoog in de wolken, en de eilandjes van "
               "&bGuhwai'i&r (626-guh!). En in elk Knuffeldal-stadje bouwt de &6Timmerguh&r een huisje.",
               "Elk verhaal eindigt met een nieuwe vriend: een verhaalguh die je (per speler) één keer mag temmen. " + NIKS_OP_SLOT]),
    "guhs_diertjes": dict(
        title="&aDiertjes van de Guhmensie", sub="Vinkjes, eendjes, egeltjes en... Sjokkel?", colour="green", icon="item:minecraft:feather",
        banner=("item:minecraft:feather", "item:minecraft:sweet_berries"),
        welcome=(["item:minecraft:feather", "item:minecraft:axolotl_bucket", "item:minecraft:sweet_berries"], None),
        links=["guhmension"],
        intro=["Dit hoofdstuk komt na &dWelkom in de Guhmensie!&r in &dDe Guhmensie&r.",
               "De Guhmensie zit vol lieve diertjes: vogeltjes in de bomen en boven de zee, guhxolotls en eendjes in de vijvers, "
               "vlindertjes en glimguhtjes boven de bloemen, en egeltjes, konijntjes en eekhoorntjes in het gras. Geen guhs, maar wel "
               "heel guh-achtig! En ergens, heel traag... Sjokkel.",
               "Kom dichtbij om ze in je &dGuhdex&r te zetten: ze tellen allemaal mee voor &6alles verzameld&r. Sommige kun je zelfs temmen "
               "en oppakken. " + NIKS_OP_SLOT]),
}
ORDER = list(CHAPTERS)

MODULE_CHAPTER = {
    "beauty": "guhs_minigames", "race": "guhs_minigames", "meppen": "guhs_minigames", "disco": "guhs_minigames",
    "golf": "guhs_minigames", "smul": "guhs_minigames", "vissen": "guhs_minigames", "eilanden": "guhs_minigames",
    "kaasmijn": "guhs_minigames", "bibliotheek": "guhs_minigames",
    "evenementen": "guhs_guhmensie", "emotes": "guhs_basis",
    "onderwater": "guhs_onderwater", "diepzee": "guhs_onderwater",
    "guheinde": "guhs_guheinde",
    "barbecuether": "guhs_barbecuether", "spiesburcht": "guhs_barbecuether",
    "gatenkaas": "guhs_extra27", "kaasmoeras": "guhs_extra27", "vadswoud": "guhs_extra27",
    "knuffeldal": "guhs_knuffeldal", "bakkerij": "guhs_knuffeldal", "creche": "guhs_knuffeldal", "theehuis": "guhs_knuffeldal",
    "kapper": "guhs_knuffeldal", "boerderij": "guhs_knuffeldal", "tuintjes": "guhs_knuffeldal", "sterrenwacht": "guhs_knuffeldal",
    "ballon": "guhs_knuffeldal", "kamperen": "guhs_knuffeldal", "knuffelbad": "guhs_knuffeldal", "wereldleven": "guhs_knuffeldal",
    "piep": "guhs_piep",
    # 2.9 (De Grote Guhspelen): everything in "Minigames & bijzondere plekken", the beroepen in Knuffeldal (FTB_CHAPTER in beroepen.py)
    "spelen": "guhs_minigames", "kleding": "guhs_basis", "gids": "guhs_basis", "klassiekers": "guhs_minigames",
    "sjoelen": "guhs_minigames", "doolhof": "guhs_minigames", "katapult": "guhs_minigames", "knabbelspelen": "guhs_minigames",
    "guhpolder": "guhs_minigames", "elftocht": "guhs_minigames", "circuit": "guhs_minigames", "beroepen": "guhs_knuffeldal",
    # 2.10 (Lieve vadsjes van elkaar): one chapter of their own
    "band": "guhs_band", "huisje": "guhs_band", "klusjes": "guhs_band", "speelgoed": "guhs_band", "guhkamer": "guhs_band",
    "samen": "guhs_band", "favorietjes": "guhs_band",
    # 3.0 (Guhverhalen): the stories and the little animals
    "verhaal": "guhs_verhalen", "timmerguh": "guhs_verhalen", "balto": "guhs_verhalen", "balto_slee": "guhs_verhalen",
    "mewtwo": "guhs_verhalen", "hemel": "guhs_verhalen", "guhwaii": "guhs_verhalen", "guhwaii_spellen": "guhs_verhalen",
    "vogels": "guhs_diertjes", "waterdiertjes": "guhs_diertjes", "landdiertjes": "guhs_diertjes",
}
PREFIX_CHAPTER = [("maag", "guhs_maag"), ("heiligdom", "guhs_maag"), ("variant_", "guhs_basis"), ("emote", "guhs_basis"),
                  ("biome_", "guhs_guhmensie"), ("struct_", "guhs_guhmensie"), ("verstop", "guhs_minigames"), ("kermis", "guhs_minigames"),
                  ("onderwater", "guhs_onderwater"), ("diepzee", "guhs_onderwater"), ("guheinde", "guhs_guheinde"),
                  ("bbq_", "guhs_barbecuether"), ("sb_", "guhs_barbecuether"), ("gatenkaas", "guhs_extra27"), ("kaasmoeras", "guhs_extra27"),
                  ("vadswoud", "guhs_extra27"), ("knuffel", "guhs_knuffeldal")]


def sec(sid, title, portrait, keys=None, module=None, until=None, upstream=None, colour=None):
    """A section: its quests are `keys`, or those of feature `module` (from where the previous section of that module stopped,
    up to `until`). upstream: what the section's first quest comes after ("intro" = this chapter's Hoe kom je hier?)."""
    return dict(sid=sid, title=title, portrait=portrait, keys=keys, module=module, until=until, upstream=upstream, colour=colour)


SECTIONS = {
    "guhs_basis": [
        sec("eerste_guh", "Je eerste guh", "guh:normal", keys=["frying", "start", "tame", "wardrobe"]),
        sec("rijden", "Rijden & vliegen", "guh:ender", keys=["ride", "launch", "ender_tame", "ender_ride"]),
        sec("soorten", "Guhsoorten & de Guhdex", "guh:starry", keys=[f"variant_{v}" for v, _ in VARIANTS] + ["guhdex_full"]),
        sec("thuis", "Lekker eten & gezellig thuis", "item:guhs:guh_taart",
            keys=["taart", "sweets", "fondue", "shake", "furniture", "vadszak", "lampgion", "wheel"]),
        sec("emotes", "Emotes: kijk wat ik kan!", "guh:mint", module="emotes", upstream="tame"),
    ],
    "guhs_guhmensie": [
        sec("portaal", "Het guhportaal", "wiki:guh_portal", keys=["portal_block", "guhmension", "pink_moon", "superkompas", "reisguhs"],
            upstream="intro"),
        sec("biomen", "De biomen", "item:minecraft:filled_map", keys=[f"biome_{b}" for b, _ in BIOMES]),
        sec("wezens", "Wezens (en Mika's, njeg)", "geo:mika:mika",
            keys=["bee", "korf", "slime", "slimeblock", "fish", "mika", "mepper", "big_mika", "nether_mika"]),
        sec("kasteel", "Het guhkasteel", "guh:koning", keys=["kasteel", "koning_tem", "kasteel_schat", "guhvriend"]),
        sec("slee", "De guhslee", "npc:slee_guh", keys=["sleehut", "sled", "ride_sled", "coaster"]),
        sec("bouwwerken", "Bouwwerken", "wiki:structure_hamster_house", keys=[f"struct_{s}" for s, _ in STRUCTS]),
        sec("spullen", "Knabbels, kristal & vads", "item:guhs:guh_kristal",
            keys=["knabbelboer", "bloesem", "ze_hangen", "crystal", "spyglass", "vads", "paxel", "bank"]),
        sec("evenementen", "Evenementen", "wiki:guh_outfit_evenementen", module="evenementen", upstream="guhmension"),
    ],
    "guhs_minigames": [
        sec("beauty", "Guh Beauty Theater", "npc:showguh", module="beauty", upstream="intro"),
        sec("race", "De guhracebaan", "npc:raceguh", module="race", upstream="intro"),
        sec("meppen", "De Mika-mephal", "npc:mepguh", module="meppen", upstream="intro"),
        sec("disco", "De Guhdisco", "npc:djguh", module="disco", upstream="intro"),
        sec("golf", "De guhgolfbaan", "npc:golfguh", module="golf", upstream="intro"),
        sec("smul", "Het Vadsig eetfestijn", "npc:smulguh", module="smul", upstream="intro"),
        sec("vissen", "De guhvisvijver", "npc:visguh", module="vissen", upstream="intro"),
        sec("verstop", "Verstopguh", "npc:verstopguhtje",
            keys=["verstop_huis", "verstop_makkelijk", "verstop_medium", "verstop_moeilijk", "verstop_detective"], upstream="intro"),
        sec("kermis", "De guhkermis", "npc:kermis_guh", keys=["kermis", "kermis_rit", "kermis_bonnen", "kermis_outfit"], upstream="intro"),
        sec("bibliotheek", "De guhbibliotheek", "npc:bibliothecaris", module="bibliotheek", upstream="intro"),
        sec("kaasmijn", "De kaasmijn", "npc:mijnguh", module="kaasmijn", upstream="intro"),
        sec("eilanden", "Zwevende guh-eilandjes", "guh:wolk", module="eilanden", upstream="intro"),
        # De Grote Guhspelen (the difficulty levels of the classics, then one section per new game)
        sec("klassiekers", "Makkelijk, medium of lastig", "npc:showguh", module="klassiekers", upstream="intro"),
        sec("sjoelen", "Het Sjoelhuisje", "npc:sjoelguh", module="sjoelen", upstream="intro"),
        sec("doolhof", "Het Guhdoolhof", "npc:doolhofguh", module="doolhof", upstream="intro"),
        sec("katapult", "De Knabbelkatapult", "npc:katapultguh", module="katapult", upstream="intro"),
        sec("knabbelspelen", "De Knabbelspelen", "npc:spelleiderguh", module="knabbelspelen", upstream="intro"),
        sec("elftocht", "De Elf-Guhjestocht", "npc:schaatsmeesterguh", module="elftocht", upstream="intro"),
        sec("circuit", "Het Guh-Circuit", "npc:circuitguh", module="circuit", upstream="intro"),
    ],
    "guhs_onderwater": [
        sec("diepzee", "De Diepe Guhzee", "npc:zeemeerguh", module="diepzee", upstream="intro"),
        sec("guhbubbel", "De Guhbubbel", "wiki:icon_duikhelm", module="onderwater", upstream="diepzee_vind"),
        # the turtles of the guhzee coasts live in the sea chapter (piepmenu); the rest of Piep keeps its own chapter guhs_piep
        sec("schildpadjes", "Schildpadjes", "geo:poepschilly:poepschilly",
            keys=["piep_schilly", "piep_poetsbeurt", "piep_schilly2", "piep_bestie", "piep_beef"], upstream="intro"),
    ],
    "guhs_maag": [
        sec("vadsig", "Moeder Vadsig", "npc:moeder_vadsig", keys=["heiligdom", "vadsig_met", "guhbert", "cake", "maag"], upstream="intro"),
        sec("maaggroei", "De maag groeit (op slot!)", "npc:tandarts", keys=[f"maag_{n}" for n in (64, 80, 96, 112, 128)]),
    ],
    "guhs_guheinde": [
        sec("opweg", "Op weg naar het Guheinde", "guh:koning", module="guheinde", until="guheinde_kristal", upstream="intro"),
        sec("oppermika", "Opper-Mika", "geo:mika:opper_mika", module="guheinde", until="guheinde_poort", upstream="guheinde_binnen"),
        sec("buiteneilanden", "De buiteneilanden", "guh:vahoege_ender", module="guheinde", upstream="guheinde_winst"),
    ],
    "guhs_barbecuether": [
        sec("barbecueput", "De kapotte barbecue", "npc:grillguh", module="barbecuether", until="bbq_dimensie", upstream="intro"),
        sec("dimensie", "De Guhbarbecuether", "wiki:barbecuether_portaal", module="barbecuether", upstream="bbq_aan"),
        sec("spiesburcht", "De Spiesburcht", "wiki:vonk_mika", module="spiesburcht", until="sb_rookguh", upstream="bbq_dimensie"),
        sec("rookdelta", "Rookguhs, paleis & Asdal", "wiki:rookguh", module="spiesburcht", upstream="bbq_biome_rookdelta"),
    ],
    "guhs_extra27": [
        sec("gatenkaas", "De Gatenkaasgrotten", "wiki:vadswaker", module="gatenkaas", upstream="intro"),
        sec("kaasmoeras", "Het Kaasmoeras", "geo:kikkerguh:kikkerguh_roze", module="kaasmoeras", until="kaasmoeras_hut", upstream="intro"),
        sec("moerasheks", "De moerasheks", "wiki:moerasheks_mika", module="kaasmoeras", upstream="kaasmoeras_vind"),
        sec("vadswoud", "Het Vadswoud", "npc:boswachterguh", module="vadswoud", until="vadswoud_gezin", upstream="intro"),
        sec("families", "Guhfamilies & pakjes", "npc:knabbelplukker", module="vadswoud", upstream="vadswoud_bezoek"),
    ],
    "guhs_knuffeldal": [
        sec("knusfeest", "Het Grote Knusfeest", "npc:burgemeesterguh", module="knuffeldal", until="knuffeldal_pluisguh", upstream="intro"),
        sec("bewoners", "Bewoners & seizoenen", "npc:cocotje", module="knuffeldal", upstream="knuffeldal_dal"),
        sec("bakkerij", "De Knabbelbakkerij", "npc:bakkerguh", module="bakkerij", upstream="knuffeldal_stadje"),
        sec("creche", "De Knuffelcreche", "npc:juf_knuffel", module="creche", upstream="knuffeldal_stadje"),
        sec("theehuis", "Het Knabbelthee-huisje", "npc:theeguh", module="theehuis", upstream="knuffeldal_stadje"),
        sec("kapper", "Knip & Vads", "npc:kapperguh", module="kapper", upstream="knuffeldal_stadje"),
        sec("boerderij", "De Guhboerderij", "npc:boerinneguh", module="boerderij", upstream="intro"),
        sec("tuintjes", "Tuintjes", "item:guhs:block/roze_guhbloem", module="tuintjes", upstream="intro"),
        sec("sterrenwacht", "De Guh-Sterrenwacht", "npc:sterrenkijkerguh", module="sterrenwacht", upstream="intro"),
        sec("ballon", "Het Ballonfestival", "npc:ballonguh", module="ballon", upstream="intro"),
        sec("kamperen", "Kamperen bij Opa Guh", "npc:opa_guh", module="kamperen", upstream="intro"),
        sec("knuffelbad", "Het Knuffelbad", "npc:badmeesterguh", module="knuffelbad", upstream="intro"),
        sec("guhdag", "Een echte guhdag", "geo:ijscoguh:ijscoguh", module="wereldleven", until="wereldleven_xylofoon", upstream="intro"),
        sec("muziek", "Muziek & knuffels", "guh:pluisguh", module="wereldleven", upstream="intro"),
    ],
    # 2.10 (Lieve vadsjes van elkaar)
    "guhs_band": [
        sec("band", "Hartjes voor je guh", "guh:normal", module="band", upstream="intro"),
        sec("huisje", "Het Guhhuisje", "item:guhs:guhhuisje_klein", module="huisje", upstream="intro"),
        sec("klusjes", "Klusjes rond het huisje", "npc:boerinneguh", module="klusjes", upstream="intro"),
        sec("speelgoed", "Speelgoed", "item:guhs:knabbelbal", module="speelgoed", upstream="intro"),
        sec("guhkamer", "De Guhkamer in je Guhmaag", "item:guhs:guhbel", module="guhkamer", upstream="intro"),
        sec("samen", "Samen spelen, samen knuffelen", "guh:mint", module="samen", upstream="intro"),
        sec("favorietjes", "Favorietjes", "guh:golden", module="favorietjes", upstream="intro"),
    ],
    # 3.0 (Guhverhalen); a module may set FTB_PORTRAIT = "<spec>" to give its section another picture (see assign)
    "guhs_verhalen": [
        sec("verhaal", "Nieuwe plekken & herinneringen", "item:minecraft:writable_book", module="verhaal", upstream="intro"),
        sec("timmerguh", "Samen een huisje bouwen", "npc:timmerguh", module="timmerguh", upstream="intro"),
        sec("balto", "Baltoguh en Nomguh", "guh:baltoguh", module="balto", upstream="intro"),
        sec("balto_slee", "Door de sneeuwstorm", "npc:steele_mika", module="balto_slee", upstream="intro"),
        sec("mewtwo", "Het kloon-eiland", "guh:mewtwo", module="mewtwo", upstream="intro"),
        sec("hemel", "Het Hemelkapelletje", "npc:wolkenhoeder", module="hemel", upstream="intro"),
        sec("guhwaii", "Ohana op Guhwai'i", "guh:stitch626", module="guhwaii", upstream="intro"),
        sec("guhwaii_spellen", "Surfen & hula", "npc:tikiguh", module="guhwaii_spellen", upstream="intro"),
    ],
    "guhs_diertjes": [
        sec("vogels", "Vogeltjes", "item:minecraft:feather", module="vogels", upstream="intro"),
        sec("waterdiertjes", "Water & insectjes", "item:minecraft:axolotl_bucket", module="waterdiertjes", upstream="intro"),
        sec("landdiertjes", "Egeltjes, konijntjes, eekhoorntjes & Sjokkel", "item:minecraft:sweet_berries", module="landdiertjes", upstream="intro"),
    ],
}
COLUMNS = {"guhs_onderwater": 1, "guhs_maag": 1, "guhs_guheinde": 1}

# the "Hoe kom je hier?" quests (new in 2.8; a checkmark to tick, nothing is locked); they come after the linked quests
for _file, _c in CHAPTERS.items():
    SOURCE[f"intro_{_file}"] = "core"
    q(f"intro_{_file}", "Hoe kom je hier?", "\n\n".join(_c["intro"]), f"guhs:textures/ftbquests/icon_{_file}.png", [{"type": "checkmark"}],
      rewards=(("guhs:kaas_knabbels", 4),), deps=list(_c["links"]), shape="gear")


def chapter_of(key):
    src = SOURCE.get(key, "core")
    if src != "core":
        mod = MODULES.get(src)
        if getattr(mod, "FTB_CHAPTER", None) in CHAPTERS:
            return mod.FTB_CHAPTER
        if src in MODULE_CHAPTER:
            return MODULE_CHAPTER[src]
    for prefix, chapter in PREFIX_CHAPTER:
        if key.startswith(prefix):
            return chapter
    return "guhs_guhmensie" if src == "core" else "guhs_minigames"


def assign():
    """-> {chapter: [section dicts with 'quests': [keys]]}: every quest exactly once (the intros aside)."""
    keys = [x[0] for x in QUESTS]
    known, done = set(keys), {f"intro_{c}" for c in CHAPTERS}
    out = {c: [] for c in CHAPTERS}
    by_module = {}
    for k in keys:
        by_module.setdefault(SOURCE[k], []).append(k)
    for c, sections in SECTIONS.items():
        for s in sections:
            if s["keys"] is not None:
                ks = [k for k in s["keys"] if k in known and k not in done]
            else:
                ks = []
                for k in by_module.get(s["module"], []):
                    if k == s["until"]:
                        break
                    if k not in done:
                        ks.append(k)
            done.update(ks)
            if ks:
                # 3.0: a module may give its section another picture (FTB_PORTRAIT = "<spec>")
                portret = getattr(MODULES.get(s["module"]), "FTB_PORTRAIT", None) if s["module"] else None
                out[c].append(dict(s, quests=ks, **({"portrait": portret} if portret else {})))
    # the rest: a section per feature module (or "Nog meer guh-dingen") at the end of its chapter
    for k in keys:
        if k in done:
            continue
        c, src = chapter_of(k), SOURCE[k]
        sid = f"extra_{src}"
        s = next((s for s in out[c] if s["sid"] == sid), None)
        if s is None:
            mod = MODULES.get(src)
            title = getattr(mod, "FTB_SECTION", None) or ("Nog meer guh-dingen" if src == "core" else src.capitalize())
            s = dict(sec(sid, title, "guh:normal", module=src, upstream="intro"), quests=[])
            out[c].append(s)
        s["quests"].append(k)
        done.add(k)
    return out


# --- layout ------------------------------------------------------------------------------------------------------------------
DX, DY, ROW = 1.5, 1.5, 7               # quest spacing and quests per row (rows snake: the next row runs back)
COL_W = (ROW - 1) * DX + 2.5            # the width of a column of sections
HEAD_W, HEAD_H = 10.0, 1.5625           # section header picture (1024x160)
TITLE_W, TITLE_H = 12.0, 3.0            # title banner (1024x256)
WELCOME_W, WELCOME_H = 6.0, 3.0         # welcome picture (768x384)
MAX_LINE = 2.3                          # only lines between neighbours (longer ones would criss-cross the chapter)


def layout(chapter, sections, deps):
    """Positions {key: (x, y)}, links [(key, x, y)], images [(name, x, y, w, h)] (x, y: the centre, like FTB Quests)."""
    pos, links, images = {}, [], []
    cols = COLUMNS.get(chapter, 2)
    right = (cols - 1) * COL_W + (ROW - 1) * DX + 0.5
    images.append(("title", (right - 0.5) / 2, -2.4, TITLE_W, TITLE_H))
    ls = CHAPTERS[chapter]["links"]
    for i, k in enumerate(ls):
        links.append((k, 0.0, 1.5 + (i - (len(ls) - 1) / 2) * 1.5))
    pos[f"intro_{chapter}"] = (2.0 if ls else 0.0, 1.5)
    images.append(("welkom", (3.3 + right) / 2 if cols > 1 else 3.3 + WELCOME_W / 2, 1.5, WELCOME_W, WELCOME_H))
    y_row, bottoms = 4.0, []
    for i, s in enumerate(sections):
        col = i % cols
        if col == 0 and bottoms:
            y_row, bottoms = max(bottoms), []
        x0, y0 = col * COL_W, y_row
        images.append((f"kop_{s['sid']}", x0 - 0.5 + HEAD_W / 2, y0 + HEAD_H / 2, HEAD_W, HEAD_H))
        n = len(s["quests"])
        chain = any(deps[b] == [a] for a, b in zip(s["quests"], s["quests"][1:]))
        for j, k in enumerate(s["quests"]):
            r, c = divmod(j, ROW)
            if r % 2 and chain:  # a chain snakes back from the right: the line goes down at the end of the row
                c = ROW - 1 - c
            pos[k] = (x0 + c * DX, y0 + HEAD_H + 1.0 + r * DY)
        rows = (n + ROW - 1) // ROW
        bottoms.append(y0 + HEAD_H + 1.0 + (rows - 1) * DY + 1.4)
    return pos, links, images


def _seg_dist(p, a, b):
    (ax, ay), (bx, by), (px, py) = a, b, p
    dx, dy = bx - ax, by - ay
    L = dx * dx + dy * dy
    t = 0 if L == 0 else max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / L))
    return math.hypot(ax + t * dx - px, ay + t * dy - py)


def _cross(a, b, c, d):
    """Do segments ab and cd cross, or overlap? (Sharing just an end point is fine.)"""
    def orient(p, q_, r):
        v = (q_[0] - p[0]) * (r[1] - p[1]) - (q_[1] - p[1]) * (r[0] - p[0])
        return 0 if abs(v) < 1e-9 else (1 if v > 0 else -1)
    shared = {a, b} & {c, d}
    if shared:
        s = shared.pop()
        o1, o2 = (b if s == a else a), (d if s == c else c)
        v1, v2 = (o1[0] - s[0], o1[1] - s[1]), (o2[0] - s[0], o2[1] - s[1])
        return abs(v1[0] * v2[1] - v1[1] * v2[0]) < 1e-9 and v1[0] * v2[0] + v1[1] * v2[1] > 0
    o = orient(a, b, c), orient(a, b, d), orient(c, d, a), orient(c, d, b)
    return o[0] != o[1] and o[2] != o[3] and 0 not in o


def _hits_rect(a, b, rect):
    x0, y0, x1, y1 = rect
    return any(x0 < a[0] + (b[0] - a[0]) * t / 40 < x1 and y0 < a[1] + (b[1] - a[1]) * t / 40 < y1 for t in range(41))


def visible_lines(pos, links, images, deps):
    """Which quests show their dependency lines: a line may not run through another quest or a picture, nor cross (or
    overlap) another line. Returns (keys whose lines are hidden, the lines that are drawn [(quest, dependency)])."""
    at = dict(pos)
    for k, x, y in links:
        at[k] = (x, y)                 # FTB Quests draws the line to the link when the quest is in another chapter
    rects = [(x - w / 2, y - h / 2, x + w / 2, y + h / 2) for _, x, y, w, h in images]
    lines = sorted(((k, d) for k in pos for d in deps[k] if d in at), key=lambda kd: math.dist(pos[kd[0]], at[kd[1]]))
    hidden, drawn = set(), []
    for k, d in lines:
        a, b = at[d], pos[k]
        if k in hidden or math.dist(a, b) > MAX_LINE or any(_seg_dist(p, a, b) < 0.62 for key, p in at.items() if key not in (k, d)) \
                or any(_hits_rect(a, b, r) for r in rects) or any(_cross(a, b, at[d2], pos[k2]) for k2, d2 in drawn):
            hidden.add(k)
        else:
            drawn.append((k, d))
    return hidden, [(k, d) for k, d in drawn if k not in hidden]


# ---------------------------------------------------------------------------------------------------------------------------
def snbt(v, indent=1):
    tab = "\t" * indent
    if isinstance(v, dict):
        return "{\n" + "".join(f"{tab}{k if re.fullmatch(r'[A-Za-z0-9_.]+', k) else snbt(k)}: {snbt(x, indent + 1)}\n"
                               for k, x in v.items()) + "\t" * (indent - 1) + "}"
    if isinstance(v, list):
        if not v:
            return "[ ]"
        if all(isinstance(x, str) for x in v):  # text lists on one line, like FTB Quests writes them
            return "[" + ", ".join(snbt(x) for x in v) + "]"
        return "[\n" + "".join(f"{tab}{snbt(x, indent + 1)}\n" for x in v) + "\t" * (indent - 1) + "]"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, float):
        return f"{v}d"
    if isinstance(v, Long):
        return f"{int(v)}L"
    if isinstance(v, int):
        return str(v)
    return '"' + str(v).replace("\\", "\\\\").replace('"', '\\"') + '"'


_JSON5_KEY = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*")


def json5(v, indent=0):
    """JSON5 like FTB Quests 26.1 writes it itself: two-space indent, bare keys where possible, a comma after every entry.
    (FTB Quests 26.1 reads only JSON5, no SNBT any more; numbers have no d/L suffixes.)"""
    pad = "  " * (indent + 1)
    if isinstance(v, dict):
        if not v:
            return "{}"
        return "{\n" + "".join(f"{pad}{k if _JSON5_KEY.fullmatch(k) else json.dumps(k, ensure_ascii=False)}: {json5(x, indent + 1)},\n"
                               for k, x in v.items()) + "  " * indent + "}"
    if isinstance(v, list):
        if not v:
            return "[]"
        return "[\n" + "".join(f"{pad}{json5(x, indent + 1)},\n" for x in v) + "  " * indent + "]"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, float):
        return repr(v)
    if isinstance(v, int):
        return str(int(v))
    return json.dumps(str(v), ensure_ascii=False)


def quest_nbt(key, icon, tasks, rewards, xp):
    task_list = []
    for n, t in enumerate(tasks):
        t = dict(t)
        t["id"] = qid(f"{key}/task{n}")
        if t["type"] == "item":
            t["item"] = {"count": 1, "id": t["item"]}
            if t["count"] > 1:
                t["count"] = Long(t["count"])
            else:
                del t["count"]
        if t["type"] == "advancement":
            t["criterion"] = ""
        if t["type"] == "kill":
            t["value"] = Long(t["value"])
        task_list.append(t)
    reward_list = [{"id": qid(f"{key}/reward{n}"), "type": "item", "item": {"count": 1, "id": r}, **({"count": c} if c > 1 else {})}
                   for n, (r, c) in enumerate(rewards)]
    if xp:
        reward_list.append({"id": qid(f"{key}/xp"), "type": "xp", "xp": xp})
    icon_nbt = {"components": {"ftbquests:icon": icon}, "id": "ftbquests:custom_icon"} if icon.endswith(".png") else {"id": icon}
    return {"id": qid(key), "icon": icon_nbt, "tasks": task_list, "rewards": reward_list}


def ftb_text(text):
    """FTB Quests reads '&' + letter as a colour code; a lone '&' (as in "Guhs & basis") must be written as '\\&'."""
    return re.sub(r"(?<!\\)&(?![0-9a-fk-or])", r"\\&", text)


def plain(text):
    return re.sub(r"&[0-9a-fk-or]", "", text)


def plan():
    """Everything but the writing: ({chapter: dict(sections, pos, links, images, hidden, drawn)}, deps, where, info)."""
    info = {x[0]: x for x in QUESTS}
    sections = assign()
    where = {}
    for c, ss in sections.items():
        where[f"intro_{c}"] = c
        for s in ss:
            for k in s["quests"]:
                where[k] = c
    deps = {x[0]: [d for d in x[6] if d in info] for x in QUESTS}
    # the logical order: a feature's quests (without dependencies of their own) follow each other, the first one comes
    # after the chapter's "Hoe kom je hier?"; a section's first quest comes after the section's upstream
    previous = {}
    for key, *_ in QUESTS:
        src = SOURCE[key]
        if src != "core" and not deps[key]:
            before = previous.get(src)
            deps[key] = [before if before and where[before] == where[key] else f"intro_{where[key]}"]
        previous[src] = key
    for c, ss in sections.items():
        for s in ss:
            if s["upstream"]:
                deps[s["quests"][0]] = [f"intro_{c}" if s["upstream"] == "intro" else s["upstream"]]
    out = {}
    for c in ORDER:
        pos, links, images = layout(c, sections[c], deps)
        hidden, drawn = visible_lines(pos, links, images, gates(deps))
        out[c] = dict(sections=sections[c], pos=pos, links=links, images=images, hidden=hidden, drawn=drawn)
    return out, deps, where, info


def gates(deps):
    """The dependencies FTB Quests really gets: only the stomach sizes come after each other (see the top)."""
    return {k: (v if k.startswith("maag_") else []) for k, v in deps.items()}


LOCALES = ("nl_nl", "en_us")   # ftbquests/lang/<locale>/<chapter>.json5; compat/FtbQuestsChapter installs both


def build(force_art=False):
    import make_ftbquests_art as art
    chapters, deps, where, info = plan()
    locks = gates(deps)
    group_id = qid("group")
    # (1.2.0) the English overlay, also for the English pictures: <chapter>/en/title.png and en/kop_<sid>.png (with the
    # English text painted in; the client shows those instead with English chosen: compat/FtbQuestsTaal.image)
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import lang as en_overlay   # tools/lang/
    overlay = en_overlay.load_overlay()
    en_text = lambda key, nl: plain(overlay.get(key, nl))  # noqa: E731
    # the pictures (only redrawn when their recipe changes)
    jobs = {}
    for i, (c, ch) in enumerate(chapters.items()):
        spec = CHAPTERS[c]
        jobs[f"icon_{c}.png"] = ("icon", [spec["icon"], 64])
        jobs[f"{c}/title.png"] = ("title", [plain(spec["title"]), spec["colour"], spec["banner"][0], spec["banner"][1], i])
        jobs[f"{c}/en/title.png"] = ("title", [en_text(f"ftb.{c}.title", spec["title"]), spec["colour"], spec["banner"][0],
                                               spec["banner"][1], i])
        jobs[f"{c}/welkom.png"] = ("welcome", [spec["welcome"][0], spec["colour"], i, spec["welcome"][1]])
        ch["komt_na"] = {}
        for s in ch["sections"]:
            first = s["quests"][0]
            sub = sub_en = None
            if deps[first] and deps[first][0] not in s["quests"] and not any(k == first for k, _ in ch["drawn"]):
                u = deps[first][0]
                if u == f"intro_{c}" and spec["links"]:
                    u = spec["links"][0]          # (the intro itself comes after that quest in another chapter)
                sub = ["Komt na: " + plain(info[u][1])]
                q_en = en_text(f"ftb.{where[u]}.q.{u}.title", info[u][1])
                sub_en = [plain(overlay.get("ftb.komt_na", "Komt na: %s")).replace("%s", q_en)]
                if where.get(u) != c:
                    sub.insert(0, sub[0] + " (" + plain(CHAPTERS[where[u]]["title"]) + ")")
                    ch_en = en_text(f"ftb.{where[u]}.title", CHAPTERS[where[u]]["title"])
                    sub_en.insert(0, plain(overlay.get("ftb.komt_na_elders", "Komt na: %s (%s)")).replace("%s", q_en, 1).replace("%s", ch_en, 1))
            ch["komt_na"][s["sid"]] = sub
            colour = s["colour"] or spec["colour"]
            jobs[f"{c}/kop_{s['sid']}.png"] = ("header", [s["title"], s["portrait"], colour, sub])
            jobs[f"{c}/en/kop_{s['sid']}.png"] = ("header", [en_text(f"ftb.{c}.section.{s['sid']}", s["title"]), s["portrait"], colour, sub_en])
    drawn = art.make_art(jobs, force=force_art)
    print(f"FTB Quests pictures: {len(jobs)} ({drawn} drawn)")

    # the chapters, their texts (Dutch, and English from the overlay) and the installer's index
    source = {}                                     # overlay key -> Dutch (tools/lang/source_ftb.json)

    def texts(key, nl):
        """{"nl_nl": Dutch, "en_us": English or the Dutch}, and the key into the source."""
        source[key] = nl
        return {"nl_nl": nl, "en_us": overlay.get(key, nl)}

    os.makedirs(os.path.join(OUT, "chapters"), exist_ok=True)
    for locale in LOCALES:
        os.makedirs(os.path.join(OUT, "lang", locale), exist_ok=True)
    for old in ("guhs.snbt", "guhs_lang.snbt"):     # (the old single chapter before 2.8; the SNBT texts before 1.1.0)
        if os.path.exists(os.path.join(OUT, old)):
            os.remove(os.path.join(OUT, old))
    for d in ["chapters", "lang"] + [os.path.join("lang", locale) for locale in LOCALES]:
        for f in os.listdir(os.path.join(OUT, d)):  # chapters that are gone, the SNBT files before 1.1.0, lang/<c>.json5 before 1.2.0
            if os.path.isfile(os.path.join(OUT, d, f)) and (d == "lang" or not f.endswith(".json5") or f[:-6] not in CHAPTERS):
                os.remove(os.path.join(OUT, d, f))
    group_title = texts("ftb.group.title", GROUP_TITLE)
    texts("ftb.komt_na", "Komt na: %s")             # (painted on the section header pictures, not in the lang files yet)
    texts("ftb.komt_na_elders", "Komt na: %s (%s)")
    index = [f"version {CHAPTER_VERSION}", f"group {group_id}"]
    total = 0
    for order, (c, ch) in enumerate(chapters.items()):
        spec = CHAPTERS[c]
        cid = qid(f"chapter/{c}")
        title, sub = texts(f"ftb.{c}.title", spec["title"]), texts(f"ftb.{c}.sub", spec["sub"])
        lang = {locale: {f"chapter.{cid}.title": ftb_text(title[locale]),
                         f"chapter.{cid}.chapter_subtitle": [ftb_text(f"{order + 1}. " + sub[locale])]} for locale in LOCALES}
        for sect in ch["sections"]:                 # (only painted on the header pictures, not in the lang files yet)
            texts(f"ftb.{c}.section.{sect['sid']}", sect["title"])
        quests = []
        for key in [f"intro_{c}"] + [k for s in ch["sections"] for k in s["quests"]]:
            _, title, desc, icon, tasks, rewards, _, _, _, shape, xp = info[key]
            qn = quest_nbt(key, icon, tasks, rewards, xp)
            x, y = ch["pos"][key]
            qn["x"], qn["y"] = float(x), float(y)
            if locks[key]:
                qn["dependencies"] = [qid(d) for d in locks[key]]
            if key in ch["hidden"]:
                qn["hide_dependency_lines"] = True
            if key.startswith("maag_"):
                qn["progression_mode"] = "linear"   # the only lock: the stomach sizes come after each other
            if key.startswith("intro_"):
                qn["size"] = 1.3
            if shape:
                qn["shape"] = shape
            quests.append(qn)
            qt, qd = texts(f"ftb.{c}.q.{key}.title", title), texts(f"ftb.{c}.q.{key}.desc", desc)
            for locale in LOCALES:
                lang[locale][f"quest.{qn['id']}.title"] = ftb_text(qt[locale])
                lang[locale][f"quest.{qn['id']}.quest_desc"] = [ftb_text(line) for line in qd[locale].split("\n")]
        images = []
        for name, x, y, w, h in ch["images"]:
            img = {"height": float(h), "id": qid(f"image/{c}/{name}"), "image": f"guhs:textures/ftbquests/{c}/{name}.png",
                   "rotation": 0.0, "width": float(w), "x": float(x), "y": float(y)}
            sub = ch["komt_na"].get(name[4:]) if name.startswith("kop_") else None
            if sub:
                img["hover"] = [ftb_text(sub[0])]
            images.append(img)
        links = [{"id": qid(f"link/{c}/{k}"), "linked_quest": qid(k), "shape": "hexagon", "x": float(x), "y": float(y)}
                 for k, x, y in ch["links"]]
        chapter = {"default_hide_dependency_lines": False, "default_quest_shape": "circle", "filename": c, "group": group_id,
                   "guhs_chapter_version": CHAPTER_VERSION,
                   "icon": {"components": {"ftbquests:icon": f"guhs:textures/ftbquests/icon_{c}.png"}, "id": "ftbquests:custom_icon"},
                   "id": cid, "images": images, "order_index": order, "progression_mode": "flexible", "quest_links": links,
                   "quests": quests}
        with open(os.path.join(OUT, "chapters", c + ".json5"), "w", encoding="utf-8", newline="\n") as f:
            f.write(json5(chapter) + "\n")
        for locale in LOCALES:
            group_lang = {f"chapter_group.{group_id}.title": ftb_text(group_title[locale])} if order == 0 else {}
            with open(os.path.join(OUT, "lang", locale, c + ".json5"), "w", encoding="utf-8", newline="\n") as f:
                f.write(json5(group_lang | lang[locale]) + "\n")
        index.append(f"chapter {c}")
        total += len(quests)
        print(f"  {c}: {len(quests)} quests in {len(ch['sections'])} sections, {len(ch['drawn'])} lines drawn, "
              f"{len(ch['hidden'])} quests with their lines hidden")
    with open(os.path.join(OUT, "index.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(index) + "\n")
    en_overlay.write_json(en_overlay.FTB_SOURCE, source)
    missing = sum(1 for k, v in source.items() if k not in overlay and en_overlay.needs_english(v))
    print(f"FTB Quests texts: {len(source)} keys, {missing} still Dutch in en_us (no English in tools/lang/en yet)")
    assert total == len(QUESTS) == len({x[0] for x in QUESTS}), (total, len(QUESTS))
    print(f"FTB Quests: {len(chapters)} chapters, {total} quests ({total - len(CHAPTERS)} + {len(CHAPTERS)} 'Hoe kom je hier?')")


if __name__ == "__main__":
    build(force_art="--art" in sys.argv)
