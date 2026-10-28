package nl.juiced.guhs.feature.raket;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Lanceerplatform (guhs:lanceerplatform): click it to hand in the current stage of the Kaasraket (everything of it that
 * you carry), or to call your astronaut crew, or (all four done) to launch; after the first launch a click flies you to the
 * Kaasmaan. Sneak-click: what the current stage still needs. While a stage is open, a ghost of it twinkles above the platform.
 */
public class LanceerplatformBlock extends Block implements EntityBlock {
    public static final MapCodec<LanceerplatformBlock> CODEC = simpleCodec(LanceerplatformBlock::new);

    public LanceerplatformBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof LanceerplatformBlockEntity be) {
            Raket.klik(sl, pos, be, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof LanceerplatformBlockEntity be) {
            Raket.klik(sl, pos, be, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LanceerplatformBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != RaketFeature.LANCEERPLATFORM_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> {
            if (l.getGameTime() % 40 == 7 && l instanceof ServerLevel sl && be instanceof LanceerplatformBlockEntity pl
                    && pl.bezig() <= RaketStappen.STAPPEN.size() && sl.getNearestPlayer(p.getX(), p.getY(), p.getZ(), 32, false) != null) {
                RaketBouw.spook(sl, p, pl.bezig());
            }
        };
    }
}
