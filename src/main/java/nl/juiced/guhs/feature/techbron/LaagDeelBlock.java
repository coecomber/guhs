package nl.juiced.guhs.feature.techbron;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.MachineDeelBlock;

/**
 * A part block that is only a low slab ({@link MachineDeelBlock} is a whole block): the rest of the Knuffelgenerator's
 * cushion ({@code guhs:techbron_kussen_deel}) and of the Disco-dynamo's dance floor ({@code guhs:techbron_vloer_deel}), so
 * guhs (and players) step onto them. The cushion's parts are invisible like every part (the kern's model draws the whole
 * cushion); a floor part draws its own tile, because a block model cannot reach three blocks far.
 */
public class LaagDeelBlock extends MachineDeelBlock {
    private final VoxelShape vorm;
    private final boolean zichtbaar;
    private final MapCodec<LaagDeelBlock> codec;

    /**
     * @param hoogte    how high the slab is, in pixels (1..16)
     * @param zichtbaar the part has a block model of its own (false: the kern draws it)
     */
    public LaagDeelBlock(Properties properties, int hoogte, boolean zichtbaar) {
        super(properties);
        this.vorm = Block.box(0, 0, 0, 16, hoogte, 16);
        this.zichtbaar = zichtbaar;
        this.codec = simpleCodec(p -> new LaagDeelBlock(p, hoogte, zichtbaar));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return zichtbaar ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm;
    }
}
