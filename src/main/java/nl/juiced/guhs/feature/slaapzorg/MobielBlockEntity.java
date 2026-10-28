package nl.juiced.guhs.feature.slaapzorg;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Mobiel's block entity: only there so client.MobielRenderer can turn its little hanging moon, stars and guh (slowly,
 * faster while the lullaby plays). It keeps no data.
 */
public class MobielBlockEntity extends BlockEntity {
    public MobielBlockEntity(BlockPos pos, BlockState state) {
        super(SlaapzorgFeature.MOBIEL_BE.get(), pos, state);
    }
}
