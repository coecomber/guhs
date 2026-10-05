package nl.juiced.guhs.feature.vadskracht;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.registry.ModBlocks;
import org.slf4j.Logger;

/**
 * The vadskracht nets of one level (see {@link VadsKracht}). Nets are cached: every block of a net points at its
 * {@link VadsNet}, and a net is only rebuilt (a flood fill from the changed spot) when a wire or knoop is placed or removed,
 * when a chunk it ends at loads, or when a chunk it lies in unloads. Once per second each net is evaluated (the nets are
 * spread over the ticks of the second); a change of what a knoop gives or asks makes its net evaluate again the next tick.
 * Nothing is saved: after a restart the nets come back from the block entities of the chunks that load.
 */
final class VadsNetten {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<ServerLevel, VadsNetten> PER_WERELD = new ConcurrentHashMap<>();
    /** True while a net flips its own Guhdraad: the neighbour updates of that must not start a check. */
    static boolean bezig;

    static VadsNetten van(ServerLevel level) {
        return PER_WERELD.computeIfAbsent(level, VadsNetten::new);
    }

    static void vergeet(LevelAccessor level) {
        if (level instanceof ServerLevel server) {
            PER_WERELD.remove(server);
        }
    }

    static void vergeetAlles() {
        PER_WERELD.clear();
    }

    private final ServerLevel level;
    private final Long2ReferenceOpenHashMap<VadsNet> perPlek = new Long2ReferenceOpenHashMap<>();
    private final ReferenceLinkedOpenHashSet<VadsNet> netten = new ReferenceLinkedOpenHashSet<>();
    /** Spots to build a net from at the next tick, and nets that only have to be evaluated again. */
    private final LongLinkedOpenHashSet zaden = new LongLinkedOpenHashSet();
    private final ReferenceLinkedOpenHashSet<VadsNet> opnieuw = new ReferenceLinkedOpenHashSet<>();
    private final List<VadsNet> oud = new ArrayList<>();
    /** Chunks that loaded / unloaded since the last tick (events may come from the loading code: handled at the tick). */
    private final LongArrayList erbij = new LongArrayList(), weg = new LongArrayList();
    private final LongOpenHashSet netWeg = new LongOpenHashSet();
    /** What each consumer was told last. */
    private final Map<VadsVerbruiker, Boolean> verteld = new WeakHashMap<>();
    private long foutGemeld = Long.MIN_VALUE;

    private VadsNetten(ServerLevel level) {
        this.level = level;
    }

    // =====================================================================================================================
    // what is where
    // =====================================================================================================================

    static boolean geladen(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private boolean geladen(BlockPos pos) {
        return !netWeg.contains(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4)) && geladen(level, pos);
    }

    private boolean isDraad(BlockState state) {
        return state.is(ModBlocks.GUH_WIRE.get());
    }

    /** The knoop of the block here (null: none; Guhdraad is not a knoop). */
    @Nullable
    private VadsKnoop knoop(BlockPos pos, BlockState state) {
        return level.getCapability(VadsKracht.KNOOP, pos, state, null, null);
    }

    /** Is what the cache says about this spot still what stands there? */
    private boolean klopt(@Nullable VadsNet net, BlockPos pos) {
        if (!geladen(pos)) {
            return net == null;
        }
        BlockState state = level.getBlockState(pos);
        if (isDraad(state)) {
            return net != null && !net.knoopOp.containsKey(pos.asLong());
        }
        VadsKnoop k = knoop(pos, state);
        return net == null ? k == null : net.knoopOp.get(pos.asLong()) == k && k != null;
    }

    @Nullable
    VadsNet bekend(BlockPos pos) {
        return perPlek.get(pos.asLong());
    }

    // =====================================================================================================================
    // changes
    // =====================================================================================================================

    void veranderd(BlockPos pos) {
        VadsNet net = perPlek.get(pos.asLong());
        if (net != null && !net.vervallen && klopt(net, pos)) {
            opnieuw.add(net);   // the same blocks: only the numbers changed
            return;
        }
        herbouw(pos);
    }

    void herbouw(BlockPos pos) {
        verval(perPlek.get(pos.asLong()));
        zaden.add(pos.asLong());
        for (BlockPos buur : GuhWireBlock.connections(pos)) {
            zaden.add(buur.asLong());
        }
    }

    /** This net is no longer right: every block of it gets a new net at the next tick. */
    private void verval(@Nullable VadsNet net) {
        if (net == null || net.vervallen) {
            return;
        }
        net.vervallen = true;
        netten.remove(net);
        opnieuw.remove(net);
        oud.add(net);
        for (VadsKnoop k : net.knopen) {
            zaden.add(k.vadsPlek().asLong());
        }
        if (net.teGroot) {
            // (the pieces of one too big setup: they may fit in one net now)
            for (VadsNet ander : new ArrayList<>(netten)) {
                if (ander.teGroot && !ander.plekken.isEmpty()) {
                    zaden.add(ander.plekken.iterator().nextLong());
                    verval(ander);
                }
            }
        }
    }

    /** A piece of Guhdraad got a neighbour update: did a knoop appear or disappear next to it without telling us? */
    void controleer(BlockPos draad) {
        if (bezig) {
            return;
        }
        for (Direction d : Direction.values()) {
            BlockPos buur = draad.relative(d);
            VadsNet net = perPlek.get(buur.asLong());
            if (net != null && net.vervallen) {
                continue;
            }
            if (!klopt(net, buur)) {
                herbouw(buur);
            }
        }
    }

    void chunkErbij(ChunkPos chunk) {
        synchronized (erbij) {
            erbij.add(chunk.pack());
        }
    }

    void chunkWeg(ChunkPos chunk) {
        synchronized (erbij) {
            weg.add(chunk.pack());
        }
    }

    // =====================================================================================================================
    // the tick
    // =====================================================================================================================

    void tick() {
        chunks();
        if (!zaden.isEmpty() || !oud.isEmpty()) {
            bouwAlles();
        }
        if (!opnieuw.isEmpty()) {
            List<VadsNet> nu = new ArrayList<>(opnieuw);
            opnieuw.clear();
            for (VadsNet net : nu) {
                if (!net.vervallen) {
                    evalueer(net, false);
                }
            }
        }
        if (!netten.isEmpty()) {
            int fase = (int) Math.floorMod(level.getGameTime(), (long) VadsGetallen.TIK);
            List<VadsNet> nu = null;
            for (VadsNet net : netten) {
                if (net.fase == fase) {
                    (nu == null ? nu = new ArrayList<>() : nu).add(net);
                }
            }
            if (nu != null) {
                for (VadsNet net : nu) {
                    if (!net.vervallen) {
                        evalueer(net, true);
                    }
                }
            }
        }
        netWeg.clear();
    }

    private void chunks() {
        long[] nieuw, verdwenen;
        synchronized (erbij) {
            if (erbij.isEmpty() && weg.isEmpty()) {
                return;
            }
            nieuw = erbij.toLongArray();
            verdwenen = weg.toLongArray();
            erbij.clear();
            weg.clear();
        }
        for (long key : verdwenen) {
            if (level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) == null) {
                netWeg.add(key);
            }
            for (VadsNet net : new ArrayList<>(netten)) {
                if (net.chunks.contains(key)) {
                    verval(net);
                    zaden.addAll(net.plekken);
                }
            }
        }
        for (long key : nieuw) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk == null) {
                continue;
            }
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof VadsKnoop && !be.isRemoved() && !perPlek.containsKey(be.getBlockPos().asLong())) {
                    zaden.add(be.getBlockPos().asLong());
                }
            }
            for (VadsNet net : new ArrayList<>(netten)) {
                if (net.rand.contains(key)) {
                    verval(net);
                    if (!net.plekken.isEmpty()) {
                        zaden.add(net.plekken.iterator().nextLong());
                    }
                }
            }
        }
    }

    /** Builds a net from every waiting spot, drops what the old nets still claim, and evaluates the new nets. */
    private void bouwAlles() {
        List<VadsNet> nieuw = new ArrayList<>();
        for (int ronde = 0; ronde < 4 && !zaden.isEmpty(); ronde++) {
            long[] nu = zaden.toLongArray();
            zaden.clear();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (long key : nu) {
                VadsNet er = perPlek.get(key);
                if (er != null && !er.vervallen) {
                    continue;   // already in a net that is right (built a moment ago, or never changed)
                }
                p.set(key);
                VadsNet net = bouw(p.immutable());
                if (net != null) {
                    nieuw.add(net);
                }
            }
            // what an old net still claims: gone, or missed by the fill (then it gets a net of its own next round)
            for (VadsNet net : oud) {
                LongIterator it = net.plekken.iterator();
                while (it.hasNext()) {
                    long key = it.nextLong();
                    if (perPlek.get(key) == net) {
                        perPlek.remove(key);
                        zaden.add(key);
                    }
                }
            }
            oud.clear();
        }
        zaden.clear();
        for (VadsNet net : nieuw) {
            if (!net.vervallen) {
                evalueer(net, false);
            }
        }
    }

    /** Flood fill from this spot: the net of the Guhdraad or knoop here (null when there is neither). */
    @Nullable
    private VadsNet bouw(BlockPos start) {
        if (!geladen(start)) {
            return null;
        }
        BlockState startState = level.getBlockState(start);
        VadsKnoop startKnoop = isDraad(startState) ? null : knoop(start, startState);
        if (startKnoop == null && !isDraad(startState)) {
            return null;
        }
        VadsNet net = new VadsNet();
        net.fase = (int) Math.floorMod(start.asLong() * 31L + level.getGameTime(), (long) VadsGetallen.TIK);
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        neem(net, start, startKnoop);
        todo.add(start);
        while (!todo.isEmpty()) {
            BlockPos pos = todo.poll();
            VadsKnoop hier = net.knoopOp.get(pos.asLong());
            if (hier == null) {
                // Guhdraad: 6 faces + one step up or down diagonally to other Guhdraad; knopen through the 6 faces only
                List<BlockPos> buren = GuhWireBlock.connections(pos);
                for (int i = 0; i < buren.size(); i++) {
                    BlockPos buur = buren.get(i);
                    if (!mag(net, buur)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(buur);
                    if (isDraad(state)) {
                        if (!erbij(net, buur, null, todo)) {
                            return net;
                        }
                    } else if (i < 6) {
                        VadsKnoop k = knoop(buur, state);
                        if (k != null && k.vadsVerbindt(Direction.values()[i].getOpposite()) && !erbij(net, buur, k, todo)) {
                            return net;
                        }
                    }
                }
            } else {
                for (Direction d : Direction.values()) {
                    BlockPos buur = pos.relative(d);
                    if (!mag(net, buur)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(buur);
                    VadsKnoop k = isDraad(state) ? null : knoop(buur, state);
                    if (k == hier) {
                        if (!erbij(net, buur, k, todo)) {   // another block of the same machine
                            return net;
                        }
                    } else if (hier.vadsVerbindt(d) && (k != null ? k.vadsVerbindt(d.getOpposite()) : isDraad(state))) {
                        if (!erbij(net, buur, k, todo)) {
                            return net;
                        }
                    }
                }
            }
        }
        return net;
    }

    /**
     * May the fill look at this spot? Not when it is in the net already, not in an unloaded chunk (the net remembers the
     * chunks it ends at), and not when it belongs to a net that is too big: then this net is part of the same too big
     * setup and stands still as well.
     */
    private boolean mag(VadsNet net, BlockPos pos) {
        long key = pos.asLong();
        if (net.plekken.contains(key)) {
            return false;
        }
        if (!geladen(pos)) {
            if (!level.isOutsideBuildHeight(pos)) {
                net.rand.add(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
            }
            return false;
        }
        VadsNet was = perPlek.get(key);
        if (was != null && !was.vervallen && was.teGroot) {
            net.teGroot = true;
            return false;
        }
        return true;
    }

    /** Adds a block to the net being built; false = the net is full (too big). */
    private boolean erbij(VadsNet net, BlockPos pos, @Nullable VadsKnoop k, ArrayDeque<BlockPos> todo) {
        if (net.plekken.size() >= VadsGetallen.MAX_NET) {
            net.teGroot = true;
            return false;
        }
        neem(net, pos, k);
        todo.add(pos);
        return true;
    }

    private void neem(VadsNet net, BlockPos pos, @Nullable VadsKnoop k) {
        if (k == null) {
            net.draad(pos);
        } else {
            net.knoop(pos, k);
        }
        net.chunks.add(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
        VadsNet was = perPlek.put(pos.asLong(), net);
        if (was != null && was != net && !was.vervallen) {
            // a net that was still right is swallowed by this one (or cut off by the size limit: then it is too big as well)
            was.vervallen = true;
            netten.remove(was);
            opnieuw.remove(was);
            oud.add(was);
        }
        netten.add(net);
    }

    private void evalueer(VadsNet net, boolean seconde) {
        try {
            if (!net.evalueer(level, this, seconde)) {
                verval(net);
                zaden.addAll(net.plekken);
            }
        } catch (RuntimeException e) {
            // (a knoop of some machine threw: its net stands still, the server ticks on)
            if (level.getGameTime() - foutGemeld > 1200 || foutGemeld == Long.MIN_VALUE) {
                foutGemeld = level.getGameTime();
                LOG.error("Guhs vadskracht: a net could not be evaluated (knopen at {})", net.knopen.isEmpty() ? "-" : net.knopen.get(0).vadsPlek(), e);
            }
            try {
                net.stil(level, this);
            } catch (RuntimeException again) {
                // (nothing more to do)
            }
        }
    }

    void vertel(VadsVerbruiker v, boolean aan) {
        Boolean was = verteld.put(v, aan);
        if (was == null || was != aan) {
            v.vadsStroom(aan);
        }
    }

    // =====================================================================================================================
    // asking
    // =====================================================================================================================

    /** The net here, built if need be and evaluated now. */
    VadsNet nu(BlockPos pos) {
        if (!geladen(level, pos)) {
            return VadsNet.EMPTY;
        }
        long key = pos.asLong();
        VadsNet net = perPlek.get(key);
        if (net == null || net.vervallen) {
            zaden.add(key);
        }
        if (!zaden.isEmpty() || !oud.isEmpty()) {
            bouwAlles();
            net = perPlek.get(key);
        }
        if (net == null || net.vervallen) {
            return VadsNet.EMPTY;
        }
        evalueer(net, false);
        if (net.vervallen) {   // (a knoop of it was gone)
            bouwAlles();
            net = perPlek.get(key);
        }
        return net == null || net.vervallen ? VadsNet.EMPTY : net;
    }

    /** How many nets this level has right now (for the tests and the dev command). */
    int aantal() {
        return netten.size();
    }
}
