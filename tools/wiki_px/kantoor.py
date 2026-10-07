"""
The wiki part of the guhpixel slice "kantoor": the Guhkantoor (Prikklok, Bureautjes, loonstrookjes, the kwartaalrapport,
Werknemer van de maand).

body(w) returns the HTML of this slice's part, built with the helpers of tools/make_wiki.py; every text is written twice,
(English, Dutch); the English is hand-written here and follows tools/lang/GLOSSARY.md and tools/lang/glossary_px/kantoor.md.
renders(r) adds this slice's pictures docs/wiki/img/guhkantoor_*.png (r is tools/wiki_renders.py, r.OUT the folder).
"""
import os


def body(w):
    out = []
    out.append(w.h3("The Guh Office", "Het Guhkantoor"))
    out.append(w.p(
        "An office for at home, bought from the Shopkeeper Guh in the Guhpixel lobby. You put your guhs to work there. Working means: "
        "sleeping on the keyboard. After eight real hours each guh gets a payslip. It earns you nothing at all, and that is exactly the point.",
        "Een kantoor voor thuis, te koop bij de Verkoper-guh in de Guhpixel-lobby. Je zet er je guhs aan het werk. Werken betekent: "
        "slapen op het toetsenbord. Na acht echte uren krijgt elke guh een loonstrookje. Het levert helemaal niks op, en dat is precies de bedoeling."))
    out.append(w.entry(
        w.img("guhkantoor_prikklok", "Time Clock"), "Time Clock", "Prikklok",
        w.p("The heart of the office. Right-click it for the screen: every desk, who sleeps there and for how long, when the next "
            "payslip is due, and the Employee of the Month. Breaking the clock sends every guh of its desks home.",
            "Het hart van het kantoor. Rechtsklik voor het scherm: elk bureau, wie er slaapt en hoe lang al, wanneer het volgende "
            "loonstrookje komt, en de Werknemer van de maand. Breek je de klok af, dan gaan alle guhs van zijn bureaus naar huis."),
        stats=[(("Shop", "Winkel"), w.t("Guh Office Set: 200 coins (with one Little Desk)", "Guhkantoor-set: 200 muntjes (met één Bureautje)")),
               (("Desks", "Bureaus"), w.t("at most 4, within 8 blocks", "hooguit 4, binnen 8 blokken"))]))
    out.append(w.entry(
        w.img("guhkantoor_bureautje", "Little Desk"), "Little Desk", "Bureautje",
        w.p("A desk, an old beige computer and a little stool. It connects to the nearest Time Clock with room. One guh works here: "
            "it lies across the keyboard with its eyes shut, and the monitor slowly fills with the letter j. Right-click the desk for the "
            "same screen as the clock.",
            "Een bureau, een oude beige computer en een krukje. Het sluit zich aan op de dichtstbijzijnde Prikklok met plek. Hier werkt één "
            "guh: hij ligt dwars over het toetsenbord met zijn oogjes dicht, en het beeldscherm loopt langzaam vol met de letter j. "
            "Rechtsklik op het bureau voor hetzelfde scherm als bij de klok."),
        stats=[(("Shop", "Winkel"), w.t("40 coins for each extra desk", "40 muntjes per extra bureau"))]))
    out.append(w.h3("How it works", "Zo werkt het"))
    out.append(w.ul([
        ("Stand near the office with one of your own guhs, click the Time Clock, pick a desk and choose <b>Put to Work</b>. The guh "
         "clocks in and falls asleep at once.",
         "Ga met een eigen guh bij het kantoor staan, klik op de Prikklok, kies een bureau en kies <b>Aan het werk</b>. De guh klokt in en "
         "valt meteen in slaap."),
        ("While it works the guh is stored safely: it cannot wander off, and My Guhs says \"At work at the Guh Office\". It cannot be "
         "called away; you send it home at the clock.",
         "Zolang hij werkt is de guh veilig opgeborgen: hij kan niet weglopen, en bij Mijn guhs staat \"Aan het werk op het Guhkantoor\". "
         "Roepen kan niet; naar huis sturen doe je bij de klok."),
        ("A shift takes <b>8 real hours</b>. The time keeps running when you are offline. At most three payslips wait per guh.",
         "Een dienst duurt <b>8 echte uren</b>. De tijd loopt door als je offline bent. Per guh liggen er hooguit drie loonstrookjes klaar."),
        ("<b>Go Home</b> clocks the guh out: the very same guh steps out next to the desk, with the payslips it earned. The same "
         "happens when a desk or the clock is broken, by anyone or anything. A guh can never get lost.",
         "<b>Naar huis</b> klokt de guh uit: precies dezelfde guh stapt naast het bureau, met de loonstrookjes die hij verdiende. "
         "Hetzelfde gebeurt als een bureau of de klok wordt afgebroken, door wie of wat dan ook. Een guh kan nooit kwijtraken."),
        ("Several players can share one clock: everybody puts their own guhs at a free desk and only gets their own payslips.",
         "Meerdere spelers kunnen één klok delen: iedereen zet zijn eigen guhs aan een vrij bureau en krijgt alleen zijn eigen loonstrookjes."),
    ]))
    out.append(w.h3("The paperwork", "Het papierwerk"))
    out.append(w.p(
        "All three papers can be read (right-click in the air) and hung on a wall (right-click a wall). Taken off the wall, a paper is "
        "still the same paper. Every paper is put together from dozens of lines, so no two are quite the same.",
        "Alle drie de papieren kun je lezen (rechtsklik in de lucht) en ophangen (rechtsklik op een muur). Haal je er een van de muur, "
        "dan is het nog steeds hetzelfde papier. Elk papier wordt samengesteld uit tientallen regels, dus geen twee zijn precies gelijk."))
    out.append(w.table(
        [("Paper", "Papier"), ("When", "Wanneer"), ("What it says", "Wat erop staat")],
        [[w.img("guhkantoor_loonstrookje", "Payslip", "px") + w.t(" Payslip", " Loonstrookje"),
          ("per guh, every 8 real hours", "per guh, elke 8 echte uren"),
          ("position, gross pay (0 nibbles), two deductions, a bonus, net pay, a remark from the boss and a stamp",
           "functie, bruto loon (0 knabbels), twee inhoudingen, een toeslag, netto, een opmerking van de baas en een stempel")],
         [w.img("guhkantoor_kwartaalrapport", "Quarterly Report", "px") + w.t(" Quarterly Report", " Kwartaalrapport"),
          ("with every third payslip of a player", "bij elk derde loonstrookje van een speler"),
          ("three graphs (a flat line, equal bars, a pie with one slice), three highlights, a conclusion and an outlook",
           "drie grafieken (een vlakke lijn, even hoge staven, een taart met één punt), drie hoogtepunten, een conclusie en een vooruitblik")],
         [w.img("guhkantoor_oorkonde", "Certificate", "px") + w.t(" Employee of the Month Certificate", " Oorkonde Werknemer van de maand"),
          ("when a real month is over", "als een echte maand voorbij is"),
          ("the guh that slept the most hours that month, and why it deserved it",
           "de guh die die maand de meeste uren sliep, en waarom hij het verdiende")]]))
    out.append(w.p(
        "Some lines you may come across: <i>\"Withheld: 3 naps\"</i>, <i>\"Chonk bonus: 7 extra minutes lying down\"</i>, "
        "<i>\"Net: 8 hours of sleep, tax-free\"</i>, <i>\"Meeting postponed due to chonking.\"</i>, "
        "<i>\"Accidentally typed the letter J 4,000 times. Fine report.\"</i>, <i>\"Conclusion: the figures don't lie. They sleep.\"</i>",
        "Een paar regels die je kunt tegenkomen: <i>\"Ingehouden: 3 dutjes\"</i>, <i>\"Vadstoeslag: 7 extra minuten liggen\"</i>, "
        "<i>\"Netto: 8 uur slaap, belastingvrij\"</i>, <i>\"Vergadering uitgesteld wegens vadsen.\"</i>, "
        "<i>\"Heeft per ongeluk 4.000 keer de letter J getypt. Prima rapport.\"</i>, <i>\"Conclusie: de cijfers liegen niet. Ze slapen.\"</i>"))
    out.append(w.p(
        "The <b>Employee of the Month</b> is the guh of yours that slept the most hours at a desk this real month. It is shown on the "
        "Time Clock's screen, in the Guhdex (tab Guhpixel &amp; Outings) and on every quarterly report.",
        "De <b>Werknemer van de maand</b> is jouw guh die deze echte maand de meeste uren aan een bureau sliep. Hij staat op het scherm van "
        "de Prikklok, in de Guhdex (tabblad Guhpixel &amp; uitjes) en op elk kwartaalrapport."))
    return "".join(out)


def renders(r):
    for naam, yaw in (("guhkantoor_prikklok", 30), ("guhkantoor_bureautje", 35), ("guhkantoor_bureautje_bezet", 35)):
        try:
            beeld = r.render(r.model_quads(f"guhs:block/{naam}"), yaw, -25, 320)
            beeld.save(os.path.join(r.OUT, f"{naam}.png"))
            if not naam.endswith("_bezet"):
                beeld.resize((64, 64), r.Image.LANCZOS).save(os.path.join(r.OUT, f"icon_{naam}.png"))
            print("rendered", naam)
        except Exception as e:
            print("no render for", naam, e)
    for naam in ("guhkantoor_loonstrookje", "guhkantoor_kwartaalrapport", "guhkantoor_oorkonde"):
        try:
            # the sheet as it hangs on the wall (big), and the item icon
            r.texture(f"guhs:block/{naam}").resize((256, 256), r.Image.NEAREST).save(os.path.join(r.OUT, f"{naam}.png"))
            r.item_icon(f"guhs:item/{naam}").save(os.path.join(r.OUT, f"icon_{naam}.png"))
            print("rendered", naam)
        except Exception as e:
            print("no render for", naam, e)
