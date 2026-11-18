package nl.juiced.guhs.feature.guhwinter.plein;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 1.10.9 Guhwinter: the named spots of the winter square (data/guhs/guhwinter/ankers.json, made by
 * tools/features/guhwinter_plein.py), in world coordinates of guhs:guhmension. x/z are block centres where that matters
 * (npc spots, chairs), y is the standing level (the block a guh stands in). See guhs_winter/CONTRACT.md (plein) for the names.
 * <p>
 * Read from the mod jar itself (like the dream routes), so it works on every server and in the tests; never changes at runtime.
 */
public final class GuhwinterAnkers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PAD = "/data/guhs/guhwinter/ankers.json";
    private static volatile Map<String, Anker> ankers;

    /** A spot: position and the way to look (Minecraft yaw: 0 south, 90 west, 180 north, -90 east). */
    public record Anker(String naam, double x, double y, double z, float yaw) {
        /** The block the spot is in. */
        public BlockPos pos() {
            return BlockPos.containing(x, y, z);
        }

        public Vec3 vec() {
            return new Vec3(x, y, z);
        }

        /** (outside the square, on natural ground) the same x/z on the surface as it is now. */
        public BlockPos opGrond(ServerLevel level) {
            BlockPos p = pos();
            level.getChunk(p);
            return new BlockPos(p.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()), p.getZ());
        }
    }

    private GuhwinterAnkers() {
    }

    /** The spot with this name, or null (logged once per name). */
    @Nullable
    public static Anker get(String naam) {
        Anker a = alle().get(naam);
        if (a == null) {
            LOGGER.warn("Guhwinter: no anchor named '{}'", naam);
        }
        return a;
    }

    /** The spot with this name; an unknown name is a programming error. */
    public static Anker vereist(String naam) {
        Anker a = alle().get(naam);
        if (a == null) {
            throw new IllegalArgumentException("Guhwinter: no anchor named '" + naam + "'");
        }
        return a;
    }

    /** Every spot named prefix0, prefix1, ... in order (e.g. "wandel_", "pepernoot_verstop_"). */
    public static List<Anker> reeks(String prefix) {
        List<Anker> uit = new ArrayList<>();
        for (int i = 0; ; i++) {
            Anker a = alle().get(prefix + i);
            if (a == null) {
                return uit;
            }
            uit.add(a);
        }
    }

    /** All spots by name. */
    public static Map<String, Anker> alle() {
        Map<String, Anker> m = ankers;
        if (m == null) {
            synchronized (GuhwinterAnkers.class) {
                if (ankers == null) {
                    ankers = laad();
                }
                m = ankers;
            }
        }
        return m;
    }

    private static Map<String, Anker> laad() {
        Map<String, Anker> m = new LinkedHashMap<>();
        try (InputStream in = GuhwinterAnkers.class.getResourceAsStream(PAD)) {
            if (in == null) {
                LOGGER.error("Guhwinter: {} is missing", PAD);
                return Collections.unmodifiableMap(m);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : root.getAsJsonObject("ankers").entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                m.put(e.getKey(), new Anker(e.getKey(), o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble(),
                        o.has("yaw") ? o.get("yaw").getAsFloat() : 0f));
            }
        } catch (Exception e) {
            LOGGER.error("Guhwinter: could not read {}", PAD, e);
        }
        return Collections.unmodifiableMap(m);
    }
}
