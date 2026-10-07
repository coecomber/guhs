package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speeltje;
import nl.juiced.guhs.feature.speelgoed.SpeelTaak;

/**
 * einde2, slice sponsguh_wezens: a guh plays with a bubble blower ({@link SponsguhBlokken.BellenblaasBlock}): it trots to
 * the blower, the blower puffs, and the guh chases the bubbles around it, hopping after the high ones. One session of the
 * toy system of the Guhhuisje and the speelgoed ({@link SpeelTaak}; the kind {@link #SOORT} is registered in
 * {@link E2SponsguhFeature}), so tamed guhs near a blower play with it when a player is near and huisje residents pick it
 * at random, exactly as they do with the knabbelbal or the glijbaantje.
 */
public class BellenSpel extends SpeelTaak {
    /** The toy kind's id (registry of feature/huisje/Speelgoed). */
    public static final String ID = "einde2_sponsguh_wezens_bellenblaas";
    public static final int DUUR = 240;

    private final BlockPos blazer;
    private boolean daar;
    private int spel;
    private Vec3 bel;

    public BellenSpel(Mob mob, ServerLevel level, BlockPos blazer) {
        super(mob, level);
        this.blazer = blazer.immutable();
        this.bel = Vec3.atBottomCenterOf(blazer);
    }

    public BlockPos blazer() {
        return blazer;
    }

    /** Did it reach the blower (and is it chasing bubbles now)? */
    public boolean speelt() {
        return daar;
    }

    @Override
    public int maxTicks() {
        return DUUR + 300;
    }

    @Override
    protected boolean speel() {
        if (!level.getBlockState(blazer).is(E2SponsguhFeature.BELLENBLAAS.get())) {
            return false;                                                    // (somebody took the blower away)
        }
        if (!daar) {
            if (vast()) {
                return false;
            }
            daar = loopNaar(Vec3.atBottomCenterOf(blazer), 1.15, 2.2);
            return true;
        }
        if (++spel > DUUR) {
            level.sendParticles(BandFeature.HARTJE.get(), mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 3, 0.3, 0.15, 0.3, 0);
            return false;
        }
        if (spel % 40 == 1) {                                                // a new puff, and a new bubble to chase
            SponsguhBlokken.BellenblaasBlock.blaas(level, blazer, 10);
            double hoek = mob.getRandom().nextDouble() * Math.PI * 2, ver = 1.5 + mob.getRandom().nextDouble() * 1.8;
            bel = Vec3.atBottomCenterOf(blazer).add(Math.cos(hoek) * ver, 0, Math.sin(hoek) * ver);
        }
        mob.getLookControl().setLookAt(bel.x, bel.y + 1.4, bel.z);
        if (mob.position().distanceToSqr(bel) > 0.8) {
            mob.getNavigation().moveTo(bel.x, bel.y, bel.z, 1.25);
        } else if (mob.onGround() && spel % 10 == 0) {
            mob.getJumpControl().jump();                                     // (snapping at the bubble above its head)
            level.sendParticles(ParticleTypes.BUBBLE_POP, mob.getX(), mob.getY() + mob.getBbHeight() + 0.5, mob.getZ(), 2, 0.2, 0.1, 0.2, 0);
        }
        return true;
    }

    /** The toy kind: the nearest bubble blower within reach (found through the blower's point of interest). */
    public static final Speeltje SOORT = new Speeltje() {
        @Override
        public String id() {
            return ID;
        }

        @Nullable
        @Override
        public KlusTaak zoek(ServerLevel level, Mob speler, BlockPos rond, int bereik) {
            if (!(speler instanceof GuhEntity)) {
                return null;
            }
            return blazers(level, rond, bereik).min(Comparator.comparingDouble(p -> p.distSqr(speler.blockPosition())))
                    .map(p -> (KlusTaak) new BellenSpel(speler, level, p)).orElse(null);
        }

        @Override
        public List<Telling> tel(ServerLevel level, BlockPos rond, int bereik, Predicate<BlockPos> binnen) {
            return List.of(new Telling("bellenblaas", new ItemStack(E2SponsguhFeature.BELLENBLAAS.get()), (int) blazers(level, rond, bereik).filter(binnen).count()));
        }
    };

    /** The bubble blowers within bereik of rond. */
    public static java.util.stream.Stream<BlockPos> blazers(ServerLevel level, BlockPos rond, int bereik) {
        return level.getPoiManager().getInRange(h -> h.is(E2SponsguhFeature.BELLENBLAAS_POI.getKey()), rond, bereik, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos);
    }
}
