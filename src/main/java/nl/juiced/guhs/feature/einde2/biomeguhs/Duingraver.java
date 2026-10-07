package nl.juiced.guhs.feature.einde2.biomeguhs;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.VariantGedrag;

/**
 * einde2, slice biomeguhs: the Duinguh.
 * <ul>
 *   <li>A tamed one that stands still on kruimelzand for {@link #STIL_TICKS} ticks digs in ({@link E2Vlaggen#TRUC}): the
 *       client sinks the model until only the ears show. It stays put while its owner is near and pops up again when the
 *       owner walks more than {@link #WEG_AFSTAND} blocks away (then it follows), when it is picked up or ridden, or when
 *       the sand is gone. Told to sit, it keeps watch like that for as long as you like.</li>
 *   <li>No Duinguh, wild or tame, sinks in kruimelzand: whatever slows an entity inside that block is taken off it again
 *       every tick ({@link #tick(GuhEntity)}, both sides). The kruimelzand may also ask {@link Biomeguhs#zaktNiet}.</li>
 * </ul>
 */
public final class Duingraver implements VariantGedrag {
    /** This long without moving on kruimelzand: it digs in. */
    public static final int STIL_TICKS = 60;
    /** Its owner further away than this: it comes out (FollowOwnerGoal starts at 8). */
    public static final double WEG_AFSTAND = 7.0;

    /** Server: how many ticks each Duinguh has stood still. */
    private static final Map<GuhEntity, int[]> STIL = Collections.synchronizedMap(new WeakHashMap<>());

    public static boolean ingegraven(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.DUINGUH && Trucs.doet(guh);
    }

    /** Does it stand on (or in) kruimelzand? */
    public static boolean opZand(GuhEntity guh) {
        return zand(guh) != null;
    }

    private static BlockState zand(GuhEntity guh) {
        BlockPos p = guh.blockPosition();
        BlockState hier = guh.level().getBlockState(p);
        if (Biomeguhs.isKruimelzand(hier)) {
            return hier;
        }
        BlockState onder = guh.level().getBlockState(BlockPos.containing(guh.getX(), guh.getY() - 0.2, guh.getZ()));
        return Biomeguhs.isKruimelzand(onder) ? onder : null;
    }

    /** Digs in here (it must stand on kruimelzand). */
    public static boolean graafIn(GuhEntity guh) {
        BlockState zand = zand(guh);
        if (zand == null || Trucs.doet(guh)) {
            return false;
        }
        Trucs.zet(guh, true);
        guh.getNavigation().stop();
        Trucs.geluid(guh, E2BiomeguhsFeature.DUINGUH_GRAAF.get(), 0.9f, 1f);
        stof(guh, zand, 24);
        ServerPlayer baas = Trucs.baasBij(guh, 16);
        if (baas != null) {
            GidsFeature.grant(baas, "guheinde/einde2_biomeguhs_oortjes");
        }
        return true;
    }

    public static void graafUit(GuhEntity guh) {
        STIL.remove(guh);
        if (!Trucs.doet(guh)) {
            return;
        }
        Trucs.zet(guh, false);
        Trucs.geluid(guh, E2BiomeguhsFeature.DUINGUH_PLOP.get(), 0.9f, guh.getVoicePitch());
        BlockState zand = zand(guh);
        if (zand != null) {
            stof(guh, zand, 12);
        }
    }

    private static void stof(GuhEntity guh, BlockState zand, int n) {
        if (guh.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, zand), guh.getX(), guh.getY() + 0.1, guh.getZ(), n, 0.3, 0.1, 0.3, 0.05);
        }
    }

    /** (GuhHooks.tick, server) */
    static void serverTick(GuhEntity guh) {
        boolean in = Trucs.doet(guh);
        if (!Trucs.tam(guh) || guh.isVehicle() || guh.isPassenger() || !opZand(guh)) {
            if (in) {
                graafUit(guh);
            }
            STIL.remove(guh);
            return;
        }
        ServerPlayer baas = Trucs.baas(guh);
        boolean baasWeg = baas != null && !guh.isOrderedToSit() && baas.distanceToSqr(guh) > WEG_AFSTAND * WEG_AFSTAND;
        if (in) {
            if (baasWeg || guh.isLeashed()) {
                graafUit(guh);
            } else {
                guh.getNavigation().stop();   // dug in is dug in: no strolling off
            }
            return;
        }
        int[] stil = STIL.computeIfAbsent(guh, g -> new int[1]);
        boolean beweegt = guh.getDeltaMovement().horizontalDistanceSqr() > 4.0e-4 || !guh.getNavigation().isDone();
        stil[0] = beweegt || baasWeg || !guh.onGround() ? 0 : stil[0] + 1;
        if (stil[0] >= STIL_TICKS) {
            stil[0] = 0;
            graafIn(guh);
        }
    }

    /** The Duinguh's own footing on kruimelzand: the sand's slow wading (its speed factor) does not count for it. */
    public static final Identifier ZAND_VLOT = Guhs.id("einde2_biomeguhs_duinguh_zand");

    /**
     * Both sides: kruimelzand never holds a Duinguh. The slow-down a block gives whoever is inside it is dropped again, and
     * while it stands in or on the sand it walks as fast as on any ground (the sand's speed factor slows every entity; a
     * full movement efficiency takes that away, as soul speed does on soul sand).
     */
    @Override
    public void tick(GuhEntity guh) {
        BlockState zand = zand(guh);
        if (zand != null) {
            guh.makeStuckInBlock(zand, Vec3.ZERO);
        }
        AttributeInstance vlot = guh.getAttribute(Attributes.MOVEMENT_EFFICIENCY);
        if (vlot == null) {
            return;
        }
        boolean heeft = vlot.hasModifier(ZAND_VLOT);
        if (zand != null && !heeft) {
            vlot.addTransientModifier(new AttributeModifier(ZAND_VLOT, 1.0, AttributeModifier.Operation.ADD_VALUE));
        } else if (zand == null && heeft) {
            vlot.removeModifier(ZAND_VLOT);
        }
    }
}
