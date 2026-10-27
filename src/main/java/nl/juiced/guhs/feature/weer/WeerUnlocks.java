package nl.juiced.guhs.feature.weer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.kleding.KledingFeature;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.8.0: the guh-weertjes a player has unlocked (with a {@link WeerPotjeItem}): saved per player in
 * {@code GuhQuests.saved(player)["guhs_weertjes"]} (a list of weertje ids; survives death), the same way as the clothes
 * (KledingUnlocks). The client gets them with the Weer vadser's menu ({@link WeerPayloads.Open}). The zon, regen and
 * onweer are always there.
 * <p>
 * The one-time sources (a minigame win, a constellation...) give the potje once per player and weertje
 * ({@link #potjeEenmalig}: {@code "guhs_weerpotjes_gekregen"}).
 */
public final class WeerUnlocks {
    public static final String KEY = "guhs_weertjes", GEKREGEN = "guhs_weerpotjes_gekregen";

    private WeerUnlocks() {
    }

    public static boolean heeft(Player player, Weertje w) {
        return w.altijd || ids(player, KEY).contains(w.id());
    }

    /** Every weather this player may choose (the always-there ones too). */
    public static Set<Weertje> alle(Player player) {
        Set<Weertje> out = EnumSet.noneOf(Weertje.class);
        for (Weertje w : Weertje.values()) {
            if (heeft(player, w)) {
                out.add(w);
            }
        }
        return out;
    }

    /** Unlocks it (with its little party); false when it was already there (then nothing is used up). */
    public static boolean ontgrendel(ServerPlayer player, Weertje w) {
        if (heeft(player, w)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.weer.al_gehad", Component.translatable(w.key()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.6f);
            return false;
        }
        voegToe(player, w);
        ServerLevel level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1f, 1.4f);
        level.playSound(null, player.blockPosition(), KledingFeature.ONTGRENDEL_SOUND.get(), SoundSource.PLAYERS, 1f, 1.1f);
        level.sendParticles(KledingFeature.CONFETTI.get(), player.getX(), player.getY() + 1.6, player.getZ(), 40, 0.5, 0.3, 0.5, 0.15);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.6, 0.6, 0.6, 0.0);
        player.sendSystemMessage(Component.translatable("gui.guhs.weer.ontgrendeld",
                Component.translatable(w.key()).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** (Tests / commands) adds it without a party. */
    public static void voegToe(Player player, Weertje w) {
        if (w.altijd || ids(player, KEY).contains(w.id())) {
            return;
        }
        ListTag list = GuhQuests.saved(player).getListOrEmpty(KEY);
        list.add(StringTag.valueOf(w.id()));
        GuhQuests.saved(player).put(KEY, list);
    }

    /** (Commands / tests) forgets every unlocked weertje and every one-time potje. */
    public static void wis(Player player) {
        GuhQuests.saved(player).remove(KEY);
        GuhQuests.saved(player).remove(GEKREGEN);
    }

    /**
     * A one-time source (a won minigame, a constellation...): the weerpotje of this weertje, but only the first time for this
     * player (and not when it is already unlocked). True when it was given.
     */
    public static boolean potjeEenmalig(ServerPlayer player, Weertje w) {
        if (heeft(player, w) || ids(player, GEKREGEN).contains(w.id())) {
            return false;
        }
        ListTag list = GuhQuests.saved(player).getListOrEmpty(GEKREGEN);
        list.add(StringTag.valueOf(w.id()));
        GuhQuests.saved(player).put(GEKREGEN, list);
        Minigames.give(player, new ItemStack(WeerFeature.potje(w)));
        player.sendSystemMessage(Component.translatable("gui.guhs.weer.potje_gekregen", Component.translatable(w.key()).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** The ids (for the menu payload). */
    public static List<String> ontgrendeldeIds(Player player) {
        return new ArrayList<>(ids(player, KEY));
    }

    private static List<String> ids(Player player, String key) {
        ListTag list = GuhQuests.saved(player).getListOrEmpty(key);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getStringOr(i, ""));
        }
        return out;
    }
}
