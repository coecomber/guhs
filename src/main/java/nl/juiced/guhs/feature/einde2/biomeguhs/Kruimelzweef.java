package nl.juiced.guhs.feature.einde2.biomeguhs;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VariantGedrag;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2, slice biomeguhs: the Kruimelguh is lighter than a crumb.
 * <ul>
 *   <li>It never falls faster than {@link #DAAL} blocks per tick (wild or tame, ridden too: the rider floats down with
 *       it). While it floats, the synced flag {@code VerhaalVlaggen.ZWEEFT} is on (the client spreads its legs and flaps
 *       its little cloud wings).</li>
 *   <li>It stands on the kruimelwolken everybody else sinks through: registered with
 *       {@code biomes.Wolken.looptErop} in E2BiomeguhsFeature.</li>
 * </ul>
 */
public final class Kruimelzweef implements VariantGedrag {
    /** Its fastest fall (blocks per tick; a guh normally reaches about 3). */
    public static final double DAAL = 0.09;
    /** In the air this many ticks: it floats (the flag); this many: its owner gets the hidden advancement. */
    public static final int ZWEEF_NA = 4, LANG = 20;

    /** How many ticks each Kruimelguh has been in the air (per side). */
    private static final Map<GuhEntity, int[]> LUCHT = Collections.synchronizedMap(new WeakHashMap<>());

    public static boolean zweeft(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.KRUIMELGUH && GuhHooks.heeft(guh, VerhaalVlaggen.ZWEEFT);
    }

    /** Does a kruimelwolk carry this entity (the rule for biomes.Wolken)? */
    public static boolean looptOpWolken(net.minecraft.world.entity.Entity e) {
        return e instanceof GuhEntity guh && guh.getVariant() == GuhVariant.KRUIMELGUH;
    }

    /** Both sides (the rider's own client moves a ridden guh): slow the fall. */
    @Override
    public void tick(GuhEntity guh) {
        int[] lucht = LUCHT.computeIfAbsent(guh, g -> new int[1]);
        if (guh.onGround() || guh.isInWater() || guh.isPassenger() || guh.isNoGravity()) {
            lucht[0] = 0;
            return;
        }
        lucht[0]++;
        Vec3 v = guh.getDeltaMovement();
        if (v.y < -DAAL) {
            guh.setDeltaMovement(v.x, -DAAL, v.z);
        }
        guh.resetFallDistance();
    }

    /** (GuhHooks.tick, server) */
    static void serverTick(GuhEntity guh) {
        int[] lucht = LUCHT.get(guh);
        int n = lucht == null ? 0 : lucht[0];
        boolean zweeft = n >= ZWEEF_NA;
        if (zweeft != GuhHooks.heeft(guh, VerhaalVlaggen.ZWEEFT)) {
            GuhHooks.zet(guh, VerhaalVlaggen.ZWEEFT, zweeft);
            if (!zweeft) {
                Trucs.geluid(guh, E2BiomeguhsFeature.KRUIMELGUH_PLOF.get(), 0.7f, guh.getVoicePitch());
            }
        }
        if (n == LANG && Trucs.tam(guh)) {
            ServerPlayer baas = Trucs.baasBij(guh, 16);
            if (baas != null) {
                GuhAdvancements.grant(baas, "einde2_biomeguhs_zweef");
            }
        }
    }
}
