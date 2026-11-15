"""
1.10.4 "Ambachten in het verhaal" (ONTWERP_1104 phase 2, slice E): Meester Knutsel and the Ambachtseiland, and two craft steps per
zone in the main story. Java: feature/vadsrijk/ambacht/AmbachtVerhaal.java (chains, gate, migration, story recipes) and
ambacht/EilandRegistratie.java (the islet NPCs, gathering groups, info boards, the zone 2/3 hub masters).

One source for:
  - the craft-step quests -> data/guhs/vadsrijk/npc/quests_ambacht.json (read by Quests.java AFTER every zone file: each quest goes
    into the questlog right after "voeg_na", and "na_patch" points the quest that used to follow at the new chain);
  - every text of it (Dutch) + the English overlay tools/lang/en/c481_vr_ambacht_verhaal.json;
  - the story items (vanilla textures), the hidden advancements guhs:quest/vr_<quest>.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_ambacht_verhaal.py
then: python tools/lang/apply_en.py --no-ftb   (en_us from nl_nl + the overlays)
"""
import json
import os
import re

P = "vadsrijk.npc."
A = "vadsrijk.ambacht."

TEKSTEN = {}   # key -> Dutch
EN = {}        # key -> English


# the English names of places and things (tools/lang/GLOSSARY_VADSRIJK.md), so the English lines can be written plainly
EN_NAMEN = [("the Hoogvads Craft Guild", "the Crafters' Guild of Highchonk"), ("Craft Guild", "Crafters' Guild"), ("Zwamzoet", "Shroomsbury"),
            ("Knuffelkamp", "Camp Snuggle"), ("Kandijkom", "Sugarbowl"), ("Honingtak", "Honeybough"), ("Hoogvads", "Highchonk"),
            ("Lampionhaven", "Lanternhaven"), ("Spore Wood", "Sporewood"), ("Cuddle Lake", "Snuggle Lake"), ("Glowbug Wood", "Glowbeetle Woods"),
            ("Comb Meadow", "Combmeadow"), ("Mushroom Temple", "Shroom Temple"), ("Granny Truus", "Granny Trudy"),
            ("Small Vads Potions", "Small Chonk Potions"), ("Small Vads Potion", "Small Chonk Potion"), ("clodder clover", "blob clover"),
            ("mushroom stalk", "shroom stem"), ("Mushroom stalk", "Shroom stem"), ("a mushroom stalk", "a shroom stem"),
            ("Just vads.", "Just chonky."), ("The vadsier", "The chonkier"), ("the Vadsrijk", "the Chonkdom")]


def t(k, nl, en):
    assert k not in TEKSTEN, k
    TEKSTEN[k] = nl
    for a, b in EN_NAMEN:
        en = en.replace(a, b)
    EN[k] = en


# ---------------------------------------------------------------------------------------------------------------------------
# NPCs: Meester Knutsel (a quest NPC of the npc slice, EilandRegistratie) + the workshop masters (AmbachtNpcs, role by id)
# ---------------------------------------------------------------------------------------------------------------------------
t(P + "naam.meester_knutsel", "Meester Knutsel", "Master Tinker")
t(P + "praat.meester_knutsel.1", "Een ambacht is een knuffel die je met je pootjes maakt.", "A craft is a cuddle you make with your paws.")
t(P + "praat.meester_knutsel.2", "Ik heb dit eiland zelf gebouwd. Nou ja, de brug dan. Nou ja, één plank.",
  "I built this island myself. Well, the bridge. Well, one plank.")
t(P + "praat.meester_knutsel.3", "Verzamelen, vissen, smeden, koken. Vier ambachten, één hart. En heel veel kruimels.",
  "Gathering, fishing, smithing, cooking. Four crafts, one heart. And lots of crumbs.")

# the workshop masters (the idle lines come from their role, like the hub masters: tools/features/vadsrijk_t_ambacht.py ROLLEN)
MEESTERS = {  # npc id: (Dutch name, English name)
    "amb_smid_eiland": ("Smidguh Vonkje", "Smithguh Sparky"),
    "amb_kok_eiland": ("Kok Proefje", "Cook Nibbles"),
    "amb_vis_eiland": ("Visguh Hengeltje", "Fishguh Rodney"),
    "amb_verzamel_eiland": ("Verzamelguh Plukje", "Gatherguh Pip"),
    "amb_verzamel_z2": ("Verzamelguh Sneeuwklokje", "Gatherguh Snowdrop"),
    "amb_vis_z2": ("Visguh Wakje", "Fishguh Icehole"),
    "amb_verzamel_z3": ("Imkerguh Zoemie", "Beeguh Buzzy"),
    "amb_vis_z3": ("Visguh Lantaarntje", "Fishguh Lanterny"),
}


def _rol_regels():
    """{rol: ([Dutch idle lines], [English idle lines])} from the zone-1 hub masters (same lines per role)."""
    try:
        from features import vadsrijk_t_ambacht as TA
    except ImportError:
        import vadsrijk_t_ambacht as TA
    en = {}
    d = os.path.join("tools", "lang", "en")
    for f in sorted(os.listdir(d)):
        if f.endswith(".json"):
            with open(os.path.join(d, f), encoding="utf-8") as fh:
                en.update(json.load(fh))
    uit = {}
    for rol in ("smid", "kok", "vis", "verzamel"):
        nl = list(TA.ROLLEN[rol][2])
        uit[rol] = (nl, [en.get(f"{P}praat.amb_{rol}_z1.{i + 1}", nl[i]) for i in range(len(nl))])
    return uit


for _npc, (_nl, _en) in MEESTERS.items():
    t(P + "naam." + _npc, _nl, _en)
_ROL_REGELS = _rol_regels()
for _npc in MEESTERS:
    _rol = _npc.split("_")[1]
    _r = _ROL_REGELS[_rol]
    for _i, (_a, _b) in enumerate(zip(*_r)):
        t(f"{P}praat.{_npc}.{_i + 1}", _a, _b)

# ---------------------------------------------------------------------------------------------------------------------------
# The islet's gathering groups (EilandRegistratie: AmbachtPlekken.groep, T1, 20-30 s)
# ---------------------------------------------------------------------------------------------------------------------------
GROEPEN = {
    "eiland_groeve": ("de Kiezelgroeve op het Ambachtseiland", "the Pebble Quarry on the Craft Island"),
    "eiland_bosje": ("het Knuffelbosje op het Ambachtseiland", "the Cuddle Grove on the Craft Island"),
    "eiland_bloemenwei": ("de Bloemenwei op het Ambachtseiland", "the Flower Meadow on the Craft Island"),
    "eiland_bijenhofje": ("het Bijenhofje op het Ambachtseiland", "the Bee Garden on the Craft Island"),
    "eiland_visvijver": ("de Visvijver op het Ambachtseiland", "the Fish Pond on the Craft Island"),
    "eiland_vissteiger": ("de Vissteiger van het Ambachtseiland", "the Fishing Pier of the Craft Island"),
}
for _id, (_nl, _en) in GROEPEN.items():
    t(A + "groep." + _id, _nl, _en)

# ---------------------------------------------------------------------------------------------------------------------------
# Info boards on the islet (guhs:infobord without a species: EilandRegistratie picks the text by the nearest place)
# ---------------------------------------------------------------------------------------------------------------------------
BORDEN = {
    "welkom": ("Welkom op het Ambachtseiland! Hier leer je de vier ambachten: Verzamelen, Vissen, Smeden en Koken. Meester Knutsel "
               "staat op het ronde plein. Alles groeit hier extra snel terug, dus oefen maar raak.",
               "Welcome to the Craft Island! Here you learn the four crafts: Gathering, Fishing, Smithing and Cooking. Master Tinker stands "
               "on the round square. Everything grows back extra fast here, so practise all you like."),
    "knutsel": ("Prikbord van Meester Knutsel: 1. Tik erts in de Kiezelgroeve (noordwest). 2. Pluk kruiden in de Bloemenwei (west). "
                "3. Vis in de Visvijver (zuidoost) of op de Vissteiger (ver in het westen). 4. Smeed in de Smederij, kook in de Keuken. "
                "5. Niet vergeten: af en toe een koekje.",
                "Master Tinker's notice board: 1. Tap ore in the Pebble Quarry (north-west). 2. Pick herbs in the Flower Meadow (west). "
                "3. Fish in the Fish Pond (south-east) or on the Fishing Pier (far west). 4. Smith in the Smithy, cook in the Kitchen. "
                "5. Don't forget: a biscuit now and then."),
    "smederij": ("De Smederij. Hier smeed je ringen, kettingen, oorbellen en armbanden, en je kiest zelf de bonussen. Erts en hout "
                 "haal je uit de groeve en het bosje. Klik op Smidguh Vonkje en kies 'Werkplaats'.",
                 "The Smithy. Here you forge rings, necklaces, earrings and bracelets, and you pick the bonuses yourself. Ore and wood come "
                 "from the quarry and the grove. Click Smithguh Sparky and choose 'Workshop'."),
    "keuken": ("De Keuken. Hier kook je eten met buffs en brouw je alle drankjes, ook het Klein Vadsdrankje. Kruiden uit de wei, vis "
               "uit de vijver. Klik op Kok Proefje en kies 'Werkplaats'.",
               "The Kitchen. Here you cook food with buffs and brew every potion, the Small Vads Potion too. Herbs from the meadow, fish "
               "from the pond. Click Cook Nibbles and choose 'Workshop'."),
    "viswinkel": ("De Viswinkel. Waar het water borrelt, zit vis. Rechtsklik op de borrelplek, wacht op 'Beet!' en klik dan meteen. "
                  "'s Nachts en als het regent bijten er andere vissen. Visguh Hengeltje vertelt je waar.",
                  "The Fish Shop. Where the water bubbles, there are fish. Right-click the bubbling spot, wait for 'Bite!' and click at "
                  "once. At night and in the rain other fish bite. Fishguh Rodney tells you where."),
    "verzamelhuisje": ("Het Verzamelhuisje. Rechtsklik op een glinsterend plekje: erts tik je, hout hak je, kruiden pluk je en bijen "
                       "geef je een beetje rook. Hoe vadsiger je het spelletje speelt, hoe mooier je oogst.",
                       "The Gathering Hut. Right-click a glittering spot: you tap ore, chop wood, pick herbs and give bees a puff of "
                       "smoke. The vadsier you play the little game, the finer your harvest."),
    "wegwijzer": ("Wegwijzer. Plein en Meester Knutsel: in het midden. Kiezelgroeve: noordwest. Knuffelbosje: noordoost. Bloemenwei: "
                  "west. Bijenhofje: zuidwest. Visvijver: zuidoost. Vissteiger: ver in het westen, bij de zee. Knuffelkamp: oost, over "
                  "de brug.",
                  "Signpost. Square and Master Tinker: in the middle. Pebble Quarry: north-west. Cuddle Grove: north-east. Flower Meadow: "
                  "west. Bee Garden: south-west. Fish Pond: south-east. Fishing Pier: far west, by the sea. Knuffelkamp: east, over the "
                  "bridge."),
}
t(A + "eiland.bord.naam", "Bordje op het Ambachtseiland", "Sign on the Craft Island")
for _id, (_nl, _en) in BORDEN.items():
    t(A + "eiland.bord." + _id, _nl, _en)

# ---------------------------------------------------------------------------------------------------------------------------
# Story items (AmbachtVerhaal.VERHAAL_ITEMS: guhs:vr_amb_verhaal_<id>) and their story recipes (Recepten.verhaalRecept)
# ---------------------------------------------------------------------------------------------------------------------------
VERHAAL_ITEMS = {  # id: (Dutch name, English name, vanilla texture, Dutch lore, English lore)
    "tempelsleutel": ("Tempelsleutel", "Temple Key", "minecraft:item/trial_key",
                      "Van zwamhout en pluisijzer. Past precies in de grote deur van de Zwamtempel.",
                      "Made of mushwood and fluff iron. Fits the great door of the Mushroom Temple exactly."),
    "warme_kandijsoep": ("Warme kandijsoep", "Warm Candy Soup", "minecraft:item/mushroom_stew",
                         "Tegen koude pootjes. Volgens Oma Truus ook tegen koude harten.",
                         "For cold paws. According to Granny Truus, for cold hearts too."),
    "lijmdrankje": ("Lijmdrankje", "Glue Potion", "minecraft:item/honey_bottle",
                    "Plakt alles. Ook je pootjes, dus niet proeven.", "Sticks everything. Your paws too, so no tasting."),
    "koraallampion": ("Koraallampion", "Coral Lantern", "minecraft:block/sea_lantern",
                      "Gloeit roze in het donker, zodat verdwaalde knuffels de weg naar huis vinden.",
                      "Glows pink in the dark, so lost cuddles find their way home."),
    "wolkendekentje": ("Wolkendekentje", "Cloud Blanket", "minecraft:block/white_wool",
                       "Zo zacht dat zelfs een walvis er nog vijf minuutjes onder wil.",
                       "So soft that even a whale wants five more minutes under it."),
    "kompaskoord": ("Sterrenzijden kompaskoord", "Star-silk Compass Cord", "minecraft:item/string",
                    "Een kompasnaald van de oude Sterrenguhs draait alleen aan een draadje dat glinstert als de nacht.",
                    "A compass needle of the old Star Guhs only turns on a thread that glitters like the night."),
}
RECEPT_NAMEN = {  # recipe id: (Dutch, English)
    "tempelsleutel": ("Tempelsleutel (verhaal)", "Temple Key (story)"),
    "warme_kandijsoep": ("Warme kandijsoep (verhaal)", "Warm Candy Soup (story)"),
    "lijmdrankje": ("Lijmdrankje (verhaal)", "Glue Potion (story)"),
    "koraallampion": ("Koraallampion (verhaal)", "Coral Lantern (story)"),
    "wolkendekentje": ("Wolkendekentje (verhaal)", "Cloud Blanket (story)"),
    "kompaskoord": ("Sterrenzijden kompaskoord (verhaal)", "Star-silk Compass Cord (story)"),
    "klein_vadsdrankje": ("Klein Vadsdrankje", "Small Vads Potion"),
}
for _id, (_nl, _en, _tex, _lnl, _len) in VERHAAL_ITEMS.items():
    t(f"item.guhs.vr_amb_verhaal_{_id}", _nl, _en)
    t(f"item.guhs.vr_amb_verhaal_{_id}.lore", _lnl, _len)
for _id, (_nl, _en) in RECEPT_NAMEN.items():
    t(A + "recept." + _id, _nl, _en)

# ---------------------------------------------------------------------------------------------------------------------------
# Messages (AmbachtVerhaal, ZonePoort, Werkplaats)
# ---------------------------------------------------------------------------------------------------------------------------
t(A + "verhaal.brief", "Meester Knutsel heeft je een brief gestuurd: 'Lieve avonturier, kom je de ambachten leren op het "
  "Ambachtseiland? Het lantaarnpad begint bij het kampvuur van Knuffelkamp. Neem je pootjes mee!'",
  "Master Tinker sent you a letter: 'Dear adventurer, will you come and learn the crafts on the Craft Island? The lantern path starts "
  "at the Knuffelkamp campfire. Bring your paws!'")
t(A + "verhaal.gemist", "Nieuw in het verhaal: ambachtstappen! Je hebt er %s gemist. Ze staan in je questlog (J). Je hebt ze nodig voor "
  "de volgende zone.", "New in the story: craft steps! You missed %s of them. They are in your quest log (J). You need them for the "
  "next zone.")
t(A + "werk.verhaal_niet", "Dit maak je alleen als het verhaal erom vraagt.", "You only make this when the story asks for it.")
t("vadsrijk.zonepoort.ambacht.titel", "Volgende: ambachtstap '%s'", "Next: craft step '%s'")
t("vadsrijk.zonepoort.eis.kaart_ambacht", "Doe eerst je ambachtstappen: nog %s te gaan (questlog, toets J)",
  "Do your craft steps first: %s to go (quest log, key J)")

# ---------------------------------------------------------------------------------------------------------------------------
# Tracker places (QuestPlek.POI aliases registered by AmbachtVerhaal: "poi:<id>")
# ---------------------------------------------------------------------------------------------------------------------------
PLEKKEN = {
    "ambacht_brug_kamp": ("aan het eind van het lantaarnpad, ten westen van het kampvuur van Knuffelkamp: over de brug naar het Ambachtseiland",
                          "at the end of the lantern path, west of the Knuffelkamp campfire: over the bridge to the Craft Island"),
    "ambacht_knutsel": ("op het ronde plein van het Ambachtseiland", "on the round square of the Craft Island"),
    "ambacht_groeve": ("in de Kiezelgroeve, in het noordwesten van het Ambachtseiland", "in the Pebble Quarry, in the north-west of the Craft Island"),
    "ambacht_bloemenwei": ("in de Bloemenwei, in het westen van het Ambachtseiland", "in the Flower Meadow, in the west of the Craft Island"),
    "ambacht_visvijver": ("bij de Visvijver, in het zuidoosten van het Ambachtseiland", "at the Fish Pond, in the south-east of the Craft Island"),
    "ambacht_smederij": ("in de Smederij aan het plein van het Ambachtseiland", "in the Smithy on the square of the Craft Island"),
    "ambacht_keuken": ("in de Keuken aan het plein van het Ambachtseiland", "in the Kitchen on the square of the Craft Island"),
    "amb_z1_hout": ("bij de stronken in het Sporenwoud, ten westen van Zwamzoet", "by the stumps in the Spore Wood, west of Zwamzoet"),
    "amb_z1_erts": ("bij de ertsaders in de Druipgrotten, in het noordwesten", "by the ore veins in the Drip Caves, in the north-west"),
    "amb_smidse_z1": ("bij de smid op het plein van Zwamzoet (of in de Smederij op het Ambachtseiland)",
                      "at the smith on the Zwamzoet square (or in the Smithy on the Craft Island)"),
    "amb_z2_vis": ("bij de borrelplekken in het Knuffelmeer", "at the bubbling spots in the Cuddle Lake"),
    "amb_keuken_z2": ("bij de kok op het plein van Kandijkom", "at the cook on the Kandijkom square"),
    "amb_z3_hout": ("bij de schemerbomen in het Lichtkeverbos", "by the dusk trees in the Glowbug Wood"),
    "amb_z3_korf": ("bij de bijenkorven op de Raatweide", "by the beehives on the Comb Meadow"),
    "amb_keuken_z3": ("bij de kok op de markt van Honingtak", "at the cook on the Honingtak market"),
    "amb_z4_duik": ("bij de duiksteiger in de Koraalbaai", "at the diving jetty in Coral Bay"),
    "amb_z5_wol": ("in het prieel van het Staartpark in Hoogvads", "in the arbour of the Tail Park in Hoogvads"),
    "amb_z5_erts": ("op de noordvin van Moeder Wolkwal, bij Hoogvads", "on Mother Cloudwhale's north fin, by Hoogvads"),
}
for _id, (_nl, _en) in PLEKKEN.items():
    t(P + "plek.poi." + _id, _nl, _en)
t(P + "plek.npc.meester_knutsel", "op het ronde plein van het Ambachtseiland, ten westen van Knuffelkamp (over de lantaarnbrug)",
  "on the round square of the Craft Island, west of Knuffelkamp (over the lantern bridge)")

# ---------------------------------------------------------------------------------------------------------------------------
# The quests. D(type, doel, n, nl, en, bron, plek) = an objective (Quest.Doel); "houd" as bron on a VERZAMEL objective = the items
# are only checked, not taken at hand-in (Questlog.voltooi), so the next step can use them.
# Speakers: "npc" (the quest NPC), "jij" (the player) or an NPC id. Each text is (nl, en).
# ---------------------------------------------------------------------------------------------------------------------------
QUESTS = []


def D(soort, doel, n, nl, en, bron="", plek=""):
    return dict(soort=soort, doel=doel, n=n, nl=nl, en=en, bron=bron, plek=plek)


def Q(id, voeg_na, gever, titel, tekst, doelen, aanbod, bezig, klaar, xp, munten, items=(), lv=1, na=(), inleveren=None,
      actie_aanname="", actie_klaar="", start_items=()):
    QUESTS.append(dict(id=id, voeg_na=voeg_na, gever=gever, titel=titel, tekst=tekst, doelen=doelen, aanbod=aanbod, bezig=bezig, klaar=klaar,
                       xp=xp, munten=munten, items=[list(i) for i in items], lv=lv, na=list(na), inleveren=inleveren or gever,
                       actie_aanname=actie_aanname, actie_klaar=actie_klaar, start_items=[list(i) for i in start_items]))


# The quest that used to follow each chain now waits for the chain's last step (players already past it: AmbachtVerhaal).
NA_PATCH = {
    "hq04_kampvuur": ["hq03f_maken"],
    "hq11_opperzwam": ["hq10b_tempelsleutel"],
    "z2_hq07_onder_het_ijs": ["z2_hq06b_kandijsoep"],
    "z3_hq08_werf": ["z3_hq07b_lijmdrankje"],
    "z4_hq06_feest": ["z4_hq05b_lampion"],
    "z5_hq09_slaaplied": ["z5_hq08b_dekentje"],
    "z6_hq08_sterrenwacht": ["z6_hq07b_kompaskoord"],
}

# ===== the Ambachtseiland lesson (zone 1 tutorial, between Opruimen and the campfire) ======================================
Q("hq03d_knutsel", "hq03c_opruimen", "kok",
  ("Meester Knutsel wacht", "Master Tinker is waiting"),
  ("Ten westen van Knuffelkamp ligt het Ambachtseiland. Daar leert Meester Knutsel je de vier ambachten: Verzamelen, Vissen, Smeden en "
   "Koken. Volg het lantaarnpad vanaf het kampvuur naar het westen, over de brug, en meld je bij hem op het ronde plein.",
   "West of Knuffelkamp lies the Craft Island. There Master Tinker teaches you the four crafts: Gathering, Fishing, Smithing and "
   "Cooking. Follow the lantern path west from the campfire, over the bridge, and report to him on the round square."),
  [D("gebied", "ambachtseiland", 1, "Loop over de lantaarnbrug naar het Ambachtseiland", "Walk over the lantern bridge to the Craft Island",
     plek="poi:ambacht_brug_kamp"),
   D("praat", "meester_knutsel", 1, "Praat met Meester Knutsel op het plein", "Talk to Master Tinker on the square", plek="npc:meester_knutsel")],
  [("npc", ("Opgeruimd staat netjes! Maar weet je wat nog netter staat? Zelfgemaakt.", "Tidied up is neat! But you know what's even neater? Home-made.")),
   ("npc", ("Zie je dat pad met de lantaarntjes? Dat gaat naar het Ambachtseiland. Daar woont Meester Knutsel. Hij leert iedereen "
            "verzamelen, vissen, smeden en koken.",
            "See that path with the little lanterns? It goes to the Craft Island. Master Tinker lives there. He teaches everyone to gather, "
            "fish, smith and cook.")),
   ("jij", ("Moet dat nu? Ik wilde net een koekje.", "Right now? I was just about to have a biscuit.")),
   ("npc", ("Juist nu! Wie zelf drankjes kan maken, valt minder vaak flauw. Ga maar, het kampvuur loopt niet weg. Ik wel, soms, naar de "
            "koekjestrommel.",
            "Right now! Whoever can make their own potions faints less often. Off you go, the campfire won't run away. I do, sometimes, "
            "to the biscuit tin."))],
  ("Volg het lantaarnpad naar het westen, over de brug. Meester Knutsel staat op het ronde plein.",
   "Follow the lantern path west, over the bridge. Master Tinker stands on the round square."),
  [("npc", ("Hallo, hallo! Een nieuwe leerling! Ik ben Meester Knutsel. Kijk eens om je heen: een groeve, een bosje, een wei, een "
            "vijver en vier werkhuisjes. Allemaal voor jou.",
            "Hello, hello! A new apprentice! I'm Master Tinker. Have a look around: a quarry, a grove, a meadow, a pond and four "
            "workshops. All for you.")),
   ("jij", ("Allemaal voor mij?", "All for me?")),
   ("npc", ("Nou ja, en voor de andere leerlingen. En voor de bijen. Maar vooral voor jou! Zullen we beginnen?",
            "Well, and for the other apprentices. And for the bees. But mostly for you! Shall we begin?"))],
  xp=40, munten=20, na=["hq03c_opruimen"], inleveren="meester_knutsel", actie_klaar="start:hq03e_verzamelen")

Q("hq03e_verzamelen", "hq03d_knutsel", "meester_knutsel",
  ("Tikken, plukken en vissen", "Tapping, picking and fishing"),
  ("Les één van Meester Knutsel: Verzamelen en Vissen. Rechtsklik op een glinsterend plekje en speel het spelletje: erts tik je in de "
   "Kiezelgroeve, kruiden pluk je in de Bloemenwei. Vis daarna een visje in de Visvijver. Op het eiland groeit alles extra snel terug.",
   "Master Tinker's lesson one: Gathering and Fishing. Right-click a glittering spot and play the little game: you tap ore in the "
   "Pebble Quarry and pick herbs in the Flower Meadow. Then catch a fish in the Fish Pond. On the island everything grows back extra fast."),
  [D("meld", "ambacht_oogst:erts", 2, "Tik 2 keer erts in de Kiezelgroeve", "Tap ore twice in the Pebble Quarry", plek="poi:ambacht_groeve"),
   D("meld", "ambacht_oogst:kruid", 1, "Pluk kruiden in de Bloemenwei", "Pick herbs in the Flower Meadow", plek="poi:ambacht_bloemenwei"),
   D("meld", "ambacht_oogst:vis", 1, "Vang een vis in de Visvijver", "Catch a fish in the Fish Pond", plek="poi:ambacht_visvijver")],
  [("npc", ("Les één! Verzamelen. Zie je die glinsterende plekjes? Rechtsklik erop. Erts: tik op het glinsterende. Kruiden: houd vast en "
            "laat los in het groen. Niet te vroeg, niet te laat. Precies vads.",
            "Lesson one! Gathering. See those glittering spots? Right-click them. Ore: tap the glittering bit. Herbs: hold and let go in "
            "the green. Not too early, not too late. Just vads.")),
   ("npc", ("Tik twee keer erts in de Kiezelgroeve en pluk een plukje in de Bloemenwei. En dan vissen! Rechtsklik op een borrelplek in de "
            "vijver, wacht op 'Beet!' en klik meteen.",
            "Tap ore twice in the Pebble Quarry and pick a little bunch in the Flower Meadow. And then fishing! Right-click a bubbling "
            "spot in the pond, wait for 'Bite!' and click at once.")),
   ("jij", ("En als ik te vroeg klik?", "And if I click too early?")),
   ("npc", ("Dan schrikt de vis en zwemt hij giechelend weg. Vissen zijn heel kietelig. Probeer het gewoon nog eens!",
            "Then the fish gets a fright and swims off giggling. Fish are very ticklish. Just try again!"))],
  ("Erts in de Kiezelgroeve, kruiden in de Bloemenwei, een vis in de Visvijver. Rechtsklik op de glinsterende plekjes!",
   "Ore in the Pebble Quarry, herbs in the Flower Meadow, a fish in the Fish Pond. Right-click the glittering spots!"),
  [("npc", ("Kijk nou! Erts, kruiden en een visje. En helemaal zelf! Ik moet even een traantje wegpinken.",
            "Look at that! Ore, herbs and a little fish. All by yourself! I need to wipe away a little tear.")),
   ("npc", ("Op dit eiland komt alles na een halve minuut terug. Elders in het Vadsrijk duurt dat wat langer: de natuur daar is "
            "minder ongeduldig dan ik.",
            "On this island everything comes back after half a minute. Elsewhere in the Vadsrijk it takes a bit longer: nature there is "
            "less impatient than me."))],
  xp=60, munten=30, na=["hq03d_knutsel"], actie_klaar="start:hq03f_maken")

Q("hq03f_maken", "hq03e_verzamelen", "meester_knutsel",
  ("Een ring en een drankje", "A ring and a potion"),
  ("Les twee van Meester Knutsel: Smeden en Koken. Hij gaf je wat erts, hout en kruiden. Smeed een ring in de Smederij (klik op "
   "Smidguh Vonkje en kies 'Werkplaats') en kook een Klein Vadsdrankje in de Keuken (bij Kok Proefje). Kom dan terug bij hem.",
   "Master Tinker's lesson two: Smithing and Cooking. He gave you some ore, wood and herbs. Forge a ring in the Smithy (click "
   "Smithguh Sparky and choose 'Workshop') and cook a Small Vads Potion in the Kitchen (at Cook Nibbles). Then come back to him."),
  [D("meld", "ambacht_maak:sieraad_ring", 1, "Smeed een ring in de Smederij", "Forge a ring in the Smithy", plek="poi:ambacht_smederij"),
   D("meld", "ambacht_maak:klein_vadsdrankje", 1, "Kook een Klein Vadsdrankje in de Keuken", "Cook a Small Vads Potion in the Kitchen",
     plek="poi:ambacht_keuken")],
  [("npc", ("Les twee! Maken. Hier: vijf pluisijzer, een zwamsteeltje en wat klodderklaver. Uit mijn eigen voorraadje. Niet tegen de "
            "bijen zeggen.",
            "Lesson two! Making. Here: five fluff iron, a mushroom stalk and some clodder clover. From my own little stash. Don't tell "
            "the bees.")),
   ("npc", ("In de Smederij smeed je een ring. Jij kiest zelf twee bonussen. Kracht? Leven? Allebei? Kies met je hart, en een beetje "
            "met je hoofd.",
            "In the Smithy you forge a ring. You choose two bonuses yourself. Strength? Life? Both? Choose with your heart, and a little "
            "with your head.")),
   ("npc", ("En in de Keuken kook je een Klein Vadsdrankje. Dat heelt je als je ergens te hard tegenaan bent geknuffeld. De winkels "
            "verkopen alleen die kleintjes. De grote maak je zelf!",
            "And in the Kitchen you cook a Small Vads Potion. It heals you when you've been cuddled a bit too hard by something. The "
            "shops only sell the small ones. The big ones you make yourself!"))],
  ("Een ring in de Smederij, een Klein Vadsdrankje in de Keuken. Klik op de meester in het huisje en kies 'Werkplaats'.",
   "A ring in the Smithy, a Small Vads Potion in the Kitchen. Click the master in the hut and choose 'Workshop'."),
  [("npc", ("Een echte zelfgesmede ring! En een drankje dat naar zondag ruikt. Je bent geen leerling meer. Nou ja, een beetje nog.",
            "A real home-forged ring! And a potion that smells like Sunday. You're no apprentice any more. Well, a little bit still.")),
   ("npc", ("Hier, nog wat Kleine Vadsdrankjes voor onderweg. Later in het verhaal heb je de ambachten echt nodig: een sleutel, een "
            "soepje, een drankje. Dan weet je hoe het moet!",
            "Here, some more Small Vads Potions for the road. Later in the story you will really need the crafts: a key, a soup, a potion. "
            "Then you'll know how!")),
   ("npc", ("Ga nu maar terug naar Kok Knapperkorst bij het kampvuur. Hij wacht met een koekje. Of hij heeft het opgegeten. Dat kan ook.",
            "Now go back to Cook Crispycrust at the campfire. He's waiting with a biscuit. Or he ate it. That could be too."))],
  xp=80, munten=60, items=[("guhs:vr_item_klein_vadsdrankje", 3)], na=["hq03e_verzamelen"],
  start_items=[("guhs:vr_amb_pluisijzer", 5), ("guhs:vr_amb_zwamsteel", 1), ("guhs:vr_amb_klodderklaver", 2)])

# ===== zone 1: the temple key (between De Zwamtempel and De Opperzwam) =====================================================
Q("hq10a_zwamhout", "hq10_zwamtempel", "tempelwachter_steeltje",
  ("Hout en ijzer voor een sleutel", "Wood and iron for a key"),
  ("De Mika's hebben het slot van de grote tempeldeur volgepropt met grauwe pluis. Tempelwachter Steeltje wil een nieuwe tempelsleutel. "
   "Daarvoor heb je zwamsteel nodig, het taaie zwamhout uit het Sporenwoud, en pluisijzer uit de druipsteenaders van de Druipgrotten.",
   "The Mikas stuffed the lock of the great temple door with grey fluff. Temple Guard Stalky wants a new temple key. For that you need "
   "mushroom stalk, the tough mushwood of the Spore Wood, and fluff iron from the dripstone veins of the Drip Caves."),
  [D("verzamel", "guhs:vr_amb_zwamsteel", 4, "Hak 4 zwamsteel in het Sporenwoud", "Chop 4 mushroom stalk in the Spore Wood", bron="houd",
     plek="poi:amb_z1_hout"),
   D("verzamel", "guhs:vr_amb_pluisijzer", 4, "Tik 4 pluisijzer in de Druipgrotten", "Tap 4 fluff iron in the Drip Caves", bron="houd",
     plek="poi:amb_z1_erts")],
  [("npc", ("Ik heb slecht nieuws. Ik keek door het sleutelgat van de grote deur, en het sleutelgat keek grauw terug.",
            "I have bad news. I looked through the keyhole of the great door, and the keyhole looked back, all grey.")),
   ("npc", ("Grauwe pluis! Helemaal volgepropt. Die Mika's. De oude sleutel krijgt hem niet meer open. We hebben een nieuwe nodig, "
            "een die door pluis heen klikt.",
            "Grey fluff! Stuffed full. Those Mikas. The old key won't open it any more. We need a new one, one that clicks right "
            "through fluff.")),
   ("jij", ("Waar maak je zo'n sleutel van?", "What do you make a key like that from?")),
   ("npc", ("Van zwamsteel uit het Sporenwoud en pluisijzer uit de Druipgrotten. Breng er van elk vier. Dan zeg ik je hoe je hem smeedt.",
            "From mushroom stalk out of the Spore Wood and fluff iron out of the Drip Caves. Bring four of each. Then I'll tell you how "
            "to forge it."))],
  ("Vier zwamsteel uit het Sporenwoud en vier pluisijzer uit de Druipgrotten. Houd ze bij je, je hebt ze nodig voor de sleutel.",
   "Four mushroom stalk from the Spore Wood and four fluff iron from the Drip Caves. Keep them with you, you need them for the key."),
  [("npc", ("Mooi spul! Dat zwamsteel ruikt nog naar bos, en dat pluisijzer glimt als een vers gepoetst kaasknabbeltje.",
            "Fine stuff! That mushroom stalk still smells of forest, and that fluff iron shines like a freshly polished cheese nibble.")),
   ("npc", ("Houd het maar bij je. Nu moet het nog gesmeed worden.", "Keep it with you. Now it still needs forging."))],
  xp=900, munten=400, lv=16, na=["hq10_zwamtempel"], actie_klaar="start:hq10b_tempelsleutel")

Q("hq10b_tempelsleutel", "hq10a_zwamhout", "tempelwachter_steeltje",
  ("De tempelsleutel", "The temple key"),
  ("Smeed de tempelsleutel bij een smid: in Zwamzoet of in de Smederij op het Ambachtseiland. Je hebt Smeden niveau 5 nodig. Breng de "
   "sleutel naar Tempelwachter Steeltje bij de grote deur.",
   "Forge the temple key at a smith: in Zwamzoet or in the Smithy on the Craft Island. You need Smithing level 5. Bring the key to "
   "Temple Guard Stalky at the great door."),
  [D("verzamel", "guhs:vr_amb_verhaal_tempelsleutel", 1, "Smeed de tempelsleutel bij een smid (Smeden 5)",
     "Forge the temple key at a smith (Smithing 5)", plek="poi:amb_smidse_z1")],
  [("npc", ("Een sleutel smeed je niet met je hoofd, maar met je pootjes. Ga naar een smid: in Zwamzoet, of in de Smederij van Meester "
            "Knutsel.",
            "You don't forge a key with your head, but with your paws. Go to a smith: in Zwamzoet, or in Master Tinker's Smithy.")),
   ("npc", ("Kies 'Tempelsleutel' in de werkplaats. Je moet wel Smeden niveau 5 zijn, anders wordt het een tempellepeltje.",
            "Choose 'Temple Key' in the workshop. You do need Smithing level 5, or it turns into a temple teaspoon."))],
  ("De tempelsleutel smeed je bij een smid (Smeden 5). Te laag? Smeed eerst een paar ringen, dan gaat het vanzelf.",
   "You forge the temple key at a smith (Smithing 5). Too low? Forge a few rings first, then it comes by itself."),
  [("npc", ("Laat zien... Klik! Hij past! Hoor je dat? De grauwe pluis knispert er gewoon vanaf.",
            "Let me see... Click! It fits! Hear that? The grey fluff just crackles right off.")),
   ("npc", ("Vanaf nu gaat de grote deur open voor wie sterk genoeg is. Ben je level 18? Praat dan nog eens met mij.",
            "From now on the great door opens for whoever is strong enough. Are you level 18? Then talk to me again."))],
  xp=1200, munten=600, lv=16, na=["hq10a_zwamhout"])

# ===== zone 2: soup for the frozen guard (between Versteend! and Onder het ijs) ============================================
Q("z2_hq06a_ijsforellen", "z2_hq06_versteend", "z2_visser",
  ("IJsforellen", "Ice trout"),
  ("Visser Snoek is weer vrij, maar zijn pootjes zijn nog ijskoud van het kristal. IJsvisser Wak weet raad: warme kandijsoep. Vang daarvoor "
   "5 spiegelforellen, de ijsforellen van het Knuffelmeer. Daarvoor heb je Vissen niveau 12 nodig.",
   "Fisher Pike is free again, but his paws are still ice-cold from the crystal. Ice Fisher Holey knows what to do: warm candy soup. "
   "Catch 5 mirror trout for it, the ice trout of the Cuddle Lake. You need Fishing level 12 for that."),
  [D("verzamel", "guhs:vr_amb_spiegelforel", 5, "Vang 5 spiegelforellen in het Knuffelmeer (Vissen 12)",
     "Catch 5 mirror trout in the Cuddle Lake (Fishing 12)", bron="houd", plek="poi:amb_z2_vis")],
  [("npc", ("Snoek klappertandt nog steeds. Klik-klak-klik. Hij zegt dat het muziek is, maar ik weet wel beter.",
            "Pike's teeth are still chattering. Click-clack-click. He says it's music, but I know better.")),
   ("npc", ("Wat hij nodig heeft is warme kandijsoep. Met ijsforel. De ijsforellen van het meer heten eigenlijk spiegelforellen: als je goed "
            "kijkt, zwaait je spiegelbeeld terug.",
            "What he needs is warm candy soup. With ice trout. The lake's ice trout are really called mirror trout: if you look closely, "
            "your reflection waves back.")),
   ("jij", ("Hoeveel heb je er nodig?", "How many do you need?")),
   ("npc", ("Vijf. Eén voor elk pootje, en één voor de smaak. Je hebt wel Vissen niveau 12 nodig, het zijn slimme visjes.",
            "Five. One for each paw, and one for the flavour. You do need Fishing level 12, they're clever little fish."))],
  ("Vijf spiegelforellen uit het Knuffelmeer. Vissen niveau 12, en geduld.", "Five mirror trout from the Cuddle Lake. Fishing level 12, and patience."),
  [("npc", ("Vijf! Ze glimmen als nieuwe ijsblokjes. Houd ze bij je.", "Five! They shine like new ice cubes. Keep them with you.")),
   ("npc", ("Nu moet er nog soep van gekookt worden. Ik kan alleen water koken, en dat brandt ook nog vaak aan.",
            "Now they still need cooking into soup. I can only boil water, and even that often burns."))],
  xp=450, munten=2500, lv=24, na=["z2_hq06_versteend"], actie_klaar="start:z2_hq06b_kandijsoep")

Q("z2_hq06b_kandijsoep", "z2_hq06a_ijsforellen", "z2_visser",
  ("Warme kandijsoep", "Warm candy soup"),
  ("Kook warme kandijsoep van de spiegelforellen in een keuken (Koken niveau 12), bijvoorbeeld bij de kok op het plein van Kandijkom. "
   "Breng de soep naar IJsvisser Wak, dan krijgt de bevroren wacht weer warme pootjes.",
   "Cook warm candy soup from the mirror trout in a kitchen (Cooking level 12), for instance at the cook on the Kandijkom square. Bring "
   "the soup to Ice Fisher Holey, and the frozen guard gets warm paws again."),
  [D("verzamel", "guhs:vr_amb_verhaal_warme_kandijsoep", 1, "Kook warme kandijsoep in een keuken (Koken 12)",
     "Cook warm candy soup in a kitchen (Cooking 12)", plek="poi:amb_keuken_z2")],
  [("npc", ("Oma Truus schreef het al: kandijsoep, tegen koude pootjes. Kies 'Warme kandijsoep' in een keuken. Koken niveau 12, hoor.",
            "Granny Truus wrote it down long ago: candy soup, for cold paws. Choose 'Warm Candy Soup' in a kitchen. Cooking level 12, mind you.")),
   ("npc", ("En niet onderweg oplepelen! Ik zie het aan je snorharen.", "And no sipping on the way! I'll see it on your whiskers."))],
  ("Warme kandijsoep, gekookt in een keuken (Koken 12). Bijvoorbeeld bij de kok op het plein van Kandijkom.",
   "Warm candy soup, cooked in a kitchen (Cooking 12). For instance at the cook on the Kandijkom square."),
  [("npc", ("Mmm, ruik dat! Snoek, kom eens hier. Slurp. Kijk, zijn oortjes worden alweer roze.",
            "Mmm, smell that! Pike, come here. Slurp. Look, his little ears are going pink again.")),
   ("z2_versteend_snoek", ("Dank je wel, avonturier. Ik voel mijn tenen weer. Alle zeven.", "Thank you, adventurer. I can feel my toes again. All seven.")),
   ("npc", ("Zeven? Daar hebben we het later nog wel over. Nu eerst dat geluid onder het ijs.",
            "Seven? We'll talk about that later. First that noise under the ice."))],
  xp=600, munten=3000, lv=24, na=["z2_hq06a_ijsforellen"])

# ===== zone 3: glue for the cable bridge (between Imkerdorp Zoemrust and De Kaaszeppelinwerf) ==============================
Q("z3_hq07a_hars", "z3_hq07_hangnest", "z3_lusje",
  ("Hars en honing", "Resin and honey"),
  ("De Mika's hebben de touwen van de kabelbrug naar de werf losgeknaagd. Kaartenmaakster Lusje weet een lijmdrankje dat alles weer vast "
   "plakt. Verzamel roze hars van de schemerbomen (Verzamelen 22) en schemerhoning van de bijenkorven op de Raatweide.",
   "The Mikas gnawed the ropes of the cable bridge to the shipyard loose. Mapmaker Loopy knows a glue potion that sticks everything back "
   "together. Gather pink resin from the dusk trees (Gathering 22) and dusk honey from the beehives on the Comb Meadow."),
  [D("verzamel", "guhs:vr_amb_roze_hars", 3, "Verzamel 3 roze hars van de schemerbomen (Verzamelen 22)",
     "Gather 3 pink resin from the dusk trees (Gathering 22)", bron="houd", plek="poi:amb_z3_hout"),
   D("verzamel", "guhs:vr_amb_schemerhoning", 2, "Verzamel 2 schemerhoning op de Raatweide", "Gather 2 dusk honey on the Comb Meadow",
     bron="houd", plek="poi:amb_z3_korf")],
  [("npc", ("Ik ging net kijken bij de kabelbrug naar de werf. De touwen hangen er als slappe spaghetti bij. Mika-tandjes, overal.",
            "I just went to look at the cable bridge to the shipyard. The ropes hang there like limp spaghetti. Mika teeth marks, everywhere.")),
   ("npc", ("Maar ik ken een lijmdrankje. Van roze hars en schemerhoning. Het plakt zo goed dat mijn kaarten nog steeds aan mijn tafel "
            "vastzitten. Al drie jaar.",
            "But I know a glue potion. From pink resin and dusk honey. It sticks so well that my maps are still stuck to my table. For "
            "three years now.")),
   ("jij", ("Waar vind ik dat?", "Where do I find those?")),
   ("npc", ("Hars huilen de schemerbomen, roze en blij. Honing halen de bijen op de Raatweide. Drie hars en twee honing, dat is genoeg.",
            "The dusk trees weep resin, pink and happy. The bees on the Comb Meadow make the honey. Three resin and two honey, that's enough."))],
  ("Drie roze hars van de schemerbomen en twee schemerhoning van de Raatweide. Houd ze bij je.",
   "Three pink resin from the dusk trees and two dusk honey from the Comb Meadow. Keep them with you."),
  [("npc", ("Het plakt al aan je pootjes, zie ik. Prima, dan is het goed spul.", "It's already sticking to your paws, I see. Good, then it's good stuff.")),
   ("npc", ("Nu nog brouwen. Dat moet in een keuken, anders wordt het kleverige thee.", "Now it still needs brewing. That has to be in a kitchen, or it turns into sticky tea."))],
  xp=3000, munten=3000, lv=35, na=["z3_hq07_hangnest"], actie_klaar="start:z3_hq07b_lijmdrankje")

Q("z3_hq07b_lijmdrankje", "z3_hq07a_hars", "z3_lusje",
  ("Het lijmdrankje", "The glue potion"),
  ("Brouw het lijmdrankje van de hars en de honing in een keuken (Koken niveau 22), bijvoorbeeld bij de kok op de markt van Honingtak. "
   "Breng het naar Kaartenmaakster Lusje, dan plakt ze de kabelbrug in de kruin weer vast.",
   "Brew the glue potion from the resin and the honey in a kitchen (Cooking level 22), for instance at the cook on the Honingtak market. "
   "Bring it to Mapmaker Loopy, and she sticks the cable bridge in the crown back together."),
  [D("verzamel", "guhs:vr_amb_verhaal_lijmdrankje", 1, "Brouw het lijmdrankje in een keuken (Koken 22)",
     "Brew the glue potion in a kitchen (Cooking 22)", plek="poi:amb_keuken_z3")],
  [("npc", ("Kies 'Lijmdrankje' in een keuken. Koken niveau 22. En roer linksom, rechtsom plakt de lepel vast.",
            "Choose 'Glue Potion' in a kitchen. Cooking level 22. And stir to the left, to the right the spoon gets stuck."))],
  ("Het lijmdrankje brouw je in een keuken (Koken 22), bijvoorbeeld op de markt van Honingtak.",
   "You brew the glue potion in a kitchen (Cooking 22), for instance at the Honingtak market."),
  [("npc", ("Perfect! Ik smeer de touwen in en... zie je dat? Strak als een trommelvel. De kabelbrug is weer heel.",
            "Perfect! I rub it into the ropes and... see that? Tight as a drum. The cable bridge is whole again.")),
   ("npc", ("En mijn kaart zit nu aan mijn pootje vast. Maakt niet uit. Dan raak ik hem tenminste niet kwijt. Op naar de werf!",
            "And now my map is stuck to my paw. Doesn't matter. At least I won't lose it. On to the shipyard!"))],
  xp=4000, munten=4000, lv=35, na=["z3_hq07a_hars"])

# ===== zone 4: a coral lantern for the party (between De Vliegervanger and the party) ======================================
Q("z4_hq05a_koraal", "z4_hq05_vliegervanger", "z4_opvang",
  ("Duiken naar koraal", "Diving for coral"),
  ("Voor het terug-naar-huis-feest wil Opvangmoeder Zachtje een koraallampion op de steiger, zodat verdwaalde knuffels altijd de weg "
   "naar huis vinden. Duik in de Koraalbaai naar roze koraal en zeesponsjes (Vissen niveau 32).",
   "For the home-coming party Foster Mom Softie wants a coral lantern on the jetty, so lost cuddles always find their way home. Dive in "
   "Coral Bay for pink coral and sea sponges (Fishing level 32)."),
  [D("verzamel", "guhs:vr_amb_roze_koraal", 4, "Duik 4 roze koraal op in de Koraalbaai (Vissen 32)",
     "Dive up 4 pink coral in Coral Bay (Fishing 32)", bron="houd", plek="poi:amb_z4_duik"),
   D("verzamel", "guhs:vr_amb_zeesponsje", 2, "Duik 2 zeesponsjes op in de Koraalbaai", "Dive up 2 sea sponges in Coral Bay", bron="houd",
     plek="poi:amb_z4_duik")],
  [("npc", ("Weet je wat ik wil voor het feest? Een lampion die nooit uitgaat. Van roze koraal. Dan vindt elke knuffel de weg naar huis, "
            "ook als ik slaap.",
            "Do you know what I want for the party? A lantern that never goes out. Made of pink coral. Then every cuddle finds its way "
            "home, even when I'm asleep.")),
   ("jij", ("Waar groeit dat koraal?", "Where does that coral grow?")),
   ("npc", ("Op de bodem van de Koraalbaai. Duiken dus! Vier stukjes koraal en twee zeesponsjes. Neem lucht mee. Zoveel als je kunt.",
            "On the bottom of Coral Bay. So: diving! Four bits of coral and two sea sponges. Take air with you. As much as you can."))],
  ("Vier roze koraal en twee zeesponsjes uit de Koraalbaai (Vissen 32). Houd ze bij je.",
   "Four pink coral and two sea sponges from Coral Bay (Fishing 32). Keep them with you."),
  [("npc", ("Wat glimt dat mooi! En de sponsjes piepen als je erin knijpt. Niet doen. Oké, één keer.",
            "How beautifully that shines! And the sponges squeak when you squeeze them. Don't. Okay, once.")),
   ("npc", ("Nu nog een lampion van smeden. In de werkplaats van de haven kan dat.", "Now it still needs forging into a lantern. The harbour workshop can do that."))],
  xp=4000, munten=5000, lv=53, na=["z4_hq05_vliegervanger"], actie_klaar="start:z4_hq05b_lampion")

Q("z4_hq05b_lampion", "z4_hq05a_koraal", "z4_opvang",
  ("De koraallampion", "The coral lantern"),
  ("Smeed de koraallampion in de werkplaats van Lampionhaven of in het Ambachtsgilde van Hoogvads (Smeden niveau 32). Breng hem naar "
   "Opvangmoeder Zachtje voor het feest.",
   "Forge the coral lantern in the Lampionhaven workshop or in the Hoogvads Craft Guild (Smithing level 32). Bring it to Foster Mom "
   "Softie for the party."),
  [D("verzamel", "guhs:vr_amb_verhaal_koraallampion", 1, "Smeed de koraallampion in de havenwerkplaats (Smeden 32)",
     "Forge the coral lantern in the harbour workshop (Smithing 32)", plek="npc:z4_werkplaats")],
  [("npc", ("Kies 'Koraallampion' in de werkplaats van de haven. Smeden niveau 32. En maak hem een beetje roze. Nog rozer. Ja, zo.",
            "Choose 'Coral Lantern' in the harbour workshop. Smithing level 32. And make it a bit pink. Pinker. Yes, like that."))],
  ("De koraallampion smeed je in de werkplaats van Lampionhaven (Smeden 32).", "You forge the coral lantern in the Lampionhaven workshop (Smithing 32)."),
  [("npc", ("Oh... Hij gloeit. Precies zoals ik had gedroomd. Ik hang hem aan het eind van de steiger.",
            "Oh... It glows. Just as I dreamed. I'll hang it at the end of the jetty.")),
   ("npc", ("Nu kan het feest beginnen. Kom, er is schelpenlimonade, en die is niet van mij, dus er zit geen zand in.",
            "Now the party can begin. Come, there's shell lemonade, and I didn't make it, so there's no sand in it."))],
  xp=5000, munten=6000, lv=53, na=["z4_hq05a_koraal"])

# ===== zone 5: a blanket for Moeder Wolkwal (between De Wolkwaldroom and the lullaby) ======================================
Q("z5_hq08a_wolkenwol", "z5_hq08_wolkwaldroom", "z5_koningin",
  ("Wolkenwol", "Cloud wool"),
  ("Wie wakker wordt, wil eerst nog vijf minuutjes onder een dekentje. Koningin Zachtmoed wil Moeder Wolkwal een wolkendekentje geven "
   "voordat je het slaaplied zingt. Pluk wolkenwol in de tuinen van Hoogvads (Verzamelen niveau 40).",
   "Whoever wakes up wants five more minutes under a blanket first. Queen Gentleheart wants to give Mother Cloudwhale a cloud blanket "
   "before you sing the lullaby. Pick cloud wool in the gardens of Hoogvads (Gathering level 40)."),
  [D("verzamel", "guhs:vr_amb_wolkenwol", 5, "Pluk 5 wolkenwol in de tuinen van Hoogvads (Verzamelen 40)",
     "Pick 5 cloud wool in the gardens of Hoogvads (Gathering 40)", bron="houd", plek="poi:amb_z5_wol")],
  [("npc", ("Wacht even. Voordat je zingt. Weet je wat ik altijd wil als ik wakker word? Nog vijf minuutjes. Onder een dekentje.",
            "Wait a moment. Before you sing. Do you know what I always want when I wake up? Five more minutes. Under a blanket.")),
   ("npc", ("Moeder Wolkwal heeft al honderd jaar geen dekentje gehad. Laten we er een maken. Van wolkenwol, het zachtste wat er is.",
            "Mother Cloudwhale hasn't had a blanket in a hundred years. Let's make her one. From cloud wool, the softest there is.")),
   ("jij", ("Een dekentje voor een walvis? Hoe groot wordt dat?", "A blanket for a whale? How big will that be?")),
   ("npc", ("Klein. Ze legt het op haar oog. Net als ik. Vijf plukjes wolkenwol uit de tuinen, alsjeblieft.",
            "Small. She puts it over her eye. Just like me. Five tufts of cloud wool from the gardens, please."))],
  ("Vijf wolkenwol uit de tuinen van Hoogvads (Verzamelen 40). Houd ze bij je.", "Five cloud wool from the gardens of Hoogvads (Gathering 40). Keep them with you."),
  [("npc", ("Zo zacht! Ik zou er zelf onder kruipen. Niet zeggen tegen de hofdames.", "So soft! I'd crawl under it myself. Don't tell the ladies of the court.")),
   ("npc", ("Nu moet het nog genaaid worden. In het Ambachtsgilde naaien ze alles. Zelfs knoopjes aan wolken.",
            "Now it still needs sewing. In the Craft Guild they sew everything. Even buttons on clouds."))],
  xp=1500, munten=8000, lv=59, na=["z5_hq08_wolkwaldroom"], actie_klaar="start:z5_hq08b_dekentje")

Q("z5_hq08b_dekentje", "z5_hq08a_wolkenwol", "z5_koningin",
  ("Het wolkendekentje", "The cloud blanket"),
  ("Naai het wolkendekentje in het Ambachtsgilde van Hoogvads, bij de leermeester van Smeden (Smeden niveau 40). Breng het naar "
   "Koningin Zachtmoed.",
   "Sew the cloud blanket in the Hoogvads Craft Guild, at the Smithing teacher (Smithing level 40). Bring it to Queen Gentleheart."),
  [D("verzamel", "guhs:vr_amb_verhaal_wolkendekentje", 1, "Naai het wolkendekentje in het Ambachtsgilde (Smeden 40)",
     "Sew the cloud blanket in the Craft Guild (Smithing 40)", plek="npc:z5_leermeester_smeden")],
  [("npc", ("Kies 'Wolkendekentje' in de werkplaats van het Gilde. Smeden niveau 40: bij ons smeden de naaisters ook.",
            "Choose 'Cloud Blanket' in the Guild workshop. Smithing level 40: our seamstresses are smiths too."))],
  ("Het wolkendekentje naai je in het Ambachtsgilde van Hoogvads (Smeden 40).", "You sew the cloud blanket in the Hoogvads Craft Guild (Smithing 40)."),
  [("npc", ("Het is perfect. Ik leg het straks zelf op haar oog. Ze zal het merken, ook al is ze zo groot als een stad.",
            "It's perfect. I'll lay it over her eye myself. She'll notice, even though she's as big as a city.")),
   ("npc", ("Ga nu maar. Zing het slaaplied, de goede kant op. Dan wordt ze wakker onder iets zachts.",
            "Go now. Sing the lullaby, the right way round. Then she wakes up under something soft."))],
  xp=2000, munten=10000, lv=59, na=["z5_hq08a_wolkenwol"])

# ===== zone 6: a star-silk cord for the compass (between De Zoutvlakte and De Verzonken Sterrenwacht) =======================
# (zone 6 has no gathering spots of its own yet (Zone6Haken): the threads come from Hoogvads; see the open questions)
Q("z6_hq07a_zijde", "z6_hq07_zoutvlakte", "z6_twinkel",
  ("Draad voor sterrenzijde", "Thread for star silk"),
  ("Het Sterrenkompas van de oude Sterrenguhs draait alleen aan een koord van sterrenzijde. Sterrenkijker Twinkel weet dat je dat spint "
   "van wolkenwol en hemelzilver uit Hoogvads (Verzamelen niveau 40).",
   "The Star Compass of the old Star Guhs only turns on a cord of star silk. Stargazer Twinkle knows you spin that from cloud wool and "
   "sky silver from Hoogvads (Gathering level 40)."),
  [D("verzamel", "guhs:vr_amb_wolkenwol", 3, "Pluk 3 wolkenwol in de tuinen van Hoogvads", "Pick 3 cloud wool in the gardens of Hoogvads",
     bron="houd", plek="poi:amb_z5_wol"),
   D("verzamel", "guhs:vr_amb_hemelzilver", 2, "Tik 2 hemelzilver op de vinnen van Moeder Wolkwal", "Tap 2 sky silver on Mother Cloudwhale's fins",
     bron="houd", plek="poi:amb_z5_erts")],
  [("npc", ("Het Sterrenkompas. Ik heb er tekeningen van. Zie je dit draadje? De naald hangt aan een koord van sterrenzijde. Zonder "
            "koord draait hij alleen maar rondjes. Net als ik zonder thee.",
            "The Star Compass. I have drawings of it. See this little thread? The needle hangs on a cord of star silk. Without the cord it "
            "just spins in circles. Like me without tea.")),
   ("jij", ("Waar vind je sterrenzijde?", "Where do you find star silk?")),
   ("npc", ("Je maakt het. Wolkenwol uit de tuinen van Hoogvads, met een draadje hemelzilver van de vinnen van Moeder Wolkwal. Dan "
            "glinstert het als de nacht.",
            "You make it. Cloud wool from the gardens of Hoogvads, with a thread of sky silver from Mother Cloudwhale's fins. Then it "
            "glitters like the night."))],
  ("Drie wolkenwol en twee hemelzilver uit Hoogvads (Verzamelen 40). Houd ze bij je.",
   "Three cloud wool and two sky silver from Hoogvads (Gathering 40). Keep them with you."),
  [("npc", ("Ja! Dat is het. Ik voel de sterren er al in kriebelen.", "Yes! That's it. I can already feel the stars tickling in it.")),
   ("npc", ("Nu nog smeden tot een koord. Dat kan alleen in het Ambachtsgilde van Hoogvads, daar hebben ze de fijnste naalden.",
            "Now it still needs forging into a cord. Only the Hoogvads Craft Guild can do that, they have the finest needles."))],
  xp=7000, munten=12000, lv=65, na=["z6_hq07_zoutvlakte"], actie_klaar="start:z6_hq07b_kompaskoord")

Q("z6_hq07b_kompaskoord", "z6_hq07a_zijde", "z6_twinkel",
  ("Het kompaskoord", "The compass cord"),
  ("Smeed het sterrenzijden kompaskoord in het Ambachtsgilde van Hoogvads, bij de leermeester van Smeden (Smeden niveau 48). Breng het "
   "naar Sterrenkijker Twinkel.",
   "Forge the star-silk compass cord in the Hoogvads Craft Guild, at the Smithing teacher (Smithing level 48). Bring it to Stargazer Twinkle."),
  [D("verzamel", "guhs:vr_amb_verhaal_kompaskoord", 1, "Smeed het kompaskoord in het Ambachtsgilde van Hoogvads (Smeden 48)",
     "Forge the compass cord in the Hoogvads Craft Guild (Smithing 48)", plek="npc:z5_leermeester_smeden")],
  [("npc", ("Kies 'Sterrenzijden kompaskoord' in het Gilde. Smeden niveau 48, het is heel fijn werk. Adem niet te hard, dan waait het weg.",
            "Choose 'Star-silk Compass Cord' at the Guild. Smithing level 48, it's very fine work. Don't breathe too hard, or it blows away."))],
  ("Het kompaskoord smeed je in het Ambachtsgilde van Hoogvads (Smeden 48).", "You forge the compass cord in the Hoogvads Craft Guild (Smithing 48)."),
  [("npc", ("Het glinstert als de nacht. Precies zoals op de tekening. Nu kan het kompas weer draaien.",
            "It glitters like the night. Just like in the drawing. Now the compass can turn again.")),
   ("npc", ("Op naar De Verzonken Sterrenwacht. Daar wacht het kompas op zijn koord.", "On to the Sunken Observatory. The compass is waiting there for its cord."))],
  xp=9000, munten=15000, lv=65, na=["z6_hq07a_zijde"])


# ---------------------------------------------------------------------------------------------------------------------------
# output
# ---------------------------------------------------------------------------------------------------------------------------
def _quest_teksten():
    for q in QUESTS:
        s = P + "q." + q["id"]
        t(s + ".titel", *q["titel"])
        t(s + ".tekst", *q["tekst"])
        t(s + ".bezig", *q["bezig"])
        for i, d in enumerate(q["doelen"]):
            t(s + f".doel.{i}", d["nl"], d["en"])
        for i, (_w, (nl, en)) in enumerate(q["aanbod"]):
            t(s + f".aanbod.{i}", nl, en)
        for i, (_w, (nl, en)) in enumerate(q["klaar"]):
            t(s + f".klaar.{i}", nl, en)


_quest_teksten()


def quests_json():
    ids = [q["id"] for q in QUESTS]
    assert len(ids) == len(set(ids)), "duplicate quest id"
    uit = []
    for q in QUESTS:
        for d in q["doelen"]:
            assert d["soort"] in ("gebied", "praat", "meld", "verzamel"), d
        uit.append(dict(id=q["id"], soort="hoofd", gever=q["gever"], inleveren=q["inleveren"], lv=q["lv"], na=q["na"], voeg_na=q["voeg_na"],
                        doelen=[dict(soort=d["soort"], doel=d["doel"], n=d["n"], bron=d["bron"], kans=0.0, **({"plek": d["plek"]} if d["plek"] else {}))
                                for d in q["doelen"]],
                        beloning=dict(xp=q["xp"], munten=q["munten"], items=q["items"], wapen=False, loot=""),
                        aanbod=[w for w, _x in q["aanbod"]], klaar=[w for w, _x in q["klaar"]], praat={}, actie_aanname=q["actie_aanname"],
                        actie_klaar=q["actie_klaar"], start_items=q["start_items"], auto=False))
    for volgende, na in NA_PATCH.items():
        assert all(n in ids for n in na), volgende
    return {"_": "generated by tools/features/vadsrijk_ambacht_verhaal.py; do not edit", "quests": uit, "na_patch": NA_PATCH}


def advancements():
    return {f"advancement/quest/vr_{q['id']}.json": {"criteria": {"done": {"trigger": "minecraft:impossible"}}} for q in QUESTS}


def _assets(w, a):
    for id, (_n, _e, tex, _l, _le) in VERHAAL_ITEMS.items():
        w(os.path.join(a, "items", f"vr_amb_verhaal_{id}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_amb_verhaal_{id}"}})
        w(os.path.join(a, "models", "item", f"vr_amb_verhaal_{id}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})


def _check():
    for k, v in TEKSTEN.items():
        for x in (v, EN[k]):
            assert not re.search(r"%(?!s|%|\d\$s)", x), f"stray % in {k}"
            assert x.count("%s") == v.count("%s"), f"placeholders differ in {k}"


def build(h):
    _check()
    h.w(f"{h.D}/vadsrijk/npc/quests_ambacht.json", quests_json())
    for pad, obj in advancements().items():
        h.w(f"{h.D}/{pad}", obj)
    _assets(h.w, h.A)
    for k, nl in TEKSTEN.items():
        h.lang(k, EN[k], nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


OVERLAY = os.path.join("tools", "lang", "en", "c481_vr_ambacht_verhaal.json")


def _direct():
    _check()
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "vadsrijk", "npc", "quests_ambacht.json"), quests_json())
    for pad, obj in advancements().items():
        _schrijf(os.path.join(d, *pad.split("/")), obj)
    _assets(_schrijf, os.path.join(a, "assets", "guhs"))
    p = os.path.join(a, "assets", "guhs", "lang", "nl_nl.json")
    with open(p, encoding="utf-8") as f:
        data = json.load(f)
    data.update(TEKSTEN)
    _schrijf(p, data)
    _schrijf(OVERLAY, dict(sorted(EN.items())))
    print(f"vadsrijk_ambacht_verhaal: {len(QUESTS)} quests, {len(TEKSTEN)} texts; now run python tools/lang/apply_en.py --no-ftb")


if __name__ == "__main__":
    _direct()
