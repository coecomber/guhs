# NOTITIES_FIX_QUESTS — fix round 1 (2026-11-04), branch `110-fix-quests`

User feedback: "Kok Knapperkorst said 'talk to someone', and later neither he nor the questlog said who." Plus the Erfenis keys
(O/M took the user's keys) and, as extra scope, a proper element-triangle lesson in Knuffelkamp.

## What changed

### Quest tracker + light beam (npc slice)
- `QuestStap` (new): the current step of a quest or of the whole main story, always **what + who + where** + a spot to point
  at (+ an optional extra line such as a recommended level). Rules:
  - quest not taken: "Praat met <giver>" + where the giver stands; level too low: "Word eerst level N (nu M)" + "Praat daarna
    met <giver>, <where>" + where to level up (area by level band);
  - active: the first open objective (with progress n/m) and its place: NPC (live position when loaded), POI, area (campfire
    or entrance), campfire, boss arena, training field, the class temple, Akim's stall...; all done: "Lever in bij <NPC>";
  - an escort that is not following yet: "Haal <NPC> op: <where>"; following: the destination;
  - target on the other side of the sea (camp island vs. mainland, after hq05): "...Eerst met het luchtschip: praat met
    <Kapitein Wolkewiet / Stuurguh Zeppie>, <where>", pointing at the pilot.
- Place texts: generator tables `PLEK_NPC`, `PLEK_POI`, `PLEK_GEBIED`, `PLEK_KAMPVUUR`, `PLEK_BAAS` (keys
  `vadsrijk.npc.plek.*`), written from the real map (POI_VADSRIJK.md v1). The generator asserts every NPC and POI has one.
- `NpcVolg` (new): every second each player in the Vadsrijk gets `NpcPayloads.Volg` (main step + the pinned quest's step),
  only when it changed. Choices in `extra("npc")`: `volg_pin`, `volg_uit`, `straal_uit` (payload `VolgKeuze`).
- `client/VolgHud` (new): the panel top right (★ title, ▸ what, where, extra line, an arrow + "N blokken[, M hoger/lager]");
  below the boss bar while one shows (`RijkHud.bovenVrij()`); hidden in scenes (RijkHud cancels layers during scenes) and with
  F1. The light beam: a soft pink column (pinned side quest: pale yellow) over the target, drawn in AfterTranslucentBlocks
  with debugQuads, wider with distance, pulled closer than 480 blocks so it stays inside the far plane, fades out within 5-17
  blocks of the target. Toggles: questlog buttons "Volger: aan/uit", "Straal: aan/uit" and two key mappings
  (`key.guhs.vr_volger`, `key.guhs.vr_straal`) that are **unbound by default** (no player key gets taken).
- Questlog (J): the main story is always on top (also when its next quest is not taken yet: a "Volgende" row), the detail shows
  "Nu: <what>" + where in gold, then objectives, the quest text and "Eerder in het verhaal:" (finished main quests, newest
  first). Side quests get a "Volgen" / "Niet volgen" button (pin into the tracker). Screen 360x240 (was 340x220).
- Hand-in of a main quest: a gold chat line "Volgende stap: <what>, <where>" (not after hq04/hq12: the BAM/cliffhanger follow).
- Re-talking to whoever you last handed a main quest in to repeats the next step ("Weet je nog? ..."), and "how is it going"
  pages of an active quest end with the step too.
- Dialogue texts: every hand-off page names person and place (hq03, hq05, hq06, hq07, hq08, hq09, hq10, hq11).

### Quest flow fixes (soft-locks / ambiguity)
- hq11 -> hq12: hq12 was offered by Steeltje while the text said "bring it to the Grootmeester" (so going straight there did
  nothing). Now hq12 starts by itself at the hq11 hand-in (`actie_klaar start:hq12_schaduw`, `auto`).
- The Mika pin was a collect objective with no drop source: losing it soft-locked hq12. Now it is hq12's start item and the
  objective is only "Breng het Mika-speldje naar Grootmeester Vadsvoet".
- hq09 -> hq10: Mopje walks back deep into the caves 5 minutes after the escort, so the player had to go and find him again at
  lv 16. Now hq10 starts at the hq09 hand-in; the tracker shows "Aanrader: level 16 (jij bent level N)".
- Auto main quests whose start was missed (log out during the BAM before hq05 started) are offered by their giver (gold "!"),
  so nothing can get stuck.
- "start:" actions now run at hand-in time (before, after the dialogue closed), so the tracker/chat line are right at once.
- Accepting a quest with "go to area X" while standing in X counts at once (kern only reports entering).
- Trainer spots: Meester Kussensloop and Juf Pijltje stood inside a training dummy's fence arm (training -5/+5, -4 = the dummy
  row z 977); moved to z -1 (between the dummy rows).
- Dev: `/guhs vadsrijk npc quest af <id>` hands a quest in at once (rewards, runs "start:" actions, not BAM/cliffhanger).

### Element triangle (extra scope)
- New main quest **hq03b_elementen** "De elementendriehoek" by **Opa Sterrenstof** (trainer_magier, on the training field):
  between hq03 (dummies) and hq04 (campfire/BAM). Four pages: the icons (flame = Vuur, mushroom = Zwam, drop = Slijm, heart =
  Roze), the triangle `VUUR > ZWAM > SLIJM > VUUR` with a reason for each, "Zwak!" x1.5 / x0.7, then a quiz: one of three
  random questions with three answers (Vuur/Zwam/Slijm). Wrong = a friendly hint + another question; right = straight to the
  hand-in + the elementen tip card. Hand-in sends you back to Kok Knapperkorst (named + place).
- Questlog Gids page "Elementen": the triangle is drawn (three coloured nodes, dotted arrows "wint van", the x1,5/x0,7 line).
- Not done (owned by others): a real hands-on "hit three coloured dummies with an element and see Zwak!" needs element dummies
  on the training field (wereld) and element damage on dummies (gevecht). The quiz is the stand-in.

### Tips (kern RijkHud, small touch)
- The tip card stays top left (the tracker owns top right), moves below the boss bar while one shows, is 190 wide, stays
  10 s + 1.5 s per extra line, and shows a thin bar that runs out. `RijkHud.bovenVrij()` is public for the tracker.

### Erfenis keys (Guhs 1.9)
- Overview F9 (was O), world map F10 (was M), identical to the 1.9.2 hotfix on main: `ErfenisClient`/`KaartClient` defaults,
  lang `erfenis.welkom.*`, `tools/features/erfenis.py`, `tools/features/guhs190_wiki.py`, TESTEN_ERFENIS.md, TESTEN_OPKNAPPER.md.
  Separate commit f4aab0a46.

## For the integrator
- FTB: `vr_hq03b_elementen` is in the Knuffelkamp section of FTB_SECTIES.
- Map (static check of the tiles, `bereik.py`/`pad.py` in my scratchpad): see the "Map check" section below.
- The questlog/HUD use `RijkHud.bovenVrij()` (kern); if kern's HUD moves the boss bar, keep that value right.
- Minimap: the dev client runs JourneyMap (localRuntime) with its minimap top right; the AutoCheck showed the tracker under it.
  Fixed: when journeymap/xaerominimap is loaded the tracker sits left of the minimap (x minus 31% of the GUI height, the JM
  default size). If the pack uses another minimap size/corner, adjust `VolgHud.MINIMAP` / that offset.
- The gevecht agent may add element dummies; then hq03b could get a "hit the right dummy" objective (MELD "element:raak").

## Map check (static, on the committed tiles of 6ec8724ca; for the integrator / the map agents)
A rough walker over the .nbt tiles (step up 1 with headroom, drop up to 3, doors/gates passable, lifts/ladders climbed;
committed as `tools/vadsrijk/loopcheck_doelen.py` + `loopcheck_pad.py`, run from the project root with `.`). Every main-quest NPC/POI has a
floor and open space around it, except what is listed here. **Two likely survival blockers (verify in the client: the walker is rough and the tiles in git may differ from a fresh regen):**
1. **South jetty -> plateau (Knuffelkamp)**: the 3-wide spruce stair from the jetty (0,65,1126) ends at (0,72,1119), at the
   foot of the cliff; the cliff above rises in 2-5 block steps to the plateau (84-87). No stair, ladder or lift. A new player
   arrives on this jetty (kern `Gebieden.AANKOMST` (0,65,1130)) and Sergeant Pluizebol stands there: in survival you cannot
   walk up to the camp. (Going DOWN works: the cliff can be dropped.) Fix: continue the stair up the cliff to the plateau
   (~z 1118 -> 1105, y 72 -> 87), or move AANKOMST + the sergeant to the plateau.
2. **Druipgrotten**: the cave network around Mijnwerker Mopje (`npc_mopje` (-400,41,-380), knots N1-N4, x -468..-251,
   z -478..-290, y 39-52) is NOT walkable up to any of the 3 entrances (`grot_ingangen.1/2/3`) or the grotkamp: the way in has
   drops of 2+ blocks you cannot climb back up (a pit you cannot leave; walkability standard). hq09 sends the player down to
   Mopje and back up to the grotkamp. The escort itself is safe (Mopje teleports to you when far behind), the player is not.
   Fix: stairs/slopes or a lift on the tunnels from the entrances down to the knots. (Water columns were not modelled: if the
   tunnels have a swimmable water way up, verify in the client.)
Checked OK: camp plateau <-> tempeltje / kapitein / training / pond door; haven -> Zwamzoet plein; plein -> lift foot; lift
top -> Grootmeester (through the side door `ordetempel_zijdeur`); plein -> Boswachter Mos -> woud scar; plein -> Klodderweide
campfire -> weide scar; Sporenwoud -> grotkamp -> entrance 1; plein -> fossil bridge -> temple door; voorhal -> Opperzwam door.
Fixed here: the two trainers stood inside a dummy's fence arm.
Not modelled: the pink door gates / temple puzzles (bazen) and moving doors.

## Tests
- `VadsrijkNpcGameTests.hoofdlijnHeeftAltijdWieEnWaar`: walks all 13 main quests through the API; at every step (offer, level
  gate, each objective, hand-in) the tracker step has translated what + where, a spot, the NPC by name for talk/hand-in, the
  right marker; auto starts work; 13 steps of history.
- AutoCheck script: `tools/autocheck/vadsrijk_quests.txt`.

## AutoCheck (one run, `tools/autocheck/vadsrijk_quests.txt`, screenshots in run/screenshots/autocheck)
- OK: the tracker shows title / ▸ what / where / arrow + distance for hq01, hq02, hq03b, hq06; the gold "Volgende stap" chat
  lines after each hand-in; the first-time tip card top left does not overlap the tracker; Opa Sterrenstof stands on the field.
- Found + fixed after the run (not re-run, one run allowed): (1) the JourneyMap minimap covered the right half of the tracker
  -> tracker left of the minimap; (2) the kapitein's dock (x 149) lies outside the knuffelkamp area box, so the airship hint
  doubled ("praat met Kapitein ..., eerst met het luchtschip: praat met Kapitein ...") -> island = z > 800.
- Not verified visually: the light beam (all shots were within 6 blocks of the target or the camera was inside the cliff) and
  the questlog screen (the runner closed the screen before the shot). Check both in the dev client.

## Resume point
Head = the last commit on `110-fix-quests`. Everything compiles; gametests `-Pgt=VadsrijkNpcGameTests` 5/5. Open: the beam
and questlog visuals in the client; the two map blockers above (map agents); element dummies (gevecht/wereld).
