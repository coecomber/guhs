# NOTITIES_POC — Guhs 1.10.0 "Het Vadsrijk", de kern-slice

Werkboom: `guhs_vadsrijk/w-poc`, branch `110-vadsrijk-poc` (vanaf v1.9.0). Contract voor alle slices: `CONTRACT_VADSRIJK.md`.

## Hervatpunt
1. Contract + kern-API: commit `1b061fabf` (de lead brancht de slices hiervan af).
2. Daarna: HUD (`kern/client/RijkHud`), scènespeler (`kern/client/SceneSpeler`), toetsen (de andere Guhs-toetsen op dezelfde toets worden in de dimensie ingeslikt), gametests (`kern/VadsrijkKernGameTests`, `-Pgt=VadsrijkKern`), TESTEN_VADSRIJK.md.
3. Nog open (zie onderaan): een autocheck-ronde en de opvolgpunten.

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
- **Curios/Accessories-slots** (in ATM) zitten NIET in de wissel: die gear blijft gewoon aan. Daarvoor is een compat-laag nodig, of die slots moeten in de dimensie geblokkeerd worden.
- **Andere mods die items geven of afpakken** tijdens de wissel (bijv. graven, backpacks) kunnen botsen. Graven zou niet mogen voorkomen, want doodgaan wordt geannuleerd.
- **HUD:** de vanilla-lagen worden alleen in de dimensie verborgen. HUD-mods (AppleSkin, JourneyMap, minimap) tekenen eigen lagen en kunnen overlappen met de skillslots rechts van de hotbar.
- **De naamplaatjes** gebruiken de custom name. Een mod die mobs hernoemt (of een nametag) overschrijft ze.
- **Partijen** worden niet bewaard (een herstart betekent: geen party).
- **De arena-status** (respawn-tijd) wordt niet bewaard. Na een herstart spawnt de baas meteen.
