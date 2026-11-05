# NOTITIES T-Z4BOOT: het eigen guh-zeilbootje (zone 4, beslissing W-2)

Branch `110-t-z4boot` (vanaf dbf82899d). Code: `src/main/java/nl/juiced/guhs/feature/vadsrijk/zone4/boot/`. Generator:
`tools/features/vadsrijk_t_z4boot.py` (1 regel in FEATURES). Spiekbrief: `SPIEK_T_Z4BOOT.md`.

## Wat er gebouwd is
- **Rijdiersoort `ZEILBOOTJE`** in `RijdierSoort` (6e, `water() == true`, snelheid 0.30 = 300%, geen sprong, bron "quest"). Het zit dus
  in het N-scherm met level/xp (varen geeft xp, per blok zoals rijden), de X-toets roept het, het poeft weg bij afstappen/uitloggen/
  geraakt worden/arena, en het wordt nooit opgeslagen (net als elk rijdier).
- **`BootEntity extends RijdierEntity`** (`vr_eco_zeilbootje`, 2.0 x 1.1): eigen `travel` met `BootFysica` (puur, getest):
  W = zeil hijsen (ACC 0.012/tick tot topsnelheid = soortsnelheid x 2 = 0.6 b/t op lv 1), loslaten = uitglijden (x0.985/tick),
  S = remmen en 25% achteruit, A/D = boeg draaien (3.6 -> 2.0 graden/tick; het beeld van de speler draait mee). Drijft op het
  wateroppervlak (diepgang 0.40) met een zachte dobber; op land schuurt het (20% snelheid, stap-hoogte 0). Wind: draait over 2
  dagen rond met een zwaai, factor 0.85..1.15. Spetters/belletjes/geluid client-side. Geen verdrinking/omgevingsschade.
- **De Kringstroom** (`BootZee` + `BootFysica.Kringstroom`): ellips midden (2140, -491), 470 x 290, band 0.93..1.0. In de band
  verdwijnt je snelheid naar buiten (helemaal op de buitenlijn), duwt de stroom je naar binnen (tot 0.3 b/t, voorbij de lijn
  meer) en draait je boeg naar binnen. Getest uit 16 richtingen met volle zeilen: komt nooit door de lijn; van buiten de lijn
  drijft hij vanzelf terug tot binnen de band. Verder dan 1.4 x de ellips: geen stroom (dev-proefvaart elders).
- **Steigers**: uit `Z4WereldPlekken.ALLE` (POI-ids `boot_steiger_<id>`, via reflectie) zodra t-z4wereld die levert; tot dan 9
  PLACEHOLDERS bij de eilandmiddens van WERELDINDELING.md (y 64). Binnen 10 blokken: "Shift = aanmeren" en die steiger wordt de
  laatste; Shift = je stapt op de steigerplek. Uitloggen op zee (vlag `opzee`, gezet terwijl je vaart, gewist zodra je 1 s niet
  in de boot zit) = bij inloggen op de laatste steiger (LOWEST-prioriteit login-listener).
- **Andere boten verboden** in de dimensie: `AbstractBoat` + elk entity-/item-id dat op *boat* / *raft* eindigt (ATM-mods),
  plaatsen geweigerd met een berichtje, joinen geweigerd.
- **Krijgen**: `Boot.geef(speler)` (API voor t-z4verhaal) = rijdier + questvlag `Boot.gekregen(p)` + tip + hook `Boot.GEKREGEN`
  (1x per speler). Item "Bootje in een fles" (`vr_item_zeilbootje`, ook het icoon in N-scherm/HUD): rechtsklik = `Boot.geef`.
- **Varianten** (`BootVariant`, 3 assen): zeil (6 kleuren = 6 textures), wimpel (4 botten), boegbeeld (4 botten); start / winkel
  (brons, in het bootscherm) / beloning (`Boot.geefVariant(p, "boeg_draakje")`). Opslag `extra("boot")`.
- **Bootscherm** (tab "Zeilbootje", volgorde 15 in CollectieTabs, gebouwd op `EcoScherm`, eigen payloads `vr_z4boot_scherm/doe`),
  **windpaneel** rechts op het scherm tijdens het varen.
- **Model** (generator): roze romp met guhgezicht + oortjes op de boeg, witte reling, roze streep, dek, 2 bankjes, roer; mast +
  giek + zeil (guhgezicht aan beide kanten, oortjes bovenop); wimpel- en boegbeeldbotten; lantaarn aan een paaltje achter
  (glowmask); guh-matroosje met petje voorin. Animaties idle / varen / hard. Preview gerenderd (scratchpad), ziet er lief uit;
  de gebruiker beoordeelt het echte uiterlijk in de client.

## Eigen keuzes (de gebruiker sliep)
- X-toets: **in het water of op een steiger = bootje, op land = landrijdier**, ook als het bootje "actief" gekozen is. Zo hoef je
  nooit te wisselen in het N-scherm.
- Het bootje mag alleen op de zee van zone 4 (binnen de Kringstroom); `proefvaart` (dev) = overal.
- Besturing zoals een echte boot (A/D draaien de boeg), niet "kijk-sturen" zoals de landrijdieren: met de muis kun je rondkijken.
- Varianten: eigen opslag en eigen scherm i.p.v. het N-scherm aan te passen (t-rijdier werkt daar parallel aan kleur/patroon/tuig).
- Prijzen: zeil 600/900, wimpel 300, boegbeeld 800 brons; sterrennacht-zeil, Vadsrijk-wimpel en draakje = beloning (nog niemand
  geeft ze: t-z4verhaal / zone-4-baas kan `Boot.geefVariant` aanroepen).
- Geraakt worden door een monster laat het bootje schrikken zoals elk rijdier (je valt in het water). Omgevingsschade niet.
- Steiger-placeholders liggen bij de eilandmiddens: daar is nog geen water. Testen kan met proefvaart op elk meertje.

## Gedeelde bestanden (add-only, gemarkeerd `T-Z4BOOT`)
- `economie/RijdierSoort.java`: enumwaarde `ZEILBOOTJE` (laatste) + `water()`.
- `economie/EcoSpul.java`: 2 switch-cases (`holder`, `icoon`) die naar `zone4.boot.BootSpul` wijzen.
- `economie/Rijdieren.java`: hook in `toets` (`Boot.kiesSoort`) en in `roep` (`Boot.plaats` voor `s.water()`).
- `economie/client/EconomieClient.java`: renderer-lus slaat `water()`-soorten over (eigen renderer in BootClient).
- `economie/VadsrijkRijdierenGameTests.java`: `rooster` verwacht nu ook "zeilbootje".
- `VadsrijkFeature.java` (register + payloads), `client/VadsrijkClient.java` (init): elk 1 regel.
- `tools/features/__init__.py`: `'vadsrijk_t_z4boot'` achteraan FEATURES. Lang-bestanden: alleen nieuwe sleutels
  (`vadsrijk.z4boot.*`, `entity.guhs.vr_eco_zeilbootje`, `item.guhs.vr_item_zeilbootje`, `vadsrijk.economie.rijdier[.eigen].zeilbootje`,
  `vadsrijk.economie.stal.nog_niet.zeilbootje`).

## Voor de andere agents / integratie
- **t-z4verhaal:** `nl.juiced.guhs.feature.vadsrijk.zone4.boot.Boot.geef(ServerPlayer)` vroeg in het verhaal; questvlag
  `Boot.gekregen(p)`; hook `Boot.GEKREGEN.add(p -> ...)`; beloningen `Boot.geefVariant(p, "zeil_sterrennacht" | "wimpel_vadsrijk" | "boeg_draakje")`.
- **t-z4wereld (POI-verzoek):** aanlegsteigers als POI `boot_steiger_<id>` in `Z4WereldPlekken.ALLE` (package
  `...vadsrijk.zone4.wereld`), id's: lampionhaven, knuffeltij, bloemeneiland, strandfeest, koraalbaai, vliegereiland, vuurtoren,
  mysterie, opstijgpier (andere id's mogen ook: alles met dat voorvoegsel telt). De plek = waar je op de planken staat (1 blok
  boven de steigervloer, vrije lucht erboven); binnen 10 blokken moet open water liggen (3x3 water, 2 lucht erboven) zodat X het
  bootje kan neerzetten. Kringstroom anders? `BootZee.zetKringstroom(cx, cz, a, b, binnen)`. Lampionhaven = thuissteiger.
  Taalsleutel per steiger: `vadsrijk.z4boot.steiger.<id>` (voor nieuwe id's toevoegen).
- **t-rijdier:** het bootje is een `RijdierSoort` met `water() == true`; als de nieuwe kleur/patroon/tuig-assen voor alle
  soorten gaan gelden: sla `water()`-soorten over (het bootje heeft zijn eigen `BootVariant`-assen en scherm) of koppel ze.
  Het N-scherm toont het bootje gewoon (6 rijen passen; "Sprong 0.0" staat er dan bij).
- Karakterslots (accountbreed: mounts): bootdata staat in `SpelerData.extra("boot")` + de rijdierentag van economie.

## Tests
- `VadsrijkZ4BootGameTests` (batch `vadsrijk_z4boot`): zeilen, kringstroom, drijven (echt water), krijgenEnUiterlijk, welkRijdier,
  steigers, geenVreemdeBoten. Plus `VadsrijkRijdieren` (rooster aangepast).
- Draaien: `-Pgt=VadsrijkZ4Boot,VadsrijkRijdieren`.

## Open punten
- Echte steigers + zee van zone 4 (t-z4wereld); daarna in de client: aanmeren op echte planken, Kringstroom aan de rand.
- Niet in de client getest (geen AutoCheck-run gedaan, zie eindrapport); stuurgevoel/snelheid/wind zijn knoppen in `BootFysica`.
- Het windpaneel overlapt mogelijk met andere HUD-elementen rechts in het midden; verschuiven als dat zo is.
- Wie de beloning-varianten geeft is nog niet beslist.
