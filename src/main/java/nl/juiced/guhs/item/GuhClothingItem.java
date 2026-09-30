package nl.juiced.guhs.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingOntgrendel;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * A piece of guh clothing. 2.9: a one-time UNLOCK. Hold right-click (~1.5 s, like eating) to use it up: from then on the
 * piece is in the wardrobe of all your tamed guhs (feature.kleding). A piece you already have can't be used up ("Deze heb
 * je al, njeg!"): give it to a friend. Right-click your own guh with a piece you already have: it puts it on right away.
 * <p>
 * The kapsels (hair, GuhClothes.Slot.HAAR) are not unlocks: they are used on a guh at the kapper (feature.kapper).
 */
public class GuhClothingItem extends Item {
    /** How long you hold right-click to unlock it (ticks). */
    public static final int ONTGRENDEL_TICKS = 30;

    private final GuhClothes clothes;

    public GuhClothingItem(GuhClothes clothes, Properties properties) {
        super(properties);
        this.clothes = clothes;
    }

    public GuhClothes getClothes() {
        return clothes;
    }

    /** Can this item be unlocked at all? (Not the hair.) */
    public boolean isOntgrendelbaar() {
        return clothes.slot != GuhClothes.Slot.HAAR;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isOntgrendelbaar()) {
            return InteractionResult.PASS;
        }
        if (KledingUnlocks.heeft(player, clothes)) {
            if (player instanceof ServerPlayer sp) {
                KledingOntgrendel.alGehad(sp, clothes);
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME.heldItemTransformedTo(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isOntgrendelbaar() ? ONTGRENDEL_TICKS : 0;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return isOntgrendelbaar() ? ItemUseAnimation.EAT : ItemUseAnimation.NONE;   // (the crumbs look like confetti of the piece)
    }

    /**
     * 26.1: the eating sound + crumbs while unlocking come from a CONSUMABLE component (Item#getEatingSound is gone).
     * Only its sounds/particles are used: use() and finishUsingItem() stay ours.
     */
    public static Properties properties(GuhClothes clothes, Properties props) {
        if (clothes.slot == GuhClothes.Slot.HAAR) {
            return props;
        }
        return props.component(net.minecraft.core.component.DataComponents.CONSUMABLE, net.minecraft.world.item.component.Consumable.builder()
                .consumeSeconds(ONTGRENDEL_TICKS / 20.0f).animation(ItemUseAnimation.EAT).sound(SoundEvents.ARMOR_EQUIP_LEATHER).build());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && isOntgrendelbaar()) {
            if (KledingOntgrendel.ontgrendel(player, clothes)) {
                stack.consume(1, player);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (!isOntgrendelbaar()) {
            tooltip.accept(Component.translatable("item.guhs.guh_clothes.lore").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (KledingUnlocks.Client.heeft(clothes)) {
            tooltip.accept(Component.translatable("item.guhs.guh_clothes.al_ontgrendeld").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.accept(Component.translatable("item.guhs.guh_clothes.ontgrendel").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("gui.guhs.menu.clothes." + clothes.slot.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_PURPLE));
        String bron = KledingBronnen.bron(clothes);
        if (bron != null) {
            tooltip.accept(Component.translatable("item.guhs.guh_clothes.bron", Component.translatable("gui.guhs.kledingbron." + bron))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
