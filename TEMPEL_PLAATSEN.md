# De vaste ordetempel plaatsen (release 1.10.0)

De enige weg naar Het Vadsrijk is de ingestorte ordetempel in de Guhmensie. De worldgen-tempels komen alleen in NIEUW land,
en de Guhmensie op de server is grotendeels verkend. Daarom zetten we er na de release één vaste tempel neer, in de
bestaande wereld, zonder iets van spelers kapot te maken. **Geen world reset, geen regeneratie.**

Alles hieronder pas NA de rollout van 1.10.0 (de nieuwe jar moet draaien), en elke stap alleen met OK van de gebruiker.
Commando's via rcon op de server (`mc ...`), als console = op.

## 1. Plekken zoeken

```
mc "guhs vadsrijk tempel zoek 192"
```

- Zoekt rond het midden van de Guhmensie (0, 0); vanaf de console is er geen speler, dus altijd rond 0,0.
  Wil je rond een andere plek zoeken: ga daar als op in de Guhmensie staan en typ het in-game (`/guhs vadsrijk tempel zoek 192`).
- Straal 32..512 (standaard 192). Groter = trager (alleen bestaand land wordt geladen, er wordt nooit nieuw land gemaakt).
- Je krijgt maximaal 5 plekken: `x y z  score  (hoogteverschil)` + het plaats-commando om te kopiëren.
- Een plek is alleen goed als het hele vak (63x63 + rand) alleen natuurlijke blokken heeft (geen planken, glas, wol,
  kisten, bedden, bordjes, Guhs-bouwblokken of andere bouwsels), vlak is (max 5 blokken verschil), droog is (geen water/lava),
  niet in een beschermd gebied ligt (beschermde bouwwerken, Protected-gebieden) en er geen Guhhuisje vlakbij staat.
  (Erfenis-plots liggen in hun eigen dimensie; FTB Chunks draait niet op de server, dus er zijn geen claims.)
- Niks gevonden? Grotere straal proberen.

## 2. Een plek kiezen

- Kijk de kandidaten na op https://map.guhs.nl (kaart `guhmension`): ligt hij mooi, bereikbaar, niet midden in iemands uitzicht?
- Hoogste score = vlakst en dichtst bij het midden.

## 3. Plaatsen

```
mc "guhs vadsrijk tempel plaats <x> <y> <z>"
mc "guhs vadsrijk tempel plaats <x> <y> <z> 90"      # optioneel gedraaid: 0, 90, 180 of 270
mc "guhs vadsrijk tempel info"                       # controle: dimensie, plek, portaal
```

- x/z = het midden van de tempel, y = de grond (de y die `zoek` gaf). De checks worden opnieuw gedaan; faalt er één, dan
  wordt er NIETS geplaatst en zegt hij waarom.
- Er wordt eerst een backup van alle blokken gemaakt: `world/data/vr_vaste_tempel_backup_<tijd>.nbt`, plus een logregel
  `[Vadsrijk] vaste ordetempel geplaatst ...` in `logs/latest.log`.
- Staat er al een vaste tempel, dan weigert hij. Verplaatsen: `... plaats <x> <y> <z> [draai] --opnieuw` (zet eerst de oude terug).
- Terugdraaien: `mc "guhs vadsrijk tempel ongedaan"` (zet de backup terug, Werver Wimpel weg, registratie weg).
- Het portaal slaapt, net als bij een worldgen-tempel. Werver Wimpel verschijnt zodra er een speler in de buurt komt;
  eerst met hem praten, dan wordt het portaal wakker.

## 4. Nakijken op map.guhs.nl

- BlueMap werkt regio's bij met een vertraging (update-cooldown 15 min). Sneller: `mc "bluemap update guhmension"`.
- Zoek de coördinaten op https://map.guhs.nl en kijk of de tempel er netjes staat (geen half afgehakte bomen, geen gaten).
- Niet goed? `mc "guhs vadsrijk tempel ongedaan"` en een andere plek kiezen.

## 5. Discord

- Zet de coördinaten in de Discord-post (`guhs_vadsrijk\DISCORD_1.10.0.md`), bijvoorbeeld:
  "De oude ordetempel staat in de Guhmensie bij **x … , z …**. Of pak je superkompas: tabblad *Avontuur* > *Het Vadsrijk*."
- Het Superkompas (tab Avontuur, "Het Vadsrijk") wijst naar de vaste tempel; zonder vaste tempel naar de dichtstbijzijnde
  worldgen-tempel in bestaand land.
