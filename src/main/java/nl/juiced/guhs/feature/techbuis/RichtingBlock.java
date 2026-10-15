package nl.juiced.guhs.feature.techbuis;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Het Richtingstuk ({@code guhs:knabbelbuis_richting}): the piece with the arrow. It needs no vadskracht. Against a chest (or
 * anything that holds items) it nibbles one item at a time out of it and sends it into the tubes; between tubes it lets
 * things through one way only. Use it with an empty hand to read what it is doing; sneak + use turns it around.
 */
public class RichtingBlock extends BuisStukBlock {
    public static final MapCodec<RichtingBlock> CODEC = simpleCodec(RichtingBlock::new);

    public RichtingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BuisStukBlockEntity(TechbuisFeature.RICHTING_BE.get(), pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;   // (so you can build on with a tube in your hand)
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            draaiOm(level, pos, state, player);
        } else if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof BuisStukBlockEntity stuk) {
            sp.sendOverlayMessage(stuk.stand());
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
