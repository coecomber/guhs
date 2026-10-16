package nl.juiced.guhs.feature.einde2.trap5;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * Small shared bits of the tier-5 machines: the block item with its lore and what it asks or gives (keys
 * {@code block.guhs.<id>.lore}, {@code gui.guhs.einde2_trap5_machines.vraagt} / {@code .geeft}), and the owner of a block
 * as a player who is online.
 */
public final class T5Machine {
    private T5Machine() {
    }

    /** The item of a tier-5 machine: its lore line and, in pink, its VK (positive = asks, negative = gives, 0 = nothing). */
    public static class Item extends BlockItem {
        private final int vk;

        public Item(Block block, Properties properties, int vk) {
            super(block, properties);
            this.vk = vk;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
            if (vk > 0) {
                tooltip.accept(Component.translatable("gui.guhs.einde2_trap5_machines.vraagt", vk).withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (vk < 0) {
                tooltip.accept(Component.translatable("gui.guhs.einde2_trap5_machines.geeft", -vk).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** The owner as an online player in this world's server (null: offline, unknown, or no owner). */
    @Nullable
    public static ServerPlayer speler(@Nullable ServerLevel level, @Nullable UUID eigenaar) {
        return level == null || eigenaar == null ? null : level.getServer().getPlayerList().getPlayer(eigenaar);
    }
}
