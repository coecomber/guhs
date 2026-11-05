# T-DIALOOG: better dialogues (roadmap §5.4), the Vadsrijk talking screen

Branch 110-t-dialoog, from dbf82899d (r-int, incl. the q-dialoog portrait fix).

## What I built
- **The flag:** `feature/vadsrijk/dialoog/RijkDialoog` (server side) hooks into `verhaal.Praat` through a new `Praat.Versierder`.
  - A conversation gets `Rijk:true` when the reader is in the Vadsrijk dimension, or when its sleutel starts with `vr_`.
  - The client then opens `dialoog/client/RijkPraatScherm` (a subclass of `PraatScherm`) instead of the plain screen.
  - Everything outside the Vadsrijk (Knuffeldal, Erfenis, the stories, the Knusfeest list) keeps the old screen byte-for-byte.
- **Per page it also gets:**
  - `Factie`: the name plate and frame colour. Values: z1-z5, orde, mika, draak, speler, verteller, wezen.
  - `Plek`: the lang key of the NPC's home area (Gebieden).
  - `Stem` and `Toon`: the blip voice and its pitch. Stable per NPC, via a hash.
  - `Emotie`: explicit only. It comes from `RijkDialoog.emotie(key, ...)`, quest data `"npc:blij"`, or the rule "a hand-in's first line `.klaar.0` = blij". Without one, the client guesses the mood from the visible text (`Emotie.raad`).
- **"Wat moet ik doen?":** at a Vadsrijk NPC guh (the conversation's first speaker) `RijkDialoog.doel` adds `Doel {Soort, Args[titel, wat, waar], Extra?}`. It takes the first match of:
  - (1) **actief**: a quest this NPC gives, takes in or is part of (talk/escort target). It shows `QuestStap.van`.
  - (2) **aanbod**: the quest this NPC offers now. It shows the first step via the new `QuestStap.eerste`.
  - (3) **hoofd**: the NPC is who you last handed a main quest in to. It shows the main story's next step.
- **What the client does with `Doel`:**
  - It shows a strip above the answers: "Opdracht:/Nu:/Verhaal: wat, waar". Hovering shows the full text.
  - It adds a compass button that shows a page where the NPC says it as a sentence.
  - It is all client-side: no new answer ids, so QuestMotor's answer handling is untouched.
- **RijkPraatScherm:**
  - typewriter text (speed 0-3 in config, click/space = all, again = next page, auto-scroll);
  - blip per word per voice: guh = note bit, mika = xylophone, verteller = harp, speler = pling, draak = bass, with ~70 ms throttling;
  - name plate in the faction colour + a label (the faction or the home area);
  - mood: happy hops + ♥, sad sinks/looks down + …, surprised jump/pop + !, angry shakes + ╬. It moves the PIP box; no textures needed;
  - page dots, `« Terug`/backspace, shift+space to the choices (the choices are never skipped);
  - the log (L / ≡): this session, max 80 lines, scrollable;
  - answers with item icons + a colour stripe + the number key 1-9;
  - the gear: speed button + volume slider (config `guhs-vadsrijk-dialoog.toml`);
  - the narrator gets a full-width parchment page in italics with a book;
  - your own lines show your portrait on the right;
  - GUI scale safety:
    - the panel width is min(330, width-8);
    - the portrait is 54 below 240 px of height;
    - more than 2 answers that don't fit go into 2 columns;
    - the balloon shrinks and scrolls;
    - the height stays the same over all pages.
- **The Mika-imker** (q-dialoog's open point) gets a `guhs:mika` portrait through `Praat.portretBron`.
- **Dev:** `/guhs vadsrijk dialoog demo|vol|oud`. F3+H shows `factie/stem/emotie(!=explicit,~=guessed)`.

## Decisions I made myself
- **A Vadsrijk-only subclass, not a change to the shared screen.** This is the roadmap's own recommendation. It means zero risk for Knuffeldal/Erfenis.
- **The flag is set by dimension or a `vr_` sleutel,** not per caller. All zone 1/2/3 callers work without changes.
- **Emotions:** a mood sign + portrait motion instead of face-texture swaps. Textures per mood × guh variant would be expensive; this works for every entity (Mika, player, creatures).
- **Voices:** vanilla note-block sounds, not new OGGs. The volume slider has a default of 60%.
- **The faction colours** are my choice: zone 1 pink, zone 2 ice blue, zone 3 honey, zone 4 lantern orange, zone 5 gold, Knuffelorde purple, Mika grey, dragon rose-red, you mint, narrator parchment.
- **The "Wat moet ik doen?" text is built client-side** from server components, so no new answer ids and no QuestMotor changes beyond the parser.
- **The log only lives for the session.** It is not in the questlog (J) yet.

## Shared-file changes (all marked `T-DIALOOG`, add-only/minimal)
- `verhaal/Praat.java`:
  - adds the `Versierder` interface + `versierder()`;
  - calls `versier(p, sleutel, firstSpeaker, data)` in `open()` and `scene()` before sending (try/catch, never blocks).
- `knuffeldal/client/PraatScherm.java`:
  - visibility private → protected for `npcId`, `data`, `pagina`, `klaarGemeld`, `paginas()`, `laatstePagina()`, `nu()`, `tekst()`, `send()`, `STAND_INS`, `portret()`;
  - new `stijl` factory + `maak()` + `past()`;
  - new `schermAchtergrond()`.
  - The plain screen's behaviour is unchanged.
- `knuffeldal/client/KnuffeldalClient.open`:
  - `PraatScherm.maak(...)` instead of `new PraatScherm(...)`;
  - in-place update only when `scherm.past(data)` (same style).
- `vadsrijk/npc/QuestStap.java`: + `public static eerste(p, q)`.
- `vadsrijk/npc/QuestMotor.regels`: a speaker `"npc:blij"` → strips `:blij` and registers the mood for that line's key.
- `vadsrijk/VadsrijkFeature.register` + `vadsrijk/client/VadsrijkClient.init`: one line each.
- `tools/features/__init__.py`: + `vadsrijk_t_dialoog`.
- `tools/features/vadsrijk_t_dialoog.py`: new, with 51 texts nl+en. The lang JSONs were updated with its `_direct()`.

## Tests
- Gametests `VadsrijkDialoogGameTests` (`-Pgt=VadsrijkDialoog`):
  - moods;
  - factions/voices/option kinds;
  - the Rijk flag (vr_ yes, other no, `oud` no);
  - `"npc:blij"`;
  - "Wat moet ik doen?" through a real `QuestMotor.klik` on the sergeant (offer → Doel aanbod; after accepting → actief).
- See the report/resume point below for the results.

## Resume point / open
- See the final report in the commit log. Open:
  - the last conversation per NPC in the questlog (J) (roadmap);
  - small choices with consequences;
  - context greetings (night, pet, zone done);
  - explicit moods in the existing quest texts (content pass: add `:blij` etc. in the quest generators where it fits);
  - a client screenshot check at GUI 2/3/4 by the user (no AutoCheck run by me if RAM was short, see report).
