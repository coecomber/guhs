package nl.juiced.guhs.feature.vadsrijk.geluid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;

/**
 * T-GELUID, the client side: the sliders (guhs-vadsrijk-geluid.toml), the music player ({@link GeluidSpeler}), the ambience
 * ({@link GebiedSfeerSpeler}), the packet receiver and the "secret found" stinger (it listens to kern's title cards: every
 * "...schat.titel" card of zones 1-3 is a found treasure). Call {@link #init} AFTER kern's client init (it wraps kern's receiver).
 */
public final class GeluidClient {
    private GeluidClient() {
    }

    public static void init(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.CLIENT, GeluidConfig.SPEC, "guhs-vadsrijk-geluid.toml"));
        GeluidSpeler.init();
        GebiedSfeerSpeler.init();
        GeluidPayloads.speel = s -> Minecraft.getInstance().execute(() -> ontvang(s.soort(), s.naam()));
        var oud = KernPayloads.titelOntvanger;
        KernPayloads.titelOntvanger = t -> {
            oud.accept(t);
            if (isSchat(t.titel())) {
                Minecraft.getInstance().execute(() -> GeluidSpeler.stinger("geheim"));
            }
        };
    }

    static boolean isSchat(Component c) {
        return c.getContents() instanceof TranslatableContents tc && tc.getKey().endsWith("schat.titel");
    }

    private static void ontvang(String soort, String naam) {
        switch (soort) {
            case "stinger" -> GeluidSpeler.stinger(naam);
            case "gebied", "baas" -> GeluidSpeler.voorbeeld(soort, naam);
            case "bed" -> GebiedSfeerSpeler.voorbeeld(naam);
            case "stop" -> {
                GeluidSpeler.stopAlles();
                GebiedSfeerSpeler.stop();
            }
            case "nu" -> GeluidSpeler.nu();
            case "info" -> GeluidSpeler.info();
            default -> {
            }
        }
    }
}
