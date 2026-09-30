"""
The Guhs wiki site generator (tools/make_wiki_site.py): a multi-page static wiki in docs/site/.

  gamedata.py  reads the game data from the project (lang, Java enums, worldgen, loot, recipes, advancements, FTB quests)
  kb.py        the knowledge base: every text block of the one-page wiki (tools/make_wiki.py), cut into chunks
  site.py      the page model: pages, categories, image claims, aliases (auto-links)
  pages.py     builds every page from the game data + the knowledge base
  render.py    HTML templates, CSS, JS, search index, sitemap, 404
  images.py    copies / converts the pictures (PNG -> WebP) and makes fallback icons
  check.py     the link checker
"""
