package nl.juiced.guhs.feature.kleding;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Dressing a guh (2.9, server side): only its owner, and only with pieces that owner unlocked. A guh keeps what it wears
 * when it changes owner (it's just data on the guh), but the new owner can only switch it to their own unlocks. A
 * backpack only comes off when it's empty (its 18 slots belong to that guh).
 */
public final class KledingKast {
    /** Why a change was refused (or OK). */
    public enum Uitkomst { OK, NIET_JOUW_GUH, NIET_ONTGRENDELD, RUGZAK_NIET_LEEG, VERKEERD_VAKJE }

    /** Can this player put {@code stuk} (null = nothing) in {@code slot} on this guh? */
    public static Uitkomst mag(GuhEntity guh, Player player, GuhClothes.Slot slot, @Nullable GuhClothes stuk) {
        if (!guh.isTame() || !player.getUUID().equals(guh.getOwnerUUID())) {
            return Uitkomst.NIET_JOUW_GUH;
        }
        if (slot == GuhClothes.Slot.HAAR || (stuk != null && stuk.slot != slot)) {
            return Uitkomst.VERKEERD_VAKJE;
        }
        GuhClothes nu = guh.getClothes(slot);
        if (stuk == nu) {
            return Uitkomst.OK;
        }
        if (stuk != null && !KledingUnlocks.heeft(player, stuk)) {
            return Uitkomst.NIET_ONTGRENDELD;
        }
        if (nu == GuhClothes.GUH_BACKPACK && !guh.backpackIsEmpty()) {
            return Uitkomst.RUGZAK_NIET_LEEG;
        }
        return Uitkomst.OK;
    }

    /** Puts one unlocked piece on (right-clicking your guh with a piece you already have). */
    public static boolean trekAan(GuhEntity guh, Player player, GuhClothes stuk) {
        Uitkomst u = mag(guh, player, stuk.slot, stuk);
        if (u != Uitkomst.OK) {
            if (player instanceof ServerPlayer sp) {
                meld(sp, u);
            }
            return false;
        }
        if (guh.getClothes(stuk.slot) != stuk) {
            guh.wear(stuk);
            blij(guh);
            nl.juiced.guhs.feature.band.Band.moment(guh, player instanceof ServerPlayer sp ? sp : null, nl.juiced.guhs.feature.band.Moment.KLEDING, stuk.id());   // 2.10
        }
        return true;
    }

    /**
     * The wardrobe's "Aantrekken!": the whole outfit at once, one piece (or null) per wardrobe slot in
     * {@code GuhClothes.Slot.kleding()} order. Slots that aren't allowed stay as they were (with a message). Returns how
     * many slots changed.
     */
    public static int kleed(ServerPlayer player, GuhEntity guh, List<GuhClothes> outfit) {
        List<GuhClothes.Slot> slots = GuhClothes.Slot.kleding();
        int veranderd = 0;
        Uitkomst eerste = Uitkomst.OK;
        for (int i = 0; i < slots.size() && i < outfit.size(); i++) {
            GuhClothes.Slot slot = slots.get(i);
            GuhClothes stuk = outfit.get(i);
            if (guh.getClothes(slot) == stuk) {
                continue;
            }
            Uitkomst u = mag(guh, player, slot, stuk);
            if (u != Uitkomst.OK) {
                if (eerste == Uitkomst.OK) {
                    eerste = u;
                }
                continue;
            }
            if (stuk == null) {
                guh.takeOff(slot);
            } else {
                guh.wear(stuk);
                nl.juiced.guhs.feature.band.Band.moment(guh, player, nl.juiced.guhs.feature.band.Moment.KLEDING, stuk.id());   // 2.10
            }
            veranderd++;
        }
        if (eerste != Uitkomst.OK) {
            meld(player, eerste);
        }
        if (veranderd > 0) {
            blij(guh);
            player.displayClientMessage(Component.translatable("gui.guhs.kleding.aangekleed", guh.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return veranderd;
    }

    private static void blij(GuhEntity guh) {
        guh.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 1.2f);
        guh.triggerAnim("action", "happy");
        if (guh.level() instanceof ServerLevel level) {
            level.sendParticles(KledingFeature.CONFETTI.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.8, guh.getZ(),
                    10, guh.getBbWidth() * 0.4, 0.2, guh.getBbWidth() * 0.4, 0.08);
        }
    }

    public static void meld(ServerPlayer player, Uitkomst u) {
        if (u == Uitkomst.OK) {
            return;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.kleding.nee." + u.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.RED), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    private KledingKast() {
    }
}
