package nl.juiced.guhs.feature.maagwand.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.maagwand.Maagwand;

/**
 * The Guhtong: a living tongue hanging next to the survival inventory, its plasma rising with the count and guhlichaampjes
 * swimming in it (more and livelier per layer). With the recipe book open it hangs left of the book; where there is no room it
 * is hidden, never over the panel. Hover: layer, count, training food, bonus, and a raster of all locked food.
 */
public final class MaagwandClient {
    private static final Identifier TONG = Guhs.id("textures/gui/maagwand/tong.png");
    private static final Identifier CELLEN = Guhs.id("textures/gui/maagwand/cellen.png");
    /** tong.png: one column per layer, three rows: the tongue, its plasma, its rim. */
    private static final int W = 24, H = 74, RIJEN = 3, LAGEN = Maagwand.MAX;
    private static final int CEL = 7, CEL_SOORTEN = LAGEN + 1;
    private static final int RAND_BOVEN = 6, MARGE_BOVEN = 6;
    private static final int[] AANTAL = {5, 9, 14, 20, 30};
    private static final int BOEK_BREED = 147, BOEK_VERSCHUIF = 86, BOEK_SMAL = 379, TIP_BREED = 170;

    private MaagwandClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(MaagwandClient::fabrieken);
        NeoForge.EVENT_BUS.addListener(MaagwandClient::tekenen);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            Maagwand.klantLaag = Maagwand.MIN;
            Maagwand.klantTeller = 0;
        });
    }

    private static void fabrieken(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(MaagwandTooltip.class, MaagwandTooltip.Klant::new);
    }

    /** The left edge of the tongue, or -1: no room. */
    static int linkerRand(InventoryScreen s) {
        boolean boek = s.recipeBookComponent.isVisible();
        if (boek && s.width < BOEK_SMAL) {
            return -1;                  // the book lies over the panel
        }
        int rand = boek ? (s.width - BOEK_BREED) / 2 - BOEK_VERSCHUIF : s.getGuiLeft();
        return rand >= W ? rand - W : -1;
    }

    private static void tekenen(ScreenEvent.Render.Background event) {
        if (!(event.getScreen() instanceof InventoryScreen s)) {
            return;
        }
        int x = linkerRand(s);
        if (x < 0) {
            return;
        }
        int y = s.getGuiTop() + MARGE_BOVEN;
        GuiGraphicsExtractor g = event.getGuiGraphics();
        tong(g, x, y, Maagwand.klantLaag, Maagwand.klantTeller);
        int mx = event.getMouseX(), my = event.getMouseY();
        if (mx >= x && mx < x + W && my >= y && my < y + H) {
            hover(g, mx, my, Maagwand.klantLaag, Maagwand.klantTeller);
        }
    }

    private static void blok(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, TONG, x, y, u, v, w, h, W * LAGEN, H * RIJEN);
    }

    private static float vul(int laag, int teller) {
        Maagwand.Stap stap = Maagwand.stapNaar(laag + 1);
        return stap == null ? 1f : Math.max(0.25f, teller / (float) stap.doel());
    }

    private static void tong(GuiGraphicsExtractor g, int x, int y, int laag, int teller) {
        int kol = (laag - 1) * W;
        float t = (System.currentTimeMillis() % 3_600_000L) / 1000f;
        blok(g, x, y, kol, 0, W, H);
        int top = RAND_BOVEN + (int) ((1 - vul(laag, teller)) * (H - 16));
        for (int c = 0; c < W; c++) {             // the plasma, with a little wave on top
            int cy = top + Math.round(0.7f * Mth.sin(c + t * 4));
            blok(g, x + c, y + cy, kol + c, H + cy, 1, H - cy);
        }
        cellen(g, x, y, laag, top, t);
        blok(g, x, y, kol, 2 * H, W, H);
        for (int i = 0; i < LAGEN; i++) {         // five little dots: your layer
            g.fill(x + 3, y + H - 19 - i * 4, x + 6, y + H - 17 - i * 4, i < laag ? 0xFFFFD45A : 0x66501828);
        }
    }

    private static void cellen(GuiGraphicsExtractor g, int x, int y, int laag, int top, float t) {
        int boven = top + 2, onder = H - 13;
        float tempo = 0.6f + laag * 0.25f;
        for (int i = 0; i < AANTAL[laag - 1]; i++) {
            float fx = (i * 0.618f) % 1, fy = (i * 0.382f + 0.13f) % 1;
            int cy = boven + Math.round(Math.max(0, onder - boven) * (fy + 0.08f * Mth.sin(t * tempo + i)) * 0.92f);
            int smal = cy > H - 22 ? 3 : 0;      // the tip of the tongue is narrower
            int cx = 3 + smal + Math.round((W - 6 - CEL - 2 * smal) * (0.5f + 0.5f * Mth.sin(t * tempo * 0.6f + fx * 6.28f)));
            boolean goud = laag == Maagwand.MAX && i % 4 == 0;
            int soort = goud ? LAGEN : laag - 1;
            g.blit(RenderPipelines.GUI_TEXTURED, CELLEN, x + cx, y + cy, soort * CEL, 0, CEL, CEL, CEL * CEL_SOORTEN, CEL);
        }
        if (laag == Maagwand.MAX) {               // twinkling
            for (int i = 0; i < 3; i++) {
                if (Mth.sin(t * 2 + i * 2) > 0.4f) {
                    int sx = x + 6 + i * 5, sy = y + 16 + i * 17;
                    g.fill(sx - 1, sy, sx + 2, sy + 1, 0xFFFFF0AA);
                    g.fill(sx, sy - 1, sx + 1, sy + 2, 0xFFFFF0AA);
                }
            }
        }
    }

    private static void regel(List<Component> regels, Font font, Component tekst, Style stijl) {
        font.getSplitter().splitLines(tekst, TIP_BREED, stijl).forEach(r -> regels.add(Component.literal(r.getString()).withStyle(stijl)));
    }

    private static void hover(GuiGraphicsExtractor g, int mx, int my, int laag, int teller) {
        Font font = Minecraft.getInstance().font;
        Style grijs = Style.EMPTY.withColor(ChatFormatting.GRAY);
        List<Component> regels = new ArrayList<>();
        regels.add(Component.translatable("gui.guhs.maagwand.tip.titel", Component.translatable("gui.guhs.maagwand.laag." + laag), laag, Maagwand.MAX)
                .withStyle(ChatFormatting.GOLD));
        Maagwand.Stap stap = Maagwand.stapNaar(laag + 1);
        if (stap != null) {
            regels.add(Component.translatable("gui.guhs.maagwand.tip.teller", teller, stap.doel()).withStyle(ChatFormatting.LIGHT_PURPLE));
            Component namen = Maagwand.trainvoerNamen(stap);
            if (namen != null) {
                regel(regels, font, Component.translatable("gui.guhs.maagwand.tip.eet", namen), grijs);
            }
        }
        if (laag > Maagwand.MIN) {
            regel(regels, font, Component.translatable("gui.guhs.maagwand.tip.bonus", Component.translatable("gui.guhs.maagwand.bonus." + laag)), grijs);
        }
        List<ItemStack> opSlot = Maagwand.opSlot(laag);
        Optional<TooltipComponent> raster = Optional.empty();
        if (opSlot.isEmpty()) {
            regels.add(Component.translatable("gui.guhs.maagwand.tip.alles").withStyle(ChatFormatting.YELLOW));
        } else {
            regels.add(Component.translatable("gui.guhs.maagwand.tip.op_slot").withStyle(ChatFormatting.WHITE));
            raster = Optional.of(new MaagwandTooltip(opSlot));
        }
        g.setTooltipForNextFrame(font, regels, raster, mx, my);
    }
}
