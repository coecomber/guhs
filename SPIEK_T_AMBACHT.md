# Spiekbrief: Ambachten (t-ambacht)

Alles hieronder werkt in het Vadsrijk. Commando's beginnen met `/guhs vadsrijk ambacht` (op 2). Toets **Y** = het Ambachtenboek
(ook als tabblad naast Rijdieren / Huisdieren / Verkenningsboek).

## 0. Snel alles klaarzetten
```
/guhs vadsrijk ambacht level alle 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht boek
```
- **Kijk:** 9 tegels (Mijnen, Houthakken, Plukken, Vissen, Imkeren, Smeden, Naaien, Koken, Brouwen), elk "Niveau 50 / 50" met een xp-balk.
- Klik op een tegel: je springt naar de recepten van dat ambacht.
- Terugzetten: `/guhs vadsrijk ambacht reset`.

## 1. Verzamelen (Mijnen, Houthakken, Plukken, Vissen)
```
/guhs vadsrijk ambacht reset
/guhs vadsrijk ambacht tp z1_pluk_kamp
```
- **Kijk:** rondom zie je kleine plantjes (azalea-bosjes) staan. Alleen JIJ ziet ze (een tweede speler ziet zijn eigen plekken).
- Kijk ernaar: onder je kruisje staat "Plukken · tier 1 (rechtsklik)".
- **Rechtsklik** = het spelletje (2-4 s):
  - Plukken: houd rechtsklik vast, laat los in het groene stuk.
  - Mijnen: drie keer tikken op het roze.
  - Houthakken: twee keer snel tikken.
  - Vissen: wacht op "BEET!" en klik meteen.
- Perfect = "extra vadsig", goed = "vadsig", niks doen = na 4 s gewoon. **Sluip + rechtsklik** = meteen gewoon oogsten.
- **Daarna:**
  - de plek verdwijnt voor jou en komt na 1-2 min ergens anders terug;
  - boven de hotbar "+30 Plukken-xp";
  - het materiaal zit in je tas.
- Andere zones / ambachten:
```
/guhs vadsrijk ambacht tp z1_mijn_grot_1
/guhs vadsrijk ambacht tp z1_hout_bos_1
/guhs vadsrijk ambacht tp z1_vis_vijver
/guhs vadsrijk ambacht tp z2_mijn_kloof_1
/guhs vadsrijk ambacht tp z3_vis_moeras
```
- **Te laag niveau** (bijv. zone 2 met Mijnen 1): de plek is half doorzichtig, met "niveau 20 nodig".
- Snel testen:
  - lijst van plekken in de buurt: `/guhs vadsrijk ambacht plekken`;
  - alles meteen weer vol: `/guhs vadsrijk ambacht vul`;
  - de dichtste plek oogsten zonder spelletje: `/guhs vadsrijk ambacht oogst`.
- **Zeldzame plek:** `/guhs vadsrijk ambacht zeldzaam`.
  - De dichtste plek wordt goudkleurig en glinstert, met "Glinsterplek: driedubbele oogst!".
  - Oogsten geeft 3x, en een kans op Glinstergruis, Oerbast, Gouden Klaver of Parelschelp.
- **Vissen:** 's nachts en bij regen bijten andere soorten. Een nieuwe soort geeft een chatregel "Nieuwe vis gevangen".
- **Anti-AFK:** blijf >10 min op dezelfde plek oogsten en je krijgt de helft (met een melding).
- **Uitgerust:** `/guhs vadsrijk ambacht uitgerust 600`.
  - Links onder staat "Uitgerust: 10 min dubbele ambachts-xp".
  - Oogsten geeft dubbele xp ("uitgerust: dubbel!").
  - Offline bouwt dit vanzelf op: 1 s per 6 s weg, max 2 uur.

## 2. Imkeren (zone 3)
```
/guhs vadsrijk ambacht tp z3_korf_bloesemdal
```
- **Kijk:** bijenkorfjes (half doorzichtig) bij de bloemen: "Lege korfplaats: klik om je korf te zetten".
- **Klik:** je korf staat (een hint-kaartje verschijnt) en de korf wordt een echte bijenkorf.
- Gele stipjes (bijen) vliegen tussen de korf en de bloemen.
- Raten vullen in echte tijd: 1 per ~10 min, sneller met meer bloemen, max 8. Snel vullen: `/guhs vadsrijk ambacht vul`.
- **Oogsten** = rook geven: houd vast en laat los in het groen.
  - Te vroeg = "chagrijnige bijen": je plakt even. Nooit schade.
  - Je krijgt honing per raat (Bloesemhoning / Schemerhoning / Wortelhoning, één per wei) + bijenwas + soms Koninginnengelei.
- Meer korven: 1 op niveau 1, 2 op 15, 3 op 30, 4 op 45.

## 3. Werkplaatsen in de steden (leermeester-guhs)
```
/guhs vadsrijk ambacht npcs
/guhs vadsrijk ambacht tpnpc amb_kok_z1
```
- De guhs staan in een ring rond het plein: Zwamzoet (8 + bordguh), Kandijkom (4 + bord), Honingtak (4 + bord + Imkerguh Zoemie bij de Raatweide).
  - Ze verschijnen pas als je in de buurt van het plein komt (zoeken zelf een vrij plekje).
  - Staat er een raar? Dan zoek je opnieuw met `/guhs vadsrijk ambacht scan` (en loop weg en terug).
- **Klik op een guh:** een praatscherm met de keuzes Werkplaats / Receptenboek / Vertel eens... / Dag!
  - Verzamelmeesters (Bonk, Spaan, Madelief, Dobbertje, Zoemie) hebben "Waar vind ik plekken?": uitleg + de dichtste plek met x/z.
- Zonder lopen: `/guhs vadsrijk ambacht werk koken` (of `smeden` / `naaien` / `brouwen`).

## 4. Koken (eten met buffs)
```
/guhs vadsrijk ambacht level koken 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht werk koken
```
- Links de recepten (grijs = nog te laag), rechts wat je nodig hebt (groen = genoeg).
- **Maak:** een balkje met een roze stuk; druk "Nu!" als het streepje in het roze staat.
  - Perfect = hogere Vadsigheid.
  - De tooltip toont "Vadsigheid ▮▮▮▮▮▮▮▮▯▯ 84%" en "Gemaakt door <jij>".
- **Maak 10x:** zonder spelletje, altijd 60%.
- **Eet** (rechtsklik):
  - links onder staat "Eten: Klodderpudding (29m 59s)";
  - in het Verkenningsboek van je stats zie je Vads +60 en Kracht +3 (x1,0 tot x1,3 door de Vadsigheid);
  - één buff tegelijk, 30 min, en hij blijft bij flauwvallen.
- **Feestmaal:** iedereen in je party binnen 10 blokken krijgt de buff + "Gezellig" (Kritiek +1, Heal-kracht/Elementschade/Elementweerstand +2%).
- Snel: `/guhs vadsrijk ambacht eten ridderontbijt` (dit geeft ook +5% gevechts-xp).

## 5. Brouwen (drankjes + kristallen slijpen)
```
/guhs vadsrijk ambacht werk brouwen
```
- Gebrouwen drankjes: dezelfde getallen als in de winkel, maar tot +30% door de Vadsigheid. Ze zitten in dezelfde 10 s drankjes-cooldown.
- **Kristal slijpen (geslepen):** kies rechts een kristalsoort (het getal = hoeveel ruwe je hebt). Drie ruwe worden één geslepen.
- **Stralend:** drie geslepen + 1 Glinstergruis worden één stralend (Brouwen 40).
- **Honingelixer:** Vads + energie, en het wast Plakkerig en Gesporen af.

## 6. Smeden + Naaien: Maatwerk en Meesterwerk (fase B)
```
/guhs vadsrijk ambacht level smeden 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht werk smeden
```
- **Maatwerk:** kies bijv. "Amberkoperen Zwaard".
  - Kies 2 bonussen (de knopjes met de stat-namen; groen streepje = gekozen) en druk Maak.
  - Je krijgt een **Episch** stuk met jouw 2 bonussen + willekeurige, en minstens 1 sokje.
  - De tooltip toont "Maatwerk", "Zelf gekozen: ...", "Maatwerk telt niet voor een setbonus" en "Gemaakt door".
  - Met 100% Vadsigheid zitten je gekozen bonussen bovenaan hun bereik, en de naam wordt "Vadsig ...".
- **Naaien** maakt de broek + schoentjes, Smeden het zwaard / de boog / de staf / de helm / de borstplaat.
- **Meesterwerk-sieraden** (BiS) hebben een bazenkern nodig (zit in `geef alles`).
  - Kies 3 bonussen + 1 eigenschap, bijv. "Schade tegen bazen +8%".
  - Je krijgt een **Legendarisch**, gebonden sieraad. Draag het met rechtsklik.
- **Bazenkernen:**
  - elke baas 15% per helper (Zwamkern zone 1, Splinterkern zone 2, Wortelkern zone 3);
  - elites 3% een Kernsplinter;
  - 5 splinters = 1 kern in de smidse.
- Snel een stuk: `/guhs vadsrijk ambacht maatwerk zwaard 3` / `/guhs vadsrijk ambacht maatwerk ring 3`.

## 7. Bestelbord (fase B)
```
/guhs vadsrijk ambacht bestel
```
- 3 bestellingen van stadsguhs ("Bakker Bram wil 10 Pluisijzer.").
  - Ze passen bij jouw niveaus: met alles op 1 krijg je beginnersbestellingen.
  - Elke dag om 06:00 nieuwe.
- **Leveren** (als het balkje groen is) geeft ambachts-xp + munten.
- **Ruilen:** 1 gratis per dag.
- Na 12 bestellingen in een week: de **Ambachtskist** (rechtsklik: kernsplinters, iets zeldzaams, 5 zilver).
- Snel testen:
  - een bestelling klaarzetten (inclusief de spullen): `/guhs vadsrijk ambacht bestel klaar 0`;
  - de volgende levering geeft de kist: `/guhs vadsrijk ambacht bestel week`;
  - nieuwe set: `/guhs vadsrijk ambacht bestel nieuw`;
  - kist direct: `/guhs vadsrijk ambacht kist`.

## 8. Ranglijst (Ambachtsbord) + website (fase C)
- Bij de bordguh (Klerk Krabbel / Kriebel / Kraaltje) of het boek, tab "Ranglijst":
  - de top 10 per ambacht + "Totaal";
  - onderaan "Jij staat op plek X van de Y". Nooit meldingen in de chat.
- **Website-export:** `/guhs vadsrijk ambacht export`.
  - Dit schrijft `guhs-web/live/hiscores/ambacht_<ambacht>.json` + `ambacht_totaal.json` + `ambachten_recepten.json` (het pad staat in de chat).
  - Het gebeurt ook vanzelf elke 5 min en bij uitloggen.

## 9. Verhaaltjes + titels + spullen naar huis (fase C)
- Klik een leermeester → "Vertel eens...".
  - Per ambacht 5 stapjes, op niveau 1 / 10 / 20 / 35 / 50 (bijv. Koken: Oma Truus' receptenboek, Smeden: het Aambeeld van de Orde).
  - Elke stap geeft munten en een beetje xp.
  - De laatste stap stuurt een aandenken met de **Post** naar huis.
- Snel: `/guhs vadsrijk ambacht level koken 50` en `/guhs vadsrijk ambacht verhaal koken 4`, dan "Vertel eens...".
- Zonder de guh te zoeken: `/guhs vadsrijk ambacht npc amb_kok_z1`.
- **Naar huis (alleen versiering):**
  - Naaien maakt kokschort, vissershoedje, tuinhoedje, imkershoed, vaandel en klodderreus-knuffel;
  - Smeden maakt mijnhelmpje, Opperzwambeeld en Mini Koning Klodder;
  - alles gaat met de Post: buiten het Vadsrijk komt het in je gewone tas.
- **Titels:** Leerling-<ambacht> op niveau 25, Gezel-<ambacht> op 50, en "Ambachtskenner" voor 100% van het Ambachtsboek.
- **Verkenningsboek (B):** een eigen pagina "Ambachten" met de gevonden plekken per zone, vissen, honing en recepten. Telt NIET mee voor de zonepercentages.

## 10. Markt en winkel
- Materialen stapelen tot 64 (alleen dezelfde kwaliteit stapelt). Op de markt zet je dus een hele stapel tegelijk te koop.
- De winkelguhs kopen materialen, gerechten en drankjes nu ook:
  - T1 3 brons per stuk, T2 8, T3 15, zeldzaam 60;
  - vadsig x1,5, extra vadsig x2.
