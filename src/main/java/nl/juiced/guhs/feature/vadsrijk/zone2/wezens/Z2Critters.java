package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Critter;

/**
 * Keeps a few critters around every player in zone 2 (outside Kandijkom too: they are harmless): up to {@link #PER_SPELER}
 * within 40 blocks, one new one every 5 s at most, 14-34 blocks away on the surface. Snow hares and ice birds by day, crystal
 * moths at night (and a few by day). They leave by themselves when nobody is within 80 blocks.
 */
public final class Z2Critters {
    public static final int PER_SPELER = 6;

    private Z2Critters() {
    }

    public static void tick(ServerLevel sl) {
        if (sl.getGameTime() % 100 != 37) {
            return;
        }
        RandomSource r = sl.getRandom();
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || !Z2WezensPlekken.ZONE.bevat(p.getX(), p.getZ())) {
                continue;
            }
            int n = sl.getEntitiesOfClass(Critter.class, new AABB(p.blockPosition()).inflate(40, 24, 40)).size();
            if (n >= PER_SPELER) {
                continue;
            }
            double a = r.nextDouble() * Math.PI * 2, d = 14 + r.nextDouble() * 20;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (!sl.isLoaded(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos onder = new BlockPos(x, y - 1, z);
            if (y <= sl.getMinY() + 1 || !sl.getFluidState(onder).isEmpty() || Math.abs(y - p.getY()) > 20) {
                continue;
            }
            boolean nacht = sl.isDarkOutside();
            EntityType<? extends Critter> type;
            int w = r.nextInt(10);
            if (nacht) {
                type = w < 7 ? Z2WezensRegistry.KRISTALMOT.get() : Z2WezensRegistry.SNEEUWHAAS.get();
            } else {
                type = w < 5 ? Z2WezensRegistry.SNEEUWHAAS.get() : w < 9 ? Z2WezensRegistry.IJSVOGEL.get() : Z2WezensRegistry.KRISTALMOT.get();
            }
            Critter c = type.create(sl, EntitySpawnReason.NATURAL);
            if (c == null) {
                continue;
            }
            c.snapTo(x + 0.5, y + (c.vliegt() ? 1.5 : 0), z + 0.5, r.nextFloat() * 360, 0);
            sl.addFreshEntity(c);
        }
    }
}
