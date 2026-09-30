package nl.juiced.guhs.feature.creche;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * guhs:guh_wiegje: a little crib with a guh face on its headboard. In the Knuffelcreche the babyguhtjes sleep in them
 * ({@link Baby}): during the care round a crib shows what its baby wants ({@link Wens}: a bubble above it), during the
 * minigame the babies crawl out and have to be put back ({@link CrecheGame}). At home you rock an empty crib (right-click):
 * a slaapliedje, sleepy stars, and the baby guhs around it grow a little (once in a while).
 */
public class WiegjeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WiegjeBlock> CODEC = simpleCodec(WiegjeBlock::new);

    /** Who lies in the crib: nobody, an awake baby, or a baby tucked in under its blanket. */
    public enum Baby implements StringRepresentable {
        LEEG, WAKKER, INGESTOPT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What the baby wants now (the care round; the bubble above the crib). */
    public enum Wens implements StringRepresentable {
        GEEN, KNUTSELEN, HONGER, LUIER, SLAAP, LIEDJE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** lang gui.guhs.creche.wens.&lt;id&gt; */
        public String key() {
            return "gui.guhs.creche.wens." + getSerializedName();
        }
    }

    public static final EnumProperty<Baby> BABY = EnumProperty.create("baby", Baby.class);
    public static final EnumProperty<Wens> WENS = EnumProperty.create("wens", Wens.class);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 2, 1, 15, 9, 15), Block.box(1, 0, 1, 3, 2, 3), Block.box(13, 0, 1, 15, 2, 3),
            Block.box(1, 0, 13, 3, 2, 15), Block.box(13, 0, 13, 15, 2, 15));

    public WiegjeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BABY, Baby.LEEG).setValue(WENS, Wens.GEEN));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BABY, WENS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && CrecheGame.opWiegje((ServerLevel) level, pos, sp, stack)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            if (!CrecheGame.opWiegje((ServerLevel) level, pos, sp, ItemStack.EMPTY)) {
                CrecheGame.wieg((ServerLevel) level, pos, sp);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(BABY) == Baby.INGESTOPT && random.nextInt(6) == 0) {
            level.addParticle(CrecheFeature.SLAAPSTERRETJE.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.8,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
        } else if (state.getValue(WENS) != Wens.GEEN && random.nextInt(4) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.02, 0.01, (random.nextDouble() - 0.5) * 0.02);
        }
    }
}
