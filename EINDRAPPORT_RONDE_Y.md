# Eindrapport ronde Y (Guhs 1.10.0 "Het Vadsrijk"): Vadsrijk ↔ survival, klaar voor de release met alleen zone 1

Testclient: **`guhs_vadsrijk\y-int\run-client.bat`** (tak `110-y-int`). Nieuwe wereld maken; de client staat op alle zones
open en heeft FTB Essentials (zoals de server). Spiekbrief: `SPIEKBRIEF.md` (hoofdstuk 1 = ronde Y). Afvinklijst:
`TESTEN_RONDE_Y.md`. Releasestappen: `RELEASE_CHECKLIST.md` (elke stap wacht op jouw OK).

## Wat er in zit
| Slice | Wat |
|---|---|
| y-ftb | het compacte FTB-hoofdstuk "Het Vadsrijk" (rekruut, zone 1-6, raid, geheimpjes 1-6; zones achter de poort verborgen), 14 pronkstukken als beloning, claimen nooit in het Vadsrijk |
| y-vitrine | de Vadsrijk-vitrine: trofeekastje voor je basis met je rijdier als beeldje en een bordje (naam, klasse, level, zone-badges) |
| y-scheiding | 14 lek-kanalen getest en dichtgezet: inventaris, enderkist, xp, effecten/hartjes/honger, dieren, teleports, opslag, survival-beloningen binnen |
| y-cosmetica | de Garderobe: je Vadsrijk-kleding thuis als looks (geen krachten), vrijspelen alleen binnen, geen spoilers bij poort 1 |
| integratie | zie hieronder |

## Wat de integratie zelf deed
- Alles gemerged; de taal-conflicten sleutel voor sleutel. De vitrine-beloning van de rekruut-quest is nu een echt item.
- **Jouw besluit a: Vadsmunten kopen thuis alleen Vadsrijk-deco en kleding.**
  - Mollie verkoopt nog 7 dingen: Guh-stoel, Guh-tafel, Guh-bank, roze zitzak, magenta kussen, vlaggetjes, Vadsrijk-vaandel.
  - **Weg bij Mollie:** Guh-taart (eten), Roze guhbloem en Guhoortjes (bloemen), lantaarns, roze tapijt, roze bed, bloempotten,
    kersenplanken, roze glas, boekenkasten.
  - **De Post:** alles wat hij bracht bleek al deco of kleding (ambacht-hoedjes en -schortje, hartjesbril, Vadsrijk-kleding,
    vaandel, knuffel, beeldjes, het guh-vliegertje). Er ging dus niets af, maar er zit nu een slot op: iets anders gaat nooit
    per Post naar huis (het blijft dan in je Vadsrijk-tas).
  - Wat spelers al gekocht hadden, houden ze. Nog wachtende Post wordt gewoon bezorgd.
  - Eigen keuze: de Guhs-meubels bleven (pure huisdeco); bloemen en taart gingen (die hebben survival-nut). Zeg het als je dat anders wilt.
- **Jouw besluit b: geen FTB-werkbanken in het Vadsrijk.** Vondst: FTB Essentials 26.1.2.4 heeft geen `/anvil` maar
  `/open anvil|crafting|smithing|stonecutter`, en die mogen alleen ops. Binnen worden ze geweigerd (plus de losse namen voor
  later). Er is ook geen `/ec`, alleen `/enderchest` (dat was al dicht).
- **Wiki:** "Zo werkt het" voor de vitrine en de 14 pronkstukken. De vitrine kreeg een eigen itemmodel, anders had hij geen
  wikipagina. Pronkstukken van dichte zones verschijnen niet op de wiki (hun namen noemen zones).
- **`GUHS_OPEN_ZONES` volgt nu vanzelf `--open-zones`** van de wiki-build. Ze kunnen dus niet meer verschillen.
- **Vondst voor de release:** NeoForge 26 leest de zone-poort uit `config/guhs-vadsrijk-zones.toml` (of de
  `serverconfig/` van de wereld). `defaultconfigs/` doet niets meer. RELEASE_CHECKLIST stap 5 is daarop aangepast.
- **Drie blokken draaiden niet mee met gebouwen:** de vitrine, het Vadsrijk-portaal en de lichtbron van de Spiegelburcht.
  Opgelost.
- **De patchnotes 1.10.0** gebruikten plaatjes die y-ftb had verplaatst. Ze wijzen nu naar de nieuwe plek.

## De echte grens (AutoCheck in de client, poort 1): **42 PASS, 0 FAIL**
De gametest-server heeft geen Vadsrijk-dimensie, dus de grens is in een echte client gecontroleerd. Dat deed
`/guhs grenscheck` (een nieuw dev-commando), aangestuurd door `tools/autocheck/vadsrijk_y_int.txt`. Gecheckt als gewone speler:
| Kanaal | Uitkomst |
|---|---|
| inventaris | binnen weg, thuis terug |
| vanilla xp | 17 -> 0 binnen -> 17 thuis |
| effect thuis (Speed II) | binnen weg, thuis terug |
| effect binnen (Nachtzicht) | niet mee naar buiten |
| hartjes / eten | 13 en 11 precies terug |
| item bij de grens laten vallen | ligt nog thuis, nergens binnen |
| item-entiteit over de grens teleporteren | geweigerd, beide kanten op |
| `/enderchest`, `/open anvil/crafting/smithing/stonecutter` | binnen geweigerd, thuis werken ze |
| `/home`, `/back`, `/spawn` | binnen geweigerd |
| FTB-beloning (de vitrine) claimen | binnen geweigerd (niet gemarkeerd, geen item); thuis gelukt |
| vitrine en pronkstuk neerzetten | binnen niet, thuis wel (eigenaar = de speler) |
| Garderobe thuis | titel "Garderobe", 8 tabs, de looks-regel, de "meer later"-regel bij poort 1 |
| FTB-boek bij poort 1 | alleen rekruut, zone 1 en geheimpjes 1 |
| vitrine bij poort 1 | ① ? ? ? ? ?, geen raid-regel, Knuffelhert als beeldje |

Bewijs: `guhs_vadsrijk\y-int-autocheck-bewijs\` (grenscheck.txt + yi_*.png).

## Controles
| Check | Uitkomst |
|---|---|
| engelscheck `--alles` | 0 zonder Engels, 0 NL gewijzigd, 0 formaat, 0 verouderd, 0 en_us oud; `--merk --alles` 49.779 |
| taalcheck | 2.982 sleutels, 0 ontbreken |
| make_ftbquests + apply_en | geen verschil: 26 hoofdstukken, 1.758 quests, 0 nog Nederlands |
| compileJava | OK |
| kaart | geen wijzigingen deze ronde: tegels, gpak, WereldPlekken en POI's gelijk aan ronde X (geen regen) |
| wiki-build (poort 1) | 0 kapotte links; 209/209 doe-blokken met "Zo werkt het"; zone1check gelijk aan ronde X (alleen oude naamgenoten) |

## Tests
- **Volledige suite, de HELE mod** (2.778 tests; ronde X draaide 503 Vadsrijk-tests): 11 rood.
  - 1 kwam door ronde Y: de vitrine draaide niet mee. Opgelost, net als 2 oudere Vadsrijk-blokken.
  - 3 zijn de bekende volgorde-afhankelijke (Spiegelzucht, Piepvel, het verhaal-wolkje). Los slagen ze: Z2Bazen 6/6,
    Z3Bazen+Ruiters 8/8, Verhaal groen.
  - 7 zitten buiten het Vadsrijk en buiten alles wat deze ronde veranderde. Ze vallen ook los om:
    - Gids kussengevecht (2x);
    - Vadsschaar weidegras;
    - Wereld-/Techquest-bescherming (2x);
    - titel-hint vr_karavaanvriend;
    - SNOES-Guhdex.

    Ronde X draaide die tests niet. Zie "Open punten".
- **Gericht na de laatste fixes:** Y-INT + Scheiding + Vitrine + Draai + Z2Bazen: allemaal groen.
- **Questrobot:**
  - zone 1-6: 41/19/24/23/24/25 gehaald, 0 vast;
  - poort 6: hoofdverhaal 13/12/15/7/10/10;
  - poort 1: zone 1 hoofd 13/13, de rest 26/26.

## Website
- u-site bijgewerkt: de runtime-dump uit y-int; renders, iconen en kaart opnieuw gemaakt. De kaarttegels zijn ongewijzigd.
- Previews (gh-pages 3c0d41d8d):
  - https://guhs.nl/vadsrijk-preview/ : volledig, 946 pagina's, 0 kapotte links;
  - https://guhs.nl/vadsrijk-preview-zone1/ : de publieke release, 380 bestanden, **0 lekken**, 0 kapotte links; `zones/zone2.html` geeft 404.
- main en de live map /vadsrijk/ zijn niet aangeraakt.

## Wat jij nog moet bekijken
- De 14 pronkstukken (`/guhs vadsrijk ftb trofeeen`) en de vitrine. Kijk vooral naar het glas over het beeldje.
- De Garderobe thuis. Let op: de regel "Thuis is je Vadsrijk-kleding alleen voor de looks: geen krachten, geen harnas." valt
  rechts net buiten het lijstje (afgekapt na "geen").
- Mollies kortere winkel: wil je de bloemen of de taart toch terug?

## Open punten (bewust niet gedaan)
- **De 7 rode tests buiten het Vadsrijk.** Ze zijn niet door ronde Y gekomen: geen van hun bestanden is aangeraakt. Ronde X
  draaide ze niet, dus het is onbekend sinds wanneer ze rood zijn.
  - Gids kussengevecht;
  - Vadsschaar;
  - Bescherming;
  - titel-hint;
  - SNOES.

  Ze horen bij een eigen klusje vóór de release.
- **Oudere blokken zonder eigen itemmodel hebben geen wikipagina:** kermisrit_botsguhs, koorkom_podium, lanceerplatform enz.
  Dat gold al eerder; alleen de vitrine is nu opgelost.
- Een speler die zone 1 al af had vóór de vitrine bestond, krijgt als "eerste zone"-datum de dag van de eerste check
  (y-vitrine-keuze).
- "Alle geheimpjes" telt geen flessenpost/uitzichten/kampvuren (y-ftb; uit te breiden via `VrFtb.GEHEIM_CATS`).
