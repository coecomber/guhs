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
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;

/**
 * The Brievenbus ({@code guhs:bzg_brievenbus}): the mailbox of Boer zoekt Guh. In the show it holds the three letters for
 * Boer Guhrrit; at home (it can be crafted) it is a decoration that never has post ("Geen post vandaag, njeg").
 */
public class BrievenbusBlock extends DecoBlock {
    public static final VoxelShape VORM = Block.box(4, 0, 4, 12, 16, 12);
    private static final MapCodec<BrievenbusBlock> CODEC = simpleCodec(BrievenbusBlock::new);

    public BrievenbusBlock(Properties properties) {
        super(properties, VORM);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Bzg.brievenbus(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
