# QUESTROBOT_RAPPORT

Automatisch gemaakt door `tools/vadsrijk/questrobot/rapport.py` uit de speelronde van de questrobot (gametest `VadsrijkQuestRobotGameTests` of `/guhs vadsrijk questrobot alles`), de statische controle en de staanplek-controle op de kaarttegels.

## Samenvatting (Nederlands)

| Zone | Quests | Gehaald | Vast | Niet gestart | Statische fouten | Staanplek-fouten |
|---|---|---|---|---|---|---|
| 1 Knuffelkamp / Zwamzoet | 41 | 41 | 0 | 0 | 0 | 0 |
| 2 De Splinterpieken | 19 | 19 | 0 | 0 | 0 | 0 |
| 3 De Schemerwortel | 24 | 24 | 0 | 0 | 0 | 0 |
| 4 De Lampionkust | 23 | 23 | 0 | 0 | 0 | 0 |
| 5 Hoogvads | 24 | 24 | 0 | 0 | 0 | 0 |

Zonevlaggen en luchtschip-poorten (gecontroleerd zodra het hoofdverhaal van een zone klaar was):

- zone 1 hoofdverhaal klaar: vlag zone1_klaar = true
-   luchtschip naar kandijkom (lv 20): open
- zone 2 hoofdverhaal klaar: vlag zone2_klaar = true
-   luchtschip naar schemerwortel (lv 42): open
- zone 3 hoofdverhaal klaar: vlag zone3_klaar = true
-   luchtschip naar lampionkust (lv 52): open
- zone 4 hoofdverhaal klaar: vlag zone4_klaar = true
-   luchtschip naar hoogvads (lv 57): open
- zone 5 hoofdverhaal klaar: vlag zone5_klaar = true

Legenda: **gehaald** = van aannemen tot inleveren door de echte quest-motor gespeeld; **vast** = de robot liep vast (en duwde de quest daarna met de hand door, zodat alles erachter ook getest werd); **niet gestart** = kwam nooit vrij.

Wat de robot NIET echt speelt (en via de haak van het spel meldt): gevechten (VERSLAGEN-haak), minigames/momenten (NpcApi.meld), lopen met een escorte, de luchtschipvluchten. De statische controle kijkt wel of er code bestaat die zo'n melding doet.

## Per zone (technisch)

### Zone 1: Knuffelkamp / Zwamzoet


(41 van de 41 quests zonder enige opmerking zijn hierboven weggelaten.)

### Zone 2: De Splinterpieken

- **z2_hq04_naalden** (hoofd): **gehaald**
  - staanplek [waarschuwing] plek `poi:z2_galmnaald` @ 886,146,-1014: staanplek (884, 145, -1017) is ingesloten (6 loopbare cellen in de buurt)
  - staanplek [waarschuwing] poi `z2_galmnaald` @ 886,146,-1014: staanplek (884, 145, -1017) is ingesloten (6 loopbare cellen in de buurt)

(19 van de 19 quests zonder enige opmerking zijn hierboven weggelaten.)

### Zone 3: De Schemerwortel


(24 van de 24 quests zonder enige opmerking zijn hierboven weggelaten.)

### Zone 4: De Lampionkust


(23 van de 23 quests zonder enige opmerking zijn hierboven weggelaten.)

### Zone 5: Hoogvads

- **z5_hq04_soep** (hoofd): **gehaald**
  - staanplek [waarschuwing] plek `npc:z5_kok` @ 2726,219,-534: staanplek (2723, 219, -537) is ingesloten (21 loopbare cellen in de buurt)

(24 van de 24 quests zonder enige opmerking zijn hierboven weggelaten.)

## Robot-notities (wat gesimuleerd is)

- `hq03_poppen`: oefenpoppen: geteld zonder echte poppen (geen wereld hier)
- `hq03b_elementen`: meld quiz:elementen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `hq05_klodderreus`: auto-hoofdquest startte niet vanzelf na de vorige quest; via de gever aangenomen; baas guhs:vr_klodderreus: 'verslagen'-haak afgevuurd (gevecht niet gespeeld)
- `hq07_littekens`: guhs:vr_npc_grauwe_pluis: 5 gevallen na 4 keer opvrolijken in gebied:klodderweide
- `hq09_druipgrotten`: level 8 -> 12 (levelpoort); escorte mijnwerker_mopje -> grotkamp: aankomst gesimuleerd (lopen niet gespeeld)
- `hq11_opperzwam`: level 13 -> 18 (levelpoort); meld zwamtempel:deur via NpcApi.meld (minigame/moment zelf niet gespeeld); baas gebied:zwamtempel: 'verslagen'-haak afgevuurd (gevecht niet gespeeld)
- `z01_wolletjes`: guhs:vr_npc_wolletje: 6 gevallen na 17 keer opvrolijken in gebied:klodderweide
- `z02_zwamtaart`: guhs:vr_npc_zwamhoedje: 5 gevallen na 7 keer opvrolijken in gebied:sporenwoud
- `z08_pluis`: guhs:vr_npc_grauwe_pluis: 8 gevallen na 32 keer opvrolijken in *
- `z09_kristal`: guhs:vr_npc_druppelkristal: 3 gevallen na 13 keer opvrolijken in gebied:druipgrotten
- `z10_rammel`: escorte ridder_rammel -> kampvuur_weide_1: aankomst gesimuleerd (lopen niet gespeeld)
- `z11_slijmgel`: guhs:vr_npc_slijmdruppel: 6 gevallen na 9 keer opvrolijken in gebied:klodderweide
- `z14_kistjes`: guhs:vr_npc_kistje: 4 gevallen na 18 keer opvrolijken in gebied:druipgrotten
- `z17_koning`: baas gebied:klodderweide: 'verslagen'-haak afgevuurd (gevecht niet gespeeld)
- `rijdier_hoedhupper`: guhs:vr_npc_wortelzwam: 5 gevallen na 12 keer opvrolijken in gebied:sporenwoud
- `rijdier_knuffelhert`: meld hert:open_plek via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `spec17_pluisjager`: level 1 -> 16 (levelpoort)
- `spec17_vadsmagier`: level 1 -> 16 (levelpoort)
- `geheim_spion`: meld spion:ontmaskerd via NpcApi.meld (minigame/moment zelf niet gespeeld); meld spion:gevangen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z2_hq04_naalden`: guhs:vr_z2_npc_galmscherf: 6 gevallen na 12 keer opvrolijken in gebied:z2_galmhellingen
- `z2_hq05_galmwolf`: baas gebied:z2_galmhellingen: 'verslagen'-haak afgevuurd (gevecht niet gespeeld)
- `z2_hq09_draak`: meld z2:draak_geholpen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z2_hq11_spiegelzucht`: baas gebied:z2_spiegelburcht: 'verslagen'-haak afgevuurd (gevecht niet gespeeld)
- `z2_hq12_hartsplinter`: auto-hoofdquest startte niet vanzelf na de vorige quest; via de gever aangenomen
- `z2_s03_knuffels`: teller z2_knuffels gevuld door de robot (10x verzamelen niet gespeeld)
- `z2_s13_ijsbloemen`: guhs:vr_z2_npc_ijsbloem: 5 gevallen na 16 keer opvrolijken in gebied:z2_knuffelmeer
- `z2_geheim_boormeester`: auto-quest: door de robot gestart (de echte start is een wereldmoment); teller z2_aanwijzingen gevuld door de robot (5x verzamelen niet gespeeld); meld z2_boormeester:verslagen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z3_hq01_boom`: meld z3:aangekomen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z3_hq03_pluimstal`: meld rijdier:starter via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3:opgestegen via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3:gezweefd via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z3_hq08_werf`: meld z3_taleggio:verslagen via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3_brokkel:verslagen via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3_roquefort:verslagen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z3_hq10_piepvel`: meld z3_lantaarn:ring via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3_piepvel:verslagen via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3:flash_f1: echt geteld via poi:z3_pelszaal
- `z3_hq11_wortel`: meld z3_ketel:een: echt geteld via poi:z3_ketel_1; meld z3_ketel:twee: echt geteld via poi:z3_ketel_2
- `z3_hq12_duister`: meld z3_grotlantaarn:aan via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z3_hq13_hart`: meld z3_wortelwurger:verslagen via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3_nestwachter:opgevrolijkt via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3_bleu:verslagen via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3:ei: echt geteld via poi:z3_nestkamer
- `z3_hq14_vahoegnir`: meld z3_vahoegnir:opgevrolijkt via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z3:finale: echt geteld via poi:z3_kruintop
- `z3_s01_kuikens`: teller z3_kuikens gevuld door de robot (6x verzamelen niet gespeeld)
- `z3_s02_opa_bast`: escorte z3_opa_bast -> z3_kruintop: aankomst gesimuleerd (lopen niet gespeeld)
- `z3_s03_thee`: guhs:vr_z3_npc_moerasbloem: 3 gevallen na 6 keer opvrolijken in gebied:z3_wortelvelden
- `z3_s04_lichtjes`: teller z3_lichtjes gevuld door de robot (4x verzamelen niet gespeeld)
- `z3_adv35_pluisjager`: level 17 -> 33 (levelpoort)
- `z3_adv35_vadsmagier`: level 17 -> 33 (levelpoort)
- `z3_geheim_imker`: teller z3_aanwijzingen gevuld door de robot (5x verzamelen niet gespeeld); meld z3_imker:verslagen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z4_hq01_lampionkust`: meld z4:aangekomen: echt geteld via npc:z3_kapitein
- `z4_hq02_knuffeltij`: meld z4:zeilboot: echt geteld via npc:z4_zeilverhuur; teller z4_tijknuffels gevuld door de robot (6x verzamelen niet gespeeld)
- `z4_hq05_vliegervanger`: meld z4_vliegervanger:verslagen: echt geteld via baas:z4_wrak; teller z4_wrakknuffels gevuld door de robot (2x verzamelen niet gespeeld)
- `z4_hq06_feest`: meld z4:feest: echt geteld via poi:z4_feestplek
- `z4_s01_vlieger`: teller z4_vliegerspul gevuld door de robot (4x verzamelen niet gespeeld)
- `z4_s02_wedstrijd`: meld z4:vliegerwedstrijd via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z4_s03_vissen`: meld z4_vis:gevangen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z4_s04_parels`: teller z4_parels gevuld door de robot (4x verzamelen niet gespeeld)
- `z4_s05_zeeslurf`: meld z4_zeeslurf:opgevrolijkt via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z4_s06_volleybal`: meld z4:volleybal via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z4_s07_limonade`: teller z4_schelpen gevuld door de robot (6x verzamelen niet gespeeld)
- `z4_s08_vuurtoren`: meld z4:lens via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z4_s09_bijen`: teller z4_bijtjes gevuld door de robot (5x verzamelen niet gespeeld)
- `z4_s10_kransen`: teller z4_bloemen gevuld door de robot (6x verzamelen niet gespeeld)
- `z4_s11_schildpad`: guhs:vr_z4_npc_zeewier: 5 gevallen na 17 keer opvrolijken in gebied:z4_lampionhaven|z4_knuffeltij|z4_strandfeest|z4_bloemeneiland|z4_koraalbaai|z4_vliegereiland|z4_vuurtoreneiland|z4_pierkop|z4_mysterie|z4_wrak|z4_schildpad|z4_zee
- `z4_s12_mistbrieven`: teller z4_mistbrieven gevuld door de robot (3x verzamelen niet gespeeld)
- `z4_s14_lampionnen`: teller z4_lampionhaken gevuld door de robot (5x verzamelen niet gespeeld)
- `z4_s15_strandavond`: meld z4:dans via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z5_hq01_stad`: auto-hoofdquest startte niet vanzelf na de vorige quest; via de gever aangenomen; meld z5:aangekomen via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z5_hq02_verraad`: meld z5:inspecteur: echt geteld via npc:z5_inspecteur; meld z5_verhoor:inktvlek via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z5_hq03_mol`: meld z5_verhoor:plukje via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z5_hq04_soep`: meld z5_verhoor:kok via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z5_soep:kom via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z5_verhoor:mientje via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z5:herinnering: echt geteld via npc:z5_mientje
- `z5_hq05_logboek`: meld z5_verhoor:gouddraad via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z5_hq06_verhoren`: meld z5_verhoor:donsveer via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z5_verhoor:zachtpels via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z5_hq07_bewijsbord`: meld z5_beschuldigd:juist via NpcApi.meld (minigame/moment zelf niet gespeeld); meld z5:ontmaskerd: echt geteld via poi:z5_raadszaal
- `z5_adv58_pluisjager`: level 35 -> 56 (levelpoort)
- `z5_adv58_vadsmagier`: level 35 -> 56 (levelpoort)
- `z5_s01_bessen`: teller z5_bessen gevuld door de robot (8x verzamelen niet gespeeld)
- `z5_s04_oog`: meld z5:oog_gemeten via NpcApi.meld (minigame/moment zelf niet gespeeld)
- `z5_s07_kadet`: escorte z5_kadet_dons -> z5_academie: aankomst gesimuleerd (lopen niet gespeeld)
- `z5_s10_wol`: teller z5_bollen gevuld door de robot (3x verzamelen niet gespeeld)

2 doelplekken liggen buiten de gegenereerde kaarttegels (niet op staanbaarheid gecontroleerd): poi:z4_kringstroom, z4_kringstroom
