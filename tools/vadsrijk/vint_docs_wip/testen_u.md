## Nieuw in ronde U (feedback ronde 4): de testlijst

Speel vanuit **`guhs_vadsrijk\v-int\run-client.bat`** in een **nieuwe wereld**: de kaart is opnieuw gegenereerd.
De commando's per punt staan in SPIEKBRIEF.md, hoofdstuk 8. Hieronder staat per onderdeel wat je test en wat goed is.

| # | Onderdeel | Test | Goed als |
|---|---|---|---|
| U1 | Vadsrijk-menu (H) | Druk H in het Vadsrijk en klik alle hoofdtabs af; Ctrl+Tab bladert. | Eén kader, even grote tabs, niets buiten beeld, GUI-schaal 3/4 ook netjes. |
| U2 | H buiten het Vadsrijk | In het Guheinde: H ingedrukt houden. | Het krachtenwiel (Vadskunst) komt, geen Vadsrijk-menu. Geen rode botsing in Besturing. |
| U3 | Toetsen in het pauzemenu | Druk Esc in het Vadsrijk. | Rechts het toetsenoverzicht, met H (menu), `;` (emotes), Alt+1/2/3 (riem). |
| U4 | Karakters | Nieuwe wereld: het welkomstscherm na het filmpje. Probeer daarna de ⚔-knop, de tab Ridders en shift+klik op een kampvuur. | Maken lukt de eerste keer; 3 vaste slots (Normaal / Knuffel / Eénvads), de modus is niet te wisselen. |
| U5 | Rustende Ridders-zaal | `/guhs vadsrijk karakter zaal tp` | De zaal staat er zonder `zaal bouw`. |
| U6 | Luchtschip | Klik op ballon, staart en oortjes; loop met WASD over het dek; probeer een dichte lijn met een nieuw karakter. | Je stapt in, loopt vrij rond tot de reling, en ziet uitleg over de quest + het level. |
| U7 | Overgangen | `/guhs vadsrijk indeling rand wolkenkloof_noord`, en vlieg rond. | Geen rechte kleur- of wolkenlijnen, geen onderwaterklif. |
| U8 | Hekken | Kijk bij de kampen, de Boomtrap, de vlaggetjes in de Splinterpieken, Hoogvads, de kastelen en de huizenwijk. | Overal doorlopende lijnen. |
| U9 | Dubbel | `npc tp z2_kapitein` / `z3bazen tp kruinroofvogel`, weg en terug, dan `/guhs vadsrijk dubbel scan`. | Steeds 1 Splinter en 1 Kruinroofvogel; de scan meldt 0. |
| U10 | Zichtbaarheid | `z3npc klaarzetten` + F5; vraag een tweede speler om te kijken. | Cape, handschoenen en sieraden zijn zichtbaar, ook voor anderen. |
| U11 | Deco | Kamp 's nachts, Honingtak, de wenteltrap, de arena-onderkant, het nest, het Schildpadeiland. | Zie SPIEKBRIEF 8.3, 8.10-8.13 en 8.16. |
| U12 | Specialisaties | `/guhs vadsrijk spec ...` (SPIEK_U_SPECS), spec kiezen op lv 17. Bekijk de Karakter-tab en de party-frames. | Er staat "Specialisatie: X"; respec gratis bij de klasse-NPC; de tooltip toont ▲/▼. |
| U13 | Party, emotes, HUD | `/guhs vadsrijk hud party nep`, `;` voor het emotewiel, Instellingen › HUD aanpassen. | Frames links, de poses zien er goed uit, en wat je versleept blijft bewaard. |
| U14 | Grotten | `/guhs vadsrijk grot tp <zwamgrot/kristalgrot/wortelgrot/koraalgrot/wolkengrot>` | Een ontdekkaart; de schat gaat pas open na de puzzel; de puzzel telt 1× in het boek en staat in de tab Puzzels. |
| U15 | Ruiters | `/guhs vadsrijk ruiters lijst` en de tp's. | Ze rijden soepel over de trappen, zonder dubbele ruiters. Stem de zadelhoogte en snelheid af op wat jij ziet. |
| U16 | Audio | `/guhs vadsrijk geluid fase <baas>`, `fanfare <baas>`, en loop door de stadjes en grotten. | De fase-overgang is hoorbaar, er klinkt een fanfare, en de wereldgeluiden zijn subtiel. |
| U17 | Ambachten T4/T5 | `/guhs vadsrijk ambacht tp z4_duik_koraalbaai`, de Knutselwinkel en het Opdrachtenbord. | Duiken werkt; T5 kan alleen in het Gilde; het Opdrachtenbord kan niet dupen. |
| U18 | Knuffelbond | Tab Knuffelbond, `/bond`, oprichten bij Zegeltje (Hoogvads), de Bondenpoort, een kasteel bouwen. | Het kasteel staat met verbonden hekken; de banier krijgt kleur; de kluis werkt. |
| U19 | Huizen | Kwartiermeester op een plein, een huis kopen, inrichten, de brievenbus. | Lantaarnpalen = guh-lantaarnpaal; de rechten kloppen; 1 huis per account. |
| U20 | Zone 5 | De kathedraal, universiteit, troonzaal, staartvin en Wolkengrotten. | Diepte, kroonluchters, licht; de 4 staartvinplekken zijn te belopen. |
| U21 | Vliegend tapijt | SPIEK_U_TAPIJT | Glijden werkt; de eerste rit toont alleen de tapijt-tip. |
| U22 | Zone 6 rondkijken | SPIEKBRIEF hoofdstuk 6: luchthaven → Siq → Saffraanpoort lopen in survival; Schatkamer van het plein af bekijken. | Geen sprongen > 1 blok, diepe Schatkamer-gevel met vrijstaande zuilen, reliëf in de kloof. |
| U23 | Zone-6-verhaal | Questlopen (hoofdstuk 0) of zelf: zone 5 af → zwemfilm → lijn E → "Land in zicht!" … tot Ringeling. | Alle films spelen, bakens gaan aan, Ringeling wordt opgevrolijkt en staat daarna als NPC in Saffraanpoort. |
| U24 | Raid | `/guhs vadsrijk z6raid tp wachtzaal`, `z6raid start 2` (met nep-leden), de 4 bazen. | Raid-frames rechts (verschuifbaar in HUD aanpassen), torendrone in de Wachtzaal, bazenmuziek met fases, kist per clear. |
| U25 | Rijdier-drops | `/guhs vadsrijk loot kansen`, `loot pech <bron> 15`, `loot rol <bron>`. | 10 % per worp; na 15 keer mis zeker. Sterrennacht-Bultguh krijgt ook de Bultguh erbij. |
| U26 | Questlopen | `/guhs vadsrijk questlopen` (of H › Instellingen › Questlopen). Reset hoofdlijn, dan Volgende… | Je loopt de hele hoofdlijn zone 1 → 6 door met echte gesprekken/films; bazen zelf of laten verslaan. |
| U27 | Grotkerkers | `/guhs vadsrijk grot run`, ga een grot in, versla de eindbaas. | Poort dicht tijdens de run, eindbaas + eigen kist, na 10 min reset (ongeopende kisten weg). |
| U28 | Stadsleven | `/guhs vadsrijk leven tp <stad>`, `leven speeltuin <stad>`, overdag en 's nachts. | Speeltuin met verbonden hekjes, terrassen, beestjes; 's nachts winkels dicht maar quest-NPC's op hun plek. |
| U29 | Ritme + weer | `/guhs vadsrijk ritme status`, een emote (`;`) bij een guh, het Pluizenblaadje. | Reacties + knuffelmeter, weer met charme, nachtbel. Werkt ook in Rozenkloof/Saffraanpoort. |
| U30 | Geroezemoes | Loop over de Marktlaan in Hoogvads en door de bazaar van Saffraanpoort. | Zacht guh-geroezemoes, schuif "geroezemoes" in Instellingen. Niet te aanwezig. |
| U31 | Opslag | Probeer het eerste extra knapzak-vak te kopen; kijk naar de andere vakken. | Eerst de VIP++-grap (eenmalig), dan de koop. Andere vakken: "Binnenkort". |
| U32 | Website | https://guhs.nl/vadsrijk-preview/ | Zone 6, raid, kansen, renders, tabellen, live posities. |
