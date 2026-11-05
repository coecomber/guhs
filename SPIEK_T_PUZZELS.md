# Spiekbrief: Puzzels en geheimpjes (T-PUZZELS)

22 puzzels en geheimpjes in zone 1-3 (8 + 7 + 7). Elk heeft een beloning (Vadsmunten, soms een gescheurde bladzijde over de Keizer, spullen of iets voor je kledingkast). Ze staan in het **Verkenningsboek** onder **"Puzzels en geheimpjes"** en tellen mee voor het zonepercentage.

## Zo werkt het (voor jou)
- **Waar staan ze?** Een puzzel verschijnt vanzelf de eerste keer dat je binnen ~70 blokken van zijn plek komt. Dat is een vlak plekje vlak bij een kampvuur of POI. Daarna blijft hij daar staan.
- **Onderaan je scherm** staat steeds wat je moet doen (de "puzzeltip constant in beeld"). Geheimpjes zeggen niks.
- **Na 3 foute pogingen** krijg je tip 1 in de chat, **na 6** tip 2. Daarna staat de tip onderaan je scherm.
- **Opgelost?** Iedereen die vlakbij staat krijgt de beloning, maar **één keer per speler**. De puzzel zet zichzelf na ~20-30 s terug. Een half opgeloste puzzel zonder spelers in de buurt zet zich na 2 minuten terug.
- **Tijdritten en raadsels** zijn per speler (iedereen zijn eigen stopwatch of eigen antwoord).
- **Je antwoord op een raadsel** zien andere spelers niet in de chat.

## Dev-commando's (op 2)
| commando | wat het doet |
|---|---|
| `/guhs vadsrijk puzzel lijst` | alle puzzels: ✔ = jij hebt hem, met de plek (of "nog niet geplaatst" + anker) |
| `/guhs vadsrijk puzzel tp <id>` | naar de puzzel (staat hij er nog niet? Dan naar het anker; hij verschijnt binnen 1 s; doe dan nog eens tp) |
| `/guhs vadsrijk puzzel los <id>` | lost hem op via de echte invoer (klikken/platen/duwen/chat), dus je krijgt ook de beloning |
| `/guhs vadsrijk puzzel reset <id>` | zet de puzzel terug + vergeet je foute pogingen (tips weer weg) |
| `/guhs vadsrijk puzzel hint <id>` | toont uitleg, tip 1, tip 2, de boek-aanwijzing (en de antwoorden van een raadsel) |
| `/guhs vadsrijk puzzel vergeet <id>` / `vergeet alles` | jouw beloning vergeten (dan krijg je hem opnieuw) |
| `/guhs vadsrijk puzzel nacht` | sterrenpuzzels doen alsof het nacht is (aan/uit) |
| `/guhs vadsrijk puzzel plaats <id>` / `plaats <id> opnieuw` | nu plaatsen / een nieuw plekje zoeken |
| `/guhs vadsrijk puzzel hier <id>` | een testkopie vlak voor je neerzetten (overal, ook buiten het Vadsrijk; telt niet als de echte plek) |
| `/guhs vadsrijk puzzel bladzijde <n>` | bladzijde n (1-9) lezen + krijgen |
| `/guhs vadsrijk puzzel herlaad` | de puzzeldata opnieuw lezen |

Snelle rondgang: `/guhs vadsrijk puzzel tp z1_kleurplaten` → wacht 1 s → nog eens → puzzelen → `/guhs vadsrijk puzzel lijst`.

## Zone 1: De Knuffelzwammen
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z1_kleurplaten` | De Kleurplaten | drukplaten in volgorde | bij kampvuur Klodderweide 1 | **groen, roze, blauw, geel** (stap erop) | 4 zilver + bladzijde 1 |
| `z1_zwammenlied` | Het Zwammenlied | klankstenen (lied naspelen) | bij kampvuur Sporenwoud 1 | klik eerst de voorspeelsteen; dan **links, rechts, midden, rechts, voorste** | 5 zilver + 2 verbetersteentjes |
| `z1_lichtstraal` | De Verdwaalde Lichtstraal | spiegels + lichtstraal | bij lichtstraal 1 (Sporenwoud) | straal: 2× omlaag, 1× rechts; spiegel linksonder en rechtsboven niet nodig | 6 zilver + bladzijde 2 |
| `z1_gebarsten_muur` | De Gebarsten Muur | **geheimpje** (barst) | bij kampvuur Druipgrotten 3 | klik op de barst in het muurtje → de muur zakt in | 3,5 zilver + bladzijde 3 |
| `z1_fluistersteen` | De Fluistersteen | raadsel (chat) | bij kampvuur Klodderweide 3 | klik de steen, typ **vads** | 3 zilver + herrolknabbel |
| `z1_sterrenveld` | Het Sterrenveld | sterrenbeeld ('s nachts!) | Hoedhupperwei | klik de sterrenkaart; het is een **hartje**: rij 1: 2+4, rij 2: 1+3+5, rij 3: 1+5, rij 4: 2+4, rij 5: 3; dan de knop | 7 zilver + **Sterrenstofvleugels** (ontgrendeling) |
| `z1_vuurtorenschuur` | Het Schuurtje bij de Vuurtoren | **geheimpje** (verstopte hendel) | Knuffelkamp, bij de vuurtoren | hendeltje **achter** het schuurtje tussen de struikjes → de deur gaat open | 2 zilver + Klodderreus-knuffel |
| `z1_symboolslot` | Het Tempelslot | symboolslot | op weg naar de Zwamtempel (kampvuur 1) | **hart, ster, maan**, dan de knop | 8 zilver + 3 verbetersteentjes |

## Zone 2: De Splinterpieken
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z2_ijsschuif` | De Ijsblokken | schuifblokken | bij kampvuur Knuffelmeer 1 | linkerblok 2× omlaag, rechterblok 2× omhoog (klik = duwen van je af); vast? de gele herstelknop | 18 zilver + bladzijde 4 |
| `z2_kristallied` | Het Galmlied | klankstenen | bij kampvuur Galmhellingen 1 | **midden, linksonder, linksboven, rechtsonder, rechtsboven, midden** | 20 zilver + 3 verbetersteentjes |
| `z2_kristalspiegels` | De Kristalspiegels | spiegels + lichtstraal | bij kampvuur Splinterkloof 3 | omhoog bij de 1e spiegel, rechts, omhoog, dan helemaal naar rechts | 24 zilver + bladzijde 5 |
| `z2_poollicht` | Het Poollichtveld | sterrenbeeld ('s nachts!) | bij kampvuur Knuffelmeer 2 | **ruit met een stip**: rij 1: 3, rij 2: 2+4, rij 3: 1+3+5, rij 4: 2+4, rij 5: 3; dan de knop | 26 zilver + **Poollichtvleugels** |
| `z2_lantaarnsprint` | De Lantaarnsprint | tijdrit (25 s) | bij kampvuur Galmhellingen 2 | klik de lantaarn, spring over de kristalpilaren naar de gouden eindplaat bovenop | 22 zilver + **Sneeuwvlokcape** |
| `z2_ijsgrot` | Het Bevroren Holletje | **geheimpje** (barst) | bij kampvuur Splinterkloof 1 | klik op de barst | 15 zilver + bladzijde 6 |
| `z2_raadselsteen` | De Bibberende Steen | raadsel (chat) | bij het Meerkampje | klik de steen, typ **ijs** | 16 zilver + 2 herrolknabbels |

## Zone 3: De Schemerwortel (alles beneden langs de Wortelweg)
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z3_wortelplaten` | De Wortelplaten | drukplaten | bij kampvuur Wortelweg 1 (oost) | **geel, blauw, roze, groen** | 45 zilver + 4 verbetersteentjes |
| `z3_bijenlied` | Het Bijenlied | klankstenen | bij kampvuur Wortelweg 2 | achterste rij 1+2, voorste rij rechts + links, achterste rij 3+4 | 50 zilver + bladzijde 7 + **Honingraatcape** |
| `z3_wortelschuif` | De Wortelknoesten | schuifblokken (3) | bij kampvuur Wortelweg 3 (west) | begin rechtsonder: 1× omlaag; dan de andere twee naar hun vakje | 55 zilver + 4 verbetersteentjes |
| `z3_lantaarnloop` | De Lantaarnloop | tijdrit (30 s) | bij kampvuur Wortelweg 4 | onderste rij naar rechts, rechterkant omhoog, bovenste rij naar links (de pilaren in het midden zijn doodlopend) | 60 zilver + bladzijde 9 + **Lantaarnmotvleugels** |
| `z3_mossymbolen` | Het Mosslot | symboolslot (4) | bij kampvuur Wortelweg 5 | **blad, druppel, zon, blad** | 50 zilver + **Moscape** |
| `z3_schorsdeur` | Het Schorshuisje | **geheimpje** (verstopte hendel) | bij kampvuur Wortelweg 6 | hendeltje achter het huisje | 40 zilver + bladzijde 8 |
| `z3_honingfluister` | De Zoemsteen | raadsel (chat) | bij de Mosvoet | klik de steen, typ **bij** (of bijtje/bijen) | 40 zilver + 3 herrolknabbels |

## Wat moet je zien
- **Bij aankomst:** een netjes vlak plekje met een echte vloer (mos, sneeuw of aangestampte modder per zone). Is er hoogteverschil? Dan lopen er traptreden naar het omringende land. Geen planten of struiken in de weg.
- **Drukplaten:** de plaat licht op als je erop stapt. Fout? Alles gaat uit met een bromtoon en je ziet "Hmm, dat klopt niet".
- **Klankstenen:** de voorspeelsteen speelt het lied en laat de stenen één voor één oplichten. Elke steen heeft zijn eigen toon.
- **Spiegels:** een geel lichtspoor (deeltjes) laat zien waar de straal nu loopt. Raakt hij het doel? Dan licht het doel roze op.
- **Schuifblokken:** klik = het blok schuift van je af. Een roze vakje licht op als er een blok op staat.
- **Tijdrit:** de lantaarn wordt goud en onderaan telt "⏱ Nog 24" af. Te laat: "De tijd is om!".
- **Sterren:** overdag zegt een steen "De sterrenstenen slapen nog...". 's Nachts (of met `puzzel nacht`) lichten ze op als je klikt. De sterrenkaart (houten blok) zet het sterrenbeeld als ★ in de chat.
- **Geheimpjes:** de barst of het hendeltje glinstert heel zachtjes (lichtniveau 3). Opgelost: de muur of deur gaat ~30 s open, met de titel "✦ Geheimpje gevonden".
- **Beloning:** een titel in beeld + chatregels: "+ 4 zilver", "+ een gescheurde bladzijde: ...", "+ ontgrendeld voor je kledingkast: ...". Een bladzijde lees je met rechtsklik.
- **Verkenningsboek:** onder "Puzzels en geheimpjes" (amethist-icoon) zie je per zone x/y. Geheimpjes heten "???" tot je ze vindt. De hint is een aanwijzing zoals "Bij het eerste kampvuur van de Klodderweide liggen vier gekleurde platen."

## De 9 gescheurde bladzijdes (de legende van de Lege Schaal)
1 Een gewone kleine Mika · 2 De eerste kaasknabbel · 3 Wat elke guh weet · 4 Lange armen · 5 Een gestolen kroontje · 6 De Lege Schaal · 7 Nooitgenoeg · 8 Het Oog · 9 Een eenzaam katje. Lezen: `/guhs vadsrijk puzzel bladzijde <n>`.
