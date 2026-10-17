package nl.juiced.guhs.feature.einde2.h3aarde;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * The characters of the chapter that stand somewhere for ONE player at one step ({@link H3Acteurs}): the two Dai Li-Mika's
 * who follow you after Joo Dee-guh (step {@link E2H3Aarde#KAMP}: they keep their distance and look away when you look at
 * them), Oom Miroh under the tree on the hill ({@link E2H3Aarde#LU_TEN}; the only Miroh there: the resident on that
 * marker is hidden, {@link H3Rollen#bijMaken}), and, once the whole story is over, Oom Miroh in his reopened tea house (tea any time).
 */
public final class H3Personen {
    /** How far behind you the two Dai Li-Mika's walk (blocks). */
    public static final double VOLG_AFSTAND = 7.5;

    private H3Personen() {
    }

    static void tik(ServerPlayer p, E2H3Aarde.Stad stad, int stap) {
        if (stap == E2H3Aarde.KAMP) {
            volgers(p, stad);
        } else {
            H3Acteurs.wegAlle(p, "volger");
        }
        zet(p, stad, "miroh_heuvel", stap == E2H3Aarde.LU_TEN, Kind.OOM_MIROH, "lu_ten_miroh", 90f);
        H3Acteurs.weg(p, "toph_kamp");   // (fix3-h3: Toph jumps onto Appa in the escape scene; nobody waits at the camp any more)
        boolean thee = E2Verhaal.finaleKlaar(p) && stad.bij(p, "miroh", 48);
        zetOp(p, stad, "miroh_thee", thee, Kind.OOM_MIROH, "miroh", "miroh_thee", 180f);
    }

    /** One character on the spot of a marker, for this player only, while wil holds; its role spot = the marker. */
    private static void zet(ServerPlayer p, E2H3Aarde.Stad stad, String naam, boolean wil, Kind kind, String plek, float yaw) {
        zetOp(p, stad, naam, wil, kind, plek, plek, yaw);
    }

    private static void zetOp(ServerPlayer p, E2H3Aarde.Stad stad, String naam, boolean wil, Kind kind, String plek, String rol, float yaw) {
        if (!wil) {
            H3Acteurs.weg(p, naam);
            return;
        }
        Vec3 waar = stad.voet(plek);
        if (waar != null && H3Acteurs.van(p, naam) == null) {
            H3Acteurs.npc(p, naam, kind, stad.level(), waar, yaw, E2H3Aarde.STRUCTUUR + "/" + rol, false);
        }
    }

    /** Is there room for a Dai Li-Mika here (feet and head in the open, something to stand on)? */
    private static boolean vrij(E2H3Aarde.Stad stad, Vec3 waar) {
        net.minecraft.core.BlockPos b = net.minecraft.core.BlockPos.containing(waar);
        var level = stad.level();
        return level.getBlockState(b).getCollisionShape(level, b).isEmpty() && level.getBlockState(b.above()).getCollisionShape(level, b.above()).isEmpty()
                && !level.getBlockState(b.below()).getCollisionShape(level, b.below()).isEmpty();
    }

    /** The two Dai Li-Mika's: never nearer than {@link #VOLG_AFSTAND}, a step back when you turn round. */
    private static void volgers(ServerPlayer p, E2H3Aarde.Stad stad) {
        if (!stad.doos(0).contains(p.position())) {
            H3Acteurs.wegAlle(p, "volger");
            return;
        }
        Vec3 achter = Vec3.directionFromRotation(0, p.getYRot()).scale(-1);
        Vec3 zij = new Vec3(-achter.z, 0, achter.x);
        for (int i = 0; i < 2; i++) {
            String naam = "volger_" + i;
            Vec3 doel = p.position().add(achter.scale(VOLG_AFSTAND + i * 1.5)).add(zij.scale(i == 0 ? 1.5 : -1.5));
            doel = new Vec3(doel.x, p.getY(), doel.z);
            Entity e = H3Acteurs.van(p, naam);
            if (e == null) {
                GuhNpcEntity npc = H3Acteurs.npc(p, naam, Kind.DAI_LI_MIKA, stad.level(), doel, p.getYRot(), E2H3Aarde.STRUCTUUR + "/volger", true);
                if (npc != null) {
                    npc.speel("verschijn");
                }
                continue;
            }
            Vec3 nu = e.position();
            double ver = nu.distanceTo(p.position());
            Vec3 naar = ver > VOLG_AFSTAND * 4 ? doel : ver > VOLG_AFSTAND + 0.5 ? nu.add(doel.subtract(nu).scale(0.35)) : null;
            if (naar != null && vrij(stad, naar)) {
                e.teleportTo(naar.x, doel.y, naar.z);
            }
            float yaw = Cutscene.yawVan(p.position().subtract(e.position()));
            e.setYRot(yaw);
            e.setYHeadRot(yaw);
            if (e instanceof GuhNpcEntity npc) {
                npc.setYBodyRot(yaw);
            }
        }
    }
}
