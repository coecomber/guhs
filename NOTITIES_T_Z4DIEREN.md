# NOTITIES T-Z4DIEREN - zone 4 "De Lampionkust": Ouwe Dobber + de guh-vliegers

Nachtronde T (2026-11-06). Worktree `guhs_vadsrijk/t-z4dieren`, branch `110-t-z4dieren` (van 110-t-z4wereld 09d93c3c9).

## Wat er gebouwd is

### 1. Ouwe Dobber, de reuzenschildpad (`feature/vadsrijk/zone4/dieren/OuweDobber`, `DobberRoute`)
- Naam uit t-z4verhaal: daar heet de schildpad al **Ouwe Dobber** (Schelpie woont "op de schildpad").
- GeckoLib-model (gegenereerd, `tools/features/vadsrijk_t_z4dieren.py`): schild in 4 lagen met bloemetjes erop, kop met lief
  gezichtje, guh-oortjes, roze blosjes en een hibiscus, 4 vinnen, staartje. Model op 1/4 grootte, renderer schaalt x4:
  schild 14 x 11 blokken, bovenkant 3,5 blok boven het water. Animaties `zwem` (vinnen peddelen, kop knikt, schommelt) en
  `rust` (kijkt rond, oortjes wiebelen).
- **Zwemt als pure functie van de speltijd** (`DobberRoute`): server en clients rekenen dezelfde plek uit, er gaan geen
  posities over de lijn (vloeiend voor passagiers, geen schokken in multiplayer). De client negeert serverposities
  (eigen `InterpolationHandler`). Cyclus: 30 s wachten bij zijn halte naast de steiger van het Schildpadeiland, dan een
  rondje van ~319 blokken met 0,1 blok/tick (2 blok/s, ~2,7 min), rustig optrekken/afremmen.
- **Route:** de punten van `SCHILDPAD_ROUTE` 4..20 + 3 eigen punten bij de halte (`tools/vadsrijk/zone4/dieren4.py`). Het
  statische eiland ligt OP de plan-lus (punt 0), dus de punten 21..3 rond het eiland zijn eruit gelaten; de halte ligt aan
  de binnenkant van het eiland, naast het eind van de steiger `boot_dok_schildpad`. `dieren4.py` controleert elk punt van de
  spline (straal 6) tegen het eiland (schild + kop + vinnen), de steiger en ondiep water: **0 botsingen**.
- **Meerijden = passagiersstoelen (8), geen loopbaar schild.** Waarom: een bewegende botsbox laat spelers in multiplayer
  wegglijden/haperen (vanilla draagt staande spelers niet mee) en kan je in iets duwen; stoelen zijn robuust en simpel.
  Klik op hem (vanaf de steiger, of zwemmend) = opstappen; sluipen = afstappen.
- **Nooit vast, nooit verdrinken:**
  - de stoelen zitten 2,9-3,5 blok boven het water;
  - afstappen bij de halte = op de steiger; op zee = in het water naast hem (7,5 blok opzij, aan jouw kant), je kunt altijd
    weer op hem klikken;
  - een speler in het water binnen 12 blok met minder dan halve lucht wordt op zijn rug gezet ("Ouwe Dobber vist je uit het
    water"); is hij vol, dan krijgt die speler 20 s waterademhaling;
  - uitloggen op zijn rug = eerst op de steiger gezet (dan wordt je nooit opgeslagen "rijdend op" een schildpad die er niet is);
  - hij botst nergens tegen (geen collision, geen duwen), is onkwetsbaar.
- **Altijd precies een:** hij wordt nooit in een chunk opgeslagen (`shouldBeSaved` = false). `Z4DierenRegistry` maakt hem
  (elke seconde) zodra een speler binnen 192 blok van zijn plek is en die chunk tickt, en ruimt hem op als niemand er meer is.
  Een tweede exemplaar (bv. uit een speler-save na een crash) ruimt zichzelf op (`isOfficieel`).
  Hij moet wel serialiseerbaar zijn: vanilla laat je alleen op serialiseerbare entities rijden.
- Meldingen (actiebalk): halte/terug, vertrek, op, vol, af (steiger/water), gered. Geluid: schildpadgeluid bij aankomst/vertrek.
- Kielzog-spatjes achter hem tijdens het zwemmen (client).

### 2. De guh-vliegers (`GuhVlieger` + client-renderer)
- Een entity-type `guhs:vr_z4_vlieger`, geen AI, geen physics, doet niets in `tick`, nooit opgeslagen. De manager zet ze op
  de 17 plekken uit de kaart-kit (`bouwsels.VLIEGERS`: 14 op het Vliegereiland, de grote wedstrijdvlieger van 7 blok aan de
  wedstrijdpaal, 3 op het dak van de vliegerwinkel in Lampionhaven) als er een speler binnen 128 blok is, en haalt ze weg
  als niemand er is.
- Renderer zonder model: een ruit met oortjes (tint = vliegerkleur) + vaste laag (wit kruis, oogjes met glimlichtje, blosjes,
  mondje, roze oor-binnenkant), een touw met een beetje doorzakking naar een paaltje, een staart met 6 strikjes (2e kleur).
  Wind: dobberen, zwaaien, kantelen (28 graden naar achter) en rollen, allemaal client-side uit de speltijd + een fase per
  vlieger. De vlieger kijkt naar de vliegeraar (het anker).
- **LOD:** staart alleen binnen 64 blok, touw 10 stukjes dichtbij / 2 ver weg; tekenen tot 192 blok.
- **Fallback:** de kaart bouwt de wol-vliegers + vadstouw-lijn niet meer (`bouwsels.VLIEGER_BLOKKEN`). Terug naar de
  statische blokvliegers: `GUHS_Z4_VLIEGERBLOKKEN=1 python tools/vadsrijk/maak_kaart.py --zone 4` (en dan
  `/guhs vadsrijk z4dieren vliegers uit`, anders hangen ze dubbel).

### 3. Generator, dev, tests
- `tools/vadsrijk/zone4/dieren4.py` -> `zone4/dieren/Z4DierenPlekken.java` (GEGENEREERD, niet committen): VLIEGERS,
  DOBBER_ROUTE + DOBBER_HALTE, DOK_X/Y/Z (de steiger), LENGTE, CHECK_FOUTEN. Wordt aangeroepen door `maak4.maak` (dus door
  `maak_kaart.py --zone 4`), of los: `python tools/vadsrijk/zone4/dieren4.py` (~40 s; exit 1 bij botsingen).
- `tools/features/vadsrijk_t_z4dieren.py`: model, animaties, texturen, 10 teksten (nl + en).
- Dev: `/guhs vadsrijk z4dieren schildpad tp|rijd|route|halte`, `/guhs vadsrijk z4dieren vliegers aan|uit|tp`.
- Gametests (batch `vadsrijk_z4dieren`, `-Pgt=Z4Dieren`): `dobberRoute`, `dobberZitjes`, `vliegers`.

## Zelf gekozen (de gebruiker sliep)
- **Het statische eiland blijft** (hutje, Schelpie, knuffel `groot_4`, POI's, loopcheck: alles staat erop). Het blijft er
  ook als schildpad uitzien (kop + vinnen van blokken). Verhaal-voorstel: het eiland is **Oma Dobber**, die al heel lang
  slaapt (Schelpie woont op haar rug); **Ouwe Dobber** zwemt elke dag zijn rondjes om haar heen en neemt bezoekers mee.
  t-z4verhaal kan dat in een regel dialoog zetten; ik heb hun teksten niet aangeraakt.
- Meerijden met stoelen, niet lopen op het schild (zie boven).
- Ouwe Dobber is kleiner dan het eiland (14 vs 48 blok): een groter bewegend model is zwaar en past niet langs het eiland.
- Vliegerwinkel-dakvliegers zijn ook entities geworden (zelfde lijst). De muurvliegers van de werkplaats bestonden al niet.
- Bij de vliegers geen vasthoudende guh (de kaart heeft geen guhs): het touw eindigt aan een paaltje.

## Gedeelde bestanden (add-only, gemarkeerd `T-Z4DIEREN`)
- `wereld/WereldRegistry.java`: 1 regel `Z4DierenRegistry.register(modBus)`.
- `wereld/client/WereldClient.java`: 1 regel `Z4DierenClient.init(modBus)`.
- `tools/features/__init__.py`: 1 regel `vadsrijk_t_z4dieren` in FEATURES.
- `tools/vadsrijk/zone4/maak4.py`: 2 regels (dieren4 na de POI-snap) + docstring.
- `tools/vadsrijk/zone4/kit/bouwsels.py`: `VLIEGER_BLOKKEN` (env-schakelaar), VLIEGERS-tuple kreeg kleur2 erbij, en
  `vlieger_blokken` stopt na het registreren.
- lang nl_nl/en_us: 10 sleutels via mijn generator.

## Opgemerkt (niet van mij, niet aangepast)
- `tools/features/vadsrijk_t_z4wereld.py` roept `h.lang(k, nl, en)` aan, maar `make_v2.lang` is `(key, en, nl)`: bij een
  volledige make_v2-run worden hun Engelse en Nederlandse biome-/gebiedsnamen omgedraaid. (Mijn generator doet het goed.)

## Open punten
- Niet in een echte client bekeken (geen AutoCheck: RAM gedeeld met de andere nachtagents). Te checken door de gebruiker:
  hoe Ouwe Dobber eruitziet (oogjes zijn klein op de grote kop), of zittende spelers mooi op het schild staan, de vliegers
  in de wind. Stoelhoogtes staan in `OuweDobber.ZITJES`, de schaal in `Z4DierenClient.DobberRenderer`.
- Verhaal-haak: t-z4verhaal zou "Ouwe Dobber" bij de halte kunnen laten praten (bv. melding als je voor het eerst meerijdt);
  de entity heeft geen dialoog.
- Als iemand het statische eiland of de steiger verplaatst: `dieren4.py` opnieuw draaien (doet `maak_kaart --zone 4` vanzelf)
  en kijken of CHECK_FOUTEN 0 blijft.

## Hervatpunt
- Na een checkout: `python tools/vadsrijk/maak_kaart.py --zone 4` (maakt ook Z4DierenPlekken.java), dan compileert alles.
