package nl.juiced.guhs.feature.huisje.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.huisje.HuisjePayloads;

/**
 * 1.2.8: the dialog "Wat kan hier?" on top of the huisje screen ({@link HuisjeScreen} draws it over itself and hands it
 * the mouse and the keys while it is open; Esc or "Terug" closes only the dialog). One scrolling list with two parts:
 * <ul>
 *   <li>Klusjes: per chore its icon and name, ja / straks / nee with a line why (and a count), and how many residents can
 *       do it; the tooltip tells what the chore needs and who can do it;</li>
 *   <li>Speeltjes en meer: per thing residents play with or react to its icon, name and how many stand in the blue area
 *       (also when that is none); the tooltip tells what they do with it.</li>
 * </ul>
 * The numbers come from the server ({@link nl.juiced.guhs.feature.huisje.HuisjeOverzicht}): asked when the dialog opens, on
 * "Ververs" and every {@link #VERVERS} ticks while it is open. Every text is a translation key, so the Guhs language
 * switch works here at once.
 */
final class HuisjeOverzichtPaneel {
    /** Ticks between two automatic refreshes. */
    static final int VERVERS = 60;
    private static final String P = "gui.guhs.huisje.overzicht.";
    private static final int RAND = 0xFFF7B6CB, DONKER = 0xFF3A1C30, TEKST = 0xFF5A3A4A, LICHT = 0xFFB0708A, ROZE = 0xFF7A2848;
    /** The pill colours per state (ja, straks, nee): background and text. */
    private static final int[] PIL = {0x6068D88A, 0x70F2C860, 0x60F77AB0}, PIL_TEKST = {0xFF2E8A4E, 0xFF8A6410, 0xFFB04060};
    private static final ChatFormatting[] TIP_KLEUR = {ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.RED};
    private static final String[] STAAT = {"ja", "straks", "nee"};

    private final BlockPos pos;
    private final Runnable sluit;
    private final GidsLijst lijst = new GidsLijst();
    /** The server's answer (null: still waiting for the first one). */
    @Nullable
    private CompoundTag data;
    private int x, y, w, h;
    private Button ververs, terug;
    private int ticks;

    HuisjeOverzichtPaneel(BlockPos pos, Runnable sluit) {
        this.pos = pos;
        this.sluit = sluit;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Where the dialog is (called from the screen's init, also after a resize). */
    void plaats(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        ververs = Button.builder(Component.translatable(P + "ververs"), b -> vraag()).bounds(x + 6, y + h - 23, 70, 18)
                .tooltip(Tooltip.create(Component.translatable(P + "ververs.tooltip"))).build();
        terug = Button.builder(Component.translatable(P + "terug"), b -> sluit.run()).bounds(x + w - 66, y + h - 23, 60, 18).build();
        lijst.plaats(x + 6, y + 22, w - 12, h - 22 - 28);
        herbouw();
    }

    /** Asks the server (again). */
    void vraag() {
        ticks = 0;
        ClientPacketDistributor.sendToServer(new HuisjePayloads.OverzichtVraag(pos));
    }

    /** The server's answer. */
    void zet(CompoundTag nieuw) {
        data = nieuw;
        herbouw();
    }

    void tick() {
        if (++ticks >= VERVERS) {
            vraag();
        }
    }

    private void herbouw() {
        List<GidsLijst.Regel> rijen = new ArrayList<>();
        if (data == null) {
            rijen.add(new Tekst(Component.translatable(P + "laden")));
            lijst.zet(rijen);
            return;
        }
        rijen.add(new Kop(Component.translatable(P + "kop.klusjes")));
        rijen.add(new Tekst(Component.translatable(P + "uitleg.klusjes")));
        ListTag klusjes = data.getListOrEmpty("Klusjes");
        for (int i = 0; i < klusjes.size(); i++) {
            rijen.add(new KlusRij(klusjes.getCompoundOrEmpty(i)));
        }
        rijen.add(new Kop(Component.translatable(P + "kop.dingen")));
        rijen.add(new Tekst(Component.translatable(P + "uitleg.dingen")));
        ListTag dingen = data.getListOrEmpty("Dingen");
        for (int i = 0; i < dingen.size(); i++) {
            rijen.add(new DingRij(dingen.getCompoundOrEmpty(i)));
        }
        lijst.zet(rijen);
    }

    // =====================================================================================================================
    // drawing and input (all on top of the huisje screen)
    // =====================================================================================================================

    void teken(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.nextStratum();   // (over the screen's own widgets and its little guh pictures)
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0x70201018);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, RAND);
        g.fill(x, y, x + w, y + h, 0xFFFFF4F8);
        g.fill(x, y, x + w, y + 18, 0xFFFBE0EA);
        g.fill(x, y + 18, x + w, y + 19, 0xFFD27A9C);
        g.text(font(), Component.translatable(P + "titel").withStyle(ChatFormatting.BOLD), x + 8, y + 5, ROZE, false);
        lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
        g.fill(x, y + h - 27, x + w, y + h - 26, 0x40D27A9C);
        ververs.extractRenderState(g, mouseX, mouseY, partialTick);
        terug.extractRenderState(g, mouseX, mouseY, partialTick);
        List<Component> tip = lijst.tip(mouseX, mouseY);
        if (tip != null && !tip.isEmpty()) {
            // (wrapped: these tips are whole sentences)
            List<FormattedCharSequence> regels = new ArrayList<>();
            int breed = Math.max(120, Math.min(210, g.guiWidth() - 40));
            for (Component c : tip) {
                regels.addAll(font().split(c, breed));
            }
            g.setTooltipForNextFrame(font(), regels, mouseX, mouseY);
        }
    }

    boolean klik(MouseButtonEvent event, boolean dubbel) {
        if (ververs.mouseClicked(event, dubbel) || terug.mouseClicked(event, dubbel)) {
            return true;
        }
        lijst.klik(event.x(), event.y(), event.button());
        return true;
    }

    boolean wiel(double mx, double my, double sy) {
        lijst.wiel(mx, my, sy);
        return true;
    }

    boolean sleep(double my) {
        lijst.sleep(my);
        return true;
    }

    void los() {
        lijst.los();
    }

    /** Arrow keys and page up/down scroll the list. */
    boolean toets(int keyCode) {
        switch (keyCode) {
            case 264 -> lijst.scrollNaar(lijst.scroll() + GidsLijst.STAP);
            case 265 -> lijst.scrollNaar(lijst.scroll() - GidsLijst.STAP);
            case 267 -> lijst.scrollNaar(lijst.scroll() + 100);
            case 266 -> lijst.scrollNaar(lijst.scroll() - 100);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static ItemStack icoon(String id) {
        Identifier rl = Identifier.tryParse(id);
        return rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(rl));
    }

    // =====================================================================================================================
    // rows
    // =====================================================================================================================

    /** A section header with a line under it. */
    private static final class Kop implements GidsLijst.Regel {
        private final Component tekst;

        Kop(Component tekst) {
            this.tekst = tekst;
        }

        @Override
        public int hoogte() {
            return 16;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.passend(g, tekst.copy().withStyle(ChatFormatting.BOLD), x + 2, y + 4, w - 4, 1f, ROZE, false);
            g.fill(x, y + 14, x + w, y + 15, 0x80D27A9C);
        }
    }

    /** A wrapped little explanation. */
    private final class Tekst implements GidsLijst.Regel {
        private final Component tekst;

        Tekst(Component tekst) {
            this.tekst = tekst;
        }

        @Override
        public int hoogte() {
            return GidsTekst.hoogte(tekst, lijst.rijBreedte() - 4, 0.75f) + 5;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.alinea(g, tekst, x + 2, y + 2, w - 4, 0.75f, LICHT);
        }
    }

    /** One chore: [icon] name + why, [ja / straks / nee] + how many residents can do it. */
    private final class KlusRij implements GidsLijst.Regel {
        private static final int PIL_B = 44, RECHTS = 62;
        private final String id;
        private final ItemStack icoon;
        private final int staat, kunnen, aan;
        private final Component waarom;
        private final String wie;

        KlusRij(CompoundTag c) {
            this.id = c.getStringOr("Id", "");
            this.icoon = icoon(c.getStringOr("Icoon", ""));
            this.staat = Math.floorMod(c.getByteOr("Staat", (byte) 0), 3);
            this.kunnen = c.getIntOr("Kunnen", 0);
            this.aan = c.getIntOr("Aan", 0);
            this.wie = c.getStringOr("Wie", "");
            String reden = c.getStringOr("Reden", "");
            this.waarom = reden.isEmpty() ? Component.empty() : Component.translatable(P + "klus." + id + "." + reden, c.getIntOr("Aantal", 0));
        }

        private Component naam() {
            return Component.translatable("gui.guhs.klus." + id);
        }

        @Override
        public int hoogte() {
            return Math.max(27, 17 + GidsTekst.hoogte(waarom, lijst.rijBreedte() - 23 - RECHTS, 0.625f) + 4);
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y, x + w, y + hoogte() - 2, hover ? 0x30F77AB0 : 0x18F7B6CB);
            if (!icoon.isEmpty()) {
                g.item(icoon, x + 3, y + 4);
            }
            GidsTekst.passend(g, naam().copy().withStyle(ChatFormatting.BOLD), x + 23, y + 4, w - 23 - RECHTS, 0.875f, DONKER, false);
            GidsTekst.alinea(g, waarom, x + 23, y + 15, w - 23 - RECHTS, 0.625f, TEKST);
            // the pill
            int px = x + w - 4 - PIL_B;
            g.fill(px, y + 3, px + PIL_B, y + 15, PIL[staat]);
            Component pil = Component.translatable(P + STAAT[staat]).withStyle(ChatFormatting.BOLD);
            int tw = Math.min(PIL_B - 6, Math.round(font().width(pil) * 0.75f));
            GidsTekst.passend(g, pil, px + (PIL_B - tw) / 2, y + 5, PIL_B - 6, 0.75f, PIL_TEKST[staat], false);
            // who lives here that can do it
            Component wonen = kunnen > 0 ? Component.translatable(P + "rij.kunnen", kunnen) : Component.translatable(P + "rij.niemand");
            GidsTekst.passend(g, wonen, x + w - 4, y + 17, RECHTS - 6, 0.625f, kunnen > 0 ? LICHT : 0xFFB04060, true);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> uit = new ArrayList<>();
            uit.add(naam().copy().withStyle(ChatFormatting.BOLD));
            if (!waarom.getString().isEmpty()) {
                uit.add(waarom.copy().withStyle(TIP_KLEUR[staat]));
            }
            String nodig = P + "klus." + id + ".nodig";
            uit.add((I18n.exists(nodig) ? Component.translatable(P + "nodig", Component.translatable(nodig))
                    : Component.translatable("gui.guhs.klus." + id + ".tip")).withStyle(ChatFormatting.GRAY));
            MutableComponent wonen = kunnen > 0 ? Component.translatable(P + "bewoners.ja", kunnen, aan).withStyle(ChatFormatting.AQUA)
                    : Component.translatable(P + "bewoners.nee").withStyle(ChatFormatting.LIGHT_PURPLE);
            if (!wie.isEmpty()) {
                uit.add(Component.translatable(P + "wie." + wie).withStyle(ChatFormatting.WHITE));
            }
            uit.add(wonen);
            return uit;
        }
    }

    /** One thing to play with (or react to): [icon] name, how many. */
    private final class DingRij implements GidsLijst.Regel {
        private final String id;
        private final ItemStack icoon;
        private final Component naam;
        private final int aantal;

        DingRij(CompoundTag c) {
            this.id = c.getStringOr("Id", "");
            this.icoon = icoon(c.getStringOr("Icoon", ""));
            this.naam = Component.translatable(c.getStringOr("Naam", ""));
            this.aantal = c.getIntOr("Aantal", 0);
        }

        @Override
        public int hoogte() {
            return 22;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y, x + w, y + 20, hover ? 0x30F77AB0 : 0x18F7B6CB);
            if (!icoon.isEmpty()) {
                g.item(icoon, x + 3, y + 2);
            }
            GidsTekst.passend(g, naam.copy().withStyle(ChatFormatting.BOLD), x + 23, y + 6, w - 23 - 40, 0.875f, aantal > 0 ? DONKER : LICHT, false);
            int px = x + w - 4 - 30;
            g.fill(px, y + 4, px + 30, y + 16, aantal > 0 ? PIL[0] : 0x40A0A0A0);
            Component n = Component.literal(Integer.toString(aantal)).withStyle(ChatFormatting.BOLD);
            int tw = Math.min(26, Math.round(font().width(n) * 0.875f));
            GidsTekst.passend(g, n, px + (30 - tw) / 2, y + 6, 26, 0.875f, aantal > 0 ? PIL_TEKST[0] : 0xFF9A8A92, false);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(naam.copy().withStyle(ChatFormatting.BOLD),
                    Component.translatable(P + "ding.aantal", aantal).withStyle(aantal > 0 ? ChatFormatting.GREEN : ChatFormatting.RED),
                    Component.translatable(P + "ding." + id).withStyle(ChatFormatting.GRAY));
        }
    }
}
