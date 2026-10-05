package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.speelgoed.GlijbaanBlock;
import nl.juiced.guhs.feature.speelgoed.SchommelToestel;
import nl.juiced.guhs.feature.speelgoed.SpeelDeelBlock;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;
import nl.juiced.guhs.feature.speelgoed.TunnelBlock;

/**
 * The rules of a Guh-parkour route: what counts as a piece ({@link #stuk}, {@link #soort}), adding a clicked piece to a
 * list ({@link #klik}: at most {@link #MAX_STUKKEN} pieces within {@link #BEREIK} blocks of the start, the Finishpaaltje
 * always last, a second click takes a piece out again), and the dotted line that shows a route to one player.
 */
public final class Routes {
    public static final int MAX_STUKKEN = 16, BEREIK = 32, MAX_GUHS = 4;
    private static final DustParticleOptions STIP = new DustParticleOptions(0xFF7AC8, 0.8f), KNOOP = new DustParticleOptions(0xFFE66E, 1.2f);

    /** What a piece is, for the runner. */
    public enum Soort { OBSTAKEL, GLIJBAAN, WIP_SCHOMMEL, TUNNEL, FINISH }

    /** What a click did. */
    public enum Uitkomst { ERBIJ, FINISH, ERUIT, VOL, TE_VER, GEEN }

    /** The piece a click on this block means (the controller of a toy, the tunnel piece, the Finishpaaltje), or null. */
    @Nullable
    public static BlockPos stuk(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SpeelDeelBlock) {
            pos = SpeelDeelBlock.controller(state, pos);
            state = level.getBlockState(pos);
        }
        return soort(state) == null ? null : pos.immutable();
    }

    @Nullable
    public static Soort soort(BlockState state) {
        if (state.getBlock() instanceof ObstakelBlock) {
            return Soort.OBSTAKEL;
        }
        if (state.getBlock() instanceof GlijbaanBlock) {
            return Soort.GLIJBAAN;
        }
        if (state.getBlock() instanceof SchommelToestel) {
            return Soort.WIP_SCHOMMEL;
        }
        if (state.getBlock() instanceof ToestelBlock) {
            return null;   // (an unknown toy of a later update: not a piece until the runner knows how to use it)
        }
        if (state.getBlock() instanceof TunnelBlock) {
            return Soort.TUNNEL;
        }
        if (state.is(ParkourSlice.FINISHPAALTJE.get())) {
            return Soort.FINISH;
        }
        return null;
    }

    @Nullable
    public static Soort soort(BlockGetter level, BlockPos pos) {
        return soort(level.getBlockState(pos));
    }

    public static boolean heeftFinish(BlockGetter level, List<BlockPos> stukken) {
        return !stukken.isEmpty() && soort(level, stukken.get(stukken.size() - 1)) == Soort.FINISH;
    }

    /** How many pieces count towards the maximum (the Finishpaaltje is free). */
    public static int aantal(BlockGetter level, List<BlockPos> stukken) {
        return stukken.size() - (heeftFinish(level, stukken) ? 1 : 0);
    }

    /**
     * A click on a piece while laying out a route: changes the list. midden: the Startpaaltje (or, while the post is still
     * in your hand, the first piece); null: no distance check yet.
     */
    public static Uitkomst klik(BlockGetter level, List<BlockPos> stukken, @Nullable BlockPos midden, BlockPos stuk) {
        Soort soort = soort(level, stuk);
        if (soort == null) {
            return Uitkomst.GEEN;
        }
        if (stukken.remove(stuk)) {
            return Uitkomst.ERUIT;
        }
        if (midden != null && !midden.closerThan(stuk, BEREIK + 0.5)) {
            return Uitkomst.TE_VER;
        }
        boolean finish = heeftFinish(level, stukken);
        if (soort == Soort.FINISH) {
            if (finish) {
                stukken.remove(stukken.size() - 1);   // (another Finishpaaltje: the finish moves)
            }
            stukken.add(stuk);
            return Uitkomst.FINISH;
        }
        if (aantal(level, stukken) >= MAX_STUKKEN) {
            return Uitkomst.VOL;
        }
        stukken.add(finish ? stukken.size() - 1 : stukken.size(), stuk);
        return Uitkomst.ERBIJ;
    }

    /** The number of a piece in the list (1-based; the Finishpaaltje is the last number). */
    public static int nummer(List<BlockPos> stukken, BlockPos stuk) {
        return stukken.indexOf(stuk) + 1;
    }

    /** The name of the thing at a piece's place (its block name; "weg" when it is gone). */
    public static Component naam(BlockGetter level, BlockPos stuk) {
        BlockState state = level.getBlockState(stuk);
        return soort(state) == null ? Component.translatable("gui.guhs.guhparkour.stuk.weg") : state.getBlock().getName();
    }

    public static ItemStack icoon(BlockGetter level, BlockPos stuk) {
        BlockState state = level.getBlockState(stuk);
        return soort(state) == null ? ItemStack.EMPTY : new ItemStack(state.getBlock());
    }

    /** Where a guh (or the dotted line) touches a piece: the middle of its block, a little above the floor. */
    public static Vec3 punt(BlockPos stuk) {
        return new Vec3(stuk.getX() + 0.5, stuk.getY() + 0.6, stuk.getZ() + 0.5);
    }

    /** Shows the route to this player only: a pink dotted line from the start along every piece, a yellow dot on each. */
    public static void toon(ServerPlayer speler, @Nullable BlockPos start, List<BlockPos> stukken) {
        if (!(speler.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 vorige = start == null ? null : punt(start);
        int budget = 220;
        for (BlockPos stuk : stukken) {
            Vec3 p = punt(stuk);
            level.sendParticles(speler, KNOOP, false, false, p.x, p.y + 0.5, p.z, 2, 0.08, 0.08, 0.08, 0);
            if (vorige != null) {
                double lengte = vorige.distanceTo(p);
                int n = Math.min(budget, (int) (lengte / 0.6));
                for (int i = 1; i < n; i++) {
                    Vec3 s = vorige.lerp(p, i / (double) n);
                    level.sendParticles(speler, STIP, false, false, s.x, s.y, s.z, 1, 0, 0, 0, 0);
                }
                budget -= n;
            }
            vorige = p;
        }
    }

    private Routes() {
    }
}
