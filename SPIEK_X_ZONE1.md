# SPIEK X-ZONE1: de zone-poort (release met alleen zone 1)

## Zo zet je de poort
- **Op de server (blijvend):** in de wereldmap `serverconfig/guhs-vadsrijk-zones.toml`:
  ```
  openZones = 1
  ```
  (1 = alleen Knuffelkamp + zone 1, 6 = alles open; standaard 6). Voor een nieuwe wereld kun je hetzelfde bestand in
  `defaultconfigs/` van de server zetten, dan begint elke nieuwe wereld meteen op 1.
- **Tijdens het spelen (als op):** `/guhs vadsrijk zones open 1` : werkt meteen (geen herstart) en wordt in die toml bewaard.
- **Kijken hoe hij staat:** `/guhs vadsrijk zones`
- **Ops en creative gaan overal doorheen** (testen). Wil je zien wat een speler ziet: zet jezelf in survival en haal tijdelijk je
  op weg (`/deop <naam>` vanaf de console), of test met een tweede account.

## Wat een speler ziet aan het eind van zone 1 (poort op 1)
1. Hij speelt het hele zone-1-verhaal uit tot en met "Een grauwe schaduw" (hq12, inleveren bij de Grootmeester) en krijgt de
   cliffhanger-film. Alle zijquests, de Zwamgrot en de geheimen werken gewoon.
2. De questtracker (rechtsboven) zegt daarna: **"Wordt vervolgd: Het schip naar De Splinterpieken vaart binnenkort. Houd
   Discord in de gaten!"** (geen lichtstraal).
3. De luchtschipper op De Haven zegt: **"Het schip naar De Splinterpieken vaart binnenkort! Houd Discord in de gaten."** De keuze
   "naar Kandijkom" staat er grijs bij met dezelfde zin. Op de reiskaart staan Kandijkom en verder grijs met "Binnenkort!".
4. Op de Noordklifhaven (lijn A) zegt de pier/het bord hetzelfde; het schip neemt hem niet mee.
5. Probeert hij er toch heen te lopen/zwemmen/vliegen (elytra, enderparel): hij wordt teruggeduwd of teruggezet op zijn laatste
   plek in zone 1, met de melding "De Splinterpieken is nog dicht: het schip daarheen vaart binnenkort!".
6. Level: hij stopt op **level 20** (balk vol). Eén keer in de chat: "Level 20: het hoogste level tot het schip naar De
   Splinterpieken vaart! Extra xp spaar je op (tot 3 levels)...". In het Vadsrijk-menu (Karakter) zie je bij de balk "Hoogste
   level voor nu" en hoeveel xp gespaard is.

## Later de poort omhoog (zone 2 open)
- `/guhs vadsrijk zones open 2` (of in de toml `openZones = 2`). Meteen actief; niemand hoeft iets te doen:
  - de luchtschipper biedt "De Splinterpieken" (z2_hq01) aan, de tracker wijst naar hem, lijn A vliegt, de reiskaart kleurt;
  - de spaarpot betaalt uit: elke xp-winst krijgt hetzelfde bedrag extra tot de pot leeg is (dubbele xp, geen sprong ineens);
  - het level-plafond schuift naar 30 (bij poort 2), 50 (3), 56 (4), 60 (5), geen (6).
- Terug omlaag kan ook; wie dan in een dichte zone staat, wordt bij de volgende tik naar zijn laatste open plek (of De Haven) gezet.

## Snel testen in de dev-client (zet jezelf in survival en gebruik even een niet-op account, of laat de check doen met de gametests)
| Wat | Commando | Wat je ziet |
|---|---|---|
| Poort dicht | `/guhs vadsrijk zones open 1` | "zones 1-1 open; zone 2-6 binnenkort" |
| Luchtschipper | `/execute in guhs:vadsrijk run tp @s 86 65 493` | (zone 1 klaar, survival, geen op) "Het schip naar De Splinterpieken vaart binnenkort!" |
| Lijn A-pier | `/execute in guhs:vadsrijk run tp @s 150 127 -650` | klik de pier/het bord: "Lijn A ... Het schip naar De Splinterpieken vaart binnenkort!" |
| Grens | `/execute in guhs:vadsrijk run tp @s 704 100 -1100` | (survival, geen op) je staat meteen terug in zone 1 / De Haven + melding |
| Zwamgrot | `/execute in guhs:vadsrijk run tp @s -412 69 148` | deur werkt: solo / groep / wachtrij (nep-spelers vullen) |
| Plafond | `/guhs vadsrijk level 19`, dan zonder op/creative een paar mobs | stopt op 20, balk vol, chatregel over sparen (ops hebben geen plafond) |
| Open alles | `/guhs vadsrijk zones open 6` | alles weer normaal; spaarpot betaalt uit |

Gametests: `runGameTestServer -Pgt=VadsrijkZonePoort,VadsrijkZwamgrotPoort`.

## XP-balans (zie XP_BALANS.md)
- Zone 1 alleen-hoofdquests: geen 70-90 min grinden meer maar 0-5 min (eindigt rond level 21-22, dus op het plafond 20).
- Zone 2 geeft veel minder (alleen-hoofd 20 -> ~32 i.p.v. 41), zone 3 meer (30 -> 50 echt in zone 3).
- Hoofd+zij haalt 70 nu in de tweede helft van zone 6 (z6_hq06/hq07) i.p.v. al in zone 5.
- `python tools/vadsrijk/xpbalans.py` toont alles (laatste tabel: de keten bij 0/2/5 kills onderweg).
