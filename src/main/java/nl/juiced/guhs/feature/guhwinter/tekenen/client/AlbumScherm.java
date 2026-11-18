package nl.juiced.guhs.feature.guhwinter.tekenen.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekenPayloads;

/**
 * The Kerstalbum: a book with one kerstboom drawing per spread (the drawing on the left page, its maker and the button on
 * the right). "Ophangen-versie pakken" gives that drawing as a hangable 2x2 item, once per drawing (the server keeps track and
 * sends the album back with the button greyed out). Flip with the arrows (or the arrow keys).
 */
public class AlbumScherm extends Screen {
    private static final int KAFT = 0xFFB23A48, KAFT_DONKER = 0xFF7E2433, GOUD = 0xFFE8B84A, BLAD = 0xFFFFF8E8, BLAD_SCHADUW = 0xFFEADCC2,
            INKT = 0xFF5A3A4E, GRIJS = 0xFF9A8290, GROEN = 0xFF2E7D4F;

    private List<TekenPayloads.AlbumRij> rijen;
    private int pagina;
    private boolean wacht;
    private int bw, bh, bx, by, plaat;

    public AlbumScherm(List<TekenPayloads.AlbumRij> rijen, int pagina) {
        super(Component.translatable("item.guhs.guhwinter_kerstalbum"));
        this.rijen = new ArrayList<>(rijen);
        this.pagina = pagina;
    }

    void ververs(List<TekenPayloads.AlbumRij> nieuw, int p) {
        rijen = new ArrayList<>(nieuw);
        pagina = Mth.clamp(p, 0, Math.max(0, rijen.size() - 1));
        wacht = false;
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        plaat = Mth.clamp(Math.min(height - 70, (width - 60) / 2 - 16), 96, 192);
        bw = 2 * (plaat + 28);
        bh = plaat + 50;
        bx = (width - bw) / 2;
        by = (height - bh) / 2;
        int rechts = bx + bw / 2;
        Button terug = Button.builder(Component.literal("<"), b -> blader(-1)).bounds(bx + 10, by + bh - 26, 20, 18).build();
        terug.active = pagina > 0;
        addRenderableWidget(terug);
        Button verder = Button.builder(Component.literal(">"), b -> blader(1)).bounds(bx + bw - 30, by + bh - 26, 20, 18).build();
        verder.active = pagina < rijen.size() - 1;
        addRenderableWidget(verder);
        if (!rijen.isEmpty()) {
            TekenPayloads.AlbumRij r = rijen.get(pagina);
            Button pak = Button.builder(Component.translatable(r.gepakt() ? "guhwinter.tekenen.album.al_gepakt" : "guhwinter.tekenen.album.pakken"),
                    b -> {
                        wacht = true;
                        b.active = false;
                        ClientPacketDistributor.sendToServer(new TekenPayloads.AlbumPak(r.id(), pagina));
                    }).bounds(rechts + 14, by + bh - 52, bw / 2 - 28, 20).build();
            pak.active = !r.gepakt() && !wacht;
            addRenderableWidget(pak);
        }
    }

    private void blader(int d) {
        int n = Mth.clamp(pagina + d, 0, Math.max(0, rijen.size() - 1));
        if (n != pagina) {
            pagina = n;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
            rebuildWidgets();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isLeft()) {
            blader(-1);
            return true;
        }
        if (event.isRight()) {
            blader(1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int midden = bx + bw / 2;
        // the cover, gold edge, the two pages and the fold
        g.fill(bx - 6, by - 6, bx + bw + 6, by + bh + 6, KAFT_DONKER);
        g.fill(bx - 5, by - 5, bx + bw + 5, by + bh + 5, KAFT);
        g.outline(bx - 3, by - 3, bw + 6, bh + 6, GOUD);
        g.fill(bx, by, bx + bw, by + bh, BLAD);
        g.fill(midden - 3, by, midden + 3, by + bh, BLAD_SCHADUW);
        g.fill(midden, by, midden + 1, by + bh, 0xFFD8C6A8);
        // holly in the corners
        hulst(g, bx + 8, by + 8);
        hulst(g, bx + bw - 16, by + 8);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), midden + bw / 4, by + 10, KAFT_DONKER);
        if (rijen.isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("guhwinter.tekenen.album.leeg"), bx + 16, by + 34, bw / 2 - 30, GRIJS, false);
        } else {
            TekenPayloads.AlbumRij r = rijen.get(pagina);
            int lx = bx + (bw / 2 - plaat) / 2, ly = by + 22;
            TekeningCache.Entry e = TekeningCache.get(r.id());
            g.fill(lx + 3, ly + 3, lx + plaat + 3, ly + plaat + 3, 0x33000000);
            g.blit(RenderPipelines.GUI_TEXTURED, e == null ? TekeningCache.LEEG : e.tex, lx, ly, 0f, 0f, plaat, plaat,
                    TekeningCache.B, TekeningCache.B, TekeningCache.B, TekeningCache.B);
            if (e == null) {
                g.centeredText(font, Component.literal("..."), lx + plaat / 2, ly + plaat / 2 - 4, GRIJS);
            }
            int rx = midden + 14, rw = bw / 2 - 28;
            g.textWithWordWrap(font, Component.translatable("guhwinter.tekenen.album.van", r.maker()), rx, by + 34, rw, INKT, false);
            g.text(font, Component.translatable("guhwinter.tekenen.album.pagina", pagina + 1, rijen.size()), rx, by + 58, GRIJS, false);
            g.textWithWordWrap(font, Component.translatable(r.gepakt() ? "guhwinter.tekenen.album.uitleg_gepakt" : "guhwinter.tekenen.album.uitleg"),
                    rx, by + 74, rw, r.gepakt() ? GROEN : GRIJS, false);
        }
        super.extractRenderState(g, mouseX, mouseY, a);
    }

    private static void hulst(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y + 2, x + 4, y + 4, GROEN);
        g.fill(x + 4, y, x + 8, y + 2, GROEN);
        g.fill(x + 3, y + 3, x + 5, y + 5, 0xFFD8233A);
        g.fill(x + 5, y + 4, x + 7, y + 6, 0xFFD8233A);
    }
}
