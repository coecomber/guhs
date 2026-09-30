package nl.juiced.guhs.feature.emotes.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.EmotePayload;
import nl.juiced.guhs.feature.samen.SamenBeloning;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The emote picker (Guh menu &gt; Emotes): per emote a star (favourite), "Nu" (once) and "Blijf" (keep doing it until
 * Stop), plus Stop, "no favourite" and Back. Labels follow the guh's synced state.
 * <p>
 * 2.10: fourteen emotes in two columns (so it still fits on a small screen). The three hartjes emotes (Hartjes,
 * Knuffeldansje, Bff-knuffel) are unlocks of the hartjes levels ({@link SamenBeloning}): until then they're shown with a
 * little heart and the level that unlocks them, and their buttons are off.
 */
public class EmotePickerScreen extends Screen {
    private static final int COLS = 2, ROWS = (Emote.values().length + COLS - 1) / COLS;
    private static final int COL_W = 196, GAP = 8, W = COLS * COL_W + (COLS + 1) * GAP;
    private static final int ROW_H = 19, BUTTON_H = 18, LIST_Y = 38, H = LIST_Y + ROW_H * ROWS + 4 + 20 + 8;
    /** Inside a column: star, name, "Nu", "Blijf". */
    private static final int STAR_W = 18, NAME_X = 21, NAME_W = 89, NOW_X = 112, NOW_W = 32, LOOP_X = 146, LOOP_W = 50;
    private static final int COLOR_PANEL = 0xE0301A26, COLOR_BORDER = 0xFFF7B6CB, COLOR_TEXT = 0xFFFFE6EE, COLOR_LOCKED = 0xFF9A7F8C;

    private final GuhEntity guh;
    private final Screen parent;
    private final List<Runnable> refreshers = new ArrayList<>();
    private int left, top;

    public EmotePickerScreen(GuhEntity guh, Screen parent) {
        super(Component.translatable("gui.guhs.menu.emotes"));
        this.guh = guh;
        this.parent = parent;
    }

    private void send(int action, int emote) {
        ClientPacketDistributor.sendToServer(new EmotePayload(guh.getId(), action, emote));
    }

    /** May the player pick this emote (hartjes emotes: only once unlocked)? */
    private boolean open(Emote emote) {
        return Emote.magVoor(emote, guh)   // 3.0: the ukelele only for the 626-guh
                && (minecraft == null || minecraft.player == null || SamenBeloning.heeft(minecraft.player, emote));
    }

    private int colX(int i) {
        return left + GAP + (i / ROWS) * (COL_W + GAP);
    }

    private int rowY(int i) {
        return top + LIST_Y + (i % ROWS) * ROW_H;
    }

    @Override
    protected void init() {
        refreshers.clear();
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        Emote[] emotes = Emote.values();
        for (int i = 0; i < emotes.length; i++) {
            Emote emote = emotes[i];
            int x = colX(i), y = rowY(i);
            Button star = addRenderableWidget(Button.builder(starLabel(emote), b ->
                            send(EmotePayload.FAVORITE, guh.emotes.favorite() == emote ? -1 : emote.ordinal()))
                    .bounds(x, y, STAR_W, BUTTON_H).tooltip(Tooltip.create(Component.translatable("gui.guhs.emotes.favourite.tooltip"))).build());
            Button now = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.emotes.now"), b -> send(EmotePayload.NOW, emote.ordinal()))
                    .bounds(x + NOW_X, y, NOW_W, BUTTON_H).tooltip(Tooltip.create(emote.description().copy().append("\n")
                            .append(Component.translatable("gui.guhs.emotes.now.tooltip").withStyle(ChatFormatting.GRAY)))).build());
            Button loop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.emotes.loop_kort"), b -> send(EmotePayload.LOOP, emote.ordinal()))
                    .bounds(x + LOOP_X, y, LOOP_W, BUTTON_H).tooltip(Tooltip.create(emote.description().copy().append("\n")
                            .append(Component.translatable("gui.guhs.emotes.loop.tooltip").withStyle(ChatFormatting.GRAY)))).build());
            refreshers.add(() -> {
                boolean o = open(emote);
                star.setMessage(starLabel(emote));
                star.active = o;
                now.active = o;
                loop.active = o;
                if (!o) {
                    Tooltip slot = Tooltip.create(opSlot(emote));
                    now.setTooltip(slot);
                    loop.setTooltip(slot);
                    star.setTooltip(slot);
                }
            });
        }
        int y = top + LIST_Y + ROWS * ROW_H + 4;
        Button stop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.emotes.stop"), b -> send(EmotePayload.STOP, 0))
                .bounds(left + GAP, y, 90, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.emotes.stop.tooltip"))).build());
        refreshers.add(() -> stop.active = guh.emotes.current() != null);
        Button clear = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.emotes.favourite.clear"), b -> send(EmotePayload.FAVORITE, -1))
                .bounds(left + GAP + 94, y, 150, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.emotes.favourite.clear.tooltip"))).build());
        refreshers.add(() -> clear.active = guh.emotes.favorite() != null);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                .bounds(left + W - GAP - 90, y, 90, 20).build());
        refreshers.forEach(Runnable::run);
    }

    /** "Op slot: word eerst ... met een guh". */
    private static Component opSlot(Emote emote) {
        if (Emote.alleenVoor(emote) != null) {   // 3.0: the ukelele: only the 626-guh
            return Component.translatable("gui.guhs.emotes.alleen_voor", Component.translatable("entity.guhs.guh." + Emote.alleenVoor(emote).id()));
        }
        BandNiveau n = SamenBeloning.niveau(emote);
        return Component.translatable("gui.guhs.emotes.op_slot", n == null ? Component.empty() : n.naam().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private Component starLabel(Emote emote) {
        boolean fav = guh.emotes.favorite() == emote;
        return Component.literal(fav ? "★" : "☆").withStyle(fav ? ChatFormatting.GOLD : ChatFormatting.GRAY);
    }

    @Override
    public void tick() {
        if (guh.isRemoved() || minecraft.player == null || minecraft.player.distanceToSqr(guh) > 24 * 24) {
            onClose();
            return;
        }
        refreshers.forEach(Runnable::run);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, COLOR_BORDER);
        g.fill(left, top, left + W, top + H, COLOR_PANEL);
        g.fill(left + GAP + COL_W + GAP / 2, top + LIST_Y - 2, left + GAP + COL_W + GAP / 2 + 1, top + LIST_Y + ROWS * ROW_H - 1, 0x40F7B6CB);
        g.centeredText(font, Component.translatable("gui.guhs.emotes.title", guh.getDisplayName()), left + W / 2, top + 8, COLOR_TEXT);
        Emote now = guh.emotes.current();
        Component doing = now == null ? Component.literal("-")
                : now.displayName().copy().append(guh.emotes.isLooping() ? Component.literal(" ∞") : Component.empty());
        g.text(font, Component.translatable("gui.guhs.emotes.doing", doing), left + GAP, top + 24, 0xFFD8B8C8);
        Emote fav = guh.emotes.favorite();
        Component favText = Component.translatable("gui.guhs.emotes.favourite",
                fav == null ? Component.translatable("gui.guhs.emotes.favourite.none") : fav.displayName());
        g.text(font, favText, left + W - GAP - font.width(favText), top + 24, 0xFFFFD27A);
        Emote[] emotes = Emote.values();
        for (int i = 0; i < emotes.length; i++) {
            Emote emote = emotes[i];
            int x = colX(i), y = rowY(i);
            boolean o = open(emote);
            Component naam = o ? emote.displayName() : Component.literal("♥ ").withStyle(ChatFormatting.LIGHT_PURPLE).append(emote.displayName());
            int kleur = !o ? COLOR_LOCKED : emote == fav ? 0xFFFFD27A : COLOR_TEXT;
            String tekst = font.plainSubstrByWidth(naam.getString(), NAME_W);
            g.text(font, tekst.length() < naam.getString().length() ? Component.literal(tekst) : naam, x + NAME_X, y + 5, kleur);
            if (mouseX >= x + NAME_X - 2 && mouseX < x + NOW_X - 2 && mouseY >= y && mouseY < y + BUTTON_H) {
                Component uitleg = o ? emote.description() : emote.description().copy().append("\n").append(opSlot(emote));
                setTooltipForNextRenderPass(font.split(uitleg, 200));
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
