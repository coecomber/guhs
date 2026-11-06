package nl.juiced.guhs.feature.vadsrijk.dialoog.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knuffeldal.client.PraatScherm;

/** T-DIALOOG client: the options file, and the Vadsrijk talking screen for payloads marked "Rijk". */
public final class DialoogClient {
    private DialoogClient() {
    }

    public static void init(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.CLIENT, DialoogConfig.SPEC, "guhs-vadsrijk-dialoog.toml"));
        PraatScherm.stijl = (npcId, data) -> data.getBooleanOr("Rijk", false) ? new RijkPraatScherm(npcId, data) : null;
    }
}
