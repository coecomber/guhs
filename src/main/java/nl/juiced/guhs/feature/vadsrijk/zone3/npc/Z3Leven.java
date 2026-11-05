package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;

/**
 * (P-VERHUIS, ronde 20 "meer zichtbaar leven") Pluimguh riders on the outer riding spiral: a few moving figures (a Pluimguh prop
 * with a guh prop on its back, {@link Z3Figurant}: no physics, no hitbox, never saved) that ride the spiral up and down while a
 * player is near the tree. They only spawn and ride within {@link #ZICHT} blocks of a player (loaded chunks) and vanish when nobody
 * is near. The path is {@link Z3NpcPlekken#rijpad()} (the map's riding spiral), snapped onto the real floor.
 */
public final class Z3Leven {
    public static final int RUITERS = 3, ZICHT = 96, BOOM_STRAAL = 260;
    public static final double SNELHEID = 0.3;
    private static final String[] PLUIM = {"roze", "mint", "lavendel", "honing"};
    private static final String[] GUH = {"guh", "guh_golden", "guh_bloesemguh", "guh_choco"};

    private static final class Ruiter {
        Z3Figurant pluim, guh;
        double s;
        int richting;
    }

    private static final List<Ruiter> RUITERS_NU = new ArrayList<>();
    private static List<Vec3> pad;
    private static double[] cum;
    private static int teller;

    private Z3Leven() {
    }

    /** Is any player near the tree (horizontal BOOM_STRAAL around the trunk, above the void)? */
    @Nullable
    static List<ServerPlayer> dichtbij(ServerLevel sl) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && Math.hypot(p.getX() - Z3NpcPlekken.TX, p.getZ() - Z3NpcPlekken.TZ) < BOOM_STRAAL && p.getY() > 40) {
                l.add(p);
            }
        }
        return l;
    }

    public static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        List<ServerPlayer> spelers = dichtbij(sl);
        if (spelers.isEmpty()) {
            if (!RUITERS_NU.isEmpty()) {
                opruimen();
            }
            return;
        }
        if (pad == null) {
            pad = Z3NpcPlekken.rijpad();
            cum = new double[pad.size()];
            for (int i = 1; i < pad.size(); i++) {
                cum[i] = cum[i - 1] + pad.get(i).distanceTo(pad.get(i - 1));
            }
        }
        if (pad.size() < 2) {
            return;
        }
        // drop riders that got lost (removed, or far from everyone); add new ones near a player now and then
        RUITERS_NU.removeIf(r -> {
            boolean weg = r.pluim == null || r.pluim.isRemoved() || r.guh == null || r.guh.isRemoved() || verVan(spelers, r.pluim.position());
            if (weg) {
                weg(r);
            }
            return weg;
        });
        if (RUITERS_NU.size() < RUITERS && ++teller % 100 == 0) {
            nieuw(sl, spelers);
        }
        double lengte = cum[cum.length - 1];
        for (Ruiter r : RUITERS_NU) {
            r.s += SNELHEID * r.richting;
            if (r.s >= lengte || r.s <= 0) {
                r.richting = -r.richting;
                r.s = Math.max(0, Math.min(lengte, r.s));
            }
            Vec3 v = vloer(sl, op(r.s));
            r.pluim.zet(v);
            r.guh.zet(v.add(0, 1.55, 0));
            r.guh.setYRot(r.pluim.getYRot());
        }
    }

    private static boolean verVan(List<ServerPlayer> spelers, Vec3 v) {
        for (ServerPlayer p : spelers) {
            if (p.position().distanceToSqr(v) < (ZICHT + 32) * (ZICHT + 32)) {
                return false;
            }
        }
        return true;
    }

    private static void nieuw(ServerLevel sl, List<ServerPlayer> spelers) {
        ServerPlayer p = spelers.get(sl.getRandom().nextInt(spelers.size()));
        List<Integer> kans = new ArrayList<>();
        for (int i = 0; i < pad.size(); i++) {
            double d = pad.get(i).distanceToSqr(p.position());
            if (d < ZICHT * ZICHT && d > 20 * 20 && sl.isLoaded(BlockPos.containing(pad.get(i)))) {
                kans.add(i);
            }
        }
        if (kans.isEmpty()) {
            return;
        }
        int i = kans.get(sl.getRandom().nextInt(kans.size()));
        Vec3 v = vloer(sl, pad.get(i));
        int k = sl.getRandom().nextInt(PLUIM.length);
        Ruiter r = new Ruiter();
        r.s = cum[i];
        r.richting = sl.getRandom().nextBoolean() ? 1 : -1;
        r.pluim = Z3Figurant.maak(sl, v, 0, "vr_eco_pluimguh", "textures/entity/vr_eco_pluimguh_" + PLUIM[k] + ".png", "vr_eco_pluimguh", "ren", 1.0f);
        r.guh = Z3Figurant.maak(sl, v.add(0, 1.55, 0), 0, "guh", "textures/entity/" + GUH[sl.getRandom().nextInt(GUH.length)] + ".png", "guh",
                "animation.guh.sit", 0.8f);
        if (r.pluim == null || r.guh == null) {
            weg(r);
            return;
        }
        RUITERS_NU.add(r);
    }

    /** The point at arc length s along the path. */
    static Vec3 op(double s) {
        int i = 1;
        while (i < cum.length - 1 && cum[i] < s) {
            i++;
        }
        double seg = cum[i] - cum[i - 1];
        double f = seg <= 0 ? 0 : Math.max(0, Math.min(1, (s - cum[i - 1]) / seg));
        return pad.get(i - 1).lerp(pad.get(i), f);
    }

    /** The first standable floor at most 4 above / 6 below v (the path y is the stand y; the bark may bulge). */
    static Vec3 vloer(ServerLevel sl, Vec3 v) {
        BlockPos b = BlockPos.containing(v);
        if (!sl.isLoaded(b)) {
            return v;
        }
        for (int dy = 4; dy >= -6; dy--) {
            BlockPos s = b.offset(0, dy - 1, 0);
            if (sl.getBlockState(s).isFaceSturdy(sl, s, Direction.UP) && sl.getBlockState(s.above()).getCollisionShape(sl, s.above()).isEmpty()) {
                return new Vec3(v.x, s.getY() + 1, v.z);
            }
        }
        return v;
    }

    private static void weg(Ruiter r) {
        if (r.pluim != null) {
            r.pluim.discard();
        }
        if (r.guh != null) {
            r.guh.discard();
        }
    }

    static void opruimen() {
        RUITERS_NU.forEach(Z3Leven::weg);
        RUITERS_NU.clear();
    }

    /** For the gametest / dev command: how many riders ride now. */
    public static int aantal() {
        return RUITERS_NU.size();
    }
}
