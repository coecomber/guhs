package nl.juiced.guhs.feature.techbron;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The light show of a Disco-dynamo: every disc has its own. The dance floor has {@link #RASTER} x {@link #RASTER} lamps;
 * a show is a pattern, a tempo and a few colours, and {@link #kleur} says what colour a lamp has at a moment (the client
 * draws it, {@code client.DiscoDynamoRenderer}; pure arithmetic, so the server and the game tests know the shows too).
 * The vanilla discs and the mod's own disc have a hand-picked show ({@link #VAST}); a disc of another mod gets one made
 * from its name, so that one is always the same too.
 */
public record LichtShow(Patroon patroon, int tempo, int[] kleuren) {
    /** The lamps of the dance floor: this many per side. */
    public static final int RASTER = 6;

    public enum Patroon {
        /** Squares of 2 x 2 that swap on the beat. */
        SCHAAKBORD,
        /** A wave that rolls diagonally over the floor. */
        GOLF,
        /** Rings that run from the middle outwards. */
        RINGEN,
        /** Lamps that pop on here and there and fade. */
        REGEN,
        /** Arms that turn around the middle. */
        SPIRAAL,
        /** Rows that march to the back. */
        STREPEN,
        /** The whole floor beats like a heart. */
        HARTSLAG,
        /** A plus and a cross that take turns. */
        KRUIS,
        /** A beating heart (the mod's own disc). */
        HART
    }

    /** The heart of {@link Patroon#HART}, row 0 = the back of the floor (so it stands upright seen from the front). */
    private static final String[] HART = {"XX..XX", "XXXXXX", "XXXXXX", ".XXXX.", "..XX..", "......"};

    /** No disc: every lamp dark. */
    public static final LichtShow UIT = new LichtShow(Patroon.HARTSLAG, 20, new int[] {0xFF000000});

    private static final Map<Identifier, LichtShow> VAST = new HashMap<>();

    private static void vast(String item, Patroon patroon, int tempo, int... kleuren) {
        VAST.put(item.contains(":") ? Identifier.parse(item) : Identifier.withDefaultNamespace(item), new LichtShow(patroon, tempo, kleuren));
    }

    static {
        vast("music_disc_13", Patroon.REGEN, 16, 0xFFF2C94C, 0xFFFFF1B8, 0xFF8A6D1F);
        vast("music_disc_cat", Patroon.GOLF, 10, 0xFF7BE04A, 0xFFB8F27A, 0xFF2E9E3A);
        vast("music_disc_blocks", Patroon.SCHAAKBORD, 9, 0xFFFF6A3D, 0xFFFFB03B, 0xFFE0352B);
        vast("music_disc_chirp", Patroon.STREPEN, 8, 0xFFE0352B, 0xFFFF8A80, 0xFFFFD54F);
        vast("music_disc_far", Patroon.RINGEN, 10, 0xFFB8F24A, 0xFF4AF2A0, 0xFFF2F24A);
        vast("music_disc_mall", Patroon.GOLF, 14, 0xFFA05AE0, 0xFF6A4ACF, 0xFFE0A0FF);
        vast("music_disc_mellohi", Patroon.HARTSLAG, 14, 0xFFE04AC8, 0xFFFFFFFF, 0xFF8A2BE2);
        vast("music_disc_stal", Patroon.KRUIS, 9, 0xFFFFFFFF, 0xFF9AA0A6, 0xFF4A4F57);
        vast("music_disc_strad", Patroon.SPIRAAL, 8, 0xFFFFFFFF, 0xFF7FD8FF, 0xFFFFB3E6);
        vast("music_disc_ward", Patroon.RINGEN, 8, 0xFF2ECC71, 0xFF0B5D3B, 0xFFA8FFCB);
        vast("music_disc_11", Patroon.REGEN, 5, 0xFF8B1A1A, 0xFF3A3A3A, 0xFFD94A4A);
        vast("music_disc_wait", Patroon.STREPEN, 7, 0xFF4A8CF2, 0xFF7FD0FF, 0xFF2B4FCF);
        vast("music_disc_otherside", Patroon.SPIRAAL, 6, 0xFF2BD9C4, 0xFFF2C94C, 0xFF4AF27F);
        vast("music_disc_5", Patroon.HARTSLAG, 20, 0xFF1F6F6B, 0xFF0E3B46, 0xFF6FE0D0);
        vast("music_disc_pigstep", Patroon.SCHAAKBORD, 6, 0xFFE0352B, 0xFFF2C94C, 0xFFFF7A1A);
        vast("music_disc_relic", Patroon.GOLF, 8, 0xFF2BB7A4, 0xFFC9A26B, 0xFF7FE0D0);
        vast("music_disc_creator", Patroon.KRUIS, 7, 0xFF9BE04A, 0xFF2BB7A4, 0xFFF2F24A);
        vast("music_disc_creator_music_box", Patroon.REGEN, 10, 0xFFFFE066, 0xFFFFB3C7, 0xFFB3E5FF);
        vast("music_disc_precipice", Patroon.RINGEN, 12, 0xFF8FD3FF, 0xFFB0BEC5, 0xFF4A6FA5);
        vast("music_disc_tears", Patroon.HARTSLAG, 10, 0xFFFFFFFF, 0xFFB0BEC5, 0xFFFF8A80);
        vast("music_disc_lava_chicken", Patroon.SPIRAAL, 5, 0xFFFF7A1A, 0xFFF2C94C, 0xFFE0352B);
        vast("guhs:music_disc_ze_hangen", Patroon.HART, 10, 0xFFFF6FB1, 0xFFFFC1DC, 0xFFB57BFF);
    }

    /** The discs with a hand-picked show. */
    public static List<Identifier> vasteplaten() {
        return VAST.keySet().stream().sorted().toList();
    }

    /** The show of this disc ({@link #UIT} for an empty stack). */
    public static LichtShow van(ItemStack plaat) {
        return plaat.isEmpty() ? UIT : van(BuiltInRegistries.ITEM.getKey(plaat.getItem()));
    }

    /** The show of the disc with this item id: the hand-picked one, or one made from the name. */
    public static LichtShow van(Identifier item) {
        LichtShow vast = VAST.get(item);
        if (vast != null) {
            return vast;
        }
        int h = item.toString().hashCode();
        Patroon[] alle = Patroon.values();
        Patroon patroon = alle[Math.floorMod(h, alle.length - 1)];   // (never the heart: that is our own disc's)
        float tint = Math.floorMod(h >> 4, 360) / 360f;
        int[] kleuren = {Mth.hsvToArgb(tint, 0.75f, 1f, 255), Mth.hsvToArgb((tint + 0.33f) % 1f, 0.6f, 1f, 255),
                Mth.hsvToArgb((tint + 0.58f) % 1f, 0.85f, 0.9f, 255)};
        return new LichtShow(patroon, 6 + Math.floorMod(h >> 12, 9), kleuren);
    }

    /**
     * The colour (opaque ARGB) of the lamp in this column (0 = left, seen from the front of the Disco-dynamo) and row
     * (0 = the back) at this moment (in ticks, with a fraction for smooth drawing).
     */
    public int kleur(int kolom, int rij, float tijd) {
        int n = kleuren.length;
        float maat = tijd / tempo;
        int b = Mth.floor(maat);
        float f = maat - b;
        float x = kolom - (RASTER - 1) / 2f, y = rij - (RASTER - 1) / 2f;
        int kies;
        float fel;
        switch (patroon) {
            case SCHAAKBORD -> {
                int vak = kolom / 2 + rij / 2 + b;
                kies = vak;
                fel = (vak & 1) == 0 ? 1f : 0.35f;
            }
            case GOLF -> {
                float v = kolom + rij - maat * 1.5f;
                kies = Mth.floor(v / 2f);
                fel = 0.6f + 0.4f * Mth.sin(v * Mth.PI / 3f);
            }
            case RINGEN -> {
                int ring = (int) Math.max(Math.abs(x), Math.abs(y));   // 0 (middle) .. 2 (edge)
                kies = ring - b;
                fel = Math.floorMod(ring - b, 3) == 0 ? 1f : 0.4f;
            }
            case REGEN -> {
                int h = Mth.murmurHash3Mixer(kolom * 31 + rij * 131 + b * 7919);
                boolean aan = Math.floorMod(h, 10) < 3;
                kies = h >> 8;
                fel = aan ? 1f - 0.6f * f : 0.15f;
            }
            case SPIRAAL -> {
                float hoek = (float) (Math.atan2(y, x) / (2 * Math.PI)) + 0.5f;
                float v = hoek * n * 2 + (float) Math.sqrt(x * x + y * y) * 0.6f - maat * 0.5f;
                kies = Mth.floor(v);
                fel = 0.5f + 0.5f * (v - Mth.floor(v));
            }
            case STREPEN -> {
                kies = rij + b;
                fel = ((kolom + b) & 1) == 0 ? 1f : 0.55f;
            }
            case KRUIS -> {
                boolean plus = kolom == 2 || kolom == 3 || rij == 2 || rij == 3;
                boolean kruis = Math.abs(kolom - rij) <= 1 || Math.abs(kolom + rij - (RASTER - 1)) <= 1;
                boolean aan = (b & 1) == 0 ? plus : kruis;
                kies = aan ? b : b + 1;
                fel = aan ? 1f : 0.3f;
            }
            case HART -> {
                boolean hart = HART[rij].charAt(kolom) == 'X';
                kies = hart ? 0 : 1 + ((kolom + rij + b) & 1);
                fel = hart ? klop(f) : 0.3f + 0.15f * (((kolom + rij + b) & 1));
            }
            default -> {   // HARTSLAG
                kies = b;
                fel = klop(f);
            }
        }
        return dim(kleuren[Math.floorMod(kies, n)], fel);
    }

    /** Two quick beats and a rest: how bright a heartbeat is at this part (0..1) of the beat. */
    private static float klop(float f) {
        if (f < 0.12f) {
            return 1f;
        }
        if (f < 0.26f) {
            return 0.55f;
        }
        if (f < 0.4f) {
            return 0.95f;
        }
        return Mth.lerp((f - 0.4f) / 0.6f, 0.7f, 0.3f);
    }

    /** The colour, this bright (0..1), opaque. */
    public static int dim(int argb, float fel) {
        float k = Mth.clamp(fel, 0f, 1f);
        int r = (int) (((argb >> 16) & 0xFF) * k), g = (int) (((argb >> 8) & 0xFF) * k), bl = (int) ((argb & 0xFF) * k);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** Two shows are the same when their pattern, tempo and colours are (a record compares arrays by identity). */
    public boolean zelfde(LichtShow ander) {
        return patroon == ander.patroon && tempo == ander.tempo && java.util.Arrays.equals(kleuren, ander.kleuren);
    }
}
