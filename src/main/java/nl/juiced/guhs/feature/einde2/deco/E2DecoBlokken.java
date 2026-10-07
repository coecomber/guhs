package nl.juiced.guhs.feature.einde2.deco;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.gids.GidsFeature;

/**
 * einde2, slice deco: the four little blocks the decoration buildings of the outer islands are dressed with. Each is a
 * thing a player may take home: the kool of the wrecked koolkraam, the windgong of the air nomads, the eeuwige vlam of the
 * Zonnekrijgers and the witte lotustegel of the Pai Sho table. Models and textures: tools/features/einde2_deco.py.
 */
public final class E2DecoBlokken {
    private E2DecoBlokken() {
    }

    /** A cabbage. Whoever lands on it from a jump squashes it flat ("MIJN KOLEN!"); it still drops a whole one. */
    public static class Kool extends Block {
        public static final MapCodec<Kool> CODEC = simpleCodec(Kool::new);
        public static final BooleanProperty GEPLET = BooleanProperty.create("geplet");
        private static final VoxelShape HEEL = Block.box(3, 0, 3, 13, 9, 13);
        private static final VoxelShape PLAT = Block.box(2, 0, 2, 14, 2, 14);
        /** A fall of at least this many blocks squashes a kool. */
        public static final double PLET_VAL = 0.7;

        public Kool(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(GEPLET, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(GEPLET);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(GEPLET) ? PLAT : HEEL;
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            // (a kool is soft: it breaks the fall, like hay)
            entity.causeFallDamage(fallDistance, 0.2f, level.damageSources().fall());
            if (entity instanceof LivingEntity && fallDistance >= PLET_VAL && plet(level, pos, state) && entity instanceof ServerPlayer speler) {
                GidsFeature.grant(speler, "guheinde/einde2_deco_kolen");
            }
        }

        /** Squashes the kool at this spot (server side); false when it already was flat. */
        public static boolean plet(Level level, BlockPos pos, BlockState state) {
            if (state.getValue(GEPLET) || !(level instanceof ServerLevel server)) {
                return false;
            }
            server.setBlock(pos, state.setValue(GEPLET, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.SLIME_BLOCK_FALL, SoundSource.BLOCKS, 0.8f, 0.7f);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 14, 0.25, 0.1, 0.25, 0.05);
            return true;
        }
    }

    /** A wind chime of the air nomads: hangs under a block, tinkles softly by itself and louder when you tap it. */
    public static class Windgong extends Block {
        public static final MapCodec<Windgong> CODEC = simpleCodec(Windgong::new);
        private static final VoxelShape SHAPE = Block.box(4, 1, 4, 12, 16, 12);

        public Windgong(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return direction == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                tingel(level, pos, level.getRandom());
            }
            return InteractionResult.SUCCESS;
        }

        /** Three notes, each a little later in pitch (heard by everyone nearby). */
        public static void tingel(Level level, BlockPos pos, RandomSource random) {
            float toon = 0.9f + random.nextFloat() * 0.5f;
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, toon);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.5f, toon * 1.26f);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            // (the wind of the void: a soft note now and then, only for who stands near)
            if (random.nextInt(28) == 0) {
                level.playLocalSound(pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.22f, 1.1f + random.nextFloat() * 0.7f, false);
            }
        }
    }

    /** The eternal flame of the Zonnekrijgers: a little rainbow fire that gives light and never burns anyone or anything. */
    public static class EeuwigeVlam extends Block {
        public static final MapCodec<EeuwigeVlam> CODEC = simpleCodec(EeuwigeVlam::new);
        private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 12, 12);
        /** The colours of the dragons' fire. */
        private static final int[] KLEUREN = {0xFF5A3C, 0xFFB03C, 0xFFE95A, 0x6AE06A, 0x5AC8FF, 0xB07CFF, 0xFF7CD0};

        public EeuwigeVlam(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return Block.canSupportCenter(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            double x = pos.getX() + 0.5, y = pos.getY() + 0.45, z = pos.getZ() + 0.5;
            if (random.nextInt(2) == 0) {
                level.addParticle(new DustParticleOptions(KLEUREN[random.nextInt(KLEUREN.length)], 0.8f),
                        x + (random.nextDouble() - 0.5) * 0.35, y + random.nextDouble() * 0.4, z + (random.nextDouble() - 0.5) * 0.35, 0, 0.03, 0);
            }
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.2, y + 0.3, z + (random.nextDouble() - 0.5) * 0.2, 0, 0.02, 0);
            }
            if (random.nextInt(30) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.35f, 1.2f + random.nextFloat() * 0.5f, false);
            }
        }
    }

    /** The white lotus tile of Pai Sho: a flat round tile that lies on whatever is flat enough to play on. */
    public static class Lotustegel extends Block {
        public static final MapCodec<Lotustegel> CODEC = simpleCodec(Lotustegel::new);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 1.5, 13);

        public Lotustegel(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return Block.canSupportCenter(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
    }
}
