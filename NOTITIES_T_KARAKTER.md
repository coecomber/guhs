# NOTITIES T-KARAKTER (karakterslots + spelmodi, branch 110-t-karakter)

## API voor andere slices (LEES DIT EERST)

Package `nl.juiced.guhs.feature.vadsrijk.karakter`, klasse `Karakter`.

- **De kern-attachment `SpelerData` (`Spelers.data(p)`) bevat altijd het ACTIEVE karakter.** Bij een wissel wordt de hele
  SpelerData (alle velden + alle `extra(slice)`-compounds) in het oude slot opgeslagen en die van het nieuwe slot geladen.
  - => Alles wat je in `Spelers.data(p).extra("<jouwslice>")` zet, is **automatisch per karakter**. Geen extra werk.
  - Synoniem: `Karakter.karakter(p, "<slice>")`.
- **Accountbreed** (blijft staan bij een wissel):
  - `Karakter.account(p, "<slice>")` -> een CompoundTag (= `extra("account_<slice>")`), opgeslagen in player.dat.
    Gebruik dit voor bank, materiaaltas, kledingkast/vleugels/effecten (t-opslag, t-kleding).
  - of registreer een bestaande slice/sleutel: `Karakter.accountBreed("slice")` / `Karakter.accountBreed("slice", "sleutel")`
    (bij mod-init, bv. in je registry-klasse).
- **Stores BUITEN de attachment** (SavedData, bestanden) die per speler-UUID werken:
  - per karakter: sleutel met `Karakter.sleutel(p)` -> `record Sleutel(UUID speler, int slot)`, `.tekst()` = `"uuid/slot"`;
  - accountbreed: gewoon `p.getUUID()`.
  - `Karakter.slot(p)` = het actieve slot (0..2). Slot 0 = het oorspronkelijke (gemigreerde) karakter.
- **Haken:** `Karakter.VOOR_WISSEL` (Consumer<ServerPlayer>, oud karakter nog live: sluit schermen, stuur entities weg) en
  `Karakter.GEWISSELD` (BiConsumer<ServerPlayer, Wissel(van, naar, nieuw)>, na de wissel: hersync je HUD/schermen).
  Bij een wissel IN het Vadsrijk vuurt kern's `Haken.BINNEN` ook (false, dan true), zoals bij uit- en weer ingaan.
- **Modus:** `Karakter.modus(p)` -> `Modus.NORMAAL | EENVADS | KNUFFEL`; `Karakter.gepensioneerd(p)`; `Karakter.naam(p)`.
- **Migratie:** niets te doen. Een oude save heeft geen lijst; `Karakter.lijst(p)` maakt hem aan met de huidige data als slot 0.

Wat al accountbreed is: `extra("huisdieren")` (hele slice), `extra("economie")` sleutels `rijdieren`, `actief`, `starter`,
`titels`, `post`, en de SpelerData-velden `binnen`, `opgeborgen(_slot)`, `terug`, `gevonden`, `tips`.

## Gebouwd
(wordt aangevuld)

## Eigen keuzes
(wordt aangevuld)

## Gedeelde bestanden aangeraakt
(wordt aangevuld)

## Hervatpunt / open
(wordt aangevuld)
