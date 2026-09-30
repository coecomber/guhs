package nl.juiced.guhs.feature.knabbelspelen;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Zaklopen: you stand in a guh sack, so walking hardly works: hop (jump) and every hop takes you forward the way you
 * look. Over the humps to the finish line, as fast as you can. Time is the score.
 */
public final class Zaklopen implements Wedstrijd.Spel {
    public static final Zaklopen SPEL = new Zaklopen();
    /** How hard a hop pushes you forward (blocks per tick). */
    public static final double HOP = 0.42;

    static final class Staat {
        int hops;
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
        staat(d);
        w.naarStart(d, p, level, 0);
        Wedstrijd.inHand(p, new ItemStack(KnabbelspelenFeature.GUH_ZAK.get()));
        Wedstrijd.effect(p, MobEffects.MOVEMENT_SLOWDOWN, Onderdeel.ZAKLOPEN.maxTicks + Wedstrijd.AFTEL_TICKS + 40, 4);
    }

    @Override
    public void tick(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level, int t) {
        double[] b = Speelvelden.baan(w.anker, Onderdeel.ZAKLOPEN, d.baan, p.position());
        if (b[0] >= Speelvelden.ZAK_FINISH) {
            w.klaar(d, p, t, false);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.5, 0.4, 0.2);
            return;
        }
        if (t % 5 == 0) {
            p.displayClientMessage(Component.translatable("quest.guhs.knabbelspelen.zak.bar", nl.juiced.guhs.quest.Highscores.tijd(t),
                    Math.max(0, (int) Math.ceil(Speelvelden.ZAK_FINISH - b[0]))).withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** A jump in the sack: a hop forward (only while playing zaklopen; before the whistle it does nothing). */
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Wedstrijd.isPlaying(p)) {
            return;
        }
        Wedstrijd w = Wedstrijd.van(p);
        if (w == null) {
            return;
        }
        if (w.onderdeel() == Onderdeel.EIERLOPEN) {
            Eierlopen.gesprongen(p);
            return;
        }
        if (w.onderdeel() != Onderdeel.ZAKLOPEN || !Wedstrijd.speelt(p, Onderdeel.ZAKLOPEN)) {
            return;
        }
        Wedstrijd.Deelnemer d = w.deelnemer(p);
        if (d == null) {
            return;
        }
        hop(p);
        staat(d).hops++;
    }

    /** (Also for the tests) one hop forward. */
    static void hop(ServerPlayer p) {
        Vec3 kijk = p.getLookAngle().multiply(1, 0, 1);
        if (kijk.lengthSqr() < 1e-4) {
            return;
        }
        Vec3 v = kijk.normalize().scale(HOP);
        p.setDeltaMovement(v.x, Math.max(p.getDeltaMovement().y, 0.42), v.z);
        p.hurtMarked = true;
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), KnabbelspelenFeature.HOP.get(), SoundSource.PLAYERS, 0.8f, 0.8f + level.getRandom().nextFloat() * 0.4f);
        level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 3, 0.2, 0.02, 0.2, 0.01);
    }

    @Override
    public void einde(Wedstrijd w, Wedstrijd.Deelnemer d, @Nullable ServerPlayer p, ServerLevel level) {
        if (p != null) {
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private Zaklopen() {
    }
}
