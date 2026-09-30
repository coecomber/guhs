package nl.juiced.guhs.feature.hemel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Knuffelhart's block entity: no data, it only ticks on the client (the heart's beat, its soft music and heartbeat
 * sound near you: client.HemelClient#tickHart) and carries the renderer (client.KnuffelhartRenderer).
 */
public class KnuffelhartBlockEntity extends BlockEntity {
    /** (client) ticks since this heart was loaded: the renderer's clock. */
    public int tijd;

    public KnuffelhartBlockEntity(BlockPos pos, BlockState state) {
        super(HemelFeature.KNUFFELHART_BE.get(), pos, state);
        tijd = Math.floorMod(pos.hashCode(), 40);
    }

    void clientTick() {
        tijd++;
        nl.juiced.guhs.feature.hemel.client.HemelClient.tickHart(this);
    }
}
