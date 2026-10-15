package nl.juiced.guhs.feature.bestaand;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of a bridge fire holds nothing: it only tells {@link Vuren} that this fire bowl is loaded (and gone
 * again), so the lit state can be shown to the players who lit it without searching the world for fire bowls.
 */
public class VuurkorfBlockEntity extends BlockEntity {
    public VuurkorfBlockEntity(BlockPos pos, BlockState state) {
        super(BestaandFeature.VUURKORF_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            Vuren.geladen(server, worldPosition);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) {
            Vuren.geladen(server, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) {
            Vuren.weg(server, worldPosition);
        }
        super.setRemoved();
    }
}
