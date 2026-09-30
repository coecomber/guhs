package nl.juiced.guhs.feature.piep.client;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.feature.piep.PiepPayloads;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * Client side of the piep-maatje menus: opens {@link PiepMenuScreen} when the server says so (guhs:piep_menu), and sends the
 * poetsbeurt / bestie-moment click on a guh (guhs:piep_op_guh).
 * <p>
 * Why the latter: the guh's own right-click handler (client.GuhInteractHandler, tap = sit/stand, hold = the guh menu)
 * swallows every empty-hand click on your OWN tamed guh before the server sees an interaction, so GuhHooks.klik (and with it
 * Poepschilly's poetsbeurt) never ran for your own guhs: "nothing happens". While one of your turtles is ready
 * ({@link PiepPayloads#CLIENT_KLAAR}), this listener (priority HIGH, so before that handler) takes the empty-hand click on a
 * guh instead and asks the server for the poetsbeurt. Otherwise it leaves every click alone.
 */
public final class PiepMenuClient {
    private PiepMenuClient() {
    }

    public static void init() {
        PiepPayloads.menuOpener = PiepMenuClient::open;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PiepMenuClient::onUseKey);
        NeoForge.EVENT_BUS.addListener(PiepMenuClient::onLogout);
    }

    private static void open(PiepPayloads.MenuOpen p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(p.dier()) instanceof PiepMaatje maatje) {
            mc.setScreen(new PiepMenuScreen(maatje, p.rust()));
        }
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PiepPayloads.CLIENT_KLAAR.clear();
    }

    /** Is one of my turtles ready for a guh right now? (forgets the ones that ran out or are gone) */
    static boolean turtleKlaar(Minecraft mc) {
        if (mc.level == null || PiepPayloads.CLIENT_KLAAR.isEmpty()) {
            return false;
        }
        long now = mc.level.getGameTime();
        boolean klaar = false;
        for (Map.Entry<Integer, Long> e : PiepPayloads.CLIENT_KLAAR.entrySet()) {
            if (e.getValue() < now || mc.level.getEntity(e.getKey()) == null) {
                PiepPayloads.CLIENT_KLAAR.remove(e.getKey());
            } else {
                klaar = true;
            }
        }
        return klaar;
    }

    private static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.getMainHandItem().isEmpty() || mc.player.isShiftKeyDown()
                || !(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof GuhEntity guh) || !turtleKlaar(mc)) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(true);
        PiepPayloads.CLIENT_KLAAR.clear();
        ClientPacketDistributor.sendToServer(new PiepPayloads.OpGuh(guh.getId()));
    }
}
