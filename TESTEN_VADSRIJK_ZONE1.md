# Testen: Het Vadsrijk, zone 1 (Guhs 1.10.0), alles samen

Dit is de ene testgids voor heel zone 1: de kaart, de NPC's en quests, de wezens, de bazen, de klassen en de economie. De losse
gidsen per slice (TESTEN_VADSRIJK.md, TESTEN_WERELD.md, TESTEN_NPC.md, TESTEN_WEZENS.md, TESTEN_BAZEN.md, TESTEN_KLASSEN.md,
TESTEN_ECONOMIE.md) blijven als naslag; deze gids gaat voor.

## Starten
1. Dubbelklik `run-client.bat` in `guhs_vadsrijk\w-poc`.
2. Maak een **nieuwe** wereld (Creatief, cheats aan). De ingestorte ordetempel staat alleen in nieuwe Guhmensie-chunks.
3. `/gamemode survival` als je wilt vechten (in creatief vallen de wezens je niet aan).
4. Erin: `/guhs vadsrijk` (of de echte weg: de ingestorte ordetempel in de Guhmensie, zie onderaan). Nog een keer `/guhs vadsrijk`
   = terug naar huis, met al je eigen spullen. In het Vadsrijk heb je een eigen, lege rugzak.

Je komt aan op het lage steigertje aan de zuidkust van Knuffelkamp. De eerste keer speelt vanzelf het openingsfilmpje
(luchtschip bij zonsopgang, het eiland, de skyline van Zwamzoet). Loop daarna de houten trap op naar het kamp op de klif.

## Toetsen (alleen in het Vadsrijk; Opties > Besturing, categorie Vadsrijk)
| toets | wat |
|---|---|
| Linkermuis | mep (ridder) of vasthouden = schieten (boog, kruisboog, katapult, staf, toverstok, boek) |
| R / V / G | vaardigheid 1 / 2 / 3 van je klasse |
| Z | je ultieme vaardigheid (laadt op door schade te doen; het percentage staat in het vakje) |
| K | je skillboom |
| J | je questlog: Quests, Geheimen, Wezens (het bestiarium), Gids (de Avonturiersgids) |
| Rechtsklik | NPC's (praten), kampvuren (checkpoint), deuren, de scheepsbel, munten (in je beurs), sieraden (omdoen) |
| Sluipen + rechtsklik op een speler | ruilen |

De Guhs-toetsen die op dezelfde knoppen zitten (maag, snuffel) doen in het Vadsrijk niks.

## De snelste route door zone 1
1. **Knuffelkamp**: Sergeant Pluizebol bij de steiger -> het kampvuur -> Kok Knapperkorst.
2. Zuster Zachtjes bij het klasse-tempeltje: kies je klasse (Knuffelridder, Pluisjager of Vadsmagiër). Je startwapen komt in je rugzak.
3. Meester Kussensloop op het trainingsveld: 6 keer op een trainingspop meppen -> je eerste wapen (met lichtstraal).
4. Steek het kampvuur aan, praat met de kok, klik de dialoog uit -> **de BAM**: Vahoegnir vliegt over, grauwe pluis regent, de
   bloemen verwelken, de **Grauwe Klodderreus** landt in de vijver. Vrolijk hem op (arena `npc:kampvijver`). Daarna woont de
   Kampklodder in de vijver (rechtsklik = aaien).
5. Pluizebol -> Kapitein Wolkewiet: de scheepsbel op de oostelijke steiger = **luchtschip naar De Haven**.
6. **Zwamzoet**: Grootmeester Vadsvoet in de ordetempel bovenop de hoogste zwam (wolkenliftje in de schacht = omhoog, de kolom
   ernaast = zacht omlaag; de Knuffeldeur of de altijd open zijdeur). Winkels, smid, markt, stal, questbord.
7. De littekens -> **Klodderweide** (Koning Klodder in zijn arena) -> **Sporenwoud** (Boswachter Mos) -> **Druipgrotten**
   (Mijnwerker Mopje loopt achter je aan naar het grotkampvuur) -> de Kloof met de drakenfossiel-brug -> het bergpad -> de
   **Zwamtempel** (level 16+): Lichtzwamkamer, Hendelkamer, Ridderzaal, **de Opperzwam**.
8. Het Mika-speldje naar de Grootmeester -> slotfilmpje "Wordt vervolgd..." en `zone1_klaar`: de grauwe littekens kleuren
   (voor jou) weer groen en roze.
9. Geheim: vijf Mika-aanwijzingen -> ontmasker "Akim de Kaasguh" op de markt (rechtsklik) -> het dakgevecht met de Mika-spion.

Snel door het verhaal: `/guhs vadsrijk level <n>` en `/guhs vadsrijk npc quest klaar <id>` (dan inleveren bij de NPC).

## Dev-commando's (op nodig)
**Kern**
| commando | wat |
|---|---|
| `/guhs vadsrijk` | erin / eruit |
| `/guhs vadsrijk proeftuin` | de lege testvlakte (x 3000-3192) |
| `/guhs vadsrijk level <1-20>` / `xp <n>` / `munten <n>` | niveau, xp, Vadsmunten (100 brons = 1 zilver, 100 zilver = 1 goud) |
| `/guhs vadsrijk loot <zeldzaamheid> [type] [niveau]` | buit laten vallen (types: zie "Buit" hieronder) |
| `/guhs vadsrijk vol` | Vads, energie, ult vol en cooldowns weg |
| `/guhs vadsrijk baas reset <arena>` | een baas meteen terug (`npc:kampvijver`, `bazen:koning_klodder`, `bazen:opperzwam`) |
| `/guhs vadsrijk tips reset` / `kist` / `info` | tips opnieuw / spullen die de wissel niet kwijt kon / wat er opgeslagen is |
| `/guhs vadsrijk party uitnodigen <naam>` / `party accepteer` | groepje (gedeelde xp, elkaars Vads, baas meer HP) |
| `/time set night` | werkt nu ook in het Vadsrijk (zelfde klok als de Guhmensie) |

**Wereld** (`/guhs vadsrijk wereld ...`): `plek <id>` (elke plek uit POI_VADSRIJK.md, tab-aanvulling), `deur ordetempel|zwamtempel
open`, `vlieg kamp|stad`, `littekens heel|grijs`, `uitkijk <naam>`, `vergeetportaal`.

**NPC's en quests** (`/guhs vadsrijk npc ...`): `log`, `questlog`, `quest start|klaar|vergeet <id>`, `tp <npc-id | poi.<id> |
knuffel.<id> | aanwijzing.<id> | schat.<id>>`, `reset`, `opening`, `bam`, `cliffhanger`, `vlieg stad|kamp`, `npcs`.

**Wezens** (`/guhs vadsrijk wezens ...`): `spawn <soort> [level] [elite]`, `affix <soort> gloeiend|snel|splitsend|schildend <level>`,
`opruimen`, `kampklodder`, `overvlucht`, `draak <ticks>`, `set klodder|zwam <level>`, `loot <soort> <level> [elite]`, `spawners`.
Soorten: klodder, kloddertje, bubbelklodder, stuiterklodder, moederklodder, hoedjesbol, sporenpuffer, zwamwachter, mosgeest,
druipslijm, kristalklodder, knaag, glimzwam, tempelridder, sporenpriester.

**Bazen** (`/guhs vadsrijk bazen ...`): `tp koning|tempel|opperzwam|markt|mika`, `spawn koning_klodder|opperzwam|mika_spion`,
`fase 2|3|4`, `split`, `loot <baas>`, `items`, `herstel`, `tempel reset|open|oplossing`, `mika ontmasker|reset`.
(`bouw koning|tempel|mika` is een noodbouwsel van vóór de kaart: niet meer nodig.)

**Klassen** (`/guhs vadsrijk klasse ...`): `kies`, `boom`, `balie`, `wissel <klasse>`, `wapen <type>`, `skill 0-3`, `adv`,
`respecvrij`, `reset`.

**Economie** (`/guhs vadsrijk eco ...`): `winkel drankjes|uitrusting|smidwaren|cosmetica|algemeen`, `smid`, `markt`, `stal`,
`spul`, `ster <n>`, `verpluis`, `handel <speler>`, `rijdier hoedhupper|klodderros`, `rijdierxp <n>`, `ga smid|drankjes|uitrusting|markt|cosmetica|stal`.

## Waar op letten
**De kaart**
- Knuffelkamp (tenten, Ordetent, kampvuur, trainingsveld, tempeltje, vijver, vuurtoren met kelderluik, het ballonschip).
- Zwamzoet: 9 reuzenzwammen met huisjes, touwbruggen, wolkenliftjes, markt en plein, winkels met inrichting, de stal, de kade.
- De Haven, de Klodderweide (vijvers, slapende slijmheuvel), het Sporenwoud (lichtstralen, waterval met kamer erachter), de
  Druipgrotten (3 ingangen, de verborgen kristalkamer), de Kloof, de Zwamtempel (deur, voorhal, 3 kamers, de koepelzaal).
- 5 uitkijknaalden: bovenaan één keer een panorama. De lucht: tweede (guh-)maan, 's nachts guh-sterrenbeelden, ver weg de draak.
- Zijn binnenruimtes licht genoeg? Staan NPC's en wezens op de vloer (niet in een muur, niet zwevend)?

**NPC's en quests**
- Kleding en namen; zwaaien, rondwandelen, zitten. Het **!** (goud = verhaal, geel = zij) en **?** (inleveren) boven de hoofden.
- Dialogen klik je door ("Verder »"); tussenfilmpjes lopen vanzelf. Questitems vallen alleen als je ze nog nodig hebt.
- 42 quests: 12 hoofd, 22 zij, rijdier (Stalmeester Hoefje, level 8), 6 klassenproeven (Grootmeester, level 10 en 20), 1 geheim.
- Geheimen glinsteren als je dichtbij komt: 10 knuffels (hartjes), 5 aanwijzingen (paars), 5 schatten (goudgeel).

**Wezens**
- Ontvadst = grauw-paars met een chagrijnig guh-gezichtje; na de plof fel, blij, met blosjes, hartjes en roze stof. Geen doodsanimatie.
- Elk wezen zijn trucje (Bubbelklodder belletjes, Stuiterklodder roze cirkel, Moederklodder kloddertjes, Hoedjesbol rode streep,
  Zwamwachter "Geblokt!" van voren, Mosgeest zwiept opzij, Druipslijm valt van het plafond, Kristalklodder weerkaatst spreuken,
  Grauwe Knaag in groepjes, Glimzwam heelt de rest, Tempelzwam-ridder zware zwaai, Sporenpriester buffs).
- Elites: groter, gouden gloed, betere buit. Spawners lopen vanzelf in de vier gebieden (`wezens spawners` telt ze).

**Bazen**
- **Grauwe Klodderreus** (de BAM in Knuffelkamp): springen (rode cirkel), kloddertjes spugen, rondjes rollen.
- **Koning Klodder** (Klodderweide, level 8): sprongen + schokgolf; op 60 % drie Kroonklodders: vrolijk ze binnen 12 s alle drie
  op, anders smelten ze terug; fase 3: sta in een roze cirkel als hij landt. Kans op het Klodderzadel (Klodderros!).
- **De Zwamtempel**: tempeldeur vanaf level 16 (scène + titel), Lichtzwamkamer (volgorde onthouden), Hendelkamer (spiegelverkeerd),
  Ridderzaal (3 golven). `bazen tempel oplossing` verklapt de puzzels.
- **De Opperzwam**: wortels en sporenwolken, op 66 % donker (steek de 4 Lichtzwammen aan), op 33 % groeit hij en breekt de koepel
  open. Na het gevecht is de koepel weer dicht.
- **De Mika-spion**: dakgevecht over drie zwammenhoeden, op 15 % de ballon kapotmaken binnen 20 s. Eerste keer: De Neppe Snor.
- Elke baas: een eigen buitkist per speler die genoeg hielp (ook genezen telt), alles "Gebonden".

**Klassen**
- Schieten met de linkermuis vasthouden; elke wapensoort voelt anders (kruisboog gaat door 1 heen, katapult ploft, boek zoekt).
- Pluisjager: Pluizenregen, Rolsalto (volgende schot = gouden voltreffer), Knuffelpijl (heal of schade), Sterrenschot.
- Vadsmagiër: Roze Vuurbal (element van je staf), Blink, Vadsbron, Meteoorguh.
- Skillboom (K): 22 punten aan knopen tegen 19 punten op level 20; van de twee R-varianten maar één.
- Ordetempel: wisselen en resetten (1 uur wachten). Job-advancements via de Grootmeester-quests (level 10 en 20), met titel op je HUD.

**Economie**
- Munten oprapen (rechtsklik), winkels (kopen, verkopen, "Verkoop alle grijze"), de smid (sterren, verpluisd en repareren,
  herrollen met een vast hartje), de markt (zoeken, filteren, opbrengst ophalen), ruilen (allebei Klaar, allebei Ruilen!).
- Rijdieren: Hoedhupper en Klodderros (fluitje), levels door te rijden, poef als je geraakt wordt of afstapt.
- Cosmetica: kleding, beeldjes, vaandel en titels komen per post in je gewone inventaris thuis.

**Buit**
- ~28 basistypes: kern (kussenzwaard, knuffelknots, pluishamer, pluishelm, knuffelharnas), klassen (klodderkling, pluisboog,
  druppelboog, snoepkruisboog, eikelkatapult, rozenstaf, vlammenstaf, zwamstokje, slijmboek), wezens (14 harnasstukken met de
  Klodderset en de Zwamset), plus 3 sieraden (roze_ring, knuffelketting, pluisoorbel), plus 8 benoemde bazenitems.
- Harnas is nu zichtbaar op je poppetje (elk wezens-stuk in zijn eigen kleur, de rest roze).
- Sets: trek 2 en 4 stukken aan en kijk naar de tooltip en je stats (+4 Behendigheid/Wijsheid telt nu echt mee).

## Het portaal in de Guhmensie
- `/locate structure guhs:vr_w_portaaltempel` (in de Guhmensie). Het portaal slaapt: loop erin en het ontwaakt (scène, de draak brult).
- Daarna kun je zelf een portaal bouwen: raam van Vadsrijk-poortsteen, aansteken met een Vadsrijk-vonk.
- Terugportalen in het Vadsrijk: de kampsteiger, Zwamzoet (west van het plein), De Haven.

## Bekende gaten en placeholders
- Geen muziek (de haakjes voor bazenmuziek staan klaar); geluiden zijn vanilla.
- Models en itemplaatjes zijn gegenereerd (een eerste versie); questitems hebben vanilla-plaatjes.
- Geen Guhdex-pagina's voor de wezens (het bestiarium staat in de questlog, tab Wezens).
- Balans is een eerste gok (HP, schade, prijzen, sterkansen, xp-curve).
- Curios/Accessories-vakken gaan mee in de inventariswissel, maar zijn niet getest met de echte mods.
- Sieraden (roze ring, knuffelketting, pluisoorbel) zijn zeldzame buit van elites en bazen.
- De blauwe stipjes in de lucht zijn de sporen-sfeer (vanilla "warped spore"); misschien wat veel.
- Grotten van de wereld zijn "uitgehold": buiten de gangen zitten afgesloten, pikdonkere luchtbellen. NPC's zoeken nu alleen een
  verlichte vloer als hun plek niet precies klopt; zie je toch ergens een NPC of wezen in het donker in de rots: meld de plek.
