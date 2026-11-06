# Patch notes (one source for Discord + guhs.nl/vadsrijk)

One file per release: `<versie>.md` with a front matter (versie, datum, titel, samenvatting, concept, discord_beelden) and a
markdown body. `:::spoiler Title` ... `:::` blocks only show on the site (behind a spoiler button); Discord skips them.
`concept: true` = a draft: only in local previews (`--concepten`), never on the real site.

- Site: `python tools/make_vadsrijk_site.py` (or the full `tools/make_wiki_site.py`) renders `patchnotes/<versie>.html`.
- Discord: `python tools/patchnote_discord.py <versie>` prints the payload; `--post` sends it, with the webhook URL in the
  `GUHS_DISCORD_WEBHOOK` environment variable (never in the repo). Post only after the release is live (memory rule).
Format details: tools/wiki_site/vadsrijk/patchnotes.py.
