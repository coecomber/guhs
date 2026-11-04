package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob;

import javax.annotation.Nullable;

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

/** De Takkenbanen (lv 34-38): Kaasschutter, Schorsmarter, Grauwe Wesp. */
public final class Takken {
    private Takken() {
    }

    /**
     * Kaasschutter: a Mika with a cheese musket (Vuur). Keeps 8-12 away and looks for height (a higher spot on the branch arches);
     * aims along a red line for 1 s and fires a hot cheese ball (Brandend). Too close: it runs. Kapitein Kaaskop (contract) fires
     * three balls in a fan.
     */
    public static class Kaasschutter extends Z3Wezen {
        public static final int MIK = 1;
        private int mik = -1, mikPauze = 50, hoogtePauze = 40;
        private Vec3 mikVan, mikRichting;

        public Kaasschutter(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "kaasschutter";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(36, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(8, level);
        }

        @Override
        public int xp() {
            return 42 + 5 * mobLevel();
        }

        @Override
        public double aggroBereik() {
            return 15;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.OCELOT_HURT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == MIK ? "mik" : null;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
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
            LivingEntity doel = getTarget();
            if (!geldig(doel)) {
                if (mik >= 0) {
                    mik = -1;
                    zetHouding(0);
                }
                return;
            }
            if (mik > 0) {
                getNavigation().stop();
                lookAt(doel, 30, 30);
                if (--mik == 0) {
                    zetHouding(0);
                    speel("spuug");
                    sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.5f, 1.8f);
                    sl.sendParticles(ParticleTypes.SMOKE, getX() + mikRichting.x, getY() + 1.3, getZ() + mikRichting.z, 6, 0.1, 0.1, 0.1, 0.02);
                    boolean kapitein = "kapitein_kaaskop".equals(contract());
                    for (int i = kapitein ? -1 : 0; i <= (kapitein ? 1 : 0); i++) {
                        double a = Math.toRadians(14 * i);
                        Vec3 r = new Vec3(mikRichting.x * Math.cos(a) - mikRichting.z * Math.sin(a), mikRichting.y,
                                mikRichting.x * Math.sin(a) + mikRichting.z * Math.cos(a));
                        Effecten.schiet(sl, mikVan, mikVan.add(r.scale(22)), 0.9, new DustParticleOptions(0xF2C94A, 1.5f), p -> {
                            if (voor(p)) {
                                klap(p, 1.7, Schade.Soort.AFSTAND, "kaaskogel");
                                Hulp.status(p, Status.BRANDEND, 50, Math.max(1, mobLevel() * 0.5), this);
                            }
                        });
                    }
                    mik = -1;
                }
                return;
            }
            double d = distanceTo(doel);
            if (d < 5 && tickCount % 10 == 0) {
                Vec3 weg = Hulp.richting(doel.position(), position());
                getNavigation().moveTo(getX() + weg.x * 7, getY(), getZ() + weg.z * 7, 1.35);
            } else if (d > 13 && tickCount % 10 == 0) {
                getNavigation().moveTo(doel, 1.0);
            } else if (--hoogtePauze <= 0) {
                // look for height: the highest of a few floors nearby that still sees the target
                hoogtePauze = 80;
                Vec3 best = null;
                for (int i = 0; i < 6; i++) {
                    double a = getRandom().nextDouble() * Math.PI * 2;
                    Vec3 p = position().add(Math.cos(a) * 6, 4, Math.sin(a) * 6);
                    double g = grond(p);
                    if (g > getY() + 1.5 && (best == null || g > best.y)) {
                        best = new Vec3(p.x, g, p.z);
                    }
                }
                if (best != null) {
                    getNavigation().moveTo(best.x, best.y, best.z, 1.1);
                }
            }
            if (--mikPauze <= 0 && d < 17 && hasLineOfSight(doel)) {
                mikPauze = 70 + getRandom().nextInt(30);
                mik = 20;
                zetHouding(MIK);
                mikVan = position().add(0, 1.2, 0);
                mikRichting = doel.position().add(0, 1.0, 0).subtract(mikVan).normalize();
                Telegraaf.lijn(sl, position(), position().add(mikRichting.multiply(1, 0, 1).normalize().scale(17)), 1.3, 20, Telegraaf.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.HOSTILE, 1f, 0.8f);
            }
        }
    }

    /**
     * Schorsmarter: a quick, lithe little predator of grey bark, always in pairs. It leaps from branch to branch and drops on you
     * from above (a red circle 0.8 s where it lands).
     */
    public static class Schorsmarter extends Z3Wezen {
        public static final int SPRONG = 1;
        private int sprong = -1, sprongPauze = 50, landen;
        private Vec3 landPlek;

        public Schorsmarter(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "schorsmarter";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.BEEST;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(34, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(7, level);
        }

        @Override
        public int xp() {
            return 38 + 5 * mobLevel();
        }

        @Override
        public double aggroBereik() {
            return 12;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.FOX_AGGRO;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == SPRONG ? "sprong" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.25, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (landen > 0) {
                landen--;
                if ((onGround() && landen < 14) || landen == 0) {
                    landen = 0;
                    zetHouding(0);
                    speel("aanval");
                    sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 10, 0.6, 0.1, 0.6, 0.03);
                    klapRond(landPlek, 2.2, 1.5, Schade.Soort.MELEE, "valaanval");
                }
                return;
            }
            if (sprong >= 0) {
                getNavigation().stop();
                if (sprong-- == 0) {
                    Vec3 d = landPlek.subtract(position());
                    double t = 16;
                    setDeltaMovement(d.x / t, Math.max(0.75, d.y / t + 0.6), d.z / t);
                    landen = 22;
                    sl.playSound(null, blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.HOSTILE, 1.1f, 1.4f);
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (geldig(doel) && --sprongPauze <= 0) {
                double d = distanceTo(doel);
                if (d > 3 && d < 10) {
                    sprongPauze = 90 + getRandom().nextInt(40);
                    sprong = 16;
                    zetHouding(SPRONG);
                    landPlek = new Vec3(doel.getX(), grond(doel.position()), doel.getZ());
                    Telegraaf.cirkel(sl, landPlek, 2.2, 16, Telegraaf.ROOD);
                } else {
                    sprongPauze = 20;
                }
            }
        }
    }

    /**
     * Grauwe Wesp: a swarm of 5 small grey wasps that share one health bar (one entity). It buzzes around you and darts in to
     * sting (Gesporen). Area damage clears it fastest. Moeder Wesp (contract) splits into three small swarms at half health.
     */
    public static class GrauweWesp extends Z3Vliegwezen {
        private int steek = 30, gesplitst;

        public GrauweWesp(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "grauwe_wesp";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.ROT;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(30, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(5.5, level);
        }

        @Override
        public int xp() {
            return 38 + 5 * mobLevel();
        }

        @Override
        public double aggroBereik() {
            return 11;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.BEE_LOOP_AGGRESSIVE;
        }

        @Override
        protected double snelheid() {
            return 0.2;
        }

        @Override
        protected double minHoogte() {
            return 0.8;
        }

        @Nullable
        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (doel == null) {
                return null;
            }
            hoek += 0.12;
            if (tickCount % 40 == 0) {
                sl.playSound(null, blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 0.7f, 0.8f);
            }
            if (--steek <= 0) {
                steek = 26 + getRandom().nextInt(12);
                if (distanceTo(doel) < 3.5 && voor(doel instanceof Player p ? p : null)) {
                    speel("aanval");
                    klap(doel, 0.75, Schade.Soort.MELEE, "steek");
                    if (getRandom().nextInt(3) == 0) {
                        Hulp.status(doel, Status.GESPOREN, 60, Math.max(1, mobLevel() * 0.3), this);
                    }
                }
                return doel.position().add(0, 1.0, 0);
            }
            return doel.position().add(Math.cos(hoek) * 2.4, 1.4 + Math.sin(hoek * 2) * 0.5, Math.sin(hoek) * 2.4);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if ("moeder_wesp".equals(contract()) && gesplitst == 0 && vads() < maxVads() * 0.5) {
                gesplitst = 1;
                Hulp.zeg(sl, this, "moeder_wesp.splits");
                sl.playSound(null, blockPosition(), SoundEvents.BEEHIVE_EXIT, SoundSource.HOSTILE, 1.5f, 0.6f);
                for (int i = 0; i < 3; i++) {
                    Z3Wezen w = roep(sl, getType(), mobLevel(), i);
                    if (w != null) {
                        w.zetKlein(true);
                        w.stelIn(mobLevel(), element(), false, java.util.EnumSet.noneOf(nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix.class));
                    }
                }
            }
        }
    }
}
