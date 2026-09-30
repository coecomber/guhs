"""
De beroepen (2.9, slice beroepen): four guh characters with one job each (one-time quests, DESIGN_29 §11).

  Brandweercommandant Blusguh (brandweerguh)   Brandweerkazerne   blow out the marshmallow fires, get the guhtje out of the tree
  Inspecteur Vahoegsma (politieguh)            Politiebureautje   De Knabbeldief-zaak: follow the paw prints, find the knabbels
  Dokter Snotneus-guh (apothekerguh)           Apotheekje         snotkruidjes + kaasmelk in the mengketel, for snotterig Snotje
  Bob de Guhbouwer (bouwvakkerguh)             guh village        planks and lunch, then lay the dakpannen on his roof

The first three are in the Beroepenstraat, the Knuffeldal town's 2.9 street piece on its free street jigsaw
(beroepen_straat.py; knuffeldal_stadje.VRIJ_STUKKEN puts it in the pool guhs:knuffeldal_stadje/vrij); Bob's half-built
house is in the guh village's third layout (beroepen_bouwplaats.py, patched into the pool guh_village/start).
Textures and models: beroepen_tex.py; the sounds (committed OGGs): beroepen_geluid.py. Java: feature/beroepen.
"""
import os
import re

from features import beroepen_bouwplaats as bouwplaats
from features import beroepen_straat as straat
from features import beroepen_tex as tex

FTB_CHAPTER = "guhs_knuffeldal"   # (CONTRACT_29 §3.2: the beroepen quests go to the Knuffeldal chapter)
FTB_SECTION = "De beroepen"
KINDS = ["brandweerguh", "politieguh", "apothekerguh", "bouwvakkerguh"]
BEROEPEN = ["brandweer", "politie", "apotheek", "bouw"]
VILLAGE_LAYOUT = "guh_village/layout_c"
SOUNDS = {"sirene": ["sirene"], "spuiten": ["spuiten"], "sissen": ["sissen"], "hatsjoe": ["hatsjoe1", "hatsjoe2"],
          "hamer": ["hamer1", "hamer2"], "bubbel": ["bubbel"]}
BONES = {}
CLOTHES = []


# =====================================================================================================================
# the characters: names and Guhdex pages
# =====================================================================================================================
NPCS = {
    "brandweerguh": ("Brandweercommandant Blusguh", "Uniek (de Brandweerkazerne in de Beroepenstraat van het Knuffeldal-stadje)",
                     "Brandweercommandant Blusguh heeft de rode helm met de gouden ster en de mooiste roetsnor van het hele dal. Hij "
                     "blust elk marshmallowkampvuur dat te vadsig oplaait, en haalt guhtjes uit bomen (daar klimmen ze graag in, maar "
                     "eruit komen... njeg). Help hem een keer mee, dan krijg je je eigen brandweerhelm en -jas. Tatuu-tatuu, VAHOEG!"),
    "politieguh": ("Inspecteur Vahoegsma", "Uniek (het Politiebureautje in de Beroepenstraat van het Knuffeldal-stadje)",
                   "Inspecteur Vahoegsma lost elke zaak op met zijn grote vergrootglas en een zakje kaasknabbels (om na te denken). Zijn "
                   "grootste zaak: de Knabbeldief-zaak! Een Mika heeft de knabbelvoorraad van het stadje gepikt en liet vadsige "
                   "pootafdrukken achter. Mika's worden bij hem nooit gestraft, alleen stevig geknuffeld in de knuffelcel."),
    "apothekerguh": ("Dokter Snotneus-guh", "Uniek (het Apotheekje in de Beroepenstraat van het Knuffeldal-stadje)",
                     "Dokter Snotneus-guh heeft een witte jas, een stethoscoop, een spiegeltje op zijn hoofd en een rode neus die "
                     "altijd een beetje loopt (daar komt zijn naam vandaan, hatsjoe). Met snotkruidjes uit zijn kruidentuin en kaasmelk "
                     "maakt hij het lekkerste kaasmelkdrankje van de Guhmensie: daar wordt elk snotterig guhtje weer VAHOEG van."),
    "bouwvakkerguh": ("Bob de Guhbouwer", "Uniek (bij het halve huisje in sommige guh-dorpen)",
                      "Bob de Guhbouwer bouwt het mooiste huisje van het dorp, met een potlood achter zijn oor en een hamer aan zijn "
                      "riem. Alleen het dak is nog niet af: hij heeft planken nodig, en lunch (een bouwvakker zonder kaasknabbels "
                      "wordt nooit vahoeg). Kunnen wij het maken? JA, WIJ KUNNEN HET!"),
}

T = {}


def t(key, text):
    T[key] = text


ADV = {
    "beroepen_brandweer": ("Tatuu-tatuu!", "Blus de marshmallowvuurtjes en haal het guhtje uit de boom voor Brandweercommandant Blusguh"),
    "beroepen_politie": ("De Knabbeldief-zaak", "Volg de pootafdrukken en vind de knabbelvoorraad van het stadje terug voor Inspecteur Vahoegsma"),
    "beroepen_apotheek": ("Hatsjoe... VAHOEG!", "Maak Snotje weer beter met een kaasmelkdrankje van Dokter Snotneus-guh"),
    "beroepen_bouw": ("Kunnen wij het maken?", "Leg het dak op het huisje van Bob de Guhbouwer: de vlag in top!"),
    "beroepen_alle": ("Guh van alle markten", "Leer alle vier de beroepen: brandweer, politie, apotheek en bouw"),
}
ADV_ICONS = {"beroepen_brandweer": "guhs:firefighter_helmet", "beroepen_politie": "guhs:police_cap", "beroepen_apotheek": "guhs:stethoscope",
             "beroepen_bouw": "guhs:builder_helmet", "beroepen_alle": "guhs:safety_vest"}


def texts():
    # --- general ---
    t("gui.guhs.beroepen.geleerd", "Beroep geleerd: %s! VAHOEG!")
    for b, naam in zip(BEROEPEN, ("Brandweerguh", "Politieguh", "Apothekersguh", "Bouwguh")):
        t(f"gui.guhs.beroepen.naam.{b}", naam)
    # --- Blusguh ---
    t("quest.guhs.beroepen.brandweer.start", "TATUU-TATUU! Help, help! Op het oefenterrein naast de kazerne zijn %s marshmallowkampvuurtjes "
      "veel te vadsig opgelaaid! Hier, pak mijn guh-brandslang: houd rechtsklik ingedrukt en spuit ze allemaal uit. Vlug, voordat de "
      "marshmallows verbranden!")
    t("quest.guhs.beroepen.brandweer.nog", "Nog %s vuurtjes! Blijf spuiten (rechtsklik ingedrukt houden), dan gaan ze vanzelf kleiner. Pssst!")
    t("quest.guhs.beroepen.brandweer.allemaal_uit", "Alles uit! Goed gedaan, collega! Maar... HOOR JE DAT? Piep piep! Er zit een guhtje "
      "hoog in de grote boom op het oefenterrein, het durft er niet meer uit! Klim via de ladder omhoog en help het naar beneden.")
    t("quest.guhs.beroepen.brandweer.boom", "Het guhtje zit nog in de boom! Klim de ladder op en rechtsklik op het guhtje, dan springt het "
      "zo in je armen.")
    t("quest.guhs.beroepen.brandweer.gered", "Hup, in je armen en weer veilig op de grond! Wat een held ben jij. Hier, je eigen brandweerhelm "
      "en brandweerjas: je bent nu een echte brandweerguh! Mijn slang neem ik weer mee, die moet drogen in de slangentoren.")
    t("quest.guhs.beroepen.brandweer.bezet", "Even wachten, er is al een brandweerguh aan het blussen! Kom zo nog eens terug. Tatuu!")
    t("quest.guhs.beroepen.brandweer.niks", "Hè? Ik zie hier helemaal geen oefenterrein... Njeg. Kom me maar opzoeken in de Brandweerkazerne "
      "van het Knuffeldal.")
    for i, s in enumerate(("Daar is mijn beste brandweerguh! Staat de helm je goed? Natuurlijk, VAHOEG!",
                           "Vandaag nog geen enkel marshmallowvuurtje te vadsig. Dankzij jou weten alle guhtjes nu hoe het moet!",
                           "Het guhtje uit de boom vraagt steeds naar jou. Het klimt nu alleen nog in hele lage struikjes, hihi.")):
        t(f"quest.guhs.beroepen.brandweer.bedankt{i}", s)
    t("gui.guhs.beroepen.brandweer.verlopen", "Blusguh heeft de vuurtjes zelf maar uitgeblazen. Praat nog eens met hem om opnieuw te helpen.")
    t("gui.guhs.beroepen.brandweer.guhtje_wacht", "Het guhtje wacht op de brandweer... Praat eerst met Brandweercommandant Blusguh!")
    t("entity.guhs.beroepen_boomguhtje", "Klimguhtje (help!)")
    t("entity.guhs.beroepen_boomguhtje.gered", "Klimguhtje (gered!)")
    # --- Vahoegsma ---
    t("quest.guhs.beroepen.politie.start", "Alarm, alarm! De knabbelkluis van het stadje is LEEG! Iemand heeft de hele knabbelvoorraad "
      "gepikt... En kijk: roze, vadsige pootafdrukken. Dat is een Mika, dat weet ik zeker. Volg het spoor vanaf de kluis en vind de "
      "knabbels terug! (Wees maar niet bang: een Mika giechelt alleen maar.)")
    t("quest.guhs.beroepen.politie.volg", "Volg de roze pootafdrukjes vanaf de knabbelkluis! Ze glinsteren een beetje, dan zie je ze beter.")
    t("quest.guhs.beroepen.politie.opgelost", "Je hebt de knabbels terug! Zaak opgelost! De Knabbeldief is giechelend weggerend, maar ach, "
      "hij had gewoon honger. Hier, je eigen politiepet en uniform: jij bent nu een echte politieguh. VAHOEG!")
    t("quest.guhs.beroepen.politie.bezet", "Er is al een rechercheur met deze zaak bezig! Twee speurneuzen op één spoor, dat wordt een "
      "knoeiboel. Kom zo nog eens terug.")
    t("quest.guhs.beroepen.politie.niks", "Hmm, geen kluis en geen spoor te bekennen hier... Kom me maar opzoeken in het Politiebureautje "
      "van het Knuffeldal.")
    for i, s in enumerate(("Ha, mijn beste rechercheur! Geen enkele knabbel meer gepikt sinds jij er bent.",
                           "De Knabbeldief kwam vanochtend sorry zeggen. Hij kreeg een knuffel in de knuffelcel en een kaasknabbel. Njeg!",
                           "Staat die pet je even goed! Je lijkt wel een echte inspecteur. Vahoegsma is trots op je.")):
        t(f"quest.guhs.beroepen.politie.bedankt{i}", s)
    t("gui.guhs.beroepen.politie.verlopen", "Het spoor van de Knabbeldief is afgekoeld... Praat nog eens met Inspecteur Vahoegsma voor een nieuw spoor.")
    t("gui.guhs.beroepen.politie.gevonden", "Gevonden: de knabbelvoorraad van het stadje! Ga het goede nieuws vertellen aan Inspecteur Vahoegsma.")
    t("gui.guhs.beroepen.politie.niet_van_jou", "Een zak vol kaasknabbels... Die is van het stadje! Inspecteur Vahoegsma zoekt hem.")
    t("gui.guhs.beroepen.knabbeldief.betrapt", "Hihihi! Betrapt! De Knabbeldief rent giechelend weg... njeg!")
    t("entity.guhs.knabbeldief_mika", "Knabbeldief")
    # --- Snotneus-guh ---
    t("quest.guhs.beroepen.apotheek.start", "Hatsjoe... o nee, dat was ik niet, dat was Snotje! Hij zit in het ziekenhoekje en hij is zó "
      "snotterig. Ik weet een recept: pluk %s snotkruidjes in mijn kruidentuin achter de apotheek, en meng ze met kaasmelk in de mengketel "
      "bij het raam. Geef het drankje dan aan Snotje!")
    t("quest.guhs.beroepen.apotheek.kaasmelk", "Hier, een flesje kaasmelk uit mijn koelkastje. Niet zelf opdrinken, hè!")
    t("quest.guhs.beroepen.apotheek.kruidjes", "Je hebt %s van de %s snotkruidjes. De rest groeit in de kruidentuin en in het kasje achter "
      "de apotheek: rechtsklik op een grote plant.")
    t("quest.guhs.beroepen.apotheek.ketel", "Genoeg snotkruidjes en kaasmelk! Rechtsklik nu op de mengketel bij het raam. Blub blub...")
    t("quest.guhs.beroepen.apotheek.geef", "Wat ruikt dat lekker! Geef het drankje aan Snotje: rechtsklik op hem met het drankje in je hand.")
    t("quest.guhs.beroepen.apotheek.klaar", "Snotje is weer helemaal beter, hoor je hem? Geen hatsjoe meer, alleen nog VAHOEG! Hier, je eigen "
      "doktersjas en stethoscoop. Jij bent nu een echte apothekersguh!")
    for i, s in enumerate(("Snotje vraagt of je nog eens komt spelen. Hij heeft al drie dagen niet geniesd!",
                           "Hatsjoe... Oei, nu ben ik zelf een beetje snotterig. Geeft niks, ik weet wel een drankje!",
                           "Luister eens met je stethoscoop: dat is het geluid van een heel vahoeg guhhartje.")):
        t(f"quest.guhs.beroepen.apotheek.bedankt{i}", s)
    t("gui.guhs.beroepen.snotje.snif", "Snotje: *snif snif*... HATSJOE! (Hij heeft een kaasmelkdrankje nodig.)")
    t("gui.guhs.beroepen.snotje.blij", "Snotje zwaait naar je. Geen snotneus meer!")
    t("gui.guhs.beroepen.snotje.al_beter", "Snotje is al beter! Bewaar het drankje maar voor een andere snotneus.")
    t("gui.guhs.beroepen.snotje.beter", "Glug glug... HATSJOE... VAHOEG! Snotje is weer beter! Vertel het aan Dokter Snotneus-guh.")
    t("gui.guhs.beroepen.snotje.vahoeg", "Glug glug... VAHOEG! Snotje voelt zich weer helemaal fijn.")
    t("gui.guhs.beroepen.mengketel.nodig", "Voor een kaasmelkdrankje: snotkruidjes %s/%s en kaasmelk %s/1.")
    t("gui.guhs.beroepen.mengketel.klaar", "Blub blub... Een kaasmelkdrankje! Het ruikt naar kaas en naar lente.")
    t("entity.guhs.beroepen_snotje", "Snotje (hatsjoe!)")
    # --- Bob ---
    t("quest.guhs.beroepen.bouw.start", "Kunnen wij het maken? JA, WIJ KUNNEN HET! ... Nou ja, bijna. Mijn huisje is bijna af, alleen het "
      "dak nog. Breng me %s planken (van elk hout) voor de dakbalken en %s kaasknabbels voor mijn lunch. Zonder lunch wordt een "
      "bouwvakker nooit vahoeg!")
    t("quest.guhs.beroepen.bouw.nodig", "Ik heb nog nodig: planken %s/%s en kaasknabbels %s/%s. Kom maar terug als je alles hebt!")
    t("quest.guhs.beroepen.bouw.materiaal", "Mooie planken! En kaasknabbels, njam! Hier zijn de dakpannen: %s stuks. Klim via de ladder op het "
      "dak, loop de treetjes op en rechtsklik met een dakpan op elk doorzichtig dakplekje.")
    t("quest.guhs.beroepen.bouw.nog", "Nog %s dakpannen te gaan! Rechtsklik met een dakpan op een doorzichtig plekje bovenop het dak.")
    t("quest.guhs.beroepen.bouw.af", "DE VLAG IN TOP! Het dak is af! Kunnen wij het maken? JA, WIJ HEBBEN HET GEMAAKT! Hier, je eigen "
      "bouwhelm en veiligheidshesje. Jij bent nu een echte bouwguh. VAHOEG!")
    t("quest.guhs.beroepen.bouw.gepikt", "Oei... vannacht zijn er Mika's op mijn dak geklommen en die hebben giechelend alle dakpannen "
      "geleend. Njeg! Dan maar opnieuw.")
    t("quest.guhs.beroepen.bouw.niks", "Hè, waar is mijn huisje gebleven? Ik bouw alleen in guh-dorpen, hoor.")
    for i, s in enumerate(("Kijk eens hoe mooi mijn huisje is! Met dank aan de beste bouwguh van de Guhmensie.",
                           "Ik ben al aan mijn volgende huisje aan het denken. Met een glijbaan van het dak af, VAHOEG!",
                           "Staat die bouwhelm je goed! Nu nog een potlood achter je oor, dan ben je net als ik.")):
        t(f"quest.guhs.beroepen.bouw.bedankt{i}", s)
    t("gui.guhs.beroepen.bouw.nog", "Nog %s dakpannen te gaan!")
    t("gui.guhs.beroepen.bouw.past_niet", "Die dakpan past alleen op een doorzichtig dakplekje van Bob's dak.")
    # --- items and blocks ---
    t("item.guhs.guh_brandslang", "Guh-brandslang")
    t("item.guhs.guh_brandslang.lore", "Houd rechtsklik ingedrukt om te spuiten. Te vadsige vuurtjes gaan er vanzelf van uit")
    t("item.guhs.guh_brandslang.lore2", "Geleend van Brandweercommandant Blusguh")
    t("item.guhs.kaasmelkdrankje", "Kaasmelkdrankje")
    t("item.guhs.kaasmelkdrankje.lore", "Snotkruidjes en kaasmelk, gemengd in de mengketel. Goed tegen elke snotneus")
    t("item.guhs.kaasmelkdrankje.gedronken", "Je neus is weer helemaal vrij. VAHOEG!")
    t("item.guhs.snotkruidje", "Snotkruidje")
    t("item.guhs.snotkruidje.lore", "Voor in de mengketel. Niet aan snuiven, dan... hatsjoe!")
    t("block.guhs.beroepen_dakpan", "Dakpan")
    t("item.guhs.beroepen_dakpan.lore", "Past alleen op een dakplekje van Bob de Guhbouwer's dak")
    t("block.guhs.beroepen_dakplek", "Dakplekje (hier moet nog een dakpan)")
    t("block.guhs.beroepen_marshmallowvuur", "Marshmallowkampvuurkuil")
    t("block.guhs.beroepen_marshmallowvuur.lore", "Gezellig met marshmallows. Laait hij te vadsig op? Bel de brandweer!")
    t("block.guhs.beroepen_mengketel", "Mengketel")
    t("block.guhs.beroepen_mengketel.lore", "3 snotkruidjes en een kaasmelk erin: blub blub, een kaasmelkdrankje")
    t("block.guhs.beroepen_knabbelbuit", "Zak gepikte kaasknabbels")
    t("block.guhs.beroepen_knabbelbuit.lore", "Met een roze Mikapootje erop. Van wie zou die zijn?")
    t("block.guhs.beroepen_snotkruid", "Snotkruid")
    t("block.guhs.beroepen_pootafdruk", "Mika-pootafdruk")
    t("block.guhs.beroepen_guhtjeplek", "Klimguhtje-plek")
    t("block.guhs.beroepen_kluisplek", "Knabbelkluis-plek")
    t("block.guhs.beroepen_verstopplek", "Knabbeldief-verstopplek")
    for s, text in (("sirene", "Brandweersirene: tatuu-tatuu"), ("spuiten", "Brandslang spuit"), ("sissen", "Vuurtje sist uit"),
                    ("hatsjoe", "Guhtje niest: hatsjoe!"), ("hamer", "Hamer tikt"), ("bubbel", "Mengketel borrelt")):
        t(f"subtitles.guhs.beroepen.{s}", text)
    # --- signs ---
    t("sign.guhs.beroepen_straat", "Beroepenstraat")
    t("sign.guhs.beroepen_straat2", "VAHOEG aan het werk!")
    t("sign.guhs.beroepen_naar_brandweer", "< Brandweer")
    t("sign.guhs.beroepen_naar_politie", "Politie >")
    t("sign.guhs.beroepen_naar_apotheek", "Apotheek >>")
    t("sign.guhs.beroepen_einde", "Einde straat")
    t("sign.guhs.beroepen_einde2", "Doei doei!")
    t("sign.guhs.beroepen_bouwbord1", "Hier bouwt")
    t("sign.guhs.beroepen_bouwbord2", "Bob de Guhbouwer")
    t("sign.guhs.beroepen_bouwbord3", "Kunnen wij het maken?")
    # --- advancements ---
    for key, (title, desc) in ADV.items():
        t(f"advancements.guhs.grote_guhspelen.{key}.title", title)
        t(f"advancements.guhs.grote_guhspelen.{key}.description", desc)


def advancements(h):
    D = h.D
    impossible = {"done": {"trigger": "minecraft:impossible"}}
    for key in ADV:
        h.w(f"{D}/advancement/quest/{key}.json", {"criteria": impossible})
        frame = "challenge" if key == "beroepen_alle" else "task"
        h.w(f"{D}/advancement/grote_guhspelen/{key}.json", {
            "parent": "guhs:grote_guhspelen/root",
            "display": {"icon": {"id": ADV_ICONS[key]}, "title": {"translate": f"advancements.guhs.grote_guhspelen.{key}.title"},
                        "description": {"translate": f"advancements.guhs.grote_guhspelen.{key}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": impossible})
    for kind in KINDS:
        h.w(f"{D}/advancement/quest/seen_{kind}.json", {"criteria": impossible})


# =====================================================================================================================
# blocks/items data, sounds, tags, the village pool
# =====================================================================================================================
def data(h):
    D = h.D
    for b in ("beroepen_marshmallowvuur", "beroepen_mengketel"):
        h.self_drop(b)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:beroepen_marshmallowvuur", "guhs:beroepen_mengketel"])
    h.add_tag("guhs/tags/item/loaned", ["guhs:guh_brandslang", "guhs:beroepen_dakpan"])
    h.shaped("beroepen_mengketel", [" P ", "PCP", " P "], {"P": "minecraft:pink_terracotta", "C": "minecraft:cauldron"}, "guhs:beroepen_mengketel")
    h.shapeless("beroepen_marshmallowvuur", ["minecraft:campfire", "minecraft:white_wool", "minecraft:stick"], "guhs:beroepen_marshmallowvuur")

    def sounds(d):
        for name, files in SOUNDS.items():
            d[f"beroepen.{name}"] = {"subtitle": f"subtitles.guhs.beroepen.{name}", "sounds": [f"guhs:beroepen/{f}" for f in files]}
    h.patch_json(f"{h.A}/sounds.json", sounds)

    # the guh village's third layout (Bob's bouwplaats) in its start pool, next to the two existing ones
    def pool(d):
        loc = f"guhs:{VILLAGE_LAYOUT}"
        if not any(e["element"].get("location") == loc for e in d["elements"]):
            d["elements"].append({"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": loc,
                                                           "projection": "rigid", "processors": "minecraft:empty"}})
    h.patch_json(f"{D}/worldgen/template_pool/guh_village/start.json", pool)


# =====================================================================================================================
# the game test rooms
# =====================================================================================================================
def test_templates(h):
    mc = h.mc
    # the oefenterrein: two pits and the guhtje's spot on a branch
    s = h.Structure((16, 8, 16))
    for x in range(16):
        for z in range(16):
            s.set(x, 0, z, mc("grass_block"), {"snowy": "false"})
    s.set(5, 1, 5, "guhs:beroepen_marshmallowvuur", {"vuur": "0"})
    s.set(9, 1, 5, "guhs:beroepen_marshmallowvuur", {"vuur": "0"})
    for y in range(1, 4):
        s.set(12, y, 12, mc("oak_log"), {"axis": "y"})
    s.set(11, 3, 12, mc("oak_log"), {"axis": "x"})
    s.set(11, 4, 12, "guhs:beroepen_guhtjeplek")
    s.save("beroepen_test_brandweer")
    # the Knabbeldief's trail: the kluis in one corner, a hiding place in the other, a wall to walk round
    s = h.Structure((24, 6, 24))
    for x in range(24):
        for z in range(24):
            s.set(x, 0, z, mc("smooth_stone"))
    for x in range(0, 19):
        for y in (1, 2):
            s.set(x, y, 11, mc("stone_bricks"))
    s.set(2, 1, 2, "guhs:beroepen_kluisplek")
    s.set(20, 1, 20, "guhs:beroepen_verstopplek")
    s.save("beroepen_test_politie")
    # the apotheek: a snotkruid plant, the mengketel
    s = h.Structure((12, 6, 12))
    for x in range(12):
        for z in range(12):
            s.set(x, 0, z, mc("grass_block"), {"snowy": "false"})
    s.set(6, 0, 6, mc("coarse_dirt"))
    s.set(6, 1, 6, "guhs:beroepen_snotkruid", {"age": "3"})
    s.set(3, 1, 8, "guhs:beroepen_mengketel", {"facing": "south"})
    s.save("beroepen_test_apotheek")
    # Bob's roof: a pillar with a ring of eight ghost tiles round a glass dakraam
    s = h.Structure((14, 8, 14))
    for x in range(14):
        for z in range(14):
            s.set(x, 0, z, mc("smooth_stone"))
    for y in range(1, 4):
        s.set(7, y, 7, mc("stone_bricks"))
    for x in range(6, 9):
        for z in range(6, 9):
            s.set(x, 4, z, mc("glass") if (x, z) == (7, 7) else "guhs:beroepen_dakplek")
    s.save("beroepen_test_bouw")


# =====================================================================================================================
def selfcheck(h):
    A = h.A
    missing = []
    for kind in KINDS:
        for f in (f"{A}/geckolib/models/entity/guh_npc_{kind}.geo.json", f"{A}/textures/entity/npc_{kind}.png"):
            if not os.path.exists(f):
                missing.append(f)
    for f in (f"{A}/geckolib/models/entity/knabbeldief_mika.geo.json", f"{A}/textures/entity/knabbeldief_mika.png"):
        if not os.path.exists(f):
            missing.append(f)
    for files in SOUNDS.values():
        for f in files:
            if not os.path.exists(f"{A}/sounds/beroepen/{f}.ogg"):
                missing.append(f"sound {f} (run tools/features/beroepen_geluid.py)")
    for b in ("beroepen_marshmallowvuur", "beroepen_guhtjeplek", "beroepen_kluisplek", "beroepen_verstopplek", "beroepen_pootafdruk",
              "beroepen_knabbelbuit", "beroepen_snotkruid", "beroepen_mengketel", "beroepen_dakplek", "beroepen_dakpan"):
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if f"block.guhs.{b}" not in T:
            missing.append(f"name of {b}")
    for i in ("guh_brandslang", "kaasmelkdrankje", "snotkruidje", "beroepen_dakpan", "beroepen_marshmallowvuur", "beroepen_mengketel",
              "beroepen_knabbelbuit"):
        if not os.path.exists(f"{A}/models/item/{i}.json"):
            missing.append(f"item model {i}")
    for kind in KINDS:
        for k in (f"entity.guhs.guh_npc.{kind}", f"gui.guhs.guhdex.rarity.{kind}", f"gui.guhs.guhdex.info.{kind}"):
            if k not in T:
                missing.append(k)
    # every text key the Java code uses is here (keys completed in the code - bedankt + n, naam. + id - by their prefix)
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "beroepen")
    used = set()
    for f in os.listdir(java):
        if f.endswith(".java") and "GameTests" not in f:
            used |= set(re.findall(r'"((?:quest|gui|item|entity)\.guhs\.[a-z0-9_.]+)"', open(os.path.join(java, f), encoding="utf-8").read()))
    for k in sorted(used):
        if k not in T and not any(x.startswith(k) for x in T):
            missing.append(f"text {k}")
    if missing:
        raise SystemExit(f"beroepen: missing {missing}")


def build(h):
    tex.build(h)
    T.clear()
    for kind, (name, rarity, info) in NPCS.items():
        t(f"entity.guhs.guh_npc.{kind}", name)
        t(f"gui.guhs.guhdex.rarity.{kind}", "Zeldzaamheid: " + rarity)
        t(f"gui.guhs.guhdex.info.{kind}", info)
    texts()
    for k, v in T.items():
        h.lang(k, v, v)
    advancements(h)
    data(h)
    # the Beroepenstraat (checked, saved) and Bob's bouwplaats in the third village layout
    b = straat.build(h)
    s = h.ms.guh_village(VILLAGE_LAYOUT.split("/")[1], 3, roof="light_blue_terracotta", bouwplaats=bouwplaats.bouwplaats)
    problems = bouwplaats.check(s)
    if problems:
        raise SystemExit("guh_village/layout_c (bouwplaats) check failed:\n  " + "\n  ".join(problems))
    test_templates(h)
    selfcheck(h)
    print(f"beroepen: Beroepenstraat ok ({len(b.s.blocks)} blocks, {b.walkable} walkable, {b.gezichten} + {b.grote_gezichten} faces), "
          f"bouwplaats ok ({len(bouwplaats.dakplekken(s))} dakplekken)")


# =====================================================================================================================
# FTB quests (the Knuffeldal chapter, section "De beroepen"); nothing is locked
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("beroepen_straat", "De Beroepenstraat", "Aan de oostkant van het &dKnuffeldal-stadje&r loopt de &6Beroepenstraat&r: de "
      "Brandweerkazerne (met een dak als een brandweerhelm), het Politiebureautje (met een dak als een politiepet) en het Apotheekje "
      "(met een groen kruis). Zeg hallo tegen Brandweercommandant Blusguh!", "guhs:firefighter_helmet", [fq.adv("seen_brandweerguh")],
      rewards=(("guhs:kaas_knabbels", 6),), shape="circle", xp=100)
    q("beroepen_brandweer", "Tatuu-tatuu!", "Help &6Brandweercommandant Blusguh&r: spuit met de guh-brandslang (rechtsklik ingedrukt) "
      "alle te vadsige marshmallowvuurtjes op het oefenterrein uit, en haal daarna het klimguhtje uit de hoge boom (via de ladder). "
      "Je krijgt de brandweerhelm en de brandweerjas.", "guhs:guh_brandslang", [fq.adv("beroepen_brandweer")],
      rewards=(("guhs:kaas_knabbels", 12),), xp=200)
    q("beroepen_vahoegsma", "Inspecteur Vahoegsma", "Het Politiebureautje staat midden in de Beroepenstraat, met een blauwe lamp bij "
      "de deur. Zeg hallo tegen &6Inspecteur Vahoegsma&r.", "guhs:police_cap", [fq.adv("seen_politieguh")],
      rewards=(("guhs:kaas_knabbels", 6),), xp=100)
    q("beroepen_politie", "De Knabbeldief-zaak", "De knabbelkluis van het stadje is leeg! Volg de roze pootafdrukken vanaf de kluis, "
      "vind de zak kaasknabbels (de Knabbeldief rent giechelend weg) en vertel het aan Vahoegsma. Je krijgt de politiepet en het "
      "uniform.", "guhs:beroepen_knabbelbuit", [fq.adv("beroepen_politie")], rewards=(("guhs:kaas_knabbels", 12),), xp=200)
    q("beroepen_snotneus", "Dokter Snotneus-guh", "In het Apotheekje met het groene kruis staat &6Dokter Snotneus-guh&r achter de balie. "
      "Hatsjoe!", "guhs:stethoscope", [fq.adv("seen_apothekerguh")], rewards=(("guhs:kaas_knabbels", 6),), xp=100)
    q("beroepen_apotheek", "Hatsjoe... VAHOEG!", "Pluk 3 snotkruidjes in de kruidentuin, meng ze met kaasmelk in de mengketel en geef "
      "het kaasmelkdrankje aan Snotje. Je krijgt de doktersjas en de stethoscoop.", "guhs:kaasmelkdrankje", [fq.adv("beroepen_apotheek")],
      rewards=(("guhs:kaas_knabbels", 12),), xp=200)
    q("beroepen_bob", "Bob de Guhbouwer", "In sommige &dguh-dorpen&r (Superkompas: Wonen) staat een half huisje met een bouwkraantje "
      "ernaast. Daar werkt &6Bob de Guhbouwer&r. Kunnen wij het maken?", "guhs:builder_helmet", [fq.adv("seen_bouwvakkerguh")],
      rewards=(("guhs:kaas_knabbels", 6),), xp=100)
    q("beroepen_bouw", "De vlag in top!", "Breng Bob 16 planken en 8 kaasknabbels (zijn lunch), klim dan op het dak en leg de dakpannen "
      "op alle doorzichtige dakplekjes. Je krijgt de bouwhelm en het veiligheidshesje.", "guhs:beroepen_dakpan", [fq.adv("beroepen_bouw")],
      rewards=(("guhs:kaas_knabbels", 12),), xp=200)
    q("beroepen_alle", "Guh van alle markten", "Leer alle vier de beroepen. Welke pet zet jij vandaag op?", "guhs:safety_vest",
      [fq.adv("beroepen_alle")], rewards=(("guhs:vahoege_vads_ingot", 1),), shape="gear", xp=400)
