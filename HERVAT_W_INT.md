# HERVAT W-INT (gestopt 2026-11-08 ~22:47, de pc ging uit)
- **Samengevoegd op 110-w-int:** w-modellen, w-leven, w-spel, w-grot, w-balans, w-schepen en w-engels (fase 1 klaar, zie NOTITIES_W_INT.md), plus w-party (dd0bbf091).
- **W-party-koppeling gedaan (WIP-commit):**
  - party/DungeonRijBrug.java + `koppel()` in PartyRegistry;
  - NepVulling-bug gefixt: de rijen worden nu vers bekeken, anders spawnden de vullers nooit;
  - de party-tests zetten de echte koppeling terug (`herstel()`);
  - nieuwe test grotkerker/VadsrijkDungeonNepGameTests;
  - Engels voor de 122 party-sleutels in tools/lang/en/c416_vr_party.json (apply_en, engelscheck OK, --merk).
- **Bezig was:** de testrun `-Pgt=VadsrijkParty,VadsrijkUHud,VadsrijkZ6Raid,VadsrijkMenu,VadsrijkKern,VadsrijkDungeon,TaalsleutelsGameTests`. Die is afgebroken.
  - In de vorige run faalde alleen `nepVultRijEnStart`, op een te strenge rol-check. Die check is versoepeld: de eigen klasse van de speler telt mee.
  - **Eerst deze run herhalen.**
- **Daarna: FASE 2 (GO gekregen).** Zie de opdracht van de coördinator.
  1. Merge 110-w-bereik (5851f7a93). Houd bij de havens en pieren beide bedoelingen.
  2. Volledige kaart-regen van zone 1-6 (zone 4 VOOR zone 5), daarna BereikDoelenGameTests.
  3. Alle checkers: schepen_check 0 fouten, bereikcheck --strikt, kijkcheck5/6, enzovoort.
  4. De geheimpjes vindbaar maken.
  5. De outputs committen.
  6. De volledige gametest-suite en de questrobot.
  7. AutoCheck.
  8. De site opnieuw genereren (u-site, alleen de preview).
  9. run-client.bat.
  10. Een nieuwe SPIEKBRIEF, TESTEN_VADSRIJK en EINDRAPPORT_RONDE_W.
- **Goed om te weten:**
  - build/moddev/artifacts bevat de 1.21.1-jar-workaround voor make_v2/make_ftbquests.
  - Controleer na de herstart of guhs_vadsrijk/slots leeg is.
