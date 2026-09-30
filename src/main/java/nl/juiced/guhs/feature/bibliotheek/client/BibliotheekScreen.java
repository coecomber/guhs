package nl.juiced.guhs.feature.bibliotheek.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.bibliotheek.BibliotheekPayloads;
import nl.juiced.guhs.feature.bibliotheek.Bibliothecaris;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;

/**
 * The Bibliothecaris' screen: your guh books as a row of book spines on a shelf (coloured: in your collection; an outline
 * in its colour: read on a lectern; grey: not read yet), the shop, and the Guhkwis: a question with three answer buttons.
 * The books themselves lie on the lecterns in the reading room.
 */
public class BibliotheekScreen extends Screen {
    private static final int W = 320, H = 226;
    private static final int SPINE_W = 18, SPINE_H = 38, SHELF_Y = 40;
    private static final String NEWLINE = String.valueOf('\n');
    private final int npcId;
    private final CompoundTag data;
    private int left, top;
    /** Ticks since the screen opened (for the quiz countdown). */
    private int ticks;
    private boolean refreshed;

    public BibliotheekScreen(BibliotheekPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.bibliothecaris"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new BibliotheekPayloads.Action(npcId, action));
    }

    private boolean has(Guhboek book) {
        return (data.getIntOr("Books", 0) & book.bit()) != 0;
    }

    private boolean read(Guhboek book) {
        return (data.getIntOr("Read", 0) & book.bit()) != 0;
    }

    private int spineX(int i) {
        return left + (W - Guhboek.values().length * (SPINE_W + 2)) / 2 + i * (SPINE_W + 2);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        // the spines: invisible buttons, for the tooltip
        for (Guhboek book : Guhboek.values()) {
            Component tip = has(book) ? book.title().copy().append(NEWLINE).append(Component.translatable("gui.guhs.bieb.spine.have")
                    .withStyle(ChatFormatting.GRAY))
                    : read(book) ? book.title().copy().append(NEWLINE).append(Component.translatable(
                            (data.getIntOr("Taken", 0) & book.bit()) != 0 ? "gui.guhs.bieb.spine.read" : "gui.guhs.bieb.spine.take")
                            .withStyle(ChatFormatting.GRAY))
                    : Component.translatable(book.secret() ? "gui.guhs.bieb.missing_secret" : "gui.guhs.bieb.missing");
            Button spine = Button.builder(Component.empty(), b -> {
            }).bounds(spineX(book.ordinal()), top + SHELF_Y, SPINE_W, SPINE_H).tooltip(Tooltip.create(tip)).build();
            spine.setAlpha(0f);
            addRenderableWidget(spine);
        }
        int y = top + SHELF_Y + SPINE_H + 22;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bieb.shop"), b -> send(Bibliothecaris.SHOP))
                .bounds(left + W / 2 - 80, y, 160, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.guhs.bieb.shop.tooltip"))).build());
        // the quiz: three answers in the shuffled order
        if (data.contains("Question")) {
            int question = data.getIntOr("Question", 0);
            int[] order = Bibliothecaris.ORDERS[Math.floorMod(data.getIntOr("Order", 0), Bibliothecaris.ORDERS.length)];
            int bw = (W - 40 - 8) / 3;
            for (int i = 0; i < 3; i++) {
                int position = i;
                addRenderableWidget(Button.builder(Component.translatable(Guhboek.answerKey(question, order[i])),
                                b -> send(Bibliothecaris.ANSWER + position))
                        .bounds(left + 20 + i * (bw + 4), top + H - 56, bw, 20)
                        .tooltip(Tooltip.create(Component.translatable(Guhboek.answerKey(question, order[i])))).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W / 2 - 40, top + H - 28, 80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFF7B6CB);
        g.fill(left, top, left + W, top + H, 0xE8301A26);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 8, 0xFFFFE6EE);
        int books = Integer.bitCount(data.getIntOr("Books", 0));
        g.centeredText(font, Component.translatable("gui.guhs.bieb.books", books, Guhboek.values().length).getString() + "     "
                + Component.translatable("gui.guhs.bieb.bonnen", data.getIntOr("Bonnen", 0)).getString(), width / 2, top + 22, 0xFFFFD27A);
        // the shelf with the book spines: coloured when you have it, a dusty grey outline when you don't
        int shelfTop = top + SHELF_Y;
        g.fill(left + 14, shelfTop + SPINE_H, left + W - 14, shelfTop + SPINE_H + 5, 0xFF7A4A3A);
        g.fill(left + 14, shelfTop + SPINE_H + 5, left + W - 14, shelfTop + SPINE_H + 7, 0xFF4E2E24);
        for (Guhboek book : Guhboek.values()) {
            int x = spineX(book.ordinal());
            int h = SPINE_H - (book.ordinal() * 7 % 5);                    // books of slightly different heights
            int y = shelfTop + SPINE_H - h;
            if (has(book)) {
                int c = 0xFF000000 | book.colour;
                g.fill(x, y, x + SPINE_W, y + h, c);
                g.fill(x, y + 5, x + SPINE_W, y + 7, 0xFFF5C43C);              // golden bands
                g.fill(x, y + h - 7, x + SPINE_W, y + h - 5, 0xFFF5C43C);
                g.centeredText(font, String.valueOf(book.ordinal() + 1), x + SPINE_W / 2, y + h / 2 - 4,
                        book.secret() ? 0xFFE0B0FF : 0xFFFFFFFF);
            } else if (read(book)) {
                g.outline(x, y, SPINE_W, h, 0xFF000000 | book.colour);
                g.centeredText(font, String.valueOf(book.ordinal() + 1), x + SPINE_W / 2, y + h / 2 - 4, 0xFF000000 | book.colour);
            } else {
                g.outline(x, y, SPINE_W, h, 0xFF6A5060);
                g.centeredText(font, "?", x + SPINE_W / 2, y + h / 2 - 4, 0xFF6A5060);
            }
        }
        g.centeredText(font, Component.translatable("gui.guhs.bieb.lessenaars"), width / 2, shelfTop + SPINE_H + 10, 0xFFB090A0);
        // the quiz
        int qy = top + SHELF_Y + SPINE_H + 50;
        g.centeredText(font, Component.translatable("gui.guhs.bieb.kwis").withStyle(ChatFormatting.BOLD), width / 2, qy, 0xFFFFD27A);
        Component text;
        if (data.contains("Question")) {
            int question = data.getIntOr("Question", 0);
            text = Component.translatable(Guhboek.questionKey(question)).append(" ").append(
                    Component.translatable("gui.guhs.bieb.kwis.about", Guhboek.bookOfQuestion(question).title()).withStyle(ChatFormatting.GRAY));
        } else if (data.contains("QuizWait")) {
            text = Component.translatable("gui.guhs.bieb.kwis.wait", Math.max(1, data.getIntOr("QuizWait", 0) - ticks / 20));
        } else {
            text = Component.translatable("gui.guhs.bieb.kwis.none");
        }
        int y = qy + 12;
        for (var line : font.split(text, W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFFFE6EE);
            y += 10;
        }
        g.text(font, Component.translatable("gui.guhs.bieb.kwis.score", data.getIntOr("Right", 0), Guhboek.questionCount()),
                left + 8, top + H - 12, 0xFFB090A0);
    }

    /** When the quiz wait is over, ask the Bibliothecaris for the next question (the screen comes back with it). */
    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (!refreshed && data.contains("QuizWait") && ticks >= data.getIntOr("QuizWait", 0) * 20 + 5) {
            refreshed = true;
            send(Bibliothecaris.REFRESH);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
