## Schepen: elk luchtschip ligt nu strak tegen een open pierpunt (W-SCHEPEN)

**Wat is er nieuw?**
- Elk luchtschip legt nu aan **tegen het eind van de pier**: de schroeven raken precies de pierrand, de romp ligt er een half blokje achter. Geen gat meer, geen schip dat 5 tot 20 blokken verderop in de lucht hangt.
- Aan het **eind van elke pier staat geen hek meer** (de 7 blokken voor het schip zijn open). De hekken aan de zijkanten blijven gewoon staan.
- Instappen kan op drie manieren:
  - **loop het schip op**: stap van de pierpunt af richting het schip en je staat aan boord;
  - **klik op het schip** (dat kon al; nu zit het binnen 1 blok van je neus);
  - **klik op de pierpunt zelf** (een plank aan het eind van de pier).
- Mag je nog niet mee (lijn dicht)? Dan zegt de chat waarom, en je blijft op de pier staan.
- **Vangnet:** ligt het schip er niet en loop je van de open pierpunt af, dan sta je meteen weer op de pier, met "Het luchtschip ligt hier nog niet aan. Volgende vertrek over m:ss". Je valt dus nooit van een pier.
- **Lijn 0 vliegt** (Kampsteiger ↔ De Haven). In De Haven ligt hij aan het westeinde van de lange houten pier in de baai.
- **Lijn E** vertrekt nu echt van de **Staarthaven**: het oosteinde van het pad over de staart van Moeder Wolkwal (tussen de twee staartvinnen). Daar stond eerst geen pier.
- Rozenkloof: het schip legt nu aan de **zuidkant** van de pier aan (hij lag over de pier en de toren heen). Het hekje aan het eind is weg.
- Zone 3: de "mast"-plek van beide pieren is nu een echte staplek naast de mast (was binnenin de paal).

**Snel testen** (creative of survival; in creative is elke lijn open). Per haven: teleporteer, zet de vertrektijd op 2 minuten zodat het schip blijft liggen, en kijk.
```
/guhs vadsrijk luchtschip vertrek <lijn> 120
/guhs vadsrijk luchtschip haven <haven-id>
```
De `haven`-teleport zet je op de pierpunt, met je gezicht naar het schip.

| Lijn | Haven | Teleport | Wat je moet zien |
|---|---|---|---|
| 0 | Kampsteiger | `/guhs vadsrijk luchtschip haven haven_kamp_z1_vertrek` | noordelijke arm van de kampsteiger, oosteinde open; schip naar het oosten, de vlag naast de punt raakt de ballon niet |
| 0 | De Haven | `/guhs vadsrijk luchtschip haven haven_z1_kamp_vertrek` | westeinde van de lange pier in de baai; schip boven het water naar het westen (geen zwemladder meer op de punt; die bij x 37/42 blijven) |
| A | Noordklifhaven | `/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek` | noordrand van het pierplatform open; de roze vlag aan de mast wappert nu naar buiten |
| A | Kandijkom-dok | `/guhs vadsrijk luchtschip haven haven_z2_z1_vertrek` | zuidrand van het dok open (7 breed); het oude vastgemaakte blokkenschip aan de oostkant blijft |
| B | Galmdok | `/guhs vadsrijk luchtschip haven haven_z2_z3_vertrek` | oostrand van het pierplatform open |
| B | Weststeiger | `/guhs vadsrijk luchtschip haven haven_z3_z2_vertrek` | westkant van het ronde platform open; schip naar het westen, niet meer over de mast |
| C | Oosttaksteiger | `/guhs vadsrijk luchtschip haven haven_z3_z4_vertrek` | oostkant van het ronde platform open (dit was "de boot te ver" bij de Oosttaksteiger) |
| C | Lampionhaven | `/guhs vadsrijk luchtschip haven haven_z4_z3_vertrek` | westrand van het luchthavenplatform; schip ligt er nu tegenaan (lag 12 blokken verderop) |
| D | Opstijgpier (Kapitein Hemelhoog) | `/guhs vadsrijk luchtschip haven haven_z4_z5_vertrek` | oostkant van het ronde platform: de witte hekjes zijn daar weg, schip ligt er strak tegen (lag 6 blokken los achter de hekjes) |
| D | Walvisrughaven | `/guhs vadsrijk luchtschip haven haven_z5_z4_vertrek` | eind van pier 2: geen hekjespoort meer, de twee masten zijn lager (onder de ballon) |
| E | Staarthaven | `/guhs vadsrijk luchtschip haven haven_z5_z6_vertrek` | oosteinde van het staartpad, tussen de staartvinnen; schip naar het oosten de lucht in |
| E | Rozenkloof | `/guhs vadsrijk luchtschip haven haven_z6_z5_vertrek` | zuideinde van de smalle pier: geen poortje meer, mast lager; schip naar het zuiden |

**Per haven proberen:**
1. In **survival**: loop naar het eind van de pier en stap door, het schip op. Je staat aan boord (welkomstbericht in de chat).
2. Sneak om weer uit te stappen (je komt op de pier). Klik nu met rechts op een **plank van de pierpunt**: ook aan boord.
3. Wacht tot het schip vertrokken is (of `/guhs vadsrijk luchtschip vertrek <lijn> 1`) en loop dan van de open punt af: je staat meteen weer op de pier, met de melding wanneer het volgende schip komt.
4. Vlieg mee (`/guhs vadsrijk luchtschip rij <lijn>`) en kijk bij aankomst: je komt op de pierpunt aan de overkant terecht.

**Kapitein Hemelhoog** (jouw melding): `/guhs vadsrijk luchtschip haven haven_z4_z5_vertrek`. Kijk naar het oosten: open rand, het schip ligt ertegenaan, geen hekjes ertussen.

**Bootjes in zone 4** (zeilbootje, X-toets): niets veranderd, de controle zegt dat alle 11 steigers water binnen 1 à 2 blokken hebben en bereikbaar zijn.
