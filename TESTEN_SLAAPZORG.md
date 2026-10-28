# Slaapzorg testen (1.8.0, deel B)

Starten: `run-client.bat` in deze werkmap. Alle commando's werken op de guhs binnen 16 blokken (op = permissieniveau 2).

| Commando | Wat |
|---|---|
| `/guhs slaapzorg spullen` | Eén van elk slaapzorg-blok: muziekdoosje, mobiel, nachtkastje, droomvanger, slaapmasker-kapstok, 4 maannachtlampjes, 2 krabpalen |
| `/guhs slaapzorg moe` | Guhs gapen en knikkebollen meteen, zoals 's avonds |
| `/guhs slaapzorg liedje` | Het slaapliedje hier: guhs gapen een voor een en vallen daarna in slaap |
| `/guhs slaapzorg slaap` | Meteen in slaap op hun plek (avond/nacht: tot de ochtend, overdag: een dutje van een minuut) |
| `/guhs slaapzorg wakker` | Iedereen wakker, met een geeuw |
| `/guhs slaapzorg plekjes` | Hoeveel slaapplekjes (`#guhs:guh_slaapplekken`, zonder nestjes) er zijn en hoeveel guhs slapen |

Handig: `/time set 12000` (avond: moe), `/time set 14000` (nacht: slaapplekjes), `/time set 23200` (ochtend: ontbijt op bed).

## Wat je kunt proberen
- **Moe**: 's avonds (`/time set 12000`) gapen guhs af en toe, knikkebollen ze even (hoofdje zakt) en zie je een losse zzz.
- **Slaapplekjes**: 's nachts zoekt een guh zelf een slaapplekje uit de tag op (nu: het guhbedje; agent A voegt de doos met knabbels en het kattenhuisje toe) en kruipt erin. Hij slaapt uit tot de dag begint.
- **Muziekdoosje / Mobiel**: rechtsklik = het Blub-slaapliedje (~24 s). Guhs tot 8 (mobiel: 6) blokken gapen en vallen in slaap. Het mobiel draait altijd rustig, en sneller als hij speelt.
- **Ontbijt op bed**: 's ochtends een slapende guh rechtsklikken met kaasknabbels (of een andere guh-snack): hij rekt zich uit, smikkelt, hartjes, en is één keer per dag "uitgerust". 's Nachts: "Sst... nog even slapen".
- **Nachtkastje en maannachtlampjes**: rechtsklik = lampje aan/uit.
- **Krabpaal**: tamme guhs in de buurt hupsen er overdag af en toe bovenop en blijven even zitten (zwaaien naar je).
