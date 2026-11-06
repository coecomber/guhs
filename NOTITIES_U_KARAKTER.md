# NOTITIES U-KARAKTER (branch 110-u-karakter, van eaee06d08)

Feedback ronde 4, punt "Karakters". Bouwt verder op T-KARAKTER (zie NOTITIES_T_KARAKTER.md voor de API; die blijft geldig,
behalve wat hieronder staat).

## 1. Drie vaste slots, modus nooit aan te passen
- `Modus.vanSlot(slot)`: slot 0 = LINKS = Normaal, slot 1 = MIDDEN = Knuffelmodus, slot 2 = RECHTS = Eénvads (`Modus.slot`).
- `Karakter.modus(p)` komt nu uit het slot (een gepensioneerde Eénvads telt als Normaal tot hij weggewisseld is).
- Weg: `Karakters.zetModus`, `Modus.magNaar/zwaarte`, de knop "Makkelijker...", de schermactie `modus`, het dev-commando
  `karakter modus`. `Karakters.maak(p, slot, naam)` heeft geen modus-parameter meer.
- Dev-commando's nemen het slot als `normaal|knuffel|eenvads` (of 0/1/2): `maak <slot> <naam>`, `wissel <slot>`, `naam <slot> <naam>`.

### Gepensioneerde Eénvads-ridder (eigen keuze)
**Alleen een standbeeld, niet meer speelbaar.** Reden: Eénvads = één leven; verderspelen als "Normaal" zou een vierde slot
nodig hebben en maakt de modus zinloos. Wat er gebeurt (`Karakters.pensioen` + `naRust`):
1. hij krijgt het beeld in de zaal + chatbericht + titel "Met pensioen!" (zoals voorheen);
2. ~8,5 s later (`RUST_WISSEL_TICKS` = 170) wissel je automatisch naar je Normaal-ridder (links), anders je Knuffelridder
   (midden), anders wordt links een nieuwe Normaal-ridder gemaakt met je spelersnaam; daarna opent het scherm;
3. het rechterslot toont hem als "Rust in de Rustende Ridders-zaal" + knop "Nieuwe Eénvads-ridder";
4. maak je daar een nieuwe, dan gaat de oude naar `archief` in de lijst (MET al zijn data; de nieuwste 6 houden data,
   oudere alleen hun snapshot). Niets wordt gewist. `karakter archief` toont het archief.
- Logt iemand uit tussen pensioen en wissel: bij de volgende binnenkomst wisselt hij alsnog (`Haken.BINNEN`).

### Migratie (lijst versie 1 -> 2, `Karakter.herschik`, 1x bij `Karakter.lijst`)
- Elke ridder gaat naar het slot van ZIJN modus (huidige modus; gepensioneerde v1-ridder -> rechts als beeld).
- De ACTIEVE ridder gaat eerst, dus die krijgt altijd zijn slot en blijft actief (zijn data zit live in de attachment).
- Twee ridders met dezelfde modus: de tweede gaat met al zijn data naar `archief` (reden "dubbel").
- **Opslag-sleutel:** t-opslag's knapzak was per slotnummer ("1".."3"). Nu `Karakter.opslagSleutel(p)` = vast per karakter
  (`opslag` in de slot-info): gemigreerde = oud slot + 1, nieuwe = "k<tijd><slot>". `Opslag.KARAKTER` wijst daarheen
  (KarakterRegistry). Zo verhuist de knapzak mee met de ridder.

## 2. Bug "de eerste keer kon je geen karakter maken"
Niet 1-op-1 te reproduceren in een gametest: de gametest-server heeft geen Vadsrijk-dimensie (`Vadsrijk.level` = null,
ook de bestaande Z3-tests vallen daarop terug). Analyse van het oude pad:
- Het maakpaneel had "Maken en spelen" GRIJS zodra wisselen hier niet mocht (tijdens het openingsfilmpje, buiten stad/kampvuur
  na de eerste minuut), en toonde de reden NIET in dat paneel (alleen op het overzicht). Je typte een naam en er gebeurde niets.
- De "eerste minuut na binnenkomst" werd opgegeten door het openingsfilmpje (20 + 280 ticks).
- Eerste keer = je eigen speler stond al automatisch als slot 0; er was geen moment waarop het spel je liet kiezen.
Fixes:
- Eerste keuze mag overal (`Karakters.waaromNiet`: zolang het welkom nog niet getoond is), behalve tijdens een filmpje/baasgevecht.
- Het welkomstscherm (zie 3) opent pas NA het filmpje en zet het aankomstvenster opnieuw (60 s).
- Het maakpaneel toont de rode reden boven de knoppen; Enter in het naamveld = "Maken en spelen".
- Na een geslaagde wissel sluit het scherm (de nieuwe ridder start meteen); een mislukte heropent met de reden.
- Gametest `eersteKeer`: verse speler -> 3 kaarten (links eigen ridder Normaal, midden/rechts leeg met hun modus), `mag` = true,
  de echte schermactie "maak + spelen" maakt én wisselt; aankomst ligt in een stadsgebied zonder arena.

## 3. Vindbaarheid zonder commando's
- **Welkom:** 1x per account (`welkom` in de lijst), na het openingsfilmpje (40 ticks rust, of 15 s als er geen filmpje komt)
  opent het scherm met een welkomsttekst + chatregel waar je het terugvindt (`KarakterRegistry.welkomTick`). Ook voor bestaande
  spelers 1x (zo zien ze de nieuwe vaste slots).
- **Inventaris:** derde knopje "⚔" links van de Vadsrijk-inventaris, onder de knapzak/materiaaltas-knoppen (alleen in het Vadsrijk,
  niet in creative). **Collectietabbladen:** tab "Ridders" (volgorde 5, eerste) in de rij Rijdieren/Huisdieren/...
- **Kampvuur:** shift + rechtsklik op elk kampvuur in het Vadsrijk opent het scherm (HIGH priority, vóór kern's checkpointklik);
  1x tip "vadsrijk.karakter.tip.kampvuur" bij je eerste kampvuur.
- **NPC's:** optie heet nu "Karakters: wissel van ridder of maak een nieuwe" (Grootmeester, kampwachten), bij de Rekruteerder
  "Karakters: hoe zat dat met die drie ridders?" (opent ook het scherm).
- **Werver Wimpel bij het portaal:** 3 extra gesprekspagina's (8..10) over de drie ridders en waar je ze vindt.

## 4. Rustende Ridders-zaal in de wereldgeneratie
- `tools/vadsrijk/stad/stad_wereld.py rustende_ridders(S)`: zelfde plek als `RustendeRidders.ANKER` (-27, 63, 71) = scene
  (101, G, 199), grasveld tussen Kruidenier en de boulevardkraampjes, deur zuid naar de havenpromenade. In de t-int-scene was dit
  veld vrij (gecontroleerd op stad.npz; de oude runtime-check faalde waarschijnlijk op kaartterrein/decoratie of chunk-laden).
  9x11, roze baksteen + kersenpalen, glas-in-lood, kersen-zadeldak met dichte gevels + rond raampje, hartjestegels + roze loper,
  11 kwartssokkels (= `RustendeRidders.sokkels`), hanglampen, bloempotjes, klinkerpad, lantaarnpaaltjes + 2 guhvaandels.
  Hekken/vaandels via `S.fix_connections()` en (na merge u-hekken) de centrale `verbind.py`-pas in maak_kaart.
- Java bouwt niets meer bij chunk-laden: `RustendeRidders.tick` zet alleen beelden + naambordjes (text displays) zodra de zaal
  er staat (≥ 8 van 11 sokkels kwarts), 1x per serverstart en na elk pensioen. Geen zaal (wereld van vóór deze kaart): 1 logregel;
  `zaal bouw` blijft als dev-noodgreep voor zo'n oude wereld.
- `zaal tp` zet je nu BINNEN, in het middenpad bij de deur, kijkend naar de achterwand.
- **Integratie:** stad.npz staat in `tools/vadsrijk/_cache` (niet in git): regenereren met `maak_kaart.py --geen-cache` (of
  `_cache/stad.npz` weggooien), anders komt de zaal niet in de tegels. Gegenereerde tegels heb ik NIET gecommit.
  Alleen de zone-1-stad (tegels rond x -32..-22, z 64..80) verandert.

## 5. Wisselen
Ongewijzigd: in een stad, binnen 8 blokken van een kampvuur, of de eerste minuut na binnenkomst/welkom.

## Gedeelde bestanden (add-only, gemarkeerd U-KARAKTER)
- `npc/Werver.java`: GESPREK-lijst 3 pagina's langer (teksten in mijn generator).
- `nl_nl.json` / `en_us.json` / `tools/lang/en/c300_vr_t_karakter.json`: via `tools/features/vadsrijk_t_karakter.py`;
  8 verouderde sleutels verwijderd (makkelijker, alleen_omlaag, kies_modus, fout.modus, modus_gezet, scherm.maak, scherm.nieuw, ...).
- `tools/vadsrijk/stad/stad_wereld.py`: functie `rustende_ridders` + 1 aanroep in `main`.
- Rest zit in `feature/vadsrijk/karakter/`.

## Tests
`-Pgt=VadsrijkKarakter`: migratie, slotIsolatie, accountBreed, vasteModi, pensioenEnNieuweEenvads, migratieV1, eersteKeer, namen.
Plus `VadsrijkTOpslag` (knapzak-sleutel). Resultaat: 14/14 groen (`-Pgt=VadsrijkKarakter,VadsrijkTOpslag`).

## Gecontroleerd
- `maak_kaart.py --zone 1 --alleen=-1,1` (tegel x -64..-1, z 64..127): de zaal staat in de tegel (sokkel -30,64,68 = kwarts,
  vloer -27,63,71 = roze loper), render van de tegel ziet er goed uit op het grasveld; daarna `verbind.py` op die tegel
  (13 blokken verbonden). Tegel + WereldPlekken + POI daarna teruggezet (niet gecommit).
- **Gevonden bug van u-hekken (gemerged):** maak_kaart roept `verbind.py --alleen -1,1` aan; argparse leest een negatieve
  tegel als optie -> "expected one argument" en maak_kaart breekt af. Fix voor hen: `f"--alleen={...}"` doorgeven.
  (Ook maak_kaart zelf: gebruik `--alleen=-1,1` met `=`.)

## Open / niet gedaan
- Niet in een client geklikt (scherm-layout 380x226 met welkomsttekst, de ⚔-knop, het tabblad); geen AutoCheck gedaan.
- De zaal is alleen als scene-render gecontroleerd, niet in een geregenereerde wereld.
- u-hekken had bij mijn merge nog ongecommit werk in `RustendeRidders.zet` (Verbind.vorm): bij hun commit kan dat een kleine
  conflict geven (mijn bestand laat `zet` ongemoeid, dus triviaal).
