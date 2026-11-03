# Testen: Guhs 1.9 "Een opknapper!" (branch `190-opknapper`)

Alles zit erin: de brief van de makelaar, Erfhekje + Leverkist, je eigen dorp (33 gebouwen, 10 levels, de echte vormen
uit de renders), de makelaarstour tot Hoeve 2 met daarna ansichtkaartjes, de decowinkel met paden en hekjes trekken en
Knusheid, bestellingen en karretjes van de buurdorpjes, de wereldkaart, Handje helpen, het hele Sherlock-verhaal (met
beloningen die echt iets doen) en het FTB-hoofdstuk "Een opknapper!".

## Starten

1. Dubbelklik **`run-client.bat`** in `guhs_opk\w-int`. Eerste keer duurt het even (Gradle).
2. Nieuwe wereld, **cheats aan**. Taal: Nederlands.
3. Wil je de echte weg zien: speel in **survival**. Zonder gekochte erfenis zeggen de toets **O** (bovenaanzicht), de
   wereldkaart (**M**) en `/guhs erfenis` netjes "Je hebt nog geen erfenis, njeg...". In creative mag alles altijd.

## Snelste route (± 20 minuten)

| Stap | Doen | Kijk naar |
|---|---|---|
| 1 | `/gamemode survival`, druk **O** en **M** | allebei geweigerd (actiebalk) |
| 2 | `/guhs erfenis brief` | de Postguh komt met de dikke envelop (normaal na 30 min speeltijd vanaf je eerste Guhmension-bezoek; `brief klok 29` = bijna) |
| 3 | Brief lezen → **Kopen voor 1 kaasknabbel** (`/give @s guhs:kaas_knabbels` als je er geen hebt) | Erfhekje + Leverkist in je zak |
| 4 | Hekje en kist neerzetten, door het hekje lopen | aankomst in je krakkemikkige dorp, makelaar Guhrrit begint de tour |
| 5 | `/guhs erfenis snel aan`, dan de tour doen: plukje opruimen → gratis Kippenhok → bewoner → bestelling → Hoeve 2 | elke stap pas op zijn moment; afscheid: borstbeeld + brievenbus in het decoschuurtje |
| 6 | `/guhs erfenis hoeve 7` | 5 ansichtkaartjes per Postguh (Hoeve 3 t/m 7), Kaartjesbord in het schuurtje |
| 7 | `/guhs erfenis geef alles 999` en wat bouwen in het bovenaanzicht (**O**) | winkel onderin, panelen, belletjes, Opslag / Naar huis / Deco / Loket / Logés |
| 8 | Deco-tabje (laatste in de winkelbalk): een paadje met bocht en kruising slepen, hekje eromheen, bankje + fontein | hoekjes/kruisingen vanzelf, hekjes sluiten aan, Knusheid-chipje bovenin (tooltip telt op) |
| 9 | Detectivebureau bouwen, wolkje (vergrootglas) boven het bureau → Speurboekje → zaak 1 | Sherlock + Vadson staan **voor de deur van je bureau** |
| 10 | `/guhs erfenis sherlock vind` (alle aanwijzingen) of `sherlock demo`, dan `sherlock volgende` om door de zaken te springen | beloningen: zie hieronder |
| 11 | Naar huis: venster **Naar huis** → stammen/wol/glasscherven → terug (`/guhs erfenis terug`) | je staat voor het hekje, Leverkist met vlaggetje, rechtsklik opent hem |
| 12 | FTB-boek: hoofdstuk **"Een opknapper!"** (groep Guhs, na Guhdromenland) | quests vinken binnen een seconde af |

Kant-en-klaar in één keer: **`/guhs erfenis demo`** (Hoeve 6, ~25 gebouwen; je komt op een vrij plekje, niet ín een gebouw).

## Waar je op moet letten

- **Makelaarstour**: Guhrrit praat bij aankomst in het praatscherm; latere stappen als chatregel. Klik op hem = de stap
  nog eens. Een praatscherm dat binnenkomt terwijl het bovenaanzicht open is, wacht tot je het sluit.
- **Ansichtkaartjes**: Postguh-regel + tekst in de chat, papiertje "Ansichtkaart uit ..." in je zak, kaartje in het
  vondstenkistje. 3 vind je onder plukjes, 5 komen per post = 8 op het Kaartjesbord (+2 Knusheid per kaartje als het bord staat).
  Snel: `/guhs erfenis ansicht <4-8>`.
- **Decowinkel**: categorieën Paden, Hekjes, Bloemen, Zitjes, Licht, Water, Beelden, Boerderij, Bijzonder. Slotje + "H5" =
  komt op een hoger Hoeve-level. Klik kaartje → spookje (R draait) → klik = betalen en neerzetten (spookje blijft);
  **shift-klik** = kopen voor in het schuurtje. Ligt hetzelfde stuk al in je schuurtje, dan pakt hij die eerst.
- **Paden/hekjes trekken**: slepen. Spookjes groen = nieuw, goud = gratis uit voorraad, rood = kan niet / geld op,
  wit = ligt er al. **Gum** haalt weg: paden naar je voorraad, hekjes naar het schuurtje (nooit geld terug).
- **Deco kiezen**: klik op een stuk → Verplaatsen / Draaien / Opbergen, of slepen. Gebouw eroverheen = stuk terug in het
  schuurtje, pad-tegels in je voorraad.
- **Decobon**: `/guhs erfenis decobon` geeft er een; rechts in de categorierij "Decobon: n" aanzetten. Oma Breiguh geeft
  er soms ook een (zie Sherlock zaak 4).
- **Knusheid**: tooltip = gebouwen · paden · deco (Kaartjesbord-kaartjes tellen bij deco). Mijlpalen 50 ... 5.000 geven
  één berichtje; titels Knusse Guh (250), Knusvads (1.000), Knusser Bestaat Niet (2.500).
- **Sherlocks beloningen**: zaak 2 → recept **Opa's theetaartjes** in de Bakkerij (eerst verborgen); zaak 4 → **Oma
  Breiguh** rijdt mee met 1 op 3 karretjes (+50%, extraatje); zaak 5 → **Opa's kelder** onder de Hoeve (luik in de vloer,
  ladder omlaag, dagboek op de lessenaar); zaak 6 → **Bertha** zingt op de Muziektent; zaak 9 → **Opa Knabbels testament**
  als boek; rechtszaak → het bureau in zijn **eindlook** (L10-vorm). Direct: `/guhs erfenis beloning <vlag>`
  (`recept_theetaartje`, `oma_bestelling`, `opas_kelder`, `bertha_optreden`, `testament`, `bureau_eindlook`), en
  `/guhs erfenis kelder` om naar de kelder te gaan.
- **Bestellingen**: paneel van het Marktplein → Bestellingen, rechtsklik op het Marktplein in je dorp, of **M** → een
  buurdorpje. Het scherm telt je echte leveringen.
- **FTB**: 87 quests + "Hoe kom je hier?", niks op slot. Statistieken (Esc → Statistieken → Algemeen): plukjes, Hoeve,
  Knusheid, ansichtkaarten (doel 8), burenstempels, ...

## Dev-commando's (op)

| Commando | Wat |
|---|---|
| `/guhs erfenis demo` | kant-en-klaar dorp (Hoeve 6, ~25 gebouwen) |
| `/guhs erfenis snel aan` / `tijd <min>` | alle klussen meteen klaar / tijd laten verstrijken |
| `/guhs erfenis hoeve <n>`, `geef <goed\|guhlden\|alles> <n>` | Hoeve-level, spullen |
| `/guhs erfenis opruimen alles\|<n>`, `kavels <n>` | land openmaken |
| `/guhs erfenis brief [status\|reset\|klok <min>]` | de makelaarsbrief (zonder argument: Postguh nu) |
| `/guhs erfenis leverpost [n]`, `dorpszender`, `decobon` | koppeling-testjes |
| `/guhs erfenis kaart`, `kaart bestellingen`, `kaart bestel snel aan\|vul`, `kaart npc groei` | kaart en bestellingen |
| `/guhs erfenis sherlock info`, `vind`, `demo`, `volgende`, `zaak <id>`, `moment <id>`, `bureau`, `rechtbank`, `reset` | Sherlock |
| `/guhs erfenis ansicht <4-8>` | een ansichtkaartje van de makelaar nu |
| `/guhs erfenis beloning <vlag>`, `kelder` | Sherlock-beloning meteen, naar Opa's kelder |
| `/guhs erfenis doe ...` | zelfde serverpad als een klik: `plaats <type> <x> <z> [rot]`, `verbeter <type>`, `lijn <soort> x0 z0 x1 z1`, `koop <id> [x z rot]`, `gum x,z;x,z`, `decodraai x z`, `decoverplaats x z nx nz [rot]` |
| `/guhs erfenis vorm <id> <level> <palet> <rot>`, `vorm alle <level> <palet>`, `vorm decoalle` | alle vormen bekijken |
| `/guhs erfenis terug` | terug naar je hekje |

## Ook het proberen waard

- Een plukje opruimen: bij het 1e-3e plukje vind je het roestige sleuteltje (Sherlock-proloog in de chat).
- Een gebouw verplaatsen over een tuintje: deco terug in het schuurtje, paden in de voorraad, hoekjes ernaast kloppen weer.
- Bestelling leveren: Guhlden erbij, telt voor de Hoeve-doelen, soms een bedankje-decostuk (FTB "Een bedankje van de buren").
- Met een tweede speler (LAN): elkaars dorp op de kaart, Handje helpen (versnelt de klus die het eerst klaar is, max 25%).

## Bekende gaten

- Getuigen van een lopende zaak staan nog op hun vaste tegel (niet bij "hun" gebouw).
- `paaltjes_terug` (Sherlock) geeft alleen een melding: er werden nooit echt paaltjes verzet.
- Bertha staat er altijd (er bestaan nog geen dorpsfeestjes). FTB "Dorpsfeest!" gaat af bij de Muziektent.
- Wordt de Hoeve verplaatst, dan blijft de oude kelder dicht onder de grond; de nieuwe komt onder de nieuwe plek.
- Mika-strooptochten: "binnenkort" (alleen het plaatje aan het eind van het FTB-hoofdstuk).
- De wiki/Discord/pack zijn nog niet bijgewerkt (pas bij de release).
- In de volledige gametest-suite falen 6 tests die ook op main 1.8.1 al rood zijn (kussengevecht-gids, weide_roze_gras-
  schaar, wereldBescherming, techquestGebouw, SNOES-Guhdex); niks van de opknapper.
