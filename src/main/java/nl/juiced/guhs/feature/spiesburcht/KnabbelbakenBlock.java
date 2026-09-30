package nl.juiced.guhs.feature.spiesburcht;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Het Knabbelbaken: the beacon of the guhs. On a pyramid of vads, kaas and knabbel blocks (tag
 * guhs:knabbelbaken_basis, 1 to 4 layers) it gives a guh effect to you and to every tamed guh nearby. Right-click to
 * pick the effect ({@link KnabbelbakenBlockEntity.Gunst}); a bigger pyramid reaches further and unlocks more.
 */
public class KnabbelbakenBlock extends BaseEntityBlock {
    public static final MapCodec<KnabbelbakenBlock> CODEC = simpleCodec(KnabbelbakenBlock::new);

    public KnabbelbakenBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnabbelbakenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpiesburchtFeature.KNABBELBAKEN_BE.get(), KnabbelbakenBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof KnabbelbakenBlockEntity baken && player instanceof ServerPlayer sp) {
            baken.cycle(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.HEART, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.1, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0, 0.02, 0);
        }
    }
}
