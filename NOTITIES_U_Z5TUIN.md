# NOTITIES U-Z5TUIN - Hoogvads: tuinen in plaats van gras-ruis, meer licht in alle gebouwen

Ronde U (2026-11-06). Worktree `guhs_vadsrijk/u-z5tuin`, branch `110-u-z5tuin` (van 110-u-z5binnen 57fe33c52).

## Antwoord voor de gebruiker: welke renders zijn oud en welke nieuw?
- `renders-hoogvads/ronde3/voor_*` = de **OUDE** stand (van voor ronde U).
- `renders-hoogvads/ronde3/binnen2_*` = de **NIEUWE** interieurs (U-Z5BINNEN). `binnen2_voor_*` is de oude stand van diezelfde zalen.
- Deze slice: `tuin_voor_*` = oud (zonder tuinen en extra licht), `tuin_*` = nieuw.

## Feedback
1. Het plateau links en rechts van de Raadszaal/het paleis was een ruizig roze/groen lappendeken (`paleis_raadszaal.png`). Maak er vrolijke, ontworpen tuinen van, en doe dat bij ELK zulk grasvlak in Hoogvads.
2. Meer licht binnen in alle betreedbare gebouwen van zone 5.

## Waar kwam de ruis vandaan?
`kit/stad.grond` legt op de hele stadsgrond een hash-mix van `guhs:wolkenweide_gras` (roze-achtig) en `minecraft:grass_block` (groen): elk los blokje een andere kleur. Dat zat op ELK open grasvlak van de stad (een scan vond 191 vlakken, 36 groter dan 30 blokjes, samen ongeveer 18.700 grasblokjes).
De kit zelf is niet aangepast: een nieuwe pass erna maakt het goed.

## Tuinen (`tools/vadsrijk/zone5/tuin.py`, nieuw; aangeroepen aan het eind van `bouw5.bouw`)
1. **Eén rustig gazon:** elk open grasblok van de stad (bovenste gras met lucht erboven, binnen het stadsmasker) wordt `grass_block`.
2. **Elk grasvlak van minstens 40 blokjes en minstens 5 breed wordt een tuin:**
   - een hoofdpad (dirt_path) over de lange as, dat de vorm van het vlak volgt; 3 breed op brede vlakken;
   - zijpaden evenwijdig eraan om de 8 blokjes en dwarspaden om de 9, zodat er vakken van ongeveer 7x8 ontstaan; de paden lopen tot de rand van het vlak (aansluiting op de straten);
   - elk vak krijgt een thema, om de beurt:
     - **moestuin**: rijen tarwe, wortels, bieten en aardappels op akkerland, met om de 3 rijen een watergeul met waterlelies;
     - **pompoenen/meloenen**: steel (vast aan de vrucht) + pompoen of meloen, met een watergeul;
     - **bessen**: rijen zoetbessenstruiken met alliums ertussen;
     - **bloemenperk**: ringen van tulpen, alliums, margrieten, korenbloemen, lelietjes-van-dalen ... met hoge bloemen (rozenstruik, pioen, sering) in de tweede ring en een **vogelhuisje** (`guhs:vogelhuisje` op een paaltje) in het midden;
     - **rozentuin**: rozenstruiken en roze bloemblaadjes in een dambord;
     - **vijver**: water (1 diep) met waterlelies en guh-waterlelies (`guhs:guh_waterlelie`), met een rand van roze bloemblaadjes;
     - **fontein** (op het vak dat het dichtst bij het midden van een grote tuin ligt): een ronde bak (wit, rand 1 hoog, water 1 diep), een zuil met een gouden kapiteel en een **gouden kandelaar** erop, en een ring van bloemblaadjes eromheen;
   - moestuin-, pompoen- en bessenvakken krijgen een lage **haag** van bloeiende azalea, met openingen; de andere vakken een **bloemenrand** in 2 kleuren;
   - langs de paden: **guh-lantaarnpalen** (`guhs:guh_lantaarnpaal`, om de ~9 blokjes), **roze guh-bankjes** (`guhs:guh_bankje_roze`, kijkend naar het pad) met een **prullenbak** (`guhs:park_prullenbak`) ernaast;
   - over het hoofdpad van lange tuinen: een **pergola** (paaltjes van bleek eikenhout, een dak van bloeiende azalea op 3 blokjes hoogte).
3. **Smalle stroken (voortuintjes):** een rij bloemen (en rozenstruiken) langs de huismuur; de rest is gazon.

**Regels voor begaanbaarheid:**
- Niets vasts of nats (haag, paal, bankje, bakrand, pompoen, water) binnen 2 van een deur, trap, ladder, poortje of luik.
- Niets binnen 3 van een gepubliceerde plek (ook de verhaalplekken in de parken).
- Niets op een figurant.
- Water ligt altijd gelijk met de grond of 1 diep, met een stap eruit.
- De fontein wordt alleen gebouwd als de hele bak past (een gat in de rand zou het water laten weglopen); anders wordt het vak een bloemenperk.
- Gewassen, bloemen en bloemblaadjes hebben geen botsing; akkerland is begaanbaar.

## Meer licht binnen (`tools/vadsrijk/zone5/binnenlicht.py`, nieuw; aangeroepen na `tuin`)
**Elke kamer** wordt bij een startpunt gevuld (flood fill) op hoofd+1-hoogte (vloer + 3: daar loopt nooit iemand tegenaan). Alleen cellen met een plafond tellen mee, dus buiten stopt het vanzelf. Als startpunten dienen:
- de begane grond en de bovenverdieping van elk huis, elke winkel, de herberg en de bank;
- elke gepubliceerde plek onder een dak: het station, de academie, de vleugels van de universiteit, de werkplaatsen van het gilde, de soepkeuken, ...;
- een paar extra punten in kamers zonder eigen plek (collegezaalvleugel, kazerne, stationshal).

**Wat erin komt:**
- **Kroonluchters (S, `kroonluchter` van U-Z5BINNEN)** in het open midden, op ongeveer 5 blokjes afstand van elkaar, en niet binnen 3 van een lamp die er al hing. Bij een hoog plafond hangen ze aan een ketting. In een kamer die maar 3 hoog is, komt er een hangende lantaarn.
- **Gouden wandluchters** langs de muren, om de ~4 blokjes, alleen op een dichte muur (niet op glas, ruiten, deuren, tralies, bladeren of ladders), en niet boven een deur.

Beide alleen waar de twee cellen eronder lucht zijn met een gewone vloer eronder: nooit boven een trap, bed, plaat of water, dus nooit in de weg. Lichtblokken hebben geen botsing.
De zalen die U-Z5BINNEN al deed (kathedraal, troonzaal, Raadszaal, bibliotheek, kanselarij) en de marktkraampjes worden overgeslagen.

## Gedeelde bestanden (add-only, gemarkeerd U-Z5TUIN)
- `tools/vadsrijk/zone5/bouw5.py`: 2 aanroepen na `trap_hekken` (`tuin.bouw`, `binnenlicht.bouw`).
- `tools/vadsrijk/zone5/render5.py`: shots `t_paleis`, `t_tuin`, `t_woning`, `t_herberg` (groep `tuin`).
- Geen Java, geen nieuwe blokken: alleen bestaande Guhs-blokken en vanilla-blokken.

## Cijfers (regen)
- **Tuinen:** 18.743 grasblokjes egaal; 22 tuinen.
  - Vakken: 242 bloemen, 89 kleine randvakjes, 36 bessen, 33 rozen, 9 moestuin, 9 pompoen, 8 vijvers, 3 fonteinen.
  - Langs de paden: 202 lantaarnpalen, 199 bankjes met prullenbak, 5 pergola's.
  - 571 voortuinbloemen.
- **Binnenlicht:** 69 kamers, 385 wandluchters, 96 kroonluchters (S), 1 hanglamp.

## Checks
- Geen Java gewijzigd, dus er is geen gradle gedraaid en geen gametest nodig.
- **`maak_kaart --zone 5` (met de verbind-pass) + `loopcheck5.py`:**
  - nieuw: LEEG 523 / TRAP 668 / TRAP- 468 / **VAL 475**;
  - basis (dezelfde tak zonder de 2 aanroepen, zelf gemeten): LEEG 523 / TRAP 668 / TRAP- 468 / VAL 485;
  - dus geen regressie (10 VAL minder);
  - alleen de 4 bekende onbereikbare staartvin-plekken van t-z5wezens; alle andere plekken zijn bereikbaar.
- **Lessen uit de loopcheck (verwerkt):**
  - `lily_of_the_valley` telt in de gedeelde loopcheck als vast blok. Naast een balustrade werd dat een nep-opstapje naar het lagere terras onder het paleis; hij is vervangen door klaproos/margriet.
  - Niets vasts naast een muur, balustrade of trede van 1 (masker `vrijstaand`).
  - De watergeulen in moestuin- en pompoenvakken hebben open uiteinden (geen haag); anders zou de geul tussen de gewassen een kuil zijn.
  - Geen pad onder een lage tak of overhang: pad-blok met iets erboven = PAD-melding.
- De gegenereerde tegels, `Z5WereldPlekken.java` en `POI_Z5.md` zijn teruggezet (niet gecommit). **De integratie moet `maak_kaart --zone 5` draaien.**

## Hervatpunt
- **Regen:** `python tools/vadsrijk/maak_kaart.py --zone 5`, daarna `python tools/vadsrijk/zone5/loopcheck5.py`.
- **Renders:** `python tools/vadsrijk/zone5/render5.py ../../renders-hoogvads/ronde3 tuin_ tuin`. De voor-stand krijg je door de 2 regels in `bouw5.py` tijdelijk weg te halen (git stash).

## Open
- Het uiterlijk is alleen in renders bekeken; de gebruiker beoordeelt het in de dev client.
- De zoetbessenstruiken prikken als je erdoorheen loopt (vanilla). Ze staan binnen een haag, niet op een pad.
- Gewassen groeien en kunnen vertrapt worden (vanilla-gedrag); het akkerland blijft nat door de geulen.
