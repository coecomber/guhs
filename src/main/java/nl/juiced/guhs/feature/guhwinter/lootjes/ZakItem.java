package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * De Zak van Sinterklaas: what you get on pakjesavond. It holds your Surprise and (when your giver made one) their drawing,
 * as the vanilla {@code minecraft:container} component; the lore carries the Sint's little note. Right-click: everything
 * goes into your bag; what doesn't fit stays in the sack (nothing ever lands on the ground). An empty sack is gone.
 */
public class ZakItem extends Item {
    public ZakItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack zak = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer p)) {
            return InteractionResult.SUCCESS;
        }
        ItemContainerContents c = zak.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> rest = new ArrayList<>();
        int uit = 0;
        for (ItemStack s : c.nonEmptyItemCopyStream().toList()) {
            int voor = s.getCount();
            if (rest.isEmpty() && p.getInventory().add(s) && s.isEmpty()) {
                uit++;
                continue;
            }
            if (s.getCount() < voor) {
                uit++;
            }
            if (!s.isEmpty()) {
                rest.add(s);
            }
        }
        if (rest.isEmpty()) {
            zak.shrink(1);
            p.sendSystemMessage(Component.translatable("guhwinter.lootjes.zak.open").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            zak.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(rest));
            p.sendOverlayMessage(Component.translatable("guhwinter.lootjes.zak.vol").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (uit > 0) {
            level.playSound(null, p.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        p.inventoryMenu.broadcastChanges();
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.guhs.guhwinter_zak.lore").withStyle(ChatFormatting.GRAY));
    }
}
