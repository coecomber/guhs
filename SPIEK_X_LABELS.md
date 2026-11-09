# SPIEK X-LABELS: geen ruwe sleutels meer

Wat je zag: een guh met als naam `vadsrijk.npc.naam.kk_poort_zwamzoet`. Dat was de Kwartiermeester van Zwamzoet: die wordt pas
neergezet als je in de buurt komt, en alleen de twee van zone 6 hadden een naam.

## De Kwartiermeesters (naamkaartje boven hun hoofd)
Ze verschijnen een paar seconden nadat je bij het plein bent, 6 tot 13 blokken van het midden.

| Stad | Commando | Wat je ziet |
|---|---|---|
| Zwamzoet | `/tp @s 0 66 59` | **Kwartiermeester Lamelletje** |
| Kandijkom | `/tp @s 704 93 -1068` | **Kwartiermeester Klontje** |
| Honingtak | `/tp @s 1396 169 -525` | **Kwartiermeester Wasje** |
| Lampionhaven | `/tp @s 1848 69 -467` | **Kwartiermeester Lontje** |
| Hoogvads | `/tp @s 2748 221 -529` | **Kwartiermeester Torentje** |

(Zone 6 had ze al: Kwartiermeester Rotsdak in de Rozenkloof, Kwartiermeester Koepeltje in Saffraanpoort.)

Staat er nog een oude guh zonder naam? `/guhs vadsrijk huis poorten` zet de Kwartiermeesters opnieuw neer.

## Het vangnet
Mist er ooit nog een tekst voor een Vadsrijk-sleutel, dan laat het spel nooit meer de sleutel zelf zien:
- eerst de algemene naam van dezelfde soort (een Kwartiermeester van een onbekende stad heet dan gewoon "Kwartiermeester");
- anders het id leesbaar gemaakt (bijv. "Iets heel raars").
In de log staat dan `Guhs: missing text ...`, zodat we hem kunnen toevoegen.

Wat je moet zien: nergens in het Vadsrijk nog een naam of regel met puntjes erin zoals `vadsrijk.iets.iets`.
