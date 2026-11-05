package nl.juiced.guhs.feature.vadsrijk.zone4.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Effecten;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4Vfx;

/**
 * The creatures of De Lampionkust (ZONE4_PLAN Z4-1..3): ontvadste sea and coast creatures you cheer up, plus the Mika kite crew
 * of the Wrak. Light combat: every special is telegraphed (red = damage, purple = a push/mechanic) and gentle; few creatures, and
 * several islands have none at all. Numbers: hpBasis/klapBasis on the shared curve (lv 50 ~ x15.7 HP, x11.8 damage).
 */
public final class Kust {
    private Kust() {
    }

    /** Held poses (RijkWezen.HOUDING). */
    public static final int SCHILD = 1, LAND = 2, SCHELP = 3, GLIJ = 4, MIST = 5;

    // =====================================================================================================================
    // Grauwe Mopperkrab: beaches and rocks. Every ~6 s it closes its claws in front of its face (1.5 s): hits from the front x0.5.
    // =====================================================================================================================
    public static class Mopperkrab extends Z4Wezen {
        private int schildPauze = 80, schildOver;

        public Mopperkrab(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mopperkrab";
        }

        @Override
        protected double hpBasis() {
            return 34;
        }

        @Override
        protected double klapBasis() {
            return 6;
        }

        @Override
        public double aggroBereik() {
            return 8;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == SCHILD ? "schild" : null;
        }

        /** Claws up: a hit from in front of it does half. */
        public boolean schildVoor(@Nullable LivingEntity bron) {
            if (houding() != SCHILD || bron == null) {
                return false;
            }
            Vec3 d = bron.position().subtract(position()).multiply(1, 0, 1);
            return d.lengthSqr() > 0.01 && d.normalize().dot(voor()) > 0.2;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (schildOver > 0 && --schildOver == 0) {
                zetHouding(0);
            }
            if (getTarget() != null && --schildPauze <= 0) {
                schildPauze = 110 + getRandom().nextInt(40);
                schildOver = 30;
                zetHouding(SCHILD);
                sl.playSound(null, blockPosition(), SoundEvents.ARMADILLO_SCUTE_DROP, SoundSource.HOSTILE, 1f, 1.4f);
                mopper(sl);
            }
        }
    }

    // =====================================================================================================================
    // Mokmeeuw: a sulky gull that circles above you, then dives along a red line (0.9 s) and sits on the ground for 2 s.
    // =====================================================================================================================
    public static class Mokmeeuw extends Z4Wezen {
        private int duikPauze = 70, laden = -1, duikT = -1, zitT;
        private Vec3 duikVan, duikNaar;

        public Mokmeeuw(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mokmeeuw";
        }

        @Override
        public boolean vliegt() {
            return zitT <= 0;
        }

        @Override
        public boolean isNoGravity() {
            return !geplofd() && zitT <= 0;
        }

        @Override
        protected double hpBasis() {
            return 26;
        }

        @Override
        protected double klapBasis() {
            return 6;
        }

        @Override
        public double aggroBereik() {
            return 12;
        }

        @Override
        protected String loopAnim() {
            return "vlieg";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == LAND ? "zit" : null;
        }

        @Override
        protected double vliegHoogte() {
            return 5;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (duikT >= 0 || zitT > 0) {
                return position();
            }
            if (doel == null) {
                return null;
            }
            double a = tickCount * 0.06 + getId();
            return doel.position().add(Math.cos(a) * 5, 5, Math.sin(a) * 5);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (zitT > 0) {
                if (--zitT == 0) {
                    zetHouding(0);
                    setDeltaMovement(0, 0.5, 0);
                }
                return;
            }
            if (duikT >= 0) {   // the dive itself: a straight line, hits once
                duikT++;
                double f = Math.min(1, duikT / 10.0);
                Vec3 v = duikVan.lerp(duikNaar, f);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                for (Player p : spelersRond(v, 1.4)) {
                    klap(p, 1.3, Schade.Soort.MELEE, "meeuwenduik");
                    sl.playSound(null, blockPosition(), SoundEvents.PARROT_IMITATE_BLAZE, SoundSource.HOSTILE, 0.8f, 1.6f);
                    duikT = 10;
                }
                if (duikT >= 10) {
                    duikT = -1;
                    zitT = 40;   // sits a moment: an easy target
                    zetHouding(LAND);
                    Z4Vfx.plons(sl, position(), 0.6);
                }
                return;
            }
            LivingEntity d = getTarget();
            if (laden > 0 && --laden == 0 && geldig(d)) {
                duikVan = position();
                duikNaar = new Vec3(d.getX(), grond(d.position()), d.getZ());
                duikT = 0;
                speel("aanval");
            } else if (laden <= 0 && geldig(d) && --duikPauze <= 0 && distanceTo(d) < 14) {
                duikPauze = 120 + getRandom().nextInt(40);
                laden = 18;
                Vec3 g = new Vec3(d.getX(), grond(d.position()), d.getZ());
                Vec3 o = new Vec3(getX(), g.y, getZ());
                Z4Vfx.lijn(sl, o, g.add(g.subtract(o).normalize().scale(1.5)), 1.6, 18, Z4Vfx.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 1f, 0.7f);
            }
        }
    }

    // =====================================================================================================================
    // Grauwe Lampionkwal: a grey jellyfish that floats in the air with a dead lantern inside. Every ~8 s its tentacles spark:
    // a red circle round itself (1 s), then a sting + Plakkerig.
    // =====================================================================================================================
    public static class Lampionkwal extends Z4Wezen {
        private int prikPauze = 60, laden = -1;

        public Lampionkwal(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "lampionkwal";
        }

        @Override
        public boolean vliegt() {
            return true;
        }

        @Override
        protected double hpBasis() {
            return 30;
        }

        @Override
        protected double klapBasis() {
            return 5.5;
        }

        @Override
        public double aggroBereik() {
            return 9;
        }

        @Override
        protected String loopAnim() {
            return "zweef";
        }

        @Override
        protected double vliegHoogte() {
            return 1.4;
        }

        @Override
        protected double vliegSnelheid() {
            return 0.08;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (doel == null || laden > 0) {
                return laden > 0 ? position() : null;
            }
            Vec3 naar = doel.position().subtract(position()).multiply(1, 0, 1);
            return naar.lengthSqr() < 2.2 * 2.2 ? position() : doel.position().add(0, 1.2, 0);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (tickCount % 10 == 0) {
                sl.sendParticles(new DustParticleOptions(0xB0A8B8, 0.9f), getX(), getY() + 0.3, getZ(), 2, 0.3, 0.2, 0.3, 0);
            }
            Vec3 g = new Vec3(getX(), grondOfWater(position()), getZ());
            if (laden > 0 && --laden == 0) {
                speel("speciaal");
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 30, 1.6, 0.6, 1.6, 0.1);
                for (Player p : spelersRond(new Vec3(getX(), getY() - 0.5, getZ()), 3.2)) {
                    klap(p, 1.2, Schade.Soort.MAGIE, "kwallenprik");
                    Statussen.geef(p, Status.PLAKKERIG, 40, 1, this);
                }
            } else if (laden <= 0 && geldig(getTarget()) && --prikPauze <= 0 && distanceTo(getTarget()) < 5) {
                prikPauze = 140 + getRandom().nextInt(40);
                laden = 20;
                Z4Vfx.cirkel(sl, g, 3.0, 20, Z4Vfx.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1f, 2f);
            }
        }
    }

    // =====================================================================================================================
    // Bromvlieger: a grumpy kite with a face and ears. It hangs high in the wind; every ~7 s a wind gust along a wide purple
    // strip (0.9 s) pushes you back, and when it swoops low its tail lashes everyone close by.
    // =====================================================================================================================
    public static class Bromvlieger extends Z4Wezen {
        private int windPauze = 60, laden = -1, zwiepPauze = 40;
        private Vec3 windNaar;

        public Bromvlieger(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "bromvlieger";
        }

        @Override
        public boolean vliegt() {
            return true;
        }

        @Override
        public Element standaardElement() {
            return Element.NATUUR;
        }

        @Override
        protected double hpBasis() {
            return 28;
        }

        @Override
        protected double klapBasis() {
            return 6;
        }

        @Override
        public double aggroBereik() {
            return 13;
        }

        @Override
        protected String loopAnim() {
            return "vlieg";
        }

        @Override
        protected double vliegHoogte() {
            return 4.5;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (doel == null) {
                return null;
            }
            if (laden > 0) {
                return position();
            }
            boolean laag = (tickCount / 80) % 2 == 1;   // swings low every other 4 s
            double a = tickCount * 0.05 + getId();
            return doel.position().add(Math.cos(a) * 4, laag ? 1.4 : 4.5, Math.sin(a) * 4);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity d = getTarget();
            if (laden > 0 && --laden == 0) {
                Vec3 van = new Vec3(getX(), grond(position()), getZ());
                Z4Vfx.wind(sl, van, windNaar);
                sl.playSound(null, blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1f, 1.2f);
                speel("speciaal");
                Vec3 r = windNaar.subtract(van).multiply(1, 0, 1).normalize();
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(12, 8, 12), Z4Wezen::geldig)) {
                    if (inStrook(p.position(), van, windNaar, 3.0)) {
                        klap(p, 0.6, Schade.Soort.MAGIE, "windstoot");
                        p.push(r.x * 1.1, 0.35, r.z * 1.1);
                        p.hurtMarked = true;
                    }
                }
            } else if (laden <= 0 && geldig(d) && --windPauze <= 0 && distanceTo(d) < 14) {
                windPauze = 130 + getRandom().nextInt(30);
                laden = 18;
                Vec3 van = new Vec3(getX(), grond(position()), getZ());
                Vec3 r = d.position().subtract(van).multiply(1, 0, 1);
                windNaar = van.add(r.normalize().scale(Math.max(6, r.length() + 4)));
                Z4Vfx.lijn(sl, van, windNaar, 3.0, 18, Z4Vfx.PAARS);
            }
            if (geldig(d) && --zwiepPauze <= 0 && getY() - grond(position()) < 2.6 && distanceTo(d) < 2.8) {
                zwiepPauze = 30;
                speel("aanval");
                klapRond(position(), 2.6, 1.0, Schade.Soort.MELEE, "staartzwiep");
            }
        }

        /** Is p within a strip a->b of width w (flat)? */
        static boolean inStrook(Vec3 p, Vec3 a, Vec3 b, double w) {
            Vec3 ab = b.subtract(a).multiply(1, 0, 1), ap = p.subtract(a).multiply(1, 0, 1);
            double l2 = ab.lengthSqr();
            if (l2 < 1e-4) {
                return false;
            }
            double t = ap.dot(ab) / l2;
            if (t < 0 || t > 1) {
                return false;
            }
            return ap.subtract(ab.scale(t)).length() <= w / 2;
        }
    }

    // =====================================================================================================================
    // Zeurslak: a slow sea-snail that leaves a sticky trail (Plakkerig). Below half its Vads it hides in its shell once (2.5 s,
    // no damage: "Verstopt!").
    // =====================================================================================================================
    public static class Zeurslak extends Z4Wezen {
        private boolean verstopt;
        private int schelpOver;

        public Zeurslak(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "zeurslak";
        }

        @Override
        protected double hpBasis() {
            return 44;
        }

        @Override
        protected double klapBasis() {
            return 6.5;
        }

        @Override
        protected double xpGewicht() {
            return 1.2;
        }

        @Override
        public double aggroBereik() {
            return 7;
        }

        @Override
        protected String loopAnim() {
            return "kruip";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == SCHELP ? "schelp" : null;
        }

        public boolean inSchelp() {
            return houding() == SCHELP;
        }

        @Override
        public void krijgSchade(double n, @Nullable LivingEntity bron) {
            if (inSchelp() && !geplofd()) {
                if (bron instanceof ServerPlayer p && level() instanceof ServerLevel sl && tickCount % 10 == 0) {
                    Getallen.tekst(sl, getX(), getY() + getBbHeight() + 0.3, getZ(), Component.translatable("vadsrijk.z4wezens.verstopt"), 0.8f);
                }
                return;
            }
            super.krijgSchade(n, bron);
            if (!verstopt && !geplofd() && vads() < maxVads() * 0.5) {
                verstopt = true;
                schelpOver = 50;
                zetHouding(SCHELP);
                getNavigation().stop();
            }
        }

        @Override
        public boolean stil() {
            return super.stil() || inSchelp();
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (schelpOver > 0 && --schelpOver == 0) {
                zetHouding(0);
            }
            if (getTarget() != null && tickCount % 30 == 0 && onGround() && getDeltaMovement().horizontalDistanceSqr() > 1e-4) {
                Effecten.wolk(sl, position(), 1.1, 100, new DustParticleOptions(0xC8B8D8, 1.0f), p -> {
                    if (p instanceof ServerPlayer sp && !sp.isCreative()) {
                        Statussen.geef(sp, Status.PLAKKERIG, 30, 1, this);
                    }
                });
            }
        }
    }

    // =====================================================================================================================
    // Koraalkobold: a small coral imp, in twos and threes. It keeps ~6 blocks away and throws coral shards (a lob you see coming).
    // =====================================================================================================================
    public static class Koraalkobold extends Z4Wezen {
        private int gooiPauze = 40, laden = -1;

        public Koraalkobold(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "koraalkobold";
        }

        @Override
        protected double hpBasis() {
            return 22;
        }

        @Override
        protected double klapBasis() {
            return 5;
        }

        @Override
        protected double xpGewicht() {
            return 0.8;
        }

        @Override
        public double aggroBereik() {
            return 11;
        }

        @Override
        protected void doelen() {
            this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
            this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8));
            this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 12f));
            this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this).setAlertOthers());
            this.targetSelector.addGoal(2, new Aggro(this));
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity d = getTarget();
            if (!geldig(d)) {
                return;
            }
            lookAt(d, 30, 30);
            double af = distanceTo(d);
            if (tickCount % 10 == 0 && !stil()) {   // keep ~6 away
                if (af < 4.5) {
                    Vec3 weg = position().subtract(d.position()).multiply(1, 0, 1).normalize().scale(5);
                    getNavigation().moveTo(getX() + weg.x, getY(), getZ() + weg.z, 1.2);
                } else if (af > 9) {
                    getNavigation().moveTo(d, 1.0);
                } else {
                    getNavigation().stop();
                }
            }
            if (laden > 0 && --laden == 0) {
                speel("spuug");
                Effecten.schiet(sl, position().add(0, getBbHeight() * 0.7, 0), d.getEyePosition().subtract(0, 0.4, 0), 0.5,
                        new DustParticleOptions(0xFF8A9A, 1.3f), p -> {
                            klap(p, 1.1, Schade.Soort.AFSTAND, "koraalscherf");
                            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 0.7f, 1.6f);
                        });
            } else if (laden <= 0 && --gooiPauze <= 0 && af < 12 && hasLineOfSight(d)) {
                gooiPauze = 50 + getRandom().nextInt(20);
                laden = 10;
                sl.sendParticles(new DustParticleOptions(0xFF8A9A, 1f), getX(), getY() + getBbHeight() + 0.2, getZ(), 6, 0.2, 0.1, 0.2, 0);
            }
        }
    }

    // =====================================================================================================================
    // Pruilrob: a sulky seal on the sandbanks. It drums its flippers (red line, 1 s), then belly-slides along it and shoves you.
    // =====================================================================================================================
    public static class Pruilrob extends Z4Wezen {
        private int glijPauze = 80, laden = -1, glijT = -1;
        private Vec3 glijNaar;

        public Pruilrob(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "pruilrob";
        }

        @Override
        protected double hpBasis() {
            return 46;
        }

        @Override
        protected double klapBasis() {
            return 7;
        }

        @Override
        protected double xpGewicht() {
            return 1.3;
        }

        @Override
        public double aggroBereik() {
            return 8;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == GLIJ ? "glij" : null;
        }

        @Override
        public boolean stil() {
            return super.stil() || laden > 0 || glijT >= 0;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (glijT >= 0) {
                glijT++;
                Vec3 r = glijNaar.subtract(position()).multiply(1, 0, 1);
                if (r.lengthSqr() < 1 || glijT > 24 || horizontalCollision) {
                    glijT = -1;
                    zetHouding(0);
                    setDeltaMovement(Vec3.ZERO);
                    return;
                }
                Vec3 v = r.normalize().scale(0.55);
                setDeltaMovement(v.x, getDeltaMovement().y, v.z);
                for (Player p : spelersRond(position(), 1.3)) {
                    if (p.hurtTime == 0) {
                        klap(p, 1.4, Schade.Soort.MELEE, "buikglijder");
                        p.push(v.x * 1.6, 0.4, v.z * 1.6);
                        p.hurtMarked = true;
                    }
                }
                if (glijT % 3 == 0) {
                    sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.2, getZ(), 6, 0.4, 0.1, 0.4, 0.1);
                }
                return;
            }
            LivingEntity d = getTarget();
            if (laden > 0 && --laden == 0) {
                glijT = 0;
                zetHouding(GLIJ);
                sl.playSound(null, blockPosition(), SoundEvents.DOLPHIN_PLAY, SoundSource.HOSTILE, 1f, 0.7f);
            } else if (laden <= 0 && geldig(d) && --glijPauze <= 0 && distanceTo(d) > 3 && distanceTo(d) < 11) {
                glijPauze = 140 + getRandom().nextInt(40);
                laden = 20;
                Vec3 van = position();
                Vec3 r = d.position().subtract(van).multiply(1, 0, 1).normalize();
                glijNaar = van.add(r.scale(distanceTo(d) + 3));
                lookAt(d, 360, 30);
                speel("speciaal");
                Z4Vfx.lijn(sl, van, glijNaar, 1.6, 20, Z4Vfx.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.DOLPHIN_AMBIENT, SoundSource.HOSTILE, 1f, 0.6f);
            }
        }
    }

    // =====================================================================================================================
    // Stekelpluis: a grey sea-urchin puff. Never starts a fight. Hit it and every ~5 s it puffs its spikes: red circle 2.5 (1 s).
    // =====================================================================================================================
    public static class Stekelpluis extends Z4Wezen {
        private int puf = 40, laden = -1;

        public Stekelpluis(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "stekelpluis";
        }

        @Override
        protected double hpBasis() {
            return 30;
        }

        @Override
        protected double klapBasis() {
            return 6;
        }

        @Override
        public double aggroBereik() {
            return 0;
        }

        @Override
        protected String loopAnim() {
            return "rol";
        }

        @Override
        protected void doelen() {
            standaardDoelen(0.8, false);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (laden > 0 && --laden == 0) {
                speel("puf");
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.5, getZ(), 30, 1.2, 0.4, 1.2, 0.3);
                sl.playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.HOSTILE, 1f, 1f);
                klapRond(position(), 2.6, 1.3, Schade.Soort.MELEE, "stekelpuf");
            } else if (laden <= 0 && geldig(getTarget()) && --puf <= 0 && distanceTo(getTarget()) < 4) {
                puf = 100;
                laden = 20;
                Z4Vfx.cirkel(sl, position(), 2.5, 20, Z4Vfx.ROOD);
            }
        }
    }

    // =====================================================================================================================
    // Mistmopje: a grey mist puff on the Mysterie-eiland (a soft flash of the grey empire). Every ~9 s it melts into the fog
    // (1 s, can't be hit) and comes back behind you with a grey sigh: a red circle 2 (0.8 s) + 3 s Darkness.
    // =====================================================================================================================
    public static class Mistmopje extends Z4Wezen {
        private int mistPauze = 80, mistT = -1;

        public Mistmopje(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mistmopje";
        }

        @Override
        public boolean vliegt() {
            return true;
        }

        @Override
        public Element standaardElement() {
            return Element.NATUUR;
        }

        @Override
        protected double hpBasis() {
            return 28;
        }

        @Override
        protected double klapBasis() {
            return 6;
        }

        @Override
        public double aggroBereik() {
            return 10;
        }

        @Override
        protected String loopAnim() {
            return "zweef";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == MIST ? "mist" : null;
        }

        @Override
        protected double vliegHoogte() {
            return 1.2;
        }

        @Override
        protected double vliegSnelheid() {
            return 0.12;
        }

        public boolean inMist() {
            return houding() == MIST;
        }

        @Override
        public boolean isPickable() {
            return !inMist() && super.isPickable();
        }

        @Override
        public void krijgSchade(double n, @Nullable LivingEntity bron) {
            if (!inMist()) {
                super.krijgSchade(n, bron);
            }
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (doel == null) {
                return null;
            }
            if (mistT >= 0) {
                return position();
            }
            return distanceTo(doel) < 2.0 ? position() : doel.position().add(0, 1.0, 0);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (tickCount % 6 == 0) {
                sl.sendParticles(new DustParticleOptions(0x9A98A6, 1.8f), getX(), getY() + 0.4, getZ(), 2, 0.4, 0.3, 0.4, 0);
            }
            LivingEntity d = getTarget();
            if (mistT >= 0) {
                mistT++;
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.4, getZ(), 1, 0.3, 0.3, 0.3, 0.01);
                if (mistT == 20 && geldig(d)) {   // reappear behind the target
                    double a = Math.toRadians(d.getYRot());
                    Vec3 achter = d.position().add(Math.sin(a) * 2.2, 0.8, -Math.cos(a) * 2.2);
                    setPos(achter.x, achter.y, achter.z);
                    zetHouding(0);
                    Z4Vfx.cirkel(sl, new Vec3(getX(), grond(position()), getZ()), 2.0, 16, Z4Vfx.ROOD);
                }
                if (mistT == 36) {
                    mistT = -1;
                    speel("speciaal");
                    sl.sendParticles(new DustParticleOptions(0x8A8896, 2.2f), getX(), getY() + 0.3, getZ(), 30, 1.2, 0.4, 1.2, 0);
                    for (Player p : spelersRond(position(), 2.2)) {
                        klap(p, 1.2, Schade.Soort.MAGIE, "mistzucht");
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                    }
                }
                if (mistT > 40) {
                    mistT = -1;
                    zetHouding(0);
                }
                return;
            }
            if (geldig(d) && --mistPauze <= 0 && distanceTo(d) < 9) {
                mistPauze = 160 + getRandom().nextInt(40);
                mistT = 0;
                zetHouding(MIST);
                sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1f, 0.8f);
            }
        }
    }

    // =====================================================================================================================
    // Vliegerknecht: the Mika kite crew of the Wrak van de Vliegervanger. A hook on a rope: a red line (0.8 s), then a throw that
    // makes you Plakkerig and tugs you a little toward it. Flees in a smoke puff (not cheered up).
    // =====================================================================================================================
    public static class Vliegerknecht extends Z4Wezen {
        private int haakPauze = 60, laden = -1;
        private Vec3 haakNaar;

        public Vliegerknecht(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "vliegerknecht";
        }

        @Override
        public boolean mika() {
            return true;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double hpBasis() {
            return 40;
        }

        @Override
        protected double klapBasis() {
            return 7;
        }

        @Override
        protected double xpGewicht() {
            return 1.2;
        }

        @Override
        public double aggroBereik() {
            return 12;
        }

        @Override
        protected void doelen() {
            standaardDoelen(1.1, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity d = getTarget();
            if (laden > 0 && --laden == 0) {
                speel("spuug");
                Vec3 van = position();
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(12, 4, 12), Z4Wezen::geldig)) {
                    if (Bromvlieger.inStrook(p.position(), van, haakNaar, 1.6)) {
                        klap(p, 1.1, Schade.Soort.AFSTAND, "enterhaak");
                        Statussen.geef(p, Status.PLAKKERIG, 40, 1, this);
                        Vec3 r = van.subtract(p.position()).multiply(1, 0, 1).normalize();
                        p.push(r.x * 0.6, 0.2, r.z * 0.6);
                        p.hurtMarked = true;
                    }
                }
                sl.playSound(null, blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.HOSTILE, 1f, 0.8f);
            } else if (laden <= 0 && geldig(d) && --haakPauze <= 0 && distanceTo(d) > 3 && distanceTo(d) < 10) {
                haakPauze = 100 + getRandom().nextInt(40);
                laden = 16;
                Vec3 van = position();
                Vec3 r = d.position().subtract(van).multiply(1, 0, 1).normalize();
                haakNaar = van.add(r.scale(10));
                lookAt(d, 360, 30);
                Z4Vfx.lijn(sl, van, haakNaar, 1.6, 16, Z4Vfx.ROOD);
            }
        }
    }
}
