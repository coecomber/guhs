package nl.juiced.guhs.feature.bleekwoud;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import nl.juiced.guhs.world.VoorIedereen;

/**
 * Het Krakend Guhhartje (and its soured twin, het Verzuurd Guhhartje): our Creaking Heart, with vanilla's mechanics. It sits
 * in a bleekhout trunk and only works with a bleekhout log on both ends along its axis ({@code uprooted} without them). By
 * day it sleeps ({@code dormant}); at night it wakes up ({@code awake}: its little heart beats) and calls its own creature:
 * the plain heart a {@link KraakguhEntity}, the soured one a {@link KraakMikaEntity}. Break it and its creature crumbles
 * away; it drops kaashars (silk touch: the heart itself), and a natural one a little xp.
 */
public class GuhhartjeBlock extends BaseEntityBlock {
    public static final MapCodec<GuhhartjeBlock> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.BOOL.fieldOf("verzuurd").forGetter(b -> b.verzuurd), propertiesCodec()).apply(i, GuhhartjeBlock::new));
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /** uprooted (no logs on both ends), dormant (day) or awake (night). */
    public static final EnumProperty<CreakingHeartState> STATE = BlockStateProperties.CREAKING_HEART_STATE;
    /** Grown in a tree by the world (those give xp), not placed by a player. */
    public static final BooleanProperty NATURAL = BlockStateProperties.NATURAL;

    private final boolean verzuurd;

    public GuhhartjeBlock(boolean verzuurd, Properties properties) {
        super(properties);
        this.verzuurd = verzuurd;
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(STATE, CreakingHeartState.UPROOTED).setValue(NATURAL, false));
    }

    @Override
    public MapCodec<GuhhartjeBlock> codec() {
        return CODEC;
    }

    /** An awake heart glows softly (you can find it in the dark trunk at night). */
    public static int licht(BlockState state) {
        return state.getValue(STATE) == CreakingHeartState.AWAKE ? 7 : 0;
    }

    /** The soured heart: it calls a Kraak-Mika. */
    public boolean verzuurd() {
        return verzuurd;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuhhartjeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || state.getValue(STATE) == CreakingHeartState.UPROOTED) {
            return null;
        }
        return createTickerHelper(type, BleekwoudFeature.GUHHARTJE_BE.get(), GuhhartjeBlockEntity::serverTick);
    }

    /** The heartbeat of an awake heart (client side, like vanilla's idle sound: no ticking for it). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(STATE) == CreakingHeartState.AWAKE && random.nextInt(12) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, BleekwoudFeature.HART_KLOP.get(), SoundSource.BLOCKS,
                    0.7f, verzuurd ? 0.8f : 1.0f, false);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        ticks.scheduleTick(pos, this, 1);
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState updated = updateState(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    /** An uprooted heart that got its two logs starts working (asleep or awake, by the time of day). */
    private static BlockState updateState(BlockState state, Level level, BlockPos pos) {
        boolean uprooted = state.getValue(STATE) == CreakingHeartState.UPROOTED;
        return uprooted && hasRequiredLogs(state, level, pos)
                ? state.setValue(STATE, BleekwoudBlocks.nacht(level, pos) ? CreakingHeartState.AWAKE : CreakingHeartState.DORMANT)
                : state;
    }

    /** A bleekhout log on both ends along the heart's axis, lying the same way. */
    public static boolean hasRequiredLogs(BlockState state, LevelReader level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        for (Direction dir : axis.getDirections()) {
            BlockState neighbour = level.getBlockState(pos.relative(dir));
            if (!neighbour.is(BleekwoudFeature.STAMMEN) || !neighbour.hasProperty(AXIS) || neighbour.getValue(AXIS) != axis) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateState(defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis()), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, STATE, NATURAL);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        if (level.getBlockEntity(pos) instanceof GuhhartjeBlockEntity hart && explosion instanceof ServerExplosion
                && explosion.getBlockInteraction().shouldAffectBlocklikeEntities()) {
            hart.wezenWeg(true);
            if (explosion.getIndirectSourceEntity() instanceof Player player) {
                tryAwardExperience(player, state, level, pos);
            }
        }
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof GuhhartjeBlockEntity hart) {
            hart.wezenWeg(true);
            tryAwardExperience(player, state, level, pos);
            if (player instanceof ServerPlayer sp) {
                VoorIedereen.shown(sp, "guhmension/bleekwoud_hartje");
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void tryAwardExperience(Player player, BlockState state, Level level, BlockPos pos) {
        if (!player.preventsBlockDrops() && !player.isSpectator() && state.getValue(NATURAL) && level instanceof ServerLevel server) {
            popExperience(server, pos, level.getRandom().nextIntBetweenInclusive(6, 10));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** A comparator reads how close the creature is to its heart (15: right next to it), like vanilla. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (state.getValue(STATE) == CreakingHeartState.UPROOTED) {
            return 0;
        }
        return level.getBlockEntity(pos) instanceof GuhhartjeBlockEntity hart ? hart.signaal() : 0;
    }
}
