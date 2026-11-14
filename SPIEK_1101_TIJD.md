# SPIEK 1.10.1 TIJD: een eigen dag en nacht voor het Vadsrijk

Het Vadsrijk heeft nu een eigen klok. De dag en nacht daar staan los van de overworld.

## Hoe het was (1.10.0)
- Het Vadsrijk volgde de klok van de overworld (`default_clock: minecraft:overworld`).
- Een cyclus duurde dus 20 minuten, net als vanilla: ongeveer 10 min dag, 1,5 min avond, 7 min nacht en 1,5 min ochtend.
- Slapen in de overworld of `/time set` in de overworld veranderde ook de tijd in het Vadsrijk.

## Hoe het nu is (1.10.1)
- Standaard duurt een cyclus **20 minuten**: **ongeveer 13,3 min dag** en **6,7 min avond + nacht + ochtend** (dag_deel 0.667).
- Slapen of `/time set` in de overworld doet niks met het Vadsrijk. Andersom ook niet.
- De lucht, de zon, de maan en de sterren in het Vadsrijk volgen de Vadsrijk-klok. De overgangen blijven vloeiend.
- Alles wat "is het nacht?" vraagt, volgt de Vadsrijk-klok: winkels die dichtgaan, nachtbellen, slapers, uilen, de nachtmarkt,
  muziek en geluid, critters, de sterren-sieraden en puzzels die nacht nodig hebben.
- De tijd blijft bewaard na een herstart.

## Commando's (op)
- `/guhs vadsrijk tijd`: laat zien hoe lang de cyclus duurt, hoe lang dag en nacht zijn, hoe laat het nu is in het Vadsrijk
  (bijv. "12:00", dag of nacht), het tempo en hoe laat het in de overworld is.
- `/guhs vadsrijk tijd cyclus <minuten>`: de lengte van een hele dag + nacht (1 tot 240, standaard 20). Komma-getallen mogen, bijv. `12.5`.
- `/guhs vadsrijk tijd dag <0.3-0.9>`: welk deel van de cyclus dag is. `0.5` = half om half (zoals vanilla), `0.667` = 2/3 dag, `0.75` = 3/4 dag.
- `/guhs vadsrijk tijd zet dag`: meteen naar de volgende ochtend in het Vadsrijk.
- `/guhs vadsrijk tijd zet nacht`: meteen naar de volgende avond in het Vadsrijk.
- `/guhs vadsrijk tijd herlaad`: leest het config-bestand opnieuw (na het bewerken met de hand).
- `/time set ...` in het Vadsrijk werkt ook. Het verandert dan alleen de Vadsrijk-klok.

`cyclus` en `dag` werken meteen en worden opgeslagen in `config/guhs-vadsrijk-tijd.properties`:
```
cyclus_minuten=20
dag_deel=0.667
```

## Wat moet je zien (testen in de dev-client)
1. Ga het Vadsrijk in en typ `/guhs vadsrijk tijd`. Je ziet: "cyclus 20 min · dag 13.34 min · avond + nacht + ochtend 6.66 min".
2. `/guhs vadsrijk tijd zet nacht`: de lucht wordt donker, je ziet sterren en de maan. Winkeliers gaan naar bed en de nachtmuziek
   begint (zoals bij `/guhs vadsrijk ritme nacht`, maar nu echt via de klok).
3. Ga naar de overworld (`/guhs vadsrijk`). Daar is het nog gewoon de tijd van de overworld.
4. Typ in de overworld `/time set night` en ga terug naar het Vadsrijk. De Vadsrijk-tijd is niet veranderd.
5. Kort testen: `/guhs vadsrijk tijd cyclus 2` en dan `/guhs vadsrijk tijd zet dag`. Kijk naar de zon: overdag gaat ze langzamer
   dan 's nachts de maan (80 s dag, 40 s nacht). Daarna weer `/guhs vadsrijk tijd cyclus 20`.
6. Herstart de server. `/guhs vadsrijk tijd` toont dezelfde tijd als ervoor (plus de paar seconden van het opstarten).

## Goed om te weten
- De gamerule `advance_time` (doDaylightCycle) false zet ook de Vadsrijk-klok stil.
- `/neoforge day` of `/time rate` in het Vadsrijk wordt meteen weer teruggezet: de config bepaalt het tempo.
- Bij de eerste start na de update begint de Vadsrijk-klok bij de ochtend (dag 0).
- Alleen het Vadsrijk heeft de eigen klok. Het Knuffelkwartier (de huizen) en alle andere dimensies volgen de overworld.

## Gametests
`runGameTestServer -Pgt=VadsrijkTijd` (4 tests): de verdeling en de lengte, `/time set` in de overworld raakt het Vadsrijk niet
(en andersom), "is het nacht" volgt de Vadsrijk-klok, en de tijd wordt goed opgeslagen en geladen.
