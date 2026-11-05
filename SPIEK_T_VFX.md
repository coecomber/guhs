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
