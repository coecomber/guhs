package nl.juiced.guhs.feature.vadsrijk.wereld;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The scar plant: a little flower (cross model) that is grey and withered until the zone is healed (tint, see {@link Littekens}). */
public class PlantBlok extends Block {
    public static final MapCodec<PlantBlok> CODEC = simpleCodec(PlantBlok::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);

    public PlantBlok(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 o = state.getOffset(pos);
        return SHAPE.move(o.x, o.y, o.z);
    }
}
