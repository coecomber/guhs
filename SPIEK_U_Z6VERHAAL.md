# Spiek: zone 6 "Het Zijdeland", het verhaal (u-z6verhaal)

Alles in het Vadsrijk, in survival of creative. Zonder de wereld van zone 6 (u-z6wereld) zweven de NPC's op hun plek en zijn de
plekken lucht; met de wereld staan ze op de echte vloer. De commando's staan onder `/guhs vadsrijk z6verhaal ...`.

## 1. Levelcap 70
```
/guhs vadsrijk level 70
```
- Je HUD toont level 70 en de balk is vol (geen "volgend level").
- Level 61-69 kosten steeds iets meer xp (de curve loopt gewoon door).

## 2. De overgang: Moeder Wolkwal zwemt naar het oosten
Vanaf het begin (zone 5 nog niet af):
```
/guhs vadsrijk z5verhaal quest 10
/guhs vadsrijk z5verhaal stap
/guhs vadsrijk z5verhaal stap
/guhs vadsrijk z5verhaal stap
```
- Lever "Sterren in het oosten" in bij Havenmeester Windvaan (`/guhs vadsrijk z5verhaal tp havenmeester`).
- Windvaan zegt "Hou je vast!" en zodra de praatbox dicht is (binnen ~5 s) speelt de film **"De grote zwemtocht"**: de walvis
  bromt, zwemt naar het oosten, de roze kliffen, een wolkje "Tot hier".
- Daarna: lijn E is open en de quest **"Land in zicht!"** start vanzelf. Hoogvads blijft gewoon op zijn plek staan.

Alleen de film nog eens bekijken:
```
/guhs vadsrijk z6verhaal scene wolkwaltocht
```

## 3. Snel klaarzetten voor zone 6
```
/guhs vadsrijk z6verhaal klaarzetten
```
- Zone 1-5 klaar, level 60, lijn E open, "Land in zicht!" loopt.
- Naar Rozenkloof vliegen (de eerste keer de aankomstfilm):
```
/guhs vadsrijk z6verhaal vlieg
```
- Of via lijn E: bij de Staarthaven van Hoogvads (achteraan op de staart) een schip pakken. Bij de pier moet "Jij mag mee" staan.
- Op de reiskaart (bij een havenmeester) staat nu **Het Zijdeland**. Vóór de zwemtocht is het een grijs "?".

## 4. De hoofdlijn "De Sterrenkaravaan" (10 stappen)
Elke stap overslaan / direct naar een stap:
```
/guhs vadsrijk z6verhaal quest 1
/guhs vadsrijk z6verhaal quest 5
/guhs vadsrijk z6verhaal stap
```
(`quest N` zet alles vóór stap N klaar, ook de bakens; `stap` vinkt het eerstvolgende doel af.)

Wat je moet zien, per stap (de tracker wijst steeds de plek aan):
1. **Land in zicht!** Havenmeester Zandloper, dan de wandtrappen af naar Kloofmoeder Roosje in de Schatkamer.
2. **De karavaan van Oma Dadel**: drie klusjes op het Rozenplein (dadels, waterzakken, tapijten), dan bij **Stalmeester Hobbel** je
   eigen **bultguh** (zit de bultguh nog niet in het spel, dan krijg je een grijs berichtje en komt hij later vanzelf).
3. **Door de Roze Duinen**: de Siq omhoog, rustplek, Bobbeltje achter een duin, Koningin Wormelien in de verte, de Westpoort, Oma Dadel
   in de Karavanserai.
4. **Audiëntie bij Sultane Rozijntje**: Kaneelstok, de Sultane met de sterrenkaart, dan bovenin de Sterrenwacht de **eerste
   sterrenpuzzel** (Het Knuffeltje). Daarna een visioen: Moeder Bron fluistert. Je krijgt de **Sterrenlens** (rechtsklik!).
5. **Het Palmbaken**: Dadelpit, de watervallen op, sterrenpuzzel "De Dadelpalm" -> het baken vlamt op + visioen 1.
6. **Een tapijt van sterrenzijde**: 5 cocons (witte glinsters), Knoopje weeft het **vliegende tapijt**, proefvlucht naar de Saffraanbrug.
7. **De Zoutvlakte bij nacht**: karavaankamp, Zoutbaken (puzzel), de **Zoutkolos** wordt wakker (film), de Grauwe brief in het Mika-kamp.
8. **De Verzonken Sterrenwacht**: Professor Stof, de kerker (zonder kerker in de tree: een korte film die het Sterrenkompas geeft),
   het Ruïnebaken.
9. **De Spiegelzaal**: kompas op de loper -> **ontmaskering**: Meester Glans is **de Spiegelvizier**, een Mika-illusionist en de Knokkel van Ringeling. Achtervolging
   over drie daken, hij vlucht naar de toren.
10. **De Moederoase**: Oma Dadel en de grote karavaan, Duinbaken, de luchtspiegeling, Oasebaken, **Moeder Bron opvrolijken** (klik op
    haar), de voet van de toren: **Ringeling lacht van boven en verzegelt de poort**. Inleveren bij Moeder Bron: titelkaart
    **"Ridder van het Zijdeland"**. Het verhaal is af; de raid is open.

## 5. De sterrenbakens en puzzels los testen
```
/guhs vadsrijk z6verhaal puzzel palm
/guhs vadsrijk z6verhaal puzzel zout
/guhs vadsrijk z6verhaal puzzel examen_1
/guhs vadsrijk z6verhaal puzzel lied
/guhs vadsrijk z6verhaal puzzel afdingen_kardemom
/guhs vadsrijk z6verhaal baken duin
/guhs vadsrijk z6verhaal scene visioen
```
- Kies expres eerst een verkeerde ster: een lief berichtje, en je begint opnieuw. Fout kost niks.
- Overdag: de lens laat even de nachthemel zien (scherm wordt donker met sterren).
- `baken <id>` steekt een baken meteen aan: lichtstraal, sterrenscherfje, visioen (elke keer sterker).
- Rechtsklik de **Sterrenlens**: "x van de 5 bakens branden" + het volgende baken.

## 6. Alle films
```
/guhs vadsrijk z6verhaal scene aankomst
/guhs vadsrijk z6verhaal scene zoutkolos
/guhs vadsrijk z6verhaal scene kerker
/guhs vadsrijk z6verhaal scene ontmasker
/guhs vadsrijk z6verhaal scene ontsnapt
/guhs vadsrijk z6verhaal scene luchtspiegeling
/guhs vadsrijk z6verhaal scene bron
/guhs vadsrijk z6verhaal scene toren
/guhs vadsrijk z6verhaal scene brief
/guhs vadsrijk z6verhaal scene klaar
```

## 7. Na de raid: Ringeling wordt opgevrolijkt + flash F4
```
/guhs vadsrijk z6verhaal ringeling
```
- Film "Opgevrolijkt!": haar tien ringen rollen weg, ze giechelt, wordt roze en geeft je **de ring van de Keizer**.
- Daarna **F4**: Krabbel leest haar briefje voor aan de Keizer ("Ik stop. Ik heb sterren gezien."), de grauwe vinger tikt, en het Oog
  draait naar Hoogvads. Dan het slot: de karavanen komen terug.
- Titel **"Vriend van Ringeling"**.
- Ga naar de **Gesluierde Dame** in de Paradijstuin van Saffraanpoort:
```
/guhs vadsrijk z6verhaal tp ringeling
```
  Vóór de raid: "Ken ik jou? Nog niet, schat." Erna praat ze als Ringeling, jouw bondgenoot.

## 8. NPC's en plekken
```
/guhs vadsrijk z6verhaal tp zandloper
/guhs vadsrijk z6verhaal tp dadel
/guhs vadsrijk z6verhaal tp hobbel
/guhs vadsrijk z6verhaal tp rozijntje
/guhs vadsrijk z6verhaal tp twinkel
/guhs vadsrijk z6verhaal tp knoopje
/guhs vadsrijk z6verhaal tp bron
/guhs vadsrijk z6verhaal tp kk_poort_saffraanpoort
/guhs vadsrijk z6verhaal tp bond_bord_rozenkloof
```
- In beide steden: een **bankier** ("Naar mijn kluis"), een **Kwartiermeester** (Knuffelkwartier) en een **Bondenbord** (Knuffelbonden).
  Zonder die onderdelen in de build zeggen ze "binnenkort".
- In de bazaar: Kardemom (winkel), Pistache (veilinghuis), Lokum (juwelier), Koperkop (smid) en de leermeesters smeden/naaien; koken in
  de Karavanserai, brouwen onder de Sterrenwacht, imkeren in de Paradijstuin ("Werkplaats").
- Het **Verkenningsboek** (toets B) heeft een pagina **Het Zijdeland**.

## 9. Opnieuw beginnen
```
/guhs vadsrijk z6verhaal vergeet
```
(Vergeet zone 6: quests, bakens, geheimen, films, lijn E, Ringeling. Als zone 5 klaar is, speelt de zwemtocht daarna opnieuw.)
