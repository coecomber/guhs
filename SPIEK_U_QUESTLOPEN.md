## Questlopen (admin): de hoofdlijn zone 1 → 6 doorklikken (U-QUESTLOPEN)

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
