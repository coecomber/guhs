package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;

/**
 * Toph-guh's lesson (DESIGN 5 h3: "Zij leert je geblinddoekt aardesturen: het scherm wordt donker en je ziet via
 * trillingen"). Once Toph told you to shut your eyes (the flag {@link E2H3Aarde#V_BAAN}) the walled course next to the
 * arena ({@code blinddoek_baan}) goes dark for you (vanilla's darkness; only while you are in it) and you "see" with your
 * feet: every few seconds, and every time you stamp (crouch), a ring of light runs over the floor from where you stand, and
 * what it touches flickers up for you alone: the six aardemunt stones of the course ({@code tril_doel_1..6}) brighter the
 * nearer they are, the walls, whoever moves. Stand on each stone once: it hums and stays lit for you. All six: the flag
 * {@code geleerd} of the line (earth bending learnt, CONTRACT_E2_B 2.2) and Toph's last word ({@link H3Rollen}).
 * <p>
 * The same tremor is the chapter's story power {@code einde2_h3_aarde_tril} ({@link #KRACHT}: zij's key and wheel, only
 * inside Ba Sing Guh).
 */
public final class H3Blinddoek {
    public static final int DOELEN = 6;
    /** Ticks between two tremors by themselves; a stamp (crouching) gives one at once, at most every 10 ticks. */
    public static final int RITME = 50, STAMP_RUST = 10;
    /** How far a tremor reaches (blocks) and how near you must stand on a stone to find it. */
    public static final double BEREIK = 14, OP_STEEN = 1.3;
    public static final String KRACHT = "einde2_h3_aarde_tril";

    private static final DustParticleOptions LICHT = new DustParticleOptions(0xF2F2D0, 1.4f);
    private static final DustParticleOptions GEVONDEN = new DustParticleOptions(0x7CE07C, 1.6f);

    private record Stand(int laatste, boolean gebukt) {
    }

    private static final Map<UUID, Stand> STAND = new ConcurrentHashMap<>();

    private H3Blinddoek() {
    }

    /** The stones this player found (bits 0..5, the counter "tril" of the line). */
    static int gevonden(ServerPlayer p) {
        return E2H3Aarde.LIJN.teller(p, "tril");
    }

    static void tik(ServerPlayer p, E2H3Aarde.Stad stad) {
        if (!E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_BAAN) || !stad.in(p, "blinddoek_baan", 0.5)) {
            stop(p);
            return;
        }
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
        if (!STAND.containsKey(p.getUUID())) {
            // (fix2-h3) stepping in: what to do, in one line
            H3Hints.titel(p, Component.translatable("gui.guhs.einde2_h3_aarde.baan.titel").withStyle(ChatFormatting.GREEN),
                    Component.translatable("gui.guhs.einde2_h3_aarde.baan.onder", Integer.bitCount(gevonden(p)), DOELEN).withStyle(ChatFormatting.YELLOW), 60);
        }
        Stand s = STAND.getOrDefault(p.getUUID(), new Stand(-RITME, false));
        boolean stamp = p.isShiftKeyDown() && !s.gebukt() && p.tickCount - s.laatste() >= STAMP_RUST;
        if (stamp || p.tickCount - s.laatste() >= RITME) {
            tril(p, stad, stamp);
            s = new Stand(p.tickCount, p.isShiftKeyDown());
        } else {
            s = new Stand(s.laatste(), p.isShiftKeyDown());
        }
        STAND.put(p.getUUID(), s);
        int bits = gevonden(p);
        for (int i = 1; i <= DOELEN; i++) {
            if ((bits & (1 << (i - 1))) == 0 && stad.bij(p, "tril_doel_" + i, OP_STEEN)) {
                vind(p, stad, i);
            }
        }
    }

    /** A tremor from where the player stands: the ring over the floor, what it touches, the stones. */
    static void tril(ServerPlayer p, E2H3Aarde.Stad stad, boolean stamp) {
        Vec3 voet = p.position();
        Schijn.geluid(p, SoundEvents.ROOTED_DIRT_STEP, p.blockPosition(), stamp ? 1f : 0.4f, 0.5f);
        for (int r = 2; r <= BEREIK; r += 3) {
            for (int a = 0; a < 360; a += Math.max(10, 90 / r)) {
                double x = voet.x + r * Math.cos(Math.toRadians(a)), z = voet.z + r * Math.sin(Math.toRadians(a));
                Schijn.deeltjes(p, LICHT, new Vec3(x, voet.y + 0.1, z), 1, 0.02, 0);
            }
        }
        int bits = gevonden(p);
        for (int i = 1; i <= DOELEN; i++) {
            Vec3 steen = stad.voet("tril_doel_" + i);
            if (steen == null) {
                continue;
            }
            double d = steen.distanceTo(voet);
            boolean al = (bits & (1 << (i - 1))) != 0;
            if (al) {
                Schijn.deeltjes(p, GEVONDEN, steen.add(0, 0.2, 0), 6, 0.25, 0);
            } else if (d <= BEREIK) {
                int n = (int) Math.max(3, 24 - d * 1.5);
                Schijn.deeltjes(p, ParticleTypes.END_ROD, steen.add(0, 0.15, 0), n, 0.35, 0.01);
            }
        }
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(BEREIK, 3, BEREIK), e -> e != p)) {
            Schijn.deeltjes(p, ParticleTypes.END_ROD, e.position().add(0, 0.1, 0), 5, 0.3, 0);
        }
    }

    /** Stone i found: it hums, and the last one teaches you earth bending. */
    static void vind(ServerPlayer p, E2H3Aarde.Stad stad, int i) {
        int bits = gevonden(p) | (1 << (i - 1));
        E2H3Aarde.LIJN.teller(p, "tril", bits);
        int n = Integer.bitCount(bits);
        Vec3 steen = stad.voet("tril_doel_" + i);
        if (steen != null) {
            Schijn.deeltjes(p, GEVONDEN, steen.add(0, 0.3, 0), 20, 0.4, 0);
            Schijn.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, BlockPos.containing(steen), 1f, 0.6f + n * 0.1f);
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_h3_aarde.tril.gevonden", n, DOELEN).withStyle(ChatFormatting.GREEN));
        if (n >= DOELEN) {
            geleerd(p);
        }
    }

    /** All six: earth bending learnt (the flag of the line), the darkness lifts, Toph says her word. */
    static void geleerd(ServerPlayer p) {
        E2H3Aarde.LIJN.vlag(p, E2Verhaal.V_GELEERD, true);
        stop(p);
        p.removeEffect(MobEffects.DARKNESS);
        H3Hints.gelukt(p, "baan.klaar", Component.translatable("gui.guhs.einde2_h3_aarde.baan.klaar_onder").withStyle(ChatFormatting.YELLOW),
                SoundEvents.PLAYER_LEVELUP);
        H3Rollen.tophGeleerd(p);
    }

    /** The lesson is over or paused for this player (left the course, another step): no darkness of ours lingers. */
    static void stop(ServerPlayer p) {
        if (STAND.remove(p.getUUID()) != null) {
            p.removeEffect(MobEffects.DARKNESS);
        }
    }
}
