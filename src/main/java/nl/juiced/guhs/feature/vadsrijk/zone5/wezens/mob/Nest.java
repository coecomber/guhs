package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob;

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

/** Het Schuilnest (lv 58-60): the Vermomde Mika. */
public final class Nest {
    private Nest() {
    }

    /**
     * Vermomde Mika: a Mika in a costume of a Wolkwacht knight guh (a round padded suit with a helmet and a painted smile). It stands
     * guard like a real knight (no nameplate, not hittable) until you come within 4 blocks: "Ontmaskerd!", the costume pops off in
     * a smoke puff and it attacks with a hatpin. Every ~7 s it vanishes in smoke and reappears BEHIND you (red circle 0.6 s where it
     * lands): step away. Idle 20 s: it puts its costume back on. Meester-Vermommer Feta is its leader in the Schuilnest.
     */
    public static class VermomdeMika extends Z5Wezen {
        private int blink = -1, blinkPauze = 90, verveeld;
        private Vec3 blinkNaar = Vec3.ZERO;

        public VermomdeMika(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            zetHouding(VERBORGEN);
        }

        @Override
        public String naam() {
            return "vermomde_mika";
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
            return schade5(10, level);
        }

        @Override
        public int xp() {
            return xp5(1.2);
        }

        @Override
        public double aggroBereik() {
            return 4;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.OCELOT_HURT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == VERBORGEN ? "wacht" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.2, true);
        }

        /** Dev/tests: unmask now. */
        public void ontmasker(ServerLevel sl) {
            zetHouding(0);
            speel("speciaal");
            zegBoven(sl, "vermomd.ontmaskerd");
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 30, 0.5, 0.8, 0.5, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.4f, 1.2f);
            verveeld = 0;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (verborgen()) {
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y, 0);
                Player p = sl.getNearestPlayer(this, 4);
                if (p != null && !p.isCreative() && !p.isSpectator()) {
                    ontmasker(sl);
                    setTarget(p);
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (!geldig(doel)) {
                if (++verveeld > 400 && blink < 0) {
                    zetHouding(VERBORGEN);
                }
                return;
            }
            verveeld = 0;
            if (blink >= 0) {
                getNavigation().stop();
                if (blink-- == 0) {
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 20, 0.4, 0.6, 0.4, 0.02);
                    teleportTo(blinkNaar.x, blinkNaar.y, blinkNaar.z);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, blinkNaar.x, blinkNaar.y + 0.8, blinkNaar.z, 20, 0.4, 0.6, 0.4, 0.02);
                    sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.7f, 1.6f);
                    lookAt(doel, 360, 360);
                    speel("aanval");
                    for (Player p : spelersRond(blinkNaar, 1.8)) {
                        klap(p, 1.5, Schade.Soort.MELEE, "rugsteek");
                    }
                }
                return;
            }
            if (--blinkPauze <= 0 && distanceTo(doel) < 10) {
                blinkPauze = 140 + getRandom().nextInt(40);
                Vec3 achter = doel.position().subtract(doel.getLookAngle().multiply(1, 0, 1).normalize().scale(1.8));
                blinkNaar = new Vec3(achter.x, grond(achter), achter.z);
                blink = 12;
                Telegraaf.cirkel(sl, blinkNaar, 1.8, 12, Telegraaf.ROOD);
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }
}
