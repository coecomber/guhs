# NOTITIES U-TABS (Het Vadsrijk-menu, branch 110-u-tabs)

## API voor andere U-slices (LEES DIT EERST)

Klasse `nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu` (client). EEN groot scherm met hoofdtabs bovenaan; elke tab
toont zijn paneel in HETZELFDE kader (meegroeiend 430x250 .. 560x330, schaalt netjes omlaag bij GUI-schaal 4 / klein venster).

```java
// in je client-init (1x):
VadsrijkMenu.tab("gilden", () -> new ItemStack(Items.WHITE_BANNER), () -> stuurNaarServer(new MijnVraag()), 115); // server stuurt data -> jij opent je paneel
VadsrijkMenu.scherm("huizen", () -> new ItemStack(Items.OAK_DOOR), HuisPaneel::new, 117);                         // alleen-client paneel
VadsrijkMenu.subtab("instellingen", "hud", () -> Component.translatable("..."), HudBewerker::open, 10);             // sub-tab (2e rij in de titelband)
VadsrijkMenu.subtab("skillboom", "specs", () -> Component.translatable("..."), SpecPaneel::open, 10);
VadsrijkMenu.toets("gilden", MijnClient.TOETS);   // toets in de tooltip
```
- Naam van een hoofdtab = langkey `vadsrijk.menu.tab.<id>`, hover-uitleg = `vadsrijk.menu.tab.<id>.uitleg` (optioneel).
- **Je paneel:** `class GildenPaneel extends MenuPaneel` (package `menu.client`), constructor `super("gilden", titel, data)`,
  teken in `inhoud(g, mx, my, pt)` binnen `left/top/w/h` (titelband = bovenste 18 px). Kader, tabs, schaling, tooltips
  (`tip(...)`, `slot(...)`), knoppen (`knop(...)`), zoekveld (`zoekveld(...)`) zitten erin. MenuPaneel = EcoScherm zonder
  server-acties (override `doe` als je wel iets wilt sturen).
- Een bestaand EcoScherm komt in het menu door `collectieTab()` zijn tab-id te laten teruggeven (zoals Rijdieren nu).
- Volgorde hoofdtabs: karakter 10, skillboom 20, questlog 30, verkenningsboek 40, rijdieren 50 (sub: zeilbootje),
  huisdieren 60, kledingkast 70, knapzak 80, ambachten 90, puzzels 100, ridders 110, reiskaart 120, instellingen 130.
  Vrije plekken voor jullie: gilden ~115, huizen ~117, enz.
- `CollectieTabs.registreer(...)` (oud) werkt nog als shim: voegt een tab toe als die id nog niet bestaat.
- **Eigen (niet-EcoScherm) Screen in het menu:** implementeer `Geschaald`, en zet in `init()`:
  `kader = MenuClient.kader(); width = kader.lw(); height = kader.lh();`. Teken `VadsrijkMenu.tekenKader(...)` in
  extractBackground, roep eerst `VadsrijkMenu.klik(...)` aan in mouseClicked en `VadsrijkMenu.toetsInMenu(...)` in keyPressed.
  Voorbeelden: QuestlogScherm, BoomScherm, ReiskaartScherm, OpslagScherm.
- **Karakter-tab extra regels** (bv. specialisatie, gilde): server-side `MenuServer.KARAKTER_EXTRA.add((p, data) -> ...)`. Voeg
  compounds `{k: langkey, a: arg-langkey}` toe aan `data.getListOrEmpty("extra")` (lijst bestaat al).
- **Openen vanaf de server:** `MenuServer.open(player, "tab-id")`. Commando: `/guhs vadsrijk menu [tab]`.
- **Pure logica (servervrij, testbaar):** `menu.MenuIndeling` (registry + `Kader`-geometrie).

## Gebouwd
Package `feature/vadsrijk/menu/`:
- `MenuIndeling` is servervrij: de tab-registry en de kadergeometrie.
  - Het kader is 430x250 minimaal en 560x330 maximaal.
  - Als het venster te klein is, gaat schaal < 1.
- `MenuServer` levert de payloads `Vraag`/`Antwoord`:
  - de Karakter-data (level, xp, klasse, titel, alle klassen, munten, wapenschade, stats, 12 gear-slots);
  - de Reiskaart om te bekijken (zonder `Reiskaart.open`, dus de server laat je er nooit mee vliegen);
  - het commando `/guhs vadsrijk menu [tab]`.
- `client/VadsrijkMenu` is de API-gevel. Hij tekent het kader, de hoofdtabs (icoon + naam, of alleen het icoon als het te smal
  is) en de subtabs in de titelband. Verder: tooltips (naam, uitleg, toets), klikken en toetsen.
  - De toets van een andere tab springt daarheen.
  - De eigen toets of H sluit het menu.
  - Ctrl+Tab bladert door de tabs.
  - `wissel()` sluit eerst een open container.
- `client/MenuClient`:
  - toets **H** (`key.guhs.vr_menu`): opent het menu op de laatst bekeken tab (de eerste keer Karakter);
  - de schaling van `Geschaald`-schermen: via `ScreenEvent.Render.Pre` (LOWEST) wordt het scherm in een geschaalde pose
    getekend, met logische muis; tooltips staan zo ook goed;
  - Pre-events voor muisklik, loslaten, slepen en scrollen worden opnieuw verstuurd met muis/schaal.
- `client/MenuTabs`: alle registraties op één plek, met icoon, toets en de open-actie die de toets al had.
- `client/MenuPaneel` (basis voor nieuwe panelen), `client/RozeKnop` (een roze vanilla-Button).
- `client/KarakterPaneel`: drie kolommen, Wie ik ben | Uitrusting | Stats.
- `client/PuzzelPaneel`: alle puzzels, geheimen en knuffels van alle zones, uit de Verkenningsboek-data.
  - Filter: alles / open / klaar.
  - Een zoekveld.
- `client/InstellingenPaneel`: schuiven en keuzes direct op de bestaande ModConfigSpecs.
  - Die specs zijn Geluid, Dialoog, Beleving, Vfx en Sfeer; ze worden meteen opgeslagen.
  - Knoppen: "Toetsen..." en "HUD aanpassen..." (alleen als er een tab "hud" geregistreerd is).
- `VadsrijkMenuGameTests`: 3 tests (tabRegister, kaderPast, karakterData).
- Teksten: `tools/features/vadsrijk_u_tabs.py` (in FEATURES), EN-overlay `tools/lang/en/c310_vr_u_tabs.json`.

## Fix: Verkenningsboek-overflow
De zoneknoppen stonden rechts uitgelijnd in de titelband. Met 5 zones liepen ze links buiten het scherm.
- Nu staan ze als een rij gelijke tabs onder de band, binnen het paneel.
- Lange namen worden ingekort; hover toont de volledige naam + x/y + %.
- Het boek begint daaronder.
- Er staat een zoekveld in de band; dat filtert de rechterpagina.

## Eigen keuzes
- **Schalen in plaats van herontwerpen.** Elk scherm werkt in een logische resolutie van minimaal 430x250. Is het venster
  kleiner (GUI 4, klein venster), dan tekent MenuClient het hele scherm geschaald. Zo hoeft geen enkel paneel twee layouts te hebben.
- **Groeien.** Het kader groeit mee tot 560x330. De panelen gebruikten al w/h, dus lijsten worden gewoon langer.
  - Karakterkaarten (Ridders) zijn gecentreerd.
- **Boom met veel rijen:** `kader(sw, sh, minW, minH)` maakt het kader voor die ene tab hoger; zo nodig wordt het geschaald.
- **Questlog in roze stijl:** lichte kleuren en RozeKnop in plaats van de donkere stijl.
  - De lijstknoppen kregen witte tekst: geel op roze was onleesbaar.
- **Skillboom:** het eigen donkere boompaneel blijft, binnen het roze kader. Zo weinig mogelijk in BoomScherm, omdat u-specs daar werkt.
- **Zeilbootje** is een subtab van Rijdieren. **Materiaaltas** is een subtab van Knapzak.
- De **bank** (bij de bankier) blijft een los scherm.
- **Puzzels/geheimen** gebruikt dezelfde data als het Verkenningsboek (geen nieuwe server-code). PuzzelPaneel extends BoekScherm,
  zodat de bestaande boek-ontvanger het paneel ververst.
- **Reiskaart-tab** is alleen om te kijken. De server kiest pas als `Reiskaart.open` (luchtschip-NPC) net gebeurd is. Ook de
  client stuurt in menumodus geen ReisKies.
- **Toets H** voor het menu/Karakter: H was nog vrij in de Vadsrijk-categorie.
- Zoekvelden staan bij de lange lijsten: Verkenningsboek (rechterpagina) en Puzzels. (Ambachten-recepten, Kledingkast en
  Questlog hebben paginering/scroll; daar is geen zoekveld bijgekomen.)

## Gedeelde bestanden aangeraakt (gemarkeerd U-TABS)
- `VadsrijkFeature` (+2 regels: MenuServer.register/payloads), `client/VadsrijkClient` (+1: MenuClient.init, als laatste).
- `client/CollectieTabs` is nu een shim naar VadsrijkMenu.
- `economie/client/EcoScherm`:
  - implements Geschaald;
  - menumodus als `collectieTab() != null`: kader, tabs en schaal;
  - `keyPressed` (menutoetsen) en `zoekveld()`/`past()`.
  - Let op: `heeftTabs()` is nu protected en waar zodra er een collectieTab is.
- `voltooiing/client/BoekScherm`:
  - de zonerij (overflow-fix), het zoekveld en een protected ctor met titel;
  - naam/hint/icoon zijn nu public;
  - de eigen B-toets is weg: EcoScherm doet dat nu generiek.
- `npc/client/QuestlogScherm`: Geschaald, menukader, maat volgt het kader, roze kleuren, RozeKnop, klik/toets.
- `npc/client/ReiskaartScherm`: een tweede constructor `(kaart, menu)`; menumodus is kijken-only binnen het kader.
- `klassen/client/BoomScherm`: Geschaald, kader eromheen, klik/toets (klein, voor u-specs).
- `opslag/client/OpslagScherm` + `MateriaalScherm`: Geschaald, kader, tabs; klikken in het kader laat je item niet vallen.
- `karakter/client/KarakterSchermClient`: `collectieTab()` = "ridders" en de kaarten gecentreerd (u-karakter: let op bij mergen).
- `tools/features/__init__.py` (+1 regel), `nl_nl.json`/`en_us.json` (83 sleutels).

## Open punten
- Niet in een echte client bekeken: alleen gecompileerd + gametests. Ik heb geen client-run gedaan (geheugen; de gebruiker test).
- De schaalroute (GUI 4) is het meest kwetsbaar: als een tooltip of klik verschuift, zit het in MenuClient (Pre-events).
- Ambachten-, Kledingkast- en Questlog-lijsten hebben nog geen zoekveld. Dat kan met `zoekveld()` in EcoScherm, of later voor Questlog.
