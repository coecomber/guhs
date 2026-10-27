"""
1.8.0 (Guhdromenland), TEST BUILD: the Blub-guh (ontwerp-guhs180-dromen.md). Java: feature/blub (BlubFeature,
BlubGuhEntity, BlubTrekking, BlubMenu, BlubCommando, client/BlubClient + BlubMenuScreen).

  build(h)    the model, animations and textures (blub_modellen), the item icon, the spawn egg and pick-up item models,
              the sound, the TIJDELIJKE spawns (Wolkenweide + Bloemenvallei, until Guhdromenland exists), the texts (Dutch
              in nl_nl, English in tools/lang/en/c210_blub.json), the loot table (nothing) and a self-check
  preview     (python tools/features/blub.py preview <out>) offline renders of the 5 soorten x meter 0/25/50/75/100

Standalone (from the project root): python tools/features/blub.py   (writes only the Blub's own files, including their
26.1 shapes: the item definitions and the baked spawn egg; a full tools/mc26.py run is not needed)
"""
import json
import os
import sys

from PIL import Image, ImageDraw

try:
    from features import blub_modellen as modellen
    from features import blub_klusjes as klusjes
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import blub_modellen as modellen
    from features import blub_klusjes as klusjes

BONES = {}
CLOTHES = []

# TIJDELIJK (1.8.0 test build): the Blub-guh lives in the Wolkenweide and the Bloemenvallei until Guhdromenland exists; then
# only this entry moves to the dream dimension's biomes.
SPAWNS = {"blubguh_tijdelijk": (["guhs:wolkenweide", "guhs:bloemenvallei"], [("guhs:blubguh", 6, 2, 4)])}

# (the sounds: one event per action, tools/blub_audio_import.py fills them from inspiration/Blub audio/; its ACTIES give the
# subtitles below)

SOORT_NL = {"blauw": "Blauw", "weide": "Weide", "schild": "Schildje", "ravijn": "Ravijn", "zwam": "Zwam"}
SOORT_EN = {"blauw": "Blue", "weide": "Meadow", "schild": "Shell", "ravijn": "Ravine", "zwam": "Fungus"}
TINT_NL = {"blauw": ["normaal", "ijsblauw", "babyblauw"], "weide": ["normaal", "muntgroen", "lentegroen"],
           "schild": ["normaal", "perzik", "zeepbel"], "ravijn": ["normaal", "lavendel", "framboos"], "zwam": ["normaal", "vanille", "maanwit"]}
TINT_EN = {"blauw": ["normal", "ice blue", "baby blue"], "weide": ["normal", "mint green", "spring green"],
           "schild": ["normal", "peach", "soap bubble"], "ravijn": ["normal", "lavender", "raspberry"], "zwam": ["normal", "vanilla", "moon white"]}
TRUC_NL = ["Zitten! %s gaat netjes zitten (of staat weer op).", "Rondje! %s draait een rondje, wiee!",
           "Dood spelen! %s valt om... en staat weer op. Njeg, hij leeft!", "Hoge sprong! %s springt een salto!"]
TRUC_EN = ["Sit! %s sits down nicely (or gets up again).", "Spin! %s spins around, wheee!",
           "Play dead! %s flops over... and gets up again. Njeg, alive!", "High jump! %s does a somersault!"]
# its Trucjes screen (feature/blub/BlubTruc, in that order): id -> (name NL, EN, what it does NL, EN)
TRUCJES = {
    "zitten": ("Zitten", "Sit", "Zitten of weer opstaan (zoals Rondvadsen).", "Sit or get up again (like Rondvadsen)."),
    "rondje": ("Rondje draaien", "Spin", "Een rondje draaien. De guhs in de buurt juichen!", "A little spin. The guhs nearby cheer!"),
    "dood": ("Dood spelen", "Play dead", "Hij valt om... en staat weer op. De guhs juichen.", "It flops over... and gets up again. The guhs cheer."),
    "sprong": ("Hoge sprong", "High jump", "Een hoge sprong met een salto.", "A high jump with a somersault."),
    "hupsen": ("Hupsen!", "Hop hop!", "Hij hupst rondjes om je heen, steeds sneller. Record: %s sprongetjes.",
               "It hops in circles around you, faster and faster. Record: %s hops."),
    "slaapliedje": ("Slaapliedje piepen", "Squeak a lullaby", "Hij piept een liedje en de guhs in de buurt worden heel slaperig.",
                    "It squeaks a little tune and the guhs nearby get very sleepy."),
    "verstoppertje": ("Verstoppertje", "Hide and seek", "Hij verstopt zich ergens in de buurt. Vind hem binnen een minuut (rechtsklik)!",
                      "It hides somewhere nearby. Find it within a minute (right-click)!"),
    "squish": ("Squish!", "Squish!", "Een squish-knuffel: plat als een pudding, en weer omhoog.", "A squish hug: flat as a pudding, and up again."),
    "oor_blij": ("Oortjes: blij", "Ears: happy", "Oortjes rechtop, blij!", "Ears up, happy!"),
    "oor_plat": ("Oortjes: verlegen", "Ears: shy", "Oortjes plat, een beetje verlegen.", "Ears flat, a bit shy."),
    "oor_krul": ("Oortjes: nieuwsgierig", "Ears: curious", "Oortjes in een krul: wat is dat?", "Curled ears: what is that?"),
    "knuffel_guh": ("Knuffelen met een guh", "Hug a guh", "Hij zoekt de dichtstbijzijnde tamme guh (binnen 8 blokken) voor een dikke knuffel.",
                    "It looks for the nearest tame guh (within 8 blocks) for a big hug."),
}

# key -> (Dutch, English)
TEKSTEN = {
    "entity.guhs.blubguh": ("Blub", "Blub"),
    "entity.guhs.blubguh.puur": ("Pure Blub", "Pure Blub"),
    "entity.guhs.blubguh.vol": ("Volle Guh", "Full Guh"),
    "item.guhs.blubguh_item": ("Blub-guh", "Blub-guh"),
    "item.guhs.blubguh_item.tooltip": ("Zet hem neer met rechtsklik op de grond. Squish!", "Right-click the ground to put it down. Squish!"),
    "item.guhs.blubguh_spawn_egg": ("Blub-guh-spawnei", "Blub-guh Spawn Egg"),
    "gui.guhs.landdiertjes.getemd.blubguh": ("%s is nu jouw Blub-guh! Squish!", "%s is your Blub-guh now! Squish!"),
    "gui.guhs.piep.menu.sub.blubguh": ("Een zachte Blub-guh", "A soft Blub-guh"),
    "gui.guhs.piep.menu.speciaal.blubguh": ("Op je schouder!", "On your shoulder!"),
    "gui.guhs.piep.menu.speciaal.blubguh.tooltip": ("Hij hupt op je schouder. Sluip + rechtsklik op een blok: eraf.",
                                                    "It hops onto your shoulder. Sneak + right-click a block: off it hops."),
    "gui.guhs.piep.opgepakt.blubguh": ("Je hebt %s opgepakt. Hij is zo zacht!", "You picked up %s. So squishy!"),
    "gui.guhs.piep.neergezet.blubguh": ("%s hupt weer rond. Blub!", "%s hops about again. Blub!"),
    "gui.guhs.guhdex.info.blubguh": ("Een zachte Blub uit een droom, met een beetje guh erin. Hoeveel guh? Dat zegt zijn Blub-Guh-Meter!",
                                     "A soft Blub from a dream, with a bit of guh in it. How much guh? Its Blub-Guh-Meter says!"),
    # the menu
    "gui.guhs.blubguh.menu.meter": ("Blub %s Guh %s%%", "Blub %s Guh %s%%"),
    "gui.guhs.blubguh.menu.soort": ("%s, %s, %s%% groot", "%s, %s, %s%% big"),
    "gui.guhs.blubguh.menu.baby": ("Babyblubje!", "Baby blub!"),
    "gui.guhs.blubguh.menu.titel.puur": ("Een Pure Blub! Helemaal zonder guh. Zeldzaam!", "A Pure Blub! No guh at all. Rare!"),
    "gui.guhs.blubguh.menu.titel.vol": ("Een Volle Guh! Bijna een echte guh. Zeldzaam!", "A Full Guh! Almost a real guh. Rare!"),
    "gui.guhs.blubguh.menu.kast": ("Kledingkast", "Wardrobe"),
    "gui.guhs.blubguh.menu.kast.tooltip": ("Strikjes en (later) meer: kies wat hij draagt.", "Bows and (later) more: pick what it wears."),
    "gui.guhs.blubguh.menu.trucjes": ("Trucjes", "Tricks"),
    "gui.guhs.blubguh.menu.trucjes.tooltip": ("Alle trucjes en oortjes-emoties van je Blub.", "All your Blub's tricks and ear moods."),
    "gui.guhs.blubguh.trucjes.titel": ("Trucjes van %s", "Tricks of %s"),
    "gui.guhs.blubguh.truc.doe": ("Doe!", "Go!"),
    "gui.guhs.blubguh.kast.titel": ("Kledingkast van %s", "Wardrobe of %s"),
    "gui.guhs.blubguh.kast.uitleg": ("Klik om aan te trekken. Alle strikjes zijn gratis!", "Click to put it on. Every bow is free!"),
    "gui.guhs.blubguh.kast.aan": ("Draagt nu: %s", "Wearing: %s"),
    "gui.guhs.blubguh.kast.plek.strik": ("Strikje", "Bow"),
    "gui.guhs.blubguh.kast.strik": ("%s strikje", "%s bow"),
    "gui.guhs.blubguh.snoes_eerst": ("Deze Blub kent je nog niet... help eerst Snoes in slaap, njeg!",
                                     "This Blub doesn't know you yet... help Snoes fall asleep first, njeg!"),
    "commands.guhs.blub.snoes.aan": ("Snoes is geholpen: je ziet nu wilde Blubs (en kunt ze temmen).", "Snoes was helped: you see wild Blubs now (and can tame them)."),
    "commands.guhs.blub.snoes.uit": ("Snoes is nog niet geholpen: wilde Blubs zijn weer onzichtbaar voor je.", "Snoes not helped yet: wild Blubs are invisible to you again."),
    "gui.guhs.blubguh.menu.schouder": ("Op je schouder", "On your shoulder"),
    "gui.guhs.blubguh.menu.schouder.tooltip": ("Hij hupt op je schouder. Sluip + rechtsklik op een blok: eraf.",
                                               "It hops onto your shoulder. Sneak + right-click a block: off it hops."),
    # messages
    "gui.guhs.blubguh.op_schouder": ("%s zit op je schouder. Squish!", "%s sits on your shoulder. Squish!"),
    "gui.guhs.blubguh.liedje": ("%s piept een slaapliedje... piep, piep, pieeep...", "%s squeaks a lullaby... squeak, squeak, squeeeak..."),
    "gui.guhs.blubguh.liedje.klaar": ("Sst... %s guhs zijn heel slaperig geworden. Njeg.", "Shh... %s guhs got very sleepy. Njeg."),
    "gui.guhs.blubguh.verstop.start": ("%s verstopt zich! Tel tot tien... en zoek hem binnen een minuut!",
                                       "%s is hiding! Count to ten... and find it within a minute!"),
    "gui.guhs.blubguh.verstop.geen_plek": ("%s vindt hier geen goed verstopplekje.", "%s can't find a good hiding place here."),
    "gui.guhs.blubguh.verstop.gevonden": ("Gevonden! %s is helemaal blij. Hartjes!", "Found! %s is so happy. Hearts!"),
    "gui.guhs.blubguh.verstop.op": ("Njeg, niet gevonden! %s komt zelf maar tevoorschijn. Piep!", "Njeg, not found! %s comes out by itself. Squeak!"),
    "gui.guhs.blubguh.hupsen.start": ("%s gaat hupsen! Hup, hup, hup...", "%s starts hopping! Hop, hop, hop..."),
    "gui.guhs.blubguh.hupsen.klaar": ("%s hupste %s keer rond je! (record: %s)", "%s hopped around you %s times! (record: %s)"),
    "gui.guhs.blubguh.hupsen.record": ("Nieuw record! %s hupste %s keer rond je! (record: %s)", "New record! %s hopped around you %s times! (record: %s)"),
    # commands
    "commands.guhs.blub.spawn": ("Blub-guh gemaakt: meter %s, soort %s, tint %s, grootte %s", "Blub-guh made: meter %s, kind %s, tint %s, size %s"),
    "commands.guhs.blub.raster": ("Het raster staat voor je neus: 5 soorten x meter 0 / 25 / 50 / 75 / 100 (tam, zittend).",
                                  "The grid is in front of you: 5 kinds x meter 0 / 25 / 50 / 75 / 100 (tame, sitting)."),
    "commands.guhs.blub.groepje": ("%s random Blub-guhs gemaakt (de echte spawn-trekking).", "Made %s random Blub-guhs (the real spawn dice)."),
    "commands.guhs.blub.tam": ("%s Blub-guhs in de buurt zijn nu van jou.", "%s Blub-guhs nearby are yours now."),
    "commands.guhs.blub.geen": ("Geen Blub-guh in de buurt.", "No Blub-guh nearby."),
    "commands.guhs.blub.info": ("%s: Blub %s Guh %s%% | soort %s | tint %s | grootte %s | %s | %s | strikje %s",
                                "%s: Blub %s Guh %s%% | kind %s | tint %s | size %s | %s | %s | bow %s"),
}
for _s in SOORT_NL:
    TEKSTEN[f"gui.guhs.blubguh.soort.{_s}"] = (SOORT_NL[_s], SOORT_EN[_s])
    for _t in range(3):
        TEKSTEN[f"gui.guhs.blubguh.tint.{_s}.{_t}"] = (TINT_NL[_s][_t], TINT_EN[_s][_t])
for _i in range(4):
    TEKSTEN[f"gui.guhs.blubguh.truc_melding.{_i}"] = (TRUC_NL[_i], TRUC_EN[_i])
for _id, (_nl, _en, _unl, _uen) in TRUCJES.items():
    TEKSTEN[f"gui.guhs.blubguh.truc.{_id}"] = (_nl, _en)
    TEKSTEN[f"gui.guhs.blubguh.truc.{_id}.tooltip"] = (_unl, _uen)
try:                                                   # the subtitles of its sound events (one per action)
    import blub_audio_import as _audio
except ImportError:
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    import blub_audio_import as _audio
for _a in _audio.ACTIES:
    TEKSTEN[f"subtitles.guhs.blubguh.{_a.id}"] = (_a.ondertitel_nl, _a.ondertitel_en)
TEKSTEN.update(klusjes.TEKSTEN)


def zichtbaar(soort, meter):
    """Which switchable bones show (the same rules as client/BlubClient.zichtbaar)."""
    stap = 0 if meter <= 0 else 6 if meter >= 100 else 1 if meter < 20 else 2 if meter < 50 else 3 if meter < 60 else 4 if meter < 80 else 5
    z = {"lijf": stap < 4, "lijf_zacht": stap >= 4,
         "oor_l": soort == "blauw" and stap < 6, "oor_r": soort == "blauw" and stap < 6,
         "guhoortje_l": stap == 5 and soort in ("weide", "schild", "zwam"), "guhoortje_r": stap == 5 and soort in ("weide", "schild", "zwam"),
         "guhoor_l": stap == 6, "guhoor_r": stap == 6,
         "weide_sprieten": soort == "weide", "weide_hengel": soort == "weide",
         "schild_vleugel_l": soort == "schild", "schild_vleugel_r": soort == "schild", "schild_sprieten": soort == "schild",
         "ravijn_spriet_l": soort == "ravijn" and stap < 6, "ravijn_spriet_r": soort == "ravijn" and stap < 6,
         "ravijn_vin_l": soort == "ravijn", "ravijn_vin_r": soort == "ravijn",
         "zwam_gewei": soort == "zwam", "strik": False}
    return z


def stap_naam(meter):
    """The texture step of a meter (client/BlubClient.textuur): p, b, or the meter rounded down to 5."""
    if meter <= 0:
        return "p"
    if meter < 20:
        return "b"
    return str(min(100, meter // 5 * 5))


# =====================================================================================================================
def icoon():
    """The picked-up Blub-guh: a light blue block with two little ears and the three stripes."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rand, lijf, kern = (160, 213, 247), (138, 201, 247), (120, 186, 240)
    d.rectangle([4, 1, 5, 5], fill=rand)
    d.rectangle([10, 1, 11, 5], fill=rand)
    d.rectangle([5, 2, 5, 5], fill=(118, 178, 232))
    d.rectangle([10, 2, 10, 5], fill=(118, 178, 232))
    d.rectangle([1, 5, 14, 13], fill=rand)
    d.rectangle([2, 6, 13, 12], fill=lijf)
    d.rectangle([3, 7, 12, 11], fill=kern)
    st = (112, 67, 80)
    d.rectangle([3, 8, 4, 8], fill=st)
    d.rectangle([11, 8, 12, 8], fill=st)
    d.rectangle([7, 9, 8, 9], fill=st)
    for x in (2, 12):
        d.rectangle([x, 14, x + 1, 15], fill=(126, 188, 236))
    return img


def build(h):
    A, D, w = h.A, h.D, h.w
    modellen.build(h)
    klusjes.build(h)
    h.save(icoon(), "item", "blubguh_item.png")
    h.item_model("blubguh_item")
    w(f"{A}/models/item/blubguh_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    w(f"{D}/loot_table/entities/blubguh.json", {"type": "minecraft:entity", "pools": []})
    for name, (biomes, spawners) in SPAWNS.items():
        w(f"{D}/neoforge/biome_modifier/{name}.json", {
            "type": "neoforge:add_spawns", "biomes": biomes,
            "spawners": [{"type": t, "weight": wt, "minCount": lo, "maxCount": hi} for t, wt, lo, hi in spawners]})

    def patch(d):
        _audio.sound_events(d)                          # (keeps the imported files, adds missing events, drops old ones)
    h.patch_json(f"{A}/sounds.json", patch)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    problems = modellen.check(h)
    for p in (f"{h.A}/models/item/blubguh_item.json", f"{h.A}/models/item/blubguh_spawn_egg.json",
              os.path.join(h.TEX, "entity", "blubguh", "blauw_gloed.png")):
        if not os.path.exists(p):
            problems.append(f"missing {p}")
    for s in modellen.SOORTEN:
        for t in range(3):
            for st in modellen.STAPPEN:
                if not os.path.exists(os.path.join(h.TEX, "entity", "blubguh", f"{s}_{t}_{st}.png")):
                    problems.append(f"missing texture {s}_{t}_{st}")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    if problems:
        raise SystemExit("blub self-check failed:\n  " + "\n  ".join(problems))


# =====================================================================================================================
def preview(out):
    """Offline renders: the 5 soorten x meter 0 / 25 / 50 / 75 / 100, front-left, as one sheet (out/blub_preview.png)."""
    sys.path.insert(0, "tools")
    import tempfile
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo_pad = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity", "blubguh.geo.json")
    geo = json.load(open(geo_pad, encoding="utf-8"))
    tiles = []
    for soort in modellen.SOORTEN:
        for meter in (0, 25, 50, 75, 100):
            z = zichtbaar(soort, meter)
            g = json.loads(json.dumps(geo))
            g["minecraft:geometry"][0]["bones"] = [b for b in g["minecraft:geometry"][0]["bones"] if z.get(b["name"], True)]
            with tempfile.NamedTemporaryFile("w", suffix=".geo.json", delete=False, encoding="utf-8") as f:
                json.dump(g, f)
            q = wr.geo_quads(f.name, f"guhs:entity/blubguh/{soort}_0_{stap_naam(meter)}")
            os.unlink(f.name)
            tiles.append(wr.render(q, 25, -20, 240, margin=0.08))
    sheet = Image.new("RGBA", (240 * 5, 240 * 5), (236, 232, 240, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 5) * 240, (i // 5) * 240))
    sheet.save(os.path.join(out, "blub_preview.png"))
    print("blub preview:", os.path.join(out, "blub_preview.png"))


def _direct():
    """Standalone: only the Blub's own files (nl_nl gets the Dutch texts, en_us the English ones; the English also goes
    into tools/lang/en/c210_blub.json so a full make_resources run keeps it)."""
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c210_blub.json")
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
    _mc26()
    print(f"blub: model, {len(modellen.SOORTEN) * 3 * len(modellen.STAPPEN)} textures, {len(TEKSTEN)} texts")


def _mc26():
    """Only the Blub's items in their 26.1 shape (tools/mc26.py does this for the whole tree): the client item definitions
    and the baked spawn egg (from the 1.21.1 template in the main tree's build/moddev/artifacts)."""
    import zipfile
    import mc26
    naam = "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"
    for base in (os.path.join("build", "moddev", "artifacts"), os.path.join("..", "guhs", "build", "moddev", "artifacts"),
                 os.path.join("..", "..", "guhs", "build", "moddev", "artifacts")):
        if os.path.exists(os.path.join(base, naam)):
            mc26._JAR = zipfile.ZipFile(os.path.join(base, naam))
            break
    mc26.SPAWN_EGGS = {"blubguh_spawn_egg": mc26.SPAWN_EGGS["blubguh_spawn_egg"]}
    mc26.spawn_eggs()
    for n in ["blubguh_item", "blubguh_spawn_egg"] + klusjes.ITEMS:
        with open(os.path.join(mc26.A, "items", n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "preview":
        preview(sys.argv[2] if len(sys.argv) > 2 else ".")
    else:
        _direct()
