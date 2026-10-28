"""
1.8.0: the Guh-technologie chapter (guhs_techniek) in tiers, with short texts. make_ftbquests.py calls toepassen(fq) once all
feature modules have added their quests: it adds a few quests for blocks that had none, gives every quest of the chapter a
new short Dutch title and text, places them in the tier sections below (SECTIONS["guhs_techniek"], so the sections of the
modules themselves stay empty) and links each section's quests one after another (only the lines: nothing is locked).

The quest keys stay the same, so the quest ids (qid(key)) and the progress of every player stay the same too; only the
sections, the order and the texts change. The English: EN at the bottom, and tools/lang/en/c218_ftb_techniek.json.

  Tier 1  Vadskracht thuis          everything you build from things of the overworld and the Guhmensie
  Tier 2  Met de Barbecuether       zoutkristal, grillspiesen, the receptkaarten and the gloeister
  Tier 3  Grote projecten           the Koepel, the Mega-knuffelcentrale, the Hamsterbuis, the Kaasraket and the fossils
  Tier 4  Het Guheinde              the werkplaats of the Werktuigguh and the Vredesballon
"""

TIERS = {1: "lime", 2: "orange", 3: "cyan", 4: "purple"}

# (sid, tier, title, portrait or "<module>" = the portrait of that module's own section, upstream, [quest keys])
SECTIES = [
    ("t1_begin", 1, "Tier 1: Je eerste vadskracht", "item:guhs:guh_wire", "intro",
     ["tech_quests_rad", "tech_quests_draad", "tk_schakelaar", "tech_quests_oven", "tech_quests_vadslamp",
      "tech_quests_aflezen", "tech_quests_hoeveel", "tech_quests_beurs"]),
    ("t1_kracht", 1, "Tier 1: Meer kracht, minder werk", "item:guhs:block/guh_oven_front_on", "tech_quests_oven",
     ["tech_quests_trechters", "tech_quests_molen", "tech_quests_knuffel", "tech_quests_disco", "vb_knabbelketel",
      "vb_antenne"]),
    ("t1_klusjes", 1, "Tier 1: Guhs aan het werk", "guh:normal", "tech_quests_oven",
     ["tech_quests_klus_machines", "tech_quests_klus_oogst", "kaaskas_band", "tk_kassier"]),
    ("t1_wol", 1, "Tier 1: Kaaskas & wolfabriek", "item:guhs:kaaskas", "tech_quests_draad",
     ["kaaskas_kas", "kaaskas_pluis", "fabriek_scheerstal", "fabriek_recycler", "fabriek_spin", "fabriek_verf",
      "fabriek_kleurmolen", "fabriek_regenboogpers", "fabriek_bouwblokken"]),
    ("t2_centrale", 2, "Tier 2: De Oude Guhrad-centrale", "npc:uitvinderguh", "bbq_dimensie",
     ["tech_quests_centrale_vind", "tech_quests_techniek_1", "tech_quests_techniek_2", "tech_quests_techniek_3",
      "tech_quests_techniek_4", "tech_quests_techniek_5", "tech_quests_techniek_6", "tech_quests_techniek_7",
      "tech_quests_techniek_8", "tech_quests_maagje", "tech_quests_kaarten"]),
    ("t2_buizen", 2, "Tier 2: Buizen & de bank", "item:guhs:block/hapluikje_voor_werkt", "tech_quests_techniek_4",
     ["tech_quests_zout", "tech_quests_buis", "tech_quests_richting", "tech_quests_filter", "tech_quests_sorteer",
      "tech_quests_sleutel", "tech_quests_hapluikje", "tech_quests_klus_luikje", "tech_quests_opzuiger",
      "tech_quests_oogster", "tech_quests_sensor", "tech_quests_batterij", "tech_quests_magazijn"]),
    ("t2_saus", 2, "Tier 2: Saus & slangen", "item:guhs:block/sauspomp_voor_werkt", "tech_quests_kaarten",
     ["tech_quests_saus_kaart", "tk_sausslang", "tech_quests_pomp", "tech_quests_vat", "vb_saustank", "vb_sausbuis",
      "tech_quests_frituur", "tech_quests_brouw", "tech_quests_pers", "vb_generator", "tech_quests_blub"]),
    ("t2_machines", 2, "Tier 2: Knabbelende machines", "item:guhs:block/knabbelaar_voor_werkt", "tech_quests_kaarten",
     ["tech_quests_machine_kaart", "tech_quests_knabbelaar", "tech_quests_neerzetter", "tech_quests_plantagebak",
      "tech_quests_klus_plantage", "tech_quests_tekening", "tech_quests_knutsel"]),
    ("t2_bezorg", 2, "Tier 2: Het Bezorgguhtje", "<tech_bezorg>", "tech_quests_kaarten",
     ["tech_quests_bezorg_kaart", "tech_quests_station", "tech_quests_halte", "tech_quests_bezorgd",
      "tech_quests_guhtje", "tech_quests_fluitje"]),
    ("t2_knabbelmachine", 2, "Tier 2: De Grote Knabbelmachine", "item:guhs:perfecte_knabbel", "tech_quests_techniek_8",
     ["tech_quests_gloeister", "tech_quests_kern", "tech_quests_knabbelmachine_1", "tech_quests_knabbelmachine_2",
      "tech_quests_knabbelmachine_3", "tech_quests_knabbelmachine_4", "tech_quests_knabbelmachine_5",
      "tech_quests_knabbelmachine_6", "tech_quests_perfect", "tech_quests_beeldje"]),
    ("t3_koepel", 3, "Tier 3: De Koepel", "<koepel>", "tech_quests_beurs",
     ["koepel_oven", "koepel_station", "koepel_koepel", "koepel_molen", "koepel_vanger"]),
    ("t3_kracht", 3, "Tier 3: Mega-kracht & hamsterbuizen", "item:guhs:mega_knuffelcentrale", "tech_quests_kern",
     ["centrale_knuffel", "centrale_export", "tech_hamsterknooppunt", "tech_hamsterbuis_station"]),
    ("t3_raket", 3, "Tier 3: De Kaasraket", "<raket>", "tech_quests_batterij",
     ["raket_platform", "raket_romp", "raket_motor", "raket_brandstof", "raket_bemanning", "raket_gelanceerd",
      "kaasmaan_vlag"]),
    ("t3_fossiel", 3, "Tier 3: Fossielen & oerguhs", "<fossiel>", "tech_quests_beurs",
     ["fossiel_gevonden", "fossiel_broedmachine", "fossiel_uitgebroed", "fossiel_mammoet", "fossiel_tguh",
      "fossiel_pteroguh", "fossiel_stokstaartje"]),
    ("t4_werkplaats", 4, "Tier 4: De werkplaats", "<einde2_trap5_machines>", "einde2_h4_vuur_guhvatar",
     ["einde2_trap5_machines_werkplaats", "einde2_trap5_machines_zender", "einde2_trap5_machines_terminal",
      "einde2_trap5_machines_bestel", "einde2_trap5_machines_poortbuis", "einde2_trap5_machines_thuispoort",
      "einde2_trap5_machines_elementen", "einde2_trap5_machines_enderguhrad", "einde2_trap5_machines_komeet",
      "einde2_trap5_machines_bronnen", "einde2_trap5_machines_klaar"]),
    ("t4_ballon", 4, "Tier 4: De Vredesballon", "<einde2_trap5_ballon>", "einde2_trap5_machines_klaar",
     ["einde2_trap5_ballon_vredesballon_1", "einde2_trap5_ballon_vredesballon_2", "einde2_trap5_ballon_vredesballon_3",
      "einde2_trap5_ballon_vredesballon_4", "einde2_trap5_ballon_vredesballon_5", "einde2_trap5_ballon_vredesballon_6",
      "einde2_trap5_ballon_vredesballon_7", "einde2_trap5_ballon_beeldje", "einde2_trap5_ballon_zweven",
      "einde2_trap5_ballon_zakje", "einde2_trap5_ballon_titel"]),
]

INTRO = [
    "Dit hoofdstuk komt na &dHet Guhrad&r in &dGuhs & basis&r: een guh die in een rad rent, maakt &bvadskracht&r. "
    "Met &dGuhdraad&r breng je die naar machines met een snoetje.",
    "Het gaat in &6tiers&r: &aTier 1&r bouw je meteen thuis, &6Tier 2&r vraagt spullen uit de &6Guhbarbecuether&r, "
    "&bTier 3&r zijn de grote projecten en &5Tier 4&r ligt in het &5Guheinde&r. Kijk naar een rad, draad of machine en je "
    "ziet hoeveel vadskracht je opstelling gebruikt.",
    "Niks zit op slot: elke quest vinkt zichzelf af zodra je hem gedaan hebt, ook als dat al eerder was. Vink dit af en ga "
    "lekker vadsig aan de slag!",
]

# the quests this chapter didn't have yet: (key, title, item = icon, task: get the item or tick it off); text in TEKST
NIEUW = [
    ("tk_schakelaar", "Aan, uit, aan, uit", "guhs:vadsschakelaar", "item"),
    ("tk_kassier", "De kas plukt zichzelf", "guhs:kaaskas", "check"),
    ("tk_sausslang", "Een slang vol saus", "guhs:sausslang", "item"),
]

# key: (title, text). Short: one to three sentences, a guh joke where it fits.
TEKST = {
    # --- Tier 1: Je eerste vadskracht
    "tech_quests_rad": ("Een guh aan het werk",
        "Zet een &dGuhrad&r neer en klik erop met een opgepakte guh: hij rent voor &b10 vadskracht&r, voor altijd. "
        "Moe wordt hij nooit, eten hoeft niet. Klik met een lege hand om hem terug te pakken."),
    "tech_quests_draad": ("Guhdraad trekken",
        "Redstone, roze kleurstof en een kaasknabbel geven 3 &dGuhdraad&r. Leg het van je rad naar je machines: alles wat "
        "aan elkaar vastzit is één &eopstelling&r. Een machine pal naast het rad heeft geen draad nodig."),
    "tk_schakelaar": ("Aan, uit, aan, uit",
        "Leg een &dVadsschakelaar&r in je draad: klik erop en alles erachter gaat uit of aan. Ook je guh mag dan even "
        "uitpuffen (al wil hij eigenlijk niet)."),
    "tech_quests_oven": ("De oven met een snoetje",
        "De &dGuhoven&r bakt alles wat een gewone oven bakt, zonder kolen, op &b5 vadskracht&r. Zijn snoet laat zien hoe het "
        "gaat: slapen zonder kracht, blij als hij werkt, verbaasd als hij vol zit."),
    "tech_quests_vadslamp": ("Licht met een snoetje",
        "De &dVadslamp&r brandt op &b1 vadskracht&r en verlicht tot 6 blokken om zich heen. Eén lamp en je hele grot is "
        "licht. Monsters balen."),
    "tech_quests_aflezen": ("Kijken is weten",
        "Kijk naar een rad, draad of machine: onder je vizier staat bijvoorbeeld &bDeze opstelling gebruikt 5/10 "
        "vadskracht&r. Vraag je te veel, dan staat alles stil en lees je af hoeveel je tekortkomt. Gezien? Vink hem af."),
    "tech_quests_hoeveel": ("Hoeveel mag eraan?",
        "Per opstelling tellen &e4&r Guhraden helemaal mee (daarna steeds minder, tot &e16&r), &e2&r Knuffelgeneratoren, "
        "&e2&r Disco-dynamo's, &e4&r Blubkacheltjes en &e1&r Gloeisterkern. Meer nodig? Bouw een tweede opstelling."),
    "tech_quests_beurs": ("Op de Guhtechniek-beurs",
        "In de Guhmensie staat een glazen hal waar &delke machine echt werkt&r (Superkompas, onder Knus). Elke stand heeft een "
        "bordje en een beursguh die vertelt welke quest hem leert. Kijken mag, pakken niet!"),
    # --- Tier 1: Meer kracht, minder werk
    "tech_quests_trechters": ("Nooit meer zelf bakken",
        "Een kist met een &etrechter&r boven je Guhoven, een trechter eronder naar een tweede kist. Gooi erin, loop weg, "
        "en even later ligt alles gebakken klaar."),
    "tech_quests_molen": ("Malen zonder wind",
        "De &dVadsmolen&r (&b5 vadskracht&r) maalt knabbelgraan tot meel, botten tot beendermeel en keisteen tot grind en "
        "zand. Hij geeft meer dan malen met de hand. Maal er iets mee!"),
    "tech_quests_knuffel": ("Blije guhs rennen harder",
        "Op de &dKnuffelgenerator&r komen tamme guhs vanzelf liggen: &b3 vadskracht&r per guh, tot 8 guhs. Een guh die net "
        "geknuffeld is, rent daarna &b15&r in plaats van 10 in zijn rad."),
    "tech_quests_disco": ("Dansen voor de fabriek",
        "Klik op de &dDisco-dynamo&r met een muziekplaat: tot 4 guhs komen dansen, elk voor &b5 vadskracht&r. Elke plaat "
        "heeft zijn eigen lichtshow. Njeg, wat een moves."),
    "vb_knabbelketel": ("Kaas in de ketel",
        "De &dKnabbelketel&r stookt kaasknabbels voor &b80 vadskracht&r. Een blok knabbels brandt negen keer zo lang. "
        "Wel een veelvraat: tijd voor een knabbelboerderij!"),
    "vb_antenne": ("Weer in de wijde omtrek",
        "Een &dWeer vadser&r reikt 10 blokken. Zet er een &dWolkenantenne&r op: I, II en III maken dat 20, 35 en 60 "
        "blokken. Elke stap kost meer vadskracht."),
    # --- Tier 1: Guhs aan het werk
    "tech_quests_klus_machines": ("Collega's met een snoet",
        "Zet bij een bewoner van je &dGuhhuisje&r het klusje &eMachines bijvullen & leeghalen&r aan. Leg zelf één keer iets "
        "in een machine: hij onthoudt het en vult bij uit de kist of je Bank Guh. Wie heeft er nog trechters nodig?"),
    "tech_quests_klus_oogst": ("Pompoenenplukker",
        "Een bewoner met het klusje &eFarmen&r oogst ook &epompoenen&r, &emeloenen&r, &esuikerriet&r, &ecacao&r en "
        "&enetherwrat&r, en laat de plant staan. Laat hem er één oogsten. Njeg, pompoentaart!"),
    "kaaskas_band": ("Alles op z'n plek",
        "Leg een &dGuh-sorteerband&r en geef een bewoner van een huisje ernaast het klusje &eSorteerder&r. Hij stopt alles "
        "in de goede kist. Netter dan jouw kamer!"),
    "tk_kassier": ("De kas plukt zichzelf",
        "Geef een bewoner van je &dGuhhuisje&r het klusje &eKassier: de kas plukken&r. Hij loopt je Kaaskas in, plukt alles "
        "wat rijp is en plant het meteen opnieuw. Gezien? Vink hem af."),
    # --- Tier 1: Kaaskas & wolfabriek
    "kaaskas_kas": ("Een kas vol kaas",
        "Zet een &dKaaskas&r tussen je gewassen en geef hem &bvadskracht&r en een emmer &6kaassaus&r. Kijk ze groeien!"),
    "kaaskas_pluis": ("Pluizig!",
        "Plant &fpluisbolletjes&r (zaadjes: tarwezaad + wol) en oogst je eerste &fpluis&r. Zacht, hè?"),
    "fabriek_scheerstal": ("Scheerstal",
        "Zet tamme &dWolkenschaapjes&r rond een &dScheerstal&r: hij scheert ze vanzelf. Tem en kweek ze met "
        "wolkensuikerspin. Bleh!"),
    "fabriek_recycler": ("Niks gaat verloren",
        "Stop oude knuffels, guhkleding, wol of kussens in de &dKnuffelrecycler&r: er komt weer pluis uit."),
    "fabriek_spin": ("Snorrende spinmachine",
        "De &dSpinmachine&r spint &fpluis&r tot wol: 2 pluis is 1 witte wol."),
    "fabriek_verf": ("Plons in het verfbad",
        "Wol en kleurstof in het &dVerfbad&r: het verft alles wat het spel kan verven, ook glas, klei en knuffelblokken."),
    "fabriek_kleurmolen": ("Alle kleuren van de regenboog",
        "De &dKleurmolen&r maalt een bloem tot 2 à 3 kleurstof. Een guh met het klusje &eBloemetjes plukken&r houdt hem "
        "eindeloos vol."),
    "fabriek_regenboogpers": ("Regenboogpers",
        "De &dRegenboogpers&r perst pluis met &6kaassaus&r tot elke kleur. Kies de kleur met sluipen + klikken, of leg een "
        "voorbeeld-kleurstof neer."),
    "fabriek_bouwblokken": ("Bouwblokken in bulk",
        "Leg een voorbeeld in de &dBouwblokken-fabriek&r: hij maakt trappen, platen, muurtjes en gekleurde blokken. Koppel "
        "een Bank Guh of een kist (sluip en klik) en hij pakt zelf zijn spullen."),
    # --- Tier 2: De Oude Guhrad-centrale
    "tech_quests_centrale_vind": ("De Oude Guhrad-centrale",
        "In de &6Guhbarbecuether&r staat een oude centrale met twee schoorstenen, waar vijf oude guhs nog altijd rondjes "
        "rennen. Je Superkompas (Barbecue) wijst de weg."),
    "tech_quests_techniek_1": ("De Uitvinder-guh",
        "Praat met de &dUitvinder-guh&r in zijn werkplaats. Zijn oefenhal ligt in puin: vijf opstellingen, allemaal kapot."),
    "tech_quests_techniek_2": ("De losse draad",
        "&eOpstelling 1:&r het rad rent, de oven slaapt. Leg &dGuhdraad&r op de gele tegel."),
    "tech_quests_techniek_3": ("Te zwaar!",
        "&eOpstelling 2:&r twee ovens en een molen aan één rad. Lees de draad af en knip er één door bij de rode wol."),
    "tech_quests_techniek_4": ("Zout voor de buizen",
        "Breng de Uitvinder-guh 4 &dzoutkristallen&r. Je hakt ze in de &6Zoutkristalmijn&r of uit zoutkristalerts in de "
        "Guhbarbecuether."),
    "tech_quests_techniek_5": ("Achterstevoren",
        "&eOpstelling 3:&r het &dRichtingstuk&r wijst de verkeerde kant op. Sluip en klik erop met een lege hand."),
    "tech_quests_techniek_6": ("Het filter staat verkeerd",
        "&eOpstelling 4:&r het &dFilterstuk&r laat alles door behalve knabbels. Zet het op &ealleen deze&r."),
    "tech_quests_techniek_7": ("De slang is zoek",
        "&eOpstelling 5:&r er mist een stuk &dSausslang&r tussen pomp en vat. Leg de slang van de Uitvinder-guh op de gele "
        "tegel."),
    "tech_quests_techniek_8": ("De oefenhal draait weer",
        "Vertel het de Uitvinder-guh. Je krijgt het &dBodemloos Knabbelmaagje&r en drie &dreceptkaarten&r. Hij is zo trots "
        "dat zijn snorharen trillen."),
    "tech_quests_maagje": ("Een maag zonder bodem",
        "Klik met het &dBodemloos Knabbelmaagje&r op je Bank Guh: de grens van 256 per soort is weg, voor altijd. En nu mag "
        "een Filterstuk ook spullen uit je bank halen."),
    "tech_quests_kaarten": ("Drie receptkaarten",
        "Leg een &dreceptkaart&r linksboven in het werkbankrooster: hij &eblijft liggen&r, dus één is genoeg. Kwijt? De "
        "Uitvinder-guh verkoopt ze opnieuw."),
    # --- Tier 2: Buizen & de bank
    "tech_quests_zout": ("Een zak zout",
        "Bijna alle slimme machines vragen &dzoutkristal&r. De kristalader in de &6Zoutkristalmijn&r groeit vanzelf weer "
        "aan: hak een voorraadje."),
    "tech_quests_buis": ("Knabbelbuizen",
        "Glas, kaasknabbels en zoutkristal geven 8 doorzichtige &dKnabbelbuizen&r. Ze plakken vanzelf aan kisten, machines "
        "en je Bank Guh, zonder vadskracht."),
    "tech_quests_richting": ("Van kist naar kist",
        "Een &dRichtingstuk&r tegen een kist haalt er steeds één ding uit en stuurt het door de buizen. Sluip en klik om het "
        "om te draaien; redstone zet het op slot."),
    "tech_quests_filter": ("Het Filterstuk",
        "Een &dFilterstuk&r is een Richtingstuk met een lijstje: &ealleen deze&r of &ealles behalve&r. Met &elaat liggen&r "
        "blijft er een stapeltje achter."),
    "tech_quests_sorteer": ("Een opslag die zichzelf sorteert",
        "Eén inleverkist, een lange buis langs je kisten en voor elke kist een Filterstuk op &ealleen deze&r. Gooi je hele "
        "rugzak erin en loop weg. Werkt hij? Vink hem af."),
    "tech_quests_sleutel": ("De Banksleutel",
        "Klik met een &dBanksleutel&r op je Bank Guh: de sleutel onthoudt welke bank het is. Koppel er zoveel Hapluikjes "
        "mee als je wilt."),
    "tech_quests_hapluikje": ("Alles vanzelf in de bank",
        "Het &dHapluikje&r is een mondje van je Bank Guh: alles wat erin gaat, belandt in je bank, zelfs in een andere "
        "dimensie. Zet er een bij je mijn, je akker en je oven. Hap!"),
    "tech_quests_klus_luikje": ("Hap, opgeruimd!",
        "Staat er een werkend Hapluikje in het klusgebied van je &dGuhhuisje&r, dan stoppen je bewoners hun buit daarin. "
        "Laat een bewoner iets door het luikje happen."),
    "tech_quests_opzuiger": ("De Opzuiger",
        "De &dOpzuiger&r (&b3 vadskracht&r) slurpt alles op wat binnen vier blokken op de grond ligt. Buis erachter, "
        "Hapluikje eraan, klaar."),
    "tech_quests_oogster": ("De akker die zichzelf oogst",
        "De &dOogster&r (&b8 vadskracht&r) knipt alles wat rijp is in 5 bij 5 voor zijn snoet en laat het geplant staan. "
        "Je oogst ligt in de bank voor je thuis bent."),
    "tech_quests_sensor": ("Genoeg is genoeg",
        "De &dVoorraadmeter&r, &dSnuffelsensor&r, &dGuhklok&r en &dGuhteller&r geven een redstonesignaal. Zet er een "
        "Richtingstuk mee op slot en je lijn stopt vanzelf als de kist vol genoeg is."),
    "tech_quests_batterij": ("De Knabbelbatterij",
        "De &dKnabbelbatterij&r spaart op wat je guhs te veel rennen en springt bij als je even tekortkomt. Een half uur "
        "Guhrad in je broekzak."),
    "tech_quests_magazijn": ("De bank als magazijn",
        "Zet een Filterstuk tegen je Bank Guh, kies wat eruit mag en zet &elaat liggen 64&r. Je bank voert je ovens, maar "
        "houdt altijd een stapel voor jou achter."),
    # --- Tier 2: Saus & slangen
    "tech_quests_saus_kaart": ("Saus en slangen",
        "Voor dit rijtje heb je de &dreceptkaart: saus en slangen&r nodig, en grillspiesen of blubroom uit de "
        "Guhbarbecuether."),
    "tk_sausslang": ("Een slang vol saus",
        "Gedroogde kelp, koper en een grillspies geven 8 &dSausslangen&r (zonder receptkaart). Ze brengen saus van pomp naar "
        "vat en machine. Slurp!"),
    "tech_quests_pomp": ("De Sauspomp",
        "Zet de &dSauspomp&r (&b6 vadskracht&r) op een bron van kaassaus, frituursaus of water. Hij pompt een emmer per "
        "twee seconden en de bron raakt nooit op."),
    "tech_quests_vat": ("Saus uit de kraan",
        "Een &dSausvat&r bewaart 16 emmers van één saus (ook melk). Tap er met een lege emmer uit, thuis."),
    "vb_saustank": ("Een zwembad saus",
        "De &dGrote saustank&r bewaart 64 emmers. De meter op zijn buik laat zien hoe vol hij zit. Niet in zwemmen!"),
    "vb_sausbuis": ("Buizen tot de horizon",
        "Een &dSausbuis&r brengt saus ver weg: hij telt maar als een zestiende slang."),
    "tech_quests_frituur": ("Frituur aan de lopende band",
        "De &dFrituurautomaat&r (&b8 vadskracht&r) frituurt kaasknabbels en guh-vis met frituursaus uit de slang. Buis "
        "erin, buis eruit, altijd iets lekkers."),
    "tech_quests_brouw": ("Drankjes die zichzelf brouwen",
        "De &dBrouwautomaat&r (&b8 vadskracht&r) maakt van kaassaus, drie flesjes en één ingrediënt drie &dGuhdrankjes&r."),
    "tech_quests_pers": ("De grillkoollijn",
        "De &dGrillkoolpers&r perst een emmer frituursaus en een emmer water tot &dgrillkool&r. Twee pompen en één slang "
        "zijn genoeg."),
    "vb_generator": ("Zachte steen",
        "Kaassaus + water = &dknuffelsteen&r. Zet de &dKnuffelsteen-generator&r naast water, geef hem saus via een slang en "
        "vadskracht, en zet er een kist naast."),
    "tech_quests_blub": ("Het Blubkacheltje",
        "Een &dSausblubje&r in een potje op een &dBlubkacheltje&r geeft &b6 vadskracht&r zolang hij warm is. Voer hem af en "
        "toe een kaasknabbel; blub blub."),
    # --- Tier 2: Knabbelende machines
    "tech_quests_machine_kaart": ("Knabbelende machines",
        "De &dreceptkaart: knabbelende machines&r is voor de Knabbelaar, de Neerzetter, de Knutselmachine en de Tekentafel."),
    "tech_quests_knabbelaar": ("Steen zonder houweel",
        "De &dKnabbelaar&r (&b10 vadskracht&r) knaagt elke twee seconden het blok voor zijn snoet weg. Voor een "
        "keisteenmaker heb je een eindeloze steenlijn. Kisten eet hij nooit op."),
    "tech_quests_neerzetter": ("De Neerzetter",
        "De &dNeerzetter&r (&b5 vadskracht&r) zet de blokken uit zijn buikje voor zich neer, één per seconde. Samen met een "
        "Knabbelaar: zetten, knabbelen, zetten..."),
    "tech_quests_plantagebak": ("Een bos in een bak",
        "De &dPlantagebak&r (&b6 vadskracht&r) laat elk boompje binnen een minuut groeien. Klik met een bijl en de hele boom "
        "ligt in je zakken. (Receptkaart: de Grillcamping.)"),
    "tech_quests_klus_plantage": ("Houthakkertje guh",
        "Een bewoner met het klusje &ePlantage: planten & hakken&r hakt de boom op je Plantagebak om en brengt het hout weg. "
        "Krak, njeg, klaar!"),
    "tech_quests_tekening": ("De Tekentafel",
        "Leg een recept in de &dTekentafel&r met een lege &dBouwtekening&r erbij: het recept staat erop getekend. Het kost "
        "alleen het vel."),
    "tech_quests_knutsel": ("Knutselen zonder handen",
        "Stop de Bouwtekening in een &dKnutselmachine&r (&b10 vadskracht&r) en voer hem met buizen: hij knutselt door zolang "
        "er spullen zijn."),
    # --- Tier 2: Het Bezorgguhtje
    "tech_quests_bezorg_kaart": ("Het Bezorgguhtje",
        "De &dreceptkaart: het Bezorgguhtje&r is voor het Stepstation en de Haltepaaltjes. Een Bezorgguhtje brengt spullen "
        "tot 96 blokken ver, om hoekjes en over heuvels."),
    "tech_quests_station": ("Het Stepstation",
        "In een &dStepstation&r (&b4 vadskracht&r) woont één Bezorgguhtje: een mini-guh op een step met een veel te grote "
        "rugzak."),
    "tech_quests_halte": ("Haltepaaltjes",
        "Zet een &dHaltepaaltje&r bij een kist of machine: &agroen&r = ophalen, &6oranje&r = afleveren. Tot 8 haltes per "
        "station."),
    "tech_quests_bezorgd": ("De bezorgronde",
        "Laat je Bezorgguhtje iets afleveren, bijvoorbeeld van je akker naar je oven. Wat in zijn rugzak zit, raakt nooit "
        "zoek."),
    "tech_quests_guhtje": ("Guhdex: het Bezorgguhtje",
        "Kom dicht bij een Bezorgguhtje en hij staat in je &dGuhdex&r. Een kaasknabbel en hij stept een minuut lang extra "
        "hard."),
    "tech_quests_fluitje": ("Fluiten naar je pakketje",
        "Met het &dBezorgguhtje-fluitje&r (van de Torenwachter-guh in de Rookguh-vuurtoren) komt je Bezorgguhtje naar je toe. "
        "Sluip en fluit: allemaal naar huis."),
    # --- Tier 2: De Grote Knabbelmachine
    "tech_quests_gloeister": ("Een gloeister",
        "Een &dgloeister&r laat alleen de &cAangebrande Mika&r vallen, de baas van het Mika-grillpaleis in de "
        "Guhbarbecuether. Heet!"),
    "tech_quests_kern": ("De Gloeisterkern",
        "Gloeister, grillkool en zoutkristal maken een &dGloeisterkern&r: &b200 vadskracht&r, voor altijd, zonder guh. Er "
        "telt er één per opstelling."),
    "tech_quests_knabbelmachine_1": ("Het grote plan",
        "Oefenhal af en de Aangebrande Mika verslagen? Dan wil de Uitvinder-guh &6De Grote Knabbelmachine&r bouwen. Alleen "
        "jij ziet de jouwe groeien."),
    "tech_quests_knabbelmachine_2": ("De fundering",
        "Breng 256 stenen en 128 boomstammen (in porties mag). Tip: een steenlijn en een bos in een bak."),
    "tech_quests_knabbelmachine_3": ("De ketel",
        "Breng 96 grillkool en 128 gefrituurde kaasknabbels. Tip: de grillkoollijn en de Frituurautomaat."),
    "tech_quests_knabbelmachine_4": ("De maag",
        "Breng 128 knabbelmeel en 64 Guhdraad. Tip: Oogster, Vadsmolen en een Knutselmachine."),
    "tech_quests_knabbelmachine_5": ("De snoet",
        "Breng 12 Guhdrankjes en 128 glas. Tip: de Brouwautomaat, en zand in een Guhoven die zichzelf voert."),
    "tech_quests_knabbelmachine_6": ("Het gloeisterhart",
        "Breng 1 gloeister en 256 kaasknabbels. Dan gaan zijn ogen open..."),
    "tech_quests_perfect": ("De perfecte knabbel",
        "Je Grote Knabbelmachine maakt elke dag één &dperfecte knabbel&r: klik op het bakje onder zijn bek. Je bent nu "
        "&6Knabbelmachinist&r (titel in je Guhdex)."),
    "tech_quests_beeldje": ("De machine op de kast",
        "Zet het &dKnabbelmachine-beeldje&r neer waar iedereen het ziet. Klik erop: hij knabbelt."),
    # --- Tier 3: De Koepel
    "koepel_oven": ("Bakken in bulk",
        "De &dMega-oven&r is een hele guh-bakkerij op vadskracht. Klik met een lege hand om te kiezen wat hij bakt."),
    "koepel_station": ("Weer voor de hele basis",
        "Zet een &dKnabbel-weerstation&r naast je Weer vadser en voer hem knabbels: het weertje geldt dan voor je hele "
        "basis."),
    "koepel_koepel": ("Een sterrenhemel van guhs",
        "Bouw de &dSterrenkoepel&r en voer hem knabbels tot het eerste guh-sterrenbeeld oplicht. Oooh."),
    "koepel_molen": ("Kermis voor guhs",
        "Bouw een &dDraaimolen&r en koop een kaartje (een kaasknabbel): je guhs rijden mee en worden blij."),
    "koepel_vanger": ("Wolken vangen",
        "Zet een &dWolkenvanger&r op het dak: hoe hoger, hoe meer pluis."),
    # --- Tier 3: Mega-kracht & hamsterbuizen
    "centrale_knuffel": ("Zestien knuffels",
        "Op de &dMega-knuffelcentrale&r knuffelen tot 16 tamme guhs, elk voor &b22 vadskracht&r. Voer ze en geef elke dag "
        "één wol voor verse dekentjes. Eén per net."),
    "centrale_export": ("Export-vads",
        "De &dguh-villagers&r kopen elke dag &6bulk&r van je fabrieken: knabbelblokken, knuffelsteen, wol en baksels. Ze "
        "betalen met zeldzame dingen, zelfs fossielen."),
    "tech_hamsterknooppunt": ("Het Hamsterknooppunt",
        "Bouw een &dHamsterknooppunt&r en leg er Guhdraad naartoe: daarmee rollen al je hamsterbuizen. Eentje per guhbaas!"),
    "tech_hamsterbuis_station": ("Woesj!",
        "Zet twee &dhamsterbuis-stations&r ver uit elkaar (mag in een andere dimensie). Stap in, kies waarheen, en woesj!"),
    # --- Tier 3: De Kaasraket
    "raket_platform": ("Een plek om te lanceren",
        "Maak een &dLanceerplatform&r en zet het op een open plek: de raket wordt groot!"),
    "raket_romp": ("De romp",
        "Klik op het platform met 64 knuffelsteen, 24 ijzer en 16 glas (in porties mag)."),
    "raket_motor": ("De motor",
        "Twee knabbelbatterijen, een Guhoven, 4 ijzerblokken en 16 redstone. Brommm!"),
    "raket_brandstof": ("De brandstof",
        "24 blokken kaasknabbels en 4 emmers kaassaus. Raketten rijden op kaas, dat weet iedereen."),
    "raket_bemanning": ("De bemanning",
        "Trek drie guhs een &dastronautenpakje&r aan, neem ze mee naar het platform en klik erop."),
    "raket_gelanceerd": ("Lancering!",
        "Klik op het platform: aftellen, motoren aan, en weg is de raket!"),
    "kaasmaan_vlag": ("Eén kleine stap",
        "Plant een &dmaanvlaggetje&r op de Kaasmaan. Ruil kaasmaankristallen bij &dAstronaut Kaasje&r voor souvenirs."),
    # --- Tier 3: Fossielen & oerguhs
    "fossiel_gevonden": ("Een fossiel!",
        "&6Fossielen&r liggen in oude opgravingen, de fossielmijn van de Barbecuether, de verlaten guhmijn en bij de "
        "grotschilderingen onder de wol. Vind er een!"),
    "fossiel_broedmachine": ("De Fossiel-broedmachine",
        "Stop een fossiel en 16 kaasknabbels in de &dFossiel-broedmachine&r en geef hem &b20 vadskracht&r."),
    "fossiel_uitgebroed": ("Krak!",
        "Na een hele dag broeden kruipt er een baby-oerguh uit het ei. Hij is meteen van jou!"),
    "fossiel_mammoet": ("Mammoet-guh",
        "Een &dMammoet-guh&r doet zijn klusjes twee keer zo snel, en knuffelen geeft wol."),
    "fossiel_tguh": ("T-Guh",
        "Geef een &dT-Guh&r de klus &eWaken&r: hij ziet monsters van heel ver aankomen. Rawr, njeg."),
    "fossiel_pteroguh": ("Pteroguh",
        "Een &dPteroguh&r in een guhhuisje vliegt spullen naar kisten tot ver om het huisje heen."),
    "fossiel_stokstaartje": ("Oer-stokstaartje-guh",
        "Neem een &dOer-stokstaartje-guh&r mee: hij gaat rechtop staan en wijst zeldzame guhs in de buurt aan."),
    # --- Tier 4: De werkplaats van de Werktuigguh
    "einde2_trap5_machines_werkplaats": ("De werkplaats gaat open",
        "Na de finale van het Guhvatar-verhaal gaat de werkplaats in de &dZuidelijke Luchttempel&r open. De &6Werktuigguh&r "
        "geeft je zijn Bouwplan voor alle machines hieronder."),
    "einde2_trap5_machines_zender": ("Kracht door de lucht",
        "Een &dVadszender&r stuurt vadskracht naar &dVadsontvangers&r tot 32 blokken ver. Samen zijn ze één net."),
    "einde2_trap5_machines_terminal": ("Je bank, overal",
        "Een &dGuhterminal&r is een raampje in je Bank Guh: koppel hem met je Banksleutel en zoek, pak en stort."),
    "einde2_trap5_machines_bestel": ("De Bestelguh",
        "De &dBestelguh&r aan een &dBestelbalie&r haalt alles uit de bank, laat je Knutselmachines draaien en brengt het "
        "bonnetje."),
    "einde2_trap5_machines_poortbuis": ("Post door de poort",
        "Een &dKnabbelpoort-buis&r stuurt wat erin gaat naar zijn maatje, ook in een andere dimensie."),
    "einde2_trap5_machines_thuispoort": ("Thuis in één stap",
        "Bouw je eigen netwerk van &dThuispoorten&r. Je guhs aan de lijn en je rijdier springen mee."),
    "einde2_trap5_machines_elementen": ("De vier elementen",
        "&dLuchtblazer&r, &dWaterdrager&r, &dKorstduwer&r en &dVuurkorf&r: laat ze elk hun ding doen."),
    "einde2_trap5_machines_enderguhrad": ("Het Enderguhrad",
        "Een &5Vahoege Enderguh&r vliegt rondjes in een reuzenrad: &b50 vadskracht&r. Eén per opstelling."),
    "einde2_trap5_machines_komeet": ("Een staartje van de komeet",
        "Bij het duelplein van de Vuurnatie-hoofdstad ligt een scherf van de knabbelkomeet. De &dKomeetkern&r geeft &b500 "
        "vadskracht&r!"),
    "einde2_trap5_machines_bronnen": ("Nieuwe bronnen",
        "Een draaiend Enderguhrad en een gloeiende Komeetkern, allebei in je eigen net."),
    "einde2_trap5_machines_klaar": ("Helemaal af",
        "Vertel het de Werktuigguh. Voor het eerst in honderd jaar maakt hij iets af: een diploma, voor jou."),
    # --- Tier 4: De Vredesballon
    "einde2_trap5_ballon_vredesballon_1": ("Een plan met oortjes",
        "De Werktuigguh heeft nog één groot plan. Klik op het bord op de &6bouwplaats&r achter zijn werkplaats."),
    "einde2_trap5_ballon_vredesballon_2": ("Het doek",
        "Breng 128 tentdoek en 64 Guhdraad. Tip: een Knutselmachine met een Bouwtekening van tentdoek."),
    "einde2_trap5_ballon_vredesballon_3": ("De mand",
        "Breng 256 boomstammen en 16 doosschillen. Tip: een bos in een bak, en een Schuilguh."),
    "einde2_trap5_ballon_vredesballon_4": ("De brander",
        "Breng 2 Vuurkorven, 1 Vadsontvanger, 96 grillkool en 16 kristalkaas. De brander draait op vadskracht uit je eigen "
        "fabriek."),
    "einde2_trap5_ballon_vredesballon_5": ("De knabbelvoorraad",
        "Breng 512 kaasknabbels en 128 gefrituurde kaasknabbels. Tip: de frituurlijn en de Oogster."),
    "einde2_trap5_ballon_vredesballon_6": ("De versiering",
        "Breng 64 lichtstaafjes en 32 flitskruimels. Dan zet de Werktuigguh er oortjes op."),
    "einde2_trap5_ballon_vredesballon_7": ("Opstijgen!",
        "Klik op het bord en laat de &dVredesballon&r opstijgen. Je krijgt een beeldje, je eigen heteluchtballon en de titel "
        "&6Vredesbrenger&r."),
    "einde2_trap5_ballon_beeldje": ("Een ballon op de kast",
        "Zet het &dVredesballon-beeldje&r neer. Klik erop: er dwarrelt een kruimeltje uit het mandje."),
    "einde2_trap5_ballon_zweven": ("Zelf zweven",
        "Zet je &dheteluchtballon&r neer, stap in en houd springen ingedrukt: de brander loeit en je stijgt. De wind doet de "
        "rest."),
    "einde2_trap5_ballon_zakje": ("Elke dag knabbelregen",
        "De Vredesballon vliegt elke dag een rondje. Kom daarna langs bij het bord: er zit een zakje knabbels in de mand, "
        "speciaal voor jou."),
    "einde2_trap5_ballon_titel": ("Vredesbrenger",
        "Kies de titel &6Vredesbrenger&r in je Guhdex. Wie had gedacht dat een fabriek zoveel blije oortjes kon maken?"),
}


def portret(fq, spec):
    """'<module>' = the portrait that module gives its own section (FTB_SECTIES or FTB_PORTRAIT)."""
    if not spec.startswith("<"):
        return spec
    mod = fq.MODULES[spec[1:-1]]
    secties = getattr(mod, "FTB_SECTIES", None)
    return secties[0][2] if secties else getattr(mod, "FTB_PORTRAIT", None) or "guh:normal"


def toepassen(fq):
    for key, title, icon, soort in NIEUW:
        tasks = [fq.item(icon)] if soort == "item" else [{"type": "checkmark"}]
        fq.q(key, title, TEKST[key][1], icon, tasks, rewards=(("guhs:kaas_knabbels", 8),), xp=50)
        fq.SOURCE[key] = "tech_quests"
    keys = [k for *_, ks in SECTIES for k in ks]
    assert len(keys) == len(set(keys)), "ftb_techniek: a quest in two sections"
    eigen = {x[0] for x in fq.QUESTS if fq.chapter_of(x[0]) == "guhs_techniek" and x[0] not in fq.INTROS}
    assert eigen == set(keys), f"ftb_techniek: missing {sorted(eigen - set(keys))}, unknown {sorted(set(keys) - eigen)}"
    assert set(TEKST) == set(keys), f"ftb_techniek: texts for {sorted(set(keys) ^ set(TEKST))}"
    # each section's quests one after another (the lines; nothing is locked), the first one after the upstream
    na = {}
    for sid, tier, title, spec, upstream, ks in SECTIES:
        for a, b in zip(ks, ks[1:]):
            na[b] = [a]
    for i, x in enumerate(fq.QUESTS):
        if x[0] in TEKST:
            title, desc = TEKST[x[0]]
            fq.QUESTS[i] = (x[0], title, desc, *x[3:6], na.get(x[0], []), *x[7:])
        elif x[0] == fq.intro_key("guhs_techniek"):
            fq.QUESTS[i] = (x[0], x[1], "\n\n".join(INTRO), *x[3:])   # (the "Hoe kom je hier?" is already made)
    fq.SECTIONS["guhs_techniek"] = [fq.sec(sid, title, portret(fq, spec), keys=ks, upstream=upstream, colour=TIERS[tier])
                                    for sid, tier, title, spec, upstream, ks in SECTIES]
    fq.CHAPTERS["guhs_techniek"]["intro"] = INTRO


# the new English of the quests that already had English (make_ftbquests.py build() lays it over tools/lang/en, where the
# modules' own English files keep their older texts); the English of the new quests and sections is in
# tools/lang/en/c218_ftb_techniek.json
EN = {
    "ftb.guhs_techniek.q.intro_guhs_techniek.desc":
        "This chapter comes after &dThe Guh Wheel&r in &dGuhs & Basics&r: a guh running in a wheel makes &bchonk power&r. With &dGuh Wire&r you bring it to machines with a little snoot.\n\nIt goes in &6tiers&r: &aTier 1&r you build right away at home, &6Tier 2&r needs stuff from the &6Guhbarbecuether&r, &bTier 3&r are the big projects and &5Tier 4&r lies in the &5Guh End&r. Look at a wheel, wire or machine and you see how much chonk power your setup uses.\n\nNothing is locked: every quest ticks itself off as soon as you've done it, even if that was earlier. Tick this off and get chonkily to work!",
    "ftb.guhs_techniek.q.tech_quests_rad.title":
        "A Guh at Work",
    "ftb.guhs_techniek.q.tech_quests_rad.desc":
        "Put down a &dGuh Wheel&r and click it with a picked-up guh: it runs for &b10 chonk power&r, forever. It never gets tired and never needs to eat. Click with an empty hand to take it back.",
    "ftb.guhs_techniek.q.tech_quests_draad.title":
        "Laying Guh Wire",
    "ftb.guhs_techniek.q.tech_quests_draad.desc":
        "Redstone, pink dye and a cheese nibble give 3 &dGuh Wire&r. Lay it from your wheel to your machines: everything joined together is one &esetup&r. A machine right next to the wheel needs no wire.",
    "ftb.guhs_techniek.q.tech_quests_oven.title":
        "The Oven with a Little Snoot",
    "ftb.guhs_techniek.q.tech_quests_oven.desc":
        "The &dGuh Oven&r bakes everything a regular furnace bakes, without coal, on &b5 chonk power&r. Its snoot shows how it's doing: asleep without power, happy when working, surprised when full.",
    "ftb.guhs_techniek.q.tech_quests_vadslamp.title":
        "Light with a Little Snoot",
    "ftb.guhs_techniek.q.tech_quests_vadslamp.desc":
        "The &dChonk Lamp&r burns on &b1 chonk power&r and lights up to 6 blocks around it. One lamp and your whole cave is bright. Monsters hate it.",
    "ftb.guhs_techniek.q.tech_quests_aflezen.title":
        "Looking Is Knowing",
    "ftb.guhs_techniek.q.tech_quests_aflezen.desc":
        "Look at a wheel, wire or machine: under your crosshair it says, for example, &bThis setup uses 5/10 chonk power&r. Ask too much and everything stops, and you can read how much you're short. Seen it? Tick it off.",
    "ftb.guhs_techniek.q.tech_quests_hoeveel.title":
        "How Much Can You Hook Up?",
    "ftb.guhs_techniek.q.tech_quests_hoeveel.desc":
        "Per setup, &e4&r Guh Wheels count in full (then less and less, up to &e16&r), plus &e2&r Cuddle Generators, &e2&r Disco Dynamos, &e4&r Blub Stoves and &e1&r Glowstar Core. Need more? Build a second setup.",
    "ftb.guhs_techniek.q.tech_quests_beurs.title":
        "At the Guh Technology Fair",
    "ftb.guhs_techniek.q.tech_quests_beurs.desc":
        "In the Guhmension stands a glass hall where &devery machine really works&r (Super Compass, under Cozy). Every stand has a sign and a fair guh who tells you which quest teaches it. Look, don't touch!",
    "ftb.guhs_techniek.q.tech_quests_trechters.title":
        "Never Bake Yourself Again",
    "ftb.guhs_techniek.q.tech_quests_trechters.desc":
        "A chest with a &ehopper&r above your Guh Oven, a hopper underneath into a second chest. Toss it in, walk away, and a bit later it's all baked.",
    "ftb.guhs_techniek.q.tech_quests_molen.title":
        "Milling Without Wind",
    "ftb.guhs_techniek.q.tech_quests_molen.desc":
        "The &dChonk Mill&r (&b5 chonk power&r) grinds nibble grain into flour, bones into bone meal and cobblestone into gravel and sand. It gives more than grinding by hand. Grind something!",
    "ftb.guhs_techniek.q.tech_quests_knuffel.title":
        "Happy Guhs Run Faster",
    "ftb.guhs_techniek.q.tech_quests_knuffel.desc":
        "Tame guhs come and lie on the &dCuddle Generator&r by themselves: &b3 chonk power&r per guh, up to 8 guhs. A freshly cuddled guh then runs for &b15&r instead of 10 in its wheel.",
    "ftb.guhs_techniek.q.tech_quests_disco.title":
        "Dancing for the Factory",
    "ftb.guhs_techniek.q.tech_quests_disco.desc":
        "Click the &dDisco Dynamo&r with a music disc: up to 4 guhs come to dance, each for &b5 chonk power&r. Every disc has its own light show. Nyeg, what moves.",
    "ftb.guhs_techniek.q.vb_knabbelketel.title":
        "Cheese in the Boiler",
    "ftb.guhs_techniek.q.vb_knabbelketel.desc":
        "The &dNibble Boiler&r burns cheese nibbles for &b80 chonk power&r. A block of nibbles burns nine times as long. A real glutton, though: time for a nibble farm!",
    "ftb.guhs_techniek.q.vb_antenne.title":
        "Weather All Around",
    "ftb.guhs_techniek.q.vb_antenne.desc":
        "A &dWeather Vadser&r reaches 10 blocks. Put a &dCloud Antenna&r on it: I, II and III make that 20, 35 and 60 blocks. Every step costs more chonk power.",
    "ftb.guhs_techniek.q.tech_quests_klus_machines.title":
        "Colleagues with a Snoot",
    "ftb.guhs_techniek.q.tech_quests_klus_machines.desc":
        "Switch on the chore &eRefill & Empty Machines&r for a resident of your &dGuh House&r. Put something in a machine once yourself: it remembers and refills from the chest or your Bank Guh. Who still needs hoppers?",
    "ftb.guhs_techniek.q.tech_quests_klus_oogst.title":
        "Pumpkin Picker",
    "ftb.guhs_techniek.q.tech_quests_klus_oogst.desc":
        "A resident with the chore &eFarming&r also harvests &epumpkins&r, &emelons&r, &esugar cane&r, &ecocoa&r and &enether wart&r, and leaves the plant standing. Let it harvest one. Nyeg, pumpkin pie!",
    "ftb.guhs_techniek.q.kaaskas_band.title":
        "Everything in Its Place",
    "ftb.guhs_techniek.q.kaaskas_band.desc":
        "Lay a &dGuh Sorting Belt&r and give a resident of a house next to it the chore &eSorter&r. It puts everything into the right chest. Tidier than your room!",
    "ftb.guhs_techniek.q.kaaskas_kas.title":
        "A Greenhouse Full of Cheese",
    "ftb.guhs_techniek.q.kaaskas_kas.desc":
        "Put a &dCheese Greenhouse&r among your crops and give it &bchonk power&r and a bucket of &6cheese sauce&r. Watch them grow!",
    "ftb.guhs_techniek.q.kaaskas_pluis.title":
        "Fluffy!",
    "ftb.guhs_techniek.q.kaaskas_pluis.desc":
        "Plant &ffluffballs&r (seeds: wheat seeds + wool) and harvest your first &ffluff&r. Soft, right?",
    "ftb.guhs_techniek.q.fabriek_scheerstal.title":
        "Shearing Stable",
    "ftb.guhs_techniek.q.fabriek_scheerstal.desc":
        "Keep tame &dCloud Lambs&r around a &dShearing Stable&r: it shears them by itself. Tame and breed them with cloud candy floss. Bleh!",
    "ftb.guhs_techniek.q.fabriek_recycler.title":
        "Nothing Is Wasted",
    "ftb.guhs_techniek.q.fabriek_recycler.desc":
        "Put old plushies, guh clothes, wool or cushions into the &dPlushie Recycler&r: fluff comes out again.",
    "ftb.guhs_techniek.q.fabriek_spin.title":
        "Whirring Spinner",
    "ftb.guhs_techniek.q.fabriek_spin.desc":
        "The &dSpinning Machine&r spins &ffluff&r into wool: 2 fluff is 1 white wool.",
    "ftb.guhs_techniek.q.fabriek_verf.title":
        "Splash in the Dye Bath",
    "ftb.guhs_techniek.q.fabriek_verf.desc":
        "Wool and dye into the &dDye Bath&r: it dyes everything the game can dye, glass, clay and cuddle blocks too.",
    "ftb.guhs_techniek.q.fabriek_kleurmolen.title":
        "All the Colours of the Rainbow",
    "ftb.guhs_techniek.q.fabriek_kleurmolen.desc":
        "The &dColour Mill&r grinds a flower into 2 or 3 dye. A guh with the chore &ePicking Flowers&r keeps it going forever.",
    "ftb.guhs_techniek.q.fabriek_regenboogpers.title":
        "Rainbow Press",
    "ftb.guhs_techniek.q.fabriek_regenboogpers.desc":
        "The &dRainbow Press&r presses fluff with &6cheese sauce&r into any colour. Pick the colour with sneak + click, or put down an example dye.",
    "ftb.guhs_techniek.q.fabriek_bouwblokken.title":
        "Building Blocks in Bulk",
    "ftb.guhs_techniek.q.fabriek_bouwblokken.desc":
        "Put an example in the &dBuilding Block Factory&r: it makes stairs, slabs, walls and coloured blocks. Link a Bank Guh or a chest (sneak and click) and it fetches its own stuff.",
    "ftb.guhs_techniek.q.tech_quests_centrale_vind.title":
        "The Old Guh Wheel Power Plant",
    "ftb.guhs_techniek.q.tech_quests_centrale_vind.desc":
        "In the &6Guhbarbecuether&r stands an old power plant with two chimneys, where five old guhs still run their laps. Your Super Compass (Barbecue) shows the way.",
    "ftb.guhs_techniek.q.tech_quests_techniek_1.title":
        "The Inventor Guh",
    "ftb.guhs_techniek.q.tech_quests_techniek_1.desc":
        "Talk to the &dInventor Guh&r in his workshop. His practice hall is in ruins: five setups, all broken.",
    "ftb.guhs_techniek.q.tech_quests_techniek_2.title":
        "The Loose Wire",
    "ftb.guhs_techniek.q.tech_quests_techniek_2.desc":
        "&eSetup 1:&r the wheel runs, the oven sleeps. Lay &dGuh Wire&r on the yellow tile.",
    "ftb.guhs_techniek.q.tech_quests_techniek_3.title":
        "Too Heavy!",
    "ftb.guhs_techniek.q.tech_quests_techniek_3.desc":
        "&eSetup 2:&r two ovens and a mill on one wheel. Read the wire and cut one at the red wool.",
    "ftb.guhs_techniek.q.tech_quests_techniek_4.title":
        "Salt for the Tubes",
    "ftb.guhs_techniek.q.tech_quests_techniek_4.desc":
        "Bring the Inventor Guh 4 &dsalt crystals&r. You chip them in the &6Salt Crystal Mine&r or out of salt crystal ore in the Guhbarbecuether.",
    "ftb.guhs_techniek.q.tech_quests_techniek_5.title":
        "Back to Front",
    "ftb.guhs_techniek.q.tech_quests_techniek_5.desc":
        "&eSetup 3:&r the &dDirection Piece&r points the wrong way. Sneak and click it with an empty hand.",
    "ftb.guhs_techniek.q.tech_quests_techniek_6.title":
        "The Filter Is Wrong",
    "ftb.guhs_techniek.q.tech_quests_techniek_6.desc":
        "&eSetup 4:&r the &dFilter Piece&r lets everything through except nibbles. Set it to &eonly these&r.",
    "ftb.guhs_techniek.q.tech_quests_techniek_7.title":
        "The Hose Is Missing",
    "ftb.guhs_techniek.q.tech_quests_techniek_7.desc":
        "&eSetup 5:&r a piece of &dSauce Hose&r is missing between pump and vat. Lay the Inventor Guh's hose on the yellow tile.",
    "ftb.guhs_techniek.q.tech_quests_techniek_8.title":
        "The Practice Hall Runs Again",
    "ftb.guhs_techniek.q.tech_quests_techniek_8.desc":
        "Tell the Inventor Guh. You get the &dBottomless Nibble Belly&r and three &drecipe cards&r. He's so proud his whiskers tremble.",
    "ftb.guhs_techniek.q.tech_quests_maagje.title":
        "A Belly Without a Bottom",
    "ftb.guhs_techniek.q.tech_quests_maagje.desc":
        "Click your Bank Guh with the &dBottomless Nibble Belly&r: the limit of 256 per kind is gone, forever. And now a Filter Piece may take stuff out of your bank too.",
    "ftb.guhs_techniek.q.tech_quests_kaarten.title":
        "Three Recipe Cards",
    "ftb.guhs_techniek.q.tech_quests_kaarten.desc":
        "Put a &drecipe card&r in the top left of the crafting grid: it &estays put&r, so one is enough. Lost it? The Inventor Guh sells them again.",
    "ftb.guhs_techniek.q.tech_quests_zout.title":
        "A Sack of Salt",
    "ftb.guhs_techniek.q.tech_quests_zout.desc":
        "Almost all clever machines ask for &dsalt crystal&r. The crystal vein in the &6Salt Crystal Mine&r grows back by itself: chip a little supply.",
    "ftb.guhs_techniek.q.tech_quests_buis.title":
        "Nibble Tubes",
    "ftb.guhs_techniek.q.tech_quests_buis.desc":
        "Glass, cheese nibbles and salt crystal give 8 see-through &dNibble Tubes&r. They stick by themselves to chests, machines and your Bank Guh, without chonk power.",
    "ftb.guhs_techniek.q.tech_quests_richting.title":
        "From Chest to Chest",
    "ftb.guhs_techniek.q.tech_quests_richting.desc":
        "A &dDirection Piece&r against a chest keeps taking one thing out and sends it through the tubes. Sneak and click to turn it around; redstone locks it.",
    "ftb.guhs_techniek.q.tech_quests_filter.title":
        "The Filter Piece",
    "ftb.guhs_techniek.q.tech_quests_filter.desc":
        "A &dFilter Piece&r is a Direction Piece with a little list: &eonly these&r or &eall except&r. With &eleave behind&r a little stack stays put.",
    "ftb.guhs_techniek.q.tech_quests_sorteer.title":
        "Storage That Sorts Itself",
    "ftb.guhs_techniek.q.tech_quests_sorteer.desc":
        "One drop-off chest, a long tube past your chests and a Filter Piece set to &eonly these&r in front of each chest. Toss in your whole backpack and walk away. Works? Tick it off.",
    "ftb.guhs_techniek.q.tech_quests_sleutel.title":
        "The Bank Key",
    "ftb.guhs_techniek.q.tech_quests_sleutel.desc":
        "Click your Bank Guh with a &dBank Key&r: the key remembers which bank it is. Link as many Nom Hatches with it as you like.",
    "ftb.guhs_techniek.q.tech_quests_hapluikje.title":
        "Everything into the Bank by Itself",
    "ftb.guhs_techniek.q.tech_quests_hapluikje.desc":
        "The &dNom Hatch&r is a little mouth of your Bank Guh: everything that goes in ends up in your bank, even in another dimension. Put one by your mine, your field and your oven. Nom!",
    "ftb.guhs_techniek.q.tech_quests_klus_luikje.title":
        "Nom, All Tidy!",
    "ftb.guhs_techniek.q.tech_quests_klus_luikje.desc":
        "Is there a working Nom Hatch in the chore area of your &dGuh House&r? Then your residents pop their haul in there. Let a resident nom something through the hatch.",
    "ftb.guhs_techniek.q.tech_quests_opzuiger.title":
        "The Slurper",
    "ftb.guhs_techniek.q.tech_quests_opzuiger.desc":
        "The &dSlurper&r (&b3 chonk power&r) slurps up everything lying on the ground within four blocks. Tube behind it, Nom Hatch on it, done.",
    "ftb.guhs_techniek.q.tech_quests_oogster.title":
        "The Field That Harvests Itself",
    "ftb.guhs_techniek.q.tech_quests_oogster.desc":
        "The &dHarvester&r (&b8 chonk power&r) snips everything ripe in the 5 by 5 in front of its snoot and leaves it planted. Your harvest is in the bank before you're home.",
    "ftb.guhs_techniek.q.tech_quests_sensor.title":
        "Enough Is Enough",
    "ftb.guhs_techniek.q.tech_quests_sensor.desc":
        "The &dStock Meter&r, &dSniff Sensor&r, &dGuh Clock&r and &dGuh Counter&r give a redstone signal. Lock a Direction Piece with it and your line stops by itself when the chest is full enough.",
    "ftb.guhs_techniek.q.tech_quests_batterij.title":
        "The Nibble Battery",
    "ftb.guhs_techniek.q.tech_quests_batterij.desc":
        "The &dNibble Battery&r saves whatever your guhs run too much and jumps in when you're short for a moment. Half an hour of Guh Wheel in your pocket.",
    "ftb.guhs_techniek.q.tech_quests_magazijn.title":
        "The Bank as a Warehouse",
    "ftb.guhs_techniek.q.tech_quests_magazijn.desc":
        "Put a Filter Piece against your Bank Guh, choose what may come out and set &eleave behind 64&r. Your bank feeds your ovens, but always keeps a stack back for you.",
    "ftb.guhs_techniek.q.tech_quests_saus_kaart.title":
        "Sauce and Hoses",
    "ftb.guhs_techniek.q.tech_quests_saus_kaart.desc":
        "For this row you need the &dRecipe Card: Sauce and Hoses&r, plus grill skewers or blub cream from the Guhbarbecuether.",
    "ftb.guhs_techniek.q.tech_quests_pomp.title":
        "The Sauce Pump",
    "ftb.guhs_techniek.q.tech_quests_pomp.desc":
        "Put the &dSauce Pump&r (&b6 chonk power&r) on a source of cheese sauce, frying sauce or water. It pumps a bucket every two seconds and the source never runs dry.",
    "ftb.guhs_techniek.q.tech_quests_vat.title":
        "Sauce on Tap",
    "ftb.guhs_techniek.q.tech_quests_vat.desc":
        "A &dSauce Vat&r stores 16 buckets of one sauce (milk too). Tap it with an empty bucket, at home.",
    "ftb.guhs_techniek.q.vb_saustank.title":
        "A Pool of Sauce",
    "ftb.guhs_techniek.q.vb_saustank.desc":
        "The &dBig Sauce Tank&r holds 64 buckets. The meter on its belly shows how full it is. No swimming!",
    "ftb.guhs_techniek.q.vb_sausbuis.title":
        "Pipes All the Way",
    "ftb.guhs_techniek.q.vb_sausbuis.desc":
        "A &dSauce Pipe&r carries sauce far: it only counts as a sixteenth of a hose.",
    "ftb.guhs_techniek.q.tech_quests_frituur.title":
        "Fry-Up on the Assembly Line",
    "ftb.guhs_techniek.q.tech_quests_frituur.desc":
        "The &dAuto Fryer&r (&b8 chonk power&r) fries cheese nibbles and guhfish with frying sauce from the hose. Tube in, tube out, always something yummy.",
    "ftb.guhs_techniek.q.tech_quests_brouw.title":
        "Potions That Brew Themselves",
    "ftb.guhs_techniek.q.tech_quests_brouw.desc":
        "The &dAuto Brewer&r (&b8 chonk power&r) turns cheese sauce, three bottles and one ingredient into three &dGuh Potions&r.",
    "ftb.guhs_techniek.q.tech_quests_pers.title":
        "The Grill Coal Line",
    "ftb.guhs_techniek.q.tech_quests_pers.desc":
        "The &dGrill Coal Press&r presses a bucket of frying sauce and a bucket of water into &dgrill coal&r. Two pumps and one hose are enough.",
    "ftb.guhs_techniek.q.vb_generator.title":
        "Soft Stone",
    "ftb.guhs_techniek.q.vb_generator.desc":
        "Cheese sauce + water = &dcuddlestone&r. Put the &dCuddlestone Generator&r next to water, give it sauce through a hose and chonk power, and put a chest next to it.",
    "ftb.guhs_techniek.q.tech_quests_blub.title":
        "The Blub Stove",
    "ftb.guhs_techniek.q.tech_quests_blub.desc":
        "A &dSauce Blubby&r in a jar on a &dBlub Stove&r gives &b6 chonk power&r as long as it's warm. Feed it a cheese nibble now and then; blub blub.",
    "ftb.guhs_techniek.q.tech_quests_machine_kaart.title":
        "Nibbling Machines",
    "ftb.guhs_techniek.q.tech_quests_machine_kaart.desc":
        "The &dRecipe Card: Nibbling Machines&r is for the Nibbler, the Placer, the Tinker Machine and the Drawing Table.",
    "ftb.guhs_techniek.q.tech_quests_knabbelaar.title":
        "Stone Without a Pickaxe",
    "ftb.guhs_techniek.q.tech_quests_knabbelaar.desc":
        "The &dNibbler&r (&b10 chonk power&r) gnaws away the block in front of its snoot every two seconds. Put it in front of a cobblestone generator for an endless stone line. It never eats chests.",
    "ftb.guhs_techniek.q.tech_quests_neerzetter.title":
        "The Placer",
    "ftb.guhs_techniek.q.tech_quests_neerzetter.desc":
        "The &dPlacer&r (&b5 chonk power&r) puts the blocks from its tummy down in front of it, one per second. Together with a Nibbler: place, nibble, place...",
    "ftb.guhs_techniek.q.tech_quests_plantagebak.title":
        "A Forest in a Box",
    "ftb.guhs_techniek.q.tech_quests_plantagebak.desc":
        "The &dPlantation Box&r (&b6 chonk power&r) makes any sapling grow within a minute. Click with an axe and the whole tree is in your pockets. (Recipe card: the Grill Camping.)",
    "ftb.guhs_techniek.q.tech_quests_klus_plantage.title":
        "Little Lumberjack Guh",
    "ftb.guhs_techniek.q.tech_quests_klus_plantage.desc":
        "A resident with the chore &ePlantation: Plant & Chop&r chops down the tree on your Plantation Box and carries the wood away. Crack, nyeg, done!",
    "ftb.guhs_techniek.q.tech_quests_tekening.title":
        "The Drawing Table",
    "ftb.guhs_techniek.q.tech_quests_tekening.desc":
        "Lay a recipe in the &dDrawing Table&r with an empty &dBlueprint&r: the recipe gets drawn on it. It only costs the sheet.",
    "ftb.guhs_techniek.q.tech_quests_knutsel.title":
        "Tinkering Without Hands",
    "ftb.guhs_techniek.q.tech_quests_knutsel.desc":
        "Pop the Blueprint into a &dTinker Machine&r (&b10 chonk power&r) and feed it with tubes: it keeps tinkering as long as there's stuff.",
    "ftb.guhs_techniek.q.tech_quests_bezorg_kaart.title":
        "The Delivery Guhling",
    "ftb.guhs_techniek.q.tech_quests_bezorg_kaart.desc":
        "The &dRecipe Card: The Delivery Guhling&r is for the Scooter Station and the Stop Posts. A Delivery Guhling carries stuff up to 96 blocks away, around corners and over hills.",
    "ftb.guhs_techniek.q.tech_quests_station.title":
        "The Scooter Station",
    "ftb.guhs_techniek.q.tech_quests_station.desc":
        "In a &dScooter Station&r (&b4 chonk power&r) lives one Delivery Guhling: a mini guh on a scooter with a backpack that's far too big.",
    "ftb.guhs_techniek.q.tech_quests_halte.title":
        "Stop Posts",
    "ftb.guhs_techniek.q.tech_quests_halte.desc":
        "Put a &dStop Post&r by a chest or machine: &agreen&r = pick up, &6orange&r = drop off. Up to 8 stops per station.",
    "ftb.guhs_techniek.q.tech_quests_bezorgd.title":
        "The Delivery Round",
    "ftb.guhs_techniek.q.tech_quests_bezorgd.desc":
        "Let your Delivery Guhling drop something off, say from your field to your oven. Whatever is in its backpack never gets lost.",
    "ftb.guhs_techniek.q.tech_quests_guhtje.title":
        "Guhdex: the Delivery Guhling",
    "ftb.guhs_techniek.q.tech_quests_guhtje.desc":
        "Get close to a Delivery Guhling and it's in your &dGuhdex&r. Give it a cheese nibble and it scoots extra hard for a minute.",
    "ftb.guhs_techniek.q.tech_quests_fluitje.title":
        "Whistling for Your Parcel",
    "ftb.guhs_techniek.q.tech_quests_fluitje.desc":
        "With the &dDelivery Guhling Whistle&r (from the Tower Keeper Guh in the Smoke Guh Lighthouse) your Delivery Guhling comes to you. Sneak and whistle: everyone home.",
    "ftb.guhs_techniek.q.tech_quests_gloeister.title":
        "A Glowstar",
    "ftb.guhs_techniek.q.tech_quests_gloeister.desc":
        "Only &cBurnt Mika&r drops a &dglowstar&r, the boss of the Mika Grill Palace in the Guhbarbecuether. Hot!",
    "ftb.guhs_techniek.q.tech_quests_kern.title":
        "The Glowstar Core",
    "ftb.guhs_techniek.q.tech_quests_kern.desc":
        "Glowstar, grill coal and salt crystal make a &dGlowstar Core&r: &b200 chonk power&r, forever, without a guh. One per setup counts.",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_1.title":
        "The Big Plan",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_1.desc":
        "Practice hall done and Burnt Mika beaten? Then the Inventor Guh wants to build &6The Great Nibble Machine&r. Only you see yours grow.",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_2.title":
        "The Foundation",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_2.desc":
        "Bring 256 stone and 128 logs (in portions is fine). Tip: a stone line and a forest in a box.",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_3.title":
        "The Boiler",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_3.desc":
        "Bring 96 grill coal and 128 fried cheese nibbles. Tip: the grill coal line and the Auto Fryer.",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_4.title":
        "The Belly",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_4.desc":
        "Bring 128 nibble flour and 64 Guh Wire. Tip: Harvester, Chonk Mill and a Tinker Machine.",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_5.title":
        "The Snoot",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_5.desc":
        "Bring 12 Guh Potions and 128 glass. Tip: the Auto Brewer, and sand in a Guh Oven that feeds itself.",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_6.title":
        "The Glowstar Heart",
    "ftb.guhs_techniek.q.tech_quests_knabbelmachine_6.desc":
        "Bring 1 glowstar and 256 cheese nibbles. Then its eyes open...",
    "ftb.guhs_techniek.q.tech_quests_perfect.title":
        "The Perfect Nibble",
    "ftb.guhs_techniek.q.tech_quests_perfect.desc":
        "Your Great Nibble Machine makes one &dperfect nibble&r every day: click the little tray under its mouth. You're now a &6Nibble Machinist&r (title in your Guhdex).",
    "ftb.guhs_techniek.q.tech_quests_beeldje.title":
        "The Machine on the Shelf",
    "ftb.guhs_techniek.q.tech_quests_beeldje.desc":
        "Put the &dNibble Machine Statuette&r where everyone can see it. Click it: it nibbles.",
    "ftb.guhs_techniek.q.koepel_oven.title":
        "Baking in Bulk",
    "ftb.guhs_techniek.q.koepel_oven.desc":
        "The &dMega Oven&r is a whole guh bakery on chonk power. Click with an empty hand to choose what it bakes.",
    "ftb.guhs_techniek.q.koepel_station.title":
        "Weather for the Whole Base",
    "ftb.guhs_techniek.q.koepel_station.desc":
        "Put a &dNibble Weather Station&r next to your Weather Vadser and feed it nibbles: then the weather covers your whole base.",
    "ftb.guhs_techniek.q.koepel_koepel.title":
        "A Sky Full of Guhs",
    "ftb.guhs_techniek.q.koepel_koepel.desc":
        "Build the &dStar Dome&r and feed it nibbles until the first guh constellation lights up. Ooooh.",
    "ftb.guhs_techniek.q.koepel_molen.title":
        "A Fair for Guhs",
    "ftb.guhs_techniek.q.koepel_molen.desc":
        "Build a &dMerry-Go-Round&r and buy a ticket (a cheese nibble): your guhs ride along and get happy.",
    "ftb.guhs_techniek.q.koepel_vanger.title":
        "Catching Clouds",
    "ftb.guhs_techniek.q.koepel_vanger.desc":
        "Put a &dCloud Catcher&r on the roof: the higher, the more fluff.",
    "ftb.guhs_techniek.q.centrale_knuffel.title":
        "Sixteen Cuddles",
    "ftb.guhs_techniek.q.centrale_knuffel.desc":
        "Up to 16 tame guhs cuddle on the &dMega Cuddle Plant&r, each for &b22 chonk power&r. Feed them and give one wool every day for fresh blankets. One per net.",
    "ftb.guhs_techniek.q.centrale_export.title":
        "Export Chonk",
    "ftb.guhs_techniek.q.centrale_export.desc":
        "The &dguh villagers&r buy &6bulk&r from your factories every day: nibble blocks, cuddlestone, wool and bakes. They pay with rare things, even fossils.",
    "ftb.guhs_techniek.q.tech_hamsterknooppunt.title":
        "The Hamster Junction",
    "ftb.guhs_techniek.q.tech_hamsterknooppunt.desc":
        "Build a &dHamster Junction&r and run Guh Wire to it: that keeps all your hamster tubes rolling. One per guh owner!",
    "ftb.guhs_techniek.q.tech_hamsterbuis_station.title":
        "Whoosh!",
    "ftb.guhs_techniek.q.tech_hamsterbuis_station.desc":
        "Place two &dhamster tube stations&r far apart (another dimension is fine). Step in, pick where to go, and whoosh!",
    "ftb.guhs_techniek.q.raket_platform.title":
        "A Place to Launch",
    "ftb.guhs_techniek.q.raket_platform.desc":
        "Make a &dLaunch Pad&r and put it somewhere open: the rocket gets big!",
    "ftb.guhs_techniek.q.raket_romp.title":
        "The Hull",
    "ftb.guhs_techniek.q.raket_romp.desc":
        "Click the pad with 64 cuddlestone, 24 iron and 16 glass (in portions is fine).",
    "ftb.guhs_techniek.q.raket_motor.title":
        "The Engine",
    "ftb.guhs_techniek.q.raket_motor.desc":
        "Two nibble batteries, a Guh Oven, 4 iron blocks and 16 redstone. Vroom!",
    "ftb.guhs_techniek.q.raket_brandstof.title":
        "The Fuel",
    "ftb.guhs_techniek.q.raket_brandstof.desc":
        "24 blocks of cheese nibbles and 4 buckets of cheese sauce. Rockets run on cheese, everybody knows that.",
    "ftb.guhs_techniek.q.raket_bemanning.title":
        "The Crew",
    "ftb.guhs_techniek.q.raket_bemanning.desc":
        "Dress three guhs in an &dastronaut suit&r, take them to the pad and click it.",
    "ftb.guhs_techniek.q.raket_gelanceerd.title":
        "Liftoff!",
    "ftb.guhs_techniek.q.raket_gelanceerd.desc":
        "Click the pad: countdown, engines on, and off goes the rocket!",
    "ftb.guhs_techniek.q.kaasmaan_vlag.title":
        "One Small Step",
    "ftb.guhs_techniek.q.kaasmaan_vlag.desc":
        "Plant a &dlittle moon flag&r on the Cheese Moon. Swap cheese moon crystals with &dAstronaut Kaasje&r for souvenirs.",
    "ftb.guhs_techniek.q.fossiel_gevonden.title":
        "A Fossil!",
    "ftb.guhs_techniek.q.fossiel_gevonden.desc":
        "&6Fossils&r lie in old digs, the fossil mine of the Barbecuether, the abandoned guh mine and by the cave paintings under the wool. Find one!",
    "ftb.guhs_techniek.q.fossiel_broedmachine.title":
        "The Fossil Hatchery",
    "ftb.guhs_techniek.q.fossiel_broedmachine.desc":
        "Put a fossil and 16 cheese nibbles in the &dFossil Hatchery&r and give it &b20 chonk power&r.",
    "ftb.guhs_techniek.q.fossiel_uitgebroed.title":
        "Crack!",
    "ftb.guhs_techniek.q.fossiel_uitgebroed.desc":
        "After a whole day of hatching, a baby ancient guh crawls out of the egg. It's yours right away!",
    "ftb.guhs_techniek.q.fossiel_mammoet.title":
        "Mammoth Guh",
    "ftb.guhs_techniek.q.fossiel_mammoet.desc":
        "A &dMammoth Guh&r does its chores twice as fast, and cuddling gives wool.",
    "ftb.guhs_techniek.q.fossiel_tguh.title":
        "T-Guh",
    "ftb.guhs_techniek.q.fossiel_tguh.desc":
        "Give a &dT-Guh&r the chore &eWatch&r: it spots monsters coming from very far away. Rawr, nyeg.",
    "ftb.guhs_techniek.q.fossiel_pteroguh.title":
        "Pteroguh",
    "ftb.guhs_techniek.q.fossiel_pteroguh.desc":
        "A &dPteroguh&r in a guh house flies goods to chests far around the house.",
    "ftb.guhs_techniek.q.fossiel_stokstaartje.title":
        "Ancient Meerkat Guh",
    "ftb.guhs_techniek.q.fossiel_stokstaartje.desc":
        "Take an &dAncient Meerkat Guh&r along: it stands up and points out rare guhs nearby.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_werkplaats.title":
        "The Workshop Opens",
    "ftb.guhs_techniek.q.einde2_trap5_machines_werkplaats.desc":
        "After the finale of the Guhvatar story the workshop in the &dSouthern Air Temple&r opens. The &6Mechanist Guh&r gives you his Blueprint for all the machines below.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_zender.title":
        "Power Through the Air",
    "ftb.guhs_techniek.q.einde2_trap5_machines_zender.desc":
        "A &dChonk Transmitter&r sends chonk power to &dChonk Receivers&r up to 32 blocks away. Together they're one net.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_terminal.title":
        "Your Bank, Everywhere",
    "ftb.guhs_techniek.q.einde2_trap5_machines_terminal.desc":
        "A &dGuh Terminal&r is a little window into your Bank Guh: link it with your Bank Key and search, take and put in.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_bestel.title":
        "The Order Guh",
    "ftb.guhs_techniek.q.einde2_trap5_machines_bestel.desc":
        "The &dOrder Guh&r at an &dOrder Counter&r takes everything out of the bank, runs your Tinker Machines and brings the receipt.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_poortbuis.title":
        "Mail Through the Gate",
    "ftb.guhs_techniek.q.einde2_trap5_machines_poortbuis.desc":
        "A &dNibble Gate Tube&r sends what goes in to its buddy, even in another dimension.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_thuispoort.title":
        "Home in One Step",
    "ftb.guhs_techniek.q.einde2_trap5_machines_thuispoort.desc":
        "Build your own network of &dHome Gates&r. Your guhs on a lead and your mount jump along.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_elementen.title":
        "The Four Elements",
    "ftb.guhs_techniek.q.einde2_trap5_machines_elementen.desc":
        "&dAir Blower&r, &dWater Bearer&r, &dCrust Pusher&r and &dFire Basket&r: let each of them do its thing.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_enderguhrad.title":
        "The Enderguh Wheel",
    "ftb.guhs_techniek.q.einde2_trap5_machines_enderguhrad.desc":
        "A &5Wahoog Enderguh&r flies rounds in a giant wheel: &b50 chonk power&r. One per setup.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_komeet.title":
        "A Little Tail of the Comet",
    "ftb.guhs_techniek.q.einde2_trap5_machines_komeet.desc":
        "By the duel square of the Fire Nation Capital lies a shard of the nibble comet. The &dComet Core&r gives &b500 chonk power&r!",
    "ftb.guhs_techniek.q.einde2_trap5_machines_bronnen.title":
        "New Sources",
    "ftb.guhs_techniek.q.einde2_trap5_machines_bronnen.desc":
        "A turning Enderguh Wheel and a glowing Comet Core, both in your own net.",
    "ftb.guhs_techniek.q.einde2_trap5_machines_klaar.title":
        "All Done",
    "ftb.guhs_techniek.q.einde2_trap5_machines_klaar.desc":
        "Tell the Mechanist Guh. For the first time in a hundred years he finishes something: a diploma, for you.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_1.title":
        "A Plan with Little Ears",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_1.desc":
        "The Mechanist Guh has one more big plan. Click the board on the &6building site&r behind his workshop.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_2.title":
        "The Cloth",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_2.desc":
        "Bring 128 tent canvas and 64 Guh Wire. Tip: a Tinker Machine with a Blueprint of tent canvas.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_3.title":
        "The Basket",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_3.desc":
        "Bring 256 logs and 16 box shells. Tip: a forest in a box, and a Hidey Guh.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_4.title":
        "The Burner",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_4.desc":
        "Bring 2 Fire Baskets, 1 Chonk Receiver, 96 grill coal and 16 crystal cheese. The burner runs on chonk power from your own factory.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_5.title":
        "The Nibble Supply",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_5.desc":
        "Bring 512 cheese nibbles and 128 fried cheese nibbles. Tip: the frying line and the Harvester.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_6.title":
        "The Decorations",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_6.desc":
        "Bring 64 light rods and 32 flash crumbs. Then the Mechanist Guh puts ears on it.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_7.title":
        "Takeoff!",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_vredesballon_7.desc":
        "Click the board and let the &dPeace Balloon&r take off. You get a statuette, your own hot air balloon and the title &6Peacebringer&r.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_beeldje.title":
        "A Balloon on the Shelf",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_beeldje.desc":
        "Put down the &dPeace Balloon Statuette&r. Click it: a tiny crumb floats out of the little basket.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_zweven.title":
        "Floating Yourself",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_zweven.desc":
        "Put down your &dhot air balloon&r, step in and hold jump: the burner roars and you rise. The wind does the rest.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_zakje.title":
        "Nibble Rain Every Day",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_zakje.desc":
        "The Peace Balloon flies a round every day. Drop by the board afterwards: there's a little bag of nibbles in the basket, just for you.",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_titel.title":
        "Peacebringer",
    "ftb.guhs_techniek.q.einde2_trap5_ballon_titel.desc":
        "Pick the title &6Peacebringer&r in your Guhdex. Who'd have thought a factory could make so many happy little ears?",
}
