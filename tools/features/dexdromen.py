"""1.8.0 Guhdromenland: the Guhdex pages of the dream creatures and the Blub-meterkaart (feature/dexdromen).

Writes the page texts (rarity + info) for the nine new pages, the meterkaart texts, the seen_<page> advancements and the two
meterkaart advancements (FTB: one row complete, a golden box). Dutch in nl_nl, English in en_us and in
tools/lang/en/c214_dexdromen.json (so a full make_resources run keeps it).

Standalone: PYTHONPATH=tools python tools/features/dexdromen.py
"""
import json
import os

PAGINAS = ["blubguh", "sterrenvisje", "snoes", "zandguh", "wekkerguh", "rommelguh", "droom_omaguh", "kussengevecht_guh", "bubbelbadguh"]

TEKSTEN = {
    # --- the Guhdex pages: rarity + info --------------------------------------------------------------------------------
    "gui.guhs.guhdex.rarity.blubguh": (
        "Zeldzaamheid: Zeldzaam (De Zingende Weide en Guhdromenland; wilde Blubs zie je pas als je Snoes hebt geholpen)",
        "Rarity: Rare (De Zingende Weide and Guh Dreamland; you only see wild Blubs once you helped Snoes)"),
    # (gui.guhs.guhdex.info.blubguh comes from tools/features/blub.py)
    "gui.guhs.guhdex.rarity.sterrenvisje": (
        "Zeldzaamheid: Zeldzaam (het maanmeertje van het Maanmolentje, in Guhdromenland)",
        "Rarity: Rare (the moon pond of the Moon Mill, in Guh Dreamland)"),
    "gui.guhs.guhdex.info.sterrenvisje": (
        "Een klein visje met sterretjes als vinnen. Het gloeit zachtjes in vier kleuren en laat glinsters achter. Ruikt naar "
        "sterrenstof. Niet voeren: het eet alleen dromen.",
        "A tiny fish with little stars for fins. It glows softly in four colours and leaves sparkles behind. Smells of star "
        "dust. Don't feed it: it only eats dreams."),
    "gui.guhs.guhdex.rarity.snoes": (
        "Zeldzaamheid: Uniek (bij je bed, op een avond; later op het Droomplein)",
        "Rarity: Unique (by your bed, one evening; later on the Dream Square)"),
    "gui.guhs.guhdex.info.snoes": (
        "Een verfomfaaide zwerfguh met een geknakt oortje en een pleister. Hij had nooit een eigen plekje en durfde daarom nooit "
        "te slapen. Na zijn droom slaapt hij in een doos met knabbels (niet in het mooie kattenhuisje, natuurlijk).",
        "A scruffy stray guh with a bent ear and a plaster. He never had a place of his own, so he never dared to sleep. After "
        "his dream he sleeps in a box of nibbles (not in the lovely cat house, of course)."),
    "gui.guhs.guhdex.rarity.zandguh": (
        "Zeldzaamheid: Uniek (het Maanmolentje, in Guhdromenland)",
        "Rarity: Unique (the Moon Mill, in Guh Dreamland)"),
    "gui.guhs.guhdex.info.zandguh": (
        "De guh die de dromen maalt tot droomzand. Slaapmuts, nachtjapon met sterretjes en een zak vol glitter. Hij strooit "
        "nooit te veel, want dan slaap je door je ontbijt heen. Vahoeg slaperig.",
        "The guh who grinds dreams into dream sand. Nightcap, starry nightgown and a sack full of glitter. He never sprinkles "
        "too much, or you'd sleep through breakfast. Very sleepy."),
    "gui.guhs.guhdex.rarity.wekkerguh": (
        "Zeldzaamheid: Uniek (het Droomplein, in Guhdromenland)",
        "Rarity: Unique (the Dream Square, in Guh Dreamland)"),
    "gui.guhs.guhdex.info.wekkerguh": (
        "Een guh met een wekker op zijn rug en belletjes aan zijn oortjes. Praat met hem en je wordt wakker. Hij rinkelt heel "
        "zachtjes, want niemand wordt graag hard wakker. Njeg... nog vijf minuutjes?",
        "A guh with an alarm clock on his back and little bells on his ears. Talk to him and you wake up. He rings very "
        "softly, because nobody likes waking up loudly. Njeg... five more minutes?"),
    "gui.guhs.guhdex.rarity.rommelguh": (
        "Zeldzaamheid: Uniek (het Herinneringseiland, in Guhdromenland)",
        "Rarity: Unique (the Memory Island, in Guh Dreamland)"),
    "gui.guhs.guhdex.info.rommelguh": (
        "Overal waar Rommelguh is geweest, staat een glas met een restje. Sap, cola, limonade... ze zijn nooit helemaal leeg. "
        "'Die is nog niet leeg!' zegt hij dan. Opruimen? Morgen. Misschien.",
        "Wherever Rommelguh has been, there's a glass with a little bit left. Juice, cola, lemonade... they're never quite "
        "empty. 'That one isn't finished!' he says. Tidying up? Tomorrow. Maybe."),
    "gui.guhs.guhdex.rarity.droom_omaguh": (
        "Zeldzaamheid: Uniek (het Herinneringseiland, in Guhdromenland)",
        "Rarity: Unique (the Memory Island, in Guh Dreamland)"),
    "gui.guhs.guhdex.info.droom_omaguh": (
        "Oma-guh zoals ze in de herinnering van de Guh-familiedag zit: met breinaalden, een bolletje wol en een trommel vol roze "
        "guh-koeken. 'Nog een koekje, vadsje?' Ja, oma.",
        "Grandma guh as she is in the memory of the Guh Family Day: with knitting needles, a ball of wool and a tin full of "
        "pink guh cookies. 'Another cookie, sweetie?' Yes, grandma."),
    "gui.guhs.guhdex.rarity.kussengevecht_guh": (
        "Zeldzaamheid: Zeldzaam (het Sterrenkussenveld, in Guhdromenland)",
        "Rarity: Rare (the Star Pillow Field, in Guh Dreamland)"),
    "gui.guhs.guhdex.info.kussengevecht_guh": (
        "Pyjama aan, slaapmutsje op en een kussen in de pootjes. Hij gooit zijn kussen naar iedereen die in de buurt komt. Het "
        "doet nooit pijn, alleen veertjes. Alleen zachte vads, njeg!",
        "Pyjamas on, nightcap on and a pillow in his paws. He throws his pillow at anyone who comes close. It never hurts, only "
        "feathers. Only soft fights, njeg!"),
    "gui.guhs.guhdex.rarity.bubbelbadguh": (
        "Zeldzaamheid: Zeldzaam (het badhuisje bij de Bubbelsap-poeltjes in De Zingende Weide)",
        "Rarity: Rare (the bath hut by the Bubble Sap Pools in De Zingende Weide)"),
    "gui.guhs.guhdex.info.bubbelbadguh": (
        "Badmeester Bubbels van de Bubbelsap-poeltjes. Hij deelt elke dag een handdoekje uit en houdt een oogje op de "
        "badderende Blubs. Hij ruikt altijd naar zeep en zingt onder het afdrogen.",
        "Lifeguard Bubbles of the Bubble Sap Pools. He hands out a little towel every day and keeps an eye on the bathing "
        "Blubs. He always smells of soap and sings while drying off."),
    # --- the meterkaart ---------------------------------------------------------------------------------------------------
    "gui.guhs.meterkaart.titel": ("Blub-meterkaart", "Blub Meter Map"),
    "gui.guhs.meterkaart.teller": ("%s van de %s vakjes gevuld", "%s of the %s boxes filled"),
    "gui.guhs.meterkaart.link": ("▦ Meterkaart: %s/%s (klik)", "▦ Meter map: %s/%s (click)"),
    "gui.guhs.meterkaart.terug": ("Terug", "Back"),
    "gui.guhs.meterkaart.as_blub": ("◀ meer Blub", "◀ more Blub"),
    "gui.guhs.meterkaart.as_guh": ("meer Guh ▶", "more Guh ▶"),
    "gui.guhs.meterkaart.vak": ("%s, meter %s-%s", "%s, meter %s-%s"),
    "gui.guhs.meterkaart.vak.puur": ("%s: een Pure Blub (meter precies 0)", "%s: a Pure Blub (meter exactly 0)"),
    "gui.guhs.meterkaart.vak.vol": ("%s: een Volle Guh (meter precies 100)", "%s: a Volle Guh (meter exactly 100)"),
    "gui.guhs.meterkaart.vak.ja": ("Gevuld! Je hebt er zo een getemd of gekweekt.", "Filled! You tamed or bred one like this."),
    "gui.guhs.meterkaart.vak.nee": ("Nog leeg: tem of kweek een Blub met deze soort en meter.",
                                    "Still empty: tame or breed a Blub of this kind and meter."),
    "gui.guhs.meterkaart.nieuw": ("Nieuw vakje op je Blub-meterkaart! (%s/%s)", "New box on your Blub meter map! (%s/%s)"),
    "gui.guhs.meterkaart.nieuw_goud": ("Een GOUDEN vakje op je Blub-meterkaart! (%s/%s)", "A GOLDEN box on your Blub meter map! (%s/%s)"),
}

ADVANCEMENTS = ["seen_" + p for p in PAGINAS] + ["blub_meterkaart_rij", "blub_meterkaart_goud"]


def build(h):
    """make_resources hook: the texts (the advancements are plain files, see _schrijf)."""
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    _schrijf()


def _schrijf():
    a = os.path.join("src", "main", "resources", "data", "guhs", "advancement", "quest")
    os.makedirs(a, exist_ok=True)
    for n in ADVANCEMENTS:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"criteria": {"done": {"trigger": "minecraft:impossible"}}}, f, indent=2)
            f.write("\n")


def selfcheck():
    problems = []
    for p in PAGINAS:
        for k in ("gui.guhs.guhdex.rarity." + p, "gui.guhs.guhdex.info." + p):
            if k not in TEKSTEN and k != "gui.guhs.guhdex.info.blubguh":   # (that one is blub.py's)
                problems.append("missing " + k)
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append("%s count differs: " + k)
    if problems:
        raise SystemExit("dexdromen self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    selfcheck()
    _schrijf()
    en_pad = os.path.join("tools", "lang", "en", "c214_dexdromen.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"dexdromen: {len(PAGINAS)} Guhdex pages, meterkaart, {len(ADVANCEMENTS)} advancements, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
