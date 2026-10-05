package nl.juiced.guhs.feature.guhpixel.grap2;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhpixel.blok.MuurDecoBlock;

/**
 * The Ingelijste brief ({@code guhs:bzg_ingelijste_brief}): the keepsake of Boer zoekt Guh, the thank-you letter of Boer
 * Guhrrit in a wooden frame on the wall. Right-click to read it. (A wall block, not a painting variant.)
 */
public class IngelijsteBriefBlock extends MuurDecoBlock {
    public static final VoxelShape VORM = Block.box(2, 1, 14, 14, 15, 16);
    private static final MapCodec<IngelijsteBriefBlock> CODEC = simpleCodec(IngelijsteBriefBlock::new);

    public IngelijsteBriefBlock(Properties properties) {
        super(properties, VORM);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Bzg.leesIngelijst(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
