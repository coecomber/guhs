"""Het Vadsrijk 1.10, T-DIALOOG: the Vadsrijk talking screen (Java: feature/vadsrijk/dialoog).

Only texts: the screen's buttons/options, the "Wat moet ik doen?" lines, the faction labels and the dev demo scene.
Run standalone from the project root:  PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_dialoog.py
"""
import json
import os

K = "vadsrijk.dialoog."
# key: (nl, en)
TEKSTEN = {
    K + "knop.log": ("Gesprekslog (L): wat zei iedereen ook alweer?", "Conversation log (L): what did everyone say again?"),
    K + "knop.instellingen": ("Tekstsnelheid en stemmetjes", "Text speed and voices"),
    K + "knop.log_dicht": ("Terug naar het gesprek", "Back to the conversation"),
    K + "knop.terug": ("« Terug", "« Back"),
    K + "knop.wat_doen": ("Wat moet ik doen?", "What should I do?"),
    K + "snelheid": ("Tekst: %s", "Text: %s"),
    K + "snelheid.0": ("meteen", "instant"),
    K + "snelheid.1": ("langzaam", "slow"),
    K + "snelheid.2": ("normaal", "normal"),
    K + "snelheid.3": ("snel", "fast"),
    K + "stemmen": ("Stemmetjes: %s%%", "Voices: %s%%"),
    K + "stemmen.uit": ("Stemmetjes: uit", "Voices: off"),
    K + "hint": ("Klik of spatie: verder  ·  shift+spatie: naar de keuzes  ·  L: log", "Click or space: next  ·  shift+space: to the choices  ·  L: log"),
    K + "log.kop": ("Gesprekslog", "Conversation log"),
    K + "log.leeg": ("Nog niets gezegd.", "Nothing said yet."),
    K + "doel.kop.actief": ("Nu:", "Now:"),
    K + "doel.kop.aanbod": ("Opdracht:", "Quest:"),
    K + "doel.kop.hoofd": ("Verhaal:", "Story:"),
    K + "doel.strip.actief": ("%s, %s", "%s, %s"),
    K + "doel.strip.aanbod": ("%s, %s", "%s, %s"),
    K + "doel.strip.hoofd": ("%s, %s", "%s, %s"),
    K + "doel.zin.actief": ("Voor \"%1$s\": %2$s, %3$s.%4$s", "For \"%1$s\": %2$s, %3$s.%4$s"),
    K + "doel.zin.aanbod": ("Als je ja zegt (\"%1$s\"): %2$s, %3$s.%4$s", "If you say yes (\"%1$s\"): %2$s, %3$s.%4$s"),
    K + "doel.zin.hoofd": ("Het verhaal gaat verder! %2$s, %3$s.%4$s", "The story goes on! %2$s, %3$s.%4$s"),
    K + "doel.extra": (" (%s)", " (%s)"),
    K + "factie.orde": ("Knuffelorde", "Knuffelorde"),
    K + "factie.mika": ("Mika", "Mika"),
    K + "factie.draak": ("Draak", "Dragon"),
    K + "factie.z2": ("De Splinterpieken", "De Splinterpieken"),
    K + "factie.z3": ("De Schemerwortel", "De Schemerwortel"),
    K + "factie.z4": ("De Lampionkust", "De Lampionkust"),
    K + "factie.z5": ("Hoogvads", "Hoogvads"),
    K + "cmd.oud": ("Dialoog: je krijgt nu het oude praatscherm (nog eens = het nieuwe).", "Dialogue: you now get the old talking screen (again = the new one)."),
    K + "cmd.nieuw": ("Dialoog: je krijgt weer het nieuwe Vadsrijk-praatscherm.", "Dialogue: you get the new Vadsrijk talking screen again."),
    # the dev demo scene (/guhs vadsrijk dialoog demo)
    K + "demo.0": ("Welkom bij de dialoog-demo! Ik ben de verteller: ik krijg een hele perkamentpagina in schuine letters, zonder portret. "
                   "Klik of druk op spatie om de tekst meteen helemaal te zien, en nog een keer om verder te gaan.",
                   "Welcome to the dialogue demo! I am the narrator: I get a whole parchment page in italics, without a portrait. "
                   "Click or press space to see all of the text at once, and again to go on."),
    K + "demo.1": ("Hoera, bezoek! Zie je hoe ik op en neer hop, met een hartje? Dat is blij. Mijn naambordje heeft de kleur van zone 1, en ernaast staat waar ik woon.",
                   "Hooray, a visitor! See how I hop up and down, with a little heart? That is happy. My name plate has the colour of zone 1, and next to it is where I live."),
    K + "demo.2": ("Ach, ik zak een beetje in en kijk naar beneden. Dat is verdrietig. Mijn bordje is ijsblauw: de kleur van de Splinterpieken.",
                   "Oh dear, I sink a little and look down. That is sad. My plate is ice blue: the colour of the Splinterpieken."),
    K + "demo.3": ("Oh! Een sprongetje en een uitroepteken: verrast! Guhs uit de Schemerwortel zoals ik hebben een honinggeel bordje.",
                   "Oh! A little jump and an exclamation mark: surprised! Guhs from the Schemerwortel like me have a honey-yellow plate."),
    K + "demo.4": ("Grr! Ik schud van boosheid. Mika's krijgen een grijs bordje en een eigen, sluw stemmetje.",
                   "Grr! I shake with anger. Mikas get a grey plate and their own sly little voice."),
    K + "demo.5": ("En dit ben ik zelf: mijn portret staat rechts en mijn bordje is mintgroen.",
                   "And this is me: my portrait is on the right and my plate is mint green."),
    K + "demo.6": ("Ik hoor bij de Knuffelorde, daarom is mijn bordje paars. Druk op L of op het streepjesknopje om het gesprekslog te zien, "
                   "en op « Terug of backspace om terug te bladeren.",
                   "I belong to the Knuffelorde, that is why my plate is purple. Press L or the lines button to see the conversation log, "
                   "and « Back or backspace to page back."),
    K + "demo.7": ("Dit is een lange bladzijde, om te zien dat alles past op GUI-schaal 2, 3 en 4. De tekst typt zichzelf, met een zacht piepje per woord; "
                   "hoe hard dat piepje is en hoe snel de tekst gaat, stel je in onder het tandwieltje. Met shift+spatie spring je meteen naar de keuzes. "
                   "Onder deze tekst staat de opdrachtstrook, en bij de keuzes zie je per soort een icoontje: een boek voor een opdracht, een smaragd voor een winkel, "
                   "een kaart en een veertje om te reizen, een deur om weg te gaan. Met de cijfertoetsen kies je meteen. "
                   "En de knop \"Wat moet ik doen?\" vertelt nog eens wie, wat en waar. Joepie!",
                   "This is a long page, to see that everything fits at GUI scale 2, 3 and 4. The text types itself, with a soft blip per word; "
                   "how loud that blip is and how fast the text goes, you set under the little gear. Shift+space jumps straight to the choices. "
                   "Below this text is the quest strip, and the choices each have a little icon: a book for a quest, an emerald for a shop, "
                   "a map and a feather to travel, a door to leave. The number keys choose at once. "
                   "And the button \"What should I do?\" tells you again who, what and where. Yippee!"),
    K + "demo.vol": ("Een vol scherm: veel tekst en negen keuzes. Op een laag venster of GUI-schaal 4 komen de keuzes in twee kolommen naast elkaar, "
                     "en als de tekst dan niet meer past, scroll je hem met het muiswiel (of spatie). Er valt niets buiten beeld. "
                     "Dit is nog wat extra tekst zodat de ballon echt vol raakt: guhs zijn altijd lief, ze knuffelen graag, en ze eten het liefst koekjes "
                     "met roze glazuur en hagelslag, het liefst drie tegelijk, en dan nog een vierde voor later.",
                     "A full screen: lots of text and nine choices. On a low window or GUI scale 4 the choices go in two columns side by side, "
                     "and when the text no longer fits, you scroll it with the mouse wheel (or space). Nothing falls off the screen. "
                     "This is some extra text so the balloon really fills up: guhs are always sweet, they love to cuddle, and they like cookies best "
                     "with pink icing and sprinkles, preferably three at once, and then a fourth for later."),
    K + "demo.gekozen": ("Dialoog-demo: je koos keuze %s.", "Dialogue demo: you picked choice %s."),
    K + "demo.doel.titel": ("De dialoog-demo", "The dialogue demo"),
    K + "demo.doel.wat": ("Breng 3 knuffelkoekjes naar Oma Truus", "Bring 3 cuddle cookies to Oma Truus"),
    K + "demo.doel.waar": ("bij de bakkerij in Zwamzoet (dit is maar een voorbeeld)", "at the bakery in Zwamzoet (just an example)"),
    "guhs.configuration.vadsrijkTekstSnelheid": ("Vadsrijk: snelheid van de praattekst (0 = meteen, 3 = snel)", "Vadsrijk: talking text speed (0 = instant, 3 = fast)"),
    "guhs.configuration.vadsrijkTekstSnelheid.tooltip": ("Hoe snel de tekst in het praatscherm verschijnt. Klikken laat altijd meteen alles zien.",
                                                         "How fast the text in the talking screen appears. A click always shows all of it at once."),
    "guhs.configuration.vadsrijkStemmen": ("Vadsrijk: stemmetjes in het praatscherm (%)", "Vadsrijk: little voices in the talking screen (%)"),
    "guhs.configuration.vadsrijkStemmen.tooltip": ("Hoe hard het zachte piepje per woord is (guh, Mika, verteller). 0 = uit.",
                                                   "How loud the soft blip per word is (guh, Mika, narrator). 0 = off."),
}


def build(h):
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)   # (T-INT: make_v2.lang(key, en, nl))


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, data)
    print(f"vadsrijk_t_dialoog: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
