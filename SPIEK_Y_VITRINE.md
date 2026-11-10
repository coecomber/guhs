## Y-VITRINE: de Vadsrijk-vitrine (trofeeblok voor je basis)

Eén blok, `guhs:vr_vitrine`: een glazen kastje op een pruimhouten sokkel met goudrand. Het laat **live** je held uit Het Vadsrijk zien. Je zet hem **buiten** Het Vadsrijk neer, in je survivalbasis of Guhmension.

### 1. Krijgen en neerzetten
In de overworld of de Guhmension, NIET in Het Vadsrijk:
```
/give @s guhs:vr_vitrine
```
(of `/guhs vadsrijk vitrine geef`; hij staat ook in de creative-tab Guhs). In survival komt hij als FTB-beloning van "Word rekruut van De Knuffelorde" (zodra y-ftb/integratie dat koppelt).
- Zet hem neer. In de chat: **"Je Vadsrijk-vitrine staat!"** Jij bent nu de eigenaar.
- Je ziet:
  - in het glas: **je rijdier als beeldje** dat langzaam ronddraait. Geen rijdier? Dan je huisdier. Geen van beide? Dan het **roze hartschild van De Knuffelorde** met een kroontje;
  - erboven, zwevend (alleen binnen ~14 blokken): **"<naam> in Het Vadsrijk"**, je klasse (+ spec) + level, de zone-badges **① ② ③ ④ ⑤ ⑥**, en eventueel "Raid: …x gewonnen" en "Eerste zone voltooid: dd-mm-jjjj".
- Het kastje geeft een zacht licht (lichtniveau 6).

Proberen in Het Vadsrijk:
```
/guhs vadsrijk
```
- Zet hem daar neer: dat lukt **niet**, en boven je hotbar staat "De vitrine hoort in je eigen basis: buiten Het Vadsrijk." (In Het Knuffelkwartier ook niet.)

### 2. Klik erop
- Rechtsklik (met lege hand of een niet-blok) = een **overzicht in de chat**: klasse, "Zones voltooid: x van y", per zone ✔ / · (nog bezig) / "? Zone n: binnenkort", raid, datum en wat er in de vitrine staat.
- Iemand anders klikt? Die ziet JOUW held. Ben jij offline, dan staat eronder: "(… is nu niet online: dit is de stand van de laatste keer.)"

### 3. Zien vollopen (nep-voortgang, als op)
Ga bij je vitrine staan (overworld) en doe na elkaar:
```
/guhs vadsrijk level 25
/guhs vadsrijk vitrine zone 1
/guhs vadsrijk mount geef knuffelhert
/guhs vadsrijk mount rijd knuffelhert
/guhs vadsrijk huisdier geef koning_klodder
/guhs vadsrijk vitrine raid 3
/guhs vadsrijk vitrine ververs
```
- `vitrine zone <1-6>` zet alle hoofdquests van die zone op klaar. Badge ① licht roze op, en "Eerste zone voltooid: <vandaag>" verschijnt.
- Het beeldje wordt een **Knuffelhert** (je actieve rijdier; anders je rijdier met het hoogste level). Zonder rijdier: je actieve huisdier.
- Vanzelf ververst hij elke **5 seconden** zolang de eigenaar online is. `vitrine ververs` doet het meteen.
- `/guhs vadsrijk vitrine` = jouw eigen overzicht + de dev-opties.
- `/guhs vadsrijk vitrine wisdatum` = de datum opnieuw laten zetten.

### 4. Zone-poort (release: alleen zone 1 open)
```
/guhs vadsrijk zones open 1
/guhs vadsrijk vitrine zone 2
/guhs vadsrijk vitrine ververs
```
- Badges: **① ? ? ? ? ?**. Zone 2-6 zijn "?" en worden **nergens bij naam** genoemd, ook niet als je ze (als op) al klaar hebt.
- Geen raid-regel.
- Klik: "? Zone 2: binnenkort" enz.

```
/guhs vadsrijk zones open 6
```
- Binnen 5 s (of na `vitrine ververs`): alle zes badges, de zonenamen in het overzicht en de raid-regel. Dat werkt ook als de eigenaar offline is.

### 5. Offline-cache
- Log uit met account A. Met account B zie je nog steeds A's vitrine, met de stand van de laatste keer.
- A logt weer in: binnen 5 s staat de nieuwe stand erin.
- Na een herstart van de server blijft het staan (het zit in het blok opgeslagen).
