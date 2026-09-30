package nl.juiced.guhs.feature.emotes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.entity.GuhEntity;

/** Client side of the emotes feature: the particles and the emote picker (only loaded on the client). */
public final class EmotesClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> EmoteParticles.register(event));
    }

    /** Opens the emote picker (from the Guh menu); Back returns to `parent`. */
    public static void openPicker(GuhEntity guh, Screen parent) {
        Minecraft.getInstance().setScreen(new EmotePickerScreen(guh, parent));
    }

    private EmotesClient() {
    }
}
