package nl.juiced.guhs.feature.mijnhulp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.verblijf.ThuisDier;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfFeature;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;

/**
 * 1.7.0 (rondes 32-34 and 37-40): terraria of any shape. A terrarium (the glass tank a terrariumbord becomes at home) with
 * terrariumglas or terrariumbodem built onto it (block tag {@code guhs:terrarium_ruimte}) is one living space together
 * with everything joined to it, face to face, in any shape: a big glass tank, tunnels one block wide, up and down. Its
 * little creatures (still the one family of the one terrarium) walk around in all of it, from piece to piece, and never
 * out of it: they only ever move between two pieces that touch, and one whose piece disappears is back in the terrarium
 * itself straight away.
 * <p>
 * Performance: the shape is a flood fill from the terrarium over at most {@link #MAX} pieces (the rest is left out),
 * kept per terrarium and only worked out again when a block in or right next to it changes (NeoForge's neighbour notify
 * event: {@link #onBlok}) and, to be safe, every {@link #OPNIEUW} ticks. Two terraria joined by glass don't share: the
 * one with the lowest position gets the whole space, the other keeps just its own tank.
 */
public final class TerrariumRuimte {
    /** At most this many pieces in one living space (the terrarium itself included). */
    public static final int MAX = 4096;
    /** What a living space may be made of (besides the terrarium itself): terrariumglas, terrariumbodem. */
    public static final TagKey<Block> CEL = BlockTags.create(Guhs.id("terrarium_ruimte"));
    /** Worked out again at least this often (ticks). */
    private static final int OPNIEUW = 600;
    /** How far (in pieces) a creature walks in one go. */
    private static final int STAPPEN = 24;
    private static final Map<Level, Map<BlockPos, Ruimte>> RUIMTES = new WeakHashMap<>();

    /** The living space of one terrarium. */
    static final class Ruimte {
        final BlockPos thuis;
        LongOpenHashSet cellen = new LongOpenHashSet();
        List<BlockPos> lijst = List.of();
        int x0, y0, z0, x1, y1, z1;
        boolean vuil = true;
        long berekend, gezien;
        final Map<UUID, Wandel> wandel = new HashMap<>();

        Ruimte(BlockPos thuis) {
            this.thuis = thuis.immutable();
        }

        boolean bevat(BlockPos p) {
            return cellen.contains(p.asLong());
        }

        boolean dichtbij(BlockPos p) {
            return p.getX() >= x0 - 1 && p.getX() <= x1 + 1 && p.getY() >= y0 - 1 && p.getY() <= y1 + 1 && p.getZ() >= z0 - 1 && p.getZ() <= z1 + 1;
        }
    }

    /** A creature on its way: the points still to go (one per piece), and how long it still rests. */
    static final class Wandel {
        final ArrayDeque<Vec3> pad = new ArrayDeque<>();
        int pauze = 40;
    }

    // =================================================================================================================
    // the shape
    // =================================================================================================================

    /** The living space of the terrarium at {@code thuis} (the terrarium first), as it is now: for tests and the cache. */
    public static List<BlockPos> ruimte(ServerLevel level, BlockPos thuis) {
        Ruimte r = new Ruimte(thuis);
        bereken(level, r);
        return r.lijst;
    }

    private static void bereken(ServerLevel level, Ruimte r) {
        LongOpenHashSet cellen = new LongOpenHashSet();
        List<BlockPos> lijst = new ArrayList<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        LongOpenHashSet gezien = new LongOpenHashSet();
        boolean verloren = false;
        cellen.add(r.thuis.asLong());
        gezien.add(r.thuis.asLong());
        lijst.add(r.thuis);
        rij.add(r.thuis);
        Block terrarium = VerblijfFeature.TERRARIUM.get();
        while (!rij.isEmpty() && lijst.size() < MAX) {
            BlockPos p = rij.poll();
            for (Direction d : Direction.values()) {
                BlockPos q = p.relative(d);
                if (!gezien.add(q.asLong()) || !level.isLoaded(q)) {
                    continue;
                }
                BlockState s = level.getBlockState(q);
                if (s.is(terrarium)) {
                    verloren |= q.asLong() < r.thuis.asLong();       // (another terrarium joined to this one)
                } else if (s.is(CEL) && lijst.size() < MAX) {
                    cellen.add(q.asLong());
                    lijst.add(q.immutable());
                    rij.add(q.immutable());
                }
            }
        }
        if (verloren) {
            cellen.clear();
            cellen.add(r.thuis.asLong());
            lijst = List.of(r.thuis);
        }
        r.cellen = cellen;
        r.lijst = List.copyOf(lijst);
        r.x0 = r.x1 = r.thuis.getX();
        r.y0 = r.y1 = r.thuis.getY();
        r.z0 = r.z1 = r.thuis.getZ();
        for (BlockPos p : lijst) {
            r.x0 = Math.min(r.x0, p.getX());
            r.y0 = Math.min(r.y0, p.getY());
            r.z0 = Math.min(r.z0, p.getZ());
            r.x1 = Math.max(r.x1, p.getX());
            r.y1 = Math.max(r.y1, p.getY());
            r.z1 = Math.max(r.z1, p.getZ());
        }
        r.vuil = false;
        r.berekend = level.getGameTime();
    }

    /** A block changed: the living spaces it touches are worked out again (before their creatures' next step). */
    static void onBlok(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<BlockPos, Ruimte> hier = RUIMTES.get(level);
        if (hier == null || hier.isEmpty()) {
            return;
        }
        BlockPos p = event.getPos();
        long nu = level.getGameTime();
        for (Iterator<Ruimte> it = hier.values().iterator(); it.hasNext(); ) {
            Ruimte r = it.next();
            if (nu - r.gezien > 200 || nu < r.gezien) {
                it.remove();                    // (its terrarium is gone or not loaded: no ticks any more)
            } else if (r.dichtbij(p) && (r.bevat(p) || event.getState().is(CEL) || event.getState().is(VerblijfFeature.TERRARIUM.get()))) {
                r.vuil = true;
            }
        }
    }

    // =================================================================================================================
    // the creatures
    // =================================================================================================================

    /** Every tick of a terrarium (called from VerblijfsbordBlockEntity#tick): its creatures walk around in its space. */
    public static void tick(ServerLevel level, VerblijfsbordBlockEntity be) {
        Ruimte r = RUIMTES.computeIfAbsent(level, k -> new HashMap<>()).computeIfAbsent(be.getBlockPos().immutable(), Ruimte::new);
        long nu = level.getGameTime();
        r.gezien = nu;
        if (r.vuil || nu - r.berekend >= OPNIEUW || nu < r.berekend) {
            bereken(level, r);
        }
        ParkData.Soort info = ParkData.get().soort(be.soort());
        boolean zee = info != null && info.zee();
        double snel = info == null ? 0.03 : Mth.clamp(info.snelheid() * 0.2, 0.015, 0.06);
        List<ThuisDier> dieren = be.dieren(level);
        for (ThuisDier d : dieren) {
            stap(level, r, d, zee, snel);
        }
        if (r.wandel.size() > dieren.size() + 4) {
            r.wandel.keySet().removeIf(id -> dieren.stream().noneMatch(d -> d.getUUID().equals(id)));
        }
    }

    private static void stap(ServerLevel level, Ruimte r, ThuisDier d, boolean zee, double snel) {
        Vec3 v = d.vast();
        if (v == null) {
            return;
        }
        Wandel w = r.wandel.computeIfAbsent(d.getUUID(), k -> new Wandel());
        BlockPos cel = cel(v);
        RandomSource rnd = d.getRandom();
        if (!r.bevat(cel)) {
            // its piece is gone (or it was never in this space): back into the terrarium itself, at once
            w.pad.clear();
            d.schuifVast(plek(level, r.thuis, zee, rnd), d.getYRot());
            return;
        }
        if (w.pad.isEmpty()) {
            if (r.lijst.size() <= 1 || d.slaapt() || --w.pauze > 0) {
                return;
            }
            w.pauze = 80 + rnd.nextInt(240);
            kiesPad(level, r, cel, w, zee, rnd);
            return;
        }
        Vec3 doel = w.pad.peek();
        if (!r.bevat(cel(doel))) {
            w.pad.clear();      // (the way changed: think again)
            return;
        }
        Vec3 delta = doel.subtract(v);
        double afstand = delta.length();
        float yaw = Math.abs(delta.x) + Math.abs(delta.z) > 1.0e-4 ? (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90f : d.getYRot();
        if (afstand <= snel) {
            d.schuifVast(doel, yaw);
            w.pad.poll();
        } else {
            d.schuifVast(v.add(delta.scale(snel / afstand)), yaw);
        }
    }

    /** The piece a point is in. */
    private static BlockPos cel(Vec3 v) {
        return BlockPos.containing(v.x, v.y + 0.01, v.z);
    }

    /** A walk to a random piece at most {@link #STAPPEN} pieces away: one point per piece on the way (all of them touching). */
    private static void kiesPad(ServerLevel level, Ruimte r, BlockPos van, Wandel w, boolean zee, RandomSource rnd) {
        Long2LongOpenHashMap ouder = new Long2LongOpenHashMap();
        ouder.defaultReturnValue(Long.MIN_VALUE);
        LongArrayList bereikt = new LongArrayList();
        ArrayDeque<long[]> rij = new ArrayDeque<>();
        ouder.put(van.asLong(), van.asLong());
        rij.add(new long[]{van.asLong(), 0});
        while (!rij.isEmpty()) {
            long[] e = rij.poll();
            if (e[1] >= STAPPEN) {
                continue;
            }
            BlockPos p = BlockPos.of(e[0]);
            for (Direction d : Direction.values()) {
                long q = p.relative(d).asLong();
                if (r.cellen.contains(q) && !ouder.containsKey(q)) {
                    ouder.put(q, e[0]);
                    bereikt.add(q);
                    rij.add(new long[]{q, e[1] + 1});
                }
            }
        }
        if (bereikt.isEmpty()) {
            return;
        }
        long doel = bereikt.getLong(rnd.nextInt(bereikt.size()));
        List<Long> terug = new ArrayList<>();
        for (long p = doel; p != van.asLong(); p = ouder.get(p)) {
            terug.add(p);
        }
        for (int i = terug.size() - 1; i >= 0; i--) {
            w.pad.add(plek(level, BlockPos.of(terug.get(i)), zee, rnd));
        }
    }

    /** Where a creature stands in a piece: on its floor (the terrarium's and the terrariumbodem's sand is higher), a swimmer higher. */
    static Vec3 plek(Level level, BlockPos cel, boolean zee, RandomSource rnd) {
        BlockState s = level.getBlockState(cel);
        double vloer = zee ? 0.4 : s.is(VerblijfFeature.TERRARIUM.get()) || s.is(MijnhulpFeature.TERRARIUMBODEM.get()) ? 2.6 / 16 : 0.5 / 16;
        return new Vec3(cel.getX() + 0.5 + (rnd.nextDouble() - 0.5) * 0.4, cel.getY() + vloer, cel.getZ() + 0.5 + (rnd.nextDouble() - 0.5) * 0.4);
    }

    // =================================================================================================================
    /** Terrariumbodem: a glass tank piece with a sandy floor (like the terrarium's), its glass joining the other pieces. */
    public static class Bodem extends TransparentBlock {
        public static final MapCodec<Bodem> CODEC = simpleCodec(Bodem::new);

        public Bodem(Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<? extends TransparentBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState buur, Direction richting) {
            return buur.is(CEL) || super.skipRendering(state, buur, richting);
        }
    }

    /** (Tests) every piece of the living space the cache holds now for this terrarium, or empty. */
    public static Set<BlockPos> gecached(Level level, BlockPos thuis) {
        Map<BlockPos, Ruimte> hier = RUIMTES.get(level);
        Ruimte r = hier == null ? null : hier.get(thuis);
        return r == null ? Set.of() : new LinkedHashSet<>(r.lijst);
    }

    private TerrariumRuimte() {
    }
}
