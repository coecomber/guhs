"""
The Guhs wiki site generator (tools/make_wiki_site.py): a multi-page static wiki in docs/site/.

  gamedata.py  reads the game data from the project (lang, Java enums, worldgen, loot, recipes, advancements, FTB quests)
  kb.py        the knowledge base: every text block of the one-page wiki (tools/make_wiki.py), cut into chunks
  site.py      the page model: pages, categories, image claims, aliases (auto-links)
  pages.py     builds every page from the game data + the knowledge base
  render.py    HTML templates, CSS, JS, search index, sitemap, 404
  images.py    copies / converts the pictures (PNG -> WebP) and makes fallback icons
  check.py     the link checker

The pages of bbq2 (Guh-technologie, the Guhbarbecuether buildings, In de ban van de Knabbelring, Super Guhrio) come from the
slices' own notes tools/features/*_wiki.py, joined and given their English and spoiler rules by tools/wiki_bbq2
(topics.extend() puts them into the hand tables; tools/make_wiki.py bbq2_sections() into the knowledge base).
"""
