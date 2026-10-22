package nl.juiced.guhs.feature.safari.bus;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.feature.safari.lobby.LobbyPlekken;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * 1.6.0 (F1b): the rides of the Guh-safaribus, click-to-continue scenes with Chauffeur Toeter (texts:
 * tools/features/safari_busstation.py).
 * <ul>
 *   <li>{@link #heen} (three variants, another joke each time): plays in the park, anchored on the corner of the lobby's
 *   bus ({@link #parkAnker}, the lowest corner of lobby.json "bus"; the park is never turned): down the bus road, a zebra
 *   family crossing ("njeg, even wachten!"), the olifantenpoort of the entrance building in view, and you step out on
 *   the busplein (where SafariToegang put the real player);</li>
 *   <li>{@link #TERUG}: plays at the busstation where you got on, anchored on the chauffeur's spot (turned with the copy):
 *   you step out beside the yellow bus, "tot de volgende keer, vads!", and the camera rises over the thatched lodge.</li>
 * </ul>
 * The bus itself is the block bus that stands at both stops; the scene does not drive a vehicle.
 */
public final class BusScenes {
    public static final int HEEN_DUUR = 340, TERUG_DUUR = 180;
    public static List<Cutscene> HEEN = List.of();
    public static Cutscene TERUG;

    private BusScenes() {
    }

    static void registreer() {
        HEEN = List.of(heen(0), heen(1), heen(2));
        TERUG = terug();
    }

    /** A random way there. */
    public static Cutscene heen(RandomSource rnd) {
        return HEEN.get(rnd.nextInt(HEEN.size()));
    }

    /** The anchor of the way there: the lowest corner of the lobby's bus (null: lobby.json has no bus). */
    @Nullable
    public static BlockPos parkAnker() {
        AABB bus = LobbyPlekken.get().bus;
        return bus == null ? null : BlockPos.containing(bus.minX, bus.minY, bus.minZ);
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    private static void zebra(Cutscene.Builder b, String naam, String variant, Vec3 start) {
        b.acteur(naam, SafariFeature.DIER, start, -90f, tag -> {
            tag.putString("Soort", "zebra");
            tag.putString("Variant", variant);
        });
    }

    // the way there, relative to the bus' corner: the bus is x 0..10, z 0..17 (the road x -3..13 runs on to the park's
    // south fence at z 31), the busplein is west of it (x < -3), the entrance building with the olifantenpoort north (z < -10)
    private static Cutscene heen(int i) {
        Cutscene.Builder b = Cutscene.maak("safari_bus_heen_" + i).duur(HEEN_DUUR).verbergEcht(20).cameraOntwijkt()
                .npc("chauffeur", GuhNpcEntity.Kind.SAFARI_CHAUFFEUR, v(-1.5, 0, 10.5), 90f)
                .speler(v(-0.8, 0, 8.5), 90f);
        zebra(b, "zebra1", "vrouw", v(-14, 0, 23.5));
        zebra(b, "zebra2", "man", v(-17, 0, 24.5));
        zebra(b, "zebra3", "baby", v(-15.5, 0, 25.2));
        zebra(b, "zebra4", "vrouw", v(-19, 0, 23.0));
        // 1. down the bus road, behind the bus (0 - 80)
        b.camera(0, v(5.5, 3, 29.5), v(5.5, 3, 8)).camera(80, v(5.5, 2.6, 22), v(5.5, 3, 4));
        b.zeg(10, "chauffeur", "weg", 40).zeg(46, "chauffeur", "savanne", 34);
        // 2. a zebra family crosses in front of the camera (80 - 170)
        b.cameraKnip(80, v(9, 1.8, 30), v(3, 1.4, 24)).camera(170, v(8, 1.9, 29.5), v(14, 1.4, 24));
        b.loop("zebra1", 84, 160, v(22, 0, 23.5)).loop("zebra2", 90, 164, v(19, 0, 24.5)).loop("zebra3", 92, 166, v(20.5, 0, 25.2))
                .loop("zebra4", 98, 168, v(17, 0, 23));
        b.zeg(104, "chauffeur", "zebra", 40);
        // 3. over the bus' roof: the entrance building with the olifantenpoort (170 - 250)
        b.cameraKnip(170, v(5.5, 9, 4), v(5.5, 10, -25)).camera(250, v(5.5, 10, -2), v(5.5, 10, -25));
        b.zeg(190, "chauffeur", "poort", 50);
        // 4. stepping out on the busplein (250 - 340)
        b.cameraKnip(250, v(-13, 2.6, 15), v(-4, 1.2, 9)).camera(340, v(-12, 2.4, 13), v(-5, 1.2, 8.5));
        b.kijk("chauffeur", 252, v(-5.5, 1, 8.5));
        b.zeg(256, "chauffeur", "grap", 60);
        b.loop(Cutscene.SPELER, 262, 300, v(-5.5, 0, 8.5)).kijk(Cutscene.SPELER, 302, v(-5.5, 1, -10));
        b.animatie("chauffeur", 304, "zwaai").zeg(306, "chauffeur", "uitstappen", 34);
        return b.registreer();
    }

    // the way back, relative to the chauffeur's spot (template 57, G+1, 38): the bus is x -7..-2, z -12..3, its door at
    // x -2, z 0; the lodge is north west (template 12..47 x 6..24 = x -45..-10, z -32..-14)
    private static Cutscene terug() {
        Cutscene.Builder b = Cutscene.maak("safari_bus_terug").duur(TERUG_DUUR).verbergEcht(16).cameraOntwijkt()
                .npc("chauffeur", GuhNpcEntity.Kind.SAFARI_CHAUFFEUR, v(0.5, 0, 0.5), 90f)
                .speler(v(-0.6, 0, 1.5), -90f);
        // 1. stepping out beside the yellow bus (0 - 100)
        b.camera(0, v(5, 2.6, 9), v(-1, 1.4, 0.5)).camera(100, v(4.5, 2.2, 7.5), v(-1, 1.4, 1.5));
        b.loop(Cutscene.SPELER, 8, 50, v(2.5, 0, 5.5)).kijk(Cutscene.SPELER, 52, v(0.5, 1, 0.5));
        b.kijk("chauffeur", 30, v(2.5, 1, 5.5)).animatie("chauffeur", 56, "zwaai").zeg(58, "chauffeur", "doei", 40);
        // 2. the camera rises over the station, toward the lodge in the evening light (100 - 180)
        b.cameraKnip(100, v(-6, 7, 22), v(-27, 10, -23)).camera(180, v(-14, 18, 24), v(-27, 12, -23));
        b.zeg(112, "", "zon", 50);
        return b.registreer();
    }
}
