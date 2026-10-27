package nl.juiced.guhs.feature.blub.klus;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeGoal;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.kamperen.KamperenFeature;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.klusjes.StappenTaak;

/**
 * Slaapliedjeszanger (a Blub's chore, id {@value #ID}): in the evening (once a day) the Blub sits down by the huisje's door
 * and squeaks the whole lullaby (about 25 seconds). The guhs that live in this huisje sleep extra well: they are {@code uitgerust} (the guh_slaapzak's
 * effect, reused: a little faster, and their chores go {@code Band.klusSnelheid} a bit quicker) until the next evening.
 */
public class ZingKlus extends BlubKlusjes.BlubKlus {
    public static final String ID = "blub_zingen";
    /** A day: uitgerust from this evening until the next one. */
    public static final int UITGERUST = 24000;
    static final String GEZONGEN_DAG = "guhs_blub_gezongen_dag";

    ZingKlus() {
        super(ID, () -> new ItemStack(BlubFeature.KLUS_ICONEN.get("liedje").get()), 200);
    }

    static long dag(ServerLevel level) {
        return Math.floorDiv(nl.juiced.guhs.world.GuhTime.dayTime(level.getServer().overworld()), 24000L);
    }

    static boolean avond(ServerLevel level, Huisje huisje) {
        return HuisjeGoal.dagdeel(level, huisje) == Dagdeel.AVOND;
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        return avond(level, huisje) ? KlusStand.ja("avond", 0) : KlusStand.straks("avond", 0);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        if (!avond(level, huisje) || bewoner.getPersistentData().getLongOr(GEZONGEN_DAG, -1L) == dag(level)) {
            return null;
        }
        return new Taak(level, huisje, bewoner);
    }

    /** The guhs of this huisje (in the world) sleep well tonight: uitgerust until tomorrow evening. Returns how many. */
    public static int uitrusten(ServerLevel level, Huisje huisje) {
        List<GuhEntity> guhs = level.getEntitiesOfClass(GuhEntity.class, huisje.gebied().inflate(8),
                g -> g.isAlive() && huisje.bewoners().contains(Band.id(g)));
        for (GuhEntity g : guhs) {
            g.addEffect(new MobEffectInstance(KamperenFeature.UITGERUST, UITGERUST, 0, false, true, true));
            level.sendParticles(ParticleTypes.NOTE, g.getX(), g.getY() + g.getBbHeight() + 0.3, g.getZ(), 0, level.getRandom().nextFloat(), 0, 0, 1);
        }
        return guhs.size();
    }

    private class Taak extends StappenTaak {
        Taak(ServerLevel level, Huisje huisje, Mob mob) {
            super(level, huisje, mob, ZingKlus.this);
        }

        @Override
        protected void begin() {
            erbij(loop(huisje.pos(), 3.0));
            erbij(werk(BlubGuhEntity.LIEDJE_TICKS, Vec3.atCenterOf(huisje.pos()), t -> {
                if (t == 1 && mob instanceof BlubGuhEntity b) {
                    BlubGeluid.KLUS_ZINGEN.speel(b);                    // (the whole lullaby, once)
                }
                if (t % 15 == 0) {
                    level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 0, level.getRandom().nextFloat(), 0, 0, 1);
                }
            }));
            erbij(doe(() -> {
                mob.getPersistentData().putLong(GEZONGEN_DAG, dag(level));
                aantal = uitrusten(level, huisje);
                gelukt();
            }));
        }
    }
}
