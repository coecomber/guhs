package nl.juiced.guhs.feature.blub;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.kleding.KledingFeature;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.8.0 extension: the Blub-kleding a player has unlocked (with a {@link BlubKledingItem}): saved per player in
 * {@code GuhQuests.saved(player)["guhs_blubkleding"]} (a list of piece ids; survives death), the same way as the weertjes
 * and the guh clothes. The client gets them with the Blub's menu ({@link BlubMenu.MenuOpen}).
 */
public final class BlubKledingUnlocks {
    public static final String KEY = "guhs_blubkleding";

    private BlubKledingUnlocks() {
    }

    public static boolean heeft(Player player, String id) {
        return ids(player).contains(id);
    }

    /** Unlocks it (with its little party); false when it was already there (then nothing is used up). */
    public static boolean ontgrendel(ServerPlayer player, BlubKleding.Stuk stuk) {
        if (heeft(player, stuk.id())) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.kleding.al_gehad", stuk.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.6f);
            return false;
        }
        voegToe(player, stuk.id());
        ServerLevel level = player.level();
        level.playSound(null, player.blockPosition(), KledingFeature.ONTGRENDEL_SOUND.get(), SoundSource.PLAYERS, 1f, 1.2f);
        level.sendParticles(KledingFeature.CONFETTI.get(), player.getX(), player.getY() + 1.6, player.getZ(), 30, 0.5, 0.3, 0.5, 0.15);
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.5, 0.4, 0.5, 0.0);
        player.sendSystemMessage(Component.translatable("gui.guhs.blubguh.kleding.ontgrendeld", stuk.naam().copy().withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** (Tests / commands) adds it without a party. */
    public static void voegToe(Player player, String id) {
        if (heeft(player, id)) {
            return;
        }
        ListTag list = GuhQuests.saved(player).getListOrEmpty(KEY);
        list.add(StringTag.valueOf(id));
        GuhQuests.saved(player).put(KEY, list);
    }

    /** (Commands / tests) forgets every unlocked piece. */
    public static void wis(Player player) {
        GuhQuests.saved(player).remove(KEY);
    }

    public static List<String> ids(Player player) {
        ListTag list = GuhQuests.saved(player).getListOrEmpty(KEY);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getStringOr(i, ""));
        }
        return out;
    }
}
