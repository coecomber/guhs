package nl.juiced.guhs.feature.guhwinter.npc;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;

/**
 * Guhwinter (npc slice): the plein slice's named positions ({@code data/guhs/guhwinter/ankers.json}, name to
 * {@code {x, y, z, yaw}}), read here directly because plein's {@code GuhwinterAnkers} did not exist yet in this branch.
 * Missing file or name: null (the NPC then uses its fallback spot inside the plaza, see {@link NpcDefs}).
 * <p>Since the 1.10.9 integration {@link #bron} reads plein's {@code GuhwinterAnkers}; the json reader stays as a fall-back.
 */
public final class NpcAnkers {
    /** One anchor: where (block-centred when the file gives whole numbers) and which way it looks. */
    public record Anker(Vec3 pos, float yaw) {
    }

    /** Where anchors come from; the default reads the json itself. Replace at integration (e.g. with GuhwinterAnkers). */
    public interface Bron {
        @Nullable
        Anker get(MinecraftServer server, String naam);
    }

    /** 1.10.9 integration: plein's {@link nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers} (no log line for a missing name). */
    public static Bron bron = NpcAnkers::uitPlein;

    @Nullable
    private static Anker uitPlein(MinecraftServer server, String naam) {
        nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers.Anker a = nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers.zoek(naam);
        if (a == null) {
            return null;
        }
        double x = a.x(), z = a.z();
        if (x == Math.floor(x)) {   // whole numbers are a block: stand in its middle (as the json reader below)
            x += 0.5;
        }
        if (z == Math.floor(z)) {
            z += 0.5;
        }
        return new Anker(new Vec3(x, a.y(), z), a.yaw());
    }
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final Map<String, Anker> CACHE = new HashMap<>();
    @Nullable
    private static ResourceManager gelezenVan;

    private NpcAnkers() {
    }

    @Nullable
    public static Anker get(MinecraftServer server, String naam) {
        return bron.get(server, naam);
    }

    private static synchronized Anker uitJson(MinecraftServer server, String naam) {
        ResourceManager rm = server.getResourceManager();
        if (rm != gelezenVan) {            // (first time, or after /reload: read the file again)
            gelezenVan = rm;
            CACHE.clear();
            lees(rm);
        }
        return CACHE.get(naam);
    }

    private static void lees(ResourceManager rm) {
        Optional<Resource> r = rm.getResource(Guhs.id("guhwinter/ankers.json"));
        if (r.isEmpty()) {
            return;
        }
        try (Reader in = r.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(in).getAsJsonObject();
            if (root.has("ankers") && root.get("ankers").isJsonObject()) {
                root = root.getAsJsonObject("ankers");
            }
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject o = e.getValue().getAsJsonObject();
                if (!o.has("x") || !o.has("y") || !o.has("z")) {
                    continue;
                }
                double x = o.get("x").getAsDouble(), y = o.get("y").getAsDouble(), z = o.get("z").getAsDouble();
                // whole numbers are a block: stand in its middle
                if (x == Math.floor(x)) {
                    x += 0.5;
                }
                if (z == Math.floor(z)) {
                    z += 0.5;
                }
                float yaw = o.has("yaw") ? o.get("yaw").getAsFloat() : 0f;
                CACHE.put(e.getKey(), new Anker(new Vec3(x, y, z), yaw));
            }
        } catch (Exception ex) {
            LOGGER.warn("guhwinter: could not read ankers.json for the NPCs: {}", ex.toString());
        }
    }
}
