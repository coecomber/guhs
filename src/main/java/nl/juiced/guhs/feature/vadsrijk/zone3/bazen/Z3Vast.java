package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen;

/**
 * "Held" players (zone 3): the Kruinroofvogel's talons, the Wortelwurger's root, Baron Bleu's Rijpingskamer. A held player can't
 * move (kept at a spot, which may move, e.g. under the bird) and takes a small mechanic tick. They get loose:
 * <ul>
 *   <li>solo with a click series: left-click (swing) or tap shift {@code nodig} times;</li>
 *   <li>with help: a teammate who stands next to them and sneaks, or who hits the boss enough ({@link #help}); </li>
 *   <li>or when the time runs out ({@code einde} runs: e.g. the bird drops you).</li>
 * </ul>
 * Never a one-shot: the tick is a few % per second.
 */
public final class Z3Vast {
    public record Greep(UUID baas, String soort, Supplier<Vec3> plek, int nodig, int[] klik, long tot, double tikFractie, @Nullable Runnable einde,
            boolean[] vorig, int[] help) {
    }

    private static final Map<UUID, Greep> VAST = new ConcurrentHashMap<>();

    private Z3Vast() {
    }

    public static boolean vast(ServerPlayer p) {
        return VAST.containsKey(p.getUUID());
    }

    @Nullable
    public static Greep greep(ServerPlayer p) {
        return VAST.get(p.getUUID());
    }

    /** Holds the player. {@code soort}: lang suffix (vadsrijk.z3bazen.vast.&lt;soort&gt;). */
    public static void pak(Z3Baas b, ServerPlayer p, String soort, Supplier<Vec3> plek, int nodig, int ticks, double tikFractie, @Nullable Runnable einde) {
        if (Flauwvallen.beschermd(p) || vast(p)) {
            return;
        }
        VAST.put(p.getUUID(), new Greep(b.getUUID(), soort, plek, nodig, new int[1], p.level().getGameTime() + ticks, tikFractie, einde, new boolean[2], new int[1]));
        p.sendSystemMessage(Component.translatable("vadsrijk.z3bazen.vast." + soort).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.stopRiding();
    }

    /** Lets go (no end action). */
    public static void los(ServerPlayer p) {
        Greep g = VAST.remove(p.getUUID());
        if (g != null) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.vast.los").withStyle(ChatFormatting.GREEN));
            geluid(p, SoundEvents.CHAIN_BREAK, 1f, 1.2f);
        }
    }

    /** A teammate's help (hits on the boss while you hang, or sneaking next to you): counts as clicks. */
    public static void help(ServerPlayer p, int n) {
        Greep g = VAST.get(p.getUUID());
        if (g != null) {
            g.help()[0] += n;
        }
    }

    /** Lets go of everyone this boss holds. */
    public static void losAlles(UUID baas, MinecraftServer server) {
        for (Iterator<Map.Entry<UUID, Greep>> it = VAST.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Greep> e = it.next();
            if (e.getValue().baas().equals(baas)) {
                it.remove();
            }
        }
    }

    /** A sound only this player hears. */
    public static void geluid(ServerPlayer p, net.minecraft.sounds.SoundEvent s, float volume, float pitch) {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s),
                SoundSource.PLAYERS, p.getX(), p.getY(), p.getZ(), volume, pitch, p.getRandom().nextLong()));
    }

    static void tick(MinecraftServer server) {
        if (VAST.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<UUID, Greep>> it = VAST.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Greep> e = it.next();
            Greep g = e.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive() || Flauwvallen.beschermd(p) || !(p.level() instanceof ServerLevel sl)
                    || !(sl.getEntity(g.baas()) instanceof Z3Baas b) || b.geplofd()) {
                it.remove();
                continue;
            }
            long nu = sl.getGameTime();
            // hold the player at the spot
            Vec3 v = g.plek().get();
            if (p.position().distanceToSqr(v) > 0.09) {
                p.teleportTo(sl, v.x, v.y, v.z, java.util.Set.of(), p.getYRot(), p.getXRot(), false);
            }
            p.setDeltaMovement(Vec3.ZERO);
            p.resetFallDistance();
            // clicks: a new swing or a new shift press counts
            boolean zwaai = p.swinging, shift = p.isShiftKeyDown();
            if (zwaai && !g.vorig()[0]) {
                g.klik()[0]++;
            }
            if (shift && !g.vorig()[1]) {
                g.klik()[0]++;
            }
            g.vorig()[0] = zwaai;
            g.vorig()[1] = shift;
            // teammates sneaking next to you help
            if (nu % 10 == 0) {
                for (ServerPlayer m : sl.players()) {
                    if (m != p && !vast(m) && m.isShiftKeyDown() && m.distanceToSqr(p) < 3.2 * 3.2) {
                        g.help()[0] += 2;
                    }
                }
            }
            int klik = g.klik()[0] + g.help()[0];
            if (nu % 20 == 0) {
                b.mech(p, g.tikFractie(), "vast_" + g.soort());
                sl.sendParticles(new DustParticleOptions(0x8A9AB0, 1.6f), p.getX(), p.getY() + 1, p.getZ(), 12, 0.4, 0.6, 0.4, 0);
            }
            if (nu % 5 == 0) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.vast.klik", Math.min(klik, g.nodig()), g.nodig()).withStyle(ChatFormatting.YELLOW));
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            }
            if (klik >= g.nodig()) {
                it.remove();
                p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.vast.los").withStyle(ChatFormatting.GREEN));
                sl.playSound(null, p.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1f, 1.2f);
                b.losgekomen(p, g.soort());
            } else if (nu >= g.tot()) {
                it.remove();
                if (g.einde() != null) {
                    g.einde().run();
                }
            }
        }
    }
}
