# SPIEK U-Z6MUZIEK: muziek en sfeer van het Zijdeland (zone 6)

Oud-Midden-Oosten, maar guh-zoet: oed (gitaar), kanun (hakkebord), ney (fluit), darbuka en riq, met speeldoos, harp en belletjes.
Het **karavaanlied van Oma Dadel** is het leidmotief: je hoort het in de duinen, 's nachts op de Zoutvlakte, in Rozenkloof, bij de
Moederoase, in de stingers en (gezongen door de hele oase) in de laatste fase van Ringeling.

> Smaak gaat voor. Zeg welk stuk en wat eraan schort ("te druk", "ander instrument", "te somber"). Elk stuk is één blokje in
> `tools/vadsrijk/geluid/zijdeland.py`.

## Snel alles horen
| Wat | Commando |
|---|---|
| Klikbare lijst van alles | `/guhs vadsrijk geluid` |
| Alles stil | `/guhs vadsrijk geluid stop` |
| Wat speelt hier en waarom | `/guhs vadsrijk geluid info` |

## 1. Gebiedsmuziek (met lange stiltes, zoals overal)
| Gebied | Commando | Wat je hoort |
|---|---|---|
| De Roze Duinen | `/guhs vadsrijk geluid gebied roze_duinen` | ney over een wiegende oed, zachte darbuka; in het midden het karavaanlied op de speeldoos |
| De Palmenkloven | `/guhs vadsrijk geluid gebied palmenkloven` | huppelende 6/8 met kanun en oed, harp als stromend water |
| De Zoutvlakte (dag) | `/guhs vadsrijk geluid gebied zoutvlakte` | trage wals, kanun en celesta, glinsterende belletjes |
| De Zoutvlakte ('s nachts) | `/guhs vadsrijk geluid gebied zoutvlakte_nacht` | ademende ney, het karavaanlied op de speeldoos, sterrenpad |
| Het Ruïnedal | `/guhs vadsrijk geluid gebied ruinedal` | stil en geheimzinnig maar lief: kanun, vibrafoon, zacht koor |
| De Moederoase | `/guhs vadsrijk geluid gebied moederoase` | warm en groots: het karavaanlied bloeit open (ney, dan viool), koor, harp |

In de wereld (zodra zone 6 erin zit): loop een gebied in, na 5-15 s begint het thema. Op de Zoutvlakte kiest hij dag of nacht vanzelf
(test: `/time set night`, dan `/guhs vadsrijk geluid nu`). Bij de grauwe toren is er GEEN muziek, alleen een lage zuigende bromtoon.

## 2. Stadsdeuntjes
| Stad | Commando | Wat je hoort |
|---|---|---|
| Rozenkloof 1 (Rozenplein) | `/guhs vadsrijk geluid stad rozenkloof_1` | vrolijk aankomstdeuntje, speeldoos + kanun |
| Rozenkloof 2 (echo in de kloof) | `/guhs vadsrijk geluid stad rozenkloof_2` | ney met echo tussen de rotswanden |
| Saffraanpoort 1 (de Grote Bazaar) | `/guhs vadsrijk geluid stad saffraanpoort_1` | druk en vrolijk: tokkelende mandoline, klarinet, darbuka + tamboerijn |
| Saffraanpoort 2 (paleis en tuin) | `/guhs vadsrijk geluid stad saffraanpoort_2` | sierlijke hofwals, kanun en fluit, fontein van celesta |

## 3. Bazen: thema, laatste fase, fanfare
Per baas drie commando's:
- `fase <baas>`: het thema, na 12 s zwelt de laatste fase erin;
- `baas <baas>`: alleen het thema;
- `fanfare <baas>`: de overwinningsfanfare.

| Baas | Laatste fase | Commando's |
|---|---|---|
| Koningin Wormelien (veldbaas) | snellere darbuka, koor, kanunloopjes | `/guhs vadsrijk geluid fase wormelien` · `/guhs vadsrijk geluid fanfare wormelien` |
| De Zoutkolos (veldbaas, nacht) | zingende kristallen, koor | `/guhs vadsrijk geluid fase zoutkolos` · `/guhs vadsrijk geluid fanfare zoutkolos` |
| De Astrolabiumwachter (kerker) | klokwerk wordt drukker | `/guhs vadsrijk geluid fase sterrenwacht` · `/guhs vadsrijk geluid fanfare sterrenwacht` |
| Kootje Glimmertje (kerker) | sluipmuziek wordt opgejaagd | `/guhs vadsrijk geluid fase glimmertje` · `/guhs vadsrijk geluid fanfare glimmertje` |
| De Verzonken Sterrenguh (kerker) | koper + koor + bellen (de bundels komen samen) | `/guhs vadsrijk geluid fase verzonken_sterrenguh` · `/guhs vadsrijk geluid fanfare verzonken_sterrenguh` |
| De Zandwachter (raid 1) | de zandloper tikt dubbel zo snel | `/guhs vadsrijk geluid fase zandwachter` · `/guhs vadsrijk geluid fanfare zandwachter` |
| De Spiegelvizier (raid 2) | de zaal breekt: rinkelend kristal, bekkens | `/guhs vadsrijk geluid fase spiegelvizier` · `/guhs vadsrijk geluid fanfare spiegelvizier` |
| De Gedoofde Sterrenbeelden (raid 3) | het steek-aan-venster: gespannen strijkers | `/guhs vadsrijk geluid fase sterrenbeelden` · `/guhs vadsrijk geluid fanfare sterrenbeelden` |
| Hertogin Ringeling (raid 4) | Ringdans: de hele oase zingt het karavaanlied | `/guhs vadsrijk geluid fase ringeling` · `/guhs vadsrijk geluid fanfare ringeling` |

Let op bij Ringeling's fanfare: eerst nog even boos (een chagrijnig trompetloopje), dan een harp-glissando en het karavaanlied
in zoet majeur. Zo wordt ze opgevrolijkt.

In de raid zelf (als u-z6raid erin zit): de muziek wisselt niet van stuk per fase. Hetzelfde thema loopt door en de laatste fase
zwelt er naadloos in. Bij de Sterrenbeelden zwelt hij in tijdens het steek-aan-venster en zakt daarna weer terug.

## 4. Stingers
| Wat | Commando |
|---|---|
| Sterrenbaken aangestoken | `/guhs vadsrijk geluid stinger baken` |
| Moederoase gevonden | `/guhs vadsrijk geluid stinger moederoase` |

De Moederoase-stinger speelt vanzelf de eerste keer dat je De Moederoase binnenloopt (in plaats van de gewone ontdekkingsstinger).
De baken-stinger moet nog aan het aansteken van een baken gekoppeld worden (verhaal-slice).

## 5. Sfeer (zacht, geen harde wind)
| Bed | Commando | Wat je hoort |
|---|---|---|
| Duinen | `/guhs vadsrijk geluid bed duinen` | heel zacht zandgeruis, verre bultguh-belletjes; GEEN wind |
| Oase | `/guhs vadsrijk geluid bed oase` | kabbelend water, vogeltjes, druppels |
| Bazaar | `/guhs vadsrijk geluid bed bazaar` | ver geroezemoes, tinkelende kopjes (bed van Saffraanpoort) |
| Sterrennacht | `/guhs vadsrijk geluid bed sterren` | krekels en twinkelende sterretjes (overal in het Zijdeland 's nachts) |
| Toren | `/guhs vadsrijk geluid bed toren` | lage zuigende bromtoon bij de grauwe toren |

Wat moet je horen:
- Overdag in de duinen zo goed als stilte met af en toe een belletje.
- 's Nachts krekels en sterrengetwinkel, en soms een zingend zoutkristal.
- Is het zandgeruis toch te veel? Zeg het, dan gaat het nog zachter.
