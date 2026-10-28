package nl.juiced.guhs.feature.fabriek;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.storage.BankStorage;

/**
 * (1.8.0) A Fabriekshal machine that a Banksleutel links to a Bank Guh (any distance, any dimension): the
 * Bouwblokken-fabriek, the Kleurmolen and the Regenboogpers put what they make into it.
 */
public interface MetBank {
    @Nullable
    UUID bank();

    void koppelBank(@Nullable UUID bank);

    /** A Banksleutel on the machine: link the bank it remembers. */
    default void sleutel(ServerPlayer speler, ItemStack sleutel, Level level, net.minecraft.core.BlockPos pos) {
        UUID id = BankSleutelItem.bank(sleutel);
        if (id == null) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.fabriek.koppel.lege_sleutel"));
            return;
        }
        koppelBank(id);
        speler.sendOverlayMessage(Component.translatable("gui.guhs.fabriek.koppel.bank"));
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.6f);
    }

    /** The stomach of the linked bank (its chunk is loaded when needed), or null. */
    @Nullable
    static BankStorage maag(@Nullable Level level, @Nullable UUID bank) {
        if (bank == null || !(level instanceof ServerLevel server)) {
            return null;
        }
        BankGuhBlockEntity b = BankAdressen.zoek(server.getServer(), bank, true);
        return b == null ? null : b.getStorage();
    }

    /** Keeps the linked bank's chunk a little longer after a delivery. */
    static void wakker(@Nullable Level level, @Nullable UUID bank) {
        if (bank != null && level instanceof ServerLevel server) {
            BankGuhBlockEntity b = BankAdressen.zoek(server.getServer(), bank, false);
            if (b != null) {
                BankAdressen.houdWakker(b);
            }
        }
    }
}
