# SPIEK U-TAPIJT: het vliegend tapijt (zone 6, Zijdeland)

Het tapijt is een rijdier zoals de andere (N-scherm, X-toets, levels), maar het beweegt heel anders: het **zweeft en glijdt**.
Zone 6 bestaat nog niet, dus je krijgt het met een commando. Alles werkt overal in het Vadsrijk (niet in een baasarena).

## 1. Krijgen en meteen rijden
```
/guhs vadsrijk tapijt geef
/guhs vadsrijk tapijt rijd
```
- `geef`: het tapijt is van jou (met een roze kussentje en gouden kwastjes), plus een tip. Daarna: **N** → "Vliegend tapijt" → Kiezen,
  en dan **X** zoals elk rijdier.
- `rijd`: geeft het tapijt en zet je er meteen op, hier.
- Kijk: jij zit **in kleermakerszit** (benen gekruist) midden op het tapijt; het tapijt golft zachtjes, de kwastjes wapperen mee,
  op de vier hoeken staan guh-oortjes (roze binnenkant).

## 2. Het gevoel (dit is de kern, graag goed proberen)
- **W indrukken**: hij trekt stevig maar soepel op (in ~2 s op topsnelheid, ~15 blokken per seconde op level 1).
- **W loslaten**: hij stopt NIET meteen. Hij glijdt door en wordt langzaam trager (vanaf topsnelheid ~45 blokken uitglijden).
- **S**: remmen (duidelijk sneller dan uitglijden); stilstaand gaat hij langzaam achteruit.
- **A/D**: een zacht duwtje opzij.
- **Drift**: draai met de muis. Het tapijt draait mee met je kijkrichting, maar zweeft nog even **de oude kant op** en buigt pas
  geleidelijk bij (met W ingedrukt sneller, zonder W heel langzaam, als op ijs). Probeer: volle vaart, W los, snel 90° draaien.
- **Kantelen**: in een bocht helt het tapijt over (jij helt mee), bij optrekken gaat de neus iets omhoog, bij remmen omlaag.
- **Zweven**: ~1,25 blok boven de grond, ook boven **water, zand en lava**. Trapjes en heuvels: hij glijdt soepel omhoog (hij kijkt
  vooruit), geen gehobbel. Van een **richel** zakt hij langzaam en zweverig naar beneden. Onder een laag plafond zweeft hij lager.
- **Spatie**: een klein hupje (alleen als hij op zweefhoogte is). **Hoger vliegen kan niet**: nooit meer dan ~1,5 blok boven de
  zweefhoogte, ook niet als je spatie ingedrukt houdt.
- Multiplayer: je wordt niet gekickt voor "vliegen"; de server zet een tapijt terug als het te snel of te hoog gaat (alleen bij valsspelen).

## 3. Live afstellen (tunen)
```
/guhs vadsrijk tapijt tune
/guhs vadsrijk tapijt tune uitglijden 0.99
/guhs vadsrijk tapijt tune grip_glijden 0.005
/guhs vadsrijk tapijt tune versnelling 0.1
/guhs vadsrijk tapijt tune zweefhoogte 1.5
/guhs vadsrijk tapijt tune kanteling -3.5
/guhs vadsrijk tapijt tune reset
/guhs vadsrijk tapijt status
```
- `tune` zonder meer: alle getallen (een * = niet standaard). Elke wijziging werkt **meteen** (ook als je al rijdt), wordt opgeslagen
  in `config/guhs-vadsrijk-balans.toml` onder `[tapijt]`, en gaat binnen een seconde naar alle spelers.
- De belangrijkste knoppen:

| sleutel | standaard | wat |
|---|---|---|
| `topfactor` | 2.1 | topsnelheid = rijdiersnelheid x dit |
| `versnelling` | 0.07 | hoe stevig W optrekt (hoger = sneller op topsnelheid) |
| `uitglijden` | 0.984 | snelheid die per tick overblijft zonder W (hoger = langer doorglijden; 0.99 = heel lang) |
| `remmen` | 0.04 | hoe hard S remt |
| `achteruit` | 0.25 | achteruit-snelheid (deel van de top) |
| `zijwaarts` | 0.4 | hoe hard A/D opzij duwt |
| `grip` | 0.045 | met W: hoe snel de vaart naar de kijkrichting buigt (lager = meer drift) |
| `grip_glijden` | 0.012 | zonder W: hetzelfde (lager = langer de oude kant op) |
| `draai_volg` / `draai_max` | 0.2 / 5 | hoe snel het tapijt naar je kijkrichting draait (max graden per tick) |
| `zweefhoogte` | 1.25 | blokken boven grond/water |
| `veer` / `omhoog_max` / `omhoog_acc` | 0.22 / 0.42 / 0.09 | hoe soepel/snel hij omhoog glijdt (trapjes) |
| `zwaarte` / `val_max` | 0.035 / 0.14 | hoe zweverig hij zakt (van een richel) |
| `hop_max` / `hop_kracht` | 1.5 / 1.0 | het hupje: max extra hoogte / kracht |
| `kanteling` / `kanteling_max` | 3.5 / 20 | overhellen in bochten (negatief = andere kant op, als het verkeerd om voelt) |
| `server_marge` | 1.35 | hoeveel sneller dan de top de server toestaat (lag) |

- Ook via de gewone balans: `/guhs vadsrijk balans tapijt <sleutel> <waarde>`.

## 4. Aankleden (N → Vliegend tapijt → Aankleden)
```
/guhs vadsrijk tapijt looks
```
(alle tapijtlooks van jou). Drie rijen, met eigen namen:
- **Weefsel**: Rozensaffraan (standaard), Turkoois, Granaatappel (stal), Zandroos (8 Gouden Veren), Sterrennacht (tapijt level 10),
  Eerste Karavaan (beloning: de kist in de Zingende Zoutgrot, later).
- **Kwastjes**: goud (standaard), roze, turkoois (stal), wit (3 veren), regenboog (beloning: "De verloren tapijtkwastjes", later).
- **Kussen**: roze (standaard), saffraan, turkoois (stal), paars (4 veren). "×" = zonder kussen.
- Kijk: het N-scherm toont het tapijt in de lijst (rijen iets lager nu er 10 rijdieren zijn), met Snelheid, Sprong en een regel
  "Zweeft 1.2 blok hoog · glijdt ~45 blokken uit". Tooltip/bron bij looks die je nog niet hebt.

## 5. Overig
```
/guhs vadsrijk tapijt status
/guhs vadsrijk tapijt reset
```
- `status`: heb je het tapijt, level; terwijl je rijdt: topsnelheid, hoogte boven de grond, zweefdoel.
- `reset`: tapijt vergeten (om `geef` opnieuw te testen).

**Let op / graag beoordelen in de client:** de kantelrichting (anders `tune kanteling -3.5`), de kleermakerszit (benen), het kussen
(plek/grootte), de golf-animatie en de hoekoortjes. Dat heb ik niet in een client gezien.
