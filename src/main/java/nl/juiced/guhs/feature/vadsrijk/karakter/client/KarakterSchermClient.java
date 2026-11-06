package nl.juiced.guhs.feature.vadsrijk.karakter.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.karakter.Badges;
import nl.juiced.guhs.feature.vadsrijk.karakter.KarakterPayloads;
import nl.juiced.guhs.feature.vadsrijk.karakter.Modus;

/**
 * T-KARAKTER / U-KARAKTER: the character screen ("Je ridders"), in the pink economie style. Three FIXED slots, each with its own
 * mode (U-KARAKTER, user decision): left Normaal, middle Knuffelmodus, right Eénvads. Every card shows its mode on top (name,
 * badge, a short line), then the knight (name, level, class, coins) with Spelen / Naam wijzigen, or, when empty, "Maak ...-ridder"
 * -> the creation panel (name field + the slot's mode explained, "Maken en spelen"). A retired Eénvads knight shows as a statue
 * with "Nieuwe Eénvads-ridder". On the first entry a welcome text sits above the cards; at the entrance "Verder als ...". The
 * server decides; when switching isn't allowed here, the reason is shown and Spelen is grey. Also a tab in the collection row.
 */
public class KarakterSchermClient extends EcoScherm {
    public static final String TAB = "ridders";
    private static final int KAART_W = 116, KAART_H = 156, MARGE = 8;
    /** -1 = the overview; else the slot being created / renamed. */
    private int maakSlot = -1, naamSlot = -1;
    @Nullable
    private EditBox naamVeld;

    public KarakterSchermClient(CompoundTag data) {
        super("karakter", Component.translatable("vadsrijk.karakter.scherm.titel"), data, List.of(), 3 * KAART_W + 4 * MARGE, 226);
    }

    @Override
    protected String collectieTab() {
        return TAB;
    }

    private boolean welkom() {
        return data.getBooleanOr("welkom", false);
    }

    /** Top of the cards (lower when the welcome text is shown). */
    private int kaartTop() {
        return top + (welkom() ? 44 : 24);
    }

    @Override
    protected void init() {
        super.init();
        String oud = naamVeld == null ? "" : naamVeld.getValue();
        naamVeld = new EditBox(font, left + 16, top + 44, 160, 16, Component.translatable("vadsrijk.karakter.scherm.naam"));
        naamVeld.setMaxLength(16);
        naamVeld.setHint(Component.translatable("vadsrijk.karakter.scherm.naam_hint").withStyle(ChatFormatting.GRAY));
        naamVeld.setValue(oud);
        addRenderableWidget(naamVeld);
        zichtbaar();
    }

    private void zichtbaar() {
        if (naamVeld != null) {
            naamVeld.visible = maakSlot >= 0 || naamSlot >= 0;
            naamVeld.active = naamVeld.visible;
            naamVeld.setFocused(naamVeld.visible);
            setFocused(naamVeld.visible ? naamVeld : null);
        }
    }

    @Override
    protected void opNieuweData() {
        maakSlot = naamSlot = -1;
        if (naamVeld != null) {
            naamVeld.setValue("");
        }
        zichtbaar();
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        if (!actie.equals("dicht")) {
            ClientPacketDistributor.sendToServer(new KarakterPayloads.Doe(actie, args));
        }
    }

    /** (U-KARAKTER) Enter in the name field = the main button (create and play / save). */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if ((event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) && naamVeld != null
                && naamVeld.visible && !naamVeld.getValue().isBlank()) {
            if (maakSlot >= 0) {
                stuurMaak(data.getBooleanOr("mag", false));
            } else if (naamSlot >= 0) {
                stuurNaam();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    private List<CompoundTag> slots() {
        List<CompoundTag> l = new ArrayList<>();
        var lijst = data.getListOrEmpty("slots");
        for (int i = 0; i < lijst.size(); i++) {
            l.add(lijst.getCompoundOrEmpty(i));
        }
        return l;
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        if (maakSlot >= 0) {
            maakPaneel(g, mx, my);
            return;
        }
        if (naamSlot >= 0) {
            naamPaneel(g, mx, my);
            return;
        }
        boolean mag = data.getBooleanOr("mag", false);
        int actief = data.getIntOr("actief", 0);
        if (welkom()) {
            int y = top + 22;
            for (var line : font.split(Component.translatable("vadsrijk.karakter.scherm.welkom"), w - 24)) {
                g.centeredText(font, line, left + w / 2, y, TEKST);
                y += 10;
            }
        }
        List<CompoundTag> slots = slots();
        for (int i = 0; i < slots.size(); i++) {
            kaart(g, mx, my, left + (w - 3 * KAART_W - 2 * MARGE) / 2 + i * (KAART_W + MARGE), kaartTop(), slots.get(i), i == actief, mag);   // V-INT: u-karakter's cards, centred (u-tabs: the menu frame can be wider)
        }
        int y = kaartTop() + KAART_H + 5;
        if (!mag) {
            g.centeredText(font, Component.translatable(data.getStringOr("waarom", "vadsrijk.karakter.niet.plek")), left + w / 2, y, ROOD);
        } else if (!welkom()) {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.uitleg_kort"), left + w / 2, y, ZACHT);
        }
        if (data.getBooleanOr("aankomst", false) && y + 12 + 16 <= top + h) {
            CompoundTag a = actief < slots.size() ? slots.get(actief) : new CompoundTag();
            knop(g, mx, my, left + w / 2 - 70, top + h - 20, 140, 16,
                    Component.translatable("vadsrijk.karakter.scherm.verder", a.getStringOr("naam", "?")), true, this::onClose);
        }
    }

    private void kaart(GuiGraphicsExtractor g, int mx, int my, int x, int y, CompoundTag s, boolean isActief, boolean mag) {
        int slot = s.getIntOr("slot", 0);
        Modus m = Modus.van(s.getStringOr("modus", Modus.vanSlot(slot).id));
        boolean rust = s.getBooleanOr("pensioen", false);
        g.fill(x, y, x + KAART_W, y + KAART_H, isActief ? 0xFFFF4FA8 : 0xFFF0B8D8);
        g.fill(x + 1, y + 1, x + KAART_W - 1, y + KAART_H - 1, isActief ? 0xFFFFE6F3 : 0xFFFFF6FB);
        // the slot's fixed mode on top: a coloured band with badge + name, a one-line hint below
        int band = m == Modus.EENVADS ? 0xFFFFD24A : m == Modus.KNUFFEL ? 0xFFFFB8E0 : 0xFFE8D8F0;
        g.fill(x + 1, y + 1, x + KAART_W - 1, y + 15, band);
        Component kop = Badges.badge(m == Modus.NORMAAL ? "" : m.id).append(Component.literal(m == Modus.NORMAAL ? "" : " "))
                .append(Component.translatable(m.naamKey()).withStyle(ChatFormatting.BOLD));
        g.centeredText(font, kop, x + KAART_W / 2, y + 4, TEKST);
        tip(mx, my, x, y, KAART_W, 15, List.of(Component.translatable(m.naamKey()).withStyle(ChatFormatting.BOLD), Component.translatable(m.uitlegKey())));
        int ty = y + 19;
        for (var line : font.split(Component.translatable("vadsrijk.karakter.modus." + m.id + ".regel"), KAART_W - 10)) {
            g.centeredText(font, line, x + KAART_W / 2, ty, ZACHT);
            ty += 9;
        }
        ty = y + 42;
        if (s.getBooleanOr("leeg", false)) {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.leeg").withStyle(ChatFormatting.ITALIC), x + KAART_W / 2, ty + 20, ZACHT);
            knop(g, mx, my, x + 6, y + KAART_H - 24, KAART_W - 12, 16, Component.translatable("vadsrijk.karakter.scherm.nieuw." + m.id), true,
                    () -> begin(slot));
            return;
        }
        Component naam = Badges.badge(s.getStringOr("badge", "")).append(Component.literal(" "))
                .append(Component.literal(s.getStringOr("naam", "?")).withStyle(ChatFormatting.BOLD));
        g.centeredText(font, naam, x + KAART_W / 2, ty, TEKST);
        ty += 13;
        if (rust) {
            for (var line : font.split(Component.translatable("vadsrijk.karakter.scherm.rust"), KAART_W - 12)) {
                g.centeredText(font, line, x + KAART_W / 2, ty, GOUD);
                ty += 10;
            }
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.level", s.getIntOr("glevel", 1)), x + KAART_W / 2, ty + 2, ZACHT);
            knop(g, mx, my, x + 6, y + KAART_H - 24, KAART_W - 12, 16, Component.translatable("vadsrijk.karakter.scherm.nieuw.eenvads_weer"), !isActief,
                    () -> begin(slot));
            return;
        }
        if (s.getBooleanOr("nieuw", false)) {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.nooit"), x + KAART_W / 2, ty, ZACHT);
        } else {
            g.text(font, Component.translatable("vadsrijk.karakter.scherm.level", s.getIntOr("glevel", 1)), x + 8, ty, TEKST, false);
            ty += 10;
            g.text(font, Component.translatable("vadsrijk.karakter.scherm.klasse", Component.translatable(nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.ofRidder(s.getStringOr("klasse", "knuffelridder")).naamKey()),
                    s.getIntOr("level", 1)), x + 8, ty, ZACHT, false);
            ty += 10;
            munten(g, x + 8, ty, s.getLongOr("munten", 0), false, false);
        }
        int by = y + KAART_H - 41;
        if (isActief) {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.actief").withStyle(ChatFormatting.BOLD), x + KAART_W / 2, by + 3, GROEN);
        } else {
            knop(g, mx, my, x + 6, by, KAART_W - 12, 16, Component.translatable("vadsrijk.karakter.scherm.spelen"), mag, () -> {
                CompoundTag a = new CompoundTag();
                a.putInt("slot", slot);
                doe("wissel", a);
            });
        }
        knop(g, mx, my, x + 6, by + 19, KAART_W - 12, 14, Component.translatable("vadsrijk.karakter.scherm.hernoem"), true, () -> {
            naamSlot = slot;
            if (naamVeld != null) {
                naamVeld.setValue(s.getStringOr("naam", ""));
            }
            zichtbaar();
        });
    }

    private void begin(int slot) {
        maakSlot = slot;
        if (naamVeld != null) {
            naamVeld.setValue("");
        }
        zichtbaar();
    }

    private void maakPaneel(GuiGraphicsExtractor g, int mx, int my) {
        Modus m = Modus.vanSlot(maakSlot);
        kop(g, Component.translatable("vadsrijk.karakter.scherm.maak." + m.id), left + 16, top + 26);
        g.text(font, Component.translatable("vadsrijk.karakter.scherm.naam"), left + 186, top + 48, ZACHT, false);
        int y = top + 72;
        g.text(font, Badges.badge(m == Modus.NORMAAL ? "" : m.id).append(Component.literal(m == Modus.NORMAAL ? "" : " "))
                .append(Component.translatable(m.naamKey()).withStyle(ChatFormatting.BOLD)), left + 16, y, m == Modus.EENVADS ? GOUD : TEKST, false);
        y += 13;
        for (var line : font.split(Component.translatable(m.uitlegKey()), w - 32)) {
            g.text(font, line, left + 16, y, TEKST, false);
            y += 10;
        }
        y += 4;
        for (var line : font.split(Component.translatable("vadsrijk.karakter.scherm.vast"), w - 32)) {
            g.text(font, line, left + 16, y, ZACHT, false);
            y += 10;
        }
        String naam = naamVeld == null ? "" : naamVeld.getValue().trim();
        boolean mag = data.getBooleanOr("mag", false);
        int by = top + h - 26;
        if (!mag) {
            g.text(font, Component.translatable(data.getStringOr("waarom", "vadsrijk.karakter.niet.plek")), left + 16, by - 12, ROOD, false);
        }
        knop(g, mx, my, left + 16, by, 120, 18, Component.translatable("vadsrijk.karakter.scherm.maak_speel"), !naam.isEmpty() && mag,
                () -> stuurMaak(true));
        knop(g, mx, my, left + 142, by, 100, 18, Component.translatable("vadsrijk.karakter.scherm.alleen_maak"), !naam.isEmpty(), () -> stuurMaak(false));
        knop(g, mx, my, left + w - 16 - 70, by, 70, 18, Component.translatable("vadsrijk.karakter.scherm.terug"), true, () -> {
            maakSlot = -1;
            zichtbaar();
        });
    }

    private void stuurMaak(boolean spelen) {
        CompoundTag a = new CompoundTag();
        a.putInt("slot", maakSlot);
        a.putString("naam", naamVeld == null ? "" : naamVeld.getValue());
        a.putBoolean("spelen", spelen);
        doe("maak", a);
    }

    private void stuurNaam() {
        CompoundTag a = new CompoundTag();
        a.putInt("slot", naamSlot);
        a.putString("naam", naamVeld == null ? "" : naamVeld.getValue());
        doe("naam", a);
    }

    private void naamPaneel(GuiGraphicsExtractor g, int mx, int my) {
        kop(g, Component.translatable("vadsrijk.karakter.scherm.hernoem"), left + 16, top + 26);
        String naam = naamVeld == null ? "" : naamVeld.getValue().trim();
        int by = top + 70;
        knop(g, mx, my, left + 16, by, 100, 18, Component.translatable("vadsrijk.karakter.scherm.opslaan"), !naam.isEmpty(), this::stuurNaam);
        knop(g, mx, my, left + 122, by, 70, 18, Component.translatable("vadsrijk.karakter.scherm.terug"), true, () -> {
            naamSlot = -1;
            zichtbaar();
        });
    }
}
