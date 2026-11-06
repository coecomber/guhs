# SPIEK U-SPECS: specialisaties + nieuwe job-advancements + gear-pijltjes

Het nieuwe model (jouw besluit V2/V3):
- **Lv 1:** je eerste job meteen; de klasse = het wapen in je hand, vrij wisselen tussen de 3 klassen.
- **Lv 17:** advancement 1 = de **specialisatie**, bij Grootmeester Vadsvoet, vlak vóór de Zwamtempel.
- **Lv 35:** advancement 2, in zone 3, bij Ordemeester Wasja in de Nestkapel (na de Kaaszeppelinwerf en je Pluimguh).
- **Lv 58:** advancement 3, bij Kapitein Veerkracht in Hoogvads (na het slaaplied van Moeder Wolkwal).

| klasse | spec A (links in de boom) | spec B (rechts) | zijtak |
|---|---|---|---|
| Knuffelridder | Schildridder → Kristalkampioen → Vahoege Paladijn | Reuzenkussen → Stormkussen → Nestwachter | Tank |
| Pluisjager | Scherpschutter → Sterrenschutter → Kruinvalk | Strikkenzetter → Poolstervalk → Vahoege Strikmeester | Steun |
| Vadsmagiër | Sterrenwever → Kristalorakel → Schemerwijze | Zegenzanger → Koorvads → Vahoege Aartszanger | Heel |

Alle commando's werken in het Vadsrijk (eerst `/guhs vadsrijk tp ...` of via Knuffelkamp).

## 1. De keuze op lv 17 (zoals een speler het ziet)
```
/guhs vadsrijk klasse wissel knuffelridder
/guhs vadsrijk klasse klasselevel knuffelridder 17
/guhs vadsrijk spec scherm
```
- Je ziet **twee grote kaarten** (Schildridder en Reuzenkussen): stijl, en per advancement de titel + wat je krijgt.
- Klik **"Word Schildridder!"** (of Reuzenkussen):
  - de **ceremonie** speelt (camera, lichtstralen);
  - de titelkaart toont **"Schildridder"**;
  - er komt een ring in de kleur van je spec.
- Druk **K**: de skillboom heeft nu **4 kolommen**: Schildridder | Stam | Reuzenkussen | Tank.
  - De andere spec staat dof met een ×.
  - Hover over een kolomkop: uitleg van de tak.
- Onderaan de boom staan de advancement-badges met de **spec-titels** (hover = wat je krijgt).

Via de echte quest (één keer proberen):
```
/guhs vadsrijk npc tp grootmeester
```
- De quest **"De keuze van de Knuffelridder"** verschijnt na de Druipgrotten (hq09), vanaf lv 16.
- Inleveren op lv 17 opent het keuzescherm.
- Sluit je het scherm zonder te kiezen? Dan staat er in **K** een gouden knop **"Kies je specialisatie!"**.

## 2. Doorspeccen: stap 2 en 3
```
/guhs vadsrijk spec stap 2
/guhs vadsrijk spec stap 3
```
- Elke stap speelt de ceremonie met de nieuwe titel. Stap 2 = de nest-ceremonie (veertjes); stap 3 = de kristal-ceremonie.
- `/guhs vadsrijk spec` toont per klasse: level, spec, stap en titel.

## 3. Wat elke spec doet (even voelen op een oefenpop of klodder)
- **Schildridder:**
  - houd **Knuffelschild** op en laat je raken: er komen kritiek-vonkjes;
  - je volgende **Kussenklap** binnen 4 s doet +50 % (zwaai-effect + schildgeluid);
  - Kussenklap slaat 2× (Dubbelklap).
- **Reuzenkussen:**
  - gewone klappen raken ook vijanden rond je doel (zwaai + wolkje);
  - Kussenklap is zwaarder maar 1 s trager;
  - stap 2: Stormsprong verdooft.
- **Scherpschutter:**
  - pijlen buigen een beetje;
  - schoten van ver (>10 blokken) doen meer;
  - stap 2: Knuffelpijl +80 %.
- **Strikkenzetter:**
  - **Rolsalto** laat een groene ring (val) achter, en vijanden die erin lopen plakken;
  - midden in de salto 3 pijlen;
  - je schiet merkbaar sneller.
- **Sterrenwever:** de Vuurbal laat een plas achter; elke 3e spreuk is gratis.
- **Zegenzanger:**
  - in een gevecht elke 2 s een gele ring met muzieknootjes (liedpuls);
  - teamgenoten binnen 10 blokken doen 8 % meer;
  - stap 2: de Vadsbron doet schade.

## 4. Wisselen en resetten
```
/guhs vadsrijk klasse balie
```
- **"Skillboom resetten"** is altijd gratis (geen uur wachten meer). Je spec en advancements blijven.
- Knop **"✦ Specialisatie"** → het kaartenscherm → **"Wissel naar Reuzenkussen"**:
  - alleen de punten uit de oude spec-tak komen terug; stam, zijtak en stappen blijven;
  - meteen nóg een keer wisselen mag niet: de knop is grijs, met de tooltip "Je kunt weer wisselen over 10 min";
  - in een gevecht mag het niet:
    ```
    /guhs vadsrijk spec gevecht
    ```
    De knop is grijs: "Niet midden in een gevecht!". Typ het commando nog een keer om het gevecht weer uit te zetten.
- Zonder balie (gewoon K) is er geen wisselknop. Wisselen kan alleen bij de klassemeester:
  - **Zone 1-3:** Grootmeester, Ordemeester (Kandijkom), Wasja (Nestkapel).
  - **Zone 4/5:** een extra praat-optie bij de **Duinwachter** (Lampionhaven) en **Kapitein Veerkracht** (Hoogvads): "Skillboom, specialisatie en reset".
- Testen zonder regels: `/guhs vadsrijk spec wissel forceer`.

## 5. Oude save (migratie)
```
/guhs vadsrijk klasse wissel knuffelridder
/guhs vadsrijk klasse klasselevel knuffelridder 52
/guhs vadsrijk spec oud 4
/guhs vadsrijk spec migreer
```
- Na de migratie heb je **stap 2** (oud 4 → nieuw 2), al je boompunten zijn terug, en er staat een gouden regel "mag nog een specialisatie kiezen".
- In K staat de knop **"Kies je specialisatie!"**. Kies → de ceremonie toont de stap-2-titel van die spec.
- De omzetting: oud 2+ → nieuw 1, oud 3+ → nieuw 2. Alleen oud 1 telt pas als nieuw 1 als de klasse lv 17 is.
- Een echte oude save wordt vanzelf omgezet bij binnenkomen in het Vadsrijk, met een roze chatregel.

## 6. Gear-pijltjes (geen item level)
```
/guhs vadsrijk klasse wapen pluisboog
```
- Leg dat wapen in je tas en pak een ander wapen in je hand; of hover over een harnasstuk of sieraad in je inventaris.
- Onderaan de tooltip staat:
  - **"Vergeleken met je wapen:"**;
  - per stat **▲ groen** of **▼ rood**, en bij wapens ook "Schade per klap";
  - het oordeel: **▲ Beter**, **▼ Slechter** of **= Ongeveer even goed**.
- Het stuk dat je draagt: "(dit draag je nu)". Een leeg vak: "Je draagt nog niets als ..." → altijd Beter.
- Een wapen van een andere klasse dan in je hand: geen vergelijking (eerlijk vergelijken kan niet).

## 7. Tests
- `runGameTestServer -Pgt=VadsrijkSpecs` (boomindeling, advancement + spec, wisselregels, migratie, gear-oordeel)
- `-Pgt=VadsrijkKlassen` (punten, gratis reset, DPS: beide specs binnen 10 %)
- `-Pgt=VadsrijkZ2Systemen`, `-Pgt=VadsrijkZ3Systemen`, `-Pgt=VadsrijkNpc`
