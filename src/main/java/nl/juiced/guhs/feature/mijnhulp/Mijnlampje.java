package nl.juiced.guhs.feature.mijnhulp;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * 1.7.0 (ronde 44): the Mijnlampje, a little miner's helmet with a lamp for your tamed guh (guh clothes, slot HEAD,
 * {@link GuhClothes#MIJNLAMPJE}). A guh wearing it lights up the dark where it walks. Minecraft has no moving light, so
 * the server puts an invisible light block ({@link Licht}, light {@link #LICHT}) in the free spot at the guh's head (or
 * feet) while that spot is dark, moves it along as the guh walks and takes the old one away.
 * <p>
 * Never left behind: the guh takes its light along when it leaves the level (unloaded, died, teleported to another
 * dimension: {@link #onLeave}), and every light block checks for itself, every second and on random ticks, whether a guh
 * with a Mijnlampje is still right there, and disappears if not (also after a crash or a chunk that was saved with one in
 * it). It only ever goes into air, gives way to anything placed or flowing into it, has no collision, no outline and no
 * drops. Server-side only; the client just sees the light.
 */
public final class Mijnlampje {
    /** The light level of the lamp. */
    public static final int LICHT = 13;
    /** Below this (sky and other block light) it counts as dark. */
    private static final int DONKER = 8;
    /** Every this many ticks a lamp guh looks where its light should be. */
    private static final int ELKE = 4;
    /** Where each lamp guh's light is now (server thread only). */
    private static final Map<GuhEntity, BlockPos> LAMPEN = new WeakHashMap<>();

    public static boolean draagt(GuhEntity g) {
        return g.isAlive() && g.isTame() && g.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.MIJNLAMPJE;
    }

    /** Every few ticks per guh: put the light where the guh is (if it is dark there), take the old one away. */
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof GuhEntity g) || !(g.level() instanceof ServerLevel level) || (g.tickCount + g.getId()) % ELKE != 0) {
            return;
        }
        BlockPos oud = LAMPEN.get(g);
        if (oud == null && !draagt(g)) {
            return;
        }
        BlockPos nieuw = draagt(g) ? plek(level, g, oud) : null;
        if (nieuw != null && nieuw.equals(oud) && level.getBlockState(nieuw).is(MijnhulpFeature.MIJNLAMP_LICHT.get())) {
            return;     // (still there; another lamp guh on the same spot may have taken it away: then it is put back)
        }
        if (nieuw != null) {
            level.setBlock(nieuw, MijnhulpFeature.MIJNLAMP_LICHT.get().defaultBlockState(), Block.UPDATE_ALL);
            LAMPEN.put(g, nieuw);
        } else {
            LAMPEN.remove(g);
        }
        if (oud != null && !oud.equals(nieuw)) {
            weg(level, oud);
        }
    }

    /** The guh leaves the level (unloaded, died, another dimension): its light goes with it. */
    static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof GuhEntity g && event.getLevel() instanceof ServerLevel level) {
            BlockPos oud = LAMPEN.remove(g);
            if (oud != null) {
                weg(level, oud);
            }
        }
    }

    private static void weg(ServerLevel level, BlockPos p) {
        if (level.isLoaded(p) && level.getBlockState(p).is(MijnhulpFeature.MIJNLAMP_LICHT.get())) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * Where the light should be: the first free spot of head and feet (air, or our own light), if it is dark there. Our own
     * light (still at {@code oud}) doesn't count as light: what it gives there is subtracted.
     */
    @Nullable
    static BlockPos plek(ServerLevel level, GuhEntity g, @Nullable BlockPos oud) {
        BlockPos voeten = BlockPos.containing(g.getX(), g.getY() + 0.2, g.getZ());
        BlockPos hoofd = BlockPos.containing(g.getX(), g.getEyeY(), g.getZ());
        for (BlockPos p : hoofd.equals(voeten) ? new BlockPos[]{voeten} : new BlockPos[]{hoofd, voeten}) {
            BlockState s = level.getBlockState(p);
            boolean eigen = s.is(MijnhulpFeature.MIJNLAMP_LICHT.get());
            if (!eigen && !s.isAir()) {
                continue;
            }
            return donker(level, p, oud) ? p : null;
        }
        return null;
    }

    static boolean donker(Level level, BlockPos p, @Nullable BlockPos oud) {
        int lucht = level.getBrightness(LightLayer.SKY, p) - level.getSkyDarken();
        int blok = level.getBrightness(LightLayer.BLOCK, p);
        int eigen = oud == null ? 0 : LICHT - oud.distManhattan(p);
        if (blok <= eigen) {
            blok = 0;      // (all of it may be our own lamp's)
        }
        return Math.max(lucht, blok) < DONKER;
    }

    /** Is a guh with a Mijnlampje right at this light? */
    static boolean lampGuhBij(Level level, BlockPos p) {
        return !level.getEntitiesOfClass(GuhEntity.class, new AABB(p).inflate(2.0), Mijnlampje::draagt).isEmpty();
    }

    /** (Tests) where this guh's light is now. */
    @Nullable
    public static BlockPos lamp(GuhEntity g) {
        return LAMPEN.get(g);
    }

    // =================================================================================================================
    /** The invisible light: nothing to see or touch; goes away by itself as soon as no lamp guh is near. */
    public static class Licht extends Block {
        public static final MapCodec<Licht> CODEC = simpleCodec(Licht::new);
        private static final int KIJK = 20;

        public Licht(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oud, boolean piston) {
            if (!level.isClientSide()) {
                level.scheduleTick(pos, this, KIJK);
            }
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            kijk(level, pos);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            kijk(level, pos);
        }

        private void kijk(ServerLevel level, BlockPos pos) {
            if (lampGuhBij(level, pos)) {
                level.scheduleTick(pos, this, KIJK);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private Mijnlampje() {
    }
}
