# Spiekbriefje W-SPEL (cutscenes, laden, zonenamen, Questlopen, kussens)

Alles in het Vadsrijk, als OP. Kopieer de regel, plak in de chat.

## 1. "Ver in het oosten wil iets steeds meer" (flash F2, einde zone 3)
```
/guhs vadsrijk z3npc f2
```
Wat je ziet: eerst een zwart scherm met "De wereld wordt geladen..." (hooguit 3 s, alleen de eerste keer), dan vlieg je
over het roze Zijdeland naar één grauwe toren (de Sterrendovende Toren), met een grijze chatregel. Daarna de grauwe troonzaal:
Graaf Piepvel in zijn hemdje, Krabbel met zijn rapport, getik achter de troon, en de titel "WORDT VERVOLGD... / Ver in het
oosten wil iets steeds meer." Na afloop sta je weer waar je stond en komt "Zone 3 klaar" in de chat.
Niet meer: een lege lucht onder een donker-effect.

## 2. Laden vóór elke cutscene (alle cutscenes)
- Speel een cutscene van een plek waar je nog niet was (bijvoorbeeld hierboven, of `/guhs vadsrijk z3npc cliffhanger`).
- Wat je ziet: als de wereld daar nog niet klaar is, eerst kort zwart (na een halve seconde met "De wereld wordt geladen..."),
  dan begint het shot met een korte fade, met de wereld al helemaal opgebouwd. Is alles er al: geen zwart scherm.
- Na een verre cutscene: kort zwart tot jouw eigen omgeving weer is opgebouwd (ook hooguit 3 s).

## 3. Zonenaam bij zonewissel
```
/execute in guhs:vadsrijk run tp @s 2669 226 -536
```
(de voet van de geveltoren van de Ordekathedraal in Hoogvads). Wat je ziet: de titel **"De Ordekathedraal - veilig gebied"**,
niet meer "vadsrijk.kern.gebied.z5_kathedraal". Ook terug: De Paleisheuvel, De Universiteit, De Academie van de Wolkwacht,
De Marktlaan, Moeder Wolkwal, De Buikgangen, De Proefarena, Het Schuilnest, De Noordelijke/Zuidelijke Wolkrand, De Staartvlakte.

## 4. Kussen bij Kloofmoeder Roosje
```
/execute in guhs:vadsrijk run tp @s 3486 75 -698
```
Klik met rechts op het magenta kussen naast de troon van Roosje. Wat je ziet: je gaat zitten (en zakt niet meer door de grond).
Shift = opstaan: je staat óp of naast het kussen, nooit in of onder de vloer. Geldt voor alle kussens, zitzakken en stoelen.

## 5. Questlopen
```
/guhs vadsrijk questlopen
```
Nieuw onderaan het paneel: **[Sla stap over]** en **[Naar veilige plek]**. Ze werken altijd, ook midden in een gevecht of
terwijl het paneel "Bezig..." zegt (heropen met hetzelfde commando).
- Sla stap over: de stap telt meteen mee en je gaat naar de plek van de volgende stap.
- Naar veilige plek: stopt wat er loopt en zet je terug op de laatste rustige plek buiten een arena.
- Bij een baas (Zelf vechten of Laat hem verslaan): als de baas weg is maar de stap niet telde, telt hij na 5 s alsnog;
  daarna word je naar de volgende stap gezet, nooit achtergelaten in de arena.
Test bij Kootje Zilverstaart: Spring naar de stap "z5_hq08_wolkwaldroom" (doel 2), Zelf vechten, versla hem; daarna sta je
bij de volgende stap. Of klik meteen Sla stap over.
