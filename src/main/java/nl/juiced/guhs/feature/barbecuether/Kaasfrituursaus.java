package nl.juiced.guhs.feature.barbecuether;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidType;
import nl.juiced.guhs.registry.ModFluids;

import net.minecraft.world.entity.InsideBlockEffectApplier;
/**
 * Kaasfrituursaus: boiling cheese frying sauce, the lava of the Barbecuether. It glows, burns you like lava, sets things
 * on fire, flows faster in the heat of the Barbecuether (like lava in the Nether) and fills the big "lava seas".
 * <ul>
 *   <li>with water or kaassaus next to it: a source becomes grillkool, flowing sauce becomes houtskoolsteen
 *       (like lava + water = obsidian / cobblestone)</li>
 *   <li>kaassaus flowing onto or against a block of coal turns that coal into grillkool (the way to make portal
 *       frames without ever having been in the Barbecuether)</li>
 * </ul>
 */
public final class Kaasfrituursaus {
    /** The fluid type: like lava (can't swim, sinks slowly, burns). */
    public static FluidType createType() {
        return new FluidType(FluidType.Properties.create()
                .descriptionId("block.guhs.kaasfrituursaus")
                .canSwim(false)
                .canDrown(false)
                .canExtinguish(false)
                .pathType(PathType.LAVA)
                .adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .lightLevel(15)
                .density(3000)
                .viscosity(6000)
                .temperature(1300)) {
            @Override
            public double motionScale(Entity entity) {
                return fast(entity.level()) ? 0.007D : 0.0023333333333333335D;
            }

            @Override
            public void setItemMovement(ItemEntity entity) {
                Vec3 v = entity.getDeltaMovement();
                entity.setDeltaMovement(v.x * 0.95F, v.y + (v.y < 0.06F ? 5.0E-4F : 0.0F), v.z * 0.95F);
            }

            /** Moving through it: the same thick, slow movement as lava (vanilla only does that for lava itself). */
            @Override
            public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
                double y0 = entity.getY();
                entity.moveRelative(0.02F, movementVector);
                entity.move(net.minecraft.world.entity.MoverType.SELF, entity.getDeltaMovement());
                if (entity.getFluidTypeHeight(this) <= entity.getFluidJumpThreshold()) {
                    entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
                    entity.setDeltaMovement(entity.getFluidFallingAdjustedMovement(gravity, entity.getDeltaMovement().y <= 0.0, entity.getDeltaMovement()));
                } else {
                    entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
                }
                if (gravity != 0.0) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, -gravity / 4.0, 0.0));
                }
                Vec3 v = entity.getDeltaMovement();
                if (entity.horizontalCollision && entity.isFree(v.x, v.y + 0.6F - entity.getY() + y0, v.z)) {
                    entity.setDeltaMovement(v.x, 0.3F, v.z);
                }
                return true;
            }
        };
    }

    /** 1.1.0: the dimension's "fast lava" (26.1 environment attribute; was DimensionType#ultraWarm, the nether). */
    static boolean fast(LevelReader level) {
        return level.environmentAttributes().getDimensionValue(net.minecraft.world.attribute.EnvironmentAttributes.FAST_LAVA);
    }

    static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(BarbecuetherFeature.KAASFRITUURSAUS_TYPE, BarbecuetherFeature.KAASFRITUURSAUS,
                BarbecuetherFeature.FLOWING_KAASFRITUURSAUS)
                .bucket(BarbecuetherFeature.KAASFRITUURSAUS_BUCKET)
                .block(BarbecuetherFeature.KAASFRITUURSAUS_BLOCK)
                .tickRate(30).slopeFindDistance(2).levelDecreasePerBlock(2).explosionResistance(100f);
    }

    /** What lava does, for our sauce: faster in the heat, sets things on fire, pops and bubbles. */
    private static void lavaRandomTick(ServerLevel level, BlockPos pos) {
        if (!(level.getGameRules().get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER) != 0)) {
            return;
        }
        RandomSource random = level.getRandom();
        int i = random.nextInt(3);
        if (i > 0) {
            BlockPos p = pos;
            for (int j = 0; j < i; j++) {
                p = p.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                if (!level.isLoaded(p)) {
                    return;
                }
                BlockState state = level.getBlockState(p);
                if (state.isAir()) {
                    for (Direction d : Direction.values()) {
                        if (flammable(level, p.relative(d), d.getOpposite())) {
                            level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p));
                            return;
                        }
                    }
                } else if (state.blocksMotion()) {
                    return;
                }
            }
        } else {
            for (int k = 0; k < 3; k++) {
                BlockPos p = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                if (!level.isLoaded(p)) {
                    return;
                }
                if (level.isEmptyBlock(p.above()) && flammable(level, p, Direction.UP)) {
                    level.setBlockAndUpdate(p.above(), BaseFireBlock.getState(level, p.above()));
                }
            }
        }
    }

    private static boolean flammable(LevelReader level, BlockPos pos, Direction face) {
        if (pos.getY() >= level.getMinY() && pos.getY() < level.getMaxY() + 1 && !level.hasChunkAt(pos)) {
            return false;
        }
        return level.getBlockState(pos).ignitedByLava(level, pos, face);
    }

    private static void lavaAnimateTick(Level level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && !level.getBlockState(above).isSolidRender()) {
            if (random.nextInt(100) == 0) {
                double x = pos.getX() + random.nextDouble(), y = pos.getY() + 1.0, z = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
                level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
            if (random.nextInt(60) == 0) {   // a little cheesy steam
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0, 0.03, 0);
            }
            if (random.nextInt(200) == 0) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS,
                        0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
        }
    }

    /** Is this water or kaassaus (what cools the frying sauce down)? */
    static boolean cooling(FluidState state) {
        return state.getFluidType() == NeoForgeMod.WATER_TYPE.value() || state.getFluidType() == ModFluids.KAAS_SAUS_TYPE.get();
    }

    public static class Source extends BaseFlowingFluid.Source {
        public Source() {
            super(properties());
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return fast(level) ? 10 : 30;
        }

        @Override
        protected int getSlopeFindDistance(LevelReader level) {
            return fast(level) ? 4 : 2;
        }

        @Override
        protected int getDropOff(LevelReader level) {
            return fast(level) ? 1 : 2;
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
            lavaRandomTick(level, pos);
        }

        @Override
        protected void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
            lavaAnimateTick(level, pos, random);
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            level.levelEvent(1501, pos, 0);
        }

        @Override
        protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
            if (spreadIntoCooling(level, pos, blockState, direction)) {
                return;
            }
            super.spreadTo(level, pos, blockState, direction, fluidState);
        }
    }

    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing() {
            super(properties());
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return fast(level) ? 10 : 30;
        }

        @Override
        protected int getSlopeFindDistance(LevelReader level) {
            return fast(level) ? 4 : 2;
        }

        @Override
        protected int getDropOff(LevelReader level) {
            return fast(level) ? 1 : 2;
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
            lavaRandomTick(level, pos);
        }

        @Override
        protected void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
            lavaAnimateTick(level, pos, random);
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            level.levelEvent(1501, pos, 0);
        }

        @Override
        protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
            if (spreadIntoCooling(level, pos, blockState, direction)) {
                return;
            }
            super.spreadTo(level, pos, blockState, direction, fluidState);
        }
    }

    /** Falling onto water or kaassaus: that becomes houtskoolsteen (like lava onto water makes stone). */
    private static boolean spreadIntoCooling(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction) {
        if (direction == Direction.DOWN && cooling(level.getFluidState(pos))) {
            if (blockState.getBlock() instanceof LiquidBlock) {
                level.setBlock(pos, BarbecuetherFeature.HOUTSKOOLSTEEN.get().defaultBlockState(), 3);
            }
            level.levelEvent(1501, pos, 0);
            return true;
        }
        return false;
    }

    /** The fluid block: burns whoever is in it, like lava. */
    public static class SausBlock extends LiquidBlock {
        public SausBlock(FlowingFluid fluid, Properties properties) {
            super(fluid, properties);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
            if (!level.isClientSide() && entity.getBoundingBox().minY < pos.getY() + state.getFluidState().getHeight(level, pos)) {
                burn(entity);
            }
            super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
        }
    }

    /** What the sauce does to you: like lava (on fire for 15 s, 4 damage), unless you're fire resistant. */
    public static void burn(Entity entity) {
        if (entity instanceof ItemEntity item) {
            if (!item.fireImmune() && item.tickCount % 10 == 0) {
                item.hurtOrSimulate(item.damageSources().lava(), 4f);
            }
            return;
        }
        entity.lavaIgnite();   // (26.1: lavaHurt no longer sets you on fire; 1.21.1 lavaHurt did both)
        entity.lavaHurt();
        entity.fallDistance *= 0.5F;
    }

    // --- mixing with water / kaassaus / coal --------------------------------------------------------------------------

    /** Called once at setup: the fluid interactions of the sauce and of kaassaus. */
    public static void registerInteractions() {
        // frying sauce + water or kaassaus next to it: source -> grillkool, flowing -> houtskoolsteen
        FluidInteractionRegistry.addInteraction(BarbecuetherFeature.KAASFRITUURSAUS_TYPE.get(), new FluidInteractionRegistry.InteractionInformation(
                (level, currentPos, relativePos, currentState) -> cooling(level.getFluidState(relativePos)),
                fluid -> fluid.isSource() ? BarbecuetherFeature.GRILLKOOL.get().defaultBlockState()
                        : BarbecuetherFeature.HOUTSKOOLSTEEN.get().defaultBlockState()));
        // kaassaus flowing over or against a block of coal: the coal bakes into grillkool
        FluidInteractionRegistry.addInteraction(ModFluids.KAAS_SAUS_TYPE.get(), new FluidInteractionRegistry.InteractionInformation(
                (level, currentPos, relativePos, currentState) -> isCoal(level.getBlockState(relativePos)) || isCoal(level.getBlockState(currentPos.below())),
                (level, currentPos, relativePos, currentState) -> {
                    BlockPos coal = isCoal(level.getBlockState(relativePos)) ? relativePos : currentPos.below();
                    bakeCoal(level, coal);
                }));
    }

    static boolean isCoal(BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.COAL_BLOCK);
    }

    /** A block of coal under / next to kaassaus becomes grillkool, with a hiss and a puff of smoke. */
    public static void bakeCoal(Level level, BlockPos coal) {
        level.setBlockAndUpdate(coal, BarbecuetherFeature.GRILLKOOL.get().defaultBlockState());
        level.levelEvent(1501, coal, 0);
    }

    /** (for the game tests) */
    public static boolean isSauce(Fluid fluid) {
        return fluid.isSame(BarbecuetherFeature.KAASFRITUURSAUS.get());
    }

    private Kaasfrituursaus() {
    }
}
