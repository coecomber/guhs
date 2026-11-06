# NOTITIES U-Z5 - zone 5 Hoogvads: diepte, interieurs, figuranten

Ronde U (2026-11-06, feedback: "heel vlak en saai, moet veel meer diepte krijgen"). Worktree `guhs_vadsrijk/u-z5`, branch `110-u-z5`
(van eaee06d08 = 110-t-int), 110-u-hekken erin gemerged (fast-forward, de centrale verbind-pass).

## Wat er gebouwd is

### Gotiek met diepte (`tools/vadsrijk/zone5/kit/gotiek.py`, nieuw)
Bouwstenen in de kit-stijl (palet-sleutels, alleen in lucht tenzij een functie zelf uitsnijdt):
- `Vlak`: een gevelvlak (as, muurcoördinaat, naar buiten); `p(u, d)` met d = 0 de muur, d > 0 buiten.
- `pinakel` (muurtjes-schacht, gouden hogels om en om, knop, end-rod-staafje);
- `hogels`: gouden hogels langs de ribben van een spits;
- `slanke_spits`;
- `steunbeer`: getrapt, met schuine kappen, een gouden band per sprong, een nis met beeld, een waterspuwer en een pinakel;
- `nis`: met een guh-heilige, console en baldakijn;
- `gargouille`: een guh-waterspuwer met oortjes;
- `omlijsting`: terugspringende archivolten naar buiten, colonnetten, kapiteeltjes, een vensterbank en een druiplijst;
- `maaswerk`: middenstijl, Y-tracering en drieblad in het glas;
- `portaal`: lagen spitsbogen die naar binnen kleiner worden, colonnetten op elke sprong, pinakels, en een optionele wimperg met hogels;
- `borstwering`;
- `kroonluchter`:
  - de hoofdketting loopt tot het plafond, en de ringkettingen naar het plafond of naar de ring erboven;
  - 2 tot 4 ringen van gouden platen met kaarsen en hangende lantaarns, en kristal (end rods) aan de onderste ring;
- `kandelaar`;
- `kruisribgewelf`: twee spitstongewelven die elkaar snijden, met gouden graatribben, gordelbogen en een scheitelrib; daarboven een rug;
- `bundelpijler`: diensten met gouden kapitelen.

### Ordekathedraal (`kit/kathedraal3.py`, nieuw; aangeroepen vanuit `kit/kathedraal.py` [U-Z5])
Het grondplan, de maten en alles waar bouw5 op rekent zijn gebleven: de poort, het plein, de torentrap en de klokkenvloer.

**Westgevel:**
- 4 getrapte steunberen (4 diep, met fundering tot het plein);
- blinde zijportalen in de torenvoeten;
- een koningsgalerij (6 nissen per toren);
- omlijste en vertraceerde lancetten;
- omlijste galmgaten;
- colonnetten met beelden in de Grote Poort;
- het guh-roosvenster met 2 ringen (de oortjes blijven vrij);
- hogels op de topgevel;
- grote hoekpinakels, een kroon van 8 spitsjes en hogels op de torenspitsen.

**Flanken:**
- de zijbeukramen hebben een omlijsting, maaswerk en een wimperg;
- de lichtbeukramen hebben een omlijsting en maaswerk;
- borstweringen;
- pilasters tussen de lichtbeukramen;
- de steunberen hebben een waterspuwer, een nis en hogels;
- een tweede (lagere) luchtboog en maaswerkmuurtjes op de bovenste.

**Transept:**
- een diep portaal (3 lagen);
- een roos-omlijsting;
- 2 achthoekige traptorentjes met spitsen per gevel;
- omlijste ramen.

**Koor:** pinakels op de straalsteunberen.

**Dak:** een nokkam, dakkapellen en 4 spitsjes rond de koepel.

**Binnen:**
- kruisribgewelven in het schip, de zijbeuken en de transeptarmen;
- de kruising is open tot in de koepel (een gouden ring waar die begint);
- bundelpijlers met diensten die tot de gewelfaanzet doorlopen;
- een triforium-arcade met een gouden lijst;
- kroonluchters:
  - 2 grote in het schip (r 5,6, 3 ringen) en 1 achter de kruising;
  - een reuzenkroonluchter in de koepel (r 8,5, 4 ringen);
  - kleine in de zijbeuken;
- kandelaars bij het altaar.

`KA.luchter` is nu de nieuwe kroonluchter, dus de troonzaal krijgt die vanzelf.

### De rest van de stad (`tools/vadsrijk/zone5/hoog3.py`, nieuw; aangeroepen in `bouw5.bouw` na de huizen [U-Z5])

**Universiteit:**
- getrapte steunberen met pinakels op de dakrand;
- in elke travee een hoog gotisch raam van 2 verdiepingen (goud kader, omlijsting, maaswerk) in plaats van de kleine bovenramen;
- gotische dakkapellen (wimpergjes met een glas-in-lood-raampje);
- lijsten op de verdiepingsbanden;
- hogels op de hoektorenspitsen;
- het poortgebouw: een diep portaal (de trap loopt erdoor) en 2 hangende hoektorentjes met spitsen.

**Bibliotheek:**
- galerijen op y0+6 langs beide boekenwanden, met een reling en lantaarns;
- een ladder aan de westkant (nieuwe plek `universiteit_bibliotheek_galerij`);
- kroonluchters.

**Paleis:** pinakels op de balustrade, waterspuwers, lijsten onder de banden, hogels op de 4 ronde torens en pinakels op de vleugeldaken.

**Raadszaal:**
- wandpilasters met gouden kapitelen;
- een goudraster (cassetten) onder het plafond;
- kroonluchters (`bouw5.KA_luchter` = de nieuwe kroonluchter).

**Academie:** mezekouwen (een uitkragende borstwering op consoles) op alle buitenmuren en de donjon, en pinakels op de donjon.

**Station:** hangende steunberen tussen de bogen (pas vanaf hoofdhoogte, dus de looprichting blijft vrij), met pinakels, en roos-omlijstingen.

**Vensterkappen (hele stad, ook de huizen):**
- elk raam dat naar buiten kijkt krijgt een druiplijst: een omgekeerde witte trap, 3 breed waar de muur doorloopt;
- een raam dat hoog hangt krijgt een vensterbank;
- dit levert ongeveer 8.000 druiplijsten en 1.400 vensterbanken op.

### Figuranten-bug (Java)
**Oorzaak:**
- `Z3BazenClient.FigurantRenderer` tekende het guh-model met **al zijn botten**: zadel, 3 harnassen, alle variant-botten (nek, teckel, ender, koning, ...) en alle ~350 kledingstukken. Dat werd een stapel kleren.
- `GuhRenderer` verbergt die botten per variant en per gedragen slot. De prop deed dat niet.
- Verder kregen de npc_*-skins (512px, voor de rechtopstaande `guh_npc_*`-modellen) het guh-model, kreeg de uiltje-skin (een vogel) het guh-model, en hetzelfde gold voor het konijntje.

**De fix:**
- `FigurantRenderer.adjustModelBonesForRender` doet bij model "guh" hetzelfde als een gewone guh: zadel en harnassen weg, en de variant-masker (`GuhBoneMasks.hidden(variant, geen kleren)`). De variant komt uit de textuur (`GuhVariant.texture()`). Dit fixt ook de zone-3-ruiters (Z3Leven).
- `Z5Bewoners.vorm(b)` zet elke skin op het juiste model:
  - npc_* gaat naar `guh_npc_*` met de animaties van `guh_sitting`;
  - konijntje gaat naar `guh_konijntje`;
  - uiltje wordt een sneeuw-guh;
  - al het andere is guh-vacht op het guh-model.
- Gametest `Z5BewonersGameTests` (batch `vadsrijk_z5bewoners`) controleert dat model, textuur en animaties bestaan voor elke soort.

### Render-kit
- `tools/vadsrijk/zone5/render5.py` (nieuw): rendert de SPELbouw (maak5.bouw_wereld) met de ronde-2-kit. `kit/opname.py` en `kit/prender.py` zijn gekopieerd uit `renders-hoogvads/_bron`.
- Renders staan in `renders-hoogvads/ronde3/`. De `voor_*`-renders zijn gemaakt met een git-archive van eaee06d08 in de scratchpad.

## Gedeelde bestanden (add-only, gemarkeerd `U-Z5`)
- `feature/vadsrijk/zone3/bazen/client/Z3BazenClient.java`: `FigurantRenderer` verbergt de guh-botten (zie boven).
- `feature/vadsrijk/zone5/wereld/Z5Bewoners.java`: `vorm()` en `maak()`.
- `tools/vadsrijk/zone5/bouw5.py`:
  - `KA_luchter` is nu een kroonluchter;
  - `hoog3.bouw`-aanroep;
  - `trap_hekken` slaat gouden hogels en dakpannen over (anders sloopte die regel de muurtjes van pinakels en pilasters boven een hogel of dakpan).
- `tools/vadsrijk/zone5/kit/kathedraal.py`: `luchter` is nu een kroonluchter, de oude nave-luchters zijn weg, en `K3.buiten`/`K3.binnen` worden aangeroepen.

## Zelf gekozen
- **Ombouwen in plaats van herbouwen:** het grondplan van kathedraal en universiteit is gebleven (POI's, trappen, torentrap, deuren, kamers van bouw5). De diepte komt van ervoor geplakte lagen en uitsnijdingen.
- **Geen nieuwe hv_-blokken:**
  - de kroonluchter is gemaakt van ketting, gouden platen, kaarsen, lantaarns en end rods;
  - het maaswerk is gemaakt van wolkmarmer-muurtjes.
  - Een gouden muurtje (kroonlijst_muur) zou mooi zijn voor gouden maaswerk en pinakels, maar is niet gedaan.
- **Huizen:** alleen vensterkappen en vensterbanken (de huizen hadden al overkraging, erkers en luifels).
- **De Raadszaal blijft een tempel** (die zuilengang heeft al diepte). Alleen het interieur is groter gemaakt.

## Checks
- `compileJava` is groen.
- `runGameTestServer -Pgt=Z5BewonersGameTests,Z5WereldGameTests,WolkwalGameTests`: **All 12 required tests passed**.
- `maak_kaart --zone 5` + `loopcheck5.py`:
  - Alle gepubliceerde plekken zijn bereikbaar, behalve 4 plekken op de staartvin van t-z5wezens: `z5w_donderbuik_arena/_deur`, `z5w_leider_mistmoeder` en `z5w_staartvlakte_noord`.
  - Die 4 zijn niet door deze slice veroorzaakt: de staart is niet aangeraakt, en ze zijn ook onbereikbaar zonder de verbind-pass.
  - Dat is iets voor de integratie of t-z5wezens: ze liggen op y 206 op de staartlobben, de staartlift landt op y 212.
- Bij de eerste regen waren ook deze plekken onbereikbaar; ze zijn hersteld:
  - `academie_toren`: een pinakel stond in de laddertoren;
  - `universiteit_sterrenwacht_top`: een dakkapel stak in de wenteltrap;
  - 5 `*_boven`: vensterkappen stonden in de huiskamers boven de trap; nu krijgen de huizen alleen buitenom kappen;
  - `wolkwacht_3`: een colonnet stond op de plek van de wacht in de poort.
- LEEG 523 / TRAP 668 / TRAP- 468 / VAL 485:
  - TRAP zijn sier-trappen (kapjes, kroonlijsten, hogels);
  - VAL is de walvisrand onder de stad (Wolkvangnet).
- `verbind.py` liep mee op de 56 zone-5-tegels.
- De gegenereerde tegels, `Z5WereldPlekken.java` en `POI_Z5.md` zijn teruggezet (niet gecommit). **De integratie moet `maak_kaart --zone 5` draaien.** Pas dan bestaan de nieuwe plek `universiteit_bibliotheek_galerij` en de nieuwe bouw in de tegels.

## Hervatpunt
- **Regen:**
  - `python tools/vadsrijk/maak_kaart.py --zone 5` (nu met de verbind-pass);
  - daarna `python tools/vadsrijk/zone5/loopcheck5.py` en `python tools/vadsrijk/audit/verbindcheck.py`.
- **Renders:** `python tools/vadsrijk/zone5/render5.py <map> <prefix> alles [snel|groot]`.

## Open
- Het uiterlijk is alleen in renders bekeken. De gebruiker beoordeelt het in de client.
- Van binnen is het gewelf in de renders donker (het licht komt alleen door de ramen); in het spel geven de kaarsen en lantaarns licht.
