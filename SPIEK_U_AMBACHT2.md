# SPIEK u-ambacht2: de Ambachten groeien mee met zone 4 en 5

Alles in het Vadsrijk, met op-rechten. Het Ambachtenboek open je met **Y**.

## 1. Het plafond is nu 60
```
/guhs vadsrijk ambacht level alle 60
/guhs vadsrijk ambacht boek
```
- Elke tegel zegt "Niveau 60 / 60" en rechtsboven paars **Meester**.
- In de chat: "Je bent nu Meester in ..." (9 keer) en +20 Knutselpenningen per ambacht.
- Open de Kledingkast (**O**), tab capes: de **Meesterschort** staat erin.
- Titels: "Meester-kok" enz. en "Grootmeester der Ambachten".

## 2. Fix: zone 4 in het Plekken-overzicht
```
/guhs vadsrijk ambacht boek plekken
```
- Zes blokjes: zone 1-3 boven, zone 4-5 onder.
- Zone 4 heeft een regel "Vissen: 0/7" (5 visplekken + 2 duikplekken).
- Onderaan staan nu 15 vissen en 7 honingsoorten.
- Ook in het Verkenningsboek (**B**), pagina Ambachten: "Plekken zone 4" en "Plekken zone 5".

## 3. Zone 4 verzamelen (T4, niveau 50)
```
/guhs vadsrijk ambacht level alle 50
/guhs vadsrijk ambacht tp z4_pluk_bloemeneiland
/guhs vadsrijk ambacht plekken
```
- Je ziet varentjes (zeekraal en zeegaren). Rechtsklik en pluk.
- Probeer ook deze plekken:
  - `tp z4_hout_vliegereiland`: bamboeblokjes (drijfhout en vliegerbamboe);
  - `tp z4_mijn_pierkop`: goudklompjes (lampionbrons);
  - `tp z4_vis_haven`: waterlelies, vissen. 's Nachts maanrog, bij regen regenboogmakreel.
- Op niveau 49 zijn de plekken vaag en krijg je "niveau 50 nodig".

## 4. Koraalduiken (zone 4)
```
/guhs vadsrijk ambacht tp z4_duik_koraalbaai
```
- Spring in het water. Op de bodem (2-8 blokken diep) staan roze hersenkoraaltjes.
- Rechtsklik: "Duiken! Houd vast en laat los in het groen".
- Je krijgt roze koraal of een zeesponsje, soms een parelschelp. Het telt NIET als vis.
- Het water is nooit dieper dan 8 blokken, dus je komt altijd boven.

## 5. Zone 5 verzamelen (T5, niveau 55)
```
/guhs vadsrijk ambacht level alle 55
/guhs vadsrijk ambacht tp z5_pluk_staartpark
```
- Blauwe bloempjes: wolkbes en wolkenwol.
- Probeer ook deze plekken:
  - `tp z5_hout_staartpark_1`: wolkenhout;
  - `tp z5_mijn_noordvin`: witte calcietblokjes (hemelzilver) op de vin van Moeder Wolkwal;
  - `tp z5_vis_fontein`: vissen in de fontein op de Marktlaan (wolkenvis).

## 6. Bijen in zone 4 en 5
```
/guhs vadsrijk ambacht level imkeren 19
/guhs vadsrijk ambacht tp z4_korf_bloemeneiland
```
- Klik op een korfplaats: "niveau 20 nodig".
- `level imkeren 20`, nog eens klikken: je korf staat (lampionhoning).
- Zet de raten vol met `/guhs vadsrijk ambacht vul` en klik weer: oranje lampionhoning.
- Zone 5 (`tp z5_korf_staartpark`) heeft Imkeren 35 nodig. Op 55 mag je 5 korven.

## 7. T4/T5 maken: alleen in het Ambachtsgilde van Hoogvads
```
/guhs vadsrijk ambacht level alle 60
/guhs vadsrijk ambacht geef tier 5
/guhs vadsrijk ambacht geef tier 4
```
- Ga naar een smid in Zwamzoet (`/guhs vadsrijk ambacht tpnpc amb_smid_z1`) → Werkplaats.
  - T4/T5-recepten staan erbij, met een rode regel "Alleen in het Ambachtsgilde van Hoogvads".
  - Proberen te maken geeft dezelfde melding.
- In Hoogvads, in het Gildehuis: Leermeester Smeden → Werkplaats. Maak een "Hemelzilveren Zwaard".
  - Het is Episch, niveau 56 en je kiest 2 bonussen.
  - Een Wolkkern-Meesterring (Meesterwerk) kan hier ook.
- De werkplaats-guh in Lampionhaven (rol "werkplaats") maakt T4 wel, T5 niet.
- Nieuw eten:
  - zone 4: Zeekraalsalade, Koraalvissoep, Zeemanshapje, Lampionspiesjes;
  - zone 5: Wolkbessentaart, Donderstoofpot, Hemelfeestmaal.
- Nieuwe drankjes: Zee-/Wolken-Vadsdrank en -Energiedrank, en het Hemelelixer.
- Vliegerbouw: Naaien → "Guh-vlieger (naar huis)". Hij komt met de Post thuis.
- Snel testen zonder lopen: `/guhs vadsrijk ambacht werk smeden`. Dat werkt overal; het is de dev-werkplaats.

## 8. Het verhaal gaat verder in het Gilde (stap 6 en 7)
```
/guhs vadsrijk ambacht verhaal koken 5
/guhs vadsrijk ambacht level koken 55
```
- Praat met de Leermeester Koken in het Gildehuis → "Vertel eens..." → Oma Truus' bladzijde uit de Koraalbaai.
- Daarna `level koken 60`, nog eens "Vertel eens..." → het koekje met wolkbessen.
- De Gildemeester vertelt de verhalen van Mijnen, Houthakken, Plukken en Vissen.
- De oude leermeester in Zwamzoet zegt na stap 5: "Ga naar het Ambachtsgilde in Hoogvads".
- Snel: `/guhs vadsrijk ambacht gilde vissen`. Dat zet stap 5 klaar en vertelt de Gilde-stap meteen; wel eerst het level goed zetten.

## 9. Knutselpenningen en de Knutselwinkel
```
/guhs vadsrijk ambacht penningen 100
/guhs vadsrijk ambacht winkel
```
- Tab "Knutselwinkel":
  - kernsplinter, glinstergruis, oerbast, gouden klaver, parelschelp, koninginnengelei;
  - Uitrustthee (+30 min uitgerust);
  - de 5 bazenkernen en de Ambachtskist;
  - deco naar huis.
- Koop iets: de penningen gaan omlaag en het spul komt in je tas (materialen in de materiaaltas, deco met de Post).
- Penningen verdien je zo: een bestelling op het Bestelbord (+2 tot +6), de weekkist (+10), een spelersopdracht (+3), een verhaalstap (+5), Meester worden (+20).

## 10. Het Opdrachtenbord (spelers vragen, makers leveren)
```
/guhs vadsrijk ambacht opdracht
```
- Rechts: "Nieuwe opdracht".
  - Kies met < > een recept (eten, drankje, Maatwerk, bazenkern), het aantal met - + en de beloning met - +.
  - Klik "Ophangen": je munten gaan meteen in de pot.
- Links staat je opdracht roze, met "Intrekken". Klik "Intrekken": je krijgt precies je munten terug.
- Leveren alleen testen:
  ```
  /guhs vadsrijk ambacht opdracht nep klodderpudding
  /guhs vadsrijk ambacht geef tier 1
  /guhs vadsrijk ambacht werk koken
  ```
  - Maak in de keuken 1x Klodderpudding (geeft er 2). Open daarna weer `/guhs vadsrijk ambacht opdracht`.
  - "Lever" wordt roze zodra je er 2 hebt. Klik: je krijgt 500 brons + 3 penningen en de puddingen gaan weg.
- Met twee spelers:
  - A hangt een opdracht op, B levert.
  - A krijgt een chatbericht en klikt "Ophalen (1 klaar)".
- Ook via de bordguhs: Klerk Krabbel / Kriebel / Kraaltje, en nieuw Klerk Kwastje (Lampionhaven) en Klerk Kroontje (Hoogvads, bij het Gildehuis). Kies "Opdrachtenbord" of "Knutselwinkel".

## 11. Meesterschort zelf zien
```
/guhs vadsrijk ambacht reset
/guhs vadsrijk ambacht meester smeden
```
- Een echte level-up naar 60: paarse chatregel, geluidje, +20 penningen.
- Kledingkast → capes → Meesterschort: roze schortje met gouden rand, een zakje, een hamertje en een naald.

## Kijk vooral naar
- Zien de nieuwe plekken er goed uit (bamboe, varen, calciet, koraal) en staan ze nergens raar (niet op paden, niet in huizen)?
- Past het grotere Ambachtenboek op je scherm? Passen alle 7 tabs?
- Staan de nieuwe bordguhs op een fijne plek in Lampionhaven en bij het Gildehuis?
