"""
3.0 "Guhverhalen" - the fundament (Java: feature/verhaal, and the core spots of CONTRACT_30 §3).

  build(h)  the worldgen (verhaal_wereld: the Sneeuwguhtoendra, Guhwai'i and the region placement), the texts of the shared
            parts (story guhs, the talking screen, "In de wolkjes... njeg", huisje ownership, the Verhalen tab of the Superkompas,
            the new Minigames groups and Highscores rows, the clothing sources), the advancement tabs guhs:verhalen/ and
            guhs:diertjes/ (their roots) and the fundament's own advancements, and the game test templates verhaal_test_*.
  ftb(fq)   the quests of the section "Nieuwe plekken & herinneringen" (chapter guhs_verhalen).

The ten story/critter slices (timmerguh, balto, balto_slee, mewtwo, hemel, guhwaii, guhwaii_spellen, vogels, waterdiertjes,
landdiertjes) run after this module (FEATURES order) and may override any text here by writing the same key again.
"""
from features import verhaal_wereld

IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
VERHAAL_GUHS = ["baltoguh", "mewtwo", "stitch626"]


# =====================================================================================================================
# advancements: the two new tabs (roots) and the fundament's own
# =====================================================================================================================
def zichtbaar(h, tab, name, parent, icon, frame, title, desc, criteria=None, background=None, hidden=False):
    """A visible advancement in tab guhs:<tab>/ (template: band.py visible)."""
    adv = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.{tab}.{name}.title"},
                       "description": {"translate": f"advancements.guhs.{tab}.{name}.description"},
                       "frame": frame, "show_toast": True, "announce_to_chat": frame != "task", "hidden": hidden},
           "criteria": criteria or IMPOSSIBLE}
    if parent:
        adv["parent"] = parent if ":" in parent else f"guhs:{tab}/{parent}"
    else:
        adv["display"]["background"] = background
        adv["display"]["announce_to_chat"] = False
    h.w(f"{h.D}/advancement/{tab}/{name}.json", adv)
    h.lang(f"advancements.guhs.{tab}.{name}.title", title, title)
    h.lang(f"advancements.guhs.{tab}.{name}.description", desc, desc)


def root_icoon(h):
    """guhs:baltoguh_beeldje once balto made it (balto.py sets BEELDJE_KLAAR = True), else a book."""
    try:
        from features import balto
    except ImportError:
        balto = None
    return "guhs:baltoguh_beeldje" if getattr(balto, "BEELDJE_KLAAR", False) else "minecraft:book"


def advancements(h):
    zichtbaar(h, "verhalen", "root", None, root_icoon(h), "task", "Guhverhalen",
              "Stap de Guhmensie in: daar wachten Baltoguh, Guhtwo, 626-guh, het Knuffelhart en de Timmerguh op je. Njeg!",
              criteria={"done": {"trigger": "minecraft:changed_dimension", "conditions": {"to": "guhs:guhmension"}}},
              background="minecraft:textures/block/pink_wool.png")
    zichtbaar(h, "diertjes", "root", None, "minecraft:feather", "task", "Diertjes van de Guhmensie",
              "Zet een van de lieve diertjes van de Guhmensie in je Guhdex: een vinkje, een eendje, een egeltje... of Sjokkel?",
              background="minecraft:textures/block/moss_block.png")
    zichtbaar(h, "verhalen", "verhaal_toendra", "root", "minecraft:snow_block", "task", "Brrr, wat wit!",
              "Vind de Sneeuwguhtoendra, het witte heuvelland waar ergens Nomguh ligt",
              criteria={"done": {"trigger": "minecraft:location",
                                 "conditions": {"player": {"location": {"biomes": f"guhs:{verhaal_wereld.TOENDRA}"}}}}})
    zichtbaar(h, "verhalen", "verhaal_guhwaii", "root", "minecraft:sand", "task", "Aloha, njeg!",
              "Vind een eilandje van Guhwai'i in de Diepe Guhzee: palmen, kokosnoten en warm zand",
              criteria={"done": {"trigger": "minecraft:location",
                                 "conditions": {"player": {"location": {"biomes": f"guhs:{verhaal_wereld.GUHWAII}"}}}}})
    zichtbaar(h, "verhalen", "verhaal_kompas", "root", "guhs:guhmensie_superkompas", "task", "Er was eens...",
              "Kies een plek in de nieuwe tab Verhalen van je superkompas")
    for name in ["verhaal_kompas"] + [f"verhaal_vrij_{g}" for g in VERHAAL_GUHS] + [f"verhaal_getemd_{g}" for g in VERHAAL_GUHS]:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})


# =====================================================================================================================
# texts
# =====================================================================================================================
LANG = {
    # the talking screen (Praat) and the story copies (VerhaalGuhs)
    "gui.guhs.verhaal.verder": "Verder »",
    "gui.guhs.verhaal.kopie.guh": "Njeg? Ik ben druk met mijn verhaal. Kom straks terug!",
    "gui.guhs.verhaal.kopie.baltoguh": "Snuf snuf... ruik jij dat ook? Sneeuw. Heel veel sneeuw. Njeg.",
    "gui.guhs.verhaal.kopie.mewtwo": "Ik heb... dubbel zoveel honger. Dat is best vahoeg, eigenlijk.",
    "gui.guhs.verhaal.kopie.stitch626": "Blub-njeg! (626-guh kijkt je met grote ogen aan en verstopt snel iets achter zijn rug.)",
    # "In de wolkjes... njeg" (band.Wolkjes)
    "gui.guhs.wolkjes.dood": "%s is naar de wolkjes... njeg. Er bleef een gloeiend sterretje achter, en al zijn herinneringen.",
    "gui.guhs.wolkjes.tab": "☁ %s is in de wolkjes... njeg. Zijn dagboekje blijft hier, vol lieve herinneringen.",
    "gui.guhs.wolkjes.wist": "Ik ben even in de wolkjes, heel zacht en heel roze. Denk je nog aan %1$s? Ik aan jou. Njeg.",
    "gui.guhs.band.plek.in_de_wolkjes": "In de wolkjes... njeg",
    "gui.guhs.dagboek.eerste.terug_uit_de_wolkjes": "Terug uit de wolkjes!",
    "gui.guhs.dagboek.eerste.terug_uit_de_wolkjes.tekst": "Ik was even in de wolkjes, maar het Knuffelhart klopte en toen was ik er weer. VAHOEG!",
    # huisje ownership
    "gui.guhs.huisje.van_wie": "Dit is het huisje van %1$s (%2$s). Alleen %1$s mag het veranderen.",
    # the Verhalen tab of the Superkompas, the Kleding tab, the Minigames tab
    "gui.guhs.superkompas.verhalen": "Verhalen",
    # (1.4.1: the tab lists every big story's places per world of the Guhpad, feature.guhpad.KompasVerhalen)
    "gui.guhs.superkompas.verhalen.tooltip": "De plekken van de grote verhalen, per wereld van het Guhpad",
    "gui.guhs.gids.soort.verhalen": "Guhverhalen",
    "gui.guhs.kledingbron.timmerguh": "Timmerguh's bouwplaats",
    "gui.guhs.kledingbron.nomguh": "Held van Nomguh",
    "gui.guhs.kledingbron.sledesprint": "De Nomguh-sledesprint",
    "gui.guhs.kledingbron.mewtwo": "Het kloon-eiland",
    "gui.guhs.kledingbron.hemel": "Het Hemelkapelletje",
    "gui.guhs.kledingbron.guhwaii": "Ohana op Guhwai'i",
    "gui.guhs.kledingbron.guhwaii_spellen": "Surfen & hula",
}
HIGHSCORES = {"sledesprint": "Nomguh-sledesprint", "surfen": "Surfen op Guhwai'i", "hula": "Hula-dansen"}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for base, name in HIGHSCORES.items():
        for lvl in ("makkelijk", "medium", "lastig"):
            h.lang(f"gui.guhs.highscores.game.{base}_{lvl}", f"{name}: {lvl}", f"{name}: {lvl}")


# =====================================================================================================================
# game test templates (VerhaalGameTests / VerhaalWereldGameTests)
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((12, 6, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("verhaal_test_wei")


def selfcheck():
    problems = []
    for key, text in LANG.items():
        low = text.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {key}")
    if problems:
        raise SystemExit("verhaal self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    verhaal_wereld.build(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck()


# =====================================================================================================================
# FTB quests: "Nieuwe plekken & herinneringen" (chapter guhs_verhalen)
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("verhaal_kompas", "Er was eens...", "Je &6superkompas&r heeft een nieuwe tab: &dVerhalen&r. Kies er een plek en volg de naald: "
      "Nomguh, het kloon-eiland, het Hemelkapelletje, de eilandjes van Guhwai'i of de bouwplaats van de Timmerguh. Elk verhaal eindigt "
      "met een nieuwe vriend. Njeg!", "guhs:guhmensie_superkompas", [adv("guhs:verhalen/verhaal_kompas")],
      rewards=(("guhs:kaas_knabbels", 12),), shape="gear", xp=100)
    q("verhaal_toendra", "De Sneeuwguhtoendra", "Ergens naast de Guhpieken ligt een wit heuvelland vol sneeuw: de &fSneeuwguhtoendra&r. "
      "Trek een warme sjaal aan! Midden in de toendra ligt het guhstadje &fNomguh&r...", "minecraft:snow_block",
      [fq.biome(verhaal_wereld.TOENDRA)], rewards=(("guhs:kaas_knabbels", 12),), deps=["verhaal_kompas"], xp=100)
    q("verhaal_guhwaii", "Guhwai'i", "In de Diepe Guhzee liggen tropische eilandjes: &bGuhwai'i&r! Warm zand, een ondiepe lagune en "
      "palmen. Aloha, njeg!", "minecraft:sand", [fq.biome(verhaal_wereld.GUHWAII)], rewards=(("guhs:kaas_knabbels", 12),),
      deps=["verhaal_kompas"], xp=100)
    q("verhaal_wolkjes", "In de wolkjes", "Soms gaat een tamme guh even naar de &dwolkjes&r. Njeg... Maar niks is ooit weg: zijn "
      "&ddagboekje&r blijft in je Guhdex (tab Mijn guhs), er blijft een gloeiend &dherinneringssterretje&r achter, en hoog in de lucht "
      "klopt het &dKnuffelhart&r, dat hem terug kan halen. Vink dit af als je het weet.", "minecraft:nether_star",
      [{"type": "checkmark"}], rewards=(("guhs:kaas_knabbels", 8),), deps=["verhaal_kompas"])
