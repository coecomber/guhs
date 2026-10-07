package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;
import org.slf4j.Logger;

/**
 * The arena allocator. Arenas are built far from the lobby on a grid: cell k has its min corner at
 * (4096 + 512 * (k % 64), 64, 512 * (k / 64)) ({@link Guhpixel#celOorsprong}); a game takes a cell ({@link #neem}), plays
 * there alone and gives it back ({@link #geefTerug}), after which the same cell serves the next game of that kind. Any
 * number of games run at once, one cell each.
 * <p>
 * The grid (which cell holds which kind of arena) is saved ({@code guhs:px_arenas}); which cells are in use is not: after a
 * server start every cell is free. A cell that was in use when the world was last saved (a crash) is written off and never
 * used again, and stale entities that load from disk in the arena region are removed ({@link Regels}), because no game
 * survives a restart. While a cell is in use its chunks stay loaded (a ticket), so nothing in it unloads half-way.
 */
public final class Arenas {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ArenaSoort> SOORTEN = new LinkedHashMap<>();
    /** Cells in use right now. */
    private static final Set<Integer> BEZET = new HashSet<>();
    static final String AFGESCHREVEN = "-";

    public static void registreer(ArenaSoort soort) {
        SOORTEN.put(soort.id(), soort);
    }

    @Nullable
    public static ArenaSoort soort(String id) {
        return SOORTEN.get(id);
    }

    public static List<ArenaSoort> soorten() {
        return List.copyOf(SOORTEN.values());
    }

    /** A free built cell of this kind (already cleaned), or a newly stamped cell. Null when the template is missing. */
    @Nullable
    public static synchronized Arena neem(ServerLevel level, ArenaSoort soort) {
        Raster raster = Raster.get(level.getServer());
        int cel = vrijeCel(raster.cellen, BEZET, soort.id());
        boolean nieuw = cel == raster.cellen.size();
        Arena a = new Arena(cel, soort, level, Guhpixel.celOorsprong(cel));
        ticket(a, true);
        if (nieuw) {
            if (!Stempel.plaats(level, soort.template(), a.oorsprong())) {
                ticket(a, false);
                return null;
            }
            raster.cellen.add(soort.id());
        }
        BEZET.add(cel);
        raster.bezet.add(cel);
        raster.setDirty();
        return a;
    }

    /** Pure allocator maths: the lowest free cell that already holds this kind, else the next new cell index. */
    static int vrijeCel(List<String> cellen, Set<Integer> bezet, String soortId) {
        for (int k = 0; k < cellen.size(); k++) {
            if (soortId.equals(cellen.get(k)) && !bezet.contains(k)) {
                return k;
            }
        }
        return cellen.size();
    }

    /** The game is over: removes every non-player entity in the box, then herstel / herstempel; the cell is free again. */
    public static synchronized void geefTerug(Arena a) {
        ServerLevel level = a.level();
        Stempel.laad(level, a.oorsprong(), a.soort().maat());
        Stempel.ruim(level, a.doos().inflate(2));
        try {
            if (a.soort().herstempel()) {
                Stempel.leeg(level, a.oorsprong(), a.soort().maat());
                Stempel.ruim(level, a.doos().inflate(2));   // (drops of what was cleared)
                Stempel.plaats(level, a.soort().template(), a.oorsprong());
            } else {
                a.soort().herstel().accept(a);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Guhpixel: cleaning arena {} (cell {}) failed", a.soort().id(), a.cel(), e);
        }
        if (a.cel() >= 0) {
            ticket(a, false);
            BEZET.remove(a.cel());
            Raster raster = Raster.get(level.getServer());
            raster.bezet.remove(a.cel());
            raster.setDirty();
        }
    }

    /** (Tests) stamps an arena at a given spot, outside the grid. Give it back with {@link #geefTerug} all the same. */
    @Nullable
    public static Arena neemOp(ServerLevel level, BlockPos oorsprong, ArenaSoort soort) {
        Arena a = new Arena(-1, soort, level, oorsprong);
        return Stempel.plaats(level, soort.template(), oorsprong) ? a : null;
    }

    private static void ticket(Arena a, boolean aan) {
        if (a.cel() < 0) {
            return;
        }
        BlockPos midden = a.oorsprong().offset(a.soort().maat().getX() / 2, 0, a.soort().maat().getZ() / 2);
        int straal = Math.max(a.soort().maat().getX(), a.soort().maat().getZ()) / 32 + 2;
        var bron = a.level().getChunkSource();
        if (aan) {
            bron.addTicketWithRadius(GuhpixelFeature.TICKET.get(), ChunkPos.containing(midden), straal);
        } else {
            bron.removeTicketWithRadius(GuhpixelFeature.TICKET.get(), ChunkPos.containing(midden), straal);
        }
    }

    /** Is this cell in use by a game right now? */
    public static synchronized boolean bezet(int cel) {
        return BEZET.contains(cel);
    }

    /** (Dev) the grid as lines "cell: kind (in use)". */
    public static synchronized List<String> lijst(MinecraftServer server) {
        Raster raster = Raster.get(server);
        List<String> out = new ArrayList<>();
        for (int k = 0; k < raster.cellen.size(); k++) {
            BlockPos o = Guhpixel.celOorsprong(k);
            out.add(k + ": " + raster.cellen.get(k) + " @ " + o.getX() + " " + o.getY() + " " + o.getZ() + (BEZET.contains(k) ? " (bezet)" : ""));
        }
        return out;
    }

    /** (Dev) writes every free cell off, so the next games get freshly stamped arenas. Returns how many. */
    public static synchronized int ruim(MinecraftServer server) {
        Raster raster = Raster.get(server);
        int n = 0;
        for (int k = 0; k < raster.cellen.size(); k++) {
            if (!BEZET.contains(k) && !AFGESCHREVEN.equals(raster.cellen.get(k))) {
                raster.cellen.set(k, AFGESCHREVEN);
                n++;
            }
        }
        raster.setDirty();
        return n;
    }

    /** Server start and stop: nothing is in use. */
    static synchronized void opStart(MinecraftServer server) {
        BEZET.clear();
        Raster raster = Raster.get(server);
        // a cell that was in use when the world was last saved: a crash left it dirty, never use it again
        for (int cel : raster.bezet) {
            if (cel >= 0 && cel < raster.cellen.size()) {
                raster.cellen.set(cel, AFGESCHREVEN);
            }
        }
        if (!raster.bezet.isEmpty()) {
            raster.bezet.clear();
            raster.setDirty();
        }
    }

    static synchronized void opStop() {
        BEZET.clear();
    }

    /** The saved grid. */
    static final class Raster extends SavedData {
        static final SavedDataType<Raster> TYPE = GuhSavedData.tagType("px_arenas", Raster::new, Raster::load, Raster::save);
        /** Per cell the kind of arena that stands there ({@link #AFGESCHREVEN}: written off). */
        final List<String> cellen = new ArrayList<>();
        /** Cells in use (saved, so a crash is noticed at the next start). */
        final Set<Integer> bezet = new HashSet<>();

        static Raster get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        }

        private CompoundTag save() {
            CompoundTag out = new CompoundTag();
            ListTag l = new ListTag();
            for (String s : cellen) {
                l.add(StringTag.valueOf(s));
            }
            out.put("Cellen", l);
            out.putIntArray("Bezet", bezet.stream().mapToInt(Integer::intValue).toArray());
            return out;
        }

        private static Raster load(CompoundTag tag) {
            Raster r = new Raster();
            ListTag l = tag.getListOrEmpty("Cellen");
            for (int i = 0; i < l.size(); i++) {
                r.cellen.add(l.getStringOr(i, AFGESCHREVEN));
            }
            for (int cel : tag.getIntArray("Bezet").orElse(new int[0])) {
                r.bezet.add(cel);
            }
            return r;
        }
    }

    private Arenas() {
    }
}
