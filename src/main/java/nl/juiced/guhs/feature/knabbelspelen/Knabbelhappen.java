package nl.juiced.guhs.feature.knabbelspelen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Knabbelhappen: two kaasknabbels per lane swing on strings from the beam, towards you and away again. Stand on your
 * mat and bite (hit or right-click) them when they swing close: a normal one is 1 point, a golden one 3. A bitten
 * knabbel comes back at once on a new string (a bit faster as the time runs out). The guh gets nice and VAHOEG.
 */
public final class Knabbelhappen implements Wedstrijd.Spel {
    public static final Knabbelhappen SPEL = new Knabbelhappen();
    /** How far from your eyes you can bite. */
    public static final double BIJT = 3.3;

    static final class Staat {
        int punten, goud;
        final List<SpelDing> dingen = new ArrayList<>();
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
        return Speelvelden.HAP_MAT;
    }

    @Override
    public void klaarzetten(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        Staat s = staat(d);
        w.naarStart(d, p, level, Speelvelden.HAP_MAT);
        for (int i = 0; i < 2; i++) {
            SpelDing ding = KnabbelspelenFeature.DING.get().create(level, EntitySpawnReason.TRIGGERED);
            if (ding == null) {
                continue;
            }
            ding.soort(SpelDing.HANGKNABBEL);
            ding.spel = w.npcId;
            ding.baan = d.baan;
            Vec3 touw = Speelvelden.punt(w.anker, Onderdeel.KNABBELHAPPEN, d.baan, Speelvelden.HAP_BALK + i, i == 0 ? -0.7 : 0.7, Speelvelden.HAP_Y);
            ding.touw(touw);
            ding.as = w.anker.vector(0, Speelvelden.veld(Onderdeel.KNABBELHAPPEN).richting()).normalize();
            nieuw(ding, level.getRandom(), 0f, i == 1);
            Vec3 at = ding.slinger(0);
            ding.snapTo(at.x, at.y, at.z, 0, 0);
            level.addFreshEntity(ding);
            s.dingen.add(ding);
        }
    }

    /** A fresh knabbel on the string: a new length, swing and speed (faster later on), sometimes a golden one. */
    static void nieuw(SpelDing ding, RandomSource rng, float voortgang, boolean tweede) {
        ding.lengte = 3.3 + rng.nextDouble() * 1.0;
        ding.amp = 0.45 + rng.nextDouble() * 0.3;
        ding.omega = (0.075 + rng.nextDouble() * 0.05) * (1 + voortgang * 0.6);
        ding.fase = tweede ? Math.PI + rng.nextDouble() : rng.nextDouble() * Math.PI * 2;
        ding.leeftijd = 0;
        ding.goud(rng.nextFloat() < 0.15f);
    }

    @Override
    public void aftellen(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        opMat(w, d, p, level);
    }

    @Override
    public void tick(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level, int t) {
        opMat(w, d, p, level);
        Staat s = staat(d);
        if (t % 5 == 0) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.hap.bar", s.punten,
                    (Onderdeel.KNABBELHAPPEN.maxTicks - t + 19) / 20).withStyle(ChatFormatting.AQUA));
        }
    }

    /** You stay on your mat (the knabbels come to you, not the other way round). */
    private static void opMat(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        double[] b = Speelvelden.baan(w.anker, Onderdeel.KNABBELHAPPEN, d.baan, p.position());
        if (Math.abs(b[0] - Speelvelden.HAP_MAT) > 1.3 || Math.abs(b[1]) > 1.6) {
            Vec3 m = Speelvelden.punt(w.anker, Onderdeel.KNABBELHAPPEN, d.baan, Speelvelden.HAP_MAT, 0, Speelvelden.G + 1);
            Wedstrijd.teleport(p, level, m.x, m.y, m.z, p.getYRot());
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.hap.mat").withStyle(ChatFormatting.GOLD));
        }
    }

    /** A bite (from SpelDing): close enough, your own lane, while playing -> points and a new knabbel. */
    static void hap(ServerPlayer p, SpelDing ding) {
        if (!Wedstrijd.speelt(p, Onderdeel.KNABBELHAPPEN)) {
            return;
        }
        Wedstrijd w = Wedstrijd.van(p);
        Wedstrijd.Deelnemer d = w == null ? null : w.deelnemer(p);
        if (d == null || !w.npcId.equals(ding.spel) || ding.baan != d.baan || ding.leeftijd < 4) {
            return;
        }
        if (p.getEyePosition().distanceTo(ding.position()) > BIJT) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.hap.te_ver").withStyle(ChatFormatting.GRAY));
            return;
        }
        hapRaak(w, d, p, ding);
    }

    /** (Also for the tests) the bite counts. */
    static void hapRaak(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, SpelDing ding) {
        Staat s = staat(d);
        ServerLevel level = p.level();
        boolean goud = ding.goud();
        s.punten += goud ? 3 : 1;
        if (goud) {
            s.goud++;
        }
        level.playSound(null, p.blockPosition(), KnabbelspelenFeature.HAP.get(), SoundSource.PLAYERS, 1f, goud ? 1.4f : 1f + level.getRandom().nextFloat() * 0.2f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(ding.stack())), ding.getX(), ding.getY() + 0.2, ding.getZ(), 10, 0.15, 0.15, 0.15, 0.08);
        if (goud) {
            level.sendParticles(ParticleTypes.WAX_ON, ding.getX(), ding.getY() + 0.2, ding.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.hap.goud", s.punten).withStyle(ChatFormatting.GOLD));
        }
        nieuw(ding, level.getRandom(), Math.min(1f, w.ticks / (float) Onderdeel.KNABBELHAPPEN.maxTicks), level.getRandom().nextBoolean());
    }

    @Override
    public int eindScore(Wedstrijd.Deelnemer d) {
        return staat(d).punten;
    }

    @Override
    public void einde(Wedstrijd w, Wedstrijd.Deelnemer d, @Nullable ServerPlayer p, ServerLevel level) {
        Staat s = staat(d);
        for (SpelDing ding : s.dingen) {
            ding.discard();
        }
        s.dingen.clear();
    }

    private Knabbelhappen() {
    }
}
