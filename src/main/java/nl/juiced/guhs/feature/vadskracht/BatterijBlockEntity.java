package nl.juiced.guhs.feature.vadskracht;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A battery's content ({@link VadsOpslag}): how many VK it holds, saved with the block. The net it stands in does the rest:
 * once per second it puts in what its sources give more than its machines ask, or takes out what they ask more. The block
 * ({@link BatterijBlock}) shows the content in five steps and to a comparator.
 */
public class BatterijBlockEntity extends BlockEntity implements VadsOpslag {
    private long inhoud;

    public BatterijBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public BlockPos vadsPlek() {
        return worldPosition;
    }

    @Override
    public long vadsInhoud() {
        return inhoud;
    }

    @Override
    public long vadsMax() {
        return getBlockState().getBlock() instanceof BatterijBlock batterij ? batterij.max() : 0;
    }

    @Override
    public long vadsLaad(long vk) {
        long erin = Mth.clamp(vk, 0, vadsMax() - inhoud);
        if (erin > 0) {
            zetInhoud(inhoud + erin);
        }
        return erin;
    }

    @Override
    public long vadsOntlaad(long vk) {
        long eruit = Mth.clamp(vk, 0, inhoud);
        if (eruit > 0) {
            zetInhoud(inhoud - eruit);
        }
        return eruit;
    }

    /** Sets the content (clamped to what fits); the block shows the new step and tells its comparators. */
    public void zetInhoud(long vk) {
        long max = vadsMax();
        long nieuw = Mth.clamp(vk, 0, max);
        if (nieuw == inhoud) {
            return;
        }
        inhoud = nieuw;
        setChanged();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        int stap = BatterijBlock.lading(inhoud, max);
        if (state.hasProperty(BatterijBlock.LADING) && state.getValue(BatterijBlock.LADING) != stap) {
            level.setBlock(worldPosition, state.setValue(BatterijBlock.LADING, stap), Block.UPDATE_CLIENTS);
        }
        level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        inhoud = Math.max(0, in.getLongOr("Inhoud", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        uit.putLong("Inhoud", inhoud);
    }
}
