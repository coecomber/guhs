package nl.juiced.guhs.feature.guhwinter.tekenen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.guhwinter.tekenen.Doel;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekenPayloads;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekenPixels;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekenenFeature;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekeningItem;

/** The client side of the tekenen slice: renderers, the tooltip preview, the editor and the album screens. */
public final class TekenenClient {
    private TekenenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(TekenenFeature.TEKENING_ENTITY.get(), TekeningRenderer::new);
            e.registerEntityRenderer(TekenenFeature.BOOMKAARTJE.get(), BoomKaartjeRenderer::new);
        });
        modBus.addListener((RegisterClientTooltipComponentFactoriesEvent e) -> e.register(TekeningItem.Voorbeeld.class, Voorbeeld::new));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            TekeningCache.wis();
            TekenScherm.vergeetWiel();
        });
    }

    /** guhs:guhwinter_teken_open */
    public static void open(TekenPayloads.Open p) {
        int[] px = p.pixels().length == 0 ? null : TekenPixels.pakUit(p.pixels());
        Minecraft.getInstance().setScreen(new TekenScherm(p.token(), Doel.van(p.doel()), px));
    }

    /** guhs:guhwinter_teken_antwoord */
    public static void antwoord(TekenPayloads.Antwoord p) {
        if (Minecraft.getInstance().screen instanceof TekenScherm s) {
            s.antwoord(p.token(), p.ok(), p.sleutel());
        }
    }

    /** guhs:guhwinter_album_open */
    public static void album(TekenPayloads.AlbumOpen p) {
        if (Minecraft.getInstance().screen instanceof AlbumScherm s) {
            s.ververs(p.rijen(), p.pagina());
        } else {
            Minecraft.getInstance().setScreen(new AlbumScherm(p.rijen(), p.pagina()));
        }
    }

    /** The tooltip preview of a drawing item. */
    static final class Voorbeeld implements ClientTooltipComponent {
        private static final int S = 48;
        private final java.util.UUID id;

        Voorbeeld(TekeningItem.Voorbeeld data) {
            this.id = data.id();
        }

        @Override
        public int getHeight(Font font) {
            return S + 4;
        }

        @Override
        public int getWidth(Font font) {
            return S;
        }

        @Override
        public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
            TekeningCache.Entry e = TekeningCache.get(id);
            if (e == null) {
                g.blit(RenderPipelines.GUI_TEXTURED, TekeningCache.LEEG, x, y + 2, 0f, 0f, S, S, TekeningCache.B, TekeningCache.B,
                        TekeningCache.B, TekeningCache.B);
                g.centeredText(font, Component.literal("..."), x + S / 2, y + S / 2 - 2, 0xFF9A7A8A);
                return;
            }
            g.blit(RenderPipelines.GUI_TEXTURED, e.tex, x, y + 2, 0f, 0f, S, S, TekeningCache.B, TekeningCache.B, TekeningCache.B, TekeningCache.B);
        }
    }
}
