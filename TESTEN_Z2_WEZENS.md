# Testen: zone 2 wezens (De Splinterpieken)

Alles met `/guhs vadsrijk z2wezens ...` (op-rechten), in het Vadsrijk (bijv. de proeftuin: `/guhs vadsrijk proeftuin`).

## Snel alles zien
- `/guhs vadsrijk z2wezens alle 25`: alle 14 monsters + 3 critters op een rij op een sneeuwplateau (zonder AI).
- `/guhs vadsrijk z2wezens opruimen`: alles weer weg.

## Per wezen proberen (`spawn <soort> [level] [elite]`)
| wezen | waar op letten |
|---|---|
| splinterling | elke klap → 3 scherven terug naar jou |
| galmvleermuis | wordt onzichtbaar, je hoort alleen de galm; hoog belletje = hij duikt achter je op |
| sneeuwknabbelaar | komt met een roedel (via spawner); huil, rode streep = sprong |
| kristalnaaldkruiper | ligt begraven (alleen naalden); kom binnen 5 blokken → rode cirkel → hij springt eruit |
| ijzelgeest | ijsblauwe cirkel onder je → bevroren grond → Bevroren (ijs-overlay, trager) |
| schotsbijter | onder het ijs; kraken + rode cirkel → bijt omhoog |
| versteende_wacht | traag; rode cirkel = zware slag + even duizelig |
| ijspuffer | blaast zich op → vrieswolk |
| mika_mijnwerker | onder 30 %: "Mamaaa!", rent weg, komt terug met vriendjes; verslagen = rookpluim |
| mika_boorder | rode streep = boorkop (Brandend) |
| kloofgier | cirkelt hoog; rode cirkel = duik; daarna even uitgeput op de grond |
| kristalspin | web → Plakkerig + plakplek |
| spiegelwacht | van voren kaatst hij pijlen/spreuken terug; van opzij raken |
| scherfgeest | flitst om je heen, dan 3 scherven |

- Kristalwezens spatten uit elkaar in grijze scherven die wegsmelten; Mika's vluchten in een rookpluim; beesten worden sneeuwstof. Geen roze plof.
- Versteende wezens: Roze doet x1,5 (tip verschijnt de eerste keer).
- `affix <soort> <gloeiend|snel|splitsend|schildend> [level]`: een elite.
- `bevroren [ticks]`: jezelf bevriezen (een zware klap breekt het ijs: "IJs gebroken!").

## Loot
- `loot <soort> [level] [elite]`, `set kristal|mika [level]`, `sieraden [level]`.
- Setregels staan in de tooltip (Kristalset / Mika-buitset, 2 en 4 stukken).
- Ruwe kristallen vallen alleen als z2-systemen de items `vr_z2_kristal_*` heeft.

## Spawners
- `spawners`: aantal spawners en max levend per gebied (~70-85). Ze staan pas goed als de zone-2-kaart er is (plekken zijn nog gegokt).
