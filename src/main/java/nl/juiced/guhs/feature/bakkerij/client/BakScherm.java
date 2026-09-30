package nl.juiced.guhs.feature.bakkerij.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bakkerij.Bakken;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.bakkerij.BakkerijPayloads;
import nl.juiced.guhs.feature.bakkerij.Recept;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The baking screen of a knabbeloven: pick a dough, a shape and a topping (the screen says what it'll become), slide it
 * in, and take it out when the pointer is in the golden part of the bar (space or "Eruit!"). In Korstje's game the
 * customers' orders are on the right (click one to pick its recipe) and the pastry goes straight into your hand; at
 * your own oven the right side shows the ingredients the choice needs.
 */
public class BakScherm extends Screen {
    private static final int W = 344, H = 236;
    private static final Identifier KNOPPEN = Guhs.id("textures/gui/bakkerij_knoppen.png");
    private static final int TEX_W = 128, TEX_H = 64;
    /** The last choice (kept between screens). */
    private static int deeg, vorm = 0, topping;

    private final long oven;
    private CompoundTag data;
    private int left, top;
    private int tick;
    /** What's in the oven (-1: nothing), since which screen tick, and how long the whole bar takes. */
    private int bakt = -1, bakStart, bakTicks = 60;
    private boolean eruitGestuurd;
    private Component melding = Component.empty();
    private int meldingKleur = 0xFFFFE9C4;
    private int sluitOver = -1;
    private Button bakKnop;
    private final List<KeuzeKnop> keuzes = new ArrayList<>();

    public BakScherm(long oven, CompoundTag data) {
        super(Component.translatable(data.getBooleanOr("Spel", false) ? "gui.guhs.bakkerij.scherm.spel" : "gui.guhs.bakkerij.scherm.oven"));
        this.oven = oven;
        this.data = data;
        if (data.contains("Bakt")) {
            bakt = data.getIntOr("Bakt", 0);
            bakStart = -data.getIntOr("Al", 0);
            bakTicks = Math.max(1, data.getIntOr("BakTicks", 0));
        }
    }

    private boolean spel() {
        return data.getBooleanOr("Spel", false);
    }

    // --- layout ---------------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        keuzes.clear();
        for (Recept.Deeg d : Recept.Deeg.values()) {
            keuze(0, d.ordinal(), left + 12 + d.ordinal() * 26, top + 36, d.naam());
        }
        for (Recept.Vorm v : Recept.Vorm.values()) {
            keuze(1, v.ordinal(), left + 12 + v.ordinal() * 26, top + 78, v.naam());
        }
        for (Recept.Topping t : Recept.Topping.values()) {
            keuze(2, t.ordinal(), left + 12 + t.ordinal() * 26, top + 120, t.naam());
        }
        bakKnop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bakkerij.in_de_oven").withStyle(ChatFormatting.BOLD), b -> knop())
                .bounds(left + 12, top + H - 28, 170, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 82, top + H - 28, 70, 20).build());
        verversKnoppen();
    }

    private void keuze(int rij, int i, int x, int y, Component naam) {
        KeuzeKnop k = new KeuzeKnop(rij, i, x, y, naam);
        k.setTooltip(Tooltip.create(naam));
        keuzes.add(addRenderableWidget(k));
    }

    private Recept.Deeg d() {
        return Recept.Deeg.values()[Math.floorMod(deeg, Recept.Deeg.values().length)];
    }

    private Recept.Vorm v() {
        return Recept.Vorm.values()[Math.floorMod(vorm, Recept.Vorm.values().length)];
    }

    private Recept.Topping t() {
        return Recept.Topping.values()[Math.floorMod(topping, Recept.Topping.values().length)];
    }

    private void verversKnoppen() {
        if (bakKnop == null) {
            return;
        }
        if (bakt >= 0) {
            bakKnop.setMessage(Component.translatable("gui.guhs.bakkerij.eruit").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
            bakKnop.active = !eruitGestuurd;
        } else {
            bakKnop.setMessage(Component.translatable("gui.guhs.bakkerij.in_de_oven").withStyle(ChatFormatting.BOLD));
            Recept r = Recept.van(d(), v(), t());
            bakKnop.active = r != null && (spel() ? (r != Recept.FEESTTAART || data.getBooleanOr("Feest", false))
                    : r != Recept.FEESTTAART && Bakken.kan(data.getIntArray("Voorraad").orElse(new int[0]), d(), t()));
        }
        for (KeuzeKnop k : keuzes) {
            k.active = bakt < 0;
        }
    }

    // --- actions ----------------------------------------------------------------------------------------------------------

    private void knop() {
        if (bakt >= 0) {
            eruit();
        } else {
            melding = Component.empty();
            ClientPacketDistributor.sendToServer(new BakkerijPayloads.Bak(oven, BakkerijPayloads.IN_DE_OVEN, deeg, vorm, topping, 0));
        }
    }

    private void eruit() {
        if (bakt >= 0 && !eruitGestuurd) {
            eruitGestuurd = true;
            ClientPacketDistributor.sendToServer(new BakkerijPayloads.Bak(oven, BakkerijPayloads.ERUIT, 0, 0, 0, Math.max(0, tick - bakStart)));
            verversKnoppen();
        }
    }

    /** guhs:bakkerij_status from the server. */
    public void update(CompoundTag nieuw) {
        CompoundTag oud = data;
        data = nieuw;
        if (!data.contains("Oven")) {
            data.putLong("Oven", oud.getLongOr("Oven", 0L));
        }
        if (nieuw.contains("Nee")) {
            melding = Component.translatable(nieuw.getStringOr("Nee", ""));
            meldingKleur = 0xFFFF9A9A;
            bakt = -1;
        } else if (nieuw.contains("Bakt")) {
            bakt = nieuw.getIntOr("Bakt", 0);
            bakStart = tick - nieuw.getIntOr("Al", 0);
            bakTicks = Math.max(1, nieuw.getIntOr("BakTicks", 0));
            eruitGestuurd = false;
            melding = Component.translatable("gui.guhs.bakkerij.bakt", naam(bakt));
            meldingKleur = 0xFFFFE9C4;
        } else if (nieuw.getBooleanOr("Klaar", false)) {
            bakt = -1;
            eruitGestuurd = false;
            Recept r = Recept.byIndex(nieuw.getIntOr("Recept", 0));
            Recept.Kwaliteit k = Recept.Kwaliteit.byIndex(nieuw.getIntOr("Kwaliteit", 0));
            if (r != null && k != null) {
                melding = k == Recept.Kwaliteit.AANGEBRAND ? Component.translatable("gui.guhs.bakkerij.uit.aangebrand", r.naam())
                        : spel() ? Component.translatable("gui.guhs.bakkerij.uit.spel", r.naam(), k.naam())
                        : Component.translatable("gui.guhs.bakkerij.uit.oven", nieuw.getIntOr("Aantal", 0), r.naam(), k.naam());
                meldingKleur = k == Recept.Kwaliteit.PERFECT ? 0xFFFFE066 : k == Recept.Kwaliteit.AANGEBRAND ? 0xFFB0A0A0 : 0xFFB8F0C8;
                if (spel() && k != Recept.Kwaliteit.AANGEBRAND) {
                    sluitOver = 14;                     // (to the customer with it!)
                }
            }
        }
        verversKnoppen();
    }

    private static Component naam(int recept) {
        Recept r = Recept.byIndex(recept);
        return r == null ? Component.literal("?") : r.naam();
    }

    @Override
    public void tick() {
        super.tick();
        tick++;
        if (bakt >= 0 && !eruitGestuurd && (tick - bakStart) * 100 / bakTicks >= 100 + 6) {
            eruit();                                    // (it's burnt by now anyway)
        }
        if (sluitOver > 0 && --sluitOver == 0) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE && bakt >= 0) {
            eruit();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void removed() {
        eruit();                                        // (closing while it bakes: it comes out now)
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (spel() && bakt < 0) {
            List<CompoundTag> orders = bestellingen();
            for (int i = 0; i < orders.size(); i++) {
                int y = top + 40 + i * 34;
                if (mx >= left + 214 && mx < left + W - 10 && my >= y - 2 && my < y + 30) {
                    Recept r = Recept.byIndex(orders.get(i).getIntOr("Recept", 0));
                    if (r != null) {
                        deeg = r.deeg.ordinal();
                        vorm = r.vorm.ordinal();
                        topping = r.topping.ordinal();
                        verversKnoppen();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private List<CompoundTag> bestellingen() {
        List<CompoundTag> out = new ArrayList<>();
        for (Tag t : data.getListOrEmpty("Bestellingen")) {
            out.add((CompoundTag) t);
        }
        return out;
    }

    private Set<Integer> bekend() {
        Set<Integer> out = new HashSet<>();
        for (String s : data.getStringOr("Bekend", "").split(",")) {
            if (!s.isEmpty()) {
                try {
                    out.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    // --- drawing ----------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFE0A050);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFFFE2A8);
        g.fill(left, top, left + W, top + H, 0xEE3A2418);
        g.fill(left + 206, top + 22, left + 207, top + H - 64, 0x60FFE2A8);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 8, 0xFFFFE9C4);
        g.text(font, Component.translatable("gui.guhs.bakkerij.kies.deeg").append(": ").append(d().naam().copy().withStyle(ChatFormatting.YELLOW)),
                left + 12, top + 24, 0xFFF3DCC0);
        g.text(font, Component.translatable("gui.guhs.bakkerij.kies.vorm").append(": ").append(v().naam().copy().withStyle(ChatFormatting.YELLOW)),
                left + 12, top + 66, 0xFFF3DCC0);
        g.text(font, Component.translatable("gui.guhs.bakkerij.kies.topping").append(": ").append(t().naam().copy().withStyle(ChatFormatting.YELLOW)),
                left + 12, top + 108, 0xFFF3DCC0);
        // what it becomes
        Recept r = Recept.van(d(), v(), t());
        Component wordt;
        if (r == null) {
            wordt = Component.translatable("gui.guhs.bakkerij.wordt.niets").withStyle(ChatFormatting.GRAY);
        } else if (r == Recept.FEESTTAART) {
            wordt = Component.translatable(spel() && data.getBooleanOr("Feest", false) ? "gui.guhs.bakkerij.wordt.feesttaart" : "gui.guhs.bakkerij.wordt.geheim")
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
        } else if (bekend().contains(r.ordinal()) || spel()) {
            wordt = Component.translatable("gui.guhs.bakkerij.wordt", r.naam().copy().withStyle(ChatFormatting.GOLD));
        } else {
            wordt = Component.translatable("gui.guhs.bakkerij.wordt.nieuw").withStyle(ChatFormatting.AQUA);
        }
        g.text(font, wordt, left + 12, top + 150, 0xFFFFE9C4);
        if (r != null && r != Recept.FEESTTAART && (bekend().contains(r.ordinal()) || spel())) {
            g.item(new ItemStack(BakkerijFeature.bakje(r)), left + 180, top + 145);
        }
        if (spel()) {
            rechtsSpel(g);
        } else {
            rechtsOven(g, r);
        }
        ovenBalk(g, partialTick);
        g.centeredText(font, melding, width / 2, top + H - 42, meldingKleur);
    }

    /** The orders of the customers waiting at the counter (click one to pick its recipe). */
    private void rechtsSpel(GuiGraphicsExtractor g) {
        int x = left + 214;
        g.text(font, Component.translatable("gui.guhs.bakkerij.bestellingen").withStyle(ChatFormatting.BOLD), x, top + 24, 0xFFFFE9C4);
        List<CompoundTag> orders = bestellingen();
        if (orders.isEmpty()) {
            for (var line : font.split(Component.translatable("gui.guhs.bakkerij.geen_bestellingen"), W - 224)) {
                g.text(font, line, x, top + 42, 0xFFB0A090);
            }
        }
        for (int i = 0; i < orders.size() && i < 4; i++) {
            Recept r = Recept.byIndex(orders.get(i).getIntOr("Recept", 0));
            if (r == null) {
                continue;
            }
            int y = top + 40 + i * 34;
            g.fill(x - 2, y - 2, left + W - 10, y + 30, 0x40FFE2A8);
            g.item(new ItemStack(BakkerijFeature.bakje(r)), x, y);
            g.text(font, r.naam(), x + 19, y, r == Recept.FEESTTAART ? 0xFFFF9FD8 : 0xFFFFE9C4);
            icoon(g, 0, r.deeg.ordinal(), x + 19, y + 10, 10);
            icoon(g, 1, r.vorm.ordinal(), x + 31, y + 10, 10);
            icoon(g, 2, r.topping.ordinal(), x + 43, y + 10, 10);
            float geduld = orders.get(i).getIntOr("Geduld", 0) / 100f;
            int kleur = geduld > 0.5f ? 0xFF6CD66C : geduld > 0.25f ? 0xFFF2C94C : 0xFFE8555A;
            g.fill(x + 19, y + 23, x + 110, y + 26, 0xFF503040);
            g.fill(x + 19, y + 23, x + 19 + Math.round(91 * geduld), y + 26, kleur);
        }
        g.text(font, Component.translatable("gui.guhs.bakkerij.score", data.getIntOr("Score", 0), data.getIntOr("Combo", 0)), x, top + H - 60, 0xFFFFD27A);
    }

    /** What the choice needs from your pockets. */
    private void rechtsOven(GuiGraphicsExtractor g, Recept r) {
        int x = left + 214;
        g.text(font, Component.translatable("gui.guhs.bakkerij.nodig").withStyle(ChatFormatting.BOLD), x, top + 24, 0xFFFFE9C4);
        int[] voorraad = data.getIntArray("Voorraad").orElse(new int[0]);
        int y = top + 40;
        for (Map.Entry<Bakken.Nodig, Integer> e : Bakken.nodig(d(), t()).entrySet()) {
            int heb = voorraad.length > e.getKey().ordinal() ? voorraad[e.getKey().ordinal()] : 0;
            boolean ok = heb >= e.getValue();
            g.text(font, Component.translatable("gui.guhs.bakkerij.nodig." + e.getKey().id()), x, y, ok ? 0xFFB8F0C8 : 0xFFFF9A9A);
            g.text(font, Math.min(heb, 99) + "/" + e.getValue(), left + W - 34, y, ok ? 0xFFB8F0C8 : 0xFFFF9A9A);
            y += 11;
        }
        y += 4;
        for (var line : font.split(Component.translatable("gui.guhs.bakkerij.vers"), W - 226)) {
            g.text(font, line, x, y, 0xFFB0A090);
            y += 10;
        }
        if (r == Recept.FEESTTAART) {
            for (var line : font.split(Component.translatable("gui.guhs.bakkerij.geheim"), W - 226)) {
                g.text(font, line, x, y + 4, 0xFFFF9FD8);
                y += 10;
            }
        }
    }

    /** The oven bar: raw (grey), good (yellow), perfect (gold), good, burnt (dark); the pointer while something bakes. */
    private void ovenBalk(GuiGraphicsExtractor g, float partialTick) {
        int x0 = left + 12, x1 = left + W - 12, y0 = top + 170, y1 = y0 + 14;
        int w = x1 - x0;
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFFFFE2A8);
        zone(g, x0, w, 0, Recept.Kwaliteit.GOED_VAN, y0, y1, 0xFF8A7F78);
        zone(g, x0, w, Recept.Kwaliteit.GOED_VAN, Recept.Kwaliteit.PERFECT_VAN, y0, y1, 0xFFE8C860);
        zone(g, x0, w, Recept.Kwaliteit.PERFECT_VAN, Recept.Kwaliteit.PERFECT_TOT, y0, y1, 0xFFFFB02E);
        zone(g, x0, w, Recept.Kwaliteit.PERFECT_TOT, Recept.Kwaliteit.GOED_TOT, y0, y1, 0xFFE8C860);
        zone(g, x0, w, Recept.Kwaliteit.GOED_TOT, 100, y0, y1, 0xFF4A3028);
        g.centeredText(font, Component.translatable("gui.guhs.bakkerij.kwaliteit.perfect"),
                x0 + w * (Recept.Kwaliteit.PERFECT_VAN + Recept.Kwaliteit.PERFECT_TOT) / 200, y0 + 3, 0xFF5A2A10);
        if (bakt >= 0) {
            float p = Math.min(100f, (tick - bakStart + partialTick) * 100f / bakTicks);
            int px = x0 + Math.round(w * p / 100f);
            g.fill(px - 1, y0 - 4, px + 2, y1 + 4, 0xFFFFFFFF);
            g.fill(px - 3, y0 - 6, px + 4, y0 - 3, 0xFFFF6FA8);
            g.centeredText(font, Component.translatable("gui.guhs.bakkerij.spatie"), width / 2, y1 + 5, 0xFFFFE9C4);
        } else {
            g.centeredText(font, Component.translatable("gui.guhs.bakkerij.uitleg"), width / 2, y1 + 5, 0xFFB0A090);
        }
    }

    private static void zone(GuiGraphicsExtractor g, int x0, int w, int from, int to, int y0, int y1, int colour) {
        g.fill(x0 + w * from / 100, y0, x0 + w * to / 100, y1, colour);
    }

    /** An icon from the button sheet (row 0 dough, 1 shape, 2 topping). */
    private void icoon(GuiGraphicsExtractor g, int rij, int i, int x, int y, int size) {
        g.blit(KNOPPEN, x, y, size, size, i * 16, rij * 16, 16, 16, TEX_W, TEX_H);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A choice button: an icon from the sheet, a golden frame when chosen. */
    private class KeuzeKnop extends Button {
        private final int rij, i;

        KeuzeKnop(int rij, int i, int x, int y, Component naam) {
            super(x, y, 22, 22, naam, b -> {
            }, DEFAULT_NARRATION);
            this.rij = rij;
            this.i = i;
        }

        @Override
        public void onPress() {
            switch (rij) {
                case 0 -> deeg = i;
                case 1 -> vorm = i;
                default -> topping = i;
            }
            verversKnoppen();
        }

        private boolean gekozen() {
            return switch (rij) {
                case 0 -> Math.floorMod(deeg, Recept.Deeg.values().length) == i;
                case 1 -> Math.floorMod(vorm, Recept.Vorm.values().length) == i;
                default -> Math.floorMod(topping, Recept.Topping.values().length) == i;
            };
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            boolean kies = gekozen();
            g.fill(getX(), getY(), getX() + width, getY() + height, kies ? 0xFFFFC040 : isHoveredOrFocused() ? 0xFFB08868 : 0xFF6A4A38);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, kies ? 0xFFFFF0C8 : active ? 0xFFF3DCC0 : 0xFFA89888);
            icoon(g, rij, i, getX() + 3, getY() + 3, 16);
        }
    }
}
