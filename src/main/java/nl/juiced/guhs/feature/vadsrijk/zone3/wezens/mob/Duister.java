package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensRegistry;

/** Het Wortelduister (lv 46-49): Wortelschim, De Dove Ziener. */
public final class Duister {
    private Duister() {
    }

    /**
     * Wortelschim: a faceless shadow, only visible in light (the soft lantern rule: in the dark it is a mere shimmer and takes
     * half damage). Out of your light it slips round behind you (a puff, then a short red line 0.6 s) and claws. A fleeting pair
     * of guh ears flickers in its silhouette now and then (the small guh touch of ronde 5).
     */
    public static class Wortelschim extends Z3Wezen {
        public static final int KLAUW = 1;
        private int sluip = 80, klauw = -1;
        private Vec3 klauwRichting;

        public Wortelschim(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "wortelschim";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.GEEST;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(38, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(9, level);
        }

        @Override
        public int xp() {
            return xp3(1.0);
        }

        @Override
        public double aggroBereik() {
            return 11;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.PHANTOM_AMBIENT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == KLAUW ? "uithaal" : null;
        }

        @Override
        public boolean zonderNaam() {
            return donker() || super.zonderNaam();
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.1, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity doel = getTarget();
            if (klauw >= 0) {
                getNavigation().stop();
                if (klauw-- == 0) {
                    zetHouding(0);
                    speel("aanval");
                    sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 1.2f, 0.6f);
                    for (Player p : spelersIn(getBoundingBox().inflate(3.5, 2, 3.5))) {
                        if (voor(p) && Hulp.inStrook(position(), klauwRichting, 3.2, 2.2, p.position())) {
                            klap(p, 1.6, Schade.Soort.MELEE, "schimklauw");
                        }
                    }
                }
                return;
            }
            if (geldig(doel) && --sluip <= 0) {
                sluip = 100 + getRandom().nextInt(50);
                // slip round behind the target (only from the dark: in the light it just fights)
                if (donker() && distanceTo(doel) < 12) {
                    Vec3 kijk = doel.getLookAngle().multiply(1, 0, 1);
                    kijk = kijk.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : kijk.normalize();
                    Vec3 achter = doel.position().subtract(kijk.scale(2.6));
                    double g = grond(achter.add(0, 2, 0));
                    if (Math.abs(g - doel.getY()) < 2.5) {
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                        snapTo(achter.x, g, achter.z, getYRot(), 0);
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.5f, 0.5f);
                    }
                }
                if (distanceTo(doel) < 4) {
                    klauw = 12;
                    zetHouding(KLAUW);
                    klauwRichting = Hulp.richting(position(), doel.position());
                    lookAt(doel, 180, 30);
                    Telegraaf.lijn(sl, position(), position().add(klauwRichting.scale(3.2)), 2.2, 12, Telegraaf.ROOD);
                }
            }
        }
    }

    /**
     * De Dove Ziener: big, blind, made of roots. It reacts to SOUND: running (sprinting) within 16, fighting within 16, or walking
     * past within 5; it never hears you sneak. When it hears you it roars and calls two Wortelschimmen. Sneak up on it: the first
     * hit on an unaware Ziener does x2 (Z3WezensRegistry). De Stille Ziener (contract) even hears sneaking within 6.
     */
    public static class DoveZiener extends Z3Wezen {
        public static final int BRUL = 1, SLAG = 2;
        private int brul = -1, roepPauze, slag = -1, slagPauze = 80, stil;

        public DoveZiener(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "dove_ziener";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.ROT;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(100, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(11, level);
        }

        @Override
        public int xp() {
            return xp3(1.6);
        }

        /** Never by sight: only {@link #hoort}. */
        @Override
        public double aggroBereik() {
            return 0;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.WARDEN_AMBIENT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == BRUL ? "brul" : h == SLAG ? "hef" : null;
        }

        /** Not aware of anyone yet: a sneak attack does x2. */
        public boolean onwetend() {
            return getTarget() == null && brul < 0;
        }

        /** Does it hear this player? */
        public boolean hoort(Player p) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive() || !voor(p)) {
                return false;
            }
            double d = p.distanceTo(this);
            if (d > 16) {
                return false;
            }
            if (p.isShiftKeyDown()) {
                return d < 1.8 || "de_stille_ziener".equals(contract()) && d < 6;
            }
            boolean vecht = p.tickCount - p.getLastHurtMobTimestamp() < 40;
            return p.isSprinting() || vecht || d < 5 && p.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 || d < 2;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.0, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (roepPauze > 0) {
                roepPauze--;
            }
            if (brul >= 0) {
                getNavigation().stop();
                if (brul-- == 0) {
                    zetHouding(0);
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (doel == null && tickCount % 10 == 0) {
                for (Player p : spelersIn(getBoundingBox().inflate(16, 6, 16))) {
                    if (hoort(p)) {
                        hoorde(sl, p);
                        return;
                    }
                }
                return;
            }
            if (doel instanceof Player p && tickCount % 10 == 0) {
                // it loses you when it cannot hear you any more for 3 s
                stil = hoort(p) || distanceTo(p) < 4 ? 0 : stil + 10;
                if (stil > 60) {
                    stil = 0;
                    setTarget(null);
                    Hulp.zeg(sl, this, "ziener.kwijt");
                    return;
                }
            }
            if (slag >= 0) {
                getNavigation().stop();
                if (slag-- == 0) {
                    zetHouding(0);
                    speel("klap");
                    sl.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.6f, 0.5f);
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), getX(), getY() + 0.2, getZ(), 50,
                            2.5, 0.2, 2.5, 0.2);
                    for (Player p : spelersRond(position(), 4.5)) {
                        if (voor(p)) {
                            klap(p, 1.8, Schade.Soort.MELEE, "wortelslag");
                            Hulp.duw(p, Hulp.richting(position(), p.position()).scale(0.8).add(0, 0.5, 0));
                        }
                    }
                }
                return;
            }
            if (geldig(doel) && --slagPauze <= 0 && distanceTo(doel) < 5) {
                slagPauze = 120 + getRandom().nextInt(40);
                slag = 24;
                zetHouding(SLAG);
                Telegraaf.cirkel(sl, position(), 4.5, 24, Telegraaf.ROOD);
            }
        }

        /** It heard someone: a roar, maybe two Wortelschimmen, and the fight is on. */
        public void hoorde(ServerLevel sl, Player p) {
            brul = 30;
            zetHouding(BRUL);
            lookAt(p, 180, 30);
            setTarget(p);
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.2f, 1.3f);
            sl.sendParticles(new DustParticleOptions(0x6A6080, 1.6f), getX(), getY() + getBbHeight() * 0.8, getZ(), 30, 1, 0.6, 1, 0);
            Hulp.zeg(sl, this, "ziener.brul");
            if (roepPauze <= 0) {
                roepPauze = 600;
                int n = "de_stille_ziener".equals(contract()) ? 3 : 2;
                for (int i = 0; i < n; i++) {
                    roep(sl, Z3WezensRegistry.WORTELSCHIM.get(), Math.max(1, mobLevel() - 1), i);
                }
            }
        }

        @Override
        public void setTarget(@Nullable LivingEntity t) {
            super.setTarget(t);
        }
    }
}
