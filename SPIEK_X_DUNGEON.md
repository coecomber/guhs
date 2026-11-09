# SPIEK X-DUNGEON: naamborden bij de dungeondeuren (ronde X)

Overal: kom aan, kijk naar de deur. Boven de deur (1 blok naar buiten) hangt een bord van twee regels:
**grote naam** (vet, in de kleur van de dungeon) en daaronder klein **"Dungeon · Grot · lv A-B"**.
Het bord draait mee met je kijkrichting, is ook 's nachts/in de grot fel (volle helderheid, donkere achtergrond, schaduw) en
is van ~16+ blokken goed te lezen. Er hangt een onzichtbaar lichtblok bij het bord (de deur is 's nachts verlicht) en binnen
24 blokken zie je af en toe zachte gloei-vonkjes op de deur.

Survival-test aanbevolen: `/gamemode survival` (creative mag ook; de borden zijn er voor iedereen).

| Dungeon | Teleport (staanplek, kijkt naar de deur) | Wat je ziet |
|---|---|---|
| De Zwamgrot (zone 1) | `/tp @s -412 69 148 90 0` | "De Zwamgrot" + "Dungeon · Grot · lv 11-14" boven de deur in de paddenstoelsteel |
| De Kristalgrot (zone 2) | `/tp @s 869 119 -1104 -135 0` | bord in de tunnel, 2-3 blokken voor de deur, net onder het plafond |
| De Wortelgrot (zone 3) | `/tp @s 1410 69 -737 0 0` | bord boven de deur in de wortelstronk (2 blokken naar buiten) |
| De Koraalgrot (zone 4, hoofddeur) | `/tp @s 2230 71 -596 -135 0` | bord boven de deur in de koraalschoorsteen op het atol |
| De Koraalgrot (zone 4, zeebodem) | `/tp @s 2270 52 -572 90 0` (onder water) | dichte koraalpanelen ZONDER klopper; bord "De Koraalgrot" + "Dichtgegroeid · de deur is boven op het atol". Rechtsklik: chatbericht dat de deur boven op het atol zit, GEEN venster |
| De Wolkengrotten (zone 5) | `/tp @s 2494 205 -520 90 30` | bord zweeft ~2 blokken boven het luik (je staat erop); van de wenteltrap erboven goed leesbaar. Het luik is nu 22 luikblokken + 6 traptreden (samen helemaal dicht) |
| De Zingende Zoutgrot (zone 6) | `/tp @s 3874 50 -521 180 0` | bord op hoofdhoogte in de trapkoker, 1 blok voor de deur |

(De level-getallen komen uit de dungeon zelf: laagste level t/m het level van de eindbaas.)

## Andere fixes
- **Dungeondeur als item** (alleen creative/JEI): `/give @s guhs:vr_dungeondeur` of zoek "Dungeondeur" in de Guhs-tab / JEI.
  Je ziet de zwam-deur met klopper; tooltip "De dichte deur van een dungeon in Het Vadsrijk." + "Alleen voor bouwers: zelf
  neergezet doet hij niets." Niet te maken of te vinden in survival.
- **Blauwe stadsblubs gloeien weer zacht** zoals de echte blauwe Blub: ga naar een stad met Diertjes/de Wandelaar met zijn
  blubje (bijv. Zwamzoet) en kijk 's nachts of in de schaduw: het lijfje gloeit zachtjes.
- **Bord komt terug**: `/kill @e[type=text_display,tag=guhs_vr_dungeonbord,distance=..10]` bij een deur: binnen 2 s staan beide
  regels er weer (en nooit dubbel, ook niet na uitloggen/inloggen of wegvliegen en terugkomen).
