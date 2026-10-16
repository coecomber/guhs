package nl.juiced.guhs.feature.einde2.trap5.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.einde2.trap5.T5Payloads;

/**
 * The order list of the Bestelguh (DESIGN 6): everything the Knutselmachines around the balies of this bank can make, as a
 * grid of icons with how many the bank holds; pick one, set an amount (1 / 8 / 64 steps), and "Bestellen". The answer
 * (accepted, or what is missing) comes as a chat line; the receipt follows when it is done.
 */
public class BestelScreen extends Screen {
    private static final int W = 236, H = 240, KOL = 10, VAK = 20, RASTER_Y = 30, RIJEN = 6;
    private T5Payloads.BestelLijst lijst;
    private int left, top, keuze = -1, aantal = 1, pagina;

    public BestelScreen(T5Payloads.BestelLijst lijst) {
        super(Component.translatable("gui.guhs.einde2_trap5_machines.bestel.titel"));
        this.lijst = lijst;
    }

    public BlockPos bron() {
        return lijst.bron();
    }

    public void update(T5Payloads.BestelLijst nieuw) {
        this.lijst = nieuw;
        rebuildWidgets();
    }

    private List<ItemStack> items() {
        return lijst.items();
    }

    private int perPagina() {
        return KOL * RIJEN;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int n = items().size(), pages = Math.max(1, (n + perPagina() - 1) / perPagina());
        pagina = Math.max(0, Math.min(pagina, pages - 1));
        for (int i = 0; i < perPagina() && pagina * perPagina() + i < n; i++) {
            int index = pagina * perPagina() + i;
            int x = left + 18 + (i % KOL) * VAK, y = top + RASTER_Y + (i / KOL) * VAK;
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                keuze = index;
                aantal = Math.max(1, items().get(index).getCount());
                rebuildWidgets();
            }).bounds(x, y, VAK - 2, VAK - 2).build());
        }
        int by = top + RASTER_Y + RIJEN * VAK + 6;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                pagina = (pagina + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left + 12, by, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                pagina = (pagina + 1) % pages;
                rebuildWidgets();
            }).bounds(left + 36, by, 20, 20).build());
        }
        if (keuze >= 0 && keuze < n) {
            int ay = by + 26;
            int[] stappen = {-64, -8, -1, 1, 8, 64};
            for (int s = 0; s < stappen.length; s++) {
                int stap = stappen[s];
                addRenderableWidget(Button.builder(Component.literal((stap > 0 ? "+" : "") + stap), b -> {
                    aantal = Math.max(1, Math.min(9999, aantal + stap));
                }).bounds(left + 12 + s * 30 + (s >= 3 ? 40 : 0), ay, 28, 18).build());
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_trap5_machines.bestel.knop").withStyle(ChatFormatting.GREEN), b -> {
                ClientPacketDistributor.sendToServer(new T5Payloads.Bestel(lijst.bron(), lijst.terminal(), items().get(keuze).copyWithCount(1), aantal));
                onClose();
            }).bounds(left + W - 92, by, 80, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 92, top + H - 26, 80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFE0A85A);
        g.fill(left, top, left + W, top + H, 0xF0302018);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 8, 0xFFFFE6C0);
        g.text(font, Component.translatable("gui.guhs.einde2_trap5_machines.bestel.uitleg", lijst.balies()), left + 12, top + 19, 0xFFD8C0A0);
        if (items().isEmpty()) {
            int y = top + RASTER_Y + 4;
            for (var regel : font.split(Component.translatable("gui.guhs.einde2_trap5_machines.bestel.leeg"), W - 24)) {
                g.text(font, regel, left + 12, y, 0xFFC8B090);
                y += 11;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int n = items().size();
        for (int i = 0; i < perPagina() && pagina * perPagina() + i < n; i++) {
            int index = pagina * perPagina() + i;
            int x = left + 18 + (i % KOL) * VAK, y = top + RASTER_Y + (i / KOL) * VAK;
            ItemStack s = items().get(index);
            if (index == keuze) {
                g.fill(x - 1, y - 1, x + VAK - 1, y + VAK - 1, 0x80FFD060);
            }
            g.item(s, x + 1, y + 1);
            if (mouseX >= x && mouseX < x + VAK - 2 && mouseY >= y && mouseY < y + VAK - 2) {
                int inBank = index < lijst.inBank().size() ? lijst.inBank().get(index) : -1;
                g.setTooltipForNextFrame(font, Component.translatable(inBank < 0 ? "gui.guhs.einde2_trap5_machines.bestel.item_onbekend"
                        : "gui.guhs.einde2_trap5_machines.bestel.item", s.getHoverName(), s.getCount(), Math.max(0, inBank)), mouseX, mouseY);
            }
        }
        if (keuze >= 0 && keuze < n) {
            int by = top + RASTER_Y + RIJEN * VAK + 6;
            g.text(font, Component.translatable("gui.guhs.einde2_trap5_machines.bestel.gekozen", aantal, items().get(keuze).getHoverName()),
                    left + 62, by + 6, 0xFFFFF0D0);
            g.text(font, Component.literal(String.valueOf(aantal)), left + 12 + 3 * 30 + 6, by + 31, 0xFFFFE080);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
