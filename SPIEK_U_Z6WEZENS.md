## Zone 6 "Het Zijdeland": wezens, veldbazen, kerker, buit en de Bultguh (U-Z6WEZENS)

Zone 6 is lv 60-70: lief en zoet, alleen grauw bij de toren. De meeste wezens zijn ontvadste woestijndieren met een grauw korstje "sterrenzout": die **vrolijk je op** (roze plof, daarna huppelen ze blij rond). De Mika's (Lampendovers, Ringdragers) vluchten in een rookwolk. Constructen (Zandvlooien, Spiegelslang, Zandgeestje, de Zoutslang) vallen uiteen in glinsterend zand.

> **Let op: alle plekken zijn nog PLACEHOLDERS** tot u-z6wereld de kaart van zone 6 bouwt (dan pakt alles vanzelf de echte POI).
> Zonder kaart staan de veldbazen misschien in de lucht of in zee. Typ dan eerst `/guhs vadsrijk z6wezens bouw arenas` (legt zandvloeren onder de twee veldbaas-arena's; ze verdwijnen weer bij een herstart).
> De kerker bouwt zichzelf (eigen kamers onder het Ruïnedal) zodra je erheen gaat.

### 0. Snel overzicht
- `/guhs vadsrijk z6wezens alle` zet alle 14 wezens op een rij (zonder AI). Sla ze om de afloop te zien.
  - Kijk naar: het zandvlooitje met voelsprietjes, de fennekguh met gigantische oren, de springpiep op zijn springstaart, de roze schorpioen met een strikje op zijn staart, het dadelaapje met een dadel, de gierguh met een pluizige kraag, de glazen spiegelslang, de zoutkreeft met zoutkristallen op zijn rug, het stofwolkje, de stenen ruïnewachter met een gouden hoedje, de twee Mika's (met reuzendover / met lichtpotten), het gedoofde sterretje en de grote roze Zoutslang.
- `/guhs vadsrijk z6wezens plekken`: alle plek-ids met coördinaten (+ of ze echt of placeholder zijn).
- `/guhs vadsrijk z6wezens spawners`: hoeveel wezens waar (max ~70 in heel zone 6).
- `/guhs vadsrijk z6wezens opruimen`: haalt alle zone-6-wezens binnen 64 blokken weg.
- `/guhs vadsrijk z6wezens spawn <soort> [level] [elite]`: één wezen voor je neus.

### 1. De Roze Duinen (lv 60-62)
- `/guhs vadsrijk z6wezens tp duinen`
- **Zandvlooi**: zit verstopt in het zand (je ziet alleen twee voelsprietjes, geen naambordje). Loop binnen 5 blokken: ze springen eruit! Daarna springen ze om de ~3 s naar je toe.
- **Fennekguh**: hoort je al van 16 blokken en roept zijn maatje. Oren omhoog + **rode streep** = zijn sprong: stap opzij. Zeldzaam huisdier: **Fennekje**.
- **Springpiep**: **rode cirkel** = zandschop (1,5 s donker als je erin staat), daarna stuitert hij weg en weer terug. Zeldzaam huisdier: **Springpiepje**.
- **Duinkruiper** (roze schorpioen): **rode kegel** voor hem = staartprik + Plakkerig.
- Leider **Oude Oorschelp** (gouden gloed).

### 2. De Palmenkloven (lv 62-64)
- `/guhs vadsrijk z6wezens tp palmenkloven`
- **Dadelaapje**: gooit dadels (rode cirkel) en **pikt soms munten** ("Het dadelaapje pikt 12 brons!"). Vrolijk hem op: je krijgt alles terug.
- **Gierguh**: cirkelt hoog en duikt op wie het **laagst** staat (rode streep). Daarna zit hij 2 s op de grond.
- Leider **Dadelbaas Pitje**.

### 3. De Zoutvlakte (lv 64-66)
- `/guhs vadsrijk z6wezens tp zoutvlakte`
- **Spiegelslang**: maakt **twee kopieën** (luchtspiegeling). Maar één is echt; een kopie spat uit elkaar bij de eerste tik ("Een luchtspiegeling!") en doet nooit pijn. Testen: `/guhs vadsrijk z6wezens mechaniek spiegel`.
- **Zoutkreeft**: zoutkorst = van voren maar 35% schade ("Zoutkorst!"), van **achteren** 150% ("Zwakke plek!"). Loop om hem heen.
- **'s Nachts**: `/guhs vadsrijk z6wezens tp zoutnacht` (of `/time set night`). **Zandgeestjes**: paarse cirkel = hij zuigt je naar zich toe (duizelig). Laat **Sterrenstof** vallen. Overdag zijn ze er niet.
- Leider **Kapitein Korstje**.

### 4. Het Ruïnedal en De Moederoase (lv 66-70)
- `/guhs vadsrijk z6wezens tp ruinedal` en `tp moederoase`
- **Ruïnewachter**: staat stil als een standbeeld (geen naambordje). Binnen 6 blokken: "De wachter wordt wakker!". Grote rode cirkel = stamp.
- **Mika-Lampendover** (ook bij de bakens): rode kegel = reuzendover: donker + je **Vahoeg en Snelpootjes gaan uit**.
- **Mika-Ringdrager**: paarse cirkel = hij opent een lichtvangpot: een paarse straal zuigt Vads. **Loop uit de cirkel** om de straal te breken. Testen: `mechaniek ringpot`.
- **Gedoofd sterretje** (bij de oase): drie rode cirkeltjes = sterrengruis. Zeldzaam huisdier: **Sterretje**.
- Leiders **De Oudste Wachter** (Ruïnedal) en **Ringdrager Brie** (oase).

### 5. Veldbaas: Koningin Wormelien (lv 63, De Roze Duinen, overdag)
- `/guhs vadsrijk z6wezens baas wormelien` (werkt ook 's nachts; zonder kaart eerst `bouw arenas`).
- Een reusachtige roze zandworm met een guh-snoet en wimpers, grauw en hongerig.
- **Onderduiken**: ze verdwijnt in het zand (onraakbaar) en een rimpel volgt iemand. **Blijf bewegen**: wie stilstaat krijgt een zandfontein. Dan komt ze omhoog onder haar doelwit (rode cirkel). Testen: `mechaniek duik`.
- **Zandgolf**: rode baan, plakkerig.
- **Fase 2 (60%) Duinspuug**: ze slokt zand (castbalk "Duinspuug"), er verschijnen drie **roze plekken**: daar ben je veilig. Wie erbuiten staat wordt bedolven (35%). Testen: `mechaniek duinspuug`.
- **Fase 3 (30%) Helemaal boven!**: een **grauwe zoutkorst** op haar rug. Zolang die er zit is ze onraakbaar. Spring er vanaf een duin tegenaan of schiet hem eraf. Dan is ze 6 s beduusd en krijgt ze +50%. Testen: `mechaniek korst`.
- Aan het eind: ze wordt roze en valt in slaap in het zand (hartjes).
- Buit: **Karavaanwacht**-stuk (episch), Wormelientand, Duinzwemmerslaarzen, heel zeldzaam de Wimperwaaier (Guhddelijk), trofee, 2 Gouden Veren bij de eerste keer. Huisdier **Wormelientje**. Voor alle rijdieren: de **Wormelien-zadeldeken** (tuig, open je rijdierscherm N > Aankleden).

### 6. Veldbaas: De Zoutkolos (lv 66, De Zoutvlakte, alleen 's nachts)
- `/guhs vadsrijk z6wezens baas zoutkolos` (werkt ook overdag).
- Een reus van samengekoekt sterrenzout. Hij valt uiteen in zout, geen opvrolijken.
- **Zoutgolven**: rode barstlijnen vanaf zijn voeten (castbalk). `mechaniek zoutgolf`.
- **Fase 2 (60%) Zingende kristallen**: hij gooit 3 roze kristallen. Om de 3 s klinkt een **hoge noot**: een paarse ring rond elk kristal. Stap eruit, of sla het kristal stuk. `mechaniek kristallen` / `mechaniek zing`.
- **Fase 3 (30%) Gloeiend hart**: **kijk recht naar zijn hart** terwijl je slaat: dubbele schade ("Hart geraakt!"). Met de Sterrenlens in je hand x2,5.
- Buit: Karavaanwacht-stuk, Zingend Zoutzwaard, Zoutsplinterboog, Zoutkristalkroon, trofee. Huisdier **Zoutklontje**.

### 7. De kerker: De Verzonken Sterrenwacht (lv 66-68)
- Ingang: het luik in de oude koepel in het Ruïnedal (rechtsklik). Snel: `/guhs vadsrijk z6wezens tp sterrenwacht` (bij het luik) of meteen `tp kerker` (binnen).
- De kamers: **Ingangshal** (Lampendovers + een Ruïnewachter) > gang > **Ringenzaal** > gang > **Kaartenzaal** (sterrenkaarten, Lampendovers, een Ringdrager, een sterretje) > gang > **Vuurschalenzaal** > gang > de **Koepel** met een sterrenhemel. Terug naar boven: de ladder bij de ingang of achter in de Koepel (rechtsklik).
  - Kijk naar: vlakke vloeren, gangen van 5 breed, overal licht, geen gaten.
- **Baas 1: De Astrolabiumwachter** (`baas astrolabiumwachter`): een ster met draaiende messing ringen.
  - Je klappen komen maar voor 25% door ("De ringen kaatsen je klap af!"), behalve als de ringen **in één lijn** staan: **gouden licht**, "NU!", 5 s volle schade (elke ~11 s). `mechaniek lijn`.
  - **Ringzwaai**: rode banden rond hem met **roze openingen**: sta in een opening. Elke 25% een ring erbij (max 4). `mechaniek ringzwaai`, `fase 4`.
- **Baas 2: Kootje Glimmertje** (`baas glimmertje`): Mika-ploegbaas met een reuzendover.
  - Vier **vuurschalen** in de hoeken. Om de ~15 s dooft hij er een (paarse streep). **Rechtsklik een donkere schaal 2x** om hem weer aan te steken.
  - 2 uit = donker; 3 uit = zijn telegraphs worden grijs; **4 uit = onraakbaar**. `mechaniek doof` dooft ze allemaal.
  - Op 50% fluit hij: twee schalen uit + twee Lampendovers. Hij vlucht in rook.
- **Baas 3: De Verzonken Sterrenguh** (`baas sterrenguh`): een oude reuzen-guh van sterrensteen, grauw geworden.
  - Vier **telescopen** rond de Koepel. **Rechtsklik** = een kwartslag draaien. Een gouden straal = deze staat goed. Alle vier goed: **zijn hart gloeit**, 10 s dubbele schade. Daarna een sterrenschok (rode ring, blijf dichtbij) en de telescopen staan weer scheef. Zonder hart: 35%. `mechaniek hart`.
  - Sterrengruis (rode cirkels), vanaf fase 2 een wanhoopsgolf (rode ring vanaf hem: kom dichtbij).
  - Aan het eind gloeit hij weer, en je krijgt **Het Sterrenkompas** (voor het verhaal, hq08). Huisdier **Sterrenguhtje**. 1 Gouden Veer.
- Buit van alle drie: een stuk van de **Sterrenwachtset** (episch) + benoemde items.

### 8. De grot-elite: De Zoutslang (lv 66, De Zingende Zoutgrot)
- `/guhs vadsrijk z6wezens tp zoutgrot` (placeholder) of `spawn zoutslang`.
- Ze zwemt door de rots en komt af op **geluid**: rennen en slaan. Is ze onder? Dan kun je haar niet raken. **Rode streep** = daar komt ze uit. `mechaniek slangduik`.

### 9. Buit (lv 60-70)
- `/guhs vadsrijk z6wezens set duinwandelaar` (ridder/jager), `set duinzijde` (magiër), `set karavaanwacht`, `set sterrenwacht`.
  - De wanten/handschoenen, cape en armband draag je in je **sieradenvakken**; ze tellen mee voor de set.
  - Set-regels in de tooltip, groen als je ze hebt. Bijvoorbeeld Duinwandelaar: (2) Verdediging +14 en Vads +80, (4) Schade tegen bazen +8%, (6) Loopsnelheid +8%. Sterrenwacht (6): elke 20 s doet je volgende klap +40% ("Sterretje!").
- `/guhs vadsrijk z6wezens sieraden`: Saffraanring en **Woestijnroosring** (allebei ook in je **tweede ringvak**), Rozenketting, Sterrenoorbel, Zoutkristalarmband.
- `/guhs vadsrijk z6wezens wapens`: per **specialisatie** een wapen, de tooltip zegt "Gemaakt voor: ..." (Rozenkromzwaard = Schildridder, Dadelpalmknots = Reuzenkussen, Duinvalkboog = Scherpschutter, Karavaankruisboog = Strikkenzetter, Saffraanstaf = Sterrenwever, Oasefluit = Zegenzanger). Vergelijk met je eigen wapen: de pijltjes in de tooltip.
- `/guhs vadsrijk z6wezens kristallen`: Rozenkwarts (Kritiek) en Sterrenzout (Schade tegen bazen).
- `/guhs vadsrijk z6wezens items`: alle 10 benoemde baasitems + 3 trofeeën + het Sterrenkompas. Elk heeft een "Effect:"-regel.
- `/guhs vadsrijk z6wezens kist <baas>`: een buitkist van die baas.
- `/guhs vadsrijk z6wezens huisdieren`: alle 8 zone-6-huisdieren (druk **P**): Fennekje, Springpiepje, Sterretje, Dadelaapje, Bobbeltje, Wormelientje, Zoutklontje, Sterrenguhtje.
- `/guhs vadsrijk z6wezens sterrenstof`: 16 Sterrenstof (materiaal).

### 10. De Bultguh (rijdier, 2 zitplaatsen)
- `/guhs vadsrijk z6wezens bultguh` (of `bultguh saffraan|wolkwit|kaneel|sterrennacht`): je krijgt hem en zit er meteen op.
  - Kijk naar: twee zachte bulten met het zadel ertussen, een lange nek, guh-oortjes, **kwastjes** langs de zadeldeken en **belletjes** om de nek die meewiegen.
  - **Op zand loopt hij 30% sneller.**
- **Tandem**: laat een **tweede speler** de Bultguh rechtsklikken terwijl jij erop zit: die zit achterop ("... zit achterop!"). Alleen jij stuurt. Stap jij af, dan draaft de Bultguh naar huis.
- Rijdierscherm (**N**): de Bultguh staat erbij, met vachten (Zandroze, Saffraan, Wolkwit, Kaneel, Sterrennacht), patronen en tuig.
- In het echte spel krijg je hem in de stalquest van Oma Dadel in Rozenkloof (verhaal-slice u-z6verhaal).

### 11. Muziek
- `/guhs vadsrijk z6wezens muziek`: welk bazenthema elke baas wil, en wat hij nu speelt (zolang de nieuwe thema's niet gemaakt zijn, speelt er een bestaand thema).
