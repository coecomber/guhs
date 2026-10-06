package nl.juiced.guhs.feature.torenpeper;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * De peperplant: the nether wart of the Guhbarbecuether. It needs no light and no water, only the right ground (block tag
 * guhs:torenpeper/pepergrond), and that ground decides which pepper it carries when it is ripe ({@link PeperSoort},
 * property {@code soort}: kept in step with the block under it).
 * <ul>
 *   <li>It grows on a clock of its own (scheduled ticks, a stage about every minute), three times as fast with glass
 *       somewhere above it: a greenhouse. A plant that has no clock yet (it came with a building) gets one from a random
 *       tick.</li>
 *   <li>A ripe plant is picked with a right-click: {@link #PLUK_MIN} to {@link #PLUK_MAX} peppers, and the plant starts over
 *       from stage 1. Breaking a ripe plant gives the peppers and seeds (loot table); only a broken plant gives seeds.</li>
 * </ul>
 * It is a {@link CropBlock} so that harvesting machines and chores that know crops know this one too
 * ({@link #isMaxAge}, age 0..3).
 */
public class PeperplantBlock extends CropBlock {
    public static final MapCodec<PeperplantBlock> CODEC = simpleCodec(PeperplantBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final EnumProperty<PeperSoort> SOORT = EnumProperty.create("soort", PeperSoort.class);
    /** Ticks per stage in the open and under glass (plus up to a third of it, so a bed doesn't ripen all at once). */
    public static final int GROEI_BUITEN = 1200, GROEI_KAS = 400;
    /** How far above the plant glass still counts as a greenhouse roof. */
    public static final int GLAS_BEREIK = 8;
    public static final int PLUK_MIN = 2, PLUK_MAX = 3;
    private static final VoxelShape[] SHAPES = {Block.column(10.0, 0.0, 5.0), Block.column(12.0, 0.0, 9.0), Block.column(12.0, 0.0, 13.0),
            Block.column(14.0, 0.0, 15.0)};

    public PeperplantBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(SOORT, PeperSoort.GROEN));
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SOORT);
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return TorenpeperFeature.PEPERZAADJES.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    // --- the ground ----------------------------------------------------------------------------------------------------------

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(TorenpeperFeature.PEPERGROND);
    }

    /** No light needed (it grows in the smoke of the Barbecuether): the ground is all that counts. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(SOORT, PeperSoort.vanGrond(context.getLevel().getBlockState(context.getClickedPos().below())));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction richting, BlockPos buurPos,
                                     BlockState buur, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return richting == Direction.DOWN ? state.setValue(SOORT, PeperSoort.vanGrond(buur)) : state;
    }

    /** Whatever put the plant here (a seed, a template, a machine that replants): the kind follows the ground, the clock starts. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server) {
            PeperSoort soort = PeperSoort.vanGrond(level.getBlockState(pos.below()));
            if (state.getValue(SOORT) != soort) {
                level.setBlock(pos, state.setValue(SOORT, soort), Block.UPDATE_CLIENTS);
                return;   // (onPlace comes again for the corrected state)
            }
            zetKlok(server, pos, state);
        }
    }

    // --- growing -----------------------------------------------------------------------------------------------------------------

    /** Is there glass above this spot, with nothing solid in between (a greenhouse)? */
    public static boolean onderGlas(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int i = 0; i < GLAS_BEREIK; i++) {
            p.move(Direction.UP);
            BlockState s = level.getBlockState(p);
            if (s.is(Tags.Blocks.GLASS_BLOCKS) || s.is(Tags.Blocks.GLASS_PANES) || s.is(BlockTags.IMPERMEABLE)) {
                return true;
            }
            if (s.canOcclude()) {
                return false;
            }
        }
        return false;
    }

    /** Ticks until the next stage here. */
    public static int groeitijd(Level level, BlockPos pos) {
        int basis = onderGlas(level, pos) ? GROEI_KAS : GROEI_BUITEN;
        return basis + level.getRandom().nextInt(basis / 3);
    }

    private void zetKlok(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(AGE) < MAX_AGE && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, groeitijd(level, pos));
        }
    }

    /** The plant's own clock: one stage further, and on to the next. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        groei(level, pos, state, 1);
    }

    /** (a plant without a clock: one that came with a building, or whose clock got lost) */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        zetKlok(level, pos, state);
    }

    /** Lets the plant grow this many stages; returns the new state. */
    public BlockState groei(ServerLevel level, BlockPos pos, BlockState state, int stappen) {
        int age = state.getValue(AGE);
        if (age >= MAX_AGE) {
            return state;
        }
        BlockState groter = state.setValue(AGE, Math.min(MAX_AGE, age + stappen)).setValue(SOORT, PeperSoort.vanGrond(level.getBlockState(pos.below())));
        level.setBlock(pos, groter, Block.UPDATE_CLIENTS);
        zetKlok(level, pos, groter);
        return groter;
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            groei(server, pos, state, 1 + level.getRandom().nextInt(2));
        }
    }

    // --- picking -------------------------------------------------------------------------------------------------------------------

    public static boolean isRijp(BlockState state) {
        return state.getBlock() instanceof PeperplantBlock && state.getValue(AGE) >= MAX_AGE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isRijp(state)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (level instanceof ServerLevel server) {
            pluk(server, pos, state, player);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Picks a ripe plant: its peppers pop out, the plant starts over from stage 1. Returns what was picked (empty: not ripe).
     * {@code player} may be null (a harvesting machine, a chore guh).
     */
    public ItemStack pluk(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (!isRijp(state)) {
            return ItemStack.EMPTY;
        }
        PeperSoort soort = state.getValue(SOORT);
        ItemStack pepers = new ItemStack(soort.peper(), PLUK_MIN + level.getRandom().nextInt(PLUK_MAX - PLUK_MIN + 1));
        popResource(level, pos, pepers.copy());
        level.playSound(null, pos, TorenpeperFeature.PLUK.get(), SoundSource.BLOCKS, 1f, 0.9f + level.getRandom().nextFloat() * 0.3f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 5, 0.25, 0.25, 0.25, 0.0);
        BlockState geplukt = state.setValue(AGE, 1);
        level.setBlock(pos, geplukt, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, geplukt));
        zetKlok(level, pos, geplukt);
        if (player instanceof ServerPlayer sp) {
            GuhAdvancements.grant(sp, "toren_peper_geplukt");
        }
        return pepers;
    }
}
