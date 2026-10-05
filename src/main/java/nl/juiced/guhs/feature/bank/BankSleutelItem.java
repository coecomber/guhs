package nl.juiced.guhs.feature.bank;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.band.GuhVolger;

/**
 * The Banksleutel ({@code guhs:bank_sleutel}): the link key of the Hapluikje. Click a placed Bank Guh with it and the key
 * remembers that bank (its id, {@link BankFeature#BANK_ID}); click a Hapluikje with a key that remembers a bank and the
 * luikje feeds that bank from then on. The key is never used up: one key links as many luikjes as you like, and clicking
 * another bank makes it remember that one instead.
 */
public class BankSleutelItem extends Item {
    public BankSleutelItem(Properties properties) {
        super(properties);
    }

    /** The bank this key remembers, or null: a blank key. */
    @Nullable
    public static UUID bank(ItemStack sleutel) {
        return sleutel.get(BankFeature.BANK_ID.get());
    }

    /** Makes the key remember this bank (and where it stands now, for the tooltip). */
    public static void onthoud(ItemStack sleutel, BankGuhBlockEntity bank) {
        bank.meld();
        sleutel.set(BankFeature.BANK_ID.get(), bank.bankId());
        if (bank.getLevel() != null) {
            sleutel.set(BankFeature.BANK_PLEK.get(), GlobalPos.of(bank.getLevel().dimension(), bank.getBlockPos()));
        }
    }

    /** A player clicks a placed Bank Guh with the key (server side). */
    public static void opBank(ItemStack sleutel, BankGuhBlockEntity bank, ServerPlayer player) {
        onthoud(sleutel, bank);
        BlockPos p = bank.getBlockPos();
        player.sendOverlayMessage(Component.translatable("item.guhs.bank_sleutel.onthouden", p.getX(), p.getY(), p.getZ()));
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, p, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.6f);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return bank(stack) != null || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.bank_sleutel.lore").withStyle(ChatFormatting.GRAY));
        if (bank(stack) == null) {
            tooltip.accept(Component.translatable("item.guhs.bank_sleutel.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        GlobalPos plek = stack.get(BankFeature.BANK_PLEK.get());
        if (plek == null) {
            tooltip.accept(Component.translatable("item.guhs.bank_sleutel.gebonden").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.accept(Component.translatable("item.guhs.bank_sleutel.gebonden.plek", plek.pos().getX(), plek.pos().getY(), plek.pos().getZ(),
                    GuhVolger.dimensie(plek.dimension())).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
