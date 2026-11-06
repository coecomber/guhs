"""
1.10.0 "Het Vadsrijk", round U, slice U-TABS: Het Vadsrijk-menu (Java: feature/vadsrijk/menu).

Only texts (Dutch + real English): the tab names + hover lines, the Karakter / Puzzels / Reiskaart / Instellingen panels,
the search box, the menu key.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_tabs.py
"""
import json
import os

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")

M = "vadsrijk.menu."
T = M + "tab."
I = M + "instellingen."
TEKSTEN = {
    "key.guhs.vr_menu": ("Vadsrijk-menu (Karakter)", "Vadsrijk menu (Character)"),
    M + "toets": ("Toets: %s", "Key: %s"),
    M + "zoek": ("Zoeken...", "Search..."),
    M + "zoek.niks": ("Niks gevonden.", "Nothing found."),
    M + "laden": ("Even geduld...", "One moment..."),
    # the tabs
    T + "karakter": ("Karakter", "Character"),
    T + "karakter.uitleg": ("Je level, klasse, stats en al je uitrusting.", "Your level, class, stats and all your gear."),
    T + "skillboom": ("Skillboom", "Skill tree"),
    T + "skillboom.uitleg": ("Leer nieuwe knopen met je skillpunten.", "Learn new nodes with your skill points."),
    T + "questlog": ("Questlog", "Quest log"),
    T + "questlog.uitleg": ("Je quests, geheimen, wezens en de Avonturiersgids.", "Your quests, secrets, creatures and the Adventurer's guide."),
    T + "voltooiing": ("Verkenningsboek", "Exploration book"),
    T + "voltooiing.uitleg": ("Wat je per zone al gedaan en gevonden hebt.", "What you have done and found in each zone."),
    T + "rijdieren": ("Rijdieren", "Mounts"),
    T + "rijdieren.uitleg": ("Je rijdieren (en het Zeilbootje).", "Your mounts (and the Sailboat)."),
    T + "huisdieren": ("Huisdieren", "Pets"),
    T + "huisdieren.uitleg": ("Je huisdiertjes: kies wie er met je meeloopt.", "Your little pets: pick who walks with you."),
    T + "kledingkast": ("Kledingkast", "Wardrobe"),
    T + "kledingkast.uitleg": ("Hoe je eruitziet: kleding, vleugels, capes en effecten.", "How you look: clothes, wings, capes and effects."),
    T + "knapzak": ("Knapzak", "Knapsack"),
    T + "knapzak.uitleg": ("Je knapzak en je materiaaltas.", "Your knapsack and your material bag."),
    T + "materiaaltas": ("Materiaaltas", "Material bag"),
    T + "ambachten": ("Ambachten", "Professions"),
    T + "ambachten.uitleg": ("Je ambachten, recepten en bestellingen.", "Your professions, recipes and orders."),
    T + "puzzels": ("Puzzels", "Puzzles"),
    T + "puzzels.uitleg": ("Alle puzzels, geheimpjes en verstopte knuffels van alle zones.", "All puzzles, secrets and hidden cuddlies of every zone."),
    T + "ridders": ("Ridders", "Knights"),
    T + "ridders.uitleg": ("Je karakters: wisselen kan in een stad of bij een kampvuur.", "Your characters: switch in a town or at a campfire."),
    T + "reiskaart": ("Reiskaart", "Travel map"),
    T + "reiskaart.uitleg": ("De kaart van het Vadsrijk (alleen kijken; vliegen doe je bij een luchtschip).",
                             "The map of the Vadsrijk (look only; you fly at an airship)."),
    T + "instellingen": ("Instellingen", "Settings"),
    T + "instellingen.uitleg": ("Geluid, effecten, schermschud en je toetsen.", "Sound, effects, screen shake and your keys."),
    # reiskaart (look only)
    M + "reiskaart.uitleg": ("Sleep om te kijken. Vliegen doe je bij een luchtschip-NPC.", "Drag to look around. You fly at an airship NPC."),
    M + "reiskaart.open": ("Hier kun je heen vliegen (bij een luchtschip).", "You can fly here (at an airship)."),
    # puzzels
    M + "puzzels.alles": ("Alles", "All"),
    M + "puzzels.open": ("Nog te doen", "To do"),
    M + "puzzels.klaar": ("Gevonden", "Found"),
    M + "puzzels.uitleg": ("Tip: zoek op een naam of een plek.", "Tip: search for a name or a place."),
    # karakter
    M + "karakter.level": ("Level %s", "Level %s"),
    M + "karakter.max": ("Hoogste level bereikt!", "Highest level reached!"),
    M + "karakter.xp": ("%s / %s xp naar het volgende level", "%s / %s xp to the next level"),
    M + "karakter.klasse": ("%s, klasse-level %s", "%s, class level %s"),
    M + "karakter.klassen": ("Je klassen", "Your classes"),
    M + "karakter.schade": ("Wapenschade: %s per seconde", "Weapon damage: %s per second"),
    M + "karakter.schade.uitleg": ("Hoe hard je vaardigheden ongeveer raken met dit wapen (voor stats en bonussen).",
                                   "Roughly how hard your skills hit with this weapon (before stats and bonuses)."),
    M + "karakter.uitrusting": ("Uitrusting", "Gear"),
    M + "karakter.uitrusting.uitleg": ("Ringen, ketting, oorbel, armband, handschoenen en cape doe je aan met rechtsklik of shift-klik in je inventaris.",
                                       "Put on rings, necklace, earring, bracelet, gloves and cape with right-click or shift-click in your inventory."),
    M + "karakter.leeg": ("Nog leeg.", "Still empty."),
    M + "karakter.stats": ("Stats", "Stats"),
    # instellingen
    I + "kop.geluid": ("Geluid", "Sound"),
    I + "kop.beeld": ("Beeld", "Picture"),
    I + "gebiedsmuziek": ("Gebiedsmuziek", "Area music"),
    I + "gebiedsmuziek.uitleg": ("Zachte muziek buiten de steden. 0% = uit.", "Soft music outside the towns. 0% = off."),
    I + "bazenmuziek": ("Bazenmuziek", "Boss music"),
    I + "bazenmuziek.uitleg": ("Het thema van elke baas tijdens het gevecht.", "Each boss's theme during the fight."),
    I + "stingers": ("Ta-daa's", "Stingers"),
    I + "stingers.uitleg": ("Korte muziekjes: nieuw gebied, quest klaar, level omhoog, geheim gevonden.",
                            "Short tunes: new area, quest done, level up, secret found."),
    I + "omgeving": ("Omgevingsgeluid", "Ambience"),
    I + "omgeving.uitleg": ("Vogels, water, krekels, bijtjes, golven...", "Birds, water, crickets, bees, waves..."),
    I + "stemmen": ("Stemmetjes", "Little voices"),
    I + "stemmen.uitleg": ("De piepjes van NPC's als ze praten.", "The beeps of NPCs when they talk."),
    I + "stadsmuziek": ("Stadsmuziek", "Town music"),
    I + "stadsmuziek.uitleg": ("Lieve deuntjes in de steden.", "Sweet tunes in the towns."),
    I + "effecten": ("Effecten", "Effects"),
    I + "effecten.uitleg": ("Deeltjes en flitsen van bazen en gevechten. De cirkels op de grond blijven altijd zichtbaar.",
                            "Particles and flashes of bosses and fights. The circles on the ground always stay visible."),
    I + "sfeer": ("Sfeer", "Atmosphere"),
    I + "sfeer.uitleg": ("Mist, licht, sporenstorm en roze buitjes per gebied.", "Fog, light, spore storm and pink showers per area."),
    I + "schermschud": ("Schermschud", "Screen shake"),
    I + "schermschud.uitleg": ("Schudden, hit-stop en camerabewegingen bij klappen en bazen. 0% = uit.",
                               "Shaking, hit-stop and camera moves on hits and bosses. 0% = off."),
    I + "tekstsnelheid": ("Tekstsnelheid", "Text speed"),
    I + "tekstsnelheid.uitleg": ("Hoe snel de tekst in gesprekken verschijnt.", "How fast the text appears in conversations."),
    I + "snelheid.0": ("meteen", "instant"),
    I + "snelheid.1": ("traag", "slow"),
    I + "snelheid.2": ("normaal", "normal"),
    I + "snelheid.3": ("snel", "fast"),
    I + "niveau.vol": ("Vol", "Full"),
    I + "niveau.zacht": ("Zacht", "Soft"),
    I + "niveau.uit": ("Uit", "Off"),
    I + "aan": ("Aan", "On"),
    I + "uit": ("Uit", "Off"),
    I + "toetsen": ("Toetsen...", "Keys..."),
    I + "hud": ("HUD aanpassen...", "Edit HUD..."),
    I + "meer": ("Alles wordt meteen bewaard. Meer opties: Mods > Guhs > Config.", "Everything is saved at once. More options: Mods > Guhs > Config."),
}


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


EN_OVERLAY = os.path.join("tools", "lang", "en", "c310_vr_u_tabs.json")


def _engels():
    """The real English goes into the overlay (make_v2.write_lang builds en_us from nl_nl + tools/lang/en)."""
    _schrijf(EN_OVERLAY, {k: v[1] for k, v in TEKSTEN.items()})


def build(h):
    _engels()
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    _engels()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, data)
    print(f"vadsrijk_u_tabs: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
