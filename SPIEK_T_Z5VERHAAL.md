# Spiekbrief zone 5 "Hoogvads": het verhaal (t-z5verhaal)

Hoogvads is de feestelijke hoofdstad op de rug van Moeder Wolkwal (lv 56-60). Het is er veilig. Het hoofdverhaal heet **"Verraad in de stad"**: een speurdersverhaal over een Mika-spion in de Hoge Raad. De walvis is de inzet.

> **Let op:** de stad zelf (de blokken) bouwt t-z5wereld later. Tot die tijd staan de NPC's op **voorlopige plekken** (ze zweven als er nog geen vloer is). Het verhaal en de dialogen kun je al helemaal spelen met de commando's hieronder.

Alle commando's beginnen met `/guhs vadsrijk z5verhaal`. Je moet in het Vadsrijk zijn voor `tp` en `vlieg`.

## 1. Klaarzetten
| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z5verhaal klaarzetten` | Zone 1-4 klaar (alle hoofdquests + vlaggen `zone1..4_klaar`), level 56. **z5_hq01 "De Zwevende Stad"** start vanzelf. |
| `/guhs vadsrijk z5verhaal vlieg` | De vlucht naar Hoogvads. De eerste keer: de **aankomstfilm** (vanaf de Opstijgpier langs de kop van de walvis, haar oog gaat open en lacht, over de rand, de stad in het goud, landing op de Luchthaven). |
| `/guhs vadsrijk z5verhaal quest <1-10>` | Spring naar hoofdquest 1-10 (alles ervoor wordt afgevinkt, ook de sporen in je boekje). |
| `/guhs vadsrijk z5verhaal stap` | Vinkt de huidige stap van je zone-5-quest af (overslaan). |
| `/guhs vadsrijk z5verhaal tp <naam>` | Naar een NPC of plek. Tab geeft de lijst, bv. `koningin`, `inspecteur`, `havenmeester`, `zachtpels`, `mientje`, `spuitgat`, `ooggaarde_balkon`. |
| `/guhs vadsrijk z5verhaal vergeet` | Wist alles van zone 5 (quests, sporen, geheimen, gezien-films). |

## 2. Het hoofdverhaal (10 stappen)
| # | Quest | Wie / waar | Wat moet er gebeuren |
|---|---|---|---|
| 1 | De Zwevende Stad | Havenmeester Windvaan (Luchthaven) → Koningin Zachtmoed (troonzaal) | Aankomstfilm, meld je bij de havenmeester, loop de Zachtmoedlaan op, de trap op naar de troonzaal. Je krijgt een stukje kaas. |
| 2 | Verraad in de stad | Koningin → Inspecteur Snuffelaar (zijkamer Raadszaal) | De gescheurde bladzijde is gestolen. Je krijgt het **Speurdersboekje**. Lessenaar onderzoeken (spoor: grijze haartjes), **verhoor** Raadsvrouwe Inktvlek. |
| 3 | De mol in de Wolkwacht | Kapitein Veerkracht (Academie) | Kastjes van de kadetten (spoor: bevelbriefje met pootjesstempel), **verhoor** Kadet Plukje. |
| 4 | Soep en geruchten | Kok Pollepel (Grote Soepkeuken) | **Verhoor** de kok, deel 4 kommen soep uit (klik op de 4 vluchtelingen in de rij), **verhoor** Mager Mientje → de **herinnering** (flash F3: haar vlucht uit Grauwhoven). |
| 5 | Wie vertrekt er stiekem? | Havenmeester → nachtlogboek → Raadsheer Gouddraad (bank) | Spoor: het logboek ("Z.P." naar de Stormrand), **verhoor** Gouddraad (zegel van de Kanselarij). |
| 6 | De laatste twee verhoren | Generaal Donsveer (toren Academie) + Kanselier Zachtpels (Kanselarij) | **Verhoren** in willekeurige volgorde. Bij Zachtpels: de kaastest! |
| 7 | Het bewijsbord | Inspecteur Snuffelaar (of rechtsklik op je boekje) | **Beschuldig de verrader.** Fout = uitleg, geen straf. Goed = de **ontmaskering** in de Raadszaal (een rits in een guh-pak: Kootje Zilverstaart van de Ringvinger). |
| 8 | De Wolkwaldroom | Het middelste spuitgat | Achtervolging; de **finale**: Zilverstaart verslaan + de Grauwe Droom van Moeder Wolkwal opvrolijken. Daarna leest de Koningin de bladzijde voor: de legende van de Keizer en de Lege Schaal. |
| 9 | Het slaaplied | Balkon van de Ooggaarde | Zing het slaaplied: Moeder Wolkwal wordt wakker en knipoogt. Titelkaart **"Ridder van Hoogvads"**, vlag `zone5_klaar`. |
| 10 | De Stormrand | Kanselarij → oostpier → Havenmeester | De **Grauwe brief** "Aan de Ringvinger", en de **cliffhanger**: een bliksemmuur, de Grauwe Vloot, De Gouden Greep, tien ringen aan tien tenen. "Wat een mooi lampionnetje, die walvis. Die wil ik." |

**Waar je op let:** de tracker (rechtsboven + lichtstraal) wijst elke stap aan met wie/waar/wat. Na stap 7 is Kanselier Zachtpels voor jou een "kartonnen Kanselier" (grapje, omdat NPC's voor iedereen zichtbaar blijven).

## 3. Speurwerk los testen
| Commando | Wat je ziet |
|---|---|
| `/guhs vadsrijk z5verhaal verhoor <wie>` | Een verhoor met keuzes: `inktvlek`, `plukje`, `kok`, `mientje`, `gouddraad`, `donsveer`, `zachtpels`. Per verhoor levert één vraag het spoor op ("Nieuw spoor in je Speurdersboekje: ... (n/10)"). Je mag alle vragen stellen; daarna verschijnt "Dat was het. Dank u." |
| `/guhs vadsrijk z5verhaal scene bord` | Het **bewijsbord**: pagina 1 de sporen (✔ of ???), pagina 2 de verdachten (vrijgepleite krijgen "(vrijgepleit)"), pagina 3 als de 3 doorslaggevende sporen er zijn. |
| `/guhs vadsrijk z5verhaal bewijs` | Alle 10 sporen in je boekje (om het bord vol te zien). |
| `/guhs vadsrijk z5verhaal quest 7` en dan het bord | Nu staan er knoppen "Beschuldig ...". Probeer eerst een foute (uitleg met het bewijs dat hem vrijpleit), dan Zachtpels → ontmaskering. |

## 4. Losse scènes
`/guhs vadsrijk z5verhaal scene <naam>` met: `aankomst`, `herinnering` (F3), `ontmasker`, `spion` + `droom` (de finale zonder baasgevecht), `ontwaken`, `brief`, `cliffhanger`, `klaar` (titelkaart + vlag).

`/guhs vadsrijk z5verhaal finale` start de volgende fase van de finale (tijdens quest 8, na het spuitgat). Zonder de bazen van t-z5wezens speelt er een filmpje dat de fase zelf afrondt.

## 5. Zijquests (11)
| Quest | Bij wie | Wat |
|---|---|---|
| Een kom voor iedereen | Kok Pollepel | 8 wolkbessen (roze glinsters) in het Staartpark en de Ooggaarde |
| Pluisje's knuffel | Pluisje (soepkeuken) | Knuffelkonijn Flap achter het beeld van de Eerste Knuffel (kathedraal) |
| Brieven naar huis | Opa Grauwsnor | 3 brieven: havenmeester, Zuster Zachtzang, Pluisje |
| Het Oog meten | Professor Brilsma | De sterrenwacht van de universiteit |
| De adem van Moeder Wolkwal | Professor (na quest 9) | 3 spuitgaten |
| Proef: het Wolkenparcours | Kapitein Veerkracht | 4 palen op het oefenplein |
| Proef: de verdwaalde kadet | Kapitein Veerkracht | Kadet Dons van de Luchthaven naar de Academie begeleiden (hij loopt mee) |
| Marktdrukte | Kraamguh Lientje | Pakjes naar bakker, kleermaker, juwelier |
| Water voor het Staartpark | Tuinguh Stekje | 4 bloesembomen |
| De kroon van wol | Hofdame Breinaald | 3 bolletjes roze wol (markt, paleishof, universiteit) |
| Het lied van de Eerste Knuffel | Zuster Zachtzang (na quest 9) | 2 roosvensters + het beeld |

## 6. Stad, teasers en ambachten
- **4e klasse (alleen teasers, niet speelbaar):** Vadszanger Lieflied bij de kathedraaltrappen, Pluisninja Schaduwdons bij het oefenplein. Klik → "Wat staat er in de vitrine?" → de Guhfluit / de Pluisdolkjes, "Binnenkort." (`tp vadszanger`, `tp pluisninja`)
- **Ambachtsgilde** naast de universiteit: Gildemeester Duimdop + leermeesters koken, brouwen, smeden, naaien, imkeren. Nu nog een "binnenkort"-zinnetje; de ambachten-slice kan ze later koppelen. (`tp gildemeester`)
- **Winkels:** bakker (drankjes), kleermaker, smid, juwelier, Lientje (veilinghuis), stalmeester bij de Luchthaven.
- **Reizen:** Havenmeester Windvaan → reiskaart (Hoogvads staat er nu op, rechtsboven) en vluchten naar alle eilanden. Na zone 4 staat "Naar Hoogvads" ook bij de andere kapiteins.

## 7. Verkenningsboek (toets B), pagina "Hoogvads"
Hoofd- en zijquests, geheimen (3 schatten + de Grauwe brief), 8 knuffels, de bazen (Zilverstaart, de Grauwe Droom), 4 uitkijkpunten (Ooggaarde-balkon, Geveltoren, Staartvin, Sterrenwacht: panoramafilmpje), 4 kampvuren (Luchthaven, markt, Staartpark, Ooggaarde) en 5 gebieden. Titel bij 100%: **Kenner van Hoogvads**.

## 8. Wat (nog) niet kan
- De stad, de kampvuurblokken en de vitrines bestaan pas als t-z5wereld ze bouwt. De plekken zijn nu schattingen uit de concept-renders.
- De echte finale-gevechten zijn van t-z5wezens; tot dan speelt een filmpje.
- De herinnering van Mientje (F3) is een verteld filmpje, nog niet speelbaar (dat kan pas met een kaart van het keizerrijk).
