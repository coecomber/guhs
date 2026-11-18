package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Hidden pepernoten: little bags ({@link SpelBlokken.Zakje}) at plein's anchors {@code pepernoot_verstop_*} (a few spots on
 * the plaza while those don't exist). Every player may take each bag once ({@value #HANDJE} pepernoten); the bag stays for
 * the others. A bag you haven't found yet glitters, for you only, when you come near.
 */
public final class VerstopteNoten {
    public static final int HANDJE = 5;
    static final double ZIEN = 18;

    private VerstopteNoten() {
    }

    static String sleutel(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /** Where the bags are. */
    public static List<BlockPos> plekken(MinecraftServer server) {
        return SpelBrug.ankers(server, "pepernoot_verstop").stream().map(SpelBrug.Anker::blok).toList();
    }

    public static void pak(ServerPlayer p, BlockPos pos) {
        MinecraftServer server = p.level().getServer();
        if (!SpelBrug.open(server)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.voorbij").withStyle(ChatFormatting.GRAY));
            return;
        }
        SpellenData data = SpellenData.get(server);
        String k = sleutel(pos);
        if (data.gevonden(p.getUUID(), k)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhwinter_spellen.zakje.al").withStyle(ChatFormatting.GRAY));
            return;
        }
        data.vind(p.getUUID(), k);
        SpelBrug.geef(p, HANDJE);
        ServerLevel level = p.level();
        level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 1f, 1.1f);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
        int totaal = plekken(server).size();
        p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.zakje.gevonden", HANDJE,
                Math.min(totaal, data.aantalGevonden(p.getUUID())), totaal).withStyle(ChatFormatting.GOLD));
    }

    /** Twice a second: glitter at the bags each player hasn't found yet, sent to that player alone. */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        ServerLevel level = SpelBrug.plein(server);
        if (level.players().isEmpty()) {
            return;
        }
        List<BlockPos> plekken = plekken(server);
        SpellenData data = SpellenData.get(server);
        DustParticleOptions goud = new DustParticleOptions(0xFFD34E, 0.9f);
        for (ServerPlayer p : level.players()) {
            for (BlockPos pos : plekken) {
                if (p.blockPosition().distSqr(pos) > ZIEN * ZIEN || !level.getBlockState(pos).is(SpellenFeature.ZAKJE.get())
                        || data.gevonden(p.getUUID(), sleutel(pos))) {
                    continue;
                }
                level.sendParticles(p, goud, false, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.25, 0.3, 0.25, 0);
                if (server.getTickCount() % 40 == 0) {
                    level.sendParticles(p, ParticleTypes.WAX_OFF, false, false, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }
    }
}
