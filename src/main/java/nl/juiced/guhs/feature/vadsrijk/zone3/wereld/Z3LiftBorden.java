package nl.juiced.guhs.feature.vadsrijk.zone3.wereld;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord;

/**
 * 1.10.10: a name board over both stops of the three honey lifts down into the cells under the ring deck (Nestkapel, cafe,
 * Raattrossen). Nothing told a player that the fenced hole with chains on the deck is a lift. Each board has two lines:
 * <ul>
 *   <li>the lift's name, big, in honey colour ("Kapellift");</li>
 *   <li>a small line under it: "Omlaag naar de Nestkapel · klik op een blok ernaast", or at the bottom "Omhoog naar het ringdek".</li>
 * </ul>
 * The top board floats in the shaft between the chains, the bottom one over the stop. A few wax sparks drift down the shaft while
 * a player is near.
 * Same machinery as the dungeon name boards ({@link DungeonBord#maak}): text displays with one key each (U-DUBBEL); a copy a chunk
 * brings back from disk is refused there, and a fresh one is made here. Every 2 s per stop, only while a player is within
 * {@link #AFSTAND} blocks (so no chunk is ever loaded for it).
 */
public final class Z3LiftBorden {
    /** The lifts that get a board, with their cell ({@code vadsrijk.wereld.z3.liftbord.cel.<lift>}). */
    static final String[] LIFTEN = {"lift_kapel", "lift_cafe", "lift_raattros"};
    static final double AFSTAND = 48;
    static final int KLEUR = 0xFFD27A;

    private Z3LiftBorden() {
    }

    /** Every 2 s (Z3WereldRegistry's ticker). */
    static void tick(ServerLevel sl) {
        for (String id : LIFTEN) {
            Z3WereldPlekken.Lift l = Z3Liften.lift(id);
            if (l == null) {
                continue;
            }
            for (boolean onder : new boolean[] {false, true}) {
                Vec3 v = plek(l, onder);
                if (sl.getNearestPlayer(v.x, v.y, v.z, AFSTAND, false) == null || !Dubbel.klaar(sl, BlockPos.containing(v))) {
                    continue;
                }
                zorg(sl, l, onder, v);
            }
        }
    }

    /**
     * Where a board goes (the bottom of its small line). Top: in the shaft, 3 blocks out from the stop (bouw3.py: the shaft is 3
     * blocks further out from the trunk than the stop), 2 blocks over the deck. Bottom: 2.2 over the stop.
     */
    static Vec3 plek(Z3WereldPlekken.Lift l, boolean onder) {
        if (onder) {
            return Vec3.atBottomCenterOf(l.onder()).add(0, 2.2, 0);
        }
        BlockPos b = l.boven();
        double dx = b.getX() - Z3WereldPlekken.STAM_X, dz = b.getZ() - Z3WereldPlekken.STAM_Z, n = Math.max(1e-3, Math.hypot(dx, dz));
        return new Vec3(b.getX() + 0.5 + dx / n * 3, b.getY() + 2.0, b.getZ() + 0.5 + dz / n * 3);
    }

    /** The two lines of one stop's board. */
    static Component[] regels(Z3WereldPlekken.Lift l, boolean onder) {
        String z = "vadsrijk.wereld.z3.liftbord.";
        Component naam = Component.translatable(z + l.id()).withStyle(s -> s.withBold(true).withColor(KLEUR));
        Component klein = onder ? Component.translatable(z + "omhoog").withStyle(ChatFormatting.WHITE)
                : Component.translatable(z + "omlaag", Component.translatable(z + "cel." + l.id())).withStyle(ChatFormatting.WHITE);
        return new Component[] {naam, klein};
    }

    static String sleutel(Z3WereldPlekken.Lift l, boolean onder, int regel) {
        return "z3liftbord:" + l.id() + ":" + (onder ? "onder" : "boven") + ":" + regel;
    }

    /** Both lines there, on their spot (one each); a few sparks down the shaft. */
    static void zorg(ServerLevel sl, Z3WereldPlekken.Lift l, boolean onder, Vec3 v) {
        Component[] r = regels(l, onder);
        double[] hoog = {DungeonBord.HOOG_ONDER, 0};
        float[] schaal = {1.3f, 0.8f};
        for (int i = 0; i < 2; i++) {
            Vec3 p = v.add(0, hoog[i], 0);
            String k = sleutel(l, onder, i);
            Entity e = Dubbel.zoek(sl, k);
            if (e != null && e.position().distanceToSqr(p) < 0.04) {
                continue;
            }
            if (e != null) {
                e.discard();
            }
            DungeonBord.maak(sl, p, r[i], schaal[i], k);
        }
        if (!onder && sl.getNearestPlayer(v.x, v.y, v.z, 20, false) != null) {
            sl.sendParticles(ParticleTypes.WAX_ON, v.x, v.y - 1.5, v.z, 4, 0.5, 1.2, 0.5, 0.0);
        }
    }

    /** The board entity of one line (gametests, dev), or null. */
    @Nullable
    public static Entity bord(ServerLevel sl, String liftId, boolean onder, int regel) {
        Z3WereldPlekken.Lift l = Z3Liften.lift(liftId);
        return l == null ? null : Dubbel.zoek(sl, sleutel(l, onder, regel));
    }
}
