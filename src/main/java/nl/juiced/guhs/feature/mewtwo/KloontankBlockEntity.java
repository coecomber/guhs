package nl.juiced.guhs.feature.mewtwo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The kloontank's block entity: only there so the client can draw the tank (client.KloontankRenderer). */
public class KloontankBlockEntity extends BlockEntity {
    public KloontankBlockEntity(BlockPos pos, BlockState state) {
        super(MewtwoFeature.KLOONTANK_BE.get(), pos, state);
    }
}
