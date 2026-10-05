package nl.juiced.guhs.feature.guhpixel.reisbureau;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;

/**
 * The Reisbalie ({@code guhs:reisbureau_balie}): right-click opens the trip screen (today's trips, who is away, the
 * reispas). The block holds nothing: every trip and every stored guh lives in the player's own saved data, so breaking a
 * balie never loses anything and any balie collects a returning guh. The counter in the Reisbureau itself is the same
 * block with {@code vast=true}: it cannot be broken (so nobody takes the structure's counter home) and opens the very
 * same screen.
 */
public class BalieBlock extends DecoBlock {
    public static final BooleanProperty VAST = BooleanProperty.create("vast");

    public BalieBlock(Properties properties) {
        super(properties, Block.box(0, 0, 2, 16, 12, 14));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VAST);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            level.playSound(null, pos, ReisbureauSlice.GELUID_BALIE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1f);
            Reizen.open(sp, pos, null);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(VAST) && !player.isCreative() ? 0f : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (state.getValue(VAST) && player instanceof ServerPlayer sp && !sp.isCreative()) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.reisbureau.melding.vast").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
