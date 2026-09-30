package nl.juiced.guhs.feature.wereldleven.client;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.wereldleven.Koortje;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;
import nl.juiced.guhs.feature.wereldleven.WereldlevenPayloads;
import nl.juiced.guhs.feature.wereldleven.WereldlevenVoortgang;
import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The guh-xylofoon (and, without a xylofoon, the liedjesboekje to practise): eight coloured bars to click (or the keys
 * 1-8 / A S D F G H J K), and the liedjesboekje next to it with the six songs as coloured dots. Pick a song and the
 * next bar to play lights up. A whole song on a real xylofoon: the koortje sings along (Koortje).
 */
public class XylofoonScherm extends Screen {
    private static final int W = 330, H = 196;
    private static final int PANEL = 0xF0301A26, BORDER = 0xFFF7B6CB, TEXT = 0xFFFFE6EE;
    /** The colours of the bars (low to high), as on the block. */
    public static final int[] KLEUREN = {0xFFFF6F7D, 0xFFFFA25A, 0xFFFFD84E, 0xFF7ED957, 0xFF4FD1C5, 0xFF5B9DFF, 0xFFA879F2, 0xFFFF8AD0};
    private static final int[] TOETSEN = {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H,
            GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K};
    @Nullable
    private final BlockPos pos;
    private int left, top;
    private final int[] geraakt = new int[Koortje.NOTEN];
    private final Deque<Integer> gespeeld = new ArrayDeque<>();
    private long laatsteNoot;
    private int ticks;
    @Nullable
    private Koortje.Liedje gekozen = Koortje.Liedje.TOONLADDER;
    @Nullable
    private Koortje.Liedje banner;
    private int bannerTicks;

    public XylofoonScherm(@Nullable BlockPos pos) {
        super(Component.translatable(pos == null ? "gui.guhs.wereldleven.liedjesboekje" : "block.guhs.guh_xylofoon"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int y = top + 24;
        for (Koortje.Liedje l : Koortje.Liedje.values()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knus.liedjesboek." + l.id()), b -> kies(l))
                    .bounds(left + 176, y, 144, 16).build());
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffeldal.doei"), b -> onClose())
                .bounds(left + W - 10 - 60, top + H - 24, 60, 18).build());
    }

    private void kies(Koortje.Liedje l) {
        gekozen = l;
        gespeeld.clear();
    }

    // --- playing ---------------------------------------------------------------------------------------------------------------

    private void speel(int noot) {
        if (minecraft == null) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WereldlevenFeature.XYLOFOON.get(), Koortje.pitch(noot), 0.9f));
        geraakt[noot] = 6;
        long now = minecraft.level == null ? ticks : minecraft.level.getGameTime();
        if (now - laatsteNoot > Koortje.PAUZE) {
            gespeeld.clear();
        }
        laatsteNoot = now;
        gespeeld.addLast(noot);
        while (gespeeld.size() > 24) {
            gespeeld.removeFirst();
        }
        if (pos != null) {
            ClientPacketDistributor.sendToServer(new WereldlevenPayloads.Noot(pos, noot));
        }
        Koortje.Liedje af = Koortje.herken(gespeeld);
        if (af != null) {
            banner = af;
            bannerTicks = 60;
            gespeeld.clear();
        }
    }

    /** How far the last notes are into the chosen song (the next note to play lights up). */
    private int voortgang() {
        if (gekozen == null) {
            return 0;
        }
        int[] noten = gekozen.noten;
        Integer[] g = gespeeld.toArray(new Integer[0]);
        for (int n = Math.min(noten.length - 1, g.length); n > 0; n--) {
            boolean ok = true;
            for (int i = 0; i < n && ok; i++) {
                ok = g[g.length - n + i] == noten[i];
            }
            if (ok) {
                return n;
            }
        }
        return 0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        int bar = barAt(mx, my);
        if (bar >= 0 && button == 0) {
            speel(bar);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key(), scan = event.scancode(), modifiers = event.modifiers();
        for (int i = 0; i < Koortje.NOTEN; i++) {
            if (key == GLFW.GLFW_KEY_1 + i || key == TOETSEN[i]) {
                speel(i);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    // --- drawing ---------------------------------------------------------------------------------------------------------------

    private int barX(int i) {
        return left + 14 + i * 19;
    }

    private int barTop(int i) {
        return top + 40 + i * 6;
    }

    private int barBottom(int i) {
        return top + 150 - i * 6;
    }

    private int barAt(double mx, double my) {
        for (int i = 0; i < Koortje.NOTEN; i++) {
            if (mx >= barX(i) && mx < barX(i) + 16 && my >= barTop(i) && my < barBottom(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFFFB6D8, false);
        // the xylofoon: a pink frame with two rails, the bars on top
        g.fill(left + 8, top + 34, left + 166, top + 160, 0xFF5A2E44);
        g.fill(left + 10, top + 36, left + 164, top + 158, 0xFFF7B6CB);
        g.fill(left + 10, top + 58, left + 164, top + 61, 0xFFB86C8F);
        g.fill(left + 10, top + 132, left + 164, top + 135, 0xFFB86C8F);
        int next = gekozen == null ? -1 : gekozen.noten[Math.min(gekozen.noten.length - 1, voortgang())];
        for (int i = 0; i < Koortje.NOTEN; i++) {
            int x = barX(i), y0 = barTop(i), y1 = barBottom(i);
            int c = KLEUREN[i];
            if (geraakt[i] > 0) {
                c = lichter(c);
            }
            g.fill(x - 1, y0 - 1, x + 17, y1 + 1, 0xFF3A1C30);
            g.fill(x, y0 + (geraakt[i] > 0 ? 1 : 0), x + 16, y1, c);
            g.fill(x + 2, y0 + 3, x + 5, y1 - 3, 0x40FFFFFF);
            g.fill(x + 6, y0 + 8, x + 10, y0 + 12, 0xFF3A1C30);    // the nail
            g.fill(x + 6, y1 - 12, x + 10, y1 - 8, 0xFF3A1C30);
            if (i == next && (ticks / 6) % 2 == 0) {
                g.outline(x - 3, y0 - 3, 22, y1 - y0 + 6, 0xFFFFFFFF);
            }
            g.centeredText(font, String.valueOf(i + 1), x + 8, y1 + 2, TEXT);
        }
        // the little guh mallets (a knabbel on a stick)
        g.text(font, Component.translatable(pos == null ? "gui.guhs.wereldleven.oefenen" : "gui.guhs.wereldleven.xylofoon_tip"),
                left + 10, top + 176, 0xFFB8A0B0, false);
        // the liedjesboekje
        g.fill(left + 172, top + 20, left + 324, top + 172, 0xFFFFF4F8);
        g.text(font, Component.translatable("gui.guhs.wereldleven.liedjesboekje"), left + 176, top + 22 - 12, 0xFFFFB6D8, false);
        Set<String> ontdekt = KnusVoortgang.Client.ontdekt(WereldlevenVoortgang.LIEDJESBOEK);
        int y = top + 24;
        for (Koortje.Liedje l : Koortje.Liedje.values()) {
            boolean kies = l == gekozen;
            if (kies) {
                g.fill(left + 174, y - 2, left + 322, y + 24, 0xFFFFD6E8);
            }
            int dx = left + 178;
            for (int n : l.noten) {
                g.fill(dx, y + 18, dx + 7, y + 23, KLEUREN[n]);
                dx += 9;
            }
            if (ontdekt.contains(l.id())) {
                g.text(font, "✔", left + 312, y + 17, 0xFF3C9A5A, false);
            }
            y += 24;
        }
        if (banner != null && bannerTicks > 0) {
            Component b = Component.literal("♪ ").append(Component.translatable("gui.guhs.knus.liedjesboek." + banner.id())).append(" ♪");
            int bw = font.width(b) + 16;
            g.fill(left + 88 - bw / 2, top + 90, left + 88 + bw / 2, top + 108, 0xE0FFF4F8);
            g.centeredText(font, b, left + 88, top + 95, 0xFFD04A8A);
        }
    }

    private static int lichter(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 60), gr = Math.min(255, ((c >> 8) & 0xFF) + 60), b = Math.min(255, (c & 0xFF) + 60);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    @Override
    public void tick() {
        ticks++;
        for (int i = 0; i < geraakt.length; i++) {
            if (geraakt[i] > 0) {
                geraakt[i]--;
            }
        }
        if (bannerTicks > 0) {
            bannerTicks--;
        }
        if (pos != null && (minecraft == null || minecraft.player == null
                || minecraft.player.distanceToSqr(Vec3.atCenterOf(pos)) > Koortje.BEREIK * Koortje.BEREIK
                || !minecraft.player.level().getBlockState(pos).is(WereldlevenFeature.GUH_XYLOFOON.get()))) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
