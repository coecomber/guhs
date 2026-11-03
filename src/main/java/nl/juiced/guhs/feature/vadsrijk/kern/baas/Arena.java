package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import net.minecraft.core.BlockPos;

/**
 * A boss arena: id, its centre (where the boss spawns, on the floor), radius (inside = in the fight: HP scaling, intro,
 * contribution), the door (where fainted fighters wake up), the respawn time after a defeat, where the countdown board
 * hangs, and the boss's title lang key (for the board).
 */
public record Arena(String id, BlockPos centrum, int straal, BlockPos deur, int respawnSeconden, BlockPos bord, String titelKey) {
    public boolean binnen(double x, double y, double z) {
        double dx = x - (centrum.getX() + 0.5), dz = z - (centrum.getZ() + 0.5);
        return dx * dx + dz * dz <= (double) straal * straal && Math.abs(y - centrum.getY()) < 24;
    }
}
