"""1.8.0 effecttips: what an effect does, shown when you hover it in the inventory (feature/effecttips/client/EffectTips).

Every guhs effect gets effect.guhs.<id>.uitleg (taken from what its code does: see the effect classes named per line), the
vanilla effects get a short one too (effect.minecraft.<id>.uitleg). Dutch goes into nl_nl, English into en_us and into
tools/lang/en/c215_effecttips.json (so a full make_resources run keeps it). Run standalone from the guhs root:
    PYTHONPATH=tools python tools/features/effecttips.py
"""
import json
import os

GUHS = {  # id: (nl, en)   (source of the behaviour)
    # gatenkaas/StilEffect + Knabbelgeluid
    "stil": ("Je maakt geen enkel geluid: knabbelsensoren horen je niet lopen, eten of graven, en de Vadswaker kan je niet "
             "horen of ruiken. Sst... zelfs je buikje knort op fluistertoon.",
             "You make no sound at all: knabbel sensors don't hear you walk, eat or dig, and the Vadswaker can neither hear "
             "nor sniff you. Shh... even your tummy rumbles in a whisper."),
    # kaasmoeras/OnvahoegEffect
    "onvahoeg": ("Je voelt je even wat minder vahoeg: je loopt 12% langzamer en krijgt sneller honger. Nooit gevaarlijk, "
                 "wel een beetje sneu.",
                 "You feel a little less vahoeg for a while: you walk 12% slower and get hungry faster. Never dangerous, "
                 "just a bit sad."),
    # theehuis/TheeBlocks.Gezellig
    "gezellig": ("Elke 4 seconden word je een beetje beter (een half hartje, meer bij een sterker effect) en er zweven roze "
                 "hartjes om je heen. Thee met guhs, njeg!",
                 "Every 4 seconds you heal a little (half a heart, more when it is stronger) and pink hearts float around "
                 "you. Tea with guhs, njeg!"),
    # kamperen/UitgerustEffect + Band.klusSnelheid
    "uitgerust": ("Je loopt 10% sneller en als je gewond bent word je elke 5 seconden een half hartje beter. Een uitgeruste "
                  "guh doet zijn klusjes 15% sneller. VAHOEG uitgeslapen!",
                  "You walk 10% faster, and when you are hurt you heal half a heart every 5 seconds. A well-rested guh does "
                  "its chores 15% faster. VAHOEG well slept!"),
    # wereldleven/WereldlevenEffects.Blosjes
    "blosjes": ("Er zweven hartjes om je heen en af en toe word je een half hartje beter. Van een ijsje krijg je gewoon "
                "blosjes.",
                "Little hearts float around you, and now and then you heal half a heart. An ice cream just makes you blush."),
    # wereldleven/WereldlevenEffects.Zweverig
    "zweverig": ("De zwaartekracht is 60% minder: je springt hoger en zweeft zacht naar beneden, en vallen doet pas pijn "
                 "vanaf 16 blokken extra. Als een ijsjeswolkje!",
                 "Gravity is 60% lower: you jump higher and float gently down, and a fall only hurts after 16 extra "
                 "blocks. Like an ice-cream cloud!"),
    # piep/PiepEffecten.Besties
    "besties": ("Elke 2,5 seconde word je een beetje beter (een half hartje, meer bij een sterker effect) en af en toe "
                "verschijnt er een hartje. Beste vriendjes, net als Schilly en zijn guh.",
                "Every 2.5 seconds you heal a little (half a heart, more when it is stronger) and now and then a heart "
                "appears. Best friends, just like Schilly and its guh."),
    # piep/PiepEffecten.FrisVanBinnen
    "fris_van_binnen": ("Je loopt 25% sneller en je glinstert af en toe. Poepschilly heeft goed gepoetst!",
                        "You walk 25% faster and sparkle now and then. Poepschilly did a fine scrubbing job!"),
    # piep/PiepEffecten.LiefKijken
    "lief_kijken": ("Guhs binnen 8 blokken maken hartjes, en met een kaasknabbel heb je extra kans om een wilde guh meteen te "
                    "temmen. Muisjes temmen gaat makkelijker, en Poepschilly en guhxolotls laten zich meteen temmen. Wie kan zo'n "
                    "snoetje weerstaan?",
                    "Guhs within 8 blocks make hearts, and a kaasknabbel gets an extra chance to tame a wild guh right "
                    "away. Taming muisjes is easier, and Poepschilly and guhxolotls let themselves be tamed at once. Who can "
                    "resist that little face?"),
    # sausdieren/SausdierenEvents.onFall
    "sausdieren_stuiter": ("Vallen doet geen pijn, en val je meer dan anderhalf blok, dan stuiter je weer omhoog. Boing, vads!",
                           "Falling doesn't hurt, and when you fall more than a block and a half you bounce back up. Boing, vads!"),
    # torenpeper/PeperItems.Peperadem
    "torenpeper_peperadem": ("Bij elke ademhaling komt er een klein vlammetje uit je mond, en je kunt niet bevriezen. Je steekt "
                             "er niks mee in brand, beloofd.",
                             "A tiny flame comes out with every breath, and you can't freeze. It sets nothing on fire, "
                             "promise."),
    # einde2/mikas/HikEffect
    "hik": ("Ongeveer elke anderhalve seconde een hikje: een klein sprongetje en een piepend 'hik!'. Helemaal ongevaarlijk, "
            "wel een beetje gek.",
            "About every second and a half a hiccup: a little hop and a squeaky 'hic!'. Completely harmless, just a bit "
            "silly."),
    # blub/klus/FrisEnBlijEffect
    "fris_en_blij": ("Elke paar seconden een hartje en af en toe een blij dansje. Verder niks: gewoon fris en blij na een "
                     "bubbelbad!",
                     "Every few seconds a heart and now and then a happy little dance. Nothing more: just fresh and happy "
                     "after a bubble bath!"),
    # zingendeweide/Bubbelsap.Bubbelig
    "bubbelig": ("Je springt iets hoger, je valt iets zachter (2 blokken extra zonder valschade) en er knappen belletjes om "
                 "je heen. Ongevaarlijk en heel bubbelig.",
                 "You jump a bit higher, you land a bit softer (2 extra blocks without fall damage) and little bubbles "
                 "pop around you. Harmless and very bubbly."),
}

ONBEKEND = ("Een guh-effect. Wat het precies doet? Dat houdt de guh nog even geheim, njeg!",
            "A guh effect. What it does exactly? The guh keeps that secret for now, njeg!")

VANILLA = {  # id: (nl, en)
    "speed": ("Je loopt sneller (20% per niveau).", "You walk faster (20% per level)."),
    "slowness": ("Je loopt langzamer (15% per niveau).", "You walk slower (15% per level)."),
    "haste": ("Je hakt en slaat sneller.", "You mine and attack faster."),
    "mining_fatigue": ("Je hakt en slaat veel langzamer.", "You mine and attack much slower."),
    "strength": ("Je slaat harder (+3 schade per niveau).", "You hit harder (+3 damage per level)."),
    "instant_health": ("Je geneest meteen.", "You heal at once."),
    "instant_damage": ("Je krijgt meteen schade.", "You take damage at once."),
    "jump_boost": ("Je springt hoger en valt zachter.", "You jump higher and land softer."),
    "nausea": ("Je beeld wiebelt en draait.", "Your view wobbles and spins."),
    "regeneration": ("Je geneest langzaam vanzelf.", "You slowly heal by yourself."),
    "resistance": ("Je krijgt minder schade (20% per niveau).", "You take less damage (20% per level)."),
    "fire_resistance": ("Vuur en lava doen je geen pijn.", "Fire and lava don't hurt you."),
    "water_breathing": ("Je kunt onder water ademen.", "You can breathe under water."),
    "invisibility": ("Je bent onzichtbaar, maar je harnas en spullen niet.", "You are invisible, but your armour and items are not."),
    "blindness": ("Je ziet bijna niks en kunt niet sprinten.", "You can barely see and can't sprint."),
    "night_vision": ("Je ziet alles in het donker.", "You see everything in the dark."),
    "hunger": ("Je krijgt sneller honger.", "You get hungry faster."),
    "weakness": ("Je slaat zachter (-4 schade per niveau).", "You hit softer (-4 damage per level)."),
    "poison": ("Je verliest langzaam leven, maar gaat er niet dood aan.", "You slowly lose health, but it won't kill you."),
    "wither": ("Je verliest langzaam leven, en daar kun je wel dood aan gaan.", "You slowly lose health, and this one can kill you."),
    "health_boost": ("Je krijgt extra hartjes erbij (2 per niveau).", "You get extra hearts (2 per level)."),
    "absorption": ("Gouden extra hartjes die eerst de schade opvangen.", "Golden extra hearts that take the damage first."),
    "saturation": ("Je honger wordt meteen aangevuld.", "Your hunger is filled up at once."),
    "glowing": ("Je hebt een gloeiende rand die iedereen door muren heen ziet.", "You have a glowing outline everyone sees through walls."),
    "levitation": ("Je zweeft langzaam omhoog.", "You slowly float upwards."),
    "luck": ("Iets meer kans op goede buit uit kisten en bij het vissen.", "A bit more chance on good loot from chests and fishing."),
    "unluck": ("Iets minder kans op goede buit.", "A bit less chance on good loot."),
    "slow_falling": ("Je valt heel langzaam en krijgt geen valschade.", "You fall very slowly and take no fall damage."),
    "conduit_power": ("Onder water kun je ademen, zie je beter en hak je sneller.", "Under water you can breathe, see better and mine faster."),
    "dolphins_grace": ("Je zwemt veel sneller.", "You swim much faster."),
    "bad_omen": ("Een slecht voorteken: ga je hiermee een dorp of een trial chamber in, dan wordt het spannend.",
                 "A bad omen: walk into a village or a trial chamber with it and things get exciting."),
    "hero_of_the_village": ("Dorpelingen geven je korting en cadeautjes.", "Villagers give you discounts and gifts."),
    "darkness": ("Het wordt om je heen af en toe heel donker.", "Now and then it gets very dark around you."),
    "trial_omen": ("Trial spawners worden spannender: zwaardere vijanden, betere buit.",
                   "Trial spawners get tougher: stronger enemies, better loot."),
    "raid_omen": ("Zo meteen begint er een overval op dit dorp.", "A raid on this village starts any moment now."),
    "wind_charged": ("Ga je dood, dan barst er een windstoot uit je.", "When you die, a gust of wind bursts out of you."),
    "weaving": ("Ga je dood, dan spin je spinnenwebben om je heen.", "When you die, you spin cobwebs around you."),
    "oozing": ("Ga je dood, dan komen er slijmpjes uit je.", "When you die, little slimes come out of you."),
    "infested": ("Krijg je schade, dan kunnen er zilvervisjes uit je kruipen.", "When you are hurt, silverfish may crawl out of you."),
    "breath_of_the_nautilus": ("Onder water raak je je adem niet kwijt.", "Under water you don't run out of breath."),
}


def teksten():
    t = {f"effect.guhs.{k}.uitleg": v for k, v in GUHS.items()}
    t["effect.guhs.uitleg.onbekend"] = ONBEKEND
    t.update({f"effect.minecraft.{k}.uitleg": v for k, v in VANILLA.items()})
    return t


def _direct():
    t = teksten()
    en_pad = os.path.join("tools", "lang", "en", "c215_effecttips.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in t.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in t.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"effecttips: {len(GUHS)} guhs effects, {len(VANILLA)} vanilla effects, {len(t)} texts")


if __name__ == "__main__":
    _direct()
