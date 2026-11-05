package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import net.minecraft.core.BlockPos;

/**
 * A boss arena: id, its centre (where the boss spawns, on the floor), radius (inside = in the fight: HP scaling, intro,
 * contribution), the door (where fainted fighters wake up), the respawn time after a defeat, where the countdown board
 * hangs, and the boss's title lang key (for the board).
 * <p>(M-BAZEN) {@code onder}/{@code boven}: the height window around the centre (inside = {@code -onder <= dy < boven}).
 * The old 7-argument constructor keeps the old window (|dy| &lt; 24), so stacked arenas (zone 3's zeppelin decks) can be
 * given narrower windows without touching the others.
 */
public record Arena(String id, BlockPos centrum, int straal, BlockPos deur, int respawnSeconden, BlockPos bord, String titelKey, int onder, int boven) {
    public static final int STANDAARD_HOOGTE = 24;

    public Arena(String id, BlockPos centrum, int straal, BlockPos deur, int respawnSeconden, BlockPos bord, String titelKey) {
        this(id, centrum, straal, deur, respawnSeconden, bord, titelKey, STANDAARD_HOOGTE, STANDAARD_HOOGTE);
    }

    public boolean binnen(double x, double y, double z) {
        double dx = x - (centrum.getX() + 0.5), dz = z - (centrum.getZ() + 0.5), dy = y - centrum.getY();
        return dx * dx + dz * dz <= (double) straal * straal && dy > -onder && dy < boven;
    }

    /** The same arena with another radius / height window. */
    public Arena met(int straal, int onder, int boven) {
        return new Arena(id, centrum, straal, deur, respawnSeconden, bord, titelKey, onder, boven);
    }

    /** Do the two arenas share any space? */
    public boolean overlapt(Arena o) {
        double dx = centrum.getX() - o.centrum.getX(), dz = centrum.getZ() - o.centrum.getZ();
        if (dx * dx + dz * dz >= Math.pow(straal + o.straal, 2)) {
            return false;
        }
        int a0 = centrum.getY() - onder, a1 = centrum.getY() + boven, b0 = o.centrum.getY() - o.onder, b1 = o.centrum.getY() + o.boven;
        return a0 < b1 && b0 < a1;   // (open intervals)
    }
}
