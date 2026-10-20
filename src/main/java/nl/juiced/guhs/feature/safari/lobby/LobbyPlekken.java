package nl.juiced.guhs.feature.safari.lobby;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * Where the lobby's characters stand and where its chores happen: {@code data/guhs/safari/lobby.json}, written by the park
 * generator (tools/features/safari_park/zone_s2_lobby.py, on top of the entreedorp of zone_entree). Read once from the jar.
 */
public final class LobbyPlekken {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** One character: its id ("hoofdranger", "expert_savanne"...), kind, standing block, heading, and (experts) zone + counter. */
    public record Npc(String id, GuhNpcEntity.Kind kind, BlockPos pos, float yaw, @Nullable String zone, @Nullable BlockPos balie) {
    }

    /** An aaihoek pen (inclusive corners, any y). */
    public record Pen(String soort, int x1, int z1, int x2, int z2) {
        public boolean binnen(double x, double z, double marge) {
            return x >= x1 - marge && x <= x2 + 1 + marge && z >= z1 - marge && z <= z2 + 1 + marge;
        }

        public BlockPos midden(int y) {
            return new BlockPos((x1 + x2) / 2, y, (z1 + z2) / 2);
        }
    }

    public final List<Npc> npcs;
    public final List<Pen> pennen;
    public final List<BlockPos> alpacaLus;
    public final AABB lodge;
    public final BlockPos plein;
    public final int pleinStraal;

    private static volatile LobbyPlekken data;

    private LobbyPlekken(List<Npc> npcs, List<Pen> pennen, List<BlockPos> alpacaLus, AABB lodge, BlockPos plein, int pleinStraal) {
        this.npcs = npcs;
        this.pennen = pennen;
        this.alpacaLus = alpacaLus;
        this.lodge = lodge;
        this.plein = plein;
        this.pleinStraal = pleinStraal;
    }

    public static LobbyPlekken get() {
        LobbyPlekken d = data;
        if (d == null) {
            synchronized (LobbyPlekken.class) {
                if (data == null) {
                    data = laad();
                }
                d = data;
            }
        }
        return d;
    }

    @Nullable
    public Npc npc(String id) {
        for (Npc n : npcs) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return null;
    }

    @Nullable
    public Pen pen(String soort) {
        for (Pen p : pennen) {
            if (p.soort().equals(soort)) {
                return p;
            }
        }
        return null;
    }

    private static BlockPos pos(JsonElement e) {
        JsonArray a = e.getAsJsonArray();
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static LobbyPlekken laad() {
        List<Npc> npcs = new ArrayList<>();
        List<Pen> pennen = new ArrayList<>();
        List<BlockPos> lus = new ArrayList<>();
        AABB lodge = new AABB(0, 0, 0, 0, 0, 0);
        BlockPos plein = BlockPos.ZERO;
        int straal = 20;
        try (InputStream in = LobbyPlekken.class.getResourceAsStream("/data/guhs/safari/lobby.json")) {
            if (in == null) {
                LOGGER.warn("Safari lobby: no lobby.json, the lobby has no characters");
                return new LobbyPlekken(npcs, pennen, lus, lodge, plein, straal);
            }
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement e : o.getAsJsonArray("npcs")) {
                JsonObject n = e.getAsJsonObject();
                GuhNpcEntity.Kind kind;
                try {
                    kind = GuhNpcEntity.Kind.valueOf(n.get("kind").getAsString().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    LOGGER.warn("Safari lobby: unknown NPC kind {}", n.get("kind"));
                    continue;
                }
                npcs.add(new Npc(n.get("id").getAsString(), kind, pos(n.get("pos")), n.get("yaw").getAsFloat(),
                        n.has("zone") ? n.get("zone").getAsString() : null, n.has("balie") ? pos(n.get("balie")) : null));
            }
            for (JsonElement e : o.getAsJsonArray("pennen")) {
                JsonObject p = e.getAsJsonObject();
                JsonArray a = p.getAsJsonArray("min"), b = p.getAsJsonArray("max");
                pennen.add(new Pen(p.get("soort").getAsString(), a.get(0).getAsInt(), a.get(1).getAsInt(), b.get(0).getAsInt(), b.get(1).getAsInt()));
            }
            for (JsonElement e : o.getAsJsonArray("alpaca_lus")) {
                JsonArray a = e.getAsJsonArray();
                lus.add(new BlockPos(a.get(0).getAsInt(), 65, a.get(1).getAsInt()));
            }
            JsonObject l = o.getAsJsonObject("lodge");
            BlockPos a = pos(l.get("min")), b = pos(l.get("max"));
            lodge = new AABB(a.getX(), a.getY(), a.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1);
            JsonObject pl = o.getAsJsonObject("plein");
            plein = pos(pl.get("centrum"));
            straal = pl.get("straal").getAsInt();
        } catch (Exception e) {
            LOGGER.error("Safari lobby: could not read lobby.json", e);
        }
        return new LobbyPlekken(npcs, pennen, lus, lodge, plein, straal);
    }

    /** (Tests) forget the cached data. */
    static synchronized void vergeet() {
        data = null;
    }
}
