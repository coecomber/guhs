package nl.juiced.guhs.feature.bedden;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A player bed of the guhs (1.8.0: Droombed, Guh-hoofdbed, Blub-bed, Wolkenhemelbed): a real two-block bed (sleep, set the
 * spawn point, skip the night; Snoes and the dream know it as a bed), drawn by block models instead of the vanilla bed
 * renderer, so it has no block entity.
 */
public class GuhsBedBlock extends BedBlock {
    public GuhsBedBlock(DyeColor kleur, Properties properties) {
        super(kleur, properties);
    }

    @Override
    @SuppressWarnings("null")
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;   // (its looks come from the block models, not the vanilla bed renderer)
    }
}
