# NOTITIES_POC — Guhs 1.10.0 "Het Vadsrijk", de kern-slice

Werkboom: `guhs_vadsrijk/w-poc`, branch `110-vadsrijk-poc` (vanaf v1.9.0). Contract voor alle slices: `CONTRACT_VADSRIJK.md`.

## Hervatpunt
1. Contract + kern-API: commit `1b061fabf` (de lead brancht de slices hiervan af).
2. Daarna: HUD (`kern/client/RijkHud`), scènespeler (`kern/client/SceneSpeler`), toetsen (de andere Guhs-toetsen op dezelfde toets worden in de dimensie ingeslikt), gametests (`kern/VadsrijkKernGameTests`, `-Pgt=VadsrijkKern`), TESTEN_VADSRIJK.md.
3. Autocheck (`tools/autocheck/vadsrijk_kern.txt`) gedraaid, 8 screenshots in `run/screenshots/autocheck/vr_*.png`:
   - wat werkt: de dimensie met de proeftuin, de HUD, lootstralen + tip, naamplaatjes, de baas-intro (camera + titelkaart), de baasbalk en het respawn-bord;
   - gevonden en opgelost: een flat generator met alleen lucht slaat de biome-features over (void), dus er staat nu 1 laag bedrock;
   - de tipkaart staat nu linksboven (rechtsboven zit de minimap).
4. Kern-opvolging (na het rapport):
   - **Arena-respawntimers worden bewaard** in `ArenaData` (`data/guhs/vadsrijk_arenas.dat`, absolute speltijd per arena). Een herstart haalt een net verslagen baas dus niet meteen terug. Partijen worden nog steeds niet bewaard.
   - **Curios/Accessories:** soft compat via reflectie (`kern/compat/ExtraVakken`), zonder harde dependency.
     - Curios gaat via `CuriosApi.getCuriosInventory(..).getEquippedCurios()` (getSlots/getStackInSlot/setStackInSlot).
     - Accessories gaat via `AccessoriesCapability.get(..).getContainers()` (de accessories- en cosmetic-containers).
     - Die vakken gaan mee in dezelfde wissel: tag met `vr_extra` + index, terug naar hetzelfde vak, anders de inventaris, anders `gevonden`.
     - Als de API er niet uitziet zoals verwacht: de vakken worden NIET gewisseld (gear blijft aan) en er komt een waarschuwing in de serverlog. Ze worden dan niet geblokkeerd, want blokkeren kan zonder API ook niet.
     - Getest met een nepbron (gametest). Met de echte mods is het NIET getest (ze zitten niet in de dev-run). Eerst op de ATM-testserver proberen.
   - **Naamplaatje** wordt nu op de client gebouwd via NeoForge's `RenderNameTagEvent.CanRender` (binnen 32 blokken), uit gesyncte waarden (Vads, max, level, element, elite, plof).
     - Een custom name of nametag vervangt alleen het naamdeel.
     - De server zet geen custom name meer.
   - **De 3 gele ringetjes** waren de verzadiging-overlay van **AppleSkin** (dev-only `localRuntime`, maar ook in ATM).
     - Verzadiging staat in de dimensie op 0.
     - Alle lagen in de namespace `appleskin` worden binnen verborgen.
   - **Baasmuziek-hook:** sound event `guhs:vr.kern.muziek.baas` is geregistreerd, zonder asset en zonder sounds.json-entry.
     - `VadsrijkBaas.muziek()` geeft de id; de baasbalk stuurt hem mee.
     - De client loopt hem (en pauzeert de gewone muziek) alleen als de sound bestaat.
     - Asset toevoegen = sounds.json-entry + ogg; meer is niet nodig.

## Technische keuzes
- **MC 26.1.2 / NeoForge 26.1.2.112** (niet 1.21.1): `Identifier`, `ValueInput/ValueOutput`, `GuiGraphicsExtractor`, render states.
- **Spelerdata = attachment** `guhs:vr_speler` (`SpelerData`, `copyOnDeath`). Die wordt in player.dat opgeslagen, samen met de echte inventaris. Daardoor is de wissel atomair: na een crash heb je óf de oude, óf de nieuwe toestand, nooit half.
- **Inventariswissel:**
  - Alles (36 vakjes + harnas + offhand + body + zadel) gaat als ruwe NBT-tags weg. Een item dat niet meer te lezen is (bijv. een verwijderde mod) blijft als tag bewaard.
  - Cursor en 2x2-craftgrid eerst terug in de inventaris.
  - Een niet-lege kist wordt nooit overschreven: de inhoud gaat naar `gevonden`, en `/guhs vadsrijk kist` geeft die terug in de juiste wereld.
  - De **reconciler** (elke dimensiewissel, login, respawn en elke seconde) vergelijkt de vlag `binnen` met de echte dimensie. Daardoor werken ook `/tp`, `/execute in` en portals.
- **Eigen HP:** Vads in `SpelerData`. Vanilla-gezondheid en -honger blijven vol, alle vanilla-schade aan spelers in de dimensie wordt afgevangen (een val wordt WAAR-schade, de void of `/kill` = flauwvallen). Vanilla-sterven in de dimensie wordt geannuleerd en wordt flauwvallen.
- **Mobs:** `VadsrijkMob` met eigen Vads (gesynced). Een linkerklik gaat via `hurtServer` naar `Schade.basisaanval`. "Stil" (verdoofd, plof, intro) zet de goal-flags uit, maar de fysica loopt door (NoAI zou ze in de lucht bevriezen).
- **Naamplaatje + HP-balk** = custom name (letter van het element, naam, Nv, balk van ▌-tekens). Dat is goedkoop en werkt met elke renderer. Later eventueel een echte balk.
- **Schadegetallen / respawn-bord** = korte TextDisplays (tag `guhs_vr_getal`). Ze worden niet bewaard: bij het laden uit een chunk worden ze weggegooid.
- **Kaart:** tegels van 64x320x64 (`structure/vadsrijk/tegels/t_<tx>_<tz>.nbt`) die per chunk geplaatst worden door de feature `guhs:vr_kaart`. Zonder tegel is het void, behalve de **proeftuin** (x/z 3000-3192), die in code wordt gestempeld (`kern/wereld/Ondergrond`).
- **Baas-intro** = eigen niet-klik-camera (`RijkScene`: keyframes in wereldcoördinaten + titelkaart), los van de verhaal-cutscene-engine, want die is ankergebonden.
- **Toetsen R/V/G/Z:** standaard bezet door de maag (G) en snuffel (R/V/Z). In de dimensie slikt `KernClient` (ClientTickEvent.Pre) de klikken van die Guhs-toetsen in.
- **Lootstralen** = gekleurde stofdeeltjes boven de ItemEntity, elke 5 ticks (de hoogte hangt af van de zeldzaamheid). Guhddelijk krijgt end-rod- en kersenbloesemglitter plus een eigen geluid.

## Ontwerpkeuzes (POC-getallen)
- **Ridder, level 1:** 100 Vads, Kracht 10, Verdediging 8.
  - Groei per level: +14 Vads, +2 Kracht, +1,5 Verdediging.
  - Energie: 100, +6 per seconde.
- **Vaardigheden:**

  | toets | naam | effect | energie | afkoeltijd |
  |---|---|---|---|---|
  | R | Kussenklap | 1,6x | 20 | 3 s |
  | V | Stormsprong | 1,3x | 25 | 8 s |
  | G | Knuffelschild | 20 % binnen, 50 % terug, 3 s | 30 | 15 s |
  | Z | Vahoege Plof | 3x, 3 s verdoofd, 15 % heal | - | 10 s, laadt op door schade |

- **Schade:** basis x (1 + 2 % per hoofdstat) x kritiek (1,5 + Kritiekschade%) x element (x1,5 / x0,7, + Elementschade%).
  - Verdediging: x100/(100 + 2 x verd).
  - Elementweerstand: maximaal 75 %.
- **Xp-curve:** `40 + 25 x lv^1,6` per level, maximaal level 20.
- **Klodderreus (lv 3):** 700 Vads, +75 % per extra speler in de arena.
  - Fasen: springen, dan bij 50 % 4 kloddertjes (later +2 per 12 s, maximaal 6), dan bij 20 % rollen.
  - Respawn na 60 s.
  - Buitkist voor iedereen met meer dan 5 % bijdrage (gemiddelde van de aandelen schade, heal en tanken).

## Risico's voor de volledige bouw
- **Curios/Accessories:** soft compat bestaat (zie hierboven), maar is niet getest met de echte mods.
- **Andere mods die items geven of afpakken** tijdens de wissel (bijv. graven, backpacks) kunnen botsen. Graven zou niet mogen voorkomen, want doodgaan wordt geannuleerd.
- **HUD:** de vanilla-lagen worden alleen in de dimensie verborgen. HUD-mods (AppleSkin, JourneyMap, minimap) tekenen eigen lagen en kunnen overlappen met de skillslots rechts van de hotbar.
- **Partijen** worden niet bewaard (een herstart betekent: geen party).
