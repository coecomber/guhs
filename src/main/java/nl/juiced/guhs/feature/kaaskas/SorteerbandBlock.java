package nl.juiced.guhs.feature.kaaskas;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De Guh-sorteerband ({@code guhs:sorteerband}): a low belt you lay in a row. Whatever lies on it (items, and you too, njeg)
 * rides along in the direction the arrows point ({@link #FACING}); at the last piece (no band in front of it) the items wait
 * in a little heap until a guh with the chore Sorteerder ({@link SorteerderKlus}) picks them up and sorts them into the chests.
 * No vadskracht: it is a guh's band.
 */
public class SorteerbandBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SorteerbandBlock> CODEC = simpleCodec(SorteerbandBlock::new);
    private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 4, 16);

    public SorteerbandBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    /** Is this the last piece of a band (nothing that carries on in front of it)? */
    public static boolean isEind(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof SorteerbandBlock)) {
            return false;
        }
        return !(level.getBlockState(pos.relative(s.getValue(FACING))).getBlock() instanceof SorteerbandBlock);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.getY() < pos.getY() + 0.2 || entity.getY() > pos.getY() + 0.6) {
            return;
        }
        Direction richting = state.getValue(FACING);
        if (isEind(level, pos) && entity instanceof ItemEntity) {
            // the end: the items wait on a little heap in the middle
            Vec3 midden = Vec3.atCenterOf(pos);
            entity.setDeltaMovement((midden.x - entity.getX()) * 0.2, entity.getDeltaMovement().y, (midden.z - entity.getZ()) * 0.2);
            return;
        }
        double v = KaaskasGetallen.BAND_SNELHEID;
        Vec3 d = entity.getDeltaMovement();
        // keep it in the middle of the band sideways, push it along
        double dx = richting.getStepX() != 0 ? richting.getStepX() * v : (pos.getX() + 0.5 - entity.getX()) * 0.2;
        double dz = richting.getStepZ() != 0 ? richting.getStepZ() * v : (pos.getZ() + 0.5 - entity.getZ()) * 0.2;
        entity.setDeltaMovement(dx, d.y, dz);
    }
}
