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
