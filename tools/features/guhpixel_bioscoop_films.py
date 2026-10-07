"""
Guhbioscoop: the nine films (DESIGN_PX section 4): one per joke game (Skyblok, Bedwars, Vadsnite, Guhmon, Boer zoekt Guh,
Among Guhs) and one per story (Balto, Mewtwo, 626). The films are written in guhpixel_bioscoop_films1/2/3.py with the tools
of guhpixel_bioscoop_film.py (the format is described there). The order here is the order of Films.IDS (Java).

A preview without the game:  python tools/features/guhpixel_bioscoop_films.py <id> [tick ...]      (from the worktree root)
  writes build/guhbioscoop/<id>_<tick>.png (default: a strip of pictures through the whole film).
  --en shows the English texts (tools/lang/en/c37_px_bioscoop.json).
"""
import os
import sys

if __name__ == "__main__":
    sys.path.insert(0, "tools")

from features import guhpixel_bioscoop_film as film
from features import guhpixel_bioscoop_films1 as deel1
from features import guhpixel_bioscoop_films2 as deel2
from features import guhpixel_bioscoop_films3 as deel3

MAKERS = deel1.FILMS + deel2.FILMS + deel3.FILMS


def alle():
    """Every film, freshly built."""
    return [maak() for maak in MAKERS]


def build(h):
    """Writes the sprite sheets, the film files and the server's index; returns the films' texts {lang key: Dutch}."""
    return film.schrijf(h, alle(), film.lees_engels())


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    engels = film.lees_engels() if "--en" in sys.argv else None
    uit = os.path.join("build", "guhbioscoop")
    os.makedirs(uit, exist_ok=True)
    for f in alle():
        if args and f.id != args[0]:
            continue
        teksten = dict(f.teksten, **{k: v for k, v in (engels or {}).items() if k in f.teksten})
        ticks = [int(a) for a in args[1:]]
        if ticks:
            for t in ticks:
                film.beeld(f, t, teksten=teksten).save(os.path.join(uit, f"{f.id}_{t:04d}.png"))
        else:
            from PIL import Image
            stap = 25
            n = f.duur // stap
            kol = 6
            vel = Image.new("RGBA", (kol * (film.B * 3 + 4), ((n + kol - 1) // kol) * (film.H * 3 + 4)), (60, 60, 60, 255))
            for i in range(n):
                b = film.beeld(f, i * stap + 12, schaal=3, teksten=teksten)
                vel.paste(b, ((i % kol) * (film.B * 3 + 4), (i // kol) * (film.H * 3 + 4)))
            vel.save(os.path.join(uit, f"{f.id}.png"))
        print(f.id, f.duur, "ticks,", len(f.scenes), "scenes,", len(f.ondertitels), "subtitles,", len(f.cues), "cues,", len(f.sprites), "sprites")
