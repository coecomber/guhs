package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.SfeerPayloads;

/**
 * Client side of the Vadsrijk atmosphere (see {@code sfeer.SfeerRegistry}): fog per area ({@link GebiedMist}), the sky layer
 * ({@link SfeerLucht}), the world layer ({@link SfeerWereld}, {@link Lichten}) and particles/sounds ({@link SfeerDeeltjes}).
 * Config {@code config/guhs-vadsrijk-sfeer.toml} ({@link SfeerConfig}): VOL / ZACHT / UIT.
 */
public final class SfeerClient {
    private static ClientLevel vorig;

    private SfeerClient() {
    }

    public static void init(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.CLIENT, SfeerConfig.SPEC, "guhs-vadsrijk-sfeer.toml"));
        modBus.addListener((RegisterRenderPipelinesEvent e) -> Teken.pipelines(e));
        SfeerPayloads.vlaggen = v -> Minecraft.getInstance().execute(() -> Momenten.opperzwam = v.opperzwam());
        SfeerPayloads.forceer = v -> Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().level != null) {
                Momenten.forceer(Minecraft.getInstance().level, v.wat());
            }
        });
        SfeerPayloads.draakPas = v -> Minecraft.getInstance().execute(() -> SfeerDeeltjes.pas(v));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFogColor e) -> {
            if (actief()) {
                GebiedMist.kleur(e);
            }
        });
        NeoForge.EVENT_BUS.addListener((ViewportEvent.RenderFog e) -> {
            if (actief()) {
                GebiedMist.afstand(e);
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterSky e) -> {
            if (actief()) {
                SfeerLucht.teken(e);
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentBlocks e) -> {
            if (actief()) {
                SfeerWereld.teken(e);
            }
        });
    }

    private static boolean actief() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null && Vadsrijk.in(mc.level) && SfeerConfig.aan();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != vorig) {
            vorig = mc.level;
            GebiedMist.wis();
            Lichten.wis();
            SfeerDeeltjes.wis();
            Momenten.wis();
        }
        if (!actief() || mc.isPaused()) {
            return;
        }
        GebiedMist.tick(mc);
        Lichten.tick(mc);
        SfeerWereld.tick(mc);
        SfeerDeeltjes.tick(mc);
    }
}
