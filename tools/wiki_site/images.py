"""
The pictures of the site: the renders of tools/wiki_renders.py (docs/wiki/img/*.png) are copied as WebP into
docs/site/img (only the ones a page uses), with small thumbnails for the lists in docs/site/img/t. Items and blocks
without a render get an icon made from their own texture (the item model's layer0, or the block model's texture).
"""
import json
import os

from PIL import Image

ICON_MAX = 128          # at most this big: pixel art, lossless and scaled with nearest neighbour
BIG_MAX = 1280          # renders and screenshots are scaled down to this (longest side)
THUMB = 96              # the thumbnails in the category tables


class Images:
    def __init__(self, src_dir, assets_dir, out_dir):
        self.src_dir, self.assets, self.out = src_dir, assets_dir, out_dir
        self.available = {f[:-4] for f in os.listdir(src_dir) if f.endswith(".png")} if os.path.isdir(src_dir) else set()
        self.generated = {}         # name -> PIL image (fallback icons)
        self.gen_src = {}           # name -> the texture it was made from
        self.used, self.thumbs = set(), set()
        self.sizes = {}

    def has(self, name):
        return bool(name) and (name in self.available or name in self.generated)

    def size(self, name):
        if name not in self.sizes:
            if name in self.generated:
                self.sizes[name] = self.generated[name].size
            else:
                with Image.open(os.path.join(self.src_dir, name + ".png")) as im:
                    self.sizes[name] = im.size
        return self.sizes[name]

    def first(self, *names):
        for n in names:
            if self.has(n):
                return n
        return None

    # --- fallback icons from the textures -------------------------------------------------------------------------------------
    def _texture(self, ref):
        ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
        if ns != "guhs":
            return None
        f = os.path.join(self.assets, "textures", path + ".png")
        if not os.path.exists(f):
            return None
        im = Image.open(f).convert("RGBA")
        if im.height > im.width:
            im = im.crop((0, 0, im.width, im.width))
        return im

    def _model(self, ref):
        ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
        if ns != "guhs":
            return None
        f = os.path.join(self.assets, "models", path + ".json")
        if not os.path.exists(f):
            return None
        with open(f, encoding="utf-8") as fh:
            return json.load(fh)

    def _model_textures(self, ref, depth=0):
        m = self._model(ref)
        if m is None or depth > 6:
            return {}
        tex = {k: (v.get("sprite", "") if isinstance(v, dict) else v)       # (26.1: {"sprite": ..., "force_translucent": true})
               for k, v in m.get("textures", {}).items()}
        if "parent" in m:
            parent = self._model_textures(m["parent"], depth + 1)
            parent.update(tex)
            tex = parent
        return tex

    def item_icon(self, item_id):
        """'icon_<id>' / '<id>' / 'block_<id>' from the renders, else an icon made from the texture (None if there is none)."""
        name = self.first(f"icon_{item_id}", item_id, f"block_{item_id}", f"kleding_{item_id}", f"oren_{item_id}")
        if name:
            return name
        gen = f"gen_{item_id}"
        if gen in self.generated:
            return gen
        tex = self._model_textures(f"guhs:item/{item_id}")
        ref = None
        for key in ("layer0", "all", "side", "front", "texture", "top", "cross", "plant", "particle"):
            if key in tex and not str(tex[key]).startswith("#"):
                ref = tex[key]
                break
        if ref is None:   # compasses and other animated items: their first frame
            for suffix in ("_00", "_0", "_01"):
                if os.path.exists(os.path.join(self.assets, "textures", "item", item_id + suffix + ".png")):
                    ref = f"guhs:item/{item_id}{suffix}"
                    break
        im = self._texture(ref) if ref else None
        if im is None:
            return None
        im = im.resize((64, 64), Image.NEAREST)
        self.generated[gen] = im
        ns, path = ref.split(":", 1)
        self.gen_src[gen] = os.path.join(self.assets, "textures", path + ".png")
        return gen

    def spoiler_tile(self):
        """'spoiler': the picture a list, card or search result shows instead of a page's own when that would give a story's
        surprise away (a question mark on a dashed tile)."""
        if "spoiler" in self.generated:
            return "spoiler"
        from PIL import ImageDraw
        small = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(small)
        d.rounded_rectangle((0, 0, 15, 15), radius=3, fill=(253, 227, 236, 255), outline=(184, 161, 212, 255))
        for x, y in ((6, 3), (7, 3), (8, 3), (9, 3), (5, 4), (10, 4), (10, 5), (9, 6), (8, 7), (7, 8), (7, 9), (7, 12), (8, 12), (8, 8), (8, 9),
                     (5, 5), (10, 6), (9, 7)):
            small.putpixel((x, y), (217, 70, 122, 255))
        self.generated["spoiler"] = small.resize((64, 64), Image.NEAREST)
        return "spoiler"

    # --- use + write ----------------------------------------------------------------------------------------------------------
    def use(self, name, thumb=False):
        if not self.has(name):
            return None
        if thumb:
            self.thumbs.add(name)
            return f"img/t/{name}.webp"
        self.used.add(name)
        return f"img/{name}.webp"

    def _load(self, name):
        if name in self.generated:
            return self.generated[name].copy()
        im = Image.open(os.path.join(self.src_dir, name + ".png"))
        im.load()
        return im.convert("RGBA")

    def _src_mtime(self, name):
        if name in self.generated:
            return os.path.getmtime(self.gen_src[name]) if name in self.gen_src else float("inf")
        return os.path.getmtime(os.path.join(self.src_dir, name + ".png"))

    def write(self):
        os.makedirs(os.path.join(self.out, "t"), exist_ok=True)
        written = 0
        for name in sorted(self.used):
            dst = os.path.join(self.out, name + ".webp")
            if os.path.exists(dst) and os.path.getmtime(dst) >= self._src_mtime(name):
                continue
            im = self._load(name)
            if max(im.size) <= ICON_MAX:
                if max(im.size) < 64:
                    f = 64 // max(im.size) or 1
                    im = im.resize((im.width * f, im.height * f), Image.NEAREST)
                im.save(dst, "WEBP", lossless=True, quality=100, method=4)
            else:
                if max(im.size) > BIG_MAX:
                    im.thumbnail((BIG_MAX, BIG_MAX), Image.LANCZOS)
                im.save(dst, "WEBP", quality=80, alpha_quality=90, method=5)
            written += 1
        for name in sorted(self.thumbs):
            dst = os.path.join(self.out, "t", name + ".webp")
            if os.path.exists(dst) and os.path.getmtime(dst) >= self._src_mtime(name):
                continue
            im = self._load(name)
            if max(im.size) <= ICON_MAX:
                im = im.resize((THUMB // 2 * im.width // max(im.size) or 1, THUMB // 2 * im.height // max(im.size) or 1), Image.NEAREST) \
                    if max(im.size) < THUMB // 2 else im
                im.save(dst, "WEBP", lossless=True, quality=100, method=4)
            else:
                bbox = im.getbbox()      # trim the empty border of renders
                if bbox:
                    im = im.crop(bbox)
                im.thumbnail((THUMB * 2, THUMB * 2), Image.LANCZOS)
                im.save(dst, "WEBP", quality=78, alpha_quality=85, method=5)
            written += 1
        # remove pictures no page uses any more
        keep = {n + ".webp" for n in self.used}
        for f in os.listdir(self.out):
            if f.endswith(".webp") and f not in keep:
                os.remove(os.path.join(self.out, f))
        keep_t = {n + ".webp" for n in self.thumbs}
        for f in os.listdir(os.path.join(self.out, "t")):
            if f.endswith(".webp") and f not in keep_t:
                os.remove(os.path.join(self.out, "t", f))
        return written
