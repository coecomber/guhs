package nl.juiced.guhs.feature.techsaus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import nl.juiced.guhs.feature.vadskracht.Meerblok;
import nl.juiced.guhs.feature.vadskracht.Sauzen;

/**
 * The Sausslang nets: who is joined to whom by hoses. A hose holds nothing itself; whoever wants sauce moved asks here what
 * is at the other ends:
 * <ul>
 *   <li>a Sauspomp PUSHES what it pumped into everything that takes it ({@link #duw});</li>
 *   <li>a machine SLURPS the sauce it needs out of everything that gives it ({@link #haal}): a Sausvat, a pump;</li>
 *   <li>a Sausvat does nothing by itself (so sauce never runs in circles).</li>
 * </ul>
 * "Joined" = touching directly, or through at most {@link SausGetallen#SLANG_MAX} {@link SausslangBlock}s. An end is any
 * block with NeoForge's fluid capability, so tanks of other mods (and a cauldron) join in. The nets are derived, never
 * saved: an answer is remembered per asker until a hose is placed, removed or turned ({@link #veranderd}) and for at most
 * {@link SausGetallen#SLANG_ONTHOUD} ticks.
 */
public final class Slangen {
    /** One end of a hose net: a block that may hold fluid, and the side of it the hose (or the asker) touches. */
    public record Aansluiting(BlockPos pos, Direction kant) {
        @Nullable
        public ResourceHandler<FluidResource> handler(Level level) {
            return Sauzen.van(level, pos, kant);
        }
    }

    private record Onthouden(int versie, long tot, List<Aansluiting> lijst) {
    }

    private static final class PerLevel {
        int versie;
        final Map<BlockPos, Onthouden> lijsten = new HashMap<>();
    }

    private static final Map<Level, PerLevel> LEVELS = new WeakHashMap<>();

    private static PerLevel van(Level level) {
        return LEVELS.computeIfAbsent(level, l -> new PerLevel());
    }

    /** A hose (or something a hose may end at) was placed, removed or changed: every remembered net is looked up again. */
    public static void veranderd(@Nullable Level level) {
        if (level instanceof ServerLevel) {
            PerLevel per = van(level);
            per.versie++;
            if (per.lijsten.size() > 4096) {
                per.lijsten.clear();   // (askers that are gone)
            }
        }
    }

    /** Forget a level (it unloaded) or everything (null: the server stopped). */
    static void vergeet(@Nullable Level level) {
        if (level == null) {
            LEVELS.clear();
        } else {
            LEVELS.remove(level);
        }
    }

    /**
     * Everything that may hold fluid and is joined to the block at {@code pos} (a machine: its kern; the blocks of a machine
     * bigger than one block all count as the asker), in a fixed order. Never the asker itself. An entry is a spot, not a
     * promise: {@link Aansluiting#handler} is null when the block there holds no fluid (any more).
     */
    public static List<Aansluiting> aansluitingen(ServerLevel level, BlockPos pos) {
        PerLevel per = van(level);
        long nu = level.getGameTime();
        Onthouden oud = per.lijsten.get(pos);
        if (oud != null && oud.versie == per.versie && nu < oud.tot) {
            return oud.lijst;
        }
        List<Aansluiting> lijst = zoek(level, pos);
        per.lijsten.put(pos.immutable(), new Onthouden(per.versie, nu + SausGetallen.SLANG_ONTHOUD, lijst));
        return lijst;
    }

    private static List<Aansluiting> zoek(ServerLevel level, BlockPos pos) {
        Set<BlockPos> zelf = new HashSet<>();
        zelf.add(pos.immutable());
        zelf.addAll(Meerblok.delen(pos, level.getBlockState(pos)));
        Set<BlockPos> gezien = new HashSet<>();
        Set<Aansluiting> uit = new LinkedHashSet<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        for (BlockPos blok : zelf) {
            for (Direction kant : Direction.values()) {
                BlockPos buur = blok.relative(kant);
                if (zelf.contains(buur) || !level.isLoaded(buur)) {
                    continue;
                }
                BlockState state = level.getBlockState(buur);
                if (state.getBlock() instanceof SausslangBlock) {
                    if (state.getValue(SausslangBlock.kant(kant.getOpposite())) && gezien.add(buur)) {
                        rij.add(buur);
                    }
                } else if (!state.isAir()) {
                    uit.add(new Aansluiting(buur, kant.getOpposite()));
                }
            }
        }
        int slangen = gezien.size();
        while (!rij.isEmpty()) {
            BlockPos slang = rij.poll();
            BlockState slangState = level.getBlockState(slang);
            for (Direction kant : Direction.values()) {
                if (!slangState.getValue(SausslangBlock.kant(kant))) {
                    continue;
                }
                BlockPos buur = slang.relative(kant);
                if (zelf.contains(buur) || !level.isLoaded(buur)) {
                    continue;
                }
                BlockState state = level.getBlockState(buur);
                if (state.getBlock() instanceof SausslangBlock) {
                    if (slangen < SausGetallen.SLANG_MAX && gezien.add(buur)) {
                        slangen++;
                        rij.add(buur);
                    }
                } else {
                    uit.add(new Aansluiting(buur, kant.getOpposite()));
                }
            }
        }
        return List.copyOf(uit);
    }

    /**
     * Slurps up to {@code mb} of this sauce into {@code naar} out of everything joined to the machine at {@code pos} (what
     * gives it: a Sausvat, a pump). {@code eigen} is the handler the machine shows to the outside (so it never drinks from
     * itself through one of its part blocks). Returns what came in.
     */
    public static int haal(ServerLevel level, BlockPos pos, @Nullable ResourceHandler<FluidResource> eigen, ResourceHandler<FluidResource> naar,
                           FluidResource wat, int mb) {
        int binnen = 0;
        for (Aansluiting a : aansluitingen(level, pos)) {
            if (binnen >= mb) {
                break;
            }
            ResourceHandler<FluidResource> bron = a.handler(level);
            if (bron != null && bron != eigen && bron != naar) {
                binnen += Sauzen.verplaats(bron, naar, wat, mb - binnen);
            }
        }
        return binnen;
    }

    /**
     * Pushes up to {@code mb} out of {@code van} into everything joined to the block at {@code pos} that takes it: first an
     * equal share for every end (each time starting at the next one, so one greedy vat does not get everything), then what
     * is left to whoever still has room. Returns what went out.
     */
    public static int duw(ServerLevel level, BlockPos pos, @Nullable ResourceHandler<FluidResource> eigen, ResourceHandler<FluidResource> van, int mb) {
        if (mb <= 0) {
            return 0;
        }
        List<ResourceHandler<FluidResource>> doelen = new ArrayList<>();
        for (Aansluiting a : aansluitingen(level, pos)) {
            ResourceHandler<FluidResource> doel = a.handler(level);
            if (doel != null && doel != eigen && doel != van && !doelen.contains(doel)) {
                doelen.add(doel);
            }
        }
        if (doelen.isEmpty()) {
            return 0;
        }
        int weg = 0;
        int begin = (int) Math.floorMod(level.getGameTime() / SausGetallen.SLANG_TIKKEN, (long) doelen.size());
        int deel = Math.max(1, mb / doelen.size());
        for (int ronde = 0; ronde < 2 && weg < mb; ronde++) {
            for (int i = 0; i < doelen.size() && weg < mb; i++) {
                weg += Sauzen.verplaats(van, doelen.get((begin + i) % doelen.size()), null, ronde == 0 ? Math.min(deel, mb - weg) : mb - weg);
            }
        }
        return weg;
    }

    /** (tests, the op command) the ends of the block here, looked up afresh. */
    public static List<Aansluiting> opnieuw(ServerLevel level, BlockPos pos) {
        veranderd(level);
        return new ArrayList<>(aansluitingen(level, pos));
    }

    private Slangen() {
    }
}
