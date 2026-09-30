package nl.juiced.guhs.feature.speelgoed;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A guh plays with the knabbelbal: runs after it, nudges it with its snoet (a few times), and when the kaasknabbel pops
 * out it runs to it and eats it (smakken!). Ends after enough nudges, or with the knabbel eaten.
 */
class KnabbelbalSpel extends SpeelTaak {
    private final KnabbelbalEntity bal;
    private final int maxDuwtjes;
    private int duwtjes, kijken;
    @Nullable
    private ItemEntity knabbel;

    KnabbelbalSpel(Mob mob, ServerLevel level, KnabbelbalEntity bal) {
        super(mob, level);
        this.bal = bal;
        this.maxDuwtjes = 4 + level.random.nextInt(5);
    }

    KnabbelbalEntity bal() {
        return bal;
    }

    @Override
    public int maxTicks() {
        return 700;
    }

    @Override
    protected boolean speel() {
        if (knabbel != null) {
            return eetKnabbel();
        }
        if (bal.isRemoved()) {
            return klaar();
        }
        mob.getLookControl().setLookAt(bal, 30, 30);
        if (kijken > 0) {            // watching it roll
            kijken--;
            return true;
        }
        if (duwtjes >= maxDuwtjes) {
            return klaar();
        }
        if (loopNaar(bal.position(), 1.15, 1.1 + mob.getBbWidth() * 0.5)) {
            Vec3 r = bal.position().subtract(mob.position());
            double hoek = (level.random.nextDouble() - 0.5) * 1.0;
            Vec3 dir = new Vec3(r.x * Math.cos(hoek) - r.z * Math.sin(hoek), 0, r.x * Math.sin(hoek) + r.z * Math.cos(hoek));
            knabbel = bal.duw(dir, 0.28 + level.random.nextDouble() * 0.2, mob);
            duwtjes++;
            kijken = 12 + level.random.nextInt(14);
            if (level.random.nextInt(3) == 0) {
                level.playSound(null, mob.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.7f,
                        mob instanceof GuhEntity g ? g.getVoicePitch() * 1.1f : 1.2f);
            }
        } else if (vast()) {
            return klaar();
        }
        return true;
    }

    /** The knabbel popped out: run to it and eat it. */
    private boolean eetKnabbel() {
        if (knabbel.isRemoved()) {      // (someone else was quicker)
            return klaar();
        }
        if (loopNaar(knabbel.position(), 1.25, 0.9 + mob.getBbWidth() * 0.5)) {
            knabbel.discard();
            level.playSound(null, mob.blockPosition(), ModSounds.GUH_EAT.get(), SoundSource.NEUTRAL, 1f, 1f);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, knabbel.getItem()), mob.getX(), mob.getY() + mob.getBbHeight() * 0.7,
                    mob.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
            mob.heal(10);
            if (mob instanceof GuhEntity g) {
                if (GuhEmotes.canStart(g)) {
                    g.emotes.start(Emote.SMAKKEN, false, GuhEmotes.Source.SELF);
                }
                ServerPlayer baas = Band.eigenaarOnline(g);
                Band.moment(g, baas, Moment.GEGETEN, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.KAAS_KNABBELS.get()).toString());
                Dagboek.tel(g, DagboekStat.KNABBELS_GEGETEN, 1);
                Band.geefHartjes(g, baas, Reden.VOEREN.standaard(), Reden.VOEREN);
            }
            return klaar();
        }
        return !vast() || klaar();
    }

    private boolean klaar() {
        if (duwtjes > 0) {
            Spelen.gespeeld(mob, "knabbelbal", null);
        }
        return false;
    }
}
