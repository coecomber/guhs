package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.io.Reader;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import nl.juiced.guhs.Guhs;
import org.slf4j.Logger;

/**
 * The local anchor adapter of the tekenen slice: the kerstboom position from the plein's {@code data/guhs/guhwinter/ankers.json}
 * ({@code {name: {x, y, z, yaw}}}). INTEGRATION: replace {@link #kerstboom} with {@code GuhwinterAnkers.get("kerstboom")}.
 * Read once per minute at most (cheap; a /reload with a new plaza is picked up).
 */
public final class TekenPlek {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long VERS_MS = 60_000;
    @Nullable
    private static BlockPos boom;
    private static long gelezen = Long.MIN_VALUE;
    private static boolean gemeld;

    private TekenPlek() {
    }

    /** The kerstboom anchor (in guhs:guhmension), or null when the plaza has none (yet). */
    @Nullable
    public static BlockPos kerstboom(MinecraftServer server) {
        long nu = System.currentTimeMillis();
        if (nu - gelezen > VERS_MS) {
            gelezen = nu;
            boom = lees(server, "kerstboom");
        }
        return boom;
    }

    public static void vergeet() {
        gelezen = Long.MIN_VALUE;
    }

    @Nullable
    private static BlockPos lees(MinecraftServer server, String naam) {
        Optional<Resource> res = server.getResourceManager().getResource(Guhs.id("guhwinter/ankers.json"));
        if (res.isEmpty()) {
            return null;
        }
        try (Reader r = res.get().openAsReader()) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject o = root.getAsJsonObject();
            if (!o.has(naam) && o.has("ankers") && o.get("ankers").isJsonObject()) {
                o = o.getAsJsonObject("ankers");   // {"ankers": {name: {...}}} works too
            }
            if (!o.has(naam)) {
                return null;
            }
            JsonObject a = o.getAsJsonObject(naam);
            return BlockPos.containing(a.get("x").getAsDouble(), a.get("y").getAsDouble(), a.get("z").getAsDouble());
        } catch (Exception e) {
            if (!gemeld) {
                gemeld = true;
                LOGGER.warn("Guhwinter tekenen: can't read anchor {} from guhwinter/ankers.json", naam, e);
            }
            return null;
        }
    }
}
