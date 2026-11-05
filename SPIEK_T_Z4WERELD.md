# SPIEK T-Z4WERELD - zone 4 "De Lampionkust" (de kaart)

Zone 4 is een slinger van eilandjes in een roze zee, rechts van de Schemerwortel (x 1740..2540, z -716..-266, lv 50-56).
Alles staat op de kaart; de wezens, het verhaal en het zeilbootje komen van andere slices. Je komt er nu met de dev-commando's
(teleport). Elke regel `/guhs vadsrijk z4wereld plek <id>` brengt je erheen (ook van buiten de dimensie). Tab-aanvullen werkt.
Losse coordinaten staan erachter als je liever `/tp` gebruikt (in de dimensie `guhs:vadsrijk`).

**Kaart opnieuw maken:** `python tools/vadsrijk/maak_kaart.py --zone 4` (~3 min). Controle: `python tools/vadsrijk/zone4/loopcheck4.py`,
`python tools/vadsrijk/zone4/poicheck4.py`, `python tools/vadsrijk/zone4/audit4.py`. Een NIEUWE wereld/regen is nodig om de tegels te zien
(chunks die al bestaan veranderen niet).

## Lampionhaven (de hub)

- `/guhs vadsrijk z4wereld plek respawn`  (1848 67 -467) Het Havenplein: fonteintje met guh-beeldje, de lampionmast met snoeren als een tentje, kampvuur ernaast (unlit = checkpoint).
- `/guhs vadsrijk z4wereld plek kade`  (1866 66 -470) De kade met de 3 steigers en bootjes. Loop de steigers op: de lampionsnoeren hangen nu boven je hoofd, aan het eind een ladder uit het water.
- `/guhs vadsrijk z4wereld plek haven_3_4_landing`  (1796 71 -468) De luchthaven (lijn C naar de Boom): platform op palen met het aangemeerde luchtschip, vertrekbord. Trap aan de oostkant naar beneden.
- `/guhs vadsrijk z4wereld plek gebouw.vliegerwinkel.deur`  (1844 75 -481) Vliegerwinkel (winkel): toonbank, weefgetouw. Trap van de deur naar beneden moet aansluiten.
- `/guhs vadsrijk z4wereld plek gebouw.winkel.deur`  (1843 67 -445) Algemene winkel: toonbank + kist.
- `/guhs vadsrijk z4wereld plek gebouw.herberg.deur`  (1829 68 -466) Herberg: bed, tafeltjes.
- `/guhs vadsrijk z4wereld plek gebouw.bank.deur`  (1829 72 -480) Bank: toonbank + goudblok (kluis).
- `/guhs vadsrijk z4wereld plek gebouw.lampionwerkplaats.deur`  (1835 67 -448) Lampionwerkplaats (ambacht): werkbank + weefgetouw, afdak vol lampions.
- `/guhs vadsrijk z4wereld plek gebouw.kookhuis.deur`  (1852 67 -445) Kookhuis (ambacht): smoker + werkbank.
- `/guhs vadsrijk z4wereld plek gebouw.havenmeester.deur`  (1860 66 -447) Havenmeester: toonbank + lessenaar.
- `/guhs vadsrijk z4wereld plek gebouw.botenverhuur.deur`  (1870 66 -490) Botenverhuur op de noordarm (paalwoning, loopplank vast aan de kade).
- `/guhs vadsrijk z4wereld plek gebouw.visrestaurant.deur`  (1909 66 -492) Visrestaurant 'De Blije Bot' met terras boven het water.
- `/guhs vadsrijk z4wereld plek gebouw.stal.deur`  (1818 67 -452) Stal + de wei met hek (hooi, waterbak).
- `/guhs vadsrijk z4wereld plek boot_dok_lampionhaven`  (1891 65 -464) Het eind van de middelste steiger: hier vertrekt het zeilbootje (t-z4boot).
- `/guhs vadsrijk z4wereld plek uitkijk_lampionduin`  (1860 90 -517) Uitkijkpunt op de Lampionduin: panorama op de Boom (zone 3). Trappenpad vanaf het plein.
- `/guhs vadsrijk z4wereld plek vuurtorentje`  (1912 64 -436) Het vuurtorentje met oortjes op de punt van de zuidarm.

## De eilandjes

- `/guhs vadsrijk z4wereld plek eiland_knuffeltij`  (1935 67 -345) Knuffeltij-strand: grauwe knuffels in de branding, het Opvangcentrum (binnen: knuffels op de toonbank), knuffelwas, droogwaslijnen, soepkraampje.
- `/guhs vadsrijk z4wereld plek eiland_strandfeest`  (1995 68 -605) Strandfeesteiland: parasols, kampvuur (checkpoint) met dansplek, schelpenlimonadebar, beachvolleybal.
- `/guhs vadsrijk z4wereld plek eiland_bloemeneiland`  (2085 71 -405) Bloemeneiland: bloemenstroken, bijenboerderij (binnen), bijenkorven, picknickkleedjes.
- `/guhs vadsrijk z4wereld plek eiland_koraalbaai`  (2185 64 -565) Koraalbaai: atol met roze lagune. Duiksteiger vanaf de noordring, het ronde vlot in het midden = de arena van de Grauwe Zeeslurf.
- `/guhs vadsrijk z4wereld plek eiland_vliegereiland`  (2299 89 -665) Vliegereiland: vliegers (wol, met oortjes en oogjes) aan touwtjes op de helling, wedstrijdpaal op de top, vliegerwerkplaats.
- `/guhs vadsrijk z4wereld plek eiland_vuurtoreneiland`  (2404 83 -490) Vuurtoreneiland: de grote vuurtoren (deur aan de zuidkant, wenteltrap naar de galerij) + het huisje van de wachter.
- `/guhs vadsrijk z4wereld plek eiland_pierkop`  (2470 73 -565) Pierkop: de Opstijgpier (pier op palen naar het oosten, rond platform met meermast).
- `/guhs vadsrijk z4wereld plek eiland_mysterie`  (2480 79 -330) Mysterie-eiland: GRIJZE MIST (de Grauwe Mistbank, zicht ~14 blokken), vervallen grauwe poort, bleke bomen.
- `/guhs vadsrijk z4wereld plek schildpad`  (2239 72 -445) Schildpadeiland (stilstaande versie): schild met hutje, palm en bloementuintje; drijvende steiger aan de flank met pad omhoog.
- `/guhs vadsrijk z4wereld plek wrak_ingang`  (2178 67 -700) Het Wrak van de Vliegervanger (kerker): gestrand Mika-schip op een rots in het zuiden.

## Het Wrak van de Vliegervanger (kleine kerker)

- `/guhs vadsrijk z4wereld plek wrak_aanleg`  (2192 64 -700) Steiger met ladder; loop over de rots, 3 treden op naar het achterdek.
- `/guhs vadsrijk z4wereld plek wrak_luik`  (2176 67 -705) Het luik in het achterdek: 9 treden naar beneden het ruim in.
- `/guhs vadsrijk z4wereld plek wrak_ruim`  (2175 57 -700) Kamer 1, het ruim (tonnen). Deuropening naar het westen.
- `/guhs vadsrijk z4wereld plek wrak_kombuis`  (2165 57 -700) Kamer 2, de kombuis (smoker).
- `/guhs vadsrijk z4wereld plek wrak_knuffelkooi`  (2155 57 -700) Kamer 3, de knuffelkooi: gestolen knuffels achter tralies, schatkist. Trap omhoog naar het voordek.
- `/guhs vadsrijk z4wereld plek arena_vliegervanger`  (2158 67 -700) Het voordek = de baasarena (de Vliegervanger), met de grauwe oorlogsvlieger aan de mast.

## Koraalbaai (wereldbaas + duiken)

- `/guhs vadsrijk z4wereld plek duiksteiger`  (2185 64 -605) Duiksteiger vanaf de noordring de lagune in; aan het eind een trapje en een drijvend looppad naar het vlot.
- `/guhs vadsrijk z4wereld plek arena_zeeslurf`  (2185 64 -565) Het vlot in de lagune = de arena van de Grauwe Zeeslurf (lampionnetjes langs de rand).
- `/guhs vadsrijk z4wereld plek grotje_1_uitgang`  (2171 63 -565) Boven grotje 1. Duik naar beneden: elk grotje heeft een schacht met een BELLENKOLOM omhoog (snel weer boven + lucht).

## Opstijgpier (naar zone 5)

- `/guhs vadsrijk z4wereld plek opstijgpier_begin`  (2472 71 -565) Begin van de pier op het eiland.
- `/guhs vadsrijk z4wereld plek haven_4_5_landing`  (2517 73 -562) Het ronde platform: hier landt/vertrekt het luchtschip naar Hoogvads (lijn D). Vertrekbord, meermast met oortjes.
- `/guhs vadsrijk z4wereld plek opstijgpier_cliffhanger`  (2521 73 -565) Het eind van de pier (de cliffhanger-plek van het verhaal).

## Kampvuren (allemaal unlit = checkpoint; rechtsklik)

- `/guhs vadsrijk z4wereld plek kampvuur_bloemeneiland`  (2089 70 -401)
- `/guhs vadsrijk z4wereld plek kampvuur_knuffeltij`  (1949 67 -343)
- `/guhs vadsrijk z4wereld plek kampvuur_koraalbaai`  (2177 71 -615)
- `/guhs vadsrijk z4wereld plek kampvuur_lampionhaven`  (1839 67 -460)
- `/guhs vadsrijk z4wereld plek kampvuur_mysterie`  (2466 77 -324)
- `/guhs vadsrijk z4wereld plek kampvuur_pierkop`  (2466 71 -573)
- `/guhs vadsrijk z4wereld plek kampvuur_strandfeest`  (1989 68 -595)
- `/guhs vadsrijk z4wereld plek kampvuur_vliegereiland`  (2278 64 -641)
- `/guhs vadsrijk z4wereld plek kampvuur_vuurtoreneiland`  (2404 82 -486)

## Zeilbootsteigers (elk met een ladder uit het water)

- `/guhs vadsrijk z4wereld plek boot_dok_bloemeneiland`  (2028 65 -405)
- `/guhs vadsrijk z4wereld plek boot_dok_knuffeltij`  (2002 65 -345)
- `/guhs vadsrijk z4wereld plek boot_dok_koraalbaai`  (2114 65 -565)
- `/guhs vadsrijk z4wereld plek boot_dok_lampionhaven`  (1891 65 -464)
- `/guhs vadsrijk z4wereld plek boot_dok_mysterie`  (2441 65 -330)
- `/guhs vadsrijk z4wereld plek boot_dok_pierkop`  (2470 65 -530)
- `/guhs vadsrijk z4wereld plek boot_dok_schildpad`  (2258 64 -431)
- `/guhs vadsrijk z4wereld plek boot_dok_strandfeest`  (1995 65 -558)
- `/guhs vadsrijk z4wereld plek boot_dok_vliegereiland`  (2300 65 -621)
- `/guhs vadsrijk z4wereld plek boot_dok_vuurtoreneiland`  (2365 65 -500)
- `/guhs vadsrijk z4wereld plek boot_dok_wrak`  (2192 64 -700)

## Uitzichtpunten (panorama bij eerste bezoek)

- `/guhs vadsrijk z4wereld plek uitkijk_lampionduin`  (1860 90 -517)
- `/guhs vadsrijk z4wereld plek uitkijk_pierkop`  (2509 73 -559)
- `/guhs vadsrijk z4wereld plek uitkijk_vliegerheuvel`  (2301 91 -663)
- `/guhs vadsrijk z4wereld plek uitkijk_vuurtoren`  (2400 128 -495)

## Geheimen (een knuffel of een kist op de plek)

- `/guhs vadsrijk z4wereld plek geheim_knuffel_1`  (1916 63 -426)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_2`  (2201 48 -563)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_3`  (2350 70 -585)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_4`  (2488 81 -326)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_5`  (1790 69 -610)
- `/guhs vadsrijk z4wereld plek geheim_schat_1`  (2519 64 -564)
- `/guhs vadsrijk z4wereld plek geheim_schat_2`  (2240 64 -640)
- `/guhs vadsrijk z4wereld plek geheim_schat_3`  (2183 46 -579)
- `/guhs vadsrijk z4wereld plek geheim_schat_4`  (2425 63 -400)

## Randen, mist, gebieden

- `/guhs vadsrijk z4wereld stroming`: je staat in het water aan de zuidrand. **Wat moet er gebeuren:** je drijft langzaam terug naar het noorden
  (de Kringstroom) en ziet even "De Kringstroom duwt je zachtjes terug naar de eilandjes...". Zwem je harder naar buiten, dan kom je niet ver.
- `/guhs vadsrijk z4wereld mist`: het strand van het Mysterie-eiland. **Wat moet er gebeuren:** grijze mist, kort zicht, mistwolkjes;
  vaar/zwem weg naar het westen en de mist trekt zacht op.
- `/guhs vadsrijk z4wereld gebied`: zegt het gebied (z4_...), het kern-gebied, de mobzone, de biome, de stroming en de mist waar je staat.
  F3 laat de biome zien als "Het Vadsrijk - <eiland>" (alleen in een NIEUWE wereld/nieuwe chunks).
- `/guhs vadsrijk z4wereld eiland <naam>` (tab) / `/guhs vadsrijk z4wereld dok <naam>` / `/guhs vadsrijk z4wereld uitkijk <naam>`.

## Wat er (nog) NIET is

- Geen wezens/NPC's/quests (andere slices). De NPC-plekken staan klaar in POI_Z4.md (`gebouw.*.npc`, `npc_*`).
- De schildpad zwemt nog niet: het eiland ligt stil op het begin van de ronde (de route staat in `SCHILDPAD_ROUTE`).
- De vliegers zijn blokken (wol + touw), geen bewegende entities. Hoogvads (zone 5) hangt NIET in deze tegels.