package nl.juiced.guhs.feature.klusjes;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Vriendjes;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Muisje-oppas & verzorgen: a hurt tamed guh of the same owner in the home base gets a snack from another guh
 * (#guhs:band/snacks from the huisje's chest: it heals a lot; no snack: a warm cuddle still helps a bit), and the two
 * become a little better friends. Otherwise the resident looks after the pieppiepmuisjes, Schilly and Poepschilly nearby
 * (not someone else's): a cuddle (hearts) and, when there is one in the chest, a kaasknabbel; each maatje once per
 * {@link #RUST} ticks. Guhs only.
 */
public class OppasKlus extends BasisKlus {
    public static final String VERZORGD = "guhs_klusjes_verzorgd";
    public static final int RUST = 2400;
    /** A hurt guh gets a snack at most once per this many ticks (then the maatjes get their turn too). */
    public static final int PATIENT_RUST = 600;
    public static final float SNACK_HEAL = 100f, KNUFFEL_HEAL = 25f;

    /** Not looked after in the last {@code rust} ticks. */
    static boolean uitgerust(Mob m, int rust, long nu) {
        return !m.getPersistentData().contains(VERZORGD) || m.getPersistentData().getLongOr(VERZORGD, 0L) + rust <= nu;
    }

    OppasKlus() {
        super("oppas", () -> new ItemStack(ModItems.KAAS_KNABBELS.get()), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        UUID baas = Band.eigenaar(bewoner);
        List<GuhEntity> gewond = level.getEntitiesOfClass(GuhEntity.class, huisje.gebied(), g -> g != bewoner && g.isAlive()
                && g.getType() == ModEntities.GUH.get() && g.isTame() && baas != null && baas.equals(g.getOwnerUUID())
                && g.getHealth() < g.getMaxHealth() - 1 && huisje.inGebied(g.blockPosition()) && !Huisjes.isBinnen(g)
                && !KlusGebied.geclaimd(level, g.blockPosition()) && uitgerust(g, PATIENT_RUST, level.getGameTime()));
        GuhEntity patient = gewond.stream().min(Comparator.comparingDouble(g -> g.getHealth() / g.getMaxHealth())).orElse(null);
        if (patient != null) {
            return new Taak(level, huisje, bewoner, patient);
        }
        long nu = level.getGameTime();
        List<TamableAnimal> maatjes = level.getEntitiesOfClass(TamableAnimal.class, huisje.gebied(), m -> m instanceof PiepMaatje pm
                && m.isAlive() && !pm.isBezig() && huisje.inGebied(m.blockPosition()) && !Huisjes.isBinnen(m)
                && (nl.juiced.guhs.entity.Owners.uuid(m) == null || nl.juiced.guhs.entity.Owners.uuid(m).equals(baas))
                && uitgerust(m, RUST, nu));
        TamableAnimal maatje = maatjes.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(bewoner))).orElse(null);
        return maatje == null ? null : new Taak(level, huisje, bewoner, maatje);
    }

    private class Taak extends StappenTaak {
        private final Mob wie;

        Taak(ServerLevel level, Huisje huisje, Mob mob, Mob wie) {
            super(level, huisje, mob, OppasKlus.this);
            this.wie = wie;
        }

        @Override
        protected void begin() {
            claim(wie.blockPosition(), 200);
            if (wie instanceof GuhEntity) {
                erbij(loop(Voorraad.brengPlek(level, huisje), 2.3));
                erbij(doe(() -> {
                    ItemStack snack = Voorraad.neem(level, huisje, s -> s.is(BandFeature.SNACKS), 1);
                    if (!snack.isEmpty()) {
                        toon(snack);
                        snackje = snack;
                    }
                }));
            }
            erbij(loopNaar(() -> wie.isAlive() ? wie : null, 1.7));
            erbij(werk(30, wie.position().add(0, wie.getBbHeight() * 0.6, 0), t -> {
                if (t % 10 == 0) {
                    hartjes(wie, 1);
                    wie.getNavigation().stop();
                }
            }));
            erbij(doe(this::zorg));
        }

        private ItemStack snackje = ItemStack.EMPTY;

        private void zorg() {
            if (!wie.isAlive()) {
                return;
            }
            if (wie instanceof GuhEntity patient) {
                if (!snackje.isEmpty()) {
                    patient.heal(SNACK_HEAL);
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, snackje.getItem()), patient.getX(), patient.getEyeY(), patient.getZ(),
                            8, 0.15, 0.1, 0.15, 0.05);
                    level.playSound(null, patient.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.8f, 1.3f);
                    patient.emotes.start(Emote.SMAKKEN, false, GuhEmotes.Source.SELF);
                    snackje = ItemStack.EMPTY;
                } else {
                    patient.heal(KNUFFEL_HEAL);
                }
                hartjes(patient, 5);
                patient.getPersistentData().putLong(VERZORGD, level.getGameTime());
                Vriendjes.samen(mob, patient, 30);                 // (looking after each other: friends a little more)
            } else {
                wie.heal(wie.getMaxHealth());
                ItemStack knabbel = Voorraad.neem(level, huisje, s -> s.is(ModItems.KAAS_KNABBELS.get()), 1);
                if (!knabbel.isEmpty()) {
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, knabbel.getItem()), wie.getX(), wie.getEyeY(), wie.getZ(),
                            6, 0.1, 0.1, 0.1, 0.04);
                    level.playSound(null, wie.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.6f, 1.6f);
                }
                hartjes(wie, 4);
                wie.getPersistentData().putLong(VERZORGD, level.getGameTime());
            }
            sprankel(wie.position().add(0, wie.getBbHeight() + 0.3, 0), 3);
            aantal = 1;
            gelukt();
        }

        @Override
        public void stop() {
            if (!snackje.isEmpty()) {
                pak(snackje);                                    // (the patient never got it: back into the chest)
                snackje = ItemStack.EMPTY;
            }
            super.stop();
        }
    }
}
