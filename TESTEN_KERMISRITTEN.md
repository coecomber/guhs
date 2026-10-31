# Testen: kermisritten (1.8.1)

Zonder kermis-stempel kun je elke rit los testen. In een creative wereld op een vlakke plek:

1. `/setblock ~ ~ ~5 guhs:kermisrit_reuzenrad[facing=south]` (of `kermisrit_booster`, `kermisrit_zweefmolen`, `kermisrit_twister`).
2. Binnen 2 seconden verschijnt het kaartjespaaltje met naamlabel, en de bewegende delen staan in rust.
3. Klik op het paaltje: je zit in een stoel. Na 8 seconden start de rit.
4. Sluipen tijdens de rit: melding, je blijft zitten.
5. Einde: je staat bij de poort met 1 kermisbon en de advancement.
6. Niemand in de rit, wel iemand binnen 64 blokken: om de 20 seconden stilstand draait hij leeg.
7. Andere facing: `[facing=north]` enz. draait de hele rit en het paaltje mee.

Te bekijken in de client (nog niet gezien):

- Loopt de beweging soepel, ook als rijder en voor toeschouwers?
- Klopt de stand van de rijder in de stoel en gondel?
- Booster: passeert de gondel de mast netjes?
- Uitloggen tijdens een rit: bij terugkomst sta je op de grond.

Voor de stempel: controllerblok op (midden, G, midden), in de vloer onder de rit. Facing is de poortkant. De stempel bouwt alleen de vaste delen; zie het rapport van deze slice voor wat er weg moet uit de renders.
