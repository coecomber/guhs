package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The explicit target of an objective ({@link Quest.Doel#plek()}, zone 2 onwards): "npc:&lt;id&gt;", "poi:&lt;id&gt;",
 * "gebied:&lt;area&gt;" or "baas:&lt;area&gt;". Where it is ({@link #pos}) and the "where" phrase for it ({@link #tekstKey}:
 * {@code vadsrijk.npc.plek.<npc|poi|gebied|baas>.<id>}). The quest tracker (fix round 1, QuestStap) prefers this over what it
 * derives from the objective type when it is set. Other zones fill {@link #POI} / {@link #GEBIED} / {@link #BAAS} with their spots
 * (zone 1's POIs are in NpcPlekken.POI and are found too).
 */
public final class QuestPlek {
    public static final Map<String, BlockPos> POI = new ConcurrentHashMap<>();
    public static final Map<String, BlockPos> GEBIED = new ConcurrentHashMap<>();
    public static final Map<String, BlockPos> BAAS = new ConcurrentHashMap<>();

    private QuestPlek() {
    }

    /** Where this target is now (an NPC: the live guh when loaded, else its spot); null when unknown or "". */
    @Nullable
    public static BlockPos pos(@Nullable ServerLevel sl, String plek) {
        int c = plek.indexOf(':');
        if (c < 0) {
            return null;
        }
        String id = plek.substring(c + 1);
        return switch (plek.substring(0, c)) {
            case "npc" -> {
                RijkGuh g = sl == null ? null : Npcs.levend(sl, id);
                NpcDef d = Npcs.van(id);
                yield g != null ? g.blockPosition() : d == null ? null : d.plek();
            }
            case "poi" -> POI.containsKey(id) ? POI.get(id) : NpcPlekken.poi(id);
            case "gebied" -> GEBIED.get(id);
            case "baas" -> BAAS.get(id);
            default -> null;
        };
    }

    /** The lang key of the "where" phrase ("bij ... in ..."). */
    public static String tekstKey(String plek) {
        int c = plek.indexOf(':');
        return c < 0 ? "vadsrijk.npc.plek.overal" : "vadsrijk.npc.plek." + plek.substring(0, c) + "." + plek.substring(c + 1);
    }
}
