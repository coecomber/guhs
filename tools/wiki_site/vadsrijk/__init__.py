"""
guhs.nl/vadsrijk: the Vadsrijk section of the Guhs site (S-SITE, 1.10). Built by tools/make_vadsrijk_site.py (alone, fast) or as
part of tools/make_wiki_site.py (--vadsrijk). Static, NL + EN, link-checked; the live part reads JSON from live.guhs.nl.

  bron.py    the data export: reads the repo (quests*.json, lang, POI_*.md, *_wiki.py prose, CHANGELOG/patch notes) and, when it
             exists, docs/wiki/vadsrijk_runtime.json (the mod's WikiExport dump: areas, spawners, arenas, base types, loot tables,
             classes, pets, mounts, NPC positions, sets, crystals, stars) -> one dict, also written as vadsrijk/data/vadsrijk.json
  kaart.py   the map tiles: top-down from the .nbt map tiles (technical/satellite style), cut into Leaflet zoom tiles
  pages.py   the HTML pages (wiki, map, highscores, profiles, live, guide, patch notes)
  stijl.py   the stylesheet + scripts of the section
  (W-SITE: no sample live data any more; previews read live.guhs.nl too and show an empty state when it is not there)
"""
