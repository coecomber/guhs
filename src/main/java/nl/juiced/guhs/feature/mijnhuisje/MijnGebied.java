package nl.juiced.guhs.feature.mijnhuisje;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * 1.7.0 (ronde 37-40): the area a Mijn-guhhuisje mines out, pure geometry (no level).
 * <ul>
 *   <li>Columns: {@code rx} blocks to each side on x and {@code rz} on z around the huisje ({@link #MAX_ZIJ} at most), so at
 *       most 41 x 41.</li>
 *   <li>Layers: from {@link #boven()} (the layer just under the huisje) down to {@link #bodem()} (never below
 *       {@link #MIN_Y}), mined one whole layer at a time, top first, so nothing is ever dug out from under a block that is
 *       still to be mined.</li>
 *   <li>Three columns are special: the {@link #isPilaar pilaar} under the huisje (never mined: the huisje stays on it, the
 *       ladder hangs on it and a lampje glows in it every few layers), the {@link #isLadder ladder} column in front of
 *       the door (mined, and a ladder put in every layer) and the lampjes along the edge ({@link #isRandLampje}).</li>
 * </ul>
 * Positions are walked by index: layer {@code y}, index {@code i} in 0 .. {@link #perLaag()} (row by row, x first).
 */
public record MijnGebied(BlockPos huisje, Direction voor, int rx, int rz, int bodemInstelling, int minY) {
    /** At most this many blocks to each side. */
    public static final int MAX_ZIJ = 20;
    /** Never below this y (design: "tot y 10"). */
    public static final int MIN_Y = 10;
    /** A lampje in the pilaar every this many layers, and along the edge every {@link #RAND_STAP} blocks. */
    public static final int LAMP_LAGEN = 5, RAND_STAP = 8;

    public MijnGebied {
        rx = Mth.clamp(rx, 0, MAX_ZIJ);
        rz = Mth.clamp(rz, 0, MAX_ZIJ);
        if (voor.getAxis().isVertical()) {
            voor = Direction.NORTH;
        }
    }

    /** The area of a huisje with its settings, with the real {@link #MIN_Y}. */
    public static MijnGebied van(BlockPos huisje, Direction voor, int rx, int rz, int bodem) {
        return new MijnGebied(huisje, voor, rx, rz, bodem, MIN_Y);
    }

    /** The top layer: right under the huisje. */
    public int boven() {
        return huisje.getY() - 1;
    }

    /** The lowest layer that is mined: the setting, never below {@link #minY} and never above the top. */
    public int bodem() {
        return Math.min(boven(), Math.max(minY, bodemInstelling));
    }

    /** Is there anything to mine at all (the top is not below the floor)? */
    public boolean leeg() {
        return boven() < minY;
    }

    public int breedte() {
        return 2 * rx + 1;
    }

    public int diepte() {
        return 2 * rz + 1;
    }

    /** Positions per layer. */
    public int perLaag() {
        return breedte() * diepte();
    }

    /** Position {@code i} of layer {@code y}. */
    public BlockPos plek(int y, int i) {
        int dx = i % breedte() - rx, dz = i / breedte() - rz;
        return new BlockPos(huisje.getX() + dx, y, huisje.getZ() + dz);
    }

    /** Inside the area (any layer from the top to the floor)? */
    public boolean binnen(BlockPos p) {
        return Math.abs(p.getX() - huisje.getX()) <= rx && Math.abs(p.getZ() - huisje.getZ()) <= rz && p.getY() <= boven() && p.getY() >= bodem()
                && boven() >= minY;
    }

    /** The column under the huisje: never mined. */
    public boolean isPilaar(BlockPos p) {
        return p.getX() == huisje.getX() && p.getZ() == huisje.getZ();
    }

    /** The column in front of the door: a ladder on the pilaar all the way down. */
    public boolean isLadder(BlockPos p) {
        return p.getX() == huisje.getX() + voor.getStepX() && p.getZ() == huisje.getZ() + voor.getStepZ();
    }

    /** The layers where the pilaar gets a lampje (counted from the top, the third layer first). */
    public boolean isLampLaag(int y) {
        return (boven() - y) % LAMP_LAGEN == 2;
    }

    /**
     * A lampje along the edge of the area: on the outer ring, every {@link #RAND_STAP} blocks along it, in the lamp layers
     * (it stays hanging against the wall when the layers under it are mined).
     */
    public boolean isRandLampje(BlockPos p) {
        if (!isLampLaag(p.getY()) || rx < 2 || rz < 2) {
            return false;
        }
        int dx = p.getX() - huisje.getX(), dz = p.getZ() - huisje.getZ();
        boolean randX = Math.abs(dx) == rx, randZ = Math.abs(dz) == rz;
        if (randX && randZ) {
            return true;                                   // (the corners)
        }
        return randX && Math.floorMod(dz, RAND_STAP) == 0 || randZ && Math.floorMod(dx, RAND_STAP) == 0;
    }

    /** The positions of the outline for "laat zien": the four edges of the top and the floor, and the four corner posts. */
    public java.util.List<net.minecraft.world.phys.Vec3> rand(int stap) {
        java.util.List<net.minecraft.world.phys.Vec3> uit = new java.util.ArrayList<>();
        double x0 = huisje.getX() - rx, x1 = huisje.getX() + rx + 1.0, z0 = huisje.getZ() - rz, z1 = huisje.getZ() + rz + 1.0;
        double[] hoogtes = {boven() + 1.0, bodem()};
        for (double y : hoogtes) {
            for (double x = x0; x <= x1; x += stap) {
                uit.add(new net.minecraft.world.phys.Vec3(x, y, z0));
                uit.add(new net.minecraft.world.phys.Vec3(x, y, z1));
            }
            for (double z = z0; z <= z1; z += stap) {
                uit.add(new net.minecraft.world.phys.Vec3(x0, y, z));
                uit.add(new net.minecraft.world.phys.Vec3(x1, y, z));
            }
        }
        for (double y = bodem(); y <= boven() + 1.0; y += Math.max(stap, 2)) {
            uit.add(new net.minecraft.world.phys.Vec3(x0, y, z0));
            uit.add(new net.minecraft.world.phys.Vec3(x1, y, z0));
            uit.add(new net.minecraft.world.phys.Vec3(x0, y, z1));
            uit.add(new net.minecraft.world.phys.Vec3(x1, y, z1));
        }
        return uit;
    }
}
