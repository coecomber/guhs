package nl.juiced.guhs.feature.techbuis.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.techbuis.BuisPayloads;
import nl.juiced.guhs.feature.techbuis.Buizen;

/**
 * The items you see rolling through the Knabbelbuizen: the rides the server told this client about
 * ({@code guhs:techbuis_rol}), kept per piece that sent them and drawn by {@link BuisStukRenderer}. A ride knows its whole
 * way, so where the item is right now is only a matter of the time; nothing more comes from the server. A ride is over
 * when its time is up, and is dropped when a tube under the item was broken.
 */
public final class BuisRitten {
    /** One ride on this client. */
    public static final class Rit {
        /** The blocks the item rolls through: the piece, then every tube. */
        public final BlockPos[] pad;
        /** The way as points: the middle of each of those blocks, then the face of the block where the ride ends. */
        public final Vec3[] punten;
        public final ItemStack stack;
        /** The client's game time at which the ride started. */
        public final long vertrek;
        /** A little difference per ride, so two items do not spin in step. */
        public final int zaad;
        boolean weg;

        Rit(BlockPos start, byte[] stappen, ItemStack stack, long vertrek) {
            this.pad = new BlockPos[stappen.length];
            this.punten = new Vec3[stappen.length + 1];
            BlockPos pos = start;
            for (int i = 0; i < stappen.length; i++) {
                pad[i] = pos;
                punten[i] = Vec3.atCenterOf(pos);
                Direction stap = Direction.from3DDataValue(stappen[i]);
                if (i == stappen.length - 1) {
                    punten[i + 1] = punten[i].add(stap.getStepX() * 0.5, stap.getStepY() * 0.5, stap.getStepZ() * 0.5);
                } else {
                    pos = pos.relative(stap);
                }
            }
            this.stack = stack;
            this.vertrek = vertrek;
            this.zaad = (int) (start.asLong() * 31 + vertrek);
        }

        /** How many blocks along its way the item is at this time; negative or past the end: not to be seen. */
        public float voortgang(float nu) {
            return (nu - vertrek) / Buizen.TIKKEN_PER_BLOK;
        }

        public void laatVallen() {
            weg = true;
        }
    }

    private static final Map<BlockPos, List<Rit>> RITTEN = new HashMap<>();
    private static final List<Rit> GEEN = List.of();
    private static Level wereld;

    /** The rides that left from the piece at this spot. */
    public static List<Rit> van(BlockPos stuk) {
        List<Rit> lijst = RITTEN.get(stuk);
        return lijst == null ? GEEN : lijst;
    }

    static void ontvang(BuisPayloads.Rol rol) {
        Level level = Minecraft.getInstance().level;
        if (level == null || rol.stappen().length == 0 || rol.stack().isEmpty()) {
            return;
        }
        if (level != wereld) {
            RITTEN.clear();
            wereld = level;
        }
        RITTEN.computeIfAbsent(rol.start().immutable(), p -> new ArrayList<>()).add(new Rit(rol.start(), rol.stappen(), rol.stack(), level.getGameTime()));
    }

    /** Once per client tick: forget the rides that are over. */
    static void tick() {
        Level level = Minecraft.getInstance().level;
        if (level == null || level != wereld) {
            RITTEN.clear();
            wereld = level;
            return;
        }
        if (RITTEN.isEmpty()) {
            return;
        }
        long nu = level.getGameTime();
        for (Iterator<List<Rit>> per = RITTEN.values().iterator(); per.hasNext();) {
            List<Rit> lijst = per.next();
            lijst.removeIf(rit -> rit.weg || nu < rit.vertrek || rit.voortgang(nu) >= rit.punten.length - 1);
            if (lijst.isEmpty()) {
                per.remove();
            }
        }
    }

    private BuisRitten() {
    }
}
