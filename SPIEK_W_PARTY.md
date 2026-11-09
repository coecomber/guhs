# SPIEK W-PARTY: Party-tab + nep-spelers

Alles in het Nederlands bekijken. De dungeon-wachtrij werkt pas na de koppeling met w-grot (integratie); de raid-rij en alles
hieronder werkt los.

## 1. De Party-tab
- `H` (Vadsrijk-menu) -> tabblad **Party** (taart-icoon), of `/guhs vadsrijk party`.
  - Zonder party: links uitleg, rechts "Spelers in het Vadsrijk" met **Uitnodigen** (dichtbij eerst, met afstand).
  - Met een tweede account/speler: klik **Uitnodigen** -> bij de ander een **toast** "Party-uitnodiging" + belletje + in de chat
    **[Accepteer] [Weiger]** (klikbaar). In diens tab staat de uitnodiging ook met knoppen.
  - Na accepteren: links beide leden, kroon ♛ bij de leider, groene stip = online, regel "Lv · klasse · spec · zone".
  - Als leider: knoppen **Leider** en **Weg** per lid. Iedereen: **Party verlaten**.
- Commando's werken nog: `/guhs vadsrijk party uitnodigen <speler>`, `... accepteer`, `... weiger`, `... verlaat`,
  `... verwijder <naam>`, `... leider <naam>`.

## 2. Party bewaard na een herstart
- Maak een party met 2 spelers, sluit de wereld/server en start opnieuw: de party staat er nog (zelfde leider, zelfde volgorde).
- Uitloggen = niet meer uit de party: je frame wordt bij de ander grijs ("offline"). Logt de leider uit, dan wordt de ander leider.

## 3. Samen in de wachtrij
- Raid (leider, min. 2 spelers in het Vadsrijk): tab -> **Raid: party in de rij** (of `/guhs vadsrijk party raid`).
  Verwacht: "De party staat in de rij..." en de raid-melding "je staat in de rij (#1)". Is de toren vrij: binnen een minuut naar
  De Wachtzaal, dan gaan jullie samen naar boven.
- Dungeon (na koppeling): kies met < > een dungeon -> **Party in rij**; bovenin "In de rij voor: <dungeon>".

## 4. Nep-spelers
- Snel een paar naast je: `/guhs vadsrijk nepspeler spawn` (willekeurige rol) of `/guhs vadsrijk nepspeler spawn heler 3`.
  Kijk naar: speler-model met standaard-skin, naambordje `[NPC] xX_Vadsjesloper_Xx · Heler · Lv 12`, iets in de hand.
- In de wildernis vanzelf: loop een mobzone in (geen stad; bv. de Klodderweide naast Zwamzoet) en blijf een paar minuten bij mobs. Zeldzaam: max 2-3 per zone, ze vechten tegen de mobs daar en kletsen ("lfg dungeon??", "brb mama roept").
  Chat verschijnt boven hun hoofd en in de chat als `[Nep] naam: ...`.
- Rekruteren: rechtsklik er een -> "hoi" + **[Kom je mee?]** -> klik -> "gaat met je mee!" en hij staat in je party-frames en in de
  Party-tab met **[NPC]** en "nog 24 min". Of direct: `/guhs vadsrijk nepspeler rekruteer`.
  - Hij volgt je, valt aan wat jij raakt; een heler heelt je (hartjes), een tank trekt mobs naar zich toe (boos wolkje).
  - Laat hem bijna "doodgaan": hij zakt 10 s door de knieën ("rip") en staat weer op.
  - Jouw nep-speler rechtsklikken: resterende minuten + **[Doei!]**. Als leider kun je hem ook in de tab **Weg**-klikken.
  - Na 20-30 min: afscheidsregel ("moet eten, doei!") en weg.
  - Loot: als hij de laatste klap geeft krijg JIJ gewoon xp + loot; hij neemt nooit iets.
- Wachtrij vullen (na koppeling): ga alleen in een dungeon-rij en wacht 1 minuut: "Niemand anders kwam opdagen: 3 nep-speler(s)
  vullen de rij...". Direct: `/guhs vadsrijk nepspeler vul`. In de dungeon lopen ze met je mee; ga je eruit, dan zeggen ze "gg" en
  zijn weg. Nooit in de raid.
- Overzicht: `/guhs vadsrijk nepspeler lijst`; allemaal weg: `/guhs vadsrijk nepspeler weg`.

## 5. Aan/uit
- Per speler: Vadsrijk-menu > **Instellingen** > **Nep-spelers** uit. Verwacht: alle nep-spelers verdwijnen voor jou (niet meer te
  zien of aan te klikken), geen `[Nep]`-chat, je eigen metgezellen gaan naar huis, ze vullen jouw rij niet.
  (Zonder client-knop testen: `/guhs vadsrijk nepspeler mij uit` / `mij aan`, alleen server-kant.)
- Hele server: `/guhs vadsrijk nepspeler uit` -> iedereen gaat weg met een afscheidsregel, er komt niets meer bij;
  `/guhs vadsrijk nepspeler aan` zet ze terug. Staat ook in `serverconfig/guhs-vadsrijk-nepspelers.toml`.
