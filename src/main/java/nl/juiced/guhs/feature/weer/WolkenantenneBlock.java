package nl.juiced.guhs.feature.weer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.8.0 (vadskracht): a Wolkenantenne (I, II or III) for the Weer vadser. Right-click a Weer vadser with it (I first, then II,
 * then III: it goes in and is gone) or simply put it on top of one: its zone grows to 20 / 35 / 60 blocks
 * ({@link WeerZones#STRALEN}), and it asks more vadskracht ({@link WeerFeature#KRACHTEN}). A small pole with a cloud on top.
 */
public class WolkenantenneBlock extends Block {
    private static final VoxelShape VORM = Block.box(4, 0, 4, 12, 16, 12);

    /** 1, 2 or 3. */
    public final int niveau;

    public WolkenantenneBlock(int niveau, Properties properties) {
        super(properties);
        this.niveau = niveau;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(p -> new WolkenantenneBlock(niveau, p));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }
}
