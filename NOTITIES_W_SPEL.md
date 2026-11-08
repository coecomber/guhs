# NOTITIES W-SPEL (ronde W, branch 110-w-spel, vanaf 0f00119f2)

Vijf meldingen van de gebruiker: lege cutscene F2, wereld niet geladen bij cutscenes, rauwe taalsleutels bij zonewissel,
vastlopen in Questlopen bij Kootje Zilverstaart, en door de grond zakken op een kussen bij Kloofmoeder Roosje.

## 1. "Ver in het oosten wil iets steeds meer" toonde niks
- **Oorzaak:** er zijn twee versies van flash F2. z3-bazen's troonzaal (`Z3Scenes.troonzaal`, na Vahoegnir) bouwt een echte
  set. De npc-versie (`Z3Verhaal.flashF2`, bij de cliffhanger als je die troonzaal niet zag, bijvoorbeeld via Questlopen) filmde
  `Z3NpcPlekken.TROON` = v2(2990, 300, -680): daar is nooit iets gebouwd. Daarbij gaf hij DARKNESS. Resultaat: een lege, donkere
  lucht met alleen de titel.
- **Nu:**
  - shot 1 (140 ticks) is "ver in het oosten": de camera vliegt vanuit het zuidwesten naar de Sterrendovende Toren, de enige
    grauwe plek in het roze Zijdeland (zone 6, ZONE6_PLAN: de toren van de Vinger; vanaf daar loopt de eerste Vadsleiding naar
    het oosten). Dit is bestaand gebouwd terrein (`Z6VerhaalPlekken.TOREN_TOP`), met een nieuwe chatregel `vadsrijk.z3npc.scene.f2.oosten`;
  - shot 2 is dezelfde troonzaal-set als bij z3-bazen (`Z3Scenes.troonzaal`, met figuren, regels en de titel);
  - daarna, net als voorheen: "zone3.klaar" + `NpcApi.meld(z3, cliffhanger)`.
- **Code:** loopt-soort "f2" duurt nu 140 ticks en start daarna "f2zaal" (`TROON_TICKS + 10`), die de zone afsluit. Er is geen
  DARKNESS meer. `f2Tik` is weg (zijn regels zitten in de bazen-set). `Z3NpcPlekken.FLASH_F2`/`TROON` zijn ongebruikt (gemarkeerd).
- **Dev:** `/guhs vadsrijk z3npc f2` speelt de flash opnieuw af (nieuw in de scene-lijst van het z3npc-commando).
- De camera staat ~1800 blokken van zone 3: precies het geval dat punt 2 oplost.

## 2. Chunks vooraf laden voor ALLE cutscenes (generiek in RijkScenes/SceneSpeler)
- **Oorzaak:** de cutscene-camera is een client-entity. De server stuurt alleen chunks rond de speler, dus een verre camera
  keek naar niks. Een camera binnen het zicht keek naar chunks zonder mesh (die worden pas gebouwd als de camera er is).
- **Server (`kern/scene/SceneLaden`, nieuw):**
  - Ligt een punt van het camerapad (keyframes + elke ~24 blokken ertussen) buiten het normale zicht van de speler (marge
    2 chunks), dan krijgen de chunks rond de camera een eigen ticket (`guhs:vr_scene_camera`, time-out 80 ticks, ververst).
  - De chunk-tracking-view van de speler verhuist naar de camera (straal min(view distance, 10)), via vanilla's
    `ChunkMap#applyChunkTrackingView`. De server stuurt die chunks dan zelf, met zijn eigen boekhouding.
  - De view volgt de camera (hercentreren bij 3 chunks verschil, een halve wachtmarge achter de servertijd).
  - Aan het einde (duur + 60 ticks), bij een nieuwe scene, `RijkScenes.vergeet`, uitloggen of dimensiewissel komt de normale
    view terug.
  - Scenes die binnen het zicht blijven veranderen niets op de server.
  - Muted scenes (Questlopen-spoelen) en FakePlayers doen niet mee.
- **Client (`SceneSpeler`):**
  - Bij de start wacht de camera op keyframe 0 achter een zwart scherm tot de chunks waar hij naar kijkt binnen zijn en een
    gecompileerde render-sectie hebben. Gecontroleerd worden de camera, het kijkpunt (max 96 blokken) met twee punten ertussen,
    en de 3x3 chunks rond het kijkpunt. Het maximum is 60 ticks.
  - Daarna een fade van 8 ticks, en pas dan start het shot (ook de titelkaart wordt vanaf dan getimed).
  - Na de scene, als de eigen omgeving van de speler weg is (na een verre scene), blijft het zwart tot die terug is (max 60 ticks).
  - "De wereld wordt geladen..." verschijnt pas na 10 ticks zwart.
  - Is alles al klaar, dan komt er geen zwart scherm.
- **Bewuste afweging:** de server-tijdlijn van een scene (chatregels in de zone-loops) wacht niet op de client. Bij een eerste
  verre scene kunnen regels tot ~3 s vóór het beeld komen. Dat is gekozen boven een C2S-ack plus het aanpassen van 60 scene-aanroepen.
- **Na een verre scene** stuurt de server de omgeving van de speler opnieuw (net als na een teleport). Het zwarte scherm vangt dat op.
- **Test:** `SceneLadenGameTests` (verre camera verhuist de view, einde zet hem terug, dichtbij verandert niets, camera(t)
  volgt de keyframes). Of het er goed uitziet, moet in de dev-client gekeken worden (geen client in deze slice).

## 3. Rauwe taalsleutels ("vadsrijk.kern.gebied.z5_kathedraal - veilig gebied")
- **Oorzaak:** de namen van de zone-5-wijken waren uit nl_nl/en_us verdwenen. Terug: z5_paleisheuvel, z5_universiteit,
  z5_academie, z5_marktlaan, z5_kathedraal, z5_moeder_wolkwal, z5_buikgangen, z5_proefarena, z5_schuilnest, z5_wolkrand_noord,
  z5_wolkrand_zuid, z5_staartvlakte.
- De HUD-editor-demo (PartyHud/PartyFrames) gebruikte "vadsrijk.klassen.klasse.knuffelridder" (bestaat niet) en gebruikt nu
  "vadsrijk.kern.klasse.knuffelridder".
- **Checks:**
  - `tools/vadsrijk/taalcheck.py` (statisch, exit 1 bij ontbrekend) controleert:
    - letterlijke `"vadsrijk.x.y"` in Java;
    - prefix-constanten (`Z = "vadsrijk.z3npc."`) met `Z + "..."` én losse helper-literals zoals `zeg(p, "scene.f2.tik")`;
    - elke gebied-id in de code (`Gebied("id"`, `Gebied.cilinder("id"`, `Wijk("id"`), die "vadsrijk.kern.gebied.<id>" nodig heeft;
    - elke key-achtige string in data/guhs/**.json.
    - Het resultaat nu: 2368 sleutels, 0 ontbreken.
  - `VadsrijkTaalGameTests` (runtime) controleert in nl_nl én en_us (uit de jar gelezen):
    - elk echt geregistreerd gebied (`Gebieden.alle()`, ook de gegenereerde);
    - elke NPC-naam;
    - elke questtitel, questtekst en elk doel.
- `tools/features/vadsrijk_u_questlopen.py` kent de nieuwe Questlopen-teksten ook. De andere nieuwe teksten staan alleen in de
  lang-json, met en_us = Nederlands (Engelse pass volgt).

## 4. Questlopen robuust (vast bij Kootje Zilverstaart)
- Kootje = `z5_hq08_wolkwaldroom` doel 1 (MELD z5_zilverstaart:verslagen, plek baas:z5_wolkwaldroom). De uitgang van de kaart
  daar is voor w-bereik.
- **VECHT (zelf vechten):** is de baas verslagen of weg maar telt de stap niet, dan gaat hij na 5 rustige seconden naar WACHT:
  eerst de haak, dan de teller, en anders de melding "vast". Vroeger bleef hij eindeloos in VECHT.
- **Na een stap (`naStap`):** staat de speler in een arena, dan teleporteert hij altijd naar de volgende stap (ook als die
  dichterbij dan 6 blokken is). Heeft de volgende stap geen plek, dan gaat hij naar de laatste veilige plek.
- **Nieuwe knoppen** (altijd actief behalve tijdens spoelen, ook midden in een gevecht of wachttijd):
  - **Sla stap over**: haak + spoel, anders de teller, en dan door naar de plek van de volgende stap;
  - **Naar veilige plek**: stopt alles en zet je op de laatste rustige plek buiten een arena (onthouden als het paneel in rust
    opent en vlak voor een arena-teleport), anders bij de huidige stap of op het Kampplein.
- **Headless:** `QuestLoper.loopAlles(p)` speelt elke stap van zone 1-6 stil, tier voor tier, en meldt per stap echt/haak/dwing,
  "VAST", of "ARENA" (de volgende stap na een baas ligt in een arena). `VadsrijkQuestLopenGameTests.alleStappenZone1tot6` faalt
  op VAST/ARENA en zet het rapport in de log (`grep questlopen-alles`).

## 5. Door de grond zakken op een kussen (Roosje, en alle zitmeubels)
- **Oorzaak:** `Buitenstaanders` weigert in het Vadsrijk elke Guhs-entity zonder "vr_"-prefix, dus ook `guhs:guh_seat`, het
  onzichtbare zitje van ALLE guh-meubels (kussens, zitzakken, stoelen). `GuhFurnitureBlock` liet de speler toch
  `startRiding(seat)` doen op dat geweigerde zitje. De server zette de client 0.4 blok in de vloer (zitpositie), maar de client
  kreeg nooit een zitje en viel door de vloer (en door het luchtgat onder Roosjes troonzaal) tot y 1.
- **Fixes:**
  - `guh_seat` staat in `Buitenstaanders.TOEGESTAAN`;
  - `GuhFurnitureBlock` gaat alleen zitten als het zitje echt in het level kwam;
  - `GuhSeatEntity.getDismountLocationForPassenger` kiest een gecontroleerde plek: bovenop het zitblok als daar ruimte is,
    anders vrije vloer ernaast (eerst voor), en anders recht omhoog; nooit in of onder blokken.
- Alle zitbare dingen in het Vadsrijk zijn GuhFurnitureBlocks met dit zitje, dus dit dekt ze allemaal. Andere zit-entities zijn
  er niet in het Vadsrijk (NPC's die zitten zijn animaties).
- **Test:** `VadsrijkZitGameTests`:
  - het zitje hoort erbij;
  - zitten en opstaan op een vloer van één blok boven lucht, ook onder een laag plafond;
  - hetzelfde in de echte Vadsrijk-dimensie (als de testserver die heeft).

## Gedeelde bestanden (minimaal, gemarkeerd W-SPEL)
- `kern/Buitenstaanders.java`: TOEGESTAAN + "guh_seat".
- `block/GuhFurnitureBlock.java`, `entity/GuhSeatEntity.java`: zitten/opstaan.
- `kern/KernPayloads.java`: `stuur` geeft nu boolean terug (verstuurd of gesmoord); de aanroepers blijven werken.
- `kern/scene/RijkScenes.java`: speel → SceneLaden.begin/stop; vergeet → SceneLaden.stop.
- `kern/KernRegistry.java`: SceneLaden.register(modBus).
- `kern/client/SceneSpeler.java`: wachten op de wereld, zwart scherm, fade, titel pas bij de echte start.
- `kern/client/RijkHud.java`: tekent het zwarte laadscherm in de titellaag.
- `META-INF/accesstransformer.cfg`: `ChunkMap#applyChunkTrackingView` public.
- `zone3/npc/Z3Verhaal.java`, `Z3NpcPlekken.java`, `Z3NpcRegistry.java`: F2.
- `hud/client/PartyHud.java`, `hud/PartyFrames.java`: demo-klassesleutel.
- `questlopen/QuestLoper.java`, `client/QuestLopenScherm.java`, `VadsrijkQuestLopenGameTests.java`.
- `tools/features/vadsrijk_u_questlopen.py`.
- `lang/nl_nl.json` + `en_us.json`: alleen toegevoegde sleutels, met een script ingevoegd (geen herformattering).
- Nieuw: `kern/scene/SceneLaden.java`, `kern/scene/SceneLadenGameTests.java`, `kern/VadsrijkTaalGameTests.java`,
  `kern/VadsrijkZitGameTests.java`, `tools/vadsrijk/taalcheck.py`.

## Open punten
- Of het laden er goed uitziet (duur van het zwart, de fade, de troonzaal-flash) moet in de dev-client gekeken worden.
- Bij render distance 32 (instelling van de gebruiker) valt bijna alles binnen het normale zicht. Dan wacht alleen de client op
  meshes. Het verhuizen van de view gebeurt pas bij camera's >30 chunks weg, zoals F2.
- Flash F4 (zone 6, `Z6VerhaalPlekken.CAMERA "f4"`) kijkt in zijn eerste helft naar (4900, 200, -700): oostelijk van de kaart,
  waar nog niks is (zone 7). Dat is hetzelfde soort lege shot, maar niet gemeld en niet aangepast.
- Server-tijdlijn en client-wachttijd lopen bij een eerste verre scene tot ~3 s uiteen (zie punt 2).
