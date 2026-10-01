package nl.juiced.guhs.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.network.GuhActionPayload;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The Guh menu (hold right-click on your own guh): HP, size, rideable status, sit / teleport / gravity toggles,
 * behaviour (+ attack radius), a Sounds submenu, renaming and the wardrobe. Every button explains itself on hover.
 * <p>
 * A new simple toggle = one more {@link Entry} in {@link #toggles()} (plus an Action in GuhActionPayload and a
 * "&lt;key&gt;.tooltip" text).
 */
public class GuhScreen extends Screen {
    static final int PANEL_W = 340;
    static final int PANEL_H = 270;
    private static final int PREVIEW_W = 92;
    private static final int COL_W = 222;
    private static final int ROW_H = 20;
    private static final int GAP = 3;

    static final int COLOR_PANEL = 0xE0301A26;
    static final int COLOR_BORDER = 0xFFF7B6CB;
    static final int COLOR_TEXT = 0xFFFFE6EE;
    private static final int COLOR_HP_BG = 0xFF4A2030;
    private static final int COLOR_HP = 0xFFE8506E;

    private final GuhEntity guh;
    private final List<Runnable> refreshers = new ArrayList<>();
    private int left;
    private int top;
    private EditBox nameBox;

    /** A toggle button whose label follows the guh's (synced) state. */
    private record Entry(Function<GuhEntity, Component> label, String tooltip, GuhActionPayload.Action action) {
    }

    static net.minecraft.client.gui.components.Tooltip tip(String key) {
        return net.minecraft.client.gui.components.Tooltip.create(Component.translatable(key));
    }

    private static List<Entry> toggles() {
        return List.of(
                new Entry(g -> Component.translatable(g.isOrderedToSit() ? "gui.guhs.menu.stand" : "gui.guhs.menu.sit"),
                        "gui.guhs.menu.sit.tooltip", GuhActionPayload.Action.TOGGLE_SIT),
                new Entry(g -> Component.translatable("gui.guhs.menu.teleport", onOff(g.isTeleportEnabled())),
                        "gui.guhs.menu.teleport.tooltip", GuhActionPayload.Action.TOGGLE_TELEPORT),
                new Entry(g -> Component.translatable("gui.guhs.menu.gravity", onOff(g.isGravityEnabled())),
                        "gui.guhs.menu.gravity.tooltip", GuhActionPayload.Action.TOGGLE_GRAVITY),
                new Entry(g -> Component.translatable("gui.guhs.menu.wander", onOff(g.isWandering())),
                        "gui.guhs.menu.wander.tooltip", GuhActionPayload.Action.TOGGLE_WANDER)
        );
    }

    public GuhScreen(GuhEntity guh) {
        super(guh.getDisplayName());
        this.guh = guh;
    }

    static Component onOff(boolean on) {
        return Component.translatable(on ? "options.on" : "options.off");
    }

    static void send(GuhEntity guh, GuhActionPayload.Action action, int value, String text) {
        ClientPacketDistributor.sendToServer(new GuhActionPayload(guh.getId(), action, value, text));
    }

    @Override
    protected void init() {
        refreshers.clear();
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;
        int x = left + PREVIEW_W + 16;
        int y = top + 58;
        int half = (COL_W - GAP) / 2;

        // 1.2.0: the language of the Guhs texts (Auto / NL / EN), under the guh preview; applies at once
        addRenderableWidget(Button.builder(nl.juiced.guhs.client.GuhsTaal.label(), b -> nl.juiced.guhs.client.GuhsTaal.cycle())
                .bounds(left + 8, top + PANEL_H - 8 - ROW_H, PREVIEW_W, ROW_H).tooltip(tip("gui.guhs.menu.taal.tooltip")).build());

        // toggles, two per row, then the Sounds submenu in the next free spot
        List<Entry> entries = toggles();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            Button button = Button.builder(entry.label().apply(guh), b -> send(guh, entry.action(), 0, ""))
                    .bounds(x + (i % 2) * (half + GAP), y + (i / 2) * (ROW_H + GAP), half, ROW_H).tooltip(tip(entry.tooltip())).build();
            addRenderableWidget(button);
            refreshers.add(() -> button.setMessage(entry.label().apply(guh)));
        }
        int n = entries.size();
        // 2.10: the row under the toggles has three buttons: Sounds, the emote picker (feature/emotes) and a big cuddle
        int third = (COL_W - 2 * GAP) / 3, rowY = y + (n / 2) * (ROW_H + GAP);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.sounds"),
                        b -> minecraft.setScreen(new GuhSoundsScreen(guh, this)))
                .bounds(x, rowY, third, ROW_H)
                .tooltip(tip("gui.guhs.menu.sounds.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.emotes"),
                        b -> nl.juiced.guhs.feature.emotes.client.EmotesClient.openPicker(guh, this))
                .bounds(x + third + GAP, rowY, third, ROW_H)
                .tooltip(tip("gui.guhs.menu.emotes.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.knuffelen"),
                        b -> send(guh, GuhActionPayload.Action.KNUFFEL, 0, ""))
                .bounds(x + 2 * (third + GAP), rowY, COL_W - 2 * (third + GAP), ROW_H)
                .tooltip(tip("gui.guhs.menu.knuffelen.tooltip")).build());
        y = rowY + ROW_H + GAP;

        // behaviour
        Button behavior = Button.builder(behaviorLabel(), b -> {
            GuhEntity.Behavior next = GuhEntity.Behavior.values()[(guh.getBehavior().ordinal() + 1) % GuhEntity.Behavior.values().length];
            send(guh, GuhActionPayload.Action.SET_BEHAVIOR, next.ordinal(), "");
        }).bounds(x, y, COL_W, ROW_H).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.guhs.menu.behavior.tooltip"))).build();
        addRenderableWidget(behavior);
        refreshers.add(() -> behavior.setMessage(behaviorLabel()));
        y += ROW_H + GAP;

        // attack radius (only used when aggressive)
        RadiusSlider slider = new RadiusSlider(x, y, COL_W, ROW_H);
        slider.setTooltip(tip("gui.guhs.menu.radius.tooltip"));
        addRenderableWidget(slider);
        refreshers.add(() -> slider.active = guh.getBehavior() == GuhEntity.Behavior.AGGRESSIVE);
        y += ROW_H + GAP;

        // rename (no name tag needed)
        nameBox = new EditBox(font, x + 1, y + 1, COL_W - 50, ROW_H - 2, Component.translatable("gui.guhs.menu.name"));
        nameBox.setMaxLength(GuhActionPayload.MAX_NAME_LENGTH);
        nameBox.setValue(guh.hasCustomName() ? guh.getCustomName().getString() : "");
        nameBox.setHint(Component.translatable("gui.guhs.menu.name").withStyle(ChatFormatting.GRAY));
        addRenderableWidget(nameBox);
        nameBox.setTooltip(tip("gui.guhs.menu.name.tooltip"));
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.rename"), b -> rename())
                .bounds(x + COL_W - 46, y, 46, ROW_H).tooltip(tip("gui.guhs.menu.name.tooltip")).build());
        y += ROW_H + GAP;

        // clothes, armour and backpack: a chest-like wardrobe screen with your inventory; 2.10: its dagboekje next to it
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.wardrobe"),
                b -> send(guh, GuhActionPayload.Action.OPEN_WARDROBE, 0, "")).bounds(x, y, half, ROW_H)
                .tooltip(tip("gui.guhs.menu.wardrobe.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.dagboekje"),
                b -> send(guh, GuhActionPayload.Action.DAGBOEK, 0, "")).bounds(x + half + GAP, y, COL_W - half - GAP, ROW_H)
                .tooltip(tip("gui.guhs.menu.dagboekje.tooltip")).build());
        y += ROW_H + GAP;

        // 3.0: a story variant's own button (Baltoguh sniffs the way, the 626-guh plays the ukelele...), left of the bottom row
        var gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(guh);
        String speciaal = gedrag == null ? null : gedrag.speciaalKnop();
        boolean kamerKnop = nl.juiced.guhs.feature.guhkamer.Guhkamer.menuKnop(guh) != nl.juiced.guhs.feature.guhkamer.Guhkamer.MenuKnop.GEEN;
        if (speciaal != null) {
            int doneW = 60;
            int w = kamerKnop ? (COL_W - doneW - 2 * GAP) / 2 : COL_W - doneW - GAP;
            addRenderableWidget(Button.builder(Component.translatable(speciaal), b -> send(guh, GuhActionPayload.Action.VARIANT_SPECIAAL, 0, ""))
                    .bounds(x, y + GAP, w, ROW_H).tooltip(tip(speciaal + ".tooltip")).build());
            if (!kamerKnop) {
                addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                        .bounds(x + COL_W - doneW, y + GAP, doneW, ROW_H).build());
                refreshers.forEach(Runnable::run);
                return;
            }
            x += w + GAP;   // (the Guhkamer button and Klaar share the rest)
        }
        // 2.10.1: the Guhkamer (logeerkamer): send this guh there, or a guest comes out to you; next to "Klaar" when it applies
        if (kamerKnop) {
            int doneW = 60;
            int kamerW = (left + PREVIEW_W + 16 + COL_W) - x - doneW - GAP;
            Button kamer = Button.builder(guhkamerLabel(), b -> send(guh, nl.juiced.guhs.feature.guhkamer.Guhkamer.menuKnop(guh)
                            == nl.juiced.guhs.feature.guhkamer.Guhkamer.MenuKnop.UIT ? GuhActionPayload.Action.GUHKAMER_UIT
                            : GuhActionPayload.Action.GUHKAMER_LOGEREN, 0, ""))
                    .bounds(x, y + GAP, kamerW, ROW_H).tooltip(tip(guhkamerKey() + ".tooltip")).build();
            addRenderableWidget(kamer);
            String[] shown = {guhkamerKey()};
            refreshers.add(() -> {
                String key = guhkamerKey();
                if (!key.equals(shown[0])) {   // (the guh became a guest, or came out: label and tooltip follow)
                    shown[0] = key;
                    kamer.setMessage(guhkamerLabel());
                    kamer.setTooltip(tip(key + ".tooltip"));
                }
            });
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(x + kamerW + GAP, y + GAP, doneW, ROW_H).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x, y + GAP, COL_W, ROW_H).build());
        }
        refreshers.forEach(Runnable::run);
    }

    /** "gui.guhs.menu.guhkamer.logeren" or ".uit" (a guest of the Guhkamer). */
    private String guhkamerKey() {
        return nl.juiced.guhs.feature.guhkamer.Guhkamer.menuKnop(guh) == nl.juiced.guhs.feature.guhkamer.Guhkamer.MenuKnop.UIT
                ? "gui.guhs.menu.guhkamer.uit" : "gui.guhs.menu.guhkamer.logeren";
    }

    private Component guhkamerLabel() {
        return Component.translatable(guhkamerKey());
    }

    private void rename() {
        // 1.2.0: a story guh's name is translatable; pressing rename with the shown name unchanged must not freeze it
        Component current = guh.getCustomName();
        if (current != null && !(current.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents)
                && nameBox.getValue().strip().equals(current.getString())) {
            return;
        }
        send(guh, GuhActionPayload.Action.RENAME, 0, nameBox.getValue());
    }

    private Component behaviorLabel() {
        return Component.translatable("gui.guhs.menu.behavior",
                Component.translatable("gui.guhs.behavior." + guh.getBehavior().name().toLowerCase(Locale.ROOT)));
    }

    /** "Attack radius: 8 blocks", 1-20. */
    private class RadiusSlider extends AbstractSliderButton {
        RadiusSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), toValue(guh.getAttackRadius()));
            updateMessage();
        }

        private static double toValue(int radius) {
            return (radius - GuhEntity.MIN_ATTACK_RADIUS) / (double) (GuhEntity.MAX_ATTACK_RADIUS - GuhEntity.MIN_ATTACK_RADIUS);
        }

        private int radius() {
            return GuhEntity.MIN_ATTACK_RADIUS + (int) Math.round(value * (GuhEntity.MAX_ATTACK_RADIUS - GuhEntity.MIN_ATTACK_RADIUS));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.guhs.menu.radius", radius()));
        }

        @Override
        protected void applyValue() {
            send(guh, GuhActionPayload.Action.SET_ATTACK_RADIUS, radius(), "");
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameBox.isFocused() && event.isConfirmation()) { // enter
            rename();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        if (guh.isRemoved() || this.minecraft.player == null || this.minecraft.player.distanceToSqr(guh) > 24 * 24) {
            onClose();
            return;
        }
        refreshers.forEach(Runnable::run);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, COLOR_BORDER);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, COLOR_PANEL);

        Component size = Component.translatable("gui.guhs.menu.size", String.format(Locale.ROOT, "%.1f", guh.getGuhScale() * guh.getAgeScale() * 1.45f));
        int sw = font.width(size);
        g.text(font, size, left + PANEL_W - 10 - sw, top + 8, COLOR_BORDER);
        fitted(g, guh.getDisplayName(), left + 10, top + 8, PANEL_W - 30 - sw, COLOR_TEXT, mouseX, mouseY);
        Component personality = Component.translatable("gui.guhs.menu.personality", guh.getPersonality().displayName());
        int pw = font.width(personality);
        int pxp = left + PANEL_W - 10 - pw;
        g.text(font, personality, pxp, top + 20, 0xFFFFD27A);
        fitted(g, rideableText(), left + 10, top + 20, PANEL_W - 30 - pw, 0xFFD8B8C8, mouseX, mouseY);
        if (mouseX >= pxp && mouseX <= pxp + pw && mouseY >= top + 18 && mouseY <= top + 30) {
            g.setTooltipForNextFrame(font, personalityTooltip(), mouseX, mouseY);
        }

        float hp = guh.getHealth();
        float max = guh.getMaxHealth();
        int barX = left + 10;
        int barY = top + 34;
        int barW = PANEL_W - 20;
        g.fill(barX, barY, barX + barW, barY + 12, COLOR_HP_BG);
        g.fill(barX, barY, barX + (int) (barW * Mth.clamp(hp / max, 0f, 1f)), barY + 12, COLOR_HP);
        g.centeredText(font, Component.translatable("gui.guhs.menu.hp", Mth.ceil(hp), Mth.ceil(max)), barX + barW / 2, barY + 2, 0xFFFFFFFF);

        int px = left + 8;
        int py = top + 58;
        g.fill(px, py, px + PREVIEW_W, py + 150, 0x40FFFFFF);
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, px, py, px + PREVIEW_W, py + 150, 36, 0.0625f, mouseX, mouseY, guh);
    }

    /** Text that stops ("...") before `maxWidth`; hover it to read all of it. */
    private void fitted(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int colour, int mouseX, int mouseY) {
        if (font.width(text) <= maxWidth) {
            g.text(font, text, x, y, colour);
            return;
        }
        String cut = font.plainSubstrByWidth(text.getString(), maxWidth - font.width("...")) + "...";
        g.text(font, cut, x, y, colour);
        if (mouseX >= x && mouseX <= x + maxWidth && mouseY >= y - 2 && mouseY <= y + 10) {
            g.setTooltipForNextFrame(font, font.split(text, 240), mouseX, mouseY);
        }
    }

    /** All personalities and what they do, with this guh's own one highlighted. */
    private List<net.minecraft.util.FormattedCharSequence> personalityTooltip() {
        List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.guhs.menu.personality.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY).getVisualOrderText());
        for (nl.juiced.guhs.entity.GuhPersonality p : nl.juiced.guhs.entity.GuhPersonality.values()) {
            boolean mine = p == guh.getPersonality();
            Component line = Component.literal(mine ? "▶ " : "   ")
                    .append(p.displayName().copy().withStyle(mine ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.WHITE,
                            net.minecraft.ChatFormatting.BOLD))
                    .append(Component.literal(": "))
                    .append(p.description().copy().withStyle(mine ? net.minecraft.ChatFormatting.YELLOW : net.minecraft.ChatFormatting.GRAY));
            lines.addAll(font.split(line, 260));
        }
        return lines;
    }

    /** Can I ride this guh? (size, age and saddle) */
    private Component rideableText() {
        if (guh.isBaby()) {
            return Component.translatable("gui.guhs.menu.ride.baby");
        }
        if (guh.getGuhScale() < GuhEntity.RIDEABLE_SCALE) {
            return Component.translatable("gui.guhs.menu.ride.small", String.format(Locale.ROOT, "%.1f", GuhEntity.RIDEABLE_SCALE * 1.45f));
        }
        return Component.translatable(guh.isSaddled() ? "gui.guhs.menu.ride.yes" : "gui.guhs.menu.ride.saddle");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Sounds submenu: ambient sounds on/off and how often. */
    public static class GuhSoundsScreen extends Screen {
        private final GuhEntity guh;
        private final Screen parent;
        private Button toggle;
        private Button frequency;

        GuhSoundsScreen(GuhEntity guh, Screen parent) {
            super(Component.translatable("gui.guhs.menu.sounds"));
            this.guh = guh;
            this.parent = parent;
        }

        @Override
        protected void init() {
            int x = (width - 200) / 2;
            int y = (height - 100) / 2 + 20;
            toggle = addRenderableWidget(Button.builder(label(), b -> send(guh, GuhActionPayload.Action.TOGGLE_SOUNDS, 0, ""))
                    .bounds(x, y, 200, 20).tooltip(tip("gui.guhs.menu.sounds.ambient.tooltip")).build());
            frequency = addRenderableWidget(Button.builder(frequencyLabel(), b ->
                            send(guh, GuhActionPayload.Action.SET_SOUND_FREQUENCY, (guh.getSoundFrequency() + 1) % GuhEntity.SOUND_INTERVALS.length, ""))
                    .bounds(x, y + 24, 200, 20).tooltip(tip("gui.guhs.menu.sounds.frequency.tooltip")).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                    .bounds(x, y + 52, 200, 20).build());
        }

        private Component label() {
            return Component.translatable("gui.guhs.menu.sounds.ambient", onOff(guh.areSoundsEnabled()));
        }

        private Component frequencyLabel() {
            return Component.translatable("gui.guhs.menu.sounds.frequency", Component.translatable("gui.guhs.menu.sounds.frequency." + guh.getSoundFrequency()));
        }

        @Override
        public void tick() {
            toggle.setMessage(label());
            frequency.setMessage(frequencyLabel());
            frequency.active = guh.areSoundsEnabled();
            if (guh.isRemoved()) {
                onClose();
            }
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            super.extractBackground(g, mouseX, mouseY, partialTick);
            int x = (width - 220) / 2;
            int y = (height - 100) / 2 - 10;
            g.fill(x - 1, y - 1, x + 221, y + 121, COLOR_BORDER);
            g.fill(x, y, x + 220, y + 120, COLOR_PANEL);
            g.centeredText(font, Component.translatable("gui.guhs.menu.sounds.title", guh.getDisplayName()), width / 2, y + 10, COLOR_TEXT);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
