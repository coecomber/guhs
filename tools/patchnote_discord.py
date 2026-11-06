"""
The Discord changelog from the same source as the site's patch notes (docs/patchnotes/<versie>.md).

    python tools/patchnote_discord.py 1.10.0            # prints the payload (dry run)
    python tools/patchnote_discord.py 1.10.0 --post     # posts it: needs GUHS_DISCORD_WEBHOOK in the environment

Text = the body without the :::spoiler blocks and without inline pictures; at most 4 pictures (discord_beelden).
Post only after the release is live on the server and in the pack (see the memory rule on Discord changelogs).
"""
import argparse
import json
import mimetypes
import os
import sys
import urllib.request
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from wiki_site.vadsrijk import patchnotes as PN  # noqa: E402


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("versie")
    ap.add_argument("--post", action="store_true")
    a = ap.parse_args()
    map_ = os.path.join(os.path.dirname(HERE), "docs", "patchnotes")
    n = next((x for x in PN.lees(map_, concepten=True) if x["versie"] == a.versie), None)
    if not n:
        raise SystemExit(f"no docs/patchnotes/{a.versie}.md")
    tekst = PN.discord_tekst(n)
    payload = {"embeds": [{"title": f"Guhs {n['versie']}: {n['titel']}", "description": tekst[:4000], "color": 0xC2577F}]}
    beelden = [os.path.normpath(os.path.join(map_, b)) for b in n["discord_beelden"]]
    print(json.dumps(payload, ensure_ascii=False, indent=1))
    print("pictures:", *beelden, sep="\n  ")
    if not a.post:
        return
    if n["concept"]:
        raise SystemExit("this is a draft (concept: true); refusing to post")
    url = os.environ.get("GUHS_DISCORD_WEBHOOK")
    if not url:
        raise SystemExit("set GUHS_DISCORD_WEBHOOK")
    grens = uuid.uuid4().hex
    delen = [f'--{grens}\r\nContent-Disposition: form-data; name="payload_json"\r\nContent-Type: application/json\r\n\r\n'.encode()
             + json.dumps(payload).encode() + b"\r\n"]
    for i, b in enumerate(beelden):
        mt = mimetypes.guess_type(b)[0] or "application/octet-stream"
        delen.append(f'--{grens}\r\nContent-Disposition: form-data; name="files[{i}]"; filename="{os.path.basename(b)}"\r\nContent-Type: {mt}\r\n\r\n'.encode()
                     + open(b, "rb").read() + b"\r\n")
    data = b"".join(delen) + f"--{grens}--\r\n".encode()
    req = urllib.request.Request(url, data=data, headers={"Content-Type": f"multipart/form-data; boundary={grens}", "User-Agent": "guhs-changelog"})
    with urllib.request.urlopen(req) as r:
        print("posted:", r.status)


if __name__ == "__main__":
    main()
