# Notities: Sherlock-slice (Guhs 1.9 "Een opknapper!", branch 190-opk-sherlock)

Het goedgekeurde Sherlock-verhaal (`erfenis-voorstellen/sherlock.md` + rondes 8, 13, 16): proloog + 11 lineaire
hoofdstukken, alleen op vaste momenten, met vergrootglas, getuigen, speurbord, Speurboekje en de rechtszaak als finale.

## Wat er is

**Java** (`feature/erfenis/sherlock/`):
- `Sherlock` — API + verloop: `start(speler, zaakId)`, `openBureau(speler)` (Speurboekje), `zaakKlaar(server, uuid)`
  (voor het vergrootglas-wolkje boven het bureau in het bovenaanzicht), `beloningen(server, uuid)` (deco-ids + vlaggen),
  `opgelost(server, uuid, zaakId)`, en de luisteraar op `ErfenisMomenten.luister`.
- `Zaken` / `Zaak` — de 11 zaken (mechaniek: kaartjes, plekken op het perceel, verdachten, dader, getuigen, beloning).
- `Voortgang` in `SherlockData` (eigen SavedData `data/guhs/erfenis_sherlock.dat`, per speler-uuid).
- `Sporen` — sporen op het eigen perceel (tegel-coördinaat per zaak; staat er iets, dan schuift het spoor naar het
  dichtstbijzijnde vrije gras), het glinsteren (alleen de eigenaar ziet het, alleen met het vergrootglas vast), vinden.
- `Vergrootglas` (item `guhs:sherlock_vergrootglas`, look van het bestaande detective-vergrootglas).
- `Cast` — alle personages tijdelijk in de wereld (getagd `guhs_sherlock`, nooit opgeslagen: wat een chunk ervan
  bewaarde wordt bij het laden weggegooid); `Gesprek` + `Regels` — dialogen in het praatscherm (verhaal.Praat).
- `Guhrrit` — makelaar Guhrrit, **ook voor de tutorial** (zie hieronder).
- `Rechtbank` + `SherlockScenes` — de finale: rechtszaal-set in een gereserveerde cel, 4 klik-verder-cutscenes.
- `client/BureauScherm` (Speurboekje + kurken speurbord met rode draadjes), `client/SherlockClient`.
- `SherlockCommando`, `SherlockGameTests` (6 tests, batch `sherlock`).

**Resources**: `tools/features/erfenis_sherlock.py` is de enige bron van alle teksten (552 sleutels, NL; en_us krijgt
dezelfde Nederlandse tekst), wie welke dialoogregel zegt (`data/guhs/erfenis/sherlock/regels.json`), de 8 modellen +
texturen + animaties van de cast, en het item. Draaien: `PYTHONPATH="tools;tools/features" python tools/features/erfenis_sherlock.py`.

**Cast** (nieuwe `GuhNpcEntity.Kind`s in blok `<sherlock>`): SHERLOCK_GUH (ruitjes-deerstalker, slaperige oogleden,
bellenblaaspijp met opstijgende belletjes, knikt in slaap en schrikt wakker), DOKTER_VADSON (bolhoed, snor, strikje,
rood Speurboekje), MAKELAAR_GUHRRIT (colbert, das, klembord, zonnebril op het hoofd), GUHRRIT_VAKANTIE (zonnebrand,
bloemetjeshemd, sombrero, zonnebril op), MORRIS_MIKA (glimmend grijs pak, kuif, stropdas, koffertje), PIM_MIKA
(stagiairspasje, roze strikje, kiemplantje op zijn hoofd), OEHOE (knuffel-uil: grote gele ogen, snavel, pluimpjes),
KNABBELHAMER (enorme pruik, toga met jabot, kaasknabbel-hamer).

## Shared files (één regel per plek)
`Features.java` (register, payloads, creative), `FeaturesClient.java` (init), `GuhNpcEntity.Kind` (eigen markerblok
`<sherlock>`), `tools/features/__init__.py` (feature-lijst).

## Stubs (STUB-kern, de lead vervangt ze bij de merge)
- `ErfenisMomenten` — met `melding(...)` én `luister(...)` (stuurt door naar luisteraars als de speler online is).
  De kaart-slice heeft een eigen stub met alleen `melding`: mijn versie is een superset.
- `ErfenisEconomie` — exacte kopie van de stub van de kaart-slice (Guhlden via `geef(server, uuid, "guhlden", n)`).
- `ErfenisDeco` — exacte kopie van de stub van de kaart-slice (`geef(server, uuid, decoId)`).

## Momenten → hoofdstukken
| # | Zaak (id) | Trigger (naast: vorige opgelost) |
|---|---|---|
| proloog | — | eerste `vondst:*` (welk id dan ook) → roestig sleuteltje |
| 1 | eieren | `gebouw_gebouwd:detectivebureau` (dan ook: welkom-scène + vergrootglas) |
| 2 | kruimels | `gebouw_gebouwd:bakkerij` |
| 3 | spook | `hoeve_level:4` |
| 4 | wol | `gebouw_gebouwd:weverij` + `bestelling_geleverd:<n>` met n ≥ 10 |
| 5 | kelder | `hoeve_level:5` |
| 6 | koeienbel | `gebouw_gebouwd:muziektent` |
| 7 | paaltjes | `hoeve_level:7` |
| 8 | licht | `gebouw_gebouwd:kaarsenmakerij` |
| 9 | blikje | `hoeve_level:8` |
| 10 | voorbereiding | meteen na 9 |
| 11 | rechtszaak | `hoeve_level:9` |

Momenten worden onthouden (gebouwd-set, hoogste Hoeve-level, hoogste bestellingen-totaal), dus een trigger die eerder
kwam laat de zaak later "klaar in de brievenbus" liggen. Zaken blokkeren nooit iets.

## Beloningen (deco-ids en vlaggen, via `Sherlock.beloningen`)
Deco (ook naar `ErfenisDeco.geef`): `hartjesnest`, `theekransje_tafeltje`, `uilenhuisje`, `reuzensjaal`,
`koeienbel_lantaarn`, `moestuintje_pim`, `glimwormguhtjes`, `oudste_boompje`, `getuigenbank`, `standbeeld_opa_knabbel`,
`knabbelparkinkje`. **Deze staan niet in deco.md** (daar zitten geen Sherlock-stukken in): de vormen-slice heeft er nog
geen vorm voor; de lead/vormen moet ze toevoegen aan de deco-catalogus.
Vlaggen voor andere slices: `recept_theetaartje` (bakkerij), `oma_bestelling` (kaart: vaste Oma-bestelling),
`opas_kelder` (kern/vormen: kamer in de Hoeve), `bertha_optreden` (dorpsfeest), `paaltjes_terug`, `testament`,
`bureau_eindlook` (vormen: Detectivebureau eindlook). Item: `guhs:detective_pet` (de bestaande Guhlock-pet = Sherlocks
ruitjespet als cosmetic voor je guhs). Guhlden per zaak: 150 / 250 / 400 / 600 / 900 / 1200 / 1800 / 2400 / 3000 / 3500 / 6000.
Titels (feature/titels, eigen ids want `speurneus` bestond al bij Safari): `sherlock_speurneus` ("Dorpsspeurneus"),
`sherlock_spokenjager`, `sherlock_archiefguh`, `sherlock_grenswachter`, `sherlock_lichtpuntje`, `sherlock_schatbewaarder`,
`sherlock_meesterspeurneus`.

**FTB (later)**: de quests `erfenis_zaak_<n>` / `erfenis_bezwaar` kunnen `Sherlock.opgelost(server, uuid, zaakId)`
lezen (hook-plek gemarkeerd in `Sherlock.losOp`).

## Guhrrit voor de tutorial (kern)
```java
GuhNpcEntity g = Guhrrit.verschijn(level, hekjePos);            // of verschijn(level, pos, yaw, vakantie)
Guhrrit.bijKlik((speler, guhrrit) -> { ...; return true; });    // true = afgehandeld; anders zegt hij een standaardzin
Guhrrit.zeg(speler, g, "erfenis_tut1", List.of("erfenis.tut.0", "erfenis.tut.1"), new Praat.Optie(1, "erfenis.tut.ja"));
Praat.luister("erfenis_tut1", (speler, spreker, optie) -> ...); // -1 = uitgelezen/gesloten
Guhrrit.weg(g);                                                  // op vakantie (wolkje)
```
Standaardzinnen: `sherlock.guhrrit.0..5` ("Bouwkundig rapport: vads. Emotioneel rapport: heel vads." enz.).
Hij is tijdelijk zoals de hele cast: is er niemand binnen 40 blokken, dan gaat hij weg; roep `verschijn` opnieuw aan.

## Keuzes waar de documenten stil waren
- **Sporen** zijn deeltjes (gekleurd stof per spoorsoort, pootafdrukjes als drie stipjes, af en toe een glinster),
  alleen voor de eigenaar en alleen met het vergrootglas in de hand; geen blokken, dus nooit in de weg bij bouwen.
  Een spoor ligt op een vaste tegel per zaak; staat daar een gebouw, dan schuift het naar het dichtste vrije gras.
  Vinden: kijk ernaar (of sta erbovenop) binnen 6 blokken en gebruik het glas. Anders: "warm/heet/koud".
  Hoek: "Sporen 2/4 · <zaak>" in de actiebalk.
- **Elke zaak heeft 5 kaartjes**: 3-4 sporen + 1 getuigenis, waarvan precies één rode haring (wijst naar een
  onschuldige verdachte). 3 passende draadjes aan de dader + geen haring = opgelost. Fout raden: lieve, beledigde
  reactie + hint van Vadson; haring aan de dader: Vadson kleurt dat kaartje rood. Nooit straf; alleen de sticker
  "In één keer goed!" mis je.
- **Getuigen** (Bonnie, Meelmuts, Bouwer Bram, Wolletje, Koos, Oehoe, Pim, Oma Breiguh, Hendrik, Guhrrit) staan zelf in
  je dorp zolang de zaak loopt (op een vaste tegel). Ze hergebruiken bestaande NPC-looks (BOERINNEGUH, BAKKERGUH,
  BOUWVAKKERGUH, STALKNECHTGUH, KROEGBAASGUH, DROOM_OMAGUH; Hendrik = een kip) met hun eigen naam. Ze zijn dus nog niet
  de "vaste verhaalbewoners" van kern: als kern die later maakt, kunnen ze de rol overnemen.
- **Zaak 4**: de oplossing (Oma Breiguh) stond niet tussen de verdachten in het voorstel; ze is de 4e verdachte
  ("Oma Breiguh"), anders kan je haar niet aanwijzen.
- **Zaak 9**: 4 verdachten (Morris, Pim, Oehoe, de makelaar); het knoopje van Morris is de rode haring.
- **Zaak 10** heeft geen dader: praat met alle 5 getuigen; het bord hangt hun kaartjes aan Opa Knabbels testament.
- **Morris duwt** in hoofdstuk 3 en 6 echt even (klein zetje, nooit schade).
- **De rechtbank** staat in cel (0, -3000) van `guhs:erfenis` (nooit een basis; ver van de spiraal), één keer
  gestempeld (goudblok onder het midden = "staat er"). Iedereen deelt dezelfde set; de cutscene-acteurs zijn per
  kijker. Je gaat erheen, kijkt de opening, kiest 3x bewijs (5 opties) in het praatscherm; na de uitspraak ga je terug
  naar je dorp. Halverwege weg? De ronde wordt onthouden; klik de rechter om verder te gaan.
- **Detectivebureau-plek**: ik weet niet waar het bureau op het perceel staat. Personages voor een dialoog verschijnen
  daarom naast de speler (halve cirkel ervoor). Hook `Sherlock.bureauPlek` is er voor later (nu ongebruikt).
  Kern roept `Sherlock.openBureau(speler)` aan bij een klik op het bureau.
- **Titels** heten `sherlock_*` omdat Safari de id `speurneus` al had.

## Nog niet / voor de lead
- Deco-vormen voor de 11 Sherlock-deco's (vormen-slice) en hun plek in de decowinkel/opbergen (kern).
- Kern: `ErfenisMomenten` echt afvuren; `zaakKlaar` tonen als wolkje boven het bureau; `openBureau` bij klik.
- De Getuige-NPC's gebruiken vaste perceeltegels; als kern bewoners/gebouwen met posities kent, kunnen getuigen bij
  "hun" gebouw gaan staan.
- FTB-quests (later): zie hierboven.

## Testen in de dev-client (Nederlandse commando's)
1. `/guhs erfenis` (naar je erfenis), dan bijvoorbeeld:
   - `/guhs erfenis sherlock moment gebouw_gebouwd:detectivebureau` — Sherlock en Vadson trekken in, je krijgt het
     vergrootglas, zaak 1 ligt klaar (klik op "[Open het bureau]" in de chat, of `/guhs erfenis sherlock bureau`).
   - In het Speurboekje: **Begin de zaak!** Houd het vergrootglas vast en loop rond: gekleurd glinsterstof = spoor. Kijk
     ernaar en klik rechts. Praat met Boerin Bonnie (rechtsklik). Vadson (rechtsklik) geeft tips.
   - Speurbord: klik een kaartje, dan een verdachte (rood draadje); rechtsklik op een kaartje = los. 3 draadjes → **Wijs aan!**
2. Snel springen: `/guhs erfenis sherlock zaak <id>` (eieren, kruimels, spook, wol, kelder, koeienbel, paaltjes, licht,
   blikje, voorbereiding, rechtszaak), `/guhs erfenis sherlock vind` (alle kaartjes), `/guhs erfenis sherlock demo`
   (draadjes gespannen + bord open), `/guhs erfenis sherlock volgende` (huidige zaak meteen opgelost),
   `/guhs erfenis sherlock info`, `/guhs erfenis sherlock reset`.
3. Momenten zoals kern ze stuurt: `/guhs erfenis sherlock moment hoeve_level:4`, `... moment bestelling_geleverd:10`,
   `... moment vondst:iets`.
4. Finale: `/guhs erfenis sherlock zaak rechtszaak` (je gaat naar de rechtbank, scène, 3x bewijs kiezen).
   Alleen kijken: `/guhs erfenis sherlock rechtbank`. De hele cast op een rij: `/guhs erfenis sherlock cast`.
5. AutoCheck: `gradlew runAutocheckClient -PautocheckScript=tools/autocheck/190_sherlock.txt`.

## Stand: KLAAR
- `compileJava` groen; `-Pgt=SherlockGameTests` groen (6 eigen tests); `-Pgt=TaalsleutelsGameTests,TitelsGameTests` groen.
- AutoCheck `tools/autocheck/190_sherlock.txt` gedraaid en bekeken (cast, Speurboekje, speurbord + tooltip, boekje na
  oplossen, rechtbank). Na de eerste ronde aangepast: vachtkleur per personage (Sherlock karamel, Vadson blauwgrijs,
  Guhrrit mint, op vakantie roze verbrand, Oehoe bruin, de rechter roze met een lila pruik), Morris een vlinderdas (zijn
  lange das leek een tong), de rechter zit hoger (was half verstopt achter zijn bank), de prologregel past op de linkerbladzijde.
- Bekende, niet-mijne fout: `upstreamsamenkindsvoorguhpixel` (SNOES heeft geen Guhdex-pagina) bestond al.
