# Spiek 1.10.1 balans

Kort overzicht om te testen in de dev-client (in het Nederlands). Details: `NOTITIES_1101_BALANS.md`, xp: `XP_BALANS.md`.

## Wat is er anders

- **Geen loot uit hogere zones meer**: questbeloningen, kerkerkisten, grot-elites en schatten kiezen alleen uit de wezens-loot van
  hún zone. Geen benoemde baas/raid-items (die alleen van hun eigen baas), geen Maatwerk.
- **Mobs**: +30 % schade (x1,69 totaal), +40 % leven, en het leven groeit sneller per level (0,45 i.p.v. 0,30). Bazen niet.
- **Regen**: in gevecht (5 s na een klap) 0,3 % Vads per seconde, daarbuiten 1 % zoals vroeger.
- **Drops**: gewone wezens x0,2, elites minder (kans ~0,35 per rol, geluk 2,5 -> 1,6). Kerkerkist: 2 stukken.
- **Quest-xp**: zone 1 vol t/m hq07, daarna x0,6; zone 2 x0,6, zone 3 x0,8, zone 4 gelijk, zone 5 x0,65, zone 6 x0,7.
- **Kerker-xp**: een run = ~0,8 level (Zwamgrot ~1.400 i.p.v. 791).
- **Schade**: Kracht/Behendigheid/Wijsheid 1 % per punt (was 2 %), met een vaste x1,7 voor spelers. Bazen hebben daarvoor
  gecompenseerd leven: een baasgevecht duurt even lang als in 1.10.0.
- **Inhaalbonus**: een tweede/derde klasse krijgt +15 % klasse-xp per level achterstand op je hoogste klasse, max x3.

## Even proberen

1. Nieuw karakter, Klodderweide: mobs gaan iets langer mee (~2-3 s met skills), 3 tegelijk doet echt pijn.
2. Even niks raken: na 5 s vult je Vads weer sneller.
3. Karakter-tab (menu): een klasse die achterloopt toont "Inhaalbonus ×1,6" (groen), met uitleg bij de muis.
4. Zwamgrot uitspelen: 2 stukken uitrusting in de kist, alleen zone-1 spul.
5. Config: `/guhs vadsrijk balans toon` -> `[mobs] schade=1.69 leven=1.40 versie=2.00`.
6. Opruimen (op): `/guhs vadsrijk loot opruimen <naam>` = alleen lijst; met `--echt` weg. Geef jezelf eerst een
   `z5baas_moersleutel` om te zien dat hij gevonden wordt.

## Bij de uitrol (server)

1. Jar + pack. De mod zet `[mobs]` in `config/guhs-vadsrijk-balans.toml` zelf om (log: "migratie [mobs] versie 1 -> 2").
   Controle: `guhs vadsrijk balans toon`. Lukt dat niet: onder `[mobs]` zelf `schade = 1.69`, `leven = 1.4`, `versie = 2`.
2. `/guhs vadsrijk loot opruimen alle` (lijst nakijken), dan `/guhs vadsrijk loot opruimen alle --echt`.
   Offline spelers krijgen een back-up `playerdata/<uuid>.dat_voor_opruimen`.

## Bekend / open

- Zone 3, 5 en 6: alleen-hoofd zit boven de 10-30 min grind (72 / 39 / 64 min); met kerker-runs nog 13 / 28 / 18 min. Die zones
  zijn nog dicht; factoren niet aangepast.
- Opruimen bereikt geen marktaanbiedingen, bondbank of items op de grond.
