package nl.juiced.guhs.feature.knabbelspelen;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Eierlopen met knabbelei: a knabbelei on a spoon, through the flags of your lane (left, right, left, right) to the
 * finish. Walk nicely: running, sharp turns and jumping make the egg wobble ({@link Staat#wiebel}); too wild and it
 * drops - back to the last flag with a fresh egg. Time is the score.
 */
public final class Eierlopen implements Wedstrijd.Spel {
    public static final Eierlopen SPEL = new Eierlopen();
    /** Faster than this (blocks/tick) wobbles; turning faster than this (degrees/tick) too. */
    public static final double RUSTIG = 0.165, DRAAI = 12;
    /** Within this distance of a flag's spot the flag counts. */
    public static final double VLAG = 1.35;

    static final class Staat {
        int vlag, gevallen, melding;
        double wiebel;
        Vec3 vorige = Vec3.ZERO;
        float vorigeYaw;
        double herstartU;
        double herstartS;
    }

    static Staat staat(Wedstrijd.Deelnemer d) {
        if (d.staat instanceof Staat s) {
            return s;
        }
        Staat s = new Staat();
        d.staat = s;
        return s;
    }

    @Override
    public void klaarzetten(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        Staat s = staat(d);
        w.naarStart(d, p, level, 0);
        s.vorige = p.position();
        s.vorigeYaw = p.getYRot();
        Wedstrijd.inHand(p, new ItemStack(KnabbelspelenFeature.KNABBELEI_LEPEL.get()));
        Wedstrijd.effect(p, MobEffects.MOVEMENT_SLOWDOWN, Onderdeel.EIERLOPEN.maxTicks + Wedstrijd.AFTEL_TICKS + 40, 1);
    }

    @Override
    public void start(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        Staat s = staat(d);
        s.vorige = p.position();
        s.vorigeYaw = p.getYRot();
        s.wiebel = 0;
    }

    /** The spot of flag i of lane k (next to the flag, where you walk past it). */
    static Vec3 vlag(Wedstrijd w, int k, int i) {
        return Speelvelden.punt(w.anker, Onderdeel.EIERLOPEN, k, Speelvelden.EI_VLAGGEN[i], Speelvelden.EI_KANT[i] * 1.0, Speelvelden.G + 1);
    }

    @Override
    public void tick(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level, int t) {
        Staat s = staat(d);
        Vec3 pos = p.position();
        double snelheid = Math.sqrt(Math.pow(pos.x - s.vorige.x, 2) + Math.pow(pos.z - s.vorige.z, 2));
        double draai = Math.abs(Mth.wrapDegrees(p.getYRot() - s.vorigeYaw));
        s.vorige = pos;
        s.vorigeYaw = p.getYRot();
        if (snelheid < 1.5) {                                   // (not a teleport)
            s.wiebel += Math.max(0, snelheid - RUSTIG) * 9 + Math.max(0, draai - DRAAI) * 0.01 + (p.isSprinting() ? 0.05 : 0);
        }
        s.wiebel = Math.max(0, s.wiebel - 0.012);
        if (s.wiebel >= 1) {
            valt(w, d, p, level);
            return;
        }
        if (s.vlag < Speelvelden.EI_VLAGGEN.length) {
            Vec3 v = vlag(w, d.baan, s.vlag);
            if (Math.hypot(pos.x - v.x, pos.z - v.z) < VLAG) {
                s.herstartU = Speelvelden.EI_VLAGGEN[s.vlag];
                s.herstartS = Speelvelden.EI_KANT[s.vlag];
                s.vlag++;
                level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1f + 0.15f * s.vlag);
                p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.ei.vlag", s.vlag, Speelvelden.EI_VLAGGEN.length)
                        .withStyle(ChatFormatting.GREEN));
            }
        }
        double[] b = Speelvelden.baan(w.anker, Onderdeel.EIERLOPEN, d.baan, pos);
        if (b[0] >= Speelvelden.EI_FINISH) {
            if (s.vlag >= Speelvelden.EI_VLAGGEN.length) {
                w.klaar(d, p, t, false);
                return;
            }
            if (s.melding-- <= 0) {
                p.sendSystemMessage(Component.translatable("quest.guhs.knabbelspelen.ei.gemist", s.vlag + 1).withStyle(ChatFormatting.GOLD));
                s.melding = 60;
            }
        }
        if (t % 3 == 0) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.ei.bar", nl.juiced.guhs.quest.Highscores.tijd(t), meter(s.wiebel),
                    s.vlag, Speelvelden.EI_VLAGGEN.length));
        }
    }

    /** The wobble meter: green when calm, red when it's about to drop. */
    static Component meter(double wiebel) {
        int n = (int) Math.round(Math.min(1, wiebel) * 10);
        ChatFormatting kleur = n < 4 ? ChatFormatting.GREEN : n < 7 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return Component.literal("|".repeat(n)).withStyle(kleur).append(Component.literal("|".repeat(10 - n)).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Oops: the egg drops. Back to the last flag, with a new egg. */
    static void valt(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        Staat s = staat(d);
        s.gevallen++;
        s.wiebel = 0;
        level.playSound(null, p.blockPosition(), KnabbelspelenFeature.EI_KAPOT.get(), SoundSource.PLAYERS, 1f, 1f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(net.minecraft.world.item.Items.EGG)), p.getX(), p.getY() + 0.8, p.getZ(),
                14, 0.2, 0.2, 0.2, 0.1);
        Vec3 terug = Speelvelden.punt(w.anker, Onderdeel.EIERLOPEN, d.baan, s.herstartU, s.herstartS * 0.5, Speelvelden.G + 1);
        Wedstrijd.teleport(p, level, terug.x, terug.y, terug.z, Speelvelden.yaw(w.anker, Onderdeel.EIERLOPEN));
        s.vorige = terug;
        s.vorigeYaw = Speelvelden.yaw(w.anker, Onderdeel.EIERLOPEN);
        p.sendSystemMessage(Component.translatable("quest.guhs.knabbelspelen.ei.valt").withStyle(ChatFormatting.GOLD));
    }

    /** A jump while carrying the egg: a big wobble (from Zaklopen.onJump, which listens to all jumps). */
    static void gesprongen(ServerPlayer p) {
        if (!Wedstrijd.speelt(p, Onderdeel.EIERLOPEN)) {
            return;
        }
        Wedstrijd w = Wedstrijd.van(p);
        Wedstrijd.Deelnemer d = w == null ? null : w.deelnemer(p);
        if (d != null) {
            staat(d).wiebel += 0.8;
        }
    }

    @Override
    public void einde(Wedstrijd w, Wedstrijd.Deelnemer d, @Nullable ServerPlayer p, ServerLevel level) {
        if (p != null) {
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private Eierlopen() {
    }
}
