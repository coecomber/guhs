# Eindrapport ronde U (2026-11-06 t/m 11-08): Het Vadsrijk na je feedback van ronde 4 + zone 6

**Testclient:** `guhs_vadsrijk\v-int\run-client.bat` (branch `110-v-int`). Maak een **nieuwe wereld**: de hele kaart is opnieuw gegenereerd.
**Testen:** `SPIEKBRIEF.md` (lang: Questlopen bovenaan, toetsen, loot, zone 1 → 6, systemen, elk feedbackpunt) en `TESTEN_VADSRIJK.md` (de testlijst).
**Niets is gepusht of op de server gezet.** De website is alleen lokaal opnieuw gebouwd. Het verborgen voorbeeld op
https://guhs.nl/vadsrijk-preview/ is van u-site (ronde 2/3); de hoofdagent werkt het bij met de nieuwe kaart.

## In één alinea
Alle ronde-U-slices (36 branches) zijn samengevoegd in `110-v-int`. Daarin zit elk punt uit je feedback van ronde 4:
- natuurlijke zone-overgangen;
- een epische zone 5 met gotiek, kroonluchters en tuinen;
- verbonden hekken overal;
- het hele luchtschip klikbaar;
- vaste karakterslots;
- zichtbare capes;
- geen dubbele spawns;
- deco, licht en bijen in Honingtak en het kamp.

Daarbij komen alle besluiten uit de vraagrondes: specialisaties, de Knuffelbond met kastelen, huizen in het Knuffelkwartier, een sfeergrot per zone (nu ook als herhaalbare mini-kerker), party-frames, emotes, de HUD-bewerker, één Vadsrijk-menu (H) en het toetsenoverzicht in het pauzemenu. **Zone 6 "Het Zijdeland"** (lv 60-70) is gebouwd: Rozenkloof, Saffraanpoort, De Sterrenkaravaan, 13 wezens, 2 veldbazen, een kerker, de Bultguh, het vliegende tapijt en de raid **De Sterrendovende Toren** voor 2-10 spelers. Het levelplafond is 70. Nieuw voor je verhaaltest is **Questlopen**: een admin-paneel om de hele hoofdlijn door te klikken. De quest-robot speelt alle 156 quests van zone 1-6 zonder vast te lopen. **Gametests: 368 Vadsrijk-tests in de volledige run; 2 echte fouten gerepareerd, de andere 4 slagen los (volgorde-afhankelijk).**

## Wat er per slice gebouwd is
| slice | wat |
|---|---|
| u-hekken | gedeelde verbind-pass (`tools/vadsrijk/verbind.py`): hekken, muren, ruiten, kettingen, slingers, vlaggetjes overal verbonden, ook over tegelranden; draait na elke regen |
| u-overgang | één open zee tussen alle zones, verlopende waterkleur/lucht/mist, rafelende wolkenzee, rotsjes; W3-uitkijkpunten op epische plekken |
| u-deco | kamppaden verlicht, Honingtak (licht, bankjes, prullenbakjes, bijen), wenteltrap + verdiepingsbordjes, arena-onderkant, `uitkijk_nest`, schildpadeiland, gondel zonder mobs |
| u-dubbel | dubbele spawns weg (sleutels per NPC/baas/elite, sweeper, `/guhs vadsrijk dubbel scan`) |
| u-zichtbaar | cape, handschoenen en sieraden zichtbaar voor jezelf en anderen |
| u-karakter | 3 vaste slots (Normaal / Knuffelmodus / Eénvads), eerste keer maken werkt, vindbaar via knop/tab/kampvuur, de Rustende Ridders-zaal in de wereld |
| u-luchtschip | hele schip klikbaar, vrij lopen op het dek, uitleg als een lijn nog dicht is |
| u-ruiters | 22 rijdende guhs op Pluimguhs (zone 3 trappen/Honingtak/stal, 2 in zone 1) |
| u-specs | nieuw advancement-model: lv 17 specialisatie, 35, 58; stam + 2 spec-takken + zijtak; gratis respec; ▲/▼ in de tooltip |
| u-hud | party-frames, emote-wiel (`;`), HUD-bewerker |
| u-tabs | één Vadsrijk-menu (H) met even grote hoofdtabs, alles erin |
| u-toetsen | toetsenoverzicht rechts in het pauzemenu, botsingen in rood |
| u-audio2 | bazenmuziek met fases, fanfare per baas, 5 nieuwe thema's, zachte wereldgeluiden, NPC-stemmetjes |
| u-ambacht2 | ambachten T4/T5 tot lv 60, koraalduiken, Hoogvads ambachtshoofdstad |
| u-gilden | de Knuffelbond: oprichten in Hoogvads, joinen vanaf zone 1, kastelen (niveau 1-5) in de Bondenvallei, weektaken, perks, `/bc` |
| u-huizen | Het Knuffelkwartier: kavels in straatjes, 1 huis per account, Kwartiermeester op elk plein |
| u-grotten | 1 sfeergrot per zone (Zwamgrot, Kristalgrot, Wortelgrot, Koraalgrot, Wolkengrotten), elk met ambacht, elite, puzzel, schat |
| u-grotkerker | elke grot (zone 1-6) een herhaalbare mini-kerker: run, poort, eindbaas, eigen kist per clear, grotkleur-Pluimguhs |
| u-z5, u-z5binnen, u-z5tuin | Hoogvads episch: gotische kathedraal/universiteit/paleis, kroonluchters, 22 tuinen, licht binnen; figurantenbug opgelost |
| u-tapijt | het vliegende tapijt: zweverig, glijden, drift, momentum los van de kijkrichting |
| u-z6wereld | zone 6 kaart: Rozenkloof, Saffraanpoort, 5 gebieden, Zoutgrot, kerker, toren; Schatkamer als diepe Al-Khazneh-gevel, reliëf in de kloof |
| u-z6verhaal | De Sterrenkaravaan: Wolkwal-zwemfilm, lijn E, 10 hoofdstappen, 15 zijquests, Ringeling opgevrolijkt; cap 70 |
| u-z6wezens | 13 wezens, Koningin Wormelien + De Zoutkolos, De Verzonken Sterrenwacht, de Bultguh (2 zitplekken) |
| u-z6raid | De Sterrendovende Toren: 2-10 spelers, schaling, Wachtzaal + rij, 4 bazen, raid-frames, kist per clear |
| u-z6muziek | zone-6 muziek: gebieden, steden, bazen met fases, fanfares, stingers |
| u-geroezemoes | zacht guh-geroezemoes op drukke plekken, eigen schuif |
| u-leven-stad | beestjes, guh-huisdieren, straatleven, speeltuinen (ook Saffraanpoort), terrassen |
| u-leven-ritme | dag/nacht in de steden, weer met charme, emote-reacties + knuffelmeter, het Pluizenblaadje |
| u-opslagvip | alleen het eerste extra vak koopbaar ("Binnenkort" voor de rest), eenmalige VIP++-grap |
| u-questrobot | robot speelt alle quests zone 1-6 (41/19/24/23/24/25, 0 vast) + rapport; 5 bugs gefixt |
| u-questlopen | admin-paneel: hoofdlijn zone 1 → 6 doorklikken met echte films en bazen |
| u-grootte, u-krimp | jar kleiner: structuren gepakt, OGG mono ~48 kbps, grote textures gehalveerd, PNG verliesvrij (334 → ~270 MB) |
| u-site | website: zone 6 + raid, drops met kansen, renders, tabellen, verhaal-overhaul, geheimen, live posities (opt-out) |

## Wat de integratie (v-int) zelf deed
- **Samenvoegen:** 36 branches, steeds `--no-ff`, compileJava na elke merge.
  - Taalbestanden gaan als sleutel-unie, zonder sleutels terug te zetten die een slice wiste.
  - In de FEATURES-volgorde blijft `vadsrijk_t_vfx` als laatste.
  - Elke registratie staat er één keer in (`Z6RaidRegistry` 2× weggehaald), en `SpecRegistry` blijft LAATST.
- **Koppelingen tussen slices:**
  - **H:** opent het Vadsrijk-menu alleen IN het Vadsrijk. Buiten het Vadsrijk blijft H de Vadskunst, zonder valse toetsbotsing.
  - **Menu:** tabs Knuffelbond en Mijn huis, plus de subtabs HUD aanpassen en Specialisatie.
  - **Specialisatie:** zichtbaar in de Karakter-tab en in de party-frames.
  - **Rijdier-drops (je loot-regel):** overal 10 % per speler per kill/clear, zeker na 15 keer mis, per karakter en per bron. Bronnen:
    - Zwamzwijntje;
    - Kristal-Pluimguh;
    - Gouden Pluimguh;
    - de grotkleuren;
    - de Sterrennacht-Bultguh uit de raid.

    Testen met `/guhs vadsrijk loot kansen|pech|rol`.
  - **Raid:**
    - de raid-frames zijn verplaatsbaar in de HUD-bewerker;
    - een clear geeft 200 bondsxp;
    - in de toren speelt de torendrone in plaats van de gebiedsmuziek (nieuwe haak `GeluidPlan.PLEK_STREKEN`);
    - Ringelings fanfare speelt via de baas-haak;
    - de torendeur stuurt je niet meteen terug naar binnen als je net naar buiten komt.
  - **Skillboom tot cap 70:** de gaten in rij 6-7 zijn gevuld, en rij 8/9 (lv 62/66) is erbij, zodat 69 punten een echte keuze zijn.
  - **Zone 3/4-bazen:** spelen nu de VFX-animaties.
  - **Luchtschip:** lijn E staat in de uitleg bij een gesloten lijn.
  - **Grotten:**
    - een puzzel telt 1× en staat in de tab Puzzels;
    - een grot-reset haalt ook ongeopende eindkisten weg;
    - grot- en raid-huisdieren tellen in de goede zone van het Verkenningsboek.
  - **Quests:** de soepkom-volger (z6_s15) wijst nu naar de volgende vluchteling zonder soep.
  - **Geluid:**
    - StadsLeven gebruikt de dag/nacht van u-leven-ritme (één schakelaar);
    - stadsguhs tellen mee voor het geroezemoes;
    - het bazaar-bed in Saffraanpoort speelt op half volume;
    - de tooltip van de schuif "wereldgeluiden" klopt weer.
- **Kaart:**
  - volledige regen van zone 1 → 6 met de speeltuin-pass (ook Saffraanpoort), de verbind-pass en kaartPak;
  - de ruiterroutes zijn opnieuw uitgerekend;
  - zweefcheck kent nu zone 6.
- **Jar:** `tools/krimp.py --proc 10` op de eindboom: nog 234 PNG's kleiner en 45 OGG's opnieuw gecodeerd; `--dry` geeft daarna 0.
- **Website:** kaarttegels opnieuw (zone 1-6) en de site lokaal gebouwd (939 pagina's, 0 kapotte links). **Niet gepubliceerd.**
- **Run-map:** `run/` is gekopieerd uit t-int (config, opties, resourcepacks), zonder saves of logs. Een AutoCheck-wandeling over zone 1-6 is gedaan (zie Tests).

## Tests
- **Volledige suite:** `runGameTestServer -Pgt=Vadsrijk,Dubbel,Geluid,Geroezemoes,Indeling,StadsMuziek,Stadsleven,TModels,UDeco,VInt,Wolkwal,Z2Npc,Z3Npc,Z4,Z5,Z6` gaf **368 tests, 6 rood**.
  - **2 echte fouten, gerepareerd:**
    - questDataKlopt kende de zone-6-plekken niet;
    - het Verkenningsboek telde de grot- en raid-huisdieren in zone 1.
  - **4 slagen los** (volgorde-afhankelijk, bekend): grotSpawnerVindtDeVloer, opperzwamFasen, spiegelzuchtSchaduwenstraal, en de speeltuin-test van Stadsleven (een andere test staat "te kijken").
  - **Na de fixes:** Voltooiing, Npc en Z6Raid 15/15; Z2Bazen, Wezens, Bazen, Stadsleven en GrotKerker los groen.
- **Onderweg, per merge:**
  - Specs, Klassen, Z6Verhaal, VInt en TRijdier: groen na de fix van de kosten van rij 8/9;
  - Z6Raid, VInt, Specs en Geluid: 22/22;
  - QuestLopen, URitme, Geroezemoes, OpslagVip, GrotKerker, Grotten en VInt: 19/19.
- **Quest-robot:** zone 1-6: 41/19/24/23/24/25 quests gehaald, 0 vast, 0 niet gestart. Alle zone-vlaggen staan en alle luchtschiplijnen (ook lijn E) gaan open. 328 NPC's, 0 ontbrekende teksten.
- **AutoCheck-wandeling** (`tools/autocheck/vadsrijk_v_int.txt`):
  - 23 schermafdrukken van zone 1 t/m 6 (o.a. Schatkamer, bazaar, Moederoase, toren, Wachtzaal van de raid) in `run/screenshots/autocheck/vi_*.png`;
  - geen crash; elke zone laadt en de raid-binnentoren bouwt zichzelf;
  - in het log alleen de oude modelwaarschuwingen (droombed, bliksemsmidse).

  Het uiterlijk beoordeel jij in de client.

## Checks na de laatste kaart-regen (zone 1 → 6, 2026-11-08)
| check | uitslag |
|---|---|
| audit (zone 1) | kuil 4 (= vorige keer), poi 20, stap 5, water 20 |
| verbindcheck | 1308 tegels, los 0, fout 8 (Raadszaal, muur-`up`-vlag, cosmetisch) |
| grotcheck | 0 problemen in alle 5 grotten |
| loopcheck3 | PAD 397, VAL 1 (= vorige keer) |
| loopcheck4 | 0 |
| loopcheck5 | VAL 475, LEEG 660 (de open staartvin; het Wolkvangnet vangt je), alle POI's bereikbaar |
| loopcheck6 | alleen `toren_top` onbereikbaar (anker van de raid: zo bedoeld) |
| poicheck3 / 4 / 6 | 0 / 0 / 0 |
| zweefcheck | z1 115, z2 20, z3 0, z4 26, z5 141, z6 0 (muurlantaarns en lampionslingers: zo bedoeld) |
| luchtschip_check | lijnen A-D OK (lijn 0 nog uit) |
| kaartPak controle | 0 verschillen |
| krimp `--dry` | 0 |
| website | 939 pagina's, 0 kapotte links |

## Niet af / open
- **Nog niet in een client gezien** (alleen gametests, checkers en de AutoCheck-foto's). Kijk hier zelf naar:
  - het Vadsrijk-menu (H) met alle tabs en de spec-rijen 8/9 bij GUI-schaal 3/4;
  - de raid-frames en de HUD-bewerker;
  - de VIP++-grap boven de knapzak;
  - het Questlopen-paneel;
  - de kastelen van de Knuffelbond en het Knuffelkwartier;
  - de speeltuinen (zithoogtes);
  - de ruiters (zadelhoogte);
  - de emote-poses;
  - het tapijt.
- **Smaak / luisteren:**
  - alle nieuwe muziek (zone 6, bazen-fases, fanfares);
  - het geroezemoes en de wereldgeluiden (de volumes zijn een gok, aan de zachte kant).
- **Website:** lokaal gebouwd met de definitieve kaart, maar NIET gepubliceerd. De hoofdagent zet het voorbeeld op guhs.nl/vadsrijk-preview/ bij. Verder open:
  - 8 items zonder icoon;
  - zone 4/5 hebben nog geen Gids-onderwerpen;
  - live.guhs.nl/server vraagt je OK.
- **Bekende kleine dingen:**
  - verbindcheck: 8 meldingen bij de Raadszaal (cosmetisch);
  - zweefcheck: muurlantaarns en lampionslingers in zone 1/2/4/5 (zo bedoeld);
  - de havenmasten in zone 3 hebben geen staplek (alleen decor);
  - lijn 0 (kamp ↔ De Haven) staat nog uit.
- **Bewust niet gedaan (je besluiten):** dialoogkeuzes, Het Grote Werk, een iNv voor gear.
- **Zone-1-terugkeerquests voor rijdieren** gaan pas open na je eerste rijdier (zone 3). Zo bedoeld (besluit ronde 3, "teaser").
- **Engels:** de vertalingen bestaan (en_us), maar de echte Engelse eindronde komt "aan het eind, alles in één keer".
