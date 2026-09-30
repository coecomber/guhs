package nl.juiced.guhs.feature.knabbelspelen;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Guhguhtje prik: a big guh on a board (seen from behind, with its tail missing). A blindfold on, spun around three
 * times, and then find the board and pin the tail on (right-click the board with the staartje). The closer to the spot
 * where the tail belongs, the more points: 1000 right on it, 0 from {@value #MIS} blocks away.
 */
public final class GuhguhtjePrik implements Wedstrijd.Spel {
    public static final GuhguhtjePrik SPEL = new GuhguhtjePrik();
    public static final int DRAAI_TICKS = 40;
    public static final double MIS = 2.5;

    static final class Staat {
        int draai;
        float startYaw, extra;
        boolean geprikt;
        @Nullable
        SpelDing staartje;
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
    public double startU() {
        return Speelvelden.PRIK_MAT;
    }

    @Override
    public void klaarzetten(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        staat(d);
        w.naarStart(d, p, level, Speelvelden.PRIK_MAT);
        Wedstrijd.inHand(p, new ItemStack(KnabbelspelenFeature.GUHGUHTJE_STAARTJE.get()));
    }

    @Override
    public void aftellen(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        w.houdBijStart(d, p, level);
        if (w.ticks == 1) {
            blinddoek(p, true);
        }
    }

    @Override
    public void start(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        Staat s = staat(d);
        s.draai = DRAAI_TICKS;
        s.startYaw = p.getYRot();
        s.extra = level.getRandom().nextFloat() * 360f;
        blinddoek(p, true);
    }

    @Override
    public void tick(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level, int t) {
        Staat s = staat(d);
        if (s.draai > 0) {
            // spun around three times (and a bit), standing on the mat
            float yaw = s.startYaw + (DRAAI_TICKS - s.draai + 1) * (360f * 3 / DRAAI_TICKS) + s.extra * (1 - (s.draai - 1) / (float) DRAAI_TICKS);
            Vec3 m = Speelvelden.punt(w.anker, Onderdeel.GUHGUHTJE_PRIK, d.baan, Speelvelden.PRIK_MAT, 0, Speelvelden.G + 1);
            draai(p, level, m, yaw);
            s.draai--;
            if (s.draai % 13 == 0) {
                level.playSound(null, p.blockPosition(), KnabbelspelenFeature.HOP.get(), SoundSource.PLAYERS, 0.6f, 1.6f);
            }
            if (s.draai == 0) {
                p.sendSystemMessage(Component.translatable("quest.guhs.knabbelspelen.prik.zoek").withStyle(ChatFormatting.AQUA));
            }
            return;
        }
        if (t % 10 == 0) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.prik.bar", (Onderdeel.GUHGUHTJE_PRIK.maxTicks - t + 19) / 20)
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    private static void draai(ServerPlayer p, ServerLevel level, Vec3 m, float yaw) {
        if (p instanceof FakePlayer || p.connection == null) {
            p.snapTo(m.x, m.y, m.z, yaw, p.getXRot());
        } else {
            p.connection.teleport(new net.minecraft.world.entity.PositionMoveRotation(new net.minecraft.world.phys.Vec3(m.x, m.y, m.z), net.minecraft.world.phys.Vec3.ZERO, yaw, 0f), Set.of(Relative.X_ROT));
        }
    }

    /** The staartje on the board (from GuhguhtjeStaartjeItem.useOn): how close to its spot? */
    static boolean prik(ServerPlayer p, Vec3 hit, BlockPos blok) {
        if (!Wedstrijd.speelt(p, Onderdeel.GUHGUHTJE_PRIK)) {
            return false;
        }
        Wedstrijd w = Wedstrijd.van(p);
        Wedstrijd.Deelnemer d = w == null ? null : w.deelnemer(p);
        if (d == null) {
            return false;
        }
        Staat s = staat(d);
        if (s.draai > 0 || s.geprikt) {
            return false;
        }
        double[] b = Speelvelden.baan(w.anker, Onderdeel.GUHGUHTJE_PRIK, d.baan, Vec3.atCenterOf(blok));
        if (Math.abs(b[0] - Speelvelden.PRIK_BORD) > 0.6 || Math.abs(b[1]) > 2.6 || b[2] < Speelvelden.G + 0.5 || b[2] > Speelvelden.G + 6.5) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.prik.geen_bord").withStyle(ChatFormatting.GOLD));
            return false;
        }
        prikOp(w, d, p, hit);
        return true;
    }

    /** (Also for the tests) the tail goes on here: the score, the tail stays on the board, the blindfold comes off. */
    static int prikOp(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, Vec3 hit) {
        Staat s = staat(d);
        ServerLevel level = p.level();
        double[] h = Speelvelden.baan(w.anker, Onderdeel.GUHGUHTJE_PRIK, d.baan, hit);
        double afstand = Math.hypot(h[1], h[2] - Speelvelden.PRIK_DOEL_Y);
        int score = score(afstand);
        s.geprikt = true;
        SpelDing staart = KnabbelspelenFeature.DING.get().create(level, EntitySpawnReason.TRIGGERED);
        if (staart != null) {
            staart.soort(SpelDing.STAARTJE);
            staart.spel = w.npcId;
            staart.baan = d.baan;
            Vec3 at = Speelvelden.punt(w.anker, Onderdeel.GUHGUHTJE_PRIK, d.baan, Speelvelden.PRIK_BORD - 0.55, h[1], h[2]);
            staart.snapTo(at.x, at.y - 0.25, at.z, Speelvelden.yaw(w.anker, Onderdeel.GUHGUHTJE_PRIK), 0);
            level.addFreshEntity(staart);
            s.staartje = staart;
        }
        blinddoek(p, false);
        level.playSound(null, p.blockPosition(), KnabbelspelenFeature.JUICH.get(), SoundSource.PLAYERS, 1f, score >= 800 ? 1.3f : 0.9f);
        level.sendParticles(score >= 800 ? ParticleTypes.HEART : ParticleTypes.POOF, hit.x, hit.y, hit.z, 8, 0.2, 0.2, 0.2, 0.02);
        p.sendSystemMessage(Component.translatable(score >= 950 ? "quest.guhs.knabbelspelen.prik.raak" : "quest.guhs.knabbelspelen.prik.naast",
                String.format(java.util.Locale.ROOT, "%.1f", afstand), score).withStyle(ChatFormatting.YELLOW));
        w.klaar(d, p, score, false);
        return score;
    }

    /** 1000 right on the spot, 0 from MIS blocks away. */
    public static int score(double afstand) {
        return (int) Math.round(1000 * Math.max(0, 1 - afstand / MIS));
    }

    @Override
    public int eindScore(Wedstrijd.Deelnemer d) {
        return 0;
    }

    @Override
    public void einde(Wedstrijd w, Wedstrijd.Deelnemer d, @Nullable ServerPlayer p, ServerLevel level) {
        if (p != null) {
            blinddoek(p, false);
        }
    }

    /** The blindfold: on (dark screen, blind) or off. */
    public static void blinddoek(ServerPlayer p, boolean aan) {
        if (aan) {
            Wedstrijd.effect(p, MobEffects.BLINDNESS, Onderdeel.GUHGUHTJE_PRIK.maxTicks + Wedstrijd.AFTEL_TICKS + 40, 0);
        } else {
            var e = p.getEffect(MobEffects.BLINDNESS);
            if (e != null && !e.isVisible()) {
                p.removeEffect(MobEffects.BLINDNESS);
            }
        }
        if (!(p instanceof FakePlayer) && p.connection != null) {
            nl.juiced.guhs.network.ModNetworking.sendTo(p, new KnabbelspelenPayloads.Blinddoek(aan));
        }
    }

    private GuhguhtjePrik() {
    }
}
