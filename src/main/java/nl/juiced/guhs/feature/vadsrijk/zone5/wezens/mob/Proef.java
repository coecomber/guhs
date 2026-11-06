package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensApi;

/** De Proefarena van de Wolkwacht (lv 56-57): the Bezeten Oefenpop and the Sparringridder. */
public final class Proef {
    private Proef() {
    }

    /**
     * Bezeten Oefenpop: a straw training dummy that grey fluff (blown in by the saboteurs) has crept into. It stands still on its
     * pole like any other dummy (no nameplate, not hittable) until you come within 5 blocks: then it hops off and bonks. Every ~6 s
     * it spins (red circle 1 s). Defeated, the grey fluff blows out and it is an ordinary dummy again. Idle 15 s: it stands still again.
     * Its area leader is Opperpop Strobaal.
     */
    public static class BezetenOefenpop extends Z5Wezen {
        public static final int DRAAI = 1;
        private int draai = -1, draaiPauze = 80, verveeld;

        public BezetenOefenpop(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            zetHouding(VERBORGEN);
        }

        @Override
        public String naam() {
            return "bezeten_oefenpop";
        }

        @Override
        public Soort soort() {
            return Soort.GRAUW;
        }

        @Override
        protected double basisVads(int level) {
            return vads5(46, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade5(8, level);
        }

        @Override
        public double aggroBereik() {
            return 5;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.WOOL_HIT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == VERBORGEN ? "stil" : h == DRAAI ? "draai" : null;
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
            if (verborgen()) {
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y, 0);
                Player p = sl.getNearestPlayer(this, 5);
                if (p != null && !p.isCreative() && !p.isSpectator() || getTarget() instanceof Player) {
                    zetHouding(0);
                    speel("aanval");
                    zegBoven(sl, "oefenpop.wakker");
                    sl.sendParticles(new DustParticleOptions(0x9A9AA6, 1.4f), getX(), getY() + 1.2, getZ(), 20, 0.4, 0.6, 0.4, 0);
                    if (p != null && getTarget() == null) {
                        setTarget(p);
                    }
                    verveeld = 0;
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (!geldig(doel)) {
                if (++verveeld > 300 && draai < 0) {
                    zetHouding(VERBORGEN);
                }
                return;
            }
            verveeld = 0;
            if (draai >= 0) {
                getNavigation().stop();
                if (draai-- == 0) {
                    zetHouding(0);
                    sl.playSound(null, blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.4f, 0.6f);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 6, 1.6, 0.2, 1.6, 0);
                    klapRond(position(), 3.2, 1.6, Schade.Soort.MELEE, "oefenpop_draai");
                }
                return;
            }
            if (--draaiPauze <= 0 && distanceTo(doel) < 5) {
                draaiPauze = 120 + getRandom().nextInt(40);
                draai = 20;
                zetHouding(DRAAI);
                Telegraaf.cirkel(sl, position(), 3.2, 20, Telegraaf.ROOD);
            }
        }
    }

    /**
     * Sparringridder: a knight guh of the Wolkwacht with a pillow sword and a round training shield. A GUH: never hostile, never
     * hurts anyone. Right-click it to spar ("Op je plaats... sparren!"): then it swings its pillow along a red strip every 3 s
     * (step out: a hit is only a "Kussenklap!", 3 of them and you lose the bout), and every ~7 s it holds up its shield (pink
     * sparkle: hits then do nothing, "Geblokt!"). Bring its stamina (its bar) down to 5 % and it bows: "Goed gevochten!" (xp, a
     * Proefpenning tally, now and then a Wolkwacht piece). Only its sparring partner can hit it; it never faints, never drops loot by
     * itself, and comes back to full stamina after every bout.
     */
    public static class Sparringridder extends Z5Wezen {
        public static final int BLOK = 1, SWING = 2, BUIG = 3;
        public static final int PLUIMPJES = 3, DUUR = 20 * 90;
        @Nullable
        private UUID partner;
        private int t, swing = -1, blok = -1, swingPauze = 60, blokPauze = 140, pluimpjes, buig = -1;
        private Vec3 swingRichting = Vec3.ZERO;
        /** Gametests: bouts won/lost. */
        public int gewonnen, verloren;

        public Sparringridder(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "sparringridder";
        }

        @Override
        public Soort soort() {
            return Soort.GUH;
        }

        @Override
        public Element standaardElement() {
            return Element.ROZE;
        }

        @Override
        protected double basisVads(int level) {
            return vads5(40, level);
        }

        @Override
        protected double basisSchade(int level) {
            return 0;
        }

        @Override
        public int xp() {
            return 0;
        }

        @Nullable
        @Override
        public String lootTabel() {
            return null;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.WOOL_HIT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == BLOK ? "blok" : h == SWING ? "mik" : h == BUIG ? "buig" : null;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
            this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        }

        @Override
        public void setTarget(@Nullable LivingEntity t) {
            // a guh never targets anyone
        }

        @Override
        public boolean isPushable() {
            return false;
        }

        @Nullable
        public UUID partner() {
            return partner;
        }

        public boolean blokt() {
            return blok >= 0;
        }

        @Override
        protected InteractionResult mobInteract(Player p, InteractionHand hand) {
            if (hand != InteractionHand.MAIN_HAND || !(p instanceof ServerPlayer sp) || !(level() instanceof ServerLevel sl)) {
                return InteractionResult.PASS;
            }
            if (partner != null && !partner.equals(sp.getUUID())) {
                sp.sendOverlayMessage(Component.translatable("vadsrijk.z5wezens.sparren.bezet").withStyle(ChatFormatting.GOLD));
                return InteractionResult.SUCCESS_SERVER;
            }
            if (partner == null && buig < 0) {
                begin(sl, sp);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        /** Starts a bout with this player (also the dev command / gametests). */
        public void begin(ServerLevel sl, ServerPlayer p) {
            partner = p.getUUID();
            t = 0;
            pluimpjes = 0;
            swing = -1;
            blok = -1;
            swingPauze = 50;
            blokPauze = 130;
            heal(maxVads());
            zetHouding(0);
            Tips.toon(p, "vadsrijk.z5wezens.tip.sparringridder");
            p.sendSystemMessage(Component.translatable("vadsrijk.z5wezens.sparren.start").withStyle(ChatFormatting.LIGHT_PURPLE));
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1f, 1.4f);
        }

        @Nullable
        private ServerPlayer partnerSpeler(ServerLevel sl) {
            return partner != null && sl.getPlayerByUUID(partner) instanceof ServerPlayer p && p.isAlive() ? p : null;
        }

        @Override
        public void krijgSchade(double n, @Nullable LivingEntity bron) {
            if (geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (partner == null || bron == null || !partner.equals(bron.getUUID())) {
                if (bron instanceof ServerPlayer p) {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.z5wezens.sparren.eerst_klikken").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                return;
            }
            if (blokt()) {
                Getallen.tekst(sl, getX(), getY() + getBbHeight() + 0.4, getZ(), Component.translatable("vadsrijk.z5wezens.sparren.geblokt"), 0.8f);
                sl.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.NEUTRAL, 1f, 1.3f);
                return;
            }
            double vloer = maxVads() * 0.05;
            if (vads() - n <= vloer) {
                heal(vloer - vads());
                if (bron instanceof ServerPlayer p) {
                    gewonnen(sl, p);
                }
                return;
            }
            heal(-n);
            hurtTime = 10;
            sl.broadcastEntityEvent(this, (byte) 2);
            if (bron instanceof ServerPlayer p) {
                Getallen.toon(sl, this, n, false, Element.ROZE, 1);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (buig >= 0) {
                getNavigation().stop();
                if (buig-- == 0) {
                    zetHouding(0);
                    heal(maxVads());
                }
                return;
            }
            if (partner == null) {
                if (tickCount % 40 == 0 && vads() < maxVads()) {
                    heal(maxVads());
                }
                return;
            }
            ServerPlayer p = partnerSpeler(sl);
            if (p == null || p.distanceTo(this) > 16 || ++t > DUUR) {
                if (p != null) {
                    verloren(sl, p, "weg");
                } else {
                    stop();
                }
                return;
            }
            lookAt(p, 30, 30);
            double d = p.distanceTo(this);
            if (swing < 0 && blok < 0) {
                if (d > 3.2 && tickCount % 10 == 0) {
                    getNavigation().moveTo(p, 0.9);
                } else if (d < 1.6) {
                    getNavigation().stop();
                }
            }
            if (blok >= 0 && blok-- == 0) {
                zetHouding(0);
            }
            if (blok > 0 && blok % 5 == 0) {
                sl.sendParticles(new DustParticleOptions(0xFF5FC8, 1.2f), getX() + swingVoor().x * 0.8, getY() + 1.1, getZ() + swingVoor().z * 0.8, 3, 0.2, 0.3, 0.2, 0);
            }
            if (swing >= 0) {
                getNavigation().stop();
                if (swing-- == 0) {
                    zetHouding(0);
                    speel("aanval");
                    sl.playSound(null, blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1.4f, 1.2f);
                    sl.sendParticles(ParticleTypes.CLOUD, getX() + swingRichting.x * 2, getY() + 1, getZ() + swingRichting.z * 2, 10, 1, 0.3, 1, 0.02);
                    if (inStrook(position(), swingRichting, 4.5, 2.6, p.position())) {
                        pluimpjes++;
                        Getallen.tekst(sl, p.getX(), p.getY() + 2.2, p.getZ(), Component.translatable("vadsrijk.z5wezens.sparren.kussenklap", pluimpjes, PLUIMPJES), 0.9f);
                        duw(p, swingRichting.scale(0.6).add(0, 0.25, 0));
                        if (pluimpjes >= PLUIMPJES) {
                            verloren(sl, p, "klappen");
                        }
                    }
                }
                return;
            }
            if (--blokPauze <= 0) {
                blokPauze = 130 + getRandom().nextInt(40);
                blok = 30;
                zetHouding(BLOK);
                return;
            }
            if (--swingPauze <= 0 && d < 5) {
                swingPauze = 55 + getRandom().nextInt(15);
                swing = 16;
                swingRichting = richting(position(), p.position());
                zetHouding(SWING);
                Telegraaf.lijn(sl, position(), position().add(swingRichting.scale(4.5)), 2.6, 16, Telegraaf.ROOD);
            }
        }

        private Vec3 swingVoor() {
            return voor();
        }

        private void stop() {
            partner = null;
            swing = -1;
            blok = -1;
            zetHouding(0);
            heal(maxVads());
        }

        /** Gametests/dev: a sweep now at the partner (skips the wind-up). */
        public void zwaaiNu(ServerLevel sl) {
            ServerPlayer p = partnerSpeler(sl);
            if (p != null) {
                swingRichting = richting(position(), p.position());
                swing = 0;
            }
        }

        private void verloren(ServerLevel sl, ServerPlayer p, String waarom) {
            verloren++;
            p.sendSystemMessage(Component.translatable("vadsrijk.z5wezens.sparren.verloren." + waarom).withStyle(ChatFormatting.GOLD));
            stop();
        }

        private void gewonnen(ServerLevel sl, ServerPlayer p) {
            gewonnen++;
            stop();
            buig = 50;
            zetHouding(BUIG);
            speel("aanval");
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 6, 0.4, 0.3, 0.4, 0);
            sl.sendParticles(new DustParticleOptions(0xFFD62A, 1.4f), getX(), getY() + 1, getZ(), 30, 0.8, 0.8, 0.8, 0);
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.5f);
            CompoundTag extra = Spelers.data(p).extra("z5wezens");
            int n = extra.getIntOr("sparren", 0) + 1;
            extra.putInt("sparren", n);
            Spelers.opgeslagen(p);
            p.sendSystemMessage(Component.translatable("vadsrijk.z5wezens.sparren.gewonnen", n).withStyle(ChatFormatting.LIGHT_PURPLE));
            Spelers.geefXp(p, xp5(6), "sparren");
            LootTabel tabel = LootTabellen.van("z5wezens:sparren");
            if (tabel != null && (n == 1 || getRandom().nextDouble() < 0.3)) {
                LootBundel.laatVallen(p, position().add(0, 0.8, 0), tabel.rol(Math.max(56, mobLevel()), getRandom()), 0);
            }
            Z5WezensApi.gespard(p, n);
        }
    }
}
