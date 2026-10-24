package nl.juiced.guhs.feature.onderwol;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.7.0 "Onder de Wol": the block classes of the new underground that do more than stand there (registered in
 * {@link OnderWolFeature}): the glitter vine of the Glitterspelonk (with or without berries), the plants on the cave
 * floors, and the soft pluiskussen of the Pluisgrot.
 */
public final class OnderWolBlokken {
    // ------------------------------------------------------------------------------------------------------------------
    /** A glowing vine hanging from a cave ceiling (or from another glitterrank). Breaks when what it hangs on goes. */
    public static class Glitterrank extends Block {
        public static final MapCodec<Glitterrank> CODEC = simpleCodec(Glitterrank::new);
        private static final VoxelShape SHAPE = Block.column(10, 0, 16);

        public Glitterrank(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Glitterrank> codec() {
            return CODEC;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            BlockState boven = level.getBlockState(pos.above());
            return boven.getBlock() instanceof Glitterrank || boven.isFaceSturdy(level, pos.above(), Direction.DOWN);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction naar,
                                         BlockPos buurPos, BlockState buur, RandomSource random) {
            if (naar == Direction.UP && !canSurvive(state, level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, level, ticks, pos, naar, buurPos, buur, random);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(9) == 0) {
                level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0, -0.01, 0);
            }
        }
    }

    /** The berry end of a glitterrank: pick it (empty hand) for glitterbessen, and it grows new ones later. */
    public static class GlitterrankBes extends Glitterrank {
        public static final MapCodec<GlitterrankBes> CODEC = simpleCodec(GlitterrankBes::new);

        public GlitterrankBes(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Glitterrank> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                Block.popResource(server, pos, new ItemStack(OnderWolFeature.GLITTERBES.get(), 1 + server.getRandom().nextInt(2)));
                level.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 1.2f);
                level.setBlock(pos, OnderWolFeature.GLITTERRANK.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A small plant on a cave floor: the glinsterpaddenstoel, the pluisbolletje. Stands on anything solid. */
    public static class Grotplantje extends VegetationBlock {
        public static final MapCodec<Grotplantje> CODEC = simpleCodec(p -> new Grotplantje(10, 8, false, p));
        private final VoxelShape shape;
        private final boolean pluis;

        public Grotplantje(int breed, int hoog, boolean pluis, BlockBehaviour.Properties properties) {
            super(properties);
            this.shape = Block.column(breed, 0, hoog);
            this.pluis = pluis;
        }

        @Override
        protected MapCodec<? extends VegetationBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.isFaceSturdy(level, pos, Direction.UP);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.shape.move(state.getOffset(pos));
        }

        /** The pluisbolletje: whoever walks through it blows up a few fluff seeds. */
        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
            if (pluis && level.isClientSide() && entity instanceof LivingEntity && entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4
                    && level.getRandom().nextInt(4) == 0) {
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + level.getRandom().nextDouble(), pos.getY() + 0.4,
                            pos.getZ() + level.getRandom().nextDouble(), 0, 0.05, 0);
                }
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (!pluis && random.nextInt(14) == 0) {
                level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 0, 0.02, 0);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A big soft cushion of fluff: you land on it without hurting yourself and bounce up a little. */
    public static class Pluiskussen extends Block {
        public static final MapCodec<Pluiskussen> CODEC = simpleCodec(Pluiskussen::new);

        public Pluiskussen(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Pluiskussen> codec() {
            return CODEC;
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            // (no fall damage: it is a cushion)
        }

        @Override
        public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
            if (entity.isSuppressingBounce()) {
                super.updateEntityMovementAfterFallOn(level, entity);
                return;
            }
            Vec3 v = entity.getDeltaMovement();
            if (v.y < -0.1) {
                // a small bounce, softer than slime
                entity.setDeltaMovement(v.x, -v.y * 0.45, v.z);
            } else {
                super.updateEntityMovementAfterFallOn(level, entity);
            }
        }
    }

    private OnderWolBlokken() {
    }
}
