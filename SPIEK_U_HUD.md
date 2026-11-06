## U-HUD: party-frames, emotes en de HUD-bewerker

Alles hieronder werkt in het Vadsrijk (emotes werken overal). Dev-commando's beginnen met `/guhs vadsrijk hud` (op 2).

### Nieuwe toets
| toets | wat |
|---|---|
| **;** (puntkomma) | **Emote-wiel**: ingedrukt houden, muis naar een emote, loslaten = doen. Kort tikken = het wiel blijft open, dan klik je er een. |

Te veranderen in Opties → Besturing → Vadsrijk ("Emotes (ingedrukt houden)").

### 1. Party-frames
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

### 2. Emotes
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

### 3. HUD-bewerker
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
