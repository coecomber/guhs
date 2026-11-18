package nl.juiced.guhs.feature.guhwinter.kern;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.quest.Reisguh;

/**
 * 1.10.9 Guhwinter: "Winterplein" as a Reisguh destination while the event is on. It is a waypoint without a Reisguh
 * ({@link Reisguh#zetVastPunt}): every player gets it in their list when they log in (or click the chat hint), and it is
 * taken off the list at AFGELOPEN.
 * <p>
 * Where: the plein anchor {@code winterplein_reis} (data/guhs/guhwinter/ankers.json, {x, y, z, yaw}: where the
 * "Reisguh" would stand; you arrive 2 blocks in front of it, looking back) when plein has it, else a fixed spot at the
 * south side of the plaza, just north of the new portal, so you arrive at the plaza's edge looking north into it. The
 * {@code portaal} anchor is NOT used: arriving inside the portal would send you to the overworld.
 */
public final class Winterplein {
    /** Its id in the Reisguh list (fixed, so players keep it between restarts). */
    public static final UUID ID = UUID.nameUUIDFromBytes("guhs:guhwinter_winterplein".getBytes(StandardCharsets.UTF_8));
    /** The plein anchor that, when present, says where the point is. */
    public static final String ANKER = "winterplein_reis";
    /** Without the anchor: here (the plaza floor is y 30), facing south (yaw 0), so the arrival is 2 blocks south of it. */
    public static final BlockPos STANDAARD = new BlockPos(-300, 31, -662);
    public static final float STANDAARD_YAW = 0;

    private Winterplein() {
    }

    /** Puts the point where it belongs while the event is on (returns where), takes it away after (null). Idempotent. */
    @javax.annotation.Nullable
    public static BlockPos zorg(MinecraftServer server) {
        if (!Guhwinter.bezig(server)) {
            Reisguh.haalPuntWeg(server, ID);
            return null;
        }
        BlockPos pos = STANDAARD;
        float yaw = STANDAARD_YAW;
        JsonObject a = anker(server, ANKER);
        if (a != null && a.has("x") && a.has("y") && a.has("z")) {
            pos = new BlockPos(a.get("x").getAsInt(), a.get("y").getAsInt(), a.get("z").getAsInt());
            yaw = a.has("yaw") ? a.get("yaw").getAsFloat() : 0;
        }
        Reisguh.zetVastPunt(server, ID, Component.translatable("guhwinter.kern.reis.winterplein"), pos, yaw);
        return pos;
    }

    /** The player gets the Winterplein in their Reisguh list (true: it was new). */
    public static boolean ontdek(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        zorg(server);
        return Reisguh.ontdekVoor(server, player.getUUID(), ID);
    }

    /**
     * One anchor from plein's {@code data/guhs/guhwinter/ankers.json} (the names at the top level, or under "ankers"),
     * read straight from the data packs so kern does not depend on plein's classes. Null when missing.
     */
    static JsonObject anker(MinecraftServer server, String naam) {
        Optional<Resource> r = server.getResourceManager().getResource(Guhs.id("guhwinter/ankers.json"));
        if (r.isEmpty()) {
            return null;
        }
        try (Reader in = r.get().openAsReader()) {
            JsonElement root = JsonParser.parseReader(in);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject o = root.getAsJsonObject();
            if (o.has("ankers") && o.get("ankers").isJsonObject()) {
                o = o.getAsJsonObject("ankers");
            }
            return o.has(naam) && o.get(naam).isJsonObject() ? o.getAsJsonObject(naam) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
