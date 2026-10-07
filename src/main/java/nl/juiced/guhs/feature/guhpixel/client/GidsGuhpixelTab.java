package nl.juiced.guhs.feature.guhpixel.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GuhpixelPayloads;
import nl.juiced.guhs.feature.guhpixel.Rang;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * The Guhdex tab "Guhpixel &amp; uitjes": on top your muntjes and your rank (click the rank to show or hide the prefix
 * before your name), below it the sections of the slices as one scrolling list. Everything comes from the server as one
 * tag (GidsBlad.stand); the slices write no client code for it.
 */
public final class GidsGuhpixelTab {
    private static final int DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, LICHT = 0xFFB0708A, GROEN = 0xFF2A8A50, GRIJS = 0xFF9A8090, GOUD = 0xFFB07A10;
    private static final int KOP_H = 16, STAT_H = 11, BALK_H = 13, STAP_H = 10, CEL = 22, HOOFD = 30;

    private static CompoundTag data = new CompoundTag();
    private static boolean geladen;
    private static int versie;

    private final GidsLijst lijst = new GidsLijst();
    private int left, top, w, getekend = -1;

    /** New data from the server (also used by AutoCheck). */
    public static void zet(CompoundTag nieuw) {
        data = nieuw;
        geladen = true;
        versie++;
    }

    public static void vergeet() {
        data = new CompoundTag();
        geladen = false;
        versie++;
    }

    /** Asks the server for a fresh page. */
    public static void vraag() {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new GuhpixelPayloads.GidsVraag());
        }
    }

    public void init(int left, int top, int w, int h) {
        this.left = left;
        this.top = top;
        this.w = w;
        lijst.plaats(left + 8, top + 28 + HOOFD, w - 14, h - 34 - HOOFD);
        vul();
    }

    private void vul() {
        double scroll = lijst.scroll();
        List<GidsLijst.Regel> regels = new ArrayList<>();
        ListTag rijen = data.getListOrEmpty("Rijen");
        List<CompoundTag> plaatjes = new ArrayList<>();
        for (int i = 0; i < rijen.size(); i++) {
            CompoundTag r = rijen.getCompoundOrEmpty(i);
            String type = r.getStringOr("T", "");
            if (type.equals(GidsBlad.PLAATJE)) {
                plaatjes.add(r);
                continue;
            }
            if (!plaatjes.isEmpty()) {
                regels.add(new Album(List.copyOf(plaatjes)));
                plaatjes.clear();
            }
            switch (type) {
                case GidsBlad.KOP -> regels.add(new Kop(Tekst.get(r, "A")));
                case GidsBlad.STAT -> regels.add(new Stat(Tekst.get(r, "A"), Tekst.get(r, "B")));
                case GidsBlad.VOORTGANG -> regels.add(new Voortgang(Tekst.get(r, "A"), r.getIntOr("Heb", 0), r.getIntOr("Totaal", 0)));
                case GidsBlad.STAP -> regels.add(new Stap(Tekst.get(r, "A"), r.getBooleanOr("Klaar", false)));
                default -> regels.add(new Alinea(Tekst.get(r, "A")));
            }
        }
        if (!plaatjes.isEmpty()) {
            regels.add(new Album(List.copyOf(plaatjes)));
        }
        if (!geladen) {
            regels.add(new Alinea(Component.translatable("gui.guhs.guhpixel.gids.laden")));
        }
        lijst.zet(regels);
        lijst.scrollNaar(scroll);
        getekend = versie;
    }

    private boolean opRang(double mx, double my) {
        return mx >= left + w / 2 && mx < left + w - 8 && my >= top + 28 && my < top + 28 + HOOFD - 4;
    }

    public void teken(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (getekend != versie) {
            vul();
        }
        // the header: muntjes on the left, the rank on the right
        int y = top + 29;
        g.fill(left + 8, y, left + w - 8, y + HOOFD - 5, 0x24F7B6CB);
        Component munt = Component.translatable("gui.guhs.guhpixel.hud.muntjes", data.getIntOr("Saldo", 0));
        g.text(Minecraft.getInstance().font, munt.copy().withStyle(ChatFormatting.BOLD), left + 13, y + 3, GOUD, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.guhpixel.gids.totaal", data.getIntOr("Totaal", 0)), left + 13, y + 14, w / 2 - 20, 0.75f, LICHT, false);
        Rang rang = Rang.op(data.getIntOr("Rang", 0));
        boolean aan = data.getBooleanOr("RangAan", true), toegang = data.getBooleanOr("Toegang", false);
        if (toegang) {
            boolean hover = opRang(mouseX, mouseY);
            if (hover) {
                g.fill(left + w / 2, y, left + w - 8, y + HOOFD - 5, 0x30F7B6CB);
            }
            Component naam = Component.literal(aan ? "✔ " : "○ ").withStyle(aan ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY).append(rang.naam());
            GidsTekst.schaal(g, naam, left + w - 13, y + 3, 1f, DONKER, true);
            Rang volgende = rang.volgende();
            Component onder = volgende == null ? Component.translatable("gui.guhs.guhpixel.gids.rang_hoogste")
                    : Component.translatable("gui.guhs.guhpixel.gids.rang_volgende", volgende.naam(), Math.max(0, volgende.vanaf() - data.getIntOr("Totaal", 0)));
            GidsTekst.passend(g, onder, left + w - 13, y + 14, w / 2 - 20, 0.75f, LICHT, true);
        }
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
        if (button == 0 && data.getBooleanOr("Toegang", false) && opRang(mx, my)) {
            boolean aan = !data.getBooleanOr("RangAan", true);
            data.putBoolean("RangAan", aan);   // (shown at once; the server's answer follows)
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            if (mc.getConnection() != null) {
                ClientPacketDistributor.sendToServer(new GuhpixelPayloads.RangKies(aan));
            }
            return true;
        }
        return lijst.klik(mx, my, button);
    }

    @Nullable
    public List<Component> tip(double mx, double my) {
        if (data.getBooleanOr("Toegang", false) && opRang(mx, my)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.guhs.guhpixel.gids.rang.tip.titel").withStyle(ChatFormatting.LIGHT_PURPLE));
            tip.addAll(wrap(Component.translatable("gui.guhs.guhpixel.gids.rang.tip"), ChatFormatting.WHITE));
            for (Rang r : Rang.values()) {
                tip.add(Component.literal("  ").append(r.naam()).append(Component.literal("  " + r.vanaf()).withStyle(ChatFormatting.GRAY)));
            }
            tip.add(Component.translatable(data.getBooleanOr("RangAan", true) ? "gui.guhs.guhpixel.gids.rang.tip.uit" : "gui.guhs.guhpixel.gids.rang.tip.aan")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return tip;
        }
        return lijst.tip(mx, my);
    }

    static List<Component> wrap(Component tekst, ChatFormatting kleur) {
        List<Component> out = new ArrayList<>();
        for (var regel : Minecraft.getInstance().font.getSplitter().splitLines(tekst, 190, Style.EMPTY)) {
            out.add(Component.literal(regel.getString()).withStyle(kleur));
        }
        return out;
    }

    // =====================================================================================================================

    private record Kop(Component tekst) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return KOP_H;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y + KOP_H - 2, x + w, y + KOP_H - 1, 0x60D27A9C);
            GidsTekst.passend(g, tekst.copy().withStyle(ChatFormatting.BOLD), x + 2, y + 4, w - 4, 1f, ROZE, false);
        }
    }

    private record Alinea(Component tekst) implements GidsLijst.Regel {
        private int breedte() {
            return 266;
        }

        @Override
        public int hoogte() {
            return GidsTekst.hoogte(tekst, breedte(), 0.875f) + 3;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.alinea(g, tekst, x + 3, y + 1, breedte(), 0.875f, DONKER);
        }
    }

    private record Stat(Component label, Component waarde) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return STAT_H;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            int gebruikt = GidsTekst.passend(g, waarde.copy().withStyle(ChatFormatting.BOLD), x + w - 4, y + 1, w / 2, 0.875f, ROZE, true);
            GidsTekst.passend(g, label, x + 3, y + 1, w - gebruikt - 14, 0.875f, DONKER, false);
        }
    }

    private record Voortgang(Component label, int heb, int totaal) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return BALK_H;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            Component telling = Component.literal(heb + " / " + totaal);
            GidsTekst.schaal(g, telling, x + w - 4, y + 2, 0.875f, heb >= totaal && totaal > 0 ? GROEN : ROZE, true);
            int bw = 70, bx = x + w - 4 - 40 - bw;
            GidsTekst.balk(g, bx, y + 3, bw, 5, totaal <= 0 ? 0f : heb / (float) totaal);
            GidsTekst.passend(g, label, x + 3, y + 2, bx - x - 10, 0.875f, DONKER, false);
        }
    }

    private record Stap(Component tekst, boolean klaar) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return STAP_H;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.schaal(g, Component.literal(klaar ? "✔" : "○"), x + 6, y + 1, 0.875f, klaar ? GROEN : GRIJS, false);
            GidsTekst.passend(g, tekst, x + 17, y + 1, w - 22, 0.8f, klaar ? DONKER : GRIJS, false);
        }
    }

    /** A block of album cells (several rows of icons). */
    private static final class Album implements GidsLijst.Regel {
        private final List<CompoundTag> cellen;
        private final List<ItemStack> iconen = new ArrayList<>();
        private static final int PER_RIJ = 12;

        Album(List<CompoundTag> cellen) {
            this.cellen = cellen;
        }

        private ItemStack icoon(int i) {
            if (iconen.isEmpty()) {
                Minecraft mc = Minecraft.getInstance();
                for (CompoundTag c : cellen) {
                    iconen.add(mc.level == null ? ItemStack.EMPTY : Nbt.parseStack(mc.level.registryAccess(), c.getCompoundOrEmpty("Icoon")));
                }
            }
            return iconen.get(i);
        }

        @Override
        public int hoogte() {
            return ((cellen.size() + PER_RIJ - 1) / PER_RIJ) * CEL + 3;
        }

        private int cel(double mx, double my, int x, int y) {
            int cx = (int) Math.floor((mx - x - 3) / CEL), cy = (int) Math.floor((my - y - 1) / CEL);
            int i = cy * PER_RIJ + cx;
            return cx >= 0 && cx < PER_RIJ && cy >= 0 && i < cellen.size() ? i : -1;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            int onder = hover ? cel(mouseX, mouseY, x, y) : -1;
            for (int i = 0; i < cellen.size(); i++) {
                int cx = x + 3 + (i % PER_RIJ) * CEL, cy = y + 1 + (i / PER_RIJ) * CEL;
                boolean klaar = cellen.get(i).getBooleanOr("Klaar", false);
                g.fill(cx, cy, cx + CEL - 2, cy + CEL - 2, klaar ? 0xFFD27A9C : 0xFFCDB8C2);
                g.fill(cx + 1, cy + 1, cx + CEL - 3, cy + CEL - 3, i == onder ? 0xFFFFE6F0 : klaar ? 0xFFFFF4F8 : 0xFFEADFE4);
                g.item(icoon(i), cx + 2, cy + 2);
                if (!klaar) {
                    g.pose().pushMatrix();
                    g.pose().translate(0, 0);
                    g.fill(cx + 1, cy + 1, cx + CEL - 3, cy + CEL - 3, 0xB0EADFE4);   // (a veil over what you do not have yet)
                    g.pose().popMatrix();
                }
            }
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            int i = cel(mx, my, x, y);
            if (i < 0) {
                return null;
            }
            CompoundTag c = cellen.get(i);
            boolean klaar = c.getBooleanOr("Klaar", false);
            List<Component> tip = new ArrayList<>();
            tip.add(Tekst.get(c, "A").copy().withStyle(klaar ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
            Component b = Tekst.get(c, "B");
            if (!Tekst.empty(b)) {
                tip.addAll(wrap(b, ChatFormatting.WHITE));
            }
            if (!klaar) {
                tip.add(Component.translatable("gui.guhs.guhpixel.gids.nog_niet").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
            return tip;
        }
    }
}
