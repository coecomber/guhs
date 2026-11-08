# Spiekbrief: Het Vadsrijk na ronde U (zone 1 t/m 6 + alle systemen + al je feedback)

Dit is de lange spiekbrief van ronde U (2026-11-06 t/m 11-08): alles wat je in je feedback van ronde 4 vroeg, plus zone 6
"Het Zijdeland" met de eerste raid. Alles zit in één nieuwe testclient:
**`guhs_vadsrijk\v-int\run-client.bat`** (de oude t-int-client heb je niet meer nodig).

**Volgorde van deze spiekbrief:** eerst starten, de toetsen en de loot-regels, dan de nieuwe wereld, dan zone 1 → 6, dan de
systemen, en helemaal achteraan (hoofdstuk 8) **elk feedbackpunt** met wat er gedaan is en hoe je het controleert.
Elk blok test één onderdeel: copy-paste de commando's in de chat en kijk of gebeurt wat erbij staat.

## Starten
- Dubbelklik `run-client.bat`. Maak een **nieuwe** wereld (Creatief, cheats aan); de saves-map is leeg.
  De hele kaart (zone 1 t/m 6) is opnieuw gegenereerd, dus een oude wereld klopt niet meer.
- `/guhs vadsrijk` om erin te gaan. De eerste keer krijg je meteen het karakterscherm (3 vaste slots, zie §1).
- Wil je geraakt kunnen worden: `/gamemode survival` (in creatief vallen de wezens je niet aan).
- Alle teleportcommando's gebruiken plek-namen, dus die kloppen vanzelf.

## Handig tussendoor
- `/guhs vadsrijk vol`: Vads, energie en ult vol, cooldowns gewist.
- `/guhs vadsrijk info`: je data.
- `/guhs vadsrijk level 70`: het level-plafond is nu **70** (was 60). Zone 5 is lv 56-60, zone 6 lv 60-70.
- `/guhs vadsrijk tips reset`: alle tips opnieuw.
- `/guhs vadsrijk loot kansen`: alle rijdier-drops met jouw pech-teller (zie "Loot" hieronder).
- `/toetsen`: het toetsenoverzicht als los scherm (ook in het pauzemenu, rechts).
- Plekken: `/guhs vadsrijk wereld plek <id>` (zone 1), `z2wereld`, `z3wereld`, `z4wereld`, `z5wereld`, `z6wereld plek <id>`;
  Tab-aanvullen toont alle ids.

## Toetsen (na ronde U)
Alle Vadsrijk-toetsen werken alleen in het Vadsrijk. Je vindt ze in Opties > Besturing, categorie Vadsrijk, én rechts in het
pauzemenu (Esc). Een rode **!** daar = de toets botst met een andere.

| toets | wat |
|---|---|
| R / V / G | vaardigheid 1 / 2 / 3 |
| Z | ultieme vaardigheid |
| C | knuffelsprong |
| U | losknuffelen (vast? naar een veilig plekje) |
| **H** | **Vadsrijk-menu** (nieuw: alle tabs in één scherm; Ctrl+Tab bladert). Buiten het Vadsrijk blijft H de Vadskunst van het Guheinde. |
| K | skillboom (ook als tab in het menu; subtab Specialisatie) |
| J | questlog |
| B | Verkenningsboek |
| X | rijdier: op- en afstappen (zone 4-water: je zeilbootje; zone 6: tapijt / bultguh) |
| N | rijdieren-scherm |
| P | huisdieren-scherm |
| Y | Ambachten |
| I | Knapzak |
| O | Kledingkast |
| **`;`** | **emote-wiel** (nieuw, u-hud) |
| Alt+1 / Alt+2 / Alt+3 | drankjesriem vak 1 / 2 / 3 (je hotbar-vakje wisselt niet) |
| Spatie / Shift (op het tapijt) | stijgen / dalen |
| F9 / F10 | Erfenis (bovenaanzicht / kaart), buiten het Vadsrijk |
| (geen) | questvolger aan/uit, lichtstraal aan/uit: zelf een toets geven als je wilt |

## Loot: wie krijgt wat (je besluit V6)
- **Alles is persoonlijk.** Wezens/elites: elke helper rolt zelf, alleen de eigenaar kan de drop oprapen. Bazen: een eigen kist
  boven een bijdrage-drempel. Raid: een eigen kist per speler per clear. Grotten: de schatkist 1× per speler (de grot-kerker:
  een eigen kist per clear).
- **Rijdier-drops: OVERAL 10 % per speler per kill/clear**, met een **pech-teller**: na **15 keer mis** (per karakter, per bron)
  is de volgende poging zeker raak. Dus nooit meer dan 16 pogingen.
- **Zeldzame item-drops** (vleugels, huisdieren, Guhddelijke items) blijven persoonlijke worpen met hun eigen kans (tabel hieronder).
- **Dialoogkeuzes:** zijn er (nog) NIET, jouw besluit; alle gesprekken lopen recht door.

| bron | wat | kans |
|---|---|---|
| Opperzwam (Zwamtempel, zone 1) | Zwamzwijntje (rijdier) | 10 % + pech 15 |
| Spiegelzucht (zone 2) | Kristal-Pluimguh (kleur, als je een Pluimguh hebt) | 10 % + pech 15 |
| Einde van een Vahoeg-run (zone 3) | Gouden Pluimguh | 10 % + pech 15 |
| Sterrendovende Toren, clear (zone 6 raid) | Bultguh in Sterrennacht-vacht (krijg je de Bultguh nog niet: die erbij) | 10 % + pech 15 |
| Raid-clear | Sterrenzijde-setstuk / klassewapen | 1e clear: wapen zeker; daarna zie §6 |
| Raid-clear | Sterrenring-vleugels (Kledingkast) | 3 % |
| Raid-clear | Sterrenpluisje (huisdier) | 4 % |
| Raid-clear | Ringreplica (Knuffelbond-trofee) | 12 % (1e clear zeker) |
| Raid-clear | Ringelings Teenring (Guhddelijk) | 1,5 % |
| Raid-bazen 1-3 | kleine kist: munten + 30 % een setstuk | elke kill |
| Zone-4-zijquests z4_s14 / z4_s15 | Lampion-Pluimguh / Duinhupper | vaste beloning |

Testen:
```
/guhs vadsrijk loot kansen
/guhs vadsrijk loot pech opperzwam_zwamzwijntje 15
/guhs vadsrijk loot rol opperzwam_zwamzwijntje
```
Na `pech ... 15` is de volgende `rol` zeker raak (gouden chatregel + geluid). Een misser toont grijs "nog N tot zeker".

---
