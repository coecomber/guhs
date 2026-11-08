# NOTITIES W-BALANS (ronde W, 2026-11-08)

Branch `110-w-balans` vanaf 0f00119f2. Vraag van de gebruiker: haalt de Lampionkust-questreeks (zone 4) genoeg xp om 56 te
halen zonder terug te moeten naar zone 3?

## Wat ik deed
1. **Analyse-tool** `tools/vadsrijk/xpbalans.py` (nieuw, eigen bestand). Leest de quest-JSON's, rekent de Java-formules na
   (Levels-curve, mob-xp per zone, bazen, kerker-run) en simuleert per zone drie spelers (A hoofd, B hoofd+zij+bazen,
   C B+kerker) in twee varianten: **keten** (binnen op het eigen level uit de vorige zone) en **los** (binnen op precies het
   poortlevel). Telt grind-minuten voor levelpoorten onderweg en tot het doel. Opties `--kpm`, `--onderweg`, `--zone N`,
   `--schaal z:hoofd:zij` (wat-als zonder iets te wijzigen).
2. **Rapport** `XP_BALANS.md` met tabellen per zone, voor/na en het directe antwoord voor de Lampionkust.
3. **Fix (alleen quest-xp, alleen generators + hun quest-JSON):**
   - zone 4 (`tools/features/vadsrijk_t_z4verhaal.py`): hoofdstap 45 % → **65 %** van een level, zijquest 18 % → **22 %**
     (constanten `Z4_HOOFD`, `Z4_ZIJ`). `quests_z4.json` opnieuw gegenereerd.
   - zone 6 (`tools/features/vadsrijk_u_z6verhaal.py`): hoofdstap 45 % → **80 %**, zijquest 18 % → **22 %**
     (`Z6_HOOFD`, `Z6_ZIJ`). `quests_z6.json` opnieuw gegenereerd.
   - Alleen het `xp`-veld van de beloningen veranderde (diff gecontroleerd). De generators schreven ook nl_nl/en_us,
     maar daar zat alleen drift van andere slices in (geen xp): **teruggezet**, niet gecommit.

## Besluiten
- **Curve en cap (70) niet aangeraakt.** Niet nodig: de gaten zaten in de quest-xp van zone 4 en 6.
- **Mob-xp niet aangeraakt.** Zone-4/6-mobs geven al 1/55 level per kill zoals z3/z5; de grind-minuten worden klein genoeg
  met alleen de quest-aanpassing. Zo blijft het tempo tussen zones gelijk.
- **Zone 1, 2, 3, 5 niet aangepast.** Zone 1: alleen-hoofd grind (~70-90 min) maar de zijquests (tutorial, premies) staan
  direct open; met zij geen grind. Zone 2: veel te ruim (geen gat). Zone 3: los vanaf 30 grote grind, maar in de keten
  komt iedereen op 41-48 binnen door zone 2, dus geen grind. Zone 5: binnen op 56 geen grind.
- Doel zone 4 = **56** (wat de gebruiker vraagt en wat `z5_hq02` vraagt); `z5_hq01` vraagt maar 55.
- Kerkercode (w-grot) niet aangeraakt; alleen gerekend met de huidige run-samenstelling (3+4 mobs + elite + grotbaas).

## Shared-file wijzigingen
- `tools/features/vadsrijk_t_z4verhaal.py` (blok "quest xp", gemarkeerd W-BALANS)
- `tools/features/vadsrijk_u_z6verhaal.py` (blok "quest xp", gemarkeerd W-BALANS)
- `src/main/resources/data/guhs/vadsrijk/npc/quests_z4.json`, `quests_z6.json` (gegenereerd; alleen xp-waarden)
- Integratie: als een andere slice deze generators ook draait, komen mijn xp-waarden vanzelf mee (ze staan in de generator).

## Tests
- Gametests `-Pgt=Z4VerhaalGameTests,Z6VerhaalGameTests,VadsrijkQuestRobotGameTests`: zie eindrapport/log `../w-balans-gt.log`.
- De questrobot kijkt niet naar xp-bedragen (alleen of quests te doen zijn); levelpoorten (`lv`) zijn ongewijzigd.

## Open punten (voor de gebruiker)
- **Zone 2 geeft veel te veel xp**: alleen hoofdquests brengen je van 20 naar ~41 (band 20-30); vaste beloningen 3.000-15.000
  (z2_hq11 = ~3 levels). Daardoor loopt iedereen in zone 3 voor. Niet aangepast (geen gat, en zone 2 is al gespeeld);
  wil je het rechttrekken, dan moet zone 3 tegelijk omhoog (zone 3 los vanaf 30 mist ~175 min aan xp).
- **Zone 1 alleen-hoofd** vraagt ~70-90 min grinden (lage hoofd-xp, poorten 6/12/16/18). Met zijquests geen probleem.
- **Overshoot na de fix**: hoofd+zij-spelers zitten na zone 4 op ~58-60 en na zone 5 op de cap; geen probleem voor de
  voortgang, wel voor het gevoel van "op level" in zone 5/6. Dat komt vooral door zone 2.
- Kerkers: **geen zone mist er een** (Zoutgrot z6 heeft geen poort). Een solo-run is ~0,3 level waard.
- Bazenlevels in de tool zijn schattingen; kill-tempo 2/min is een aanname (`--kpm` om te variëren).
