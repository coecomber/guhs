package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

/** De Wolkrand (lv 58-60): Grauwe Wolkduivel, Mistslier, Mika-harpoenier. */
public final class Rand {
    private Rand() {
    }

    /**
     * Grauwe Wolkduivel: a grey cloud-imp from the Grauwe Mistbank, with little horns and a thundercloud body. Circles above you;
     * every ~4.5 s it dives along a red strip (1 s) and rams whoever stands in it. Close by it pokes with its little trident.
     */
    public static class GrauweWolkduivel extends Z5Vliegwezen {
        private int duik = -1, duikPauze = 60, prik = 20;
        private Vec3 duikVan = Vec3.ZERO, duikNaar = Vec3.ZERO;
        private final Set<UUID> geraakt = new HashSet<>();

        public GrauweWolkduivel(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "grauwe_wolkduivel";
        }

        @Override
        public Soort soort() {
            return Soort.GRAUW;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
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
            return 14;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.VEX_HURT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == 1 ? "duik" : null;
        }

        @Override
        protected double snelheid() {
            return duik >= 0 && duik < 12 ? 0.9 : 0.2;
        }

        @Nullable
        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (duik >= 0) {
                if (duik >= 12) {
                    duik--;
                    return position();
                }
                double f = 1 - duik / 12.0;
                Vec3 v = duikVan.lerp(duikNaar, f).add(0, 1, 0);
                for (Player p : spelersRond(v, 1.6)) {
                    if (geraakt.add(p.getUUID())) {
                        klap(p, 1.5, Schade.Soort.MELEE, "wolkduik");
                    }
                }
                if (duik-- == 0) {
                    zetHouding(0);
                }
                return v;
            }
            if (doel == null) {
                return null;
            }
            if (--duikPauze <= 0) {
                duikPauze = 90 + getRandom().nextInt(30);
                Vec3 r = richting(position(), doel.position());
                duikVan = new Vec3(getX(), grond(position()), getZ());
                duikNaar = doel.position().add(r.scale(3));
                duik = 32;   // 20 ticks warning, then 12 ticks of dive
                geraakt.clear();
                zetHouding(1);
                Telegraaf.lijn(sl, duikVan, duikNaar, 1.8, 20, Telegraaf.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1f, 1.4f);
                return position();
            }
            if (distanceTo(doel) < 2.6 && --prik <= 0) {
                prik = 30;
                speel("aanval");
                klap(doel, 1.0, Schade.Soort.MELEE, "drietand");
            }
            hoek += 0.04;
            return doel.position().add(Math.cos(hoek) * 4.5, 3.2, Math.sin(hoek) * 4.5);
        }
    }

    /**
     * Mistslier: a grey wisp of the Grauwe Mistbank, a ragged veil with two cold eyes. Floats low; shoots mist bolts; every ~9 s it
     * blows a mist cloud around itself (grey circle 1 s): who stays in it can hardly see (Blindness) and is Plakkerig. Moeder Mist
     * is its area leader on the tail fluke.
     */
    public static class Mistslier extends Z5Vliegwezen {
        private int mist = -1, mistPauze = 100, schot = 40;

        public Mistslier(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mistslier";
        }

        @Override
        public Soort soort() {
            return Soort.GRAUW;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads5(40, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade5(8, level);
        }

        @Override
        public int xp() {
            return xp5(1.1);
        }

        @Override
        public double aggroBereik() {
            return 13;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.SOUL_ESCAPE.value();
        }

        @Override
        protected String houdingAnim(int h) {
            return h == 1 ? "blaas" : null;
        }

        @Override
        protected double minHoogte() {
            return 1.2;
        }

        @Override
        protected double snelheid() {
            return 0.12;
        }

        @Nullable
        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (mist >= 0) {
                if (mist-- == 0) {
                    zetHouding(0);
                    speel("speciaal");
                    sl.playSound(null, blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.2f, 0.6f);
                    boolean moeder = !spawner.isEmpty() && spawner.contains("leider_mistmoeder");
                    Effecten.wolk(sl, new Vec3(getX(), grond(position()), getZ()), moeder ? 7 : 5, 80, new DustParticleOptions(0xB8BCC8, 2.0f), q -> {
                        q.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                        status(q, Status.PLAKKERIG, 30, 1, this);
                    });
                }
                return position();
            }
            if (doel == null) {
                return null;
            }
            if (--mistPauze <= 0) {
                mistPauze = 170 + getRandom().nextInt(40);
                mist = 20;
                zetHouding(1);
                Telegraaf.cirkel(sl, new Vec3(getX(), grond(position()), getZ()), 5, 20, Telegraaf.GRIJS);
                return position();
            }
            if (--schot <= 0 && distanceTo(doel) < 14 && hasLineOfSight(doel)) {
                schot = 45 + getRandom().nextInt(20);
                speel("spreuk");
                Effecten.schiet(sl, position().add(0, 1.2, 0), doel.getEyePosition(), 0.55, new DustParticleOptions(0xC8CCD8, 1.3f),
                        p -> klap(p, 1.1, Schade.Soort.MAGIE, "mistschicht"));
            }
            hoek += 0.03;
            return doel.position().add(Math.cos(hoek) * 6, 1.6, Math.sin(hoek) * 6);
        }
    }

    /**
     * Mika-harpoenier: a Mika on the fins with a harpoon gun, trying to hook the grey fleet onto the whale. Keeps 7-12 away; aims
     * along a red line (1 s) and fires a harpoon that hits and yanks you TO him (never toward the edge). Close by: a kick.
     */
    public static class MikaHarpoenier extends Z5Wezen {
        public static final int MIK = 1;
        private int mik = -1, mikPauze = 60;
        private Vec3 mikRichting = Vec3.ZERO;

        public MikaHarpoenier(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mika_harpoenier";
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
            return vads5(40, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade5(9, level);
        }

        @Override
        public double aggroBereik() {
            return 14;
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
            if (mik >= 0) {
                getNavigation().stop();
                lookAt(doel, 30, 30);
                if (mik-- == 0) {
                    zetHouding(0);
                    speel("spuug");
                    sl.playSound(null, blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.2f, 0.7f);
                    for (int i = 0; i < 14; i++) {
                        Vec3 v = position().add(mikRichting.scale(i)).add(0, 1.2, 0);
                        sl.sendParticles(ParticleTypes.CRIT, v.x, v.y, v.z, 1, 0, 0, 0, 0);
                    }
                    for (Player p : spelersRond(position(), 15)) {
                        if (inStrook(position(), mikRichting, 14, 1.4, p.position())) {
                            klap(p, 1.4, Schade.Soort.AFSTAND, "harpoen");
                            Vec3 naarMij = richting(p.position(), position());
                            double afst = Math.max(0, p.distanceTo(this) - 2);
                            duw(p, naarMij.scale(Math.min(1.6, 0.25 * afst)).add(0, 0.3, 0));
                        }
                    }
                }
                return;
            }
            double d = distanceTo(doel);
            if (d < 2.6 && tickCount % 25 == 0) {
                speel("aanval");
                klap(doel, 1.0, Schade.Soort.MELEE, "schop");
            }
            if (d < 6 && tickCount % 10 == 0) {
                Vec3 weg = richting(doel.position(), position());
                getNavigation().moveTo(getX() + weg.x * 6, getY(), getZ() + weg.z * 6, 1.2);
            } else if (d > 12 && tickCount % 10 == 0) {
                getNavigation().moveTo(doel, 1.0);
            }
            if (--mikPauze <= 0 && d < 14 && hasLineOfSight(doel)) {
                mikPauze = 110 + getRandom().nextInt(30);
                mik = 20;
                mikRichting = richting(position(), doel.position());
                zetHouding(MIK);
                Telegraaf.lijn(sl, position(), position().add(mikRichting.scale(14)), 1.4, 20, Telegraaf.ROOD);
            }
        }
    }
}
