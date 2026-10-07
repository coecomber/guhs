package nl.juiced.guhs.feature.guhpixel.bioscoop.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;

/**
 * A film of the Guhbioscoop, read from {@code assets/guhs/guhbioscoop/films/<id>.json} (a resource pack can replace or
 * add one). A film is a row of <b>scenes</b> on a canvas of {@link #B} x {@link #H} film pixels (7 x 4 blocks of 16
 * pixels; on a smaller screen it is drawn smaller with black bars). A scene is a list of <b>layers</b>, drawn in order
 * (the first one is the back); a layer is a sprite of the film's own sprite sheet, a coloured rectangle, or a line of
 * text, and it moves along <b>keys</b> (time, place, size, turn). On top come the <b>subtitles</b>; the film also holds
 * <b>sound cues</b> (played by the client at the screen) and the audience's <b>cues</b> (the server's copy: FilmInfo).
 * Times are ticks (20 a second); in a layer they count from the start of its scene.
 *
 * <pre>
 * {"formaat": 1, "id": "skyblok", "duur": 900,
 *  "atlas": {"textuur": "guhs:textures/guhbioscoop/skyblok.png", "breedte": 256, "hoogte": 256},
 *  "sprites": {"guh": {"uv": [0, 0, 16, 13]}, "vuurwerk": {"uv": [0, 20, 9, 9], "frames": 3, "per": 4}},
 *  "scenes": [{"naam": "eiland", "duur": 120, "lagen": [
 *      {"vlak": "#8fd0ff", "x": 0, "y": 0, "w": 112, "h": 64},
 *      {"sprite": "guh", "sleutels": [{"t": 0, "x": -16, "y": 40}, {"t": 40, "x": 48, "y": 40, "e": "zacht"}],
 *       "spiegel": false, "van": 0, "tot": 120, "golf": {"ay": 1, "per": 12}, "kleur": "#ffffff", "frame": 0},
 *      {"tekst": "gui.guhs.guhbioscoop.film.skyblok.t1", "x": 56, "y": 6, "schaal": 1.0, "uitlijn": "midden",
 *       "kleur": "#ffffff", "schaduw": true, "teller": {"van": 100, "naar": 1, "t0": 20, "t1": 100}}]}],
 *  "ondertitels": [{"van": 10, "tot": 70, "tekst": "gui.guhs.guhbioscoop.film.skyblok.o1"}],
 *  "geluiden": [{"t": 2, "geluid": "guhs:guhbioscoop.film.titel", "volume": 1.0, "toon": 1.0}],
 *  "cues": [{"t": 100, "soort": "lach"}]}
 * </pre>
 * A layer: exactly one of {@code sprite} (a name of "sprites"), {@code vlak} (a colour) or {@code tekst} (a lang key;
 * {@code letterlijk} for a literal text). Its place: {@code x}, {@code y} (the upper left corner in film pixels; for a
 * text the anchor) and optionally {@code s} (size, 1 = normal, around the middle), {@code r} (degrees, around the
 * middle), for a vlak {@code w} and {@code h}: either directly in the layer (it stands still) or as {@code sleutels},
 * each with a {@code t}; between two keys the values glide ({@code e} of the later key: "lin" straight, "zacht" eased,
 * "uit" slowing down, "in" speeding up, "stap" a jump). {@code van}/{@code tot}: when it shows; {@code spiegel}: mirrored;
 * {@code golf}: a gentle wave on top ({@code ax}, {@code ay} pixels, {@code per} ticks, {@code fase}); {@code kleur}: a
 * tint; {@code frame}: one fixed frame of an animated sprite. Text only: {@code schaal} (1 = 8 film pixels high),
 * {@code uitlijn} (links, midden, rechts), {@code schaduw}, {@code teller} (a number that runs from van to naar between
 * t0 and t1: the %s of the text). Pictures have no half-transparent pixels and text always lies on top of the pictures.
 */
public final class FilmData {
    /** The canvas, in film pixels. */
    public static final int B = 112, H = 64;
    static final int SPRITE = 0, VLAK = 1, TEKST = 2;
    static final int LINKS = 0, MIDDEN = 1, RECHTS = 2;
    private static final byte LIN = 0, ZACHT = 1, UIT = 2, IN = 3, STAP = 4;

    record Sprite(int u, int v, int w, int h, int frames, int per) {
    }

    record Ondertitel(int van, int tot, String sleutel) {
    }

    record Geluid(int t, Identifier id, float volume, float toon) {
    }

    static final class Scene {
        int van, duur;
        Laag[] lagen;
    }

    /** One layer; the arrays are its keys (all the same length, at least one). */
    static final class Laag {
        int soort;
        @Nullable
        Sprite sprite;
        String tekst = "";
        boolean letterlijk, spiegel, schaduw;
        int kleur = 0xFFFFFFFF;
        int van, tot = Integer.MAX_VALUE;
        int frame = -1;
        int uitlijn = LINKS;
        float schaal = 1f;
        float[] t, x, y, s, r, w, h;
        byte[] e;
        float golfAx, golfAy, golfPer = 20f, golfFase;
        boolean teller;
        int tellerVan, tellerNaar, tellerT0, tellerT1;

        /** Where between key i and i + 1 the time lt lies (0..1, eased), and i itself in {@code uit[0]}. */
        private float plek(float lt, int[] uit) {
            int n = t.length;
            if (n == 1 || lt <= t[0]) {
                uit[0] = 0;
                return 0f;
            }
            if (lt >= t[n - 1]) {
                uit[0] = n - 1;
                return 0f;
            }
            int i = 0;
            while (i < n - 2 && lt >= t[i + 1]) {
                i++;
            }
            uit[0] = i;
            float f = (lt - t[i]) / Math.max(0.0001f, t[i + 1] - t[i]);
            return switch (e[i + 1]) {
                case ZACHT -> f * f * (3 - 2 * f);
                case UIT -> 1 - (1 - f) * (1 - f);
                case IN -> f * f;
                case STAP -> 0f;
                default -> f;
            };
        }

        /** Fills {@code uit}: x, y, s, r, w, h at scene time lt. */
        void op(float lt, float[] uit, int[] hulp) {
            float f = plek(lt, hulp);
            int i = hulp[0], j = Math.min(i + 1, t.length - 1);
            uit[0] = Mth.lerp(f, x[i], x[j]);
            uit[1] = Mth.lerp(f, y[i], y[j]);
            uit[2] = Mth.lerp(f, s[i], s[j]);
            uit[3] = Mth.lerp(f, r[i], r[j]);
            uit[4] = Mth.lerp(f, w[i], w[j]);
            uit[5] = Mth.lerp(f, h[i], h[j]);
            if (golfAx != 0 || golfAy != 0) {
                double a = (lt + golfFase) / golfPer * Math.PI * 2;
                uit[0] += (float) Math.cos(a) * golfAx;
                uit[1] += (float) Math.sin(a) * golfAy;
            }
        }
    }

    public final String id;
    public final int duur;
    public final Identifier atlas;
    public final int atlasB, atlasH;
    final Map<String, Sprite> sprites = new HashMap<>();
    final Scene[] scenes;
    final Ondertitel[] ondertitels;
    final Geluid[] geluiden;
    /** The sprite "wit" (one white pixel block): rectangles are drawn with it. */
    final Sprite wit;

    private FilmData(String id, JsonObject o) {
        this.id = id;
        this.duur = o.get("duur").getAsInt();
        JsonObject a = o.getAsJsonObject("atlas");
        this.atlas = Identifier.parse(a.get("textuur").getAsString());
        this.atlasB = a.get("breedte").getAsInt();
        this.atlasH = a.get("hoogte").getAsInt();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("sprites").entrySet()) {
            JsonObject s = e.getValue().getAsJsonObject();
            JsonArray uv = s.getAsJsonArray("uv");
            sprites.put(e.getKey(), new Sprite(uv.get(0).getAsInt(), uv.get(1).getAsInt(), uv.get(2).getAsInt(), uv.get(3).getAsInt(),
                    Math.max(1, geheel(s, "frames", 1)), Math.max(1, geheel(s, "per", 4))));
        }
        Sprite w = sprites.get("wit");
        if (w == null) {
            throw new IllegalArgumentException("film " + id + " has no sprite 'wit'");
        }
        this.wit = w;
        List<Scene> lijst = new ArrayList<>();
        int van = 0;
        for (JsonElement se : o.getAsJsonArray("scenes")) {
            JsonObject so = se.getAsJsonObject();
            Scene scene = new Scene();
            scene.van = van;
            scene.duur = so.get("duur").getAsInt();
            van += scene.duur;
            List<Laag> lagen = new ArrayList<>();
            for (JsonElement le : so.getAsJsonArray("lagen")) {
                lagen.add(laag(le.getAsJsonObject()));
            }
            scene.lagen = lagen.toArray(Laag[]::new);
            lijst.add(scene);
        }
        this.scenes = lijst.toArray(Scene[]::new);
        List<Ondertitel> ond = new ArrayList<>();
        if (o.has("ondertitels")) {
            for (JsonElement e : o.getAsJsonArray("ondertitels")) {
                JsonObject x = e.getAsJsonObject();
                ond.add(new Ondertitel(x.get("van").getAsInt(), x.get("tot").getAsInt(), x.get("tekst").getAsString()));
            }
        }
        this.ondertitels = ond.toArray(Ondertitel[]::new);
        List<Geluid> gel = new ArrayList<>();
        if (o.has("geluiden")) {
            for (JsonElement e : o.getAsJsonArray("geluiden")) {
                JsonObject x = e.getAsJsonObject();
                gel.add(new Geluid(x.get("t").getAsInt(), Identifier.parse(x.get("geluid").getAsString()), getal(x, "volume", 1f), getal(x, "toon", 1f)));
            }
        }
        this.geluiden = gel.toArray(Geluid[]::new);
    }

    private Laag laag(JsonObject o) {
        Laag l = new Laag();
        if (o.has("sprite")) {
            l.soort = SPRITE;
            l.sprite = sprites.get(o.get("sprite").getAsString());
            if (l.sprite == null) {
                throw new IllegalArgumentException("film " + id + ": unknown sprite " + o.get("sprite").getAsString());
            }
        } else if (o.has("vlak")) {
            l.soort = VLAK;
            l.kleur = kleur(o.get("vlak").getAsString());
        } else {
            l.soort = TEKST;
            l.letterlijk = o.has("letterlijk");
            l.tekst = o.get(l.letterlijk ? "letterlijk" : "tekst").getAsString();
            l.schaal = getal(o, "schaal", 1f);
            String u = o.has("uitlijn") ? o.get("uitlijn").getAsString() : "links";
            l.uitlijn = u.equals("midden") ? MIDDEN : u.equals("rechts") ? RECHTS : LINKS;
            l.schaduw = o.has("schaduw") && o.get("schaduw").getAsBoolean();
            if (o.has("teller")) {
                JsonObject t = o.getAsJsonObject("teller");
                l.teller = true;
                l.tellerVan = t.get("van").getAsInt();
                l.tellerNaar = t.get("naar").getAsInt();
                l.tellerT0 = t.get("t0").getAsInt();
                l.tellerT1 = t.get("t1").getAsInt();
            }
        }
        if (o.has("kleur")) {
            l.kleur = kleur(o.get("kleur").getAsString());
        }
        l.spiegel = o.has("spiegel") && o.get("spiegel").getAsBoolean();
        l.van = geheel(o, "van", 0);
        l.tot = geheel(o, "tot", Integer.MAX_VALUE);
        l.frame = geheel(o, "frame", -1);
        if (o.has("golf")) {
            JsonObject g = o.getAsJsonObject("golf");
            l.golfAx = getal(g, "ax", 0f);
            l.golfAy = getal(g, "ay", 0f);
            l.golfPer = Math.max(1f, getal(g, "per", 20f));
            l.golfFase = getal(g, "fase", 0f);
        }
        JsonArray keys = o.has("sleutels") ? o.getAsJsonArray("sleutels") : null;
        int n = keys == null ? 1 : Math.max(1, keys.size());
        l.t = new float[n];
        l.x = new float[n];
        l.y = new float[n];
        l.s = new float[n];
        l.r = new float[n];
        l.w = new float[n];
        l.h = new float[n];
        l.e = new byte[n];
        for (int i = 0; i < n; i++) {
            JsonObject k = keys == null || keys.isEmpty() ? o : keys.get(i).getAsJsonObject();
            // a value that a key leaves out stays what it was at the key before it
            l.t[i] = getal(k, "t", i == 0 ? 0f : l.t[i - 1]);
            l.x[i] = getal(k, "x", i == 0 ? 0f : l.x[i - 1]);
            l.y[i] = getal(k, "y", i == 0 ? 0f : l.y[i - 1]);
            l.s[i] = getal(k, "s", i == 0 ? 1f : l.s[i - 1]);
            l.r[i] = getal(k, "r", i == 0 ? 0f : l.r[i - 1]);
            l.w[i] = getal(k, "w", i == 0 ? B : l.w[i - 1]);
            l.h[i] = getal(k, "h", i == 0 ? H : l.h[i - 1]);
            String e = k.has("e") ? k.get("e").getAsString() : "lin";
            l.e[i] = switch (e) {
                case "zacht" -> ZACHT;
                case "uit" -> UIT;
                case "in" -> IN;
                case "stap" -> STAP;
                default -> LIN;
            };
        }
        return l;
    }

    private static int geheel(JsonObject o, String key, int anders) {
        return o.has(key) ? o.get(key).getAsInt() : anders;
    }

    private static float getal(JsonObject o, String key, float anders) {
        return o.has(key) ? o.get(key).getAsFloat() : anders;
    }

    /** "#rrggbb" (or "#aarrggbb") to ARGB; pictures are never half transparent, so the alpha is always full. */
    static int kleur(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        long v = Long.parseLong(h, 16);
        return 0xFF000000 | (int) (v & 0xFFFFFF);
    }

    /** The scene at film time t (ticks), or null after the end. */
    @Nullable
    Scene scene(float t) {
        for (Scene s : scenes) {
            if (t >= s.van && t < s.van + s.duur) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    Ondertitel ondertitel(float t) {
        for (Ondertitel o : ondertitels) {
            if (t >= o.van && t < o.tot) {
                return o;
            }
        }
        return null;
    }

    // --- loading --------------------------------------------------------------------------------------------------------

    private static final Map<String, Optional<FilmData>> CACHE = new HashMap<>();

    /** The film with this id (read once; null when it is missing or broken, the log says why). */
    @Nullable
    public static FilmData laad(String id) {
        Optional<FilmData> f = CACHE.get(id);
        if (f == null) {
            f = Optional.ofNullable(lees(id));
            CACHE.put(id, f);
        }
        return f.orElse(null);
    }

    /** (Resource reload) read the films again. */
    public static void vergeet() {
        CACHE.clear();
    }

    @Nullable
    private static FilmData lees(String id) {
        if (!Identifier.isValidPath(id)) {
            return null;
        }
        Identifier bestand = Guhs.id("guhbioscoop/films/" + id + ".json");
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(bestand);
            if (res.isEmpty()) {
                LogUtils.getLogger().warn("Guhbioscoop: no film {}", bestand);
                return null;
            }
            try (Reader r = res.get().openAsReader()) {
                return new FilmData(id, JsonParser.parseReader(r).getAsJsonObject());
            }
        } catch (Exception e) {
            LogUtils.getLogger().warn("Guhbioscoop: cannot read film {}", bestand, e);
            return null;
        }
    }
}
