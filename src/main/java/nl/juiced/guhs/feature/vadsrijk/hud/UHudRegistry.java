package nl.juiced.guhs.feature.vadsrijk.hud;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * U-HUD (Guhs 1.10.0, round U): party frames, emotes and the HUD editor. Server side: {@link PartyFrames} (the sync),
 * {@link Emotes} (+ {@link Emote}), commands {@link UHudCommando}. Client: {@code hud.client} (frames, emote wheel + poses,
 * HUD editor). See NOTITIES_U_HUD.md.
 */
public final class UHudRegistry {
    private UHudRegistry() {
    }

    public static void register(IEventBus modBus) {
        PartyFrames.init();
        nl.juiced.guhs.world.TikWacht.post("UHudRegistry:22", e -> {
            PartyFrames.tick(e.getServer());
            Emotes.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e) -> {
            if (e.getEntity() instanceof ServerPlayer kijker && e.getTarget() instanceof Player doel) {
                Emotes.inBeeld(kijker, doel);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            PartyFrames.vergeet(e.getEntity().getUUID());
            Emotes.vergeet(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener(UHudCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        UHudPayloads.register(registrar);
    }
}
