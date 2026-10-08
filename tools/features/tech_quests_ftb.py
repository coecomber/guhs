"""
bbq2 (tech-quests): the WHOLE FTB chapter Guh-technologie (guhs_techniek), written as PROJECTS, not as a list of machines
(DESIGN_130 2 "Teaching", CONTRACT_130 8). After every quest, or every two, something works by itself in the player's own
world:

  Project 1  Nooit meer zelf bakken        a Guhrad, Guhdraad, a Guhoven that is fed and emptied by hoppers, a Vadsmolen,
                                           more vadskracht from cuddling and dancing guhs            (tier "Knutselen")
  the Oude Guhrad-centrale                 the Uitvinder-guh's practice hall: the questline "techniek" (features/tech_quests.py)
  Project 2  Alles vanzelf in de bank      zoutkristal, Knabbelbuizen, a sorted storage, the Hapluikje, an Opzuiger, a field
                                           that harvests itself, sensors, the battery                (tier "Zout")
  Project 3  Saus uit de kraan             pump, hose and vat, the frituur line, drinks that brew themselves, the grillkool
                                           line, the Blubkacheltje                                   (tier "Saus", recipe card)
  Project 4  Een fabriek die doorwerkt     the stone line, a forest in a tub, crafting without hands, the Bezorgguhtje
                                                                                                     (tier "Saus", recipe cards)
  De Grote Knabbelmachine                  the Gloeisterkern and the questline "knabbelmachine"      (tier "Gloeister")

Nothing is locked (a dependency only draws a line). The tasks are the hidden advancements the tech slices grant when a
machine of the player really does its thing (tech_machines_*, tech_vloeistof_*, tech_bronnen_*, tech_bezorg_*, bank_*), an
item in the hand, or a tick the player sets (a project that only the player can judge). The picture at the end of the
chapter (FTB_SLOT in tech_quests.py) previews what the Guheinde will add. Dutch only.

Added at the merge of tech-klusjes and tech-quests (the two slices were built side by side): the two chores of the Guhhuisje
that work the machines, "tech_quests_klus_machines" in project 1 and "tech_quests_klus_plantage" in project 4. Their tasks
are the hidden advancements of features/tech_klusjes.py (quest/tech_klusjes_machines, quest/tech_klusjes_plantage).

Added in phase 3: the other two hidden advancements of tech-klusjes that had a visible twin but no quest (DESIGN_130 0: every
new quest is in the book): "tech_quests_klus_oogst" (the bigger harvest of the farmen chore) in project 1 and
"tech_quests_klus_luikje" (chore output through a Hapluikje) in project 2. And the quests that send the player into the
Guhbarbecuether say that the way there goes through the grill portal first (the portal lock of the Knabbelring).
"""
from features import tech_bezorg, tech_bronnen, tech_vloeistof, vadskracht, wereld

SECTIES = [
    ("tech_quests_bakken", "Project 1: Nooit meer zelf bakken", "item:guhs:block/guh_oven_front_on",
     ["tech_quests_rad", "tech_quests_draad", "tech_quests_oven", "tech_quests_aflezen", "tech_quests_trechters", "tech_quests_klus_machines",
      "tech_quests_klus_oogst", "tech_quests_molen", "tech_quests_knuffel", "tech_quests_disco", "tech_quests_hoeveel"]),
    ("tech_quests_centrale", "De Oude Guhrad-centrale", "npc:uitvinderguh",
     ["tech_quests_centrale_vind"] + [f"tech_quests_techniek_{i}" for i in range(1, 9)] + ["tech_quests_maagje", "tech_quests_kaarten"]),
    ("tech_quests_bank", "Project 2: Alles vanzelf in de bank", "item:guhs:block/hapluikje_voor_werkt",
     ["tech_quests_zout", "tech_quests_buis", "tech_quests_richting", "tech_quests_filter", "tech_quests_sorteer", "tech_quests_sleutel",
      "tech_quests_hapluikje", "tech_quests_klus_luikje", "tech_quests_opzuiger", "tech_quests_oogster", "tech_quests_sensor", "tech_quests_batterij",
      "tech_quests_magazijn"]),
    ("tech_quests_saus", "Project 3: Saus uit de kraan", "item:guhs:block/sauspomp_voor_werkt",
     ["tech_quests_saus_kaart", "tech_quests_pomp", "tech_quests_vat", "tech_quests_frituur", "tech_quests_brouw", "tech_quests_pers",
      "tech_quests_blub"]),
    ("tech_quests_fabriek", "Project 4: Een fabriek die doorwerkt", "item:guhs:block/knabbelaar_voor_werkt",
     ["tech_quests_machine_kaart", "tech_quests_knabbelaar", "tech_quests_neerzetter", "tech_quests_plantagebak", "tech_quests_klus_plantage",
      "tech_quests_tekening",
      "tech_quests_knutsel", "tech_quests_bezorg_kaart", "tech_quests_station", "tech_quests_halte", "tech_quests_bezorgd", "tech_quests_guhtje",
      "tech_quests_fluitje"]),
    ("tech_quests_knabbelmachine", "De Grote Knabbelmachine", "item:guhs:perfecte_knabbel", None),
]
VINK = [{"type": "checkmark"}]
# the five stages of De Grote Knabbelmachine: (counter name, Dutch name, amount) twice per stage: Knabbelmachine.FASEN
# (features/tech_quests.py checks them against the Java side)
LEVERINGEN = [
    [("steen", "stenen (keisteen, zwartsteen of diepleisteen)", 256), ("hout", "boomstammen", 128)],
    [("grillkool", "grillkool", 96), ("frituur", "gefrituurde kaasknabbels", 128)],
    [("meel", "knabbelmeel", 128), ("draad", "Guhdraad", 64)],
    [("drankje", "Guhdrankjes (welke je wilt)", 12), ("glas", "glas", 128)],
    [("gloeister", "gloeister", 1), ("knabbels", "kaasknabbels", 256)],
]
ZOUT = 4                         # (UitvinderRol.ZOUT)
# Everything from the Zout tier on lies in the Guhbarbecuether, and the way there is the grill portal, which only works for
# who may go through (today: chapter 1 of the Knabbelring; that gate gets more conditions later, so the text names the
# portal, not the conditions: the portal itself tells a player it refuses what must happen first).
PORTAAL = ("&7Daar kom je alleen &eeerst door het grillportaal&7. Laat het je nog niet door? Dan vertelt het portaal zelf wat er eerst "
           "moet gebeuren.&r")


def breng(fase):
    """"Breng 256 stenen en 128 boomstammen" for a stage (1..5)."""
    (_, a, na), (_, b, nb) = LEVERINGEN[fase - 1]
    return f"Breng {na} {a} en {nb} {b}"


def ftb(fq):
    q, item, adv, structure = fq.q, fq.item, fq.adv, fq.structure
    g = vadskracht.getal
    rad, blij = g("GUHRAD"), g("GUHRAD_BLIJ")
    maxen = vadskracht.bron_max()

    # === Project 1: Nooit meer zelf bakken (Knutselen) =====================================================================
    q("tech_quests_rad", "Een guh aan het werk",
      f"Alles begint met een &dGuhrad&r (zie &dGuhs & basis&r). Zet het neer en klik erop met een opgepakte guh: hij rent, en dat geeft "
      f"&b{rad} vadskracht&r. Voor altijd: een guh wordt nooit moe en hoeft niet te eten. Klik met een lege hand om je guh terug te pakken.",
      "guhs:guh_wheel", [item("guhs:guh_wheel")], deps=["intro_guhs_techniek"], shape="hexagon")
    q("tech_quests_draad", "Guhdraad trekken",
      "Redstone + roze kleurstof + een kaasknabbel = 3 &dGuhdraad&r. Leg het van je rad naar je machines; een machine pal naast het rad "
      "heeft geen draad nodig. Alles wat aan elkaar vastzit is één &bopstelling&r.",
      "guhs:guh_wire", [item("guhs:guh_wire", 6)], deps=["tech_quests_rad"])
    q("tech_quests_oven", "De oven met een snoetje",
      f"Hang een &dGuhoven&r aan de draad. Hij bakt alles wat een gewone oven bakt, zonder kolen, op {g('GUH_OVEN')} vadskracht. Elke machine "
      "heeft een &dsnoet&r: hij slaapt zonder kracht, kijkt blij als hij werkt en verbaasd als hij vol zit.",
      "guhs:guh_oven", [item("guhs:guh_oven")], deps=["tech_quests_draad"])
    q("tech_quests_aflezen", "Kijken is weten",
      f"Kijk naar een rad, een draad of een machine: onder je vizier staat &bDeze opstelling gebruikt {g('GUH_OVEN')}/{rad} vadskracht&r. "
      "Vraagt je opstelling méér dan je guhs bij elkaar rennen, dan gaat het niet langzamer: dan staat &calles stil&r, en je leest hoeveel je "
      "tekortkomt. Een meter heb je niet nodig. Gezien? Vink hem af.",
      "minecraft:spyglass", VINK, deps=["tech_quests_oven"], shape="circle")
    q("tech_quests_trechters", "Nooit meer zelf bakken",
      "Het eerste project: zet een kist met een &etrechter&r boven je Guhoven en een trechter eronder naar een tweede kist. Alles wat je in "
      "de bovenste kist gooit, ligt even later gebakken in de onderste, en je guh rent gewoon door terwijl jij iets anders doet. Later "
      "vervang je de trechters door Knabbelbuizen.",
      "minecraft:hopper", [item("minecraft:hopper", 2)], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_aflezen"], shape="gear", xp=100)
    q("tech_quests_klus_machines", "Collega's met een snoet",
      "Woont er een guh in je &dGuhhuisje&r? Dan doet hij je machines: het klusje &eMachines bijvullen & leeghalen&r. Het staat &cuit&r tot "
      "jij het bij je bewoner aanzet. Doe het één keer voor: leg zelf een stapeltje in de machine, dan onthoudt hij wat erin hoort en vult "
      "hij bij uit de kist naast het huisje of uit je Bank Guh. Wat klaarligt, haalt hij op. Alleen bij machines van jou in zijn klus-area. "
      "Njeg, wie heeft er dan nog trechters nodig?",
      "guhs:guhhuisje_klein", [adv("tech_klusjes_machines")], deps=["tech_quests_trechters"])
    q("tech_quests_klus_oogst", "Pompoenenplukker",
      "Een bewoner met het klusje &eFarmen&r oogst meer dan graan: ook &epompoenen&r en &emeloenen&r aan hun stengel, &esuikerriet&r, "
      "&ecacao&r, &enetherwrat&r en de &escheutjes&r uit de Guhbarbecuether. Hij laat de plant staan, dus alles groeit vanzelf weer aan. "
      "Laat hem er één oogsten. Njeg, pompoentaart!",
      "minecraft:pumpkin", [adv("tech_klusjes_oogst")], deps=["tech_quests_klus_machines"])
    q("tech_quests_molen", "Malen zonder wind",
      f"De &dVadsmolen&r (een guh-molentje, ijzer, Guhdraad, steen en een knabbel) maalt op {g('MOLEN')} vadskracht: knabbelgraan tot "
      "knabbelmeel, botten tot beendermeel, grillspiesen tot poeder, keisteen tot grind en grind tot zand. En hij geeft meer dan malen "
      "met de hand. Maal er iets mee!",
      "guhs:vadsmolen", [adv("tech_machines_gemalen")], deps=["tech_quests_trechters"])
    q("tech_quests_knuffel", "Blije guhs rennen harder",
      f"De &dKnuffelgenerator&r is een groot roze kussen: tamme guhs in de buurt komen er vanzelf op liggen en geven "
      f"{g('KNUFFEL_PER_GUH')} vadskracht per guh (hooguit {g('KNUFFEL_MAX_GUHS')} guhs). En wie net op het kussen lag is &dblij&r: stop zo'n "
      f"guh in je rad en hij geeft {blij} in plaats van {rad}.",
      "guhs:knuffelgenerator", [adv("tech_bronnen_knuffel")], deps=["tech_quests_trechters"])
    q("tech_quests_disco", "Dansen voor de fabriek",
      f"De &dDisco-dynamo&r is een dansvloer van 3 bij 3 met een draaitafel. Klik erop met een muziekplaat: hij draait hem steeds opnieuw, "
      f"tot {g('DISCO_MAX_GUHS')} guhs komen dansen en elk geeft {g('DISCO_PER_GUH')} vadskracht. Elke plaat heeft een eigen lichtshow.",
      "guhs:disco_dynamo", [adv("tech_bronnen_disco")], deps=["tech_quests_knuffel"])
    q("tech_quests_hoeveel", "Hoeveel mag eraan?",
      f"Per opstelling tellen hooguit &e{maxen['guhrad']}&r Guhraden, &e{maxen['knuffelgenerator']}&r Knuffelgeneratoren en "
      f"&e{maxen['disco_dynamo']}&r Disco-dynamo's mee (later ook {maxen['blubkacheltje']} Blubkacheltjes en {maxen['gloeisterkern']} "
      "Gloeisterkern). Wat je er meer aan hangt, slaapt, en dat lees je af. Verhaalguhs zijn sterker: de Baltoguh rent harder, Guhtwo laat "
      "het rad zweven. Meer nodig? Bouw een tweede opstelling.",
      "minecraft:oak_sign", VINK, deps=["tech_quests_disco"], shape="circle")

    # === the Oude Guhrad-centrale: the practice hall ========================================================================
    q("tech_quests_centrale_vind", "De Oude Guhrad-centrale",
      "In de &6Guhbarbecuether&r staat een oude krachtcentrale met een koperen dak en twee schoorstenen. Vijf oude guhs rennen er nog "
      "altijd hun rondjes. Het superkompas (Barbecue > Oude Guhrad-centrale) wijst de weg. " + PORTAAL,
      "minecraft:waxed_weathered_cut_copper", [structure("oude_guhrad_centrale")], rewards=(("guhs:kaas_knabbels", 12),),
      deps=["intro_guhs_techniek"], shape="hexagon", xp=100)
    wereld.ftb_questlijn(fq, "tech_quests", "techniek", [
        ("De Uitvinder-guh", "Praat met de &dUitvinder-guh&r in zijn werkplaats. Zijn oefenhal ligt in puin: vijf opstellingen, allemaal "
                             "kapot. Je krijgt twee stukjes Guhdraad mee.", "guhs:guh_wire"),
        ("De losse draad", "&eOpstelling 1.&r Het rad rent, de oven slaapt: er mist een stukje draad. Leg &dGuhdraad&r op de gele tegel. "
                           "(Elke opstelling gaat vanzelf weer stuk als je wegloopt: de volgende speler wil ook oefenen.)", "guhs:guh_oven"),
        ("Te zwaar!", "&eOpstelling 2.&r Twee ovens en een molen aan één rad: te zwaar, dus alles staat stil. Kijk naar een draad en lees "
                      "het af. Knip dan een draad door bij de rode wol.", "minecraft:shears"),
        ("Zout voor de buizen", f"Breng de Uitvinder-guh {ZOUT} &dzoutkristallen&r. Die hak je uit de kristalader van de &6Zoutkristalmijn&r, of "
                                "uit zoutkristalerts in de rotsen van de Guhbarbecuether.", "guhs:zoutkristal"),
        ("Achterstevoren", "&eOpstelling 3.&r De knabbels moeten naar de lege ton, maar het &dRichtingstuk&r wijst de verkeerde kant op. "
                           "Sluip en klik erop met een lege hand.", "guhs:knabbelbuis_richting"),
        ("Het filter staat verkeerd", "&eOpstelling 4.&r Het &dFilterstuk&r laat alles door behalve knabbels: er rolt alleen papier de "
                                      "knabbelton in. Klik erop en zet het van &ealles behalve&r op &ealleen deze&r.", "guhs:knabbelbuis_filter"),
        ("De slang is zoek", "&eOpstelling 5.&r De pomp slurpt kaassaus, maar er mist een stuk &dSausslang&r naar het vat. De Uitvinder-guh "
                             "geeft er een: leg hem op de gele tegel en wacht op de saus.", "guhs:sausslang"),
        ("De oefenhal draait weer", "Vertel het de Uitvinder-guh. Je krijgt het &dBodemloos Knabbelmaagje&r voor je Bank Guh en zijn drie "
                                    "&dreceptkaarten&r: daarmee maak je de machines van projecten 3 en 4.", "guhs:bank_upgrade")],
        na=["tech_quests_centrale_vind"], eind=(("guhs:kaas_knabbels", 24),))
    q("tech_quests_maagje", "Een maag zonder bodem",
      "Klik met het &dBodemloos Knabbelmaagje&r op je neergezette Bank Guh: de grens van 256 per soort is weg, voor altijd (ook als je hem "
      "oppakt). En pas dan mag een &dFilterstuk&r er ook spullen &euit&r happen: jij zet erop wat het mag pakken en hoeveel er moet blijven "
      "liggen. Een trechter onder je bank, een gewoon Richtingstuk of een ophaal-Haltepaaltje krijgt nooit iets uit je bank.",
      "guhs:bank_upgrade", [adv("bank_opgevoerd")], deps=["tech_quests_techniek_8"], shape="rsquare")
    q("tech_quests_kaarten", "Drie receptkaarten",
      "Een receptkaart leg je linksboven in het werkbankrooster bij het recept: hij &eblijft liggen&r, dus één kaart is genoeg voor altijd. "
      "Kwijt? Sluip en klik op de Uitvinder-guh, dan verkoopt hij ze opnieuw.",
      "guhs:techquest_recept_machines", [item("guhs:techquest_recept_saus"), item("guhs:techquest_recept_machines"), item("guhs:techquest_recept_bezorg")],
      deps=["tech_quests_techniek_8"], shape="diamond")

    # === Project 2: Alles vanzelf in de bank (Zout) =========================================================================
    q("tech_quests_zout", "Een zak zout",
      "De slimme machines vragen &dzoutkristal&r uit de &6Guhbarbecuether&r. Haal een voorraadje: de kristalader van de Zoutkristalmijn "
      "groeit voor iedere speler vanzelf weer aan. " + PORTAAL,
      "guhs:zoutkristal", [item("guhs:zoutkristal", 8)], deps=["intro_guhs_techniek"], shape="hexagon")
    q("tech_quests_buis", "Knabbelbuizen",
      "Glas, kaasknabbels en zoutkristal geven 8 &dKnabbelbuizen&r. Ze zijn doorzichtig: je ziet je spullen erdoor rollen. Een buis "
      "plakt vanzelf vast aan kisten, machines, een Bank Guh en andere buizen, en hij heeft geen vadskracht nodig.",
      "guhs:knabbelbuis", [adv("guhs:techniek/tech_buizen_buis")], deps=["tech_quests_zout"])
    q("tech_quests_richting", "Van kist naar kist",
      "Een buis doet niks tot er een &dRichtingstuk&r aan zit. Zet het tegen een kist (de pijl wijst ervan af): het haalt er steeds één "
      "ding uit en stuurt het door de buizen naar de dichtstbijzijnde kist of machine waar het past. Sluip en klik om het om te draaien. "
      "Een redstonesignaal zet het op slot.",
      "guhs:knabbelbuis_richting", [item("guhs:knabbelbuis_richting")], deps=["tech_quests_buis"])
    q("tech_quests_filter", "Het Filterstuk",
      f"Een &dFilterstuk&r is een Richtingstuk met een lijstje (en een snoet: {g('BUISFILTER')} vadskracht). Kies &ealleen deze&r of "
      "&ealles behalve&r, en met &elaat liggen&r blijft er altijd een aantal achter in de kist.",
      "guhs:knabbelbuis_filter", [adv("guhs:techniek/tech_buizen_filter")], deps=["tech_quests_richting"])
    q("tech_quests_sorteer", "Een opslag die zichzelf sorteert",
      "Project: één inleverkist met een Richtingstuk, een lange buis langs je kisten, en vóór elke kist een Filterstuk met &ealleen deze&r. "
      "Wat op een lijstje staat gaat naar die kist; de rest rolt door naar de restkist aan het eind. Gooi je hele rugzak in de inleverkist "
      "en loop weg. Werkt hij? Vink hem af.",
      "minecraft:chest", VINK, rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_filter"], shape="gear", xp=100)
    q("tech_quests_sleutel", "De Banksleutel",
      "Klik met een &dBanksleutel&r (zoutkristal, goud, een knabbel) op je Bank Guh: de sleutel onthoudt welke bank het is. Je raakt hem "
      "niet kwijt en je kunt er zoveel Hapluikjes mee koppelen als je wilt.",
      "guhs:bank_sleutel", [item("guhs:bank_sleutel")], deps=["tech_quests_richting"])
    q("tech_quests_hapluikje", "Alles vanzelf in de bank",
      f"Het &dHapluikje&r ({g('HAPLUIKJE')} vadskracht) is een mondje van je Bank Guh: klik er met de Banksleutel op en alles wat erin gaat "
      "(uit je hand, een trechter, een buis, klusguhs, een Bezorgguhtje) belandt in je bank. Hoe ver weg ook, zelfs in een andere dimensie. "
      "Zet er een bij je mijn, je akker en je oven. Vol is vol: wat niet past, blijft waar het was.",
      "guhs:hapluikje", [adv("bank_gehapt")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_sleutel"], shape="gear", xp=100)
    q("tech_quests_klus_luikje", "Hap, opgeruimd!",
      "Staat er een werkend &dHapluikje&r in het klusgebied van je &dGuhhuisje&r? Dan stoppen je bewoners de buit van hun klusjes daarin "
      "als er geen Bank Guh in de buurt staat (of als die vol zit): de oogst, het hout, wat ze opruimen. Zo ligt alles in je bank, hoe ver die ook weg "
      "staat. Laat een bewoner iets door het luikje happen.",
      "guhs:hapluikje", [adv("tech_klusjes_luikje")], deps=["tech_quests_hapluikje"])
    q("tech_quests_opzuiger", "De Opzuiger",
      f"De &dOpzuiger&r ({g('OPZUIGER')} vadskracht) slurpt alles op wat binnen vier blokken los op de grond ligt. Zet hem onder je "
      "boomgaard of naast je guhs, met een buis naar een Hapluikje.",
      "guhs:opzuiger", [adv("guhs:techniek/tech_buizen_opzuiger")], deps=["tech_quests_hapluikje"])
    q("tech_quests_oogster", "De akker die zichzelf oogst",
      f"De &dOogster&r ({g('OOGSTER')} vadskracht) knipt alles wat rijp is in het veld van 5 bij 5 vóór zijn snoet, en laat het geplant "
      "staan: graan, knabbelgraan, wortels, bessen, pompoenen, suikerriet, pepers... Buis erachter, Hapluikje eraan: je oogst ligt in de "
      "bank voor je thuis bent.",
      "guhs:oogster", [adv("tech_machines_geoogst")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_hapluikje"], shape="gear", xp=100)
    q("tech_quests_sensor", "Genoeg is genoeg",
      f"Vier sensoren ({g('SENSOR')} vadskracht per stuk) geven een redstonesignaal: de &dVoorraadmeter&r (zit er genoeg van iets in een "
      "kist of bank?), de &dSnuffelsensor&r (guhs, Mika's of spelers dichtbij), de &dGuhklok&r en de &dGuhteller&r. Een signaal zet een "
      "Richtingstuk op slot: zo stopt je lijn vanzelf als de kist vol genoeg is.",
      "guhs:voorraadmeter", [adv("guhs:techniek/tech_buizen_sensor")], deps=["tech_quests_filter"])
    q("tech_quests_batterij", "De Knabbelbatterij",
      "Wat je guhs te veel rennen, spaart de &dKnabbelbatterij&r op (goed voor een half uur van één Guhrad). Komt je opstelling even "
      "tekort, dan springt hij bij. Hij houdt zijn lading als je hem oppakt.",
      "guhs:knabbelbatterij", [item("guhs:knabbelbatterij")], deps=["tech_quests_sensor"])
    q("tech_quests_magazijn", "De bank als magazijn",
      "Met het Bodemloos Knabbelmaagje (van de Uitvinder-guh) mag een Filterstuk ook uit je Bank Guh happen. Zet er een Filterstuk tegen, "
      "kies wat eruit mag en zet &elaat liggen 64&r: de bank voert je ovens en molens, maar houdt altijd een stapel voor jou achter. Een "
      "trechter of een gewoon Richtingstuk krijgt niks uit je bank: dat kan alleen het Filterstuk.",
      "guhs:bank_guh", VINK, deps=["tech_quests_hapluikje"], shape="circle")

    # === Project 3: Saus uit de kraan (Saus) ================================================================================
    q("tech_quests_saus_kaart", "Saus en slangen",
      "Voor dit project heb je de &dreceptkaart: saus en slangen&r nodig (van de Uitvinder-guh, na zijn oefenhal), en &dgrillspiesen&r of "
      "&dblubroom&r uit de Guhbarbecuether. &dSausslangen&r maak je zonder kaart: gedroogde kelp, koper en een grillspies geven er 8. "
      + PORTAAL,
      "guhs:techquest_recept_saus", [item("guhs:techquest_recept_saus")], deps=["intro_guhs_techniek"], shape="hexagon")
    q("tech_quests_pomp", "De Sauspomp",
      f"Zet de &dSauspomp&r ({g('POMP')} vadskracht) óp een bronblok kaassaus, kaasfrituursaus of water. Hij pompt een emmer per twee "
      "seconden en de bron raakt nooit op. Met &dSausslangen&r duwt hij de saus naar alles wat eraan vastzit.",
      "guhs:sauspomp", [adv("tech_vloeistof_gepompt")], deps=["tech_quests_saus_kaart"])
    q("tech_quests_vat", "Saus uit de kraan",
      f"Een &dSausvat&r bewaart {tech_vloeistof.emmers(tech_vloeistof.getal('VAT'))} van één saus (ook melk, uit een emmer). Tap er met een "
      "lege emmer uit, thuis, zonder ooit nog naar de sauszee te lopen. Het vat houdt zijn saus als je het oppakt.",
      "guhs:sausvat", [adv("tech_vloeistof_getapt")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_pomp"], shape="gear", xp=100)
    q("tech_quests_frituur", "Frituur aan de lopende band",
      f"De &dFrituurautomaat&r ({g('FRITUUR')} vadskracht) slurpt kaasfrituursaus uit de slang en frituurt alles wat je erin stopt: "
      "kaasknabbels en guh-vis. Buis erin, buis eruit, en er ligt altijd iets lekkers klaar.",
      "guhs:frituurautomaat", [adv("tech_vloeistof_gefrituurd")], deps=["tech_quests_vat"])
    q("tech_quests_brouw", "Drankjes die zichzelf brouwen",
      f"De &dBrouwautomaat&r ({g('BROUWKETEL')} vadskracht) maakt van een emmer kaassaus, drie flesjes en één ingrediënt drie "
      "&dGuhdrankjes&r. Voer hem met buizen flesjes en ingrediënten, en haal de drankjes er aan de andere kant uit.",
      "guhs:brouwautomaat", [adv("tech_vloeistof_gebrouwen")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_vat"], shape="gear", xp=100)
    q("tech_quests_pers", "De grillkoollijn",
      f"De &dGrillkoolpers&r ({tech_vloeistof.getal('GRILLKOOLPERS')} vadskracht, twee blokken hoog) perst een emmer kaasfrituursaus en een "
      "emmer water tot één &dgrillkool&r. Twee pompen, één slang (er mogen twee sauzen door dezelfde slang) en een buis naar je bank: "
      "brandstof en bouwsteen zonder te hakken.",
      "guhs:grillkoolpers", [adv("tech_vloeistof_geperst")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_frituur"], shape="gear", xp=100)
    q("tech_quests_blub", "Het Blubkacheltje",
      f"Een &dSausblubje&r in een potje op een &dBlubkacheltje&r geeft {g('BLUBKACHELTJE')} vadskracht zolang hij warm is: voer hem af en toe "
      f"een kaasknabbel (één knabbel is {tech_bronnen.getal('BLUB_SECONDEN') // 60} minuten; een buis mag hem ook voeren). Sluip en klik "
      "met een lege hand om je blubje terug te krijgen. Geen blubje te vinden? De Verzorger-guh van de &6Sausloper-stal&r geeft er na "
      "zijn questlijn elke dag één.",
      "guhs:blubkacheltje", [adv("tech_bronnen_blub")], deps=["tech_quests_saus_kaart"])

    # === Project 4: Een fabriek die doorwerkt (Saus) ========================================================================
    q("tech_quests_machine_kaart", "Knabbelende machines",
      "De &dreceptkaart: knabbelende machines&r (van de Uitvinder-guh) is voor de Knabbelaar, de Neerzetter, de Knutselmachine en de "
      "Tekentafel. Machines die de wereld veranderen doen dat nooit in beschermde gebouwen of in het klusgebied van andermans Guhhuisje.",
      "guhs:techquest_recept_machines", [item("guhs:techquest_recept_machines")], deps=["intro_guhs_techniek"], shape="hexagon")
    q("tech_quests_knabbelaar", "Steen zonder houweel",
      f"De &dKnabbelaar&r ({g('KNABBELAAR')} vadskracht) knaagt het blok vóór zijn snoet weg, elke twee seconden één, met de buit van een "
      "ijzeren houweel. Zet hem voor een keisteenmaker (water en lava) en je hebt een steenlijn; een Vadsmolen erachter maakt er grind en "
      "zand van. Kisten en machines eet hij nooit op.",
      "guhs:knabbelaar", [adv("tech_machines_geknabbeld")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_machine_kaart"], shape="gear", xp=100)
    q("tech_quests_neerzetter", "De Neerzetter",
      f"De &dNeerzetter&r ({g('NEERZETTER')} vadskracht) zet de blokken uit zijn buikje vóór zich neer, één per seconde, zoals jij dat zou "
      "doen (zaadjes alleen op akkergrond). Samen met een Knabbelaar: neerzetten, knabbelen, neerzetten...",
      "guhs:neerzetter", [adv("tech_machines_neergezet")], deps=["tech_quests_knabbelaar"])
    q("tech_quests_plantagebak", "Een bos in een bak",
      f"De &dPlantagebak&r ({g('PLANTAGEBAK')} vadskracht, 3 bij 3) laat elk boompje binnen een minuut groeien: gewone bomen, modbomen, "
      "saté- en worstzwammetjes. Klik met een bijl en de hele boom ligt in je zakken; de zaailingen plant hij zelf opnieuw. (Zijn "
      "receptkaart krijg je op de &6Grillcamping&r.)",
      "guhs:plantagebak", [adv("tech_machines_boom")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_machine_kaart"], shape="gear", xp=100)
    q("tech_quests_klus_plantage", "Houthakkertje guh",
      "Geen zin om zelf te hakken? Een bewoner van je &dGuhhuisje&r met het klusje &ePlantage: planten & hakken&r hakt de boom op je "
      "Plantagebak in één keer om en brengt het hout naar de kist of je Bank Guh. Is de bak leeg, dan haalt hij zaailingen uit de kist. "
      "Andere bomen laat hij lekker staan. Staat er een werkend Hapluikje in de klus-area, dan hapt dat de buit van al zijn klusjes door "
      "naar je bank. Krak, njeg, klaar!",
      "minecraft:iron_axe", [adv("tech_klusjes_plantage")], deps=["tech_quests_plantagebak"])
    q("tech_quests_tekening", "De Tekentafel",
      "Leg een recept in het rooster van de &dTekentafel&r en een lege &dBouwtekening&r (papier + blauwe kleurstof) erbij: het recept "
      "staat erop getekend. Het kost alleen het vel, niks uit het rooster.",
      "guhs:tekentafel", [adv("tech_machines_tekening")], deps=["tech_quests_machine_kaart"])
    q("tech_quests_knutsel", "Knutselen zonder handen",
      f"Stop de Bouwtekening in een &dKnutselmachine&r ({g('KNUTSELMACHINE')} vadskracht) en voer hem de ingrediënten met buizen: hij "
      "knutselt door zolang er spullen zijn. Guhdraad uit redstone, blokken uit knabbels, flesjes uit glas: wat je maar tekent.",
      "guhs:knutselmachine", [adv("tech_machines_geknutseld")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_tekening"], shape="gear", xp=100)
    q("tech_quests_bezorg_kaart", "Het Bezorgguhtje",
      "De &dreceptkaart: het Bezorgguhtje&r is voor het Stepstation en de Haltepaaltjes. Buizen zijn voor dichtbij; een Bezorgguhtje "
      f"brengt spullen tot {tech_bezorg.getal('BEREIK')} blokken ver, om hoekjes en over heuvels.",
      "guhs:techquest_recept_bezorg", [item("guhs:techquest_recept_bezorg")], deps=["intro_guhs_techniek"], shape="hexagon")
    q("tech_quests_station", "Het Stepstation",
      f"In een &dStepstation&r ({g('STEPSTATION')} vadskracht) woont één Bezorgguhtje: een mini-guh op een step met een veel te grote "
      "rugzak. Zet het neer en hij is er meteen.",
      "guhs:stepstation", [adv("tech_bezorg_station")], deps=["tech_quests_bezorg_kaart"])
    q("tech_quests_halte", "Haltepaaltjes",
      "Zet een &dHaltepaaltje&r naast of op een kist of machine. Groen = hier haalt hij op, oranje = hier levert hij af; in het paaltje "
      f"kies je wat (leeg = alles). Tot {tech_bezorg.getal('MAX_HALTES')} haltes per station.",
      "guhs:haltepaaltje", [adv("tech_bezorg_halte")], deps=["tech_quests_station"])
    q("tech_quests_bezorgd", "De bezorgronde",
      "Laat je Bezorgguhtje iets afleveren: van je akker naar je oven, van je steenlijn naar een Hapluikje. Hij pakt alleen mee wat hij "
      "ook ergens kwijt kan, en wat in zijn rugzak zit raakt nooit zoek.",
      "guhs:bezorgguhtje_spawn_egg", [adv("tech_bezorg_bezorgd")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_halte"], shape="gear", xp=100)
    q("tech_quests_guhtje", "Guhdex: het Bezorgguhtje",
      "Kom dicht bij een Bezorgguhtje en hij staat in je &dGuhdex&r. Geef hem een kaasknabbel en hij stept een minuut lang extra hard.",
      "guhs:guhdex", [adv("seen_bezorgguhtje")], deps=["tech_quests_station"], shape="circle")
    q("tech_quests_fluitje", "Fluiten naar je pakketje",
      "Met het &dBezorgguhtje-fluitje&r (van de Torenwachter-guh in de &6Rookguh-vuurtoren&r) komt je dichtstbijzijnde Bezorgguhtje naar "
      "je toe en gaat zijn rugzak open. Sluip en fluit: allemaal naar huis.",
      "guhs:bezorgguhtje_fluitje", [adv("tech_bezorg_fluitje")], deps=["tech_quests_bezorgd"], shape="diamond")

    # === De Grote Knabbelmachine (Gloeister) ================================================================================
    q("tech_quests_gloeister", "Een gloeister",
      "De laatste laag begint met een &dgloeister&r: die laat alleen de &cAangebrande Mika&r vallen, de baas van het Mika-grillpaleis in "
      "de &6Guhbarbecuether&r. " + PORTAAL,
      "guhs:gloeister", [item("guhs:gloeister")], deps=["intro_guhs_techniek"], shape="hexagon")
    q("tech_quests_kern", "De Gloeisterkern",
      f"Eén gloeister, grillkool en zoutkristal maken een &dGloeisterkern&r: &b{g('GLOEISTERKERN')} vadskracht&r, voor altijd, zonder guh. "
      f"Er telt er {maxen['gloeisterkern']} per opstelling mee. Daar draait een hele fabriek op.",
      "guhs:gloeisterkern", [adv("tech_bronnen_kern")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_gloeister"], shape="gear", xp=100)
    wereld.ftb_questlijn(fq, "tech_quests", "knabbelmachine", [
        ("Het grote plan", "Heb je de oefenhal af én de Aangebrande Mika verslagen? Praat dan met de &dUitvinder-guh&r: hij wil op het "
                           "bordes &6De Grote Knabbelmachine&r bouwen. Iedere speler bouwt zijn eigen: alleen jij ziet de jouwe groeien.",
         "guhs:grote_knabbelmachine"),
        ("De fundering", breng(1) + ". In porties mag, hij telt alles. Tip: de steenlijn en een bos in een bak.", "minecraft:cobblestone"),
        ("De ketel", breng(2) + ". Tip: de grillkoollijn en de Frituurautomaat.", "guhs:grillkool"),
        ("De maag", breng(3) + ". Tip: een Oogster bij je knabbelgraan, een Vadsmolen, en een Knutselmachine met een Bouwtekening van "
                               "Guhdraad.", "guhs:knabbelmeel"),
        ("De snoet", breng(4) + ". Tip: de Brouwautomaat, en zand in een Guhoven die zichzelf voert.", "minecraft:glass"),
        ("Het gloeisterhart", breng(5) + ". Dan gaan zijn ogen open...", "guhs:gloeister")],
        na=["tech_quests_gloeister"], eind=(("guhs:kaas_knabbels", 32),))
    q("tech_quests_perfect", "De perfecte knabbel",
      "Jouw Grote Knabbelmachine maakt elke dag één &dperfecte knabbel&r. Klik op het bakje onder zijn bek. Je bent nu ook "
      "&6Knabbelmachinist&r: kies de titel in je Guhdex.",
      "guhs:perfecte_knabbel", [adv("tech_quests_perfecte_knabbel")], rewards=(("guhs:kaas_knabbels", 16),), deps=["tech_quests_knabbelmachine_6"],
      shape="gear", xp=200)
    q("tech_quests_beeldje", "De machine op de kast",
      "Zet het &dKnabbelmachine-beeldje&r neer waar iedereen het ziet. Klik erop: hij knabbelt.",
      "guhs:knabbelmachine_beeldje", [adv("tech_quests_beeldje")], deps=["tech_quests_knabbelmachine_6"], shape="diamond")
