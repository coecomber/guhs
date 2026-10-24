package nl.juiced.guhs.feature.onderwol;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.registry.ModFluids;
import org.jspecify.annotations.Nullable;

/**
 * 1.7.0 "Onder de Wol": the kristalpegel, a pink guhkristal "dripstone" (registered in {@link OnderWolGrot}). It behaves
 * like vanilla's pointed dripstone (whose class checks {@code Blocks.POINTED_DRIPSTONE} by identity, hence this copy):
 * stalactites hang from a sturdy face or another kristalpegel, stalagmites stand on one; the thickness (tip, frustum,
 * middle, base, merged tip) follows the column; a stalactite whose support goes falls down and hurts what it lands on, a
 * stalagmite tip hurts whoever falls on it, a fast trident breaks it.
 * <p>
 * Instead of water or lava a stalactite tip now and then drips a little drop of kaas saus (the honey drip particle, orange
 * yellow); nothing fills a cauldron. A column grows slowly (as vanilla's) when the kristalpegelblok it hangs from has a
 * source of water or kaas saus on top. The tips glow softly.
 */
public class Kristalpegel extends Block implements SimpleWaterloggedBlock, Fallable {
    public static final MapCodec<Kristalpegel> CODEC = simpleCodec(Kristalpegel::new);
    public static final EnumProperty<Direction> TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Chance per animate tick that a free stalactite tip drips a drop of kaas saus. */
    private static final float DRUP_KANS = 0.025f;
    /** Chance per random tick that a fed column grows (vanilla's: about once in five days). */
    private static final float GROEI_KANS = 0.011377778f;
    private static final int MAX_GROEI = 7;
    private static final VoxelShape SHAPE_TIP_MERGE = Block.column(6.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_TIP_UP = Block.column(6.0, 0.0, 11.0);
    private static final VoxelShape SHAPE_TIP_DOWN = Block.column(6.0, 5.0, 16.0);
    private static final VoxelShape SHAPE_FRUSTUM = Block.column(8.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_MIDDLE = Block.column(10.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_BASE = Block.column(12.0, 0.0, 16.0);
    private static final double DRUP_START = SHAPE_TIP_DOWN.min(Direction.Axis.Y);
    private static final float MAX_HORIZONTAL_OFFSET = (float) SHAPE_BASE.min(Direction.Axis.X);

    public Kristalpegel(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIP_DIRECTION, Direction.UP).setValue(THICKNESS, DripstoneThickness.TIP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<Kristalpegel> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    /** The light of a state: the tips glow a little. */
    public static int licht(BlockState state) {
        DripstoneThickness d = state.getValue(THICKNESS);
        return d == DripstoneThickness.TIP || d == DripstoneThickness.TIP_MERGE ? 4 : 0;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return magHier(level, pos, state.getValue(TIP_DIRECTION));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction naar,
                                     BlockPos buurPos, BlockState buur, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (naar != Direction.UP && naar != Direction.DOWN) {
            return state;
        }
        Direction punt = state.getValue(TIP_DIRECTION);
        if (punt == Direction.DOWN && ticks.getBlockTicks().hasScheduledTick(pos, this)) {
            return state;
        }
        if (naar == punt.getOpposite() && !canSurvive(state, level, pos)) {
            // (a stalactite falls two ticks later, a stalagmite breaks on the next)
            ticks.scheduleTick(pos, this, punt == Direction.DOWN ? 2 : 1);
            return state;
        }
        boolean samen = state.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
        return state.setValue(THICKNESS, dikte(level, pos, punt, samen));
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        BlockPos pos = hit.getBlockPos();
        if (level instanceof ServerLevel server && projectile.mayInteract(server, pos) && projectile.mayBreak(server)
                && projectile instanceof ThrownTrident && projectile.getDeltaMovement().length() > 0.6) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2.5, 2.0f, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    /** A free stalactite tip drips a little kaas saus now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kanDruppen(state) && random.nextFloat() < DRUP_KANS) {
            Vec3 offset = state.getOffset(pos);
            level.addParticle(ParticleTypes.DRIPPING_HONEY, pos.getX() + 0.5 + offset.x, pos.getY() + DRUP_START - 0.0625,
                    pos.getZ() + 0.5 + offset.z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isPegel(state, Direction.UP) && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        } else {
            laatVallen(state, level, pos);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < GROEI_KANS && isBovenkant(state, level, pos)) {
            groeiAlsHetKan(state, level, pos, random);
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction gewenst = context.getNearestLookingVerticalDirection().getOpposite();
        Direction punt = magHier(level, pos, gewenst) ? gewenst : magHier(level, pos, gewenst.getOpposite()) ? gewenst.getOpposite() : null;
        if (punt == null) {
            return null;
        }
        return defaultBlockState().setValue(TIP_DIRECTION, punt).setValue(THICKNESS, dikte(level, pos, punt, !context.isSecondaryUseActive()))
                .setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = switch (state.getValue(THICKNESS)) {
            case TIP_MERGE -> SHAPE_TIP_MERGE;
            case TIP -> state.getValue(TIP_DIRECTION) == Direction.DOWN ? SHAPE_TIP_DOWN : SHAPE_TIP_UP;
            case FRUSTUM -> SHAPE_FRUSTUM;
            case MIDDLE -> SHAPE_MIDDLE;
            case BASE -> SHAPE_BASE;
        };
        return shape.move(state.getOffset(pos));
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected float getMaxHorizontalOffset() {
        return MAX_HORIZONTAL_OFFSET;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity entity) {
        if (!entity.isSilent()) {
            level.levelEvent(1045, pos, 0);   // (the dripstone landing sound)
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingStalactite(entity);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // falling, growing (vanilla's logic, with this block in place of pointed dripstone)
    // ------------------------------------------------------------------------------------------------------------------

    /** The whole stalactite below pos falls; its tip hurts like a vanilla one (more the longer it is). */
    private static void laatVallen(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos p = pos.mutable();
        BlockState s = state;
        while (isPegel(s, Direction.DOWN)) {
            FallingBlockEntity vallend = FallingBlockEntity.fall(level, p, s);
            if (isPunt(s, true)) {
                int lengte = Math.max(1 + pos.getY() - p.getY(), 6);
                vallend.setHurtsEntities(lengte, 40);
                break;
            }
            p.move(Direction.DOWN);
            s = level.getBlockState(p);
        }
    }

    private static void groeiAlsHetKan(BlockState boven, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!gevoed(level.getBlockState(pos.above()), level.getBlockState(pos.above(2)))) {
            return;
        }
        BlockPos puntPos = zoekPunt(boven, level, pos, MAX_GROEI, false);
        if (puntPos == null) {
            return;
        }
        BlockState punt = level.getBlockState(puntPos);
        if (kanDruppen(punt) && puntKanGroeien(punt, level, puntPos)) {
            if (random.nextBoolean()) {
                groei(level, puntPos, Direction.DOWN);
            } else {
                groeiStalagmietOnder(level, puntPos);
            }
        }
    }

    /** A column grows when its kristalpegelblok has a source of water or kaas saus on top. */
    private static boolean gevoed(BlockState wortel, BlockState daarboven) {
        FluidState f = daarboven.getFluidState();
        return wortel.is(OnderWolGrot.KRISTALPEGELBLOK.get()) && f.isSource()
                && (f.is(Fluids.WATER) || f.getType().isSame(ModFluids.KAAS_SAUS.get()));
    }

    private static void groeiStalagmietOnder(ServerLevel level, BlockPos boven) {
        BlockPos.MutableBlockPos p = boven.mutable();
        for (int i = 0; i < 10; i++) {
            p.move(Direction.DOWN);
            BlockState s = level.getBlockState(p);
            if (!s.getFluidState().isEmpty()) {
                return;
            }
            if (isLossePunt(s, Direction.UP) && puntKanGroeien(s, level, p)) {
                groei(level, p, Direction.UP);
                return;
            }
            if (magHier(level, p, Direction.UP) && !level.isWaterAt(p.below())) {
                groei(level, p.below(), Direction.UP);
                return;
            }
            if (!s.isAir() && (s.isSolidRender() || !s.getCollisionShape(level, p).isEmpty())) {
                return;
            }
        }
    }

    private static void groei(ServerLevel level, BlockPos van, Direction richting) {
        BlockPos doel = van.relative(richting);
        BlockState daar = level.getBlockState(doel);
        if (isLossePunt(daar, richting.getOpposite())) {
            samenvoegen(daar, level, doel);
        } else if (daar.isAir() || daar.is(net.minecraft.world.level.block.Blocks.WATER)) {
            zet(level, doel, richting, DripstoneThickness.TIP);
        }
    }

    private static void zet(LevelAccessor level, BlockPos pos, Direction richting, DripstoneThickness dikte) {
        level.setBlock(pos, OnderWolGrot.KRISTALPEGEL.get().defaultBlockState().setValue(TIP_DIRECTION, richting).setValue(THICKNESS, dikte)
                .setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER)), Block.UPDATE_ALL);
    }

    private static void samenvoegen(BlockState punt, LevelAccessor level, BlockPos puntPos) {
        BlockPos hangend = punt.getValue(TIP_DIRECTION) == Direction.UP ? puntPos.above() : puntPos;
        zet(level, hangend, Direction.DOWN, DripstoneThickness.TIP_MERGE);
        zet(level, hangend.below(), Direction.UP, DripstoneThickness.TIP_MERGE);
    }

    private static boolean puntKanGroeien(BlockState punt, ServerLevel level, BlockPos puntPos) {
        Direction richting = punt.getValue(TIP_DIRECTION);
        BlockState daar = level.getBlockState(puntPos.relative(richting));
        if (!daar.getFluidState().isEmpty()) {
            return false;
        }
        return daar.isAir() || isLossePunt(daar, richting.getOpposite());
    }

    private static @Nullable BlockPos zoekPunt(BlockState state, LevelAccessor level, BlockPos pos, int max, boolean ookSamen) {
        if (isPunt(state, ookSamen)) {
            return pos;
        }
        Direction richting = state.getValue(TIP_DIRECTION);
        BiPredicate<BlockPos, BlockState> pad = (p, s) -> isPegel(s, richting);
        return zoekVerticaal(level, pos, richting, pad, s -> isPunt(s, ookSamen), max).orElse(null);
    }

    private static Optional<BlockPos> zoekVerticaal(LevelAccessor level, BlockPos pos, Direction richting, BiPredicate<BlockPos, BlockState> pad,
                                                    Predicate<BlockState> doel, int max) {
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int i = 1; i < max; i++) {
            p.move(richting);
            BlockState s = level.getBlockState(p);
            if (doel.test(s)) {
                return Optional.of(p.immutable());
            }
            if (level.isOutsideBuildHeight(p.getY()) || !pad.test(p, s)) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the shape of a column
    // ------------------------------------------------------------------------------------------------------------------

    /** The thickness at pos for a pegel pointing punt (vanilla's calculateDripstoneThickness). */
    public static DripstoneThickness dikte(LevelReader level, BlockPos pos, Direction punt, boolean samenvoegen) {
        Direction voet = punt.getOpposite();
        BlockState voor = level.getBlockState(pos.relative(punt));
        if (isPegel(voor, voet)) {
            return !samenvoegen && voor.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE ? DripstoneThickness.TIP : DripstoneThickness.TIP_MERGE;
        }
        if (!isPegel(voor, punt)) {
            return DripstoneThickness.TIP;
        }
        DripstoneThickness voorDikte = voor.getValue(THICKNESS);
        if (voorDikte == DripstoneThickness.TIP || voorDikte == DripstoneThickness.TIP_MERGE) {
            return DripstoneThickness.FRUSTUM;
        }
        return isPegel(level.getBlockState(pos.relative(voet)), punt) ? DripstoneThickness.MIDDLE : DripstoneThickness.BASE;
    }

    /** May a pegel pointing punt stand at pos (a sturdy face or another such pegel behind it)? */
    public static boolean magHier(LevelReader level, BlockPos pos, Direction punt) {
        BlockPos achter = pos.relative(punt.getOpposite());
        BlockState s = level.getBlockState(achter);
        return s.isFaceSturdy(level, achter, punt) || isPegel(s, punt);
    }

    public static boolean kanDruppen(BlockState state) {
        return isPegel(state, Direction.DOWN) && state.getValue(THICKNESS) == DripstoneThickness.TIP && !state.getValue(WATERLOGGED);
    }

    private static boolean isPunt(BlockState state, boolean ookSamen) {
        if (!(state.getBlock() instanceof Kristalpegel)) {
            return false;
        }
        DripstoneThickness d = state.getValue(THICKNESS);
        return d == DripstoneThickness.TIP || ookSamen && d == DripstoneThickness.TIP_MERGE;
    }

    private static boolean isLossePunt(BlockState state, Direction richting) {
        return isPunt(state, false) && state.getValue(TIP_DIRECTION) == richting;
    }

    private static boolean isBovenkant(BlockState state, LevelReader level, BlockPos pos) {
        return isPegel(state, Direction.DOWN) && !(level.getBlockState(pos.above()).getBlock() instanceof Kristalpegel);
    }

    public static boolean isPegel(BlockState state, Direction punt) {
        return state.getBlock() instanceof Kristalpegel && state.getValue(TIP_DIRECTION) == punt;
    }
}
