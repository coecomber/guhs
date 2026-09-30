package nl.juiced.guhs.feature.landdiertjes;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.Schouder;

/** The items of the landdiertjes: the picked-up pluiseekhoorntje (with the shoulder), Sjokkel's bessensapje, a plain lore item. */
public final class LanddierItems {
    private LanddierItems() {
    }

    /**
     * A picked-up pluiseekhoorntje ({@link PiepDierItem}: name and all). Use it on a block: it hops down there. Use it in the
     * air: it climbs onto your shoulder.
     */
    public static class EekhoorntjeItem extends PiepDierItem {
        public EekhoorntjeItem(Properties properties) {
            super(() -> LanddiertjesFeature.PLUISEEKHOORNTJE.get(), properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                return InteractionResultHolder.success(stack);
            }
            ServerPlayer sp = (ServerPlayer) player;
            if (Schouder.heeft(sp)) {
                sp.displayClientMessage(Component.translatable("gui.guhs.landdiertjes.schouder_vol").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            PluiseekhoorntjeEntity eekhoorn = PiepDierItem.naar(stack, level, LanddiertjesFeature.PLUISEEKHOORNTJE.get());
            if (eekhoorn == null) {
                return InteractionResultHolder.fail(stack);
            }
            if (!eekhoorn.isTame()) {
                eekhoorn.tame(sp);                               // (a creative item without data: a new one, yours)
            }
            PluiseekhoorntjeEntity.opSchouder(sp, eekhoorn);
            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }
    }

    /**
     * Sjokkel's bessensapje: sweet berries, patiently turned into juice inside Sjokkel's shell (like the real one does). A
     * drink: a little food, a moment of Regeneratie and a happy feeling. Njeg!
     */
    public static class BessensapjeItem extends Item {
        public BessensapjeItem(Properties properties) {
            super(properties);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) {
            return 32;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack rest = super.finishUsingItem(stack, level, entity);  // (food: nutrition + the Regeneratie effect)
            if (entity instanceof ServerPlayer sp) {
                GidsFeature.grant(sp, "diertjes/landdiertjes_sapje");
            }
            return rest;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.guhs.landdiertjes_bessensapje.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** An item with a grey lore line (item.guhs.&lt;id&gt;.lore). */
    public static class LoreItem extends Item {
        public LoreItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }
}
