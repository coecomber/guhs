package nl.juiced.guhs.feature.vadsrijk.party.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu;
import nl.juiced.guhs.feature.vadsrijk.party.PartyPayloads;
import nl.juiced.guhs.feature.vadsrijk.party.PartyRegistry;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpeler;

/**
 * W-PARTY client: the Party tab ({@link PartyPaneel}, menu order 112, between Ridders and Knuffelbond), the invite toast, the
 * nep-speler renderer, and the setting "Nep-spelers: aan/uit" (client config; sent to the server at login and on every change, and
 * used here to hide them).
 */
public final class PartyClient {
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(8000L);
    @Nullable
    private static Boolean verstuurd;

    private PartyClient() {
    }

    public static void init(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.CLIENT, NepClientConfig.SPEC,
                "guhs-vadsrijk-nepspelers.toml"));
        NepSpeler.ZICHTBAAR = NepClientConfig::aan;
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(PartyRegistry.NEPSPELER.get(), NepSpelerRenderer::new));
        VadsrijkMenu.tab("party", () -> new ItemStack(Items.CAKE), () -> stuur(new PartyPayloads.Vraag(true)), 112);
        PartyPayloads.staatOntvanger = s -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof PartyPaneel p) {
                p.update(s.data(), List.of());
            } else if (s.open()) {
                mc.setScreen(new PartyPaneel(s.data()));
            }
        });
        PartyPayloads.uitnodigingOntvanger = u -> Minecraft.getInstance().execute(() -> SystemToast.addOrUpdate(Minecraft.getInstance().getToastManager(), TOAST,
                Component.translatable("vadsrijk.party.toast.titel"), Component.translatable("vadsrijk.party.toast.tekst", u.van())));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> verstuurd = null);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.getConnection() == null) {
                return;
            }
            if (mc.screen instanceof PartyPaneel && mc.player.tickCount % 40 == 0) {
                stuur(new PartyPayloads.Vraag(false));
            }
            if (mc.player.tickCount % 20 == 7) {
                boolean aan = NepClientConfig.aan();
                if (verstuurd == null || verstuurd != aan) {
                    verstuurd = aan;
                    if (!stuur(new PartyPayloads.NepStand(aan))) {
                        verstuurd = null;
                    }
                }
            }
        });
    }

    static boolean stuur(net.minecraft.network.protocol.common.custom.CustomPacketPayload p) {
        try {
            ClientPacketDistributor.sendToServer(p);
            return true;
        } catch (RuntimeException ex) {
            return false;   // (a server without this mod)
        }
    }
}
