package nl.juiced.guhs.feature.einde2.trap5.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.client.screen.BankGuhScreen;
import nl.juiced.guhs.feature.einde2.trap5.E2Trap5;
import nl.juiced.guhs.feature.einde2.trap5.E2Trap5Feature;
import nl.juiced.guhs.feature.einde2.trap5.T5Payloads;

/**
 * einde2 tier 5, client side: the Bestelguh's renderer (the sitting-guh renderer with the BESTELGUH model), the
 * Enderguhrad's renderer and models, the screens of the Thuispoort and the order list, and the "Bestellen" button on the
 * bank screen when it was opened from a Guhterminal (added with {@link ScreenEvent.Init.Post}: the bank screen itself is
 * not this slice's).
 */
public final class E2Trap5Client {
    /** The terminal the bank screen that is opening now came from (null: a plain bank screen). */
    @Nullable
    private static volatile BlockPos terminal;

    private E2Trap5Client() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(E2Trap5Feature.BESTELGUH.get(), SittingGuhRenderers.NpcRenderer::new);
            event.registerBlockEntityRenderer(E2Trap5Feature.ENDERGUHRAD_BE.get(), EnderguhradRenderer::new);
        });
        modBus.addListener(EnderguhradRenderer::modellen);
        T5Payloads.KLANT = new T5Payloads.Klant() {
            @Override
            public void terminal(BlockPos pos) {
                terminal = pos;
            }

            @Override
            public void lijst(T5Payloads.BestelLijst lijst) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof BestelScreen s && s.bron().equals(lijst.bron())) {
                    s.update(lijst);
                } else {
                    mc.setScreen(new BestelScreen(lijst));
                }
            }

            @Override
            public void poort(T5Payloads.PoortOpen open) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof ThuispoortScreen s && s.poort().equals(open.poort())) {
                    s.update(open);
                } else {
                    mc.setScreen(new ThuispoortScreen(open));
                }
            }
        };
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            BlockPos t = terminal;
            if (t != null && event.getScreen() instanceof BankGuhScreen bank) {
                event.addListener(Button.builder(Component.translatable("gui.guhs.einde2_trap5_machines.bestellen"),
                                b -> ClientPacketDistributor.sendToServer(new T5Payloads.BestelVraag(t)))
                        .bounds(bank.getGuiLeft() + bank.getXSize() - 82, bank.getGuiTop() - 22, 82, 20)
                        .tooltip(Tooltip.create(Component.translatable("gui.guhs.einde2_trap5_machines.bestellen.tooltip"))).build());
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing event) -> {
            if (event.getScreen() instanceof BankGuhScreen) {
                terminal = null;
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            terminal = null;
            E2Trap5.klantOpen = false;
        });
    }
}
