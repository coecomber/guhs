package nl.juiced.guhs.feature.hemel;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Het Knuffelhart: a glowing pink heart under a little glass dome on a golden pedestal (block model: the pedestal and the
 * dome; the heart itself is drawn beating by client.KnuffelhartRenderer). It only exists in the Hemelkapelletje: not
 * craftable, not minable (hardness -1, no loot, no item), explosion proof, can't be pushed.
 * <ul>
 *   <li>right-click: the revive screen once the heart beats for you ({@link Hemel#openScherm}), otherwise "it's asleep";</li>
 *   <li>right-click with a Herinnering star: brings exactly that guh back ({@link Hemel#ster}).</li>
 * </ul>
 */
public class KnuffelhartBlock extends BaseEntityBlock {
    public static final MapCodec<KnuffelhartBlock> CODEC = simpleCodec(KnuffelhartBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(2.5, 4, 2.5, 13.5, 16, 13.5));

    public KnuffelhartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;   // no item: it only exists in the chapel
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnuffelhartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, HemelFeature.KNUFFELHART_BE.get(), (l, p, s, be) -> be.clientTick()) : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(HemelFeature.HERINNERING.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof ServerPlayer sp) {
            Hemel.ster(sp, pos, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            if (!Hemel.openScherm(sp, pos)) {
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 0.7f);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Little sparkles and now and then a heart, day and night. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.6, z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(HemelFeature.STERRETJE.get(), x + (random.nextDouble() - 0.5) * 1.2, y + random.nextDouble() * 0.8,
                    z + (random.nextDouble() - 0.5) * 1.2, 0, 0.01, 0);
        }
        if (random.nextInt(24) == 0) {
            level.addParticle(ParticleTypes.HEART, x + (random.nextDouble() - 0.5) * 0.6, y + 0.9, z + (random.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
        }
    }
}
