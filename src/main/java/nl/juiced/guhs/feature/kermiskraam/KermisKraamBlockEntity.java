package nl.juiced.guhs.feature.kermiskraam;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Looks every 5 seconds whether its stall guh still stands behind the counter, and puts one there when it does not. */
public class KermisKraamBlockEntity extends BlockEntity {
    private static final int ELKE = 100;

    public KermisKraamBlockEntity(BlockPos pos, BlockState state) {
        super(KermisKraamFeature.KRAAM_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KermisKraamBlockEntity be) {
        if (level instanceof ServerLevel server && Math.floorMod(level.getGameTime() + pos.hashCode(), ELKE) == 0
                && server.areEntitiesLoaded(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4))) {
            KermisKraam.zorgVoorGuh(server, pos);
        }
    }
}
