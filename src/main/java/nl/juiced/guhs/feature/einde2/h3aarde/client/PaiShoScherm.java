package nl.juiced.guhs.feature.einde2.h3aarde.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.h3aarde.H3Payloads;
import nl.juiced.guhs.feature.einde2.h3aarde.PaiSho;

/**
 * The Pai Sho board (Guh Pai Sho: put a tile on a free square; first with a lotus wins, {@link PaiSho}). One screen for the
 * story game with Miroh and for a placed table: the server sends the board ({@link H3Payloads.Stand}) and every click goes
 * back as a {@link H3Payloads.Zet}. The picture: textures/gui/einde2_h3_aarde/pai_sho.png (tools/features/einde2_h3_aarde.py):
 * the board 176 x 176 at (0, 0), the tiles 28 x 28 at (176, 0) ('a': the white lotus), (176, 28) ('b': the red one),
 * (176, 56) (a winning tile's glow).
 */
public class PaiShoScherm extends Screen {
    private static final Identifier TEX = Guhs.id("textures/gui/einde2_h3_aarde/pai_sho.png");
    private static final int TEX_W = 256, TEX_H = 256, BORD = 176, TEGEL = 28, RAND = 18, VAK = 28;
    private static final int TEXT = 0xFFFFF0DC, GOUD = 0xFFF2C14E;

    private H3Payloads.Stand stand;
    private int left, top;
    private int[] lotus = new int[0];

    private PaiShoScherm(H3Payloads.Stand stand) {
        super(Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.titel"));
        this.stand = stand;
        this.lotus = PaiSho.winnendeLotus(stand.bord());
    }

    /** (H3Payloads.Stand) open the board, or refresh it when it is already open. */
    public static void toon(H3Payloads.Stand s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PaiShoScherm open && open.stand.spel().equals(s.spel()) && open.stand.tafel().equals(s.tafel())) {
            open.stand = s;
            open.lotus = PaiSho.winnendeLotus(s.bord());
            open.rebuildWidgets();
            return;
        }
        mc.setScreen(new PaiShoScherm(s));
    }

    private void stuur(int cel) {
        ClientPacketDistributor.sendToServer(new H3Payloads.Zet(stand.spel(), stand.tafel(), cel));
    }

    @Override
    protected void init() {
        left = (width - BORD - 140) / 2;
        top = (height - BORD) / 2;
        if (stand.status() == H3Payloads.KLAAR && H3Payloads.TAFEL.equals(stand.spel())) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.nieuw"), b -> stuur(H3Payloads.NIEUW))
                    .bounds(left + BORD + 12, top + BORD - 48, 124, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.sluit"), b -> onClose())
                .bounds(left + BORD + 12, top + BORD - 22, 124, 20).build());
    }

    private static Component naam(String n) {
        return n.startsWith("#") ? Component.translatable(n.substring(1)) : Component.literal(n);
    }

    private boolean mijnBeurt() {
        char beurt = PaiSho.beurt(stand.bord());
        return stand.status() == H3Payloads.BEZIG && ((beurt == PaiSho.A && stand.jij() == 1) || (beurt == PaiSho.B && stand.jij() == 2));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 6, top - 6, left + BORD + 146, top + BORD + 6, 0xF0281A12);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, left, top, 0, 0, BORD, BORD, BORD, BORD, TEX_W, TEX_H);
        String bord = stand.bord();
        int hover = cel(mouseX, mouseY);
        for (int i = 0; i < bord.length(); i++) {
            int x = left + RAND + (i % PaiSho.N) * VAK, y = top + RAND + (i / PaiSho.N) * VAK;
            char c = bord.charAt(i);
            for (int w : lotus) {
                if (w == i) {
                    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, x, y, BORD, 56, TEGEL, TEGEL, TEGEL, TEGEL, TEX_W, TEX_H);
                }
            }
            if (c == PaiSho.A || c == PaiSho.B) {
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, x, y, BORD, c == PaiSho.A ? 0 : 28, TEGEL, TEGEL, TEGEL, TEGEL, TEX_W, TEX_H);
            } else if (i == hover && mijnBeurt()) {
                g.fill(x + 4, y + 4, x + VAK - 4, y + VAK - 4, 0x60FFFFFF);
            }
        }
        int tx = left + BORD + 12, ty = top + 4;
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), tx, ty, GOUD, false);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, tx, ty + 16, BORD, 0, 14, 14, TEGEL, TEGEL, TEX_W, TEX_H);
        g.text(font, naam(stand.naamA()), tx + 18, ty + 19, TEXT, false);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, tx, ty + 34, BORD, 28, 14, 14, TEGEL, TEGEL, TEX_W, TEX_H);
        g.text(font, naam(stand.naamB()), tx + 18, ty + 37, TEXT, false);
        Component regel;
        if (stand.status() == H3Payloads.KLAAR) {
            regel = Component.translatable(stand.uitslag().isEmpty() ? "gui.guhs.einde2_h3_aarde.pai_sho.gelijk" : stand.uitslag()).withStyle(ChatFormatting.GOLD);
        } else if (stand.status() == H3Payloads.WACHT) {
            regel = Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.wacht_kort");
        } else if (mijnBeurt()) {
            regel = Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.jij").withStyle(ChatFormatting.GREEN);
        } else if (stand.jij() == 0) {
            regel = Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.kijk");
        } else {
            regel = Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.denkt", naam(PaiSho.beurt(bord) == PaiSho.A ? stand.naamA() : stand.naamB()));
        }
        int y = ty + 60;
        for (var r : font.split(regel, 124)) {
            g.text(font, r, tx, y, TEXT, false);
            y += 10;
        }
        y += 6;
        for (var r : font.split(Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.regels").withStyle(ChatFormatting.GRAY), 124)) {
            if (y > top + BORD - 56) {
                break;
            }
            g.text(font, r, tx, y, 0xFFB8A890, false);
            y += 9;
        }
    }

    /** The square under the mouse (-1: none). */
    private int cel(double mx, double my) {
        int x = (int) Math.floor((mx - left - RAND) / VAK), y = (int) Math.floor((my - top - RAND) / VAK);
        return x < 0 || y < 0 || x >= PaiSho.N || y >= PaiSho.N ? -1 : PaiSho.cel(x, y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int c = cel(event.x(), event.y());
        if (event.button() == 0 && c >= 0 && mijnBeurt() && stand.bord().charAt(c) == PaiSho.LEEG) {
            stuur(c);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void removed() {
        stuur(H3Payloads.SLUIT);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
