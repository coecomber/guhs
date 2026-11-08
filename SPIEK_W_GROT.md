# SPIEK W-GROT: grotten hebben een echte dichte deur + een kerkervenster met wachtrij

Elke grotkerker (De Zwamgrot, De Kristalgrot, De Wortelgrot, De Koraalgrot, De Wolkengrotten, De Zingende Zoutgrot) heeft nu een
**echte dichte deur**. Je kunt er niet doorheen lopen. **Rechtsklik op de deur** (ook met een wapen in je hand) en het
**kerkervenster** gaat open. Daarin start je de grot: solo, met je groepje, of via de wachtrij met andere spelers.

> Test in **survival** (in creative zet de grot je er niet uit). De deur verschijnt vanzelf zodra het stukje wereld geladen is
> (een paar seconden). Zie je hem niet: `/guhs vadsrijk grot deur <grot> tp` zet hem meteen neer.

## 1. Naar elke deur

| Grot | Deur | Teleport (je staat voor de deur) |
|---|---|---|
| De Zwamgrot (z1) | in de stam van de reuzenpaddenstoel, 3 breed x 3 hoog | `/guhs vadsrijk grot deur zwamgrot tp` |
| De Kristalgrot (z2) | in de tunnel achter de kristallen boog (6 blokken naar binnen) | `/guhs vadsrijk grot deur kristalgrot tp` |
| De Wortelgrot (z3) | in de holle wortelstronk (noordkant), 3 x 3 | `/guhs vadsrijk grot deur wortelgrot tp` |
| De Koraalgrot (z4) | in de koraalschoorsteen op de atolrand (de droge weg); de duikingang op de zeebodem heeft óók een deur | `/guhs vadsrijk grot deur koraalgrot tp` |
| De Wolkengrotten (z5) | een **wolkenluik**: loop het blaasgat in, de wenteltrap af; het luik zit in de schacht | `/guhs vadsrijk grot deur wolkengrot tp` |
| De Zingende Zoutgrot (z6) | onderaan de uitgehakte trap, vlak voor de grote zaal, 3 x 4 | `/guhs vadsrijk grot deur zoutgrot tp` |

Coördinaten (staanplek voor de deur): Zwamgrot -412 69 148 · Kristalgrot 869 119 -1104 · Wortelgrot 1410 69 -737 ·
Koraalgrot 2230 71 -596 · Wolkengrotten 2494 205 -520 (op het luik) · Zoutgrot 3874 50 -521.
Lijst van alle deuren: `/guhs vadsrijk grot deur`

Wat je moet zien:
- Een dichte deur in het thema van de grot (paddenstoelhout, kristal, donker wortelhout, koraal/prismarine, wolkenmarmer, zout met
  messing). In het midden zit een **klopper** (ring). Bij de Wolkengrotten is het een **luik** in de vloer.
- Je kunt er niet doorheen lopen of hem kapotslaan.

## 2. Het kerkervenster

Rechtsklik op de deur (of zonder lopen: `/guhs vadsrijk grot deur zwamgrot`). Je ziet:
- bovenin (roze balk): **de naam van de grot** en de zone;
- **Aanbevolen: level 11-14 · 1-4 spelers (alleen goed te doen)** (zone 3+: "2-4 spelers (alleen is pittig)");
- **Grotbaas: Moeder Glimhoed** + haar ondertitel;
- **Buit bij elke clear**: munten (zone 1: 700 brons), 1 uitrustingsstuk (je eerste clear: Episch; daarna Zeldzaam met 12% kans op
  Episch), 3-5 ertsen, 25% kans op de bazenkern;
- **Zeldzaam (persoonlijk)**: het huisdier (bijv. Glimhoedje: 6% per clear · pech 0/25) en de Pluimguh-kleur (bijv. Glimzwam: 10%
  per clear · pech 0/15, of "pas als je een Pluimguh hebt", of "✔ al van jou"). Dezelfde getallen als `/guhs vadsrijk loot kansen`;
- **Jij: N keer uitgespeeld** en je **wachttijd** ("Geen wachttijd" of "Wachttijd: nog 9:12");
- de status: "De grot is vrij." / "Bezet: een groep van 2 is binnen (3:10)." / "Jij staat in de wachtrij: 2/4";
- knoppen: **Solo starten**, **Starten met groep (n)**, **In de wachtrij** (of **Uit de wachtrij** + **Nu starten (2/4)**).
  Beweeg over een knop voor uitleg.

## 3. Solo

Klik **Solo starten**: je staat meteen binnen (vlak achter de deur), de titel van de grot verschijnt. Loop de grot in: de run
start ("⚔ De grot wordt wakker!") en gaat verder zoals in SPIEK_U_GROTKERKER (2 kamers, poort, baas, eindkist).
Dev-snelweg (geen wachttijd/regels): `/guhs vadsrijk grot run zwamgrot`

Na de baas:
- chat: "Wachttijd voor De Zwamgrot: 10:00" en "Grot uitgespeeld! Rechtsklik op de deur ... om naar buiten te gaan";
- na 3 seconden opent het venster vanzelf met **Grot verlaten** / **Nog even blijven**;
- **Grot verlaten** = je staat buiten op de staanplek voor de deur. Je wordt NIET meteen weer naar binnen gezet.
- Rechtsklik nu op de deur: je ziet "Wachttijd: nog 9:5x" en de startknoppen staan grijs.
- Wachttijd weg: `/guhs vadsrijk grot wachttijd weg` (alle grotten) of `/guhs vadsrijk grot wachttijd weg zwamgrot`
- Wachttijd zetten om het venster te zien: `/guhs vadsrijk grot wachttijd zwamgrot 90`

## 4. De wachtrij alleen testen (nep-spelers)

```
/guhs vadsrijk grot nep zwamgrot 3
```
- Er komen 3 nep-spelers ("Testguh 1..3") in de wachtrij van De Zwamgrot, en jij ook: de rij is 4/4.
- Meteen: **Klaar voor De Zwamgrot?** venster met de 4 namen (… = wacht), een tijdbalk van 30 s, en **Klaar!** / **Nee**.
- Klik **Klaar!**: na een seconde zeggen de nep-spelers ook ja (✔). Jullie gaan naar binnen; in de chat: "(Testguh 1 is een
  nep-testspeler en blijft buiten)". Je bent alleen binnen (de run telt als 1 speler).

Weigeren testen:
```
/guhs vadsrijk grot nep zwamgrot 3 nee
```
- Klik **Klaar!**: "Testguh 1, Testguh 2, Testguh 3 doet (nog) niet mee. De anderen blijven in de wachtrij." Je staat nog in de
  rij (1/4), de nep-spelers zijn eruit.

Te laat testen:
```
/guhs vadsrijk grot nep zwamgrot 3 niets
```
- Klik **Klaar!** en wacht 30 s: "Niet iedereen was op tijd klaar...". Jij blijft in de rij.

Eerder starten (niet vol):
```
/guhs vadsrijk grot nep zwamgrot 1
/guhs vadsrijk grot deur zwamgrot
```
- De rij is 2/4. Klik in het venster **Nu starten (2/4)** → klaar-check voor 2 → Klaar! → naar binnen.

Wachtrij-status:
- In de wachtrij zie je elke 2 seconden in de actiebalk: **"Wachtrij De Zwamgrot: 1/4"** (en "· grot bezet" als er een groep binnen is).
- Je mag rondlopen; je blijft in de rij. Uitloggen of naar een andere wereld = uit de rij ("Je bent uit de wachtrij ... (andere wereld)").
- **Uit de wachtrij** in het venster haalt je eruit.
- Overzicht van alles: `/guhs vadsrijk grot rij`

## 5. Met je groepje

Maak een groepje (party) met een tweede speler. De leider klikt **Starten met groep (2)** → iedereen krijgt de klaar-check (de
leider staat al op ✔) → allemaal Klaar! → samen naar binnen. Iemand op Nee → "... doet (nog) niet mee. Starten gaat niet door."
Heeft iemand nog wachttijd of zit iemand al in een grot/de raid, dan zegt de knop dat (en wie).

## 6. Regels die je kunt proberen

- **Niet-leden komen er niet in**: zit er een groep in De Zwamgrot, dan zegt de deur voor een ander "Bezet: een groep van 1 is
  binnen". Loop (in survival) met `/guhs vadsrijk grot tp zwamgrot hal`... nee, dat maakt je lid (dev). Teleporteer in plaats daarvan
  met `/tp @s -482 47 130` de grot in terwijl een andere groep binnen is: je staat meteen weer buiten de deur ("De grot is van een
  andere groep"). Zonder groep binnen: "Deze grot is dicht. Start hem via de deur."
- **Flauwgevallen?** Je wordt buiten wakker (zoals altijd). Rechtsklik op de deur: **Terug naar binnen** brengt je terug bij je groep.
- **Groep weg** (iedereen een minuut buiten de grot, na een clear 10 s): "Jullie groep is weg: de grot gaat weer dicht."
- **Uitgespeeld en er wacht een andere groep**: "Er wacht een andere groep: de grot gaat over 2:00 dicht" → daarna sta je buiten.
- Niet starten terwijl je al in een grotkerker of de raid zit.

## 7. Dev-commando's (kort)

| Commando | Wat |
|---|---|
| `/guhs vadsrijk grot deur` | lijst van alle deuren (staanplek + binnen) |
| `/guhs vadsrijk grot deur <grot>` | het kerkervenster openen |
| `/guhs vadsrijk grot deur <grot> tp` | naar de staanplek voor de deur (en de deur meteen neerzetten) |
| `/guhs vadsrijk grot nep <grot> <1-3> [ja\|nee\|niets]` | nep-spelers in de wachtrij (jij erbij) |
| `/guhs vadsrijk grot rij` | alle grotten: bezet door wie, wachtrijen, klaar-checks |
| `/guhs vadsrijk grot wachttijd weg [grot]` | wachttijd weg |
| `/guhs vadsrijk grot wachttijd <grot> <seconden>` | wachttijd zetten |
| `/guhs vadsrijk grot verlaat` | uit je grotkerker (naar buiten) |
| `/guhs vadsrijk grot run <grot>` | dev: solo naar binnen, zonder wachttijd/regels |
