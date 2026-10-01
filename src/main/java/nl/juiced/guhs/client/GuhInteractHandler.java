package nl.juiced.guhs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.screen.GuhScreen;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.network.GuhActionPayload;
import nl.juiced.guhs.registry.ModItems;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * Right-clicking your own guh:
 * <ul>
 *     <li>short tap  -> pet it (1.2.0: only petting; sit/stand is in the Guh menu), or ride it when it is big enough and saddled</li>
 *     <li>sneak + tap -> pick it up (as an item)</li>
 *     <li>hold       -> open the Guh menu</li>
 * </ul>
 * Holding kaas knabbels, a snack (#guhs:band/snacks, 2.10), a lead, a name tag or a saddle skips this so
 * feeding/leashing/naming/saddling work normally.
 */
@EventBusSubscriber(modid = Guhs.MODID, value = Dist.CLIENT)
public final class GuhInteractHandler {
    /** How long (in ticks, 20 = 1 second) right-click must be held to open the menu. */
    private static final int HOLD_TICKS = 8;

    private static int trackedGuhId = -1;
    private static int heldTicks;

    @SubscribeEvent
    public static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // riding a sled: right-click = the sled's panel
        if (mc.player != null && mc.player.getVehicle() instanceof nl.juiced.guhs.entity.GuhSleeEntity sled && event.getHand() == InteractionHand.MAIN_HAND) {
            event.setCanceled(true);
            event.setSwingHand(false);
            mc.setScreen(new nl.juiced.guhs.client.screen.SledPanelScreen(sled));
            return;
        }
        // riding your own guh with an empty hand: right-click = launch (or stop the launch)
        if (mc.player != null && mc.player.getVehicle() instanceof GuhEntity mount && mount.isOwnedBy(mc.player)
                && mc.player.getMainHandItem().isEmpty() && event.getHand() == InteractionHand.MAIN_HAND) {
            event.setCanceled(true);
            event.setSwingHand(false);
            ClientPacketDistributor.sendToServer(new GuhActionPayload(mount.getId(), GuhActionPayload.Action.LAUNCH));
            return;
        }
        GuhEntity guh = ownGuhUnderCrosshair(mc);
        if (guh == null || !handlesItem(mc.player.getMainHandItem(), guh)) {
            return;
        }
        // Swallow the vanilla interaction; we decide between tap and hold once the button is released.
        event.setCanceled(true);
        event.setSwingHand(false);
        if (trackedGuhId != guh.getId() && event.getHand() == InteractionHand.MAIN_HAND) {
            trackedGuhId = guh.getId();
            heldTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (trackedGuhId == -1) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuhEntity guh = ownGuhUnderCrosshair(mc);
        if (guh == null || guh.getId() != trackedGuhId || mc.screen != null) {
            trackedGuhId = -1; // looked away / something else opened
            return;
        }
        if (mc.options.keyUse.isDown()) {
            if (++heldTicks >= HOLD_TICKS) {
                trackedGuhId = -1;
                mc.setScreen(new GuhScreen(guh));
            }
        } else {
            trackedGuhId = -1;
            ClientPacketDistributor.sendToServer(new GuhActionPayload(guh.getId(), GuhActionPayload.Action.TAP));
        }
    }

    private static GuhEntity ownGuhUnderCrosshair(Minecraft mc) {
        Player player = mc.player;
        if (player == null || !(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof GuhEntity guh)) {
            return null;
        }
        return guh.isTame() && guh.isOwnedBy(player) ? guh : null;
    }

    /** Items that should keep their normal right-click behaviour on a guh (feeding, leashing, naming, saddling). */
    private static boolean handlesItem(ItemStack stack, GuhEntity guh) {
        return !stack.is(ModItems.KAAS_KNABBELS.get()) && !stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get())
                && !stack.is(nl.juiced.guhs.feature.band.BandFeature.SNACKS)   // 2.10: feeding a snack (hearts); a tap is a pet
                && !stack.is(Items.LEAD) && !stack.is(Items.NAME_TAG) && !(stack.is(Items.SADDLE) && !guh.isSaddled());
    }

    private GuhInteractHandler() {
    }
}
