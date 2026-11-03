package nl.juiced.guhs.feature.vadsrijk.wereld.client;

import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.wereld.Littekens;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldRegistry;

/**
 * Het Vadsrijk, slice "wereld", client: the zone-1 sky ({@link WereldLucht}, drawn after the vanilla sky), ambient sounds and
 * particles per area ({@link WereldSfeer}) and the per-player tint of the grey dragon scars (flag from {@link Littekens}).
 */
public final class WereldClient {
    /** This player's zone-1 quest is done: the scars are green again. */
    static volatile boolean heel;
    private static final int GRIJS_GRAS = 0x8E8A94, GROEN_GRAS = 0x9AD86E, GRIJS_PLANT = 0xA29CA8, ROZE_PLANT = 0xFF8FC8;

    private WereldClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(WereldClient::kleuren);
        Littekens.ontvanger = k -> Minecraft.getInstance().execute(() -> {
            if (heel != k) {
                heel = k;
                Minecraft mc = Minecraft.getInstance();
                if (mc.levelRenderer != null && mc.level != null) {
                    mc.levelRenderer.allChanged();
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterSky e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && Vadsrijk.in(mc.level)) {
                WereldLucht.teken(e);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> WereldSfeer.tick());
    }

    private static void kleuren(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new Tint(GRIJS_GRAS, GROEN_GRAS)), WereldRegistry.LITTEKEN_GRAS.get());
        event.register(List.of(new Tint(GRIJS_PLANT, ROZE_PLANT)), WereldRegistry.LITTEKEN_PLANT.get());
    }

    private record Tint(int grijs, int geheeld) implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return ARGB.opaque(heel ? geheeld : grijs);
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of();
        }
    }
}
