package nl.juiced.guhs.feature.onderwol;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RootedDirtBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7.0 "Onder de Wol": the block classes of the new cave kinds (registered in {@link OnderWolGrot}; the kristalpegel has
 * its own class, {@link Kristalpegel}): the knuffelwortelaarde that grows knuffelwortels, and the musical belletjes.
 * <p>
 * The belletjes play a note whose pitch depends only on where the block is (a major pentatonic scale over two octaves,
 * along x+y+z), so a row of them is a little scale and you can tap tunes. A note echoes once, softer, a few ticks later.
 */
public final class OnderWolGrotBlokken {
    /** The scale: note block steps (0-24) of a major pentatonic over two octaves, starting at F#. */
    private static final int[] TOONLADDER = {0, 2, 4, 7, 9, 12, 14, 16, 19, 21};
    private static final int GRONDTOON = 3;
    /** Ticks until the echo, and its loudness relative to the note. */
    private static final int ECHO_TICKS = 5;
    private static final float ECHO_VOLUME = 0.3f;

    /** The note block step (0-24) a belletje at pos plays. */
    public static int noot(BlockPos pos) {
        return GRONDTOON + TOONLADDER[Math.floorMod(pos.getX() + pos.getY() + pos.getZ(), TOONLADDER.length)];
    }

    /** The sound pitch of a note block step (as the note block). */
    public static float toonhoogte(int noot) {
        return (float) Math.pow(2.0, (noot - 12) / 12.0);
    }

    /**
     * Plays the belletje at pos (server side): the note, a coloured music note particle at (x, y, z), and schedules the
     * echo (the block's {@code tick} plays it).
     */
    static void klink(ServerLevel level, BlockPos pos, Block block, Holder<SoundEvent> geluid, float volume, double x, double y, double z) {
        int noot = noot(pos);
        level.playSound(null, pos, geluid.value(), SoundSource.BLOCKS, volume, toonhoogte(noot));
        level.sendParticles(ParticleTypes.NOTE, x, y, z, 0, noot / 24.0, 0.0, 0.0, 1.0);
        if (!level.getBlockTicks().hasScheduledTick(pos, block)) {
            level.scheduleTick(pos, block, ECHO_TICKS);
        }
    }

    static void echo(ServerLevel level, BlockPos pos, Holder<SoundEvent> geluid, float volume) {
        level.playSound(null, pos, geluid.value(), SoundSource.BLOCKS, volume * ECHO_VOLUME, toonhoogte(noot(pos)));
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Belletjeskristal (like an amethyst block): rings when you step onto it (not when sneaking), land on it, hit it, or
     * when a projectile hits it. Walking over a row of them plays the scale.
     */
    public static class Belletjeskristal extends Block {
        public static final MapCodec<Belletjeskristal> CODEC = simpleCodec(Belletjeskristal::new);
        private static final float VOLUME = 0.8f;
        /** Per entity the block it last rang (as a long) and when, so standing still does not ring on and on. */
        private static final Map<Entity, long[]> LAATST = new WeakHashMap<>();
        private static final int OPNIEUW_NA = 20;

        public Belletjeskristal(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Belletjeskristal> codec() {
            return CODEC;
        }

        private void klink(ServerLevel level, BlockPos pos) {
            OnderWolGrotBlokken.klink(level, pos, this, SoundEvents.NOTE_BLOCK_BELL, VOLUME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (level instanceof ServerLevel server && !entity.isSteppingCarefully()) {
                long[] l = LAATST.get(entity);
                long nu = server.getGameTime();
                if (l == null || l[0] != pos.asLong() || nu - l[1] > OPNIEUW_NA && entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3) {
                    LAATST.put(entity, new long[]{pos.asLong(), nu});
                    klink(server, pos);
                }
            }
            super.stepOn(level, pos, state, entity);
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            if (level instanceof ServerLevel server && fallDistance > 0.5) {
                LAATST.put(entity, new long[]{pos.asLong(), server.getGameTime()});
                klink(server, pos);
            }
            super.fallOn(level, state, pos, entity, fallDistance);
        }

        @Override
        protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
            if (level instanceof ServerLevel server) {
                klink(server, pos);
            }
        }

        @Override
        protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
            if (level instanceof ServerLevel server) {
                klink(server, hit.getBlockPos());
            }
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            echo(level, pos, SoundEvents.NOTE_BLOCK_BELL, VOLUME);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Belletjestrosje (like an amethyst cluster, on any face): a little bunch of crystal bells. Click it or hit it (or shoot
     * it) and it chimes its note.
     */
    public static class Belletjestrosje extends AmethystClusterBlock {
        private static final float VOLUME = 0.9f;

        public Belletjestrosje(float hoogte, float breedte, BlockBehaviour.Properties properties) {
            super(hoogte, breedte, properties);
        }

        private void klink(ServerLevel level, BlockPos pos, BlockState state) {
            Direction d = state.getValue(FACING);
            OnderWolGrotBlokken.klink(level, pos, this, SoundEvents.NOTE_BLOCK_CHIME, VOLUME, pos.getX() + 0.5 + d.getStepX() * 0.3,
                    pos.getY() + 0.5 + d.getStepY() * 0.3, pos.getZ() + 0.5 + d.getStepZ() * 0.3);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                klink(server, pos, state);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
            if (level instanceof ServerLevel server) {
                klink(server, pos, state);
            }
        }

        @Override
        protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
            if (level instanceof ServerLevel server) {
                klink(server, hit.getBlockPos(), state);
            }
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            echo(level, pos, SoundEvents.NOTE_BLOCK_CHIME, VOLUME);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Knuffelwortelaarde (like rooted dirt): bone meal grows knuffelwortels under it. */
    public static class Knuffelwortelaarde extends RootedDirtBlock {
        public Knuffelwortelaarde(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            level.setBlockAndUpdate(pos.below(), OnderWolGrot.KNUFFELWORTELS.get().defaultBlockState());
        }
    }

    private OnderWolGrotBlokken() {
    }
}
