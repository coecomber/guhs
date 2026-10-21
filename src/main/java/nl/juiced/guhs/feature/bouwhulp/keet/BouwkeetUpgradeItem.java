package nl.juiced.guhs.feature.bouwhulp.keet;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
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

/**
 * An upgrade of the Bouwkeet (design 2.3, the item names and recipes are an own choice, BOUWHULP_KEUZES.md): used on a keet
 * it raises the keet ONE level, to its {@link #naar}, and is used up. Each one is crafted in its tech tier: the
 * Guhrad-tandwieltje (tier 1, Knutselen), the Zoutkristal-waterpas (tier 2, a zoutkristal), the Saus-kraanhaakje (tier 3,
 * the Uitvinder-guh's machine card, which stays) and the Gloei-bouwlamp (tier 4, a gloeister).
 */
public class BouwkeetUpgradeItem extends Item {
    private final BouwNiveau naar;

    public BouwkeetUpgradeItem(Properties properties, BouwNiveau naar) {
        super(properties);
        this.naar = naar;
    }

    public BouwNiveau naar() {
        return naar;
    }

    void gebruik(ItemStack stack, BouwkeetBlockEntity keet, ServerPlayer p) {
        if (!BouwkeetBlock.magBewerken(keet, p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwkeet.niet_van_jou"));
            return;
        }
        BouwNiveau nu = keet.niveau();
        if (nu.compareTo(naar) >= 0) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwkeet.upgrade.al", Component.translatable(naar.sleutel())));
            return;
        }
        if (!keet.upgrade(naar)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwkeet.upgrade.eerst", Component.translatable(BouwNiveau.van(nu.nummer() + 1).sleutel())));
            return;
        }
        stack.consume(1, p);
        if (keet.getLevel() instanceof ServerLevel level) {
            var pos = keet.getBlockPos();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, 30, 1.2, 1, 1.2, 0.1);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.5f);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7f, 1.2f);
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.bouwkeet.upgrade.klaar", Component.translatable(naar.sleutel())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.bouwkeet_upgrade.naar", Component.translatable(naar.sleutel()), naar.vk)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
