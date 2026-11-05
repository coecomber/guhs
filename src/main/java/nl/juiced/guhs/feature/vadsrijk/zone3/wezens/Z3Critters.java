package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Critter;

/**
 * Keeps a few critters around every player in zone 3 (in the towns too: they are harmless): up to {@link #PER_SPELER} within 32
 * blocks (3D: the layers are stacked), one new one every 5 s at most, 10-26 blocks away on a floor near the player's own height.
 * Which kind depends on the layer and the time: honey bees in the crown, on the branches and in Honingtak by day; lantern
 * fireflies at night everywhere and always below; spore moths on the ground and in the dark; root mice on the ground, in the trunk
 * and the caves.
 */
public final class Z3Critters {
    public static final int PER_SPELER = 6;

    private Z3Critters() {
    }

    public static void tick(ServerLevel sl) {
        if (sl.getGameTime() % 100 != 53) {
            return;
        }
        RandomSource r = sl.getRandom();
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || !Z3WezensPlekken.inZone(p.getX(), p.getZ())) {
                continue;
            }
            if (sl.getEntitiesOfClass(Z3Critter.class, new AABB(p.blockPosition()).inflate(32, 16, 32)).size() >= PER_SPELER) {
                continue;
            }
            double a = r.nextDouble() * Math.PI * 2, d = 10 + r.nextDouble() * 16;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (!sl.isLoaded(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = vloer(sl, x, z, p.getBlockY() + 8, p.getBlockY() - 12);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            EntityType<Z3Critter> type = soort(sl, p, r);
            Z3Critter c = type.create(sl, EntitySpawnReason.NATURAL);
            if (c == null) {
                continue;
            }
            c.snapTo(x + 0.5, y + (c.vliegt() ? 1.5 : 0), z + 0.5, r.nextFloat() * 360, 0);
            sl.addFreshEntity(c);
        }
    }

    /** Which critter suits where this player is now. */
    static EntityType<Z3Critter> soort(ServerLevel sl, ServerPlayer p, RandomSource r) {
        boolean nacht = sl.isDarkOutside();
        int w = r.nextInt(10);
        double y = p.getY(), rr = Z3WezensPlekken.r(p.getX(), p.getZ());
        // (P-VERHUIS) the revamped layers: the roots hall under the trunk, the hollow trunk (r < 26, 55-232), branches + crown from 140
        if (y < 64 && rr <= 160) {   // Wortelduister
            return w < 5 ? Z3WezensRegistry.LANTAARNKEVER.get() : w < 9 ? Z3WezensRegistry.SPORENMOT.get() : Z3WezensRegistry.WORTELMUIS.get();
        }
        if (rr <= 26 && y >= 55 && y < 232) {   // the hollow trunk
            return w < 6 ? Z3WezensRegistry.WORTELMUIS.get() : Z3WezensRegistry.LANTAARNKEVER.get();
        }
        if (!nacht && bijKorf(p)) {  // a bee swarm by the beekeepers' hives (POI v2 BIJENKORVEN)
            return w < 9 ? Z3WezensRegistry.HONINGBIJ.get() : Z3WezensRegistry.SPORENMOT.get();
        }
        if (y >= 140) {              // crown, branches, Honingtak
            if (nacht) {
                return w < 7 ? Z3WezensRegistry.LANTAARNKEVER.get() : Z3WezensRegistry.HONINGBIJ.get();
            }
            return w < 8 ? Z3WezensRegistry.HONINGBIJ.get() : Z3WezensRegistry.SPORENMOT.get();
        }
        if (nacht) {                 // the ground
            return w < 6 ? Z3WezensRegistry.LANTAARNKEVER.get() : w < 9 ? Z3WezensRegistry.SPORENMOT.get() : Z3WezensRegistry.WORTELMUIS.get();
        }
        return w < 5 ? Z3WezensRegistry.WORTELMUIS.get() : w < 7 ? Z3WezensRegistry.HONINGBIJ.get() : Z3WezensRegistry.SPORENMOT.get();
    }

    /** Within 30 blocks of a beekeepers' hive (the swarm spots of the map). */
    static boolean bijKorf(ServerPlayer p) {
        for (BlockPos b : nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.BIJENKORVEN) {
            if (b.distToCenterSqr(p.getX(), p.getY(), p.getZ()) < 30 * 30) {
                return true;
            }
        }
        return false;
    }

    /** The first standing spot searching down (solid floor, 2 free blocks, no fluid), or MIN_VALUE. */
    static int vloer(ServerLevel sl, int x, int z, int boven, int onder) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = boven; y >= onder; y--) {
            m.set(x, y - 1, z);
            if (sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() || !sl.getFluidState(m).isEmpty()) {
                continue;
            }
            m.set(x, y, z);
            boolean vrij1 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() && sl.getFluidState(m).isEmpty();
            m.set(x, y + 1, z);
            boolean vrij2 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty();
            if (vrij1 && vrij2) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
