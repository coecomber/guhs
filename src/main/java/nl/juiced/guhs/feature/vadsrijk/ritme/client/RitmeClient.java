package nl.juiced.guhs.feature.vadsrijk.ritme.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.ritme.RitmePayloads;
import nl.juiced.guhs.feature.vadsrijk.ritme.Steden;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client.SfeerClient;

/**
 * U-LEVEN-RITME client: the weather + fireflies ({@link WeerBeeld}), the rainbow of the zone 2-4 showers (through
 * {@link SfeerClient#U_REGENBOOG}) and the dusk light wave in every town (through {@link SfeerClient#U_STADPLEIN}: fix-sfeer's
 * Lichten scans the other towns' light blocks too and switches them on in a wave out from their own square, with halos all
 * night; Honingtak keeps its own zone-3 evening glow). Config {@link RitmeConfig}.
 */
public final class RitmeClient {
    private static ClientLevel vorig;

    private RitmeClient() {
    }

    public static void init(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.CLIENT, RitmeConfig.SPEC, "guhs-vadsrijk-ritme.toml"));
        RitmePayloads.forceer = v -> Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().level != null) {
                WeerBeeld.forceer(Minecraft.getInstance().level, v.wat());
            }
        });
        SfeerClient.U_REGENBOOG = () -> actief() ? WeerBeeld.boogNu() : 0;
        SfeerClient.U_STADPLEIN = RitmeClient::plein;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (ViewportEvent.ComputeFogColor e) -> {
            if (actief()) {
                WeerBeeld.kleur(e);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (ViewportEvent.RenderFog e) -> {
            if (actief()) {
                WeerBeeld.afstand(e);
            }
        });
    }

    /** The square of a town other than Zwamzoet (and Honingtak, which has its own glow), for the dusk light wave. */
    static BlockPos plein(double x, double y, double z) {
        for (Steden.Stad s : Steden.ALLE) {
            if (s.id().equals("zwamzoet") || s.id().equals("honingtak")) {
                continue;
            }
            if (Double.isNaN(y) ? s.bevatXZ(x, z) : s.bevat(x, y, z)) {
                if (s.markt() != null) {
                    return s.markt().get();
                }
                return s.bord() != null ? s.bord().get() : s.midden();
            }
        }
        return null;
    }

    private static boolean actief() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null && Vadsrijk.in(mc.level);
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != vorig) {
            vorig = mc.level;
            WeerBeeld.wis();
        }
        if (!actief() || mc.isPaused()) {
            return;
        }
        WeerBeeld.tick(mc);
    }
}
