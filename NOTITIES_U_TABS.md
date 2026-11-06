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

(De rest van deze notities volgt onderaan zodra het af is.)
