package nl.juiced.guhs.feature.elftocht;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * The tour's housekeeping: every tick the ride ({@link ElftochtTocht#tick}) and the skates ({@link ElftochtSchaatsen},
 * both sides); skaters can't get hurt; the loaned skates and stempelkaart never leave the tour (no dropping, no chests,
 * no item frames; gone when you log out, die or change dimension).
 */
public final class ElftochtEvents {

    private static boolean geleend(ItemStack stack) {
        return stack.is(ElftochtFeature.SCHAATSEN.get()) || stack.is(ElftochtFeature.STEMPELKAART.get());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Pre event) {
        ElftochtSchaatsen.tick(event.getEntity());
    }

    @SubscribeEvent
    public static void onTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ElftochtTocht.tick(player);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && ElftochtTocht.isBezig(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (geleend(stack)) {
            event.setCanceled(true);
            if (ElftochtTocht.isBezig(event.getPlayer())) {
                event.getPlayer().getInventory().add(stack.copy());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ElftochtTocht.isBezig(player)) {
            ElftochtTocht.stop(player, null);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> geleend(item.getItem()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ElftochtTocht.isBezig(player)) {
            ElftochtTocht.stop(player, null);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ElftochtTocht.isBezig(event.getEntity())) {
            ElftochtTocht.opruimen(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ElftochtTocht.isBezig(player)) {
            ElftochtTocht.stop(player, "gui.guhs.elftocht.verlaten");
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ElftochtTocht.vergeetAlles();
    }

    /** Whatever got put into a chest, a guh's backpack...: loaned things in it disappear. */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        for (Slot slot : event.getContainer().slots) {
            if (slot.container != event.getEntity().getInventory() && geleend(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
            }
        }
    }

    /** No putting loaned things into blocks that keep them without a screen. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (geleend(event.getItemStack()) && event.getLevel().getBlockEntity(event.getPos()) != null) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    /** No hanging them in an item frame or giving them to an armour stand, an allay... (NPCs are fine: talking). */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (geleend(event.getItemStack()) && !(event.getTarget() instanceof GuhNpcEntity)
                && !(event.getTarget() instanceof nl.juiced.guhs.entity.GuhEntity)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (geleend(event.getItemStack()) && !(event.getTarget() instanceof GuhNpcEntity)
                && !(event.getTarget() instanceof nl.juiced.guhs.entity.GuhEntity)) {
            event.setCanceled(true);
        }
    }

    private ElftochtEvents() {
    }
}
