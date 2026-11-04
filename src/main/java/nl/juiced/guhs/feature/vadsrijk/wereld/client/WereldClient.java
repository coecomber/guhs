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
    /**
     * MOMENTEN-HOOK (fix round 1, feature/vadsrijk/momenten GolfClient): while the recolour wave runs (or waits to run) this
     * decides per block whether a scar is healed already; null = the plain flag {@link #heel}.
     */
    public static volatile java.util.function.Predicate<net.minecraft.core.BlockPos> golf;
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
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            WereldSfeer.tick();
            vluchtTick();
        });
        nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen.ontvanger = t -> Minecraft.getInstance().execute(() -> overslaanTicks = t);
        nl.juiced.guhs.feature.vadsrijk.zone2.wereld.client.Z2WereldClient.init(modBus);   // [zone 2] sky + ambient of De Splinterpieken
    }

    /** The airship flight may be skipped by sneaking for this many more ticks (0 = not skippable / no flight). */
    private static int overslaanTicks;

    private static void vluchtTick() {
        if (overslaanTicks <= 0) {
            return;
        }
        overslaanTicks--;
        Minecraft mc = Minecraft.getInstance();
        com.mojang.blaze3d.platform.InputConstants.Key k = mc.options.keyShift.getKey();
        if (k.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM && overslaanTicks < WereldReizenDuur.MAX - 20
                && com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(), k.getValue())) {
            overslaanTicks = 0;
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                    new nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen.Overslaan());
        }
    }

    /** (the first second of a flight never skips: the sneak that may still be held from before) */
    private static final class WereldReizenDuur {
        static final int MAX = 720;
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
        public int colorInWorld(BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
            java.util.function.Predicate<net.minecraft.core.BlockPos> g = golf;   // MOMENTEN-HOOK: the wave decides per position
            return g == null ? color(state) : ARGB.opaque(g.test(pos) ? geheeld : grijs);
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of();
        }
    }
}
