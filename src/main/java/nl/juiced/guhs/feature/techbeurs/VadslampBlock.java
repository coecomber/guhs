package nl.juiced.guhs.feature.techbeurs;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;

/**
 * 1.7.0 (ronde 45-47): {@code guhs:vadslamp}, the Vadslamp. A little lantern with a guh face on 1 vadskracht (the early
 * tier: Guhrad + Guhdraad is enough). Asleep (dark) without vadskracht; with it the face wakes up, the lamp burns at light
 * level 15 and it lights a much bigger area than a normal lamp: {@link VadslampBlockEntity} keeps invisible light blocks
 * ({@link VadslampLicht}) in the air around it while it has power.
 * <p>
 * A machine like every other ({@link MachineBlock}: facing + face, the hover readout, the nets), one block, no slots.
 */
public class VadslampBlock extends MachineBlock {
    public static final MapCodec<VadslampBlock> CODEC = simpleCodec(VadslampBlock::new);
    private static final VoxelShape VORM = Shapes.or(Block.box(4, 0, 4, 12, 11, 12), Block.box(5, 11, 6, 11, 13, 9));

    public VadslampBlock(Properties p) {
        super(p);
    }

    /** The light of the lamp itself: 15 while it has vadskracht (its face is awake), else none. */
    public static int licht(BlockState state) {
        return state.hasProperty(SNOET) && state.getValue(SNOET) != Snoet.SLAAPT ? 15 : 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VadslampBlockEntity(pos, state);
    }

    /** The machine's own tick, then the lamp's: its light blocks. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<T> machine = super.getTicker(level, state, type);
        if (level.isClientSide()) {
            return machine;
        }
        return (l, pos, s, be) -> {
            if (machine != null) {
                machine.tick(l, pos, s, be);
            }
            if (be instanceof VadslampBlockEntity lamp) {
                lamp.lampTick();
            }
        };
    }
}
