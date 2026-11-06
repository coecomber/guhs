# DIALOOG: het nieuwe Vadsrijk-praatscherm (T-DIALOOG)

Alle gesprekken **in het Vadsrijk** krijgen nu het nieuwe praatscherm. Buiten het Vadsrijk (Knuffeldal, Erfenis, de verhalen)
blijft het oude scherm precies zoals het was.

Test dit op **GUI-schaal 2, 3 en 4**:
- Opties > Video > GUI-schaal.
- Doe de demo op elke schaal één keer.

## D1. De demo: alles in één gesprek
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

## D2. Een vol scherm (GUI-schaal 3-4)
```
/guhs vadsrijk dialoog vol
```
Dit is een lange tekst met 9 keuzes.
- Op GUI-schaal 4, of in een klein venster, komen de keuzes in **twee kolommen**.
- De tekst scrollt, met een knipperend ▼ als er nog meer staat.
- Er mag **niets** boven of onder buiten beeld vallen.
- "Naar Kandijkom (nog op slot)" staat met **grijze** tekst.

## D3. Echte quest-NPC's: "Wat moet ik doen?"
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

## D4. Vergelijken met het oude scherm
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

## D5. Ontwikkelaarshulp
**F3+H aan (geavanceerde tooltips):**
- Onder het scherm staat een grijze regel met: sleutel | tekstsleutel | factie/stem/emotie (een ! = vast gezet, ~ = geraden uit de tekst) | portretbron.

**Een emotie per regel in questdata:**
- Zet bij een spreker `"npc:blij"` (of `verdrietig`, `verrast`, `boos`) in plaats van `"npc"`.
- Zonder zo'n emotie raadt het scherm die uit de tekst: "Hoera" = blij, "?!" of "Ooh" = verrast, "Grr" = boos, "helaas" of "..." = verdrietig.
