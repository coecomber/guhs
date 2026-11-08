## Questrobot (U-QUESTROBOT): alle quests van zone 1-6 automatisch laten spelen

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
