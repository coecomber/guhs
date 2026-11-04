package nl.juiced.guhs.feature.vadsrijk.kern.veilig;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * The safe spots of the Vadsrijk for the unstuck key ("losknuffelen"): walkable floors on the main network of the map.
 * <ul>
 *     <li>the generated list {@code data/guhs/vadsrijk/veilige_plekken.json} (written by {@code tools/vadsrijk/audit/audit.py}
 *     from the real tiles: one spot per 8x8 columns of the network a player can walk to AND back from a campfire; soort 1 = on a path);</li>
 *     <li>always also the campfires and the main POIs of {@link WereldPlekken} (soort 2), so it works without the json too.</li>
 * </ul>
 * {@link #kies} is pure (the world check comes in as a predicate) so a gametest can check the choice.
 */
public final class VeiligePlekken {
    public static final int GRID = 0, PAD = 1, POI = 2;
    /** Search radius around the player (blocks). */
    public static final int STRAAL = 64;
    /** How many of the nearest candidates are checked against the real world (each check reads a few blocks). */
    static final int MAX_CONTROLES = 32;

    public record Plek(BlockPos pos, int soort) {
        /** Lower = better: the distance, with paths and campfires/POIs a little "closer". */
        double score(Vec3 van) {
            double d = Math.sqrt(pos.distToCenterSqr(van.x, van.y, van.z));
            return d * (soort == POI ? 0.7 : soort == PAD ? 0.85 : 1.0);
        }
    }

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final int CEL = 32;
    @Nullable
    private static volatile Index index;

    private record Index(List<Plek> alle, Map<Long, List<Plek>> cellen) {
    }

    private VeiligePlekken() {
    }

    /** All spots (loaded on first use; {@link #vergeet} after a datapack reload). */
    public static List<Plek> alle(@Nullable MinecraftServer server) {
        return idx(server).alle;
    }

    public static void vergeet() {
        index = null;
    }

    /** The spots within {@code straal} blocks (2D) of a position. */
    public static List<Plek> rond(@Nullable MinecraftServer server, Vec3 v, int straal) {
        Index ix = idx(server);
        List<Plek> uit = new ArrayList<>();
        int cx0 = Math.floorDiv((int) Math.floor(v.x) - straal, CEL), cx1 = Math.floorDiv((int) Math.floor(v.x) + straal, CEL);
        int cz0 = Math.floorDiv((int) Math.floor(v.z) - straal, CEL), cz1 = Math.floorDiv((int) Math.floor(v.z) + straal, CEL);
        long r2 = (long) straal * straal;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                for (Plek p : ix.cellen.getOrDefault(sleutel(cx, cz), List.of())) {
                    double dx = p.pos.getX() + 0.5 - v.x, dz = p.pos.getZ() + 0.5 - v.z;
                    if (dx * dx + dz * dz <= r2) {
                        uit.add(p);
                    }
                }
            }
        }
        return uit;
    }

    /**
     * The best safe spot for a player at {@code van}: the candidates sorted by {@link Plek#score} (nearest first; paths and
     * campfires/POIs preferred), spots right where the player already is skipped (that is where they are stuck), the first
     * one that {@code geldig} accepts wins. Null = none (the caller falls back to the last campfire).
     */
    @Nullable
    public static BlockPos kies(List<Plek> kandidaten, Vec3 van, Predicate<Plek> geldig) {
        List<Plek> l = new ArrayList<>(kandidaten);
        l.sort(Comparator.comparingDouble(p -> p.score(van)));
        int gecontroleerd = 0;
        for (Plek p : l) {
            double dx = p.pos.getX() + 0.5 - van.x, dz = p.pos.getZ() + 0.5 - van.z, dy = p.pos.getY() - van.y;
            if (dx * dx + dz * dz < 2.5 * 2.5 && Math.abs(dy) < 2) {
                continue;
            }
            if (++gecontroleerd > MAX_CONTROLES) {
                break;
            }
            if (geldig.test(p)) {
                return p.pos;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ loading
    private static Index idx(@Nullable MinecraftServer server) {
        Index ix = index;
        if (ix == null) {
            ix = laad(server);
            index = ix;
        }
        return ix;
    }

    private static long sleutel(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    private static Index laad(@Nullable MinecraftServer server) {
        List<Plek> alle = new ArrayList<>(vasteplekken());
        int json = 0;
        if (server != null) {
            var res = server.getResourceManager().getResource(Guhs.id("vadsrijk/veilige_plekken.json"));
            if (res.isPresent()) {
                try (Reader r = res.get().openAsReader()) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    JsonArray a = o.getAsJsonArray("plekken");
                    for (int i = 0; i + 3 < a.size(); i += 4) {
                        alle.add(new Plek(new BlockPos(a.get(i).getAsInt(), a.get(i + 1).getAsInt(), a.get(i + 2).getAsInt()), a.get(i + 3).getAsInt() == 1 ? PAD : GRID));
                        json++;
                    }
                } catch (Exception e) {
                    LOG.warn("Vadsrijk: veilige_plekken.json not readable: {}", e.toString());
                }
            }
        }
        Map<Long, List<Plek>> cellen = new HashMap<>();
        for (Plek p : alle) {
            cellen.computeIfAbsent(sleutel(Math.floorDiv(p.pos.getX(), CEL), Math.floorDiv(p.pos.getZ(), CEL)), k -> new ArrayList<>()).add(p);
        }
        LOG.info("Vadsrijk: {} safe spots ({} from veilige_plekken.json)", alle.size(), json);
        return new Index(List.copyOf(alle), cellen);
    }

    /** The campfires (a stand spot next to them is found by the world check) and the main meeting points of the map. */
    static List<Plek> vasteplekken() {
        List<Plek> l = new ArrayList<>();
        for (BlockPos k : WereldPlekken.KAMPVUREN) {
            for (BlockPos b : new BlockPos[] {k.south(2), k.north(2), k.east(2), k.west(2)}) {
                l.add(new Plek(b, POI));
            }
        }
        for (var e : WereldPlekken.ALLE.entrySet()) {
            String id = e.getKey();
            if (id.startsWith("ridder_") || id.startsWith("poorten.") || id.startsWith("terugportaal_") || id.equals("plein")
                    || id.equals("haven_plein") || id.equals("aankomst") || id.equals("aankomst_trap_top") || id.equals("steiger_eind")
                    || id.equals("questbord") || id.endsWith("_voet") || (id.startsWith("gebouw.") && id.endsWith(".deur"))) {
                l.add(new Plek(e.getValue(), POI));
            }
        }
        return l;
    }
}
