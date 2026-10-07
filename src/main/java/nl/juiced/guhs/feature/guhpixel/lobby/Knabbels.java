package nl.juiced.guhs.feature.guhpixel.lobby;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.Toegang;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The ten hidden golden knabbels of the lobby. Per player: each knabbel pays {@link #MUNTJES} muntjes once (the kern's
 * once-keys {@code lobby:knabbel_0..9}, which are also the "found" flags); the blocks stay where they are for everybody.
 * All ten: the hidden advancement quest/lobby_knabbels and the title Knabbelspeurder.
 */
public final class Knabbels {
    public static final int AANTAL = 10, MUNTJES = 10;

    static String sleutel(int nummer) {
        return "lobby:knabbel_" + nummer;
    }

    public static boolean heeft(ServerPlayer p, int nummer) {
        return Muntjes.isVerdiend(p, sleutel(nummer));
    }

    public static int gevonden(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < AANTAL; i++) {
            if (heeft(p, i)) {
                n++;
            }
        }
        return n;
    }

    public static boolean alleGevonden(ServerPlayer p) {
        return gevonden(p) >= AANTAL;
    }

    /** The player clicked knabbel {@code nummer} at pos. True when it is new for them. */
    public static boolean pak(ServerPlayer p, BlockPos pos, int nummer) {
        if (nummer < 0 || nummer >= AANTAL) {
            return false;
        }
        if (!Toegang.heeft(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.lobby.knabbel.op_slot").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (heeft(p, nummer)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.lobby.knabbel.al", gevonden(p), AANTAL).withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (!Muntjes.verdienEens(p, sleutel(nummer), MUNTJES)) {
            return false;
        }
        int nu = gevonden(p);
        p.sendSystemMessage(Component.translatable("gui.guhs.lobby.knabbel.gevonden", nu, AANTAL).withStyle(ChatFormatting.GOLD));
        PxGeluid.speel(p, LobbySlice.KNABBEL_GELUID.get(), SoundSource.PLAYERS, 0.9f, 0.9f + 0.06f * nu);
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0.6);
        }
        if (nu >= AANTAL) {
            GuhAdvancements.grant(p, "lobby_knabbels");
            PxGeluid.titel(p, Component.translatable("gui.guhs.lobby.knabbel.alle.titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.lobby.knabbel.alle.onder").withStyle(ChatFormatting.YELLOW), 60);
            p.sendSystemMessage(Component.translatable("gui.guhs.lobby.knabbel.alle.chat").withStyle(ChatFormatting.GOLD));
        }
        LobbyBorden.meteen(p);
        return true;
    }

    private Knabbels() {
    }
}
