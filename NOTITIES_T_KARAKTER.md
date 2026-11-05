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
Package `feature/vadsrijk/karakter/`:
- `Karakter` - de API + de splitsing van SpelerData in karakterdeel / accountdeel (`alleenKarakter`, `metAccount`) + migratie.
- `Karakters` - maak / wissel / hernoem / modus omlaag / pensioen; de plekregel (`waaromNiet`); een wissel-lock per speler.
- `Modus` - NORMAAL / EENVADS / KNUFFEL (+ factoren, badge, "alleen omlaag").
- `Badges` - badge bij de naam (server NameFormat + TabListNameFormat, client NameFormat via payload-kaart).
- `RustendeRidders` - SavedData `guhs:vr_rustende_ridders` (lijst) + de zaal (gebouwd door code) + standbeelden.
- `KarakterScherm` (server) + `client/KarakterSchermClient` (EcoScherm-stijl) + `client/KarakterClient`.
- `KarakterPayloads` - Scherm / Doe / Badges.
- `KarakterRegistry` - haken (XP_FACTOR, Schade.VOOR_KLAP, FLAUW, BINNEN, login/logout, tick, NPC-opties) + `/guhs vadsrijk karakter`.
- `KarakterTest` (migratie-test) + `VadsrijkKarakterGameTests` (5 tests).
- Teksten: `tools/features/vadsrijk_t_karakter.py` (NL in nl_nl via build, EN in `tools/lang/en/c300_vr_t_karakter.json`).

### Hoe de wissel werkt
1. `Karakter.VOOR_WISSEL`; in het Vadsrijk ook kern `Haken.BINNEN(false)` (handel weg, rijdier naar huis, huisdier weg, verhaal-loopjes
   gewist) + statussen gewist + `closeContainer`.
2. Zit de Vadsrijk-inventaris live (`d.binnen`): `Inventaris.naarThuis` (kern's eigen "nooit iets kwijt"-wissel) zet hem in
   `rijkInventaris` van het oude karakter.
3. SpelerData -> tag; het karakterdeel (zonder accountvelden/-slices) gaat in `s<oud>.data`, plus waar hij stond (`plek`) en
   een snapshot (level, klasse, munten) voor het scherm.
4. Het nieuwe karakterdeel + het accountdeel van nu -> SpelerData; `Inventaris.naarRijk` haalt zijn inventaris eruit.
5. In het Vadsrijk: teleport naar zijn eigen plek (nieuw karakter: `Gebieden.AANKOMST` = Knuffelkamp, dus het openingsfilmpje
   start), `Haken.BINNEN(true)` (huisdier terug, quest-markers vies), Sieraden + HUD sync, titelkaart.
6. `Karakter.GEWISSELD` + badge verversen.
Alles in 1 servertick, in player.dat: geen dupe via accountopslag (een wissel-lock voorkomt dubbel klikken).

## Eigen keuzes
- **Actieve-karakter-in-de-attachment** in plaats van elke store om te bouwen naar `Map<Sleutel,...>`: alles in SpelerData is
  daardoor automatisch per karakter (ook stores die andere slices vannacht toevoegen) en de bestaande code hoeft niet te veranderen.
  Default = per karakter; accountbreed moet je expliciet zeggen. Zo kan een vergeten store nooit "doorlekken".
- **Rijdieren + huisdieren: ook hun levels accountbreed** (voorstel zei levels per karakter). Reden: eenvoud, en R1 noemt
  "mounts, huisdieren" accountbreed zonder uitzondering. Wil de gebruiker het toch per karakter: alleen de `accountBreed`-regels
  in `Karakter` aanpassen (bv. alleen `rijdieren` bezit splitsen) - kleine wijziging.
- **Tips accountbreed** (geen uitlegtips opnieuw), **verhaalfilmpjes (`gezien`) per karakter** (nieuw karakter = verhaal opnieuw).
- **Markt**: aanbiedingen + opbrengst blijven per speler-UUID (de bestaande MarktData). De opbrengst komt bij het karakter dat
  hem ophaalt. Munten tussen je eigen ridders schuiven kan dus via de markt; geen probleem (geen IJzerguh).
- **Party, arena-bijdragen, gesprekken**: runtime per UUID; prima, want je speelt maar 1 karakter tegelijk.
- **Wissel-plekregel**: stad (`Gebieden.Gebied.stad`), binnen 8 blokken van een kampvuurblok, of de eerste 60 s na binnenkomst
  (het scherm bij de ingang). Niet in een baasarena, niet tijdens een scene. Buiten het Vadsrijk mag het altijd (wisselt alleen data).
- **Nieuw karakter begint in de Knuffelkamp** (`Gebieden.AANKOMST`), een bestaand karakter waar hij stond, anders zijn kampvuur.
- **Eénvads**: pensioen bij flauwvallen BUITEN een stad (in creative/spectator nooit). +10% xp zolang hij leeft.
  **Knuffelmodus**: 50% inkomende schade (alle schade via `Schade.doe`, ook omgeving), -10% xp.
  Factoren staan in `Modus` (makkelijk te tunen).
- **Badges**: ❤ goud = Eénvads, ☁ roze = Knuffel, ✦ goud = met pensioen. Alleen zichtbaar terwijl je in het Vadsrijk bent.
- **Rustende Ridders-zaal = structuurstuk door code**, niet in de kaartgenerator (de kaart wordt vannacht door anderen omgebouwd;
  zo botst er niets). Plek: `RustendeRidders.ANKER` = (-27, 63, 71), Zwamzoet, grasveld west van het plein tussen de Kruidenier
  (deur -35,64,68 blijft vrij: 3 blokken tussenruimte) en de huizen, ingang zuid op de havenpromenade (z 80). 9x11, wanden 5 hoog.
  Wordt 1x gebouwd zodra die chunk geladen is EN het veld nog vrij is (anders 1 waarschuwing in de log; dan `zaal bouw hier`).
  Beelden = pantserstandaards (getagd `vr_rustende_ridder`) met spelershoofd; de 11 nieuwste ridders krijgen een sokkel.
- **Scherm-opening**: geen nieuwe toets (alle goede toetsen bezet); via commando, NPC-keuze en automatisch bij binnenkomst (2+ ridders).

## Gedeelde bestanden aangeraakt (add-only, gemarkeerd T-KARAKTER)
- `feature/vadsrijk/VadsrijkFeature.java`: 2 regels (register + payloads).
- `feature/vadsrijk/client/VadsrijkClient.java`: 1 regel (KarakterClient.init).
- `tools/features/__init__.py`: 1 regel in FEATURES (`vadsrijk_t_karakter`).
- `assets/guhs/lang/nl_nl.json` + `en_us.json`: 52 nieuwe sleutels `vadsrijk.karakter.*` (door de generator).
- Geen wijziging in kern-klassen: alles via bestaande haken (`Spelers.XP_FACTOR`, `Schade.VOOR_KLAP`, `Haken.FLAUW/BINNEN`,
  `QuestMotor.EXTRA`).

## Hervatpunt / open
(wordt aangevuld)
