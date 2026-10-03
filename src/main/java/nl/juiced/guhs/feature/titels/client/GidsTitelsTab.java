package nl.juiced.guhs.feature.titels.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.titels.Titels;

/**
 * The Guhdex tab "Titels" (1.2.6): every title as a row. An earned title can be clicked to show it behind your name (one
 * at a time; click it again, or the row "Geen titel", to show none); a title you haven't earned is grey with a little
 * lock and a hint how to earn it. The tooltips tell where the title shows (player list, above your head, chat) and
 * what your name looks like with it. Everything comes from {@link TitelsCache}.
 */
public final class GidsTitelsTab {
    private static final int RIJ = 20, GEEN_RIJ = 16, TIP_BREEDTE = 190;
    private static final int DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, LICHT = 0xFFB0708A, GROEN = 0xFF2A8A50, GRIJS = 0xFF9A8090;

    private final GidsLijst lijst = new GidsLijst();
    private int left, top, w;

    public void init(int left, int top, int w, int h) {
        this.left = left;
        this.top = top;
        this.w = w;
        lijst.plaats(left + 8, top + 28, w - 14, h - 34);
        List<GidsLijst.Regel> regels = new ArrayList<>();
        regels.add(new Geen());
        for (Titels.Titel t : Titels.ALLE) {
            regels.add(new Rij(t));
        }
        lijst.zet(regels);
    }

    public void teken(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
    }

    public boolean wiel(double mx, double my, double delta) {
        return lijst.wiel(mx, my, delta);
    }

    public boolean sleep(double my) {
        return lijst.sleep(my);
    }

    public void los() {
        lijst.los();
    }

    public boolean klik(double mx, double my, int button) {
        return lijst.klik(mx, my, button);
    }

    @Nullable
    public List<Component> tip(double mx, double my) {
        return lijst.tip(mx, my);
    }

    // =====================================================================================================================

    private static void klikGeluid() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private static ItemStack stack(String id) {
        Identifier rl = Identifier.tryParse(id);
        var item = rl == null ? Items.NAME_TAG : BuiltInRegistries.ITEM.getValue(rl);
        return new ItemStack(item == Items.AIR ? Items.NAME_TAG : item);
    }

    /** A text wrapped to tooltip lines of at most {@link #TIP_BREEDTE} wide, each line in this style. */
    static List<Component> regels(Component tekst, Style stijl) {
        Font font = Minecraft.getInstance().font;
        List<Component> out = new ArrayList<>();
        StringBuilder regel = new StringBuilder();
        for (String woord : tekst.getString().split(" ")) {
            if (regel.length() > 0 && font.width(regel + " " + woord) > TIP_BREEDTE) {
                out.add(Component.literal(regel.toString()).withStyle(stijl));
                regel.setLength(0);
            }
            regel.append(regel.length() > 0 ? " " : "").append(woord);
        }
        if (regel.length() > 0) {
            out.add(Component.literal(regel.toString()).withStyle(stijl));
        }
        return out;
    }

    /** Where a title shows (the last lines of every tooltip). */
    private static List<Component> waar() {
        return regels(Component.translatable("gui.guhs.titels.tip.waar"), Style.EMPTY.withColor(ChatFormatting.GRAY));
    }

    private static Component spelerNaam() {
        var speler = Minecraft.getInstance().player;
        return speler == null ? Component.literal("Guh") : speler.getName();
    }

    /** A little lock (7 x 9) with its top-left corner at (x, y). */
    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        int rand = 0xFF4A3A44, lijf = 0xFFE8B84A, beugel = 0xFFB8AEB4;
        // (above the item it sits on: see GidsKledingIcoon)
        g.pose().pushMatrix();
        g.pose().translate(0, 0);
        g.fill(x + 1, y, x + 6, y + 1, rand);
        g.fill(x + 1, y + 1, x + 2, y + 4, rand);
        g.fill(x + 5, y + 1, x + 6, y + 4, rand);
        g.fill(x + 2, y + 1, x + 5, y + 2, beugel);
        g.fill(x, y + 4, x + 7, y + 9, rand);
        g.fill(x + 1, y + 5, x + 6, y + 8, lijf);
        g.fill(x + 3, y + 6, x + 4, y + 7, rand);
        g.pose().popMatrix();
    }

    /** The first row: "Geen titel" (click: show no title), with how many titles you have on the right. */
    private final class Geen implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return GEEN_RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean gekozen = TitelsCache.geladen() && TitelsCache.actief().isEmpty();
            g.fill(x + 1, y + 1, x + w - 1, y + GEEN_RIJ - 1, gekozen ? 0x3068D88A : hover ? 0x40F7B6CB : 0x18F7B6CB);
            Component telling = Component.literal(TitelsCache.behaald().size() + " / " + Titels.ALLE.size() + " ✔");
            GidsTekst.schaal(g, telling, x + w - 5, y + 5, 0.75f, ROZE, true);
            int tw = Math.round(Minecraft.getInstance().font.width(telling) * 0.75f) + 12;
            Component naam = Component.literal(gekozen ? "✔ " : "○ ").append(Component.translatable("gui.guhs.titels.geen"));
            int gebruikt = GidsTekst.passend(g, naam.copy().withStyle(ChatFormatting.BOLD), x + 6, y + 4, 90, 0.875f, gekozen ? GROEN : DONKER, false);
            GidsTekst.passend(g, Component.translatable("gui.guhs.titels.uitleg"), x + 6 + gebruikt + 6, y + 4, w - 12 - gebruikt - 6 - tw, 0.75f, LICHT, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            if (!TitelsCache.actief().isEmpty()) {
                TitelsCache.kies(Titels.GEEN);
                klikGeluid();
            }
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.guhs.titels.geen").withStyle(ChatFormatting.LIGHT_PURPLE));
            tip.addAll(regels(Component.translatable("gui.guhs.titels.geen.tip"), Style.EMPTY.withColor(ChatFormatting.WHITE)));
            tip.addAll(waar());
            return tip;
        }
    }

    /** One title. */
    private final class Rij implements GidsLijst.Regel {
        private final Titels.Titel t;
        private final ItemStack icoon;

        Rij(Titels.Titel t) {
            this.t = t;
            this.icoon = stack(t.icoon());
        }

        private boolean behaald() {
            return TitelsCache.behaald().contains(t.id());
        }

        private boolean gekozen() {
            return TitelsCache.actief().equals(t.id());
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean behaald = behaald(), gekozen = gekozen();
            if (gekozen) {
                g.fill(x + 1, y + 1, x + w - 1, y + RIJ - 1, 0xFF4CC06A);
                g.fill(x + 2, y + 2, x + w - 2, y + RIJ - 2, 0xFFE4F6E6);
            } else {
                g.fill(x + 1, y + 1, x + w - 1, y + RIJ - 1, behaald ? (hover ? 0x50F7B6CB : 0x20F7B6CB) : 0x14806070);
            }
            g.item(icoon, x + 4, y + 2);
            if (!behaald) {
                // locked: a veil over the icon (drawn above the item) and a little lock
                g.pose().pushMatrix();
                g.pose().translate(0, 0);
                g.fill(x + 4, y + 2, x + 20, y + 18, 0xC0F4E6EC);
                g.pose().popMatrix();
                slot(g, x + 14, y + 9);
            }
            int tx = x + 26, rechts = gekozen ? 16 : 0, tw = w - 26 - 8 - rechts;
            GidsTekst.passend(g, t.naam().withStyle(ChatFormatting.BOLD), tx, y + 3, tw, 0.875f, behaald ? DONKER : GRIJS, false);
            Component onder = gekozen ? Component.translatable("gui.guhs.titels.gekozen")
                    : behaald ? Component.translatable("gui.guhs.titels.kies") : t.hint();
            GidsTekst.passend(g, onder, tx, y + 12, tw, 0.75f, gekozen ? GROEN : behaald ? ROZE : GRIJS, false);
            if (gekozen) {
                GidsTekst.schaal(g, Component.literal("✔"), x + w - 8, y + 5, 1.25f, GROEN, true);
            }
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            if (behaald()) {
                TitelsCache.kies(gekozen() ? Titels.GEEN : t.id());   // (click the chosen one again: no title)
                klikGeluid();
            }
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>();
            if (behaald()) {
                tip.add(t.naam().withStyle(t.kleur()));
                tip.add(Component.translatable("gui.guhs.titels.tip.voorbeeld").withStyle(ChatFormatting.WHITE));
                tip.add(Component.literal("  ").append(Titels.metTitel(spelerNaam(), t)));
                tip.addAll(waar());
                tip.add(Component.translatable(gekozen() ? "gui.guhs.titels.tip.weg" : "gui.guhs.titels.tip.kies")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                tip.add(t.naam().withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("gui.guhs.titels.tip.op_slot").withStyle(ChatFormatting.RED));
                tip.addAll(regels(t.hint(), Style.EMPTY.withColor(ChatFormatting.WHITE)));
                tip.addAll(waar());
            }
            return tip;
        }
    }
}
