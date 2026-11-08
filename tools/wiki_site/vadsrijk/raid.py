"""
U-SITE round 3: the first raid, De Sterrendovende Toren (zone 6, u-z6raid), on guhs.nl/vadsrijk: an overview page (raid/index.html)
and one page per floor with the mechanics, the scaling and every drop with its chance.

Data: `data(bron)` reads the numbers from the mod's sources (zone6/raid/*.java: HP, enrage, drop chances) and the names/tips from
the lang file, so a balance change follows on the next build. The mechanics prose is written out here from SPIEK_U_Z6RAID.md
(player words, NL + EN). Spoilers: everything sits in data-zone="6"; the way the raid ENDS (Ringeling is cheered up and turns against
the Keizer) and who the Spiegelvizier really is are behind spoiler buttons.

User decisions: EVERY clear gives a chest (no week limit), loot is personal; rare MOUNTS drop with 10% per player per clear and
a pity counter (sure after 15 misses), like every mount in 1.10.
"""
import os
import re

from .pages import ESC, t, p, spoiler, feiten, lv

MAP = ("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone6", "raid")

# floor -> (id, entities [(kort, entity)], tip keys, en title, nl title, mechanics [(en, nl)], spoiler (en, nl) or None)
VERDIEPINGEN = [
    (1, "zandwachter", [("zandwachter", "vr_z6raid_baas_zandwachter")], ["vadsrijk.z6raid.tip.zandwachter"],
     "Floor 1: The Sand Guard", "Verdieping 1: De Zandwachter", [
         ("<b>Sand skin</b>: he only takes half damage.", "<b>Zandhuid</b>: hij krijgt maar half zoveel schade."),
         ("<b>The hourglass</b> (cast bar, 25 s): someone must stand on BOTH pink plates (west and east) at the same time for 2 seconds. Then he turns over: stunned and vulnerable for 18 s, and hits in his <b>back</b> do x1.8. Missed it? <b>Buried</b>: 45% damage to everyone.",
          "<b>De zandloper</b> (cast-balk, 25 s): op BEIDE roze platen (west en oost) moet tegelijk iemand staan, 2 tellen lang. Dan draait hij om: verdoofd en 18 s kwetsbaar, en in zijn <b>rug</b> doe je x1,8 schade. Mislukt? <b>Begraven</b>: 45% schade voor iedereen."),
         ("<b>Sand pillars</b>: break them within 20 s, otherwise he builds a sand wall.", "<b>Zandpilaren</b>: sla ze binnen 20 s kapot, anders bouwt hij een zandmuur."),
         ("<b>Sand waves</b>: red lanes, find the gap.", "<b>Zandgolven</b>: rode banen, zoek het gat."),
         ("<b>Sand fist</b>: a yellow circle. Stand in it TOGETHER (stack) to share the hit.", "<b>Zandvuist</b>: een gele cirkel. Ga er SAMEN in staan (stack) om de klap te delen."),
         ("<b>Splashes</b>: spread out.", "<b>Spetters</b>: ga uit elkaar (spread)."),
         ("<b>Sandstorm</b>: a purple cast bar. Interrupt it by doing damage.", "<b>Zandstorm</b>: een paarse cast-balk. Onderbreek hem door schade te doen."),
     ], None),
    (2, "spiegelvizier", [("spiegelvizier", "vr_z6raid_baas_spiegelvizier")], ["vadsrijk.z6raid.tip.spiegelvizier"],
     "Floor 2: The Mirror Vizier", "Verdieping 2: De Spiegelvizier", [
         ("He hides among identical mirror images. <b>Only the real one sparkles now and then</b>: look closely!", "Hij verstopt zich tussen identieke spiegelbeelden. <b>Alleen de echte fonkelt af en toe</b>: kijk goed!"),
         ("Right-click a <b>turning mirror</b>: it points at the next image. Two light beams on the same image: the real one = vulnerable for 20 s; a fake = a mirror clone steps out and everyone takes 10%.",
          "Rechtsklik een <b>draaispiegel</b>: die wijst naar het volgende beeld. Twee lichtbundels op hetzelfde beeld: de echte = 20 s kwetsbaar; een nepbeeld = er stapt een spiegelkloon uit en iedereen krijgt 10%."),
         ("<b>Who is who?</b>: every 30 s he swaps two players.", "<b>Wie is wie?</b>: elke 30 s wisselt hij twee spelers van plek."),
         ("At <b>20%</b> the hall breaks: shards rain down.", "Op <b>20%</b> breekt de zaal: er regenen scherven."),
     ], ("Who is he really? Meester Glans, the mirror polisher of the palace's Mirror Hall in Saffraanpoort: a Mika illusionist, in the service of Hertogin Ringeling.",
         "Wie is hij echt? Meester Glans, de spiegelpoetser van de Spiegelzaal in het paleis van Saffraanpoort: een Mika-illusionist, in dienst van Hertogin Ringeling.")),
    (3, "sterrenbeelden", [("ster_knuffel", "vr_z6raid_baas_ster_knuffel"), ("ster_bultguh", "vr_z6raid_baas_ster_bultguh"), ("ster_pluis", "vr_z6raid_baas_ster_pluis")],
     ["vadsrijk.z6raid.tip.sterrenbeelden", "vadsrijk.z6raid.tip.ster_bultguh", "vadsrijk.z6raid.tip.ster_pluis"],
     "Floor 3: The Dimmed Constellations", "Verdieping 3: De Gedoofde Sterrenbeelden", [
         ("Three at once. <b>The Great Cuddle</b> (tough): <i>Knuffelklem</i> = a circle on its target, the rest moves away; <i>Berenbrul</i> = a donut, so go INSIDE.",
          "Drie tegelijk. <b>De Grote Knuffel</b> (taai): <i>Knuffelklem</i> = een cirkel op het doelwit, de rest gaat weg; <i>Berenbrul</i> = een donut, dus ga naar BINNEN."),
         ("<b>The Flying Bultguh</b> charges over a red lane: step out.", "<b>De Vliegende Bultguh</b> stormt over een rode baan: stap eruit."),
         ("<b>The Little Fluff</b> heals the other two: interrupt her <b>Star Song</b>.", "<b>De Kleine Pluis</b> heelt de andere twee: onderbreek haar <b>Sterrenlied</b>."),
         ("At 0 they are not beaten but <b>\"out\"</b> (they glow). Then a <b>10-second window</b> runs: all three must go out inside it. Too late? They light each other again, at 40%.",
          "Op 0 zijn ze niet verslagen maar <b>\"uit\"</b> (ze stralen). Dan loopt er een <b>venster van 10 s</b>: alle drie moeten erin uit. Te laat? Dan steken ze elkaar weer aan, op 40%."),
         ("The raid frames show the three bars and the window.", "In de raidframes zie je de drie balken en het venster."),
     ], None),
    (4, "ringeling", [("ringeling", "vr_z6raid_baas_ringeling")], ["vadsrijk.z6raid.tip.ringeling"],
     "The roof: Duchess Ringeling", "Het dak: Hertogin Ringeling", [
         ("<b>Phase 1, Ten Toes</b>: she kicks rings: a pink catch circle inside a red one. <b>JUMP</b> in the pink circle just before the ring lands and you catch it (it flies back). Not caught? You get the ring's effect: fire, ice, light thief, push or swap.",
          "<b>Fase 1, Tien Tenen</b>: ze schopt ringen: een roze vangcirkel in een rode cirkel. <b>SPRING</b> in de roze cirkel vlak voor de ring landt en je vangt hem (hij vliegt terug). Niet gevangen? Dan krijg je het effect van de ring: vuur, ijs, lichtdief, duw of wissel."),
         ("<b>Phase 2, The Empty Lens</b> (60%): it goes dark. Pick up a <b>star shard</b> (right-click) and bring it to a dimmed <b>star beacon</b>. All beacons lit = the light returns: she is stunned and takes x1.5. Until then she does not drop below 25%.",
          "<b>Fase 2, De Lege Lens</b> (60%): het wordt donker. Pak een <b>sterrenscherf</b> (rechtsklik) en breng hem naar een gedoofd <b>sterrenbaken</b>. Branden alle bakens, dan komt het licht terug: ze is verdoofd en krijgt x1,5 schade. Tot dan zakt ze niet onder 25%."),
         ("<b>Phase 3, Ring Dance</b> (25%): low ring = jump, high ring = sneak. After 3 rings the Lens bursts: she falls (x2 damage).",
          "<b>Fase 3, Ringdans</b> (25%): lage ring = springen, hoge ring = sluipen. Na 3 ringen barst de Lens: ze valt (x2 schade)."),
     ], ("The end: Ringeling is not beaten but CHEERED UP. She talks, the stars in the dome light up again, and she turns against the Keizer.",
         "Het einde: Ringeling wordt niet verslagen maar OPGEVROLIJKT. Ze praat, de sterren in de koepel gaan weer branden en ze keert zich tegen de Keizer.")),
]


def _java(root, naam):
    try:
        return open(os.path.join(root, *MAP, naam + ".java"), encoding="utf-8").read()
    except OSError:
        return ""


def _getal(tekst, rx, standaard):
    m = re.search(rx, tekst)
    return float(m[1]) if m else standaard


def data(bron):
    """The raid as data (names, HP, enrage, chances) for the pages, the renders and data/vadsrijk.json."""
    root = bron.root
    loot = _java(root, "Z6RaidLoot")
    if not loot:
        return None
    kans = {k: _getal(loot, rf"KANS_{k}\s*=\s*([0-9.]+)", d) for k, d in
            (("VLEUGELS", 0.03), ("HUISDIER", 0.04), ("HALSTER", 0.02), ("REPLICA", 0.12), ("TEENRING", 0.015), ("WAPEN", 0.30))}
    hp_bron = {"zandwachter": ("Zandwachter", r"basisHp\(\)\s*\{\s*return\s*(\d+)", 30000),
               "spiegelvizier": ("Spiegelvizier", r"KLOON \? \d+ : (\d+)", 32000),
               "ster_knuffel": ("Sterrenbeeld", r"default -> (\d+)", 15000),
               "ster_bultguh": ("Sterrenbeeld", r"BULTGUH -> (\d+)", 12000),
               "ster_pluis": ("Sterrenbeeld", r"PLUIS -> (\d+)", 9000),
               "ringeling": ("Ringeling", r"basisHp\(\)\s*\{\s*return\s*(\d+)", 40000)}
    basis_enrage = _getal(_java(root, "Z6RaidBaas"), r"enrageTicks\(\)\s*\{\s*return 20 \* 60 \* (\d+)", 8)
    vds = []
    for nr, vid, ents, tips, ten, tnl, mech, sp in VERDIEPINGEN:
        bazen = []
        for kort, eid in ents:
            cls, rx, d = hp_bron[kort]
            bazen.append(dict(id=kort, entity=eid, naam=bron.t("entity.guhs." + eid, kort), hp=int(_getal(_java(root, cls), rx, d))))
        enr = _getal(_java(root, "Ringeling"), r"enrageTicks\(\)\s*\{\s*return 20 \* 60 \* (\d+)", 10) if vid == "ringeling" else basis_enrage
        vds.append(dict(nr=nr, id=vid, titel=tnl, titel_en=ten, bazen=bazen, enrage_min=int(enr),
                        tips=[bron.schoon(bron.t(k, "")) for k in tips if bron.t(k)],
                        fasen=[bron.t(f"vadsrijk.kern.beleving.fase.{ents[0][1]}.{i}") for i in (2, 3) if bron.t(f"vadsrijk.kern.beleving.fase.{ents[0][1]}.{i}")]))
    it = lambda i, d: bron.t("item.guhs.vr_z6raid_" + i, d)  # noqa: E731
    set_ids = re.findall(r'"(sterrenzijde_[a-z]+)"', loot) or ["sterrenzijde_tulband", "sterrenzijde_mantel", "sterrenzijde_pofbroek", "sterrenzijde_teenslofjes"]
    kist = [
        dict(soort="set", naam="Sterrenzijde-set (1 stuk)", kans=1.0, nl="zeker, elke clear (eerst een stuk dat je nog niet hebt); Legendarisch, lv 70",
             en="sure, every clear (a piece you don't have yet first); Legendary, lv 70", items=["z6raid_" + s for s in set_ids]),
        dict(soort="wapen", naam="Raidwapen voor jouw klasse", kans=kans["WAPEN"], nl=f"{kans['WAPEN'] * 100:g}%; zeker bij je eerste clear",
             en=f"{kans['WAPEN'] * 100:g}%; sure on your first clear", items=["z6raid_ringbreker", "z6raid_komeetboog", "z6raid_sterrenstaf"]),
        dict(soort="trofee", naam=it("ringreplica", "Ringreplica"), kans=kans["REPLICA"], nl=f"{kans['REPLICA'] * 100:g}%; zeker bij je eerste clear",
             en=f"{kans['REPLICA'] * 100:g}%; sure on your first clear (guild trophy)", items=[]),
        dict(soort="rijdier", naam=it("halster", "Sterrennacht-halster") + " (Sterrennacht-bultguh)", kans=0.10,
             nl="10% per speler per clear; zeker na 15 keer mis (geduldteller per karakter)",
             en="10% per player per clear; guaranteed after 15 misses (pity counter per character)", items=[]),
        dict(soort="huisdier", naam=bron.t("vadsrijk.huisdier.sterrenpluisje.naam", "Sterrenpluisje"), kans=kans["HUISDIER"],
             nl=f"{kans['HUISDIER'] * 100:g}%", en=f"{kans['HUISDIER'] * 100:g}%", items=[]),
        dict(soort="kleding", naam=bron.t("vadsrijk.kleding.ding.vleugel.sterrenring", "Sterrenring-vleugels"), kans=kans["VLEUGELS"],
             nl=f"{kans['VLEUGELS'] * 100:g}% (vleugels met de luchtdash, in je Kledingkast)", en=f"{kans['VLEUGELS'] * 100:g}% (wings with the air dash, in your Wardrobe)", items=[]),
        dict(soort="sieraad", naam=it("teenring", "Ringelings Teenring"), kans=kans["TEENRING"], nl=f"{kans['TEENRING'] * 100:g}%; Guhddelijk (de hele server ziet het)",
             en=f"{kans['TEENRING'] * 100:g}%; Guhddelijk (the whole server sees it)", items=["z6raid_teenring"]),
    ]
    munten = re.search(r"Resultaat\(uit, (\d+) \+ r\.nextInt\((\d+)\)\)", loot)
    klein = re.search(r"Resultaat\(items, (\d+) \+ r\.nextInt\((\d+)\)\)", loot)
    return dict(naam=bron.t("vadsrijk.z6raid.titel", "De Sterrendovende Toren"), spelers=[2, 10], level=70, verdiepingen=vds, kist=kist,
                munten=[int(munten[1]), int(munten[1]) + int(munten[2])] if munten else [2500, 3500],
                tussenkist=dict(munten=[int(klein[1]), int(klein[1]) + int(klein[2])] if klein else [700, 1100], setkans=0.30),
                titels=[bron.t("gui.guhs.titels.naam.z6raid_ringbreker", "Ringbreker"), bron.t("gui.guhs.titels.naam.z6raid_sterrenaansteker", "Sterrenaansteker")],
                halster_code=kans["HALSTER"])


KIST_SOORT = {"set": ("Set piece", "Setstuk"), "wapen": ("Weapon", "Wapen"), "trofee": ("Trophy", "Trofee"), "rijdier": ("Mount", "Rijdier"),
              "huisdier": ("Pet", "Huisdier"), "kleding": ("Wardrobe", "Kledingkast"), "sieraad": ("Jewellery", "Sieraad")}


def _vak(b, R, P):
    """The shared facts card of the raid pages."""
    return feiten([(t("Players", "Spelers"), f'{R["spelers"][0]}-{R["spelers"][1]}'), ("Level", lv([R["level"], R["level"]])),
                   (t("Floors", "Verdiepingen"), "4"), ("Zone", f'<a href="{b.r(P)}zones/zone6.html">{ESC(b.zone_naam("zone6"))}</a>'),
                   (t("Chest", "Kist"), t("every clear, per player", "elke clear, per speler")),
                   (t("Map", "Kaart"), b.l_kaart(next((x["pos"] for x in b.d["punten"] if x["soort"] == "raid"), None), P))])


def _loottabel(b, R, P):
    rijen = ""
    for k in R["kist"]:
        items = ", ".join(b.l_item(i, P) for i in k["items"] if i in b.items)
        rijen += (f'<tr><td>{t(*KIST_SOORT.get(k["soort"], (k["soort"], k["soort"])))}</td><td>{ESC(k["naam"])}{("<br><small>" + items + "</small>") if items else ""}</td>'
                  f'<td class="num" data-s="{k["kans"]}">{k["kans"] * 100:g}%</td><td>{t(k["en"], k["nl"])}</td></tr>')
    m0, m1 = R["munten"]
    rijen += f'<tr><td>{t("Coins", "Munten")}</td><td>{m0}-{m1} {t("bronze", "brons")}</td><td class="num" data-s="1">100%</td><td>{t("every clear", "elke clear")}</td></tr>'
    tk = R["tussenkist"]
    return (f'<h2 id="buit">{t("Loot", "Buit")}</h2>'
            + p("Loot is personal: EVERY clear gives every group member in the tower their own chest. No week limit, no rolling against each other. The chances are per player.",
                "Buit is persoonlijk: ELKE clear geeft elk groepslid in de toren een eigen kist. Geen weeklimiet, geen dobbelen tegen elkaar. De kansen zijn per speler.")
            + f'<h3>{t("The clear chest (after Ringeling)", "De clear-kist (na Ringeling)")}</h3><div class="tscroll"><table class="data sort"><thead><tr><th>{t("Kind", "Soort")}</th><th>{t("What", "Wat")}</th>'
            + f'<th class="num">{t("Chance", "Kans")}</th><th>{t("Details", "Details")}</th></tr></thead><tbody>{rijen}</tbody></table></div>'
            + f'<h3>{t("Floors 1-3", "Verdieping 1-3")}</h3>'
            + p(f'A small chest per player: {tk["munten"][0]}-{tk["munten"][1]} bronze and a {tk["setkans"] * 100:g}% chance of a Sterrenzijde piece.',
                f'Een kleine kist per speler: {tk["munten"][0]}-{tk["munten"][1]} brons en {tk["setkans"] * 100:g}% kans op een Sterrenzijde-stuk.')
            + f'<h3>{t("Titles", "Titels")}</h3><ul><li><b>{ESC(R["titels"][0])}</b>: {t("clear the raid", "maak de raid uit")}</li>'
            + f'<li><b>{ESC(R["titels"][1])}</b>: {t("a clear without fainting once", "een clear zonder één keer flauw te vallen")}</li></ul>')


def paginas(b):
    R = b.d.get("raid")
    if not R:
        b.notities.append("raid: no data (zone6/raid/Z6RaidLoot.java missing)")
        return
    verstopt = f'<div class="verstopt-zone" data-zone="6">{t("Hidden by your spoiler setting.", "Verborgen door je spoilerinstelling.")}</div>'
    # overview
    P = "raid/index.html"
    vk = ""
    for v in R["verdiepingen"]:
        img = "".join(b.render("raid_" + w["id"], P, "render klein", w["naam"]) for w in v["bazen"][:1])
        vk += (f'<a class="kaartje" href="{v["id"]}.html">{img}<h3>{t(v["titel_en"], v["titel"])}</h3>'
               f'<p>{", ".join(ESC(w["naam"]) for w in v["bazen"])}</p></a>')
    regels = [("2 to 10 players; you can't go in alone. One group at a time: the rest queue in De Wachtzaal.", "2 tot 10 spelers; alleen kom je er niet in. Eén groep tegelijk: de rest staat in de rij in De Wachtzaal."),
              ("Stand on the golden start circle for 3 seconds: party members in the waiting hall come along.", "Ga 3 tellen op de gouden startcirkel staan: partyleden in de Wachtzaal gaan mee."),
              ("Four floors, each with a stair; the stair up is closed by purple star film until that floor's boss is cheered up.", "Vier verdiepingen, elk met een trap; de trap omhoog zit dicht met paars sterrenvlies tot de baas van die verdieping is opgevrolijkt."),
              ("Fainting is mild: you wake up on the landing of your floor and may join in again. Eenvads knights do NOT retire in the raid.", "Flauwvallen is mild: je wordt wakker op het bordes van je verdieping en mag weer meedoen. Eénvads-ridders gaan in de raid NIET met pensioen."),
              ("Wipe: nobody of the group alive in the hall during a fight = the floor starts over after 3 seconds (the wipe counter goes +1).", "Wipe: niemand van de groep meer levend in de zaal tijdens een gevecht = de verdieping begint na 3 tellen opnieuw (de wipeteller gaat +1)."),
              ("Raid frames (top right): up to 10 players, the floor + boss, the fight clock to the enrage, wipes and total time.", "Raidframes (rechtsboven): tot 10 spelers, de verdieping + baas, de gevechtsklok tot de woede (enrage), wipes en de totale tijd."),
              ("On the live map of guhs.nl you are hidden while you are in the tower (\"in de raid\").", "Op de live kaart van guhs.nl ben je onzichtbaar zolang je in de toren bent (\"in de raid\").")]
    schaal = p("The bosses scale with the group size n at the start: HP x (0.55 + 0.45 n), mechanic counts ceil(n/2) (at least 2), at most 1 + n/3 adds. Hits only grow a little (x(0.9 + 0.02 n)): the challenge is the mechanics, not one-shots.",
               "De bazen schalen met de groepsgrootte n bij de start: levens x (0,55 + 0,45 n), mechanics ceil(n/2) keer (minstens 2), hooguit 1 + n/3 hulpjes. Klappen groeien maar een beetje (x(0,9 + 0,02 n)): de uitdaging zijn de mechanics, niet de eenklappers.")
    hp_rijen = "".join(f'<tr><td>{v["nr"]}</td><td>{ESC(w["naam"])}</td><td class="num">{w["hp"]:,}</td><td class="num">{round(w["hp"] * 1.45):,}</td>'
                       f'<td class="num">{round(w["hp"] * 5.05):,}</td><td class="num">{v["enrage_min"]} min</td></tr>'.replace(",", ".")
                       for v in R["verdiepingen"] for w in v["bazen"])
    hp = (f'<div class="tscroll"><table class="data"><thead><tr><th>#</th><th>{t("Boss", "Baas")}</th><th class="num">{t("HP (base)", "Levens (basis)")}</th>'
          f'<th class="num">{t("2 players", "2 spelers")}</th><th class="num">{t("10 players", "10 spelers")}</th><th class="num">Enrage</th></tr></thead><tbody>{hp_rijen}</tbody></table></div>')
    main = (f'<div data-zone="6"><nav class="crumbs"><a href="../bazen/index.html">{t("Bosses", "Bazen")}</a></nav><h1>{ESC(R["naam"])} <small class="muted">{t("raid", "raid")}</small></h1>'
            f'<div class="tw"><div><p class="lead">{t("The first raid of the Vadsrijk: a tower at the Mother Oasis in Het Zijdeland whose stars were put out. Climb four floors with 2-10 players and light the stars again.", "De eerste raid van het Vadsrijk: een toren bij de Moederoase in Het Zijdeland waarvan de sterren zijn gedoofd. Klim met 2-10 spelers vier verdiepingen omhoog en steek de sterren weer aan.")}</p>'
            f'<h2>{t("The floors", "De verdiepingen")}</h2><div class="grid">{vk}</div>'
            f'<h2>{t("How it works", "Zo werkt het")}</h2><ul>' + "".join(f"<li>{t(en, nl)}</li>" for en, nl in regels) + "</ul>"
            f'<h2>{t("Scaling", "Schalen")}</h2>{schaal}{hp}'
            + _loottabel(b, R, P)
            + f'</div><aside class="zij"><div class="kaartje">{_vak(b, R, P)}</div></aside></div></div>{verstopt}')
    b.schrijf(P, R["naam"], "The Star-Dousing Tower", main, "bazen/index.html", beschrijving=f'{R["naam"]}: de eerste raid van Het Vadsrijk (2-10 spelers, lv 70).')
    # one page per floor
    for i, v in enumerate(R["verdiepingen"]):
        P = f'raid/{v["id"]}.html'
        mech = next(x[6] for x in VERDIEPINGEN if x[1] == v["id"])
        sp = next(x[7] for x in VERDIEPINGEN if x[1] == v["id"])
        imgs = "".join(b.render("raid_" + w["id"], P, "render groot" if len(v["bazen"]) == 1 else "render klein", w["naam"]) for w in v["bazen"])
        tips = "".join(f'<p class="note">{ESC(x)}</p>' for x in v["tips"])
        fasen = ("<p>" + t("Phases", "Fasen") + ": " + " &middot; ".join(ESC(f) for f in v["fasen"]) + "</p>") if v["fasen"] else ""
        hp = ", ".join(f'{ESC(w["naam"])} {w["hp"]:,}'.replace(",", ".") for w in v["bazen"])
        nav = ((f'<a href="{R["verdiepingen"][i - 1]["id"]}.html">&larr; {t("previous floor", "vorige verdieping")}</a> ' if i else "")
               + (f'<a href="{R["verdiepingen"][i + 1]["id"]}.html">{t("next floor", "volgende verdieping")} &rarr;</a>' if i + 1 < len(R["verdiepingen"]) else ""))
        buit = (p("Small chest per player: coins and a 30% chance of a Sterrenzijde piece.", "Kleine kist per speler: munten en 30% kans op een Sterrenzijde-stuk.") if v["nr"] < 4
                else p("The clear chest: see the table on the raid page.", "De clear-kist: zie de tabel op de raidpagina.") + _loottabel(b, R, P))
        info = feiten([(t("Floor", "Verdieping"), str(v["nr"])), (t("HP (base, 1 player)", "Levens (basis)"), hp), ("Enrage", f'{v["enrage_min"]} min'),
                       ("Raid", f'<a href="index.html">{ESC(R["naam"])}</a>')])
        main = (f'<div data-zone="6"><nav class="crumbs"><a href="index.html">{ESC(R["naam"])}</a></nav><h1>{t(v["titel_en"], v["titel"])}</h1><div class="tw"><div>'
                + (f'<div class="raidfotos">{imgs}</div>' if imgs else "")
                + f'<h2>{t("Mechanics", "Mechanics")}</h2>{tips}<ul>' + "".join(f"<li>{th_(en, nl)}</li>" for en, nl in mech) + f"</ul>{fasen}"
                + (spoiler(p(ESC(sp[0]), ESC(sp[1]))) if sp else "")
                + f'<h2>{t("Loot", "Buit")}</h2>{buit}<p>{nav}</p>'
                + f'</div><aside class="zij"><div class="kaartje">{info}</div></aside></div></div>{verstopt}')
        b.schrijf(P, v["titel"], v["titel_en"], main, "bazen/index.html")


def th_(en, nl):
    """Both languages; the strings are trusted html written above."""
    return f'<span lang="en">{en}</span><span lang="nl">{nl}</span>'


def kaartje(b, P):
    """The raid card on the bosses index and the zone-6 page."""
    R = b.d.get("raid")
    if not R:
        return ""
    img = b.render("raid_ringeling", P, "render klein") or b.render("raid_zandwachter", P, "render klein")
    href = f'{b.r(P)}raid/index.html'
    return (f'<div class="kaartje" data-zone="6">' + (f'<a class="baasfoto" href="{href}">{img}</a>' if img else "")
            + f'<h3><a href="{href}">{ESC(R["naam"])}</a></h3><p><span class="badge">Raid</span> {t("2-10 players, 4 floors", "2-10 spelers, 4 verdiepingen")}</p></div>')
