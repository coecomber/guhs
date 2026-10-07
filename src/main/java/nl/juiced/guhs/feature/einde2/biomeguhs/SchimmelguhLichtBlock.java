package nl.juiced.guhs.feature.einde2.biomeguhs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * einde2, slice biomeguhs: {@code guhs:schimmelguh_licht}, the light of a tamed Schimmelguh. Nothing but light: no shape, no model,
 * no item, and anything may be placed in it (replaceable). {@link Schimmellamp} puts it where the guh's head is; every
 * {@link #KIJK} ticks it looks whether a glowing Schimmelguh is still next to it and turns back into air when not (also
 * after a chunk comes back from disk: the scheduled tick is saved with it).
 */
public class SchimmelguhLichtBlock extends Block {
    /** The light it gives; how often it checks its guh (ticks); how far the guh may be (blocks around this block). */
    public static final int LICHT = 12, KIJK = 10;
    public static final double BIJ = 2.6;      // (the lamp lets its light lag Schimmellamp.SPELING blocks behind)

    public SchimmelguhLichtBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, KIJK);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(BIJ), g -> g.isAlive() && Schimmellamp.gloeit(g)).isEmpty()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else {
            level.scheduleTick(pos, this, KIJK);
        }
    }
}
