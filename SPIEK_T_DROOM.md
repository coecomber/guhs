## De Grauwe Droom (zone 5, finale fase DROOM) — T-DROOM

**Wat is het?** Na Kootje Zilverstaart (de spion) in De Wolkwaldroom zak je in de **droom van Moeder Wolkwal**. Je komt op
**de Droomwolk** (een ronde wolkvloer in de lucht, r 17, ten oosten van de staartvin). Daar zwemt **De Grauwe Droom** rondjes:
een grote grijze walvis van mist met traantjes (lv 60). Je verslaat hem niet, je **vrolijkt hem op**: aan het eind wordt hij roze
en speelt het echte slaapliedje op een fluit. Daarna word je **wakker bij het middelste spuitgat**.

### Snel testen (in Het Vadsrijk, op level 2)
```
/guhs vadsrijk droom start
```
Je ziet "Je zakt in een droom...", je staat op de wolk, na 1,5 s de intro (camera + titel "De Grauwe Droom").

| Commando | Wat er moet gebeuren |
|---|---|
| `/guhs vadsrijk droom start` | Droom + gevecht (als herinnering, geen quest nodig). Je party binnen 24 blokken gaat mee. |
| `/guhs vadsrijk droom info` | Plek van de wolk, levens, fase, neurie-meter, ranken, nacht, slaapliedjes goed/mis. |
| `/guhs vadsrijk droom wiegelied` | De 4 nootstenen op de rand lichten één voor één op (kleur + fluittoon). Daarna: rechtsklik ze **in die volgorde**. Goed = roze hartjes, kaart "Het slaapliedje klinkt weer goed!", de walvis staat 8 s stil en krijgt 10 s +50% schade. Een verkeerde noot = "Vals! Begin weer bij de eerste noot." (geen schade). Te laat (14-16 s) = "Nachtmerrie!": 30% bij iedereen, hij heelt 4%. |
| `/guhs vadsrijk droom ranken` | 3 grijze ranken groeien uit de wolk. Ze grijpen de dichtstbijzijnde speler (grijs lijntje, plakkerig, 4% per 2 s) en maken de droom 15% taaier per rank. Sla ze stuk: roze pluf, "Doorgeknipt!". |
| `/guhs vadsrijk droom omkeer` | De droom draait om: de wolkvloer wordt **donkergrijs/zwart** (omgekeerde kleuren), vier **roze wolkjes** (roze cirkels) drijven rond. Wie niet op een roze wolkje staat: 7% per 2 s. Na 20 s wordt de vloer weer wit. |
| `/guhs vadsrijk droom fase 2` | Zet hem op 66%: de volgende klap start fase 2 ("De droom draait om!" + omkeer, daarna het slaapliedje). |
| `/guhs vadsrijk droom fase 3` | Zet hem op 31%: de volgende klap start fase 3 "Samen neuriën!": een **gele cirkel** in het midden. Hij zakt niet onder 30% ("De droom is te zwaar: neurie eerst...") tot de neurie-meter (actiebalk) vol is: sta in de gele cirkel (solo ~15 s, met meer spelers sneller; muzieknootjes boven je hoofd). Vol = "Moeder Wolkwal neuriet mee!", hij wordt **lila** en krijgt +25% schade. |
| `/guhs vadsrijk droom neurie` | Neurie-meter meteen vol (lila). |
| `/guhs vadsrijk droom win` | Meteen opvrolijken: roze, hartjes, slaapliedje op de fluit, hij zweeft omhoog. Je kist staat op de wolk. Open je kist → je wordt wakker bij het spuitgat ("Je wordt wakker / Moeder Wolkwal slaapt weer zoet"). Zonder kist open: na 90 s. |
| `/guhs vadsrijk droom wakker` | Meteen wakker worden bij het middelste spuitgat. |
| `/guhs vadsrijk droom tp` | Alleen naar de wolk (geen gevecht). |
| `/guhs vadsrijk droom bouw` | De wolkvloer opnieuw leggen (alleen in lucht, sloopt nooit iets). |
| `/guhs vadsrijk droom reset` | Grauwe Droom weg (vloer wordt weer wit). |
| `/guhs vadsrijk droom loot [level]` | Een kistworp in je tas: 1e keer zeker een benoemd Legendarisch wapen + de trofee. |
| `/guhs vadsrijk droom verhaal` | Roept de echte verhaal-haak (`Z5Finale.start`) aan: doet alleen iets tijdens z5_hq08, na het spuitgat. |

### Via het verhaal
1. `z5_hq08` → naar het middelste spuitgat → Kootje Zilverstaart (t-z5wezens) → verslagen → **meteen de droom** (deze slice).
2. Na het opvrolijken telt het doel "De Grauwe Droom" voor je hele party en ga je terug naar de koningin.
3. Zonder deze slice speelde er een korte camerascène; die is nu vervangen door het echte gevecht.

### Opletten / kijken
- **Van de wolk vallen:** tijdens het gevecht vangt een droomwolkje je op ("Een droomwolkje vangt je op...") en sta je weer aan de zuidrand. Na het gevecht = van de wolk stappen is wakker worden.
- **Herinnering (opnieuw spelen, voor loot + huisdier):** na het verhaal **2 s hurken op het middelste spuitgat** → "Je ogen worden zwaar..." → je droomt het nog een keer.
- **Huisdier:** *Droomwalvisje* (klein roze wolkwalvisje) na de eerste keer opvrolijken.
- **Loot:** de zone-5 bazenkist (Wolkwachtset, sieraden, wapens) + **Wiegeliedstaf** (Magiër), **Zwaard van de Zoete Droom** (Ridder), **Slaapsterrenboog** (Jager), heel zeldzaam **Moeder Wolkwals Slaapmuts** (Guhddelijk, hoofd), trofee **Een Fijne Droom (in een potje)**.
- **Uiterlijk** (zelf beoordelen): grijze walvis met traantjes en wolkjes op zijn rug; lila met "o"-mondje na het neuriën; roze en blij aan het eind. Nootstenen: wolksteen met een gouden randje en een draaiende muzieknoot (roze, geel, lichtblauw, lila). Ranken: zachte grijze sliertjes met blaadjes.
- **Muziek:** hook-id `guhs:vr.z5droom.muziek.grauwe_droom` (nog geen geluid; t-geluid kan het vullen).
