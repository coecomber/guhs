package nl.juiced.guhs.feature.guhpixel.bioscoop;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.block.GuhFurnitureBlock;

/**
 * Bioscoopstoeltje: a red plush cinema seat. A player sits on it like on any guh furniture (right-click with an empty
 * hand); during a film the guhs nearby pick a free one themselves ({@link BioscoopGoal}). A seat a guh sits on is taken.
 * Its front (FACING) is where the sitter looks: put it with its back to you, facing the screen... or just place it while
 * you look away from the screen, like every chair.
 */
public class StoeltjeBlock extends GuhFurnitureBlock {
    public static final MapCodec<StoeltjeBlock> CODEC = simpleCodec(StoeltjeBlock::new);
    /** How high the cushion is (blocks): a player sits here, a guh a little lower (on the cushion itself). */
    public static final double ZIT = 0.45, KUSSEN = 7 / 16.0;

    public StoeltjeBlock(Properties properties) {
        super(properties, ZIT,
                new double[]{2, 0, 2, 14, 7, 13},      // the cushion on its foot
                new double[]{2, 7, 13, 14, 16, 15});   // the back
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** Where a guh sits: on the cushion, a little to the front. */
    public static Vec3 guhPlek(BlockPos pos, BlockState state) {
        Direction voor = state.getValue(FACING);
        return new Vec3(pos.getX() + 0.5 + voor.getStepX() * 0.06, pos.getY() + KUSSEN, pos.getZ() + 0.5 + voor.getStepZ() * 0.06);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && !player.isSecondaryUseActive() && !player.isPassenger() && Voorstelling.opStoel(level, pos) != null) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.guhbioscoop.stoel.bezet").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
