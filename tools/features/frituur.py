"""
1.8.1: de frituurlijn. Java: feature/frituur (FrituurFeature, FrituurRecepten, SnackRecepten, VetsteHapItem) + block/FryingPanBlock.

  build(h)    the item models + textures (icons in tools/features/frituur_iconen; the three patatje variants are recoloured from
              patatje_kaassaus), the pan's messages, the recipe page in the Knus tab, the FTB texts and the blokuitleg of the pan
              (Dutch in nl_nl, English in tools/lang/en/c218_frituur.json)
  ftb(fq)     a section in the Knuffeldal chapter: fry on, patat, learn a recipe, De Vetste Hap

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/frituur.py
"""
import json
import os
import re
import sys

from PIL import Image

FTB_CHAPTER = "guhs_knuffeldal"
FTB_SECTION = "De frituurlijn"

ICONEN = os.path.join("tools", "features", "frituur_iconen")
ITEMS = ["dubbel_gefrituurde_kaasknabbels", "driedubbel_gefrituurde_kaasknabbels", "kaaskroket", "frikandel_guhspeciaal", "bitterguhllen",
         "kaassoufflguh", "patatje_kaassaus", "vetste_hap"]
PATATJES = {  # variant -> (colour of the paper instead of red, sauce dots on the fries)
    "patatje_vads": ((238, 120, 170), (250, 240, 215)),
    "patatje_oorlog": ((120, 150, 80), (120, 72, 40)),
    "patatje_guhspeciaal": ((232, 178, 52), (220, 60, 70)),
}
RECEPTEN = {  # recipe id -> (Dutch, English, what goes into the pan NL, EN)
    "patatje_vads": ("Patatje Vads", "Chonky Fries", "een gepofte aardappel", "a baked potato"),
    "patatje_oorlog": ("Patatje Oorlog-maar-lief", "War-but-Sweet Fries", "een giftige aardappel (de pan haalt het gif eruit, njeg)",
                       "a poisonous potato (the pan takes the poison out, njeg)"),
    "patatje_guhspeciaal": ("Patatje Guhspeciaal", "Guh Special Fries", "een patatje kaassaus", "fries with cheese sauce"),
    "kaaskroket": ("Kaaskroket", "Cheese Croquette", "een kaasbrok", "a chunk of cheese"),
    "frikandel_guhspeciaal": ("Frikandel Guhspeciaal", "Guh Special Frikandel", "een rauwe karbonade", "a raw porkchop"),
    "bitterguhllen": ("Bitterguhllen", "Bitterguhllen", "rauw rundvlees", "raw beef"),
    "kaassoufflguh": ("Kaassoufflguh", "Cheese Soufflguh", "een brood", "a loaf of bread"),
    "vetste_hap": ("De Vetste Hap", "The Chonkiest Bite", "16 driedubbel gefrituurde kaasknabbels in je hand, plus 4 kaaskroketten en 4 frikandellen",
                   "16 triple fried cheese nibbles in your hand, plus 4 cheese croquettes and 4 frikandels"),
}


def teksten():
    T = {
        "item.guhs.dubbel_gefrituurde_kaasknabbels": ("Dubbel gefrituurde kaasknabbels", "Double Fried Cheese Nibbles"),
        "item.guhs.dubbel_gefrituurde_kaasknabbels.lore": ("Nog een keertje de pan in. Waarom? Omdat het kan, njeg.",
                                                           "Back in the pan for another go. Why? Because it can, njeg."),
        "item.guhs.driedubbel_gefrituurde_kaasknabbels": ("Driedubbel gefrituurde kaasknabbels", "Triple Fried Cheese Nibbles"),
        "item.guhs.driedubbel_gefrituurde_kaasknabbels.lore": ("Meer vet dan knabbel. Je dokter huilt zachtjes.",
                                                               "More grease than nibble. Your doctor quietly weeps."),
        "item.guhs.kaaskroket": ("Kaaskroket", "Cheese Croquette"),
        "item.guhs.kaaskroket.lore": ("Buiten krokant, binnen lavaheet. Wacht even met bijten.", "Crispy outside, lava hot inside. Wait before biting."),
        "item.guhs.frikandel_guhspeciaal": ("Frikandel Guhspeciaal", "Guh Special Frikandel"),
        "item.guhs.frikandel_guhspeciaal.lore": ("Met kaassaus, knabbeluitjes en vahoege curry. Niemand weet wat erin zit, en dat is maar beter ook.",
                                                 "With cheese sauce, nibble onions and wahoog curry. Nobody knows what is in it, and that is for the best."),
        "item.guhs.bitterguhllen": ("Bitterguhllen", "Bitterguhllen"),
        "item.guhs.bitterguhllen.lore": ("Rond, krokant en kijkt je een beetje aan. Met een klodder mosterd.",
                                         "Round, crispy and looking at you a bit. With a dollop of mustard."),
        "item.guhs.kaassoufflguh": ("Kaassoufflguh", "Cheese Soufflguh"),
        "item.guhs.kaassoufflguh.lore": ("Een krokant kussentje vol gesmolten vads.", "A crispy little pillow full of molten chonk."),
        "item.guhs.patatje_kaassaus": ("Patatje Kaassaus", "Fries with Cheese Sauce"),
        "item.guhs.patatje_kaassaus.lore": ("Uit de pan, in een puntzak, onder de saus. Vahoeg.", "From the pan, into a paper cone, under the sauce. Wahoog."),
        "item.guhs.patatje_vads": ("Patatje Vads", "Chonky Fries"),
        "item.guhs.patatje_vads.lore": ("Met een extra laagje vads, voor de vadsers onder ons.", "With an extra layer of chonk, for the chonkers among us."),
        "item.guhs.patatje_oorlog": ("Patatje Oorlog-maar-lief", "War-but-Sweet Fries"),
        "item.guhs.patatje_oorlog.lore": ("Alle sauzen vechten om je aandacht, maar ze knuffelen het daarna weer uit.",
                                          "All the sauces fight for your attention, but they hug it out afterwards."),
        "item.guhs.patatje_guhspeciaal": ("Patatje Guhspeciaal", "Guh Special Fries"),
        "item.guhs.patatje_guhspeciaal.lore": ("Twee keer gefrituurd en goud van trots.", "Fried twice and golden with pride."),
        "item.guhs.vetste_hap": ("De Vetste Hap", "The Chonkiest Bite"),
        "item.guhs.vetste_hap.lore": ("Een toren van vads. Vult je hele balk. Alleen voor Onverwoestbare Vadsmagen.",
                                      "A tower of chonk. Fills your whole bar. Only for Indestructible Chonk Stomachs."),
        "item.guhs.vetste_hap.feestje": ("VAHOEG! Je hebt De Vetste Hap op! Heel Guhland hoort je boeren. Njeg!",
                                         "WAHOOG! You finished The Chonkiest Bite! All of Guhland hears you burp. Njeg!"),
        "block.guhs.frying_pan.recept_onbekend": ("Je kent het recept van %s nog niet. Vraag het aan de Snackbar-guh!",
                                                  "You don't know the recipe for %s yet. Ask the Snackbar Guh!"),
        "block.guhs.frying_pan.vetste_hap_nodig": ("De Vetste Hap: %s driedubbel gefrituurde kaasknabbels in je hand, %s kaaskroketten, %s frikandellen en %s vet in de pan",
                                                   "The Chonkiest Bite: %s triple fried nibbles in your hand, %s croquettes, %s frikandels and %s grease in the pan"),
        "gui.guhs.knus.verzameling.snackrecepten": ("Snackrecepten", "Snack Recipes"),
        "gui.guhs.knus.mijlpaal.frituur_recepten": ("Ken alle snackrecepten", "Know every snack recipe"),
        "gui.guhs.knus.mijlpaal.frituur_vetste_hap": ("Eet De Vetste Hap", "Eat The Chonkiest Bite"),
    }
    for rid, (nl, en, in_nl, in_en) in RECEPTEN.items():
        T[f"gui.guhs.knus.snackrecepten.{rid}"] = (nl, en)
        T[f"gui.guhs.knus.snackrecepten.{rid}.info"] = (f"In de frituurpan: {in_nl}.", f"In the frying pan: {in_en}.")
    return T


UITLEG = {"frying_pan": (
    "Rechtsklik met Mika's vet om de pan te vullen en met een lege hand om te zien hoeveel vet er nog in zit. Rechtsklik met iets lekkers om het te "
    "frituren: kaasknabbels worden gefrituurde kaasknabbels, en die kunnen nog twee keer de pan in (dubbel en driedubbel). Een guhvis wordt gebakken en "
    "een aardappel wordt Patatje Kaassaus.\n\nVan de Snackbar-guh leer je meer: kaasbrok wordt kaaskroket, karbonade frikandel, rundvlees bitterguhllen en "
    "brood kaassoufflguh. Een gepofte aardappel, een giftige aardappel en een Patatje Kaassaus geven de andere patatjes. Met 16 driedubbel gefrituurde "
    "kaasknabbels, 4 kaaskroketten en 4 frikandellen maak je De Vetste Hap.",
    "Right-click with Mika's grease to fill the pan, and with an empty hand to see how much is left. Right-click with something tasty to fry it: cheese "
    "nibbles become fried cheese nibbles, and those can go in twice more (double and triple). A guhfish gets fried and a potato becomes Fries with Cheese "
    "Sauce.\n\nThe Snackbar Guh teaches you more: a chunk of cheese becomes a croquette, a porkchop a frikandel, beef bitterguhllen and bread a soufflguh. A "
    "baked potato, a poisonous potato and Fries with Cheese Sauce give the other fries. With 16 triple fried nibbles, 4 croquettes and 4 frikandels you "
    "make The Chonkiest Bite.")}
BLOKUITLEG = os.path.join("tools", "wiki_site", "blokuitleg.py")
_MARK = "# <frituur> (1.8.1, tools/features/frituur.py: rewritten on every run)"


def blokuitleg():
    with open(BLOKUITLEG, encoding="utf-8") as f:
        src = f.read()
    src = re.sub(r"\n" + re.escape(_MARK) + r".*?# </frituur>\n", "\n", src, flags=re.S)
    regels = [_MARK] + [f"UITLEG[{k!r}] = dict(nl={nl!r}, en={en!r})" for k, (nl, en) in UITLEG.items()] + ["# </frituur>"]
    if not src.endswith("\n"):
        src += "\n"
    with open(BLOKUITLEG, "w", encoding="utf-8", newline="\n") as f:
        f.write(src + "\n".join(regels) + "\n")


def patatje(kleur, saus):
    """The red paper of patatje_kaassaus in another colour, with a few dots of sauce on the fries."""
    img = Image.open(os.path.join(ICONEN, "patatje_kaassaus.png")).convert("RGBA")
    px = img.load()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a and r > 120 and g < 90 and b < 90:
                schaal = r / 225
                px[x, y] = tuple(min(255, int(c * schaal)) for c in kleur) + (a,)
    for x, y in ((6, 3), (9, 2), (7, 5), (10, 4)):
        if px[x, y][3]:
            px[x, y] = saus + (255,)
    return img


def build(h):
    for n in ITEMS:
        h.save(Image.open(os.path.join(ICONEN, n + ".png")).convert("RGBA"), "item", n + ".png")
        h.item_model(n)
    for n, (kleur, saus) in PATATJES.items():
        h.save(patatje(kleur, saus), "item", n + ".png")
        h.item_model(n)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)
    blokuitleg()


def ftb(fq):
    q = fq.q
    q("frituur_dubbel", "Nog een keer de pan in", "Stop &6gefrituurde kaasknabbels&r nog een keer in de frituurpan. En daarna nog een keer. Vahoeg!",
      "guhs:driedubbel_gefrituurde_kaasknabbels", [fq.item("guhs:driedubbel_gefrituurde_kaasknabbels")],
      rewards=(("guhs:kaas_knabbels", 16),), x=0, y=0, xp=100)
    q("frituur_patat", "Patatje Kaassaus", "Frituur een &faardappel&r in de pan: je krijgt een Patatje Kaassaus.",
      "guhs:patatje_kaassaus", [fq.item("guhs:patatje_kaassaus")], rewards=(("guhs:kaas_knabbels", 8),), x=1.5, y=0, xp=100)
    q("frituur_kroket", "Vers van de Snackbar-guh", "Leer bij de &dSnackbar-guh&r een recept en maak thuis je eerste &6Kaaskroket&r.",
      "guhs:kaaskroket", [fq.item("guhs:kaaskroket")], rewards=(("guhs:kaas_knabbels", 16),), x=3, y=0, xp=150)
    q("frituur_hap", "De Vetste Hap", "Maak in de pan &6De Vetste Hap&r: 16 driedubbel gefrituurde kaasknabbels, 4 kaaskroketten en 4 frikandellen. "
      "Het opeten vraagt om een hele sterke maag!", "guhs:vetste_hap", [fq.item("guhs:vetste_hap")],
      rewards=(("guhs:mika_vet", 4),), x=4.5, y=0, xp=300)


FTB_EN = {
    "frituur_dubbel": ("Back in the Pan", "Put &6fried cheese nibbles&r in the frying pan once more. And then once more. Wahoog!"),
    "frituur_patat": ("Fries with Cheese Sauce", "Fry a &fpotato&r in the pan and you get Fries with Cheese Sauce."),
    "frituur_kroket": ("Fresh from the Snackbar Guh", "Learn a recipe from the &dSnackbar Guh&r and make your first &6Cheese Croquette&r at home."),
    "frituur_hap": ("The Chonkiest Bite", "Make &6The Chonkiest Bite&r in the pan: 16 triple fried cheese nibbles, 4 croquettes and 4 frikandels. "
                    "Eating it takes a very strong stomach!"),
}


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    T = teksten()
    ftb_en = {"ftb.guhs_knuffeldal.section.extra_frituur": "The Frying Line"}
    for key, (titel, tekst) in FTB_EN.items():
        ftb_en[f"ftb.guhs_knuffeldal.q.{key}.title"] = titel
        ftb_en[f"ftb.guhs_knuffeldal.q.{key}.desc"] = tekst
    with open(os.path.join("tools", "lang", "en", "c218_frituur_ftb.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(ftb_en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    en_pad = os.path.join("tools", "lang", "en", "c218_frituur.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in T.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as fh:
            d = json.load(fh)
        d.update({k: v[i] for k, v in T.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(d, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in ITEMS + list(PATATJES):
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"frituur: {len(ITEMS) + len(PATATJES)} items, {len(T)} texts")


if __name__ == "__main__":
    _direct()
