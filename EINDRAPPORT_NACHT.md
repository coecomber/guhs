# Eindrapport nachtronde T (2026-11-05/06): Het Vadsrijk, zone 1 t/m 5 + alle systemen

**Testclient:** `guhs_vadsrijk\t-int\run-client.bat` (branch `110-t-int`). Maak een nieuwe wereld.
**Testen:** `SPIEKBRIEF.md` (lang, alle commando's) en `TESTEN_VADSRIJK.md` (de gids).
**Niets is gepusht, gepubliceerd of op de server gezet.**

## In één alinea
Alle 25 nachtslices zijn samengevoegd in `110-t-int`. De wereld hangt nu aan elkaar (Knuffelkamp → zone 1 → 2 → 3 → 4, zone 5 boven
zone 4) met zee, zachte barrières en luchtschepen op een dienstregeling. Zone 4 "De Lampionkust" en zone 5 "Hoogvads" zijn speelbaar
met verhaal, wezens, bazen en hun finale (De Grauwe Droom). Het levelplafond is 60. Daarbij komen alle besloten systemen: karakters,
opslag, kledingkast, rijdieren, items, ambachten, puzzels, dialoog, geluid, modellen en effecten. De kaart is opnieuw gegenereerd en
alle loop-checkers zijn gedraaid. **Gametests: 243 Vadsrijk-tests, 239 groen in de volledige run; de 4 andere (grotSpawnerVindtDeVloer,
spawnerCapsEnVeiligeStraal, klodderreusVecht, opperzwamVecht) zijn bekende volgorde-afhankelijke flakes en slagen los (12/12).**

## Wat er per slice gebouwd is
| slice | wat | tests (slice) |
|---|---|---|
| t-wereld | wereldindeling W-3: zones verschoven (kamp -384, zone 2 +704/+1344), natuurlijke kusten, de Splinterwand, zee tussen de zones (ZeeVulling), zachte barrières, havens met vaste id's, uitkijkpunten | 16/16 (op 1 oude na) |
| t-dialoog | het nieuwe praatscherm: typemachine, stemmetjes, emoties, logboek, "Wat moet ik doen?" | 12/12 |
| t-geluid | 20 gebiedsthema's, 19 baasloops, stingers, omgevingsgeluid, volumeschuifjes (+22 MB) | 4/4 |
| t-models | modelkit v2: 50 mobs opnieuw, de Grauwe Klodderreus als GeckoLib-model, de Vahoegnir mooier | 9/9 |
| t-vfx | grond-telegraphs (decals), eigen deeltjes, cast-balk, de bazenbouwdoos: 30 nieuwe aanvallen voor 15 bazen | 26/27 (koningVecht flaky) |
| t-z4boot | het zeilbootje (rijdier op water), de Kringstroom, steigers, wind, uiterlijk (zeilen, wimpels, boegbeelden) | 14/14 |
| t-z4wereld | zone 4: 104 tegels, Lampionhaven + 8 eilanden, 43 interieurs, 9 kampvuren, 9 geheimen, mistbank, 371 plekken | 4/4 |
| t-z4wezens | 10 zone-4-wezens, de Grauwe Zeeslurf, het Wrak van de Vliegervanger, 2 sets, 2 huisdieren | 8/8 |
| t-z4verhaal | "De Grote Knuffeltij": 7 hoofdquests, 16 zijquests, 48 NPC's, geheimen, boekpagina | 10/10 |
| t-z4dieren | Ouwe Dobber (rijdbare reuzenschildpad die rondjes zwemt) + 17 guh-vliegers | 4/4 |
| t-z5wereld | Hoogvads op Moeder Wolkwal: 88 tegels, 30 eigen blokken, wijken, interieurs, luchthaven, het Wolkvangnet | 4/4 |
| t-wolkwal | het Wolkwacht-riddermodel, wuivende vinnen en staart, spuitgaten, panorama's, bovenverdiepingen | 11/11 |
| t-z5verhaal | "Verraad in de stad": 10 hoofdquests (speuren, 7 verhoren), 11 zijquests, 48 NPC's | 4/4 |
| t-z5wezens | Proefarena, Buikgangen (Gorgonzola), Wolkrand (Donderbuik), Schuilnest (spion Kootje Zilverstaart), Wolkwachtset | 9/9 |
| t-droom | de finale: De Grauwe Droom op de Droomwolk, 3 legendarische wapens, slaapmuts, huisdier Droomwalvisje | 11/11 |
| t-imker | geheime baas Mika de Imker (3 fases) in een grotere raatzaal, huisdier Zoempje | 3/3 |
| t-karakter | 3 karakterslots, karakterscherm, spelmodi Normaal / Eénvads / Knuffelmodus, de Rustende Ridders-zaal | 21/21 |
| t-opslag | knapzak (I), materiaaltas + kluis (account), HUD: buff-rij, doelframe, cast-balken | 5/5 |
| t-kleding | kledingkast (O): vleugels met een beweegtruc, capes, hoofddingen, transmog, verf, effecten | 8/8 |
| t-rijdier | rijdieren aankleden (vacht, patroon, tuig), 3 nieuwe rijdieren, 2 Pluimguh-kleuren, huisdierrollen | 18/18 |
| t-items | ring 2, armband, handschoenen, cape; 4 nieuwe stats met duidelijke namen; Erfstukken; smid-tabs; drankjesriem | 14/14 |
| t-ambacht | 9 ambachten (verzamelen, imkeren, koken, brouwen, smeden, naaien), maatwerk, bestelbord, ranglijst | 11/11 |
| t-puzzels | 22 puzzels en geheimpjes in zone 1-3 (8 soorten, 19 blokken), 9 Keizer-bladzijden | 13/13 |
| t-luchtschip | luchtschepen op dienstregeling (lijnen 0, A-D), vertrekborden, luxe reizen, verre schimmen | 5/5 |
| s-site | guhs.nl/vadsrijk lokaal (kaart, wiki, highscores, live-export); NIET gepubliceerd | 3/3 |

## Wat de integratie (t-int) zelf deed
- **Samenvoegen:** 25 branches; taalbestanden als sleutel-unie; FEATURES-volgorde bewaakt (t-vfx als laatste).
- **Koppelingen:**
  - Klodderreus-model ↔ VFX-animaties;
  - zeilbootje ↔ de echte steigers en de Kringstroom van zone 4;
  - het bootje als soort in de nieuwe rijdier-API;
  - ambachten ↔ materiaaltas;
  - één cast-balk;
  - de drankjesriem op de gereserveerde HUD-plek;
  - karakters ↔ opslag, rijdier-uiterlijk en bootje;
  - Eénvads mild in de Proefarena en tijdens scènes;
  - luchtschiphavens ↔ de echte havens van zone 4/5;
  - verhaal zone 4 ↔ de kaart van zone 4 (68 plek-namen gekoppeld);
  - zone 5 volgt na zone 4 (z4_hq07);
  - vissen in zone 4 telt mee voor het verhaal;
  - het Ambachtsgilde in Hoogvads opent de werkplaatsen;
  - bankiers in Lampionhaven en Hoogvads;
  - beloningen Lampion-Pluimguh en Duinhupper;
  - lampion-cosmetica via het zone-4-boek;
  - geen zweven in kerkers;
  - muziek voor de Grauwe Droom;
  - ambacht-highscores via de website-export;
  - blokuitleg voor de 19 puzzelblokken.
- **Levelplafond 60** (generiek + klasse; dezelfde curve loopt door), LV_CAP 60 in de zone-4/5-verhalen.
- **Toetsen:** de drankjesriem naar Alt+1/2/3 (NeoForge-modifier: Alt+cijfer kiest géén hotbar-vak); Y = Ambachten, I = Knapzak, O = Kledingkast.
- **Kaart/fouten gevonden en opgelost:**
  - zone 4 had nog een vast blokken-luchtschip waar het echte schip aanlegt (weg);
  - het noordvin-platform zweefde op y 244 (nu op de vin);
  - de walvis-kamers 20 lager;
  - de vlieger-finish was onbereikbaar (verplaatst);
  - de barrière van zone 4 dekt alle eilanden maar laat de Wolkenval een barrière;
  - de luchtschiproutes A, B en D vlogen door pieken en de walvis (opnieuw gelegd, alle lijnen ≥ 10 blokken vrij, heen en terug; check-script `tools/vadsrijk/luchtschip_check.py`);
  - NL/EN stonden omgewisseld in 12 generatoren bij een volledige `make_resources`-build;
  - 20 wereldteksten waren bij een merge weggevallen;
  - de zone-4-verhaalgebieden overschaduwden die van de kaart.
- **FTB:** CHAPTER_VERSION 63 (zone-4/5-secties in het Vadsrijk-hoofdstuk).
- **Website:** kaarttegels opnieuw (nu alle 5 zones), site lokaal opnieuw gebouwd (zie onder).

## Tests
- Volledige suite: `runGameTestServer -Pgt=Vadsrijk,Z2Npc,Z3Npc,Z4Verhaal,Z5Verhaal,Z5Wereld`: **243 tests, 239 groen**. De 4 rode
  (grotSpawnerVindtDeVloer, spawnerCapsEnVeiligeStraal, klodderreusVecht, opperzwamVecht) zijn volgorde-afhankelijk en slagen los
  (`-Pgt=VadsrijkBazenGevecht,VadsrijkWezens`: 12/12). Ze wisselen per run (ook koningVecht en opperzwamFasen doen dit soms).
- Onderweg gevonden en gerepareerd door de volledige runs:
  - de Wolkenval-barrière;
  - de dps-test met de wapens van zone 4/5;
  - de test van het levelplafond;
  - zone 5 na zone 4;
  - de zone-4-gebieden;
  - de Droomwolk zonder muziek;
  - de volgorde van de gevechtsgebieden in zone 5.
- Website: `python tools/make_vadsrijk_site.py`: 677 pagina's, 0 kapotte links (17.579 links + 2.704 plaatjes), met de nieuwe
  runtime-dump en kaarttegels van alle 5 zones. Alleen lokaal (`site_build/vadsrijk`); niets gepubliceerd. Let op: de site leest de
  quests nog alleen van zone 1-3 (90 quests); zone 4/5 komen er pas in als `tools/wiki_site/vadsrijk/bron.py` hun bestanden leest.

## Checks na de laatste kaart-regen
| check | uitslag |
|---|---|
| audit.py (zone 1 + kamp) | trappen los 0, deuren 0, onder de kaart 0; kuilen 4 + padstapjes 5 = de bekende oude (kampvuurtoren-kelder, Zwamzoet-daken, Druipgrotten, tempelvoet) |
| controleer_verticaal (Hoedenpaden + Druipschacht) | 0 fouten |
| loopcheck.py zone 2 | VAL 30 / TRAP 46 = basislijn (de TRAP's zijn stoelen en bankjes) |
| loopcheck3 + poicheck3 (zone 3) | PAD 396 (basislijn), 0 van 430 plekken niet sta-baar; 1 VAL = de raatzaal van de Imker (het luik boven de ladder telt de checker niet: in het spel klik je het luik open) |
| loopcheck4 + poicheck4 + audit4 (zone 4) | 0 bevindingen, 0 van 371 plekken niet sta-baar |
| loopcheck5 (zone 5) | VAL = onder de stad (het Wolkvangnet vangt je), TRAP = zitbanken en dakranden, zoals t-z5wereld meldde |
| luchtschip_check (alle lijnen) | A 22, B 21, C 13, D 24 blokken vrij; lijn 0 staat uit (nog geen eigen havens) |

## Bekende problemen en wat NIET af is
- **AutoCheck-wandeling** (`tools/autocheck/vadsrijk_t_int.txt`, 13 schermafdrukken in `run/screenshots/autocheck/ti_*.png`):
  - alle zones laden;
  - Lampionhaven, de luchthaven, de steiger, het Vliegereiland en de Walvisrughaven zien er goed uit.
  - Met de hand nakijken:
    - de schermafdruk van de Noordklifhaven kijkt tegen de klif aan. Waarschijnlijk alleen de kijkrichting, maar loop de pier even af;
    - de noordvin-plek (z5w_noordvin) ligt nu onder de overhang van de walvis, met Wolkduivels eromheen. Kijk of dat de bedoeling is
      voor de Wolkrand-gevechten.
- **Niet gedaan door t-int:**
  - de veilige plekken (unstuck) van zone 4 en 5 staan niet in de gedeelde json;
  - `WolkwalRegistry.BOVEN` (bovenverdiepingen) is niet apart nagelopen, alleen met loopcheck5;
  - de website leest de quests van zone 4/5 nog niet.
- **Niets is in een echte client bekeken door een mens:** uiterlijk van alle nieuwe modellen, decals, vleugels, muziek (nog nooit
  beluisterd), zeilen-gevoel, zweven, het schildpadritje, de vliegers en het luchtschip.
- **Drankjesriem op Alt+cijfer:** volgens de NeoForge-code kiest Alt+1 géén hotbar-vak; nog niet met de hand geprobeerd. Lukt het
  niet goed: terugval = riem op Y/I/O, Ambachten op M, Knapzak/Kledingkast op [ en ].
- **Grauwe Droom:** speelt het Vahoegnir-thema; een eigen slaapliedje-baasloop is niet gemaakt.
- **Lijn 0** (kamp ↔ De Haven) rijdt niet: de havens hebben nog geen eigen steiger (de oude scènevlucht blijft werken).
- **Job-advancement op lv 60** bestaat niet (de 4e stap blijft op 50); boven 50 krijg je gewoon skillpunten.
- **Zone 4/5 in het Ambachtenboek:** de nieuwe vis-plekken van zone 4 staan niet in het boek-overzicht (wel in de wereld). Honing
  in zone 4/5, Meesterschort, Knutselpenningen-winkel en spelersbestellingen ontbreken (t-ambacht).
- **Oude kaartgaten zone 1:** 5 padstapjes van 2 blokken en 2 dakkuilen in Zwamzoet (al lang bekend, niet aangepakt).
- **Imker-raatzaal:** de checker ziet het luik als een stap van 2; in het spel werkt het (luik open klikken).
- **t-items:** de inventaris-skin (iNv) en de website-export van Erfstukken.
- **t-rijdier:** het huisdierportret in de HUD en huisdiertalenten.
- **t-opslag:** loadouts, rommel markeren, affixen in het doelframe.
- **t-kleding:** armour-verf (de Vadsrijk-pantsers zijn niet verfbaar).
- **t-luchtschip:** het dek is niet beloopbaar; de zone-3-film bij aankomst via lijn B.
- **t-z4verhaal:** knuffeldeco-blokken.
- **t-z5verhaal:** wiki-pagina's voor zone 5; klasse 4 (F3) is alleen een teaser.
- **Website:** alleen lokaal gebouwd; publiceren (pages.yml, live.guhs.nl, Caddy/DNS) wacht op jouw OK.
- **De oude POI_VERZOEKEN*.md** in `guhs_vadsrijk/` hebben nog oude zone-2-coördinaten (+704, +1344 erbij tellen).
- **Ouwe Dobber** (schildpad) is niet in een client gereden; het ritje met meerdere spelers is niet getest.

## Hervatten
- Branch `110-t-int`; gegenereerde kaartbestanden zijn in deze boom gecommit.
- Opnieuw genereren: `python tools/vadsrijk/maak_kaart.py --zone alle --geen-cache --audit`. De volgorde is 1, 2, 3, 4, 5; zone 5 moet
  na zone 4, door de gedeelde tegels.
- Generatoren: elk `tools/features/vadsrijk_t_*.py` los in FEATURES-volgorde, daarna
  `python tools/vadsrijk/wolkwal/maak_modellen.py --sparring-over`. Let op: t-z5wezens' generator overschrijft de sparringridder.
- Tests: `runGameTestServer -Pgt=Vadsrijk,Z2Npc,Z3Npc,Z4Verhaal,Z5Verhaal,Z5Wereld`.
