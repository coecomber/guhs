package nl.juiced.guhs.feature.einde2.reizen;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;

/**
 * einde2 fix-reizen (playtest 1, items 4 and 12): where Appa lands at each place, and where his rider is set down.
 * <p>
 * Every place has an {@code aankomst} zone in its sidecar: the landing field (free up to the sky). At the four main buildings
 * Appa lands exactly where the arrival cutscene of that chapter shows him, and his rider stands on {@code aankomst_speler},
 * where the scene puts the player: the offsets below are taken from those scenes (template coordinates relative to the
 * {@code aankomst_speler} block, the same anchor the scenes use) and were checked against the structure templates
 * ({@code stuk_*.nbt}): his box (2.9 x 2.5) and the seat on his head are free, every block under him is solid, and the
 * column 3 blocks around him is free up to the top of the build, so he can come straight down onto it.
 * <ul>
 *   <li>Zuidelijke Luchttempel: the scene {@code einde2_h1_lucht_aankomst} has no Appa; he lands at (-5, 0, +3.5) on the
 *       west half of the landing terrace (template 91.0, 41, 175.5), looking north at the gate;</li>
 *   <li>Noordelijke Waterstam: {@code einde2_h2_water_aankomst} lands him on the landing ice at template (127.5, 31, 162.5),
 *       looking north;</li>
 *   <li>Ba Sing Guh: {@code einde2_h3_aarde_aankomst} has him at (-5.5, 0, +0.5) on the west lobe (template 5.5, 46, 128.5),
 *       looking east at the gate;</li>
 *   <li>Vuurnatie-hoofdstad: {@code einde2_h4_vuur_aankomst} has him at (-5.0, 0, +4.5) on the landing field by the harbour
 *       (template 104.0, 45, 198.5), looking north.</li>
 * </ul>
 * Any other place: the middle of its {@code aankomst} zone, his rider beside him. A spot that is not free at run time (a
 * player built on it) gives way to the nearest free spot in the zone.
 */
public final class Landingen {
    /** Appa's spot relative to the {@code aankomst_speler} block (its corner), and the way he looks. */
    record Plek(double x, double y, double z, float yaw) {
    }

    static final Map<String, Plek> HOOFDGEBOUWEN = Map.of(
            Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, new Plek(-5.0, 0, 3.5, 180f),
            Einde2Ids.NOORDELIJKE_WATERSTAM, new Plek(1.5, 0, 3.5, 180f),
            Einde2Ids.BA_SING_GUH, new Plek(-5.5, 0, 0.5, -90f),
            Einde2Ids.VUURNATIE_HOOFDSTAD, new Plek(-5.0, 0, 4.5, 180f));

    /** How far (blocks) around a place's box his rider being taken off him still counts as "arrived" (the veil's edge and more). */
    static final int AANKOMST_RAND = E2Verhaal.SLUIER_RAND + 24;

    private Landingen() {
    }

    /**
     * The landing at this place of the Guheinde (null: its spot is not known yet). {@code afstappen}: his rider gets off onto
     * their spot when he has landed (the story flights; a free flight keeps them on him).
     */
    @Nullable
    public static Appa.Landing van(ServerLevel guheinde, String structuur, boolean afstappen) {
        Vec3 aankomst = Plekken.aankomst(guheinde, structuur);
        if (aankomst == null) {
            return null;
        }
        BlockPos speler = Plekken.wereld(guheinde, structuur, "aankomst_speler");
        Plek plek = HOOFDGEBOUWEN.get(structuur);
        Vec3 appa;
        float yaw = Float.NaN;
        Vec3 spelerPlek = null;
        if (plek != null && speler != null) {
            appa = new Vec3(speler.getX() + plek.x(), speler.getY() + plek.y(), speler.getZ() + plek.z());
            yaw = plek.yaw();
            spelerPlek = new Vec3(speler.getX() + 0.5, speler.getY(), speler.getZ() + 0.5);
        } else {
            appa = aankomst;
            if (speler != null) {
                spelerPlek = new Vec3(speler.getX() + 0.5, speler.getY(), speler.getZ() + 0.5);
            }
        }
        appa = vrij(guheinde, structuur, appa);
        BoundingBox doos = Plekken.doos(guheinde, structuur);
        AABB gebied = doos == null ? null
                : new AABB(doos.minX() - AANKOMST_RAND, guheinde.getMinY(), doos.minZ() - AANKOMST_RAND,
                        doos.maxX() + 1 + AANKOMST_RAND, guheinde.getMaxY() + 64, doos.maxZ() + 1 + AANKOMST_RAND);
        return new Appa.Landing(appa, yaw, spelerPlek, gebied, afstappen);
    }

    /** The spot itself when Appa fits there (or its chunk is not loaded: it was checked against the template); else the nearest free one in the zone. */
    private static Vec3 vrij(ServerLevel level, String structuur, Vec3 at) {
        var type = E2AppaFeature.APPA_GUH.get();
        if (!level.hasChunkAt(BlockPos.containing(at)) || level.noCollision(type.getSpawnAABB(at.x, at.y, at.z))) {
            return at;
        }
        Plekken.Plek zone = Plekken.plek(structuur, "aankomst");
        BlockPos o = Plekken.oorsprong(level, structuur);
        if (zone == null || o == null) {
            return at;
        }
        BlockPos a = o.offset(zone.pos()), b = o.offset(zone.tot() == null ? zone.pos() : zone.tot());
        Vec3 best = null;
        double bestD = Double.MAX_VALUE;
        for (int x = Math.min(a.getX(), b.getX()) + 2; x <= Math.max(a.getX(), b.getX()) - 2; x++) {
            for (int z = Math.min(a.getZ(), b.getZ()) + 2; z <= Math.max(a.getZ(), b.getZ()) - 2; z++) {
                Vec3 v = new Vec3(x + 0.5, at.y, z + 0.5);
                double d = v.distanceToSqr(at);
                if (d < bestD && level.noCollision(type.getSpawnAABB(v.x, v.y, v.z))) {
                    best = v;
                    bestD = d;
                }
            }
        }
        return best != null ? best : at;
    }
}
