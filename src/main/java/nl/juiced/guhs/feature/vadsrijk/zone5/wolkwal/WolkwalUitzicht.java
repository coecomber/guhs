package nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken;

/**
 * T-WOLKWAL: the two viewpoints of Moeder Wolkwal get a panorama (zone 1/2/3's viewpoint idea: a short non-click camera scene with a
 * title card, once, the first time you get there; seen = {@code SpelerData.gezien "vr_w_uitkijk_<id>"}):
 * <ul>
 *   <li><b>de Ooggaarde</b> (the garden on her head, or the balcony above her eye): from the garden over her head, along her big eye
 *       and her smile, out in front of her nose, then back over the whole city;</li>
 *   <li><b>het Staartpark</b> (the pavilion or the pond): up over the tail, along the waving fluke, behind her looking west over the
 *       whole city and the cathedral.</li>
 * </ul>
 * The camera stays in the open air around the whale (all keyframes are relative to the map spots).
 */
public final class WolkwalUitzicht {
    /** A viewpoint: id, the spots that trigger it (within {@link #STRAAL}), the camera keyframes. */
    public record Uitzicht(String id, List<String> plekken) {
    }

    public static final int STRAAL = 9;
    public static final List<Uitzicht> UITZICHTEN = List.of(
            new Uitzicht("z5_ooggaarde", List.of("ooggaarde_tuin", "balkon_oog", "kampvuur_ooggaarde")),
            new Uitzicht("z5_staartpark", List.of("staartpark_prieel", "staartpark_vijver", "kampvuur_staartpark")));

    private WolkwalUitzicht() {
    }

    public static String gezienSleutel(String id) {
        return "vr_w_uitkijk_" + id;
    }

    @Nullable
    public static Uitzicht van(String id) {
        for (Uitzicht u : UITZICHTEN) {
            if (u.id().equals(id) || u.id().equals("z5_" + id)) {
                return u;
            }
        }
        return null;
    }

    /** Which viewpoint (if any) a player standing here triggers. */
    @Nullable
    public static Uitzicht bij(Vec3 p) {
        for (Uitzicht u : UITZICHTEN) {
            for (String id : u.plekken()) {
                BlockPos b = Z5WereldPlekken.plek(id);
                if (b != null && p.distanceToSqr(Vec3.atBottomCenterOf(b)) < STRAAL * STRAAL) {
                    return u;
                }
            }
        }
        return null;
    }

    static void tick(ServerLevel sl) {
        if (sl.getGameTime() % 10 != 0) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || RijkScenes.bezig(p) || !Z5WereldPlekken.inZone5(p.getX(), p.getY(), p.getZ())) {
                continue;
            }
            Uitzicht u = bij(p.position());
            if (u != null) {
                SpelerData d = Spelers.data(p);
                if (d.gezien.add(gezienSleutel(u.id()))) {
                    speel(p, u);
                }
            }
        }
    }

    /** The camera keyframes (tick, camera, look-at) of a viewpoint; empty when its spots are not on the map. */
    public static List<RijkScene.Punt> punten(Uitzicht u) {
        List<RijkScene.Punt> l = new ArrayList<>();
        if (u.id().equals("z5_ooggaarde")) {
            BlockPos tuin = Z5WereldPlekken.plek("ooggaarde_tuin"), oog = Z5WereldPlekken.plek("balkon_oog");
            if (tuin == null || oog == null) {
                return l;
            }
            Vec3 t = Vec3.atBottomCenterOf(tuin), o = Vec3.atBottomCenterOf(oog);
            Vec3 kop = new Vec3(2470, t.y - 25, -520);              // her head, nose at x ~2440
            l.add(punt(0, t.add(6, 9, 10), kop));
            l.add(punt(45, o.add(10, 14, -16), o.add(-14, -14, 0)));  // over the balcony, down along her eye
            l.add(punt(105, new Vec3(2380, t.y + 6, -600), kop));     // out in front of her nose: her face, her smile
            l.add(punt(165, new Vec3(2400, t.y + 40, -470), new Vec3(2720, t.y + 20, -520)));   // back over the whole city
            l.add(punt(180, new Vec3(2405, t.y + 42, -468), new Vec3(2720, t.y + 22, -520)));
        } else {
            BlockPos prieel = Z5WereldPlekken.plek("staartpark_prieel");
            if (prieel == null) {
                return l;
            }
            Vec3 p = Vec3.atBottomCenterOf(prieel);
            Vec3 staart = new Vec3(3000, p.y + 12, -520);              // the fluke
            l.add(punt(0, p.add(-8, 12, 12), staart));
            l.add(punt(50, new Vec3(2935, p.y + 34, -585), staart));   // up over the tail stem
            l.add(punt(110, new Vec3(3065, p.y + 45, -520), new Vec3(2700, p.y + 35, -520)));   // behind her: the whole city
            l.add(punt(170, new Vec3(3040, p.y + 70, -440), new Vec3(2700, p.y + 80, -520)));   // up to the cathedral spires
            l.add(punt(190, new Vec3(3038, p.y + 72, -438), new Vec3(2700, p.y + 82, -520)));
        }
        return l;
    }

    private static RijkScene.Punt punt(int t, Vec3 pos, Vec3 kijk) {
        return new RijkScene.Punt(t, pos, kijk, -1);
    }

    public static RijkScene scene(Uitzicht u) {
        List<RijkScene.Punt> pt = punten(u);
        int duur = pt.isEmpty() ? 20 : pt.get(pt.size() - 1).t() + 10;
        RijkScene.Builder b = RijkScene.maak(gezienSleutel(u.id()), duur);
        for (RijkScene.Punt p : pt) {
            b.camera(p.t(), p.pos(), p.kijk());
        }
        b.titel("vadsrijk.wolkwal.uitzicht." + u.id(), "vadsrijk.wolkwal.uitzicht." + u.id() + ".onder", 30, 90, 0xFFE3F0);
        return b.bouw();
    }

    public static void speel(ServerPlayer p, Uitzicht u) {
        if (punten(u).isEmpty()) {
            return;
        }
        RijkScenes.speel(p, scene(u));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
        p.sendSystemMessage(Component.translatable("vadsrijk.wereld.uitkijk.gevonden", Component.translatable("vadsrijk.wolkwal.uitzicht." + u.id())));
    }
}
