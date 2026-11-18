package nl.juiced.guhs.feature.guhwinter.tekenen.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekenPayloads;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekenPixels;

/**
 * The drawings this client has seen, fetched by id on demand ({@link TekenPayloads.Vraag}) and kept as a dynamic texture:
 * the 64x64 picture inside a small wooden frame with pink corners, {@link #B} x {@link #B} pixels (the picture at
 * {@link #RAND},{@link #RAND}). Paper shows where nothing is drawn. At most {@link #MAX} drawings (the least recently used
 * go first); an edited drawing is dropped when the server says so ({@link TekenPayloads.Vergeet}) and fetched again.
 */
public final class TekeningCache {
    public static final int RAND = 4, B = TekenPixels.W + 2 * RAND;
    /** The empty frame (shown while a drawing is on its way, or for an unknown one). */
    public static final Identifier LEEG = Guhs.id("textures/entity/guhwinter_tekening_leeg.png");
    private static final int MAX = 192;
    private static final long OPNIEUW_MS = 8_000, ONBEKEND_OPNIEUW_MS = 60_000;

    public static final class Entry {
        public final Identifier tex;
        public final String maker;
        public final int[] pixels;

        Entry(Identifier tex, String maker, int[] pixels) {
            this.tex = tex;
            this.maker = maker;
            this.pixels = pixels;
        }
    }

    private static final LinkedHashMap<UUID, Entry> CACHE = new LinkedHashMap<>(64, 0.75f, true);
    /** id -> when it was asked (or found unknown): not asked again before the pause is over. */
    private static final Map<UUID, Long> GEVRAAGD = new HashMap<>();
    private static final Map<UUID, Long> ONBEKEND = new HashMap<>();

    private TekeningCache() {
    }

    /** The drawing, or null while it is being fetched (asks the server once in a while). */
    @Nullable
    public static Entry get(UUID id) {
        Entry e = CACHE.get(id);
        if (e != null) {
            return e;
        }
        long nu = System.currentTimeMillis();
        Long o = ONBEKEND.get(id);
        if (o != null && nu - o < ONBEKEND_OPNIEUW_MS) {
            return null;
        }
        Long g = GEVRAAGD.get(id);
        if ((g == null || nu - g > OPNIEUW_MS) && Minecraft.getInstance().getConnection() != null) {
            GEVRAAGD.put(id, nu);
            ClientPacketDistributor.sendToServer(new TekenPayloads.Vraag(id));
        }
        return null;
    }

    /** The texture to show for this drawing (the empty frame while it isn't there). */
    public static Identifier tex(@Nullable UUID id) {
        Entry e = id == null ? null : get(id);
        return e == null ? LEEG : e.tex;
    }

    public static void ontvang(UUID id, String maker, byte[] data) {
        GEVRAAGD.remove(id);
        int[] px = data.length == 0 ? null : TekenPixels.pakUit(data);
        if (px == null) {
            ONBEKEND.put(id, System.currentTimeMillis());
            return;
        }
        ONBEKEND.remove(id);
        weg(id);
        Identifier texId = Guhs.id("dynamic/guhwinter_tekening/" + id);
        Minecraft.getInstance().getTextureManager().register(texId, new DynamicTexture(() -> "guhs guhwinter tekening", beeld(px)));
        CACHE.put(id, new Entry(texId, maker, px));
        while (CACHE.size() > MAX) {
            Iterator<UUID> it = CACHE.keySet().iterator();
            UUID oudste = it.next();
            weg(oudste);
        }
    }

    public static void vergeet(UUID id) {
        weg(id);
        GEVRAAGD.remove(id);
        ONBEKEND.remove(id);
    }

    private static void weg(UUID id) {
        Entry e = CACHE.remove(id);
        if (e != null) {
            Minecraft.getInstance().getTextureManager().release(e.tex);
        }
    }

    public static void wis() {
        for (UUID id : CACHE.keySet().toArray(new UUID[0])) {
            weg(id);
        }
        GEVRAAGD.clear();
        ONBEKEND.clear();
    }

    // ------------------------------------------------------------------------------------------------ the framed picture

    static final int HOUT_DONKER = 0xFF6B4226, HOUT = 0xFFA8733F, HOUT_LICHT = 0xFFC08A52, ROZE = 0xFFF7A8C8, ROZE_HOEK = 0xFFFF7EB6;

    /** The framed picture (ARGB) of these pixels. */
    public static NativeImage beeld(int[] px) {
        NativeImage img = new NativeImage(B, B, false);
        for (int y = 0; y < B; y++) {
            for (int x = 0; x < B; x++) {
                img.setPixel(x, y, lijst(x, y));
            }
        }
        for (int y = 0; y < TekenPixels.H; y++) {
            for (int x = 0; x < TekenPixels.W; x++) {
                img.setPixel(RAND + x, RAND + y, TekenPixels.toon(px[y * TekenPixels.W + x]));
            }
        }
        return img;
    }

    /** The frame's pixel at x, y (only the outer {@link #RAND} pixels matter): dark rim, wood with grain, a pink inner line, pink corners. */
    static int lijst(int x, int y) {
        int r = Math.min(Math.min(x, y), Math.min(B - 1 - x, B - 1 - y));
        boolean hoek = (x < RAND || x >= B - RAND) && (y < RAND || y >= B - RAND);
        if (r == 0) {
            return HOUT_DONKER;
        }
        if (hoek) {
            return r == 1 ? ROZE_HOEK : ROZE;
        }
        if (r == RAND - 1) {
            return ROZE;
        }
        int nerf = (x * 7 + y * 13) % 11;
        return nerf < 2 ? HOUT_LICHT : nerf == 5 ? HOUT_DONKER : HOUT;
    }
}
