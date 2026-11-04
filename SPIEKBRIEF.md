# Spiekbriefje: alles testen met commando's (ronde 3 bovenaan, ronde 2 daaronder)

Start de testclient uit `guhs_vadsrijk\j-int\run-client.bat` (ronde 3: ronde 2 + melee + alle fixes van ronde 3) en maak een
**nieuwe** wereld (de saves-map is leeg).

Doorloop het van boven naar beneden: elk blok test één onderdeel. Copy-paste de commando's in de chat.

Starten:
- Nieuwe wereld, cheats aan.
- `/guhs vadsrijk` om erin te gaan.
- Wil je geraakt kunnen worden: `/gamemode survival`. In creatief vallen de wezens je niet aan.

Handig tussendoor:
- `/guhs vadsrijk vol` zet Vads, energie en ult vol en wist de cooldowns.
- `/guhs vadsrijk info` laat je data zien.
- Met `/guhs vadsrijk tips reset` krijg je alle tips opnieuw.

---

# RONDE 3

## R1. Makelaar en andere buitenstaanders blijven buiten
In het Vadsrijk:
```
/summon guhs:guh_npc ~ ~ ~2
/guhs erfenis brief
```
- **Summon:** er verschijnt niks. Guhs-wezens van thuis (makelaar Guhrrit, de Postguh, de Sherlock-cast) mogen er niet in.
- **Brief:** de makelaarsbrief komt niet in het Vadsrijk. Hij wacht tot je weer thuis bent (`/guhs vadsrijk` = terug).
- **Roepen:** "Roep naar mij" (guh-band) of de Guhkamer-roep in het Vadsrijk zegt "Je guhs blijven lekker thuis...".
- **Volgers:** een Libelkonijnguh, Sponsguh, Gerrit of Momo op je schouder gaat niet mee naar binnen, en staat er weer als je terug bent.

## R2. Huisdier zonder naambordje
```
/guhs vadsrijk huisdier geef koning_klodder
```
- Druk P, kies "Meenemen".
- Boven je huisdier staat **geen** naambordje meer (eerst stond er "entity.guhs.vr_huisdier").

## R3. Reiskaart: het kruisje
```
/guhs vadsrijk npc tp kapitein
```
- Klik op de kapitein. Rechtsboven in de roze titelbalk van de reiskaart staat een ✕ ("Sluiten" als je eroverheen gaat).
- Klik erop: de kaart gaat dicht, met een klikgeluidje.

## R4. Tabbladen: Rijdieren / Huisdieren / Verkenningsboek
```
/guhs vadsrijk mount alle
/guhs vadsrijk huisdier alle
```
- Druk N. Boven het paneel, tussen de guh-oortjes, zitten drie tabjes: **Rijdieren**, **Huisdieren**, **Verkenningsboek**.
- Klik heen en weer. Het actieve tabje hangt vast aan de roze titelbalk.
- Bij de stalmeester (`/guhs vadsrijk mount stal`) zijn er géén tabjes.

## R5. Rijdieren (ronde 3)
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

## R6. Zone 2: hangbruggen
```
/guhs vadsrijk z2wereld plek brug_3_zuid
```
- Dit is de grote brug bij Kloofwachter Touwtje naar de Spiegelburcht. Loop eroverheen **zonder te springen**: elke stap is
  hooguit een half blokje, aan beide kanten sluit hij aan op de grond.
- Ook: `brug_1_zuid`, `brug_2_zuid`, `brug_4_zuid` (Splinterkloof) en `brug_5_zuid` (Galmspleet).

## R7. Zone 2: aankomst bij het dok en de poorten
```
/guhs vadsrijk z2wereld plek dok_landing
```
- **Glas weg:** het zwevende roze glas boven de poort is weg. Er staat nu een calciet-poortboog met een kristallamp.
- **Bergpas:** de weg naar de zuidpoort loopt glooiend door een pas, niet meer tussen rechte wanden.
- **Aankleding:** lampions en lichtsnoeren over de weg, vuurkorven, een bankje, een **chocomelk-kiosk** (`z2wereld plek dok_kiosk`)
  en een houten **welkomstboog** bij de pier (`z2wereld plek dok_welkom`). Sneeuw en kristallen langs de randen.
- **Andere poorten:** loop vanaf het plein van Kandijkom de noord-, oost- en westpoort uit. Dezelfde boog en een glooiende pas.

## R8. Muziek in de steden
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

## R9. Het Verkenningsboek (B)
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

## R10. Kleine geheimpjes en de horizon (lore)
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

# RONDE 2 (nog steeds geldig)

## 1. Klassen: het wapen is je klasse
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

## 2. Balans (live aanpassen)
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

## 3. Drankjes
```
/guhs vadsrijk eco winkel drankjes
```
- Koop een Klein Vadsdrankje (120), een Vadsdrankje (300) en een Energiedrankje.
- Drinken duurt 2 s. Daarna staan alle drankjes 10 s op cooldown; je ziet het grijs over het item.

## 4. Melee en de tutorial (melee-fix: deze map, h-melee)
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

## 5. Sieraden en UI
```
/guhs vadsrijk loot zeldzaam roze_ring 5
/guhs vadsrijk loot zeldzaam pluisoorbel 5
```
- **Tooltip:** lees de uitleg "Rechtsklik om te dragen".
- **Aandoen:** open je inventaris en doe shift+klik op het sieraad.
- **Hoveren:** ga met de muis over het vakje rechts. Je ziet de volledige stats en "Klik om af te doen".
- **Sets:** `/guhs vadsrijk z2wezens set` geeft set-stukken. De tooltip toont "Set: Kristalset (x/4)".

## 6. Rijdieren (X / N)
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

## 7. Huisdieren (P)
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

## 8. Reiskaart en kapiteins
```
/guhs vadsrijk npc tp kapitein
```
- **Kapitein:** klik op hem. Je krijgt de reiskaart, met grijze "?"-plekken en de teasers voor zone 3 en 4.
- **Kaart:** sleep met de muis om te schuiven en klik een open plek aan om te vliegen.
- **Scheepsbel:** rechtsklik op de bel opent ook de reiskaart.
- **Zone 2 vrijspelen:**
  1. `/guhs vadsrijk npc quest af hq12` en daarna `/guhs vadsrijk klasse generiek 20`.
  2. Kandijkom gaat dan open op de kaart.

## 9. Werver Wimpel (Guhmension)
- Ga terug: `/guhs vadsrijk`.
- Zoek de ruïne: `/locate structure guhs:vr_w_portaaltempel`, of zet Wimpel aan je voeten met `/guhs vadsrijk npc werver`.
- Raak het portaal aan vóór je met hem praat. Er gebeurt niks.
- Praat met Wimpel. Je krijgt het Wynn-grapje en het advies over de weergave. Daarna wordt het portaal wakker.
- Opnieuw testen: `/guhs vadsrijk npc werver vergeet` en `/guhs vadsrijk wereld vergeetportaal`.

## 10. Terug-quests (Hert en Wolf)
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

## 11. Questvolger en job-advancement
```
/guhs vadsrijk klasse klasselevel knuffelridder 10
```
- Rechtsboven verschijnt een lichtblauw blok met ✦, met een lichtblauwe straal naar de Grootmeester.
- **Tempeldeur:** `/guhs vadsrijk klasse generiek 18` en `/guhs vadsrijk bazen tp tempel`. Rechtsklik op de grote deur; hij gaat open.
- **Puzzelkamer:** loop een kamer in. De tip blijft onderin beeld staan.

## 12. Kaart zone 1
```
/guhs vadsrijk wereld plek aankomst
```
- **Kamp:** loop de kliftrap op. Er staat geen boom op de uitgang, en een pad met lantaarns loopt naar het kampvuur.
- **Bram:** `/guhs vadsrijk npc tp bakker_bram`. Hij staat in zijn kraam.
- **Kasteeltje:** `/guhs vadsrijk wereld plek ordetempel_binnen`. Kijk naar de torens van het kasteeltje.
- **Biomes:** F3+F6 of JourneyMap toont "Het Vadsrijk - <plek>".
- **Draak:** `/guhs vadsrijk sfeer draak stad` (50% groter). Zie je de opening graag nog eens: `/guhs vadsrijk npc opening`.
- **Opgevrolijkte wezens:** versla een klodder. Hij wordt doorzichtig en je slaat erdoorheen.

## 13. De nieuwe zone 2
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
