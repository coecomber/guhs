## Geen dubbele bazen, NPC's en wezens meer (U-DUBBEL)

**Wat was er mis?** Kristalwacht Splinter stond 2× op dezelfde plek en er zaten 3 Kruinroofvogels in de arena. Dat kwam niet door
één baas, maar door hoe Minecraft chunks laadt:
- Als een chunk terugkomt, staan de blokken er al voordat de wezens van die chunk terug zijn. Onze bazen-arena's en de NPC-bewaker
  keken soms precies in dat gaatje, zagen "niemand" en zetten er een nieuwe neer. Daarna kwam de oude ook nog terug.
- Wezens die we nooit opslaan (NPC's, luchtschepen, vliegers, Ouwe Dobber, bewoners van Hoogvads...) bleven onzichtbaar in het
  geheugen hangen als hun chunk wegging, en kwamen later gewoon weer tevoorschijn naast de nieuwe.
Snel heen en weer vliegen in creative maakte het erger.

**Wat is er nu?**
- Elke unieke baas, NPC, gebiedsleider, luchtschip, vertrekbord, vlieger, Ouwe Dobber, bewoner, huisdier en rijdier heeft een
  eigen vaste "sleutel". Er kan er per sleutel maar één bestaan.
- Een oude kopie die van schijf terugkomt terwijl de echte er al is, wordt geweigerd.
- Spook-wezens worden opgeruimd zodra hun chunk weggaat.
- Arena's en de NPC-bewaker wachten tot de wezens van een chunk echt terug zijn voordat ze iets nieuws neerzetten.
- Al bestaande dubbelen met dezelfde sleutel worden elke 10 seconden vanzelf opgeruimd (ook die in je testwereld van vannacht).

**Testen**

1. Splinter (zone 2):
```
/guhs vadsrijk npc tp z2_kapitein
/guhs vadsrijk dubbel scan
```
Er staat nu één Kristalwacht Splinter (een paar blokken van de kapitein af). De scan zegt "Geen dubbele unieke wezens..." (of hij
noemt nog oude dubbelen; wacht dan 10 s of doe `/guhs vadsrijk dubbel opruimen`).

2. Kruinroofvogel (zone 3):
```
/guhs vadsrijk level 50
/guhs vadsrijk z3bazen tp kruinroofvogel
/guhs vadsrijk dubbel scan
```
Precies één Kruinroofvogel. Vlieg daarna in creative een paar keer heel ver weg (500+ blokken) en weer terug, en doe opnieuw
`/guhs vadsrijk dubbel scan`. Nog steeds één.

3. Rondvliegen langs de rest (doe na elk stukje `/guhs vadsrijk dubbel scan`):
```
/guhs vadsrijk npc tp kapitein
/guhs vadsrijk npc tp z2_wacht_galm_1
/guhs vadsrijk luchtschip lijnen
/guhs vadsrijk dubbel scan
```
Kijk ook bij de luchtschiphavens (vertrekbord: 3 regels, niet 6), bij Ouwe Dobber en de vliegers in zone 4, en tussen de
bewoners en de Wolkwacht-ridders in Hoogvads.

4. Tellingen:
```
/guhs vadsrijk dubbel stats
```
Laat zien hoeveel oude kopieën geweigerd, vervangen en spook-wezens opgeruimd zijn sinds de start.

**Uitleg bij de scan:** ● (rood) = twee met dezelfde sleutel (wordt vanzelf opgeruimd). ○ (geel) = twee dezelfde NPC's of twee
dezelfde benoemde dingen op één plek zonder sleutel; die haalt alleen `/guhs vadsrijk dubbel opruimen` weg. Klik op een
coördinaat om erheen te gaan.

**Let op:** een NPC of baas die net buiten je simulatieafstand staat, verschijnt pas als je wat dichterbij komt. Dat is expres.
