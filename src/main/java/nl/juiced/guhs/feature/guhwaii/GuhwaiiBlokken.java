package nl.juiced.guhs.feature.guhwaii;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.world.level.ScheduledTickAccess;
/**
 * The blocks of Guhwai'i (see {@link GuhwaiiFeature}): the guh-palm (trunk, a trunk block with a guh face, fronds, the
 * sprouting coconut), the kokosnoot, the tropical flowers, the Schilly-eitjes, the vadsigheid-scanner and its poster, and
 * the rommeltjes 626-guh leaves behind.
 */
public final class GuhwaiiBlokken {
    public static final IntegerProperty RIJP = IntegerProperty.create("rijp", 0, 2);
    public static final BooleanProperty HANGEND = BooleanProperty.create("hangend");
    public static final BooleanProperty KNIPOOG = BooleanProperty.create("knipoog");
    public static final IntegerProperty EITJES = IntegerProperty.create("eitjes", 1, 4);
    public static final IntegerProperty SOORT = IntegerProperty.create("soort", 0, 3);

    private GuhwaiiBlokken() {
    }

    /** Where Guhwai'i plants grow: sand, grass, dirt. */
    static boolean eilandgrond(BlockState state) {
        return state.is(BlockTags.SAND) || state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL);
    }

    // =================================================================================================================
    // the guh-palm
    // =================================================================================================================

    /** The palm trunk: an axe strips it (to {@code guhwaii_palm_gestript}, same axis). */
    public static class PalmStam extends RotatedPillarBlock {
        public PalmStam(Properties properties) {
            super(properties);
        }

        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (ability == ItemAbilities.AXE_STRIP) {
                return GuhwaiiFeature.PALM_GESTRIPT.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
            }
            return super.getToolModifiedState(state, context, ability, simulate);
        }
    }

    /**
     * A palm trunk block with a guh face on one side (the palms of Guhwai'i each have one, looking out to sea). Click it:
     * it winks at you, "Njeg!".
     */
    public static class PalmGezicht extends HorizontalDirectionalBlock {
        public static final MapCodec<PalmGezicht> CODEC = simpleCodec(PalmGezicht::new);

        public PalmGezicht(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KNIPOOG, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, KNIPOOG);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        /** An axe strips the face away too (like vadshout_gezicht): a plain stripped palm log is left. */
        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (ability == ItemAbilities.AXE_STRIP) {
                return GuhwaiiFeature.PALM_GESTRIPT.get().defaultBlockState();
            }
            return super.getToolModifiedState(state, context, ability, simulate);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (hit.getDirection() != state.getValue(FACING)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(KNIPOOG, true), Block.UPDATE_ALL);
                level.scheduleTick(pos, this, 30);
                level.playSound(null, pos, ModSounds.GUH_AMBIENT.get(), SoundSource.BLOCKS, 0.7f, 1.4f);
                if (level instanceof ServerLevel server) {
                    Direction f = state.getValue(FACING);
                    server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5 + f.getStepX() * 0.7, pos.getY() + 0.9,
                            pos.getZ() + 0.5 + f.getStepZ() * 0.7, 2, 0.2, 0.1, 0.2, 0.01);
                }
                if (player instanceof ServerPlayer sp) {
                    GuhwaiiFeature.advancement(sp, "palm_knipoog");
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (state.getValue(KNIPOOG)) {
                level.setBlock(pos, state.setValue(KNIPOOG, false), Block.UPDATE_ALL);
            }
        }
    }

    /** Palm fronds (leaves: they decay without a trunk nearby); now and then a coconut grows under them. */
    public static class PalmBlad extends LeavesBlock {
        public static final com.mojang.serialization.MapCodec<PalmBlad> CODEC = simpleCodec(PalmBlad::new);

        public PalmBlad(Properties properties) {
            // 26.1: LeavesBlock has falling-leaf particles; palm fronds had none in 1.0.0 (chance 0, no particle)
            super(0f, properties);
        }

        @Override
        public com.mojang.serialization.MapCodec<PalmBlad> codec() {
            return CODEC;
        }

        @Override
        protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            if (!level.getBlockState(pos).is(this) || state.getValue(PERSISTENT)) {
                return;
            }
            // a new coconut under a frond that touches the trunk area (distance 1-2), not too many around
            if (state.getValue(DISTANCE) <= 2 && random.nextInt(40) == 0 && level.isEmptyBlock(pos.below())) {
                int al = 0;
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 0, 3))) {
                    if (level.getBlockState(p).is(GuhwaiiFeature.KOKOSNOOT.get())) {
                        al++;
                    }
                }
                if (al < 4) {
                    level.setBlock(pos.below(), GuhwaiiFeature.KOKOSNOOT.get().defaultBlockState().setValue(HANGEND, true).setValue(RIJP, 0),
                            Block.UPDATE_ALL);
                }
            }
        }
    }

    /** The sprouting coconut: a kokosnoot planted on sand or grass grows into a guh-palm. */
    public static class Kiemplant extends SaplingBlock {
        public Kiemplant(Properties properties) {
            super(GuhwaiiFeature.PALM_GROWER, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return eilandgrond(state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return new ItemStack(GuhwaiiFeature.KOKOSNOOT_ITEM.get());
        }
    }

    // =================================================================================================================
    // the kokosnoot
    // =================================================================================================================

    /**
     * A coconut: hanging under palm fronds (it ripens from green to brown: rijp 0..2) or lying on the ground. A ripe one:
     * click it to pick it, or wait: now and then it drops by itself, bonk! Breaking a ripe one gives the kokosnoot.
     */
    public static class Kokosnoot extends Block implements BonemealableBlock {
        public static final MapCodec<Kokosnoot> CODEC = simpleCodec(Kokosnoot::new);
        private static final VoxelShape HANGT = Block.box(4.5, 6, 4.5, 11.5, 16, 11.5);
        private static final VoxelShape LIGT = Block.box(4.5, 0, 4.5, 11.5, 6.5, 11.5);

        public Kokosnoot(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(RIJP, 0).setValue(HANGEND, true));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(RIJP, HANGEND);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(HANGEND) ? HANGT : LIGT;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            if (state.getValue(HANGEND)) {
                BlockState boven = level.getBlockState(pos.above());
                return boven.is(GuhwaiiFeature.PALM_BLAD.get()) || boven.is(GuhwaiiFeature.PALM_STAM.get());
            }
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos otherPos, BlockState other, RandomSource random) {
            if (!canSurvive(state, level, pos)) {
                if (state.getValue(HANGEND) && state.getValue(RIJP) == 2) {
                    ticks.scheduleTick(pos, this, 1);   // (it falls in its tick instead of vanishing)
                    return state;
                }
                return Blocks.AIR.defaultBlockState();
            }
            return state;
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!canSurvive(state, level, pos)) {
                valt(level, pos, state);
            }
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return state.getValue(HANGEND);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!canSurvive(state, level, pos)) {
                valt(level, pos, state);
                return;
            }
            int rijp = state.getValue(RIJP);
            if (rijp < 2 && random.nextInt(4) == 0) {
                level.setBlock(pos, state.setValue(RIJP, rijp + 1), Block.UPDATE_CLIENTS);
            } else if (rijp == 2 && random.nextInt(30) == 0) {
                valt(level, pos, state);   // bonk!
            }
        }

        /** A ripe coconut falls: it lands as an item (with a soft bonk); an unripe one just disappears. */
        static void valt(ServerLevel level, BlockPos pos, BlockState state) {
            level.removeBlock(pos, false);
            if (state.getValue(RIJP) == 2) {
                Block.popResource(level, pos, new ItemStack(GuhwaiiFeature.KOKOSNOOT_ITEM.get()));
                level.playSound(null, pos, SoundEvents.WOOD_FALL, SoundSource.BLOCKS, 0.8f, 1.3f);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (state.getValue(RIJP) < 2) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    GuhQuests.hint(sp, "gui.guhs.guhwaii.kokos_onrijp");
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                if (state.getValue(HANGEND)) {
                    level.setBlock(pos, state.setValue(RIJP, 0), Block.UPDATE_ALL);
                } else {
                    level.removeBlock(pos, false);
                }
                Block.popResource(level, pos, new ItemStack(GuhwaiiFeature.KOKOSNOOT_ITEM.get()));
                level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8f, 1.2f);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return new ItemStack(GuhwaiiFeature.KOKOSNOOT_ITEM.get());
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return state.getValue(RIJP) < 2;
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            level.setBlock(pos, state.setValue(RIJP, Math.min(2, state.getValue(RIJP) + 1)), Block.UPDATE_CLIENTS);
        }
    }

    // =================================================================================================================
    // flowers
    // =================================================================================================================

    /** A tropical flower (roze hibiscus, plumeria, paradijsvogelbloem, orchidee): grows on sand too. */
    public static class TropischeBloem extends FlowerBlock {
        public TropischeBloem(Holder<MobEffect> effect, float seconds, Properties properties) {
            super(effect, seconds, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return eilandgrond(state) || super.mayPlaceOn(state, level, pos);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(60) == 0) {
                level.addParticle(ParticleTypes.CHERRY_LEAVES, pos.getX() + random.nextDouble(), pos.getY() + 0.7, pos.getZ() + random.nextDouble(),
                        0, 0, 0);
            }
        }
    }

    // =================================================================================================================
    // the rommeltjes of 626-guh
    // =================================================================================================================

    /**
     * A rommeltje (a toppled sand bucket, pillow feathers, coconut shells, a knocked-over flower pot): 626-guh's mess. Only
     * mess, never anything broken! Click it (or break it) to clean it up: Nani-guh counts along (the ohana questline).
     */
    public static class Rommeltje extends Block {
        public static final MapCodec<Rommeltje> CODEC = simpleCodec(Rommeltje::new);
        private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 5, 14);

        public Rommeltje(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(SOORT, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SOORT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos otherPos, BlockState other, RandomSource random) {
            return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                Ohana.opgeruimd(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                Ohana.telOpgeruimd(sp, pos);
            }
            return super.playerWillDestroy(level, pos, state, player);
        }
    }

    // =================================================================================================================
    // the poster
    // =================================================================================================================

    /** The famous picture: the VADSIGHEIDSNIVEAU meter breaking through to ONBEREKENBAAR VAHOEG. Hangs on a wall. */
    public static class Poster extends HorizontalDirectionalBlock {
        public static final MapCodec<Poster> CODEC = simpleCodec(Poster::new);
        private static final VoxelShape N = Block.box(0, 0, 15, 16, 16, 16), S = Block.box(0, 0, 0, 16, 16, 1),
                W = Block.box(15, 0, 0, 16, 16, 16), E = Block.box(0, 0, 0, 1, 16, 16);

        public Poster(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> S;
                case WEST -> W;
                case EAST -> E;
                default -> N;
            };
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction face = context.getClickedFace();
            if (face.getAxis().isHorizontal()) {
                BlockState s = defaultBlockState().setValue(FACING, face);
                return s.canSurvive(context.getLevel(), context.getClickedPos()) ? s : null;
            }
            for (Direction d : context.getNearestLookingDirections()) {
                if (d.getAxis().isHorizontal()) {
                    BlockState s = defaultBlockState().setValue(FACING, d.getOpposite());
                    if (s.canSurvive(context.getLevel(), context.getClickedPos())) {
                        return s;
                    }
                }
            }
            return null;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            Direction f = state.getValue(FACING);
            BlockPos wand = pos.relative(f.getOpposite());
            return level.getBlockState(wand).isFaceSturdy(level, wand, f);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos otherPos, BlockState other, RandomSource random) {
            return dir == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
        }
    }

    /** Block properties helper: a plain little deco block. */
    static BlockBehaviour.Properties klein(BlockBehaviour.Properties p) {
        return p.noOcclusion().instabreak();
    }
}
