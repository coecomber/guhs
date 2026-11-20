package nl.juiced.guhs.feature.slijmhuisje;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import nl.juiced.guhs.feature.techmachine.PlantagebakDeelBlock.Stuk;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineDeelBlock;

/**
 * One of the 26 other blocks of a Slijmhuisje: a {@link MachineDeelBlock} (belongs to its kern, passes clicks and
 * capabilities on, goes when the kern goes) that knows which piece of the 3 x 3 it is ({@link #STUK}, by the compass, the
 * same pieces as the Plantagebak's bed) and on which layer ({@link MachineDeelBlock#DY}). The blockstate picks the look
 * from both: the base on layer 0, the dome on the middle block of layer 1, nothing elsewhere (tools/features/slijmhuisje.py).
 */
public class SlijmhuisjeDeelBlock extends MachineDeelBlock {
    public static final MapCodec<SlijmhuisjeDeelBlock> CODEC = simpleCodec(SlijmhuisjeDeelBlock::new);
    public static final EnumProperty<Stuk> STUK = EnumProperty.create("stuk", Stuk.class);

    public SlijmhuisjeDeelBlock(Properties properties) {
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

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        int i = state.getValue(STUK).ordinal();
        BlockPos r = new BlockPos(i % 3 - 1, 0, i / 3 - 1).rotate(rotation);
        return super.rotate(state, rotation).setValue(STUK, Stuk.van(r.getX(), r.getZ()));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        int i = state.getValue(STUK).ordinal(), dx = i % 3 - 1, dz = i / 3 - 1;
        if (mirror == Mirror.LEFT_RIGHT) {
            dz = -dz;
        } else if (mirror == Mirror.FRONT_BACK) {
            dx = -dx;
        }
        return super.mirror(state, mirror).setValue(STUK, Stuk.van(dx, dz));
    }

    /** Placed (by its kern): it finds out which piece of the 3 x 3 it is. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockPos kern = kern(state, pos);
        BlockState kernState = level.getBlockState(kern);
        if (!level.isClientSide() && kernState.getBlock() instanceof SlijmhuisjeBlock) {
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
}
