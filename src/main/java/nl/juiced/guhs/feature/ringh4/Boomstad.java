package nl.juiced.guhs.feature.ringh4;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (ring-h4): the copy of the tree city a player stands in: where its spots ({@link Plekken}) are in the world.
 * The structure {@code guhs:guhladriel_boomstad} is one build in tiles, placed turned around its anchor
 * ({@link BoomstadStructure}); {@link #bij} finds the copy around a spot (only in loaded chunks), {@link Kopie} turns
 * template coordinates into the world. Game tests (the test server has no Guhbarbecuether) put down a copy of their own
 * with {@link #test}.
 */
public final class Boomstad {
    public static final String STRUCTUUR = "guhladriel_boomstad";

    /**
     * One copy: the world position of the build's anchor and how the build is turned. {@code eigen} (tests): spots and a
     * boat path given in world coordinates, instead of the template's.
     */
    public record Kopie(BlockPos anker, Rotation draai, @Nullable Map<String, Vec3> eigen, @Nullable List<Vec3> eigenRoute) {
        /** A point of the template (coordinates of the whole build) in the world. */
        public Vec3 wereld(Vec3 lokaal) {
            return Cutscene.wereld(anker, draai, lokaal.subtract(Plekken.ANKER.getX(), Plekken.ANKER.getY(), Plekken.ANKER.getZ()));
        }

        /** The middle of the floor of a block spot, or the exact point of a boat spot, in the world. */
        public Vec3 punt(String plek) {
            if (eigen != null) {
                Vec3 p = eigen.get(plek);
                return p != null ? p : Vec3.atBottomCenterOf(anker);
            }
            Vec3 p = Plekken.punt(plek);
            boolean blok = p.x == Math.floor(p.x) && p.z == Math.floor(p.z);
            return wereld(blok ? p.add(0.5, 0, 0.5) : p);
        }

        public BlockPos blok(String plek) {
            return BlockPos.containing(punt(plek));
        }

        /** A yaw of the template in the world. */
        public float yaw(float lokaal) {
            return Cutscene.wereldYaw(draai, lokaal);
        }

        /** The boat's path in the world: down the river, or (terug) back up. */
        public List<Vec3> route(boolean terug) {
            List<Vec3> uit = new ArrayList<>();
            if (eigenRoute != null) {
                uit.addAll(eigenRoute);
            } else {
                for (Vec3 p : Plekken.ROUTE) {
                    uit.add(wereld(p));
                }
            }
            return terug ? uit.reversed() : uit;
        }
    }

    private record Proef(ServerLevel level, Kopie kopie) {
    }

    private static final List<Proef> TEST = new CopyOnWriteArrayList<>();

    /** (Game tests) a copy that counts as standing in this level; its spots are {@code plekken} (world positions). */
    public static Kopie test(ServerLevel level, BlockPos anker, Map<String, Vec3> plekken, List<Vec3> route) {
        Kopie k = new Kopie(anker, Rotation.NONE, Map.copyOf(plekken), List.copyOf(route));
        TEST.add(new Proef(level, k));
        return k;
    }

    /** (Game tests) forget the test copy with this anchor. */
    public static void testWeg(BlockPos anker) {
        TEST.removeIf(t -> t.kopie.anker().equals(anker));
    }

    /** The copy that has a piece within 48 blocks of this spot (loaded chunks only; server thread), or null. */
    @Nullable
    public static Kopie bij(ServerLevel level, BlockPos pos) {
        Kopie proef = null;
        for (Proef t : TEST) {
            if (t.level == level && t.kopie.anker().closerThan(pos, 64) && (proef == null || t.kopie.anker().distSqr(pos) < proef.anker().distSqr(pos))) {
                proef = t.kopie;
            }
        }
        if (proef != null) {
            return proef;
        }
        if (level.dimension() != BarbecuetherFeature.BARBECUETHER && !Ring.OVERAL) {
            return null;
        }
        StructureStart start = Bezetting.start(level, STRUCTUUR, pos);
        if (start == null) {
            return null;
        }
        BlockPos anker = Kopieen.wereld(start, null, Plekken.ANKER);
        return anker == null ? null : new Kopie(anker, Kopieen.draai(start, null), null, null);
    }

    /** Is this spot inside the box of a tree city (the sluier's box: only known once a player came near since the start)? */
    public static boolean binnen(Level level, BlockPos pos) {
        Sluiers.Zone zone = Sluiers.zone(level, pos);
        return zone != null && zone.structuur().equals(STRUCTUUR);
    }

    /** The spot a step of the chapter sends the player to (the names of {@link Plekken}). */
    static String plekVan(int stap) {
        return switch (stap) {
            case 0 -> "poort";
            case 1 -> "leguhlas_poort";
            case 2 -> "zaal";
            case 3 -> "gast";
            case 4 -> "spiegel";
            case 5 -> "dal";
            default -> "steiger";
        };
    }

    /**
     * Where the compass and Sam-guh point: far away the structure, and once the player stands in the city the very spot of
     * their step (the gate, the hall, the guest flet, the mirror, the jetty).
     */
    static Doel doel(ServerPlayer p, int stap) {
        Kopie k = stap > 0 ? bij(p.level(), p.blockPosition()) : null;
        if (k == null) {
            return Ring.doel(4);
        }
        return Doel.plek(p.level().dimension(), k.blok(plekVan(stap)), Component.translatable("gui.guhs.ringh4.doel." + plekVan(stap)));
    }

    private Boomstad() {
    }
}
