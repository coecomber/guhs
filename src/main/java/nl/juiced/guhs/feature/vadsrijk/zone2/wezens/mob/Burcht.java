package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Effecten;

/** The adds of De Spiegelburcht (lv 28-30): Spiegelwacht and Scherfgeest. */
public final class Burcht {
    private Burcht() {
    }

    /**
     * Spiegelwacht: a tall guard of grey crystal holding a mirror shield. From the front, arrows and spells bounce off the mirror
     * back at you (the hook in Z2WezensRegistry, {@link #spiegelt}) and melee does half; from the side or back it is open. It
     * swings a crystal halberd (red strip, 0.9 s). Versteend (Roze x1.5).
     */
    public static class Spiegelwacht extends Z2Wezen {
        public static final int ZWAAI = 1;
        private int zwaai = -1;

        public Spiegelwacht(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "spiegelwacht";
        }

        @Override
        public Aard aard() {
            return Aard.KRISTAL;
        }

        @Override
        public boolean versteend() {
            return true;
        }

        @Override
        public Element standaardElement() {
            return Element.GEEN;
        }

        @Override
        protected double basisVads(int level) {
            return vads(80, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(9.5, level);
        }

        @Override
        public int xp() {
            return 46 + 5 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.AMETHYST_BLOCK_HIT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == ZWAAI ? "uithaal" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(0.95, true);
        }

        /** Is the attacker in front of the mirror (and not stunned)? */
        public boolean spiegelt(@Nullable LivingEntity bron) {
            if (bron == null || geplofd() || stil() || zwaai >= 0) {
                return false;
            }
            Vec3 naar = bron.position().subtract(position()).multiply(1, 0, 1);
            return naar.lengthSqr() > 1.0E-4 && naar.normalize().dot(voor()) > 0.35;
        }

        public void spiegelEffect(ServerLevel sl) {
            speel("speciaal");
            sl.sendParticles(ParticleTypes.END_ROD, getX() + voor().x * 0.8, getY() + 1.4, getZ() + voor().z * 0.8, 8, 0.3, 0.4, 0.3, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1f, 1.8f);
        }

        @Override
        public void aanval(LivingEntity doel) {
            if (zwaai < 0 && aanvalPauze <= 0 && level() instanceof ServerLevel sl) {
                zwaai = 18;
                aanvalPauze = 45;
                lookAt(doel, 90, 90);
                setYBodyRot(getYRot());
                zetHouding(ZWAAI);
                Telegraaf.lijn(sl, position(), position().add(voor().scale(4.5)), 3.0, 18, Telegraaf.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1f, 0.6f);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (zwaai >= 0) {
                getNavigation().stop();
                setYRot(yBodyRot);
                if (zwaai-- == 0) {
                    Vec3 v = voor(), z = new Vec3(-v.z, 0, v.x);
                    for (Player p : spelersIn(getBoundingBox().inflate(5, 2, 5))) {
                        Vec3 d = p.position().subtract(position());
                        if (d.dot(v) > -0.5 && d.dot(v) < 4.8 && Math.abs(d.dot(z)) < 1.8) {
                            klap(p, 2.1, Schade.Soort.MELEE, "hellebaard");
                        }
                    }
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + v.x * 2, getY() + 1, getZ() + v.z * 2, 3, 0.8, 0.1, 0.8, 0);
                    sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 0.7f);
                    zetHouding(0);
                }
            } else if (houding() == ZWAAI) {
                zetHouding(0);
            }
        }
    }

    /**
     * Scherfgeest: a floating cluster of grey mirror shards around a cold light. It blinks (vanishes and reappears a few blocks
     * away around you) and then throws a fan of three glowing shards (Vuur: light burns). Hit it right after a blink.
     */
    public static class Scherfgeest extends Vliegwezen {
        private int blinkPauze = 50, waaierOver = -1;

        public Scherfgeest(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "scherfgeest";
        }

        @Override
        public Aard aard() {
            return Aard.KRISTAL;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double basisVads(int level) {
            return vads(36, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(7.5, level);
        }

        @Override
        public int xp() {
            return 38 + 4 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.AMETHYST_CLUSTER_HIT;
        }

        @Override
        protected double minHoogte() {
            return 1.2;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (doel == null) {
                return null;
            }
            hoek += 0.05;
            if (waaierOver > 0 && --waaierOver == 0) {
                speel("spreuk");
                sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1f, 1.6f);
                Vec3 van = position().add(0, 0.8, 0);
                Vec3 naar = doel.getEyePosition().subtract(0, 0.4, 0).subtract(van);
                for (int i = -1; i <= 1; i++) {
                    double a = Math.toRadians(i * 15);
                    Vec3 r = new Vec3(naar.x * Math.cos(a) - naar.z * Math.sin(a), naar.y, naar.x * Math.sin(a) + naar.z * Math.cos(a));
                    Effecten.schiet(sl, van, van.add(r), 0.6, new DustParticleOptions(0xFFE6A8, 1.3f), p -> klap(p, 0.9, Schade.Soort.MAGIE, "lichtscherf"));
                }
            }
            if (--blinkPauze <= 0) {
                blinkPauze = 60 + getRandom().nextInt(30);
                double a = getRandom().nextDouble() * Math.PI * 2;
                Vec3 nieuw = doel.position().add(Math.cos(a) * 5.5, 1.6, Math.sin(a) * 5.5);
                BlockPos b = BlockPos.containing(nieuw);
                if (sl.getBlockState(b).getCollisionShape(sl, b).isEmpty() && sl.getBlockState(b.above()).getCollisionShape(sl, b.above()).isEmpty()) {
                    sl.sendParticles(new DustParticleOptions(0xDCD8F0, 1.2f), getX(), getY() + 0.6, getZ(), 16, 0.3, 0.4, 0.3, 0);
                    sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 1.6f);
                    setPos(nieuw.x, nieuw.y, nieuw.z);
                    sl.sendParticles(new DustParticleOptions(0xDCD8F0, 1.2f), getX(), getY() + 0.6, getZ(), 16, 0.3, 0.4, 0.3, 0);
                    hoek = a;
                    waaierOver = 12;
                }
            }
            return doel.position().add(Math.cos(hoek) * 5.5, 1.8, Math.sin(hoek) * 5.5);
        }
    }
}
