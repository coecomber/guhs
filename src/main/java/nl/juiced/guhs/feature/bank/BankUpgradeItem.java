package nl.juiced.guhs.feature.bank;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.storage.BankStorage;

/**
 * The Bodemloos Knabbelmaagje ({@code guhs:bank_upgrade}): the one upgrade of the Bank Guh. Used on a placed bank it takes away
 * the cap of {@link BankStorage#CAP} per kind of item, for good: the upgrade stays with that bank, also when the bank is
 * picked up and put down again. One item per bank; a bank that has it already does not eat a second one. The uitvinder-guh
 * of the Oude Guhrad-centrale gives it (tech-quests); there is no recipe.
 */
public class BankUpgradeItem extends Item {
    public BankUpgradeItem(Properties properties) {
        super(properties);
    }

    /** A player clicks a placed Bank Guh with the upgrade (server side). Returns whether the bank took it. */
    public static boolean opBank(ItemStack upgrade, BankGuhBlockEntity bank, ServerPlayer player) {
        if (bank.isUpgraded()) {
            player.sendOverlayMessage(Component.translatable("item.guhs.bank_upgrade.al"));
            return false;
        }
        bank.getStorage().setUpgraded(true);
        if (!player.getAbilities().instabuild) {
            upgrade.shrink(1);
        }
        BlockPos p = bank.getBlockPos();
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, p.getX() + 0.5, p.getY() + 1.3, p.getZ() + 0.5, 8, 0.4, 0.3, 0.4, 0.0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5, 14, 0.5, 0.5, 0.5, 0.0);
            level.playSound(null, p, ModSounds.GUH_AMBIENT.get(), SoundSource.BLOCKS, 0.9f, 0.7f);
            level.playSound(null, p, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5f, 1.4f);
        }
        player.sendOverlayMessage(Component.translatable("item.guhs.bank_upgrade.gelukt"));
        BankFeature.opgevoerd(player);
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.bank_upgrade.lore", BankStorage.CAP).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.bank_upgrade.lore.blijft").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
