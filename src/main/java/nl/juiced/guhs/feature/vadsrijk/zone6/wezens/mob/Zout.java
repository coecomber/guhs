package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
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
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;

/**
 * De Zoutvlakte (lv 64-66): the Spiegelslang, the Zoutkreeft, the Zandgeestje (night) and the cave elite De Zoutslang
 * (ZONE6_PLAN §5 rows 7-9 and §7).
 */
public final class Zout {
    private Zout() {
    }

    public static final int ONDER = 1;

    // =====================================================================================================================
    // Spiegelslang: a mirage snake of glass-white salt (a construct). When it starts a fight it makes two COPIES (a mirage):
    // only one is real. A copy bursts at the first touch ("Een luchtspiegeling!") and never hurts. Lunge = red strip 0.8 s.
    // =====================================================================================================================
    public static class Spiegelslang extends Z6Wezen {
        private boolean kopie;
        private int kopieT, pauze = 50, laden = -1, spiegelPauze;
        private final List<UUID> kopieen = new ArrayList<>();
        private Vec3 naar;

        public Spiegelslang(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "spiegelslang";
        }

        @Override
        public Afloop afloop() {
            return Afloop.VERSLAAN;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double hpBasis() {
            return 30;
        }

        @Override
        protected double klapBasis() {
            return 6.2;
        }

        @Override
        public double aggroBereik() {
            return 10;
        }

        public boolean kopie() {
            return kopie;
        }

        public int kopieen() {
            return kopieen.size();
        }

        @Override
        public boolean shouldBeSaved() {
            return !kopie && super.shouldBeSaved();
        }

        @Override
        public void krijgSchade(double n, @Nullable LivingEntity bron) {
            if (kopie) {
                if (level() instanceof ServerLevel sl) {
                    Z6Vfx.glinster(sl, position().add(0, 0.4, 0), 1);
                    Getallen.tekst(sl, getX(), getY() + 1.2, getZ(), Component.translatable("vadsrijk.z6wezens.spiegelslang.kopie"), 0.8f);
                    sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1f, 1.6f);
                }
                discard();
                return;
            }
            super.krijgSchade(n, bron);
        }

        @Override
        public Schade.Uitkomst klap(LivingEntity doel, double factor, Schade.Soort soort, String skill) {
            return kopie ? null : super.klap(doel, factor, soort, skill);   // a mirage never hurts
        }

        @Override
        public void aanval(LivingEntity doel) {
            if (kopie) {
                speel("aanval");
                return;
            }
            super.aanval(doel);
        }

        @Override
        protected void opPlof() {
            super.opPlof();
            if (level() instanceof ServerLevel sl) {
                for (UUID u : kopieen) {
                    if (sl.getEntity(u) instanceof Spiegelslang k) {
                        Z6Vfx.glinster(sl, k.position(), 1);
                        k.discard();
                    }
                }
                kopieen.clear();
            }
        }

        /** Makes the two mirage copies (dev / tests too). */
        public void spiegel(ServerLevel sl) {
            for (int i = 0; i < 2; i++) {
                if (getType().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Spiegelslang k) {
                    double a = getRandom().nextDouble() * Math.PI * 2;
                    k.kopie = true;
                    k.kopieT = 20 * 15;
                    k.snapTo(getX() + Math.cos(a) * 2.5, getY(), getZ() + Math.sin(a) * 2.5, getYRot(), 0);
                    k.stelIn(mobLevel(), element(), elite(), Set.copyOf(affixen()));
                    k.zetMaxVads(1, false);
                    k.setTarget(getTarget());
                    sl.addFreshEntity(k);
                    kopieen.add(k.getUUID());
                }
            }
            Z6Vfx.glinster(sl, position().add(0, 0.5, 0), 2);
            sl.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1f, 1.3f);
            // the real one jumps a bit too, so the player can't just follow the first
            Vec3 a = position().add(getRandom().nextDouble() * 3 - 1.5, 0, getRandom().nextDouble() * 3 - 1.5);
            setPos(a.x, grond(a), a.z);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (kopie) {
                if (--kopieT <= 0) {
                    Z6Vfx.glinster(sl, position(), 0.8);
                    discard();
                }
                return;
            }
            LivingEntity d = getTarget();
            kopieen.removeIf(u -> !(sl.getEntity(u) instanceof Spiegelslang k) || !k.isAlive());
            if (geldig(d) && kopieen.isEmpty() && --spiegelPauze <= 0) {
                spiegelPauze = 20 * 15;
                spiegel(sl);
            }
            if (laden > 0 && --laden == 0) {
                speel("speciaal");
                Vec3 van = position();
                setDeltaMovement(naar.subtract(van).multiply(1, 0, 1).scale(0.35).add(0, 0.2, 0));
                for (Player p : spelersRond(van.lerp(naar, 0.5), 3.2)) {
                    if (nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas.inStrook(p.position(), van, naar, 1.8)) {
                        klap(p, 1.25, Schade.Soort.MELEE, "spiegelbeet");
                    }
                }
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 7) {
                pauze = 110 + getRandom().nextInt(30);
                laden = 16;
                Vec3 r = d.position().subtract(position()).multiply(1, 0, 1);
                naar = position().add(r.lengthSqr() < 0.01 ? voor() : r.normalize().scale(Math.min(6, r.length() + 1)));
                Z6Vfx.lijn(sl, position(), naar, 1.8, 16, Z6Vfx.ROOD);
            }
        }
    }

    // =====================================================================================================================
    // Zoutkreeft: a sideways salt crayfish with a crust-shield of salt crystal. Hits from the FRONT or sides do only 35 %; from
    // BEHIND it is soft: 150 % ("Zwakke plek!"). Every ~6 s its claws snap: a red circle in front (0.8 s).
    // =====================================================================================================================
    public static class Zoutkreeft extends Z6Wezen {
        private int pauze = 60, laden = -1, zijT;
        private Vec3 plek;

        public Zoutkreeft(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "zoutkreeft";
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double hpBasis() {
            return 40;
        }

        @Override
        protected double klapBasis() {
            return 6.6;
        }

        @Override
        public double aggroBereik() {
            return 8;
        }

        /** The damage factor for a hit from this attacker: behind = soft (1.5), else the crust (0.35). */
        public double schildFactor(@Nullable LivingEntity bron) {
            if (bron == null) {
                return 1;
            }
            Vec3 d = bron.position().subtract(position()).multiply(1, 0, 1);
            if (d.lengthSqr() < 0.01) {
                return 1;
            }
            return d.normalize().dot(voor()) < -0.35 ? 1.5 : 0.35;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity d = getTarget();
            if (geldig(d) && ++zijT % 40 == 0 && onGround() && laden <= 0) {   // a little sideways scuttle
                Vec3 zij = voor().yRot((float) (Math.PI / 2)).scale(getRandom().nextBoolean() ? 0.35 : -0.35);
                setDeltaMovement(zij.x, 0.1, zij.z);
            }
            if (laden > 0 && --laden == 0) {
                speel("aanval");
                for (Player p : spelersRond(plek, 2.4)) {
                    klap(p, 1.3, Schade.Soort.MELEE, "zoutknijp");
                }
                sl.playSound(null, blockPosition(), SoundEvents.ARMADILLO_SCUTE_DROP, SoundSource.HOSTILE, 1f, 1.2f);
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) < 4) {
                pauze = 110 + getRandom().nextInt(30);
                laden = 16;
                plek = position().add(voor().scale(1.8));
                plek = new Vec3(plek.x, grond(plek), plek.z);
                Z6Vfx.cirkel(sl, plek, 2.4, 16, Z6Vfx.ROOD);
                mopper(sl);
            }
        }
    }

    // =====================================================================================================================
    // Zandgeestje: a whirling dust devil full of caught star dust (only at night on the salt flat). Every ~6 s a purple circle
    // (1 s): it sucks everyone within 5 blocks in (dizzy). Beaten it bursts and drops a pinch of sterrenstof.
    // =====================================================================================================================
    public static class Zandgeestje extends Z6Wezen {
        private int pauze = 50, laden = -1;

        public Zandgeestje(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "zandgeestje";
        }

        @Override
        public Afloop afloop() {
            return Afloop.VERSLAAN;
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
            return 26;
        }

        @Override
        protected double klapBasis() {
            return 5.6;
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
            return 0.6;
        }

        @Override
        protected double vliegSnelheid() {
            return 0.12;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (doel == null || laden > 0) {
                return laden > 0 ? position() : null;
            }
            return distanceTo(doel) < 2.5 ? position() : doel.position().add(0, 0.6, 0);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (tickCount % 4 == 0) {
                sl.sendParticles(new DustParticleOptions(Z6Vfx.ZAND, 1.1f), getX(), getY() + 0.6, getZ(), 3, 0.4, 0.6, 0.4, 0.02);
            }
            if (getTarget() == null && sl.isBrightOutside() && tickCount % 100 == 0 && !spawner.isEmpty()) {
                Z6Vfx.zand(sl, position(), 1);   // morning: it settles into the salt
                discard();
                return;
            }
            Vec3 g = new Vec3(getX(), grond(position()), getZ());
            if (laden > 0 && --laden == 0) {
                speel("speciaal");
                for (Player p : spelersRond(g, 5.2)) {
                    Vec3 r = position().subtract(p.position()).multiply(1, 0, 1);
                    if (r.lengthSqr() > 0.01) {
                        p.push(r.normalize().x * 0.8, 0.2, r.normalize().z * 0.8);
                        p.hurtMarked = true;
                    }
                    klap(p, 0.6, Schade.Soort.MAGIE, "wervelzuig");
                    Statussen.geef(p, Status.DUIZELIG, 40, 1, this);
                }
                sl.playSound(null, blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.HOSTILE, 1f, 1.5f);
            } else if (laden <= 0 && geldig(getTarget()) && --pauze <= 0 && distanceTo(getTarget()) < 6) {
                pauze = 110 + getRandom().nextInt(30);
                laden = 20;
                Z6Vfx.cirkel(sl, g, 5.2, 20, Z6Vfx.PAARS);
            }
        }
    }

    // =====================================================================================================================
    // De Zoutslang (the elite of De Zingende Zoutgrot, lv 66): a long snake of pink salt crystal that swims THROUGH the walls.
    // Noise draws her: a player who sprints or hits within 20 blocks is heard. Every ~9 s she dives into the rock (gone, can't be
    // hit, a trail of salt dust under the ground), then bursts out next to the noisiest player along a red strip (1 s).
    // =====================================================================================================================
    public static class Zoutslang extends Z6Wezen {
        private static final EntityDataAccessor<Boolean> DATA_ONDER = SynchedEntityData.defineId(Zoutslang.class, EntityDataSerializers.BOOLEAN);
        private int onderT = -1, duikPauze = 120;
        private Vec3 uit, naar;
        @Nullable
        private UUID luidste;
        /** Gametests: dives done. */
        public int duiken;

        public Zoutslang(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(DATA_ONDER, false);
        }

        public boolean onder() {
            return entityData.get(DATA_ONDER);
        }

        @Override
        public String naam() {
            return "zoutslang";
        }

        @Override
        public Afloop afloop() {
            return Afloop.VERSLAAN;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double hpBasis() {
            return 76;
        }

        @Override
        protected double klapBasis() {
            return 8.2;
        }

        @Override
        protected double xpGewicht() {
            return 4;
        }

        @Override
        public double aggroBereik() {
            return 7;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == ONDER ? "onder" : null;
        }

        @Override
        public void stelIn(int level, @Nullable Element element, boolean elite, Set<Affix> affixen) {
            super.stelIn(level, element, true, affixen);   // always the elite (gold glow, bigger)
        }

        @Override
        public void krijgSchade(double n, @Nullable LivingEntity bron) {
            if (onder()) {
                if (bron instanceof ServerPlayer p) {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.z6wezens.zoutslang.onder").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                return;
            }
            if (bron instanceof ServerPlayer p) {
                luidste = p.getUUID();   // hitting makes noise too
            }
            super.krijgSchade(n, bron);
        }

        /** The noisiest player within 20 blocks: sprinting, or the last one to hit her; else her target. */
        @Nullable
        private Player luidste() {
            Player best = null;
            for (Player p : spelersRond(position(), 20)) {
                if (p.isSprinting()) {
                    return p;
                }
                if (p.getUUID().equals(luidste)) {
                    best = p;
                }
            }
            return best != null ? best : getTarget() instanceof Player t ? t : null;
        }

        /** Dives into the rock now (dev / tests). */
        public void duik(ServerLevel sl) {
            duiken++;
            entityData.set(DATA_ONDER, true);
            zetHouding(ONDER);
            onderT = 50;
            Z6Vfx.zand(sl, position(), 1.6);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.5f, 0.6f);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (getTarget() == null) {   // noise draws her
                for (Player p : spelersRond(position(), 20)) {
                    if (p.isSprinting() && geldig(p)) {
                        setTarget(p);
                        break;
                    }
                }
            }
            if (onderT >= 0) {
                getNavigation().stop();
                setDeltaMovement(Vec3.ZERO);
                if (onderT % 6 == 0) {
                    sl.sendParticles(new DustParticleOptions(0xF4B8D0, 1.2f), getX(), getY() + 0.2, getZ(), 6, 0.6, 0.1, 0.6, 0);
                }
                if (onderT-- == 20) {   // where she will come out: next to the noisiest player, a red strip across them
                    Player p = luidste();
                    Vec3 doel = p != null ? p.position() : position();
                    Vec3 r = doel.subtract(position()).multiply(1, 0, 1);
                    r = r.lengthSqr() < 0.01 ? voor() : r.normalize();
                    uit = doel.subtract(r.scale(4));
                    uit = new Vec3(uit.x, grond(uit), uit.z);
                    naar = doel.add(r.scale(3));
                    Z6Vfx.lijn(sl, uit, naar, 2.0, 20, Z6Vfx.ROOD);
                    setPos(uit.x, uit.y, uit.z);
                }
                if (onderT < 0) {
                    entityData.set(DATA_ONDER, false);
                    zetHouding(0);
                    speel("speciaal");
                    Z6Vfx.zand(sl, position(), 2);
                    sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 2f, 0.7f);
                    for (Player p : spelersRond(uit.lerp(naar, 0.5), 5)) {
                        if (nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas.inStrook(p.position(), uit, naar, 2.0)) {
                            klap(p, 1.6, Schade.Soort.MELEE, "zoutuitbarsting");
                        }
                    }
                    setDeltaMovement(naar.subtract(uit).multiply(1, 0, 1).scale(0.25).add(0, 0.3, 0));
                }
                return;
            }
            if (geldig(getTarget()) && --duikPauze <= 0) {
                duikPauze = 160 + getRandom().nextInt(40);
                duik(sl);
            }
        }
    }
}
