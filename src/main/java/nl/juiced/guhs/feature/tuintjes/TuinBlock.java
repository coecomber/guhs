package nl.juiced.guhs.feature.tuintjes;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;

/**
 * A guh tuintje: a {@link GuhBloempotBlock guh_bloempot} or a {@link GuhMoestuinbakBlock guh_moestuinbak}, with a guh face
 * on the front. Put seeds in it (knabbelzaadjes, theekruidzaadjes, guhbloemzaadjes) and it grows in {@link #RIJP} steps
 * (block state "groei"); right-click it when it's ripe to harvest (the plant stays and grows again).
 * <ul>
 *   <li>By itself it grows slowly ({@link #KANS_DROOG} per random tick); watered ("gewaterd": with the guh_gieter, or a
 *       tamed guh does it, see {@link GuhGietGoal}) much faster ({@link #KANS_NAT}) until the next step.</li>
 *   <li>Guhs singing nearby (KnusSignalen.zang: the ZINGEN emote, the koortje...) make it grow right away now and then
 *       ({@link #zang}): {@link #KANS_ZANG} per song signal (one a second), and after a sung step that plant rests
 *       {@link #ZANG_RUST} ticks before a song can help it again. So singing is a nice extra on top of watering (about
 *       as fast as a watered plant on its own), never a replacement for it.</li>
 * </ul>
 */
public abstract class TuinBlock extends HorizontalDirectionalBlock {
    public static final int RIJP = 3;
    public static final EnumProperty<TuinPlant> PLANT = EnumProperty.create("plant", TuinPlant.class);
    public static final IntegerProperty GROEI = IntegerProperty.create("groei", 0, RIJP);
    public static final BooleanProperty GEWATERD = BooleanProperty.create("gewaterd");
    /** Chance per random tick to grow a step: dry, watered; and per zang signal. */
    public static final float KANS_DROOG = 0.1f, KANS_NAT = 0.35f, KANS_ZANG = 0.04f;
    /** Ticks a plant rests after a step it grew from singing, before singing can help it again (3 minutes). */
    public static final long ZANG_RUST = 3600;
    /** Per level: packed block pos -> game time of the last step that plant grew from singing (not saved: harmless). */
    private static final Map<Level, Long2LongOpenHashMap> ZANG_STAP = new WeakHashMap<>();

    protected TuinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PLANT, TuinPlant.LEEG).setValue(GROEI, 0)
                .setValue(GEWATERD, false));
    }

    protected abstract VoxelShape vorm();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PLANT, GROEI, GEWATERD);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm();
    }

    // --- state helpers ------------------------------------------------------------------------------------------------

    public static boolean isTuin(BlockState state) {
        return state.getBlock() instanceof TuinBlock;
    }

    /** Something is planted and it isn't ripe yet. */
    public static boolean groeit(BlockState state) {
        return isTuin(state) && state.getValue(PLANT) != TuinPlant.LEEG && state.getValue(GROEI) < RIJP;
    }

    public static boolean rijp(BlockState state) {
        return isTuin(state) && state.getValue(PLANT) != TuinPlant.LEEG && state.getValue(GROEI) >= RIJP;
    }

    /** Could use some water (planted, growing, dry). */
    public static boolean dorstig(BlockState state) {
        return groeit(state) && !state.getValue(GEWATERD);
    }

    // --- growing ------------------------------------------------------------------------------------------------------

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return groeit(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (groeit(state) && random.nextFloat() < (state.getValue(GEWATERD) ? KANS_NAT : KANS_DROOG)) {
            groei(level, pos);
        }
    }

    /** One step further (a watered plant is thirsty again after it). */
    public static void groei(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!groeit(state)) {
            return;
        }
        level.setBlock(pos, state.setValue(GROEI, state.getValue(GROEI) + 1).setValue(GEWATERD, false), Block.UPDATE_ALL);
        if (level instanceof ServerLevel server) {
            server.sendParticles(TuintjesFeature.GROEISPRANKEL.get(), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.3, 0.25, 0.3, 0.0);
        }
    }

    /** Waters it: true when it was thirsty. */
    public static boolean water(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!dorstig(state)) {
            return false;
        }
        level.setBlock(pos, state.setValue(GEWATERD, true), Block.UPDATE_ALL);
        if (level instanceof ServerLevel server) {
            server.sendParticles(TuintjesFeature.GIETERDRUPPEL.get(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.25, 0.1, 0.25, 0.0);
        }
        return true;
    }

    /** Guhs sing at pos: the plants around grow now and then (returns how many grew). */
    public static int zang(ServerLevel level, BlockPos pos, int radius) {
        return zang(level, pos, radius, KANS_ZANG);
    }

    /** {@link #zang(ServerLevel, BlockPos, int)} with its own chance per plant (the gametests use 1). */
    static int zang(ServerLevel level, BlockPos pos, int radius, float kans) {
        long now = level.getGameTime();
        Long2LongOpenHashMap stappen = ZANG_STAP.computeIfAbsent(level, l -> new Long2LongOpenHashMap());
        if (stappen.size() > 512) {
            stappen.long2LongEntrySet().removeIf(e -> now - e.getLongValue() >= ZANG_RUST || e.getLongValue() > now);
        }
        int grew = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -2, -radius), pos.offset(radius, 2, radius))) {
            if (groeit(level.getBlockState(p))) {
                level.sendParticles(ParticleTypes.NOTE, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 1, 0.2, 0.1, 0.2, level.getRandom().nextDouble());
                long key = p.asLong();
                if (stappen.containsKey(key)) {
                    long last = stappen.get(key);
                    if (last <= now && now - last < ZANG_RUST) {
                        continue;                                  // (it just grew from a song: it rests a while)
                    }
                }
                if (level.getRandom().nextFloat() < kans) {
                    groei(level, p.immutable());
                    stappen.put(key, now);
                    grew++;
                }
            }
        }
        return grew;
    }

    /** Forgets that the plant at pos grew from singing (new seeds start fresh). */
    public static void vergeetZang(Level level, BlockPos pos) {
        Long2LongOpenHashMap stappen = ZANG_STAP.get(level);
        if (stappen != null) {
            stappen.remove(pos.asLong());
        }
    }

    // --- planting and harvesting ------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.getItem() instanceof GuhGieterItem) {
            return InteractionResult.PASS;       // (the gieter does its own thing)
        }
        TuinPlant plant = TuinPlant.vanZaadje(stack);
        if (plant == null || state.getValue(PLANT) != TuinPlant.LEEG) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            plant(level, pos, plant, (ServerPlayer) player);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Plants seeds (a new page in the tuinboek). */
    public static void plant(Level level, BlockPos pos, TuinPlant plant, @Nullable ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(PLANT, plant).setValue(GROEI, 0).setValue(GEWATERD, false), Block.UPDATE_ALL);
        vergeetZang(level, pos);
        level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1f, 1.1f);
        if (player != null) {
            TuintjesVoortgang.geplant(player, plant);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!rijp(state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            oogst(level, pos, (ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Harvests a ripe plant into the player's pockets (what doesn't fit drops in front of them); returns how many. */
    public static int oogst(Level level, BlockPos pos, @Nullable ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (!rijp(state)) {
            return 0;
        }
        TuinPlant plant = state.getValue(PLANT);
        Item oogst = plant.oogst();
        int n = 2 + level.getRandom().nextInt(2);
        level.setBlock(pos, state.setValue(GROEI, 0).setValue(GEWATERD, false), Block.UPDATE_ALL);
        level.playSound(null, pos, TuintjesFeature.OOGST_GELUID.get(), SoundSource.BLOCKS, 1f, 1f);
        ItemStack stack = new ItemStack(oogst, n);
        ItemStack zaad = level.getRandom().nextFloat() < 0.3f ? new ItemStack(plant.zaadje()) : ItemStack.EMPTY;
        if (player != null) {
            Minigames.give(player, stack);
            Minigames.give(player, zaad);
            TuintjesVoortgang.geoogst(player, plant, n);
        } else {
            Block.popResource(level, pos.above(), stack);
            if (!zaad.isEmpty()) {
                Block.popResource(level, pos.above(), zaad);
            }
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
        }
        return n;
    }
}
