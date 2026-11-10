# Spiekbrief: Het Vadsrijk na ronde Y (Vadsrijk ↔ survival, klaar voor de release met alleen zone 1)

Dit is de spiekbrief van ronde Y (2026-11-10). Ronde Y maakt de grens tussen je survival-wereld en het Vadsrijk dicht en legt
de drie bruggen die jij koos: het compacte **FTB-hoofdstuk "Het Vadsrijk"** met **pronkstukken** als beloning (claimen nooit in
het Vadsrijk), de **Vadsrijk-vitrine** voor je basis en je **Vadsrijk-kleding als looks** thuis (Garderobe). Plus je twee
besluiten (Mollie/Post alleen deco en kleding; geen FTB-werkbanken binnen). Alles zit in één nieuwe testclient:
**`guhs_vadsrijk\y-int\run-client.bat`** (de x-int-client heb je niet meer nodig). De client staat op **alle zones open**
(openZones 6); hoofdstuk X2 zegt hoe je de release-blik (1) aanzet. FTB Essentials zit er nu in, net als op de server.

> **Het verhaal testen? Hoofdstuk 0: QUESTLOPEN** (`/guhs vadsrijk questlopen`, alleen als OP).
>
> **Ronde Y nalopen? Hoofdstuk 1: VADSRIJK ↔ SURVIVAL.** FTB-hoofdstuk, pronkstukken, vitrine, Garderobe, de scheiding en je
> twee besluiten, met per punt een commando en wat je hoort te zien.
>
> **De release met alleen zone 1 bekijken? Hoofdstuk X2.** De stappen voor de echte release staan in `RELEASE_CHECKLIST.md`.

**Volgorde van deze spiekbrief:**
- starten, toetsen en de loot-regels;
- hoofdstuk 0: Questlopen;
- hoofdstuk 1: ronde Y, Vadsrijk ↔ survival;
- daarna de hoofdstukken van ronde X (X1 je feedback, X2 release met alleen zone 1), ronde W (W1 feedback, W2 de systemen)
  en ronde U (B1 t/m B8). Ze gelden nog steeds.

Elk blok test één onderdeel: copy-paste de commando's in de chat en kijk of gebeurt wat erbij staat.

## Starten
- Dubbelklik `run-client.bat` in `guhs_vadsrijk\y-int`. Maak een **nieuwe** wereld (Creatief, cheats aan); de saves-map is leeg.
  De hele kaart (zone 1 t/m 6) is opnieuw gegenereerd, dus een oude wereld klopt niet meer.
- `/guhs vadsrijk` om erin te gaan. De eerste keer krijg je meteen het karakterscherm (3 vaste slots).
- Wil je geraakt kunnen worden of iets lopend testen: `/gamemode survival` (in creatief vallen de wezens je niet aan, en in
  creatief zet een dungeon je er niet uit).
- Alle teleportcommando's gebruiken plek-namen of de nieuwe kaart-coördinaten, dus die kloppen vanzelf.

## Handig tussendoor
- `/guhs vadsrijk vol`: Vads, energie en ult vol, cooldowns gewist.
- `/guhs vadsrijk info`: je data.
- `/guhs vadsrijk level 70`: het level-plafond is **70**.
- `/guhs vadsrijk tips reset`: alle tips opnieuw.
- `/guhs vadsrijk loot kansen`: alle rijdier-drops met jouw pech-teller.
- `/toetsen`: het toetsenoverzicht als los scherm (ook in het pauzemenu, rechts).
- Plekken: `/guhs vadsrijk wereld plek <id>` (zone 1), `z2wereld`, `z3wereld`, `z4wereld`, `z5wereld`, `z6wereld plek <id>`;
  Tab-aanvullen toont alle ids.
- **Nieuw:** `/guhs vadsrijk dungeon deur` (alle dungeondeuren), `/guhs vadsrijk party` (de Party-tab),
  `/guhs vadsrijk nepspeler lijst` (de nep-spelers).

## Toetsen (na ronde W)
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
| H → tab **Party** | **nieuw:** de Party-tab (uitnodigen, leider, wachtrij), ook `/guhs vadsrijk party` |
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
| Grotkerker-eindkist (elke grot, zone 1-6) | Pluimguh in de grotkleur (Glimzwam, Amethist, Wortelhout, Koraal, Onweer, Zoutroze; als je een Pluimguh hebt) | 10 % + pech 15 |
| Grotkerker-eindkist | huisdier (chibi van de grotbaas) | 6 % per clear, zeker na 25 keer mis |
| Grotkerker-eindkist | uitrusting EPISCH (anders ZELDZAAM) | 1e clear zeker episch, daarna 12 % |
| Grotkerker-eindkist | bazenkern van de zone | 25 % |
| Zone-4-zijquests z4_s14 / z4_s15 | Lampion-Pluimguh / Duinhupper | vaste beloning |

Testen:
```
/guhs vadsrijk loot kansen
/guhs vadsrijk loot pech opperzwam_zwamzwijntje 15
/guhs vadsrijk loot rol opperzwam_zwamzwijntje
```
Na `pech ... 15` is de volgende `rol` zeker raak (gouden chatregel + geluid). Een misser toont grijs "nog N tot zeker".

---

---

# 0. QUESTLOPEN: HET HELE VERHAAL DOORKLIKKEN (voor je verhaal-playtest)

Dit is het nieuwe admin-paneel waarmee je de hoofdlijn van zone 1 t/m 6 op volgorde doorloopt: teleport, stap afronden, level goed zetten, elke film zien, bazen zelf doen of laten verslaan. Begin je verhaaltest hier.

### Questlopen (admin): de hoofdlijn zone 1 → 6 doorklikken (U-QUESTLOPEN)

Alleen voor OP's. Werkt op je HUIDIGE karakter en verandert echt je voortgang (quests, level, vlaggen, beloningen).
Tip: maak er een apart testkarakter voor (Karakter-scherm), dan blijft je eigen karakter heel.

**Openen**
```
/guhs vadsrijk questlopen
```
- Of: H → tab Instellingen → knop **Questlopen (admin)** (zie je alleen als OP).
- Eerste keer: waarschuwing met je karakternaam → **Begrepen**.
- Het paneel is klein en staat rechtsboven; **sleep de roze titelbalk** om het te verplaatsen. Het spel pauzeert niet.
- Tijdens gesprekken/scènes gaat het paneel vanzelf dicht en komt het daarna vanzelf terug. Toch weg? Dan:
```
/guhs vadsrijk questlopen verder
```

**Wat je ziet**
- Bovenaan de huidige stap: zone · quest, wat (Aannemen / Stap / Inleveren), waar, wie, en *min. level · verwacht level* (plus je eigen level).
- Daaronder de hele hoofdlijn als lijst: ✔ = gedaan (grijs), ▶ = huidige stap (roze), ☠ = baasgevecht, `+` = extra quest (toggle). Roze streep = nieuwe zone.

**Volgende ▶** (of Enter)
1. zet je level (generiek + klasse) op minimaal wat de stap vraagt;
2. doet de stap via de ECHTE haken: echte klik op de NPC (het echte gesprek opent, lees het en klik door), op de plek gaan staan (de echte verhaaltriggers), gebied betreden, kampvuur...;
3. alle scènes spelen gewoon: opening, BAM, luchtschipvluchten, Vahoegnir, de Wolkwal-zwemfilm, flitsen, aankomstfilms. Het paneel wacht tot alles klaar is;
4. daarna word je naar de plek van de volgende stap geteleporteerd (op een vloer) en komt het paneel terug.
- Telt een stap niet via de echte route (bv. een minigame), dan vuurt hij de haak af die het spel zelf gebruikt, en als laatste redmiddel de teller zelf.
- **Let op bij Aannemen:** het aanbod-gesprek opent echt; klik zelf op "Ja"/de hoofdquest-knop. Sluit je het zonder ja, dan neemt Volgende hem daarna alsnog aan.

**Baasgevecht** (☠ in de lijst): in plaats van Volgende twee knoppen:
- **Zelf vechten** → je gaat de arena in, de echte intro + titelkaart spelen, jij vecht. Te zwaar? `/guhs vadsrijk vol`. Paneel komt terug als de baas verslagen is.
- **Laat hem verslaan** → zelfde intro, daarna wordt de baas vanaf jouw kant verslagen: de echte opvrolijk-/verslagenscène, de buitkist en het verhaalmoment spelen.
- Tijdens Zelf vechten toch opgeven: `/guhs vadsrijk questlopen verder` → **Laat hem verslaan**.

**Overige knoppen**
- **◀ Terug**: zet de vorige stap terug (quest weer open) en teleporteert erheen. Beloningen, items en vlaggen blijven.
- **Spring naar…**: klik eerst een regel in de lijst, dan **Spring naar N**. Vooruit = alle stappen ervoor worden STIL gedaan (geen films, wel alle vlaggen/beloningen); dit kan even duren, je ziet "stap x → y". **Stop** breekt af. Achteruit = quests worden teruggezet.
- **Herhaal scène**: speelt de film(s) van de laatste stap nog eens.
- **Alleen teleporteren**: alleen naar de plek van de huidige stap, zonder iets af te ronden.
- **Reset hoofdlijn**: 2× klikken. Wist het hele questlog van dit karakter (ook zijquests, geheimen, verhaalvlaggen) en zet je in het Knuffelkamp. Level en items blijven.
- **Level: minimum / verwacht**: naar welk level Volgende je zet (minimum van de stap, of wat een gewone speler daar ongeveer heeft).
- **Advancements: aan/uit**: neemt de advancement-quests van je klasse mee (lv 17 = de specialisatiekeuze komt echt in beeld, lv 35, lv 58), op hun plek in de lijst.
- **Rijdier/huisdier: aan/uit**: neemt de rijdier-quests (Hoedhupper, Knuffelhert, Guhwolf) en de huisdier-zijquests van zone 6 mee.

**Snelle testronde**
```
/guhs vadsrijk questlopen
```
1. Begrepen → Reset hoofdlijn (2×) → Volgende: de openingsfilm speelt in het Knuffelkamp.
2. Een paar keer Volgende/Enter: Welkom-quest, klassekeuze (het echte klassescherm), poppen, quiz.
3. Klik regel "hq05 ... Stap" (de Klodderreus, ☠) → Spring naar → bij de baas **Laat hem verslaan**: intro, verslagen-scène, kist.
4. Klik een regel in zone 4 (Lampionkust) → Spring naar: stil vooruit, daarna sta je bij de juiste NPC met het juiste level.
5. Herhaal scène na een stap met een film.
6. Zet Advancements aan → in zone 1 verschijnt "+ spec17_..." na de Druipgrotten; bij Inleveren komt de specialisatiekeuze echt in beeld.

**Wat er mis kan gaan / wat je ziet**
- "Deze stap telt niet mee": een stap die zelfs via de haak niet telt → Spring naar de volgende stap en meld het.
- "Geen echte baas gevonden": de baas staat niet in een arena (of nog niet geladen); de verslagen-haak is gebruikt.
- Chat staat vol "Nieuwe quest / Quest af" na Spring naar: dat hoort zo.

### Nieuw in ronde W: [Sla stap over] en [Naar veilige plek] (W-SPEL)
```
/guhs vadsrijk questlopen
```
Nieuw onderaan het paneel: **[Sla stap over]** en **[Naar veilige plek]**. Ze werken altijd, ook midden in een gevecht of
terwijl het paneel "Bezig..." zegt (heropen met hetzelfde commando).
- Sla stap over: de stap telt meteen mee en je gaat naar de plek van de volgende stap.
- Naar veilige plek: stopt wat er loopt en zet je terug op de laatste rustige plek buiten een arena.
- Bij een baas (Zelf vechten of Laat hem verslaan): als de baas weg is maar de stap niet telde, telt hij na 5 s alsnog;
  daarna word je naar de volgende stap gezet, nooit achtergelaten in de arena.
Test bij Kootje Zilverstaart: Spring naar de stap "z5_hq08_wolkwaldroom" (doel 2), Zelf vechten, versla hem; daarna sta je
bij de volgende stap. Of klik meteen Sla stap over.

---

# 1. RONDE Y: VADSRIJK ↔ SURVIVAL

Ronde Y maakt de grens tussen je survival-wereld en het Vadsrijk dicht, met precies drie bruggen die jij koos: de
**pronkstukken** (FTB-beloningen), de **Vadsrijk-vitrine** en je **Vadsrijk-kleding als looks** (Garderobe). Plus je twee
besluiten: Vadsmunten kopen thuis alleen Vadsrijk-deco en kleding, en de FTB-werkbank-commando's werken niet in het Vadsrijk.

> Test de grensregels als **gewone speler**: ops mogen erlangs (om te testen). Met één account:
> `/guhs grenscheck start` zet jou tijdelijk op "geen op-vrijstelling" tot `/guhs grenscheck rapport` (zie 1.6).
> FTB Essentials zit nu in deze client (zoals op de server), dus `/enderchest`, `/home`, `/back` en `/open anvil` bestaan echt.
> Let op: FTB Essentials 26.1.2.4 heeft **geen `/ec`** en **geen los `/anvil`**: de werkbanken heten daar `/open anvil`,
> `/open crafting`, `/open smithing`, `/open stonecutter`, en die mogen alleen ops.

## 1.1 Het FTB-hoofdstuk "Het Vadsrijk" (compact)
Open het FTB-boek, hoofdstuk **Het Vadsrijk**. Drie kopjes: De Knuffelorde, Het avontuur, Geheimpjes. 14 quests:
- **Word rekruut van De Knuffelorde**: vinkt af aan het einde van de opleiding in het Knuffelkamp (`hq05_klodderreus`).
  Beloning: **Rekruutvaantje + Vadsrijk-vitrine**.
- **Zone 1..6 uitgespeeld**: de laatste hoofdquest van die zone. Beloning: een pronkstuk per zone.
- **De Sterrendovende Toren verslagen**: de eerste raid-clear. Beloning: Sterrendovende trofee.
- **Alle geheimpjes van zone 1..6**: alle Geheimen + Verloren knuffels + Puzzels van die zone. Beloning: Geheimpjesdoosje.
- Geen kaasknabbels, geen xp, geen spullen: elke beloning is precies één deco-blok. Elke gamemode en elk karakter telt.

Bij **gate 1** (de release) zie je alleen rekruut, zone 1 en geheimpjes 1. Gaat een zone open, dan verschijnt "Zone N is open!"
(afgevinkt) met de twee quests van die zone.
```
/guhs vadsrijk zones open 1
/guhs vadsrijk ftb status
/guhs vadsrijk zones open 2
```
Na `open 2` verschijnen binnen 10 s **Zone 2 is open!**, **Zone 2 uitgespeeld** en **Alle geheimpjes van zone 2**. Daarna weer
`/guhs vadsrijk zones open 6` (de client staat standaard op 6; wat verscheen blijft staan).

Snel afvinken: `/guhs vadsrijk ftb geef zone1` (of `rekruut`, `raid`, `geheimen1`...), alles terug: `/guhs vadsrijk ftb vergeet`.

**De claimregel: nooit in het Vadsrijk.**
1. Vink iets af terwijl je **in het Vadsrijk** staat (`/guhs vadsrijk`, dan `/guhs vadsrijk ftb geef zone1`). Klik de beloning
   (of "claim all"). Zie: goud bericht **"Beloningen claim je in je eigen wereld, niet in het Vadsrijk."**, je krijgt niets, en de
   beloning blijft claimbaar.
2. Ga naar huis (`/guhs vadsrijk`), klik nog eens. Zie: het **Knuffelzwamlampje** in je inventaris.
3. In het Knuffelkwartier (je Vadsrijk-huis) mag claimen wel: daar heb je je thuis-inventaris.
Dit geldt voor álle FTB-beloningen, ook van de andere Guhs-hoofdstukken.

## 1.2 De pronkstukken (14 deco-blokken voor je eigen wereld)
```
/guhs vadsrijk ftb trofeeen
```
Rekruutvaantje, Knuffelzwamlampje, Splinternaaldjes, Schemerwortellantaarn, Lampionvliegertje, Hoogvads-roosvenstertje,
Sterrentorentje, Sterrendovende trofee en 6x Geheimpjesdoosje (deksel in de zonekleur).
- Thuis neerzetten: kijkt naar jou; de lampjes geven licht. Tooltip "Beloning: Zone N van Het Vadsrijk uitgespeeld".
- In het Vadsrijk neerzetten: lukt niet ("Dit pronkstuk is voor je eigen wereld...").
- Hoe ze eruitzien heb je nog niet gezien: graag even oordelen (alleen vanilla-texturen, geen nieuwe plaatjes).

## 1.3 De Vadsrijk-vitrine
Eén glazen trofeekastje voor je basis, dat **live** je Vadsrijk-held laat zien. Je krijgt hem bij de rekruut-quest.
```
/give @s guhs:vr_vitrine
```
- Thuis neerzetten: "Je Vadsrijk-vitrine staat!", jij bent de eigenaar. In het glas je rijdier als draaiend beeldje (anders je
  huisdier, anders het Knuffelorde-hartschild). Erboven (binnen ~14 blokken): naam, klasse + level, zone-badges, raid en de datum
  van je eerste zone.
- Rechtsklik met lege hand: een overzicht in de chat. Met een blok in je hand zet je dat blok gewoon tegen de vitrine.
- In het Vadsrijk (en het Knuffelkwartier) neerzetten: lukt niet.
- Vullen (als op), bij je vitrine:
```
/guhs vadsrijk vitrine zone 1
/guhs vadsrijk mount geef knuffelhert
/guhs vadsrijk mount rijd knuffelhert
/guhs vadsrijk vitrine ververs
```
- Gate 1: badges **① ? ? ? ? ?**, geen zonenamen, geen raid-regel, ook niet als je (als op) alles al klaar hebt.
  `/guhs vadsrijk zones open 6`: binnen 5 s alle badges en de raid-regel.
- Offline: de vitrine houdt de laatste stand vast ("... is nu niet online").
- Graag beoordelen: hoe het glas over het beeldje valt, en of grote rijdiers door het glas steken.

## 1.4 De Garderobe: je Vadsrijk-kleding thuis als looks
- Toets **O** of `/guhs garderobe` (iedereen, overal). Thuis heet het scherm **Garderobe** (8 tabs, geen Harnas-tab), in het
  Vadsrijk nog "Kledingkast".
- Bovenaan: "Thuis is je Vadsrijk-kleding alleen voor de looks: geen krachten, geen harnas."
- Kies thuis bij Rug de **Knuffelorde-cape**, bij Hoofd het **Strikje**: je ziet ze meteen (F5), en andere spelers ook.
- Vleugels thuis: alleen een look. Zweven, dubbele sprong en zachte landing doen thuis niks.
- Met een echte elytra aan verdwijnen de cosmetische vleugels.
- Vrijspelen gebeurt alleen in het Vadsrijk; thuis openen geeft niks gratis. Bij gate 1 zie je van zone 2-6 geen vergrendelde
  stukken ("Meer kleding vind je later, in nieuwe delen van het Vadsrijk.").
- Een diamanten helm wordt nooit een Vadsrijk-harnaslook.

## 1.5 De scheiding: wat is dicht (14 kanalen getest)
| kanaal | wat er nu gebeurt |
|---|---|
| Inventaris | wisselt bij de grens; volle tas + item aan je muis: er valt niks op de grond (rest in `/guhs vadsrijk kist`) |
| Enderkist | binnen dicht: `/enderchest` "werkt niet in het Vadsrijk", een enderkistscherm sluit meteen |
| Shulkers/bundels | reizen gewoon mee in je tas (spullen erin blijven erin), dus geen lek |
| Spullen/xp-bolletjes/karretjes | geen enkel niet-speler-ding gaat over de grens, beide kanten op |
| Vanilla xp | wisselt: binnen een eigen pot (meestal 0), buiten precies terug |
| Effecten, hartjes, honger, vuur | thuis-lijf wordt bewaard en precies teruggegeven; Vadsrijk-effecten (Nachtzicht...) blijven binnen |
| Dood / respawn | binnen flauwvallen; nergens binnen een spawnpunt zetten |
| Uitloggen / crash | wissel is in één tick en idempotent |
| Dieren / paarden / leidtouw | je stapt af, het dier blijft aan zijn kant |
| Teleports van andere mods | `/home`, `/back`, `/tpa`, `/warp`, `/rtp`, `/spawn` binnen geweigerd; van buiten naar binnen kan alleen via de ingang |
| Opslag en handel | Bank Guh en Leverkist binnen dicht; Vadsrijk-opslag (Opslag, Materiaaltas, Bondbank) buiten dicht; Premiebord alleen binnen |
| Munten | Vadsmunten blijven een getal in je Vadsrijk-data; thuis kopen ze alleen deco/kleding (zie 1.7) |
| Survival-beloningen terwijl je binnen bent | (Sherlock, Momo, Opa's kelder...) gaan naar je thuis-inventaris, niet in je Vadsrijk-tas |
| Ops | mogen erlangs bij de grensregels; de wissels gelden voor iedereen |

Proberen (als gewone speler, of met `/guhs grenscheck start` eerst):
```
/xp add @s 30 levels
/effect give @s minecraft:strength 300 1
/guhs vadsrijk
```
Binnen: xp-balk 0, geen Kracht, volle hartjes. Typ `/enderchest`, `/home test`, `/open anvil`: allemaal "werkt niet in het
Vadsrijk" (zonder `/guhs grenscheck start` mag je als op wel).
Weer `/guhs vadsrijk`: 30 levels, Kracht II met de resttijd, je oude hartjes en honger.

## 1.6 De echte grenstest in één commando-rij (wat de AutoCheck deed)
De gametest-server heeft geen Vadsrijk-dimensie, dus de echte grens is in de client getest met `/guhs grenscheck` (op):
```
/guhs grenscheck start
/guhs grenscheck in
/guhs grenscheck binnen
/guhs grenscheck uit
/guhs grenscheck thuis
/guhs grenscheck vitrine
/guhs grenscheck rapport
```
Elke stap zet groene **PASS**- of rode **FAIL**-regels in de chat (en in `run\grenscheck.txt`). `thuis` zet ook een vitrine en
twee pronkstukken voor je neer, `vitrine` geeft je een Knuffelhert en zet je camera op het kastje.

**Uitslag van de AutoCheck (poort 1, nieuwe wereld): 42 PASS, 0 FAIL.** Gecontroleerd: inventaris, vanilla xp (17 -> 0 -> 17),
Speed II weg binnen en terug thuis, Nachtzicht van binnen niet mee naar buiten, hartjes 13 en eten 11 precies terug, een
smaragd die je bij de grens liet vallen ligt nog thuis en is nergens binnen, een item-entiteit kan niet over de grens (beide
kanten), `/enderchest` + de vier `/open`-werkbanken + `/home` + `/back` + `/spawn` binnen geweigerd (thuis werken ze),
FTB-claim van de vitrine binnen geweigerd en thuis gelukt, vitrine en pronkstuk binnen niet neer te zetten en thuis wel.
Schermafdrukken en `grenscheck.txt`: `guhs_vadsrijk\y-int-autocheck-bewijs\` (yi_*.png).

## 1.7 Jouw twee besluiten
**a) Vadsmunten kopen thuis alleen Vadsrijk-deco en kleding.**
- Meubelmeester Mollie (Knuffelkwartier, markt) verkoopt nu alleen nog: Guh-stoel, Guh-tafel, Guh-bank, roze zitzak, magenta
  kussen, vlaggetjes en het Vadsrijk-vaandel. **Weg:** Guh-taart (eten), Roze guhbloem en Guhoortjes (bloemen), lantaarns,
  roze tapijt, roze bed, bloempotten, kersenplanken, roze glas, boekenkasten.
- De Vadsrijk Post brengt alleen nog deco en kleding thuis. Wat hij bracht was al allemaal deco of kleding (Guh-kleding,
  vr_eco-deco, het guh-vliegertje), dus daar verdwijnt niks; er zit nu een slot op: iets anders gaat nooit meer per Post naar huis
  (het blijft dan in je Vadsrijk-tas).
- Wat je al gekocht had, blijft van jou.
```
/guhs vadsrijk munten 50000
/guhs vadsrijk huis wijk
```
Loop naar **Meubelmeester Mollie** in het middelste marktkraampje en klik haar aan: nog 7 dingen (6 + 1 op "Meer spulletjes »").

**b) FTB-werkbanken werken niet in het Vadsrijk.** In FTB Essentials 26.1.2.4 heten ze `/open anvil`, `/open crafting`,
`/open smithing` en `/open stonecutter`, en ze zijn **alleen voor ops** (gewone spelers hebben ze sowieso niet). Binnen geeft
`/open ...` nu "werkt niet in het Vadsrijk" (voor wie geen op-vrijstelling heeft); thuis werken ze gewoon. Voor de zekerheid
staan ook de losse namen `/anvil`, `/crafting`, `/smithing`, `/stonecutter`, `/grindstone`, `/loom`, `/cartography`, `/craft`
en `/workbench` op de lijst (een latere FTB-versie of een andere mod).

---

# X1. RONDE X: JOUW FEEDBACK, PUNT VOOR PUNT (vorige ronde)

Elk punt: wat er mis was, het commando om er te komen, en wat je nu hoort te zien. Tip: `/gamemode spectator` om rond te
kijken, `/gamemode survival` om iets echt lopend te testen.

## X1.1 De ingang van de walvis (zone 5): de trap naar het Staartpark
**Was:** de ingang zat in een tunneltje recht achter de troon, je zag hem niet.
**Nu:** een brede poort aan het eind van het noordelijke zijpad van de troonzaal, met een lichtblauwe loper ernaartoe, een
gouden lijst en een bord "Naar het Staartpark & de Staarthaven". Buiten een bordes en twee brede trappen (3 breed) met een
tussenbordes, tot op het gras van het Staartpark.
- **De poort vanuit de zaal:** `/tp @s 2845 233 -531 facing 2856 234 -531`
  - Zie: een opening van 3 breed en 4 hoog in de achterwand, gelijk met de vloer, met een gouden lijst en het bord erboven.
- **Het bordes buiten:** `/tp @s 2856 233 -531`
  - Zie: een balustrade, een lantaarn op de hoek, een gouden boog boven de trap met hetzelfde bord.
- **Het tussenbordes:** `/tp @s 2858 218 -514`
- **Onderaan, op het gras:** `/tp @s 2860 204 -531`
- In survival: loop van de troonzaal naar beneden en weer terug, zonder te springen of iets te breken.
- **Let op:** de borden in de kaart zijn nieuw (tekst in de tegels). Zijn ze leeg? Zeg het: dan zit het in het bordformaat.

## X1.2 Zone 6 buiten de stadjes: alles staat op de grond
**Was:** gebouwen, tenten, palmstammen en randen zweefden net boven de grond; wegen liepen als droge geul door het water.
**Nu:** een funderingspass zet alles buiten Rozenkloof/Saffraanpoort op de grond; wegen door water zijn een dijkje of een
doorwaadplaats. De checker telt nu 0 gaten en 0 zwevende stukken buiten de steden (was 399 en 169 cellen).
- **De voet van de Sterrendovende Toren** (was het ergst): `/guhs vadsrijk z6wereld plek toren_voet`
- **Het rivierpad door de Palmenkloven:** `/tp @s 3500 65 -963`
- **De Oostroute door de Moederoase:** `/tp @s 4106 65 -738`
- **Het Palmbaken:** `/guhs vadsrijk z6wereld plek palmbaken`
- **Het Duinbaken:** `/guhs vadsrijk z6wereld plek duinbaken_voet`
- **Het karavaankamp:** `/guhs vadsrijk z6wereld plek karavaankamp`

## X1.3 "Is de grote toren de raid?": De Sterrendovende Toren
**Was:** ja, maar alleen de buitenkant. De echte raid zit in De Wachtzaal onder de toren, en de poort was een zwart glas van 5
breed in een nis van 9 breed: je liep erlangs de holle toren in en er gebeurde niks. De verdiepingen, ringen en de lens
zweefden los.
**Nu:**
- **Naar de poort:** `/guhs vadsrijk z6raid tp poort` (kijk naar het oosten).
  - Zie: een open nis met een gouden cirkel in de vloer en achterin een paarse sterrenvlies-wand in een gouden ring.
  - Erboven een naambord in dezelfde stijl als de dungeondeuren: groot **De Sterrendovende Toren**, klein eronder
    "Raid voor 2-10 spelers · Loop naar binnen: De Wachtzaal".
  - Op de trap staat **Heraut Sterrenroep**: klik hem, hij legt de raid uit en zegt of de toren vrij is.
- **Naar binnen lopen** (ook aan de zijkant van de nis): je staat meteen in **De Wachtzaal**, met de gouden startcirkel
  (bord erboven), **Zaalwachter Pril** en in het westen de paarse boog **Uitgang: naar buiten**.
- **Weer naar buiten:** door de paarse boog. Je staat buiten op de trap en wordt niet terug naar binnen gestuurd.
- **Starten:** met minstens één vriend samen 3 tellen op de cirkel. Alleen gebeurt er niks. Solo testen (dev):
  `/guhs vadsrijk z6raid start 2`, stoppen: `/guhs vadsrijk z6raid stop`.
- **De toren van buiten:** `/tp @s 4080 120 -688 -90 0`. Zie: één toren uit één stuk; de gouden ringen zitten als banden
  om de toren; de Lege Lens rust op tien spitsen.
- **Tracker:** na z6_hq10, zolang je de raid nog niet hebt gehaald, staat in zone 6 de regel
  **De Sterrendovende Toren (raid)** met de straal op de poort.

## X1.4 Naamborden bij de dungeondeuren
**Was:** de deuren vielen niet op en hadden geen naam.
**Nu:** boven elke deur een bord van twee regels: de naam groot en vet in de kleur van de dungeon, eronder klein
"Dungeon · Grot · lv A-B". Fel, ook 's nachts, met een onzichtbaar lichtje en af en toe gloei-vonkjes op de deur.

| Dungeon | Teleport | Wat je ziet |
|---|---|---|
| De Zwamgrot (zone 1) | `/tp @s -412 69 148 90 0` | "De Zwamgrot" + "Dungeon · Grot · lv 11-14" |
| De Kristalgrot (zone 2) | `/tp @s 869 119 -1104 -135 0` | bord in de tunnel, vlak voor de deur |
| De Wortelgrot (zone 3) | `/tp @s 1410 69 -737 0 0` | bord boven de deur in de wortelstronk |
| De Koraalgrot (zone 4) | `/tp @s 2230 71 -596 -135 0` | bord boven de deur in de koraalschoorsteen |
| De Koraalgrot, zeebodem | `/tp @s 2270 52 -572 90 0` | dichtgegroeid: geen klopper, bord "Dichtgegroeid · de deur is boven op het atol"; klik = alleen een chatregel |
| De Wolkengrotten (zone 5) | `/tp @s 2494 205 -520 90 30` | bord boven het luik |
| De Zingende Zoutgrot (zone 6) | `/tp @s 3874 50 -521 180 0` | bord op hoofdhoogte in de trapkoker |

- Het bord komt altijd terug en nooit dubbel: `/kill @e[type=text_display,tag=guhs_vr_dungeonbord,distance=..10]`, binnen 2 s
  staat het er weer.
- De dungeondeur als item (alleen creative/JEI): `/give @s guhs:vr_dungeondeur`.
- De blauwe stadsblubs gloeien weer zacht, net als de echte blauwe Blub (kijk 's nachts in Zwamzoet).

## X1.5 Ruwe teksten weg: de Kwartiermeesters
**Was:** een guh heette `vadsrijk.npc.naam.kk_poort_zwamzoet`.
**Nu:** elke stad heeft een Kwartiermeester met een naam. Ze verschijnen een paar seconden nadat je bij het plein bent.

| Stad | Commando | Naam |
|---|---|---|
| Zwamzoet | `/tp @s 0 66 59` | **Kwartiermeester Lamelletje** |
| Kandijkom | `/tp @s 704 93 -1068` | **Kwartiermeester Klontje** |
| Honingtak | `/tp @s 1396 169 -525` | **Kwartiermeester Wasje** |
| Lampionhaven | `/tp @s 1848 69 -467` | **Kwartiermeester Lontje** |
| Hoogvads | `/tp @s 2748 221 -529` | **Kwartiermeester Torentje** |

- Gesmede uitrusting heette `vadsrijk.loot.kussenzwaard`: `/guhs vadsrijk ambacht maatwerk zwaard 1` geeft nu een gewone naam.
- Vangnet: mist er ooit nog een tekst, dan toont het spel een leesbare naam in plaats van de sleutel (en zet het in de log).
- Een nieuwe test leest ~4.100 sleutels die pas tijdens het spelen ontstaan, in het Nederlands én Engels: alles aanwezig.

## X1.6 Zone 1 (+ Knuffeleiland) en zone 2 op de grond
Muren, stelen, stammen en rotsvoeten die over een kuiltje hingen staan nu op de grond (zone 1: 400 kolommen, zone 2: 474).

| # | Wat | Commando | Wat je moet zien |
|---|---|---|---|
| 1 | Knuffelkamp: huis aan de helling | `/tp @s -464 89 1063 -90 10` | de roze muur loopt door tot op het gras |
| 2 | Sporenwoud: grote paddenstoel op een heuvel | `/tp @s -374 94 -137 -90 10` | de steel staat op de heuvel |
| 3 | Druipspits (uitkijknaald) | `/tp @s -95 100 -490 180 15` | de rotsvoet gaat aan de lage kant door tot de grond |
| 4 | Sporenwoud-waterval | `/tp @s -470 72 24 180 -10` | het water valt langs een rotswand; de geheime ingang erachter is open |
| 5 | Schildpadeiland | `/tp @s 524 77 726 180 30` | kist en boom staan op het schild |
| 6 | Zwamzoet: lampen onder het plafond | `/tp @s 0 196 -40 0 -60` | elke lamp hangt aan een ketting |
| 7 | Zone 2: ijsbouwsel op de helling | `/tp @s 853 118 -944` | het pakijs loopt door tot de sneeuw |
| 8 | **Nieuw (integratie):** de stenen trappen uit de baai bij De Haven | `/tp @s 60 65 456 90 30` | de trap uit het water loopt nu door tot op de kade (eerst moest je aan het eind 1 blok omhoog springen); de tweede trap: `/tp @s 87 65 528 90 30` |

## X1.7 Uitkijkpunt Galmtop (zone 2)
**Was:** de wenteltrap begon 18 blokken boven een klif. Daarna (tussenversie) kwam hij boven onder de vloer uit en zat de
reling dicht waar je aankwam.
**Nu:** de trap begint op de helling en komt boven via een paar treden net buiten het platform en een brede opening in de
reling op het platform uit.
- **Onderaan:** `/tp @s 916 162 -1098`
- **Bovenop:** `/guhs vadsrijk indeling plek uitkijk_galmtop`
- In survival: loop van onder naar boven en terug zonder te springen.

## X1.8 Engels voor alles
Ook de oude teksten (De Erfenis, Sherlock Guh, de mijnen, de FTB-hoofdstukken) hebben nu Engels: 0 ontbrekend. Niets om in
de client te bekijken (jouw regel: Engels nooit in de client checken).

## X1.9 De hotfixes van 1.9.3 t/m 1.9.7 zitten erin
De 1.10-tak was gebouwd op 1.9.2. De fixes die sindsdien live gingen (Houtzagerij zonder planken, één Guhrrit bij de tour,
Knabbelbaken-menu, Guhvilla-ramen, safari-namen, opslag x3 + plafond per goed, Leverkist 54 x 9999, geen dubbele
deco-guhs, chat in het overzicht, Konijnenpluis e.d., het Terughekje, weggeven/naar de Leverkist vanuit de opslag) zijn erin
gemerged, zodat de release ze niet ongedaan maakt.


# X2. RELEASE MET ALLEEN ZONE 1

Het plan: de publieke server gaat live met alleen het Knuffelkamp (tutorial) en zone 1 open. Het schip naar zone 2 vaart
"binnenkort".

## X2.1 De zone-poort
- **Op de server (blijvend):** in de wereldmap `serverconfig/guhs-vadsrijk-zones.toml`:
  ```
  openZones = 1
  ```
  (1 = alleen Knuffelkamp + zone 1, 6 = alles open; standaard 6). Ronde Y: NeoForge 26 leest dit bestand uit `config/`
  (in deze client: `run\config\guhs-vadsrijk-zones.toml`, staat op 6); `defaultconfigs/` doet niets meer.
- **Tijdens het spelen (als op):** `/guhs vadsrijk zones open 1` (meteen actief, wordt bewaard). Kijken: `/guhs vadsrijk zones`.
- **Ops en creative gaan overal doorheen.** Wil je als op zien wat een speler ziet (nieuw in de integratie):
  ```
  /guhs vadsrijk zones alsspeler aan
  ```
  Nu telt iedereen als gewone speler, ook jij in creative. Uit: `/guhs vadsrijk zones alsspeler uit` (gaat ook uit na een herstart).
- **Jouw testclient staat op 6** (alles open), zodat je alles kunt testen. Zo bekijk je de release:
  ```
  /guhs vadsrijk zones open 1
  /guhs vadsrijk zones alsspeler aan
  ```
  en terug: `/guhs vadsrijk zones alsspeler uit` en `/guhs vadsrijk zones open 6`.

## X2.2 Het einde van zone 1 (zoals een speler het ziet)
Snel naar het einde (dev, nieuw): `/guhs vadsrijk zones zone1klaar` (je zone-1-hoofdverhaal klaar, level 20).
1. Rechtsboven in de tracker: **"Wordt vervolgd: Het schip naar De Splinterpieken vaart binnenkort. Houd Discord in de gaten!"**
   (geen lichtstraal).
2. De luchtschipper op De Haven: `/tp @s 86 65 495 180 5`, klik hem: **"Het schip naar De Splinterpieken vaart binnenkort!
   Houd Discord in de gaten."** Op de reiskaart staan Kandijkom en verder grijs met "Binnenkort!".
3. De pier van lijn A (Noordklifhaven): `/tp @s 150 127 -650`: het bord/de pier zegt hetzelfde; het schip neemt je niet mee.
4. Er toch heen lopen/vliegen: `/tp @s 704 100 -1100`: je staat meteen terug in zone 1 (of op De Haven) met de melding
   "De Splinterpieken is nog dicht: het schip daarheen vaart binnenkort!".
5. De Zwamgrot (zone 1) werkt gewoon: `/tp @s -412 69 148 90 0` (solo, groep, wachtrij).

## X2.3 Het zachte level-plafond
- Met de poort op 1 stop je op **level 20** (balk vol). Eén chatregel; extra xp wordt gespaard (tot 3 levels).
  In het Vadsrijk-menu (Karakter) staat "Hoogste level voor nu" en hoeveel xp gespaard is.
- Testen: `/guhs vadsrijk level 19`, dan (met `alsspeler aan`) een paar wezens: je stopt op 20.
- Gaat de poort later naar 2: het plafond schuift naar 30 en de spaarpot betaalt uit als dubbele xp tot hij leeg is.
  Plafonds: 20 (poort 1), 30 (2), 50 (3), 56 (4), 60 (5), geen (6).
- Xp in zone 1: met alleen de hoofdquests hoef je niet meer te grinden (0-5 minuten). Zie `XP_BALANS.md`.
- Bekend: de twee rijdier-terugkeerquests van zone 1 zijn met poort 1 niet te doen (je eerste rijdier komt in zone 3).

## X2.4 De wiki: alleen zone 1
- https://guhs.nl/vadsrijk-preview-zone1/ : zo ziet guhs.nl/vadsrijk eruit bij de release (alleen tutorial + zone 1, een
  kaart "Binnenkort: nieuwe zones"; zone 2-6 staan er echt niet op: `.../zones/zone2.html` geeft 404).
- https://guhs.nl/vadsrijk-preview/ : de volledige versie (alle zones, achter de spoilerschakelaar).
- De poort in de mod en `--open-zones` van de wiki moeten altijd hetzelfde getal hebben. Zie `RELEASE_CHECKLIST.md`.


# W1. RONDE W: JOUW FEEDBACK, PUNT VOOR PUNT (vorige ronde)

Per punt: wat er gedaan is, een commando om te kopiëren en wat je hoort te zien. Alles in het Vadsrijk, als OP.
Waar "lopend" staat: eerst `/gamemode survival`, niet vliegen, en het laatste stuk zelf lopen.

## 1.1 Knuffelkamp: huisdiertjes en de drukte op de steiger
Zet Stadsleven op VOL (Vadsrijk-menu, toets H > Instellingen) als je het goed wilt zien.

**Huisdiertje loopt verder achter zijn guh**
```
/guhs vadsrijk leven tp knuffelkamp
/guhs vadsrijk leven spawn wandelaar
```
- Een guh die zijn huisdiertje uitlaat (konijntje, muisje, Blub of eendje): het diertje loopt nu **ongeveer 2 blokken** achter
  de guh (was 1). Ook als ze net verschijnen staat het diertje al een stukje naast de guh.

**De steiger van het Knuffelkamp is rustiger**
```
/execute in guhs:vadsrijk run tp @s -240 88 1012
```
- Je staat bij de luchtschipkapitein op de steiger. Wacht een halve minuut.
- Er staan nog maar een paar guhs/diertjes op de steiger, met ruimte ertussen. Op het plein en de grasvelden blijft het
  gezellig druk.
- Dat geldt voor alle smalle plekken (steigers, pieren, steegjes), ook in De Haven, de Lampionhaven en het Kandijkom-dok.

## 1.2 De blobs: de oude look terug, en de blauwe stadsdiertjes zijn Blubs
**De Grauwe Klodderreus en de proefklodders** (ga op een open plek staan):
```
/guhs vadsrijk models reus
/guhs vadsrijk models reus vecht
/summon guhs:vr_proefklodder ~ ~ ~3
/summon guhs:vr_proefkloddertje ~ ~ ~3
```
- Een groot, grijs, **vierkant slijmblok** (ongeveer 3 blokken) met een binnenblokje en een boos gezichtje. Geen rond model met
  wenkbrauwen meer. Springt/landt plat en weer hoog; deint mee bij een treffer, een fase-overgang en het juichen.
- Na de plof: roze en doorzichtig.
- De proefklodders en kloddertjes zijn kleine grijze vierkante slijmblokjes.

**Alle klodders van zone 1**
```
/guhs vadsrijk models toon zone1
/guhs vadsrijk models plof
/guhs vadsrijk models weg
```
- Elke klodder is weer een **vierkant blok** met een plat kapje en twee guh-oortjes (geen afgeronde randen, geen wiebelkapje).
  Groen (klodder), lichtgroen (kloddertje), blauw met bubbel, geel op een veer, paars met strik, blauwgroen met druppels, lila met
  gloeiende kristallen, roze met bloem (kampklodder). Na `plof`: blij en gekleurd. Ook de elite- en blije versies zijn de oude.

**De blauwe stadsdiertjes = Blubs**
```
/guhs vadsrijk leven tp knuffelkamp
```
Loop 10 tot 20 seconden tussen de huisjes (andere stadjes: `/guhs vadsrijk leven tp zwamzoet`).
- Ongeveer 3 op de 10 diertjes zijn kleine **blauwe Blubs**: hetzelfde lijfje, dezelfde oortjes en pootjes als de Blub in de
  droom en de tamme Blub in survival. Drie blauwtinten en verschillende snoetjes (puur, blosjes, guh-oogjes, :3).
- Geen vleugels, sprieten, gewei, strikje of kleertjes. Een guh die een Blub uitlaat heeft een iets kleinere Blub achter zich.

## 1.3 Pieppiepmuisjes: zeldzamer (behalve in de Kandijkom)
```
/guhs vadsrijk leven tp knuffelkamp
/guhs vadsrijk leven status
```
- In het Knuffelkamp en de andere stadjes zie je nu zo'n **2 muisjes tegelijk in plaats van 6** (gewone en roze samen, ongeveer
  30 %). De Blubs blijven even vaak.
```
/guhs vadsrijk leven tp kandijkom
```
- In de Kandijkom is het aantal muisjes **hetzelfde gebleven** (zo'n 6).

## 1.4 "Ver in het oosten", laden vóór cutscenes en flash F4
**"Ver in het oosten wil iets steeds meer"** (flash F2, einde zone 3):
```
/guhs vadsrijk z3npc f2
```
- Eerst kort zwart met "De wereld wordt geladen..." (hooguit 3 s, alleen de eerste keer). Dan vlieg je over het roze Zijdeland
  naar één grauwe toren (de Sterrendovende Toren), met een grijze chatregel. Daarna de grauwe troonzaal: Graaf Piepvel in zijn
  hemdje, Krabbel met zijn rapport, getik achter de troon, en de titel "WORDT VERVOLGD... / Ver in het oosten wil iets steeds
  meer." Na afloop sta je weer waar je stond, met "Zone 3 klaar" in de chat.
- **Niet meer:** een lege lucht onder een donker-effect.

**Laden vóór elke cutscene**
```
/guhs vadsrijk z3npc cliffhanger
```
- Is de wereld daar nog niet klaar: eerst kort zwart (na een halve seconde met "De wereld wordt geladen..."), dan begint het
  shot met een korte fade, met de wereld al helemaal opgebouwd. Is alles er al: geen zwart scherm.
- Na een verre cutscene: kort zwart tot jouw eigen omgeving weer is opgebouwd (hooguit 3 s).

**Flash F4** (na de raid, zone 6):
```
/guhs vadsrijk z6verhaal ringeling
```
- Eerst de film "Opgevrolijkt!". Dan F4: de eerste helft begint nu **bij de Sterrendovende Toren** (het gebouwde terrein van zone
  6, van oost gezien) en de blik zwaait naar het westen, naar Hoogvads. **Niet meer:** naar lege lucht kijken ten oosten van de kaart.

## 1.5 Luchtschepen: Kapitein Hemelhoog, de Oosttaksteiger en alle andere schepen
Elk luchtschip legt nu **strak tegen het open eind van de pier** aan: de schroeven raken de pierrand, de romp ligt er een half
blokje achter. Aan het eind van elke pier staat geen hek meer (de zijkanten houden hun hek).
Instappen kan op drie manieren: **het schip oplopen**, **op het schip klikken**, of **op de plank van de pierpunt klikken**.
**Vangnet:** ligt het schip er niet en loop je van de open punt af, dan sta je meteen weer op de pier met "Het luchtschip ligt
hier nog niet aan. Volgende vertrek over m:ss". Je valt nooit van een pier.

**Kapitein Hemelhoog** (jouw melding):
```
/guhs vadsrijk luchtschip vertrek D 120
/guhs vadsrijk luchtschip haven haven_z4_z5_vertrek
```
- Oostkant van het ronde platform: de witte hekjes zijn daar weg, het schip ligt er strak tegen (lag 6 blokken los achter de
  hekjes). Ook de westkant, waar de pier aankomt, is open: je loopt zo naar de kapitein.

**De Oosttaksteiger** ("de boot te ver"):
```
/guhs vadsrijk luchtschip vertrek C 120
/guhs vadsrijk luchtschip haven haven_z3_z4_vertrek
```
- Oostkant van het ronde platform open, het schip ligt ertegenaan, binnen 1 blok van je neus om te klikken.

**Alle havens** (zet eerst de vertrektijd van die lijn op 120 s met `/guhs vadsrijk luchtschip vertrek <lijn> 120`):

| Lijn | Haven | Teleport | Wat je ziet |
|---|---|---|---|
| 0 | Kampsteiger | `/guhs vadsrijk luchtschip haven haven_kamp_z1_vertrek` | noordarm van de kampsteiger, oosteinde open; **lijn 0 vliegt nu** |
| 0 | De Haven | `/guhs vadsrijk luchtschip haven haven_z1_kamp_vertrek` | westeinde van de lange pier in de baai, schip boven het water |
| A | Noordklifhaven | `/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek` | noordrand open, de roze vlag wappert naar buiten |
| A | Kandijkom-dok | `/guhs vadsrijk luchtschip haven haven_z2_z1_vertrek` | zuidrand van het dok open (7 breed) |
| B | Galmdok | `/guhs vadsrijk luchtschip haven haven_z2_z3_vertrek` | oostrand open |
| B | Weststeiger | `/guhs vadsrijk luchtschip haven haven_z3_z2_vertrek` | westkant open, schip niet meer over de mast |
| C | Oosttaksteiger | `/guhs vadsrijk luchtschip haven haven_z3_z4_vertrek` | oostkant open |
| C | Lampionhaven | `/guhs vadsrijk luchtschip haven haven_z4_z3_vertrek` | westrand, schip ertegen (lag 12 blokken verderop) |
| D | Opstijgpier | `/guhs vadsrijk luchtschip haven haven_z4_z5_vertrek` | oost open, Kapitein Hemelhoog |
| D | Walvisrughaven | `/guhs vadsrijk luchtschip haven haven_z5_z4_vertrek` | eind van pier 2: geen hekjespoort, masten lager |
| E | Staarthaven | `/guhs vadsrijk luchtschip haven haven_z5_z6_vertrek` | **nieuw:** oosteinde van het staartpad, tussen de staartvinnen |
| E | Rozenkloof | `/guhs vadsrijk luchtschip haven haven_z6_z5_vertrek` | schip aan de zuidkant, geen poortje, mast lager |

Per haven, in survival: loop het schip op (welkomstbericht), sneak om uit te stappen, klik dan op een plank van de pierpunt (ook
aan boord), laat het schip vertrekken (`/guhs vadsrijk luchtschip vertrek <lijn> 1`) en loop van de punt af: je staat meteen
weer op de pier. De zeilbootjes van zone 4 (X-toets) zijn niet veranderd; alle 11 steigers zijn gecontroleerd.

## 1.6 Kootje, de walvis en het kussen van Roosje
**Kootje Zilverstaart: er weer uit**
```
/tp @s 2587 167 -520
```
- Je staat in de Wolkwaldroom (de vechtzaal in de walvis). Naast de deur, tegen de wand: een **ladder met een open luikje**.
  Rechtsklik de ladder: "Je klimt de ladder op, terug naar het middelste spuitgat." en je staat bij het spuitgat.

**De walvis: lopend naar de staart** (Staartpark, lijn E naar zone 6), lopend:
```
/tp @s 2848 233 -523
```
- Je staat op de Paleisheuvel. Loop naar het oosten: in de borstwering zit een opening met een bordes, dan een **brede witte trap
  langs de klif** naar beneden (twee stukken, met een tussenbordes) tot op het gras van het Staartpark. Daarna verder naar de
  Staarthaven en de staartlift. (Vroeger: een klif van 29 blokken, dus zone 6 was lopend niet te bereiken.)

**Het kussen bij Kloofmoeder Roosje**
```
/execute in guhs:vadsrijk run tp @s 3486 75 -698
```
- Rechtsklik het magenta kussen naast de troon: je gaat zitten en zakt niet meer door de grond. Shift = opstaan: je staat óp of
  naast het kussen, nooit in de vloer. Geldt voor alle kussens, zitzakken en stoelen.

## 1.7 Zonenamen bij een zonewissel
```
/execute in guhs:vadsrijk run tp @s 2669 226 -536
```
- De titel **"De Ordekathedraal - veilig gebied"**, niet meer "vadsrijk.kern.gebied.z5_kathedraal". Ook terug: De Paleisheuvel, De
  Universiteit, De Academie van de Wolkwacht, De Marktlaan, Moeder Wolkwal, De Buikgangen, De Proefarena, Het Schuilnest, de
  Wolkrand-delen, De Staartvlakte. Er is een check (`taalcheck`) die nu 0 ontbrekende teksten meldt.

## 1.8 Figuranten: minder, en ze kijken de goede kant op
```
/guhs vadsrijk z5wereld plek kathedraal_schip
```
- In de kathedraal zitten nog ongeveer 40 % van de kerkgangers, en ze **kijken naar het altaar**. Het koortje kijkt naar de banken.
- Elders in Hoogvads staan ongeveer een kwart minder figuranten. Ridders en de bekende figuren blijven allemaal.
- Ook goed gedraaid: de ridders in de troonzaal (naar het middenpad), het publiek (naar de koningin), de uiltjes-studenten (naar
  hun professor), de twee duellerende ridders (naar elkaar).
```
/execute in guhs:vadsrijk run tp @s 3760 75 -905
```
- Zone 6, de Spiegelzaal in Saffraanpoort: de hofguhs kijken naar het middenpad (stonden met hun neus tegen de muur), de gouden
  guh op de troon kijkt de zaal in. Ruiters kijken dezelfde kant op als hun rijdier.

## 1.9 Duimdop, de kathedraal en de deuren van Saffraanpoort
| Wat | Commando | Wat je ziet |
|---|---|---|
| **Gildemeester Duimdop** (lopend) | `/tp @s 2692 218 -483` | Loop de trap af naar de Gildehal: vlak voor de deur een **vlak bordesje** op deurhoogte, je loopt rechtop naar binnen. Duimdop staat binnen. Geldt voor elke deur met een trap omhoog in zone 5. |
| **Kathedraal: muurtjes en lantaarns** | `/tp @s 2692 221 -520` | Loop de grote trap op naar de Grote Poort: **geen wit muurtje** meer dat 3-4 blokken boven de treden zweeft (een balustrade in de lucht, je liep eronderdoor). Geen lantaarns of kettingen meer óp traptreden of op hoofdhoogte; die aan muren en hoog aan plafonds blijven. Dit geldt in alle zones (~1.300 stuks opgeruimd). |
| **Saffraanpoort: nepdeuren** | `/tp @s 3646 71 -842` | De deuren zonder kamer erachter zijn nu **ijzeren deuren** (met de hand niet open): duidelijk "op slot". Echte huizen houden hun houten deur en een kamer. |
| Opstijgpier: de hemelkapitein (lopend) | `/tp @s 2500 73 -565` | Loop over de pier naar het oosten: het witte hekje is open waar de pier aankomt. |
| Nestkapel-lift (zone 3, Wasja) | `/tp @s 1393 167 -641` | De bovenste liftstop staat niet meer achter het hek van het takdek. |
| Kaaszeppelinwerf: trap naar de kajuit | `/tp @s 1683 212 -803` | De torentrap van Dok 3 heeft overal hoofdruimte (geen kaas/beton op hoofdhoogte). |

## 1.10 De website: geen nepdata meer, en de wiki begint bij zone 1
Open **https://guhs.nl/vadsrijk-preview/** (de verborgen voorbeeldsite; de gewone site is niet aangeraakt).
- **Geen verzonnen spelers meer** (geen Lieke/PluisPiet, geen nep-highscores, geen "Voorbeelddata"-bordjes, geen verzonnen "Event
  van vandaag"). Waar nog geen echte data is, staat "Nog niet live / Live zodra de server de update draait".
- **De spoiler-schakelaar begint altijd bij zone 1** ("Toon tot zone 1"), bij elke pagina. Onderaan verschijnt een briefje: "Alle
  informatie toont alleen de tutorial en de eerste zone. Spoilers of meer informatie? Kies rechtsboven bij Toon tot..." met OK en
  "Niet meer tonen" (dat verbergt alleen het briefje).
- De site is opnieuw gebouwd met de ronde-W-modellen (de vierkante klodders, de Klodderreus, de Blubs) en de nieuwe kaart.

## 1.11 Xp in de Lampionkust (zone 4)
**Antwoord op je vraag:** ja, de Lampionkust brengt je in de zone zelf naar 56. Hoofd + zij + de twee bazen: ~58,5 (was 56,75,
krap). Alleen hoofdquests: ~55,7, daarna nog ~7-10 min mobs aan het Mysterie-eiland. De Koraalgrot (lv 53-55) is de dungeon van
zone 4 (~5.000 xp per run).
```
/guhs vadsrijk level 50
/guhs vadsrijk npc quest klaar z4_hq01_lampionkust
```
- Lever in bij de Havenmeester: **+8.522 xp** (was 5.900), de balk springt ~2/3 vol.
```
/guhs vadsrijk npc quest klaar z4_s03_vissen
```
- **+2.884 xp** (was 2.360).
```
/guhs vadsrijk level 60
/guhs vadsrijk npc quest klaar z6_hq01_land
```
- Zone 6: **+14.030 xp** (was 7.892), bijna een heel level.
- Nog open, voor jou om te beslissen: zone 2 geeft te veel xp, en wie hoofd + zij doet, zit eind zone 5 al op het plafond 70.
  Overzicht: `XP_BALANS.md`, narekenen met `python tools/vadsrijk/xpbalans.py`.

## 1.12 Alle geheimpjes vindbaar (jouw release-eis)
De bereik-check (hoofdstuk 2.4) vond ~9 geheimpjes die je lopend niet kon halen. Die zijn nu allemaal te bereiken:

| Geheimpje | Commando | Wat je ziet |
|---|---|---|
| Zone 1: knuffel **Mosje** (holle boom) | `/execute in guhs:vadsrijk run tp @s -403 72 -100` | Mosje zit op de bosgrond onder de grote paddenstoel (zat 36 blokken hoog bovenop de hoed). |
| Zone 1: **de schat op de Hoedenpaden** | `/guhs vadsrijk wereld plek geheim_hoedenschat`, loop dan terug over de smalle plankenloop | De smalle, donkere plankenloop naar het geheime hutje heeft geen "dichte hoekjes" meer: bij elke knik ligt een extra plank. |
| Zone 1: **de geheime tempelkamer** (Zwamtempel) | `/execute in guhs:vadsrijk run tp @s 320 152 -416` en kijk naar het westen | In de gebarsten muur zit nu een bres van 2 hoog: erdoor en je staat in het kamertje met de kist (niets opende die muur). |
| Zone 2: **de Galmgrot** | `/execute in guhs:vadsrijk run tp @s 927 118 -1002` | Een **trap van sneeuwblokken** omhoog naar de tunnel achter de bevroren waterval (die lag 3 blokken boven de grond). |
| Zone 2: **de IJsgrot** | `/execute in guhs:vadsrijk run tp @s 462 113 -1064` | Je staat op de ijskoepel. In het dak zit een **gat met steigerwerk** naar beneden (de tunnelingang hangt hoog boven het meer). Steiger: springen = omhoog, sneaken = omlaag. |
| Zone 2: aanwijzing **boorgaten** | `/execute in guhs:vadsrijk run tp @s 870 124 -1082` | De aanwijzing ligt aan de voet van de lawine, niet meer bovenop een ijsrichel. |
| Zone 3: **kuiken 3** | `/execute in guhs:vadsrijk run tp @s 1301 222 -626` | Het kuikentje zit aan de bereikbare rand van honingvijver 3 (zat in de steile bladrand van de kruin). |
| Zone 3: **uitkijkpunt op de Westtak** | `/execute in guhs:vadsrijk run tp @s 1299 211 -622` en loop naar het platform | Geen bladeren meer op hoofdhoogte boven het pad naar het roze uitkijkplatform. |
| Zone 4: **vliegerspul 1 en 3** (zijquest z4_s01) | `/execute in guhs:vadsrijk run tp @s 2286 72 -673` en `/execute in guhs:vadsrijk run tp @s 2298 81 -673` | De twee stukken liggen op het pad onder de vliegerrotsen (lagen 6-8 blokken hoger). De glinstering zie je alleen als de zijquest loopt. |

Nog over (zie het eindrapport): het uitkijkpunt op de ijsnaald bij de Galmtop (zone 2, decor) en de schat achter de kristalmuur
in de Zoutgrot (zone 6, die opent de puzzel tijdens het spel).


---

# W2. DE NIEUWE SYSTEMEN VAN RONDE W
## 2.1 Dungeons: een echte dichte deur, een dungeonvenster en een wachtrij (W-GROT)

Het woord is nu **dungeon**: grotten zijn het eerste type dungeon ("Type: Grot"). Elke grot-dungeon (De Zwamgrot, De Kristalgrot, De Wortelgrot, De Koraalgrot, De Wolkengrotten, De Zingende Zoutgrot) heeft nu een
**echte dichte deur**. Je kunt er niet doorheen lopen. **Rechtsklik op de deur** (ook met een wapen in je hand) en het
**dungeonvenster** gaat open. Daarin start je de grot: solo, met je groepje, of via de wachtrij met andere spelers.

> Test in **survival** (in creative zet de grot je er niet uit). De deur verschijnt vanzelf zodra het stukje wereld geladen is
> (een paar seconden). Zie je hem niet: `/guhs vadsrijk dungeon deur <grot> tp` zet hem meteen neer.

### 2.1.1 Naar elke deur

| Grot | Deur | Teleport (je staat voor de deur) |
|---|---|---|
| De Zwamgrot (z1) | in de stam van de reuzenpaddenstoel, 3 breed x 3 hoog | `/guhs vadsrijk dungeon deur zwamgrot tp` |
| De Kristalgrot (z2) | in de tunnel achter de kristallen boog (6 blokken naar binnen) | `/guhs vadsrijk dungeon deur kristalgrot tp` |
| De Wortelgrot (z3) | in de holle wortelstronk (noordkant), 3 x 3 | `/guhs vadsrijk dungeon deur wortelgrot tp` |
| De Koraalgrot (z4) | in de koraalschoorsteen op de atolrand (de droge weg); de duikingang op de zeebodem heeft óók een deur | `/guhs vadsrijk dungeon deur koraalgrot tp` |
| De Wolkengrotten (z5) | een **wolkenluik**: loop het blaasgat in, de wenteltrap af; het luik zit in de schacht | `/guhs vadsrijk dungeon deur wolkengrot tp` |
| De Zingende Zoutgrot (z6) | onderaan de uitgehakte trap, vlak voor de grote zaal, 3 x 4 | `/guhs vadsrijk dungeon deur zoutgrot tp` |

Coördinaten (staanplek voor de deur): Zwamgrot -412 69 148 · Kristalgrot 869 119 -1104 · Wortelgrot 1410 69 -737 ·
Koraalgrot 2230 71 -596 · Wolkengrotten 2494 205 -520 (op het luik) · Zoutgrot 3874 50 -521.
Lijst van alle deuren: `/guhs vadsrijk dungeon deur`

Wat je moet zien:
- Een dichte deur in het thema van de grot (paddenstoelhout, kristal, donker wortelhout, koraal/prismarine, wolkenmarmer, zout met
  messing). In het midden zit een **klopper** (ring). Bij de Wolkengrotten is het een **luik** in de vloer.
- Je kunt er niet doorheen lopen of hem kapotslaan.

### 2.1.2 Het dungeonvenster

Rechtsklik op de deur (of zonder lopen: `/guhs vadsrijk dungeon deur zwamgrot`). Je ziet:
- bovenin (roze balk): **"Dungeon: De Zwamgrot"**, daaronder **"Type: Grot · zone 1"**;
- **Aanbevolen: level 11-14 · 1-4 spelers (alleen goed te doen)** (zone 3+: "2-4 spelers (alleen is pittig)");
- **Eindbaas: Moeder Glimhoed** + haar ondertitel;
- **Buit bij elke clear**: munten (zone 1: 700 brons), 1 uitrustingsstuk (je eerste clear: Episch; daarna Zeldzaam met 12% kans op
  Episch), 3-5 ertsen, 25% kans op de bazenkern;
- **Zeldzaam (persoonlijk)**: het huisdier (bijv. Glimhoedje: 6% per clear · pech 0/25) en de Pluimguh-kleur (bijv. Glimzwam: 10%
  per clear · pech 0/15, of "pas als je een Pluimguh hebt", of "✔ al van jou"). Dezelfde getallen als `/guhs vadsrijk loot kansen`;
- **Jij: N keer uitgespeeld** en je **wachttijd** ("Geen wachttijd" of "Wachttijd: nog 9:12");
- de status: "De dungeon is vrij." / "Bezet: een groep van 2 is binnen (3:10)." / "Jij staat in de wachtrij: 2/4";
- knoppen: **Solo starten**, **Starten met groep (n)**, **In de wachtrij** (of **Uit de wachtrij** + **Nu starten (2/4)**).
  Beweeg over een knop voor uitleg.

### 2.1.3 Solo

Klik **Solo starten**: je staat meteen binnen (vlak achter de deur), de titel van de grot verschijnt. Loop de grot in: de run
start ("⚔ De grot wordt wakker!") en gaat verder zoals in SPIEK_U_GROTKERKER (2 kamers, poort, baas, eindkist).
Dev-snelweg (geen wachttijd/regels): `/guhs vadsrijk grot run zwamgrot`

Na de baas:
- chat: "Wachttijd voor dungeon De Zwamgrot: 10:00" en "Dungeon uitgespeeld! Rechtsklik op de deur ... om naar buiten te gaan";
- na 3 seconden opent het venster vanzelf met **Dungeon verlaten** / **Nog even blijven**;
- **Dungeon verlaten** = je staat buiten op de staanplek voor de deur. Je wordt NIET meteen weer naar binnen gezet.
- Rechtsklik nu op de deur: je ziet "Wachttijd: nog 9:5x" en de startknoppen staan grijs.
- Wachttijd weg: `/guhs vadsrijk dungeon wachttijd weg` (alle grotten) of `/guhs vadsrijk dungeon wachttijd weg zwamgrot`
- Wachttijd zetten om het venster te zien: `/guhs vadsrijk dungeon wachttijd zwamgrot 90`

### 2.1.4 De wachtrij alleen testen (nep-spelers)

```
/guhs vadsrijk dungeon nep zwamgrot 3
```
- Er komen 3 nep-spelers ("Testguh 1..3") in de wachtrij van De Zwamgrot, en jij ook: de rij is 4/4.
- Meteen: **Klaar voor De Zwamgrot?** venster met de 4 namen (… = wacht), een tijdbalk van 30 s, en **Klaar!** / **Nee**.
- Klik **Klaar!**: na een seconde zeggen de nep-spelers ook ja (✔). Jullie gaan naar binnen; in de chat: "(Testguh 1 is een
  nep-testspeler en blijft buiten)". Je bent alleen binnen (de run telt als 1 speler).

Weigeren testen:
```
/guhs vadsrijk dungeon nep zwamgrot 3 nee
```
- Klik **Klaar!**: "Testguh 1, Testguh 2, Testguh 3 doet (nog) niet mee. De anderen blijven in de wachtrij." Je staat nog in de
  rij (1/4), de nep-spelers zijn eruit.

Te laat testen:
```
/guhs vadsrijk dungeon nep zwamgrot 3 niets
```
- Klik **Klaar!** en wacht 30 s: "Niet iedereen was op tijd klaar...". Jij blijft in de rij.

Eerder starten (niet vol):
```
/guhs vadsrijk dungeon nep zwamgrot 1
/guhs vadsrijk dungeon deur zwamgrot
```
- De rij is 2/4. Klik in het venster **Nu starten (2/4)** → klaar-check voor 2 → Klaar! → naar binnen.

Wachtrij-status:
- In de wachtrij zie je elke 2 seconden in de actiebalk: **"Wachtrij dungeon De Zwamgrot 1/4"** (en "· bezet" als er een groep binnen is).
- Je mag rondlopen; je blijft in de rij. Uitloggen of naar een andere wereld = uit de rij ("Je bent uit de wachtrij ... (andere wereld)").
- **Uit de wachtrij** in het venster haalt je eruit.
- Overzicht van alles: `/guhs vadsrijk dungeon rij`

### 2.1.5 Met je groepje

Maak een groepje (party) met een tweede speler. De leider klikt **Starten met groep (2)** → iedereen krijgt de klaar-check (de
leider staat al op ✔) → allemaal Klaar! → samen naar binnen. Iemand op Nee → "... doet (nog) niet mee. Starten gaat niet door."
Heeft iemand nog wachttijd of zit iemand al in een grot/de raid, dan zegt de knop dat (en wie).

### 2.1.6 Regels die je kunt proberen

- **Niet-leden komen er niet in**: zit er een groep in De Zwamgrot, dan zegt de deur voor een ander "Bezet: een groep van 1 is
  binnen". (`/guhs vadsrijk grot tp zwamgrot hal` maakt je zelf lid: dat is een dev-snelweg.) Teleporteer in survival
  met `/tp @s -482 47 130` de Zwamgrot in terwijl een andere groep binnen is: je staat meteen weer buiten de deur ("Deze dungeon is van een
  andere groep"). Zonder groep binnen: "Deze dungeon is dicht. Start hem via de deur."
- **Flauwgevallen?** Je wordt buiten wakker (zoals altijd). Rechtsklik op de deur: **Terug naar binnen** brengt je terug bij je groep.
- **Groep weg** (iedereen een minuut buiten de grot, na een clear 10 s): "Jullie groep is weg: de dungeon gaat weer dicht."
- **Uitgespeeld en er wacht een andere groep**: "Er wacht een andere groep: de dungeon gaat over 2:00 dicht" → daarna sta je buiten.
- Niet starten terwijl je al in een grotkerker of de raid zit.

### 2.1.7 Dev-commando's (kort)

| Commando | Wat |
|---|---|
| `/guhs vadsrijk dungeon deur` | lijst van alle deuren (staanplek + binnen) |
| `/guhs vadsrijk dungeon deur <grot>` | het dungeonvenster openen |
| `/guhs vadsrijk dungeon deur <grot> tp` | naar de staanplek voor de deur (en de deur meteen neerzetten) |
| `/guhs vadsrijk dungeon nep <grot> <1-3> [ja\|nee\|niets]` | nep-spelers in de wachtrij (jij erbij) |
| `/guhs vadsrijk dungeon rij` | alle grotten: bezet door wie, wachtrijen, klaar-checks |
| `/guhs vadsrijk dungeon wachttijd weg [grot]` | wachttijd weg |
| `/guhs vadsrijk dungeon wachttijd <grot> <seconden>` | wachttijd zetten |
| `/guhs vadsrijk dungeon verlaat` | uit je grotkerker (naar buiten) |
| `/guhs vadsrijk dungeon start <id>` (of `grot run <id>`) | dev: solo naar binnen, zonder wachttijd/regels |

> Alle `dungeon`-commando's werken ook met `grot` in plaats van `dungeon` (bijv. `/guhs vadsrijk grot nep zwamgrot 3`).

## 2.2 De Party-tab en de nep-spelers (W-PARTY)

De koppeling met de dungeons is gedaan (w-int): "Party in rij" en het vullen van de wachtrij met nep-spelers werken.

### 2.2.1 De Party-tab
- `H` (Vadsrijk-menu) -> tabblad **Party** (taart-icoon), of `/guhs vadsrijk party`.
  - Zonder party: links uitleg, rechts "Spelers in het Vadsrijk" met **Uitnodigen** (dichtbij eerst, met afstand).
  - Met een tweede account/speler: klik **Uitnodigen** -> bij de ander een **toast** "Party-uitnodiging" + belletje + in de chat
    **[Accepteer] [Weiger]** (klikbaar). In diens tab staat de uitnodiging ook met knoppen.
  - Na accepteren: links beide leden, kroon ♛ bij de leider, groene stip = online, regel "Lv · klasse · spec · zone".
  - Als leider: knoppen **Leider** en **Weg** per lid. Iedereen: **Party verlaten**.
- Commando's werken nog: `/guhs vadsrijk party uitnodigen <speler>`, `... accepteer`, `... weiger`, `... verlaat`,
  `... verwijder <naam>`, `... leider <naam>`.

### 2.2.2 Party bewaard na een herstart
- Maak een party met 2 spelers, sluit de wereld/server en start opnieuw: de party staat er nog (zelfde leider, zelfde volgorde).
- Uitloggen = niet meer uit de party: je frame wordt bij de ander grijs ("offline"). Logt de leider uit, dan wordt de ander leider.

### 2.2.3 Samen in de wachtrij
- Raid (leider, min. 2 spelers in het Vadsrijk): tab -> **Raid: party in de rij** (of `/guhs vadsrijk party raid`).
  Verwacht: "De party staat in de rij..." en de raid-melding "je staat in de rij (#1)". Is de toren vrij: binnen een minuut naar
  De Wachtzaal, dan gaan jullie samen naar boven.
- Dungeon: kies met < > een dungeon -> **Party in rij**; bovenin "In de rij voor: <dungeon>".

### 2.2.4 Nep-spelers
- Snel een paar naast je: `/guhs vadsrijk nepspeler spawn` (willekeurige rol) of `/guhs vadsrijk nepspeler spawn heler 3`.
  Kijk naar: speler-model met standaard-skin, naambordje `[NPC] xX_Vadsjesloper_Xx · Heler · Lv 12`, iets in de hand.
- In de wildernis vanzelf: loop een mobzone in (geen stad; bv. de Klodderweide naast Zwamzoet) en blijf een paar minuten bij mobs. Zeldzaam: max 2-3 per zone, ze vechten tegen de mobs daar en kletsen ("lfg dungeon??", "brb mama roept").
  Chat verschijnt boven hun hoofd en in de chat als `[Nep] naam: ...`.
- Rekruteren: rechtsklik er een -> "hoi" + **[Kom je mee?]** -> klik -> "gaat met je mee!" en hij staat in je party-frames en in de
  Party-tab met **[NPC]** en "nog 24 min". Of direct: `/guhs vadsrijk nepspeler rekruteer`.
  - Hij volgt je, valt aan wat jij raakt; een heler heelt je (hartjes), een tank trekt mobs naar zich toe (boos wolkje).
  - Laat hem bijna "doodgaan": hij zakt 10 s door de knieën ("rip") en staat weer op.
  - Jouw nep-speler rechtsklikken: resterende minuten + **[Doei!]**. Als leider kun je hem ook in de tab **Weg**-klikken.
  - Na 20-30 min: afscheidsregel ("moet eten, doei!") en weg.
  - Loot: als hij de laatste klap geeft krijg JIJ gewoon xp + loot; hij neemt nooit iets.
- Wachtrij vullen: ga alleen in een dungeon-rij en wacht 1 minuut: "Niemand anders kwam opdagen: 3 nep-speler(s)
  vullen de rij...". Direct: `/guhs vadsrijk nepspeler vul`. In de dungeon lopen ze met je mee; ga je eruit, dan zeggen ze "gg" en
  zijn weg. Nooit in de raid.
- Overzicht: `/guhs vadsrijk nepspeler lijst`; allemaal weg: `/guhs vadsrijk nepspeler weg`.

### 2.2.5 Aan/uit
- Per speler: Vadsrijk-menu > **Instellingen** > **Nep-spelers** uit. Verwacht: alle nep-spelers verdwijnen voor jou (niet meer te
  zien of aan te klikken), geen `[Nep]`-chat, je eigen metgezellen gaan naar huis, ze vullen jouw rij niet.
  (Zonder client-knop testen: `/guhs vadsrijk nepspeler mij uit` / `mij aan`, alleen server-kant.)
- Hele server: `/guhs vadsrijk nepspeler uit` -> iedereen gaat weg met een afscheidsregel, er komt niets meer bij;
  `/guhs vadsrijk nepspeler aan` zet ze terug. Staat ook in `serverconfig/guhs-vadsrijk-nepspelers.toml`.

### 2.2.6 Uitloggen = niet meer uit de party
- Let op, dit is anders dan vroeger: wie uitlogt blijft in de party (grijs frame, "offline"). Logt de leider uit, dan wordt een
  ander leider. Weg uit de party gaat met **Party verlaten** in de tab of `/guhs vadsrijk party verlaat`.

## 2.3 Elke dungeon is één gedeelde ruimte
- Per grot kan er één groep tegelijk binnen. Een tweede groep komt in de wachtrij ("Bezet: een groep van 2 is binnen"). Zo
  bedoeld voor 1.10.0; zie het eindrapport.

## 2.4 De bereik-check: kun je overal lopend komen? (W-BEREIK)
Een nieuwe controle over de hele kaart (zone 1-6) met het lijf van een survival-speler: 1,8 hoog, een stap van 1 blok alleen
met hoofdruimte, maximaal 3 blokken vallen, hekken en lantaarns blokkeren, ijzeren deuren zijn dicht. Hij rekent voor elke
quest-plek, NPC, geheimpje, arena en luchtschip-haven uit of je er lopend kunt komen.
```
python tools/vadsrijk/bereikcheck.py
python tools/vadsrijk/bereikcheck.py --strikt
python tools/vadsrijk/bereikcheck.py --punt 2650,224,-520
```
- Schrijft `BEREIK_RAPPORT.md`: per zone hoeveel hoofdquest-doelen, zijquest-doelen, NPC's en geheimen je lopend haalt, en per
  gemist doel de dichtstbijzijnde plek waar je wel komt. `--strikt` stopt met een fout als een hoofdquest-doel of NPC niet te
  halen is. De uitslag van deze ronde staat in `EINDRAPPORT_RONDE_W.md`.
- Hij draait ook mee in `maak_kaart.py --audit`, dus na elke kaart-regen.
- Daarnaast ruimt een nieuwe pas (`vrijpad.py`) bij elke regen lantaarns/kettingen op trappen en paden en zwevende hekjes op.

---

# DEEL B: DE HOOFDSTUKKEN UIT RONDE U (nog steeds geldig)

Hieronder de spiekbrief van ronde U, ongewijzigd (alleen de hoofdstuknummers kregen een B). Waar ronde W iets veranderde,
staat dat in hoofdstuk 1 en 2 hierboven.

# B1. START EN DE NIEUWE WERELD

Eerst hoe alles aan elkaar hangt (zee, overgangen, havens, hekken), dan de luchtschepen, je karakters, het nieuwe Vadsrijk-menu (H) en de toetsen. Ronde U staat bovenaan, de blokken van de nachtronde daaronder (die gelden nog steeds).

### U-OVERGANG: natuurlijke zone-overgangen + epische uitkijkpunten

**Belangrijk:** dit werkt alleen in een **nieuwe Vadsrijk-wereld** (of op plekken waar nog geen chunks waren). Bestaande chunks
houden de oude zee en de oude biomen.

#### 1. De naden tussen de zones
Wat er veranderd is:
- **Waterkleur, lucht en mist** verlopen nu geleidelijk over de hele zee tussen twee zones (8 stapjes), in plaats van een
  rechte lijn waar de kleur ineens omslaat. De zee rond de Boom (zone 3) is niet meer turquoise.
- **De zeebodem** sluit overal aan: geen onderwaterklif meer op de tegelgrens, geen ineens andere bodemkleur. De diepe kom van
  zone 4 loopt met een golvende rand af naar de open zee.
- **De wolkenzee van zone 3** stopt niet meer op een rechte lijn, maar rafelt uit en dunt uit in de open zee (ook aan de
  Splinterpieken-kant van de Wolkenkloof).
- **Losse rotsjes** steken hier en daar uit de open zee bij de kusten.

Kijk vooral van bovenaf (vliegen, of vanaf een hoge plek) en onder water. Druk **F3**: de biome-naam op open zee is
"Het Vadsrijk - De Zee".
```
/guhs vadsrijk indeling rand wolkenkloof_noord
/guhs vadsrijk indeling rand wolkenkloof
/guhs vadsrijk indeling plek haven_z2_z3_vertrek
```
- **Wolkenkloof:** de wolkbanken langs de kloof lopen door tot in zone 2 en waaieren uit. Geen rechte wolkenrand meer, en het
  water verkleurt langzaam van ijsblauw (zone 2) naar lila/honing (zone 3).
- **Onder het Galmdok** (`haven_z2_z3_vertrek`): kijk naar beneden en naar het oosten. De bodem loopt rustig door, geen
  rechte afgrond.

Meer naden om te bekijken (vlieg rond, kijk naar water, bodem en wolken):
```
/execute in guhs:vadsrijk run tp @s 1024 90 -1040
/execute in guhs:vadsrijk run tp @s 1730 90 -500
/execute in guhs:vadsrijk run tp @s 1400 90 -110
/execute in guhs:vadsrijk run tp @s 380 90 -1100
/execute in guhs:vadsrijk run tp @s 700 90 -740
/execute in guhs:vadsrijk run tp @s 2140 90 -250
/execute in guhs:vadsrijk run tp @s 2140 90 -780
/execute in guhs:vadsrijk run tp @s 2560 90 -420
/execute in guhs:vadsrijk run tp @s 900 90 -400
/execute in guhs:vadsrijk run tp @s -380 90 860
```
1. Zone 2 / zone 3, noordkant van de Wolkenkloof (x 1024).
2. Zone 3 / zone 4, de Wolkenval (x 1728): de wolkjes van zone 3 dunnen uit richting de Lampionkust.
3. Zuidrand van zone 3 (z -129): de wolkenzee rafelt uit.
4. Westkant van zone 2 (x 384).
5. Splinterwand / zone 2 (z -768).
6. Zuidrand van zone 4 (z -257).
7. Noordrand van zone 4 (z -768).
8. Oostrand van zone 4 onder Hoogvads (x 2560).
9. Open zee tussen zone 1 en zone 3 (hier staan losse rotsjes).
10. Zee tussen Knuffelkamp en zone 1.

#### 2. Uitkijkpunten (W3): nu op epische plekken
Eerste keer bovenaan = panorama-filmpje + "uitkijkpunt gevonden" met de **naam** (geen rare tekst meer).
```
/guhs vadsrijk indeling plek uitkijk_hoedtop
/guhs vadsrijk indeling plek uitkijk_nest
/guhs vadsrijk indeling plek uitkijk_galmtop
/guhs vadsrijk indeling plek uitkijk_kroon
/guhs vadsrijk indeling plek uitkijk_vuurtoren
/guhs vadsrijk z5verhaal tp ooggaarde_balkon
```
- **De Reuzenhoed** (NIEUW, Zwamzoet): neem de **tempel-lift** omhoog. Op het voorplein van de tempel loopt een
  plankenpad met reling over de rand van de hoed naar een rond balkon op palen. Uitzicht over heel Zwamzoet en de zee.
  Test: lift op, pad af, balkon op, zonder springen.
- **Het Kruinnest** (bovenop de Boom): de naam klopt nu. De reling en de 2 lantaarns staan óp de nestvloer (niet meer
  zwevend), en de reling is één doorlopend hek.
- **Galmtop:** de hoogste naald van de Splinterpieken, met de wenteltrap omhoog.
- **De Kroon**, **De Vuurtorengalerij** (zone 4, wenteltrap in de vuurtoren; heeft nu een naam), **Ooggaarde-balkon** (Hoogvads).
- Zone 4 heeft nu ook namen voor **De Lampionduin**, **De Pierkop** en **De Vliegerheuvel**:
```
/guhs vadsrijk z4wereld plek uitkijk_lampionduin
/guhs vadsrijk z4wereld plek uitkijk_pierkop
/guhs vadsrijk z4wereld plek uitkijk_vliegerheuvel
```
- **Weg:** de uitkijkpunten bij de ballonhavens (Noordkliftop, Galmkam, Kandijkom-balkon, Westtak, Oosttak). De havens
  zelf zijn er nog gewoon.
### U-HEKKEN: hekken, muurtjes, tralies, kettingen en vlaggetjes zijn verbonden

**Wat is er veranderd?** Overal in het Vadsrijk (zone 1 t/m 5) zijn hekken, muurtjes, ijzeren tralies, glaspanelen,
kettingen, vadstouw, guh-hekjes, vlaggetjes en feestslingers nu **verbonden lijnen** in plaats van losse paaltjes.

- **Schuine stukjes in een hekrij** (bijvoorbeeld een ronde reling) krijgen een hoekpaaltje. Dat komt aan de buitenkant,
  boven de afgrond, zodat het looppad vrij blijft.
- **Een trapje van 1 omhoog** krijgt een verhoogd paaltje, zodat de reling doorloopt.
- **Twee losse lantaarnpalen** schuin naast elkaar blijven los. Dat hoort zo.

Kijk overal of het hek een **doorlopende lijn** is (de planken lopen van paal naar paal), ook precies op de tegelgrenzen
(om de 64 blokken).

**Uitkijkpunt in de kruin (zone 3, `uitkijk_nest`):** de reling rondom moet één ring zijn.

```
/guhs vadsrijk indeling plek uitkijk_nest
```

**Wenteltrap in de boom en het Honingtak-platform (zone 3):** de relingen langs de trap en rond het platform zijn
doorlopend. Loop de trap op en af: er staat nergens een paaltje óp de trap.

```
/execute in guhs:vadsrijk run tp @s 1383 168 -519
/execute in guhs:vadsrijk run tp @s 1425 168 -542
/guhs vadsrijk indeling plek uitkijk_kroon
/guhs vadsrijk indeling plek uitkijk_westtak
```

**Vlaggetjes en feestslingers (zone 2, Splinterpieken):** de slingers hangen als één lint tussen de palen, niet dwars.

```
/execute in guhs:vadsrijk run tp @s 698 104 -1043
/execute in guhs:vadsrijk run tp @s 824 109 -1058
/execute in guhs:vadsrijk run tp @s 585 104 -951
```

**Tralies en glas (zone 2 en zone 3):** de tralies zijn een dicht rooster, geen losse staafjes.

```
/execute in guhs:vadsrijk run tp @s 817 115 -1123
/execute in guhs:vadsrijk run tp @s 1640 193 -866
```

**Lampionkust (zone 4):** vlaggetjes, slingers en vadstouw lopen als één lijn.

```
/execute in guhs:vadsrijk run tp @s 1848 74 -452
/execute in guhs:vadsrijk run tp @s 1876 70 -478
/execute in guhs:vadsrijk run tp @s 1998 73 -600
```

**Hoogvads (zone 5):** de muurtjes langs de wegen sluiten aan. Hoge stukjes onder een blok zijn "tall".

```
/execute in guhs:vadsrijk run tp @s 2851 259 -564
/execute in guhs:vadsrijk run tp @s 2680 226 -555
```

**Knuffelkamp en Zwamzoet (zone 1):** de hekken rond de kampen en tuintjes zijn verbonden.

```
/execute in guhs:vadsrijk run tp @s -51 129 -32
/execute in guhs:vadsrijk run tp @s 49 124 -43
/execute in guhs:vadsrijk run tp @s -525 85 -39
```

**Door code geplaatste stukken:**
- de Rustende Ridders-zaal;
- de puzzels;
- de Z5-proefarena: de hekring rond de proefarena is nu één ring, met de vier open doorgangen;
- terugzetten na een baasgevecht.

Bij al deze stukken zijn hekken en muurtjes ook meteen verbonden.

**Als je toch nog een los paaltje ziet:** noteer de coördinaten (F3). De controle
`python tools/vadsrijk/audit/verbindcheck.py` hoort 0 te geven.
### Luchtschip: overal klikbaar, vrij rondlopen op het dek, duidelijke "nog dicht"-melding (U-LUCHTSCHIP)

**Wat is er nieuw?**
- Je kunt het luchtschip nu **overal** aanklikken om in te stappen: de romp, het dek, de kajuit, de ballon (ook de neus met het gezicht, de staart en de oren). Rechtsklik en linksklik doen allebei hetzelfde. Linksklik slaat het schip nooit.
- Aan boord kun je **vrij rondlopen** over het dek (WASD, rennen gaat iets sneller), ook tijdens de vlucht. Je benen lopen gewoon mee. De reling houdt je tegen: je kunt niet van het schip lopen of vallen, en springen doet niets. Andere spelers aan boord zie je ook rondlopen.
- Is een lijn nog **dicht** voor jou? Klik op het schip, op de pier of op het vertrekbord, dan zegt de chat precies wat je eerst moet doen, bijvoorbeeld: "Lijn A is nog dicht voor jou. Volg eerst de hoofdquest: na "Een grauwe schaduw" kun je hier vliegen. Je bent nu bij: "Welkom, rekruut!". En je moet level 20 zijn (jij: level 3)."
- Bij de pier zegt de **actiebalk** of JIJ mee mag: groen "✔ Jij mag mee: klik ergens op het schip!" of "✖ nog dicht voor jou: volg eerst de hoofdquest tot na ...".
- Het **vertrekbord** heeft een 4e regel: "Mee na de hoofdquest "...", vanaf level ..." (aan de andere kant: "Terugvliegen mag altijd"). Je kunt het bord ook aanklikken.
- Lijn C en D gebruiken nu dezelfde regels als de kapiteins: C na De Schemerwortel + level 50, D na de Lampionkust + level 55.

**Welke lijn gaat open na welke hoofdquest?**

| Lijn | Heenweg open na | Level |
|---|---|---|
| 0 Kampsteiger → Haven Zwamzoet | "De Grauwe Klodderreus" | - |
| A Noordklifhaven → Kandijkom-dok | "Een grauwe schaduw" | 20 |
| B Galmdok → Weststeiger | "Een roze splinter" | 30 |
| C Oosttaksteiger → Lampionhaven | "Vahoegnir komt thuis" | 50 |
| D De Opstijgpier → Walvisrughaven | "De Opstijgpier" | 55 |

**1. Overal klikken** (in creative, dan is elke lijn open):
```
/gamemode creative
/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek
/guhs vadsrijk luchtschip vertrek A 60
```
- Klik met **rechts op de ballon** (hoog boven je): je staat aan boord.
- Sneak om weer uit te stappen (je komt terug op de pier). Klik nu met **links op de onderkant van de romp** of op de kajuit: weer aan boord. Het schip krijgt geen klap.
- Probeer ook de neus van de ballon (het gezicht), de staartvinnen en de oren.
- **Kijk naar:** loop over de pier naast het schip en klik op blokken van de pier. Dat moet gewoon werken (het schip zit niet in de weg en duwt je niet weg).

**2. Vrij rondlopen op het dek:**
```
/guhs vadsrijk luchtschip rij A
```
- Loop met WASD over het dek, van de kajuit tot de punt van de boeg en van reling tot reling. Ren ook eens (sprinttoets).
- **Bij vertrek en tijdens de vlucht:** blijf lopen. Het moet soepel gaan, zonder terugspringen of haperen, ook als het schip draait.
- Probeer over de reling te lopen: dat lukt niet. Springen doet niets. Sneaken geeft "Njeg! Blijf lekker aan boord".
- Eten en items gebruiken aan boord werkt gewoon (een klik op het dek wordt niet meer "opgegeten").
- **Met twee spelers** (als dat kan): je ziet elkaar rondlopen, met lopende benen.
- **Uitloggen tijdens de vlucht:** log uit en weer in. Je staat op de pier van de bestemming.

**3. Een lijn die nog dicht is** (een nieuwe speler, of reset je eigen quests):
```
/gamemode survival
/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek
```
- **Actiebalk bij de pier:** "Lijn A naar Kandijkom-dok: vertrek over ... ✖ nog dicht voor jou: volg eerst de hoofdquest tot na "Een grauwe schaduw"".
- **Klik op het schip** (als het ligt): je stapt niet in. De chat legt uit: "Lijn A is nog dicht voor jou. Volg eerst de hoofdquest: na "Een grauwe schaduw" kun je hier vliegen. Je tracker rechtsboven wijst de weg. Je bent nu bij: "...". En je moet level 20 zijn (jij: level ...)."
- **Klik op een blok van de pier** of op het vertrekbord: dezelfde uitleg (hooguit eens per 3 seconden in de chat).
- Probeer ook de andere havens:
```
/guhs vadsrijk luchtschip haven haven_kamp_z1_vertrek
/guhs vadsrijk luchtschip haven haven_z2_z3_vertrek
/guhs vadsrijk luchtschip haven haven_z3_z4_vertrek
/guhs vadsrijk luchtschip haven haven_z4_z5_vertrek
```
- **De terugweg** is altijd open. Probeer bijvoorbeeld `haven_z2_z1_vertrek`: de actiebalk is groen, en het bord zegt "Terugvliegen mag altijd".
- In **creative** is alles open (zoals eerst).

**4. Het vertrekbord:**
```
/guhs vadsrijk luchtschip haven haven_z2_z3_vertrek
```
- **Kijk naar:** er zijn 4 regels: lijn, vertrektijd, instappen, en "Mee na de hoofdquest "Een roze splinter", vanaf level 30". De tekst staat op dezelfde plek als eerst.
- Klik op het bord (rechts of links). Mag je mee, dan zegt de chat "Lijn B naar Weststeiger: jij mag mee! Vertrek over ...". Anders krijg je de uitleg.
### Karakters: drie vaste ridders (U-KARAKTER)

Nieuw: je hebt 3 vaste plekken, elk met een eigen spelmodus die NOOIT verandert:
- **links = Normaal**, **midden = Knuffelmodus** (☁, halve schade, 10% minder xp), **rechts = Eénvads** (❤, één leven, 10% meer xp).
- Je bestaande ridder staat automatisch in het slot van zijn modus (meestal links).
- Een Eénvads-ridder die flauwvalt buiten een stad gaat met pensioen: standbeeld in de Rustende Ridders-zaal, je speelt
  daarna verder met je linker (of middelste) ridder, en rechts kun je een nieuwe Eénvads-ridder maken.
- Wisselen: in een stad of bij een kampvuur (zoals het was).

#### 1. Het welkomstscherm (de eerste keer, zonder commando)
Nieuwe wereld, eerste keer door het portaal. Of in je bestaande wereld:
```
/guhs vadsrijk karakter welkom vergeet
```
en dan het Vadsrijk uit en weer in. Wat moet gebeuren: NA het openingsfilmpje (of na ~2 s als er geen filmpje is) gaat
"Je ridders" vanzelf open met bovenin "Welkom in Het Vadsrijk! Kies hoe je wilt spelen...". In de chat staat waar je het
later terugvindt. Direct openen kan ook:
```
/guhs vadsrijk karakter welkom
```

#### 2. Een ridder maken (de bug van de eerste keer)
In het scherm: middelste kaart "Maak een Knuffelridder" -> naam typen -> **Enter** (of "Maken en spelen").
Kijk: het scherm sluit, je staat als nieuwe ridder bij de Knuffelkamp en het verhaal begint opnieuw.
Mag het hier niet (bv. tijdens een filmpje)? Dan staat de rode reden nu BOVEN de knoppen in het maakpaneel.
Elke kaart toont bovenin zijn vaste modus (gekleurde balk; muis erop = uitleg). Er is geen "Makkelijker..." meer.

#### 3. Het scherm vinden zonder commando
- Inventaris (E) in het Vadsrijk: derde knopje **⚔** links (onder knapzak en materiaaltas).
- Rijdieren/Huisdieren-scherm: tabblad **Ridders** helemaal links in de tabbladenrij.
- **Shift + rechtsklik op een kampvuur** (bij je eerste kampvuur komt hier 1x een tip over).
- Praat met de Grootmeester (ordetempel Zwamzoet) of een kampwacht: "Karakters: wissel van ridder of maak een nieuwe".
  De Rekruteerder (Knuffelkamp): "Karakters: hoe zat dat met die drie ridders?".
- Werver Wimpel bij het portaal vertelt het nu ook (3 nieuwe pagina's aan het eind). Opnieuw horen:
```
/guhs vadsrijk npc werver vergeet
```

#### 4. Eénvads met pensioen
```
/guhs vadsrijk karakter maak eenvads Dappere Dirk
/guhs vadsrijk karakter wissel eenvads
/guhs vadsrijk karakter sterf-test
```
Kijk: titel "Met pensioen!", chatbericht voor iedereen, en ~8 s later wissel je vanzelf naar je linker ridder en gaat het
scherm open. De rechterkaart: "✦ Dappere Dirk - Rust in de Rustende Ridders-zaal" met de knop "Nieuwe Eénvads-ridder".
"Spelen" kan niet meer bij hem. Maak een nieuwe:
```
/guhs vadsrijk karakter maak eenvads Dirk Twee
/guhs vadsrijk karakter archief
```
`archief` toont dat de oude Dirk bewaard is (met data).

#### 5. De Rustende Ridders-zaal (nu in de wereldgeneratie)
```
/guhs vadsrijk karakter zaal tp
```
Je staat BINNEN in de zaal (Zwamzoet, west van het plein, naast de Kruidenier, deur naar de havenpromenade).
Kijk: roze baksteen + kersenhouten dak met dichte gevels, glas-in-lood, hartjestegels met een roze loper, 11 kwarts sokkels
met de beelden, hanglampen, bloempotjes achterin; buiten een klinkerpad, twee lantaarnpaaltjes en twee guhvaandels.
Nep-ridders om beelden te zien:
```
/guhs vadsrijk karakter zaal test
/guhs vadsrijk karakter zaal lijst
/guhs vadsrijk karakter zaal leeg
```
LET OP: de zaal zit in de kaarttegels. Een wereld waarvan Zwamzoet al eerder gegenereerd was, heeft hem niet: dan
eenmalig `/guhs vadsrijk karakter zaal bouw` (noodgreep). In een nieuwe wereld hoort dat NIET nodig te zijn.

#### 6. Overige dev-commando's
```
/guhs vadsrijk karakter lijst
/guhs vadsrijk karakter wissel normaal
/guhs vadsrijk karakter naam knuffel Sir Pluis
/guhs vadsrijk karakter migratie-test
/guhs vadsrijk karakter v1-test
```
`v1-test` controleert de omzetting van de oude (vrije) slots naar de vaste: allemaal "OK" verwacht.
### Het Vadsrijk-menu (U-TABS): alles in één scherm

**Wat is er nieuw?** Eén groot roze scherm met hoofdtabs bovenaan:
Karakter · Skillboom · Questlog · Verkenningsboek · Rijdieren (+ Zeilbootje) · Huisdieren · Kledingkast · Knapzak (+ Materiaaltas) ·
Ambachten · Puzzels · Ridders · Reiskaart · Instellingen.
Elke tab heeft HETZELFDE kader. Het kader groeit mee met je venster en schaalt netjes kleiner bij GUI-schaal 4 of een klein venster.

**Openen**
- **H** = het menu (eerste keer op Karakter, daarna op de tab waar je het laatst was).
- De oude toetsen werken nog en openen het menu op hun eigen tab: **K** Skillboom, **J** Questlog, **B** Verkenningsboek,
  **N** Rijdieren, **P** Huisdieren, **O** Kledingkast, **I** Knapzak, **Y** Ambachten.
- **In het menu:**
  - druk de toets van een ANDERE tab om ernaartoe te springen;
  - de toets van de huidige tab (of H) sluit het menu;
  - **Ctrl+Tab** = volgende tab, **Ctrl+Shift+Tab** = vorige.
- Hover over een tab: naam, uitleg en de toets.

**Testen (in het Vadsrijk)**
```
/guhs vadsrijk
/guhs vadsrijk menu
/guhs vadsrijk menu puzzels
```
(`/guhs vadsrijk` = naar binnen; `menu [tab]` opent het menu op een tab, tab-namen vult Tab aan.)
Druk **H** en klik alle tabs langs. Let op:
1. **Elke tab** heeft hetzelfde kader. De tabs bovenaan lopen nooit buiten het scherm.
2. **Verkenningsboek (B):**
   - de zone-knoppen staan nu als rij BINNEN het boek (liepen eerst links buiten het scherm);
   - hover over een zone = het percentage;
   - het zoekvak rechtsboven filtert de rechterpagina.
3. **Karakter:**
   - links naam, modus, level + xp-balk (hover), klasse + titel, alle klasse-levels, wapenschade en munten;
   - midden alle uitrusting incl. de nieuwe vakjes (Ring 2, Armband, Handschoenen, Cape), hover = tooltip;
   - rechts alle stats met duidelijke namen.
4. **Rijdieren:** in de titelband staat de subtab "Zeilbootje".
5. **Knapzak:** de kist staat in het kader met de subtab "Materiaaltas". Wisselen sluit de kist netjes.
   Een item dat je vasthoudt valt niet als je in het kader klikt.
6. **Puzzels:**
   - alle puzzels, geheimpjes en verstopte knuffels van alle zones, per zone met "x/y";
   - filter Alles / Nog te doen / Gevonden + zoekvak;
   - scrollen met het muiswiel.
7. **Ridders:** het karakterscherm (3 kaarten, gecentreerd).
8. **Reiskaart:** de kaart IN het kader.
   - Slepen om te kijken; hover = info.
   - Klikken laat je NIET vliegen (dat blijft bij het luchtschip-NPC).
9. **Instellingen:**
   - schuiven voor gebiedsmuziek, bazenmuziek, ta-daa's, omgeving, stemmetjes en schermschud;
   - stadsmuziek aan/uit;
   - Effecten / Sfeer: Vol / Zacht / Uit;
   - tekstsnelheid.
   - Alles wordt meteen bewaard: check in Mods > Guhs > Config dat het daar ook veranderd is.
   - "Toetsen..." opent de toetsinstellingen; Esc brengt je terug.
10. **Skillboom (K) en Questlog (J)** zitten ook in het kader, in de roze stijl.

**Kleine schermen:**
- Opties > Video > GUI-schaal **4**, of maak het venster klein: het menu wordt kleiner, maar blijft klikbaar en de tooltips staan bij de muis.
- GUI-schaal **2/3**: het kader is groter dan de oude schermen.

**Wat het NIET doet:** de bank (bij de bankier) blijft een los scherm; die hoort bij een NPC.
### Toetsenoverzicht in het pauzemenu (U-TOETSEN)

**Wat:** druk in het Vadsrijk op **Esc**. Rechts naast de knoppen staat nu een roze guh-paneel met al je Vadsrijk-toetsen.

1. Ga naar het Vadsrijk (`/guhs vadsrijk`) en druk op **Esc**.
   - Rechts zie je **"Toetsen in het Vadsrijk"** met de groepen **Gevecht** (R V G Z C U), **Menu's** (K J B N P O I Y), **Rijden** (X), **Sociaal** (emote-wiel `;`, zodra u-hud erbij zit) en **Overig** (drankjesriem **Alt + 1/2/3**, questvolger, lichtstraal).
   - Questvolger en lichtstraal staan standaard **grijs met "—"**: die hebben nog geen toets.
   - Het paneel zit **nooit over de gewone knoppen** heen.
2. Ga met de muis over een regel: je krijgt een uitleg-tooltip. **Klik** erop: je komt bij **Besturing** (toetsen wijzigen). Met *Klaar* ga je terug naar het pauzemenu.
3. **Toets wijzigen:** zet in Besturing bijv. *Knapzak* op **H**, en ga terug: in het paneel staat nu meteen **H**.
   - Zet er twee op dezelfde toets (bijv. *Knapzak* op **Q**, die al "Item laten vallen" is): de toets wordt **rood met een !**, en de tooltip zegt **"Botst met: Item laten vallen"**.
   - Zet ze daarna weer terug (in Besturing: *Herstellen*).
4. **GUI-schaal:** probeer Opties → Video → GUI-schaal **2, 3 en 4**.
   - Bij 2 staat alles groot in één kolom. Bij 3/4 wordt het paneel kleiner, maar het blijft leesbaar en het overlapt nooit de knoppen.
   - Is er echt geen plek (heel klein venster), dan zie je rechts een knop **"Toetsen ▸"**. Die opent het overzicht als een los scherm.
5. **Buiten het Vadsrijk** (overworld): Esc toont **geen** paneel.
6. Overal te openen als los scherm:
   ```
   /toetsen
   ```
### Geen dubbele bazen, NPC's en wezens meer (U-DUBBEL)

**Wat was er mis?** Kristalwacht Splinter stond 2× op dezelfde plek en er zaten 3 Kruinroofvogels in de arena. Dat kwam niet door
één baas, maar door hoe Minecraft chunks laadt:
- Als een chunk terugkomt, staan de blokken er al voordat de wezens van die chunk terug zijn. Onze bazen-arena's en de NPC-bewaker
  keken soms precies in dat gaatje, zagen "niemand" en zetten er een nieuwe neer. Daarna kwam de oude ook nog terug.
- Wezens die we nooit opslaan (NPC's, luchtschepen, vliegers, Ouwe Dobber, bewoners van Hoogvads...) bleven onzichtbaar in het
  geheugen hangen als hun chunk wegging, en kwamen later gewoon weer tevoorschijn naast de nieuwe.
Snel heen en weer vliegen in creative maakte het erger.

**Wat is er nu?**
- Elke unieke baas, NPC, gebiedsleider, luchtschip, vertrekbord, vlieger, Ouwe Dobber, bewoner, huisdier en rijdier heeft een
  eigen vaste "sleutel". Er kan er per sleutel maar één bestaan.
- Een oude kopie die van schijf terugkomt terwijl de echte er al is, wordt geweigerd.
- Spook-wezens worden opgeruimd zodra hun chunk weggaat.
- Arena's en de NPC-bewaker wachten tot de wezens van een chunk echt terug zijn voordat ze iets nieuws neerzetten.
- Al bestaande dubbelen met dezelfde sleutel worden elke 10 seconden vanzelf opgeruimd (ook die in je testwereld van vannacht).

**Testen**

1. Splinter (zone 2):
```
/guhs vadsrijk npc tp z2_kapitein
/guhs vadsrijk dubbel scan
```
Er staat nu één Kristalwacht Splinter (een paar blokken van de kapitein af). De scan zegt "Geen dubbele unieke wezens..." (of hij
noemt nog oude dubbelen; wacht dan 10 s of doe `/guhs vadsrijk dubbel opruimen`).

2. Kruinroofvogel (zone 3):
```
/guhs vadsrijk level 50
/guhs vadsrijk z3bazen tp kruinroofvogel
/guhs vadsrijk dubbel scan
```
Precies één Kruinroofvogel. Vlieg daarna in creative een paar keer heel ver weg (500+ blokken) en weer terug, en doe opnieuw
`/guhs vadsrijk dubbel scan`. Nog steeds één.

3. Rondvliegen langs de rest (doe na elk stukje `/guhs vadsrijk dubbel scan`):
```
/guhs vadsrijk npc tp kapitein
/guhs vadsrijk npc tp z2_wacht_galm_1
/guhs vadsrijk luchtschip lijnen
/guhs vadsrijk dubbel scan
```
Kijk ook bij de luchtschiphavens (vertrekbord: 3 regels, niet 6), bij Ouwe Dobber en de vliegers in zone 4, en tussen de
bewoners en de Wolkwacht-ridders in Hoogvads.

4. Tellingen:
```
/guhs vadsrijk dubbel stats
```
Laat zien hoeveel oude kopieën geweigerd, vervangen en spook-wezens opgeruimd zijn sinds de start.

**Uitleg bij de scan:** ● (rood) = twee met dezelfde sleutel (wordt vanzelf opgeruimd). ○ (geel) = twee dezelfde NPC's of twee
dezelfde benoemde dingen op één plek zonder sleutel; die haalt alleen `/guhs vadsrijk dubbel opruimen` weg. Klik op een
coördinaat om erheen te gaan.

**Let op:** een NPC of baas die net buiten je simulatieafstand staat, verschijnt pas als je wat dichterbij komt. Dat is expres.
## Spiekbrief U-DECO (ronde U: verlichting, Honingtak, arena-onderkant, wenteltrap, Schildpadeiland, gondel)

Alle commando's werken ook van buiten het Vadsrijk. Handig erbij: `/time set night` (verlichting) en `/time set day` (bijen).

### 1. Knuffelkamp: alle paden verlicht
Overal langs de paden van het kamp-eiland staat nu om de ~14 blokken een guh-lantaarnpaal (naast het pad, nooit erop),
zodat geen stuk pad 's nachts donker is. Niet alleen het aankomstpad maar ook de paden naar de vijver, het trainingsveld,
het tempeltje en de vuurtoren.
```
/time set night
/guhs vadsrijk wereld plek kampvuur_kamp
/guhs vadsrijk wereld plek aankomst
/guhs vadsrijk wereld plek tempeltje
/guhs vadsrijk wereld plek vijver
/guhs vadsrijk wereld plek training
/guhs vadsrijk wereld plek kampvuur_vuurtoren
```
Kijk: loop van het kampvuur naar elk van deze plekken; je zou steeds de volgende lantaarnpaal al moeten zien. Geen paal midden
op een pad of voor een tentdeur.

### 2. Honingtak: licht, straatmeubels en bijen
- Op de ringstraat (tussen de huizen en de stam) staat om de ~10 blokken een guh-lantaarnpaal, met daartussen om en om een
  bankje (kijkt uit over de straat) of een prullenbakje + een bloembakje.
- Op de buitenpromenade (waar die breed genoeg is) ook palen.
- Op het Honingplein: een kring van 4 palen en 4 bankjes rond de fontein, palen en prullenbakjes op de hoeken.
- Overdag vliegen er rond je (op het dek) gewone bijen en guhbijtjes (half/half, max 8) plus de honingbijtjes van zone 3. Ze
  steken nooit, worden nooit boos (ook niet als je ze slaat) en gaan niet in de korven; 's nachts en bij regen zijn ze weg.
```
/time set day
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk z3wereld plek markt
/guhs vadsrijk z3wereld plek gebouw.smid.deur
/guhs vadsrijk z3wereld plek gebouw.burgemeester.deur
/guhs vadsrijk udeco bijen
/time set night
```
Kijk: is het dek 's nachts goed verlicht? Staat er niets voor een deur of een kraampje? `udeco bijen` telt de bijen om je heen.
Sla er eens een: niks gebeurt, er steekt er geen een.

### 3. Kruinroofvogel-arena: geen platte plaat meer onder
De onderkant van de Kale Kruin is nu een organische, taps toelopende nestkom: diep in het midden, dun aan de rand, met een
grillige rand, gevlochten takken die naar een punt onder het midden lopen, en hangend bleek mos, hangende wortels,
gloeibessen-ranken en honingdruppels (raat + honingblok).
```
/guhs vadsrijk z3wereld plek honinglift_kruin_onder
/guhs vadsrijk z3wereld plek arena_kruinroofvogel
```
Kijk: vanaf Honingtak (eerste commando) omhoog kijken richting het noordwesten: geen vlakke plaat meer. Het tweede commando: de
arena zelf is hetzelfde gebleven (vlak dak, oprit vanaf kruin 1).

### 4. Uitkijkpunt `uitkijk_nest` en de wenteltrap in de boom
- Bovenop het nest staan het hekje langs de trapsleuf en de 2 lantaarns nu óp de nestvloer (ze zweefden 1 blok erboven).
- De lantaarns bij de deuren van de bordessen staan nu op een paaltje (ze zweefden naast het deurkozijn).
- Verdiepingsindicatie op de wenteltrap:
  - een gekleurde band op de stamwand net boven je hoofd die de trap volgt, in de kleur van de verdieping waar je naartoe
    klimt (bruin Schorsstad, oranje Honingtak, roze Kruin 1, magenta Kruin 2, paars Kruin 3, wit Het Nest);
  - op elk bordes is de band 2 rijen hoog, met aan beide uiteinden een banier in die kleur en een hangend lantaarntje;
  - boven elk bordes hangt een naambord: de naam (bv. **Honingtak**), "Verdieping 2 van 6" en waar je omhoog/omlaag uitkomt.
- De hekken zijn verbonden door de centrale verbindingspas van u-hekken (gemerged).
```
/guhs vadsrijk z3wereld plek trap_bordes_1
/guhs vadsrijk z3wereld plek trap_bordes_2
/guhs vadsrijk z3wereld plek trap_bordes_3
/guhs vadsrijk z3wereld plek trap_bordes_4
/guhs vadsrijk z3wereld plek trap_bordes_5
/guhs vadsrijk z3wereld plek trap_bordes_6
/guhs vadsrijk z3wereld plek uitkijk_nest
/guhs vadsrijk udeco borden
```
Kijk: loop een stuk van de trap: zie je steeds in welke kleur/verdieping je zit? Op elk bordes het naambord (Nederlands).
`udeco borden` zet de naamborden opnieuw neer als je er een mist. Bovenop het nest: geen zwevende lantaarns of hekjes meer.

### 5. Schildpadeiland (zone 4)
- De bloemetjes, de 2 lampionpaaltjes en de palm op de rug van de schildpad staan nu op de echte hoogte van het schild (ze
  stonden allemaal op de hoogte van de top van de koepel, dus 1 blok te hoog op de lagere plekken).
- Geheime knuffel 6 lag precies op Kluizenaar Schelpie (zijn plek op de rug); die heeft nu een eigen plekje 8 blokken verder
  op het schild (met een knuffelblokje).
- Elders in zone 4 ook rechtgezet: de magere knuffels in het Opvangcentrum zweefden 1 blok boven de vloer, en losse
  plantjes die zweefden waar later een pad/huis de grond weghaalde, worden nu opgeruimd.
```
/guhs vadsrijk z4wereld plek schildpad_rug
/guhs vadsrijk z4wereld plek geheim_knuffel_6
/guhs vadsrijk z4wereld plek gebouw.schildpadhutje.deur
/guhs vadsrijk z4wereld plek gebouw.opvangcentrum.binnen
```
Kijk: niets zweeft op het schild, Schelpie zit vrij (geen knuffel/hartjes op hem), de knuffels in het Opvangcentrum staan op
de vloer tegen de achterwand.

### 6. Gondel (zone 2) en andere aankomstplekken: geen mobs meer bij aankomst
Er spawnen geen mobs meer binnen 24 blokken van:
- beide gondelstations;
- elke luchtschiphaven (landing, bel, vertrekbord, plein);
- elke bootsteiger;
- de luchthaven-aankomst en de terugportalen;
- kampvuren en stadsranden (een spawner die zelf vlak bij een kampvuur staat, zoals in een kerker, houdt de oude 12 blokken).
```
/guhs vadsrijk z2wereld plek gondel_dal
/guhs vadsrijk udeco vrij
/guhs vadsrijk z2wereld plek gondel_berg
/guhs vadsrijk udeco vrij
/guhs vadsrijk z2wereld plek dok_landing
/guhs vadsrijk z4wereld plek haven_3_4_landing
```
Kijk: rondom de stations geen aanvallende mobs meer (mobs die al rondliepen kunnen nog even in de buurt zijn; wacht een minuut
of kom terug). `udeco vrij` laat de dichtstbijzijnde vrije plekken zien en of je erbinnen staat.

## Uit de nachtronde (nog steeds geldig)

Eerst: kijk hoe alles aan elkaar hangt (havens, barrières, de zee tussen de zones), dan de luchtschepen en je karakters.

## WERELDINDELING W-3: de wereld hangt aan elkaar (T-WERELD)

De Vadsrijk-wereld is opnieuw ingedeeld: Knuffelkamp linksonder, zone 1 in het midden, zone 2 (Kandijkom) erboven naar het
noordoosten, zone 3 (de Boom) rechts daarvan, zone 4 (eilandjes) en Hoogvads nog verder naar rechts.
**Maak een NIEUWE wereld** (of gooi `dimensions/guhs/vadsrijk` weg): oude stukken wereld houden de oude kaart.

Handig:
- `/guhs vadsrijk indeling zone`: in welke zone sta je, sta je in "vrij gebied", en welke barrière duwt je terug.
- `/guhs vadsrijk indeling barriere uit` / `aan`: de terugduw-barrières uit/aan (om rond te zwemmen/vliegen tijdens het testen).
- Barrières werken NIET in creatief. Test ze in `/gamemode survival` (of adventure).

---

### W1. De overgangen bekijken (de randen tussen de zones)
```
/gamemode survival
/guhs vadsrijk indeling rand splinterwand
```
- Je dobbert in zee onder de **noordkliffen van zone 1**. Voor je (noord) staat **de Splinterwand**: een rij spitse rotsen en
  blauwe kristalnaalden, 90-145 blokken hoog, met gaten ertussen waardoor je zone 2 ziet liggen.
- Zwem naar het noorden: je wordt **zachtjes teruggeduwd** (bubbels + spatjes) en op de actiebalk staat
  "De Splinterwand is te steil en de wind te hard...". Nergens een zichtbare muur.
```
/guhs vadsrijk indeling rand splinterwand_west
/guhs vadsrijk indeling rand hoog_wind
```
- `hoog_wind`: je hangt hoog boven de wand. Pak een elytra / vlieg met een rijdier: de **windmuur** duwt je terug en omlaag
  ("Een harde windmuur blaast je terug...").
```
/guhs vadsrijk indeling rand wolkenkloof
/guhs vadsrijk indeling rand wolkenkloof_noord
```
- **De Wolkenkloof** (zone 2 -> 3): een kronkelend kanaal open water tussen dikke wolkenkliffen. Links (west) de oostkust
  van zone 2 die in wolkenbanken uiteenvalt, rechts (oost) de wolkenzee rond de Boom. Zwem/vaar oost of west: terugduwen
  met "De Wolkenkloof is te diep en te breed...".
```
/guhs vadsrijk indeling rand wolkenval
```
- **De Wolkenval** (zone 3 -> 4): de wolkenbanken rond de Boom worden dunner en vallen uit elkaar, daarachter open zee naar
  Lampionhaven. Terugduwen: "De Wolkenval en het tij duwen je terug...".
```
/guhs vadsrijk indeling rand zoetezee
```
- **De Zoete-Zeestroom** tussen Knuffelkamp en zone 1: open zee; zwem je te ver, dan duwt de stroming je terug.
```
/guhs vadsrijk indeling rand z2_zuidkliffen
/guhs vadsrijk indeling rand z2_oostkust
/guhs vadsrijk indeling rand kust_oost_z1
/guhs vadsrijk indeling rand kust_zw_z1
```
- **Natuurlijke kusten**: nergens meer een recht, vierkant eiland. Zone 1 heeft kapen en baaien (een baai bij de Klodderweide,
  een schiereiland in het zuidwesten naar het kamp, een kaap in het noorden). Zone 2 heeft steile ijskliffen in het zuiden en
  een afbrokkelende oostkust. De wolkjes langs de oude vierkante rand zijn weg.
- **Geen zwarte leegte** meer in zee: tussen de zones is overal zee (dieper naar de verte, ondiep rond zone 2).
- Barrières zijn er alleen tussen de zones: rond je eigen zone zwemmen kan gewoon (een stuk de zee in).

### W2. De havens (vertrekplatforms met vertrekbord)
Het luchtschip zelf (dienstregeling, meevliegen) is van T-LUCHTSCHIP. Hier: de plekken.
```
/guhs vadsrijk indeling plek haven_z1_z2_vertrek
```
- **De Noordklifhaven** (zone 1, op de noordkliffen): een pleintje met lantaarns, een lange steiger over de klif, een platform
  met reling, een **meerpaal met guh-vlag** en een **donker vertrekbord** op palen. Lopend bereikbaar: een nieuw pad begint
  aan de noordkant van de **fossielbrug** (over de Kloof) en klimt rustig (geen sprongen) naar het noorden, de klifrand op.
  Loop het eens helemaal: `/guhs vadsrijk wereld plek haven_z1_z2_plein` en dan het pad zuidwaarts terug naar de fossielbrug.
```
/guhs vadsrijk indeling plek haven_z2_z1_vertrek
/guhs vadsrijk indeling plek haven_z2_z3_vertrek
```
- **Kandijkom-dok** (zone 2, de bekende pier): er staat nu ook een **vertrekbord** op de pier.
- **Het Galmdok** (zone 2, op de zuidoostkaap): pleintje + steiger naar het oosten, platform, meerpaal met blauwe vlag,
  vertrekbord. Lopend: het pad langs de warme bron (Galmhellingen) loopt door naar het dok.
```
/guhs vadsrijk indeling plek haven_z3_z2_vertrek
/guhs vadsrijk indeling plek haven_z3_z4_vertrek
```
- **Weststeiger** en **Oosttaksteiger** (zone 3, op de takken van Honingtak): die bestonden al; ze hebben nu de gedeelde namen.
```
/guhs vadsrijk indeling plek haven_z4_z3_vertrek
```
- **Lampionhaven** (zone 4) wordt door T-Z4WERELD gebouwd; zonder hun kaart kom je op de geplande plek in zee uit.

### W3. De uitkijkpunten bij de overgangen
Eerste keer op het punt = het panorama-filmpje + "uitkijkpunt gevonden".
```
/guhs vadsrijk indeling plek uitkijk_noordkliftop
/guhs vadsrijk indeling plek uitkijk_kandijkom_balkon
/guhs vadsrijk indeling plek uitkijk_galmkam
/guhs vadsrijk indeling plek uitkijk_westtak
/guhs vadsrijk indeling plek uitkijk_oosttak
```
- **Noordkliftop**: hoek van het Noordklif-platform; je kijkt over de Splinterwand naar Kandijkom.
- **Kandijkom-balkon**: eind van de Kandijkom-pier; je kijkt naar het zuiden, naar de noordkust van zone 1.
- **Galmkam**: hoek van het Galmdok-platform; je kijkt oost over de Wolkenkloof naar de Boom.
- **Westtak / Oosttak**: de uitkijkjes bij de steigers van Honingtak (zone 3).
- Kijk ook vanuit Zwamzoet naar de horizon: Kandijkom (kristalspitsen) staat nu in het noordnoordoosten, de Boom in het
  oostnoordoosten met Hoogvads erachter.

### W4. Zone 1 + Knuffelkamp opnieuw testen (alles staat op een nieuwe plek)
Het kamp is **384 blokken naar het westen** geschoven; zone 1 zelf bleef staan (alleen de kust en het noorden veranderden).
```
/guhs vadsrijk
```
- Nieuwe wereld: je komt aan op de **zuidsteiger van het kamp** (nu rond x -384, z 1130). Trap omhoog de klif op, kampvuur,
  ordetent, training, tempeltje, vijver (Klodderreus-arena), vuurtoren: alles gewoon, alleen verder naar het westen.
```
/guhs vadsrijk wereld plek aankomst
/guhs vadsrijk wereld plek kampvuur_kamp
/guhs vadsrijk wereld plek ordetent
/guhs vadsrijk wereld plek arena_klodderreus.deur
/guhs vadsrijk wereld plek kampvuur_vuurtoren
/guhs vadsrijk wereld plek terugportaal_kamp
/guhs vadsrijk npc tp kapitein
/guhs vadsrijk npc tp visser_dobber
```
- Het **luchtschip kamp -> De Haven** (kapitein): de vlucht buigt nu van het westen naar de Haven; het schip aan anker en de
  wolkenbanken staan langs de nieuwe route. Kijk of de vlucht niet door land of rotsen gaat.
- Questvolger in het kamp: wijst hij naar de goede plekken (rekruteerder, kok, priesteres)? En de BAM (de draak over het kamp).
- Zone 1 binnenland is niet verplaatst: `wereld plek plein`, `wereld plek haven_plein`, `wereld plek arena_koning_klodder.deur`,
  `wereld plek uitkijk_druipspits`, `wereld plek uitkijk_klodderkijk` werken als vroeger. Let op de **nieuwe kust**: staan alle
  kampvuren, uitkijknaalden en de Haven nog op het land? Is het noorden (onder de Zwamtempel) nu een kale, besneeuwde
  klifrand?

### W5. Zone 2 opnieuw testen (de hele zone ligt nu op +704, +1344)
Zone 2 is 704 naar het oosten en 1344 naar het zuiden geschoven (Kandijkom rond x 704, z -1056).
```
/guhs vadsrijk z3npc klaarzetten
/guhs vadsrijk z2wereld vlieg kandijkom
/guhs vadsrijk z2wereld plek plein
/guhs vadsrijk z2wereld plek dok_landing
/guhs vadsrijk z2wereld plek arena_galmwolf_deur
/guhs vadsrijk z2wereld plek deur_spiegelburcht_voor
/guhs vadsrijk z2wereld plek arena_boormeester_deur
/guhs vadsrijk z2wereld plek gondel_dal
/guhs vadsrijk z2wereld plek brug_3_zuid
/guhs vadsrijk z2wereld plek meer_eiland
/guhs vadsrijk npc tp z2_kapitein
```
- De openingsfilm naar Kandijkom (storm, naald, wolf) en de film terug: camera op de goede plek?
- **Spiegelburcht**: puzzels (lichtkamer/prismazaal), de spiegelgang en Vrouwe Spiegelzucht staan mee verschoven. Doe één
  puzzel en de baas.
- Galmwolf, Boormeester, het Mika-fort, de gondel, de bevroren knuffels onder het ijs (Kleine Vlokje), de draak op het zadel.
- Wezens spawnen alleen in zone 2 (niet in zee); geen wezens in Kandijkom / bij de kampvuren.
- Biomen: `F3` in Kandijkom = kandijkom, in de Galmhellingen = galmhellingen.
- Kandijkom-stadsmuziek klinkt alleen in Kandijkom.

### W6. Zone 3 (stond al op zijn plek; nu met zee eromheen)
```
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk indeling plek uitkijk_nest
```
- Rond het wortel-eiland ligt nu **zee met een wolkenzee erop**; onder het eiland geen leegte meer. Kijk of nergens water in
  de wortelgrotten/het Hart staat (`z3wereld plek` naar `hart_poort`) en of de wortelrand niet half onder water staat.

### Nieuwe coördinaten (voor `/tp`, oude → nieuw)
| plek | oud | nieuw |
|---|---|---|
| aankomst (zuidsteiger kamp) | 0 65 1130 | -384 65 1130 |
| kampvuur kamp | 0 87 1025 | -384 87 1025 |
| kampsteiger (luchtschip kamp) | 151 87 1015 | -233 87 1015 |
| Kandijkom plein | 0 91 -2412 | 704 91 -1068 |
| Kandijkom-dok | 0 91 -2244 | 704 91 -900 |
| Galmwolf-deur | 146 115 -2318 | 850 115 -974 |
| Spiegelburcht-deur | 150 176 -2629 | 854 176 -1285 |
| Boormeester-deur | -30 66 -2559 | 674 66 -1215 |
| Noordklifhaven (nieuw) | - | 150 127 -653 |
| Galmdok (nieuw) | - | 980 117 -856 |
| Weststeiger / Oosttaksteiger (zone 3) | - | 1201 167 -601 / 1607 165 -488 |

Alle andere zone-2-plekken: oud + (704, 0, 1344). Zone 1 (behalve het kamp) en zone 3 zijn niet verschoven.

### W7. Trappencheck (worldgen)
- Zie NOTITIES_T_WERELD.md, "Trappenrapport": welke trappen de checker nog meldt. Loop er een paar na met de coördinaten daar.

## Luchtschepen (dienstregeling) + verre schimmen + zichtafstand (T-LUCHTSCHIP)

**Wat is er nieuw?**
- Grote guh-luchtschepen vliegen nu echt. Elke lijn heeft twee schepen die elke ~3 minuten vertrekken (aan beide kanten tegelijk, ze passeren elkaar halverwege). Een vlucht duurt 60-90 seconden.
- Je staat met maximaal 16 spelers op het dek en kunt vrij rondkijken. Tijdens de vlucht kun je er NIET af (sneaken geeft "Njeg!") en kan niets je pijn doen.
- Elke haven heeft een zwevend vertrekbord (3 regels) met aftelling. Sta je bij de pier (binnen ~9 blokken), dan zie je de vertrektijd ook in de actiebalk.
- De oude reiskaart-vlucht (kapitein / scheepsbel) blijft, maar is nu **luxe**: de eerste keer naar een plek is gratis, daarna kost het 400 brons. Het grote schip is altijd gratis.
- Verre **schimmen** (silhouetten) van De Boom, de Splinterpieken, de Kaaszeppelinwerf en Moeder Wolkwal met Hoogvads staan op hun ECHTE plek, ook ver buiten je renderafstand. Ze vervagen vanzelf zodra het echte ding geladen is.
- De Vadsrijk-dimensie heeft een eigen **zichtafstand** van 20 chunks (instelbaar tot 32), zonder server.properties aan te raken. Je eigen render-instelling blijft de bovengrens.

**De lijnen** (havens zijn nu nog plaatshouders tot t-wereld de POI's bouwt):

| Lijn | Van | Naar | Slot (heenweg) |
|---|---|---|---|
| 0 | Kampsteiger (-235, 87, 1008) | Haven Zwamzoet (92, 65, 500) | kampverhaal (zoals de oude bel); vliegt pas vanzelf als t-wereld eigen pieren voor lijn 0 heeft gebouwd (daar ligt nu het oude blok-luchtschip). Testen kan wel: `rij 0` |
| A | Noordklifhaven (150, 121, -600) | Kandijkom-dok (704, 91, -900) | zelfde als de kapitein naar Kandijkom |
| B | Galmdok (955, 111, -835) | Weststeiger (1190, 171, -590) | zone2_klaar + lv 30 |
| C | Oosttaksteiger (1630, 171, -500) | Lampionhaven (1800, 71, -470) | zone3_klaar |
| D | De Opstijgpier (2515, 73, -565) | Walvisrughaven (2630, 263, -520) | vlag zone4_klaar (finale zone 4) |

De terugweg is altijd open. In creative geldt geen slot en mag je wel uitstappen.

**Snel testen** (alles in de dev-client):

```
/guhs vadsrijk luchtschip lijnen
```
Toont per lijn: lengte, vluchtduur, vertrek over m:ss, en waar beide schepen nu zijn ("(er)" = het schip bestaat nu als entity).

```
/guhs vadsrijk luchtschip rij A
```
Je staat op de Noordklifhaven, het schip ligt klaar, je staat al aan boord, en over 8 seconden vertrekt het.
- **Kijk naar:** het vertrekbord boven de pier telt af. De actiebalk zegt "Aan boord naar Kandijkom-dok - vertrek over ...".
- **Bij vertrek:** een scheepsbel en "Daar gaan we!". Probeer te sneaken: "Njeg! Blijf lekker aan boord".
- **Onderweg:** ~85 s langs de Splinterwand. Kijk rond. De propellers draaien, de ballon deint, en het tweede schip komt je halverwege tegemoet (iets opzij).
- **Bij de landing:** het schip draait bij, je staat op de pier van Kandijkom-dok en ziet de titel "Kandijkom-dok - Geland met het luchtschip".

Andere lijnen en de terugweg:
```
/guhs vadsrijk luchtschip rij B
/guhs vadsrijk luchtschip rij C
/guhs vadsrijk luchtschip rij D
/guhs vadsrijk luchtschip rij 0
/guhs vadsrijk luchtschip rij A terug
```

Zelf instappen, zoals een speler dat doet:
```
/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek
/guhs vadsrijk luchtschip vertrek A 60
```
Je staat bij de pier en het schip vertrekt over 60 s. Klik met rechts op het schip om in te stappen. Zolang het nog ligt, kun je met sneaken weer uitstappen (je komt dan terug op de pier).

**Vertrek forceren / landen / alles terug:**
```
/guhs vadsrijk luchtschip vertrek C 5
/guhs vadsrijk luchtschip land
/guhs vadsrijk luchtschip reset
```
- `vertrek <lijn> [s]`: verschuift de klok van die lijn.
- `land`: zet je meteen af op de bestemming.
- `reset`: zet alle verschuivingen terug naar de echte dienstregeling.

**Randgevallen om te proberen:**
- **Uitloggen tijdens de vlucht:** log opnieuw in. Je staat op de pier van de bestemming.
- **Server stoppen tijdens de vlucht** (dev-client afsluiten midden in de lucht): na het opnieuw laden sta je op de bestemming, met de melding "Je vlucht is netjes afgemaakt".
- **Doodgaan** (`/kill`) aan boord: je spawnt gewoon bij je kampvuur, en de vlucht is vergeten.
- **Creative:** je mag uitstappen waar je wilt.

**Luxe reiskaart:**
```
/gamemode survival
```
Klik op een scheepsbel of praat met een kapitein, en kies een plek op de reiskaart:
- de eerste keer: "Luxe vlucht! Deze eerste keer trakteert de kapitein";
- de tweede keer naar dezelfde plek: 400 brons, of de melding "Een luxe vlucht kost 400 brons...".

**Schimmen (silhouetten):**
```
/guhs vadsrijk luchtschip schimmen altijd
/guhs vadsrijk luchtschip haven haven_z4_z3_vertrek
```
- `altijd`: alle schimmen op volle sterkte, ook als het echte ding dichtbij is.
- Vanaf Lampionhaven zie je De Boom achter je (west) en Moeder Wolkwal met Hoogvads voor je (oost).

Daarna weer op normaal, en de standaard-renderafstand:
```
/guhs vadsrijk luchtschip schimmen aan
```
- Zet je client-renderafstand op 12 en kijk vanaf de Noordklifhaven naar het noordoosten: de Splinterpieken staan er als schim.
- Zet de renderafstand op 32 en kom dichterbij: de schim vervaagt en het echte land neemt het over.
- De oude vaste plaatjes aan de horizon (kristalspitsen, boom, Hoogvads) zijn weg, behalve de vulkaan.

Helemaal uit (om te vergelijken):
```
/guhs vadsrijk luchtschip schimmen uit
```

**Zichtafstand van de dimensie:**
```
/guhs vadsrijk luchtschip zicht
/guhs vadsrijk luchtschip zicht 32
/guhs vadsrijk luchtschip zicht 20
/guhs vadsrijk luchtschip zicht 0
```
- Zonder getal: toont de huidige waarde. Met een getal: zet die meteen, en ook in `config/guhs-vadsrijk-zicht.properties`.
- `0` = uit: de dimensie gebruikt dan de gewone serverafstand.
- **Kijk naar:** zet je client op 32 en `zicht 32`. Vanaf Strandfeesteiland zie je dan de kruin én de walviskop echt.
- Andere dimensies blijven op de gewone afstand. Ga naar de Guhmension en terug: je client krijgt telkens de juiste radius.

**Tests:**
```
gradlew runGameTestServer -Pgt=VadsrijkLuchtschip
```

## Karakterslots + spelmodi (T-KARAKTER)

Je hebt nu tot **3 ridders** per account. Elke ridder heeft eigen levels, klasse-levels, skillbomen, Vadsrijk-inventaris
(+ harnas en sieraden), quests, verhaalfilmpjes, verkenningsboek, zonevlaggen/deuren, kampvuur en Vadsmunten.
**Gedeeld** (accountbreed): rijdieren (met hun levels), huisdieren, titels, de kleding-post, de bank/materiaaltas/kledingkast
(zodra die er zijn) en de tips die je al zag. Je gewone (buiten-)inventaris is natuurlijk ook gewoon van jou.

Spelmodi (kies je bij het maken, kan later alleen MAKKELIJKER):
- **Normaal**: zoals altijd.
- **Eénvads** (gouden hartje ❤ bij je naam): één leven. Flauwvallen BUITEN een stad = je ridder gaat met pensioen.
  Hij wordt niet gewist: hij krijgt een standbeeld in de **Rustende Ridders-zaal** in Zwamzoet, speelt verder als Normaal
  en krijgt een gouden sterretje ✦. Zolang hij leeft: +10% xp.
- **Knuffelmodus** (roze wolkje ☁): monsters doen maar de helft schade bij jou, je krijgt 10% minder xp.
- Geen IJzerguh.

### Snel testen (dev-commando's, OP nodig)
```
/guhs vadsrijk karakter
```
Opent het scherm "Je ridders" (mag iedereen). Drie kaarten: naam + badge, modus, level, klasse, munten; knoppen
Spelen / Naam wijzigen / Makkelijker... Een lege kaart: "Nieuw karakter" -> naam intikken, modus kiezen, "Maken en spelen".
Kijk: buiten een stad/kampvuur is "Spelen" grijs met de rode uitleg eronder.

```
/guhs vadsrijk karakter lijst
```
Alle slots in de chat: naam, modus, level, munten, welke actief is, en de sleutel ("uuid/slot").

```
/guhs vadsrijk karakter maak 1 normaal Pluisridder
/guhs vadsrijk karakter wissel 1
```
Maakt slot 1 en wisselt erheen (dev-wissel mag overal). Wat moet gebeuren: kersenbloesempjes, je staat bij de Knuffelkamp-aankomst
met een LEGE Vadsrijk-inventaris, level 1, 0 munten, en het openingsfilmpje start (nieuwe ridder = nieuw verhaal).
Titel "Pluisridder - Een nieuw avontuur begint!". Je rijdier (X), huisdier (P) en titels zijn er nog wel.

```
/guhs vadsrijk karakter wissel 0
```
Terug naar je eerste ridder: je staat weer precies waar je die achterliet, met al je spullen, levels, quests en munten.

```
/guhs vadsrijk karakter maak 2 eenvads Dappere Dirk
/guhs vadsrijk karakter wissel 2
/guhs vadsrijk karakter sterf-test
```
Eénvads: gouden ❤ voor je naam (tablijst, chat, boven je hoofd). `sterf-test` = meteen met pensioen: na ~3 s de titel
"Met pensioen!", een chatbericht voor iedereen, je naam krijgt ✦, en `lijst` toont "met pensioen" + modus normaal.
`sterf-test echt` = echt flauwvallen (gaat alleen met pensioen als je NIET in een stad bent).

```
/guhs vadsrijk karakter zaal tp
```
Teleport naar de Rustende Ridders-zaal in Zwamzoet (west van het plein, op het grasveld naast de Kruidenier, ingang aan de
zuidkant bij de havenpromenade, rond -27, 64, 76). Kijk: roze bakstenen hal met kersenhouten dak, glas-in-lood, hartjestegels,
een open ingang van 3 breed, een klinkerpaadje naar de promenade, en binnen standbeelden (pantserstandaard met het hoofd van de
speler, roze harnas, gouden zwaard, naambordje "✦ Naam lv X Klasse - Speler").
- Staat hij er niet? `/guhs vadsrijk karakter zaal bouw` bouwt hem op de vaste plek, `zaal bouw hier` op je eigen plek.
- `zaal test` voegt een nep-ridder toe (beeld erbij), `zaal lijst` toont ze, `zaal leeg` haalt alle beelden weg.

```
/guhs vadsrijk karakter modus 1 knuffel
```
Zet de modus van slot 1 (dev mag ook omhoog). In het scherm kan het alleen omlaag ("Makkelijker...").
Knuffelmodus: laat een klodder je slaan: de rode schadegetallen zijn de helft.

```
/guhs vadsrijk karakter naam 1 Sir Pluis
/guhs vadsrijk karakter scherm
/guhs vadsrijk karakter migratie-test
```
`scherm` = het scherm zoals bij binnenkomst (met "Verder als ..."). `migratie-test` doet alsof je data een oude save is en
controleert dat slot 0 precies hetzelfde blijft: allemaal "OK" verwacht.

### In het echt (zonder commando's)
- Praat met de **Grootmeester** (ordetempel Zwamzoet), de **Rekruteerder** (Knuffelkamp) of een **kampwacht** bij een kampvuur:
  keuze "Ik wil een andere ridder spelen" opent het scherm.
- Wisselen mag: in een stad, binnen 8 blokken van een kampvuur, of in de eerste minuut na binnenkomst. Niet in een baasgevecht
  of tijdens een filmpje.
- Heb je 2+ ridders, dan krijg je bij binnenkomst in het Vadsrijk het scherm met "Verder als <naam>".
- Met maar 1 ridder krijg je 1x een tip dat je er 3 kunt hebben.

### Gametests
`-Pgt=VadsrijkKarakter` (migratie, slotIsolatie, accountBreed, modiEnPensioen, namen).

# B2. ZONE 1 (Het Tussenland, lv 1-20) EN ZONE 2 (De Splinterpieken, lv 20-35)

## Nieuw in ronde U: zone 1 (Knuffelkamp, Zwamzoet, Sporenwoud)
- **Knuffelkamp:** alle paden verlicht met guh-lantaarnpalen (u-deco).
```
/time set night
/guhs vadsrijk wereld plek kampvuur_kamp
```
- **De Reuzenhoed** (nieuw uitkijkpunt bovenop de tempelzwam van Zwamzoet): neem de tempel-lift en loop over het plankenpad
  naar het balkon.
```
/guhs vadsrijk indeling plek uitkijk_hoedtop
```
- **De Rustende Ridders-zaal** staat nu in de wereld, in Zwamzoet tussen de Kruidenier en de boulevard. De beelden komen er
  vanzelf in zodra de zaal geladen is.
```
/guhs vadsrijk karakter zaal tp
```
- **De Zwamgrot** (lv 11-13), onder het Sporenwoud. De ingang is een holle reuzenpaddenstoel met een wenteltrap. Binnen:
  Het Echolied (puzzel), de Oude Zwamwachter (elite), een schat en plukken + mijnen.
```
/guhs vadsrijk grot tp zwamgrot
/guhs vadsrijk grot tp zwamgrot puzzel
```
- **Twee rijdende guhs** in zone 1: de koerier Haven ↔ Zwamzoet en de hoofdweg Zwamzoet ↔ Sporenwoud. Klik erop.
```
/guhs vadsrijk ruiters tp z1_koerier
/guhs vadsrijk ruiters tp z1_hoofdweg
```
- **Specialisatie op lv 17:** de quest `spec17_<klasse>` bij de Grootmeester (vanaf lv 16, na de Druipgrotten).
- **Hoedenpaden:** elk hoed-plateau heeft nu een open ingang in zijn hekring, ook de hoed bij (-374, 103, 82), die was
  dichtgegroeid.
```
/execute in guhs:vadsrijk run tp @s -374 105 82
```
- **Kwartiermeester** (huizen) en een **Bondenbord** (Knuffelbond) op het plein van Zwamzoet, en een Bondenbord bij het
  kampvuur van het Knuffelkamp.

## Nieuw in ronde U: zone 2 (Kandijkom, De Splinterpieken)
- **Gondel:** bij aankomst geen aanvallende mobs meer (24 blokken vrij rond beide stations).
```
/guhs vadsrijk z2wereld plek gondel_dal
/guhs vadsrijk udeco vrij
```
- **Geen dubbele Kristalwacht Splinter meer:**
```
/guhs vadsrijk npc tp z2_kapitein
/guhs vadsrijk dubbel scan
```
- **De Kristalgrot** (lv 22-24), in de Galmtop. De ingang is een kristallen boog in de westflank. Binnen: een ijsmeer om te
  vissen, De IJsspiegels (lichtpuzzel), de IJskoude Wacht (elite) en een schat.
```
/guhs vadsrijk grot tp kristalgrot
```
- **Vlaggetjes, slingers en tralies** in de Splinterpieken zijn verbonden (zie §1 bij U-HEKKEN).
- **Uitkijkpunt Galmtop** (hoogste naald, wenteltrap). De oude uitkijkpunten bij de havens zijn weg.

## Uit de nachtronde: zone 1 en 2 (nieuwe coördinaten, oude testblokken)

Het Knuffelkamp schoof 384 blokken naar het westen en zone 2 ligt nu op (+704, +1344) van zijn oude plek. Alle oude testblokken
hieronder werken met plek-namen, dus ze kloppen vanzelf op de nieuwe kaart. Loop ze allemaal nog een keer door: let vooral op
de overgangen (havens, de Splinterwand, de zee rond de eilanden) en of NPC's, kampvuren en arena's op hun plek staan.

## De belangrijkste plekken met hun NIEUWE coördinaten

### Knuffelkamp en zone 1 (Zwamzoet)

| plek | waar (x, y, z) | commando |
|---|---|---|
| aankomst | -384, 65, 1130 | `/guhs vadsrijk wereld plek aankomst` |
| steiger_eind | -233, 87, 1015 | `/guhs vadsrijk wereld plek steiger_eind` |
| ordetent | -384, 87, 991 | `/guhs vadsrijk wereld plek ordetent` |
| training | -322, 87, 981 | `/guhs vadsrijk wereld plek training` |
| kampvuur_kamp | -384, 87, 1025 | `/guhs vadsrijk wereld plek kampvuur_kamp` |
| haven_plein | 90, 65, 470 | `/guhs vadsrijk wereld plek haven_plein` |
| plein | 0, 64, 59 | `/guhs vadsrijk wereld plek plein` |
| questbord | -7, 64, 31 | `/guhs vadsrijk wereld plek questbord` |
| kampvuur_klodderweide_1 | 240, 69, -126 | `/guhs vadsrijk wereld plek kampvuur_klodderweide_1` |
| kampvuur_sporenwoud_1 | -228, 65, 17 | `/guhs vadsrijk wereld plek kampvuur_sporenwoud_1` |
| kampvuur_druipgrotten_1 | -260, 98, -311 | `/guhs vadsrijk wereld plek kampvuur_druipgrotten_1` |
| kampvuur_zwamtempel_1 | 138, 79, -236 | `/guhs vadsrijk wereld plek kampvuur_zwamtempel_1` |
| druipschacht_ingang | -463, 41, -471 | `/guhs vadsrijk wereld plek druipschacht_ingang` |
| slaapheuvel | 300, 96, 300 | `/guhs vadsrijk wereld plek slaapheuvel` |
| haven_z1_z2_vertrek | 150, 127, -653 | `/guhs vadsrijk wereld plek haven_z1_z2_vertrek` |
| uitkijk_noordkliftop | 154, 127, -656 | `/guhs vadsrijk wereld plek uitkijk_noordkliftop` |

### Zone 2 (De Splinterpieken)

| plek | waar (x, y, z) | commando |
|---|---|---|
| dok_landing | 704, 91, -900 | `/guhs vadsrijk z2wereld plek dok_landing` |
| plein | 704, 91, -1068 | `/guhs vadsrijk z2wereld plek plein` |
| kampvuur_galmhellingen_1 | 814, 100, -1046 | `/guhs vadsrijk z2wereld plek kampvuur_galmhellingen_1` |
| kampvuur_knuffelmeer_1 | 604, 100, -986 | `/guhs vadsrijk z2wereld plek kampvuur_knuffelmeer_1` |
| kampvuur_splinterkloof_1 | 508, 114, -1168 | `/guhs vadsrijk z2wereld plek kampvuur_splinterkloof_1` |
| warmwaterbron | 900, 120, -908 | `/guhs vadsrijk z2wereld plek warmwaterbron` |
| haven_z2_z3_vertrek | 980, 117, -856 | `/guhs vadsrijk z2wereld plek haven_z2_z3_vertrek` |
| uitkijk_galmkam | 983, 117, -860 | `/guhs vadsrijk z2wereld plek uitkijk_galmkam` |
| uitkijk_kandijkom_balkon | 704, 91, -882 | `/guhs vadsrijk z2wereld plek uitkijk_kandijkom_balkon` |
| arena_galmwolf | 886, 117, -974 | `/guhs vadsrijk z2wereld plek arena_galmwolf` |
| arena_boormeester | 674, 66, -1270 | `/guhs vadsrijk z2wereld plek arena_boormeester` |
| arena_spiegelzucht | 854, 176, -1397 | `/guhs vadsrijk z2wereld plek arena_spiegelzucht` |

## Oude testblokken ronde 3 (zone 1 + 2)

### R1. Makelaar en andere buitenstaanders blijven buiten
In het Vadsrijk:
```
/summon guhs:guh_npc ~ ~ ~2
/guhs erfenis brief
```
- **Summon:** er verschijnt niks. Guhs-wezens van thuis (makelaar Guhrrit, de Postguh, de Sherlock-cast) mogen er niet in.
- **Brief:** de makelaarsbrief komt niet in het Vadsrijk. Hij wacht tot je weer thuis bent (`/guhs vadsrijk` = terug).
- **Roepen:** "Roep naar mij" (guh-band) of de Guhkamer-roep in het Vadsrijk zegt "Je guhs blijven lekker thuis...".
- **Volgers:** een Libelkonijnguh, Sponsguh, Gerrit of Momo op je schouder gaat niet mee naar binnen, en staat er weer als je terug bent.

### R2. Huisdier zonder naambordje
```
/guhs vadsrijk huisdier geef koning_klodder
```
- Druk P, kies "Meenemen".
- Boven je huisdier staat **geen** naambordje meer (eerst stond er "entity.guhs.vr_huisdier").

### R3. Reiskaart: het kruisje
```
/guhs vadsrijk npc tp kapitein
```
- Klik op de kapitein. Rechtsboven in de roze titelbalk van de reiskaart staat een ✕ ("Sluiten" als je eroverheen gaat).
- Klik erop: de kaart gaat dicht, met een klikgeluidje.

### R4. Tabbladen: Rijdieren / Huisdieren / Verkenningsboek
```
/guhs vadsrijk mount alle
/guhs vadsrijk huisdier alle
```
- Druk N. Boven het paneel, tussen de guh-oortjes, zitten drie tabjes: **Rijdieren**, **Huisdieren**, **Verkenningsboek**.
- Klik heen en weer. Het actieve tabje hangt vast aan de roze titelbalk.
- Bij de stalmeester (`/guhs vadsrijk mount stal`) zijn er géén tabjes.

### R5. Rijdieren (ronde 3)
```
/guhs vadsrijk mount reset
```
- **Nog geen rijdier:** druk X en N: "Nog geen rijdier: dat krijg je in De Schemerwortel."
- **Teasers:** `/guhs vadsrijk npc tp stalmeester`. Hoefje plaagt over het Wolkenschaapje en de stal verkoopt nog niks.
  Ook bij Boswachter Mos (`npc tp boswachter_mos`) en in Kandijkom bij Breister Wolwinter (Guhwolf).
- **Weg:** de Hoedhupper en de Klodderros bestaan niet meer. Er zijn er nu 5.
```
/guhs vadsrijk mount alle
```
- **Snelheid:** Guhwolf het snelst (springt laag), Pluimguh, Knuffelhert (springt het hoogst, ~5 blokken), Wolkenschaapje (dubbele
  sprong), Slakkie lekker traag.
- **Opstapjes:** rijd zonder springen tegen een randje van 2 blokken: je rijdier stapt erop. Let op: een hek (1,5 hoog) stapt hij
  nu ook over. Zeg het als je dat niet wilt.
- **Instellen:** `/guhs vadsrijk balans rijdieren snelheid 1.2` (sectie `[rijdieren]` in `run/config/guhs-vadsrijk-balans.toml`).

### R6. Zone 2: hangbruggen
```
/guhs vadsrijk z2wereld plek brug_3_zuid
```
- Dit is de grote brug bij Kloofwachter Touwtje naar de Spiegelburcht. Loop eroverheen **zonder te springen**: elke stap is
  hooguit een half blokje, aan beide kanten sluit hij aan op de grond.
- Ook: `brug_1_zuid`, `brug_2_zuid`, `brug_4_zuid` (Splinterkloof) en `brug_5_zuid` (Galmspleet).

### R7. Zone 2: aankomst bij het dok en de poorten
```
/guhs vadsrijk z2wereld plek dok_landing
```
- **Glas weg:** het zwevende roze glas boven de poort is weg. Er staat nu een calciet-poortboog met een kristallamp.
- **Bergpas:** de weg naar de zuidpoort loopt glooiend door een pas, niet meer tussen rechte wanden.
- **Aankleding:** lampions en lichtsnoeren over de weg, vuurkorven, een bankje, een **chocomelk-kiosk** (`z2wereld plek dok_kiosk`)
  en een houten **welkomstboog** bij de pier (`z2wereld plek dok_welkom`). Sneeuw en kristallen langs de randen.
- **Andere poorten:** loop vanaf het plein van Kandijkom de noord-, oost- en westpoort uit. Dezelfde boog en een glooiende pas.

### R8. Muziek in de steden
```
/guhs vadsrijk npc tp bakker_bram
/guhs vadsrijk wereld plek haven_plein
/guhs vadsrijk z2wereld vlieg kandijkom
```
- Zet de schuif **Muziek** aan (en **Omgeving** voor de achtergrond).
- Na ~2 s begint een lief deuntje (Zwamzoet = wals, Knuffelkamp = ochtendfolk, De Haven = zeemanslied, Kandijkom = winterspeeldoos).
  Elke stad heeft er twee, met wat stilte ertussen.
- **Achtergrond:** vogeltjes, guh-gegiechel, belletjes; golfjes in De Haven, een haardvuur in Kandijkom; geknetter bij de kampvuren.
- **Weglopen:** loop de stad uit. Na ~3 s fadet de muziek weg; in de wildernis is er geen muziek.
- **Baas of scène:** bij een bazenbalk of een filmpje stopt het deuntje.
- **Uit:** in de mod-config (sfeer) `stadsmuziek = false`.

### R9. Het Verkenningsboek (B)
```
/guhs vadsrijk boek scherm
```
- Druk **B** (of klik het derde tabje). Een open roze boek met een bladwijzer per zone (De Knuffelzwammen, De Splinterpieken).
- **Links:** groot %, "x van de y gedaan", de beloning bij 100 %, en de categorieën (hoofdverhaal, zijmissies, uitkijkpunten,
  kampvuren, geheimen, verloren knuffels, bazen, huisdieren, gebieden).
- **Rijdieren (later):** die categorie staat er wel (zone 1: Wolkenschaapje + Knuffelhert, zone 2: Guhwolf), maar telt **niet** mee
  voor je %; rijdieren krijg je pas in zone 3. Hover erover voor de uitleg.
- **Rechts:** wat je hebt (groen vinkje), waar je mee bezig bent (geel) en wat nog niet (grijs met een hint; geheimen zijn "???").
- **Mijlpalen:** steek een paar kampvuren aan of klim een uitkijknaald op. Bij 25/50/75 % krijg je een roze toast.
  Bij 100 %: "Helemaal verkend!", een titel en Vadsmunten.
- `/guhs vadsrijk boek tel` laat je tellingen zien, `boek reset` begint opnieuw.
- **B en de snuffelhond:** B is ook "blaffen" als hond op Het Snuffeleiland. In het Vadsrijk wint het boek; als hond blaf je gewoon.

### R10. Kleine geheimpjes en de horizon (lore)
```
/guhs vadsrijk npc cliffhanger
/guhs vadsrijk z2npc flashback
/guhs vadsrijk z2npc cliffhanger
```
- Er zitten nu een paar hele kleine hints in zone 1 en 2 (praatjes, tooltips, de filmpjes). Let in de filmpjes goed op: soms is er
  maar één seconde iets te zien. Het einde van zone 2 wijst nu naar het **oosten**.
- **Horizon:** in zone 1 staat de reuzenboom (zone 3) nu in het oosten, naast het zwevende kasteel. Kijk vanaf Knuffelkamp naar het
  oosten (F3: richting "east").

---

## Oude testblokken ronde 2 (zone 1 + 2, nog steeds geldig)

### 1. Klassen: het wapen is je klasse
```
/guhs vadsrijk level 12
/guhs vadsrijk klasse wapen kussenzwaard
/guhs vadsrijk klasse wapen pluisboog
/guhs vadsrijk klasse wapen rozenstaf
/guhs vadsrijk wezens spawn klodder 10
```
- **HUD:** links van de hotbar staan twee regels, "Generiek nv 12" en de klasse.
- **Vasthouden:** hou de boog alleen vast. Je blijft dan ridder.
- **Aanvallen:** schiet één keer. Je wordt Pluisjager en R/V/G staan op volle cooldown.
- **Leeg:** met een lege hand wordt de klasseregel grijs en heb je geen skills.
- **Te hoog wapen:** `/guhs vadsrijk loot zeldzaam pluisboog 20`. De tooltip zegt in het rood "Vereist: Pluisjager nv 20", en aanvallen gaat niet.
- **Klasse-level apart zetten:** `/guhs vadsrijk klasse klasselevel pluisjager 15` en `/guhs vadsrijk klasse generiek 18`.
- **Skillboom:** druk K en bekijk de tabs per klasse. Hover over een skill voor uitleg met ✔ en ✖.

### 2. Balans (live aanpassen)
```
/guhs vadsrijk balans toon
/guhs vadsrijk balans knuffelridder schade 1.5
/guhs vadsrijk balans mobs schade 1.0
/guhs vadsrijk balans herlaad
```
- Het bestand staat in `run/config/guhs-vadsrijk-balans.toml`. Bewerken mag, en het wordt binnen 5 s opnieuw gelezen.
- **Verdediging:**
  1. Doe harnas aan: `/guhs vadsrijk loot episch knuffelharnas 12`.
  2. Laat je raken door `/guhs vadsrijk wezens spawn hoedjesbol 12`, dan door een hoger wezen (`... 25`).
  3. Bij het hogere wezen helpt je verdediging minder.

### 3. Drankjes
```
/guhs vadsrijk eco winkel drankjes
```
- Koop een Klein Vadsdrankje (120), een Vadsdrankje (300) en een Energiedrankje.
- Drinken duurt 2 s. Daarna staan alle drankjes 10 s op cooldown; je ziet het grijs over het item.

### 4. Melee en de tutorial (melee-fix: deze map, h-melee)
```
/guhs vadsrijk klasse wapen kussenzwaard
/guhs vadsrijk wezens spawn klodder 1
/guhs vadsrijk npc bam
```
- **Ingedrukt houden:** hou linkermuis ingedrukt met het zwaard. Hij blijft meppen op het tempo van het wapen.
- **Klik net te vroeg:** een klik vlak voordat je weer mag slaan wordt onthouden en landt alsnog.
- **Startzwaard:** het zwaard dat je in Knuffelkamp krijgt doet 2× schade. Een klodder is in 2-3 klappen weg.
- **Arena:** de vijver-arena van de Grauwe Klodderreus is groter, met droge vlakke grond eromheen.
- **Door gras heen:** slaan lukt door gras en riet heen, maar niet door blokken.

### 5. Sieraden en UI
```
/guhs vadsrijk loot zeldzaam roze_ring 5
/guhs vadsrijk loot zeldzaam pluisoorbel 5
```
- **Tooltip:** lees de uitleg "Rechtsklik om te dragen".
- **Aandoen:** open je inventaris en doe shift+klik op het sieraad.
- **Hoveren:** ga met de muis over het vakje rechts. Je ziet de volledige stats en "Klik om af te doen".
- **Sets:** `/guhs vadsrijk z2wezens set` geeft set-stukken. De tooltip toont "Set: Kristalset (x/4)".

### 6. Rijdieren (X / N)
```
/guhs vadsrijk mount starter
/guhs vadsrijk mount alle
```
- **Starterkeuze:** de eerste regel opent de keuze uit 3 Pluimguhs. Kies er één en lees het chocobo-grapje.
- **Alle rijdieren:** met de tweede regel heb je ze allemaal. N opent het scherm met kleuren, en daar kies je "Hierop rijden".
- **Rijden:** X is op- en afstijgen. Rijd met de Pluimguh, het Wolkenschaapje (dubbele sprong) en Slakkie.
- **Ronde 3, eerst zonder rijdier:** doe `/guhs vadsrijk mount reset`. Druk X en N: "Nog geen rijdier...". Praat met Stalmeester
  Hoefje (`/guhs vadsrijk npc tp stalmeester`): hij plaagt over het Wolkenschaapje, en de stal verkoopt nog niks.
- **Ronde 3:** er zijn nu 5 rijdieren (de Hoedhupper en de Klodderros zijn weg). Voel het verschil: de Guhwolf is het snelst
  maar springt laag, het Knuffelhert springt het hoogst, Slakkie is lekker traag.
- **Opstapjes:** loop zonder springen tegen een randje van 2 blokken: je rijdier stapt er zo op.
- **Silhouetten:** zie je die? Doe `/guhs vadsrijk mount reset` en open N opnieuw.
- **Afstijgen bij een baas:** `/guhs vadsrijk bazen tp koning` en rijd de arena in.
- **Stal:** `/guhs vadsrijk mount stal`.

### 7. Huisdieren (P)
```
/guhs vadsrijk huisdier geef koning_klodder
/guhs vadsrijk huisdier alle
/guhs vadsrijk huisdier weg
```
- **Scherm:** druk P. Je ziet "Verzameld x/y", met silhouetten voor wat je nog niet hebt.
- **Meenemen:** kies "Meenemen" en loop rond.
- **Teleport:** `huisdier weg` zet je huisdier 20 blokken weg. Kijk of hij met een poef terugkomt.
- **Echt verdienen:**
  1. `/guhs vadsrijk huisdier reset`.
  2. `/guhs vadsrijk baas reset bazen:koning_klodder` en dan `/guhs vadsrijk bazen tp koning`.
  3. Versla hem. Dan verschijnt "Nieuw huisdier!".

### 8. Reiskaart en kapiteins
```
/guhs vadsrijk npc tp kapitein
```
- **Kapitein:** klik op hem. Je krijgt de reiskaart, met grijze "?"-plekken en de teasers voor zone 3 en 4.
- **Kaart:** sleep met de muis om te schuiven en klik een open plek aan om te vliegen.
- **Scheepsbel:** rechtsklik op de bel opent ook de reiskaart.
- **Zone 2 vrijspelen:**
  1. `/guhs vadsrijk npc quest af hq12` en daarna `/guhs vadsrijk klasse generiek 20`.
  2. Kandijkom gaat dan open op de kaart.

### 9. Werver Wimpel (Guhmension)
- Ga terug: `/guhs vadsrijk`.
- Zoek de ruïne: `/locate structure guhs:vr_w_portaaltempel`, of zet Wimpel aan je voeten met `/guhs vadsrijk npc werver`.
- Raak het portaal aan vóór je met hem praat. Er gebeurt niks.
- Praat met Wimpel. Je krijgt het Wynn-grapje en het advies over de weergave. Daarna wordt het portaal wakker.
- Opnieuw testen: `/guhs vadsrijk npc werver vergeet` en `/guhs vadsrijk wereld vergeetportaal`.

### 10. Terug-quests (Hert en Wolf)
```
/guhs vadsrijk mount geef pluimguh
/guhs vadsrijk npc quest start rijdier_knuffelhert
/guhs vadsrijk npc tp boswachter_mos
```
- **Knuffelhert:**
  1. Je hebt al een rijdier, dus de quest staat in de questvolger.
  2. Steek de 3 lantaarns aan in het Sporenwoud.
  3. Volg de hoefsporen naar de open plek.
- **Guhwolf:** `/guhs vadsrijk npc quest start z2_rijdier_guhwolf` (zone 2, Kandijkom).

### 11. Questvolger en job-advancement
```
/guhs vadsrijk klasse klasselevel knuffelridder 10
```
- Rechtsboven verschijnt een lichtblauw blok met ✦, met een lichtblauwe straal naar de Grootmeester.
- **Tempeldeur:** `/guhs vadsrijk klasse generiek 18` en `/guhs vadsrijk bazen tp tempel`. Rechtsklik op de grote deur; hij gaat open.
- **Puzzelkamer:** loop een kamer in. De tip blijft onderin beeld staan.

### 12. Kaart zone 1
```
/guhs vadsrijk wereld plek aankomst
```
- **Kamp:** loop de kliftrap op. Er staat geen boom op de uitgang, en een pad met lantaarns loopt naar het kampvuur.
- **Bram:** `/guhs vadsrijk npc tp bakker_bram`. Hij staat in zijn kraam.
- **Kasteeltje:** `/guhs vadsrijk wereld plek ordetempel_binnen`. Kijk naar de torens van het kasteeltje.
- **Biomes:** F3+F6 of JourneyMap toont "Het Vadsrijk - <plek>".
- **Draak:** `/guhs vadsrijk sfeer draak stad` (50% groter). Zie je de opening graag nog eens: `/guhs vadsrijk npc opening`.
- **Opgevrolijkte wezens:** versla een klodder. Hij wordt doorzichtig en je slaat erdoorheen.

### 13. De nieuwe zone 2
```
/guhs vadsrijk z2wereld vlieg kandijkom
```
- **Kandijkom:** de terrassen, de klokkentoren, de kerstmarkt, interieurs (ga een huisje in), schaatsers en pinguhs.
- **Poorten:** loop alle vier de poorten uit, vooral oost naar de Galmhellingen. Er staat geen muur meer.
- **Gondel:** `/guhs vadsrijk z2wereld gondel op`.
- **Spiegelburcht:** `/guhs vadsrijk z2wereld plek burcht_trap_voet`.
- **Galmwolf:** `/guhs vadsrijk z2wereld plek arena_galmwolf_deur`.
- **Knuffelmeer:** loop het ijs op en weer af.

---
Valt je iets op? Noteer het commando-blok en wat je zag; dan kan ik het gericht fixen.

# B3. ZONE 3: DE SCHEMERWORTEL (lv 35-50)

## Nieuw in ronde U: zone 3 (De Schemerwortel, de Boom)
- **Honingtak:**
  - om de ~10 blokken een lantaarnpaal;
  - bankjes, prullenbakjes en bloembakken;
  - een kring rond de fontein;
  - overdag bijen en guhbijtjes (u-deco).
```
/time set day
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk udeco bijen
```
- **De wenteltrap in de Boom:** gekleurde banden per verdieping, banieren en lantaarntjes op de bordessen, en een naambord
  boven elk bordes. De hekken zijn verbonden.
```
/guhs vadsrijk z3wereld plek trap_bordes_2
/guhs vadsrijk z3wereld plek trap_bordes_5
```
- **Het Kruinnest** (`uitkijk_nest`): goede naam, één ring als reling, de lantaarns op de vloer.
```
/guhs vadsrijk indeling plek uitkijk_nest
```
- **De Kruinroofvogel-arena** heeft van onderen een organische nestkom (geen plaat meer). Er is altijd maar 1 Kruinroofvogel.
```
/guhs vadsrijk z3wereld plek honinglift_kruin_onder
/guhs vadsrijk z3bazen tp kruinroofvogel
/guhs vadsrijk dubbel scan
```
- **Rijdende guhs op Pluimguhs** (20 in zone 3):
  - over het Rijpad en de binnentrap;
  - rondjes op Honingtak;
  - over de takpaden;
  - bij de Pluimstal (met een verzorger);
  - op het oefenovaal en over de Wortelweg.
```
/guhs vadsrijk ruiters lijst
/guhs vadsrijk ruiters tp z3_wenteltrap
/guhs vadsrijk ruiters tp z3_honingtak
/guhs vadsrijk ruiters tp z3_pluimstal
```
- **De Wortelgrot** (lv 44-46), onder de Wortelvelden ten noorden van de stam. De ingang is een holle stronk naast de Wortelweg.
  Binnen: De Wortelknopen (schuifpuzzel), de Grote Wortelslang (elite) en een schat.
```
/guhs vadsrijk grot tp wortelgrot
```
- **Job-advancement 2 (lv 35):** bij Ordemeester Wasja in de Nestkapel, na de Kaaszeppelinwerf en de Pluimstal.

## Uit de nachtronde: zone 3

## De zone-3-tests (r-int, vernieuwde kaart)

### Z1. Erheen + de openingsfilm
```
/guhs vadsrijk z3npc klaarzetten
/guhs vadsrijk npc tp z2_kapitein
```
- Klaarzetten = zone 2 klaar + level 30. Klik Kapitein Poolster: "De Schemerwortel" staat in de keuzes en op de reiskaart.
  De stippellijnen op de reiskaart: Kandijkom → Schemerwortel (lijn B) en Schemerwortel → zone 4 (lijn C, nog "binnenkort").
- Kies hem: de **openingsfilm** (~26 s). Kijk of de camera nergens in een tak of blad zit. Je landt op de **Weststeiger**.
  Hoor je de **honingwals**?
- Nog een keer: `/guhs vadsrijk z3wereld vlieg opnieuw`. Terug: de scheepsbel bij de mast.

### Z2. Honingtak, de route en de Pluimguh-starter
```
/guhs vadsrijk npc questlog
```
- Raatje staat op het **Bloesemplein** (zuidkant van het ringdek). hq02: de Nestkapel is een honingraatcel ONDER het dek (kapellift).
  Daarna stuurt Raatje je OMLAAG: binnenhoflift (`schorslift_1`) en de noordpoort naar de **Pluimstal** op de grond.
- hq03: Veertje rijdt voor je uit het **rijpad** op (4 rondjes), langs het Bloesemplein, door de bladtunnel naar kruin 1. Zweef van
  het zweefplatform op de zuidrand van kruin 1 (spatie ingedrukt).
- Kijk rond: korfhuisjes met interieurs, de 3 cellen onder het dek (café, Nestkapel, Raattrossen) met hun liftjes, het marktje.

### Z3. Rijpad, binnentrap, windkolommen en liften
```
/guhs vadsrijk z3wereld plek rijpad_start
/guhs vadsrijk z3wereld plek trap_bordes_1
/guhs vadsrijk z3wereld plek binnenhof
/guhs vadsrijk z3wereld plek wind_honingtak_onder
/guhs vadsrijk z3wereld lift schorslift_2 op
```
- **Het Rijpad** (gebiedsnaam, was "De Stamspiraal"): rij het van `rijpad_start` tot `rijpad_eind` (kruin 1) op je Pluimguh, zonder te
  springen. Afslagen naar Honingtak en de takken, balkonnetjes om af te zweven.
- **De binnentrap**: van `trap_bordes_1` (vloer 80) tot `trap_bordes_6` (nest). Halve stapjes, leuning, deuren naar Honingtak en elke kruin.
- Windkolommen (zie TESTEN voor alle 7): erin lopen = omhoog naar het landingsdek.
- Liften: honinglift (pas na hq09), honinglift_kruin, lift_cafe, lift_kapel, lift_raattros, schorslift_1, schorslift_2.

### Z4. Bazen
```
/guhs vadsrijk level 50
/guhs vadsrijk z3bazen arenas
/guhs vadsrijk z3bazen spawn kruinroofvogel
/guhs vadsrijk z3bazen tp werf
/guhs vadsrijk z3bazen spawn piepvel
/guhs vadsrijk z3bazen tp hart
/guhs vadsrijk z3bazen vahoegnir
```
- **Kruinroofvogel**: de Kale Kruin (dode mesa aan kruin 1). Fase 3: op je rijdier het roze spoor om de boom volgen.
- **Werf** (eigen eiland, kabelbrug): Taleggio (Dok 1), Brokkel (Dok 2, toren), Roquefort (Dok 3, masttoren; wrak + trap terug).
- **Piepvel**: de Pelszaal ligt ONDER de Schorsstad-vloer (trap omlaag). Kijk naar zijn **cape** van muizenvelletjes (oortjes,
  staartjes; per kooi een lap minder). F1: grijze mist + twee Mika-handlangers. Ga daarna op de Schorsstad-vloer erboven staan:
  géén bazenbalk/intro.
- **Hart**: een ring kamers om de Hartslag-zaal; Wortelwurger, Nestwachter, Baron Bleu in looprichting.
- **Vahoegnir**: de **nieuwe draak** (roze, guh-kop, grote oren, 6 kleurstappen, paarse gloei-ogen, vleugels in een V in rust).
  `vahoegnir waar` / `vahoegnir reset` als hij zoek is.

### Z4b. Bazen-testgids (m-bazen): elke zone-3-baas, in kerkervolgorde
Eerst `/guhs vadsrijk level 50`, `/gamemode survival`, `/guhs vadsrijk vol`.
- **Eén gevecht tegelijk** ("Je bent nog in gevecht met ...") en **kerkervolgorde** ("Versla eerst ..."). `tp <baas>` en
  `spawn <baas>` geven de volgorde vrij voor jou; `spawn` zet de baas terug en jou bij zijn bord.
1. `spawn kruinroofvogel` (geen volgorde).
2. Werf: `tp werf`, dan `spawn taleggio`, `spawn brokkel`, `spawn roquefort`. Kamers: `tp loods`, `kerkers loods`, `kerkers reset`.
   Intro-check: vecht Taleggio, loop naar zijn dekrand: geen intro van Brokkel/Roquefort.
3. `spawn piepvel` (cape bekijken).
4. Hart: `tp hart` (chat geeft route + bazen), `spawn wortelwurger`, `spawn nestwachter`, `spawn bleu`; kamers `tp aderloop|kwekerij|ringen`.
5. Vahoegnir: `vahoegnir` (zet je aan de nestrand), `vahoegnir herinnering` (zonder verhaal), `vahoegnir waar|reset`.
- Fase overslaan: `/guhs vadsrijk z3bazen fase 2|3|4`. Vahoeg-vorm: `/guhs vadsrijk z3bazen vahoeg werf|hart`.

### Z5. De roze draak, de beestjes en de naambordjes
```
/guhs vadsrijk vriendjes draak nest
/guhs vadsrijk vriendjes draak bezoek
/guhs vadsrijk huisdier geef vahoegje
```
- `draak nest`: je staat aan de nestrand; de **roze Vahoegnir rust** daar (normaal alleen met `zone3_klaar`). Rechtsklik = aaien
  (hartjes, spinnen, buiging, een lief zinnetje). Tot 32 **beestjes** komen aanvliegen/huppelen: vinkjes, meesjes (op zijn rug),
  eekhoorntjes, konijntjes, roze muisjes, vlindertjes. Loop weg: ze vliegen/huppelen weg.
- `draak bezoek` (in zone 1/2, bij een plein/kampvuur): hij landt in de buurt, blijft 60-90 s, vliegt weer weg.
- Vahoegje: P, "Meenemen": het nieuwe **chibi-draakje**. Kijk ernaar: naambordje "Vahoegje van <jouw naam>" (weg met F1).

### Z6. Premiebord en het Verkenningsboek
```
/guhs vadsrijk z3systemen bord
/guhs vadsrijk z3systemen penningen 100
```
- Premiebord: drie tabs. B: pagina "De Schemerwortel"; staan "Het Rijpad", de dorpen en de binnentrap goed bij de gebieden?

### Z7. Lopen (survival)
- Rijpad helemaal op en af, binnentrap van de vloer tot het nest, de Pelszaal-trap, de Wortelgang naar de Hartslag-zaal en terug,
  de kabelbrug naar de werf en de torentrappen, de imkerdorpjes op de takken (huisjes leeg van blad?).
- Overal: kun je uit elk gat weer omhoog? (`U` = losknuffelen als het toch misgaat.)

### Z8. Praatscherm-portretten (q-dialoog)
```
/guhs vadsrijk npc portrettest
```
- Je ziet achter elkaar: een regel van jezelf (jouw eigen portret), Oma Truus als stand-in (zone 1, ver weg), een zone-3-imker als
  stand-in, de verteller (een boekje), en daarna een echte NPC uit de buurt.
- Praat daarna met een paar NPC's in Honingtak: staat er bij elke pagina een portret (ook bij jouw antwoorden)?
- Met F3+H: onderin een grijs regeltje (sleutel | tekst | spreker | bron van het portret).

---

## De geheime baas: Mika de Imker (zone 3, T-IMKER)

De geheime quest "De grauwe honing" (z3_geheim_imker) eindigt nu niet meer in een losse scène, maar in een echt baasgevecht
in de raatzaal onder de Raattros (Honingtak). Lv 47, Natuur. Beloning: huisdier **Zoempje** (een goudgeel bijtje), een trofee,
een buitkist.

> Let op: de raatzaal is groter gemaakt (straal 11, 10 hoog) en een stukje verschoven, zodat de ladder aan de rand uitkomt.
> Dat zit in de kaartgenerator; pas na de integratie-regen (`maak_kaart --zone 3`) staat de nieuwe zaal in de wereld.

### Snel testen
```
/guhs vadsrijk imker spawn
```
Je staat in de raatzaal, hij wordt meteen wakker (volle Vads, fase 1). Kijk naar:
- in het midden-noorden de **Koninginnekorf** (strokorf, koninginnetje bovenop), aan de rand **drie rookpotjes**;
- titelkaart **MIKA DE IMKER** / "Spion van de Keizer. Imker voor spek en bonen." en een tip-kaartje;
- een Mika in een wit imkerpak met strohoed, gaassluier, roker en een speldje van de Keizer op zijn borst.

**Fase 1 "Kwaliteitscontrole" (100-66%)**
- Rode cirkel = grauwe honingklodder. Daarna blijft een grijze **plas** liggen (20 tellen): erin = plakkerig (traag) + klein beetje schade.
- Rode strook voor hem = **kaasrook-puf** (opzij stappen!).
- "Een boze zwerm zoemt achter jou aan!": een zwart-gele wolk volgt je. **Rechtsklik een rookpotje** (rookwolk 8 tellen) en loop
  door de rook: "De zwerm wordt suf in de rook en valt in slaap." In de rook steekt geen bij je. Een potje moet daarna afkoelen
  (rechtsklik = "nog X tellen").

**Fase 2 "De grauwe koningin" (66-33%)**
- Hij springt op een stok naast de korf en giet grauwe honing erin (grijs draadje). Hij is dan **onkwetsbaar**
  ("Hij giet en is nergens te raken!").
- Elke 6 tellen spuit de korf bijen langs een **rode baan** uit het gat. Sta er niet in!
- **Rechtsklik de korf** = gat een zesde slag draaien (shift + rechtsklik = terug). Wijst de baan naar zijn stok, dan steken de
  bijen HEM: "AU! Niet mij! IK ben de imker!" -> 5 tellen verdoofd, je raakt hem extra hard, de grauw-meter zakt.
  Daarna klimt hij op een andere stok.
- Duurt het te lang (30 tellen gieten): "De koningin wordt grauw!" -> flinke klap op iedereen. De korf wordt zichtbaar grijzer.

**Fase 3 "De grote rookpot" (33-0%)**
- Hij komt naar beneden en jaagt weer; twee zwermen tegelijk, meer klodders.
- **Grote kaasrookbom**: gele cirkel bij de korf. Ga er (samen) in staan: de klap wordt gedeeld. Te weinig mensen erin = iedereen geraakt.
- Na 7 minuten: ALLES OF NIKS (enrage).

**Verslagen:** hij vlucht in een rookwolk ("Dit komt in mijn rapport! Met drie uitroeptekens!"), de korf wordt weer goud,
buitkist + Zoempje-moment.

### Het hele verhaal (zoals een speler het speelt)
```
/guhs vadsrijk imker klaarzetten
```
Quest loopt, 5/5 sporen, het luik is open. Je staat bij het luik in de Raattros-cel. Klim de ladder af:
1. In de zaal start de **intro-scène**. Controleer het **portret**: dat moet de Mika-imker zelf zijn (wit pak, strohoed), niet een boek.
   De verteller-pagina's tonen het boek (zo hoort het).
2. Scène dicht -> hij wordt wakker, titelkaart, gevecht.
3. Na de overwinning: **vervolgscène** (notitieboekje "Rapport aan de Keizer...", de koningin zingt, Zoempje), en de quest-stap
   "Versla de Mika-imker" vinkt af. Lever in bij de Raatwever.
4. Daarna kun je hem opnieuw bevechten (elke 10 minuten), zonder scènes; hij zegt dan "Jij weer?!".

### Losse commando's
| Commando | Wat |
|---|---|
| `/guhs vadsrijk imker spawn` | meteen het gevecht (fase 1, volle Vads) |
| `/guhs vadsrijk imker klaarzetten` | quest + sporen + luik open, je staat bij het luik |
| `/guhs vadsrijk imker fase 2` / `fase 3` | spring naar fase 2 / 3 |
| `/guhs vadsrijk imker steek` | (fase 2) draai de korf naar zijn stok en laat hem spuiten |
| `/guhs vadsrijk imker zwerm` | een zwerm achter jou aan |
| `/guhs vadsrijk imker bom` | de grote kaasrookbom (gele cirkel) |
| `/guhs vadsrijk imker intro` | alleen de intro-scène (portret checken) |
| `/guhs vadsrijk imker na` | alleen de vervolgscène |
| `/guhs vadsrijk imker zoempje` | huisdier Zoempje meteen krijgen |
| `/guhs vadsrijk imker waar` | waar is de zaal, fase, Vads, korfrichting, grauw-meter |
| `/guhs vadsrijk imker reset` | baas weg, scènes vergeten |
| `/guhs vadsrijk imker tp` | naar het luik |

Huisdier: in het huisdierenmenu staat **Zoempje** ("Een goudgeel bijtje van de bijenkoningin. Steekt nooit, zoemt altijd."),
een zwevend bijtje met fladderende vleugeltjes.

Gametests: `runGameTestServer -Pgt=VadsrijkImker` (3 tests).

# B4. ZONE 4: DE LAMPIONKUST (lv 50-56)

## Nieuw in ronde U: zone 4 (De Lampionkust)
- **Schildpadeiland:** de bloemetjes, lampionpaaltjes en de palm staan nu op het schild, en geheime knuffel 6 zit niet meer
  op Schelpie.
```
/guhs vadsrijk z4wereld plek schildpad_rug
/guhs vadsrijk z4wereld plek geheim_knuffel_6
```
- **De Koraalgrot** (lv 53-55), via een **duik**:
  - het vlot met lantaarn staat bij de oostrand van de Koraalbaai;
  - onder water een koraalboog, een tunnel met 3 luchtbellen;
  - de droge uitgang is een koraalschoorsteen met een wenteltrap.
  - Binnen: Het Schelpenpad (puzzel), Koning Mopperkrab (elite) en een schat.
```
/guhs vadsrijk grot tp koraalgrot
```
- **Ambachten T4:**
  - verzamelplekken in zone 4;
  - koraalduiken (houd vast en laat los: "Duiken!");
  - lampionhoning;
  - T4-recepten ook in de werkplaats van Lampionhaven.
```
/guhs vadsrijk ambacht tp z4_duik_koraalbaai
```
- **Uitkijkpunten met een naam:** De Vuurtorengalerij, De Lampionduin, De Pierkop, De Vliegerheuvel.
- **Natuurlijke overgangen:** de diepe kom van zone 4 loopt golvend af naar de open zee, en de wolken van zone 3 dunnen uit.

## Uit de nachtronde: zone 4

## SPIEK T-Z4WERELD - zone 4 "De Lampionkust" (de kaart)

Zone 4 is een slinger van eilandjes in een roze zee, rechts van de Schemerwortel (x 1740..2540, z -716..-266, lv 50-56).
Alles staat op de kaart; de wezens, het verhaal en het zeilbootje komen van andere slices. Je komt er nu met de dev-commando's
(teleport). Elke regel `/guhs vadsrijk z4wereld plek <id>` brengt je erheen (ook van buiten de dimensie). Tab-aanvullen werkt.
Losse coordinaten staan erachter als je liever `/tp` gebruikt (in de dimensie `guhs:vadsrijk`).

**Kaart opnieuw maken:** `python tools/vadsrijk/maak_kaart.py --zone 4` (~3 min). Controle: `python tools/vadsrijk/zone4/loopcheck4.py`,
`python tools/vadsrijk/zone4/poicheck4.py`, `python tools/vadsrijk/zone4/audit4.py`. Een NIEUWE wereld/regen is nodig om de tegels te zien
(chunks die al bestaan veranderen niet).

### Lampionhaven (de hub)

- `/guhs vadsrijk z4wereld plek respawn`  (1848 67 -467) Het Havenplein: fonteintje met guh-beeldje, de lampionmast met snoeren als een tentje, kampvuur ernaast (unlit = checkpoint).
- `/guhs vadsrijk z4wereld plek kade`  (1866 66 -470) De kade met de 3 steigers en bootjes. Loop de steigers op: de lampionsnoeren hangen nu boven je hoofd, aan het eind een ladder uit het water.
- `/guhs vadsrijk z4wereld plek haven_3_4_landing`  (1796 71 -468) De luchthaven (lijn C naar de Boom): platform op palen met het aangemeerde luchtschip, vertrekbord. Trap aan de oostkant naar beneden.
- `/guhs vadsrijk z4wereld plek gebouw.vliegerwinkel.deur`  (1844 75 -481) Vliegerwinkel (winkel): toonbank, weefgetouw. Trap van de deur naar beneden moet aansluiten.
- `/guhs vadsrijk z4wereld plek gebouw.winkel.deur`  (1843 67 -445) Algemene winkel: toonbank + kist.
- `/guhs vadsrijk z4wereld plek gebouw.herberg.deur`  (1829 68 -466) Herberg: bed, tafeltjes.
- `/guhs vadsrijk z4wereld plek gebouw.bank.deur`  (1829 72 -480) Bank: toonbank + goudblok (kluis).
- `/guhs vadsrijk z4wereld plek gebouw.lampionwerkplaats.deur`  (1835 67 -448) Lampionwerkplaats (ambacht): werkbank + weefgetouw, afdak vol lampions.
- `/guhs vadsrijk z4wereld plek gebouw.kookhuis.deur`  (1852 67 -445) Kookhuis (ambacht): smoker + werkbank.
- `/guhs vadsrijk z4wereld plek gebouw.havenmeester.deur`  (1860 66 -447) Havenmeester: toonbank + lessenaar.
- `/guhs vadsrijk z4wereld plek gebouw.botenverhuur.deur`  (1870 66 -490) Botenverhuur op de noordarm (paalwoning, loopplank vast aan de kade).
- `/guhs vadsrijk z4wereld plek gebouw.visrestaurant.deur`  (1909 66 -492) Visrestaurant 'De Blije Bot' met terras boven het water.
- `/guhs vadsrijk z4wereld plek gebouw.stal.deur`  (1818 67 -452) Stal + de wei met hek (hooi, waterbak).
- `/guhs vadsrijk z4wereld plek boot_dok_lampionhaven`  (1891 65 -464) Het eind van de middelste steiger: hier vertrekt het zeilbootje (t-z4boot).
- `/guhs vadsrijk z4wereld plek uitkijk_lampionduin`  (1860 90 -517) Uitkijkpunt op de Lampionduin: panorama op de Boom (zone 3). Trappenpad vanaf het plein.
- `/guhs vadsrijk z4wereld plek vuurtorentje`  (1912 64 -436) Het vuurtorentje met oortjes op de punt van de zuidarm.

### De eilandjes

- `/guhs vadsrijk z4wereld plek eiland_knuffeltij`  (1935 67 -345) Knuffeltij-strand: grauwe knuffels in de branding, het Opvangcentrum (binnen: knuffels op de toonbank), knuffelwas, droogwaslijnen, soepkraampje.
- `/guhs vadsrijk z4wereld plek eiland_strandfeest`  (1995 68 -605) Strandfeesteiland: parasols, kampvuur (checkpoint) met dansplek, schelpenlimonadebar, beachvolleybal.
- `/guhs vadsrijk z4wereld plek eiland_bloemeneiland`  (2085 71 -405) Bloemeneiland: bloemenstroken, bijenboerderij (binnen), bijenkorven, picknickkleedjes.
- `/guhs vadsrijk z4wereld plek eiland_koraalbaai`  (2185 64 -565) Koraalbaai: atol met roze lagune. Duiksteiger vanaf de noordring, het ronde vlot in het midden = de arena van de Grauwe Zeeslurf.
- `/guhs vadsrijk z4wereld plek eiland_vliegereiland`  (2299 89 -665) Vliegereiland: vliegers (wol, met oortjes en oogjes) aan touwtjes op de helling, wedstrijdpaal op de top, vliegerwerkplaats.
- `/guhs vadsrijk z4wereld plek eiland_vuurtoreneiland`  (2404 83 -490) Vuurtoreneiland: de grote vuurtoren (deur aan de zuidkant, wenteltrap naar de galerij) + het huisje van de wachter.
- `/guhs vadsrijk z4wereld plek eiland_pierkop`  (2470 73 -565) Pierkop: de Opstijgpier (pier op palen naar het oosten, rond platform met meermast).
- `/guhs vadsrijk z4wereld plek eiland_mysterie`  (2480 79 -330) Mysterie-eiland: GRIJZE MIST (de Grauwe Mistbank, zicht ~14 blokken), vervallen grauwe poort, bleke bomen.
- `/guhs vadsrijk z4wereld plek schildpad`  (2239 72 -445) Schildpadeiland (stilstaande versie): schild met hutje, palm en bloementuintje; drijvende steiger aan de flank met pad omhoog.
- `/guhs vadsrijk z4wereld plek wrak_ingang`  (2178 67 -700) Het Wrak van de Vliegervanger (kerker): gestrand Mika-schip op een rots in het zuiden.

### Het Wrak van de Vliegervanger (kleine kerker)

- `/guhs vadsrijk z4wereld plek wrak_aanleg`  (2192 64 -700) Steiger met ladder; loop over de rots, 3 treden op naar het achterdek.
- `/guhs vadsrijk z4wereld plek wrak_luik`  (2176 67 -705) Het luik in het achterdek: 9 treden naar beneden het ruim in.
- `/guhs vadsrijk z4wereld plek wrak_ruim`  (2175 57 -700) Kamer 1, het ruim (tonnen). Deuropening naar het westen.
- `/guhs vadsrijk z4wereld plek wrak_kombuis`  (2165 57 -700) Kamer 2, de kombuis (smoker).
- `/guhs vadsrijk z4wereld plek wrak_knuffelkooi`  (2155 57 -700) Kamer 3, de knuffelkooi: gestolen knuffels achter tralies, schatkist. Trap omhoog naar het voordek.
- `/guhs vadsrijk z4wereld plek arena_vliegervanger`  (2158 67 -700) Het voordek = de baasarena (de Vliegervanger), met de grauwe oorlogsvlieger aan de mast.

### Koraalbaai (wereldbaas + duiken)

- `/guhs vadsrijk z4wereld plek duiksteiger`  (2185 64 -605) Duiksteiger vanaf de noordring de lagune in; aan het eind een trapje en een drijvend looppad naar het vlot.
- `/guhs vadsrijk z4wereld plek arena_zeeslurf`  (2185 64 -565) Het vlot in de lagune = de arena van de Grauwe Zeeslurf (lampionnetjes langs de rand).
- `/guhs vadsrijk z4wereld plek grotje_1_uitgang`  (2171 63 -565) Boven grotje 1. Duik naar beneden: elk grotje heeft een schacht met een BELLENKOLOM omhoog (snel weer boven + lucht).

### Opstijgpier (naar zone 5)

- `/guhs vadsrijk z4wereld plek opstijgpier_begin`  (2472 71 -565) Begin van de pier op het eiland.
- `/guhs vadsrijk z4wereld plek haven_4_5_landing`  (2517 73 -562) Het ronde platform: hier landt/vertrekt het luchtschip naar Hoogvads (lijn D). Vertrekbord, meermast met oortjes.
- `/guhs vadsrijk z4wereld plek opstijgpier_cliffhanger`  (2521 73 -565) Het eind van de pier (de cliffhanger-plek van het verhaal).

### Kampvuren (allemaal unlit = checkpoint; rechtsklik)

- `/guhs vadsrijk z4wereld plek kampvuur_bloemeneiland`  (2089 70 -401)
- `/guhs vadsrijk z4wereld plek kampvuur_knuffeltij`  (1949 67 -343)
- `/guhs vadsrijk z4wereld plek kampvuur_koraalbaai`  (2177 71 -615)
- `/guhs vadsrijk z4wereld plek kampvuur_lampionhaven`  (1839 67 -460)
- `/guhs vadsrijk z4wereld plek kampvuur_mysterie`  (2466 77 -324)
- `/guhs vadsrijk z4wereld plek kampvuur_pierkop`  (2466 71 -573)
- `/guhs vadsrijk z4wereld plek kampvuur_strandfeest`  (1989 68 -595)
- `/guhs vadsrijk z4wereld plek kampvuur_vliegereiland`  (2278 64 -641)
- `/guhs vadsrijk z4wereld plek kampvuur_vuurtoreneiland`  (2404 82 -486)

### Zeilbootsteigers (elk met een ladder uit het water)

- `/guhs vadsrijk z4wereld plek boot_dok_bloemeneiland`  (2028 65 -405)
- `/guhs vadsrijk z4wereld plek boot_dok_knuffeltij`  (2002 65 -345)
- `/guhs vadsrijk z4wereld plek boot_dok_koraalbaai`  (2114 65 -565)
- `/guhs vadsrijk z4wereld plek boot_dok_lampionhaven`  (1891 65 -464)
- `/guhs vadsrijk z4wereld plek boot_dok_mysterie`  (2441 65 -330)
- `/guhs vadsrijk z4wereld plek boot_dok_pierkop`  (2470 65 -530)
- `/guhs vadsrijk z4wereld plek boot_dok_schildpad`  (2258 64 -431)
- `/guhs vadsrijk z4wereld plek boot_dok_strandfeest`  (1995 65 -558)
- `/guhs vadsrijk z4wereld plek boot_dok_vliegereiland`  (2300 65 -621)
- `/guhs vadsrijk z4wereld plek boot_dok_vuurtoreneiland`  (2365 65 -500)
- `/guhs vadsrijk z4wereld plek boot_dok_wrak`  (2192 64 -700)

### Uitzichtpunten (panorama bij eerste bezoek)

- `/guhs vadsrijk z4wereld plek uitkijk_lampionduin`  (1860 90 -517)
- `/guhs vadsrijk z4wereld plek uitkijk_pierkop`  (2509 73 -559)
- `/guhs vadsrijk z4wereld plek uitkijk_vliegerheuvel`  (2301 91 -663)
- `/guhs vadsrijk z4wereld plek uitkijk_vuurtoren`  (2400 128 -495)

### Geheimen (een knuffel of een kist op de plek)

- `/guhs vadsrijk z4wereld plek geheim_knuffel_1`  (1916 63 -426)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_2`  (2201 48 -563)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_3`  (2350 70 -585)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_4`  (2488 81 -326)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_5`  (1790 69 -610)
- `/guhs vadsrijk z4wereld plek geheim_schat_1`  (2519 64 -564)
- `/guhs vadsrijk z4wereld plek geheim_schat_2`  (2240 64 -640)
- `/guhs vadsrijk z4wereld plek geheim_schat_3`  (2183 46 -579)
- `/guhs vadsrijk z4wereld plek geheim_schat_4`  (2425 63 -400)

### Randen, mist, gebieden

- `/guhs vadsrijk z4wereld stroming`: je staat in het water aan de zuidrand. **Wat moet er gebeuren:** je drijft langzaam terug naar het noorden
  (de Kringstroom) en ziet even "De Kringstroom duwt je zachtjes terug naar de eilandjes...". Zwem je harder naar buiten, dan kom je niet ver.
- `/guhs vadsrijk z4wereld mist`: het strand van het Mysterie-eiland. **Wat moet er gebeuren:** grijze mist, kort zicht, mistwolkjes;
  vaar/zwem weg naar het westen en de mist trekt zacht op.
- `/guhs vadsrijk z4wereld gebied`: zegt het gebied (z4_...), het kern-gebied, de mobzone, de biome, de stroming en de mist waar je staat.
  F3 laat de biome zien als "Het Vadsrijk - <eiland>" (alleen in een NIEUWE wereld/nieuwe chunks).
- `/guhs vadsrijk z4wereld eiland <naam>` (tab) / `/guhs vadsrijk z4wereld dok <naam>` / `/guhs vadsrijk z4wereld uitkijk <naam>`.

### Wat er (nog) NIET is

- Geen wezens/NPC's/quests (andere slices). De NPC-plekken staan klaar in POI_Z4.md (`gebouw.*.npc`, `npc_*`).
- De schildpad zwemt nog niet: het eiland ligt stil op het begin van de ronde (de route staat in `SCHILDPAD_ROUTE`).
- De vliegers zijn blokken (wol + touw), geen bewegende entities. Hoogvads (zone 5) hangt NIET in deze tegels.

## Spiekbrief zone 4 "De Lampionkust": het verhaal (t-z4verhaal)

Alles hieronder speel je in de Vadsrijk-dimensie, als OP. Een commando begint altijd met `/guhs vadsrijk z4verhaal ...`.

**Wat je ziet als iets van een ander nog ontbreekt:**
- **Het zeilbootje** is van t-z4boot. Zonder bootje zegt Botenguh Zeiltje dat het nog gebouwd wordt, maar de stap telt wel. Steek dan zwemmend over, of tp.
- **De bazen** (Zeeslurf, Kootje) zijn van een andere slice. Zonder die slice krijg je een kort gesprekje in plaats van een gevecht.
- **Het echte luchtschip** is van t-luchtschip. Zonder dat vlieg je met een camerafilmpje.
- **De kaart** is van t-z4wereld. Zonder die kaart staan NPC's en plekken op de render-coördinaten.

### Snel klaarzetten
| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z4verhaal klaarzetten` | Zone 2 en 3 zijn klaar en je bent minstens level 50. Wasja in de Nestkapel biedt nu "Lampions aan de kust" aan. |
| `/guhs vadsrijk z4verhaal hoofdlijn 1` ... `7` | Spring naar hoofdstap 1-7: alles ervoor is ingeleverd en de stap loopt. Je level wordt minstens 49+stap. |
| `/guhs vadsrijk z4verhaal skip` | Alle doelen van de lopende zone-4-hoofdquest staan op klaar. Lever zelf in bij de NPC die de tracker aanwijst. |
| `/guhs vadsrijk z4verhaal quest <id> start` / `klaar` / `af` | Eén quest starten, alle doelen klaarzetten, of meteen afronden (met beloning, zonder gesprek). |
| `/guhs vadsrijk z4verhaal tp <plek>` | Teleport naar een NPC (`z4_havenmeester`), een POI (`poi.z4_wrak`), een Knuffeltij-knuffel (`tij.strand_1`), een geheim (`knuffel.knuffel_1`, `fles.fles_1`, `schat.mist`, `uitzicht.pierkop`). Tab vult aan. |
| `/guhs vadsrijk z4verhaal status` | Toont zone4_klaar, de zone-5-luchtschipvlag, het bootje en de tellers (knuffeltij x/12, knuffels x/8, flessenpost x/6). |
| `/guhs vadsrijk z4verhaal vergeet` | Wist alle zone-4-quests, knuffels, geheimen en gezien-scènes (om opnieuw te testen). |
| `/guhs vadsrijk z4verhaal opening` / `vlieg` / `mist` / `feest` / `wolkwal` / `cliffhanger` / `wrak` / `zeeslurf` | Speel één scène los af. Let op: `cliffhanger` zet ook zone4_klaar en de zone-5-vlag. |

De algemene questcommando's werken ook: `/guhs vadsrijk npc quest af z4_hq03_waarvandaan` en `/guhs vadsrijk npc tp z4_opvang`.

### De hoofdlijn "De Grote Knuffeltij" (7 stappen, lv 50-55)
Volg steeds de tracker (rechtsboven) en de lichtstraal. Elke stap zegt wie, waar en wat.

#### 1. Lampions aan de kust (`z4_hq01_lampionkust`)
- **Start:** Ordemeester Wasja in de Nestkapel (zone 3).
- **Wat gebeurt er:** na "Ja" vlieg je naar Lampionhaven.
  - De eerste keer zie je een filmpje: door de Wolkenval, over de eilandjes, Hoogvads op de walvis en grauwe vlekjes op het Knuffeltij-strand.
  - Je landt op het luchthavenplatform.
- **Daarna:** praat met **Havenmeester Ankertje** op de kade, bij de middelste steiger.
- **Kijk naar:**
  - Komt het filmpje mooi aan bij Lampionhaven?
  - Staat Ankertje echt op de kade?
  - Op de reiskaart heet zone 4 nu "De Lampionkust" (was "???").

#### 2. De Grote Knuffeltij (`z4_hq02_knuffeltij`)
- Klik op **Botenguh Zeiltje** aan de noordelijke steiger: je krijgt je zeilbootje (of "wordt nog gebouwd").
- Vaar naar het **Knuffeltij-strand** (zuidoost) en praat met **Wasguh Sopje** bij de knuffelwas.
- Loop langs de vloedlijn naar **6 grauwe knuffels** (grijze stofjes in de branding).
  - Elke knuffel vertelt een klein verhaaltje en wordt roze, met hartjes.
  - De teller loopt tot x van de 12.
- Lever in bij **Opvangmoeder Zachtje** in het opvangcentrum.
- Snel testen: `tp tij.strand_1` ... `tij.strand_6`.

#### 3. Waar kom jij vandaan? (`z4_hq03_waarvandaan`)
Er zijn 4 grote knuffels, elk met een **zachte flash van het grauwe oosten**: een grijze titel, grauwe as en 3 herinneringen.
- Strandfeesteiland zuidkust: de walrus. Posters "MAGER IS MOOI", de zuigende buis.
- Bloemeneiland westkust: de giraf. De lange grauwe weg, karren vol warme dingen, de burcht.
- Koraalbaai zuidring: de kwal. De Grauwe Vloot, de kapitein met ringen om haar tenen.
- Op de schildpad: de draak. De troonzaal, vijf vingertjes tik-tik-tik, "Meer."

Daarna praat je met **Opa Lichtstraal** onder de vuurtoren. Hij zegt: "ze komen uit de Grauwe Mistbank".

Snel testen: `tp poi.z4_grote_knuffel_1` tot `_4`.

#### 4. De Grauwe Mistbank (`z4_hq04_mist`)
- Ga naar de **grauwe poort op het Mysterie-eiland** (zuidoost).
  - Je ziet de **mistflash**: een grauwe weg, Mika-karren, de zuigende buis, en een burcht als een kom onder een maan die kijkt.
- Praat met **Verdwaalde Nevel** (een vluchteling). Hij vertelt over de Grauwe Vloot en de Ringvinger.
- Lever in bij **Zachtje**: "Er zijn knuffels gestolen van de waslijn!" Stap 5 start vanzelf.

#### 5. De Vliegervanger (`z4_hq05_vliegervanger`)
- Ga naar het **Wrak** ten noorden van de Koraalbaai en dan het ruim in.
  - Met de bazen-slice: het gevecht tegen Kootje.
  - Zonder: een grappig gesprekje (zijn vlieger zit vast aan zijn eigen staart, plons).
- Pak de **2 gestolen knuffels** in het ruim. Dat zijn nummer 11 en 12: "Alle twaalf knuffels zijn weer roze!"
- Lever in bij **Zachtje**.

#### 6. Het terug-naar-huis-feest (`z4_hq06_feest`)
- Haal de spullen voor het feest:
  - lampions bij **Gloeiwiek** (Lampionhaven, westkant van het plein);
  - vliegers bij **Wimpel** (Vliegereiland);
  - limonade bij **Bubbel** (Strandfeesteiland).
- Ga naar de **feestplek** op het Knuffeltij-strand. Het feestfilmpje begint:
  - lampions, 12 vliegers met knuffels, en Grijsje vindt haar knuffel Muis terug;
  - Opa Dun lacht;
  - Zachtje zegt: "Welkom thuis".
- Meteen daarna **zakt Moeder Wolkwal** laag over de zee. Je hoort een walvislied en ziet een moe oog en een grauw sliertje.
- Lever in bij **Havenmeester Ankertje**.

#### 7. De Opstijgpier (`z4_hq07_opstijgpier`)
- Vaar naar de **Pierkop** (oost). Praat met **Pierwachter Wolkje** en loop de pier op naar **Kapitein Hemelhoog**.
- Lever in. Na het gesprek volgt de **cliffhanger**:
  - het luchtschip stijgt op langs het oog van de walvis, en je ziet Hoogvads;
  - terug op de pier dwarrelt uit de mouw van een guh met een wit-gouden cape een **grauw pluisje** (een hint naar de spion van zone 5);
  - daarna verschijnt "Wordt vervolgd...".
- **Kijk naar:** `status` geeft nu `zone4_klaar=true` en `z5_luchtschip_open=true`.

### Zijquests (16)
| Id | Bij wie / waar | Wat (en hoe snel testen) |
|---|---|---|
| `z4_s01_vlieger` | Wimpel, Vliegereiland | 4 vliegeronderdelen op het eiland (wolkjes). Je krijgt een guh-vlieger. |
| `z4_s02_wedstrijd` | Zwiep, wedstrijdpaal op de top | Blijf 30 s met de vlieger in je tas bij de paal: windstoten, en een titel als je wint. Loop je weg, dan begin je opnieuw. |
| `z4_s03_vissen` | Visser Dobber, zuidsteiger | Vang 5 vissen met een hengel, ergens in zone 4. |
| `z4_s04_parels` | Duikmeester Parel, duiksteiger Koraalbaai | 4 parels onder water (`tp poi.z4_grotje_1`). |
| `z4_s05_zeeslurf` | Parel | Duik naar grotje 1: de Grauwe Zeeslurf. Zonder bazen-slice: een gesprekje met roze bellen. |
| `z4_s06_volleybal` | Smash, Strandfeesteiland | Een klikspel: raad links, midden of rechts. **Tip: het wiebelende oortje wijst de kant aan** (bij "midden" fluit hij). Wie 3 punten heeft, wint. |
| `z4_s07_limonade` | Bubbel | 6 schelpjes op de stranden van het Strandfeesteiland. |
| `z4_s08_vuurtoren` | Opa Lichtstraal | Klim de vuurtoren op en blijf 5 s bij de lens. Hij vertelt over de Grauwe Vloot en een Mika die met één vinger wees. |
| `z4_s09_bijen` | Imker Nectar, Bloemeneiland | 5 bijtjes. |
| `z4_s10_kransen` | Madelief, Bloemeneiland | 6 bloemen. Je krijgt een bloemenkrans. |
| `z4_s11_schildpad` | Kluizenaar Schelpie, op de schildpad | 5 zeewier van zone-4-wezens (40% kans per wezen). |
| `z4_s12_mistbrieven` | Verdwaalde Nevel | 3 briefjes in de mist. Je leest zijn vluchtdagboek. |
| `z4_s13_soep` | Soepguh Lepeltje | Breng 4 kommen soep naar Grijsje, Opa Dun, Vaaltje en Mama Pluis. |
| `z4_s14_lampionnen` | Gloeiwiek | Hang 5 lampions op bij de lege haken in Lampionhaven (gouden glinsters). |
| `z4_s15_strandavond` | Confetti, kampvuur Strandfeest | Blijf 10 s bij het vuur. Je danst. |
| `z4_s16_flessenpost` | Grijsje (na stap 5) | Vaar met haar flesje naar de Kringstroom ten zuiden van het strand. |

**Hoe snel testen:** start een quest met `quest <id> start`, tp naar de plek en loop erheen. Of zet alles klaar met `quest <id> klaar` en lever in.

### Geheimen (Verkenningsboek, toets B, pagina "De Lampionkust")
- **8 knuffels** (hartjes): `tp knuffel.knuffel_1` t/m `_8`.
- **6 flessenpost** (blauwe glinsters): lore over de Keizer, Nooitgenoeg en de Ringvinger. `tp fles.fles_1` t/m `_6`.
- **4 schatten:** `schat.duintop`, `schat.lagune`, `schat.vliegertop`, `schat.mist`.
- **4 uitzichtpunten:** de Boom achter je, Hoogvads voor je, de Mistbank. `uitzicht.duin`, `uitzicht.vlieger`, `uitzicht.vuurtoren`, `uitzicht.pierkop`.
- **Verder in het boek:**
  - de bazen (Zeeslurf, Vliegervanger);
  - de kampvuren van 8 eilanden;
  - de gebieden;
  - een titel "Kenner van de Lampionkust".

### Diensten in Lampionhaven
| NPC | Dienst |
|---|---|
| Luchthavenguh Windvaan (luchthavenplatform) | Reiskaart en vluchten terug |
| Bankier Muntschelp | Kluis (rol `bank`) |
| Werkmeester Krul | Werkplaats (rol `werkplaats`, van t-ambacht) |
| Winkelguh Strandtas | Winkel |
| Smidguh Ankerslag | Smid |
| Harpoentje | Wapens |
| Zeemeeuw | Kleermaker |
| Schelpje | Markt |
| Kok Garnaal (De Blije Bot) | Gratis vissoep, 1x per 2 min |
| Bubbel | Limonade |
| Lepeltje | Soep |

**Kustwacht:** bij elk kampvuur staat een kustwacht.

### Wat moet je in de dev-client vooral checken
1. **NPC-plekken op de echte kaart:**
   - Staat niemand in een muur of in het water?
   - Staan de vluchtelingen bij het opvangcentrum?
2. **De knuffelplekken langs de vloedlijn:** liggen er echte grauwe knuffelblokjes? Dat is van t-z4wereld.
3. **De camerapaden:**
   - opening, mist, feest, wolkwal, cliffhanger;
   - kijkt de cliffhanger echt naar de walviskop?
4. **De tracker:** wijst hij vanuit zone 3 eerst naar Kapitein Zoemzeil, en in zone 4 naar het juiste eiland?
5. **De reiskaart:** staat "De Lampionkust" open na zone 3 en vlieg je erheen?

## Spiekbrief T-Z4BOOT: je eigen guh-zeilbootje (zone 4, De Lampionkust)

Alle commando's werken met op 2. In het Vadsrijk testen (`/guhs vadsrijk` brengt je erheen).

### 1. Snel varen (overal op water, ook zonder zone 4)
```
/guhs vadsrijk boot varen
```
- Geeft je het bootje (+ questvlag) en zet **proefvaart** aan: dan mag het bootje overal op water. Het verschijnt op het dichtstbijzijnde open water (3x3 water met lucht erboven, binnen 6 blokken) en je zit er meteen in.
- Geen water in de buurt? Dan krijg je "Hier is geen plek voor je bootje". Zoek een meertje of de zee op en probeer opnieuw.
- **Wat je moet zien:** een roze bootje met een guhgezichtje op de boeg (met oortjes), een zeil met een guhgezicht en oortjes bovenop, een hartjeswimpel bovenin de mast, een guhkopje als boegbeeld, een klein guh-matroosje voorin met een matrozenpetje, en achterin een lantaarntje aan een paaltje dat over het water hangt (gloeit). Jij zit achterin op het bankje bij het roer.

### 2. Besturing (het "zeilgevoel")
- **W** = zeil hijsen: het bootje komt langzaam op gang (ongeveer 3 seconden tot volle vaart, ~12 blokken per seconde op level 1).
- **Loslaten** = het bootje glijdt nog even door en wordt langzaam trager.
- **S** = remmen, en daarna heel langzaam achteruit.
- **A/D** = de boeg draaien. Je beeld draait mee. Op volle snelheid draai je wat trager.
- Het bootje **dobbert** zachtjes op en neer en schommelt (animatie). Bij snelheid zie je spetters bij de boeg en belletjes in het kielzog, en hoor je af en toe het water.
- **Wind:** rechts op je scherm staat een windpaneeltje met een pijltje (pijl omhoog = wind van achteren), "mee!" / "zijwind" / "tegenwind" en je snelheid in blokken per seconde. Met de wind mee ga je ~15% sneller, ertegenin ~15% trager. De wind draait langzaam mee met de dag.
- Op het strand of op land schuurt het bootje en komt het bijna niet vooruit (het klimt niet op het land).
- `/guhs vadsrijk boot waar` laat zien hoe de wind staat, waar je bent t.o.v. de Kringstroom en of er een steiger in de buurt is.

### 3. Rijdier-toets X en het N-scherm
- **X in het water of op een steiger (zee van zone 4)** = je bootje. **X op het land** = je landrijdier (ook als je het bootje als actief rijdier hebt gekozen).
- Heb je alleen het bootje en sta je op het land: "Je zeilbootje vaart alleen op het water".
- **N** (Rijdieren): het Zeilbootje staat in de lijst (6e rij) met level, xp en snelheid. Varen geeft xp, net als rijden.
- Bovenaan zit een nieuw **tabblad "Zeilbootje"** (tussen Rijdieren en Huisdieren). Ook: `/guhs vadsrijk boot scherm`.

### 4. Aankleden (varianten)
In het tabblad Zeilbootje: links het draaiende bootje, rechts drie rijen: **Zeil**, **Wimpel**, **Boegbeeld**.
- Klik een vakje: het voorbeeld laat het meteen zien. Daarna "Dit wil ik!" (heb je het al) of "Kopen (prijs)".
- Zeilen: roze (heb je meteen), munt / lavendel / zeeblauw (600 brons), honing (900), sterrennacht (beloning).
- Wimpels: hartje (meteen), ster / streepjes (300), Vadsrijk (beloning).
- Boegbeelden: guhkopje (meteen), dolfijntje / sterretje (800), draakje (beloning).
- Alles in één keer: `/guhs vadsrijk boot varianten`. Eén kiezen: `/guhs vadsrijk boot variant zeil_sterrennacht` (of `boeg_draakje`, `wimpel_ster`, ...).
- **Kijk:** als je vaart en iets anders kiest, verandert het bootje meteen.

### 5. Steigers (aanmeren) en uitloggen op zee
```
/guhs vadsrijk boot steigers
/guhs vadsrijk boot naar lampionhaven
```
- `steigers` toont alle aanlegsteigers met afstand (* = je laatste). **Let op:** zolang zone 4 nog niet gebouwd is, zijn dit placeholders (bij de eilandmiddens van de wereldindeling, y 64). Daar is dan nog geen water: proefvaart (punt 1) is dan de manier om te testen.
- Vaar je binnen ~10 blokken langs een steiger: in beeld "Steiger X: druk op Shift om aan te meren". Die steiger wordt je "laatste steiger".
- **Shift** naast een steiger = je stapt op de planken van de steiger (niet in het water). Het bootje poeft weg.
- **Uitloggen op zee** (terwijl je in het bootje zit): bij het inloggen sta je op je laatste steiger, met een berichtje. Snel testen zonder uit te loggen: `/guhs vadsrijk boot thuis`.

### 6. De Kringstroom (de rand van zone 4)
- Een ellips rond alle eilanden (midden 2140, -491; 470 x 290 blokken). In de buitenste rand (de laatste ~7%) duwt de stroom je terug: je snelheid naar buiten valt weg, je boeg draait vanzelf terug naar binnen, je ziet schuimwolkjes en "De Kringstroom duwt je bootje terug naar de eilanden!".
- **Wat moet gebeuren:** je komt er NOOIT doorheen, ook niet met volle zeilen recht naar buiten, en je raakt nooit vast: loslaten = je drijft vanzelf terug naar open water.
- Kan pas echt getest worden als de zee van zone 4 er is (t-z4wereld).

### 7. Geen andere bootjes
- Probeer in het Vadsrijk een gewone eiken boot of bamboevlot te plaatsen: dat gaat niet ("Andere bootjes drijven hier niet: in het Vadsrijk vaar je met je eigen guh-zeilbootje"). Dat geldt ook voor boten van andere mods (alles wat *boat* of *raft* heet).

### 8. Het bootje krijgen (verhaal) en de rest
- `/guhs vadsrijk boot geef`: zoals het verhaal het geeft (questvlag + tip "Je eigen zeilbootje!").
- `/guhs vadsrijk boot fles`: het item "Bootje in een fles" (rechtsklik = het bootje is van jou).
- `/guhs vadsrijk boot status`: bootje, questvlag, op zee, laatste steiger, level, gekozen uiterlijk.
- `/guhs vadsrijk boot proefvaart aan|uit`, `/guhs vadsrijk boot reset` (alles van het bootje weg).
- Gewone rijdier-commando's werken ook: `/guhs vadsrijk mount rijd zeilbootje`.

## T-Z4WEZENS: de wezens, de Grauwe Zeeslurf en het Wrak van de Vliegervanger (zone 4, De Lampionkust)

**Wat is er nieuw?**
- **9 grauwe kustwezens** die je **opvrolijkt** (net als in zone 1): ze zijn grijs en chagrijnig, en na de laatste klap krijg je een
  **roze plof**. Ze worden fel van kleur met een blij gezichtje en blosjes, en huppelen nog ~10 seconden rond.
- **De Mika-vliegerknechten** in het Wrak worden NIET opgevrolijkt: die vluchten in een rookwolkje ("Dit komt in mijn rapport!").
- **Licht gevecht:**
  - weinig wezens (max ~40 tegelijk in de hele zone);
  - elk speciaal trucje heeft een rode of paarse waarschuwing;
  - geen wezens op Lampionhaven, het Knuffeltij-strand, het Strandfeesteiland, het Vuurtoreneiland, het Schildpadeiland en de Pierkop.
- **Wereldbaas De Grauwe Zeeslurf** (lv 54) in de lagune van de Koraalbaai, met duiken.
- **Kleine kerker: het Wrak van de Vliegervanger**, met eindbaas **Kootje de Vliegervanger** (lv 56): een Mika met een oorlogsvlieger.
- **Buit:**
  - de Schelpenset en de Lampionset (lv 50-56);
  - 3 sieraden, 6 klassenwapens en 2 kristallen;
  - 6 benoemde baasitems met een effect;
  - 2 trofeeën en 2 huisdieren (Zeeslurfje en Vliegertje);
  - 5 stukken voor de nieuwe slots (wanten, cape, armband): die verschijnen pas na de merge met t-items.

**Let op:** de plekken komen uit POI_Z4 v2 van t-z4wereld. Die staat in deze branch nog NIET op de classpath, dus in een losse
t-z4wezens-client zijn het placeholders (dezelfde coördinaten). Na de integratie leest de code `Z4WereldPlekken` zelf (via reflectie).
- `/guhs vadsrijk z4wezens spawners` zegt welke van de twee het is;
- `/guhs vadsrijk z4wezens herlaad` leest de plekken opnieuw.

### 1. Alle wezens op een rij (sneller dan zoeken)
```
/guhs vadsrijk z4wezens alle
```
Er komt een zandvloer voor je met alle 10 wezens op een rij (lv 52, ze staan stil).

**Kijk naar:**
- ze zijn grijs, met een boos guh-gezichtje;
- de Lampionkwal heeft een zwak lampje.

**Sla er een paar:** je ziet een roze plof, hartjes en belletjes. Het wezen wordt fel en blij (blosjes), wordt half doorzichtig en
huppelt nog ~10 s. De Vliegerknecht verdwijnt in rook.

**De wezens, met wat ze doen:**

| wezen | waar | trucje (wat je ziet) |
|---|---|---|
| Grauwe Mopperkrab | Koraalbaai, rotsjes | Houdt hij zijn scharen voor zijn gezicht? Dan doen klappen van voren de helft ("Scharen dicht!"). Loop om hem heen. |
| Koraalkobold | Koraalbaai | Houdt ~6 blokken afstand en gooit roze koraalscherfjes. |
| Stekelpluis | Koraalbaai, rotsjes | Begint nooit zelf. Na een klap: een rode cirkel (1 s) en dan prikt hij. |
| Zeurslak | Koraalbaai, Bloemeneiland | Laat een slijmspoor achter (Plakkerig). Onder de helft kruipt hij 2,5 s in zijn huisje ("Verstopt!", geen schade). |
| Grauwe Lampionkwal | Koraalbaai, Mysterie-eiland | Zweeft. Een rode cirkel onder hem (1 s), dan prikken de tentakels + Plakkerig. |
| Mokmeeuw | Vliegereiland, Bloemeneiland | Cirkelt boven je, dan een rode streep (0,9 s) en een duikvlucht. Daarna zit hij 2 s op de grond: je kans. |
| Bromvlieger | Vliegereiland, het Wrak | Een paarse brede streep (0,9 s) = een windstoot die je wegblaast. Komt hij laag, dan zwiept zijn staart. |
| Pruilrob | rotsjes | Trommelt met zijn flippers (rode streep, 1 s) en glijdt dan op zijn buik langs de streep. |
| Mistmopje | Mysterie-eiland | Smelt weg in de mist (niet te raken) en duikt achter je op: een rode cirkel en dan 3 s donker. |
| Vliegerknecht (Mika) | het Wrak | Een rode streep = zijn enterhaak: Plakkerig en een rukje naar hem toe. |

**Losse commando's:**
```
/guhs vadsrijk z4wezens spawn mopperkrab
/guhs vadsrijk z4wezens spawn mokmeeuw 54 true
/guhs vadsrijk z4wezens opruimen
```
- De tweede maakt een **elite** (25 % groter, met een gouden gloed).
- `opruimen` haalt alles binnen 64 blokken weg.

### 2. De eilanden met wezens (spawners)
```
/guhs vadsrijk z4wezens spawners
/guhs vadsrijk z4wezens tp koraalbaai
/guhs vadsrijk z4wezens tp vliegereiland
/guhs vadsrijk z4wezens tp bloemeneiland
/guhs vadsrijk z4wezens tp mysterie
/guhs vadsrijk z4wezens tp rotsjes
/guhs vadsrijk z4wezens tp wrak
```
**Wat je moet zien:**
- per plek 2-3 wezens, rustig verspreid;
- op het Bloemeneiland alleen aan de buitenkant (niet bij de bijen of de picknick);
- bij een kampvuur of in een dorp vergeten ze je en lopen ze terug;
- in het Wrak staan Vliegerknechten in het ruim, in de kombuis, bij de knuffelkooi (onderdeks) en op het dek.

### 3. De Grauwe Zeeslurf (wereldbaas, lv 54)
```
/guhs vadsrijk z4bazen spawn zeeslurf
```
Je staat bij de duiksteiger. De Zeeslurf zwemt in de lagune (het gevecht begint op het vlot).

**Fase 1:**
- **Slurfspuit:** een rode streep vanaf zijn slurf, dan een straal water (duwt je weg).
- **Mokwolk:** 2-3 rode cirkels onder spelers, daarna 3 s donker.

**Fase 2 (66 %), "Het grotje":**
- Hij zakt naar zijn grotje en is **onkwetsbaar** (melding).
- Duik naar de **3 grauwe poliepen** op de bodem en sla ze kapot.
- **Roze ringen** op de bodem geven lucht (en 3 % Vads).
- Elke 6 s trekt een **paarse stroming** zwemmers naar het grotje.
- Zijn alle poliepen kapot, dan komt hij boven: 5 s suf, en je doet +25 % schade.

**Fase 3 (33 %):**
- **Golfslag:** een rode ring van 9 om hem heen. Zwem weg, of duik eronderdoor.
- **Samen aaien:** een gele cirkel. Ga er samen in staan (solo: alleen jij). Dan moet hij giechelen: 4 s suf en +25 %.

**Het einde:** hij wordt **roze**, zingt (muzieknootjes) en blijft 10 s blij drijven. Daarna komt de buitkist.
- Bij de eerste keer: een Legendarisch item + de trofee;
- en het huisdier **Zeeslurfje**.

**Handig bij testen:**
```
/guhs vadsrijk z4bazen fase 2
/guhs vadsrijk z4bazen poliepen
/guhs vadsrijk z4bazen fase 3
/guhs vadsrijk z4bazen reset zeeslurf
```
- `fase 2` zet hem op 66 %.
- `poliepen` slaat alle poliepen in één keer kapot.
- `fase 3` zet hem op 33 %.
- `reset zeeslurf` zet hem terug met volle Vads.

**Geen lagune in je testwereld?**
```
/guhs vadsrijk z4bazen bouw
```
Dit bouwt een testlagune (zand, water tot y 62, een vlot en een grotje) en een noodek voor het Wrak. Terugzetten met
`/guhs vadsrijk z4bazen herstel`. **Alleen in een testwereld gebruiken.**

### 4. Het Wrak van de Vliegervanger (kleine kerker)
```
/guhs vadsrijk z4bazen tp wrak
/guhs vadsrijk z4bazen spawn vliegervanger
```
- **De route:** het dek op, door het luik naar beneden (het ruim, de kombuis, de knuffelkooi, met Vliegerknechten), en dan naar het
  voordek: daar staat **Kootje de Vliegervanger** (lv 56).
- **Fase 1:**
  - **Knuffelnet:** een rode streep, dan een net (Plakkerig).
  - **Kaasbom:** 2 rode cirkels.
  - Op 80 % fluit hij 2 Vliegerknechten erbij.
- **Fase 2 (60 %), "Opstijgen!":** hij hangt onder zijn **oorlogsvlieger**, 9 blokken boven het dek, en is **onkwetsbaar**.
  - Op het dek staan **3 lieren** met een paars vlaggetje: **rechtsklik elke lier 3 keer** ("Lier: 1/3").
  - Je ziet paarse touwtjes van de lieren naar de vlieger.
  - Ondertussen gooit hij knuffelzakken (rode cirkels) en komen er windvlagen (een paarse streep die je wegduwt).
  - Zijn alle 3 lieren binnen: "KRAK!", de vlieger stort neer. Hij ligt 6 s plat en je doet +50 % schade.
- **Fase 3 (25 %), "Knuffelregen":** er vallen gestolen knuffels uit zijn zak, met een gele cirkel. Vang ze SAMEN op
  ("Knuffel gered! 1/3").
- **Het einde:** hij vlucht in een rookwolk ("Dit komt in mijn rapport!"). Daarna komt de buitkist, het huisdier **Vliegertje** en een
  verhaalmelding voor t-z4verhaal.

**Handig bij testen:**
```
/guhs vadsrijk z4bazen fase 2
/guhs vadsrijk z4bazen lieren
/guhs vadsrijk z4bazen fase 3
/guhs vadsrijk z4bazen reset vliegervanger
```
`lieren` draait alle 3 lieren in één keer binnen.

### 5. De buit
```
/guhs vadsrijk z4wezens set schelpen
/guhs vadsrijk z4wezens set lampion
/guhs vadsrijk z4wezens sieraden
/guhs vadsrijk z4wezens wapens
/guhs vadsrijk z4wezens kristallen
/guhs vadsrijk z4wezens loot mokmeeuw 54 true
/guhs vadsrijk z4bazen items
/guhs vadsrijk z4bazen loot zeeslurf
```
- **Schelpenset** (Kracht/Behendigheid/Verdediging):
  - (2) +12 Verdediging;
  - (4) **Schade tegen bazen +8%**.
- **Lampionset** (Wijsheid):
  - (2) +8 Wijsheid;
  - (4) +10 Heal-kracht, en je geeft zelf licht in het donker.
- **Kijk bij de sets naar:** de tooltip toont "Set: Schelpenset (n/4 gedragen)" en de bonussen worden groen.
- **Sieraden:** Parelring, Schelpenketting, Lampionoorbel.
- **Wapens:**
  - Ankerknots en Golfbreker (Ridder);
  - Vliegerboog en Harpoenkruisboog (Jager);
  - Lampionstaf en Koraalstaf (Magier).
- **Kristallen:** Parelkristal (+Vads) en Zeeglas (+Elementweerstand). Elk wezen in zone 4 laat ze soms vallen.
- **Baasitems (met een effect):**
  - Slurfstaf: +15 % schade op wezens in het water;
  - Zeekoeienhuid: in het water 15 % minder schade;
  - Lagunelampion (Guhddelijk, 2 %): je geeft licht in het donker;
  - Oorlogsvliegerboog: +20 % op vliegende wezens;
  - Kootjes Enterhaak: +15 % op Plakkerige wezens;
  - Kaasdoek: onder 30 % Vads 3 s sneller lopen.
- **Na de merge met t-items** komen er 5 stukken voor de nieuwe slots bij:
  - Schelpenwanten (telt mee voor de Schelpenset);
  - Lampioncape (telt mee voor de Lampionset);
  - Koraalwantjes, Vliegercape en Zeeglasarmband.

### 6. Wat nog NIET kan / let op
- Het uiterlijk is niet in een client bekeken: **beoordeel de modellen zelf** (schattig genoeg?).
- De VFX-kit van t-vfx (cast-balk, wind-up) werkt via reflectie en pas na de merge. Zonder de merge zie je de gewone gekleurde stofcirkels.
- Het Schildpadeiland heeft geen wezens; de schildpad zelf is van t-z4wereld.

## Zone 4: Ouwe Dobber (de reuzenschildpad) en de guh-vliegers (T-Z4DIEREN)

### Ouwe Dobber, de reuzenschildpad
Een grote, lieve schildpad (schild van 14 blokken, guh-oortjes, bloemetjes op zijn rug) die langzaam zijn rondje zwemt
langs het Schildpadeiland. Bij de steiger van het eiland wacht hij steeds 30 seconden, daarna zwemt hij een rondje van
ongeveer 2,5 minuut. Het eiland zelf (met het hutje en Schelpie) blijft gewoon liggen (dat is Oma Dobber, die slaapt).
Schelpie, de grote knuffel en de zeewier-quest blijven op het eiland; Ouwe Dobber komt elk rondje 30 s naast de steiger liggen.

| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z4dieren schildpad tp` | Je staat op het eind van de steiger van het Schildpadeiland en kijkt naar zijn halte. |
| `/guhs vadsrijk z4dieren schildpad halte` | Ouwe Dobber springt naar de halte en wacht daar 30 s (handig om op te stappen). |
| `/guhs vadsrijk z4dieren schildpad rijd` | Je wordt meteen op zijn schild gezet, waar hij nu ook zwemt. |
| `/guhs vadsrijk z4dieren schildpad route` | 20 s lang zie je zijn hele route als lichtpuntjes (groen = de halte), plus info in de chat. |

Wat je moet zien / proberen:
- `schildpad tp` en dan `schildpad halte`: hij ligt naast de steiger. Klik op hem: "Ouwe Dobber neemt je mee op zijn rondje!".
  Je zit op zijn schild. Na 30 s zwemt hij weg ("dobber, dobber"); de vinnen peddelen en de kop knikt.
- Met z'n tweeen/meer: er zijn 8 plekjes op zijn schild. De 9e krijgt "Ouwe Dobber zit vol".
- Sluipen op zee: je plonst naast hem in het water. Klik weer op hem om op te stappen.
- Sluipen bij de steiger: je staat netjes op de steiger.
- Duik onder water bij hem tot je lucht bijna op is: hij vist je eruit en zet je op zijn rug.
- Uitloggen op zijn rug en weer inloggen: je staat op de steiger.
- Het ritje moet soepel zijn (geen schokken), ook met meer spelers. Hij zwemt nergens door het eiland of de steiger heen.

### De guh-vliegers
Echte bewegende vliegers met oortjes en een gezichtje, aan een touwtje naar een paaltje: 14 op het Vliegereiland, de grote
wedstrijdvlieger aan de wedstrijdpaal, en 3 kleintjes op het dak van de vliegerwinkel in Lampionhaven.

| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z4dieren vliegers tp` | Naar het Vliegereiland. |
| `/guhs vadsrijk z4dieren vliegers uit` | Alle vliegers weg (om te vergelijken / als het te druk is). |
| `/guhs vadsrijk z4dieren vliegers aan` | Ze komen terug (binnen een seconde). |
| `/guhs vadsrijk z4wereld plek gebouw.vliegerwinkel.deur` | De vliegerwinkel in Lampionhaven: kijk omhoog naar het dak. |

Wat je moet zien:
- De vliegers dobberen, zwaaien en kantelen een beetje in de wind, elk op een eigen ritme. Ze kijken naar het paaltje.
- Dichtbij: een staart met gekleurde strikjes die wappert, en een touw dat een beetje doorhangt.
- Ver weg (meer dan 64 blokken): geen staart meer, wel nog de vlieger en het touw (dat scheelt werk voor je pc).
- Geen wol-vliegers van blokken meer op de kaart.

# B5. ZONE 5: HOOGVADS (lv 56-60)

## Nieuw in ronde U: zone 5 (Hoogvads op Moeder Wolkwal)
- **Gebouwen met diepte (u-z5):**
  - de kathedraal is gotisch: steunberen, luchtbogen, pinakels, roosvenster, diepe portalen en ribgewelven;
  - de universiteit kreeg hoge gotische ramen en een bibliotheekgalerij;
  - het paleis, de raadszaal, de academie en het station kregen reliëf;
  - de hele stad kreeg vensterkappen.
- **Interieurs (u-z5binnen):**
  - echte ronde kroonluchters (een nieuw 3D-blok), met kaarsen en kristallen;
  - lichtere zalen met dambordvloeren, wandluchters, kandelaars en licht in de gewelfribben.
- **Figuranten** zien er weer uit als normale guhs.
- **De staartvin:** het pad vanaf de staartlift loopt nu door tot de Donderbuik-arena, de Staartvlakte en Mistmoeder (die 4 plekken
  waren onbereikbaar).
```
/guhs vadsrijk z5wereld plek kathedraal_schip
/guhs vadsrijk z5wereld plek universiteit_bibliotheek_galerij
/guhs vadsrijk z5wereld plek troonzaal_audientie
/guhs vadsrijk z5wereld plek z5w_lift_staart_onder
/guhs vadsrijk z5wereld plek z5w_donderbuik_arena
```
- **De Wolkengrotten** (lv 58-60) in de kop van de walvis. De ingang is Het Blaasgat voor de stadspoort. Binnen: Het Wolkenslot
  (symboolpuzzel), de Grauwe Wolkduivel (elite) en een schat.
```
/guhs vadsrijk grot tp wolkengrot
```
- **Ambachten T5 + cap 60:** alleen bij de leermeesters van het Ambachtsgilde. Hoogvads is de ambachtshoofdstad.
- **Knuffelbond oprichten:**
  - bij Bondsklerk Zegeltje (paleis, kanselarij), na zone 4 of vanaf lv 56;
  - de Bondenpoort in het Staartpark brengt je naar de Bondenvallei.
```
/bond
```
- **Job-advancement 3 (lv 58):** bij Kapitein Veerkracht (Academie), na de finale.

### U-Z5: Hoogvads met diepte (kathedraal, universiteit, interieurs) + de figuranten

**Wat is er veranderd?** De gebouwen van Hoogvads zijn niet meer vlak:

- De **Ordekathedraal** en de **universiteit** zijn gotisch gemaakt, met diepte.
- **Paleis, Raadszaal, academie en station** hebben reliëf gekregen.
- **Elk raam in de stad** (ook van de huizen) heeft nu een druiplijst en een vensterbank.
- **Binnen** hangen grote kroonluchters aan kettingen.
- De **figuranten** (de guhs die in Hoogvads rondstaan) zien er weer uit als gewone guhs.

Vergelijk met de renders in `renders-hoogvads/ronde3/` (`voor_*` = oud, de rest = nieuw).

#### De kathedraal van buiten

```
/guhs vadsrijk z5wereld plek kathedraal_plein
```

Kijk naar de westgevel (de kant met de Grote Poort). Je ziet:

- **Steunberen** tegen de torens. Ze springen naar boven toe terug, met schuine kapjes, een nis met een guh-beeldje, een guh-waterspuwer en een pinakel bovenop.
- **Blinde zijportalen** onderin de twee torens: twee lagen bogen met colonnetjes en gouden deuren.
- **De koningsgalerij**: een rij nissen met guh-heiligen boven de zijportalen.
- **Diepe torenramen** met een omlijsting die naar buiten trapt, met stenen maaswerk.
- **Galmgaten** met een omlijsting.
- **Het guh-roosvenster** met twee ringen eromheen (wit en goud), plus hogels.
- **In de Grote Poort**: colonnetjes met beelden op consoles langs de zijkanten.
- **Bovenop de torens**: grote pinakels op de hoeken, een kroon van spitsjes rond de grote spitsen en gouden hogels langs de spitsen.

Loop daarna om de kathedraal heen (de zuidflank):

```
/execute in guhs:vadsrijk run tp @s 2647 226 -553
```

Daar zie je:

- **Zijbeukramen** met een terugspringende omlijsting en maaswerk, met erboven een wimperg (een puntgeveltje met een pinakel).
- **Een open borstwering** op de dakrand.
- **Steunberen** met een nis met een beeld en een waterspuwer.
- **Twee rijen luchtbogen** naar het schip.
- **Pilasters** tussen de hoge ramen.

Aan de transeptgevels zie je:

- **Een diep portaal** met drie lagen.
- **Een roosvenster** met een ring eromheen.
- **Twee traptorentjes** met slanke spitsen.

Op het dak zie je **dakkapellen**, een gouden **nokkam** en **vier spitsjes** rond de koepel.

#### De kathedraal van binnen

```
/guhs vadsrijk z5wereld plek kathedraal_schip
```

Kijk omhoog. Je ziet:

- **Kruisribgewelven** met gouden ribben.
- **Bundelpijlers**: slanke witte diensten die van de vloer tot het gewelf lopen, met gouden kapitelen.
- **Een triforium**: een rij kleine boogjes boven de arcade.
- **Grote kroonluchters** in het schip: drie ringen met kaarsen en lantaarns, aan lange kettingen.

```
/guhs vadsrijk z5wereld plek kathedraal_altaar
```

De kruising is nu **open tot in de gouden koepel**. Er hangt een **reuzenkroonluchter** (vier ringen) aan de koepel, en bij het altaar staan kandelaars. In de zijbeuken hangen kleine kroonluchters.

**Loop even rond:** de banken, het middenpad, het altaar, de torentrap en de klokkenvloer werken nog.

```
/guhs vadsrijk z5wereld plek kathedraal_geveltoren
```

#### De universiteit

```
/guhs vadsrijk z5wereld plek universiteit_poort
```

Je ziet:

- **Een diep portaal** voor de poort, met pinakels. De trap naar de straat loopt erdoor.
- **Twee hangende hoektorentjes** met spitsjes aan het poortgebouw.

Loop rond de gevels. Je ziet:

- **Hoge gotische ramen** van twee verdiepingen, met goud, maaswerk en een omlijsting.
- **Getrapte steunberen** met pinakels op de dakrand.
- **Puntgeveltjes (dakkapellen)** met een rond raampje op de dakrand.
- **Lijsten** op de verdiepingsbanden.
- **Gouden hogels** op de torenspitsen.

```
/guhs vadsrijk z5wereld plek universiteit_bibliotheek
/guhs vadsrijk z5wereld plek universiteit_bibliotheek_galerij
```

In de bibliotheek zie je:

- **Een galerij langs beide boekenwanden**, met een reling en lantaarns. Je komt er met de **ladder** aan de westkant.
- **Kroonluchters** in plaats van de kale lampjes.

#### Paleis, troonzaal en Raadszaal

```
/guhs vadsrijk z5wereld plek paleis_hof
/guhs vadsrijk z5wereld plek troonzaal_audientie
/guhs vadsrijk z5wereld plek raadszaal_zaal
```

- **Paleisgevel:** pinakels op de balustrade, guh-waterspuwers, lijsten onder de banden, en hogels op de vier ronde torens.
- **Troonzaal:** drie grote kroonluchters.
- **Raadszaal:** kroonluchters, wandpilasters met gouden kapitelen en een gouden cassettenplafond.

#### Academie, station en huizen

```
/guhs vadsrijk z5wereld plek academie_oefenplein
/guhs vadsrijk z5wereld plek luchthaven_station
/guhs vadsrijk z5wereld plek zachtmoedlaan
```

- **Academie:** de muren en de donjon hebben een uitkragende borstwering op consoles (mezekouwen), en pinakels op de donjon.
- **Station:**
  - Tussen de bogen staan hangende steunberen met pinakels. Ze beginnen pas boven je hoofd, dus je loopt er gewoon langs.
  - De roosvensters op de kopse kanten hebben een ring.
- **Huizen:** elk raam heeft een druiplijst erboven en, als het hoger zit, een vensterbank eronder.

#### Figuranten (de bug)

Voorheen leken de figuranten op een stapel kleren: alle zadels, harnassen en kledingstukken van het guh-model tegelijk. Nu horen ze eruit te zien als normale guhs, zoals Kadet Dons.

```
/guhs vadsrijk z5wereld plek markt_fontein
/guhs vadsrijk z5wereld bewoners
```

Kijk naar de marktgangers, de vluchtelingen bij de Soepkeuken en de kerkgangers in de banken:

- **Gewone guhs** in hun vachtkleur: geen zadel, harnas of hoedjes, en alleen de eigen vachtextra's (pluisguh, wolkguh, bloesemguh).
- **De bakker, burgemeester, kapitein, oma, luchtnon en professor** staan rechtop als hun eigen NPC-model.
- **Konijntjes** zijn echte konijntjes.
- **De ridders** zijn Wolkwacht-ridders.

**Het zaakje vernieuwen** (nieuwe figuranten laten spawnen):

```
/guhs vadsrijk z5wereld bewoners uit
/guhs vadsrijk z5wereld bewoners aan
```
### U-Z5BINNEN: ronde kroonluchters en lichtere zalen in Hoogvads

**Wat is er veranderd?**
- De kroonluchters zijn geen houten roosters meer. Het zijn nu **ronde hangende kroonluchters**: gouden ringen met kaarsjes, kristallen druppels en kleine ronde kroontjes, aan kettingen van het plafond.
- Alle grote zalen zijn **veel lichter**:
  - marmeren dambordvloeren met gouden rozetten;
  - wandluchters (gouden wandkandelaars);
  - staande gouden kandelaars;
  - gloeiend goud in de gewelfribben.
- In de kathedraal liggen **gekleurde lichtvlekken** op de vloer onder de glas-in-loodramen.

Vergelijk met de renders `renders-hoogvads/ronde3/binnen2_voor_*` (oud) en `binnen2_*` (nieuw).

Bekijk alles in de **dev client, op een nacht**. Dan zie je het licht van de lampen het best:
```
/time set midnight
```

#### De kathedraal
```
/guhs vadsrijk z5wereld plek kathedraal_schip
```
- **Boven het schip** hangen grote ronde kroonluchters. Ze hebben een ring van kaarsjes met kristallen druppels, kleine ronde kroontjes op de ring en hangers eronder, en ze hangen aan kettingen.
  - Lijkt er nog iets op een houten rooster of een steiger? Dat is fout.
- **Op elke pijler** zie je twee gouden wandluchters (op ooghoogte en hoger) en een vaandel.
- **Langs het middenpad** staan gouden kandelaars. De banken hebben gouden uiteinden.
- **De vloer** is wit en roze marmer in een dambordpatroon.
- **In de zijbeuken** liggen onder de ramen gekleurde tapijtvlekken in de vorm van een spitsboog (de "lichtvlekken").
- **Kijk omhoog:** sommige gouden ribben in de gewelven gloeien.
- **Het is nergens echt donker meer**, ook niet tussen de banken.

```
/guhs vadsrijk z5wereld plek kathedraal_altaar
```
- **Boven de kruising** hangt de reuzenkroonluchter. Hij heeft drie ringen met acht kroontjes en een gordijn van kristallen hangers.
- **Op de vloer** ligt een grote gouden rozet.
- **Het gouden guh-beeld** in de apsis is goed verlicht:
  - om de sokkel staan kandelaars;
  - achter het hoofd zit een stralenkrans van gloeiend goud.

#### De bibliotheek
```
/guhs vadsrijk z5wereld plek universiteit_bibliotheek
```
- De vloer is van marmer.
- De bureaus en de galerijen zijn van licht hout (bleek eiken).
- In de boekenwanden loopt een gouden band.
- Onder en boven de galerijen hangen wandluchters.
- In het plafond zit gloeiend goud, en er hangen kleine ronde kroonluchters.

#### De Raadszaal
```
/guhs vadsrijk z5wereld plek raadszaal_zaal
```
- Het **gouden rooster onder het plafond is weg**. Er zit nu gloeiend goud vlak in het plafond; er hangt geen rooster meer.
- Er hangen 3 ronde kroonluchters.
- Tussen de pilasters zie je wandluchters en vaandels.
- Bij de lessenaar staan 4 kandelaars.
- De vloer is een marmeren dambord met een rozet.

#### De troonzaal en de kanselarij
```
/guhs vadsrijk z5wereld plek troonzaal_audientie
```
- De vloer is een marmeren dambord met een rozet.
- Er hangen ronde kroonluchters.
- Aan de zuilen hangen wandluchters en tussen de zuilen staan kandelaars.
- De koningin op de troon is goed verlicht.

```
/guhs vadsrijk z5wereld plek paleis_kanselarij
```
- Er hangen twee kleine ronde kroonluchters en aan de muren hangen wandluchters.
- De vloer is van marmer.

#### De academiehal
```
/guhs vadsrijk z5wereld plek academie_kapitein
```
- Er hangen 2 ronde kroonluchters en aan de muren hangen wandluchters.
- Bij de kapiteinstafel staan kandelaars.
- De vloer is van marmer met een gouden rozet.

#### Loopcheck (survival)
- Loop door alle zalen. Je mag **nergens blijven haken**:
  - kandelaars hebben een dun paaltje;
  - kroonluchters, wandluchters en hangers hebben geen botsing.
- De tapijtvlekken zijn gewoon tapijt.
- Alle deuren, trappen en plekken zijn nog vrij.

#### De nieuwe blokken zelf
In de creative-tab **"Guhs: Bouwen"** staan:
- Kroonluchter;
- Gouden kaarsenrand;
- Kristallen hanger;
- Gouden wandluchter;
- Gouden kandelaar;
- Gouden kroonlijstmuurtje;
- Gouden lichtlijst.

Of geef ze jezelf:
```
/give @s guhs:kroonluchter 4
/give @s guhs:gouden_kaarsenrand 32
/give @s guhs:gouden_wandluchter 8
/give @s guhs:gouden_kandelaar 4
/give @s guhs:kristallen_hanger 8
```
Probeer dit:
1. Hang een kroonluchter aan het plafond. Dat is een rond 3D-model van ongeveer 3 blokken breed.
2. Leg een rij kaarsenranden naast elkaar. Ze verbinden zich met elkaar en met een kroonluchter.
3. Breek er één weg. De buren laten dan los.
4. Zet een wandluchter tegen een muur. Hij steekt van de muur af.
5. De kandelaar is 2 blokken hoog.
6. Alles geeft licht.
### U-Z5TUIN: tuinen in Hoogvads en meer licht in alle gebouwen

**Welke renders zijn oud en welke nieuw?**
- `renders-hoogvads/ronde3/voor_*` = de OUDE stand.
- `binnen2_*` = de NIEUWE interieurs (de vorige slice); `binnen2_voor_*` is daarvan de oude stand.
- Deze ronde: `tuin_voor_*` = oud, `tuin_*` = nieuw (paleistuin, een woning, de herberg).

**Wat is er veranderd?**
- Het roze/groene lappendeken-gras is overal in de stad weg: één rustig groen gazon.
- Elk groter grasvlak is nu een **ontworpen tuin**:
  - paden met guh-lantaarnpalen, roze guh-bankjes en prullenbakjes;
  - moestuinvakken (tarwe, wortels, bieten, aardappels) met watergeulen;
  - pompoenen en meloenen, zoetbessen;
  - bloemenperken met een vogelhuisje, rozentuinen;
  - vijvers met waterlelies;
  - een fontein met een gouden kandelaar in de grote tuinen;
  - lage azaleahaagjes en pergola's over de lange paden.
- Smalle voortuintjes hebben een bloemenrij langs de muur.
- **Binnen** hangen in elk huis, elke winkel, de herberg, de bank, het station, de academie, de universiteit, de gildewerkplaatsen en de soepkeuken nu gouden wandluchters en kleine ronde kroonluchters.

Test in de dev client. Overdag zie je de tuinen het best, 's nachts het licht:
```
/time set noon
```

#### De paleistuin (naast de Raadszaal)
```
/guhs vadsrijk z5wereld plek paleis_hof
```
- Kijk naar het plateau naast de Raadszaal (de kant van de kanselarij).
- Er is **geen ruis meer**: het gras is overal hetzelfde groen.
- Je ziet:
  - paden (lichte aarde) in een raster;
  - een fontein met een zuil en een gouden kandelaar;
  - moestuinvakken met watergeultjes;
  - pompoenen en meloenen;
  - bessen achter een azaleahaagje;
  - een bloemenperk met een vogelhuisje;
  - rozen;
  - lantaarnpalen, bankjes en prullenbakjes langs de paden.
- **Loop over alle paden:** niets mag je de weg versperren, en geen bankje of paal mag op een pad staan.
- **Spring in de fontein en weer eruit:** dat moet in één sprong lukken.
- Het plateau aan de andere kant van de Raadszaal (de noordkant) is ook een tuin.

#### Andere tuinen
```
/guhs vadsrijk z5wereld plek kathedraal_roos_zuid
```
- Naast de transeptgevel van de kathedraal liggen ook tuinen. Loop er even doorheen.
```
/guhs vadsrijk z5wereld plek ooggaarde_tuin
/guhs vadsrijk z5wereld plek staartpark_prieel
```
- De parken: een vijver, perken, pergola's. Loop over een paar paden, onder een pergola door en langs een vijver.
- Nergens mag je vast komen te zitten. Water is hooguit 1 diep.

#### Meer licht binnen (doe dit 's nachts)
```
/time set midnight
/guhs vadsrijk z5wereld plek huis_woning_1
```
- In de woning hangen gouden wandluchters aan de muren, naast de lamp die er al was. Het is nergens echt donker meer.
- Niets hangt in de weg: je loopt nergens met je hoofd tegen een lamp, ook niet bij de deur.
```
/guhs vadsrijk z5wereld plek huis_herberg_hemelbed
/guhs vadsrijk z5wereld plek herberg_hemelbed_boven
```
- In de herberg zie je beneden en boven wandluchters en (waar het plafond hoog genoeg is) een klein rond kroonluchtertje.
- **Loop de trap op:** daar mag geen lamp in de weg hangen.
```
/guhs vadsrijk z5wereld plek bank_hoogvads_balie
/guhs vadsrijk z5wereld plek luchthaven_station
/guhs vadsrijk z5wereld plek academie_kazerne
/guhs vadsrijk z5wereld plek universiteit_collegezaal
/guhs vadsrijk z5wereld plek gilde_smidse
/guhs vadsrijk z5wereld plek soepkeuken_tafel
```
- Overal is het lichter: wandluchters langs de muren en kroonluchtertjes in grote ruimtes.
- Lampen staan nooit boven een trap, deur of bed.

## Uit de nachtronde: zone 5

## Zone 5 "Hoogvads" op Moeder Wolkwal (T-Z5WERELD)

De hoofdstad ligt op de rug van de walvis, BOVEN het oosten van zone 4 (kop naar het westen, boven de Opstijgpier).
Alle teleports: `/guhs vadsrijk z5wereld plek <id>` (tab vult de ids aan; alle ids staan in `POI_Z5.md`).
Het kost ~10 s voor de tegels om een wijk heen geladen zijn: even rondkijken voor je oordeelt.

### Aankomst en luchthaven
- `/guhs vadsrijk z5wereld plek luchthaven_station` - station met glazen tongewelf: banken, balie van de havenmeester (bel, logboek), kratten in de hoek met een open luikje (de losse plank).
- `/guhs vadsrijk z5wereld plek haven_z5_z4_vertrek` - eind van pier 2 (lijn D): 2 meermasten, poortjes, bel. Kijk terug: poorten door de stadsmuur op alle 3 pieren, 2 vaste luchtschepen naast pier 1 en 3, vertrekborden bij de pierkoppen.
- Wolkvangnet: `/guhs vadsrijk z5wereld vangnet` zet je net voorbij de pier in de lucht. Je valt, en een wolkje zet je terug (paarse melding "Een zacht wolkje vangt je op..."). Ook van een waterval of een vin.

### Ordekathedraal
- `/guhs vadsrijk z5wereld plek kathedraal_plein` - het plein met de westgevel: de Grote Poort is nu ~57 hoog (walvis 20 lager, kathedraal hoger). Loop de trap op en door de open gouden deur.
- `/guhs vadsrijk z5wereld plek kathedraal_schip` - binnen: banken, gouden ribben, kathedraalglas met gouden lood, kroonluchters.
- `/guhs vadsrijk z5wereld plek kathedraal_altaar` - de kruising met het altaar en de gouden koepel; de apsis met "de Eerste Knuffel".
- `/guhs vadsrijk z5wereld plek kathedraal_geveltoren_voet` - wenteltrap in de zuidelijke geveltoren tot de klokkenvloer (`kathedraal_geveltoren`): uitzicht door de galmgaten.

### Marktlaan en Grote Soepkeuken
- `/guhs vadsrijk z5wereld plek markt_fontein` - kraampjes (6 soorten), parasols, lampionslingers, de fontein met het guhbeeld.
- `/guhs vadsrijk z5wereld plek soepkeuken_ketel` - reuzenketel, lange tafels. Achterin het open luikje bij `z5w_buik_luik`: een ECHTE ladder omlaag naar de Buikgangen (t-z5wezens' kerker in de walvis).

### Paleisheuvel
- `/guhs vadsrijk z5wereld plek paleis_trap` - de monumentale trap omhoog (sierlijke keermuren).
- `/guhs vadsrijk z5wereld plek raadszaal_ingang` - de Raadszaal: gouden deuropening, lessenaar op een podium, banken, zijkamer met deur (`raadszaal_zijkamer`).
- `/guhs vadsrijk z5wereld plek paleis_poort` - via de poort naar de troonzaal (`troonzaal_audientie`): loper, zuilen, troon met baldakijn.
- `/guhs vadsrijk z5wereld plek paleis_kanselarij` - de Kanselarij (gang met deur vanuit de troonzaal): bureau, kasten, kaartentafel.

### Universiteit en Gildehuis
- `/guhs vadsrijk z5wereld plek universiteit_poort` - door de poort (trap omlaag) naar de binnenhof met kloostergang en fontein.
- `/guhs vadsrijk z5wereld plek universiteit_bibliotheek` - 2 verdiepingen hoge bibliotheek, leestafels.
- `/guhs vadsrijk z5wereld plek universiteit_archief` - archief + collegezaal in de westvleugel.
- `/guhs vadsrijk z5wereld plek gilde_hal` - het Gildehuis (oostvleugel): hal + keuken, brouwerij, smidse, atelier, bijenstal.
- `/guhs vadsrijk z5wereld plek universiteit_sterrenwacht` - deur in de noordvleugel, wenteltrap tot boven (`universiteit_sterrenwacht_top`).
- Kijk ook van buiten: steunberen tussen de ramen, vensterbanken, druiplijsten, bloembakken, 4 erkers (minder vlakke muren).

### Academie van de Wolkwacht
- `/guhs vadsrijk z5wereld plek academie_poort` - trap omlaag door de poort naar het oefenplein (`academie_oefenplein`), ridders in formatie.
- `/guhs vadsrijk z5wereld plek academie_kazerne` - de donjon: hal van de kapitein, bedden, kastjes.
- `/guhs vadsrijk z5wereld plek academie_toren_voet` - deur in de oosttoren, ladder omhoog naar een uitkijkbalkon (`academie_toren`).
- `/guhs vadsrijk z5wereld plek z5w_proefarena` - de Proefarena (zand, hekring met 4 openingen).

### Huizen, herberg, bank, winkels
- `/guhs vadsrijk z5wereld huis herberg_hemelbed`, `huis bank_hoogvads`, `huis winkel_smid` (en `winkel_kleding/bakker/wapens/kruiden/juwelen/kaarten/speelgoed`, `woning_1..`).
- Elk huis: echte deur, begane grond met houten vloer, lamp, meubels; trapje van de deur naar de straat. Bovenverdiepingen zijn dicht.

### Parken, rand en walvis
- `/guhs vadsrijk z5wereld plek staartpark_prieel` - vijver, brug, gouden prieel, bloesembomen.
- `/guhs vadsrijk z5wereld plek staartpark_westeinde` - het pad met hekjes over de staartsteel naar de staartvin: Donderbuik-arena (`z5w_donderbuik_arena`) en Staartvlakte (`z5w_staartvlakte_noord`) op wolkplatforms.
- `/guhs vadsrijk z5wereld plek z5w_lift_zuidvin_boven` - luikje binnen de muur: ladder door de flank naar de zuidvin (`z5w_zuidvin`, wolkplatform). Idem `noordvin`.
- `/guhs vadsrijk z5wereld plek ooggaarde_tuin` en `balkon_oog` - tuin aan de kop, balkon boven het oog met de oortjes.
- Van buiten bekijken: `/tp @s 2560 260 -760` (naast de kop: oog, oortje, vin), `/tp @s 3060 260 -520` (achter de staart). Huid met vlekken, wolkpokjes en wolkbolletjes, watervallen langs de flanken (met kettingrooster in de muur).

### Wijken, biomen, bewoners
- `/guhs vadsrijk z5wereld wijk` - zegt de wijk + kern-gebied waar je staat; F3 toont "Het Vadsrijk - Hoogvads - ..." (onder de walvis is het weer zone 4).
- `/guhs vadsrijk z5wereld bewoners` (aan/uit) - de guhs van de renders lopen niet, ze staan/zitten als decor als je dichtbij bent (max 160).

### Nieuwe blokken
- Creative-tab "Guhs: Bouwen", achteraan: roze marmer, wolkmarmer (+ gepolijst/trap/plaat/muurtje/zuil), guhgezicht, gouden kroonlijst, zuilkop, gouden dakpannen, kathedraalglas (4), walvishuid (4), Hoogvads-vaandel. Allemaal in de stad gebruikt.

### Bekend / niet gedaan
- Geen eigen Wolkwacht-ridder-model (ridders = guh met wachter-textuur), geen bewegende vinnen, geen panorama op de uitkijkpunten.
- De troon zelf is decor (de koningin staat erop via het verhaal).

## Spiekbrief zone 5 "Hoogvads": het verhaal (t-z5verhaal)

Hoogvads is de feestelijke hoofdstad op de rug van Moeder Wolkwal (lv 56-60). Het is er veilig. Het hoofdverhaal heet **"Verraad in de stad"**: een speurdersverhaal over een Mika-spion in de Hoge Raad. De walvis is de inzet.

> **Let op:** de stad zelf (de blokken) bouwt t-z5wereld later. Tot die tijd staan de NPC's op **voorlopige plekken** (ze zweven als er nog geen vloer is). Het verhaal en de dialogen kun je al helemaal spelen met de commando's hieronder.

Alle commando's beginnen met `/guhs vadsrijk z5verhaal`. Je moet in het Vadsrijk zijn voor `tp` en `vlieg`.

### 1. Klaarzetten
| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z5verhaal klaarzetten` | Zone 1-4 klaar (alle hoofdquests + vlaggen `zone1..4_klaar`), level 56 (zolang de levelgrens nog 50 is: level 50). **z5_hq01 "De Zwevende Stad"** start vanzelf. |
| `/guhs vadsrijk z5verhaal vlieg` | De vlucht naar Hoogvads. De eerste keer: de **aankomstfilm** (vanaf de Opstijgpier langs de kop van de walvis, haar oog gaat open en lacht, over de rand, de stad in het goud, landing op de Luchthaven). |
| `/guhs vadsrijk z5verhaal quest <1-10>` | Spring naar hoofdquest 1-10 (alles ervoor wordt afgevinkt, ook de sporen in je boekje). |
| `/guhs vadsrijk z5verhaal stap` | Vinkt de huidige stap van je zone-5-quest af (overslaan). |
| `/guhs vadsrijk z5verhaal tp <naam>` | Naar een NPC of plek. Tab geeft de lijst, bv. `koningin`, `inspecteur`, `havenmeester`, `zachtpels`, `mientje`, `spuitgat`, `ooggaarde_balkon`. |
| `/guhs vadsrijk z5verhaal vergeet` | Wist alles van zone 5 (quests, sporen, geheimen, gezien-films). |

### 2. Het hoofdverhaal (10 stappen)
| # | Quest | Wie / waar | Wat moet er gebeuren |
|---|---|---|---|
| 1 | De Zwevende Stad | Havenmeester Windvaan (Luchthaven) → Koningin Zachtmoed (troonzaal) | Aankomstfilm, meld je bij de havenmeester, loop de Zachtmoedlaan op, de trap op naar de troonzaal. Je krijgt een stukje kaas. |
| 2 | Verraad in de stad | Koningin → Inspecteur Snuffelaar (zijkamer Raadszaal) | De gescheurde bladzijde is gestolen. Je krijgt het **Speurdersboekje**. Lessenaar onderzoeken (spoor: grijze haartjes), **verhoor** Raadsvrouwe Inktvlek. |
| 3 | De mol in de Wolkwacht | Kapitein Veerkracht (Academie) | Kastjes van de kadetten (spoor: bevelbriefje met pootjesstempel), **verhoor** Kadet Plukje. |
| 4 | Soep en geruchten | Kok Pollepel (Grote Soepkeuken) | **Verhoor** de kok, deel 4 kommen soep uit (klik op de 4 vluchtelingen in de rij), **verhoor** Mager Mientje → de **herinnering** (flash F3: haar vlucht uit Grauwhoven). |
| 5 | Wie vertrekt er stiekem? | Havenmeester → nachtlogboek → Raadsheer Gouddraad (bank) | Spoor: het logboek ("Z.P." naar de Stormrand), **verhoor** Gouddraad (zegel van de Kanselarij). |
| 6 | De laatste twee verhoren | Generaal Donsveer (toren Academie) + Kanselier Zachtpels (Kanselarij) | **Verhoren** in willekeurige volgorde. Bij Zachtpels: de kaastest! |
| 7 | Het bewijsbord | Inspecteur Snuffelaar (of rechtsklik op je boekje) | **Beschuldig de verrader.** Fout = uitleg, geen straf. Goed = de **ontmaskering** in de Raadszaal (een rits in een guh-pak: Kootje Zilverstaart van de Ringvinger). |
| 8 | De Wolkwaldroom | Het middelste spuitgat | Achtervolging; de **finale**: Zilverstaart verslaan + de Grauwe Droom van Moeder Wolkwal opvrolijken. Daarna leest de Koningin de bladzijde voor: de legende van de Keizer en de Lege Schaal. |
| 9 | Het slaaplied | Balkon van de Ooggaarde | Zing het slaaplied: Moeder Wolkwal wordt wakker en knipoogt. Titelkaart **"Ridder van Hoogvads"**, vlag `zone5_klaar`. |
| 10 | De Stormrand | Kanselarij → oostpier → Havenmeester | De **Grauwe brief** "Aan de Ringvinger", en de **cliffhanger**: een bliksemmuur, de Grauwe Vloot, De Gouden Greep, tien ringen aan tien tenen. "Wat een mooi lampionnetje, die walvis. Die wil ik." |

**Waar je op let:** de tracker (rechtsboven + lichtstraal) wijst elke stap aan met wie/waar/wat. Na stap 7 is Kanselier Zachtpels voor jou een "kartonnen Kanselier" (grapje, omdat NPC's voor iedereen zichtbaar blijven).

### 3. Speurwerk los testen
| Commando | Wat je ziet |
|---|---|
| `/guhs vadsrijk z5verhaal verhoor <wie>` | Een verhoor met keuzes: `inktvlek`, `plukje`, `kok`, `mientje`, `gouddraad`, `donsveer`, `zachtpels`. Per verhoor levert één vraag het spoor op ("Nieuw spoor in je Speurdersboekje: ... (n/10)"). Je mag alle vragen stellen; daarna verschijnt "Dat was het. Dank u." |
| `/guhs vadsrijk z5verhaal scene bord` | Het **bewijsbord**: pagina 1 de sporen (✔ of ???), pagina 2 de verdachten (vrijgepleite krijgen "(vrijgepleit)"), pagina 3 als de 3 doorslaggevende sporen er zijn. |
| `/guhs vadsrijk z5verhaal bewijs` | Alle 10 sporen in je boekje (om het bord vol te zien). |
| `/guhs vadsrijk z5verhaal quest 7` en dan het bord | Nu staan er knoppen "Beschuldig ...". Probeer eerst een foute (uitleg met het bewijs dat hem vrijpleit), dan Zachtpels → ontmaskering. |

### 4. Losse scènes
`/guhs vadsrijk z5verhaal scene <naam>` met: `aankomst`, `herinnering` (F3), `ontmasker`, `spion` + `droom` (de finale zonder baasgevecht), `ontwaken`, `brief`, `cliffhanger`, `klaar` (titelkaart + vlag).

`/guhs vadsrijk z5verhaal finale` start de volgende fase van de finale (tijdens quest 8, na het spuitgat). Zonder de bazen van t-z5wezens speelt er een filmpje dat de fase zelf afrondt.

### 5. Zijquests (11)
| Quest | Bij wie | Wat |
|---|---|---|
| Een kom voor iedereen | Kok Pollepel | 8 wolkbessen (roze glinsters) in het Staartpark en de Ooggaarde |
| Pluisje's knuffel | Pluisje (soepkeuken) | Knuffelkonijn Flap achter het beeld van de Eerste Knuffel (kathedraal) |
| Brieven naar huis | Opa Grauwsnor | 3 brieven: havenmeester, Zuster Zachtzang, Pluisje |
| Het Oog meten | Professor Brilsma | De sterrenwacht van de universiteit |
| De adem van Moeder Wolkwal | Professor (na quest 9) | 3 spuitgaten |
| Proef: het Wolkenparcours | Kapitein Veerkracht | 4 palen op het oefenplein |
| Proef: de verdwaalde kadet | Kapitein Veerkracht | Kadet Dons van de Luchthaven naar de Academie begeleiden (hij loopt mee) |
| Marktdrukte | Kraamguh Lientje | Pakjes naar bakker, kleermaker, juwelier |
| Water voor het Staartpark | Tuinguh Stekje | 4 bloesembomen |
| De kroon van wol | Hofdame Breinaald | 3 bolletjes roze wol (markt, paleishof, universiteit) |
| Het lied van de Eerste Knuffel | Zuster Zachtzang (na quest 9) | 2 roosvensters + het beeld |

### 6. Stad, teasers en ambachten
- **4e klasse (alleen teasers, niet speelbaar):** Vadszanger Lieflied bij de kathedraaltrappen, Pluisninja Schaduwdons bij het oefenplein. Klik → "Wat staat er in de vitrine?" → de Guhfluit / de Pluisdolkjes, "Binnenkort." (`tp vadszanger`, `tp pluisninja`)
- **Ambachtsgilde** naast de universiteit: Gildemeester Duimdop + leermeesters koken, brouwen, smeden, naaien, imkeren. Nu nog een "binnenkort"-zinnetje; de ambachten-slice kan ze later koppelen. (`tp gildemeester`)
- **Winkels:** bakker (drankjes), kleermaker, smid, juwelier, Lientje (veilinghuis), stalmeester bij de Luchthaven.
- **Reizen:** Havenmeester Windvaan → reiskaart (Hoogvads staat er nu op, rechtsboven) en vluchten naar alle eilanden. Na zone 4 staat "Naar Hoogvads" ook bij de andere kapiteins.

### 7. Verkenningsboek (toets B), pagina "Hoogvads"
Hoofd- en zijquests, geheimen (3 schatten + de Grauwe brief), 8 knuffels, de bazen (Zilverstaart, de Grauwe Droom), 4 uitkijkpunten (Ooggaarde-balkon, Geveltoren, Staartvin, Sterrenwacht: panoramafilmpje), 4 kampvuren (Luchthaven, markt, Staartpark, Ooggaarde) en 5 gebieden. Titel bij 100%: **Kenner van Hoogvads**.

### 8. Wat (nog) niet kan
- De stad, de kampvuurblokken en de vitrines bestaan pas als t-z5wereld ze bouwt. De plekken zijn nu schattingen uit de concept-renders.
- De echte finale-gevechten zijn van t-z5wezens; tot dan speelt een filmpje.
- De herinnering van Mientje (F3) is een verteld filmpje, nog niet speelbaar (dat kan pas met een kaart van het keizerrijk).

## Zone 5 "Hoogvads": gevechten (T-Z5WEZENS)

De stad zelf is veilig. Je vecht op vier afgesloten plekken: de **Proefarena van de Wolkwacht** (bij de Academie), de **Buikgangen** (in de walvis, via een luik in de kelder van de Soepkeuken), de **Wolkrand** (de vinnen en de staartvin, via wolkenliften) en het geheime **Schuilnest** (onder de Luchthaven, achter een losse plank).

> Alle plekken zijn nog **placeholders** tot t-z5wereld de stad bouwt. Staat er nog geen stad/vloer? Typ eerst:
> `/guhs vadsrijk z5wezens bouw alles`. Dat bouwt de Buikgangen, het Schuilnest en witte stand-in-vloeren. Het zet alleen blokken in lucht, dus de stad wordt nooit overschreven.

### 0. Snel overzicht
- `/guhs vadsrijk z5wezens alle` zet alle 9 wezens op een rij (zonder AI, de vermomde ook zichtbaar).
  - Kijk naar: de bezeten stropop met grijs pluis, de ridder-guh met kussenzwaard en roze pluim, de Mika's (saboteur met moersleutel, lontlegger met kaasbommen, harpoenier), de grijze pluismijt, het wolkduiveltje met hoorntjes, de mistsluier en de Mika in Wolkwacht-kostuum.
- `/guhs vadsrijk z5wezens plekken`: alle plek-ids met coördinaten.
- `/guhs vadsrijk z5wezens spawners`: aantallen per plek (max ~50 levend in heel zone 5).
- `/guhs vadsrijk z5wezens opruimen`: haalt alle zone-5-wezens binnen 64 blokken weg.

### 1. De Proefarena (lv 56-57)
- `/guhs vadsrijk z5wezens tp proefarena`
  - **Bezeten Oefenpop**: staat stil als een gewone pop (geen naambordje). Loop binnen 5 blokken: "De oefenpop beweegt!".
    - Rode cirkel = hij draait rond: stap weg.
    - Verslagen: grijs pluis waait eruit.
  - Leider **Opperpop Strobaal** (elite, gouden gloed).
- **Sparren met een ridder-guh** (doet NOOIT pijn):
  - Typ `/guhs vadsrijk z5wezens sparren`, of rechtsklik een Sparringridder in de arena.
  - Rode strook = kussenzwaai: stap eruit. Drie "Kussenklap!" en je verliest de ronde. Je Vads blijven vol.
  - Roze glinster bij zijn schild = hij blokt: je klappen doen dan niets ("Geblokt!").
  - Sla zijn balk naar 5%: hij buigt, "Goed gevochten!". Je krijgt xp, een Proefpenning-teller, de eerste keer zeker een Wolkwacht-stuk, daarna 30% kans.
  - Klappen van iemand anders dan je sparpartner doen niets.

### 2. De Buikgangen + de kerker (lv 57-59)
- **Luik**: de kelder van de Soepkeuken heeft een luik (rechtsklik: "naar de Buikgangen"). Snel: `/guhs vadsrijk z5wezens tp buikgangen`.
- **De kamers**: de Ingangshal, dan de Ribbengang (botten-bogen), de Kleppenzaal (koperen pijpen) en een gang naar de ronde Ketelkamer.
  - Wezens: Mika-saboteur (rode cirkel = gassleutel, grijze wolk = plakkerig), Mika-lontlegger (blijft op afstand, kaasbom = rode cirkel), Grauwe Pluismijten (in groepjes).
  - Terug naar boven: het luik bij de ingang (rechtsklik).
- **Kerkerbaas Opzichter Gorgonzola** in de Ketelkamer: `/guhs vadsrijk z5wezens baas gorgonzola` (reset + naar de deur).
  - 4 warme-luchtkleppen (roze wiel = open, grijs = dicht).
  - Om de ~25 s paarse lijn = hij draait een klep dicht. Elke dichte klep: hij neemt 15% minder schade, en iedereen krijgt elke 3 s een beetje kou.
  - **Rechtsklik een dichte klep 3x** om hem open te draaien.
  - Verder: moersleutelzwaai (rode cirkel om hem), kaasgasbom (rode cirkel op iemand), buikschuiver (rode strook).
  - Op 60% fluit hij 2 saboteurs. Op 25% gaan ALLE kleppen dicht en zakt hij niet onder 15% tot er weer minstens drie open zijn.
  - Snel testen: `/guhs vadsrijk z5wezens mechaniek kleppen` (sluit alle kleppen van de dichtstbijzijnde Gorgonzola).

### 3. De Wolkrand (lv 58-60)
- **Wolkenliften** (witte geglazuurde blokken, rechtsklik) aan de stadsrand. Snel: `tp zuidvin`, `tp noordvin` of `tp staartvlakte`.
- Wezens:
  - Grauwe Wolkduivel: vliegt, en rode streep = duik.
  - Mistslier: grijze cirkel = mist (blind + plakkerig).
  - Mika-harpoenier: rode lijn = harpoen. Die trekt je NAAR hem toe, nooit naar de rand.
  - Leider **Moeder Mist** (grote mistcirkel).
- **Vangnet**: val je van een vin of de staart, dan vangt een wolkje je op ("Een wolkje vangt je op!") en sta je weer bij de lift.
- **Wereldbaas Donderbuik** (een dikke grauwe onweerswolk) op de staartvin: `/guhs vadsrijk z5wezens baas donderbuik`.
  - Bliksem: een rode cirkel onder iedereen.
  - Regenlaan: een brede rode strook (plakkerig).
  - **Zuigwind**: drie ROZE wolkenkussens verschijnen. Sta erop, anders zuigt hij je naar zijn buik (35%). Testen: `mechaniek zuig`.
  - 60%: 3 wolkduiveltjes + Donderslag (gele cirkel: sta er samen in).
  - Aan het eind wordt hij ROZE, giechelt en drijft weg, met roze regen. Huisdier: **Donderbuikje**.
  - Respawn 20 min.

### 4. Het Schuilnest (lv 58-60)
- Een losse plank achter de kratten op de Luchthaven (rechtsklik). Snel: `tp schuilnest`.
- **Vermomde Mika's** staan er als Wolkwachters (geen naambordje). Kom binnen 4 blokken: "Ontmaskerd!", het kostuum ploft eraf.
  - Rode cirkel achter je = daar verschijnt hij. Stap weg.
- Leider **Meester-Vermommer Feta**.

### 5. De spion: Kootje Zilverstaart (de finale van "Verraad in de stad")
- Het verhaal start hem (t-z5verhaal: `Z5Finale.start` → onze `Z5Bazen.start(speler, "spion")`). Testen: `/guhs vadsrijk z5wezens baas zilverstaart` (zet je bij de Wolkwaldroom en roept hem op).
- Fase 1: speldsprong (rode strook), rookbom (grijze cirkel: blind), daarna duikt hij op achter wie het verst staat.
- Fase 2 (70%), **De Vermommingen**: vijf Wolkwachters in een kring, één is hem.
  - Zoek de ZILVEREN glinster bij de staart.
  - Raak je een lokpop: "Een lokpop!" en dat kost jou 10%.
  - Raak je hem: ontmaskerd en +50% schade, 8 s lang.
  - Niet gevonden binnen 25 s: hij springt eruit (25% op iedereen).
  - Testen: `mechaniek vermom`.
- Fase 3 (35%), **De ontsnapping**: drie grauwgasventielen bij het spuitgat. Rechtsklik elk 3x. Tot ze alle drie dicht zijn zakt hij niet onder 10%. Testen: `mechaniek ontsnap`.
- Verslagen: hij vlucht in rook. Het verhaal gaat door naar de Grauwe Droom (die fase speelt de verhaal-scène van t-z5verhaal).

### 6. Buit (lv 56-60)
- `/guhs vadsrijk z5wezens set`: de **Wolkwachtset** + sieraden.
  - Met de nieuwe slots van t-items zijn het 6 stukken: helm, kuras, beenstukken, laarzen, handschoenen, cape.
  - Sieraden: Wolkring, Walvisketting, Regendruppeloorbel, Stormarmband.
  - Setbonussen in de tooltip:
    - (2) Verdediging +10 en Vads +60;
    - (4) Schade tegen bazen +8%;
    - (6) Wolkkussen: een schild onder 30% Vads.
- `/guhs vadsrijk z5wezens wapens`: de 6 klassewapens + de 7 benoemde baasstukken + 3 trofeeën.
  - Klassewapens: Wolkwachtzwaard, Walvisbotknots, Stormboog, Harpoenkruisboog, Wolkenstaf, Bliksemstaf.
  - Baasstukken: Hoedenspeld, Mantel der Vermommingen, Moersleutel, Kleppenstaf, Donderboog, Onweerstaf, en het Guhddelijke Regenjasje.
- `/guhs vadsrijk z5wezens loot <wezen|baas> [level] [elite]`: rolt een tabel (een baas = zijn kist; de eerste keer zeker een Legendarisch + trofee).
- **Huisdieren** (eerste kill): Donderbuikje (Donderbuik), Warmpje (Gorgonzola), Lokpopje (Zilverstaart).

## Zone 5: de restjes van Hoogvads (T-WOLKWAL)

Wolkwacht-ridder-model, bewegende vinnen en staart van Moeder Wolkwal, spuitgaten, twee panorama's en betreedbare bovenverdiepingen.
Alles onder `/guhs vadsrijk wolkwal ...` (de plekken zelf: `/guhs vadsrijk z5wereld plek <id>`).

### Wolkwacht-ridders (eigen model)
- `/guhs vadsrijk wolkwal ridders` - 6 ridders op een rij voor je neus, in alle 6 vachten (gewoon, sneeuw, goud, mint, choco, wolk). De laatste zit. Na 60 s weg.
  Kijk naar: een echte guh (oortjes, ogen, staartje bewegen) in wolkblauw/wit harnas met gouden randen. Helm met gouden rand, neusstuk en wangplaatjes, een wit-blauwe pluim die zacht wiebelt. Borstplaat met een gouden wolkje, gouden riem, wolkenkraag, kort cape'je, een klein wolkenschild op de linkerflank, een zwaard in de schede rechts, laarsjes met een gouden band.
- `/guhs vadsrijk z5wereld plek academie_oefenplein` - de ridders in formatie op het oefenplein zijn nu Wolkwacht-ridders (geen guh met wachter-textuur meer). Ook bij de poorten en in de Proefarena (`z5w_proefarena`).
- De Wolkwacht-NPC's van het verhaal (t-z5verhaal, `z5_wacht_1..6`: ridderhelm + wolkenkraag) krijgen in het spel hetzelfde model in hun eigen vacht. Werkt pas na de integratie met t-z5verhaal (in deze tak bestaan ze nog niet).
- De Sparringridder (t-z5wezens) kan hetzelfde uiterlijk krijgen: na de integratie `python tools/vadsrijk/wolkwal/maak_modellen.py --sparring-over` (kussenzwaard rechts, groot wolkenschild links, hun animaties blijven).

### Moeder Wolkwal zwemt
- `/guhs vadsrijk wolkwal kijk vin` - naast de zuidvin. Het puntje van de vin wuift langzaam op en neer (4 s), het uiterste stukje loopt iets achter. Wolkslierten waaien naar achteren (oost: zij zwemt naar het westen).
- `/guhs vadsrijk wolkwal kijk staart` - naast de staartvin: beide punten van de staart slaan zacht op en neer (5 s).
- `/guhs vadsrijk wolkwal kijk spuitgat` dan `/guhs vadsrijk wolkwal spuit` - beide spuitgaten op haar kop spuiten: wolkjes schieten omhoog langs de wolkenzuil, een pluim bovenin, druppeltjes, een zacht "pfffsh". Vanzelf elke 20 s.
- `/guhs vadsrijk wolkwal kijk kop` - voor haar neus (om alles in één keer te zien).
- `/guhs vadsrijk wolkwal beweging` - status (hoeveel bewegende punten er nu zijn, max 4). `... beweging uit` / `aan` om te vergelijken met de statische blokken.
- Let op: de bewegende puntjes zijn alleen beeld (geen botsing). De beloopbare wolkplatforms op de vinnen en de staart zijn niet veranderd. Ze verschijnen pas binnen ~190 blokken.

### Panorama's (eenmalig, vanzelf)
- `/guhs vadsrijk wolkwal uitzicht reset`, dan `/guhs vadsrijk z5wereld plek ooggaarde_tuin` - zodra je aankomt: een korte camerarit (zonder klikken) over haar kop, langs haar oog, voor haar neus langs en terug over de stad. Titel "De Ooggaarde".
- `/guhs vadsrijk z5wereld plek staartpark_prieel` - camerarit over de staart, langs de wuivende staartvin, achter haar met de hele stad en de kathedraal. Titel "Het Staartpark".
- Direct afspelen zonder te lopen: `/guhs vadsrijk wolkwal uitzicht ooggaarde` / `... uitzicht staartpark`.

### Bovenverdiepingen (betreedbaar)
- `/guhs vadsrijk z5wereld huis herberg_hemelbed` - binnen: achterin een houten trap omhoog (langs de zijmuur), boven een gastenkamer met bedden langs de voorgevel, een kist en een hanglamp. Hekje rond het trapgat.
- Of meteen naar boven: `/guhs vadsrijk wolkwal boven herberg_hemelbed`.
- Zelfde voor `bank_hoogvads` (boven: de kluis met goud en kisten, een lessenaar), `winkel_smid`, `winkel_bakker`, `winkel_wapens`, `winkel_kruiden` (boven: voorraad, het bed van de winkelier, een werkbank).
- Kijk naar: de trap begint achter de toonbank (via de opening aan de zijkant), elke trede sluit aan, de laatste trede ligt gelijk met de vloer boven, de ramen boven zijn nu echte ramen.
- Andere huizen (en de kledingwinkel: te klein voor een trap) hebben nog een dichte bovenverdieping.

## De Grauwe Droom (zone 5, finale fase DROOM) — T-DROOM

**Wat is het?** Na Kootje Zilverstaart (de spion) in De Wolkwaldroom zak je in de **droom van Moeder Wolkwal**. Je komt op
**de Droomwolk** (een ronde wolkvloer in de lucht, r 17, ten oosten van de staartvin). Daar zwemt **De Grauwe Droom** rondjes:
een grote grijze walvis van mist met traantjes (lv 60). Je verslaat hem niet, je **vrolijkt hem op**: aan het eind wordt hij roze
en speelt het echte slaapliedje op een fluit. Daarna word je **wakker bij het middelste spuitgat**.

### Snel testen (in Het Vadsrijk, op level 2)
```
/guhs vadsrijk droom start
```
Je ziet "Je zakt in een droom...", je staat op de wolk, na 1,5 s de intro (camera + titel "De Grauwe Droom").

| Commando | Wat er moet gebeuren |
|---|---|
| `/guhs vadsrijk droom start` | Droom + gevecht (als herinnering, geen quest nodig). Je party binnen 24 blokken gaat mee. |
| `/guhs vadsrijk droom info` | Plek van de wolk, levens, fase, neurie-meter, ranken, nacht, slaapliedjes goed/mis. |
| `/guhs vadsrijk droom wiegelied` | De 4 nootstenen op de rand lichten één voor één op (kleur + fluittoon). Daarna: rechtsklik ze **in die volgorde**. Goed = roze hartjes, kaart "Het slaapliedje klinkt weer goed!", de walvis staat 8 s stil en krijgt 10 s +50% schade. Een verkeerde noot = "Vals! Begin weer bij de eerste noot." (geen schade). Te laat (14-16 s) = "Nachtmerrie!": 30% bij iedereen, hij heelt 4%. |
| `/guhs vadsrijk droom ranken` | 3 grijze ranken groeien uit de wolk. Ze grijpen de dichtstbijzijnde speler (grijs lijntje, plakkerig, 4% per 2 s) en maken de droom 15% taaier per rank. Sla ze stuk: roze pluf, "Doorgeknipt!". |
| `/guhs vadsrijk droom omkeer` | De droom draait om: de wolkvloer wordt **donkergrijs/zwart** (omgekeerde kleuren), vier **roze wolkjes** (roze cirkels) drijven rond. Wie niet op een roze wolkje staat: 7% per 2 s. Na 20 s wordt de vloer weer wit. |
| `/guhs vadsrijk droom fase 2` | Zet hem op 66%: de volgende klap start fase 2 ("De droom draait om!" + omkeer, daarna het slaapliedje). |
| `/guhs vadsrijk droom fase 3` | Zet hem op 31%: de volgende klap start fase 3 "Samen neuriën!": een **gele cirkel** in het midden. Hij zakt niet onder 30% ("De droom is te zwaar: neurie eerst...") tot de neurie-meter (actiebalk) vol is: sta in de gele cirkel (solo ~15 s, met meer spelers sneller; muzieknootjes boven je hoofd). Vol = "Moeder Wolkwal neuriet mee!", hij wordt **lila** en krijgt +25% schade. |
| `/guhs vadsrijk droom neurie` | Neurie-meter meteen vol (lila). |
| `/guhs vadsrijk droom win` | Meteen opvrolijken: roze, hartjes, slaapliedje op de fluit, hij zweeft omhoog. Je kist staat op de wolk. Open je kist → je wordt wakker bij het spuitgat ("Je wordt wakker / Moeder Wolkwal slaapt weer zoet"). Zonder kist open: na 90 s. |
| `/guhs vadsrijk droom wakker` | Meteen wakker worden bij het middelste spuitgat. |
| `/guhs vadsrijk droom tp` | Alleen naar de wolk (geen gevecht). |
| `/guhs vadsrijk droom bouw` | De wolkvloer opnieuw leggen (alleen in lucht, sloopt nooit iets). |
| `/guhs vadsrijk droom reset` | Grauwe Droom weg (vloer wordt weer wit). |
| `/guhs vadsrijk droom loot [level]` | Een kistworp in je tas: 1e keer zeker een benoemd Legendarisch wapen + de trofee. |
| `/guhs vadsrijk droom verhaal` | Roept de echte verhaal-haak (`Z5Finale.start`) aan: doet alleen iets tijdens z5_hq08, na het spuitgat. |

### Via het verhaal
1. `z5_hq08` → naar het middelste spuitgat → Kootje Zilverstaart (t-z5wezens) → verslagen → **meteen de droom** (deze slice).
2. Na het opvrolijken telt het doel "De Grauwe Droom" voor je hele party en ga je terug naar de koningin.
3. Zonder deze slice speelde er een korte camerascène; die is nu vervangen door het echte gevecht.

### Opletten / kijken
- **Van de wolk vallen:** tijdens het gevecht vangt een droomwolkje je op ("Een droomwolkje vangt je op...") en sta je weer aan de zuidrand. Na het gevecht = van de wolk stappen is wakker worden.
- **Herinnering (opnieuw spelen, voor loot + huisdier):** na het verhaal **2 s hurken op het middelste spuitgat** → "Je ogen worden zwaar..." → je droomt het nog een keer.
- **Huisdier:** *Droomwalvisje* (klein roze wolkwalvisje) na de eerste keer opvrolijken.
- **Loot:** de zone-5 bazenkist (Wolkwachtset, sieraden, wapens) + **Wiegeliedstaf** (Magiër), **Zwaard van de Zoete Droom** (Ridder), **Slaapsterrenboog** (Jager), heel zeldzaam **Moeder Wolkwals Slaapmuts** (Guhddelijk, hoofd), trofee **Een Fijne Droom (in een potje)**.
- **Uiterlijk** (zelf beoordelen): grijze walvis met traantjes en wolkjes op zijn rug; lila met "o"-mondje na het neuriën; roze en blij aan het eind. Nootstenen: wolksteen met een gouden randje en een draaiende muzieknoot (roze, geel, lichtblauw, lila). Ranken: zachte grijze sliertjes met blaadjes.
- **Muziek:** hook-id `guhs:vr.z5droom.muziek.grauwe_droom` (nog geen geluid; t-geluid kan het vullen).

# B6. ZONE 6: HET ZIJDELAND (lv 60-70)

## Zone 6 in één oogopslag (nieuw in ronde U)
Het Zijdeland is het eerste stuk van het grote continent: lv 60-70, roze duinen, oases, karavanen, bazaars en zwevende koepels.
Het verhaal is **De Sterrenkaravaan**: Hertogin Ringeling (de Ringvinger) dooft de sterrenbakens; jij steekt ze weer aan, vindt de
Moederoase en beklimt de raid **De Sterrendovende Toren**. Het levelplafond is nu **70**.

**Snel rondkijken:**
```
/guhs vadsrijk level 65
/guhs vadsrijk z6wereld plek haven_z6_z5_vertrek
/guhs vadsrijk z6wereld plek rozenkloof_schatkamer
/guhs vadsrijk z6wereld plek bazaar_kruising
/guhs vadsrijk z6wereld plek moederpalm
/guhs vadsrijk z6wereld plek toren_voet
```
**Hoe kom je er (verhaal):** zone 5 afmaken → Havenmeester Windvaan → de film "De grote zwemtocht" (Moeder Wolkwal zwemt met heel
Hoogvads naar het oosten) → lijn E (Staarthaven ↔ Rozenkloof) gaat open → quest "Land in zicht!".

**Wat er is (elk heeft hieronder een eigen blok):**
- **De kaart** (u-z6wereld): Rozenkloof (kloofstad, aankomst), Saffraanpoort (hoofdstad), 5 gebieden met elk een sterrenbaken,
  De Zingende Zoutgrot, De Verzonken Sterrenwacht (kerker), de toren. De Schatkamer is opnieuw gebouwd als diepe
  Petra/Al-Khazneh-gevel (vrijstaande zuilen, verdiept portiek, gebroken fronton, ronde tholos) en de kloofwanden hebben reliëf.
- **Het verhaal** (u-z6verhaal): 10 hoofdstappen, 15 zijquests, Ringeling wordt opgevrolijkt en bondgenoot.
- **Wezens en bazen** (u-z6wezens): 13 wezens, 2 veldbazen (Koningin Wormelien, de Zoutkolos), de kerker met 3 bazen,
  de **Bultguh** (rijdier met 2 zitplekken) en het **vliegende tapijt** (u-tapijt, zweverig en glijderig).
- **De raid** (u-z6raid): 2-10 spelers, 4 verdiepingen, schaalt mee; zie het raid-blok en de loot-tabel bovenaan.
- **Muziek en sfeer** (u-z6muziek, u-geroezemoes): eigen gebiedsthema's, stadsmuziek, bazenmuziek met fases, bazaar-geroezemoes.
- **Leven in de stad** (u-leven-stad, u-leven-ritme): ook in Saffraanpoort een speeltuin, terrassen, beestjes en een dag/nacht-ritme.
- **De grotkerker** (u-grotkerker): ook de Zoutgrot is een herhaalbare mini-kerker met eindbaas.

**Controleren dat het klopt:** `/guhs vadsrijk z6wereld gebied` (gebied, biome en levelband), en in survival van de luchthaven via de
Siq naar Saffraanpoort lopen: geen sprongen hoger dan 1 blok, geen gaten.

## SPIEK U-Z6WERELD: zone 6 "Het Zijdeland" (de kaart)

Alles via `/guhs vadsrijk z6wereld plek <id>` (teleporteert je erheen, ook vanuit de bovenwereld). Tab-aanvulling kent alle ids.
De volledige lijst staat in `POI_Z6.md`. Speel in survival om de beloopbaarheid te voelen; zet `/time set night` voor de lampen.

### 1. Aankomst: Rozenkloof (lv 60, stad)
- `/guhs vadsrijk z6wereld plek haven_z6_z5_vertrek` — de luchthaven van lijn E op de noordrand. Kijk: meertoren met bogen, dek met hek, mast, bel, vertrekbord; het schip dokt aan de open (zuid)kant.
- Loop van het dek naar beneden de kloof in (trap/pad, geen sprongen > 1 blok).
- `/guhs vadsrijk z6wereld plek rozenkloof_plein` — het Rozenplein: fontein, kraampjes, bultguhs/tapijten/Pluimguh bij de stal en de tapijtwinkel.
- Kijk naar de kloofwanden: uitgehouwen tempelgevels (zuilen, frontons, diepe deuren, nissen met beelden), balkons, hangende tuinen, natuurlijke steenlagen en overhangen. Geen vlakke gestreepte wand meer.
- `/guhs vadsrijk z6wereld plek rozenkloof_schatkamer` en `... rozenkloof_schatkamer_hal` — de grote gevel en de hal van Kloofmoeder Roosje (vloer, licht, ronde messing hanglampen).
- `/guhs vadsrijk z6wereld plek rozenkloof_richel_2` — loop over de richels en de bruggen; `... rozenkloof_post_1` t/m `_6` (de brievenhuizen), `... rozenkloof_tuin_1` t/m `_3`.
- `/guhs vadsrijk z6wereld plek rozenkloof_siq_boven` — de Siq omhoog naar het plateau; hier begint de karavaanweg.
- `/guhs vadsrijk z6wereld plek uitkijk_zeemond` — uitzicht over de zeestraat naar Hoogvads.

### 2. De hoofdstad: Saffraanpoort (lv 62, stad)
- `/guhs vadsrijk z6wereld plek saffraanpoort_westpoort` — kom binnen door de Westpoort (de karavaanweg eindigt hier).
- `/guhs vadsrijk z6wereld plek saffraanpoort_paleisplein`, `... paleis_spiegelzaal`, `... paleis_troon` — paleis, Spiegelzaal, troon.
- `/guhs vadsrijk z6wereld plek bazaar_kruising` — de overdekte Grote Bazaar (koepeltjes met lichtgaten, winkelnissen).
- `/guhs vadsrijk z6wereld plek karavanserai` — het hof met alle drie de rijdieren.
- `/guhs vadsrijk z6wereld plek sterrenwacht_top` — de Sterrenwacht van Twinkel (wenteltrap naar boven).
- `/guhs vadsrijk z6wereld plek saffraanbrug`, `... kanaalwijk`, `... noria_1` — brug, kanalen, waterraderen.
- `/guhs vadsrijk z6wereld huis <id>` — naar binnen in een huis/winkel met een rol (zie `HUIZEN` in POI_Z6.md).

### 3. Het land
- `/guhs vadsrijk z6wereld plek duinbaken` (Roze Duinen), `... palmbaken` (Palmenkloven, op het plateau), `... zoutbaken` (Zoutvlakte), `... ruinebaken` (Ruïnedal, op een mesa), `... oasebaken` (Moederoase). De bakens zijn nog DONKER (het verhaal steekt ze aan).
- Wegen: loop van `rozenkloof_siq_boven` naar `saffraanpoort_westpoort`, van de Zuidpoort naar het zuiden (`duinen_karavaanweg`) en naar de Zoutvlakte/oase. Elke helling heeft traptreden, lantaarns langs de weg.
- `/guhs vadsrijk z6wereld plek watervallen_voet` — de watervaltrap omhoog naar het plateau; `... moerbei_1` (zijdecocons).
- `/guhs vadsrijk z6wereld plek karavaankamp` — tenten, kampvuur, rijdieren; `... zoutziedershuisje`; `... zoutvlakte_mikakamp`.
- `/guhs vadsrijk z6wereld plek zoutgrot_ingang` — **De Zingende Zoutgrot**: loop de trap af, grote zoutkristalhal, gloeiend meertje (`zoutgrot_vis`), puzzelnis met 5 kristallen (`zoutgrot_puzzel`), de kristalwand (`zoutgrot_kristalwand`, gaat open via het verhaal), elitekamer (`zoutgrot_elite`).
- `/guhs vadsrijk z6wereld plek kerker_ingang` — De Verzonken Sterrenwacht (koepelruïne, hal, trap naar de verzegelde sterrenpoort `kerker_poort`).
- `/guhs vadsrijk z6wereld plek moederpalm`, `... moeder_bron`, `... mager_mientje` — de oase, de reuzenpalm, het vluchtelingenkamp.
- `/guhs vadsrijk z6wereld plek toren_poort` — De Sterrendovende Toren (grauw, gouden ringen, de Lege Lens bovenop); de poort is verzegeld met een gouden ring.
- Uitkijkpunten: `uitkijk_duinkam`, `uitkijk_palmwaterval`, `uitkijk_zoutrots`, `uitkijk_mesa`, `uitkijk_oase`.
- Geheimen: `geheim_knuffel_1` t/m `_8`, `geheim_schat_1` t/m `_3` (de mesa-schat via een verborgen trapje).

### 4. Gebieden, biomen, levels
- `/guhs vadsrijk z6wereld gebied` — zegt in welk gebied je staat, de biome ("Het Vadsrijk - ...", ook in F3) en de levelband.
  Verwacht: Rozenkloof 60 (stad), Roze Duinen 60-62, Palmenkloven 62-64, Saffraanpoort 62 (stad), Zoutvlakte 64-66, Ruïnedal 66-68, Moederoase 68-70, de toren 70.
- `/guhs vadsrijk z6wereld bewoners` — telt de figuren + rijdieren; `... bewoners uit|aan` om ze weg te halen/terug te zetten.

### 5. De barrière tussen zone 5 en 6
- Zwem of vlieg (zonder luchtschip) vanaf de kust de zeestraat in: `/tp @s 3250 64 -760` → je wordt zachtjes teruggeduwd met bloesemblaadjes; melding "De warme Zijdewind waait je zachtjes terug...".
- Ten oosten van de oase (`/tp @s 4220 70 -735`): terugduw met "Verder naar het oosten wordt het grauw en gevaarlijk..." (zone 7 komt later).
- Let op de randen van de kaart: het land loopt aan de noord-, zuid- en oostrand af in de open zee, geen afgesneden chunks.

### 6. Ronde afronding (nieuw om te testen)
- **De Schatkamer** (diepe gevel): `/guhs vadsrijk z6wereld plek rozenkloof_schatkamer`. Loop een stuk terug op het plein en kijk
  omhoog: vrijstaande ronde zuilen in een diepe portiek, fronton met guh-medaillon, twee paviljoens met halve frontons en de ronde
  tholos in een diepe nis onder de rotskroon. Loop tussen de zuilen door de portiek in.
- **Reliëf op de kloofwanden**: `/guhs vadsrijk z6wereld plek rozenkloof_richel_2` en `... haven_z6_z5_vertrek` (van boven):
  pilasters op elke vakgrens, kroonlijsten onder de overhangen. Loop over de richels: niets mag in de weg staan op trappen/bruggen.
- **Zoutgrot** (was helemaal onbereikbaar): `/guhs vadsrijk z6wereld plek zoutgrot_ingang`, loop in survival de trap af tot in de
  hal, dan oostwaarts door de tunnel naar de elitekamer (`zoutgrot_elite`), en weer terug naar boven.
- **Kerker**: `/guhs vadsrijk z6wereld plek kerker_ingang` → trap af naar de poort (`kerker_poort`) en weer omhoog zonder springen.
- **Toren**: `/guhs vadsrijk z6wereld plek toren_voet` → de trap op naar de poort; de drempel heeft geen gat meer.
- **Oostpoort-wegen**: `/guhs vadsrijk z6wereld plek saffraanpoort_paleisplein`, loop de Oostpoort uit: de wegen naar de oase en
  het Ruïnedal gaan nu om de rotsmesa heen (geen tunneltrap meer).
- **Kampvuren**: `/guhs vadsrijk z6wereld plek kampvuur_karavaankamp` en `... kampvuur_moederoase`: je staat op de grond naast het
  vuur, niet op een tent of palm.
- **Oudste Wachter**: `/guhs vadsrijk z6wereld plek z6w_leider_oudstewachter`: naast het Ruïnebaken op de grond (stond op het dak).
## Spiek: zone 6 "Het Zijdeland", het verhaal (u-z6verhaal)

Alles in het Vadsrijk, in survival of creative. Zonder de wereld van zone 6 (u-z6wereld) zweven de NPC's op hun plek en zijn de
plekken lucht; met de wereld staan ze op de echte vloer. De commando's staan onder `/guhs vadsrijk z6verhaal ...`.

### 1. Levelcap 70
```
/guhs vadsrijk level 70
```
- Je HUD toont level 70 en de balk is vol (geen "volgend level").
- Level 61-69 kosten steeds iets meer xp (de curve loopt gewoon door).

### 2. De overgang: Moeder Wolkwal zwemt naar het oosten
Vanaf het begin (zone 5 nog niet af):
```
/guhs vadsrijk z5verhaal quest 10
/guhs vadsrijk z5verhaal stap
/guhs vadsrijk z5verhaal stap
/guhs vadsrijk z5verhaal stap
```
- Lever "Sterren in het oosten" in bij Havenmeester Windvaan (`/guhs vadsrijk z5verhaal tp havenmeester`).
- Windvaan zegt "Hou je vast!" en zodra de praatbox dicht is (binnen ~5 s) speelt de film **"De grote zwemtocht"**: de walvis
  bromt, zwemt naar het oosten, de roze kliffen, een wolkje "Tot hier".
- Daarna: lijn E is open en de quest **"Land in zicht!"** start vanzelf. Hoogvads blijft gewoon op zijn plek staan.

Alleen de film nog eens bekijken:
```
/guhs vadsrijk z6verhaal scene wolkwaltocht
```

### 3. Snel klaarzetten voor zone 6
```
/guhs vadsrijk z6verhaal klaarzetten
```
- Zone 1-5 klaar, level 60, lijn E open, "Land in zicht!" loopt.
- Naar Rozenkloof vliegen (de eerste keer de aankomstfilm):
```
/guhs vadsrijk z6verhaal vlieg
```
- Of via lijn E: bij de Staarthaven van Hoogvads (achteraan op de staart) een schip pakken. Bij de pier moet "Jij mag mee" staan.
- Op de reiskaart (bij een havenmeester) staat nu **Het Zijdeland**. Vóór de zwemtocht is het een grijs "?".

### 4. De hoofdlijn "De Sterrenkaravaan" (10 stappen)
Elke stap overslaan / direct naar een stap:
```
/guhs vadsrijk z6verhaal quest 1
/guhs vadsrijk z6verhaal quest 5
/guhs vadsrijk z6verhaal stap
```
(`quest N` zet alles vóór stap N klaar, ook de bakens; `stap` vinkt het eerstvolgende doel af.)

Wat je moet zien, per stap (de tracker wijst steeds de plek aan):
1. **Land in zicht!** Havenmeester Zandloper, dan de wandtrappen af naar Kloofmoeder Roosje in de Schatkamer.
2. **De karavaan van Oma Dadel**: drie klusjes op het Rozenplein (dadels, waterzakken, tapijten), dan bij **Stalmeester Hobbel** je
   eigen **bultguh** (zit de bultguh nog niet in het spel, dan krijg je een grijs berichtje en komt hij later vanzelf).
3. **Door de Roze Duinen**: de Siq omhoog, rustplek, Bobbeltje achter een duin, Koningin Wormelien in de verte, de Westpoort, Oma Dadel
   in de Karavanserai.
4. **Audiëntie bij Sultane Rozijntje**: Kaneelstok, de Sultane met de sterrenkaart, dan bovenin de Sterrenwacht de **eerste
   sterrenpuzzel** (Het Knuffeltje). Daarna een visioen: Moeder Bron fluistert. Je krijgt de **Sterrenlens** (rechtsklik!).
5. **Het Palmbaken**: Dadelpit, de watervallen op, sterrenpuzzel "De Dadelpalm" -> het baken vlamt op + visioen 1.
6. **Een tapijt van sterrenzijde**: 5 cocons (witte glinsters), Knoopje weeft het **vliegende tapijt**, proefvlucht naar de Saffraanbrug.
7. **De Zoutvlakte bij nacht**: karavaankamp, Zoutbaken (puzzel), de **Zoutkolos** wordt wakker (film), de Grauwe brief in het Mika-kamp.
8. **De Verzonken Sterrenwacht**: Professor Stof, de kerker (zonder kerker in de tree: een korte film die het Sterrenkompas geeft),
   het Ruïnebaken.
9. **De Spiegelzaal**: kompas op de loper -> **ontmaskering**: Meester Glans is **de Spiegelvizier**, een Mika-illusionist en de Knokkel van Ringeling. Achtervolging
   over drie daken, hij vlucht naar de toren.
10. **De Moederoase**: Oma Dadel en de grote karavaan, Duinbaken, de luchtspiegeling, Oasebaken, **Moeder Bron opvrolijken** (klik op
    haar), de voet van de toren: **Ringeling lacht van boven en verzegelt de poort**. Inleveren bij Moeder Bron: titelkaart
    **"Ridder van het Zijdeland"**. Het verhaal is af; de raid is open.

### 5. De sterrenbakens en puzzels los testen
```
/guhs vadsrijk z6verhaal puzzel palm
/guhs vadsrijk z6verhaal puzzel zout
/guhs vadsrijk z6verhaal puzzel examen_1
/guhs vadsrijk z6verhaal puzzel lied
/guhs vadsrijk z6verhaal puzzel afdingen_kardemom
/guhs vadsrijk z6verhaal baken duin
/guhs vadsrijk z6verhaal scene visioen
```
- Kies expres eerst een verkeerde ster: een lief berichtje, en je begint opnieuw. Fout kost niks.
- Overdag: de lens laat even de nachthemel zien (scherm wordt donker met sterren).
- `baken <id>` steekt een baken meteen aan: lichtstraal, sterrenscherfje, visioen (elke keer sterker).
- Rechtsklik de **Sterrenlens**: "x van de 5 bakens branden" + het volgende baken.

### 6. Alle films
```
/guhs vadsrijk z6verhaal scene aankomst
/guhs vadsrijk z6verhaal scene zoutkolos
/guhs vadsrijk z6verhaal scene kerker
/guhs vadsrijk z6verhaal scene ontmasker
/guhs vadsrijk z6verhaal scene ontsnapt
/guhs vadsrijk z6verhaal scene luchtspiegeling
/guhs vadsrijk z6verhaal scene bron
/guhs vadsrijk z6verhaal scene toren
/guhs vadsrijk z6verhaal scene brief
/guhs vadsrijk z6verhaal scene klaar
```

### 7. Na de raid: Ringeling wordt opgevrolijkt + flash F4
```
/guhs vadsrijk z6verhaal ringeling
```
- Film "Opgevrolijkt!": haar tien ringen rollen weg, ze giechelt, wordt roze en geeft je **de ring van de Keizer**.
- Daarna **F4**: Krabbel leest haar briefje voor aan de Keizer ("Ik stop. Ik heb sterren gezien."), de grauwe vinger tikt, en het Oog
  draait naar Hoogvads. Dan het slot: de karavanen komen terug.
- Titel **"Vriend van Ringeling"**.
- Ga naar de **Gesluierde Dame** in de Paradijstuin van Saffraanpoort:
```
/guhs vadsrijk z6verhaal tp ringeling
```
  Vóór de raid: "Ken ik jou? Nog niet, schat." Erna praat ze als Ringeling, jouw bondgenoot.

### 8. NPC's en plekken
```
/guhs vadsrijk z6verhaal tp zandloper
/guhs vadsrijk z6verhaal tp dadel
/guhs vadsrijk z6verhaal tp hobbel
/guhs vadsrijk z6verhaal tp rozijntje
/guhs vadsrijk z6verhaal tp twinkel
/guhs vadsrijk z6verhaal tp knoopje
/guhs vadsrijk z6verhaal tp bron
/guhs vadsrijk z6verhaal tp kk_poort_saffraanpoort
/guhs vadsrijk z6verhaal tp bond_bord_rozenkloof
```
- In beide steden: een **bankier** ("Naar mijn kluis"), een **Kwartiermeester** (Knuffelkwartier) en een **Bondenbord** (Knuffelbonden).
  Zonder die onderdelen in de build zeggen ze "binnenkort".
- In de bazaar: Kardemom (winkel), Pistache (veilinghuis), Lokum (juwelier), Koperkop (smid) en de leermeesters smeden/naaien; koken in
  de Karavanserai, brouwen onder de Sterrenwacht, imkeren in de Paradijstuin ("Werkplaats").
- Het **Verkenningsboek** (toets B) heeft een pagina **Het Zijdeland**.

### 9. Opnieuw beginnen
```
/guhs vadsrijk z6verhaal vergeet
```
(Vergeet zone 6: quests, bakens, geheimen, films, lijn E, Ringeling. Als zone 5 klaar is, speelt de zwemtocht daarna opnieuw.)
### Zone 6 "Het Zijdeland": wezens, veldbazen, kerker, buit en de Bultguh (U-Z6WEZENS)

Zone 6 is lv 60-70: lief en zoet, alleen grauw bij de toren. De meeste wezens zijn ontvadste woestijndieren met een grauw korstje "sterrenzout": die **vrolijk je op** (roze plof, daarna huppelen ze blij rond). De Mika's (Lampendovers, Ringdragers) vluchten in een rookwolk. Constructen (Zandvlooien, Spiegelslang, Zandgeestje, de Zoutslang) vallen uiteen in glinsterend zand.

> **Let op: alle plekken zijn nog PLACEHOLDERS** tot u-z6wereld de kaart van zone 6 bouwt (dan pakt alles vanzelf de echte POI).
> Zonder kaart staan de veldbazen misschien in de lucht of in zee. Typ dan eerst `/guhs vadsrijk z6wezens bouw arenas` (legt zandvloeren onder de twee veldbaas-arena's; ze verdwijnen weer bij een herstart).
> De kerker bouwt zichzelf (eigen kamers onder het Ruïnedal) zodra je erheen gaat.

#### 0. Snel overzicht
- `/guhs vadsrijk z6wezens alle` zet alle 14 wezens op een rij (zonder AI). Sla ze om de afloop te zien.
  - Kijk naar: het zandvlooitje met voelsprietjes, de fennekguh met gigantische oren, de springpiep op zijn springstaart, de roze schorpioen met een strikje op zijn staart, het dadelaapje met een dadel, de gierguh met een pluizige kraag, de glazen spiegelslang, de zoutkreeft met zoutkristallen op zijn rug, het stofwolkje, de stenen ruïnewachter met een gouden hoedje, de twee Mika's (met reuzendover / met lichtpotten), het gedoofde sterretje en de grote roze Zoutslang.
- `/guhs vadsrijk z6wezens plekken`: alle plek-ids met coördinaten (+ of ze echt of placeholder zijn).
- `/guhs vadsrijk z6wezens spawners`: hoeveel wezens waar (max ~70 in heel zone 6).
- `/guhs vadsrijk z6wezens opruimen`: haalt alle zone-6-wezens binnen 64 blokken weg.
- `/guhs vadsrijk z6wezens spawn <soort> [level] [elite]`: één wezen voor je neus.

#### 1. De Roze Duinen (lv 60-62)
- `/guhs vadsrijk z6wezens tp duinen`
- **Zandvlooi**: zit verstopt in het zand (je ziet alleen twee voelsprietjes, geen naambordje). Loop binnen 5 blokken: ze springen eruit! Daarna springen ze om de ~3 s naar je toe.
- **Fennekguh**: hoort je al van 16 blokken en roept zijn maatje. Oren omhoog + **rode streep** = zijn sprong: stap opzij. Zeldzaam huisdier: **Fennekje**.
- **Springpiep**: **rode cirkel** = zandschop (1,5 s donker als je erin staat), daarna stuitert hij weg en weer terug. Zeldzaam huisdier: **Springpiepje**.
- **Duinkruiper** (roze schorpioen): **rode kegel** voor hem = staartprik + Plakkerig.
- Leider **Oude Oorschelp** (gouden gloed).

#### 2. De Palmenkloven (lv 62-64)
- `/guhs vadsrijk z6wezens tp palmenkloven`
- **Dadelaapje**: gooit dadels (rode cirkel) en **pikt soms munten** ("Het dadelaapje pikt 12 brons!"). Vrolijk hem op: je krijgt alles terug.
- **Gierguh**: cirkelt hoog en duikt op wie het **laagst** staat (rode streep). Daarna zit hij 2 s op de grond.
- Leider **Dadelbaas Pitje**.

#### 3. De Zoutvlakte (lv 64-66)
- `/guhs vadsrijk z6wezens tp zoutvlakte`
- **Spiegelslang**: maakt **twee kopieën** (luchtspiegeling). Maar één is echt; een kopie spat uit elkaar bij de eerste tik ("Een luchtspiegeling!") en doet nooit pijn. Testen: `/guhs vadsrijk z6wezens mechaniek spiegel`.
- **Zoutkreeft**: zoutkorst = van voren maar 35% schade ("Zoutkorst!"), van **achteren** 150% ("Zwakke plek!"). Loop om hem heen.
- **'s Nachts**: `/guhs vadsrijk z6wezens tp zoutnacht` (of `/time set night`). **Zandgeestjes**: paarse cirkel = hij zuigt je naar zich toe (duizelig). Laat **Sterrenstof** vallen. Overdag zijn ze er niet.
- Leider **Kapitein Korstje**.

#### 4. Het Ruïnedal en De Moederoase (lv 66-70)
- `/guhs vadsrijk z6wezens tp ruinedal` en `tp moederoase`
- **Ruïnewachter**: staat stil als een standbeeld (geen naambordje). Binnen 6 blokken: "De wachter wordt wakker!". Grote rode cirkel = stamp.
- **Mika-Lampendover** (ook bij de bakens): rode kegel = reuzendover: donker + je **Vahoeg en Snelpootjes gaan uit**.
- **Mika-Ringdrager**: paarse cirkel = hij opent een lichtvangpot: een paarse straal zuigt Vads. **Loop uit de cirkel** om de straal te breken. Testen: `mechaniek ringpot`.
- **Gedoofd sterretje** (bij de oase): drie rode cirkeltjes = sterrengruis. Zeldzaam huisdier: **Sterretje**.
- Leiders **De Oudste Wachter** (Ruïnedal) en **Ringdrager Brie** (oase).

#### 5. Veldbaas: Koningin Wormelien (lv 63, De Roze Duinen, overdag)
- `/guhs vadsrijk z6wezens baas wormelien` (werkt ook 's nachts; zonder kaart eerst `bouw arenas`).
- Een reusachtige roze zandworm met een guh-snoet en wimpers, grauw en hongerig.
- **Onderduiken**: ze verdwijnt in het zand (onraakbaar) en een rimpel volgt iemand. **Blijf bewegen**: wie stilstaat krijgt een zandfontein. Dan komt ze omhoog onder haar doelwit (rode cirkel). Testen: `mechaniek duik`.
- **Zandgolf**: rode baan, plakkerig.
- **Fase 2 (60%) Duinspuug**: ze slokt zand (castbalk "Duinspuug"), er verschijnen drie **roze plekken**: daar ben je veilig. Wie erbuiten staat wordt bedolven (35%). Testen: `mechaniek duinspuug`.
- **Fase 3 (30%) Helemaal boven!**: een **grauwe zoutkorst** op haar rug. Zolang die er zit is ze onraakbaar. Spring er vanaf een duin tegenaan of schiet hem eraf. Dan is ze 6 s beduusd en krijgt ze +50%. Testen: `mechaniek korst`.
- Aan het eind: ze wordt roze en valt in slaap in het zand (hartjes).
- Buit: **Karavaanwacht**-stuk (episch), Wormelientand, Duinzwemmerslaarzen, heel zeldzaam de Wimperwaaier (Guhddelijk), trofee, 2 Gouden Veren bij de eerste keer. Huisdier **Wormelientje**. Voor alle rijdieren: de **Wormelien-zadeldeken** (tuig, open je rijdierscherm N > Aankleden).

#### 6. Veldbaas: De Zoutkolos (lv 66, De Zoutvlakte, alleen 's nachts)
- `/guhs vadsrijk z6wezens baas zoutkolos` (werkt ook overdag).
- Een reus van samengekoekt sterrenzout. Hij valt uiteen in zout, geen opvrolijken.
- **Zoutgolven**: rode barstlijnen vanaf zijn voeten (castbalk). `mechaniek zoutgolf`.
- **Fase 2 (60%) Zingende kristallen**: hij gooit 3 roze kristallen. Om de 3 s klinkt een **hoge noot**: een paarse ring rond elk kristal. Stap eruit, of sla het kristal stuk. `mechaniek kristallen` / `mechaniek zing`.
- **Fase 3 (30%) Gloeiend hart**: **kijk recht naar zijn hart** terwijl je slaat: dubbele schade ("Hart geraakt!"). Met de Sterrenlens in je hand x2,5.
- Buit: Karavaanwacht-stuk, Zingend Zoutzwaard, Zoutsplinterboog, Zoutkristalkroon, trofee. Huisdier **Zoutklontje**.

#### 7. De kerker: De Verzonken Sterrenwacht (lv 66-68)
- Ingang: het luik in de oude koepel in het Ruïnedal (rechtsklik). Snel: `/guhs vadsrijk z6wezens tp sterrenwacht` (bij het luik) of meteen `tp kerker` (binnen).
- De kamers: **Ingangshal** (Lampendovers + een Ruïnewachter) > gang > **Ringenzaal** > gang > **Kaartenzaal** (sterrenkaarten, Lampendovers, een Ringdrager, een sterretje) > gang > **Vuurschalenzaal** > gang > de **Koepel** met een sterrenhemel. Terug naar boven: de ladder bij de ingang of achter in de Koepel (rechtsklik).
  - Kijk naar: vlakke vloeren, gangen van 5 breed, overal licht, geen gaten.
- **Baas 1: De Astrolabiumwachter** (`baas astrolabiumwachter`): een ster met draaiende messing ringen.
  - Je klappen komen maar voor 25% door ("De ringen kaatsen je klap af!"), behalve als de ringen **in één lijn** staan: **gouden licht**, "NU!", 5 s volle schade (elke ~11 s). `mechaniek lijn`.
  - **Ringzwaai**: rode banden rond hem met **roze openingen**: sta in een opening. Elke 25% een ring erbij (max 4). `mechaniek ringzwaai`, `fase 4`.
- **Baas 2: Kootje Glimmertje** (`baas glimmertje`): Mika-ploegbaas met een reuzendover.
  - Vier **vuurschalen** in de hoeken. Om de ~15 s dooft hij er een (paarse streep). **Rechtsklik een donkere schaal 2x** om hem weer aan te steken.
  - 2 uit = donker; 3 uit = zijn telegraphs worden grijs; **4 uit = onraakbaar**. `mechaniek doof` dooft ze allemaal.
  - Op 50% fluit hij: twee schalen uit + twee Lampendovers. Hij vlucht in rook.
- **Baas 3: De Verzonken Sterrenguh** (`baas sterrenguh`): een oude reuzen-guh van sterrensteen, grauw geworden.
  - Vier **telescopen** rond de Koepel. **Rechtsklik** = een kwartslag draaien. Een gouden straal = deze staat goed. Alle vier goed: **zijn hart gloeit**, 10 s dubbele schade. Daarna een sterrenschok (rode ring, blijf dichtbij) en de telescopen staan weer scheef. Zonder hart: 35%. `mechaniek hart`.
  - Sterrengruis (rode cirkels), vanaf fase 2 een wanhoopsgolf (rode ring vanaf hem: kom dichtbij).
  - Aan het eind gloeit hij weer, en je krijgt **Het Sterrenkompas** (voor het verhaal, hq08). Huisdier **Sterrenguhtje**. 1 Gouden Veer.
- Buit van alle drie: een stuk van de **Sterrenwachtset** (episch) + benoemde items.

#### 8. De grot-elite: De Zoutslang (lv 66, De Zingende Zoutgrot)
- `/guhs vadsrijk z6wezens tp zoutgrot` (placeholder) of `spawn zoutslang`.
- Ze zwemt door de rots en komt af op **geluid**: rennen en slaan. Is ze onder? Dan kun je haar niet raken. **Rode streep** = daar komt ze uit. `mechaniek slangduik`.

#### 9. Buit (lv 60-70)
- `/guhs vadsrijk z6wezens set duinwandelaar` (ridder/jager), `set duinzijde` (magiër), `set karavaanwacht`, `set sterrenwacht`.
  - De wanten/handschoenen, cape en armband draag je in je **sieradenvakken**; ze tellen mee voor de set.
  - Set-regels in de tooltip, groen als je ze hebt. Bijvoorbeeld Duinwandelaar: (2) Verdediging +14 en Vads +80, (4) Schade tegen bazen +8%, (6) Loopsnelheid +8%. Sterrenwacht (6): elke 20 s doet je volgende klap +40% ("Sterretje!").
- `/guhs vadsrijk z6wezens sieraden`: Saffraanring en **Woestijnroosring** (allebei ook in je **tweede ringvak**), Rozenketting, Sterrenoorbel, Zoutkristalarmband.
- `/guhs vadsrijk z6wezens wapens`: per **specialisatie** een wapen, de tooltip zegt "Gemaakt voor: ..." (Rozenkromzwaard = Schildridder, Dadelpalmknots = Reuzenkussen, Duinvalkboog = Scherpschutter, Karavaankruisboog = Strikkenzetter, Saffraanstaf = Sterrenwever, Oasefluit = Zegenzanger). Vergelijk met je eigen wapen: de pijltjes in de tooltip.
- `/guhs vadsrijk z6wezens kristallen`: Rozenkwarts (Kritiek) en Sterrenzout (Schade tegen bazen).
- `/guhs vadsrijk z6wezens items`: alle 10 benoemde baasitems + 3 trofeeën + het Sterrenkompas. Elk heeft een "Effect:"-regel.
- `/guhs vadsrijk z6wezens kist <baas>`: een buitkist van die baas.
- `/guhs vadsrijk z6wezens huisdieren`: alle 8 zone-6-huisdieren (druk **P**): Fennekje, Springpiepje, Sterretje, Dadelaapje, Bobbeltje, Wormelientje, Zoutklontje, Sterrenguhtje.
- `/guhs vadsrijk z6wezens sterrenstof`: 16 Sterrenstof (materiaal).

#### 10. De Bultguh (rijdier, 2 zitplaatsen)
- `/guhs vadsrijk z6wezens bultguh` (of `bultguh saffraan|wolkwit|kaneel|sterrennacht`): je krijgt hem en zit er meteen op.
  - Kijk naar: twee zachte bulten met het zadel ertussen, een lange nek, guh-oortjes, **kwastjes** langs de zadeldeken en **belletjes** om de nek die meewiegen.
  - **Op zand loopt hij 30% sneller.**
- **Tandem**: laat een **tweede speler** de Bultguh rechtsklikken terwijl jij erop zit: die zit achterop ("... zit achterop!"). Alleen jij stuurt. Stap jij af, dan draaft de Bultguh naar huis.
- Rijdierscherm (**N**): de Bultguh staat erbij, met vachten (Zandroze, Saffraan, Wolkwit, Kaneel, Sterrennacht), patronen en tuig.
- In het echte spel krijg je hem in de stalquest van Oma Dadel in Rozenkloof (verhaal-slice u-z6verhaal).

#### 11. Muziek
- `/guhs vadsrijk z6wezens muziek`: welk bazenthema elke baas wil, en wat hij nu speelt (zolang de nieuwe thema's niet gemaakt zijn, speelt er een bestaand thema).
## SPIEK U-Z6RAID: De Sterrendovende Toren (de eerste raid)

Branch `110-u-z6raid`. Alles onder `/guhs vadsrijk z6raid ...` (gamemaster). Speel in de Vadsrijk-dimensie.

### Hoe de raid werkt (kort)
- **2 tot 10 spelers.** Alleen kom je er niet in.
- De toren aan de Moederoase heeft een deur (die bouwt u-z6wereld). Loop je erin, dan sta je in **De Wachtzaal**: de begane grond van de binnentoren (een ruimte onder de toren).
- Ga met 2-10 spelers **3 tellen op de gouden startcirkel** staan. Partyleden die in de Wachtzaal staan, gaan mee.
- **Eén groep tegelijk.** Is de toren bezet? Dan kom je via de cirkel in de **rij**. Ben je aan de beurt, dan krijg je een bericht en ga je vanzelf naar boven als er 2+ van jullie in de Wachtzaal staan.
- **Vier verdiepingen**, elk met een trap. De trap omhoog zit dicht met paars **sterrenvlies** tot de baas van die verdieping is opgevrolijkt.
- **Flauwvallen is mild.**
  - Je wordt wakker op het trapbordes van de verdieping waar je bent, en je mag gewoon weer meedoen.
  - Eénvads-ridders gaan in de raid NIET met pensioen.
- **Wipe:** staat er niemand van de groep meer levend in de zaal terwijl het gevecht bezig is? Dan begint de verdieping na 3 tellen opnieuw. De wipeteller gaat dan +1.
- **Elke clear geeft een kist** (er is geen weeklimiet). Na een clear brengt de lichtzuil in het midden van het dak je naar buiten. De uitgang in de Wachtzaal is de paarse boog in het westen.
- Rechtsboven zie je de **raidframes**:
  - tot 10 spelers met hun Vads, flauw/weg/buiten;
  - de verdieping en de baas;
  - de gevechtsklok tot de woede (enrage);
  - de wipes, de totale tijd en het aantal spelers.

### Snel testen (alleen, met nepspelers)
```
/guhs vadsrijk z6raid bouw
/guhs vadsrijk z6raid start 4
```
- `bouw` plaatst de binnentoren onder de toren op 4140,16,-688. Dat gebeurt ook vanzelf als je er dichtbij komt.
- `start 4` start de raid als groep van 4: jij plus 3 nepguhs, die grijs in de raidframes staan.
- Je staat dan op het bordes van verdieping 1. **Loop de zaal in** (naar het westen): de Zandwachter staat in het midden.
- Wat je moet zien:
  - de intro;
  - de baasbalk;
  - de raidframes rechts;
  - bij 4 spelers 2 pilaren en 2 spiegels (de bazen schalen x2.35).

```
/guhs vadsrijk z6raid info
/guhs vadsrijk z6raid verdieping 2
/guhs vadsrijk z6raid verdieping 3
/guhs vadsrijk z6raid verdieping 4
/guhs vadsrijk z6raid wipe
/guhs vadsrijk z6raid klaar
/guhs vadsrijk z6raid stop
```
- `info`: wie er binnen is, de rij, de HP van de 4 bazen en welke trappen open zijn.
- `verdieping 2`/`3`/`4`: spring naar die verdieping (de trappen eronder gaan open).
- `wipe`: forceer een wipe. De baas komt vol terug, de teller gaat +1 en je staat weer op het bordes.
- `klaar`: meteen een clear. Je krijgt een kist, de sterren in de koepel gaan branden en je krijgt de titels.
- `stop`: de toren is weer vrij.

Een baas zonder groep (creatief vecht je mee):
```
/guhs vadsrijk z6raid baas 1
```
(Dit kan ook met 2, 3 of 4.)

Een fase overslaan (sta bij de baas):
```
/guhs vadsrijk z6raid fase 2
/guhs vadsrijk z6raid fase 3
```

Teleporteren:
```
/guhs vadsrijk z6raid tp wachtzaal
/guhs vadsrijk z6raid tp poort
/guhs vadsrijk z6raid tp 3
```
- `tp poort` zet je vlak bij de torendeur. Loop erin en je komt in de Wachtzaal.

### Verdieping 1: De Zandwachter
- De **zandhuid**: hij krijgt maar half zoveel schade.
- **De zandloper** (cast-balk, 25 s):
  - Op de twee roze platen (west en oost) moet TEGELIJK iemand staan, 2 tellen lang.
  - Dan draait hij om: hij is verdoofd en 18 s kwetsbaar. In zijn **rug** doe je x1,8 schade.
  - Lukt het niet, dan volgt **Begraven**: 45 % schade.
- **Zandpilaren:** sla ze binnen 20 s kapot, anders bouwt hij een zandmuur.
- **Zandgolven:** rode banen; zoek het gat.
- **Zandvuist:** een gele cirkel. Ga er SAMEN in staan (stack).
- **Spetters:** ga uit elkaar (spread).
- **Zandstorm:** een paarse cast-balk. Onderbreek die door schade te doen.

Mechanieken testen:
```
/guhs vadsrijk z6raid mech zandloper
/guhs vadsrijk z6raid mech omdraaien
/guhs vadsrijk z6raid mech pilaren
```

### Verdieping 2: De Spiegelvizier (Meester Glans)
- Hij verstopt zich tussen identieke spiegelbeelden. **Alleen de echte fonkelt af en toe** (kijk goed!).
- Rechtsklik een **draaispiegel**: die wijst dan naar het volgende beeld.
- Staan 2 lichtbundels op hetzelfde beeld?
  - Is het de echte: hij is 20 s kwetsbaar.
  - Is het nep: er stapt een spiegelkloon uit en iedereen krijgt 10 %.
- **Wie is wie?:** elke 30 s wisselt hij twee spelers van plek.
- Bij **20 %** breekt de zaal: er regenen scherven.

```
/guhs vadsrijk z6raid mech verstop
/guhs vadsrijk z6raid mech ontmasker
/guhs vadsrijk z6raid mech fout
```

### Verdieping 3: De Gedoofde Sterrenbeelden
- Er zijn er drie tegelijk:
  - **De Grote Knuffel** (taai):
    - Knuffelklem: een cirkel op het doelwit; de rest gaat weg.
    - Berenbrul: een donut, dus ga naar BINNEN.
  - **De Vliegende Bultguh:** stormt over een rode baan.
  - **De Kleine Pluis:** heelt de andere twee. Onderbreek haar **Sterrenlied**.
- Op 0 zijn ze niet verslagen maar **"uit"** (ze stralen). Dan loopt er een **venster van 10 s**: alle drie moeten erin uit.
- Te laat? Dan steken ze elkaar weer aan, op 40 %.
- In de raidframes zie je drie balken plus het venster.

```
/guhs vadsrijk z6raid mech uit
```
(Dit zet alle drie meteen uit.)

### Verdieping 4: Hertogin Ringeling (het dak)
- **Fase 1, Tien Tenen:**
  - Ze schopt ringen. Je ziet een roze vangcirkel in een rode cirkel.
  - **SPRING** in de roze cirkel vlak voor de ring landt: dan vang je hem en vliegt hij terug.
  - Niet gevangen? Dan krijg je het effect van de ring: vuur, ijs, lichtdief, duw of wissel.
- **Fase 2, De Lege Lens** (60 %):
  - Het wordt donker.
  - Pak een **sterrenscherf** (rechtsklik) en breng hem naar een gedoofd **sterrenbaken** (rechtsklik of erlangs lopen).
  - Branden alle bakens? Dan komt het licht terug: ze is verdoofd en krijgt x1,5 schade.
  - Zolang de bakens niet branden, zakt ze niet onder 25 %.
- **Fase 3, Ringdans** (25 %):
  - Lage ring = springen, hoge ring = sluipen.
  - Na 3 ringen barst de Lens: ze valt (x2 schade).
- **Einde:** ze wordt **opgevrolijkt** en praat. De sterren in de koepel gaan branden en ze keert zich tegen de Keizer.

```
/guhs vadsrijk z6raid mech schop
/guhs vadsrijk z6raid mech lens
/guhs vadsrijk z6raid mech bakens
/guhs vadsrijk z6raid mech val
```

### Buit
```
/guhs vadsrijk z6raid loot
/guhs vadsrijk z6raid loot baas
/guhs vadsrijk z6raid loot zeldzaam
/guhs vadsrijk z6raid items
```
- `loot`: een clear-kist.
- `loot baas`: een kleine kist (zoals bij baas 1-3).
- `loot zeldzaam`: de Sterrenring-vleugels (in je Kledingkast, toets O; luchtdash), het huisdier Sterrenpluisje, het Sterrennacht-halster (= het rijdier, zie hieronder) en Ringelings Teenring.
- `items`: de hele Sterrenzijde-set, de 3 raidwapens en de Ringreplica.
- **Rijdier (V-INT, jouw loot-regel):** de **Bultguh in Sterrennacht-vacht** valt bij elke clear met **10 % per speler**, en is
  zeker na 15 keer mis. Heb je de Bultguh nog niet, dan krijg je hem erbij. Het halster uit `loot zeldzaam` geeft precies
  hetzelfde (rechtsklik).
```
/guhs vadsrijk loot pech z6raid_sterrennacht 15
/guhs vadsrijk loot rol z6raid_sterrennacht
```
  Daarna: N (rijdieren) > Bultguh > vacht Sterrennacht staat aan.
- **Knuffelbond:** een clear geeft elk lid in de toren **200 bondsxp** (plus 40 per verslagen baas, binnen de dagcap van 600).
- **HUD:** de raid-frames rechts zijn te verschuiven en te schalen in Instellingen > HUD aanpassen ("Raid-frames"), ook buiten de raid.
- **Geluid:** in de Wachtzaal en op de verdiepingen hoor je de zachte torendrone (geen gebiedsmuziek); in een gevecht de
  bazenmuziek met fases; na Ringeling haar eigen fanfare.
- Wat je moet zien:
  - de **Sterrenzijde-set** (lv 70, legendarisch):
    - 2 stuks = +15 Verdediging en +150 Vads;
    - 4 stuks = Schade tegen bazen +10%;
  - de wapens: Ringbreker (Ridder), Komeetboog (Jager) en Sterrenstaf (Magier);
  - de titels **Ringbreker** en **Sterrenaansteker** (Sterrenaansteker als je niet flauwviel). Die kies je in de Guhdex.

### Sterren
```
/guhs vadsrijk z6raid sterren aan
/guhs vadsrijk z6raid sterren uit
```
Dit zet de sterren in de koepel van het dak aan of uit.
## SPIEK U-Z6MUZIEK: muziek en sfeer van het Zijdeland (zone 6)

Oud-Midden-Oosten, maar guh-zoet: oed (gitaar), kanun (hakkebord), ney (fluit), darbuka en riq, met speeldoos, harp en belletjes.
Het **karavaanlied van Oma Dadel** is het leidmotief: je hoort het in de duinen, 's nachts op de Zoutvlakte, in Rozenkloof, bij de
Moederoase, in de stingers en (gezongen door de hele oase) in de laatste fase van Ringeling.

> Smaak gaat voor. Zeg welk stuk en wat eraan schort ("te druk", "ander instrument", "te somber"). Elk stuk is één blokje in
> `tools/vadsrijk/geluid/zijdeland.py`.

### Snel alles horen
| Wat | Commando |
|---|---|
| Klikbare lijst van alles | `/guhs vadsrijk geluid` |
| Alles stil | `/guhs vadsrijk geluid stop` |
| Wat speelt hier en waarom | `/guhs vadsrijk geluid info` |

### 1. Gebiedsmuziek (met lange stiltes, zoals overal)
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

### 2. Stadsdeuntjes
| Stad | Commando | Wat je hoort |
|---|---|---|
| Rozenkloof 1 (Rozenplein) | `/guhs vadsrijk geluid stad rozenkloof_1` | vrolijk aankomstdeuntje, speeldoos + kanun |
| Rozenkloof 2 (echo in de kloof) | `/guhs vadsrijk geluid stad rozenkloof_2` | ney met echo tussen de rotswanden |
| Saffraanpoort 1 (de Grote Bazaar) | `/guhs vadsrijk geluid stad saffraanpoort_1` | druk en vrolijk: tokkelende mandoline, klarinet, darbuka + tamboerijn |
| Saffraanpoort 2 (paleis en tuin) | `/guhs vadsrijk geluid stad saffraanpoort_2` | sierlijke hofwals, kanun en fluit, fontein van celesta |

### 3. Bazen: thema, laatste fase, fanfare
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

### 4. Stingers
| Wat | Commando |
|---|---|
| Sterrenbaken aangestoken | `/guhs vadsrijk geluid stinger baken` |
| Moederoase gevonden | `/guhs vadsrijk geluid stinger moederoase` |

De Moederoase-stinger speelt vanzelf de eerste keer dat je De Moederoase binnenloopt (in plaats van de gewone ontdekkingsstinger).
De baken-stinger moet nog aan het aansteken van een baken gekoppeld worden (verhaal-slice).

### 5. Sfeer (zacht, geen harde wind)
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

# B7. DE SYSTEMEN

Eerst alles wat in ronde U nieuw is of veranderd, daarna de systemen van de nachtronde.

## SPIEK U-SPECS: specialisaties + nieuwe job-advancements + gear-pijltjes

Het nieuwe model (jouw besluit V2/V3):
- **Lv 1:** je eerste job meteen; de klasse = het wapen in je hand, vrij wisselen tussen de 3 klassen.
- **Lv 17:** advancement 1 = de **specialisatie**, bij Grootmeester Vadsvoet, vlak vóór de Zwamtempel.
- **Lv 35:** advancement 2, in zone 3, bij Ordemeester Wasja in de Nestkapel (na de Kaaszeppelinwerf en je Pluimguh).
- **Lv 58:** advancement 3, bij Kapitein Veerkracht in Hoogvads (na het slaaplied van Moeder Wolkwal).

| klasse | spec A (links in de boom) | spec B (rechts) | zijtak |
|---|---|---|---|
| Knuffelridder | Schildridder → Kristalkampioen → Vahoege Paladijn | Reuzenkussen → Stormkussen → Nestwachter | Tank |
| Pluisjager | Scherpschutter → Sterrenschutter → Kruinvalk | Strikkenzetter → Poolstervalk → Vahoege Strikmeester | Steun |
| Vadsmagiër | Sterrenwever → Kristalorakel → Schemerwijze | Zegenzanger → Koorvads → Vahoege Aartszanger | Heel |

Alle commando's werken in het Vadsrijk (eerst `/guhs vadsrijk tp ...` of via Knuffelkamp).

### 1. De keuze op lv 17 (zoals een speler het ziet)
```
/guhs vadsrijk klasse wissel knuffelridder
/guhs vadsrijk klasse klasselevel knuffelridder 17
/guhs vadsrijk spec scherm
```
- Je ziet **twee grote kaarten** (Schildridder en Reuzenkussen): stijl, en per advancement de titel + wat je krijgt.
- Klik **"Word Schildridder!"** (of Reuzenkussen):
  - de **ceremonie** speelt (camera, lichtstralen);
  - de titelkaart toont **"Schildridder"**;
  - er komt een ring in de kleur van je spec.
- Druk **K**: de skillboom heeft nu **4 kolommen**: Schildridder | Stam | Reuzenkussen | Tank.
  - De andere spec staat dof met een ×.
  - Hover over een kolomkop: uitleg van de tak.
- Onderaan de boom staan de advancement-badges met de **spec-titels** (hover = wat je krijgt).

Via de echte quest (één keer proberen):
```
/guhs vadsrijk npc tp grootmeester
```
- De quest **"De keuze van de Knuffelridder"** verschijnt na de Druipgrotten (hq09), vanaf lv 16.
- Inleveren op lv 17 opent het keuzescherm.
- Sluit je het scherm zonder te kiezen? Dan staat er in **K** een gouden knop **"Kies je specialisatie!"**.

### 2. Doorspeccen: stap 2 en 3
```
/guhs vadsrijk spec stap 2
/guhs vadsrijk spec stap 3
```
- Elke stap speelt de ceremonie met de nieuwe titel. Stap 2 = de nest-ceremonie (veertjes); stap 3 = de kristal-ceremonie.
- `/guhs vadsrijk spec` toont per klasse: level, spec, stap en titel.

### 3. Wat elke spec doet (even voelen op een oefenpop of klodder)
- **Schildridder:**
  - houd **Knuffelschild** op en laat je raken: er komen kritiek-vonkjes;
  - je volgende **Kussenklap** binnen 4 s doet +50 % (zwaai-effect + schildgeluid);
  - Kussenklap slaat 2× (Dubbelklap).
- **Reuzenkussen:**
  - gewone klappen raken ook vijanden rond je doel (zwaai + wolkje);
  - Kussenklap is zwaarder maar 1 s trager;
  - stap 2: Stormsprong verdooft.
- **Scherpschutter:**
  - pijlen buigen een beetje;
  - schoten van ver (>10 blokken) doen meer;
  - stap 2: Knuffelpijl +80 %.
- **Strikkenzetter:**
  - **Rolsalto** laat een groene ring (val) achter, en vijanden die erin lopen plakken;
  - midden in de salto 3 pijlen;
  - je schiet merkbaar sneller.
- **Sterrenwever:** de Vuurbal laat een plas achter; elke 3e spreuk is gratis.
- **Zegenzanger:**
  - in een gevecht elke 2 s een gele ring met muzieknootjes (liedpuls);
  - teamgenoten binnen 10 blokken doen 8 % meer;
  - stap 2: de Vadsbron doet schade.

### 4. Wisselen en resetten
```
/guhs vadsrijk klasse balie
```
- **"Skillboom resetten"** is altijd gratis (geen uur wachten meer). Je spec en advancements blijven.
- Knop **"✦ Specialisatie"** → het kaartenscherm → **"Wissel naar Reuzenkussen"**:
  - alleen de punten uit de oude spec-tak komen terug; stam, zijtak en stappen blijven;
  - meteen nóg een keer wisselen mag niet: de knop is grijs, met de tooltip "Je kunt weer wisselen over 10 min";
  - in een gevecht mag het niet:
    ```
    /guhs vadsrijk spec gevecht
    ```
    De knop is grijs: "Niet midden in een gevecht!". Typ het commando nog een keer om het gevecht weer uit te zetten.
- Zonder balie (gewoon K) is er geen wisselknop. Wisselen kan alleen bij de klassemeester:
  - **Zone 1-3:** Grootmeester, Ordemeester (Kandijkom), Wasja (Nestkapel).
  - **Zone 4/5:** een extra praat-optie bij de **Duinwachter** (Lampionhaven) en **Kapitein Veerkracht** (Hoogvads): "Skillboom, specialisatie en reset".
- Testen zonder regels: `/guhs vadsrijk spec wissel forceer`.

### 5. Oude save (migratie)
```
/guhs vadsrijk klasse wissel knuffelridder
/guhs vadsrijk klasse klasselevel knuffelridder 52
/guhs vadsrijk spec oud 4
/guhs vadsrijk spec migreer
```
- Na de migratie heb je **stap 2** (oud 4 → nieuw 2), al je boompunten zijn terug, en er staat een gouden regel "mag nog een specialisatie kiezen".
- In K staat de knop **"Kies je specialisatie!"**. Kies → de ceremonie toont de stap-2-titel van die spec.
- De omzetting: oud 2+ → nieuw 1, oud 3+ → nieuw 2. Alleen oud 1 telt pas als nieuw 1 als de klasse lv 17 is.
- Een echte oude save wordt vanzelf omgezet bij binnenkomen in het Vadsrijk, met een roze chatregel.

### 6. Gear-pijltjes (geen item level)
```
/guhs vadsrijk klasse wapen pluisboog
```
- Leg dat wapen in je tas en pak een ander wapen in je hand; of hover over een harnasstuk of sieraad in je inventaris.
- Onderaan de tooltip staat:
  - **"Vergeleken met je wapen:"**;
  - per stat **▲ groen** of **▼ rood**, en bij wapens ook "Schade per klap";
  - het oordeel: **▲ Beter**, **▼ Slechter** of **= Ongeveer even goed**.
- Het stuk dat je draagt: "(dit draag je nu)". Een leeg vak: "Je draagt nog niets als ..." → altijd Beter.
- Een wapen van een andere klasse dan in je hand: geen vergelijking (eerlijk vergelijken kan niet).

### 7. Tests
- `runGameTestServer -Pgt=VadsrijkSpecs` (boomindeling, advancement + spec, wisselregels, migratie, gear-oordeel)
- `-Pgt=VadsrijkKlassen` (punten, gratis reset, DPS: beide specs binnen 10 %)
- `-Pgt=VadsrijkZ2Systemen`, `-Pgt=VadsrijkZ3Systemen`, `-Pgt=VadsrijkNpc`
## SPIEK U-ZICHTBAAR: je Vadsrijk-uitrusting zichtbaar op je karakter

Alles werkt alleen **in het Vadsrijk** (daarbuiten tellen de stukken niet mee en zie je ze ook niet). Kijk naar jezelf met **F5**.

### 1. De cape (de melding uit de feedback)
```
/guhs vadsrijk z3npc klaarzetten
```
Doe de cape aan (shift-klik in je inventaris), druk **F5**.
- Je ziet nu een echte cape op je rug die wappert zoals een Minecraft-cape.
- Elke cape heeft zijn eigen look: Knuffelcapeje (roze met hartje), Sporencape (paddenstoel), Galmcape (paarse kringen), Buitcape (donker met goud),
  Wortelcape (wortels + groen randje), Schemercape (sterren), plus Lampion-, Vlieger- en Wolkwachtcape (zone 4/5). Andere capes: een roze standaardcape.

### 2. Alles in één keer aantrekken
```
/guhs vadsrijk zichtbaar aankleden
/guhs vadsrijk zichtbaar aankleden schemercape bloesemhandjes honingarmband
/guhs vadsrijk zichtbaar aankleden galmcape kristalhandschoenen kristalarmband
/guhs vadsrijk zichtbaar aankleden buitcape schorshandschoenen harsarmband
```
(Tab geeft de keuzes. Wat je al droeg gaat naar je tas.) Kijk met F5:
- **Handschoenen**: om beide onderarmen/handen, met een manchet.
- **Armband**: om je linkerpols, net boven de handschoen, met een steentje.
- **Ringen**: een dun bandje met een steentje op je rechterhand (ring 1) en linkerhand (ring 2).
- **Ketting**: een V-kettinkje met een hangertje op je borst.
- **Oorbellen**: kleine druppeltjes aan beide kanten van je hoofd.
- Met een harnas/helm aan zitten ze er netjes overheen (niet erin verstopt).

```
/guhs vadsrijk zichtbaar status
/guhs vadsrijk zichtbaar uitkleden
```
`status` = wat de server over jou doorstuurt naar andere spelers. `uitkleden` = alle 7 vakjes terug in je tas.

### 3. De Kledingkast gaat voor (toets O)
- Kies in de Kledingkast een **cosmetische cape**: die vervangt je stat-cape op je rug.
- Doe **vleugels** aan: dan verdwijnt de cape (vleugels en capes delen je rug).
- Zet de rug weer op niks: je stat-cape komt terug.
- Transmog op je harnas werkt zoals eerst.

### 4. Andere spelers (als er iemand meedoet, of met een tweede client)
- De ander ziet jouw cape, handschoenen en sieraden ook; ook als hij pas later in de buurt komt.
- Ver weg: eerst verdwijnen ringen en oorbellen (16 blokken), dan ketting en armband (32), dan handschoenen (48). De cape blijft altijd.
- Ga naar huis: alles van het Vadsrijk is weg (je eigen wereld-cape komt terug).

### Wat al werkte (gecheckt, niets veranderd)
- De 4 harnasstukken zijn echte harnasvakjes en worden al getekend; je wapen in je hand ook.
### U-HUD: party-frames, emotes en de HUD-bewerker

Alles hieronder werkt in het Vadsrijk (emotes werken overal). Dev-commando's beginnen met `/guhs vadsrijk hud` (op 2).

#### Nieuwe toets
| toets | wat |
|---|---|
| **;** (puntkomma) | **Emote-wiel**: ingedrukt houden, muis naar een emote, loslaten = doen. Kort tikken = het wiel blijft open, dan klik je er een. |

Te veranderen in Opties → Besturing → Vadsrijk ("Emotes (ingedrukt houden)").

#### 1. Party-frames
Alleen testen? Neem een nep-party:
```
/guhs vadsrijk hud party nep
```
- Links in het midden staan nu 4 kaartjes: **Knuffelaar** (★ = leider, ridder-icoontje, buff Vahoeg), **Pluisje** (Vads-balk gaat op en neer, rood als hij laag is, pijltje + afstand "60m" dat meedraait als jij draait, buffjes rechts), **Vadsmagiertje** (rood: "Flauwgevallen!"), **Slaapkop** (grijs: "offline").
- Onder elke naam staat de klasse/titel ("spec"), rechts "Nv 42".
- Weer weg: `/guhs vadsrijk hud party nep 0` (of na 5 minuten vanzelf). Korter/langer: `/guhs vadsrijk hud party nep 2 60` (2 leden, 60 seconden).

Met een tweede speler (echte party): `/guhs vadsrijk party uitnodigen <naam>` en die doet `/guhs vadsrijk party accepteer` (de bestaande party-commando's).
- Loop ver weg (> 24 blokken): op het kaartje komt een pijltje + afstand. In een andere wereld: "elders".
- Log de ander uit: zijn kaartje wordt grijs ("offline"), hij blijft in de party.
- **Klik op een kaartje**: open de chat (T), klik op het kaartje. Het krijgt een gouden rand = jouw party-doelwit. Nog een keer klikken = weer uit.

#### 2. Emotes
Druk **;** ingedrukt, kies er een. Of typ:
```
/emote zwaai
/emote knuffel
/emote dans
/emote juich
/emote zit
/emote slaap
/emote lach
/emote wijs
/emote hartje
/emote stop
```
- Kijk in derde persoon (F5) om jezelf te zien.
- **zwaai**: rechterarm omhoog en zwaaien, groene sterretjes, belletje.
- **knuffel**: armen naar voren om iemand heen. Staat er iemand binnen 3,5 blok: **die knuffelt mee**, er komen hartjes tussen jullie, en hij krijgt "<naam> knuffelt je!". Niemand in de buurt: "je knuffelt de lucht".
- **dans**: armen en benen op de maat, heen en weer wiegen, muzieknootjes. Blijft doorgaan tot je loopt.
- **juich**: twee armen omhoog, huppeltjes, totem-sterretjes.
- **zit**: je gaat op de grond zitten (benen vooruit). Blijft tot je loopt.
- **slaap**: je gaat op je rug liggen, af en toe een snurkje. Blijft tot je loopt.
- **lach**: hoofd achterover, buik vasthouden, schudden.
- **wijs**: rechterarm wijst waar je kijkt, een spoortje vonkjes.
- **hartje**: armen boven je hoofd in een hartje, hartjes erboven.
- Een tweede speler ziet het ook (ook als hij later aan komt lopen terwijl jij danst/zit/slaapt).
- Snel achter elkaar spammen: "Even wachten...". Op een rijdier / vliegend / zwemmend: "Dat kan nu even niet."
- Een andere speler laten doen (test): `/guhs vadsrijk hud emote dans <speler>`

#### 3. HUD-bewerker
Openen (in het Vadsrijk):
- **Esc** → knop **"HUD aanpassen"** linksboven, of Opties → zelfde knop linksboven;
- of typ `/hudbewerker` (werkt altijd, ook zonder op);
- of `/guhs vadsrijk hud bewerker`.

Wat je ziet: elk HUD-deel heeft een doosje met zijn naam (Vads- en energiebalk, Ervaringsbalk, Vaardigheden, Level/munten, Baasbalk, Tips, Party, Buffs en debuffs, Doelwit, Jouw spreukbalk, Spreukbalk van de baas, Drankjesriem, Questvolger, Baas laadt een aanval, Munten erbij, Rijdier, Wind (zeilbootje)). De party-kaartjes laten in de bewerker een voorbeeld zien.
- **Slepen** = verplaatsen. Hij klikt vast aan de randen en het midden van het scherm, aan andere doosjes en aan een rooster van 4. **Shift** ingedrukt = vrij slepen.
- **Scrollen** op een doosje = groter/kleiner (50% tot 200%; onderaan zie je het percentage).
- **Rechtsklik** (of **R**) op een doosje = dat deel terug naar normaal.
- **Alles terug** (2x klikken) = alles terug. **Klaar** of Esc = opslaan.
- Check: sleep de Vaardigheden naar links, scroll ze groter, druk Klaar. Ze staan daar in het spel. Open je inventaris (E) en ga met de muis op een vaardigheid staan: de uitleg komt op de **nieuwe** plek.
- Check: de buff-rij, het doelwit-kaartje, de drankjesriem en de questvolger verplaatsen ook (sla een slijmpje voor een buff/doelwit).
- Check GUI-schaal 2, 3 en 4 (Opties → Video → GUI-schaal): niks valt buiten beeld; verplaatste delen blijven binnen het scherm.
- Opgeslagen per computer in `config/guhs-vadsrijk-hud.json` (ook na herstarten nog zo).
## SPIEK U-AUDIO2: bazenmuziek met fases, fanfares, wereldgeluiden, NPC-stemmetjes

Besluit V4 (feedbackronde 4):
- eigen bazenmuziek met fases + een eigen overwinningsfanfare;
- een eigen thema voor de Grauwe Droom;
- wereldgeluiden SUBTIEL (les: de wind in de havenstadjes was te aanwezig);
- NPC-stemmetjes met de bestaande guh-geluiden, hier en daar een pieppiepmuisje of blub.

> Smaak gaat voor. Klinkt iets niet goed? Zeg welk stuk en wat eraan schort ("te druk", "ander instrument", "fanfare te lang", "bijen te hard"). Muziek = één regel in `tools/vadsrijk/geluid/stukken.py`; volumes van de wereldgeluiden = één tabel in `WereldGeluiden.java`.

### Snel alles horen
| Wat | Commando |
|---|---|
| Klikbare lijst van ALLES (klik = luisteren) | `/guhs vadsrijk geluid` |
| Baasthema + na 12 s de laatste fase erin | `/guhs vadsrijk geluid fase grauwe_droom` |
| De fanfare van een baas | `/guhs vadsrijk geluid fanfare zeeslurf` |
| Een NPC-stemmetje | `/guhs vadsrijk geluid stem groet` |
| Een wereldgeluid (4 blokken voor je) | `/guhs vadsrijk geluid wereld bijen` |
| Wat hoort er hier (wereldgeluiden) | `/guhs vadsrijk geluid wereldinfo` |
| Alles stil (ook een baas-voorbeeld) | `/guhs vadsrijk geluid stop` |

### 1. Bazenmuziek met fases
Elke baas heeft nu een **eigen thema** en een **laatste-fase-versie**:
- Het is hetzelfde stuk, noot voor noot even lang.
- Er komen drums, pauken, een hoog strijkers-ostinato, koor en de melodie een octaaf hoger op koper bij.
- Bij de overgang naar de laatste fase loopt de muziek in ~2 s naadloos over (crossfade op dezelfde plek in het stuk).

Testen zonder gevecht:
1. `/guhs vadsrijk geluid fase vahoegnir`
   → Het gewone thema start. Na **12 s** zwelt de laatste fase erin, zonder hapering of sprong.
2. `/guhs vadsrijk geluid stop` → Stil.
3. Doe hetzelfde met een paar andere bazen: `/guhs vadsrijk geluid fase gorgonzola`, `... fase mika_imker`.

Nieuwe thema's (die bazen leenden eerst een thema of kregen "algemeen"):
| Baas | Commando | Karakter |
|---|---|---|
| De Mika-imker (zone 3) | `/guhs vadsrijk geluid fase mika_imker` | zoemend strijkers-ostinato (bijen!), fagot + klarinet |
| **De Grauwe Droom** (zone 5) | `/guhs vadsrijk geluid fase grauwe_droom` | dromerig **slaapliedje-baasthema**: speeldoos-wals, celesta, glaspad. Laatste fase: donker koor, taiko, tremolo |
| Donderbuik (zone 5) | `/guhs vadsrijk geluid fase donderbuik` | knorrig en zwaar: tuba + hoorn, taiko-donder, kalimba-regendruppels |
| Opzichter Gorgonzola (zone 5) | `/guhs vadsrijk geluid fase gorgonzola` | knorrig-komische ketelkamer-polka: fagot + trombone, houtblok |
| Kootje Zilverstaart (zone 5) | `/guhs vadsrijk geluid fase zilverstaart` | sluwe tango: viool + accordeon, staccato strijkers |

De zone 1-4-bazen houden hun thema (Zeeslurf, Vliegervanger enz.) en krijgen er een laatste fase + fanfare bij.

In een echt gevecht:
1. Ga naar een baas, bijvoorbeeld de Zeeslurf of de Grauwe Droom.
2. Vecht tot de LAATSTE fase. Dat is de laatste fasekaart, bij de Grauwe Droom fase 3.
   → De muziek zwelt dan aan.
3. `/guhs vadsrijk geluid info` → Achter de baas staat dan "(laatste fase)".

### 2. Eigen overwinningsfanfare
Een verslagen baas speelt nu zijn **eigen fanfare** (~5 s), in plaats van de gedeelde stinger.
- Het is zijn eigen toonsoort in majeur (hij is opgevrolijkt!), met zijn eigen hoofdinstrument en een eigen loopje.
- De Grauwe Droom krijgt een zacht speeldoos-slotje.
- De baasmuziek maakt meteen plaats.

| Testen | Commando |
|---|---|
| Lijst, klik door de fanfares | `/guhs vadsrijk geluid` (regel "fanfare:") |
| Eén fanfare | `/guhs vadsrijk geluid fanfare grauwe_droom` |

De fanfare volgt de schuif **stingers**.

### 3. Wereldgeluiden (SUBTIEL)
Het zijn zachte geluidjes van plekken vlak bij je. Er klinken er hooguit 4 tegelijk mee, er komt hooguit één geluidje per ~2-4 s, en elk geluidje heeft zijn eigen lange pauze. Op ~12 blokken hoor je ze niet meer.

| Bron | Wanneer | Voorbeeld |
|---|---|---|
| fonteintje | een KLEIN watervalletje (1-3 blokken): fonteinen, spuwers. Grote watervallen hadden al hun eigen ruis (die is niet verdubbeld). | `/guhs vadsrijk geluid wereld fontein` |
| bijen | bijenkorf / bijennest | `... wereld bijen` |
| smidse | aambeeld / smeedtafel: 2-4 zachte hamertikjes | `... wereld smidse` |
| markt | 3+ NPC-guhs dicht bij elkaar: zacht guh-geroezemoes | `... wereld markt` |
| windgong | klok / windgong-blok | `... wereld windgong` |
| kampvuur | brandend kampvuur: een zacht extra knisperen | `... wereld kampvuur` |
| kraak | gondelstations (Kandijkom) en liften (zone 3) | `... wereld kraak` |
| grot-echo | onder de grond lopen: je voetstappen krijgen een zachte echo | ga een grot in en loop |

Testen:
1. Ga naar Honingtak (bijen, liften) of de Kandijkom-gondel. Typ `/guhs vadsrijk geluid wereldinfo`.
   → Je ziet wat er gevonden is en wat je nu hoort.
2. Luister een minuut.
   → Het moet zacht en spaarzaam zijn: af en toe een geluidje, nooit een muur van geluid.
3. Loop een grot in.
   → Je stappen krijgen een zachte echo. Bij sluipen is er geen echo.
4. Zet de schuif **wereldgeluiden** op 0.
   → Alles hierboven is weg. Het gewone omgevingsgeluid blijft.

### 4. NPC-stemmetjes
Ze gebruiken de BESTAANDE guh-geluiden van de Guhs-mod. Elke NPC heeft zijn eigen toonhoogte (op naam), dus dezelfde NPC klinkt altijd hetzelfde.
- **Klik op een NPC** of open een gesprek → een "guh!" (groet).
- Een pagina die iets **vraagt** (?) → een nieuwsgierig "oeh?".
- Een **blije** pagina (bedankje, quest ingeleverd) → een giechel.
- Mika-sprekers → Mika-geluidjes. De verteller en je eigen regels zijn stil.
- Af en toe hoor je een **pieppiepmuisje** of een **blub**. Aan de kust van zone 4 hoor je vaker blub.

Testen:
1. Klik in Zwamzoet op een paar NPC's.
   → Elke NPC zegt "guh" in zijn eigen toonhoogte, en maar één keer per klik (niet dubbel bij het openen van het gesprek).
2. Blader door een gesprek.
   → Bij een vraag een "oeh?", bij een blije pagina een giechel. Het typ-getik blijft zoals het was.
3. Losse geluidjes: `/guhs vadsrijk geluid stem groet`, `... stem vraag`, `... stem dank`, `... stem mika`, `... stem piep`, `... stem blub`.
4. Zet de schuif **stemmen** op 0.
   → Geen stemmetjes meer. Het typ-getik heeft zijn eigen schuif (tandwiel in het praatscherm).

### Schuifjes
**Mods → Guhs → Config** (`guhs-vadsrijk-geluid.toml`). De oude schuiven blijven; er zijn er twee bij:
- **wereldgeluiden** (standaard 60). Volgt ook de schuif **Omgeving**.
- **stemmen** (standaard 70). Volgt ook de schuif **Vriendelijke wezens**.

Bazen-fases volgen **bazenmuziek**, fanfares volgen **stingers**.
## SPIEK u-ambacht2: de Ambachten groeien mee met zone 4 en 5

Alles in het Vadsrijk, met op-rechten. Het Ambachtenboek open je met **Y**.

### 1. Het plafond is nu 60
```
/guhs vadsrijk ambacht level alle 60
/guhs vadsrijk ambacht boek
```
- Elke tegel zegt "Niveau 60 / 60" en rechtsboven paars **Meester**.
- In de chat: "Je bent nu Meester in ..." (9 keer) en +20 Knutselpenningen per ambacht.
- Open de Kledingkast (**O**), tab capes: de **Meesterschort** staat erin.
- Titels: "Meester-kok" enz. en "Grootmeester der Ambachten".

### 2. Fix: zone 4 in het Plekken-overzicht
```
/guhs vadsrijk ambacht boek plekken
```
- Zes blokjes: zone 1-3 boven, zone 4-5 onder.
- Zone 4 heeft een regel "Vissen: 0/7" (5 visplekken + 2 duikplekken).
- Onderaan staan nu 15 vissen en 7 honingsoorten.
- Ook in het Verkenningsboek (**B**), pagina Ambachten: "Plekken zone 4" en "Plekken zone 5".

### 3. Zone 4 verzamelen (T4, niveau 50)
```
/guhs vadsrijk ambacht level alle 50
/guhs vadsrijk ambacht tp z4_pluk_bloemeneiland
/guhs vadsrijk ambacht plekken
```
- Je ziet varentjes (zeekraal en zeegaren). Rechtsklik en pluk.
- Probeer ook deze plekken:
  - `tp z4_hout_vliegereiland`: bamboeblokjes (drijfhout en vliegerbamboe);
  - `tp z4_mijn_pierkop`: goudklompjes (lampionbrons);
  - `tp z4_vis_haven`: waterlelies, vissen. 's Nachts maanrog, bij regen regenboogmakreel.
- Op niveau 49 zijn de plekken vaag en krijg je "niveau 50 nodig".

### 4. Koraalduiken (zone 4)
```
/guhs vadsrijk ambacht tp z4_duik_koraalbaai
```
- Spring in het water. Op de bodem (2-8 blokken diep) staan roze hersenkoraaltjes.
- Rechtsklik: "Duiken! Houd vast en laat los in het groen".
- Je krijgt roze koraal of een zeesponsje, soms een parelschelp. Het telt NIET als vis.
- Het water is nooit dieper dan 8 blokken, dus je komt altijd boven.

### 5. Zone 5 verzamelen (T5, niveau 55)
```
/guhs vadsrijk ambacht level alle 55
/guhs vadsrijk ambacht tp z5_pluk_staartpark
```
- Blauwe bloempjes: wolkbes en wolkenwol.
- Probeer ook deze plekken:
  - `tp z5_hout_staartpark_1`: wolkenhout;
  - `tp z5_mijn_noordvin`: witte calcietblokjes (hemelzilver) op de vin van Moeder Wolkwal;
  - `tp z5_vis_fontein`: vissen in de fontein op de Marktlaan (wolkenvis).

### 6. Bijen in zone 4 en 5
```
/guhs vadsrijk ambacht level imkeren 19
/guhs vadsrijk ambacht tp z4_korf_bloemeneiland
```
- Klik op een korfplaats: "niveau 20 nodig".
- `level imkeren 20`, nog eens klikken: je korf staat (lampionhoning).
- Zet de raten vol met `/guhs vadsrijk ambacht vul` en klik weer: oranje lampionhoning.
- Zone 5 (`tp z5_korf_staartpark`) heeft Imkeren 35 nodig. Op 55 mag je 5 korven.

### 7. T4/T5 maken: alleen in het Ambachtsgilde van Hoogvads
```
/guhs vadsrijk ambacht level alle 60
/guhs vadsrijk ambacht geef tier 5
/guhs vadsrijk ambacht geef tier 4
```
- Ga naar een smid in Zwamzoet (`/guhs vadsrijk ambacht tpnpc amb_smid_z1`) → Werkplaats.
  - T4/T5-recepten staan erbij, met een rode regel "Alleen in het Ambachtsgilde van Hoogvads".
  - Proberen te maken geeft dezelfde melding.
- In Hoogvads, in het Gildehuis: Leermeester Smeden → Werkplaats. Maak een "Hemelzilveren Zwaard".
  - Het is Episch, niveau 56 en je kiest 2 bonussen.
  - Een Wolkkern-Meesterring (Meesterwerk) kan hier ook.
- De werkplaats-guh in Lampionhaven (rol "werkplaats") maakt T4 wel, T5 niet.
- Nieuw eten:
  - zone 4: Zeekraalsalade, Koraalvissoep, Zeemanshapje, Lampionspiesjes;
  - zone 5: Wolkbessentaart, Donderstoofpot, Hemelfeestmaal.
- Nieuwe drankjes: Zee-/Wolken-Vadsdrank en -Energiedrank, en het Hemelelixer.
- Vliegerbouw: Naaien → "Guh-vlieger (naar huis)". Hij komt met de Post thuis.
- Snel testen zonder lopen: `/guhs vadsrijk ambacht werk smeden`. Dat werkt overal; het is de dev-werkplaats.

### 8. Het verhaal gaat verder in het Gilde (stap 6 en 7)
```
/guhs vadsrijk ambacht verhaal koken 5
/guhs vadsrijk ambacht level koken 55
```
- Praat met de Leermeester Koken in het Gildehuis → "Vertel eens..." → Oma Truus' bladzijde uit de Koraalbaai.
- Daarna `level koken 60`, nog eens "Vertel eens..." → het koekje met wolkbessen.
- De Gildemeester vertelt de verhalen van Mijnen, Houthakken, Plukken en Vissen.
- De oude leermeester in Zwamzoet zegt na stap 5: "Ga naar het Ambachtsgilde in Hoogvads".
- Snel: `/guhs vadsrijk ambacht gilde vissen`. Dat zet stap 5 klaar en vertelt de Gilde-stap meteen; wel eerst het level goed zetten.

### 9. Knutselpenningen en de Knutselwinkel
```
/guhs vadsrijk ambacht penningen 100
/guhs vadsrijk ambacht winkel
```
- Tab "Knutselwinkel":
  - kernsplinter, glinstergruis, oerbast, gouden klaver, parelschelp, koninginnengelei;
  - Uitrustthee (+30 min uitgerust);
  - de 5 bazenkernen en de Ambachtskist;
  - deco naar huis.
- Koop iets: de penningen gaan omlaag en het spul komt in je tas (materialen in de materiaaltas, deco met de Post).
- Penningen verdien je zo: een bestelling op het Bestelbord (+2 tot +6), de weekkist (+10), een spelersopdracht (+3), een verhaalstap (+5), Meester worden (+20).

### 10. Het Opdrachtenbord (spelers vragen, makers leveren)
```
/guhs vadsrijk ambacht opdracht
```
- Rechts: "Nieuwe opdracht".
  - Kies met < > een recept (eten, drankje, Maatwerk, bazenkern), het aantal met - + en de beloning met - +.
  - Klik "Ophangen": je munten gaan meteen in de pot.
- Links staat je opdracht roze, met "Intrekken". Klik "Intrekken": je krijgt precies je munten terug.
- Leveren alleen testen:
  ```
  /guhs vadsrijk ambacht opdracht nep klodderpudding
  /guhs vadsrijk ambacht geef tier 1
  /guhs vadsrijk ambacht werk koken
  ```
  - Maak in de keuken 1x Klodderpudding (geeft er 2). Open daarna weer `/guhs vadsrijk ambacht opdracht`.
  - "Lever" wordt roze zodra je er 2 hebt. Klik: je krijgt 500 brons + 3 penningen en de puddingen gaan weg.
- Met twee spelers:
  - A hangt een opdracht op, B levert.
  - A krijgt een chatbericht en klikt "Ophalen (1 klaar)".
- Ook via de bordguhs: Klerk Krabbel / Kriebel / Kraaltje, en nieuw Klerk Kwastje (Lampionhaven) en Klerk Kroontje (Hoogvads, bij het Gildehuis). Kies "Opdrachtenbord" of "Knutselwinkel".

### 11. Meesterschort zelf zien
```
/guhs vadsrijk ambacht reset
/guhs vadsrijk ambacht meester smeden
```
- Een echte level-up naar 60: paarse chatregel, geluidje, +20 penningen.
- Kledingkast → capes → Meesterschort: roze schortje met gouden rand, een zakje, een hamertje en een naald.

### Kijk vooral naar
- Zien de nieuwe plekken er goed uit (bamboe, varen, calciet, koraal) en staan ze nergens raar (niet op paden, niet in huizen)?
- Past het grotere Ambachtenboek op je scherm? Passen alle 7 tabs?
- Staan de nieuwe bordguhs op een fijne plek in Lampionhaven en bij het Gildehuis?
### Grotten (U-GROTTEN): in elke zone één sfeergrot

Elke zone heeft nu een handgebouwde grot met een eigen thema. In elke grot vind je:
- 2 soorten **ambachtplekjes**;
- 1 **elite** (komt na 5 minuten terug);
- een **puzzel**;
- een **schatkist** die pas opengaat als de puzzel is opgelost.

Het Verkenningsboek heeft per zone een categorie **Grotten** met 4 vinkjes per grot: ontdekt, elite, puzzel en schat.

**Let op: nieuwe wereld nodig.** De grotten zitten in de kaarttegels. Een al gegenereerd stuk wereld verandert niet, dus test in een verse wereld (of laat de chunks opnieuw genereren).

#### Snel rondkijken
```
/guhs vadsrijk grot lijst
/guhs vadsrijk grot tp zwamgrot
/guhs vadsrijk grot tp kristalgrot
/guhs vadsrijk grot tp wortelgrot
/guhs vadsrijk grot tp koraalgrot
/guhs vadsrijk grot tp wolkengrot
```
`tp <grot>` zet je bij de ingang (buiten). Met een plek erachter ga je direct naar binnen: `binnen`, `hal`, `puzzel`, `elite`, `schat`, `amb_1`, `amb_2` (en bij de koraalgrot ook `uitgang`). Bijvoorbeeld:
```
/guhs vadsrijk grot tp zwamgrot hal
```

#### Per grot: wat moet je zien?

**1. De Zwamgrot (zone 1, Sporenwoud, lv 11-13).**
- **Ingang:** een reuzenpaddenstoel. Hij is hol, met een deur naar het oosten en lantaarntjes ernaast. Een paadje loopt naar het bospad.
- **Binnen:** een wenteltrap rond een gloeiende steel naar beneden.
- **Zalen:** de Sporenhal, dan de grote Glimzaal. Daar staan gloeiende paddenstoelen, kleine reuzenzwammen, shroomlights in het plafond en zwevende sporen. Er zijn pluk- en mijnplekjes.
- **Puzzel:** de puzzelhal met "Het Echolied" (zingende stenen).
- **Elite:** het hol van de Oude Zwamwachter.
- **Schat:** een schatnisje met een kist op goud.

**2. De Kristalgrot (zone 2, in de Galmtop, lv 22-24).**
- **Ingang:** een kristallen poort in de westflank, met zielenlantaarns.
- **Zalen:** de Kristalhal (mijnplekjes), daarna een zaal met een **ijsmeer**. Het meer heeft een bevroren rand en een open midden: daar zijn de visstekjes.
- **Sfeer:** amethist- en kristalpieken en sneeuwsterretjes.
- **Puzzel:** "De IJsspiegels" (lichtstraal).
- **Elite:** de IJskoude Wacht.

**3. De Wortelgrot (zone 3, onder de Wortelvelden ten noorden van de stam, lv 44-46).**
- **Ingang:** een holle wortelstronk vlak naast de Wortelweg, met een wenteltrap naar beneden.
- **Wortelhal:** wortelpilaren (houthakplekjes), mijnplekjes, hangende wortels en gloeibessen.
- **Boomwortels:** de echte wortels van de Wereldboom die door het plafond steken, zijn blijven staan.
- **Puzzel:** "De Wortelknopen" (schuifpuzzel).
- **Elite:** de Grote Wortelslang.

**4. De Koraalgrot (zone 4, onder de oostrand van de Koraalbaai, lv 53-55).**
- **Ingang:** **duiken!** Op het water ten oosten van de atol drijft een vlotje met een lantaarn. Recht daaronder ligt een koraalboog op de zeebodem.
- **De gang:** volg de zeelantaarns en het zeegras. Om de ~8 blokken zit een **luchtbel** om adem te halen.
- **Koraalhal:** je komt boven in een poeltje (met zeeaugurken) in een droge hal, met nepkoraal en zeelantaarns. Daar zijn vis- en mijnplekjes.
- **Uitgang:** een wenteltrap omhoog naar een koraalschoorsteen op de rand van de atol. Dit is ook de weg naar binnen voor wie niet wil duiken.
- **Puzzel:** "Het Schelpenpad" (gekleurde platen).
- **Elite:** Koning Mopperkrab.

**5. De Wolkengrotten (zone 5, in de kop van Moeder Wolkwal, lv 58-60).**
- **Ingang:** het **Blaasgat** bovenop de walviskop, vóór de stadspoort, met een hekje eromheen en de opening naar het oosten.
- **Binnen:** een wenteltrap naar beneden.
- **Zalen:** de Wolkenhal (mijn- en plukplekjes) met wolkenpluimen, parelmoerlampen en witte asdeeltjes.
- **Puzzel:** "Het Wolkenslot" (symboolstenen).
- **Elite:** de Grauwe Wolkduivel, in een grijs wolkenhol.

#### Testen per onderdeel
- **Ontdekkingskaart.** De eerste keer dat je een grot inloopt, zie je eerst de normale gebiedstitel. Een tel later komt een gouden titelkaart (naam + "Grot ontdekt!"), met een belletje en een chatregel. Opnieuw zien:
  ```
  /guhs vadsrijk grot wis zwamgrot
  ```
  Of alleen de kaart tonen:
  ```
  /guhs vadsrijk grot kaart zwamgrot
  ```
- **Biome.** Druk binnen in de grot op F3. Je hoort "Het Vadsrijk - De Zwamgrot" enz. te zien. Op de grond erboven zie je weer het normale gebied.
- **Geluid.** Binnen speelt het grot-bed: druppels. Af en toe hoor je een druppel of kristal-ping met een zachte echo.
- **Elite.** De elite staat in zijn hol. Versla hem: je krijgt een chatregel, een klein buitje munten, en de eerste keer ook een zeldzaam stuk uitrusting. Direct spawnen (sta minstens 12 blokken weg):
  ```
  /guhs vadsrijk grot elite zwamgrot
  ```
- **Puzzel.**
  ```
  /guhs vadsrijk puzzel tp grot_zwamgrot
  ```
  Dat werkt ook met `grot_kristalgrot`, `grot_wortelgrot`, `grot_koraalgrot` en `grot_wolkengrot`. Snel oplossen:
  ```
  /guhs vadsrijk puzzel los grot_zwamgrot
  ```
- **Schat.** Loop naar de kist.
  - Zonder puzzel: "De schatkist zit op slot..." (plus een slotgeluidje).
  - Met puzzel: een gouden titel "Grotschat!", munten en een episch stuk uitrusting. Dit krijg je één keer.
  - Glinsteringen bij de kist laten zien dat er nog iets te halen valt.
- **Ambachten.** Toets Y > Plekken, of bekijk de plekjes in de hallen. Per grot zijn er 2 groepen, bijvoorbeeld in de Zwamgrot plukken + mijnen.
- **Boek.** Zie het Verkenningsboek > zone 1..5 > Grotten. Daar staan 4 regels per grot.

#### Wat je kunt melden
- **Uiterlijk:** hoe de ingangen eruitzien (zijn ze uitnodigend genoeg?), de lichthoeveelheid, en of de deeltjes te veel of te weinig zijn.
- **De duikgang van de Koraalgrot:** zitten de luchtbellen ver genoeg uit elkaar?
- **De wenteltrappen:** lopen ze lekker, zonder springen?
### Rijdende guhs (U-RUITERS)

Guhs op Pluimguhs rennen rond in zone 3 (en 2 in zone 1). Ze bestaan alleen in de buurt van spelers (max 14 tegelijk, binnen
80 blokken), komen altijd terug op dezelfde plek van hun route en worden nooit dubbel.

**Overzicht**
- `/guhs vadsrijk ruiters lijst`: alle routes, aantal ruiters, lengte, hoe lang een rondje duurt en hoeveel er nu zichtbaar zijn.
- `/guhs vadsrijk ruiters uit` en `/guhs vadsrijk ruiters aan`: alles weg / weer aan (bv. om FPS te vergelijken).

**Zone 3: kijken (tp zet je achter een ruiter, kijkend naar hem)**
- `/guhs vadsrijk ruiters tp z3_rijpad` (ook `tp z3_rijpad 1` en `2`): ruiters op Het Rijpad, de spiraal om de boom. Kijk: rennen ze soepel omhoog/omlaag over de halve treden, zonder haperen of door de reling te gaan?
- `/guhs vadsrijk ruiters tp z3_wenteltrap` (en `1`): de binnentrap in de stam, van de Schorsstad tot kruin 1. Huppelend tempo.
- `/guhs vadsrijk ruiters tp z3_honingtak` (en `1`): een rondje over het dek van Honingtak.
- `/guhs vadsrijk ruiters tp z3_tak_westtak`, `tp z3_tak_molenhoek`, `tp z3_tak_lianenbrug`: over de takpaden.
- `/guhs vadsrijk ruiters tp z3_pluimstal`: bij de Pluimstal: één ruiter wacht bij de deur, bij de keuzewei wordt een Pluimguh geborsteld (hartjes/sterretjes) door een guh met tuinschortje. (`tp z3_pluimstal 1` = die bij de wei.)
- `/guhs vadsrijk ruiters tp z3_oefenbaan`: rondjes op de oefenbaan.
- `/guhs vadsrijk ruiters tp z3_wortelweg` (en `1`, `2`), `tp z3_stalweg`, `tp z3_schimmelpad`: verspreid over de grond onder de boom.
- Wat te checken: zit de guh mooi op het zadel (niet erin, niet zwevend)? Ren-animatie op vlak terrein, hup-animatie op trappen, stilstaan (idle) aan het eind van een route. Stofwolkjes bij de pootjes en af en toe een wolkje veertjes in de kleur van de Pluimguh. Verschillende kleuren Pluimguhs, patroontjes, belletjes/bloemenkransen, verschillende guhs met petjes/hoedjes/sjaaltjes.
- Klik op een ruiter (of zijn Pluimguh): een schattig zinnetje in de chat.

**Zone 1 (nieuwsgierig maken)**
- `/guhs vadsrijk ruiters tp z1_koerier`: Koerier Zoef (rugzakje, jockeypetje) rijdt van De Haven naar het plein van Zwamzoet en terug.
- `/guhs vadsrijk ruiters tp z1_hoofdweg`: een ruiter op de bosweg Zwamzoet -> Sporenwoud.
- Klik erop: "Ooit krijg jij er ook één!"

**Geen dubbelen / niet vastlopen**
- Vlieg weg (meer dan 100 blokken) en kom terug: dezelfde ruiters staan weer op hun route, nooit twee dezelfde naast elkaar.
- `/guhs vadsrijk ruiters lijst` → "zichtbaar" telt nooit meer dan het aantal ruiters per route.

**Demo**
- `/guhs vadsrijk ruiters spawn`: een demo-ruiter rijdt 2 minuten rondjes (straal 5) om je heen, met een willekeurige look. Doe dit op vlakke grond.
## SPIEK U-TAPIJT: het vliegend tapijt (zone 6, Zijdeland)

Het tapijt is een rijdier zoals de andere (N-scherm, X-toets, levels), maar het beweegt heel anders: het **zweeft en glijdt**.
Zone 6 bestaat nog niet, dus je krijgt het met een commando. Alles werkt overal in het Vadsrijk (niet in een baasarena).

### 1. Krijgen en meteen rijden
```
/guhs vadsrijk tapijt geef
/guhs vadsrijk tapijt rijd
```
- `geef`: het tapijt is van jou (met een roze kussentje en gouden kwastjes), plus een tip. Daarna: **N** → "Vliegend tapijt" → Kiezen,
  en dan **X** zoals elk rijdier.
- `rijd`: geeft het tapijt en zet je er meteen op, hier.
- Kijk: jij zit **in kleermakerszit** (benen gekruist) midden op het tapijt; het tapijt golft zachtjes, de kwastjes wapperen mee,
  op de vier hoeken staan guh-oortjes (roze binnenkant).

### 2. Het gevoel (dit is de kern, graag goed proberen)
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

### 3. Live afstellen (tunen)
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

### 4. Aankleden (N → Vliegend tapijt → Aankleden)
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

### 5. Overig
```
/guhs vadsrijk tapijt status
/guhs vadsrijk tapijt reset
```
- `status`: heb je het tapijt, level; terwijl je rijdt: topsnelheid, hoogte boven de grond, zweefdoel.
- `reset`: tapijt vergeten (om `geef` opnieuw te testen).

**Let op / graag beoordelen in de client:** de kantelrichting (anders `tune kanteling -3.5`), de kleermakerszit (benen), het kussen
(plek/grootte), de golf-animatie en de hoekoortjes. Dat heb ik niet in een client gezien.
## SPIEK U-GILDEN: de Knuffelbonden (guilds), de Bondenvallei en de kastelen

**Naam:** het heet **Knuffelbond** (kort: "bond"). "Gilde" botst met de Ambachtsgilde (met de Gildemeester) die al in Hoogvads staat.
Het dal heet de **Bondenvallei**. De dev-commando's heten wel `/guhs vadsrijk gilde ...`.

Voor spelers is alles via **`/bond`**: een klikbaar paneel in de chat (alle knoppen zijn commando's).
In het Vadsrijk staat er ook een **⚑-knopje** links van je inventaris (onder de twee opslag-knopjes), dat opent hetzelfde paneel.

### 0. Snel alles klaarzetten (dev)
```
/guhs vadsrijk gilde maak ROZE Roze Ridders
/bond
```
- Dit maakt de bond gratis aan, zonder level-eis en zonder dat je bij de klerk hoeft te staan.
- Je krijgt een kavel in de Bondenvallei met een **Wachttoren** (kasteel niveau 1).
- In de chat zie je: `[ROZE] Roze Ridders  lv 1  ||||....  0/1000`, plus de knoppen.
- Beweeg over de bondsnaam: je ziet de **banier** (roze met een witte bloem en een magenta rand).

### 1. Oprichten (alleen in Hoogvads, zone 5)
```
/guhs vadsrijk gilde klerk
```
- Je staat nu bij **Bondsklerk Zegeltje** in de kanselarij van het paleis. Praat met hem.
- Kies **"Ik wil een bond oprichten"**:
  - zit je al in een bond: "één bond per ridder";
  - is zone 4 nog niet klaar én ben je onder lv 56: "kom later terug";
  - anders krijg je in de chat een klikbare **[Bond oprichten]**: het commando `/bond maak TAG Naam` staat dan klaar.
- Het kost **1 goud**. De naam is 3-20 letters/cijfers/spaties/streepjes, de tag 2-4 letters/cijfers. Beide zijn uniek.
- Overal anders zegt `/bond maak` dat het alleen bij de Bondsklerk kan.

### 2. Lid worden (vanaf zone 1)
- **Uitnodigen:** `/bond uitnodig <speler>`, of in `/bond leden` de knop [Iemand uitnodigen].
  - De ander krijgt een melding met [Ja] [Nee].
  - Uitnodigen mag een leider, officier of lid. Een rekruut mag het niet.
- **Bondenbord:** in elke stad staat een **Bondenbord-guh** (Knuffelkamp, Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads), plus één op het valleiplein.
  - De guh verschijnt rond het plein zodra je in de buurt komt. Praat met hem en kies "Welke bonden zoeken leden?".
  - Je krijgt een lijst met [Word lid] (open bond) of [Aanmelden] (een officier keurt goed).
  - Testen met 1 speler kan zo: `/bond werving open`, dan met een tweede account `/bond bord`.
- **Aanmeldingen goedkeuren:** in `/bond leden` staan ze onderaan met [Ja] [Nee]. Officieren krijgen ook meteen een melding.
- Nieuwe leden zijn **rekruut**. Na 200 bijgedragen bondsxp worden ze vanzelf **lid**.
- Een bond heeft max 50 leden.

### 3. Rangen en rechten
| rang | wat mag het |
|---|---|
| leider | alles: banier, leiderschap overdragen (♛), opheffen |
| officier | uitnodigen, rekruten/leden eruit zetten, rekruut→lid, werving, kasteel bouwen, de hele kluis |
| lid | uitnodigen, uit kluisvak 1 pakken |
| rekruut | chatten, dingen IN de kluis leggen, bouwmateriaal geven |

- `/bond leden` toont iedereen (● online, ○ offline) met knoppen ▲ ▼ ✖ ♛, alleen als jouw rang dat mag.
- `/bond verlaat`: een leider moet eerst het leiderschap overdragen. De laatste die vertrekt heft de bond op.
- `/bond ophef`: alleen de leider, en alleen als de kluis leeg is (er raakt nooit iets kwijt). De kas en het bouwdepot gaan naar de leider.

### 4. Lidmaatschap is per KARAKTER
- Wissel van ridder (`/guhs vadsrijk karakter`): je andere ridder zit niet in de bond.
- Het ⟨TAG⟩ bij je naam verdwijnt dan, en `/bond` zegt "Je zit (met dit karakter) niet in een Knuffelbond".
- Zo kan een ander karakter in een andere bond.

### 5. Bondschat, naamplaatje, banier
- `/bc hallo allemaal` stuurt één regel naar de bond.
- `/bond chat` zet de bondschat-modus aan/uit: wat je typt gaat naar de bond. Begin een regel met `!` om één keer naar iedereen te praten.
- In het Vadsrijk staat **⟨ROZE⟩** achter je naam: boven je hoofd, in de chat en in de spelerslijst (Tab). Het staat in de kleur van de banier.
- **Banier ontwerpen:** `/bond banier`.
  - Grondkleur: ◀ ▶. Per laag (max 3): patroon ◀ ▶, kleur ◀ ▶, ✖ weg, [+ laag].
  - Alle banieren in jullie kasteel kleuren meteen mee, en het naamplaatje krijgt de grondkleur.

### 6. Bondsxp, levels, perks, weektaken
- Bondsxp komt vanzelf van leden:
  - wezen opgevrolijkt: +1 (elite +3);
  - baas: +40 (plus een trofee);
  - quest: +15;
  - level omhoog: +10;
  - ambachts-level: +10;
  - 1 per 50 ambachts-xp.
  - Per lid max 600 per dag.
- **Snel testen:**
```
/guhs vadsrijk gilde level 6
/bond perks
```
  - Je ziet de perks met ✔ tot level 6. Het dev-commando zegt ook: bank-tabs 3, reiskorting 10%, teleport elke 15 min.
- **Perks** (gemak, GEEN stats):
  - Bondsrust (lv 2/6);
  - kluisvakken 1 t/m 5 (lv 1/3/5/7/9);
  - reiskorting op de luxe reiskaart-vlucht 5/10/15% (lv 4/6/8);
  - teleport naar het kasteel elke 30/15/10/5 min (lv 1/5/7/10).
- **Bondsrust testen:**
```
/guhs vadsrijk gilde rust
```
  - De rustpot vult zich alsof je 24 uur offline was.
  - Versla wat wezens: je krijgt +50% gevechts-xp. In `/bond` staat "Bondsrust: nog X xp".
- **Weektaken:** `/bond taken` toont er 3, met een balkje. Ze zijn nieuw elke maandag om 06:00.
  - `/guhs vadsrijk gilde week klaar`: alle drie klaar. Je ziet 3x "Weektaak klaar: +400 bondsxp en 10 zilver in de kas" en "Alle drie de weektaken klaar!".
  - `/guhs vadsrijk gilde week nieuw`: andere taken.
- **Reiskorting:** op bondslevel 4+ kost een luxe vlucht met de reiskaart (niet de eerste keer naar een plek) minder dan 4 zilver.

### 7. De Bondenvallei en het kasteel
```
/guhs vadsrijk gilde poort
```
- Je staat bij de **Bondenpoort** in het Staartpark van Hoogvads (het prieel), met een ring van lichtjes.
- Praat met **Poortwachter Wolkenpoort** en kies "Naar de Bondenvallei!".
- **Het valleiplein** is een zwevend roze eiland met een fontein, bankjes en lantaarns.
  - Noord staat de **poortboog** (paarse lichtjes): loop erdoorheen en je bent terug in Hoogvads.
  - **Pleinwachter Pluim** brengt je terug, of naar je kasteel. Er staat ook een Bondenbord.
- Ten zuiden van het plein liggen de **kavels** van de bonden: zwevende eilanden met kersenhouten vlonderwegen en lantaarnpalen.
  - Elk kavel heeft een bloeiende azalea-haag met een poortje.
  - Aan de open randen staan hekjes (verbonden), zodat je niet van het eiland valt.
  - Val je toch: een wolkje brengt je terug naar het plein.
- **Naar je kasteel:**
  - `/bond thuis` of [Naar ons kasteel] werkt vanuit **elke stad** en overal in de vallei. Wachttijd 30 min (korter met bondslevel).
  - Dev zonder wachttijd: `/guhs vadsrijk gilde tp`. Wachttijd wissen: `/guhs vadsrijk gilde tpklaar`.
- **In de Wachttoren (niveau 1):**
  - **teleportsteen** (lodestone) in het midden: rechtsklik → [Terug naar waar ik was] (gratis), [Valleiplein], [Knuffelkamp] [Zwamzoet] ... (alleen steden die je al mag bereizen; gebruikt de wachttijd);
  - **bondskluis** (de kisten tegen de westmuur);
  - een trap naar boven (elke verdieping heeft een trap met een reling) en een dakterras met kantelen, hoektorentjes en een vlaggenmast met jullie banier.
- **Uitbouwen:** `/bond kasteel` toont wat het volgende niveau nodig heeft: bondslevel, kas en bouwmaterialen van de Ambachten.
  - [Bouwmaterialen geven] neemt wat nodig is uit je inventaris en je materiaaltas.
  - [+1z] [+10z] [+1g] stopt munten in de kas.
  - [Bouwen!] mag een officier of de leider.
  - Snel testen:
```
/guhs vadsrijk gilde kasteel vol
/bond kasteel
```
  - Klik [Bouwen!]: het kasteel wordt opnieuw opgebouwd op het volgende niveau.
  - Of direct naar een niveau: `/guhs vadsrijk gilde kasteel 5`.
- **De niveaus:**
  1. **Wachttoren**: toren, teleportsteen, kluis.
  2. **Torenhof**: ringmuur met kantelen en steunpilaren, poortgebouw met twee torentjes met spitsen, en de **Trofeeënzaal** (lange hal met puntdak, 3 kroonluchters en 12 sokkels).
  3. **Burcht**: + twee hoektorens met spitsen, + westvleugel met **smidse** (smeedtafel, aambeeld) en **keuken** (rookoven). Rechtsklik opent de werkplaats van dat Ambacht.
  4. **Vesting**: + vier hoektorens, + oostvleugel met **naaiatelier** (weefgetouw) en **brouwerij** (brouwstandaard), bloembedden.
  5. **Knuffelslot**: een grote donjon (3 verdiepingen) met een troon, kroonluchters, banieren, een dakterras met een hoge torenspits en vier torentjes.
- **Trofeeën:** versla met een bondslid een baas, dan komt er een trofee bij (gouden beeldje + naam) in de Trofeeënzaal (niveau 2+).
  - Testen:
```
/guhs vadsrijk gilde kasteel 2
/guhs vadsrijk gilde trofee vadsrijk.bazen.koning_klodder
```
  - Je ziet de trofee in de hal, en in de chat "Nieuwe trofee ...".

### 8. De bondskluis (rechtsklik een kist in je kasteel)
- Je ziet een gewone kist van 6 rijen. De onderste rij zijn knoppen:
  - roze ruitjes = vakken, groen = het vak waar je nu in kijkt, grijs = op slot (met het bondslevel dat nodig is);
  - papiertje = wat jij mag;
  - trechter = sorteren (als je eruit mag pakken).
- Rekruut: alleen erin leggen. Lid: ook uit vak 1 pakken. Officier/leider: overal.
- Bundels, shulkers en gebonden spullen mogen er niet in.
- Kijken twee leden tegelijk in hetzelfde vak, dan zien ze elkaars zetten meteen.
- Wordt iemand uit de bond gezet, dan sluit zijn kluisscherm.
- Dev: `/guhs vadsrijk gilde bank` opent de kluis overal.

### 9. Ranglijst + website
- `/bond ranglijst` toont de top 10 (level, xp, leden, kasteel).
- Voor de site: `/guhs vadsrijk gilde export` schrijft `hiscores/bonden.json`, in het formaat van de andere hiscores.
  - Alleen als de live export aanstaat (`/guhs vadsrijk web nu`).

### 10. Overig dev
- `/guhs vadsrijk gilde info`: alle bonden met level, kavel, kasteel, kas en depot.
- `/guhs vadsrijk gilde xp <n>`: bondsxp erbij.
- `/guhs vadsrijk gilde uitnodig <speler>`: zet die speler direct in je bond.
- `/guhs vadsrijk gilde vallei`: naar het plein.
- `/guhs vadsrijk gilde kasteel bouw`: het huidige niveau opnieuw neerzetten.
- `/guhs vadsrijk gilde wis`: alle bonden vergeten (de blokken blijven staan).
## SPIEK U-HUIZEN: Het Knuffelkwartier (eigen huizen)

Eigen huizen als FF14-buurtwijk: een eigen dimensie met wijken van 24 kavels. Binnen in je huis en in je tuin mag je bouwen;
de rest van het Vadsrijk blijft onbebouwbaar.

### Zo kom je erin
- In ELKE hubstad staat een **Kwartiermeester**: een sneeuwguh met ridderhelm, cape en monocle, op 6 tot 13 blokken van het plein.
  - Zwamzoet, Kandijkom, Honingtak, Lampionhaven en Hoogvads (bij de marktfontein).
  - Hij verschijnt zodra je in de buurt komt.
- Klik op hem. Je krijgt de knoppen:
  - **Naar mijn huis**;
  - **Naar de buurt: een kavel kiezen** (of **Naar mijn wijk** als je al een huis hebt);
  - **Bij iemand op bezoek**;
  - **Hoe werkt het?**
- Snel testen:
  - `/guhs vadsrijk huis wijk`: naar de wijk met vrije kavels (wijk 1 wordt de eerste keer gebouwd, ~1 s);
  - `/guhs vadsrijk huis poorten`: toont waar de vijf hubs liggen en laat de Kwartiermeesters opnieuw plaatsen;
  - `/guhs vadsrijk indeling tp ...` werkt zoals altijd om bij een hub te komen.
- **Kijk:**
  - Je komt aan op het plein, vóór een roze poort, kijkend naar de fontein.
  - Je INVENTARIS is nu je gewone thuis-inventaris. Dat is de bedoeling: je richt in met je Guhs-deco van thuis.
- **Terug:** loop door de roze poort met het bordje "Terug naar de stad". Je staat weer waar je bij de Kwartiermeester was, met je
  Vadsrijk-tas terug.

### De wijk
- Rond het plein:
  - fontein, drie marktkraampjes (west), bankjes (oost), het prikbord (rechts van de poort) en lantaarnpalen;
  - vier straatjes met stoepjes en straatnaambordjes (Oostpluislaan, Zuidknuffelstraat, Westsnoepsteeg, Noordvadspad);
  - lantaarnpalen tussen de kavels, een heg aan het eind, en vier parkjes met een kersenboom.
- **Prikbord:** klik erop. Je ziet wie waar woont, hoeveel kavels er nog vrij zijn en hoeveel wijken er zijn.
- **Meubelmeester Mollie** staat in het middelste kraampje. Klik op haar:
  - stoel, tafel, bank, zitzak, kussen, vlaggetjes, taart, guhbloemen, lantaarns, tapijt, bed, bloempotten, kersenplanken,
    roze ramen, boekenkasten en het Vadsrijk-vaandel;
  - betalen gaat met Vadsmunten; het spul gaat meteen in je tas;
  - **Meer spulletjes »** bladert verder.
  - Munten nodig? `/guhs vadsrijk munten 50000` (kern). Level nodig voor een villa? `/guhs vadsrijk level 45`.
- **Vrije kavel:** roze hoekstenen en een bordje "Te koop! / Kavel N / Wijk N / (klik mij)" aan de straatkant.

### Een huis kopen
- Klik op het bordje "Te koop!". Makelaar Muis toont de prijzen:

  | Huis | Prijs | Vanaf level | Decoblokjes |
  |---|---|---|---|
  | Huisje | 15 zilver | 10 | 150 |
  | Huis | 60 zilver | 25 | 300 |
  | Villa | 2,5 goud | 45 | 500 |

- Kies een maat, daarna een kleurtje:
  - **Snoepje:** kersenroze;
  - **Muntje:** wit met mint, met de gevel naar de straat;
  - **Lavendel:** paars, met veranda en een rokend schoorsteentje.
- Het huis verschijnt meteen, met hartjes en blaadjes. Je staat voor je tuinhekje.
- Snel (gratis, dev):
  - `/guhs vadsrijk huis koop` = huisje Snoepje;
  - `/guhs vadsrijk huis koop villa 2` = villa Lavendel;
  - `/guhs vadsrijk huis koop huis 1` = huis Muntje.
- **Eén huis per ACCOUNT:** al je karakters wonen samen. Probeer een tweede kavel te kopen: Makelaar Muis zegt nee.
- **Kijk:**
  - alle hekjes zijn verbonden, met een hekpoortje en twee lantaarnpaaltjes bij het pad;
  - het pad loopt plat naar de voordeur;
  - binnen: een echte vloer en hangende lantaarns; bij huis en villa een ladder naar boven met een vloer;
  - de trofeeënplank achterin; in de tuin bloemetjes en een kersenboompje;
  - het stalletje (hooi, palen, drinkbakken) links achter, en de brievenbus (paaltje met tonnetje) naast het hekje.

### Inrichten (alleen in je eigen hekje)
- Zet blokken neer binnen je hekje: binnen, in de tuin, en tot 18 blokken hoog (ook op je dak).
- Weghalen kan alleen bij wat je zelf neerzette. Het huis zelf (muren, dak, hek) blijft staan:
  "Het huis zelf blijft staan: alleen wat je zelf neerzette kan weg."
- Wat NIET mag:
  - kisten/vaten/hoppers (alles met een inventaris), zand/grind, tnt, zuigers;
  - emmers, enderparels, spawn-eieren, harnasstandaards en lijstjes.
- Een buurmans kavel of de straat: niets plaatsen of breken.
- Deco-limiet: zie de tabel. Bij vol: "Je huis zit vol: maximaal N decoblokjes."

### De brievenbus (alles regelen)
- Klik op het tonnetje op het paaltje naast je hekje. Je ziet: post, deurstand, aantal vrienden, deco x van y, rust-xp en wat
  een groter huis kost. De knoppen:
  - **Post ophalen:** de cosmetica die je in het Vadsrijk kocht (de Post) en decoblokjes uit een vorig huis/kleurtje.
  - **Deur: volgende stand:** open voor iedereen → alleen vrienden → op slot (alleen jij) → open...
    - Bij "op slot" gaat de voordeur dicht.
    - Wie niet binnen mag, kan deuren, luiken en hekpoortjes niet openen.
    - Wie er toch in staat, wordt netjes naar buiten gezet.
  - **Vrienden:**
    - "Spelers naast mij worden vriend" (binnen 10 blokken);
    - "Vrienden mogen inrichten" aan/uit;
    - "Alle vrienden weghalen";
    - via chat: `/guhs vadsrijk huis vriend <naam>` en `/guhs vadsrijk huis ontvriend <naam>`.
  - **Groter huis:** je betaalt het verschil en het huis wordt verbouwd. Je decoblokjes gaan naar de brievenbus: haal ze op en zet
    ze opnieuw neer.
  - **Ander kleurtje:** gratis, ook hier gaat de deco even naar de brievenbus.
  - **Huis verkopen:** je krijgt de helft van wat je betaalde terug. De deco blijft bewaard voor je volgende huis.
  - **Terug naar de stad.**
- Een ander klikt op jouw brievenbus en ziet: "Dit is het huis van X (Villa). Deur: ... Jij bent hier bezoeker: kijken mag, aankomen niet."
- Chat-snelkoppeling voor de deur: `/guhs vadsrijk huis deur open|vrienden|slot`.

### Rijdier, huisdieren, trofeeën
- Je actieve **rijdier** staat stil in het stalletje (klikken of berijden kan niet).
- Tot 3 **huisdieren** dartelen in de tuin (je actieve eerst).
- Op de **trofeeënplank**: per verslagen baas een figuurtje met de naam erboven, van AL je karakters samen (wissel van karakter en
  kijk: de bazen van je andere ridder staan er ook). Koning Klodder, de Opperzwam en de Klodderreus
  krijgen hun eigen beeldje; de rest een gouden figuurtje.
- Dit wordt bijgewerkt zolang jij thuis bent. Bezoekers zien het daarna ook als jij weg bent.
- Testen:
  1. eerst in het Vadsrijk: `/guhs vadsrijk mount alle` en `/guhs vadsrijk huisdier alle` (+ `/guhs vadsrijk huisdier kies <id>`) (de bestaande dev-commando's);
  2. dan `/guhs vadsrijk huis tp` en kijk na ~2 s in de stal en de tuin;
  3. `/guhs vadsrijk huis figuren` haalt alles even weg; het komt vanzelf terug.

### Rust-xp thuis
- Log uit **binnen in je eigen huis** (niet in de tuin). Je komt uitgerust terug:
  - per uur weg 5% van een level aan rust-xp, maximaal 1,5 level;
  - zolang de pot vol is, krijg je dubbele xp;
  - de rust-xp hoort bij het karakter waarmee je weer inlogt.
- Snel: `/guhs vadsrijk huis rust 10` (alsof je 10 uur thuis sliep). Versla daarna een mob in het Vadsrijk: dubbele xp. Bij een lege
  pot: "Je rust-xp is op."

### Overige dev-commando's
- `/guhs vadsrijk huis tp`: naar je eigen huis (zonder huis: wijk 1).
- `/guhs vadsrijk huis wijk 2`: naar wijk 2 (wordt gebouwd als die er nog niet is). `/guhs vadsrijk huis wijk nieuw`: een nieuwe wijk.
- `/guhs vadsrijk huis bezoek <speler>`: naar het huis van een andere speler.
- `/guhs vadsrijk huis reset` (of `reset <speler>`): huis weg; de deco gaat naar de brievenbus van het account.
- `/guhs vadsrijk huis bouw`: je eigen huis opnieuw bouwen. `/guhs vadsrijk huis bouw wijk`: de hele wijk opnieuw bouwen, met de
  huizen en de deco naar de brievenbus.
- `/guhs vadsrijk huis groter`, `/guhs vadsrijk huis uiterlijk 0|1|2`, `/guhs vadsrijk huis post`.
- `/guhs vadsrijk huis info`: het aantal wijken en huizen, jouw kavel, deco, vrienden, deur, rust-xp en post.
- `/guhs vadsrijk huis terug`: terug naar de stad.

### Let op
- Buiten de wijk (meer dan 100 blokken van het plein) of naar beneden vallen: je staat weer op het plein.
- Niets doet pijn en honger bestaat hier niet.
- Een volle wijk: de volgende wijk wordt vanzelf gebouwd. Test dat met `/guhs vadsrijk huis wijk nieuw`.
## SPIEK U-GROTKERKER: elke grot is nu een herspeelbare mini-kerker

Elke zonegrot (De Zwamgrot, De Kristalgrot, De Wortelgrot, De Koraalgrot, De Wolkengrotten en, zodra zone 6 erin zit, De Zingende Zoutgrot) is een **run**:
grotkamer 1 (een groepje grotbewoners) → grotkamer 2 (een groter groepje + de elite) → de **grotpoort** gaat open → de **grotbaas** → een **eigen eindkist** voor iedereen die meer dan 5% meehielp. 10 minuten na de clear valt de grot weer in slaap en kun je opnieuw.

De puzzel, de eenmalige schat, de verzamelplekjes, de ontdekkingskaart en de boekjes van U-GROTTEN blijven gewoon zoals ze waren.

> Test in een **nieuwe wereld** (of met nieuwe tegels): de grotten zitten in de kaarttegels.
> Let op: het oude `/guhs vadsrijk grot reset <id>` (jouw ontdekt/elite/schat wissen) heet nu **`/guhs vadsrijk grot wis <id>`**. `reset` is nu de run.

### Snel overzicht

| Grot | Baas | Model | Kenmerkende aanval (vanaf de helft) | Zeldzaam huisdier | Zeldzame Pluimguh-kleur |
|---|---|---|---|---|---|
| `zwamgrot` (z1, lv 11-14) | Moeder Glimhoed | grote Sporenpriester | **Glimschuil**: alleen in de roze cirkels ben je veilig | Glimhoedje | Glimzwam |
| `kristalgrot` (z2, lv 22-25) | Tante Tinkelpoot | grote Kristalspin | **Kristalzang**: onderbreken (paars), anders heelt ze | Tinkeltje | Amethist |
| `wortelgrot` (z3, lv 44-48) | Ouwe Knoestbuik | grote Harsgolem | **Samen tillen!**: gele cirkel, samen erin staan (alleen = 60%) | Knoestje | Wortelhout |
| `koraalgrot` (z4, lv 53-56) | De Grote Zeurslak | grote Zeurslak | **Getij**: eerst de cirkel dichtbij, dan de ring (in-UIT) + Schelpzeur (onderbreken) | Zeurtje | Koraal |
| `wolkengrot` (z5, lv 58-60) | Opperpluis | reuze Grauwe Pluismijt | **Wolkenschuil**: eerst de ring, dan de cirkel (UIT-in) | Pluisje | Onweer |
| `zoutgrot` (z6, lv 64-67) | Koning Galmschaar | reuze Zoutkreeft | **Kristalschuil**: alleen de roze plekken zijn veilig | Galmpje | Zoutroze |

Op de helft van zijn Vads roept elke baas **2 grotbewoners** te hulp.

### 1. Een hele run (solo)

```
/guhs vadsrijk grot run zwamgrot
```
- Je staat in grotkamer 1. In de chat: "⚔ De grot wordt wakker! ...". Er staan **3** grotbewoners in kamer 1.
- Loop naar de grote zaal (kamer 2): **4** grotbewoners + **de elite** (Oude Zwamwachter).
- Loop naar de gang naar het elitehol: daar zit de **grotpoort** dicht (onzichtbare muur met roze glinsters + een bordje "Grotpoort - Versla eerst de grotbewoners: N over"). Tegen de poort aan lopen geeft "De grotpoort zit dicht...".
- Per lege kamer: "✔ Grotkamer 1 is leeg! (1/2)". Elite: "✔ De elite is verslagen!".
- Alles verslagen: "De grotpoort gaat open... de grotbaas wacht!" (deursgeluid), de poort is weg.
- Loop het elitehol in: **titelkaart** van de baas (de eerste keer ooit met camerazwaai), de baasbalk + **baasmuziek** (een geleend thema: Opperzwam / Spiegelzucht / Wortelwurger / Zeeslurf / Donderbuik / Zoutkolos).
- Kijk naar de aanvallen: roze/rode **vloervlekken** (decals) en de laadbalk onder de baasbalk ("Moeder Glimhoed laadt: Sporenkegel"). Op de helft: fase-moment + 2 helpers + de kenmerkende aanval.
- Baas verslagen: confetti, je **eigen eindkist** valt neer midden in het elitehol, titelkaart "Grot uitgespeeld! - 1 keer uitgespeeld", chat "★ Moeder Glimhoed verslagen! Je eindkist staat klaar." en "De grot slaapt over 10:00 weer in".
- Het bordje bij de poort telt af: "Open! Volgende run over 9:59".

Elke grot even proberen (snel naar binnen):
```
/guhs vadsrijk grot run kristalgrot
/guhs vadsrijk grot run wortelgrot
/guhs vadsrijk grot run koraalgrot
/guhs vadsrijk grot run wolkengrot
```

### 2. Meteen naar de baas (packs overslaan)

```
/guhs vadsrijk grot baas zwamgrot
/guhs vadsrijk grot tp zwamgrot elite
```
- De run start (als hij sliep), alle grotbewoners zijn weg, de poort is open, de baas staat in het elitehol.
- Zo kun je elke baas snel bekijken: vervang `zwamgrot` door `kristalgrot`, `wortelgrot`, `koraalgrot` of `wolkengrot`.
- Let op het **uiterlijk** (grootte, kleurtint), of de **aanvallen** passen in de kamer, en of de baas niet vast komt te zitten.

### 3. De eindkist en de buit

```
/guhs vadsrijk grot kist zwamgrot
```
- Er valt een eindkist voor je neer (2 blokken voor je). Open hem: munten (zone 1: 700 brons), **een uitrustingsstuk** op baas-level (de allereerste clear van een grot: Episch, daarna Zeldzaam met 12% kans op Episch), **3-5 ertsen** van de zone, soms (25%) de **zonekern**.
- Zeldzaam, direct gegeven (niet in de kist):
  - **Huisdier** (een mini-baas): 6% per clear, zeker na 25 keer mis.
  - **Pluimguh-kleur** (rijdier): 10% per speler per clear, zeker na 15 keer mis (de regel uit V-INT). Alleen als je al een Pluimguh hebt. De grotkleuren staan pas in het kleurenrijtje als je ze hebt.
- Pech-tellers bekijken/zetten:
```
/guhs vadsrijk loot kansen
/guhs vadsrijk loot pech grot_zwamgrot_pluim 15
/guhs vadsrijk grot kist zwamgrot
```
(de tweede kist geeft dan zeker de Glimzwam-Pluimguh als je een Pluimguh hebt)

### 4. Reset en opnieuw

```
/guhs vadsrijk grot run
```
- Lijst van alle grotkerkers: SLAAPT / BEZIG (nog x van y, spelers, poort dicht/open) / BAAS / KLAAR (reset over m:ss), en jouw clears per grot.
```
/guhs vadsrijk grot reset zwamgrot
```
- De grot valt meteen in slaap: alle grotbewoners en de baas weg, ongeopende eindkisten weg, poort open. Sta je nog in de grot, dan start er meteen een nieuwe run (nieuwe grotbewoners, poort weer dicht).
- Regels zonder commando:
  - **10 minuten** na een clear: de grot slaapt weer in (alleen als niemand meer achter de poort staat, anders wacht hij).
  - Een **onafgemaakte run** reset **1 minuut** nadat de laatste speler de grot uit is.
  - Een **uitgespeelde** grot wacht altijd de 10 minuten (ook als iedereen weggaat), zodat je niet kunt farmen door even naar buiten te lopen.

### 5. Met meer spelers (gedeelde wereld)

- Start je met 2 spelers in de grot: "(2 spelers: meer grotbewoners, een sterkere baas)", **4 + 5** grotbewoners, de elite krijgt 1 affix (3+ spelers: 2).
- De baas krijgt +75% Vads per extra speler die in de grot is op het moment dat hij wakker wordt.
- Komt er iemand bij tijdens een run: "⚔ Je doet mee met de run die al bezig is in De Zwamgrot." Er is nooit een tweede kopie; nieuwkomers doen gewoon mee.
- Een eindkist krijgt iedereen die **meer dan 5%** meehielp tegen de baas (schade, healen of tanken). Te weinig: "(Te weinig meegeholpen voor een eindkist...)" maar de clear telt wel.

### 6. Boek

- Verkenningsboek, zone 1-5, categorie **Grotten**: per grot nu 5 regels: ontdekt, elite, puzzel, schat en **"Grotbaas van de Zwamgrot verslagen"**.
- Zone 6 (als die erin zit): een eigen categorie "Grotten" met de Zingende Zoutgrot.

### 7. Oude commando's van U-GROTTEN (werken nog)

```
/guhs vadsrijk grot lijst
/guhs vadsrijk grot tp zwamgrot schat
/guhs vadsrijk grot elite zwamgrot
/guhs vadsrijk grot wis zwamgrot
/guhs vadsrijk grot kaart zwamgrot
```
- `elite` spawnt nu de elite van de run (de run start als de grot sliep).
- `wis` = jouw ontdekt/elite/schat van die grot vergeten (was `reset`).

### Wat je kunt melden
- Past de baas in de kamer (grootte, hitbox), loopt hij niet vast in de deco (paddenstoelen, stalagmieten)?
- Zijn de veilige plekken (roze) altijd bereikbaar, niet in een muur of meertje?
- Staan de grotbewoners niet in water of in de muur? (Ze zoeken zelf een vrije vloerplek in de kamer.)
- Is de poort echt dicht (niet ernaast langs te lopen) en gaat hij netjes open?
- Kleurtint en grootte van de baas: mooi of te gek? (Grootte/tint staan per baas in `GrotBazen.java`.)
- Het huisdiertje: goede grootte? Direct bekijken:
```
/guhs vadsrijk huisdier geef glimhoedje
/guhs vadsrijk huisdier kies glimhoedje
```
  (de andere: `tinkeltje`, `knoestje`, `zeurtje`, `pluisje`, `galmpje`)
## SPIEK U-LEVEN-STAD: leven in de guh-stadjes (zone 1-6)

Wat is er nieuw? In elk guh-stadje loopt nu van alles rond, maar alleen in de buurt van spelers (niets wordt opgeslagen).
- Pieppiepmuisjes die rondscharrelen, en lichtblauwe guh-klodders (~30%) die vrolijk rondstuiteren.
- Katten van de guhs op de daken, eendjesfamilies in vijvers en grachten, en vogeltjes die op lantaarns en bankjes landen.
- Guhs die hun huisdiertje uitlaten, een straatmuzikant, de ijskar (in Kandijkom: de soepkar), picknickende guhs en guhtjes die tikkertje doen.
- Terrasjes met kletsende guhs, met spraakwolkjes en roddels per zone.
- Een roze speeltuin in elke hoofdstad, waar kleine guhtjes spelen.

Het niveau stel je zelf in. Druk op **H**, ga naar **Instellingen** en kies bij **Stadsleven**: VOL, ZACHT of UIT.

### Snel naar een stad
Kopieer een van deze commando's:
```
/guhs vadsrijk leven tp knuffelkamp
/guhs vadsrijk leven tp zwamzoet
/guhs vadsrijk leven tp haven
/guhs vadsrijk leven tp kandijkom
/guhs vadsrijk leven tp honingtak
/guhs vadsrijk leven tp lampionhaven
/guhs vadsrijk leven tp hoogvads
/guhs vadsrijk leven tp saffraanpoort
/guhs vadsrijk leven tp rozenkloof
```
Zone 6 werkt pas als de zone-6 kaart erin zit.

Wat moet je zien? Loop een rondje en kijk na zo'n 10-20 seconden om je heen:
- **Muisjes en klodders.** Muisjes scharrelen in korte sprintjes rond en blijven dan even zitten om te eten of te piepen. Ongeveer 3 op de 10 diertjes zijn lichtblauwe klodders met een blij guh-gezichtje en oortjes. Die stuiteren.
- **Katten** zitten op daken. Ze hebben een roze halsbandje en draaien af en toe hun kopje. Klik op een kat: je krijgt hartjes en een tevreden "miauw".
- **Eendjes** zwemmen bij water: een moedereend met drie kuikentjes erachteraan, netjes op een rijtje.
- **Vogeltjes** landen op een lantaarn of op de leuning van een bankje. Ze pikken wat en vliegen dan door.
- **Wandelaars** lopen met hun huisdiertje erachteraan: een muisje, een konijntje, een blauwe klodder of een eendje. Af en toe aaien ze het diertje (hartje).
- **De straatmuzikant** speelt ukelele, met zachte belletjes- en harpnootjes en muzieknootjes boven zijn hoofd. Soms komt er een guh bij die gaat dansen.
- **De ijskar** (de fietsende ijscoguh) rijdt langzaam door de straten. Bij elke stop klingelt de bel en roept hij "IJsjes!". In Kandijkom is het een soepkar.
- **Picknick:** 2 of 3 guhs zitten op een roze-wit geruit kleedje op het gras, met een mandje. Ze eten (smakken) en kletsen.
- **Tikkertje:** 3 kleine guhtjes rennen achter elkaar aan. Wie getikt wordt, zegt "Tikkie! Jij bent 'm!" en giechelt, en telt dan even tot drie.
- **Terrasjes:** waar guh-bankjes staan, zitten 2-4 guhs te eten en te kletsen. Zijn er geen bankjes, dan staan er 2-3 guhs samen te kletsen op straat.
  - Je ziet **spraakwolkjes** (wit, tussen aanhalingstekens): "njeg!", gezellige praatjes en roddels van de zone.
  - Zone 1: de draak en de Keizer. Zone 2: kristallen. Zone 3: Graaf Piepvel. Zone 4: de Vliegervanger en de Knuffeltij. Zone 5: Moeder Wolkwal. Zone 6: de karavaan.
  - Er staan nooit meer dan 3 wolkjes tegelijk in een stad, en alleen als je dichtbij bent (binnen ~20 blokken).
- **Klik op een stadsguh:** in de chat zegt hij iets dat bij hem past: een kind, een wandelaar, de muzikant of een terrasguh.
- 's Nachts gaan de kinderen, de picknickers en de ijskar naar huis (als niemand kijkt). Katten, muisjes, vogels en de muzikant blijven.

### De speeltuinen
Er is één roze speeltuin per hoofdstad: Knuffelkamp, Zwamzoet, Kandijkom, Honingtak, Lampionhaven en Hoogvads. Saffraanpoort volgt zodra zone 6 in de kaart zit.
```
/guhs vadsrijk leven speeltuin knuffelkamp
/guhs vadsrijk leven speeltuin zwamzoet
/guhs vadsrijk leven speeltuin kandijkom
/guhs vadsrijk leven speeltuin honingtak
/guhs vadsrijk leven speeltuin lampionhaven
/guhs vadsrijk leven speeltuin hoogvads
```
Overdag. Is het nacht? Doe dan eerst `/time set day`.

Wat moet je zien?
- **De speeltuin zelf:** een roze-witte vloer met een kersenhouten hekje eromheen, met in het midden van elke kant een opening. Op de vier hoekpaaltjes staan lantaarntjes.
  - **Glijbaan:** een ladder, een roze torentje en een glijbaan van kersenhout.
  - **Wipwap:** een plank op een paaltje.
  - **Twee schommels** aan een balk.
  - **Draaimolen:** een roze schijf met handvatten en een parasolletje.
  - **Zandbak** en twee bankjes.
- **Kinderen (VOL: 9 guhtjes en een volwassene op het bankje):**
  - **Glijbaan:** ze lopen naar de ladder, klimmen omhoog, zitten bovenaan, glijden naar beneden ("Wiiiii!") en juichen even. Daarna doen ze het opnieuw.
  - **Wipwap:** twee guhtjes gaan om de beurt op en neer, en de plank kantelt mee.
  - **Schommels:** ze schommelen heen en weer, elk in een eigen ritme.
  - **Draaimolen:** twee guhtjes draaien rond, met hun gezicht naar het paaltje.
  - **Zandbak:** een guhtje zit te graven (zandkorreltjes).
  - Af en toe hoor je gegiechel en zie je een wolkje ("Hogerrr!", "Op en neer!").
- **Kijk ook of je er normaal kunt lopen:**
  - Je kunt de glijbaan op lopen (halve stapjes) of de ladder op klimmen.
  - De hekjes zijn **verbonden** en geen losse paaltjes.
  - Je komt er aan alle kanten in of uit.

### Instelling en FPS
1. Druk op **H**, ga naar **Instellingen** en kies bij **Stadsleven** voor VOL, ZACHT of UIT.
   - **ZACHT:** ongeveer de helft, alleen dichtbij te zien, en 5 kinderen in de speeltuin.
   - **UIT:** je ziet niets meer, en de server stuurt je ook niets meer.
2. Je kunt ook alles in één keer testen, voor iedereen tegelijk:
```
/guhs vadsrijk leven niveau zacht
/guhs vadsrijk leven niveau uit
/guhs vadsrijk leven niveau eigen
```
`eigen` zet het terug naar ieders eigen instelling.

### Dev-commando's
```
/guhs vadsrijk leven status
/guhs vadsrijk leven spawn diertje
/guhs vadsrijk leven spawn wandelaar
/guhs vadsrijk leven spawn kat
/guhs vadsrijk leven spawn eendjes
/guhs vadsrijk leven spawn vogel
/guhs vadsrijk leven spawn muzikant
/guhs vadsrijk leven spawn kar
/guhs vadsrijk leven spawn picknick
/guhs vadsrijk leven spawn tikkertje
/guhs vadsrijk leven spawn terras
/guhs vadsrijk leven spawn speeltuin
/guhs vadsrijk leven wis
/guhs vadsrijk leven uit
/guhs vadsrijk leven aan
```
- **status:** hoeveel scènes en entities er nu zijn, wat de limieten zijn, en in welke stad je staat.
- **spawn:** zet één scène vlak bij je neer, ook als de limiet al vol is. Dat lukt alleen in een stad.
  - Bij `kat` moet er een dak in de buurt zijn, bij `eendjes` water en bij `terras` guh-bankjes. Zijn die er niet, dan krijg je een kletsgroepje op straat.
- **wis:** haalt alles weg. Het komt daarna vanzelf terug.
- **uit / aan:** zet het stadsleven voor de hele server uit of weer aan.
## SPIEK U-LEVEN-RITME: dag en nacht in de steden, weer met charme, emote-reacties, het Pluizenblaadje

Alle commando's werken als op (permissieniveau 2), in het Vadsrijk. Handig om eerst naar een stad te gaan, bijvoorbeeld naar
Bakker Bram in Zwamzoet:
```
/guhs vadsrijk npc tp bakker_bram
```

Overzicht van wat er in de stad gebeurt waar je staat:
```
/guhs vadsrijk ritme status
```
Dit toont de stad, dag of nacht, hoeveel winkeliers in bed liggen, nachtbellen, bekende bedden, slapers, uilen, nachtmarkten en prikborden.

### 1. Dag- en nachtritme in de steden

Je hoeft niet op de avond te wachten. Zet het stadsritme op nacht, zonder de tijd te veranderen:
```
/guhs vadsrijk ritme nacht
```
Terug naar dag of naar de gewone klok:
```
/guhs vadsrijk ritme dag
/guhs vadsrijk ritme auto
```
Echt 's nachts gaat het ook vanzelf: tussen `/time set 13000` en `/time set 23000`.

**Winkels gaan dicht.** Welke winkeliers gaan slapen en welke blijven open (en waarom):
```
/guhs vadsrijk ritme winkels
```
Wat je ziet:
- Een winkelier wandelt naar het dichtstbijzijnde vrije bed (binnen 40 blokken) en gaat daar opgekruld slapen, met af en toe een klein "z"-vlokje.
- Op zijn plek staat een bordje **"Gesloten, morgen weer open!"**, met een belletje en "Klik voor de nachtbel".
- Klik op de bel: je hoort een belletje, er is een slaperig zinnetje ("Komt eraan... *gaap*") en de winkel gaat gewoon open.
- Klik je op de slapende winkelier zelf, dan wordt hij een beetje wakker ("Vooruit, omdat jij het bent...") en gaat de winkel ook open.
- Je zit dus nooit vast.

**Quests lopen altijd door.** Elke guh die in een quest voorkomt, blijft 24/7 op zijn plek. Dat geldt ook voor reisguhs en winkeliers
met extra keuzes. 's Nachts gapen ze af en toe. In `/guhs vadsrijk ritme winkels` staat bij hen "blijft 24/7".

**Slapende guhs in de huizen.** Loop 's nachts door een stad en kijk in de huizen: in een deel van de bedden ligt een guh te slapen,
soms met slaapmutsje of pyjama, met "z Z z" boven zijn hoofdje. Klik erop: hij snurkt zachtjes en slaapt verder. Bij de ochtend zijn
ze weg.

**Lichtjes bij schemering.** Bij zonsondergang gaan in alle steden de lichtjes aan, in een golf vanaf het plein: eerst een flitsje, de
hele nacht een zachte gloed en een hoornsignaal. Zwamzoet deed dit al; nu doen De Haven, Knuffelkamp, Kandijkom, Lampionhaven,
Hoogvads, Rozenkloof en Saffraanpoort ook mee. Honingtak houdt zijn eigen avondgloed. Dit volgt de echte klok:
```
/time set 12400
```
Of forceer de golf direct:
```
/guhs vadsrijk sfeer golf
```

**Nachtmarkt met lampionnen** in Zwamzoet, Kandijkom, Lampionhaven en Saffraanpoort. Zet hem meteen neer in de stad waar je staat:
```
/guhs vadsrijk ritme markt
```
Wat je ziet:
- Een ring van gloeiende lampionslingers boven het plein, roze, geel en mint.
- Een bord "✦ Nachtmarkt ✦".
- Twee kraamguhs:
  - **Lotje Lampion** verkoopt een lampionsnoepje (10 brons) voor een minuut Snelpootjes;
  - **Opa Sterrenkoek** verkoopt een sterrenkoekje (5 brons). Daar zit een grappige spreuk in en je krijgt er wat Vads van terug.

**Uilen en vuurvliegjes.** 's Nachts zitten in elke stad met een speler erin 3 guh-uiltjes op de daken. Ze vliegen rond en roepen
"Oehoe-njeg!". Rond je voeten dwarrelen vuurvliegjes. Vuurvliegjes uitzetten kan in `config/guhs-vadsrijk-ritme.toml`
(`vuurvliegjes = false`).

### 2. Weer met charme

Elke zone heeft zijn eigen weer, en elke speler ziet hetzelfde op hetzelfde moment:

| zone | wat |
|---|---|
| 1 | Zwamzoet: het roze buitje met regenboog dat er al was (`/guhs vadsrijk sfeer bui`) |
| 2 | Splinterpieken: een sneeuwbuitje met dikke vlokken, daarna een regenboog |
| 3 | Schemerwortel: bloesemregen, daarna een regenboog |
| 4 | Lampionkust: een lief regenbuitje met zacht regengeluid, daarna een regenboog; elke avond een warme lampiongloed in de mist |
| 5 | Hoogvads: altijd een paar wolkenslierten, en soms een hele optocht |
| 6 | Zijdeland: altijd een zacht stoffig briesje, en soms warme zandwervels |
| alle | om de dag ochtendmist rond zonsopgang |

Bekijk het meteen. Dit is alleen voor jou en gaat over de zone waar je nu staat:
```
/guhs vadsrijk ritme weer bui
/guhs vadsrijk ritme weer regenboog
/guhs vadsrijk ritme weer gloed
/guhs vadsrijk ritme weer mist
/guhs vadsrijk ritme weer stop
```
- `gloed` werkt alleen in zone 4.
- `bui` doet in zone 2 sneeuw, in zone 3 bloesem, in zone 4 regen, in zone 5 wolken en in zone 6 zand.

**Schuif:** Vadsrijk-menu (H) > Instellingen > **Weer met charme** (0 = uit, 100 = vol, standaard 70). Het weer volgt ook de
sfeer-stand: UIT zet alles uit, ZACHT maakt het minder.

### 3. Reacties op je emotes + de knuffelmeter

Ga bij een paar guhs staan in een stad en gebruik het emotewiel (`;` ingedrukt houden) of typ:
```
/emote zwaai
/emote knuffel
/emote dans
/emote juich
/emote lach
/emote hartje
/emote slaap
/emote wijs
```
Wat er moet gebeuren (bij maximaal 4 guhs binnen 7 blokken, een voor een):

| emote | reactie |
|---|---|
| zwaai | ze zwaaien terug |
| knuffel | de dichtstbijzijnde guh (binnen 3,5 blok) knuffelt terug, met hartjes en "X knuffelt je terug!"; de rest krijgt hartjes |
| dans | ze dansen mee |
| juich | ze juichen mee |
| lach | eentje rolt om van het lachen, de rest juicht |
| hartje | ze worden verlegen en er komen hartjes |
| slaap | ze gapen mee |
| wijs | ze kijken waar jij naar wijst |

**Knuffelmeter:**
- Elke stadsguh die je blij maakt telt een keer per dag, met een klein hartje boven zijn hoofd.
- Je ziet de stand in het Vadsrijk-menu (H) > **Karakter** (onderaan links: "Knuffelmeter: ... guhs blij gemaakt" plus de stand per stad), en
  op het Pluizenblaadje.
- Beloningen per stad:
  - 5 = 25 brons;
  - 15 = 100 brons;
  - 25 = 200 brons, en de eerste keer de titel **Knuffelvriend**.
- Beloningen voor alle steden samen:
  - 50 = een hartjesbril voor je guh, met de post (thuis af te halen);
  - 120 = de titel **Grote Knuffelaar** en 500 brons.

Handig bij het testen:
```
/guhs vadsrijk ritme knuffel
/guhs vadsrijk ritme knuffel zet zwamzoet 24
/guhs vadsrijk ritme knuffel reset
```
Zet je hem op 24, dan geeft de volgende blije guh de drempel van 25. Een `zet` zelf geeft geen beloningen.

### 4. Het Pluizenblaadje (krant + prikbord per stad)

Naast het questbord van elke stad staat een berkenhouten bord met daarboven bijvoorbeeld **"Het Zwamzoetse Pluizenblaadje"** en
"Nieuws & klusjes (klik!)". Dat staat in Zwamzoet, De Haven, Kandijkom, Honingtak, Lampionhaven, Hoogvads, Rozenkloof en
Saffraanpoort. Klik erop. Openen kan ook van overal in de stad:
```
/guhs vadsrijk ritme krant
```
Wat je leest, met "Verder »":
- de kop met de dag;
- een nieuwtje van de stad zelf;
- een algemeen nieuwtje met een echte burger erin;
- iets over gisteren: de knuffelkampioen van gisteren, of hoeveel klodders er zijn opgevrolijkt, of "er gebeurde niets";
- het weerbericht van vandaag. Dat klopt echt met het weer van de zone, inclusief de tijd en de ochtendmist van morgen;
- jouw knuffelmeter in deze stad;
- twee klusjes.

**Klusjes** (2 per stad per dag, voor iedereen dezelfde, de voortgang is per speler):
- **ZOEK**: "Oma Truus zoekt haar breinaald! Het ligt ergens bij ...".
  - Het verloren ding glinstert alleen voor jou, en dichtbij krijg je "Warm..." en "Heel warm!" te zien.
  - Ga erop staan, dan heb je het gevonden.
  - Lever het in bij de vrager of bij het bord.
- **BEZORG**: breng een pakketje naar een andere burger. Klik op die burger en je krijgt meteen je beloning.
- **HAAL**: haal iets op bij een burger en breng het terug naar de vrager of naar het bord.

Wat je krijgt per klusje: 20-50 brons en een knuffelpunt voor de stad.

Alleen burgers zonder questrol doen mee, dus een questklik wordt nooit overgenomen. De klusjes en het nieuws wisselen elke dag. Doe
alsof het morgen is:
```
/guhs vadsrijk ritme krant morgen
/guhs vadsrijk ritme krant vandaag
```

### Waar je op moet letten (voor jouw oordeel)
- Hoe het staat en ligt:
  - slapende guhs op het bed: ligt hij mooi op de matras?
  - de nachtbel en het bordje op de winkelplek;
  - de lampionring van de nachtmarkt: hangt hij niet in een dak?
  - het bord van het Pluizenblaadje naast het questbord.
- De hoeveelheid weer met de schuif op 70. Is het lief genoeg en niet te druk?
- Of de lichtjesgolf in de andere steden mooi aangaat. Daarvoor is `/time set 12400` en dan even kijken het beste.

### Gametests
```
runGameTestServer -Pgt=VadsrijkURitme
```
Er zijn 4 tests:
- `roosterBlokkeertNooitQuests`: geen enkele quest-guh gaat ooit naar bed, de nachtbel kan altijd iets openen, en een slapende winkelier opent toch zijn winkel;
- `knuffelmeter`: een keer per guh per dag, de beloningen en de knuffelreactie;
- `krantReset`: alle teksten bestaan in het Nederlands en het Engels, de klusjes zijn voor iedereen gelijk en wisselen per dag, en de voortgang reset;
- `stedenEnWeer`: de stadsvakken kloppen met de gebieden, de zones, en de weerklok.
## SPIEK U-GEROEZEMOES: guh-geroezemoes op drukke plekken

Wens: in Hoogvads en andere drukke plekken een levendig achtergrond-**geroezemoes**.
- Een bedje van gepraat, maar guh-achtig: vooral onverstaanbaar zacht guh-gekwebbel.
- Hier en daar een hoorbare "njeg", een giechel, een pieppiepmuisje of een blub.
- **Niet overdrijven** (les van de wind): het moet een prettige achtergrond blijven.

> Smaak gaat voor. Te hard, te zacht, te vaak een stemmetje? Zeg het, het zijn een paar getallen:
> - het volume van het bed: `BASIS` in `GeroezemoesSpeler.java`;
> - hoe vaak er een stemmetje klinkt: `pauze` in `Geroezemoes.java`;
> - hoe druk een plek is: de tabel `PLEKKEN` in `Geroezemoes.java`.

### Snel horen
| Wat | Commando |
|---|---|
| Klikbare lijst (onderaan: geroezemoes) | `/guhs vadsrijk geluid` |
| Wat hoort er nu: plek, aantal guhs, intensiteit, bedden | `/guhs vadsrijk geluid geroezemoes info` |
| Rustig bed vastzetten | `/guhs vadsrijk geluid geroezemoes rustig` |
| Drukke straat vastzetten | `/guhs vadsrijk geluid geroezemoes druk` |
| Volle markt vastzetten | `/guhs vadsrijk geluid geroezemoes markt` |
| Zelf een intensiteit kiezen (0-100) | `/guhs vadsrijk geluid geroezemoes kracht 40` |
| Terug naar automatisch | `/guhs vadsrijk geluid geroezemoes auto` |
| Eén stemmetje, 5 blokken voor je | `/guhs vadsrijk geluid geroezemoes njeg` (ook `lach`, `guh`, `oeh`, `piep`, `blub`) |

Vastzetten werkt overal, ook buiten het Vadsrijk, dus je kunt de drie bedden rustig na elkaar vergelijken. Daarna altijd `auto`.

### 1. De drie bedden
Ze zijn gemaakt van ALLEEN de bestaande guh-geluiden: geen nep-stemmen. Veel kleine guhs praten door elkaar, elk met een eigen toonhoogte en afstand, in zinnetjes met pauzes. De verre guhs klinken gedempt, dus je verstaat niemand.

1. `/guhs vadsrijk geluid geroezemoes rustig`
   → Een paar guhs, ver weg. Een stille straat of de avond.
2. `/guhs vadsrijk geluid geroezemoes druk`
   → Een drukke straat. Heel af en toe een "njeg" of piepje ín het bed.
3. `/guhs vadsrijk geluid geroezemoes markt`
   → Een vol marktplein. Ook een blub ergens.
4. Luister 40 s per bed.
   → Je hoort GEEN tik of sprong als het bed rondloopt (lussen van 30 s).
   → Het bed mag nooit boven de muziek of de stemmen uit komen.
5. `/guhs vadsrijk geluid geroezemoes auto`

### 2. Losse stemmetjes
- Af en toe klinkt één duidelijk stemmetje, bij een van de guhs om je heen (je hoort het van die kant komen):
  - een "njeg", een "guh!", een giechel of een "oeh?";
  - soms een pieppiepmuisje of een blub (aan de Lampionkust vaker een blub).
- Hoe vaak:
  - een rustige plek: ongeveer elke 25-40 s;
  - een volle markt: elke 8-14 s.
- Ze wachten op de pauze van de wereldgeluiden (bijen, smidse, ...), dus er stapelen nooit twee geluidjes op elkaar.

### 3. Hoe druk waar (automatisch)
Volgorde van druk naar rustig:
1. Hoogvads (de Marktlaan het drukst);
2. de bazaar van Saffraanpoort;
3. de markt van Kandijkom;
4. Zwamzoet;
5. de kade van Lampionhaven;
6. Honingtak;
7. het Rozenplein van Rozenkloof;
8. Knuffelkamp;
9. (De Haven van zone 1, nog iets rustiger.)

Hoe het werkt:
- Hoe meer guhs er om je heen staan (binnen 22 blokken: NPC's, de stadsbewoners van Hoogvads, ruiters), hoe drukker.
- Een stad wordt nooit helemaal stil.
- 's Nachts is alles veel stiller (40%).
- Buiten deze plekken: staan er 3+ guhs bij elkaar, dan hoor je een klein beetje geroezemoes (dat was vroeger het "marktgeroezemoes" van de wereldgeluiden; dat is nu hierin opgegaan).
- Het wisselen gaat geleidelijk (een paar seconden), nooit met een sprong.
- Tijdens een baasgevecht zakt het naar een kwart.

Teleporteren en luisteren (zeg overal `/guhs vadsrijk geluid geroezemoes info`):
```
/execute in guhs:vadsrijk run tp @s 2738 240 -510
```
→ De Marktlaan van Hoogvads: overdag "plek hoogvads (markt)". Het bed staat op of tegen "markt".
```
/execute in guhs:vadsrijk run tp @s 3762 72 -812
```
→ De bazaar van Saffraanpoort: druk. Het synthetische bazaar-bed van de zone loopt eronder door.
```
/guhs vadsrijk z2wereld plek plein
```
→ Kandijkom.
```
/guhs vadsrijk wereld plek plein
```
→ Zwamzoet.
```
/execute in guhs:vadsrijk run tp @s 1844 67 -460
```
→ Het havenplein van Lampionhaven.
```
/guhs vadsrijk z3wereld plek markt
```
→ Honingtak.
```
/execute in guhs:vadsrijk run tp @s 3400 71 -716
```
→ Het Rozenplein van Rozenkloof.
```
/guhs vadsrijk wereld plek kampvuur_kamp
```
→ Knuffelkamp: het rustigst van de rij.

Nacht testen:
```
/time set 18000
```
→ `info` zegt "nacht", de intensiteit is veel lager. Terug: `/time set 1000`.

### 4. De schuif
- Mods → Guhs → Config → **"Vadsrijk: geroezemoes"** (standaard 60).
- 0 = helemaal uit (ook de losse stemmetjes).
- Hij volgt ook Minecraft's schuif "Omgeving".
### Opslag-uitbreidingen + de VIP-grap (U-OPSLAGVIP)

Alles werkt alleen **in het Vadsrijk** (met je Vadsrijk-inventaris). Munten nodig? `/guhs vadsrijk munten 100000` (= 10 goud, in brons).

#### 1. Alleen de EERSTE gekochte uitbreiding is te koop
- `/guhs vadsrijk opslag knapzak reset` en `/guhs vadsrijk opslag bank tabs 0` → begin opnieuw (je spullen blijven bewaard).
- `/guhs vadsrijk opslag knapzak` (of toets **I**)
  - **Kijk naar** de flapjes boven het scherm:
    - **1** = open;
    - **"?"** = nog te halen (verhaal zone 1 / zone 2);
    - **"+"** (geel) = te koop;
    - **grijs flapje met een slotje** = "Binnenkort!".
  - Bij een nieuw karakter is **flapje 5** grijs met een slotje. Hover erover: **"Binnenkort!"** + "Deze uitbreiding komt in een latere update."
  - Koop het "+"-vak (25 zilver). Daarna is er niks meer te koop. Flapjes die je niet via het verhaal krijgt, blijven "Binnenkort!".
- `/guhs vadsrijk opslag bank` (alsof je bij de bankier staat)
  - Vak 2 = **"+"** voor **10 zilver**. Vak 3, 4 en 5 = grijs met een slotje, "Binnenkort!".
  - Koop vak 2: daarna staan vak 3-5 nog steeds op "Binnenkort!".
  - Heb je vroeger al meer bankvakken gekocht? Die hou je gewoon.

#### 2. De prijscurve (later zelf de getallen kiezen)
- `/guhs vadsrijk opslag prijzen` → in de chat staat de curve: uitbreiding 1-5 met de prijs voor knapzak en bank, en "te koop" of "Binnenkort!". Daaronder staat jouw stand.
- Prijs van de n-de uitbreiding = **basis × factor^n** (n = 0 voor de eerste). Standaard is dat knapzak 2500 × 4^n en bank 1000 × 4^n brons. De factor 4 is een **plaatshouder**: jij kiest later de echte getallen.
- Een tweede uitbreiding openzetten, zonder code:
  - `/guhs vadsrijk opslag prijzen max_koopbaar 2`
  - Open het scherm opnieuw: het volgende vak is nu "+" voor 4× de prijs (knapzak 1 goud, bank 40 zilver).
- De andere knoppen:
  - `/guhs vadsrijk opslag prijzen knapzak_factor 10`
  - `/guhs vadsrijk opslag prijzen bank_basis 2000`
  - De sleutels: `max_koopbaar`, `knapzak_basis`, `knapzak_factor`, `bank_basis`, `bank_factor`.
- Alles staat ook in `config/guhs-vadsrijk-balans.toml`, onder **[opslag]**. Een aangepast bestand wordt binnen 5 seconden opnieuw gelezen.
- Terugzetten: `/guhs vadsrijk opslag prijzen max_koopbaar 1`
- Snel testen hoeveel je gekocht hebt:
  - `/guhs vadsrijk opslag knapzak gekocht <0-4>`
  - `/guhs vadsrijk opslag bank tabs <0-4>`

#### 3. De grap: "WORD GUH VIP++" (een parodie, er wordt nergens echt betaald)
- Meteen zien: `/guhs vadsrijk opslag vipgrap`. Daarmee wis je de vlag en gaat de knapzak open met de grap erin.
- Of op de echte manier:
  - `/guhs vadsrijk opslag vipgrap reset`
  - Klik in de knapzak of de bank op een geel **"+"**-vak.
- **Wat moet er gebeuren** (de eerste ~10 seconden):
  - Het hele scherm wordt een schreeuwerige deal:
    - **WORD GUH VIP++** in grote letters die knipperen en pulseren;
    - **VAN ~~50 EURO~~ VOOR MAAR 34,99!!!** (de 50 euro is doorgestreept);
    - een gouden rand met knipperende lampjes, confetti en twinkelende sterretjes;
    - "🔥 BESTE DEAL 🔥"-badges in de hoeken;
    - **⏰ NOG MAAR 00:09!**, dat aftelt;
    - een grote groene **KOOP NU!!!**-knop met een scheve "97% KORTING"-sticker;
    - een dikke **rode pijl** die ernaar wijst en heen en weer wipt.
  - Je hoort een kort belletjes-deuntje en dan een "ka-ching".
  - **Klik op KOOP NU!!!** of op "Nee bedankt, ik betaal liever de volle prijs": het scherm **schudt** en er gebeurt niks ("Er gebeurt niks. Dat is de grap.").
  - Onderaan staat klein: "(Grapje! Guhs heeft geen echte betalingen. Nergens. Nooit.)"
  - **Esc** slaat de grap over.
- **Daarna** verschijnt een gewoon praatvenster: **Winkelguh Snoes** (een guh met een strikje en een strohoed). Die zegt: "Hihi, nee is cap. Je kan het gewoon met in-game munten kopen: de eerste uitbreiding kost 25 zilver." (Bij de bank staat er 10 zilver.)
  - **Kopen** (of toets 1/Enter) koopt het vak meteen met munten.
  - **Nee, dank je** (of toets 2/Esc) sluit het venster.
- **Maar één keer per account:**
  - Klik daarna nog eens op een "+"-vak, ook bij een ander karakter of bij de bank: geen grap meer, maar de gewone "Klik nog een keer"-bevestiging.
  - `/guhs vadsrijk opslag vipgrap stand` → in de chat staat "al gezien: ja/nee".
- De grap bij de bank testen: `/guhs vadsrijk opslag vipgrap reset`, dan `/guhs vadsrijk opslag bank` en klik op "+".

#### Gametests
- `runGameTestServer -Pgt=VadsrijkUOpslagVip` test twee dingen:
  - `alleenEersteKoopbaar`: alleen de eerste is te koop, de curve klopt, en een oude save telt als 1 gekocht;
  - `vipVlagEenKeerPerAccount`: de vlag gaat via de knop aan, geldt voor het hele account, en blijft na opslaan en laden.
### Questrobot (U-QUESTROBOT): alle quests van zone 1-6 automatisch laten spelen

De questrobot speelt elke hoofd- en zijquest van begin tot eind door de echte quest-motor (aannemen bij de gever, elke stap via
de haak die het spel zelf gebruikt, inleveren, de actie na het inleveren) en schrijft op waar iets vastloopt. Hij gebruikt een
**nep-speler** ("guhs_questrobot"): jouw eigen quests, level en spullen blijven precies zoals ze zijn.

**Alles laten spelen (in je testwereld, als op):**
```
/guhs vadsrijk questrobot alles
```
- In de chat: per zone hoeveel quests gehaald / vast / niet gestart, en elke zonevlag of luchtschip-poort die fout is.
- Het rapport staat daarna in `run/questrobot/robot.md` (kort) en `run/questrobot/robot.json` (alles).

**Alleen tot en met een zone (de eerdere zones speelt hij ook, die zijn nodig voor de vlaggen):**
```
/guhs vadsrijk questrobot zone 3
```

**Het grote rapport maken (op de pc, in de projectmap):**
```
python tools/vadsrijk/questrobot/rapport.py run/questrobot/robot.json
```
Dat schrijft `QUESTROBOT_RAPPORT.md`: een Nederlandse samenvatting per zone + per quest waar hij vastliep en waarom, plus de
statische controle (teksten nl/en, volgorde, acties, wie meldt een minigame) en of elke doelplek op de kaart staanbaar is.

Wat je ziet / wat er moet gebeuren:
- De robot is in een paar seconden klaar; je merkt er in de wereld niets van (geen NPC's die verdwijnen, geen teleport van jou).
- "gehaald" = echt gespeeld; "vast" = hier liep hij vast (daarna duwt hij de quest door zodat alles erachter ook getest wordt).
- Gevechten, minigames en vluchten speelt hij niet echt: hij meldt ze via dezelfde haak als het spel (zie "Robot-notities" in het rapport).
## SPIEK U-GROOTTE: kleinere mod + soepeler beeld

De mod-jar is ongeveer **190 MB kleiner**: de Vadsrijk-kaart (130 MB) en alle andere bouwwerken (70 MB) zitten nu samengeperst in
twee kleine pakbestanden (samen ~10 MB). De wereld die eruit komt is **precies hetzelfde**, blok voor blok.

### 1. Komt de kaart uit het pakbestand?
```
/guhs vadsrijk grootte status
```
- Groen: "Kaartpak gevonden: 1116 tegels, 937 verschillende. Tegel 0,0 geladen (… ms)."
- Oranje "NIET gevonden" betekent: de oude losse bestanden worden gebruikt (mag in een testomgeving, hoort niet in de echte jar).

### 2. Ziet de wereld er nog hetzelfde uit?
Maak een NIEUWE wereld (of ga naar een stuk Vadsrijk waar je nog nooit was) en loop rond:
```
/guhs vadsrijk
```
- Zwamzoet, de steiger bij aankomst, Hoogvads, de Lampionkust: alles staat er zoals altijd.
- Kisten, bordjes en andere blokken met inhoud zijn er ook nog.
- Andere bouwwerken buiten het Vadsrijk (kasteel, golfbaan, Guhmension-dingen) laden ook gewoon.

### 3. Soepeler beeld (FPS) in drukke plekken
Ga naar Hoogvads (veel bewoners) of een stad met veel NPC's en ruiters:
```
/guhs vadsrijk
```
- Bewoners/figuranten verder dan ~96 blokken worden niet meer getekend (de reuzen, zoals de vinnen van Moeder Wolkwal, wel).
- Kleding en patronen van guhs en rijdieren verder dan 64 blokken weg worden niet meer getekend (op die afstand is het een pixel).
- Dichtbij moet alles er precies hetzelfde uitzien als eerst.
- Druk **F3** en kijk naar de fps op dezelfde plek: die hoort hoger te zijn dan in de vorige versie.
- De spuitende Moeder Wolkwal is van ver iets minder wolkerig (de helft van de deeltjes), van dichtbij hetzelfde.

### Wat NIET veranderd is (jouw keuze)
- Geluid (117 MB) en plaatjes (172 MB) zijn niet aangepast: dat zou het geluid of het uiterlijk veranderen.
  Voorstel staat in NOTITIES_U_GROOTTE.md (lange muziek iets lager van kwaliteit, de grootste plaatjes half zo groot). Zeg het maar.
## SPIEK U-KRIMP: nog kleinere mod

De mod-jar is weer een flink stuk kleiner (334 MB -> 270 MB). Plaatjes zijn verliesvrij kleiner gemaakt (pixel voor pixel
hetzelfde), alleen de 46 allergrootste textures (1024x1024 en groter) zijn half zo groot, en de geluiden zijn iets compacter
opgeslagen (mono, ~48 kbps). Je hoort en ziet als het goed is **geen verschil**.

### 1. Luisteren: klinkt het nog hetzelfde?
Ga naar het Vadsrijk en luister naar muziek en sfeer:
```
/guhs vadsrijk
```
- De muziek in Zwamzoet en Hoogvads: geen gekraak, geen "onderwater"-geluid, even lang als altijd.
- Een baasgevecht (muziek van de baas) klinkt nog goed.
- Gewone guh-geluidjes (aaien, eten, pijn) en safari-dieren klinken zoals altijd.
- De Wolkenweide-sfeer en het kriebelhuis 's nachts: geen tikjes of ruis.

### 2. Kijken: de grote textures
Deze hadden de grootste plaatjes en zijn half zo groot gemaakt. Ze moeten er hetzelfde uitzien (niet wazig, geen randjes):
- de **draak** (zone 4),
- de **luchtbizon**,
- de **wolkwacht-ridder**,
- **Roquefort** (de baas).
Kijk ook even of gloeiende delen (ogen, gloed) nog op de goede plek zitten.

### 3. De rest
Alle andere plaatjes zijn precies hetzelfde gebleven (verliesvrij), daar hoef je niets voor te checken.
## Spiekbrief: de Vadsrijk-website (U-SITE)

### Online (verborgen voorbeeld, staat nergens gelinkt)
- Start: https://guhs.nl/vadsrijk-preview/
- Kaart (alle 5 zones): https://guhs.nl/vadsrijk-preview/kaart.html
- Zone 4: https://guhs.nl/vadsrijk-preview/zones/zone4.html · Zone 5: https://guhs.nl/vadsrijk-preview/zones/zone5.html
- Nieuwe systemen: https://guhs.nl/vadsrijk-preview/systemen/index.html (Ambachten, Uitrusting 2.0, Karakters, Kledingkast, Puzzels)
- Een baas van zone 4: https://guhs.nl/vadsrijk-preview/bazen/zeeslurf.html

**Kijk:**
- bovenaan een gele balk "Voorbeeld van Guhs 1.10.0 ... voorbeelddata";
- de server, spelers, highscores en live-tegels zijn NEP (voorbeelddata), dat staat er ook bij;
- rechtsboven "Toon tot zone 5": zet hem op zone 3 en zone 4/5 verdwijnen (ook van de kaart);
- de kaart: De Lampionkust (eilandjes) en Hoogvads (de walvis) rechts, met kampvuren, bazen, luchtschiplijnen C en D.

### Ronde 2: kijk ook naar
- Bazen: https://guhs.nl/vadsrijk-preview/bazen/index.html — alle zones dicht, klik er een open; elke baas heeft een plaatje.
  Een baaspagina (bijv. bazen/opperzwam.html): onderaan "Alle buit" met kansen (rijdier 10%, zeker na 15 keer).
- Wezens/Items: tabel met bladzijdes (onderaan 1 2 3 ...), klik op een kolomkop = sorteren, filters + zoeken.
- Quests: alle zones dicht, klik er een open.
- Verhaal: https://guhs.nl/vadsrijk-preview/verhaal.html — wereldkaart, tijdlijn, personages, hoofdstukken per zone.
- Kaart: laag "Spelers tonen (live)" rechtsboven; 3 nep-spelers + een luchtschip (voorbeelddata, beweegt niet).
- In het spel (mod): `/guhs vadsrijk kaart verberg` = jij staat niet meer op de kaart, `/guhs vadsrijk kaart toon` = weer wel.

### Lokaal
```
cd C:\Users\Juiced\Documents\MineScapeStory\guhs_vadsrijk\u-site
python tools/make_vadsrijk_site.py
python -m http.server 8000 -d site_build
```
Open http://localhost:8000/vadsrijk/

### Weghalen van guhs.nl
GitHub -> coecomber/guhs -> Actions -> "Wiki (GitHub Pages)" -> Run workflow (main). Daarna is /vadsrijk-preview/ weg (404);
de gewone site blijft precies hetzelfde. (Elke gewone push naar main doet dit ook vanzelf.)

## Uit de nachtronde: de systemen

## DIALOOG: het nieuwe Vadsrijk-praatscherm (T-DIALOOG)

Alle gesprekken **in het Vadsrijk** krijgen nu het nieuwe praatscherm. Buiten het Vadsrijk (Knuffeldal, Erfenis, de verhalen)
blijft het oude scherm precies zoals het was.

Test dit op **GUI-schaal 2, 3 en 4**:
- Opties > Video > GUI-schaal.
- Doe de demo op elke schaal één keer.

### D1. De demo: alles in één gesprek
```
/guhs vadsrijk dialoog demo
```
Dit werkt overal, ook buiten het Vadsrijk. Klik je door de 8 bladzijden heen.

**Bladzijde 1, de verteller:**
- Je ziet een hele perkamentpagina in schuine letters, zonder portret, met een boekje in de hoek.
- De tekst **typt zichzelf**, met een zacht piepje per woord.
- **Klik of spatie** = meteen de hele tekst. Nog een keer = de volgende bladzijde.

**Bladzijde 2, Oma Truus (blij):**
- Het portret **hopt** op en neer.
- Er staat een roze **♥** in de hoek.
- Het naambordje is **roze** (zone 1). Ernaast staat waar ze woont.

**Bladzijde 3, Poollicht Pien (verdrietig):**
- Het portret **zakt in** en kijkt naar beneden.
- Er staat een blauw **…** in de hoek.
- Het naambordje is **ijsblauw** (De Splinterpieken).

**Bladzijde 4, Imker Zoemzang (verrast):**
- Het portret maakt een **sprongetje** en wordt even iets groter.
- Er staat een **!** in de hoek.
- Het naambordje is **honinggeel** (De Schemerwortel).

**Bladzijde 5, de Mika-imker (boos):**
- Het portret is een **Mika** (dat was eerst een boekje).
- Het **schudt** en er staat een rood **╬** in de hoek.
- Het naambordje is **grijs**, met een ander (sluw) stemmetje.

**Bladzijde 6, jij:**
- Je **eigen portret staat rechts**.
- De ballon is lichtgroen en het bordje **mintgroen**.

**Bladzijde 7, de trainer:**
- Het naambordje is **paars**, met "Knuffelorde" ernaast.

**Bladzijde 8, een lange tekst:**
- Past alles? De ballon groeit mee.
- Is het scherm te laag, dan scroll je de tekst met het **muiswiel** of met **spatie**.

**De keuzes op de laatste bladzijde, elk met een icoontje:**
- een boek: een opdracht aannemen;
- een smaragd: een winkel;
- een kaart: de reiskaart;
- een veertje: reizen;
- een deur: "Nu even niet";
- een kompas: **"Wat moet ik doen?"**;
- **Doei!** heeft ook een deur.

Rechts in elke knop staat het **cijfer**. Druk op 1-6 om meteen te kiezen.
- Kies je een gewone keuze, dan sluit het scherm en zie je in de chat "Dialoog-demo: je koos keuze N."

**Boven de keuzes staat de opdrachtstrook:**
- "**Opdracht:** Breng 3 knuffelkoekjes naar Oma Truus, bij de bakkerij in Zwamzoet".
- Wijs hem aan voor de hele tekst.

**"Wat moet ik doen?":**
- Oma Truus zegt de opdracht dan als een gewone zin: wie, wat en waar.
- Met **« Terug** ga je terug naar de keuzes.

**Bladeren en de log:**
- Onder de tekst staan **stipjes**: één per bladzijde, en de huidige in de kleur van de spreker.
- **« Terug** of **backspace** = een bladzijde terug.
- **Shift+spatie** = meteen naar de keuzes. De keuzes sla je nooit over.
- **L** of het knopje **≡** rechtsboven opent het **gesprekslog**: alles wat er in deze sessie gezegd is, met de kleur van de spreker en scrollen met het muiswiel.
- **L** of **Esc** sluit de log weer. Daarna ben je terug in het gesprek, het scherm gaat niet dicht.

**De instellingen (het tandwieltje ⚙ rechtsboven):**
- **"Tekst: normaal"**: klik om te wisselen tussen meteen, langzaam, normaal en snel.
- **"Stemmetjes: 60%"**: een schuif. Bij het schuiven hoor je een voorbeeldpiepje. Op 0 = uit.
- Je vindt allebei ook onder Mods > Guhs > Config (`config/guhs-vadsrijk-dialoog.toml`).

### D2. Een vol scherm (GUI-schaal 3-4)
```
/guhs vadsrijk dialoog vol
```
Dit is een lange tekst met 9 keuzes.
- Op GUI-schaal 4, of in een klein venster, komen de keuzes in **twee kolommen**.
- De tekst scrollt, met een knipperend ▼ als er nog meer staat.
- Er mag **niets** boven of onder buiten beeld vallen.
- "Naar Kandijkom (nog op slot)" staat met **grijze** tekst.

### D3. Echte quest-NPC's: "Wat moet ik doen?"
In een nieuwe wereld, in het Vadsrijk:
```
/guhs vadsrijk npc tp rekruteerder
```
**Klik de sergeant:**
- Hij biedt hq01 aan. Heb je hq01 al (dan zie je meteen "Nu:"), test het aanbod dan bij de volgende questgever (volg de questvolger).
- Boven de keuzes staat "**Opdracht:**" met de eerste stap (wat, waar).
- Het kompasknopje "Wat moet ik doen?" zegt het als een zin: *Als je ja zegt ("..."): ..., ....*
- Neem de quest aan en klik hem nog een keer. Er staat nu "**Nu:**" met de huidige stap.

**Klik een NPC zonder quest (een burger):**
- Je ziet géén kompasknop en geen strook.

**Zone 2 en 3:**
- Ook daar krijgen de gesprekken het nieuwe scherm.
- Probeer `/guhs vadsrijk npc tp z2_kapitein` en klik.
- Het bordje is ijsblauw en de reisknoppen hebben een veertje en een kaart.

### D4. Vergelijken met het oude scherm
```
/guhs vadsrijk dialoog oud
/guhs vadsrijk dialoog demo
/guhs vadsrijk dialoog oud
```
- De eerste keer `oud` geeft je het oude scherm. De tweede keer krijg je weer het nieuwe.
- Het oude scherm moet net zo werken als vroeger.

**Buiten het Vadsrijk:**
- Een Knuffeldal-NPC, een Erfenis-gesprek en een verhaal-NPC houden **altijd** het oude scherm.
- Even één aanklikken is genoeg.

### D5. Ontwikkelaarshulp
**F3+H aan (geavanceerde tooltips):**
- Onder het scherm staat een grijze regel met: sleutel | tekstsleutel | factie/stem/emotie (een ! = vast gezet, ~ = geraden uit de tekst) | portretbron.

**Een emotie per regel in questdata:**
- Zet bij een spreker `"npc:blij"` (of `verdrietig`, `verrast`, `boos`) in plaats van `"npc"`.
- Zonder zo'n emotie raadt het scherm die uit de tekst: "Hoera" = blij, "?!" of "Ooh" = verrast, "Grr" = boos, "helaas" of "..." = verdrietig.

## SPIEK T-GELUID: muziek + omgevingsgeluid in het Vadsrijk

Besluit R3 was "muziek ja, zacht". Dit blok heeft vier delen:
- **zachte gebiedsmuziek**, met lange stiltes ertussen;
- **een thema per baas**;
- **stingers** (korte ta-daa's);
- **een rijkere omgevingsgeluid-laag**, zonder harde wind.

De muziek is gemaakt met een eigen pijplijn: MIDI → SoundFont (GeneralUser GS) → OGG. De instrumenten zijn speeldoos, celesta, harp, fluit, marimba, kalimba, zachte strijkers en koor. Alleen de bazen klinken episch.

> **Smaak gaat voor.** Vind je een stuk niet mooi? Zeg dan welk stuk en wat eraan schort, bijvoorbeeld "te druk", "te somber", "ander instrument" of "trager". Elk stuk is één regel data in `tools/vadsrijk/geluid/stukken.py` en is in ~10 s opnieuw gemaakt.

### Snel alles horen
| Wat | Commando |
|---|---|
| Klikbare lijst van ALLE tracks (klik = luisteren) | `/guhs vadsrijk geluid` |
| Eén gebiedsthema | `/guhs vadsrijk geluid gebied klodderweide` |
| Eén baasthema | `/guhs vadsrijk geluid baas vahoegnir` |
| Eén stinger | `/guhs vadsrijk geluid stinger baas_verslagen` |
| Eén omgevingsbed (12 s) | `/guhs vadsrijk geluid bed krekels` |
| Alles stil | `/guhs vadsrijk geluid stop` |
| De gebiedsmuziek hier NU starten (sla de stilte over) | `/guhs vadsrijk geluid nu` |
| Waar ben ik, wat hoort hier, hoe staan de schuifjes | `/guhs vadsrijk geluid info` |

Je kunt de OGG's ook buiten het spel afspelen. Ze staan in `src/main/resources/assets/guhs/sounds/vadsrijk/muziek/{gebied,baas,stinger}/` en `.../vadsrijk/sfeer/`.

### Schuifjes
Ga naar **Mods → Guhs → Config** (het bestand `guhs-vadsrijk-geluid.toml`). Elke waarde is een schuif van 0-100.
- **gebiedsmuziek:** standaard 70.
- **bazenmuziek:** standaard 85.
- **stingers:** standaard 80.
- **omgeving:** standaard 70.
- **pauze_min / pauze_max:** de stilte tussen twee gebiedsstukken. Standaard 60 / 180 s.

De muziek volgt OOK de schuif **Muziek** van Minecraft, en het omgevingsgeluid volgt **Omgeving**. Zet je een schuif op 0, dan staat dat deel uit. Staat gebiedsmuziek op 0, dan speelt in de wildernis weer de gewone Minecraft-muziek.

Testen:
1. Zet gebiedsmuziek op 0. → Na ~5 s stopt het stuk.
2. Zet hem terug op 70. → Daarna start `/guhs vadsrijk geluid nu` hem weer.

### 1. Gebiedsmuziek (zones 1-3 + Lampionkust + Hoogvads)
Zo werkt het:
- Je loopt een gebied in. Na 5-15 s komt het thema zacht op (~1-1,5 min).
- Daarna is het **1-3 min stil**, met alleen omgevingsgeluid. Dan speelt het thema opnieuw.
- Loop je een ander gebied in, dan vervaagt het oude stuk binnen ~9 s. Kort daarna komt het nieuwe.
- In steden speelt de bestaande stadsmuziek (die blijft zoals hij was).
- Tijdens een baasgevecht, scène of cutscene speelt er geen gebiedsmuziek.

| Gebied | Commando | Karakter |
|---|---|---|
| Klodderweide (+ zone-1-rand) | `/guhs vadsrijk geluid gebied klodderweide` | vrolijk pastoraal: fluit, marimba, harp, pizzicato |
| Sporenwoud | `... gebied sporenwoud` | lydisch toverbos-walsje: ocarina, celesta, speeldoos, glaspad |
| Druipgrotten | `... gebied druipgrotten` | rustig mineur: kalimba met echo, druppelklanken |
| Zwamtempel | `... gebied zwamtempel` | plechtig dorisch: panfluit, koor-oehs, harp, buisklok |
| Knuffelmeer | `... gebied knuffelmeer` | dromerig meer-walsje: klarinet, vibrafoon, harp |
| Galmhellingen (+ zone-2-rand) | `... gebied galmhellingen` | blokfluit met ECHO (galm!), glockenspiel, hoorn |
| Splinterkloof | `... gebied splinterkloof` | 6/8 avontuur in mineur: hobo, viool, pizzicato-ostinato |
| Spiegelburcht | `... gebied spiegelburcht` | spiegel-wals: celesta met glockenspiel-echo, klavecimbel |
| Wortelvelden (+ zone-3-rand) | `... gebied wortelvelden` | mixolydisch: ocarina, gitaar-arpeggio, marimba, shaker |
| Wortelduister | `... gebied wortelduister` | donker maar lief: kalimba met echo, lage strijkers |
| Het Hart | `... gebied hart` | heilig en traag: celesta, harp, koor |
| Schorsstad | `... gebied schorsstad` | huppelend: hakkebord, klarinet, fagot, accordeon |
| Stamspiraal | `... gebied stamspiraal` | opklimmend: panfluit, harp, pizzicato |
| Takkenbanen | `... gebied takkenbanen` | luchtig 6/8: fluit, piccolo, harp, glockenspiel |
| Kruinzee | `... gebied kruinzee` | breed en zwevend: viool, celesta, strijkers |
| Zeppelinwerf | `... gebied zeppelinwerf` | bedrijvig-grappig: klarinet, xylofoon, tuba, fagot |
| Lampionkust overdag | `... gebied lampionkust_dag` | eilandjes: steeldrum, ukelele, fluit, **met het Vadsrijk-leidmotief** in het middenstuk |
| Lampionkust 's avonds | `... gebied lampionkust_avond` | lampionfeest-wiegje: speeldoos, vibrafoon, **leidmotief** |
| Hoogvads | `... gebied hoogvads` | feestelijk-groots: hoorn, trompet, strijkers, koor, pauk, **leidmotief** |
| Hoogvads markt | `... gebied hoogvads_markt` | markt-wals: fluit, accordeon, pizzicato |

- **Lampionkust:** overdag speelt dag, vanaf de avond (tijd 11500-23500) avond.
- **Hoogvads:** de twee stukken wisselen elkaar af.
- **Zones 4 en 5:** die herkent het spel aan een gebied-id `z4_*` / `z5_*`, of anders aan de W-3-vakken (zone 4: x 1728..2559, z -768..-257; zone 5 erboven vanaf y 150).

**Kijk/luister:**
1. Ga naar de Klodderweide.
2. Wacht ~10 s. → Het stuk komt zacht op. Na afloop is het stil.
3. Vraag `/guhs vadsrijk geluid info`. → Je ziet "stilte, nog N s".
4. Loop naar Zwamzoet. → Het gebiedsstuk vervaagt en het stadsdeuntje neemt over.

### 2. Baasthema's
Het thema start zodra de baasbalk verschijnt en loopt naadloos rond. Gaat de balk weg, dan vervaagt het. Elke baas heeft een eigen thema:

| Zone | Baas → commando |
|---|---|
| 1 | Klodderreus `baas klodderreus` · Koning Klodder `baas koning_klodder` · Opperzwam `baas opperzwam` · Mika-spion `baas mika_spion` |
| 2 | Boormeester `baas boormeester` · Galmwolf `baas galmwolf` · Vrouwe Spiegelzucht `baas spiegelzucht` (ook haar spiegelbeeld) |
| 3 | Baron Bleu `baas baron_bleu` · Oude Nestwachter `baas nestwachter` · Wortelwurger `baas wortelwurger` · Graaf Piepvel `baas piepvel` · Kruinroofvogel `baas kruinroofvogel` · **Vahoegnir** `baas vahoegnir` · Bombardier Brokkel `baas brokkel` · Kapitein Roquefort `baas roquefort` · Takelmeester Taleggio `baas taleggio` |
| 4 | Grauwe Zeeslurf `baas zeeslurf` · de Vliegervanger `baas vliegervanger` |
| nieuw/onbekend | `baas algemeen` (vangnet voor elke baas zonder eigen thema) |

Elk commando begint met `/guhs vadsrijk geluid`.

**Echt testen:**
1. Vecht met een baas, bijvoorbeeld `/guhs vadsrijk baas reset` bij een arena.
2. Bij de balk → het eigen thema.
3. Win je → het thema vervaagt en de stinger **baas_verslagen** (koperfanfare) speelt.

Zone-4-bazen worden herkend aan een entity-id met `zeeslurf` / `vliegervanger` erin. Een baas kan ook zelf een geluid-id teruggeven via `muziek()`.

### 3. Stingers
| Stinger | Wanneer | Commando |
|---|---|---|
| ontdekking | de EERSTE keer dat je een gebied inloopt (niet de grote zone-randen) | `/guhs vadsrijk geluid stinger ontdekking` |
| quest_klaar | een quest ingeleverd | `... stinger quest_klaar` |
| level_op | level omhoog (één ta-daa, ook als generiek + klasse tegelijk stijgen) | `... stinger level_op` |
| baas_verslagen | een baas opgevrolijkt (iedereen die hielp) | `... stinger baas_verslagen` |
| geheim | een schat/geheim gevonden (de "schat"-titelkaart van zone 1-3) | `... stinger geheim` |

- Tijdens een stinger zakt de muziek even weg.
- Twee stingers tegelijk spelen na elkaar.
- Snel testen: `/guhs vadsrijk level <hoger dan nu>` (of `/guhs vadsrijk xp 5000`) → stinger level_op.

### 4. Omgevingsgeluid (de §5.3-pas)
Per gebied speelt een zacht "bed" (een lus van 22 s). Elke 5-12 s klinkt ergens om je heen een los geluidje.

| Bed | Waar | Wat je hoort |
|---|---|---|
| weide | Klodderweide, zone-1-rand, Hoogvads | vogeltjes, zacht bladgeritsel |
| bos | Wortelvelden, Schorsstad, Stamspiraal | meer vogels, af en toe een krakend takje |
| sporen | Sporenwoud | toverglinstering, zachte zoemtoon, een enkel vogeltje |
| grot | Druipgrotten + overal ondergronds | druppels met galm, diepe grotruis |
| tempel | Zwamtempel, Spiegelburcht, Het Hart | zachte zoemtoon, druppels |
| meer | Knuffelmeer | kabbelend water, vogels, een kikkertje |
| berg | Galmhellingen | belletjes (windgong), verre beek, vogeltje: **géén wind** |
| kloof | Splinterkloof | beekje (stroompje), druppels |
| wortel | Wortelduister | diepe druppels met lange galm |
| kruin | Takkenbanen, Kruinzee, Zeppelinwerf | bladeren + vogels in de kruin |
| honing | Honingtak (bovenop het stadsbed) | bijengezoem |
| kust | Lampionkust (zone 4) | golven, zeemeeuwen, windgong |
| krekels | 's nachts (tijd 13000-23000), buiten | krekels, uiltje, zachte windgong |

- 's Nachts maken de vogelbedden plaats voor de krekels. Bij water (meer, kloof, kust) komen de krekels erbovenop.
- De oude Z2-wind (Balto-wind op de Galmhellingen, het Knuffelmeer en de Splinterkloof) is vervangen door een zacht belletje.

**Luister:**
1. Speel `/guhs vadsrijk geluid bed kust` (12 s), daarna `bed krekels` en `bed honing`.
2. Ga 's nachts op de Klodderweide staan (`/time set 18000`). → Je hoort krekels en een uiltje.
3. Ga een grot in. → Je hoort druppels.

### Bekend / nog niet
- De muziek is door een computer gecomponeerd (regels + zaadjes). Hij klinkt lief en netjes, maar niet als een echte componist. Het blijft smaakwerk: zeg welke stukken weg of anders moeten.
- Bazen hebben nog geen aparte fase-2-laag: het thema is één lus van ~40-70 s.
- Er zijn nog geen puntbronnen uit de POI-lijsten (watervallen, bijenkorven, smidse). Er is ook nog geen grot-echo op voetstappen.
- De Lampionkust en Hoogvads gebruiken nu één thema per zone (+ dag/avond, + markt). Een thema per eiland of wijk kan later.

## T-MODELS: mooiere mobs (R5 hybride) en de Klodderreus/Vahoegnir als showcase

Wat is er veranderd? Alle gewone wezens van zone 1-3 zijn opnieuw gemaakt met de verbeterde generator:
- **Rondere vormen:** grote blokken hebben afgeronde randen en hoeken (een paar trapjes, zoals een kussen), poten lopen iets taps toe.
- **Betere textures:** licht van boven (rug lichter, buik donkerder), een zachte schaduw onderaan, glansplekken op slijm, zachte
  vlekjes en stippen, vachtstreepjes op vacht en pluis, kristallen met lichte randjes.
- **Gezichtjes:** groter (ze lopen nu over de ronde voorkant door), zachte randjes en een extra glimpje in de ogen.
- **Gloed:** gloeiende delen (kristallen, ogen, lampjes) zijn in het midden feller en aan de rand zachter.
- **Wiebelen:** oren, staarten en manen wiebelen een beetje na. Slijmen hebben een wiebelend kapje bovenop (squash & stretch).
- **De Grauwe Klodderreus** is geen vanilla-slijmblok meer: een eigen groot model met guh-oren, boze 3D-wenkbrauwen, paarse
  gloei-ogen, druppels langs zijn lijf, een mond die opengaat, en een roze blije versie na de plof.
- **Vahoegnir:** hetzelfde goedgekeurde ontwerp (ook de grote oren), alleen met meer volume (licht van boven, buik in de schaduw)
  en zijn oren wiebelen mee als hij vliegt.
- Voor/na-plaatjes: `guhs_vadsrijk\t-models\voorbeelden\` (zone1_*.png, zone2_*.png, zone3_*.png, klodderreus.png, vahoegnir.png).

### Showroom: alle wezens op een rij (stil, onkwetsbaar)
Ga op een vlakke, open plek staan en kijk de kant op waar ze moeten komen.
```
/guhs vadsrijk models toon zone1
/guhs vadsrijk models toon zone2
/guhs vadsrijk models toon zone3
/guhs vadsrijk models toon critters
/guhs vadsrijk models plof
/guhs vadsrijk models weg
```
- `toon` zet een raster neer (6 per rij, 4 blokken uit elkaar) van alle wezens van die groep. Ze staan stil maar spelen hun
  idle-animatie: let op het wiebelen van oren, staarten en het slijmkapje.
- `plof` vrolijkt alle showroom-wezens op: zo zie je de roze, blije textures (zone 1) en de plof-doorzichtigheid.
- `weg` ruimt alles op (alles met de showroom-tag binnen 96 blokken). `toon alles` kan ook, maar dat zijn er ~55.
- Kijk vooral naar: zijn de vormen ronder, lopen de gezichtjes netjes over de voorkant, zitten er geen gaten of rare randjes
  aan de afgeronde hoeken, en lezen de ogen goed op afstand.

### Eén wezen in actie (lopen, aanvallen)
```
/guhs vadsrijk wezens spawn klodder 3
/guhs vadsrijk wezens spawn hoedjesbol 3
/guhs vadsrijk wezens spawn kristalklodder 3 true
/guhs vadsrijk z2wezens spawn sneeuwknabbelaar
/guhs vadsrijk z3wezens spawn rotzwijn
```
- Met `true` erachter (zone 1) krijg je een elite: de gouden gloed moet nog netjes over het nieuwe model liggen.

### De Grauwe Klodderreus
```
/guhs vadsrijk models reus
/guhs vadsrijk models reus anim spuug
/guhs vadsrijk models reus anim brul
/guhs vadsrijk models reus anim aanloop
/guhs vadsrijk models weg
/guhs vadsrijk models reus vecht
```
- `reus` zet hem stil neer (7 blokken voor je), om rond te lopen en te bekijken. `reus anim ...` speelt een actie bij alle
  reuzen binnen 32 blokken: spuug (mond open, lijf zwelt op), brul (bij een nieuwe fase), aanloop (hij duikt in elkaar voor een sprong).
- `reus vecht` = een echt gevecht (lv 3) zonder arena. Let op: in de lucht wordt hij lang, bij het landen plat (squash),
  bij het rollen in fase 3 rolt hij echt over de kop, en in fase 2 spuugt hij kloddertjes: die hebben nu ook het ronde
  klodder-model in plaats van het vanilla-slijmblok. Na de plof: roze, blij, doorzichtig, zonder gloei-ogen.
- Het echte gevecht bij de kampvijver (BAM-quest) gebruikt hetzelfde model.

### Vahoegnir
```
/guhs vadsrijk wezens draak
/guhs vadsrijk z3bazen vahoegnir
```
- `wezens draak` laat de grijze overvlieger langskomen: zijn grote ronde oren wiebelen nu na op de vleugelslag.
- `z3bazen vahoegnir` voor de baas (grijs -> roze per stap): zelfde ontwerp, iets meer diepte (buik en poten donkerder, rug lichter).

## T-VFX: bazen-effecten, telegraphs op de grond, cast-balk en de bazenbouwdoos

**Wat is er nieuw?**
- Alle rode/roze/paarse/gele cirkels op de grond zijn nu **echte vlakken op de grond** (decals) in plaats van stofjes. Ze lopen
  vol tot de klap komt en flitsen vlak ervoor.
- Er zijn nieuwe vormen: **kegel, lijn, donut** (veilig in het midden), **kruis** en **"veilige plek"** (alles is gevaarlijk behalve
  de roze cirkel met hartjes).
- Er zijn **eigen deeltjes**: sterretjes, roze pluis, slijmspatjes, sporenwolkjes, kristalsplinters, honing, confetti, hartjes,
  veren, blaadjes en rook.
- Er zijn **schokgolven, stralen, flitsen, sporen** (lint achter een sprong/duik) en **lichtzuilen**.
- Er is een **cast-balk** onder de baasbalk: "Koning Klodder laadt: **Koninklijk Bad**".
  - Een paarse balk met "Onderbreek!" eronder betekent: raak hem hard (of verdoof hem), dan stopt de aanval en is de baas even suf.
- Bazen hebben **animaties**:
  - aanloop (inzakken en trillen tot de klap);
  - de klap;
  - een schokje als je ze raakt;
  - een faseovergang (steigeren met een schokgolf en een lichtzuil);
  - woede na 6 minuten;
  - de opgevrolijkt-finale (wiegen, huppen, zwaaien, confetti).
- **Elke baas uit zone 1-3 heeft minstens één nieuwe aanval** (de lijst staat hieronder).

**Het schuifje.** Je VFX-niveau zet je met `/guhs vadsrijk vfx niveau vol|zacht|uit`. Het staat ook in Mods > Guhs > Config
(`guhs-vadsrijk-vfx.toml`).
- **zacht** = minder deeltjes;
- **uit** = geen effecten.
- De cirkels op de grond blijven ALTIJD zichtbaar.
- Het schermschudden heeft zijn eigen schuif: "Vadsrijk: schermschud" (die was er al).

### Snel alles zien (zonder baas)
```
/guhs vadsrijk vfx demo alles
```
Kijk een kant op en voer het uit. Je ziet:
- een cirkel voor je;
- een kegel in je kijkrichting;
- een paarse lijn;
- een donut om je heen;
- een roze veilige plek met hartjes;
- een geel kruis;
- 13 soorten deeltjes in een kring;
- een schokgolf, een straal, een flits, een spoor achter jou (loop rond!) en een lichtzuil.

Losse onderdelen: `/guhs vadsrijk vfx demo cirkel|kegel|lijn|donut|veilig|kruis|deeltjes|golf|straal|flits|spoor|zuil`.

Bij een baas in de buurt: `demo cast` (een paarse cast-balk met "Onderbreek!") en `demo pose`.

**Let op:**
- De vlakken liggen netjes óp de grond, ook op een trapje of helling: ze zweven niet en zakken niet weg.
- Ze vullen zich gelijkmatig en flitsen op het eind.
- Zet `/guhs vadsrijk vfx niveau uit` en doe de demo nog eens: de cirkels zijn er nog, de deeltjes niet.

### Een baas testen
1. `/guhs vadsrijk vfx spawn <baas>`. Dit roept het juiste bestaande zone-commando aan (reset + tp, of spawn).
2. `/guhs vadsrijk vfx aanval <id>` start een nieuwe aanval meteen op de dichtstbijzijnde baas.
3. `/guhs vadsrijk vfx aanvallen` geeft de lijst.

Meer commando's:
- `/guhs vadsrijk vfx anim vfx_windup|vfx_treffer|vfx_geraakt|vfx_fase|vfx_woede|vfx_juich` speelt een animatie af op de
  dichtstbijzijnde baas.
- `/guhs vadsrijk vfx woede` zet de woede nu aan (de aanvallen komen 2x zo vaak, met rode rook).
- `/guhs vadsrijk vfx stop` stopt een lopende aanval.
- Fase overslaan kan met de bestaande commando's: `/guhs vadsrijk z3bazen fase 2|3|4` en `/guhs vadsrijk z2bazen fase 2|3`.
  Je ziet dan de faseovergang: steigeren, twee schokgolven, een lichtzuil en sterretjes.

In een gewoon gevecht komen de nieuwe aanvallen vanzelf: ongeveer om de 9-20 s, tussen de eigen aanvallen van de baas door.

### Per baas: wat moet je zien?
| Baas | Spawn | Nieuwe aanvallen (`vfx aanval <id>`) | Let op |
|---|---|---|---|
| **Grauwe Klodderreus** | `vfx spawn klodderreus` (los, zonder baasbalk; of in zijn vijver bij Knuffelkamp) | `kloddertjesregen` (6 slijmcirkels + 1 onder jou, plakkerig), `kloddergolf` (fase 2: donut, sta DICHT bij hem) | Sprong: hij zakt trillend in (aanloop), de cast-balk zegt "Plettersprong", hij laat een spoor in de lucht achter, en bij de landing komen een schokgolf en slijmspatjes. Rollen: "Rolaanval" met een groen spoor. |
| **Koning Klodder** | `vfx spawn koning_klodder` | `kroonregen` (sterrencirkels op iedereen + 4 los), `koninklijk_bad` (fase 2, PAARS: onderbreek met ~5% schade, anders heelt hij 8%) | De schokgolf heet "Grote Plof" in de cast-balk. Hij zakt in voor zijn sprong, en bij de landing komt er een blauwe schokgolf. |
| **Opperzwam** | `vfx spawn opperzwam` | `sporenkegel` (fase 1: kegel voor hem), `sporenkruis` (fase 2+: voor+achter, daarna links+rechts) | Stamp: aanloop + "Sporenstamp", dan per plek een paarse sporengolf. In het donker doet hij geen extra aanvallen. |
| **Galmwolf** | `vfx spawn galmwolf` | `echobrul` (kegel voor, dan achter hem), `galmring` (fase 2: eerst de cirkel om hem heen, dan de ring: eerst WEG, dan TERUG) | Kristalsplinters bij elke klap. |
| **Vrouwe Spiegelzucht** | `vfx spawn spiegelzucht` | `spiegelscherven` (3 lijnen in een waaier), `spiegelzaal` (fase 2: alleen de 2 roze hartjesplekken zijn veilig) | De rode ring rond de roze plekken loopt tot de rand van de zaal. |
| **Boormeester** | `vfx spawn boormeester` | `boorlinie` (3 lijnen na elkaar), `overdruk` (fase 2, PAARS: onderbreek met ~6%, of ren naar een roze plek) | Bij het onderbreken: "ONDERBROKEN!" in de balk, sterren boven zijn hoofd, en hij staat 2,5 s suf. |
| **Kruinroofvogel** | `vfx spawn kruinroofvogel` | `veerregen` (8 cirkels + 1 onder iedereen), `windkegel` (fase 2: duwt je weg) | Veren dwarrelen. |
| **Takelmeester Taleggio** | `vfx spawn taleggio` | `touwzwiep` (brede kegel), `kaasvaten` (fase 2: plakkerige kaascirkels) | Honingdruppels. |
| **Bombardier Brokkel** | `vfx spawn brokkel` | `kruisvuur` (kruis op jou), `dubbel_kruisvuur` (fase 2: + een kruis 45° gedraaid) | Stralen langs de armen van het kruis. |
| **Kapitein Roquefort** | `vfx spawn roquefort` | `sabelsnede` (tankbuster: grote klap op zijn doelwit, ga weg uit de cirkel), `sabelstorm` (fase 2: draaiend kruis om hem heen) | |
| **Graaf Piepvel** | `vfx spawn piepvel` | `capezwiep` (kegel voor, dan achter), `piepkoor` (fase 2, PAARS: onderbreek, of sta dicht bij hem; niet onderbroken = hij heelt 6%) | Rookwolkjes. |
| **De Wortelwurger** | `vfx spawn wortelwurger` | `wortelgreep` (cirkel onder iedereen + 5 los, plakkerig) | Blaadjes. |
| **De Nestwachter** | `vfx spawn nestwachter` | `nestschok` (eerst de ring: kom DICHTBIJ, dan de cirkel: ga WEG; het omgekeerde van de Galmwolf) | |
| **Baron Bleu** | `vfx spawn bleu` | `degenreeks` (3 snelle lijnen), `duet` (fase 2, GEEL: stapel, samen erin staan deelt de schade; alleen = 60%) | |
| **Vahoegnir** | `vfx spawn vahoegnir` (of `/guhs vadsrijk z3bazen vahoegnir herinnering`) | `vuurregen` (10 vuurcirkels + 1 onder iedereen), `hemelstorm` (fase 2-3: 2 roze veilige plekken), `vlammenkruis` (fase 3: een groot kruis in het midden, draait 45°, dan terug), `laatste_brul` (fase 3, PAARS: onderbreek, of sta in het midden) | Eigen aanvallen met een cast-balk: "Grauwe Huil", "Vleugelwind" (aanloop + verenschokgolf), "Grauw Vuur" (aanloop + een grijze vuurstraal langs de lijn). Een gouden spoor bij zijn duiken. In fase 4 (het ei en het einde) komen geen extra aanvallen. |

### Altijd checken
- **Verslagen:** de baas wiegt, huppelt en zwaait. Je ziet confetti, hartjes, een roze flits en een roze lichtzuil.
  Zone-2-bazen spatten in grijze kristalscherven.
- **Raken:** een klein schokje (niet vaker dan om de 1,5 s). Tijdens een aanloop schokt hij niet.
- **FPS:** zet alles op VOL en laat een baas een paar aanvallen doen. Het zijn maximaal 700 deeltjes. Op ZACHT zijn het er
  maximaal 250.
- **Shaders (Iris, indien aanwezig):** de vlakken en gloed moeten zichtbaar blijven.

## Itemisatie 2.0 (T-ITEMS): nieuwe vakken, nieuwe stats, Erfstukken, smid-vangnetten, drankjesriem

Alles onder `/guhs vadsrijk items ...` (op 2). Test in het Vadsrijk (de vakken en de riem werken alleen daar).

### 1. Nieuwe vakken: 2e ring, armband, handschoenen, cape
```
/guhs vadsrijk items nieuwe
/guhs vadsrijk items ringen
```
- Je krijgt alle 17 nieuwe stukken (Zeldzaam, jouw niveau) en 2 ringen.
- Open je inventaris (E).
- **Rechts staat een kolom van 7 vakjes:** R, R2, K, O, A, H, C (Ring, Ring 2, Ketting, Oorbel, Armband, Handschoenen, Cape).
- **Aandoen:** Shift+klik een stuk in je inventaris, of rechtsklik het in je hand.
  - De 1e ring gaat naar R, de 2e naar R2.
  - Een 3e ring wisselt met de ring in R.
- **Afdoen:** klik op het vakje.
- **Wat moet je zien:**
  - de tooltip van een armband/oorbel/handschoen/cape toont lagere getallen dan een ring van vroeger (het "vakbudget": 70 % voor sieraden, 60 % voor handschoenen en cape);
  - na het aandoen stijgen je stats.
- **Overzicht en reset:**
  - `/guhs vadsrijk items slots` laat zien wat je draagt;
  - `/guhs vadsrijk items uittrekken` doet alles af.
- **Stats bekijken:** `/guhs vadsrijk items stats` (de 4 nieuwe stats staan in het paars).
- **De inventariswissel:** loop het Vadsrijk uit en weer in. Je vakken moeten er nog zijn, en buiten het Vadsrijk tellen ze niet.
- **Sets:** de Kloddergrijpers, Sporencape, Kristalhandschoenen, Buitcape, Bloesemhandjes, Schorshandschoenen en Wortelcape tellen als setstuk. De tooltip zegt bij welke set.
  - Test: 3 stukken van de Klodderset + de Kloddergrijpers = de 4-stukbonus.
  - Voorbeeld: `/guhs vadsrijk items geef klodderkapje` (en klodderhesje, klodderbroek), plus de grijpers.
- **Losse stukken:** `/guhs vadsrijk items geef <basistype> [zeldzaamheid] [level]` (tab-aanvulling op de id).

### 2. Duidelijke stat-namen + 4 nieuwe stats
Hover over gear. Elke regel is gewoon te lezen, bijvoorbeeld:
- "+3 Kracht (schade van dichtbij)"
- "+2,5% Kans op kritieke treffer"
- "+4% Kortere wachttijd vaardigheden"

**De 4 nieuwe stats:**
- **Verdediging negeren %:** neemt dat deel terug van wat je klap kleiner maakt: een slecht element (x0,7), schilden en blokken van wezens, en hun Knuffelschild. Max 50 %.
- **Schade tegen bazen %:** meer schade op elke baas.
- **Leven per treffer:** zoveel Vads terug per raak (max 4 per tik bij een vlakaanval).
- **Kans op effect %:** je element zet zijn status:
  - vuur = Brandend
  - natuur = Gesporen
  - water = Plakkerig
  - roze = kort Verdoofd (niet op bazen)

  Max 40 %.

**Waar ze vallen:**
- alleen op wapens, ringen, handschoenen, armbanden, kettingen, oorbellen en capes;
- nooit op helm, borst, benen of voeten.

**Test:**
```
/guhs vadsrijk items geef pluisarmband legendarisch
```
Dat stuk heeft 4-5 bonussen; herrol het een paar keer bij de smid tot er een nieuwe stat op staat. Sla daarna een wezen en kijk naar de Vads/status.

### 3. Smid: tabs, geduld (pity), Knuffeltalisman, polijsten, previews
```
/guhs vadsrijk items smidspul
/guhs vadsrijk items smid
```
`smidspul` geeft steentjes, knabbels, draad, 5 talismannen, 5 groeipluis en 50 zilver.

**Bovenaan het aambeeld staan tabs:** Sterren · Bonussen · Groeien · Overvadsen.

**Sterren:**
- Zichtbaar: de kans, het risico, de prijs, en "Als het lukt:" met de echte stat/schade-verandering van de volgende ster.
- **Geduld:**
  - elke mislukte poging geeft +5 % op de volgende (tot ster 7);
  - het balkje "Smidguh wordt ongeduldig..." vult;
  - na een succes is het weer leeg.
  - Snel testen: neem een stuk in je hand, `/guhs vadsrijk items pity 4`, kies het bij de smid → "+20.0% geduld".
- **Knuffeltalisman:**
  - vink "☐ Knuffeltalisman" aan;
  - mislukt een ster die je normaal een ster kost? Dan krijg je "De Knuffeltalisman beschermde je ster!" en is er 1 talisman op;
  - "Koop 1" koopt er een voor munten;
  - verpluisd kan nog steeds (vanaf +6).
  - Sterrenvangen bestaat niet (zoals besloten).

**Bonussen:**
- Herrollen werkt als vroeger (klik een bonus = ♥ vasthouden).
- **Polijsten:** klik een bonus en je ziet "kan X tot Y worden (nu Z)". De knop Polijsten geeft een nieuwe waarde binnen dat bereik (kan ook lager!).

### 4. Erfstukken: Vertrouwd, Doorgroeien, Overvadsen
**Vertrouwd:**
- Gedragen gear (wapen in je hand, harnas, alle 7 vakken) krijgt item-xp per opgevrolijkt wezen (elite 3, baas 15).
- Tooltip: "Vertrouwd 2 (130/280 xp): +4% basisstats"; bij 5 een gouden regel.
- Bij elk niveau komt er een chatberichtje.
- Snel: `/guhs vadsrijk items vertrouwd 300` (alles wat je draagt).

**Doorgroeien:**
```
/guhs vadsrijk items legendarisch
```
- Je krijgt een Legendarisch wapen van je klasse dat al Vertrouwd 5 is.
- Smid → tab **Groeien**: je ziet "Niveau X → X+10" en de stat-verschillen, en wat het kost (Groeipluis + munten).
- "Laat groeien!" → het stuk is Gebonden, "Doorgegroeid I", Vertrouwd begint opnieuw.
- **Grenzen:**
  - Legendarisch groeit 2x (de 3e keer zegt hij dat het op is);
  - Guhddelijk groeit altijd (tot niveau 50);
  - Episch en lager groeit nooit.
- Groeipluis valt bij elke baas (1 per helper).

**Overvadsen:**
- Smid, tab **Overvadsen**:
  1. klik een ander (oud) stuk;
  2. kies links een bonus van het oude stuk, en rechts welke bonus hij vervangt (of "een nieuwe erbij");
  3. je ziet de nieuwe waarde en de kans (15 %) op een "lieve mislukking" (iets lager).
- Het oude stuk verdwijnt; het doel blijft altijd heel.
- Het mag niet als:
  - het doel die bonus al heeft;
  - de bonus niet op dat soort stuk mag;
  - het stuk verpluisd is.

### 5. Drankjesriem
```
/guhs vadsrijk items drankjes
```
- Dit vult de riem met Vads-, energie- en Vahoegdrankjes.
- **Links naast je hotbar** staan 3 vakjes met de toetsen **Alt+1 / Alt+2 / Alt+3** (Alt ingedrukt houden + cijfer; je hotbar-vakje wisselt dan niet). Druk erop om meteen te drinken.
  - Het gedeelde drankjes-wachten (10 s) telt: het vakje wordt donker.
  - Spam = "Je drankjes moeten nog even afkoelen."
- **In je inventaris:** een kolom "Riem" rechts van de sieradenkolom.
  - Klik met een drankje op je cursor = erin.
  - Klik leeg = eruit.
  - Shift+klik een drankje in je inventaris = in de riem.
- Alleen drinkbare dingen passen (geen steen e.d.).
- De toetsen kun je veranderen in Opties → Besturing → Vadsrijk.

### 6. Balans (één plek)
```
/guhs vadsrijk balans toon
/guhs vadsrijk balans items budget_sieraad 0.8
```
- `toon` laat de sectie `[items]` zien.
- Het tweede voorbeeld zet het sieradenbudget op 80 %, live.
- Alle sleutels en waarom: `NOTITIES_T_ITEMS.md` → "Balans".

## Rijdieren aankleden, 3 nieuwe rijdieren en huisdieren met een rol (T-RIJDIER)

Alles in het Vadsrijk testen (`/guhs vadsrijk`). Eerst even alles geven:
```
/guhs vadsrijk mount alle
/guhs vadsrijk rijdier garderobe alles
/guhs vadsrijk huisdier alle
```

### 1. Drie nieuwe landrijdieren
```
/guhs vadsrijk mount rijd duinhupper
/guhs vadsrijk mount rijd wolkwalkalfje
/guhs vadsrijk mount rijd zwamzwijntje
```
- **Duinhupper** (strandhaas, lange oren): hupt bij het lopen, springt het allerhoogst (bijna 6 blokken), iets langzamer.
- **Wolkwalkalfje** (wolkwalvisje op een eigen wolkje): zweeft, valt zacht en kan in de lucht nog een keer springen. Vinnetjes en staart wapperen.
- **Zwamzwijntje** (biggetje met een rode paddenstoel als zadel): stevig. Test de eerste tik:
  ```
  /guhs vadsrijk rijdier schrik
  /guhs vadsrijk rijdier schrik
  ```
  Eerste keer: "schudt de tik van zich af", je blijft zitten. Tweede keer (binnen 10 s): het rent weg.
- Druk **N**: de lijst heeft nu 8 rijdieren (kleinere rijen), de nieuwe staan er als silhouet bij als je ze nog niet hebt, met waar je ze haalt.

Hoe je ze echt krijgt (zonder dev):
- Duinhupper: vind 10 verstopte knuffels (alle zones samen). Het scherm toont "Al gevonden: x / 10".
- Wolkwalkalfje: 20 Gouden Veren, knop **Ruilen** in N. Veren geven: `/guhs vadsrijk rijdier veren 25`
- Zwamzwijntje: versla de Opperzwam (zone 1).
- Nieuwe Pluimguh-kleuren: **Kristal** (versla Spiegelzucht, zone 2) en **Lampion** (12 Gouden Veren, knop Ruilen bij de kleur).

### 2. Aankleden (N, rechts het knopje "Aankleden")
```
/guhs vadsrijk rijdier garderobe alles
```
Druk N, kies een rijdier, klik **Aankleden**. Drie rijen vakjes:
- **Vacht**: per rijdier 3 tot 5 kleuren (bijv. Sneeuwhert, Nachthert, Zonnewolf, Suikerspin-schaapje, Munthuisje voor Slakkie, Glimzwam voor het biggetje). De Pluimguh houdt zijn eigen kleurvakjes onder het plaatje.
- **Patroon**: vlekjes, streepjes, hartjes, sterretjes, glitter (glitter licht op in het donker).
- **Tuig**: zadeldekje, bloemenkrans, belletjes, hoge hoed, lampionnetjes (gloeien), kroontje. Het × -vakje = uittrekken.

Wat moet er gebeuren:
- Klik een vakje: het draaiende rijdier past het meteen (ook als je het nog niet hebt).
- Knop **Aantrekken**: daarna roept X het rijdier in die look. Zit je erop, dan verandert het meteen.
- Vacht en patroon veranderen NOOIT de snelheid of sprong.
- Zonder garderobe (`/guhs vadsrijk rijdier garderobe reset`): de vakjes hebben een "?", de tooltip zegt waar je het haalt.
  Bij de stal (`/guhs vadsrijk mount stal`) staat dan **Kopen (prijs)**; bij veren-dingen staat **Ruilen (n veren)**.

Snel een look op een rijdier zetten:
```
/guhs vadsrijk rijdier look knuffelhert vacht nacht
/guhs vadsrijk rijdier look knuffelhert patroon sterretjes
/guhs vadsrijk rijdier look knuffelhert tuig hoedje
/guhs vadsrijk rijdier look guhwolf tuig lampionnetjes
/guhs vadsrijk rijdier look pluimguh patroon glitter
/guhs vadsrijk rijdier look zwamzwijntje tuig geen
```
Let op (graag in de devclient kijken): zit het hoedje/de krans goed op het hoofd, hangen de belletjes voor de borst, hangen de lampionnetjes netjes aan de zijkant, ligt het dekje op het zadel? Bij elk rijdier anders; de plekken staan in `UiterlijkLaag.ANKERS`.

Waar alles vandaan komt (prestaties, bazen, geheimen) en wat je al gehaald hebt:
```
/guhs vadsrijk rijdier bronnen
/guhs vadsrijk rijdier garderobe inhalen
```
Een los ding geven: `/guhs vadsrijk rijdier geef tuig:kroontje` (of `patroon:hartjes`, `vacht:guhwolf:zon`).
Tuig bestaat ook als item (`vr_rijtuig_hoedje` enz.): rechtsklik = in je kast.

### 3. Huisdieren met een rol (P)
Huisdieren vechten NIET mee en worden nooit geraakt. Elk huisdier is **Steuner** of **Snuffelaar** en heeft een eigen kracht + een klein "Altijd"-bonusje. Ze worden level 1-10 door gevechten (baas = 10 xp).

Druk **P**: rechts staat nu Level x/10 met een balkje, een gekleurd label (roze Steuner / geel Snuffelaar), de kracht en wat die doet, en "Altijd: ...". Het naambordje "<huisdier> van <speler>" is hetzelfde gebleven.

Testen per huisdier (`laag` = Vads naar 30 %, energie 0, je telt als "in gevecht", cooldowns klaar):
```
/guhs vadsrijk maatje test klodderreus
/guhs vadsrijk maatje laag
```
-> binnen 1 s: "geeft je een klodderschildje!" (schild, slijm-deeltjes, hartjes).
```
/guhs vadsrijk maatje test koning_klodder
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje gevecht uit
```
-> buiten gevecht loopt je Vads elke seconde op (hartjes boven je hoofd).
```
/guhs vadsrijk maatje test galmwolf
/guhs vadsrijk maatje laag
```
-> je energie komt terug (muzieknootjes).
```
/guhs vadsrijk maatje test piepje
/guhs vadsrijk maatje strooi
```
-> 5 hoopjes goud 3 blokken verderop; Piepje raapt ze voor je op (alleen jouw buit).
```
/guhs vadsrijk maatje test kruinroofvogel
/guhs vadsrijk maatje snuffel
```
-> loop in de buurt van een geheim (knuffel, schat, aanwijzing) dat je nog niet hebt: elke paar seconden een gouden spoortje van het huisdier naar het geheim, en één keer "ruikt iets! Volg het gouden spoor...". `snuffel` zegt waar het dichtstbijzijnde geheim ligt.
```
/guhs vadsrijk maatje test vahoegje
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje vahoegje middel
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje vahoegje auto
```
-> klein: een roze vlammetje geneest je. Middel (na zone 5): Vahoegje wordt zichtbaar groter, grotere vlam + schildje. In P staat "Klein draakje" of "Jonge draak".

Levels: `/guhs vadsrijk maatje level 10` (kracht ~45 % sterker) of `/guhs vadsrijk maatje xp 50` (level-op berichtje + groene deeltjes).

## Opslag + HUD (T-OPSLAG): knapzak, materiaaltas, kluis, buffs, doelwit, gietbalk

Alles werkt alleen **in het Vadsrijk** (met je Vadsrijk-inventaris). Buiten het Vadsrijk gaat er niets in of uit.

### 1. Knapzak (per karakter): toets **I**
- Druk op **I** in het Vadsrijk. Of klik op het knopje **▤** links naast je gewone inventaris (E).
- `/guhs vadsrijk opslag knapzak` (of `... knapzak 2` voor vak 2)
- **Kijk naar:** een kistscherm met 3 rijen. Erboven staan **flapjes 1-5**. Rechts staan 3 knoppen: **Sorteren**, **Materialen ➜ tas** en **Materiaaltas**.
- **Wat moet er gebeuren:**
  - Begin: alleen flapje **1** is open. De andere tonen "?" of "+".
  - Hover over een dicht flapje: je ziet hoe je het krijgt (verhaal zone 1, verhaal zone 2, kopen, beloning later). Wat al klaar is, staat groen met ✔.
  - Klik op het "+"-flapje: de tekst vraagt "Klik nog een keer". Klik nog eens: je betaalt **25 zilver** en het vak gaat open.
  - Te weinig munten? Dan zie je een rode melding en gebeurt er niets.
  - Wisselen van vak: klik op een flapje, of druk **Ctrl+1..5**.
  - Druk nog een keer op **I**: het scherm gaat dicht.
- Vakken testen zonder verhaal:
  - `/guhs vadsrijk opslag knapzak ontgrendel proef1` → je krijgt een vak erbij (in de chat).
  - `/guhs vadsrijk opslag knapzak ontgrendel proef2`, enz. Er zijn maximaal 5 vakken.
  - `/guhs vadsrijk opslag knapzak reset` → de vakken gaan weer dicht. **De spullen blijven bewaard.**
  - Munten nodig? `/guhs vadsrijk munten 100000` (= 10 goud, in brons).

### 2. Materiaaltas (accountbreed, onbeperkt stapelen)
- Open hem met de knop **Materiaaltas** in je knapzak, met het knopje **✿** links naast je inventaris, of met `/guhs vadsrijk opslag tas`.
- **Automatisch oprapen:**
  - `/guhs vadsrijk opslag strooi`: er vallen 6 stapels rond je neer. Loop erdoorheen.
  - Steentjes, draad, knabbels en kristallen gaan **meteen in de tas**. Ze verschijnen niet in je inventaris. Boven je hotbar zie je "+5 Verbetersteentje in je Materiaaltas (5)". Je hoort een zacht tas-geluidje.
  - Het **brood** is geen materiaal. Dat gaat gewoon in je inventaris.
  - Werkt ook met een **volle inventaris** (vul je inventaris eerst met troep).
- **In het scherm:**
  - Je ziet een raster met alle soorten en hun aantal. Grote getallen worden afgekort (1.2k).
  - Er is een zoekvak.
  - Linksklik = een stapel aan je muis. Rechtsklik = de helft. Shift-klik = rechtstreeks naar je inventaris.
  - Klik met een materiaal aan je muis op het raster = terug erin. Shift-klik op een materiaal in je inventaris = erin.
  - **Alles opbergen** = alle materialen uit je inventaris gaan in de tas.
  - **Oprapen: aan/uit**: zet je het uit, dan gaan opgeraapte materialen weer gewoon in je inventaris.
- **Kristallen:** ruw en geslepen staan apart, als twee soorten.
- **De Smid:** die pakt Verbetersteentjes, Herrolknabbels en Pluisdraad **zelf uit je tas**. Je hoeft ze dus niet eerst te pakken.
  - Test: `/guhs vadsrijk opslag materiaal 50` en ga dan verbeteren bij de Smid.
- `/guhs vadsrijk opslag stand` → in de chat zie je hoeveel er in je knapzak, kluis en tas zit.

### 3. Kluis bij de bankier (accountbreed, in elke stad)
- Praat met een van deze bankiers. Je krijgt de keuze **"Naar mijn kluis"**.
  - **Zwamzoet:** de Bankier (gouden guh met monocle, in de bank).
  - **Knuffelkamp:** de Kok (zijn voorraadkast).
  - **De Haven:** de Havenmeester.
  - **Kandijkom:** de Veilingmeester (in het bankgebouw).
  - **Honingtak:** de Juwelier.
- `/guhs vadsrijk opslag bank` → opent de kluis, alsof je bij een bankier staat.
- **Kijk naar:** een kistscherm met 6 rijen (54 vakjes) en flapjes erboven.
  - Vak 2 kost 10 zilver, vak 3 50 zilver, vak 4 2 goud en vak 5 5 goud. Kopen gaat op dezelfde manier: klik op het "+"-flapje, en daarna nog een keer.
  - `/guhs vadsrijk opslag bank tabs 4` → alle vakken zijn open (dev).
- Loop je meer dan 8 blokken weg, dan gaat de kluis vanzelf dicht.

### 4. Dupe-veiligheid (even proberen te breken!)
- Pak iets op in de knapzak of kluis en houd het aan je muis. Doe dan een van deze dingen:
  - wissel van vak;
  - druk op Esc;
  - ga door een portaal terug naar huis;
  - log uit.
- **Wat moet er gebeuren:** het ding komt altijd precies één keer terug, in je Vadsrijk-inventaris. Nooit dubbel en nooit weg.
- Ga met de knapzak open terug naar huis: het scherm sluit vanzelf. **Thuis** opent I niets ("Dat kan alleen in het Vadsrijk").

### 5. HUD: buff-rij, doelframe, gietbalk
- **Buff-rij** (boven de Vads- en energiebalk):
  - `/guhs vadsrijk opslag hud alles` → je krijgt alle statussen die geen schade doen.
  - Links staan de **buffs** (groen randje), rechts de **debuffs** (rood randje). Elk icoontje heeft een eigen pixelplaatje, een donkere "zandloper" die volloopt, en de seconden in klein schrift.
  - Een nieuwe status flitst 2 s met zijn naam boven het icoontje.
  - Los: `/guhs vadsrijk opslag hud buff vahoeg 20` (of `bevroren`, `knuffelschild`, `snelpootjes`, `plakkerig`...).
- **Doelframe** (bovenaan in het midden):
  - Kijk naar een wezen (tot 24 blokken) of sla er een.
  - Je ziet: element-icoontje, (Elite), naam, "Nv X", een HP-balk met getallen, en eronder de statussen op dat wezen (als je het geslagen hebt).
  - Het blijft nog 5 s staan nadat je bent weggekeken.
  - Bij een baas met de grote baasbalk is het er niet.
  - `/guhs vadsrijk opslag hud doel` → het dichtstbijzijnde wezen wordt je doelwit (en krijgt even "Brandend" om te tonen).
- **Gietbalk:**
  - **Losknuffelen** (toets U) toont nu een balk "Losknuffelen..." boven de buff-rij. Als je beweegt of geraakt wordt, kleurt hij rood met "Onderbroken!".
  - `/guhs vadsrijk opslag hud giet 3` → een proefbalk van 3 s.
  - `/guhs vadsrijk opslag hud baasgiet 4` → een rode baas-gietbalk ("Grote Plonsstamp") onder de baasbalk, met een **gouden rand** (= onderbreekbaar). De questlijst rechtsboven schuift dan iets omlaag.
- **GUI-schaal:** check alles op GUI-schaal 2, 3 en 4. Er mag niets over de skills, de balken of de hotbar heen vallen.

### Toetsen
- **I** = knapzak. Ctrl+1..5 = van vak wisselen in de knapzak of kluis.

## Kledingkast, vleugels en effecten (T-KLEDING)

**Openen:** toets **O** (overal, ook thuis), of het tabje "Kledingkast" bovenaan Rijdieren/Huisdieren/Verkenningsboek, of de kleermakers (Zwamzoet, de breister in Kandijkom, de kleermaker in zone 3): keuze **"Naar de Kledingkast"**, of:
```
/guhs vadsrijk kledingkast
```
Alles is **per account** (niet per karakter): wat je ontgrendelt, blijft voor al je ridders.

### Snel alles testen
```
/guhs vadsrijk kleding alles
/guhs vadsrijk kleding kast
```
→ alles ontgrendeld (ook de "later"-dingen) + alle Vadsrijk-harnaslooks. Links zie je jezelf draaien met de muis; klik in een categorie op **Kies**: je ziet het meteen aan je poppetje.
- Overzicht wat je hebt + bron: `/guhs vadsrijk kleding lijst`
- Alles weer weg (ook voor opnieuw testen): `/guhs vadsrijk kleding wis`
- Eén ding geven / afpakken / aandoen: `/guhs vadsrijk kleding geef vleugel:kristal`, `.../kleding neem vleugel:kristal`, `.../kleding kies kaap:sterren`

### Vleugels + zweven (alleen in het Vadsrijk)
```
/guhs vadsrijk kleding vahoegnir
```
→ zoals na Vahoegnir (roze, einde zone 3): titelkaart **"Vahoegveertjes"**, een tip, en je hebt ze meteen om.
- **Zweven:** spring van iets hoogs en **houd spatie ingedrukt** → je zakt heel langzaam (nooit omhoog). Vooruit ingedrukt = ongeveer loopsnelheid. Geen valschade bij landen.
- Kijk of het mag: `/guhs vadsrijk kleding zweef` (zegt: ontgrendeld, welke vleugels, mag hier, zweeft nu).
- **Mag NIET** in bazen-arena's en kerkers (Spiegelburcht, Zeppelinwerf, Hart van de Boom): daar val je gewoon.
- **Thuis** zie je je vleugels wél op je rug, maar zweven doet niets.

Elke vleugel één eigen beweeg-trucje (geen gevechtsstats). Test met `kleding kies <id>`:
| vleugel | id | trucje | hoe test je het |
|---|---|---|---|
| Vahoegveertjes | `vleugel:vahoegveertjes` | gewoon zweven | spatie vasthouden in de lucht |
| Kristalvleugels | `vleugel:kristal` | dubbele sprong | spring, en tik in de lucht nog eens op spatie (1x per sprong) |
| Honingbijvleugeltjes | `vleugel:honingbij` | sneller zweven | zweven + W: duidelijk vlotter vooruit |
| Goudveren Vleugels | `vleugel:goudveer` | langer zweven | je zakt ongeveer half zo snel |
| Schemervleugels | `vleugel:schemer` | luchtdash | in de lucht nog eens spatie tikken: zwiepje vooruit |
| Pluisvleugeltjes | `vleugel:pluis` | zachte landing | spring van hoog zónder zweven: geen schade + roze plofje |
| Lampionvlieger | `vleugel:lampion` | langer zweven | (zone 4, komt later) |
| Sterrenstofvleugels | `vleugel:sterrenstof` | dubbele sprong | puzzelbeloning (t-puzzels) |
| Poollichtvleugels | `vleugel:poollicht` | langer zweven | puzzelbeloning |
| Lantaarnmotvleugels | `vleugel:lantaarnmot` | zachte landing | puzzelbeloning |

**Wat je moet zien aan de vleugels:** stilstaan/lopen = schuin naar achteren op je rug, springen/vallen = verder uitgeklapt, zweven = klapperen (op en neer). Verder dan 24 blokken: stilstaand model (LOD). Verf (tab Verf) kleurt vleugels + hoofdding.

**Waar komen ze vandaan (echt spelen):** Vahoegveertjes = Vahoegnir roze; Kristal = Splinterpieken 100% (B-boek); Honingbij = Schemerwortel 100%; Goudveren = 25 Gouden Veren verzameld; Schemer = zeldzaam (3%, laatste baas 8%) van een baas in Vahoeg-vorm; Pluis = geheim (één zweefvlucht van 100+ blokken omlaag, bv. van de kruin van de Schemerwortel naar beneden); Lampion = zone 4.

### Geheimen testen
```
/guhs vadsrijk kleding geheim zweefvlucht
/guhs vadsrijk kleding geheim kleermakers
```
→ Pluisvleugeltjes / Reiscape. Echt: Reiscape = de Kledingkast openen bij alle drie de kleermakers (Zwamzoet, Kandijkom, zone 3).

### Puzzelbeloningen (van t-puzzels)
```
/guhs vadsrijk kleding puzzel pz_vleugels_sterrenstof
/guhs vadsrijk kleding puzzel pz_cape_mos
```
→ doet hetzelfde als het oplossen van die puzzel. In de Kledingkast staan ze als "???" tot je ze hebt.

### Cape (rug-vak, over de stat-cape heen)
`kleding kies kaap:knuffelorde` (ook `roze`, `sterren`, `bloesem`, `reis`, `sneeuwvlok`, `honingraat`, `mos`) → in F5 een cape op je rug. Vleugels én cape tegelijk kan niet (één rugvak). Met vleugels om wordt een gewone cape verborgen.

### Hoofd, harnas, naam, titel
- **Hoofd:** `accessoire:oortjes`, `strikje`, `paddenhoedje`, `kristaltiara`, `honingkroon`, `feestmuts` (+ `lampionspeld` later). Zit ook netjes bovenop een helm.
- **Harnas (transmog):** tab Harnas, per stuk `<` `>`: "Je eigen stuk" / "Verborgen" / elk Vadsrijk-harnas dat je ooit in je tas had. Alleen zichtbaar **in het Vadsrijk** en alleen als je in dat vak echt iets draagt. Helm verbergen = "Verborgen" bij Helm.
- **Naamkleur:** tab Naamkleur → je naam boven je hoofd, in de tablijst en in de chat in die kleur (titel erachter blijft).
- **Titel:** tab Titel = de bestaande titels (zelfde als in de Guhdex).

### Effecten (één van elk actief)
- **Opvrolijken** (bij JOUW laatste klap op een wezen; bovenop de roze plof): confetti, regenboogje, hartjesballon, bloemenregen, knuffelregen, guh-giechel, "Njeg!"-letters. Snel kijken zonder vechten:
```
/guhs vadsrijk kleding plof plof:njeg
/guhs vadsrijk kleding plof plof:regenboog
```
- **Spoor** (lopen of rijden): bloemblaadjes, sneeuwvlokjes, sterretjes, honingdruppels, muzieknootjes.
- **Aura** (pas na ~3 s stilstaan, weg zodra je beweegt): roze gloed, pluisjes, vuurvliegjes.
- **Schuifjes** links onder in de Kledingkast: **Anderen: Alle/Minder/Uit** en **Eigen: Alle/Minder/Uit** (bewaard in `config/guhs_kleding.properties`). In een bazen-arena: sporen minder, aura's uit.
- Teller-beloningen testen: `/guhs vadsrijk kleding kills 1000` en dan `/guhs vadsrijk kleding check` → guh-giechel, paarse naam, Njeg!-letters.

## Spiekbrief: Ambachten (t-ambacht)

Alles hieronder werkt in het Vadsrijk. Commando's beginnen met `/guhs vadsrijk ambacht` (op 2). Toets **Y** = het Ambachtenboek
(ook als tabblad naast Rijdieren / Huisdieren / Verkenningsboek).

### 0. Snel alles klaarzetten
```
/guhs vadsrijk ambacht level alle 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht boek
```
- **Kijk:** 9 tegels (Mijnen, Houthakken, Plukken, Vissen, Imkeren, Smeden, Naaien, Koken, Brouwen), elk "Niveau 50 / 50" met een xp-balk.
- Klik op een tegel: je springt naar de recepten van dat ambacht.
- Terugzetten: `/guhs vadsrijk ambacht reset`.

### 1. Verzamelen (Mijnen, Houthakken, Plukken, Vissen)
```
/guhs vadsrijk ambacht reset
/guhs vadsrijk ambacht tp z1_pluk_kamp
```
- **Kijk:** rondom zie je kleine plantjes (azalea-bosjes) staan. Alleen JIJ ziet ze (een tweede speler ziet zijn eigen plekken).
- Kijk ernaar: onder je kruisje staat "Plukken · tier 1 (rechtsklik)".
- **Rechtsklik** = het spelletje (2-4 s):
  - Plukken: houd rechtsklik vast, laat los in het groene stuk.
  - Mijnen: drie keer tikken op het roze.
  - Houthakken: twee keer snel tikken.
  - Vissen: wacht op "BEET!" en klik meteen.
- Perfect = "extra vadsig", goed = "vadsig", niks doen = na 4 s gewoon. **Sluip + rechtsklik** = meteen gewoon oogsten.
- **Daarna:**
  - de plek verdwijnt voor jou en komt na 1-2 min ergens anders terug;
  - boven de hotbar "+30 Plukken-xp";
  - het materiaal zit in je tas.
- Andere zones / ambachten:
```
/guhs vadsrijk ambacht tp z1_mijn_grot_1
/guhs vadsrijk ambacht tp z1_hout_bos_1
/guhs vadsrijk ambacht tp z1_vis_vijver
/guhs vadsrijk ambacht tp z2_mijn_kloof_1
/guhs vadsrijk ambacht tp z3_vis_moeras
```
- **Te laag niveau** (bijv. zone 2 met Mijnen 1): de plek is half doorzichtig, met "niveau 20 nodig".
- Snel testen:
  - lijst van plekken in de buurt: `/guhs vadsrijk ambacht plekken`;
  - alles meteen weer vol: `/guhs vadsrijk ambacht vul`;
  - de dichtste plek oogsten zonder spelletje: `/guhs vadsrijk ambacht oogst`.
- **Zeldzame plek:** `/guhs vadsrijk ambacht zeldzaam`.
  - De dichtste plek wordt goudkleurig en glinstert, met "Glinsterplek: driedubbele oogst!".
  - Oogsten geeft 3x, en een kans op Glinstergruis, Oerbast, Gouden Klaver of Parelschelp.
- **Vissen:** 's nachts en bij regen bijten andere soorten. Een nieuwe soort geeft een chatregel "Nieuwe vis gevangen".
- **Anti-AFK:** blijf >10 min op dezelfde plek oogsten en je krijgt de helft (met een melding).
- **Uitgerust:** `/guhs vadsrijk ambacht uitgerust 600`.
  - Links onder staat "Uitgerust: 10 min dubbele ambachts-xp".
  - Oogsten geeft dubbele xp ("uitgerust: dubbel!").
  - Offline bouwt dit vanzelf op: 1 s per 6 s weg, max 2 uur.

### 2. Imkeren (zone 3)
```
/guhs vadsrijk ambacht tp z3_korf_bloesemdal
```
- **Kijk:** bijenkorfjes (half doorzichtig) bij de bloemen: "Lege korfplaats: klik om je korf te zetten".
- **Klik:** je korf staat (een hint-kaartje verschijnt) en de korf wordt een echte bijenkorf.
- Gele stipjes (bijen) vliegen tussen de korf en de bloemen.
- Raten vullen in echte tijd: 1 per ~10 min, sneller met meer bloemen, max 8. Snel vullen: `/guhs vadsrijk ambacht vul`.
- **Oogsten** = rook geven: houd vast en laat los in het groen.
  - Te vroeg = "chagrijnige bijen": je plakt even. Nooit schade.
  - Je krijgt honing per raat (Bloesemhoning / Schemerhoning / Wortelhoning, één per wei) + bijenwas + soms Koninginnengelei.
- Meer korven: 1 op niveau 1, 2 op 15, 3 op 30, 4 op 45.

### 3. Werkplaatsen in de steden (leermeester-guhs)
```
/guhs vadsrijk ambacht npcs
/guhs vadsrijk ambacht tpnpc amb_kok_z1
```
- De guhs staan in een ring rond het plein: Zwamzoet (8 + bordguh), Kandijkom (4 + bord), Honingtak (4 + bord + Imkerguh Zoemie bij de Raatweide).
  - Ze verschijnen pas als je in de buurt van het plein komt (zoeken zelf een vrij plekje).
  - Staat er een raar? Dan zoek je opnieuw met `/guhs vadsrijk ambacht scan` (en loop weg en terug).
- **Klik op een guh:** een praatscherm met de keuzes Werkplaats / Receptenboek / Vertel eens... / Dag!
  - Verzamelmeesters (Bonk, Spaan, Madelief, Dobbertje, Zoemie) hebben "Waar vind ik plekken?": uitleg + de dichtste plek met x/z.
- Zonder lopen: `/guhs vadsrijk ambacht werk koken` (of `smeden` / `naaien` / `brouwen`).

### 4. Koken (eten met buffs)
```
/guhs vadsrijk ambacht level koken 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht werk koken
```
- Links de recepten (grijs = nog te laag), rechts wat je nodig hebt (groen = genoeg).
- **Maak:** een balkje met een roze stuk; druk "Nu!" als het streepje in het roze staat.
  - Perfect = hogere Vadsigheid.
  - De tooltip toont "Vadsigheid ▮▮▮▮▮▮▮▮▯▯ 84%" en "Gemaakt door <jij>".
- **Maak 10x:** zonder spelletje, altijd 60%.
- **Eet** (rechtsklik):
  - links onder staat "Eten: Klodderpudding (29m 59s)";
  - in het Verkenningsboek van je stats zie je Vads +60 en Kracht +3 (x1,0 tot x1,3 door de Vadsigheid);
  - één buff tegelijk, 30 min, en hij blijft bij flauwvallen.
- **Feestmaal:** iedereen in je party binnen 10 blokken krijgt de buff + "Gezellig" (Kritiek +1, Heal-kracht/Elementschade/Elementweerstand +2%).
- Snel: `/guhs vadsrijk ambacht eten ridderontbijt` (dit geeft ook +5% gevechts-xp).

### 5. Brouwen (drankjes + kristallen slijpen)
```
/guhs vadsrijk ambacht werk brouwen
```
- Gebrouwen drankjes: dezelfde getallen als in de winkel, maar tot +30% door de Vadsigheid. Ze zitten in dezelfde 10 s drankjes-cooldown.
- **Kristal slijpen (geslepen):** kies rechts een kristalsoort (het getal = hoeveel ruwe je hebt). Drie ruwe worden één geslepen.
- **Stralend:** drie geslepen + 1 Glinstergruis worden één stralend (Brouwen 40).
- **Honingelixer:** Vads + energie, en het wast Plakkerig en Gesporen af.

### 6. Smeden + Naaien: Maatwerk en Meesterwerk (fase B)
```
/guhs vadsrijk ambacht level smeden 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht werk smeden
```
- **Maatwerk:** kies bijv. "Amberkoperen Zwaard".
  - Kies 2 bonussen (de knopjes met de stat-namen; groen streepje = gekozen) en druk Maak.
  - Je krijgt een **Episch** stuk met jouw 2 bonussen + willekeurige, en minstens 1 sokje.
  - De tooltip toont "Maatwerk", "Zelf gekozen: ...", "Maatwerk telt niet voor een setbonus" en "Gemaakt door".
  - Met 100% Vadsigheid zitten je gekozen bonussen bovenaan hun bereik, en de naam wordt "Vadsig ...".
- **Naaien** maakt de broek + schoentjes, Smeden het zwaard / de boog / de staf / de helm / de borstplaat.
- **Meesterwerk-sieraden** (BiS) hebben een bazenkern nodig (zit in `geef alles`).
  - Kies 3 bonussen + 1 eigenschap, bijv. "Schade tegen bazen +8%".
  - Je krijgt een **Legendarisch**, gebonden sieraad. Draag het met rechtsklik.
- **Bazenkernen:**
  - elke baas 15% per helper (Zwamkern zone 1, Splinterkern zone 2, Wortelkern zone 3);
  - elites 3% een Kernsplinter;
  - 5 splinters = 1 kern in de smidse.
- Snel een stuk: `/guhs vadsrijk ambacht maatwerk zwaard 3` / `/guhs vadsrijk ambacht maatwerk ring 3`.

### 7. Bestelbord (fase B)
```
/guhs vadsrijk ambacht bestel
```
- 3 bestellingen van stadsguhs ("Bakker Bram wil 10 Pluisijzer.").
  - Ze passen bij jouw niveaus: met alles op 1 krijg je beginnersbestellingen.
  - Elke dag om 06:00 nieuwe.
- **Leveren** (als het balkje groen is) geeft ambachts-xp + munten.
- **Ruilen:** 1 gratis per dag.
- Na 12 bestellingen in een week: de **Ambachtskist** (rechtsklik: kernsplinters, iets zeldzaams, 5 zilver).
- Snel testen:
  - een bestelling klaarzetten (inclusief de spullen): `/guhs vadsrijk ambacht bestel klaar 0`;
  - de volgende levering geeft de kist: `/guhs vadsrijk ambacht bestel week`;
  - nieuwe set: `/guhs vadsrijk ambacht bestel nieuw`;
  - kist direct: `/guhs vadsrijk ambacht kist`.

### 8. Ranglijst (Ambachtsbord) + website (fase C)
- Bij de bordguh (Klerk Krabbel / Kriebel / Kraaltje) of het boek, tab "Ranglijst":
  - de top 10 per ambacht + "Totaal";
  - onderaan "Jij staat op plek X van de Y". Nooit meldingen in de chat.
- **Website-export:** `/guhs vadsrijk ambacht export`.
  - Dit schrijft `guhs-web/live/hiscores/ambacht_<ambacht>.json` + `ambacht_totaal.json` + `ambachten_recepten.json` (het pad staat in de chat).
  - Het gebeurt ook vanzelf elke 5 min en bij uitloggen.

### 9. Verhaaltjes + titels + spullen naar huis (fase C)
- Klik een leermeester → "Vertel eens...".
  - Per ambacht 5 stapjes, op niveau 1 / 10 / 20 / 35 / 50 (bijv. Koken: Oma Truus' receptenboek, Smeden: het Aambeeld van de Orde).
  - Elke stap geeft munten en een beetje xp.
  - De laatste stap stuurt een aandenken met de **Post** naar huis.
- Snel: `/guhs vadsrijk ambacht level koken 50` en `/guhs vadsrijk ambacht verhaal koken 4`, dan "Vertel eens...".
- Zonder de guh te zoeken: `/guhs vadsrijk ambacht npc amb_kok_z1`.
- **Naar huis (alleen versiering):**
  - Naaien maakt kokschort, vissershoedje, tuinhoedje, imkershoed, vaandel en klodderreus-knuffel;
  - Smeden maakt mijnhelmpje, Opperzwambeeld en Mini Koning Klodder;
  - alles gaat met de Post: buiten het Vadsrijk komt het in je gewone tas.
- **Titels:** Leerling-<ambacht> op niveau 25, Gezel-<ambacht> op 50, en "Ambachtskenner" voor 100% van het Ambachtsboek.
- **Verkenningsboek (B):** een eigen pagina "Ambachten" met de gevonden plekken per zone, vissen, honing en recepten. Telt NIET mee voor de zonepercentages.

### 10. Markt en winkel
- Materialen stapelen tot 64 (alleen dezelfde kwaliteit stapelt). Op de markt zet je dus een hele stapel tegelijk te koop.
- De winkelguhs kopen materialen, gerechten en drankjes nu ook:
  - T1 3 brons per stuk, T2 8, T3 15, zeldzaam 60;
  - vadsig x1,5, extra vadsig x2.

## Spiekbrief: Puzzels en geheimpjes (T-PUZZELS)

22 puzzels en geheimpjes in zone 1-3 (8 + 7 + 7). Elk heeft een beloning (Vadsmunten, soms een gescheurde bladzijde over de Keizer, spullen of iets voor je kledingkast). Ze staan in het **Verkenningsboek** onder **"Puzzels en geheimpjes"** en tellen mee voor het zonepercentage.

### Zo werkt het (voor jou)
- **Waar staan ze?** Een puzzel verschijnt vanzelf de eerste keer dat je binnen ~70 blokken van zijn plek komt. Dat is een vlak plekje vlak bij een kampvuur of POI. Daarna blijft hij daar staan.
- **Onderaan je scherm** staat steeds wat je moet doen (de "puzzeltip constant in beeld"). Geheimpjes zeggen niks.
- **Na 3 foute pogingen** krijg je tip 1 in de chat, **na 6** tip 2. Daarna staat de tip onderaan je scherm.
- **Opgelost?** Iedereen die vlakbij staat krijgt de beloning, maar **één keer per speler**. De puzzel zet zichzelf na ~20-30 s terug. Een half opgeloste puzzel zonder spelers in de buurt zet zich na 2 minuten terug.
- **Tijdritten en raadsels** zijn per speler (iedereen zijn eigen stopwatch of eigen antwoord).
- **Je antwoord op een raadsel** zien andere spelers niet in de chat.

### Dev-commando's (op 2)
| commando | wat het doet |
|---|---|
| `/guhs vadsrijk puzzel lijst` | alle puzzels: ✔ = jij hebt hem, met de plek (of "nog niet geplaatst" + anker) |
| `/guhs vadsrijk puzzel tp <id>` | naar de puzzel (staat hij er nog niet? Dan naar het anker; hij verschijnt binnen 1 s; doe dan nog eens tp) |
| `/guhs vadsrijk puzzel los <id>` | lost hem op via de echte invoer (klikken/platen/duwen/chat), dus je krijgt ook de beloning |
| `/guhs vadsrijk puzzel reset <id>` | zet de puzzel terug + vergeet je foute pogingen (tips weer weg) |
| `/guhs vadsrijk puzzel hint <id>` | toont uitleg, tip 1, tip 2, de boek-aanwijzing (en de antwoorden van een raadsel) |
| `/guhs vadsrijk puzzel vergeet <id>` / `vergeet alles` | jouw beloning vergeten (dan krijg je hem opnieuw) |
| `/guhs vadsrijk puzzel nacht` | sterrenpuzzels doen alsof het nacht is (aan/uit) |
| `/guhs vadsrijk puzzel plaats <id>` / `plaats <id> opnieuw` | nu plaatsen / een nieuw plekje zoeken |
| `/guhs vadsrijk puzzel hier <id>` | een testkopie vlak voor je neerzetten (overal, ook buiten het Vadsrijk; telt niet als de echte plek) |
| `/guhs vadsrijk puzzel bladzijde <n>` | bladzijde n (1-9) lezen + krijgen |
| `/guhs vadsrijk puzzel herlaad` | de puzzeldata opnieuw lezen |

Snelle rondgang: `/guhs vadsrijk puzzel tp z1_kleurplaten` → wacht 1 s → nog eens → puzzelen → `/guhs vadsrijk puzzel lijst`.

### Zone 1: De Knuffelzwammen
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z1_kleurplaten` | De Kleurplaten | drukplaten in volgorde | bij kampvuur Klodderweide 1 | **groen, roze, blauw, geel** (stap erop) | 4 zilver + bladzijde 1 |
| `z1_zwammenlied` | Het Zwammenlied | klankstenen (lied naspelen) | bij kampvuur Sporenwoud 1 | klik eerst de voorspeelsteen; dan **links, rechts, midden, rechts, voorste** | 5 zilver + 2 verbetersteentjes |
| `z1_lichtstraal` | De Verdwaalde Lichtstraal | spiegels + lichtstraal | bij lichtstraal 1 (Sporenwoud) | straal: 2× omlaag, 1× rechts; spiegel linksonder en rechtsboven niet nodig | 6 zilver + bladzijde 2 |
| `z1_gebarsten_muur` | De Gebarsten Muur | **geheimpje** (barst) | bij kampvuur Druipgrotten 3 | klik op de barst in het muurtje → de muur zakt in | 3,5 zilver + bladzijde 3 |
| `z1_fluistersteen` | De Fluistersteen | raadsel (chat) | bij kampvuur Klodderweide 3 | klik de steen, typ **vads** | 3 zilver + herrolknabbel |
| `z1_sterrenveld` | Het Sterrenveld | sterrenbeeld ('s nachts!) | Hoedhupperwei | klik de sterrenkaart; het is een **hartje**: rij 1: 2+4, rij 2: 1+3+5, rij 3: 1+5, rij 4: 2+4, rij 5: 3; dan de knop | 7 zilver + **Sterrenstofvleugels** (ontgrendeling) |
| `z1_vuurtorenschuur` | Het Schuurtje bij de Vuurtoren | **geheimpje** (verstopte hendel) | Knuffelkamp, bij de vuurtoren | hendeltje **achter** het schuurtje tussen de struikjes → de deur gaat open | 2 zilver + Klodderreus-knuffel |
| `z1_symboolslot` | Het Tempelslot | symboolslot | op weg naar de Zwamtempel (kampvuur 1) | **hart, ster, maan**, dan de knop | 8 zilver + 3 verbetersteentjes |

### Zone 2: De Splinterpieken
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z2_ijsschuif` | De Ijsblokken | schuifblokken | bij kampvuur Knuffelmeer 1 | linkerblok 2× omlaag, rechterblok 2× omhoog (klik = duwen van je af); vast? de gele herstelknop | 18 zilver + bladzijde 4 |
| `z2_kristallied` | Het Galmlied | klankstenen | bij kampvuur Galmhellingen 1 | **midden, linksonder, linksboven, rechtsonder, rechtsboven, midden** | 20 zilver + 3 verbetersteentjes |
| `z2_kristalspiegels` | De Kristalspiegels | spiegels + lichtstraal | bij kampvuur Splinterkloof 3 | omhoog bij de 1e spiegel, rechts, omhoog, dan helemaal naar rechts | 24 zilver + bladzijde 5 |
| `z2_poollicht` | Het Poollichtveld | sterrenbeeld ('s nachts!) | bij kampvuur Knuffelmeer 2 | **ruit met een stip**: rij 1: 3, rij 2: 2+4, rij 3: 1+3+5, rij 4: 2+4, rij 5: 3; dan de knop | 26 zilver + **Poollichtvleugels** |
| `z2_lantaarnsprint` | De Lantaarnsprint | tijdrit (25 s) | bij kampvuur Galmhellingen 2 | klik de lantaarn, spring over de kristalpilaren naar de gouden eindplaat bovenop | 22 zilver + **Sneeuwvlokcape** |
| `z2_ijsgrot` | Het Bevroren Holletje | **geheimpje** (barst) | bij kampvuur Splinterkloof 1 | klik op de barst | 15 zilver + bladzijde 6 |
| `z2_raadselsteen` | De Bibberende Steen | raadsel (chat) | bij het Meerkampje | klik de steen, typ **ijs** | 16 zilver + 2 herrolknabbels |

### Zone 3: De Schemerwortel (alles beneden langs de Wortelweg)
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z3_wortelplaten` | De Wortelplaten | drukplaten | bij kampvuur Wortelweg 1 (oost) | **geel, blauw, roze, groen** | 45 zilver + 4 verbetersteentjes |
| `z3_bijenlied` | Het Bijenlied | klankstenen | bij kampvuur Wortelweg 2 | achterste rij 1+2, voorste rij rechts + links, achterste rij 3+4 | 50 zilver + bladzijde 7 + **Honingraatcape** |
| `z3_wortelschuif` | De Wortelknoesten | schuifblokken (3) | bij kampvuur Wortelweg 3 (west) | begin rechtsonder: 1× omlaag; dan de andere twee naar hun vakje | 55 zilver + 4 verbetersteentjes |
| `z3_lantaarnloop` | De Lantaarnloop | tijdrit (30 s) | bij kampvuur Wortelweg 4 | onderste rij naar rechts, rechterkant omhoog, bovenste rij naar links (de pilaren in het midden zijn doodlopend) | 60 zilver + bladzijde 9 + **Lantaarnmotvleugels** |
| `z3_mossymbolen` | Het Mosslot | symboolslot (4) | bij kampvuur Wortelweg 5 | **blad, druppel, zon, blad** | 50 zilver + **Moscape** |
| `z3_schorsdeur` | Het Schorshuisje | **geheimpje** (verstopte hendel) | bij kampvuur Wortelweg 6 | hendeltje achter het huisje | 40 zilver + bladzijde 8 |
| `z3_honingfluister` | De Zoemsteen | raadsel (chat) | bij de Mosvoet | klik de steen, typ **bij** (of bijtje/bijen) | 40 zilver + 3 herrolknabbels |

### Wat moet je zien
- **Bij aankomst:** een netjes vlak plekje met een echte vloer (mos, sneeuw of aangestampte modder per zone). Is er hoogteverschil? Dan lopen er traptreden naar het omringende land. Geen planten of struiken in de weg.
- **Drukplaten:** de plaat licht op als je erop stapt. Fout? Alles gaat uit met een bromtoon en je ziet "Hmm, dat klopt niet".
- **Klankstenen:** de voorspeelsteen speelt het lied en laat de stenen één voor één oplichten. Elke steen heeft zijn eigen toon.
- **Spiegels:** een geel lichtspoor (deeltjes) laat zien waar de straal nu loopt. Raakt hij het doel? Dan licht het doel roze op.
- **Schuifblokken:** klik = het blok schuift van je af. Een roze vakje licht op als er een blok op staat.
- **Tijdrit:** de lantaarn wordt goud en onderaan telt "⏱ Nog 24" af. Te laat: "De tijd is om!".
- **Sterren:** overdag zegt een steen "De sterrenstenen slapen nog...". 's Nachts (of met `puzzel nacht`) lichten ze op als je klikt. De sterrenkaart (houten blok) zet het sterrenbeeld als ★ in de chat.
- **Geheimpjes:** de barst of het hendeltje glinstert heel zachtjes (lichtniveau 3). Opgelost: de muur of deur gaat ~30 s open, met de titel "✦ Geheimpje gevonden".
- **Beloning:** een titel in beeld + chatregels: "+ 4 zilver", "+ een gescheurde bladzijde: ...", "+ ontgrendeld voor je kledingkast: ...". Een bladzijde lees je met rechtsklik.
- **Verkenningsboek:** onder "Puzzels en geheimpjes" (amethist-icoon) zie je per zone x/y. Geheimpjes heten "???" tot je ze vindt. De hint is een aanwijzing zoals "Bij het eerste kampvuur van de Klodderweide liggen vier gekleurde platen."

### De 9 gescheurde bladzijdes (de legende van de Lege Schaal)
1 Een gewone kleine Mika · 2 De eerste kaasknabbel · 3 Wat elke guh weet · 4 Lange armen · 5 Een gestolen kroontje · 6 De Lege Schaal · 7 Nooitgenoeg · 8 Het Oog · 9 Een eenzaam katje. Lezen: `/guhs vadsrijk puzzel bladzijde <n>`.

# B8. FEEDBACK RONDE 4: ELK PUNT, WAT ER GEDAAN IS EN HOE JE HET CONTROLEERT

Per punt uit je feedback: wat er veranderd is, de commando's en waar je op let. Alles is getest vanuit deze testclient
(v-int). De kaart is opnieuw gegenereerd, dus begin in een **nieuwe wereld** (zie Starten).

## 8.1 Zone-overgangen natuurlijker (u-overgang)
- **Gedaan:** één gedeelde open zee tussen alle zones: de zeebodem sluit aan, de waterkleur, lucht en mist verlopen in 8 stapjes,
  de wolkenzee van zone 3 rafelt uit, en er staan losse rotsjes in de zee.
- **Controleren:**
```
/guhs vadsrijk indeling rand wolkenkloof_noord
/guhs vadsrijk indeling plek haven_z2_z3_vertrek
```
- **Let op:** geen rechte wolkenrand, geen ineens andere waterkleur, geen onderwaterklif op een rechte lijn. F3: open zee =
  "Het Vadsrijk - De Zee". Meer plekken staan in §1 bij U-OVERGANG.

## 8.2 Zone 5: gebouwen episch, interieurs, figuranten (u-z5, later u-z5binnen)
- **Gedaan:**
  - de kathedraal is gotisch met diepte: steunberen, luchtbogen, pinakels, een roosvenster, diepe portalen, ribgewelven en
    grote kroonluchters;
  - de universiteit kreeg hoge gotische ramen, dakkapellen en een bibliotheekgalerij;
  - het paleis, de raadszaal, de academie en het station kregen reliëf;
  - de hele stad kreeg vensterkappen;
  - de figuranten zijn gefixt: geen stapel kleren meer en het goede model per soort.
- **Controleren:**
```
/guhs vadsrijk z5wereld plek kathedraal_plein
/guhs vadsrijk z5wereld plek kathedraal_schip
/guhs vadsrijk z5wereld plek universiteit_bibliotheek_galerij
/guhs vadsrijk z5wereld plek troonzaal_audientie
/guhs vadsrijk z5wereld plek markt_fontein
/guhs vadsrijk z5wereld bewoners
```
- **Let op:** de diepte van de gevels, de kroonluchters binnen en of de figuranten er als normale guhs uitzien.

## 8.3 Knuffelkamp: alle paden verlicht (u-deco)
- **Gedaan:** ~86 guh-lantaarnpalen langs alle kamppaden, om de ~14 blokken.
- **Controleren:** `/time set night`, dan `/guhs vadsrijk wereld plek kampvuur_kamp` en loop alle paden af.
- **Let op:** je ziet steeds de volgende paal, en er staat nooit een paal op het pad.

## 8.4 Luchtschip: hele schip klikbaar, vrij lopen, uitleg als het nog niet mag (u-luchtschip)
- **Gedaan:**
  - 12 klikvakken over het hele schip (romp, ballon, oortjes, staart);
  - lopen met WASD over het dek (de reling houdt je tegen);
  - bij een dichte lijn: "Volg eerst de hoofdquest: na ... kun je hier vliegen" + het level.
- **Controleren:**
```
/gamemode survival
/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek
```
- **Let op:** klik op de ballon of de staart: je stapt in. Met een nieuw karakter (lijn dicht) zie je de uitleg in de chat en
  in de actiebalk.

## 8.5 W3-uitkijkpunten op epische plekken (u-overgang)
- **Gedaan:** nieuw is De Reuzenhoed bovenop de tempelzwam. De uitkijkpunten bij de ballonhavens zijn weg. Alle 21 namen zijn
  vertaald.
- **Controleren:**
```
/guhs vadsrijk indeling plek uitkijk_hoedtop
/guhs vadsrijk indeling plek uitkijk_nest
/guhs vadsrijk indeling plek uitkijk_galmtop
```

## 8.6 Zichtbaarheid van cape en uitrusting (u-zichtbaar)
- **Gedaan:** de stat-cape gaat via de echte cape-laag. Handschoenen, armband, ringen, ketting en oorbellen staan op het
  spelermodel en worden gesynct naar anderen.
- **Controleren:**
```
/guhs vadsrijk z3npc klaarzetten
/guhs vadsrijk zichtbaar aankleden
```
- **Let op:** druk F5 en kijk of je de cape ziet en de handschoenen. In de eerste persoon zie je de handschoenen NIET; dat is
  bekend.

## 8.7 Gondel zone 2: geen mobs bij aankomst (u-deco)
- **Gedaan:** binnen 24 blokken van elke aankomstplek spawnt niets meer. Dat geldt voor de gondels, havens, steigers, de
  luchthaven, terugportalen, kampvuren en stadsranden.
- **Controleren:**
```
/guhs vadsrijk z2wereld plek gondel_dal
/guhs vadsrijk udeco vrij
```

## 8.8 Dubbele spawns (u-dubbel)
- **Oorzaak:** niet-opgeslagen entities bleven als "spook" in het geheugen hangen, en een manager keek in het laadvenster
  (de chunk is er al, de entities nog niet).
- **Gedaan:** uniek-sleutels per baas/NPC, een binnenkomst-bewaker en een spookveger.
- **Controleren:**
```
/guhs vadsrijk npc tp z2_kapitein
/guhs vadsrijk dubbel scan
/guhs vadsrijk z3bazen tp kruinroofvogel
/guhs vadsrijk dubbel scan
```
- **Let op:** 1 Kristalwacht Splinter en 1 Kruinroofvogel, ook na een paar keer weg- en terugvliegen.

## 8.9 Hekken, kettingen, vlaggetjes en slingers verbonden (u-hekken)
- **Gedaan:** een centrale verbindingspas over alle tegels (24.136 open verbindingen → 0). Hij zit in het bouwproces
  (`maak_kaart`), en door code geplaatste stukken gebruiken `Verbind.java`.
- **Controleren:** de teleports in §1 bij U-HEKKEN, plus de kastelen van de Knuffelbond en de huizenwijk.
- **Let op:** overal doorlopende lijnen, ook op de tegelgrenzen.

## 8.10 Uitkijkpunt `uitkijk_nest`: naam, losse hekken, zwevende lantaarns (u-overgang + u-deco + u-hekken)
- **Gedaan:**
  - de naam is nu **Het Kruinnest** (NL/EN);
  - de reling is één ring;
  - de lantaarns staan op de vloer.
- **Controleren:** `/guhs vadsrijk indeling plek uitkijk_nest`

## 8.11 Wenteltrap in de boom: verbonden hekken + verdiepingen (u-deco + u-hekken)
- **Gedaan:**
  - een gekleurde band per verdieping;
  - banieren en een lantaarntje op elk bordes;
  - een naambord: "Honingtak, Verdieping 2 van 6".
- **Controleren:** `/guhs vadsrijk z3wereld plek trap_bordes_1` t/m `_6`; als een bord ontbreekt: `/guhs vadsrijk udeco borden`.

## 8.12 Kruinroofvogel-arena: onderkant (u-deco)
- **Gedaan:** een organische nestkom met takken, hangend mos, wortels en honingdruppels.
- **Controleren:** `/guhs vadsrijk z3wereld plek honinglift_kruin_onder` en kijk omhoog.

## 8.13 Honingtak: licht, bankjes, prullenbakjes, bijen (u-deco)
- **Gedaan:**
  - om de ~10 blokken een paal;
  - om en om een bankje of een prullenbak + bloembak;
  - een kring rond de fontein;
  - overdag max 8 bijen (vanilla + guhbijtjes) plus de honingbijtjes. Ze steken nooit.
- **Controleren:**
```
/time set day
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk udeco bijen
```

## 8.14 Karakters (u-karakter)
- **Gedaan:**
  - drie VASTE slots: links Normaal, midden Knuffelmodus, rechts Eénvads. De modus is niet aan te passen.
  - De eerste keer maken werkt nu: een welkomstscherm na het openingsfilmpje, met een rode reden als iets niet mag.
  - Vindbaar zonder commando's:
    - knop ⚔ bij de inventaris;
    - tab **Ridders** in het Vadsrijk-menu (H);
    - shift + rechtsklik op een kampvuur;
    - de NPC's (Grootmeester, kampwachten, Rekruteerder);
    - Werver Wimpel.
  - De Rustende Ridders-zaal staat nu in de wereldgeneratie (Zwamzoet, tussen de Kruidenier en de boulevard).
- **Controleren:**
```
/guhs vadsrijk karakter welkom vergeet
/guhs vadsrijk karakter welkom
/guhs vadsrijk karakter zaal tp
```
- **Let op:** wisselen mag alleen in een stad, bij een kampvuur of in de eerste minuut.

## 8.15 Spiekbrief zonder zone 4 en 5
- **Gedaan:** deze spiekbrief heeft hoofdstuk 4 (De Lampionkust) en 5 (Hoogvads) volledig, plus alle ronde-U-punten per zone.

## 8.16 Schildpadeiland: lampion en bloemetjes te hoog, knuffel op de NPC (u-deco)
- **Gedaan:** alles staat nu op de echte schildhoogte, en geheime knuffel 6 heeft een eigen plekje.
- **Controleren:**
```
/guhs vadsrijk z4wereld plek schildpad_rug
/guhs vadsrijk z4wereld plek geheim_knuffel_6
```

## 8.17 Audio (u-audio2)
- **Gedaan:**
  - bazenmuziek met een laatste fase (crossfade);
  - een eigen overwinningsfanfare per baas;
  - 5 nieuwe thema's: imker, Grauwe Droom, Donderbuik, Gorgonzola, Zilverstaart;
  - zachte wereldgeluiden (korven, aambeelden, fonteintjes, kampvuren);
  - NIEUW (u-geroezemoes): zacht guh-geroezemoes op drukke plekken (Hoogvads, de bazaar van Saffraanpoort, de markt van
    Kandijkom), met een eigen schuif "geroezemoes"; in Saffraanpoort speelt het bazaar-bed nu op half volume eronder;
  - een grot-echo;
  - NPC-stemmetjes uit de bestaande guh/piep/blub-geluiden;
  - schuifjes "wereldgeluiden" en "stemmen".
- **Controleren:**
```
/guhs vadsrijk geluid fase grauwe_droom
/guhs vadsrijk geluid fanfare klodderreus
/guhs vadsrijk geluid stem groet
```
- **Let op:** of de wereldgeluiden nu subtiel genoeg zijn.

## 8.18 Website: volledig testen (u-site)
- **Gedaan (u-site, 3 rondes):** de wiki staat als verborgen voorbeeld op **https://guhs.nl/vadsrijk-preview/** (noindex,
  nergens gelinkt). Erbij: zone 6 + de raid, bazen met alle drops en kansen, renders van bazen en wezens, ingeklapte zones op de
  bazen- en questpagina, tabellen met pagina's/sorteren/filteren voor wezens en items, een nieuwe verhaalpagina, een
  geheimenpagina, en live spelersposities op de kaart.
- **Live posities (je besluit):** standaard aan, ververst elke ~10 s, nooit in kerkers, de raid, huizen of de Bondenvallei.
  Zelf uit: `/guhs vadsrijk kaart verberg` (weer aan: `kaart toon`).
- **Let op:** de kaarttegels op de site worden na de laatste regen opnieuw gemaakt; het publiceren doet de hoofdagent (zie EINDRAPPORT).

## 8.19 Besluiten V1-V5 die nu gebouwd zijn
- **Knuffelbond (guilds):**
  - oprichten in Hoogvads, lid worden vanaf zone 1;
  - eigen kasteel (niveau 1-5) in de Bondenvallei;
  - weektaken, perks, bondschat `/bc`, ranglijst.
  - Waar: tab **Knuffelbond** in H of `/bond`.
- **Huizen:**
  - Het Knuffelkwartier, met kavels in straatjes en 1 huis per account;
  - te bereiken via de Kwartiermeester op elk hoofdplein; tab **Mijn huis** in H.
- **Grotten:** in elke zone 1 sfeergrot: Zwamgrot, Kristalgrot, Wortelgrot, Koraalgrot (duiken!) en de Wolkengrotten (Blaasgat).
  Elk met een ambacht, een elite, een puzzel en een schat.
- **Job-advancements nieuw model:**
  - lv 17: de specialisatie kiezen;
  - lv 35: zone 3;
  - lv 58: na de zone-5-finale;
  - respec is gratis bij de klasse-NPC;
  - de spec staat in de Karakter-tab en in de party-frames.
- **Gear:** geen iNv; een beter/slechter-pijl in de tooltip.
- **Rijdende guhs:** 22 ruiters op Pluimguhs (20 in zone 3 op de trappen, Honingtak en stal; 2 in zone 1).
- **Party-frames, emotes (;) en de HUD-bewerker:** de HUD-bewerker zit in Instellingen › HUD aanpassen, of linksboven in het
  pauzemenu.
- **Ambachten groeien mee:** T4/T5, cap 60, koraalduiken, Hoogvads als ambachtshoofdstad.
- **Collectiescherm:** alles zit in één Vadsrijk-menu (H) met even grote hoofdtabs, en de overflow is gefixt.
- **Pauzemenu:** rechts staat een overzicht van alle Vadsrijk-toetsen.

## 8.20 Nieuw verzoek ronde 4 + vraagrondes: wat er verder gebouwd is
- **Zone 6 "Het Zijdeland" (lv 60-70)** met De Sterrenkaravaan, Rozenkloof + Saffraanpoort, 5 gebieden, kerker en de raid: zie
  hoofdstuk 6. Toon: lief en vrolijk, alleen de toren is grauw.
- **De raid De Sterrendovende Toren:** 2-10 spelers, schaalt, een eigen kist per speler per clear. Zie het raid-blok in hoofdstuk 6.
- **Schatkamer te vlak:** diepe Al-Khazneh-gevel + reliëf in de kloofwanden. `/guhs vadsrijk z6wereld plek rozenkloof_schatkamer`
  en loop terug over het plein; kijk omhoog.
- **Vliegend tapijt zweverig/glijderig:** acceleratie en uitglijden, drift, momentum los van je kijkrichting (SPIEK_U_TAPIJT).
- **Rijdieren: tapijt + bultguh** naast de Pluimguh, bij NPC's, stalls en structures.
- **Loot (V6):** alles persoonlijk; rijdier-drops overal 10 % per speler per kill/clear met een pech-teller (zeker na 15 keer mis).
  Tabel bovenaan deze spiekbrief. `/guhs vadsrijk loot kansen`.
- **Grotten als kerkers (u-grotkerker):** elke grot (zone 1 t/m 6) is nu een herhaalbare mini-kerker met een eindbaas en een eigen
  kist per clear, en zeldzaam een grot-kleur Pluimguh (10 % + pech). `/guhs vadsrijk grot run`.
- **Leven in de stad (u-leven-stad + u-leven-ritme):** beestjes, guh-huisdieren, straatleven, speeltuinen, terrassen met kletsende
  guhs; winkels sluiten 's nachts (quest-NPC's blijven bereikbaar), weer per zone, emote-reacties + knuffelmeter, het Pluizenblaadje.
- **Opslag (u-opslagvip):** alleen het eerste extra knapzak/kluis-vak is te koop, de rest zegt "Binnenkort"; vóór je eerste koop
  een eenmalige grap. `/guhs vadsrijk opslag vipgrap`.
- **Questrobot + Questlopen:** de robot speelt alle quests van zone 1-6 automatisch (41/19/24/23/24/25, 0 vast); Questlopen
  (hoofdstuk 0) laat jou de hoofdlijn zelf doorklikken.
- **Dialoogkeuzes:** zijn er bewust NIET (je besluit); gesprekken lopen recht door.
- **Kleinere jar (u-grootte + u-krimp):** structuren gepakt, muziek/geluid compacter (mono ~48 kbps), grote textures gehalveerd,
  PNG verliesvrij: de mod-jar is ruim 60 MB kleiner.
