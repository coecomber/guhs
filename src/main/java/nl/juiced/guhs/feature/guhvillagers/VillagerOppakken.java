package nl.juiced.guhs.feature.guhvillagers;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.registry.ModDataComponents;

/**
 * 1.7.0: pick up any villager (vanilla or guh, grown-up or child) with sneak + right-click and an empty hand, like a tamed
 * guh. It becomes an {@link OpgepakteVillagerItem} that keeps the whole villager (profession, level, xp, trades, type,
 * name, gossip...: its saved data); right-click the ground to put it back. Not while it is trading with someone, and not
 * the invulnerable villagers that structures and minigames place.
 * <p>
 * Its bed, workstation and meeting point are let go of first (like when a villager dies), so they are free for others: a
 * villager that ever traded keeps its profession and finds a new workstation of its kind near its new home (vanilla).
 */
public final class VillagerOppakken {
    @SuppressWarnings("unchecked")
    private static final MemoryModuleType<GlobalPos>[] PLEKKEN = new MemoryModuleType[]{MemoryModuleType.HOME, MemoryModuleType.JOB_SITE,
            MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT};

    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty() || player.isSpectator()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player.level().isClientSide()) {
            return;
        }
        String nee = magNiet(villager, player);
        if (nee != null) {
            player.sendOverlayMessage(Component.translatable(nee));
            return;
        }
        player.getInventory().placeItemBackInInventory(oppakken(villager));
        player.level().playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f,
                villager.isBaby() ? 1.5f : 1f);
    }

    /** Why this villager can't be picked up right now (a translation key), or null when it can. */
    public static String magNiet(Villager villager, Player player) {
        if (!villager.isAlive() || villager.isInvulnerable()) {
            return "gui.guhs.villager_oppakken.kan_niet";
        }
        if (villager.isTrading()) {
            return "gui.guhs.villager_oppakken.handelt";
        }
        return null;
    }

    /** Takes the villager out of the world and returns it as an item. */
    public static ItemStack oppakken(Villager villager) {
        if (villager.isSleeping()) {
            villager.stopSleeping();
        }
        villager.stopRiding();
        villager.ejectPassengers();
        if (villager.isLeashed()) {
            villager.dropLeash();
        }
        for (MemoryModuleType<GlobalPos> plek : PLEKKEN) {
            villager.releasePoi(plek);
            villager.getBrain().eraseMemory(plek);
        }
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.saveWithoutId(villager);
        tag.putString("id", EntityType.getKey(villager.getType()).toString());
        tag.remove("UUID");   // a new one when it is put back (two copies of the item never clash)
        if (villager.hasCustomName()) {
            nl.juiced.guhs.taal.Tekst.put(tag, OpgepakteVillagerItem.NAAM, villager.getCustomName());
        }
        villager.discard();
        ItemStack stack = new ItemStack(GuhVillagersFeature.OPGEPAKTE_VILLAGER.get());
        stack.set(ModDataComponents.GUH_DATA.get(), net.minecraft.world.item.component.CustomData.of(tag));
        return stack;
    }

    private VillagerOppakken() {
    }
}
