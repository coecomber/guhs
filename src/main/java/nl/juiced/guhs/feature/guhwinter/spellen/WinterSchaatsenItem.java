package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.elftocht.ElftochtSchaatsen;

/**
 * Guhwinter-schaatsen: the elftocht skates for the winter plaza (bought in the pepernoten shop, or borrowed for one
 * schaatsrondje). On your feet or in either hand, on ice (the elftocht's ice tag {@code guhs:elftocht/schaatsijs}: all
 * vanilla ice), you glide {@link #SNELHEID} x faster, like on the Elftocht ({@link ElftochtSchaatsen}); off the ice you
 * just walk. A borrowed pair ({@link #leen}) goes back as soon as your lap is over, and never into a chest.
 */
public class WinterSchaatsenItem extends Item {
    public static final Identifier MODIFIER = Guhs.id("guhwinter_schaatsen");
    public static final double SNELHEID = ElftochtSchaatsen.SNELHEID;
    private static final String LEEN = "guhwinter_leen", LAATST = "guhs_guhwinter_ijs";

    public WinterSchaatsenItem(Properties properties) {
        super(properties);
    }

    /** A borrowed pair for one lap. */
    public static ItemStack leen() {
        ItemStack s = new ItemStack(SpellenFeature.SCHAATSEN.get());
        CompoundTag t = new CompoundTag();
        t.putBoolean(LEEN, true);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    public static boolean isLeen(ItemStack s) {
        if (s.isEmpty() || !s.is(SpellenFeature.SCHAATSEN.get())) {
            return false;
        }
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().getBooleanOr(LEEN, false);
    }

    /** Does the player have skates anywhere (own or borrowed)? */
    public static boolean heeft(Player p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(SpellenFeature.SCHAATSEN.get())) {
                return true;
            }
        }
        return p.getItemBySlot(EquipmentSlot.FEET).is(SpellenFeature.SCHAATSEN.get());
    }

    public static void neemLeenTerug(Player p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isLeen(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (isLeen(p.getItemBySlot(EquipmentSlot.FEET))) {
            p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        }
        if (isLeen(p.containerMenu.getCarried())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        if (isLeen(stack) && (!(entity instanceof ServerPlayer p) || !Schaatsrondje.bezig(p))) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return !isLeen(item);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (isLeen(stack) && !entity.level().isClientSide()) {
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guhwinter_schaatsen.lore").withStyle(ChatFormatting.GRAY));
        if (isLeen(stack)) {
            tooltip.accept(Component.translatable("item.guhs.guhwinter_schaatsen.leen").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        }
    }

    // --- skating (both sides, every player tick) ---------------------------------------------------------------------------

    static boolean aan(Player p) {
        Item s = SpellenFeature.SCHAATSEN.get();
        return p.getItemBySlot(EquipmentSlot.FEET).is(s) || p.getMainHandItem().is(s) || p.getOffhandItem().is(s);
    }

    static boolean schaatst(Player p) {
        if (!aan(p) || p.isPassenger() || p.getAbilities().flying || p.isInWater() || p.isFallFlying()) {
            return false;
        }
        if (ElftochtSchaatsen.opIJs(p)) {
            return true;
        }
        var data = p.getPersistentData();
        return !p.onGround() && data.contains(LAATST) && p.tickCount - data.getIntOr(LAATST, 0) <= ElftochtSchaatsen.SPRONG;
    }

    static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (ElftochtSchaatsen.opIJs(p)) {
            p.getPersistentData().putInt(LAATST, p.tickCount);
        }
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean nu = schaatst(p);
        boolean heeft = speed.hasModifier(MODIFIER);
        if (nu && !heeft) {
            speed.addTransientModifier(new AttributeModifier(MODIFIER, SNELHEID, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!nu && heeft) {
            speed.removeModifier(MODIFIER);
        }
    }

    /** A borrowed pair left in a chest is gone when it closes. */
    static void dichtKist(PlayerContainerEvent.Close event) {
        for (Slot slot : event.getContainer().slots) {
            if (slot.container != event.getEntity().getInventory() && isLeen(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
            }
        }
    }
}
