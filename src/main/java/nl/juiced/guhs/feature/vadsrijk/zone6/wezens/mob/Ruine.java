package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
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
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;

/**
 * Het Ruïnedal and De Moederoase (lv 66-70) + the beacon guards everywhere: the Ruïnewachter, the Mika-Lampendover, the
 * Mika-Ringdrager and the Gedoofd sterretje (ZONE6_PLAN §5 rows 10-13).
 */
public final class Ruine {
    private Ruine() {
    }

    public static final int STANDBEELD = 1;

    // =====================================================================================================================
    // Ruïnewachter: an old stone guh statue of the Sterrenguhs, crusted with grey star-salt. It stands perfectly still (no name
    // plate) until you come within 6 blocks: "De wachter wordt wakker!". Slow and heavy: a big red circle round itself (1.2 s).
    // Cheered up, the salt falls off and it waves.
    // =====================================================================================================================
    public static class Ruinewachter extends Z6Wezen {
        private int pauze = 60, laden = -1;

        public Ruinewachter(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "ruinewachter";
        }

        @Override
        protected double hpBasis() {
            return 52;
        }

        @Override
        protected double klapBasis() {
            return 8.0;
        }

        @Override
        protected double xpGewicht() {
            return 1.3;
        }

        @Override
        public double aggroBereik() {
            return 6;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == STANDBEELD ? "standbeeld" : null;
        }

        public boolean slaapt() {
            return houding() == STANDBEELD;
        }

        @Override
        public void krijgSchade(double n, @Nullable LivingEntity bron) {
            if (slaapt()) {
                wakker();
            }
            super.krijgSchade(n, bron);
        }

        private void wakker() {
            zetHouding(0);
            if (level() instanceof ServerLevel sl) {
                Getallen.tekst(sl, getX(), getY() + getBbHeight() + 0.4, getZ(), Component.translatable("vadsrijk.z6wezens.ruinewachter.wakker"), 0.9f);
                Z6Vfx.zoutKorst(sl, position().add(0, 1, 0), 1);
                sl.playSound(null, blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.2f, 0.6f);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity d = getTarget();
            if (d == null && !slaapt() && tickCount % 100 == 0 && getNavigation().isDone()) {
                zetHouding(STANDBEELD);   // back to standing guard
            }
            if (slaapt()) {
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                if (geldig(d) && distanceTo(d) < 6) {
                    wakker();
                }
                return;
            }
            if (laden > 0 && --laden == 0) {
                speel("speciaal");
                Z6Vfx.golf(sl, position(), 3.6, Z6Vfx.ZOUT);
                for (Player p : spelersRond(position(), 3.6)) {
                    klap(p, 1.5, Schade.Soort.MELEE, "wachterstamp");
                    Vec3 r = p.position().subtract(position()).multiply(1, 0, 1);
                    if (r.lengthSqr() > 0.01) {
                        p.push(r.normalize().x * 0.7, 0.3, r.normalize().z * 0.7);
                        p.hurtMarked = true;
                    }
                }
                sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.7f, 0.6f);
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 4) {
                pauze = 130 + getRandom().nextInt(30);
                laden = 24;
                Z6Vfx.cirkel(sl, position(), 3.6, 24, Z6Vfx.ROOD);
                mopper(sl);
            }
        }
    }

    // =====================================================================================================================
    // Mika-Lampendover: a Mika with a giant candle snuffer, the beacon thieves of Ringeling. Every ~6 s it swings the snuffer: a
    // red cone (0.9 s): a hit, 3 s of darkness, and it puts out your light buffs (Vahoeg, Snelpootjes).
    // =====================================================================================================================
    public static class MikaLampendover extends Z6Wezen {
        private int pauze = 60, laden = -1;

        public MikaLampendover(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mika_lampendover";
        }

        @Override
        public Afloop afloop() {
            return Afloop.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double hpBasis() {
            return 32;
        }

        @Override
        protected double klapBasis() {
            return 6.2;
        }

        @Override
        public double aggroBereik() {
            return 12;
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
                for (Player p : spelersRond(position(), 4.6)) {
                    Vec3 r = p.position().subtract(position()).multiply(1, 0, 1);
                    if (r.lengthSqr() < 0.5 || r.normalize().dot(v) > 0.5) {
                        klap(p, 1.1, Schade.Soort.MELEE, "dover");
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                        boolean gedoofd = Statussen.heeft(p, Status.VAHOEG) || Statussen.heeft(p, Status.SNELPOOTJES);
                        Statussen.wis(p, Status.VAHOEG);
                        Statussen.wis(p, Status.SNELPOOTJES);
                        if (gedoofd && p instanceof ServerPlayer sp) {
                            sp.sendOverlayMessage(Component.translatable("vadsrijk.z6wezens.lampendover.gedoofd").withStyle(ChatFormatting.GRAY));
                        }
                    }
                }
                sl.playSound(null, blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.HOSTILE, 1.5f, 0.7f);
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 4.5) {
                pauze = 120 + getRandom().nextInt(30);
                laden = 18;
                lookAt(d, 360, 30);
                setYBodyRot(getYRot());
                Z6Vfx.kegel(sl, position(), voor(), 4.6, 70, 18, Z6Vfx.ROOD);
            }
        }
    }

    // =====================================================================================================================
    // Mika-Ringdrager: an elite Mika with light-catching jars. Every ~8 s it opens a jar at someone: a purple circle of 9 round
    // it, then a beam that drains you for up to 3 s (and heals the Mika a bit). Walk out of the circle to break the beam.
    // =====================================================================================================================
    public static class MikaRingdrager extends Z6Wezen {
        private int pauze = 80, laden = -1, straalT;
        @Nullable
        private Player bundel;
        /** Gametests: drain ticks done. */
        public int getapt;

        public MikaRingdrager(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mika_ringdrager";
        }

        @Override
        public Afloop afloop() {
            return Afloop.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double hpBasis() {
            return 44;
        }

        @Override
        protected double klapBasis() {
            return 7.0;
        }

        @Override
        protected double xpGewicht() {
            return 1.4;
        }

        @Override
        public double aggroBereik() {
            return 12;
        }

        /** Opens a jar at this player now (dev / tests). */
        public void pot(ServerLevel sl, Player p) {
            bundel = p;
            straalT = 60;
            speel("speciaal");
            sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1f, 1.6f);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (straalT > 0) {
                straalT--;
                if (bundel == null || !geldig(bundel) || bundel.distanceTo(this) > 9) {
                    straalT = 0;
                    if (bundel instanceof ServerPlayer sp) {
                        sp.sendOverlayMessage(Component.translatable("vadsrijk.z6wezens.ringdrager.los").withStyle(ChatFormatting.GREEN));
                    }
                    bundel = null;
                    return;
                }
                if (straalT % 5 == 0) {
                    Z6Vfx.straal(sl, position().add(0, 1.2, 0), bundel.position().add(0, 1, 0), 0.4, Z6Vfx.PAARS, 6);
                }
                if (straalT % 10 == 0) {
                    getapt++;
                    Schade.Uitkomst u = klap(bundel, 0.3, Schade.Soort.MAGIE, "lichtvang");
                    if (u != null && u.schade() > 0) {
                        heal(u.schade() * 0.6);
                    }
                }
                return;
            }
            LivingEntity d = getTarget();
            if (laden > 0 && --laden == 0 && d instanceof Player p && geldig(p) && p.distanceTo(this) <= 9) {
                pot(sl, p);
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 8) {
                pauze = 160 + getRandom().nextInt(40);
                laden = 20;
                Z6Vfx.cirkel(sl, new Vec3(getX(), grond(position()), getZ()), 9, 20, Z6Vfx.PAARS);
            }
        }
    }

    // =====================================================================================================================
    // Gedoofd sterretje: a grumpy grey little star that fell into the oasis grass. It floats low; every ~6 s it sprinkles star
    // noise: 3 small red circles round you (1 s). Cheered up it twinkles again (rare pet: Sterretje).
    // =====================================================================================================================
    public static class GedoofdSterretje extends Z6Wezen {
        private int pauze = 60, laden = -1;
        private final Vec3[] plekken = new Vec3[3];

        public GedoofdSterretje(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "gedoofd_sterretje";
        }

        @Override
        public boolean vliegt() {
            return true;
        }

        @Override
        public Element standaardElement() {
            return Element.ROZE;
        }

        @Override
        protected double hpBasis() {
            return 30;
        }

        @Override
        protected double klapBasis() {
            return 6.6;
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
        protected double vliegHoogte() {
            return 1.5;
        }

        @Override
        protected double vliegSnelheid() {
            return 0.13;
        }

        @Override
        public String huisdier() {
            return "sterretje";
        }

        @Override
        public double huisdierKans() {
            return 0.02;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (doel == null) {
                return null;
            }
            return distanceTo(doel) < 3 ? position() : doel.position().add(0, 1.6, 0);
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
                for (Vec3 v : plekken) {
                    Z6Vfx.glinster(sl, v.add(0, 0.3, 0), 0.8);
                    for (Player p : spelersRond(v, 1.6)) {
                        klap(p, 0.9, Schade.Soort.MAGIE, "sterrengruis");
                    }
                }
                sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1f, 1.6f);
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 9) {
                pauze = 110 + getRandom().nextInt(30);
                laden = 20;
                for (int i = 0; i < 3; i++) {
                    double a = getRandom().nextDouble() * Math.PI * 2;
                    double r = i == 0 ? 0 : 2.2;
                    Vec3 v = d.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    plekken[i] = new Vec3(v.x, grond(v), v.z);
                    Z6Vfx.cirkel(sl, plekken[i], 1.6, 20, Z6Vfx.ROOD);
                }
            }
        }
    }
}
