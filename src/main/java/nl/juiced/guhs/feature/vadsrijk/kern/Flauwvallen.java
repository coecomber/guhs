package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * Dying in the dimension = "flauwvallen" (ronde 4): you lose nothing, you wake up at your checkpoint (the last campfire you lit;
 * else the area's own respawn, else the Knuffelkamp), 30 s Duizelig (-25 % main stats). For 3 s after waking nothing can hurt you.
 * Bosses can pick the arena door instead with {@link #CHECKPOINT_KIEZER}.
 */
public final class Flauwvallen {
    public static final int DUIZELIG_TICKS = 600, BESCHERMD_TICKS = 60;
    /** First non-null answer wins (bazen: "you fainted in my arena: wake at the door"). */
    public static final List<Function<ServerPlayer, BlockPos>> CHECKPOINT_KIEZER = new CopyOnWriteArrayList<>();

    private Flauwvallen() {
    }

    public static boolean beschermd(ServerPlayer p) {
        return p.level().getGameTime() < Spelers.data(p).duizeligTot - DUIZELIG_TICKS + BESCHERMD_TICKS;
    }

    public static void flauw(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Haken.FLAUW.forEach(h -> h.accept(p));
        SpelerData d = Spelers.data(p);
        sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1f, 0.6f);
        Statussen.wisAlles(p);
        BlockPos waar = waarWakker(p);
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(waar), Vec3.ZERO, p.getYRot(), 0f, Relative.ROTATION, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        d.vads = Spelers.maxVads(p);
        d.energie = Spelers.maxEnergie(p);
        d.duizeligTot = sl.getGameTime() + DUIZELIG_TICKS;
        Statussen.geef(p, Status.DUIZELIG, DUIZELIG_TICKS, 0.25, null);
        RijkScenes.titel(p, Component.translatable("vadsrijk.kern.flauw.titel").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("vadsrijk.kern.flauw.onder"), 60, 0xFF7AC8);
        Spelers.sync(p);
    }

    public static BlockPos waarWakker(ServerPlayer p) {
        for (Function<ServerPlayer, BlockPos> f : CHECKPOINT_KIEZER) {
            BlockPos b = f.apply(p);
            if (b != null) {
                return b;
            }
        }
        BlockPos c = Spelers.data(p).checkpoint;
        if (c != null) {
            return c;
        }
        return standaard(p);
    }

    /** The area's respawn: the proeftuin camp for the test area, else the Knuffelkamp arrival. */
    public static BlockPos standaard(@Nullable ServerPlayer p) {
        if (p != null) {
            Gebieden.Gebied g = Gebieden.op(p.getX(), p.getZ());
            if (g != null && g.id().equals("proeftuin")) {
                return nl.juiced.guhs.feature.vadsrijk.kern.wereld.Ondergrond.KAMP;
            }
        }
        return Gebieden.AANKOMST;
    }
}
