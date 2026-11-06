package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Effecten;

/** De Buikgangen and Het Schuilnest (lv 57-60): Mika-saboteur, Mika-lontlegger, Grauwe Pluismijt. */
public final class Buik {
    private Buik() {
    }

    /**
     * Mika-saboteur: a Mika in grey overalls with a big wrench and a sack of grey gas spanners. Fights with the wrench; every ~10 s
     * it throws a gas spanner at you (red circle 1 s): a hit, and a grey cloud stays 5 s that makes you Plakkerig.
     */
    public static class MikaSaboteur extends Z5Wezen {
        private int gooi = -1, gooiPauze = 80;
        private Vec3 gooiDoel = Vec3.ZERO;

        public MikaSaboteur(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mika_saboteur";
        }

        @Override
        public Soort soort() {
            return Soort.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double basisVads(int level) {
            return vads5(44, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade5(9, level);
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.OCELOT_HURT;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.05, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (gooi >= 0) {
                if (gooi-- == 0) {
                    sl.playSound(null, gooiDoel.x, gooiDoel.y, gooiDoel.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 1.6f);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, gooiDoel.x, gooiDoel.y + 0.4, gooiDoel.z, 20, 1.2, 0.3, 1.2, 0.02);
                    klapRond(gooiDoel, 2.5, 1.3, Schade.Soort.AFSTAND, "gassleutel");
                    Effecten.wolk(sl, gooiDoel, 2.5, 100, new DustParticleOptions(0x8C8C98, 1.4f), q -> status(q, Status.PLAKKERIG, 30, 1, this));
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (geldig(doel) && --gooiPauze <= 0 && distanceTo(doel) < 10) {
                gooiPauze = 190 + getRandom().nextInt(50);
                gooi = 20;
                gooiDoel = doel.position();
                speel("spuug");
                Telegraaf.cirkel(sl, gooiDoel, 2.5, 20, Telegraaf.ROOD);
            }
        }
    }

    /**
     * Mika-lontlegger: a Mika with a basket of cheese bombs with a fuse. Keeps 6-11 blocks away (runs off when you come close) and
     * every ~5 s lobs a bomb where you stand (red circle 1.5 s): a big bang and a hop in the air.
     */
    public static class MikaLontlegger extends Z5Wezen {
        private int bom = -1, bomPauze = 60;
        private Vec3 bomDoel = Vec3.ZERO;

        public MikaLontlegger(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mika_lontlegger";
        }

        @Override
        public Soort soort() {
            return Soort.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double basisVads(int level) {
            return vads5(34, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade5(9, level);
        }

        @Override
        public double aggroBereik() {
            return 13;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.OCELOT_HURT;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
            this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
            this.targetSelector.addGoal(2, new Aggro(this));
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (bom >= 0) {
                if (bom % 6 == 0) {
                    sl.sendParticles(ParticleTypes.SMALL_FLAME, bomDoel.x, bomDoel.y + 0.4, bomDoel.z, 2, 0.05, 0.05, 0.05, 0.01);
                }
                if (bom-- == 0) {
                    sl.playSound(null, bomDoel.x, bomDoel.y, bomDoel.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.4f);
                    sl.sendParticles(ParticleTypes.EXPLOSION, bomDoel.x, bomDoel.y + 0.4, bomDoel.z, 2, 0.6, 0.2, 0.6, 0);
                    sl.sendParticles(new DustParticleOptions(0xF2C94A, 1.6f), bomDoel.x, bomDoel.y + 0.4, bomDoel.z, 30, 1.4, 0.4, 1.4, 0);
                    for (Player p : spelersRond(bomDoel, 3)) {
                        klap(p, 1.7, Schade.Soort.AFSTAND, "kaasbom");
                        duw(p, p.getDeltaMovement().add(0, 0.5, 0));
                    }
                }
            }
            LivingEntity doel = getTarget();
            if (!geldig(doel)) {
                return;
            }
            lookAt(doel, 30, 30);
            double d = distanceTo(doel);
            if (d < 6 && tickCount % 10 == 0) {
                Vec3 weg = richting(doel.position(), position());
                getNavigation().moveTo(getX() + weg.x * 7, getY(), getZ() + weg.z * 7, 1.3);
            } else if (d > 11 && tickCount % 10 == 0) {
                getNavigation().moveTo(doel, 1.0);
            }
            if (bom < 0 && --bomPauze <= 0 && d < 14 && hasLineOfSight(doel)) {
                bomPauze = 100 + getRandom().nextInt(30);
                bom = 30;
                bomDoel = doel.position();
                speel("spuug");
                Telegraaf.cirkel(sl, bomDoel, 3, 30, Telegraaf.ROOD);
            }
        }
    }

    /**
     * Grauwe Pluismijt: a small grey fluff mite that nibbles at the whale's insides. They come in little groups, are quick and weak;
     * their bites now and then make you Gesporen (grey itch).
     */
    public static class GrauwePluismijt extends Z5Wezen {
        public GrauwePluismijt(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "grauwe_pluismijt";
        }

        @Override
        public Soort soort() {
            return Soort.GRAUW;
        }

        @Override
        protected double basisVads(int level) {
            return vads5(18, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade5(5, level);
        }

        @Override
        public int xp() {
            return xp5(0.5);
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.SILVERFISH_HURT;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.25, true);
        }

        @Override
        public void aanval(LivingEntity doel) {
            super.aanval(doel);
            if (getRandom().nextFloat() < 0.3f) {
                status(doel, Status.GESPOREN, 50, Math.max(1, mobLevel() * 0.25), this);
            }
        }
    }
}
