package nl.juiced.guhs.feature.voetjes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingKleermaker;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 "Onder de Wol" (ronde 26): the FEET slot of the wardrobe (GuhClothes.Slot.FEET) and its six pieces.
 * <ul>
 *   <li>pluisslofjes, regenlaarsjes: the kleermaker's fixed offer (emeralds; ModVillagers.kleermakerAanbod)</li>
 *   <li>golfschoentjes: the Golfguh's shop (golfballetjes)</li>
 *   <li>mijnwerkerslaarsjes: rare in the chests of the Verlaten kaasmijnschacht</li>
 *   <li>kaassauslopers: rare in the chests of the Mika-grillpaleis and the Spiesburcht (Barbecuether); walks on every
 *       kaassaus ({@link Voetjes})</li>
 *   <li>lavalopers: the kleermaker's upgrade (right-click him with magma cream): kaassauslopers + 4 magma cream + 1
 *       vahoege vads; walks on lava and on every kaassaus</li>
 * </ul>
 * Resources: tools/features/voetjes.py (bones, textures, icons, texts, loot).
 */
public final class VoetjesFeature {
    /** The kleermaker's prices (emeralds). */
    public static final int PRIJS_SLOFJES = 6, PRIJS_REGENLAARSJES = 8;
    /** The Golfguh's price (golfballetjes; his shop is the golf feature's). */
    public static final int PRIJS_GOLFSCHOENTJES = 12;
    /** The upgrade at the kleermaker: kaassauslopers + this much magma cream + this many vahoege vads. */
    public static final int MAGMA = 4, VADS = 1;

    public static void register(IEventBus modBus) {
        KledingBronnen.bron(GuhClothes.PLUISSLOFJES, "kleermaker", KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_SLOFJES));
        KledingBronnen.bron(GuhClothes.REGENLAARSJES, "kleermaker", KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_REGENLAARSJES));
        KledingBronnen.bron(GuhClothes.LAVALOPERS, "kleermaker", KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.lavalopers_opwaarderen"));
        KledingBronnen.bron(GuhClothes.GOLFSCHOENTJES, "golf", KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.golfballetjes", PRIJS_GOLFSCHOENTJES));
        KledingBronnen.bron(GuhClothes.MIJNWERKERSLAARSJES, "loot_mijnschacht", KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.kisten_in_de_kaasmijnschacht"));
        KledingBronnen.bron(GuhClothes.KAASSAUSLOPERS, "loot_barbecuether", KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.kisten_van_de_barbecuether_mikas"));
        NeoForge.EVENT_BUS.addListener(VoetjesFeature::onGuhTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, VoetjesFeature::onInteract);
        NeoForge.EVENT_BUS.addListener(VoetjesFeature::onTooltip);
    }

    /** Floating on the sauce, never burning there, and paths across it (server: the guh's AI moves it there). */
    private static void onGuhTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof GuhEntity guh) || guh.level().isClientSide()) {
            return;
        }
        if (guh.tickCount % 20 == 0) {
            Voetjes.zetPaden(guh);
        }
        if (!Voetjes.heeftLopers(guh)) {
            return;
        }
        Voetjes.drijf(guh);
        if (guh.isOnFire() && Voetjes.vuurvast(guh)) {
            guh.clearFire();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // the kleermaker's upgrade: kaassauslopers -> lavalopers
    // ---------------------------------------------------------------------------------------------------------------

    /** Right-clicking a kleermaker with magma cream in your hand: the upgrade (instead of his trades). */
    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || !KledingKleermaker.is(villager)
                || !event.getEntity().getItemInHand(event.getHand()).is(Items.MAGMA_CREAM)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            waardeerOp(player, villager);
        }
    }

    /** What the upgrade still needs (empty: everything is there). */
    public static List<Component> mist(Player player) {
        List<Component> mist = new ArrayList<>();
        if (tel(player, ModItems.clothingItem(GuhClothes.KAASSAUSLOPERS)) == 0 && !KledingUnlocks.heeft(player, GuhClothes.KAASSAUSLOPERS)) {
            mist.add(Component.translatable("gui.guhs.voetjes.mist.lopers"));
        }
        if (tel(player, Items.MAGMA_CREAM) < MAGMA) {
            mist.add(Component.translatable("gui.guhs.voetjes.mist.magma", MAGMA));
        }
        if (tel(player, ModItems.VAHOEGE_VADS.get()) < VADS) {
            mist.add(Component.translatable("gui.guhs.voetjes.mist.vads", VADS));
        }
        return mist;
    }

    /**
     * The upgrade. The kaassauslopers item is handed in when you carry one; when you already unlocked them, the
     * kleermaker just copies the pattern (the unlock stays: guhs wearing them keep them). True when it happened.
     */
    public static boolean waardeerOp(ServerPlayer player, Villager kleermaker) {
        if (KledingUnlocks.heeft(player, GuhClothes.LAVALOPERS) || tel(player, ModItems.clothingItem(GuhClothes.LAVALOPERS)) > 0) {
            player.sendSystemMessage(Component.translatable("gui.guhs.voetjes.al").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        List<Component> mist = mist(player);
        if (!mist.isEmpty()) {
            Component lijst = Component.empty();
            for (int i = 0; i < mist.size(); i++) {
                lijst = i == 0 ? mist.get(i) : Component.translatable("gui.guhs.voetjes.en", lijst, mist.get(i));
            }
            player.sendSystemMessage(Component.translatable("gui.guhs.voetjes.mist", lijst).withStyle(ChatFormatting.LIGHT_PURPLE));
            kleermaker.playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
            return false;
        }
        neem(player, ModItems.clothingItem(GuhClothes.KAASSAUSLOPERS), 1);   // (none carried: the unlock is the pattern)
        neem(player, Items.MAGMA_CREAM, MAGMA);
        neem(player, ModItems.VAHOEGE_VADS.get(), VADS);
        GuhQuests.give(player, ModItems.clothingItem(GuhClothes.LAVALOPERS));
        player.sendSystemMessage(Component.translatable("gui.guhs.voetjes.opgewaardeerd").withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, kleermaker.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.NEUTRAL, 1f, 1.2f);
        kleermaker.playSound(SoundEvents.VILLAGER_YES, 1f, 1f);
        return true;
    }

    private static int tel(Player player, Item item) {
        int n = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        ItemStack off = player.getOffhandItem();
        return n + (off.is(item) ? off.getCount() : 0);
    }

    private static void neem(Player player, Item item, int aantal) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (aantal <= 0) {
                return;
            }
            if (stack.is(item)) {
                int t = Math.min(aantal, stack.getCount());
                stack.shrink(t);
                aantal -= t;
            }
        }
        ItemStack off = player.getOffhandItem();
        if (aantal > 0 && off.is(item)) {
            off.shrink(Math.min(aantal, off.getCount()));
        }
    }

    /** What the lopers do, under their name (client). */
    private static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof GuhClothingItem item
                && (item.getClothes() == GuhClothes.KAASSAUSLOPERS || item.getClothes() == GuhClothes.LAVALOPERS)) {
            event.getToolTip().add(1, Component.translatable("item.guhs." + item.getClothes().id() + ".uitleg").withStyle(ChatFormatting.GOLD));
        }
    }

    private VoetjesFeature() {
    }
}
