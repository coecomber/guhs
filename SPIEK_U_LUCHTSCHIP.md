## Luchtschip: overal klikbaar, vrij rondlopen op het dek, duidelijke "nog dicht"-melding (U-LUCHTSCHIP)

**Wat is er nieuw?**
- Je kunt het luchtschip nu **overal** aanklikken om in te stappen: de romp, het dek, de kajuit, de ballon (ook de neus met het gezicht, de staart en de oren). Rechtsklik en linksklik doen allebei hetzelfde. Linksklik slaat het schip nooit.
- Aan boord kun je **vrij rondlopen** over het dek (WASD, rennen gaat iets sneller), ook tijdens de vlucht. Je benen lopen gewoon mee. De reling houdt je tegen: je kunt niet van het schip lopen of vallen, en springen doet niets. Andere spelers aan boord zie je ook rondlopen.
- Is een lijn nog **dicht** voor jou? Klik op het schip, op de pier of op het vertrekbord, dan zegt de chat precies wat je eerst moet doen, bijvoorbeeld: "Lijn A is nog dicht voor jou. Volg eerst de hoofdquest: na "Een grauwe schaduw" kun je hier vliegen. Je bent nu bij: "Welkom, rekruut!". En je moet level 20 zijn (jij: level 3)."
- Bij de pier zegt de **actiebalk** of JIJ mee mag: groen "✔ Jij mag mee: klik ergens op het schip!" of "✖ nog dicht voor jou: volg eerst de hoofdquest tot na ...".
- Het **vertrekbord** heeft een 4e regel: "Mee na de hoofdquest "...", vanaf level ..." (aan de andere kant: "Terugvliegen mag altijd"). Je kunt het bord ook aanklikken.
- Lijn C en D gebruiken nu dezelfde regels als de kapiteins: C na De Schemerwortel + level 50, D na de Lampionkust + level 55.

**Welke lijn gaat open na welke hoofdquest?**

| Lijn | Heenweg open na | Level |
|---|---|---|
| 0 Kampsteiger → Haven Zwamzoet | "De Grauwe Klodderreus" | - |
| A Noordklifhaven → Kandijkom-dok | "Een grauwe schaduw" | 20 |
| B Galmdok → Weststeiger | "Een roze splinter" | 30 |
| C Oosttaksteiger → Lampionhaven | "Vahoegnir komt thuis" | 50 |
| D De Opstijgpier → Walvisrughaven | "De Opstijgpier" | 55 |

**1. Overal klikken** (in creative, dan is elke lijn open):
```
/gamemode creative
/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek
/guhs vadsrijk luchtschip vertrek A 60
```
- Klik met **rechts op de ballon** (hoog boven je): je staat aan boord.
- Sneak om weer uit te stappen (je komt terug op de pier). Klik nu met **links op de onderkant van de romp** of op de kajuit: weer aan boord. Het schip krijgt geen klap.
- Probeer ook de neus van de ballon (het gezicht), de staartvinnen en de oren.
- **Kijk naar:** loop over de pier naast het schip en klik op blokken van de pier. Dat moet gewoon werken (het schip zit niet in de weg en duwt je niet weg).

**2. Vrij rondlopen op het dek:**
```
/guhs vadsrijk luchtschip rij A
```
- Loop met WASD over het dek, van de kajuit tot de punt van de boeg en van reling tot reling. Ren ook eens (sprinttoets).
- **Bij vertrek en tijdens de vlucht:** blijf lopen. Het moet soepel gaan, zonder terugspringen of haperen, ook als het schip draait.
- Probeer over de reling te lopen: dat lukt niet. Springen doet niets. Sneaken geeft "Njeg! Blijf lekker aan boord".
- Eten en items gebruiken aan boord werkt gewoon (een klik op het dek wordt niet meer "opgegeten").
- **Met twee spelers** (als dat kan): je ziet elkaar rondlopen, met lopende benen.
- **Uitloggen tijdens de vlucht:** log uit en weer in. Je staat op de pier van de bestemming.

**3. Een lijn die nog dicht is** (een nieuwe speler, of reset je eigen quests):
```
/gamemode survival
/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek
```
- **Actiebalk bij de pier:** "Lijn A naar Kandijkom-dok: vertrek over ... ✖ nog dicht voor jou: volg eerst de hoofdquest tot na "Een grauwe schaduw"".
- **Klik op het schip** (als het ligt): je stapt niet in. De chat legt uit: "Lijn A is nog dicht voor jou. Volg eerst de hoofdquest: na "Een grauwe schaduw" kun je hier vliegen. Je tracker rechtsboven wijst de weg. Je bent nu bij: "...". En je moet level 20 zijn (jij: level ...)."
- **Klik op een blok van de pier** of op het vertrekbord: dezelfde uitleg (hooguit eens per 3 seconden in de chat).
- Probeer ook de andere havens:
```
/guhs vadsrijk luchtschip haven haven_kamp_z1_vertrek
/guhs vadsrijk luchtschip haven haven_z2_z3_vertrek
/guhs vadsrijk luchtschip haven haven_z3_z4_vertrek
/guhs vadsrijk luchtschip haven haven_z4_z5_vertrek
```
- **De terugweg** is altijd open. Probeer bijvoorbeeld `haven_z2_z1_vertrek`: de actiebalk is groen, en het bord zegt "Terugvliegen mag altijd".
- In **creative** is alles open (zoals eerst).

**4. Het vertrekbord:**
```
/guhs vadsrijk luchtschip haven haven_z2_z3_vertrek
```
- **Kijk naar:** er zijn 4 regels: lijn, vertrektijd, instappen, en "Mee na de hoofdquest "Een roze splinter", vanaf level 30". De tekst staat op dezelfde plek als eerst.
- Klik op het bord (rechts of links). Mag je mee, dan zegt de chat "Lijn B naar Weststeiger: jij mag mee! Vertrek over ...". Anders krijg je de uitleg.
