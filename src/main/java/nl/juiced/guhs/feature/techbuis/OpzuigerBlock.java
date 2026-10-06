package nl.juiced.guhs.feature.techbuis;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * De Opzuiger ({@code guhs:opzuiger}): a guh machine with a big round snoet that slurps up the loose items around it
 * ({@link OpzuigerBlockEntity}). Use it to look inside; a Richtingstuk, Filterstuk or hopper takes the items out.
 */
public class OpzuigerBlock extends MachineBlock {
    public static final MapCodec<OpzuigerBlock> CODEC = simpleCodec(OpzuigerBlock::new);

    public OpzuigerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OpzuigerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof OpzuigerBlockEntity opzuiger) {
            sp.openMenu(opzuiger);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
