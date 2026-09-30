package nl.juiced.guhs.feature.elftocht;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.quest.Highscores;

/**
 * The stempelkaart of your tour (lent by Schaatsmeester Guhglij): eleven boxes, one per village, filled with a stamp
 * and your split time as you go. Its data ({@code Stempels}: how many, {@code Tijden}: the split times) is kept up to
 * date by {@link ElftochtTocht}; right-click shows the card in the chat.
 */
public class StempelkaartItem extends Item {
    public StempelkaartItem(Properties properties) {
        super(properties);
    }

    /** Writes the stamps so far on the card. */
    public static void schrijf(ItemStack stack, int stempels, int[] tijden) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("Stempels", stempels);
            tag.putIntArray("Tijden", tijden.clone());
        });
    }

    public static int stempels(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("Stempels", 0);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            ElftochtTocht.toonKaart(sp);
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && (!(entity instanceof Player player) || !ElftochtTocht.opTocht(player))) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.discard();
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int n = tag.getIntOr("Stempels", 0);
        int[] tijden = tag.getIntArray("Tijden").orElse(new int[0]);
        tooltip.add(Component.translatable("item.guhs.stempelkaart.lore", n).withStyle(ChatFormatting.GRAY));
        for (int k = 0; k < ElftochtTocht.VOLGORDE.length; k++) {
            int dorp = ElftochtTocht.VOLGORDE[k];
            boolean klaar = k < n;
            tooltip.add(Component.literal(klaar ? " ✔ " : " ○ ").append(ElftochtTocht.dorpNaam(dorp))
                    .append(klaar && k < tijden.length ? Component.literal("  " + Highscores.tijd(tijden[k])) : Component.empty())
                    .withStyle(klaar ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }
}
