# LIVE_DATA_PLAN: live.guhs.nl (the real live data for guhs.nl/vadsrijk)

Status 2026-11-08 (w-site): **NOT deployed.** The coordinator passed on the user's OK to set up the endpoint, but my
read-only SSH check (`ssh root@2.28.142.15 'cat /etc/caddy/Caddyfile ...'`) was refused by the session's permission system
("Production Reads"). The Cloudflare DNS needs the user's dashboard (or a token I do not have). I did not try to work around that.
The steps below are ready to copy-paste for whoever has access (the user, or a session that is allowed to SSH).

## How it fits together
```
Minecraft server (Guhs 1.10.0, WebExport aan=true)
   writes JSON atomically every 10-600 s  ->  /var/www/guhs-live/{status,wereld,posities}.json, hiscores/*.json, spelers/*.json
Caddy (already runs for map.guhs.nl, ports 80/443 open in ufw)
   live.guhs.nl  ->  file_server /var/www/guhs-live  (+ CORS, short cache, 404 = "not live yet")
guhs.nl/vadsrijk (static, GitHub Pages)
   assets/vadsrijk.js fetches https://live.guhs.nl/<file> (8 s timeout); on failure, a 404 or {"nog_niet_live":true}
   it shows "Live zodra de server de update draait" / "Nog geen spelers in het Vadsrijk"; the map backs off to one poll per 60 s
```
- Mod side: `feature/vadsrijk/web/WebExport.java` + `WebPosities.java` (commit 4d7c621f1, merged in 110-v-int). Config
  `config/guhs-vadsrijk-web.properties`: `aan=false` by default, `map=web/live`, `posities_s=10`, `status_s=60`, `wereld_s=30`,
  `hiscores_s=300`, `spelers_s=600`. Players hide themselves with `/guhs vadsrijk kaart verberg` (and `kaart toon`).
  Dungeons, the raid and houses only give a status row ("in een kerker"), never a position.
- Site side: `tools/make_vadsrijk_site.py` has `LIVE_URL = "https://live.guhs.nl/"`. The preview and the real build both use it.
  There is no sample data any more.
- Why `/var/www/guhs-live` and not `/opt/guhs/web/live`: `/opt/guhs` is `750 minecraft`, so Caddy cannot read inside it.

## Phase A: the endpoint, now (no MC change; serves an empty dataset)
Nothing here touches the world, the jar/mods or the running MC process.

### A1. DNS (Cloudflare dashboard -> guhs.nl -> DNS -> Add record)
| Type | Name | Content | Proxy | TTL |
|---|---|---|---|---|
| A | live | 2.28.142.15 | **DNS only (grey)** | Auto |
| AAAA | live | 2a01:4f8:c016:ca38::1 | **DNS only (grey)** | Auto |

Use DNS only, the same as map.guhs.nl: Caddy then gets its own Let's Encrypt certificate (HTTP-01 on port 80), and there is no
Cloudflare cache in front of the 10-second files. The server IP is already public through play.guhs.nl.

Optional API variant (needs a token with Zone.DNS edit; never paste it into chat):
```bash
ZONE=<zone id of guhs.nl>; TOKEN=<token>
for t in "A 2.28.142.15" "AAAA 2a01:4f8:c016:ca38::1"; do set -- $t
  curl -s -X POST "https://api.cloudflare.com/client/v4/zones/$ZONE/dns_records" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" --data "{\"type\":\"$1\",\"name\":\"live\",\"content\":\"$2\",\"proxied\":false,\"ttl\":1}"; done
```

### A2. Server: folder + empty dataset (as root on 2.28.142.15)
```bash
install -d -o minecraft -g minecraft -m 755 /var/www/guhs-live /var/www/guhs-live/hiscores /var/www/guhs-live/spelers
cd /var/www/guhs-live
echo '{"nog_niet_live":true}' > status.json
echo '{"nog_niet_live":true}' > wereld.json
echo '{"tijd":0,"interval_s":10,"spelers":[],"status":[],"schepen":[]}' > posities.json
echo '{"tijd":"","spelers":[]}' > spelers/index.json
chown -R minecraft:minecraft /var/www/guhs-live
```
The hiscore files are left out on purpose: a 404 there gives "Nog geen spelers in het Vadsrijk. Live zodra de server de update
draait." Once the mod runs with `aan=true`, it overwrites all of these files itself (they are owned by `minecraft`).

### A3. Server: Caddy site (append to /etc/caddy/Caddyfile)
```caddy
live.guhs.nl {
	root * /var/www/guhs-live
	header {
		Access-Control-Allow-Origin "*"
		Access-Control-Allow-Methods "GET, HEAD, OPTIONS"
		Cache-Control "public, max-age=5"
		X-Content-Type-Options nosniff
		-Server
	}
	@tmp path *.tmp
	respond @tmp 404
	@opties method OPTIONS
	respond @opties 204
	file_server
	encode gzip
}
```
Then:
```bash
cp /etc/caddy/Caddyfile /etc/caddy/Caddyfile.voor-live-$(date +%Y%m%d-%H%M)
# (append the block above)
caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile && systemctl reload caddy
```
`reload` keeps map.guhs.nl running (no restart). CORS `*` is fine because every file is public data. This also lets the
hidden preview, local previews and the real site read it.

### A4. Check
```bash
curl -sI https://live.guhs.nl/posities.json | grep -iE '^(HTTP|access-control|cache-control)'   # 200 + CORS + max-age=5
curl -s  https://live.guhs.nl/status.json        # {"nog_niet_live":true}
curl -sI https://live.guhs.nl/hiscores/generiek.json | head -1   # 404 (fine)
curl -sI https://map.guhs.nl/ | head -1           # still 200 (BlueMap untouched)
```
On https://guhs.nl/vadsrijk-preview/ the server tile then shows "Nog niet live", and the map says "Nog geen spelers in het
Vadsrijk" (an empty dataset). Before A1-A3 it shows "Live zodra de server de update draait".

### Rollback of phase A
```bash
cp /etc/caddy/Caddyfile.voor-live-<stamp> /etc/caddy/Caddyfile && caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile && systemctl reload caddy
rm -rf /var/www/guhs-live
```
Then delete the two `live` records in Cloudflare. The site falls back to "Live zodra de server de update draait" by itself.

## Phase B: switch the mod on, at the 1.10.0 rollout (with the normal release; needs the user's OK at that moment)
1. Before the rollout restart, write the config (the mod reads it on start, so no extra restart is needed):
   ```bash
   F=/opt/guhs/server26/config/guhs-vadsrijk-web.properties
   [ -f "$F" ] && cp "$F" "$F.bak"
   cat > "$F" <<'EOT'
   aan=true
   map=/var/www/guhs-live
   posities_s=10
   status_s=60
   wereld_s=30
   hiscores_s=300
   spelers_s=600
   EOT
   chown minecraft:minecraft "$F"
   ```
   If the file does not exist yet, the mod writes the defaults on its first start (`aan=false`). In that case, write the file
   above before the start, or set it after the first start and use `/guhs vadsrijk web nu` (console: `mc "guhs vadsrijk web nu"`)
   to write once; the periodic export needs `aan=true` at start.
2. After the restart: `ls -la /var/www/guhs-live` shows fresh time stamps, and
   `curl -s https://live.guhs.nl/status.json` has `"online":true`.
3. The site needs no change: it already reads live.guhs.nl.

Rollback of phase B: `aan=false` in the config at the next restart. Or, without a restart, put the empty dataset of A2 back and
`chmod 555 /var/www/guhs-live`, so the mod's writes fail silently (it logs a warning).

## Phase C: the real site (with the release, separate from this plan)
- `.github/workflows/pages.yml` on main: build with `--vadsrijk --live-url https://live.guhs.nl/` (see NOTITIES_S_SITE.md).
- On gh-pages, remove `vadsrijk-preview/` + `.github/workflows/vadsrijk-preview.yml`.
