package nl.juiced.guhs.feature.einde2.verhaal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;

/**
 * einde2 fix round 1 (fix-kern, playtest 1): the direction helper of {@link StapHint}. From where the player is to a target
 * it makes a short Dutch phrase: compass direction + distance band + up/down, e.g. "in het oosten, een eindje verder en
 * boven je", "recht boven je", "hier vlakbij". Use it in step texts ({@code %s}) and in the action bar. Texts:
 * {@code gui.guhs.einde2_verhaal.richting.*} (tools/features/einde2_verhaal.py, English in c91).
 */
public final class Richting {
    /** The lang prefix. */
    public static final String P = "gui.guhs.einde2_verhaal.richting.";
    /** The eight compass points, clockwise from north (Minecraft: north = -z, east = +x). */
    public static final List<String> WINDSTREKEN = List.of("n", "no", "o", "zo", "z", "zw", "w", "nw");
    /** The distance bands (horizontal blocks): below 24 "dichtbij", below 80 "een eindje verder", below 300 "ver weg", else "heel ver weg". */
    public static final int DICHTBIJ = 24, EINDJE = 80, VER = 300;
    /** Closer than this (sideways and up/down) is "hier vlakbij"; this far up or down counts as above / below. */
    public static final int HIER = 6, HOOGTE = 6;

    private Richting() {
    }

    /** The phrase from {@code van} to {@code naar} (both world positions). */
    public static MutableComponent van(Vec3 van, Vec3 naar) {
        List<String> keys = keys(van, naar);
        List<Object> delen = new ArrayList<>();
        for (String k : keys) {
            delen.add(Component.translatable(P + k));
        }
        return switch (delen.size()) {
            case 1 -> (MutableComponent) delen.get(0);
            case 2 -> Component.translatable(P + "zin2", delen.toArray());
            default -> Component.translatable(P + "zin3", delen.toArray());
        };
    }

    /**
     * The parts of the phrase as lang key ends (under {@link #P}), in order: one of {@code hier}, {@code recht_boven},
     * {@code recht_onder}; or a compass point, a distance band and maybe {@code boven} / {@code onder}.
     */
    public static List<String> keys(Vec3 van, Vec3 naar) {
        double dx = naar.x - van.x, dz = naar.z - van.z, dy = naar.y - van.y;
        double vlak = Math.sqrt(dx * dx + dz * dz);
        if (vlak < HIER) {
            if (dy > HOOGTE) {
                return List.of("recht_boven");
            }
            if (dy < -HOOGTE) {
                return List.of("recht_onder");
            }
            return List.of("hier");
        }
        List<String> uit = new ArrayList<>();
        uit.add(windstreek(dx, dz));
        uit.add(vlak < DICHTBIJ ? "dichtbij" : vlak < EINDJE ? "eindje" : vlak < VER ? "ver" : "heel_ver");
        if (dy > HOOGTE) {
            uit.add("boven");
        } else if (dy < -HOOGTE) {
            uit.add("onder");
        }
        return uit;
    }

    /** The compass point of a horizontal step (dx east, dz south). */
    public static String windstreek(double dx, double dz) {
        // the angle from north, clockwise: north = -z, east = +x
        double hoek = Math.toDegrees(Math.atan2(dx, -dz));
        int i = (int) Math.floorMod(Math.round(hoek / 45.0), 8L);
        return WINDSTREKEN.get(i);
    }

    /** The horizontal distance in whole blocks (for a "(120 blokken)" in a text). */
    public static int blokken(Vec3 van, Vec3 naar) {
        double dx = naar.x - van.x, dz = naar.z - van.z;
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }
}
