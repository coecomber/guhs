# NOTITIES U-Z6WERELD - zone 6 "Het Zijdeland": de kaart

Ronde U (2026-11-06). Worktree `guhs_vadsrijk/u-z6wereld`, branch `110-u-z6wereld` (van eaee06d08 = t-int), met 110-u-overgang en
de laatste 110-u-hekken erin gemerged (verbind-pas + naadpas).

## Wat er gebouwd is

### De kaartgenerator (`tools/vadsrijk/zone6/`)
- `kit/` = KOPIE van de conceptgenerators (`renders-zijdeland/_bron`); aanpassingen gemarkeerd `[zone 6 kaart]`:
  - `hoogkit.py`: jars van buiten (renders-hoogvads/_bron/jars) + de eigen zone-5/6-blokken van de repo in een zip PER PROCES
    (meerdere renders/builds tegelijk schreven anders dezelfde zip kapot); renders naar `renders-zijdeland/ronde2`;
  - `zpalet.py`: hv_-conceptnamen -> de echte zone-5-blokken; de nieuwe Zijdeland-blokken als sleutels (zs, zs_beitel, mozaiek_*,
    mashrabiya, koepel_tegel, zijde, luifel_zijde, messing_lantaarn, palm_*); `palet.py` = de zone-5-spelpaletversie;
  - `zijwereld.py`: de buitenkust: naar de noord-/zuid-/oostrand van de tegels zakt het land in de gedeelde open zee (strand op zand,
    kliffen op rots); zone 7 vervangt de oostrand later;
  - `zijbouw.py`: `plek()` / `huis_rol()` registreren spelplekken tijdens het bouwen; `zijfiguren.py`: elk rijdier-figuur wordt ook
    vastgelegd (`W.rijdieren`) voor het spel.
  - `rozenkloof.py` (+ `petra.py`) en `saffraanpoort.py`: de twee steden met diepte (zie onder, gebouwd door twee forks).
- `plan6.py`: tegelvak (tx 51..66, tz -18..-7), venster, gebieden (biome, levels, stad), ALLE geplande plekken incl. de ids van
  u-z6verhaal / u-z6wezens / u-z6raid (+ ALIAS-tabel voor hun namen van dezelfde plek).
- `bouw6.py`: het spel-deel van het open land: beloopbare wegen (`pad`: profiel met max 1 per blok, traptreden op elke stijging,
  1:1-bermen, lantaarns op dadelpalmhekpaaltjes), de 5 sterrenbakens (DONKER), de toren (buitenkant + verzegelde poort), de kerker-
  ingang (koepelruïne met hal en trap naar de sterrenpoort), De Zingende Zoutgrot, kampen/kampvuren, Duinput, zoutziedershuisje,
  Mika-kamp, waterval-trap + watervallen + moerbeibomen, Moederoase (Moederpalm, vluchtelingenkamp), ruïnes + tabletten, uitkijkpunten,
  geheimen, karavanen op de wegen, rijdier-stops (alle 3 de rijdieren) bij kampen.
- `maak6.py`: alles in ÉÉN venster (x 3264..4288, z -1152..-384, y 0..260; ~1 GB), dan 192 tegels als plakjes (geen draaiing),
  hol gemaakt. `--alleen-plekken` = alleen de POI's (snel). Testschakelaar `GUHS_Z6_ZONDER_STEDEN=1`.
- `poi6.py`: snapt elke plek op echte blokken, schrijft `Z6WereldPlekken.java`, `POI_Z6.md`, `_cache/plekken_z6.json`.
- Checkers: `loopcheck6.py` (gedeelde loopcheck, y 30..260, zaden = luchthaven + Rozenplein + paleisplein), `poicheck6.py`,
  `audit6.py` (AUDIT_Z6.md + veilige_plekken_z6.json).
- Hook: `maak_kaart.py --zone 6` en `--zone alle` (eigen proces, na zone 5). `overgang.py` kent groep z6 (naadpas naar de open zee).

### Blokken (`tools/features/vadsrijk_u_z6blokken.py`, Java `zone6/wereld/Z6Blokken`) - 19 stuks
rozenzandsteen (+ trap, plaat, muur), gebeitelde rozenzandsteen (guh-motief), mozaïektegels ster/guh/bloem, mashrabiya-traliewerk
(pane, verbindt als ijzeren tralies), turkoois koepeltegels (+ trap), zijden doek, zijden luifel (zweeft, 1px, geen botsing),
messing lantaarn (hangend/staand, licht 15), dadelpalm stam/planken/trap/plaat/hek. Tab "Guhs: Bouwen", te maken (shapeless +
steenzaag), loot, tags (hek in #fences/#wooden_fences, muur in #walls), NL + EN. Gametest `Z6BlokkenGameTests` 4/4 groen.

### Java (`feature/vadsrijk/zone6/wereld/`)
- `Z6WereldPlekken` (gegenereerd; wel gecommit omdat de code ervan afhangt, integratie regenereert): ALLE, GEBIEDEN, HUIZEN,
  BEWONERS, RIJDIEREN, UITKIJKEN, KAMPVUREN, `inZone6`, `gebied`.
- `Z6Wereld`: kern-gebieden met levelbanden (Rozenkloof 60 stad, Duinen 60-62, Palmenkloven 62-64, Saffraanpoort 62 stad,
  Zoutvlakte 64-66, Ruïnedal 66-68, Moederoase 68-70, toren 70), `NA_GEBIEDEN`-haak voor andere zone-6-slices, `biome(x, z)`,
  dev-commando's `/guhs vadsrijk z6wereld plek|huis|gebied|bewoners`.
- `Z6Bewoners`: figuren + rijdieren als passieve props (Z3Figurant). Pluimguh-model bestaat; bultguh/tapijt registreren hun model
  in `Z6Bewoners.MODELLEN` zodra die slices er zijn (tot dan zie je alleen de ruiter).
- `Z6WereldGameTests` (batch vadsrijk_z6wereld): gevraagde ids, gebieden/biomen/levels, barrière + indeling.
- `Z6WereldRegistry`: blokken + `Z6Wereld.register()`.
- Data: `tools/features/vadsrijk_u_z6wereld.py` (9 biomen "Het Vadsrijk - <gebied>", gebiedsnamen, barrièreteksten).

## Gedeelde bestanden (add-only, gemarkeerd U-Z6WERELD)
- `wereld/WereldRegistry.java`: 1 regel `Z6WereldRegistry.register(modBus)`.
- `biomen/VadsrijkBiomen.java`: 9 ids achteraan BASIS; `index(x,y,z)` en `index(x,z)` vragen eerst `Z6Wereld.biome`.
- `indeling/VadsrijkZones.java`: `Z6_DX/DZ`, `Zone.Z6` (tegelvak), `LAND_Z6`, `inZone6`.
- `indeling/Barrieres.java`: Vrij "z6", `inZeegebied` tot x 4700, soorten `zijdewind` (zeestraat x 3060..3330: bloesem + zijdedraad-
  deeltjes) en `oostgrens` (x > 4150).
- `indeling/ZeeVulling.java`: `MAX_X` 3600 -> 4672 (de open zee rond zone 6).
- `tools/features/__init__.py`: 2 regels; `tools/vadsrijk/indeling.py` (TEGELS/LAND z6); `maak_kaart.py` (--zone 6); `overgang.py`
  (groep z6); lang nl/en + minecraft-tags via de generators.
- `guhs1100-avontuur/wereldindeling/WERELDINDELING.md`: zone 6 (vak, tegels, barrière Roze Kliffen + Zijdewind, §8).

## Zelf gekozen
- Tegels met een kustrand (tx 51..66, tz -18..-7): het land loopt aan noord/zuid/oost af in zee i.p.v. een afgesneden rand; de oostgrens
  is voorlopig een zachte terugduw ("De Oostroute houdt hier (nog) op").
- De bakens staan in de tegels DONKER; het verhaal vervangt de ster (`<baken>_ster`, r ~3.6).
- `zoutgrot` = de hal ONDER de grond (u-z6verhaal gebruikte het als oppervlakte-ingang: dat is `zoutgrot_ingang`).
- Geheimen: de namen + plekken van u-z6verhaal overgenomen (`geheim_knuffel_1..8`, `geheim_schat_1..3`).
- Raid-pocket (u-z6raid, x 4115..4174, z -713..-663, y 2..68): geen grot/water gebouwd in die doos.
