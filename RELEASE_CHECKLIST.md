# RELEASE_CHECKLIST: Guhs 1.10.0 "Het Vadsrijk" (release met alleen Knuffelkamp + zone 1)

Bron: `110-y-int` (worktree `guhs_vadsrijk\y-int`; ronde Y bovenop ronde X). Niets hieronder is gedaan. **Elke stap wacht op jouw OK** ("needs the
user's OK"); doe ze in deze volgorde. De schakelaars **moeten hetzelfde getal hebben**: de zone-poort van de mod
(`openZones`) en `--open-zones` van de wiki. Bij deze release: **1**.
- `GUHS_OPEN_ZONES` (welke FTB-quests de wiki laat zien, `tools/wiki_site/gamedata.py`) volgt sinds ronde Y vanzelf
  `--open-zones`: `make_wiki_site.py` zet hem. Zet hem dus niet los; staat hij in de omgeving op iets anders, dan meldt de
  build `!! GUHS_OPEN_ZONES=... differs` en gebruikt hij `--open-zones`. Het FTB-boek in het spel zelf volgt `openZones`
  (de verborgen "Zone N is open!"-quests).

Stand bij het schrijven: main = v1.9.7 (ab7a99f5a). 110-y-int heeft v1.9.7 al in zich (gemerged in ronde X), dus main kan
fast-forward naar de release-commit. De versie in `gradle.properties` staat nog op 1.9.7.

---

## 0. Vooraf (lokaal, geen OK nodig om te lezen; wel voor de commits op een release-tak)
- [ ] Je hebt de testclient gespeeld en gezegd dat het goed is (`y-int\run-client.bat`, zie SPIEKBRIEF hoofdstuk 1 en X2).
- [ ] Release-tak maken vanaf 110-y-int (bijv. `release-1.10.0` in een eigen worktree, NIET in y-int/x-int/w-int):
  - `gradle.properties`: `mod_version=1.10.0`;
  - `CHANGELOG.md`: blok 1.10.0 (zonder spoilers van zone 2-6);
  - de werkbestanden eruit zoals bij 1.9.0 (NOTITIES_*, SPIEK_*, HERVAT_*, EINDRAPPORT_*, AUDIT_*, BEREIK_RAPPORT, POI_*.md
    blijven of gaan: jouw keuze; bij 1.9.0 gingen de NOTITIES eruit);
  - `docs/patchnotes/1.10.0.md`: `concept: false`, `datum: 2026-11-xx` (zone 2-6-regels staan al in `:::zone N`-blokken);
    ronde Y erbij (zonder spoilers): FTB-hoofdstuk "Het Vadsrijk" met pronkstukken, de Vadsrijk-vitrine, Vadsrijk-kleding thuis
    als looks (Garderobe), "claimen alleen in je eigen wereld";
  - FTB Essentials is in `build.gradle` alleen een dev-afhankelijkheid (`localRuntime`, niet in de jar): niets aan doen.
- [ ] `gradlew build` + een korte gametest-ronde op de release-tak (alleen in die worktree).
- [ ] Wiki-lekcheck lokaal (u-site, zie `guhs_vadsrijk\u-site\RELEASE_SITE.md` §0.4): 0 lekken, 0 kapotte links.

## 1. Backup van de server — **needs the user's OK**
```bash
ssh root@2.28.142.15
STAMP=$(date +%Y%m%d-%H%M)
# (zoals bij 1.9.x: een tar.zst van de wereld + config naast de dagelijkse backups)
tar --zstd -cf /opt/guhs/backups/voor-1.10.0-$STAMP.tar.zst -C /opt/guhs/server26 world config serverconfig defaultconfigs 2>/dev/null
ls -la /opt/guhs/backups/ | tail -3
```
Nooit de wereld wissen of opnieuw genereren (jouw vaste regel).

## 2. Merge naar main + tag — **needs the user's OK**
```bash
git -C <repo> checkout main && git merge --ff-only release-1.10.0   # (of --no-ff als main intussen bewoog)
git tag -a v1.10.0 -m "Guhs 1.10.0: Het Vadsrijk"
git push origin main && git push origin v1.10.0
```
- Daarna op GitHub: de workflows **Release** (bouwt de jar als asset), **Build** en **Pages** groen.
- Pages bouwt nu nog zonder /vadsrijk/ (dat komt in stap 6).

## 3. De jar — **needs the user's OK**
- Controleer het release-asset `guhs-1.10.0.jar` (downloaden, sha256 noteren, grootte ~ als de dev-jar, ~+12 MB kaartpak).
- Snelle rooktest: dedicated server lokaal starten met alleen deze jar + dependencies (geen crash bij start, "Done").

## 4. Pack (packwiz) — **needs the user's OK**
In `MineScapeStory\guhs-server\guhs-pack-repo`:
```bash
../packwiz.exe url add guhs https://github.com/coecomber/guhs/releases/download/v1.10.0/guhs-1.10.0.jar --force
# pack.toml: version = "1.10.0"
../packwiz.exe refresh
git commit -am "Guhs 1.10.0" && git push
```
- Controleer op https://pack.guhs.nl/pack.toml de nieuwe versie en dat de hash in index.toml klopt.

## 5. Server deploy met openZones=1 — **needs the user's OK**
Vóór de herstart (de mod leest beide bestanden bij de start):
```bash
# a) de zone-poort op 1. RONDE Y-VONDST: NeoForge 26 leest server-configs uit <server>/config/ (de wereld-map serverconfig/
#    mag hem overschrijven); defaultconfigs/ doet NIETS meer. Dus config/ is de echte plek, serverconfig/ voor de zekerheid.
for d in /opt/guhs/server26/config /opt/guhs/server26/world/serverconfig; do
  mkdir -p $d && printf 'openZones = 1\n' > $d/guhs-vadsrijk-zones.toml && chown minecraft:minecraft $d/guhs-vadsrijk-zones.toml
done
# b) live-data fase B (LIVE_DATA_PLAN.md): config/guhs-vadsrijk-web.properties met aan=true, map=/var/www/guhs-live
#    (alleen als fase A, stap 7, al staat; anders aan=false laten)
# c) uitrollen zoals bij 1.9.x (1 minuut waarschuwing, herstart, pack wordt bij de start opgehaald)
guhs-rollout-when-live 1.10.0
```
Na de start:
```bash
journalctl -u guhs -n 50 | grep -i "Done\|error\|exception"
mc "guhs vadsrijk zones"        # moet zeggen: zones 1-1 open; zone 2-6 binnenkort; level-plafond 20
```
- Ronde Y, in-game als gewone speler (geen op):
  - in het Vadsrijk: `/ec`, `/home`, `/anvil` geven "werkt niet in het Vadsrijk"; een FTB-beloning claimen geeft
    "Beloningen claim je in je eigen wereld"; thuis lukt claimen wel;
  - het FTB-hoofdstuk "Het Vadsrijk" toont alleen rekruut, zone 1 en geheimpjes 1;
  - een vitrine zet je thuis neer, niet in het Vadsrijk.
- Kijk in-game (gewone speler, geen op): de luchtschipper op De Haven zegt na zone 1 "Het schip naar De Splinterpieken vaart
  binnenkort!", lijn A neemt niemand mee, de grens duwt terug.
- Ops/creative mogen overal in; test de spelersblik met `/guhs vadsrijk zones alsspeler aan` (en weer `uit`).

## 6. De wiki met `--open-zones 1` — **needs the user's OK**
- Sinds 2026-11-09 staat er op main al een vooraf gebouwde `docs/vadsrijk-site/` (u-site NOTITIES_SITE_LIVE.md). Bij de merge
  van de 1.10-tak: die map weg en `make_wiki_site.py`/`landing.py` van de 110-tak nemen (de echte `--vadsrijk`-build).
Volg `guhs_vadsrijk\u-site\RELEASE_SITE.md` §1 (na server + pack live):
```bash
git apply RELEASE_SITE_pages.patch     # .github/workflows/pages.yml: --vadsrijk --live-url https://live.guhs.nl/ --open-zones 1
git commit -m "pages: publish guhs.nl/vadsrijk (open zones: 1)" && git push
```
- Controleer (alles 200, zone2 404, geen noindex, sitemap heeft /vadsrijk/):
  `curl -s -o /dev/null -w "%{http_code}\n" https://guhs.nl/vadsrijk/` en de andere regels uit RELEASE_SITE.md §1.3.

## 7. live.guhs.nl — **needs the user's OK** (SSH)
- **Fase A** (`u-site\LIVE_DATA_PLAN.md`): de DNS-records zijn er al (A/AAAA `live`, grijze wolk). Nog te doen op de server:
  - A2: map `/var/www/guhs-live` + lege dataset;
  - A3: Caddy-blok voor `live.guhs.nl` toevoegen (eerst `cp /etc/caddy/Caddyfile /etc/caddy/Caddyfile.voor-live-<stamp>`),
    `caddy validate`, `systemctl reload caddy`;
  - A4: `curl -s https://live.guhs.nl/status.json` geeft de lege dataset (`nog_niet_live`).
- **Fase B** (bij stap 5b): `aan=true` in de web-config; na de start verse tijdstempels in `/var/www/guhs-live` en
  `"online":true` in status.json.
- Terugdraaien: zie LIVE_DATA_PLAN.md (Caddyfile terug, map weg; fase B: `aan=false`).

## 8. De previews weghalen — **needs the user's OK**
- Op gh-pages in één commit: `vadsrijk-preview/`, `vadsrijk-preview-zone1/` en `.github/workflows/vadsrijk-preview.yml` weg.
- Daarna "Wiki (GitHub Pages)" op main één keer handmatig draaien (workflow_dispatch).

## 9. Discord — **needs the user's OK** (en vraag eerst welke screenshots)
- Pas posten als de server én het pack live zijn (jouw vaste volgorde).
- Concept (geen spoilers: geen verhaal-afloop, geen zone 2-6-namen, alleen plagen):
  ```
  Guhs 1.10.0: Het Vadsrijk!
  Er ligt een heel nieuw rijk achter de horizon. Stap in het Knuffelkamp, kies je klasse en vlieg naar De Haven:
  zwammen, klodders, een grot met een échte dungeondeur en een Grootmeester die meer weet dan hij zegt.
  - Klassen, levels en uitrusting die je zelf smeedt
  - Dungeons met wachtrij (en een Party-tab voor je vriendjes)
  - Zwamzoet, geheimpjes, knuffels en een wiki op guhs.nl/vadsrijk
  Het schip naar het volgende eiland vaart... binnenkort. Houd Discord in de gaten!
  Start je Guhs-instance: het pack werkt zichzelf bij.
  ```
- Afbeeldingen: max 4; kies uit de AutoCheck-shots in `x-int\run\screenshots\autocheck\xi_*.png` (alleen zone 1/kamp),
  `y-int\run\screenshots\autocheck\yi_*.png` (vitrine, Garderobe, FTB-boek bij poort 1) of maak nieuwe. **Vraag de gebruiker welke.**
- `python tools/patchnote_discord.py 1.10.0` (standaard `--open-zones 1`) print de zone-1-tekst; `--post` stuurt hem pas na OK.

## Later: zone 2 openen — **needs the user's OK**
- Server: `mc "guhs vadsrijk zones open 2"` (meteen actief, bewaard). Spaarpot betaalt uit, plafond 30.
- Wiki: in pages.yml `--open-zones 1` -> `2` op beide plekken (`GUHS_OPEN_ZONES` volgt vanzelf: de FTB-quests van zone 2
  verschijnen dan ook op de wiki, net als in het boek); eerst lokaal `zone1check.py --release --open-zones 2`
  ("Spiegelzaal" vrijgeven of hernoemen, zie RELEASE_SITE.md).
- Discord: `patchnote_discord.py <versie> --open-zones 2`.

## Terugdraaien
- Server: stop, wereld uit `/opt/guhs/backups/voor-1.10.0-<stamp>.tar.zst` terug, pack terug naar de vorige commit, start.
- Wiki: de pages.yml-commit terugdraaien en pushen.
- live.guhs.nl: zie LIVE_DATA_PLAN.md.
