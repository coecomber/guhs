# NOTITIES X-Z1GROND (ronde X, 2026-11-09): zone 1 (+ Knuffeleiland) en zone 2 op echte funderingen

Branch `110-x-z1grond` vanaf c927d87af (110-x-kaart). Alleen bronnen gecommit; tegels en BEREIK_RAPPORT.md zijn lokaal
geregenereerd om te verifiëren en daarna teruggezet. **Integratie regenereert zone 1 en zone 2**
(`maak_kaart.py --zone 1`, `--zone 2`). Spiekbrief: `SPIEK_X_Z1GROND.md`.

## 1. Checker: hoogte-gebaseerd voor zone 1 en 2 (`tools/vadsrijk/funderingcheck_hoogte.py`, nieuw)
`funderingcheck.py --zone 1|2` gebruikt automatisch de nieuwe check zodra `_cache/z1|z2/terrein.npz` bestaat (maak_kaart/maak2
schrijven die; los: `python tools/vadsrijk/wereld/fundering1.py` of `zone2/fundering2.py`). Zone 6 blijft op de x-kaart-check.

De naam-gebaseerde check werkte in zone 1 niet: tempelmuren van `roze_knuffelsteen` heetten "grond", `grass` ving ook
`grass_block`, paddenstoelblokken telden als plant, en hoeden/hoedenpaden/uitkijkplatforms/bruggen zweven allemaal bewust.
Nieuw, zonder namenlijsten voor "bewust zwevend":
- Per kolom: vanaf de echte grondhoogte T van de generator omhoog door de vaste basis, eerste open cel s, eerste vaste cel b
  erboven; gat = b - s.
- **GAT/ZWEEFT telt alleen als hetzelfde bouwsel er vlak naast wél op de grond staat**: een kolom binnen 5 die vast is op b en
  b-1 met zijn grond op b-1 (±1). Dus een muur/vloer/steel/stam-rij die van de grond over een kuil doorloopt. Een hoed, een
  boomtoppad, een uitkijkplatform, een brugdek of een latei boven een deur raakt op zijn eigen onderkant nergens de grond en telt
  dus niet.
- Niet: boven water; gegraven onder de grond; holle binnenkant (alleen lucht die met de lucht erboven verbonden is telt: de tegels
  zijn uitgehold); een doorgang van 2+ boven een gebouwde vloer of een pad (`pad`-masker uit de Kaart); een natuurlijke overhang
  of grotmond; de rand van een paddenstoelhoed (wol/hoedblokken); de BEWUST-gebieden (zone 1: fossielbrug, Noordklifpier;
  zone 2: het gebouw op palen op de zuidhelling).
- GAT = gat 1..6, ZWEEFT = 7..16. LEK zoals x-kaart. BROK = losse component (≤ 64 blokken, alles behalve lucht/water, dus
  bladeren en planten tellen mee) die nergens iets raakt; niet: wolken/vlaggen/hoeddots (alleen wol-/hoedblokken), slingers
  tussen palen, componenten aan het water, BEWUST-gebieden. `minecraft:light` telt als lucht.
- Zone 1 = alleen de zone-1-tegelgrid (tx -11..11, tz -11..18). Het oude vak (-20..15, -16..20) telde zone 2 mee.

## 2. De pass (`tools/vadsrijk/wereld/fundering1.py`, nieuw; zone 2: `zone2/fundering2.py`)
maak_kaart (zone 1) en maak2 (zone 2) schrijven de grondhoogte-cache vóór de tegels en draaien `naloop()` direct na de tegels
(vóór overgang/speeltuin/verbind/vrijpad). Zelfde detectie als de checker (`funderingcheck_hoogte.vind`). Idempotent.
- **Fundering:** onder een gewoon vol blok (muur, steel, stam, rots) gaat hetzelfde blok omlaag tot de grond (tot 16 diep);
  onder iets anders (trap, plaat, hek, glas, deur) komt het grondblok van de kolom (tot 6).
- **Losse brokjes:** een paal (hek/muur/stam) die boven iets vasts hangt gaat omlaag tot dat vaste blok; een lamp (lantaarn,
  ketting, shroomlight) 1..4 onder een plafond krijgt een ketting omhoog; een los plantje/tapijtje/druipsteen in de lucht gaat weg;
  een los blok 1..3 boven de grond krijgt grond eronder.
- **Lek:** lucht naast of onder water mét iets vasts of water er direct boven (holte onder een vijverrand, holle grond onder de zee)
  wordt dichtgezet met het bodemblok. Onzichtbaar; dat zijn er veel (zone 1: 9286, zone 2: 5708 cellen), voorkomt dat water
  bij een blokupdate in holtes wegloopt.
- Debug: `GUHS_GEEN_FUNDERING=1` bouwt zonder de pass.
- Uitkomst regen: zone 1: 400 kolommen / 660 blokken fundering, 1 paal, 16 lampen aan een ketting, 5 plantjes weg;
  zone 2: 474 kolommen / 1356 blokken, 3 palen, 2 lampen, 3 blokjes onderstut.

## 3. Generatorfixes zone 1 (`wereld/bouwsels.py`, gemarkeerd X-Z1GROND)
- `naald()` (de 5 uitkijknaalden): de rotsvoet begon op g-3 van het midden en hing op een helling aan de lage kant tot 6-10 in
  de lucht (Druipspits). De voet loopt nu door tot de echte grond onder elke voetkolom.
- Sporenwoud-waterval (`geheim_waterval`): het watergordijn hing los in de lucht (open bovenkant, lucht erachter en ernaast).
  Nu: rotswand erachter boven de ingang, bemoste wangen aan beide kanten, een rotslip erboven. De ingang (3 hoog) en de kamer
  blijven vrij. De voorkant van het gordijn is open (dat is een waterval): de checker blijft daar LEK 57 melden.
- `schildpad()`: boom en kist stonden 1-2 boven het schild. De boom begint nu op het schild; onder de kist komt gras
  (kist blijft op 524,76,722: POI `geheim_schildpad` en bereik_doelen ongewijzigd).

## Cijfers (cellen / plekken; "vóór" = de x-kaart-tegels met dezelfde nieuwe checker)
| zone | | GAT | ZWEEFT | LEK | BROK |
|---|---|---|---|---|---|
| 1 | vóór | 407 / 198 | 9 / 7 | 57 / 1 | 111 / 27 |
| 1 | na | **13 / 5** | **0 / 0** | 57 / 1 (waterval-voorkant, bewust) | **41 / 4** |
| 2 | vóór | 366 / 127 | 37 / 8 | 0 | 326 / 120 |
| 2 | na | **27 / 11** | **2 / 1** | 0 | 295 / 110 |

(Ter vergelijking, de oude naam-check zone 1: GAT 1604, ZWEEFT 4958, LEK 340, BROK 677: dat waren vooral hoeden, platforms,
uitgeholde grond en zone-2-tegels.)

## Checks na regen van zone 1 + 2
- funderingcheck: zie tabel.
- zweefcheck: zone 1 43 → 43, zone 2 20 → 20 (niet slechter).
- loopcheck zone 1 (box -704,-704,767,1215): PAD 39, TRAP 215, TRAP- 88, VAL 2 → **identiek**. De 2 VAL's bestonden al
  (vuurtorenkelder: ladder + luik, checker ziet het luik niet als klimbaar; 83,95,27 een 1-cel kuiltje in Zwamzoet).
- loopcheck zone 2 (box 384,-1472,1023,-833): TRAP 46, VAL 32 → 33. De nieuwe VAL komt NIET van deze pass (ook met
  `GUHS_GEEN_FUNDERING=1` aanwezig) maar van x-kaarts Galmtop-fix, zie open punten.
- verbindcheck 0 (los 0, fout 0, as 0, gat 0); schepen_check 23 instappunten, 0 fout.
- bereikcheck --strikt: exit 0; zone 1 hoofd 24/24, zij 40/40, npc 52/52, geheim 61/61; zone 2 hoofd/zij/npc 100 %,
  geheim 63/63 (was 62/63).
- Geen gametests/client gedraaid (alleen Python). taalcheck/engelscheck niet geraakt (geen taal).

## Gedeelde bestanden
- `tools/vadsrijk/funderingcheck.py`: `laad_terrein(zone)`, zone-1-vak = zone-1-grid, `HOOGTE_ZONES = ("1", "2")` → nieuwe check.
- `tools/vadsrijk/maak_kaart.py`: cache + `fundering1.naloop` in de zone-1-bouw (5 regels).
- `tools/vadsrijk/zone2/maak2.py`: cache + `fundering2.naloop` (5 regels).
- `tools/vadsrijk/wereld/bouwsels.py`: `naald()`, sporenwoud-waterval, `schildpad()`.
- Nieuw: `funderingcheck_hoogte.py`, `wereld/fundering1.py`, `zone2/fundering2.py`.

## Open punten
- **Galmtop (zone 2, x-kaart):** door de nieuwe starthoek van `_naald_start` komt de bovenste trede onder de plankenvloer van het
  platform uit, en de reling-opening (`ea`) valt niet op de aankomst. loopcheck: VAL 930,195,-1108 (platform 98 cellen, uitweg
  alleen 1,5 omhoog over de reling). Fix hoort in `bouw2.ijsnaald()`: de laatste treden buiten de plankcilinder houden of de
  opening op de echte hoek van de bovenste trede zetten. Niet door mij aangeraakt (bereikcheck zegt alles bereikbaar).
- Zwamtempel-terras (zone 1, 300..315, -394..-376): het westelijke deel hangt tot 14 boven de bergflank, maar het bergpad loopt
  eronder door (doorgang): niet gevuld, wordt ook niet geteld.
- Zone 1 rest: 13 GAT-cellen (gat 1) bij stenen hekpaaltjes in Knuffelkamp (-386,75,1121); BROK: twee roze vlaggen op het
  tempeldak (322/338,194,-392), een lampje bij de Reuzenhoed (-5,178,-22), een froglight in de kristalkamer.
- Zone 2 rest: BROK 110 plekken, vooral losse punten van kristalnaalden (amethist, glas, kristalblok 1 cel naast een scheve kegel)
  en grote ballonnen van rode wol: bewust of cosmetisch, niet aangeraakt.
- Paden met 1-bloksprongen: loopcheck TRAP/TRAP- ongewijzigd en niet nagelopen (meest decoratieve trappen).
- Niet in een client bekeken.
