# NOTITIES_T_Z5VERHAAL — Vadsrijk zone 5 "Hoogvads", the story

Branch `110-t-z5verhaal`, worktree `guhs_vadsrijk/t-z5verhaal`, from dbf82899d + merged `110-r-int` (abb8a027c). Package
`feature/vadsrijk/zone5/verhaal`, generator `tools/features/vadsrijk_t_z5verhaal.py` (in FEATURES). Cheat sheet: `SPIEK_T_Z5VERHAAL.md`.

## What is built
- **Same quest system as zones 1-3** (feature/vadsrijk/npc): quests in `data/guhs/vadsrijk/npc/quests_z5.json` (`Quests.BESTAND_Z5`,
  read AFTER every other quest file, so zone 4's file can be added to the main array without conflict), texts under the shared
  keys `vadsrijk.npc.q|naam|praat|plek|gids|tip.z5_*`, own texts `vadsrijk.z5verhaal.*`. Every objective has an explicit `plek`
  and says WHO + WHERE + WHAT (generator asserts, gametest checks).
- **Main line "Verraad in de stad" (10 quests, `Z5Haken.HOOFD`)**: hq01 arrival (auto-start on flag `zone4_klaar`, arrival film
  from the Opstijgpier) → hq02 the stolen torn page, Inspecteur Snuffelaar, the lectern, interrogation Inktvlek → hq03 the mole in
  the Wolkwacht (Academie lockers, Kadet Plukje) → hq04 the Grote Soepkeuken (cook, 4 bowls of soup, Mager Mientje, flash **F3**
  as a narrated camera scene) → hq05 the Luchthaven night logbook + Raadsheer Gouddraad → hq06 Generaal Donsveer + Kanselier
  Zachtpels (the cheese test) → hq07 the **evidence board** + accusation → unmasking (Kootje Zilverstaart, a Mika of the
  Ringvinger) → hq08 **finale** in De Wolkwaldroom (spy fight + the Grauwe Droom; `Z5Finale`) and the queen reads the legend of
  the Lege Schaal (WERELDVERHAAL 4.1 "de onthulling van zone 5") → hq09 the lullaby at the eye, `zone5_klaar`, title card "Ridder
  van Hoogvads" → hq10 the Grauwe brief "Aan de Ringvinger" + **cliffhanger** toward zone 6 De Stormrand (storm wall, Grauwe Vloot,
  De Gouden Greep, Hertogin Ringeling's ten toe rings).
- **Detective work** (`Z5Speurwerk`, data `data/guhs/vadsrijk/z5verhaal/speurwerk.json`): 10 clues (per player map `z5_bewijs`),
  4 suspects (Gouddraad, Inktvlek, Donsveer = cleared by one clue each; Zachtpels = the traitor), 7 interrogations with dialogue
  choices (one question yields the clue + `meld("z5_verhoor", id)`). The board ("a simple screen") is the talking screen
  (`Praat.scene`): clue list, suspects (cleared marked), and during hq07 "Beschuldig X" buttons (shown once the 3 decisive clues
  staart/zegel/kaastest are found). Wrong accusation = explanation, no penalty. Opened at the inspector (role `z5_bewijsbord`) or by
  right-clicking the **Speurdersboekje** item (`vr_z5_npc_speurdersboekje`, a start item of hq02).
- **11 side quests**: soup berries, Pluisje's knuffel Flap, letters home, measuring the Oog (university), the whale's breath
  (3 blowholes), academy trials (Wolkenparcours, escort of the lost cadet), market parcels, watering the Staartpark, the queen's
  woollen crown, the song of the Eerste Knuffel (cathedral).
- **48 NPCs** (`Z5Npcs`): queen, inspector, havenmeester (travel, role `luchtschip_z5`), academy captain, professor, cook, the 4
  suspects, 6 refugees, cathedral sister, court lady, gardener, market (Lientje = veilinghuis, bakker = winkel, kleermaker, smid,
  juwelier), stalmeester, Ambachtsgilde (gildemeester + 5 leermeesters: roles `ambacht_gilde|koken|brouwen|smeden|naaien|imkeren`,
  "binnenkort" fallback unless the professions slice binds them), the **4th-class teasers** Vadszanger Lieflied + Pluisninja
  Schaduwdons (role `z5_vitrine`: "De Guhfluit / De Pluisdolkjes ... Binnenkort."), 6 citizens, 6 Wolkwacht guards.
- **Verkenningsboek** page `zone5` (title `vr_kenner_van_hoogvads`, 90000 brons): main, side, secrets (3 treasures + the Grauwe
  brief), 8 knuffels, bosses (Zilverstaart, Grauwe Droom), 4 lookouts (kern `Uitkijken.panorama`), 4 campfires, 5 areas.
- **Travel**: destination `hoogvads` on the travel map (marker top-right, route zone4→hoogvads), `QuestMotor.reisSlot/vliegNaar`,
  "hoogvads" appended at runtime to the other captains' REIZEN (shown only once `zone4_klaar` or zone 5 started), the havenmeester
  flies to zwamzoet/kandijkom/schemerwortel/knuffelkamp. Tracker hop (`QuestStap.ROUTES`): before the first arrival a Hoogvads
  target points to the Opstijgpier; later to the nearest captain / the havenmeester.
- **Gids pages** z5_hoogvads/speuren/soepkeuken/ambachten/reizen, tips z5_veilig/verhoor/bord, FTB sections (chapter guhs_vadsrijk),
  hidden advancements `vr_z5_*`.

## Hook for t-z5wezens (the finale) — `Z5Finale`
`Z5Finale.start(ServerPlayer)` is THE entry point (called when the player reaches the middle blowhole during hq08, and after the
spy phase). Two phases `Fase.SPION` (Kootje Zilverstaart, a fight) and `Fase.DROOM` (the Grauwe Droom of Moeder Wolkwal, the
part-2 deelbaas, cheered up). t-z5wezens plugs in with ANY of:
- `Z5Finale.registreer(Fase.SPION, p -> {...; return true;})` (+ `Fase.DROOM`);
- NpcRollen binding `"z5_finale"` with arg "spion"/"droom";
- static `zone5.wezens.Z5Bazen.start(ServerPlayer, String fase)` (reflection).
End of a phase: `Z5Finale.spionVerslagen(p)` / `Z5Finale.droomOpgevrolijkt(p)` (or `NpcApi.meld(p, "z5_zilverstaart",
"verslagen")` / `meld(p, "z5_droom", "opgevrolijkt")`); kills of `guhs:vr_z5_*zilverstaart*` / `*droom*` entities are caught too.
Suggested ids `guhs:vr_z5_baas_zilverstaart`, `guhs:vr_z5_baas_grauwe_droom` (used in the book). Arena = `Z5VerhaalPlekken.WOLKWALDROOM`
(placeholder 2600,200,-520), entrance `SPUITGAT`. Without any of this a fallback camera scene ends each phase (chain never blocks).

## For t-z4verhaal / t-z5wereld / t-luchtschip
- **t-z4verhaal**: set `Questlog.zetVlag(p, "zone4_klaar", true)` at zone 4's end (that name is MY GUESS, same pattern as
  zone2/3) and fly the player up with `Z5VerhaalApi.naarHoogvads(p)` (or action `z5_vlieg`). z5_hq01's `na` is `z3_hq15_hoogvads`
  (zone 4's last quest id was unknown): at integration change `VORIGE` in the generator to zone 4's last main quest.
- **t-z5wereld**: all spots are `w("<id>", x, y, z)` in `Z5VerhaalPlekken`; a generated `zone5.wereld.Z5WereldPlekken.plek(String)`
  is picked up by REFLECTION (no code change needed) when it uses the same ids: luchthaven_aankomst, luchthaven_station,
  zachtmoedlaan, markt_fontein, soepkeuken_ketel, raadszaal_lessenaar, troonzaal_troon, academie_oefenplein, universiteit_binnenhof,
  kathedraal_poort, kathedraal_altaar, staartpark_vijver, ooggaarde_tuin, balkon_oog, spuitgat_midden/noord/zuid,
  wolkwaldroom_arena, opstijgpier, raadszaal_zijkamer, luchthaven_havenmeester, academie_kapitein, academie_kazerne,
  academie_kastjes, academie_toren, academie_parcours_1..4, universiteit_bibliotheek/archief/sterrenwacht(_top), bank_balie,
  paleis_kanselarij(_bureau), paleis_hof, soepkeuken_tafel/hoek/bank/rij_1..4, markt_bakker/kleermaker/smid/juwelier,
  luchthaven_pier_west/oost, luchthaven_logboek, gilde_hal/keuken/brouwerij/smidse/atelier/bijenstal, burger_1..6, wolkwacht_1..6,
  kathedraal_apsis/roos_noord/roos_zuid/geveltoren, staartvin_top, staartpark_boom_1..4, kampvuur_luchthaven/markt/staartpark/
  ooggaarde, geheim_knuffel_1..8, geheim_schat_1..3, wolkbes_1..8, wolbol_1..3. Please place: 4 campfire blocks, the two
  vitrines (guhfluit / pluisdolkjes, "binnenkort" sign) next to the teasers, a lectern in the Raadszaal, lockers, a logbook, a
  telescope, and a walkable way into the middle blowhole.
- Placeholder numbers: renders-hoogvads/_bron (stad.py ZONES/NIV, wijken.py, markt2.py, kathedraal.py) through export.py
  `gx = 2702 - x, gz = -520 - z` (head west). The render agent is reworking details; district layout stays.
- **t-luchtschip**: binding `z5_luchtlijn` (arg "hoogvads") replaces our short flight after the first arrival.
- **T-WERELD**: our `Z5VerhaalPlekken.zone5(x,y,z)` copies `VadsrijkZones.Zone.Z5` (tile box + y >= 150); switch to it at integration.

## Shared-file changes (all marked `// T-Z5VERHAAL`, add-only)
| file | change |
|---|---|
| `VadsrijkFeature` | one line `zone5.verhaal.Z5VerhaalRegistry.register` |
| `npc/Quests` | `BESTAND_Z5` + `leesNa(...)` after the main loop (zone 5 loads after every zone, whatever zone 4 adds to the array) |
| `npc/QuestMotor` | role label `luchtschip_z5` (rol switch + dienst exclusion), `reisDoelen` filter for "hoogvads" (`Z5Verhaal.magZien`), `reisSlot` case "hoogvads", `vliegNaar` case "hoogvads" → action `z5_vlieg` |
| `npc/Reiskaart` | "hoogvads" in PLEKKEN, `hier()` zone 5, teaser until `magZien` |
| `npc/client/ReiskaartScherm` | marker position `hoogvads` {0.97, 0.05} + route zone4→hoogvads (no art yet) |
| `npc/client/QuestlogScherm` | 5 Gids ids `z5_*` appended |
| `tools/features/__init__.py` | `vadsrijk_t_z5verhaal` appended to FEATURES |
| lang nl_nl/en_us | ~760 keys (generator; zone 5 own keys only, never overwrites another slice's text) |

## Decisions made alone (open for the user)
- The spy: **Kanselier Zachtpels = Kootje Zilverstaart**, a Kootje of the Ringvinger (Hertogin Ringeling, zone 6); the stake: he
  sings the whale's lullaby (on the back of the torn page) backwards → the Grauwe Droom → the whale sinks → the city as a "lantern"
  for the Grauwe Vloot. The torn page front = the legend of the Lege Schaal (read aloud by the queen after the finale).
- Suspects and their alibis: Gouddraad (visits his mum in Lampionhaven), Inktvlek (spilled tea on another book), Donsveer
  (secretly practises a birthday song for the queen). All names (Snuffelaar, Windvaan, Veerkracht, Plukje, Brilsma, Pollepel,
  Mientje from WERELDVERHAAL F3, Pluisje, Grauwsnor, Zachtzang, Breinaald, Stekje, Lientje, Wolkbol, Zoompje, Aambeeld, Glimmertje,
  Hoefje, Dons, Duimdop, Peper, Bubbel, Vonk, Vingerhoedje, Zoemzoet, Lieflied, Schaduwdons).
- The board accusation needs the 3 decisive clues (staart, zegel, kaastest); the main line always gives all 10.
- After the unmasking the Zachtpels NPC stays (NPCs are shared by all players): for that player it is a "cardboard Chancellor".
- F3 (Mientje's memory) is a narrated camera scene with Darkness, not the playable memory (no empire map yet).
- The finale has a fallback scene so the story never blocks without t-z5wezens.
- Levels: hq01 lv 55 (arrival), the rest 56-60; side 56-59; quest xp = the z3 formula extended to lv 60.
- In-game text Dutch; en_us gets the same Dutch except ~10 cheap English titles.
- Campfires in a safe city are still checkpoints (the book lists them).

## Resume point / tests
- See the final report / `git log` on 110-t-z5verhaal. Gametests: `runGameTestServer -Pgt=Z5VerhaalGameTests` (elkeStapWieEnWaar,
  hoofdlijnViaApi, finaleHaak, luchtschipNaarHoogvads).
- Not done: real city geometry (t-z5wereld), real fights (t-z5wezens), the playable F3, a proper travel-map art spot for Hoogvads,
  zone 4's quest id/flag (guessed), job advancement at lv 60 (no class step defined for zone 5), wiki pages.
