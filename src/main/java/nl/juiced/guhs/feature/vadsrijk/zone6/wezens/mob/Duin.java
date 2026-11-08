package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob;

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
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;

/**
 * De Roze Duinen (lv 60-62): Zandvlooien, the Fennekguh, the Springpiep and the Duinkruiper (ZONE6_PLAN §5, rows 1-4). Every special
 * is telegraphed (red = damage); none is a one-shot.
 */
public final class Duin {
    private Duin() {
    }

    /** Held poses (RijkWezen.HOUDING). */
    public static final int VERSTOPT = 1;

    // =====================================================================================================================
    // Zandvlooi: tiny rose-sand fleas, in little swarms. They hide in the sand (only a twitching pair of antennae) and jump out
    // when you come within 5 blocks; then they leap at you every ~3 s. Beaten they burst into glittering sand.
    // =====================================================================================================================
    public static class Zandvlooi extends Z6Wezen {
        private int sprongPauze = 40;

        public Zandvlooi(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "zandvlooi";
        }

        @Override
        public Afloop afloop() {
            return Afloop.VERSLAAN;
        }

        @Override
        protected double hpBasis() {
            return 11;
        }

        @Override
        protected double klapBasis() {
            return 3.6;
        }

        @Override
        protected double xpGewicht() {
            return 0.45;
        }

        @Override
        public double aggroBereik() {
            return 5;
        }

        @Override
        protected String loopAnim() {
            return "hop";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == VERSTOPT ? "verstopt" : null;
        }

        public boolean verstopt() {
            return houding() == VERSTOPT;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity d = getTarget();
            if (d == null && !verstopt() && onGround() && tickCount % 40 == 0 && getRandom().nextInt(3) == 0) {
                zetHouding(VERSTOPT);   // no fight: dig in again
                Z6Vfx.zand(sl, position(), 0.6);
            }
            if (verstopt()) {
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                if (geldig(d) && distanceTo(d) < 5.5) {
                    zetHouding(0);   // out of the sand!
                    Z6Vfx.zand(sl, position(), 0.8);
                    sl.playSound(null, blockPosition(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 0.8f, 1.8f);
                    springNaar(d, 0.5);
                }
                return;
            }
            if (geldig(d) && --sprongPauze <= 0 && onGround() && distanceTo(d) < 6) {
                sprongPauze = 60 + getRandom().nextInt(20);
                speel("stuiter");
                springNaar(d, 0.45);
            }
        }

        private void springNaar(LivingEntity d, double hoog) {
            Vec3 r = d.position().subtract(position()).multiply(1, 0, 1);
            Vec3 v = r.lengthSqr() < 0.01 ? Vec3.ZERO : r.normalize().scale(Math.min(0.7, r.length() * 0.18));
            setDeltaMovement(v.x, hoog, v.z);
        }
    }

    // =====================================================================================================================
    // Fennekguh: a desert fox-guh with HUGE ears. It hears you coming from far (aggro 16) and calls its friend (they come in
    // pairs). Every ~6 s its ears perk up: a red strip (0.8 s), then it pounces along it.
    // =====================================================================================================================
    public static class Fennekguh extends Z6Wezen {
        private int pauze = 70, laden = -1, sprongT = -1;
        private Vec3 van, naar;

        public Fennekguh(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "fennekguh";
        }

        @Override
        protected double hpBasis() {
            return 30;
        }

        @Override
        protected double klapBasis() {
            return 6.4;
        }

        @Override
        public double aggroBereik() {
            return 16;
        }

        @Override
        public String huisdier() {
            return "fennekje";
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (sprongT >= 0) {
                sprongT++;
                double f = Math.min(1, sprongT / 8.0);
                Vec3 v = van.lerp(naar, f).add(0, Math.sin(f * Math.PI) * 1.2, 0);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                for (Player p : spelersRond(v, 1.4)) {
                    klap(p, 1.3, Schade.Soort.MELEE, "fennekbons");
                    sprongT = 8;
                }
                if (sprongT >= 8) {
                    sprongT = -1;
                }
                return;
            }
            LivingEntity d = getTarget();
            if (laden > 0 && --laden == 0 && geldig(d)) {
                van = position();
                Vec3 r = d.position().subtract(van).multiply(1, 0, 1);
                naar = r.lengthSqr() < 0.01 ? van : van.add(r.normalize().scale(Math.min(7, r.length() + 1)));
                naar = new Vec3(naar.x, grond(naar), naar.z);
                sprongT = 0;
                speel("aanval");
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 8) {
                pauze = 120 + getRandom().nextInt(40);
                laden = 16;
                Vec3 r = d.position().subtract(position()).multiply(1, 0, 1);
                Vec3 eind = position().add(r.lengthSqr() < 0.01 ? Vec3.ZERO : r.normalize().scale(Math.min(7, r.length() + 1)));
                Z6Vfx.lijn(sl, position(), eind, 1.6, 16, Z6Vfx.ROOD);
                speel("speciaal");
                sl.playSound(null, blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.HOSTILE, 0.8f, 1.6f);
            }
        }
    }

    // =====================================================================================================================
    // Springpiep (springmuisguh): a jerboa-guh on a spring tail. Every ~5 s it kicks sand at you (red circle 0.9 s: a hit and
    // 1.5 s of sand in your eyes), then bounces away and comes back.
    // =====================================================================================================================
    public static class Springpiep extends Z6Wezen {
        private int pauze = 60, laden = -1;
        private Vec3 plek;

        public Springpiep(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "springpiep";
        }

        @Override
        protected double hpBasis() {
            return 24;
        }

        @Override
        protected double klapBasis() {
            return 5.6;
        }

        @Override
        public double aggroBereik() {
            return 11;
        }

        @Override
        protected String loopAnim() {
            return "hop";
        }

        @Override
        public String huisdier() {
            return "springpiepje";
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
                Z6Vfx.zand(sl, plek, 1.4);
                for (Player p : spelersRond(plek, 2.0)) {
                    klap(p, 1.0, Schade.Soort.AFSTAND, "zandschop");
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false));
                }
                if (geldig(d) && onGround()) {   // and boing, away
                    Vec3 weg = position().subtract(d.position()).multiply(1, 0, 1);
                    weg = weg.lengthSqr() < 0.01 ? voor().scale(-1) : weg.normalize();
                    setDeltaMovement(weg.x * 0.8, 0.6, weg.z * 0.8);
                    speel("stuiter");
                }
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 9) {
                pauze = 100 + getRandom().nextInt(30);
                laden = 18;
                plek = new Vec3(d.getX(), grond(d.position()), d.getZ());
                Z6Vfx.cirkel(sl, plek, 2.0, 18, Z6Vfx.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.RABBIT_JUMP, SoundSource.HOSTILE, 1f, 1.4f);
            }
        }
    }

    // =====================================================================================================================
    // Duinkruiper: a pink scorpion with a ribbon on its tail. Every ~6 s the tail curls: a red cone in front (0.8 s), a sting and
    // sticky poison (Plakkerig 3 s).
    // =====================================================================================================================
    public static class Duinkruiper extends Z6Wezen {
        private int pauze = 70, laden = -1;

        public Duinkruiper(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "duinkruiper";
        }

        @Override
        protected double hpBasis() {
            return 34;
        }

        @Override
        protected double klapBasis() {
            return 6.0;
        }

        @Override
        public double aggroBereik() {
            return 8;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity d = getTarget();
            if (laden > 0 && --laden == 0) {
                speel("speciaal");
                Vec3 v = voor();
                for (Player p : spelersRond(position(), 4.2)) {
                    Vec3 r = p.position().subtract(position()).multiply(1, 0, 1);
                    if (r.lengthSqr() < 0.5 || r.normalize().dot(v) > 0.5) {
                        klap(p, 1.2, Schade.Soort.MELEE, "staartprik");
                        Statussen.geef(p, Status.PLAKKERIG, 60, 1, this);
                    }
                }
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 4) {
                pauze = 120 + getRandom().nextInt(30);
                laden = 16;
                lookAt(d, 360, 30);
                setYBodyRot(getYRot());
                Z6Vfx.kegel(sl, position(), voor(), 4.2, 60, 16, Z6Vfx.ROOD);
                mopper(sl);
                sl.playSound(null, blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 0.8f, 1.6f);
            }
        }
    }
}
