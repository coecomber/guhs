package nl.juiced.guhs.feature.techmachine;

import java.util.Locale;
import java.util.function.BiConsumer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineDeelBlock;

/**
 * One of the eight other blocks of a Plantagebak: a {@link MachineDeelBlock} (it belongs to its kern, passes clicks and
 * capabilities on, goes when the kern goes) that is NOT invisible: each shows its own piece of the bed (a corner, an
 * edge, the soil in the middle: {@link #STUK}, worked out from where it lies when it is placed). Like the kern it is soil
 * for every plant and is left alone by a growing tree.
 */
public class PlantagebakDeelBlock extends MachineDeelBlock {
    public static final MapCodec<PlantagebakDeelBlock> CODEC = simpleCodec(PlantagebakDeelBlock::new);

    /** Which piece of the bed of 3 x 3 this is, by the points of the compass. */
    public enum Stuk implements StringRepresentable {
        NW, N, NO, W, MIDDEN, O, ZW, Z, ZO;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** dx, dz from the middle of the bed (-1..1). */
        public static Stuk van(int dx, int dz) {
            return values()[(dz + 1) * 3 + (dx + 1)];
        }
    }

    public static final EnumProperty<Stuk> STUK = EnumProperty.create("stuk", Stuk.class);

    public PlantagebakDeelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STUK, Stuk.MIDDEN));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STUK);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Placed (by its kern): it finds out which piece of the bed it is. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockPos kern = kern(state, pos);
        BlockState kernState = level.getBlockState(kern);
        if (!level.isClientSide() && kernState.getBlock() instanceof PlantagebakBlock) {
            BlockPos midden = kern.relative(kernState.getValue(MachineBlock.FACING).getOpposite());
            int dx = pos.getX() - midden.getX(), dz = pos.getZ() - midden.getZ();
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                Stuk stuk = Stuk.van(dx, dz);
                if (state.getValue(STUK) != stuk) {
                    level.setBlock(pos, state.setValue(STUK, stuk), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 0.2f;   // (a solid block again, unlike the invisible parts)
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false;
    }

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
        return facing == Direction.UP ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override
    public boolean onTreeGrow(BlockState state, WorldGenLevel level, BiConsumer<BlockPos, BlockState> placeFunction, RandomSource randomSource,
                              BlockPos pos, TreeConfiguration config) {
        return true;
    }
}
