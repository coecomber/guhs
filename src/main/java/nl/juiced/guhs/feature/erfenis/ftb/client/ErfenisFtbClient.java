package nl.juiced.guhs.feature.erfenis.ftb.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.erfenis.client.OverzichtScherm;
import nl.juiced.guhs.feature.erfenis.ftb.ErfenisFtbFeature;
import nl.juiced.guhs.feature.erfenis.kaart.client.KaartScherm;

/** The FTB quests "Van bovenaf" and "Op de kaart": tells the server when the overview or the world map opens. */
public final class ErfenisFtbClient {
    private ErfenisFtbClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            String wat = e.getNewScreen() instanceof OverzichtScherm ? "overzicht" : e.getNewScreen() instanceof KaartScherm ? "kaart" : null;
            if (wat != null && Minecraft.getInstance().getConnection() != null) {
                ClientPacketDistributor.sendToServer(new ErfenisFtbFeature.Geopend(wat));
            }
        });
    }
}
