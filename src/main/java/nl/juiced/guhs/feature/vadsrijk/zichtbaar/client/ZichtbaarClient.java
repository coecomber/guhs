package nl.juiced.guhs.feature.vadsrijk.zichtbaar.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.zichtbaar.ZichtbaarPayloads;

/**
 * U-ZICHTBAAR client: the synced looks per player ({@link #looks}), the render layer {@link ZichtbaarLaag} on both player
 * renderers, its render-state modifier, and the per-frame FPS budget.
 */
public final class ZichtbaarClient {
    private static final Map<UUID, List<String>> LOOKS = new ConcurrentHashMap<>();
    /** Other players that got the small parts this frame (reset every frame). */
    static int budgetGebruikt;

    private ZichtbaarClient() {
    }

    public static List<String> looks(UUID u) {
        return LOOKS.getOrDefault(u, List.of());
    }

    public static void init(IEventBus modBus) {
        ZichtbaarPayloads.ontvanger = u -> Minecraft.getInstance().execute(() -> LOOKS.put(u.speler(), u.looks()));
        modBus.addListener(ZichtbaarClient::lagen);
        modBus.addListener(ZichtbaarClient::renderStates);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> LOOKS.clear());
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre e) -> budgetGebruikt = 0);
    }

    private static void lagen(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> r = event.getPlayerRenderer(skin);
            if (r != null) {
                r.addLayer(new ZichtbaarLaag(r, skin == PlayerModelType.SLIM));
            }
        }
    }

    private static void renderStates(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar instanceof Player p) {
                    ZichtbaarLaag.extract(p, state);
                }
            }
        });
    }
}
